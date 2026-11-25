package az.freight.entity;

import az.freight.enums.AssignmentKind;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "assignment")
@Getter @Setter @NoArgsConstructor
public class Assignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "truck_id", nullable = false)
    private Long truckId;

    @Column(name = "distance_km", nullable = false)
    private double distanceKm;

    @Column(name = "eta_hours", nullable = false)
    private double etaHours;

    @Column(nullable = false)
    private double cost;

    @Column(nullable = false)
    private double score;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(name = "assigned_by")
    private String assignedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    /** REPLACEMENT: maşın əvvəl pickup nöqtəsinə (nasaz maşının dayandığı yerə) gedib yükü götürməlidir. */
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private AssignmentKind kind = AssignmentKind.INITIAL;

    @Column(name = "incident_id")
    private Long incidentId;

    @Column(name = "pickup_lat")
    private Double pickupLat;

    @Column(name = "pickup_lon")
    private Double pickupLon;

    @Column(name = "pickup_km")
    private Double pickupKm;

    @Column(name = "pickup_eta_hours")
    private Double pickupEtaHours;
}
