package com.qwibbler.salarm.salah;

import static com.qwibbler.salarm.SalarmApplication.getDefaultSharedPreferences;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.DEFAULT_INTERNET_ACCESS_ALLOWED;
import static com.qwibbler.salarm.settings.PreferencesKeys.KEY_INTERNET_ACCESS_ALLOWED;

import android.content.Context;
import android.content.SharedPreferences;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.Madhab;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.settings.PreferencesDefaultValues;
import com.qwibbler.salarm.settings.PreferencesKeys;
import com.qwibbler.salarm.utils.LogUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.json.JSONObject;
import com.qwibbler.salarm.data.City;
import com.qwibbler.salarm.data.DataModel;
import java.util.Map;
import java.util.HashMap;

/**
 * Shared utility methods for Salah features.
 */
public class SalahUtils {
    /**
     * Retrieves the current Salah coordinates from preferences.
     */
    public static double[] getCoordinates(Context context) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        String latStr = prefs.getString(PreferencesKeys.KEY_SALAH_LATITUDE, PreferencesDefaultValues.DEFAULT_SALAH_LATITUDE);
        String lonStr = prefs.getString(PreferencesKeys.KEY_SALAH_LONGITUDE, PreferencesDefaultValues.DEFAULT_SALAH_LONGITUDE);
        try {
            double lat = Double.parseDouble(latStr);
            double lon = Double.parseDouble(lonStr);
            return new double[]{lat, lon};
        } catch (NumberFormatException e) {
            LogUtils.e("Error parsing coordinates from preferences", e);
            return new double[]{0.0, 0.0};
        }
    }

    /**
     * Retrieves the current Salah calculation method from preferences.
     */
    public static CalculationMethod getCalculationMethod(Context context) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        String methodStr = prefs.getString(PreferencesKeys.KEY_SALAH_CALC_METHOD, PreferencesDefaultValues.DEFAULT_SALAH_CALC_METHOD);
        try {
            return CalculationMethod.valueOf(methodStr);
        } catch (IllegalArgumentException e) {
            LogUtils.e("Invalid calculation method in preferences: " + methodStr, e);
            return CalculationMethod.valueOf(PreferencesDefaultValues.DEFAULT_SALAH_CALC_METHOD);
        }
    }

    /**
     * Retrieves the current Madhab from preferences.
     */
    public static Madhab getMadhab(Context context) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        String madhabStr = prefs.getString(PreferencesKeys.KEY_SALAH_MADHAB, PreferencesDefaultValues.DEFAULT_SALAH_MADHAB);
        try {
            return Madhab.valueOf(madhabStr);
        } catch (IllegalArgumentException e) {
            LogUtils.e("Invalid madhab in preferences: " + madhabStr, e);
            return Madhab.valueOf(PreferencesDefaultValues.DEFAULT_SALAH_MADHAB);
        }
    }

    /**
     * Unified reverse geocoding to get a city name from coordinates.
     */
    public static void updateCityNameFromCoords(Context context, double lat, double lon) {
        final Context appContext = context.getApplicationContext();

        AppExecutors.getNetworkIO().execute(() -> {
            // 1. Try local cache/JSON first (Priority 1 & 2)
            OfflineCity match = findNearestOfflineCity(appContext, lat, lon);
            if (match != null) {
                String displayName = match.isNear ? appContext.getString(com.qwibbler.salarm.R.string.salah_near_city, match.name) : match.name;
                getDefaultSharedPreferences(appContext).edit()
                        .putString(PreferencesKeys.KEY_SALAH_CITY_NAME, displayName)
                        .apply();
                return;
            }

            // 2. Try online lookup if allowed (Priority 3)
            if (getDefaultSharedPreferences(appContext).getBoolean(KEY_INTERNET_ACCESS_ALLOWED, DEFAULT_INTERNET_ACCESS_ALLOWED)) {
                NominatimClient.SearchResult res = NominatimClient.reverseGeocodeFull(lat, lon);
                if (res != null) {
                    String locationName = res.toString();
                    getDefaultSharedPreferences(appContext).edit()
                            .putString(PreferencesKeys.KEY_SALAH_CITY_NAME, locationName)
                            .apply();

                    // Save successful online lookup to Room cache (normalized)
                    double normLat = Math.round(lat * 1000.0) / 1000.0;
                    double normLon = Math.round(lon * 1000.0) / 1000.0;
                    String nameKey = String.format(Locale.US, "%s|%s|%s", res.name, res.province, res.country);

                    CityCoordinate cached = new CityCoordinate(nameKey, res.name, res.province, res.country, normLat, normLon, res.countryCode, SalahTimezoneUtils.guessTimezone(locationName));
                    SalahDatabase.getDatabase(appContext).cityCoordinateDao().insert(cached);
                    return;
                }
            }

            // 3. Fallback (Priority 4)
            String fallbackName = String.format(Locale.getDefault(), "Lat: %.4f, Lon: %.4f", lat, lon);
            getDefaultSharedPreferences(appContext).edit()
                    .putString(PreferencesKeys.KEY_SALAH_CITY_NAME, fallbackName)
                    .apply();
        });
    }

    public static class OfflineCity {
        public String name;
        public String timezoneId;
        public double latitude;
        public double longitude;
        public boolean isNear;
    }

    private static Map<String, Object[]> sBuiltInCoords;

    private static Map<String, Object[]> getBuiltInCoords(Context context) {
        if (sBuiltInCoords == null) {
            sBuiltInCoords = new HashMap<>();
            try (InputStream is = context.getAssets().open("city_coordinates.json")) {
                java.io.ByteArrayOutputStream result = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int length;
                while ((length = is.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                String json = result.toString("UTF-8");
                JSONObject obj = new JSONObject(json);
                java.util.Iterator<String> keys = obj.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    try {
                        JSONObject coord = obj.getJSONObject(key);
                        sBuiltInCoords.put(key, new Object[]{
                                coord.getDouble("lat"),
                                coord.getDouble("lon"),
                                coord.optString("tz", "")
                        });
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception e) {
                LogUtils.e("Error loading built-in coordinates", e);
            }
        }
        return sBuiltInCoords;
    }

    /**
     * Calculates distance between two points in meters using Haversine formula.
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Radius of the earth in meters
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public static OfflineCity findNearestOfflineCity(Context context, double lat, double lon) {
        OfflineCity bestMatch = null;
        double minDistance = 80467; // 50 miles threshold

        // 1. Check local database cache (Priority 1)
        List<CityCoordinate> cachedResults = SalahDatabase.getDatabase(context).cityCoordinateDao().getAll();
        for (CityCoordinate cached : cachedResults) {
            double distance = calculateDistance(lat, lon, cached.latitude, cached.longitude);
            if (distance < minDistance) {
                minDistance = distance;
                bestMatch = new OfflineCity();
                bestMatch.name = cached.cityName + (cached.country != null && !cached.country.isEmpty() ? ", " + cached.country : "");
                bestMatch.latitude = cached.latitude;
                bestMatch.longitude = cached.longitude;
                bestMatch.timezoneId = cached.timeZoneId;
                bestMatch.isNear = distance > 32187; // > 20 miles
            }
        }

        if (bestMatch != null) return bestMatch;

        // 2. Check built-in cities (Priority 2)
        final List<City> cities = new ArrayList<>();
        try {
            DataModel.getDataModel().run(() -> cities.addAll(DataModel.getDataModel().getAllCities()));
        } catch (Exception e) {
            LogUtils.e("Error fetching cities from DataModel", e);
        }

        Map<String, Object[]> builtInCoords = getBuiltInCoords(context);
        for (City c : cities) {
            Object[] data = builtInCoords.get(c.getId());
            if (data != null) {
                double cLat = (double) data[0];
                double cLon = (double) data[1];
                String tz = (data.length > 2 && data[2] instanceof String s && !s.isEmpty()) ? s : c.getTimeZone().getID();

                double distance = calculateDistance(lat, lon, cLat, cLon);
                if (distance < minDistance) {
                    minDistance = distance;
                    bestMatch = new OfflineCity();
                    bestMatch.name = c.getName();
                    bestMatch.latitude = cLat;
                    bestMatch.longitude = cLon;
                    bestMatch.timezoneId = tz;
                    bestMatch.isNear = distance > 32187; // > 20 miles
                }
            }
        }

        return bestMatch;
    }
}
