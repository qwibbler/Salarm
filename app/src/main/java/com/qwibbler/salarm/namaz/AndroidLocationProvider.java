package com.qwibbler.salarm.namaz;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.qwibbler.salarm.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A framework-only location provider for F-Droid compatibility.
 * This app depends only on Android's LocationManager and can work with whatever
 * location providers the device or ROM makes available (e.g., GPS, Network, or
 * a system-level fused provider).
 *
 * It uses the Android framework location providers and, where available, the framework
 * FUSED_PROVIDER (API 31+). The actual location implementation is provided by the device/OS.
 */
public class AndroidLocationProvider {

    public interface LocationResultListener {
        void onLocationResult(@Nullable Location location);
    }

    /**
     * Finds the best available last-known location across all providers.
     * Prefers freshness and accuracy.
     */
    @SuppressLint("MissingPermission")
    @Nullable
    public static Location getBestLastKnownLocation(Context context) {
        if (!hasLocationPermission(context)) return null;

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) return null;

        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;

        for (String provider : providers) {
            try {
                Location l = locationManager.getLastKnownLocation(provider);
                if (l == null) continue;

                if (bestLocation == null || isBetterLocation(l, bestLocation)) {
                    bestLocation = l;
                }
            } catch (SecurityException ignored) {}
        }

        if (bestLocation != null) {
            long age = System.currentTimeMillis() - bestLocation.getTime();
            LogUtils.i("Using last known location from " + bestLocation.getProvider() + ", age: " + (age / 1000) + "s");
            return bestLocation;
        }

        return null;
    }

    /**
     * Requests a single location update with a strict timeout.
     * Stops updates immediately after the first result or timeout.
     */
    @SuppressLint("MissingPermission")
    public static void requestSingleUpdate(Context context, long timeoutMs, LocationResultListener listener) {
        final Context appContext = context.getApplicationContext();
        if (!hasLocationPermission(appContext)) {
            listener.onLocationResult(null);
            return;
        }

        final LocationManager locationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            listener.onLocationResult(null);
            return;
        }

        final AtomicBoolean finished = new AtomicBoolean(false);
        final Handler handler = new Handler(Looper.getMainLooper());

        final LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (finished.compareAndSet(false, true)) {
                    LogUtils.i("Received fresh location from " + location.getProvider());
                    locationManager.removeUpdates(this);
                    handler.removeCallbacksAndMessages(null);
                    listener.onLocationResult(location);
                }
            }

            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(@NonNull String provider) {}
            @Override public void onProviderDisabled(@NonNull String provider) {}
        };

        final Runnable timeoutRunnable = () -> {
            if (finished.compareAndSet(false, true)) {
                LogUtils.w("Location request timed out after " + timeoutMs + "ms");
                locationManager.removeUpdates(locationListener);
                listener.onLocationResult(null);
            }
        };

        // Timeout fallback
        handler.postDelayed(timeoutRunnable, timeoutMs);

        List<String> providersToRequest = getProvidersToRequest(locationManager);
        if (!providersToRequest.isEmpty()) {
            boolean requestedAny = false;
            for (String provider : providersToRequest) {
                try {
                    LogUtils.i("Requesting location update from: " + provider);
                    locationManager.requestLocationUpdates(provider, 0, 0, locationListener, Looper.getMainLooper());
                    requestedAny = true;
                } catch (SecurityException e) {
                    LogUtils.e("Permission denied for location provider: " + provider, e);
                } catch (IllegalArgumentException e) {
                    LogUtils.w("Provider not available: " + provider);
                }
            }

            if (!requestedAny) {
                if (finished.compareAndSet(false, true)) {
                    handler.removeCallbacks(timeoutRunnable);
                    locationManager.removeUpdates(locationListener);
                    listener.onLocationResult(null);
                }
            }
        } else {
            LogUtils.w("No suitable location providers enabled");
            if (finished.compareAndSet(false, true)) {
                handler.removeCallbacks(timeoutRunnable);
                listener.onLocationResult(null);
            }
        }
    }

    private static List<String> getProvidersToRequest(LocationManager locationManager) {
        List<String> result = new ArrayList<>();

        // API 31+ provides a framework-level fused provider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                if (locationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER)) {
                    result.add(LocationManager.FUSED_PROVIDER);
                    return result; // Prefer FUSED if available on API 31+
                }
            } catch (Exception ignored) {}
        }

        // Fallback to all enabled providers (GPS, Network, etc.)
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            result.add(LocationManager.GPS_PROVIDER);
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            result.add(LocationManager.NETWORK_PROVIDER);
        }

        // If GPS/Network aren't available but others are, just grab all enabled ones
        if (result.isEmpty()) {
            List<String> allEnabled = locationManager.getProviders(true);
            result.addAll(allEnabled);
        }

        return result;
    }

    private static boolean hasLocationPermission(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Determines whether one Location reading is better than the current Location fix.
     */
    private static boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) return true;

        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > 1000 * 60 * 2;
        boolean isSignificantlyOlder = timeDelta < -1000 * 60 * 2;
        boolean isNewer = timeDelta > 0;

        if (isSignificantlyNewer) return true;
        if (isSignificantlyOlder) return false;

        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        if (isMoreAccurate) return true;
        if (isNewer && !isLessAccurate) return true;
        return isNewer && !isSignificantlyLessAccurate &&
                isSameProvider(location.getProvider(), currentBestLocation.getProvider());
    }

    private static boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) return provider2 == null;
        return provider1.equals(provider2);
    }
}
