package az.freight.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Sürücü: telefon + PİN ilə daxil olur, bir maşına bağlıdır. */
@Entity
@Table(name = "driver")
@Getter @Setter @NoArgsConstructor
public class Driver {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(name = "pin_hash", nullable = false)
    private String pinHash;

    @Column(name = "truck_id")
    private Long truckId;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // ---- Qeydiyyat məlumatları
    @Column(name = "national_id") private String nationalId;
    @Column(name = "birth_date") private LocalDate birthDate;
    @Column(name = "license_number") private String licenseNumber;
    /** Vəsiqə kateqoriyaları, vergüllə: "C,CE". */
    @Column(name = "license_categories") private String licenseCategories;
    @Column(name = "license_expiry") private LocalDate licenseExpiry;
    @Column(name = "experience_years") private Integer experienceYears;
    private String address;
    private String email;
    @Column(name = "emergency_contact_name") private String emergencyContactName;
    @Column(name = "emergency_contact_phone") private String emergencyContactPhone;
    @Column(name = "hire_date") private LocalDate hireDate;
    @Column(name = "medical_expiry") private LocalDate medicalExpiry;
    @Column(name = "adr_certified", nullable = false) private boolean adrCertified;
    private String notes;

}
