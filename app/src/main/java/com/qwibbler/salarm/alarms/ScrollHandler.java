/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.qwibbler.salarm.alarms;

import com.qwibbler.salarm.provider.Alarm;

/**
 * API that handles scrolling.
 */
public interface ScrollHandler {

    /**
     * Sets the stable id that view should be scrolled to. The view does not actually scroll yet.
     */
    void setSmoothScrollStableId(long stableId);

    /**
     * Sets an alarm that is pending to be edited. When the list is next updated,
     * this alarm will be scrolled to and its edit BottomSheet will be opened.
     *
     * @param alarm the alarm to be edited
     */
    void setPendingAlarmToEdit(Alarm alarm);

}
