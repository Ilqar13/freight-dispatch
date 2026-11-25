package az.freight.controller;

import az.freight.config.CustomerAuthInterceptor;
import az.freight.dto.*;
import az.freight.entity.Customer;
import az.freight.mapper.CustomerMapper;
import az.freight.service.AssistantService;
import az.freight.service.CustomerAuthService;
import az.freight.service.CustomerService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Müştəri portalı. Qeydiyyat və giriş istisna olmaqla hamısı X-Customer-Token tələb edir. */
@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerPortalController {

    private final CustomerAuthService auth;
    private final CustomerService service;
    private final CustomerMapper mapper;
    private final AssistantService assistant;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<CustomerDto>> register(@Valid @RequestBody CustomerRegisterDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Qeydiyyat qəbul olundu. Dispetçer təsdiq etdikdən sonra sifariş verə bilərsiniz",
                service.register(dto)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<CustomerLoginResponse>> login(@Valid @RequestBody CustomerLoginRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(auth.login(req)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader(CustomerAuthInterceptor.HEADER) String token) {
        auth.logout(token);
        return ResponseEntity.ok(ApiResponse.ok("OK", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CustomerDto>> me(@RequestAttribute(CustomerAuthInterceptor.ATTR) Customer c) {
        return ResponseEntity.ok(ApiResponse.ok(mapper.toDto(c)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<CustomerDto>> updateMe(
            @RequestAttribute(CustomerAuthInterceptor.ATTR) Customer c, @Valid @RequestBody CustomerProfileDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Updated", service.updateProfile(c, dto)));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderDto>>> orders(
            @RequestAttribute(CustomerAuthInterceptor.ATTR) Customer c,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        Page<OrderDto> r = service.myOrders(c, page, size);
        return ResponseEntity.ok(ApiResponse.page(r.getContent(), r.getNumber(), r.getSize(), r.getTotalElements()));
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<OrderDto>> create(
            @RequestAttribute(CustomerAuthInterceptor.ATTR) Customer c, @Valid @RequestBody OrderDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Created", service.createOrder(c, dto)));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<OrderDto>> order(
            @RequestAttribute(CustomerAuthInterceptor.ATTR) Customer c, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.myOrder(c, id)));
    }

    @GetMapping("/orders/{id}/events")
    public ResponseEntity<ApiResponse<List<OrderEventDto>>> events(
            @RequestAttribute(CustomerAuthInterceptor.ATTR) Customer c, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.myOrderEvents(c, id)));
    }

    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderDto>> cancel(
            @RequestAttribute(CustomerAuthInterceptor.ATTR) Customer c, @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return ResponseEntity.ok(ApiResponse.ok("Cancelled", service.cancelOrder(c, id, reason)));
    }

    /** Müştəri köməkçisi (Claude): sərbəst mətnlə sifariş, status və ləğv. */
    @PostMapping("/assistant")
    public ResponseEntity<ApiResponse<AssistantResponse>> assistant(
            @RequestAttribute(CustomerAuthInterceptor.ATTR) Customer c, @Valid @RequestBody AssistantRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(assistant.chat(c, req)));
    }
}
