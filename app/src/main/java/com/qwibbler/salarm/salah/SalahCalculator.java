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

    public interface SalahTimesProvider {
        SalahTimes getTimes(Date date);
    }

    public static Date getIntervalExtremeTime(Context context, Date date, Masjid masjid, RelationalRule.AnchorEvent event, boolean isMax) {
        SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(context);
        String latStr = prefs.getString(PreferencesKeys.KEY_SALAH_LATITUDE, "0.0");
        String lonStr = prefs.getString(PreferencesKeys.KEY_SALAH_LONGITUDE, "0.0");
        double lat = Double.parseDouble(latStr);
        double lon = Double.parseDouble(lonStr);
        CalculationMethod method = SalahUtils.getCalculationMethod(context);

        return getIntervalExtremeTime(date, masjid, event, isMax, d -> calculateAllSalahTimes(context, d, lat, lon, method));
    }

    public static Date getIntervalExtremeTime(Date date, Masjid masjid, RelationalRule.AnchorEvent event, boolean isMax, SalahTimesProvider timesProvider) {
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

        Date extremeTime = null;

        Calendar loopCal = (Calendar) cal.clone();
        int maxDayInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int endDay = Math.min(targetInterval.endDay, maxDayInMonth);

        for (int day = targetInterval.startDay; day <= endDay; day++) {
            loopCal.set(Calendar.DAY_OF_MONTH, day);
            SalahTimes times = timesProvider.getTimes(loopCal.getTime());
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

    public static long evaluateDefinition(Context context, RelationalDefinition definition, SalahTimes times, Date date) {
        if (definition.isFixedTime) {
            Calendar cal = Calendar.getInstance();
            if (date != null) {
                cal.setTime(date);
            }
            cal.set(Calendar.HOUR_OF_DAY, definition.fixedHour);
            cal.set(Calendar.MINUTE, definition.fixedMinute);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        }

        long anchorTime = evaluateAnchor(context, definition.anchor, times, date);
        if (anchorTime <= 0) return 0;

        long targetMillis = applyCalculation(anchorTime,
                definition.calculation.offsetMinutes,
                definition.calculation.roundingDirection,
                definition.calculation.roundingIntervalMinutes,
                definition.calculation.adjustmentMinutes);

        if (definition.clip != null) {
            long clipAnchorTime = evaluateAnchor(context, definition.clip.anchor, times, date);
            if (clipAnchorTime > 0) {
                long clipMillis = applyCalculation(clipAnchorTime,
                        definition.clip.offsetMinutes,
                        definition.clip.roundingDirection,
                        definition.clip.roundingIntervalMinutes,
                        0);

                switch (definition.clip.boundaryType) {
                    case NO_LATER_THAN -> {
                        if (targetMillis > clipMillis) targetMillis = clipMillis;
                    }
                    case NO_EARLIER_THAN -> {
                        if (targetMillis < clipMillis) targetMillis = clipMillis;
                    }
                }
            }
        }

        return targetMillis;
    }

    public static long evaluateAnchor(Context context, RelationalAnchor anchor, SalahTimes times, Date date) {
        if (anchor.masjidEventId != null && context != null) {
            MasjidEvent event = SalahDatabase.getDatabase(context).masjidEventDao().getById(anchor.masjidEventId);
            if (event != null) {
                return evaluateMasjidEvent(context, event, times, date);
            }
        }

        if (anchor.basicEvent != null) {
            Date time = getEventTime(anchor.basicEvent, times);
            return time != null ? time.getTime() : 0;
        }

        return 0;
    }

    public static long evaluateMasjidEvent(Context context, MasjidEvent event, SalahTimes times, Date date) {
        return evaluateMasjidEvent(context, event, times, date, false);
    }

    public static long evaluateMasjidEvent(Context context, MasjidEvent event, SalahTimes times, Date date, boolean ignoreJumaDayCheck) {
        if (!ignoreJumaDayCheck && "JUMA".equals(event.salahType)) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {
                return 0;
            }
        }

        if (!event.definition.isFixedTime && event.definition.anchor.basicEvent != null) {
            Masjid masjid = SalahDatabase.getDatabase(context).masjidDao().getById(event.masjidId);
            if (masjid != null) {
                Date extreme = getIntervalExtremeTime(context, date, masjid, event.definition.anchor.basicEvent, event.definition.anchor.isMax);
                if (extreme != null) {
                    return applyCalculation(extreme.getTime(),
                            event.definition.calculation.offsetMinutes,
                            event.definition.calculation.roundingDirection,
                            event.definition.calculation.roundingIntervalMinutes,
                            event.definition.calculation.adjustmentMinutes);
                }
            }
        }

        return evaluateDefinition(context, event.definition, times, date);
    }

    public static long applyCalculation(long baseMillis, int offsetMinutes, RelationalRule.RoundingDirection direction, int intervalMinutes, int adjustmentMinutes) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(baseMillis);
        cal.add(Calendar.MINUTE, offsetMinutes);

        long millis = cal.getTimeInMillis();
        if (intervalMinutes > 0 && direction != null) {
            long intervalMillis = intervalMinutes * 60 * 1000L;
            if (direction == RelationalRule.RoundingDirection.NEAREST) {
                millis = Math.round((double) millis / intervalMillis) * intervalMillis;
            } else if (direction == RelationalRule.RoundingDirection.UP) {
                millis = (long) Math.ceil((double) millis / intervalMillis) * intervalMillis;
            } else if (direction == RelationalRule.RoundingDirection.DOWN) {
                millis = (long) Math.floor((double) millis / intervalMillis) * intervalMillis;
            }
        }

        if (adjustmentMinutes != 0) {
            millis += adjustmentMinutes * 60 * 1000L;
        }

        return millis;
    }

    public static Date getEventTime(RelationalRule.AnchorEvent event, SalahTimes times) {
        return switch (event) {
            case FAJR -> times.fajr;
            case SUNRISE -> times.sunrise;
            case DHUHR -> times.dhuhr;
            case ASR_SHAFI -> times.asrShafi;
            case ASR_HANAFI -> times.asrHanafi;
            case MAGHRIB -> times.maghrib;
            case ISHA -> times.isha;
        };
    }

    public static class SalahTimes {
        public Date fajr;
        public Date sunrise;
        public Date dhuhr;
        public Date asrShafi;
        public Date asrHanafi;
        public Date maghrib;
        public Date isha;

        public SalahTimes() {}

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
