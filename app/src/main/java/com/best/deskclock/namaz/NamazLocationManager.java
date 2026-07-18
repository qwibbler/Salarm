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
            requestGpsUpdate(context);
        }
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
                handleNewLocation(context, location.getLatitude(), location.getLongitude(), true);
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

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            client.requestLocationUpdates(request, new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        handleNewLocation(context, location.getLatitude(), location.getLongitude(), true);
                    }
                    client.removeLocationUpdates(this);
                }
            }, Looper.getMainLooper());
        }
    }

    /**
     * Handles a new set of coordinates, updates prefs, and triggers sync if significant.
     * @param isGps If true, will attempt to reverse-geocode the city name.
     */
    public static void handleNewLocation(Context context, double lat, double lon, boolean isGps) {
        LogUtils.i("Processing new location: " + lat + ", " + lon + " (GPS: " + isGps + ")");

        SharedPreferences prefs = getDefaultSharedPreferences(context);
        double oldLat = Double.parseDouble(prefs.getString(KEY_NAMAZ_LATITUDE, DEFAULT_NAMAZ_LATITUDE));
        double oldLon = Double.parseDouble(prefs.getString(KEY_NAMAZ_LONGITUDE, DEFAULT_NAMAZ_LONGITUDE));

        boolean significantChange = Math.abs(lat - oldLat) > SIGNIFICANT_CHANGE_THRESHOLD
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
        AppExecutors.getDiskIO().execute(() -> {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String cityName = address.getLocality();
                    if (cityName == null) cityName = address.getSubAdminArea();
                    if (cityName == null) cityName = address.getAdminArea();

                    if (cityName != null) {
                        String country = address.getCountryName();
                        final String finalName = cityName + (country != null ? ", " + country : "");
                        getDefaultSharedPreferences(context).edit()
                                .putString(KEY_NAMAZ_CITY_NAME, finalName)
                                .apply();
                    }
                }
            } catch (IOException e) {
                LogUtils.e("Error reverse-geocoding coordinates", e);
            }
        });
    }
}
