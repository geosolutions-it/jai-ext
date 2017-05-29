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
package it.geosolutions.jaiext.warp;

import it.geosolutions.jaiext.interpolators.InterpolationBilinear;
import it.geosolutions.jaiext.range.Range;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.Map;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.RasterAccessor;
import javax.media.jai.Warp;
import javax.media.jai.iterator.RandomIter;

/**
 * An <code>OpImage</code> implementing the general "Warp" operation as described in <code>javax.media.jai.operator.WarpDescriptor</code>. It supports
 * the bilinear interpolation.
 * 
 * <p>
 * The layout for the destination image may be specified via the <code>ImageLayout</code> parameter. However, only those settings suitable for this
 * operation will be used. The unsuitable settings will be replaced by default suitable values. An optional ROI object and a NoData Range can be used.
 * If a backward mapped pixel lies outside ROI or it is a NoData, then the destination pixel value is a background value.
 * 
 * If the input image contains an IndexColorModel, then pixel values are taken directly from the input color table.
 * 
 * 
 * @since EA2
 * @see javax.media.jai.Warp
 * @see javax.media.jai.WarpOpImage
 * @see javax.media.jai.operator.WarpDescriptor
 * @see WarpRIF
 * 
 */
@SuppressWarnings("unchecked")
final class WarpBilinearOpImage extends WarpOpImage {

    private final static double FRACTION_THRESHOLD_D = 0.5d;

    /** Color table representing source's IndexColorModel. */
    private byte[][] ctable = null;

    /** LookupTable used for a faster NoData check */
    private boolean[] booleanLookupTable;

    /**
     * Constructs a WarpBilinearOpImage.
     * 
     * @param source The source image.
     * @param extender A BorderExtender, or null.
     * @param config RenderingHints used in calculations.
     * @param layout The destination image layout.
     * @param warp An object defining the warp algorithm.
     * @param interp An object describing the interpolation method.
     * @param roi input ROI object used.
     * @param noData NoData Range object used for checking if NoData are present.
     */
    public WarpBilinearOpImage(final RenderedImage source, final BorderExtender extender,
            final Map<?, ?> config, final ImageLayout layout, final Warp warp,
            final Interpolation interp, final ROI sourceROI, Range noData, double[] bkg) {
        super(source, layout, config, false, extender, interp, warp, bkg, sourceROI, noData);

        /*
         * If the source has IndexColorModel, get the RGB color table. Note, in this case, the source should have an integral data type. And dest
         * always has data type byte.
         */
        final ColorModel srcColorModel = source.getColorModel();
        if (srcColorModel instanceof IndexColorModel) {
            final IndexColorModel icm = (IndexColorModel) srcColorModel;
            ctable = new byte[3][icm.getMapSize()];
            icm.getReds(ctable[0]);
            icm.getGreens(ctable[1]);
            icm.getBlues(ctable[2]);
        }

        /*
         * Selection of a destinationNoData value for each datatype
         */
        //backgroundValues[b] = backgroundValues[0];
        SampleModel sm = source.getSampleModel();
        // Source image data Type
        int srcDataType = sm.getDataType();

        // Creation of a lookuptable containing the values to use for no data
        if (srcDataType == DataBuffer.TYPE_BYTE && hasNoData) {
            booleanLookupTable = new boolean[256];
            for (int i = 0; i < booleanLookupTable.length; i++) {
                byte value = (byte) i;
                booleanLookupTable[i] = noDataRange.contains(value);
            }
        }
    }

    protected void computeRectByte(final PlanarImage src, final RasterAccessor dst,
            final RandomIter roiIter, boolean roiContainsTile) {
        // Random iterator initialization. If an extender is used, then an extended image is taken.
        RandomIter iterSource = getRandomIterator(src, extender);

        final int minX = src.getMinX();
        final int maxX = src.getMaxX() - (extended ? 0 : 1); // Right padding
        final int minY = src.getMinY();
        final int maxY = src.getMaxY() - (extended ? 0 : 1); // Bottom padding

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int lineStride = dst.getScanlineStride();
        final int pixelStride = dst.getPixelStride();
        final int[] bandOffsets = dst.getBandOffsets();
        final byte[][] data = dst.getByteDataArrays();

        final float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

        if (ctable == null) { // source does not have IndexColorModel
            // ONLY VALID DATA
            if (caseA || (caseB && roiContainsTile)) {
                for (int h = 0; h < dstHeight; h++) {
                    int pixelOffset = lineOffset;
                    lineOffset += lineStride;

                    warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                    int count = 0;
                    for (int w = 0; w < dstWidth; w++) {
                        final float sx = warpData[count++];
                        final float sy = warpData[count++];

                        final int xint = floor(sx);
                        final int yint = floor(sy);
                        final float xfrac = sx - xint;
                        final float yfrac = sy - yint;

                        if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                            /* Fill with a background color. */
                            if (setBackground) {
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = (byte)backgroundValues[b];
                                }
                            }
                        } else {
                            //
                            // NO ROI
                            //
                            for (int b = 0; b < dstBands; b++) {
                                int s00 = iterSource.getSample(xint, yint, b) & 0xFF;
                                int s01 = iterSource.getSample(xint + 1, yint, b) & 0xFF;
                                int s10 = iterSource.getSample(xint, yint + 1, b) & 0xFF;
                                int s11 = iterSource.getSample(xint + 1, yint + 1, b) & 0xFF;

                                float s0 = (s01 - s00) * xfrac + s00;
                                float s1 = (s11 - s10) * xfrac + s10;
                                float s = (s1 - s0) * yfrac + s0;

                                data[b][pixelOffset + bandOffsets[b]] = (byte) s;
                            }
                        }
                        // next desination pixel
                        pixelOffset += pixelStride;
                    } // COLS LOOP
                }
                // ONLY ROI
            } else if (caseB) {
                for (int h = 0; h < dstHeight; h++) {
                    int pixelOffset = lineOffset;
                    lineOffset += lineStride;

                    warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                    int count = 0;
                    for (int w = 0; w < dstWidth; w++) {
                        final float sx = warpData[count++];
                        final float sy = warpData[count++];

                        final int xint = floor(sx);
                        final int yint = floor(sy);
                        final float xfrac = sx - xint;
                        final float yfrac = sy - yint;

                        if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                            /* Fill with a background color. */
                            if (setBackground) {
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = (byte)backgroundValues[b];
                                }
                            }
                        } else {
                            //
                            // ROI
                            //
                            // checks with roi
                            final boolean w00 = !(roiBounds.contains(xint, yint) && roiIter.getSample(xint, yint, 0) > 0);
                            final boolean w01 = !(roiBounds.contains(xint + 1, yint) && roiIter.getSample(xint + 1, yint, 0) > 0);
                            final boolean w10 = !(roiBounds.contains(xint, yint + 1) && roiIter.getSample(xint, yint + 1, 0) > 0);
                            final boolean w11 = !(roiBounds.contains(xint + 1, yint + 1) && roiIter.getSample(xint + 1, yint + 1, 0) > 0);
                            if (w00 && w01 && w10 && w11) { // SG should not happen
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = (byte)backgroundValues[b];
                                }
                            } else {

                                for (int b = 0; b < dstBands; b++) {
                                    int s00 = iterSource.getSample(xint, yint, b) & 0xFF;
                                    int s01 = iterSource.getSample(xint + 1, yint, b) & 0xFF;
                                    int s10 = iterSource.getSample(xint, yint + 1, b) & 0xFF;
                                    int s11 = iterSource.getSample(xint + 1, yint + 1, b) & 0xFF;

                                    float s0 = (s01 - s00) * xfrac + s00;
                                    float s1 = (s11 - s10) * xfrac + s10;
                                    float s = (s1 - s0) * yfrac + s0;

                                    data[b][pixelOffset + bandOffsets[b]] = (byte) s;
                                }
                            }
                        }

                        // next desination pixel
                        pixelOffset += pixelStride;
                    } // COLS LOOP
                } // ROWS LOOP
                  // ONLY NODATA
            } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
                for (int h = 0; h < dstHeight; h++) {
                    int pixelOffset = lineOffset;
                    lineOffset += lineStride;

                    warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                    int count = 0;
                    for (int w = 0; w < dstWidth; w++) {
                        final float sx = warpData[count++];
                        final float sy = warpData[count++];

                        final int xint = floor(sx);
                        final int yint = floor(sy);
                        final float xfrac = sx - xint;
                        final float yfrac = sy - yint;

                        if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                            /* Fill with a background color. */
                            if (setBackground) {
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = (byte)backgroundValues[b];
                                }
                            }
                        } else {
                            //
                            // NODATA
                            //
                            // checks with nodata

                            for (int b = 0; b < dstBands; b++) {

                                int s00 = iterSource.getSample(xint, yint, b) & 0xFF;
                                int s01 = iterSource.getSample(xint + 1, yint, b) & 0xFF;
                                int s10 = iterSource.getSample(xint, yint + 1, b) & 0xFF;
                                int s11 = iterSource.getSample(xint + 1, yint + 1, b) & 0xFF;

                                final boolean w00 = booleanLookupTable[s00];
                                final boolean w01 = booleanLookupTable[s01];
                                final boolean w10 = booleanLookupTable[s10];
                                final boolean w11 = booleanLookupTable[s11];

                                if (w00 && w01 && w10 && w11) {
                                    data[b][pixelOffset + bandOffsets[b]] = (byte)backgroundValues[b];
                                } else {
                                    data[b][pixelOffset + bandOffsets[b]] = (byte) (InterpolationBilinear.computeValueDouble(
                                            s00, s01, s10, s11, w00, w01, w10, w11, xfrac, yfrac, 
                                            DataBuffer.TYPE_INT, backgroundValues[b]).intValue() & 0xFF);
                                }
                            }
                        }

                        // next desination pixel
                        pixelOffset += pixelStride;
                    } // COLS LOOP
                } // ROWS LOOP
                  // BOTH ROI AND NODATA
            } else {
                for (int h = 0; h < dstHeight; h++) {
                    int pixelOffset = lineOffset;
                    lineOffset += lineStride;

                    warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                    int count = 0;
                    for (int w = 0; w < dstWidth; w++) {
                        final float sx = warpData[count++];
                        final float sy = warpData[count++];

                        final int xint = floor(sx);
                        final int yint = floor(sy);
                        final float xfrac = sx - xint;
                        final float yfrac = sy - yint;

                        if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                            /* Fill with a background color. */
                            if (setBackground) {
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = (byte)backgroundValues[b];
                                }
                            }
                        } else {
                            //
                            // ROI
                            //
                            // checks with roi
                            final boolean w00Roi = !(roiBounds.contains(xint, yint) && roiIter.getSample(xint, yint, 0) > 0);
                            final boolean w01Roi = !(roiBounds.contains(xint + 1, yint) && roiIter.getSample(xint + 1, yint, 0) > 0);
                            final boolean w10Roi = !(roiBounds.contains(xint, yint + 1) && roiIter.getSample(xint, yint + 1, 0) > 0);
                            final boolean w11Roi = !(roiBounds.contains(xint + 1, yint + 1) && roiIter.getSample(xint + 1, yint + 1, 0) > 0);
                            if (w00Roi && w01Roi && w10Roi && w11Roi) { // SG should not happen
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = (byte)backgroundValues[b];
                                }
                            } else {
                                for (int b = 0; b < dstBands; b++) {
                                    //
                                    // NODATA
                                    //
                                    // checks with nodata
                                    int s00 = iterSource.getSample(xint, yint, b) & 0xFF;
                                    int s01 = iterSource.getSample(xint + 1, yint, b) & 0xFF;
                                    int s10 = iterSource.getSample(xint, yint + 1, b) & 0xFF;
                                    int s11 = iterSource.getSample(xint + 1, yint + 1, b) & 0xFF;

                                    final boolean w00 = booleanLookupTable[s00];
                                    final boolean w01 = booleanLookupTable[s01];
                                    final boolean w10 = booleanLookupTable[s10];
                                    final boolean w11 = booleanLookupTable[s11];

                                    if (w00 && w01 && w10 && w11) {
                                        data[b][pixelOffset + bandOffsets[b]] = (byte)backgroundValues[b];
                                    } else {
                                        data[b][pixelOffset + bandOffsets[b]] = (byte) (InterpolationBilinear.computeValueDouble(
                                                s00, s01, s10, s11, w00, w01, w10, w11, xfrac, yfrac, 
                                                DataBuffer.TYPE_INT, backgroundValues[b]).intValue() & 0xFF);
                                    }
                                }
                            }
                        }
                        // next desination pixel
                        pixelOffset += pixelStride;
                    } // COLS LOOP
                } // ROWS LOOP
            }
        } else {// source has IndexColorModel
                // ONLY VALID DATA
            if (caseA || (caseB && roiContainsTile)) {
                for (int h = 0; h < dstHeight; h++) {
                    int pixelOffset = lineOffset;
                    lineOffset += lineStride;

                    warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                    int count = 0;
                    for (int w = 0; w < dstWidth; w++) {
                        final float sx = warpData[count++];
                        final float sy = warpData[count++];

                        final int xint = floor(sx);
                        final int yint = floor(sy);
                        final float xfrac = sx - xint;
                        final float yfrac = sy - yint;

                        if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                            /* Fill with a background color. */
                            if (setBackground) {
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = (byte)backgroundValues[b];
                                }
                            }
                        } else {
                            //
                            // NO ROI
                            //
                            for (int b = 0; b < dstBands; b++) {
                                final byte[] t = ctable[b];

                                int s00 = t[iterSource.getSample(xint, yint, 0) & 0xFF] & 0xFF;
                                int s01 = t[iterSource.getSample(xint + 1, yint, 0) & 0xFF] & 0xFF;
                                int s10 = t[iterSource.getSample(xint, yint + 1, 0) & 0xFF] & 0xFF;
                                int s11 = t[iterSource.getSample(xint + 1, yint + 1, 0) & 0xFF] & 0xFF;

                                float s0 = (s01 - s00) * xfrac + s00;
                                float s1 = (s11 - s10) * xfrac + s10;
                                float s = (s1 - s0) * yfrac + s0;

                                data[b][pixelOffset + bandOffsets[b]] = (byte) s;
                            }
                        }
                        // next desination pixel
                        pixelOffset += pixelStride;
                    } // COLS LOOP
                }
                // ONLY ROI
            } else if (caseB) {
                for (int h = 0; h < dstHeight; h++) {
                    int pixelOffset = lineOffset;
                    lineOffset += lineStride;

                    warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                    int count = 0;
                    for (int w = 0; w < dstWidth; w++) {
                        final float sx = warpData[count++];
                        final float sy = warpData[count++];

                        final int xint = floor(sx);
                        final int yint = floor(sy);
                        final float xfrac = sx - xint;
                        final float yfrac = sy - yint;

                        if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                            /* Fill with a background color. */
                            if (setBackground) {
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = (byte)backgroundValues[b];
                                }
                            }
                        } else {
                            //
                            // ROI
                            //
                            // checks with roi
                            final boolean w00 = !(roiBounds.contains(xint, yint) && roiIter.getSample(xint, yint, 0) > 0);
                            final boolean w01 = !(roiBounds.contains(xint + 1, yint) && roiIter.getSample(xint + 1, yint, 0) > 0);
                            final boolean w10 = !(roiBounds.contains(xint, yint + 1) && roiIter.getSample(xint, yint + 1, 0) > 0);
                            final boolean w11 = !(roiBounds.contains(xint + 1, yint + 1) && roiIter.getSample(xint + 1, yint + 1, 0) > 0);
                            if (w00 && w01 && w10 && w11) { // SG should not happen
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = (byte)backgroundValues[b];
                                }
                            } else {

                                for (int b = 0; b < dstBands; b++) {
                                    final byte[] t = ctable[b];

                                    int s00 = t[iterSource.getSample(xint, yint, 0) & 0xFF] & 0xFF;
                                    int s01 = t[iterSource.getSample(xint + 1, yint, 0) & 0xFF] & 0xFF;
                                    int s10 = t[iterSource.getSample(xint, yint + 1, 0) & 0xFF] & 0xFF;
                                    int s11 = t[iterSource.getSample(xint + 1, yint + 1, 0) & 0xFF] & 0xFF;

                                    float s0 = (s01 - s00) * xfrac + s00;
                                    float s1 = (s11 - s10) * xfrac + s10;
                                    float s = (s1 - s0) * yfrac + s0;

                                    data[b][pixelOffset + bandOffsets[b]] = (byte) s;
                                }
                            }
                        }

                        // next desination pixel
                        pixelOffset += pixelStride;
                    } // COLS LOOP
                } // ROWS LOOP
                  // ONLY NODATA
            } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
                for (int h = 0; h < dstHeight; h++) {
                    int pixelOffset = lineOffset;
                    lineOffset += lineStride;

                    warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                    int count = 0;
                    for (int w = 0; w < dstWidth; w++) {
                        final float sx = warpData[count++];
                        final float sy = warpData[count++];

                        final int xint = floor(sx);
                        final int yint = floor(sy);
                        final float xfrac = sx - xint;
                        final float yfrac = sy - yint;

                        if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                            /* Fill with a background color. */
                            if (setBackground) {
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = (byte)backgroundValues[b];
                                }
                            }
                        } else {
                            //
                            // NODATA
                            //
                            // checks with nodata

                            for (int b = 0; b < dstBands; b++) {

                                final byte[] t = ctable[b];

                                int s00 = t[iterSource.getSample(xint, yint, 0) & 0xFF] & 0xFF;
                                int s01 = t[iterSource.getSample(xint + 1, yint, 0) & 0xFF] & 0xFF;
                                int s10 = t[iterSource.getSample(xint, yint + 1, 0) & 0xFF] & 0xFF;
                                int s11 = t[iterSource.getSample(xint + 1, yint + 1, 0) & 0xFF] & 0xFF;

                                final boolean w00 = booleanLookupTable[s00];
                                final boolean w01 = booleanLookupTable[s01];
                                final boolean w10 = booleanLookupTable[s10];
                                final boolean w11 = booleanLookupTable[s11];

                                if (w00 && w01 && w10 && w11) {
                                    data[b][pixelOffset + bandOffsets[b]] = (byte)backgroundValues[b];
                                } else {
                                    data[b][pixelOffset + bandOffsets[b]] = (byte) (InterpolationBilinear.computeValueDouble(
                                            s00, s01, s10, s11, w00, w01, w10, w11, xfrac, yfrac, 
                                            DataBuffer.TYPE_INT, backgroundValues[b]).intValue() & 0xFF);
                                }
                            }
                        }

                        // next desination pixel
                        pixelOffset += pixelStride;
                    } // COLS LOOP
                } // ROWS LOOP
                  // BOTH ROI AND NODATA
            } else {
                for (int h = 0; h < dstHeight; h++) {
                    int pixelOffset = lineOffset;
                    lineOffset += lineStride;

                    warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                    int count = 0;
                    for (int w = 0; w < dstWidth; w++) {
                        final float sx = warpData[count++];
                        final float sy = warpData[count++];

                        final int xint = floor(sx);
                        final int yint = floor(sy);
                        final float xfrac = sx - xint;
                        final float yfrac = sy - yint;

                        if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                            /* Fill with a background color. */
                            if (setBackground) {
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = (byte)backgroundValues[b];
                                }
                            }
                        } else {
                            //
                            // ROI
                            //
                            // checks with roi
                            final boolean w00Roi = !(roiBounds.contains(xint, yint) && roiIter.getSample(xint, yint, 0) > 0);
                            final boolean w01Roi = !(roiBounds.contains(xint + 1, yint) && roiIter.getSample(xint + 1, yint, 0) > 0);
                            final boolean w10Roi = !(roiBounds.contains(xint, yint + 1) && roiIter.getSample(xint, yint + 1, 0) > 0);
                            final boolean w11Roi = !(roiBounds.contains(xint + 1, yint + 1) && roiIter.getSample(xint + 1, yint + 1, 0) > 0);
                            if (w00Roi && w01Roi && w10Roi && w11Roi) { // SG should not happen
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = (byte)backgroundValues[b];
                                }
                            } else {
                                for (int b = 0; b < dstBands; b++) {
                                    //
                                    // NODATA
                                    //
                                    // checks with nodata
                                    final byte[] t = ctable[b];

                                    int s00 = t[iterSource.getSample(xint, yint, 0) & 0xFF] & 0xFF;
                                    int s01 = t[iterSource.getSample(xint + 1, yint, 0) & 0xFF] & 0xFF;
                                    int s10 = t[iterSource.getSample(xint, yint + 1, 0) & 0xFF] & 0xFF;
                                    int s11 = t[iterSource.getSample(xint + 1, yint + 1, 0) & 0xFF] & 0xFF;

                                    final boolean w00 = booleanLookupTable[s00];
                                    final boolean w01 = booleanLookupTable[s01];
                                    final boolean w10 = booleanLookupTable[s10];
                                    final boolean w11 = booleanLookupTable[s11];

                                    if (w00 && w01 && w10 && w11) {
                                        data[b][pixelOffset + bandOffsets[b]] = (byte)backgroundValues[b];
                                    } else {
                                        data[b][pixelOffset + bandOffsets[b]] = (byte) (InterpolationBilinear.computeValueDouble(
                                                s00, s01, s10, s11, w00, w01, w10, w11, xfrac, yfrac, 
                                                DataBuffer.TYPE_INT, backgroundValues[b]).intValue() & 0xFF);
                                    }
                                }
                            }
                        }
                        // next desination pixel
                        pixelOffset += pixelStride;
                    } // COLS LOOP
                } // ROWS LOOP
            }
        }

        iterSource.done();
    }

    protected void computeRectUShort(final PlanarImage src, final RasterAccessor dst,
            final RandomIter roiIter, boolean roiContainsTile) {
        // Random iterator initialization. If an extender is used, then an extended image is taken.
        RandomIter iterSource = getRandomIterator(src, extender);

        final int minX = src.getMinX();
        final int maxX = src.getMaxX() - (extended ? 0 : 1); // Right padding
        final int minY = src.getMinY();
        final int maxY = src.getMaxY() - (extended ? 0 : 1); // Bottom padding

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int lineStride = dst.getScanlineStride();
        final int pixelStride = dst.getPixelStride();
        final int[] bandOffsets = dst.getBandOffsets();
        final short[][] data = dst.getShortDataArrays();

        final float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

        // ONLY VALID DATA
        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    final float sx = warpData[count++];
                    final float sy = warpData[count++];

                    final int xint = floor(sx);
                    final int yint = floor(sy);
                    final float xfrac = sx - xint;
                    final float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = (short)backgroundValues[b];
                            }
                        }
                    } else {
                        //
                        // NO ROI
                        //
                        for (int b = 0; b < dstBands; b++) {
                            int s00 = iterSource.getSample(xint, yint, b) & 0xFFFF;
                            int s01 = iterSource.getSample(xint + 1, yint, b) & 0xFFFF;
                            int s10 = iterSource.getSample(xint, yint + 1, b) & 0xFFFF;
                            int s11 = iterSource.getSample(xint + 1, yint + 1, b) & 0xFFFF;

                            float s0 = (s01 - s00) * xfrac + s00;
                            float s1 = (s11 - s10) * xfrac + s10;
                            float s = (s1 - s0) * yfrac + s0;

                            data[b][pixelOffset + bandOffsets[b]] = (short) s;
                        }
                    }
                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            }
            // ONLY ROI
        } else if (caseB) {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    final float sx = warpData[count++];
                    final float sy = warpData[count++];

                    final int xint = floor(sx);
                    final int yint = floor(sy);
                    final float xfrac = sx - xint;
                    final float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = (short)backgroundValues[b];
                            }
                        }
                    } else {
                        //
                        // ROI
                        //
                        // checks with roi
                        final boolean w00 = !(roiBounds.contains(xint, yint) && roiIter.getSample(xint, yint, 0) > 0);
                        final boolean w01 = !(roiBounds.contains(xint + 1, yint) && roiIter.getSample(xint + 1, yint, 0) > 0);
                        final boolean w10 = !(roiBounds.contains(xint, yint + 1) && roiIter.getSample(xint, yint + 1, 0) > 0);
                        final boolean w11 = !(roiBounds.contains(xint + 1, yint + 1) && roiIter.getSample(xint + 1, yint + 1, 0) > 0);
                        if (w00 && w01 && w10 && w11) { // SG should not happen
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = (short)backgroundValues[b];
                            }
                        } else {

                            for (int b = 0; b < dstBands; b++) {
                                int s00 = iterSource.getSample(xint, yint, b) & 0xFFFF;
                                int s01 = iterSource.getSample(xint + 1, yint, b) & 0xFFFF;
                                int s10 = iterSource.getSample(xint, yint + 1, b) & 0xFFFF;
                                int s11 = iterSource.getSample(xint + 1, yint + 1, b) & 0xFFFF;

                                float s0 = (s01 - s00) * xfrac + s00;
                                float s1 = (s11 - s10) * xfrac + s10;
                                float s = (s1 - s0) * yfrac + s0;

                                data[b][pixelOffset + bandOffsets[b]] = (short) s;
                            }
                        }
                    }

                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            } // ROWS LOOP
              // ONLY NODATA
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    final float sx = warpData[count++];
                    final float sy = warpData[count++];

                    final int xint = floor(sx);
                    final int yint = floor(sy);
                    final float xfrac = sx - xint;
                    final float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = (short)backgroundValues[b];
                            }
                        }
                    } else {
                        //
                        // NODATA
                        //
                        // checks with nodata

                        for (int b = 0; b < dstBands; b++) {

                            int s00 = iterSource.getSample(xint, yint, b) & 0xFFFF;
                            int s01 = iterSource.getSample(xint + 1, yint, b) & 0xFFFF;
                            int s10 = iterSource.getSample(xint, yint + 1, b) & 0xFFFF;
                            int s11 = iterSource.getSample(xint + 1, yint + 1, b) & 0xFFFF;

                            final boolean w00 = noDataRange.contains((short) s00);
                            final boolean w01 = noDataRange.contains((short) s01);
                            final boolean w10 = noDataRange.contains((short) s10);
                            final boolean w11 = noDataRange.contains((short) s11);

                            if (w00 && w01 && w10 && w11) {
                                data[b][pixelOffset + bandOffsets[b]] = (short)backgroundValues[b];
                            } else {
                                data[b][pixelOffset + bandOffsets[b]] = (short)(InterpolationBilinear.computeValueDouble(
                                        s00, s01, s10, s11, w00, w01, w10, w11, xfrac, yfrac, 
                                        DataBuffer.TYPE_USHORT, backgroundValues[b]).intValue() & 0xFFFF);
                            }
                        }
                    }

                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            } // ROWS LOOP
              // BOTH ROI AND NODATA
        } else {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    final float sx = warpData[count++];
                    final float sy = warpData[count++];

                    final int xint = floor(sx);
                    final int yint = floor(sy);
                    final float xfrac = sx - xint;
                    final float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = (short)backgroundValues[b];
                            }
                        }
                    } else {
                        //
                        // ROI
                        //
                        // checks with roi
                        final boolean w00Roi = !(roiBounds.contains(xint, yint) && roiIter.getSample(xint, yint, 0) > 0);
                        final boolean w01Roi = !(roiBounds.contains(xint + 1, yint) && roiIter.getSample(xint + 1, yint, 0) > 0);
                        final boolean w10Roi = !(roiBounds.contains(xint, yint + 1) && roiIter.getSample(xint, yint + 1, 0) > 0);
                        final boolean w11Roi = !(roiBounds.contains(xint + 1, yint + 1) && roiIter.getSample(xint + 1, yint + 1, 0) > 0);
                        if (w00Roi && w01Roi && w10Roi && w11Roi) { // SG should not happen
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = (short)backgroundValues[b];
                            }
                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                //
                                // NODATA
                                //
                                // checks with nodata
                                int s00 = iterSource.getSample(xint, yint, b) & 0xFFFF;
                                int s01 = iterSource.getSample(xint + 1, yint, b) & 0xFFFF;
                                int s10 = iterSource.getSample(xint, yint + 1, b) & 0xFFFF;
                                int s11 = iterSource.getSample(xint + 1, yint + 1, b) & 0xFFFF;

                                final boolean w00 = noDataRange.contains((short) s00);
                                final boolean w01 = noDataRange.contains((short) s01);
                                final boolean w10 = noDataRange.contains((short) s10);
                                final boolean w11 = noDataRange.contains((short) s11);

                                if (w00 && w01 && w10 && w11) {
                                    data[b][pixelOffset + bandOffsets[b]] = (short)backgroundValues[b];
                                } else {
                                    data[b][pixelOffset + bandOffsets[b]] = (short) (InterpolationBilinear.computeValueDouble(
                                                    s00, s01, s10, s11, w00, w01, w10, w11, xfrac, yfrac, 
                                                    DataBuffer.TYPE_USHORT, backgroundValues[b]).intValue() & 0xFFFF);
                                }
                            }
                        }
                    }
                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            } // ROWS LOOP
        }

        iterSource.done();
    }

    protected void computeRectShort(final PlanarImage src, final RasterAccessor dst,
            final RandomIter roiIter, boolean roiContainsTile) {
        // Random iterator initialization. If an extender is used, then an extended image is taken.
        RandomIter iterSource = getRandomIterator(src, extender);

        final int minX = src.getMinX();
        final int maxX = src.getMaxX() - (extended ? 0 : 1); // Right padding
        final int minY = src.getMinY();
        final int maxY = src.getMaxY() - (extended ? 0 : 1); // Bottom padding

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int lineStride = dst.getScanlineStride();
        final int pixelStride = dst.getPixelStride();
        final int[] bandOffsets = dst.getBandOffsets();
        final short[][] data = dst.getShortDataArrays();

        final float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

        // ONLY VALID DATA
        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    final float sx = warpData[count++];
                    final float sy = warpData[count++];

                    final int xint = floor(sx);
                    final int yint = floor(sy);
                    final float xfrac = sx - xint;
                    final float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = (short)backgroundValues[b];
                            }
                        }
                    } else {
                        //
                        // NO ROI
                        //
                        for (int b = 0; b < dstBands; b++) {
                            int s00 = iterSource.getSample(xint, yint, b);
                            int s01 = iterSource.getSample(xint + 1, yint, b);
                            int s10 = iterSource.getSample(xint, yint + 1, b);
                            int s11 = iterSource.getSample(xint + 1, yint + 1, b);

                            float s0 = (s01 - s00) * xfrac + s00;
                            float s1 = (s11 - s10) * xfrac + s10;
                            float s = (s1 - s0) * yfrac + s0;

                            data[b][pixelOffset + bandOffsets[b]] = (short) s;
                        }
                    }
                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            }
            // ONLY ROI
        } else if (caseB) {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    final float sx = warpData[count++];
                    final float sy = warpData[count++];

                    final int xint = floor(sx);
                    final int yint = floor(sy);
                    final float xfrac = sx - xint;
                    final float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = (short)backgroundValues[b];
                            }
                        }
                    } else {
                        //
                        // ROI
                        //
                        // checks with roi
                        final boolean w00 = !(roiBounds.contains(xint, yint) && roiIter.getSample(xint, yint, 0) > 0);
                        final boolean w01 = !(roiBounds.contains(xint + 1, yint) && roiIter.getSample(xint + 1, yint, 0) > 0);
                        final boolean w10 = !(roiBounds.contains(xint, yint + 1) && roiIter.getSample(xint, yint + 1, 0) > 0);
                        final boolean w11 = !(roiBounds.contains(xint + 1, yint + 1) && roiIter.getSample(xint + 1, yint + 1, 0) > 0);
                        if (w00 && w01 && w10 && w11) { // SG should not happen
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = (short)backgroundValues[b];
                            }
                        } else {

                            for (int b = 0; b < dstBands; b++) {
                                int s00 = iterSource.getSample(xint, yint, b);
                                int s01 = iterSource.getSample(xint + 1, yint, b);
                                int s10 = iterSource.getSample(xint, yint + 1, b);
                                int s11 = iterSource.getSample(xint + 1, yint + 1, b);

                                float s0 = (s01 - s00) * xfrac + s00;
                                float s1 = (s11 - s10) * xfrac + s10;
                                float s = (s1 - s0) * yfrac + s0;

                                data[b][pixelOffset + bandOffsets[b]] = (short) s;
                            }
                        }
                    }

                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            } // ROWS LOOP
              // ONLY NODATA
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    final float sx = warpData[count++];
                    final float sy = warpData[count++];

                    final int xint = floor(sx);
                    final int yint = floor(sy);
                    final float xfrac = sx - xint;
                    final float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = (short)backgroundValues[b];
                            }
                        }
                    } else {
                        //
                        // NODATA
                        //
                        // checks with nodata

                        for (int b = 0; b < dstBands; b++) {

                            int s00 = iterSource.getSample(xint, yint, b);
                            int s01 = iterSource.getSample(xint + 1, yint, b);
                            int s10 = iterSource.getSample(xint, yint + 1, b);
                            int s11 = iterSource.getSample(xint + 1, yint + 1, b);

                            final boolean w00 = noDataRange.contains((short) s00);
                            final boolean w01 = noDataRange.contains((short) s01);
                            final boolean w10 = noDataRange.contains((short) s10);
                            final boolean w11 = noDataRange.contains((short) s11);

                            if (w00 && w01 && w10 && w11) {
                                data[b][pixelOffset + bandOffsets[b]] = (short)backgroundValues[b];
                            } else {
                                data[b][pixelOffset + bandOffsets[b]] = InterpolationBilinear.computeValueDouble(
                                        s00, s01, s10, s11, w00, w01, w10, w11, xfrac, yfrac, 
                                        DataBuffer.TYPE_SHORT, backgroundValues[b]).shortValue();
                            }
                        }
                    }

                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            } // ROWS LOOP
              // BOTH ROI AND NODATA
        } else {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    final float sx = warpData[count++];
                    final float sy = warpData[count++];

                    final int xint = floor(sx);
                    final int yint = floor(sy);
                    final float xfrac = sx - xint;
                    final float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = (short)backgroundValues[b];
                            }
                        }
                    } else {
                        //
                        // ROI
                        //
                        // checks with roi
                        final boolean w00Roi = !(roiBounds.contains(xint, yint) && roiIter.getSample(xint, yint, 0) > 0);
                        final boolean w01Roi = !(roiBounds.contains(xint + 1, yint) && roiIter.getSample(xint + 1, yint, 0) > 0);
                        final boolean w10Roi = !(roiBounds.contains(xint, yint + 1) && roiIter.getSample(xint, yint + 1, 0) > 0);
                        final boolean w11Roi = !(roiBounds.contains(xint + 1, yint + 1) && roiIter.getSample(xint + 1, yint + 1, 0) > 0);
                        if (w00Roi && w01Roi && w10Roi && w11Roi) { // SG should not happen
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = (short)backgroundValues[b];
                            }
                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                //
                                // NODATA
                                //
                                // checks with nodata
                                int s00 = iterSource.getSample(xint, yint, b);
                                int s01 = iterSource.getSample(xint + 1, yint, b);
                                int s10 = iterSource.getSample(xint, yint + 1, b);
                                int s11 = iterSource.getSample(xint + 1, yint + 1, b);

                                final boolean w00 = noDataRange.contains((short) s00);
                                final boolean w01 = noDataRange.contains((short) s01);
                                final boolean w10 = noDataRange.contains((short) s10);
                                final boolean w11 = noDataRange.contains((short) s11);

                                if (w00 && w01 && w10 && w11) {
                                    data[b][pixelOffset + bandOffsets[b]] = (short)backgroundValues[b];
                                } else {
                                    data[b][pixelOffset + bandOffsets[b]] = InterpolationBilinear.computeValueDouble(
                                            s00, s01, s10, s11, w00, w01, w10, w11, xfrac, yfrac, 
                                            DataBuffer.TYPE_SHORT, backgroundValues[b]).shortValue();
                                }
                            }
                        }
                    }
                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            } // ROWS LOOP
        }

        iterSource.done();
    }

    protected void computeRectInt(final PlanarImage src, final RasterAccessor dst,
            final RandomIter roiIter, boolean roiContainsTile) {
        // Random iterator initialization. If an extender is used, then an extended image is taken.
        RandomIter iterSource = getRandomIterator(src, extender);

        final int minX = src.getMinX();
        final int maxX = src.getMaxX() - (extended ? 0 : 1); // Right padding
        final int minY = src.getMinY();
        final int maxY = src.getMaxY() - (extended ? 0 : 1); // Bottom padding

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int lineStride = dst.getScanlineStride();
        final int pixelStride = dst.getPixelStride();
        final int[] bandOffsets = dst.getBandOffsets();
        final int[][] data = dst.getIntDataArrays();

        final float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

        // ONLY VALID DATA
        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    final float sx = warpData[count++];
                    final float sy = warpData[count++];

                    final int xint = floor(sx);
                    final int yint = floor(sy);
                    final float xfrac = sx - xint;
                    final float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = (int)backgroundValues[b];
                            }
                        }
                    } else {
                        //
                        // NO ROI
                        //
                        for (int b = 0; b < dstBands; b++) {
                            int s00 = iterSource.getSample(xint, yint, b);
                            int s01 = iterSource.getSample(xint + 1, yint, b);
                            int s10 = iterSource.getSample(xint, yint + 1, b);
                            int s11 = iterSource.getSample(xint + 1, yint + 1, b);

                            float s0 = (s01 - s00) * xfrac + s00;
                            float s1 = (s11 - s10) * xfrac + s10;
                            float s = (s1 - s0) * yfrac + s0;

                            data[b][pixelOffset + bandOffsets[b]] = (int) s;
                        }
                    }
                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            }
            // ONLY ROI
        } else if (caseB) {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    final float sx = warpData[count++];
                    final float sy = warpData[count++];

                    final int xint = floor(sx);
                    final int yint = floor(sy);
                    final float xfrac = sx - xint;
                    final float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = (int)backgroundValues[b];
                            }
                        }
                    } else {
                        //
                        // ROI
                        //
                        // checks with roi
                        final boolean w00 = !(roiBounds.contains(xint, yint) && roiIter.getSample(xint, yint, 0) > 0);
                        final boolean w01 = !(roiBounds.contains(xint + 1, yint) && roiIter.getSample(xint + 1, yint, 0) > 0);
                        final boolean w10 = !(roiBounds.contains(xint, yint + 1) && roiIter.getSample(xint, yint + 1, 0) > 0);
                        final boolean w11 = !(roiBounds.contains(xint + 1, yint + 1) && roiIter.getSample(xint + 1, yint + 1, 0) > 0);
                        if (w00 && w01 && w10 && w11) { // SG should not happen
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = (int)backgroundValues[b];
                            }
                        } else {

                            for (int b = 0; b < dstBands; b++) {
                                int s00 = iterSource.getSample(xint, yint, b);
                                int s01 = iterSource.getSample(xint + 1, yint, b);
                                int s10 = iterSource.getSample(xint, yint + 1, b);
                                int s11 = iterSource.getSample(xint + 1, yint + 1, b);

                                float s0 = (s01 - s00) * xfrac + s00;
                                float s1 = (s11 - s10) * xfrac + s10;
                                float s = (s1 - s0) * yfrac + s0;

                                data[b][pixelOffset + bandOffsets[b]] = (int) s;
                            }
                        }
                    }

                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            } // ROWS LOOP
              // ONLY NODATA
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    final float sx = warpData[count++];
                    final float sy = warpData[count++];

                    final int xint = floor(sx);
                    final int yint = floor(sy);
                    final float xfrac = sx - xint;
                    final float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = (int)backgroundValues[b];
                            }
                        }
                    } else {
                        //
                        // NODATA
                        //
                        // checks with nodata

                        for (int b = 0; b < dstBands; b++) {

                            int s00 = iterSource.getSample(xint, yint, b);
                            int s01 = iterSource.getSample(xint + 1, yint, b);
                            int s10 = iterSource.getSample(xint, yint + 1, b);
                            int s11 = iterSource.getSample(xint + 1, yint + 1, b);

                            final boolean w00 = noDataRange.contains(s00);
                            final boolean w01 = noDataRange.contains(s01);
                            final boolean w10 = noDataRange.contains(s10);
                            final boolean w11 = noDataRange.contains(s11);

                            if (w00 && w01 && w10 && w11) {
                                data[b][pixelOffset + bandOffsets[b]] = (int)backgroundValues[b];
                            } else {
                                data[b][pixelOffset + bandOffsets[b]] = InterpolationBilinear.computeValueDouble(
                                        s00, s01, s10, s11, w00, w01, w10, w11, xfrac, yfrac, 
                                        DataBuffer.TYPE_INT, backgroundValues[b]).intValue();
                            }
                        }
                    }

                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            } // ROWS LOOP
              // BOTH ROI AND NODATA
        } else {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    final float sx = warpData[count++];
                    final float sy = warpData[count++];

                    final int xint = floor(sx);
                    final int yint = floor(sy);
                    final float xfrac = sx - xint;
                    final float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = (int)backgroundValues[b];
                            }
                        }
                    } else {
                        //
                        // ROI
                        //
                        // checks with roi
                        final boolean w00Roi = !(roiBounds.contains(xint, yint) && roiIter.getSample(xint, yint, 0) > 0);
                        final boolean w01Roi = !(roiBounds.contains(xint + 1, yint) && roiIter.getSample(xint + 1, yint, 0) > 0);
                        final boolean w10Roi = !(roiBounds.contains(xint, yint + 1) && roiIter.getSample(xint, yint + 1, 0) > 0);
                        final boolean w11Roi = !(roiBounds.contains(xint + 1, yint + 1) && roiIter.getSample(xint + 1, yint + 1, 0) > 0);
                        if (w00Roi && w01Roi && w10Roi && w11Roi) { // SG should not happen
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = (int)backgroundValues[b];
                            }
                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                //
                                // NODATA
                                //
                                // checks with nodata
                                int s00 = iterSource.getSample(xint, yint, b);
                                int s01 = iterSource.getSample(xint + 1, yint, b);
                                int s10 = iterSource.getSample(xint, yint + 1, b);
                                int s11 = iterSource.getSample(xint + 1, yint + 1, b);

                                final boolean w00 = noDataRange.contains(s00);
                                final boolean w01 = noDataRange.contains(s01);
                                final boolean w10 = noDataRange.contains(s10);
                                final boolean w11 = noDataRange.contains(s11);

                                if (w00 && w01 && w10 && w11) {
                                    data[b][pixelOffset + bandOffsets[b]] = (int)backgroundValues[b];
                                } else {
                                    data[b][pixelOffset + bandOffsets[b]] = InterpolationBilinear.computeValueDouble(
                                            s00, s01, s10, s11, w00, w01, w10, w11, xfrac, yfrac, 
                                            DataBuffer.TYPE_DOUBLE, backgroundValues[b]).intValue();
                                }
                            }
                        }
                    }
                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            } // ROWS LOOP
        }

        iterSource.done();
    }

    protected void computeRectFloat(final PlanarImage src, final RasterAccessor dst,
            final RandomIter roiIter, boolean roiContainsTile) {
        // Random iterator initialization. If an extender is used, then an extended image is taken.
        RandomIter iterSource = getRandomIterator(src, extender);

        final int minX = src.getMinX();
        final int maxX = src.getMaxX() - (extended ? 0 : 1); // Right padding
        final int minY = src.getMinY();
        final int maxY = src.getMaxY() - (extended ? 0 : 1); // Bottom padding

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int lineStride = dst.getScanlineStride();
        final int pixelStride = dst.getPixelStride();
        final int[] bandOffsets = dst.getBandOffsets();
        final float[][] data = dst.getFloatDataArrays();

        final float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

        // ONLY VALID DATA
        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    final float sx = warpData[count++];
                    final float sy = warpData[count++];

                    final int xint = floor(sx);
                    final int yint = floor(sy);
                    final float xfrac = sx - xint;
                    final float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = (float)backgroundValues[b];
                            }
                        }
                    } else {
                        //
                        // NO ROI
                        //
                        for (int b = 0; b < dstBands; b++) {
                            float s00 = iterSource.getSampleFloat(xint, yint, b);
                            float s01 = iterSource.getSampleFloat(xint + 1, yint, b);
                            float s10 = iterSource.getSampleFloat(xint, yint + 1, b);
                            float s11 = iterSource.getSampleFloat(xint + 1, yint + 1, b);

                            float s0 = (s01 - s00) * xfrac + s00;
                            float s1 = (s11 - s10) * xfrac + s10;
                            float s = (s1 - s0) * yfrac + s0;

                            data[b][pixelOffset + bandOffsets[b]] = s;
                        }
                    }
                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            }
            // ONLY ROI
        } else if (caseB) {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    final float sx = warpData[count++];
                    final float sy = warpData[count++];

                    final int xint = floor(sx);
                    final int yint = floor(sy);
                    final float xfrac = sx - xint;
                    final float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = (float)backgroundValues[b];
                            }
                        }
                    } else {
                        //
                        // ROI
                        //
                        // checks with roi
                        final boolean w00 = !(roiBounds.contains(xint, yint) && roiIter.getSample(xint, yint, 0) > 0);
                        final boolean w01 = !(roiBounds.contains(xint + 1, yint) && roiIter.getSample(xint + 1, yint, 0) > 0);
                        final boolean w10 = !(roiBounds.contains(xint, yint + 1) && roiIter.getSample(xint, yint + 1, 0) > 0);
                        final boolean w11 = !(roiBounds.contains(xint + 1, yint + 1) && roiIter.getSample(xint + 1, yint + 1, 0) > 0);
                        if (w00 && w01 && w10 && w11) { // SG should not happen
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = (float)backgroundValues[b];
                            }
                        } else {

                            for (int b = 0; b < dstBands; b++) {
                                float s00 = iterSource.getSampleFloat(xint, yint, b);
                                float s01 = iterSource.getSampleFloat(xint + 1, yint, b);
                                float s10 = iterSource.getSampleFloat(xint, yint + 1, b);
                                float s11 = iterSource.getSampleFloat(xint + 1, yint + 1, b);

                                float s0 = (s01 - s00) * xfrac + s00;
                                float s1 = (s11 - s10) * xfrac + s10;
                                float s = (s1 - s0) * yfrac + s0;

                                data[b][pixelOffset + bandOffsets[b]] = s;
                            }
                        }
                    }

                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            } // ROWS LOOP
              // ONLY NODATA
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    final float sx = warpData[count++];
                    final float sy = warpData[count++];

                    final int xint = floor(sx);
                    final int yint = floor(sy);
                    final float xfrac = sx - xint;
                    final float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = (float)backgroundValues[b];
                            }
                        }
                    } else {
                        //
                        // NODATA
                        //
                        // checks with nodata

                        for (int b = 0; b < dstBands; b++) {

                            float s00 = iterSource.getSampleFloat(xint, yint, b);
                            float s01 = iterSource.getSampleFloat(xint + 1, yint, b);
                            float s10 = iterSource.getSampleFloat(xint, yint + 1, b);
                            float s11 = iterSource.getSampleFloat(xint + 1, yint + 1, b);

                            final boolean w00 = noDataRange.contains(s00);
                            final boolean w01 = noDataRange.contains(s01);
                            final boolean w10 = noDataRange.contains(s10);
                            final boolean w11 = noDataRange.contains(s11);

                            if (w00 && w01 && w10 && w11) {
                                data[b][pixelOffset + bandOffsets[b]] = (float)backgroundValues[b];
                            } else {
                                data[b][pixelOffset + bandOffsets[b]] = InterpolationBilinear.computeValueDouble(
                                        s00, s01, s10, s11, w00, w01, w10, w11, xfrac, yfrac, 
                                        DataBuffer.TYPE_FLOAT, backgroundValues[b]).floatValue();
                            }
                        }
                    }

                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            } // ROWS LOOP
              // BOTH ROI AND NODATA
        } else {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    final float sx = warpData[count++];
                    final float sy = warpData[count++];

                    final int xint = floor(sx);
                    final int yint = floor(sy);
                    final float xfrac = sx - xint;
                    final float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = (float)backgroundValues[b];
                            }
                        }
                    } else {
                        //
                        // ROI
                        //
                        // checks with roi
                        final boolean w00Roi = !(roiBounds.contains(xint, yint) && roiIter.getSample(xint, yint, 0) > 0);
                        final boolean w01Roi = !(roiBounds.contains(xint + 1, yint) && roiIter.getSample(xint + 1, yint, 0) > 0);
                        final boolean w10Roi = !(roiBounds.contains(xint, yint + 1) && roiIter.getSample(xint, yint + 1, 0) > 0);
                        final boolean w11Roi = !(roiBounds.contains(xint + 1, yint + 1) && roiIter.getSample(xint + 1, yint + 1, 0) > 0);
                        if (w00Roi && w01Roi && w10Roi && w11Roi) { // SG should not happen
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = (float)backgroundValues[b];
                            }
                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                //
                                // NODATA
                                //
                                // checks with nodata
                                float s00 = iterSource.getSampleFloat(xint, yint, b);
                                float s01 = iterSource.getSampleFloat(xint + 1, yint, b);
                                float s10 = iterSource.getSampleFloat(xint, yint + 1, b);
                                float s11 = iterSource.getSampleFloat(xint + 1, yint + 1, b);

                                final boolean w00 = noDataRange.contains(s00);
                                final boolean w01 = noDataRange.contains(s01);
                                final boolean w10 = noDataRange.contains(s10);
                                final boolean w11 = noDataRange.contains(s11);

                                if (w00 && w01 && w10 && w11) {
                                    data[b][pixelOffset + bandOffsets[b]] = (float)backgroundValues[b];
                                } else {
                                    data[b][pixelOffset + bandOffsets[b]] = InterpolationBilinear.computeValueDouble(
                                            s00, s01, s10, s11, w00, w01, w10, w11, xfrac, yfrac, 
                                            DataBuffer.TYPE_FLOAT, backgroundValues[b]).floatValue();
                                }
                            }
                        }
                    }
                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            } // ROWS LOOP
        }
        iterSource.done();
    }

    protected void computeRectDouble(final PlanarImage src, final RasterAccessor dst,
            final RandomIter roiIter, boolean roiContainsTile) {
        // Random iterator initialization. If an extender is used, then an extended image is taken.
        RandomIter iterSource = getRandomIterator(src, extender);

        final int minX = src.getMinX();
        final int maxX = src.getMaxX() - (extended ? 0 : 1); // Right padding
        final int minY = src.getMinY();
        final int maxY = src.getMaxY() - (extended ? 0 : 1); // Bottom padding

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int lineStride = dst.getScanlineStride();
        final int pixelStride = dst.getPixelStride();
        final int[] bandOffsets = dst.getBandOffsets();
        final double[][] data = dst.getDoubleDataArrays();

        final float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

        // ONLY VALID DATA
        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    final float sx = warpData[count++];
                    final float sy = warpData[count++];

                    final int xint = floor(sx);
                    final int yint = floor(sy);
                    final float xfrac = sx - xint;
                    final float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = backgroundValues[b];
                            }
                        }
                    } else {
                        //
                        // NO ROI
                        //
                        for (int b = 0; b < dstBands; b++) {
                            double s00 = iterSource.getSampleDouble(xint, yint, b);
                            double s01 = iterSource.getSampleDouble(xint + 1, yint, b);
                            double s10 = iterSource.getSampleDouble(xint, yint + 1, b);
                            double s11 = iterSource.getSampleDouble(xint + 1, yint + 1, b);

                            double s0 = (s01 - s00) * xfrac + s00;
                            double s1 = (s11 - s10) * xfrac + s10;
                            double s = (s1 - s0) * yfrac + s0;

                            data[b][pixelOffset + bandOffsets[b]] = s;
                        }
                    }
                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            }
            // ONLY ROI
        } else if (caseB) {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    final float sx = warpData[count++];
                    final float sy = warpData[count++];

                    final int xint = floor(sx);
                    final int yint = floor(sy);
                    final float xfrac = sx - xint;
                    final float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = backgroundValues[b];
                            }
                        }
                    } else {
                        //
                        // ROI
                        //
                        // checks with roi
                        final boolean w00 = !(roiBounds.contains(xint, yint) && roiIter.getSample(xint, yint, 0) > 0);
                        final boolean w01 = !(roiBounds.contains(xint + 1, yint) && roiIter.getSample(xint + 1, yint, 0) > 0);
                        final boolean w10 = !(roiBounds.contains(xint, yint + 1) && roiIter.getSample(xint, yint + 1, 0) > 0);
                        final boolean w11 = !(roiBounds.contains(xint + 1, yint + 1) && roiIter.getSample(xint + 1, yint + 1, 0) > 0);
                        if (w00 && w01 && w10 && w11) { // SG should not happen
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = backgroundValues[b];
                            }
                        } else {

                            for (int b = 0; b < dstBands; b++) {
                                double s00 = iterSource.getSampleDouble(xint, yint, b);
                                double s01 = iterSource.getSampleDouble(xint + 1, yint, b);
                                double s10 = iterSource.getSampleDouble(xint, yint + 1, b);
                                double s11 = iterSource.getSampleDouble(xint + 1, yint + 1, b);

                                double s0 = (s01 - s00) * xfrac + s00;
                                double s1 = (s11 - s10) * xfrac + s10;
                                double s = (s1 - s0) * yfrac + s0;

                                data[b][pixelOffset + bandOffsets[b]] = s;
                            }
                        }
                    }

                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            } // ROWS LOOP
              // ONLY NODATA
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    final float sx = warpData[count++];
                    final float sy = warpData[count++];

                    final int xint = floor(sx);
                    final int yint = floor(sy);
                    final float xfrac = sx - xint;
                    final float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = backgroundValues[b];
                            }
                        }
                    } else {
                        //
                        // NODATA
                        //
                        // checks with nodata

                        for (int b = 0; b < dstBands; b++) {

                            double s00 = iterSource.getSampleDouble(xint, yint, b);
                            double s01 = iterSource.getSampleDouble(xint + 1, yint, b);
                            double s10 = iterSource.getSampleDouble(xint, yint + 1, b);
                            double s11 = iterSource.getSampleDouble(xint + 1, yint + 1, b);

                            final boolean w00 = noDataRange.contains(s00);
                            final boolean w01 = noDataRange.contains(s01);
                            final boolean w10 = noDataRange.contains(s10);
                            final boolean w11 = noDataRange.contains(s11);

                            if (w00 && w01 && w10 && w11) {
                                data[b][pixelOffset + bandOffsets[b]] = backgroundValues[b];
                            } else {
                                data[b][pixelOffset + bandOffsets[b]] = InterpolationBilinear.computeValueDouble(
                                        s00, s01, s10, s11, w00, w01, w10, w11, xfrac, yfrac, 
                                        DataBuffer.TYPE_DOUBLE, backgroundValues[b]).doubleValue();
                            }
                        }
                    }

                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            } // ROWS LOOP
              // BOTH ROI AND NODATA
        } else {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);
                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    final float sx = warpData[count++];
                    final float sy = warpData[count++];

                    final int xint = floor(sx);
                    final int yint = floor(sy);
                    final float xfrac = sx - xint;
                    final float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = backgroundValues[b];
                            }
                        }
                    } else {
                        //
                        // ROI
                        //
                        // checks with roi
                        final boolean w00Roi = !(roiBounds.contains(xint, yint) && roiIter.getSample(xint, yint, 0) > 0);
                        final boolean w01Roi = !(roiBounds.contains(xint + 1, yint) && roiIter.getSample(xint + 1, yint, 0) > 0);
                        final boolean w10Roi = !(roiBounds.contains(xint, yint + 1) && roiIter.getSample(xint, yint + 1, 0) > 0);
                        final boolean w11Roi = !(roiBounds.contains(xint + 1, yint + 1) && roiIter.getSample(xint + 1, yint + 1, 0) > 0);
                        if (w00Roi && w01Roi && w10Roi && w11Roi) { // SG should not happen
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = backgroundValues[b];
                            }
                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                //
                                // NODATA
                                //
                                // checks with nodata
                                double s00 = iterSource.getSampleDouble(xint, yint, b);
                                double s01 = iterSource.getSampleDouble(xint + 1, yint, b);
                                double s10 = iterSource.getSampleDouble(xint, yint + 1, b);
                                double s11 = iterSource.getSampleDouble(xint + 1, yint + 1, b);

                                final boolean w00 = noDataRange.contains(s00);
                                final boolean w01 = noDataRange.contains(s01);
                                final boolean w10 = noDataRange.contains(s10);
                                final boolean w11 = noDataRange.contains(s11);

                                if (w00 && w01 && w10 && w11) {
                                    data[b][pixelOffset + bandOffsets[b]] = backgroundValues[b];
                                } else {
                                    data[b][pixelOffset + bandOffsets[b]] = InterpolationBilinear.computeValueDouble(
                                            s00, s01, s10, s11, w00, w01, w10, w11, xfrac, yfrac, 
                                            DataBuffer.TYPE_DOUBLE, backgroundValues[b]).doubleValue();
                                }
                            }
                        }
                    }
                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            } // ROWS LOOP
        }
        iterSource.done();
    }
}
