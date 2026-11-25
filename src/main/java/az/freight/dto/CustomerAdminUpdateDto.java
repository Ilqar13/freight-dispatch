package az.freight.dto;

import az.freight.enums.CustomerStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Dispetçer müştərini redaktə edir: anket + müqavilə şərtləri + status. */
@Getter @Setter
public class CustomerAdminUpdateDto extends CustomerProfileDto {
    @NotNull(message = "Status tələb olunur")
    private CustomerStatus status;

    @Size(max = 50) private String contractNo;
    @Min(0) @Max(365) private int paymentTermsDays;
    @PositiveOrZero private Double creditLimit;
    @Size(max = 1000) private String notes;
}
