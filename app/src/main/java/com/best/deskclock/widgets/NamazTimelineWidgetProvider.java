// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.widgets;

import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT;
import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH;
import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT;
import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH;
import static android.util.TypedValue.COMPLEX_UNIT_PX;
import static android.view.View.MeasureSpec.UNSPECIFIED;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Keep;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

/**
 * An adaptive widget that displays the Namaz (prayer) timeline.
 * Features:
 * - Large countdown header.
 * - Responsive layout states (WIDE, NARROW).
 * - Full-width font scaling via measurement logic.
 * - Customizable colors and styles.
 * - Cyclical highlighting of active and next prayers.
 */
public class NamazTimelineWidgetProvider extends AppWidgetProvider {

    private static final int MODE_WIDE = 0;
    private static final int MODE_NARROW = 1;

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
        NamazWidgetUpdateReceiver.cancelUpdate(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        relayoutWidget(context, appWidgetManager, appWidgetId, newOptions);
    }

    private void relayoutWidget(Context context, AppWidgetManager wm, int widgetId, Bundle options) {
        com.best.deskclock.base.AppExecutors.getDiskIO().execute(() -> {
            Bundle finalOptions = options != null ? options : wm.getAppWidgetOptions(widgetId);
            SharedPreferences prefs = com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences(context);
            List<RelationalRule> rules = null;
            if (WidgetDAO.isNamazWidgetShowAlarms(prefs)) {
                rules = NamazDatabase.getDatabase(context).relationalRuleDao().getAll();
            }
            List<RelationalRule> finalRules = rules;

            // Pre-calculate NamazTimes
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
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.appwidget_namaz_timeline);

        if (times == null) {
            rv.setTextViewText(R.id.countdown_time, "Set Location");
            rv.setViewVisibility(R.id.countdown_label, View.GONE);
            rv.setViewVisibility(R.id.timeline_container, View.GONE);
            return rv;
        }

        PrayerItem nextPrayer = getNextPrayerFromItems(context, allPrayers, prefs);
        int activeIndex = getActivePrayerIndex(allPrayers);

        // 2. Responsive Mode Determination
        final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        final float density = displayMetrics.density;
        final int minWidthPx = (int) (density * options.getInt(OPTION_APPWIDGET_MIN_WIDTH));
        final int minHeightPx = (int) (density * options.getInt(OPTION_APPWIDGET_MIN_HEIGHT));
        final int maxWidthPx = (int) (density * options.getInt(OPTION_APPWIDGET_MAX_WIDTH));
        final int maxHeightPx = (int) (density * options.getInt(OPTION_APPWIDGET_MAX_HEIGHT));

        final int targetWidthPx = portrait ? minWidthPx : maxWidthPx;
        final int targetHeightPx = portrait ? maxHeightPx : minHeightPx;

        boolean showSymbols = WidgetDAO.isNamazWidgetSymbolsVisible(prefs);
        int mode = getLayoutMode(targetWidthPx / density, showSymbols);

        boolean isCountdownVisible = WidgetDAO.isNamazWidgetCountdownVisible(prefs);
        boolean isHeight1 = (options.getInt(OPTION_APPWIDGET_MIN_HEIGHT) < 100);

        // 3. Size Optimization (Binary Search)
        int maxFontSize = WidgetDAO.getNamazWidgetMaxFontSize(prefs);
        DigitalWidgetSizes sizes = optimizeSizes(context, targetWidthPx, targetHeightPx, maxFontSize, mode, allPrayers, isCountdownVisible, isHeight1);

        // 4. Styles Application
        configureBackground(rv, context, prefs, targetWidthPx, targetHeightPx);
        configurePadding(rv, context, prefs);

        // 5. Content Rendering
        if (!isCountdownVisible) {
            rv.setViewVisibility(R.id.countdown_container, View.GONE);
            rv.setViewVisibility(R.id.timeline_container, View.VISIBLE);
            renderTimeline(rv, context, prefs, allPrayers, activeIndex, mode, sizes.mFontSizePx);
        } else if (isHeight1) {
            rv.setViewVisibility(R.id.countdown_container, View.VISIBLE);
            rv.setViewVisibility(R.id.timeline_container, View.GONE);
            updateCountdown(rv, context, prefs, nextPrayer, sizes.mWidgetFontSizePx, sizes.mFontSizePx);
        } else {
            rv.setViewVisibility(R.id.countdown_container, View.VISIBLE);
            rv.setViewVisibility(R.id.timeline_container, View.VISIBLE);
            updateCountdown(rv, context, prefs, nextPrayer, sizes.mWidgetFontSizePx, sizes.mFontSizePx);
            renderTimeline(rv, context, prefs, allPrayers, activeIndex, mode, sizes.mFontSizePx);
        }

        // 6. Click Action
        Intent intent = new Intent(context, DeskClock.class);
        intent.putExtra(WidgetUtils.KEY_LAUNCHED_FROM_WIDGET, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.namaz_widget_root, pendingIntent);

        return rv;
    }

    private static void updateCountdown(RemoteViews rv, Context context, SharedPreferences prefs, PrayerItem next, float timeFontSize, float labelFontSize) {
        if (next == null) return;

        long diff = next.time.getTime() - System.currentTimeMillis();
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;

        String timeLeft = String.format(Locale.getDefault(), "%dh:%02dm", hours, minutes);
        String label = context.getString(R.string.namaz_countdown_format, "", next.fullName).replace(" To ", "").trim();
        if (WidgetDAO.isTextUppercaseDisplayedOnNamazWidget(prefs)) {
            timeLeft = timeLeft.toUpperCase();
            label = label.toUpperCase();
        }

        rv.setTextViewText(R.id.countdown_time, timeLeft);
        rv.setTextViewTextSize(R.id.countdown_time, COMPLEX_UNIT_PX, timeFontSize);

        rv.setTextViewText(R.id.countdown_label, "TO " + label);
        rv.setTextViewTextSize(R.id.countdown_label, COMPLEX_UNIT_PX, labelFontSize);

        // Colors
        if (!WidgetDAO.isNamazWidgetDefaultCountdownTimeColor(prefs)) {
            rv.setTextColor(R.id.countdown_time, WidgetDAO.getNamazWidgetCustomCountdownTimeColor(prefs));
        } else {
            rv.setTextColor(R.id.countdown_time, ContextCompat.getColor(context, R.color.digital_widget_time_color));
        }

        if (!WidgetDAO.isNamazWidgetDefaultCountdownLabelColor(prefs)) {
            rv.setTextColor(R.id.countdown_label, WidgetDAO.getNamazWidgetCustomCountdownLabelColor(prefs));
        } else {
            rv.setTextColor(R.id.countdown_label, ContextCompat.getColor(context, R.color.next_alarm_widget_title_color));
        }
    }

    private static void renderTimeline(RemoteViews rv, Context context, SharedPreferences prefs,
                                      List<PrayerItem> prayers, int activeIndex, int mode, float fontSize) {
        rv.removeAllViews(R.id.timeline_container);

        SimpleDateFormat timeFormat = new SimpleDateFormat(DateFormat.is24HourFormat(context) ? "HH:mm" : "h:mm a", Locale.getDefault());
        boolean uppercase = WidgetDAO.isTextUppercaseDisplayedOnNamazWidget(prefs);

        int activeColor = WidgetDAO.isNamazWidgetDefaultTimelineActiveColor(prefs)
                ? ContextCompat.getColor(context, R.color.digital_widget_time_color)
                : WidgetDAO.getNamazWidgetCustomTimelineActiveColor(prefs);

        int nextColor = WidgetDAO.isNamazWidgetDefaultTimelineNextColor(prefs)
                ? ContextCompat.getColor(context, R.color.next_alarm_widget_title_color)
                : WidgetDAO.getNamazWidgetCustomTimelineNextColor(prefs);

        int standardColor = WidgetDAO.isNamazWidgetDefaultTimelineStandardColor(prefs)
                ? ContextCompat.getColor(context, R.color.widget_text_color)
                : WidgetDAO.getNamazWidgetCustomTimelineStandardColor(prefs);

        int nextIndex = (activeIndex + 1) % prayers.size();

        for (int i = 0; i < prayers.size(); i++) {
            PrayerItem prayer = prayers.get(i);
            RemoteViews itemRv = new RemoteViews(context.getPackageName(), R.layout.namaz_widget_item);

            itemRv.setViewVisibility(R.id.mode_wide, mode == MODE_WIDE ? View.VISIBLE : View.GONE);
            itemRv.setViewVisibility(R.id.mode_narrow, mode == MODE_NARROW ? View.VISIBLE : View.GONE);
            itemRv.setViewVisibility(R.id.mode_compact_container, View.GONE);
            itemRv.setViewVisibility(R.id.mode_compact_vertical, View.GONE);

            String name = prayer.name;
            String time = timeFormat.format(prayer.time);
            if (uppercase) {
                if (name.startsWith("🔔 ")) {
                    name = "🔔 " + name.substring(2).toUpperCase();
                } else {
                    name = name.toUpperCase();
                }
                time = time.toUpperCase();
            }

            int color = standardColor;
            if (i == activeIndex) color = activeColor;
            else if (i == nextIndex) color = nextColor;

            applyItemHighlight(itemRv, prefs, i, activeIndex, nextIndex, activeColor, nextColor);

            if (mode == MODE_WIDE) {
                itemRv.setTextViewText(R.id.label_wide, name);
                itemRv.setTextViewText(R.id.time_wide, time);
                itemRv.setTextColor(R.id.label_wide, color);
                itemRv.setTextColor(R.id.time_wide, color);
                itemRv.setTextViewTextSize(R.id.label_wide, COMPLEX_UNIT_PX, fontSize);
                itemRv.setTextViewTextSize(R.id.time_wide, COMPLEX_UNIT_PX, fontSize);
            } else {
                itemRv.setTextViewText(R.id.label_narrow, name);
                itemRv.setTextViewText(R.id.time_narrow, time);
                itemRv.setTextColor(R.id.label_narrow, color);
                itemRv.setTextColor(R.id.time_narrow, color);
                itemRv.setTextViewTextSize(R.id.label_narrow, COMPLEX_UNIT_PX, fontSize * 0.8f);
                itemRv.setTextViewTextSize(R.id.time_narrow, COMPLEX_UNIT_PX, fontSize);
            }

            rv.addView(R.id.timeline_container, itemRv);
        }
    }

    private static void applyItemHighlight(RemoteViews itemRv, SharedPreferences prefs, int index,
                                          int activeIndex, int nextIndex, int activeColor, int nextColor) {
        if (!WidgetDAO.isItemHighlightDisplayedOnNamazWidget(prefs)) {
            itemRv.setViewVisibility(R.id.item_background, View.GONE);
            return;
        }

        if (index == activeIndex || index == nextIndex) {
            int highlightColor;
            if (index == activeIndex) {
                highlightColor = WidgetDAO.isNamazWidgetItemActiveHighlightDefaultColor(prefs)
                        ? ColorUtils.setAlphaComponent(activeColor, 0x4D) // ~30% alpha
                        : WidgetDAO.getNamazWidgetItemActiveHighlightColor(prefs);
            } else {
                highlightColor = WidgetDAO.isNamazWidgetItemNextHighlightDefaultColor(prefs)
                        ? ColorUtils.setAlphaComponent(nextColor, 0x26) // ~15% alpha
                        : WidgetDAO.getNamazWidgetItemNextHighlightColor(prefs);
            }

            itemRv.setViewVisibility(R.id.item_background, View.VISIBLE);
            itemRv.setInt(R.id.item_background, "setColorFilter", highlightColor);
        } else {
            itemRv.setViewVisibility(R.id.item_background, View.GONE);
        }
    }

    private static int getLayoutMode(float widthDp, boolean showSymbols) {
        if (showSymbols) return MODE_WIDE;
        if (widthDp <= 180) return MODE_NARROW;
        return MODE_WIDE;
    }

    private static void configureBackground(RemoteViews rv, Context context, SharedPreferences prefs, int widthPx, int heightPx) {
        if (!WidgetDAO.isBackgroundDisplayedOnNamazWidget(prefs) || widthPx <= 0 || heightPx <= 0) {
            rv.setImageViewResource(R.id.widget_background, 0);
            return;
        }

        int radius = (int) dpToPx(WidgetDAO.isNamazWidgetBackgroundCornerRadiusCustomizable(prefs)
            ? WidgetDAO.getNamazWidgetBackgroundCornerRadius(prefs)
            : 24, context.getResources().getDisplayMetrics());

        int color = WidgetDAO.isNamazWidgetDefaultBackgroundColor(prefs)
                ? ContextCompat.getColor(context, R.color.widget_background_color)
                : WidgetDAO.getNamazWidgetBackgroundColor(prefs);

        if (SdkUtils.isAtLeastAndroid12()) {
             if (WidgetDAO.isNamazWidgetDefaultBackgroundColor(prefs)) {
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
        int padding = (int) dpToPx(WidgetDAO.isNamazWidgetHorizontalPaddingApplied(prefs)
                ? 20
                : 0, context.getResources().getDisplayMetrics());
        rv.setViewPadding(R.id.namaz_widget_container, padding, padding, padding, padding);
    }

    private DigitalWidgetSizes optimizeSizes(Context context, int targetWidthPx, int targetHeightPx,
                                           int maxFontSize, int mode, List<PrayerItem> prayers,
                                           boolean isCountdownVisible, boolean isHeight1) {

        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams")
        View sizer = inflater.inflate(R.layout.appwidget_namaz_timeline_sizer, null);

        View countdownContainer = sizer.findViewById(R.id.countdown_container);
        if (countdownContainer != null) {
            countdownContainer.setVisibility(isCountdownVisible ? View.VISIBLE : View.GONE);
        }

        LinearLayout container = sizer.findViewById(R.id.timeline_container);
        if (container != null) {
            container.setVisibility((!isCountdownVisible || !isHeight1) ? View.VISIBLE : View.GONE);
            container.removeAllViews();
            for (int i = 0; i < prayers.size(); i++) {
                View item = inflater.inflate(R.layout.namaz_widget_item, container, false);
                container.addView(item);
            }
        }

        SharedPreferences prefs = getDefaultSharedPreferences(context);
        int padding = (int) dpToPx(WidgetDAO.isNamazWidgetHorizontalPaddingApplied(prefs)
                ? 20
                : 0, context.getResources().getDisplayMetrics());
        sizer.setPadding(padding, padding, padding, padding);

        DigitalWidgetSizes template = new DigitalWidgetSizes(targetWidthPx, targetHeightPx, (int)dpToPx(maxFontSize, context.getResources().getDisplayMetrics()));

        // Improved scaling: use a smaller ratio (1.5f) to let timeline items grow even more
        DigitalWidgetSizes low = measure(template, 10, sizer, mode, prayers, isCountdownVisible, isHeight1);
        DigitalWidgetSizes high = measure(template, template.getLargestFontSizePx(), sizer, mode, prayers, isCountdownVisible, isHeight1);

        if (!high.hasViolations()) return high;

        while (low.getWidgetFontSizePx() < high.getWidgetFontSizePx() - 1) {
            int mid = (low.getWidgetFontSizePx() + high.getWidgetFontSizePx()) / 2;
            DigitalWidgetSizes midSize = measure(template, mid, sizer, mode, prayers, isCountdownVisible, isHeight1);
            if (midSize.hasViolations()) high = midSize;
            else low = midSize;
        }
        return low;
    }

    private DigitalWidgetSizes measure(DigitalWidgetSizes template, int fontSizePx, View sizer, int mode, List<PrayerItem> prayers,
                                     boolean isCountdownVisible, boolean isHeight1) {
        DigitalWidgetSizes measured = template.newSize();
        measured.setWidgetFontSizePx(fontSizePx, 1.5f);

        View countdownContainer = sizer.findViewById(R.id.countdown_container);
        if (countdownContainer != null) {
            countdownContainer.setVisibility(isCountdownVisible ? View.VISIBLE : View.GONE);
        }

        TextView timeView = sizer.findViewById(R.id.countdown_time);
        if (timeView != null) {
            timeView.setTextSize(COMPLEX_UNIT_PX, measured.mWidgetFontSizePx);
            timeView.setText("11h:59m");
        }

        TextView labelView = sizer.findViewById(R.id.countdown_label);
        if (labelView != null) {
            labelView.setTextSize(COMPLEX_UNIT_PX, measured.mFontSizePx);
            labelView.setText("TO ASR SHAFI'I");
        }

        LinearLayout container = sizer.findViewById(R.id.timeline_container);
        if (container != null) {
            container.setVisibility((!isCountdownVisible || !isHeight1) ? View.VISIBLE : View.GONE);
            for (int i = 0; i < container.getChildCount(); i++) {
                View item = container.getChildAt(i);
                TextView tvWide = item.findViewById(R.id.label_wide);
                TextView tvNarrow = item.findViewById(R.id.label_narrow);
                TextView tvTimeWide = item.findViewById(R.id.time_wide);
                TextView tvTimeNarrow = item.findViewById(R.id.time_narrow);

                String name = prayers.get(i).name;
                if (mode == MODE_WIDE) {
                    if (tvWide != null) {
                        tvWide.setText(name);
                        tvWide.setTextSize(COMPLEX_UNIT_PX, measured.mFontSizePx);
                    }
                    if (tvTimeWide != null) {
                        tvTimeWide.setText("11:59 PM");
                        tvTimeWide.setTextSize(COMPLEX_UNIT_PX, measured.mFontSizePx);
                    }
                    View modeWide = item.findViewById(R.id.mode_wide);
                    if (modeWide != null) modeWide.setVisibility(View.VISIBLE);
                    View modeNarrow = item.findViewById(R.id.mode_narrow);
                    if (modeNarrow != null) modeNarrow.setVisibility(View.GONE);
                } else {
                    if (tvNarrow != null) {
                        tvNarrow.setText(name);
                        tvNarrow.setTextSize(COMPLEX_UNIT_PX, measured.mFontSizePx * 0.8f);
                    }
                    if (tvTimeNarrow != null) {
                        tvTimeNarrow.setText("11:59 PM");
                        tvTimeNarrow.setTextSize(COMPLEX_UNIT_PX, measured.mFontSizePx);
                    }
                    View modeWide = item.findViewById(R.id.mode_wide);
                    if (modeWide != null) modeWide.setVisibility(View.GONE);
                    View modeNarrow = item.findViewById(R.id.mode_narrow);
                    if (modeNarrow != null) modeNarrow.setVisibility(View.VISIBLE);
                }
            }
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
        boolean showBothAsr = WidgetDAO.isNamazWidgetShowBothAsrs(prefs);
        boolean showSymbols = WidgetDAO.isNamazWidgetSymbolsVisible(prefs);

        List<PrayerItem> items = new ArrayList<>();
        if (WidgetDAO.isNamazWidgetFajrVisible(prefs)) {
            String label = context.getString(R.string.fajr_label);
            items.add(new PrayerItem(showSymbols ? "🌌" : label, label, times.fajr));
        }
        if (WidgetDAO.isNamazWidgetSunriseVisible(prefs)) {
            String label = context.getString(R.string.sunrise_label);
            items.add(new PrayerItem(showSymbols ? "🌅" : label, label, times.sunrise));
        }
        if (WidgetDAO.isNamazWidgetDhuhrVisible(prefs)) {
            String label = context.getString(R.string.dhuhr_label);
            items.add(new PrayerItem(showSymbols ? "☀️" : label, label, times.dhuhr));
        }

        if (showBothAsr) {
            if (WidgetDAO.isNamazWidgetAsrShafiVisible(prefs)) {
                String label = context.getString(R.string.asr_shafii_label);
                items.add(new PrayerItem(showSymbols ? "🌤️" : label, label, times.asrShafi));
            }
            if (WidgetDAO.isNamazWidgetAsrHanafiVisible(prefs)) {
                String label = context.getString(R.string.asr_hanafi_label);
                items.add(new PrayerItem(showSymbols ? "⛅" : label, label, times.asrHanafi));
            }
        } else {
            if (WidgetDAO.isNamazWidgetAsrVisible(prefs)) {
                String label = context.getString(R.string.asr_label);
                String name = showSymbols ? (isShafi ? "🌤️" : "⛅") : label;
                items.add(new PrayerItem(name, label, isShafi ? times.asrShafi : times.asrHanafi));
            }
        }

        if (WidgetDAO.isNamazWidgetMaghribVisible(prefs)) {
            String label = context.getString(R.string.maghrib_label);
            items.add(new PrayerItem(showSymbols ? "🌇" : label, label, times.maghrib));
        }
        if (WidgetDAO.isNamazWidgetIshaVisible(prefs)) {
            String label = context.getString(R.string.isha_label);
            items.add(new PrayerItem(showSymbols ? "🌙" : label, label, times.isha));
        }

        if (WidgetDAO.isNamazWidgetShowAlarms(prefs) && rules != null) {
            Date now = new Date();
            Calendar tomorrowCal = Calendar.getInstance();
            tomorrowCal.add(Calendar.DAY_OF_YEAR, 1);
            NamazCalculator.NamazTimes tomorrowTimes = getNamazTimes(prefs, tomorrowCal.getTime());

            for (RelationalRule rule : rules) {
                if (WidgetDAO.isNamazWidgetRuleVisible(prefs, rule.id)) {
                    // Check Today
                    addAlarmIfValid(context, items, rule, times, now);
                    // Check Tomorrow (rolling 24h)
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
            // Check if this alarm occurrence is within +/- 24h of now
            long diff = ruleTime.getTime() - now.getTime();
            if (Math.abs(diff) < TimeUnit.HOURS.toMillis(24)) {
                com.best.deskclock.provider.Alarm alarm = com.best.deskclock.provider.Alarm.getAlarmByRuleId(context.getContentResolver(), rule.id);

                // For repeating alarms, always show them if they are in the rolling 24h window
                // For non-repeating, show them only if they are in the future or very recently past
                if (alarm != null && !alarm.daysOfWeek.isRepeating()) {
                    if (now.getTime() - ruleTime.getTime() > TimeUnit.MINUTES.toMillis(30)) {
                        return; // Skip non-repeating if more than 30m old
                    }
                }

                String label = (alarm != null && !alarm.label.isEmpty()) ? alarm.label : (RelationalRule.getEventName(context, rule.anchorEvent) + " Alarm");
                if (WidgetDAO.isNamazWidgetSymbolsVisible(getDefaultSharedPreferences(context))) {
                    items.add(new PrayerItem("🔔", label, ruleTime, rule.id));
                } else {
                    items.add(new PrayerItem("🔔 " + label, label, ruleTime, rule.id));
                }
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
        new NamazTimelineWidgetProvider().relayoutWidget(context, wm, widgetId, null);
    }

    private static PrayerItem getNextPrayerFromItems(Context context, List<PrayerItem> items, SharedPreferences prefs) {
        Date now = new Date();
        for (PrayerItem item : items) {
            if (item.ruleId == -1 && now.before(item.time)) return item;
        }
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        NamazCalculator.NamazTimes tomorrow = getNamazTimes(prefs, cal.getTime());
        if (tomorrow != null) {
            String label = context.getString(R.string.fajr_label);
            return new PrayerItem(label, label, tomorrow.fajr);
        }
        return null;
    }

    private static class PrayerItem {
        String name; String fullName; Date time; long ruleId = -1;
        PrayerItem(String name, String fullName, Date time) { this.name = name; this.fullName = fullName; this.time = time; }
        PrayerItem(String name, String fullName, Date time, long ruleId) { this.name = name; this.fullName = fullName; this.time = time; this.ruleId = ruleId; }
    }
}
