package com.qwibbler.salarm.salah;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.Madhab;
import com.batoulapps.adhan.PrayerTimes;
import com.batoulapps.adhan.data.DateComponents;

import java.util.Date;

/**
 * Helper class to calculate prayer times using the Adhan library.
 */
public class SalahCalculator {

    public static PrayerTimes getPrayerTimes(Date date, double latitude, double longitude, Madhab madhab, CalculationMethod method) {
        Coordinates coordinates = new Coordinates(latitude, longitude);
        DateComponents dateComponents = DateComponents.from(date);
        com.batoulapps.adhan.CalculationParameters parameters = method.getParameters();
        parameters.madhab = madhab;
        return new PrayerTimes(coordinates, dateComponents, parameters);
    }

    public static SalahTimes calculateAllSalahTimes(Date date, double latitude, double longitude, CalculationMethod method) {
        PrayerTimes hanafi = getPrayerTimes(date, latitude, longitude, Madhab.HANAFI, method);
        PrayerTimes shafi = getPrayerTimes(date, latitude, longitude, Madhab.SHAFI, method);
        return new SalahTimes(hanafi, shafi);
    }

    public static class SalahTimes {
        public final Date fajr;
        public final Date sunrise;
        public final Date dhuhr;
        public final Date asrShafi;
        public final Date asrHanafi;
        public final Date maghrib;
        public final Date isha;

        public SalahTimes(PrayerTimes hanafi, PrayerTimes shafi) {
            this.fajr = hanafi.fajr;
            this.sunrise = hanafi.sunrise;
            this.dhuhr = hanafi.dhuhr;
            this.asrShafi = shafi.asr;
            this.asrHanafi = hanafi.asr;
            this.maghrib = hanafi.maghrib;
            this.isha = hanafi.isha;
        }
    }
}
