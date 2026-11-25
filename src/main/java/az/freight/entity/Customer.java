package az.freight.entity;

import az.freight.enums.CustomerStatus;
import az.freight.enums.CustomerType;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Müştəri (hüquqi və ya fiziki şəxs). Telefon + parol ilə daxil olur. */
@Entity
@Table(name = "customer")
@Getter @Setter @NoArgsConstructor
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING) @Column(name = "customer_type", nullable = false)
    private CustomerType customerType;

    @Column(nullable = false)
    private String name;

    @Column(name = "tax_id")
    private String taxId;

    @Column(name = "legal_address")
    private String legalAddress;

    @Column(name = "contact_name", nullable = false)
    private String contactName;

    @Column(nullable = false, unique = true)
    private String phone;

    private String email;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_account")
    private String bankAccount;

    @Column(name = "contract_no")
    private String contractNo;

    @Column(name = "payment_terms_days", nullable = false)
    private int paymentTermsDays;

    @Column(name = "credit_limit")
    private Double creditLimit;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private CustomerStatus status = CustomerStatus.PENDING;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
