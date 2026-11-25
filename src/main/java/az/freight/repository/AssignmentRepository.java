package az.freight.repository;

import az.freight.entity.Assignment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByOrderId(Long orderId);

    /** Maşına edilmiş bütün təyinatlar (ən yenisi əvvəldə). */
    List<Assignment> findByTruckIdOrderByIdDesc(Long truckId);

    /** Sifarişin ən son təyinatı (ötürmə zamanı köhnə maşını tapmaq üçün). */
    Optional<Assignment> findTopByOrderIdOrderByIdDesc(Long orderId);
}
