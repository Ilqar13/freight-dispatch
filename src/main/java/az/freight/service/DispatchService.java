package az.freight.service;

import az.freight.config.DispatchProperties;
import az.freight.client.RouteClient;
import az.freight.dto.AssignmentDto;
import az.freight.dto.Candidate;
import az.freight.dto.RouteInfo;
import az.freight.dto.TransferRequest;
import az.freight.entity.Assignment;
import az.freight.entity.Order;
import az.freight.entity.Truck;
import az.freight.enums.OrderEventType;
import az.freight.enums.OrderStatus;
import az.freight.enums.TruckStatus;
import az.freight.event.TruckAssignedEvent;
import az.freight.exception.ApiException;
import az.freight.exception.ResourceNotFoundException;
import az.freight.filter.TruckFilter;
import az.freight.util.GeoUtils;
import az.freight.mapper.AssignmentMapper;
import az.freight.repository.AssignmentRepository;
import az.freight.repository.OrderRepository;
import az.freight.repository.TruckRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DispatchService {

    private final TruckRepository trucks;
    private final OrderRepository orders;
    private final AssignmentRepository assignments;
    private final AssignmentMapper assignmentMapper;
    private final RouteClient routes;
    private final ScoringService scorer;
    private final List<TruckFilter> filters;
    private final DispatchProperties props;
    private final ApplicationEventPublisher events;
    private final IncidentService incidents;
    private final OrderEventService eventLog;

    /** Təklif: sifariş üçün namizəd maşınlar, ən yaxşıdan başlayaraq. Heç nə dəyişmir. */
    @Transactional(readOnly = true)
    public List<Candidate> preview(Long orderId) {
        Order order = orders.findById(orderId).orElseThrow(() -> ResourceNotFoundException.of("Sifariş", orderId));
        return rank(order);
    }

    /** Təyinat: truckId verilməyibsə ən yaxşı namizəd seçilir (operator isə istənilən namizədi seçə bilər). */
    @Transactional
    public AssignmentDto assign(Long orderId, Long truckId, String user) {
        Order order = orders.findForUpdate(orderId).orElseThrow(() -> ResourceNotFoundException.of("Sifariş", orderId));
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new ApiException(HttpStatus.CONFLICT, "Sifarişə artıq maşın təyin olunub və ya sifariş bağlanıb");
        }

        List<Candidate> ranked = rank(order);
        if (ranked.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Uyğun maşın tapılmadı, sifariş operatora yönləndirilməlidir");
        }
        Candidate chosen = truckId == null
                ? ranked.get(0)
                : ranked.stream().filter(c -> c.getTruckId().equals(truckId)).findFirst()
                        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Seçilən maşın bu sifariş üçün uyğun deyil"));

        // Eyni maşını iki sifarişə verməmək üçün sətir kilidi
        Truck truck = trucks.findForUpdate(chosen.getTruckId())
                .orElseThrow(() -> ResourceNotFoundException.of("Maşın", chosen.getTruckId()));
        if (truck.getStatus() != TruckStatus.FREE) {
            throw new ApiException(HttpStatus.CONFLICT, "Maşın artıq başqa sifarişə təyin olunub, yenidən cəhd edin");
        }
        truck.setStatus(TruckStatus.BUSY);
        order.setStatus(OrderStatus.TRUCK_ASSIGNED);

        Assignment a = new Assignment();
        a.setOrderId(order.getId());
        a.setTruckId(truck.getId());
        a.setDistanceKm(chosen.getDistanceKm());
        a.setEtaHours(chosen.getEtaHours());
        a.setCost(chosen.getCost());
        a.setScore(chosen.getScore());
        a.setReason(String.join("; ", chosen.getReasons()));
        a.setAssignedBy(truckId == null ? "AUTO" : (user == null ? "OPERATOR" : user));
        assignments.save(a);

        eventLog.log(order.getId(), OrderEventType.ASSIGNED,
                truck.getPlate() + " maşını təyin olundu (" + a.getAssignedBy() + ")", a.getAssignedBy());
        events.publishEvent(new TruckAssignedEvent(order.getId(), truck.getId(), truck.getPlate(), chosen.getEtaHours()));
        return assignmentMapper.toDto(a);
    }

    /**
     * Sürücü hovuzdakı sifarişi özü qəbul edir. Sifariş və maşın kilidlənir, ona görə iki sürücü
     * eyni sifarişi (və ya bir maşın iki sifarişi) eyni anda götürə bilməz.
     */
    @Transactional
    public AssignmentDto claim(Long orderId, Long truckId, String driverName) {
        Order order = orders.findForUpdate(orderId).orElseThrow(() -> ResourceNotFoundException.of("Sifariş", orderId));
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new ApiException(HttpStatus.CONFLICT, "Bu sifariş artıq götürülüb");
        }
        Truck truck = trucks.findForUpdate(truckId).orElseThrow(() -> ResourceNotFoundException.of("Maşın", truckId));
        if (truck.getStatus() != TruckStatus.FREE) {
            throw new ApiException(HttpStatus.CONFLICT, "Maşınınız hazırda boş deyil (aktiv reysiniz və ya texniki xidmətdə)");
        }
        if (!order.getCargoType().truckTypeNames().contains(truck.getType().name())
                || truck.getCapacityTons() < order.getTons()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Maşınınız bu yükə uyğun deyil");
        }
        RouteInfo route = trip(truck, order, loadedLeg(order));

        truck.setStatus(TruckStatus.BUSY);
        order.setStatus(OrderStatus.TRUCK_ASSIGNED);

        Assignment a = new Assignment();
        a.setOrderId(order.getId());
        a.setTruckId(truck.getId());
        a.setDistanceKm(route.km());
        a.setEtaHours(route.hours());
        a.setCost(scorer.cost(truck, route));
        a.setScore(scorer.score(order, truck, route));
        a.setReason("Sürücü özü qəbul etdi");
        a.setAssignedBy("DRIVER:" + driverName);
        assignments.save(a);

        eventLog.log(orderId, OrderEventType.ACCEPTED, driverName + " sifarişi qəbul etdi (" + truck.getPlate() + ")", driverName);
        events.publishEvent(new TruckAssignedEvent(order.getId(), truck.getId(), truck.getPlate(), route.hours()));
        return assignmentMapper.toDto(a);
    }

    /**
     * Aktiv reysi başqa (boş və uyğun) maşına ötürür.
     * Köhnə maşın boşaldılır və ya outOfService = true olduqda texniki xidmətə keçirilir.
     * Sifariş yenidən TRUCK_ASSIGNED olur, yeni təyinat cədvəl sətri yaranır.
     */
    @Transactional
    public AssignmentDto transfer(Long orderId, TransferRequest body, String user) {
        if (body == null || body.getTruckId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Yeni maşın (truckId) göstərilməlidir");
        }

        Order order = orders.findForUpdate(orderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Sifariş", orderId));
        if (order.getStatus() != OrderStatus.TRUCK_ASSIGNED && order.getStatus() != OrderStatus.IN_TRANSIT) {
            throw new ApiException(HttpStatus.CONFLICT, "Yalnız aktiv reys başqa sürücüyə ötürülə bilər");
        }

        Truck next = trucks.findForUpdate(body.getTruckId())
                .orElseThrow(() -> ResourceNotFoundException.of("Maşın", body.getTruckId()));
        if (next.getStatus() != TruckStatus.FREE) {
            throw new ApiException(HttpStatus.CONFLICT, "Seçilən maşın hazırda boş deyil");
        }
        if (!order.getCargoType().truckTypeNames().contains(next.getType().name())
                || next.getCapacityTons() < order.getTons()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Seçilən maşın bu yükə uyğun deyil");
        }

        Assignment current = assignments.findTopByOrderIdOrderByIdDesc(orderId).orElse(null);
        if (current != null && current.getTruckId().equals(next.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Yük artıq bu maşındadır");
        }

        // Köhnə maşın: qəza/nasazlıq varsa xidmətdən çıxarılır, əks halda boşaldılır
        if (current != null) {
            trucks.findForUpdate(current.getTruckId()).ifPresent(old -> {
                if (body.isOutOfService()) {
                    old.setStatus(TruckStatus.MAINTENANCE);
                } else if (old.getStatus() == TruckStatus.BUSY) {
                    old.setStatus(TruckStatus.FREE);
                }
            });
        }

        next.setStatus(TruckStatus.BUSY);
        order.setStatus(OrderStatus.TRUCK_ASSIGNED);

        RouteInfo route = trip(next, order, loadedLeg(order));

        Assignment a = new Assignment();
        a.setOrderId(order.getId());
        a.setTruckId(next.getId());
        a.setDistanceKm(route.km());
        a.setEtaHours(route.hours());
        a.setCost(scorer.cost(next, route));
        a.setScore(scorer.score(order, next, route));

        StringBuilder reason = new StringBuilder("Yük başqa sürücüyə ötürüldü");
        if (body.getReason() != null && !body.getReason().isBlank()) {
            reason.append(": ").append(body.getReason().trim());
        }
        if (body.isOutOfService()) {
            reason.append(" (köhnə maşın texniki xidmətə keçirildi)");
        }
        a.setReason(reason.toString());
        a.setAssignedBy(user == null ? "DRIVER" : user);
        incidents.inheritOnTransfer(current, a, next, order);
        assignments.save(a);

        eventLog.log(orderId, OrderEventType.TRANSFERRED,
                "Reys " + next.getPlate() + " maşınına ötürüldü" + (body.getReason() == null || body.getReason().isBlank() ? "" : ": " + body.getReason().trim()),
                a.getAssignedBy());
        events.publishEvent(new TruckAssignedEvent(order.getId(), next.getId(), next.getPlate(), route.hours()));
        return assignmentMapper.toDto(a);
    }

    /** Yükləmə nöqtəsindən çatdırılmaya gedən hissə (yükləmə nöqtəsi yoxdursa null). Bütün maşınlar üçün eynidir, bir dəfə hesablanır. */
    private RouteInfo loadedLeg(Order order) {
        return order.hasPickup()
                ? routes.route(order.getPickupLat(), order.getPickupLon(), order.getDestLat(), order.getDestLon())
                : null;
    }

    /** Tam yol: maşın → yükləmə → çatdırılma. Yükləmə nöqtəsi yoxdursa: maşın → çatdırılma. */
    private RouteInfo trip(Truck t, Order order, RouteInfo loadedLeg) {
        if (loadedLeg == null) {
            return routes.route(t.getLat(), t.getLon(), order.getDestLat(), order.getDestLon());
        }
        RouteInfo toLoad = routes.route(t.getLat(), t.getLon(), order.getPickupLat(), order.getPickupLon());
        return new RouteInfo(toLoad.km() + loadedLeg.km(), toLoad.hours() + loadedLeg.hours());
    }

    private List<Candidate> rank(Order order) {
        // 1) PostGIS: tip, tutum və status üzrə ən yaxın namizədlər
        // Maşın əvvəl YÜKLƏMƏ nöqtəsinə gələcək (yoxdursa birbaşa çatdırılma ünvanına), ona görə ən yaxını oradan axtarılır
        List<Truck> nearest = trucks.findNearestFree(order.getCargoType().truckTypeNames(), order.getTons(),
                order.loadLon(), order.loadLat(), props.candidateLimit());
        RouteInfo loadedLeg = loadedLeg(order);

        List<Candidate> result = new ArrayList<>();
        for (Truck t : nearest) {
            // 2) Əlavə sərt şərtlər (GPS yaşı, sonra: sürücü iş saatı, sığorta, texniki baxış...)
            boolean rejected = filters.stream().anyMatch(f -> f.reject(t, order).isPresent());
            if (rejected) continue;

            // 3) Real marşrut və 4) xal
            RouteInfo r = trip(t, order, loadedLeg);
            double waste = scorer.wasteTons(t, order);
            List<String> reasons = new ArrayList<>();
            reasons.add("Tip və tutum uyğundur");
            if (loadedLeg != null) {
                reasons.add("Yükləmə nöqtəsinə " + String.format("%.0f", GeoUtils.haversineKm(
                        t.getLat(), t.getLon(), order.getPickupLat(), order.getPickupLon())) + " km (düz xətt)");
            }
            if (waste <= 5) reasons.add("Boş tutum itkisi azdır (" + String.format("%.0f", waste) + " t)");
            reasons.add(order.isUrgent() ? "Təcili rejim: gəliş vaxtı üstün tutuldu" : "Ümumi xərc və tutum itkisi nəzərə alındı");
            result.add(new Candidate(t.getId(), t.getPlate(), t.getType().name(), t.getCapacityTons(),
                    r.km(), r.hours(), scorer.cost(t, r), scorer.score(order, t, r), reasons));
        }
        result.sort(Comparator.comparingDouble(Candidate::getScore));
        return result;
    }
}
