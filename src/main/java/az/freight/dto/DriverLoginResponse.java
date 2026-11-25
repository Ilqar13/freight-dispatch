package az.freight.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverLoginResponse {
    private String token;
    private Instant expiresAt;
    private DriverDto driver;
}
