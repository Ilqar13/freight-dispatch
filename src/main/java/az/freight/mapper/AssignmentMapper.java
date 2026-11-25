package az.freight.mapper;

import az.freight.dto.AssignmentDto;
import az.freight.entity.Assignment;
import org.springframework.stereotype.Component;

@Component
public class AssignmentMapper {

    public AssignmentDto toDto(Assignment e) {
        if (e == null) return null;
        AssignmentDto dto = new AssignmentDto();
        dto.setId(e.getId());
        dto.setOrderId(e.getOrderId());
        dto.setTruckId(e.getTruckId());
        dto.setDistanceKm(e.getDistanceKm());
        dto.setEtaHours(e.getEtaHours());
        dto.setCost(e.getCost());
        dto.setScore(e.getScore());
        dto.setReason(e.getReason());
        dto.setAssignedBy(e.getAssignedBy());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setKind(e.getKind());
        dto.setIncidentId(e.getIncidentId());
        dto.setPickupLat(e.getPickupLat());
        dto.setPickupLon(e.getPickupLon());
        dto.setPickupKm(e.getPickupKm());
        dto.setPickupEtaHours(e.getPickupEtaHours());
        return dto;
    }
}
