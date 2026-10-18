package com.qwibbler.salarm.salah;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteTableLockedException;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.SystemClock;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.settings.SettingsActivity;
import com.qwibbler.salarm.utils.LogUtils;

/**
 * Centralized utility for handling and reporting Salah-related errors to the user.
 */
public class SalahErrorHandler {

    private static final String SALAH_SYNC_ERROR_CHANNEL_ID = "salah_sync_errors";
    private static final int ERROR_NOTIFICATION_ID = 1001;

    /**
     * Handles background synchronization errors.
     * Decisions on whether to show a notification or just log are made here.
     */
    public static void handleSyncError(Context context, Throwable t) {
        LogUtils.e("Salah Sync Error encountered: ", t);

        final Context appContext = context.getApplicationContext();

        // Critically important errors (DB corruption, invalid location data)
        if (isCriticalError(t)) {
            sendErrorNotification(appContext,
                appContext.getString(R.string.salarm_error_background_sync),
                appContext.getString(R.string.salarm_error_background_sync_details));
        }
    }

    /**
     * Reports a save failure to the user via a Toast on the main thread.
     */
    public static void reportSaveFailure(Context context) {
        AppExecutors.getMainThread().post(() ->
            Toast.makeText(context, R.string.salah_error_saving, Toast.LENGTH_LONG).show());
    }

    private static boolean isCriticalError(Throwable t) {
        // Database errors are generally critical, but we suppress transient lock errors
        // during the boot grace period to avoid annoying the user on slow devices.
        if (t instanceof SQLiteException) {
            boolean isTransient = t instanceof SQLiteDatabaseLockedException ||
                                  t instanceof SQLiteTableLockedException ||
                                  t instanceof SQLiteCantOpenDatabaseException;

            return SystemClock.elapsedRealtime() >= 120_000 || !isTransient;
        }

        return t instanceof NullPointerException;
    }

    private static void sendErrorNotification(Context context, String title, String message) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(SALAH_SYNC_ERROR_CHANNEL_ID,
                "Salah Synchronization Issues", NotificationManager.IMPORTANCE_DEFAULT);
            nm.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, SettingsActivity.class);
        intent.putExtra(SettingsActivity.KEY_SHOW_SALAH_SETTINGS, true);
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(context, SALAH_SYNC_ERROR_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_tab_alarm_static)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build();

        nm.notify(ERROR_NOTIFICATION_ID, notification);
    }
}
