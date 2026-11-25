package az.freight.service;

import az.freight.dto.AssignmentDto;
import az.freight.dto.BreakdownRequest;
import az.freight.dto.DriverOrderDto;
import az.freight.dto.IncidentDto;
import az.freight.dto.OrderStatusUpdateDto;
import az.freight.entity.Assignment;
import az.freight.entity.Driver;
import az.freight.entity.Incident;
import az.freight.entity.Order;
import az.freight.entity.Truck;
import az.freight.enums.AssignmentKind;
import az.freight.enums.OrderStatus;
import az.freight.enums.TruckStatus;
import az.freight.exception.ApiException;
import az.freight.exception.ResourceNotFoundException;
import az.freight.mapper.AssignmentMapper;
import az.freight.mapper.OrderMapper;
import az.freight.repository.AssignmentRepository;
import az.freight.repository.OrderRepository;
import az.freight.repository.TruckRepository;
import az.freight.util.GeoUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sürücü portalının bütün əməliyyatları. Hər əməliyyat yalnız giriş etmiş sürücünün öz maşını üçündür. */
@Service
@RequiredArgsConstructor
public class DriverPortalService {

    private static final int POOL_LIMIT = 100;

    private final OrderRepository orders;
    private final TruckRepository trucks;
    private final AssignmentRepository assignments;
    private final OrderMapper orderMapper;
    private final AssignmentMapper assignmentMapper;
    private final OrderService orderService;
    private final DispatchService dispatch;
    private final IncidentService incidents;
    private final OrderEventService eventLog;

    /** Mənim reyslərim: aktiv reyslər + öz nasazlığım bağlanmayıbsa həmin sifariş. */
    @Transactional(readOnly = true)
    public List<DriverOrderDto> myOrders(Driver driver) {
        Truck truck = requireTruck(driver);
        Map<Long, DriverOrderDto> result = new LinkedHashMap<>();
        for (var o : orderService.activeForTruck(truck.getId())) {
            Order order = orders.findById(o.getId()).orElse(null);
            if (order != null) {
                result.put(order.getId(), toView(order, truck, false));
            }
        }
        for (Incident i : incidents.openForTruck(truck.getId())) {
            if (!result.containsKey(i.getOrderId())) {
                orders.findById(i.getOrderId()).ifPresent(o -> result.put(o.getId(), toView(o, truck, false)));
            }
        }
        return new ArrayList<>(result.values());
    }

    /** Hovuzdakı, mənim maşınıma uyğun boş yüklər (yaxından uzağa). */
    @Transactional(readOnly = true)
    public List<DriverOrderDto> available(Driver driver) {
        Truck truck = requireTruck(driver);
        if (truck.getStatus() != TruckStatus.FREE) {
            return List.of();
        }
        List<DriverOrderDto> out = new ArrayList<>();
        for (Order o : orders.findByStatus(OrderStatus.CREATED, PageRequest.of(0, POOL_LIMIT)).getContent()) {
            if (!suits(truck, o)) continue;
            DriverOrderDto dto = new DriverOrderDto();
            dto.setOrder(orderMapper.toDto(o));
            dto.setDistanceKm(GeoUtils.haversineKm(truck.getLat(), truck.getLon(), o.loadLat(), o.loadLon()));
            out.add(dto);
        }
        out.sort(Comparator.comparingDouble(DriverOrderDto::getDistanceKm));
        return out;
    }

    @Transactional(readOnly = true)
    public DriverOrderDto detail(Driver driver, Long orderId) {
        Truck truck = requireTruck(driver);
        Order order = orders.findById(orderId).orElseThrow(() -> ResourceNotFoundException.of("Sifariş", orderId));
        boolean mine = assignments.findByOrderId(orderId).stream().anyMatch(a -> a.getTruckId().equals(truck.getId()));
        if (!mine && !(order.getStatus() == OrderStatus.CREATED && suits(truck, order))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bu sifariş sizə aid deyil");
        }
        DriverOrderDto dto = toView(order, truck, true);
        if (order.getStatus() == OrderStatus.CREATED) {
            dto.setDistanceKm(GeoUtils.haversineKm(truck.getLat(), truck.getLon(), order.loadLat(), order.loadLon()));
        }
        return dto;
    }

    @Transactional
    public AssignmentDto accept(Driver driver, Long orderId) {
        Truck truck = requireTruck(driver);
        return dispatch.claim(orderId, truck.getId(), driver.getFullName());
    }

    @Transactional
    public void start(Driver driver, Long orderId) {
        requireLatestMine(driver, orderId);
        orderService.updateStatus(orderId, status(OrderStatus.IN_TRANSIT, null), driver.getFullName());
    }

    @Transactional
    public void deliver(Driver driver, Long orderId) {
        requireLatestMine(driver, orderId);
        orderService.updateStatus(orderId, status(OrderStatus.DELIVERED, null), driver.getFullName());
    }

    /** Sürücü yükdən imtina edir: sifariş hovuza qayıdır, maşın boşalır. */
    @Transactional
    public void release(Driver driver, Long orderId, String reason) {
        requireLatestMine(driver, orderId);
        orderService.updateStatus(orderId, status(OrderStatus.CREATED, reason), driver.getFullName());
    }

    @Transactional
    public IncidentDto reportBreakdown(Driver driver, Long orderId, BreakdownRequest req) {
        requireTruck(driver);
        return incidents.report(driver, orderId, req);
    }

    @Transactional
    public void cargoPickedUp(Driver driver, Long orderId) {
        requireTruck(driver);
        incidents.markCargoPickedUp(driver, orderId);
    }

    /** Maşın təmir olunub: açıq nasazlıq yoxdursa yenidən boş olur. */
    @Transactional
    public void truckRepaired(Driver driver) {
        Truck truck = trucks.findForUpdate(requireTruck(driver).getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Maşın", driver.getTruckId()));
        if (truck.getStatus() != TruckStatus.MAINTENANCE) {
            throw new ApiException(HttpStatus.CONFLICT, "Maşın texniki xidmətdə deyil");
        }
        if (incidents.truckHasOpenIncident(truck.getId())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Yük hələ götürülməyib. Əvvəl əvəzedici maşın yükü götürməlidir");
        }
        truck.setStatus(TruckStatus.FREE);
    }

    // ------------------------------------------------------------------ köməkçilər

    private Truck requireTruck(Driver d) {
        if (d.getTruckId() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "Sizə maşın bağlanmayıb, dispetçerlə əlaqə saxlayın");
        }
        return trucks.findById(d.getTruckId()).orElseThrow(() -> ResourceNotFoundException.of("Maşın", d.getTruckId()));
    }

    /** Reysin ƏN SON təyinatı mənim maşınımdır. */
    private void requireLatestMine(Driver d, Long orderId) {
        Truck truck = requireTruck(d);
        Assignment latest = assignments.findTopByOrderIdOrderByIdDesc(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Bu reys sizin deyil"));
        if (!latest.getTruckId().equals(truck.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bu reys artıq sizin maşında deyil");
        }
    }

    private static OrderStatusUpdateDto status(OrderStatus s, String reason) {
        OrderStatusUpdateDto dto = new OrderStatusUpdateDto();
        dto.setStatus(s);
        dto.setReason(reason);
        return dto;
    }

    private static boolean suits(Truck t, Order o) {
        return o.getCargoType().truckTypeNames().contains(t.getType().name()) && t.getCapacityTons() >= o.getTons();
    }

    private DriverOrderDto toView(Order order, Truck truck, boolean withEvents) {
        DriverOrderDto dto = new DriverOrderDto();
        dto.setOrder(orderMapper.toDto(order));
        Assignment latest = assignments.findTopByOrderIdOrderByIdDesc(order.getId()).orElse(null);
        dto.setAssignment(assignmentMapper.toDto(latest));
        dto.setMine(latest != null && latest.getTruckId().equals(truck.getId()));
        dto.setRescue(dto.isMine() && latest.getKind() == AssignmentKind.REPLACEMENT);
        dto.setIncident(incidents.latestForOrder(order.getId()).orElse(null));
        if (withEvents) {
            dto.setEvents(eventLog.list(order.getId()));
        }
        return dto;
    }
}
