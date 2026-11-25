package az.freight.dto;

import az.freight.enums.CustomerType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Müştəri anketi (qeydiyyat və profil redaktəsi üçün ortaq). */
@Getter @Setter
public class CustomerProfileDto {
    @NotNull(message = "Müştəri növü tələb olunur")
    private CustomerType customerType;

    /** Şirkət adı və ya fiziki şəxsin ad-soyadı. */
    @NotBlank(message = "Ad tələb olunur")
    @Size(max = 200)
    private String name;

    /** Hüquqi şəxs üçün məcburidir (10 rəqəm). */
    @Pattern(regexp = "^$|\\d{10}", message = "VÖEN 10 rəqəmdən ibarət olmalıdır")
    private String taxId;

    @Size(max = 500) private String legalAddress;

    @NotBlank(message = "Əlaqə şəxsi tələb olunur")
    @Size(max = 200)
    private String contactName;

    @Email @Size(max = 200) private String email;
    @Size(max = 100) private String bankName;

    /** Azərbaycan IBAN: AZ + 2 rəqəm + 4 hərf + 20 simvol. */
    @Pattern(regexp = "^$|AZ\\d{2}[A-Z]{4}[0-9A-Z]{20}$", message = "IBAN formatı yanlışdır (AZ21NABZ00000000137010001944)")
    private String bankAccount;
}
