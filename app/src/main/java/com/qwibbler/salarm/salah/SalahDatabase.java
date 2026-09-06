package com.qwibbler.salarm.salah;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {RelationalRule.class, CityCoordinate.class, Masjid.class}, version = 10, exportSchema = false)
public abstract class SalahDatabase extends RoomDatabase {

    private static volatile SalahDatabase INSTANCE;

    public abstract RelationalRuleDao relationalRuleDao();

    public abstract CityCoordinateDao cityCoordinateDao();

    public abstract MasjidDao masjidDao();

    static final Migration MIGRATION_6_8 = new Migration(6, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `relational_rules` ADD COLUMN `adjustmentMinutes` INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `masjids` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `showMasjidEvents` INTEGER NOT NULL, `intervalsCount` INTEGER NOT NULL, `boundary1` INTEGER NOT NULL, `boundary2` INTEGER NOT NULL)");
            database.execSQL("ALTER TABLE `relational_rules` ADD COLUMN `masjidId` INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE `relational_rules` ADD COLUMN `anchorIsMax` INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE `relational_rules` ADD COLUMN `clipIsMax` INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE `relational_rules` ADD COLUMN `masjidSalahType` TEXT");
            database.execSQL("ALTER TABLE `relational_rules` ADD COLUMN `isSelected` INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `relational_rules` ADD COLUMN `anchorMasjidId` INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE `relational_rules` ADD COLUMN `anchorMasjidSalahType` TEXT");
            database.execSQL("ALTER TABLE `relational_rules` ADD COLUMN `clipMasjidId` INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE `relational_rules` ADD COLUMN `clipMasjidSalahType` TEXT");
        }
    };


    public static SalahDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (SalahDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    SalahDatabase.class, "salah_database")
                            .addMigrations(MIGRATION_6_8, MIGRATION_8_9, MIGRATION_9_10)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
