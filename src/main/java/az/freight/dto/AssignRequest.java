package az.freight.dto;

import lombok.Data;

/** truckId boşdursa ən yaxşı namizəd avtomatik seçilir. */
@Data
public class AssignRequest {
    private Long truckId;
}
