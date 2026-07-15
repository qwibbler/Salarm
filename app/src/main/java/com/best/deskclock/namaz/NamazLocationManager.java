package com.best.deskclock.namaz;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesKeys.*;
import static com.best.deskclock.settings.PreferencesDefaultValues.*;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.best.deskclock.utils.LogUtils;
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

    private static final double SIGNIFICANT_CHANGE_THRESHOLD = 0.05;

    /**
     * Sets the location mode (GPS or MANUAL) and triggers an update.
     */
    public static void setLocationMode(Context context, LocationMode mode) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        prefs.edit().putString(KEY_NAMAZ_LOCATION_MODE, mode.name()).apply();
        LogUtils.i("Location mode set to: " + mode);

        if (mode == LocationMode.GPS) {
            requestGpsUpdate(context);
        } else {
            updateActiveCoordinatesFromManual(context);
        }
    }

    /**
     * Returns the current location mode.
     */
    public static LocationMode getLocationMode(Context context) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        String modeStr = prefs.getString(KEY_NAMAZ_LOCATION_MODE, DEFAULT_NAMAZ_LOCATION_MODE);
        return LocationMode.valueOf(modeStr);
    }

    /**
     * Manually sets the coordinates and city name.
     */
    public static void setManualLocation(Context context, double lat, double lon, String cityName) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        prefs.edit()
                .putString(KEY_NAMAZ_MANUAL_LATITUDE, String.valueOf(lat))
                .putString(KEY_NAMAZ_MANUAL_LONGITUDE, String.valueOf(lon))
                .putString(KEY_NAMAZ_CITY_NAME, cityName)
                .apply();

        if (getLocationMode(context) == LocationMode.MANUAL) {
            updateActiveCoordinates(context, lat, lon);
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
                handleNewGpsLocation(context, location);
            } else {
                LogUtils.i("Last location null, requesting fresh update...");
                requestFreshLocation(context, client);
            }
        });
    }

    private static void requestFreshLocation(Context context, FusedLocationProviderClient client) {
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000)
                .setMaxUpdates(1)
                .build();

        // Re-check permissions just in case
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            client.requestLocationUpdates(request, new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        handleNewGpsLocation(context, location);
                    }
                    client.removeLocationUpdates(this);
                }
            }, Looper.getMainLooper());
        }
    }

    private static void handleNewGpsLocation(Context context, Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        LogUtils.i("New GPS location received: " + lat + ", " + lon);

        SharedPreferences prefs = getDefaultSharedPreferences(context);
        prefs.edit()
                .putString(KEY_NAMAZ_AUTO_LATITUDE, String.valueOf(lat))
                .putString(KEY_NAMAZ_AUTO_LONGITUDE, String.valueOf(lon))
                .apply();

        if (getLocationMode(context) == LocationMode.GPS) {
            updateActiveCoordinates(context, lat, lon);
        }
    }

    private static void updateActiveCoordinatesFromManual(Context context) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        double lat = Double.parseDouble(prefs.getString(KEY_NAMAZ_MANUAL_LATITUDE, DEFAULT_NAMAZ_LATITUDE));
        double lon = Double.parseDouble(prefs.getString(KEY_NAMAZ_MANUAL_LONGITUDE, DEFAULT_NAMAZ_LONGITUDE));
        updateActiveCoordinates(context, lat, lon);
    }

    private static void updateActiveCoordinates(Context context, double newLat, double newLon) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        double oldLat = Double.parseDouble(prefs.getString(KEY_NAMAZ_LATITUDE, DEFAULT_NAMAZ_LATITUDE));
        double oldLon = Double.parseDouble(prefs.getString(KEY_NAMAZ_LONGITUDE, DEFAULT_NAMAZ_LONGITUDE));

        boolean significantChange = Math.abs(newLat - oldLat) > SIGNIFICANT_CHANGE_THRESHOLD
                || Math.abs(newLon - oldLon) > SIGNIFICANT_CHANGE_THRESHOLD;

        prefs.edit()
                .putString(KEY_NAMAZ_LATITUDE, String.valueOf(newLat))
                .putString(KEY_NAMAZ_LONGITUDE, String.valueOf(newLon))
                .apply();

        if (significantChange) {
            LogUtils.i("Significant location change detected. Triggering alarm resync...");
            RelationalAlarmScheduler.syncRelationalAlarms(context);
        }
    }
}
