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

import com.sun.media.jai.util.ImageUtil;

/**
 * An <code>OpImage</code> implementing the general "Warp" operation as described in <code>javax.media.jai.operator.WarpDescriptor</code>. It supports
 * all interpolation cases.
 * 
 * <p>
 * The layout for the destination image may be specified via the <code>ImageLayout</code> parameter. However, only those settings suitable for this
 * operation will be used. The unsuitable settings will be replaced by default suitable values. An optional ROI object and a NoData Range can be used.
 * If a backward mapped pixel lies outside ROI or it is a NoData, then the destination pixel value is a background value.
 * 
 * If the input image contains an IndexColorModel, then pixel values are taken directly from the input color table.
 * 
 * @since EA2
 * @see javax.media.jai.Warp
 * @see javax.media.jai.WarpOpImage
 * @see javax.media.jai.operator.WarpDescriptor
 * @see WarpRIF
 * 
 */
@SuppressWarnings("unchecked")
final class WarpGeneralOpImage extends WarpOpImage {

    private static final int NODATA_VALUE = 0;

    /** Color table representing source's IndexColorModel. */
    private byte[][] ctable = null;

    /** LookupTable used for a faster NoData check */
    private byte[] byteLookupTable;

    /**
     * Constructs a WarpGeneralOpImage.
     * 
     * @param source The source image.
     * @param extender A BorderExtender, or null.
     * @param extender A BorderExtender, or null.
     * @param layout The destination image layout.
     * @param warp An object defining the warp algorithm.
     * @param interp An object describing the interpolation method.
     * @param background background values to set.
     * @param roi input ROI object used.
     * @param noData NoData Range object used for checking if NoData are present.
     */
    public WarpGeneralOpImage(RenderedImage source, BorderExtender extender, Map<?, ?> config,
            ImageLayout layout, Warp warp, Interpolation interp, double[] background,
            ROI sourceROI, Range noData) {
        super(source, layout, config, false, extender, interp, warp, background, sourceROI, noData);

        /*
         * If the source has IndexColorModel, get the RGB color table. Note, in this case, the source should have an integral data type. And dest
         * always has data type byte.
         */
        ColorModel srcColorModel = source.getColorModel();
        if (srcColorModel instanceof IndexColorModel) {
            IndexColorModel icm = (IndexColorModel) srcColorModel;
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
                byteLookupTable = new byte[256];
                for (int i = 0; i < byteLookupTable.length; i++) {
                    byte value = (byte) i;
                    if (noDataRange.contains(value)) {
                        byteLookupTable[i] = NODATA_VALUE;
                    } else {
                        byteLookupTable[i] = value;
                    }
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
        
        // Definition of the padding
        if (interp != null) {
            leftPad = interp.getLeftPadding();
            rightPad = interp.getRightPadding();
            topPad = interp.getTopPadding();
            bottomPad = interp.getBottomPadding();
        } else {
            leftPad = rightPad = topPad = bottomPad = 0;
        }
    }

    protected void computeRectByte(PlanarImage src, RasterAccessor dst, final RandomIter roiIter,
            boolean roiContainsTile) {

        // Setting of the Random iterator keeping into account the presence of the Borderextender
        int minX, maxX, minY, maxY;
        RandomIter iter;
        if (extended) {
            // Creation of an iterator on the image extended by the padding factors
            iter = getRandomIterator(src, leftPad, rightPad, topPad, bottomPad, extender);
            // Definition of the image bounds
            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();

        } else {
            // Creation of an iterator on the image
            iter = getRandomIterator(src, null);
            // Definition of the image bounds
            minX = src.getMinX() + leftPad; // Left padding
            maxX = src.getMaxX() - rightPad; // Right padding
            minY = src.getMinY() + topPad; // Top padding
            maxY = src.getMaxY() - bottomPad; // Bottom padding
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

        int kwidth = interp.getWidth();
        int kheight = interp.getHeight();

        int precH = 1 << interp.getSubsampleBitsH();
        int precV = 1 << interp.getSubsampleBitsV();

        int[][] samples = new int[kheight][kwidth];

        boolean roiWeight;

        if (ctable == null) { // source does not have IndexColorModel

            // ONLY VALID DATA
            if (caseA || (caseB && roiContainsTile)) {
                for (int h = 0; h < dstHeight; h++) {
                    int pixelOffset = lineOffset;
                    lineOffset += lineStride;

                    warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                    int count = 0;
                    for (int w = 0; w < dstWidth; w++) {
                        float sx = warpData[count++];
                        float sy = warpData[count++];

                        int xint = floor(sx);
                        int yint = floor(sy);
                        int xfrac = (int) ((sx - xint) * precH);
                        int yfrac = (int) ((sy - yint) * precV);

                        if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                            /* Fill with a background color. */
                            if (setBackground) {
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            // Interpolation
                            xint -= leftPad;
                            yint -= topPad;

                            for (int b = 0; b < dstBands; b++) {
                                for (int j = 0; j < kheight; j++) {
                                    for (int i = 0; i < kwidth; i++) {
                                        samples[j][i] = iter.getSample(xint + i, yint + j, b) & 0xFF;
                                    }
                                }

                                data[b][pixelOffset + bandOffsets[b]] = ImageUtil.clampByte(interp
                                        .interpolate(samples, xfrac, yfrac));
                            }
                        }

                        pixelOffset += pixelStride;
                    }
                }
                // ONLY ROI
            } else if (caseB) {
                for (int h = 0; h < dstHeight; h++) {
                    int pixelOffset = lineOffset;
                    lineOffset += lineStride;

                    warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                    int count = 0;
                    for (int w = 0; w < dstWidth; w++) {
                        float sx = warpData[count++];
                        float sy = warpData[count++];

                        int xint = floor(sx);
                        int yint = floor(sy);
                        int xfrac = (int) ((sx - xint) * precH);
                        int yfrac = (int) ((sy - yint) * precV);

                        if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                            /* Fill with a background color. */
                            if (setBackground) {
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            xint -= leftPad;
                            yint -= topPad;

                            roiWeight = false;

                            for (int j = 0; j < kheight; j++) {
                                for (int i = 0; i < kwidth; i++) {
                                    int x = xint + i;
                                    int y = yint + j;
                                    if (roiBounds.contains(x, y)) {
                                        roiWeight |= roiIter.getSample(x, y, 0) > 0;
                                    }
                                }
                            }
                            //
                            // ROI check
                            //
                            //
                            if (roiWeight) {
                                for (int b = 0; b < dstBands; b++) {
                                    for (int j = 0; j < kheight; j++) {
                                        for (int i = 0; i < kwidth; i++) {
                                            samples[j][i] = iter.getSample(xint + i, yint + j, b) & 0xFF;
                                        }
                                    }

                                    data[b][pixelOffset + bandOffsets[b]] = ImageUtil
                                            .clampByte(interp.interpolate(samples, xfrac, yfrac));
                                }
                            } else {
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        }

                        pixelOffset += pixelStride;
                    }
                }
                // ONLY NODATA
            } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
                for (int h = 0; h < dstHeight; h++) {
                    int pixelOffset = lineOffset;
                    lineOffset += lineStride;

                    warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                    int count = 0;
                    for (int w = 0; w < dstWidth; w++) {
                        float sx = warpData[count++];
                        float sy = warpData[count++];

                        int xint = floor(sx);
                        int yint = floor(sy);
                        int xfrac = (int) ((sx - xint) * precH);
                        int yfrac = (int) ((sy - yint) * precV);

                        if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                            /* Fill with a background color. */
                            if (setBackground) {
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            xint -= leftPad;
                            yint -= topPad;

                            roiWeight = false;
                            for (int j = 0; j < kheight; j++) {
                                for (int i = 0; i < kwidth; i++) {
                                    int x = xint + i;
                                    int y = yint + j;
                                    if (roiBounds.contains(x, y)) {
                                        roiWeight |= roiIter.getSample(x, y, 0) > 0;
                                    }
                                }
                            }
                            //
                            // NODATA check
                            //
                            //
                            for (int b = 0; b < dstBands; b++) {
                                for (int j = 0; j < kheight; j++) {
                                    for (int i = 0; i < kwidth; i++) {
                                        // If the value is a NODATA, is substituted with 0
                                        samples[j][i] = byteLookupTable[iter.getSample(xint + i,
                                                yint + j, b) & 0xFF];
                                    }
                                }

                                data[b][pixelOffset + bandOffsets[b]] = ImageUtil.clampByte(interp
                                        .interpolate(samples, xfrac, yfrac));
                            }
                        }
                        pixelOffset += pixelStride;
                    }
                }
                // BOTH ROI AND NODATA
            } else {
                for (int h = 0; h < dstHeight; h++) {
                    int pixelOffset = lineOffset;
                    lineOffset += lineStride;

                    warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                    int count = 0;
                    for (int w = 0; w < dstWidth; w++) {
                        float sx = warpData[count++];
                        float sy = warpData[count++];

                        int xint = floor(sx);
                        int yint = floor(sy);
                        int xfrac = (int) ((sx - xint) * precH);
                        int yfrac = (int) ((sy - yint) * precV);

                        if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                            /* Fill with a background color. */
                            if (setBackground) {
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            xint -= leftPad;
                            yint -= topPad;

                            roiWeight = false;
                            for (int j = 0; j < kheight; j++) {
                                for (int i = 0; i < kwidth; i++) {
                                    int x = xint + i;
                                    int y = yint + j;
                                    if (roiBounds.contains(x, y)) {
                                        roiWeight |= roiIter.getSample(x, y, 0) > 0;
                                    }
                                }
                            }
                            //
                            // ROI check
                            //
                            //
                            if (roiWeight) {
                                for (int b = 0; b < dstBands; b++) {
                                    for (int j = 0; j < kheight; j++) {
                                        //
                                        // NODATA check
                                        //
                                        //
                                        for (int i = 0; i < kwidth; i++) {
                                            // If the value is a NODATA, is substituted with 0
                                            samples[j][i] = byteLookupTable[iter.getSample(
                                                    xint + i, yint + j, b) & 0xFF];
                                        }
                                    }

                                    data[b][pixelOffset + bandOffsets[b]] = ImageUtil
                                            .clampByte(interp.interpolate(samples, xfrac, yfrac));
                                }
                            } else {
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        }

                        pixelOffset += pixelStride;
                    }
                }
            }
        } else { // source has IndexColorModel

            // ONLY VALID DATA
            if (caseA || (caseB && roiContainsTile)) {
                for (int h = 0; h < dstHeight; h++) {
                    int pixelOffset = lineOffset;
                    lineOffset += lineStride;

                    warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                    int count = 0;
                    for (int w = 0; w < dstWidth; w++) {
                        float sx = warpData[count++];
                        float sy = warpData[count++];

                        int xint = floor(sx);
                        int yint = floor(sy);
                        int xfrac = (int) ((sx - xint) * precH);
                        int yfrac = (int) ((sy - yint) * precV);

                        if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                            /* Fill with a background color. */
                            if (setBackground) {
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            xint -= leftPad;
                            yint -= topPad;

                            for (int b = 0; b < dstBands; b++) {
                                byte[] t = ctable[b];
                                for (int j = 0; j < kheight; j++) {
                                    for (int i = 0; i < kwidth; i++) {
                                        samples[j][i] = t[iter.getSample(xint + i, yint + j, 0) & 0xFF] & 0xFF;
                                    }
                                }

                                data[b][pixelOffset + bandOffsets[b]] = ImageUtil.clampByte(interp
                                        .interpolate(samples, xfrac, yfrac));
                            }
                        }

                        pixelOffset += pixelStride;
                    }
                }
                // ONLY ROI
            } else if (caseB) {
                for (int h = 0; h < dstHeight; h++) {
                    int pixelOffset = lineOffset;
                    lineOffset += lineStride;

                    warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                    int count = 0;
                    for (int w = 0; w < dstWidth; w++) {
                        float sx = warpData[count++];
                        float sy = warpData[count++];

                        int xint = floor(sx);
                        int yint = floor(sy);
                        int xfrac = (int) ((sx - xint) * precH);
                        int yfrac = (int) ((sy - yint) * precV);

                        if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                            /* Fill with a background color. */
                            if (setBackground) {
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            xint -= leftPad;
                            yint -= topPad;

                            roiWeight = false;
                            for (int j = 0; j < kheight; j++) {
                                for (int i = 0; i < kwidth; i++) {
                                    int x = xint + i;
                                    int y = yint + j;
                                    if (roiBounds.contains(x, y)) {
                                        roiWeight |= roiIter.getSample(x, y, 0) > 0;
                                    }
                                }
                            }
                            //
                            // ROI check
                            //
                            //
                            if (roiWeight) {
                                for (int b = 0; b < dstBands; b++) {
                                    byte[] t = ctable[b];
                                    for (int j = 0; j < kheight; j++) {
                                        for (int i = 0; i < kwidth; i++) {
                                            samples[j][i] = t[iter.getSample(xint + i, yint + j, 0) & 0xFF] & 0xFF;
                                        }
                                    }

                                    data[b][pixelOffset + bandOffsets[b]] = ImageUtil
                                            .clampByte(interp.interpolate(samples, xfrac, yfrac));
                                }
                            } else {
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        }

                        pixelOffset += pixelStride;
                    }
                }
                // ONLY NODATA
            } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
                for (int h = 0; h < dstHeight; h++) {
                    int pixelOffset = lineOffset;
                    lineOffset += lineStride;

                    warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                    int count = 0;
                    for (int w = 0; w < dstWidth; w++) {
                        float sx = warpData[count++];
                        float sy = warpData[count++];

                        int xint = floor(sx);
                        int yint = floor(sy);
                        int xfrac = (int) ((sx - xint) * precH);
                        int yfrac = (int) ((sy - yint) * precV);

                        if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                            /* Fill with a background color. */
                            if (setBackground) {
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            xint -= leftPad;
                            yint -= topPad;
                            //
                            // NODATA check
                            //
                            //
                            for (int b = 0; b < dstBands; b++) {
                                byte[] t = ctable[b];
                                for (int j = 0; j < kheight; j++) {
                                    for (int i = 0; i < kwidth; i++) {
                                        // If the value is a NODATA, is substituted with 0
                                        samples[j][i] = byteLookupTable[t[iter.getSample(xint + i,
                                                yint + j, 0) & 0xFF] & 0xFF];
                                    }
                                }

                                data[b][pixelOffset + bandOffsets[b]] = ImageUtil.clampByte(interp
                                        .interpolate(samples, xfrac, yfrac));
                            }
                        }

                        pixelOffset += pixelStride;
                    }
                }
                // BOTH ROI AND NODATA
            } else {
                for (int h = 0; h < dstHeight; h++) {
                    int pixelOffset = lineOffset;
                    lineOffset += lineStride;

                    warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                    int count = 0;
                    for (int w = 0; w < dstWidth; w++) {
                        float sx = warpData[count++];
                        float sy = warpData[count++];

                        int xint = floor(sx);
                        int yint = floor(sy);
                        int xfrac = (int) ((sx - xint) * precH);
                        int yfrac = (int) ((sy - yint) * precV);

                        if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                            /* Fill with a background color. */
                            if (setBackground) {
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            xint -= leftPad;
                            yint -= topPad;

                            roiWeight = false;
                            for (int j = 0; j < kheight; j++) {
                                for (int i = 0; i < kwidth; i++) {
                                    int x = xint + i;
                                    int y = yint + j;
                                    if (roiBounds.contains(x, y)) {
                                        roiWeight |= roiIter.getSample(x, y, 0) > 0;
                                    }
                                }
                            }
                            //
                            // ROI check
                            //
                            //
                            if (roiWeight) {
                                for (int b = 0; b < dstBands; b++) {
                                    byte[] t = ctable[b];
                                    //
                                    // NODATA check
                                    //
                                    //
                                    for (int j = 0; j < kheight; j++) {
                                        for (int i = 0; i < kwidth; i++) {
                                            // If the value is a NODATA, is substituted with 0
                                            samples[j][i] = byteLookupTable[t[iter.getSample(xint
                                                    + i, yint + j, 0) & 0xFF] & 0xFF];
                                        }
                                    }

                                    data[b][pixelOffset + bandOffsets[b]] = ImageUtil
                                            .clampByte(interp.interpolate(samples, xfrac, yfrac));
                                }
                            } else {
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset + bandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        }

                        pixelOffset += pixelStride;
                    }
                }
            }
        }
        iter.done();
    }

    protected void computeRectUShort(PlanarImage src, RasterAccessor dst, final RandomIter roiIter,
            boolean roiContainsTile) {
        // Setting of the Random iterator keeping into account the presence of the Borderextender
        int minX, maxX, minY, maxY;
        RandomIter iter;
        if (extended) {
            // Creation of an iterator on the image extended by the padding factors
            iter = getRandomIterator(src, leftPad, rightPad, topPad, bottomPad, extender);
            // Definition of the image bounds
            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();

        } else {
            // Creation of an iterator on the image
            iter = getRandomIterator(src, null);
            // Definition of the image bounds
            minX = src.getMinX() + leftPad; // Left padding
            maxX = src.getMaxX() - rightPad; // Right padding
            minY = src.getMinY() + topPad; // Top padding
            maxY = src.getMaxY() - bottomPad; // Bottom padding
        }

        int kwidth = interp.getWidth();
        int kheight = interp.getHeight();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int lineStride = dst.getScanlineStride();
        int pixelStride = dst.getPixelStride();
        int[] bandOffsets = dst.getBandOffsets();
        short[][] data = dst.getShortDataArrays();

        int precH = 1 << interp.getSubsampleBitsH();
        int precV = 1 << interp.getSubsampleBitsV();

        float[] warpData = new float[2 * dstWidth];

        int[][] samples = new int[kheight][kwidth];

        boolean roiWeight;

        int lineOffset = 0;

        // ONLY VALID DATA
        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    float sx = warpData[count++];
                    float sy = warpData[count++];

                    int xint = floor(sx);
                    int yint = floor(sy);
                    int xfrac = (int) ((sx - xint) * precH);
                    int yfrac = (int) ((sy - yint) * precV);

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                    } else {
                        xint -= leftPad;
                        yint -= topPad;

                        for (int b = 0; b < dstBands; b++) {
                            for (int j = 0; j < kheight; j++) {
                                for (int i = 0; i < kwidth; i++) {
                                    samples[j][i] = iter.getSample(xint + i, yint + j, b) & 0xFFFF;
                                }
                            }

                            data[b][pixelOffset + bandOffsets[b]] = ImageUtil.clampUShort(interp
                                    .interpolate(samples, xfrac, yfrac));
                        }
                    }

                    pixelOffset += pixelStride;
                }
            }
            // ONLY ROI
        } else if (caseB) {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    float sx = warpData[count++];
                    float sy = warpData[count++];

                    int xint = floor(sx);
                    int yint = floor(sy);
                    int xfrac = (int) ((sx - xint) * precH);
                    int yfrac = (int) ((sy - yint) * precV);

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                    } else {
                        xint -= leftPad;
                        yint -= topPad;

                        roiWeight = false;
                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                int x = xint + i;
                                int y = yint + j;
                                if (roiBounds.contains(x, y)) {
                                    roiWeight |= roiIter.getSample(x, y, 0) > 0;
                                }
                            }
                        }
                        //
                        // ROI check
                        //
                        //
                        if (roiWeight) {
                            for (int b = 0; b < dstBands; b++) {
                                for (int j = 0; j < kheight; j++) {
                                    for (int i = 0; i < kwidth; i++) {
                                        samples[j][i] = iter.getSample(xint + i, yint + j, b) & 0xFFFF;
                                    }
                                }

                                data[b][pixelOffset + bandOffsets[b]] = ImageUtil
                                        .clampUShort(interp.interpolate(samples, xfrac, yfrac));
                            }
                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                    }

                    pixelOffset += pixelStride;
                }
            }
            // ONLY NODATA
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            int value = 0;
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    float sx = warpData[count++];
                    float sy = warpData[count++];

                    int xint = floor(sx);
                    int yint = floor(sy);
                    int xfrac = (int) ((sx - xint) * precH);
                    int yfrac = (int) ((sy - yint) * precV);

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                    } else {
                        xint -= leftPad;
                        yint -= topPad;

                        for (int b = 0; b < dstBands; b++) {
                            for (int j = 0; j < kheight; j++) {
                                //
                                // NODATA check
                                //
                                //
                                for (int i = 0; i < kwidth; i++) {
                                    value = iter.getSample(xint + i, yint + j, b) & 0xFFFF;
                                    // If the value is a NODATA, is substituted with 0 inside the kernel
                                    if (noDataRange.contains((short) value)) {
                                        samples[j][i] = NODATA_VALUE;
                                    } else {
                                        samples[j][i] = value;
                                    }
                                }
                            }
                            data[b][pixelOffset + bandOffsets[b]] = ImageUtil.clampUShort(interp
                                    .interpolate(samples, xfrac, yfrac));
                        }
                    }

                    pixelOffset += pixelStride;
                }
            }
            // BOTH ROI AND NODATA
        } else {
            int value = 0;
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    float sx = warpData[count++];
                    float sy = warpData[count++];

                    int xint = floor(sx);
                    int yint = floor(sy);
                    int xfrac = (int) ((sx - xint) * precH);
                    int yfrac = (int) ((sy - yint) * precV);

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                    } else {
                        xint -= leftPad;
                        yint -= topPad;

                        roiWeight = false;
                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                int x = xint + i;
                                int y = yint + j;
                                if (roiBounds.contains(x, y)) {
                                    roiWeight |= roiIter.getSample(x, y, 0) > 0;
                                }
                            }
                        }
                        //
                        // ROI check
                        //
                        //
                        if (roiWeight) {
                            for (int b = 0; b < dstBands; b++) {
                                for (int j = 0; j < kheight; j++) {
                                    //
                                    // NODATA check
                                    //
                                    //
                                    for (int i = 0; i < kwidth; i++) {
                                        value = iter.getSample(xint + i, yint + j, b) & 0xFFFF;
                                        // If the value is a NODATA, is substituted with 0 inside the kernel
                                        if (noDataRange.contains((short) value)) {
                                            samples[j][i] = NODATA_VALUE;
                                        } else {
                                            samples[j][i] = value;
                                        }
                                    }
                                }

                                data[b][pixelOffset + bandOffsets[b]] = ImageUtil
                                        .clampUShort(interp.interpolate(samples, xfrac, yfrac));
                            }
                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                    }

                    pixelOffset += pixelStride;
                }
            }
        }
        iter.done();
    }

    protected void computeRectShort(PlanarImage src, RasterAccessor dst, final RandomIter roiIter,
            boolean roiContainsTile) {
        // Setting of the Random iterator keeping into account the presence of the Borderextender
        int minX, maxX, minY, maxY;
        RandomIter iter;
        if (extended) {
            // Creation of an iterator on the image extended by the padding factors
            iter = getRandomIterator(src, leftPad, rightPad, topPad, bottomPad, extender);
            // Definition of the image bounds
            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();

        } else {
            // Creation of an iterator on the image
            iter = getRandomIterator(src, null);
            // Definition of the image bounds
            minX = src.getMinX() + leftPad; // Left padding
            maxX = src.getMaxX() - rightPad; // Right padding
            minY = src.getMinY() + topPad; // Top padding
            maxY = src.getMaxY() - bottomPad; // Bottom padding
        }

        int kwidth = interp.getWidth();
        int kheight = interp.getHeight();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int lineStride = dst.getScanlineStride();
        int pixelStride = dst.getPixelStride();
        int[] bandOffsets = dst.getBandOffsets();
        short[][] data = dst.getShortDataArrays();

        int precH = 1 << interp.getSubsampleBitsH();
        int precV = 1 << interp.getSubsampleBitsV();

        float[] warpData = new float[2 * dstWidth];

        int[][] samples = new int[kheight][kwidth];

        boolean roiWeight;

        int lineOffset = 0;

        // ONLY VALID DATA
        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    float sx = warpData[count++];
                    float sy = warpData[count++];

                    int xint = floor(sx);
                    int yint = floor(sy);
                    int xfrac = (int) ((sx - xint) * precH);
                    int yfrac = (int) ((sy - yint) * precV);

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                    } else {
                        xint -= leftPad;
                        yint -= topPad;

                        for (int b = 0; b < dstBands; b++) {
                            for (int j = 0; j < kheight; j++) {
                                for (int i = 0; i < kwidth; i++) {
                                    samples[j][i] = iter.getSample(xint + i, yint + j, b);
                                }
                            }

                            data[b][pixelOffset + bandOffsets[b]] = ImageUtil.clampShort(interp
                                    .interpolate(samples, xfrac, yfrac));
                        }
                    }

                    pixelOffset += pixelStride;
                }
            }
            // ONLY ROI
        } else if (caseB) {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    float sx = warpData[count++];
                    float sy = warpData[count++];

                    int xint = floor(sx);
                    int yint = floor(sy);
                    int xfrac = (int) ((sx - xint) * precH);
                    int yfrac = (int) ((sy - yint) * precV);

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                    } else {
                        xint -= leftPad;
                        yint -= topPad;

                        roiWeight = false;
                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                int x = xint + i;
                                int y = yint + j;
                                if (roiBounds.contains(x, y)) {
                                    roiWeight |= roiIter.getSample(x, y, 0) > 0;
                                }
                            }
                        }
                        //
                        // ROI check
                        //
                        //
                        if (roiWeight) {
                            for (int b = 0; b < dstBands; b++) {
                                for (int j = 0; j < kheight; j++) {
                                    for (int i = 0; i < kwidth; i++) {
                                        samples[j][i] = iter.getSample(xint + i, yint + j, b);
                                    }
                                }

                                data[b][pixelOffset + bandOffsets[b]] = ImageUtil.clampShort(interp
                                        .interpolate(samples, xfrac, yfrac));
                            }
                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                    }

                    pixelOffset += pixelStride;
                }
            }
            // ONLY NODATA
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            int value = 0;
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    float sx = warpData[count++];
                    float sy = warpData[count++];

                    int xint = floor(sx);
                    int yint = floor(sy);
                    int xfrac = (int) ((sx - xint) * precH);
                    int yfrac = (int) ((sy - yint) * precV);

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                    } else {
                        xint -= leftPad;
                        yint -= topPad;

                        for (int b = 0; b < dstBands; b++) {
                            for (int j = 0; j < kheight; j++) {
                                //
                                // NODATA check
                                //
                                //
                                for (int i = 0; i < kwidth; i++) {
                                    value = iter.getSample(xint + i, yint + j, b);
                                    // If the value is a NODATA, is substituted with 0 inside the kernel
                                    if (noDataRange.contains((short) value)) {
                                        samples[j][i] = NODATA_VALUE;
                                    } else {
                                        samples[j][i] = value;
                                    }
                                }
                            }
                            data[b][pixelOffset + bandOffsets[b]] = ImageUtil.clampShort(interp
                                    .interpolate(samples, xfrac, yfrac));
                        }
                    }

                    pixelOffset += pixelStride;
                }
            }
            // BOTH ROI AND NODATA
        } else {
            int value = 0;
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    float sx = warpData[count++];
                    float sy = warpData[count++];

                    int xint = floor(sx);
                    int yint = floor(sy);
                    int xfrac = (int) ((sx - xint) * precH);
                    int yfrac = (int) ((sy - yint) * precV);

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                    } else {
                        xint -= leftPad;
                        yint -= topPad;

                        roiWeight = false;
                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                int x = xint + i;
                                int y = yint + j;
                                if (roiBounds.contains(x, y)) {
                                    roiWeight |= roiIter.getSample(x, y, 0) > 0;
                                }
                            }
                        }
                        //
                        // ROI check
                        //
                        //
                        if (roiWeight) {
                            for (int b = 0; b < dstBands; b++) {
                                for (int j = 0; j < kheight; j++) {
                                    //
                                    // NODATA check
                                    //
                                    //
                                    for (int i = 0; i < kwidth; i++) {
                                        value = iter.getSample(xint + i, yint + j, b);
                                        // If the value is a NODATA, is substituted with 0 inside the kernel
                                        if (noDataRange.contains((short) value)) {
                                            samples[j][i] = NODATA_VALUE;
                                        } else {
                                            samples[j][i] = value;
                                        }
                                    }
                                }

                                data[b][pixelOffset + bandOffsets[b]] = ImageUtil.clampShort(interp
                                        .interpolate(samples, xfrac, yfrac));
                            }
                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                    }

                    pixelOffset += pixelStride;
                }
            }
        }
        iter.done();
    }

    protected void computeRectInt(PlanarImage src, RasterAccessor dst, final RandomIter roiIter,
            boolean roiContainsTile) {
        // Setting of the Random iterator keeping into account the presence of the Borderextender
        int minX, maxX, minY, maxY;
        RandomIter iter;
        if (extended) {
            // Creation of an iterator on the image extended by the padding factors
            iter = getRandomIterator(src, leftPad, rightPad, topPad, bottomPad, extender);
            // Definition of the image bounds
            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();

        } else {
            // Creation of an iterator on the image
            iter = getRandomIterator(src, null);
            // Definition of the image bounds
            minX = src.getMinX() + leftPad; // Left padding
            maxX = src.getMaxX() - rightPad; // Right padding
            minY = src.getMinY() + topPad; // Top padding
            maxY = src.getMaxY() - bottomPad; // Bottom padding
        }

        int kwidth = interp.getWidth();
        int kheight = interp.getHeight();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int lineStride = dst.getScanlineStride();
        int pixelStride = dst.getPixelStride();
        int[] bandOffsets = dst.getBandOffsets();
        int[][] data = dst.getIntDataArrays();

        int precH = 1 << interp.getSubsampleBitsH();
        int precV = 1 << interp.getSubsampleBitsV();

        float[] warpData = new float[2 * dstWidth];

        int[][] samples = new int[kheight][kwidth];

        boolean roiWeight;

        int lineOffset = 0;

        // ONLY VALID DATA
        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    float sx = warpData[count++];
                    float sy = warpData[count++];

                    int xint = floor(sx);
                    int yint = floor(sy);
                    int xfrac = (int) ((sx - xint) * precH);
                    int yfrac = (int) ((sy - yint) * precV);

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataInt;
                            }
                        }
                    } else {
                        xint -= leftPad;
                        yint -= topPad;

                        for (int b = 0; b < dstBands; b++) {
                            for (int j = 0; j < kheight; j++) {
                                for (int i = 0; i < kwidth; i++) {
                                    samples[j][i] = iter.getSample(xint + i, yint + j, b);
                                }
                            }

                            data[b][pixelOffset + bandOffsets[b]] = (interp.interpolate(samples,
                                    xfrac, yfrac));
                        }
                    }

                    pixelOffset += pixelStride;
                }
            }
            // ONLY ROI
        } else if (caseB) {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    float sx = warpData[count++];
                    float sy = warpData[count++];

                    int xint = floor(sx);
                    int yint = floor(sy);
                    int xfrac = (int) ((sx - xint) * precH);
                    int yfrac = (int) ((sy - yint) * precV);

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataInt;
                            }
                        }
                    } else {
                        xint -= leftPad;
                        yint -= topPad;

                        roiWeight = false;
                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                int x = xint + i;
                                int y = yint + j;
                                if (roiBounds.contains(x, y)) {
                                    roiWeight |= roiIter.getSample(x, y, 0) > 0;
                                }
                            }
                        }
                        //
                        // ROI check
                        //
                        //
                        if (roiWeight) {
                            for (int b = 0; b < dstBands; b++) {
                                for (int j = 0; j < kheight; j++) {
                                    for (int i = 0; i < kwidth; i++) {
                                        samples[j][i] = iter.getSample(xint + i, yint + j, b);
                                    }
                                }

                                data[b][pixelOffset + bandOffsets[b]] = (interp.interpolate(
                                        samples, xfrac, yfrac));
                            }
                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataInt;
                            }
                        }
                    }

                    pixelOffset += pixelStride;
                }
            }
            // ONLY NODATA
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            int value = 0;
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    float sx = warpData[count++];
                    float sy = warpData[count++];

                    int xint = floor(sx);
                    int yint = floor(sy);
                    int xfrac = (int) ((sx - xint) * precH);
                    int yfrac = (int) ((sy - yint) * precV);

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataInt;
                            }
                        }
                    } else {
                        xint -= leftPad;
                        yint -= topPad;

                        for (int b = 0; b < dstBands; b++) {
                            for (int j = 0; j < kheight; j++) {
                                //
                                // NODATA check
                                //
                                //
                                for (int i = 0; i < kwidth; i++) {
                                    value = iter.getSample(xint + i, yint + j, b);
                                    // If the value is a NODATA, is substituted with 0 inside the kernel
                                    if (noDataRange.contains(value)) {
                                        samples[j][i] = NODATA_VALUE;
                                    } else {
                                        samples[j][i] = value;
                                    }
                                }
                            }
                            data[b][pixelOffset + bandOffsets[b]] = (interp.interpolate(samples,
                                    xfrac, yfrac));
                        }
                    }

                    pixelOffset += pixelStride;
                }
            }
            // BOTH ROI AND NODATA
        } else {
            int value = 0;
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    float sx = warpData[count++];
                    float sy = warpData[count++];

                    int xint = floor(sx);
                    int yint = floor(sy);
                    int xfrac = (int) ((sx - xint) * precH);
                    int yfrac = (int) ((sy - yint) * precV);

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataInt;
                            }
                        }
                    } else {
                        xint -= leftPad;
                        yint -= topPad;

                        roiWeight = false;
                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                int x = xint + i;
                                int y = yint + j;
                                if (roiBounds.contains(x, y)) {
                                    roiWeight |= roiIter.getSample(x, y, 0) > 0;
                                }
                            }
                        }
                        //
                        // ROI check
                        //
                        //
                        if (roiWeight) {
                            for (int b = 0; b < dstBands; b++) {
                                for (int j = 0; j < kheight; j++) {
                                    //
                                    // NODATA check
                                    //
                                    //
                                    for (int i = 0; i < kwidth; i++) {
                                        value = iter.getSample(xint + i, yint + j, b);
                                        // If the value is a NODATA, is substituted with 0 inside the kernel
                                        if (noDataRange.contains(value)) {
                                            samples[j][i] = NODATA_VALUE;
                                        } else {
                                            samples[j][i] = value;
                                        }
                                    }
                                }

                                data[b][pixelOffset + bandOffsets[b]] = (interp.interpolate(
                                        samples, xfrac, yfrac));
                            }
                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataInt;
                            }
                        }
                    }

                    pixelOffset += pixelStride;
                }
            }
        }
        iter.done();
    }

    protected void computeRectFloat(PlanarImage src, RasterAccessor dst, final RandomIter roiIter,
            boolean roiContainsTile) {
        // Setting of the Random iterator keeping into account the presence of the Borderextender
        int minX, maxX, minY, maxY;
        RandomIter iter;
        if (extended) {
            // Creation of an iterator on the image extended by the padding factors
            iter = getRandomIterator(src, leftPad, rightPad, topPad, bottomPad, extender);
            // Definition of the image bounds
            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();

        } else {
            // Creation of an iterator on the image
            iter = getRandomIterator(src, null);
            // Definition of the image bounds
            minX = src.getMinX() + leftPad; // Left padding
            maxX = src.getMaxX() - rightPad; // Right padding
            minY = src.getMinY() + topPad; // Top padding
            maxY = src.getMaxY() - bottomPad; // Bottom padding
        }

        int kwidth = interp.getWidth();
        int kheight = interp.getHeight();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int lineStride = dst.getScanlineStride();
        int pixelStride = dst.getPixelStride();
        int[] bandOffsets = dst.getBandOffsets();
        float[][] data = dst.getFloatDataArrays();

        float[] warpData = new float[2 * dstWidth];

        float[][] samples = new float[kheight][kwidth];

        boolean roiWeight;

        int lineOffset = 0;

        // ONLY VALID DATA
        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    float sx = warpData[count++];
                    float sy = warpData[count++];

                    int xint = floor(sx);
                    int yint = floor(sy);
                    float xfrac = sx - xint;
                    float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataFloat;
                            }
                        }
                    } else {
                        xint -= leftPad;
                        yint -= topPad;

                        for (int b = 0; b < dstBands; b++) {
                            for (int j = 0; j < kheight; j++) {
                                for (int i = 0; i < kwidth; i++) {
                                    samples[j][i] = iter.getSampleFloat(xint + i, yint + j, b);
                                }
                            }

                            data[b][pixelOffset + bandOffsets[b]] = (interp.interpolate(samples,
                                    xfrac, yfrac));
                        }
                    }

                    pixelOffset += pixelStride;
                }
            }
            // ONLY ROI
        } else if (caseB) {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    float sx = warpData[count++];
                    float sy = warpData[count++];

                    int xint = floor(sx);
                    int yint = floor(sy);
                    float xfrac = sx - xint;
                    float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataFloat;
                            }
                        }
                    } else {
                        xint -= leftPad;
                        yint -= topPad;

                        roiWeight = false;
                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                int x = xint + i;
                                int y = yint + j;
                                if (roiBounds.contains(x, y)) {
                                    roiWeight |= roiIter.getSample(x, y, 0) > 0;
                                }
                            }
                        }
                        //
                        // ROI check
                        //
                        //
                        if (roiWeight) {
                            for (int b = 0; b < dstBands; b++) {
                                for (int j = 0; j < kheight; j++) {
                                    for (int i = 0; i < kwidth; i++) {
                                        samples[j][i] = iter.getSampleFloat(xint + i, yint + j, b);
                                    }
                                }

                                data[b][pixelOffset + bandOffsets[b]] = (interp.interpolate(
                                        samples, xfrac, yfrac));
                            }
                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataFloat;
                            }
                        }
                    }

                    pixelOffset += pixelStride;
                }
            }
            // ONLY NODATA
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            float value = 0;
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    float sx = warpData[count++];
                    float sy = warpData[count++];

                    int xint = floor(sx);
                    int yint = floor(sy);
                    float xfrac = sx - xint;
                    float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataFloat;
                            }
                        }
                    } else {
                        xint -= leftPad;
                        yint -= topPad;

                        for (int b = 0; b < dstBands; b++) {
                            for (int j = 0; j < kheight; j++) {
                                //
                                // NODATA check
                                //
                                //
                                for (int i = 0; i < kwidth; i++) {
                                    value = iter.getSampleFloat(xint + i, yint + j, b);
                                    // If the value is a NODATA, is substituted with 0 inside the kernel
                                    if (noDataRange.contains(value)) {
                                        samples[j][i] = NODATA_VALUE;
                                    } else {
                                        samples[j][i] = value;
                                    }
                                }
                            }
                            data[b][pixelOffset + bandOffsets[b]] = (interp.interpolate(samples,
                                    xfrac, yfrac));
                        }
                    }

                    pixelOffset += pixelStride;
                }
            }
            // BOTH ROI AND NODATA
        } else {
            float value = 0;
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    float sx = warpData[count++];
                    float sy = warpData[count++];

                    int xint = floor(sx);
                    int yint = floor(sy);
                    float xfrac = sx - xint;
                    float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataFloat;
                            }
                        }
                    } else {
                        xint -= leftPad;
                        yint -= topPad;

                        roiWeight = false;
                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                int x = xint + i;
                                int y = yint + j;
                                if (roiBounds.contains(x, y)) {
                                    roiWeight |= roiIter.getSample(x, y, 0) > 0;
                                }
                            }
                        }
                        //
                        // ROI check
                        //
                        //
                        if (roiWeight) {
                            for (int b = 0; b < dstBands; b++) {
                                for (int j = 0; j < kheight; j++) {
                                    //
                                    // NODATA check
                                    //
                                    //
                                    for (int i = 0; i < kwidth; i++) {
                                        value = iter.getSampleFloat(xint + i, yint + j, b);
                                        // If the value is a NODATA, is substituted with 0 inside the kernel
                                        if (noDataRange.contains(value)) {
                                            samples[j][i] = NODATA_VALUE;
                                        } else {
                                            samples[j][i] = value;
                                        }
                                    }
                                }

                                data[b][pixelOffset + bandOffsets[b]] = (interp.interpolate(
                                        samples, xfrac, yfrac));
                            }
                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataFloat;
                            }
                        }
                    }

                    pixelOffset += pixelStride;
                }
            }
        }
        iter.done();
    }

    protected void computeRectDouble(PlanarImage src, RasterAccessor dst, final RandomIter roiIter,
            boolean roiContainsTile) {
        // Setting of the Random iterator keeping into account the presence of the Borderextender
        int minX, maxX, minY, maxY;
        RandomIter iter;
        if (extended) {
            // Creation of an iterator on the image extended by the padding factors
            iter = getRandomIterator(src, leftPad, rightPad, topPad, bottomPad, extender);
            // Definition of the image bounds
            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();

        } else {
            // Creation of an iterator on the image
            iter = getRandomIterator(src, null);
            // Definition of the image bounds
            minX = src.getMinX() + leftPad; // Left padding
            maxX = src.getMaxX() - rightPad; // Right padding
            minY = src.getMinY() + topPad; // Top padding
            maxY = src.getMaxY() - bottomPad; // Bottom padding
        }

        int kwidth = interp.getWidth();
        int kheight = interp.getHeight();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int lineStride = dst.getScanlineStride();
        int pixelStride = dst.getPixelStride();
        int[] bandOffsets = dst.getBandOffsets();
        double[][] data = dst.getDoubleDataArrays();

        float[] warpData = new float[2 * dstWidth];

        double[][] samples = new double[kheight][kwidth];

        boolean roiWeight;

        int lineOffset = 0;

        // ONLY VALID DATA
        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    float sx = warpData[count++];
                    float sy = warpData[count++];

                    int xint = floor(sx);
                    int yint = floor(sy);
                    float xfrac = sx - xint;
                    float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataDouble;
                            }
                        }
                    } else {
                        xint -= leftPad;
                        yint -= topPad;

                        for (int b = 0; b < dstBands; b++) {
                            for (int j = 0; j < kheight; j++) {
                                for (int i = 0; i < kwidth; i++) {
                                    samples[j][i] = iter.getSampleDouble(xint + i, yint + j, b);
                                }
                            }

                            data[b][pixelOffset + bandOffsets[b]] = (interp.interpolate(samples,
                                    xfrac, yfrac));
                        }
                    }

                    pixelOffset += pixelStride;
                }
            }
            // ONLY ROI
        } else if (caseB) {
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    float sx = warpData[count++];
                    float sy = warpData[count++];

                    int xint = floor(sx);
                    int yint = floor(sy);
                    float xfrac = sx - xint;
                    float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataDouble;
                            }
                        }
                    } else {
                        xint -= leftPad;
                        yint -= topPad;

                        roiWeight = false;
                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                int x = xint + i;
                                int y = yint + j;
                                if (roiBounds.contains(x, y)) {
                                    roiWeight |= roiIter.getSample(x, y, 0) > 0;
                                }
                            }
                        }
                        //
                        // ROI check
                        //
                        //
                        if (roiWeight) {
                            for (int b = 0; b < dstBands; b++) {
                                for (int j = 0; j < kheight; j++) {
                                    for (int i = 0; i < kwidth; i++) {
                                        samples[j][i] = iter.getSampleDouble(xint + i, yint + j, b);
                                    }
                                }

                                data[b][pixelOffset + bandOffsets[b]] = (interp.interpolate(
                                        samples, xfrac, yfrac));
                            }
                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataDouble;
                            }
                        }
                    }

                    pixelOffset += pixelStride;
                }
            }
            // ONLY NODATA
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            double value = 0;
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    float sx = warpData[count++];
                    float sy = warpData[count++];

                    int xint = floor(sx);
                    int yint = floor(sy);
                    float xfrac = sx - xint;
                    float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataDouble;
                            }
                        }
                    } else {
                        xint -= leftPad;
                        yint -= topPad;

                        for (int b = 0; b < dstBands; b++) {
                            for (int j = 0; j < kheight; j++) {
                                //
                                // NODATA check
                                //
                                //
                                for (int i = 0; i < kwidth; i++) {
                                    value = iter.getSampleDouble(xint + i, yint + j, b);
                                    // If the value is a NODATA, is substituted with 0 inside the kernel
                                    if (noDataRange.contains(value)) {
                                        samples[j][i] = NODATA_VALUE;
                                    } else {
                                        samples[j][i] = value;
                                    }
                                }
                            }
                            data[b][pixelOffset + bandOffsets[b]] = (interp.interpolate(samples,
                                    xfrac, yfrac));
                        }
                    }

                    pixelOffset += pixelStride;
                }
            }
            // BOTH ROI AND NODATA
        } else {
            double value = 0;
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY() + h, dstWidth, 1, warpData);

                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    float sx = warpData[count++];
                    float sy = warpData[count++];

                    int xint = floor(sx);
                    int yint = floor(sy);
                    float xfrac = sx - xint;
                    float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataDouble;
                            }
                        }
                    } else {
                        xint -= leftPad;
                        yint -= topPad;

                        roiWeight = false;
                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                int x = xint + i;
                                int y = yint + j;
                                if (roiBounds.contains(x, y)) {
                                    roiWeight |= roiIter.getSample(x, y, 0) > 0;
                                }
                            }
                        }
                        //
                        // ROI check
                        //
                        //
                        if (roiWeight) {
                            for (int b = 0; b < dstBands; b++) {
                                for (int j = 0; j < kheight; j++) {
                                    //
                                    // NODATA check
                                    //
                                    //
                                    for (int i = 0; i < kwidth; i++) {
                                        value = iter.getSampleDouble(xint + i, yint + j, b);
                                        // If the value is a NODATA, is substituted with 0 inside the kernel
                                        if (noDataRange.contains(value)) {
                                            samples[j][i] = NODATA_VALUE;
                                        } else {
                                            samples[j][i] = value;
                                        }
                                    }
                                }

                                data[b][pixelOffset + bandOffsets[b]] = (interp.interpolate(
                                        samples, xfrac, yfrac));
                            }
                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = destinationNoDataDouble;
                            }
                        }
                    }

                    pixelOffset += pixelStride;
                }
            }
        }
        iter.done();
    }
}
