package az.freight.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "routing")
public record RoutingProperties(String provider, String osrmUrl, double roadFactor, double averageSpeedKmh) {}
