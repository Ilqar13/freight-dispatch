package az.freight.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Sürücü sessiyası. Tokenin özü saxlanmır, yalnız SHA-256 hash-i. */
@Entity
@Table(name = "driver_session")
@Getter @Setter @NoArgsConstructor
public class DriverSession {
    @Id
    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
