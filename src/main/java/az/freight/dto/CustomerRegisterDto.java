package az.freight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Müştəri qeydiyyatı: anket + giriş məlumatı. */
@Getter @Setter
public class CustomerRegisterDto extends CustomerProfileDto {
    @NotBlank(message = "Telefon tələb olunur")
    @Size(max = 30)
    private String phone;

    @NotBlank(message = "Parol tələb olunur")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$", message = "Parol ən azı 8 simvol olmalı, hərf və rəqəm ehtiva etməlidir")
    private String password;
}
