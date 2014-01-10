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
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.util.Map;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.RasterAccessor;
import javax.media.jai.Warp;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import com.sun.media.jai.util.ImageUtil;

/**
 * An <code>OpImage</code> implementing the general "Warp" operation as
 * described in <code>javax.media.jai.operator.WarpDescriptor</code>.
 * It supports all interpolation cases.
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

    /** Color table representing source's IndexColorModel. */
    private byte[][] ctable = null;

    /**
     * Constructs a ROIAwareWarpGeneralOpImage.
     *
     * @param source  The source image.
     * @param extender A BorderExtender, or null.
     * @param layout  The destination image layout.
     * @param warp    An object defining the warp algorithm.
     * @param interp  An object describing the interpolation method.
     */
    public WarpGeneralOpImage(RenderedImage source,
                              BorderExtender extender,
                              Map<?,?> config,
                              ImageLayout layout,
                              Warp warp,
                              Interpolation interp,
                              double[] backgroundValues,
                              ROI sourceROI) {
        super(source,
              layout,
              config,
              false,
              extender,
              interp,
              warp,
              backgroundValues,
              sourceROI);

        /*
         * If the source has IndexColorModel, get the RGB color table.
         * Note, in this case, the source should have an integral data type.
         * And dest always has data type byte.
         */
        ColorModel srcColorModel = source.getColorModel();
        if (srcColorModel instanceof IndexColorModel) {
            IndexColorModel icm = (IndexColorModel)srcColorModel;
            ctable = new byte[3][icm.getMapSize()];
            icm.getReds(ctable[0]);
            icm.getGreens(ctable[1]);
            icm.getBlues(ctable[2]);
        }
    }



    protected void computeRectByte(PlanarImage src, RasterAccessor dst, final ROI roiTile) {
        int lpad, rpad, tpad, bpad;
        if(interp != null) {
            lpad = interp.getLeftPadding();
            rpad = interp.getRightPadding();
            tpad = interp.getTopPadding();
            bpad = interp.getBottomPadding();
        } else {
            lpad = rpad = tpad = bpad = 0;
        }

        int minX, maxX, minY, maxY;
        RandomIter iter;
        if(extender != null) {
            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();
            Rectangle bounds = new Rectangle(src.getMinX() - lpad,
                                             src.getMinY() - tpad,
                                             src.getWidth() + lpad + rpad,
                                             src.getHeight() + tpad + bpad);
            iter = RandomIterFactory.create(src.getExtendedData(bounds, extender), bounds);
        } else {
            minX = src.getMinX() + lpad;
            maxX = src.getMaxX() - rpad;
            minY = src.getMinY() + tpad;
            maxY = src.getMaxY() - bpad;
            iter = RandomIterFactory.create(src, src.getBounds());

        }

        int kwidth = interp.getWidth();
        int kheight = interp.getHeight();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int lineStride = dst.getScanlineStride();
        int pixelStride = dst.getPixelStride();
        int[] bandOffsets = dst.getBandOffsets();
        byte[][] data = dst.getByteDataArrays();

        int precH = 1 << interp.getSubsampleBitsH();
        int precV = 1 << interp.getSubsampleBitsV();

        float[] warpData = new float[2 * dstWidth];

        int[][] samples = new int[kheight][kwidth];

        int lineOffset = 0;

        byte[] backgroundByte = new byte[dstBands];
	for (int i = 0; i < dstBands; i++)
	    backgroundByte[i] = (byte)backgroundValues[i];

        if (ctable == null) {	// source does not have IndexColorModel
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1, warpData);

                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    float sx = warpData[count++];
                    float sy = warpData[count++];

                    int xint = floor(sx);
                    int yint = floor(sy);
                    int xfrac = (int)((sx - xint) * precH);
                    int yfrac = (int)((sy - yint) * precV);

                    if (xint < minX || xint >= maxX ||
                        yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset+bandOffsets[b]] =
                                    backgroundByte[b];
                            }
                        }
                    } else {
                        xint -= lpad;
                        yint -= tpad;

                        for (int b = 0; b < dstBands; b++) {
                            for (int j = 0; j < kheight; j++) {
                                for (int i = 0; i < kwidth; i++) {
                                    samples[j][i] = iter.getSample(
                                        xint+i, yint+j, b) & 0xFF;
                                }
                            }

                            data[b][pixelOffset+bandOffsets[b]] =
                                ImageUtil.clampByte(
                                interp.interpolate(samples, xfrac, yfrac));
                        }
                    }

                    pixelOffset += pixelStride;
                }
            }
        } else {	// source has IndexColorModel
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1, warpData);

                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    float sx = warpData[count++];
                    float sy = warpData[count++];

                    int xint = floor(sx);
                    int yint = floor(sy);
                    int xfrac = (int)((sx - xint) * precH);
                    int yfrac = (int)((sy - yint) * precV);

                    if (xint < minX || xint >= maxX ||
                        yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset+bandOffsets[b]] =
                                    backgroundByte[b];
                            }
                        }
                    } else {
                        xint -= lpad;
                        yint -= tpad;

                        for (int b = 0; b < dstBands; b++) {
                            byte[] t = ctable[b];

                            for (int j = 0; j < kheight; j++) {
                                for (int i = 0; i < kwidth; i++) {
                                    samples[j][i] = t[iter.getSample(
                                        xint+i, yint+j, 0) & 0xFF] & 0xFF;
                                }
                            }

                            data[b][pixelOffset+bandOffsets[b]] =
                                ImageUtil.clampByte(
                                interp.interpolate(samples, xfrac, yfrac));
                        }
                    }

                    pixelOffset += pixelStride;
                }
            }
        }
        iter.done();
    }

    protected void computeRectUShort(PlanarImage src, RasterAccessor dst, final ROI roiTile) {
        int lpad, rpad, tpad, bpad;
        if(interp != null) {
            lpad = interp.getLeftPadding();
            rpad = interp.getRightPadding();
            tpad = interp.getTopPadding();
            bpad = interp.getBottomPadding();
        } else {
            lpad = rpad = tpad = bpad = 0;
        }

        int minX, maxX, minY, maxY;
        RandomIter iter;
        if(extender != null) {
            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();
            Rectangle bounds = new Rectangle(src.getMinX() - lpad,
                                             src.getMinY() - tpad,
                                             src.getWidth() + lpad + rpad,
                                             src.getHeight() + tpad + bpad);
            iter = RandomIterFactory.create(src.getExtendedData(bounds,
                                                                extender),
                                            bounds);
        } else {
            minX = src.getMinX() + lpad;
            maxX = src.getMaxX() - rpad;
            minY = src.getMinY() + tpad;
            maxY = src.getMaxY() - bpad;
            iter = RandomIterFactory.create(src, src.getBounds());
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

        int lineOffset = 0;

	short[] backgroundUShort = new short[dstBands];
	for (int i = 0; i < dstBands; i++)
	    backgroundUShort[i] = (short)backgroundValues[i];

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1, warpData);

            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                float sx = warpData[count++];
                float sy = warpData[count++];

                int xint = floor(sx);
                int yint = floor(sy);
                int xfrac = (int)((sx - xint) * precH);
                int yfrac = (int)((sy - yint) * precV);

                if (xint < minX || xint >= maxX ||
                    yint < minY || yint >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundUShort[b];
                        }
                    }
                } else {
                    xint -= lpad;
                    yint -= tpad;

                    for (int b = 0; b < dstBands; b++) {
                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                samples[j][i] = iter.getSample(
                                    xint+i, yint+j, b) & 0xFFFF;
                            }
                        }

                        data[b][pixelOffset+bandOffsets[b]] =
                            ImageUtil.clampUShort(
                            interp.interpolate(samples, xfrac, yfrac));
                    }
                }

                pixelOffset += pixelStride;
            }
        }
        iter.done();
    }

    protected void computeRectShort(PlanarImage src, RasterAccessor dst, final ROI roiTile) {
        int lpad, rpad, tpad, bpad;
        if(interp != null) {
            lpad = interp.getLeftPadding();
            rpad = interp.getRightPadding();
            tpad = interp.getTopPadding();
            bpad = interp.getBottomPadding();
        } else {
            lpad = rpad = tpad = bpad = 0;
        }

        int minX, maxX, minY, maxY;
        RandomIter iter;
        if(extender != null) {
            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();
            Rectangle bounds = new Rectangle(src.getMinX() - lpad,
                                             src.getMinY() - tpad,
                                             src.getWidth() + lpad + rpad,
                                             src.getHeight() + tpad + bpad);
            iter = RandomIterFactory.create(src.getExtendedData(bounds,
                                                                extender),
                                            bounds);
        } else {
            minX = src.getMinX() + lpad;
            maxX = src.getMaxX() - rpad;
            minY = src.getMinY() + tpad;
            maxY = src.getMaxY() - bpad;
            iter = RandomIterFactory.create(src, src.getBounds());
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

        int lineOffset = 0;

        short[] backgroundShort = new short[dstBands];
	for (int i = 0; i < dstBands; i++)
	    backgroundShort[i] = (short)backgroundValues[i];

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1, warpData);

            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                float sx = warpData[count++];
                float sy = warpData[count++];

                int xint = floor(sx);
                int yint = floor(sy);
                int xfrac = (int)((sx - xint) * precH);
                int yfrac = (int)((sy - yint) * precV);

                if (xint < minX || xint >= maxX ||
                    yint < minY || yint >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundShort[b];
                        }
                    }
                } else {
                    xint -= lpad;
                    yint -= tpad;

                    for (int b = 0; b < dstBands; b++) {
                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                samples[j][i] = iter.getSample(
                                    xint+i, yint+j, b);
                            }
                        }

                        data[b][pixelOffset+bandOffsets[b]] =
                            ImageUtil.clampShort(
                            interp.interpolate(samples, xfrac, yfrac));
                    }
                }

                pixelOffset += pixelStride;
            }
        }
        iter.done();
    }

    protected void computeRectInt(PlanarImage src, RasterAccessor dst, final ROI roiTile) {
        int lpad, rpad, tpad, bpad;
        if(interp != null) {
            lpad = interp.getLeftPadding();
            rpad = interp.getRightPadding();
            tpad = interp.getTopPadding();
            bpad = interp.getBottomPadding();
        } else {
            lpad = rpad = tpad = bpad = 0;
        }

        int minX, maxX, minY, maxY;
        RandomIter iter;
        if(extender != null) {
            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();
            Rectangle bounds = new Rectangle(src.getMinX() - lpad,
                                             src.getMinY() - tpad,
                                             src.getWidth() + lpad + rpad,
                                             src.getHeight() + tpad + bpad);
            iter = RandomIterFactory.create(src.getExtendedData(bounds,
                                                                extender),
                                            bounds);
        } else {
            minX = src.getMinX() + lpad;
            maxX = src.getMaxX() - rpad;
            minY = src.getMinY() + tpad;
            maxY = src.getMaxY() - bpad;
            iter = RandomIterFactory.create(src, src.getBounds());
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

        int lineOffset = 0;

	int[] backgroundInt = new int[dstBands];
	for (int i = 0; i < dstBands; i++)
	    backgroundInt[i] = (int)backgroundValues[i];

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1, warpData);

            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                float sx = warpData[count++];
                float sy = warpData[count++];

                int xint = floor(sx);
                int yint = floor(sy);
                int xfrac = (int)((sx - xint) * precH);
                int yfrac = (int)((sy - yint) * precV);

                if (xint < minX || xint >= maxX ||
                    yint < minY || yint >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundInt[b];
                        }
                    }
                } else {
                    xint -= lpad;
                    yint -= tpad;

                    for (int b = 0; b < dstBands; b++) {
                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                samples[j][i] = iter.getSample(
                                    xint+i, yint+j, b);
                            }
                        }

                        data[b][pixelOffset+bandOffsets[b]] =
                            interp.interpolate(samples, xfrac, yfrac);
                    }
                }

                pixelOffset += pixelStride;
            }
        }
        iter.done();
    }

    protected void computeRectFloat(PlanarImage src, RasterAccessor dst, final ROI roiTile) {
        int lpad, rpad, tpad, bpad;
        if(interp != null) {
            lpad = interp.getLeftPadding();
            rpad = interp.getRightPadding();
            tpad = interp.getTopPadding();
            bpad = interp.getBottomPadding();
        } else {
            lpad = rpad = tpad = bpad = 0;
        }

        int minX, maxX, minY, maxY;
        RandomIter iter;
        if(extender != null) {
            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();
            Rectangle bounds = new Rectangle(src.getMinX() - lpad,
                                             src.getMinY() - tpad,
                                             src.getWidth() + lpad + rpad,
                                             src.getHeight() + tpad + bpad);
            iter = RandomIterFactory.create(src.getExtendedData(bounds,
                                                                extender),
                                            bounds);
        } else {
            minX = src.getMinX() + lpad;
            maxX = src.getMaxX() - rpad;
            minY = src.getMinY() + tpad;
            maxY = src.getMaxY() - bpad;
            iter = RandomIterFactory.create(src, src.getBounds());
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

        int lineOffset = 0;

        float[] backgroundFloat = new float[dstBands];
	for (int i = 0; i < dstBands; i++)
	    backgroundFloat[i] = (float)backgroundValues[i];

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1, warpData);

            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                float sx = warpData[count++];
                float sy = warpData[count++];

                int xint = floor(sx);
                int yint = floor(sy);
                float xfrac = sx - xint;
                float yfrac = sy - yint;

                if (xint < minX || xint >= maxX ||
                    yint < minY || yint >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundFloat[b];
                        }
                    }
                } else {
                    xint -= lpad;
                    yint -= tpad;

                    for (int b = 0; b < dstBands; b++) {
                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                samples[j][i] = iter.getSampleFloat(
                                    xint+i, yint+j, b);
                            }
                        }

                        data[b][pixelOffset+bandOffsets[b]] =
                            interp.interpolate(samples, xfrac, yfrac);
                    }
                }

                pixelOffset += pixelStride;
            }
        }
        iter.done();
    }

    protected void computeRectDouble(PlanarImage src, RasterAccessor dst, final ROI roiTile) {
        int lpad, rpad, tpad, bpad;
        if(interp != null) {
            lpad = interp.getLeftPadding();
            rpad = interp.getRightPadding();
            tpad = interp.getTopPadding();
            bpad = interp.getBottomPadding();
        } else {
            lpad = rpad = tpad = bpad = 0;
        }

        int minX, maxX, minY, maxY;
        RandomIter iter;
        if(extender != null) {
            minX = src.getMinX();
            maxX = src.getMaxX();
            minY = src.getMinY();
            maxY = src.getMaxY();
            Rectangle bounds = new Rectangle(src.getMinX() - lpad,
                                             src.getMinY() - tpad,
                                             src.getWidth() + lpad + rpad,
                                             src.getHeight() + tpad + bpad);
            iter = RandomIterFactory.create(src.getExtendedData(bounds,
                                                                extender),
                                            bounds);
        } else {
            minX = src.getMinX() + lpad;
            maxX = src.getMaxX() - rpad;
            minY = src.getMinY() + tpad;
            maxY = src.getMaxY() - bpad;
            iter = RandomIterFactory.create(src, src.getBounds());
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

        int lineOffset = 0;

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1, warpData);

            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                float sx = warpData[count++];
                float sy = warpData[count++];

                int xint = floor(sx);
                int yint = floor(sy);
                float xfrac = sx - xint;
                float yfrac = sy - yint;

                if (xint < minX || xint >= maxX ||
                    yint < minY || yint >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundValues[b];
                        }
                    }
                } else {
                    xint -= lpad;
                    yint -= tpad;

                    for (int b = 0; b < dstBands; b++) {
                        for (int j = 0; j < kheight; j++) {
                            for (int i = 0; i < kwidth; i++) {
                                samples[j][i] = iter.getSampleDouble(
                                    xint+i, yint+j, b);
                            }
                        }

                        data[b][pixelOffset+bandOffsets[b]] =
                            interp.interpolate(samples, xfrac, yfrac);
                    }
                }

                pixelOffset += pixelStride;
            }
        }
        iter.done();
    }

    /** Returns the "floor" value of a float. */
    private static final int floor(float f) {
        return f >= 0 ? (int)f : (int)f - 1;
    }
}
