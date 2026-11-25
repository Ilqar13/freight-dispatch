package az.freight.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerLoginRequest {
    @NotBlank(message = "Telefon tələb olunur")
    private String phone;

    @NotBlank(message = "Parol tələb olunur")
    private String password;
}
