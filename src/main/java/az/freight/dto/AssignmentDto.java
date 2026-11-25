package az.freight.dto;

import az.freight.enums.AssignmentKind;
import java.time.Instant;
import lombok.Data;

@Data
public class AssignmentDto {
    private Long id;
    private Long orderId;
    private Long truckId;
    private double distanceKm;
    private double etaHours;
    private double cost;
    private double score;
    private String reason;
    private String assignedBy;
    private Instant createdAt;

    /** REPLACEMENT olduqda maşın əvvəl pickup nöqtəsinə gedir; distanceKm/etaHours isə pickup + təyinat cəmidir. */
    private AssignmentKind kind;
    private Long incidentId;
    private Double pickupLat;
    private Double pickupLon;
    private Double pickupKm;
    private Double pickupEtaHours;
}
