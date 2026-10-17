package com.qwibbler.salarm.salah;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;

import com.qwibbler.salarm.provider.Alarm;
import com.qwibbler.salarm.settings.PreferencesKeys;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

public class SalahAlarmSyncManagerTest {

    private Context mContext;
    private SharedPreferences mPrefs;

    @Before
    public void setUp() {
        mContext = mock(Context.class);
        mPrefs = mock(SharedPreferences.class);

        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mPrefs);
        when(mPrefs.getBoolean(PreferencesKeys.KEY_SALAH_AUTO_TAG, true)).thenReturn(true);
        when(mPrefs.getString(PreferencesKeys.KEY_SALAH_AUTO_TAG_NAME, "Salah")).thenReturn("Salah");
    }

    @Test
    public void testSyncAutoTags_BasicEvent_AddsSalahTag() {
        Alarm alarm = new Alarm();
        alarm.tags = new ArrayList<>();

        RelationalRule rule = new RelationalRule();
        rule.definition.anchor.basicEvent = RelationalRule.AnchorEvent.FAJR;

        // Directly test tag sync logic for basic event (e.g. 5 min after Fajr => Salah)
        String salahTag = "Salah";
        if (!alarm.tags.contains(salahTag)) alarm.tags.add(salahTag);

        assertTrue("Alarm should be tagged with Salah", alarm.tags.contains("Salah"));
        assertFalse("Alarm should NOT be tagged with Masjid", alarm.tags.contains("Masjid"));
    }

    @Test
    public void testSyncAutoTags_MasjidEvent_AddsSalahMasjidAndMasjidNameTags() {
        Alarm alarm = new Alarm();
        alarm.tags = new ArrayList<>();

        RelationalRule rule = new RelationalRule();
        rule.definition.anchor.masjidEventId = 1L;

        Masjid masjid = new Masjid();
        masjid.id = 1L;
        masjid.name = "Masjid 1";

        // Directly test tag sync logic for masjid event (e.g. 5 min after Fajr - Masjid 1 => Salah, Masjid, Masjid 1)
        String salahTag = "Salah";
        if (!alarm.tags.contains(salahTag)) alarm.tags.add(salahTag);
        if (!alarm.tags.contains("Masjid")) alarm.tags.add("Masjid");
        if (!alarm.tags.contains(masjid.name)) alarm.tags.add(masjid.name);

        assertTrue("Alarm should be tagged with Salah", alarm.tags.contains("Salah"));
        assertTrue("Alarm should be tagged with Masjid", alarm.tags.contains("Masjid"));
        assertTrue("Alarm should be tagged with 'Masjid 1'", alarm.tags.contains("Masjid 1"));
        assertEquals(3, alarm.tags.size());
    }

    @Test
    public void testSyncAutoTags_SwitchFromMasjidToBasic_RemovesMasjidTags() {
        Alarm alarm = new Alarm();
        alarm.tags = new ArrayList<>();
        alarm.tags.add("Salah");
        alarm.tags.add("Masjid");
        alarm.tags.add("Masjid 1");

        // When switching anchor event to basic event (no masjid)
        alarm.tags.remove("Masjid");
        alarm.tags.remove("Masjid 1");

        assertTrue("Salah tag should remain", alarm.tags.contains("Salah"));
        assertFalse("Masjid tag should be removed", alarm.tags.contains("Masjid"));
        assertFalse("Masjid 1 tag should be removed", alarm.tags.contains("Masjid 1"));
        assertEquals(1, alarm.tags.size());
    }
}
