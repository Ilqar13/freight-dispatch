package az.freight.mapper;

import az.freight.dto.OrderEventDto;
import az.freight.entity.OrderEvent;
import org.springframework.stereotype.Component;

@Component
public class OrderEventMapper {

    public OrderEventDto toDto(OrderEvent e) {
        if (e == null) return null;
        OrderEventDto dto = new OrderEventDto();
        dto.setId(e.getId());
        dto.setOrderId(e.getOrderId());
        dto.setType(e.getType());
        dto.setMessage(e.getMessage());
        dto.setActor(e.getActor());
        dto.setCreatedAt(e.getCreatedAt());
        return dto;
    }
}
