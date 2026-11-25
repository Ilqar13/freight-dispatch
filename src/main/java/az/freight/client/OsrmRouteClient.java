package az.freight.client;

import az.freight.config.RoutingProperties;
import az.freight.dto.RouteInfo;
import az.freight.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Real yol məsafəsi: OSRM serveri. routing.provider=osrm olduqda aktivləşir. */
@Component
@ConditionalOnProperty(name = "routing.provider", havingValue = "osrm")
public class OsrmRouteClient implements RouteClient {

    private final RestClient rest;

    public OsrmRouteClient(RoutingProperties props) {
        this.rest = RestClient.builder().baseUrl(props.osrmUrl()).build();
    }

    @Override
    public RouteInfo route(double fromLat, double fromLon, double toLat, double toLon) {
        JsonNode json = rest.get()
                .uri("/route/v1/driving/{a},{b};{c},{d}?overview=false", fromLon, fromLat, toLon, toLat)
                .retrieve()
                .body(JsonNode.class);
        if (json == null || !"Ok".equals(json.path("code").asText())) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Marşrut serveri cavab vermədi");
        }
        JsonNode r = json.path("routes").get(0);
        return new RouteInfo(r.path("distance").asDouble() / 1000.0, r.path("duration").asDouble() / 3600.0);
    }
}
