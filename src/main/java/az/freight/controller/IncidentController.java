package az.freight.controller;

import az.freight.dto.ApiResponse;
import az.freight.dto.IncidentAssignRequest;
import az.freight.dto.IncidentDto;
import az.freight.enums.IncidentStatus;
import az.freight.factory.PageRequestFactory;
import az.freight.service.IncidentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Dispetçer üçün nasazlıqlar: siyahı və əvəzedici maşını əllə təyin etmək. */
@RestController
@RequestMapping("/api/incident")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<IncidentDto>>> getAll(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String sort) {
        var req = PageRequestFactory.of(page, size, sort, Set.of("id", "createdAt", "status"), "id", Sort.Direction.DESC);
        Page<IncidentDto> result = service.list(status, req);
        return ResponseEntity.ok(ApiResponse.page(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IncidentDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.get(id)));
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<IncidentDto>> assign(
            @PathVariable Long id, @Valid @RequestBody IncidentAssignRequest body,
            @RequestHeader(value = "X-User", required = false) String user) {
        return ResponseEntity.ok(ApiResponse.ok("Assigned", service.assignManually(id, body.getTruckId(), user)));
    }
}
