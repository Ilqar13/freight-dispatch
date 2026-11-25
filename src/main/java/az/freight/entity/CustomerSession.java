package az.freight.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customer_session")
@Getter @Setter @NoArgsConstructor
public class CustomerSession {
    @Id
    @Column(name = "token_hash")
    private String tokenHash;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
