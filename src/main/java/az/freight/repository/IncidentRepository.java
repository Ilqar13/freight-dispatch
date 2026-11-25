package az.freight.repository;

import az.freight.entity.Incident;
import az.freight.enums.IncidentStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Page<Incident> findByStatus(IncidentStatus status, Pageable pageable);

    List<Incident> findByOrderIdOrderByIdDesc(Long orderId);

    Optional<Incident> findTopByOrderIdOrderByIdDesc(Long orderId);

    /** Nasaz maşının hələ bağlanmamış nasazlıqları (sürücü portalı üçün). */
    List<Incident> findByTruckIdAndStatusNotOrderByIdDesc(Long truckId, IncidentStatus status);

    boolean existsByTruckIdAndStatusNot(Long truckId, IncidentStatus status);

    List<Incident> findByOrderIdAndStatusNot(Long orderId, IncidentStatus status);

    long countByStatus(IncidentStatus status);

    @Query("select i.id from Incident i where i.status = :status order by i.id")
    List<Long> findIdsByStatus(@Param("status") IncidentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Incident i where i.id = :id")
    Optional<Incident> findForUpdate(@Param("id") Long id);
}
