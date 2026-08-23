/*
 * Copyright (C) 2026 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.qwibbler.salarm.settings.custompreference;

import android.content.Context;
import android.database.Cursor;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.provider.ClockContract.AlarmLogColumns;
import com.qwibbler.salarm.utils.AlarmUtils;

public class AlarmLogPreference extends Preference {

    public AlarmLogPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.settings_preference_layout);
        setIcon(R.drawable.ic_backup_restore); // Use a suitable icon, maybe ic_history if exists

        setSummaryProvider(preference -> getLatestEventSummary());
    }

    private String getLatestEventSummary() {
        String selection = AlarmLogColumns.EVENT_TIME + " > ?";
        String[] selectionArgs = {String.valueOf(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)};
        String sortOrder = AlarmLogColumns.EVENT_TIME + " DESC LIMIT 1";

        try (Cursor cursor = getContext().getContentResolver().query(
                AlarmLogColumns.CONTENT_URI, null, selection, selectionArgs, sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                String label = cursor.getString(cursor.getColumnIndexOrThrow(AlarmLogColumns.LABEL));
                long firedTime = cursor.getLong(cursor.getColumnIndexOrThrow(AlarmLogColumns.FIRED_TIME));
                String originalTimeStr =  AlarmUtils.getSimpleFormattedTime(getContext(), firedTime);

                long eventTime = cursor.getLong(cursor.getColumnIndexOrThrow(AlarmLogColumns.EVENT_TIME));
                String timeStr = AlarmUtils.getSimpleFormattedTime(getContext(), eventTime);

                String alarmName;
                if (label == null || label.isEmpty()) {
                    alarmName = getContext().getString(R.string.default_label) + " (" + originalTimeStr + ")";
                } else {
                    alarmName = getContext().getString(R.string.default_label) + " (" + label + ")";
                }

                return alarmName + " at " + timeStr;
            }
        } catch (Exception e) {
            // Log or ignore
        }
        return getContext().getString(R.string.alarm_log_no_events);
    }
}
