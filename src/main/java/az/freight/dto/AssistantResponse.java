package az.freight.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AssistantResponse {
    private String reply;
    /** Bu cavabda sifariş yaradılıbsa və ya ləğv edilibsə dolu olur: interfeys siyahını yeniləyir. */
    private OrderDto order;
    private boolean ordersChanged;
}
