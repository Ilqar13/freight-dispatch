package az.freight.entity;

import az.freight.enums.CargoType;
import az.freight.enums.OrderStatus;
import az.freight.enums.PackageType;
import az.freight.enums.Payer;
import az.freight.enums.PaymentMethod;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Enumerated(EnumType.STRING) @Column(name = "cargo_type", nullable = false)
    private CargoType cargoType;

    @Column(nullable = false)
    private double tons;

    @Column(name = "dest_address")
    private String destAddress;

    @Column(name = "dest_lat", nullable = false)
    private double destLat;

    @Column(name = "dest_lon", nullable = false)
    private double destLon;

    @Column(nullable = false)
    private boolean urgent;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private OrderStatus status = OrderStatus.CREATED;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    /** Sifarişi verən qeydiyyatlı müştəri (köhnə sifarişlərdə boşdur). */
    @Column(name = "customer_id")
    private Long customerId;

    /** İnsana görünən nömrə: 166-2026-00005. */
    @Column(name = "order_no", unique = true)
    private String orderNo;

    // ---- Müştəri / sifarişçi
    @Column(name = "contact_name") private String contactName;
    @Column(name = "contact_phone") private String contactPhone;
    @Column(name = "contact_email") private String contactEmail;
    @Column(name = "tax_id") private String taxId;
    @Column(name = "reference_no") private String referenceNo;

    // ---- Yükləmə nöqtəsi (boşdursa maşın birbaşa çatdırılma ünvanına gedir: köhnə sifarişlər)
    @Column(name = "pickup_address") private String pickupAddress;
    @Column(name = "pickup_lat") private Double pickupLat;
    @Column(name = "pickup_lon") private Double pickupLon;
    @Column(name = "pickup_contact_name") private String pickupContactName;
    @Column(name = "pickup_contact_phone") private String pickupContactPhone;
    @Column(name = "pickup_from") private Instant pickupFrom;
    @Column(name = "pickup_to") private Instant pickupTo;

    // ---- Çatdırılma
    @Column(name = "dest_contact_name") private String destContactName;
    @Column(name = "dest_contact_phone") private String destContactPhone;
    @Column(name = "delivery_from") private Instant deliveryFrom;
    @Column(name = "delivery_to") private Instant deliveryTo;

    // ---- Yük
    @Column(name = "cargo_description") private String cargoDescription;
    @Column(name = "volume_m3") private Double volumeM3;
    @Column(name = "packages_count") private Integer packagesCount;
    @Enumerated(EnumType.STRING) @Column(name = "package_type") private PackageType packageType;
    @Column(name = "declared_value") private Double declaredValue;
    @Column(nullable = false) private boolean hazardous;
    @Column(name = "un_number") private String unNumber;
    @Column(name = "temp_min") private Double tempMin;
    @Column(name = "temp_max") private Double tempMax;
    @Column(nullable = false) private boolean fragile;
    @Column(nullable = false) private boolean stackable = true;

    // ---- Xidmətlər
    @Column(name = "needs_loading_help", nullable = false) private boolean needsLoadingHelp;
    @Column(name = "needs_unloading_help", nullable = false) private boolean needsUnloadingHelp;
    @Column(name = "needs_insurance", nullable = false) private boolean needsInsurance;
    @Column(name = "special_instructions") private String specialInstructions;

    // ---- Ödəniş
    private Double price;
    @Column(nullable = false) private String currency = "AZN";
    @Enumerated(EnumType.STRING) @Column(name = "payment_method") private PaymentMethod paymentMethod;
    @Enumerated(EnumType.STRING) private Payer payer;
    @Column(name = "invoice_required", nullable = false) private boolean invoiceRequired;

    public boolean hasPickup() {
        return pickupLat != null && pickupLon != null;
    }

    /** Maşının əvvəl gələcəyi nöqtə: yükləmə nöqtəsi, yoxdursa çatdırılma ünvanı. */
    public double loadLat() {
        return hasPickup() ? pickupLat : destLat;
    }

    public double loadLon() {
        return hasPickup() ? pickupLon : destLon;
    }

}
