package com.best.deskclock.namaz;

import android.content.ContentResolver;
import android.content.Context;

import com.batoulapps.adhan.CalculationMethod;
import com.best.deskclock.alarms.AlarmStateManager;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.utils.LogUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class RelationalAlarmScheduler {

    private static final String LABEL_PREFIX = "NAMAZ_RULE_";

    public static void syncRelationalAlarms(Context context) {
        AppExecutors.getDiskIO().execute(() -> {
            try {
                syncRelationalAlarmsInternal(context);
            } catch (Exception e) {
                LogUtils.e("Error syncing relational Namaz alarms", e);
            }
        });
    }

    private static void syncRelationalAlarmsInternal(Context context) {
        LogUtils.i("Syncing relational Namaz alarms...");
        NamazDatabase db = NamazDatabase.getDatabase(context);
        List<RelationalRule> rules = db.relationalRuleDao().getEnabledRules();

        double[] coords = NamazUtils.getCoordinates(context);
        double lat = coords[0];
        double lon = coords[1];
        CalculationMethod method = NamazUtils.getCalculationMethod(context);

        ContentResolver cr = context.getContentResolver();

        for (RelationalRule rule : rules) {
            String ruleLabel = LABEL_PREFIX + rule.id;
            Alarm nativeAlarm = getNativeAlarmByLabel(cr, ruleLabel);

            if (nativeAlarm == null) {
                nativeAlarm = new Alarm();
                nativeAlarm.enabled = true;
                nativeAlarm.label = ruleLabel;
                // Inherit some defaults or set specific ones for namaz
                nativeAlarm.addAlarm(cr);
            }

            Calendar triggerTime = calculateNextTriggerTime(rule, lat, lon, method);

            AlarmInstance instance = AlarmInstance.getNextUpcomingInstanceByAlarmId(cr, nativeAlarm.id);
            if (instance == null) {
                instance = new AlarmInstance(triggerTime, nativeAlarm.id);
                instance.mLabel = nativeAlarm.label;
                instance.mRingtone = nativeAlarm.alert;
                instance.mVibrate = nativeAlarm.vibrate;
                instance.addInstance(cr);
                AlarmStateManager.registerInstance(context, instance, true);
            } else {
                Calendar currentAlarmTime = instance.getAlarmTime();
                if (!currentAlarmTime.equals(triggerTime)) {
                    LogUtils.i("Updating instance time for rule " + rule.id + " to " + triggerTime.getTime());
                    instance.setAlarmTime(triggerTime);
                    instance.updateInstance(cr);
                    AlarmStateManager.registerInstance(context, instance, true);
                }
            }
        }

        // Cleanup: remove native alarms for disabled or deleted rules
        List<Alarm> allNativeAlarms = Alarm.getAlarms(cr, null);
        for (Alarm alarm : allNativeAlarms) {
            if (alarm.label != null && alarm.label.startsWith(LABEL_PREFIX)) {
                try {
                    long ruleId = Long.parseLong(alarm.label.substring(LABEL_PREFIX.length()));
                    boolean found = false;
                    for (RelationalRule r : rules) {
                        if (r.id == ruleId) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        LogUtils.i("Cleaning up native alarm for disabled/deleted rule: " + ruleId);
                        AlarmStateManager.deleteAllInstances(context, alarm.id);
                        Alarm.deleteAlarm(cr, alarm.id);
                    }
                } catch (NumberFormatException e) {
                    LogUtils.e("Error parsing rule id from label: " + alarm.label);
                }
            }
        }
    }

    private static Calendar calculateNextTriggerTime(RelationalRule rule, double lat, double lon, CalculationMethod method) {
        Calendar now = Calendar.getInstance();

        // Try today
        NamazCalculator.NamazTimes timesToday = NamazCalculator.calculateAllTimes(now.getTime(), lat, lon, method);
        Date dateToday = RelationalRule.evaluateTime(rule, timesToday, now.getTime());

        if (dateToday.after(now.getTime())) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(dateToday);
            return cal;
        }

        // Try tomorrow
        Calendar tomorrow = (Calendar) now.clone();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        NamazCalculator.NamazTimes timesTomorrow = NamazCalculator.calculateAllTimes(tomorrow.getTime(), lat, lon, method);
        Date dateTomorrow = RelationalRule.evaluateTime(rule, timesTomorrow, tomorrow.getTime());

        Calendar cal = Calendar.getInstance();
        cal.setTime(dateTomorrow);
        return cal;
    }

    private static Alarm getNativeAlarmByLabel(ContentResolver cr, String label) {
        List<Alarm> alarms = Alarm.getAlarms(cr, Alarm.LABEL + "=?", label);
        if (!alarms.isEmpty()) {
            return alarms.get(0);
        }
        return null;
    }
}
