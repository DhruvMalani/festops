package com.festops.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the Haversine great-circle distance against known reference values.
 */
class HaversineUtilTest {

    /** One degree of arc ≈ 111.19 km (2πR/360 with R = 6371 km). */
    private static final double ONE_DEGREE_KM = 111.19;

    @Test
    void samePointIsZero() {
        assertEquals(0.0, HaversineUtil.distanceKm(28.3635, 75.5870, 28.3635, 75.5870), 1e-6);
    }

    @Test
    void oneDegreeOfLongitudeAtEquator() {
        assertEquals(ONE_DEGREE_KM, HaversineUtil.distanceKm(0.0, 0.0, 0.0, 1.0), 0.5);
    }

    @Test
    void oneDegreeOfLatitude() {
        assertEquals(ONE_DEGREE_KM, HaversineUtil.distanceKm(0.0, 0.0, 1.0, 0.0), 0.5);
    }

    @Test
    void londonToParisIsAboutThreeFortyThreeKm() {
        // London (51.5074, -0.1278) -> Paris (48.8566, 2.3522): ~343 km great-circle
        double d = HaversineUtil.distanceKm(51.5074, -0.1278, 48.8566, 2.3522);
        assertEquals(343.0, d, 5.0);
    }

    @Test
    void isSymmetric() {
        double ab = HaversineUtil.distanceKm(28.3635, 75.5870, 28.3641, 75.5878);
        double ba = HaversineUtil.distanceKm(28.3641, 75.5878, 28.3635, 75.5870);
        assertEquals(ab, ba, 1e-9);
    }

    @Test
    void shortCampusHopIsRoughlyAHundredMetres() {
        // ~0.0006° lat + ~0.0008° lng around BITS Pilani -> ~0.1 km
        double km = HaversineUtil.distanceKm(28.3635, 75.5870, 28.3641, 75.5878);
        assertTrue(km > 0.05 && km < 0.20, "expected ~0.1 km but was " + km + " km");
    }
}
