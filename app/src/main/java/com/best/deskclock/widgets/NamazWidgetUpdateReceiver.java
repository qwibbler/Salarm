// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.widgets;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

/**
 * Receiver that handles minute-by-minute updates for Namaz widgets.
 * This is necessary to keep the countdown timer accurate.
 */
public class NamazWidgetUpdateReceiver extends BroadcastReceiver {

    public static final String ACTION_MINUTE_UPDATE = "com.best.deskclock.NAMAZ_MINUTE_UPDATE";

    @Override
    public void onReceive(Context context, Intent intent) {
        AppWidgetManager wm = AppWidgetManager.getInstance(context);

        // Update Timeline Widgets
        int[] timelineIds = wm.getAppWidgetIds(new ComponentName(context, NamazTimelineWidgetProvider.class));
        if (timelineIds.length > 0) {
            Intent updateIntent = new Intent(context, NamazTimelineWidgetProvider.class);
            updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, timelineIds);
            context.sendBroadcast(updateIntent);
        }

        // Reschedule for the next minute if any Namaz widget is still active
        if (timelineIds.length > 0) {
            scheduleNextUpdate(context);
        }
    }

    public static void scheduleNextUpdate(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NamazWidgetUpdateReceiver.class);
        intent.setAction(ACTION_MINUTE_UPDATE);

        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Schedule at the start of the next minute
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pi);
    }

    public static void cancelUpdate(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NamazWidgetUpdateReceiver.class);
        intent.setAction(ACTION_MINUTE_UPDATE);

        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) {
            am.cancel(pi);
            pi.cancel();
        }
    }
}
