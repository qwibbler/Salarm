package com.qwibbler.salarm.salah;

import static com.qwibbler.salarm.SalarmApplication.getDefaultSharedPreferences;
import static com.qwibbler.salarm.settings.PreferencesKeys.*;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.*;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.core.app.ActivityCompat;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.utils.LogUtils;

/**
 * Manages location settings and GPS updates for Salah prayer times.
 */
public class SalahLocationManager {

    public enum LocationMode {
        GPS, MANUAL
    }

    private static final int LOCATION_UPDATE_THRESHOLD_METERS = 50000; // 50km

    /**
     * Sets the location mode (GPS or MANUAL) and triggers an update.
     */
    public static void setLocationMode(Context context, LocationMode mode) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        prefs.edit().putString(KEY_SALAH_LOCATION_MODE, mode.name()).apply();
        LogUtils.i("Location mode set to: " + mode);

        if (mode == LocationMode.GPS) {
            // Clear manual state and any stored timezone to follow device timezone in GPS mode
            prefs.edit()
                    .remove(KEY_SALAH_LATITUDE)
                    .remove(KEY_SALAH_LONGITUDE)
                    .remove(KEY_SALAH_CITY_NAME)
                    .remove(KEY_SALAH_DISPLAY_TIMEZONE)
                    .apply();
            prefs.edit().putString(KEY_SALAH_CITY_NAME, context.getString(R.string.salarm_detecting_location)).apply();
            requestGpsUpdate(context);
        }

        // Always trigger a resync when switching modes to ensure correctness
        SalahAlarmSyncManager.recalculateAllActiveRules(context);
    }

    /**
     * Returns the current location mode.
     */
    public static LocationMode getLocationMode(Context context) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        String modeStr = prefs.getString(KEY_SALAH_LOCATION_MODE, DEFAULT_SALAH_LOCATION_MODE);
        try {
            return LocationMode.valueOf(modeStr);
        } catch (IllegalArgumentException e) {
            return LocationMode.GPS;
        }
    }

    /**
     * Checks if the location has been set (either via GPS or manual selection).
     */
    public static boolean isLocationSet(Context context) {
        double[] coords = SalahUtils.getCoordinates(context);
        // Relaxed check: we only need valid coordinates to calculate times.
        return (coords[0] != 0.0 || coords[1] != 0.0);
    }

    /**
     * Requests a one-time GPS update if permissions are granted.
     */
    public static void requestGpsUpdate(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            LogUtils.w("Location permissions not granted for GPS update");
            return;
        }

        Location lastKnown = AndroidLocationProvider.getBestLastKnownLocation(context);
        if (lastKnown != null) {
            handleNewLocation(context, lastKnown.getLatitude(), lastKnown.getLongitude(), true, false);
        } else {
            LogUtils.i("No suitable last location, requesting fresh update...");
            requestFreshLocation(context);
        }
    }

    private static void requestFreshLocation(Context context) {
        final Context appContext = context.getApplicationContext();
        AndroidLocationProvider.requestSingleUpdate(appContext, 30000, location -> {
            if (location != null) {
                handleNewLocation(appContext, location.getLatitude(), location.getLongitude(), true, false);
            } else {
                // If we're still stuck in "Detecting...", revert to a neutral state
                SharedPreferences prefs = getDefaultSharedPreferences(appContext);
                if (appContext.getString(R.string.salarm_detecting_location).equals(prefs.getString(KEY_SALAH_CITY_NAME, ""))) {
                    prefs.edit().putString(KEY_SALAH_CITY_NAME, "Not detected").apply();
                }
            }
        });
    }

    /**
     * Handles a new set of coordinates, updates prefs, and triggers sync if significant.
     * @param isGps If true, will attempt to reverse-geocode the city name.
     * @param forceSync If true, triggers a resync regardless of the distance changed.
     */
    public static void handleNewLocation(Context context, double lat, double lon, boolean isGps, boolean forceSync) {
        LogUtils.i("Processing new location: " + lat + ", " + lon + " (GPS: " + isGps + ", ForceSync: " + forceSync + ")");

        SharedPreferences prefs = getDefaultSharedPreferences(context);
        double[] oldCoords = SalahUtils.getCoordinates(context);
        double oldLat = oldCoords[0];
        double oldLon = oldCoords[1];

        double distance = SalahUtils.calculateDistance(lat, lon, oldLat, oldLon);
        boolean significantDistance = forceSync || distance > LOCATION_UPDATE_THRESHOLD_METERS;

        // Also resolve if city name is missing or "Detecting..."
        String currentCity = prefs.getString(KEY_SALAH_CITY_NAME, "");
        boolean needsResolution = significantDistance || currentCity.isEmpty() ||
                currentCity.equals(context.getString(R.string.salarm_detecting_location));

        prefs.edit()
                .putString(KEY_SALAH_LATITUDE, String.valueOf(lat))
                .putString(KEY_SALAH_LONGITUDE, String.valueOf(lon))
                .apply();

        if (isGps && needsResolution) {
            SalahUtils.updateCityNameFromCoords(context, lat, lon);
        }

        if (significantDistance) {
            LogUtils.i("Significant location change detected (" + (int) distance + "m). Triggering alarm resync...");
            SalahAlarmSyncManager.recalculateAllActiveRules(context);
        }
    }
}
