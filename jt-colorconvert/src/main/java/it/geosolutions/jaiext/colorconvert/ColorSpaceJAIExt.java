/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2014 GeoSolutions


 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.geosolutions.jaiext.colorconvert;

import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

import javax.media.jai.ColorSpaceJAI;
import javax.media.jai.PixelAccessor;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RasterFactory;
import javax.media.jai.UnpackedImageData;
import javax.media.jai.iterator.RandomIter;

import com.sun.media.jai.util.ImageUtil;

/**
 * Abstract class extending the {@link ColorSpaceJAI} class in order to add support for external ROI or NoData.
 * 
 * @author Nicola Lagomarsini geosolutions
 * 
 */
public abstract class ColorSpaceJAIExt extends ColorSpaceJAI {

    /** Cache the maximum value for XYZ color space. */
    private static final double maxXYZ = 1 + 32767.0 / 32768.0;

    /** The map from byte RGB to the step before matrix operation. */
    private static double[] LUT = new double[256];

    static {
        for (int i = 0; i < 256; i++) {
            double v = i / 255.0;
            if (v < 0.040449936)
                LUT[i] = v / 12.92;
            else
                LUT[i] = Math.pow((v + 0.055) / 1.055, 2.4);
        }
    }

    /**
     * Constant indicating that the inner random iterators must pre-calculate an array of the image positions
     */
    public static final boolean ARRAY_CALC = true;

    /**
     * Constant indicating that the inner random iterators must cache the current tile position
     */
    public static final boolean TILE_CACHED = true;

    /** Cache the power value for XYZ to RGB */
    public static final double POWER1 = 1.0 / 2.4;

    protected ColorSpaceJAIExt(int type, int numComponents, boolean isRGBPreferredIntermediary) {
        super(type, numComponents, isRGBPreferredIntermediary);
    }

    /**
     * Converts an input CIEXYZ Raster into a new one with a new ColorSpace
     * 
     * @param src
     * @param srcComponentSize
     * @param dest
     * @param dstComponentSize
     * @param roi
     * @param nodata
     * @param destNodata
     * @return a new {@link WritableRaster} with destination data values
     */
    public abstract WritableRaster fromCIEXYZ(Raster src, int[] srcComponentSize,
            WritableRaster dest, int[] dstComponentSize, ROI roi, Range nodata, float[] destNodata);

    /**
     * Converts an input RGB Raster into a new one with a new ColorSpace
     * 
     * @param src
     * @param srcComponentSize
     * @param dest
     * @param dstComponentSize
     * @param roi
     * @param nodata
     * @param destNodata
     * @return a new {@link WritableRaster} with destination data values
     */
    public abstract WritableRaster fromRGB(Raster src, int[] srcComponentSize, WritableRaster dest,
            int[] dstComponentSize, ROI roi, Range nodata, float[] destNodata);

    /**
     * Converts an input Raster into a new one with CIEXYZ ColorSpace
     * 
     * @param src
     * @param srcComponentSize
     * @param dest
     * @param dstComponentSize
     * @param roi
     * @param nodata
     * @param destNodata
     * @return a new {@link WritableRaster} with destination data values
     */
    public abstract WritableRaster toCIEXYZ(Raster src, int[] srcComponentSize,
            WritableRaster dest, int[] dstComponentSize, ROI roi, Range nodata, float[] destNodata);

    /**
     * Converts an input Raster into a new one with RGB ColorSpace
     * 
     * @param src
     * @param srcComponentSize
     * @param dest
     * @param dstComponentSize
     * @param roi
     * @param nodata
     * @param destNodata
     * @return a new {@link WritableRaster} with destination data values
     */
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

    /**
     * Method for converting an RGB array into an XYZ one
     * 
     * @param RGB
     * @param XYZ
     */
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

    /**
     * Method for converting an XYZ array into an RGB one
     * 
     * @param XYZ
     * @param RGB
     */
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

    /**
     * Converts the input array of data into signed ones
     */
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

    /**
     * @return a new instance of {@link IHSColorSpaceJAIExt}.
     */
    public static ColorSpaceJAIExt getIHSColorSpaceJAIEXT() {
        return new IHSColorSpaceJAIExt();
    }

    public static WritableRaster RGBToCIEXYZ(Raster src, int[] srcComponentSize,
            WritableRaster dest, int[] destComponentSize, ROI roi, Range nodata, float[] destNodata) {

        checkParameters(src, srcComponentSize, dest, destComponentSize);

        // ROI check
        ROI roiTile = null;

        RandomIter roiIter = null;

        boolean roiContainsTile = false;
        boolean roiDisjointTile = false;

        // If a ROI is present, then only the part contained inside the current
        // tile bounds is taken.
        Rectangle bounds = null;
        if (roi != null) {
            bounds = roi.getBounds();
            Rectangle srcRectExpanded = dest.getBounds();
            // The tile dimension is extended for avoiding border errors
            srcRectExpanded.setRect(srcRectExpanded.getMinX() - 1, srcRectExpanded.getMinY() - 1,
                    srcRectExpanded.getWidth() + 2, srcRectExpanded.getHeight() + 2);
            roiTile = roi.intersect(new ROIShape(srcRectExpanded));

            if (!bounds.intersects(srcRectExpanded)) {
                roiDisjointTile = true;
            } else {
                roiContainsTile = roiTile.contains(srcRectExpanded);
                if (!roiContainsTile) {
                    if (!roiTile.intersects(srcRectExpanded)) {
                        roiDisjointTile = true;
                    } else {
                        PlanarImage roiIMG = roi.getAsImage();
                        roiIter = RandomIterFactory.create(roiIMG, null, TILE_CACHED, ARRAY_CALC);
                    }
                }
            }
        }

        SampleModel srcSampleModel = src.getSampleModel();

        // if the srcComponentSize is not provided, use the sample sizes
        // from the source's sample model
        if (srcComponentSize == null)
            srcComponentSize = srcSampleModel.getSampleSize();

        // if the destination raster is not provided, create a new one
        if (dest == null) {
            Point origin = new Point(src.getMinX(), src.getMinY());
            dest = RasterFactory.createWritableRaster(srcSampleModel, origin);
        }

        SampleModel dstSampleModel = dest.getSampleModel();

        // if the destComponentSize is not provided, use the sample sizes
        // from the destination's sample model
        if (destComponentSize == null)
            destComponentSize = dstSampleModel.getSampleSize();

        PixelAccessor srcAcc = new PixelAccessor(srcSampleModel, null);
        UnpackedImageData srcUid = srcAcc.getPixels(src, src.getBounds(),
                srcSampleModel.getDataType(), false);

        switch (srcSampleModel.getDataType()) {

        case DataBuffer.TYPE_BYTE:
            RGBToCIEXYZByte(srcUid, srcComponentSize, dest, destComponentSize, roiContainsTile,
                    roiDisjointTile, roiIter, bounds, nodata, destNodata);
            break;
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            RGBToCIEXYZShort(srcUid, srcComponentSize, dest, destComponentSize, roiContainsTile,
                    roiDisjointTile, roiIter, bounds, nodata, destNodata);
            break;
        case DataBuffer.TYPE_INT:
            RGBToCIEXYZInt(srcUid, srcComponentSize, dest, destComponentSize, roiContainsTile,
                    roiDisjointTile, roiIter, bounds, nodata, destNodata);
            break;
        case DataBuffer.TYPE_FLOAT:
            RGBToCIEXYZFloat(srcUid, srcComponentSize, dest, destComponentSize, roiContainsTile,
                    roiDisjointTile, roiIter, bounds, nodata, destNodata);
            break;
        case DataBuffer.TYPE_DOUBLE:
            RGBToCIEXYZDouble(srcUid, srcComponentSize, dest, destComponentSize, roiContainsTile,
                    roiDisjointTile, roiIter, bounds, nodata, destNodata);
            break;
        }

        return dest;
    }

    // convert a byte ratser from RGB to CIEXYZ
    private static void RGBToCIEXYZByte(UnpackedImageData src, int[] srcComponentSize,
            WritableRaster dest, int[] destComponentSize, boolean roiContainsTile,
            boolean roiDisjointTile, RandomIter roiIter, Rectangle roiBounds, Range nodata,
            float[] destNodata) {
        byte[] rBuf = src.getByteData(0);
        byte[] gBuf = src.getByteData(1);
        byte[] bBuf = src.getByteData(2);

        // used to left-shift the value to fill in all the 8-bits
        int normr = 8 - srcComponentSize[0];
        int normg = 8 - srcComponentSize[1];
        int normb = 8 - srcComponentSize[2];

        // the norms used to map the color value to the desired range
        double normx = 1.0, normy = normx, normz = normx;

        int dstType = dest.getSampleModel().getDataType();
        boolean isInt = (dstType < DataBuffer.TYPE_FLOAT);

        // for the integer type, redefine the norms and upper bounds
        // because rgb={1.0, 1.0, 1.0} is xyz={0.950456, 1.0, 1.088754},
        // so for normx, normz, they are specially treated
        if (isInt) {
            normx = ((1L << destComponentSize[0]) - 1) / maxXYZ;
            normy = ((1L << destComponentSize[1]) - 1) / maxXYZ;
            normz = ((1L << destComponentSize[2]) - 1) / maxXYZ;
        }

        int height = dest.getHeight();
        int width = dest.getWidth();

        double[] dstPixels = new double[3 * height * width];

        int rStart = src.bandOffsets[0];
        int gStart = src.bandOffsets[1];
        int bStart = src.bandOffsets[2];
        int srcPixelStride = src.pixelStride;
        int srcLineStride = src.lineStride;

        int dIndex = 0;

        boolean hasROI = roiIter != null && roiBounds != null;
        boolean hasNodata = nodata != null;

        int destX = dest.getMinX();
        int destY = dest.getMinY();

        double[] destNoDataFinal = getConvertedNodataB(isInt, destNodata, normr, normg, normb,
                normx, normy, normz);

        if (roiDisjointTile) {
            ImageUtil.fillBackground(dest, dest.getBounds(), destNoDataFinal);
            return;
        }

        if (!hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {
                    double R = LUT[(rBuf[rIndex] & 0xFF) << normr];
                    double G = LUT[(gBuf[gIndex] & 0xFF) << normg];
                    double B = LUT[(bBuf[bIndex] & 0xFF) << normb];

                    if (isInt) {
                        dstPixels[dIndex++] = (0.45593763 * R + 0.39533819 * G + 0.19954964 * B)
                                * normx;
                        dstPixels[dIndex++] = (0.23157515 * R + 0.77905262 * G + 0.07864978 * B)
                                * normy;
                        dstPixels[dIndex++] = (0.01593493 * R + 0.09841772 * G + 0.78488615 * B)
                                * normz;
                    } else {
                        dstPixels[dIndex++] = 0.45593763 * R + 0.39533819 * G + 0.19954964 * B;
                        dstPixels[dIndex++] = 0.23157515 * R + 0.77905262 * G + 0.07864978 * B;
                        dstPixels[dIndex++] = 0.01593493 * R + 0.09841772 * G + 0.78488615 * B;
                    }
                }
            }
        } else if (!hasNodata && hasROI) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {

                int y0 = j + destY;

                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {

                    int x0 = i + destX;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    double R = LUT[(rBuf[rIndex] & 0xFF) << normr];
                    double G = LUT[(gBuf[gIndex] & 0xFF) << normg];
                    double B = LUT[(bBuf[bIndex] & 0xFF) << normb];

                    if (isInt) {
                        dstPixels[dIndex++] = (0.45593763 * R + 0.39533819 * G + 0.19954964 * B)
                                * normx;
                        dstPixels[dIndex++] = (0.23157515 * R + 0.77905262 * G + 0.07864978 * B)
                                * normy;
                        dstPixels[dIndex++] = (0.01593493 * R + 0.09841772 * G + 0.78488615 * B)
                                * normz;
                    } else {
                        dstPixels[dIndex++] = 0.45593763 * R + 0.39533819 * G + 0.19954964 * B;
                        dstPixels[dIndex++] = 0.23157515 * R + 0.77905262 * G + 0.07864978 * B;
                        dstPixels[dIndex++] = 0.01593493 * R + 0.09841772 * G + 0.78488615 * B;
                    }
                }
            }
        } else if (hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {

                    byte r = rBuf[rIndex];
                    byte g = gBuf[gIndex];
                    byte b = bBuf[bIndex];

                    boolean notValid = nodata.contains(r) || nodata.contains(g)
                            || nodata.contains(b);

                    if (notValid) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    double R = LUT[(r & 0xFF) << normr];
                    double G = LUT[(g & 0xFF) << normg];
                    double B = LUT[(b & 0xFF) << normb];

                    if (isInt) {
                        dstPixels[dIndex++] = (0.45593763 * R + 0.39533819 * G + 0.19954964 * B)
                                * normx;
                        dstPixels[dIndex++] = (0.23157515 * R + 0.77905262 * G + 0.07864978 * B)
                                * normy;
                        dstPixels[dIndex++] = (0.01593493 * R + 0.09841772 * G + 0.78488615 * B)
                                * normz;
                    } else {
                        dstPixels[dIndex++] = 0.45593763 * R + 0.39533819 * G + 0.19954964 * B;
                        dstPixels[dIndex++] = 0.23157515 * R + 0.77905262 * G + 0.07864978 * B;
                        dstPixels[dIndex++] = 0.01593493 * R + 0.09841772 * G + 0.78488615 * B;
                    }
                }
            }
        } else {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {

                int y0 = j + destY;

                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {

                    int x0 = i + destX;

                    byte r = rBuf[rIndex];
                    byte g = gBuf[gIndex];
                    byte b = bBuf[bIndex];

                    boolean notValid = nodata.contains(r) || nodata.contains(g)
                            || nodata.contains(b);

                    if (notValid
                            || !(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    double R = LUT[(r & 0xFF) << normr];
                    double G = LUT[(g & 0xFF) << normg];
                    double B = LUT[(b & 0xFF) << normb];

                    if (isInt) {
                        dstPixels[dIndex++] = (0.45593763 * R + 0.39533819 * G + 0.19954964 * B)
                                * normx;
                        dstPixels[dIndex++] = (0.23157515 * R + 0.77905262 * G + 0.07864978 * B)
                                * normy;
                        dstPixels[dIndex++] = (0.01593493 * R + 0.09841772 * G + 0.78488615 * B)
                                * normz;
                    } else {
                        dstPixels[dIndex++] = 0.45593763 * R + 0.39533819 * G + 0.19954964 * B;
                        dstPixels[dIndex++] = 0.23157515 * R + 0.77905262 * G + 0.07864978 * B;
                        dstPixels[dIndex++] = 0.01593493 * R + 0.09841772 * G + 0.78488615 * B;
                    }
                }
            }
        }

        // Because of 4738524: setPixels should round the provided double
        // value instead of casting
        // If it is fixed, then this piece of code can be removed.
        if (dstType < DataBuffer.TYPE_FLOAT)
            roundValues(dstPixels);

        convertToSigned(dstPixels, dstType);
        dest.setPixels(dest.getMinX(), dest.getMinY(), width, height, dstPixels);
    }

    // convert a short raster from RGB to CIEXYZ
    private static void RGBToCIEXYZShort(UnpackedImageData src, int[] srcComponentSize,
            WritableRaster dest, int[] destComponentSize, boolean roiContainsTile,
            boolean roiDisjointTile, RandomIter roiIter, Rectangle roiBounds, Range nodata,
            float[] destNodata) {
        short[] rBuf = src.getShortData(0);
        short[] gBuf = src.getShortData(1);
        short[] bBuf = src.getShortData(2);

        // used to left-shift the input value to fill all the bits
        float normr = (1 << srcComponentSize[0]) - 1;
        float normg = (1 << srcComponentSize[1]) - 1;
        float normb = (1 << srcComponentSize[2]) - 1;

        // used to map the output to the desired range
        double normx = 1.0, normy = 1.0, normz = 1.0;

        int dstType = dest.getSampleModel().getDataType();
        boolean isInt = (dstType < DataBuffer.TYPE_FLOAT);

        // define the norms and upper bounds for the integer types
        // see the comments in RGBToCIEXYZByte
        if (isInt) {
            normx = ((1L << destComponentSize[0]) - 1) / maxXYZ;
            normy = ((1L << destComponentSize[1]) - 1) / maxXYZ;
            normz = ((1L << destComponentSize[2]) - 1) / maxXYZ;
        }

        int height = dest.getHeight();
        int width = dest.getWidth();

        double[] dstPixels = new double[3 * height * width];

        int rStart = src.bandOffsets[0];
        int gStart = src.bandOffsets[1];
        int bStart = src.bandOffsets[2];
        int srcPixelStride = src.pixelStride;
        int srcLineStride = src.lineStride;

        float[] XYZ = new float[3];
        float[] RGB = new float[3];

        int dIndex = 0;

        boolean hasROI = roiIter != null && roiBounds != null;
        boolean hasNodata = nodata != null;

        int destX = dest.getMinX();
        int destY = dest.getMinY();

        double[] destNoDataFinal = getConvertedNodata(isInt, destNodata, DataBuffer.TYPE_SHORT,
                normr, normg, normb, normx, normy, normz);

        if (roiDisjointTile) {
            ImageUtil.fillBackground(dest, dest.getBounds(), destNoDataFinal);
            return;
        }

        if (!hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {
                    RGB[0] = (rBuf[rIndex] & 0xFFFF) / normr;
                    RGB[1] = (gBuf[gIndex] & 0xFFFF) / normg;
                    RGB[2] = (bBuf[bIndex] & 0xFFFF) / normb;

                    RGB2XYZ(RGB, XYZ);

                    if (isInt) {
                        dstPixels[dIndex++] = XYZ[0] * normx;
                        dstPixels[dIndex++] = XYZ[1] * normy;
                        dstPixels[dIndex++] = XYZ[2] * normz;
                    } else {
                        dstPixels[dIndex++] = XYZ[0];
                        dstPixels[dIndex++] = XYZ[1];
                        dstPixels[dIndex++] = XYZ[2];
                    }
                }
            }
        } else if (!hasNodata && hasROI) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                int y0 = j + destY;
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {
                    int x0 = i + destX;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    RGB[0] = (rBuf[rIndex] & 0xFFFF) / normr;
                    RGB[1] = (gBuf[gIndex] & 0xFFFF) / normg;
                    RGB[2] = (bBuf[bIndex] & 0xFFFF) / normb;

                    RGB2XYZ(RGB, XYZ);

                    if (isInt) {
                        dstPixels[dIndex++] = XYZ[0] * normx;
                        dstPixels[dIndex++] = XYZ[1] * normy;
                        dstPixels[dIndex++] = XYZ[2] * normz;
                    } else {
                        dstPixels[dIndex++] = XYZ[0];
                        dstPixels[dIndex++] = XYZ[1];
                        dstPixels[dIndex++] = XYZ[2];
                    }
                }
            }
        } else if (hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {

                    short r = rBuf[rIndex];
                    short g = gBuf[gIndex];
                    short b = bBuf[bIndex];

                    boolean notValid = nodata.contains(r) || nodata.contains(g)
                            || nodata.contains(b);

                    if (notValid) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    RGB[0] = (r & 0xFFFF) / normr;
                    RGB[1] = (g & 0xFFFF) / normg;
                    RGB[2] = (b & 0xFFFF) / normb;

                    RGB2XYZ(RGB, XYZ);

                    if (isInt) {
                        dstPixels[dIndex++] = XYZ[0] * normx;
                        dstPixels[dIndex++] = XYZ[1] * normy;
                        dstPixels[dIndex++] = XYZ[2] * normz;
                    } else {
                        dstPixels[dIndex++] = XYZ[0];
                        dstPixels[dIndex++] = XYZ[1];
                        dstPixels[dIndex++] = XYZ[2];
                    }
                }
            }
        } else {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                int y0 = j + destY;
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {
                    int x0 = i + destX;

                    short r = rBuf[rIndex];
                    short g = gBuf[gIndex];
                    short b = bBuf[bIndex];

                    boolean notValid = nodata.contains(r) || nodata.contains(g)
                            || nodata.contains(b);

                    if (notValid
                            || !(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    RGB[0] = (r & 0xFFFF) / normr;
                    RGB[1] = (g & 0xFFFF) / normg;
                    RGB[2] = (b & 0xFFFF) / normb;

                    RGB2XYZ(RGB, XYZ);

                    if (isInt) {
                        dstPixels[dIndex++] = XYZ[0] * normx;
                        dstPixels[dIndex++] = XYZ[1] * normy;
                        dstPixels[dIndex++] = XYZ[2] * normz;
                    } else {
                        dstPixels[dIndex++] = XYZ[0];
                        dstPixels[dIndex++] = XYZ[1];
                        dstPixels[dIndex++] = XYZ[2];
                    }
                }
            }
        }

        // Because of 4738524: setPixels should round the provided double
        // value instead of casting
        // If it is fixed, then this piece of code can be removed.
        if (dstType < DataBuffer.TYPE_FLOAT)
            roundValues(dstPixels);

        convertToSigned(dstPixels, dstType);
        dest.setPixels(dest.getMinX(), dest.getMinY(), width, height, dstPixels);
    }

    // convert a int type ratser from RGB to CIEXYZ
    private static void RGBToCIEXYZInt(UnpackedImageData src, int[] srcComponentSize,
            WritableRaster dest, int[] destComponentSize, boolean roiContainsTile,
            boolean roiDisjointTile, RandomIter roiIter, Rectangle roiBounds, Range nodata,
            float[] destNodata) {
        int[] rBuf = src.getIntData(0);
        int[] gBuf = src.getIntData(1);
        int[] bBuf = src.getIntData(2);

        // used to left-shift the input to fill all the bits
        float normr = (1L << srcComponentSize[0]) - 1;
        float normg = (1L << srcComponentSize[1]) - 1;
        float normb = (1L << srcComponentSize[2]) - 1;

        // norms to map the output to the desired range
        double normx = 1.0, normy = 1.0, normz = 1.0;

        int dstType = dest.getSampleModel().getDataType();
        boolean isInt = (dstType < DataBuffer.TYPE_FLOAT);

        // define the norm and upper bounds for the integer output types
        // see also the comments in RGBToCIEXYZByte
        if (isInt) {
            normx = ((1L << destComponentSize[0]) - 1) / maxXYZ;
            normy = ((1L << destComponentSize[1]) - 1) / maxXYZ;
            normz = ((1L << destComponentSize[2]) - 1) / maxXYZ;
        }

        int height = dest.getHeight();
        int width = dest.getWidth();

        double[] dstPixels = new double[3 * height * width];

        int rStart = src.bandOffsets[0];
        int gStart = src.bandOffsets[1];
        int bStart = src.bandOffsets[2];
        int srcPixelStride = src.pixelStride;
        int srcLineStride = src.lineStride;

        float[] XYZ = new float[3];
        float[] RGB = new float[3];

        int dIndex = 0;

        boolean hasROI = roiIter != null && roiBounds != null;
        boolean hasNodata = nodata != null;

        int destX = dest.getMinX();
        int destY = dest.getMinY();

        double[] destNoDataFinal = getConvertedNodata(isInt, destNodata, DataBuffer.TYPE_INT,
                normr, normg, normb, normx, normy, normz);

        if (roiDisjointTile) {
            ImageUtil.fillBackground(dest, dest.getBounds(), destNoDataFinal);
            return;
        }

        if (!hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {
                    RGB[0] = (rBuf[rIndex] & 0xFFFFFFFFl) / normr;
                    RGB[1] = (gBuf[gIndex] & 0xFFFFFFFFl) / normg;
                    RGB[2] = (bBuf[bIndex] & 0xFFFFFFFFl) / normb;

                    RGB2XYZ(RGB, XYZ);

                    if (isInt) {
                        dstPixels[dIndex++] = XYZ[0] * normx;
                        dstPixels[dIndex++] = XYZ[1] * normy;
                        dstPixels[dIndex++] = XYZ[2] * normz;
                    } else {
                        dstPixels[dIndex++] = XYZ[0];
                        dstPixels[dIndex++] = XYZ[1];
                        dstPixels[dIndex++] = XYZ[2];
                    }
                }
            }
        } else if (!hasNodata && hasROI) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                int y0 = j + destY;
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {
                    int x0 = i + destX;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    RGB[0] = (rBuf[rIndex] & 0xFFFFFFFFl) / normr;
                    RGB[1] = (gBuf[gIndex] & 0xFFFFFFFFl) / normg;
                    RGB[2] = (bBuf[bIndex] & 0xFFFFFFFFl) / normb;

                    RGB2XYZ(RGB, XYZ);

                    if (isInt) {
                        dstPixels[dIndex++] = XYZ[0] * normx;
                        dstPixels[dIndex++] = XYZ[1] * normy;
                        dstPixels[dIndex++] = XYZ[2] * normz;
                    } else {
                        dstPixels[dIndex++] = XYZ[0];
                        dstPixels[dIndex++] = XYZ[1];
                        dstPixels[dIndex++] = XYZ[2];
                    }
                }
            }
        } else if (hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {

                    int r = rBuf[rIndex];
                    int g = gBuf[gIndex];
                    int b = bBuf[bIndex];

                    boolean notValid = nodata.contains(r) || nodata.contains(g)
                            || nodata.contains(b);

                    if (notValid) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    RGB[0] = (r & 0xFFFFFFFFl) / normr;
                    RGB[1] = (g & 0xFFFFFFFFl) / normg;
                    RGB[2] = (b & 0xFFFFFFFFl) / normb;

                    RGB2XYZ(RGB, XYZ);

                    if (isInt) {
                        dstPixels[dIndex++] = XYZ[0] * normx;
                        dstPixels[dIndex++] = XYZ[1] * normy;
                        dstPixels[dIndex++] = XYZ[2] * normz;
                    } else {
                        dstPixels[dIndex++] = XYZ[0];
                        dstPixels[dIndex++] = XYZ[1];
                        dstPixels[dIndex++] = XYZ[2];
                    }
                }
            }
        } else {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                int y0 = j + destY;
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {
                    int x0 = i + destX;

                    int r = rBuf[rIndex];
                    int g = gBuf[gIndex];
                    int b = bBuf[bIndex];

                    boolean notValid = nodata.contains(r) || nodata.contains(g)
                            || nodata.contains(b);

                    if (notValid
                            || !(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    RGB[0] = (r & 0xFFFFFFFFl) / normr;
                    RGB[1] = (g & 0xFFFFFFFFl) / normg;
                    RGB[2] = (b & 0xFFFFFFFFl) / normb;

                    RGB2XYZ(RGB, XYZ);

                    if (isInt) {
                        dstPixels[dIndex++] = XYZ[0] * normx;
                        dstPixels[dIndex++] = XYZ[1] * normy;
                        dstPixels[dIndex++] = XYZ[2] * normz;
                    } else {
                        dstPixels[dIndex++] = XYZ[0];
                        dstPixels[dIndex++] = XYZ[1];
                        dstPixels[dIndex++] = XYZ[2];
                    }
                }
            }
        }

        // Because of 4738524: setPixels should round the provided double
        // value instead of casting
        // If it is fixed, then this piece of code can be removed.
        if (dstType < DataBuffer.TYPE_FLOAT)
            roundValues(dstPixels);

        convertToSigned(dstPixels, dstType);
        dest.setPixels(dest.getMinX(), dest.getMinY(), width, height, dstPixels);
    }

    // convert a float type ratser from RGB to CIEXYZ color space
    private static void RGBToCIEXYZFloat(UnpackedImageData src, int[] srcComponentSize,
            WritableRaster dest, int[] destComponentSize, boolean roiContainsTile,
            boolean roiDisjointTile, RandomIter roiIter, Rectangle roiBounds, Range nodata,
            float[] destNodata) {
        float[] rBuf = src.getFloatData(0);
        float[] gBuf = src.getFloatData(1);
        float[] bBuf = src.getFloatData(2);

        // norms to map the output value to the desired range
        double normx = 1.0, normy = 1.0, normz = 1.0;

        int dstType = dest.getSampleModel().getDataType();
        boolean isInt = (dstType < DataBuffer.TYPE_FLOAT);

        // define the norms and upper bounds for the integer types
        if (isInt) {
            normx = ((1L << destComponentSize[0]) - 1) / maxXYZ;
            normy = ((1L << destComponentSize[1]) - 1) / maxXYZ;
            normz = ((1L << destComponentSize[2]) - 1) / maxXYZ;
        }

        int height = dest.getHeight();
        int width = dest.getWidth();

        double[] dstPixels = new double[3 * height * width];

        int rStart = src.bandOffsets[0];
        int gStart = src.bandOffsets[1];
        int bStart = src.bandOffsets[2];
        int srcPixelStride = src.pixelStride;
        int srcLineStride = src.lineStride;

        float[] XYZ = new float[3];
        float[] RGB = new float[3];

        int dIndex = 0;

        boolean hasROI = roiIter != null && roiBounds != null;
        boolean hasNodata = nodata != null;
        if (hasNodata) {
            nodata = RangeFactory.convertToDoubleRange(nodata);
        }

        int destX = dest.getMinX();
        int destY = dest.getMinY();

        double[] destNoDataFinal = getConvertedNodata(isInt, destNodata, DataBuffer.TYPE_FLOAT, 0,
                0, 0, normx, normy, normz);

        if (roiDisjointTile) {
            ImageUtil.fillBackground(dest, dest.getBounds(), destNoDataFinal);
            return;
        }

        if (!hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {
                    RGB[0] = rBuf[rIndex];
                    RGB[1] = gBuf[gIndex];
                    RGB[2] = bBuf[bIndex];

                    RGB2XYZ(RGB, XYZ);

                    if (isInt) {
                        dstPixels[dIndex++] = XYZ[0] * normx;
                        dstPixels[dIndex++] = XYZ[1] * normy;
                        dstPixels[dIndex++] = XYZ[2] * normz;
                    } else {
                        dstPixels[dIndex++] = XYZ[0];
                        dstPixels[dIndex++] = XYZ[1];
                        dstPixels[dIndex++] = XYZ[2];
                    }
                }
            }
        } else if (!hasNodata && hasROI) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                int y0 = j + destY;
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {
                    int x0 = i + destX;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    RGB[0] = rBuf[rIndex];
                    RGB[1] = gBuf[gIndex];
                    RGB[2] = bBuf[bIndex];

                    RGB2XYZ(RGB, XYZ);

                    if (isInt) {
                        dstPixels[dIndex++] = XYZ[0] * normx;
                        dstPixels[dIndex++] = XYZ[1] * normy;
                        dstPixels[dIndex++] = XYZ[2] * normz;
                    } else {
                        dstPixels[dIndex++] = XYZ[0];
                        dstPixels[dIndex++] = XYZ[1];
                        dstPixels[dIndex++] = XYZ[2];
                    }
                }
            }
        } else if (hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {

                    RGB[0] = rBuf[rIndex];
                    RGB[1] = gBuf[gIndex];
                    RGB[2] = bBuf[bIndex];

                    boolean notValid = nodata.contains(RGB[0]) || nodata.contains(RGB[1])
                            || nodata.contains(RGB[2]);

                    if (notValid) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    RGB2XYZ(RGB, XYZ);

                    if (isInt) {
                        dstPixels[dIndex++] = XYZ[0] * normx;
                        dstPixels[dIndex++] = XYZ[1] * normy;
                        dstPixels[dIndex++] = XYZ[2] * normz;
                    } else {
                        dstPixels[dIndex++] = XYZ[0];
                        dstPixels[dIndex++] = XYZ[1];
                        dstPixels[dIndex++] = XYZ[2];
                    }
                }
            }
        } else {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                int y0 = j + destY;
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {
                    int x0 = i + destX;

                    RGB[0] = rBuf[rIndex];
                    RGB[1] = gBuf[gIndex];
                    RGB[2] = bBuf[bIndex];

                    boolean notValid = nodata.contains(RGB[0]) || nodata.contains(RGB[1])
                            || nodata.contains(RGB[2]);

                    if (notValid
                            || !(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    RGB2XYZ(RGB, XYZ);

                    if (isInt) {
                        dstPixels[dIndex++] = XYZ[0] * normx;
                        dstPixels[dIndex++] = XYZ[1] * normy;
                        dstPixels[dIndex++] = XYZ[2] * normz;
                    } else {
                        dstPixels[dIndex++] = XYZ[0];
                        dstPixels[dIndex++] = XYZ[1];
                        dstPixels[dIndex++] = XYZ[2];
                    }
                }
            }
        }

        // Because of 4738524: setPixels should round the provided double
        // value instead of casting
        // If it is fixed, then this piece of code can be removed.
        if (dstType < DataBuffer.TYPE_FLOAT)
            roundValues(dstPixels);

        convertToSigned(dstPixels, dstType);
        dest.setPixels(dest.getMinX(), dest.getMinY(), width, height, dstPixels);
    }

    // convert a double type raster from RGB to CIEXYZ
    private static void RGBToCIEXYZDouble(UnpackedImageData src, int[] srcComponentSize,
            WritableRaster dest, int[] destComponentSize, boolean roiContainsTile,
            boolean roiDisjointTile, RandomIter roiIter, Rectangle roiBounds, Range nodata,
            float[] destNodata) {
        double[] rBuf = src.getDoubleData(0);
        double[] gBuf = src.getDoubleData(1);
        double[] bBuf = src.getDoubleData(2);

        // norms to map the output to the desired range
        double normx = 1.0, normy = 1.0, normz = 1.0;

        int dstType = dest.getSampleModel().getDataType();
        boolean isInt = (dstType < DataBuffer.TYPE_FLOAT);

        if (isInt) {
            normx = ((1L << destComponentSize[0]) - 1) / maxXYZ;
            normy = ((1L << destComponentSize[1]) - 1) / maxXYZ;
            normz = ((1L << destComponentSize[2]) - 1) / maxXYZ;
        }

        int height = dest.getHeight();
        int width = dest.getWidth();

        double[] dstPixels = new double[3 * height * width];

        int rStart = src.bandOffsets[0];
        int gStart = src.bandOffsets[1];
        int bStart = src.bandOffsets[2];
        int srcPixelStride = src.pixelStride;
        int srcLineStride = src.lineStride;

        float[] XYZ = new float[3];
        float[] RGB = new float[3];

        int dIndex = 0;

        boolean hasROI = roiIter != null && roiBounds != null;
        boolean hasNodata = nodata != null;

        int destX = dest.getMinX();
        int destY = dest.getMinY();

        double[] destNoDataFinal = getConvertedNodata(isInt, destNodata, DataBuffer.TYPE_DOUBLE, 0,
                0, 0, normx, normy, normz);

        if (roiDisjointTile) {
            ImageUtil.fillBackground(dest, dest.getBounds(), destNoDataFinal);
            return;
        }

        if (!hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {
                    RGB[0] = (float) rBuf[rIndex];
                    RGB[1] = (float) gBuf[gIndex];
                    RGB[2] = (float) bBuf[bIndex];

                    RGB2XYZ(RGB, XYZ);

                    if (isInt) {
                        dstPixels[dIndex++] = XYZ[0] * normx;
                        dstPixels[dIndex++] = XYZ[1] * normy;
                        dstPixels[dIndex++] = XYZ[2] * normz;
                    } else {
                        dstPixels[dIndex++] = XYZ[0];
                        dstPixels[dIndex++] = XYZ[1];
                        dstPixels[dIndex++] = XYZ[2];
                    }
                }
            }
        } else if (!hasNodata && hasROI) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                int y0 = j + destY;
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {
                    int x0 = i + destX;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    RGB[0] = (float) rBuf[rIndex];
                    RGB[1] = (float) gBuf[gIndex];
                    RGB[2] = (float) bBuf[bIndex];

                    RGB2XYZ(RGB, XYZ);

                    if (isInt) {
                        dstPixels[dIndex++] = XYZ[0] * normx;
                        dstPixels[dIndex++] = XYZ[1] * normy;
                        dstPixels[dIndex++] = XYZ[2] * normz;
                    } else {
                        dstPixels[dIndex++] = XYZ[0];
                        dstPixels[dIndex++] = XYZ[1];
                        dstPixels[dIndex++] = XYZ[2];
                    }
                }
            }
        } else if (hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {

                    RGB[0] = (float) rBuf[rIndex];
                    RGB[1] = (float) gBuf[gIndex];
                    RGB[2] = (float) bBuf[bIndex];

                    boolean notValid = nodata.contains(rBuf[rIndex])
                            || nodata.contains(gBuf[gIndex]) || nodata.contains(bBuf[bIndex]);

                    if (notValid) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    RGB2XYZ(RGB, XYZ);

                    if (isInt) {
                        dstPixels[dIndex++] = XYZ[0] * normx;
                        dstPixels[dIndex++] = XYZ[1] * normy;
                        dstPixels[dIndex++] = XYZ[2] * normz;
                    } else {
                        dstPixels[dIndex++] = XYZ[0];
                        dstPixels[dIndex++] = XYZ[1];
                        dstPixels[dIndex++] = XYZ[2];
                    }
                }
            }
        } else {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                int y0 = j + destY;
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {
                    int x0 = i + destX;

                    RGB[0] = (float) rBuf[rIndex];
                    RGB[1] = (float) gBuf[gIndex];
                    RGB[2] = (float) bBuf[bIndex];

                    boolean notValid = nodata.contains(rBuf[rIndex])
                            || nodata.contains(gBuf[gIndex]) || nodata.contains(bBuf[bIndex]);

                    if (notValid
                            || !(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    RGB2XYZ(RGB, XYZ);

                    if (isInt) {
                        dstPixels[dIndex++] = XYZ[0] * normx;
                        dstPixels[dIndex++] = XYZ[1] * normy;
                        dstPixels[dIndex++] = XYZ[2] * normz;
                    } else {
                        dstPixels[dIndex++] = XYZ[0];
                        dstPixels[dIndex++] = XYZ[1];
                        dstPixels[dIndex++] = XYZ[2];
                    }
                }
            }
        }

        // Because of 4738524: setPixels should round the provided double
        // value instead of casting
        // If it is fixed, then this piece of code can be removed.
        if (dstType < DataBuffer.TYPE_FLOAT)
            roundValues(dstPixels);

        convertToSigned(dstPixels, dstType);
        dest.setPixels(dest.getMinX(), dest.getMinY(), width, height, dstPixels);
    }

    public static WritableRaster CIEXYZToRGB(Raster src, int[] srcComponentSize,
            WritableRaster dest, int[] destComponentSize, ROI roi, Range nodata, float[] destNodata) {

        // Validate the parameters
        checkParameters(src, srcComponentSize, dest, destComponentSize);

        // ROI check
        ROI roiTile = null;

        RandomIter roiIter = null;

        boolean roiContainsTile = false;
        boolean roiDisjointTile = false;

        // If a ROI is present, then only the part contained inside the current
        // tile bounds is taken.
        Rectangle bounds = null;
        if (roi != null) {
            bounds = roi.getBounds();
            Rectangle srcRectExpanded = dest.getBounds();
            // The tile dimension is extended for avoiding border errors
            srcRectExpanded.setRect(srcRectExpanded.getMinX() - 1, srcRectExpanded.getMinY() - 1,
                    srcRectExpanded.getWidth() + 2, srcRectExpanded.getHeight() + 2);
            roiTile = roi.intersect(new ROIShape(srcRectExpanded));

            if (!bounds.intersects(srcRectExpanded)) {
                roiDisjointTile = true;
            } else {
                roiContainsTile = roiTile.contains(srcRectExpanded);
                if (!roiContainsTile) {
                    if (!roiTile.intersects(srcRectExpanded)) {
                        roiDisjointTile = true;
                    } else {
                        PlanarImage roiIMG = roi.getAsImage();
                        roiIter = RandomIterFactory.create(roiIMG, null, TILE_CACHED, ARRAY_CALC);
                    }
                }
            }
        }

        SampleModel srcSampleModel = src.getSampleModel();

        /*
         * if the parameter srcComponentSize is null, use the sample size of the source raster.
         */
        if (srcComponentSize == null)
            srcComponentSize = srcSampleModel.getSampleSize();

        // if the destination raster is null, create a new WritableRaster
        if (dest == null) {
            Point origin = new Point(src.getMinX(), src.getMinY());
            dest = RasterFactory.createWritableRaster(srcSampleModel, origin);
        }

        /*
         * if the parameter dstComponentSize is null, use the sample size of the source raster.
         */
        SampleModel dstSampleModel = dest.getSampleModel();
        if (destComponentSize == null)
            destComponentSize = dstSampleModel.getSampleSize();

        PixelAccessor srcAcc = new PixelAccessor(srcSampleModel, null);
        UnpackedImageData srcUid = srcAcc.getPixels(src, src.getBounds(),
                srcSampleModel.getDataType(), false);

        switch (srcSampleModel.getDataType()) {

        case DataBuffer.TYPE_BYTE:
            CIEXYZToRGBByte(srcUid, srcComponentSize, dest, destComponentSize, roiContainsTile,
                    roiDisjointTile, roiIter, bounds, nodata, destNodata);
            break;
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            CIEXYZToRGBShort(srcUid, srcComponentSize, dest, destComponentSize, roiContainsTile,
                    roiDisjointTile, roiIter, bounds, nodata, destNodata);
            break;
        case DataBuffer.TYPE_INT:
            CIEXYZToRGBInt(srcUid, srcComponentSize, dest, destComponentSize, roiContainsTile,
                    roiDisjointTile, roiIter, bounds, nodata, destNodata);
            break;
        case DataBuffer.TYPE_FLOAT:
            CIEXYZToRGBFloat(srcUid, srcComponentSize, dest, destComponentSize, roiContainsTile,
                    roiDisjointTile, roiIter, bounds, nodata, destNodata);
            break;
        case DataBuffer.TYPE_DOUBLE:
            CIEXYZToRGBDouble(srcUid, srcComponentSize, dest, destComponentSize, roiContainsTile,
                    roiDisjointTile, roiIter, bounds, nodata, destNodata);
            break;
        }

        return dest;
    }

    // Convert a byte raster from CIEXYZ to RGB color space
    private static void CIEXYZToRGBByte(UnpackedImageData src, int[] srcComponentSize,
            WritableRaster dest, int[] destComponentSize, boolean roiContainsTile,
            boolean roiDisjointTile, RandomIter roiIter, Rectangle roiBounds, Range nodata,
            float[] destNodata) {
        byte[] xBuf = src.getByteData(0);
        byte[] yBuf = src.getByteData(1);
        byte[] zBuf = src.getByteData(2);

        // maps the integral XYZ into floating-point
        float normx = (float) (maxXYZ / ((1L << srcComponentSize[0]) - 1));
        float normy = (float) (maxXYZ / ((1L << srcComponentSize[1]) - 1));
        float normz = (float) (maxXYZ / ((1L << srcComponentSize[2]) - 1));

        // the upper bounds for the red, green and blue bands
        double upperr = 1.0, upperg = 1.0, upperb = 1.0;

        int dstType = dest.getSampleModel().getDataType();

        // for the integer type, re-calculate the bounds
        if (dstType < DataBuffer.TYPE_FLOAT) {
            upperr = (1L << destComponentSize[0]) - 1;
            upperg = (1L << destComponentSize[1]) - 1;
            upperb = (1L << destComponentSize[2]) - 1;
        }

        int height = dest.getHeight();
        int width = dest.getWidth();

        double[] dstPixels = new double[3 * height * width];

        int xStart = src.bandOffsets[0];
        int yStart = src.bandOffsets[1];
        int zStart = src.bandOffsets[2];
        int srcPixelStride = src.pixelStride;
        int srcLineStride = src.lineStride;

        float[] XYZ = new float[3];
        float[] RGB = new float[3];

        int dIndex = 0;

        boolean hasROI = roiIter != null && roiBounds != null;
        boolean hasNodata = nodata != null;

        int destX = dest.getMinX();
        int destY = dest.getMinY();

        double[] destNoDataFinal = getConvertedNodataCIEXYZ(destNodata, DataBuffer.TYPE_BYTE,
                upperr, upperg, upperb, normx, normy, normz);

        if (roiDisjointTile) {
            ImageUtil.fillBackground(dest, dest.getBounds(), destNoDataFinal);
            return;
        }

        if (!hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, xStart += srcLineStride, yStart += srcLineStride, zStart += srcLineStride) {
                for (int i = 0, xIndex = xStart, yIndex = yStart, zIndex = zStart; i < width; i++, xIndex += srcPixelStride, yIndex += srcPixelStride, zIndex += srcPixelStride) {
                    XYZ[0] = (xBuf[xIndex] & 0xFF) * normx;
                    XYZ[1] = (yBuf[yIndex] & 0xFF) * normy;
                    XYZ[2] = (zBuf[zIndex] & 0xFF) * normz;

                    XYZ2RGB(XYZ, RGB);

                    dstPixels[dIndex++] = upperr * RGB[0];
                    dstPixels[dIndex++] = upperg * RGB[1];
                    dstPixels[dIndex++] = upperb * RGB[2];
                }
            }
        } else if (!hasNodata && hasROI) {
            for (int j = 0; j < height; j++, xStart += srcLineStride, yStart += srcLineStride, zStart += srcLineStride) {
                int y0 = j + destY;
                for (int i = 0, xIndex = xStart, yIndex = yStart, zIndex = zStart; i < width; i++, xIndex += srcPixelStride, yIndex += srcPixelStride, zIndex += srcPixelStride) {
                    int x0 = i + destX;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    XYZ[0] = (xBuf[xIndex] & 0xFF) * normx;
                    XYZ[1] = (yBuf[yIndex] & 0xFF) * normy;
                    XYZ[2] = (zBuf[zIndex] & 0xFF) * normz;

                    XYZ2RGB(XYZ, RGB);

                    dstPixels[dIndex++] = upperr * RGB[0];
                    dstPixels[dIndex++] = upperg * RGB[1];
                    dstPixels[dIndex++] = upperb * RGB[2];
                }
            }
        } else if (hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, xStart += srcLineStride, yStart += srcLineStride, zStart += srcLineStride) {
                for (int i = 0, xIndex = xStart, yIndex = yStart, zIndex = zStart; i < width; i++, xIndex += srcPixelStride, yIndex += srcPixelStride, zIndex += srcPixelStride) {

                    byte x = xBuf[xIndex];
                    byte y = yBuf[yIndex];
                    byte z = zBuf[zIndex];

                    boolean notValid = nodata.contains(x) || nodata.contains(y)
                            || nodata.contains(z);

                    if (notValid) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    XYZ[0] = (x & 0xFF) * normx;
                    XYZ[1] = (y & 0xFF) * normy;
                    XYZ[2] = (z & 0xFF) * normz;

                    XYZ2RGB(XYZ, RGB);

                    dstPixels[dIndex++] = upperr * RGB[0];
                    dstPixels[dIndex++] = upperg * RGB[1];
                    dstPixels[dIndex++] = upperb * RGB[2];
                }
            }
        } else {
            for (int j = 0; j < height; j++, xStart += srcLineStride, yStart += srcLineStride, zStart += srcLineStride) {
                int y0 = j + destY;
                for (int i = 0, xIndex = xStart, yIndex = yStart, zIndex = zStart; i < width; i++, xIndex += srcPixelStride, yIndex += srcPixelStride, zIndex += srcPixelStride) {
                    int x0 = i + destX;

                    byte x = xBuf[xIndex];
                    byte y = yBuf[yIndex];
                    byte z = zBuf[zIndex];

                    boolean notValid = nodata.contains(x) || nodata.contains(y)
                            || nodata.contains(z);

                    if (notValid
                            || !(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    XYZ[0] = (x & 0xFF) * normx;
                    XYZ[1] = (y & 0xFF) * normy;
                    XYZ[2] = (z & 0xFF) * normz;

                    XYZ2RGB(XYZ, RGB);

                    dstPixels[dIndex++] = upperr * RGB[0];
                    dstPixels[dIndex++] = upperg * RGB[1];
                    dstPixels[dIndex++] = upperb * RGB[2];
                }
            }
        }

        // Because of 4738524: setPixels should round the provided double
        // value instead of casting
        // If it is fixed, then this piece of code can be removed.
        if (dstType < DataBuffer.TYPE_FLOAT)
            roundValues(dstPixels);

        convertToSigned(dstPixels, dstType);
        dest.setPixels(dest.getMinX(), dest.getMinY(), width, height, dstPixels);
    }

    // convert a short type raster from CIEXYZ to RGB
    private static void CIEXYZToRGBShort(UnpackedImageData src, int[] srcComponentSize,
            WritableRaster dest, int[] destComponentSize, boolean roiContainsTile,
            boolean roiDisjointTile, RandomIter roiIter, Rectangle roiBounds, Range nodata,
            float[] destNodata) {
        short[] xBuf = src.getShortData(0);
        short[] yBuf = src.getShortData(1);
        short[] zBuf = src.getShortData(2);

        // maps the integral XYZ into floating-point
        float normx = (float) (maxXYZ / ((1L << srcComponentSize[0]) - 1));
        float normy = (float) (maxXYZ / ((1L << srcComponentSize[1]) - 1));
        float normz = (float) (maxXYZ / ((1L << srcComponentSize[2]) - 1));

        // the upper bounds for the red, green and blue bands
        double upperr = 1.0, upperg = 1.0, upperb = 1.0;

        int dstType = dest.getSampleModel().getDataType();

        // for the integer type, re-calculate the norm and bands
        if (dstType < DataBuffer.TYPE_FLOAT) {
            upperr = (1L << destComponentSize[0]) - 1;
            upperg = (1L << destComponentSize[1]) - 1;
            upperb = (1L << destComponentSize[2]) - 1;
        }

        int height = dest.getHeight();
        int width = dest.getWidth();

        double[] dstPixels = new double[3 * height * width];

        int xStart = src.bandOffsets[0];
        int yStart = src.bandOffsets[1];
        int zStart = src.bandOffsets[2];
        int srcPixelStride = src.pixelStride;
        int srcLineStride = src.lineStride;

        float[] XYZ = new float[3];
        float[] RGB = new float[3];

        int dIndex = 0;

        boolean hasROI = roiIter != null && roiBounds != null;
        boolean hasNodata = nodata != null;

        int destX = dest.getMinX();
        int destY = dest.getMinY();

        double[] destNoDataFinal = getConvertedNodataCIEXYZ(destNodata, DataBuffer.TYPE_SHORT,
                upperr, upperg, upperb, normx, normy, normz);

        if (roiDisjointTile) {
            ImageUtil.fillBackground(dest, dest.getBounds(), destNoDataFinal);
            return;
        }

        if (!hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, xStart += srcLineStride, yStart += srcLineStride, zStart += srcLineStride) {
                for (int i = 0, xIndex = xStart, yIndex = yStart, zIndex = zStart; i < width; i++, xIndex += srcPixelStride, yIndex += srcPixelStride, zIndex += srcPixelStride) {
                    XYZ[0] = (xBuf[xIndex] & 0xFFFF) * normx;
                    XYZ[1] = (yBuf[yIndex] & 0xFFFF) * normy;
                    XYZ[2] = (zBuf[zIndex] & 0xFFFF) * normz;

                    XYZ2RGB(XYZ, RGB);

                    dstPixels[dIndex++] = upperr * RGB[0];
                    dstPixels[dIndex++] = upperg * RGB[1];
                    dstPixels[dIndex++] = upperb * RGB[2];
                }
            }
        } else if (!hasNodata && hasROI) {
            for (int j = 0; j < height; j++, xStart += srcLineStride, yStart += srcLineStride, zStart += srcLineStride) {
                int y0 = j + destY;
                for (int i = 0, xIndex = xStart, yIndex = yStart, zIndex = zStart; i < width; i++, xIndex += srcPixelStride, yIndex += srcPixelStride, zIndex += srcPixelStride) {
                    int x0 = i + destX;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    XYZ[0] = (xBuf[xIndex] & 0xFFFF) * normx;
                    XYZ[1] = (yBuf[yIndex] & 0xFFFF) * normy;
                    XYZ[2] = (zBuf[zIndex] & 0xFFFF) * normz;

                    XYZ2RGB(XYZ, RGB);

                    dstPixels[dIndex++] = upperr * RGB[0];
                    dstPixels[dIndex++] = upperg * RGB[1];
                    dstPixels[dIndex++] = upperb * RGB[2];
                }
            }
        } else if (hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, xStart += srcLineStride, yStart += srcLineStride, zStart += srcLineStride) {
                for (int i = 0, xIndex = xStart, yIndex = yStart, zIndex = zStart; i < width; i++, xIndex += srcPixelStride, yIndex += srcPixelStride, zIndex += srcPixelStride) {

                    short x = xBuf[xIndex];
                    short y = yBuf[yIndex];
                    short z = zBuf[zIndex];

                    boolean notValid = nodata.contains(x) || nodata.contains(y)
                            || nodata.contains(z);

                    if (notValid) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    XYZ[0] = (x & 0xFFFF) * normx;
                    XYZ[1] = (y & 0xFFFF) * normy;
                    XYZ[2] = (z & 0xFFFF) * normz;

                    XYZ2RGB(XYZ, RGB);

                    dstPixels[dIndex++] = upperr * RGB[0];
                    dstPixels[dIndex++] = upperg * RGB[1];
                    dstPixels[dIndex++] = upperb * RGB[2];
                }
            }
        } else {
            for (int j = 0; j < height; j++, xStart += srcLineStride, yStart += srcLineStride, zStart += srcLineStride) {
                int y0 = j + destY;
                for (int i = 0, xIndex = xStart, yIndex = yStart, zIndex = zStart; i < width; i++, xIndex += srcPixelStride, yIndex += srcPixelStride, zIndex += srcPixelStride) {
                    int x0 = i + destX;

                    short x = xBuf[xIndex];
                    short y = yBuf[yIndex];
                    short z = zBuf[zIndex];

                    boolean notValid = nodata.contains(x) || nodata.contains(y)
                            || nodata.contains(z);

                    if (notValid
                            || !(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    XYZ[0] = (x & 0xFFFF) * normx;
                    XYZ[1] = (y & 0xFFFF) * normy;
                    XYZ[2] = (z & 0xFFFF) * normz;

                    XYZ2RGB(XYZ, RGB);

                    dstPixels[dIndex++] = upperr * RGB[0];
                    dstPixels[dIndex++] = upperg * RGB[1];
                    dstPixels[dIndex++] = upperb * RGB[2];
                }
            }
        }

        // Because of 4738524: setPixels should round the provided double
        // value instead of casting
        // If it is fixed, then this piece of code can be removed.
        if (dstType < DataBuffer.TYPE_FLOAT)
            roundValues(dstPixels);

        convertToSigned(dstPixels, dstType);
        dest.setPixels(dest.getMinX(), dest.getMinY(), width, height, dstPixels);
    }

    // convert an int type raster from CIEXYZ to RGB
    private static void CIEXYZToRGBInt(UnpackedImageData src, int[] srcComponentSize,
            WritableRaster dest, int[] destComponentSize, boolean roiContainsTile,
            boolean roiDisjointTile, RandomIter roiIter, Rectangle roiBounds, Range nodata,
            float[] destNodata) {
        int[] xBuf = src.getIntData(0);
        int[] yBuf = src.getIntData(1);
        int[] zBuf = src.getIntData(2);

        // maps the integral XYZ into floating-point
        float normx = (float) (maxXYZ / ((1L << srcComponentSize[0]) - 1));
        float normy = (float) (maxXYZ / ((1L << srcComponentSize[1]) - 1));
        float normz = (float) (maxXYZ / ((1L << srcComponentSize[2]) - 1));

        // the upper bound for each band
        double upperr = 1.0, upperg = 1.0, upperb = 1.0;

        int dstType = dest.getSampleModel().getDataType();

        // for the integer type, re-calculate the bounds
        if (dstType < DataBuffer.TYPE_FLOAT) {
            upperr = (1L << destComponentSize[0]) - 1;
            upperg = (1L << destComponentSize[1]) - 1;
            upperb = (1L << destComponentSize[2]) - 1;
        }

        int height = dest.getHeight();
        int width = dest.getWidth();

        double[] dstPixels = new double[3 * height * width];

        int xStart = src.bandOffsets[0];
        int yStart = src.bandOffsets[1];
        int zStart = src.bandOffsets[2];
        int srcPixelStride = src.pixelStride;
        int srcLineStride = src.lineStride;

        float[] XYZ = new float[3];
        float[] RGB = new float[3];

        int dIndex = 0;

        boolean hasROI = roiIter != null && roiBounds != null;
        boolean hasNodata = nodata != null;

        int destX = dest.getMinX();
        int destY = dest.getMinY();

        double[] destNoDataFinal = getConvertedNodataCIEXYZ(destNodata, DataBuffer.TYPE_SHORT,
                upperr, upperg, upperb, normx, normy, normz);

        if (roiDisjointTile) {
            ImageUtil.fillBackground(dest, dest.getBounds(), destNoDataFinal);
            return;
        }

        if (!hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, xStart += srcLineStride, yStart += srcLineStride, zStart += srcLineStride) {
                for (int i = 0, xIndex = xStart, yIndex = yStart, zIndex = zStart; i < width; i++, xIndex += srcPixelStride, yIndex += srcPixelStride, zIndex += srcPixelStride) {
                    XYZ[0] = (xBuf[xIndex] & 0xFFFFFFFFl) * normx;
                    XYZ[1] = (yBuf[yIndex] & 0xFFFFFFFFl) * normy;
                    XYZ[2] = (zBuf[zIndex] & 0xFFFFFFFFl) * normz;

                    XYZ2RGB(XYZ, RGB);

                    dstPixels[dIndex++] = upperr * RGB[0];
                    dstPixels[dIndex++] = upperg * RGB[1];
                    dstPixels[dIndex++] = upperb * RGB[2];
                }
            }
        } else if (!hasNodata && hasROI) {
            for (int j = 0; j < height; j++, xStart += srcLineStride, yStart += srcLineStride, zStart += srcLineStride) {
                int y0 = j + destY;
                for (int i = 0, xIndex = xStart, yIndex = yStart, zIndex = zStart; i < width; i++, xIndex += srcPixelStride, yIndex += srcPixelStride, zIndex += srcPixelStride) {
                    int x0 = i + destX;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    XYZ[0] = (xBuf[xIndex] & 0xFFFFFFFFl) * normx;
                    XYZ[1] = (yBuf[yIndex] & 0xFFFFFFFFl) * normy;
                    XYZ[2] = (zBuf[zIndex] & 0xFFFFFFFFl) * normz;

                    XYZ2RGB(XYZ, RGB);

                    dstPixels[dIndex++] = upperr * RGB[0];
                    dstPixels[dIndex++] = upperg * RGB[1];
                    dstPixels[dIndex++] = upperb * RGB[2];
                }
            }
        } else if (hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, xStart += srcLineStride, yStart += srcLineStride, zStart += srcLineStride) {
                for (int i = 0, xIndex = xStart, yIndex = yStart, zIndex = zStart; i < width; i++, xIndex += srcPixelStride, yIndex += srcPixelStride, zIndex += srcPixelStride) {

                    int x = xBuf[xIndex];
                    int y = yBuf[yIndex];
                    int z = zBuf[zIndex];

                    boolean notValid = nodata.contains(x) || nodata.contains(y)
                            || nodata.contains(z);

                    if (notValid) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    XYZ[0] = (x & 0xFFFFFFFFl) * normx;
                    XYZ[1] = (y & 0xFFFFFFFFl) * normy;
                    XYZ[2] = (z & 0xFFFFFFFFl) * normz;

                    XYZ2RGB(XYZ, RGB);

                    dstPixels[dIndex++] = upperr * RGB[0];
                    dstPixels[dIndex++] = upperg * RGB[1];
                    dstPixels[dIndex++] = upperb * RGB[2];
                }
            }
        } else {
            for (int j = 0; j < height; j++, xStart += srcLineStride, yStart += srcLineStride, zStart += srcLineStride) {
                int y0 = j + destY;
                for (int i = 0, xIndex = xStart, yIndex = yStart, zIndex = zStart; i < width; i++, xIndex += srcPixelStride, yIndex += srcPixelStride, zIndex += srcPixelStride) {
                    int x0 = i + destX;

                    int x = xBuf[xIndex];
                    int y = yBuf[yIndex];
                    int z = zBuf[zIndex];

                    boolean notValid = nodata.contains(x) || nodata.contains(y)
                            || nodata.contains(z);

                    if (notValid
                            || !(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    XYZ[0] = (x & 0xFFFFFFFFl) * normx;
                    XYZ[1] = (y & 0xFFFFFFFFl) * normy;
                    XYZ[2] = (z & 0xFFFFFFFFl) * normz;

                    XYZ2RGB(XYZ, RGB);

                    dstPixels[dIndex++] = upperr * RGB[0];
                    dstPixels[dIndex++] = upperg * RGB[1];
                    dstPixels[dIndex++] = upperb * RGB[2];
                }
            }
        }

        // Because of 4738524: setPixels should round the provided double
        // value instead of casting
        // If it is fixed, then this piece of code can be removed.
        if (dstType < DataBuffer.TYPE_FLOAT)
            roundValues(dstPixels);

        convertToSigned(dstPixels, dstType);
        dest.setPixels(dest.getMinX(), dest.getMinY(), width, height, dstPixels);
    }

    // convert a float type ratser from CIEXYZ to RGB
    private static void CIEXYZToRGBFloat(UnpackedImageData src, int[] srcComponentSize,
            WritableRaster dest, int[] destComponentSize, boolean roiContainsTile,
            boolean roiDisjointTile, RandomIter roiIter, Rectangle roiBounds, Range nodata,
            float[] destNodata) {
        float[] xBuf = src.getFloatData(0);
        float[] yBuf = src.getFloatData(1);
        float[] zBuf = src.getFloatData(2);

        // the upper bounds for the 3 bands
        double upperr = 1.0, upperg = 1.0, upperb = 1.0;

        int dstType = dest.getSampleModel().getDataType();

        // for the integer type, re-calculate the bounds
        if (dstType < DataBuffer.TYPE_FLOAT) {
            upperr = (1L << destComponentSize[0]) - 1;
            upperg = (1L << destComponentSize[1]) - 1;
            upperb = (1L << destComponentSize[2]) - 1;
        }

        int height = dest.getHeight();
        int width = dest.getWidth();

        double[] dstPixels = new double[3 * height * width];

        int xStart = src.bandOffsets[0];
        int yStart = src.bandOffsets[1];
        int zStart = src.bandOffsets[2];
        int srcPixelStride = src.pixelStride;
        int srcLineStride = src.lineStride;

        float[] XYZ = new float[3];
        float[] RGB = new float[3];

        int dIndex = 0;

        boolean hasROI = roiIter != null && roiBounds != null;
        boolean hasNodata = nodata != null;
        if (hasNodata) {
            nodata = RangeFactory.convertToDoubleRange(nodata);
        }

        int destX = dest.getMinX();
        int destY = dest.getMinY();

        double[] destNoDataFinal = getConvertedNodataCIEXYZ(destNodata, DataBuffer.TYPE_FLOAT,
                upperr, upperg, upperb, 0, 0, 0);

        if (roiDisjointTile) {
            ImageUtil.fillBackground(dest, dest.getBounds(), destNoDataFinal);
            return;
        }

        if (!hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, xStart += srcLineStride, yStart += srcLineStride, zStart += srcLineStride) {
                for (int i = 0, xIndex = xStart, yIndex = yStart, zIndex = zStart; i < width; i++, xIndex += srcPixelStride, yIndex += srcPixelStride, zIndex += srcPixelStride) {
                    XYZ[0] = xBuf[xIndex];
                    XYZ[1] = yBuf[yIndex];
                    XYZ[2] = zBuf[zIndex];

                    XYZ2RGB(XYZ, RGB);

                    dstPixels[dIndex++] = upperr * RGB[0];
                    dstPixels[dIndex++] = upperg * RGB[1];
                    dstPixels[dIndex++] = upperb * RGB[2];
                }
            }
        } else if (!hasNodata && hasROI) {
            for (int j = 0; j < height; j++, xStart += srcLineStride, yStart += srcLineStride, zStart += srcLineStride) {
                int y0 = j + destY;
                for (int i = 0, xIndex = xStart, yIndex = yStart, zIndex = zStart; i < width; i++, xIndex += srcPixelStride, yIndex += srcPixelStride, zIndex += srcPixelStride) {
                    int x0 = i + destX;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    XYZ[0] = xBuf[xIndex];
                    XYZ[1] = yBuf[yIndex];
                    XYZ[2] = zBuf[zIndex];

                    XYZ2RGB(XYZ, RGB);

                    dstPixels[dIndex++] = upperr * RGB[0];
                    dstPixels[dIndex++] = upperg * RGB[1];
                    dstPixels[dIndex++] = upperb * RGB[2];
                }
            }
        } else if (hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, xStart += srcLineStride, yStart += srcLineStride, zStart += srcLineStride) {
                for (int i = 0, xIndex = xStart, yIndex = yStart, zIndex = zStart; i < width; i++, xIndex += srcPixelStride, yIndex += srcPixelStride, zIndex += srcPixelStride) {

                    XYZ[0] = xBuf[xIndex];
                    XYZ[1] = yBuf[yIndex];
                    XYZ[2] = zBuf[zIndex];

                    boolean notValid = nodata.contains(XYZ[0]) || nodata.contains(XYZ[0])
                            || nodata.contains(XYZ[0]);

                    if (notValid) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    XYZ2RGB(XYZ, RGB);

                    dstPixels[dIndex++] = upperr * RGB[0];
                    dstPixels[dIndex++] = upperg * RGB[1];
                    dstPixels[dIndex++] = upperb * RGB[2];
                }
            }
        } else {
            for (int j = 0; j < height; j++, xStart += srcLineStride, yStart += srcLineStride, zStart += srcLineStride) {
                int y0 = j + destY;
                for (int i = 0, xIndex = xStart, yIndex = yStart, zIndex = zStart; i < width; i++, xIndex += srcPixelStride, yIndex += srcPixelStride, zIndex += srcPixelStride) {
                    int x0 = i + destX;

                    XYZ[0] = xBuf[xIndex];
                    XYZ[1] = yBuf[yIndex];
                    XYZ[2] = zBuf[zIndex];

                    boolean notValid = nodata.contains(XYZ[0]) || nodata.contains(XYZ[0])
                            || nodata.contains(XYZ[0]);

                    if (notValid
                            || !(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    XYZ2RGB(XYZ, RGB);

                    dstPixels[dIndex++] = upperr * RGB[0];
                    dstPixels[dIndex++] = upperg * RGB[1];
                    dstPixels[dIndex++] = upperb * RGB[2];
                }
            }
        }

        // Because of 4738524: setPixels should round the provided double
        // value instead of casting
        // If it is fixed, then this piece of code can be removed.
        if (dstType < DataBuffer.TYPE_FLOAT)
            roundValues(dstPixels);

        convertToSigned(dstPixels, dstType);
        dest.setPixels(dest.getMinX(), dest.getMinY(), width, height, dstPixels);
    }

    // convert a double type ratser form CIEXYZ to RGB color space
    private static void CIEXYZToRGBDouble(UnpackedImageData src, int[] srcComponentSize,
            WritableRaster dest, int[] destComponentSize, boolean roiContainsTile,
            boolean roiDisjointTile, RandomIter roiIter, Rectangle roiBounds, Range nodata,
            float[] destNodata) {
        double[] xBuf = src.getDoubleData(0);
        double[] yBuf = src.getDoubleData(1);
        double[] zBuf = src.getDoubleData(2);

        // the upper bound of each band
        double upperr = 1.0, upperg = 1.0, upperb = 1.0;

        int dstType = dest.getSampleModel().getDataType();

        // for the integer type, re-calculate the bounds
        if (dstType < DataBuffer.TYPE_FLOAT) {
            upperr = (1L << destComponentSize[0]) - 1;
            upperg = (1L << destComponentSize[1]) - 1;
            upperb = (1L << destComponentSize[2]) - 1;
        }

        int height = dest.getHeight();
        int width = dest.getWidth();

        double[] dstPixels = new double[3 * height * width];

        int xStart = src.bandOffsets[0];
        int yStart = src.bandOffsets[1];
        int zStart = src.bandOffsets[2];
        int srcPixelStride = src.pixelStride;
        int srcLineStride = src.lineStride;

        float[] XYZ = new float[3];
        float[] RGB = new float[3];

        int dIndex = 0;

        boolean hasROI = roiIter != null && roiBounds != null;
        boolean hasNodata = nodata != null;
        if (hasNodata) {
            nodata = RangeFactory.convertToDoubleRange(nodata);
        }

        int destX = dest.getMinX();
        int destY = dest.getMinY();

        double[] destNoDataFinal = getConvertedNodataCIEXYZ(destNodata, DataBuffer.TYPE_DOUBLE,
                upperr, upperg, upperb, 0, 0, 0);

        if (roiDisjointTile) {
            ImageUtil.fillBackground(dest, dest.getBounds(), destNoDataFinal);
            return;
        }

        if (!hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, xStart += srcLineStride, yStart += srcLineStride, zStart += srcLineStride) {
                for (int i = 0, xIndex = xStart, yIndex = yStart, zIndex = zStart; i < width; i++, xIndex += srcPixelStride, yIndex += srcPixelStride, zIndex += srcPixelStride) {
                    XYZ[0] = (float) xBuf[xIndex];
                    XYZ[1] = (float) yBuf[yIndex];
                    XYZ[2] = (float) zBuf[zIndex];

                    XYZ2RGB(XYZ, RGB);

                    dstPixels[dIndex++] = upperr * RGB[0];
                    dstPixels[dIndex++] = upperg * RGB[1];
                    dstPixels[dIndex++] = upperb * RGB[2];
                }
            }
        } else if (!hasNodata && hasROI) {
            for (int j = 0; j < height; j++, xStart += srcLineStride, yStart += srcLineStride, zStart += srcLineStride) {
                int y0 = j + destY;
                for (int i = 0, xIndex = xStart, yIndex = yStart, zIndex = zStart; i < width; i++, xIndex += srcPixelStride, yIndex += srcPixelStride, zIndex += srcPixelStride) {
                    int x0 = i + destX;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    XYZ[0] = (float) xBuf[xIndex];
                    XYZ[1] = (float) yBuf[yIndex];
                    XYZ[2] = (float) zBuf[zIndex];

                    XYZ2RGB(XYZ, RGB);

                    dstPixels[dIndex++] = upperr * RGB[0];
                    dstPixels[dIndex++] = upperg * RGB[1];
                    dstPixels[dIndex++] = upperb * RGB[2];
                }
            }
        } else if (hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, xStart += srcLineStride, yStart += srcLineStride, zStart += srcLineStride) {
                for (int i = 0, xIndex = xStart, yIndex = yStart, zIndex = zStart; i < width; i++, xIndex += srcPixelStride, yIndex += srcPixelStride, zIndex += srcPixelStride) {

                    XYZ[0] = (float) xBuf[xIndex];
                    XYZ[1] = (float) yBuf[yIndex];
                    XYZ[2] = (float) zBuf[zIndex];

                    boolean notValid = nodata.contains(xBuf[xIndex])
                            || nodata.contains(yBuf[yIndex]) || nodata.contains(zBuf[zIndex]);

                    if (notValid) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    XYZ2RGB(XYZ, RGB);

                    dstPixels[dIndex++] = upperr * RGB[0];
                    dstPixels[dIndex++] = upperg * RGB[1];
                    dstPixels[dIndex++] = upperb * RGB[2];
                }
            }
        } else {
            for (int j = 0; j < height; j++, xStart += srcLineStride, yStart += srcLineStride, zStart += srcLineStride) {
                int y0 = j + destY;
                for (int i = 0, xIndex = xStart, yIndex = yStart, zIndex = zStart; i < width; i++, xIndex += srcPixelStride, yIndex += srcPixelStride, zIndex += srcPixelStride) {
                    int x0 = i + destX;

                    XYZ[0] = (float) xBuf[xIndex];
                    XYZ[1] = (float) yBuf[yIndex];
                    XYZ[2] = (float) zBuf[zIndex];

                    boolean notValid = nodata.contains(xBuf[xIndex])
                            || nodata.contains(yBuf[yIndex]) || nodata.contains(zBuf[zIndex]);

                    if (notValid
                            || !(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    XYZ2RGB(XYZ, RGB);

                    dstPixels[dIndex++] = upperr * RGB[0];
                    dstPixels[dIndex++] = upperg * RGB[1];
                    dstPixels[dIndex++] = upperb * RGB[2];
                }
            }
        }

        // Because of 4738524: setPixels should round the provided double
        // value instead of casting
        // If it is fixed, then this piece of code can be removed.
        if (dstType < DataBuffer.TYPE_FLOAT)
            roundValues(dstPixels);

        convertToSigned(dstPixels, dstType);
        dest.setPixels(dest.getMinX(), dest.getMinY(), width, height, dstPixels);
    }

    private static void roundValues(double[] data) {
        for (int i = 0; i < data.length; i++)
            data[i] = (long) (data[i] + 0.5);
    }

    private static double[] getConvertedNodataB(boolean isInt, float[] destNodata, int normr,
            int normg, int normb, double normx, double normy, double normz) {
        double[] result = new double[3];
        double R = LUT[((int) destNodata[0] & 0xFF) << normr];
        double G = LUT[((int) destNodata[1] & 0xFF) << normg];
        double B = LUT[((int) destNodata[2] & 0xFF) << normb];

        if (isInt) {
            result[0] = (0.45593763 * R + 0.39533819 * G + 0.19954964 * B) * normx;
            result[1] = (0.23157515 * R + 0.77905262 * G + 0.07864978 * B) * normy;
            result[2] = (0.01593493 * R + 0.09841772 * G + 0.78488615 * B) * normz;
        } else {
            result[0] = 0.45593763 * R + 0.39533819 * G + 0.19954964 * B;
            result[1] = 0.23157515 * R + 0.77905262 * G + 0.07864978 * B;
            result[2] = 0.01593493 * R + 0.09841772 * G + 0.78488615 * B;
        }

        return result;
    }

    private static double[] getConvertedNodata(boolean isInt, float[] destNodata, int dataType,
            double normr, double normg, double normb, double normx, double normy, double normz) {

        double[] result = new double[3];
        float[] RGB = new float[3];
        float[] XYZ = new float[3];

        switch (dataType) {
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT:
            RGB[0] = (float) (((int) destNodata[0] & 0xFFFF) / normr);
            RGB[1] = (float) (((int) destNodata[1] & 0xFFFF) / normg);
            RGB[2] = (float) (((int) destNodata[2] & 0xFFFF) / normb);
            break;
        case DataBuffer.TYPE_INT:
            RGB[0] = (float) (((int) destNodata[0] & 0xFFFFFFFFl) / normr);
            RGB[1] = (float) (((int) destNodata[1] & 0xFFFFFFFFl) / normg);
            RGB[2] = (float) (((int) destNodata[2] & 0xFFFFFFFFl) / normb);
            break;
        case DataBuffer.TYPE_FLOAT:
        case DataBuffer.TYPE_DOUBLE:
            RGB[0] = destNodata[0];
            RGB[1] = destNodata[1];
            RGB[2] = destNodata[2];
            break;
        default:
            throw new IllegalArgumentException("Wrong data type defined");
        }

        RGB2XYZ(RGB, XYZ);

        if (isInt) {
            result[0] = XYZ[0] * normx;
            result[1] = XYZ[1] * normy;
            result[2] = XYZ[2] * normz;
        } else {
            result[0] = XYZ[0];
            result[1] = XYZ[1];
            result[2] = XYZ[2];
        }

        return result;
    }

    private static double[] getConvertedNodataCIEXYZ(float[] destNodata, int dataType,
            double upperr, double upperg, double upperb, double normx, double normy, double normz) {

        double[] result = new double[3];
        float[] RGB = new float[3];
        float[] XYZ = new float[3];

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            XYZ[0] = (float) (((int) destNodata[0] & 0xFF) * normx);
            XYZ[1] = (float) (((int) destNodata[1] & 0xFF) * normy);
            XYZ[2] = (float) (((int) destNodata[2] & 0xFF) * normz);
            break;
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT:
            XYZ[0] = (float) (((int) destNodata[0] & 0xFFFF) * normx);
            XYZ[1] = (float) (((int) destNodata[1] & 0xFFFF) * normy);
            XYZ[2] = (float) (((int) destNodata[2] & 0xFFFF) * normz);
            break;
        case DataBuffer.TYPE_INT:
            XYZ[0] = (float) (((int) destNodata[0] & 0xFFFFFFFFl) * normx);
            XYZ[1] = (float) (((int) destNodata[1] & 0xFFFFFFFFl) * normy);
            XYZ[2] = (float) (((int) destNodata[2] & 0xFFFFFFFFl) * normz);
            break;
        case DataBuffer.TYPE_FLOAT:
        case DataBuffer.TYPE_DOUBLE:
            XYZ[0] = destNodata[0];
            XYZ[1] = destNodata[1];
            XYZ[2] = destNodata[2];
            break;
        default:
            throw new IllegalArgumentException("Wrong data type defined");
        }

        XYZ2RGB(XYZ, RGB);

        result[0] = upperr * RGB[0];
        result[1] = upperg * RGB[1];
        result[2] = upperb * RGB[2];

        return result;
    }
}
