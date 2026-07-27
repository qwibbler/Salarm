/*
 * Copyright (C) 2023 The LineageOS Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.qwibbler.salarm.timer;

import android.content.Context;
import android.view.View;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.data.DataModel;
import com.qwibbler.salarm.data.Timer;
import com.qwibbler.salarm.data.TimerStringFormatter;
import com.qwibbler.salarm.dialogfragment.LabelDialogFragment;
import com.qwibbler.salarm.dialogfragment.TimerAddTimeButtonDialogFragment;
import com.qwibbler.salarm.dialogfragment.TimerSetNewDurationDialogFragment;
import com.qwibbler.salarm.events.Events;

/**
 * Click handler for a timer item.
 */
public record TimerClickHandler(TimerFragment mTimerFragment) {

    public void onEditLabelClicked(Timer timer) {
        Events.sendAlarmEvent(R.string.action_set_label, R.string.label_salarm);
        final LabelDialogFragment fragment = LabelDialogFragment.newInstance(timer);
        LabelDialogFragment.show(mTimerFragment.getParentFragmentManager(), fragment);
    }

    public void onEditAddTimeButtonLongClicked(Timer timer) {
        Events.sendTimerEvent(R.string.action_add_custom_time_to_timer, R.string.label_salarm);
        final TimerAddTimeButtonDialogFragment fragment = TimerAddTimeButtonDialogFragment.newInstance(timer);
        TimerAddTimeButtonDialogFragment.show(mTimerFragment.getParentFragmentManager(), fragment);
    }

    public void onDurationClicked(Timer timer) {
        Events.sendTimerEvent(R.string.action_set_new_timer_duration, R.string.label_salarm);
        final TimerSetNewDurationDialogFragment fragment = TimerSetNewDurationDialogFragment.newInstance(timer);
        TimerSetNewDurationDialogFragment.show(mTimerFragment.getParentFragmentManager(), fragment);
    }

    public void onDeleteTimerClicked(Timer timer) {
        mTimerFragment.confirmAndDeleteTimer(timer);
    }

    public void onPlayPauseClicked(Timer timer) {
        if (timer.isPaused() || timer.isReset()) {
            DataModel.getDataModel().startTimer(timer);
        } else if (timer.isRunning()) {
            DataModel.getDataModel().pauseTimer(timer);
        } else if (timer.isExpired()) {
            DataModel.getDataModel().resetOrDeleteExpiredTimers(R.string.label_salarm);
        } else if (timer.isMissed()) {
            DataModel.getDataModel().resetMissedTimers(R.string.label_salarm);
        }
    }

    public void onResetClicked(Timer timer) {
        DataModel.getDataModel().resetOrDeleteTimer(timer, R.string.label_salarm);
    }

    public void onAddTimeClicked(Timer timer, View v) {
        DataModel.getDataModel().addCustomTimeToTimer(timer);
        Events.sendTimerEvent(R.string.action_add_custom_time_to_timer, R.string.label_salarm);

        Context context = mTimerFragment.requireContext();

        final long currentTime = timer.getRemainingTime();
        final String buttonTime = timer.getButtonTime();

        if (currentTime > 0) {
            v.announceForAccessibility(TimerStringFormatter.formatString(
                context, R.string.timer_accessibility_custom_time_added, buttonTime, currentTime, true));
        }
    }

}
