package com.qwibbler.salarm.salah;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

public class MasjidCardData {

    public enum PaperFormat {
        CARD,
        A4_PORTRAIT,
        A4_LANDSCAPE
    }

    public String masjidName;
    public String subtitle;
    public String iconUri;
    public Bitmap iconBitmap;
    public String qrContent;
    public String footerTextFront;
    public String footerTextBack;
    public boolean isFront = true;
    public PaperFormat format = PaperFormat.CARD;
    public List<MonthGroup> months = new ArrayList<>();

    public String getFooterText() {
        if (format == PaperFormat.CARD) {
            return isFront ? footerTextFront : footerTextBack;
        }
        String front = footerTextFront != null ? footerTextFront.trim() : "";
        String back = footerTextBack != null ? footerTextBack.trim() : "";
        if (!front.isEmpty() && !back.isEmpty()) {
            return front + "\n" + back;
        } else if (!front.isEmpty()) {
            return front;
        } else {
            return back;
        }
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
