package com.qwibbler.salarm.salah;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.util.Base64;
import android.widget.Toast;

import com.qwibbler.salarm.base.AppExecutors;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

public class MasjidCardExporter {

    private final Context context;
    private final MasjidCardRenderer renderer;

    public MasjidCardExporter(Context context, MasjidCardRenderer renderer) {
        this.context = context;
        this.renderer = renderer;
    }

    public void exportPng(MasjidCardData data, Uri uri) {
        int width = 1200; // High res
        int height = (int) (width * (MasjidCardRenderer.VIRTUAL_HEIGHT / MasjidCardRenderer.VIRTUAL_WIDTH));
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        MasjidCardRenderer.CanvasDrawTarget target = new MasjidCardRenderer.CanvasDrawTarget(canvas, width, height);
        renderer.render(target, data);

        try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
            if (out != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                AppExecutors.getMainThread().post(() ->
                    Toast.makeText(context, "Saved PNG successfully", Toast.LENGTH_SHORT).show());
            }
        } catch (IOException e) {
            e.printStackTrace();
            AppExecutors.getMainThread().post(() ->
                Toast.makeText(context, "Failed to save PNG", Toast.LENGTH_SHORT).show());
        }
    }

    public void exportPdf(Masjid masjid, MasjidCardData data, Uri uri) {
        PdfDocument document = new PdfDocument();

        // Front
        renderer.populatePrayerTimes(data, masjid, true);
        PdfDocument.PageInfo frontInfo = new PdfDocument.PageInfo.Builder((int) MasjidCardRenderer.VIRTUAL_WIDTH, (int) MasjidCardRenderer.VIRTUAL_HEIGHT, 1).create();
        PdfDocument.Page frontPage = document.startPage(frontInfo);
        MasjidCardRenderer.CanvasDrawTarget frontTarget = new MasjidCardRenderer.CanvasDrawTarget(frontPage.getCanvas(), (int) MasjidCardRenderer.VIRTUAL_WIDTH, (int) MasjidCardRenderer.VIRTUAL_HEIGHT);
        renderer.render(frontTarget, data);
        document.finishPage(frontPage);

        // Back
        renderer.populatePrayerTimes(data, masjid, false);
        PdfDocument.PageInfo backInfo = new PdfDocument.PageInfo.Builder((int) MasjidCardRenderer.VIRTUAL_WIDTH, (int) MasjidCardRenderer.VIRTUAL_HEIGHT, 2).create();
        PdfDocument.Page backPage = document.startPage(backInfo);
        MasjidCardRenderer.CanvasDrawTarget backTarget = new MasjidCardRenderer.CanvasDrawTarget(backPage.getCanvas(), (int) MasjidCardRenderer.VIRTUAL_WIDTH, (int) MasjidCardRenderer.VIRTUAL_HEIGHT);
        renderer.render(backTarget, data);
        document.finishPage(backPage);

        try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
            if (out != null) {
                document.writeTo(out);
                AppExecutors.getMainThread().post(() ->
                    Toast.makeText(context, "Saved PDF successfully", Toast.LENGTH_SHORT).show());
            }
        } catch (IOException e) {
            e.printStackTrace();
            AppExecutors.getMainThread().post(() ->
                Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show());
        } finally {
            document.close();
        }
    }

    public void exportSvg(MasjidCardData data, Uri uri) {
        SvgDrawTarget target = new SvgDrawTarget();
        renderer.render(target, data);
        String svg = target.getSvgString();

        try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
            if (out != null) {
                out.write(svg.getBytes());
                AppExecutors.getMainThread().post(() ->
                    Toast.makeText(context, "Saved SVG successfully", Toast.LENGTH_SHORT).show());
            }
        } catch (IOException e) {
            e.printStackTrace();
            AppExecutors.getMainThread().post(() ->
                Toast.makeText(context, "Failed to save SVG", Toast.LENGTH_SHORT).show());
        }
    }

    private static class SvgDrawTarget implements MasjidCardRenderer.DrawTarget {
        private final StringBuilder sb = new StringBuilder();

        public SvgDrawTarget() {
            sb.append(String.format(Locale.US, "<svg width=\"%.2f\" height=\"%.2f\" xmlns=\"http://www.w3.org/2000/svg\">\n",
                    MasjidCardRenderer.VIRTUAL_WIDTH, MasjidCardRenderer.VIRTUAL_HEIGHT));
            sb.append("<rect width=\"100%\" height=\"100%\" fill=\"white\"/>\n");
        }

        @Override
        public void drawLine(float x1, float y1, float x2, float y2, float width, int color) {
            sb.append(String.format(Locale.US, "<line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" stroke=\"%s\" stroke-width=\"%.2f\" />\n",
                    x1, y1, x2, y2, colorToHex(color), width));
        }

        @Override
        public void drawText(String text, float x, float y, float size, Paint.Align align, int color, boolean bold, float rotateDegrees) {
            String anchor = switch (align) {
                case LEFT -> "start";
                case CENTER -> "middle";
                case RIGHT -> "end";
            };
            String fontWeight = bold ? "font-weight=\"bold\"" : "";
            String transform = rotateDegrees != 0 ? String.format(Locale.US, " transform=\"rotate(%.2f, %.2f, %.2f)\"", rotateDegrees, x, y) : "";

            sb.append(String.format(Locale.US, "<text x=\"%.2f\" y=\"%.2f\" font-family=\"sans-serif\" font-size=\"%.2f\" text-anchor=\"%s\" fill=\"%s\" %s %s>%s</text>\n",
                    x, y, size, anchor, colorToHex(color), fontWeight, transform, escapeXml(text)));
        }

        @Override
        public void drawRect(float left, float top, float right, float bottom, float strokeWidth, int color, boolean fill) {
            String style = fill ? String.format(Locale.US, "fill=\"%s\"", colorToHex(color))
                              : String.format(Locale.US, "fill=\"none\" stroke=\"%s\" stroke-width=\"%.2f\"", colorToHex(color), strokeWidth);
            sb.append(String.format(Locale.US, "<rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" %s />\n",
                    left, top, right - left, bottom - top, style));
        }

        @Override
        public void drawBitmap(Bitmap bitmap, float left, float top, float right, float bottom) {
            if (bitmap == null) return;
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                String base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
                sb.append(String.format(Locale.US, "<image x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" href=\"data:image/png;base64,%s\" />\n",
                        left, top, right - left, bottom - top, base64));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public String getSvgString() {
            return sb + "</svg>";
        }

        private String colorToHex(int color) {
            return String.format("#%06X", (0xFFFFFF & color));
        }

        private String escapeXml(String text) {
            if (text == null) return "";
            return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
        }
    }
}
