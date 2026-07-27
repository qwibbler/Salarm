/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.qwibbler.salarm.controller;

import static com.qwibbler.salarm.SalarmApplication.getDefaultSharedPreferences;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.provider.AlarmClock;

import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;

import com.qwibbler.salarm.SalarmActivity;
import com.qwibbler.salarm.SalarmApplication;
import com.qwibbler.salarm.R;
import com.qwibbler.salarm.data.DataModel;
import com.qwibbler.salarm.data.SettingsDAO;
import com.qwibbler.salarm.data.Stopwatch;
import com.qwibbler.salarm.data.StopwatchListener;
import com.qwibbler.salarm.events.Events;
import com.qwibbler.salarm.events.ShortcutEventTracker;
import com.qwibbler.salarm.screensaver.ScreensaverActivity;
import com.qwibbler.salarm.stopwatch.StopwatchService;
import com.qwibbler.salarm.uidata.UiDataModel;
import com.qwibbler.salarm.utils.DeviceUtils;
import com.qwibbler.salarm.utils.LogUtils;
import com.qwibbler.salarm.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiresApi(Build.VERSION_CODES.N_MR1)
class ShortcutController {

    private final ComponentName mComponentName;
    private final ShortcutManager mShortcutManager;

    ShortcutController() {
        Context appContext = SalarmApplication.getAppContext();
        mComponentName = new ComponentName(appContext, SalarmActivity.class);
        mShortcutManager = appContext.getSystemService(ShortcutManager.class);
        Controller.getController().addEventTracker(new ShortcutEventTracker());
        DataModel.getDataModel().addStopwatchListener(new StopwatchWatcher());
    }

    void updateShortcuts() {
        Context appContext = Utils.getLocalizedContext(SalarmApplication.getAppContext());

        if (!DeviceUtils.isUserUnlocked(appContext)) {
            return;
        }

        try {
            SharedPreferences prefs = getDefaultSharedPreferences(appContext);

            List<ShortcutInfo> dynamicShortcuts = new ArrayList<>();
            List<String> disabledShortcutIds = new ArrayList<>();

            if (SettingsDAO.isAlarmTabVisible(prefs)) {
                dynamicShortcuts.add(createNewAlarmShortcut());
            } else {
                disabledShortcutIds.add(UiDataModel.getUiDataModel().getShortcutId(R.string.category_alarm, R.string.action_create));
            }

            if (SettingsDAO.isTimerTabVisible(prefs)) {
                dynamicShortcuts.add(createNewTimerShortcut());
            } else {
                disabledShortcutIds.add(UiDataModel.getUiDataModel().getShortcutId(R.string.category_timer, R.string.action_create));
            }

            if (SettingsDAO.isStopwatchTabVisible(prefs)) {
                dynamicShortcuts.add(createStopwatchShortcut());
            } else {
                disabledShortcutIds.add(UiDataModel.getUiDataModel().getShortcutId(R.string.category_stopwatch, R.string.action_start));
                disabledShortcutIds.add(UiDataModel.getUiDataModel().getShortcutId(R.string.category_stopwatch, R.string.action_pause));
            }

            dynamicShortcuts.add(createScreensaverShortcut());

            mShortcutManager.setDynamicShortcuts(dynamicShortcuts);

            if (!disabledShortcutIds.isEmpty()) {
                mShortcutManager.disableShortcuts(disabledShortcutIds, appContext.getString(R.string.shortcut_disabled));
            }

            List<String> enabledShortcutIds = new ArrayList<>();
            for (ShortcutInfo info : dynamicShortcuts) {
                enabledShortcutIds.add(info.getId());
            }

            mShortcutManager.enableShortcuts(enabledShortcutIds);
        } catch (IllegalStateException e) {
            LogUtils.wtf(e);
        }
    }

    private ShortcutInfo createNewAlarmShortcut() {
        Context appContext = Utils.getLocalizedContext(SalarmApplication.getAppContext());

        final Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM)
            .setClass(appContext, HandleApiCalls.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_shortcut);
        final String setAlarmShortcut = UiDataModel.getUiDataModel().getShortcutId(R.string.category_alarm, R.string.action_create);

        return new ShortcutInfo.Builder(appContext, setAlarmShortcut)
            .setIcon(Icon.createWithResource(appContext, R.drawable.shortcut_new_alarm))
            .setActivity(mComponentName)
            .setShortLabel(appContext.getString(R.string.shortcut_new_alarm_short))
            .setLongLabel(appContext.getString(R.string.shortcut_new_alarm_long))
            .setIntent(intent)
            .setRank(0)
            .build();
    }

    private ShortcutInfo createNewTimerShortcut() {
        Context appContext = Utils.getLocalizedContext(SalarmApplication.getAppContext());

        final Intent intent = new Intent(AlarmClock.ACTION_SET_TIMER)
            .setClass(appContext, HandleApiCalls.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_shortcut);
        final String setTimerShortcut = UiDataModel.getUiDataModel().getShortcutId(R.string.category_timer, R.string.action_create);

        return new ShortcutInfo.Builder(appContext, setTimerShortcut)
            .setIcon(Icon.createWithResource(appContext, R.drawable.shortcut_new_timer))
            .setActivity(mComponentName)
            .setShortLabel(appContext.getString(R.string.shortcut_new_timer_short))
            .setLongLabel(appContext.getString(R.string.shortcut_new_timer_long))
            .setIntent(intent)
            .setRank(1)
            .build();
    }

    private ShortcutInfo createStopwatchShortcut() {
        Context appContext = Utils.getLocalizedContext(SalarmApplication.getAppContext());

        final @StringRes int action = DataModel.getDataModel().getStopwatch().isRunning()
            ? R.string.action_pause
            : R.string.action_start;
        final String shortcutId = UiDataModel.getUiDataModel().getShortcutId(R.string.category_stopwatch, action);
        final ShortcutInfo.Builder shortcut = new ShortcutInfo.Builder(appContext, shortcutId)
            .setIcon(Icon.createWithResource(appContext, R.drawable.shortcut_stopwatch))
            .setActivity(mComponentName)
            .setRank(2);
        final Intent intent;

        if (DataModel.getDataModel().getStopwatch().isRunning()) {
            intent = new Intent(StopwatchService.ACTION_PAUSE_STOPWATCH).putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_shortcut);
            shortcut.setShortLabel(appContext.getString(R.string.shortcut_pause_stopwatch_short)).setLongLabel(appContext.getString(R.string.shortcut_pause_stopwatch_long));
        } else {
            intent = new Intent(StopwatchService.ACTION_START_STOPWATCH).putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_shortcut);
            shortcut.setShortLabel(appContext.getString(R.string.shortcut_start_stopwatch_short)).setLongLabel(appContext.getString(R.string.shortcut_start_stopwatch_long));
        }

        intent.setClass(appContext, HandleShortcuts.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return shortcut.setIntent(intent).build();
    }

    private ShortcutInfo createScreensaverShortcut() {
        Context appContext = Utils.getLocalizedContext(SalarmApplication.getAppContext());

        final Intent intent = new Intent(Intent.ACTION_MAIN)
            .setClass(appContext, ScreensaverActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_shortcut);
        final String screensaverShortcut = UiDataModel.getUiDataModel().getShortcutId(R.string.category_screensaver, R.string.action_show);

        return new ShortcutInfo.Builder(appContext, screensaverShortcut)
            .setIcon(Icon.createWithResource(appContext, R.drawable.shortcut_screensaver))
            .setActivity(mComponentName)
            .setShortLabel((appContext.getString(R.string.shortcut_start_screensaver_short)))
            .setLongLabel((appContext.getString(R.string.shortcut_start_screensaver_long)))
            .setIntent(intent)
            .setRank(3)
            .build();
    }

    private class StopwatchWatcher implements StopwatchListener {
        @Override
        public void stopwatchUpdated(Stopwatch after) {
            Context context = SalarmApplication.getAppContext();

            if (!DeviceUtils.isUserUnlocked(context)) {
                return;
            }

            try {
                mShortcutManager.updateShortcuts(Collections.singletonList(createStopwatchShortcut()));
            } catch (IllegalStateException e) {
                LogUtils.wtf(e);
            }
        }

    }
}
