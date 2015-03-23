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
import java.awt.color.ColorSpace;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.lang.ref.SoftReference;

import javax.media.jai.PixelAccessor;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RasterFactory;
import javax.media.jai.UnpackedImageData;
import javax.media.jai.iterator.RandomIter;

import com.sun.media.jai.util.ImageUtil;

/**
 * Singleton class representing the IHS (<i>I</i>ntensity, <i>H</i>ue, <i>S</i>aturation) color space (also known as HSI or HIS).
 * 
 * <p>
 * The RGB-to-IHS transformation is defined by the equations:
 * <ul>
 * <li>I = (R+G+B)/3</li>
 * <li>S = 1-min(R, G, B)/I</li>
 * <li>H = G > B ? h : 2*<code>PI</code> - h, where <br>
 * h = cos<sup>-1</sup>{[(R-G)+(R-B)]/2[(R-G)<sup>2</sup>+ (R-G)(G-B)]<sup>1/2</sup>}</li>
 * </ul>
 * where the R, G, B values have been normalized to the range [0.0,&nbsp;1.0]. Refer to <i>Practical Algorithms for Image Analysis</i>, Seul, et. al.,
 * (Cambridge, 2000), pages 50-55, for more information.
 * 
 * The inverse transformation (IHS-to-RGB) is defined as:
 * 
 * <p>
 * When <code>H</code> is in <code>[2PI/3, 4PI/3]</code>, <code>R</code> should be the smallest. Then, there exists
 * <p>
 * <code>R = (1-S)I</code>
 * <p>
 * and <code><ul><li>G = (c1 + c2)/2</li>
 *    <li>B = (c1 - c2)/2</li></ul></code>
 * <p>
 * where <code>c1 = 3I-R</code> and <code>c2 = sqrt(3)(R-I)tg(H)</code>
 * 
 * <p>
 * when <code>H</code> is in <code>[4PI/3, 2PI]</code>,
 * 
 * <p>
 * <code>G = (1-S)I</code>
 * <p>
 * and <code><ul><li>B = (c1 + c2)/2</li>
 *        <li>R = (c1 - c2)/2</li></ul></code>
 * <p>
 * where c1 = 3I-G and c2 = sqrt(3)(G-I)tg(H-2PI/3)
 * 
 * <p>
 * when <code>H</code> is in <code>[0, 2PI/3]</code>,
 * 
 * <p>
 * <code>B = (1-S)I</code>
 * <p>
 * and <code><ul><li>R = (c1 + c2)/2</li>
 * 	 <li>G = (c1 - c2)/2</li></ul></code>
 * 
 * <p>
 * where <code>c1 = 3I-B</code> and <code>c2 = sqrt(3)(B-I)tg(H-4PI/3)</code>
 * <p>
 * Methods defined in the superclasses are not commented extensively.
 * 
 */
public class IHSColorSpaceJAIExt extends ColorSpaceJAIExt {

    /** Constant indicating that the inner random iterators must pre-calculate an array of the image positions */
    public static final boolean ARRAY_CALC = true;

    /** Constant indicating that the inner random iterators must cache the current tile position */
    public static final boolean TILE_CACHED = true;

    // Constant for rgb-to-ihs
    private static final double PI2 = Math.PI * 2.0;

    // Constants for ihs-to-rgb
    private static final double PI23 = PI2 / 3.0;

    private static final double PI43 = PI23 * 2.0;

    private static final double SQRT3 = Math.sqrt(3.0);

    private final static double BYTESCALE = 127.5 / Math.PI;

    private static SoftReference reference = new SoftReference(null);

    /* tables and their softrefrences */
    private static byte[] acosTable = null;

    private static double[] sqrtTable = null;

    private static double[] tanTable = null;

    private static SoftReference acosSoftRef;

    private static SoftReference sqrtSoftRef;

    private static SoftReference tanSoftRef;

    /**
     * Retrieves the unique instance of this class the construction of which is deferred until the first invocation of this method.
     */
    public static IHSColorSpaceJAIExt getInstance() {
        synchronized (reference) {
            Object referent = reference.get();
            IHSColorSpaceJAIExt cs;
            if (referent == null) {
                // First invocation or SoftReference has been cleared.
                reference = new SoftReference(cs = new IHSColorSpaceJAIExt());
            } else {
                // SoftReference has not been cleared.
                cs = (IHSColorSpaceJAIExt) referent;
            }

            return cs;
        }
    }

    /**
     * Constructs an instance of this class with <code>type</code> <code>ColorSpace.TYPE_HSV</code>, 3 components, and preferred intermediary space
     * sRGB.
     */
    public IHSColorSpaceJAIExt() {
        super(ColorSpace.TYPE_HSV, 3, true);
    }

    // generate the cos table used in RGBtoIHS for byte type
    private synchronized void generateACosTable() {

        if ((acosSoftRef == null) || (acosSoftRef.get() == null)) {
            acosTable = new byte[1001];
            acosSoftRef = new SoftReference(acosTable);

            for (int i = 0; i <= 1000; i++)
                acosTable[i] = (byte) (BYTESCALE * Math.acos((i - 500) * 0.002) + 0.5);
        }
    }

    // generate the square root table used in RGBtoIHS for byte type
    private synchronized void generateSqrtTable() {
        if ((sqrtSoftRef == null) || (sqrtSoftRef.get() == null)) {
            sqrtTable = new double[1001];
            sqrtSoftRef = new SoftReference(sqrtTable);

            for (int i = 0; i <= 1000; i++)
                sqrtTable[i] = Math.sqrt(i / 1000.0);
        }
    }

    // generate the tangent table used in IHStoRGB for byte type
    private synchronized void generateTanTable() {
        if ((tanSoftRef == null) || (tanSoftRef.get() == null)) {
            tanTable = new double[256];
            tanSoftRef = new SoftReference(tanTable);

            for (int i = 0; i < 256; i++)
                tanTable[i] = Math.tan(i * PI2 / 255.0);
        }
    }

    /**
     * Converts a single color value from CIEXYZ to IHS.
     */
    public float[] fromCIEXYZ(float[] colorValue) {
        float[] rgb = new float[3];
        XYZ2RGB(colorValue, rgb);

        float r = rgb[0];
        float g = rgb[1];
        float b = rgb[2];

        float[] ihs = new float[3];

        ihs[0] = (r + g + b) / 3.0f;
        float drg = r - g;
        float drb = r - b;
        float temp = (float) Math.sqrt(drg * (double) drg + drb * (double) (drb - drg));

        // when temp is zero, R=G=B. Hue should be NaN. To make
        // numerically consistent, set it to 2PI
        if (temp != 0.0f) {
            temp = (float) Math.acos((drg + drb) / (double) temp / 2);
            if (g < b)
                ihs[1] = (float) (PI2 - temp);
            else
                ihs[1] = temp;
        } else
            ihs[1] = (float) PI2;

        float min = (r < g) ? r : g;
        min = (min < b) ? min : b;

        // when intensity is 0, means R=G=B=0. S can be set to 0 to indicate
        // R=G=B.
        if (ihs[0] == 0.0f)
            ihs[2] = 0.0f;
        else
            ihs[2] = 1.0f - min / ihs[0];

        return ihs;
    }

    /**
     * Converts a single color value from sRGB to IHS.
     */
    public float[] fromRGB(float[] rgbValue) {
        float r = rgbValue[0];
        float g = rgbValue[1];
        float b = rgbValue[2];

        r = (r < 0.0f) ? 0.0f : ((r > 1.0f) ? 1.0f : r);
        g = (g < 0.0f) ? 0.0f : ((g > 1.0f) ? 1.0f : g);
        b = (b < 0.0f) ? 0.0f : ((b > 1.0f) ? 1.0f : b);

        float[] ihs = new float[3];

        ihs[0] = (r + g + b) / 3.0f;
        float drg = r - g;
        float drb = r - b;
        float temp = (float) Math.sqrt(drg * (double) drg + drb * (double) (drb - drg));

        // when temp is zero, R=G=B. Hue should be NaN. To make
        // numerically consistent, set it to 2PI
        if (temp != 0.0f) {
            temp = (float) Math.acos((drg + drb) / (double) temp / 2);
            if (g < b)
                ihs[1] = (float) (PI2 - temp);
            else
                ihs[1] = temp;
        } else
            ihs[1] = (float) PI2;

        float min = (r < g) ? r : g;
        min = (min < b) ? min : b;

        // when intensity is 0, means R=G=B=0. S can be set to 0 to indicate
        // R=G=B.
        if (ihs[0] == 0.0f)
            ihs[2] = 0.0f;
        else
            ihs[2] = 1.0f - min / ihs[0];

        return ihs;
    }

    /**
     * Converts a single color value from IHS to CIEXYZ.
     */
    public float[] toCIEXYZ(float[] colorValue) {
        float i = colorValue[0];
        float h = colorValue[1];
        float s = colorValue[2];

        i = (i < 0.0f) ? 0.0f : ((i > 1.0f) ? 1.0f : i);
        h = (h < 0.0f) ? 0.0f : ((h > (float) PI2) ? (float) PI2 : h);
        s = (s < 0.0f) ? 0.0f : ((s > 1.0f) ? 1.0f : s);

        float r = 0.0f, g = 0.0f, b = 0.0f;

        // when the saturation is zero, means this color is grey color.
        // so R=G=B.
        if (s == 0.0f) {
            r = g = b = i;
        } else {
            if (h >= PI23 && h < PI43) {
                r = (1 - s) * i;
                float c1 = 3 * i - r;
                float c2 = (float) (SQRT3 * (r - i) * Math.tan(h));
                g = (c1 + c2) / 2;
                b = (c1 - c2) / 2;
            } else if (h > PI43) {
                g = (1 - s) * i;
                float c1 = 3 * i - g;
                float c2 = (float) (SQRT3 * (g - i) * Math.tan(h - PI23));
                b = (c1 + c2) / 2;
                r = (c1 - c2) / 2;
            } else if (h < PI23) {
                b = (1 - s) * i;
                float c1 = 3 * i - b;
                float c2 = (float) (SQRT3 * (b - i) * Math.tan(h - PI43));
                r = (c1 + c2) / 2;
                g = (c1 - c2) / 2;
            }
        }

        float[] xyz = new float[3];
        float[] rgb = new float[3];
        rgb[0] = r;
        rgb[1] = g;
        rgb[2] = b;

        RGB2XYZ(rgb, xyz);

        return xyz;
    }

    /**
     * Converts a single color value from IHS to sRGB.
     */
    public float[] toRGB(float[] colorValue) {
        float i = colorValue[0];
        float h = colorValue[1];
        float s = colorValue[2];

        i = (i < 0.0f) ? 0.0f : ((i > 1.0f) ? 1.0f : i);
        h = (h < 0.0f) ? 0.0f : ((h > (float) PI2) ? (float) PI2 : h);
        s = (s < 0.0f) ? 0.0f : ((s > 1.0f) ? 1.0f : s);

        float[] rgb = new float[3];

        // when the saturation is 0, the color is grey. so R=G=B=I.
        if (s == 0.0f) {
            rgb[0] = rgb[1] = rgb[2] = i;
        } else {
            if (h >= PI23 && h <= PI43) {
                float r = (1 - s) * i;
                float c1 = 3 * i - r;
                float c2 = (float) (SQRT3 * (r - i) * Math.tan(h));
                rgb[0] = r;
                rgb[1] = (c1 + c2) / 2;
                rgb[2] = (c1 - c2) / 2;
            } else if (h > PI43) {
                float g = (1 - s) * i;
                float c1 = 3 * i - g;
                float c2 = (float) (SQRT3 * (g - i) * Math.tan(h - PI23));
                rgb[0] = (c1 - c2) / 2;
                rgb[1] = g;
                rgb[2] = (c1 + c2) / 2;
            } else if (h < PI23) {
                float b = (1 - s) * i;
                float c1 = 3 * i - b;
                float c2 = (float) (SQRT3 * (b - i) * Math.tan(h - PI43));
                rgb[0] = (c1 + c2) / 2;
                rgb[1] = (c1 - c2) / 2;
                rgb[2] = b;
            }
        }

        return rgb;
    }

    /**
     * Converts a <code>Raster</code> of colors represented as pixels from CIEXYZ to IHS.
     */
    public WritableRaster fromCIEXYZ(Raster src, int[] srcComponentSize, WritableRaster dest,
            int[] destComponentSize, ROI roi, Range nodata, float[] destNodata) {
        WritableRaster tempRas = CIEXYZToRGB(src, srcComponentSize, null, null, roi, nodata,
                destNodata);
        return fromRGB(tempRas, tempRas.getSampleModel().getSampleSize(), dest, destComponentSize,
                roi, nodata, destNodata);
    }

    /**
     * Converts a <code>Raster</code> of colors represented as pixels from sRGB to IHS.
     */
    public WritableRaster fromRGB(Raster src, int[] srcComponentSize, WritableRaster dest,
            int[] destComponentSize, ROI roi, Range nodata, float[] destNodata) {
        // Validate the parameters
        checkParameters(src, srcComponentSize, dest, destComponentSize);

        // ROI check
        ROI roiTile = null;

        RandomIter roiIter = null;

        boolean roiContainsTile = false;
        boolean roiDisjointTile = false;

        // If a ROI is present, then only the part contained inside the current tile bounds is taken.
        Rectangle bounds = null;
        if (roi != null) {
            bounds = roi.getBounds();
            Rectangle srcRectExpanded = src.getBounds();
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

        // When the parameter is not provided by the caller, set it as the
        // sample sizes of the source
        if (srcComponentSize == null)
            srcComponentSize = srcSampleModel.getSampleSize();

        // when the destination ratser is not provided, create a new one
        if (dest == null) {
            Point origin = new Point(src.getMinX(), src.getMinY());
            dest = RasterFactory.createWritableRaster(srcSampleModel, origin);
        }

        SampleModel dstSampleModel = dest.getSampleModel();
        if (destComponentSize == null)
            destComponentSize = dstSampleModel.getSampleSize();

        PixelAccessor srcAcc = new PixelAccessor(srcSampleModel, null);
        UnpackedImageData srcUid = srcAcc.getPixels(src, src.getBounds(),
                srcSampleModel.getDataType(), false);

        switch (srcSampleModel.getDataType()) {

        case DataBuffer.TYPE_BYTE:
            fromRGBByte(srcUid, srcComponentSize, dest, destComponentSize, roiContainsTile,
                    roiDisjointTile, roiIter, bounds, nodata, destNodata);
            break;
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            fromRGBShort(srcUid, srcComponentSize, dest, destComponentSize, roiContainsTile,
                    roiDisjointTile, roiIter, bounds, nodata, destNodata);
            break;
        case DataBuffer.TYPE_INT:
            fromRGBInt(srcUid, srcComponentSize, dest, destComponentSize, roiContainsTile,
                    roiDisjointTile, roiIter, bounds, nodata, destNodata);
            break;
        case DataBuffer.TYPE_FLOAT:
            fromRGBFloat(srcUid, srcComponentSize, dest, destComponentSize, roiContainsTile,
                    roiDisjointTile, roiIter, bounds, nodata, destNodata);
            break;
        case DataBuffer.TYPE_DOUBLE:
            fromRGBDouble(srcUid, srcComponentSize, dest, destComponentSize, roiContainsTile,
                    roiDisjointTile, roiIter, bounds, nodata, destNodata);
            break;
        }
        return dest;
    }

    // convert a byte type raster from RGB to IHS
    private void fromRGBByte(UnpackedImageData src, int[] srcComponentSize, WritableRaster dest,
            int[] destComponentSize, boolean roiContainsTile, boolean roiDisjointTile,
            RandomIter roiIter, Rectangle roiBounds, Range nodata, float[] destNodata) {
        byte[] rBuf = src.getByteData(0);
        byte[] gBuf = src.getByteData(1);
        byte[] bBuf = src.getByteData(2);

        int normr = 8 - srcComponentSize[0];
        int normg = 8 - srcComponentSize[1];
        int normb = 8 - srcComponentSize[2];

        double normi = 1.0 / 255.0, normh = 1.0, norms = 1.0;

        int bnormi = 0, bnormh = 0, bnorms = 0;

        int dstType = dest.getSampleModel().getDataType();
        boolean isByte = (dstType == DataBuffer.TYPE_BYTE);

        if (isByte) {
            bnormi = 8 - destComponentSize[0];
            bnormh = 8 - destComponentSize[1];
            bnorms = 8 - destComponentSize[2];
            generateACosTable();
            generateSqrtTable();
        } else if (dstType < DataBuffer.TYPE_FLOAT) {
            normi = ((1l << destComponentSize[0]) - 1) / 255.0;
            normh = ((1l << destComponentSize[1]) - 1) / PI2;
            norms = ((1l << destComponentSize[2]) - 1);
        }

        int height = dest.getHeight();
        int width = dest.getWidth();

        double[] dstPixels = null;
        int[] dstIntPixels = null;

        if (isByte)
            dstIntPixels = new int[3 * height * width];
        else
            dstPixels = new double[3 * height * width];

        int rStart = src.bandOffsets[0];
        int gStart = src.bandOffsets[1];
        int bStart = src.bandOffsets[2];
        int srcPixelStride = src.pixelStride;
        int srcLineStride = src.lineStride;

        int dIndex = 0;

        if (destNodata == null) {
            destNodata = new float[3];
        }

        boolean hasROI = roiIter != null && roiBounds != null;
        boolean hasNodata = nodata != null;

        int destX = dest.getMinX();
        int destY = dest.getMinY();

        double[] destNoDataFinal = getConvertedNodataBfromRGB(isByte, destNodata, normr, normg,
                normb, bnormi, bnormh, bnorms, normi, normh, norms);

        if (roiDisjointTile) {
            ImageUtil.fillBackground(dest, dest.getBounds(), destNoDataFinal);
            return;
        }

        if (!hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {
                    short R = (short) ((rBuf[rIndex] & 0xFF) << normr);
                    short G = (short) ((gBuf[gIndex] & 0xFF) << normg);
                    short B = (short) ((bBuf[bIndex] & 0xFF) << normb);

                    if (isByte) {
                        float intensity = (R + G + B) / 3.0f;
                        dstIntPixels[dIndex++] = ((short) (intensity + 0.5f)) >> bnormi;

                        short drg = (short) (R - G);
                        short drb = (short) (R - B);

                        int tint = drg * drg + drb * (drb - drg);

                        short sum = (short) (drg + drb);
                        double temp;
                        if (tint != 0)
                            temp = sqrtTable[(int) (250.0 * sum * sum / tint + 0.5)];
                        else
                            temp = -1.0;

                        int hue;
                        if (sum > 0)
                            hue = acosTable[(int) (500 * temp + 0.5) + 500];
                        else
                            hue = acosTable[(int) (-500 * temp - 0.5) + 500];

                        if (B >= G)
                            dstIntPixels[dIndex++] = (255 - hue) >> bnormh;
                        else
                            dstIntPixels[dIndex++] = hue >> bnormh;

                        short min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstIntPixels[dIndex++] = (255 - (int) (255 * min / intensity + 0.5f)) >> bnorms;
                    } else {
                        float intensity = (R + G + B) / 3.0f;
                        dstPixels[dIndex++] = normi * intensity;

                        double drg = R - G;
                        double drb = R - B;
                        double temp = Math.sqrt(drg * drg + drb * (drb - drg));

                        if (temp != 0) {
                            temp = Math.acos((drg + drb) / temp / 2);
                            if (B >= G)
                                temp = PI2 - temp;
                        } else
                            temp = PI2;

                        dstPixels[dIndex++] = normh * temp;

                        double min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstPixels[dIndex++] = norms * (1 - min / intensity);
                    }
                }
            }
        } else if (hasROI && !hasNodata) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                int y0 = j + destY;
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {
                    int x0 = i + destX;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        if (isByte) {
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[0];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[1];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[2];
                        } else {
                            dstPixels[dIndex++] = destNoDataFinal[0];
                            dstPixels[dIndex++] = destNoDataFinal[1];
                            dstPixels[dIndex++] = destNoDataFinal[2];
                        }
                        continue;
                    }

                    short R = (short) ((rBuf[rIndex] & 0xFF) << normr);
                    short G = (short) ((gBuf[gIndex] & 0xFF) << normg);
                    short B = (short) ((bBuf[bIndex] & 0xFF) << normb);

                    if (isByte) {
                        float intensity = (R + G + B) / 3.0f;
                        dstIntPixels[dIndex++] = ((short) (intensity + 0.5f)) >> bnormi;

                        short drg = (short) (R - G);
                        short drb = (short) (R - B);

                        int tint = drg * drg + drb * (drb - drg);

                        short sum = (short) (drg + drb);
                        double temp;
                        if (tint != 0)
                            temp = sqrtTable[(int) (250.0 * sum * sum / tint + 0.5)];
                        else
                            temp = -1.0;

                        int hue;
                        if (sum > 0)
                            hue = acosTable[(int) (500 * temp + 0.5) + 500];
                        else
                            hue = acosTable[(int) (-500 * temp - 0.5) + 500];

                        if (B >= G)
                            dstIntPixels[dIndex++] = (255 - hue) >> bnormh;
                        else
                            dstIntPixels[dIndex++] = hue >> bnormh;

                        short min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstIntPixels[dIndex++] = (255 - (int) (255 * min / intensity + 0.5f)) >> bnorms;
                    } else {
                        float intensity = (R + G + B) / 3.0f;
                        dstPixels[dIndex++] = normi * intensity;

                        double drg = R - G;
                        double drb = R - B;
                        double temp = Math.sqrt(drg * drg + drb * (drb - drg));

                        if (temp != 0) {
                            temp = Math.acos((drg + drb) / temp / 2);
                            if (B >= G)
                                temp = PI2 - temp;
                        } else
                            temp = PI2;

                        dstPixels[dIndex++] = normh * temp;

                        double min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstPixels[dIndex++] = norms * (1 - min / intensity);
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
                        if (isByte) {
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[0];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[1];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[2];
                        } else {
                            dstPixels[dIndex++] = destNoDataFinal[0];
                            dstPixels[dIndex++] = destNoDataFinal[1];
                            dstPixels[dIndex++] = destNoDataFinal[2];
                        }
                        continue;
                    }

                    short R = (short) ((r & 0xFF) << normr);
                    short G = (short) ((g & 0xFF) << normg);
                    short B = (short) ((b & 0xFF) << normb);

                    if (isByte) {
                        float intensity = (R + G + B) / 3.0f;
                        dstIntPixels[dIndex++] = ((short) (intensity + 0.5f)) >> bnormi;

                        short drg = (short) (R - G);
                        short drb = (short) (R - B);

                        int tint = drg * drg + drb * (drb - drg);

                        short sum = (short) (drg + drb);
                        double temp;
                        if (tint != 0)
                            temp = sqrtTable[(int) (250.0 * sum * sum / tint + 0.5)];
                        else
                            temp = -1.0;

                        int hue;
                        if (sum > 0)
                            hue = acosTable[(int) (500 * temp + 0.5) + 500];
                        else
                            hue = acosTable[(int) (-500 * temp - 0.5) + 500];

                        if (B >= G)
                            dstIntPixels[dIndex++] = (255 - hue) >> bnormh;
                        else
                            dstIntPixels[dIndex++] = hue >> bnormh;

                        short min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstIntPixels[dIndex++] = (255 - (int) (255 * min / intensity + 0.5f)) >> bnorms;
                    } else {
                        float intensity = (R + G + B) / 3.0f;
                        dstPixels[dIndex++] = normi * intensity;

                        double drg = R - G;
                        double drb = R - B;
                        double temp = Math.sqrt(drg * drg + drb * (drb - drg));

                        if (temp != 0) {
                            temp = Math.acos((drg + drb) / temp / 2);
                            if (B >= G)
                                temp = PI2 - temp;
                        } else
                            temp = PI2;

                        dstPixels[dIndex++] = normh * temp;

                        double min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstPixels[dIndex++] = norms * (1 - min / intensity);
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
                        if (isByte) {
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[0];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[1];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[2];
                        } else {
                            dstPixels[dIndex++] = destNoDataFinal[0];
                            dstPixels[dIndex++] = destNoDataFinal[1];
                            dstPixels[dIndex++] = destNoDataFinal[2];
                        }

                        continue;
                    }

                    short R = (short) ((r & 0xFF) << normr);
                    short G = (short) ((g & 0xFF) << normg);
                    short B = (short) ((b & 0xFF) << normb);

                    if (isByte) {
                        float intensity = (R + G + B) / 3.0f;
                        dstIntPixels[dIndex++] = ((short) (intensity + 0.5f)) >> bnormi;

                        short drg = (short) (R - G);
                        short drb = (short) (R - B);

                        int tint = drg * drg + drb * (drb - drg);

                        short sum = (short) (drg + drb);
                        double temp;
                        if (tint != 0)
                            temp = sqrtTable[(int) (250.0 * sum * sum / tint + 0.5)];
                        else
                            temp = -1.0;

                        int hue;
                        if (sum > 0)
                            hue = acosTable[(int) (500 * temp + 0.5) + 500];
                        else
                            hue = acosTable[(int) (-500 * temp - 0.5) + 500];

                        if (B >= G)
                            dstIntPixels[dIndex++] = (255 - hue) >> bnormh;
                        else
                            dstIntPixels[dIndex++] = hue >> bnormh;

                        short min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstIntPixels[dIndex++] = (255 - (int) (255 * min / intensity + 0.5f)) >> bnorms;
                    } else {
                        float intensity = (R + G + B) / 3.0f;
                        dstPixels[dIndex++] = normi * intensity;

                        double drg = R - G;
                        double drb = R - B;
                        double temp = Math.sqrt(drg * drg + drb * (drb - drg));

                        if (temp != 0) {
                            temp = Math.acos((drg + drb) / temp / 2);
                            if (B >= G)
                                temp = PI2 - temp;
                        } else
                            temp = PI2;

                        dstPixels[dIndex++] = normh * temp;

                        double min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstPixels[dIndex++] = norms * (1 - min / intensity);
                    }
                }
            }
        }
        if (isByte)
            dest.setPixels(dest.getMinX(), dest.getMinY(), width, height, dstIntPixels);
        else {
            convertToSigned(dstPixels, dstType);
            dest.setPixels(dest.getMinX(), dest.getMinY(), width, height, dstPixels);
        }

    }

    // convert a short type ratser from RGB to IHS
    private void fromRGBShort(UnpackedImageData src, int[] srcComponentSize, WritableRaster dest,
            int[] destComponentSize, boolean roiContainsTile, boolean roiDisjointTile,
            RandomIter roiIter, Rectangle roiBounds, Range nodata, float[] destNodata) {
        short[] rBuf = src.getShortData(0);
        short[] gBuf = src.getShortData(1);
        short[] bBuf = src.getShortData(2);

        // used to left-shift the input to fill all the bits
        int normr = 16 - srcComponentSize[0];
        int normg = 16 - srcComponentSize[1];
        int normb = 16 - srcComponentSize[2];

        // map the output to the desired range
        double normi = 1.0 / 65535.0, normh = 1.0, norms = 1.0;

        // right-shift the output values to the desired range
        int bnormi = 0, bnormh = 0, bnorms = 0;

        int dstType = dest.getSampleModel().getDataType();
        boolean isByte = (dstType == DataBuffer.TYPE_BYTE);

        if (isByte) {
            bnormi = 16 - destComponentSize[0];
            bnormh = 8 - destComponentSize[1];
            bnorms = 8 - destComponentSize[2];
            generateACosTable();
            generateSqrtTable();
        } else if (dstType < DataBuffer.TYPE_FLOAT) {
            normi = ((1l << destComponentSize[0]) - 1) / 65535.0;
            normh = ((1l << destComponentSize[1]) - 1) / PI2;
            norms = ((1l << destComponentSize[2]) - 1);
        }

        int height = dest.getHeight();
        int width = dest.getWidth();

        double[] dstPixels = null;
        int[] dstIntPixels = null;

        if (isByte)
            dstIntPixels = new int[3 * height * width];
        else
            dstPixels = new double[3 * height * width];

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

        double[] destNoDataFinal = getConvertedNodataIfromRGB(isByte, destNodata, normr, normg,
                normb, bnormi, bnormh, bnorms, normi, normh, norms, false);

        if (roiDisjointTile) {
            ImageUtil.fillBackground(dest, dest.getBounds(), destNoDataFinal);
            return;
        }

        if (!hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {
                    int R = (rBuf[rIndex] & 0xFFFF) << normr;
                    int G = (gBuf[gIndex] & 0xFFFF) << normg;
                    int B = (bBuf[bIndex] & 0xFFFF) << normb;

                    if (isByte) {
                        float intensity = (R + G + B) / 3.0f;
                        dstIntPixels[dIndex++] = ((int) (intensity + 0.5f)) >> bnormi;

                        int drg = R - G;
                        int drb = R - B;

                        double tint = drg * (double) drg + drb * (double) (drb - drg);

                        double sum = drg + drb;
                        double temp;
                        if (tint != 0)
                            temp = sqrtTable[(int) (250.0 * sum * sum / tint + 0.5)];
                        else
                            temp = -1.0;

                        int hue;
                        if (sum > 0)
                            hue = acosTable[(int) (500 * temp + 0.5) + 500];
                        else
                            hue = acosTable[(int) (-500 * temp - 0.5) + 500];

                        if (B >= G)
                            dstIntPixels[dIndex++] = (255 - hue) >> bnormh;
                        else
                            dstIntPixels[dIndex++] = hue >> bnormh;

                        int min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstIntPixels[dIndex++] = (255 - (int) (255 * min / intensity + 0.5f)) >> bnorms;

                    } else {
                        float intensity = (R + G + B) / 3.0f;
                        dstPixels[dIndex++] = normi * intensity;

                        double drg = R - G;
                        double drb = R - B;
                        double temp = Math.sqrt(drg * drg + drb * (drb - drg));

                        if (temp != 0) {
                            temp = Math.acos((drg + drb) / temp / 2);
                            if (B >= G)
                                temp = PI2 - temp;
                        } else
                            temp = PI2;

                        dstPixels[dIndex++] = normh * temp;

                        double min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstPixels[dIndex++] = norms * (1 - min / intensity);
                    }
                }
            }
        } else if (hasROI && !hasNodata) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                int y0 = j + destY;
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {
                    int x0 = i + destX;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        if (isByte) {
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[0];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[1];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[2];
                        } else {
                            dstPixels[dIndex++] = destNoDataFinal[0];
                            dstPixels[dIndex++] = destNoDataFinal[1];
                            dstPixels[dIndex++] = destNoDataFinal[2];
                        }
                        continue;
                    }

                    int R = (rBuf[rIndex] & 0xFFFF) << normr;
                    int G = (gBuf[gIndex] & 0xFFFF) << normg;
                    int B = (bBuf[bIndex] & 0xFFFF) << normb;

                    if (isByte) {
                        float intensity = (R + G + B) / 3.0f;
                        dstIntPixels[dIndex++] = ((int) (intensity + 0.5f)) >> bnormi;

                        int drg = R - G;
                        int drb = R - B;

                        double tint = drg * (double) drg + drb * (double) (drb - drg);

                        double sum = drg + drb;
                        double temp;
                        if (tint != 0)
                            temp = sqrtTable[(int) (250.0 * sum * sum / tint + 0.5)];
                        else
                            temp = -1.0;

                        int hue;
                        if (sum > 0)
                            hue = acosTable[(int) (500 * temp + 0.5) + 500];
                        else
                            hue = acosTable[(int) (-500 * temp - 0.5) + 500];

                        if (B >= G)
                            dstIntPixels[dIndex++] = (255 - hue) >> bnormh;
                        else
                            dstIntPixels[dIndex++] = hue >> bnormh;

                        int min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstIntPixels[dIndex++] = (255 - (int) (255 * min / intensity + 0.5f)) >> bnorms;

                    } else {
                        float intensity = (R + G + B) / 3.0f;
                        dstPixels[dIndex++] = normi * intensity;

                        double drg = R - G;
                        double drb = R - B;
                        double temp = Math.sqrt(drg * drg + drb * (drb - drg));

                        if (temp != 0) {
                            temp = Math.acos((drg + drb) / temp / 2);
                            if (B >= G)
                                temp = PI2 - temp;
                        } else
                            temp = PI2;

                        dstPixels[dIndex++] = normh * temp;

                        double min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstPixels[dIndex++] = norms * (1 - min / intensity);
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
                        if (isByte) {
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[0];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[1];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[2];
                        } else {
                            dstPixels[dIndex++] = destNoDataFinal[0];
                            dstPixels[dIndex++] = destNoDataFinal[1];
                            dstPixels[dIndex++] = destNoDataFinal[2];
                        }
                        continue;
                    }

                    int R = (r & 0xFFFF) << normr;
                    int G = (g & 0xFFFF) << normg;
                    int B = (b & 0xFFFF) << normb;

                    if (isByte) {
                        float intensity = (R + G + B) / 3.0f;
                        dstIntPixels[dIndex++] = ((int) (intensity + 0.5f)) >> bnormi;

                        int drg = R - G;
                        int drb = R - B;

                        double tint = drg * (double) drg + drb * (double) (drb - drg);

                        double sum = drg + drb;
                        double temp;
                        if (tint != 0)
                            temp = sqrtTable[(int) (250.0 * sum * sum / tint + 0.5)];
                        else
                            temp = -1.0;

                        int hue;
                        if (sum > 0)
                            hue = acosTable[(int) (500 * temp + 0.5) + 500];
                        else
                            hue = acosTable[(int) (-500 * temp - 0.5) + 500];

                        if (B >= G)
                            dstIntPixels[dIndex++] = (255 - hue) >> bnormh;
                        else
                            dstIntPixels[dIndex++] = hue >> bnormh;

                        int min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstIntPixels[dIndex++] = (255 - (int) (255 * min / intensity + 0.5f)) >> bnorms;

                    } else {
                        float intensity = (R + G + B) / 3.0f;
                        dstPixels[dIndex++] = normi * intensity;

                        double drg = R - G;
                        double drb = R - B;
                        double temp = Math.sqrt(drg * drg + drb * (drb - drg));

                        if (temp != 0) {
                            temp = Math.acos((drg + drb) / temp / 2);
                            if (B >= G)
                                temp = PI2 - temp;
                        } else
                            temp = PI2;

                        dstPixels[dIndex++] = normh * temp;

                        double min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstPixels[dIndex++] = norms * (1 - min / intensity);
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
                        if (isByte) {
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[0];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[1];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[2];
                        } else {
                            dstPixels[dIndex++] = destNoDataFinal[0];
                            dstPixels[dIndex++] = destNoDataFinal[1];
                            dstPixels[dIndex++] = destNoDataFinal[2];
                        }
                        continue;
                    }

                    int R = (r & 0xFFFF) << normr;
                    int G = (g & 0xFFFF) << normg;
                    int B = (b & 0xFFFF) << normb;

                    if (isByte) {
                        float intensity = (R + G + B) / 3.0f;
                        dstIntPixels[dIndex++] = ((int) (intensity + 0.5f)) >> bnormi;

                        int drg = R - G;
                        int drb = R - B;

                        double tint = drg * (double) drg + drb * (double) (drb - drg);

                        double sum = drg + drb;
                        double temp;
                        if (tint != 0)
                            temp = sqrtTable[(int) (250.0 * sum * sum / tint + 0.5)];
                        else
                            temp = -1.0;

                        int hue;
                        if (sum > 0)
                            hue = acosTable[(int) (500 * temp + 0.5) + 500];
                        else
                            hue = acosTable[(int) (-500 * temp - 0.5) + 500];

                        if (B >= G)
                            dstIntPixels[dIndex++] = (255 - hue) >> bnormh;
                        else
                            dstIntPixels[dIndex++] = hue >> bnormh;

                        int min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstIntPixels[dIndex++] = (255 - (int) (255 * min / intensity + 0.5f)) >> bnorms;

                    } else {
                        float intensity = (R + G + B) / 3.0f;
                        dstPixels[dIndex++] = normi * intensity;

                        double drg = R - G;
                        double drb = R - B;
                        double temp = Math.sqrt(drg * drg + drb * (drb - drg));

                        if (temp != 0) {
                            temp = Math.acos((drg + drb) / temp / 2);
                            if (B >= G)
                                temp = PI2 - temp;
                        } else
                            temp = PI2;

                        dstPixels[dIndex++] = normh * temp;

                        double min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstPixels[dIndex++] = norms * (1 - min / intensity);
                    }
                }
            }
        }

        if (isByte)
            dest.setPixels(dest.getMinX(), dest.getMinY(), width, height, dstIntPixels);
        else {
            convertToSigned(dstPixels, dstType);
            dest.setPixels(dest.getMinX(), dest.getMinY(), width, height, dstPixels);
        }
    }

    // convert an int type raster from RGB to IHS
    private void fromRGBInt(UnpackedImageData src, int[] srcComponentSize, WritableRaster dest,
            int[] destComponentSize, boolean roiContainsTile, boolean roiDisjointTile,
            RandomIter roiIter, Rectangle roiBounds, Range nodata, float[] destNodata) {
        int[] rBuf = src.getIntData(0);
        int[] gBuf = src.getIntData(1);
        int[] bBuf = src.getIntData(2);

        int normr = 32 - srcComponentSize[0];
        int normg = 32 - srcComponentSize[1];
        int normb = 32 - srcComponentSize[2];

        double range = Integer.MAX_VALUE - (double) Integer.MIN_VALUE;
        double normi = 1.0 / range, normh = 1.0, norms = 1.0;

        int bnormi = 0, bnormh = 0, bnorms = 0;

        int dstType = dest.getSampleModel().getDataType();
        boolean isByte = (dstType == DataBuffer.TYPE_BYTE);

        if (isByte) {
            bnormi = 32 - destComponentSize[0];
            bnormh = 8 - destComponentSize[1];
            bnorms = 8 - destComponentSize[2];
            generateACosTable();
            generateSqrtTable();
        } else if (dstType < DataBuffer.TYPE_FLOAT) {
            normi = ((1l << destComponentSize[0]) - 1) / range;
            normh = ((1l << destComponentSize[1]) - 1) / PI2;
            norms = ((1l << destComponentSize[2]) - 1);
        }

        int height = dest.getHeight();
        int width = dest.getWidth();

        double[] dstPixels = null;
        int[] dstIntPixels = null;

        if (isByte)
            dstIntPixels = new int[3 * height * width];
        else
            dstPixels = new double[3 * height * width];

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

        double[] destNoDataFinal = getConvertedNodataIfromRGB(isByte, destNodata, normr, normg,
                normb, bnormi, bnormh, bnorms, normi, normh, norms, true);

        if (roiDisjointTile) {
            ImageUtil.fillBackground(dest, dest.getBounds(), destNoDataFinal);
            return;
        }

        if (!hasNodata && (!hasROI || (hasROI && roiContainsTile))) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {
                    long R = (rBuf[rIndex] & 0xFFFFFFFFl) << normr;
                    long G = (gBuf[gIndex] & 0xFFFFFFFFl) << normg;
                    long B = (bBuf[bIndex] & 0xFFFFFFFFl) << normb;

                    if (isByte) {
                        float intensity = (R + G + B) / 3.0f;
                        dstIntPixels[dIndex++] = (int) (((long) (intensity + 0.5f)) >> bnormi);

                        long drg = R - G;
                        long drb = R - B;

                        double tint = drg * (double) drg + drb * (double) (drb - drg);

                        double sum = drg + drb;
                        double temp;
                        if (tint != 0)
                            temp = sqrtTable[(int) (250.0 * sum * sum / tint + 0.5)];
                        else
                            temp = -1.0;

                        int hue;
                        if (sum > 0)
                            hue = acosTable[(int) (500 * temp + 0.5) + 500];
                        else
                            hue = acosTable[(int) (-500 * temp - 0.5) + 500];

                        if (B >= G)
                            dstIntPixels[dIndex++] = (255 - hue) >> bnormh;
                        else
                            dstIntPixels[dIndex++] = hue >> bnormh;

                        long min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstIntPixels[dIndex++] = (255 - (int) (255 * min / intensity + 0.5f)) >> bnorms;
                    } else {
                        float intensity = (R + G + B) / 3.0f;
                        dstPixels[dIndex++] = normi * intensity;

                        double drg = R - G;
                        double drb = R - B;
                        double temp = Math.sqrt(drg * drg + drb * (drb - drg));

                        if (temp != 0) {
                            temp = Math.acos((drg + drb) / temp / 2);
                            if (B >= G)
                                temp = PI2 - temp;
                        } else
                            temp = PI2;

                        dstPixels[dIndex++] = normh * temp;

                        double min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstPixels[dIndex++] = norms * (1 - min / intensity);
                    }
                }
            }
        } else if (hasROI && !hasNodata) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                int y0 = j + destY;
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {
                    int x0 = i + destX;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        if (isByte) {
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[0];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[1];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[2];
                        } else {
                            dstPixels[dIndex++] = destNoDataFinal[0];
                            dstPixels[dIndex++] = destNoDataFinal[1];
                            dstPixels[dIndex++] = destNoDataFinal[2];
                        }
                        continue;
                    }

                    long R = (rBuf[rIndex] & 0xFFFFFFFFl) << normr;
                    long G = (gBuf[gIndex] & 0xFFFFFFFFl) << normg;
                    long B = (bBuf[bIndex] & 0xFFFFFFFFl) << normb;

                    if (isByte) {
                        float intensity = (R + G + B) / 3.0f;
                        dstIntPixels[dIndex++] = (int) (((long) (intensity + 0.5f)) >> bnormi);

                        long drg = R - G;
                        long drb = R - B;

                        double tint = drg * (double) drg + drb * (double) (drb - drg);

                        double sum = drg + drb;
                        double temp;
                        if (tint != 0)
                            temp = sqrtTable[(int) (250.0 * sum * sum / tint + 0.5)];
                        else
                            temp = -1.0;

                        int hue;
                        if (sum > 0)
                            hue = acosTable[(int) (500 * temp + 0.5) + 500];
                        else
                            hue = acosTable[(int) (-500 * temp - 0.5) + 500];

                        if (B >= G)
                            dstIntPixels[dIndex++] = (255 - hue) >> bnormh;
                        else
                            dstIntPixels[dIndex++] = hue >> bnormh;

                        long min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstIntPixels[dIndex++] = (255 - (int) (255 * min / intensity + 0.5f)) >> bnorms;
                    } else {
                        float intensity = (R + G + B) / 3.0f;
                        dstPixels[dIndex++] = normi * intensity;

                        double drg = R - G;
                        double drb = R - B;
                        double temp = Math.sqrt(drg * drg + drb * (drb - drg));

                        if (temp != 0) {
                            temp = Math.acos((drg + drb) / temp / 2);
                            if (B >= G)
                                temp = PI2 - temp;
                        } else
                            temp = PI2;

                        dstPixels[dIndex++] = normh * temp;

                        double min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstPixels[dIndex++] = norms * (1 - min / intensity);
                    }
                }
            }
        } else if (hasNodata && (!hasROI || (hasROI && roiContainsTile))) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {

                    int r = rBuf[rIndex];
                    int g = gBuf[gIndex];
                    int b = bBuf[bIndex];

                    boolean notValid = nodata.contains(r) || nodata.contains(g)
                            || nodata.contains(b);

                    if (notValid) {
                        if (isByte) {
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[0];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[1];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[2];
                        } else {
                            dstPixels[dIndex++] = destNoDataFinal[0];
                            dstPixels[dIndex++] = destNoDataFinal[1];
                            dstPixels[dIndex++] = destNoDataFinal[2];
                        }
                        continue;
                    }

                    long R = (r & 0xFFFFFFFFl) << normr;
                    long G = (g & 0xFFFFFFFFl) << normg;
                    long B = (b & 0xFFFFFFFFl) << normb;

                    if (isByte) {
                        float intensity = (R + G + B) / 3.0f;
                        dstIntPixels[dIndex++] = (int) (((long) (intensity + 0.5f)) >> bnormi);

                        long drg = R - G;
                        long drb = R - B;

                        double tint = drg * (double) drg + drb * (double) (drb - drg);

                        double sum = drg + drb;
                        double temp;
                        if (tint != 0)
                            temp = sqrtTable[(int) (250.0 * sum * sum / tint + 0.5)];
                        else
                            temp = -1.0;

                        int hue;
                        if (sum > 0)
                            hue = acosTable[(int) (500 * temp + 0.5) + 500];
                        else
                            hue = acosTable[(int) (-500 * temp - 0.5) + 500];

                        if (B >= G)
                            dstIntPixels[dIndex++] = (255 - hue) >> bnormh;
                        else
                            dstIntPixels[dIndex++] = hue >> bnormh;

                        long min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstIntPixels[dIndex++] = (255 - (int) (255 * min / intensity + 0.5f)) >> bnorms;
                    } else {
                        float intensity = (R + G + B) / 3.0f;
                        dstPixels[dIndex++] = normi * intensity;

                        double drg = R - G;
                        double drb = R - B;
                        double temp = Math.sqrt(drg * drg + drb * (drb - drg));

                        if (temp != 0) {
                            temp = Math.acos((drg + drb) / temp / 2);
                            if (B >= G)
                                temp = PI2 - temp;
                        } else
                            temp = PI2;

                        dstPixels[dIndex++] = normh * temp;

                        double min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstPixels[dIndex++] = norms * (1 - min / intensity);
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
                        if (isByte) {
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[0];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[1];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[2];
                        } else {
                            dstPixels[dIndex++] = destNoDataFinal[0];
                            dstPixels[dIndex++] = destNoDataFinal[1];
                            dstPixels[dIndex++] = destNoDataFinal[2];
                        }
                        continue;
                    }

                    long R = (r & 0xFFFFFFFFl) << normr;
                    long G = (g & 0xFFFFFFFFl) << normg;
                    long B = (b & 0xFFFFFFFFl) << normb;

                    if (isByte) {
                        float intensity = (R + G + B) / 3.0f;
                        dstIntPixels[dIndex++] = (int) (((long) (intensity + 0.5f)) >> bnormi);

                        long drg = R - G;
                        long drb = R - B;

                        double tint = drg * (double) drg + drb * (double) (drb - drg);

                        double sum = drg + drb;
                        double temp;
                        if (tint != 0)
                            temp = sqrtTable[(int) (250.0 * sum * sum / tint + 0.5)];
                        else
                            temp = -1.0;

                        int hue;
                        if (sum > 0)
                            hue = acosTable[(int) (500 * temp + 0.5) + 500];
                        else
                            hue = acosTable[(int) (-500 * temp - 0.5) + 500];

                        if (B >= G)
                            dstIntPixels[dIndex++] = (255 - hue) >> bnormh;
                        else
                            dstIntPixels[dIndex++] = hue >> bnormh;

                        long min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstIntPixels[dIndex++] = (255 - (int) (255 * min / intensity + 0.5f)) >> bnorms;
                    } else {
                        float intensity = (R + G + B) / 3.0f;
                        dstPixels[dIndex++] = normi * intensity;

                        double drg = R - G;
                        double drb = R - B;
                        double temp = Math.sqrt(drg * drg + drb * (drb - drg));

                        if (temp != 0) {
                            temp = Math.acos((drg + drb) / temp / 2);
                            if (B >= G)
                                temp = PI2 - temp;
                        } else
                            temp = PI2;

                        dstPixels[dIndex++] = normh * temp;

                        double min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstPixels[dIndex++] = norms * (1 - min / intensity);
                    }
                }
            }
        }

        if (isByte)
            dest.setPixels(dest.getMinX(), dest.getMinY(), width, height, dstIntPixels);
        else {
            convertToSigned(dstPixels, dstType);
            dest.setPixels(dest.getMinX(), dest.getMinY(), width, height, dstPixels);
        }
    }

    // convert a float type raster from RGB to IHS
    private void fromRGBFloat(UnpackedImageData src, int[] srcComponentSize, WritableRaster dest,
            int[] destComponentSize, boolean roiContainsTile, boolean roiDisjointTile,
            RandomIter roiIter, Rectangle roiBounds, Range nodata, float[] destNodata) {
        float[] rBuf = src.getFloatData(0);
        float[] gBuf = src.getFloatData(1);
        float[] bBuf = src.getFloatData(2);

        double normi = 1.0, normh = 1.0, norms = 1.0;

        int bnormi = 0, bnormh = 0, bnorms = 0;

        int dstType = dest.getSampleModel().getDataType();
        boolean isByte = (dstType == DataBuffer.TYPE_BYTE);

        if (isByte) {
            bnormi = (1 << destComponentSize[0]) - 1;
            bnormh = 8 - destComponentSize[1];
            bnorms = 8 - destComponentSize[2];
            generateACosTable();
            generateSqrtTable();
        } else if (dstType < DataBuffer.TYPE_FLOAT) {
            normi = (1l << destComponentSize[0]) - 1;
            normh = ((1l << destComponentSize[1]) - 1) / PI2;
            norms = (1l << destComponentSize[2]) - 1;
        }

        int height = dest.getHeight();
        int width = dest.getWidth();

        double[] dstPixels = null;
        int[] dstIntPixels = null;

        if (isByte)
            dstIntPixels = new int[3 * height * width];
        else
            dstPixels = new double[3 * height * width];

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

        double[] destNoDataFinal = getConvertedNodataDfromRGB(isByte, destNodata, bnormi, bnormh,
                bnorms, normi, normh, norms);

        if (roiDisjointTile) {
            ImageUtil.fillBackground(dest, dest.getBounds(), destNoDataFinal);
            return;
        }

        if (!hasNodata && (!hasROI || (hasROI && roiContainsTile))) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {
                    float R = rBuf[rIndex];
                    float G = gBuf[gIndex];
                    float B = bBuf[bIndex];

                    if (isByte) {
                        float intensity = (R + G + B) / 3.0f;
                        dstIntPixels[dIndex++] = (int) (intensity * bnormi + 0.5f);

                        float drg = R - G;
                        float drb = R - B;

                        double tint = drg * (double) drg + drb * (double) (drb - drg);

                        double sum = drg + drb;
                        double temp;
                        if (tint != 0)
                            temp = sqrtTable[(int) (250.0 * sum * sum / tint + 0.5)];
                        else
                            temp = -1.0;

                        int hue;
                        if (sum > 0)
                            hue = acosTable[(int) (500 * temp + 0.5) + 500];
                        else
                            hue = acosTable[(int) (-500 * temp - 0.5) + 500];

                        if (B >= G)
                            dstIntPixels[dIndex++] = (255 - hue) >> bnormh;
                        else
                            dstIntPixels[dIndex++] = hue >> bnormh;

                        float min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstIntPixels[dIndex++] = (255 - (int) (255 * min / intensity + 0.5f)) >> bnorms;
                    } else {
                        float intensity = (R + G + B) / 3.0f;
                        dstPixels[dIndex++] = normi * intensity;

                        double drg = R - G;
                        double drb = R - B;
                        double temp = Math.sqrt(drg * drg + drb * (drb - drg));

                        if (temp != 0) {
                            temp = Math.acos((drg + drb) / temp / 2);
                            if (B >= G)
                                temp = PI2 - temp;
                        } else
                            temp = PI2;

                        dstPixels[dIndex++] = normh * temp;

                        double min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstPixels[dIndex++] = norms * (1 - min / intensity);
                    }
                }
            }
        } else if (hasROI && !hasNodata) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                int y0 = j + destY;
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {
                    int x0 = i + destX;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        if (isByte) {
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[0];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[1];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[2];
                        } else {
                            dstPixels[dIndex++] = destNoDataFinal[0];
                            dstPixels[dIndex++] = destNoDataFinal[1];
                            dstPixels[dIndex++] = destNoDataFinal[2];
                        }
                        continue;
                    }

                    float R = rBuf[rIndex];
                    float G = gBuf[gIndex];
                    float B = bBuf[bIndex];

                    if (isByte) {
                        float intensity = (R + G + B) / 3.0f;
                        dstIntPixels[dIndex++] = (int) (intensity * bnormi + 0.5f);

                        float drg = R - G;
                        float drb = R - B;

                        double tint = drg * (double) drg + drb * (double) (drb - drg);

                        double sum = drg + drb;
                        double temp;
                        if (tint != 0)
                            temp = sqrtTable[(int) (250.0 * sum * sum / tint + 0.5)];
                        else
                            temp = -1.0;

                        int hue;
                        if (sum > 0)
                            hue = acosTable[(int) (500 * temp + 0.5) + 500];
                        else
                            hue = acosTable[(int) (-500 * temp - 0.5) + 500];

                        if (B >= G)
                            dstIntPixels[dIndex++] = (255 - hue) >> bnormh;
                        else
                            dstIntPixels[dIndex++] = hue >> bnormh;

                        float min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstIntPixels[dIndex++] = (255 - (int) (255 * min / intensity + 0.5f)) >> bnorms;
                    } else {
                        float intensity = (R + G + B) / 3.0f;
                        dstPixels[dIndex++] = normi * intensity;

                        double drg = R - G;
                        double drb = R - B;
                        double temp = Math.sqrt(drg * drg + drb * (drb - drg));

                        if (temp != 0) {
                            temp = Math.acos((drg + drb) / temp / 2);
                            if (B >= G)
                                temp = PI2 - temp;
                        } else
                            temp = PI2;

                        dstPixels[dIndex++] = normh * temp;

                        double min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstPixels[dIndex++] = norms * (1 - min / intensity);
                    }
                }
            }
        } else if (hasNodata && (!hasROI || (hasROI && roiContainsTile))) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {

                    float R = rBuf[rIndex];
                    float G = gBuf[gIndex];
                    float B = bBuf[bIndex];

                    boolean notValid = nodata.contains(R) || nodata.contains(G)
                            || nodata.contains(B);

                    if (notValid) {
                        if (isByte) {
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[0];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[1];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[2];
                        } else {
                            dstPixels[dIndex++] = destNoDataFinal[0];
                            dstPixels[dIndex++] = destNoDataFinal[1];
                            dstPixels[dIndex++] = destNoDataFinal[2];
                        }
                        continue;
                    }

                    if (isByte) {
                        float intensity = (R + G + B) / 3.0f;
                        dstIntPixels[dIndex++] = (int) (intensity * bnormi + 0.5f);

                        float drg = R - G;
                        float drb = R - B;

                        double tint = drg * (double) drg + drb * (double) (drb - drg);

                        double sum = drg + drb;
                        double temp;
                        if (tint != 0)
                            temp = sqrtTable[(int) (250.0 * sum * sum / tint + 0.5)];
                        else
                            temp = -1.0;

                        int hue;
                        if (sum > 0)
                            hue = acosTable[(int) (500 * temp + 0.5) + 500];
                        else
                            hue = acosTable[(int) (-500 * temp - 0.5) + 500];

                        if (B >= G)
                            dstIntPixels[dIndex++] = (255 - hue) >> bnormh;
                        else
                            dstIntPixels[dIndex++] = hue >> bnormh;

                        float min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstIntPixels[dIndex++] = (255 - (int) (255 * min / intensity + 0.5f)) >> bnorms;
                    } else {
                        float intensity = (R + G + B) / 3.0f;
                        dstPixels[dIndex++] = normi * intensity;

                        double drg = R - G;
                        double drb = R - B;
                        double temp = Math.sqrt(drg * drg + drb * (drb - drg));

                        if (temp != 0) {
                            temp = Math.acos((drg + drb) / temp / 2);
                            if (B >= G)
                                temp = PI2 - temp;
                        } else
                            temp = PI2;

                        dstPixels[dIndex++] = normh * temp;

                        double min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstPixels[dIndex++] = norms * (1 - min / intensity);
                    }
                }
            }
        } else {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                int y0 = j + destY;
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {
                    int x0 = i + destX;

                    float R = rBuf[rIndex];
                    float G = gBuf[gIndex];
                    float B = bBuf[bIndex];

                    boolean notValid = nodata.contains(R) || nodata.contains(G)
                            || nodata.contains(B);

                    if (notValid
                            || !(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        if (isByte) {
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[0];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[1];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[2];
                        } else {
                            dstPixels[dIndex++] = destNoDataFinal[0];
                            dstPixels[dIndex++] = destNoDataFinal[1];
                            dstPixels[dIndex++] = destNoDataFinal[2];
                        }
                        continue;
                    }

                    if (isByte) {
                        float intensity = (R + G + B) / 3.0f;
                        dstIntPixels[dIndex++] = (int) (intensity * bnormi + 0.5f);

                        float drg = R - G;
                        float drb = R - B;

                        double tint = drg * (double) drg + drb * (double) (drb - drg);

                        double sum = drg + drb;
                        double temp;
                        if (tint != 0)
                            temp = sqrtTable[(int) (250.0 * sum * sum / tint + 0.5)];
                        else
                            temp = -1.0;

                        int hue;
                        if (sum > 0)
                            hue = acosTable[(int) (500 * temp + 0.5) + 500];
                        else
                            hue = acosTable[(int) (-500 * temp - 0.5) + 500];

                        if (B >= G)
                            dstIntPixels[dIndex++] = (255 - hue) >> bnormh;
                        else
                            dstIntPixels[dIndex++] = hue >> bnormh;

                        float min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstIntPixels[dIndex++] = (255 - (int) (255 * min / intensity + 0.5f)) >> bnorms;
                    } else {
                        float intensity = (R + G + B) / 3.0f;
                        dstPixels[dIndex++] = normi * intensity;

                        double drg = R - G;
                        double drb = R - B;
                        double temp = Math.sqrt(drg * drg + drb * (drb - drg));

                        if (temp != 0) {
                            temp = Math.acos((drg + drb) / temp / 2);
                            if (B >= G)
                                temp = PI2 - temp;
                        } else
                            temp = PI2;

                        dstPixels[dIndex++] = normh * temp;

                        double min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstPixels[dIndex++] = norms * (1 - min / intensity);
                    }
                }
            }
        }

        if (isByte)
            dest.setPixels(dest.getMinX(), dest.getMinY(), width, height, dstIntPixels);
        else {
            convertToSigned(dstPixels, dstType);
            dest.setPixels(dest.getMinX(), dest.getMinY(), width, height, dstPixels);
        }
    }

    // convert a double type raster from RGB to IHS
    private void fromRGBDouble(UnpackedImageData src, int[] srcComponentSize, WritableRaster dest,
            int[] destComponentSize, boolean roiContainsTile, boolean roiDisjointTile,
            RandomIter roiIter, Rectangle roiBounds, Range nodata, float[] destNodata) {
        double[] rBuf = src.getDoubleData(0);
        double[] gBuf = src.getDoubleData(1);
        double[] bBuf = src.getDoubleData(2);

        double normi = 1.0, normh = 1.0, norms = 1.0;

        int bnormi = 0, bnormh = 0, bnorms = 0;

        int dstType = dest.getSampleModel().getDataType();
        boolean isByte = (dstType == DataBuffer.TYPE_BYTE);

        if (isByte) {
            bnormi = (1 << destComponentSize[0]) - 1;
            bnormh = 8 - destComponentSize[1];
            bnorms = 8 - destComponentSize[2];
            generateACosTable();
            generateSqrtTable();
        } else if (dstType < DataBuffer.TYPE_FLOAT) {
            normi = (1l << destComponentSize[0]) - 1;
            normh = ((1l << destComponentSize[1]) - 1) / PI2;
            norms = (1l << destComponentSize[2]) - 1;
        }

        int height = dest.getHeight();
        int width = dest.getWidth();

        double[] dstPixels = null;
        int[] dstIntPixels = null;

        if (isByte)
            dstIntPixels = new int[3 * height * width];
        else
            dstPixels = new double[3 * height * width];

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

        double[] destNoDataFinal = getConvertedNodataDfromRGB(isByte, destNodata, bnormi, bnormh,
                bnorms, normi, normh, norms);

        if (roiDisjointTile) {
            ImageUtil.fillBackground(dest, dest.getBounds(), destNoDataFinal);
            return;
        }

        if (!hasNodata && (!hasROI || (hasROI && roiContainsTile))) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {
                    double R = rBuf[rIndex];
                    double G = gBuf[gIndex];
                    double B = bBuf[bIndex];

                    if (isByte) {
                        double intensity = (R + G + B) / 3.0f;
                        dstIntPixels[dIndex++] = (int) (intensity * bnormi + 0.5);

                        double drg = R - G;
                        double drb = R - B;

                        double tint = drg * drg + drb * (drb - drg);

                        double sum = drg + drb;
                        double temp;
                        if (tint != 0.0)
                            temp = sqrtTable[(int) (250.0 * sum * sum / tint + 0.5)];
                        else
                            temp = -1.0;

                        int hue;
                        if (sum > 0)
                            hue = acosTable[(int) (500 * temp + 0.5) + 500];
                        else
                            hue = acosTable[(int) (-500 * temp - 0.5) + 500];

                        if (B >= G)
                            dstIntPixels[dIndex++] = (255 - hue) >> bnormh;
                        else
                            dstIntPixels[dIndex++] = hue >> bnormh;

                        double min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstIntPixels[dIndex++] = (255 - (int) (255 * min / intensity + 0.5f)) >> bnorms;
                    } else {
                        double intensity = (R + G + B) / 3.0f;
                        dstPixels[dIndex++] = normi * intensity;

                        double drg = R - G;
                        double drb = R - B;
                        double temp = Math.sqrt(drg * drg + drb * (drb - drg));

                        if (temp != 0) {
                            temp = Math.acos((drg + drb) / temp / 2);
                            if (B >= G)
                                temp = PI2 - temp;
                        } else
                            temp = PI2;

                        dstPixels[dIndex++] = normh * temp;

                        double min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstPixels[dIndex++] = norms * (1 - min / intensity);
                    }
                }
            }
        } else if (!hasNodata && hasROI) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                int y0 = j + destY;
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {

                    int x0 = i + destX;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        if (isByte) {
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[0];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[1];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[2];
                        } else {
                            dstPixels[dIndex++] = destNoDataFinal[0];
                            dstPixels[dIndex++] = destNoDataFinal[1];
                            dstPixels[dIndex++] = destNoDataFinal[2];
                        }
                        continue;
                    }

                    double R = rBuf[rIndex];
                    double G = gBuf[gIndex];
                    double B = bBuf[bIndex];

                    if (isByte) {
                        double intensity = (R + G + B) / 3.0f;
                        dstIntPixels[dIndex++] = (int) (intensity * bnormi + 0.5);

                        double drg = R - G;
                        double drb = R - B;

                        double tint = drg * drg + drb * (drb - drg);

                        double sum = drg + drb;
                        double temp;
                        if (tint != 0.0)
                            temp = sqrtTable[(int) (250.0 * sum * sum / tint + 0.5)];
                        else
                            temp = -1.0;

                        int hue;
                        if (sum > 0)
                            hue = acosTable[(int) (500 * temp + 0.5) + 500];
                        else
                            hue = acosTable[(int) (-500 * temp - 0.5) + 500];

                        if (B >= G)
                            dstIntPixels[dIndex++] = (255 - hue) >> bnormh;
                        else
                            dstIntPixels[dIndex++] = hue >> bnormh;

                        double min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstIntPixels[dIndex++] = (255 - (int) (255 * min / intensity + 0.5f)) >> bnorms;
                    } else {
                        double intensity = (R + G + B) / 3.0f;
                        dstPixels[dIndex++] = normi * intensity;

                        double drg = R - G;
                        double drb = R - B;
                        double temp = Math.sqrt(drg * drg + drb * (drb - drg));

                        if (temp != 0) {
                            temp = Math.acos((drg + drb) / temp / 2);
                            if (B >= G)
                                temp = PI2 - temp;
                        } else
                            temp = PI2;

                        dstPixels[dIndex++] = normh * temp;

                        double min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstPixels[dIndex++] = norms * (1 - min / intensity);
                    }
                }
            }
        } else if (hasNodata && (!hasROI || (hasROI && roiContainsTile))) {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {

                    double R = rBuf[rIndex];
                    double G = gBuf[gIndex];
                    double B = bBuf[bIndex];

                    boolean notValid = nodata.contains(R) || nodata.contains(G)
                            || nodata.contains(B);

                    if (notValid) {
                        if (isByte) {
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[0];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[1];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[2];
                        } else {
                            dstPixels[dIndex++] = destNoDataFinal[0];
                            dstPixels[dIndex++] = destNoDataFinal[1];
                            dstPixels[dIndex++] = destNoDataFinal[2];
                        }
                        continue;
                    }

                    if (isByte) {
                        double intensity = (R + G + B) / 3.0f;
                        dstIntPixels[dIndex++] = (int) (intensity * bnormi + 0.5);

                        double drg = R - G;
                        double drb = R - B;

                        double tint = drg * drg + drb * (drb - drg);

                        double sum = drg + drb;
                        double temp;
                        if (tint != 0.0)
                            temp = sqrtTable[(int) (250.0 * sum * sum / tint + 0.5)];
                        else
                            temp = -1.0;

                        int hue;
                        if (sum > 0)
                            hue = acosTable[(int) (500 * temp + 0.5) + 500];
                        else
                            hue = acosTable[(int) (-500 * temp - 0.5) + 500];

                        if (B >= G)
                            dstIntPixels[dIndex++] = (255 - hue) >> bnormh;
                        else
                            dstIntPixels[dIndex++] = hue >> bnormh;

                        double min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstIntPixels[dIndex++] = (255 - (int) (255 * min / intensity + 0.5f)) >> bnorms;
                    } else {
                        double intensity = (R + G + B) / 3.0f;
                        dstPixels[dIndex++] = normi * intensity;

                        double drg = R - G;
                        double drb = R - B;
                        double temp = Math.sqrt(drg * drg + drb * (drb - drg));

                        if (temp != 0) {
                            temp = Math.acos((drg + drb) / temp / 2);
                            if (B >= G)
                                temp = PI2 - temp;
                        } else
                            temp = PI2;

                        dstPixels[dIndex++] = normh * temp;

                        double min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstPixels[dIndex++] = norms * (1 - min / intensity);
                    }
                }
            }
        } else {
            for (int j = 0; j < height; j++, rStart += srcLineStride, gStart += srcLineStride, bStart += srcLineStride) {
                int y0 = j + destY;
                for (int i = 0, rIndex = rStart, gIndex = gStart, bIndex = bStart; i < width; i++, rIndex += srcPixelStride, gIndex += srcPixelStride, bIndex += srcPixelStride) {

                    int x0 = i + destX;

                    double R = rBuf[rIndex];
                    double G = gBuf[gIndex];
                    double B = bBuf[bIndex];

                    boolean notValid = nodata.contains(R) || nodata.contains(G)
                            || nodata.contains(B);

                    if (notValid
                            && !(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        if (isByte) {
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[0];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[1];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[2];
                        } else {
                            dstPixels[dIndex++] = destNoDataFinal[0];
                            dstPixels[dIndex++] = destNoDataFinal[1];
                            dstPixels[dIndex++] = destNoDataFinal[2];
                        }
                        continue;
                    }

                    if (isByte) {
                        double intensity = (R + G + B) / 3.0f;
                        dstIntPixels[dIndex++] = (int) (intensity * bnormi + 0.5);

                        double drg = R - G;
                        double drb = R - B;

                        double tint = drg * drg + drb * (drb - drg);

                        double sum = drg + drb;
                        double temp;
                        if (tint != 0.0)
                            temp = sqrtTable[(int) (250.0 * sum * sum / tint + 0.5)];
                        else
                            temp = -1.0;

                        int hue;
                        if (sum > 0)
                            hue = acosTable[(int) (500 * temp + 0.5) + 500];
                        else
                            hue = acosTable[(int) (-500 * temp - 0.5) + 500];

                        if (B >= G)
                            dstIntPixels[dIndex++] = (255 - hue) >> bnormh;
                        else
                            dstIntPixels[dIndex++] = hue >> bnormh;

                        double min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstIntPixels[dIndex++] = (255 - (int) (255 * min / intensity + 0.5f)) >> bnorms;
                    } else {
                        double intensity = (R + G + B) / 3.0f;
                        dstPixels[dIndex++] = normi * intensity;

                        double drg = R - G;
                        double drb = R - B;
                        double temp = Math.sqrt(drg * drg + drb * (drb - drg));

                        if (temp != 0) {
                            temp = Math.acos((drg + drb) / temp / 2);
                            if (B >= G)
                                temp = PI2 - temp;
                        } else
                            temp = PI2;

                        dstPixels[dIndex++] = normh * temp;

                        double min = (G > B) ? B : G;
                        min = (R > min) ? min : R;
                        dstPixels[dIndex++] = norms * (1 - min / intensity);
                    }
                }
            }
        }

        if (isByte)
            dest.setPixels(dest.getMinX(), dest.getMinY(), width, height, dstIntPixels);
        else {
            convertToSigned(dstPixels, dstType);
            dest.setPixels(dest.getMinX(), dest.getMinY(), width, height, dstPixels);
        }
    }

    /**
     * Converts a <code>Raster</code> of colors represented as pixels from IHS to CIEXYZ.
     */
    public WritableRaster toCIEXYZ(Raster src, int[] srcComponentSize, WritableRaster dest,
            int[] destComponentSize, ROI roi, Range nodata, float[] destNodata) {
        WritableRaster tempRas = toRGB(src, srcComponentSize, null, null, roi, nodata, destNodata);
        return RGBToCIEXYZ(tempRas, tempRas.getSampleModel().getSampleSize(), dest,
                destComponentSize, roi, nodata, destNodata);
    }

    /**
     * Converts a <code>Raster</code> of colors represented as pixels from IHS to sRGB.
     */
    public WritableRaster toRGB(Raster src, int[] srcComponentSize, WritableRaster dest,
            int[] destComponentSize, ROI roi, Range nodata, float[] destNodata) {

        checkParameters(src, srcComponentSize, dest, destComponentSize);

        // ROI check
        ROI roiTile = null;

        RandomIter roiIter = null;

        boolean roiContainsTile = false;
        boolean roiDisjointTile = false;

        // If a ROI is present, then only the part contained inside the current tile bounds is taken.
        Rectangle bounds = null;
        if (roi != null) {
            bounds = roi.getBounds();
            Rectangle srcRectExpanded = src.getBounds();
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

        if (srcComponentSize == null)
            srcComponentSize = srcSampleModel.getSampleSize();

        if (dest == null) {
            Point origin = new Point(src.getMinX(), src.getMinY());
            dest = RasterFactory.createWritableRaster(srcSampleModel, origin);
        }

        SampleModel dstSampleModel = dest.getSampleModel();
        if (destComponentSize == null)
            destComponentSize = dstSampleModel.getSampleSize();

        PixelAccessor srcAcc = new PixelAccessor(srcSampleModel, null);
        UnpackedImageData srcUid = srcAcc.getPixels(src, src.getBounds(),
                srcSampleModel.getDataType(), false);

        switch (srcSampleModel.getDataType()) {

        case DataBuffer.TYPE_BYTE:
            toRGBByte(srcUid, srcComponentSize, dest, destComponentSize, roiContainsTile,
                    roiDisjointTile, roiIter, bounds, nodata, destNodata);
            break;
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            toRGBShort(srcUid, srcComponentSize, dest, destComponentSize, roiContainsTile,
                    roiDisjointTile, roiIter, bounds, nodata, destNodata);
            break;
        case DataBuffer.TYPE_INT:
            toRGBInt(srcUid, srcComponentSize, dest, destComponentSize, roiContainsTile,
                    roiDisjointTile, roiIter, bounds, nodata, destNodata);
            break;
        case DataBuffer.TYPE_FLOAT:
            toRGBFloat(srcUid, srcComponentSize, dest, destComponentSize, roiContainsTile,
                    roiDisjointTile, roiIter, bounds, nodata, destNodata);
            break;
        case DataBuffer.TYPE_DOUBLE:
            toRGBDouble(srcUid, srcComponentSize, dest, destComponentSize, roiContainsTile,
                    roiDisjointTile, roiIter, bounds, nodata, destNodata);
            break;
        }
        return dest;
    }

    // convert a byte type raster from IHS to RGB
    private void toRGBByte(UnpackedImageData src, int[] srcComponentSize, WritableRaster dest,
            int[] destComponentSize, boolean roiContainsTile, boolean roiDisjointTile,
            RandomIter roiIter, Rectangle roiBounds, Range nodata, float[] destNodata) {
        byte[] iBuf = src.getByteData(0);
        byte[] hBuf = src.getByteData(1);
        byte[] sBuf = src.getByteData(2);

        double normi = 1.0 / ((1 << srcComponentSize[0]) - 1);
        double normh = 1.0 / ((1 << srcComponentSize[1]) - 1) * PI2;
        double norms = 1.0 / ((1 << srcComponentSize[2]) - 1);

        double normr = 1.0, normg = 1.0, normb = 1.0;

        int dstType = dest.getSampleModel().getDataType();
        boolean isByte = (dstType == DataBuffer.TYPE_BYTE);

        if (isByte) {
            generateTanTable();
        }
        if (dstType < DataBuffer.TYPE_FLOAT) {
            normr = (1l << destComponentSize[0]) - 1;
            normg = (1l << destComponentSize[1]) - 1;
            normb = (1l << destComponentSize[2]) - 1;
        }

        int height = dest.getHeight();
        int width = dest.getWidth();

        double[] dstPixels = null;
        int[] dstIntPixels = null;

        if (isByte)
            dstIntPixels = new int[3 * height * width];
        else
            dstPixels = new double[3 * height * width];

        int iStart = src.bandOffsets[0];
        int hStart = src.bandOffsets[1];
        int sStart = src.bandOffsets[2];
        int srcPixelStride = src.pixelStride;
        int srcLineStride = src.lineStride;

        int dIndex = 0;

        boolean hasROI = roiIter != null && roiBounds != null;
        boolean hasNodata = nodata != null;

        int destX = dest.getMinX();
        int destY = dest.getMinY();

        double[] destNoDataFinal = getConvertedNodataBToRGB(isByte, destNodata, normi, norms,
                normr, normg, normb, normh);

        if (roiDisjointTile) {
            ImageUtil.fillBackground(dest, dest.getBounds(), destNoDataFinal);
            return;
        }

        if (!hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, iStart += srcLineStride, hStart += srcLineStride, sStart += srcLineStride) {
                for (int i = 0, iIndex = iStart, hIndex = hStart, sIndex = sStart; i < width; i++, iIndex += srcPixelStride, hIndex += srcPixelStride, sIndex += srcPixelStride) {

                    double I = (iBuf[iIndex] & 0xFF) * normi;
                    int h = hBuf[hIndex] & 0xFF;
                    double S = (sBuf[sIndex] & 0xFF) * norms;

                    if (isByte) {
                        float r, g, b;

                        r = g = b = (float) I;

                        if (S != 0.0) {
                            if (h >= 85 && h <= 170) {
                                r = (float) ((1 - S) * I);
                                float c1 = (float) (3 * I - r);
                                float c2 = (float) (SQRT3 * (r - I) * tanTable[h]);
                                g = (float) ((c1 + c2) / 2);
                                b = (float) ((c1 - c2) / 2);
                            } else if (h > 170) {
                                g = (float) ((1 - S) * I);
                                float c1 = (float) (3 * I - g);
                                float c2 = (float) (SQRT3 * (g - I) * tanTable[h - 85]);
                                b = (c1 + c2) / 2;
                                r = (c1 - c2) / 2;
                            } else if (h < 85) {
                                b = (float) ((1 - S) * I);
                                float c1 = (float) (3 * I - b);
                                float c2 = (float) (SQRT3 * (b - I) * tanTable[h + 85]);
                                r = (c1 + c2) / 2;
                                g = (c1 - c2) / 2;
                            }
                        }
                        dstIntPixels[dIndex++] = (int) (((r < 0.0f) ? 0.0f
                                : ((r > 1.0f) ? 1.0f : r)) * normr + 0.5);
                        dstIntPixels[dIndex++] = (int) (((g < 0.0f) ? 0.0f
                                : ((g > 1.0f) ? 1.0f : g)) * normg + 0.5);
                        dstIntPixels[dIndex++] = (int) (((b < 0.0f) ? 0.0f
                                : ((b > 1.0f) ? 1.0f : b)) * normb + 0.5);
                    } else {
                        double R, G, B;

                        R = G = B = I;
                        if (S != 0) {
                            double H = h * normh;
                            if (H >= PI23 && H <= PI43) {
                                R = (1 - S) * I;
                                double c1 = 3 * I - R;
                                double c2 = SQRT3 * (R - I) * Math.tan(H);
                                G = (c1 + c2) / 2;
                                B = (c1 - c2) / 2;
                            } else if (H > PI43) {
                                G = (1 - S) * I;
                                double c1 = 3 * I - G;
                                double c2 = SQRT3 * (G - I) * Math.tan(H - PI23);
                                B = (c1 + c2) / 2;
                                R = (c1 - c2) / 2;
                            } else if (H < PI23) {
                                B = (1 - S) * I;
                                double c1 = 3 * I - B;
                                double c2 = SQRT3 * (B - I) * Math.tan(H - PI43);
                                R = (c1 + c2) / 2;
                                G = (c1 - c2) / 2;
                            }
                        }
                        dstPixels[dIndex++] = ((R < 0) ? 0 : ((R > 1.0) ? 1.0 : R)) * normr;
                        dstPixels[dIndex++] = ((G < 0) ? 0 : ((G > 1.0) ? 1.0 : G)) * normg;
                        dstPixels[dIndex++] = ((B < 0) ? 0 : ((B > 1.0) ? 1.0 : B)) * normb;
                    }
                }
            }
        } else if (!hasNodata && hasROI) {
            for (int j = 0; j < height; j++, iStart += srcLineStride, hStart += srcLineStride, sStart += srcLineStride) {

                int y0 = j + destY;
                for (int i = 0, iIndex = iStart, hIndex = hStart, sIndex = sStart; i < width; i++, iIndex += srcPixelStride, hIndex += srcPixelStride, sIndex += srcPixelStride) {

                    int x0 = i + destX;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        if (isByte) {
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[0];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[1];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[2];
                        } else {
                            dstPixels[dIndex++] = destNoDataFinal[0];
                            dstPixels[dIndex++] = destNoDataFinal[1];
                            dstPixels[dIndex++] = destNoDataFinal[2];
                        }
                        continue;
                    }

                    double I = (iBuf[iIndex] & 0xFF) * normi;
                    int h = hBuf[hIndex] & 0xFF;
                    double S = (sBuf[sIndex] & 0xFF) * norms;

                    if (isByte) {
                        float r, g, b;

                        r = g = b = (float) I;

                        if (S != 0.0) {
                            if (h >= 85 && h <= 170) {
                                r = (float) ((1 - S) * I);
                                float c1 = (float) (3 * I - r);
                                float c2 = (float) (SQRT3 * (r - I) * tanTable[h]);
                                g = (float) ((c1 + c2) / 2);
                                b = (float) ((c1 - c2) / 2);
                            } else if (h > 170) {
                                g = (float) ((1 - S) * I);
                                float c1 = (float) (3 * I - g);
                                float c2 = (float) (SQRT3 * (g - I) * tanTable[h - 85]);
                                b = (c1 + c2) / 2;
                                r = (c1 - c2) / 2;
                            } else if (h < 85) {
                                b = (float) ((1 - S) * I);
                                float c1 = (float) (3 * I - b);
                                float c2 = (float) (SQRT3 * (b - I) * tanTable[h + 85]);
                                r = (c1 + c2) / 2;
                                g = (c1 - c2) / 2;
                            }
                        }
                        dstIntPixels[dIndex++] = (int) (((r < 0.0f) ? 0.0f
                                : ((r > 1.0f) ? 1.0f : r)) * normr + 0.5);
                        dstIntPixels[dIndex++] = (int) (((g < 0.0f) ? 0.0f
                                : ((g > 1.0f) ? 1.0f : g)) * normg + 0.5);
                        dstIntPixels[dIndex++] = (int) (((b < 0.0f) ? 0.0f
                                : ((b > 1.0f) ? 1.0f : b)) * normb + 0.5);
                    } else {
                        double R, G, B;

                        R = G = B = I;
                        if (S != 0) {
                            double H = h * normh;
                            if (H >= PI23 && H <= PI43) {
                                R = (1 - S) * I;
                                double c1 = 3 * I - R;
                                double c2 = SQRT3 * (R - I) * Math.tan(H);
                                G = (c1 + c2) / 2;
                                B = (c1 - c2) / 2;
                            } else if (H > PI43) {
                                G = (1 - S) * I;
                                double c1 = 3 * I - G;
                                double c2 = SQRT3 * (G - I) * Math.tan(H - PI23);
                                B = (c1 + c2) / 2;
                                R = (c1 - c2) / 2;
                            } else if (H < PI23) {
                                B = (1 - S) * I;
                                double c1 = 3 * I - B;
                                double c2 = SQRT3 * (B - I) * Math.tan(H - PI43);
                                R = (c1 + c2) / 2;
                                G = (c1 - c2) / 2;
                            }
                        }
                        dstPixels[dIndex++] = ((R < 0) ? 0 : ((R > 1.0) ? 1.0 : R)) * normr;
                        dstPixels[dIndex++] = ((G < 0) ? 0 : ((G > 1.0) ? 1.0 : G)) * normg;
                        dstPixels[dIndex++] = ((B < 0) ? 0 : ((B > 1.0) ? 1.0 : B)) * normb;
                    }
                }
            }
        } else if (hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, iStart += srcLineStride, hStart += srcLineStride, sStart += srcLineStride) {
                for (int i = 0, iIndex = iStart, hIndex = hStart, sIndex = sStart; i < width; i++, iIndex += srcPixelStride, hIndex += srcPixelStride, sIndex += srcPixelStride) {

                    byte iB = iBuf[iIndex];
                    byte hB = hBuf[hIndex];
                    byte sB = sBuf[sIndex];

                    boolean notValid = nodata.contains(iB) || nodata.contains(hB)
                            || nodata.contains(sB);

                    if (notValid) {
                        if (isByte) {
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[0];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[1];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[2];
                        } else {
                            dstPixels[dIndex++] = destNoDataFinal[0];
                            dstPixels[dIndex++] = destNoDataFinal[1];
                            dstPixels[dIndex++] = destNoDataFinal[2];
                        }
                        continue;
                    }

                    double I = (iB & 0xFF) * normi;
                    int h = hB & 0xFF;
                    double S = (sB & 0xFF) * norms;

                    if (isByte) {
                        float r, g, b;

                        r = g = b = (float) I;

                        if (S != 0.0) {
                            if (h >= 85 && h <= 170) {
                                r = (float) ((1 - S) * I);
                                float c1 = (float) (3 * I - r);
                                float c2 = (float) (SQRT3 * (r - I) * tanTable[h]);
                                g = (float) ((c1 + c2) / 2);
                                b = (float) ((c1 - c2) / 2);
                            } else if (h > 170) {
                                g = (float) ((1 - S) * I);
                                float c1 = (float) (3 * I - g);
                                float c2 = (float) (SQRT3 * (g - I) * tanTable[h - 85]);
                                b = (c1 + c2) / 2;
                                r = (c1 - c2) / 2;
                            } else if (h < 85) {
                                b = (float) ((1 - S) * I);
                                float c1 = (float) (3 * I - b);
                                float c2 = (float) (SQRT3 * (b - I) * tanTable[h + 85]);
                                r = (c1 + c2) / 2;
                                g = (c1 - c2) / 2;
                            }
                        }
                        dstIntPixels[dIndex++] = (int) (((r < 0.0f) ? 0.0f
                                : ((r > 1.0f) ? 1.0f : r)) * normr + 0.5);
                        dstIntPixels[dIndex++] = (int) (((g < 0.0f) ? 0.0f
                                : ((g > 1.0f) ? 1.0f : g)) * normg + 0.5);
                        dstIntPixels[dIndex++] = (int) (((b < 0.0f) ? 0.0f
                                : ((b > 1.0f) ? 1.0f : b)) * normb + 0.5);
                    } else {
                        double R, G, B;

                        R = G = B = I;
                        if (S != 0) {
                            double H = h * normh;
                            if (H >= PI23 && H <= PI43) {
                                R = (1 - S) * I;
                                double c1 = 3 * I - R;
                                double c2 = SQRT3 * (R - I) * Math.tan(H);
                                G = (c1 + c2) / 2;
                                B = (c1 - c2) / 2;
                            } else if (H > PI43) {
                                G = (1 - S) * I;
                                double c1 = 3 * I - G;
                                double c2 = SQRT3 * (G - I) * Math.tan(H - PI23);
                                B = (c1 + c2) / 2;
                                R = (c1 - c2) / 2;
                            } else if (H < PI23) {
                                B = (1 - S) * I;
                                double c1 = 3 * I - B;
                                double c2 = SQRT3 * (B - I) * Math.tan(H - PI43);
                                R = (c1 + c2) / 2;
                                G = (c1 - c2) / 2;
                            }
                        }
                        dstPixels[dIndex++] = ((R < 0) ? 0 : ((R > 1.0) ? 1.0 : R)) * normr;
                        dstPixels[dIndex++] = ((G < 0) ? 0 : ((G > 1.0) ? 1.0 : G)) * normg;
                        dstPixels[dIndex++] = ((B < 0) ? 0 : ((B > 1.0) ? 1.0 : B)) * normb;
                    }
                }
            }
        } else {
            for (int j = 0; j < height; j++, iStart += srcLineStride, hStart += srcLineStride, sStart += srcLineStride) {

                int y0 = j + destY;
                for (int i = 0, iIndex = iStart, hIndex = hStart, sIndex = sStart; i < width; i++, iIndex += srcPixelStride, hIndex += srcPixelStride, sIndex += srcPixelStride) {

                    int x0 = i + destX;

                    byte iB = iBuf[iIndex];
                    byte hB = hBuf[hIndex];
                    byte sB = sBuf[sIndex];

                    boolean notValid = nodata.contains(iB) || nodata.contains(hB)
                            || nodata.contains(sB);

                    if (notValid
                            || !(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        if (isByte) {
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[0];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[1];
                            dstIntPixels[dIndex++] = (int) destNoDataFinal[2];
                        } else {
                            dstPixels[dIndex++] = destNoDataFinal[0];
                            dstPixels[dIndex++] = destNoDataFinal[1];
                            dstPixels[dIndex++] = destNoDataFinal[2];
                        }
                        continue;
                    }

                    double I = (iB & 0xFF) * normi;
                    int h = hB & 0xFF;
                    double S = (sB & 0xFF) * norms;

                    if (isByte) {
                        float r, g, b;

                        r = g = b = (float) I;

                        if (S != 0.0) {
                            if (h >= 85 && h <= 170) {
                                r = (float) ((1 - S) * I);
                                float c1 = (float) (3 * I - r);
                                float c2 = (float) (SQRT3 * (r - I) * tanTable[h]);
                                g = (float) ((c1 + c2) / 2);
                                b = (float) ((c1 - c2) / 2);
                            } else if (h > 170) {
                                g = (float) ((1 - S) * I);
                                float c1 = (float) (3 * I - g);
                                float c2 = (float) (SQRT3 * (g - I) * tanTable[h - 85]);
                                b = (c1 + c2) / 2;
                                r = (c1 - c2) / 2;
                            } else if (h < 85) {
                                b = (float) ((1 - S) * I);
                                float c1 = (float) (3 * I - b);
                                float c2 = (float) (SQRT3 * (b - I) * tanTable[h + 85]);
                                r = (c1 + c2) / 2;
                                g = (c1 - c2) / 2;
                            }
                        }
                        dstIntPixels[dIndex++] = (int) (((r < 0.0f) ? 0.0f
                                : ((r > 1.0f) ? 1.0f : r)) * normr + 0.5);
                        dstIntPixels[dIndex++] = (int) (((g < 0.0f) ? 0.0f
                                : ((g > 1.0f) ? 1.0f : g)) * normg + 0.5);
                        dstIntPixels[dIndex++] = (int) (((b < 0.0f) ? 0.0f
                                : ((b > 1.0f) ? 1.0f : b)) * normb + 0.5);
                    } else {
                        double R, G, B;

                        R = G = B = I;
                        if (S != 0) {
                            double H = h * normh;
                            if (H >= PI23 && H <= PI43) {
                                R = (1 - S) * I;
                                double c1 = 3 * I - R;
                                double c2 = SQRT3 * (R - I) * Math.tan(H);
                                G = (c1 + c2) / 2;
                                B = (c1 - c2) / 2;
                            } else if (H > PI43) {
                                G = (1 - S) * I;
                                double c1 = 3 * I - G;
                                double c2 = SQRT3 * (G - I) * Math.tan(H - PI23);
                                B = (c1 + c2) / 2;
                                R = (c1 - c2) / 2;
                            } else if (H < PI23) {
                                B = (1 - S) * I;
                                double c1 = 3 * I - B;
                                double c2 = SQRT3 * (B - I) * Math.tan(H - PI43);
                                R = (c1 + c2) / 2;
                                G = (c1 - c2) / 2;
                            }
                        }
                        dstPixels[dIndex++] = ((R < 0) ? 0 : ((R > 1.0) ? 1.0 : R)) * normr;
                        dstPixels[dIndex++] = ((G < 0) ? 0 : ((G > 1.0) ? 1.0 : G)) * normg;
                        dstPixels[dIndex++] = ((B < 0) ? 0 : ((B > 1.0) ? 1.0 : B)) * normb;
                    }
                }
            }
        }

        if (isByte)
            dest.setPixels(dest.getMinX(), dest.getMinY(), width, height, dstIntPixels);
        else {
            convertToSigned(dstPixels, dstType);
            dest.setPixels(dest.getMinX(), dest.getMinY(), width, height, dstPixels);
        }

    }

    // convert a short type ratser from IHS to RGB
    private void toRGBShort(UnpackedImageData src, int[] srcComponentSize, WritableRaster dest,
            int[] destComponentSize, boolean roiContainsTile, boolean roiDisjointTile,
            RandomIter roiIter, Rectangle roiBounds, Range nodata, float[] destNodata) {
        short[] iBuf = src.getShortData(0);
        short[] hBuf = src.getShortData(1);
        short[] sBuf = src.getShortData(2);

        double normi = 1.0 / ((1 << srcComponentSize[0]) - 1);
        double normh = 1.0 / ((1 << srcComponentSize[1]) - 1) * PI2;
        double norms = 1.0 / ((1 << srcComponentSize[2]) - 1);

        double normr = 1.0, normg = 1.0, normb = 1.0;

        int dstType = dest.getSampleModel().getDataType();

        if (dstType < DataBuffer.TYPE_FLOAT) {
            normr = (1l << destComponentSize[0]) - 1;
            normg = (1l << destComponentSize[1]) - 1;
            normb = (1l << destComponentSize[2]) - 1;
        }

        int height = dest.getHeight();
        int width = dest.getWidth();

        double[] dstPixels = new double[3 * height * width];

        int iStart = src.bandOffsets[0];
        int hStart = src.bandOffsets[1];
        int sStart = src.bandOffsets[2];
        int srcPixelStride = src.pixelStride;
        int srcLineStride = src.lineStride;

        int dIndex = 0;

        boolean hasROI = roiIter != null && roiBounds != null;
        boolean hasNodata = nodata != null;

        int destX = dest.getMinX();
        int destY = dest.getMinY();

        double[] destNoDataFinal = getConvertedNodataToRGB(destNodata, DataBuffer.TYPE_SHORT,
                normi, normh, norms, normr, normg, normb);

        if (roiDisjointTile) {
            ImageUtil.fillBackground(dest, dest.getBounds(), destNoDataFinal);
            return;
        }

        if (!hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, iStart += srcLineStride, hStart += srcLineStride, sStart += srcLineStride) {
                for (int i = 0, iIndex = iStart, hIndex = hStart, sIndex = sStart; i < width; i++, iIndex += srcPixelStride, hIndex += srcPixelStride, sIndex += srcPixelStride) {
                    double I = (iBuf[iIndex] & 0xFFFF) * normi;
                    double H = (hBuf[hIndex] & 0xFFFF) * normh;
                    double S = (sBuf[sIndex] & 0xFFFF) * norms;
                    double R, G, B;

                    R = G = B = I;
                    if (S != 0.0) {
                        if (H >= PI23 && H <= PI43) {
                            R = (1 - S) * I;
                            double c1 = 3 * I - R;
                            double c2 = SQRT3 * (R - I) * Math.tan(H);
                            G = (c1 + c2) / 2;
                            B = (c1 - c2) / 2;
                        } else if (H > PI43) {
                            G = (1 - S) * I;
                            double c1 = 3 * I - G;
                            double c2 = SQRT3 * (G - I) * Math.tan(H - PI23);
                            B = (c1 + c2) / 2;
                            R = (c1 - c2) / 2;
                        } else if (H < PI23) {
                            B = (1 - S) * I;
                            double c1 = 3 * I - B;
                            double c2 = SQRT3 * (B - I) * Math.tan(H - PI43);
                            R = (c1 + c2) / 2;
                            G = (c1 - c2) / 2;
                        }
                    }

                    dstPixels[dIndex++] = ((R < 0) ? 0 : ((R > 1.0) ? 1.0 : R)) * normr;
                    dstPixels[dIndex++] = ((G < 0) ? 0 : ((G > 1.0) ? 1.0 : G)) * normg;
                    dstPixels[dIndex++] = ((B < 0) ? 0 : ((B > 1.0) ? 1.0 : B)) * normb;
                }
            }
        } else if (!hasNodata && hasROI) {
            for (int j = 0; j < height; j++, iStart += srcLineStride, hStart += srcLineStride, sStart += srcLineStride) {

                int y0 = j + destY;
                for (int i = 0, iIndex = iStart, hIndex = hStart, sIndex = sStart; i < width; i++, iIndex += srcPixelStride, hIndex += srcPixelStride, sIndex += srcPixelStride) {
                    int x0 = i + destX;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    double I = (iBuf[iIndex] & 0xFFFF) * normi;
                    double H = (hBuf[hIndex] & 0xFFFF) * normh;
                    double S = (sBuf[sIndex] & 0xFFFF) * norms;
                    double R, G, B;

                    R = G = B = I;
                    if (S != 0.0) {
                        if (H >= PI23 && H <= PI43) {
                            R = (1 - S) * I;
                            double c1 = 3 * I - R;
                            double c2 = SQRT3 * (R - I) * Math.tan(H);
                            G = (c1 + c2) / 2;
                            B = (c1 - c2) / 2;
                        } else if (H > PI43) {
                            G = (1 - S) * I;
                            double c1 = 3 * I - G;
                            double c2 = SQRT3 * (G - I) * Math.tan(H - PI23);
                            B = (c1 + c2) / 2;
                            R = (c1 - c2) / 2;
                        } else if (H < PI23) {
                            B = (1 - S) * I;
                            double c1 = 3 * I - B;
                            double c2 = SQRT3 * (B - I) * Math.tan(H - PI43);
                            R = (c1 + c2) / 2;
                            G = (c1 - c2) / 2;
                        }
                    }

                    dstPixels[dIndex++] = ((R < 0) ? 0 : ((R > 1.0) ? 1.0 : R)) * normr;
                    dstPixels[dIndex++] = ((G < 0) ? 0 : ((G > 1.0) ? 1.0 : G)) * normg;
                    dstPixels[dIndex++] = ((B < 0) ? 0 : ((B > 1.0) ? 1.0 : B)) * normb;
                }
            }
        } else if (hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, iStart += srcLineStride, hStart += srcLineStride, sStart += srcLineStride) {
                for (int i = 0, iIndex = iStart, hIndex = hStart, sIndex = sStart; i < width; i++, iIndex += srcPixelStride, hIndex += srcPixelStride, sIndex += srcPixelStride) {

                    short iB = iBuf[iIndex];
                    short hB = hBuf[hIndex];
                    short sB = sBuf[sIndex];

                    boolean notValid = nodata.contains(iB) || nodata.contains(hB)
                            || nodata.contains(sB);

                    if (notValid) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    double I = (iB & 0xFFFF) * normi;
                    double H = (hB & 0xFFFF) * normh;
                    double S = (sB & 0xFFFF) * norms;
                    double R, G, B;

                    R = G = B = I;
                    if (S != 0.0) {
                        if (H >= PI23 && H <= PI43) {
                            R = (1 - S) * I;
                            double c1 = 3 * I - R;
                            double c2 = SQRT3 * (R - I) * Math.tan(H);
                            G = (c1 + c2) / 2;
                            B = (c1 - c2) / 2;
                        } else if (H > PI43) {
                            G = (1 - S) * I;
                            double c1 = 3 * I - G;
                            double c2 = SQRT3 * (G - I) * Math.tan(H - PI23);
                            B = (c1 + c2) / 2;
                            R = (c1 - c2) / 2;
                        } else if (H < PI23) {
                            B = (1 - S) * I;
                            double c1 = 3 * I - B;
                            double c2 = SQRT3 * (B - I) * Math.tan(H - PI43);
                            R = (c1 + c2) / 2;
                            G = (c1 - c2) / 2;
                        }
                    }

                    dstPixels[dIndex++] = ((R < 0) ? 0 : ((R > 1.0) ? 1.0 : R)) * normr;
                    dstPixels[dIndex++] = ((G < 0) ? 0 : ((G > 1.0) ? 1.0 : G)) * normg;
                    dstPixels[dIndex++] = ((B < 0) ? 0 : ((B > 1.0) ? 1.0 : B)) * normb;
                }
            }
        } else {
            for (int j = 0; j < height; j++, iStart += srcLineStride, hStart += srcLineStride, sStart += srcLineStride) {

                int y0 = j + destY;
                for (int i = 0, iIndex = iStart, hIndex = hStart, sIndex = sStart; i < width; i++, iIndex += srcPixelStride, hIndex += srcPixelStride, sIndex += srcPixelStride) {
                    int x0 = i + destX;

                    short iB = iBuf[iIndex];
                    short hB = hBuf[hIndex];
                    short sB = sBuf[sIndex];

                    boolean notValid = nodata.contains(iB) || nodata.contains(hB)
                            || nodata.contains(sB);

                    if (notValid
                            || !(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    double I = (iB & 0xFFFF) * normi;
                    double H = (hB & 0xFFFF) * normh;
                    double S = (sB & 0xFFFF) * norms;
                    double R, G, B;

                    R = G = B = I;
                    if (S != 0.0) {
                        if (H >= PI23 && H <= PI43) {
                            R = (1 - S) * I;
                            double c1 = 3 * I - R;
                            double c2 = SQRT3 * (R - I) * Math.tan(H);
                            G = (c1 + c2) / 2;
                            B = (c1 - c2) / 2;
                        } else if (H > PI43) {
                            G = (1 - S) * I;
                            double c1 = 3 * I - G;
                            double c2 = SQRT3 * (G - I) * Math.tan(H - PI23);
                            B = (c1 + c2) / 2;
                            R = (c1 - c2) / 2;
                        } else if (H < PI23) {
                            B = (1 - S) * I;
                            double c1 = 3 * I - B;
                            double c2 = SQRT3 * (B - I) * Math.tan(H - PI43);
                            R = (c1 + c2) / 2;
                            G = (c1 - c2) / 2;
                        }
                    }

                    dstPixels[dIndex++] = ((R < 0) ? 0 : ((R > 1.0) ? 1.0 : R)) * normr;
                    dstPixels[dIndex++] = ((G < 0) ? 0 : ((G > 1.0) ? 1.0 : G)) * normg;
                    dstPixels[dIndex++] = ((B < 0) ? 0 : ((B > 1.0) ? 1.0 : B)) * normb;
                }
            }
        }

        convertToSigned(dstPixels, dstType);
        dest.setPixels(dest.getMinX(), dest.getMinY(), width, height, dstPixels);
    }

    private void toRGBInt(UnpackedImageData src, int[] srcComponentSize, WritableRaster dest,
            int[] destComponentSize, boolean roiContainsTile, boolean roiDisjointTile,
            RandomIter roiIter, Rectangle roiBounds, Range nodata, float[] destNodata) {
        int[] iBuf = src.getIntData(0);
        int[] hBuf = src.getIntData(1);
        int[] sBuf = src.getIntData(2);

        double normi = 1.0 / ((1l << srcComponentSize[0]) - 1);
        double normh = 1.0 / ((1l << srcComponentSize[1]) - 1) * PI2;
        double norms = 1.0 / ((1l << srcComponentSize[2]) - 1);

        double normr = 1.0, normg = 1.0, normb = 1.0;

        int dstType = dest.getSampleModel().getDataType();

        if (dstType < DataBuffer.TYPE_FLOAT) {
            normr = (1l << destComponentSize[0]) - 1;
            normg = (1l << destComponentSize[1]) - 1;
            normb = (1l << destComponentSize[2]) - 1;
        }

        int height = dest.getHeight();
        int width = dest.getWidth();

        double[] dstPixels = new double[3 * height * width];

        int iStart = src.bandOffsets[0];
        int hStart = src.bandOffsets[1];
        int sStart = src.bandOffsets[2];
        int srcPixelStride = src.pixelStride;
        int srcLineStride = src.lineStride;

        int dIndex = 0;

        boolean hasROI = roiIter != null && roiBounds != null;
        boolean hasNodata = nodata != null;

        int destX = dest.getMinX();
        int destY = dest.getMinY();

        double[] destNoDataFinal = getConvertedNodataToRGB(destNodata, DataBuffer.TYPE_INT, normi,
                normh, norms, normr, normg, normb);

        if (roiDisjointTile) {
            ImageUtil.fillBackground(dest, dest.getBounds(), destNoDataFinal);
            return;
        }

        if (!hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, iStart += srcLineStride, hStart += srcLineStride, sStart += srcLineStride) {
                for (int i = 0, iIndex = iStart, hIndex = hStart, sIndex = sStart; i < width; i++, iIndex += srcPixelStride, hIndex += srcPixelStride, sIndex += srcPixelStride) {
                    double I = (iBuf[iIndex] & 0xFFFFFFFFl) * normi;
                    double H = (hBuf[hIndex] & 0xFFFFFFFFl) * normh;
                    double S = (sBuf[sIndex] & 0xFFFFFFFFl) * norms;
                    double R, G, B;

                    R = G = B = I;
                    if (S != 0.0) {
                        if (H >= PI23 && H <= PI43) {
                            R = (1 - S) * I;
                            double c1 = 3 * I - R;
                            double c2 = SQRT3 * (R - I) * Math.tan(H);
                            G = (c1 + c2) / 2;
                            B = (c1 - c2) / 2;
                        } else if (H > PI43) {
                            G = (1 - S) * I;
                            double c1 = 3 * I - G;
                            double c2 = SQRT3 * (G - I) * Math.tan(H - PI23);
                            B = (c1 + c2) / 2;
                            R = (c1 - c2) / 2;
                        } else if (H < PI23) {
                            B = (1 - S) * I;
                            double c1 = 3 * I - B;
                            double c2 = SQRT3 * (B - I) * Math.tan(H - PI43);
                            R = (c1 + c2) / 2;
                            G = (c1 - c2) / 2;
                        }
                    }

                    dstPixels[dIndex++] = ((R < 0) ? 0 : ((R > 1.0) ? 1.0 : R)) * normr;
                    dstPixels[dIndex++] = ((G < 0) ? 0 : ((G > 1.0) ? 1.0 : G)) * normg;
                    dstPixels[dIndex++] = ((B < 0) ? 0 : ((B > 1.0) ? 1.0 : B)) * normb;
                }
            }
        } else if (!hasNodata && hasROI) {
            for (int j = 0; j < height; j++, iStart += srcLineStride, hStart += srcLineStride, sStart += srcLineStride) {

                int y0 = j + destY;
                for (int i = 0, iIndex = iStart, hIndex = hStart, sIndex = sStart; i < width; i++, iIndex += srcPixelStride, hIndex += srcPixelStride, sIndex += srcPixelStride) {
                    int x0 = i + destX;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    double I = (iBuf[iIndex] & 0xFFFFFFFFl) * normi;
                    double H = (hBuf[hIndex] & 0xFFFFFFFFl) * normh;
                    double S = (sBuf[sIndex] & 0xFFFFFFFFl) * norms;
                    double R, G, B;

                    R = G = B = I;
                    if (S != 0.0) {
                        if (H >= PI23 && H <= PI43) {
                            R = (1 - S) * I;
                            double c1 = 3 * I - R;
                            double c2 = SQRT3 * (R - I) * Math.tan(H);
                            G = (c1 + c2) / 2;
                            B = (c1 - c2) / 2;
                        } else if (H > PI43) {
                            G = (1 - S) * I;
                            double c1 = 3 * I - G;
                            double c2 = SQRT3 * (G - I) * Math.tan(H - PI23);
                            B = (c1 + c2) / 2;
                            R = (c1 - c2) / 2;
                        } else if (H < PI23) {
                            B = (1 - S) * I;
                            double c1 = 3 * I - B;
                            double c2 = SQRT3 * (B - I) * Math.tan(H - PI43);
                            R = (c1 + c2) / 2;
                            G = (c1 - c2) / 2;
                        }
                    }

                    dstPixels[dIndex++] = ((R < 0) ? 0 : ((R > 1.0) ? 1.0 : R)) * normr;
                    dstPixels[dIndex++] = ((G < 0) ? 0 : ((G > 1.0) ? 1.0 : G)) * normg;
                    dstPixels[dIndex++] = ((B < 0) ? 0 : ((B > 1.0) ? 1.0 : B)) * normb;
                }
            }
        } else if (hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, iStart += srcLineStride, hStart += srcLineStride, sStart += srcLineStride) {
                for (int i = 0, iIndex = iStart, hIndex = hStart, sIndex = sStart; i < width; i++, iIndex += srcPixelStride, hIndex += srcPixelStride, sIndex += srcPixelStride) {

                    int iB = iBuf[iIndex];
                    int hB = hBuf[hIndex];
                    int sB = sBuf[sIndex];

                    boolean notValid = nodata.contains(iB) || nodata.contains(hB)
                            || nodata.contains(sB);

                    if (notValid) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    double I = (iB & 0xFFFFFFFFl) * normi;
                    double H = (hB & 0xFFFFFFFFl) * normh;
                    double S = (sB & 0xFFFFFFFFl) * norms;
                    double R, G, B;

                    R = G = B = I;
                    if (S != 0.0) {
                        if (H >= PI23 && H <= PI43) {
                            R = (1 - S) * I;
                            double c1 = 3 * I - R;
                            double c2 = SQRT3 * (R - I) * Math.tan(H);
                            G = (c1 + c2) / 2;
                            B = (c1 - c2) / 2;
                        } else if (H > PI43) {
                            G = (1 - S) * I;
                            double c1 = 3 * I - G;
                            double c2 = SQRT3 * (G - I) * Math.tan(H - PI23);
                            B = (c1 + c2) / 2;
                            R = (c1 - c2) / 2;
                        } else if (H < PI23) {
                            B = (1 - S) * I;
                            double c1 = 3 * I - B;
                            double c2 = SQRT3 * (B - I) * Math.tan(H - PI43);
                            R = (c1 + c2) / 2;
                            G = (c1 - c2) / 2;
                        }
                    }

                    dstPixels[dIndex++] = ((R < 0) ? 0 : ((R > 1.0) ? 1.0 : R)) * normr;
                    dstPixels[dIndex++] = ((G < 0) ? 0 : ((G > 1.0) ? 1.0 : G)) * normg;
                    dstPixels[dIndex++] = ((B < 0) ? 0 : ((B > 1.0) ? 1.0 : B)) * normb;
                }
            }
        } else {
            for (int j = 0; j < height; j++, iStart += srcLineStride, hStart += srcLineStride, sStart += srcLineStride) {

                int y0 = j + destY;
                for (int i = 0, iIndex = iStart, hIndex = hStart, sIndex = sStart; i < width; i++, iIndex += srcPixelStride, hIndex += srcPixelStride, sIndex += srcPixelStride) {
                    int x0 = i + destX;

                    int iB = iBuf[iIndex];
                    int hB = hBuf[hIndex];
                    int sB = sBuf[sIndex];

                    boolean notValid = nodata.contains(iB) || nodata.contains(hB)
                            || nodata.contains(sB);

                    if (notValid
                            || !(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    double I = (iB & 0xFFFFFFFFl) * normi;
                    double H = (hB & 0xFFFFFFFFl) * normh;
                    double S = (sB & 0xFFFFFFFFl) * norms;
                    double R, G, B;

                    R = G = B = I;
                    if (S != 0.0) {
                        if (H >= PI23 && H <= PI43) {
                            R = (1 - S) * I;
                            double c1 = 3 * I - R;
                            double c2 = SQRT3 * (R - I) * Math.tan(H);
                            G = (c1 + c2) / 2;
                            B = (c1 - c2) / 2;
                        } else if (H > PI43) {
                            G = (1 - S) * I;
                            double c1 = 3 * I - G;
                            double c2 = SQRT3 * (G - I) * Math.tan(H - PI23);
                            B = (c1 + c2) / 2;
                            R = (c1 - c2) / 2;
                        } else if (H < PI23) {
                            B = (1 - S) * I;
                            double c1 = 3 * I - B;
                            double c2 = SQRT3 * (B - I) * Math.tan(H - PI43);
                            R = (c1 + c2) / 2;
                            G = (c1 - c2) / 2;
                        }
                    }

                    dstPixels[dIndex++] = ((R < 0) ? 0 : ((R > 1.0) ? 1.0 : R)) * normr;
                    dstPixels[dIndex++] = ((G < 0) ? 0 : ((G > 1.0) ? 1.0 : G)) * normg;
                    dstPixels[dIndex++] = ((B < 0) ? 0 : ((B > 1.0) ? 1.0 : B)) * normb;
                }
            }
        }

        convertToSigned(dstPixels, dstType);
        dest.setPixels(dest.getMinX(), dest.getMinY(), width, height, dstPixels);
    }

    private void toRGBFloat(UnpackedImageData src, int[] srcComponentSize, WritableRaster dest,
            int[] destComponentSize, boolean roiContainsTile, boolean roiDisjointTile,
            RandomIter roiIter, Rectangle roiBounds, Range nodata, float[] destNodata) {
        float[] iBuf = src.getFloatData(0);
        float[] hBuf = src.getFloatData(1);
        float[] sBuf = src.getFloatData(2);

        double normr = 1.0, normg = 1.0, normb = 1.0;

        int dstType = dest.getSampleModel().getDataType();

        if (dstType < DataBuffer.TYPE_FLOAT) {
            normr = (1l << destComponentSize[0]) - 1;
            normg = (1l << destComponentSize[1]) - 1;
            normb = (1l << destComponentSize[2]) - 1;
        }

        int height = dest.getHeight();
        int width = dest.getWidth();

        double[] dstPixels = new double[3 * height * width];

        int iStart = src.bandOffsets[0];
        int hStart = src.bandOffsets[1];
        int sStart = src.bandOffsets[2];
        int srcPixelStride = src.pixelStride;
        int srcLineStride = src.lineStride;

        int dIndex = 0;

        boolean hasROI = roiIter != null && roiBounds != null;
        boolean hasNodata = nodata != null;
        if (hasNodata) {
            nodata = RangeFactory.convertToDoubleRange(nodata);
        }

        int destX = dest.getMinX();
        int destY = dest.getMinY();

        double[] destNoDataFinal = getConvertedNodataToRGB(destNodata, DataBuffer.TYPE_FLOAT, 0, 0,
                0, normr, normg, normb);

        if (roiDisjointTile) {
            ImageUtil.fillBackground(dest, dest.getBounds(), destNoDataFinal);
            return;
        }

        if (!hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, iStart += srcLineStride, hStart += srcLineStride, sStart += srcLineStride) {
                for (int i = 0, iIndex = iStart, hIndex = hStart, sIndex = sStart; i < width; i++, iIndex += srcPixelStride, hIndex += srcPixelStride, sIndex += srcPixelStride) {
                    double I = iBuf[iIndex];
                    double H = hBuf[hIndex];
                    double S = sBuf[sIndex];
                    double R, G, B;

                    R = G = B = I;
                    if (S != 0.0) {
                        if (H >= PI23 && H <= PI43) {
                            R = (1 - S) * I;
                            double c1 = 3 * I - R;
                            double c2 = SQRT3 * (R - I) * Math.tan(H);
                            G = (c1 + c2) / 2;
                            B = (c1 - c2) / 2;
                        } else if (H > PI43) {
                            G = (1 - S) * I;
                            double c1 = 3 * I - G;
                            double c2 = SQRT3 * (G - I) * Math.tan(H - PI23);
                            B = (c1 + c2) / 2;
                            R = (c1 - c2) / 2;
                        } else if (H < PI23) {
                            B = (1 - S) * I;
                            double c1 = 3 * I - B;
                            double c2 = SQRT3 * (B - I) * Math.tan(H - PI43);
                            R = (c1 + c2) / 2;
                            G = (c1 - c2) / 2;
                        }
                    }

                    dstPixels[dIndex++] = ((R < 0) ? 0 : ((R > 1.0) ? 1.0 : R)) * normr;
                    dstPixels[dIndex++] = ((G < 0) ? 0 : ((G > 1.0) ? 1.0 : G)) * normg;
                    dstPixels[dIndex++] = ((B < 0) ? 0 : ((B > 1.0) ? 1.0 : B)) * normb;
                }
            }
        } else if (!hasNodata && hasROI) {
            for (int j = 0; j < height; j++, iStart += srcLineStride, hStart += srcLineStride, sStart += srcLineStride) {

                int y0 = j + destY;
                for (int i = 0, iIndex = iStart, hIndex = hStart, sIndex = sStart; i < width; i++, iIndex += srcPixelStride, hIndex += srcPixelStride, sIndex += srcPixelStride) {
                    int x0 = i + destX;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    double I = iBuf[iIndex];
                    double H = hBuf[hIndex];
                    double S = sBuf[sIndex];
                    double R, G, B;

                    R = G = B = I;
                    if (S != 0.0) {
                        if (H >= PI23 && H <= PI43) {
                            R = (1 - S) * I;
                            double c1 = 3 * I - R;
                            double c2 = SQRT3 * (R - I) * Math.tan(H);
                            G = (c1 + c2) / 2;
                            B = (c1 - c2) / 2;
                        } else if (H > PI43) {
                            G = (1 - S) * I;
                            double c1 = 3 * I - G;
                            double c2 = SQRT3 * (G - I) * Math.tan(H - PI23);
                            B = (c1 + c2) / 2;
                            R = (c1 - c2) / 2;
                        } else if (H < PI23) {
                            B = (1 - S) * I;
                            double c1 = 3 * I - B;
                            double c2 = SQRT3 * (B - I) * Math.tan(H - PI43);
                            R = (c1 + c2) / 2;
                            G = (c1 - c2) / 2;
                        }
                    }

                    dstPixels[dIndex++] = ((R < 0) ? 0 : ((R > 1.0) ? 1.0 : R)) * normr;
                    dstPixels[dIndex++] = ((G < 0) ? 0 : ((G > 1.0) ? 1.0 : G)) * normg;
                    dstPixels[dIndex++] = ((B < 0) ? 0 : ((B > 1.0) ? 1.0 : B)) * normb;
                }
            }
        } else if (hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, iStart += srcLineStride, hStart += srcLineStride, sStart += srcLineStride) {
                for (int i = 0, iIndex = iStart, hIndex = hStart, sIndex = sStart; i < width; i++, iIndex += srcPixelStride, hIndex += srcPixelStride, sIndex += srcPixelStride) {

                    double I = iBuf[iIndex];
                    double H = hBuf[hIndex];
                    double S = sBuf[sIndex];

                    boolean notValid = nodata.contains(I) || nodata.contains(H)
                            || nodata.contains(S);

                    if (notValid) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    double R, G, B;

                    R = G = B = I;
                    if (S != 0.0) {
                        if (H >= PI23 && H <= PI43) {
                            R = (1 - S) * I;
                            double c1 = 3 * I - R;
                            double c2 = SQRT3 * (R - I) * Math.tan(H);
                            G = (c1 + c2) / 2;
                            B = (c1 - c2) / 2;
                        } else if (H > PI43) {
                            G = (1 - S) * I;
                            double c1 = 3 * I - G;
                            double c2 = SQRT3 * (G - I) * Math.tan(H - PI23);
                            B = (c1 + c2) / 2;
                            R = (c1 - c2) / 2;
                        } else if (H < PI23) {
                            B = (1 - S) * I;
                            double c1 = 3 * I - B;
                            double c2 = SQRT3 * (B - I) * Math.tan(H - PI43);
                            R = (c1 + c2) / 2;
                            G = (c1 - c2) / 2;
                        }
                    }

                    dstPixels[dIndex++] = ((R < 0) ? 0 : ((R > 1.0) ? 1.0 : R)) * normr;
                    dstPixels[dIndex++] = ((G < 0) ? 0 : ((G > 1.0) ? 1.0 : G)) * normg;
                    dstPixels[dIndex++] = ((B < 0) ? 0 : ((B > 1.0) ? 1.0 : B)) * normb;
                }
            }
        } else {
            for (int j = 0; j < height; j++, iStart += srcLineStride, hStart += srcLineStride, sStart += srcLineStride) {

                int y0 = j + destY;
                for (int i = 0, iIndex = iStart, hIndex = hStart, sIndex = sStart; i < width; i++, iIndex += srcPixelStride, hIndex += srcPixelStride, sIndex += srcPixelStride) {
                    int x0 = i + destX;

                    double I = iBuf[iIndex];
                    double H = hBuf[hIndex];
                    double S = sBuf[sIndex];

                    boolean notValid = nodata.contains(I) || nodata.contains(H)
                            || nodata.contains(S);

                    if (notValid
                            || !(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    double R, G, B;

                    R = G = B = I;
                    if (S != 0.0) {
                        if (H >= PI23 && H <= PI43) {
                            R = (1 - S) * I;
                            double c1 = 3 * I - R;
                            double c2 = SQRT3 * (R - I) * Math.tan(H);
                            G = (c1 + c2) / 2;
                            B = (c1 - c2) / 2;
                        } else if (H > PI43) {
                            G = (1 - S) * I;
                            double c1 = 3 * I - G;
                            double c2 = SQRT3 * (G - I) * Math.tan(H - PI23);
                            B = (c1 + c2) / 2;
                            R = (c1 - c2) / 2;
                        } else if (H < PI23) {
                            B = (1 - S) * I;
                            double c1 = 3 * I - B;
                            double c2 = SQRT3 * (B - I) * Math.tan(H - PI43);
                            R = (c1 + c2) / 2;
                            G = (c1 - c2) / 2;
                        }
                    }

                    dstPixels[dIndex++] = ((R < 0) ? 0 : ((R > 1.0) ? 1.0 : R)) * normr;
                    dstPixels[dIndex++] = ((G < 0) ? 0 : ((G > 1.0) ? 1.0 : G)) * normg;
                    dstPixels[dIndex++] = ((B < 0) ? 0 : ((B > 1.0) ? 1.0 : B)) * normb;
                }
            }
        }

        convertToSigned(dstPixels, dstType);
        dest.setPixels(dest.getMinX(), dest.getMinY(), width, height, dstPixels);
    }

    private void toRGBDouble(UnpackedImageData src, int[] srcComponentSize, WritableRaster dest,
            int[] destComponentSize, boolean roiContainsTile, boolean roiDisjointTile,
            RandomIter roiIter, Rectangle roiBounds, Range nodata, float[] destNodata) {
        double[] iBuf = src.getDoubleData(0);
        double[] hBuf = src.getDoubleData(1);
        double[] sBuf = src.getDoubleData(2);

        double normr = 1.0, normg = 1.0, normb = 1.0;

        int dstType = dest.getSampleModel().getDataType();

        if (dstType < DataBuffer.TYPE_FLOAT) {
            normr = (1l << destComponentSize[0]) - 1;
            normg = (1l << destComponentSize[1]) - 1;
            normb = (1l << destComponentSize[2]) - 1;
        }

        int height = dest.getHeight();
        int width = dest.getWidth();

        double[] dstPixels = new double[3 * height * width];

        int iStart = src.bandOffsets[0];
        int hStart = src.bandOffsets[1];
        int sStart = src.bandOffsets[2];
        int srcPixelStride = src.pixelStride;
        int srcLineStride = src.lineStride;

        int dIndex = 0;

        boolean hasROI = roiIter != null && roiBounds != null;
        boolean hasNodata = nodata != null;

        int destX = dest.getMinX();
        int destY = dest.getMinY();

        double[] destNoDataFinal = getConvertedNodataToRGB(destNodata, DataBuffer.TYPE_DOUBLE, 0,
                0, 0, normr, normg, normb);

        if (roiDisjointTile) {
            ImageUtil.fillBackground(dest, dest.getBounds(), destNoDataFinal);
            return;
        }

        if (!hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, iStart += srcLineStride, hStart += srcLineStride, sStart += srcLineStride) {
                for (int i = 0, iIndex = iStart, hIndex = hStart, sIndex = sStart; i < width; i++, iIndex += srcPixelStride, hIndex += srcPixelStride, sIndex += srcPixelStride) {
                    double I = iBuf[iIndex];
                    double H = hBuf[hIndex];
                    double S = sBuf[sIndex];
                    double R, G, B;

                    R = G = B = I;
                    if (S != 0.0) {
                        if (H >= PI23 && H <= PI43) {
                            R = (1 - S) * I;
                            double c1 = 3 * I - R;
                            double c2 = SQRT3 * (R - I) * Math.tan(H);
                            G = (c1 + c2) / 2;
                            B = (c1 - c2) / 2;
                        } else if (H > PI43) {
                            G = (1 - S) * I;
                            double c1 = 3 * I - G;
                            double c2 = SQRT3 * (G - I) * Math.tan(H - PI23);
                            B = (c1 + c2) / 2;
                            R = (c1 - c2) / 2;
                        } else if (H < PI23) {
                            B = (1 - S) * I;
                            double c1 = 3 * I - B;
                            double c2 = SQRT3 * (B - I) * Math.tan(H - PI43);
                            R = (c1 + c2) / 2;
                            G = (c1 - c2) / 2;
                        }
                    }

                    dstPixels[dIndex++] = ((R < 0) ? 0 : ((R > 1.0) ? 1.0 : R)) * normr;
                    dstPixels[dIndex++] = ((G < 0) ? 0 : ((G > 1.0) ? 1.0 : G)) * normg;
                    dstPixels[dIndex++] = ((B < 0) ? 0 : ((B > 1.0) ? 1.0 : B)) * normb;
                }
            }
        } else if (!hasNodata && hasROI) {
            for (int j = 0; j < height; j++, iStart += srcLineStride, hStart += srcLineStride, sStart += srcLineStride) {

                int y0 = j + destY;
                for (int i = 0, iIndex = iStart, hIndex = hStart, sIndex = sStart; i < width; i++, iIndex += srcPixelStride, hIndex += srcPixelStride, sIndex += srcPixelStride) {
                    int x0 = i + destX;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    double I = iBuf[iIndex];
                    double H = hBuf[hIndex];
                    double S = sBuf[sIndex];
                    double R, G, B;

                    R = G = B = I;
                    if (S != 0.0) {
                        if (H >= PI23 && H <= PI43) {
                            R = (1 - S) * I;
                            double c1 = 3 * I - R;
                            double c2 = SQRT3 * (R - I) * Math.tan(H);
                            G = (c1 + c2) / 2;
                            B = (c1 - c2) / 2;
                        } else if (H > PI43) {
                            G = (1 - S) * I;
                            double c1 = 3 * I - G;
                            double c2 = SQRT3 * (G - I) * Math.tan(H - PI23);
                            B = (c1 + c2) / 2;
                            R = (c1 - c2) / 2;
                        } else if (H < PI23) {
                            B = (1 - S) * I;
                            double c1 = 3 * I - B;
                            double c2 = SQRT3 * (B - I) * Math.tan(H - PI43);
                            R = (c1 + c2) / 2;
                            G = (c1 - c2) / 2;
                        }
                    }

                    dstPixels[dIndex++] = ((R < 0) ? 0 : ((R > 1.0) ? 1.0 : R)) * normr;
                    dstPixels[dIndex++] = ((G < 0) ? 0 : ((G > 1.0) ? 1.0 : G)) * normg;
                    dstPixels[dIndex++] = ((B < 0) ? 0 : ((B > 1.0) ? 1.0 : B)) * normb;
                }
            }
        } else if (hasNodata && (!hasROI || hasROI && roiContainsTile)) {
            for (int j = 0; j < height; j++, iStart += srcLineStride, hStart += srcLineStride, sStart += srcLineStride) {
                for (int i = 0, iIndex = iStart, hIndex = hStart, sIndex = sStart; i < width; i++, iIndex += srcPixelStride, hIndex += srcPixelStride, sIndex += srcPixelStride) {

                    double I = iBuf[iIndex];
                    double H = hBuf[hIndex];
                    double S = sBuf[sIndex];

                    boolean notValid = nodata.contains(I) || nodata.contains(H)
                            || nodata.contains(S);

                    if (notValid) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    double R, G, B;

                    R = G = B = I;
                    if (S != 0.0) {
                        if (H >= PI23 && H <= PI43) {
                            R = (1 - S) * I;
                            double c1 = 3 * I - R;
                            double c2 = SQRT3 * (R - I) * Math.tan(H);
                            G = (c1 + c2) / 2;
                            B = (c1 - c2) / 2;
                        } else if (H > PI43) {
                            G = (1 - S) * I;
                            double c1 = 3 * I - G;
                            double c2 = SQRT3 * (G - I) * Math.tan(H - PI23);
                            B = (c1 + c2) / 2;
                            R = (c1 - c2) / 2;
                        } else if (H < PI23) {
                            B = (1 - S) * I;
                            double c1 = 3 * I - B;
                            double c2 = SQRT3 * (B - I) * Math.tan(H - PI43);
                            R = (c1 + c2) / 2;
                            G = (c1 - c2) / 2;
                        }
                    }

                    dstPixels[dIndex++] = ((R < 0) ? 0 : ((R > 1.0) ? 1.0 : R)) * normr;
                    dstPixels[dIndex++] = ((G < 0) ? 0 : ((G > 1.0) ? 1.0 : G)) * normg;
                    dstPixels[dIndex++] = ((B < 0) ? 0 : ((B > 1.0) ? 1.0 : B)) * normb;
                }
            }
        } else {
            for (int j = 0; j < height; j++, iStart += srcLineStride, hStart += srcLineStride, sStart += srcLineStride) {

                int y0 = j + destY;
                for (int i = 0, iIndex = iStart, hIndex = hStart, sIndex = sStart; i < width; i++, iIndex += srcPixelStride, hIndex += srcPixelStride, sIndex += srcPixelStride) {
                    int x0 = i + destX;

                    double I = iBuf[iIndex];
                    double H = hBuf[hIndex];
                    double S = sBuf[sIndex];

                    boolean notValid = nodata.contains(I) || nodata.contains(H)
                            || nodata.contains(S);

                    if (notValid
                            || !(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        dstPixels[dIndex++] = destNoDataFinal[0];
                        dstPixels[dIndex++] = destNoDataFinal[1];
                        dstPixels[dIndex++] = destNoDataFinal[2];
                        continue;
                    }

                    double R, G, B;

                    R = G = B = I;
                    if (S != 0.0) {
                        if (H >= PI23 && H <= PI43) {
                            R = (1 - S) * I;
                            double c1 = 3 * I - R;
                            double c2 = SQRT3 * (R - I) * Math.tan(H);
                            G = (c1 + c2) / 2;
                            B = (c1 - c2) / 2;
                        } else if (H > PI43) {
                            G = (1 - S) * I;
                            double c1 = 3 * I - G;
                            double c2 = SQRT3 * (G - I) * Math.tan(H - PI23);
                            B = (c1 + c2) / 2;
                            R = (c1 - c2) / 2;
                        } else if (H < PI23) {
                            B = (1 - S) * I;
                            double c1 = 3 * I - B;
                            double c2 = SQRT3 * (B - I) * Math.tan(H - PI43);
                            R = (c1 + c2) / 2;
                            G = (c1 - c2) / 2;
                        }
                    }

                    dstPixels[dIndex++] = ((R < 0) ? 0 : ((R > 1.0) ? 1.0 : R)) * normr;
                    dstPixels[dIndex++] = ((G < 0) ? 0 : ((G > 1.0) ? 1.0 : G)) * normg;
                    dstPixels[dIndex++] = ((B < 0) ? 0 : ((B > 1.0) ? 1.0 : B)) * normb;
                }
            }
        }

        convertToSigned(dstPixels, dstType);
        dest.setPixels(dest.getMinX(), dest.getMinY(), width, height, dstPixels);
    }

    private double[] getConvertedNodataBfromRGB(boolean isByte, float[] destNodata, int normr,
            int normg, int normb, int bnormi, int bnormh, int bnorms, double normi, double normh,
            double norms) {
        short R = (short) (((int) destNodata[0] & 0xFF) << normr);
        short G = (short) (((int) destNodata[1] & 0xFF) << normg);
        short B = (short) (((int) destNodata[2] & 0xFF) << normb);

        double[] result = new double[3];

        if (isByte) {
            float intensity = (R + G + B) / 3.0f;
            result[0] = ((short) (intensity + 0.5f)) >> bnormi;

            short drg = (short) (R - G);
            short drb = (short) (R - B);

            int tint = drg * drg + drb * (drb - drg);

            short sum = (short) (drg + drb);
            double temp;
            if (tint != 0)
                temp = sqrtTable[(int) (250.0 * sum * sum / tint + 0.5)];
            else
                temp = -1.0;

            int hue;
            if (sum > 0)
                hue = acosTable[(int) (500 * temp + 0.5) + 500];
            else
                hue = acosTable[(int) (-500 * temp - 0.5) + 500];

            if (B >= G)
                result[1] = (255 - hue) >> bnormh;
            else
                result[1] = hue >> bnormh;

            short min = (G > B) ? B : G;
            min = (R > min) ? min : R;
            result[2] = (255 - (int) (255 * min / intensity + 0.5f)) >> bnorms;
        } else {
            float intensity = (R + G + B) / 3.0f;
            result[0] = normi * intensity;

            double drg = R - G;
            double drb = R - B;
            double temp = Math.sqrt(drg * drg + drb * (drb - drg));

            if (temp != 0) {
                temp = Math.acos((drg + drb) / temp / 2);
                if (B >= G)
                    temp = PI2 - temp;
            } else
                temp = PI2;

            result[1] = normh * temp;

            double min = (G > B) ? B : G;
            min = (R > min) ? min : R;
            result[2] = norms * (1 - min / intensity);
        }
        return result;
    }

    private double[] getConvertedNodataIfromRGB(boolean isByte, float[] destNodata, int normr,
            int normg, int normb, int bnormi, int bnormh, int bnorms, double normi, double normh,
            double norms, boolean isInteger) {

        long and = isInteger ? 0xFFFFFFFFl : 0xFFFF;

        long R = ((int) destNodata[0] & and) << normr;
        long G = ((int) destNodata[1] & and) << normg;
        long B = ((int) destNodata[2] & and) << normb;

        double[] result = new double[3];

        if (isByte) {
            float intensity = (R + G + B) / 3.0f;
            result[0] = ((long) (intensity + 0.5f)) >> bnormi;

            long drg = R - G;
            long drb = R - B;

            double tint = drg * (double) drg + drb * (double) (drb - drg);

            double sum = drg + drb;
            double temp;
            if (tint != 0)
                temp = sqrtTable[(int) (250.0 * sum * sum / tint + 0.5)];
            else
                temp = -1.0;

            int hue;
            if (sum > 0)
                hue = acosTable[(int) (500 * temp + 0.5) + 500];
            else
                hue = acosTable[(int) (-500 * temp - 0.5) + 500];

            if (B >= G)
                result[1] = (255 - hue) >> bnormh;
            else
                result[1] = hue >> bnormh;

            long min = (G > B) ? B : G;
            min = (R > min) ? min : R;
            result[2] = (255 - (int) (255 * min / intensity + 0.5f)) >> bnorms;

        } else {
            float intensity = (R + G + B) / 3.0f;
            result[0] = normi * intensity;

            double drg = R - G;
            double drb = R - B;
            double temp = Math.sqrt(drg * drg + drb * (drb - drg));

            if (temp != 0) {
                temp = Math.acos((drg + drb) / temp / 2);
                if (B >= G)
                    temp = PI2 - temp;
            } else
                temp = PI2;

            result[1] = normh * temp;

            double min = (G > B) ? B : G;
            min = (R > min) ? min : R;
            result[2] = norms * (1 - min / intensity);
        }

        return result;
    }

    private double[] getConvertedNodataDfromRGB(boolean isByte, float[] destNodata, int bnormi,
            int bnormh, int bnorms, double normi, double normh, double norms) {

        double R = destNodata[0];
        double G = destNodata[1];
        double B = destNodata[2];

        double[] result = new double[3];

        if (isByte) {
            double intensity = (R + G + B) / 3.0f;
            result[0] = (int) (intensity * bnormi + 0.5);

            double drg = R - G;
            double drb = R - B;

            double tint = drg * drg + drb * (drb - drg);

            double sum = drg + drb;
            double temp;
            if (tint != 0.0)
                temp = sqrtTable[(int) (250.0 * sum * sum / tint + 0.5)];
            else
                temp = -1.0;

            int hue;
            if (sum > 0)
                hue = acosTable[(int) (500 * temp + 0.5) + 500];
            else
                hue = acosTable[(int) (-500 * temp - 0.5) + 500];

            if (B >= G)
                result[1] = (255 - hue) >> bnormh;
            else
                result[1] = hue >> bnormh;

            double min = (G > B) ? B : G;
            min = (R > min) ? min : R;
            result[2] = (255 - (int) (255 * min / intensity + 0.5f)) >> bnorms;
        } else {
            double intensity = (R + G + B) / 3.0f;
            result[0] = normi * intensity;

            double drg = R - G;
            double drb = R - B;
            double temp = Math.sqrt(drg * drg + drb * (drb - drg));

            if (temp != 0) {
                temp = Math.acos((drg + drb) / temp / 2);
                if (B >= G)
                    temp = PI2 - temp;
            } else
                temp = PI2;

            result[1] = normh * temp;

            double min = (G > B) ? B : G;
            min = (R > min) ? min : R;
            result[2] = norms * (1 - min / intensity);
        }

        return result;
    }

    private double[] getConvertedNodataBToRGB(boolean isByte, float[] destNodata, double normi,
            double norms, double normr, double normg, double normb, double normh) {

        double[] result = new double[3];

        double I = ((int) destNodata[0] & 0xFF) * normi;
        int h = (int) destNodata[1] & 0xFF;
        double S = ((int) destNodata[2] & 0xFF) * norms;

        if (isByte) {
            float r, g, b;

            r = g = b = (float) I;

            if (S != 0.0) {
                if (h >= 85 && h <= 170) {
                    r = (float) ((1 - S) * I);
                    float c1 = (float) (3 * I - r);
                    float c2 = (float) (SQRT3 * (r - I) * tanTable[h]);
                    g = (float) ((c1 + c2) / 2);
                    b = (float) ((c1 - c2) / 2);
                } else if (h > 170) {
                    g = (float) ((1 - S) * I);
                    float c1 = (float) (3 * I - g);
                    float c2 = (float) (SQRT3 * (g - I) * tanTable[h - 85]);
                    b = (c1 + c2) / 2;
                    r = (c1 - c2) / 2;
                } else if (h < 85) {
                    b = (float) ((1 - S) * I);
                    float c1 = (float) (3 * I - b);
                    float c2 = (float) (SQRT3 * (b - I) * tanTable[h + 85]);
                    r = (c1 + c2) / 2;
                    g = (c1 - c2) / 2;
                }
            }
            result[0] = (int) (((r < 0.0f) ? 0.0f : ((r > 1.0f) ? 1.0f : r)) * normr + 0.5);
            result[1] = (int) (((g < 0.0f) ? 0.0f : ((g > 1.0f) ? 1.0f : g)) * normg + 0.5);
            result[2] = (int) (((b < 0.0f) ? 0.0f : ((b > 1.0f) ? 1.0f : b)) * normb + 0.5);
        } else {
            double R, G, B;

            R = G = B = I;
            if (S != 0) {
                double H = h * normh;
                if (H >= PI23 && H <= PI43) {
                    R = (1 - S) * I;
                    double c1 = 3 * I - R;
                    double c2 = SQRT3 * (R - I) * Math.tan(H);
                    G = (c1 + c2) / 2;
                    B = (c1 - c2) / 2;
                } else if (H > PI43) {
                    G = (1 - S) * I;
                    double c1 = 3 * I - G;
                    double c2 = SQRT3 * (G - I) * Math.tan(H - PI23);
                    B = (c1 + c2) / 2;
                    R = (c1 - c2) / 2;
                } else if (H < PI23) {
                    B = (1 - S) * I;
                    double c1 = 3 * I - B;
                    double c2 = SQRT3 * (B - I) * Math.tan(H - PI43);
                    R = (c1 + c2) / 2;
                    G = (c1 - c2) / 2;
                }
            }
            result[0] = ((R < 0) ? 0 : ((R > 1.0) ? 1.0 : R)) * normr;
            result[1] = ((G < 0) ? 0 : ((G > 1.0) ? 1.0 : G)) * normg;
            result[2] = ((B < 0) ? 0 : ((B > 1.0) ? 1.0 : B)) * normb;
        }

        return result;
    }

    private double[] getConvertedNodataToRGB(float[] destNodata, int dataType, double normi,
            double normh, double norms, double normr, double normg, double normb) {

        double[] result = new double[3];

        double I, H, S;

        switch (dataType) {
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT:
            I = ((int) destNodata[0] & 0xFFFF) * normi;
            H = ((int) destNodata[1] & 0xFFFF) * normh;
            S = ((int) destNodata[2] & 0xFFFF) * norms;
            break;
        case DataBuffer.TYPE_INT:
            I = ((int) destNodata[0] & 0xFFFFFFFFl) * normi;
            H = ((int) destNodata[1] & 0xFFFFFFFFl) * normh;
            S = ((int) destNodata[2] & 0xFFFFFFFFl) * norms;
            break;
        case DataBuffer.TYPE_FLOAT:
        case DataBuffer.TYPE_DOUBLE:
            I = destNodata[0];
            H = destNodata[1];
            S = destNodata[2];
            break;
        default:
            throw new IllegalArgumentException("Wrong data type defined");
        }

        double R, G, B;

        R = G = B = I;
        if (S != 0.0) {
            if (H >= PI23 && H <= PI43) {
                R = (1 - S) * I;
                double c1 = 3 * I - R;
                double c2 = SQRT3 * (R - I) * Math.tan(H);
                G = (c1 + c2) / 2;
                B = (c1 - c2) / 2;
            } else if (H > PI43) {
                G = (1 - S) * I;
                double c1 = 3 * I - G;
                double c2 = SQRT3 * (G - I) * Math.tan(H - PI23);
                B = (c1 + c2) / 2;
                R = (c1 - c2) / 2;
            } else if (H < PI23) {
                B = (1 - S) * I;
                double c1 = 3 * I - B;
                double c2 = SQRT3 * (B - I) * Math.tan(H - PI43);
                R = (c1 + c2) / 2;
                G = (c1 - c2) / 2;
            }
        }

        result[0] = ((R < 0) ? 0 : ((R > 1.0) ? 1.0 : R)) * normr;
        result[1] = ((G < 0) ? 0 : ((G > 1.0) ? 1.0 : G)) * normg;
        result[2] = ((B < 0) ? 0 : ((B > 1.0) ? 1.0 : B)) * normb;

        return result;
    }
}
