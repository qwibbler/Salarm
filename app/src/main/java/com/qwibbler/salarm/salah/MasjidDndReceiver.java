package com.qwibbler.salarm.salah;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.qwibbler.salarm.utils.LogUtils;

public class MasjidDndReceiver extends BroadcastReceiver {
    public static final String ACTION_DND_ON = "com.qwibbler.salarm.ACTION_DND_ON";
    public static final String ACTION_DND_OFF = "com.qwibbler.salarm.ACTION_DND_OFF";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null || !nm.isNotificationPolicyAccessGranted()) return;

        try {
            if (ACTION_DND_ON.equals(intent.getAction())) {
                LogUtils.i("Turning on Do Not Disturb for Masjid Jamaat");
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS);
            } else if (ACTION_DND_OFF.equals(intent.getAction())) {
                LogUtils.i("Turning off Do Not Disturb for Masjid Jamaat");
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
            }
        } catch (Exception e) {
            LogUtils.e("Error changing DND interruption filter", e);
        }
    }
}
