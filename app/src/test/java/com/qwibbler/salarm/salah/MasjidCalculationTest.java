package com.qwibbler.salarm.salah;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MasjidCalculationTest {

    @Test
    public void testFebruaryEndInterval() {
        // Setup: February 2024 (Leap year, 29 days)
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.FEBRUARY, 28);
        Date date = cal.getTime();

        Masjid masjid = new Masjid();
        masjid.intervalsCount = 2;
        masjid.boundary1 = 15;
        // Interval 2: 16 to 31

        // Mock sunrise times:
        // Feb 28: 06:31
        // Feb 29: 06:30
        // Mar 1: 06:29 (Should NOT be picked)
        // Mar 31: 05:51 (Should NOT be picked)

        Map<Integer, Integer> sunriseMinutes = new HashMap<>();
        for (int i = 16; i <= 29; i++) sunriseMinutes.put(i, 390); // 6:30 default
        sunriseMinutes.put(28, 391); // 6:31
        sunriseMinutes.put(29, 390); // 6:30

        // If the bug exists, the loop goes to March 31.
        // We'll return an earlier sunrise for any date that is not in February.

        SalahCalculator.SalahTimesProvider provider = d -> {
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            SalahCalculator.SalahTimes times = new SalahCalculator.SalahTimes();

            if (c.get(Calendar.MONTH) == Calendar.FEBRUARY) {
                int day = c.get(Calendar.DAY_OF_MONTH);
                Integer mins = sunriseMinutes.get(day);
                if (mins == null) mins = 390;
                times.sunrise = createTime(c, mins);
            } else {
                // March or other
                times.sunrise = createTime(c, 320); // 5:20 - very early
            }
            return times;
        };

        Date extreme = SalahCalculator.getIntervalExtremeTime(date, masjid, RelationalRule.AnchorEvent.SUNRISE, false, provider);

        assert extreme != null;
        Calendar resultCal = Calendar.getInstance();
        resultCal.setTime(extreme);

        assertEquals("Should pick sunrise from Feb 29 (6:30)", 6, resultCal.get(Calendar.HOUR_OF_DAY));
        assertEquals("Should pick sunrise from Feb 29 (6:30)", 30, resultCal.get(Calendar.MINUTE));
    }

    @Test
    public void testNonLeapFebruaryEndInterval() {
        // Setup: February 2023 (Non-leap year, 28 days)
        Calendar cal = Calendar.getInstance();
        cal.set(2023, Calendar.FEBRUARY, 28);
        Date date = cal.getTime();

        Masjid masjid = new Masjid();
        masjid.intervalsCount = 2;
        masjid.boundary1 = 15;
        // Interval 2: 16 to 31

        SalahCalculator.SalahTimesProvider provider = d -> {
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            SalahCalculator.SalahTimes times = new SalahCalculator.SalahTimes();

            if (c.get(Calendar.MONTH) == Calendar.FEBRUARY) {
                int day = c.get(Calendar.DAY_OF_MONTH);
                if (day == 28) {
                    times.sunrise = createTime(c, 391); // 6:31
                } else {
                    times.sunrise = createTime(c, 400); // 6:40
                }
            } else {
                times.sunrise = createTime(c, 320); // 5:20
            }
            return times;
        };

        Date extreme = SalahCalculator.getIntervalExtremeTime(date, masjid, RelationalRule.AnchorEvent.SUNRISE, false, provider);

        assert extreme != null;
        Calendar resultCal = Calendar.getInstance();
        resultCal.setTime(extreme);

        assertEquals("Should pick sunrise from Feb 28 (6:31)", 6, resultCal.get(Calendar.HOUR_OF_DAY));
        assertEquals("Should pick sunrise from Feb 28 (6:31)", 31, resultCal.get(Calendar.MINUTE));
    }

    private Date createTime(Calendar base, int minutesSinceMidnight) {
        Calendar c = (Calendar) base.clone();
        c.set(Calendar.HOUR_OF_DAY, minutesSinceMidnight / 60);
        c.set(Calendar.MINUTE, minutesSinceMidnight % 60);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }
}
