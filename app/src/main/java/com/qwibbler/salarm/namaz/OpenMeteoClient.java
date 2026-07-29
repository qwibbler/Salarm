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
 * Client for Open-Meteo Geocoding API.
 */
public class OpenMeteoClient {

    private static final String SEARCH_URL = "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=10&language=%s&format=json";

    public static class SearchResult {
        public String name;
        public String province;
        public String country;
        public String countryCode;
        public String timezone;
        public double latitude;
        public double longitude;

        @Override
        public String toString() {
            return name + (province != null && !province.isEmpty() ? ", " + province : "") + (country != null && !country.isEmpty() ? ", " + country : "");
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
                    JSONObject root = new JSONObject(json);
                    JSONArray array = root.optJSONArray("results");
                    if (array != null) {
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            SearchResult res = new SearchResult();
                            res.name = obj.optString("name");
                            res.latitude = obj.getDouble("latitude");
                            res.longitude = obj.getDouble("longitude");
                            res.timezone = obj.optString("timezone");
                            res.country = obj.optString("country");
                            res.countryCode = obj.optString("country_code");
                            res.province = obj.optString("admin1");
                            results.add(res);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.e("Error searching Open-Meteo", e);
        }
        return results;
    }
}
