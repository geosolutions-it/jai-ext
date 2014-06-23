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

import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;

import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.Map;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationTable;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.RasterAccessor;
import javax.media.jai.Warp;
import javax.media.jai.iterator.RandomIter;

/**
 * An <code>OpImage</code> implementing the general "Warp" operation as described in <code>javax.media.jai.operator.WarpDescriptor</code>. It supports
 * the bicubic interpolation.
 * 
 * <p>
 * The layout for the destination image may be specified via the <code>ImageLayout</code> parameter. However, only those settings suitable for this
 * operation will be used. The unsuitable settings will be replaced by default suitable values. An optional ROI object and a NoData Range can be used.
 * If a backward mapped pixel lies outside ROI or it is a NoData, then the destination pixel value is a background value.
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
final class WarpBicubicOpImage extends WarpOpImage {
    /** Constant holding the bicubic kernel value */
    private static final int KERNEL_LINE_DIM = 4;

    /**
     * Unsigned short Max Value
     */
    protected static final int USHORT_MAX_VALUE = Short.MAX_VALUE - Short.MIN_VALUE;

    /** Color table representing source's IndexColorModel. */
    private byte[][] ctable = null;

    /** LookupTable used for a faster NoData check */
    private boolean[] booleanLookupTable;

    /** Integer coeffs for horizontal interpolation */
    private int[] dataHi;

    /** Integer coeffs for vertical interpolation */
    private int[] dataVi;

    /** Float coeffs for horizontal interpolation */
    private float[] dataHf;

    /** Float coeffs for vertical interpolation */
    private float[] dataVf;

    /** Double coeffs for horizontal interpolation */
    private double[] dataHd;

    /** Double coeffs for vertical interpolation */
    private double[] dataVd;

    /** Shift used in integer computations */
    private int shift;

    /** Integer value used for rounding integer computations */
    private int round;

    /** Precision bits used for expanding the integer interpolation */
    private int precisionBits;

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
    public WarpBicubicOpImage(final RenderedImage source, final BorderExtender extender,
            final Map<?, ?> config, final ImageLayout layout, final Warp warp,
            final Interpolation interp, final ROI sourceROI, Range noData) {
        super(source, layout, config, false, extender, interp, warp, null, sourceROI, noData);

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
        destinationNoDataDouble = backgroundValues[0];
        SampleModel sm = source.getSampleModel();
        // Source image data Type
        int srcDataType = sm.getDataType();

        switch (srcDataType) {
        case DataBuffer.TYPE_BYTE:
            destinationNoDataByte = (byte) (((byte) destinationNoDataDouble) & 0xff);
            // Creation of a lookuptable containing the values to use for no data
            if (hasNoData) {
                booleanLookupTable = new boolean[256];
                for (int i = 0; i < booleanLookupTable.length; i++) {
                    byte value = (byte) i;
                    booleanLookupTable[i] = noDataRange.contains(value);
                }
            }
            break;
        case DataBuffer.TYPE_USHORT:
            destinationNoDataShort = (short) (((short) destinationNoDataDouble) & 0xffff);
            break;
        case DataBuffer.TYPE_SHORT:
            destinationNoDataShort = (short) destinationNoDataDouble;
            break;
        case DataBuffer.TYPE_INT:
            destinationNoDataInt = (int) destinationNoDataDouble;
            break;
        case DataBuffer.TYPE_FLOAT:
            destinationNoDataFloat = (float) destinationNoDataDouble;
            break;
        case DataBuffer.TYPE_DOUBLE:
            break;
        default:
            throw new IllegalArgumentException("Wrong data Type");
        }
        // Selection of the interpolation coefficients
        InterpolationTable interpCubic = (InterpolationTable) interp;

        switch (srcDataType) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_INT:
            dataHi = interpCubic.getHorizontalTableData();
            dataVi = interpCubic.getVerticalTableData();
            break;
        case DataBuffer.TYPE_FLOAT:
            dataHf = interpCubic.getHorizontalTableDataFloat();
            dataVf = interpCubic.getVerticalTableDataFloat();
            break;
        case DataBuffer.TYPE_DOUBLE:
            dataHd = interpCubic.getHorizontalTableDataDouble();
            dataVd = interpCubic.getVerticalTableDataDouble();
            break;
        default:
            throw new IllegalArgumentException("Wrong data Type");
        }

        // Subsample bits used
        shift = 1 << interp.getSubsampleBitsH();

        precisionBits = interpCubic.getPrecisionBits();

        if (precisionBits > 0) {
            round = 1 << (precisionBits - 1);
        }
    }

    protected void computeRectByte(final PlanarImage src, final RasterAccessor dst,
            final ROI roiTile) {

        // Random Iterator initialization, taking into account the presence of the borderExtender
        RandomIter iterSource;
        final int minX, maxX, minY, maxY;
        if (extended) {
            final Rectangle bounds = new Rectangle(src.getMinX() - 1, src.getMinY() - 1,
                    src.getWidth() + 3, src.getHeight() + 3);
            iterSource = RandomIterFactory.create(src.getExtendedData(bounds, extender), bounds,
                    TILE_CACHED, ARRAY_CALC);

            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();

        } else {
            iterSource = RandomIterFactory.create(src, src.getBounds(), TILE_CACHED, ARRAY_CALC);

            minX = src.getMinX() + 1; // Left padding
            maxX = src.getMaxX() - 2; // Right padding
            minY = src.getMinY() + 1; // Top padding
            maxY = src.getMaxY() - 2; // Bottom padding
        }

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
            if (caseA) {
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
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            // X and Y offset initialization
                            final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                            final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                            //
                            // NO ROI
                            //
                            for (int b = 0; b < dstBands; b++) {
                                // Interpolation
                                long result = (bicubicCalculationInt(b, iterSource, xint, yint,
                                        offsetX, offsetY, null));
                                // Clamp
                                if (result > 255) {
                                    result = 255;
                                } else if (result < 0) {
                                    result = 0;
                                }

                                data[b][pixelOffset + bandOffsets[b]] = (byte) (result & 0xff);
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
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            // X and Y offset initialization
                            final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                            final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                            //
                            // ROI
                            //
                            // checks with roi
                            // Initialization of the flag indicating that all the kernel pixels are inside the ROI
                            boolean inRoi = false;

                            for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                                for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                    inRoi |= roiTile.contains(xint + (i - 1), yint + (j - 1));
                                }
                            }
                            if (inRoi) {

                                for (int b = 0; b < dstBands; b++) {
                                    // Interpolation
                                    long result = (bicubicCalculationInt(b, iterSource, xint, yint,
                                            offsetX, offsetY, null));
                                    // Clamp
                                    if (result > 255) {
                                        result = 255;
                                    } else if (result < 0) {
                                        result = 0;
                                    }

                                    data[b][pixelOffset + bandOffsets[b]] = (byte) (result & 0xff);
                                }
                            } else {
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        }

                        // next desination pixel
                        pixelOffset += pixelStride;
                    } // COLS LOOP
                } // ROWS LOOP
                  // ONLY NODATA
            } else if (caseC) {
                // Array used during calculations
                final long[][] pixelKernel = new long[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
                long[] sumArray = new long[KERNEL_LINE_DIM];
                long[] emptyArray = new long[KERNEL_LINE_DIM];
                long[] tempData;

                // Value used in calculations
                short weight = 0;
                byte weightVert = 0;
                byte temp = 0;

                // Row temporary sum initialization
                long tempSum = 0;
                long sum = 0;
                // final result initialization
                long result = 0;

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
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            // X and Y offset initialization
                            final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                            final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                            //
                            // NODATA
                            //
                            // checks with nodata
                            for (int b = 0; b < dstBands; b++) {

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and check if every kernel pixel is a No Data
                                for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                                    for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                        // Selection of one pixel
                                        int sample = iterSource.getSample(xint + (i - 1), yint
                                                + (j - 1), b) & 0xFF;
                                        pixelKernel[j][i] = sample;
                                        if (booleanLookupTable[sample]) {
                                            weight &= (0xffff - (1 << KERNEL_LINE_DIM * j + i));
                                            // weigjtArray[j][z] = 0;
                                        } else {
                                            // weigjtArray[j][z] = 1;
                                            weight |= (1 << (KERNEL_LINE_DIM * j + i));
                                        }
                                    }
                                    // Data elaboration for each line
                                    temp = (byte) ((weight >> KERNEL_LINE_DIM * j) & 0x0F);
                                    tempData = bicubicInpainting(pixelKernel[j], temp, emptyArray);

                                    tempSum = tempData[0] * dataHi[offsetX] + tempData[1]
                                            * dataHi[offsetX + 1] + tempData[2]
                                            * dataHi[offsetX + 2] + tempData[3]
                                            * dataHi[offsetX + 3];

                                    if (temp > 0) {
                                        weightVert |= (1 << j);
                                        // weigjtArrayVertical[j] = 1;
                                    } else {
                                        weightVert &= (0x0F - (1 << j));
                                        // weigjtArrayVertical[j] = 0;
                                    }
                                    sumArray[j] = ((tempSum + round) >> precisionBits);
                                }

                                // Control if the 16 pixel are all No Data
                                if (weight == 0) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                } else {

                                    tempData = bicubicInpainting(sumArray, weightVert, emptyArray);

                                    // Vertical sum update
                                    sum = tempData[0] * dataVi[offsetY] + tempData[1]
                                            * dataVi[offsetY + 1] + tempData[2]
                                            * dataVi[offsetY + 2] + tempData[3]
                                            * dataVi[offsetY + 3];

                                    // Interpolation
                                    result = ((sum + round) >> precisionBits);
                                    weight = 0;
                                    weightVert = 0;
                                    sum = 0;
                                    // Clamp
                                    if (result > 255) {
                                        result = 255;
                                    } else if (result < 0) {
                                        result = 0;
                                    }
                                    data[b][pixelOffset + bandOffsets[b]] = (byte) (result & 0xff);
                                }
                            }
                        }

                        // next desination pixel
                        pixelOffset += pixelStride;
                    } // COLS LOOP
                } // ROWS LOOP
                  // BOTH ROI AND NODATA
            } else {
                // Array used during calculations
                final long[][] pixelKernel = new long[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
                long[] sumArray = new long[KERNEL_LINE_DIM];
                long[] emptyArray = new long[KERNEL_LINE_DIM];
                long[] tempData;

                // Value used in calculations
                short weight = 0;
                byte weightVert = 0;
                byte temp = 0;

                // Row temporary sum initialization
                long tempSum = 0;
                long sum = 0;
                // final result initialization
                long result = 0;
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
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            // X and Y offset initialization
                            final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                            final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                            //
                            // ROI
                            //
                            // checks with roi
                            // Initialization of the flag indicating that all the kernel pixels are inside the ROI
                            boolean inRoi = false;

                            for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                                for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                    inRoi |= roiTile.contains(xint + (i - 1), yint + (j - 1));
                                }
                            }
                            if (inRoi) {
                                //
                                // NODATA
                                //
                                // checks with nodata
                                for (int b = 0; b < dstBands; b++) {

                                    // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                    // and check if every kernel pixel is a No Data
                                    for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                                        for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                            // Selection of one pixel
                                            int sample = iterSource.getSample(xint + (i - 1), yint
                                                    + (j - 1), b) & 0xFF;
                                            pixelKernel[j][i] = sample;
                                            if (booleanLookupTable[sample]) {
                                                weight &= (0xffff - (1 << KERNEL_LINE_DIM * j + i));
                                                // weigjtArray[j][z] = 0;
                                            } else {
                                                // weigjtArray[j][z] = 1;
                                                weight |= (1 << (KERNEL_LINE_DIM * j + i));
                                            }
                                        }
                                        // Data elaboration for each line
                                        temp = (byte) ((weight >> KERNEL_LINE_DIM * j) & 0x0F);
                                        tempData = bicubicInpainting(pixelKernel[j], temp,
                                                emptyArray);

                                        tempSum = tempData[0] * dataHi[offsetX] + tempData[1]
                                                * dataHi[offsetX + 1] + tempData[2]
                                                * dataHi[offsetX + 2] + tempData[3]
                                                * dataHi[offsetX + 3];

                                        if (temp > 0) {
                                            weightVert |= (1 << j);
                                            // weigjtArrayVertical[j] = 1;
                                        } else {
                                            weightVert &= (0x0F - (1 << j));
                                            // weigjtArrayVertical[j] = 0;
                                        }
                                        sumArray[j] = ((tempSum + round) >> precisionBits);
                                    }

                                    // Control if the 16 pixel are all No Data
                                    if (weight == 0) {
                                        data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                    } else {

                                        tempData = bicubicInpainting(sumArray, weightVert,
                                                emptyArray);

                                        // Vertical sum update
                                        sum = tempData[0] * dataVi[offsetY] + tempData[1]
                                                * dataVi[offsetY + 1] + tempData[2]
                                                * dataVi[offsetY + 2] + tempData[3]
                                                * dataVi[offsetY + 3];

                                        // Interpolation
                                        result = ((sum + round) >> precisionBits);
                                        weight = 0;
                                        weightVert = 0;
                                        sum = 0;
                                        // Clamp
                                        if (result > 255) {
                                            result = 255;
                                        } else if (result < 0) {
                                            result = 0;
                                        }
                                        data[b][pixelOffset + bandOffsets[b]] = (byte) (result & 0xff);
                                    }
                                }
                            } else {
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
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
            if (caseA) {
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
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            // X and Y offset initialization
                            final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                            final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                            //
                            // NO ROI
                            //
                            for (int b = 0; b < dstBands; b++) {
                                final byte[] t = ctable[b];
                                // Interpolation
                                long result = (bicubicCalculationInt(b, iterSource, xint, yint,
                                        offsetX, offsetY, t));
                                // Clamp
                                if (result > 255) {
                                    result = 255;
                                } else if (result < 0) {
                                    result = 0;
                                }

                                data[b][pixelOffset + bandOffsets[b]] = (byte) (result & 0xff);
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
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            // X and Y offset initialization
                            final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                            final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                            //
                            // ROI
                            //
                            // checks with roi
                            // Initialization of the flag indicating that all the kernel pixels are inside the ROI
                            boolean inRoi = false;

                            for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                                for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                    inRoi |= roiTile.contains(xint + (i - 1), yint + (j - 1));
                                }
                            }
                            if (inRoi) {

                                for (int b = 0; b < dstBands; b++) {
                                    final byte[] t = ctable[b];
                                    // Interpolation
                                    long result = (bicubicCalculationInt(b, iterSource, xint, yint,
                                            offsetX, offsetY, t));
                                    // Clamp
                                    if (result > 255) {
                                        result = 255;
                                    } else if (result < 0) {
                                        result = 0;
                                    }

                                    data[b][pixelOffset + bandOffsets[b]] = (byte) (result & 0xff);
                                }
                            } else {
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        }

                        // next desination pixel
                        pixelOffset += pixelStride;
                    } // COLS LOOP
                } // ROWS LOOP
                  // ONLY NODATA
            } else if (caseC) {
                // Array used during calculations
                final long[][] pixelKernel = new long[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
                long[] sumArray = new long[KERNEL_LINE_DIM];
                long[] emptyArray = new long[KERNEL_LINE_DIM];
                long[] tempData;

                // Value used in calculations
                short weight = 0;
                byte weightVert = 0;
                byte temp = 0;

                // Row temporary sum initialization
                long tempSum = 0;
                long sum = 0;
                // final result initialization
                long result = 0;

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
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            // X and Y offset initialization
                            final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                            final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                            //
                            // NODATA
                            //
                            // checks with nodata
                            for (int b = 0; b < dstBands; b++) {
                                final byte[] t = ctable[b];
                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and check if every kernel pixel is a No Data
                                for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                                    for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                        // Selection of one pixel
                                        int sample = t[iterSource.getSample(xint + (i - 1), yint
                                                + (j - 1), 0) & 0xFF] & 0xFF;
                                        pixelKernel[j][i] = sample;
                                        if (booleanLookupTable[sample]) {
                                            weight &= (0xffff - (1 << KERNEL_LINE_DIM * j + i));
                                            // weigjtArray[j][z] = 0;
                                        } else {
                                            // weigjtArray[j][z] = 1;
                                            weight |= (1 << (KERNEL_LINE_DIM * j + i));
                                        }
                                    }
                                    // Data elaboration for each line
                                    temp = (byte) ((weight >> KERNEL_LINE_DIM * j) & 0x0F);
                                    tempData = bicubicInpainting(pixelKernel[j], temp, emptyArray);

                                    tempSum = tempData[0] * dataHi[offsetX] + tempData[1]
                                            * dataHi[offsetX + 1] + tempData[2]
                                            * dataHi[offsetX + 2] + tempData[3]
                                            * dataHi[offsetX + 3];

                                    if (temp > 0) {
                                        weightVert |= (1 << j);
                                        // weigjtArrayVertical[j] = 1;
                                    } else {
                                        weightVert &= (0x0F - (1 << j));
                                        // weigjtArrayVertical[j] = 0;
                                    }
                                    sumArray[j] = ((tempSum + round) >> precisionBits);
                                }

                                // Control if the 16 pixel are all No Data
                                if (weight == 0) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                } else {

                                    tempData = bicubicInpainting(sumArray, weightVert, emptyArray);

                                    // Vertical sum update
                                    sum = tempData[0] * dataVi[offsetY] + tempData[1]
                                            * dataVi[offsetY + 1] + tempData[2]
                                            * dataVi[offsetY + 2] + tempData[3]
                                            * dataVi[offsetY + 3];

                                    // Interpolation
                                    result = ((sum + round) >> precisionBits);
                                    weight = 0;
                                    weightVert = 0;
                                    sum = 0;

                                    // Clamp
                                    if (result > 255) {
                                        result = 255;
                                    } else if (result < 0) {
                                        result = 0;
                                    }

                                    data[b][pixelOffset + bandOffsets[b]] = (byte) (result & 0xff);
                                }
                            }
                        }

                        // next desination pixel
                        pixelOffset += pixelStride;
                    } // COLS LOOP
                } // ROWS LOOP
                  // BOTH ROI AND NODATA
            } else {
                // Array used during calculations
                final long[][] pixelKernel = new long[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
                long[] sumArray = new long[KERNEL_LINE_DIM];
                long[] emptyArray = new long[KERNEL_LINE_DIM];
                long[] tempData;

                // Value used in calculations
                short weight = 0;
                byte weightVert = 0;
                byte temp = 0;

                // Row temporary sum initialization
                long tempSum = 0;
                long sum = 0;
                // final result initialization
                long result = 0;
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
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            // X and Y offset initialization
                            final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                            final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                            //
                            // ROI
                            //
                            // checks with roi
                            // Initialization of the flag indicating that all the kernel pixels are inside the ROI
                            boolean inRoi = false;

                            for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                                for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                    inRoi |= roiTile.contains(xint + (i - 1), yint + (j - 1));
                                }
                            }
                            if (inRoi) {
                                //
                                // NODATA
                                //
                                // checks with nodata
                                for (int b = 0; b < dstBands; b++) {
                                    final byte[] t = ctable[b];
                                    // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                    // and check if every kernel pixel is a No Data
                                    for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                                        for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                            // Selection of one pixel
                                            int sample = t[iterSource.getSample(xint + (i - 1),
                                                    yint + (j - 1), 0) & 0xFF] & 0xFF;
                                            pixelKernel[j][i] = sample;
                                            if (booleanLookupTable[sample]) {
                                                weight &= (0xffff - (1 << KERNEL_LINE_DIM * j + i));
                                                // weigjtArray[j][z] = 0;
                                            } else {
                                                // weigjtArray[j][z] = 1;
                                                weight |= (1 << (KERNEL_LINE_DIM * j + i));
                                            }
                                        }
                                        // Data elaboration for each line
                                        temp = (byte) ((weight >> KERNEL_LINE_DIM * j) & 0x0F);
                                        tempData = bicubicInpainting(pixelKernel[j], temp,
                                                emptyArray);

                                        tempSum = tempData[0] * dataHi[offsetX] + tempData[1]
                                                * dataHi[offsetX + 1] + tempData[2]
                                                * dataHi[offsetX + 2] + tempData[3]
                                                * dataHi[offsetX + 3];

                                        if (temp > 0) {
                                            weightVert |= (1 << j);
                                            // weigjtArrayVertical[j] = 1;
                                        } else {
                                            weightVert &= (0x0F - (1 << j));
                                            // weigjtArrayVertical[j] = 0;
                                        }
                                        sumArray[j] = ((tempSum + round) >> precisionBits);
                                    }

                                    // Control if the 16 pixel are all No Data
                                    if (weight == 0) {
                                        data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                    } else {

                                        tempData = bicubicInpainting(sumArray, weightVert,
                                                emptyArray);

                                        // Vertical sum update
                                        sum = tempData[0] * dataVi[offsetY] + tempData[1]
                                                * dataVi[offsetY + 1] + tempData[2]
                                                * dataVi[offsetY + 2] + tempData[3]
                                                * dataVi[offsetY + 3];

                                        // Interpolation
                                        result = ((sum + round) >> precisionBits);
                                        weight = 0;
                                        weightVert = 0;
                                        sum = 0;

                                        // Clamp
                                        if (result > 255) {
                                            result = 255;
                                        } else if (result < 0) {
                                            result = 0;
                                        }

                                        data[b][pixelOffset + bandOffsets[b]] = (byte) (result & 0xff);
                                    }
                                }
                            } else {
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
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
            final ROI roiTile) {
        // Random Iterator initialization, taking into account the presence of the borderExtender
        RandomIter iterSource;
        final int minX, maxX, minY, maxY;
        if (extended) {
            final Rectangle bounds = new Rectangle(src.getMinX() - 1, src.getMinY() - 1,
                    src.getWidth() + 3, src.getHeight() + 3);
            iterSource = RandomIterFactory.create(src.getExtendedData(bounds, extender), bounds,
                    TILE_CACHED, ARRAY_CALC);

            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();

        } else {
            iterSource = RandomIterFactory.create(src, src.getBounds(), TILE_CACHED, ARRAY_CALC);

            minX = src.getMinX() + 1; // Left padding
            maxX = src.getMaxX() - 2; // Right padding
            minY = src.getMinY() + 1; // Top padding
            maxY = src.getMaxY() - 2; // Bottom padding
        }

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int lineStride = dst.getScanlineStride();
        final int pixelStride = dst.getPixelStride();
        final int[] bandOffsets = dst.getBandOffsets();
        final short[][] data = dst.getShortDataArrays();

        final float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

        // source does not have IndexColorModel
        // ONLY VALID DATA
        if (caseA) {
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                    } else {
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                        //
                        // NO ROI
                        //
                        for (int b = 0; b < dstBands; b++) {
                            // Interpolation
                            long result = (bicubicCalculationInt(b, iterSource, xint, yint,
                                    offsetX, offsetY, null));

                            // Clamp
                            if (result > USHORT_MAX_VALUE) {
                                result = USHORT_MAX_VALUE;
                            } else if (result < 0) {
                                result = 0;
                            }

                            data[b][pixelOffset + bandOffsets[b]] = (short) (result & 0xFFFF);
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                    } else {
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                        //
                        // ROI
                        //
                        // checks with roi
                        // Initialization of the flag indicating that all the kernel pixels are inside the ROI
                        boolean inRoi = false;

                        for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                            for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                inRoi |= roiTile.contains(xint + (i - 1), yint + (j - 1));
                            }
                        }
                        if (inRoi) {

                            for (int b = 0; b < dstBands; b++) {
                                // Interpolation
                                long result = (bicubicCalculationInt(b, iterSource, xint, yint,
                                        offsetX, offsetY, null));

                                // Clamp
                                if (result > USHORT_MAX_VALUE) {
                                    result = USHORT_MAX_VALUE;
                                } else if (result < 0) {
                                    result = 0;
                                }

                                data[b][pixelOffset + bandOffsets[b]] = (short) (result & 0xFFFF);
                            }
                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                    }

                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            } // ROWS LOOP
              // ONLY NODATA
        } else if (caseC) {
            // Array used during calculations
            final long[][] pixelKernel = new long[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
            long[] sumArray = new long[KERNEL_LINE_DIM];
            long[] emptyArray = new long[KERNEL_LINE_DIM];
            long[] tempData;

            // Value used in calculations
            short weight = 0;
            byte weightVert = 0;
            byte temp = 0;

            // Row temporary sum initialization
            long tempSum = 0;
            long sum = 0;
            // final result initialization
            long result = 0;

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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                    } else {
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                        //
                        // NODATA
                        //
                        // checks with nodata
                        for (int b = 0; b < dstBands; b++) {

                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            // and check if every kernel pixel is a No Data
                            for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                                for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                    // Selection of one pixel
                                    int sample = iterSource.getSample(xint + (i - 1), yint
                                            + (j - 1), b) & 0xFFFF;
                                    pixelKernel[j][i] = sample;
                                    if (noDataRange.contains((short) sample)) {
                                        weight &= (0xffff - (1 << KERNEL_LINE_DIM * j + i));
                                        // weigjtArray[j][z] = 0;
                                    } else {
                                        // weigjtArray[j][z] = 1;
                                        weight |= (1 << (KERNEL_LINE_DIM * j + i));
                                    }
                                }
                                // Data elaboration for each line
                                temp = (byte) ((weight >> KERNEL_LINE_DIM * j) & 0x0F);
                                tempData = bicubicInpainting(pixelKernel[j], temp, emptyArray);

                                tempSum = tempData[0] * dataHi[offsetX] + tempData[1]
                                        * dataHi[offsetX + 1] + tempData[2] * dataHi[offsetX + 2]
                                        + tempData[3] * dataHi[offsetX + 3];

                                if (temp > 0) {
                                    weightVert |= (1 << j);
                                    // weigjtArrayVertical[j] = 1;
                                } else {
                                    weightVert &= (0x0F - (1 << j));
                                    // weigjtArrayVertical[j] = 0;
                                }
                                sumArray[j] = ((tempSum + round) >> precisionBits);
                            }

                            // Control if the 16 pixel are all No Data
                            if (weight == 0) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            } else {

                                tempData = bicubicInpainting(sumArray, weightVert, emptyArray);

                                // Vertical sum update
                                sum = tempData[0] * dataVi[offsetY] + tempData[1]
                                        * dataVi[offsetY + 1] + tempData[2] * dataVi[offsetY + 2]
                                        + tempData[3] * dataVi[offsetY + 3];

                                // Interpolation
                                result = ((sum + round) >> precisionBits);
                                weight = 0;
                                weightVert = 0;
                                sum = 0;

                                // Clamp
                                if (result > USHORT_MAX_VALUE) {
                                    result = USHORT_MAX_VALUE;
                                } else if (result < 0) {
                                    result = 0;
                                }

                                data[b][pixelOffset + bandOffsets[b]] = (short) (result & 0xFFFF);
                            }
                        }
                    }

                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            } // ROWS LOOP
              // BOTH ROI AND NODATA
        } else {
            // Array used during calculations
            final long[][] pixelKernel = new long[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
            long[] sumArray = new long[KERNEL_LINE_DIM];
            long[] emptyArray = new long[KERNEL_LINE_DIM];
            long[] tempData;

            // Value used in calculations
            short weight = 0;
            byte weightVert = 0;
            byte temp = 0;

            // Row temporary sum initialization
            long tempSum = 0;
            long sum = 0;
            // final result initialization
            long result = 0;
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                    } else {
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                        //
                        // ROI
                        //
                        // checks with roi
                        // Initialization of the flag indicating that all the kernel pixels are inside the ROI
                        boolean inRoi = false;

                        for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                            for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                inRoi |= roiTile.contains(xint + (i - 1), yint + (j - 1));
                            }
                        }
                        if (inRoi) {
                            //
                            // NODATA
                            //
                            // checks with nodata
                            for (int b = 0; b < dstBands; b++) {

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and check if every kernel pixel is a No Data
                                for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                                    for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                        // Selection of one pixel
                                        int sample = iterSource.getSample(xint + (i - 1), yint
                                                + (j - 1), b) & 0xFFFF;
                                        pixelKernel[j][i] = sample;
                                        if (noDataRange.contains((short) sample)) {
                                            weight &= (0xffff - (1 << KERNEL_LINE_DIM * j + i));
                                            // weigjtArray[j][z] = 0;
                                        } else {
                                            // weigjtArray[j][z] = 1;
                                            weight |= (1 << (KERNEL_LINE_DIM * j + i));
                                        }
                                    }
                                    // Data elaboration for each line
                                    temp = (byte) ((weight >> KERNEL_LINE_DIM * j) & 0x0F);
                                    tempData = bicubicInpainting(pixelKernel[j], temp, emptyArray);

                                    tempSum = tempData[0] * dataHi[offsetX] + tempData[1]
                                            * dataHi[offsetX + 1] + tempData[2]
                                            * dataHi[offsetX + 2] + tempData[3]
                                            * dataHi[offsetX + 3];

                                    if (temp > 0) {
                                        weightVert |= (1 << j);
                                        // weigjtArrayVertical[j] = 1;
                                    } else {
                                        weightVert &= (0x0F - (1 << j));
                                        // weigjtArrayVertical[j] = 0;
                                    }
                                    sumArray[j] = ((tempSum + round) >> precisionBits);
                                }

                                // Control if the 16 pixel are all No Data
                                if (weight == 0) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                                } else {

                                    tempData = bicubicInpainting(sumArray, weightVert, emptyArray);

                                    // Vertical sum update
                                    sum = tempData[0] * dataVi[offsetY] + tempData[1]
                                            * dataVi[offsetY + 1] + tempData[2]
                                            * dataVi[offsetY + 2] + tempData[3]
                                            * dataVi[offsetY + 3];

                                    // Interpolation
                                    result = ((sum + round) >> precisionBits);
                                    weight = 0;
                                    weightVert = 0;
                                    sum = 0;

                                    // Clamp
                                    if (result > USHORT_MAX_VALUE) {
                                        result = USHORT_MAX_VALUE;
                                    } else if (result < 0) {
                                        result = 0;
                                    }

                                    data[b][pixelOffset + bandOffsets[b]] = (short) (result & 0xFFFF);
                                }
                            }
                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
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
            final ROI roiTile) {
        // Random Iterator initialization, taking into account the presence of the borderExtender
        RandomIter iterSource;
        final int minX, maxX, minY, maxY;
        if (extended) {
            final Rectangle bounds = new Rectangle(src.getMinX() - 1, src.getMinY() - 1,
                    src.getWidth() + 3, src.getHeight() + 3);
            iterSource = RandomIterFactory.create(src.getExtendedData(bounds, extender), bounds,
                    TILE_CACHED, ARRAY_CALC);

            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();

        } else {
            iterSource = RandomIterFactory.create(src, src.getBounds(), TILE_CACHED, ARRAY_CALC);

            minX = src.getMinX() + 1; // Left padding
            maxX = src.getMaxX() - 2; // Right padding
            minY = src.getMinY() + 1; // Top padding
            maxY = src.getMaxY() - 2; // Bottom padding
        }

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int lineStride = dst.getScanlineStride();
        final int pixelStride = dst.getPixelStride();
        final int[] bandOffsets = dst.getBandOffsets();
        final short[][] data = dst.getShortDataArrays();

        final float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

        // source does not have IndexColorModel
        // ONLY VALID DATA
        if (caseA) {
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                    } else {
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                        //
                        // NO ROI
                        //
                        for (int b = 0; b < dstBands; b++) {
                            // Interpolation
                            long result = bicubicCalculationInt(b, iterSource, xint, yint, offsetX,
                                    offsetY, null);

                            // Clamp
                            if (result > Short.MAX_VALUE) {
                                result = Short.MAX_VALUE;
                            } else if (result < Short.MIN_VALUE) {
                                result = Short.MIN_VALUE;
                            }

                            data[b][pixelOffset + bandOffsets[b]] = (short) (result);
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                    } else {
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                        //
                        // ROI
                        //
                        // checks with roi
                        // Initialization of the flag indicating that all the kernel pixels are inside the ROI
                        boolean inRoi = false;

                        for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                            for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                inRoi |= roiTile.contains(xint + (i - 1), yint + (j - 1));
                            }
                        }
                        if (inRoi) {

                            for (int b = 0; b < dstBands; b++) {
                                // Interpolation
                                long result = bicubicCalculationInt(b, iterSource, xint, yint,
                                        offsetX, offsetY, null);

                                // Clamp
                                if (result > Short.MAX_VALUE) {
                                    result = Short.MAX_VALUE;
                                } else if (result < Short.MIN_VALUE) {
                                    result = Short.MIN_VALUE;
                                }

                                data[b][pixelOffset + bandOffsets[b]] = (short) (result);
                            }
                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                    }

                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            } // ROWS LOOP
              // ONLY NODATA
        } else if (caseC) {
            // Array used during calculations
            final long[][] pixelKernel = new long[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
            long[] sumArray = new long[KERNEL_LINE_DIM];
            long[] emptyArray = new long[KERNEL_LINE_DIM];
            long[] tempData;

            // Value used in calculations
            short weight = 0;
            byte weightVert = 0;
            byte temp = 0;

            // Row temporary sum initialization
            long tempSum = 0;
            long sum = 0;
            // final result initialization
            long result = 0;

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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                    } else {
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                        //
                        // NODATA
                        //
                        // checks with nodata
                        for (int b = 0; b < dstBands; b++) {

                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            // and check if every kernel pixel is a No Data
                            for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                                for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                    // Selection of one pixel
                                    int sample = iterSource.getSample(xint + (i - 1), yint
                                            + (j - 1), b);
                                    pixelKernel[j][i] = sample;
                                    if (noDataRange.contains((short) sample)) {
                                        weight &= (0xffff - (1 << KERNEL_LINE_DIM * j + i));
                                        // weigjtArray[j][z] = 0;
                                    } else {
                                        // weigjtArray[j][z] = 1;
                                        weight |= (1 << (KERNEL_LINE_DIM * j + i));
                                    }
                                }
                                // Data elaboration for each line
                                temp = (byte) ((weight >> KERNEL_LINE_DIM * j) & 0x0F);
                                tempData = bicubicInpainting(pixelKernel[j], temp, emptyArray);

                                tempSum = tempData[0] * dataHi[offsetX] + tempData[1]
                                        * dataHi[offsetX + 1] + tempData[2] * dataHi[offsetX + 2]
                                        + tempData[3] * dataHi[offsetX + 3];

                                if (temp > 0) {
                                    weightVert |= (1 << j);
                                    // weigjtArrayVertical[j] = 1;
                                } else {
                                    weightVert &= (0x0F - (1 << j));
                                    // weigjtArrayVertical[j] = 0;
                                }
                                sumArray[j] = ((tempSum + round) >> precisionBits);
                            }

                            // Control if the 16 pixel are all No Data
                            if (weight == 0) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            } else {

                                tempData = bicubicInpainting(sumArray, weightVert, emptyArray);

                                // Vertical sum update
                                sum = tempData[0] * dataVi[offsetY] + tempData[1]
                                        * dataVi[offsetY + 1] + tempData[2] * dataVi[offsetY + 2]
                                        + tempData[3] * dataVi[offsetY + 3];

                                // Interpolation
                                result = ((sum + round) >> precisionBits);
                                weight = 0;
                                weightVert = 0;
                                sum = 0;

                                // Clamp
                                if (result > Short.MAX_VALUE) {
                                    result = Short.MAX_VALUE;
                                } else if (result < Short.MIN_VALUE) {
                                    result = Short.MIN_VALUE;
                                }

                                data[b][pixelOffset + bandOffsets[b]] = (short) (result);
                            }
                        }
                    }

                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            } // ROWS LOOP
              // BOTH ROI AND NODATA
        } else {
            // Array used during calculations
            final long[][] pixelKernel = new long[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
            long[] sumArray = new long[KERNEL_LINE_DIM];
            long[] emptyArray = new long[KERNEL_LINE_DIM];
            long[] tempData;

            // Value used in calculations
            short weight = 0;
            byte weightVert = 0;
            byte temp = 0;

            // Row temporary sum initialization
            long tempSum = 0;
            long sum = 0;
            // final result initialization
            long result = 0;
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                    } else {
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                        //
                        // ROI
                        //
                        // checks with roi
                        // Initialization of the flag indicating that all the kernel pixels are inside the ROI
                        boolean inRoi = false;

                        for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                            for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                inRoi |= roiTile.contains(xint + (i - 1), yint + (j - 1));
                            }
                        }
                        if (inRoi) {
                            //
                            // NODATA
                            //
                            // checks with nodata
                            for (int b = 0; b < dstBands; b++) {

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and check if every kernel pixel is a No Data
                                for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                                    for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                        // Selection of one pixel
                                        int sample = iterSource.getSample(xint + (i - 1), yint
                                                + (j - 1), b);
                                        pixelKernel[j][i] = sample;
                                        if (noDataRange.contains((short) sample)) {
                                            weight &= (0xffff - (1 << KERNEL_LINE_DIM * j + i));
                                            // weigjtArray[j][z] = 0;
                                        } else {
                                            // weigjtArray[j][z] = 1;
                                            weight |= (1 << (KERNEL_LINE_DIM * j + i));
                                        }
                                    }
                                    // Data elaboration for each line
                                    temp = (byte) ((weight >> KERNEL_LINE_DIM * j) & 0x0F);
                                    tempData = bicubicInpainting(pixelKernel[j], temp, emptyArray);

                                    tempSum = tempData[0] * dataHi[offsetX] + tempData[1]
                                            * dataHi[offsetX + 1] + tempData[2]
                                            * dataHi[offsetX + 2] + tempData[3]
                                            * dataHi[offsetX + 3];

                                    if (temp > 0) {
                                        weightVert |= (1 << j);
                                        // weigjtArrayVertical[j] = 1;
                                    } else {
                                        weightVert &= (0x0F - (1 << j));
                                        // weigjtArrayVertical[j] = 0;
                                    }
                                    sumArray[j] = ((tempSum + round) >> precisionBits);
                                }

                                // Control if the 16 pixel are all No Data
                                if (weight == 0) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                                } else {

                                    tempData = bicubicInpainting(sumArray, weightVert, emptyArray);

                                    // Vertical sum update
                                    sum = tempData[0] * dataVi[offsetY] + tempData[1]
                                            * dataVi[offsetY + 1] + tempData[2]
                                            * dataVi[offsetY + 2] + tempData[3]
                                            * dataVi[offsetY + 3];

                                    // Interpolation
                                    result = ((sum + round) >> precisionBits);
                                    weight = 0;
                                    weightVert = 0;
                                    sum = 0;

                                    // Clamp
                                    if (result > Short.MAX_VALUE) {
                                        result = Short.MAX_VALUE;
                                    } else if (result < Short.MIN_VALUE) {
                                        result = Short.MIN_VALUE;
                                    }

                                    data[b][pixelOffset + bandOffsets[b]] = (short) (result);
                                }
                            }
                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
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

    protected void computeRectInt(final PlanarImage src, final RasterAccessor dst, final ROI roiTile) {
        // Random Iterator initialization, taking into account the presence of the borderExtender
        RandomIter iterSource;
        final int minX, maxX, minY, maxY;
        if (extended) {
            final Rectangle bounds = new Rectangle(src.getMinX() - 1, src.getMinY() - 1,
                    src.getWidth() + 3, src.getHeight() + 3);
            iterSource = RandomIterFactory.create(src.getExtendedData(bounds, extender), bounds,
                    TILE_CACHED, ARRAY_CALC);

            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();

        } else {
            iterSource = RandomIterFactory.create(src, src.getBounds(), TILE_CACHED, ARRAY_CALC);

            minX = src.getMinX() + 1; // Left padding
            maxX = src.getMaxX() - 2; // Right padding
            minY = src.getMinY() + 1; // Top padding
            maxY = src.getMaxY() - 2; // Bottom padding
        }

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int lineStride = dst.getScanlineStride();
        final int pixelStride = dst.getPixelStride();
        final int[] bandOffsets = dst.getBandOffsets();
        final int[][] data = dst.getIntDataArrays();

        final float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

        // source does not have IndexColorModel
        // ONLY VALID DATA
        if (caseA) {
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataInt;
                            }
                        }
                    } else {
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                        //
                        // NO ROI
                        //
                        for (int b = 0; b < dstBands; b++) {
                            // Interpolation
                            long result = bicubicCalculationInt(b, iterSource, xint, yint, offsetX,
                                    offsetY, null);

                            // Clamp
                            if (result > Integer.MAX_VALUE) {
                                result = Integer.MAX_VALUE;
                            } else if (result < Integer.MIN_VALUE) {
                                result = Integer.MIN_VALUE;
                            }

                            data[b][pixelOffset + bandOffsets[b]] = (int) (result);
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataInt;
                            }
                        }
                    } else {
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                        //
                        // ROI
                        //
                        // checks with roi
                        // Initialization of the flag indicating that all the kernel pixels are inside the ROI
                        boolean inRoi = false;

                        for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                            for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                inRoi |= roiTile.contains(xint + (i - 1), yint + (j - 1));
                            }
                        }
                        if (inRoi) {

                            for (int b = 0; b < dstBands; b++) {
                                // Interpolation
                                long result = bicubicCalculationInt(b, iterSource, xint, yint,
                                        offsetX, offsetY, null);

                                // Clamp
                                if (result > Integer.MAX_VALUE) {
                                    result = Integer.MAX_VALUE;
                                } else if (result < Integer.MIN_VALUE) {
                                    result = Integer.MIN_VALUE;
                                }

                                data[b][pixelOffset + bandOffsets[b]] = (int) (result);
                            }
                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataInt;
                            }
                        }
                    }

                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            } // ROWS LOOP
              // ONLY NODATA
        } else if (caseC) {
            // Array used during calculations
            final long[][] pixelKernel = new long[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
            long[] sumArray = new long[KERNEL_LINE_DIM];
            long[] emptyArray = new long[KERNEL_LINE_DIM];
            long[] tempData;

            // Value used in calculations
            short weight = 0;
            byte weightVert = 0;
            byte temp = 0;

            // Row temporary sum initialization
            long tempSum = 0;
            long sum = 0;
            // final result initialization
            long result = 0;

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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataInt;
                            }
                        }
                    } else {
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                        //
                        // NODATA
                        //
                        // checks with nodata
                        for (int b = 0; b < dstBands; b++) {

                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            // and check if every kernel pixel is a No Data
                            for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                                for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                    // Selection of one pixel
                                    int sample = iterSource.getSample(xint + (i - 1), yint
                                            + (j - 1), b);
                                    pixelKernel[j][i] = sample;
                                    if (noDataRange.contains(sample)) {
                                        weight &= (0xffff - (1 << KERNEL_LINE_DIM * j + i));
                                        // weigjtArray[j][z] = 0;
                                    } else {
                                        // weigjtArray[j][z] = 1;
                                        weight |= (1 << (KERNEL_LINE_DIM * j + i));
                                    }
                                }
                                // Data elaboration for each line
                                temp = (byte) ((weight >> KERNEL_LINE_DIM * j) & 0x0F);
                                tempData = bicubicInpainting(pixelKernel[j], temp, emptyArray);

                                tempSum = tempData[0] * dataHi[offsetX] + tempData[1]
                                        * dataHi[offsetX + 1] + tempData[2] * dataHi[offsetX + 2]
                                        + tempData[3] * dataHi[offsetX + 3];

                                if (temp > 0) {
                                    weightVert |= (1 << j);
                                    // weigjtArrayVertical[j] = 1;
                                } else {
                                    weightVert &= (0x0F - (1 << j));
                                    // weigjtArrayVertical[j] = 0;
                                }
                                sumArray[j] = ((tempSum + round) >> precisionBits);
                            }

                            // Control if the 16 pixel are all No Data
                            if (weight == 0) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataInt;
                            } else {

                                tempData = bicubicInpainting(sumArray, weightVert, emptyArray);

                                // Vertical sum update
                                sum = tempData[0] * dataVi[offsetY] + tempData[1]
                                        * dataVi[offsetY + 1] + tempData[2] * dataVi[offsetY + 2]
                                        + tempData[3] * dataVi[offsetY + 3];

                                // Interpolation
                                result = ((sum + round) >> precisionBits);
                                weight = 0;
                                weightVert = 0;
                                sum = 0;

                                // Clamp
                                if (result > Integer.MAX_VALUE) {
                                    result = Integer.MAX_VALUE;
                                } else if (result < Integer.MIN_VALUE) {
                                    result = Integer.MIN_VALUE;
                                }

                                data[b][pixelOffset + bandOffsets[b]] = (int) (result);
                            }
                        }
                    }

                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            } // ROWS LOOP
              // BOTH ROI AND NODATA
        } else {
            // Array used during calculations
            final long[][] pixelKernel = new long[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
            long[] sumArray = new long[KERNEL_LINE_DIM];
            long[] emptyArray = new long[KERNEL_LINE_DIM];
            long[] tempData;

            // Value used in calculations
            short weight = 0;
            byte weightVert = 0;
            byte temp = 0;

            // Row temporary sum initialization
            long tempSum = 0;
            long sum = 0;
            // final result initialization
            long result = 0;
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataInt;
                            }
                        }
                    } else {
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                        //
                        // ROI
                        //
                        // checks with roi
                        // Initialization of the flag indicating that all the kernel pixels are inside the ROI
                        boolean inRoi = false;

                        for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                            for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                inRoi |= roiTile.contains(xint + (i - 1), yint + (j - 1));
                            }
                        }
                        if (inRoi) {
                            //
                            // NODATA
                            //
                            // checks with nodata
                            for (int b = 0; b < dstBands; b++) {

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and check if every kernel pixel is a No Data
                                for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                                    for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                        // Selection of one pixel
                                        int sample = iterSource.getSample(xint + (i - 1), yint
                                                + (j - 1), b);
                                        pixelKernel[j][i] = sample;
                                        if (noDataRange.contains(sample)) {
                                            weight &= (0xffff - (1 << KERNEL_LINE_DIM * j + i));
                                            // weigjtArray[j][z] = 0;
                                        } else {
                                            // weigjtArray[j][z] = 1;
                                            weight |= (1 << (KERNEL_LINE_DIM * j + i));
                                        }
                                    }
                                    // Data elaboration for each line
                                    temp = (byte) ((weight >> KERNEL_LINE_DIM * j) & 0x0F);
                                    tempData = bicubicInpainting(pixelKernel[j], temp, emptyArray);

                                    tempSum = tempData[0] * dataHi[offsetX] + tempData[1]
                                            * dataHi[offsetX + 1] + tempData[2]
                                            * dataHi[offsetX + 2] + tempData[3]
                                            * dataHi[offsetX + 3];

                                    if (temp > 0) {
                                        weightVert |= (1 << j);
                                        // weigjtArrayVertical[j] = 1;
                                    } else {
                                        weightVert &= (0x0F - (1 << j));
                                        // weigjtArrayVertical[j] = 0;
                                    }
                                    sumArray[j] = ((tempSum + round) >> precisionBits);
                                }

                                // Control if the 16 pixel are all No Data
                                if (weight == 0) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataInt;
                                } else {

                                    tempData = bicubicInpainting(sumArray, weightVert, emptyArray);

                                    // Vertical sum update
                                    sum = tempData[0] * dataVi[offsetY] + tempData[1]
                                            * dataVi[offsetY + 1] + tempData[2]
                                            * dataVi[offsetY + 2] + tempData[3]
                                            * dataVi[offsetY + 3];

                                    // Interpolation
                                    result = ((sum + round) >> precisionBits);
                                    weight = 0;
                                    weightVert = 0;
                                    sum = 0;

                                    // Clamp
                                    if (result > Integer.MAX_VALUE) {
                                        result = Integer.MAX_VALUE;
                                    } else if (result < Integer.MIN_VALUE) {
                                        result = Integer.MIN_VALUE;
                                    }

                                    data[b][pixelOffset + bandOffsets[b]] = (int) (result);
                                }
                            }
                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataInt;
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
            final ROI roiTile) {
        // Random Iterator initialization, taking into account the presence of the borderExtender
        RandomIter iterSource;
        final int minX, maxX, minY, maxY;
        if (extended) {
            final Rectangle bounds = new Rectangle(src.getMinX() - 1, src.getMinY() - 1,
                    src.getWidth() + 3, src.getHeight() + 3);
            iterSource = RandomIterFactory.create(src.getExtendedData(bounds, extender), bounds,
                    TILE_CACHED, ARRAY_CALC);

            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();

        } else {
            iterSource = RandomIterFactory.create(src, src.getBounds(), TILE_CACHED, ARRAY_CALC);

            minX = src.getMinX() + 1; // Left padding
            maxX = src.getMaxX() - 2; // Right padding
            minY = src.getMinY() + 1; // Top padding
            maxY = src.getMaxY() - 2; // Bottom padding
        }

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int lineStride = dst.getScanlineStride();
        final int pixelStride = dst.getPixelStride();
        final int[] bandOffsets = dst.getBandOffsets();
        final float[][] data = dst.getFloatDataArrays();

        final float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

        // source does not have IndexColorModel
        // ONLY VALID DATA
        if (caseA) {
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataFloat;
                            }
                        }
                    } else {
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                        //
                        // NO ROI
                        //
                        for (int b = 0; b < dstBands; b++) {
                            // Interpolation
                            double result = bicubicCalculationFloat(b, iterSource, xint, yint,
                                    offsetX, offsetY);

                            // Clamp
                            if (result > Float.MAX_VALUE) {
                                result = Float.MAX_VALUE;
                            } else if (result < -Float.MAX_VALUE) {
                                result = -Float.MAX_VALUE;
                            }

                            data[b][pixelOffset + bandOffsets[b]] = (float) (result);
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataFloat;
                            }
                        }
                    } else {
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                        //
                        // ROI
                        //
                        // checks with roi
                        // Initialization of the flag indicating that all the kernel pixels are inside the ROI
                        boolean inRoi = false;

                        for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                            for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                inRoi |= roiTile.contains(xint + (i - 1), yint + (j - 1));
                            }
                        }
                        if (inRoi) {

                            for (int b = 0; b < dstBands; b++) {
                                // Interpolation
                                double result = bicubicCalculationFloat(b, iterSource, xint, yint,
                                        offsetX, offsetY);

                                // Clamp
                                if (result > Float.MAX_VALUE) {
                                    result = Float.MAX_VALUE;
                                } else if (result < -Float.MAX_VALUE) {
                                    result = -Float.MAX_VALUE;
                                }

                                data[b][pixelOffset + bandOffsets[b]] = (float) (result);
                            }
                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataFloat;
                            }
                        }
                    }

                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            } // ROWS LOOP
              // ONLY NODATA
        } else if (caseC) {
            // Array used during calculations
            final double[][] pixelKernel = new double[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
            double[] sumArray = new double[KERNEL_LINE_DIM];
            double[] emptyArray = new double[KERNEL_LINE_DIM];
            double[] tempData;

            // Value used in calculations
            short weight = 0;
            byte weightVert = 0;
            byte temp = 0;

            // Row temporary sum initialization
            double tempSum = 0;
            double sum = 0;

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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataFloat;
                            }
                        }
                    } else {
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                        //
                        // NODATA
                        //
                        // checks with nodata
                        for (int b = 0; b < dstBands; b++) {

                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            // and check if every kernel pixel is a No Data
                            for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                                for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                    // Selection of one pixel
                                    float sample = iterSource.getSampleFloat(xint + (i - 1), yint
                                            + (j - 1), b);
                                    pixelKernel[j][i] = sample;
                                    if (noDataRange.contains(sample)) {
                                        weight &= (0xffff - (1 << KERNEL_LINE_DIM * j + i));
                                        // weigjtArray[j][z] = 0;
                                    } else {
                                        // weigjtArray[j][z] = 1;
                                        weight |= (1 << (KERNEL_LINE_DIM * j + i));
                                    }
                                }
                                // Data elaboration for each line
                                temp = (byte) ((weight >> KERNEL_LINE_DIM * j) & 0x0F);
                                tempData = bicubicInpaintingDouble(pixelKernel[j], temp, emptyArray);

                                tempSum = tempData[0] * dataHf[offsetX] + tempData[1]
                                        * dataHf[offsetX + 1] + tempData[2] * dataHf[offsetX + 2]
                                        + tempData[3] * dataHf[offsetX + 3];

                                if (temp > 0) {
                                    weightVert |= (1 << j);
                                    // weigjtArrayVertical[j] = 1;
                                } else {
                                    weightVert &= (0x0F - (1 << j));
                                    // weigjtArrayVertical[j] = 0;
                                }
                                sumArray[j] = tempSum;
                            }

                            // Control if the 16 pixel are all No Data
                            if (weight == 0) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataFloat;
                            } else {

                                tempData = bicubicInpaintingDouble(sumArray, weightVert, emptyArray);

                                // Vertical sum update
                                sum = tempData[0] * dataVf[offsetY] + tempData[1]
                                        * dataVf[offsetY + 1] + tempData[2] * dataVf[offsetY + 2]
                                        + tempData[3] * dataVf[offsetY + 3];

                                // Interpolation
                                weight = 0;
                                weightVert = 0;

                                // Clamp
                                if (sum > Float.MAX_VALUE) {
                                    sum = Float.MAX_VALUE;
                                } else if (sum < -Float.MAX_VALUE) {
                                    sum = -Float.MAX_VALUE;
                                }

                                data[b][pixelOffset + bandOffsets[b]] = (float) sum;
                            }
                        }
                    }

                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            } // ROWS LOOP
              // BOTH ROI AND NODATA
        } else {
            // Array used during calculations
            final double[][] pixelKernel = new double[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
            double[] sumArray = new double[KERNEL_LINE_DIM];
            double[] emptyArray = new double[KERNEL_LINE_DIM];
            double[] tempData;

            // Value used in calculations
            short weight = 0;
            byte weightVert = 0;
            byte temp = 0;

            // Row temporary sum initialization
            double tempSum = 0;
            double sum = 0;
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataFloat;
                            }
                        }
                    } else {
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                        //
                        // ROI
                        //
                        // checks with roi
                        // Initialization of the flag indicating that all the kernel pixels are inside the ROI
                        boolean inRoi = false;

                        for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                            for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                inRoi |= roiTile.contains(xint + (i - 1), yint + (j - 1));
                            }
                        }
                        if (inRoi) {
                            //
                            // NODATA
                            //
                            // checks with nodata
                            for (int b = 0; b < dstBands; b++) {

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and check if every kernel pixel is a No Data
                                for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                                    for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                        // Selection of one pixel
                                        float sample = iterSource.getSampleFloat(xint + (i - 1),
                                                yint + (j - 1), b);
                                        pixelKernel[j][i] = sample;
                                        if (noDataRange.contains(sample)) {
                                            weight &= (0xffff - (1 << KERNEL_LINE_DIM * j + i));
                                            // weigjtArray[j][z] = 0;
                                        } else {
                                            // weigjtArray[j][z] = 1;
                                            weight |= (1 << (KERNEL_LINE_DIM * j + i));
                                        }
                                    }
                                    // Data elaboration for each line
                                    temp = (byte) ((weight >> KERNEL_LINE_DIM * j) & 0x0F);
                                    tempData = bicubicInpaintingDouble(pixelKernel[j], temp,
                                            emptyArray);

                                    tempSum = tempData[0] * dataHf[offsetX] + tempData[1]
                                            * dataHf[offsetX + 1] + tempData[2]
                                            * dataHf[offsetX + 2] + tempData[3]
                                            * dataHf[offsetX + 3];

                                    if (temp > 0) {
                                        weightVert |= (1 << j);
                                        // weigjtArrayVertical[j] = 1;
                                    } else {
                                        weightVert &= (0x0F - (1 << j));
                                        // weigjtArrayVertical[j] = 0;
                                    }
                                    sumArray[j] = tempSum;
                                }

                                // Control if the 16 pixel are all No Data
                                if (weight == 0) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataFloat;
                                } else {

                                    tempData = bicubicInpaintingDouble(sumArray, weightVert,
                                            emptyArray);

                                    // Vertical sum update
                                    sum = tempData[0] * dataVf[offsetY] + tempData[1]
                                            * dataVf[offsetY + 1] + tempData[2]
                                            * dataVf[offsetY + 2] + tempData[3]
                                            * dataVf[offsetY + 3];

                                    // Interpolation
                                    weight = 0;
                                    weightVert = 0;

                                    // Clamp
                                    if (sum > Float.MAX_VALUE) {
                                        sum = Float.MAX_VALUE;
                                    } else if (sum < -Float.MAX_VALUE) {
                                        sum = -Float.MAX_VALUE;
                                    }

                                    data[b][pixelOffset + bandOffsets[b]] = (float) sum;
                                }
                            }
                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataFloat;
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
            final ROI roiTile) {
        // Random Iterator initialization, taking into account the presence of the borderExtender
        RandomIter iterSource;
        final int minX, maxX, minY, maxY;
        if (extended) {
            final Rectangle bounds = new Rectangle(src.getMinX() - 1, src.getMinY() - 1,
                    src.getWidth() + 3, src.getHeight() + 3);
            iterSource = RandomIterFactory.create(src.getExtendedData(bounds, extender), bounds,
                    TILE_CACHED, ARRAY_CALC);

            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();

        } else {
            iterSource = RandomIterFactory.create(src, src.getBounds(), TILE_CACHED, ARRAY_CALC);

            minX = src.getMinX() + 1; // Left padding
            maxX = src.getMaxX() - 2; // Right padding
            minY = src.getMinY() + 1; // Top padding
            maxY = src.getMaxY() - 2; // Bottom padding
        }

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int lineStride = dst.getScanlineStride();
        final int pixelStride = dst.getPixelStride();
        final int[] bandOffsets = dst.getBandOffsets();
        final double[][] data = dst.getDoubleDataArrays();

        final float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

        // source does not have IndexColorModel
        // ONLY VALID DATA
        if (caseA) {
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataDouble;
                            }
                        }
                    } else {
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                        //
                        // NO ROI
                        //
                        for (int b = 0; b < dstBands; b++) {
                            // Interpolation
                            data[b][pixelOffset + bandOffsets[b]] = bicubicCalculationDouble(b,
                                    iterSource, xint, yint, offsetX, offsetY);
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataDouble;
                            }
                        }
                    } else {
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                        //
                        // ROI
                        //
                        // checks with roi
                        // Initialization of the flag indicating that all the kernel pixels are inside the ROI
                        boolean inRoi = false;

                        for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                            for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                inRoi |= roiTile.contains(xint + (i - 1), yint + (j - 1));
                            }
                        }
                        if (inRoi) {

                            for (int b = 0; b < dstBands; b++) {
                                // Interpolation
                                data[b][pixelOffset + bandOffsets[b]] = bicubicCalculationDouble(b,
                                        iterSource, xint, yint, offsetX, offsetY);
                            }
                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataDouble;
                            }
                        }
                    }

                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            } // ROWS LOOP
              // ONLY NODATA
        } else if (caseC) {
            // Array used during calculations
            final double[][] pixelKernel = new double[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
            double[] sumArray = new double[KERNEL_LINE_DIM];
            double[] emptyArray = new double[KERNEL_LINE_DIM];
            double[] tempData;

            // Value used in calculations
            short weight = 0;
            byte weightVert = 0;
            byte temp = 0;

            // Row temporary sum initialization
            double tempSum = 0;
            double sum = 0;

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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataDouble;
                            }
                        }
                    } else {
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                        //
                        // NODATA
                        //
                        // checks with nodata
                        for (int b = 0; b < dstBands; b++) {

                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            // and check if every kernel pixel is a No Data
                            for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                                for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                    // Selection of one pixel
                                    double sample = iterSource.getSampleDouble(xint + (i - 1), yint
                                            + (j - 1), b);
                                    pixelKernel[j][i] = sample;
                                    if (noDataRange.contains(sample)) {
                                        weight &= (0xffff - (1 << KERNEL_LINE_DIM * j + i));
                                        // weigjtArray[j][z] = 0;
                                    } else {
                                        // weigjtArray[j][z] = 1;
                                        weight |= (1 << (KERNEL_LINE_DIM * j + i));
                                    }
                                }
                                // Data elaboration for each line
                                temp = (byte) ((weight >> KERNEL_LINE_DIM * j) & 0x0F);
                                tempData = bicubicInpaintingDouble(pixelKernel[j], temp, emptyArray);

                                tempSum = tempData[0] * dataHd[offsetX] + tempData[1]
                                        * dataHd[offsetX + 1] + tempData[2] * dataHd[offsetX + 2]
                                        + tempData[3] * dataHd[offsetX + 3];

                                if (temp > 0) {
                                    weightVert |= (1 << j);
                                    // weigjtArrayVertical[j] = 1;
                                } else {
                                    weightVert &= (0x0F - (1 << j));
                                    // weigjtArrayVertical[j] = 0;
                                }
                                sumArray[j] = tempSum;
                            }

                            // Control if the 16 pixel are all No Data
                            if (weight == 0) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataDouble;
                            } else {

                                tempData = bicubicInpaintingDouble(sumArray, weightVert, emptyArray);

                                // Vertical sum update
                                sum = tempData[0] * dataVd[offsetY] + tempData[1]
                                        * dataVd[offsetY + 1] + tempData[2] * dataVd[offsetY + 2]
                                        + tempData[3] * dataVd[offsetY + 3];

                                // Interpolation
                                weight = 0;
                                weightVert = 0;
                                data[b][pixelOffset + bandOffsets[b]] = (float) sum;
                            }
                        }
                    }

                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            } // ROWS LOOP
              // BOTH ROI AND NODATA
        } else {
            // Array used during calculations
            final double[][] pixelKernel = new double[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
            double[] sumArray = new double[KERNEL_LINE_DIM];
            double[] emptyArray = new double[KERNEL_LINE_DIM];
            double[] tempData;

            // Value used in calculations
            short weight = 0;
            byte weightVert = 0;
            byte temp = 0;

            // Row temporary sum initialization
            double tempSum = 0;
            double sum = 0;
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataDouble;
                            }
                        }
                    } else {
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * xfrac);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * yfrac);
                        //
                        // ROI
                        //
                        // checks with roi
                        // Initialization of the flag indicating that all the kernel pixels are inside the ROI
                        boolean inRoi = false;

                        for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                            for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                inRoi |= roiTile.contains(xint + (i - 1), yint + (j - 1));
                            }
                        }
                        if (inRoi) {
                            //
                            // NODATA
                            //
                            // checks with nodata
                            for (int b = 0; b < dstBands; b++) {

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and check if every kernel pixel is a No Data
                                for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                                    for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                        // Selection of one pixel
                                        double sample = iterSource.getSampleDouble(xint + (i - 1),
                                                yint + (j - 1), b);
                                        pixelKernel[j][i] = sample;
                                        if (noDataRange.contains(sample)) {
                                            weight &= (0xffff - (1 << KERNEL_LINE_DIM * j + i));
                                            // weigjtArray[j][z] = 0;
                                        } else {
                                            // weigjtArray[j][z] = 1;
                                            weight |= (1 << (KERNEL_LINE_DIM * j + i));
                                        }
                                    }
                                    // Data elaboration for each line
                                    temp = (byte) ((weight >> KERNEL_LINE_DIM * j) & 0x0F);
                                    tempData = bicubicInpaintingDouble(pixelKernel[j], temp,
                                            emptyArray);

                                    tempSum = tempData[0] * dataHd[offsetX] + tempData[1]
                                            * dataHd[offsetX + 1] + tempData[2]
                                            * dataHd[offsetX + 2] + tempData[3]
                                            * dataHd[offsetX + 3];

                                    if (temp > 0) {
                                        weightVert |= (1 << j);
                                        // weigjtArrayVertical[j] = 1;
                                    } else {
                                        weightVert &= (0x0F - (1 << j));
                                        // weigjtArrayVertical[j] = 0;
                                    }
                                    sumArray[j] = tempSum;
                                }

                                // Control if the 16 pixel are all No Data
                                if (weight == 0) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataDouble;
                                } else {

                                    tempData = bicubicInpaintingDouble(sumArray, weightVert,
                                            emptyArray);

                                    // Vertical sum update
                                    sum = tempData[0] * dataVd[offsetY] + tempData[1]
                                            * dataVd[offsetY + 1] + tempData[2]
                                            * dataVd[offsetY + 2] + tempData[3]
                                            * dataVd[offsetY + 3];

                                    // Interpolation
                                    weight = 0;
                                    weightVert = 0;
                                    data[b][pixelOffset + bandOffsets[b]] = sum;
                                }
                            }
                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataDouble;
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

    /**
     * Bicubic calculation for integer data
     * 
     * @param b band
     * @param iterSource source image iterator
     * @param xint source pixel X position
     * @param yint source pixel Y position
     * @param offsetX X fractional offset
     * @param offsetY Y fractional offset
     * @param t optional color table
     * @return
     */
    private long bicubicCalculationInt(int b, RandomIter iterSource, int xint, int yint,
            int offsetX, int offsetY, byte[] t) {

        // Temporary sum initialization
        long sum = 0;
        if (t == null) {
            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
            for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                // Row temporary sum initialization
                long temp = 0;
                for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                    // Selection of one pixel
                    int pixelValue = iterSource.getSample(xint + (i - 1), yint + (j - 1), b);
                    // Update of the temporary sum
                    temp += (pixelValue * dataHi[offsetX + i]);
                }
                // Vertical sum update
                sum += ((temp + round) >> precisionBits) * dataVi[offsetY + j];
            }
        } else {
            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
            for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                // Row temporary sum initialization
                long temp = 0;
                for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                    // Selection of one pixel
                    int pixelValue = t[iterSource.getSample(xint + (i - 1), yint + (j - 1), 0) & 0xFF] & 0xFF;
                    // Update of the temporary sum
                    temp += (pixelValue * dataHi[offsetX + i]);
                }
                // Vertical sum update
                sum += ((temp + round) >> precisionBits) * dataVi[offsetY + j];
            }
        }
        // Interpolation
        return ((sum + round) >> precisionBits);
    }

    /**
     * Bicubic calculation for Float data
     * 
     * @param b band
     * @param iterSource source image iterator
     * @param xint source pixel X position
     * @param yint source pixel Y position
     * @param offsetX X fractional offset
     * @param offsetY Y fractional offset
     * @return
     */
    private double bicubicCalculationFloat(int b, RandomIter iterSource, int xint, int yint,
            int offsetX, int offsetY) {

        // Temporary sum initialization
        double sum = 0;
        // Cycle through all the 16 kernel pixel and calculation of the interpolated value
        for (int j = 0; j < KERNEL_LINE_DIM; j++) {
            // Row temporary sum initialization
            double temp = 0;
            for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                // Selection of one pixel
                float pixelValue = iterSource.getSampleFloat(xint + (i - 1), yint + (j - 1), b);
                // Update of the temporary sum
                temp += (pixelValue * dataHf[offsetX + i]);
            }
            // Vertical sum update
            sum += temp * dataVf[offsetY + j];
        }
        // Interpolation
        return sum;
    }

    /**
     * Bicubic calculation for Double data
     * 
     * @param b band
     * @param iterSource source image iterator
     * @param xint source pixel X position
     * @param yint source pixel Y position
     * @param offsetX X fractional offset
     * @param offsetY Y fractional offset
     * @return
     */
    private double bicubicCalculationDouble(int b, RandomIter iterSource, int xint, int yint,
            int offsetX, int offsetY) {

        // Temporary sum initialization
        double sum = 0;
        // Cycle through all the 16 kernel pixel and calculation of the interpolated value
        for (int j = 0; j < KERNEL_LINE_DIM; j++) {
            // Row temporary sum initialization
            double temp = 0;
            for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                // Selection of one pixel
                double pixelValue = iterSource.getSampleDouble(xint + (i - 1), yint + (j - 1), b);
                // Update of the temporary sum
                temp += (pixelValue * dataHd[offsetX + i]);
            }
            // Vertical sum update
            sum += temp * dataVd[offsetY + j];
        }
        // Interpolation
        return sum;
    }

    /**
     * Private method for extracting valid values from the input pixel no data values on a 4-pixel line This method is used for filling the no data
     * values inside the interpolation kernel with the values of the adjacent pixels
     * 
     * @param array pixel array
     * @param weightSum value indicating how and where no data pixels are
     * @param emptyArray empty array to return in output
     * @return
     */
    private static long[] bicubicInpainting(long[] array, short weightSum, long[] emptyArray) {
        // Absence of No Data, the pixels are returned.
        if (weightSum == 15) {
            return array;
        }

        long s_ = array[0];
        long s0 = array[1];
        long s1 = array[2];
        long s2 = array[3];

        emptyArray[0] = 0;
        emptyArray[1] = 0;
        emptyArray[2] = 0;
        emptyArray[3] = 0;

        // s2 s1 s0 s_
        // 0/x 0/x 0/x 0/x

        switch (weightSum) {
        case 0:
            // 0 0 0 0
            break;
        case 1:
            // 0 0 0 x
            emptyArray[0] = s_;
            emptyArray[1] = s_;
            emptyArray[2] = s_;
            emptyArray[3] = s_;
            break;
        case 2:
            // 0 0 x 0
            emptyArray[0] = s0;
            emptyArray[1] = s0;
            emptyArray[2] = s0;
            emptyArray[3] = s0;
            break;
        case 3:
            // 0 0 x x
            emptyArray[0] = s_;
            emptyArray[1] = s0;
            emptyArray[2] = s0;
            emptyArray[3] = s0;
            break;
        case 4:
            // 0 x 0 0
            emptyArray[0] = s1;
            emptyArray[1] = s1;
            emptyArray[2] = s1;
            emptyArray[3] = s1;
            break;
        case 5:
            // 0 x 0 x
            emptyArray[0] = s_;
            emptyArray[1] = (s_ + s1) / 2;
            emptyArray[2] = s1;
            emptyArray[3] = s1;
            break;
        case 6:
            // 0 x x 0
            emptyArray[0] = s0;
            emptyArray[1] = s0;
            emptyArray[2] = s1;
            emptyArray[3] = s1;
            break;
        case 7:
            // 0 x x x
            emptyArray[0] = s_;
            emptyArray[1] = s0;
            emptyArray[2] = s1;
            emptyArray[3] = s1;
            break;
        case 8:
            // x 0 0 0
            emptyArray[0] = s2;
            emptyArray[1] = s2;
            emptyArray[2] = s2;
            emptyArray[3] = s2;
            break;
        case 9:
            // x 0 0 x
            emptyArray[0] = s_;
            emptyArray[1] = (s_ + s2) / 2;
            emptyArray[2] = (s_ + s2) / 2;
            emptyArray[3] = s2;
            break;
        case 10:
            // x 0 x 0
            emptyArray[0] = s0;
            emptyArray[1] = s0;
            emptyArray[2] = (s0 + s2) / 2;
            emptyArray[3] = s2;
            break;
        case 11:
            // x 0 x x
            emptyArray[0] = s_;
            emptyArray[1] = s0;
            emptyArray[2] = (s0 + s2) / 2;
            emptyArray[3] = s2;
            break;
        case 12:
            // x x 0 0
            emptyArray[0] = s1;
            emptyArray[1] = s1;
            emptyArray[2] = s1;
            emptyArray[3] = s2;
            break;
        case 13:
            // x x 0 x
            emptyArray[0] = s_;
            emptyArray[1] = (s_ + s1) / 2;
            emptyArray[2] = s1;
            emptyArray[3] = s2;
            break;
        case 14:
            // x x x 0
            emptyArray[0] = s0;
            emptyArray[1] = s0;
            emptyArray[2] = s1;
            emptyArray[3] = s2;
            break;
        default:
            throw new IllegalArgumentException("Array cannot be composed from more than 4 elements");
        }

        return emptyArray;
    }

    /**
     * Private method for extracting valid values from the input pixel no data values on a 4-pixel line This method is used for filling the no data
     * values inside the interpolation kernel with the values of the adjacent pixels
     * 
     * @param array pixel array
     * @param weightSum value indicating how and where no data pixels are
     * @param emptyArray empty array to return in output
     * @return
     */
    private static double[] bicubicInpaintingDouble(double[] array, short weightSum,
            double[] emptyArray) {
        // Absence of No Data, the pixels are returned.
        if (weightSum == 15) {
            return array;
        }

        double s_ = array[0];
        double s0 = array[1];
        double s1 = array[2];
        double s2 = array[3];

        emptyArray[0] = 0;
        emptyArray[1] = 0;
        emptyArray[2] = 0;
        emptyArray[3] = 0;

        switch (weightSum) {
        case 0:
            // 0 0 0 0
            break;
        case 1:
            // 0 0 0 x
            emptyArray[0] = s_;
            emptyArray[1] = s_;
            emptyArray[2] = s_;
            emptyArray[3] = s_;
            break;
        case 2:
            // 0 0 x 0
            emptyArray[0] = s0;
            emptyArray[1] = s0;
            emptyArray[2] = s0;
            emptyArray[3] = s0;
            break;
        case 3:
            // 0 0 x x
            emptyArray[0] = s_;
            emptyArray[1] = s0;
            emptyArray[2] = s0;
            emptyArray[3] = s0;
            break;
        case 4:
            // 0 x 0 0
            emptyArray[0] = s1;
            emptyArray[1] = s1;
            emptyArray[2] = s1;
            emptyArray[3] = s1;
            break;
        case 5:
            // 0 x 0 x
            emptyArray[0] = s_;
            emptyArray[1] = (s_ + s1) / 2;
            emptyArray[2] = s1;
            emptyArray[3] = s1;
            break;
        case 6:
            // 0 x x 0
            emptyArray[0] = s0;
            emptyArray[1] = s0;
            emptyArray[2] = s1;
            emptyArray[3] = s1;
            break;
        case 7:
            // 0 x x x
            emptyArray[0] = s_;
            emptyArray[1] = s0;
            emptyArray[2] = s1;
            emptyArray[3] = s1;
            break;
        case 8:
            // x 0 0 0
            emptyArray[0] = s2;
            emptyArray[1] = s2;
            emptyArray[2] = s2;
            emptyArray[3] = s2;
            break;
        case 9:
            // x 0 0 x
            emptyArray[0] = s_;
            emptyArray[1] = (s_ + s2) / 2;
            emptyArray[2] = (s_ + s2) / 2;
            emptyArray[3] = s2;
            break;
        case 10:
            // x 0 x 0
            emptyArray[0] = s0;
            emptyArray[1] = s0;
            emptyArray[2] = (s0 + s2) / 2;
            emptyArray[3] = s2;
            break;
        case 11:
            // x 0 x x
            emptyArray[0] = s_;
            emptyArray[1] = s0;
            emptyArray[2] = (s0 + s2) / 2;
            emptyArray[3] = s2;
            break;
        case 12:
            // x x 0 0
            emptyArray[0] = s1;
            emptyArray[1] = s1;
            emptyArray[2] = s1;
            emptyArray[3] = s2;
            break;
        case 13:
            // x x 0 x
            emptyArray[0] = s_;
            emptyArray[1] = (s_ + s1) / 2;
            emptyArray[2] = s1;
            emptyArray[3] = s2;
            break;
        case 14:
            // x x x 0
            emptyArray[0] = s0;
            emptyArray[1] = s0;
            emptyArray[2] = s1;
            emptyArray[3] = s2;
            break;
        default:
            throw new IllegalArgumentException("Array cannot be composed from more than 4 elements");
        }

        return emptyArray;
    }
}
