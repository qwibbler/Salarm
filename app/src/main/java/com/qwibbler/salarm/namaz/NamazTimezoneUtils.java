package com.qwibbler.salarm.namaz;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.TimeZone;
import org.json.JSONObject;

public class NamazTimezoneUtils {

    /**
     * Fetches the TimeZone ID from an online API for the given coordinates.
     * Must be called from a background thread.
     */
    public static String fetchTimeZoneId(double lat, double lon) {
        try {
            URL url = new URL(String.format(Locale.US, "https://timeapi.io/api/TimeZone/coordinate?latitude=%f&longitude=%f", lat, lon));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                try (InputStream is = conn.getInputStream()) {
                    java.io.ByteArrayOutputStream result = new java.io.ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = is.read(buffer)) != -1) {
                        result.write(buffer, 0, length);
                    }
                    String json = result.toString("UTF-8");
                    JSONObject obj = new JSONObject(json);
                    return obj.getString("timeZone");
                }
            }
        } catch (Exception e) {
            com.qwibbler.salarm.utils.LogUtils.e("Error fetching TimeZone ID online", e);
        }
        return null;
    }

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
                if (part.isEmpty()) continue;

                // Special case for countries with clear main TZs
                if (part.contains("mongolia")) return "Asia/Ulaanbaatar";
                if (part.contains("pakistan")) return "Asia/Karachi";
                if (part.contains("india")) return "Asia/Kolkata";
                if (part.contains("saudi arabia")) return "Asia/Riyadh";
                if (part.contains("bangladesh")) return "Asia/Dhaka";
                if (part.contains("china")) return "Asia/Shanghai";
                if (part.contains("japan")) return "Asia/Tokyo";
                if (part.contains("turkey")) return "Europe/Istanbul";
                if (part.contains("germany")) return "Europe/Berlin";
                if (part.contains("france")) return "Europe/Paris";
                if (part.contains("united kingdom") || part.equals("uk")) return "Europe/London";
                if (part.contains("united states") || part.equals("us") || part.equals("usa")) return "America/New_York"; // Default to East Coast

                String partUnderscore = part.replace(" ", "_");
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
