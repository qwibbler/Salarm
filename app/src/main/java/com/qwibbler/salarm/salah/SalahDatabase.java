package com.qwibbler.salarm.salah;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {RelationalRule.class, CityCoordinate.class}, version = 8, exportSchema = false)
public abstract class SalahDatabase extends RoomDatabase {

    private static volatile SalahDatabase INSTANCE;

    public abstract RelationalRuleDao relationalRuleDao();

    public abstract CityCoordinateDao cityCoordinateDao();

    static final Migration MIGRATION_6_8 = new Migration(6, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `relational_rules` ADD COLUMN `adjustmentMinutes` INTEGER NOT NULL DEFAULT 0");
        }
    };


    public static SalahDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (SalahDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    SalahDatabase.class, "salah_database")
                            .addMigrations(MIGRATION_6_8)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
