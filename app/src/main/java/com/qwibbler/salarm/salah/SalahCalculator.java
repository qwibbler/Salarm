package com.qwibbler.salarm.salah;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.CalculationParameters;
import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.Madhab;
import com.batoulapps.adhan.PrayerTimes;
import com.batoulapps.adhan.data.DateComponents;

import android.content.Context;
import android.content.SharedPreferences;
import com.qwibbler.salarm.SalarmApplication;
import com.qwibbler.salarm.settings.PreferencesDefaultValues;
import com.qwibbler.salarm.settings.PreferencesKeys;

import java.util.Calendar;
import java.util.Date;

/**
 * Helper class to calculate prayer times using the Adhan library.
 */
public class SalahCalculator {

    public static PrayerTimes getPrayerTimes(Date date, double latitude, double longitude, Madhab madhab, CalculationMethod method) {
        Coordinates coordinates = new Coordinates(latitude, longitude);
        DateComponents dateComponents = DateComponents.from(date);
        CalculationParameters parameters = method.getParameters();
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

    public static Date getIntervalExtremeTime(Context context, Date date, Masjid masjid, RelationalRule.AnchorEvent event, boolean isMax) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

        Masjid.Interval targetInterval = null;
        for (Masjid.Interval interval : masjid.getIntervals()) {
            if (dayOfMonth >= interval.startDay && dayOfMonth <= interval.endDay) {
                targetInterval = interval;
                break;
            }
        }

        if (targetInterval == null) return null;

        SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(context);
        String latStr = prefs.getString(PreferencesKeys.KEY_SALAH_LATITUDE, "0.0");
        String lonStr = prefs.getString(PreferencesKeys.KEY_SALAH_LONGITUDE, "0.0");
        double lat = Double.parseDouble(latStr);
        double lon = Double.parseDouble(lonStr);
        CalculationMethod method = SalahUtils.getCalculationMethod(context);

        Date extremeTime = null;

        Calendar loopCal = (Calendar) cal.clone();
        for (int day = targetInterval.startDay; day <= targetInterval.endDay; day++) {
            loopCal.set(Calendar.DAY_OF_MONTH, day);
            SalahTimes times = calculateAllSalahTimes(context, loopCal.getTime(), lat, lon, method);
            Date currentTime = switch (event) {
                case FAJR -> times.fajr;
                case SUNRISE -> times.sunrise;
                case DHUHR -> times.dhuhr;
                case ASR_SHAFI -> times.asrShafi;
                case ASR_HANAFI -> times.asrHanafi;
                case MAGHRIB -> times.maghrib;
                case ISHA -> times.isha;
            };

            if (currentTime == null) continue;

            if (extremeTime == null) {
                extremeTime = currentTime;
            } else {
                if (isMax) {
                    if (getTimeOfDay(currentTime) > getTimeOfDay(extremeTime)) {
                        extremeTime = currentTime;
                    }
                } else {
                    if (getTimeOfDay(currentTime) < getTimeOfDay(extremeTime)) {
                        extremeTime = currentTime;
                    }
                }
            }
        }

        // Return the extreme time but adjusted to the target date's year/month/day
        if (extremeTime != null) {
            Calendar resultCal = Calendar.getInstance();
            resultCal.setTime(date);
            Calendar extremeCal = Calendar.getInstance();
            extremeCal.setTime(extremeTime);
            resultCal.set(Calendar.HOUR_OF_DAY, extremeCal.get(Calendar.HOUR_OF_DAY));
            resultCal.set(Calendar.MINUTE, extremeCal.get(Calendar.MINUTE));
            resultCal.set(Calendar.SECOND, extremeCal.get(Calendar.SECOND));
            resultCal.set(Calendar.MILLISECOND, extremeCal.get(Calendar.MILLISECOND));
            return resultCal.getTime();
        }

        return null;
    }

    private static long getTimeOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.HOUR_OF_DAY) * 3600000L +
               cal.get(Calendar.MINUTE) * 60000L +
               cal.get(Calendar.SECOND) * 1000L +
               cal.get(Calendar.MILLISECOND);
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
