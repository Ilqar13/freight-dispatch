package az.freight.client;

import az.freight.config.RoutingProperties;
import az.freight.dto.RouteInfo;
import az.freight.util.GeoUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Düz xətt məsafəsi x düzəliş əmsalı. Marşrut serveri olmadan işləyir (ehtiyat və lokal işləmə üçün). */
@Component
@ConditionalOnProperty(name = "routing.provider", havingValue = "haversine", matchIfMissing = true)
public class HaversineRouteClient implements RouteClient {

    private final RoutingProperties props;

    public HaversineRouteClient(RoutingProperties props) {
        this.props = props;
    }

    @Override
    public RouteInfo route(double fromLat, double fromLon, double toLat, double toLon) {
        double km = GeoUtils.haversineKm(fromLat, fromLon, toLat, toLon) * props.roadFactor();
        return new RouteInfo(km, km / props.averageSpeedKmh());
    }
}
