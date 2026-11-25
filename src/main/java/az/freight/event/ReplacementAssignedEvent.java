package az.freight.event;

public record ReplacementAssignedEvent(Long orderId, Long incidentId, Long replacementTruckId,
                                       String replacementPlate, double pickupEtaHours) {}
