package az.freight.event;

public record TruckAssignedEvent(Long orderId, Long truckId, String plate, double etaHours) {}
