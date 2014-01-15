/*
 *    JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    (C) 2012, GeoSolutions
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    either version 3 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package it.geosolutions.jaiext.warp;

import it.geosolutions.jaiext.iterators.RandomIterFactory;
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
 * @since EA2
 * @see javax.media.jai.Warp
 * @see javax.media.jai.WarpOpImage
 * @see javax.media.jai.operator.WarpDescriptor
 * @see WarpRIF
 * 
 */
@SuppressWarnings("unchecked")
final class WarpBicubicOpImage extends WarpOpImage {

    private final static int KERNEL_DIM = 3;
    
    private static final int KERNEL_LINE_DIM = 4;
    
    /** Color table representing source's IndexColorModel. */
    private byte[][] ctable = null;

    private boolean[] booleanLookupTable;

    private int[] dataHi;

    private int[] dataVi;

    private float[] dataHf;

    private float[] dataVf;

    private double[] dataHd;

    private double[] dataVd;

    private int shift;
    
    private int round;

    private int precisionBits;

    /**
     * Constructs a WarpBilinearOpImage.
     * 
     * @param source The source image.
     * @param extender A BorderExtender, or null.
     * @param layout The destination image layout.
     * @param warp An object defining the warp algorithm.
     * @param interp An object describing the interpolation method.
     */
    public WarpBicubicOpImage(final RenderedImage source, final BorderExtender extender,
            final Map<?, ?> config, final ImageLayout layout, final Warp warp,
            final Interpolation interp, final ROI sourceROI) {
        super(source, layout, config, false, extender, interp, warp, null, sourceROI);

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

        RandomIter iterSource;
        final int minX, maxX, minY, maxY;
        if (extended) {
            final Rectangle bounds = new Rectangle(src.getMinX() - 1, src.getMinY() - 1,
                    src.getWidth() + 2, src.getHeight() + 2);
            iterSource = RandomIterFactory.create(src.getExtendedData(bounds, extender), bounds,
                    TILE_CACHED, ARRAY_CALC);
            
            minX = src.getMinX() - 1; // Left padding
            maxX = src.getMaxX() + 2; // Right padding
            minY = src.getMinY() - 1; // Top padding
            maxY = src.getMaxY() + 2; // Bottom padding
        } else {
            iterSource = RandomIterFactory.create(src, src.getBounds(), TILE_CACHED, ARRAY_CALC);
        
            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();
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
        
        // Bicubic interpolation kernel
        int[][] kernelData = new int[KERNEL_DIM][KERNEL_DIM];

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
                            long sum = 0; 
                            for (int b = 0; b < dstBands; b++) {                                
                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                for (int j = 0; j < KERNEL_LINE_DIM; j++) {
                                    // Row temporary sum initialization
                                    long temp = 0;
                                    for (int i = 0; i < KERNEL_LINE_DIM; i++) {
                                        // Selection of one pixel
                                        int pixelValue = iterSource.getSample(xint + (i - 1), yint + (j - 1), b) & 0xff;
                                        // Update of the temporary sum
                                        temp += (pixelValue * dataHi[offsetX + i]);
                                    }
                                    // Vertical sum update
                                    sum += ((temp + round) >> precisionBits) * dataVi[offsetY + j];
                                }
                                // Interpolation
                                int result = (int) ((sum + round) >> precisionBits);
 
                                data[b][pixelOffset + bandOffsets[b]] = (byte) result;
                                
                                //TODO CONTINUE FROM HERE
                                //TODO
                                //TODO
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
                            //
                            // ROI
                            //
                            // checks with roi
                            final boolean w00 = !roiTile.contains(xint, yint);
                            final boolean w01 = !roiTile.contains(xint + 1, yint);
                            final boolean w10 = !roiTile.contains(xint, yint + 1);
                            final boolean w11 = !roiTile.contains(xint + 1, yint + 1);
                            if (w00 && w01 && w10 && w11) { // SG should not happen
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
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
            } else if (caseC) {
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
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                } else {
                                    data[b][pixelOffset + bandOffsets[b]] = (byte) ((int) computePoint(
                                            s00, s01, s10, s11, xfrac, yfrac, w00, w01, w10, w11) & 0xFF);
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
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            //
                            // ROI
                            //
                            // checks with roi
                            final boolean w00Roi = !roiTile.contains(xint, yint);
                            final boolean w01Roi = !roiTile.contains(xint + 1, yint);
                            final boolean w10Roi = !roiTile.contains(xint, yint + 1);
                            final boolean w11Roi = !roiTile.contains(xint + 1, yint + 1);
                            if (w00Roi && w01Roi && w10Roi && w11Roi) { // SG should not happen
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
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
                                        data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                    } else {
                                        data[b][pixelOffset + bandOffsets[b]] = (byte) ((int) computePoint(
                                                s00, s01, s10, s11, xfrac, yfrac, w00, w01, w10,
                                                w11) & 0xFF);
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
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            //
                            // ROI
                            //
                            // checks with roi
                            final boolean w00 = !roiTile.contains(xint, yint);
                            final boolean w01 = !roiTile.contains(xint + 1, yint);
                            final boolean w10 = !roiTile.contains(xint, yint + 1);
                            final boolean w11 = !roiTile.contains(xint + 1, yint + 1);
                            if (w00 && w01 && w10 && w11) { // SG should not happen
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
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
            } else if (caseC) {
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
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                } else {
                                    data[b][pixelOffset + bandOffsets[b]] = (byte) ((int) computePoint(
                                            s00, s01, s10, s11, xfrac, yfrac, w00, w01, w10, w11) & 0xFF);
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
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            //
                            // ROI
                            //
                            // checks with roi
                            final boolean w00Roi = !roiTile.contains(xint, yint);
                            final boolean w01Roi = !roiTile.contains(xint + 1, yint);
                            final boolean w10Roi = !roiTile.contains(xint, yint + 1);
                            final boolean w11Roi = !roiTile.contains(xint + 1, yint + 1);
                            if (w00Roi && w01Roi && w10Roi && w11Roi) { // SG should not happen
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
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
                                        data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                    } else {
                                        data[b][pixelOffset + bandOffsets[b]] = (byte) ((int) computePoint(
                                                s00, s01, s10, s11, xfrac, yfrac, w00, w01, w10,
                                                w11) & 0xFF);
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
            final ROI roiTile) {

        RandomIter iterSource;
        if (extended) {
            final Rectangle bounds = new Rectangle(src.getMinX(), src.getMinY(),
                    src.getWidth() + 1, src.getHeight() + 1);
            iterSource = RandomIterFactory.create(src.getExtendedData(bounds, extender), bounds,
                    TILE_CACHED, ARRAY_CALC);
        } else {
            iterSource = RandomIterFactory.create(src, src.getBounds(), TILE_CACHED, ARRAY_CALC);
        }
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                    } else {
                        //
                        // ROI
                        //
                        // checks with roi
                        final boolean w00 = !roiTile.contains(xint, yint);
                        final boolean w01 = !roiTile.contains(xint + 1, yint);
                        final boolean w10 = !roiTile.contains(xint, yint + 1);
                        final boolean w11 = !roiTile.contains(xint + 1, yint + 1);
                        if (w00 && w01 && w10 && w11) { // SG should not happen
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
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
        } else if (caseC) {
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            } else {
                                data[b][pixelOffset + bandOffsets[b]] = (short) ((int) computePoint(
                                        s00, s01, s10, s11, xfrac, yfrac, w00, w01, w10, w11) & 0xFFFF);
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                    } else {
                        //
                        // ROI
                        //
                        // checks with roi
                        final boolean w00Roi = !roiTile.contains(xint, yint);
                        final boolean w01Roi = !roiTile.contains(xint + 1, yint);
                        final boolean w10Roi = !roiTile.contains(xint, yint + 1);
                        final boolean w11Roi = !roiTile.contains(xint + 1, yint + 1);
                        if (w00Roi && w01Roi && w10Roi && w11Roi) { // SG should not happen
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
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
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                                } else {
                                    data[b][pixelOffset + bandOffsets[b]] = (short) ((int) computePoint(
                                            s00, s01, s10, s11, xfrac, yfrac, w00, w01, w10, w11) & 0xFFFF);
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
            final ROI roiTile) {

        RandomIter iterSource;
        if (extended) {
            final Rectangle bounds = new Rectangle(src.getMinX(), src.getMinY(),
                    src.getWidth() + 1, src.getHeight() + 1);
            iterSource = RandomIterFactory.create(src.getExtendedData(bounds, extender), bounds,
                    TILE_CACHED, ARRAY_CALC);
        } else {
            iterSource = RandomIterFactory.create(src, src.getBounds(), TILE_CACHED, ARRAY_CALC);
        }
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                    } else {
                        //
                        // ROI
                        //
                        // checks with roi
                        final boolean w00 = !roiTile.contains(xint, yint);
                        final boolean w01 = !roiTile.contains(xint + 1, yint);
                        final boolean w10 = !roiTile.contains(xint, yint + 1);
                        final boolean w11 = !roiTile.contains(xint + 1, yint + 1);
                        if (w00 && w01 && w10 && w11) { // SG should not happen
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
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
        } else if (caseC) {
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            } else {
                                data[b][pixelOffset + bandOffsets[b]] = (short) (computePoint(s00,
                                        s01, s10, s11, xfrac, yfrac, w00, w01, w10, w11));
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                    } else {
                        //
                        // ROI
                        //
                        // checks with roi
                        final boolean w00Roi = !roiTile.contains(xint, yint);
                        final boolean w01Roi = !roiTile.contains(xint + 1, yint);
                        final boolean w10Roi = !roiTile.contains(xint, yint + 1);
                        final boolean w11Roi = !roiTile.contains(xint + 1, yint + 1);
                        if (w00Roi && w01Roi && w10Roi && w11Roi) { // SG should not happen
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
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
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                                } else {
                                    data[b][pixelOffset + bandOffsets[b]] = (short) (computePoint(
                                            s00, s01, s10, s11, xfrac, yfrac, w00, w01, w10, w11));
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

    protected void computeRectInt(final PlanarImage src, final RasterAccessor dst, final ROI roiTile) {

        RandomIter iterSource;
        if (extended) {
            final Rectangle bounds = new Rectangle(src.getMinX(), src.getMinY(),
                    src.getWidth() + 1, src.getHeight() + 1);
            iterSource = RandomIterFactory.create(src.getExtendedData(bounds, extender), bounds,
                    TILE_CACHED, ARRAY_CALC);
        } else {
            iterSource = RandomIterFactory.create(src, src.getBounds(), TILE_CACHED, ARRAY_CALC);
        }
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataInt;
                            }
                        }
                    } else {
                        //
                        // ROI
                        //
                        // checks with roi
                        final boolean w00 = !roiTile.contains(xint, yint);
                        final boolean w01 = !roiTile.contains(xint + 1, yint);
                        final boolean w10 = !roiTile.contains(xint, yint + 1);
                        final boolean w11 = !roiTile.contains(xint + 1, yint + 1);
                        if (w00 && w01 && w10 && w11) { // SG should not happen
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataInt;
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
        } else if (caseC) {
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataInt;
                            } else {
                                data[b][pixelOffset + bandOffsets[b]] = ((int) computePoint(s00,
                                        s01, s10, s11, xfrac, yfrac, w00, w01, w10, w11));
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataInt;
                            }
                        }
                    } else {
                        //
                        // ROI
                        //
                        // checks with roi
                        final boolean w00Roi = !roiTile.contains(xint, yint);
                        final boolean w01Roi = !roiTile.contains(xint + 1, yint);
                        final boolean w10Roi = !roiTile.contains(xint, yint + 1);
                        final boolean w11Roi = !roiTile.contains(xint + 1, yint + 1);
                        if (w00Roi && w01Roi && w10Roi && w11Roi) { // SG should not happen
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataInt;
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
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataInt;
                                } else {
                                    data[b][pixelOffset + bandOffsets[b]] = ((int) computePoint(
                                            s00, s01, s10, s11, xfrac, yfrac, w00, w01, w10, w11));
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
            final ROI roiTile) {
        RandomIter iterSource;
        if (extended) {
            final Rectangle bounds = new Rectangle(src.getMinX(), src.getMinY(),
                    src.getWidth() + 1, src.getHeight() + 1);
            iterSource = RandomIterFactory.create(src.getExtendedData(bounds, extender), bounds,
                    TILE_CACHED, ARRAY_CALC);
        } else {
            iterSource = RandomIterFactory.create(src, src.getBounds(), TILE_CACHED, ARRAY_CALC);
        }
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataFloat;
                            }
                        }
                    } else {
                        //
                        // ROI
                        //
                        // checks with roi
                        final boolean w00 = !roiTile.contains(xint, yint);
                        final boolean w01 = !roiTile.contains(xint + 1, yint);
                        final boolean w10 = !roiTile.contains(xint, yint + 1);
                        final boolean w11 = !roiTile.contains(xint + 1, yint + 1);
                        if (w00 && w01 && w10 && w11) { // SG should not happen
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataFloat;
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
        } else if (caseC) {
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataFloat;
                            } else {
                                data[b][pixelOffset + bandOffsets[b]] = (float) (computePoint(s00,
                                        s01, s10, s11, xfrac, yfrac, w00, w01, w10, w11));
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataFloat;
                            }
                        }
                    } else {
                        //
                        // ROI
                        //
                        // checks with roi
                        final boolean w00Roi = !roiTile.contains(xint, yint);
                        final boolean w01Roi = !roiTile.contains(xint + 1, yint);
                        final boolean w10Roi = !roiTile.contains(xint, yint + 1);
                        final boolean w11Roi = !roiTile.contains(xint + 1, yint + 1);
                        if (w00Roi && w01Roi && w10Roi && w11Roi) { // SG should not happen
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataFloat;
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
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataFloat;
                                } else {
                                    data[b][pixelOffset + bandOffsets[b]] = (float) (computePoint(
                                            s00, s01, s10, s11, xfrac, yfrac, w00, w01, w10, w11));
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
            final ROI roiTile) {

        RandomIter iterSource;
        if (extended) {
            final Rectangle bounds = new Rectangle(src.getMinX(), src.getMinY(),
                    src.getWidth() + 1, src.getHeight() + 1);
            iterSource = RandomIterFactory.create(src.getExtendedData(bounds, extender), bounds,
                    TILE_CACHED, ARRAY_CALC);
        } else {
            iterSource = RandomIterFactory.create(src, src.getBounds(), TILE_CACHED, ARRAY_CALC);
        }
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataDouble;
                            }
                        }
                    } else {
                        //
                        // ROI
                        //
                        // checks with roi
                        final boolean w00 = !roiTile.contains(xint, yint);
                        final boolean w01 = !roiTile.contains(xint + 1, yint);
                        final boolean w10 = !roiTile.contains(xint, yint + 1);
                        final boolean w11 = !roiTile.contains(xint + 1, yint + 1);
                        if (w00 && w01 && w10 && w11) { // SG should not happen
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataDouble;
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
        } else if (caseC) {
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataDouble;
                            } else {
                                data[b][pixelOffset + bandOffsets[b]] = (float) (computePoint(s00,
                                        s01, s10, s11, xfrac, yfrac, w00, w01, w10, w11));
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
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataDouble;
                            }
                        }
                    } else {
                        //
                        // ROI
                        //
                        // checks with roi
                        final boolean w00Roi = !roiTile.contains(xint, yint);
                        final boolean w01Roi = !roiTile.contains(xint + 1, yint);
                        final boolean w10Roi = !roiTile.contains(xint, yint + 1);
                        final boolean w11Roi = !roiTile.contains(xint + 1, yint + 1);
                        if (w00Roi && w01Roi && w10Roi && w11Roi) { // SG should not happen
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataDouble;
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
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataDouble;
                                } else {
                                    data[b][pixelOffset + bandOffsets[b]] = (float) (computePoint(
                                            s00, s01, s10, s11, xfrac, yfrac, w00, w01, w10, w11));
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

    /**
     * Computes the bilinear interpolation when No Data are present
     * 
     * @param s00
     * @param s01
     * @param s10
     * @param s11
     * @param w00
     * @param w01
     * @param w10
     * @param w11
     * @param dataType
     * @return
     */
    private double computePoint(double s00, double s01, double s10, double s11, double xfrac,
            double yfrac, boolean w00, boolean w01, boolean w10, boolean w11) {

        // Initialization
        double s0 = 0;
        double s1 = 0;
        double s = 0;

        // Complementary values of the fractional part
        double xfracCompl = 1 - xfrac;
        double yfracCompl = 1 - yfrac;

        if (!w00 && !w01 && !w10 && !w11) {
            // Perform the bilinear interpolation because all the weight are not 0.
            s0 = (s01 - s00) * xfrac + s00;
            s1 = (s11 - s10) * xfrac + s10;
            s = (s1 - s0) * yfrac + s0;
        } else {
            // upper value

            if (w00 && w01) {
                s0 = 0;
            } else if (w00) { // w01 = false
                s0 = s01 * xfrac;
            } else if (w01) {// w00 = false
                s0 = s00 * xfracCompl;// s00;
            } else {// w00 = false & W01 = false
                s0 = (s01 - s00) * xfrac + s00;
            }

            // lower value

            if (w10 && w11) {
                s1 = 0;
            } else if (w10) { // w11 = false
                s1 = s11 * xfrac;
            } else if (w11) { // w10 = false
                s1 = s10 * xfracCompl;// - (s10 * xfrac); //s10;
            } else {
                s1 = (s11 - s10) * xfrac + s10;
            }

            if (w00 && w01) {
                s = s1 * yfrac;
            } else {
                if (w10 && w11) {
                    s = s0 * yfracCompl;
                } else {
                    s = (s1 - s0) * yfrac + s0;
                }
            }
        }
        return s;
    }

    /** Returns the "floor" value of a float. */
    private static final int floor(final float f) {
        return f >= 0 ? (int) f : (int) f - 1;
    }
}
