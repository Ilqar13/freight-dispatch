package az.freight.event;

public record IncidentReportedEvent(Long orderId, Long incidentId, String truckPlate, String typeLabel) {}
