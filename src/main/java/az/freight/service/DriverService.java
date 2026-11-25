package az.freight.service;

import az.freight.dto.DriverCreateDto;
import az.freight.dto.DriverDto;
import az.freight.dto.DriverProfileDto;
import az.freight.dto.DriverUpdateDto;
import az.freight.dto.PinResetDto;
import az.freight.entity.Driver;
import az.freight.entity.Truck;
import az.freight.exception.ApiException;
import az.freight.exception.ResourceNotFoundException;
import az.freight.mapper.DriverMapper;
import az.freight.repository.DriverRepository;
import az.freight.repository.DriverSessionRepository;
import az.freight.repository.TruckRepository;
import az.freight.util.PhoneUtil;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Dispetçer tərəfdən sürücü idarəsi: yarat, maşına bağla, PİN-i sıfırla, söndür. */
@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository drivers;
    private final DriverSessionRepository sessions;
    private final TruckRepository trucks;
    private final DriverMapper mapper;
    private final PasswordEncoder encoder;

    @Transactional(readOnly = true)
    public Page<DriverDto> list(Pageable pageable) {
        Page<Driver> page = drivers.findAll(pageable);
        Set<Long> truckIds = page.getContent().stream().map(Driver::getTruckId)
                .filter(id -> id != null).collect(Collectors.toSet());
        Map<Long, Truck> byId = new HashMap<>();
        trucks.findAllById(truckIds).forEach(t -> byId.put(t.getId(), t));
        return page.map(d -> mapper.toDto(d, d.getTruckId() == null ? null : byId.get(d.getTruckId()), false));
    }

    @Transactional
    public DriverDto create(DriverCreateDto dto) {
        String phone = PhoneUtil.normalize(dto.getPhone());
        if (phone.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Telefon tələb olunur");
        }
        if (drivers.existsByPhone(phone)) {
            throw new ApiException(HttpStatus.CONFLICT, "Bu telefon nömrəsi ilə sürücü artıq var");
        }
        validateProfile(dto);
        // Yeni qeydiyyatda vəsiqə artıq bitmiş olmamalıdır
        if (!dto.getLicenseExpiry().isAfter(LocalDate.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Vəsiqənin müddəti bitib, yeni sürücü qeydiyyatdan keçə bilməz");
        }
        String fin = dto.getNationalId().trim().toUpperCase();
        String lic = dto.getLicenseNumber().trim().toUpperCase();
        if (drivers.existsByNationalId(fin)) {
            throw new ApiException(HttpStatus.CONFLICT, "Bu FİN ilə sürücü artıq qeydiyyatdadır");
        }
        if (drivers.existsByLicenseNumber(lic)) {
            throw new ApiException(HttpStatus.CONFLICT, "Bu vəsiqə nömrəsi ilə sürücü artıq qeydiyyatdadır");
        }
        Truck truck = bindableTruck(dto.getTruckId(), null);

        Driver d = new Driver();
        d.setPhone(phone);
        d.setPinHash(encoder.encode(dto.getPin()));
        applyProfile(d, dto);
        d.setTruckId(truck == null ? null : truck.getId());
        return mapper.toDto(drivers.save(d), truck, false);
    }

    @Transactional
    public DriverDto update(Long id, DriverUpdateDto dto) {
        Driver d = find(id);
        validateProfile(dto);
        String fin = dto.getNationalId().trim().toUpperCase();
        String lic = dto.getLicenseNumber().trim().toUpperCase();
        drivers.findAll().stream()
                .filter(o -> !o.getId().equals(id))
                .filter(o -> fin.equals(o.getNationalId()) || lic.equals(o.getLicenseNumber()))
                .findAny()
                .ifPresent(o -> {
                    throw new ApiException(HttpStatus.CONFLICT, "FİN və ya vəsiqə nömrəsi başqa sürücüyə aiddir");
                });
        Truck truck = dto.isActive() ? bindableTruck(dto.getTruckId(), id) : loadTruck(dto.getTruckId());
        applyProfile(d, dto);
        d.setTruckId(truck == null ? null : truck.getId());
        d.setActive(dto.isActive());
        if (!dto.isActive()) {
            sessions.deleteByDriver(id);
        }
        return mapper.toDto(drivers.save(d), truck, false);
    }

    /** Anketin sahələrarası yoxlamaları. */
    private void validateProfile(DriverProfileDto p) {
        LocalDate today = LocalDate.now();
        if (p.getBirthDate().isAfter(today.minusYears(21))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Yük maşını sürücüsü ən azı 21 yaşında olmalıdır");
        }
        if (p.getBirthDate().isBefore(today.minusYears(75))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Doğum tarixi yanlış görünür");
        }
        if (p.getExperienceYears() != null) {
            long maxExp = ChronoUnit.YEARS.between(p.getBirthDate(), today) - 18;
            if (p.getExperienceYears() > maxExp) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Təcrübə ili yaşa uyğun deyil");
            }
        }
        if (p.getHireDate() != null && p.getHireDate().isAfter(today)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "İşə qəbul tarixi gələcəkdə ola bilməz");
        }
        // Yük maşını üçün C və ya CE kateqoriyası lazımdır
        List<String> cats = List.of(p.getLicenseCategories().split(","));
        if (!cats.contains("C") && !cats.contains("CE") && !cats.contains("C1") && !cats.contains("D")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Yük maşını sürmək üçün C, CE və ya C1 kateqoriyası lazımdır");
        }
    }

    private void applyProfile(Driver d, DriverProfileDto p) {
        d.setFullName(p.getFullName().trim());
        d.setNationalId(p.getNationalId().trim().toUpperCase());
        d.setBirthDate(p.getBirthDate());
        d.setLicenseNumber(p.getLicenseNumber().trim().toUpperCase());
        d.setLicenseCategories(p.getLicenseCategories());
        d.setLicenseExpiry(p.getLicenseExpiry());
        d.setExperienceYears(p.getExperienceYears());
        d.setAddress(blank(p.getAddress()));
        d.setEmail(blank(p.getEmail()));
        d.setEmergencyContactName(blank(p.getEmergencyContactName()));
        d.setEmergencyContactPhone(blank(p.getEmergencyContactPhone()));
        d.setHireDate(p.getHireDate());
        d.setMedicalExpiry(p.getMedicalExpiry());
        d.setAdrCertified(p.isAdrCertified());
        d.setNotes(blank(p.getNotes()));
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** PİN-i dəyişir, bloku açır və bütün açıq sessiyaları bağlayır. */
    @Transactional
    public void resetPin(Long id, PinResetDto dto) {
        Driver d = find(id);
        d.setPinHash(encoder.encode(dto.getPin()));
        d.setFailedAttempts(0);
        d.setLockedUntil(null);
        drivers.save(d);
        sessions.deleteByDriver(id);
    }

    private Driver find(Long id) {
        return drivers.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Sürücü", id));
    }

    private Truck loadTruck(Long truckId) {
        if (truckId == null) return null;
        return trucks.findById(truckId).orElseThrow(() -> ResourceNotFoundException.of("Maşın", truckId));
    }

    /** Maşın mövcud olmalı və başqa aktiv sürücüyə bağlı olmamalıdır. */
    private Truck bindableTruck(Long truckId, Long selfDriverId) {
        Truck t = loadTruck(truckId);
        if (t == null) return null;
        drivers.findFirstByTruckIdAndActiveTrue(t.getId()).ifPresent(other -> {
            if (!other.getId().equals(selfDriverId)) {
                throw new ApiException(HttpStatus.CONFLICT,
                        t.getPlate() + " maşını artıq başqa sürücüyə (" + other.getFullName() + ") bağlıdır");
            }
        });
        return t;
    }
}
