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
import java.util.List;

/**
 * Helper class to calculate prayer times using the Adhan library.
 */
public class SalahCalculator {

    public static PrayerTimes getPrayerTimes(Date date, double latitude, double longitude, Madhab madhab, CalculationMethod method) {
        if (date == null) date = new Date();
        if (method == null) method = CalculationMethod.MUSLIM_WORLD_LEAGUE;
        if (madhab == null) madhab = Madhab.SHAFI;

        try {
            Coordinates coordinates = new Coordinates(latitude, longitude);
            DateComponents dateComponents = DateComponents.from(date);
            CalculationParameters parameters = method.getParameters();
            parameters.madhab = madhab;
            return new PrayerTimes(coordinates, dateComponents, parameters);
        } catch (Exception e) {
            return null;
        }
    }

    public static SalahTimes calculateAllSalahTimes(Date date, double latitude, double longitude, CalculationMethod method) {
        PrayerTimes hanafi = getPrayerTimes(date, latitude, longitude, Madhab.HANAFI, method);
        PrayerTimes shafi = getPrayerTimes(date, latitude, longitude, Madhab.SHAFI, method);
        return new SalahTimes(hanafi, shafi);
    }

    public static SalahTimes calculateAllSalahTimes(Context context, Date date, double latitude, double longitude, CalculationMethod method) {
        if (context != null && ((latitude == 0.0 && longitude == 0.0) || !MapPickerDialogFragment.isValidCoordinates(latitude, longitude))) {
            double[] coords = SalahUtils.getCoordinates(context);
            latitude = coords[0];
            longitude = coords[1];
        }
        if (context != null && method == null) {
            method = SalahUtils.getCalculationMethod(context);
        }
        SalahTimes times = calculateAllSalahTimes(date, latitude, longitude, method);
        if (context != null) {
            applyRounding(context, times);
        }
        return times;
    }

    private static void applyRounding(Context context, SalahTimes times) {
        if (times == null || context == null) return;
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
        long rounded = switch (method != null ? method : "") {
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
        if (masjid == null) return null;
        double lat;
        double lon;
        if (masjid.latitude != 0.0 || masjid.longitude != 0.0) {
            lat = masjid.latitude;
            lon = masjid.longitude;
        } else {
            SharedPreferences prefs = (context != null) ? SalarmApplication.getDefaultSharedPreferences(context) : null;
            String latStr = prefs != null ? prefs.getString(PreferencesKeys.KEY_SALAH_LATITUDE, "0.0") : "0.0";
            String lonStr = prefs != null ? prefs.getString(PreferencesKeys.KEY_SALAH_LONGITUDE, "0.0") : "0.0";
            try {
                lat = Double.parseDouble(latStr);
                lon = Double.parseDouble(lonStr);
            } catch (Exception e) {
                lat = 0.0;
                lon = 0.0;
            }
        }
        if ((lat == 0.0 && lon == 0.0) && context != null) {
            double[] fallback = SalahUtils.getCoordinates(context);
            lat = fallback[0];
            lon = fallback[1];
        }
        CalculationMethod method = masjid.getCalculationMethod(context);

        final double finalLat = lat;
        final double finalLon = lon;
        return getIntervalExtremeTime(date, masjid, event, isMax, d -> calculateAllSalahTimes(context, d, finalLat, finalLon, method));
    }

    public static Date getIntervalExtremeTime(Date date, Masjid masjid, RelationalRule.AnchorEvent event, boolean isMax, SalahTimesProvider timesProvider) {
        if (date == null || masjid == null || event == null || timesProvider == null) return null;
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

        Masjid.Interval targetInterval = null;
        List<Masjid.Interval> intervals = masjid.getIntervals();
        if (intervals != null) {
            for (Masjid.Interval interval : intervals) {
                if (interval != null && dayOfMonth >= interval.startDay && dayOfMonth <= interval.endDay) {
                    targetInterval = interval;
                    break;
                }
            }
        }

        if (targetInterval == null) {
            targetInterval = new Masjid.Interval(1, 31);
        }

        Date extremeTime = null;

        Calendar loopCal = (Calendar) cal.clone();
        int maxDayInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int endDay = Math.min(targetInterval.endDay, maxDayInMonth);

        RelationalRule.AnchorEvent targetEvent = event;
        if (event == RelationalRule.AnchorEvent.ASR_HANAFI || event == RelationalRule.AnchorEvent.ASR_SHAFI) {
            Madhab m = masjid.getMadhab(null);
            targetEvent = (m == Madhab.SHAFI) ? RelationalRule.AnchorEvent.ASR_SHAFI : RelationalRule.AnchorEvent.ASR_HANAFI;
        }

        for (int day = Math.max(1, targetInterval.startDay); day <= endDay; day++) {
            loopCal.set(Calendar.DAY_OF_MONTH, day);
            SalahTimes times = timesProvider.getTimes(loopCal.getTime());
            if (times == null) continue;

            Date currentTime = getEventTime(targetEvent, times);
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
        return evaluateDefinition(context, definition, times, date, null);
    }

    public static long evaluateDefinition(Context context, RelationalDefinition definition, SalahTimes times, Date date, Masjid masjid) {
        if (definition == null) return 0;
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

        if (definition.anchor == null || definition.calculation == null) return 0;

        long anchorTime = evaluateAnchor(context, definition.anchor, times, date, masjid);
        if (anchorTime <= 0) return 0;

        long targetMillis = applyCalculation(anchorTime,
                definition.calculation.offsetMinutes,
                definition.calculation.roundingDirection,
                definition.calculation.roundingIntervalMinutes,
                definition.calculation.adjustmentMinutes);

        if (definition.clip != null && definition.clip.anchor != null) {
            long clipAnchorTime = evaluateAnchor(context, definition.clip.anchor, times, date, masjid);
            if (clipAnchorTime > 0) {
                long clipMillis = applyCalculation(clipAnchorTime,
                        definition.clip.offsetMinutes,
                        definition.clip.roundingDirection,
                        definition.clip.roundingIntervalMinutes,
                        0);

                if (definition.clip.boundaryType != null) {
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
        }

        return targetMillis;
    }

    public static long evaluateAnchor(Context context, RelationalAnchor anchor, SalahTimes times, Date date) {
        return evaluateAnchor(context, anchor, times, date, null);
    }

    public static long evaluateAnchor(Context context, RelationalAnchor anchor, SalahTimes times, Date date, Masjid masjid) {
        if (anchor == null) return 0;
        if (anchor.masjidEventId != null && context != null) {
            MasjidEvent event = SalahDatabase.getDatabase(context).masjidEventDao().getById(anchor.masjidEventId);
            if (event != null) {
                return evaluateMasjidEvent(context, event, times, date);
            }
            return 0;
        }

        if (anchor.basicEvent != null) {
            RelationalRule.AnchorEvent targetEvent = anchor.basicEvent;
            if (masjid != null && (targetEvent == RelationalRule.AnchorEvent.ASR_HANAFI || targetEvent == RelationalRule.AnchorEvent.ASR_SHAFI)) {
                targetEvent = (masjid.getMadhab(context) == Madhab.SHAFI) ? RelationalRule.AnchorEvent.ASR_SHAFI : RelationalRule.AnchorEvent.ASR_HANAFI;
            }
            Date time = getEventTime(targetEvent, times);
            return time != null ? time.getTime() : 0;
        }

        return 0;
    }

    public static long evaluateMasjidEvent(Context context, MasjidEvent event, SalahTimes times, Date date) {
        return evaluateMasjidEvent(context, event, times, date, false);
    }

    public static long evaluateMasjidEvent(Context context, MasjidEvent event, SalahTimes times, Date date, boolean ignoreJumaDayCheck) {
        if (event == null || event.definition == null) return 0;
        try {
            Date evalDate = (date != null) ? date : new Date();
            if (!ignoreJumaDayCheck && "JUMA".equals(event.salahType)) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(evalDate);
                if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {
                    return 0;
                }
            }

            Masjid masjid = (context != null && event.masjidId > 0) ? SalahDatabase.getDatabase(context).masjidDao().getById(event.masjidId) : null;

            if (!event.definition.isFixedTime && event.definition.anchor != null && event.definition.anchor.basicEvent != null) {
                if (masjid != null) {
                    if (event.definition.anchor.basicEvent != RelationalRule.AnchorEvent.MAGHRIB
                            && !"MAGHRIB".equals(event.salahType)) {
                        Date extreme = getIntervalExtremeTime(context, evalDate, masjid, event.definition.anchor.basicEvent, event.definition.anchor.isMax);
                        if (extreme != null && event.definition.calculation != null) {
                            return applyCalculation(extreme.getTime(),
                                    event.definition.calculation.offsetMinutes,
                                    event.definition.calculation.roundingDirection,
                                    event.definition.calculation.roundingIntervalMinutes,
                                    event.definition.calculation.adjustmentMinutes);
                        }
                    }
                }
            }

            return evaluateDefinition(context, event.definition, times, evalDate, masjid);
        } catch (Exception ignored) {
            return 0;
        }
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
