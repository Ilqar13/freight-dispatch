package az.freight.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DriverLoginRequest {
    @NotBlank(message = "Telefon tələb olunur")
    private String phone;

    @NotBlank(message = "PİN tələb olunur")
    private String pin;
}
