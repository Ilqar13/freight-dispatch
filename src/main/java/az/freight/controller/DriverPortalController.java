package az.freight.controller;

import az.freight.config.DriverAuthInterceptor;
import az.freight.dto.ApiResponse;
import az.freight.dto.AssignmentDto;
import az.freight.dto.BreakdownRequest;
import az.freight.dto.DriverDto;
import az.freight.dto.DriverLoginRequest;
import az.freight.dto.DriverLoginResponse;
import az.freight.dto.DriverOrderDto;
import az.freight.dto.IncidentDto;
import az.freight.entity.Driver;
import az.freight.service.DriverAuthService;
import az.freight.service.DriverPortalService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestAttribute;

/** Sürücü portalı API-si. Giriş istisna olmaqla hamısı X-Driver-Token tələb edir. */
@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
public class DriverPortalController {

    private final DriverAuthService auth;
    private final DriverPortalService portal;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<DriverLoginResponse>> login(@Valid @RequestBody DriverLoginRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(auth.login(req)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader(DriverAuthInterceptor.HEADER) String token) {
        auth.logout(token);
        return ResponseEntity.ok(ApiResponse.ok("OK", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<DriverDto>> me(@RequestAttribute(DriverAuthInterceptor.ATTR) Driver driver) {
        return ResponseEntity.ok(ApiResponse.ok(auth.toDto(driver)));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<DriverOrderDto>>> mine(
            @RequestAttribute(DriverAuthInterceptor.ATTR) Driver driver) {
        return ResponseEntity.ok(ApiResponse.ok(portal.myOrders(driver)));
    }

    @GetMapping("/orders/available")
    public ResponseEntity<ApiResponse<List<DriverOrderDto>>> available(
            @RequestAttribute(DriverAuthInterceptor.ATTR) Driver driver) {
        return ResponseEntity.ok(ApiResponse.ok(portal.available(driver)));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<DriverOrderDto>> detail(
            @RequestAttribute(DriverAuthInterceptor.ATTR) Driver driver, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(portal.detail(driver, id)));
    }

    @PostMapping("/orders/{id}/accept")
    public ResponseEntity<ApiResponse<AssignmentDto>> accept(
            @RequestAttribute(DriverAuthInterceptor.ATTR) Driver driver, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Accepted", portal.accept(driver, id)));
    }

    @PostMapping("/orders/{id}/start")
    public ResponseEntity<ApiResponse<Void>> start(
            @RequestAttribute(DriverAuthInterceptor.ATTR) Driver driver, @PathVariable Long id) {
        portal.start(driver, id);
        return ResponseEntity.ok(ApiResponse.ok("OK", null));
    }

    @PostMapping("/orders/{id}/deliver")
    public ResponseEntity<ApiResponse<Void>> deliver(
            @RequestAttribute(DriverAuthInterceptor.ATTR) Driver driver, @PathVariable Long id) {
        portal.deliver(driver, id);
        return ResponseEntity.ok(ApiResponse.ok("OK", null));
    }

    @PostMapping("/orders/{id}/release")
    public ResponseEntity<ApiResponse<Void>> release(
            @RequestAttribute(DriverAuthInterceptor.ATTR) Driver driver, @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        portal.release(driver, id, reason);
        return ResponseEntity.ok(ApiResponse.ok("OK", null));
    }

    /** Texniki problem: maşın dayanır, sistem avtomatik əvəzedici boş maşın çağırır. */
    @PostMapping("/orders/{id}/breakdown")
    public ResponseEntity<ApiResponse<IncidentDto>> breakdown(
            @RequestAttribute(DriverAuthInterceptor.ATTR) Driver driver, @PathVariable Long id,
            @Valid @RequestBody BreakdownRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Reported", portal.reportBreakdown(driver, id, req)));
    }

    /** Əvəzedici sürücü nasazlıq nöqtəsində yükü götürdü. */
    @PostMapping("/orders/{id}/pickup")
    public ResponseEntity<ApiResponse<Void>> pickup(
            @RequestAttribute(DriverAuthInterceptor.ATTR) Driver driver, @PathVariable Long id) {
        portal.cargoPickedUp(driver, id);
        return ResponseEntity.ok(ApiResponse.ok("OK", null));
    }

    @PostMapping("/truck/repaired")
    public ResponseEntity<ApiResponse<Void>> repaired(@RequestAttribute(DriverAuthInterceptor.ATTR) Driver driver) {
        portal.truckRepaired(driver);
        return ResponseEntity.ok(ApiResponse.ok("OK", null));
    }
}
