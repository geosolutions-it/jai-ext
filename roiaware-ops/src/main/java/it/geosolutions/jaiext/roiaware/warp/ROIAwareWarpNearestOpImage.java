/*
 * $RCSfile: ROIAwareWarpNearestOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:47 $
 * $State: Exp $
 */
package it.geosolutions.jaiext.roiaware.warp;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Map;

import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.Warp;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

/**
 * An <code>OpImage</code> implementing the general "Warp" operation as
 * described in <code>javax.media.jai.operator.WarpDescriptor</code>.
 * It supports the nearest-neighbor interpolation.
 *
 * <p>The layout for the destination image may be specified via the
 * <code>ImageLayout</code> parameter. However, only those settings
 * suitable for this operation will be used. The unsuitable settings
 * will be replaced by default suitable values.
 *
 * @since EA2
 * @see javax.media.jai.Warp
 * @see javax.media.jai.WarpOpImage
 * @see javax.media.jai.operator.WarpDescriptor
 * @see ROIAwareWarpRIF
 *
 */
@SuppressWarnings("unchecked")
final class ROIAwareWarpNearestOpImage extends ROIAwareWarpOpImage {

    /**
     * Constructs a ROIAwareWarpNearestOpImage.
     *
     * @param source  The source image.
     * @param layout  The destination image layout.
     * @param warp    An object defining the warp algorithm.
     * @param interp  An object describing the interpolation method.
     */
    public ROIAwareWarpNearestOpImage(final RenderedImage source,
                              final Map<?,?> config,
                              final ImageLayout layout,
                              final Warp warp,
                              final Interpolation interp,
                              final double[] backgroundValues,
                              final ROI sourceROI) {
        super(source,
              layout,
              config,
              false,
              null,   // extender
              interp,
              warp,
              backgroundValues,
              sourceROI);

        /*
         * If the source has IndexColorModel, override the default setting
         * in OpImage. The dest shall have exactly the same SampleModel and
         * ColorModel as the source.
         * Note, in this case, the source should have an integral data type.
         */
        final ColorModel srcColorModel = source.getColorModel();
        if (srcColorModel instanceof IndexColorModel) {
             sampleModel = source.getSampleModel().createCompatibleSampleModel(
                                                   tileWidth, tileHeight);
             colorModel = srcColorModel;
        }
    }

    /** Warps a rectangle. */
    protected void computeRect(final PlanarImage[] sources,
                               final WritableRaster dest,
                               final Rectangle destRect) {
        // Retrieve format tags.
        final RasterFormatTag[] formatTags = getFormatTags();

        final RasterAccessor d = new RasterAccessor(dest, destRect,
                                              formatTags[1], getColorModel());

        switch (d.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            computeRectByte(sources[0], d);
            break;
        case DataBuffer.TYPE_USHORT:
            computeRectUShort(sources[0], d);
            break;
        case DataBuffer.TYPE_SHORT:
            computeRectShort(sources[0], d);
            break;
        case DataBuffer.TYPE_INT:
            computeRectInt(sources[0], d);
            break;
        case DataBuffer.TYPE_FLOAT:
            computeRectFloat(sources[0], d);
            break;
        case DataBuffer.TYPE_DOUBLE:
            computeRectDouble(sources[0], d);
            break;
        }

        if (d.isDataCopy()) {
            d.clampDataArrays();
            d.copyDataToRaster();
        }
    }

    private void computeRectByte(final PlanarImage src, final RasterAccessor dst) {
        final RandomIter iter = RandomIterFactory.create(src, src.getBounds());

        final int minX = src.getMinX();
        final int maxX = src.getMaxX();
        final int minY = src.getMinY();
        final int maxY = src.getMaxY();

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int lineStride = dst.getScanlineStride();
        final int pixelStride = dst.getPixelStride();
        final int[] bandOffsets = dst.getBandOffsets();
        final byte[][] data = dst.getByteDataArrays();

        final float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

	final byte[] backgroundByte = new byte[dstBands];
	for (int i = 0; i < dstBands; i++)
	    backgroundByte[i] = (byte)backgroundValues[i];

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1,
                          warpData);
            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                /*
                 * The warp object subtract 0.5 from backward mapped
                 * source coordinate. Need to do a round to get the
                 * nearest neighbor. This is different from the standard
                 * nearest implementation.
                 */
                final int sx = round(warpData[count++]);
                final int sy = round(warpData[count++]);

                if (sx < minX || sx >= maxX || sy < minY || sy >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundByte[b];
                        }
                    }
                } else {
                    if(hasROI){
                        // SG if we falls outside the roi we use the background value
                        if(!roiBounds.contains(sx,sy)){
                            /* Fill with a background color. */
                            if (setBackground) {
                                for (int b = 0; b < dstBands; b++) {
                                    data[b][pixelOffset+bandOffsets[b]] =
                                        backgroundByte[b];
                                }
                            }
                        } else {
                            // SG if we falls outside the roi we use the background value
                            final int wx= iterRoi.getSample(sx, sy, 0)& 0xFF;
                            final boolean insideROI=wx==1;
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset+bandOffsets[b]] =insideROI?
                                    (byte)(iter.getSample(sx, sy, b) & 0xFF):backgroundByte[b];
                            }
                        }
                    } else {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =(byte)(iter.getSample(sx, sy, b) & 0xFF);
                        }
                    }

                }

                pixelOffset += pixelStride;
            }
        }
        iter.done();
    }

    private void computeRectUShort(final PlanarImage src, final RasterAccessor dst) {
        final RandomIter iter = RandomIterFactory.create(src, src.getBounds());

        final int minX = src.getMinX();
        final int maxX = src.getMaxX();
        final int minY = src.getMinY();
        final int maxY = src.getMaxY();

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int lineStride = dst.getScanlineStride();
        final int pixelStride = dst.getPixelStride();
        final int[] bandOffsets = dst.getBandOffsets();
        final short[][] data = dst.getShortDataArrays();

        final float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

	final short[] backgroundUShort = new short[dstBands];
	for (int i = 0; i < dstBands; i++)
	    backgroundUShort[i] = (short)backgroundValues[i];

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1,
                          warpData);
            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                /*
                 * The warp object subtract 0.5 from backward mapped
                 * source coordinate. Need to do a round to get the
                 * nearest neighbor. This is different from the standard
                 * nearest implementation.
                 */
                final int sx = round(warpData[count++]);
                final int sy = round(warpData[count++]);

                if (sx < minX || sx >= maxX || sy < minY || sy >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundUShort[b];
                        }
                    }
                } else {
                    for (int b = 0; b < dstBands; b++) {
                        data[b][pixelOffset+bandOffsets[b]] =
                            (short)(iter.getSample(sx, sy, b) & 0xFFFF);
                    }
                }

                pixelOffset += pixelStride;
            }
        }
        iter.done();
    }

    private void computeRectShort(final PlanarImage src, final RasterAccessor dst) {
        final RandomIter iter = RandomIterFactory.create(src, src.getBounds());

        final int minX = src.getMinX();
        final int maxX = src.getMaxX();
        final int minY = src.getMinY();
        final int maxY = src.getMaxY();

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int lineStride = dst.getScanlineStride();
        final int pixelStride = dst.getPixelStride();
        final int[] bandOffsets = dst.getBandOffsets();
        final short[][] data = dst.getShortDataArrays();

        final float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

        final short[] backgroundShort = new short[dstBands];
	for (int i = 0; i < dstBands; i++)
	    backgroundShort[i] = (short)backgroundValues[i];

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1,
                          warpData);
            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                /*
                 * The warp object subtract 0.5 from backward mapped
                 * source coordinate. Need to do a round to get the
                 * nearest neighbor. This is different from the standard
                 * nearest implementation.
                 */
                final int sx = round(warpData[count++]);
                final int sy = round(warpData[count++]);

                if (sx < minX || sx >= maxX || sy < minY || sy >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundShort[b];
                        }
                    }
                } else {
                    for (int b = 0; b < dstBands; b++) {
                        data[b][pixelOffset+bandOffsets[b]] =
                            (short)iter.getSample(sx, sy, b);
                    }
                }

                pixelOffset += pixelStride;
            }
        }
        iter.done();
    }

    private void computeRectInt(final PlanarImage src, final RasterAccessor dst) {
        final RandomIter iter = RandomIterFactory.create(src, src.getBounds());

        final int minX = src.getMinX();
        final int maxX = src.getMaxX();
        final int minY = src.getMinY();
        final int maxY = src.getMaxY();

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int lineStride = dst.getScanlineStride();
        final int pixelStride = dst.getPixelStride();
        final int[] bandOffsets = dst.getBandOffsets();
        final int[][] data = dst.getIntDataArrays();

        final float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

	final int[] backgroundInt = new int[dstBands];
	for (int i = 0; i < dstBands; i++)
	    backgroundInt[i] = (int)backgroundValues[i];

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1,
                          warpData);
            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                /*
                 * The warp object subtract 0.5 from backward mapped
                 * source coordinate. Need to do a round to get the
                 * nearest neighbor. This is different from the standard
                 * nearest implementation.
                 */
                final int sx = round(warpData[count++]);
                final int sy = round(warpData[count++]);

                if (sx < minX || sx >= maxX || sy < minY || sy >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundInt[b];
                        }
                    }
                } else {
                    for (int b = 0; b < dstBands; b++) {
                        data[b][pixelOffset+bandOffsets[b]] =
                            iter.getSample(sx, sy, b);
                    }
                }

                pixelOffset += pixelStride;
            }
        }
        iter.done();
    }

    private void computeRectFloat(final PlanarImage src, final RasterAccessor dst) {
        final RandomIter iter = RandomIterFactory.create(src, src.getBounds());

        final int minX = src.getMinX();
        final int maxX = src.getMaxX();
        final int minY = src.getMinY();
        final int maxY = src.getMaxY();

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int lineStride = dst.getScanlineStride();
        final int pixelStride = dst.getPixelStride();
        final int[] bandOffsets = dst.getBandOffsets();
        final float[][] data = dst.getFloatDataArrays();

        final float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

	final float[] backgroundFloat = new float[dstBands];
	for (int i = 0; i < dstBands; i++)
	    backgroundFloat[i] = (float)backgroundValues[i];

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1,
                          warpData);
            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                /*
                 * The warp object subtract 0.5 from backward mapped
                 * source coordinate. Need to do a round to get the
                 * nearest neighbor. This is different from the standard
                 * nearest implementation.
                 */
                final int sx = round(warpData[count++]);
                final int sy = round(warpData[count++]);

                if (sx < minX || sx >= maxX || sy < minY || sy >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundFloat[b];
                        }
                    }
                } else {
                    for (int b = 0; b < dstBands; b++) {
                        data[b][pixelOffset+bandOffsets[b]] =
                            iter.getSampleFloat(sx, sy, b);
                    }
                }

                pixelOffset += pixelStride;
            }
        }
        iter.done();
    }

    private void computeRectDouble(final PlanarImage src, final RasterAccessor dst) {
        final RandomIter iter = RandomIterFactory.create(src, src.getBounds());

        final int minX = src.getMinX();
        final int maxX = src.getMaxX();
        final int minY = src.getMinY();
        final int maxY = src.getMaxY();

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int lineStride = dst.getScanlineStride();
        final int pixelStride = dst.getPixelStride();
        final int[] bandOffsets = dst.getBandOffsets();
        final double[][] data = dst.getDoubleDataArrays();

        final float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1,
                          warpData);
            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                /*
                 * The warp object subtract 0.5 from backward mapped
                 * source coordinate. Need to do a round to get the
                 * nearest neighbor. This is different from the standard
                 * nearest implementation.
                 */
                final int sx = round(warpData[count++]);
                final int sy = round(warpData[count++]);

                if (sx < minX || sx >= maxX || sy < minY || sy >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundValues[b];
                        }
                    }
                } else {
                    for (int b = 0; b < dstBands; b++) {
                        data[b][pixelOffset+bandOffsets[b]] =
                            iter.getSampleDouble(sx, sy, b);
                    }
                }

                pixelOffset += pixelStride;
            }
        }
        iter.done();
    }

    /** Returns the "round" value of a float. */
    private static final int round(final float f) {
        return f >= 0 ? (int)(f + 0.5F) : (int)(f - 0.5F);
    }
}
