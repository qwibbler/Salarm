package com.qwibbler.salarm.namaz;

import com.qwibbler.salarm.utils.LogUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Client for Nominatim (OpenStreetMap) Geocoding API.
 */
public class NominatimClient {

    private static final String SEARCH_URL = "https://nominatim.openstreetmap.org/search?q=%s&format=json&addressdetails=1&limit=10&accept-language=%s";
    private static final String REVERSE_URL = "https://nominatim.openstreetmap.org/reverse?lat=%f&lon=%f&format=json&accept-language=%s";

    public static class SearchResult {
        public String name;
        public String province;
        public String country;
        public String countryCode;
        public double latitude;
        public double longitude;

        @Override
        public String toString() {
            return name + (province != null ? ", " + province : "") + (country != null ? ", " + country : "");
        }
    }

    public static List<SearchResult> search(String query) {
        List<SearchResult> results = new ArrayList<>();
        try {
            String language = Locale.getDefault().getLanguage();
            URL url = new URL(String.format(Locale.US, SEARCH_URL, URLEncoder.encode(query, "UTF-8"), language));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "Salarm-Android-App");

            if (conn.getResponseCode() == 200) {
                try (InputStream is = conn.getInputStream()) {
                    java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                    byte[] data = new byte[8192];
                    int length;
                    while ((length = is.read(data)) != -1) {
                        buffer.write(data, 0, length);
                    }
                    String json = buffer.toString("UTF-8");
                    JSONArray array = new JSONArray(json);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        JSONObject address = obj.optJSONObject("address");
                        if (address == null) continue;

                        SearchResult res = new SearchResult();
                        res.latitude = obj.getDouble("lat");
                        res.longitude = obj.getDouble("lon");

                        // Try to find a suitable name
                        res.name = address.optString("city", address.optString("town", address.optString("village", address.optString("hamlet", address.optString("municipality", address.optString("suburb", ""))))));
                        if (res.name.isEmpty()) {
                             res.name = obj.optString("display_name").split(",")[0];
                        }

                        res.province = address.optString("state", address.optString("region", ""));
                        res.country = address.optString("country", "");
                        res.countryCode = address.optString("country_code", "");

                        results.add(res);
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.e("Error searching Nominatim", e);
        }
        return results;
    }

    public static String reverseGeocode(double lat, double lon) {
        SearchResult res = reverseGeocodeFull(lat, lon);
        if (res != null) {
            StringBuilder sb = new StringBuilder();
            if (!res.name.isEmpty()) sb.append(res.name);
            if (res.province != null && !res.province.isEmpty() && !res.province.equalsIgnoreCase(res.name)) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(res.province);
            }
            if (res.country != null && !res.country.isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(res.country);
            }
            return sb.toString();
        }
        return null;
    }

    public static SearchResult reverseGeocodeFull(double lat, double lon) {
        try {
            String language = Locale.getDefault().getLanguage();
            URL url = new URL(String.format(Locale.US, REVERSE_URL, lat, lon, language));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "Salarm-Android-App");

            if (conn.getResponseCode() == 200) {
                try (InputStream is = conn.getInputStream()) {
                    java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                    byte[] data = new byte[8192];
                    int length;
                    while ((length = is.read(data)) != -1) {
                        buffer.write(data, 0, length);
                    }
                    String json = buffer.toString("UTF-8");
                    JSONObject obj = new JSONObject(json);
                    JSONObject address = obj.optJSONObject("address");
                    if (address != null) {
                        SearchResult res = new SearchResult();
                        res.latitude = obj.optDouble("lat", lat);
                        res.longitude = obj.optDouble("lon", lon);
                        res.name = address.optString("city", address.optString("town", address.optString("village", address.optString("hamlet", address.optString("municipality", "")))));
                        res.province = address.optString("state", address.optString("region", ""));
                        res.country = address.optString("country", "");
                        res.countryCode = address.optString("country_code", "");
                        return res;
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.e("Error reverse geocoding Nominatim", e);
        }
        return null;
    }
}
