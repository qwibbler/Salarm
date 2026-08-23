// SPDX-License-Identifier: GPL-3.0-only

package com.qwibbler.salarm.settings;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;
import static com.qwibbler.salarm.settings.PreferencesKeys.*;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.HapticFeedbackConstantsCompat;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.base.BaseSettingsScreenFragment;
import com.qwibbler.salarm.data.WidgetDAO;
import com.qwibbler.salarm.salah.SalahDatabase;
import com.qwibbler.salarm.salah.RelationalRule;
import com.qwibbler.salarm.provider.Alarm;
import com.qwibbler.salarm.settings.custompreference.ColorPickerPreference;
import com.qwibbler.salarm.settings.custompreference.CustomSliderPreference;
import com.qwibbler.salarm.utils.SdkUtils;
import com.qwibbler.salarm.utils.Utils;
import com.qwibbler.salarm.utils.WidgetUtils;
import com.qwibbler.salarm.widgets.SalahTimelineWidgetProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppWidgetSalahSettingsFragment extends BaseSettingsScreenFragment implements Preference.OnPreferenceChangeListener, android.content.SharedPreferences.OnSharedPreferenceChangeListener {

    private int mAppWidgetId = INVALID_APPWIDGET_ID;

    SwitchPreferenceCompat mDisplayTextUppercasePref;
    SwitchPreferenceCompat mShowSymbolsPref;
    SwitchPreferenceCompat mDisplayTextShadowPref;
    SwitchPreferenceCompat mShowBackgroundOnSalahWidgetPref;
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

    CustomSliderPreference mSalahWidgetMaxFontSizePref;

    SwitchPreferenceCompat mShowCountdownPref;
    Preference mVisibleEventsPref;

    SwitchPreferenceCompat mShowAlarmsPref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.salah_timeline_widget_label);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_customize_salah_widget);

        mDisplayTextUppercasePref = findPreference(KEY_SALAH_WIDGET_DISPLAY_TEXT_UPPERCASE);
        mShowSymbolsPref = findPreference(KEY_SALAH_WIDGET_SHOW_SYMBOLS);
        mDisplayTextShadowPref = findPreference(KEY_SALAH_WIDGET_DISPLAY_TEXT_SHADOW);
        mShowBackgroundOnSalahWidgetPref = findPreference(KEY_SALAH_WIDGET_DISPLAY_BACKGROUND);
        mCustomizeBackgroundCornerRadiusPref = findPreference(KEY_SALAH_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS);
        mBackgroundCornerRadiusPref = findPreference(KEY_SALAH_WIDGET_BACKGROUND_CORNER_RADIUS);
        mApplyHorizontalPaddingPref = findPreference(KEY_SALAH_WIDGET_APPLY_HORIZONTAL_PADDING);
        mDefaultBackgroundColorPref = findPreference(KEY_SALAH_WIDGET_DEFAULT_BACKGROUND_COLOR);
        mCustomBackgroundColorPref = findPreference(KEY_SALAH_WIDGET_CUSTOM_BACKGROUND_COLOR);

        mDefaultCountdownTimeColorPref = findPreference(KEY_SALAH_WIDGET_DEFAULT_COUNTDOWN_TIME_COLOR);
        mCustomCountdownTimeColorPref = findPreference(KEY_SALAH_WIDGET_CUSTOM_COUNTDOWN_TIME_COLOR);
        mDefaultCountdownLabelColorPref = findPreference(KEY_SALAH_WIDGET_DEFAULT_COUNTDOWN_LABEL_COLOR);
        mCustomCountdownLabelColorPref = findPreference(KEY_SALAH_WIDGET_CUSTOM_COUNTDOWN_LABEL_COLOR);
        mDefaultTimelineActiveColorPref = findPreference(KEY_SALAH_WIDGET_DEFAULT_TIMELINE_ACTIVE_COLOR);
        mCustomTimelineActiveColorPref = findPreference(KEY_SALAH_WIDGET_CUSTOM_TIMELINE_ACTIVE_COLOR);
        mDefaultTimelineNextColorPref = findPreference(KEY_SALAH_WIDGET_DEFAULT_TIMELINE_NEXT_COLOR);
        mCustomTimelineNextColorPref = findPreference(KEY_SALAH_WIDGET_CUSTOM_TIMELINE_NEXT_COLOR);
        mDefaultTimelineStandardColorPref = findPreference(KEY_SALAH_WIDGET_DEFAULT_TIMELINE_STANDARD_COLOR);
        mCustomTimelineStandardColorPref = findPreference(KEY_SALAH_WIDGET_CUSTOM_TIMELINE_STANDARD_COLOR);

        mDisplayItemHighlightPref = findPreference(KEY_SALAH_WIDGET_DISPLAY_ITEM_HIGHLIGHT);
        mDefaultItemActiveHighlightColorPref = findPreference(KEY_SALAH_WIDGET_DEFAULT_ITEM_ACTIVE_HIGHLIGHT_COLOR);
        mCustomItemActiveHighlightColorPref = findPreference(KEY_SALAH_WIDGET_CUSTOM_ITEM_ACTIVE_HIGHLIGHT_COLOR);
        mDefaultItemNextHighlightColorPref = findPreference(KEY_SALAH_WIDGET_DEFAULT_ITEM_NEXT_HIGHLIGHT_COLOR);
        mCustomItemNextHighlightColorPref = findPreference(KEY_SALAH_WIDGET_CUSTOM_ITEM_NEXT_HIGHLIGHT_COLOR);

        mSalahWidgetMaxFontSizePref = findPreference(KEY_SALAH_WIDGET_MAXIMUM_FONT_SIZE);

        mShowCountdownPref = findPreference(KEY_SALAH_WIDGET_SHOW_COUNTDOWN);
        mVisibleEventsPref = findPreference(KEY_SALAH_WIDGET_VISIBLE_EVENTS);

        mShowAlarmsPref = findPreference(KEY_SALAH_WIDGET_SHOW_ALARMS);

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
        if (mPrefs != null) {
            mPrefs.registerOnSharedPreferenceChangeListener(this);
        }

        saveCheckedPreferenceStates();

        updateSalahWidget();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPrefs != null) {
            mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onSharedPreferenceChanged(android.content.SharedPreferences sharedPreferences, String key) {
        WidgetUtils.scheduleWidgetUpdate(requireContext(), SalahTimelineWidgetProvider.class);
    }

    @Override
    public void onDestroy() {
        nullifyPreferenceListeners(mDisplayTextUppercasePref, mShowSymbolsPref, mDisplayTextShadowPref, mShowBackgroundOnSalahWidgetPref,
            mCustomizeBackgroundCornerRadiusPref, mBackgroundCornerRadiusPref, mApplyHorizontalPaddingPref, mDefaultBackgroundColorPref,
            mCustomBackgroundColorPref, mDefaultCountdownTimeColorPref, mCustomCountdownTimeColorPref,
            mDefaultCountdownLabelColorPref, mCustomCountdownLabelColorPref, mDefaultTimelineActiveColorPref,
            mCustomTimelineActiveColorPref, mDefaultTimelineNextColorPref, mCustomTimelineNextColorPref,
            mDefaultTimelineStandardColorPref, mCustomTimelineStandardColorPref,
            mDisplayItemHighlightPref, mDefaultItemActiveHighlightColorPref, mCustomItemActiveHighlightColorPref,
            mDefaultItemNextHighlightColorPref, mCustomItemNextHighlightColorPref,
            mSalahWidgetMaxFontSizePref, mShowCountdownPref, mShowAlarmsPref);

        nullifyAllPrefs();

        super.onDestroy();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (KEY_SALAH_WIDGET_VISIBLE_EVENTS.equals(preference.getKey())) {
            showVisibleEventsDialog();
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        String key = pref.getKey();

        switch (key) {
            case KEY_SALAH_WIDGET_DISPLAY_TEXT_UPPERCASE, KEY_SALAH_WIDGET_SHOW_SYMBOLS,
                 KEY_SALAH_WIDGET_DISPLAY_TEXT_SHADOW,
                 KEY_SALAH_WIDGET_APPLY_HORIZONTAL_PADDING, KEY_SALAH_WIDGET_SHOW_COUNTDOWN ->
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

            case KEY_SALAH_WIDGET_DISPLAY_BACKGROUND -> {
                boolean displayBackground = (boolean) newValue;
                boolean isCustomColor = !WidgetDAO.isSalahWidgetDefaultBackgroundColor(mPrefs);
                boolean isRadiusCustomizable = WidgetDAO.isSalahWidgetBackgroundCornerRadiusCustomizable(mPrefs);

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

            case KEY_SALAH_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS -> {
                mBackgroundCornerRadiusPref.setVisible((boolean) newValue);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_SALAH_WIDGET_DEFAULT_BACKGROUND_COLOR -> {
                boolean isCustomColor = !(boolean) newValue;
                boolean displayBackground = WidgetDAO.isBackgroundDisplayedOnSalahWidget(mPrefs);
                boolean isRadiusCustomizable = WidgetDAO.isSalahWidgetBackgroundCornerRadiusCustomizable(mPrefs);

                mCustomBackgroundColorPref.setVisible(isCustomColor);

                if (!SdkUtils.isAtLeastAndroid12()) {
                    mCustomizeBackgroundCornerRadiusPref.setVisible(isCustomColor && displayBackground);
                    mBackgroundCornerRadiusPref.setVisible(isCustomColor && displayBackground && isRadiusCustomizable);
                }

                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_SALAH_WIDGET_DEFAULT_COUNTDOWN_TIME_COLOR -> {
                mCustomCountdownTimeColorPref.setVisible(!(boolean) newValue);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_SALAH_WIDGET_DEFAULT_COUNTDOWN_LABEL_COLOR -> {
                mCustomCountdownLabelColorPref.setVisible(!(boolean) newValue);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_SALAH_WIDGET_DEFAULT_TIMELINE_ACTIVE_COLOR -> {
                mCustomTimelineActiveColorPref.setVisible(!(boolean) newValue);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_SALAH_WIDGET_DEFAULT_TIMELINE_NEXT_COLOR -> {
                mCustomTimelineNextColorPref.setVisible(!(boolean) newValue);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_SALAH_WIDGET_DEFAULT_TIMELINE_STANDARD_COLOR -> {
                mCustomTimelineStandardColorPref.setVisible(!(boolean) newValue);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_SALAH_WIDGET_DISPLAY_ITEM_HIGHLIGHT -> {
                boolean displayHighlight = (boolean) newValue;
                mDefaultItemActiveHighlightColorPref.setVisible(displayHighlight);
                mCustomItemActiveHighlightColorPref.setVisible(displayHighlight && !WidgetDAO.isSalahWidgetItemActiveHighlightDefaultColor(mPrefs));
                mDefaultItemNextHighlightColorPref.setVisible(displayHighlight);
                mCustomItemNextHighlightColorPref.setVisible(displayHighlight && !WidgetDAO.isSalahWidgetItemNextHighlightDefaultColor(mPrefs));
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_SALAH_WIDGET_DEFAULT_ITEM_ACTIVE_HIGHLIGHT_COLOR -> {
                mCustomItemActiveHighlightColorPref.setVisible(!(boolean) newValue);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_SALAH_WIDGET_DEFAULT_ITEM_NEXT_HIGHLIGHT_COLOR -> {
                mCustomItemNextHighlightColorPref.setVisible(!(boolean) newValue);
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_SALAH_WIDGET_SHOW_ALARMS -> {
                boolean showAlarms = (boolean) newValue;
                if (showAlarms) {
                    AppExecutors.getDiskIO().execute(() -> {
                        Set<String> visibleEvents = new HashSet<>(WidgetDAO.getSalahWidgetVisibleEvents(mPrefs));

                        // Only auto-add all rules if the set doesn't already contain any rule preferences.
                        // This avoids overwriting the user's manual exclusions when they toggle the setting.
                        boolean hasRulePreferences = false;
                        for (String event : visibleEvents) {
                            if (event.startsWith("rule_")) {
                                hasRulePreferences = true;
                                break;
                            }
                        }

                        if (!hasRulePreferences) {
                            List<RelationalRule> rules = SalahDatabase.getDatabase(requireContext()).relationalRuleDao().getAll();
                            for (RelationalRule rule : rules) {
                                visibleEvents.add("rule_" + rule.id);
                            }
                            mPrefs.edit().putStringSet(KEY_SALAH_WIDGET_VISIBLE_EVENTS, visibleEvents).apply();
                            WidgetUtils.scheduleWidgetUpdate(requireContext(), SalahTimelineWidgetProvider.class);
                        }
                    });
                }
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }
        }

        return true;
    }

    private void setupPreferences() {
        mDisplayTextUppercasePref.setOnPreferenceChangeListener(this);
        mShowSymbolsPref.setOnPreferenceChangeListener(this);
        mDisplayTextShadowPref.setOnPreferenceChangeListener(this);
        mShowBackgroundOnSalahWidgetPref.setOnPreferenceChangeListener(this);

        boolean isBackgroundVisible = WidgetDAO.isBackgroundDisplayedOnSalahWidget(mPrefs);
        boolean isBackgroundCornerRadiusCustomizable = WidgetDAO.isSalahWidgetBackgroundCornerRadiusCustomizable(mPrefs);
        boolean isCustomColor = !WidgetDAO.isSalahWidgetDefaultBackgroundColor(mPrefs);

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
        mCustomCountdownTimeColorPref.setVisible(!WidgetDAO.isSalahWidgetDefaultCountdownTimeColor(mPrefs));
        mCustomCountdownTimeColorPref.setOnPreferenceChangeListener(this);

        mDefaultCountdownLabelColorPref.setOnPreferenceChangeListener(this);
        mCustomCountdownLabelColorPref.setVisible(!WidgetDAO.isSalahWidgetDefaultCountdownLabelColor(mPrefs));
        mCustomCountdownLabelColorPref.setOnPreferenceChangeListener(this);

        mDefaultTimelineActiveColorPref.setOnPreferenceChangeListener(this);
        mCustomTimelineActiveColorPref.setVisible(!WidgetDAO.isSalahWidgetDefaultTimelineActiveColor(mPrefs));
        mCustomTimelineActiveColorPref.setOnPreferenceChangeListener(this);

        mDefaultTimelineNextColorPref.setOnPreferenceChangeListener(this);
        mCustomTimelineNextColorPref.setVisible(!WidgetDAO.isSalahWidgetDefaultTimelineNextColor(mPrefs));
        mCustomTimelineNextColorPref.setOnPreferenceChangeListener(this);

        mDefaultTimelineStandardColorPref.setOnPreferenceChangeListener(this);
        mCustomTimelineStandardColorPref.setVisible(!WidgetDAO.isSalahWidgetDefaultTimelineStandardColor(mPrefs));
        mCustomTimelineStandardColorPref.setOnPreferenceChangeListener(this);

        boolean isItemHighlightVisible = WidgetDAO.isItemHighlightDisplayedOnSalahWidget(mPrefs);
        mDisplayItemHighlightPref.setOnPreferenceChangeListener(this);
        mDefaultItemActiveHighlightColorPref.setVisible(isItemHighlightVisible);
        mDefaultItemActiveHighlightColorPref.setOnPreferenceChangeListener(this);
        mCustomItemActiveHighlightColorPref.setVisible(isItemHighlightVisible && !WidgetDAO.isSalahWidgetItemActiveHighlightDefaultColor(mPrefs));
        mCustomItemActiveHighlightColorPref.setOnPreferenceChangeListener(this);
        mDefaultItemNextHighlightColorPref.setVisible(isItemHighlightVisible);
        mDefaultItemNextHighlightColorPref.setOnPreferenceChangeListener(this);
        mCustomItemNextHighlightColorPref.setVisible(isItemHighlightVisible && !WidgetDAO.isSalahWidgetItemNextHighlightDefaultColor(mPrefs));
        mCustomItemNextHighlightColorPref.setOnPreferenceChangeListener(this);

        mSalahWidgetMaxFontSizePref.setOnPreferenceChangeListener(this);

        if (mShowCountdownPref != null) mShowCountdownPref.setOnPreferenceChangeListener(this);

        if (mShowAlarmsPref != null) {
            mShowAlarmsPref.setOnPreferenceChangeListener(this);
        }
    }

    private void showVisibleEventsDialog() {
        Context context = getContext();
        if (context == null) return;

        AppExecutors.getDiskIO().execute(() -> {
            List<EventItem> items = new ArrayList<>();
            Set<String> visibleEvents = WidgetDAO.getSalahWidgetVisibleEvents(mPrefs);
            boolean showBothAsr = WidgetDAO.isSalahWidgetShowBothAsrs(mPrefs);

            items.add(new EventItem("fajr", getString(R.string.fajr_label), null, visibleEvents.contains("fajr")));
            items.add(new EventItem("sunrise", getString(R.string.sunrise_label), null, visibleEvents.contains("sunrise")));
            items.add(new EventItem("dhuhr", getString(R.string.dhuhr_label), null, visibleEvents.contains("dhuhr")));
            if (showBothAsr) {
                items.add(new EventItem("asr_shafi", getString(R.string.asr_shafii_label), null, visibleEvents.contains("asr_shafi")));
                items.add(new EventItem("asr_hanafi", getString(R.string.asr_hanafi_label), null, visibleEvents.contains("asr_hanafi")));
            } else {
                items.add(new EventItem("asr", getString(R.string.asr_label), null, visibleEvents.contains("asr")));
            }
            items.add(new EventItem("maghrib", getString(R.string.maghrib_label), null, visibleEvents.contains("maghrib")));
            items.add(new EventItem("isha", getString(R.string.isha_label), null, visibleEvents.contains("isha")));

            if (WidgetDAO.isSalahWidgetShowAlarms(mPrefs)) {
                List<RelationalRule> rules = SalahDatabase.getDatabase(context).relationalRuleDao().getAll();
                for (RelationalRule rule : rules) {
                    String key = "rule_" + rule.id;
                    String subtitle = rule.getRuleDescription(context);
                    String title;

                    Alarm alarm = Alarm.getAlarmByRuleId(context.getContentResolver(), rule.id);
                    if (alarm != null && !alarm.label.isEmpty()) {
                        title = alarm.label;
                    } else {
                        title = RelationalRule.getEventName(context, rule.anchorEvent) + " Alarm";
                    }

                    items.add(new EventItem(key, title, subtitle, visibleEvents.contains(key)));
                }
            }

            AppExecutors.getMainThread().post(() -> {
                if (!isAdded()) return;

                ListView listView = new ListView(requireContext());
                EventAdapter adapter = new EventAdapter(requireContext(), items);
                listView.setAdapter(adapter);

                new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.visible_events_title)
                    .setView(listView)
                    .setPositiveButton(android.R.string.ok, (d, w) -> {
                        Set<String> selectedEvents = new HashSet<>(visibleEvents);
                        for (EventItem item : items) {
                            selectedEvents.remove(item.key);
                            if (item.selected) {
                                selectedEvents.add(item.key);
                            }
                        }
                        mPrefs.edit().putStringSet(KEY_SALAH_WIDGET_VISIBLE_EVENTS, selectedEvents).apply();
                        WidgetUtils.scheduleWidgetUpdate(requireContext(), SalahTimelineWidgetProvider.class);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            });
        });
    }

    private static class EventItem {
        final String key;
        final String title;
        final String subtitle;
        boolean selected;

        EventItem(String key, String title, String subtitle, boolean selected) {
            this.key = key;
            this.title = title;
            this.subtitle = subtitle;
            this.selected = selected;
        }
    }

    private static class EventAdapter extends ArrayAdapter<EventItem> {
        EventAdapter(Context context, List<EventItem> items) {
            super(context, R.layout.salah_visible_event_item, items);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.salah_visible_event_item, parent, false);
            }

            EventItem item = getItem(position);
            CheckBox checkBox = convertView.findViewById(R.id.event_checkbox);
            TextView titleView = convertView.findViewById(R.id.event_title);
            TextView subtitleView = convertView.findViewById(R.id.event_subtitle);

            if (item != null) {
                titleView.setText(item.title);
                if (item.subtitle != null && !item.subtitle.isEmpty()) {
                    subtitleView.setText(item.subtitle);
                    subtitleView.setVisibility(View.VISIBLE);
                } else {
                    subtitleView.setVisibility(View.GONE);
                }
                checkBox.setChecked(item.selected);
            }

            convertView.setOnClickListener(v -> {
                if (item != null) {
                    item.selected = !item.selected;
                    checkBox.setChecked(item.selected);
                }
            });

            return convertView;
        }
    }

    private void saveCheckedPreferenceStates() {
        mDisplayTextUppercasePref.setChecked(WidgetDAO.isTextUppercaseDisplayedOnSalahWidget(mPrefs));
        mShowSymbolsPref.setChecked(WidgetDAO.isSalahWidgetSymbolsVisible(mPrefs));
        mDisplayTextShadowPref.setChecked(WidgetDAO.isTextShadowDisplayedOnSalahWidget(mPrefs));
        mShowBackgroundOnSalahWidgetPref.setChecked(WidgetDAO.isBackgroundDisplayedOnSalahWidget(mPrefs));
        mCustomizeBackgroundCornerRadiusPref.setChecked(WidgetDAO.isSalahWidgetBackgroundCornerRadiusCustomizable(mPrefs));
        mApplyHorizontalPaddingPref.setChecked(WidgetDAO.isSalahWidgetHorizontalPaddingApplied(mPrefs));
        mDefaultBackgroundColorPref.setChecked(WidgetDAO.isSalahWidgetDefaultBackgroundColor(mPrefs));

        mDefaultCountdownTimeColorPref.setChecked(WidgetDAO.isSalahWidgetDefaultCountdownTimeColor(mPrefs));
        mDefaultCountdownLabelColorPref.setChecked(WidgetDAO.isSalahWidgetDefaultCountdownLabelColor(mPrefs));
        mDefaultTimelineActiveColorPref.setChecked(WidgetDAO.isSalahWidgetDefaultTimelineActiveColor(mPrefs));
        mDefaultTimelineNextColorPref.setChecked(WidgetDAO.isSalahWidgetDefaultTimelineNextColor(mPrefs));
        mDefaultTimelineStandardColorPref.setChecked(WidgetDAO.isSalahWidgetDefaultTimelineStandardColor(mPrefs));
        mDisplayItemHighlightPref.setChecked(WidgetDAO.isItemHighlightDisplayedOnSalahWidget(mPrefs));
        mDefaultItemActiveHighlightColorPref.setChecked(WidgetDAO.isSalahWidgetItemActiveHighlightDefaultColor(mPrefs));
        mDefaultItemNextHighlightColorPref.setChecked(WidgetDAO.isSalahWidgetItemNextHighlightDefaultColor(mPrefs));

        if (mShowCountdownPref != null) mShowCountdownPref.setChecked(WidgetDAO.isSalahWidgetCountdownVisible(mPrefs));

        if (mShowAlarmsPref != null) mShowAlarmsPref.setChecked(WidgetDAO.isSalahWidgetShowAlarms(mPrefs));
    }

    private void updateSalahWidget() {
        AppWidgetManager wm = AppWidgetManager.getInstance(requireContext());
        SalahTimelineWidgetProvider.updateAppWidget(requireContext(), wm, mAppWidgetId);

        Intent result = new Intent();
        result.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
        requireActivity().setResult(Activity.RESULT_OK, result);
    }

    private void nullifyAllPrefs() {
        mDisplayTextUppercasePref = null;
        mShowSymbolsPref = null;
        mDisplayTextShadowPref = null;
        mShowBackgroundOnSalahWidgetPref = null;
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
        mSalahWidgetMaxFontSizePref = null;
        mShowCountdownPref = null;
        mShowAlarmsPref = null;
    }

}
