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
import com.best.deskclock.widgets.NamazTimelineWidgetProvider;

public class AppWidgetNamazSettingsFragment extends ScreenFragment implements Preference.OnPreferenceChangeListener {

    private int mAppWidgetId = INVALID_APPWIDGET_ID;

    SwitchPreferenceCompat mDisplayTextUppercasePref;
    SwitchPreferenceCompat mDisplayTextShadowPref;
    SwitchPreferenceCompat mShowBackgroundOnNamazWidgetPref;
    SwitchPreferenceCompat mCustomizeBackgroundCornerRadiusPref;
    CustomSliderPreference mBackgroundCornerRadiusPref;
    SwitchPreferenceCompat mApplyHorizontalPaddingPref;
    SwitchPreferenceCompat mDefaultBackgroundColorPref;
    ColorPickerPreference mCustomBackgroundColorPref;

    SwitchPreferenceCompat mDefaultCountdownTimeColorPref;
    ColorPickerPreference mCustomCountdownTimeColorPref;
    SwitchPreferenceCompat mDefaultCountdownLabelColorPref;
    ColorPickerPreference mCustomCountdownLabelColorPref;
    SwitchPreferenceCompat mDefaultTimelineActiveColorPref;
    ColorPickerPreference mCustomTimelineActiveColorPref;
    SwitchPreferenceCompat mDefaultTimelineNextColorPref;
    ColorPickerPreference mCustomTimelineNextColorPref;
    SwitchPreferenceCompat mDefaultTimelineStandardColorPref;
    ColorPickerPreference mCustomTimelineStandardColorPref;

    SwitchPreferenceCompat mDisplayItemHighlightPref;
    SwitchPreferenceCompat mDefaultItemActiveHighlightColorPref;
    ColorPickerPreference mCustomItemActiveHighlightColorPref;
    SwitchPreferenceCompat mDefaultItemNextHighlightColorPref;
    ColorPickerPreference mCustomItemNextHighlightColorPref;

    CustomSliderPreference mNamazWidgetMaxFontSizePref;

    SwitchPreferenceCompat mShowBothAsrsPref;
    SwitchPreferenceCompat mShowFajrPref;
    SwitchPreferenceCompat mShowSunrisePref;
    SwitchPreferenceCompat mShowDhuhrPref;
    SwitchPreferenceCompat mShowAsrPref;
    SwitchPreferenceCompat mShowAsrShafiPref;
    SwitchPreferenceCompat mShowAsrHanafiPref;
    SwitchPreferenceCompat mShowMaghribPref;
    SwitchPreferenceCompat mShowIshaPref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.namaz_timeline_widget_label);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_customize_namaz_widget);

        mDisplayTextUppercasePref = findPreference(KEY_NAMAZ_WIDGET_DISPLAY_TEXT_UPPERCASE);
        mDisplayTextShadowPref = findPreference(KEY_NAMAZ_WIDGET_DISPLAY_TEXT_SHADOW);
        mShowBackgroundOnNamazWidgetPref = findPreference(KEY_NAMAZ_WIDGET_DISPLAY_BACKGROUND);
        mCustomizeBackgroundCornerRadiusPref = findPreference(KEY_NAMAZ_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS);
        mBackgroundCornerRadiusPref = findPreference(KEY_NAMAZ_WIDGET_BACKGROUND_CORNER_RADIUS);
        mApplyHorizontalPaddingPref = findPreference(KEY_NAMAZ_WIDGET_APPLY_HORIZONTAL_PADDING);
        mDefaultBackgroundColorPref = findPreference(KEY_NAMAZ_WIDGET_DEFAULT_BACKGROUND_COLOR);
        mCustomBackgroundColorPref = findPreference(KEY_NAMAZ_WIDGET_CUSTOM_BACKGROUND_COLOR);

        mDefaultCountdownTimeColorPref = findPreference(KEY_NAMAZ_WIDGET_DEFAULT_COUNTDOWN_TIME_COLOR);
        mCustomCountdownTimeColorPref = findPreference(KEY_NAMAZ_WIDGET_CUSTOM_COUNTDOWN_TIME_COLOR);
        mDefaultCountdownLabelColorPref = findPreference(KEY_NAMAZ_WIDGET_DEFAULT_COUNTDOWN_LABEL_COLOR);
        mCustomCountdownLabelColorPref = findPreference(KEY_NAMAZ_WIDGET_CUSTOM_COUNTDOWN_LABEL_COLOR);
        mDefaultTimelineActiveColorPref = findPreference(KEY_NAMAZ_WIDGET_DEFAULT_TIMELINE_ACTIVE_COLOR);
        mCustomTimelineActiveColorPref = findPreference(KEY_NAMAZ_WIDGET_CUSTOM_TIMELINE_ACTIVE_COLOR);
        mDefaultTimelineNextColorPref = findPreference(KEY_NAMAZ_WIDGET_DEFAULT_TIMELINE_NEXT_COLOR);
        mCustomTimelineNextColorPref = findPreference(KEY_NAMAZ_WIDGET_CUSTOM_TIMELINE_NEXT_COLOR);
        mDefaultTimelineStandardColorPref = findPreference(KEY_NAMAZ_WIDGET_DEFAULT_TIMELINE_STANDARD_COLOR);
        mCustomTimelineStandardColorPref = findPreference(KEY_NAMAZ_WIDGET_CUSTOM_TIMELINE_STANDARD_COLOR);

        mDisplayItemHighlightPref = findPreference(KEY_NAMAZ_WIDGET_DISPLAY_ITEM_HIGHLIGHT);
        mDefaultItemActiveHighlightColorPref = findPreference(KEY_NAMAZ_WIDGET_DEFAULT_ITEM_ACTIVE_HIGHLIGHT_COLOR);
        mCustomItemActiveHighlightColorPref = findPreference(KEY_NAMAZ_WIDGET_CUSTOM_ITEM_ACTIVE_HIGHLIGHT_COLOR);
        mDefaultItemNextHighlightColorPref = findPreference(KEY_NAMAZ_WIDGET_DEFAULT_ITEM_NEXT_HIGHLIGHT_COLOR);
        mCustomItemNextHighlightColorPref = findPreference(KEY_NAMAZ_WIDGET_CUSTOM_ITEM_NEXT_HIGHLIGHT_COLOR);

        mNamazWidgetMaxFontSizePref = findPreference(KEY_NAMAZ_WIDGET_MAXIMUM_FONT_SIZE);

        mShowBothAsrsPref = findPreference(KEY_NAMAZ_WIDGET_SHOW_BOTH_ASRS);
        mShowFajrPref = findPreference(KEY_NAMAZ_WIDGET_SHOW_FAJR);
        mShowSunrisePref = findPreference(KEY_NAMAZ_WIDGET_SHOW_SUNRISE);
        mShowDhuhrPref = findPreference(KEY_NAMAZ_WIDGET_SHOW_DHUHR);
        mShowAsrPref = findPreference(KEY_NAMAZ_WIDGET_SHOW_ASR);
        mShowAsrShafiPref = findPreference(KEY_NAMAZ_WIDGET_SHOW_ASR_SHAFI);
        mShowAsrHanafiPref = findPreference(KEY_NAMAZ_WIDGET_SHOW_ASR_HANAFI);
        mShowMaghribPref = findPreference(KEY_NAMAZ_WIDGET_SHOW_MAGHRIB);
        mShowIshaPref = findPreference(KEY_NAMAZ_WIDGET_SHOW_ISHA);

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

        updateNamazWidget();
    }

    @Override
    public void onDestroy() {
        nullifyPreferenceListeners(mDisplayTextUppercasePref, mDisplayTextShadowPref, mShowBackgroundOnNamazWidgetPref,
            mCustomizeBackgroundCornerRadiusPref, mBackgroundCornerRadiusPref, mApplyHorizontalPaddingPref, mDefaultBackgroundColorPref,
            mCustomBackgroundColorPref, mDefaultCountdownTimeColorPref, mCustomCountdownTimeColorPref,
            mDefaultCountdownLabelColorPref, mCustomCountdownLabelColorPref, mDefaultTimelineActiveColorPref,
            mCustomTimelineActiveColorPref, mDefaultTimelineNextColorPref, mCustomTimelineNextColorPref,
            mDefaultTimelineStandardColorPref, mCustomTimelineStandardColorPref,
            mDisplayItemHighlightPref, mDefaultItemActiveHighlightColorPref, mCustomItemActiveHighlightColorPref,
            mDefaultItemNextHighlightColorPref, mCustomItemNextHighlightColorPref,
            mNamazWidgetMaxFontSizePref);

        nullifyAllPrefs();

        super.onDestroy();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        String key = pref.getKey();
        if (KEY_NAMAZ_WIDGET_SHOW_BOTH_ASRS.equals(key)) {
            mPrefs.edit().putBoolean(KEY_NAMAZ_WIDGET_SHOW_BOTH_ASRS, (boolean) newValue).apply();
            updateEventsVisibility();
        }

        switch (key) {
            case KEY_NAMAZ_WIDGET_DISPLAY_TEXT_UPPERCASE, KEY_NAMAZ_WIDGET_DISPLAY_TEXT_SHADOW,
                 KEY_NAMAZ_WIDGET_APPLY_HORIZONTAL_PADDING, KEY_NAMAZ_WIDGET_SHOW_FAJR,
                 KEY_NAMAZ_WIDGET_SHOW_SUNRISE, KEY_NAMAZ_WIDGET_SHOW_DHUHR,
                 KEY_NAMAZ_WIDGET_SHOW_ASR, KEY_NAMAZ_WIDGET_SHOW_ASR_SHAFI,
                 KEY_NAMAZ_WIDGET_SHOW_ASR_HANAFI, KEY_NAMAZ_WIDGET_SHOW_MAGHRIB,
                 KEY_NAMAZ_WIDGET_SHOW_ISHA ->
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

            case KEY_NAMAZ_WIDGET_DISPLAY_BACKGROUND -> {
                boolean displayBackground = (boolean) newValue;
                boolean isCustomColor = !WidgetDAO.isNamazWidgetDefaultBackgroundColor(mPrefs);
                boolean isRadiusCustomizable = WidgetDAO.isNamazWidgetBackgroundCornerRadiusCustomizable(mPrefs);

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

            case KEY_NAMAZ_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS -> {
                mBackgroundCornerRadiusPref.setVisible((boolean) newValue);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_NAMAZ_WIDGET_DEFAULT_BACKGROUND_COLOR -> {
                boolean isCustomColor = !(boolean) newValue;
                boolean displayBackground = WidgetDAO.isBackgroundDisplayedOnNamazWidget(mPrefs);
                boolean isRadiusCustomizable = WidgetDAO.isNamazWidgetBackgroundCornerRadiusCustomizable(mPrefs);

                mCustomBackgroundColorPref.setVisible(isCustomColor);

                if (!SdkUtils.isAtLeastAndroid12()) {
                    mCustomizeBackgroundCornerRadiusPref.setVisible(isCustomColor && displayBackground);
                    mBackgroundCornerRadiusPref.setVisible(isCustomColor && displayBackground && isRadiusCustomizable);
                }

                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_NAMAZ_WIDGET_DEFAULT_COUNTDOWN_TIME_COLOR -> {
                mCustomCountdownTimeColorPref.setVisible(!(boolean) newValue);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_NAMAZ_WIDGET_DEFAULT_COUNTDOWN_LABEL_COLOR -> {
                mCustomCountdownLabelColorPref.setVisible(!(boolean) newValue);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_NAMAZ_WIDGET_DEFAULT_TIMELINE_ACTIVE_COLOR -> {
                mCustomTimelineActiveColorPref.setVisible(!(boolean) newValue);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_NAMAZ_WIDGET_DEFAULT_TIMELINE_NEXT_COLOR -> {
                mCustomTimelineNextColorPref.setVisible(!(boolean) newValue);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_NAMAZ_WIDGET_DEFAULT_TIMELINE_STANDARD_COLOR -> {
                mCustomTimelineStandardColorPref.setVisible(!(boolean) newValue);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_NAMAZ_WIDGET_DISPLAY_ITEM_HIGHLIGHT -> {
                boolean displayHighlight = (boolean) newValue;
                mDefaultItemActiveHighlightColorPref.setVisible(displayHighlight);
                mCustomItemActiveHighlightColorPref.setVisible(displayHighlight && !WidgetDAO.isNamazWidgetItemActiveHighlightDefaultColor(mPrefs));
                mDefaultItemNextHighlightColorPref.setVisible(displayHighlight);
                mCustomItemNextHighlightColorPref.setVisible(displayHighlight && !WidgetDAO.isNamazWidgetItemNextHighlightDefaultColor(mPrefs));
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_NAMAZ_WIDGET_DEFAULT_ITEM_ACTIVE_HIGHLIGHT_COLOR -> {
                mCustomItemActiveHighlightColorPref.setVisible(!(boolean) newValue);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_NAMAZ_WIDGET_DEFAULT_ITEM_NEXT_HIGHLIGHT_COLOR -> {
                mCustomItemNextHighlightColorPref.setVisible(!(boolean) newValue);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }
        }

        WidgetUtils.scheduleWidgetUpdate(requireContext(), NamazTimelineWidgetProvider.class);
        return true;
    }

    private void setupPreferences() {
        mDisplayTextUppercasePref.setOnPreferenceChangeListener(this);
        mDisplayTextShadowPref.setOnPreferenceChangeListener(this);
        mShowBackgroundOnNamazWidgetPref.setOnPreferenceChangeListener(this);

        boolean isBackgroundVisible = WidgetDAO.isBackgroundDisplayedOnNamazWidget(mPrefs);
        boolean isBackgroundCornerRadiusCustomizable = WidgetDAO.isNamazWidgetBackgroundCornerRadiusCustomizable(mPrefs);
        boolean isCustomColor = !WidgetDAO.isNamazWidgetDefaultBackgroundColor(mPrefs);

        if (SdkUtils.isAtLeastAndroid12()) {
            mCustomizeBackgroundCornerRadiusPref.setVisible(isBackgroundVisible);
            mBackgroundCornerRadiusPref.setVisible(isBackgroundVisible && isBackgroundCornerRadiusCustomizable);
        } else {
            mCustomizeBackgroundCornerRadiusPref.setVisible(isBackgroundVisible && isCustomColor);
            mBackgroundCornerRadiusPref.setVisible(isBackgroundVisible
                && isCustomColor
                && isBackgroundCornerRadiusCustomizable);
        }

        mCustomizeBackgroundCornerRadiusPref.setOnPreferenceChangeListener(this);
        mApplyHorizontalPaddingPref.setTitle(R.string.apply_padding_title);
        mApplyHorizontalPaddingPref.setOnPreferenceChangeListener(this);
        mDefaultBackgroundColorPref.setVisible(isBackgroundVisible);
        mDefaultBackgroundColorPref.setOnPreferenceChangeListener(this);
        mCustomBackgroundColorPref.setVisible(isBackgroundVisible && isCustomColor);
        mCustomBackgroundColorPref.setOnPreferenceChangeListener(this);

        mDefaultCountdownTimeColorPref.setOnPreferenceChangeListener(this);
        mCustomCountdownTimeColorPref.setVisible(!WidgetDAO.isNamazWidgetDefaultCountdownTimeColor(mPrefs));
        mCustomCountdownTimeColorPref.setOnPreferenceChangeListener(this);

        mDefaultCountdownLabelColorPref.setOnPreferenceChangeListener(this);
        mCustomCountdownLabelColorPref.setVisible(!WidgetDAO.isNamazWidgetDefaultCountdownLabelColor(mPrefs));
        mCustomCountdownLabelColorPref.setOnPreferenceChangeListener(this);

        mDefaultTimelineActiveColorPref.setOnPreferenceChangeListener(this);
        mCustomTimelineActiveColorPref.setVisible(!WidgetDAO.isNamazWidgetDefaultTimelineActiveColor(mPrefs));
        mCustomTimelineActiveColorPref.setOnPreferenceChangeListener(this);

        mDefaultTimelineNextColorPref.setOnPreferenceChangeListener(this);
        mCustomTimelineNextColorPref.setVisible(!WidgetDAO.isNamazWidgetDefaultTimelineNextColor(mPrefs));
        mCustomTimelineNextColorPref.setOnPreferenceChangeListener(this);

        mDefaultTimelineStandardColorPref.setOnPreferenceChangeListener(this);
        mCustomTimelineStandardColorPref.setVisible(!WidgetDAO.isNamazWidgetDefaultTimelineStandardColor(mPrefs));
        mCustomTimelineStandardColorPref.setOnPreferenceChangeListener(this);

        boolean isItemHighlightVisible = WidgetDAO.isItemHighlightDisplayedOnNamazWidget(mPrefs);
        mDisplayItemHighlightPref.setOnPreferenceChangeListener(this);
        mDefaultItemActiveHighlightColorPref.setVisible(isItemHighlightVisible);
        mDefaultItemActiveHighlightColorPref.setOnPreferenceChangeListener(this);
        mCustomItemActiveHighlightColorPref.setVisible(isItemHighlightVisible && !WidgetDAO.isNamazWidgetItemActiveHighlightDefaultColor(mPrefs));
        mCustomItemActiveHighlightColorPref.setOnPreferenceChangeListener(this);
        mDefaultItemNextHighlightColorPref.setVisible(isItemHighlightVisible);
        mDefaultItemNextHighlightColorPref.setOnPreferenceChangeListener(this);
        mCustomItemNextHighlightColorPref.setVisible(isItemHighlightVisible && !WidgetDAO.isNamazWidgetItemNextHighlightDefaultColor(mPrefs));
        mCustomItemNextHighlightColorPref.setOnPreferenceChangeListener(this);

        mNamazWidgetMaxFontSizePref.setOnPreferenceChangeListener(this);

        if (mShowBothAsrsPref != null) {
            if (!mPrefs.contains(KEY_NAMAZ_WIDGET_SHOW_BOTH_ASRS)) {
                mShowBothAsrsPref.setChecked(mPrefs.getBoolean(KEY_NAMAZ_SHOW_BOTH_ASR, false));
            }
            mShowBothAsrsPref.setOnPreferenceChangeListener(this);
        }
        if (mShowFajrPref != null) mShowFajrPref.setOnPreferenceChangeListener(this);
        if (mShowSunrisePref != null) mShowSunrisePref.setOnPreferenceChangeListener(this);
        if (mShowDhuhrPref != null) mShowDhuhrPref.setOnPreferenceChangeListener(this);
        if (mShowAsrPref != null) mShowAsrPref.setOnPreferenceChangeListener(this);
        if (mShowAsrShafiPref != null) mShowAsrShafiPref.setOnPreferenceChangeListener(this);
        if (mShowAsrHanafiPref != null) mShowAsrHanafiPref.setOnPreferenceChangeListener(this);
        if (mShowMaghribPref != null) mShowMaghribPref.setOnPreferenceChangeListener(this);
        if (mShowIshaPref != null) mShowIshaPref.setOnPreferenceChangeListener(this);

        updateEventsVisibility();
    }

    private void updateEventsVisibility() {
        boolean showBothAsr = WidgetDAO.isNamazWidgetShowBothAsrs(mPrefs);
        if (mShowAsrPref != null) mShowAsrPref.setVisible(!showBothAsr);
        if (mShowAsrShafiPref != null) mShowAsrShafiPref.setVisible(showBothAsr);
        if (mShowAsrHanafiPref != null) mShowAsrHanafiPref.setVisible(showBothAsr);
    }

    private void saveCheckedPreferenceStates() {
        mDisplayTextUppercasePref.setChecked(WidgetDAO.isTextUppercaseDisplayedOnNamazWidget(mPrefs));
        mDisplayTextShadowPref.setChecked(WidgetDAO.isTextShadowDisplayedOnNamazWidget(mPrefs));
        mShowBackgroundOnNamazWidgetPref.setChecked(WidgetDAO.isBackgroundDisplayedOnNamazWidget(mPrefs));
        mCustomizeBackgroundCornerRadiusPref.setChecked(WidgetDAO.isNamazWidgetBackgroundCornerRadiusCustomizable(mPrefs));
        mApplyHorizontalPaddingPref.setChecked(WidgetDAO.isNamazWidgetHorizontalPaddingApplied(mPrefs));
        mDefaultBackgroundColorPref.setChecked(WidgetDAO.isNamazWidgetDefaultBackgroundColor(mPrefs));

        mDefaultCountdownTimeColorPref.setChecked(WidgetDAO.isNamazWidgetDefaultCountdownTimeColor(mPrefs));
        mDefaultCountdownLabelColorPref.setChecked(WidgetDAO.isNamazWidgetDefaultCountdownLabelColor(mPrefs));
        mDefaultTimelineActiveColorPref.setChecked(WidgetDAO.isNamazWidgetDefaultTimelineActiveColor(mPrefs));
        mDefaultTimelineNextColorPref.setChecked(WidgetDAO.isNamazWidgetDefaultTimelineNextColor(mPrefs));
        mDefaultTimelineStandardColorPref.setChecked(WidgetDAO.isNamazWidgetDefaultTimelineStandardColor(mPrefs));
        mDisplayItemHighlightPref.setChecked(WidgetDAO.isItemHighlightDisplayedOnNamazWidget(mPrefs));
        mDefaultItemActiveHighlightColorPref.setChecked(WidgetDAO.isNamazWidgetItemActiveHighlightDefaultColor(mPrefs));
        mDefaultItemNextHighlightColorPref.setChecked(WidgetDAO.isNamazWidgetItemNextHighlightDefaultColor(mPrefs));

        if (mShowFajrPref != null) mShowFajrPref.setChecked(WidgetDAO.isNamazWidgetFajrVisible(mPrefs));
        if (mShowSunrisePref != null) mShowSunrisePref.setChecked(WidgetDAO.isNamazWidgetSunriseVisible(mPrefs));
        if (mShowDhuhrPref != null) mShowDhuhrPref.setChecked(WidgetDAO.isNamazWidgetDhuhrVisible(mPrefs));
        if (mShowAsrPref != null) mShowAsrPref.setChecked(WidgetDAO.isNamazWidgetAsrVisible(mPrefs));
        if (mShowAsrShafiPref != null) mShowAsrShafiPref.setChecked(WidgetDAO.isNamazWidgetAsrShafiVisible(mPrefs));
        if (mShowAsrHanafiPref != null) mShowAsrHanafiPref.setChecked(WidgetDAO.isNamazWidgetAsrHanafiVisible(mPrefs));
        if (mShowMaghribPref != null) mShowMaghribPref.setChecked(WidgetDAO.isNamazWidgetMaghribVisible(mPrefs));
        if (mShowIshaPref != null) mShowIshaPref.setChecked(WidgetDAO.isNamazWidgetIshaVisible(mPrefs));
    }

    private void updateNamazWidget() {
        AppWidgetManager wm = AppWidgetManager.getInstance(requireContext());
        NamazTimelineWidgetProvider.updateAppWidget(requireContext(), wm, mAppWidgetId);

        Intent result = new Intent();
        result.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
        requireActivity().setResult(Activity.RESULT_OK, result);
    }

    private void nullifyAllPrefs() {
        mDisplayTextUppercasePref = null;
        mDisplayTextShadowPref = null;
        mShowBackgroundOnNamazWidgetPref = null;
        mCustomizeBackgroundCornerRadiusPref = null;
        mBackgroundCornerRadiusPref = null;
        mApplyHorizontalPaddingPref = null;
        mDefaultBackgroundColorPref = null;
        mCustomBackgroundColorPref = null;
        mDefaultCountdownTimeColorPref = null;
        mCustomCountdownTimeColorPref = null;
        mDefaultCountdownLabelColorPref = null;
        mCustomCountdownLabelColorPref = null;
        mDefaultTimelineActiveColorPref = null;
        mCustomTimelineActiveColorPref = null;
        mDefaultTimelineNextColorPref = null;
        mCustomTimelineNextColorPref = null;
        mDefaultTimelineStandardColorPref = null;
        mCustomTimelineStandardColorPref = null;
        mDisplayItemHighlightPref = null;
        mDefaultItemActiveHighlightColorPref = null;
        mCustomItemActiveHighlightColorPref = null;
        mDefaultItemNextHighlightColorPref = null;
        mCustomItemNextHighlightColorPref = null;
        mNamazWidgetMaxFontSizePref = null;
        mShowBothAsrsPref = null;
        mShowFajrPref = null;
        mShowSunrisePref = null;
        mShowDhuhrPref = null;
        mShowAsrPref = null;
        mShowAsrShafiPref = null;
        mShowAsrHanafiPref = null;
        mShowMaghribPref = null;
        mShowIshaPref = null;
    }

}
