package az.freight.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** Sürücünün qeydiyyat anketi (yaratma və redaktə üçün ortaq sahələr). */
@Getter @Setter
public class DriverProfileDto {
    @NotBlank(message = "Ad, soyad tələb olunur")
    @Size(max = 200)
    private String fullName;

    /** Şəxsiyyət vəsiqəsinin FİN kodu: 7 simvol. */
    @NotBlank(message = "FİN tələb olunur")
    @Pattern(regexp = "^[A-Za-z0-9]{7}$", message = "FİN 7 simvoldan (hərf və rəqəm) ibarət olmalıdır")
    private String nationalId;

    @NotNull(message = "Doğum tarixi tələb olunur")
    private LocalDate birthDate;

    @NotBlank(message = "Sürücülük vəsiqəsinin nömrəsi tələb olunur")
    @Pattern(regexp = "^[A-Za-z0-9]{6,12}$", message = "Vəsiqə nömrəsi 6-12 hərf/rəqəmdən ibarət olmalıdır")
    private String licenseNumber;

    /** Vergüllə: "C,CE". İcazə verilən kateqoriyalar: B, BE, C, CE, C1, D. */
    @NotBlank(message = "Vəsiqə kateqoriyası tələb olunur")
    @Pattern(regexp = "^(B|BE|C|CE|C1|D)(,(B|BE|C|CE|C1|D))*$", message = "Kateqoriya yanlışdır (B, BE, C, CE, C1, D, vergüllə)")
    private String licenseCategories;

    @NotNull(message = "Vəsiqənin bitmə tarixi tələb olunur")
    private LocalDate licenseExpiry;

    @Min(0) @Max(60)
    private Integer experienceYears;

    @Size(max = 300) private String address;
    @Email @Size(max = 200) private String email;
    @Size(max = 200) private String emergencyContactName;
    @Size(max = 30) private String emergencyContactPhone;
    private LocalDate hireDate;
    /** Tibbi arayışın bitmə tarixi. */
    private LocalDate medicalExpiry;
    private boolean adrCertified;
    private Long truckId;
    @Size(max = 1000) private String notes;
}
