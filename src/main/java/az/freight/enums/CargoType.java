package az.freight.enums;

import java.util.List;
import java.util.Set;

/** Yükün tipi və ona uyğun maşın tipləri. Yeni tip əlavə etmək üçün bura bir sətir yazmaq kifayətdir. */
public enum CargoType {
    DRY_GOODS(Set.of(TruckType.TENT, TruckType.CONTAINER, TruckType.BOX)),
    FROZEN_FOOD(Set.of(TruckType.REFRIGERATED)),
    LIQUID(Set.of(TruckType.TANKER)),
    CONTAINER(Set.of(TruckType.CONTAINER, TruckType.FLATBED)),
    HEAVY_MACHINERY(Set.of(TruckType.FLATBED)),
    /** Qum, çınqıl, tikinti materialı (tökmə yük). */
    BULK_MATERIAL(Set.of(TruckType.DUMP));

    private final Set<TruckType> truckTypes;

    CargoType(Set<TruckType> truckTypes) {
        this.truckTypes = truckTypes;
    }

    public List<String> truckTypeNames() {
        return truckTypes.stream().map(Enum::name).toList();
    }
}
