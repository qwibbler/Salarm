package com.qwibbler.salarm.salah;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import androidx.core.content.ContextCompat;

import com.batoulapps.adhan.CalculationMethod;
import com.qwibbler.salarm.R;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MasjidCardRenderer {

    public static final float VIRTUAL_HEIGHT = 1000f;
    public static final float VIRTUAL_WIDTH = 630.6f; // CR80 portrait aspect ratio
    private final Context context;

    public MasjidCardRenderer(Context context) {
        this.context = context;
    }

    private static Bitmap getBitmapFromDrawable(Context context, int drawableId, int sizePx) {
        try {
            Drawable drawable = ContextCompat.getDrawable(context, drawableId);
            if (drawable == null) return null;
            Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, sizePx, sizePx);
            drawable.draw(canvas);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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

        float padding = 30f;
        float headerTopY = padding;

        // 1. Icon (Top Left)
        float iconSize = 70f;
        float iconLeft = padding;
        float iconTop = headerTopY;
        float iconRight = iconLeft + iconSize;
        float iconBottom = iconTop + iconSize;

        Bitmap iconBitmap = getIconBitmap(data);
        if (iconBitmap != null) {
            target.drawBitmap(iconBitmap, iconLeft, iconTop, iconRight, iconBottom);
        }

        // 2. QR Code (Top Right)
        float qrSize = 70f;
        float qrLeft = VIRTUAL_WIDTH - padding - qrSize;
        float qrTop = headerTopY;

        String qrText = (data.qrContent != null && !data.qrContent.trim().isEmpty())
            ? data.qrContent
            : (data.masjidName != null ? data.masjidName : "Salarm");

        drawQrCode(target, qrText, qrLeft, qrTop, qrSize, colorText);

        // 3. Title and Subtitle (Middle between Icon and QR Code)
        float gap = 15f;
        float titleLeftBound = iconRight + gap;
        float titleRightBound = qrLeft - gap;
        float availableWidth = titleRightBound - titleLeftBound;
        float centerX = (titleLeftBound + titleRightBound) / 2f;

        boolean hasSubtitle = data.subtitle != null && !data.subtitle.trim().isEmpty();

        float maxTitleHeight = hasSubtitle ? 85f : 110f;
        TextLayout titleLayout = measureAndWrapText(data.masjidName != null ? data.masjidName : "",
            availableWidth, maxTitleHeight, 3, 42f, 16f, true);

        TextLayout subtitleLayout = null;
        if (hasSubtitle) {
            subtitleLayout = measureAndWrapText(data.subtitle.trim(),
                availableWidth, 40f, 2, 22f, 14f, false);
        }

        float totalHeaderContentHeight = titleLayout.totalHeight + (hasSubtitle ? (6f + subtitleLayout.totalHeight) : 0f);
        float currentY = headerTopY + Math.max(0, (iconSize - totalHeaderContentHeight) / 2f) + titleLayout.fontSize;

        for (String line : titleLayout.lines) {
            target.drawText(line, centerX, currentY, titleLayout.fontSize, Paint.Align.CENTER, colorText, true, 0);
            currentY += titleLayout.lineSpacing;
        }

        if (hasSubtitle && subtitleLayout != null) {
            currentY += 2f;
            for (String line : subtitleLayout.lines) {
                target.drawText(line, centerX, currentY, subtitleLayout.fontSize, Paint.Align.CENTER, colorSecondaryText, false, 0);
                currentY += subtitleLayout.lineSpacing;
            }
        }

        int maxIntervals = 0;
        for (MasjidCardData.MonthGroup month : data.months) {
            maxIntervals = Math.max(maxIntervals, month.rows.size());
        }

        // Table Header (Dynamic Y to avoid overlap with header content or icon)
        float tableHeaderY = Math.max(150f, Math.max(iconBottom + 15f, currentY + 12f));
        float headerSize = 30f;
        float colX_fajr = 250f;
        float colX_asr = 350f;
        float colX_mgrb = 450f;
        float colX_isha = 550f;

        target.drawText("Fajr", colX_fajr, tableHeaderY, headerSize, Paint.Align.CENTER, colorText, true, 0);
        target.drawText("Asr", colX_asr, tableHeaderY, headerSize, Paint.Align.CENTER, colorText, true, 0);
        target.drawText("Mgrb", colX_mgrb, tableHeaderY, headerSize, Paint.Align.CENTER, colorText, true, 0);
        target.drawText("Isha", colX_isha, tableHeaderY, headerSize, Paint.Align.CENTER, colorText, true, 0);

        currentY = tableHeaderY + 15f;
        target.drawLine(padding, currentY, VIRTUAL_WIDTH - padding, currentY, 1.5f, colorLine);
        currentY += (maxIntervals > 2 ? 30f : 40f);

        // 2. Table Area (Middle ~700px)
        float monthNameX = 65f;
        float dayX = 105f;
        float rowHeight = maxIntervals > 2 ? 33f : 44f;
        float textSize = maxIntervals > 2 ? 27f : 35f;

        for (MasjidCardData.MonthGroup month : data.months) {
            float monthStartY = currentY - rowHeight + 10f;
            float monthEndY = monthStartY + month.rows.size() * rowHeight;

            // Draw month name rotated
            target.drawText(month.monthName, monthNameX, (monthStartY + monthEndY) / 2f, textSize, Paint.Align.CENTER, colorText, true, -90);

            for (MasjidCardData.IntervalRow row : month.rows) {
                target.drawText(String.valueOf(row.day), dayX, currentY, textSize, Paint.Align.CENTER, colorText, false, 0);
                target.drawText(row.fajr, colX_fajr, currentY, textSize, Paint.Align.CENTER, colorText, false, 0);
                target.drawText(row.asr, colX_asr, currentY, textSize, Paint.Align.CENTER, colorText, false, 0);
                target.drawText(row.maghrib, colX_mgrb, currentY, textSize, Paint.Align.CENTER, colorText, false, 0);
                target.drawText(row.isha, colX_isha, currentY, textSize, Paint.Align.CENTER, colorText, false, 0);
                currentY += rowHeight;
            }

            // Separator between months
            target.drawLine(padding, currentY - (rowHeight / 2f), VIRTUAL_WIDTH - padding, currentY - (rowHeight / 2f), 1, colorLine);
            currentY += (rowHeight / 2f);
        }

        // 3. Footer Area (Bottom ~120px)
        String footer = data.getFooterText();
        if (footer != null && !footer.trim().isEmpty()) {
            float footerMaxWidth = VIRTUAL_WIDTH - (padding * 2f);
            TextLayout footerLayout = measureAndWrapFooter(footer.trim(), footerMaxWidth, 85f, 4, 28f, 14f);

            float footerY = VIRTUAL_HEIGHT - padding - footerLayout.totalHeight + footerLayout.fontSize;
            for (String line : footerLayout.lines) {
                if (!line.isEmpty()) {
                    target.drawText(line, VIRTUAL_WIDTH / 2f, footerY, footerLayout.fontSize, Paint.Align.CENTER, colorText, false, 0);
                }
                footerY += footerLayout.lineSpacing;
            }
        }
    }

    private Bitmap getIconBitmap(MasjidCardData data) {
        if (data.iconBitmap != null) {
            return data.iconBitmap;
        }
        if (data.iconUri != null && !data.iconUri.isEmpty()) {
            try {
                Uri uri = Uri.parse(data.iconUri);
                InputStream is = context.getContentResolver().openInputStream(uri);
                if (is != null) {
                    Bitmap bm = BitmapFactory.decodeStream(is);
                    is.close();
                    if (bm != null) {
                        data.iconBitmap = bm;
                        return bm;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Bitmap defaultBm = getBitmapFromDrawable(context, R.drawable.ic_masjid_timings, 256);
        data.iconBitmap = defaultBm;
        return defaultBm;
    }

    private void drawQrCode(DrawTarget target, String text, float left, float top, float size, int color) {
        try {
            boolean[][] matrix = QrCodeGenerator.encode(text);
            int matrixSize = matrix.length;
            float moduleSize = size / matrixSize;

            for (int r = 0; r < matrixSize; r++) {
                for (int c = 0; c < matrixSize; c++) {
                    if (matrix[r][c]) {
                        float mLeft = left + c * moduleSize;
                        float mTop = top + r * moduleSize;
                        float mRight = (c == matrixSize - 1) ? (left + size) : (mLeft + moduleSize);
                        float mBottom = (r == matrixSize - 1) ? (top + size) : (mTop + moduleSize);
                        target.drawRect(mLeft, mTop, mRight, mBottom, 0, color, true);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            target.drawRect(left, top, left + size, top + size, 1, color, false);
            target.drawText("QR", left + size / 2f, top + size / 2f + 5f, 16f, Paint.Align.CENTER, color, false, 0);
        }
    }

    private TextLayout measureAndWrapText(String text, float maxWidth, float maxHeight, int maxLines,
                                          float initialSize, float minSize, boolean bold) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, bold ? Typeface.BOLD : Typeface.NORMAL));

        float currentSize = initialSize;
        while (currentSize >= minSize) {
            paint.setTextSize(currentSize);
            float lineSpacing = currentSize * 1.15f;
            List<String> lines = wrapText(text, paint, maxWidth);

            float totalHeight = lines.size() * lineSpacing;
            if (lines.size() <= maxLines && totalHeight <= maxHeight) {
                return new TextLayout(lines, currentSize, lineSpacing, totalHeight);
            }
            currentSize -= 1.0f;
        }

        paint.setTextSize(minSize);
        float lineSpacing = minSize * 1.15f;
        List<String> lines = wrapText(text, paint, maxWidth);

        if (lines.size() > maxLines) {
            List<String> truncated = new ArrayList<>(lines.subList(0, maxLines));
            String lastLine = truncated.get(maxLines - 1);
            while (!lastLine.isEmpty() && paint.measureText(lastLine + "...") > maxWidth) {
                lastLine = lastLine.substring(0, lastLine.length() - 1);
            }
            truncated.set(maxLines - 1, lastLine + "...");
            lines = truncated;
        }

        float totalHeight = lines.size() * lineSpacing;
        return new TextLayout(lines, minSize, lineSpacing, totalHeight);
    }

    private TextLayout measureAndWrapFooter(String footer, float maxWidth, float maxHeight, int maxLines,
                                            float initialSize, float minSize) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));

        float currentSize = initialSize;
        while (currentSize >= minSize) {
            paint.setTextSize(currentSize);
            float lineSpacing = currentSize * 1.2f;

            List<String> allWrappedLines = new ArrayList<>();
            String[] paragraphs = footer.split("\n");
            for (String para : paragraphs) {
                if (para.trim().isEmpty()) {
                    allWrappedLines.add("");
                } else {
                    allWrappedLines.addAll(wrapText(para.trim(), paint, maxWidth));
                }
            }

            float totalHeight = allWrappedLines.size() * lineSpacing;
            if (allWrappedLines.size() <= maxLines && totalHeight <= maxHeight) {
                return new TextLayout(allWrappedLines, currentSize, lineSpacing, totalHeight);
            }
            currentSize -= 1.0f;
        }

        paint.setTextSize(minSize);
        float lineSpacing = minSize * 1.2f;
        List<String> allWrappedLines = new ArrayList<>();
        String[] paragraphs = footer.split("\n");
        for (String para : paragraphs) {
            if (para.trim().isEmpty()) {
                allWrappedLines.add("");
            } else {
                allWrappedLines.addAll(wrapText(para.trim(), paint, maxWidth));
            }
        }

        if (allWrappedLines.size() > maxLines) {
            List<String> truncated = new ArrayList<>(allWrappedLines.subList(0, maxLines));
            String lastLine = truncated.get(maxLines - 1);
            while (!lastLine.isEmpty() && paint.measureText(lastLine + "...") > maxWidth) {
                lastLine = lastLine.substring(0, lastLine.length() - 1);
            }
            truncated.set(maxLines - 1, lastLine + "...");
            allWrappedLines = truncated;
        }

        float totalHeight = allWrappedLines.size() * lineSpacing;
        return new TextLayout(allWrappedLines, minSize, lineSpacing, totalHeight);
    }

    private List<String> wrapText(String text, Paint paint, float maxWidth) {
        List<String> result = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            result.add("");
            return result;
        }

        String[] words = text.trim().split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() == 0) {
                if (paint.measureText(word) <= maxWidth) {
                    currentLine.append(word);
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (char c : word.toCharArray()) {
                        if (paint.measureText(sb.toString() + c) <= maxWidth) {
                            sb.append(c);
                        } else {
                            if (sb.length() > 0) result.add(sb.toString());
                            sb = new StringBuilder();
                            sb.append(c);
                        }
                    }
                    if (sb.length() > 0) currentLine.append(sb.toString());
                }
            } else {
                String candidate = currentLine.toString() + " " + word;
                if (paint.measureText(candidate) <= maxWidth) {
                    currentLine.append(" ").append(word);
                } else {
                    result.add(currentLine.toString());
                    currentLine = new StringBuilder();
                    if (paint.measureText(word) <= maxWidth) {
                        currentLine.append(word);
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (char c : word.toCharArray()) {
                            if (paint.measureText(sb.toString() + c) <= maxWidth) {
                                sb.append(c);
                            } else {
                                if (sb.length() > 0) result.add(sb.toString());
                                sb = new StringBuilder();
                                sb.append(c);
                            }
                        }
                        if (sb.length() > 0) currentLine.append(sb.toString());
                    }
                }
            }
        }
        if (currentLine.length() > 0) {
            result.add(currentLine.toString());
        }
        return result;
    }

    public MasjidCardData generateData(Masjid masjid) {
        MasjidCardData data = new MasjidCardData();
        data.masjidName = masjid.name;

        if (masjid.address != null && !masjid.address.trim().isEmpty()) {
            data.subtitle = masjid.address.trim();
        } else if (masjid.cityName != null && !masjid.cityName.trim().isEmpty()) {
            if (masjid.countryName != null && !masjid.countryName.trim().isEmpty()) {
                data.subtitle = masjid.cityName.trim() + ", " + masjid.countryName.trim();
            } else {
                data.subtitle = masjid.cityName.trim();
            }
        } else {
            data.subtitle = "";
        }

        data.iconUri = masjid.iconUri;

        if (masjid.latitude != 0.0 || masjid.longitude != 0.0) {
            data.qrContent = String.format(Locale.US, "https://maps.google.com/?q=%.6f,%.6f", masjid.latitude, masjid.longitude);
        } else if (masjid.address != null && !masjid.address.trim().isEmpty()) {
            data.qrContent = "geo:0,0?q=" + Uri.encode(masjid.address.trim());
        } else if (masjid.name != null && !masjid.name.trim().isEmpty()) {
            data.qrContent = masjid.name.trim();
        } else {
            data.qrContent = "Salarm";
        }

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

        double[] coords;
        if (masjid != null && (masjid.latitude != 0.0 || masjid.longitude != 0.0)) {
            coords = new double[]{masjid.latitude, masjid.longitude};
        } else {
            coords = SalahUtils.getCoordinates(context);
        }
        CalculationMethod method = SalahUtils.getCalculationMethod(context);
        SalahDatabase db = SalahDatabase.getDatabase(context);
        assert masjid != null;
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

    public interface DrawTarget {
        void drawLine(float x1, float y1, float x2, float y2, float width, int color);

        void drawText(String text, float x, float y, float size, Paint.Align align, int color, boolean bold, float rotateDegrees);

        void drawRect(float left, float top, float right, float bottom, float strokeWidth, int color, boolean fill);

        void drawBitmap(Bitmap bitmap, float left, float top, float right, float bottom);
    }

    private static class TextLayout {
        final List<String> lines;
        final float fontSize;
        final float lineSpacing;
        final float totalHeight;

        TextLayout(List<String> lines, float fontSize, float lineSpacing, float totalHeight) {
            this.lines = lines;
            this.fontSize = fontSize;
            this.lineSpacing = lineSpacing;
            this.totalHeight = totalHeight;
        }
    }

    public static class CanvasDrawTarget implements DrawTarget {
        private final Canvas canvas;
        private final Paint paint;

        public CanvasDrawTarget(Canvas canvas, int width, int height) {
            this.canvas = canvas;
            this.paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            this.paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            float scale = Math.min(width / VIRTUAL_WIDTH, height / VIRTUAL_HEIGHT);
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

        @Override
        public void drawBitmap(Bitmap bitmap, float left, float top, float right, float bottom) {
            if (bitmap == null) return;
            RectF dst = new RectF(left, top, right, bottom);
            canvas.drawBitmap(bitmap, null, dst, paint);
        }
    }
}
