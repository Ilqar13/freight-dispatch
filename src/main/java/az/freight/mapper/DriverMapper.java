package az.freight.mapper;

import az.freight.dto.DriverDto;
import az.freight.dto.TruckDto;
import az.freight.entity.Driver;
import az.freight.entity.Truck;
import org.springframework.stereotype.Component;

@Component
public class DriverMapper {

    private final TruckMapper truckMapper;

    public DriverMapper(TruckMapper truckMapper) {
        this.truckMapper = truckMapper;
    }

    /** truck null ola bilər (maşına bağlanmamış sürücü). PİN hash heç vaxt DTO-ya düşmür. */
    public DriverDto toDto(Driver e, Truck truck, boolean includeTruck) {
        if (e == null) return null;
        DriverDto dto = new DriverDto();
        dto.setId(e.getId());
        dto.setFullName(e.getFullName());
        dto.setPhone(e.getPhone());
        dto.setActive(e.isActive());
        dto.setTruckId(e.getTruckId());
        dto.setTruckPlate(truck == null ? null : truck.getPlate());
        dto.setNationalId(e.getNationalId());
        dto.setBirthDate(e.getBirthDate());
        dto.setLicenseNumber(e.getLicenseNumber());
        dto.setLicenseCategories(e.getLicenseCategories());
        dto.setLicenseExpiry(e.getLicenseExpiry());
        dto.setExperienceYears(e.getExperienceYears());
        dto.setAddress(e.getAddress());
        dto.setEmail(e.getEmail());
        dto.setEmergencyContactName(e.getEmergencyContactName());
        dto.setEmergencyContactPhone(e.getEmergencyContactPhone());
        dto.setHireDate(e.getHireDate());
        dto.setMedicalExpiry(e.getMedicalExpiry());
        dto.setAdrCertified(e.isAdrCertified());
        dto.setNotes(e.getNotes());
        dto.setCreatedAt(e.getCreatedAt());
        if (includeTruck && truck != null) {
            TruckDto t = truckMapper.toDto(truck);
            dto.setTruck(t);
        }
        return dto;
    }
}
