package az.freight.filter;

import az.freight.entity.Order;
import az.freight.entity.Truck;
import java.util.Optional;

/** Sərt şərt. Rədd səbəbini qaytarır; boşdursa maşın keçir. Yeni qayda = yeni @Component. */
public interface TruckFilter {
    Optional<String> reject(Truck truck, Order order);
}
