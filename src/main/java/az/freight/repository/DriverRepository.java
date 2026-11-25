package az.freight.repository;

import az.freight.entity.Driver;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findByPhone(String phone);

    boolean existsByPhone(String phone);

    /** Maşına bağlı aktiv sürücü. */
    Optional<Driver> findFirstByTruckIdAndActiveTrue(Long truckId);

    boolean existsByTruckIdAndActiveTrue(Long truckId);

    java.util.List<Driver> findByTruckIdInAndActiveTrue(java.util.Collection<Long> truckIds);

    boolean existsByNationalId(String nationalId);

    boolean existsByLicenseNumber(String licenseNumber);

    Page<Driver> findAll(Pageable pageable);
}
