package az.freight.service;

import az.freight.config.DispatchProperties;
import az.freight.dto.RouteInfo;
import az.freight.entity.Order;
import az.freight.entity.Truck;
import org.springframework.stereotype.Service;

/** Xal nə qədər aşağıdırsa, maşın o qədər yaxşıdır. Çəkilər application.yml-dən gəlir. */
@Service
public class ScoringService {

    private final DispatchProperties p;

    public ScoringService(DispatchProperties p) {
        this.p = p;
    }

    public double cost(Truck t, RouteInfo r) {
        return r.km() * t.getCostPerKm();
    }

    public double wasteTons(Truck t, Order o) {
        return Math.max(0, t.getCapacityTons() - o.getTons());
    }

    public double score(Order o, Truck t, RouteInfo r) {
        double waste = wasteTons(t, o);
        if (o.isUrgent()) {
            return r.hours() * p.urgentMinutesWeight() + waste * p.urgentWastePenaltyPerTon();
        }
        return cost(t, r) + waste * p.wastePenaltyPerTon();
    }

    /**
     * Əvəzedici maşın xalı: yük yolda ilişib qaldığı üçün əsas meyar nasazlıq nöqtəsinə çatma vaxtıdır
     * (dəqiqə x çəki). Boş tutum itkisi və yükü sonra təyinata aparmağın xərci kiçik əlavədir.
     */
    public double rescueScore(Order o, Truck t, RouteInfo pickup, RouteInfo onward) {
        return pickup.hours() * p.urgentMinutesWeight()
                + wasteTons(t, o) * p.urgentWastePenaltyPerTon()
                + cost(t, onward) * 0.05;
    }
}
