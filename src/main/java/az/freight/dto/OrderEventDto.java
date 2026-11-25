package az.freight.dto;

import az.freight.enums.OrderEventType;
import java.time.Instant;
import lombok.Data;

@Data
public class OrderEventDto {
    private Long id;
    private Long orderId;
    private OrderEventType type;
    private String message;
    private String actor;
    private Instant createdAt;
}
