package az.freight.enums;

/** Sifariş tarixçəsindəki hadisə növləri. */
public enum OrderEventType {
    CREATED, ACCEPTED, ASSIGNED, STARTED, DELIVERED, CANCELLED, RELEASED, TRANSFERRED,
    BREAKDOWN, REPLACEMENT_SEARCH, REPLACEMENT_ASSIGNED, CARGO_PICKED_UP, NOTE
}
