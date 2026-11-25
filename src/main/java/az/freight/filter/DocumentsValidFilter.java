package az.freight.filter;

import az.freight.entity.Order;
import az.freight.entity.Truck;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Sığortası və ya texniki baxışı bitmiş maşın reysə çıxa bilməz. Tarix boşdursa (köhnə yazı) yoxlanmır. */
@Component
public class DocumentsValidFilter implements TruckFilter {

    @Override
    public Optional<String> reject(Truck t, Order order) {
        LocalDate today = LocalDate.now();
        if (t.getInsuranceExpiry() != null && t.getInsuranceExpiry().isBefore(today)) {
            return Optional.of("Sığorta müddəti bitib (" + t.getInsuranceExpiry() + ")");
        }
        if (t.getTechInspectionExpiry() != null && t.getTechInspectionExpiry().isBefore(today)) {
            return Optional.of("Texniki baxış müddəti bitib (" + t.getTechInspectionExpiry() + ")");
        }
        return Optional.empty();
    }
}
