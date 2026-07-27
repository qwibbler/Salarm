package com.qwibbler.salarm.namaz;

import static com.qwibbler.salarm.SalarmApplication.getDefaultSharedPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;

import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.Madhab;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.settings.PreferencesDefaultValues;
import com.qwibbler.salarm.settings.PreferencesKeys;
import com.qwibbler.salarm.utils.LogUtils;
import com.qwibbler.salarm.utils.SdkUtils;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Shared utility methods for Namaz features.
 */
public class NamazUtils {

    /**
     * Checks if the device has an active internet connection.
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (SdkUtils.isAtLeastAndroid10()) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
    }

    /**
     * Retrieves the current Namaz coordinates from preferences.
     */
    public static double[] getCoordinates(Context context) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        String latStr = prefs.getString(PreferencesKeys.KEY_NAMAZ_LATITUDE, PreferencesDefaultValues.DEFAULT_NAMAZ_LATITUDE);
        String lonStr = prefs.getString(PreferencesKeys.KEY_NAMAZ_LONGITUDE, PreferencesDefaultValues.DEFAULT_NAMAZ_LONGITUDE);
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
     * Retrieves the current Namaz calculation method from preferences.
     */
    public static CalculationMethod getCalculationMethod(Context context) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        String methodStr = prefs.getString(PreferencesKeys.KEY_NAMAZ_CALC_METHOD, PreferencesDefaultValues.DEFAULT_NAMAZ_CALC_METHOD);
        try {
            return CalculationMethod.valueOf(methodStr);
        } catch (IllegalArgumentException e) {
            LogUtils.e("Invalid calculation method in preferences: " + methodStr, e);
            return CalculationMethod.valueOf(PreferencesDefaultValues.DEFAULT_NAMAZ_CALC_METHOD);
        }
    }

    /**
     * Retrieves the current Madhab from preferences.
     */
    public static Madhab getMadhab(Context context) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        String madhabStr = prefs.getString(PreferencesKeys.KEY_NAMAZ_MADHAB, PreferencesDefaultValues.DEFAULT_NAMAZ_MADHAB);
        try {
            return Madhab.valueOf(madhabStr);
        } catch (IllegalArgumentException e) {
            LogUtils.e("Invalid madhab in preferences: " + madhabStr, e);
            return Madhab.valueOf(PreferencesDefaultValues.DEFAULT_NAMAZ_MADHAB);
        }
    }

    /**
     * Unified reverse geocoding to get a city name from coordinates.
     */
    public static void updateCityNameFromCoords(Context context, double lat, double lon) {
        final Context appContext = context.getApplicationContext();
        AppExecutors.getNetworkIO().execute(() -> {
            Geocoder geocoder = new Geocoder(appContext, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String city = address.getLocality();
                    if (city == null) {
                        city = address.getSubAdminArea();
                    }
                    if (city == null) {
                        city = address.getFeatureName();
                    }

                    String state = address.getAdminArea();
                    String country = address.getCountryName();

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
                                .putString(PreferencesKeys.KEY_NAMAZ_CITY_NAME, sb.toString())
                                .apply();
                    }
                }
            } catch (IOException e) {
                LogUtils.e("Error during reverse geocoding", e);
            }
        });
    }
}
