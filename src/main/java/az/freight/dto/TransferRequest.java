package az.freight.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Aktiv reysin başqa sürücüyə/maşına ötürülməsi.
 * outOfService = true olduqda köhnə maşın FREE yerinə MAINTENANCE olur.
 */
@Data
public class TransferRequest {

    @NotNull
    private Long truckId;

    private boolean outOfService;

    private String reason;
}
