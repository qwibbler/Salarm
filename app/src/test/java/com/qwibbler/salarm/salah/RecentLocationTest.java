package com.qwibbler.salarm.salah;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RecentLocationTest {

    @Test
    public void testRecentLocationSerialization() {
        RecentLocation loc = new RecentLocation("Makkah", "Makkah Region", "Saudi Arabia", "SA", 21.4225, 39.8262, "Asia/Riyadh", false, 123456789L);

        assertEquals("Makkah", loc.getCityName());
        assertEquals("Makkah Region", loc.getProvince());
        assertEquals("Saudi Arabia", loc.getCountry());
        assertEquals("SA", loc.getCountryCode());
        assertEquals(21.4225, loc.getLatitude(), 0.0001);
        assertEquals(39.8262, loc.getLongitude(), 0.0001);
        assertEquals("Asia/Riyadh", loc.getTimeZoneId());
        assertEquals("Makkah Region, Saudi Arabia", loc.getFullLocation());

        String jsonStr = loc.toJsonString();
        assertNotNull(jsonStr);

        RecentLocation deserialized = RecentLocation.fromJsonString(jsonStr);
        assertNotNull(deserialized);
        assertEquals(loc.getCityName(), deserialized.getCityName());
        assertEquals(loc.getLatitude(), deserialized.getLatitude(), 0.0001);
        assertEquals(loc.getLongitude(), deserialized.getLongitude(), 0.0001);
        assertEquals(loc.getTimeZoneId(), deserialized.getTimeZoneId());
    }

    @Test
    public void testCustomLocationFlag() {
        RecentLocation custom = new RecentLocation("My Custom Location", "Custom", "", "", 24.8607, 67.0011, "Asia/Karachi", true, System.currentTimeMillis());

        assertTrue(custom.isCustom());
        assertEquals("My Custom Location", custom.getCityName());
    }
}
