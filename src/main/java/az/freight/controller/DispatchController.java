package az.freight.controller;

import az.freight.dto.ApiResponse;
import az.freight.dto.AssignRequest;
import az.freight.dto.AssignmentDto;
import az.freight.dto.Candidate;
import az.freight.dto.TransferRequest;
import az.freight.service.DispatchService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order/{orderId}")
@RequiredArgsConstructor
public class DispatchController {

    private final DispatchService service;

    /** Namizəd maşınlar və seçim səbəbləri (heç nə dəyişmir). */
    @PostMapping("/dispatch")
    public ResponseEntity<ApiResponse<List<Candidate>>> preview(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.ok(service.preview(orderId)));
    }

    /** Avtomatik seçim (body boş) və ya operatorun seçdiyi maşın ({"truckId": 3}). */
    @PostMapping("/assign")
    public ResponseEntity<ApiResponse<AssignmentDto>> assign(
            @PathVariable Long orderId,
            @RequestBody(required = false) AssignRequest body,
            @RequestHeader(value = "X-User", required = false) String user) {
        Long truckId = body == null ? null : body.getTruckId();
        return ResponseEntity.ok(ApiResponse.ok("Assigned", service.assign(orderId, truckId, user)));
    }

    /** Aktiv reysi başqa sürücüyə/maşına ötürür (qəza zamanı yükü təhvil vermək üçün). */
    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<AssignmentDto>> transfer(
            @PathVariable Long orderId,
            @Valid @RequestBody TransferRequest body,
            @RequestHeader(value = "X-User", required = false) String user) {
        return ResponseEntity.ok(ApiResponse.ok("Transferred", service.transfer(orderId, body, user)));
    }
}
