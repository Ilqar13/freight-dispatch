package az.freight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PinResetDto {
    @NotBlank(message = "PİN tələb olunur")
    @Pattern(regexp = "\\d{4,8}", message = "PİN 4-8 rəqəmdən ibarət olmalıdır")
    private String pin;
}
