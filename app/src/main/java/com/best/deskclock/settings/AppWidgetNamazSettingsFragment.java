// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;
import static com.best.deskclock.settings.PreferencesKeys.*;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.view.HapticFeedbackConstantsCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.namaz.NamazDatabase;
import com.best.deskclock.namaz.RelationalRule;
import com.best.deskclock.settings.custompreference.ColorPickerPreference;
import com.best.deskclock.settings.custompreference.CustomSliderPreference;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.utils.WidgetUtils;
import com.best.deskclock.widgets.NamazTimelineWidgetProvider;

import java.util.List;

public class AppWidgetNamazSettingsFragment extends ScreenFragment implements Preference.OnPreferenceChangeListener {

    private int mAppWidgetId = INVALID_APPWIDGET_ID;

    SwitchPreferenceCompat mDisplayTextUppercasePref;
    SwitchPreferenceCompat mShowSymbolsPref;
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
    SwitchPreferenceCompat mShowCountdownPref;
    SwitchPreferenceCompat mShowFajrPref;
    SwitchPreferenceCompat mShowSunrisePref;
    SwitchPreferenceCompat mShowDhuhrPref;
    SwitchPreferenceCompat mShowAsrPref;
    SwitchPreferenceCompat mShowAsrShafiPref;
    SwitchPreferenceCompat mShowAsrHanafiPref;
    SwitchPreferenceCompat mShowMaghribPref;
    SwitchPreferenceCompat mShowIshaPref;

    SwitchPreferenceCompat mShowAlarmsPref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.namaz_timeline_widget_label);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_customize_namaz_widget);

        mDisplayTextUppercasePref = findPreference(KEY_NAMAZ_WIDGET_DISPLAY_TEXT_UPPERCASE);
        mShowSymbolsPref = findPreference(KEY_NAMAZ_WIDGET_SHOW_SYMBOLS);
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
        mShowCountdownPref = findPreference(KEY_NAMAZ_WIDGET_SHOW_COUNTDOWN);
        mShowFajrPref = findPreference(KEY_NAMAZ_WIDGET_SHOW_FAJR);
        mShowSunrisePref = findPreference(KEY_NAMAZ_WIDGET_SHOW_SUNRISE);
        mShowDhuhrPref = findPreference(KEY_NAMAZ_WIDGET_SHOW_DHUHR);
        mShowAsrPref = findPreference(KEY_NAMAZ_WIDGET_SHOW_ASR);
        mShowAsrShafiPref = findPreference(KEY_NAMAZ_WIDGET_SHOW_ASR_SHAFI);
        mShowAsrHanafiPref = findPreference(KEY_NAMAZ_WIDGET_SHOW_ASR_HANAFI);
        mShowMaghribPref = findPreference(KEY_NAMAZ_WIDGET_SHOW_MAGHRIB);
        mShowIshaPref = findPreference(KEY_NAMAZ_WIDGET_SHOW_ISHA);

        mShowAlarmsPref = findPreference(KEY_NAMAZ_WIDGET_SHOW_ALARMS);

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
        nullifyPreferenceListeners(mDisplayTextUppercasePref, mShowSymbolsPref, mDisplayTextShadowPref, mShowBackgroundOnNamazWidgetPref,
            mCustomizeBackgroundCornerRadiusPref, mBackgroundCornerRadiusPref, mApplyHorizontalPaddingPref, mDefaultBackgroundColorPref,
            mCustomBackgroundColorPref, mDefaultCountdownTimeColorPref, mCustomCountdownTimeColorPref,
            mDefaultCountdownLabelColorPref, mCustomCountdownLabelColorPref, mDefaultTimelineActiveColorPref,
            mCustomTimelineActiveColorPref, mDefaultTimelineNextColorPref, mCustomTimelineNextColorPref,
            mDefaultTimelineStandardColorPref, mCustomTimelineStandardColorPref,
            mDisplayItemHighlightPref, mDefaultItemActiveHighlightColorPref, mCustomItemActiveHighlightColorPref,
            mDefaultItemNextHighlightColorPref, mCustomItemNextHighlightColorPref,
            mNamazWidgetMaxFontSizePref, mShowCountdownPref, mShowAlarmsPref);

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
            case KEY_NAMAZ_WIDGET_DISPLAY_TEXT_UPPERCASE, KEY_NAMAZ_WIDGET_SHOW_SYMBOLS,
                 KEY_NAMAZ_WIDGET_DISPLAY_TEXT_SHADOW,
                 KEY_NAMAZ_WIDGET_APPLY_HORIZONTAL_PADDING, KEY_NAMAZ_WIDGET_SHOW_COUNTDOWN,
                 KEY_NAMAZ_WIDGET_SHOW_FAJR,
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

            case KEY_NAMAZ_WIDGET_SHOW_ALARMS -> {
                boolean showAlarms = (boolean) newValue;
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
                addRulePreferences(showAlarms);
            }
        }

        WidgetUtils.scheduleWidgetUpdate(requireContext(), NamazTimelineWidgetProvider.class);
        return true;
    }

    private void setupPreferences() {
        mDisplayTextUppercasePref.setOnPreferenceChangeListener(this);
        mShowSymbolsPref.setOnPreferenceChangeListener(this);
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

        if (mShowCountdownPref != null) mShowCountdownPref.setOnPreferenceChangeListener(this);

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

        if (mShowAlarmsPref != null) {
            mShowAlarmsPref.setOnPreferenceChangeListener(this);
            addRulePreferences(WidgetDAO.isNamazWidgetShowAlarms(mPrefs));
        }

        updateEventsVisibility();
    }

    private void addRulePreferences(boolean showAlarms) {
        PreferenceCategory category = findPreference("key_namaz_widget_events_category");
        if (category == null) return;

        // Remove existing rule prefs first
        int count = category.getPreferenceCount();
        for (int i = count - 1; i >= 0; i--) {
            Preference p = category.getPreference(i);
            String key = p.getKey();
            if (key != null && key.startsWith(KEY_NAMAZ_WIDGET_SHOW_RULE_PREFIX)) {
                category.removePreference(p);
            }
        }

        if (showAlarms) {
            Context context = getContext();
            if (context == null) return;
            com.best.deskclock.base.AppExecutors.getDiskIO().execute(() -> {
                Context bgContext = getContext();
                if (bgContext == null) return;
                List<RelationalRule> rules = NamazDatabase.getDatabase(bgContext).relationalRuleDao().getAll();
                com.best.deskclock.base.AppExecutors.getMainThread().post(() -> {
                    if (!isAdded()) return;
                    for (RelationalRule rule : rules) {
                        SwitchPreferenceCompat rulePref = new SwitchPreferenceCompat(requireContext());
                        rulePref.setKey(KEY_NAMAZ_WIDGET_SHOW_RULE_PREFIX + rule.id);
                        rulePref.setTitle(rule.getRuleDescription(requireContext()));
                        rulePref.setLayoutResource(R.layout.settings_preference_layout);
                        rulePref.setSingleLineTitle(false);
                        rulePref.setDefaultValue(false);
                        rulePref.setOnPreferenceChangeListener(this);
                        category.addPreference(rulePref);
                    }
                });
            });
        }
    }

    private void updateEventsVisibility() {
        boolean showBothAsr = WidgetDAO.isNamazWidgetShowBothAsrs(mPrefs);
        if (mShowAsrPref != null) mShowAsrPref.setVisible(!showBothAsr);
        if (mShowAsrShafiPref != null) mShowAsrShafiPref.setVisible(showBothAsr);
        if (mShowAsrHanafiPref != null) mShowAsrHanafiPref.setVisible(showBothAsr);
    }

    private void saveCheckedPreferenceStates() {
        mDisplayTextUppercasePref.setChecked(WidgetDAO.isTextUppercaseDisplayedOnNamazWidget(mPrefs));
        mShowSymbolsPref.setChecked(WidgetDAO.isNamazWidgetSymbolsVisible(mPrefs));
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

        if (mShowCountdownPref != null) mShowCountdownPref.setChecked(WidgetDAO.isNamazWidgetCountdownVisible(mPrefs));

        if (mShowFajrPref != null) mShowFajrPref.setChecked(WidgetDAO.isNamazWidgetFajrVisible(mPrefs));
        if (mShowSunrisePref != null) mShowSunrisePref.setChecked(WidgetDAO.isNamazWidgetSunriseVisible(mPrefs));
        if (mShowDhuhrPref != null) mShowDhuhrPref.setChecked(WidgetDAO.isNamazWidgetDhuhrVisible(mPrefs));
        if (mShowAsrPref != null) mShowAsrPref.setChecked(WidgetDAO.isNamazWidgetAsrVisible(mPrefs));
        if (mShowAsrShafiPref != null) mShowAsrShafiPref.setChecked(WidgetDAO.isNamazWidgetAsrShafiVisible(mPrefs));
        if (mShowAsrHanafiPref != null) mShowAsrHanafiPref.setChecked(WidgetDAO.isNamazWidgetAsrHanafiVisible(mPrefs));
        if (mShowMaghribPref != null) mShowMaghribPref.setChecked(WidgetDAO.isNamazWidgetMaghribVisible(mPrefs));
        if (mShowIshaPref != null) mShowIshaPref.setChecked(WidgetDAO.isNamazWidgetIshaVisible(mPrefs));

        if (mShowAlarmsPref != null) mShowAlarmsPref.setChecked(WidgetDAO.isNamazWidgetShowAlarms(mPrefs));
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
        mShowSymbolsPref = null;
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
        mShowCountdownPref = null;
        mShowFajrPref = null;
        mShowSunrisePref = null;
        mShowDhuhrPref = null;
        mShowAsrPref = null;
        mShowAsrShafiPref = null;
        mShowAsrHanafiPref = null;
        mShowMaghribPref = null;
        mShowIshaPref = null;
        mShowAlarmsPref = null;
    }

}
