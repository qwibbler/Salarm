// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;
import static com.best.deskclock.settings.PreferencesKeys.*;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.view.HapticFeedbackConstantsCompat;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.settings.custompreference.ColorPickerPreference;
import com.best.deskclock.settings.custompreference.CustomSliderPreference;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.utils.WidgetUtils;
import com.best.deskclock.widgets.NamazCountdownWidgetProvider;

public class AppWidgetNamazCountdownSettingsFragment extends ScreenFragment implements Preference.OnPreferenceChangeListener {

    private int mAppWidgetId = INVALID_APPWIDGET_ID;

    SwitchPreferenceCompat mDisplayTextUppercasePref;
    SwitchPreferenceCompat mShowSymbolsPref;
    SwitchPreferenceCompat mDisplayTextShadowPref;
    SwitchPreferenceCompat mShowBackgroundPref;
    SwitchPreferenceCompat mCustomizeBackgroundCornerRadiusPref;
    CustomSliderPreference mBackgroundCornerRadiusPref;
    SwitchPreferenceCompat mApplyHorizontalPaddingPref;
    SwitchPreferenceCompat mDefaultBackgroundColorPref;
    ColorPickerPreference mCustomBackgroundColorPref;
    SwitchPreferenceCompat mDefaultTimeColorPref;
    ColorPickerPreference mCustomTimeColorPref;
    SwitchPreferenceCompat mDefaultLabelColorPref;
    ColorPickerPreference mCustomLabelColorPref;
    CustomSliderPreference mMaxFontSizePref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.namaz_countdown_widget_label);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_customize_namaz_countdown_widget);

        mDisplayTextUppercasePref = findPreference(KEY_NAMAZ_COUNTDOWN_WIDGET_DISPLAY_TEXT_UPPERCASE);
        mShowSymbolsPref = findPreference(KEY_NAMAZ_COUNTDOWN_WIDGET_SHOW_SYMBOLS);
        mDisplayTextShadowPref = findPreference(KEY_NAMAZ_COUNTDOWN_WIDGET_DISPLAY_TEXT_SHADOW);
        mShowBackgroundPref = findPreference(KEY_NAMAZ_COUNTDOWN_WIDGET_DISPLAY_BACKGROUND);
        mCustomizeBackgroundCornerRadiusPref = findPreference(KEY_NAMAZ_COUNTDOWN_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS);
        mBackgroundCornerRadiusPref = findPreference(KEY_NAMAZ_COUNTDOWN_WIDGET_BACKGROUND_CORNER_RADIUS);
        mApplyHorizontalPaddingPref = findPreference(KEY_NAMAZ_COUNTDOWN_WIDGET_APPLY_HORIZONTAL_PADDING);
        mDefaultBackgroundColorPref = findPreference(KEY_NAMAZ_COUNTDOWN_WIDGET_DEFAULT_BACKGROUND_COLOR);
        mCustomBackgroundColorPref = findPreference(KEY_NAMAZ_COUNTDOWN_WIDGET_CUSTOM_BACKGROUND_COLOR);
        mDefaultTimeColorPref = findPreference(KEY_NAMAZ_COUNTDOWN_WIDGET_DEFAULT_TIME_COLOR);
        mCustomTimeColorPref = findPreference(KEY_NAMAZ_COUNTDOWN_WIDGET_CUSTOM_TIME_COLOR);
        mDefaultLabelColorPref = findPreference(KEY_NAMAZ_COUNTDOWN_WIDGET_DEFAULT_LABEL_COLOR);
        mCustomLabelColorPref = findPreference(KEY_NAMAZ_COUNTDOWN_WIDGET_CUSTOM_LABEL_COLOR);
        mMaxFontSizePref = findPreference(KEY_NAMAZ_COUNTDOWN_WIDGET_MAXIMUM_FONT_SIZE);

        setupPreferences();

        WidgetUtils.addFinishOnBackPressedIfLaunchedFromWidget(this);

        requireActivity().setResult(Activity.RESULT_CANCELED);

        Intent intent = requireActivity().getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                mAppWidgetId = extras.getInt(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        saveCheckedPreferenceStates();
        updateWidget();
    }

    @Override
    public void onDestroy() {
        nullifyPreferenceListeners(mDisplayTextUppercasePref, mShowSymbolsPref, mDisplayTextShadowPref, mShowBackgroundPref,
            mCustomizeBackgroundCornerRadiusPref, mBackgroundCornerRadiusPref, mApplyHorizontalPaddingPref,
            mDefaultBackgroundColorPref, mCustomBackgroundColorPref, mDefaultTimeColorPref, mCustomTimeColorPref,
            mDefaultLabelColorPref, mCustomLabelColorPref, mMaxFontSizePref);
        nullifyAllPrefs();
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        String key = pref.getKey();
        switch (key) {
            case KEY_NAMAZ_COUNTDOWN_WIDGET_DISPLAY_TEXT_UPPERCASE, KEY_NAMAZ_COUNTDOWN_WIDGET_SHOW_SYMBOLS,
                 KEY_NAMAZ_COUNTDOWN_WIDGET_DISPLAY_TEXT_SHADOW,
                 KEY_NAMAZ_COUNTDOWN_WIDGET_APPLY_HORIZONTAL_PADDING ->
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

            case KEY_NAMAZ_COUNTDOWN_WIDGET_DISPLAY_BACKGROUND -> {
                boolean displayBackground = (boolean) newValue;
                boolean isCustomColor = !WidgetDAO.isNamazCountdownWidgetDefaultBackgroundColor(mPrefs);
                boolean isRadiusCustomizable = WidgetDAO.isNamazCountdownWidgetBackgroundCornerRadiusCustomizable(mPrefs);

                mCustomizeBackgroundCornerRadiusPref.setVisible(SdkUtils.isAtLeastAndroid12()
                    ? displayBackground
                    : displayBackground && isCustomColor);
                mBackgroundCornerRadiusPref.setVisible(SdkUtils.isAtLeastAndroid12()
                    ? displayBackground && isRadiusCustomizable
                    : displayBackground && isCustomColor && isRadiusCustomizable);
                mDefaultBackgroundColorPref.setVisible(displayBackground);
                mCustomBackgroundColorPref.setVisible(displayBackground && isCustomColor);

                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_NAMAZ_COUNTDOWN_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS -> {
                mBackgroundCornerRadiusPref.setVisible((boolean) newValue);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_NAMAZ_COUNTDOWN_WIDGET_DEFAULT_BACKGROUND_COLOR -> {
                boolean isCustomColor = !(boolean) newValue;
                boolean displayBackground = WidgetDAO.isBackgroundDisplayedOnNamazCountdownWidget(mPrefs);
                boolean isRadiusCustomizable = WidgetDAO.isNamazCountdownWidgetBackgroundCornerRadiusCustomizable(mPrefs);

                mCustomBackgroundColorPref.setVisible(isCustomColor);
                if (!SdkUtils.isAtLeastAndroid12()) {
                    mCustomizeBackgroundCornerRadiusPref.setVisible(isCustomColor && displayBackground);
                    mBackgroundCornerRadiusPref.setVisible(isCustomColor && displayBackground && isRadiusCustomizable);
                }
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_NAMAZ_COUNTDOWN_WIDGET_DEFAULT_TIME_COLOR -> {
                mCustomTimeColorPref.setVisible(!(boolean) newValue);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_NAMAZ_COUNTDOWN_WIDGET_DEFAULT_LABEL_COLOR -> {
                mCustomLabelColorPref.setVisible(!(boolean) newValue);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }
        }

        WidgetUtils.scheduleWidgetUpdate(requireContext(), NamazCountdownWidgetProvider.class);
        return true;
    }

    private void setupPreferences() {
        mDisplayTextUppercasePref.setOnPreferenceChangeListener(this);
        mShowSymbolsPref.setOnPreferenceChangeListener(this);
        mDisplayTextShadowPref.setOnPreferenceChangeListener(this);
        mShowBackgroundPref.setOnPreferenceChangeListener(this);

        boolean isBackgroundVisible = WidgetDAO.isBackgroundDisplayedOnNamazCountdownWidget(mPrefs);
        boolean isRadiusCustomizable = WidgetDAO.isNamazCountdownWidgetBackgroundCornerRadiusCustomizable(mPrefs);
        boolean isCustomColor = !WidgetDAO.isNamazCountdownWidgetDefaultBackgroundColor(mPrefs);

        if (SdkUtils.isAtLeastAndroid12()) {
            mCustomizeBackgroundCornerRadiusPref.setVisible(isBackgroundVisible);
            mBackgroundCornerRadiusPref.setVisible(isBackgroundVisible && isRadiusCustomizable);
        } else {
            mCustomizeBackgroundCornerRadiusPref.setVisible(isBackgroundVisible && isCustomColor);
            mBackgroundCornerRadiusPref.setVisible(isBackgroundVisible && isCustomColor && isRadiusCustomizable);
        }

        mCustomizeBackgroundCornerRadiusPref.setOnPreferenceChangeListener(this);
        mApplyHorizontalPaddingPref.setOnPreferenceChangeListener(this);
        mDefaultBackgroundColorPref.setVisible(isBackgroundVisible);
        mDefaultBackgroundColorPref.setOnPreferenceChangeListener(this);
        mCustomBackgroundColorPref.setVisible(isBackgroundVisible && isCustomColor);
        mCustomBackgroundColorPref.setOnPreferenceChangeListener(this);

        mDefaultTimeColorPref.setOnPreferenceChangeListener(this);
        mCustomTimeColorPref.setVisible(!WidgetDAO.isNamazCountdownWidgetDefaultTimeColor(mPrefs));
        mCustomTimeColorPref.setOnPreferenceChangeListener(this);

        mDefaultLabelColorPref.setOnPreferenceChangeListener(this);
        mCustomLabelColorPref.setVisible(!WidgetDAO.isNamazCountdownWidgetDefaultLabelColor(mPrefs));
        mCustomLabelColorPref.setOnPreferenceChangeListener(this);

        mMaxFontSizePref.setOnPreferenceChangeListener(this);
    }

    private void saveCheckedPreferenceStates() {
        mDisplayTextUppercasePref.setChecked(WidgetDAO.isTextUppercaseDisplayedOnNamazCountdownWidget(mPrefs));
        mShowSymbolsPref.setChecked(WidgetDAO.isNamazCountdownWidgetSymbolsVisible(mPrefs));
        mDisplayTextShadowPref.setChecked(WidgetDAO.isTextShadowDisplayedOnNamazCountdownWidget(mPrefs));
        mShowBackgroundPref.setChecked(WidgetDAO.isBackgroundDisplayedOnNamazCountdownWidget(mPrefs));
        mCustomizeBackgroundCornerRadiusPref.setChecked(WidgetDAO.isNamazCountdownWidgetBackgroundCornerRadiusCustomizable(mPrefs));
        mApplyHorizontalPaddingPref.setChecked(WidgetDAO.isNamazCountdownWidgetHorizontalPaddingApplied(mPrefs));
        mDefaultBackgroundColorPref.setChecked(WidgetDAO.isNamazCountdownWidgetDefaultBackgroundColor(mPrefs));
        mDefaultTimeColorPref.setChecked(WidgetDAO.isNamazCountdownWidgetDefaultTimeColor(mPrefs));
        mDefaultLabelColorPref.setChecked(WidgetDAO.isNamazCountdownWidgetDefaultLabelColor(mPrefs));
    }

    private void updateWidget() {
        AppWidgetManager wm = AppWidgetManager.getInstance(requireContext());
        NamazCountdownWidgetProvider.updateAppWidget(requireContext(), wm, mAppWidgetId);
        Intent result = new Intent();
        result.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
        requireActivity().setResult(Activity.RESULT_OK, result);
    }

    private void nullifyAllPrefs() {
        mDisplayTextUppercasePref = null;
        mShowSymbolsPref = null;
        mDisplayTextShadowPref = null;
        mShowBackgroundPref = null;
        mCustomizeBackgroundCornerRadiusPref = null;
        mBackgroundCornerRadiusPref = null;
        mApplyHorizontalPaddingPref = null;
        mDefaultBackgroundColorPref = null;
        mCustomBackgroundColorPref = null;
        mDefaultTimeColorPref = null;
        mCustomTimeColorPref = null;
        mDefaultLabelColorPref = null;
        mCustomLabelColorPref = null;
        mMaxFontSizePref = null;
    }
}
