package az.freight.entity;

import az.freight.enums.IncidentStatus;
import az.freight.enums.IncidentType;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Texniki problem: sürücü bildirir, maşın dayandırılır, əvəzedici boş maşın çağırılır. */
@Entity
@Table(name = "incident")
@Getter @Setter @NoArgsConstructor
public class Incident {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** Nasaz olan maşın. */
    @Column(name = "truck_id", nullable = false)
    private Long truckId;

    @Column(name = "driver_id")
    private Long driverId;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private IncidentType type;

    @Column(length = 1000)
    private String description;

    /** Yükün dayandığı nöqtə: əvəzedici maşın ora gəlməlidir. */
    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lon;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private IncidentStatus status = IncidentStatus.AWAITING_TRUCK;

    @Column(name = "replacement_truck_id")
    private Long replacementTruckId;

    @Column(name = "replacement_assignment_id")
    private Long replacementAssignmentId;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "replacement_assigned_at")
    private Instant replacementAssignedAt;

    @Column(name = "closed_at")
    private Instant closedAt;
}
