package az.freight.controller;

import az.freight.dto.ApiResponse;
import az.freight.dto.OrderDto;
import az.freight.dto.PositionUpdateDto;
import az.freight.dto.TruckDto;
import az.freight.dto.TruckStatusUpdateDto;
import az.freight.enums.TruckStatus;
import az.freight.factory.PageRequestFactory;
import az.freight.service.OrderService;
import az.freight.service.TruckService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/truck")
@RequiredArgsConstructor
public class TruckController {

    private final TruckService service;
    private final OrderService orders;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TruckDto>>> getAll(
            @RequestParam(required = false) TruckStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String sort) {
        var req = PageRequestFactory.of(
                page, size, sort,
                Set.of("id", "plate", "capacityTons", "status", "brand", "manufactureYear", "insuranceExpiry", "techInspectionExpiry"),
                "id", Sort.Direction.ASC);
        Page<TruckDto> result = service.list(status, req);
        return ResponseEntity.ok(ApiResponse.page(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TruckDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.get(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TruckDto>> create(@Valid @RequestBody TruckDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Created", service.create(dto)));
    }

    /** Maşın pasportunu redaktə edir (status və GPS istisna). */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TruckDto>> update(@PathVariable Long id, @Valid @RequestBody TruckDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Updated", service.update(id, dto)));
    }

    /** GPS cihazından və ya sürücü tətbiqindən gələn son məkan. */
    @PutMapping("/{id}/position")
    public ResponseEntity<ApiResponse<TruckDto>> updatePosition(@PathVariable Long id,
                                                                @Valid @RequestBody PositionUpdateDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Updated", service.updatePosition(id, dto)));
    }

    /** Sürücünün aktiv reysləri (təyinat cədvəlinə görə) — dispetçerin təyin etdiyi reyslər də buradadır. */
    @GetMapping("/{id}/orders")
    public ResponseEntity<ApiResponse<List<OrderDto>>> activeOrders(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(orders.activeForTruck(id)));
    }

    /** Sürücü maşını xidmətdən çıxarır (qəza/nasazlıq) və ya xidmətə qaytarır. */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TruckDto>> updateStatus(@PathVariable Long id,
                                                              @Valid @RequestBody TruckStatusUpdateDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Updated", service.updateStatus(id, dto)));
    }
}
