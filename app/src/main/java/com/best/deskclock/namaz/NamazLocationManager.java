package com.best.deskclock.namaz;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesKeys.*;
import static com.best.deskclock.settings.PreferencesDefaultValues.*;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.best.deskclock.R;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.utils.LogUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Manages location settings and GPS updates for Namaz prayer times.
 */
public class NamazLocationManager {

    public enum LocationMode {
        GPS, MANUAL
    }

    private static final double SIGNIFICANT_CHANGE_THRESHOLD = 0.05;

    /**
     * Sets the location mode (GPS or MANUAL) and triggers an update.
     */
    public static void setLocationMode(Context context, LocationMode mode) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        prefs.edit().putString(KEY_NAMAZ_LOCATION_MODE, mode.name()).apply();
        LogUtils.i("Location mode set to: " + mode);

        if (mode == LocationMode.GPS) {
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
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        String latStr = prefs.getString(KEY_NAMAZ_LATITUDE, DEFAULT_NAMAZ_LATITUDE);
        String lonStr = prefs.getString(KEY_NAMAZ_LONGITUDE, DEFAULT_NAMAZ_LONGITUDE);

        try {
            double lat = Double.parseDouble(latStr);
            double lon = Double.parseDouble(lonStr);
            // Relaxed check: we only need valid coordinates to calculate times.
            return (lat != 0.0 || lon != 0.0);
        } catch (NumberFormatException e) {
            return false;
        }
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
        double oldLat, oldLon;
        try {
            oldLat = Double.parseDouble(prefs.getString(KEY_NAMAZ_LATITUDE, DEFAULT_NAMAZ_LATITUDE));
            oldLon = Double.parseDouble(prefs.getString(KEY_NAMAZ_LONGITUDE, DEFAULT_NAMAZ_LONGITUDE));
        } catch (NumberFormatException e) {
            oldLat = 0.0;
            oldLon = 0.0;
        }

        boolean significantChange = forceSync || Math.abs(lat - oldLat) > SIGNIFICANT_CHANGE_THRESHOLD
                || Math.abs(lon - oldLon) > SIGNIFICANT_CHANGE_THRESHOLD;

        prefs.edit()
                .putString(KEY_NAMAZ_LATITUDE, String.valueOf(lat))
                .putString(KEY_NAMAZ_LONGITUDE, String.valueOf(lon))
                .apply();

        if (isGps) {
            updateCityNameFromCoords(context, lat, lon);
        }

        if (significantChange) {
            LogUtils.i("Significant location change detected. Triggering alarm resync...");
            RelationalAlarmSyncManager.recalculateAllActiveRules(context);
        }
    }

    private static void updateCityNameFromCoords(Context context, double lat, double lon) {
        final Context appContext = context.getApplicationContext();
        AppExecutors.getNetworkIO().execute(() -> {
            Geocoder geocoder = new Geocoder(appContext, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String city = address.getLocality();
                    String state = address.getAdminArea();
                    String country = address.getCountryName();

                    if (city == null) city = address.getSubAdminArea();
                    if (city == null) city = address.getFeatureName();

                    StringBuilder sb = new StringBuilder();
                    if (city != null) sb.append(city);
                    if (state != null && !state.equalsIgnoreCase(city)) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(state);
                    }
                    if (country != null) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(country);
                    }

                    if (sb.length() > 0) {
                        getDefaultSharedPreferences(appContext).edit()
                                .putString(KEY_NAMAZ_CITY_NAME, sb.toString())
                                .apply();
                    }
                }
            } catch (IOException e) {
                NamazErrorHandler.handleSyncError(appContext, e);
            }
        });
    }
}
