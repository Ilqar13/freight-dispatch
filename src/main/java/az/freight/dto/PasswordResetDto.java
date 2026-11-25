package az.freight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PasswordResetDto {
    @NotBlank(message = "Parol tələb olunur")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$", message = "Parol ən azı 8 simvol olmalı, hərf və rəqəm ehtiva etməlidir")
    private String password;
}
