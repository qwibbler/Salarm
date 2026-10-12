package com.qwibbler.salarm.salah;

import java.nio.charset.StandardCharsets;

/**
 * Pure Java ISO/IEC 18004 compliant QR Code generator.
 */
public class QrCodeGenerator {

    private static final int[] LOG = new int[256];
    private static final int[] EXP = new int[256];

    static {
        int x = 1;
        for (int i = 0; i < 255; i++) {
            EXP[i] = x;
            LOG[x] = i;
            x <<= 1;
            if ((x & 0x100) != 0) {
                x ^= 0x11D;
            }
        }
    }

    private static int gfMul(int x, int y) {
        if (x == 0 || y == 0) return 0;
        return EXP[(LOG[x] + LOG[y]) % 255];
    }

    public static boolean[][] encode(String content) {
        String safeContent = (content == null || content.isEmpty()) ? "Salarm" : content;
        byte[] dataBytes = safeContent.getBytes(StandardCharsets.UTF_8);

        int version;
        if (dataBytes.length <= 17) version = 1;
        else if (dataBytes.length <= 32) version = 2;
        else if (dataBytes.length <= 53) version = 3;
        else if (dataBytes.length <= 78) version = 4;
        else if (dataBytes.length <= 106) version = 5;
        else version = 6;

        return generateMatrix(dataBytes, version);
    }

    private static boolean[][] generateMatrix(byte[] dataBytes, int version) {
        int matrixSize = 17 + 4 * version;
        boolean[][] matrix = new boolean[matrixSize][matrixSize];
        boolean[][] isFunction = new boolean[matrixSize][matrixSize];

        drawFinderPattern(matrix, isFunction, 0, 0);
        drawFinderPattern(matrix, isFunction, matrixSize - 7, 0);
        drawFinderPattern(matrix, isFunction, 0, matrixSize - 7);

        if (version >= 2) {
            int alignPos = 10 + 4 * version;
            drawAlignmentPattern(matrix, isFunction, alignPos - 2, alignPos - 2);
        }

        for (int i = 0; i < matrixSize; i++) {
            setFunctionModule(matrix, isFunction, 6, i, i % 2 == 0);
            setFunctionModule(matrix, isFunction, i, 6, i % 2 == 0);
        }

        setFunctionModule(matrix, isFunction, 4 * version + 9, 8, true);

        for (int i = 0; i < 9; i++) {
            if (i != 6) {
                isFunction[8][i] = true;
                isFunction[i][8] = true;
            }
        }
        for (int i = 0; i < 8; i++) {
            isFunction[8][matrixSize - 1 - i] = true;
            isFunction[matrixSize - 1 - i][8] = true;
        }

        int[] dataCapacities = {0, 19, 34, 55, 80, 108, 136};
        int[] ecCapacities = {0, 7, 10, 15, 20, 26, 36};

        int dataCap = dataCapacities[version];
        int ecCap = ecCapacities[version];

        BitBuffer bits = new BitBuffer();
        bits.appendBits(0x4, 4);
        bits.appendBits(dataBytes.length, 8);
        for (byte b : dataBytes) {
            bits.appendBits(b & 0xFF, 8);
        }

        int maxDataBits = dataCap * 8;
        int termLen = Math.min(4, maxDataBits - bits.bitCount);
        if (termLen > 0) bits.appendBits(0, termLen);

        if (bits.bitCount % 8 != 0) {
            bits.appendBits(0, 8 - (bits.bitCount % 8));
        }

        byte[] padBytes = {(byte) 0xEC, (byte) 0x11};
        int padIdx = 0;
        while (bits.bitCount / 8 < dataCap) {
            bits.appendBits(padBytes[padIdx % 2] & 0xFF, 8);
            padIdx++;
        }

        byte[] finalData = bits.toByteArray(dataCap);
        byte[] ecBytes = calcReedSolomon(finalData, ecCap);

        byte[] allCodewords = new byte[dataCap + ecCap];
        System.arraycopy(finalData, 0, allCodewords, 0, dataCap);
        System.arraycopy(ecBytes, 0, allCodewords, dataCap, ecCap);

        int bitIdx = 0;
        int totalBits = allCodewords.length * 8;

        for (int right = matrixSize - 1; right > 0; right -= 2) {
            if (right == 6) right = 5;
            for (int vert = 0; vert < matrixSize; vert++) {
                for (int j = 0; j < 2; j++) {
                    int x = right - j;
                    boolean upward = ((right + 1) & 2) == 0;
                    int y = upward ? (matrixSize - 1 - vert) : vert;

                    if (!isFunction[y][x]) {
                        boolean bit = false;
                        if (bitIdx < totalBits) {
                            int bytePos = bitIdx / 8;
                            int bitPos = 7 - (bitIdx % 8);
                            bit = ((allCodewords[bytePos] >> bitPos) & 1) != 0;
                            bitIdx++;
                        }
                        boolean mask = ((y + x) % 2) == 0;
                        matrix[y][x] = bit ^ mask;
                    }
                }
            }
        }

        int formatInfo = 0x7784;
        for (int i = 0; i < 15; i++) {
            boolean bit = ((formatInfo >> i) & 1) != 0;
            if (i < 6) setModule(matrix, 8, i, bit);
            else if (i < 8) setModule(matrix, 8, i + 1, bit);
            else if (i == 8) setModule(matrix, 7, 8, bit);
            else setModule(matrix, 14 - i, 8, bit);

            if (i < 8) setModule(matrix, matrixSize - 1 - i, 8, bit);
            else setModule(matrix, 8, matrixSize - 15 + i, bit);
        }

        return matrix;
    }

    private static void setModule(boolean[][] matrix, int y, int x, boolean val) {
        matrix[y][x] = val;
    }

    private static void setFunctionModule(boolean[][] matrix, boolean[][] isFunction, int y, int x, boolean val) {
        matrix[y][x] = val;
        isFunction[y][x] = true;
    }

    private static void drawFinderPattern(boolean[][] matrix, boolean[][] isFunction, int r, int c) {
        for (int y = -1; y <= 7; y++) {
            for (int x = -1; x <= 7; x++) {
                int mr = r + y;
                int mc = c + x;
                if (mr >= 0 && mr < matrix.length && mc >= 0 && mc < matrix.length) {
                    boolean isBlack = (y >= 0 && y <= 6 && (x == 0 || x == 6))
                            || (x >= 0 && x <= 6 && (y == 0 || y == 6))
                            || (y >= 2 && y <= 4 && x >= 2 && x <= 4);
                    matrix[mr][mc] = isBlack;
                    isFunction[mr][mc] = true;
                }
            }
        }
    }

    private static void drawAlignmentPattern(boolean[][] matrix, boolean[][] isFunction, int r, int c) {
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                boolean isBlack = y == 0 || y == 4 || x == 0 || x == 4 || (y == 2 && x == 2);
                matrix[r + y][c + x] = isBlack;
                isFunction[r + y][c + x] = true;
            }
        }
    }

    private static byte[] calcReedSolomon(byte[] data, int ecCount) {
        int[] poly = new int[ecCount + 1];
        poly[0] = 1;
        for (int i = 0; i < ecCount; i++) {
            int root = EXP[i];
            for (int j = ecCount; j >= 1; j--) {
                poly[j] = poly[j] ^ gfMul(poly[j - 1], root);
            }
        }
        int[] res = new int[data.length + ecCount];
        for (int i = 0; i < data.length; i++) {
            res[i] = data[i] & 0xFF;
        }
        for (int i = 0; i < data.length; i++) {
            int coef = res[i];
            if (coef != 0) {
                for (int j = 0; j <= ecCount; j++) {
                    res[i + j] ^= gfMul(poly[j], coef);
                }
            }
        }
        byte[] ec = new byte[ecCount];
        for (int i = 0; i < ecCount; i++) {
            ec[i] = (byte) res[data.length + i];
        }
        return ec;
    }

    private static class BitBuffer {
        int bitCount = 0;
        byte[] buffer = new byte[256];

        void appendBits(int val, int len) {
            for (int i = len - 1; i >= 0; i--) {
                int bit = (val >> i) & 1;
                if (bit == 1) {
                    buffer[bitCount / 8] |= (byte) (1 << (7 - (bitCount % 8)));
                }
                bitCount++;
            }
        }

        byte[] toByteArray(int targetCap) {
            byte[] res = new byte[targetCap];
            System.arraycopy(buffer, 0, res, 0, Math.min(buffer.length, targetCap));
            return res;
        }
    }
}
