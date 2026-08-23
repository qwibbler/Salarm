// SPDX-License-Identifier: GPL-3.0-only

package com.qwibbler.salarm.widgets;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.qwibbler.salarm.salah.SalahUtils;
import com.qwibbler.salarm.utils.SdkUtils;

import java.util.Calendar;

/**
 * Receiver that handles minute-by-minute updates for Salah widgets.
 * This is necessary to keep the countdown timer accurate.
 */
public class SalahWidgetUpdateReceiver extends BroadcastReceiver {

    public static final String ACTION_MINUTE_UPDATE = "com.qwibbler.salarm.SALAH_MINUTE_UPDATE";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_USER_PRESENT.equals(action) ||
            Intent.ACTION_TIME_CHANGED.equals(action) ||
            Intent.ACTION_TIMEZONE_CHANGED.equals(action) ||
            Intent.ACTION_DATE_CHANGED.equals(action)) {

            updateWidgets(context);
            scheduleNextUpdate(context);
            return;
        }

        updateWidgets(context);

        AppWidgetManager wm = AppWidgetManager.getInstance(context);
        int[] timelineIds = wm.getAppWidgetIds(new ComponentName(context, SalahTimelineWidgetProvider.class));
        int[] countdownIds = wm.getAppWidgetIds(new ComponentName(context, SalahCountdownWidgetProvider.class));

        // Reschedule for the next update if any Salah widget is still active
        if (timelineIds.length > 0 || countdownIds.length > 0) {
            scheduleNextUpdate(context);
        }
    }

    private void updateWidgets(Context context) {
        AppWidgetManager wm = AppWidgetManager.getInstance(context);

        // Update Timeline Widgets
        int[] timelineIds = wm.getAppWidgetIds(new ComponentName(context, SalahTimelineWidgetProvider.class));
        for (int id : timelineIds) {
            SalahTimelineWidgetProvider.updateAppWidget(context, wm, id);
        }

        // Update Countdown Widgets
        int[] countdownIds = wm.getAppWidgetIds(new ComponentName(context, SalahCountdownWidgetProvider.class));
        for (int id : countdownIds) {
            SalahCountdownWidgetProvider.updateAppWidget(context, wm, id);
        }
    }

    public static void scheduleNextUpdate(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, SalahWidgetUpdateReceiver.class);
        intent.setAction(ACTION_MINUTE_UPDATE);

        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long triggerAtMillis;
        if (SdkUtils.isAtLeastAndroid7()) {
            long nextPrayer = SalahUtils.getNextPrayerTime(context);
            if (nextPrayer > 0) {
                // Schedule for the next prayer time + 1 second buffer to ensure calculations flip
                triggerAtMillis = nextPrayer + 1000;
            } else {
                triggerAtMillis = getNextMinuteMillis();
            }
        } else {
            triggerAtMillis = getNextMinuteMillis();
        }

        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
    }

    private static long getNextMinuteMillis() {
        // Schedule at the start of the next minute
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static void cancelUpdate(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SalahWidgetUpdateReceiver.class);
        intent.setAction(ACTION_MINUTE_UPDATE);

        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) {
            am.cancel(pi);
            pi.cancel();
        }
    }
}
