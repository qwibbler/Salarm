package com.qwibbler.salarm.salah;

import static com.qwibbler.salarm.SalarmApplication.getDefaultSharedPreferences;
import static com.qwibbler.salarm.settings.PreferencesKeys.KEY_SALAH_RECENT_LOCATIONS;

import android.content.Context;
import android.content.SharedPreferences;

import com.qwibbler.salarm.utils.LogUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manages in-memory and persistent storage of recently viewed cities, locations, and timezones in Salah times.
 */
public class RecentLocationsManager {

    private static final int MAX_RECENT_LOCATIONS = 15;
    private static List<RecentLocation> sRecentLocationsMemory = null;

    public static synchronized List<RecentLocation> getRecentLocations(Context context) {
        if (sRecentLocationsMemory == null) {
            loadRecentLocations(context);
        }
        return new ArrayList<>(sRecentLocationsMemory);
    }

    public static synchronized void addRecentLocation(Context context, RecentLocation newLoc) {
        if (newLoc == null || (newLoc.getLatitude() == 0 && newLoc.getLongitude() == 0 && newLoc.getCityName().isEmpty())) {
            return;
        }

        if (sRecentLocationsMemory == null) {
            loadRecentLocations(context);
        }

        // Deduplicate: remove any existing location with matching name/lat-lon
        Iterator<RecentLocation> iterator = sRecentLocationsMemory.iterator();
        while (iterator.hasNext()) {
            RecentLocation existing = iterator.next();
            if (isSameLocation(existing, newLoc)) {
                iterator.remove();
            }
        }

        newLoc.setTimestamp(System.currentTimeMillis());
        // Insert at index 0 (most recent first)
        sRecentLocationsMemory.add(0, newLoc);

        // Limit size
        while (sRecentLocationsMemory.size() > MAX_RECENT_LOCATIONS) {
            sRecentLocationsMemory.remove(sRecentLocationsMemory.size() - 1);
        }

        saveRecentLocations(context);
    }

    public static synchronized void addRecentLocation(Context context, String cityName, String province, String country,
                                                     String countryCode, double lat, double lon, String tzId, boolean isCustom) {
        RecentLocation loc = new RecentLocation(cityName, province, country, countryCode, lat, lon, tzId, isCustom, System.currentTimeMillis());
        addRecentLocation(context, loc);
    }

    private static boolean isSameLocation(RecentLocation loc1, RecentLocation loc2) {
        if (loc1 == null || loc2 == null) return false;

        boolean sameName = !loc1.getCityName().isEmpty()
                && loc1.getCityName().equalsIgnoreCase(loc2.getCityName())
                && (loc1.getCountry().equalsIgnoreCase(loc2.getCountry()) || loc1.getCountry().isEmpty() || loc2.getCountry().isEmpty());

        boolean sameCoords = (loc1.getLatitude() != 0 || loc1.getLongitude() != 0)
                && Math.abs(loc1.getLatitude() - loc2.getLatitude()) < 0.005
                && Math.abs(loc1.getLongitude() - loc2.getLongitude()) < 0.005;

        return sameName || sameCoords;
    }

    private static void loadRecentLocations(Context context) {
        sRecentLocationsMemory = new ArrayList<>();
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        String json = prefs.getString(KEY_SALAH_RECENT_LOCATIONS, null);
        if (json != null && !json.isEmpty()) {
            try {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    String str = array.getString(i);
                    RecentLocation loc = RecentLocation.fromJsonString(str);
                    if (loc != null) {
                        sRecentLocationsMemory.add(loc);
                    }
                }
            } catch (JSONException e) {
                LogUtils.e("Failed to parse recent locations json", e);
            }
        }
    }

    private static void saveRecentLocations(Context context) {
        if (sRecentLocationsMemory == null) return;
        JSONArray array = new JSONArray();
        for (RecentLocation loc : sRecentLocationsMemory) {
            array.put(loc.toJsonString());
        }
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        prefs.edit().putString(KEY_SALAH_RECENT_LOCATIONS, array.toString()).apply();
    }
}
