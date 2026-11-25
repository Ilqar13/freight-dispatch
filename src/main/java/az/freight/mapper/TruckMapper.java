package az.freight.mapper;

import az.freight.dto.TruckDto;
import az.freight.entity.Truck;
import org.springframework.stereotype.Component;

@Component
public class TruckMapper {

    public TruckDto toDto(Truck e) {
        if (e == null) return null;
        TruckDto d = new TruckDto();
        d.setId(e.getId());
        d.setPlate(e.getPlate());
        d.setType(e.getType());
        d.setCapacityTons(e.getCapacityTons());
        d.setCostPerKm(e.getCostPerKm());
        d.setLat(e.getLat());
        d.setLon(e.getLon());
        d.setStatus(e.getStatus());
        d.setPositionUpdatedAt(e.getPositionUpdatedAt());

        d.setBrand(e.getBrand());
        d.setModel(e.getModel());
        d.setManufactureYear(e.getManufactureYear());
        d.setVin(e.getVin());
        d.setBodyLengthM(e.getBodyLengthM());
        d.setBodyWidthM(e.getBodyWidthM());
        d.setBodyHeightM(e.getBodyHeightM());
        d.setVolumeM3(e.getVolumeM3());
        d.setAxles(e.getAxles());
        d.setFuelType(e.getFuelType());
        d.setOwnerType(e.getOwnerType());
        d.setHasTailLift(e.isHasTailLift());
        d.setAdrCertified(e.isAdrCertified());
        d.setTempMin(e.getTempMin());
        d.setTempMax(e.getTempMax());
        d.setGpsDeviceId(e.getGpsDeviceId());
        d.setInsuranceExpiry(e.getInsuranceExpiry());
        d.setTechInspectionExpiry(e.getTechInspectionExpiry());
        d.setLastServiceDate(e.getLastServiceDate());
        d.setOdometerKm(e.getOdometerKm());
        d.setNotes(e.getNotes());
        return d;
    }

    /** Yeni maşın: status FREE, GPS vaxtı indi. */
    public Truck toEntity(TruckDto d) {
        if (d == null) return null;
        Truck e = new Truck();
        e.setLat(d.getLat());
        e.setLon(d.getLon());
        apply(e, d);
        return e;
    }

    /** Pasport sahələrini köçürür (status, GPS və versiya toxunulmaz qalır). */
    public void apply(Truck e, TruckDto d) {
        e.setPlate(d.getPlate().trim().toUpperCase());
        e.setType(d.getType());
        e.setCapacityTons(d.getCapacityTons());
        e.setCostPerKm(d.getCostPerKm());

        e.setBrand(blank(d.getBrand()));
        e.setModel(blank(d.getModel()));
        e.setManufactureYear(d.getManufactureYear());
        e.setVin(blank(d.getVin()) == null ? null : d.getVin().trim().toUpperCase());
        e.setBodyLengthM(d.getBodyLengthM());
        e.setBodyWidthM(d.getBodyWidthM());
        e.setBodyHeightM(d.getBodyHeightM());
        e.setVolumeM3(d.getVolumeM3());
        e.setAxles(d.getAxles());
        e.setFuelType(d.getFuelType());
        e.setOwnerType(d.getOwnerType() == null ? az.freight.enums.OwnerType.COMPANY : d.getOwnerType());
        e.setHasTailLift(d.isHasTailLift());
        e.setAdrCertified(d.isAdrCertified());
        e.setTempMin(d.getTempMin());
        e.setTempMax(d.getTempMax());
        e.setGpsDeviceId(blank(d.getGpsDeviceId()));
        e.setInsuranceExpiry(d.getInsuranceExpiry());
        e.setTechInspectionExpiry(d.getTechInspectionExpiry());
        e.setLastServiceDate(d.getLastServiceDate());
        e.setOdometerKm(d.getOdometerKm());
        e.setNotes(blank(d.getNotes()));
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
