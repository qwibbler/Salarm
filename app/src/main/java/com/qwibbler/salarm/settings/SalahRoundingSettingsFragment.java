package com.qwibbler.salarm.settings;

import static com.qwibbler.salarm.settings.PreferencesDefaultValues.*;
import static com.qwibbler.salarm.settings.PreferencesKeys.*;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.base.BaseSettingsScreenFragment;
import com.qwibbler.salarm.salah.SalahAlarmSyncManager;

/**
 * Settings fragment for Salah time rounding configurations.
 */
public class SalahRoundingSettingsFragment extends BaseSettingsScreenFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.salah_time_rounding_title);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.salah_rounding_settings);
        updateAllSummaries();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPrefs != null) {
            mPrefs.registerOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPrefs != null) {
            mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key != null && key.startsWith("key_salah_rounding_")) {
            updatePreferenceSummary(findPreference(key));
            SalahAlarmSyncManager.recalculateAllActiveRules(requireContext());
        }
    }

    private void updateAllSummaries() {
        updatePreferenceSummary(findPreference(KEY_SALAH_ROUNDING_FAJR));
        updatePreferenceSummary(findPreference(KEY_SALAH_ROUNDING_SUNRISE));
        updatePreferenceSummary(findPreference(KEY_SALAH_ROUNDING_DHUHR));
        updatePreferenceSummary(findPreference(KEY_SALAH_ROUNDING_ASR));
        updatePreferenceSummary(findPreference(KEY_SALAH_ROUNDING_MAGHRIB));
        updatePreferenceSummary(findPreference(KEY_SALAH_ROUNDING_ISHA));
    }

    private void updatePreferenceSummary(Preference pref) {
        if (!(pref instanceof ListPreference listPref)) return;
        String value = listPref.getValue();
        if (value == null) return;

        int resId = switch (value) {
            case ROUNDING_TO -> R.string.rounding_nearest_to;
            case ROUNDING_UP -> R.string.rounding_up_to;
            case ROUNDING_DOWN -> R.string.rounding_down_to;
            default -> 0;
        };
        if (resId != 0) {
            listPref.setSummary(resId);
        }
    }
}
