package az.freight.serviceTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

import az.freight.config.DispatchProperties;
import az.freight.dto.RouteInfo;
import az.freight.entity.Order;
import az.freight.entity.Truck;
import az.freight.service.ScoringService;
import org.junit.jupiter.api.Test;

class ScoringServiceTest {
    private final ScoringService scorer = new ScoringService(new DispatchProperties(10, 15, 8, 2, 60));

    private Truck truck(double cap, double perKm) {
        Truck t = new Truck();
        t.setCapacityTons(cap);
        t.setCostPerKm(perKm);
        return t;
    }

    private Order order(double tons, boolean urgent) {
        Order o = new Order();
        o.setTons(tons);
        o.setUrgent(urgent);
        return o;
    }

    @Test
    void normalModePrefersCheaperTruck() {
        Order o = order(18, false);
        double cheapFar = scorer.score(o, truck(20, 1.0), new RouteInfo(300, 5));
        double pricyNear = scorer.score(o, truck(20, 1.5), new RouteInfo(100, 1.7));
        assertTrue(pricyNear < cheapFar);
    }

    @Test
    void urgentModePrefersFasterTruck() {
        Order o = order(18, true);
        double slow = scorer.score(o, truck(20, 0.5), new RouteInfo(300, 5));
        double fast = scorer.score(o, truck(20, 2.0), new RouteInfo(100, 1.7));
        assertTrue(fast < slow);
    }

    @Test
    void oversizedTruckIsPenalized() {
        Order o = order(10, false);
        RouteInfo r = new RouteInfo(100, 1.7);
        assertTrue(scorer.score(o, truck(12, 1.0), r) < scorer.score(o, truck(35, 1.0), r));
    }
}
