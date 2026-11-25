package az.freight.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Dispetçer əvəzedici maşını əllə seçir. */
@Data
public class IncidentAssignRequest {
    @NotNull(message = "Maşın (truckId) tələb olunur")
    private Long truckId;
}
