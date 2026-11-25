package az.freight.mapper;

import az.freight.dto.IncidentDto;
import az.freight.entity.Assignment;
import az.freight.entity.Driver;
import az.freight.entity.Incident;
import az.freight.entity.Truck;
import org.springframework.stereotype.Component;

@Component
public class IncidentMapper {

    /** Əlavə obyektlər null ola bilər. */
    public IncidentDto toDto(Incident e, Truck truck, Driver driver,
                             Truck replacement, Driver replacementDriver, Assignment replacementAssignment) {
        if (e == null) return null;
        IncidentDto dto = new IncidentDto();
        dto.setId(e.getId());
        dto.setOrderId(e.getOrderId());
        dto.setType(e.getType());
        dto.setDescription(e.getDescription());
        dto.setLat(e.getLat());
        dto.setLon(e.getLon());
        dto.setStatus(e.getStatus());
        dto.setAttempts(e.getAttempts());
        dto.setLastAttemptAt(e.getLastAttemptAt());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setReplacementAssignedAt(e.getReplacementAssignedAt());
        dto.setClosedAt(e.getClosedAt());
        dto.setTruckId(e.getTruckId());
        dto.setTruckPlate(truck == null ? null : truck.getPlate());
        dto.setDriverName(driver == null ? null : driver.getFullName());
        dto.setDriverPhone(driver == null ? null : driver.getPhone());
        dto.setReplacementTruckId(e.getReplacementTruckId());
        dto.setReplacementPlate(replacement == null ? null : replacement.getPlate());
        dto.setReplacementDriverName(replacementDriver == null ? null : replacementDriver.getFullName());
        dto.setReplacementDriverPhone(replacementDriver == null ? null : replacementDriver.getPhone());
        if (replacementAssignment != null) {
            dto.setPickupKm(replacementAssignment.getPickupKm());
            dto.setPickupEtaHours(replacementAssignment.getPickupEtaHours());
        }
        return dto;
    }
}
