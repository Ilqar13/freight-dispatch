package az.freight.dto;

import az.freight.enums.IncidentType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Sürücünün texniki problem bildirişi. lat/lon boşdursa maşının son GPS məkanı götürülür. */
@Data
public class BreakdownRequest {
    @NotNull(message = "Problem növü tələb olunur")
    private IncidentType type;

    @Size(max = 1000)
    private String description;

    @DecimalMin("-90") @DecimalMax("90")
    private Double lat;

    @DecimalMin("-180") @DecimalMax("180")
    private Double lon;
}
