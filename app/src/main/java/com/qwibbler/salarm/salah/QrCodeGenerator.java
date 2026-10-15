package com.qwibbler.salarm.salah;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * QR Code generator delegating matrix generation to ZXing core library.
 */
public class QrCodeGenerator {

    public static boolean[][] encode(String content) {
        String safeContent = (content == null || content.isEmpty()) ? "Salarm" : content;

        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
            hints.put(EncodeHintType.MARGIN, 0);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(safeContent, BarcodeFormat.QR_CODE, 0, 0, hints);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            boolean[][] matrix = new boolean[height][width];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    matrix[y][x] = bitMatrix.get(x, y);
                }
            }

            return matrix;
        } catch (Exception e) {
            e.printStackTrace();
            return new boolean[0][0];
        }
    }
}
