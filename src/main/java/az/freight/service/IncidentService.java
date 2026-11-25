package az.freight.service;

import az.freight.client.RouteClient;
import az.freight.config.DispatchProperties;
import az.freight.dto.BreakdownRequest;
import az.freight.dto.IncidentDto;
import az.freight.dto.RouteInfo;
import az.freight.entity.Assignment;
import az.freight.entity.Driver;
import az.freight.entity.Incident;
import az.freight.entity.Order;
import az.freight.entity.Truck;
import az.freight.enums.AssignmentKind;
import az.freight.enums.IncidentStatus;
import az.freight.enums.OrderEventType;
import az.freight.enums.OrderStatus;
import az.freight.enums.TruckStatus;
import az.freight.event.IncidentReportedEvent;
import az.freight.event.ReplacementAssignedEvent;
import az.freight.exception.ApiException;
import az.freight.exception.ResourceNotFoundException;
import az.freight.filter.TruckFilter;
import az.freight.mapper.IncidentMapper;
import az.freight.repository.AssignmentRepository;
import az.freight.repository.DriverRepository;
import az.freight.repository.IncidentRepository;
import az.freight.repository.OrderRepository;
import az.freight.repository.TruckRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Texniki problem axını:
 * 1) sürücü bildirir → maşın MAINTENANCE, sifariş BREAKDOWN, nasazlıq qeydə alınır;
 * 2) sistem nasazlıq nöqtəsinə ən yaxın BOŞ və uyğun maşını kilidləyib çağırır (REPLACEMENT təyinatı);
 * 3) boş maşın yoxdursa nasazlıq AWAITING_TRUCK qalır, planlaşdırıcı hər 30 saniyədə yenidən cəhd edir;
 * 4) əvəzedici sürücü yükü götürdüyünü təsdiqləyəndə nasazlıq bağlanır.
 */
@Service
@RequiredArgsConstructor
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private final IncidentRepository incidents;
    private final OrderRepository orders;
    private final TruckRepository trucks;
    private final DriverRepository drivers;
    private final AssignmentRepository assignments;
    private final IncidentMapper mapper;
    private final RouteClient routes;
    private final ScoringService scorer;
    private final List<TruckFilter> filters;
    private final DispatchProperties props;
    private final OrderEventService eventLog;
    private final ApplicationEventPublisher publisher;

    // ---------------------------------------------------------------- bildiriş

    @Transactional
    public IncidentDto report(Driver driver, Long orderId, BreakdownRequest req) {
        if (driver.getTruckId() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "Sizə maşın bağlanmayıb");
        }
        Order order = orders.findForUpdate(orderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Sifariş", orderId));
        if (order.getStatus() != OrderStatus.TRUCK_ASSIGNED && order.getStatus() != OrderStatus.IN_TRANSIT) {
            throw new ApiException(HttpStatus.CONFLICT, "Bu sifariş üçün problem bildirmək mümkün deyil");
        }
        Assignment latest = assignments.findTopByOrderIdOrderByIdDesc(orderId).orElse(null);
        if (latest == null || !latest.getTruckId().equals(driver.getTruckId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bu reys sizin maşına aid deyil");
        }
        Truck truck = trucks.findForUpdate(driver.getTruckId())
                .orElseThrow(() -> ResourceNotFoundException.of("Maşın", driver.getTruckId()));

        // Yükün olduğu nöqtə: əvvəlki nasazlıqda yük hələ götürülməyibsə, yük orada qalıb
        double lat = req.getLat() != null && req.getLon() != null ? req.getLat() : truck.getLat();
        double lon = req.getLat() != null && req.getLon() != null ? req.getLon() : truck.getLon();
        List<Incident> openOnes = incidents.findByOrderIdAndStatusNot(orderId, IncidentStatus.CLOSED);
        if (openOnes.isEmpty() && order.hasPickup() && order.getStatus() == OrderStatus.TRUCK_ASSIGNED) {
            // Maşın hələ yükləməyə gedirdi: yük yükləmə nöqtəsindədir, əvəzedici ora gəlməlidir
            lat = order.getPickupLat();
            lon = order.getPickupLon();
        }
        for (Incident open : openOnes) {
            lat = open.getLat();
            lon = open.getLon();
            close(open, "Əvəzedici maşın da nasaz oldu, yeni maşın axtarılır");
        }

        truck.setStatus(TruckStatus.MAINTENANCE);
        order.setStatus(OrderStatus.BREAKDOWN);

        Incident inc = new Incident();
        inc.setOrderId(orderId);
        inc.setTruckId(truck.getId());
        inc.setDriverId(driver.getId());
        inc.setType(req.getType());
        inc.setDescription(req.getDescription() == null ? null : req.getDescription().trim());
        inc.setLat(lat);
        inc.setLon(lon);
        incidents.save(inc);

        String desc = inc.getDescription() == null || inc.getDescription().isBlank() ? "" : ": " + inc.getDescription();
        eventLog.log(orderId, OrderEventType.BREAKDOWN,
                truck.getPlate() + " maşını dayandı (" + req.getType().label() + ")" + desc, driver.getFullName());
        publisher.publishEvent(new IncidentReportedEvent(orderId, inc.getId(), truck.getPlate(), req.getType().label()));
        log.warn("Nasazlıq #{} sifariş #{} maşın {} ({})", inc.getId(), orderId, truck.getPlate(), req.getType());

        // Marşrut xidməti işləməsə belə bildiriş itmir: xəta tutulur, planlaşdırıcı sonra yenidən cəhd edir
        try {
            tryAssignReplacement(inc, order);
        } catch (RuntimeException ex) {
            log.error("Nasazlıq #{} üçün əvəzedici maşın seçilə bilmədi, sonra yenidən cəhd olunacaq", inc.getId(), ex);
        }
        return view(inc);
    }

    // ---------------------------------------------------------------- əvəzedici axtarışı

    /** Planlaşdırıcı çağırır. Hər nasazlıq öz əməliyyatında işlənir. */
    @Transactional
    public void retry(Long incidentId) {
        Incident inc = incidents.findForUpdate(incidentId).orElse(null);
        if (inc == null || inc.getStatus() != IncidentStatus.AWAITING_TRUCK) {
            return;
        }
        Order order = orders.findForUpdate(inc.getOrderId()).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.BREAKDOWN) {
            close(inc, "Sifariş artıq aktiv deyil");
            return;
        }
        tryAssignReplacement(inc, order);
    }

    /** Order və incident artıq kilidlənib. Uğurludursa true. */
    private boolean tryAssignReplacement(Incident inc, Order order) {
        inc.setAttempts(inc.getAttempts() + 1);
        inc.setLastAttemptAt(Instant.now());

        List<Truck> nearest = trucks.findNearestFreeForUpdate(order.getCargoType().truckTypeNames(), order.getTons(),
                inc.getLon(), inc.getLat(), props.candidateLimit());

        Truck best = null;
        RouteInfo bestPickup = null;
        RouteInfo bestOnward = null;
        double bestScore = Double.MAX_VALUE;
        for (Truck t : nearest) {
            if (filters.stream().anyMatch(f -> f.reject(t, order).isPresent())) continue;
            RouteInfo pickup;
            RouteInfo onward;
            try {
                pickup = routes.route(t.getLat(), t.getLon(), inc.getLat(), inc.getLon());
                onward = routes.route(inc.getLat(), inc.getLon(), order.getDestLat(), order.getDestLon());
            } catch (RuntimeException ex) {
                log.warn("Marşrut hesablanmadı (maşın {}): {}", t.getPlate(), ex.getMessage());
                continue;
            }
            double score = scorer.rescueScore(order, t, pickup, onward);
            if (score < bestScore) {
                best = t;
                bestPickup = pickup;
                bestOnward = onward;
                bestScore = score;
            }
        }

        if (best == null) {
            eventLog.log(order.getId(), OrderEventType.REPLACEMENT_SEARCH,
                    "Boş maşın tapılmadı, axtarış davam edir (cəhd " + inc.getAttempts() + ")", "SYSTEM");
            log.info("Nasazlıq #{}: boş maşın yoxdur (cəhd {})", inc.getId(), inc.getAttempts());
            return false;
        }
        assignReplacement(inc, order, best, bestPickup, bestOnward, bestScore, "AUTO",
                "Nasazlıq nöqtəsinə ən tez çatan boş maşın seçildi");
        return true;
    }

    /** Dispetçer əvəzedici maşını əllə seçir (yalnız axtarış davam edərkən). */
    @Transactional
    public IncidentDto assignManually(Long incidentId, Long truckId, String user) {
        Incident inc = incidents.findForUpdate(incidentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Nasazlıq", incidentId));
        if (inc.getStatus() != IncidentStatus.AWAITING_TRUCK) {
            throw new ApiException(HttpStatus.CONFLICT, "Bu nasazlıq üçün maşın artıq təyin olunub və ya bağlanıb");
        }
        Order order = orders.findForUpdate(inc.getOrderId())
                .orElseThrow(() -> ResourceNotFoundException.of("Sifariş", inc.getOrderId()));
        if (order.getStatus() != OrderStatus.BREAKDOWN) {
            throw new ApiException(HttpStatus.CONFLICT, "Sifariş artıq nasazlıq vəziyyətində deyil");
        }
        Truck t = trucks.findForUpdate(truckId).orElseThrow(() -> ResourceNotFoundException.of("Maşın", truckId));
        if (t.getStatus() != TruckStatus.FREE) {
            throw new ApiException(HttpStatus.CONFLICT, "Seçilən maşın hazırda boş deyil");
        }
        if (!order.getCargoType().truckTypeNames().contains(t.getType().name()) || t.getCapacityTons() < order.getTons()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Seçilən maşın bu yükə uyğun deyil");
        }
        RouteInfo pickup = routes.route(t.getLat(), t.getLon(), inc.getLat(), inc.getLon());
        RouteInfo onward = routes.route(inc.getLat(), inc.getLon(), order.getDestLat(), order.getDestLon());
        assignReplacement(inc, order, t, pickup, onward, scorer.rescueScore(order, t, pickup, onward),
                user == null || user.isBlank() ? "OPERATOR" : user, "Dispetçer əllə seçdi");
        return view(inc);
    }

    private void assignReplacement(Incident inc, Order order, Truck t, RouteInfo pickup, RouteInfo onward,
                                   double score, String by, String why) {
        t.setStatus(TruckStatus.BUSY);
        order.setStatus(OrderStatus.TRUCK_ASSIGNED);

        Assignment a = new Assignment();
        a.setOrderId(order.getId());
        a.setTruckId(t.getId());
        a.setKind(AssignmentKind.REPLACEMENT);
        a.setIncidentId(inc.getId());
        a.setPickupLat(inc.getLat());
        a.setPickupLon(inc.getLon());
        a.setPickupKm(pickup.km());
        a.setPickupEtaHours(pickup.hours());
        a.setDistanceKm(pickup.km() + onward.km());
        a.setEtaHours(pickup.hours() + onward.hours());
        a.setCost(scorer.cost(t, pickup) + scorer.cost(t, onward));
        a.setScore(score);
        a.setReason(why);
        a.setAssignedBy(by);
        assignments.save(a);

        inc.setStatus(IncidentStatus.REPLACEMENT_ASSIGNED);
        inc.setReplacementTruckId(t.getId());
        inc.setReplacementAssignmentId(a.getId());
        inc.setReplacementAssignedAt(Instant.now());

        eventLog.log(order.getId(), OrderEventType.REPLACEMENT_ASSIGNED,
                "Əvəzedici maşın çağırıldı: " + t.getPlate() + ", nasazlıq nöqtəsinə təxminən "
                        + String.format("%.0f", pickup.km()) + " km (" + String.format("%.1f", pickup.hours()) + " saat)",
                by);
        publisher.publishEvent(new ReplacementAssignedEvent(order.getId(), inc.getId(), t.getId(),
                t.getPlate(), pickup.hours()));
        log.info("Nasazlıq #{}: əvəzedici maşın {} ({} km)", inc.getId(), t.getPlate(), String.format("%.1f", pickup.km()));
    }

    // ---------------------------------------------------------------- yükü götürmə, bağlama

    /** Əvəzedici sürücü nasazlıq nöqtəsində yükü götürdüyünü təsdiqləyir. */
    @Transactional
    public void markCargoPickedUp(Driver driver, Long orderId) {
        Order order = orders.findForUpdate(orderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Sifariş", orderId));
        Assignment latest = assignments.findTopByOrderIdOrderByIdDesc(orderId).orElse(null);
        if (latest == null || driver.getTruckId() == null || !latest.getTruckId().equals(driver.getTruckId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bu reys sizin maşına aid deyil");
        }
        if (latest.getKind() != AssignmentKind.REPLACEMENT || latest.getIncidentId() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "Bu reysdə yük götürmə tələb olunmur");
        }
        if (order.getStatus() != OrderStatus.TRUCK_ASSIGNED) {
            throw new ApiException(HttpStatus.CONFLICT, "Sifariş bu mərhələdə deyil");
        }
        Incident inc = incidents.findForUpdate(latest.getIncidentId()).orElse(null);
        if (inc == null || inc.getStatus() == IncidentStatus.CLOSED) {
            throw new ApiException(HttpStatus.CONFLICT, "Yük artıq götürülüb");
        }
        close(inc, null);
        eventLog.log(orderId, OrderEventType.CARGO_PICKED_UP, "Yük əvəzedici maşına götürüldü", driver.getFullName());
    }

    /** Sifariş bağlananda/hovuza qayıdanda açıq nasazlıqlar da bağlanır. */
    @Transactional
    public void closeOpenForOrder(Long orderId, String note) {
        for (Incident i : incidents.findByOrderIdAndStatusNot(orderId, IncidentStatus.CLOSED)) {
            close(i, note);
        }
    }

    @Transactional(readOnly = true)
    public boolean hasOpen(Long orderId) {
        return !incidents.findByOrderIdAndStatusNot(orderId, IncidentStatus.CLOSED).isEmpty();
    }

    /**
     * Dispetçer əvəzedici reysi başqa maşına ötürəndə yeni təyinat "REPLACEMENT" xüsusiyyətini miras alır:
     * maşın yenə əvvəl nasazlıq nöqtəsinə getməlidir.
     */
    @Transactional
    public void inheritOnTransfer(Assignment prev, Assignment next, Truck nextTruck, Order order) {
        if (prev == null || prev.getKind() != AssignmentKind.REPLACEMENT || prev.getIncidentId() == null) return;
        Incident inc = incidents.findForUpdate(prev.getIncidentId()).orElse(null);
        if (inc == null || inc.getStatus() == IncidentStatus.CLOSED) return;
        RouteInfo pickup = routes.route(nextTruck.getLat(), nextTruck.getLon(), inc.getLat(), inc.getLon());
        RouteInfo onward = routes.route(inc.getLat(), inc.getLon(), order.getDestLat(), order.getDestLon());
        next.setKind(AssignmentKind.REPLACEMENT);
        next.setIncidentId(inc.getId());
        next.setPickupLat(inc.getLat());
        next.setPickupLon(inc.getLon());
        next.setPickupKm(pickup.km());
        next.setPickupEtaHours(pickup.hours());
        next.setDistanceKm(pickup.km() + onward.km());
        next.setEtaHours(pickup.hours() + onward.hours());
        next.setCost(scorer.cost(nextTruck, pickup) + scorer.cost(nextTruck, onward));
        inc.setReplacementTruckId(nextTruck.getId());
    }

    private void close(Incident inc, String note) {
        inc.setStatus(IncidentStatus.CLOSED);
        inc.setClosedAt(Instant.now());
        if (note != null) {
            eventLog.log(inc.getOrderId(), OrderEventType.NOTE, note, "SYSTEM");
        }
    }

    // ---------------------------------------------------------------- oxuma

    @Transactional(readOnly = true)
    public Page<IncidentDto> list(IncidentStatus status, Pageable pageable) {
        Page<Incident> page = status == null ? incidents.findAll(pageable) : incidents.findByStatus(status, pageable);
        return page.map(this::view);
    }

    @Transactional(readOnly = true)
    public IncidentDto get(Long id) {
        return view(incidents.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Nasazlıq", id)));
    }

    @Transactional(readOnly = true)
    public Optional<IncidentDto> latestForOrder(Long orderId) {
        return incidents.findTopByOrderIdOrderByIdDesc(orderId).map(this::view);
    }

    /** Nasaz maşının bağlanmamış nasazlıqlarının sifariş id-ləri (sürücü öz nasazlığını görsün deyə). */
    @Transactional(readOnly = true)
    public List<Incident> openForTruck(Long truckId) {
        return new ArrayList<>(incidents.findByTruckIdAndStatusNotOrderByIdDesc(truckId, IncidentStatus.CLOSED));
    }

    @Transactional(readOnly = true)
    public boolean truckHasOpenIncident(Long truckId) {
        return incidents.existsByTruckIdAndStatusNot(truckId, IncidentStatus.CLOSED);
    }

    IncidentDto view(Incident i) {
        Truck truck = trucks.findById(i.getTruckId()).orElse(null);
        Driver driver = i.getDriverId() == null ? null : drivers.findById(i.getDriverId()).orElse(null);
        Truck repl = i.getReplacementTruckId() == null ? null : trucks.findById(i.getReplacementTruckId()).orElse(null);
        Driver replDriver = repl == null ? null : drivers.findFirstByTruckIdAndActiveTrue(repl.getId()).orElse(null);
        Assignment ra = i.getReplacementAssignmentId() == null ? null
                : assignments.findById(i.getReplacementAssignmentId()).orElse(null);
        return mapper.toDto(i, truck, driver, repl, replDriver, ra);
    }
}
