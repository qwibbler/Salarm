package com.qwibbler.salarm.salah;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public class MigrationTest {
    private static final String TEST_DB = "migration-test";

    @Rule
    public MigrationTestHelper helper;

    public MigrationTest() {
        helper = new MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
                SalahDatabase.class.getCanonicalName(),
                new FrameworkSQLiteOpenHelperFactory());
    }

    @Test
    public void migrate12To13() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 12);

        // Insert some data into v12
        db.execSQL("INSERT INTO relational_rules (id, enabled, masjidId, anchorIsMax, clipIsMax, masjidSalahType, isSelected, isManuallyDeselected, isFixedTime, fixedHour, fixedMinute, anchorMasjidId, anchorMasjidSalahType, clipMasjidId, clipMasjidSalahType, anchorEvent, offsetMinutes, roundingDirection, roundingIntervalMinutes, adjustmentMinutes, clip_clipEvent, clip_clipOffsetMinutes, clip_clipRoundingDirection, clip_clipRoundingIntervalMinutes, clip_boundaryType, autostopEnabled, autostopOffsetMinutes, autostopAnchorEvent) " +
                "VALUES (1, 1, 0, 0, 0, 'FAJR', 1, 0, 0, 0, 0, 0, NULL, 0, NULL, 'FAJR', 0, NULL, 0, 0, NULL, 0, NULL, 0, NULL, 0, 0, NULL)");

        // Add a masjid rule
        db.execSQL("INSERT INTO relational_rules (id, enabled, masjidId, anchorIsMax, clipIsMax, masjidSalahType, isSelected, isManuallyDeselected, isFixedTime, fixedHour, fixedMinute, anchorMasjidId, anchorMasjidSalahType, clipMasjidId, clipMasjidSalahType, anchorEvent, offsetMinutes, roundingDirection, roundingIntervalMinutes, adjustmentMinutes, clip_clipEvent, clip_clipOffsetMinutes, clip_clipRoundingDirection, clip_clipRoundingIntervalMinutes, clip_boundaryType, autostopEnabled, autostopOffsetMinutes, autostopAnchorEvent) " +
                "VALUES (2, 1, 1, 1, 0, 'DHUHR', 1, 0, 0, 0, 0, 0, NULL, 0, NULL, 'DHUHR', 5, 'NEAREST', 5, 2, NULL, 0, NULL, 0, NULL, 0, 0, NULL)");

        db.close();

        // Re-open the database with version 13 and provide MIGRATION_12_13
        helper.runMigrationsAndValidate(TEST_DB, 13, true, SalahDatabase.MIGRATION_12_13);
    }
}
