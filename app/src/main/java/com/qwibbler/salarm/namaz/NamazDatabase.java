package com.qwibbler.salarm.namaz;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {RelationalRule.class, CityCoordinate.class}, version = 8, exportSchema = false)
public abstract class NamazDatabase extends RoomDatabase {

    private static volatile NamazDatabase INSTANCE;

    public abstract RelationalRuleDao relationalRuleDao();

    public abstract CityCoordinateDao cityCoordinateDao();

    static final Migration MIGRATION_6_8 = new Migration(6, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `relational_rules` ADD COLUMN `adjustmentMinutes` INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // adjustmentEnabled was added in v7 but removed in v8.
            // adjustmentMinutes was added in v7 and kept in v8.
            // Room will ignore the extra adjustmentEnabled column if it exists.
        }
    };

    public static NamazDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (NamazDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    NamazDatabase.class, "namaz_database")
                            .addMigrations(MIGRATION_6_8, MIGRATION_7_8)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
