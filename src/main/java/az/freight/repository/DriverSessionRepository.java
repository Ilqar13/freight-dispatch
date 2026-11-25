package az.freight.repository;

import az.freight.entity.DriverSession;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverSessionRepository extends JpaRepository<DriverSession, String> {

    @Modifying
    @Query("delete from DriverSession s where s.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);

    @Modifying
    @Query("delete from DriverSession s where s.driverId = :driverId")
    int deleteByDriver(@Param("driverId") Long driverId);
}
