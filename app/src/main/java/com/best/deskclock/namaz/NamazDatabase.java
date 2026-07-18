package com.best.deskclock.namaz;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {RelationalRule.class}, version = 2, exportSchema = false)
public abstract class NamazDatabase extends RoomDatabase {

    private static volatile NamazDatabase INSTANCE;

    public abstract RelationalRuleDao relationalRuleDao();

    public static NamazDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (NamazDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    NamazDatabase.class, "namaz_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
