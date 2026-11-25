package az.freight.client;

import az.freight.dto.RouteInfo;

/** Marşrut təchizatçısı. İndi: Haversine (təxmini) və OSRM (real yol). Sonra: Google/Mapbox əlavə etmək olar. */
public interface RouteClient {
    RouteInfo route(double fromLat, double fromLon, double toLat, double toLon);
}
