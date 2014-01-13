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

    private byte[] byteLookupTable;

    /**
     * Constructs a WarpGeneralOpImage.
     * 
     * @param source The source image.
     * @param extender A BorderExtender, or null.
     * @param layout The destination image layout.
     * @param warp An object defining the warp algorithm.
     * @param interp An object describing the interpolation method.
     */
    public WarpGeneralOpImage(RenderedImage source, BorderExtender extender, Map<?, ?> config,
            ImageLayout layout, Warp warp, Interpolation interp, double[] backgroundValues,
            ROI sourceROI) {
        super(source, layout, config, false, extender, interp, warp, backgroundValues, sourceROI);

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
    }

    protected void computeRectByte(PlanarImage src, RasterAccessor dst, final ROI roiTile) {
        int lpad, rpad, tpad, bpad;
        if (interp != null) {
            lpad = interp.getLeftPadding();
            rpad = interp.getRightPadding();
            tpad = interp.getTopPadding();
            bpad = interp.getBottomPadding();
        } else {
            lpad = rpad = tpad = bpad = 0;
        }

        int minX, maxX, minY, maxY;
        RandomIter iter;
        if (extended) {
            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();
            Rectangle bounds = new Rectangle(src.getMinX() - lpad, src.getMinY() - tpad,
                    src.getWidth() + lpad + rpad, src.getHeight() + tpad + bpad);
            iter = RandomIterFactory.create(src.getExtendedData(bounds, extender), bounds,
                    TILE_CACHED, ARRAY_CALC);
        } else {
            minX = src.getMinX() + lpad;
            maxX = src.getMaxX() - rpad;
            minY = src.getMinY() + tpad;
            maxY = src.getMaxY() - bpad;
            iter = RandomIterFactory.create(src, src.getBounds(), TILE_CACHED, ARRAY_CALC);

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

        boolean[][] roiWeight = new boolean[kheight][kwidth];

        if (ctable == null) { // source does not have IndexColorModel

            // ONLY VALID DATA
            if (caseA) {
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
                            xint -= lpad;
                            yint -= tpad;

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
                            xint -= lpad;
                            yint -= tpad;

                            for (int j = 0; j < kheight; j++) {
                                for (int i = 0; i < kwidth; i++) {
                                    roiWeight[i][j] = roiTile.contains(xint + i, yint + j);
                                }
                            }
                            //
                            // ROI check
                            //
                            //
                            if (checkROI(roiWeight)) {
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
            } else if (caseC) {
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
                            xint -= lpad;
                            yint -= tpad;

                            for (int j = 0; j < kheight; j++) {
                                for (int i = 0; i < kwidth; i++) {
                                    roiWeight[i][j] = roiTile.contains(xint + i, yint + j);
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
                            xint -= lpad;
                            yint -= tpad;

                            for (int j = 0; j < kheight; j++) {
                                for (int i = 0; i < kwidth; i++) {
                                    roiWeight[i][j] = roiTile.contains(xint + i, yint + j);
                                }
                            }
                            //
                            // ROI check
                            //
                            //
                            if (checkROI(roiWeight)) {
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
            if (caseA) {
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
                            xint -= lpad;
                            yint -= tpad;

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
                            xint -= lpad;
                            yint -= tpad;

                            for (int j = 0; j < kheight; j++) {
                                for (int i = 0; i < kwidth; i++) {
                                    roiWeight[i][j] = roiTile.contains(xint + i, yint + j);
                                }
                            }
                            //
                            // ROI check
                            //
                            //
                            if (checkROI(roiWeight)) {
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
            } else if (caseC) {
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
                            xint -= lpad;
                            yint -= tpad;
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
                            xint -= lpad;
                            yint -= tpad;

                            for (int j = 0; j < kheight; j++) {
                                for (int i = 0; i < kwidth; i++) {
                                    roiWeight[i][j] = roiTile.contains(xint + i, yint + j);
                                }
                            }
                            //
                            // ROI check
                            //
                            //
                            if (checkROI(roiWeight)) {
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

    protected void computeRectUShort(PlanarImage src, RasterAccessor dst, final ROI roiTile) {
        int lpad, rpad, tpad, bpad;
        if (interp != null) {
            lpad = interp.getLeftPadding();
            rpad = interp.getRightPadding();
            tpad = interp.getTopPadding();
            bpad = interp.getBottomPadding();
        } else {
            lpad = rpad = tpad = bpad = 0;
        }

        int minX, maxX, minY, maxY;
        RandomIter iter;
        if (extended) {
            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();
            Rectangle bounds = new Rectangle(src.getMinX() - lpad, src.getMinY() - tpad,
                    src.getWidth() + lpad + rpad, src.getHeight() + tpad + bpad);
            iter = RandomIterFactory.create(src.getExtendedData(bounds, extender), bounds,
                    TILE_CACHED, ARRAY_CALC);
        } else {
            minX = src.getMinX() + lpad;
            maxX = src.getMaxX() - rpad;
            minY = src.getMinY() + tpad;
            maxY = src.getMaxY() - bpad;
            iter = RandomIterFactory.create(src, src.getBounds(), TILE_CACHED, ARRAY_CALC);
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

        boolean[][] roiWeight = new boolean[kheight][kwidth];

        int lineOffset = 0;

        // ONLY VALID DATA
        if (caseA) {
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
                        xint -= lpad;
                        yint -= tpad;

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
                        xint -= lpad;
                        yint -= tpad;

                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                roiWeight[i][j] = roiTile.contains(xint + i, yint + j);
                            }
                        }
                        //
                        // ROI check
                        //
                        //
                        if (checkROI(roiWeight)) {
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
        } else if (caseC) {
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
                        xint -= lpad;
                        yint -= tpad;

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
                        xint -= lpad;
                        yint -= tpad;

                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                roiWeight[i][j] = roiTile.contains(xint + i, yint + j);
                            }
                        }
                        //
                        // ROI check
                        //
                        //
                        if (checkROI(roiWeight)) {
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

    protected void computeRectShort(PlanarImage src, RasterAccessor dst, final ROI roiTile) {
        int lpad, rpad, tpad, bpad;
        if (interp != null) {
            lpad = interp.getLeftPadding();
            rpad = interp.getRightPadding();
            tpad = interp.getTopPadding();
            bpad = interp.getBottomPadding();
        } else {
            lpad = rpad = tpad = bpad = 0;
        }

        int minX, maxX, minY, maxY;
        RandomIter iter;
        if (extended) {
            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();
            Rectangle bounds = new Rectangle(src.getMinX() - lpad, src.getMinY() - tpad,
                    src.getWidth() + lpad + rpad, src.getHeight() + tpad + bpad);
            iter = RandomIterFactory.create(src.getExtendedData(bounds, extender), bounds,
                    TILE_CACHED, ARRAY_CALC);
        } else {
            minX = src.getMinX() + lpad;
            maxX = src.getMaxX() - rpad;
            minY = src.getMinY() + tpad;
            maxY = src.getMaxY() - bpad;
            iter = RandomIterFactory.create(src, src.getBounds(), TILE_CACHED, ARRAY_CALC);
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

        boolean[][] roiWeight = new boolean[kheight][kwidth];

        int lineOffset = 0;

        // ONLY VALID DATA
        if (caseA) {
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
                        xint -= lpad;
                        yint -= tpad;

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
                        xint -= lpad;
                        yint -= tpad;

                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                roiWeight[i][j] = roiTile.contains(xint + i, yint + j);
                            }
                        }
                        //
                        // ROI check
                        //
                        //
                        if (checkROI(roiWeight)) {
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
        } else if (caseC) {
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
                        xint -= lpad;
                        yint -= tpad;

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
                        xint -= lpad;
                        yint -= tpad;

                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                roiWeight[i][j] = roiTile.contains(xint + i, yint + j);
                            }
                        }
                        //
                        // ROI check
                        //
                        //
                        if (checkROI(roiWeight)) {
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

    protected void computeRectInt(PlanarImage src, RasterAccessor dst, final ROI roiTile) {
        int lpad, rpad, tpad, bpad;
        if (interp != null) {
            lpad = interp.getLeftPadding();
            rpad = interp.getRightPadding();
            tpad = interp.getTopPadding();
            bpad = interp.getBottomPadding();
        } else {
            lpad = rpad = tpad = bpad = 0;
        }

        int minX, maxX, minY, maxY;
        RandomIter iter;
        if (extended) {
            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();
            Rectangle bounds = new Rectangle(src.getMinX() - lpad, src.getMinY() - tpad,
                    src.getWidth() + lpad + rpad, src.getHeight() + tpad + bpad);
            iter = RandomIterFactory.create(src.getExtendedData(bounds, extender), bounds,
                    TILE_CACHED, ARRAY_CALC);
        } else {
            minX = src.getMinX() + lpad;
            maxX = src.getMaxX() - rpad;
            minY = src.getMinY() + tpad;
            maxY = src.getMaxY() - bpad;
            iter = RandomIterFactory.create(src, src.getBounds(), TILE_CACHED, ARRAY_CALC);
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

        boolean[][] roiWeight = new boolean[kheight][kwidth];

        int lineOffset = 0;

        // ONLY VALID DATA
        if (caseA) {
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
                        xint -= lpad;
                        yint -= tpad;

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
                        xint -= lpad;
                        yint -= tpad;

                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                roiWeight[i][j] = roiTile.contains(xint + i, yint + j);
                            }
                        }
                        //
                        // ROI check
                        //
                        //
                        if (checkROI(roiWeight)) {
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
        } else if (caseC) {
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
                        xint -= lpad;
                        yint -= tpad;

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
                        xint -= lpad;
                        yint -= tpad;

                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                roiWeight[i][j] = roiTile.contains(xint + i, yint + j);
                            }
                        }
                        //
                        // ROI check
                        //
                        //
                        if (checkROI(roiWeight)) {
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

    protected void computeRectFloat(PlanarImage src, RasterAccessor dst, final ROI roiTile) {
        int lpad, rpad, tpad, bpad;
        if (interp != null) {
            lpad = interp.getLeftPadding();
            rpad = interp.getRightPadding();
            tpad = interp.getTopPadding();
            bpad = interp.getBottomPadding();
        } else {
            lpad = rpad = tpad = bpad = 0;
        }

        int minX, maxX, minY, maxY;
        RandomIter iter;
        if (extended) {
            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();
            Rectangle bounds = new Rectangle(src.getMinX() - lpad, src.getMinY() - tpad,
                    src.getWidth() + lpad + rpad, src.getHeight() + tpad + bpad);
            iter = RandomIterFactory.create(src.getExtendedData(bounds, extender), bounds,
                    TILE_CACHED, ARRAY_CALC);
        } else {
            minX = src.getMinX() + lpad;
            maxX = src.getMaxX() - rpad;
            minY = src.getMinY() + tpad;
            maxY = src.getMaxY() - bpad;
            iter = RandomIterFactory.create(src, src.getBounds(), TILE_CACHED, ARRAY_CALC);
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

        boolean[][] roiWeight = new boolean[kheight][kwidth];

        int lineOffset = 0;

        // ONLY VALID DATA
        if (caseA) {
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
                        xint -= lpad;
                        yint -= tpad;

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
                        xint -= lpad;
                        yint -= tpad;

                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                roiWeight[i][j] = roiTile.contains(xint + i, yint + j);
                            }
                        }
                        //
                        // ROI check
                        //
                        //
                        if (checkROI(roiWeight)) {
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
        } else if (caseC) {
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
                        xint -= lpad;
                        yint -= tpad;

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
                        xint -= lpad;
                        yint -= tpad;

                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                roiWeight[i][j] = roiTile.contains(xint + i, yint + j);
                            }
                        }
                        //
                        // ROI check
                        //
                        //
                        if (checkROI(roiWeight)) {
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

    protected void computeRectDouble(PlanarImage src, RasterAccessor dst, final ROI roiTile) {
        int lpad, rpad, tpad, bpad;
        if (interp != null) {
            lpad = interp.getLeftPadding();
            rpad = interp.getRightPadding();
            tpad = interp.getTopPadding();
            bpad = interp.getBottomPadding();
        } else {
            lpad = rpad = tpad = bpad = 0;
        }

        int minX, maxX, minY, maxY;
        RandomIter iter;
        if (extended) {
            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();
            Rectangle bounds = new Rectangle(src.getMinX() - lpad, src.getMinY() - tpad,
                    src.getWidth() + lpad + rpad, src.getHeight() + tpad + bpad);
            iter = RandomIterFactory.create(src.getExtendedData(bounds, extender), bounds,
                    TILE_CACHED, ARRAY_CALC);
        } else {
            minX = src.getMinX() + lpad;
            maxX = src.getMaxX() - rpad;
            minY = src.getMinY() + tpad;
            maxY = src.getMaxY() - bpad;
            iter = RandomIterFactory.create(src, src.getBounds(), TILE_CACHED, ARRAY_CALC);
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

        boolean[][] roiWeight = new boolean[kheight][kwidth];

        int lineOffset = 0;

        // ONLY VALID DATA
        if (caseA) {
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
                        xint -= lpad;
                        yint -= tpad;

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
                        xint -= lpad;
                        yint -= tpad;

                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                roiWeight[i][j] = roiTile.contains(xint + i, yint + j);
                            }
                        }
                        //
                        // ROI check
                        //
                        //
                        if (checkROI(roiWeight)) {
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
        } else if (caseC) {
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
                        xint -= lpad;
                        yint -= tpad;

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
                        xint -= lpad;
                        yint -= tpad;

                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                roiWeight[i][j] = roiTile.contains(xint + i, yint + j);
                            }
                        }
                        //
                        // ROI check
                        //
                        //
                        if (checkROI(roiWeight)) {
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

    /**
     * Check if all kernel pixels are outside the ROI.
     * 
     * @param roiWeight
     * @return
     */
    private boolean checkROI(boolean[][] roiWeight) {
        // Kernel dimensions
        int roiH = roiWeight.length;
        int roiW = roiWeight[0].length;
        // Check if at least if one pixel is inside the ROI
        for (int j = 0; j < roiH; j++) {
            for (int i = 0; i < roiW; i++) {
                if (roiWeight[j][i]) {
                    return true;
                }
            }
        }
        // If no pixel is inside the ROI, then false is returned
        return false;

    }

    /** Returns the "floor" value of a float. */
    private static final int floor(float f) {
        return f >= 0 ? (int) f : (int) f - 1;
    }
}
