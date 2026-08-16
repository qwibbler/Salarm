package com.qwibbler.salarm.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.TimeZone;

public class CityTest {

    @Test
    public void testMatches() {
        City city = new City("C1", 1, "R", "Riyadh", "Riyadh", "Riyadh",
                             TimeZone.getTimeZone("Asia/Riyadh"), "Saudi Arabia",
                             Arrays.asList("Riaz", "Riadh"));

        // Match by name
        assertTrue(city.matches(prep("Riyadh")));
        assertTrue(city.matches(prep("riy")));

        // Match by country
        assertTrue(city.matches(prep("Saudi Arabia")));
        assertTrue(city.matches(prep("saudi")));

        // Match by alternative names
        assertTrue(city.matches(prep("Riaz")));
        assertTrue(city.matches(prep("riadh")));

        // Match with special characters removal
        assertTrue(city.matches(prep("Saudi-Arabia")));
        assertTrue(city.matches(prep("S.A.U.D.I")));

        // Non-matches
        assertFalse(city.matches(prep("Dubai")));
    }

    private String prep(String query) {
        return City.removeSpecialCharacters(query.toUpperCase());
    }
}
