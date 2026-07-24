// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static com.best.deskclock.settings.PreferencesKeys.KEY_ANALOG_WIDGET_CUSTOMIZATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_CUSTOMIZATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NAMAZ_COMPACT_WIDGET_CUSTOMIZATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NAMAZ_WIDGET_CUSTOMIZATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NEXT_ALARM_WIDGET_CUSTOMIZATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_WIDGET_CUSTOMIZATION;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.best.deskclock.R;

public class WidgetSettingsFragment extends ScreenFragment implements Preference.OnPreferenceClickListener {

    Preference mAnalogWidgetCustomizationPref;
    Preference mDigitalWidgetCustomizationPref;
    Preference mVerticalWidgetCustomizationPref;
    Preference mNextAlarmWidgetCustomizationPref;
    Preference mNamazWidgetCustomizationPref;
    Preference mNamazCompactWidgetCustomizationPref;

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
        mNamazWidgetCustomizationPref = findPreference(KEY_NAMAZ_WIDGET_CUSTOMIZATION);
        mNamazCompactWidgetCustomizationPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_CUSTOMIZATION);

        setupPreferences();
    }

    @Override
    public void onDestroy() {
        nullifyPreferenceListeners(mAnalogWidgetCustomizationPref, mDigitalWidgetCustomizationPref, mVerticalWidgetCustomizationPref,
            mNextAlarmWidgetCustomizationPref, mNamazWidgetCustomizationPref, mNamazCompactWidgetCustomizationPref);

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

            case KEY_NAMAZ_WIDGET_CUSTOMIZATION -> animateAndShowFragment(new AppWidgetNamazSettingsFragment());

            case KEY_NAMAZ_COMPACT_WIDGET_CUSTOMIZATION -> animateAndShowFragment(new AppWidgetNamazCompactSettingsFragment());
        }

        return true;
    }

    private void setupPreferences() {
        mAnalogWidgetCustomizationPref.setOnPreferenceClickListener(this);

        mDigitalWidgetCustomizationPref.setOnPreferenceClickListener(this);

        mVerticalWidgetCustomizationPref.setOnPreferenceClickListener(this);

        mNextAlarmWidgetCustomizationPref.setOnPreferenceClickListener(this);

        mNamazWidgetCustomizationPref.setOnPreferenceClickListener(this);

        mNamazCompactWidgetCustomizationPref.setOnPreferenceClickListener(this);
    }

    private void nullifyAllPrefs() {
        mAnalogWidgetCustomizationPref = null;
        mDigitalWidgetCustomizationPref = null;
        mVerticalWidgetCustomizationPref = null;
        mNextAlarmWidgetCustomizationPref = null;
        mNamazWidgetCustomizationPref = null;
        mNamazCompactWidgetCustomizationPref = null;
    }

}
