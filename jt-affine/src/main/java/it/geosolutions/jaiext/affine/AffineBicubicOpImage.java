package it.geosolutions.jaiext.affine;

import it.geosolutions.jaiext.interpolators.InterpolationBicubic;

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

public class AffineBicubicOpImage extends AffineOpImage {

    /** Nearest-Neighbor interpolator */
    protected InterpolationBicubic interpBN = null;
    /**Byte lookuptable used if no data are present*/
    protected final byte[] byteLookupTable = new byte[256];

    /** ROI extender */
    final static BorderExtender roiExtender = BorderExtender
            .createInstance(BorderExtender.BORDER_ZERO);

    /** Value indicating if destination No Data must be set if the pixel is outside the source rectangle */
    private boolean setDestinationNoData;

    /** Bicubic Horizontal coefficients for integer type */
    private int[] dataHi;

    /** Bicubic Vertical coefficients for integer type */
    private int[] dataVi;

    /** Bicubic Horizontal coefficients for float type */
    private float[] dataHf;

    /** Bicubic Vertical coefficients for float type */
    private float[] dataVf;

    /** Bicubic Horizontal coefficients for double type */
    private double[] dataHd;

    /** Bicubic Vertical coefficients for double type */
    private double[] dataVd;

    /** Subsample bits used for bicubic interpolation */
    protected int subsampleBits;

    private int shift;

    private int round;

    private int precisionBits;

    public AffineBicubicOpImage(RenderedImage source, BorderExtender extender, Map config,
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
        if (interp instanceof InterpolationBicubic) {
            interpBN = (InterpolationBicubic) interp;
            this.interp = interpBN;
            interpBN.setROIdata(roiBounds, roiIter);
            noData = interpBN.getNoDataRange();

            switch (srcDataType) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_INT:
                dataHi = interpBN.getHorizontalTableData();
                dataVi = interpBN.getVerticalTableData();
                break;
            case DataBuffer.TYPE_FLOAT:
                dataHf = interpBN.getHorizontalTableDataFloat();
                dataVf = interpBN.getVerticalTableDataFloat();
                break;
            case DataBuffer.TYPE_DOUBLE:
                dataHd = interpBN.getHorizontalTableDataDouble();
                dataVd = interpBN.getVerticalTableDataDouble();
                break;
            default:
                throw new IllegalArgumentException("Wrong data Type");
            }

            subsampleBits = interpBN.getSubsampleBitsH();
            shift = 1 << subsampleBits;
            precisionBits = interpBN.getPrecisionBits();

            if (precisionBits > 0) {
                round = 1 << (precisionBits - 1);
            }

            this.useROIAccessor = false;
            if (noData != null) {
                hasNoData = true;
                destinationNoDataDouble = interpBN.getDestinationNoData();
            } else if (hasROI) {
                destinationNoDataDouble = interpBN.getDestinationNoData();
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
            if (hasNoData) {
                this.isNotPointRange = !noData.isPoint();
            }
            destinationNoDataFloat = (float) destinationNoDataDouble;
            break;
        case DataBuffer.TYPE_DOUBLE:
            if (hasNoData) {
                this.isNotPointRange = !noData.isPoint();
            }
            break;
        default:
            throw new IllegalArgumentException("Wrong data Type");
        }

    }

    /** Method for evaluating the destination image tile without ROI */
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        computeRect(sources, dest, destRect, null);
    }

    /** Method for evaluating the destination image tile with ROI */
    @Override
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

                    // integral fractional value calculation
                    int xfrac = (int) (shift * fracx);
                    int yfrac = (int) (shift * fracy);
                    // X and Y offset initialization
                    int offsetX = 4 * xfrac;
                    int offsetY = 4 * yfrac;

                    int posx = (s_ix - srcRectX) * srcPixelStride;
                    int posy = (s_iy - srcRectY) * srcScanlineStride;

                    int pos = posx + posy;

                    if ((s_ix >= src_rect_x1 + 1) && (s_ix < (src_rect_x2 - 2))
                            && (s_iy >= (src_rect_y1 + 1)) && (s_iy < (src_rect_y2 - 2))) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            long sum = 0;

                            int result = 0;

                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            for (int h = 0; h < 4; h++) {
                                // Row temporary sum initialization
                                long temp = 0;
                                for (int z = 0; z < 4; z++) {
                                    // Selection of one pixel
                                    int pixelValue = srcDataArrays[k2][pos + (z - 1)
                                            * srcPixelStride + (h - 1) * srcScanlineStride
                                            + bandOffsets[k2]] & 0xff;
                                    // Update of the temporary sum
                                    temp += (pixelValue * (long) dataHi[offsetX + z]);
                                }
                                // Vertical sum update
                                sum += ((temp + round) >> precisionBits)
                                        * (long) dataVi[offsetY + h];
                            }
                            // Interpolation
                            result = (int) ((sum + round) >> precisionBits);

                            if (result > 255) {
                                result = 255;
                            } else if (result < 0) {
                                result = 0;
                            }

                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (byte) (result & 0xff);
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
                        if (fracx == 1.0F) {
                            // Avoid overflow in the interpolation table
                            fracx = 0.999999F;
                        }
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                        if (fracy == 1.0F) {
                            // Avoid overflow in the interpolation table
                            fracy = 0.999999F;
                        }
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
                            dst_min_x, dst_max_x, 1, 2, 1, 2);
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

                        // integral fractional value calculation
                        final int xfrac = (int) (shift * fracx);
                        final int yfrac = (int) (shift * fracy);
                        // X and Y offset initialization
                        final int offsetX = 4 * xfrac;
                        final int offsetY = 4 * yfrac;

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;

                        // If roiAccessor is present, the y position on the roi image is calculated
                        final int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                final int[][] pixelKernel = new int[4][4];

                                int result = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride
                                                + bandOffsets[k2]] & 0xff;
                                        int index = baseIndex - 1 + z + (h - 1)
                                                * (roiScanlineStride);
                                        if (index < roiDataLength) {
                                            // Update of the weight sum
                                            tmpROI += ((byte) (roiDataArray[index] & 0xff) != 0 ? 1
                                                    : 0);
                                        }
                                    }
                                }

                                // Control if the 16 pixel are outside the ROI
                                if (tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                                } else {
                                    for (int h = 0; h < 4; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        for (int z = 0; z < 4; z++) {
                                            // Update of the temporary sum
                                            tempSum += (pixelKernel[h][z] * (long) dataHi[offsetX
                                                    + z]);
                                        }
                                        // Vertical sum update
                                        sum += ((tempSum + round) >> precisionBits)
                                                * (long) dataVi[offsetY + h];
                                    }
                                    // Interpolation
                                    result = (int) ((sum + round) >> precisionBits);

                                    // Clamp
                                    if (result > 255) {
                                        result = 255;
                                    } else if (result < 0) {
                                        result = 0;
                                    }
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (byte) (result & 0xff);
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                            if (fracx == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracx = 0.999999F;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracy = 0.999999F;
                            }
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
                            dst_min_x, dst_max_x, 1, 2, 1, 2);
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

                        // integral fractional value calculation
                        final int xfrac = (int) (shift * fracx);
                        final int yfrac = (int) (shift * fracy);
                        // X and Y offset initialization
                        final int offsetX = 4 * xfrac;
                        final int offsetY = 4 * yfrac;

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;

                        // PixelPositions
                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (!roiBounds.contains(x0, y0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                final int[][] pixelKernel = new int[4][4];

                                int result = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride] & 0xff;
                                        tmpROI += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0) & 0xff;
                                    }
                                }

                                // Control if the 16 pixel are outside the ROI
                                if (tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                                } else {
                                    for (int h = 0; h < 4; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        for (int z = 0; z < 4; z++) {
                                            // Update of the temporary sum
                                            tempSum += (pixelKernel[h][z] * (long) dataHi[offsetX
                                                    + z]);
                                        }
                                        // Vertical sum update
                                        sum += ((tempSum + round) >> precisionBits)
                                                * (long) dataVi[offsetY + h];
                                    }
                                    // Interpolation
                                    result = (int) ((sum + round) >> precisionBits);

                                    // Clamp
                                    if (result > 255) {
                                        result = 255;
                                    } else if (result < 0) {
                                        result = 0;
                                    }
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (byte) (result & 0xff);
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                            if (fracx == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracx = 0.999999F;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracy = 0.999999F;
                            }
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

                    // integral fractional value calculation
                    final int xfrac = (int) (shift * fracx);
                    final int yfrac = (int) (shift * fracy);
                    // X and Y offset initialization
                    final int offsetX = 4 * xfrac;
                    final int offsetY = 4 * yfrac;

                    final int posx = (s_ix - srcRectX) * srcPixelStride;
                    final int posy = (s_iy - srcRectY) * srcScanlineStride;

                    final int pos = posx + posy;

                    if ((s_ix >= src_rect_x1 + 1) && (s_ix < (src_rect_x2 - 2))
                            && (s_iy >= (src_rect_y1 + 1)) && (s_iy < (src_rect_y2 - 2))) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            long sum = 0;

                            final long[][] pixelKernel = new long[4][4];
                            int[][] weightArray = new int[4][4];
                            int[] weightArrayVertical = new int[4];

                            int result = 0;

                            int tmpND = 0;

                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            // and cycle for filling all the ROI index by shifting of 1 on the x axis
                            // and by 1 on the y axis.
                            for (int h = 0; h < 4; h++) {
                                for (int z = 0; z < 4; z++) {
                                    // Selection of one pixel
                                    pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                            * srcPixelStride + (h - 1) * srcScanlineStride] & 0xff;
                                    if (byteLookupTable[(int) pixelKernel[h][z]] != destinationNoDataByte) {
                                        tmpND++;
                                        weightArray[h][z] = 1;
                                    }
                                }
                            }

                            // Control if the 16 pixel are outside the ROI
                            if (tmpND == 0) {
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                            } else {
                                long[] sumArray = new long[4];

                                for (int h = 0; h < 4; h++) {
                                    // Row temporary sum initialization
                                    long tempSum = 0;
                                    long[] tempData = bicubicInpainting(pixelKernel[h],
                                            weightArray[h], null);
                                    for (int z = 0; z < 4; z++) {
                                        // Update of the temporary sum
                                        tempSum += (tempData[z] * (long) dataHi[offsetX + z]);
                                    }
                                    weightArrayVertical[h] = weightArray[h][0] + weightArray[h][1]
                                            + weightArray[h][2] + weightArray[h][3];
                                    sumArray[h] = ((tempSum + round) >> precisionBits);
                                }

                                long[] tempData = bicubicInpainting(sumArray, null,
                                        weightArrayVertical);

                                // Vertical sum update
                                for (int h = 0; h < 4; h++) {
                                    // Update of the temporary sum
                                    sum += tempData[h] * (long) dataVi[offsetY + h];
                                }

                                // Interpolation
                                result = (int) ((sum + round) >> precisionBits);

                                // Clamp
                                if (result > 255) {
                                    result = 255;
                                } else if (result < 0) {
                                    result = 0;
                                }
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (byte) (result & 0xff);
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
                        if (fracx == 1.0F) {
                            // Avoid overflow in the interpolation table
                            fracx = 0.999999F;
                        }
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                        if (fracy == 1.0F) {
                            // Avoid overflow in the interpolation table
                            fracy = 0.999999F;
                        }
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
                            dst_min_x, dst_max_x, 1, 2, 1, 2);
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

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        // integral fractional value calculation
                        final int xfrac = (int) (shift * fracx);
                        final int yfrac = (int) (shift * fracy);
                        // X and Y offset initialization
                        final int offsetX = 4 * xfrac;
                        final int offsetY = 4 * yfrac;

                        final int pos = posx + posy;

                        if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                final long[][] pixelKernel = new long[4][4];
                                int[][] weightArray = new int[4][4];
                                int[] weightArrayVertical = new int[4];

                                int result = 0;

                                int tmpND = 0;
                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride] & 0xff;
                                        int index = baseIndex - 1 + z + (h - 1)
                                                * (roiScanlineStride);
                                        if (index < roiDataLength) {
                                            // Update of the weight sum
                                            tmpROI += ((byte) (roiDataArray[index] & 0xff) != 0 ? 1
                                                    : 0);
                                        }

                                        if (byteLookupTable[(int) pixelKernel[h][z]] != destinationNoDataByte) {
                                            tmpND++;
                                            weightArray[h][z] = 1;
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                if (tmpND == 0 || tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                                } else {
                                    long[] sumArray = new long[4];

                                    for (int h = 0; h < 4; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        long[] tempData = bicubicInpainting(pixelKernel[h],
                                                weightArray[h], null);
                                        for (int z = 0; z < 4; z++) {
                                            // Update of the temporary sum
                                            tempSum += (tempData[z] * (long) dataHi[offsetX + z]);
                                        }
                                        weightArrayVertical[h] = weightArray[h][0]
                                                + weightArray[h][1] + weightArray[h][2]
                                                + weightArray[h][3];
                                        sumArray[h] = ((tempSum + round) >> precisionBits);
                                    }

                                    long[] tempData = bicubicInpainting(sumArray, null,
                                            weightArrayVertical);

                                    // Vertical sum update
                                    for (int h = 0; h < 4; h++) {
                                        // Update of the temporary sum
                                        sum += tempData[h] * (long) dataVi[offsetY + h];
                                    }

                                    // Interpolation
                                    result = (int) ((sum + round) >> precisionBits);

                                    // Clamp
                                    if (result > 255) {
                                        result = 255;
                                    } else if (result < 0) {
                                        result = 0;
                                    }
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (byte) (result & 0xff);
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                            if (fracx == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracx = 0.999999F;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracy = 0.999999F;
                            }
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
                            dst_min_x, dst_max_x, 1, 2, 1, 2);
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

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        // integral fractional value calculation
                        final int xfrac = (int) (shift * fracx);
                        final int yfrac = (int) (shift * fracy);
                        // X and Y offset initialization
                        final int offsetX = 4 * xfrac;
                        final int offsetY = 4 * yfrac;

                        final int pos = posx + posy;

                        if (!roiBounds.contains(x0, y0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                final long[][] pixelKernel = new long[4][4];
                                int[][] weightArray = new int[4][4];
                                int[] weightArrayVertical = new int[4];

                                int result = 0;

                                int tmpND = 0;
                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride] & 0xff;

                                        tmpROI += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0) & 0xff;

                                        if (byteLookupTable[(int) pixelKernel[h][z]] != destinationNoDataByte) {
                                            tmpND++;
                                            weightArray[h][z] = 1;
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                if (tmpND == 0 || tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                                } else {
                                    long[] sumArray = new long[4];

                                    for (int h = 0; h < 4; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        long[] tempData = bicubicInpainting(pixelKernel[h],
                                                weightArray[h], null);
                                        for (int z = 0; z < 4; z++) {
                                            // Update of the temporary sum
                                            tempSum += (tempData[z] * (long) dataHi[offsetX + z]);
                                        }
                                        weightArrayVertical[h] = weightArray[h][0]
                                                + weightArray[h][1] + weightArray[h][2]
                                                + weightArray[h][3];
                                        sumArray[h] = ((tempSum + round) >> precisionBits);
                                    }

                                    long[] tempData = bicubicInpainting(sumArray, null,
                                            weightArrayVertical);

                                    // Vertical sum update
                                    for (int h = 0; h < 4; h++) {
                                        // Update of the temporary sum
                                        sum += tempData[h] * (long) dataVi[offsetY + h];
                                    }

                                    // Interpolation
                                    result = (int) ((sum + round) >> precisionBits);

                                    // Clamp
                                    if (result > 255) {
                                        result = 255;
                                    } else if (result < 0) {
                                        result = 0;
                                    }
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (byte) (result & 0xff);
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                            if (fracx == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracx = 0.999999F;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracy = 0.999999F;
                            }
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

    private void ushortLoop(int dataType, RasterAccessor src, Rectangle destRect,
            int srcRectX, int srcRectY, RasterAccessor dst, RasterAccessor roi) {

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

                    // integral fractional value calculation
                    int xfrac = (int) (shift * fracx);
                    int yfrac = (int) (shift * fracy);
                    // X and Y offset initialization
                    int offsetX = 4 * xfrac;
                    int offsetY = 4 * yfrac;

                    int posx = (s_ix - srcRectX) * srcPixelStride;
                    int posy = (s_iy - srcRectY) * srcScanlineStride;

                    int pos = posx + posy;

                    if ((s_ix >= src_rect_x1 + 1) && (s_ix < (src_rect_x2 - 2))
                            && (s_iy >= (src_rect_y1 + 1)) && (s_iy < (src_rect_y2 - 2))) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            long sum = 0;

                            int result = 0;

                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            for (int h = 0; h < 4; h++) {
                                // Row temporary sum initialization
                                long temp = 0;
                                for (int z = 0; z < 4; z++) {
                                    // Selection of one pixel
                                    int pixelValue = srcDataArrays[k2][pos + (z - 1)
                                            * srcPixelStride + (h - 1) * srcScanlineStride
                                            + bandOffsets[k2]] & 0xffff;
                                    // Update of the temporary sum
                                    temp += (pixelValue * (long) dataHi[offsetX + z]);
                                }
                                // Vertical sum update
                                sum += ((temp + round) >> precisionBits)
                                        * (long) dataVi[offsetY + h];
                            }
                            // Interpolation
                            result = (int) ((sum + round) >> precisionBits);

                            if (result > USHORT_MAX_VALUE) {
                                result = USHORT_MAX_VALUE;
                            } else if (result < 0) {
                                result = 0;
                            }

                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (short) (result & 0xffff);
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
                        if (fracx == 1.0F) {
                            // Avoid overflow in the interpolation table
                            fracx = 0.999999F;
                        }
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                        if (fracy == 1.0F) {
                            // Avoid overflow in the interpolation table
                            fracy = 0.999999F;
                        }
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
                            dst_min_x, dst_max_x, 1, 2, 1, 2);
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

                        // integral fractional value calculation
                        final int xfrac = (int) (shift * fracx);
                        final int yfrac = (int) (shift * fracy);
                        // X and Y offset initialization
                        final int offsetX = 4 * xfrac;
                        final int offsetY = 4 * yfrac;

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;

                        // If roiAccessor is present, the y position on the roi image is calculated
                        final int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                final int[][] pixelKernel = new int[4][4];

                                int result = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride
                                                + bandOffsets[k2]] & 0xffff;
                                        int index = baseIndex - 1 + z + (h - 1)
                                                * (roiScanlineStride);
                                        if (index < roiDataLength) {
                                            // Update of the weight sum
                                            tmpROI += ((byte) (roiDataArray[index] & 0xff) != 0 ? 1
                                                    : 0);
                                        }
                                    }
                                }

                                // Control if the 16 pixel are outside the ROI
                                if (tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                                } else {
                                    for (int h = 0; h < 4; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        for (int z = 0; z < 4; z++) {
                                            // Update of the temporary sum
                                            tempSum += (pixelKernel[h][z] * (long) dataHi[offsetX
                                                    + z]);
                                        }
                                        // Vertical sum update
                                        sum += ((tempSum + round) >> precisionBits)
                                                * (long) dataVi[offsetY + h];
                                    }
                                    // Interpolation
                                    result = (int) ((sum + round) >> precisionBits);

                                    // Clamp
                                    if (result > USHORT_MAX_VALUE) {
                                        result = USHORT_MAX_VALUE;
                                    } else if (result < 0) {
                                        result = 0;
                                    }

                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (short) (result & 0xffff);
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                            if (fracx == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracx = 0.999999F;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracy = 0.999999F;
                            }
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
                            dst_min_x, dst_max_x, 1, 2, 1, 2);
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

                        // integral fractional value calculation
                        final int xfrac = (int) (shift * fracx);
                        final int yfrac = (int) (shift * fracy);
                        // X and Y offset initialization
                        final int offsetX = 4 * xfrac;
                        final int offsetY = 4 * yfrac;

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;

                        // PixelPositions
                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (!roiBounds.contains(x0, y0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                final int[][] pixelKernel = new int[4][4];

                                int result = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride] & 0xffff;
                                        tmpROI += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0) & 0xff;
                                    }
                                }

                                // Control if the 16 pixel are outside the ROI
                                if (tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                                } else {
                                    for (int h = 0; h < 4; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        for (int z = 0; z < 4; z++) {
                                            // Update of the temporary sum
                                            tempSum += (pixelKernel[h][z] * (long) dataHi[offsetX
                                                    + z]);
                                        }
                                        // Vertical sum update
                                        sum += ((tempSum + round) >> precisionBits)
                                                * (long) dataVi[offsetY + h];
                                    }
                                    // Interpolation
                                    result = (int) ((sum + round) >> precisionBits);

                                    // Clamp
                                    if (result > USHORT_MAX_VALUE) {
                                        result = USHORT_MAX_VALUE;
                                    } else if (result < 0) {
                                        result = 0;
                                    }

                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (short) (result & 0xffff);
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                            if (fracx == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracx = 0.999999F;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracy = 0.999999F;
                            }
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

                    // integral fractional value calculation
                    final int xfrac = (int) (shift * fracx);
                    final int yfrac = (int) (shift * fracy);
                    // X and Y offset initialization
                    final int offsetX = 4 * xfrac;
                    final int offsetY = 4 * yfrac;

                    final int posx = (s_ix - srcRectX) * srcPixelStride;
                    final int posy = (s_iy - srcRectY) * srcScanlineStride;

                    final int pos = posx + posy;

                    if ((s_ix >= src_rect_x1 + 1) && (s_ix < (src_rect_x2 - 2))
                            && (s_iy >= (src_rect_y1 + 1)) && (s_iy < (src_rect_y2 - 2))) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            long sum = 0;

                            final long[][] pixelKernel = new long[4][4];
                            int[][] weightArray = new int[4][4];
                            int[] weightArrayVertical = new int[4];

                            int result = 0;

                            int tmpND = 0;

                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            // and cycle for filling all the ROI index by shifting of 1 on the x axis
                            // and by 1 on the y axis.
                            for (int h = 0; h < 4; h++) {
                                for (int z = 0; z < 4; z++) {
                                    // Selection of one pixel
                                    pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                            * srcPixelStride + (h - 1) * srcScanlineStride] & 0xffff;
                                    if (!noData.contains((short) pixelKernel[h][z])) {
                                        tmpND++;
                                        weightArray[h][z] = 1;
                                    }
                                }
                            }

                            // Control if the 16 pixel are outside the ROI
                            if (tmpND == 0) {
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                            } else {
                                long[] sumArray = new long[4];

                                for (int h = 0; h < 4; h++) {
                                    // Row temporary sum initialization
                                    long tempSum = 0;
                                    long[] tempData = bicubicInpainting(pixelKernel[h],
                                            weightArray[h], null);
                                    for (int z = 0; z < 4; z++) {
                                        // Update of the temporary sum
                                        tempSum += (tempData[z] * (long) dataHi[offsetX + z]);
                                    }
                                    weightArrayVertical[h] = weightArray[h][0] + weightArray[h][1]
                                            + weightArray[h][2] + weightArray[h][3];
                                    sumArray[h] = ((tempSum + round) >> precisionBits);
                                }

                                long[] tempData = bicubicInpainting(sumArray, null,
                                        weightArrayVertical);

                                // Vertical sum update
                                for (int h = 0; h < 4; h++) {
                                    // Update of the temporary sum
                                    sum += tempData[h] * (long) dataVi[offsetY + h];
                                }

                                // Interpolation
                                result = (int) ((sum + round) >> precisionBits);

                                // Clamp
                                if (result > USHORT_MAX_VALUE) {
                                    result = USHORT_MAX_VALUE;
                                } else if (result < 0) {
                                    result = 0;
                                }

                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (short) (result & 0xffff);
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
                        if (fracx == 1.0F) {
                            // Avoid overflow in the interpolation table
                            fracx = 0.999999F;
                        }
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                        if (fracy == 1.0F) {
                            // Avoid overflow in the interpolation table
                            fracy = 0.999999F;
                        }
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
                            dst_min_x, dst_max_x, 1, 2, 1, 2);
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

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        // integral fractional value calculation
                        final int xfrac = (int) (shift * fracx);
                        final int yfrac = (int) (shift * fracy);
                        // X and Y offset initialization
                        final int offsetX = 4 * xfrac;
                        final int offsetY = 4 * yfrac;

                        final int pos = posx + posy;

                        if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                final long[][] pixelKernel = new long[4][4];
                                int[][] weightArray = new int[4][4];
                                int[] weightArrayVertical = new int[4];

                                int result = 0;

                                int tmpND = 0;
                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride] & 0xffff;
                                        int index = baseIndex - 1 + z + (h - 1)
                                                * (roiScanlineStride);
                                        if (index < roiDataLength) {
                                            // Update of the weight sum
                                            tmpROI += ((byte) (roiDataArray[index] & 0xff) != 0 ? 1
                                                    : 0);
                                        }

                                        if (!noData.contains((short) pixelKernel[h][z])) {
                                            tmpND++;
                                            weightArray[h][z] = 1;
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                if (tmpND == 0 || tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                                } else {
                                    long[] sumArray = new long[4];

                                    for (int h = 0; h < 4; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        long[] tempData = bicubicInpainting(pixelKernel[h],
                                                weightArray[h], null);
                                        for (int z = 0; z < 4; z++) {
                                            // Update of the temporary sum
                                            tempSum += (tempData[z] * (long) dataHi[offsetX + z]);
                                        }
                                        weightArrayVertical[h] = weightArray[h][0]
                                                + weightArray[h][1] + weightArray[h][2]
                                                + weightArray[h][3];
                                        sumArray[h] = ((tempSum + round) >> precisionBits);
                                    }

                                    long[] tempData = bicubicInpainting(sumArray, null,
                                            weightArrayVertical);

                                    // Vertical sum update
                                    for (int h = 0; h < 4; h++) {
                                        // Update of the temporary sum
                                        sum += tempData[h] * (long) dataVi[offsetY + h];
                                    }

                                    // Interpolation
                                    result = (int) ((sum + round) >> precisionBits);

                                    // Clamp
                                    if (result > USHORT_MAX_VALUE) {
                                        result = USHORT_MAX_VALUE;
                                    } else if (result < 0) {
                                        result = 0;
                                    }

                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (short) (result & 0xffff);
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                            if (fracx == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracx = 0.999999F;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracy = 0.999999F;
                            }
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
                            dst_min_x, dst_max_x, 1, 2, 1, 2);
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

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        // integral fractional value calculation
                        final int xfrac = (int) (shift * fracx);
                        final int yfrac = (int) (shift * fracy);
                        // X and Y offset initialization
                        final int offsetX = 4 * xfrac;
                        final int offsetY = 4 * yfrac;

                        final int pos = posx + posy;

                        if (!roiBounds.contains(x0, y0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                final long[][] pixelKernel = new long[4][4];
                                int[][] weightArray = new int[4][4];
                                int[] weightArrayVertical = new int[4];

                                int result = 0;

                                int tmpND = 0;
                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride] & 0xffff;

                                        tmpROI += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0) & 0xff;

                                        if (!noData.contains((short) pixelKernel[h][z])) {
                                            tmpND++;
                                            weightArray[h][z] = 1;
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                if (tmpND == 0 || tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                                } else {
                                    long[] sumArray = new long[4];

                                    for (int h = 0; h < 4; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        long[] tempData = bicubicInpainting(pixelKernel[h],
                                                weightArray[h], null);
                                        for (int z = 0; z < 4; z++) {
                                            // Update of the temporary sum
                                            tempSum += (tempData[z] * (long) dataHi[offsetX + z]);
                                        }
                                        weightArrayVertical[h] = weightArray[h][0]
                                                + weightArray[h][1] + weightArray[h][2]
                                                + weightArray[h][3];
                                        sumArray[h] = ((tempSum + round) >> precisionBits);
                                    }

                                    long[] tempData = bicubicInpainting(sumArray, null,
                                            weightArrayVertical);

                                    // Vertical sum update
                                    for (int h = 0; h < 4; h++) {
                                        // Update of the temporary sum
                                        sum += tempData[h] * (long) dataVi[offsetY + h];
                                    }

                                    // Interpolation
                                    result = (int) ((sum + round) >> precisionBits);

                                    // Clamp
                                    if (result > USHORT_MAX_VALUE) {
                                        result = USHORT_MAX_VALUE;
                                    } else if (result < 0) {
                                        result = 0;
                                    }

                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (short) (result & 0xffff);
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                            if (fracx == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracx = 0.999999F;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracy = 0.999999F;
                            }
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

    private void shortLoop(int dataType, RasterAccessor src, Rectangle destRect,
            int srcRectX, int srcRectY, RasterAccessor dst, RasterAccessor roi) {

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

                    // integral fractional value calculation
                    int xfrac = (int) (shift * fracx);
                    int yfrac = (int) (shift * fracy);
                    // X and Y offset initialization
                    int offsetX = 4 * xfrac;
                    int offsetY = 4 * yfrac;

                    int posx = (s_ix - srcRectX) * srcPixelStride;
                    int posy = (s_iy - srcRectY) * srcScanlineStride;

                    int pos = posx + posy;

                    if ((s_ix >= src_rect_x1 + 1) && (s_ix < (src_rect_x2 - 2))
                            && (s_iy >= (src_rect_y1 + 1)) && (s_iy < (src_rect_y2 - 2))) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            long sum = 0;

                            int result = 0;

                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            for (int h = 0; h < 4; h++) {
                                // Row temporary sum initialization
                                long temp = 0;
                                for (int z = 0; z < 4; z++) {
                                    // Selection of one pixel
                                    int pixelValue = srcDataArrays[k2][pos + (z - 1)
                                            * srcPixelStride + (h - 1) * srcScanlineStride
                                            + bandOffsets[k2]];
                                    // Update of the temporary sum
                                    temp += (pixelValue * (long) dataHi[offsetX + z]);
                                }
                                // Vertical sum update
                                sum += ((temp + round) >> precisionBits)
                                        * (long) dataVi[offsetY + h];
                            }
                            // Interpolation
                            result = (int) ((sum + round) >> precisionBits);

                            if (result > Short.MAX_VALUE) {
                                result = Short.MAX_VALUE;
                            } else if (result < Short.MIN_VALUE) {
                                result = Short.MIN_VALUE;
                            }

                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (short) result;
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
                        if (fracx == 1.0F) {
                            // Avoid overflow in the interpolation table
                            fracx = 0.999999F;
                        }
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                        if (fracy == 1.0F) {
                            // Avoid overflow in the interpolation table
                            fracy = 0.999999F;
                        }
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
                            dst_min_x, dst_max_x, 1, 2, 1, 2);
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

                        // integral fractional value calculation
                        final int xfrac = (int) (shift * fracx);
                        final int yfrac = (int) (shift * fracy);
                        // X and Y offset initialization
                        final int offsetX = 4 * xfrac;
                        final int offsetY = 4 * yfrac;

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;

                        // If roiAccessor is present, the y position on the roi image is calculated
                        final int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                final int[][] pixelKernel = new int[4][4];

                                int result = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride
                                                + bandOffsets[k2]];
                                        int index = baseIndex - 1 + z + (h - 1)
                                                * (roiScanlineStride);
                                        if (index < roiDataLength) {
                                            // Update of the weight sum
                                            tmpROI += ((byte) (roiDataArray[index] & 0xff) != 0 ? 1
                                                    : 0);
                                        }
                                    }
                                }

                                // Control if the 16 pixel are outside the ROI
                                if (tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                                } else {
                                    for (int h = 0; h < 4; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        for (int z = 0; z < 4; z++) {
                                            // Update of the temporary sum
                                            tempSum += (pixelKernel[h][z] * (long) dataHi[offsetX
                                                    + z]);
                                        }
                                        // Vertical sum update
                                        sum += ((tempSum + round) >> precisionBits)
                                                * (long) dataVi[offsetY + h];
                                    }
                                    // Interpolation
                                    result = (int) ((sum + round) >> precisionBits);

                                    // Clamp
                                    if (result > Short.MAX_VALUE) {
                                        result = Short.MAX_VALUE;
                                    } else if (result < Short.MIN_VALUE) {
                                        result = Short.MIN_VALUE;
                                    }

                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (short) result;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                            if (fracx == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracx = 0.999999F;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracy = 0.999999F;
                            }
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
                            dst_min_x, dst_max_x, 1, 2, 1, 2);
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

                        // integral fractional value calculation
                        final int xfrac = (int) (shift * fracx);
                        final int yfrac = (int) (shift * fracy);
                        // X and Y offset initialization
                        final int offsetX = 4 * xfrac;
                        final int offsetY = 4 * yfrac;

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;

                        // PixelPositions
                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (!roiBounds.contains(x0, y0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                final int[][] pixelKernel = new int[4][4];

                                int result = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride];
                                        tmpROI += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0) & 0xff;
                                    }
                                }

                                // Control if the 16 pixel are outside the ROI
                                if (tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                                } else {
                                    for (int h = 0; h < 4; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        for (int z = 0; z < 4; z++) {
                                            // Update of the temporary sum
                                            tempSum += (pixelKernel[h][z] * (long) dataHi[offsetX
                                                    + z]);
                                        }
                                        // Vertical sum update
                                        sum += ((tempSum + round) >> precisionBits)
                                                * (long) dataVi[offsetY + h];
                                    }
                                    // Interpolation
                                    result = (int) ((sum + round) >> precisionBits);

                                    // Clamp
                                    if (result > Short.MAX_VALUE) {
                                        result = Short.MAX_VALUE;
                                    } else if (result < Short.MIN_VALUE) {
                                        result = Short.MIN_VALUE;
                                    }

                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (short) result;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                            if (fracx == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracx = 0.999999F;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracy = 0.999999F;
                            }
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

                    // integral fractional value calculation
                    final int xfrac = (int) (shift * fracx);
                    final int yfrac = (int) (shift * fracy);
                    // X and Y offset initialization
                    final int offsetX = 4 * xfrac;
                    final int offsetY = 4 * yfrac;

                    final int posx = (s_ix - srcRectX) * srcPixelStride;
                    final int posy = (s_iy - srcRectY) * srcScanlineStride;

                    final int pos = posx + posy;

                    if ((s_ix >= src_rect_x1 + 1) && (s_ix < (src_rect_x2 - 2))
                            && (s_iy >= (src_rect_y1 + 1)) && (s_iy < (src_rect_y2 - 2))) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            long sum = 0;

                            final long[][] pixelKernel = new long[4][4];
                            int[][] weightArray = new int[4][4];
                            int[] weightArrayVertical = new int[4];

                            int result = 0;

                            int tmpND = 0;

                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            // and cycle for filling all the ROI index by shifting of 1 on the x axis
                            // and by 1 on the y axis.
                            for (int h = 0; h < 4; h++) {
                                for (int z = 0; z < 4; z++) {
                                    // Selection of one pixel
                                    pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                            * srcPixelStride + (h - 1) * srcScanlineStride];
                                    if (!noData.contains((short) pixelKernel[h][z])) {
                                        tmpND++;
                                        weightArray[h][z] = 1;
                                    }
                                }
                            }

                            // Control if the 16 pixel are outside the ROI
                            if (tmpND == 0) {
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                            } else {
                                long[] sumArray = new long[4];

                                for (int h = 0; h < 4; h++) {
                                    // Row temporary sum initialization
                                    long tempSum = 0;
                                    long[] tempData = bicubicInpainting(pixelKernel[h],
                                            weightArray[h], null);
                                    for (int z = 0; z < 4; z++) {
                                        // Update of the temporary sum
                                        tempSum += (tempData[z] * (long) dataHi[offsetX + z]);
                                    }
                                    weightArrayVertical[h] = weightArray[h][0] + weightArray[h][1]
                                            + weightArray[h][2] + weightArray[h][3];
                                    sumArray[h] = ((tempSum + round) >> precisionBits);
                                }

                                long[] tempData = bicubicInpainting(sumArray, null,
                                        weightArrayVertical);

                                // Vertical sum update
                                for (int h = 0; h < 4; h++) {
                                    // Update of the temporary sum
                                    sum += tempData[h] * (long) dataVi[offsetY + h];
                                }

                                // Interpolation
                                result = (int) ((sum + round) >> precisionBits);

                                // Clamp
                                if (result > Short.MAX_VALUE) {
                                    result = Short.MAX_VALUE;
                                } else if (result < Short.MIN_VALUE) {
                                    result = Short.MIN_VALUE;
                                }

                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (short) result;
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
                        if (fracx == 1.0F) {
                            // Avoid overflow in the interpolation table
                            fracx = 0.999999F;
                        }
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                        if (fracy == 1.0F) {
                            // Avoid overflow in the interpolation table
                            fracy = 0.999999F;
                        }
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
                            dst_min_x, dst_max_x, 1, 2, 1, 2);
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

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        // integral fractional value calculation
                        final int xfrac = (int) (shift * fracx);
                        final int yfrac = (int) (shift * fracy);
                        // X and Y offset initialization
                        final int offsetX = 4 * xfrac;
                        final int offsetY = 4 * yfrac;

                        final int pos = posx + posy;

                        if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                final long[][] pixelKernel = new long[4][4];
                                int[][] weightArray = new int[4][4];
                                int[] weightArrayVertical = new int[4];

                                int result = 0;

                                int tmpND = 0;
                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride];
                                        int index = baseIndex - 1 + z + (h - 1)
                                                * (roiScanlineStride);
                                        if (index < roiDataLength) {
                                            // Update of the weight sum
                                            tmpROI += ((byte) (roiDataArray[index] & 0xff) != 0 ? 1
                                                    : 0);
                                        }

                                        if (!noData.contains((short) pixelKernel[h][z])) {
                                            tmpND++;
                                            weightArray[h][z] = 1;
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                if (tmpND == 0 || tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                                } else {
                                    long[] sumArray = new long[4];

                                    for (int h = 0; h < 4; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        long[] tempData = bicubicInpainting(pixelKernel[h],
                                                weightArray[h], null);
                                        for (int z = 0; z < 4; z++) {
                                            // Update of the temporary sum
                                            tempSum += (tempData[z] * (long) dataHi[offsetX + z]);
                                        }
                                        weightArrayVertical[h] = weightArray[h][0]
                                                + weightArray[h][1] + weightArray[h][2]
                                                + weightArray[h][3];
                                        sumArray[h] = ((tempSum + round) >> precisionBits);
                                    }

                                    long[] tempData = bicubicInpainting(sumArray, null,
                                            weightArrayVertical);

                                    // Vertical sum update
                                    for (int h = 0; h < 4; h++) {
                                        // Update of the temporary sum
                                        sum += tempData[h] * (long) dataVi[offsetY + h];
                                    }

                                    // Interpolation
                                    result = (int) ((sum + round) >> precisionBits);

                                    // Clamp
                                    if (result > Short.MAX_VALUE) {
                                        result = Short.MAX_VALUE;
                                    } else if (result < Short.MIN_VALUE) {
                                        result = Short.MIN_VALUE;
                                    }

                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (short) result;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                            if (fracx == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracx = 0.999999F;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracy = 0.999999F;
                            }
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
                            dst_min_x, dst_max_x, 1, 2, 1, 2);
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

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        // integral fractional value calculation
                        final int xfrac = (int) (shift * fracx);
                        final int yfrac = (int) (shift * fracy);
                        // X and Y offset initialization
                        final int offsetX = 4 * xfrac;
                        final int offsetY = 4 * yfrac;

                        final int pos = posx + posy;

                        if (!roiBounds.contains(x0, y0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                final long[][] pixelKernel = new long[4][4];
                                int[][] weightArray = new int[4][4];
                                int[] weightArrayVertical = new int[4];

                                int result = 0;

                                int tmpND = 0;
                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride];

                                        tmpROI += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0) & 0xff;

                                        if (!noData.contains((short) pixelKernel[h][z])) {
                                            tmpND++;
                                            weightArray[h][z] = 1;
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                if (tmpND == 0 || tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                                } else {
                                    long[] sumArray = new long[4];

                                    for (int h = 0; h < 4; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        long[] tempData = bicubicInpainting(pixelKernel[h],
                                                weightArray[h], null);
                                        for (int z = 0; z < 4; z++) {
                                            // Update of the temporary sum
                                            tempSum += (tempData[z] * (long) dataHi[offsetX + z]);
                                        }
                                        weightArrayVertical[h] = weightArray[h][0]
                                                + weightArray[h][1] + weightArray[h][2]
                                                + weightArray[h][3];
                                        sumArray[h] = ((tempSum + round) >> precisionBits);
                                    }

                                    long[] tempData = bicubicInpainting(sumArray, null,
                                            weightArrayVertical);

                                    // Vertical sum update
                                    for (int h = 0; h < 4; h++) {
                                        // Update of the temporary sum
                                        sum += tempData[h] * (long) dataVi[offsetY + h];
                                    }

                                    // Interpolation
                                    result = (int) ((sum + round) >> precisionBits);

                                    // Clamp
                                    if (result > Short.MAX_VALUE) {
                                        result = Short.MAX_VALUE;
                                    } else if (result < Short.MIN_VALUE) {
                                        result = Short.MIN_VALUE;
                                    }

                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (short) result;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                            if (fracx == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracx = 0.999999F;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracy = 0.999999F;
                            }
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

    private void intLoop(int dataType, RasterAccessor src, Rectangle destRect,
            int srcRectX, int srcRectY, RasterAccessor dst, RasterAccessor roi) {

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

                    // integral fractional value calculation
                    int xfrac = (int) (shift * fracx);
                    int yfrac = (int) (shift * fracy);
                    // X and Y offset initialization
                    int offsetX = 4 * xfrac;
                    int offsetY = 4 * yfrac;

                    int posx = (s_ix - srcRectX) * srcPixelStride;
                    int posy = (s_iy - srcRectY) * srcScanlineStride;

                    int pos = posx + posy;

                    if ((s_ix >= src_rect_x1 + 1) && (s_ix < (src_rect_x2 - 2))
                            && (s_iy >= (src_rect_y1 + 1)) && (s_iy < (src_rect_y2 - 2))) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            long sum = 0;

                            int result = 0;

                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            for (int h = 0; h < 4; h++) {
                                // Row temporary sum initialization
                                long temp = 0;
                                for (int z = 0; z < 4; z++) {
                                    // Selection of one pixel
                                    int pixelValue = srcDataArrays[k2][pos + (z - 1)
                                            * srcPixelStride + (h - 1) * srcScanlineStride
                                            + bandOffsets[k2]];
                                    // Update of the temporary sum
                                    temp += (pixelValue * (long) dataHi[offsetX + z]);
                                }
                                // Vertical sum update
                                sum += ((temp + round) >> precisionBits)
                                        * (long) dataVi[offsetY + h];
                            }
                            // Interpolation
                            result = (int) ((sum + round) >> precisionBits);
                            
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = result;
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
                        if (fracx == 1.0F) {
                            // Avoid overflow in the interpolation table
                            fracx = 0.999999F;
                        }
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                        if (fracy == 1.0F) {
                            // Avoid overflow in the interpolation table
                            fracy = 0.999999F;
                        }
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
                            dst_min_x, dst_max_x, 1, 2, 1, 2);
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

                        // integral fractional value calculation
                        final int xfrac = (int) (shift * fracx);
                        final int yfrac = (int) (shift * fracy);
                        // X and Y offset initialization
                        final int offsetX = 4 * xfrac;
                        final int offsetY = 4 * yfrac;

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;

                        // If roiAccessor is present, the y position on the roi image is calculated
                        final int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                final int[][] pixelKernel = new int[4][4];

                                int result = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride
                                                + bandOffsets[k2]];
                                        int index = baseIndex - 1 + z + (h - 1)
                                                * (roiScanlineStride);
                                        if (index < roiDataLength) {
                                            // Update of the weight sum
                                            tmpROI += ((byte) (roiDataArray[index] & 0xff) != 0 ? 1
                                                    : 0);
                                        }
                                    }
                                }

                                // Control if the 16 pixel are outside the ROI
                                if (tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                                } else {
                                    for (int h = 0; h < 4; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        for (int z = 0; z < 4; z++) {
                                            // Update of the temporary sum
                                            tempSum += (pixelKernel[h][z] * (long) dataHi[offsetX
                                                    + z]);
                                        }
                                        // Vertical sum update
                                        sum += ((tempSum + round) >> precisionBits)
                                                * (long) dataVi[offsetY + h];
                                    }
                                    // Interpolation
                                    result = (int) ((sum + round) >> precisionBits);
                                   
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = result;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                            if (fracx == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracx = 0.999999F;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracy = 0.999999F;
                            }
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
                            dst_min_x, dst_max_x, 1, 2, 1, 2);
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

                        // integral fractional value calculation
                        final int xfrac = (int) (shift * fracx);
                        final int yfrac = (int) (shift * fracy);
                        // X and Y offset initialization
                        final int offsetX = 4 * xfrac;
                        final int offsetY = 4 * yfrac;

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;

                        // PixelPositions
                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (!roiBounds.contains(x0, y0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                final int[][] pixelKernel = new int[4][4];

                                int result = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride];
                                        tmpROI += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0) & 0xff;
                                    }
                                }

                                // Control if the 16 pixel are outside the ROI
                                if (tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                                } else {
                                    for (int h = 0; h < 4; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        for (int z = 0; z < 4; z++) {
                                            // Update of the temporary sum
                                            tempSum += (pixelKernel[h][z] * (long) dataHi[offsetX
                                                    + z]);
                                        }
                                        // Vertical sum update
                                        sum += ((tempSum + round) >> precisionBits)
                                                * (long) dataVi[offsetY + h];
                                    }
                                    // Interpolation
                                    result = (int) ((sum + round) >> precisionBits);
                                    
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = result;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                            if (fracx == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracx = 0.999999F;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracy = 0.999999F;
                            }
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

                    // integral fractional value calculation
                    final int xfrac = (int) (shift * fracx);
                    final int yfrac = (int) (shift * fracy);
                    // X and Y offset initialization
                    final int offsetX = 4 * xfrac;
                    final int offsetY = 4 * yfrac;

                    final int posx = (s_ix - srcRectX) * srcPixelStride;
                    final int posy = (s_iy - srcRectY) * srcScanlineStride;

                    final int pos = posx + posy;

                    if ((s_ix >= src_rect_x1 + 1) && (s_ix < (src_rect_x2 - 2))
                            && (s_iy >= (src_rect_y1 + 1)) && (s_iy < (src_rect_y2 - 2))) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            long sum = 0;

                            final long[][] pixelKernel = new long[4][4];
                            int[][] weightArray = new int[4][4];
                            int[] weightArrayVertical = new int[4];

                            int result = 0;

                            int tmpND = 0;

                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            // and cycle for filling all the ROI index by shifting of 1 on the x axis
                            // and by 1 on the y axis.
                            for (int h = 0; h < 4; h++) {
                                for (int z = 0; z < 4; z++) {
                                    // Selection of one pixel
                                    pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                            * srcPixelStride + (h - 1) * srcScanlineStride];
                                    if (!noData.contains((int) pixelKernel[h][z])) {
                                        tmpND++;
                                        weightArray[h][z] = 1;
                                    }
                                }
                            }

                            // Control if the 16 pixel are outside the ROI
                            if (tmpND == 0) {
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                            } else {
                                long[] sumArray = new long[4];

                                for (int h = 0; h < 4; h++) {
                                    // Row temporary sum initialization
                                    long tempSum = 0;
                                    long[] tempData = bicubicInpainting(pixelKernel[h],
                                            weightArray[h], null);
                                    for (int z = 0; z < 4; z++) {
                                        // Update of the temporary sum
                                        tempSum += (tempData[z] * (long) dataHi[offsetX + z]);
                                    }
                                    weightArrayVertical[h] = weightArray[h][0] + weightArray[h][1]
                                            + weightArray[h][2] + weightArray[h][3];
                                    sumArray[h] = ((tempSum + round) >> precisionBits);
                                }

                                long[] tempData = bicubicInpainting(sumArray, null,
                                        weightArrayVertical);

                                // Vertical sum update
                                for (int h = 0; h < 4; h++) {
                                    // Update of the temporary sum
                                    sum += tempData[h] * (long) dataVi[offsetY + h];
                                }

                                // Interpolation
                                result = (int) ((sum + round) >> precisionBits);
                                
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = result;
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
                        if (fracx == 1.0F) {
                            // Avoid overflow in the interpolation table
                            fracx = 0.999999F;
                        }
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                        if (fracy == 1.0F) {
                            // Avoid overflow in the interpolation table
                            fracy = 0.999999F;
                        }
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
                            dst_min_x, dst_max_x, 1, 2, 1, 2);
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

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        // integral fractional value calculation
                        final int xfrac = (int) (shift * fracx);
                        final int yfrac = (int) (shift * fracy);
                        // X and Y offset initialization
                        final int offsetX = 4 * xfrac;
                        final int offsetY = 4 * yfrac;

                        final int pos = posx + posy;

                        if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                final long[][] pixelKernel = new long[4][4];
                                int[][] weightArray = new int[4][4];
                                int[] weightArrayVertical = new int[4];

                                int result = 0;

                                int tmpND = 0;
                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride];
                                        int index = baseIndex - 1 + z + (h - 1)
                                                * (roiScanlineStride);
                                        if (index < roiDataLength) {
                                            // Update of the weight sum
                                            tmpROI += ((byte) (roiDataArray[index] & 0xff) != 0 ? 1
                                                    : 0);
                                        }

                                        if (!noData.contains((int) pixelKernel[h][z])) {
                                            tmpND++;
                                            weightArray[h][z] = 1;
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                if (tmpND == 0 || tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                                } else {
                                    long[] sumArray = new long[4];

                                    for (int h = 0; h < 4; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        long[] tempData = bicubicInpainting(pixelKernel[h],
                                                weightArray[h], null);
                                        for (int z = 0; z < 4; z++) {
                                            // Update of the temporary sum
                                            tempSum += (tempData[z] * (long) dataHi[offsetX + z]);
                                        }
                                        weightArrayVertical[h] = weightArray[h][0]
                                                + weightArray[h][1] + weightArray[h][2]
                                                + weightArray[h][3];
                                        sumArray[h] = ((tempSum + round) >> precisionBits);
                                    }

                                    long[] tempData = bicubicInpainting(sumArray, null,
                                            weightArrayVertical);

                                    // Vertical sum update
                                    for (int h = 0; h < 4; h++) {
                                        // Update of the temporary sum
                                        sum += tempData[h] * (long) dataVi[offsetY + h];
                                    }

                                    // Interpolation
                                    result = (int) ((sum + round) >> precisionBits);
                                    
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = result;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                            if (fracx == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracx = 0.999999F;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracy = 0.999999F;
                            }
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
                            dst_min_x, dst_max_x, 1, 2, 1, 2);
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

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        // integral fractional value calculation
                        final int xfrac = (int) (shift * fracx);
                        final int yfrac = (int) (shift * fracy);
                        // X and Y offset initialization
                        final int offsetX = 4 * xfrac;
                        final int offsetY = 4 * yfrac;

                        final int pos = posx + posy;

                        if (!roiBounds.contains(x0, y0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                final long[][] pixelKernel = new long[4][4];
                                int[][] weightArray = new int[4][4];
                                int[] weightArrayVertical = new int[4];

                                int result = 0;

                                int tmpND = 0;
                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride];

                                        tmpROI += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0) & 0xff;

                                        if (!noData.contains((int) pixelKernel[h][z])) {
                                            tmpND++;
                                            weightArray[h][z] = 1;
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                if (tmpND == 0 || tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                                } else {
                                    long[] sumArray = new long[4];

                                    for (int h = 0; h < 4; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        long[] tempData = bicubicInpainting(pixelKernel[h],
                                                weightArray[h], null);
                                        for (int z = 0; z < 4; z++) {
                                            // Update of the temporary sum
                                            tempSum += (tempData[z] * (long) dataHi[offsetX + z]);
                                        }
                                        weightArrayVertical[h] = weightArray[h][0]
                                                + weightArray[h][1] + weightArray[h][2]
                                                + weightArray[h][3];
                                        sumArray[h] = ((tempSum + round) >> precisionBits);
                                    }

                                    long[] tempData = bicubicInpainting(sumArray, null,
                                            weightArrayVertical);

                                    // Vertical sum update
                                    for (int h = 0; h < 4; h++) {
                                        // Update of the temporary sum
                                        sum += tempData[h] * (long) dataVi[offsetY + h];
                                    }

                                    // Interpolation
                                    result = (int) ((sum + round) >> precisionBits);
                                    
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = result;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                            if (fracx == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracx = 0.999999F;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracy = 0.999999F;
                            }
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

    private void floatLoop(int dataType, RasterAccessor src, Rectangle destRect,
            int srcRectX, int srcRectY, RasterAccessor dst, RasterAccessor roi) {

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

                    // integral fractional value calculation
                    int xfrac = (int) (shift * fracx);
                    int yfrac = (int) (shift * fracy);
                    // X and Y offset initialization
                    int offsetX = 4 * xfrac;
                    int offsetY = 4 * yfrac;

                    int posx = (s_ix - srcRectX) * srcPixelStride;
                    int posy = (s_iy - srcRectY) * srcScanlineStride;

                    int pos = posx + posy;

                    if ((s_ix >= src_rect_x1 + 1) && (s_ix < (src_rect_x2 - 2))
                            && (s_iy >= (src_rect_y1 + 1)) && (s_iy < (src_rect_y2 - 2))) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            float sum = 0;

                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            for (int h = 0; h < 4; h++) {
                                // Row temporary sum initialization
                                float temp = 0;
                                for (int z = 0; z < 4; z++) {
                                    // Selection of one pixel
                                    float pixelValue = srcDataArrays[k2][pos + (z - 1)
                                            * srcPixelStride + (h - 1) * srcScanlineStride
                                            + bandOffsets[k2]];
                                    // Update of the temporary sum
                                    temp += (pixelValue * dataHf[offsetX + z]);
                                }
                                // Vertical sum update
                                sum += temp * dataVf[offsetY + h];
                            }
                            
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = sum;
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
                        if (fracx == 1.0F) {
                            // Avoid overflow in the interpolation table
                            fracx = 0.999999F;
                        }
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                        if (fracy == 1.0F) {
                            // Avoid overflow in the interpolation table
                            fracy = 0.999999F;
                        }
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
                            dst_min_x, dst_max_x, 1, 2, 1, 2);
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

                        // integral fractional value calculation
                        final int xfrac = (int) (shift * fracx);
                        final int yfrac = (int) (shift * fracy);
                        // X and Y offset initialization
                        final int offsetX = 4 * xfrac;
                        final int offsetY = 4 * yfrac;

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;

                        // If roiAccessor is present, the y position on the roi image is calculated
                        final int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                float sum = 0;

                                final float[][] pixelKernel = new float[4][4];

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride
                                                + bandOffsets[k2]];
                                        int index = baseIndex - 1 + z + (h - 1)
                                                * (roiScanlineStride);
                                        if (index < roiDataLength) {
                                            // Update of the weight sum
                                            tmpROI += ((byte) (roiDataArray[index] & 0xff) != 0 ? 1
                                                    : 0);
                                        }
                                    }
                                }

                                // Control if the 16 pixel are outside the ROI
                                if (tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                                } else {
                                    for (int h = 0; h < 4; h++) {
                                        // Row temporary sum initialization
                                        float tempSum = 0;
                                        for (int z = 0; z < 4; z++) {
                                            // Update of the temporary sum
                                            tempSum += (pixelKernel[h][z] * dataHf[offsetX + z]);
                                        }
                                        // Vertical sum update
                                        sum += tempSum * dataVf[offsetY + h];
                                    }
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = sum;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                            if (fracx == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracx = 0.999999F;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracy = 0.999999F;
                            }
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
                            dst_min_x, dst_max_x, 1, 2, 1, 2);
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

                        // integral fractional value calculation
                        final int xfrac = (int) (shift * fracx);
                        final int yfrac = (int) (shift * fracy);
                        // X and Y offset initialization
                        final int offsetX = 4 * xfrac;
                        final int offsetY = 4 * yfrac;

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;

                        // PixelPositions
                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (!roiBounds.contains(x0, y0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                float sum = 0;

                                final float[][] pixelKernel = new float[4][4];

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride];
                                        tmpROI += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0) & 0xff;
                                    }
                                }

                                // Control if the 16 pixel are outside the ROI
                                if (tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                                } else {
                                    for (int h = 0; h < 4; h++) {
                                        // Row temporary sum initialization
                                        float tempSum = 0;
                                        for (int z = 0; z < 4; z++) {
                                            // Update of the temporary sum
                                            tempSum += (pixelKernel[h][z] * dataHf[offsetX
                                                    + z]);
                                        }
                                        // Vertical sum update
                                        sum += tempSum * dataVf[offsetY + h];
                                    }
                                    
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = sum;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                            if (fracx == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracx = 0.999999F;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracy = 0.999999F;
                            }
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

                    // integral fractional value calculation
                    final int xfrac = (int) (shift * fracx);
                    final int yfrac = (int) (shift * fracy);
                    // X and Y offset initialization
                    final int offsetX = 4 * xfrac;
                    final int offsetY = 4 * yfrac;

                    final int posx = (s_ix - srcRectX) * srcPixelStride;
                    final int posy = (s_iy - srcRectY) * srcScanlineStride;

                    final int pos = posx + posy;

                    if ((s_ix >= src_rect_x1 + 1) && (s_ix < (src_rect_x2 - 2))
                            && (s_iy >= (src_rect_y1 + 1)) && (s_iy < (src_rect_y2 - 2))) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            double sum = 0;

                            final double[][] pixelKernel = new double[4][4];
                            int[][] weightArray = new int[4][4];
                            int[] weightArrayVertical = new int[4];
                            int tmpND = 0;

                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            // and cycle for filling all the ROI index by shifting of 1 on the x axis
                            // and by 1 on the y axis.
                            for (int h = 0; h < 4; h++) {
                                for (int z = 0; z < 4; z++) {
                                    // Selection of one pixel
                                    float value = srcDataArrays[k2][pos + (z - 1)
                                                                    * srcPixelStride + (h - 1) * srcScanlineStride];
                                    pixelKernel[h][z] = value;
                                    
                                    if (!noData.contains(value) && !(isNotPointRange && Float.isNaN(value))) {
                                        tmpND++;
                                        weightArray[h][z] = 1;
                                    }
                                }
                            }

                            // Control if the 16 pixel are outside the ROI
                            if (tmpND == 0) {
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                            } else {
                                double[] sumArray = new double[4];

                                for (int h = 0; h < 4; h++) {
                                    // Row temporary sum initialization
                                    double tempSum = 0;
                                    double[] tempData = bicubicInpaintingDouble(pixelKernel[h],
                                            weightArray[h], null);
                                    for (int z = 0; z < 4; z++) {
                                        // Update of the temporary sum
                                        tempSum += (tempData[z] * dataHf[offsetX + z]);
                                    }
                                    weightArrayVertical[h] = weightArray[h][0] + weightArray[h][1]
                                            + weightArray[h][2] + weightArray[h][3];
                                    sumArray[h] = tempSum;
                                }

                                double[] tempData = bicubicInpaintingDouble(sumArray, null,
                                        weightArrayVertical);

                                // Vertical sum update
                                for (int h = 0; h < 4; h++) {
                                    // Update of the temporary sum
                                    sum += tempData[h] * dataVf[offsetY + h];
                                }
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (float) sum;
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
                        if (fracx == 1.0F) {
                            // Avoid overflow in the interpolation table
                            fracx = 0.999999F;
                        }
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                        if (fracy == 1.0F) {
                            // Avoid overflow in the interpolation table
                            fracy = 0.999999F;
                        }
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
                            dst_min_x, dst_max_x, 1, 2, 1, 2);
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

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        // integral fractional value calculation
                        final int xfrac = (int) (shift * fracx);
                        final int yfrac = (int) (shift * fracy);
                        // X and Y offset initialization
                        final int offsetX = 4 * xfrac;
                        final int offsetY = 4 * yfrac;

                        final int pos = posx + posy;

                        if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                double sum = 0;

                                final double[][] pixelKernel = new double[4][4];
                                int[][] weightArray = new int[4][4];
                                int[] weightArrayVertical = new int[4];

                                int tmpND = 0;
                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        float value = srcDataArrays[k2][pos + (z - 1)
                                                                        * srcPixelStride + (h - 1) * srcScanlineStride];
                                        pixelKernel[h][z] = value;
                                        int index = baseIndex - 1 + z + (h - 1)
                                                * (roiScanlineStride);
                                        if (index < roiDataLength) {
                                            // Update of the weight sum
                                            tmpROI += ((byte) (roiDataArray[index] & 0xff) != 0 ? 1
                                                    : 0);
                                        }
                                        
                                        if (!noData.contains(value)&& !(isNotPointRange && Float.isNaN(value))) {
                                            tmpND++;
                                            weightArray[h][z] = 1;
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                if (tmpND == 0 || tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                                } else {
                                    double[] sumArray = new double[4];

                                    for (int h = 0; h < 4; h++) {
                                        // Row temporary sum initialization
                                        double tempSum = 0;
                                        double[] tempData = bicubicInpaintingDouble(pixelKernel[h],
                                                weightArray[h], null);
                                        for (int z = 0; z < 4; z++) {
                                            // Update of the temporary sum
                                            tempSum += (tempData[z] * dataHf[offsetX + z]);
                                        }
                                        weightArrayVertical[h] = weightArray[h][0]
                                                + weightArray[h][1] + weightArray[h][2]
                                                + weightArray[h][3];
                                        sumArray[h] = tempSum;
                                    }

                                    double[] tempData = bicubicInpaintingDouble(sumArray, null,
                                            weightArrayVertical);

                                    // Vertical sum update
                                    for (int h = 0; h < 4; h++) {
                                        // Update of the temporary sum
                                        sum += tempData[h] * dataVf[offsetY + h];
                                    }

                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (float) sum;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                            if (fracx == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracx = 0.999999F;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracy = 0.999999F;
                            }
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
                            dst_min_x, dst_max_x, 1, 2, 1, 2);
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

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        // integral fractional value calculation
                        final int xfrac = (int) (shift * fracx);
                        final int yfrac = (int) (shift * fracy);
                        // X and Y offset initialization
                        final int offsetX = 4 * xfrac;
                        final int offsetY = 4 * yfrac;

                        final int pos = posx + posy;

                        if (!roiBounds.contains(x0, y0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                double sum = 0;

                                final double[][] pixelKernel = new double[4][4];
                                int[][] weightArray = new int[4][4];
                                int[] weightArrayVertical = new int[4];
                                
                                int tmpND = 0;
                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        float value = srcDataArrays[k2][pos + (z - 1)
                                                                        * srcPixelStride + (h - 1) * srcScanlineStride];
                                        pixelKernel[h][z] = value;

                                        tmpROI += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0) & 0xff;

                                        if (!noData.contains(value)&& !(isNotPointRange && Float.isNaN(value))) {
                                            tmpND++;
                                            weightArray[h][z] = 1;
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                // Control if the 16 pixel are outside the ROI
                                if (tmpND == 0 || tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                                } else {
                                    double[] sumArray = new double[4];

                                    for (int h = 0; h < 4; h++) {
                                        // Row temporary sum initialization
                                        double tempSum = 0;
                                        double[] tempData = bicubicInpaintingDouble(pixelKernel[h],
                                                weightArray[h], null);
                                        for (int z = 0; z < 4; z++) {
                                            // Update of the temporary sum
                                            tempSum += (tempData[z] * dataHf[offsetX + z]);
                                        }
                                        weightArrayVertical[h] = weightArray[h][0]
                                                + weightArray[h][1] + weightArray[h][2]
                                                + weightArray[h][3];
                                        sumArray[h] = tempSum;
                                    }

                                    double[] tempData = bicubicInpaintingDouble(sumArray, null,
                                            weightArrayVertical);

                                    // Vertical sum update
                                    for (int h = 0; h < 4; h++) {
                                        // Update of the temporary sum
                                        sum += tempData[h] * dataVf[offsetY + h];
                                    }

                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (float) sum;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                            if (fracx == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracx = 0.999999F;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracy = 0.999999F;
                            }
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

    private void doubleLoop(int dataType, RasterAccessor src, Rectangle destRect,
            int srcRectX, int srcRectY, RasterAccessor dst, RasterAccessor roi) {

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
                double s_x =  src_pt.getX();
                double s_y =  src_pt.getY();

                s_x -= 0.5;
                s_y -= 0.5;

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix * 1.0d;
                fracy = s_y - s_iy * 1.0d;

                for (int x = dst_min_x; x < dst_max_x; x++) {

                    // integral fractional value calculation
                    int xfrac = (int) (shift * fracx);
                    int yfrac = (int) (shift * fracy);
                    // X and Y offset initialization
                    int offsetX = 4 * xfrac;
                    int offsetY = 4 * yfrac;

                    int posx = (s_ix - srcRectX) * srcPixelStride;
                    int posy = (s_iy - srcRectY) * srcScanlineStride;

                    int pos = posx + posy;

                    if ((s_ix >= src_rect_x1 + 1) && (s_ix < (src_rect_x2 - 2))
                            && (s_iy >= (src_rect_y1 + 1)) && (s_iy < (src_rect_y2 - 2))) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            double sum = 0;

                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            for (int h = 0; h < 4; h++) {
                                // Row temporary sum initialization
                                double temp = 0;
                                for (int z = 0; z < 4; z++) {
                                    // Selection of one pixel
                                    double pixelValue = srcDataArrays[k2][pos + (z - 1)
                                            * srcPixelStride + (h - 1) * srcScanlineStride
                                            + bandOffsets[k2]];
                                    // Update of the temporary sum
                                    temp += (pixelValue * dataHd[offsetX + z]);
                                }
                                // Vertical sum update
                                sum += temp * dataVd[offsetY + h];
                            }
                            
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = sum;
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
                        if (fracx == 1.0F) {
                            // Avoid overflow in the interpolation table
                            fracx = 0.999999F;
                        }
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                        if (fracy == 1.0F) {
                            // Avoid overflow in the interpolation table
                            fracy = 0.999999F;
                        }
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
                    double s_x =  src_pt.getX();
                    double s_y =  src_pt.getY();

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
                            dst_min_x, dst_max_x, 1, 2, 1, 2);
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

                        // integral fractional value calculation
                        final int xfrac = (int) (shift * fracx);
                        final int yfrac = (int) (shift * fracy);
                        // X and Y offset initialization
                        final int offsetX = 4 * xfrac;
                        final int offsetY = 4 * yfrac;

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;

                        // If roiAccessor is present, the y position on the roi image is calculated
                        final int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                double sum = 0;

                                final double[][] pixelKernel = new double[4][4];

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride
                                                + bandOffsets[k2]];
                                        int index = baseIndex - 1 + z + (h - 1)
                                                * (roiScanlineStride);
                                        if (index < roiDataLength) {
                                            // Update of the weight sum
                                            tmpROI += ((byte) (roiDataArray[index] & 0xff) != 0 ? 1
                                                    : 0);
                                        }
                                    }
                                }

                                // Control if the 16 pixel are outside the ROI
                                if (tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                                } else {
                                    for (int h = 0; h < 4; h++) {
                                        // Row temporary sum initialization
                                        double tempSum = 0;
                                        for (int z = 0; z < 4; z++) {
                                            // Update of the temporary sum
                                            tempSum += (pixelKernel[h][z] * dataHd[offsetX + z]);
                                        }
                                        // Vertical sum update
                                        sum += tempSum * dataVd[offsetY + h];
                                    }
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = sum;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                            if (fracx == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracx = 0.999999F;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracy = 0.999999F;
                            }
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
                    double s_x =  src_pt.getX();
                    double s_y =  src_pt.getY();

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
                            dst_min_x, dst_max_x, 1, 2, 1, 2);
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

                        // integral fractional value calculation
                        final int xfrac = (int) (shift * fracx);
                        final int yfrac = (int) (shift * fracy);
                        // X and Y offset initialization
                        final int offsetX = 4 * xfrac;
                        final int offsetY = 4 * yfrac;

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;

                        // PixelPositions
                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (!roiBounds.contains(x0, y0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                double sum = 0;

                                final double[][] pixelKernel = new double[4][4];

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride];
                                        tmpROI += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0) & 0xff;
                                    }
                                }

                                // Control if the 16 pixel are outside the ROI
                                if (tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                                } else {
                                    for (int h = 0; h < 4; h++) {
                                        // Row temporary sum initialization
                                        double tempSum = 0;
                                        for (int z = 0; z < 4; z++) {
                                            // Update of the temporary sum
                                            tempSum += (pixelKernel[h][z] * dataHd[offsetX
                                                    + z]);
                                        }
                                        // Vertical sum update
                                        sum += tempSum * dataVd[offsetY + h];
                                    }
                                    
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = sum;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                            if (fracx == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracx = 0.999999F;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracy = 0.999999F;
                            }
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
                double s_x =  src_pt.getX();
                double s_y =  src_pt.getY();

                s_x -= 0.5;
                s_y -= 0.5;

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix * 1.0d;
                fracy = s_y - s_iy * 1.0d;

                for (int x = dst_min_x; x < dst_max_x; x++) {

                    // integral fractional value calculation
                    final int xfrac = (int) (shift * fracx);
                    final int yfrac = (int) (shift * fracy);
                    // X and Y offset initialization
                    final int offsetX = 4 * xfrac;
                    final int offsetY = 4 * yfrac;

                    final int posx = (s_ix - srcRectX) * srcPixelStride;
                    final int posy = (s_iy - srcRectY) * srcScanlineStride;

                    final int pos = posx + posy;

                    if ((s_ix >= src_rect_x1 + 1) && (s_ix < (src_rect_x2 - 2))
                            && (s_iy >= (src_rect_y1 + 1)) && (s_iy < (src_rect_y2 - 2))) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            double sum = 0;

                            final double[][] pixelKernel = new double[4][4];
                            int[][] weightArray = new int[4][4];
                            int[] weightArrayVertical = new int[4];
                            int tmpND = 0;

                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            // and cycle for filling all the ROI index by shifting of 1 on the x axis
                            // and by 1 on the y axis.
                            for (int h = 0; h < 4; h++) {
                                for (int z = 0; z < 4; z++) {
                                    // Selection of one pixel
                                    double value = srcDataArrays[k2][pos + (z - 1)
                                                                    * srcPixelStride + (h - 1) * srcScanlineStride];
                                    pixelKernel[h][z] = value;
                                    
                                    if (!noData.contains(value)&& !(isNotPointRange && Double.isNaN(value))) {
                                        tmpND++;
                                        weightArray[h][z] = 1;
                                    }
                                }
                            }

                            // Control if the 16 pixel are outside the ROI
                            if (tmpND == 0) {
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                            } else {
                                double[] sumArray = new double[4];

                                for (int h = 0; h < 4; h++) {
                                    // Row temporary sum initialization
                                    double tempSum = 0;
                                    double[] tempData = bicubicInpaintingDouble(pixelKernel[h],
                                            weightArray[h], null);
                                    for (int z = 0; z < 4; z++) {
                                        // Update of the temporary sum
                                        tempSum += (tempData[z] * dataHd[offsetX + z]);
                                    }
                                    weightArrayVertical[h] = weightArray[h][0] + weightArray[h][1]
                                            + weightArray[h][2] + weightArray[h][3];
                                    sumArray[h] = tempSum;
                                }

                                double[] tempData = bicubicInpaintingDouble(sumArray, null,
                                        weightArrayVertical);

                                // Vertical sum update
                                for (int h = 0; h < 4; h++) {
                                    // Update of the temporary sum
                                    sum += tempData[h] * dataVd[offsetY + h];
                                }
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] =  sum;
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
                        if (fracx == 1.0F) {
                            // Avoid overflow in the interpolation table
                            fracx = 0.999999F;
                        }
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                        if (fracy == 1.0F) {
                            // Avoid overflow in the interpolation table
                            fracy = 0.999999F;
                        }
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
                    double s_x = src_pt.getX();
                    double s_y =  src_pt.getY();

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
                            dst_min_x, dst_max_x, 1, 2, 1, 2);
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

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        // integral fractional value calculation
                        final int xfrac = (int) (shift * fracx);
                        final int yfrac = (int) (shift * fracy);
                        // X and Y offset initialization
                        final int offsetX = 4 * xfrac;
                        final int offsetY = 4 * yfrac;

                        final int pos = posx + posy;

                        if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                double sum = 0;

                                final double[][] pixelKernel = new double[4][4];
                                int[][] weightArray = new int[4][4];
                                int[] weightArrayVertical = new int[4];

                                int tmpND = 0;
                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        double value = srcDataArrays[k2][pos + (z - 1)
                                                                        * srcPixelStride + (h - 1) * srcScanlineStride];
                                        pixelKernel[h][z] = value;
                                        int index = baseIndex - 1 + z + (h - 1)
                                                * (roiScanlineStride);
                                        if (index < roiDataLength) {
                                            // Update of the weight sum
                                            tmpROI += ((byte) (roiDataArray[index] & 0xff) != 0 ? 1
                                                    : 0);
                                        }
                                        
                                        if (!noData.contains(value)&& !(isNotPointRange && Double.isNaN(value))) {
                                            tmpND++;
                                            weightArray[h][z] = 1;
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                if (tmpND == 0 || tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                                } else {
                                    double[] sumArray = new double[4];

                                    for (int h = 0; h < 4; h++) {
                                        // Row temporary sum initialization
                                        double tempSum = 0;
                                        double[] tempData = bicubicInpaintingDouble(pixelKernel[h],
                                                weightArray[h], null);
                                        for (int z = 0; z < 4; z++) {
                                            // Update of the temporary sum
                                            tempSum += (tempData[z] * dataHd[offsetX + z]);
                                        }
                                        weightArrayVertical[h] = weightArray[h][0]
                                                + weightArray[h][1] + weightArray[h][2]
                                                + weightArray[h][3];
                                        sumArray[h] = tempSum;
                                    }

                                    double[] tempData = bicubicInpaintingDouble(sumArray, null,
                                            weightArrayVertical);

                                    // Vertical sum update
                                    for (int h = 0; h < 4; h++) {
                                        // Update of the temporary sum
                                        sum += tempData[h] * dataVd[offsetY + h];
                                    }

                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = sum;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                            if (fracx == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracx = 0.999999F;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracy = 0.999999F;
                            }
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
                    double s_x =  src_pt.getX();
                    double s_y =  src_pt.getY();

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
                            dst_min_x, dst_max_x, 1, 2, 1, 2);
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

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        // integral fractional value calculation
                        final int xfrac = (int) (shift * fracx);
                        final int yfrac = (int) (shift * fracy);
                        // X and Y offset initialization
                        final int offsetX = 4 * xfrac;
                        final int offsetY = 4 * yfrac;

                        final int pos = posx + posy;

                        if (!roiBounds.contains(x0, y0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                double sum = 0;

                                final double[][] pixelKernel = new double[4][4];
                                int[][] weightArray = new int[4][4];
                                int[] weightArrayVertical = new int[4];
                                
                                int tmpND = 0;
                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        double value = srcDataArrays[k2][pos + (z - 1)
                                                                        * srcPixelStride + (h - 1) * srcScanlineStride];
                                        pixelKernel[h][z] = value;

                                        tmpROI += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0) & 0xff;

                                        if (!noData.contains(value)&& !(isNotPointRange && Double.isNaN(value))) {
                                            tmpND++;
                                            weightArray[h][z] = 1;
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                // Control if the 16 pixel are outside the ROI
                                if (tmpND == 0 || tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                                } else {
                                    double[] sumArray = new double[4];

                                    for (int h = 0; h < 4; h++) {
                                        // Row temporary sum initialization
                                        double tempSum = 0;
                                        double[] tempData = bicubicInpaintingDouble(pixelKernel[h],
                                                weightArray[h], null);
                                        for (int z = 0; z < 4; z++) {
                                            // Update of the temporary sum
                                            tempSum += (tempData[z] * dataHd[offsetX + z]);
                                        }
                                        weightArrayVertical[h] = weightArray[h][0]
                                                + weightArray[h][1] + weightArray[h][2]
                                                + weightArray[h][3];
                                        sumArray[h] = tempSum;
                                    }

                                    double[] tempData = bicubicInpaintingDouble(sumArray, null,
                                            weightArrayVertical);

                                    // Vertical sum update
                                    for (int h = 0; h < 4; h++) {
                                        // Update of the temporary sum
                                        sum += tempData[h] * dataVd[offsetY + h];
                                    }

                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = sum;
                                }
                            }
                        }
                        // walk
                        if (fracx < fracdx1) {
                            s_ix += incx;
                            fracx += fracdx;
                            if (fracx == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracx = 0.999999F;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == 1.0F) {
                                // Avoid overflow in the interpolation table
                                fracy = 0.999999F;
                            }
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

    // This method is used for filling the no data values inside the interpolation kernel with the values of the adjacent pixels
    private long[] bicubicInpainting(long[] array, int[] weightArray, int[] weight0) {
        long s_ = array[0];
        long s0 = array[1];
        long s1 = array[2];
        long s2 = array[3];

        if (weightArray == null) {
            weightArray = new int[4];
            if (s_ == 0 && weight0[0] == 0) {
                weightArray[0] = 0;
            } else {
                weightArray[0] = 1;
            }
            if (s0 == 0 && weight0[1] == 0) {
                weightArray[1] = 0;
            } else {
                weightArray[1] = 1;
            }
            if (s1 == 0 && weight0[2] == 0) {
                weightArray[2] = 0;
            } else {
                weightArray[2] = 1;
            }
            if (s2 == 0 && weight0[3] == 0) {
                weightArray[3] = 0;
            } else {
                weightArray[3] = 1;
            }
        }

        // empty array containing the final values of the selected 4 pixels
        long[] emptyArray = new long[4];

        // Calculation of the number of data
        int sum = weightArray[0] + weightArray[1] + weightArray[2] + weightArray[3];
        // mean value used in calculations
        long meanValue = 0;
        switch (sum) {
        // All the 4 pixels are no data, an array of 0 data is returned
        case 0:
            return emptyArray;
            // Only one pixel is a valid data, all the pixel of the line have the same value.
        case 1:
            long validData = 0;
            if (weightArray[0] == 1) {
                validData = s_;
            } else if (weightArray[1] == 1) {
                validData = s0;
            } else if (weightArray[2] == 1) {
                validData = s1;
            } else {
                validData = s2;
            }
            emptyArray[0] = validData;
            emptyArray[1] = validData;
            emptyArray[2] = validData;
            emptyArray[3] = validData;
            return emptyArray;
            // Only 2 pixels are valid data. If the No Data are on the border, they takes the value of the adjacent pixel,
            // else , they take an average of the 2 neighbor pixels with valid data. A String representation is provided for a better
            // comprehension. 0 is no Data and x is valid data.
        case 2:

            // 0 0 x x
            if (weightArray[0] == 0 && weightArray[1] == 0) {
                emptyArray[0] = s1;
                emptyArray[1] = s1;
                emptyArray[2] = s1;
                emptyArray[3] = s2;
                // 0 x 0 x
            } else if (weightArray[0] == 0 && weightArray[2] == 0) {
                meanValue = (s0 + s2) / 2;
                emptyArray[0] = s0;
                emptyArray[1] = s0;
                emptyArray[2] = meanValue;
                emptyArray[3] = s2;
                // 0 x x 0
            } else if (weightArray[0] == 0 && weightArray[3] == 0) {
                emptyArray[0] = s0;
                emptyArray[1] = s0;
                emptyArray[2] = s1;
                emptyArray[3] = s1;
                // x 0 0 x
            } else if (weightArray[1] == 0 && weightArray[2] == 0) {
                meanValue = (s_ + s2) / 2;
                emptyArray[0] = s_;
                emptyArray[1] = meanValue;
                emptyArray[2] = meanValue;
                emptyArray[3] = s2;
                // x 0 x 0
            } else if (weightArray[1] == 0 && weightArray[3] == 0) {
                meanValue = (s_ + s1) / 2;
                emptyArray[0] = s_;
                emptyArray[1] = meanValue;
                emptyArray[2] = s1;
                emptyArray[3] = s1;
                // x x 0 0
            } else {
                emptyArray[0] = s_;
                emptyArray[1] = s0;
                emptyArray[2] = s0;
                emptyArray[3] = s0;
            }
            return emptyArray;
            // Only one pixel is a No Data. If it is at the boundaries, then it replicates the value
            // of the adjacent pixel, else if takes an average of the 2 neighbor pixels.A String representation is provided for a better
            // comprehension. 0 is no Data and x is valid data.
        case 3:
            // 0 x x x
            if (weightArray[0] == 0) {
                emptyArray[0] = s0;
                emptyArray[1] = s0;
                emptyArray[2] = s1;
                emptyArray[3] = s2;
                // x 0 x x
            } else if (weightArray[1] == 0) {
                meanValue = (s_ + s1) / 2;
                emptyArray[0] = s_;
                emptyArray[1] = meanValue;
                emptyArray[2] = s1;
                emptyArray[3] = s2;
                // x x 0 x
            } else if (weightArray[2] == 0) {
                meanValue = (s0 + s2) / 2;
                emptyArray[0] = s_;
                emptyArray[1] = s0;
                emptyArray[2] = meanValue;
                emptyArray[3] = s2;
                // x x x 0
            } else {
                emptyArray[0] = s_;
                emptyArray[1] = s0;
                emptyArray[2] = s1;
                emptyArray[3] = s1;
            }
            return emptyArray;
            // Absence of No Data, the pixels are returned.
        case 4:
            emptyArray[0] = s_;
            emptyArray[1] = s0;
            emptyArray[2] = s1;
            emptyArray[3] = s2;
            return emptyArray;
        default:
            throw new IllegalArgumentException("The input array cannot have more than 4 pixels");
        }
    }

    // This method is used for filling the no data values inside the interpolation kernel with the values of the adjacent pixels
    private double[] bicubicInpaintingDouble(double[] array, int[] weightArray, int[] weight0) {
        double s_ = array[0];
        double s0 = array[1];
        double s1 = array[2];
        double s2 = array[3];

        if (weightArray == null) {
            weightArray = new int[4];
            if (s_ == 0 && weight0[0] == 0) {
                weightArray[0] = 0;
            } else {
                weightArray[0] = 1;
            }
            if (s0 == 0 && weight0[1] == 0) {
                weightArray[1] = 0;
            } else {
                weightArray[1] = 1;
            }
            if (s1 == 0 && weight0[2] == 0) {
                weightArray[2] = 0;
            } else {
                weightArray[2] = 1;
            }
            if (s2 == 0 && weight0[3] == 0) {
                weightArray[3] = 0;
            } else {
                weightArray[3] = 1;
            }
        }

        // empty array containing the final values of the selected 4 pixels
        double[] emptyArray = new double[4];

        // Calculation of the number of data
        int sum = weightArray[0] + weightArray[1] + weightArray[2] + weightArray[3];
        // mean value used in calculations
        double meanValue = 0;
        switch (sum) {
        // All the 4 pixels are no data, an array of 0 data is returned
        case 0:
            return emptyArray;
            // Only one pixel is a valid data, all the pixel of the line have the same value.
        case 1:
            double validData = 0;
            if (weightArray[0] == 1) {
                validData = s_;
            } else if (weightArray[1] == 1) {
                validData = s0;
            } else if (weightArray[2] == 1) {
                validData = s1;
            } else {
                validData = s2;
            }
            emptyArray[0] = validData;
            emptyArray[1] = validData;
            emptyArray[2] = validData;
            emptyArray[3] = validData;
            return emptyArray;
            // Only 2 pixels are valid data. If the No Data are on the border, they takes the value of the adjacent pixel,
            // else , they take an average of the 2 neighbor pixels with valid data. A String representation is provided for a better
            // comprehension. 0 is no Data and x is valid data.
        case 2:

            // 0 0 x x
            if (weightArray[0] == 0 && weightArray[1] == 0) {
                emptyArray[0] = s1;
                emptyArray[1] = s1;
                emptyArray[2] = s1;
                emptyArray[3] = s2;
                // 0 x 0 x
            } else if (weightArray[0] == 0 && weightArray[2] == 0) {
                meanValue = (s0 + s2) / 2;
                emptyArray[0] = s0;
                emptyArray[1] = s0;
                emptyArray[2] = meanValue;
                emptyArray[3] = s2;
                // 0 x x 0
            } else if (weightArray[0] == 0 && weightArray[3] == 0) {
                emptyArray[0] = s0;
                emptyArray[1] = s0;
                emptyArray[2] = s1;
                emptyArray[3] = s1;
                // x 0 0 x
            } else if (weightArray[1] == 0 && weightArray[2] == 0) {
                meanValue = (s_ + s2) / 2;
                emptyArray[0] = s_;
                emptyArray[1] = meanValue;
                emptyArray[2] = meanValue;
                emptyArray[3] = s2;
                // x 0 x 0
            } else if (weightArray[1] == 0 && weightArray[3] == 0) {
                meanValue = (s_ + s1) / 2;
                emptyArray[0] = s_;
                emptyArray[1] = meanValue;
                emptyArray[2] = s1;
                emptyArray[3] = s1;
                // x x 0 0
            } else {
                emptyArray[0] = s_;
                emptyArray[1] = s0;
                emptyArray[2] = s0;
                emptyArray[3] = s0;
            }
            return emptyArray;
            // Only one pixel is a No Data. If it is at the boundaries, then it replicates the value
            // of the adjacent pixel, else if takes an average of the 2 neighbor pixels.A String representation is provided for a better
            // comprehension. 0 is no Data and x is valid data.
        case 3:
            // 0 x x x
            if (weightArray[0] == 0) {
                emptyArray[0] = s0;
                emptyArray[1] = s0;
                emptyArray[2] = s1;
                emptyArray[3] = s2;
                // x 0 x x
            } else if (weightArray[1] == 0) {
                meanValue = (s_ + s1) / 2;
                emptyArray[0] = s_;
                emptyArray[1] = meanValue;
                emptyArray[2] = s1;
                emptyArray[3] = s2;
                // x x 0 x
            } else if (weightArray[2] == 0) {
                meanValue = (s0 + s2) / 2;
                emptyArray[0] = s_;
                emptyArray[1] = s0;
                emptyArray[2] = meanValue;
                emptyArray[3] = s2;
                // x x x 0
            } else {
                emptyArray[0] = s_;
                emptyArray[1] = s0;
                emptyArray[2] = s1;
                emptyArray[3] = s1;
            }
            return emptyArray;
            // Absence of No Data, the pixels are returned.
        case 4:
            emptyArray[0] = s_;
            emptyArray[1] = s0;
            emptyArray[2] = s1;
            emptyArray[3] = s2;
            return emptyArray;
        default:
            throw new IllegalArgumentException("The input array cannot have more than 4 pixels");
        }
    }
}
