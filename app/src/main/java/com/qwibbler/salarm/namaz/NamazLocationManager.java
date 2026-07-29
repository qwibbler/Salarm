package com.qwibbler.salarm.namaz;

import static com.qwibbler.salarm.SalarmApplication.getDefaultSharedPreferences;
import static com.qwibbler.salarm.settings.PreferencesKeys.*;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.*;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.utils.LogUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * Manages location settings and GPS updates for Namaz prayer times.
 */
public class NamazLocationManager {

    public enum LocationMode {
        GPS, MANUAL
    }

    private static final int LOCATION_UPDATE_THRESHOLD_METERS = 10000; // 10km

    /**
     * Sets the location mode (GPS or MANUAL) and triggers an update.
     */
    public static void setLocationMode(Context context, LocationMode mode) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        LocationMode oldMode = getLocationMode(context);
        prefs.edit().putString(KEY_NAMAZ_LOCATION_MODE, mode.name()).apply();
        LogUtils.i("Location mode set to: " + mode);

        if (mode == LocationMode.GPS) {
            if (oldMode == LocationMode.MANUAL) {
                // Clear manual state to force fresh GPS resolution
                prefs.edit()
                        .remove(KEY_NAMAZ_LATITUDE)
                        .remove(KEY_NAMAZ_LONGITUDE)
                        .remove(KEY_NAMAZ_CITY_NAME)
                        .remove(KEY_NAMAZ_DISPLAY_TIMEZONE)
                        .apply();
            }
            prefs.edit().putString(KEY_NAMAZ_CITY_NAME, context.getString(R.string.namaz_detecting_location)).apply();
            requestGpsUpdate(context);
        }

        // Always trigger a resync when switching modes to ensure correctness
        RelationalAlarmSyncManager.recalculateAllActiveRules(context);
    }

    /**
     * Returns the current location mode.
     */
    public static LocationMode getLocationMode(Context context) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        String modeStr = prefs.getString(KEY_NAMAZ_LOCATION_MODE, DEFAULT_NAMAZ_LOCATION_MODE);
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
        double[] coords = NamazUtils.getCoordinates(context);
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

        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(context);
        client.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                handleNewLocation(context, location.getLatitude(), location.getLongitude(), true, false);
            } else {
                LogUtils.i("Last location null, requesting fresh update...");
                requestFreshLocation(context, client);
            }
        });
    }

    private static void requestFreshLocation(Context context, FusedLocationProviderClient client) {
        final Context appContext = context.getApplicationContext();
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000)
                .setMaxUpdates(1)
                .build();

        if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            final LocationCallback callback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        handleNewLocation(appContext, location.getLatitude(), location.getLongitude(), true, false);
                    }
                    client.removeLocationUpdates(this);
                }
            };

            client.requestLocationUpdates(request, callback, Looper.getMainLooper());

            // Add a timeout to stop searching and clear status if no location found
            AppExecutors.getMainThread().postDelayed(() -> {
                client.removeLocationUpdates(callback);
                // If we're still stuck in "Detecting...", revert to a neutral state
                SharedPreferences prefs = getDefaultSharedPreferences(appContext);
                if (appContext.getString(R.string.namaz_detecting_location).equals(prefs.getString(KEY_NAMAZ_CITY_NAME, ""))) {
                    prefs.edit().putString(KEY_NAMAZ_CITY_NAME, "Not detected").apply();
                }
            }, 30000); // 30 second timeout
        }
    }

    /**
     * Handles a new set of coordinates, updates prefs, and triggers sync if significant.
     * @param isGps If true, will attempt to reverse-geocode the city name.
     * @param forceSync If true, triggers a resync regardless of the distance changed.
     */
    public static void handleNewLocation(Context context, double lat, double lon, boolean isGps, boolean forceSync) {
        LogUtils.i("Processing new location: " + lat + ", " + lon + " (GPS: " + isGps + ", ForceSync: " + forceSync + ")");

        SharedPreferences prefs = getDefaultSharedPreferences(context);
        double[] oldCoords = NamazUtils.getCoordinates(context);
        double oldLat = oldCoords[0];
        double oldLon = oldCoords[1];

        double distance = NamazUtils.calculateDistance(lat, lon, oldLat, oldLon);
        boolean significantChange = forceSync || distance > LOCATION_UPDATE_THRESHOLD_METERS;

        // Also resolve if city name is missing or "Detecting..."
        String currentCity = prefs.getString(KEY_NAMAZ_CITY_NAME, "");
        boolean needsResolution = significantChange || currentCity.isEmpty() ||
                currentCity.equals(context.getString(R.string.namaz_detecting_location));

        prefs.edit()
                .putString(KEY_NAMAZ_LATITUDE, String.valueOf(lat))
                .putString(KEY_NAMAZ_LONGITUDE, String.valueOf(lon))
                .apply();

        if (isGps && needsResolution) {
            NamazUtils.updateCityNameFromCoords(context, lat, lon);

            // For GPS updates, we resolve the TimeZone.
            // Try online if allowed, otherwise fallback to phone's TZ.
            AppExecutors.getNetworkIO().execute(() -> {
                String tzId = NamazTimezoneUtils.fetchTimeZoneId(context, lat, lon);
                if (tzId == null) {
                    tzId = NamazTimezoneUtils.getPhoneTimeZoneId();
                }
                getDefaultSharedPreferences(context).edit()
                        .putString(KEY_NAMAZ_DISPLAY_TIMEZONE, tzId)
                        .apply();
            });
        }

        if (significantChange) {
            LogUtils.i("Significant location change detected (" + (int) distance + "m). Triggering alarm resync...");
            RelationalAlarmSyncManager.recalculateAllActiveRules(context);
        }
    }
}
