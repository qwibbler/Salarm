/*
 * Copyright (C) 2023 The LineageOS Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.qwibbler.salarm.timer;

import android.content.Context;
import android.view.View;

import androidx.core.view.ViewCompat;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.data.DataModel;
import com.qwibbler.salarm.data.Timer;
import com.qwibbler.salarm.data.TimerStringFormatter;
import com.qwibbler.salarm.dialogfragment.LabelDialogFragment;
import com.qwibbler.salarm.dialogfragment.TimerAddTimeButtonDialogFragment;
import com.qwibbler.salarm.dialogfragment.TimerSetNewDurationDialogFragment;
import com.qwibbler.salarm.events.Events;
import com.qwibbler.salarm.utils.LogUtils;

/**
 * Click handler for a timer item.
 */
public record TimerClickHandler(TimerFragment mTimerFragment) {

    public static final String TAG = "TimerClickHandler";
    private static final LogUtils.Logger LOGGER = new LogUtils.Logger(TAG);

    public void onEditLabelClicked(Timer timer) {
        Events.sendAlarmEvent(R.string.action_set_label, R.string.label_salarm);
        final LabelDialogFragment fragment = LabelDialogFragment.newInstance(timer.getId(), timer.getLabel());
        LabelDialogFragment.show(mTimerFragment.getParentFragmentManager(), fragment);
    }

    public void onEditAddTimeButtonLongClicked(Timer timer) {
        Events.sendTimerEvent(R.string.action_add_custom_time_to_timer, R.string.label_salarm);
        final TimerAddTimeButtonDialogFragment fragment = TimerAddTimeButtonDialogFragment.newInstance(timer.getId(), Integer.parseInt(timer.getButtonTime()));
        TimerAddTimeButtonDialogFragment.show(mTimerFragment.getParentFragmentManager(), fragment);
    }

    public void onDurationClicked(Timer timer) {
        Events.sendTimerEvent(R.string.action_set_new_timer_duration, R.string.label_salarm);
        final TimerSetNewDurationDialogFragment fragment = TimerSetNewDurationDialogFragment.newInstance(timer.getId(), timer.getLength());
        TimerSetNewDurationDialogFragment.show(mTimerFragment.getParentFragmentManager(), fragment);
    }

    public void onDeleteTimerClicked(Timer timer) {
        mTimerFragment.confirmAndDeleteTimer(timer);
    }

    public void onPlayPauseClicked(Timer timer) {
        if (timer.isPaused() || timer.isReset()) {
            Events.sendTimerEvent(R.string.action_start, R.string.label_salarm);
            DataModel.getDataModel().startTimer(timer);
        } else if (timer.isRunning()) {
            Events.sendTimerEvent(R.string.action_pause, R.string.label_salarm);
            DataModel.getDataModel().pauseTimer(timer);
        } else if (timer.isExpired()) {
            final boolean isDeleteAfterUse = timer.getDeleteAfterUse();

            Events.sendTimerEvent(isDeleteAfterUse ? R.string.action_delete : R.string.action_reset, R.string.label_salarm);

            if (isDeleteAfterUse) {
                DataModel.getDataModel().removeTimer(timer, R.string.label_salarm);
            } else {
                DataModel.getDataModel().resetOrDeleteExpiredTimers(R.string.label_salarm);
            }
        } else if (timer.isMissed()) {
            Events.sendTimerEvent(R.string.action_reset, R.string.label_salarm);
            DataModel.getDataModel().resetOrDeleteMissedTimers(R.string.label_salarm);
        }
    }

    public void onResetClicked(Timer timer) {
        Events.sendTimerEvent(R.string.action_reset, R.string.label_salarm);
        DataModel.getDataModel().resetTimer(timer, R.string.label_salarm);
    }

    public void onAddTimeClicked(Timer timer, View v) {
        Events.sendTimerEvent(R.string.action_add_custom_time_to_timer, R.string.label_salarm);
        DataModel.getDataModel().addCustomTimeToTimer(timer);

        Context context = mTimerFragment.requireContext();

        final long currentTime = timer.getRemainingTime();
        final String buttonTime = timer.getButtonTime();

        if (currentTime > 0) {
            ViewCompat.setStateDescription(v, TimerStringFormatter.formatString(
                context, R.string.timer_accessibility_custom_time_added, buttonTime, currentTime, true)
            );
        }
    }

    public void onCircleClicked(Timer timer) {
        onPlayPauseClicked(timer);
    }

    public void displayBottomSheetDialog(Timer timer) {
        TimerEditBottomSheetFragment fragment = TimerEditBottomSheetFragment.newInstance(timer.getId(), mTimerFragment.getTag());

        TimerEditBottomSheetFragment.show(mTimerFragment.getParentFragmentManager(), fragment);
        LOGGER.v("Opening BottomSheet to edit timer: " + timer.getId());
    }

}
