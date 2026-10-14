package com.qwibbler.salarm.salah;

import static com.qwibbler.salarm.SalarmApplication.getDefaultSharedPreferences;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.batoulapps.adhan.CalculationMethod;
import com.qwibbler.salarm.alarms.AlarmStateManager;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.data.DataModel;
import com.qwibbler.salarm.data.SettingsDAO;
import com.qwibbler.salarm.data.Weekdays;
import com.qwibbler.salarm.provider.Alarm;
import com.qwibbler.salarm.provider.AlarmInstance;
import com.qwibbler.salarm.utils.LogUtils;
import com.qwibbler.salarm.utils.WidgetUtils;
import com.qwibbler.salarm.widgets.SalahTimelineWidgetProvider;

import java.util.Calendar;
import java.util.List;

/**
 * Principal Manager for synchronizing Relational Rules (Room) with the native AOSP Alarm database.
 */
public class SalahAlarmSyncManager {

    public interface OnRuleSyncListener {
        void onRuleSynced(Alarm alarm);
    }

    /**
     * Saves or updates a RelationalRule and synchronizes it with a native AOSP Alarm.
     *
     * @param defaultRingtone The default ringtone URI to use if creating a new native alarm.
     *                        Passed as a parameter (Solution A) to avoid background thread crashes.
     */

    public static void saveRule(Context context, RelationalRule rule, Uri defaultRingtone, OnRuleSyncListener listener) {
        final Context appContext = context.getApplicationContext();
        SalahDatabase db = SalahDatabase.getDatabase(appContext);

        // Persist to Room
        AppExecutors.getDiskIO().execute(() -> {
            try {
                if (rule.id == 0) {
                    rule.id = db.relationalRuleDao().insert(rule);
                    LogUtils.i("Inserted new rule with ID: " + rule.id);
                } else {
                    db.relationalRuleDao().update(rule);
                    LogUtils.i("Updated rule with ID: " + rule.id);
                }

                // Sync to AOSP SQLite
                Alarm syncedAlarm = syncRuleToNativeAlarmInternal(appContext, rule, defaultRingtone, null);

                if (listener != null) {
                    AppExecutors.getMainThread().post(() -> listener.onRuleSynced(syncedAlarm));
                }
            } catch (Throwable t) {
                SalahErrorHandler.handleSyncError(appContext, t);
                SalahErrorHandler.reportSaveFailure(appContext);
            }
        });
    }

    public static void syncSelectedRulesToAlarms(Context context) {
        final Context appContext = context.getApplicationContext();
        AppExecutors.getDiskIO().execute(() -> {
            try {
                SalahDatabase db = SalahDatabase.getDatabase(appContext);
                List<RelationalRule> allRules = db.relationalRuleDao().getAll();
                List<Masjid> allMasjids = db.masjidDao().getAll();

                for (RelationalRule rule : allRules) {
                    boolean shouldBeActive = rule.isSelected;

                    Masjid masjid = null;
                    if (rule.definition.anchor.masjidEventId != null) {
                        MasjidEvent event = db.masjidEventDao().getById(rule.definition.anchor.masjidEventId);
                        if (event != null) {
                            for (Masjid m : allMasjids) {
                                if (m.id == event.masjidId) {
                                    masjid = m;
                                    break;
                                }
                            }
                        }
                        if (masjid == null || !masjid.showMasjidEvents) {
                            shouldBeActive = false;
                        }
                    }

                    if (shouldBeActive) {
                        syncRuleToNativeAlarmInternal(appContext, rule, null, masjid);
                    } else {
                        disableNativeAlarmForRule(appContext, rule.id);
                    }
                }
            } catch (Throwable t) {
                LogUtils.e("Error syncing selected rules", t);
            }
        });
    }

    private static void disableNativeAlarmForRule(Context context, long ruleId) {
        ContentResolver cr = context.getContentResolver();
        List<Alarm> alarms = Alarm.getAlarms(cr, Alarm.RELATIONAL_RULE_ID + "=?", String.valueOf(ruleId));
        for (Alarm alarm : alarms) {
            if (alarm.enabled) {
                alarm.enabled = false;
                alarm.updateAlarm(cr);
                AlarmStateManager.deleteAllInstances(context, alarm.id);
            }
        }
    }

    private static Alarm syncRuleToNativeAlarmInternal(Context context, RelationalRule rule, Uri defaultRingtone, Masjid providedMasjid) {
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
                    DataModel.getDataModel().getAlarmRingtoneUriFromSettings(true));
            }

            nativeAlarm = Alarm.createDefaultAlarm(context, 0, 0, ringtoneToUse, true);
            nativeAlarm.relationalRuleId = rule.id;
            nativeAlarm.enabled = rule.enabled;
            nativeAlarm.daysOfWeek = Weekdays.fromBits(0x7F);

            String label;
            if (rule.definition.anchor.masjidEventId != null) {
                MasjidEvent event = SalahDatabase.getDatabase(context).masjidEventDao().getById(rule.definition.anchor.masjidEventId);
                Masjid masjid = providedMasjid;
                if (masjid == null && event != null) {
                    masjid = SalahDatabase.getDatabase(context).masjidDao().getById(event.masjidId);
                }
                String masjidName = masjid != null ? masjid.name : "Masjid";
                String type = event != null ? event.salahType : "Salah";
                label = SalahUtils.getLocalizedSalahName(context, type) + " - " + masjidName;
            } else if (rule.masjidSalahType != null) {
                // Tracked default alarm
                label = SalahUtils.getLocalizedSalahName(context, rule.masjidSalahType);
            } else if (rule.definition.anchor.basicEvent != null) {
                label = RelationalRule.getEventName(context, rule.definition.anchor.basicEvent);
            } else {
                label = "Salah";
            }
            nativeAlarm.label = "🔔 " + label;

            // Add auto-tag if enabled
            SharedPreferences prefs = getDefaultSharedPreferences(context);
            if (SettingsDAO.isSalahAutoTagEnabled(prefs)) {
                String tagName = SettingsDAO.getSalahAutoTagName(prefs);
                if (!nativeAlarm.tags.contains(tagName)) {
                    nativeAlarm.tags.add(tagName);
                }
            }

            nativeAlarm.addAlarm(cr);
            LogUtils.i("Created new native alarm with ID: " + nativeAlarm.id);
        } else {
            nativeAlarm = alarms.get(0);
            nativeAlarm.enabled = rule.enabled;
            nativeAlarm.updateAlarm(cr);
            LogUtils.i("Updating existing native alarm: " + nativeAlarm.id);
        }

        recalculateNextInstanceInternal(context, nativeAlarm, rule);

        return nativeAlarm;
    }

    /**
     * Recalculates the next iteration for a specific native alarm tied to a relational rule.
     */
    public static void recalculateNextInstance(Context context, Alarm alarm) {
        recalculateNextInstance(context, alarm, null);
    }

    /**
     * Recalculates the next iteration, optionally skipping a specific time (e.g. on dismissal).
     */
    public static void recalculateNextInstance(Context context, Alarm alarm, Calendar skipTime) {
        if (alarm.relationalRuleId <= 0) return;

        final Context appContext = context.getApplicationContext();
        AppExecutors.getDiskIO().execute(() -> recalculateNextInstanceSync(appContext, alarm, skipTime));
    }

    public static void recalculateNextInstanceSync(Context context, Alarm alarm) {
        recalculateNextInstanceSync(context, alarm, null);
    }

    public static void recalculateNextInstanceSync(Context context, Alarm alarm, Calendar skipTime) {
        if (alarm.relationalRuleId <= 0) return;

        try {
            SalahDatabase db = SalahDatabase.getDatabase(context);
            RelationalRule targetRule = db.relationalRuleDao().getById(alarm.relationalRuleId);

            if (targetRule != null) {
                recalculateNextInstanceInternal(context, alarm, targetRule, skipTime);
            }
        } catch (Throwable t) {
            SalahErrorHandler.handleSyncError(context, t);
        }
    }

    private static void recalculateNextInstanceInternal(Context context, Alarm alarm, RelationalRule rule) {
        recalculateNextInstanceInternal(context, alarm, rule, null);
    }

    private static void recalculateNextInstanceInternal(Context context, Alarm alarm, RelationalRule rule, Calendar skipTime) {
        if (!alarm.enabled) {
            alarm.updateAlarm(context.getContentResolver());
            if (rule.enabled) {
                rule.enabled = false;
                AppExecutors.getDiskIO().execute(() -> SalahDatabase.getDatabase(context).relationalRuleDao().update(rule));
            }
            AlarmStateManager.deleteAllInstances(context, alarm.id);
            WidgetUtils.scheduleWidgetUpdate(context, SalahTimelineWidgetProvider.class);
            return;
        }

        ContentResolver cr = context.getContentResolver();
        final SharedPreferences prefs = getDefaultSharedPreferences(context);
        final boolean wasStyledDaysEnabled = alarm.isRepeatDayStyleEnabled(prefs);

        double[] coords = null;
        CalculationMethod method = null;
        if (rule.definition != null && rule.definition.anchor != null && rule.definition.anchor.masjidEventId != null) {
            SalahDatabase db = SalahDatabase.getDatabase(context);
            MasjidEvent event = db.masjidEventDao().getById(rule.definition.anchor.masjidEventId);
            if (event != null) {
                Masjid masjid = db.masjidDao().getById(event.masjidId);
                if (masjid != null) {
                    method = masjid.getCalculationMethod(context);
                    if (masjid.latitude != 0.0 || masjid.longitude != 0.0) {
                        coords = new double[]{masjid.latitude, masjid.longitude};
                    }
                }
            }
        }
        if (coords == null) {
            coords = SalahUtils.getCoordinates(context);
        }
        if (method == null) {
            method = SalahUtils.getCalculationMethod(context);
        }

        Calendar now = Calendar.getInstance();
        // Use minute precision for all comparisons to match AOSP Alarm behavior
        long nowMin = (now.getTimeInMillis() / 60000) * 60000;

        Calendar candidateDate = (Calendar) now.clone();

        long skipMin = -1;
        if (skipTime != null) {
            skipMin = (skipTime.getTimeInMillis() / 60000) * 60000;
            // If we are skipping an occurrence, we move to at least the next day
            // because prayer events only occur once per day.
            candidateDate.setTimeInMillis(skipTime.getTimeInMillis());
            candidateDate.add(Calendar.DAY_OF_YEAR, 1);
            candidateDate.set(Calendar.HOUR_OF_DAY, 0);
            candidateDate.set(Calendar.MINUTE, 0);
            candidateDate.set(Calendar.SECOND, 0);
            candidateDate.set(Calendar.MILLISECOND, 0);

            // Ensure candidateDate isn't in the past relative to 'now'
            if (candidateDate.before(now)) {
                candidateDate.setTime(now.getTime());
            }
        }

        // If it's a one-time alarm for a specific day, start from that date unless already past it
        if (alarm.year > 0) {
            Calendar specificDate = Calendar.getInstance();
            specificDate.set(alarm.year, alarm.month, alarm.day, 0, 0, 0);
            specificDate.set(Calendar.MILLISECOND, 0);
            if (specificDate.after(candidateDate)) {
                candidateDate = specificDate;
            }
        }

        RelationalRule.EvaluationResult result = null;
        long minTime = Math.max(nowMin, skipMin);

        // Search for the next valid occurrence (up to 366 days)
        for (int i = 0; i < 366; i++) {
            if (alarm.isDatePaused(candidateDate)) {
                candidateDate.add(Calendar.DAY_OF_YEAR, 1);
                candidateDate.set(Calendar.HOUR_OF_DAY, 0);
                candidateDate.set(Calendar.MINUTE, 0);
                candidateDate.set(Calendar.SECOND, 0);
                candidateDate.set(Calendar.MILLISECOND, 0);
                continue;
            }

            SalahCalculator.SalahTimes times = SalahCalculator.calculateAllSalahTimes(context, candidateDate.getTime(), coords[0], coords[1], method);
            result = RelationalRule.evaluateTimeWithConflictCheck(context, rule, times, candidateDate.getTime());

            if (result.time() > 0) {
                long resultMin = (result.time() / 60000) * 60000;
                if (resultMin > minTime) {
                    if (alarm.daysOfWeek.isRepeating()) {
                        if (alarm.daysOfWeek.isBitOn(candidateDate.get(Calendar.DAY_OF_WEEK))) {
                            break;
                        }
                    } else {
                        // One-time alarm (either specific date or next available)
                        break;
                    }
                }
            }
            candidateDate.add(Calendar.DAY_OF_YEAR, 1);
            candidateDate.set(Calendar.HOUR_OF_DAY, 0);
            candidateDate.set(Calendar.MINUTE, 0);
            candidateDate.set(Calendar.SECOND, 0);
            candidateDate.set(Calendar.MILLISECOND, 0);
        }

        if (result == null || result.time() <= 0) {
            LogUtils.w("Could not calculate prayer time for rule " + rule.id);
            // Disable native alarm if we can't calculate a future time to prevent stuck instances
            AppExecutors.getDiskIO().execute(() -> {
                alarm.enabled = false;
                alarm.updateAlarm(context.getContentResolver());
                AlarmStateManager.deleteAllInstances(context, alarm.id);
            });
            return;
        }

        long finalResultMin = (result.time() / 60000) * 60000;
        if (finalResultMin <= minTime) {
            LogUtils.w("Could not find a future trigger time for rule " + rule.id);
            return;
        }

        Calendar nextTrigger = toCalendar(finalResultMin);

        // 1. Update parent alarm template time so it shows correctly in list
        alarm.hour = nextTrigger.get(Calendar.HOUR_OF_DAY);
        alarm.minutes = nextTrigger.get(Calendar.MINUTE);
        // Also sync the date if it's a one-time alarm
        if (!alarm.daysOfWeek.isRepeating()) {
            alarm.year = nextTrigger.get(Calendar.YEAR);
            alarm.month = nextTrigger.get(Calendar.MONTH);
            alarm.day = nextTrigger.get(Calendar.DAY_OF_MONTH);
        }

        if (rule.autostopEnabled && result.autoSilenceSeconds() > 0) {
            alarm.dismissAfter = Math.max(1, result.autoSilenceSeconds() / 60);
        } else {
            alarm.dismissAfter = SettingsDAO.getAlarmDismissAfter(prefs, true);
        }

        alarm.updateAlarm(cr);

        // 2. Update AOSP instances
        AlarmStateManager.deleteAllInstances(context, alarm.id);
        AlarmInstance instance = alarm.createInstance(nextTrigger);
        instance.mInitialAlarmTimeMillis = nextTrigger.getTimeInMillis();

        instance.addInstance(cr);
        AlarmStateManager.registerInstance(context, instance, true);

        WidgetUtils.scheduleWidgetUpdate(context, SalahTimelineWidgetProvider.class);

        if (wasStyledDaysEnabled && skipTime != null) {
            alarm.enableRepeatDayStyleIfAllDaysSelected(prefs);
        }
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
                SalahErrorHandler.handleSyncError(appContext, t);
            }
        });
    }

    public static void deleteRule(Context context, long ruleId) {
        final Context appContext = context.getApplicationContext();
        AppExecutors.getDiskIO().execute(() -> {
            try {
                LogUtils.i("Deleting RelationalRule from Room database: " + ruleId);
                SalahDatabase.getDatabase(appContext).relationalRuleDao().deleteById(ruleId);
            } catch (Throwable t) {
                LogUtils.e("Error deleting rule " + ruleId, t);
            }
        });
    }

    private static Calendar toCalendar(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        return cal;
    }
}
