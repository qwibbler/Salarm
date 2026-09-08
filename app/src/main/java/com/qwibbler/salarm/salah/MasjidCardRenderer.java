package com.qwibbler.salarm.salah;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import com.batoulapps.adhan.CalculationMethod;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MasjidCardRenderer {

    public interface DrawTarget {
        void drawLine(float x1, float y1, float x2, float y2, float width, int color);
        void drawText(String text, float x, float y, float size, Paint.Align align, int color, boolean bold, float rotateDegrees);
        void drawRect(float left, float top, float right, float bottom, float strokeWidth, int color, boolean fill);
    }

    public static final float VIRTUAL_HEIGHT = 1000f;
    public static final float VIRTUAL_WIDTH = 630.6f; // CR80 portrait aspect ratio

    private final Context context;

    public MasjidCardRenderer(Context context) {
        this.context = context;
    }

    public void render(DrawTarget target, MasjidCardData data) {
        // Colors - Black and White
        int colorText = Color.BLACK;
        int colorSecondaryText = 0xFF555555; // Dark gray
        int colorLine = Color.BLACK;
        int colorBorder = Color.BLACK;
        int colorBackground = Color.WHITE;

        // Draw background
        target.drawRect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT, 0, colorBackground, true);

        // Draw outer border
        target.drawRect(5, 5, VIRTUAL_WIDTH - 5, VIRTUAL_HEIGHT - 5, 2, colorBorder, false);

        float padding = 40;
        boolean hasSubtitle = data.subtitle != null && !data.subtitle.isEmpty();

        // 1. Header Area (Top ~180px)
        float currentY = padding + 45;
        float titleSize = hasSubtitle ? 40 : 45;
        target.drawText(data.masjidName != null ? data.masjidName : "", VIRTUAL_WIDTH / 2, currentY, titleSize, Paint.Align.CENTER, colorText, true, 0);

        if (hasSubtitle) {
            currentY += 35;
            target.drawText(data.subtitle, VIRTUAL_WIDTH / 2, currentY, 26, Paint.Align.CENTER, colorSecondaryText, false, 0);
        }

        // QR Placeholder (Top Right)
        float qrSize = 75;
        target.drawRect(VIRTUAL_WIDTH - padding - qrSize, padding, VIRTUAL_WIDTH - padding, padding + qrSize, 1, colorSecondaryText, false);
        target.drawText("QR", VIRTUAL_WIDTH - padding - qrSize / 2, padding + qrSize / 2 + 7, 16, Paint.Align.CENTER, colorSecondaryText, false, 0);

        // Table Header
        currentY = hasSubtitle ? 165 : 145;
        float headerSize = 30;
        float colX_fajr = 250;
        float colX_asr = 350;
        float colX_mgrb = 450;
        float colX_isha = 550;

        target.drawText("Fajr", colX_fajr, currentY, headerSize, Paint.Align.CENTER, colorText, true, 0);
        target.drawText("Asr", colX_asr, currentY, headerSize, Paint.Align.CENTER, colorText, true, 0);
        target.drawText("Mgrb", colX_mgrb, currentY, headerSize, Paint.Align.CENTER, colorText, true, 0);
        target.drawText("Isha", colX_isha, currentY, headerSize, Paint.Align.CENTER, colorText, true, 0);

        currentY += 15;
        target.drawLine(padding, currentY, VIRTUAL_WIDTH - padding, currentY, 1.5f, colorLine);
        currentY += 40;

        // 2. Table Area (Middle ~700px)
        int maxIntervals = 0;
        for (MasjidCardData.MonthGroup month : data.months) {
            maxIntervals = Math.max(maxIntervals, month.rows.size());
        }

        float monthNameX = 65; // Moved right from 50
        float dayX = 105;     // Adjusted to stay clear of month name
        float rowHeight = maxIntervals > 2 ? 37 : 44;
        float textSize = maxIntervals > 2 ? 24 : 28;

        for (MasjidCardData.MonthGroup month : data.months) {
            float monthStartY = currentY - rowHeight + 10;
            float monthEndY = monthStartY + month.rows.size() * rowHeight;

            // Draw month name rotated
            target.drawText(month.monthName, monthNameX, (monthStartY + monthEndY) / 2, textSize, Paint.Align.CENTER, colorText, true, -90);

            for (MasjidCardData.IntervalRow row : month.rows) {
                target.drawText(String.valueOf(row.day), dayX, currentY, textSize, Paint.Align.CENTER, colorText, false, 0);
                target.drawText(row.fajr, colX_fajr, currentY, textSize, Paint.Align.CENTER, colorText, false, 0);
                target.drawText(row.asr, colX_asr, currentY, textSize, Paint.Align.CENTER, colorText, false, 0);
                target.drawText(row.maghrib, colX_mgrb, currentY, textSize, Paint.Align.CENTER, colorText, false, 0);
                target.drawText(row.isha, colX_isha, currentY, textSize, Paint.Align.CENTER, colorText, false, 0);
                currentY += rowHeight;
            }

            // Separator between months
            target.drawLine(padding, currentY - 10, VIRTUAL_WIDTH - padding, currentY - 10, 1, colorLine);
            currentY += (maxIntervals > 2 ? 15 : 20);
        }

        // 3. Footer Area (Bottom ~120px)
        String footer = data.getFooterText();
        if (footer != null && !footer.isEmpty()) {
            String[] lines = footer.split("\n");
            float footerY = VIRTUAL_HEIGHT - padding - (lines.length > 1 ? 55 : 25);
            for (String line : lines) {
                target.drawText(line.trim(), VIRTUAL_WIDTH / 2, footerY, 28, Paint.Align.CENTER, colorText, false, 0);
                footerY += 35;
            }
        }
    }

    public MasjidCardData generateData(Masjid masjid) {
        MasjidCardData data = new MasjidCardData();
        data.masjidName = masjid.name;
        data.subtitle = "";
        data.isFront = true;
        data.footerTextFront = calculateDefaultFooter(masjid);
        data.footerTextBack = "";
        return data;
    }

    public void populatePrayerTimes(MasjidCardData data, Masjid masjid, boolean front) {
        data.isFront = front;
        data.months.clear();

        int startMonth = front ? 0 : 6;
        int endMonth = front ? 5 : 11;

        double[] coords = SalahUtils.getCoordinates(context);
        CalculationMethod method = SalahUtils.getCalculationMethod(context);
        SalahDatabase db = SalahDatabase.getDatabase(context);
        MasjidEvent fajrEvent = db.masjidEventDao().getMasjidEvent(masjid.id, "FAJR");
        MasjidEvent asrEvent = db.masjidEventDao().getMasjidEvent(masjid.id, "ASR");
        MasjidEvent maghribEvent = db.masjidEventDao().getMasjidEvent(masjid.id, "MAGHRIB");
        MasjidEvent ishaEvent = db.masjidEventDao().getMasjidEvent(masjid.id, "ISHA");

        Calendar cal = Calendar.getInstance();
        for (int m = startMonth; m <= endMonth; m++) {
            cal.set(Calendar.MONTH, m);
            String monthName = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault());
            MasjidCardData.MonthGroup group = new MasjidCardData.MonthGroup(monthName);

            for (Masjid.Interval interval : masjid.getIntervals()) {
                cal.set(Calendar.DAY_OF_MONTH, interval.startDay);
                SalahCalculator.SalahTimes st = SalahCalculator.calculateAllSalahTimes(context, cal.getTime(), coords[0], coords[1], method);

                String fajr = (fajrEvent != null) ? formatTime(SalahCalculator.evaluateMasjidEvent(context, fajrEvent, st, cal.getTime())) : "--:--";
                String asr = (asrEvent != null) ? formatTime(SalahCalculator.evaluateMasjidEvent(context, asrEvent, st, cal.getTime())) : "--:--";
                String maghrib = (maghribEvent != null) ? formatTime(SalahCalculator.evaluateMasjidEvent(context, maghribEvent, st, cal.getTime())) : "--:--";
                String isha = (ishaEvent != null) ? formatTime(SalahCalculator.evaluateMasjidEvent(context, ishaEvent, st, cal.getTime())) : "--:--";

                group.rows.add(new MasjidCardData.IntervalRow(interval.startDay, fajr, asr, maghrib, isha));
            }
            data.months.add(group);
        }
    }

    private String formatTime(long millis) {
        if (millis <= 0) return "--:--";
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        int hour = cal.get(Calendar.HOUR);
        if (hour == 0) hour = 12;
        return String.format(Locale.getDefault(), "%d:%02d", hour, cal.get(Calendar.MINUTE));
    }

    private String calculateDefaultFooter(Masjid masjid) {
        SalahDatabase db = SalahDatabase.getDatabase(context);
        MasjidEvent dhuhrEvent = db.masjidEventDao().getMasjidEvent(masjid.id, "DHUHR");
        MasjidEvent jumaEvent = db.masjidEventDao().getMasjidEvent(masjid.id, "JUMA");

        if (dhuhrEvent == null || jumaEvent == null) return "";

        String dhuhrTime = formatTime(SalahCalculator.evaluateDefinition(context, dhuhrEvent.definition, null, new Date()));
        String jumaTime = formatTime(SalahCalculator.evaluateDefinition(context, jumaEvent.definition, null, new Date()));

        if (dhuhrTime.equals(jumaTime)) {
            return String.format("Duhr and Juma at %s", dhuhrTime);
        } else {
            return String.format("Duhr at %s, Juma at %s", dhuhrTime, jumaTime);
        }
    }

    public static class CanvasDrawTarget implements DrawTarget {
        private final Canvas canvas;
        private final Paint paint;
        private final float scale;

        public CanvasDrawTarget(Canvas canvas, int width, int height) {
            this.canvas = canvas;
            this.paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            this.paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            this.scale = Math.min(width / VIRTUAL_WIDTH, height / VIRTUAL_HEIGHT);
            canvas.scale(scale, scale);
        }

        @Override
        public void drawLine(float x1, float y1, float x2, float y2, float width, int color) {
            paint.setColor(color);
            paint.setStrokeWidth(width);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(x1, y1, x2, y2, paint);
        }

        @Override
        public void drawText(String text, float x, float y, float size, Paint.Align align, int color, boolean bold, float rotateDegrees) {
            if (text == null || text.isEmpty()) return;
            paint.setColor(color);
            paint.setTextSize(size);
            paint.setTextAlign(align);
            paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, bold ? Typeface.BOLD : Typeface.NORMAL));
            paint.setStyle(Paint.Style.FILL);

            if (rotateDegrees != 0) {
                canvas.save();
                canvas.rotate(rotateDegrees, x, y);
                canvas.drawText(text, x, y, paint);
                canvas.restore();
            } else {
                canvas.drawText(text, x, y, paint);
            }
        }

        @Override
        public void drawRect(float left, float top, float right, float bottom, float strokeWidth, int color, boolean fill) {
            paint.setColor(color);
            paint.setStrokeWidth(strokeWidth);
            paint.setStyle(fill ? Paint.Style.FILL : Paint.Style.STROKE);
            canvas.drawRect(left, top, right, bottom, paint);
        }
    }
}
