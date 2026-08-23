// SPDX-License-Identifier: GPL-3.0-only

package com.qwibbler.salarm.settings;

import static com.qwibbler.salarm.settings.PreferencesKeys.KEY_ANALOG_WIDGET_CUSTOMIZATION;
import static com.qwibbler.salarm.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_CUSTOMIZATION;
import static com.qwibbler.salarm.settings.PreferencesKeys.KEY_SALAH_WIDGET_CUSTOMIZATION;
import static com.qwibbler.salarm.settings.PreferencesKeys.KEY_NEXT_ALARM_WIDGET_CUSTOMIZATION;
import static com.qwibbler.salarm.settings.PreferencesKeys.KEY_VERTICAL_WIDGET_CUSTOMIZATION;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.base.BaseSettingsScreenFragment;

public class WidgetSettingsFragment extends BaseSettingsScreenFragment implements Preference.OnPreferenceClickListener {

    Preference mAnalogWidgetCustomizationPref;
    Preference mDigitalWidgetCustomizationPref;
    Preference mVerticalWidgetCustomizationPref;
    Preference mNextAlarmWidgetCustomizationPref;
    Preference mSalahWidgetCustomizationPref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.widgets_settings);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_widgets);

        mAnalogWidgetCustomizationPref = findPreference(KEY_ANALOG_WIDGET_CUSTOMIZATION);
        mDigitalWidgetCustomizationPref = findPreference(KEY_DIGITAL_WIDGET_CUSTOMIZATION);
        mVerticalWidgetCustomizationPref = findPreference(KEY_VERTICAL_WIDGET_CUSTOMIZATION);
        mNextAlarmWidgetCustomizationPref = findPreference(KEY_NEXT_ALARM_WIDGET_CUSTOMIZATION);
        mSalahWidgetCustomizationPref = findPreference(KEY_SALAH_WIDGET_CUSTOMIZATION);

        setupPreferences();
    }

    @Override
    public void onDestroy() {
        nullifyPreferenceListeners(mAnalogWidgetCustomizationPref, mDigitalWidgetCustomizationPref, mVerticalWidgetCustomizationPref,
            mNextAlarmWidgetCustomizationPref, mSalahWidgetCustomizationPref);

        nullifyAllPrefs();

        super.onDestroy();
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference pref) {
        switch (pref.getKey()) {
            case KEY_ANALOG_WIDGET_CUSTOMIZATION -> animateAndShowFragment(new AppWidgetAnalogSettingsFragment());

            case KEY_DIGITAL_WIDGET_CUSTOMIZATION -> animateAndShowFragment(new AppWidgetDigitalSettingsFragment());

            case KEY_VERTICAL_WIDGET_CUSTOMIZATION -> animateAndShowFragment(new AppWidgetVerticalSettingsFragment());

            case KEY_NEXT_ALARM_WIDGET_CUSTOMIZATION -> animateAndShowFragment(new AppWidgetNextAlarmSettingsFragment());

            case KEY_SALAH_WIDGET_CUSTOMIZATION -> animateAndShowFragment(new AppWidgetSalahSettingsFragment());
        }

        return true;
    }

    private void setupPreferences() {
        mAnalogWidgetCustomizationPref.setOnPreferenceClickListener(this);

        mDigitalWidgetCustomizationPref.setOnPreferenceClickListener(this);

        mVerticalWidgetCustomizationPref.setOnPreferenceClickListener(this);

        mNextAlarmWidgetCustomizationPref.setOnPreferenceClickListener(this);

        mSalahWidgetCustomizationPref.setOnPreferenceClickListener(this);
    }

    private void nullifyAllPrefs() {
        mAnalogWidgetCustomizationPref = null;
        mDigitalWidgetCustomizationPref = null;
        mVerticalWidgetCustomizationPref = null;
        mNextAlarmWidgetCustomizationPref = null;
        mSalahWidgetCustomizationPref = null;
    }

}
