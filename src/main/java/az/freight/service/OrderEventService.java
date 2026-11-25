package az.freight.service;

import az.freight.dto.OrderEventDto;
import az.freight.entity.OrderEvent;
import az.freight.enums.OrderEventType;
import az.freight.mapper.OrderEventMapper;
import az.freight.repository.OrderEventRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sifariş tarixçəsi (sürücü, dispetçer və müştəri bunu görür). */
@Service
@RequiredArgsConstructor
public class OrderEventService {

    private final OrderEventRepository repository;
    private final OrderEventMapper mapper;

    @Transactional
    public void log(Long orderId, OrderEventType type, String message, String actor) {
        OrderEvent e = new OrderEvent();
        e.setOrderId(orderId);
        e.setType(type);
        e.setMessage(cut(message, 1000));
        e.setActor(cut(actor, 100));
        repository.save(e);
    }

    @Transactional(readOnly = true)
    public List<OrderEventDto> list(Long orderId) {
        return repository.findByOrderIdOrderByIdDesc(orderId).stream().map(mapper::toDto).toList();
    }

    private static String cut(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
