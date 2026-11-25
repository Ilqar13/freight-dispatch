package az.freight.filter;

import az.freight.config.DispatchProperties;
import az.freight.entity.Order;
import az.freight.entity.Truck;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** GPS məlumatı köhnədirsə maşının yeri etibarsızdır. */
@Component
public class PositionFreshnessFilter implements TruckFilter {

    private final DispatchProperties props;

    public PositionFreshnessFilter(DispatchProperties props) {
        this.props = props;
    }

    @Override
    public Optional<String> reject(Truck truck, Order order) {
        Duration age = Duration.between(truck.getPositionUpdatedAt(), Instant.now());
        return age.toMinutes() > props.maxPositionAgeMinutes()
                ? Optional.of("GPS məlumatı köhnədir (" + age.toMinutes() + " dəq.)")
                : Optional.empty();
    }
}
