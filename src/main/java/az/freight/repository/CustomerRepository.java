package az.freight.repository;

import az.freight.entity.Customer;
import az.freight.enums.CustomerStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByPhone(String phone);

    boolean existsByPhone(String phone);

    boolean existsByTaxId(String taxId);

    Optional<Customer> findByTaxId(String taxId);

    Page<Customer> findByStatus(CustomerStatus status, Pageable pageable);

    long countByStatus(CustomerStatus status);
}
