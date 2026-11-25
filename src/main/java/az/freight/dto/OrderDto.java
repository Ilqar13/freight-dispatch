package az.freight.dto;

import az.freight.enums.CargoType;
import az.freight.enums.OrderStatus;
import az.freight.enums.PackageType;
import az.freight.enums.Payer;
import az.freight.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Data;

/**
 * Sifariş. lat/lon və destAddress ÇATDIRILMA nöqtəsidir (köhnə müştərilərlə uyğunluq üçün adları saxlanıb).
 * Yükləmə nöqtəsi pickup* sahələridir; verilməyibsə maşın birbaşa çatdırılma ünvanına göndərilir.
 */
@Data
public class OrderDto {
    private Long id;
    /** Yalnız cavabda: 166-2026-00005. */
    private String orderNo;
    /** Qeydiyyatlı müştəri. Müştəri portalında server özü təyin edir. */
    private Long customerId;

    // ---- Müştəri / sifarişçi
    @NotBlank @Size(max = 200)
    private String customerName;
    @Size(max = 200) private String contactName;
    @Size(max = 30) private String contactPhone;
    @Email @Size(max = 200) private String contactEmail;
    /** VÖEN: 10 rəqəm. */
    @Pattern(regexp = "^$|\\d{10}", message = "VÖEN 10 rəqəmdən ibarət olmalıdır")
    private String taxId;
    @Size(max = 100) private String referenceNo;

    // ---- Yükləmə
    @Size(max = 500) private String pickupAddress;
    @DecimalMin("-90") @DecimalMax("90") private Double pickupLat;
    @DecimalMin("-180") @DecimalMax("180") private Double pickupLon;
    @Size(max = 200) private String pickupContactName;
    @Size(max = 30) private String pickupContactPhone;
    private Instant pickupFrom;
    private Instant pickupTo;

    // ---- Çatdırılma
    @Size(max = 500) private String destAddress;
    @DecimalMin("-90") @DecimalMax("90") private double lat;
    @DecimalMin("-180") @DecimalMax("180") private double lon;
    @Size(max = 200) private String destContactName;
    @Size(max = 30) private String destContactPhone;
    private Instant deliveryFrom;
    private Instant deliveryTo;

    // ---- Yük
    @NotNull private CargoType cargoType;
    @Positive private double tons;
    @Size(max = 1000) private String cargoDescription;
    @Positive private Double volumeM3;
    @Positive private Integer packagesCount;
    private PackageType packageType;
    @PositiveOrZero private Double declaredValue;
    private boolean hazardous;
    @Size(max = 20) private String unNumber;
    private Double tempMin;
    private Double tempMax;
    private boolean fragile;
    private boolean stackable = true;
    private boolean urgent;

    // ---- Xidmətlər
    private boolean needsLoadingHelp;
    private boolean needsUnloadingHelp;
    private boolean needsInsurance;
    @Size(max = 2000) private String specialInstructions;

    // ---- Ödəniş
    @PositiveOrZero private Double price;
    @Pattern(regexp = "^[A-Z]{3}$", message = "Valyuta 3 böyük hərfdir (AZN, USD, EUR)")
    private String currency = "AZN";
    private PaymentMethod paymentMethod;
    private Payer payer;
    private boolean invoiceRequired;

    /** Yalnız cavabda gəlir. */
    private OrderStatus status;
    private Instant createdAt;
}
