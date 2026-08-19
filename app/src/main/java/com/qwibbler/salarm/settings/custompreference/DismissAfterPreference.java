// SPDX-License-Identifier: GPL-3.0-only

package com.qwibbler.salarm.settings.custompreference;

import static com.qwibbler.salarm.settings.PreferencesDefaultValues.DEFAULT_ALARM_DISMISS_AFTER;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

import com.qwibbler.salarm.R;

/**
 * A custom {@link DialogPreference} that allows users to select the automatic dismissal duration for alarms.
 */
public class DismissAfterPreference extends DialogPreference {

    public DismissAfterPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(true);
    }

    @Override
    public CharSequence getSummary() {
        int value = getDismissAfter();

        if (value == 0) {
            return getContext().getString(R.string.dismiss_after_never);
        } else if (value > 0) {
            return getContext().getResources().getQuantityString(R.plurals.dismiss_after_minutes, value, value);
        } else {
            int snoozeCount = Math.abs(value);
            return getContext().getResources().getQuantityString(R.plurals.dismiss_after_snoozing, snoozeCount, snoozeCount);
        }
    }

    public int getDismissAfter() {
        return getPersistedInt(DEFAULT_ALARM_DISMISS_AFTER);
    }

    public void setDismissAfter(int value) {
        persistInt(value);
        notifyChanged();
    }

}
