package az.freight.service;

import az.freight.dto.PositionUpdateDto;
import az.freight.dto.TruckDto;
import az.freight.dto.TruckStatusUpdateDto;
import az.freight.entity.Driver;
import az.freight.entity.Truck;
import az.freight.enums.TruckStatus;
import az.freight.enums.TruckType;
import az.freight.exception.ApiException;
import az.freight.exception.ResourceNotFoundException;
import az.freight.mapper.TruckMapper;
import az.freight.repository.DriverRepository;
import az.freight.repository.TruckRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TruckService {

    private final TruckRepository repository;
    private final DriverRepository drivers;
    private final TruckMapper mapper;

    @Transactional(readOnly = true)
    public Page<TruckDto> list(TruckStatus status, Pageable pageable) {
        Page<Truck> page = status == null ? repository.findAll(pageable) : repository.findByStatus(status, pageable);
        Map<Long, Driver> byTruck = driversByTruck(page.getContent().stream().map(Truck::getId).toList());
        return page.map(t -> withDriver(mapper.toDto(t), byTruck.get(t.getId())));
    }

    @Transactional(readOnly = true)
    public TruckDto get(Long id) {
        Truck t = find(id);
        return withDriver(mapper.toDto(t), driversByTruck(List.of(id)).get(id));
    }

    @Transactional
    public TruckDto create(TruckDto dto) {
        validate(dto);
        String plate = dto.getPlate().trim().toUpperCase();
        if (repository.findByPlate(plate).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Bu dövlət nömrəsi ilə maşın artıq var: " + plate);
        }
        if (dto.getVin() != null && !dto.getVin().isBlank() && repository.existsByVin(dto.getVin().trim().toUpperCase())) {
            throw new ApiException(HttpStatus.CONFLICT, "Bu VIN ilə maşın artıq var");
        }
        return mapper.toDto(repository.save(mapper.toEntity(dto)));
    }

    /** Pasport məlumatlarını dəyişir. Status və GPS ayrıca endpoint-lərlədir. */
    @Transactional
    public TruckDto update(Long id, TruckDto dto) {
        validate(dto);
        Truck t = find(id);
        String plate = dto.getPlate().trim().toUpperCase();
        repository.findByPlate(plate).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new ApiException(HttpStatus.CONFLICT, "Bu dövlət nömrəsi başqa maşına aiddir: " + plate);
            }
        });
        mapper.apply(t, dto);
        return withDriver(mapper.toDto(t), driversByTruck(List.of(id)).get(id));
    }

    /** GPS cihazından və ya sürücü tətbiqindən gələn son məkan. */
    @Transactional
    public TruckDto updatePosition(Long id, PositionUpdateDto p) {
        Truck t = find(id);
        t.setLat(p.getLat());
        t.setLon(p.getLon());
        t.setPositionUpdatedAt(Instant.now());
        return mapper.toDto(t);
    }

    /** Sürücü maşını xidmətdən çıxarır (qəza/nasazlıq) və ya xidmətə qaytarır. */
    @Transactional
    public TruckDto updateStatus(Long id, TruckStatusUpdateDto dto) {
        Truck t = repository.findForUpdate(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Maşın", id));
        TruckStatus target = dto.getStatus();

        if (target == TruckStatus.BUSY) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BUSY statusu yalnız sifariş təyinatı ilə verilir");
        }
        if (t.getStatus() == TruckStatus.BUSY) {
            throw new ApiException(HttpStatus.CONFLICT, "Maşın reysdədir, əvvəlcə reysi bağlayın");
        }
        t.setStatus(target);
        return mapper.toDto(t);
    }

    /** Sahələrarası yoxlamalar. */
    private void validate(TruckDto d) {
        int maxYear = LocalDate.now().getYear() + 1;
        if (d.getManufactureYear() != null && d.getManufactureYear() > maxYear) {
            throw bad("Buraxılış ili " + maxYear + "-dən böyük ola bilməz");
        }
        if (d.getTempMin() != null && d.getTempMax() != null && d.getTempMin() > d.getTempMax()) {
            throw bad("Minimum temperatur maksimumdan böyük ola bilməz");
        }
        if (d.getType() == TruckType.REFRIGERATED && d.getTempMin() == null && d.getTempMax() == null) {
            throw bad("Soyuducu maşın üçün işləyən temperatur diapazonunu daxil edin");
        }
        if (d.getBodyLengthM() != null && d.getBodyWidthM() != null && d.getBodyHeightM() != null
                && d.getVolumeM3() != null) {
            double calc = d.getBodyLengthM() * d.getBodyWidthM() * d.getBodyHeightM();
            if (d.getVolumeM3() > calc * 1.1) {
                throw bad("Həcm kuzovun ölçülərindən çıxan həcmdən (" + String.format("%.1f", calc) + " m³) böyük ola bilməz");
            }
        }
    }

    private Map<Long, Driver> driversByTruck(Collection<Long> truckIds) {
        Map<Long, Driver> m = new HashMap<>();
        if (truckIds.isEmpty()) return m;
        for (Driver d : drivers.findByTruckIdInAndActiveTrue(truckIds)) {
            m.put(d.getTruckId(), d);
        }
        return m;
    }

    private TruckDto withDriver(TruckDto dto, Driver d) {
        if (d != null) {
            dto.setDriverId(d.getId());
            dto.setDriverName(d.getFullName());
            dto.setDriverPhone(d.getPhone());
        }
        return dto;
    }

    private static ApiException bad(String msg) {
        return new ApiException(HttpStatus.BAD_REQUEST, msg);
    }

    private Truck find(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Maşın", id));
    }
}
