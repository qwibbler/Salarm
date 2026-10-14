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

    public static final float CARD_HEIGHT = 1000f;
    public static final float A4_HEIGHT = 1414.2f;
    public static final float CARD_WIDTH = 630.6f; // CR80 portrait aspect ratio
    public static final float A4_WIDTH = 1000f;
    private final Context context;

    public MasjidCardRenderer(Context context) {
        this.context = context;
    }

    public float getVirtualWidth(MasjidCardData data) {
        if (data == null || data.format == MasjidCardData.PaperFormat.CARD) {
            return CARD_WIDTH;
        } else if (data.format == MasjidCardData.PaperFormat.A4_PORTRAIT) {
            return A4_WIDTH;
        } else {
            return A4_HEIGHT;
        }
    }

    public float getVirtualHeight(MasjidCardData data) {
        if (data == null || data.format == MasjidCardData.PaperFormat.CARD) {
            return CARD_HEIGHT;
        } else if (data.format == MasjidCardData.PaperFormat.A4_PORTRAIT) {
            return A4_HEIGHT;
        } else {
            return A4_WIDTH;
        }
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
        if (data == null) return;
        if (data.format == MasjidCardData.PaperFormat.A4_PORTRAIT) {
            renderA4Portrait(target, data);
        } else if (data.format == MasjidCardData.PaperFormat.A4_LANDSCAPE) {
            renderA4Landscape(target, data);
        } else {
            renderCard(target, data);
        }
    }

    private void renderCard(DrawTarget target, MasjidCardData data) {
        // Colors - Black and White
        int colorText = Color.BLACK;
        int colorSecondaryText = 0xFF555555; // Dark gray
        int colorLine = Color.BLACK;
        int colorBorder = Color.BLACK;
        int colorBackground = Color.WHITE;

        // Draw background
        target.drawRect(0, 0, CARD_WIDTH, CARD_HEIGHT, 0, colorBackground, true);

        // Draw outer border
        target.drawRect(5, 5, CARD_WIDTH - 5, CARD_HEIGHT - 5, 2, colorBorder, false);

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
        float qrLeft = CARD_WIDTH - padding - qrSize;
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

        float headerHeight = 110f; // Fixed header height (from Y = 30f to Y = 170f)
        HeaderLayout headerLayout = measureAndFitHeader(
            data.masjidName != null ? data.masjidName : "",
            data.subtitle != null ? data.subtitle : "",
            availableWidth,
            headerHeight
        );

        float currentY;
        if (headerLayout.totalHeight <= iconSize) {
            currentY = headerTopY + (iconSize - headerLayout.totalHeight) / 2f;
        } else {
            currentY = headerTopY + (headerHeight - headerLayout.totalHeight) / 2f;
        }

        for (String line : headerLayout.titleLines) {
            currentY += headerLayout.titleFontSize;
            target.drawText(line, centerX, currentY, headerLayout.titleFontSize, Paint.Align.CENTER, colorText, true, 0);
            currentY += (headerLayout.titleLineSpacing - headerLayout.titleFontSize);
        }

        if (headerLayout.subtitleLines != null && !headerLayout.subtitleLines.isEmpty()) {
            currentY += 4f * (headerLayout.titleFontSize / 36f);
            for (String line : headerLayout.subtitleLines) {
                currentY += headerLayout.subtitleFontSize;
                target.drawText(line, centerX, currentY, headerLayout.subtitleFontSize, Paint.Align.CENTER, colorSecondaryText, false, 0);
                currentY += (headerLayout.subtitleLineSpacing - headerLayout.subtitleFontSize);
            }
        }

        int maxIntervals = 0;
        for (MasjidCardData.MonthGroup month : data.months) {
            maxIntervals = Math.max(maxIntervals, month.rows.size());
        }

        // Table Header
        float tableHeaderY = 180f;
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
        target.drawLine(padding, currentY, CARD_WIDTH - padding, currentY, 1.5f, colorLine);
        currentY += (maxIntervals > 2 ? 30f : 40f);

        // Table Area
        float monthNameX = 65f;
        float dayX = 105f;
        float rowHeight = maxIntervals > 2 ? 33f : 44f;
        float textSize = maxIntervals > 2 ? 27f : 35f;

        for (MasjidCardData.MonthGroup month : data.months) {
            float monthStartY = currentY - rowHeight + 10f;
            float monthEndY = monthStartY + month.rows.size() * rowHeight;

            target.drawText(month.monthName, monthNameX, (monthStartY + monthEndY) / 2f, textSize, Paint.Align.CENTER, colorText, true, -90);

            for (MasjidCardData.IntervalRow row : month.rows) {
                target.drawText(String.valueOf(row.day), dayX, currentY, textSize, Paint.Align.CENTER, colorText, false, 0);
                target.drawText(row.fajr, colX_fajr, currentY, textSize, Paint.Align.CENTER, colorText, false, 0);
                target.drawText(row.asr, colX_asr, currentY, textSize, Paint.Align.CENTER, colorText, false, 0);
                target.drawText(row.maghrib, colX_mgrb, currentY, textSize, Paint.Align.CENTER, colorText, false, 0);
                target.drawText(row.isha, colX_isha, currentY, textSize, Paint.Align.CENTER, colorText, false, 0);
                currentY += rowHeight;
            }

            target.drawLine(padding, currentY - (rowHeight / 2f), CARD_WIDTH - padding, currentY - (rowHeight / 2f), 1, colorLine);
            currentY += (rowHeight / 2f);
        }

        // Footer Area
        String footer = data.getFooterText();
        if (footer != null && !footer.trim().isEmpty()) {
            float footerMaxWidth = CARD_WIDTH - (padding * 2f);
            float footerBottomY = CARD_HEIGHT - padding; // 970f
            float footerTopY = Math.max(180f, currentY); // currentY after table
            float maxFooterHeight = Math.max(30f, footerBottomY - footerTopY);

            TextLayout footerLayout = measureAndFitFooter(footer.trim(), footerMaxWidth, maxFooterHeight);

            float footerY = footerTopY + (maxFooterHeight - footerLayout.totalHeight) / 2f;

            for (String line : footerLayout.lines) {
                footerY += footerLayout.fontSize;
                if (!line.isEmpty()) {
                    target.drawText(line, CARD_WIDTH / 2f, footerY, footerLayout.fontSize, Paint.Align.CENTER, colorText, false, 0);
                }
                footerY += (footerLayout.lineSpacing - footerLayout.fontSize);
            }
        }
    }

    private void renderA4Portrait(DrawTarget target, MasjidCardData data) {
        float width = 1000f;
        float height = 1414.2f;

        int colorText = Color.BLACK;
        int colorSecondaryText = 0xFF555555;
        int colorLine = Color.BLACK;
        int colorBorder = Color.BLACK;
        int colorBackground = Color.WHITE;

        target.drawRect(0, 0, width, height, 0, colorBackground, true);
        target.drawRect(15, 15, width - 15, height - 15, 2, colorBorder, false);

        float padding = 75f;
        float headerTopY = padding;

        // 1. Icon (Top Left)
        float iconSize = 90f;
        float iconLeft = padding;
        float iconTop = headerTopY;
        Bitmap iconBitmap = getIconBitmap(data);
        if (iconBitmap != null) {
            target.drawBitmap(iconBitmap, iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize);
        }

        // 2. QR Code (Top Right)
        float qrSize = 90f;
        float qrLeft = width - padding - qrSize;
        float qrTop = headerTopY;

        String qrText = (data.qrContent != null && !data.qrContent.trim().isEmpty())
            ? data.qrContent : (data.masjidName != null ? data.masjidName : "Salarm");

        drawQrCode(target, qrText, qrLeft, qrTop, qrSize, colorText);

        // 3. Header Title & Subtitle
        float gap = 20f;
        float titleLeftBound = iconLeft + iconSize + gap;
        float titleRightBound = qrLeft - gap;
        float availableWidth = titleRightBound - titleLeftBound;
        float centerX = width / 2f;

        float headerHeight = 130f;
        HeaderLayout headerLayout = measureAndFitHeader(
            data.masjidName != null ? data.masjidName : "",
            data.subtitle != null ? data.subtitle : "",
            availableWidth,
            headerHeight
        );

        float currentY = headerTopY + (headerHeight - headerLayout.totalHeight) / 2f;
        for (String line : headerLayout.titleLines) {
            currentY += headerLayout.titleFontSize;
            target.drawText(line, centerX, currentY, headerLayout.titleFontSize, Paint.Align.CENTER, colorText, true, 0);
            currentY += (headerLayout.titleLineSpacing - headerLayout.titleFontSize);
        }

        if (headerLayout.subtitleLines != null && !headerLayout.subtitleLines.isEmpty()) {
            currentY += 4f * (headerLayout.titleFontSize / 36f);
            for (String line : headerLayout.subtitleLines) {
                currentY += headerLayout.subtitleFontSize;
                target.drawText(line, centerX, currentY, headerLayout.subtitleFontSize, Paint.Align.CENTER, colorSecondaryText, false, 0);
                currentY += (headerLayout.subtitleLineSpacing - headerLayout.subtitleFontSize);
            }
        }

        // Table Header
        float tableHeaderY = headerTopY + headerHeight + 25f;
        float headerSize = 32f;
        float monthNameX = padding + 35f;
        float dayX = padding + 110f;
        float colX_fajr = padding + 265f;
        float colX_asr = padding + 425f;
        float colX_mgrb = padding + 585f;
        float colX_isha = padding + 745f;

        target.drawText("Fajr", colX_fajr, tableHeaderY, headerSize, Paint.Align.CENTER, colorText, true, 0);
        target.drawText("Asr", colX_asr, tableHeaderY, headerSize, Paint.Align.CENTER, colorText, true, 0);
        target.drawText("Mgrb", colX_mgrb, tableHeaderY, headerSize, Paint.Align.CENTER, colorText, true, 0);
        target.drawText("Isha", colX_isha, tableHeaderY, headerSize, Paint.Align.CENTER, colorText, true, 0);

        currentY = tableHeaderY + 18f;
        target.drawLine(padding, currentY, width - padding, currentY, 1.5f, colorLine);

        int totalRows = 0;
        for (MasjidCardData.MonthGroup month : data.months) {
            totalRows += month.rows.size();
        }
        float footerReservedHeight = 110f;
        float availableTableHeight = height - padding - footerReservedHeight - currentY;
        float numMonthDividers = Math.max(1, data.months.size());
        float rowHeight = Math.min(36f, Math.max(22f, (availableTableHeight - numMonthDividers * 10f) / Math.max(1, totalRows)));
        float textSize = Math.min(28f, Math.max(18f, rowHeight * 0.78f));

        currentY += rowHeight;

        for (MasjidCardData.MonthGroup month : data.months) {
            float monthStartY = currentY - rowHeight + 10f;
            float monthEndY = monthStartY + month.rows.size() * rowHeight;

            target.drawText(month.monthName, monthNameX, (monthStartY + monthEndY) / 2f, textSize, Paint.Align.CENTER, colorText, true, -90);

            for (MasjidCardData.IntervalRow row : month.rows) {
                target.drawText(String.valueOf(row.day), dayX, currentY, textSize, Paint.Align.CENTER, colorText, false, 0);
                target.drawText(row.fajr, colX_fajr, currentY, textSize, Paint.Align.CENTER, colorText, false, 0);
                target.drawText(row.asr, colX_asr, currentY, textSize, Paint.Align.CENTER, colorText, false, 0);
                target.drawText(row.maghrib, colX_mgrb, currentY, textSize, Paint.Align.CENTER, colorText, false, 0);
                target.drawText(row.isha, colX_isha, currentY, textSize, Paint.Align.CENTER, colorText, false, 0);
                currentY += rowHeight;
            }

            target.drawLine(padding, currentY - (rowHeight / 2f), width - padding, currentY - (rowHeight / 2f), 1f, colorLine);
            currentY += (rowHeight / 2f);
        }

        // Footer Area
        String footer = data.getFooterText();
        if (footer != null && !footer.trim().isEmpty()) {
            float footerMaxWidth = width - (padding * 2f);
            float footerBottomY = height - padding;
            float footerTopY = Math.max(height - footerReservedHeight - padding, currentY);
            float maxFooterHeight = Math.max(30f, footerBottomY - footerTopY);

            TextLayout footerLayout = measureAndFitFooter(footer.trim(), footerMaxWidth, maxFooterHeight);
            float footerY = footerTopY + (maxFooterHeight - footerLayout.totalHeight) / 2f;

            for (String line : footerLayout.lines) {
                footerY += footerLayout.fontSize;
                if (!line.isEmpty()) {
                    target.drawText(line, width / 2f, footerY, footerLayout.fontSize, Paint.Align.CENTER, colorText, false, 0);
                }
                footerY += (footerLayout.lineSpacing - footerLayout.fontSize);
            }
        }
    }

    private void renderA4Landscape(DrawTarget target, MasjidCardData data) {
        float width = 1414.2f;
        float height = 1000f;

        int colorText = Color.BLACK;
        int colorSecondaryText = 0xFF555555;
        int colorLine = Color.BLACK;
        int colorBorder = Color.BLACK;
        int colorBackground = Color.WHITE;

        target.drawRect(0, 0, width, height, 0, colorBackground, true);
        target.drawRect(15, 15, width - 15, height - 15, 2, colorBorder, false);

        float paddingX = 75f;
        float paddingY = 60f;
        float headerTopY = paddingY;

        // 1. Icon (Top Left)
        float iconSize = 80f;
        float iconLeft = paddingX;
        float iconTop = headerTopY;
        Bitmap iconBitmap = getIconBitmap(data);
        if (iconBitmap != null) {
            target.drawBitmap(iconBitmap, iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize);
        }

        // 2. QR Code (Top Right)
        float qrSize = 80f;
        float qrLeft = width - paddingX - qrSize;
        float qrTop = headerTopY;

        String qrText = (data.qrContent != null && !data.qrContent.trim().isEmpty())
            ? data.qrContent : (data.masjidName != null ? data.masjidName : "Salarm");

        drawQrCode(target, qrText, qrLeft, qrTop, qrSize, colorText);

        // 3. Header Title & Subtitle
        float gap = 20f;
        float titleLeftBound = iconLeft + iconSize + gap;
        float titleRightBound = qrLeft - gap;
        float availableWidth = titleRightBound - titleLeftBound;
        float centerX = width / 2f;

        float headerHeight = 110f;
        HeaderLayout headerLayout = measureAndFitHeader(
            data.masjidName != null ? data.masjidName : "",
            data.subtitle != null ? data.subtitle : "",
            availableWidth,
            headerHeight
        );

        float currentY = headerTopY + (headerHeight - headerLayout.totalHeight) / 2f;
        for (String line : headerLayout.titleLines) {
            currentY += headerLayout.titleFontSize;
            target.drawText(line, centerX, currentY, headerLayout.titleFontSize, Paint.Align.CENTER, colorText, true, 0);
            currentY += (headerLayout.titleLineSpacing - headerLayout.titleFontSize);
        }

        if (headerLayout.subtitleLines != null && !headerLayout.subtitleLines.isEmpty()) {
            currentY += 4f * (headerLayout.titleFontSize / 36f);
            for (String line : headerLayout.subtitleLines) {
                currentY += headerLayout.subtitleFontSize;
                target.drawText(line, centerX, currentY, headerLayout.subtitleFontSize, Paint.Align.CENTER, colorSecondaryText, false, 0);
                currentY += (headerLayout.subtitleLineSpacing - headerLayout.subtitleFontSize);
            }
        }

        // Side-by-side Tables: Left (Jan-Jun) and Right (Jul-Dec)
        float tableHeaderY = headerTopY + headerHeight + 20f;
        float headerSize = 28f;

        float tableGap = 25f;
        float leftTableStart = paddingX;
        float leftTableEnd = centerX - tableGap;
        float rightTableStart = centerX + tableGap;
        float rightTableEnd = width - paddingX;

        float L_monthNameX = leftTableStart + 35f;
        float L_dayX = leftTableStart + 105f;
        float L_fajr = leftTableStart + 215f;
        float L_asr = leftTableStart + 315f;
        float L_mgrb = leftTableStart + 415f;
        float L_isha = leftTableStart + 515f;

        float R_monthNameX = rightTableStart + 35f;
        float R_dayX = rightTableStart + 105f;
        float R_fajr = rightTableStart + 215f;
        float R_asr = rightTableStart + 315f;
        float R_mgrb = rightTableStart + 415f;
        float R_isha = rightTableStart + 515f;

        // Left Table Header
        target.drawText("Fajr", L_fajr, tableHeaderY, headerSize, Paint.Align.CENTER, colorText, true, 0);
        target.drawText("Asr", L_asr, tableHeaderY, headerSize, Paint.Align.CENTER, colorText, true, 0);
        target.drawText("Mgrb", L_mgrb, tableHeaderY, headerSize, Paint.Align.CENTER, colorText, true, 0);
        target.drawText("Isha", L_isha, tableHeaderY, headerSize, Paint.Align.CENTER, colorText, true, 0);

        // Right Table Header
        target.drawText("Fajr", R_fajr, tableHeaderY, headerSize, Paint.Align.CENTER, colorText, true, 0);
        target.drawText("Asr", R_asr, tableHeaderY, headerSize, Paint.Align.CENTER, colorText, true, 0);
        target.drawText("Mgrb", R_mgrb, tableHeaderY, headerSize, Paint.Align.CENTER, colorText, true, 0);
        target.drawText("Isha", R_isha, tableHeaderY, headerSize, Paint.Align.CENTER, colorText, true, 0);

        float headerLineY = tableHeaderY + 15f;
        target.drawLine(leftTableStart, headerLineY, leftTableEnd, headerLineY, 1.5f, colorLine);
        target.drawLine(rightTableStart, headerLineY, rightTableEnd, headerLineY, 1.5f, colorLine);

        int maxIntervals = 0;
        for (MasjidCardData.MonthGroup month : data.months) {
            maxIntervals = Math.max(maxIntervals, month.rows.size());
        }
        float rowHeight = maxIntervals > 2 ? 32f : 40f;
        float textSize = maxIntervals > 2 ? 25f : 31f;

        float tableStartY = headerLineY + (maxIntervals > 2 ? 28f : 36f);

        // Render Left Table (Months 0..5, e.g. Jan-Jun)
        float currentY_L = tableStartY;
        int leftLimit = Math.min(6, data.months.size());
        for (int i = 0; i < leftLimit; i++) {
            MasjidCardData.MonthGroup month = data.months.get(i);
            float monthStartY = currentY_L - rowHeight + 10f;
            float monthEndY = monthStartY + month.rows.size() * rowHeight;

            target.drawText(month.monthName, L_monthNameX, (monthStartY + monthEndY) / 2f, textSize, Paint.Align.CENTER, colorText, true, -90);

            for (MasjidCardData.IntervalRow row : month.rows) {
                target.drawText(String.valueOf(row.day), L_dayX, currentY_L, textSize, Paint.Align.CENTER, colorText, false, 0);
                target.drawText(row.fajr, L_fajr, currentY_L, textSize, Paint.Align.CENTER, colorText, false, 0);
                target.drawText(row.asr, L_asr, currentY_L, textSize, Paint.Align.CENTER, colorText, false, 0);
                target.drawText(row.maghrib, L_mgrb, currentY_L, textSize, Paint.Align.CENTER, colorText, false, 0);
                target.drawText(row.isha, L_isha, currentY_L, textSize, Paint.Align.CENTER, colorText, false, 0);
                currentY_L += rowHeight;
            }

            target.drawLine(leftTableStart, currentY_L - (rowHeight / 2f), leftTableEnd, currentY_L - (rowHeight / 2f), 1f, colorLine);
            currentY_L += (rowHeight / 2f);
        }

        // Render Right Table (Months 6..11, e.g. Jul-Dec)
        float currentY_R = tableStartY;
        int totalMonths = data.months.size();
        if (totalMonths > 6) {
            for (int i = 6; i < Math.min(12, totalMonths); i++) {
                MasjidCardData.MonthGroup month = data.months.get(i);
                float monthStartY = currentY_R - rowHeight + 10f;
                float monthEndY = monthStartY + month.rows.size() * rowHeight;

                target.drawText(month.monthName, R_monthNameX, (monthStartY + monthEndY) / 2f, textSize, Paint.Align.CENTER, colorText, true, -90);

                for (MasjidCardData.IntervalRow row : month.rows) {
                    target.drawText(String.valueOf(row.day), R_dayX, currentY_R, textSize, Paint.Align.CENTER, colorText, false, 0);
                    target.drawText(row.fajr, R_fajr, currentY_R, textSize, Paint.Align.CENTER, colorText, false, 0);
                    target.drawText(row.asr, R_asr, currentY_R, textSize, Paint.Align.CENTER, colorText, false, 0);
                    target.drawText(row.maghrib, R_mgrb, currentY_R, textSize, Paint.Align.CENTER, colorText, false, 0);
                    target.drawText(row.isha, R_isha, currentY_R, textSize, Paint.Align.CENTER, colorText, false, 0);
                    currentY_R += rowHeight;
                }

                target.drawLine(rightTableStart, currentY_R - (rowHeight / 2f), rightTableEnd, currentY_R - (rowHeight / 2f), 1f, colorLine);
                currentY_R += (rowHeight / 2f);
            }
        }

        float tableMaxY = Math.max(currentY_L, currentY_R);

        // Draw vertical divider between left and right tables
        target.drawLine(centerX, tableHeaderY - 10f, centerX, tableMaxY - 10f, 1f, colorLine);

        // Footer Area
        String footer = data.getFooterText();
        if (footer != null && !footer.trim().isEmpty()) {
            float footerMaxWidth = width - (paddingX * 2f);
            float footerBottomY = height - paddingY;
            float footerTopY = Math.max(tableMaxY + 10f, height - 85f);
            float maxFooterHeight = Math.max(30f, footerBottomY - footerTopY);

            TextLayout footerLayout = measureAndFitFooter(footer.trim(), footerMaxWidth, maxFooterHeight);
            float footerY = footerTopY + (maxFooterHeight - footerLayout.totalHeight) / 2f;

            for (String line : footerLayout.lines) {
                footerY += footerLayout.fontSize;
                if (!line.isEmpty()) {
                    target.drawText(line, centerX, footerY, footerLayout.fontSize, Paint.Align.CENTER, colorText, false, 0);
                }
                footerY += (footerLayout.lineSpacing - footerLayout.fontSize);
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

    private HeaderLayout measureAndFitHeader(String titleText, String subtitleText, float maxWidth, float maxHeight) {
        boolean hasSubtitle = subtitleText != null && !subtitleText.trim().isEmpty();

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

        Paint subPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));

        float initialTitleSize = 36f;
        float initialSubSize = 20f;
        float minTitleSize = 6f;

        for (float tSize = initialTitleSize; tSize >= minTitleSize; tSize -= 0.5f) {
            float scale = tSize / initialTitleSize;
            float sSize = hasSubtitle ? Math.max(5f, initialSubSize * scale) : 0f;

            titlePaint.setTextSize(tSize);
            float titleLineSpacing = tSize * 1.15f;
            List<String> tLines = wrapText(titleText, titlePaint, maxWidth);
            float titleHeight = tLines.size() * titleLineSpacing;

            List<String> sLines = new ArrayList<>();
            float subLineSpacing = 0f;
            float subHeight = 0f;
            float gap = 0f;

            if (hasSubtitle) {
                subPaint.setTextSize(sSize);
                subLineSpacing = sSize * 1.15f;
                sLines = wrapText(subtitleText.trim(), subPaint, maxWidth);
                subHeight = sLines.size() * subLineSpacing;
                gap = 4f * scale;
            }

            float totalHeight = titleHeight + gap + subHeight;
            if (totalHeight <= maxHeight) {
                return new HeaderLayout(tLines, tSize, titleLineSpacing, sLines, sSize, subLineSpacing, totalHeight);
            }
        }

        float tSize = minTitleSize;
        float scale = tSize / initialTitleSize;
        float sSize = hasSubtitle ? Math.max(5f, initialSubSize * scale) : 0f;

        titlePaint.setTextSize(tSize);
        float titleLineSpacing = tSize * 1.15f;
        List<String> tLines = wrapText(titleText, titlePaint, maxWidth);
        float titleHeight = tLines.size() * titleLineSpacing;

        List<String> sLines = new ArrayList<>();
        float subLineSpacing = 0f;
        float subHeight = 0f;
        float gap = 0f;

        if (hasSubtitle) {
            subPaint.setTextSize(sSize);
            subLineSpacing = sSize * 1.15f;
            sLines = wrapText(subtitleText.trim(), subPaint, maxWidth);
            subHeight = sLines.size() * subLineSpacing;
            gap = 4f * scale;
        }

        float totalHeight = titleHeight + gap + subHeight;
        return new HeaderLayout(tLines, tSize, titleLineSpacing, sLines, sSize, subLineSpacing, totalHeight);
    }

    private TextLayout measureAndFitFooter(String footer, float maxWidth, float maxHeight) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));

        float initialSize = 24f;
        float minSize = 6f;

        for (float currentSize = initialSize; currentSize >= minSize; currentSize -= 0.5f) {
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
            if (totalHeight <= maxHeight) {
                return new TextLayout(allWrappedLines, currentSize, lineSpacing, totalHeight);
            }
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
                    if (sb.length() > 0) currentLine.append(sb);
                }
            } else {
                String candidate = currentLine + " " + word;
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
                        if (sb.length() > 0) currentLine.append(sb);
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
        data.format = MasjidCardData.PaperFormat.CARD;
        data.footerTextFront = calculateDefaultFooter(masjid);
        data.footerTextBack = "";
        return data;
    }

    public void populatePrayerTimes(MasjidCardData data, Masjid masjid) {
        if (data == null) return;
        if (data.format == MasjidCardData.PaperFormat.CARD) {
            populatePrayerTimes(data, masjid, data.isFront);
        } else {
            populatePrayerTimesForMonths(data, masjid, 0, 11);
        }
    }

    public void populatePrayerTimes(MasjidCardData data, Masjid masjid, boolean front) {
        data.isFront = front;
        int startMonth = front ? 0 : 6;
        int endMonth = front ? 5 : 11;
        populatePrayerTimesForMonths(data, masjid, startMonth, endMonth);
    }

    private void populatePrayerTimesForMonths(MasjidCardData data, Masjid masjid, int startMonth, int endMonth) {
        data.months.clear();

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

    private static class HeaderLayout {
        final List<String> titleLines;
        final float titleFontSize;
        final float titleLineSpacing;
        final List<String> subtitleLines;
        final float subtitleFontSize;
        final float subtitleLineSpacing;
        final float totalHeight;

        HeaderLayout(List<String> titleLines, float titleFontSize, float titleLineSpacing,
                     List<String> subtitleLines, float subtitleFontSize, float subtitleLineSpacing,
                     float totalHeight) {
            this.titleLines = titleLines;
            this.titleFontSize = titleFontSize;
            this.titleLineSpacing = titleLineSpacing;
            this.subtitleLines = subtitleLines;
            this.subtitleFontSize = subtitleFontSize;
            this.subtitleLineSpacing = subtitleLineSpacing;
            this.totalHeight = totalHeight;
        }
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
        private Canvas canvas;
        private final Paint paint;
        private final RectF dstRect = new RectF();

        public CanvasDrawTarget() {
            this.paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            this.paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        }

        public CanvasDrawTarget(Canvas canvas, int width, int height, float virtualWidth, float virtualHeight) {
            this();
            setCanvas(canvas, width, height, virtualWidth, virtualHeight);
        }

        public void setCanvas(Canvas canvas, int width, int height, float virtualWidth, float virtualHeight) {
            this.canvas = canvas;
            float scale = Math.min(width / virtualWidth, height / virtualHeight);
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
            dstRect.set(left, top, right, bottom);
            canvas.drawBitmap(bitmap, null, dstRect, paint);
        }
    }
}
