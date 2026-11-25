package az.freight.entity;

import az.freight.enums.OrderEventType;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Sifarişin serverdə saxlanan tarixçəsi (əvvəl brauzerin localStorage-ində idi). */
@Entity
@Table(name = "order_event")
@Getter @Setter @NoArgsConstructor
public class OrderEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private OrderEventType type;

    @Column(nullable = false, length = 1000)
    private String message;

    private String actor;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
