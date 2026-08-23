/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.qwibbler.salarm.data;

import static com.qwibbler.salarm.settings.PreferencesDefaultValues.VISIBLE_TAB_SALAH;

import android.content.Context;
import android.content.SharedPreferences;

import com.qwibbler.salarm.SalarmApplication;
import com.qwibbler.salarm.settings.PreferencesKeys;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Handles the migration of user data from "Namaz" terminology to "Salah".
 * This includes SharedPreferences keys and the Room database filename.
 */
public final class SalahMigration {

    private static final String KEY_MIGRATION_DONE = "key_salah_migration_done";

    /**
     * Performs the full migration if it hasn't been done yet.
     */
    public static void migrate(Context context) {
        SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(context);
        if (prefs.getBoolean(KEY_MIGRATION_DONE, false)) {
            return;
        }

        migrateDatabase(context);
        migratePreferences(prefs);

        prefs.edit().putBoolean(KEY_MIGRATION_DONE, true).apply();
    }

    private static void migrateDatabase(Context context) {
        File oldDb = context.getDatabasePath("namaz_database");
        if (oldDb.exists()) {
            File newDb = context.getDatabasePath("salah_database");
            if (!newDb.exists()) {
                oldDb.renameTo(newDb);

                // Rename sidecar files
                renameSidecar(context, "namaz_database-shm", "salah_database-shm");
                renameSidecar(context, "namaz_database-wal", "salah_database-wal");
            }
        }
    }

    private static void renameSidecar(Context context, String oldName, String newName) {
        File oldFile = context.getDatabasePath(oldName);
        if (oldFile.exists()) {
            oldFile.renameTo(context.getDatabasePath(newName));
        }
    }

    private static void migratePreferences(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        Map<String, ?> allEntries = prefs.getAll();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("key_namaz_")) {
                String newKey = key.replace("key_namaz_", "key_salah_");
                Object value = entry.getValue();

                if (value instanceof Boolean) {
                    editor.putBoolean(newKey, (Boolean) value);
                } else if (value instanceof Integer) {
                    editor.putInt(newKey, (Integer) value);
                } else if (value instanceof Long) {
                    editor.putLong(newKey, (Long) value);
                } else if (value instanceof Float) {
                    editor.putFloat(newKey, (Float) value);
                } else if (value instanceof String) {
                    editor.putString(newKey, (String) value);
                } else if (value instanceof Set) {
                    editor.putStringSet(newKey, (Set<String>) value);
                }
                editor.remove(key);
            }
        }

        // Specifically migrate the tab identifiers in set/string
        migrateTabIdentifiers(prefs, editor);

        editor.apply();
    }

    private static void migrateTabIdentifiers(SharedPreferences prefs, SharedPreferences.Editor editor) {
        // 1. Visible tabs set
        Set<String> visibleTabs = prefs.getStringSet(PreferencesKeys.KEY_VISIBLE_TABS, null);
        if (visibleTabs != null && visibleTabs.contains("namaz")) {
            Set<String> newSet = new HashSet<>(visibleTabs);
            newSet.remove("namaz");
            newSet.add(VISIBLE_TAB_SALAH);
            editor.putStringSet(PreferencesKeys.KEY_VISIBLE_TABS, newSet);
        }

        // 2. Tab to display (index-based or name-based? checking DAO...)
        // It seems it's stored as a string index in KEY_TAB_TO_DISPLAY. No change needed if it's "4".
    }
}
