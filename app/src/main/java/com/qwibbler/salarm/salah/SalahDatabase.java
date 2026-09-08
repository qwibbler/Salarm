package com.qwibbler.salarm.salah;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {RelationalRule.class, CityCoordinate.class, Masjid.class, MasjidEvent.class}, version = 13)
public abstract class SalahDatabase extends RoomDatabase {

    private static volatile SalahDatabase INSTANCE;

    public abstract RelationalRuleDao relationalRuleDao();

    public abstract CityCoordinateDao cityCoordinateDao();

    public abstract MasjidDao masjidDao();

    public abstract MasjidEventDao masjidEventDao();

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

    static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `relational_rules` ADD COLUMN `isManuallyDeselected` INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `relational_rules` ADD COLUMN `isFixedTime` INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE `relational_rules` ADD COLUMN `fixedHour` INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE `relational_rules` ADD COLUMN `fixedMinute` INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_12_13 = new Migration(12, 13) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE IF EXISTS `masjid_events` ");
            database.execSQL("DROP TABLE IF EXISTS `relational_rules_new` ");

            database.execSQL("CREATE TABLE `masjid_events` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`masjidId` INTEGER NOT NULL, " +
                    "`salahType` TEXT, " +
                    "`isSelected` INTEGER NOT NULL, " +
                    "`isManuallyDeselected` INTEGER NOT NULL, " +
                    "`isFixedTime` INTEGER, " +
                    "`fixedHour` INTEGER, " +
                    "`fixedMinute` INTEGER, " +
                    "`anchor_basicEvent` TEXT, " +
                    "`anchor_masjidEventId` INTEGER, " +
                    "`anchor_isMax` INTEGER, " +
                    "`calc_offsetMinutes` INTEGER, " +
                    "`calc_roundingDirection` TEXT, " +
                    "`calc_roundingIntervalMinutes` INTEGER, " +
                    "`calc_adjustmentMinutes` INTEGER, " +
                    "`clip_offsetMinutes` INTEGER, " +
                    "`clip_roundingDirection` TEXT, " +
                    "`clip_roundingIntervalMinutes` INTEGER, " +
                    "`clip_boundaryType` TEXT, " +
                    "`clip_anchor_basicEvent` TEXT, " +
                    "`clip_anchor_masjidEventId` INTEGER, " +
                    "`clip_anchor_isMax` INTEGER)");

            database.execSQL("INSERT INTO `masjid_events` (" +
                    "masjidId, salahType, isSelected, isManuallyDeselected, isFixedTime, fixedHour, fixedMinute, " +
                    "anchor_basicEvent, anchor_masjidEventId, anchor_isMax, " +
                    "calc_offsetMinutes, calc_roundingDirection, calc_roundingIntervalMinutes, calc_adjustmentMinutes, " +
                    "clip_offsetMinutes, clip_roundingDirection, clip_roundingIntervalMinutes, clip_boundaryType, " +
                    "clip_anchor_basicEvent, clip_anchor_masjidEventId, clip_anchor_isMax) " +
                    "SELECT masjidId, masjidSalahType, isSelected, isManuallyDeselected, " +
                    "COALESCE(isFixedTime, 0), COALESCE(fixedHour, 0), COALESCE(fixedMinute, 0), " +
                    "anchorEvent, NULL, COALESCE(anchorIsMax, 0), " +
                    "COALESCE(offsetMinutes, 0), roundingDirection, COALESCE(roundingIntervalMinutes, 0), COALESCE(adjustmentMinutes, 0), " +
                    "clip_clipOffsetMinutes, clip_clipRoundingDirection, clip_clipRoundingIntervalMinutes, clip_boundaryType, " +
                    "clip_clipEvent, NULL, COALESCE(clipIsMax, 0) " +
                    "FROM `relational_rules` WHERE masjidSalahType IS NOT NULL AND masjidId > 0");

            database.execSQL("CREATE TABLE `relational_rules_new` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`enabled` INTEGER NOT NULL, " +
                    "`masjidId` INTEGER NOT NULL, " +
                    "`masjidSalahType` TEXT, " +
                    "`isSelected` INTEGER NOT NULL, " +
                    "`isManuallyDeselected` INTEGER NOT NULL, " +
                    "`autostopEnabled` INTEGER NOT NULL, " +
                    "`autostopOffsetMinutes` INTEGER NOT NULL, " +
                    "`isFixedTime` INTEGER, " +
                    "`fixedHour` INTEGER, " +
                    "`fixedMinute` INTEGER, " +
                    "`anchor_basicEvent` TEXT, " +
                    "`anchor_masjidEventId` INTEGER, " +
                    "`anchor_isMax` INTEGER, " +
                    "`calc_offsetMinutes` INTEGER, " +
                    "`calc_roundingDirection` TEXT, " +
                    "`calc_roundingIntervalMinutes` INTEGER, " +
                    "`calc_adjustmentMinutes` INTEGER, " +
                    "`clip_offsetMinutes` INTEGER, " +
                    "`clip_roundingDirection` TEXT, " +
                    "`clip_roundingIntervalMinutes` INTEGER, " +
                    "`clip_boundaryType` TEXT, " +
                    "`clip_anchor_basicEvent` TEXT, " +
                    "`clip_anchor_masjidEventId` INTEGER, " +
                    "`clip_anchor_isMax` INTEGER, " +
                    "`autostop_basicEvent` TEXT, " +
                    "`autostop_masjidEventId` INTEGER, " +
                    "`autostop_isMax` INTEGER)");

            database.execSQL("INSERT INTO `relational_rules_new` (" +
                    "id, enabled, masjidId, masjidSalahType, isSelected, isManuallyDeselected, " +
                    "autostopEnabled, autostopOffsetMinutes, " +
                    "isFixedTime, fixedHour, fixedMinute, " +
                    "anchor_basicEvent, anchor_masjidEventId, anchor_isMax, " +
                    "calc_offsetMinutes, calc_roundingDirection, calc_roundingIntervalMinutes, calc_adjustmentMinutes, " +
                    "clip_offsetMinutes, clip_roundingDirection, clip_roundingIntervalMinutes, clip_boundaryType, " +
                    "clip_anchor_basicEvent, clip_anchor_masjidEventId, clip_anchor_isMax, " +
                    "autostop_basicEvent, autostop_masjidEventId, autostop_isMax) " +
                    "SELECT id, enabled, masjidId, masjidSalahType, isSelected, isManuallyDeselected, " +
                    "COALESCE(autostopEnabled, 0), COALESCE(autostopOffsetMinutes, 0), " +
                    "COALESCE(isFixedTime, 0), COALESCE(fixedHour, 0), COALESCE(fixedMinute, 0), " +
                    "anchorEvent, NULL, COALESCE(anchorIsMax, 0), " +
                    "COALESCE(offsetMinutes, 0), roundingDirection, COALESCE(roundingIntervalMinutes, 0), COALESCE(adjustmentMinutes, 0), " +
                    "clip_clipOffsetMinutes, clip_clipRoundingDirection, clip_clipRoundingIntervalMinutes, clip_boundaryType, " +
                    "clip_clipEvent, NULL, COALESCE(clipIsMax, 0), " +
                    "autostopAnchorEvent, NULL, 0 " +
                    "FROM `relational_rules` WHERE masjidSalahType IS NULL OR masjidId = 0");


            database.execSQL("UPDATE `relational_rules_new` SET anchor_masjidEventId = (" +
                    "SELECT me.id FROM masjid_events me " +
                    "INNER JOIN relational_rules old ON me.masjidId = old.anchorMasjidId AND me.salahType = old.anchorMasjidSalahType " +
                    "WHERE old.id = relational_rules_new.id LIMIT 1) " +
                    "WHERE id IN (SELECT id FROM relational_rules WHERE anchorMasjidId > 0)");

            database.execSQL("UPDATE `relational_rules_new` SET clip_anchor_masjidEventId = (" +
                    "SELECT me.id FROM masjid_events me " +
                    "INNER JOIN relational_rules old ON me.masjidId = old.clipMasjidId AND me.salahType = old.clipMasjidSalahType " +
                    "WHERE old.id = relational_rules_new.id LIMIT 1) " +
                    "WHERE id IN (SELECT id FROM relational_rules WHERE clipMasjidId > 0)");

            database.execSQL("DROP TABLE `relational_rules` ");
            database.execSQL("ALTER TABLE `relational_rules_new` RENAME TO `relational_rules` ");
        }
    };


    public static SalahDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (SalahDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    SalahDatabase.class, "salah_database")
                            .addMigrations(MIGRATION_6_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
