package az.freight.controller;

import az.freight.dto.*;
import az.freight.enums.CustomerStatus;
import az.freight.service.CustomerService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Set;
import az.freight.factory.PageRequestFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Dispetçer tərəfi: müştəri reyestri. */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerDto>>> list(
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) String sort) {
        var req = PageRequestFactory.of(page, size, sort,
                Set.of("id", "name", "createdAt", "status"), "createdAt", Sort.Direction.DESC);
        Page<CustomerDto> r = service.list(status, req);
        return ResponseEntity.ok(ApiResponse.page(r.getContent(), r.getNumber(), r.getSize(), r.getTotalElements()));
    }

    @GetMapping("/pending-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> pending() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", service.pendingCount())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.get(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerDto>> create(@Valid @RequestBody CustomerRegisterDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Created", service.createByDispatcher(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDto>> update(@PathVariable Long id,
                                                           @Valid @RequestBody CustomerAdminUpdateDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Updated", service.adminUpdate(id, dto)));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable Long id,
                                                           @Valid @RequestBody PasswordResetDto dto) {
        service.resetPassword(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("OK", null));
    }

    @GetMapping("/{id}/orders")
    public ResponseEntity<ApiResponse<List<OrderDto>>> orders(@PathVariable Long id,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "50") int size) {
        var req = PageRequestFactory.of(page, size, null, Set.of("createdAt"), "createdAt", Sort.Direction.DESC);
        Page<OrderDto> r = service.ordersOf(id, req);
        return ResponseEntity.ok(ApiResponse.page(r.getContent(), r.getNumber(), r.getSize(), r.getTotalElements()));
    }
}
