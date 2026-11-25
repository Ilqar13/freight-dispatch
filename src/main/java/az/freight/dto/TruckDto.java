package az.freight.dto;

import az.freight.enums.FuelType;
import az.freight.enums.OwnerType;
import az.freight.enums.TruckStatus;
import az.freight.enums.TruckType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Data;

/** Maşın pasportu. status, GPS vaxtı və sürücü sahələri yalnız cavabda gəlir. */
@Data
public class TruckDto {
    private Long id;

    @NotBlank
    @Pattern(regexp = "^[A-Z0-9][A-Z0-9 \\-]{3,14}[A-Z0-9]$", message = "Dövlət nömrəsi böyük hərf və rəqəmlərlə yazılmalıdır (məs. 10-AA-166)")
    private String plate;

    @NotNull
    private TruckType type;

    /** Yükgötürmə qabiliyyəti, ton. */
    @Positive
    private double capacityTons;

    @Positive
    private double costPerKm;

    @DecimalMin("-90") @DecimalMax("90")
    private double lat;

    @DecimalMin("-180") @DecimalMax("180")
    private double lon;

    // ---- pasport
    @Size(max = 50) private String brand;
    @Size(max = 50) private String model;
    @Min(1990) @Max(2100) private Integer manufactureYear;
    @Pattern(regexp = "^$|[A-HJ-NPR-Z0-9]{17}", message = "VIN 17 simvol olmalıdır (I, O, Q hərfləri olmaz)")
    private String vin;
    @Positive private Double bodyLengthM;
    @Positive private Double bodyWidthM;
    @Positive private Double bodyHeightM;
    @Positive private Double volumeM3;
    @Min(2) @Max(10) private Integer axles;
    private FuelType fuelType;
    private OwnerType ownerType = OwnerType.COMPANY;
    private boolean hasTailLift;
    private boolean adrCertified;
    /** Soyuducu üçün işləyən temperatur diapazonu. */
    private Double tempMin;
    private Double tempMax;
    @Size(max = 50) private String gpsDeviceId;

    // ---- sənədlər və texniki vəziyyət
    private LocalDate insuranceExpiry;
    private LocalDate techInspectionExpiry;
    private LocalDate lastServiceDate;
    @PositiveOrZero private Integer odometerKm;
    @Size(max = 1000) private String notes;

    // ---- yalnız cavabda
    private TruckStatus status;
    private Instant positionUpdatedAt;
    private Long driverId;
    private String driverName;
    private String driverPhone;
}
