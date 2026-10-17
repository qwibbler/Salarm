package com.qwibbler.salarm.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.SharedPreferences;
import android.net.Uri;

import com.qwibbler.salarm.provider.Alarm;
import com.qwibbler.salarm.settings.PreferencesDefaultValues;
import com.qwibbler.salarm.settings.PreferencesKeys;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SettingsPrecedenceTest {

    private FakeSharedPreferences mPrefs;

    @Before
    public void setUp() {
        mPrefs = new FakeSharedPreferences();
    }

    @Test
    public void testSalahDefaultOverridesBasicDefault_forSalahAlarm() {
        mPrefs.edit()
                .putBoolean(PreferencesKeys.KEY_SALAH_ALARM_RINGTONE_OVERRIDE, true)
                .putInt(PreferencesKeys.KEY_SALAH_ALARM_SNOOZE_DURATION, 25)
                .putInt(PreferencesKeys.KEY_ALARM_SNOOZE_DURATION, 10)
                .apply();

        // For a Salah alarm, Salah default (25) overrides Basic default (10)
        int snooze = SettingsDAO.getSnoozeLength(mPrefs, true);
        assertEquals(25, snooze);
    }

    @Test
    public void testBasicDefaultUsed_forNonSalahAlarm_evenWhenSalahDefaultConfigured() {
        mPrefs.edit()
                .putBoolean(PreferencesKeys.KEY_SALAH_ALARM_RINGTONE_OVERRIDE, true)
                .putInt(PreferencesKeys.KEY_SALAH_ALARM_SNOOZE_DURATION, 25)
                .putInt(PreferencesKeys.KEY_ALARM_SNOOZE_DURATION, 10)
                .apply();

        // For a non-Salah alarm, Salah default (25) is ignored and Basic default (10) is used
        int snooze = SettingsDAO.getSnoozeLength(mPrefs, false);
        assertEquals(10, snooze);
    }

    @Test
    public void testBasicDefaultUsed_forSalahAlarm_whenSalahDefaultNotConfigured() {
        mPrefs.edit()
                .putBoolean(PreferencesKeys.KEY_SALAH_ALARM_RINGTONE_OVERRIDE, true)
                // KEY_SALAH_ALARM_SNOOZE_DURATION is omitted
                .putInt(PreferencesKeys.KEY_ALARM_SNOOZE_DURATION, 12)
                .apply();

        // For a Salah alarm where no Salah default is explicitly set, fall back to Basic default (12)
        int snooze = SettingsDAO.getSnoozeLength(mPrefs, true);
        assertEquals(12, snooze);
    }

    @Test
    public void testHardcodedDefaultUsed_whenNeitherSalahNorBasicConfigured() {
        // Neither KEY_SALAH_ALARM_SNOOZE_DURATION nor KEY_ALARM_SNOOZE_DURATION is set
        mPrefs.edit()
                .putBoolean(PreferencesKeys.KEY_SALAH_ALARM_RINGTONE_OVERRIDE, true)
                .apply();

        int snoozeSalah = SettingsDAO.getSnoozeLength(mPrefs, true);
        int snoozeNonSalah = SettingsDAO.getSnoozeLength(mPrefs, false);

        assertEquals(PreferencesDefaultValues.DEFAULT_ALARM_SNOOZE_DURATION, snoozeSalah);
        assertEquals(PreferencesDefaultValues.DEFAULT_ALARM_SNOOZE_DURATION, snoozeNonSalah);
    }

    @Test
    public void testPartialConfiguration_fallsBackPerField() {
        mPrefs.edit()
                .putBoolean(PreferencesKeys.KEY_SALAH_ALARM_RINGTONE_OVERRIDE, true)
                .putInt(PreferencesKeys.KEY_SALAH_ALARM_SNOOZE_DURATION, 15) // Configured Salah default
                // KEY_SALAH_AUTO_SILENCE_DURATION is omitted
                .putInt(PreferencesKeys.KEY_AUTO_SILENCE_DURATION, 300) // Configured Basic default
                .apply();

        // Snooze length uses Salah default (15)
        assertEquals(15, SettingsDAO.getSnoozeLength(mPrefs, true));

        // Auto silence falls back to Basic default (300) per-field
        assertEquals(300, SettingsDAO.getAlarmTimeout(mPrefs, true));
    }

    @Test
    public void testExplicitValuesZeroAndFalsePreserved() {
        mPrefs.edit()
                .putBoolean(PreferencesKeys.KEY_SALAH_ALARM_RINGTONE_OVERRIDE, true)
                .putInt(PreferencesKeys.KEY_SALAH_ALARM_DISMISS_AFTER, 0) // Explicit 0
                .putBoolean(PreferencesKeys.KEY_SALAH_ENABLE_PER_ALARM_VOLUME, false) // Explicit false
                .putInt(PreferencesKeys.KEY_ALARM_DISMISS_AFTER, 10)
                .putBoolean(PreferencesKeys.KEY_ENABLE_PER_ALARM_VOLUME, true)
                .apply();

        // Explicit 0 for dismiss after is preserved
        assertEquals(0, SettingsDAO.getAlarmDismissAfter(mPrefs, true));

        // Explicit false for per alarm volume is preserved
        assertFalse(SettingsDAO.isPerAlarmVolumeEnabled(mPrefs, true));
    }

    @Test
    public void testSalahDefaultChangesDoNotAffectNonSalahAlarms() {
        mPrefs.edit()
                .putInt(PreferencesKeys.KEY_ALARM_SNOOZE_DURATION, 10)
                .apply();

        assertEquals(10, SettingsDAO.getSnoozeLength(mPrefs, false));

        // Add Salah default override
        mPrefs.edit()
                .putBoolean(PreferencesKeys.KEY_SALAH_ALARM_RINGTONE_OVERRIDE, true)
                .putInt(PreferencesKeys.KEY_SALAH_ALARM_SNOOZE_DURATION, 99)
                .apply();

        // Non-Salah alarm is unchanged
        assertEquals(10, SettingsDAO.getSnoozeLength(mPrefs, false));
        // Salah alarm picks up the new Salah default
        assertEquals(99, SettingsDAO.getSnoozeLength(mPrefs, true));
    }

    @Test
    public void testIndividualSettingPrecedenceContract() {
        mPrefs.edit()
                .putBoolean(PreferencesKeys.KEY_SALAH_ALARM_RINGTONE_OVERRIDE, true)
                .putInt(PreferencesKeys.KEY_SALAH_ALARM_SNOOZE_DURATION, 20)
                .putInt(PreferencesKeys.KEY_ALARM_SNOOZE_DURATION, 10)
                .apply();

        // Create a Salah alarm with explicit individual snoozeDuration = 5
        Alarm salahAlarm = new Alarm(2026, 6, 16, 5, 30, Uri.parse("content://settings/alarm"));
        salahAlarm.relationalRuleId = 123; // Salah alarm
        salahAlarm.snoozeDuration = 5; // Individual setting

        // Create a non-Salah alarm with explicit individual snoozeDuration = 7
        Alarm nonSalahAlarm = new Alarm(2026, 6, 16, 7, 0, Uri.parse("content://settings/alarm"));
        nonSalahAlarm.relationalRuleId = -1; // Non-Salah alarm
        nonSalahAlarm.snoozeDuration = 7; // Individual setting

        // Individual settings override both Salah defaults and Basic defaults
        assertEquals(5, salahAlarm.snoozeDuration);
        assertEquals(7, nonSalahAlarm.snoozeDuration);
    }

    /**
     * Simple in-memory FakeSharedPreferences for unit testing.
     */
    private static class FakeSharedPreferences implements SharedPreferences {
        private final Map<String, Object> mMap = new HashMap<>();

        @Override
        public Map<String, ?> getAll() {
            return new HashMap<>(mMap);
        }

        @Override
        public String getString(String key, String defValue) {
            Object v = mMap.get(key);
            return v instanceof String ? (String) v : defValue;
        }

        @Override
        public Set<String> getStringSet(String key, Set<String> defValues) {
            Object v = mMap.get(key);
            return v instanceof Set ? (Set<String>) v : defValues;
        }

        @Override
        public int getInt(String key, int defValue) {
            Object v = mMap.get(key);
            return v instanceof Integer ? (Integer) v : defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            Object v = mMap.get(key);
            return v instanceof Long ? (Long) v : defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            Object v = mMap.get(key);
            return v instanceof Float ? (Float) v : defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object v = mMap.get(key);
            return v instanceof Boolean ? (Boolean) v : defValue;
        }

        @Override
        public boolean contains(String key) {
            return mMap.containsKey(key);
        }

        @Override
        public Editor edit() {
            return new FakeEditor();
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

        private class FakeEditor implements Editor {
            private final Map<String, Object> mTempMap = new HashMap<>(mMap);

            @Override
            public Editor putString(String key, String value) {
                mTempMap.put(key, value);
                return this;
            }

            @Override
            public Editor putStringSet(String key, Set<String> values) {
                mTempMap.put(key, values);
                return this;
            }

            @Override
            public Editor putInt(String key, int value) {
                mTempMap.put(key, value);
                return this;
            }

            @Override
            public Editor putLong(String key, long value) {
                mTempMap.put(key, value);
                return this;
            }

            @Override
            public Editor putFloat(String key, float value) {
                mTempMap.put(key, value);
                return this;
            }

            @Override
            public Editor putBoolean(String key, boolean value) {
                mTempMap.put(key, value);
                return this;
            }

            @Override
            public Editor remove(String key) {
                mTempMap.remove(key);
                return this;
            }

            @Override
            public Editor clear() {
                mTempMap.clear();
                return this;
            }

            @Override
            public boolean commit() {
                mMap.clear();
                mMap.putAll(mTempMap);
                return true;
            }

            @Override
            public void apply() {
                commit();
            }
        }
    }
}
