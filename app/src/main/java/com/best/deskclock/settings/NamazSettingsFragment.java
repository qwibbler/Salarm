package com.best.deskclock.settings;

import static com.best.deskclock.settings.PreferencesKeys.*;
import static com.best.deskclock.settings.PreferencesDefaultValues.*;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;

import com.best.deskclock.R;
import com.best.deskclock.namaz.CitySearchDialogFragment;
import com.best.deskclock.namaz.NamazLocationManager;
import com.best.deskclock.namaz.RelationalAlarmScheduler;

/**
 * Settings fragment for Namaz-related configurations.
 */
public class NamazSettingsFragment extends ScreenFragment implements Preference.OnPreferenceChangeListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private final ActivityResultLauncher<String[]> locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                Boolean coarseGranted = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                boolean fineLocationGranted = fineGranted != null && fineGranted;
                boolean coarseLocationGranted = coarseGranted != null && coarseGranted;
                if (fineLocationGranted || coarseLocationGranted) {
                    NamazLocationManager.requestGpsUpdate(requireContext());
                }
            });

    @Override
    protected String getFragmentTitle() {
        return "Namaz Settings";
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.namaz_settings);

        Preference locationMode = findPreference(KEY_NAMAZ_LOCATION_MODE);
        if (locationMode != null) locationMode.setOnPreferenceChangeListener(this);

        Preference madhab = findPreference(KEY_NAMAZ_MADHAB);
        if (madhab != null) madhab.setOnPreferenceChangeListener(this);

        Preference calcMethod = findPreference(KEY_NAMAZ_CALC_METHOD);
        if (calcMethod != null) calcMethod.setOnPreferenceChangeListener(this);

        Preference cityPref = findPreference(KEY_NAMAZ_CITY_NAME);
        if (cityPref != null) {
            cityPref.setOnPreferenceClickListener(preference -> {
                if (NamazLocationManager.getLocationMode(requireContext()) == NamazLocationManager.LocationMode.MANUAL) {
                    new CitySearchDialogFragment().show(getParentFragmentManager(), "city_search");
                }
                return true;
            });
        }

        updateUiStates();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPrefs != null) {
            mPrefs.registerOnSharedPreferenceChangeListener(this);
        }
        updateSummaries();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPrefs != null) {
            mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    private void updateUiStates() {
        NamazLocationManager.LocationMode mode = NamazLocationManager.getLocationMode(requireContext());
        Preference cityPref = findPreference(KEY_NAMAZ_CITY_NAME);
        if (cityPref != null) {
            cityPref.setEnabled(mode == NamazLocationManager.LocationMode.MANUAL);
        }
    }

    private void updateSummaries() {
        Preference cityPref = findPreference(KEY_NAMAZ_CITY_NAME);
        if (cityPref != null) {
            cityPref.setSummary(mPrefs.getString(KEY_NAMAZ_CITY_NAME, DEFAULT_NAMAZ_CITY_NAME));
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        String key = preference.getKey();

        if (KEY_NAMAZ_LOCATION_MODE.equals(key)) {
            NamazLocationManager.LocationMode mode = NamazLocationManager.LocationMode.valueOf((String) newValue);
            if (mode == NamazLocationManager.LocationMode.GPS) {
                checkLocationPermissions();
            }
            NamazLocationManager.setLocationMode(requireContext(), mode);
            updateUiStates();
        } else if (KEY_NAMAZ_MADHAB.equals(key) || KEY_NAMAZ_CALC_METHOD.equals(key)) {
            if (preference.getSharedPreferences() != null) {
                preference.getSharedPreferences().edit().putString(key, (String) newValue).apply();
            }
            RelationalAlarmScheduler.syncRelationalAlarms(requireContext());
            return false;
        }

        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (KEY_NAMAZ_CITY_NAME.equals(key)) {
            updateSummaries();
        }
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        } else {
            NamazLocationManager.requestGpsUpdate(requireContext());
        }
    }
}
