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
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.batoulapps.adhan.CalculationMethod;
import com.qwibbler.salarm.SalarmActivity;
import com.qwibbler.salarm.R;
import com.qwibbler.salarm.data.WidgetDAO;
import com.qwibbler.salarm.namaz.NamazCalculator;
import com.qwibbler.salarm.settings.PreferencesKeys;
import com.qwibbler.salarm.utils.SdkUtils;
import com.qwibbler.salarm.utils.WidgetUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.core.content.ContextCompat;

/**
 * A dedicated widget that displays only the countdown to the next Namaz.
 */
public class NamazCountdownWidgetProvider extends AppWidgetProvider {

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
        com.qwibbler.salarm.base.AppExecutors.getDiskIO().execute(() -> {
            Bundle finalOptions = options != null ? options : wm.getAppWidgetOptions(widgetId);
            SharedPreferences prefs = getDefaultSharedPreferences(context);

            Date now = new Date();
            NamazCalculator.NamazTimes times = getNamazTimes(prefs, now);
            PrayerItem next = null;
            if (times != null) {
                next = getNextPrayer(context, times, prefs);
            }
            PrayerItem finalNext = next;

            com.qwibbler.salarm.base.AppExecutors.getMainThread().post(() -> {
                final RemoteViews portrait = buildRemoteViews(context, finalOptions, true, finalNext);
                final RemoteViews landscape = buildRemoteViews(context, finalOptions, false, finalNext);

                final RemoteViews widget = new RemoteViews(landscape, portrait);
                wm.updateAppWidget(widgetId, widget);
            });
        });
    }

    private RemoteViews buildRemoteViews(Context context, Bundle options, boolean portrait, PrayerItem next) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        RemoteViews rv = new RemoteViews(context.getPackageName(), WidgetDAO.isTextShadowDisplayedOnNamazCountdownWidget(prefs)
                ? R.layout.appwidget_namaz_countdown_with_shadow
                : R.layout.appwidget_namaz_countdown);

        if (next == null) {
            rv.setTextViewText(R.id.countdown_time, "Set Location");
            rv.setViewVisibility(R.id.countdown_label, View.GONE);
            return rv;
        }

        // Responsive Mode Determination
        final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        final float density = displayMetrics.density;
        final int minWidthPx = (int) (density * options.getInt(OPTION_APPWIDGET_MIN_WIDTH));
        final int minHeightPx = (int) (density * options.getInt(OPTION_APPWIDGET_MIN_HEIGHT));
        final int maxWidthPx = (int) (density * options.getInt(OPTION_APPWIDGET_MAX_WIDTH));
        final int maxHeightPx = (int) (density * options.getInt(OPTION_APPWIDGET_MAX_HEIGHT));

        final int targetWidthPx = portrait ? minWidthPx : maxWidthPx;
        final int targetHeightPx = portrait ? maxHeightPx : minHeightPx;

        // Size Optimization
        int maxFontSize = WidgetDAO.getNamazCountdownWidgetMaxFontSize(prefs);
        DigitalWidgetSizes sizes = optimizeSizes(context, targetWidthPx, targetHeightPx, maxFontSize);

        // Styles Application
        configureBackground(rv, context, prefs, targetWidthPx, targetHeightPx);
        configurePadding(rv, context, prefs);

        // Content Rendering
        updateCountdown(rv, context, prefs, next, sizes.mWidgetFontSizePx, sizes.mFontSizePx);

        // Click Action
        Intent intent = new Intent(context, SalarmActivity.class);
        intent.putExtra(WidgetUtils.KEY_LAUNCHED_FROM_WIDGET, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.namaz_widget_root, pendingIntent);

        return rv;
    }

    private static void updateCountdown(RemoteViews rv, Context context, SharedPreferences prefs, PrayerItem next, float timeFontSize, float labelFontSize) {
        long diff = next.time.getTime() - System.currentTimeMillis();
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;

        String timeLeft = String.format(Locale.getDefault(), "%dh:%02dm", hours, minutes);
        String label = context.getString(R.string.namaz_countdown_format, "", next.fullName).replace(" To ", "").trim();
        if (WidgetDAO.isTextUppercaseDisplayedOnNamazCountdownWidget(prefs)) {
            timeLeft = timeLeft.toUpperCase(Locale.getDefault());
            label = label.toUpperCase(Locale.getDefault());
        }

        rv.setTextViewText(R.id.countdown_time, timeLeft);
        rv.setTextViewTextSize(R.id.countdown_time, COMPLEX_UNIT_PX, timeFontSize);

        rv.setTextViewText(R.id.countdown_label, "TO " + label);
        rv.setTextViewTextSize(R.id.countdown_label, COMPLEX_UNIT_PX, labelFontSize);

        // Colors
        if (!WidgetDAO.isNamazCountdownWidgetDefaultTimeColor(prefs)) {
            rv.setTextColor(R.id.countdown_time, WidgetDAO.getNamazCountdownWidgetCustomTimeColor(prefs));
        } else {
            rv.setTextColor(R.id.countdown_time, ContextCompat.getColor(context, R.color.digital_widget_time_color));
        }

        if (!WidgetDAO.isNamazCountdownWidgetDefaultLabelColor(prefs)) {
            rv.setTextColor(R.id.countdown_label, WidgetDAO.getNamazCountdownWidgetCustomLabelColor(prefs));
        } else {
            rv.setTextColor(R.id.countdown_label, ContextCompat.getColor(context, R.color.next_alarm_widget_title_color));
        }
    }

    private void configureBackground(RemoteViews rv, Context context, SharedPreferences prefs, int widthPx, int heightPx) {
        if (!WidgetDAO.isBackgroundDisplayedOnNamazCountdownWidget(prefs) || widthPx <= 0 || heightPx <= 0) {
            rv.setImageViewResource(R.id.widget_background, 0);
            return;
        }

        int radius = (int) dpToPx(WidgetDAO.isNamazCountdownWidgetBackgroundCornerRadiusCustomizable(prefs)
            ? WidgetDAO.getNamazCountdownWidgetBackgroundCornerRadius(prefs)
            : 24, context.getResources().getDisplayMetrics());

        int color = WidgetDAO.isNamazCountdownWidgetDefaultBackgroundColor(prefs)
                ? ContextCompat.getColor(context, R.color.widget_background_color)
                : WidgetDAO.getNamazCountdownWidgetBackgroundColor(prefs);

        if (SdkUtils.isAtLeastAndroid12()) {
             if (WidgetDAO.isNamazCountdownWidgetDefaultBackgroundColor(prefs)) {
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

    private void configurePadding(RemoteViews rv, Context context, SharedPreferences prefs) {
        int padding = (int) dpToPx(WidgetDAO.isNamazCountdownWidgetHorizontalPaddingApplied(prefs) ? 20 : 0, context.getResources().getDisplayMetrics());
        rv.setViewPadding(R.id.namaz_widget_container, padding, padding, padding, padding);
    }

    private DigitalWidgetSizes optimizeSizes(Context context, int targetWidthPx, int targetHeightPx, int maxFontSize) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        boolean shadowEnabled = WidgetDAO.isTextShadowDisplayedOnNamazCountdownWidget(prefs);
        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams")
        View sizer = inflater.inflate(shadowEnabled
                ? R.layout.appwidget_namaz_countdown_sizer_with_shadow
                : R.layout.appwidget_namaz_countdown_sizer, null);
        int padding = (int) dpToPx(WidgetDAO.isNamazCountdownWidgetHorizontalPaddingApplied(prefs) ? 20 : 0, context.getResources().getDisplayMetrics());
        sizer.setPadding(padding, padding, padding, padding);

        DigitalWidgetSizes template = new DigitalWidgetSizes(targetWidthPx, targetHeightPx, (int) dpToPx(maxFontSize, context.getResources().getDisplayMetrics()));
        DigitalWidgetSizes low = measure(template, 10, sizer);
        DigitalWidgetSizes high = measure(template, template.getLargestFontSizePx(), sizer);

        if (!high.hasViolations()) return high;

        while (low.getWidgetFontSizePx() < high.getWidgetFontSizePx() - 1) {
            int mid = (low.getWidgetFontSizePx() + high.getWidgetFontSizePx()) / 2;
            DigitalWidgetSizes midSize = measure(template, mid, sizer);
            if (midSize.hasViolations()) high = midSize;
            else low = midSize;
        }
        return low;
    }

    private DigitalWidgetSizes measure(DigitalWidgetSizes template, int fontSizePx, View sizer) {
        DigitalWidgetSizes measured = template.newSize();
        measured.setWidgetFontSizePx(fontSizePx, 1.5f);

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

        int widthSpec = View.MeasureSpec.makeMeasureSpec(template.mTargetWidthPx, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(template.mTargetHeightPx, View.MeasureSpec.UNSPECIFIED);
        sizer.measure(widthSpec, heightSpec);

        measured.mMeasuredWidthPx = sizer.getMeasuredWidth();
        measured.mMeasuredHeightPx = sizer.getMeasuredHeight();

        return measured;
    }

    private NamazCalculator.NamazTimes getNamazTimes(SharedPreferences prefs, Date date) {
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

    private PrayerItem getNextPrayer(Context context, NamazCalculator.NamazTimes times, SharedPreferences prefs) {
        long now = System.currentTimeMillis();
        String madhabStr = prefs.getString(PreferencesKeys.KEY_NAMAZ_MADHAB, "HANAFI");
        boolean isShafi = "SHAFI".equals(madhabStr);
        boolean showSymbols = WidgetDAO.isNamazCountdownWidgetSymbolsVisible(prefs);
        Date asrTime = isShafi ? times.asrShafi : times.asrHanafi;

        if (now < times.fajr.getTime()) {
            String label = context.getString(R.string.fajr_label);
            return new PrayerItem(showSymbols ? "🌌" : label, label, times.fajr);
        }
        if (now < times.sunrise.getTime()) {
            String label = context.getString(R.string.sunrise_label);
            return new PrayerItem(showSymbols ? "🌅" : label, label, times.sunrise);
        }
        if (now < times.dhuhr.getTime()) {
            String label = context.getString(R.string.dhuhr_label);
            return new PrayerItem(showSymbols ? "☀️" : label, label, times.dhuhr);
        }
        if (now < asrTime.getTime()) {
            String label = context.getString(R.string.asr_label);
            return new PrayerItem(showSymbols ? (isShafi ? "🌤️" : "⛅") : label, label, asrTime);
        }
        if (now < times.maghrib.getTime()) {
            String label = context.getString(R.string.maghrib_label);
            return new PrayerItem(showSymbols ? "🌇" : label, label, times.maghrib);
        }
        if (now < times.isha.getTime()) {
            String label = context.getString(R.string.isha_label);
            return new PrayerItem(showSymbols ? "🌙" : label, label, times.isha);
        }

        // Next day Fajr
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        NamazCalculator.NamazTimes nextDay = getNamazTimes(prefs, cal.getTime());
        if (nextDay != null) {
            String label = context.getString(R.string.fajr_label);
            return new PrayerItem(showSymbols ? "🌌" : label, label, nextDay.fajr);
        }
        return null;
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        new NamazCountdownWidgetProvider().relayoutWidget(context, appWidgetManager, appWidgetId, null);
    }

    private static class PrayerItem {
        String name; String fullName; Date time;
        PrayerItem(String name, String fullName, Date time) {
            this.name = name;
            this.fullName = fullName;
            this.time = time;
        }
    }
}
