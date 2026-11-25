package az.freight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Sürücü qeydiyyatı: anket + giriş məlumatı (telefon və PİN). */
@Getter @Setter
public class DriverCreateDto extends DriverProfileDto {
    @NotBlank(message = "Telefon tələb olunur")
    @Size(max = 30)
    private String phone;

    @NotBlank(message = "PİN tələb olunur")
    @Pattern(regexp = "\\d{4,8}", message = "PİN 4-8 rəqəmdən ibarət olmalıdır")
    private String pin;
}
