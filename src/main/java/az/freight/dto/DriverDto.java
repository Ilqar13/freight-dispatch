package az.freight.dto;

import java.time.Instant;
import java.time.LocalDate;
import lombok.Data;

@Data
public class DriverDto {
    private Long id;
    private String fullName;
    private String phone;
    private boolean active;
    private Long truckId;
    private String truckPlate;
    /** Yalnız /api/driver/me cavabında dolur. */
    private TruckDto truck;

    // ---- anket
    private String nationalId;
    private LocalDate birthDate;
    private String licenseNumber;
    private String licenseCategories;
    private LocalDate licenseExpiry;
    private Integer experienceYears;
    private String address;
    private String email;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private LocalDate hireDate;
    private LocalDate medicalExpiry;
    private boolean adrCertified;
    private String notes;
    private Instant createdAt;
}
