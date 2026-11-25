package az.freight.config;

import az.freight.entity.Driver;
import az.freight.entity.Truck;
import az.freight.repository.DriverRepository;
import az.freight.repository.TruckRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Yalnız "dev" profilində: hər maşına bir test sürücüsü yaradır (PİN: 1234). Produksiyada işləmir. */
@Component
@Profile("dev")
public class DevDriverSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDriverSeeder.class);

    private final DriverRepository drivers;
    private final TruckRepository trucks;
    private final PasswordEncoder encoder;

    public DevDriverSeeder(DriverRepository drivers, TruckRepository trucks, PasswordEncoder encoder) {
        this.drivers = drivers;
        this.trucks = trucks;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        // Sürücüsü olmayan hər maşın üçün test sürücüsü (əvvəlki işə salmalardan qalan bazada da işləyir)
        List<Truck> all = trucks.findAll();
        for (Truck t : all) {
            if (drivers.findFirstByTruckIdAndActiveTrue(t.getId()).isPresent()) continue;
            int n = t.getId().intValue();
            String phone = String.format("+99450000%04d", n);
            if (drivers.existsByPhone(phone)) continue;
            Driver d = new Driver();
            d.setFullName("Test sürücü " + n);
            d.setPhone(phone);
            d.setPinHash(encoder.encode("1234"));
            d.setTruckId(t.getId());
            // Test məlumatı: real qeydiyyat anketinin sahələri
            d.setNationalId(String.format("TST%04d", n));
            d.setBirthDate(java.time.LocalDate.now().minusYears(35));
            d.setLicenseNumber(String.format("AZE%06d", n));
            d.setLicenseCategories("C,CE");
            d.setLicenseExpiry(java.time.LocalDate.now().plusYears(3));
            d.setMedicalExpiry(java.time.LocalDate.now().plusYears(1));
            d.setExperienceYears(10);
            drivers.save(d);
            log.info("Test sürücü: {} / PİN 1234 → {}", d.getPhone(), t.getPlate());
        }
    }
}
