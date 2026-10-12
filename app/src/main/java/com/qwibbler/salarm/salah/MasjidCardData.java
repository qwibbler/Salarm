package com.qwibbler.salarm.salah;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

public class MasjidCardData {
    public String masjidName;
    public String subtitle;
    public String iconUri;
    public Bitmap iconBitmap;
    public String qrContent;
    public String footerTextFront;
    public String footerTextBack;
    public boolean isFront;
    public List<MonthGroup> months = new ArrayList<>();

    public String getFooterText() {
        return isFront ? footerTextFront : footerTextBack;
    }

    public static class MonthGroup {
        public String monthName;
        public List<IntervalRow> rows = new ArrayList<>();

        public MonthGroup(String monthName) {
            this.monthName = monthName;
        }
    }

    public static class IntervalRow {
        public int day;
        public String fajr;
        public String asr;
        public String maghrib;
        public String isha;

        public IntervalRow(int day, String fajr, String asr, String maghrib, String isha) {
            this.day = day;
            this.fajr = fajr;
            this.asr = asr;
            this.maghrib = maghrib;
            this.isha = isha;
        }
    }
}
