package com.qwibbler.salarm.salah;

import static org.junit.Assert.assertEquals;

import com.batoulapps.adhan.CalculationMethod;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

public class SalahRuleTest {

    @Test
    public void testEvaluateTime_NoRounding() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JULY, 16, 12, 0, 0);
        Date baseDate = cal.getTime();

        double lat = 24.8607;
        double lon = 67.0011;
        SalahCalculator.SalahTimes times = SalahCalculator.calculateAllSalahTimes(baseDate, lat, lon, CalculationMethod.KARACHI);

        // Rule: Fajr + 10 minutes
        RelationalRule rule = new RelationalRule();
        rule.definition.anchor.basicEvent = RelationalRule.AnchorEvent.FAJR;
        rule.definition.calculation.offsetMinutes = 10;

        Date result = RelationalRule.evaluateTime(null, rule, times, baseDate);

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
        SalahCalculator.SalahTimes times = SalahCalculator.calculateAllSalahTimes(baseDate, lat, lon, CalculationMethod.KARACHI);

        // Rule: Fajr + 7 minutes, rounded UP to nearest 5
        RelationalRule rule = new RelationalRule();
        rule.definition.anchor.basicEvent = RelationalRule.AnchorEvent.FAJR;
        rule.definition.calculation.offsetMinutes = 7;
        rule.definition.calculation.roundingDirection = RelationalRule.RoundingDirection.UP;
        rule.definition.calculation.roundingIntervalMinutes = 5;

        Date result = RelationalRule.evaluateTime(null, rule, times, baseDate);

        long resultMillis = result.getTime();
        assertEquals(0, resultMillis % (5 * 60 * 1000));

        Calendar raw = Calendar.getInstance();
        raw.setTime(times.fajr);
        raw.add(Calendar.MINUTE, 7);
        long rawMillis = raw.getTimeInMillis();

        long expectedMillis = (long) Math.ceil((double) rawMillis / (5 * 60 * 1000)) * (5 * 60 * 1000);
        assertEquals(expectedMillis, resultMillis);
    }

    @Test
    public void testEvaluateTime_Capping() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JULY, 16, 12, 0, 0);
        Date baseDate = cal.getTime();

        double lat = 24.8607;
        double lon = 67.0011;
        SalahCalculator.SalahTimes times = SalahCalculator.calculateAllSalahTimes(baseDate, lat, lon, CalculationMethod.KARACHI);

        // Rule: Fajr + 30 minutes, but CAP at Fajr + 20 minutes (No later than)
        RelationalRule rule = new RelationalRule();
        rule.definition.anchor.basicEvent = RelationalRule.AnchorEvent.FAJR;
        rule.definition.calculation.offsetMinutes = 30;

        RelationalClip clip = new RelationalClip();
        clip.anchor.basicEvent = RelationalRule.AnchorEvent.FAJR;
        clip.offsetMinutes = 20;
        clip.boundaryType = RelationalRule.BoundaryType.NO_LATER_THAN;
        rule.definition.clip = clip;

        Date result = RelationalRule.evaluateTime(null, rule, times, baseDate);

        Calendar expected = Calendar.getInstance();
        expected.setTime(times.fajr);
        expected.add(Calendar.MINUTE, 20);

        assertEquals(expected.getTimeInMillis(), result.getTime());
    }

    @Test
    public void testEvaluateTime_UserScenario() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JULY, 16, 12, 0, 0);
        Date baseDate = cal.getTime();

        double lat = 24.8607;
        double lon = 67.0011;
        SalahCalculator.SalahTimes times = SalahCalculator.calculateAllSalahTimes(baseDate, lat, lon, CalculationMethod.KARACHI);

        // Rule: 60 min after Fajr
        // Cap: 60 min before Sunrise, NO_LATER_THAN
        RelationalRule rule = new RelationalRule();
        rule.definition.anchor.basicEvent = RelationalRule.AnchorEvent.FAJR;
        rule.definition.calculation.offsetMinutes = 60;

        RelationalClip clip = new RelationalClip();
        clip.anchor.basicEvent = RelationalRule.AnchorEvent.SUNRISE;
        clip.offsetMinutes = -60;
        clip.roundingDirection = RelationalRule.RoundingDirection.NEAREST;
        clip.boundaryType = RelationalRule.BoundaryType.NO_LATER_THAN;
        rule.definition.clip = clip;

        RelationalRule.EvaluationResult result = RelationalRule.evaluateTimeWithConflictCheck(null, rule, times, baseDate);

        long fajrMillis = times.fajr.getTime();
        long sunriseMillis = times.sunrise.getTime();

        long targetMillis = fajrMillis + (60 * 60 * 1000L);
        long clipMillis = sunriseMillis - (60 * 60 * 1000L);

        long expected = Math.min(targetMillis, clipMillis);

        assertEquals("Should be capped at the earlier time", expected, result.time());
    }
}
