package az.freight.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

@Data
public class PositionUpdateDto {
    @DecimalMin("-90") @DecimalMax("90")
    private double lat;

    @DecimalMin("-180") @DecimalMax("180")
    private double lon;
}
