package com.best.deskclock.namaz;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.batoulapps.adhan.CalculationMethod;
import com.best.deskclock.R;
import com.best.deskclock.alarms.AlarmStateManager;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.settings.PreferencesDefaultValues;
import com.best.deskclock.settings.PreferencesKeys;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.LogUtils;

import java.util.Calendar;
import java.util.List;

/**
 * Principal Manager for synchronizing Relational Rules (Room) with the native AOSP Alarm database.
 */
public class RelationalAlarmSyncManager {

    private static final String CHANNEL_ID = "relational_alarm_conflicts";

    /**
     * Saves or updates a RelationalRule and synchronizes it with a native AOSP Alarm.
     * Throws InvalidRuleException if the rule is physically impossible for the next valid instance.
     *
     * @param defaultRingtone The default ringtone URI to use if creating a new native alarm.
     *                        Passed as a parameter (Solution A) to avoid background thread crashes.
     */
    public static void saveRule(Context context, RelationalRule rule, boolean forceSave, Uri defaultRingtone)
            throws RelationalRule.InvalidRuleException {
        final Context appContext = context.getApplicationContext();
        NamazDatabase db = NamazDatabase.getDatabase(appContext);

        // 1. Pre-validation for immediate UI feedback
        if (!forceSave) {
            validateRuleConflict(appContext, rule);
        }

        // 2. Persist to Room
        AppExecutors.getDiskIO().execute(() -> {
            try {
                LogUtils.i("Persisting RelationalRule to Room database...");
                if (rule.id == 0) {
                    rule.id = db.relationalRuleDao().insert(rule);
                    LogUtils.i("Inserted new rule with ID: " + rule.id);
                } else {
                    db.relationalRuleDao().update(rule);
                    LogUtils.i("Updated rule with ID: " + rule.id);
                }

                // 3. Sync to AOSP SQLite
                syncRuleToNativeAlarmInternal(appContext, rule, defaultRingtone);
            } catch (Throwable t) {
                NamazErrorHandler.handleSyncError(appContext, t);
                NamazErrorHandler.reportSaveFailure(appContext);
            }
        });
    }

    private static void validateRuleConflict(Context context, RelationalRule rule) throws RelationalRule.InvalidRuleException {
        double[] coords = getCoordinates(context);
        CalculationMethod method = getCalculationMethod(context);

        Calendar now = Calendar.getInstance();
        NamazCalculator.NamazTimes times = NamazCalculator.calculateAllTimes(now.getTime(), coords[0], coords[1], method);
        RelationalRule.EvaluationResult result = RelationalRule.evaluateTimeWithConflictCheck(rule, times);

        if (result.conflict()) {
            throw new RelationalRule.InvalidRuleException(
                "Your alarm has a rule conflict. If you choose to continue, the alarm will default to "
                + AlarmUtils.getFormattedTime(context, result.fallbackTime()) + ". Please Confirm.",
                rule.anchorEvent.name(),
                result.fallbackTime()
            );
        }
    }

    public static void syncRuleToNativeAlarm(Context context, RelationalRule rule) {
        final Context appContext = context.getApplicationContext();
        AppExecutors.getDiskIO().execute(() -> {
            try {
                syncRuleToNativeAlarmInternal(appContext, rule, null);
            } catch (Throwable t) {
                NamazErrorHandler.handleSyncError(appContext, t);
            }
        });
    }

    /**
     * Synchronizes a rule to a native alarm.
     * If a new alarm must be created, it uses the provided defaultRingtone (Solution A)
     * or fetches it safely using Solution B.
     */
    private static void syncRuleToNativeAlarmInternal(Context context, RelationalRule rule, Uri defaultRingtone) {
        ContentResolver cr = context.getContentResolver();

        // Find existing alarm mapped to this rule
        List<Alarm> alarms = Alarm.getAlarms(cr, Alarm.RELATIONAL_RULE_ID + "=?", String.valueOf(rule.id));
        Alarm nativeAlarm;
        if (alarms.isEmpty()) {
            LogUtils.i("Creating new native alarm for rule: " + rule.id);

            Uri ringtoneToUse = defaultRingtone;
            if (ringtoneToUse == null) {
                // Solution B: Fallback to thread-hopping fetcher if not provided
                ringtoneToUse = AppExecutors.getOnMainThread(() ->
                    com.best.deskclock.data.DataModel.getDataModel().getAlarmRingtoneUriFromSettings());
            }

            nativeAlarm = new Alarm(0, 0, 0, 0, 0, ringtoneToUse);
            nativeAlarm.relationalRuleId = rule.id;
            nativeAlarm.enabled = rule.enabled;
            nativeAlarm.label = "Prayer: " + rule.anchorEvent.name();
            nativeAlarm.addAlarm(cr);
            LogUtils.i("Created new native alarm with ID: " + nativeAlarm.id);
        } else {
            nativeAlarm = alarms.get(0);
            nativeAlarm.enabled = rule.enabled;
            nativeAlarm.updateAlarm(cr);
            LogUtils.i("Updating existing native alarm: " + nativeAlarm.id);
        }

        if (nativeAlarm.enabled) {
            recalculateNextInstanceInternal(context, nativeAlarm, rule);
        } else {
            AlarmStateManager.deleteAllInstances(context, nativeAlarm.id);
        }
    }

    /**
     * Recalculates the next iteration for a specific native alarm tied to a relational rule.
     */
    public static void recalculateNextInstance(Context context, Alarm alarm) {
        if (alarm.relationalRuleId <= 0) return;

        final Context appContext = context.getApplicationContext();
        AppExecutors.getDiskIO().execute(() -> {
            try {
                NamazDatabase db = NamazDatabase.getDatabase(appContext);
                List<RelationalRule> rules = db.relationalRuleDao().getAll();
                RelationalRule targetRule = null;
                for (RelationalRule r : rules) {
                    if (r.id == alarm.relationalRuleId) {
                        targetRule = r;
                        break;
                    }
                }

                if (targetRule != null) {
                    recalculateNextInstanceInternal(appContext, alarm, targetRule);
                }
            } catch (Throwable t) {
                NamazErrorHandler.handleSyncError(appContext, t);
            }
        });
    }

    private static void recalculateNextInstanceInternal(Context context, Alarm alarm, RelationalRule rule) {
        ContentResolver cr = context.getContentResolver();
        double[] coords = getCoordinates(context);
        CalculationMethod method = getCalculationMethod(context);

        Calendar now = Calendar.getInstance();
        NamazCalculator.NamazTimes times = NamazCalculator.calculateAllTimes(now.getTime(), coords[0], coords[1], method);
        RelationalRule.EvaluationResult result = RelationalRule.evaluateTimeWithConflictCheck(rule, times);

        // If today passed, try tomorrow for accurate autostop calculation
        if (result.time() <= now.getTimeInMillis()) {
            Calendar tomorrow = (Calendar) now.clone();
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
            times = NamazCalculator.calculateAllTimes(tomorrow.getTime(), coords[0], coords[1], method);
            result = RelationalRule.evaluateTimeWithConflictCheck(rule, times);
        }

        Calendar nextTrigger = toCalendar(result.time());
        if (result.conflict()) {
            sendConflictNotification(context, alarm.label, result.fallbackTime());
            nextTrigger = toCalendar(result.fallbackTime());
        }

        // 1. Update parent alarm template time so it shows correctly in list
        alarm.hour = nextTrigger.get(Calendar.HOUR_OF_DAY);
        alarm.minutes = nextTrigger.get(Calendar.MINUTE);
        alarm.updateAlarm(cr);

        // 2. Update AOSP instances
        AlarmStateManager.deleteAllInstances(context, alarm.id);
        AlarmInstance instance = new AlarmInstance(nextTrigger, alarm.id);
        instance.mLabel = alarm.label;
        instance.mRingtone = alarm.alert;
        instance.mVibrate = alarm.vibrate;

        if (result.autoSilenceSeconds() > 0) {
            instance.mAutoSilenceDuration = result.autoSilenceSeconds();
        }

        instance.addInstance(cr);
        AlarmStateManager.registerInstance(context, instance, true);
    }

    public static void recalculateAllActiveRules(Context context) {
        final Context appContext = context.getApplicationContext();
        AppExecutors.getDiskIO().execute(() -> {
            try {
                ContentResolver cr = appContext.getContentResolver();
                List<Alarm> relationalAlarms = Alarm.getAlarms(cr, Alarm.RELATIONAL_RULE_ID + ">0 AND " + Alarm.ENABLED + "=1");
                for (Alarm alarm : relationalAlarms) {
                    recalculateNextInstance(appContext, alarm);
                }
            } catch (Throwable t) {
                NamazErrorHandler.handleSyncError(appContext, t);
            }
        });
    }

    private static void sendConflictNotification(Context context, String alarmName, long fallbackTime) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Alarm Rule Conflicts", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
        }

        String timeStr = AlarmUtils.getFormattedTime(context, fallbackTime);
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_tab_alarm_static)
                .setContentTitle("Alarm Rule Conflict")
                .setContentText(alarmName + " has rule conflict, defaulting to " + timeStr + ". Please update the rule.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();

        nm.notify((int) System.currentTimeMillis(), notification);
    }

    private static double[] getCoordinates(Context context) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        double lat = Double.parseDouble(prefs.getString(PreferencesKeys.KEY_NAMAZ_LATITUDE, PreferencesDefaultValues.DEFAULT_NAMAZ_LATITUDE));
        double lon = Double.parseDouble(prefs.getString(PreferencesKeys.KEY_NAMAZ_LONGITUDE, PreferencesDefaultValues.DEFAULT_NAMAZ_LONGITUDE));
        return new double[]{lat, lon};
    }

    private static CalculationMethod getCalculationMethod(Context context) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        String methodStr = prefs.getString(PreferencesKeys.KEY_NAMAZ_CALC_METHOD, PreferencesDefaultValues.DEFAULT_NAMAZ_CALC_METHOD);
        return CalculationMethod.valueOf(methodStr);
    }

    private static Calendar toCalendar(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        return cal;
    }
}
