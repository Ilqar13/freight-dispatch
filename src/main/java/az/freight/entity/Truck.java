package az.freight.entity;

import az.freight.enums.FuelType;
import az.freight.enums.OwnerType;
import az.freight.enums.TruckStatus;
import az.freight.enums.TruckType;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "truck")
@Getter @Setter @NoArgsConstructor
public class Truck {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String plate;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TruckType type;

    @Column(name = "capacity_tons", nullable = false)
    private double capacityTons;

    @Column(name = "cost_per_km", nullable = false)
    private double costPerKm;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TruckStatus status = TruckStatus.FREE;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lon;

    @Column(name = "position_updated_at", nullable = false)
    private Instant positionUpdatedAt = Instant.now();


    // ---- Maşın pasportu
    private String brand;
    private String model;
    @Column(name = "manufacture_year") private Integer manufactureYear;
    private String vin;
    @Column(name = "body_length_m") private Double bodyLengthM;
    @Column(name = "body_width_m") private Double bodyWidthM;
    @Column(name = "body_height_m") private Double bodyHeightM;
    @Column(name = "volume_m3") private Double volumeM3;
    private Integer axles;
    @Enumerated(EnumType.STRING) @Column(name = "fuel_type") private FuelType fuelType;
    @Enumerated(EnumType.STRING) @Column(name = "owner_type", nullable = false) private OwnerType ownerType = OwnerType.COMPANY;
    @Column(name = "has_tail_lift", nullable = false) private boolean hasTailLift;
    @Column(name = "adr_certified", nullable = false) private boolean adrCertified;
    @Column(name = "temp_min") private Double tempMin;
    @Column(name = "temp_max") private Double tempMax;
    @Column(name = "gps_device_id") private String gpsDeviceId;
    @Column(name = "insurance_expiry") private LocalDate insuranceExpiry;
    @Column(name = "tech_inspection_expiry") private LocalDate techInspectionExpiry;
    @Column(name = "last_service_date") private LocalDate lastServiceDate;
    @Column(name = "odometer_km") private Integer odometerKm;
    private String notes;

    @Version
    private long version;
}
