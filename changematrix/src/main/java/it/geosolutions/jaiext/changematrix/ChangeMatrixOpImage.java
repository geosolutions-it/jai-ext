/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2011, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2008-2011 TOPP - www.openplans.org.
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package it.geosolutions.jaiext.changematrix;

import it.geosolutions.jaiext.changematrix.ChangeMatrixDescriptor.ChangeMatrix;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Map;

import javax.media.jai.AreaOpImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.PointOpImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;

/**
 * An operator to calculate change in pixels between two classified images relying on the {@link ChangeMatrix} element.
 * 
 * @see ChangeMatrixDescriptor Description
 * @author Simone Giannecchini, GeoSolutions SAS
 * @since 9.0
 */
@SuppressWarnings("unchecked")
public class ChangeMatrixOpImage extends PointOpImage {

    private final ROI roi;

    private final ChangeMatrix result;

    /**
     * Creates a new instance.
     * 
     * @param now the source image
     * @param config configurable attributes of the image (see {@link AreaOpImage})
     * @param layout an optional ImageLayout object; if the layout specifies a SampleModel and / or ColorModel that are invalid for the requested
     *        statistics (e.g. wrong number of bands) these will be overridden
     * @param bandSource the source image band to process
     * @param bandReference the reference image band to process
     * @param roi an optional {@code ROI} or {@code null}
     * @param result a {@link ChangeMatrix} object to compute and hold the results
     * @throws IllegalArgumentException if the ROI's bounds do not contain the entire source image
     * @see ChangeMatrixDescriptor
     * @see ChangeMatrix
     */
    @SuppressWarnings("rawtypes")
    public ChangeMatrixOpImage(final RenderedImage reference, final RenderedImage now,
            final Map config, final ImageLayout layout, ROI roi, final ChangeMatrix result) {

        super(reference, now, layout, config, true);

        this.result = result;

        this.roi = roi;
        if (roi != null) {
            // check that the ROI contains the source image bounds
            final Rectangle sourceBounds = new Rectangle(now.getMinX(), now.getMinY(),
                    now.getWidth(), now.getHeight());

            if (!roi.intersects(sourceBounds)) {
                throw new IllegalArgumentException(
                        "The bounds of the ROI must intersect the source image");
            }
            // massage roi
            roi = roi.intersect(new ROIShape(sourceBounds));
        }

        // where do we put the final elements?
    }

    /**
     * Multiplies the pixel values of two source images within a specified rectangle.
     * 
     * @param sources Cobbled sources, guaranteed to provide all the source data necessary for computing the rectangle.
     * @param dest The tile containing the rectangle to be computed.
     * @param destRect The rectangle within the tile to be computed.
     */
    protected void computeRect(final Raster[] sources, final WritableRaster dest,
            final Rectangle destRect) {
        // Retrieve format tags.
        final RasterFormatTag[] formatTags = getFormatTags();

        /* For PointOpImage, srcRect = destRect. */
        final RasterAccessor s1 = new RasterAccessor(sources[0], destRect, formatTags[0],
                getSourceImage(0).getColorModel());
        final RasterAccessor s2 = new RasterAccessor(sources[1], destRect, formatTags[1],
                getSourceImage(1).getColorModel());
        final RasterAccessor d = new RasterAccessor(dest, destRect, formatTags[2], getColorModel());

        final int src1LineStride = s1.getScanlineStride();
        final int src1PixelStride = s1.getPixelStride();
        final int[] src1BandOffsets = s1.getBandOffsets();

        final int src2LineStride = s2.getScanlineStride();
        final int src2PixelStride = s2.getPixelStride();
        final int[] src2BandOffsets = s2.getBandOffsets();

        final int dstNumBands = d.getNumBands();
        final int dstWidth = d.getWidth();
        final int dstHeight = d.getHeight();
        final int dstLineStride = d.getScanlineStride();
        final int dstPixelStride = d.getPixelStride();
        final int[] dstBandOffsets = d.getBandOffsets();

        switch (s1.getDataType()) {

        case DataBuffer.TYPE_BYTE:
            byteLoop(dstNumBands, dstWidth, dstHeight, sources[0].getMinX(), sources[0].getMinY(),
                    src1LineStride, src1PixelStride, src1BandOffsets, s1.getByteDataArrays(),
                    src2LineStride, src2PixelStride, src2BandOffsets, s2.getByteDataArrays(),
                    dstLineStride, dstPixelStride, dstBandOffsets, d.getByteDataArrays());
            break;

        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            shortLoop(dstNumBands, dstWidth, dstHeight, sources[0].getMinX(), sources[0].getMinY(),
                    src1LineStride, src1PixelStride, src1BandOffsets, s1.getShortDataArrays(),
                    src2LineStride, src2PixelStride, src2BandOffsets, s2.getShortDataArrays(),
                    dstLineStride, dstPixelStride, dstBandOffsets, d.getShortDataArrays());
            break;

        case DataBuffer.TYPE_INT:
            intLoop(dstNumBands, dstWidth, dstHeight, sources[0].getMinX(), sources[0].getMinY(),
                    src1LineStride, src1PixelStride, src1BandOffsets, s1.getIntDataArrays(),
                    src2LineStride, src2PixelStride, src2BandOffsets, s2.getIntDataArrays(),
                    dstLineStride, dstPixelStride, dstBandOffsets, d.getIntDataArrays());
            break;
        }
        d.copyBinaryDataToRaster();
    }

    private void intLoop(final int dstNumBands, final int dstWidth, final int dstHeight,
            final int src1MinX, final int src1MinY, final int src1LineStride,
            final int src1PixelStride, final int[] src1BandOffsets, final int[][] src1Data,
            final int src2LineStride, final int src2PixelStride, final int[] src2BandOffsets,
            final int[][] src2Data, final int dstLineStride, final int dstPixelStride,
            final int[] dstBandOffsets, final int[][] dstData) {

        for (int b = 0; b < dstNumBands; b++) {
            final int[] s1 = src1Data[b];
            final int[] s2 = src2Data[b];
            final int[] d = dstData[b];
            int src1LineOffset = src1BandOffsets[b];
            int src2LineOffset = src2BandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int src1PixelOffset = src1LineOffset;
                int src2PixelOffset = src2LineOffset;
                int dstPixelOffset = dstLineOffset;
                src1LineOffset += src1LineStride;
                src2LineOffset += src2LineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    final int before = (s1[src1PixelOffset]);
                    final int after = (s2[src1PixelOffset]);
                    d[dstPixelOffset] = before == after ? 0 : 1;
                    final int x = src1MinX + (src1PixelOffset % src1LineStride) / src1PixelStride;
                    final int y = src1MinY + (src1PixelOffset / src1LineStride);
                    if (roi == null || roi.contains(x, y)) {
                        result.registerPair(s1[src1PixelOffset], s2[src2PixelOffset]);

                    } else {
                        // we of course use 0 as NoData
                        d[dstPixelOffset] = (byte) 0;
                    }

                    src1PixelOffset += src1PixelStride;
                    src2PixelOffset += src2PixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    private void byteLoop(final int dstNumBands, final int dstWidth, final int dstHeight,
            final int src1MinX, final int src1MinY, final int src1LineStride,
            final int src1PixelStride, final int[] src1BandOffsets, final byte[][] src1Data,
            final int src2LineStride, final int src2PixelStride, final int[] src2BandOffsets,
            final byte[][] src2Data, final int dstLineStride, final int dstPixelStride,
            final int[] dstBandOffsets, final byte[][] dstData) {

        for (int b = 0; b < dstNumBands; b++) {
            final byte[] s1 = src1Data[b];
            final byte[] s2 = src2Data[b];
            final byte[] d = dstData[b];
            int src1LineOffset = src1BandOffsets[b];
            int src2LineOffset = src2BandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int src1PixelOffset = src1LineOffset;
                int src2PixelOffset = src2LineOffset;
                int dstPixelOffset = dstLineOffset;
                src1LineOffset += src1LineStride;
                src2LineOffset += src2LineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    final byte before = (byte) (s1[src1PixelOffset]);
                    final byte after = (byte) (s2[src1PixelOffset]);
                    d[dstPixelOffset] = before == after ? (byte) 0 : (byte) 1;

                    final int x = src1MinX + (src1PixelOffset % src1LineStride) / src1PixelStride;
                    final int y = src1MinY + (src1PixelOffset / src1LineStride);
                    if (roi == null || roi.contains(x, y)) {
                        result.registerPair(s1[src1PixelOffset], s2[src2PixelOffset]);

                    } else {
                        // we of course use 0 as NoData
                        d[dstPixelOffset] = (byte) 0;
                    }
                    src1PixelOffset += src1PixelStride;
                    src2PixelOffset += src2PixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    private void shortLoop(final int dstNumBands, final int dstWidth, final int dstHeight,
            final int src1MinX, final int src1MinY, final int src1LineStride,
            final int src1PixelStride, final int[] src1BandOffsets, final short[][] src1Data,
            final int src2LineStride, final int src2PixelStride, final int[] src2BandOffsets,
            final short[][] src2Data, final int dstLineStride, final int dstPixelStride,
            final int[] dstBandOffsets, final short[][] dstData) {

        for (int b = 0; b < dstNumBands; b++) {
            final short[] s1 = src1Data[b];
            final short[] s2 = src2Data[b];
            final short[] d = dstData[b];
            int src1LineOffset = src1BandOffsets[b];
            int src2LineOffset = src2BandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int src1PixelOffset = src1LineOffset;
                int src2PixelOffset = src2LineOffset;
                int dstPixelOffset = dstLineOffset;
                src1LineOffset += src1LineStride;
                src2LineOffset += src2LineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    final short before = (short) (s1[src1PixelOffset]);
                    final short after = (short) (s2[src1PixelOffset]);
                    d[dstPixelOffset] = before == after ? (short) 0 : (short) 1;
                    final int x = src1MinX + (src1PixelOffset % src1LineStride) / src1PixelStride;
                    final int y = src1MinY + (src1PixelOffset / src1LineStride);
                    if (roi == null || roi.contains(x, y)) {
                        result.registerPair(s1[src1PixelOffset], s2[src2PixelOffset]);

                    } else {
                        // we of course use 0 as NoData
                        d[dstPixelOffset] = (byte) 0;
                    }
                    src1PixelOffset += src1PixelStride;
                    src2PixelOffset += src2PixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }
}
