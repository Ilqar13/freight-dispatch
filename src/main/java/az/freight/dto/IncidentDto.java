package az.freight.dto;

import az.freight.enums.IncidentStatus;
import az.freight.enums.IncidentType;
import java.time.Instant;
import lombok.Data;

@Data
public class IncidentDto {
    private Long id;
    private Long orderId;
    private IncidentType type;
    private String description;
    private double lat;
    private double lon;
    private IncidentStatus status;
    private int attempts;
    private Instant lastAttemptAt;
    private Instant createdAt;
    private Instant replacementAssignedAt;
    private Instant closedAt;

    /** Nasaz maşın və sürücü. */
    private Long truckId;
    private String truckPlate;
    private String driverName;
    private String driverPhone;

    /** Əvəzedici maşın və sürücü (təyin olunubsa). */
    private Long replacementTruckId;
    private String replacementPlate;
    private String replacementDriverName;
    private String replacementDriverPhone;
    /** Əvəzedici maşının nasazlıq nöqtəsinə çatma vaxtı və məsafəsi. */
    private Double pickupKm;
    private Double pickupEtaHours;
}
