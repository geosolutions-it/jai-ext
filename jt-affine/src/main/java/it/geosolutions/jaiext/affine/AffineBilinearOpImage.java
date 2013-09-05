package it.geosolutions.jaiext.affine;

import it.geosolutions.jaiext.interpolators.InterpolationBilinear;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;

public class AffineBilinearOpImage extends AffineOpImage {

    /** Nearest-Neighbor interpolator */
    protected InterpolationBilinear interpB = null;

    /** Byte lookuptable used if no data are present */
    protected final byte[] byteLookupTable = new byte[255];

    /** ROI extender */
    final static BorderExtender roiExtender = BorderExtender
            .createInstance(BorderExtender.BORDER_ZERO);

    /** Value indicating if destination No Data must be set if the pixel is outside the source rectangle */
    private boolean setDestinationNoData;

    public AffineBilinearOpImage(RenderedImage source, BorderExtender extender, Map config,
            ImageLayout layout, AffineTransform transform, Interpolation interp,
            double[] backgroundValues, boolean setDestinationNoData, boolean useROIAccessor) {
        super(source, extender, config, layout, transform, interp, null);
        affineOpInitialization(source, interp, layout, useROIAccessor, setDestinationNoData);
    }

    private void affineOpInitialization(RenderedImage source, Interpolation interp,
            ImageLayout layout, boolean useROIAccessor, boolean setDestinationNoData) {

        SampleModel sm = source.getSampleModel();

        // If the source has an IndexColorModel, override the default setting
        // in OpImage. The dest shall have exactly the same SampleModel and
        // ColorModel as the source.
        // Note, in this case, the source should have an integral data type.
        ColorModel srcColorModel = source.getColorModel();
        if (srcColorModel instanceof IndexColorModel) {
            sampleModel = source.getSampleModel()
                    .createCompatibleSampleModel(tileWidth, tileHeight);
            colorModel = srcColorModel;
        }

        // Source image data Type
        int srcDataType = sm.getDataType();

        // If both roiBounds and roiIter are not null, they are used in calculation
        if (interp instanceof InterpolationBilinear) {
            interpB = (InterpolationBilinear) interp;
            this.interp = interpB;
            interpB.setROIdata(roiBounds, roiIter);
            noData = interpB.getNoDataRange();
            this.useROIAccessor = false;
            if (noData != null) {
                hasNoData = true;
                destinationNoDataDouble = interpB.getDestinationNoData();
            } else if (hasROI) {
                destinationNoDataDouble = interpB.getDestinationNoData();
                this.useROIAccessor = useROIAccessor;
            }
        }

        // Creation of the destination background values
        int srcNumBands = source.getSampleModel().getNumBands();
        double[] background = new double[srcNumBands];
        for (int i = 0; i < srcNumBands; i++) {
            background[i] = destinationNoDataDouble;
        }
        this.backgroundValues = background;

        // destination No Data set
        this.setDestinationNoData = setDestinationNoData;
        this.setBackground = setDestinationNoData;
        // Selection of the destination No Data
        switch (srcDataType) {
        case DataBuffer.TYPE_BYTE:
            destinationNoDataByte = (byte) (((byte) destinationNoDataDouble) & 0xff);
            // Creation of a lookuptable containing the values to use for no data
            if (hasNoData) {

                for (int i = 0; i < byteLookupTable.length; i++) {
                    byte value = (byte) i;
                    if (noData.contains(value)) {
                        if (setDestinationNoData) {
                            byteLookupTable[i] = destinationNoDataByte;
                        } else {
                            byteLookupTable[i] = 0;
                        }
                    } else {
                        byteLookupTable[i] = value;
                    }
                }
            }
            break;
        case DataBuffer.TYPE_USHORT:
            destinationNoDataUShort = (short) (((short) destinationNoDataDouble) & 0xffff);
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

    /** Method for evaluating the destination image tile without ROI */
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        computeRect(sources, dest, destRect, null);
    }

    @Override
    /** Method for evaluating the destination image tile with ROI */
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect,
            Raster[] rois) { // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();
        // Source image
        Raster source = sources[0];
        // Source rectangle
        Rectangle srcRect = source.getBounds();
        // Src upper left pixel coordinates
        int srcRectX = srcRect.x;
        int srcRectY = srcRect.y;

        //
        // Get data for the source rectangle & the destination rectangle
        // In the first version source Rectangle is the whole source
        // image always.
        RasterAccessor srcAccessor = new RasterAccessor(source, srcRect, formatTags[0],
                getSourceImage(0).getColorModel());
        RasterAccessor dstAccessor = new RasterAccessor(dest, destRect, formatTags[1],
                getColorModel());

        // Roi rasterAccessor initialization
        RasterAccessor roiAccessor = null;
        // Roi raster initialization
        Raster roi = null;

        // ROI calculation only if the roi raster is present
        if (useROIAccessor) {
            // Selection of the roi raster
            roi = rois[0];
            // creation of the rasterAccessor
            roiAccessor = new RasterAccessor(roi, srcRect, RasterAccessor.findCompatibleTags(
                    new RenderedImage[] { srcROIImage }, srcROIImage)[0],
                    srcROIImage.getColorModel());
        }

        int dataType = dest.getSampleModel().getDataType();
        // If the image is not binary, then for every kind of dataType, the image affine transformation
        // is performed.

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(dataType, srcAccessor, destRect, srcRectX, srcRectY, dstAccessor, roiAccessor);
            break;
        case DataBuffer.TYPE_INT:
            intLoop(dataType, srcAccessor, destRect, srcRectX, srcRectY, dstAccessor, roiAccessor);
            break;
        case DataBuffer.TYPE_SHORT:
            shortLoop(dataType, srcAccessor, destRect, srcRectX, srcRectY, dstAccessor, roiAccessor);
            break;
        case DataBuffer.TYPE_USHORT:
            ushortLoop(dataType, srcAccessor, destRect, srcRectX, srcRectY, dstAccessor,
                    roiAccessor);
            break;
        case DataBuffer.TYPE_FLOAT:
            floatLoop(dataType, srcAccessor, destRect, srcRectX, srcRectY, dstAccessor, roiAccessor);
            break;
        case DataBuffer.TYPE_DOUBLE:
            doubleLoop(dataType, srcAccessor, destRect, srcRectX, srcRectY, dstAccessor,
                    roiAccessor);
            break;
        }

        // If the RasterAccessor object set up a temporary buffer for the
        // op to write to, tell the RasterAccessor to write that data
        // to the raster, that we're done with it.
        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }

    private void byteLoop(int dataType, RasterAccessor src, Rectangle destRect, int srcRectX,
            int srcRectY, RasterAccessor dst, RasterAccessor roi) {

        final float src_rect_x1 = src.getX();
        final float src_rect_y1 = src.getY();
        final float src_rect_x2 = src_rect_x1 + src.getWidth();
        final float src_rect_y2 = src_rect_y1 + src.getHeight();

        double fracx = 0, fracy = 0;

        int dstPixelOffset;
        int dstOffset = 0;

        final Point2D dst_pt = new Point2D.Float();
        final Point2D src_pt = new Point2D.Float();

        final byte dstDataArrays[][] = dst.getByteDataArrays();
        final int dstBandOffsets[] = dst.getBandOffsets();
        final int dstPixelStride = dst.getPixelStride();
        final int dstScanlineStride = dst.getScanlineStride();

        final byte srcDataArrays[][] = src.getByteDataArrays();
        final int bandOffsets[] = src.getBandOffsets();
        final int srcPixelStride = src.getPixelStride();
        final int srcScanlineStride = src.getScanlineStride();

        final int dst_num_bands = dst.getNumBands();

        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        // ROI scanline stride
        final byte[] roiDataArray;
        final int roiDataLength;
        final int roiScanlineStride;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiScanlineStride = roi.getScanlineStride();
        } else {
            roiDataArray = null;
            roiDataLength = 0;
            roiScanlineStride = 0;
        }

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;

        if (caseA) {
            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                float s_x = (float) src_pt.getX();
                float s_y = (float) src_pt.getY();

                s_x -= 0.5;
                s_y -= 0.5;

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix * 1.0d;
                fracy = s_y - s_iy * 1.0d;

                for (int x = dst_min_x; x < dst_max_x; x++) {

                    if ((s_ix >= src_rect_x1) && (s_ix < (src_rect_x2 - 1))
                            && (s_iy >= src_rect_y1) && (s_iy < (src_rect_y2 - 1))) {

                        for (int k2 = 0; k2 < dst_num_bands; k2++) {

                            int posx = (s_ix - srcRectX) * srcPixelStride;
                            int posy = (s_iy - srcRectY) * srcScanlineStride;

                            int posxhigh = posx + srcPixelStride;
                            int posyhigh = posy + srcScanlineStride;

                            int s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]] & 0xff;
                            int s01 = srcDataArrays[k2][posxhigh + posy + bandOffsets[k2]] & 0xff;
                            int s10 = srcDataArrays[k2][posx + posyhigh + bandOffsets[k2]] & 0xff;
                            int s11 = srcDataArrays[k2][posxhigh + posyhigh + bandOffsets[k2]] & 0xff;

                            float s0 = (float) ((s01 - s00) * fracx + s00);
                            float s1 = (float) ((s11 - s10) * fracx + s10);

                            float result = (float) ((s1 - s0) * fracy + s0);

                            int intResult = 0;

                            if (result > 254.5f) {
                                intResult = 255;
                            } else if (result < 0.5f) {
                                intResult = 0;
                            } else {
                                intResult = (int) (result + 0.5f);
                            }

                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (byte) (intResult & 0xff);

                        }

                    } else if (setDestinationNoData) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                        }
                    }

                    // walk
                    if (fracx < fracdx1) {
                        s_ix += incx;
                        fracx += fracdx;
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                    } else {
                        s_iy += incy1;
                        fracy -= fracdy1;
                    }

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }

                // Go to the next line in the destination rectangle
                dstOffset += dstScanlineStride;
            }
        } else if (caseB) {
            if (useROIAccessor) {
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    float s_x = (float) src_pt.getX();
                    float s_y = (float) src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 1, 0, 1);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;
                        // If roiAccessor is present, the y position on the roi image is calculated
                        final int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        final int posxhigh = posx + srcPixelStride;
                        final int posyhigh = posy + srcScanlineStride;

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        final int w00index = baseIndex;
                        final int w01index = baseIndex + 1;
                        final int w10index = baseIndex + roiScanlineStride;
                        final int w11index = baseIndex + roiScanlineStride + 1;

                        final int w00 = w00index < roiDataLength ? roiDataArray[w00index] & 0xff
                                : 0;
                        final int w01 = w01index < roiDataLength ? roiDataArray[w01index] & 0xff
                                : 0;
                        final int w10 = w10index < roiDataLength ? roiDataArray[w10index] & 0xff
                                : 0;
                        final int w11 = w11index < roiDataLength ? roiDataArray[w11index] & 0xff
                                : 0;

                        if (baseIndex > roiDataLength || w00 == 0
                                || (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                // The interpolated value is saved in the destination array
                                int s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]] & 0xff;
                                int s01 = srcDataArrays[k2][posxhigh + posy + bandOffsets[k2]] & 0xff;
                                int s10 = srcDataArrays[k2][posx + posyhigh + bandOffsets[k2]] & 0xff;
                                int s11 = srcDataArrays[k2][posxhigh + posyhigh + bandOffsets[k2]] & 0xff;

                                float s0 = (float) ((s01 - s00) * fracx + s00);
                                float s1 = (float) ((s11 - s10) * fracx + s10);

                                float result = (float) ((s1 - s0) * fracy + s0);

                                int intResult = 0;

                                if (result > 254.5f) {
                                    intResult = 255;
                                } else if (result < 0.5f) {
                                    intResult = 0;
                                } else {
                                    intResult = (int) (result + 0.5f);
                                }

                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (byte) (intResult & 0xff);
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }
                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                        } else {
                            s_iy += incy1;
                            fracy -= fracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            } else {
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    float s_x = (float) src_pt.getX();
                    float s_y = (float) src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 1, 0, 1);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int posxhigh = posx + srcPixelStride;
                        final int posyhigh = posy + srcScanlineStride;

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (roiBounds.contains(x0, y0)) {

                            final int w00 = roiIter.getSample(x0, y0, 0) & 0xff;
                            final int w01 = roiIter.getSample(x0 + 1, y0, 0) & 0xff;
                            final int w10 = roiIter.getSample(x0, y0 + 1, 0) & 0xff;
                            final int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0) & 0xff;

                            if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                if (setDestinationNoData) {
                                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                                    }
                                }
                            } else {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    // The interpolated value is saved in the destination array
                                    int s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]] & 0xff;
                                    int s01 = srcDataArrays[k2][posxhigh + posy + bandOffsets[k2]] & 0xff;
                                    int s10 = srcDataArrays[k2][posx + posyhigh + bandOffsets[k2]] & 0xff;
                                    int s11 = srcDataArrays[k2][posxhigh + posyhigh
                                            + bandOffsets[k2]] & 0xff;

                                    float s0 = (float) ((s01 - s00) * fracx + s00);
                                    float s1 = (float) ((s11 - s10) * fracx + s10);

                                    float result = (float) ((s1 - s0) * fracy + s0);

                                    int intResult = 0;

                                    if (result > 254.5f) {
                                        intResult = 255;
                                    } else if (result < 0.5f) {
                                        intResult = 0;
                                    } else {
                                        intResult = (int) (result + 0.5f);
                                    }

                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (byte) (intResult & 0xff);
                                }
                            }
                        } else {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }
                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                        } else {
                            s_iy += incy1;
                            fracy -= fracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        } else if (caseC) {
            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                float s_x = (float) src_pt.getX();
                float s_y = (float) src_pt.getY();

                s_x -= 0.5;
                s_y -= 0.5;

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix * 1.0d;
                fracy = s_y - s_iy * 1.0d;

                for (int x = dst_min_x; x < dst_max_x; x++) {

                    final int posx = (s_ix - srcRectX) * srcPixelStride;
                    final int posy = (s_iy - srcRectY) * srcScanlineStride;

                    final int posxhigh = posx + srcPixelStride;
                    final int posyhigh = posy + srcScanlineStride;

                    if ((s_ix >= src_rect_x1) && (s_ix < (src_rect_x2 - 1))
                            && (s_iy >= src_rect_y1) && (s_iy < (src_rect_y2 - 1))) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {

                            // The interpolated value is saved in the destination array
                            int s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]];
                            int s01 = srcDataArrays[k2][posxhigh + posy + bandOffsets[k2]];
                            int s10 = srcDataArrays[k2][posx + posyhigh + bandOffsets[k2]];
                            int s11 = srcDataArrays[k2][posxhigh + posyhigh + bandOffsets[k2]];

                            int w00 = byteLookupTable[s00] == destinationNoDataByte ? 0 : 1;
                            int w01 = byteLookupTable[s01] == destinationNoDataByte ? 0 : 1;
                            int w10 = byteLookupTable[s10] == destinationNoDataByte ? 0 : 1;
                            int w11 = byteLookupTable[s11] == destinationNoDataByte ? 0 : 1;

                            if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                if (setDestinationNoData) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                                }
                            } else {
                                double result = computeValue(s00 & 0xff, s01 & 0xff, s10 & 0xff,
                                        s11 & 0xff, w00, w01, w10, w11, fracx, fracy);

                                int intResult = 0;

                                if (result > 254.5f) {
                                    intResult = 255;
                                } else if (result < 0.5f) {
                                    intResult = 0;
                                } else {
                                    intResult = (int) (result + 0.5f);
                                }

                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (byte) (intResult & 0xff);
                            }
                        }
                    } else if (setDestinationNoData) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                        }
                    }

                    // walk
                    if (fracx < fracdx1) {
                        s_ix += incx;
                        fracx += fracdx;
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                    } else {
                        s_iy += incy1;
                        fracy -= fracdy1;
                    }

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }
                // Go to the next line in the destination rectangle
                dstOffset += dstScanlineStride;
            }
        } else {
            if (useROIAccessor) {

                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    float s_x = (float) src_pt.getX();
                    float s_y = (float) src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 1, 0, 1);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;
                        // If roiAccessor is present, the y position on the roi image is calculated
                        final int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        final int posxhigh = posx + srcPixelStride;
                        final int posyhigh = posy + srcScanlineStride;

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        final int w00index = baseIndex;
                        final int w01index = baseIndex + 1;
                        final int w10index = baseIndex + roiScanlineStride;
                        final int w11index = baseIndex + roiScanlineStride + 1;

                        int w00 = w00index < roiDataLength ? roiDataArray[w00index] & 0xff : 0;
                        int w01 = w01index < roiDataLength ? roiDataArray[w01index] & 0xff : 0;
                        int w10 = w10index < roiDataLength ? roiDataArray[w10index] & 0xff : 0;
                        int w11 = w11index < roiDataLength ? roiDataArray[w11index] & 0xff : 0;

                        if (baseIndex > roiDataLength || w00 == 0
                                || (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                // The interpolated value is saved in the destination array
                                final int s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]];
                                final int s01 = srcDataArrays[k2][posxhigh + posy + bandOffsets[k2]];
                                final int s10 = srcDataArrays[k2][posx + posyhigh + bandOffsets[k2]];
                                final int s11 = srcDataArrays[k2][posxhigh + posyhigh
                                        + bandOffsets[k2]];

                                w00 = byteLookupTable[s00] == destinationNoDataByte ? 0 : 1;
                                w01 = byteLookupTable[s01] == destinationNoDataByte ? 0 : 1;
                                w10 = byteLookupTable[s10] == destinationNoDataByte ? 0 : 1;
                                w11 = byteLookupTable[s11] == destinationNoDataByte ? 0 : 1;

                                if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    if (setDestinationNoData) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                                    }
                                } else {
                                    double result = computeValue(s00 & 0xff, s01 & 0xff,
                                            s10 & 0xff, s11 & 0xff, w00, w01, w10, w11, fracx,
                                            fracy);

                                    int intResult = 0;

                                    if (result > 254.5f) {
                                        intResult = 255;
                                    } else if (result < 0.5f) {
                                        intResult = 0;
                                    } else {
                                        intResult = (int) (result + 0.5f);
                                    }

                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (byte) (intResult & 0xff);
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }
                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                        } else {
                            s_iy += incy1;
                            fracy -= fracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            } else {
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    float s_x = (float) src_pt.getX();
                    float s_y = (float) src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 1, 0, 1);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {
                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int posxhigh = posx + srcPixelStride;
                        final int posyhigh = posy + srcScanlineStride;

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (roiBounds.contains(x0, y0)) {

                            int w00 = roiIter.getSample(x0, y0, 0) & 0xff;
                            int w01 = roiIter.getSample(x0 + 1, y0, 0) & 0xff;
                            int w10 = roiIter.getSample(x0, y0 + 1, 0) & 0xff;
                            int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0) & 0xff;

                            if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                if (setDestinationNoData) {
                                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                                    }
                                }
                            } else {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    // The interpolated value is saved in the destination array
                                    final int s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]];
                                    final int s01 = srcDataArrays[k2][posxhigh + posy
                                            + bandOffsets[k2]];
                                    final int s10 = srcDataArrays[k2][posx + posyhigh
                                            + bandOffsets[k2]];
                                    final int s11 = srcDataArrays[k2][posxhigh + posyhigh
                                            + bandOffsets[k2]];

                                    w00 = byteLookupTable[s00] == destinationNoDataByte ? 0 : 1;
                                    w01 = byteLookupTable[s01] == destinationNoDataByte ? 0 : 1;
                                    w10 = byteLookupTable[s10] == destinationNoDataByte ? 0 : 1;
                                    w11 = byteLookupTable[s11] == destinationNoDataByte ? 0 : 1;

                                    if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                        if (setDestinationNoData) {
                                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                                        }
                                    } else {
                                        double result = computeValue(s00 & 0xff, s01 & 0xff,
                                                s10 & 0xff, s11 & 0xff, w00, w01, w10, w11, fracx,
                                                fracy);

                                        int intResult = 0;

                                        if (result > 254.5f) {
                                            intResult = 255;
                                        } else if (result < 0.5f) {
                                            intResult = 0;
                                        } else {
                                            intResult = (int) (result + 0.5f);
                                        }

                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (byte) (intResult & 0xff);
                                    }
                                }
                            }
                        } else {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }
                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                        } else {
                            s_iy += incy1;
                            fracy -= fracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        }
    }

    private void ushortLoop(int dataType, RasterAccessor src, Rectangle destRect, int srcRectX,
            int srcRectY, RasterAccessor dst, RasterAccessor roi) {

        final float src_rect_x1 = src.getX();
        final float src_rect_y1 = src.getY();
        final float src_rect_x2 = src_rect_x1 + src.getWidth();
        final float src_rect_y2 = src_rect_y1 + src.getHeight();

        double fracx = 0, fracy = 0;

        int dstPixelOffset;
        int dstOffset = 0;

        final Point2D dst_pt = new Point2D.Float();
        final Point2D src_pt = new Point2D.Float();

        final short dstDataArrays[][] = dst.getShortDataArrays();
        final int dstBandOffsets[] = dst.getBandOffsets();
        final int dstPixelStride = dst.getPixelStride();
        final int dstScanlineStride = dst.getScanlineStride();

        final short srcDataArrays[][] = src.getShortDataArrays();
        final int bandOffsets[] = src.getBandOffsets();
        final int srcPixelStride = src.getPixelStride();
        final int srcScanlineStride = src.getScanlineStride();

        final int dst_num_bands = dst.getNumBands();

        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        // ROI scanline stride
        final byte[] roiDataArray;
        final int roiDataLength;
        final int roiScanlineStride;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiScanlineStride = roi.getScanlineStride();
        } else {
            roiDataArray = null;
            roiDataLength = 0;
            roiScanlineStride = 0;
        }

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;

        if (caseA) {
            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                float s_x = (float) src_pt.getX();
                float s_y = (float) src_pt.getY();

                s_x -= 0.5;
                s_y -= 0.5;

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix * 1.0d;
                fracy = s_y - s_iy * 1.0d;

                for (int x = dst_min_x; x < dst_max_x; x++) {

                    if ((s_ix >= src_rect_x1) && (s_ix < (src_rect_x2 - 1))
                            && (s_iy >= src_rect_y1) && (s_iy < (src_rect_y2 - 1))) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {

                            int posx = (s_ix - srcRectX) * srcPixelStride;
                            int posy = (s_iy - srcRectY) * srcScanlineStride;

                            int posxhigh = posx + srcPixelStride;
                            int posyhigh = posy + srcScanlineStride;

                            int s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]] & 0xffff;
                            int s01 = srcDataArrays[k2][posxhigh + posy + bandOffsets[k2]] & 0xffff;
                            int s10 = srcDataArrays[k2][posx + posyhigh + bandOffsets[k2]] & 0xffff;
                            int s11 = srcDataArrays[k2][posxhigh + posyhigh + bandOffsets[k2]] & 0xffff;

                            float s0 = (float) ((s01 - s00) * fracx + s00);
                            float s1 = (float) ((s11 - s10) * fracx + s10);

                            float result = (float) ((s1 - s0) * fracy + s0);

                            int intResult = 0;

                            if (result > (float) USHORT_MAX_VALUE) {
                                intResult = USHORT_MAX_VALUE;
                            } else if (result < 0.0) {
                                intResult = 0;
                            } else {
                                intResult = (int) (result + 0.5f);
                            }

                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (short) (intResult & 0xffff);

                        }
                    } else if (setDestinationNoData) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                        }
                    }

                    // walk
                    if (fracx < fracdx1) {
                        s_ix += incx;
                        fracx += fracdx;
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                    } else {
                        s_iy += incy1;
                        fracy -= fracdy1;
                    }

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }

                // Go to the next line in the destination rectangle
                dstOffset += dstScanlineStride;
            }
        } else if (caseB) {
            if (useROIAccessor) {
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    float s_x = (float) src_pt.getX();
                    float s_y = (float) src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 1, 0, 1);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;
                        // If roiAccessor is present, the y position on the roi image is calculated
                        final int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        final int posxhigh = posx + srcPixelStride;
                        final int posyhigh = posy + srcScanlineStride;

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        final int w00index = baseIndex;
                        final int w01index = baseIndex + 1;
                        final int w10index = baseIndex + roiScanlineStride;
                        final int w11index = baseIndex + roiScanlineStride + 1;

                        final int w00 = w00index < roiDataLength ? roiDataArray[w00index] & 0xff
                                : 0;
                        final int w01 = w01index < roiDataLength ? roiDataArray[w01index] & 0xff
                                : 0;
                        final int w10 = w10index < roiDataLength ? roiDataArray[w10index] & 0xff
                                : 0;
                        final int w11 = w11index < roiDataLength ? roiDataArray[w11index] & 0xff
                                : 0;

                        if (baseIndex > roiDataLength || w00 == 0
                                || (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                // The interpolated value is saved in the destination array
                                int s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]] & 0xffff;
                                int s01 = srcDataArrays[k2][posxhigh + posy + bandOffsets[k2]] & 0xffff;
                                int s10 = srcDataArrays[k2][posx + posyhigh + bandOffsets[k2]] & 0xffff;
                                int s11 = srcDataArrays[k2][posxhigh + posyhigh + bandOffsets[k2]] & 0xffff;

                                float s0 = (float) ((s01 - s00) * fracx + s00);
                                float s1 = (float) ((s11 - s10) * fracx + s10);

                                float result = (float) ((s1 - s0) * fracy + s0);

                                int intResult = 0;

                                if (result > (float) USHORT_MAX_VALUE) {
                                    intResult = USHORT_MAX_VALUE;
                                } else if (result < 0.0) {
                                    intResult = 0;
                                } else {
                                    intResult = (int) (result + 0.5f);
                                }

                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (short) (intResult & 0xffff);
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }
                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                        } else {
                            s_iy += incy1;
                            fracy -= fracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            } else {
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    float s_x = (float) src_pt.getX();
                    float s_y = (float) src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 1, 0, 1);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int posxhigh = posx + srcPixelStride;
                        final int posyhigh = posy + srcScanlineStride;

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (roiBounds.contains(x0, y0)) {

                            final int w00 = roiIter.getSample(x0, y0, 0) & 0xff;
                            final int w01 = roiIter.getSample(x0 + 1, y0, 0) & 0xff;
                            final int w10 = roiIter.getSample(x0, y0 + 1, 0) & 0xff;
                            final int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0) & 0xff;

                            if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                if (setDestinationNoData) {
                                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                                    }
                                }
                            } else {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    // The interpolated value is saved in the destination array
                                    int s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]] & 0xffff;
                                    int s01 = srcDataArrays[k2][posxhigh + posy + bandOffsets[k2]] & 0xffff;
                                    int s10 = srcDataArrays[k2][posx + posyhigh + bandOffsets[k2]] & 0xffff;
                                    int s11 = srcDataArrays[k2][posxhigh + posyhigh
                                            + bandOffsets[k2]] & 0xffff;

                                    float s0 = (float) ((s01 - s00) * fracx + s00);
                                    float s1 = (float) ((s11 - s10) * fracx + s10);

                                    float result = (float) ((s1 - s0) * fracy + s0);

                                    int intResult = 0;

                                    if (result > (float) USHORT_MAX_VALUE) {
                                        intResult = USHORT_MAX_VALUE;
                                    } else if (result < 0.0) {
                                        intResult = 0;
                                    } else {
                                        intResult = (int) (result + 0.5f);
                                    }

                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (short) (intResult & 0xffff);
                                }
                            }
                        } else {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }
                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                        } else {
                            s_iy += incy1;
                            fracy -= fracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        } else if (caseC) {
            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                float s_x = (float) src_pt.getX();
                float s_y = (float) src_pt.getY();

                s_x -= 0.5;
                s_y -= 0.5;

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix * 1.0d;
                fracy = s_y - s_iy * 1.0d;

                for (int x = dst_min_x; x < dst_max_x; x++) {

                    final int posx = (s_ix - srcRectX) * srcPixelStride;
                    final int posy = (s_iy - srcRectY) * srcScanlineStride;

                    final int posxhigh = posx + srcPixelStride;
                    final int posyhigh = posy + srcScanlineStride;

                    if ((s_ix >= src_rect_x1) && (s_ix < (src_rect_x2 - 1))
                            && (s_iy >= src_rect_y1) && (s_iy < (src_rect_y2 - 1))) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {

                            // The interpolated value is saved in the destination array
                            short s00 = (short) (srcDataArrays[k2][posx + posy + bandOffsets[k2]] & 0xffff);
                            short s01 = (short) (srcDataArrays[k2][posxhigh + posy
                                    + bandOffsets[k2]] & 0xffff);
                            short s10 = (short) (srcDataArrays[k2][posx + posyhigh
                                    + bandOffsets[k2]] & 0xffff);
                            short s11 = (short) (srcDataArrays[k2][posxhigh + posyhigh
                                    + bandOffsets[k2]] & 0xffff);

                            int w00 = noData.contains(s00) ? 0 : 1;
                            int w01 = noData.contains(s01) ? 0 : 1;
                            int w10 = noData.contains(s10) ? 0 : 1;
                            int w11 = noData.contains(s11) ? 0 : 1;

                            if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                if (setDestinationNoData) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                                }
                            } else {
                                double result = computeValue(s00, s01, s10, s11, w00, w01, w10,
                                        w11, fracx, fracy);

                                int intResult = 0;

                                if (result > (float) USHORT_MAX_VALUE) {
                                    intResult = USHORT_MAX_VALUE;
                                } else if (result < 0.0) {
                                    intResult = 0;
                                } else {
                                    intResult = (int) (result + 0.5f);
                                }

                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (short) (intResult & 0xffff);
                            }
                        }
                    } else if (setDestinationNoData) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                        }
                    }

                    // walk
                    if (fracx < fracdx1) {
                        s_ix += incx;
                        fracx += fracdx;
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                    } else {
                        s_iy += incy1;
                        fracy -= fracdy1;
                    }

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }
                // Go to the next line in the destination rectangle
                dstOffset += dstScanlineStride;
            }
        } else {
            if (useROIAccessor) {

                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    float s_x = (float) src_pt.getX();
                    float s_y = (float) src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 1, 0, 1);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;
                        // If roiAccessor is present, the y position on the roi image is calculated
                        final int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        final int posxhigh = posx + srcPixelStride;
                        final int posyhigh = posy + srcScanlineStride;

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        final int w00index = baseIndex;
                        final int w01index = baseIndex + 1;
                        final int w10index = baseIndex + roiScanlineStride;
                        final int w11index = baseIndex + roiScanlineStride + 1;

                        int w00 = w00index < roiDataLength ? roiDataArray[w00index] & 0xff : 0;
                        int w01 = w01index < roiDataLength ? roiDataArray[w01index] & 0xff : 0;
                        int w10 = w10index < roiDataLength ? roiDataArray[w10index] & 0xff : 0;
                        int w11 = w11index < roiDataLength ? roiDataArray[w11index] & 0xff : 0;

                        if (baseIndex > roiDataLength || w00 == 0
                                || (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                // The interpolated value is saved in the destination array
                                final short s00 = (short) (srcDataArrays[k2][posx + posy
                                        + bandOffsets[k2]] & 0xffff);
                                final short s01 = (short) (srcDataArrays[k2][posxhigh + posy
                                        + bandOffsets[k2]] & 0xffff);
                                final short s10 = (short) (srcDataArrays[k2][posx + posyhigh
                                        + bandOffsets[k2]] & 0xffff);
                                final short s11 = (short) (srcDataArrays[k2][posxhigh + posyhigh
                                        + bandOffsets[k2]] & 0xffff);

                                w00 = noData.contains(s00) ? 0 : 1;
                                w01 = noData.contains(s01) ? 0 : 1;
                                w10 = noData.contains(s10) ? 0 : 1;
                                w11 = noData.contains(s11) ? 0 : 1;

                                if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    if (setDestinationNoData) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                                    }
                                } else {
                                    double result = computeValue(s00, s01, s10, s11, w00, w01, w10,
                                            w11, fracx, fracy);

                                    int intResult = 0;

                                    if (result > (float) USHORT_MAX_VALUE) {
                                        intResult = USHORT_MAX_VALUE;
                                    } else if (result < 0.0) {
                                        intResult = 0;
                                    } else {
                                        intResult = (int) (result + 0.5f);
                                    }

                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (short) (intResult & 0xffff);
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }
                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                        } else {
                            s_iy += incy1;
                            fracy -= fracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            } else {
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    float s_x = (float) src_pt.getX();
                    float s_y = (float) src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 1, 0, 1);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {
                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int posxhigh = posx + srcPixelStride;
                        final int posyhigh = posy + srcScanlineStride;

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (roiBounds.contains(x0, y0)) {

                            int w00 = roiIter.getSample(x0, y0, 0) & 0xff;
                            int w01 = roiIter.getSample(x0 + 1, y0, 0) & 0xff;
                            int w10 = roiIter.getSample(x0, y0 + 1, 0) & 0xff;
                            int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0) & 0xff;

                            if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                if (setDestinationNoData) {
                                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                                    }
                                }
                            } else {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    // The interpolated value is saved in the destination array
                                    final short s00 = (short) (srcDataArrays[k2][posx + posy
                                            + bandOffsets[k2]] & 0xffff);
                                    final short s01 = (short) (srcDataArrays[k2][posxhigh + posy
                                            + bandOffsets[k2]] & 0xffff);
                                    final short s10 = (short) (srcDataArrays[k2][posx + posyhigh
                                            + bandOffsets[k2]] & 0xffff);
                                    final short s11 = (short) (srcDataArrays[k2][posxhigh
                                            + posyhigh + bandOffsets[k2]] & 0xffff);

                                    w00 = noData.contains(s00) ? 0 : 1;
                                    w01 = noData.contains(s01) ? 0 : 1;
                                    w10 = noData.contains(s10) ? 0 : 1;
                                    w11 = noData.contains(s11) ? 0 : 1;

                                    if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                        if (setDestinationNoData) {
                                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                                        }
                                    } else {
                                        double result = computeValue(s00, s01, s10, s11, w00, w01,
                                                w10, w11, fracx, fracy);

                                        int intResult = 0;

                                        if (result > (float) USHORT_MAX_VALUE) {
                                            intResult = USHORT_MAX_VALUE;
                                        } else if (result < 0.0) {
                                            intResult = 0;
                                        } else {
                                            intResult = (int) (result + 0.5f);
                                        }

                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (short) (intResult & 0xffff);
                                    }
                                }
                            }
                        } else {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }
                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                        } else {
                            s_iy += incy1;
                            fracy -= fracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        }
    }

    private void shortLoop(int dataType, RasterAccessor src, Rectangle destRect, int srcRectX,
            int srcRectY, RasterAccessor dst, RasterAccessor roi) {

        final float src_rect_x1 = src.getX();
        final float src_rect_y1 = src.getY();
        final float src_rect_x2 = src_rect_x1 + src.getWidth();
        final float src_rect_y2 = src_rect_y1 + src.getHeight();

        double fracx = 0, fracy = 0;

        int dstPixelOffset;
        int dstOffset = 0;

        final Point2D dst_pt = new Point2D.Float();
        final Point2D src_pt = new Point2D.Float();

        final short dstDataArrays[][] = dst.getShortDataArrays();
        final int dstBandOffsets[] = dst.getBandOffsets();
        final int dstPixelStride = dst.getPixelStride();
        final int dstScanlineStride = dst.getScanlineStride();

        final short srcDataArrays[][] = src.getShortDataArrays();
        final int bandOffsets[] = src.getBandOffsets();
        final int srcPixelStride = src.getPixelStride();
        final int srcScanlineStride = src.getScanlineStride();

        final int dst_num_bands = dst.getNumBands();

        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        // ROI scanline stride
        final byte[] roiDataArray;
        final int roiDataLength;
        final int roiScanlineStride;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiScanlineStride = roi.getScanlineStride();
        } else {
            roiDataArray = null;
            roiDataLength = 0;
            roiScanlineStride = 0;
        }

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;

        if (caseA) {
            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                float s_x = (float) src_pt.getX();
                float s_y = (float) src_pt.getY();

                s_x -= 0.5;
                s_y -= 0.5;

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix * 1.0d;
                fracy = s_y - s_iy * 1.0d;

                for (int x = dst_min_x; x < dst_max_x; x++) {

                    if ((s_ix >= src_rect_x1) && (s_ix < (src_rect_x2 - 1))
                            && (s_iy >= src_rect_y1) && (s_iy < (src_rect_y2 - 1))) {

                        for (int k2 = 0; k2 < dst_num_bands; k2++) {

                            int posx = (s_ix - srcRectX) * srcPixelStride;
                            int posy = (s_iy - srcRectY) * srcScanlineStride;

                            int posxhigh = posx + srcPixelStride;
                            int posyhigh = posy + srcScanlineStride;

                            int s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]];
                            int s01 = srcDataArrays[k2][posxhigh + posy + bandOffsets[k2]];
                            int s10 = srcDataArrays[k2][posx + posyhigh + bandOffsets[k2]];
                            int s11 = srcDataArrays[k2][posxhigh + posyhigh + bandOffsets[k2]];

                            float s0 = (float) ((s01 - s00) * fracx + s00);
                            float s1 = (float) ((s11 - s10) * fracx + s10);

                            float result = (float) ((s1 - s0) * fracy + s0);

                            int intResult = 0;

                            if (result > (float) Short.MAX_VALUE) {
                                intResult = Short.MAX_VALUE;
                            } else if (result < (float) Short.MIN_VALUE) {
                                intResult = Short.MIN_VALUE;
                            } else if (result > 0) {
                                intResult = (int) (result + 0.5F);
                            } else {
                                intResult = (int) (result - 0.5F);
                            }

                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (short) (intResult);

                        }
                    } else if (setDestinationNoData) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                        }
                    }

                    // walk
                    if (fracx < fracdx1) {
                        s_ix += incx;
                        fracx += fracdx;
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                    } else {
                        s_iy += incy1;
                        fracy -= fracdy1;
                    }

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }

                // Go to the next line in the destination rectangle
                dstOffset += dstScanlineStride;
            }
        } else if (caseB) {
            if (useROIAccessor) {
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    float s_x = (float) src_pt.getX();
                    float s_y = (float) src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 1, 0, 1);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;
                        // If roiAccessor is present, the y position on the roi image is calculated
                        final int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        final int posxhigh = posx + srcPixelStride;
                        final int posyhigh = posy + srcScanlineStride;

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        final int w00index = baseIndex;
                        final int w01index = baseIndex + 1;
                        final int w10index = baseIndex + roiScanlineStride;
                        final int w11index = baseIndex + roiScanlineStride + 1;

                        final int w00 = w00index < roiDataLength ? roiDataArray[w00index] & 0xff
                                : 0;
                        final int w01 = w01index < roiDataLength ? roiDataArray[w01index] & 0xff
                                : 0;
                        final int w10 = w10index < roiDataLength ? roiDataArray[w10index] & 0xff
                                : 0;
                        final int w11 = w11index < roiDataLength ? roiDataArray[w11index] & 0xff
                                : 0;

                        if (baseIndex > roiDataLength || w00 == 0
                                || (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                // The interpolated value is saved in the destination array
                                int s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]];
                                int s01 = srcDataArrays[k2][posxhigh + posy + bandOffsets[k2]];
                                int s10 = srcDataArrays[k2][posx + posyhigh + bandOffsets[k2]];
                                int s11 = srcDataArrays[k2][posxhigh + posyhigh + bandOffsets[k2]];

                                float s0 = (float) ((s01 - s00) * fracx + s00);
                                float s1 = (float) ((s11 - s10) * fracx + s10);

                                float result = (float) ((s1 - s0) * fracy + s0);

                                int intResult = 0;

                                if (result > (float) Short.MAX_VALUE) {
                                    intResult = Short.MAX_VALUE;
                                } else if (result < (float) Short.MIN_VALUE) {
                                    intResult = Short.MIN_VALUE;
                                } else if (result > 0) {
                                    intResult = (int) (result + 0.5F);
                                } else {
                                    intResult = (int) (result - 0.5F);
                                }

                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (short) (intResult);
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }
                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                        } else {
                            s_iy += incy1;
                            fracy -= fracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            } else {
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    float s_x = (float) src_pt.getX();
                    float s_y = (float) src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 1, 0, 1);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int posxhigh = posx + srcPixelStride;
                        final int posyhigh = posy + srcScanlineStride;

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (roiBounds.contains(x0, y0)) {

                            final int w00 = roiIter.getSample(x0, y0, 0) & 0xff;
                            final int w01 = roiIter.getSample(x0 + 1, y0, 0) & 0xff;
                            final int w10 = roiIter.getSample(x0, y0 + 1, 0) & 0xff;
                            final int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0) & 0xff;

                            if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                if (setDestinationNoData) {
                                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                                    }
                                }
                            } else {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    // The interpolated value is saved in the destination array
                                    int s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]];
                                    int s01 = srcDataArrays[k2][posxhigh + posy + bandOffsets[k2]];
                                    int s10 = srcDataArrays[k2][posx + posyhigh + bandOffsets[k2]];
                                    int s11 = srcDataArrays[k2][posxhigh + posyhigh
                                            + bandOffsets[k2]];

                                    float s0 = (float) ((s01 - s00) * fracx + s00);
                                    float s1 = (float) ((s11 - s10) * fracx + s10);

                                    float result = (float) ((s1 - s0) * fracy + s0);

                                    int intResult = 0;

                                    if (result > (float) Short.MAX_VALUE) {
                                        intResult = Short.MAX_VALUE;
                                    } else if (result < (float) Short.MIN_VALUE) {
                                        intResult = Short.MIN_VALUE;
                                    } else if (result > 0) {
                                        intResult = (int) (result + 0.5F);
                                    } else {
                                        intResult = (int) (result - 0.5F);
                                    }

                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (short) (intResult);
                                }
                            }
                        } else {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }
                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                        } else {
                            s_iy += incy1;
                            fracy -= fracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        } else if (caseC) {
            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                float s_x = (float) src_pt.getX();
                float s_y = (float) src_pt.getY();

                s_x -= 0.5;
                s_y -= 0.5;

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix * 1.0d;
                fracy = s_y - s_iy * 1.0d;

                for (int x = dst_min_x; x < dst_max_x; x++) {

                    final int posx = (s_ix - srcRectX) * srcPixelStride;
                    final int posy = (s_iy - srcRectY) * srcScanlineStride;

                    final int posxhigh = posx + srcPixelStride;
                    final int posyhigh = posy + srcScanlineStride;

                    if ((s_ix >= src_rect_x1) && (s_ix < (src_rect_x2 - 1))
                            && (s_iy >= src_rect_y1) && (s_iy < (src_rect_y2 - 1))) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {

                            // The interpolated value is saved in the destination array
                            short s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]];
                            short s01 = srcDataArrays[k2][posxhigh + posy + bandOffsets[k2]];
                            short s10 = srcDataArrays[k2][posx + posyhigh + bandOffsets[k2]];
                            short s11 = srcDataArrays[k2][posxhigh + posyhigh + bandOffsets[k2]];

                            int w00 = noData.contains(s00) ? 0 : 1;
                            int w01 = noData.contains(s01) ? 0 : 1;
                            int w10 = noData.contains(s10) ? 0 : 1;
                            int w11 = noData.contains(s11) ? 0 : 1;

                            if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                if (setDestinationNoData) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                                }
                            } else {
                                double result = computeValue(s00, s01, s10, s11, w00, w01, w10,
                                        w11, fracx, fracy);

                                int intResult = 0;

                                if (result > (float) Short.MAX_VALUE) {
                                    intResult = Short.MAX_VALUE;
                                } else if (result < (float) Short.MIN_VALUE) {
                                    intResult = Short.MIN_VALUE;
                                } else if (result > 0) {
                                    intResult = (int) (result + 0.5F);
                                } else {
                                    intResult = (int) (result - 0.5F);
                                }

                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (short) (intResult);
                            }
                        }
                    } else if (setDestinationNoData) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                        }
                    }

                    // walk
                    if (fracx < fracdx1) {
                        s_ix += incx;
                        fracx += fracdx;
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                    } else {
                        s_iy += incy1;
                        fracy -= fracdy1;
                    }

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }
                // Go to the next line in the destination rectangle
                dstOffset += dstScanlineStride;
            }
        } else {
            if (useROIAccessor) {

                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    float s_x = (float) src_pt.getX();
                    float s_y = (float) src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 1, 0, 1);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;
                        // If roiAccessor is present, the y position on the roi image is calculated
                        final int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        final int posxhigh = posx + srcPixelStride;
                        final int posyhigh = posy + srcScanlineStride;

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        final int w00index = baseIndex;
                        final int w01index = baseIndex + 1;
                        final int w10index = baseIndex + roiScanlineStride;
                        final int w11index = baseIndex + roiScanlineStride + 1;

                        int w00 = w00index < roiDataLength ? roiDataArray[w00index] & 0xff : 0;
                        int w01 = w01index < roiDataLength ? roiDataArray[w01index] & 0xff : 0;
                        int w10 = w10index < roiDataLength ? roiDataArray[w10index] & 0xff : 0;
                        int w11 = w11index < roiDataLength ? roiDataArray[w11index] & 0xff : 0;

                        if (baseIndex > roiDataLength || w00 == 0
                                || (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                // The interpolated value is saved in the destination array
                                final short s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]];
                                final short s01 = srcDataArrays[k2][posxhigh + posy
                                        + bandOffsets[k2]];
                                final short s10 = srcDataArrays[k2][posx + posyhigh
                                        + bandOffsets[k2]];
                                final short s11 = srcDataArrays[k2][posxhigh + posyhigh
                                        + bandOffsets[k2]];

                                w00 = noData.contains(s00) ? 0 : 1;
                                w01 = noData.contains(s01) ? 0 : 1;
                                w10 = noData.contains(s10) ? 0 : 1;
                                w11 = noData.contains(s11) ? 0 : 1;

                                if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    if (setDestinationNoData) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                                    }
                                } else {
                                    double result = computeValue(s00, s01, s10, s11, w00, w01, w10,
                                            w11, fracx, fracy);

                                    int intResult = 0;

                                    if (result > (float) Short.MAX_VALUE) {
                                        intResult = Short.MAX_VALUE;
                                    } else if (result < (float) Short.MIN_VALUE) {
                                        intResult = Short.MIN_VALUE;
                                    } else if (result > 0) {
                                        intResult = (int) (result + 0.5F);
                                    } else {
                                        intResult = (int) (result - 0.5F);
                                    }

                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (short) (intResult);
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }
                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                        } else {
                            s_iy += incy1;
                            fracy -= fracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            } else {
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    float s_x = (float) src_pt.getX();
                    float s_y = (float) src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 1, 0, 1);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {
                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int posxhigh = posx + srcPixelStride;
                        final int posyhigh = posy + srcScanlineStride;

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (roiBounds.contains(x0, y0)) {

                            int w00 = roiIter.getSample(x0, y0, 0) & 0xff;
                            int w01 = roiIter.getSample(x0 + 1, y0, 0) & 0xff;
                            int w10 = roiIter.getSample(x0, y0 + 1, 0) & 0xff;
                            int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0) & 0xff;

                            if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                if (setDestinationNoData) {
                                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                                    }
                                }
                            } else {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    // The interpolated value is saved in the destination array
                                    final short s00 = srcDataArrays[k2][posx + posy
                                            + bandOffsets[k2]];
                                    final short s01 = srcDataArrays[k2][posxhigh + posy
                                            + bandOffsets[k2]];
                                    final short s10 = srcDataArrays[k2][posx + posyhigh
                                            + bandOffsets[k2]];
                                    final short s11 = srcDataArrays[k2][posxhigh + posyhigh
                                            + bandOffsets[k2]];

                                    w00 = noData.contains(s00) ? 0 : 1;
                                    w01 = noData.contains(s01) ? 0 : 1;
                                    w10 = noData.contains(s10) ? 0 : 1;
                                    w11 = noData.contains(s11) ? 0 : 1;

                                    if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                        if (setDestinationNoData) {
                                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                                        }
                                    } else {
                                        double result = computeValue(s00, s01, s10, s11, w00, w01,
                                                w10, w11, fracx, fracy);

                                        int intResult = 0;

                                        if (result > (float) Short.MAX_VALUE) {
                                            intResult = Short.MAX_VALUE;
                                        } else if (result < (float) Short.MIN_VALUE) {
                                            intResult = Short.MIN_VALUE;
                                        } else if (result > 0) {
                                            intResult = (int) (result + 0.5F);
                                        } else {
                                            intResult = (int) (result - 0.5F);
                                        }

                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (short) (intResult);
                                    }
                                }
                            }
                        } else {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }
                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                        } else {
                            s_iy += incy1;
                            fracy -= fracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        }
    }

    private void intLoop(int dataType, RasterAccessor src, Rectangle destRect, int srcRectX,
            int srcRectY, RasterAccessor dst, RasterAccessor roi) {

        final float src_rect_x1 = src.getX();
        final float src_rect_y1 = src.getY();
        final float src_rect_x2 = src_rect_x1 + src.getWidth();
        final float src_rect_y2 = src_rect_y1 + src.getHeight();

        double fracx = 0, fracy = 0;

        int dstPixelOffset;
        int dstOffset = 0;

        final Point2D dst_pt = new Point2D.Float();
        final Point2D src_pt = new Point2D.Float();

        final int dstDataArrays[][] = dst.getIntDataArrays();
        final int dstBandOffsets[] = dst.getBandOffsets();
        final int dstPixelStride = dst.getPixelStride();
        final int dstScanlineStride = dst.getScanlineStride();

        final int srcDataArrays[][] = src.getIntDataArrays();
        final int bandOffsets[] = src.getBandOffsets();
        final int srcPixelStride = src.getPixelStride();
        final int srcScanlineStride = src.getScanlineStride();

        final int dst_num_bands = dst.getNumBands();

        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        // ROI scanline stride
        final byte[] roiDataArray;
        final int roiDataLength;
        final int roiScanlineStride;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiScanlineStride = roi.getScanlineStride();
        } else {
            roiDataArray = null;
            roiDataLength = 0;
            roiScanlineStride = 0;
        }

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;

        if (caseA) {
            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                float s_x = (float) src_pt.getX();
                float s_y = (float) src_pt.getY();

                s_x -= 0.5;
                s_y -= 0.5;

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix * 1.0d;
                fracy = s_y - s_iy * 1.0d;

                for (int x = dst_min_x; x < dst_max_x; x++) {

                    if ((s_ix >= src_rect_x1) && (s_ix < (src_rect_x2 - 1))
                            && (s_iy >= src_rect_y1) && (s_iy < (src_rect_y2 - 1))) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {

                            int posx = (s_ix - srcRectX) * srcPixelStride;
                            int posy = (s_iy - srcRectY) * srcScanlineStride;

                            int posxhigh = posx + srcPixelStride;
                            int posyhigh = posy + srcScanlineStride;

                            int s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]];
                            int s01 = srcDataArrays[k2][posxhigh + posy + bandOffsets[k2]];
                            int s10 = srcDataArrays[k2][posx + posyhigh + bandOffsets[k2]];
                            int s11 = srcDataArrays[k2][posxhigh + posyhigh + bandOffsets[k2]];

                            float s0 = (float) ((s01 - s00) * fracx + s00);
                            float s1 = (float) ((s11 - s10) * fracx + s10);

                            float result = (float) ((s1 - s0) * fracy + s0);

                            int intResult = 0;

                            if (result > (float) Integer.MAX_VALUE) {
                                intResult = Integer.MAX_VALUE;
                            } else if (result < (float) Integer.MIN_VALUE) {
                                intResult = Integer.MIN_VALUE;
                            } else if (result > 0) {
                                intResult = (int) (result + 0.5F);
                            } else {
                                intResult = (int) (result - 0.5F);
                            }

                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = intResult;

                        }
                    } else if (setDestinationNoData) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                        }
                    }

                    // walk
                    if (fracx < fracdx1) {
                        s_ix += incx;
                        fracx += fracdx;
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                    } else {
                        s_iy += incy1;
                        fracy -= fracdy1;
                    }

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }

                // Go to the next line in the destination rectangle
                dstOffset += dstScanlineStride;
            }
        } else if (caseB) {
            if (useROIAccessor) {
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    float s_x = (float) src_pt.getX();
                    float s_y = (float) src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 1, 0, 1);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;
                        // If roiAccessor is present, the y position on the roi image is calculated
                        final int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        final int posxhigh = posx + srcPixelStride;
                        final int posyhigh = posy + srcScanlineStride;

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        final int w00index = baseIndex;
                        final int w01index = baseIndex + 1;
                        final int w10index = baseIndex + roiScanlineStride;
                        final int w11index = baseIndex + roiScanlineStride + 1;

                        final int w00 = w00index < roiDataLength ? roiDataArray[w00index] & 0xff
                                : 0;
                        final int w01 = w01index < roiDataLength ? roiDataArray[w01index] & 0xff
                                : 0;
                        final int w10 = w10index < roiDataLength ? roiDataArray[w10index] & 0xff
                                : 0;
                        final int w11 = w11index < roiDataLength ? roiDataArray[w11index] & 0xff
                                : 0;

                        if (baseIndex > roiDataLength || w00 == 0
                                || (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                // The interpolated value is saved in the destination array
                                int s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]];
                                int s01 = srcDataArrays[k2][posxhigh + posy + bandOffsets[k2]];
                                int s10 = srcDataArrays[k2][posx + posyhigh + bandOffsets[k2]];
                                int s11 = srcDataArrays[k2][posxhigh + posyhigh + bandOffsets[k2]];

                                float s0 = (float) ((s01 - s00) * fracx + s00);
                                float s1 = (float) ((s11 - s10) * fracx + s10);

                                float result = (float) ((s1 - s0) * fracy + s0);

                                int intResult = 0;

                                if (result > (float) Integer.MAX_VALUE) {
                                    intResult = Integer.MAX_VALUE;
                                } else if (result < (float) Integer.MIN_VALUE) {
                                    intResult = Integer.MIN_VALUE;
                                } else if (result > 0) {
                                    intResult = (int) (result + 0.5F);
                                } else {
                                    intResult = (int) (result - 0.5F);
                                }

                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = intResult;
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }
                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                        } else {
                            s_iy += incy1;
                            fracy -= fracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            } else {
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    float s_x = (float) src_pt.getX();
                    float s_y = (float) src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 1, 0, 1);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int posxhigh = posx + srcPixelStride;
                        final int posyhigh = posy + srcScanlineStride;

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (roiBounds.contains(x0, y0)) {

                            final int w00 = roiIter.getSample(x0, y0, 0) & 0xff;
                            final int w01 = roiIter.getSample(x0 + 1, y0, 0) & 0xff;
                            final int w10 = roiIter.getSample(x0, y0 + 1, 0) & 0xff;
                            final int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0) & 0xff;

                            if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                if (setDestinationNoData) {
                                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                                    }
                                }
                            } else {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    // The interpolated value is saved in the destination array
                                    int s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]];
                                    int s01 = srcDataArrays[k2][posxhigh + posy + bandOffsets[k2]];
                                    int s10 = srcDataArrays[k2][posx + posyhigh + bandOffsets[k2]];
                                    int s11 = srcDataArrays[k2][posxhigh + posyhigh
                                            + bandOffsets[k2]];

                                    float s0 = (float) ((s01 - s00) * fracx + s00);
                                    float s1 = (float) ((s11 - s10) * fracx + s10);

                                    float result = (float) ((s1 - s0) * fracy + s0);

                                    int intResult = 0;

                                    if (result > (float) Integer.MAX_VALUE) {
                                        intResult = Integer.MAX_VALUE;
                                    } else if (result < (float) Integer.MIN_VALUE) {
                                        intResult = Integer.MIN_VALUE;
                                    } else if (result > 0) {
                                        intResult = (int) (result + 0.5F);
                                    } else {
                                        intResult = (int) (result - 0.5F);
                                    }

                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = intResult;
                                }
                            }
                        } else {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }
                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                        } else {
                            s_iy += incy1;
                            fracy -= fracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        } else if (caseC) {
            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                float s_x = (float) src_pt.getX();
                float s_y = (float) src_pt.getY();

                s_x -= 0.5;
                s_y -= 0.5;

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix * 1.0d;
                fracy = s_y - s_iy * 1.0d;

                for (int x = dst_min_x; x < dst_max_x; x++) {

                    final int posx = (s_ix - srcRectX) * srcPixelStride;
                    final int posy = (s_iy - srcRectY) * srcScanlineStride;

                    final int posxhigh = posx + srcPixelStride;
                    final int posyhigh = posy + srcScanlineStride;

                    if ((s_ix >= src_rect_x1) && (s_ix < (src_rect_x2 - 1))
                            && (s_iy >= src_rect_y1) && (s_iy < (src_rect_y2 - 1))) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {

                            // The interpolated value is saved in the destination array
                            int s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]];
                            int s01 = srcDataArrays[k2][posxhigh + posy + bandOffsets[k2]];
                            int s10 = srcDataArrays[k2][posx + posyhigh + bandOffsets[k2]];
                            int s11 = srcDataArrays[k2][posxhigh + posyhigh + bandOffsets[k2]];

                            int w00 = noData.contains(s00) ? 0 : 1;
                            int w01 = noData.contains(s01) ? 0 : 1;
                            int w10 = noData.contains(s10) ? 0 : 1;
                            int w11 = noData.contains(s11) ? 0 : 1;

                            if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                if (setDestinationNoData) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                                }
                            } else {
                                double result = computeValue(s00, s01, s10, s11, w00, w01, w10,
                                        w11, fracx, fracy);

                                int intResult = 0;

                                if (result > (float) Integer.MAX_VALUE) {
                                    intResult = Integer.MAX_VALUE;
                                } else if (result < (float) Integer.MIN_VALUE) {
                                    intResult = Integer.MIN_VALUE;
                                } else if (result > 0) {
                                    intResult = (int) (result + 0.5F);
                                } else {
                                    intResult = (int) (result - 0.5F);
                                }

                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = intResult;
                            }
                        }
                    } else if (setDestinationNoData) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                        }
                    }

                    // walk
                    if (fracx < fracdx1) {
                        s_ix += incx;
                        fracx += fracdx;
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                    } else {
                        s_iy += incy1;
                        fracy -= fracdy1;
                    }

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }
                // Go to the next line in the destination rectangle
                dstOffset += dstScanlineStride;
            }
        } else {
            if (useROIAccessor) {

                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    float s_x = (float) src_pt.getX();
                    float s_y = (float) src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 1, 0, 1);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;
                        // If roiAccessor is present, the y position on the roi image is calculated
                        final int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        final int posxhigh = posx + srcPixelStride;
                        final int posyhigh = posy + srcScanlineStride;

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        final int w00index = baseIndex;
                        final int w01index = baseIndex + 1;
                        final int w10index = baseIndex + roiScanlineStride;
                        final int w11index = baseIndex + roiScanlineStride + 1;

                        int w00 = w00index < roiDataLength ? roiDataArray[w00index] & 0xff : 0;
                        int w01 = w01index < roiDataLength ? roiDataArray[w01index] & 0xff : 0;
                        int w10 = w10index < roiDataLength ? roiDataArray[w10index] & 0xff : 0;
                        int w11 = w11index < roiDataLength ? roiDataArray[w11index] & 0xff : 0;

                        if (baseIndex > roiDataLength || w00 == 0
                                || (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                // The interpolated value is saved in the destination array
                                final int s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]];
                                final int s01 = srcDataArrays[k2][posxhigh + posy + bandOffsets[k2]];
                                final int s10 = srcDataArrays[k2][posx + posyhigh + bandOffsets[k2]];
                                final int s11 = srcDataArrays[k2][posxhigh + posyhigh
                                        + bandOffsets[k2]];

                                w00 = noData.contains(s00) ? 0 : 1;
                                w01 = noData.contains(s01) ? 0 : 1;
                                w10 = noData.contains(s10) ? 0 : 1;
                                w11 = noData.contains(s11) ? 0 : 1;

                                if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    if (setDestinationNoData) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                                    }
                                } else {
                                    double result = computeValue(s00, s01, s10, s11, w00, w01, w10,
                                            w11, fracx, fracy);

                                    int intResult = 0;

                                    if (result > (float) Integer.MAX_VALUE) {
                                        intResult = Integer.MAX_VALUE;
                                    } else if (result < (float) Integer.MIN_VALUE) {
                                        intResult = Integer.MIN_VALUE;
                                    } else if (result > 0) {
                                        intResult = (int) (result + 0.5F);
                                    } else {
                                        intResult = (int) (result - 0.5F);
                                    }

                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = intResult;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }
                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                        } else {
                            s_iy += incy1;
                            fracy -= fracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            } else {
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    float s_x = (float) src_pt.getX();
                    float s_y = (float) src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 1, 0, 1);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {
                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int posxhigh = posx + srcPixelStride;
                        final int posyhigh = posy + srcScanlineStride;

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (roiBounds.contains(x0, y0)) {

                            int w00 = roiIter.getSample(x0, y0, 0) & 0xff;
                            int w01 = roiIter.getSample(x0 + 1, y0, 0) & 0xff;
                            int w10 = roiIter.getSample(x0, y0 + 1, 0) & 0xff;
                            int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0) & 0xff;

                            if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                if (setDestinationNoData) {
                                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                                    }
                                }
                            } else {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    // The interpolated value is saved in the destination array
                                    final int s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]];
                                    final int s01 = srcDataArrays[k2][posxhigh + posy
                                            + bandOffsets[k2]];
                                    final int s10 = srcDataArrays[k2][posx + posyhigh
                                            + bandOffsets[k2]];
                                    final int s11 = srcDataArrays[k2][posxhigh + posyhigh
                                            + bandOffsets[k2]];

                                    w00 = noData.contains(s00) ? 0 : 1;
                                    w01 = noData.contains(s01) ? 0 : 1;
                                    w10 = noData.contains(s10) ? 0 : 1;
                                    w11 = noData.contains(s11) ? 0 : 1;

                                    if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                        if (setDestinationNoData) {
                                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                                        }
                                    } else {
                                        double result = computeValue(s00, s01, s10, s11, w00, w01,
                                                w10, w11, fracx, fracy);

                                        int intResult = 0;

                                        if (result > (float) Integer.MAX_VALUE) {
                                            intResult = Integer.MAX_VALUE;
                                        } else if (result < (float) Integer.MIN_VALUE) {
                                            intResult = Integer.MIN_VALUE;
                                        } else if (result > 0) {
                                            intResult = (int) (result + 0.5F);
                                        } else {
                                            intResult = (int) (result - 0.5F);
                                        }

                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = intResult;
                                    }
                                }
                            }
                        } else {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }
                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                        } else {
                            s_iy += incy1;
                            fracy -= fracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        }
    }

    private void floatLoop(int dataType, RasterAccessor src, Rectangle destRect, int srcRectX,
            int srcRectY, RasterAccessor dst, RasterAccessor roi) {

        final float src_rect_x1 = src.getX();
        final float src_rect_y1 = src.getY();
        final float src_rect_x2 = src_rect_x1 + src.getWidth();
        final float src_rect_y2 = src_rect_y1 + src.getHeight();

        double fracx = 0, fracy = 0;

        int dstPixelOffset;
        int dstOffset = 0;

        final Point2D dst_pt = new Point2D.Float();
        final Point2D src_pt = new Point2D.Float();

        final float dstDataArrays[][] = dst.getFloatDataArrays();
        final int dstBandOffsets[] = dst.getBandOffsets();
        final int dstPixelStride = dst.getPixelStride();
        final int dstScanlineStride = dst.getScanlineStride();

        final float srcDataArrays[][] = src.getFloatDataArrays();
        final int bandOffsets[] = src.getBandOffsets();
        final int srcPixelStride = src.getPixelStride();
        final int srcScanlineStride = src.getScanlineStride();

        final int dst_num_bands = dst.getNumBands();

        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        // ROI scanline stride
        final byte[] roiDataArray;
        final int roiDataLength;
        final int roiScanlineStride;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiScanlineStride = roi.getScanlineStride();
        } else {
            roiDataArray = null;
            roiDataLength = 0;
            roiScanlineStride = 0;
        }

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;

        if (caseA) {
            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                float s_x = (float) src_pt.getX();
                float s_y = (float) src_pt.getY();

                s_x -= 0.5;
                s_y -= 0.5;

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix * 1.0d;
                fracy = s_y - s_iy * 1.0d;

                for (int x = dst_min_x; x < dst_max_x; x++) {

                    if ((s_ix >= src_rect_x1) && (s_ix < (src_rect_x2 - 1))
                            && (s_iy >= src_rect_y1) && (s_iy < (src_rect_y2 - 1))) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {

                            int posx = (s_ix - srcRectX) * srcPixelStride;
                            int posy = (s_iy - srcRectY) * srcScanlineStride;

                            int posxhigh = posx + srcPixelStride;
                            int posyhigh = posy + srcScanlineStride;

                            float s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]];
                            float s01 = srcDataArrays[k2][posxhigh + posy + bandOffsets[k2]];
                            float s10 = srcDataArrays[k2][posx + posyhigh + bandOffsets[k2]];
                            float s11 = srcDataArrays[k2][posxhigh + posyhigh + bandOffsets[k2]];

                            float s0 = (float) ((s01 - s00) * fracx + s00);
                            float s1 = (float) ((s11 - s10) * fracx + s10);

                            float result = (float) ((s1 - s0) * fracy + s0);

                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = result;

                        }
                    } else if (setDestinationNoData) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                        }
                    }

                    // walk
                    if (fracx < fracdx1) {
                        s_ix += incx;
                        fracx += fracdx;
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                    } else {
                        s_iy += incy1;
                        fracy -= fracdy1;
                    }

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }

                // Go to the next line in the destination rectangle
                dstOffset += dstScanlineStride;
            }
        } else if (caseB) {
            if (useROIAccessor) {
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    float s_x = (float) src_pt.getX();
                    float s_y = (float) src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 1, 0, 1);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;
                        // If roiAccessor is present, the y position on the roi image is calculated
                        final int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        final int posxhigh = posx + srcPixelStride;
                        final int posyhigh = posy + srcScanlineStride;

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        final int w00index = baseIndex;
                        final int w01index = baseIndex + 1;
                        final int w10index = baseIndex + roiScanlineStride;
                        final int w11index = baseIndex + roiScanlineStride + 1;

                        final int w00 = w00index < roiDataLength ? roiDataArray[w00index] & 0xff
                                : 0;
                        final int w01 = w01index < roiDataLength ? roiDataArray[w01index] & 0xff
                                : 0;
                        final int w10 = w10index < roiDataLength ? roiDataArray[w10index] & 0xff
                                : 0;
                        final int w11 = w11index < roiDataLength ? roiDataArray[w11index] & 0xff
                                : 0;

                        if (baseIndex > roiDataLength || w00 == 0
                                || (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                // The interpolated value is saved in the destination array
                                float s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]];
                                float s01 = srcDataArrays[k2][posxhigh + posy + bandOffsets[k2]];
                                float s10 = srcDataArrays[k2][posx + posyhigh + bandOffsets[k2]];
                                float s11 = srcDataArrays[k2][posxhigh + posyhigh + bandOffsets[k2]];

                                float s0 = (float) ((s01 - s00) * fracx + s00);
                                float s1 = (float) ((s11 - s10) * fracx + s10);

                                float result = (float) ((s1 - s0) * fracy + s0);

                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = result;
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }
                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                        } else {
                            s_iy += incy1;
                            fracy -= fracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            } else {
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    float s_x = (float) src_pt.getX();
                    float s_y = (float) src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 1, 0, 1);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int posxhigh = posx + srcPixelStride;
                        final int posyhigh = posy + srcScanlineStride;

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (roiBounds.contains(x0, y0)) {

                            final int w00 = roiIter.getSample(x0, y0, 0) & 0xff;
                            final int w01 = roiIter.getSample(x0 + 1, y0, 0) & 0xff;
                            final int w10 = roiIter.getSample(x0, y0 + 1, 0) & 0xff;
                            final int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0) & 0xff;

                            if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                if (setDestinationNoData) {
                                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                                    }
                                }
                            } else {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    // The interpolated value is saved in the destination array
                                    float s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]];
                                    float s01 = srcDataArrays[k2][posxhigh + posy + bandOffsets[k2]];
                                    float s10 = srcDataArrays[k2][posx + posyhigh + bandOffsets[k2]];
                                    float s11 = srcDataArrays[k2][posxhigh + posyhigh
                                            + bandOffsets[k2]];

                                    float s0 = (float) ((s01 - s00) * fracx + s00);
                                    float s1 = (float) ((s11 - s10) * fracx + s10);

                                    float result = (float) ((s1 - s0) * fracy + s0);

                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = result;
                                }
                            }
                        } else {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }
                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                        } else {
                            s_iy += incy1;
                            fracy -= fracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        } else if (caseC) {
            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                float s_x = (float) src_pt.getX();
                float s_y = (float) src_pt.getY();

                s_x -= 0.5;
                s_y -= 0.5;

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix * 1.0d;
                fracy = s_y - s_iy * 1.0d;

                for (int x = dst_min_x; x < dst_max_x; x++) {

                    final int posx = (s_ix - srcRectX) * srcPixelStride;
                    final int posy = (s_iy - srcRectY) * srcScanlineStride;

                    final int posxhigh = posx + srcPixelStride;
                    final int posyhigh = posy + srcScanlineStride;

                    if ((s_ix >= src_rect_x1) && (s_ix < (src_rect_x2 - 1))
                            && (s_iy >= src_rect_y1) && (s_iy < (src_rect_y2 - 1))) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {

                            // The interpolated value is saved in the destination array
                            float s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]];
                            float s01 = srcDataArrays[k2][posxhigh + posy + bandOffsets[k2]];
                            float s10 = srcDataArrays[k2][posx + posyhigh + bandOffsets[k2]];
                            float s11 = srcDataArrays[k2][posxhigh + posyhigh + bandOffsets[k2]];

                            int w00 = 1;
                            int w01 = 1;
                            int w10 = 1;
                            int w11 = 1;

                            w00 = noData.contains(s00) || Float.isNaN(s00) ? 0 : 1;
                            w01 = noData.contains(s01) || Float.isNaN(s01) ? 0 : 1;
                            w10 = noData.contains(s10) || Float.isNaN(s10) ? 0 : 1;
                            w11 = noData.contains(s11) || Float.isNaN(s11) ? 0 : 1;

                            if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                if (setDestinationNoData) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                                }
                            } else {
                                double result = computeValue(s00, s01, s10, s11, w00, w01, w10,
                                        w11, fracx, fracy);

                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (float) result;
                            }
                        }
                    } else if (setDestinationNoData) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                        }
                    }

                    // walk
                    if (fracx < fracdx1) {
                        s_ix += incx;
                        fracx += fracdx;
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                    } else {
                        s_iy += incy1;
                        fracy -= fracdy1;
                    }

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }
                // Go to the next line in the destination rectangle
                dstOffset += dstScanlineStride;
            }
        } else {
            if (useROIAccessor) {

                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    float s_x = (float) src_pt.getX();
                    float s_y = (float) src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 1, 0, 1);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;
                        // If roiAccessor is present, the y position on the roi image is calculated
                        final int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        final int posxhigh = posx + srcPixelStride;
                        final int posyhigh = posy + srcScanlineStride;

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        final int w00index = baseIndex;
                        final int w01index = baseIndex + 1;
                        final int w10index = baseIndex + roiScanlineStride;
                        final int w11index = baseIndex + roiScanlineStride + 1;

                        int w00 = w00index < roiDataLength ? roiDataArray[w00index] & 0xff : 0;
                        int w01 = w01index < roiDataLength ? roiDataArray[w01index] & 0xff : 0;
                        int w10 = w10index < roiDataLength ? roiDataArray[w10index] & 0xff : 0;
                        int w11 = w11index < roiDataLength ? roiDataArray[w11index] & 0xff : 0;

                        if (baseIndex > roiDataLength || w00 == 0
                                || (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                // The interpolated value is saved in the destination array
                                final float s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]];
                                final float s01 = srcDataArrays[k2][posxhigh + posy
                                        + bandOffsets[k2]];
                                final float s10 = srcDataArrays[k2][posx + posyhigh
                                        + bandOffsets[k2]];
                                final float s11 = srcDataArrays[k2][posxhigh + posyhigh
                                        + bandOffsets[k2]];

                                w00 = 1;
                                w01 = 1;
                                w10 = 1;
                                w11 = 1;

                                w00 = noData.contains(s00) || Float.isNaN(s00) ? 0 : 1;
                                w01 = noData.contains(s01) || Float.isNaN(s01) ? 0 : 1;
                                w10 = noData.contains(s10) || Float.isNaN(s10) ? 0 : 1;
                                w11 = noData.contains(s11) || Float.isNaN(s11) ? 0 : 1;

                                if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    if (setDestinationNoData) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                                    }
                                } else {
                                    double result = computeValue(s00, s01, s10, s11, w00, w01, w10,
                                            w11, fracx, fracy);

                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (float) (result);
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }
                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                        } else {
                            s_iy += incy1;
                            fracy -= fracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            } else {
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    float s_x = (float) src_pt.getX();
                    float s_y = (float) src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 1, 0, 1);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {
                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int posxhigh = posx + srcPixelStride;
                        final int posyhigh = posy + srcScanlineStride;

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (roiBounds.contains(x0, y0)) {

                            int w00 = roiIter.getSample(x0, y0, 0) & 0xff;
                            int w01 = roiIter.getSample(x0 + 1, y0, 0) & 0xff;
                            int w10 = roiIter.getSample(x0, y0 + 1, 0) & 0xff;
                            int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0) & 0xff;

                            if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                if (setDestinationNoData) {
                                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                                    }
                                }
                            } else {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    // The interpolated value is saved in the destination array
                                    final float s00 = srcDataArrays[k2][posx + posy
                                            + bandOffsets[k2]];
                                    final float s01 = srcDataArrays[k2][posxhigh + posy
                                            + bandOffsets[k2]];
                                    final float s10 = srcDataArrays[k2][posx + posyhigh
                                            + bandOffsets[k2]];
                                    final float s11 = srcDataArrays[k2][posxhigh + posyhigh
                                            + bandOffsets[k2]];

                                    w00 = 1;
                                    w01 = 1;
                                    w10 = 1;
                                    w11 = 1;

                                    w00 = noData.contains(s00) || Float.isNaN(s00) ? 0 : 1;
                                    w01 = noData.contains(s01) || Float.isNaN(s01) ? 0 : 1;
                                    w10 = noData.contains(s10) || Float.isNaN(s10) ? 0 : 1;
                                    w11 = noData.contains(s11) || Float.isNaN(s11) ? 0 : 1;

                                    if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                        if (setDestinationNoData) {
                                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                                        }
                                    } else {
                                        double result = computeValue(s00, s01, s10, s11, w00, w01,
                                                w10, w11, fracx, fracy);

                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (float) (result);
                                    }
                                }
                            }
                        } else {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }
                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                        } else {
                            s_iy += incy1;
                            fracy -= fracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        }
    }

    private void doubleLoop(int dataType, RasterAccessor src, Rectangle destRect, int srcRectX,
            int srcRectY, RasterAccessor dst, RasterAccessor roi) {

        final float src_rect_x1 = src.getX();
        final float src_rect_y1 = src.getY();
        final float src_rect_x2 = src_rect_x1 + src.getWidth();
        final float src_rect_y2 = src_rect_y1 + src.getHeight();

        double fracx = 0, fracy = 0;

        int dstPixelOffset;
        int dstOffset = 0;

        final Point2D dst_pt = new Point2D.Float();
        final Point2D src_pt = new Point2D.Float();

        final double dstDataArrays[][] = dst.getDoubleDataArrays();
        final int dstBandOffsets[] = dst.getBandOffsets();
        final int dstPixelStride = dst.getPixelStride();
        final int dstScanlineStride = dst.getScanlineStride();

        final double srcDataArrays[][] = src.getDoubleDataArrays();
        final int bandOffsets[] = src.getBandOffsets();
        final int srcPixelStride = src.getPixelStride();
        final int srcScanlineStride = src.getScanlineStride();

        final int dst_num_bands = dst.getNumBands();

        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        // ROI scanline stride
        final byte[] roiDataArray;
        final int roiDataLength;
        final int roiScanlineStride;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiScanlineStride = roi.getScanlineStride();
        } else {
            roiDataArray = null;
            roiDataLength = 0;
            roiScanlineStride = 0;
        }

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;

        if (caseA) {
            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                float s_x = (float) src_pt.getX();
                float s_y = (float) src_pt.getY();

                s_x -= 0.5;
                s_y -= 0.5;

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix * 1.0d;
                fracy = s_y - s_iy * 1.0d;

                for (int x = dst_min_x; x < dst_max_x; x++) {

                    if ((s_ix >= src_rect_x1) && (s_ix < (src_rect_x2 - 1))
                            && (s_iy >= src_rect_y1) && (s_iy < (src_rect_y2 - 1))) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {

                            int posx = (s_ix - srcRectX) * srcPixelStride;
                            int posy = (s_iy - srcRectY) * srcScanlineStride;

                            int posxhigh = posx + srcPixelStride;
                            int posyhigh = posy + srcScanlineStride;

                            double s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]];
                            double s01 = srcDataArrays[k2][posxhigh + posy + bandOffsets[k2]];
                            double s10 = srcDataArrays[k2][posx + posyhigh + bandOffsets[k2]];
                            double s11 = srcDataArrays[k2][posxhigh + posyhigh + bandOffsets[k2]];

                            double s0 = ((s01 - s00) * fracx + s00);
                            double s1 = ((s11 - s10) * fracx + s10);

                            double result = ((s1 - s0) * fracy + s0);

                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = result;

                        }
                    } else if (setDestinationNoData) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                        }
                    }

                    // walk
                    if (fracx < fracdx1) {
                        s_ix += incx;
                        fracx += fracdx;
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                    } else {
                        s_iy += incy1;
                        fracy -= fracdy1;
                    }

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }

                // Go to the next line in the destination rectangle
                dstOffset += dstScanlineStride;
            }
        } else if (caseB) {
            if (useROIAccessor) {
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    float s_x = (float) src_pt.getX();
                    float s_y = (float) src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 1, 0, 1);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;
                        // If roiAccessor is present, the y position on the roi image is calculated
                        final int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        final int posxhigh = posx + srcPixelStride;
                        final int posyhigh = posy + srcScanlineStride;

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        final int w00index = baseIndex;
                        final int w01index = baseIndex + 1;
                        final int w10index = baseIndex + roiScanlineStride;
                        final int w11index = baseIndex + roiScanlineStride + 1;

                        final int w00 = w00index < roiDataLength ? roiDataArray[w00index] & 0xff
                                : 0;
                        final int w01 = w01index < roiDataLength ? roiDataArray[w01index] & 0xff
                                : 0;
                        final int w10 = w10index < roiDataLength ? roiDataArray[w10index] & 0xff
                                : 0;
                        final int w11 = w11index < roiDataLength ? roiDataArray[w11index] & 0xff
                                : 0;

                        if (baseIndex > roiDataLength || w00 == 0
                                || (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                // The interpolated value is saved in the destination array
                                double s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]];
                                double s01 = srcDataArrays[k2][posxhigh + posy + bandOffsets[k2]];
                                double s10 = srcDataArrays[k2][posx + posyhigh + bandOffsets[k2]];
                                double s11 = srcDataArrays[k2][posxhigh + posyhigh
                                        + bandOffsets[k2]];

                                double s0 = ((s01 - s00) * fracx + s00);
                                double s1 = ((s11 - s10) * fracx + s10);

                                double result = ((s1 - s0) * fracy + s0);

                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = result;
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }
                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                        } else {
                            s_iy += incy1;
                            fracy -= fracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            } else {
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    float s_x = (float) src_pt.getX();
                    float s_y = (float) src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 1, 0, 1);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int posxhigh = posx + srcPixelStride;
                        final int posyhigh = posy + srcScanlineStride;

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (roiBounds.contains(x0, y0)) {

                            final int w00 = roiIter.getSample(x0, y0, 0) & 0xff;
                            final int w01 = roiIter.getSample(x0 + 1, y0, 0) & 0xff;
                            final int w10 = roiIter.getSample(x0, y0 + 1, 0) & 0xff;
                            final int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0) & 0xff;

                            if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                if (setDestinationNoData) {
                                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                                    }
                                }
                            } else {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    // The interpolated value is saved in the destination array
                                    double s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]];
                                    double s01 = srcDataArrays[k2][posxhigh + posy
                                            + bandOffsets[k2]];
                                    double s10 = srcDataArrays[k2][posx + posyhigh
                                            + bandOffsets[k2]];
                                    double s11 = srcDataArrays[k2][posxhigh + posyhigh
                                            + bandOffsets[k2]];

                                    double s0 = ((s01 - s00) * fracx + s00);
                                    double s1 = ((s11 - s10) * fracx + s10);

                                    double result = ((s1 - s0) * fracy + s0);

                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = result;
                                }
                            }
                        } else {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }
                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                        } else {
                            s_iy += incy1;
                            fracy -= fracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        } else if (caseC) {
            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                float s_x = (float) src_pt.getX();
                float s_y = (float) src_pt.getY();

                s_x -= 0.5;
                s_y -= 0.5;

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix * 1.0d;
                fracy = s_y - s_iy * 1.0d;

                for (int x = dst_min_x; x < dst_max_x; x++) {

                    final int posx = (s_ix - srcRectX) * srcPixelStride;
                    final int posy = (s_iy - srcRectY) * srcScanlineStride;

                    final int posxhigh = posx + srcPixelStride;
                    final int posyhigh = posy + srcScanlineStride;

                    if ((s_ix >= src_rect_x1) && (s_ix < (src_rect_x2 - 1))
                            && (s_iy >= src_rect_y1) && (s_iy < (src_rect_y2 - 1))) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {

                            // The interpolated value is saved in the destination array
                            double s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]];
                            double s01 = srcDataArrays[k2][posxhigh + posy + bandOffsets[k2]];
                            double s10 = srcDataArrays[k2][posx + posyhigh + bandOffsets[k2]];
                            double s11 = srcDataArrays[k2][posxhigh + posyhigh + bandOffsets[k2]];

                            int w00 = 1;
                            int w01 = 1;
                            int w10 = 1;
                            int w11 = 1;

                            w00 = noData.contains(s00) || Double.isNaN(s00) ? 0 : 1;
                            w01 = noData.contains(s01) || Double.isNaN(s01) ? 0 : 1;
                            w10 = noData.contains(s10) || Double.isNaN(s10) ? 0 : 1;
                            w11 = noData.contains(s11) || Double.isNaN(s11) ? 0 : 1;

                            if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                if (setDestinationNoData) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                                }
                            } else {
                                double result = computeValue(s00, s01, s10, s11, w00, w01, w10,
                                        w11, fracx, fracy);

                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = result;
                            }
                        }
                    } else if (setDestinationNoData) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                        }
                    }

                    // walk
                    if (fracx < fracdx1) {
                        s_ix += incx;
                        fracx += fracdx;
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                    } else {
                        s_iy += incy1;
                        fracy -= fracdy1;
                    }

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }
                // Go to the next line in the destination rectangle
                dstOffset += dstScanlineStride;
            }
        } else {
            if (useROIAccessor) {

                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    float s_x = (float) src_pt.getX();
                    float s_y = (float) src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 1, 0, 1);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;
                        // If roiAccessor is present, the y position on the roi image is calculated
                        final int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        final int posxhigh = posx + srcPixelStride;
                        final int posyhigh = posy + srcScanlineStride;

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        final int w00index = baseIndex;
                        final int w01index = baseIndex + 1;
                        final int w10index = baseIndex + roiScanlineStride;
                        final int w11index = baseIndex + roiScanlineStride + 1;

                        int w00 = w00index < roiDataLength ? roiDataArray[w00index] & 0xff : 0;
                        int w01 = w01index < roiDataLength ? roiDataArray[w01index] & 0xff : 0;
                        int w10 = w10index < roiDataLength ? roiDataArray[w10index] & 0xff : 0;
                        int w11 = w11index < roiDataLength ? roiDataArray[w11index] & 0xff : 0;

                        if (baseIndex > roiDataLength || w00 == 0
                                || (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                // The interpolated value is saved in the destination array
                                final double s00 = srcDataArrays[k2][posx + posy + bandOffsets[k2]];
                                final double s01 = srcDataArrays[k2][posxhigh + posy
                                        + bandOffsets[k2]];
                                final double s10 = srcDataArrays[k2][posx + posyhigh
                                        + bandOffsets[k2]];
                                final double s11 = srcDataArrays[k2][posxhigh + posyhigh
                                        + bandOffsets[k2]];

                                w00 = 1;
                                w01 = 1;
                                w10 = 1;
                                w11 = 1;

                                w00 = noData.contains(s00) || Double.isNaN(s00) ? 0 : 1;
                                w01 = noData.contains(s01) || Double.isNaN(s01) ? 0 : 1;
                                w10 = noData.contains(s10) || Double.isNaN(s10) ? 0 : 1;
                                w11 = noData.contains(s11) || Double.isNaN(s11) ? 0 : 1;

                                if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    if (setDestinationNoData) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                                    }
                                } else {
                                    double result = computeValue(s00, s01, s10, s11, w00, w01, w10,
                                            w11, fracx, fracy);

                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = result;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }
                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                        } else {
                            s_iy += incy1;
                            fracy -= fracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            } else {
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    float s_x = (float) src_pt.getX();
                    float s_y = (float) src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 1, 0, 1);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {
                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int posxhigh = posx + srcPixelStride;
                        final int posyhigh = posy + srcScanlineStride;

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (roiBounds.contains(x0, y0)) {

                            int w00 = roiIter.getSample(x0, y0, 0) & 0xff;
                            int w01 = roiIter.getSample(x0 + 1, y0, 0) & 0xff;
                            int w10 = roiIter.getSample(x0, y0 + 1, 0) & 0xff;
                            int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0) & 0xff;

                            if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                if (setDestinationNoData) {
                                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                                    }
                                }
                            } else {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    // The interpolated value is saved in the destination array
                                    final double s00 = srcDataArrays[k2][posx + posy
                                            + bandOffsets[k2]];
                                    final double s01 = srcDataArrays[k2][posxhigh + posy
                                            + bandOffsets[k2]];
                                    final double s10 = srcDataArrays[k2][posx + posyhigh
                                            + bandOffsets[k2]];
                                    final double s11 = srcDataArrays[k2][posxhigh + posyhigh
                                            + bandOffsets[k2]];

                                    w00 = 1;
                                    w01 = 1;
                                    w10 = 1;
                                    w11 = 1;

                                    w00 = noData.contains(s00) || Double.isNaN(s00) ? 0 : 1;
                                    w01 = noData.contains(s01) || Double.isNaN(s01) ? 0 : 1;
                                    w10 = noData.contains(s10) || Double.isNaN(s10) ? 0 : 1;
                                    w11 = noData.contains(s11) || Double.isNaN(s11) ? 0 : 1;

                                    if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                        if (setDestinationNoData) {
                                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                                        }
                                    } else {
                                        double result = computeValue(s00, s01, s10, s11, w00, w01,
                                                w10, w11, fracx, fracy);

                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = result;
                                    }
                                }
                            }
                        } else {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }
                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                        } else {
                            s_iy += incy1;
                            fracy -= fracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        }
    }

    /* Private method for calculate bilinear interpolation for float/double dataType */
    private double computeValue(double s00, double s01, double s10, double s11, double w00,
            double w01, double w10, double w11, double xfrac, double yfrac) {

        double s0 = 0;
        double s1 = 0;
        double s = 0;

        // Complementary values of the fractional part
        double xfracCompl = 1 - xfrac;
        double yfracCompl = 1 - yfrac;

        if (w00 == 0 || w01 == 0 || w10 == 0 || w11 == 0) {

            if (w00 == 0 && w01 == 0) {
                s0 = 0;
            } else if (w00 == 0) { // w01 = 1
                s0 = s01 * xfrac;
            } else if (w01 == 0) {// w00 = 1
                s0 = s00 * xfracCompl;// s00;
            } else {// w00 = 1 & W01 = 1
                s0 = (s01 - s00) * xfrac + s00;
            }

            // lower value

            if (w10 == 0 && w11 == 0) {
                s1 = 0;
            } else if (w10 == 0) { // w11 = 1
                s1 = s11 * xfrac;
            } else if (w11 == 0) { // w10 = 1
                s1 = s10 * xfracCompl;// - (s10 * xfrac); //s10;
            } else {
                s1 = (s11 - s10) * xfrac + s10;
            }

            if (w00 == 0 && w01 == 0) {
                s = s1 * yfrac;
            } else {
                if (w10 == 0 && w11 == 0) {
                    s = s0 * yfracCompl;
                } else {
                    s = (s1 - s0) * yfrac + s0;
                }
            }
        } else {

            // Perform the bilinear interpolation because all the weight are not 0.
            s0 = (s01 - s00) * xfrac + s00;
            s1 = (s11 - s10) * xfrac + s10;
            s = (s1 - s0) * yfrac + s0;
        }

        return s;
    }
}
