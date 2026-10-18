package com.qwibbler.salarm.settings;

import static com.qwibbler.salarm.settings.PreferencesKeys.*;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.*;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.base.BaseSettingsScreenFragment;
import com.qwibbler.salarm.salah.CitySearchDialogFragment;
import com.qwibbler.salarm.salah.SalahLocationManager;
import com.qwibbler.salarm.salah.SalahAlarmSyncManager;
import com.qwibbler.salarm.utils.WidgetUtils;
import com.qwibbler.salarm.widgets.SalahTimelineWidgetProvider;

import java.util.HashSet;
import java.util.Set;

/**
 * Settings fragment for Salah-related configurations.
 */
public class SalahSettingsFragment extends BaseSettingsScreenFragment implements Preference.OnPreferenceChangeListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private final ActivityResultLauncher<String[]> locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                Boolean coarseGranted = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                boolean fineLocationGranted = fineGranted != null && fineGranted;
                boolean coarseLocationGranted = coarseGranted != null && coarseGranted;
                if (fineLocationGranted || coarseLocationGranted) {
                    SalahLocationManager.requestGpsUpdate(requireContext());
                }
            });

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.salah_settings_title);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.salah_settings);

        Preference locationMode = findPreference(KEY_SALAH_LOCATION_MODE);
        if (locationMode != null) locationMode.setOnPreferenceChangeListener(this);

        Preference madhab = findPreference(KEY_SALAH_MADHAB);
        if (madhab != null) madhab.setOnPreferenceChangeListener(this);

        Preference calcMethod = findPreference(KEY_SALAH_CALC_METHOD);
        if (calcMethod != null) calcMethod.setOnPreferenceChangeListener(this);

        Preference showBothAsr = findPreference(KEY_SALAH_SHOW_BOTH_ASR);
        if (showBothAsr != null) showBothAsr.setOnPreferenceChangeListener(this);

        Preference cityPref = findPreference(KEY_SALAH_CITY_NAME);
        if (cityPref != null) {
            cityPref.setOnPreferenceClickListener(preference -> {
                if (SalahLocationManager.getLocationMode(requireContext()) == SalahLocationManager.LocationMode.MANUAL) {
                    new CitySearchDialogFragment().show(getParentFragmentManager(), "city_search");
                }
                return true;
            });
        }

        Preference roundingPref = findPreference(KEY_SALAH_TIME_ROUNDING_SETTINGS);
        if (roundingPref != null) {
            roundingPref.setOnPreferenceClickListener(preference -> {
                animateAndShowFragment(new SalahRoundingSettingsFragment());
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
        SalahLocationManager.LocationMode mode = SalahLocationManager.getLocationMode(requireContext());
        Preference cityPref = findPreference(KEY_SALAH_CITY_NAME);
        if (cityPref != null) {
            cityPref.setEnabled(mode == SalahLocationManager.LocationMode.MANUAL);
        }

        Preference showBothAsr = findPreference(KEY_SALAH_SHOW_BOTH_ASR);
        Preference sub1 = findPreference(KEY_SALAH_SHOW_BOTH_ASR_IN_ALARM_EVENTS);
        Preference sub2 = findPreference(KEY_SALAH_SHOW_BOTH_ASR_IN_SALAH_TIMES);
        Preference sub3 = findPreference(KEY_SALAH_SHOW_BOTH_ASR_IN_WIDGETS);

        boolean bothAsrEnabled = showBothAsr instanceof SwitchPreferenceCompat sw && sw.isChecked();

        if (sub1 != null) sub1.setVisible(bothAsrEnabled);
        if (sub2 != null) sub2.setVisible(bothAsrEnabled);
        if (sub3 != null) sub3.setVisible(bothAsrEnabled);
    }

    private void updateSummaries() {
        Preference cityPref = findPreference(KEY_SALAH_CITY_NAME);
        if (cityPref != null) {
            cityPref.setSummary(mPrefs.getString(KEY_SALAH_CITY_NAME, DEFAULT_SALAH_CITY_NAME));
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        String key = preference.getKey();

        if (KEY_SALAH_LOCATION_MODE.equals(key)) {
            SalahLocationManager.LocationMode mode = SalahLocationManager.LocationMode.valueOf((String) newValue);
            if (mode == SalahLocationManager.LocationMode.GPS) {
                checkLocationPermissions();
            }
            SalahLocationManager.setLocationMode(requireContext(), mode);
            updateUiStates();
        }

        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (KEY_SALAH_CITY_NAME.equals(key)) {
            updateSummaries();
        } else if (KEY_SALAH_MADHAB.equals(key) || KEY_SALAH_CALC_METHOD.equals(key)) {
            SalahAlarmSyncManager.recalculateAllActiveRules(requireContext());
        } else if (KEY_SALAH_SHOW_BOTH_ASR.equals(key)) {
            updateUiStates();
            WidgetUtils.scheduleWidgetUpdate(requireContext(), SalahTimelineWidgetProvider.class);
        } else if (KEY_SALAH_SHOW_BOTH_ASR_IN_WIDGETS.equals(key)) {
            if (sharedPreferences.getBoolean(KEY_SALAH_SHOW_BOTH_ASR_IN_WIDGETS, false)) {
                Set<String> visibleEvents = sharedPreferences.getStringSet(KEY_SALAH_WIDGET_VISIBLE_EVENTS, DEFAULT_SALAH_WIDGET_VISIBLE_EVENTS);
                if (!visibleEvents.contains("asr_shafi") && !visibleEvents.contains("asr_hanafi")) {
                    Set<String> newVisibleEvents = new HashSet<>(visibleEvents);
                    newVisibleEvents.add("asr_shafi");
                    newVisibleEvents.add("asr_hanafi");
                    sharedPreferences.edit().putStringSet(KEY_SALAH_WIDGET_VISIBLE_EVENTS, newVisibleEvents).apply();
                }
            }
            WidgetUtils.scheduleWidgetUpdate(requireContext(), SalahTimelineWidgetProvider.class);
        }
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        } else {
            SalahLocationManager.requestGpsUpdate(requireContext());
        }
    }
}
