package it.geosolutions.jaiext.utilities;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.Objects;

public final class ImageComparator {

    private ImageComparator() {
    }

    /**
     * Exact image comparison, no diffs allowed.
     */
    public static boolean imagesEqual(RenderedImage a, RenderedImage b) {
        // exact, no diffs allowed
        return imagesEqual(a, b, 0, 0.0);
    }

    /**
     * Image comparison with tolerance.
     *
     * @param maxDifferentPixels how many pixels may differ (default 0)
     * @param delta              numeric tolerance per sample (0.0 = exact).
     *                           Applied to FLOAT/DOUBLE; integers are exact compare.
     */
    public static boolean imagesEqual(RenderedImage a, RenderedImage b,
                                      int maxDifferentPixels, double delta) {
        Objects.requireNonNull(a, "left image is null");
        Objects.requireNonNull(b, "right image is null");

        // 1) bounds
        Rectangle ra = new Rectangle(a.getMinX(), a.getMinY(), a.getWidth(), a.getHeight());
        Rectangle rb = new Rectangle(b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight());
        if (!ra.equals(rb)) return false;

        // 2) bands + datatype
        SampleModel sma = a.getSampleModel();
        SampleModel smb = b.getSampleModel();
        if (sma.getNumBands() != smb.getNumBands()) return false;
        if (sma.getDataType() != smb.getDataType()) return false;

        final int dataType = sma.getDataType();
        final int bands = sma.getNumBands();
        final int width = ra.width;
        final int height = ra.height;
        final int minX = ra.x;
        final int minY = ra.y;

        final int STRIP_H = Math.max(1, Math.min(512, height));

        int differingPixels = 0;

        for (int y = 0; y < height; y += STRIP_H) {
            int h = Math.min(STRIP_H, height - y);
            Rectangle strip = new Rectangle(minX, minY + y, width, h);

            Raster rA = a.getData(strip);
            Raster rB = b.getData(strip);

            switch (dataType) {
                case DataBuffer.TYPE_BYTE:
                case DataBuffer.TYPE_USHORT:
                case DataBuffer.TYPE_SHORT:
                case DataBuffer.TYPE_INT: {
                    int[] pA = rA.getPixels(strip.x, strip.y, strip.width, strip.height, (int[]) null);
                    int[] pB = rB.getPixels(strip.x, strip.y, strip.width, strip.height, (int[]) null);
                    differingPixels += countDifferentPixelsInt(pA, pB, bands);
                    break;
                }
                case DataBuffer.TYPE_FLOAT: {
                    float[] pA = rA.getPixels(strip.x, strip.y, strip.width, strip.height, (float[]) null);
                    float[] pB = rB.getPixels(strip.x, strip.y, strip.width, strip.height, (float[]) null);
                    differingPixels += countDifferentPixelsFloat(pA, pB, bands, (float) delta);
                    break;
                }
                case DataBuffer.TYPE_DOUBLE: {
                    double[] pA = rA.getPixels(strip.x, strip.y, strip.width, strip.height, (double[]) null);
                    double[] pB = rB.getPixels(strip.x, strip.y, strip.width, strip.height, (double[]) null);
                    differingPixels += countDifferentPixelsDouble(pA, pB, bands, delta);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unsupported DataBuffer type: " + dataType);
            }

            if (differingPixels > maxDifferentPixels) {
                return false;
            }
        }

        return differingPixels <= maxDifferentPixels;
    }

    private static int countDifferentPixelsInt(int[] a, int[] b, int bands) {
        if (a == b) return 0;
        if (a == null || b == null || a.length != b.length) return Integer.MAX_VALUE;

        int diffs = 0;
        for (int i = 0; i < a.length; ) {
            boolean pixelDiff = false;
            for (int bnd = 0; bnd < bands; bnd++, i++) {
                if (a[i] != b[i]) pixelDiff = true;
            }
            if (pixelDiff) diffs++;
        }
        return diffs;
    }

    private static int countDifferentPixelsFloat(float[] a, float[] b, int bands, float eps) {
        if (a == b) return 0;
        if (a == null || b == null || a.length != b.length) return Integer.MAX_VALUE;

        final boolean exact = (eps == 0f);
        int diffs = 0;

        for (int i = 0; i < a.length; ) {
            boolean pixelDiff = false;
            for (int bnd = 0; bnd < bands; bnd++, i++) {
                float x = a[i], y = b[i];
                boolean same;
                if (exact) {
                    same = Float.floatToIntBits(x) == Float.floatToIntBits(y);
                } else {
                    if (Float.isNaN(x) && Float.isNaN(y)) {
                        same = true;
                    } else if (Float.isInfinite(x) || Float.isInfinite(y)) {
                        same = (x == y);
                    } else {
                        same = Math.abs(x - y) <= eps;
                    }
                }
                if (!same) pixelDiff = true;
            }
            if (pixelDiff) diffs++;
        }
        return diffs;
    }

    private static int countDifferentPixelsDouble(double[] a, double[] b, int bands, double eps) {
        if (a == b) return 0;
        if (a == null || b == null || a.length != b.length) return Integer.MAX_VALUE;

        final boolean exact = (eps == 0.0);
        int diffs = 0;

        for (int i = 0; i < a.length; ) {
            boolean pixelDiff = false;
            for (int bnd = 0; bnd < bands; bnd++, i++) {
                double x = a[i], y = b[i];
                boolean same;
                if (exact) {
                    // exact bit identity
                    same = Double.doubleToLongBits(x) == Double.doubleToLongBits(y);
                } else {
                    if (Double.isNaN(x) && Double.isNaN(y)) {
                        same = true;
                    } else if (Double.isInfinite(x) || Double.isInfinite(y)) {
                        same = (x == y);
                    } else {
                        same = Math.abs(x - y) <= eps;
                    }
                }
                if (!same) pixelDiff = true;
            }
            if (pixelDiff) diffs++;
        }
        return diffs;
    }
}
