package az.freight.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dispatch")
public record DispatchProperties(
        int candidateLimit,
        int maxPositionAgeMinutes,
        double wastePenaltyPerTon,
        double urgentWastePenaltyPerTon,
        double urgentMinutesWeight) {}
