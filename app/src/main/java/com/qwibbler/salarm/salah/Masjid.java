package com.qwibbler.salarm.salah;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

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

    public Masjid() {
        this.showMasjidEvents = true;
        this.intervalsCount = 2;
        this.boundary1 = 16;
        this.boundary2 = 31;
    }

    public static class Interval {
        public int startDay;
        public int endDay;

        public Interval(int startDay, int endDay) {
            this.startDay = startDay;
            this.endDay = endDay;
        }

        @Override
        public String toString() {
            return startDay + " to " + endDay;
        }

        public String getOrdinalSummary(android.content.Context context) {
            return context.getString(com.qwibbler.salarm.R.string.salah_masjid_interval_range, getOrdinal(context, startDay), getOrdinal(context, endDay));
        }

        private String getOrdinal(android.content.Context context, int i) {
            int j = i % 10, k = i % 100;
            int resId;
            if (j == 1 && k != 11) resId = com.qwibbler.salarm.R.string.ordinal_1st;
            else if (j == 2 && k != 12) resId = com.qwibbler.salarm.R.string.ordinal_2nd;
            else if (j == 3 && k != 13) resId = com.qwibbler.salarm.R.string.ordinal_3rd;
            else resId = com.qwibbler.salarm.R.string.ordinal_th;
            return context.getString(resId, i);
        }
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

    public String getIntervalsSummary(android.content.Context context) {
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
}
