package az.freight.enums;

/**
 * AWAITING_TRUCK: əvəzedici boş maşın axtarılır (sistem hər 30 saniyədə yenidən cəhd edir).
 * REPLACEMENT_ASSIGNED: əvəzedici maşın təyin olunub və nasazlıq nöqtəsinə yola düşməlidir.
 * CLOSED: yük əvəzedici maşına götürülüb (və ya sifariş ləğv/bağlanıb).
 */
public enum IncidentStatus { AWAITING_TRUCK, REPLACEMENT_ASSIGNED, CLOSED }
