package az.freight.mapper;

import az.freight.dto.OrderDto;
import az.freight.entity.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderDto toDto(Order e) {
        if (e == null) return null;
        OrderDto d = new OrderDto();
        d.setId(e.getId());
        d.setOrderNo(e.getOrderNo());
        d.setCustomerId(e.getCustomerId());
        d.setCustomerName(e.getCustomerName());
        d.setContactName(e.getContactName());
        d.setContactPhone(e.getContactPhone());
        d.setContactEmail(e.getContactEmail());
        d.setTaxId(e.getTaxId());
        d.setReferenceNo(e.getReferenceNo());

        d.setPickupAddress(e.getPickupAddress());
        d.setPickupLat(e.getPickupLat());
        d.setPickupLon(e.getPickupLon());
        d.setPickupContactName(e.getPickupContactName());
        d.setPickupContactPhone(e.getPickupContactPhone());
        d.setPickupFrom(e.getPickupFrom());
        d.setPickupTo(e.getPickupTo());

        d.setDestAddress(e.getDestAddress());
        d.setLat(e.getDestLat());
        d.setLon(e.getDestLon());
        d.setDestContactName(e.getDestContactName());
        d.setDestContactPhone(e.getDestContactPhone());
        d.setDeliveryFrom(e.getDeliveryFrom());
        d.setDeliveryTo(e.getDeliveryTo());

        d.setCargoType(e.getCargoType());
        d.setTons(e.getTons());
        d.setCargoDescription(e.getCargoDescription());
        d.setVolumeM3(e.getVolumeM3());
        d.setPackagesCount(e.getPackagesCount());
        d.setPackageType(e.getPackageType());
        d.setDeclaredValue(e.getDeclaredValue());
        d.setHazardous(e.isHazardous());
        d.setUnNumber(e.getUnNumber());
        d.setTempMin(e.getTempMin());
        d.setTempMax(e.getTempMax());
        d.setFragile(e.isFragile());
        d.setStackable(e.isStackable());
        d.setUrgent(e.isUrgent());

        d.setNeedsLoadingHelp(e.isNeedsLoadingHelp());
        d.setNeedsUnloadingHelp(e.isNeedsUnloadingHelp());
        d.setNeedsInsurance(e.isNeedsInsurance());
        d.setSpecialInstructions(e.getSpecialInstructions());

        d.setPrice(e.getPrice());
        d.setCurrency(e.getCurrency());
        d.setPaymentMethod(e.getPaymentMethod());
        d.setPayer(e.getPayer());
        d.setInvoiceRequired(e.isInvoiceRequired());

        d.setStatus(e.getStatus());
        d.setCreatedAt(e.getCreatedAt());
        return d;
    }

    public Order toEntity(OrderDto d) {
        if (d == null) return null;
        Order e = new Order();
        e.setCustomerName(trim(d.getCustomerName()));
        e.setContactName(blankToNull(d.getContactName()));
        e.setContactPhone(blankToNull(d.getContactPhone()));
        e.setContactEmail(blankToNull(d.getContactEmail()));
        e.setTaxId(blankToNull(d.getTaxId()));
        e.setReferenceNo(blankToNull(d.getReferenceNo()));

        e.setPickupAddress(blankToNull(d.getPickupAddress()));
        e.setPickupLat(d.getPickupLat());
        e.setPickupLon(d.getPickupLon());
        e.setPickupContactName(blankToNull(d.getPickupContactName()));
        e.setPickupContactPhone(blankToNull(d.getPickupContactPhone()));
        e.setPickupFrom(d.getPickupFrom());
        e.setPickupTo(d.getPickupTo());

        e.setDestAddress(blankToNull(d.getDestAddress()));
        e.setDestLat(d.getLat());
        e.setDestLon(d.getLon());
        e.setDestContactName(blankToNull(d.getDestContactName()));
        e.setDestContactPhone(blankToNull(d.getDestContactPhone()));
        e.setDeliveryFrom(d.getDeliveryFrom());
        e.setDeliveryTo(d.getDeliveryTo());

        e.setCargoType(d.getCargoType());
        e.setTons(d.getTons());
        e.setCargoDescription(blankToNull(d.getCargoDescription()));
        e.setVolumeM3(d.getVolumeM3());
        e.setPackagesCount(d.getPackagesCount());
        e.setPackageType(d.getPackageType());
        e.setDeclaredValue(d.getDeclaredValue());
        e.setHazardous(d.isHazardous());
        e.setUnNumber(d.isHazardous() ? blankToNull(d.getUnNumber()) : null);
        e.setTempMin(d.getTempMin());
        e.setTempMax(d.getTempMax());
        e.setFragile(d.isFragile());
        e.setStackable(d.isStackable());
        e.setUrgent(d.isUrgent());

        e.setNeedsLoadingHelp(d.isNeedsLoadingHelp());
        e.setNeedsUnloadingHelp(d.isNeedsUnloadingHelp());
        e.setNeedsInsurance(d.isNeedsInsurance());
        e.setSpecialInstructions(blankToNull(d.getSpecialInstructions()));

        e.setPrice(d.getPrice());
        e.setCurrency(d.getCurrency() == null || d.getCurrency().isBlank() ? "AZN" : d.getCurrency());
        e.setPaymentMethod(d.getPaymentMethod());
        e.setPayer(d.getPayer());
        e.setInvoiceRequired(d.isInvoiceRequired());
        return e;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
