package az.freight.utilTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import az.freight.util.GeoUtils;
import org.junit.jupiter.api.Test;

class GeoUtilsTest {
    @Test
    void bakuToGanjaIsAbout300Km() {
        double km = GeoUtils.haversineKm(40.41, 49.87, 40.68, 46.36);
        assertTrue(km > 290 && km < 305, "km=" + km);
    }

    @Test
    void samePointIsZero() {
        assertEquals(0.0, GeoUtils.haversineKm(40.4, 49.8, 40.4, 49.8), 1e-9);
    }
}
