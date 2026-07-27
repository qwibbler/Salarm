/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.qwibbler.salarm.controller;

import static com.qwibbler.salarm.uidata.UiDataModel.Tab.STOPWATCH;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.qwibbler.salarm.SalarmActivity;
import com.qwibbler.salarm.R;
import com.qwibbler.salarm.events.Events;
import com.qwibbler.salarm.stopwatch.StopwatchService;
import com.qwibbler.salarm.uidata.UiDataModel;
import com.qwibbler.salarm.utils.LogUtils;

public class HandleShortcuts extends Activity {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("HandleShortcuts");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        try {
            final String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case StopwatchService.ACTION_PAUSE_STOPWATCH -> {
                        Events.sendStopwatchEvent(R.string.action_pause, R.string.label_shortcut);

                        // Open SalarmActivity positioned on the stopwatch tab.
                        UiDataModel.getUiDataModel().setSelectedTab(STOPWATCH);
                        startActivity(new Intent(this, SalarmActivity.class).setAction(StopwatchService.ACTION_PAUSE_STOPWATCH));
                        setResult(RESULT_OK);
                    }
                    case StopwatchService.ACTION_START_STOPWATCH -> {
                        Events.sendStopwatchEvent(R.string.action_start, R.string.label_shortcut);

                        // Open SalarmActivity positioned on the stopwatch tab.
                        UiDataModel.getUiDataModel().setSelectedTab(STOPWATCH);
                        startActivity(new Intent(this, SalarmActivity.class).setAction(StopwatchService.ACTION_START_STOPWATCH));
                        setResult(RESULT_OK);
                    }
                    default -> throw new IllegalArgumentException("Unsupported action: " + action);
                }
            }
        } catch (Exception e) {
            LOGGER.e("Error handling intent: " + intent, e);
            setResult(RESULT_CANCELED);
        } finally {
            finish();
        }
    }
}
