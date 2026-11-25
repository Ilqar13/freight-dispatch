package az.freight.dto;

import az.freight.enums.TruckStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Sürücü maşını xidmətdən çıxarır (MAINTENANCE) və ya xidmətə qaytarır (FREE). */
@Data
public class TruckStatusUpdateDto {

    @NotNull
    private TruckStatus status;
}
