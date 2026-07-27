package com.qwibbler.salarm.namaz;

import static org.junit.Assert.assertNotNull;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.Madhab;
import com.batoulapps.adhan.PrayerTimes;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

public class NamazCalculatorTest {

    @Test
    public void testGetPrayerTimes() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JULY, 16);
        Date date = cal.getTime();
        double lat = 24.8607; // Karachi
        double lon = 67.0011;

        PrayerTimes times = NamazCalculator.getPrayerTimes(date, lat, lon, Madhab.HANAFI, CalculationMethod.KARACHI);

        assertNotNull(times);
        assertNotNull(times.fajr);
        assertNotNull(times.dhuhr);
        assertNotNull(times.asr);
        assertNotNull(times.maghrib);
        assertNotNull(times.isha);
    }

    @Test
    public void testCalculateAllTimes() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JULY, 16);
        Date date = cal.getTime();
        double lat = 24.8607;
        double lon = 67.0011;

        NamazCalculator.NamazTimes times = NamazCalculator.calculateAllTimes(date, lat, lon, CalculationMethod.KARACHI);

        assertNotNull(times);
        assertNotNull(times.fajr);
        assertNotNull(times.asrHanafi);
        assertNotNull(times.asrShafi);
    }
}
