package com.best.deskclock.namaz;

import java.util.Locale;
import java.util.TimeZone;

public class NamazTimezoneUtils {

    /**
     * Guesses the timezone ID for a given location string (e.g., "Lahore, Punjab, Pakistan").
     * @param fullLocation The full comma-separated location string.
     * @return A guessed TimeZone ID, or null if no reliable match is found.
     */
    public static String guessTimezone(String fullLocation) {
        if (fullLocation == null || fullLocation.isEmpty()) return null;

        String[] parts = fullLocation.split(",");
        String cityPart = parts[0].trim().toLowerCase(Locale.getDefault());
        if (cityPart.isEmpty()) return null;

        String cityUnderscore = cityPart.replace(" ", "_");
        String[] ids = TimeZone.getAvailableIDs();

        // 1. Precise match: ID ends with /City or /City_Name
        for (String id : ids) {
            String idLower = id.toLowerCase(Locale.getDefault());
            if (idLower.endsWith("/" + cityPart) || idLower.endsWith("/" + cityUnderscore)) {
                return id;
            }
        }

        // 2. Fuzzy match: ID contains city name
        for (String id : ids) {
            if (id.toLowerCase(Locale.getDefault()).contains(cityUnderscore)) {
                return id;
            }
        }

        // 3. Try matching by country if present
        if (parts.length > 1) {
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i].trim().toLowerCase(Locale.getDefault());
                String partUnderscore = part.replace(" ", "_");
                if (part.isEmpty()) continue;

                for (String id : ids) {
                    String idLower = id.toLowerCase(Locale.getDefault());
                    if (idLower.contains(part) || idLower.contains(partUnderscore)) {
                        return id;
                    }
                }
            }
        }

        return null;
    }
}
