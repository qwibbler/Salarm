package com.qwibbler.salarm.salah;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LocationResolutionTest {

    @Test
    public void testHaversineDistance() {
        // Karachi Coordinates
        double lat1 = 24.8607;
        double lon1 = 67.0011;

        // A point approx 10km north
        double lat2 = 24.9507;
        double lon2 = 67.0011;

        double distance = SalahUtils.calculateDistance(lat1, lon1, lat2, lon2);

        // Haversine for 0.09 deg lat is ~10007m
        assertEquals(10007, distance, 100);
    }

//        TODO: This test is not meaningful:
//    @Test
//    public void testThresholdLogic() {
//        // Thresholds used in findNearestOfflineCity:
//        // 32187 meters (20 miles)
//        // 80467 meters (50 miles)
//
//        double d1 = 10000; // 10km
//        double d2 = 40000; // 40km
//
//        assertFalse("10km should not be 'Near'", d1 > 32187);
//        assertTrue("40km should be 'Near'", d2 > 32187 && d2 <= 80467);
//    }

    @Test
    public void testCoordinateNormalization() {
        double lat = 24.8607492;
        double lon = 67.0011384;

        double normLat = Math.round(lat * 1000.0) / 1000.0;
        double normLon = Math.round(lon * 1000.0) / 1000.0;

        assertEquals(24.861, normLat, 0.0001);
        assertEquals(67.001, normLon, 0.0001);
    }
}
