package com.best.deskclock.namaz;

import static org.junit.Assert.assertEquals;

import com.batoulapps.adhan.CalculationMethod;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

public class RelationalRuleTest {

    @Test
    public void testEvaluateTime_NoRounding() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JULY, 16, 12, 0, 0);
        Date baseDate = cal.getTime();

        // Mocking NamazTimes - we can just use a real calculation for a specific date
        double lat = 24.8607;
        double lon = 67.0011;
        NamazCalculator.NamazTimes times = NamazCalculator.calculateAllTimes(baseDate, lat, lon, CalculationMethod.KARACHI);

        // Rule: Fajr + 10 minutes
        RelationalRule rule = new RelationalRule(RelationalRule.AnchorEvent.FAJR, 10, null, 0);

        Date result = RelationalRule.evaluateTime(rule, times, baseDate);

        Calendar expected = Calendar.getInstance();
        expected.setTime(times.fajr);
        expected.add(Calendar.MINUTE, 10);

        assertEquals(expected.getTimeInMillis(), result.getTime());
    }

    @Test
    public void testEvaluateTime_RoundingUp() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JULY, 16, 12, 0, 0);
        Date baseDate = cal.getTime();

        double lat = 24.8607;
        double lon = 67.0011;
        NamazCalculator.NamazTimes times = NamazCalculator.calculateAllTimes(baseDate, lat, lon, CalculationMethod.KARACHI);

        // Rule: Fajr + 7 minutes, rounded UP to nearest 5
        // If Fajr is 04:12, +7 is 04:19. Rounded UP to 5 min interval is 04:20.
        RelationalRule rule = new RelationalRule(RelationalRule.AnchorEvent.FAJR, 7, RelationalRule.RoundingDirection.UP, 5);

        Date result = RelationalRule.evaluateTime(rule, times, baseDate);

        long resultMillis = result.getTime();
        assertEquals(0, resultMillis % (5 * 60 * 1000));

        Calendar raw = Calendar.getInstance();
        raw.setTime(times.fajr);
        raw.add(Calendar.MINUTE, 7);
        long rawMillis = raw.getTimeInMillis();

        // Manual verification of rounding logic
        long expectedMillis = (long) Math.ceil((double) rawMillis / (5 * 60 * 1000)) * (5 * 60 * 1000);
        assertEquals(expectedMillis, resultMillis);
    }
}
