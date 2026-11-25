package az.freight.dto;

import lombok.Getter;
import lombok.Setter;

/** Sürücünün anketini dəyişir. Telefon və PİN ayrıca dəyişir. */
@Getter @Setter
public class DriverUpdateDto extends DriverProfileDto {
    private boolean active = true;
}
