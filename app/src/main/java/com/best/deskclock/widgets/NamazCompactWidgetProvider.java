// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.widgets;

import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT;
import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH;
import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT;
import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH;
import static android.util.TypedValue.COMPLEX_UNIT_PX;
import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.utils.WidgetUtils.METHOD_SET_IMAGE_ICON;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.batoulapps.adhan.CalculationMethod;
import com.best.deskclock.DeskClock;
import com.best.deskclock.R;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.namaz.NamazCalculator;
import com.best.deskclock.namaz.NamazDatabase;
import com.best.deskclock.namaz.RelationalRule;
import com.best.deskclock.settings.PreferencesKeys;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.WidgetUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Keep;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

/**
 * A vertical compact Namaz widget that displays all prayers (Initials left, Time right).
 * Stretches to fit available space.
 */
public class NamazCompactWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            relayoutWidget(context, appWidgetManager, appWidgetId, null);
        }
        NamazWidgetUpdateReceiver.scheduleNextUpdate(context);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        NamazWidgetUpdateReceiver.scheduleNextUpdate(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int widgetId, Bundle newOptions) {
        relayoutWidget(context, appWidgetManager, widgetId, newOptions);
    }

    private void relayoutWidget(Context context, AppWidgetManager wm, int widgetId, Bundle options) {
        com.best.deskclock.base.AppExecutors.getDiskIO().execute(() -> {
            Bundle finalOptions = options != null ? options : wm.getAppWidgetOptions(widgetId);
            SharedPreferences prefs = com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences(context);
            List<RelationalRule> rules = null;
            if (WidgetDAO.isNamazCompactWidgetShowAlarms(prefs)) {
                rules = NamazDatabase.getDatabase(context).relationalRuleDao().getAll();
            }
            List<RelationalRule> finalRules = rules;

            Date now = new Date();
            NamazCalculator.NamazTimes times = getNamazTimes(prefs, now);
            List<PrayerItem> allPrayers = times != null ? getAllPrayers(context, times, prefs, finalRules) : new ArrayList<>();

            com.best.deskclock.base.AppExecutors.getMainThread().post(() -> {
                final RemoteViews portrait = buildRemoteViews(context, wm, widgetId, finalOptions, true, times, allPrayers);
                final RemoteViews landscape = buildRemoteViews(context, wm, widgetId, finalOptions, false, times, allPrayers);

                final RemoteViews widget = new RemoteViews(landscape, portrait);
                wm.updateAppWidget(widgetId, widget);
            });
        });
    }

    private RemoteViews buildRemoteViews(Context context, AppWidgetManager wm, int widgetId, Bundle options, boolean portrait, NamazCalculator.NamazTimes times, List<PrayerItem> allPrayers) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.appwidget_namaz_compact);

        if (times == null) {
            RemoteViews itemRv = new RemoteViews(context.getPackageName(), R.layout.namaz_widget_item);
            itemRv.setViewVisibility(R.id.mode_compact_vertical, View.VISIBLE);
            itemRv.setTextViewText(R.id.time_compact, "Set Location");
            rv.addView(R.id.compact_items_container, itemRv);
            return rv;
        }

        int activeIndex = getActivePrayerIndex(allPrayers);
        int nextIndex = (activeIndex + 1) % allPrayers.size();

        final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        final float density = displayMetrics.density;
        final int minWidthPx = (int) (density * options.getInt(OPTION_APPWIDGET_MIN_WIDTH));
        final int minHeightPx = (int) (density * options.getInt(OPTION_APPWIDGET_MIN_HEIGHT));
        final int maxWidthPx = (int) (density * options.getInt(OPTION_APPWIDGET_MAX_WIDTH));
        final int maxHeightPx = (int) (density * options.getInt(OPTION_APPWIDGET_MAX_HEIGHT));

        final int targetWidthPx = portrait ? minWidthPx : maxWidthPx;
        final int targetHeightPx = portrait ? maxHeightPx : minHeightPx;

        int maxFontSize = WidgetDAO.getNamazCompactWidgetMaxFontSize(prefs);
        DigitalWidgetSizes sizes = optimizeSizes(context, targetWidthPx, targetHeightPx, maxFontSize, allPrayers);

        configureBackground(rv, context, prefs, targetWidthPx, targetHeightPx);
        configurePadding(rv, context, prefs);

        rv.removeAllViews(R.id.compact_items_container);
        SimpleDateFormat timeFormat = new SimpleDateFormat(DateFormat.is24HourFormat(context) ? "HH:mm" : "h:mm a", Locale.getDefault());
        boolean uppercase = WidgetDAO.isTextUppercaseDisplayedOnNamazCompactWidget(prefs);

        int activeColor = WidgetDAO.isNamazCompactWidgetDefaultTimelineActiveColor(prefs)
                ? ContextCompat.getColor(context, R.color.digital_widget_time_color)
                : WidgetDAO.getNamazCompactWidgetCustomTimelineActiveColor(prefs);

        int nextColor = WidgetDAO.isNamazCompactWidgetDefaultTimelineNextColor(prefs)
                ? ContextCompat.getColor(context, R.color.next_alarm_widget_title_color)
                : WidgetDAO.getNamazCompactWidgetCustomTimelineNextColor(prefs);

        int standardColor = WidgetDAO.isNamazCompactWidgetDefaultTimelineStandardColor(prefs)
                ? ContextCompat.getColor(context, R.color.widget_text_color)
                : WidgetDAO.getNamazCompactWidgetCustomTimelineStandardColor(prefs);

        for (int i = 0; i < allPrayers.size(); i++) {
            PrayerItem prayer = allPrayers.get(i);
            RemoteViews itemRv = new RemoteViews(context.getPackageName(), R.layout.namaz_widget_item);
            itemRv.setViewVisibility(R.id.mode_wide, View.GONE);
            itemRv.setViewVisibility(R.id.mode_narrow, View.GONE);
            itemRv.setViewVisibility(R.id.mode_compact_container, View.GONE);
            itemRv.setViewVisibility(R.id.mode_compact_vertical, View.VISIBLE);

            String initial;
            if (prayer.ruleId != -1) {
                initial = "🔔";
            } else if (prayer.name.equals(context.getString(R.string.asr_shafii_label))) {
                initial = "A1";
            } else if (prayer.name.equals(context.getString(R.string.asr_hanafi_label))) {
                initial = "A2";
            } else if (prayer.name.equals(context.getString(R.string.asr_label))) {
                initial = "A";
            } else {
                initial = prayer.name.substring(0, 1);
            }

            String time = timeFormat.format(prayer.time);
            if (uppercase) {
                if (prayer.ruleId == -1) {
                    initial = initial.toUpperCase();
                }
                time = time.toUpperCase();
            }

            int color = standardColor;
            if (i == activeIndex) color = activeColor;
            else if (i == nextIndex) color = nextColor;

            applyItemHighlight(itemRv, prefs, i, activeIndex, nextIndex, activeColor, nextColor);

            itemRv.setViewVisibility(R.id.initial_compact, View.VISIBLE);
            itemRv.setTextViewText(R.id.initial_compact, initial);
            itemRv.setTextViewTextSize(R.id.initial_compact, COMPLEX_UNIT_PX, sizes.mWidgetFontSizePx);
            itemRv.setTextColor(R.id.initial_compact, color);

            itemRv.setTextViewText(R.id.time_compact, time);
            itemRv.setTextViewTextSize(R.id.time_compact, COMPLEX_UNIT_PX, sizes.mWidgetFontSizePx);
            itemRv.setTextColor(R.id.time_compact, color);

            rv.addView(R.id.compact_items_container, itemRv);
        }

        Intent intent = new Intent(context, DeskClock.class);
        intent.putExtra(WidgetUtils.KEY_LAUNCHED_FROM_WIDGET, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.namaz_widget_root, pendingIntent);

        return rv;
    }

    private static void applyItemHighlight(RemoteViews itemRv, SharedPreferences prefs, int index,
                                          int activeIndex, int nextIndex, int activeColor, int nextColor) {
        if (!WidgetDAO.isItemHighlightDisplayedOnNamazCompactWidget(prefs)) {
            itemRv.setViewVisibility(R.id.item_background, View.GONE);
            return;
        }

        if (index == activeIndex || index == nextIndex) {
            int highlightColor;
            if (index == activeIndex) {
                highlightColor = WidgetDAO.isNamazCompactWidgetItemActiveHighlightDefaultColor(prefs)
                        ? ColorUtils.setAlphaComponent(activeColor, 0x4D) // ~30% alpha
                        : WidgetDAO.getNamazCompactWidgetItemActiveHighlightColor(prefs);
            } else {
                highlightColor = WidgetDAO.isNamazCompactWidgetItemNextHighlightDefaultColor(prefs)
                        ? ColorUtils.setAlphaComponent(nextColor, 0x26) // ~15% alpha
                        : WidgetDAO.getNamazCompactWidgetItemNextHighlightColor(prefs);
            }

            itemRv.setViewVisibility(R.id.item_background, View.VISIBLE);
            itemRv.setInt(R.id.item_background, "setColorFilter", highlightColor);
        } else {
            itemRv.setViewVisibility(R.id.item_background, View.GONE);
        }
    }

    private static void configureBackground(RemoteViews rv, Context context, SharedPreferences prefs, int widthPx, int heightPx) {
        if (!WidgetDAO.isBackgroundDisplayedOnNamazCompactWidget(prefs) || widthPx <= 0 || heightPx <= 0) {
            rv.setImageViewResource(R.id.widget_background, 0);
            return;
        }

        int radius = (int) dpToPx(WidgetDAO.isNamazCompactWidgetBackgroundCornerRadiusCustomizable(prefs)
            ? WidgetDAO.getNamazCompactWidgetBackgroundCornerRadius(prefs)
            : 24, context.getResources().getDisplayMetrics());

        int color = WidgetDAO.isNamazCompactWidgetDefaultBackgroundColor(prefs)
                ? ContextCompat.getColor(context, R.color.widget_background_color)
                : WidgetDAO.getNamazCompactWidgetBackgroundColor(prefs);

        if (SdkUtils.isAtLeastAndroid12()) {
             if (WidgetDAO.isNamazCompactWidgetDefaultBackgroundColor(prefs)) {
                 Icon dayIcon = WidgetUtils.createRoundedIcon(widthPx, heightPx, WidgetUtils.getBackgroundColorDay(context), radius);
                 Icon nightIcon = WidgetUtils.createRoundedIcon(widthPx, heightPx, WidgetUtils.getBackgroundColorNight(context), radius);
                 rv.setIcon(R.id.widget_background, METHOD_SET_IMAGE_ICON, dayIcon, nightIcon);
             } else {
                 Icon icon = WidgetUtils.createRoundedIcon(widthPx, heightPx, color, radius);
                 rv.setIcon(R.id.widget_background, METHOD_SET_IMAGE_ICON, icon);
             }
        } else {
             Icon icon = WidgetUtils.createRoundedIcon(widthPx, heightPx, color, radius);
             rv.setIcon(R.id.widget_background, METHOD_SET_IMAGE_ICON, icon);
        }
    }

    private static void configurePadding(RemoteViews rv, Context context, SharedPreferences prefs) {
        int padding = (int) dpToPx(WidgetDAO.isNamazCompactWidgetHorizontalPaddingApplied(prefs)
                ? 20
                : 0, context.getResources().getDisplayMetrics());
        rv.setViewPadding(R.id.namaz_widget_container, padding, padding, padding, padding);
    }

    private DigitalWidgetSizes optimizeSizes(Context context, int targetWidthPx, int targetHeightPx, int maxFontSize, List<PrayerItem> prayers) {
        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams")
        View sizer = inflater.inflate(R.layout.appwidget_namaz_compact, null);

        LinearLayout container = sizer.findViewById(R.id.compact_items_container);
        for (int i = 0; i < prayers.size(); i++) {
            View item = inflater.inflate(R.layout.namaz_widget_item, container, false);
            item.findViewById(R.id.mode_compact_vertical).setVisibility(View.VISIBLE);
            container.addView(item);
        }

        SharedPreferences prefs = getDefaultSharedPreferences(context);
        int padding = (int) dpToPx(WidgetDAO.isNamazCompactWidgetHorizontalPaddingApplied(prefs)
                ? 20
                : 0, context.getResources().getDisplayMetrics());
        sizer.setPadding(padding, padding, padding, padding);

        DigitalWidgetSizes template = new DigitalWidgetSizes(targetWidthPx, targetHeightPx, (int)dpToPx(maxFontSize, context.getResources().getDisplayMetrics()));

        DigitalWidgetSizes low = measure(template, 10, sizer, prayers, context);
        DigitalWidgetSizes high = measure(template, template.getLargestFontSizePx(), sizer, prayers, context);

        if (!high.hasViolations()) return high;

        while (low.getWidgetFontSizePx() < high.getWidgetFontSizePx() - 1) {
            int mid = (low.getWidgetFontSizePx() + high.getWidgetFontSizePx()) / 2;
            DigitalWidgetSizes midSize = measure(template, mid, sizer, prayers, context);
            if (midSize.hasViolations()) high = midSize;
            else low = midSize;
        }
        return low;
    }

    private DigitalWidgetSizes measure(DigitalWidgetSizes template, int fontSizePx, View sizer, List<PrayerItem> prayers, Context context) {
        DigitalWidgetSizes measured = template.newSize();
        measured.setWidgetFontSizePx(fontSizePx, 1.0f);

        LinearLayout container = sizer.findViewById(R.id.compact_items_container);
        for (int i = 0; i < container.getChildCount(); i++) {
            View item = container.getChildAt(i);
            TextView tvInitial = item.findViewById(R.id.initial_compact);
            TextView tvTime = item.findViewById(R.id.time_compact);
            tvInitial.setTextSize(COMPLEX_UNIT_PX, fontSizePx);
            tvTime.setTextSize(COMPLEX_UNIT_PX, fontSizePx);

            String prayerName = prayers.get(i).name;
            String initial;
            if (prayerName.equals(context.getString(R.string.asr_shafii_label))) {
                initial = "A1";
            } else if (prayerName.equals(context.getString(R.string.asr_hanafi_label))) {
                initial = "A2";
            } else if (prayerName.equals(context.getString(R.string.asr_label))) {
                initial = "A";
            } else {
                initial = prayerName.substring(0, 1);
            }
            tvInitial.setText(initial);
            tvTime.setText("11:59 PM");
        }

        int widthSpec = View.MeasureSpec.makeMeasureSpec(template.mTargetWidthPx, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(template.mTargetHeightPx, View.MeasureSpec.UNSPECIFIED);
        sizer.measure(widthSpec, heightSpec);

        measured.mMeasuredWidthPx = sizer.getMeasuredWidth();
        measured.mMeasuredHeightPx = sizer.getMeasuredHeight();

        return measured;
    }

    private static NamazCalculator.NamazTimes getNamazTimes(SharedPreferences prefs, Date date) {
        String latStr = prefs.getString(PreferencesKeys.KEY_NAMAZ_LATITUDE, null);
        String lonStr = prefs.getString(PreferencesKeys.KEY_NAMAZ_LONGITUDE, null);
        if (latStr == null || lonStr == null) return null;
        try {
            double lat = Double.parseDouble(latStr);
            double lon = Double.parseDouble(lonStr);
            String methodStr = prefs.getString(PreferencesKeys.KEY_NAMAZ_CALC_METHOD, CalculationMethod.MUSLIM_WORLD_LEAGUE.name());
            return NamazCalculator.calculateAllTimes(date, lat, lon, CalculationMethod.valueOf(methodStr));
        } catch (Exception e) { return null; }
    }

    private static List<PrayerItem> getAllPrayers(Context context, NamazCalculator.NamazTimes times, SharedPreferences prefs, List<RelationalRule> rules) {
        String madhabStr = prefs.getString(PreferencesKeys.KEY_NAMAZ_MADHAB, "HANAFI");
        boolean isShafi = "SHAFI".equals(madhabStr);
        boolean showBothAsr = WidgetDAO.isNamazCompactWidgetShowBothAsrs(prefs);

        List<PrayerItem> items = new ArrayList<>();
        if (WidgetDAO.isNamazCompactWidgetFajrVisible(prefs)) {
            items.add(new PrayerItem(context.getString(R.string.fajr_label), times.fajr));
        }
        if (WidgetDAO.isNamazCompactWidgetSunriseVisible(prefs)) {
            items.add(new PrayerItem(context.getString(R.string.sunrise_label), times.sunrise));
        }
        if (WidgetDAO.isNamazCompactWidgetDhuhrVisible(prefs)) {
            items.add(new PrayerItem(context.getString(R.string.dhuhr_label), times.dhuhr));
        }

        if (showBothAsr) {
            if (WidgetDAO.isNamazCompactWidgetAsrShafiVisible(prefs)) {
                items.add(new PrayerItem(context.getString(R.string.asr_shafii_label), times.asrShafi));
            }
            if (WidgetDAO.isNamazCompactWidgetAsrHanafiVisible(prefs)) {
                items.add(new PrayerItem(context.getString(R.string.asr_hanafi_label), times.asrHanafi));
            }
        } else {
            if (WidgetDAO.isNamazCompactWidgetAsrVisible(prefs)) {
                items.add(new PrayerItem(context.getString(R.string.asr_label), isShafi ? times.asrShafi : times.asrHanafi));
            }
        }

        if (WidgetDAO.isNamazCompactWidgetMaghribVisible(prefs)) {
            items.add(new PrayerItem(context.getString(R.string.maghrib_label), times.maghrib));
        }
        if (WidgetDAO.isNamazCompactWidgetIshaVisible(prefs)) {
            items.add(new PrayerItem(context.getString(R.string.isha_label), times.isha));
        }

        if (WidgetDAO.isNamazCompactWidgetShowAlarms(prefs) && rules != null) {
            Date now = new Date();
            Calendar tomorrowCal = Calendar.getInstance();
            tomorrowCal.add(Calendar.DAY_OF_YEAR, 1);
            NamazCalculator.NamazTimes tomorrowTimes = getNamazTimes(prefs, tomorrowCal.getTime());

            for (RelationalRule rule : rules) {
                if (WidgetDAO.isNamazCompactWidgetRuleVisible(prefs, rule.id)) {
                    addAlarmIfValid(context, items, rule, times, now);
                    if (tomorrowTimes != null) {
                        addAlarmIfValid(context, items, rule, tomorrowTimes, now);
                    }
                }
            }
            Collections.sort(items, new Comparator<PrayerItem>() {
                @Override
                public int compare(PrayerItem o1, PrayerItem o2) {
                    return o1.time.compareTo(o2.time);
                }
            });
        }
        return items;
    }

    private static void addAlarmIfValid(Context context, List<PrayerItem> items, RelationalRule rule, NamazCalculator.NamazTimes times, Date now) {
        Date ruleTime = RelationalRule.evaluateTime(rule, times, new Date());
        if (ruleTime.getTime() > 0) {
            long diff = ruleTime.getTime() - now.getTime();
            if (Math.abs(diff) < TimeUnit.HOURS.toMillis(24)) {
                com.best.deskclock.provider.Alarm alarm = com.best.deskclock.provider.Alarm.getAlarmByRuleId(context.getContentResolver(), rule.id);
                if (alarm != null && !alarm.daysOfWeek.isRepeating()) {
                    if (now.getTime() - ruleTime.getTime() > TimeUnit.MINUTES.toMillis(30)) {
                        return;
                    }
                }
                String label = (alarm != null && !alarm.label.isEmpty()) ? alarm.label : (RelationalRule.getEventName(context, rule.anchorEvent) + " Alarm");
                items.add(new PrayerItem(label, ruleTime, rule.id));
            }
        }
    }

    private static int getActivePrayerIndex(List<PrayerItem> items) {
        Date now = new Date();
        for (int i = items.size() - 1; i >= 0; i--) { if (now.after(items.get(i).time)) return i; }
        return items.size() - 1;
    }

    @Keep
    public static void updateAppWidget(Context context, AppWidgetManager wm, int widgetId) {
        new NamazCompactWidgetProvider().relayoutWidget(context, wm, widgetId, null);
    }

    private static class PrayerItem {
        String name; Date time; long ruleId = -1;
        PrayerItem(String name, Date time) { this.name = name; this.time = time; }
        PrayerItem(String name, Date time, long ruleId) { this.name = name; this.time = time; this.ruleId = ruleId; }
    }
}
