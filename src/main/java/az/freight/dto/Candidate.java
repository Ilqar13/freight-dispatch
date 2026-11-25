package az.freight.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Candidate {
    private Long truckId;
    private String plate;
    private String type;
    private double capacityTons;
    private double distanceKm;
    private double etaHours;
    private double cost;
    private double score;
    private List<String> reasons;
}
