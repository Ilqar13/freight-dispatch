package az.freight.filter;

import az.freight.entity.Driver;
import az.freight.entity.Order;
import az.freight.entity.Truck;
import az.freight.repository.DriverRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Maşının reysə çıxa bilməsi üçün aktiv sürücüsü olmalı, sürücünün vəsiqəsi və tibbi arayışı etibarlı olmalıdır;
 * təhlükəli yük üçün sürücünün də ADR sertifikatı lazımdır.
 * `dispatch.require-driver: false` ilə söndürülə bilər (məsələn, sürücüləri hələ daxil etmədiyiniz sınaq mühitində).
 */
@Component
public class DriverReadyFilter implements TruckFilter {

    private final DriverRepository drivers;
    private final boolean required;

    public DriverReadyFilter(DriverRepository drivers, @Value("${dispatch.require-driver:true}") boolean required) {
        this.drivers = drivers;
        this.required = required;
    }

    @Override
    public Optional<String> reject(Truck t, Order order) {
        if (!required) return Optional.empty();
        Driver d = drivers.findFirstByTruckIdAndActiveTrue(t.getId()).orElse(null);
        if (d == null) {
            return Optional.of("Maşına aktiv sürücü təyin olunmayıb");
        }
        LocalDate today = LocalDate.now();
        if (d.getLicenseExpiry() != null && d.getLicenseExpiry().isBefore(today)) {
            return Optional.of("Sürücünün vəsiqəsinin müddəti bitib");
        }
        if (d.getMedicalExpiry() != null && d.getMedicalExpiry().isBefore(today)) {
            return Optional.of("Sürücünün tibbi arayışının müddəti bitib");
        }
        if (order.isHazardous() && !d.isAdrCertified()) {
            return Optional.of("Təhlükəli yük üçün sürücünün ADR sertifikatı yoxdur");
        }
        return Optional.empty();
    }
}
