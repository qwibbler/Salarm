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
import com.best.deskclock.widgets.NamazCompactWidgetProvider;

import java.util.List;

public class AppWidgetNamazCompactSettingsFragment extends ScreenFragment implements Preference.OnPreferenceChangeListener {

    private int mAppWidgetId = INVALID_APPWIDGET_ID;

    SwitchPreferenceCompat mDisplayTextUppercasePref;
    SwitchPreferenceCompat mDisplayTextShadowPref;
    SwitchPreferenceCompat mShowBackgroundOnNamazCompactWidgetPref;
    SwitchPreferenceCompat mCustomizeBackgroundCornerRadiusPref;
    CustomSliderPreference mBackgroundCornerRadiusPref;
    SwitchPreferenceCompat mApplyHorizontalPaddingPref;
    SwitchPreferenceCompat mDefaultBackgroundColorPref;
    ColorPickerPreference mCustomBackgroundColorPref;

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

    CustomSliderPreference mNamazCompactWidgetMaxFontSizePref;

    SwitchPreferenceCompat mShowBothAsrsPref;
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
        return getString(R.string.namaz_compact_widget_label);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_customize_namaz_compact_widget);

        mDisplayTextUppercasePref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_DISPLAY_TEXT_UPPERCASE);
        mDisplayTextShadowPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_DISPLAY_TEXT_SHADOW);
        mShowBackgroundOnNamazCompactWidgetPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_DISPLAY_BACKGROUND);
        mCustomizeBackgroundCornerRadiusPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS);
        mBackgroundCornerRadiusPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_BACKGROUND_CORNER_RADIUS);
        mApplyHorizontalPaddingPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_APPLY_HORIZONTAL_PADDING);
        mDefaultBackgroundColorPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_DEFAULT_BACKGROUND_COLOR);
        mCustomBackgroundColorPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_CUSTOM_BACKGROUND_COLOR);

        mDefaultTimelineActiveColorPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_DEFAULT_TIMELINE_ACTIVE_COLOR);
        mCustomTimelineActiveColorPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_CUSTOM_TIMELINE_ACTIVE_COLOR);
        mDefaultTimelineNextColorPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_DEFAULT_TIMELINE_NEXT_COLOR);
        mCustomTimelineNextColorPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_CUSTOM_TIMELINE_NEXT_COLOR);
        mDefaultTimelineStandardColorPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_DEFAULT_TIMELINE_STANDARD_COLOR);
        mCustomTimelineStandardColorPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_CUSTOM_TIMELINE_STANDARD_COLOR);

        mDisplayItemHighlightPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_DISPLAY_ITEM_HIGHLIGHT);
        mDefaultItemActiveHighlightColorPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_DEFAULT_ITEM_ACTIVE_HIGHLIGHT_COLOR);
        mCustomItemActiveHighlightColorPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_CUSTOM_ITEM_ACTIVE_HIGHLIGHT_COLOR);
        mDefaultItemNextHighlightColorPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_DEFAULT_ITEM_NEXT_HIGHLIGHT_COLOR);
        mCustomItemNextHighlightColorPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_CUSTOM_ITEM_NEXT_HIGHLIGHT_COLOR);

        mNamazCompactWidgetMaxFontSizePref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_MAXIMUM_FONT_SIZE);

        mShowBothAsrsPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_SHOW_BOTH_ASRS);
        mShowFajrPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_SHOW_FAJR);
        mShowSunrisePref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_SHOW_SUNRISE);
        mShowDhuhrPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_SHOW_DHUHR);
        mShowAsrPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_SHOW_ASR);
        mShowAsrShafiPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_SHOW_ASR_SHAFI);
        mShowAsrHanafiPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_SHOW_ASR_HANAFI);
        mShowMaghribPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_SHOW_MAGHRIB);
        mShowIshaPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_SHOW_ISHA);

        mShowAlarmsPref = findPreference(KEY_NAMAZ_COMPACT_WIDGET_SHOW_ALARMS);

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
        updateNamazCompactWidget();
    }

    @Override
    public void onDestroy() {
        nullifyPreferenceListeners(mDisplayTextUppercasePref, mDisplayTextShadowPref, mShowBackgroundOnNamazCompactWidgetPref,
            mCustomizeBackgroundCornerRadiusPref, mBackgroundCornerRadiusPref, mApplyHorizontalPaddingPref, mDefaultBackgroundColorPref,
            mCustomBackgroundColorPref, mDefaultTimelineActiveColorPref,
            mCustomTimelineActiveColorPref, mDefaultTimelineNextColorPref, mCustomTimelineNextColorPref,
            mDefaultTimelineStandardColorPref, mCustomTimelineStandardColorPref,
            mDisplayItemHighlightPref, mDefaultItemActiveHighlightColorPref, mCustomItemActiveHighlightColorPref,
            mDefaultItemNextHighlightColorPref, mCustomItemNextHighlightColorPref,
            mNamazCompactWidgetMaxFontSizePref, mShowAlarmsPref);
        nullifyAllPrefs();
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        String key = pref.getKey();
        if (KEY_NAMAZ_COMPACT_WIDGET_SHOW_BOTH_ASRS.equals(key)) {
            mPrefs.edit().putBoolean(KEY_NAMAZ_COMPACT_WIDGET_SHOW_BOTH_ASRS, (boolean) newValue).apply();
            updateEventsVisibility();
        }

        switch (key) {
            case KEY_NAMAZ_COMPACT_WIDGET_DISPLAY_TEXT_UPPERCASE, KEY_NAMAZ_COMPACT_WIDGET_DISPLAY_TEXT_SHADOW,
                 KEY_NAMAZ_COMPACT_WIDGET_APPLY_HORIZONTAL_PADDING, KEY_NAMAZ_COMPACT_WIDGET_SHOW_FAJR,
                 KEY_NAMAZ_COMPACT_WIDGET_SHOW_SUNRISE, KEY_NAMAZ_COMPACT_WIDGET_SHOW_DHUHR,
                 KEY_NAMAZ_COMPACT_WIDGET_SHOW_ASR, KEY_NAMAZ_COMPACT_WIDGET_SHOW_ASR_SHAFI,
                 KEY_NAMAZ_COMPACT_WIDGET_SHOW_ASR_HANAFI, KEY_NAMAZ_COMPACT_WIDGET_SHOW_MAGHRIB,
                 KEY_NAMAZ_COMPACT_WIDGET_SHOW_ISHA ->
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

            case KEY_NAMAZ_COMPACT_WIDGET_DISPLAY_BACKGROUND -> {
                boolean displayBackground = (boolean) newValue;
                boolean isCustomColor = !WidgetDAO.isNamazCompactWidgetDefaultBackgroundColor(mPrefs);
                boolean isRadiusCustomizable = WidgetDAO.isNamazCompactWidgetBackgroundCornerRadiusCustomizable(mPrefs);
                mCustomizeBackgroundCornerRadiusPref.setVisible(SdkUtils.isAtLeastAndroid12() ? displayBackground : displayBackground && isCustomColor);
                mBackgroundCornerRadiusPref.setVisible(SdkUtils.isAtLeastAndroid12() ? displayBackground && isRadiusCustomizable : displayBackground && isCustomColor && isRadiusCustomizable);
                mDefaultBackgroundColorPref.setVisible(displayBackground);
                mCustomBackgroundColorPref.setVisible(displayBackground && isCustomColor);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }
            case KEY_NAMAZ_COMPACT_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS -> {
                mBackgroundCornerRadiusPref.setVisible((boolean) newValue);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }
            case KEY_NAMAZ_COMPACT_WIDGET_DEFAULT_BACKGROUND_COLOR -> {
                boolean isCustomColor = !(boolean) newValue;
                boolean displayBackground = WidgetDAO.isBackgroundDisplayedOnNamazCompactWidget(mPrefs);
                boolean isRadiusCustomizable = WidgetDAO.isNamazCompactWidgetBackgroundCornerRadiusCustomizable(mPrefs);

                mCustomBackgroundColorPref.setVisible(isCustomColor);

                if (!SdkUtils.isAtLeastAndroid12()) {
                    mCustomizeBackgroundCornerRadiusPref.setVisible(isCustomColor && displayBackground);
                    mBackgroundCornerRadiusPref.setVisible(isCustomColor && displayBackground && isRadiusCustomizable);
                }

                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }
            case KEY_NAMAZ_COMPACT_WIDGET_DEFAULT_TIMELINE_ACTIVE_COLOR -> {
                mCustomTimelineActiveColorPref.setVisible(!(boolean) newValue);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }
            case KEY_NAMAZ_COMPACT_WIDGET_DEFAULT_TIMELINE_NEXT_COLOR -> {
                mCustomTimelineNextColorPref.setVisible(!(boolean) newValue);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }
            case KEY_NAMAZ_COMPACT_WIDGET_DEFAULT_TIMELINE_STANDARD_COLOR -> {
                mCustomTimelineStandardColorPref.setVisible(!(boolean) newValue);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }
            case KEY_NAMAZ_COMPACT_WIDGET_DISPLAY_ITEM_HIGHLIGHT -> {
                boolean displayHighlight = (boolean) newValue;
                mDefaultItemActiveHighlightColorPref.setVisible(displayHighlight);
                mCustomItemActiveHighlightColorPref.setVisible(displayHighlight && !WidgetDAO.isNamazCompactWidgetItemActiveHighlightDefaultColor(mPrefs));
                mDefaultItemNextHighlightColorPref.setVisible(displayHighlight);
                mCustomItemNextHighlightColorPref.setVisible(displayHighlight && !WidgetDAO.isNamazCompactWidgetItemNextHighlightDefaultColor(mPrefs));
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }
            case KEY_NAMAZ_COMPACT_WIDGET_DEFAULT_ITEM_ACTIVE_HIGHLIGHT_COLOR -> {
                mCustomItemActiveHighlightColorPref.setVisible(!(boolean) newValue);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }
            case KEY_NAMAZ_COMPACT_WIDGET_DEFAULT_ITEM_NEXT_HIGHLIGHT_COLOR -> {
                mCustomItemNextHighlightColorPref.setVisible(!(boolean) newValue);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_NAMAZ_COMPACT_WIDGET_SHOW_ALARMS -> {
                boolean showAlarms = (boolean) newValue;
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
                addRulePreferences(showAlarms);
            }
        }
        WidgetUtils.scheduleWidgetUpdate(requireContext(), NamazCompactWidgetProvider.class);
        return true;
    }

    private void setupPreferences() {
        mDisplayTextUppercasePref.setOnPreferenceChangeListener(this);
        mDisplayTextShadowPref.setOnPreferenceChangeListener(this);
        mShowBackgroundOnNamazCompactWidgetPref.setOnPreferenceChangeListener(this);
        boolean isBackgroundVisible = WidgetDAO.isBackgroundDisplayedOnNamazCompactWidget(mPrefs);
        boolean isBackgroundCornerRadiusCustomizable = WidgetDAO.isNamazCompactWidgetBackgroundCornerRadiusCustomizable(mPrefs);
        boolean isCustomColor = !WidgetDAO.isNamazCompactWidgetDefaultBackgroundColor(mPrefs);

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

        mDefaultTimelineActiveColorPref.setOnPreferenceChangeListener(this);
        mCustomTimelineActiveColorPref.setVisible(!WidgetDAO.isNamazCompactWidgetDefaultTimelineActiveColor(mPrefs));
        mCustomTimelineActiveColorPref.setOnPreferenceChangeListener(this);

        mDefaultTimelineNextColorPref.setOnPreferenceChangeListener(this);
        mCustomTimelineNextColorPref.setVisible(!WidgetDAO.isNamazCompactWidgetDefaultTimelineNextColor(mPrefs));
        mCustomTimelineNextColorPref.setOnPreferenceChangeListener(this);

        mDefaultTimelineStandardColorPref.setOnPreferenceChangeListener(this);
        mCustomTimelineStandardColorPref.setVisible(!WidgetDAO.isNamazCompactWidgetDefaultTimelineStandardColor(mPrefs));
        mCustomTimelineStandardColorPref.setOnPreferenceChangeListener(this);

        boolean isItemHighlightVisible = WidgetDAO.isItemHighlightDisplayedOnNamazCompactWidget(mPrefs);
        mDisplayItemHighlightPref.setOnPreferenceChangeListener(this);
        mDefaultItemActiveHighlightColorPref.setVisible(isItemHighlightVisible);
        mDefaultItemActiveHighlightColorPref.setOnPreferenceChangeListener(this);
        mCustomItemActiveHighlightColorPref.setVisible(isItemHighlightVisible && !WidgetDAO.isNamazCompactWidgetItemActiveHighlightDefaultColor(mPrefs));
        mCustomItemActiveHighlightColorPref.setOnPreferenceChangeListener(this);
        mDefaultItemNextHighlightColorPref.setVisible(isItemHighlightVisible);
        mDefaultItemNextHighlightColorPref.setOnPreferenceChangeListener(this);
        mCustomItemNextHighlightColorPref.setVisible(isItemHighlightVisible && !WidgetDAO.isNamazCompactWidgetItemNextHighlightDefaultColor(mPrefs));
        mCustomItemNextHighlightColorPref.setOnPreferenceChangeListener(this);

        mNamazCompactWidgetMaxFontSizePref.setOnPreferenceChangeListener(this);

        if (mShowBothAsrsPref != null) {
            if (!mPrefs.contains(KEY_NAMAZ_COMPACT_WIDGET_SHOW_BOTH_ASRS)) {
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
            addRulePreferences(WidgetDAO.isNamazCompactWidgetShowAlarms(mPrefs));
        }

        updateEventsVisibility();
    }

    private void addRulePreferences(boolean showAlarms) {
        PreferenceCategory category = findPreference("key_namaz_compact_widget_events_category");
        if (category == null) return;

        // Remove existing rule prefs first
        int count = category.getPreferenceCount();
        for (int i = count - 1; i >= 0; i--) {
            Preference p = category.getPreference(i);
            String key = p.getKey();
            if (key != null && key.startsWith(KEY_NAMAZ_COMPACT_WIDGET_SHOW_RULE_PREFIX)) {
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
                        rulePref.setKey(KEY_NAMAZ_COMPACT_WIDGET_SHOW_RULE_PREFIX + rule.id);
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
        boolean showBothAsr = WidgetDAO.isNamazCompactWidgetShowBothAsrs(mPrefs);
        if (mShowAsrPref != null) mShowAsrPref.setVisible(!showBothAsr);
        if (mShowAsrShafiPref != null) mShowAsrShafiPref.setVisible(showBothAsr);
        if (mShowAsrHanafiPref != null) mShowAsrHanafiPref.setVisible(showBothAsr);
    }

    private void saveCheckedPreferenceStates() {
        mDisplayTextUppercasePref.setChecked(WidgetDAO.isTextUppercaseDisplayedOnNamazCompactWidget(mPrefs));
        mDisplayTextShadowPref.setChecked(WidgetDAO.isTextShadowDisplayedOnNamazCompactWidget(mPrefs));
        mShowBackgroundOnNamazCompactWidgetPref.setChecked(WidgetDAO.isBackgroundDisplayedOnNamazCompactWidget(mPrefs));
        mCustomizeBackgroundCornerRadiusPref.setChecked(WidgetDAO.isNamazCompactWidgetBackgroundCornerRadiusCustomizable(mPrefs));
        mApplyHorizontalPaddingPref.setChecked(WidgetDAO.isNamazCompactWidgetHorizontalPaddingApplied(mPrefs));
        mDefaultBackgroundColorPref.setChecked(WidgetDAO.isNamazCompactWidgetDefaultBackgroundColor(mPrefs));

        mDefaultTimelineActiveColorPref.setChecked(WidgetDAO.isNamazCompactWidgetDefaultTimelineActiveColor(mPrefs));
        mDefaultTimelineNextColorPref.setChecked(WidgetDAO.isNamazCompactWidgetDefaultTimelineNextColor(mPrefs));
        mDefaultTimelineStandardColorPref.setChecked(WidgetDAO.isNamazCompactWidgetDefaultTimelineStandardColor(mPrefs));
        mDisplayItemHighlightPref.setChecked(WidgetDAO.isItemHighlightDisplayedOnNamazCompactWidget(mPrefs));
        mDefaultItemActiveHighlightColorPref.setChecked(WidgetDAO.isNamazCompactWidgetItemActiveHighlightDefaultColor(mPrefs));
        mDefaultItemNextHighlightColorPref.setChecked(WidgetDAO.isNamazCompactWidgetItemNextHighlightDefaultColor(mPrefs));

        if (mShowFajrPref != null) mShowFajrPref.setChecked(WidgetDAO.isNamazCompactWidgetFajrVisible(mPrefs));
        if (mShowSunrisePref != null) mShowSunrisePref.setChecked(WidgetDAO.isNamazCompactWidgetSunriseVisible(mPrefs));
        if (mShowDhuhrPref != null) mShowDhuhrPref.setChecked(WidgetDAO.isNamazCompactWidgetDhuhrVisible(mPrefs));
        if (mShowAsrPref != null) mShowAsrPref.setChecked(WidgetDAO.isNamazCompactWidgetAsrVisible(mPrefs));
        if (mShowAsrShafiPref != null) mShowAsrShafiPref.setChecked(WidgetDAO.isNamazCompactWidgetAsrShafiVisible(mPrefs));
        if (mShowAsrHanafiPref != null) mShowAsrHanafiPref.setChecked(WidgetDAO.isNamazCompactWidgetAsrHanafiVisible(mPrefs));
        if (mShowMaghribPref != null) mShowMaghribPref.setChecked(WidgetDAO.isNamazCompactWidgetMaghribVisible(mPrefs));
        if (mShowIshaPref != null) mShowIshaPref.setChecked(WidgetDAO.isNamazCompactWidgetIshaVisible(mPrefs));

        if (mShowAlarmsPref != null) mShowAlarmsPref.setChecked(WidgetDAO.isNamazCompactWidgetShowAlarms(mPrefs));
    }

    private void updateNamazCompactWidget() {
        AppWidgetManager wm = AppWidgetManager.getInstance(requireContext());
        NamazCompactWidgetProvider.updateAppWidget(requireContext(), wm, mAppWidgetId);
        Intent result = new Intent();
        result.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
        requireActivity().setResult(Activity.RESULT_OK, result);
    }

    private void nullifyAllPrefs() {
        mDisplayTextUppercasePref = null; mDisplayTextShadowPref = null;
        mShowBackgroundOnNamazCompactWidgetPref = null; mCustomizeBackgroundCornerRadiusPref = null;
        mBackgroundCornerRadiusPref = null; mApplyHorizontalPaddingPref = null;
        mDefaultBackgroundColorPref = null; mCustomBackgroundColorPref = null;
        mDefaultTimelineActiveColorPref = null; mCustomTimelineActiveColorPref = null;
        mDefaultTimelineNextColorPref = null; mCustomTimelineNextColorPref = null;
        mDefaultTimelineStandardColorPref = null; mCustomTimelineStandardColorPref = null;
        mDisplayItemHighlightPref = null;
        mDefaultItemActiveHighlightColorPref = null; mCustomItemActiveHighlightColorPref = null;
        mDefaultItemNextHighlightColorPref = null; mCustomItemNextHighlightColorPref = null;
        mNamazCompactWidgetMaxFontSizePref = null;
        mShowBothAsrsPref = null;
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
