package az.freight.enums;

/**
 * BREAKDOWN: sürücü texniki problem bildirib, maşın dayandırılıb və əvəzedici boş maşın gözlənilir.
 * Yalnız nasazlıq bildirişi (IncidentService) bu statusa keçirə bilər; əvəzedici maşın tapılanda
 * sifariş yenidən TRUCK_ASSIGNED olur.
 */
public enum OrderStatus { CREATED, TRUCK_ASSIGNED, IN_TRANSIT, BREAKDOWN, DELIVERED, CANCELLED }
