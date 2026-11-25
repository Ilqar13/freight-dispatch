package az.freight.service;

import az.freight.dto.OrderDto;
import az.freight.dto.OrderStatusUpdateDto;
import az.freight.entity.Assignment;
import az.freight.entity.Customer;
import az.freight.enums.CustomerStatus;
import az.freight.repository.CustomerRepository;
import az.freight.entity.Order;
import az.freight.enums.CargoType;
import az.freight.enums.OrderEventType;
import az.freight.enums.OrderStatus;
import az.freight.enums.TruckStatus;
import az.freight.exception.ApiException;
import az.freight.exception.ResourceNotFoundException;
import az.freight.mapper.OrderMapper;
import az.freight.repository.AssignmentRepository;
import az.freight.repository.OrderRepository;
import az.freight.repository.TruckRepository;
import java.time.Instant;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final AssignmentRepository assignments;
    private final TruckRepository trucks;
    private final IncidentService incidents;
    private final CustomerRepository customers;
    private final OrderEventService eventLog;

    /** Dispetçer sifariş yaradır (customerId ixtiyaridir, verilibsə müştəri aktiv olmalıdır). */
    @Transactional
    public OrderDto create(OrderDto dto) {
        if (dto.getCustomerId() != null) {
            Customer c = customers.findById(dto.getCustomerId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Müştəri", dto.getCustomerId()));
            if (c.getStatus() != CustomerStatus.ACTIVE) {
                throw new ApiException(HttpStatus.CONFLICT, "Müştəri aktiv deyil (təsdiq gözləyir və ya bloklanıb)");
            }
        }
        return doCreate(dto, dto.getCustomerId());
    }

    /** Müştəri portalından: müştəri məlumatı serverdən götürülür, müştəri aktiv olmalıdır. */
    @Transactional
    public OrderDto createForCustomer(OrderDto dto, Customer c) {
        if (c.getStatus() == CustomerStatus.PENDING) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Hesabınız hələ təsdiqlənməyib. Dispetçer təsdiq etdikdən sonra sifariş verə bilərsiniz");
        }
        if (c.getStatus() != CustomerStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Hesab aktiv deyil");
        }
        dto.setCustomerName(c.getName());
        if (dto.getContactName() == null || dto.getContactName().isBlank()) dto.setContactName(c.getContactName());
        if (dto.getContactPhone() == null || dto.getContactPhone().isBlank()) dto.setContactPhone(c.getPhone());
        if (dto.getContactEmail() == null || dto.getContactEmail().isBlank()) dto.setContactEmail(c.getEmail());
        dto.setTaxId(c.getTaxId() == null ? "" : c.getTaxId());
        return doCreate(dto, c.getId());
    }

    private OrderDto doCreate(OrderDto dto, Long customerId) {
        validate(dto);
        Order entity = mapper.toEntity(dto);
        entity.setCustomerId(customerId);
        Order saved = repository.save(entity);
        // İnsana görünən nömrə: id yalnız saxlanandan sonra məlumdur
        saved.setOrderNo(String.format("166-%d-%05d", Year.now().getValue(), saved.getId()));
        eventLog.log(saved.getId(), OrderEventType.CREATED, "Sifariş yaradıldı (" + saved.getOrderNo() + ")", "SYSTEM");
        return mapper.toDto(saved);
    }

    /** Sahələrarası biznes yoxlamaları (tək sahə yoxlamaları DTO annotasiyalarındadır). */
    private void validate(OrderDto d) {
        if ((d.getPickupLat() == null) != (d.getPickupLon() == null)) {
            throw bad("Yükləmə nöqtəsinin enlik və uzunluğu birlikdə verilməlidir");
        }
        if (d.getPickupLat() != null && d.getPickupLat() == d.getLat() && d.getPickupLon() == d.getLon()) {
            throw bad("Yükləmə və çatdırılma nöqtəsi eyni ola bilməz");
        }
        if (d.getPickupFrom() != null && d.getPickupTo() != null && d.getPickupTo().isBefore(d.getPickupFrom())) {
            throw bad("Yükləmə pəncərəsinin sonu başlanğıcdan əvvəl ola bilməz");
        }
        if (d.getDeliveryFrom() != null && d.getDeliveryTo() != null && d.getDeliveryTo().isBefore(d.getDeliveryFrom())) {
            throw bad("Çatdırılma pəncərəsinin sonu başlanğıcdan əvvəl ola bilməz");
        }
        Instant loadStart = d.getPickupFrom();
        Instant deliverEnd = d.getDeliveryTo();
        if (loadStart != null && deliverEnd != null && deliverEnd.isBefore(loadStart)) {
            throw bad("Çatdırılma müddəti yükləmə vaxtından əvvəl ola bilməz");
        }
        if (d.isHazardous() && (d.getUnNumber() == null || d.getUnNumber().isBlank())) {
            throw bad("Təhlükəli yük (ADR) üçün UN nömrəsi tələb olunur");
        }
        if (d.getTempMin() != null && d.getTempMax() != null && d.getTempMin() > d.getTempMax()) {
            throw bad("Minimum temperatur maksimumdan böyük ola bilməz");
        }
        if (d.getCargoType() == CargoType.FROZEN_FOOD && d.getTempMin() == null && d.getTempMax() == null) {
            throw bad("Dondurulmuş qida üçün temperatur diapazonu tələb olunur");
        }
        if (d.getPackagesCount() != null && d.getPackageType() == null) {
            throw bad("Qablaşdırma sayı verilibsə qablaşdırma növü də seçilməlidir");
        }
    }

    private static ApiException bad(String msg) {
        return new ApiException(HttpStatus.BAD_REQUEST, msg);
    }

    @Transactional(readOnly = true)
    public OrderDto get(Long id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> ResourceNotFoundException.of("Sifariş", id));
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> list(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    /**
     * Sürücü və ya dispetçer sifarişin statusunu dəyişir:
     * yola çıxdı, çatdırdı, ləğv etdi və ya yükü hovuza qaytardı.
     * Sifariş bağlananda (DELIVERED/CANCELLED) və ya hovuza qaytarılanda
     * aktiv təyinatın maşını boşaldılır.
     */
    @Transactional
    public OrderDto updateStatus(Long id, OrderStatusUpdateDto dto) {
        return updateStatus(id, dto, null);
    }

    @Transactional
    public OrderDto updateStatus(Long id, OrderStatusUpdateDto dto, String actor) {
        Order order = repository.findForUpdate(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Sifariş", id));
        OrderStatus current = order.getStatus();
        OrderStatus target = dto.getStatus();

        if (current == target) {
            return mapper.toDto(order);
        }
        if (current == OrderStatus.DELIVERED || current == OrderStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "Bağlanmış sifarişin statusu dəyişdirilə bilməz");
        }
        if (!isAllowed(current, target)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Status dəyişikliyi icazəli deyil: " + current + " → " + target);
        }

        if (target == OrderStatus.IN_TRANSIT && incidents.hasOpen(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Əvvəl nasazlıq nöqtəsində yükü götürdüyünüzü təsdiqləyin");
        }
        if (target == OrderStatus.DELIVERED || target == OrderStatus.CANCELLED || target == OrderStatus.CREATED) {
            releaseTruck(order.getId());
            incidents.closeOpenForOrder(id, null);
        }
        order.setStatus(target);

        String who = actor == null || actor.isBlank() ? "SYSTEM" : actor;
        String why = dto.getReason() == null || dto.getReason().isBlank() ? "" : " (" + dto.getReason().trim() + ")";
        switch (target) {
            case IN_TRANSIT -> eventLog.log(id, OrderEventType.STARTED, "Yola çıxdı" + why, who);
            case DELIVERED -> eventLog.log(id, OrderEventType.DELIVERED, "Çatdırıldı" + why, who);
            case CANCELLED -> eventLog.log(id, OrderEventType.CANCELLED, "Sifariş ləğv edildi" + why, who);
            case CREATED -> eventLog.log(id, OrderEventType.RELEASED, "Yük hovuza qaytarıldı" + why, who);
            default -> { }
        }

        log.info("Sifariş #{} statusu {} → {}{}", id, current, target,
                dto.getReason() == null || dto.getReason().isBlank() ? "" : " (" + dto.getReason().trim() + ")");
        return mapper.toDto(order);
    }

    /**
     * Bu maşına aktiv təyin olunmuş sifarişlər — sürücü portalı üçün.
     * Yalnız sifarişin ƏN SON təyinatı bu maşındırsa göstərilir, yəni yük başqa
     * sürücüyə ötürüldükdən sonra köhnə maşının siyahısında qalmır.
     */
    @Transactional(readOnly = true)
    public List<OrderDto> activeForTruck(Long truckId) {
        Map<Long, Order> active = new LinkedHashMap<>();
        for (Assignment a : assignments.findByTruckIdOrderByIdDesc(truckId)) {
            if (active.containsKey(a.getOrderId())) {
                continue;
            }
            Assignment latest = assignments.findTopByOrderIdOrderByIdDesc(a.getOrderId()).orElse(null);
            if (latest == null || !latest.getTruckId().equals(truckId)) {
                continue;
            }
            Order order = repository.findById(a.getOrderId()).orElse(null);
            if (order == null) {
                continue;
            }
            if (order.getStatus() == OrderStatus.TRUCK_ASSIGNED || order.getStatus() == OrderStatus.IN_TRANSIT
                    || order.getStatus() == OrderStatus.BREAKDOWN) {
                active.put(order.getId(), order);
            }
        }
        return active.values().stream().map(mapper::toDto).toList();
    }

    /** Hansı statusdan hansına keçidə icazə var. */
    private boolean isAllowed(OrderStatus current, OrderStatus target) {
        return switch (current) {
            case CREATED -> target == OrderStatus.CANCELLED;
            case TRUCK_ASSIGNED -> target == OrderStatus.IN_TRANSIT
                    || target == OrderStatus.CANCELLED
                    || target == OrderStatus.CREATED;
            case IN_TRANSIT -> target == OrderStatus.DELIVERED
                    || target == OrderStatus.CANCELLED
                    || target == OrderStatus.CREATED;
            // Əvəzedici maşın yalnız nasazlıq axınından təyin olunur; dispetçer yalnız ləğv edə bilər
            case BREAKDOWN -> target == OrderStatus.CANCELLED;
            case DELIVERED, CANCELLED -> false;
        };
    }

    /** Aktiv təyinatın maşınını boşaldır; texniki xidmətdə olan maşın toxunulmur. */
    private void releaseTruck(Long orderId) {
        assignments.findTopByOrderIdOrderByIdDesc(orderId).ifPresent(a ->
                trucks.findForUpdate(a.getTruckId()).ifPresent(t -> {
                    if (t.getStatus() == TruckStatus.BUSY) {
                        t.setStatus(TruckStatus.FREE);
                    }
                }));
    }
}
