package az.freight.filter;

import az.freight.entity.Order;
import az.freight.entity.Truck;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Yükün xüsusi tələbləri: ADR (təhlükəli yük), həcm, temperatur diapazonu. */
@Component
public class CargoRequirementsFilter implements TruckFilter {

    @Override
    public Optional<String> reject(Truck t, Order o) {
        if (o.isHazardous() && !t.isAdrCertified()) {
            return Optional.of("Təhlükəli yük (ADR) üçün maşının ADR icazəsi yoxdur");
        }
        if (o.getVolumeM3() != null && t.getVolumeM3() != null && t.getVolumeM3() < o.getVolumeM3()) {
            return Optional.of("Kuzov həcmi kifayət etmir (" + t.getVolumeM3() + " m³ < " + o.getVolumeM3() + " m³)");
        }
        // Temperatur: maşının diapazonu sifarişin diapazonunu əhatə etməlidir (maşında diapazon göstərilibsə)
        if (o.getTempMin() != null && t.getTempMin() != null && t.getTempMin() > o.getTempMin()) {
            return Optional.of("Maşın tələb olunan minimum temperaturu təmin edə bilmir");
        }
        if (o.getTempMax() != null && t.getTempMax() != null && t.getTempMax() < o.getTempMax()) {
            return Optional.of("Maşın tələb olunan maksimum temperaturu təmin edə bilmir");
        }
        return Optional.empty();
    }
}
