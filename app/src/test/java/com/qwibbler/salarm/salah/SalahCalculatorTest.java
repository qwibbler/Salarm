package com.qwibbler.salarm.salah;

import static org.junit.Assert.assertNotNull;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.Madhab;
import com.batoulapps.adhan.PrayerTimes;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

public class SalahCalculatorTest {

    @Test
    public void testGetPrayerTimes() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JULY, 16);
        Date date = cal.getTime();
        double lat = 24.8607; // Karachi
        double lon = 67.0011;

        PrayerTimes times = SalahCalculator.getPrayerTimes(date, lat, lon, Madhab.HANAFI, CalculationMethod.KARACHI);

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

        SalahCalculator.SalahTimes times = SalahCalculator.calculateAllSalahTimes(date, lat, lon, CalculationMethod.KARACHI);

        assertNotNull(times);
        assertNotNull(times.fajr);
        assertNotNull(times.asrHanafi);
        assertNotNull(times.asrShafi);
    }

    @Test
    public void testEvaluateJumaWithIgnoreJumaDayCheck() {
        // Wednesday July 15, 2026
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JULY, 15);
        Date date = cal.getTime();

        MasjidEvent event = new MasjidEvent();
        event.salahType = "JUMA";
        event.definition = new RelationalDefinition();
        event.definition.isFixedTime = true;
        event.definition.fixedHour = 13;
        event.definition.fixedMinute = 30;

        SalahCalculator.SalahTimes times = SalahCalculator.calculateAllSalahTimes(date, 24.8607, 67.0011, CalculationMethod.KARACHI);

        long resultWithCheck = SalahCalculator.evaluateMasjidEvent(null, event, times, date, false);
        Assert.assertEquals(0, resultWithCheck);

        long resultIgnoreCheck = SalahCalculator.evaluateMasjidEvent(null, event, times, date, true);
        Assert.assertTrue(resultIgnoreCheck > 0);
    }
}
