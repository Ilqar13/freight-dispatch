package az.freight.repository;

import az.freight.entity.CustomerSession;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerSessionRepository extends JpaRepository<CustomerSession, String> {

    @Modifying
    @Query("delete from CustomerSession s where s.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);

    @Modifying
    @Query("delete from CustomerSession s where s.customerId = :customerId")
    int deleteByCustomer(@Param("customerId") Long customerId);
}
