package com.qwibbler.salarm.salah;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.Madhab;
import com.batoulapps.adhan.PrayerTimes;
import com.batoulapps.adhan.data.DateComponents;

import android.content.Context;
import android.content.SharedPreferences;
import com.qwibbler.salarm.SalarmApplication;
import com.qwibbler.salarm.settings.PreferencesDefaultValues;
import com.qwibbler.salarm.settings.PreferencesKeys;

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

    public static SalahTimes calculateAllSalahTimes(Context context, Date date, double latitude, double longitude, CalculationMethod method) {
        SalahTimes times = calculateAllSalahTimes(date, latitude, longitude, method);
        applyRounding(context, times);
        return times;
    }

    private static void applyRounding(Context context, SalahTimes times) {
        SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(context);

        times.fajr = round(times.fajr, prefs.getString(PreferencesKeys.KEY_SALAH_ROUNDING_FAJR, PreferencesDefaultValues.DEFAULT_SALAH_ROUNDING_FAJR));
        times.sunrise = round(times.sunrise, prefs.getString(PreferencesKeys.KEY_SALAH_ROUNDING_SUNRISE, PreferencesDefaultValues.DEFAULT_SALAH_ROUNDING_SUNRISE));
        times.dhuhr = round(times.dhuhr, prefs.getString(PreferencesKeys.KEY_SALAH_ROUNDING_DHUHR, PreferencesDefaultValues.DEFAULT_SALAH_ROUNDING_DHUHR));
        times.asrShafi = round(times.asrShafi, prefs.getString(PreferencesKeys.KEY_SALAH_ROUNDING_ASR, PreferencesDefaultValues.DEFAULT_SALAH_ROUNDING_ASR));
        times.asrHanafi = round(times.asrHanafi, prefs.getString(PreferencesKeys.KEY_SALAH_ROUNDING_ASR, PreferencesDefaultValues.DEFAULT_SALAH_ROUNDING_ASR));
        times.maghrib = round(times.maghrib, prefs.getString(PreferencesKeys.KEY_SALAH_ROUNDING_MAGHRIB, PreferencesDefaultValues.DEFAULT_SALAH_ROUNDING_MAGHRIB));
        times.isha = round(times.isha, prefs.getString(PreferencesKeys.KEY_SALAH_ROUNDING_ISHA, PreferencesDefaultValues.DEFAULT_SALAH_ROUNDING_ISHA));
    }

    private static Date round(Date date, String method) {
        if (date == null) return null;
        long time = date.getTime();
        long rounded = switch (method) {
            case PreferencesDefaultValues.ROUNDING_UP -> (long) (Math.ceil(time / 60000.0) * 60000);
            case PreferencesDefaultValues.ROUNDING_DOWN -> (long) (Math.floor(time / 60000.0) * 60000);
            case PreferencesDefaultValues.ROUNDING_TO -> Math.round(time / 60000.0) * 60000;
            default -> Math.round(time / 60000.0) * 60000;
        };
        return new Date(rounded);
    }

    public static class SalahTimes {
        public Date fajr;
        public Date sunrise;
        public Date dhuhr;
        public Date asrShafi;
        public Date asrHanafi;
        public Date maghrib;
        public Date isha;

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
