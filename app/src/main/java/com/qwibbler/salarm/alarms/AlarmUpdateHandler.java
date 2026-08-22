/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.qwibbler.salarm.alarms;

import android.content.ContentResolver;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.view.HapticFeedbackConstantsCompat;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.events.Events;
import com.qwibbler.salarm.namaz.NamazDatabase;
import com.qwibbler.salarm.namaz.RelationalAlarmSyncManager;
import com.qwibbler.salarm.namaz.RelationalRule;
import com.qwibbler.salarm.provider.Alarm;
import com.qwibbler.salarm.provider.AlarmInstance;
import com.qwibbler.salarm.uicomponents.toast.SnackbarManager;
import com.qwibbler.salarm.utils.AlarmUtils;
import com.qwibbler.salarm.utils.FileUtils;
import com.qwibbler.salarm.utils.LogUtils;
import com.qwibbler.salarm.utils.Utils;
import com.qwibbler.salarm.utils.WidgetUtils;
import com.google.android.material.snackbar.Snackbar;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * API for asynchronously mutating a single alarm.
 */
public final class AlarmUpdateHandler {

    private final Context mAppContext;
    private final ScrollHandler mScrollHandler;
    private final View mSnackbarAnchor;

    // For undo
    private Alarm mDeletedAlarm;
    private RelationalRule mDeletedRule;

    private String mSyncToastLabel = null;

    public AlarmUpdateHandler(Context context, ScrollHandler scrollHandler, ViewGroup snackbarAnchor) {
        mAppContext = context.getApplicationContext();
        mScrollHandler = scrollHandler;
        mSnackbarAnchor = snackbarAnchor;
    }

    /**
     * Adds a new alarm on the background.
     *
     * @param alarm The alarm to be added.
     */
    public void asyncAddAlarm(final Alarm alarm) {
        asyncAddAlarm(alarm, true, true);
    }

    /**
     * Adds a new alarm on the background.
     *
     * @param alarm          The alarm to be added.
     * @param showSnackbar   Whether to show a snackbar after saving.
     * @param openEditScreen Whether to open the edit screen for the new alarm.
     */
    public void asyncAddAlarm(final Alarm alarm, final boolean showSnackbar, final boolean openEditScreen) {
        AppExecutors.getDiskIO().execute(() -> {
            AlarmInstance instance = null;
            Alarm newAlarm = null;

            if (alarm != null) {
                Events.sendAlarmEvent(R.string.action_create, R.string.label_salarm);
                ContentResolver cr = mAppContext.getContentResolver();

                // Add alarm to db
                newAlarm = alarm.addAlarm(cr);

                // Create and add instance to db
                if (newAlarm.enabled) {
                    instance = setupAlarmInstance(newAlarm);
                }
            }

            final AlarmInstance finalInstance = instance;
            final Alarm finalNewAlarm = newAlarm;

            AppExecutors.getMainThread().post(() -> {
                if (showSnackbar && finalInstance != null) {
                    LogUtils.v("Alarm created: " + finalInstance);
                    AlarmUtils.popAlarmSetSnackbar(mSnackbarAnchor, finalInstance.getAlarmTime().getTimeInMillis());
                }

                if (mScrollHandler != null && finalNewAlarm != null) {
                    if (openEditScreen) {
                        mScrollHandler.setPendingAlarmToEdit(finalNewAlarm);
                    } else {
                        mScrollHandler.setSmoothScrollStableId(finalNewAlarm.id);
                    }
                }
            });
        });
    }

    /**
     * Modifies an alarm on the background, and optionally show a toast when done.
     *
     * @param alarm       The alarm to be modified.
     * @param popToast    whether a toast should be displayed when done.
     * @param minorUpdate if true, don't affect any currently snoozed instances.
     */
    public void asyncUpdateAlarm(final Alarm alarm, final boolean popToast, final boolean minorUpdate) {
        AppExecutors.getDiskIO().execute(() -> {
            ContentResolver cr = mAppContext.getContentResolver();

            if (alarm.relationalRuleId > 0 && !minorUpdate) {
                com.qwibbler.salarm.namaz.RelationalAlarmSyncManager.recalculateNextInstanceSync(mAppContext, alarm);

                if (popToast) {
                    final List<AlarmInstance> instanceList = AlarmInstance.getInstancesByAlarmId(cr, alarm.id);
                    if (!instanceList.isEmpty()) {
                        final long timeToDisplay = instanceList.get(0).getAlarmTime().getTimeInMillis();
                        AppExecutors.getMainThread().post(() ->
                                AlarmUtils.popAlarmSetSnackbar(mSnackbarAnchor, timeToDisplay)
                        );
                    }
                }
                return;
            }

            // Update alarm
            alarm.updateAlarm(cr);

            if (minorUpdate) {
                // Just update the instance in the database and update notifications.
                // Display a toast message for newly created alarms if the user took the opportunity to edit minor fields.
                final List<AlarmInstance> instanceList = AlarmInstance.getInstancesByAlarmId(cr, alarm.id);

                Long tempTime = null;

                for (AlarmInstance instance : instanceList) {
                    // Make a copy of the existing instance
                    final AlarmInstance newInstance = new AlarmInstance(instance);
                    // Copy over minor change data to the instance; we don't know
                    // exactly which minor field changed, so just copy them all.
                    newInstance.mLabel = alarm.label;
                    newInstance.mSyncByLabel = alarm.syncByLabel;
                    newInstance.mVibrate = alarm.vibrate;
                    newInstance.mVibrationPattern = alarm.vibrationPattern;
                    newInstance.mFlash = alarm.flash;
                    newInstance.mRingtone = alarm.alert;
                    newInstance.mAutoSilenceDuration = alarm.autoSilenceDuration;
                    newInstance.mSnoozeDuration = alarm.snoozeDuration;
                    newInstance.mMissedAlarmRepeatLimit = alarm.missedAlarmRepeatLimit;
                    newInstance.mCrescendoDuration = alarm.crescendoDuration;
                    newInstance.mAlarmVolume = alarm.alarmVolume;
                    newInstance.mDismissAfter = alarm.dismissAfter;

                    // If the alarm is in Missed state, mark it as Dismissed and clear its notification.
                    if (newInstance.mAlarmState == AlarmInstance.MISSED_STATE) {
                        LogUtils.i("Minor update: resetting missed alarm " + instance.mId);
                        newInstance.mAlarmState = AlarmInstance.DISMISSED_STATE;
                        AlarmNotifications.clearNotification(mAppContext, newInstance);
                    }
                    // Since we copied the mId of the old instance and the mId is used
                    // as the primary key in the AlarmInstance table, this will replace
                    // the existing instance.
                    newInstance.updateInstance(cr);
                    // Update the notification for this instance.
                    AlarmNotifications.updateNotification(mAppContext, newInstance);

                    if (popToast && tempTime == null) {
                        tempTime = newInstance.getAlarmTime().getTimeInMillis();
                    }
                }

                WidgetUtils.updateAllDigitalWidgets(mAppContext);

                if (popToast && tempTime != null) {
                    final Long timeToDisplay = tempTime;
                    AppExecutors.getMainThread().post(() ->
                        AlarmUtils.popAlarmSetSnackbar(mSnackbarAnchor, timeToDisplay)
                    );
                }

                return;
            }

            // Otherwise, this is a major update and we're going to re-create the alarm.
            AlarmStateManager.deleteAllInstances(mAppContext, alarm.id);

            final AlarmInstance finalInstance = alarm.enabled ? setupAlarmInstance(alarm) : null;
            Long tempTime = null;

            if (popToast && finalInstance != null) {
                if (mSyncToastLabel != null) {
                    String labelToSearch = mSyncToastLabel;
                    mSyncToastLabel = null;
                    AlarmInstance next = AlarmInstance.getNextAlarmInstanceByLabel(cr, labelToSearch);
                    if (next != null) {
                        tempTime = next.getAlarmTime().getTimeInMillis();
                    }
                } else {
                    tempTime = finalInstance.getAlarmTime().getTimeInMillis();
                }
            }

            final Long timeToDisplay = tempTime;

            if (timeToDisplay != null) {
                AppExecutors.getMainThread().post(() ->
                    AlarmUtils.popAlarmSetSnackbar(mSnackbarAnchor, timeToDisplay)
                );
            }

        });
    }

    /**
     * Toggles multiple alarms on the background using bulk-optimized paths.
     *
     * @param alarms  The list of alarms to be toggled.
     * @param enabled Whether to enable or disable the alarms.
     */
    public void asyncToggleAlarms(final List<Alarm> alarms, final boolean enabled) {
        AppExecutors.getDiskIO().execute(() -> {
            ContentResolver cr = mAppContext.getContentResolver();
            // Capture the current next firing alarm before the bulk operation to handle Power-off RTC correctly.
            final AlarmInstance oldNextAlarm = AlarmStateManager.getNextFiringAlarm(mAppContext);

            for (Alarm alarm : alarms) {
                alarm.enabled = enabled;
                if (alarm.relationalRuleId > 0) {
                    RelationalAlarmSyncManager.recalculateNextInstanceSync(mAppContext, alarm);
                } else {
                    // Update the alarm template in the DB.
                    alarm.updateAlarm(cr);

                    // Bulk-optimized instance lifecycle:
                    // 1. Delete existing instances without triggering global state recalculation.
                    AlarmStateManager.deleteAllInstances(mAppContext, alarm.id, false);

                    // 2. If enabling, create new instance without triggering global state recalculation.
                    if (alarm.enabled) {
                        AlarmInstance instance = alarm.createInstanceAfter(Calendar.getInstance());
                        instance.addInstance(cr);
                        AlarmStateManager.registerInstance(mAppContext, instance, false);
                    }
                }
            }

            // Exactly once: update global state, power-off alarm, and system indicators after the bulk work is done.
            AlarmStateManager.updateNextAlarmInternal(mAppContext, oldNextAlarm);
        });
    }

    /**
     * Deletes an alarm on the background.
     *
     * @param alarm The alarm to be deleted.
     */
    public void asyncDeleteAlarm(final Alarm alarm) {
        AppExecutors.getDiskIO().execute(() -> {
            // Activity may be closed at this point , make sure data is still valid
            if (alarm == null) {
                // Nothing to do here, just return.
                return;
            }

            RelationalRule ruleToDelete = null;
            if (alarm.relationalRuleId > 0) {
                ruleToDelete = NamazDatabase.getDatabase(mAppContext).relationalRuleDao().getById(alarm.relationalRuleId);
            }

            AlarmStateManager.deleteAllInstances(mAppContext, alarm.id);

            final boolean deleted = Alarm.deleteAlarm(mAppContext.getContentResolver(), alarm.id);

            if (deleted) {
                WidgetUtils.updateAllDigitalWidgets(mAppContext);
            }

            final RelationalRule finalRule = ruleToDelete;
            AppExecutors.getMainThread().post(() -> {
                if (deleted) {
                    mDeletedAlarm = alarm;
                    mDeletedRule = finalRule;
                    showUndoBar();
                }
            });
        });
    }

    /**
     * Instructs the next alarm update operation to display a toast based on the earliest upcoming
     * alarm instance that shares the specified label, rather than the specific instance being updated.
     *
     * <p>This label is consumed once during the next call to {@code asyncUpdateAlarm()}
     * where {@code popToast} is {@code true}. It is used when enabling a group of synchronized
     * alarms to ensure that only one toast is shown, corresponding to the earliest upcoming alarm
     * within that specific synchronized group.</p>
     *
     * @param label the label of the synchronized alarm group to calculate the next upcoming time for
     */
    public void useSyncToastForLabel(String label) {
        mSyncToastLabel = label;
    }

    /**
     * Hides any undo toast.
     */
    public void hideUndoBar() {
        mDeletedAlarm = null;
        mDeletedRule = null;
        SnackbarManager.dismiss();
    }

    private void showUndoBar() {
        final Alarm alarmBeingDeleted = mDeletedAlarm;
        final AtomicBoolean isUndone = new AtomicBoolean(false);

        final Context localizedContext = Utils.getLocalizedContext(mAppContext);
        final Snackbar snackbar = Snackbar.make(mSnackbarAnchor, localizedContext.getString(R.string.alarm_deleted),
            Snackbar.LENGTH_LONG).setAction(R.string.alarm_undo, v -> {
            isUndone.set(true);

            if (mDeletedAlarm != null) {
                Utils.performHapticFeedback(v, HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                final Alarm alarmToRestore = mDeletedAlarm;
                final RelationalRule ruleToRestore = mDeletedRule;

                mDeletedAlarm = null;
                mDeletedRule = null;

                Utils.performHapticFeedback(v, HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                if (ruleToRestore != null) {
                    asyncRestoreRelationalAlarm(alarmToRestore, ruleToRestore);
                } else {
                    asyncAddAlarm(alarmToRestore, true, false);
                }
            }
        });

        // Remove the alarm background image if the alarm is deleted and not restored using the Undo button.
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                // Permanently delete the background image of the deleted alarm if the user does not click the Undo button.
                if (!isUndone.get()) {
                    if (alarmBeingDeleted != null && !TextUtils.isEmpty(alarmBeingDeleted.backgroundImage)) {
                        final String imagePath = alarmBeingDeleted.backgroundImage;

                        // Delete the file in the background to avoid blocking the interface.
                        AppExecutors.getDiskIO().execute(() -> {
                            FileUtils.clearFile(imagePath);
                            LogUtils.i("Background image file permanently deleted : " + imagePath);
                        });
                    }
                }
            }
        });

        SnackbarManager.show(snackbar);
    }

    private void asyncRestoreRelationalAlarm(final Alarm alarm, final RelationalRule rule) {
        AppExecutors.getDiskIO().execute(() -> {
            // 1. Re-insert the rule to get a new ID
            rule.id = 0;
            alarm.relationalRuleId = NamazDatabase.getDatabase(mAppContext).relationalRuleDao().insert(rule);

            // 2. Add the alarm back to the provider
            final Alarm newAlarm = alarm.addAlarm(mAppContext.getContentResolver());

            // 3. Prepare for UI scroll
            if (mScrollHandler != null) {
                mScrollHandler.setSmoothScrollStableId(newAlarm.id);
            }

            // 4. Sync and schedule instances
            RelationalAlarmSyncManager.recalculateNextInstanceSync(mAppContext, newAlarm);

            // 5. Update widgets
            WidgetUtils.updateAllDigitalWidgets(mAppContext);

            // 6. Show confirmation toast (similar to asyncAddAlarm)
            final List<AlarmInstance> instances = AlarmInstance.getInstancesByAlarmId(
                    mAppContext.getContentResolver(), newAlarm.id);

            if (!instances.isEmpty()) {
                final long timeToDisplay = instances.get(0).getAlarmTime().getTimeInMillis();
                AppExecutors.getMainThread().post(() ->
                        AlarmUtils.popAlarmSetSnackbar(mSnackbarAnchor, timeToDisplay)
                );
            }
        });
    }

    private AlarmInstance setupAlarmInstance(Alarm alarm) {
        final ContentResolver cr = mAppContext.getContentResolver();
        AlarmInstance newInstance = alarm.createInstanceAfter(Calendar.getInstance());
        newInstance.addInstance(cr);
        // Register instance to state manager
        AlarmStateManager.registerInstance(mAppContext, newInstance, true);
        return newInstance;
    }

}
