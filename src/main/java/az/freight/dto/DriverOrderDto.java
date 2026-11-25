package az.freight.dto;

import java.util.List;
import lombok.Data;

/** Sürücü portalı üçün reys görünüşü: sifariş + onun son təyinatı + nasazlıq məlumatı + tarixçə. */
@Data
public class DriverOrderDto {
    private OrderDto order;
    /** Bu maşının son təyinatı (boş yüklər siyahısında null). */
    private AssignmentDto assignment;
    /** Nasazlıqla bağlı reysdə: ən son nasazlıq. */
    private IncidentDto incident;
    /** Bu reys nasaz maşının yükünü götürmək üçün (əvvəl nasazlıq nöqtəsinə getməlisən). */
    private boolean rescue;
    /** Bu reys hazırda mənim maşınımındır (son təyinat mənimdir). */
    private boolean mine;
    /** Boş yüklər siyahısında: maşından təyinata düz xətt məsafəsi, km. */
    private Double distanceKm;
    /** Yalnız detal görünüşündə: tarixçə (ən yenisi əvvəldə). */
    private List<OrderEventDto> events;
}
