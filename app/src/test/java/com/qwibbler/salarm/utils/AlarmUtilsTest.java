package com.qwibbler.salarm.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.SharedPreferences;

import com.qwibbler.salarm.provider.Alarm;
import com.qwibbler.salarm.settings.PreferencesKeys;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

public class AlarmUtilsTest {

    private SharedPreferences mPrefs;

    @Before
    public void setUp() {
        mPrefs = mock(SharedPreferences.class);
    }

    @Test
    public void testFormatTimezoneOffset_ValidTimezones() {
        String tashkentOffset = AlarmUtils.formatTimezoneOffset("Asia/Tashkent");
        assertEquals("(UTC+5:00)", tashkentOffset);

        String kolkataOffset = AlarmUtils.formatTimezoneOffset("Asia/Kolkata");
        assertEquals("(UTC+5:30)", kolkataOffset);

        String emptyOffset = AlarmUtils.formatTimezoneOffset("");
        assertEquals("", emptyOffset);

        String nullOffset = AlarmUtils.formatTimezoneOffset(null);
        assertEquals("", nullOffset);
    }

    @Test
    public void testSyncInternationalAutoTag_LockedToTzAndAutotagEnabled() {
        when(mPrefs.getBoolean(PreferencesKeys.KEY_SALAH_AUTO_TAG, true)).thenReturn(true);

        Alarm alarm = new Alarm();
        alarm.timezoneId = "America/New_York";
        alarm.tags = new ArrayList<>();

        AlarmUtils.syncInternationalAutoTag(mPrefs, alarm);

        assertTrue("Alarm locked to timezone should be tagged with 'International'",
                alarm.tags.contains("International"));
    }

    @Test
    public void testSyncInternationalAutoTag_LockedToTzAndAutotagDisabled() {
        when(mPrefs.getBoolean(PreferencesKeys.KEY_SALAH_AUTO_TAG, true)).thenReturn(false);

        Alarm alarm = new Alarm();
        alarm.timezoneId = "America/New_York";
        alarm.tags = new ArrayList<>();

        AlarmUtils.syncInternationalAutoTag(mPrefs, alarm);

        assertFalse("Alarm should NOT be tagged with 'International' when autotag is disabled",
                alarm.tags.contains("International"));
    }

    @Test
    public void testSyncInternationalAutoTag_NotLockedToTz() {
        when(mPrefs.getBoolean(PreferencesKeys.KEY_SALAH_AUTO_TAG, true)).thenReturn(true);

        Alarm alarm = new Alarm();
        alarm.timezoneId = null;
        alarm.tags = new ArrayList<>();

        AlarmUtils.syncInternationalAutoTag(mPrefs, alarm);

        assertFalse("Alarm NOT locked to timezone should NOT be tagged with 'International'",
                alarm.tags.contains("International"));
    }
}
