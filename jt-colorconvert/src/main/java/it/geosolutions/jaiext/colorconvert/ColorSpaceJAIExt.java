package it.geosolutions.jaiext.colorconvert;

import it.geosolutions.jaiext.range.Range;

import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import javax.media.jai.ColorSpaceJAI;
import javax.media.jai.IHSColorSpace;
import javax.media.jai.ROI;

public abstract class ColorSpaceJAIExt extends ColorSpaceJAI {

    /** Cache the power value for XYZ to RGB */
    public static final double POWER1 = 1.0 / 2.4;

    protected ColorSpaceJAIExt(int type, int numComponents, boolean isRGBPreferredIntermediary) {
        super(type, numComponents, isRGBPreferredIntermediary);
    }

    public abstract WritableRaster fromCIEXYZ(Raster src, int[] srcComponentSize,
            WritableRaster dest, int[] dstComponentSize, ROI roi, Range nodata, float[] destNodata);

    public abstract WritableRaster fromRGB(Raster src, int[] srcComponentSize, WritableRaster dest,
            int[] dstComponentSize, ROI roi, Range nodata, float[] destNodata);

    public abstract WritableRaster toCIEXYZ(Raster src, int[] srcComponentSize,
            WritableRaster dest, int[] dstComponentSize, ROI roi, Range nodata, float[] destNodata);

    public abstract WritableRaster toRGB(Raster src, int[] srcComponentSize, WritableRaster dest,
            int[] dstComponentSize, ROI roi, Range nodata, float[] destNodata);

    @Override
    public WritableRaster fromCIEXYZ(Raster src, int[] srcComponentSize, WritableRaster dest,
            int[] dstComponentSize) {
        return fromCIEXYZ(src, srcComponentSize, dest, dstComponentSize, null, null, null);
    }

    @Override
    public WritableRaster fromRGB(Raster src, int[] srcComponentSize, WritableRaster dest,
            int[] dstComponentSize) {
        return fromRGB(src, srcComponentSize, dest, dstComponentSize, null, null, null);
    }

    @Override
    public WritableRaster toCIEXYZ(Raster src, int[] srcComponentSize, WritableRaster dest,
            int[] dstComponentSize) {
        return toCIEXYZ(src, srcComponentSize, dest, dstComponentSize, null, null, null);
    }

    @Override
    public WritableRaster toRGB(Raster src, int[] srcComponentSize, WritableRaster dest,
            int[] dstComponentSize) {
        return toRGB(src, srcComponentSize, dest, dstComponentSize, null, null, null);
    }

    public static void RGB2XYZ(float[] RGB, float[] XYZ) {
        for (int i = 0; i < 3; i++) {
            if (RGB[i] < 0.040449936F)
                RGB[i] /= 12.92F;
            else
                RGB[i] = (float) (Math.pow((RGB[i] + 0.055) / 1.055, 2.4));
        }

        XYZ[0] = 0.45593763F * RGB[0] + 0.39533819F * RGB[1] + 0.19954964F * RGB[2];
        XYZ[1] = 0.23157515F * RGB[0] + 0.77905262F * RGB[1] + 0.07864978F * RGB[2];
        XYZ[2] = 0.01593493F * RGB[0] + 0.09841772F * RGB[1] + 0.78488615F * RGB[2];
    }

    public static void XYZ2RGB(float[] XYZ, float[] RGB) {
        RGB[0] = 2.9311227F * XYZ[0] - 1.4111496F * XYZ[1] - 0.6038046F * XYZ[2];
        RGB[1] = -0.87637005F * XYZ[0] + 1.7219844F * XYZ[1] + 0.0502565F * XYZ[2];
        RGB[2] = 0.05038065F * XYZ[0] - 0.187272F * XYZ[1] + 1.280027F * XYZ[2];

        for (int i = 0; i < 3; i++) {
            float v = RGB[i];

            if (v < 0.0F)
                v = 0.0F;

            if (v < 0.0031308F)
                RGB[i] = 12.92F * v;
            else {
                if (v > 1.0F)
                    v = 1.0F;

                RGB[i] = (float) (1.055 * Math.pow(v, POWER1) - 0.055);
            }
        }
    }

    public static void convertToSigned(double[] buf, int dataType) {
        if (dataType == DataBuffer.TYPE_SHORT) {
            for (int i = 0; i < buf.length; i++) {
                short temp = (short) (((int) buf[i]) & 0xFFFF);
                buf[i] = temp;
            }
        } else if (dataType == DataBuffer.TYPE_INT) {
            for (int i = 0; i < buf.length; i++) {
                int temp = (int) (((long) buf[i]) & 0xFFFFFFFFl);
                buf[i] = temp;
            }
        }
    }

    public static ColorSpaceJAIExt getIHSColorSpace() {
        return new IHSColorSpaceJAIExt();
    }
}
