package com.qwibbler.salarm.salah;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.Madhab;
import com.qwibbler.salarm.R;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "masjids")
public class Masjid {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String name;
    public boolean showMasjidEvents;
    public int intervalsCount; // 2 or 3

    // Boundaries are stored as the last day of the interval.
    // Interval 1: 1 to boundary1
    // Interval 2: boundary1 + 1 to boundary2 (or 31 if 2 intervals)
    // Interval 3: boundary2 + 1 to 31 (if 3 intervals)
    public int boundary1;
    public int boundary2;

    public String iconUri;
    public double latitude;
    public double longitude;
    public String address;
    public String cityName;
    public String countryName;
    public String timezoneId;
    public String calcMethod;
    public String madhab;

    public Masjid() {
        this.showMasjidEvents = true;
        this.intervalsCount = 2;
        this.boundary1 = 16;
        this.boundary2 = 31;
    }


    public boolean fillMissingValuesWithDefaults(Context context) {
        boolean modified = false;

        if (name == null || name.trim().isEmpty()) {
            name = (context != null) ? context.getString(R.string.masjid_default_name) : "Masjid";
            modified = true;
        }

        if ((latitude == 0.0 && longitude == 0.0) || !MapPickerDialogFragment.isValidCoordinates(latitude, longitude)) {
            if (context != null) {
                double[] coords = SalahUtils.getCoordinates(context);
                latitude = coords[0];
                longitude = coords[1];
            }
            modified = true;
        }

        if (timezoneId == null || timezoneId.trim().isEmpty()) {
            SalahUtils.OfflineCity autoCity = (context != null)
                ? SalahUtils.findNearestOfflineCity(context, latitude, longitude)
                : null;
            timezoneId = (autoCity != null && autoCity.timezoneId != null) ? autoCity.timezoneId : SalahTimezoneUtils.getPhoneTimeZoneId();
            modified = true;
        }

        if (calcMethod == null || calcMethod.trim().isEmpty()) {
            calcMethod = (context != null) ? SalahUtils.getCalculationMethod(context).name() : CalculationMethod.MUSLIM_WORLD_LEAGUE.name();
            modified = true;
        }

        if (madhab == null || madhab.trim().isEmpty()) {
            madhab = (context != null) ? SalahUtils.getMadhab(context).name() : Madhab.SHAFI.name();
            modified = true;
        }

        if (intervalsCount != 2 && intervalsCount != 3) {
            intervalsCount = 2;
            boundary1 = 16;
            boundary2 = 31;
            modified = true;
        }

        return modified;
    }

    public CalculationMethod getCalculationMethod(Context context) {
        if (calcMethod != null && !calcMethod.trim().isEmpty()) {
            try {
                return CalculationMethod.valueOf(calcMethod.trim());
            } catch (IllegalArgumentException ignored) {}
        }
        CalculationMethod method = (context != null) ? SalahUtils.getCalculationMethod(context) : null;
        return method != null ? method : CalculationMethod.MUSLIM_WORLD_LEAGUE;
    }

    public Madhab getMadhab(Context context) {
        if (madhab != null && !madhab.trim().isEmpty()) {
            try {
                return Madhab.valueOf(madhab.trim());
            } catch (IllegalArgumentException ignored) {}
        }
        Madhab m = (context != null) ? SalahUtils.getMadhab(context) : null;
        return m != null ? m : Madhab.SHAFI;
    }

    public List<Interval> getIntervals() {
        List<Interval> intervals = new ArrayList<>();
        if (intervalsCount == 2) {
            intervals.add(new Interval(1, boundary1));
            intervals.add(new Interval(boundary1 + 1, 31));
        } else {
            intervals.add(new Interval(1, boundary1));
            intervals.add(new Interval(boundary1 + 1, boundary2));
            intervals.add(new Interval(boundary2 + 1, 31));
        }
        return intervals;
    }

    public String getIntervalsSummary(Context context) {
        List<Interval> intervals = getIntervals();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < intervals.size(); i++) {
            sb.append(intervals.get(i).getOrdinalSummary(context));
            if (i < intervals.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    public static class Interval {
        public int startDay;
        public int endDay;

        public Interval(int startDay, int endDay) {
            this.startDay = startDay;
            this.endDay = endDay;
        }

        @NonNull
        @Override
        public String toString() {
            return startDay + " to " + endDay;
        }

        public String getOrdinalSummary(Context context) {
            return context.getString(R.string.masjid_interval_range, getOrdinal(context, startDay), getOrdinal(context, endDay));
        }

        private String getOrdinal(Context context, int i) {
            int j = i % 10, k = i % 100;
            int resId;
            if (j == 1 && k != 11) resId = R.string.ordinal_1st;
            else if (j == 2 && k != 12) resId = R.string.ordinal_2nd;
            else if (j == 3 && k != 13) resId = R.string.ordinal_3rd;
            else resId = R.string.ordinal_th;
            return context.getString(resId, i);
        }
    }
}
