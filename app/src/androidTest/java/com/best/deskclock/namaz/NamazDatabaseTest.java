package com.best.deskclock.namaz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class NamazDatabaseTest {

    private NamazDatabase db;
    private RelationalRuleDao dao;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, NamazDatabase.class).build();
        dao = db.relationalRuleDao();
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void writeAndReadRule() {
        RelationalRule rule = new RelationalRule(RelationalRule.AnchorEvent.FAJR, 15, null, 0);
        rule.enabled = true;

        long id = dao.insert(rule);

        List<RelationalRule> rules = dao.getAll();
        assertNotNull(rules);
        assertEquals(1, rules.size());
        assertEquals(RelationalRule.AnchorEvent.FAJR, rules.get(0).anchorEvent);
        assertEquals(15, rules.get(0).offsetMinutes);
    }
}
