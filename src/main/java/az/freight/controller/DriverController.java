package az.freight.controller;

import az.freight.dto.ApiResponse;
import az.freight.dto.DriverCreateDto;
import az.freight.dto.DriverDto;
import az.freight.dto.DriverUpdateDto;
import az.freight.dto.PinResetDto;
import az.freight.factory.PageRequestFactory;
import az.freight.service.DriverService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Dispetçer tərəfdən sürücü idarəsi. */
@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DriverDto>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String sort) {
        var req = PageRequestFactory.of(page, size, sort, Set.of("id", "fullName", "createdAt"), "id", Sort.Direction.DESC);
        Page<DriverDto> result = service.list(req);
        return ResponseEntity.ok(ApiResponse.page(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DriverDto>> create(@Valid @RequestBody DriverCreateDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Created", service.create(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DriverDto>> update(@PathVariable Long id, @Valid @RequestBody DriverUpdateDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Updated", service.update(id, dto)));
    }

    @PutMapping("/{id}/pin")
    public ResponseEntity<ApiResponse<Void>> resetPin(@PathVariable Long id, @Valid @RequestBody PinResetDto dto) {
        service.resetPin(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("OK", null));
    }
}
