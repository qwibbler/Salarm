// SPDX-License-Identifier: GPL-3.0-only

package com.qwibbler.salarm.widgets;

import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT;
import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH;
import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT;
import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH;
import static android.util.TypedValue.COMPLEX_UNIT_PX;
import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.qwibbler.salarm.SalarmApplication.getDefaultSharedPreferences;
import static com.qwibbler.salarm.utils.WidgetUtils.METHOD_SET_IMAGE_ICON;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.util.Locale;

import com.batoulapps.adhan.CalculationMethod;
import com.qwibbler.salarm.SalarmActivity;
import com.qwibbler.salarm.R;
import com.qwibbler.salarm.data.WidgetDAO;
import com.qwibbler.salarm.salah.SalahCalculator;
import com.qwibbler.salarm.provider.AlarmInstance;
import com.qwibbler.salarm.settings.PreferencesKeys;
import com.qwibbler.salarm.utils.SdkUtils;
import com.qwibbler.salarm.utils.WidgetUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Keep;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

/**
 * An adaptive widget that displays the Salah (prayer) timeline.
 * Features:
 * - Large countdown header.
 * - Responsive layout states (WIDE, NARROW).
 * - Full-width font scaling via measurement logic.
 * - Customizable colors and styles.
 * - Cyclical highlighting of active and next prayers.
 */
public class SalahTimelineWidgetProvider extends AppWidgetProvider {

    private static final int MODE_WIDE = 0;
    private static final int MODE_NARROW = 1;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            relayoutWidget(context, appWidgetManager, appWidgetId, null);
        }
        SalahWidgetUpdateReceiver.scheduleNextUpdate(context);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        SalahWidgetUpdateReceiver.scheduleNextUpdate(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        SalahWidgetUpdateReceiver.cancelUpdate(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        relayoutWidget(context, appWidgetManager, appWidgetId, newOptions);
    }

    private void relayoutWidget(Context context, AppWidgetManager wm, int widgetId, Bundle options) {
        com.qwibbler.salarm.base.AppExecutors.getDiskIO().execute(() -> {
            Bundle finalOptions = options != null ? options : wm.getAppWidgetOptions(widgetId);
            SharedPreferences prefs = com.qwibbler.salarm.SalarmApplication.getDefaultSharedPreferences(context);
            final List<AlarmInstance> finalInstances = WidgetDAO.isSalahWidgetShowAlarms(prefs)
                ? AlarmInstance.getInstances(context.getContentResolver(), AlarmInstance.RELATIONAL_RULE_ID + ">0")
                : null;

            // Pre-calculate SalahTimes
            Date now = new Date();
            SalahCalculator.SalahTimes salahTimes = getSalahTimes(prefs, now);
            List<PrayerItem> allPrayers = salahTimes != null ? getAllPrayers(context, salahTimes, prefs, finalInstances) : new ArrayList<>();

            com.qwibbler.salarm.base.AppExecutors.getMainThread().post(() -> {
                final RemoteViews portrait = buildRemoteViews(context, finalOptions, true, salahTimes, allPrayers);
                final RemoteViews landscape = buildRemoteViews(context, finalOptions, false, salahTimes, allPrayers);

                final RemoteViews widget = new RemoteViews(landscape, portrait);
                wm.updateAppWidget(widgetId, widget);
            });
        });
    }

    private RemoteViews buildRemoteViews(Context context, Bundle options, boolean portrait, SalahCalculator.SalahTimes salahTimes, List<PrayerItem> allPrayers) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        RemoteViews rv = new RemoteViews(context.getPackageName(), WidgetDAO.isTextShadowDisplayedOnSalahWidget(prefs)
                ? R.layout.appwidget_salah_timeline_with_shadow
                : R.layout.appwidget_salah_timeline);

        if (salahTimes == null) {
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

        boolean showSymbols = WidgetDAO.isSalahWidgetSymbolsVisible(prefs);
        float widthDp = targetWidthPx / density;
        boolean isNarrow = widthDp <= 180;
        int mode = getLayoutMode(widthDp, showSymbols);

        boolean isCountdownVisible = WidgetDAO.isSalahWidgetCountdownVisible(prefs);
        boolean isHeight1 = (options.getInt(OPTION_APPWIDGET_MIN_HEIGHT) < 100);

        // 3. Size Optimization (Binary Search)
        int maxFontSize = WidgetDAO.getSalahWidgetMaxFontSize(prefs);
        DigitalWidgetSizes sizes = optimizeSizes(context, targetWidthPx, targetHeightPx, maxFontSize, mode, allPrayers, isCountdownVisible, isHeight1, showSymbols, isNarrow);

        // 4. Styles Application
        configureBackground(rv, context, prefs, targetWidthPx, targetHeightPx);
        configurePadding(rv, context, prefs);

        // 5. Content Rendering
        if (!isCountdownVisible) {
            rv.setViewVisibility(R.id.countdown_container, View.GONE);
            rv.setViewVisibility(R.id.timeline_container, View.VISIBLE);
            renderTimeline(rv, context, prefs, allPrayers, activeIndex, mode, sizes.mFontSizePx, showSymbols, isNarrow);
        } else if (isHeight1) {
            rv.setViewVisibility(R.id.countdown_container, View.VISIBLE);
            rv.setViewVisibility(R.id.timeline_container, View.GONE);
            updateCountdown(rv, context, prefs, nextPrayer, sizes.mWidgetFontSizePx, sizes.mFontSizePx);
        } else {
            rv.setViewVisibility(R.id.countdown_container, View.VISIBLE);
            rv.setViewVisibility(R.id.timeline_container, View.VISIBLE);
            updateCountdown(rv, context, prefs, nextPrayer, sizes.mWidgetFontSizePx, sizes.mFontSizePx);
            renderTimeline(rv, context, prefs, allPrayers, activeIndex, mode, sizes.mFontSizePx, showSymbols, isNarrow);
        }

        // 6. Click Action
        Intent intent = new Intent(context, SalarmActivity.class);
        intent.putExtra(WidgetUtils.KEY_LAUNCHED_FROM_WIDGET, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.salah_widget_root, pendingIntent);

        return rv;
    }

    private static void updateCountdown(RemoteViews rv, Context context, SharedPreferences prefs, PrayerItem next, float timeFontSize, float labelFontSize) {
        if (next == null) return;

        if (SdkUtils.isAtLeastAndroid7()) {
            long base = SystemClock.elapsedRealtime() + (next.time.getTime() - System.currentTimeMillis());
            rv.setChronometer(R.id.countdown_time, base, null, true);
            rv.setChronometerCountDown(R.id.countdown_time, true);
        } else {
            long diff = next.time.getTime() - System.currentTimeMillis();
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;

            String timeLeft = String.format(Locale.getDefault(), "%dh:%02dm", hours, minutes);
            if (WidgetDAO.isTextUppercaseDisplayedOnSalahWidget(prefs)) {
                timeLeft = timeLeft.toUpperCase(Locale.getDefault());
            }
            rv.setTextViewText(R.id.countdown_time, timeLeft);
        }

        rv.setTextViewTextSize(R.id.countdown_time, COMPLEX_UNIT_PX, timeFontSize);

        String label = context.getString(R.string.salah_countdown_format, "", next.fullName).replace(" To ", "").trim();
        if (WidgetDAO.isTextUppercaseDisplayedOnSalahWidget(prefs)) {
            label = label.toUpperCase(Locale.getDefault());
        }

        rv.setTextViewText(R.id.countdown_label, "TO " + label);
        rv.setTextViewTextSize(R.id.countdown_label, COMPLEX_UNIT_PX, labelFontSize);

        // Colors
        if (!WidgetDAO.isSalahWidgetDefaultCountdownTimeColor(prefs)) {
            rv.setTextColor(R.id.countdown_time, WidgetDAO.getSalahWidgetCustomCountdownTimeColor(prefs));
        } else {
            rv.setTextColor(R.id.countdown_time, ContextCompat.getColor(context, R.color.digital_widget_time_color));
        }

        if (!WidgetDAO.isSalahWidgetDefaultCountdownLabelColor(prefs)) {
            rv.setTextColor(R.id.countdown_label, WidgetDAO.getSalahWidgetCustomCountdownLabelColor(prefs));
        } else {
            rv.setTextColor(R.id.countdown_label, ContextCompat.getColor(context, R.color.next_alarm_widget_title_color));
        }
    }

    private static void renderTimeline(RemoteViews rv, Context context, SharedPreferences prefs,
                                      List<PrayerItem> prayers, int activeIndex, int mode, float fontSize,
                                      boolean showSymbols, boolean isNarrow) {
        rv.removeAllViews(R.id.timeline_container);

        boolean shadowEnabled = WidgetDAO.isTextShadowDisplayedOnSalahWidget(prefs);
        SimpleDateFormat timeFormat = new SimpleDateFormat(DateFormat.is24HourFormat(context) ? "HH:mm" : "h:mm a", Locale.getDefault());
        boolean uppercase = WidgetDAO.isTextUppercaseDisplayedOnSalahWidget(prefs);

        int activeColor = WidgetDAO.isSalahWidgetDefaultTimelineActiveColor(prefs)
                ? ContextCompat.getColor(context, R.color.digital_widget_time_color)
                : WidgetDAO.getSalahWidgetCustomTimelineActiveColor(prefs);

        int nextColor = WidgetDAO.isSalahWidgetDefaultTimelineNextColor(prefs)
                ? ContextCompat.getColor(context, R.color.next_alarm_widget_title_color)
                : WidgetDAO.getSalahWidgetCustomTimelineNextColor(prefs);

        int standardColor = WidgetDAO.isSalahWidgetDefaultTimelineStandardColor(prefs)
                ? ContextCompat.getColor(context, R.color.widget_text_color)
                : WidgetDAO.getSalahWidgetCustomTimelineStandardColor(prefs);

        int nextIndex = (activeIndex + 1) % prayers.size();

        float effectiveFontSize = fontSize;
        if (showSymbols && isNarrow) {
            effectiveFontSize *= 0.9f;
        }

        for (int i = 0; i < prayers.size(); i++) {
            PrayerItem prayer = prayers.get(i);
            RemoteViews itemRv = new RemoteViews(context.getPackageName(), shadowEnabled
                    ? R.layout.salah_widget_item_with_shadow
                    : R.layout.salah_widget_item);

            itemRv.setViewVisibility(R.id.mode_wide, mode == MODE_WIDE ? View.VISIBLE : View.GONE);
            itemRv.setViewVisibility(R.id.mode_narrow, mode == MODE_NARROW ? View.VISIBLE : View.GONE);
            itemRv.setViewVisibility(R.id.mode_compact_container, View.GONE);
            itemRv.setViewVisibility(R.id.mode_compact_vertical, View.GONE);

            String name = prayer.name;
            String time = timeFormat.format(prayer.time);
            if (uppercase) {
                name = name.toUpperCase(Locale.getDefault());
                time = time.toUpperCase(Locale.getDefault());
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
                itemRv.setTextViewTextSize(R.id.label_wide, COMPLEX_UNIT_PX, effectiveFontSize);
                itemRv.setTextViewTextSize(R.id.time_wide, COMPLEX_UNIT_PX, effectiveFontSize);
            } else {
                itemRv.setTextViewText(R.id.label_narrow, name);
                itemRv.setTextViewText(R.id.time_narrow, time);
                itemRv.setTextColor(R.id.label_narrow, color);
                itemRv.setTextColor(R.id.time_narrow, color);
                itemRv.setTextViewTextSize(R.id.label_narrow, COMPLEX_UNIT_PX, effectiveFontSize * 0.8f);
                itemRv.setTextViewTextSize(R.id.time_narrow, COMPLEX_UNIT_PX, effectiveFontSize);
            }

            rv.addView(R.id.timeline_container, itemRv);
        }
    }

    private static void applyItemHighlight(RemoteViews itemRv, SharedPreferences prefs, int index,
                                          int activeIndex, int nextIndex, int activeColor, int nextColor) {
        if (!WidgetDAO.isItemHighlightDisplayedOnSalahWidget(prefs)) {
            itemRv.setViewVisibility(R.id.item_background, View.GONE);
            return;
        }

        if (index == activeIndex || index == nextIndex) {
            int highlightColor;
            if (index == activeIndex) {
                highlightColor = WidgetDAO.isSalahWidgetItemActiveHighlightDefaultColor(prefs)
                        ? ColorUtils.setAlphaComponent(activeColor, 0x4D) // ~30% alpha
                        : WidgetDAO.getSalahWidgetItemActiveHighlightColor(prefs);
            } else {
                highlightColor = WidgetDAO.isSalahWidgetItemNextHighlightDefaultColor(prefs)
                        ? ColorUtils.setAlphaComponent(nextColor, 0x26) // ~15% alpha
                        : WidgetDAO.getSalahWidgetItemNextHighlightColor(prefs);
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
        if (!WidgetDAO.isBackgroundDisplayedOnSalahWidget(prefs) || widthPx <= 0 || heightPx <= 0) {
            rv.setImageViewResource(R.id.widget_background, 0);
            return;
        }

        int radius = (int) dpToPx(WidgetDAO.isSalahWidgetBackgroundCornerRadiusCustomizable(prefs)
            ? WidgetDAO.getSalahWidgetBackgroundCornerRadius(prefs)
            : 24, context.getResources().getDisplayMetrics());

        int color = WidgetDAO.isSalahWidgetDefaultBackgroundColor(prefs)
                ? ContextCompat.getColor(context, R.color.widget_background_color)
                : WidgetDAO.getSalahWidgetBackgroundColor(prefs);

        if (SdkUtils.isAtLeastAndroid12()) {
             if (WidgetDAO.isSalahWidgetDefaultBackgroundColor(prefs)) {
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
        int padding = (int) dpToPx(WidgetDAO.isSalahWidgetHorizontalPaddingApplied(prefs)
                ? 20
                : 0, context.getResources().getDisplayMetrics());
        rv.setViewPadding(R.id.salah_widget_container, padding, padding, padding, padding);
    }

    private DigitalWidgetSizes optimizeSizes(Context context, int targetWidthPx, int targetHeightPx,
                                           int maxFontSize, int mode, List<PrayerItem> prayers,
                                           boolean isCountdownVisible, boolean isHeight1,
                                           boolean showSymbols, boolean isNarrow) {

        SharedPreferences prefs = getDefaultSharedPreferences(context);
        boolean shadowEnabled = WidgetDAO.isTextShadowDisplayedOnSalahWidget(prefs);
        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams")
        View sizer = inflater.inflate(shadowEnabled
                ? R.layout.appwidget_salah_timeline_sizer_with_shadow
                : R.layout.appwidget_salah_timeline_sizer, null);

        View countdownContainer = sizer.findViewById(R.id.countdown_container);
        if (countdownContainer != null) {
            countdownContainer.setVisibility(isCountdownVisible ? View.VISIBLE : View.GONE);
        }

        LinearLayout container = sizer.findViewById(R.id.timeline_container);
        if (container != null) {
            container.setVisibility((!isCountdownVisible || !isHeight1) ? View.VISIBLE : View.GONE);
            container.removeAllViews();
            for (int i = 0; i < prayers.size(); i++) {
                View item = inflater.inflate(shadowEnabled
                        ? R.layout.salah_widget_item_with_shadow
                        : R.layout.salah_widget_item, container, false);
                container.addView(item);
            }
        }

        int padding = (int) dpToPx(WidgetDAO.isSalahWidgetHorizontalPaddingApplied(prefs)
                ? 20
                : 0, context.getResources().getDisplayMetrics());
        sizer.setPadding(padding, padding, padding, padding);

        DigitalWidgetSizes template = new DigitalWidgetSizes(targetWidthPx, targetHeightPx, (int)dpToPx(maxFontSize, context.getResources().getDisplayMetrics()));

        // Improved scaling: use a smaller ratio (1.5f) to let timeline items grow even more
        DigitalWidgetSizes low = measure(template, 10, sizer, mode, prayers, isCountdownVisible, isHeight1, showSymbols, isNarrow);
        DigitalWidgetSizes high = measure(template, template.getLargestFontSizePx(), sizer, mode, prayers, isCountdownVisible, isHeight1, showSymbols, isNarrow);

        if (!high.hasViolations()) return high;

        while (low.getWidgetFontSizePx() < high.getWidgetFontSizePx() - 1) {
            int mid = (low.getWidgetFontSizePx() + high.getWidgetFontSizePx()) / 2;
            DigitalWidgetSizes midSize = measure(template, mid, sizer, mode, prayers, isCountdownVisible, isHeight1, showSymbols, isNarrow);
            if (midSize.hasViolations()) high = midSize;
            else low = midSize;
        }
        return low;
    }

    private DigitalWidgetSizes measure(DigitalWidgetSizes template, int fontSizePx, View sizer, int mode, List<PrayerItem> prayers,
                                     boolean isCountdownVisible, boolean isHeight1, boolean showSymbols, boolean isNarrow) {
        DigitalWidgetSizes measured = template.newSize();
        measured.setWidgetFontSizePx(fontSizePx, 1.5f);

        float effectiveFontSize = measured.mFontSizePx;
        if (showSymbols && isNarrow && isCountdownVisible) {
            effectiveFontSize *= 0.9f;
        }

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
            labelView.setTextSize(COMPLEX_UNIT_PX, effectiveFontSize);
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
                        tvWide.setTextSize(COMPLEX_UNIT_PX, effectiveFontSize);
                    }
                    if (tvTimeWide != null) {
                        tvTimeWide.setText("11:59 PM");
                        tvTimeWide.setTextSize(COMPLEX_UNIT_PX, effectiveFontSize);
                    }
                    View modeWide = item.findViewById(R.id.mode_wide);
                    if (modeWide != null) modeWide.setVisibility(View.VISIBLE);
                    View modeNarrow = item.findViewById(R.id.mode_narrow);
                    if (modeNarrow != null) modeNarrow.setVisibility(View.GONE);
                } else {
                    if (tvNarrow != null) {
                        tvNarrow.setText(name);
                        tvNarrow.setTextSize(COMPLEX_UNIT_PX, effectiveFontSize * 0.8f);
                    }
                    if (tvTimeNarrow != null) {
                        tvTimeNarrow.setText("11:59 PM");
                        tvTimeNarrow.setTextSize(COMPLEX_UNIT_PX, effectiveFontSize);
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

    private static SalahCalculator.SalahTimes getSalahTimes(SharedPreferences prefs, Date date) {
        String latStr = prefs.getString(PreferencesKeys.KEY_SALAH_LATITUDE, null);
        String lonStr = prefs.getString(PreferencesKeys.KEY_SALAH_LONGITUDE, null);
        if (latStr == null || lonStr == null) return null;
        try {
            double lat = Double.parseDouble(latStr);
            double lon = Double.parseDouble(lonStr);
            String methodStr = prefs.getString(PreferencesKeys.KEY_SALAH_CALC_METHOD, CalculationMethod.MUSLIM_WORLD_LEAGUE.name());
            return SalahCalculator.calculateAllSalahTimes(date, lat, lon, CalculationMethod.valueOf(methodStr));
        } catch (Exception e) { return null; }
    }

    private static List<PrayerItem> getAllPrayers(Context context, SalahCalculator.SalahTimes times, SharedPreferences prefs, List<AlarmInstance> instances) {
        String madhabStr = prefs.getString(PreferencesKeys.KEY_SALAH_MADHAB, "HANAFI");
        boolean isShafi = "SHAFI".equals(madhabStr);
        boolean showBothAsr = WidgetDAO.isSalahWidgetShowBothAsrs(prefs);
        boolean showSymbols = WidgetDAO.isSalahWidgetSymbolsVisible(prefs);

        List<PrayerItem> items = new ArrayList<>();
        if (WidgetDAO.isSalahWidgetFajrVisible(prefs)) {
            String label = context.getString(R.string.fajr_label);
            items.add(new PrayerItem(showSymbols ? "🌌" : label, label, times.fajr));
        }
        if (WidgetDAO.isSalahWidgetSunriseVisible(prefs)) {
            String label = context.getString(R.string.sunrise_label);
            items.add(new PrayerItem(showSymbols ? "🌅" : label, label, times.sunrise));
        }
        if (WidgetDAO.isSalahWidgetDhuhrVisible(prefs)) {
            String label = context.getString(R.string.dhuhr_label);
            items.add(new PrayerItem(showSymbols ? "☀️" : label, label, times.dhuhr));
        }

        if (showBothAsr) {
            if (WidgetDAO.isSalahWidgetAsrShafiVisible(prefs)) {
                String label = context.getString(R.string.asr_shafii_label);
                items.add(new PrayerItem(showSymbols ? "🌤️" : label, label, times.asrShafi));
            }
            if (WidgetDAO.isSalahWidgetAsrHanafiVisible(prefs)) {
                String label = context.getString(R.string.asr_hanafi_label);
                items.add(new PrayerItem(showSymbols ? "⛅" : label, label, times.asrHanafi));
            }
        } else {
            if (WidgetDAO.isSalahWidgetAsrVisible(prefs)) {
                String label = context.getString(R.string.asr_label);
                String name = showSymbols ? (isShafi ? "🌤️" : "⛅") : label;
                items.add(new PrayerItem(name, label, isShafi ? times.asrShafi : times.asrHanafi));
            }
        }

        if (WidgetDAO.isSalahWidgetMaghribVisible(prefs)) {
            String label = context.getString(R.string.maghrib_label);
            items.add(new PrayerItem(showSymbols ? "🌇" : label, label, times.maghrib));
        }
        if (WidgetDAO.isSalahWidgetIshaVisible(prefs)) {
            String label = context.getString(R.string.isha_label);
            items.add(new PrayerItem(showSymbols ? "🌙" : label, label, times.isha));
        }

        if (WidgetDAO.isSalahWidgetShowAlarms(prefs) && instances != null) {
            Date now = new Date();
            for (AlarmInstance instance : instances) {
                if (WidgetDAO.isSalahWidgetRuleVisible(prefs, instance.mRelationalRuleId)) {
                    long timeMillis = instance.getAlarmTime().getTimeInMillis();
                    long diff = timeMillis - now.getTime();

                    // Show if within the NEXT 24h (with 1m grace)
                    if (diff > -TimeUnit.MINUTES.toMillis(1) && diff < TimeUnit.HOURS.toMillis(24)) {
                        String label = instance.mLabel;
                        Date ruleTime = new Date(timeMillis);
                        if (WidgetDAO.isSalahWidgetSymbolsVisible(getDefaultSharedPreferences(context))) {
                            items.add(new PrayerItem("🔔", label, ruleTime, instance.mRelationalRuleId));
                        } else {
                            items.add(new PrayerItem(label, label, ruleTime, instance.mRelationalRuleId));
                        }
                    }
                }
            }
            Collections.sort(items, (o1, o2) -> o1.time.compareTo(o2.time));
        }

        return items;
    }

    private static int getActivePrayerIndex(List<PrayerItem> items) {
        Date now = new Date();
        for (int i = items.size() - 1; i >= 0; i--) { if (now.after(items.get(i).time)) return i; }
        return items.size() - 1;
    }

    @Keep
    public static void updateAppWidget(Context context, AppWidgetManager wm, int widgetId) {
        new SalahTimelineWidgetProvider().relayoutWidget(context, wm, widgetId, null);
    }

    private static PrayerItem getNextPrayerFromItems(Context context, List<PrayerItem> items, SharedPreferences prefs) {
        Date now = new Date();
        for (PrayerItem item : items) {
            if (item.ruleId == -1 && now.before(item.time)) return item;
        }
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        SalahCalculator.SalahTimes tomorrowSalahTimes = getSalahTimes(prefs, cal.getTime());
        if (tomorrowSalahTimes != null) {
            String label = context.getString(R.string.fajr_label);
            return new PrayerItem(label, label, tomorrowSalahTimes.fajr);
        }
        return null;
    }

    private static class PrayerItem {
        String name; String fullName; Date time; long ruleId = -1;
        PrayerItem(String name, String fullName, Date time) { this.name = name; this.fullName = fullName; this.time = time; }
        PrayerItem(String name, String fullName, Date time, long ruleId) { this.name = name; this.fullName = fullName; this.time = time; this.ruleId = ruleId; }
    }
}
