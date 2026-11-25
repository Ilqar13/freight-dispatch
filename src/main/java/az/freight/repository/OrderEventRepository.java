package az.freight.repository;

import az.freight.entity.OrderEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {
    /** Ən yenisi əvvəldə. */
    List<OrderEvent> findByOrderIdOrderByIdDesc(Long orderId);
}
