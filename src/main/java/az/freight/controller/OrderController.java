package az.freight.controller;

import az.freight.dto.ApiResponse;
import az.freight.dto.OrderDto;
import az.freight.dto.OrderStatusUpdateDto;
import az.freight.factory.PageRequestFactory;
import az.freight.dto.OrderEventDto;
import az.freight.service.OrderEventService;
import az.freight.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;
    private final OrderEventService eventService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderDto>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String sort) {
        var req = PageRequestFactory.of(
                page, size, sort,
                Set.of("id", "createdAt", "tons", "status"),
                "createdAt", Sort.Direction.DESC);
        Page<OrderDto> result = service.list(req);
        return ResponseEntity.ok(ApiResponse.page(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.get(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderDto>> create(@Valid @RequestBody OrderDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Created", service.create(dto)));
    }

    /** Sürücü "yola çıxdı / çatdırdı" edir və ya sifarişi ləğv edir/hovuza qaytarır. */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderDto>> updateStatus(@PathVariable Long id,
                                                              @Valid @RequestBody OrderStatusUpdateDto dto,
                                                              @RequestHeader(value = "X-User", required = false) String user) {
        return ResponseEntity.ok(ApiResponse.ok("Updated", service.updateStatus(id, dto, user)));
    }

    /** Sifarişin tarixçəsi (ən yenisi əvvəldə). */
    @GetMapping("/{id}/events")
    public ResponseEntity<ApiResponse<List<OrderEventDto>>> events(@PathVariable Long id) {
        service.get(id);
        return ResponseEntity.ok(ApiResponse.ok(eventService.list(id)));
    }
}
