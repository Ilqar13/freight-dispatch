package az.freight.dto;

import az.freight.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Sürücü/dispetçer sifarişin statusunu dəyişir.
 * Yola çıxma (IN_TRANSIT), çatdırma (DELIVERED), ləğv (CANCELLED) və ya
 * yükü hovuza qaytarma (CREATED) üçün istifadə olunur.
 */
@Data
public class OrderStatusUpdateDto {

    @NotNull
    private OrderStatus status;

    /** Səbəb (qəza, nasazlıq və s.) — jurnal üçün log-a yazılır. */
    private String reason;
}
