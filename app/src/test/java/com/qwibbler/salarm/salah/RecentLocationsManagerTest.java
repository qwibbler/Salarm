package com.qwibbler.salarm.salah;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class RecentLocationsManagerTest {

    @Test
    public void testRecentLocationModel() {
        RecentLocation loc = new RecentLocation("Lahore", "Punjab", "Pakistan", "PK", 31.5204, 74.3587, "Asia/Karachi", false, System.currentTimeMillis());
        assertEquals("Lahore", loc.getCityName());
        assertEquals("Punjab, Pakistan", loc.getFullLocation());

        String str = loc.toJsonString();
        RecentLocation parsed = RecentLocation.fromJsonString(str);
        assertNotNull(parsed);
        assertEquals("Lahore", parsed.getCityName());
        assertEquals("Asia/Karachi", parsed.getTimeZoneId());
    }
}
