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
package it.geosolutions.jaiext.affine;

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
import java.util.Arrays;
import java.util.Map;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.iterator.RandomIter;

import com.sun.media.jai.util.ImageUtil;

import it.geosolutions.jaiext.interpolators.InterpolationBicubic;
import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.NoDataContainer;
import it.geosolutions.jaiext.range.Range;

public class AffineBicubicOpImage extends AffineOpImage {

    private static final int KERNEL_LINE_DIM = 4;

    private static final float OVERFLOW = 1.0F;

    private static final float AVOID_OVERFLOW = 0.999999F;

    /** Nearest-Neighbor interpolator */
    protected InterpolationBicubic interpBN = null;

    /** Byte lookuptable used if no data are present */
    protected byte[][] byteLookupTable;

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
            double[] backgroundValues, boolean setDestinationNoData, boolean useROIAccessor, Range nodata) {
        super(source, extender, config, layout, transform, interp, backgroundValues);
        affineOpInitialization(source, interp, layout, backgroundValues, useROIAccessor, setDestinationNoData, nodata);
    }

    private void affineOpInitialization(RenderedImage source, Interpolation interp,
            ImageLayout layout, double[] backgroundValues, boolean useROIAccessor, boolean setDestinationNoData, Range nodata) {

        SampleModel sm = source.getSampleModel();

        // If the source has an IndexColorModel, override the default setting
        // in OpImage. The dest shall have exactly the same SampleModel and
        // ColorModel as the source.
        // Note, in this case, the source should have an integral data type.
        ColorModel srcColorModel = source.getColorModel();
        if (srcColorModel instanceof IndexColorModel  && ImageUtil.isBinary(sm)) {
            sampleModel = source.getSampleModel()
                    .createCompatibleSampleModel(tileWidth, tileHeight);
            colorModel = srcColorModel;
        }
        // NumBands
        int numBands = getSampleModel().getNumBands();

        // Source image data Type
        int srcDataType = sm.getDataType();

        // If both roiBounds and roiIter are not null, they are used in calculation
        Range nod = nodata;
        double[] destNod = null;
        if (backgroundValues != null && backgroundValues.length > 0) {
            destNod = backgroundValues;
        }
        if (interp instanceof InterpolationBicubic) {
            interpBN = (InterpolationBicubic) interp;
            this.interp = interpBN;
            interpBN.setROIBounds(roiBounds);
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

            if (nod == null) {
                nod = interpBN.getNoDataRange();
            }
            if (destNod == null) {
                destNod = new double[]{interpBN.getDestinationNoData()};
            }
        }

        // Nodata definition
        if (nod != null) {
            hasNoData = true;
            noData = nod;
        }
        if (destNod != null) {
            destinationNoDataDouble = destNod;
        } else if (this.backgroundValues != null && this.backgroundValues.length > 0) {
            destinationNoDataDouble = this.backgroundValues;
        }
        // Expand the destination nodata values if not defined
        if(destinationNoDataDouble != null && destinationNoDataDouble.length < numBands){
            double[] tmp = new double[numBands]; 
            Arrays.fill(tmp, destinationNoDataDouble[0]);
            destinationNoDataDouble = tmp;
        }
        // ROIAccessor definition
        if (hasROI) {
            this.useROIAccessor = useROIAccessor;
        }

        // destination No Data set
        this.setDestinationNoData = setDestinationNoData;
        this.setBackground = setDestinationNoData;
     // Create the destination No data arrays
        destinationNoDataByte = new byte[numBands];
        destinationNoDataShort = new short[numBands];
        destinationNoDataUShort = new short[numBands];
        destinationNoDataInt = new int[numBands];
        destinationNoDataFloat = new float[numBands];
        // Populate the arrays
        for (int i = 0; i < numBands; i++) {
            destinationNoDataByte[i] = (byte) ((int) destinationNoDataDouble[i] & 0xFF);
            destinationNoDataUShort[i] = (short) (((short) destinationNoDataDouble[i]) & 0xffff);
            destinationNoDataShort[i] = (short) destinationNoDataDouble[i];
            destinationNoDataInt[i] = (int) destinationNoDataDouble[i];
            destinationNoDataFloat[i] = (float) destinationNoDataDouble[i];
        }

        // special byte case
        if (srcDataType == DataBuffer.TYPE_BYTE && hasNoData) {
            // Creation of a lookuptable containing the values to use for no data
            byteLookupTable = new byte[numBands][256];
            for (int i = 0; i < byteLookupTable[0].length; i++) {
                byte value = (byte) i;
                for (int b = 0; b < numBands; b++) {
                    if (noData.contains(value)) {
                        if (setDestinationNoData) {
                            byteLookupTable[b][i] = destinationNoDataByte[b];
                        } else {
                            byteLookupTable[b][i] = 0;
                            if(i !=0){
                                byteLookupTable[b][0] = 1;    
                            }
                        }
                    } else {
                        byteLookupTable[b][i] = value;
                    }
                }
            }
        }

        if (destinationNoDataDouble != null) {
            setProperty(NoDataContainer.GC_NODATA, new NoDataContainer(destinationNoDataDouble));
        }

        // Definition of the possible cases that can be found
        // caseA = no ROI nor No Data
        // caseB = ROI present but No Data not present
        // caseC = No Data present but ROI not present
        // Last case not defined = both ROI and No Data are present
        caseA = !hasROI && !hasNoData;
        caseB = hasROI && !hasNoData;
        caseC = !hasROI && hasNoData;

    }

    /** Method for evaluating the destination image tile without ROI */
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        RasterFormatTag[] formatTags = getFormatTags();
        // Source image
        Raster source = sources[0];
        // Source rectangle
        Rectangle srcRect = source.getBounds();
        // Src upper left pixel coordinates
        int srcRectX = srcRect.x;
        int srcRectY = srcRect.y;

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
        RandomIter roiIter = null;

        // ROI calculation only if the roi raster is present
        if (hasROI) {
            if (useROIAccessor) {
                if(srcROIImage.getBounds().contains(srcRect)){
                    roi = srcROIImage.getData(srcRect);
                } else{
                    roi = srcROIImgExt.getData(srcRect);
                }
                roiAccessor = new RasterAccessor(roi, srcRect, RasterAccessor.findCompatibleTags(
                        new RenderedImage[] { srcROIImage }, srcROIImage)[0],
                        srcROIImage.getColorModel());
            } else {
                roiIter = RandomIterFactory.create(srcROIImgExt, roiRect, true, true);
            }
        }

        int dataType = dest.getSampleModel().getDataType();
        // If the image is not binary, then for every kind of dataType, the image affine transformation
        // is performed.

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(dataType, srcAccessor, destRect, srcRectX, srcRectY, dstAccessor, roiAccessor, roiIter);
            break;
        case DataBuffer.TYPE_INT:
            intLoop(dataType, srcAccessor, destRect, srcRectX, srcRectY, dstAccessor, roiAccessor, roiIter);
            break;
        case DataBuffer.TYPE_SHORT:
            shortLoop(dataType, srcAccessor, destRect, srcRectX, srcRectY, dstAccessor, roiAccessor, roiIter);
            break;
        case DataBuffer.TYPE_USHORT:
            ushortLoop(dataType, srcAccessor, destRect, srcRectX, srcRectY, dstAccessor,
                    roiAccessor, roiIter);
            break;
        case DataBuffer.TYPE_FLOAT:
            floatLoop(dataType, srcAccessor, destRect, srcRectX, srcRectY, dstAccessor, roiAccessor, roiIter);
            break;
        case DataBuffer.TYPE_DOUBLE:
            doubleLoop(dataType, srcAccessor, destRect, srcRectX, srcRectY, dstAccessor,
                    roiAccessor, roiIter);
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
            int srcRectY, RasterAccessor dst, RasterAccessor roi, RandomIter roiIter) {

        final float src_rect_x1 = src.getX();
        final float src_rect_y1 = src.getY();
        final float src_rect_x2 = src_rect_x1 + src.getWidth();
        final float src_rect_y2 = src_rect_y1 + src.getHeight();

        final float src_rect_x11 = src_rect_x1 + 1;
        final float src_rect_y11 = src_rect_y1 + 1;
        final float src_rect_x22 = src_rect_x2 - 2;
        final float src_rect_y22 = src_rect_y2 - 2;

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

        if (caseA) {
            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

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

                    if ((s_ix >= src_rect_x11) && (s_ix < (src_rect_x22))
                            && (s_iy >= (src_rect_y11)) && (s_iy < (src_rect_y22))) {
                        
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        int pos = posx + posy;
                        
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            long sum = 0;

                            int result = 0;

                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                // Row temporary sum initialization
                                long temp = 0;
                                for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                    // Selection of one pixel
                                    int pixelValue = srcDataArrays[k2][pos + (z - 1)
                                            * srcPixelStride + (h - 1) * srcScanlineStride
                                            + bandOffsets[k2]] & 0xff;
                                    // Update of the temporary sum
                                    temp += (pixelValue * dataHi[offsetX + z]);
                                }
                                // Vertical sum update
                                sum += ((temp + round) >> precisionBits) * dataVi[offsetY + h];
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
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte[k2];
                        }

                    }
                    // walk
                    if (fracx < fracdx1) {
                        s_ix += incx;
                        fracx += fracdx;
                        if (fracx == OVERFLOW) {
                            // Avoid overflow in the interpolation table
                            fracx = AVOID_OVERFLOW;
                        }
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                        if (fracy == OVERFLOW) {
                            // Avoid overflow in the interpolation table
                            fracy = AVOID_OVERFLOW;
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
                    dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

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

                    int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
                    int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte[k2];
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;

                        // If roiAccessor is present, the y position on the roi image is calculated
                        final int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte[k2];
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                final int[][] pixelKernel = new int[KERNEL_LINE_DIM][KERNEL_LINE_DIM];

                                int result = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                    for (int z = 0; z < KERNEL_LINE_DIM; z++) {
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
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte[k2];
                                } else {
                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                            // Update of the temporary sum
                                            tempSum += (pixelKernel[h][z] * dataHi[offsetX + z]);
                                        }
                                        // Vertical sum update
                                        sum += ((tempSum + round) >> precisionBits)
                                                * dataVi[offsetY + h];
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
                            if (fracx == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracx = AVOID_OVERFLOW;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracy = AVOID_OVERFLOW;
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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte[k2];
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
                    dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

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

                    int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
                    int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte[k2];
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;

                        // PixelPositions
                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (!roiBounds.contains(x0, y0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte[k2];
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                final int[][] pixelKernel = new int[KERNEL_LINE_DIM][KERNEL_LINE_DIM];

                                int result = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                    for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride + bandOffsets[k2] ] & 0xff;
                                        tmpROI += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0) & 0xff;
                                    }
                                }

                                // Control if the 16 pixel are outside the ROI
                                if (tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte[k2];
                                } else {
                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                            // Update of the temporary sum
                                            tempSum += (pixelKernel[h][z] * dataHi[offsetX + z]);
                                        }
                                        // Vertical sum update
                                        sum += ((tempSum + round) >> precisionBits)
                                                * dataVi[offsetY + h];
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
                            if (fracx == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracx = AVOID_OVERFLOW;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracy = AVOID_OVERFLOW;
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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte[k2];
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        } else if (caseC) {
            final long[][] pixelKernel = new long[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
            long[] sumArray = new long[KERNEL_LINE_DIM];
            long[] emptyArray = new long[KERNEL_LINE_DIM];
            long[] tempData;

            short weight = 0;
            byte weightVert = 0;
            byte temp = 0;

            // Row temporary sum initialization
            long tempSum = 0;
            long sum = 0;
            // final result initialization
            long result = 0;
            // initial x value definition
            final double dst_min_x_d = dst_min_x + HALF_PIXEL;
            // Band data array creation
            byte[] bandDataArray;
            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x_d, y + HALF_PIXEL);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                double s_x = src_pt.getX();
                double s_y = src_pt.getY();

                s_x -= 0.5;
                s_y -= 0.5;

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix * 1.0d;
                fracy = s_y - s_iy * 1.0d;

                for (int x = dst_min_x; x < dst_max_x; x++) {
                    if ((s_ix >= src_rect_x11) && (s_ix < (src_rect_x22))
                            && (s_iy >= (src_rect_y11)) && (s_iy < (src_rect_y22))) {
                        
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;
                        
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {                                                        
                            bandDataArray = srcDataArrays[k2];
                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            // and check if every kernel pixel is a No Data
                            for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                    // Selection of one pixel
                                    int sample = bandDataArray[pos + (z - 1) * srcPixelStride
                                            + (h - 1) * srcScanlineStride + bandOffsets[k2] ] & 0xff;
                                    pixelKernel[h][z] = sample;
                                    if (byteLookupTable[k2][sample] != destinationNoDataByte[k2]) {
                                        weight |= (1 << (4 * h + z));
                                        // weightArray[h][z] = 1;
                                    } else {
                                        // weightArray[h][z] = 0;
                                        weight &= (0xffff - (1 << 4 * h + z));
                                    }
                                }

                                temp = (byte) ((weight >> 4 * h) & 0x0F);
                                tempData = bicubicInpainting(pixelKernel[h], temp, emptyArray);

                                tempSum = tempData[0] * dataHi[offsetX] + 
                                        tempData[1] * dataHi[offsetX + 1] + 
                                        tempData[2] * dataHi[offsetX + 2] + 
                                        tempData[3] * dataHi[offsetX + 3];

                                if (temp > 0) {
                                    weightVert |= (1 << h);
                                    // weightArrayVertical[h] = 1;
                                } else {
                                    weightVert &= (0x0F - (1 << h));
                                    // weightArrayVertical[h] = 0;
                                }
                                sumArray[h] = ((tempSum + round) >> precisionBits);
                            }

                            // Control if the 16 pixel are all No Data
                            if (weight == 0) {
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte[k2];
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
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte[k2];
                        }
                    }

                    // walk
                    if (fracx < fracdx1) {
                        s_ix += incx;
                        fracx += fracdx;
                        if (fracx == OVERFLOW) {
                            // Avoid overflow in the interpolation table
                            fracx = AVOID_OVERFLOW;
                        }
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                        if (fracy == OVERFLOW) {
                            // Avoid overflow in the interpolation table
                            fracy = AVOID_OVERFLOW;
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
                final long[][] pixelKernel = new long[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
                long[] sumArray = new long[KERNEL_LINE_DIM];
                long[] emptyArray = new long[KERNEL_LINE_DIM];

                short weight = 0;
                byte weightVert = 0;
                byte temp = 0;

                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

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

                    int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
                    int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte[k2];
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

                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int pos = posx + posy;

                        if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte[k2];
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                int result = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                    for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride + bandOffsets[k2] ] & 0xff;
                                        int index = baseIndex - 1 + z + (h - 1)
                                                * (roiScanlineStride);
                                        if (index < roiDataLength) {
                                            // Update of the weight sum
                                            tmpROI += ((byte) (roiDataArray[index] & 0xff) != 0 ? 1
                                                    : 0);
                                        }

                                        if (byteLookupTable[k2][(int) pixelKernel[h][z]] != destinationNoDataByte[k2]) {
                                            weight |= (1 << (4 * h + z));
                                            // weightArray[h][z] = 1;
                                        } else {
                                            // weightArray[h][z] = 0;
                                            weight &= (0xffff - (1 << 4 * h + z));
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                if (weight == 0 || tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte[k2];
                                } else {

                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        temp = (byte) ((weight >> 4 * h) & 0x0F);
                                        long[] tempData = bicubicInpainting(pixelKernel[h], temp,
                                                emptyArray);
                                        for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                            // Update of the temporary sum
                                            tempSum += (tempData[z] * dataHi[offsetX + z]);
                                        }
                                        if (temp > 0) {
                                            weightVert |= (1 << h);
                                            // weightArrayVertical[h] = 1;
                                        } else {
                                            weightVert &= (0x0F - (1 << h));
                                            // weightArrayVertical[h] = 0;
                                        }
                                        sumArray[h] = ((tempSum + round) >> precisionBits);
                                    }

                                    long[] tempData = bicubicInpainting(sumArray, weightVert,
                                            emptyArray);
                                    weight = 0;
                                    weightVert = 0;
                                    // Vertical sum update
                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Update of the temporary sum
                                        sum += tempData[h] * dataVi[offsetY + h];
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
                            if (fracx == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracx = AVOID_OVERFLOW;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracy = AVOID_OVERFLOW;
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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte[k2];
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            } else {
                final long[][] pixelKernel = new long[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
                long[] sumArray = new long[KERNEL_LINE_DIM];
                long[] emptyArray = new long[KERNEL_LINE_DIM];

                short weight = 0;
                byte weightVert = 0;
                byte temp = 0;
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

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

                    int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
                    int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte[k2];
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {
                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int x0 = src.getX() + posx / srcPixelStride;
                        final int y0 = src.getY() + posy / srcScanlineStride;

                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int pos = posx + posy;

                        if (!roiBounds.contains(x0, y0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte[k2];
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                int result = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                    for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride + bandOffsets[k2] ] & 0xff;

                                        tmpROI += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0) & 0xff;

                                        if (byteLookupTable[k2][(int) pixelKernel[h][z]] != destinationNoDataByte[k2]) {
                                            weight |= (1 << (4 * h + z));
                                        } else {
                                            weight &= (0xffff - (1 << 4 * h + z));
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                if (weight == 0 || tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte[k2];
                                } else {

                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        temp = (byte) ((weight >> 4 * h) & 0x0F);
                                        long[] tempData = bicubicInpainting(pixelKernel[h], temp,
                                                emptyArray);
                                        for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                            // Update of the temporary sum
                                            tempSum += (tempData[z] * dataHi[offsetX + z]);
                                        }
                                        if (temp > 0) {
                                            weightVert |= (1 << h);
                                        } else {
                                            weightVert &= (0x0F - (1 << h));
                                        }
                                        sumArray[h] = ((tempSum + round) >> precisionBits);
                                    }

                                    long[] tempData = bicubicInpainting(sumArray, weightVert,
                                            emptyArray);
                                    weight = 0;
                                    weightVert = 0;
                                    // Vertical sum update
                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Update of the temporary sum
                                        sum += tempData[h] * dataVi[offsetY + h];
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
                            if (fracx == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracx = AVOID_OVERFLOW;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracy = AVOID_OVERFLOW;
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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte[k2];
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
            int srcRectY, RasterAccessor dst, RasterAccessor roi, RandomIter roiIter) {

        final float src_rect_x1 = src.getX();
        final float src_rect_y1 = src.getY();
        final float src_rect_x2 = src_rect_x1 + src.getWidth();
        final float src_rect_y2 = src_rect_y1 + src.getHeight();

        final float src_rect_x11 = src_rect_x1 + 1;
        final float src_rect_y11 = src_rect_y1 + 1;
        final float src_rect_x22 = src_rect_x2 - 2;
        final float src_rect_y22 = src_rect_y2 - 2;

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

        if (caseA) {
            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

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
                    
                    if ((s_ix >= src_rect_x11) && (s_ix < (src_rect_x22))
                            && (s_iy >= (src_rect_y11)) && (s_iy < (src_rect_y22))) {
                        
                        // X and Y offset initialization
                        int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        int posx = (s_ix - srcRectX) * srcPixelStride;
                        int posy = (s_iy - srcRectY) * srcScanlineStride;

                        int pos = posx + posy;
                        
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            long sum = 0;

                            int result = 0;

                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                // Row temporary sum initialization
                                long temp = 0;
                                for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                    // Selection of one pixel
                                    int pixelValue = srcDataArrays[k2][pos + (z - 1)
                                            * srcPixelStride + (h - 1) * srcScanlineStride
                                            + bandOffsets[k2]] & 0xffff;
                                    // Update of the temporary sum
                                    temp += (pixelValue * dataHi[offsetX + z]);
                                }
                                // Vertical sum update
                                sum += ((temp + round) >> precisionBits) * dataVi[offsetY + h];
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
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort[k2];
                        }

                    }
                    // walk
                    if (fracx < fracdx1) {
                        s_ix += incx;
                        fracx += fracdx;
                        if (fracx == OVERFLOW) {
                            // Avoid overflow in the interpolation table
                            fracx = AVOID_OVERFLOW;
                        }
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                        if (fracy == OVERFLOW) {
                            // Avoid overflow in the interpolation table
                            fracy = AVOID_OVERFLOW;
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
                    dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

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

                    int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
                    int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort[k2];
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;

                        // If roiAccessor is present, the y position on the roi image is calculated
                        final int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort[k2];
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                final int[][] pixelKernel = new int[KERNEL_LINE_DIM][KERNEL_LINE_DIM];

                                int result = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                    for (int z = 0; z < KERNEL_LINE_DIM; z++) {
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
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort[k2];
                                } else {
                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                            // Update of the temporary sum
                                            tempSum += (pixelKernel[h][z] * dataHi[offsetX + z]);
                                        }
                                        // Vertical sum update
                                        sum += ((tempSum + round) >> precisionBits)
                                                * dataVi[offsetY + h];
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
                            if (fracx == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracx = AVOID_OVERFLOW;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracy = AVOID_OVERFLOW;
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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort[k2];
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
                    dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

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

                    int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
                    int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort[k2];
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;

                        // PixelPositions
                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (!roiBounds.contains(x0, y0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort[k2];
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                final int[][] pixelKernel = new int[KERNEL_LINE_DIM][KERNEL_LINE_DIM];

                                int result = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                    for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride + bandOffsets[k2] ] & 0xffff;
                                        tmpROI += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0) & 0xff;
                                    }
                                }

                                // Control if the 16 pixel are outside the ROI
                                if (tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort[k2];
                                } else {
                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                            // Update of the temporary sum
                                            tempSum += (pixelKernel[h][z] * dataHi[offsetX + z]);
                                        }
                                        // Vertical sum update
                                        sum += ((tempSum + round) >> precisionBits)
                                                * dataVi[offsetY + h];
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
                            if (fracx == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracx = AVOID_OVERFLOW;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracy = AVOID_OVERFLOW;
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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort[k2];
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        } else if (caseC) {
            final long[][] pixelKernel = new long[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
            long[] sumArray = new long[KERNEL_LINE_DIM];
            long[] emptyArray = new long[KERNEL_LINE_DIM];

            short weight = 0;
            byte weightVert = 0;
            byte temp = 0;
            // Row temporary sum initialization
            long tempSum = 0;
            long sum = 0;
            // final result initialization
            long result = 0;
            // initial x value definition
            final double dst_min_x_d = dst_min_x + HALF_PIXEL;
            // Band data array creation
            short[] bandDataArray;

            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x_d, y + HALF_PIXEL);

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
                    if ((s_ix >= src_rect_x11) && (s_ix < (src_rect_x22))
                            && (s_iy >= (src_rect_y11)) && (s_iy < (src_rect_y22))) {
                        
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;
                        
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            bandDataArray = srcDataArrays[k2];
                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            // and check if every kernel pixel is a No Data
                            for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                    // Selection of one pixel
                                    pixelKernel[h][z] = bandDataArray[pos + (z - 1)
                                            * srcPixelStride + (h - 1) * srcScanlineStride + bandOffsets[k2] ] & 0xffff;
                                    if (!noData.contains((short) pixelKernel[h][z])) {
                                        weight |= (1 << (4 * h + z));
                                    } else {
                                        weight &= (0xffff - (1 << 4 * h + z));
                                    }
                                }
                                temp = (byte) ((weight >> 4 * h) & 0x0F);
                                long[] tempData = bicubicInpainting(pixelKernel[h], temp,
                                        emptyArray);

                                tempSum = tempData[0] * dataHi[offsetX] + tempData[1]
                                        * dataHi[offsetX + 1] + tempData[2] * dataHi[offsetX + 2]
                                        + tempData[3] * dataHi[offsetX + 3];

                                if (temp > 0) {
                                    weightVert |= (1 << h);
                                } else {
                                    weightVert &= (0x0F - (1 << h));
                                }
                                sumArray[h] = ((tempSum + round) >> precisionBits);

                            }

                            // Control if the 16 pixel are all No Data
                            if (weight == 0) {
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort[k2];
                            } else {
                                long[] tempData = bicubicInpainting(sumArray, weightVert,
                                        emptyArray);
                                weight = 0;
                                weightVert = 0;
                                // Vertical sum update
                                sum = tempData[0] * dataVi[offsetY] + tempData[1]
                                        * dataVi[offsetY + 1] + tempData[2] * dataVi[offsetY + 2]
                                        + tempData[3] * dataVi[offsetY + 3];

                                // Interpolation
                                result = (int) ((sum + round) >> precisionBits);
                                sum = 0;
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
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort[k2];
                        }
                    }

                    // walk
                    if (fracx < fracdx1) {
                        s_ix += incx;
                        fracx += fracdx;
                        if (fracx == OVERFLOW) {
                            // Avoid overflow in the interpolation table
                            fracx = AVOID_OVERFLOW;
                        }
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                        if (fracy == OVERFLOW) {
                            // Avoid overflow in the interpolation table
                            fracy = AVOID_OVERFLOW;
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
                final long[][] pixelKernel = new long[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
                long[] sumArray = new long[KERNEL_LINE_DIM];
                long[] emptyArray = new long[KERNEL_LINE_DIM];

                short weight = 0;
                byte weightVert = 0;
                byte temp = 0;
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

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

                    int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
                    int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort[k2];
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

                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int pos = posx + posy;

                        if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort[k2];
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                int result = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                    for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride + bandOffsets[k2] ] & 0xffff;
                                        int index = baseIndex - 1 + z + (h - 1)
                                                * (roiScanlineStride);
                                        if (index < roiDataLength) {
                                            // Update of the weight sum
                                            tmpROI += ((byte) (roiDataArray[index] & 0xff) != 0 ? 1
                                                    : 0);
                                        }

                                        if (!noData.contains((short) pixelKernel[h][z])) {
                                            weight |= (1 << (4 * h + z));
                                        } else {
                                            weight &= (0xffff - (1 << 4 * h + z));
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                if (weight == 0 || tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort[k2];
                                } else {

                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        temp = (byte) ((weight >> 4 * h) & 0x0F);
                                        long[] tempData = bicubicInpainting(pixelKernel[h], temp,
                                                emptyArray);
                                        for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                            // Update of the temporary sum
                                            tempSum += (tempData[z] * dataHi[offsetX + z]);
                                        }
                                        if (temp > 0) {
                                            weightVert |= (1 << h);
                                        } else {
                                            weightVert &= (0x0F - (1 << h));
                                        }
                                        sumArray[h] = ((tempSum + round) >> precisionBits);
                                    }

                                    long[] tempData = bicubicInpainting(sumArray, weightVert,
                                            emptyArray);
                                    weight = 0;
                                    weightVert = 0;
                                    // Vertical sum update
                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Update of the temporary sum
                                        sum += tempData[h] * dataVi[offsetY + h];
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
                            if (fracx == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracx = AVOID_OVERFLOW;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracy = AVOID_OVERFLOW;
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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort[k2];
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            } else {
                final long[][] pixelKernel = new long[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
                long[] sumArray = new long[KERNEL_LINE_DIM];
                long[] emptyArray = new long[KERNEL_LINE_DIM];

                short weight = 0;
                byte weightVert = 0;
                byte temp = 0;
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

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

                    int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
                    int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort[k2];
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

                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int pos = posx + posy;

                        if (!roiBounds.contains(x0, y0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort[k2];
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                int result = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                    for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride + bandOffsets[k2] ] & 0xffff;

                                        tmpROI += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0) & 0xff;

                                        if (!noData.contains((short) pixelKernel[h][z])) {
                                            weight |= (1 << (4 * h + z));
                                        } else {
                                            weight &= (0xffff - (1 << 4 * h + z));
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                if (weight == 0 || tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort[k2];
                                } else {

                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        temp = (byte) ((weight >> 4 * h) & 0x0F);
                                        long[] tempData = bicubicInpainting(pixelKernel[h], temp,
                                                emptyArray);
                                        for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                            // Update of the temporary sum
                                            tempSum += (tempData[z] * dataHi[offsetX + z]);
                                        }
                                        if (temp > 0) {
                                            weightVert |= (1 << h);
                                        } else {
                                            weightVert &= (0x0F - (1 << h));
                                        }
                                        sumArray[h] = ((tempSum + round) >> precisionBits);
                                    }

                                    long[] tempData = bicubicInpainting(sumArray, weightVert,
                                            emptyArray);
                                    weight = 0;
                                    weightVert = 0;
                                    // Vertical sum update
                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Update of the temporary sum
                                        sum += tempData[h] * dataVi[offsetY + h];
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
                            if (fracx == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracx = AVOID_OVERFLOW;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracy = AVOID_OVERFLOW;
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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort[k2];
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
            int srcRectY, RasterAccessor dst, RasterAccessor roi, RandomIter roiIter) {

        final float src_rect_x1 = src.getX();
        final float src_rect_y1 = src.getY();
        final float src_rect_x2 = src_rect_x1 + src.getWidth();
        final float src_rect_y2 = src_rect_y1 + src.getHeight();

        final float src_rect_x11 = src_rect_x1 + 1;
        final float src_rect_y11 = src_rect_y1 + 1;
        final float src_rect_x22 = src_rect_x2 - 2;
        final float src_rect_y22 = src_rect_y2 - 2;

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

        if (caseA) {
            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

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

                    if ((s_ix >= src_rect_x11) && (s_ix < (src_rect_x22))
                            && (s_iy >= (src_rect_y11)) && (s_iy < (src_rect_y22))) {
                        
                        // X and Y offset initialization
                        int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        int posx = (s_ix - srcRectX) * srcPixelStride;
                        int posy = (s_iy - srcRectY) * srcScanlineStride;

                        int pos = posx + posy;
                        
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            long sum = 0;

                            int result = 0;

                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                // Row temporary sum initialization
                                long temp = 0;
                                for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                    // Selection of one pixel
                                    int pixelValue = srcDataArrays[k2][pos + (z - 1)
                                            * srcPixelStride + (h - 1) * srcScanlineStride
                                            + bandOffsets[k2]];
                                    // Update of the temporary sum
                                    temp += (pixelValue * dataHi[offsetX + z]);
                                }
                                // Vertical sum update
                                sum += ((temp + round) >> precisionBits) * dataVi[offsetY + h];
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
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort[k2];
                        }

                    }
                    // walk
                    if (fracx < fracdx1) {
                        s_ix += incx;
                        fracx += fracdx;
                        if (fracx == OVERFLOW) {
                            // Avoid overflow in the interpolation table
                            fracx = AVOID_OVERFLOW;
                        }
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                        if (fracy == OVERFLOW) {
                            // Avoid overflow in the interpolation table
                            fracy = AVOID_OVERFLOW;
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
                    dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

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

                    int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
                    int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort[k2];
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;

                        // If roiAccessor is present, the y position on the roi image is calculated
                        final int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort[k2];
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                final int[][] pixelKernel = new int[KERNEL_LINE_DIM][KERNEL_LINE_DIM];

                                int result = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                    for (int z = 0; z < KERNEL_LINE_DIM; z++) {
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
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort[k2];
                                } else {
                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                            // Update of the temporary sum
                                            tempSum += (pixelKernel[h][z] * dataHi[offsetX + z]);
                                        }
                                        // Vertical sum update
                                        sum += ((tempSum + round) >> precisionBits)
                                                * dataVi[offsetY + h];
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
                            if (fracx == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracx = AVOID_OVERFLOW;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracy = AVOID_OVERFLOW;
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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort[k2];
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
                    dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

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

                    int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
                    int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort[k2];
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;

                        // PixelPositions
                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (!roiBounds.contains(x0, y0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort[k2];
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                final int[][] pixelKernel = new int[KERNEL_LINE_DIM][KERNEL_LINE_DIM];

                                int result = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                    for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride];
                                        tmpROI += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0) & 0xff;
                                    }
                                }

                                // Control if the 16 pixel are outside the ROI
                                if (tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort[k2];
                                } else {
                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                            // Update of the temporary sum
                                            tempSum += (pixelKernel[h][z] * dataHi[offsetX + z]);
                                        }
                                        // Vertical sum update
                                        sum += ((tempSum + round) >> precisionBits)
                                                * dataVi[offsetY + h];
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
                            if (fracx == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracx = AVOID_OVERFLOW;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracy = AVOID_OVERFLOW;
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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort[k2];
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        } else if (caseC) {
            final long[][] pixelKernel = new long[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
            long[] sumArray = new long[KERNEL_LINE_DIM];
            long[] emptyArray = new long[KERNEL_LINE_DIM];

            short weight = 0;
            byte weightVert = 0;
            byte temp = 0;
            // Row temporary sum initialization
            long tempSum = 0;
            long sum = 0;
            // final result initialization
            long result = 0;
            // initial x value definition
            final double dst_min_x_d = dst_min_x + HALF_PIXEL;
            // Band data array creation
            short[] bandDataArray;

            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x_d, y + HALF_PIXEL);

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

                    if ((s_ix >= src_rect_x11) && (s_ix < (src_rect_x22))
                            && (s_iy >= (src_rect_y11)) && (s_iy < (src_rect_y22))) {
                        
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;
                        
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            bandDataArray = srcDataArrays[k2];
                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            // and check if every kernel pixel is a No Data
                            for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                    // Selection of one pixel
                                    pixelKernel[h][z] = bandDataArray[pos + (z - 1)
                                            * srcPixelStride + (h - 1) * srcScanlineStride];
                                    if (!noData.contains((short) pixelKernel[h][z])) {
                                        weight |= (1 << (4 * h + z));
                                    } else {
                                        weight &= (0xffff - (1 << 4 * h + z));
                                    }
                                }
                                temp = (byte) ((weight >> 4 * h) & 0x0F);
                                long[] tempData = bicubicInpainting(pixelKernel[h], temp,
                                        emptyArray);

                                tempSum = tempData[0] * dataHi[offsetX] + tempData[1]
                                        * dataHi[offsetX + 1] + tempData[2] * dataHi[offsetX + 2]
                                        + tempData[3] * dataHi[offsetX + 3];

                                if (temp > 0) {
                                    weightVert |= (1 << h);
                                } else {
                                    weightVert &= (0x0F - (1 << h));
                                }
                                sumArray[h] = ((tempSum + round) >> precisionBits);

                            }

                            // Control if the 16 pixel are all No Data
                            if (weight == 0) {
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort[k2];
                            } else {
                                long[] tempData = bicubicInpainting(sumArray, weightVert,
                                        emptyArray);
                                weight = 0;
                                weightVert = 0;
                                // Vertical sum update
                                sum = tempData[0] * dataVi[offsetY] + tempData[1]
                                        * dataVi[offsetY + 1] + tempData[2] * dataVi[offsetY + 2]
                                        + tempData[3] * dataVi[offsetY + 3];

                                // Interpolation
                                result = (int) ((sum + round) >> precisionBits);
                                sum = 0;
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
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort[k2];
                        }
                    }

                    // walk
                    if (fracx < fracdx1) {
                        s_ix += incx;
                        fracx += fracdx;
                        if (fracx == OVERFLOW) {
                            // Avoid overflow in the interpolation table
                            fracx = AVOID_OVERFLOW;
                        }
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                        if (fracy == OVERFLOW) {
                            // Avoid overflow in the interpolation table
                            fracy = AVOID_OVERFLOW;
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
                final long[][] pixelKernel = new long[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
                long[] sumArray = new long[KERNEL_LINE_DIM];
                long[] emptyArray = new long[KERNEL_LINE_DIM];

                short weight = 0;
                byte weightVert = 0;
                byte temp = 0;
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

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

                    int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
                    int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort[k2];
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

                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int pos = posx + posy;

                        if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort[k2];
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                int result = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                    for (int z = 0; z < KERNEL_LINE_DIM; z++) {
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
                                            weight |= (1 << (4 * h + z));
                                        } else {
                                            weight &= (0xffff - (1 << 4 * h + z));
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                if (weight == 0 || tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort[k2];
                                } else {

                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        temp = (byte) ((weight >> 4 * h) & 0x0F);
                                        long[] tempData = bicubicInpainting(pixelKernel[h], temp,
                                                emptyArray);
                                        for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                            // Update of the temporary sum
                                            tempSum += (tempData[z] * dataHi[offsetX + z]);
                                        }
                                        if (temp > 0) {
                                            weightVert |= (1 << h);
                                        } else {
                                            weightVert &= (0x0F - (1 << h));
                                        }
                                        sumArray[h] = ((tempSum + round) >> precisionBits);
                                    }

                                    long[] tempData = bicubicInpainting(sumArray, weightVert,
                                            emptyArray);
                                    weight = 0;
                                    weightVert = 0;
                                    // Vertical sum update
                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Update of the temporary sum
                                        sum += tempData[h] * dataVi[offsetY + h];
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
                            if (fracx == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracx = AVOID_OVERFLOW;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracy = AVOID_OVERFLOW;
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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort[k2];
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            } else {
                final long[][] pixelKernel = new long[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
                long[] sumArray = new long[KERNEL_LINE_DIM];
                long[] emptyArray = new long[KERNEL_LINE_DIM];

                short weight = 0;
                byte weightVert = 0;
                byte temp = 0;
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

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

                    int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
                    int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort[k2];
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

                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int pos = posx + posy;

                        if (!roiBounds.contains(x0, y0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort[k2];
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                int result = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                    for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride];

                                        tmpROI += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0) & 0xff;

                                        if (!noData.contains((short) pixelKernel[h][z])) {
                                            weight |= (1 << (4 * h + z));
                                        } else {
                                            weight &= (0xffff - (1 << 4 * h + z));
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                if (weight == 0 || tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort[k2];
                                } else {

                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        temp = (byte) ((weight >> 4 * h) & 0x0F);
                                        long[] tempData = bicubicInpainting(pixelKernel[h], temp,
                                                emptyArray);
                                        for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                            // Update of the temporary sum
                                            tempSum += (tempData[z] * dataHi[offsetX + z]);
                                        }
                                        if (temp > 0) {
                                            weightVert |= (1 << h);
                                        } else {
                                            weightVert &= (0x0F - (1 << h));
                                        }
                                        sumArray[h] = ((tempSum + round) >> precisionBits);
                                    }

                                    long[] tempData = bicubicInpainting(sumArray, weightVert,
                                            emptyArray);
                                    weight = 0;
                                    weightVert = 0;
                                    // Vertical sum update
                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Update of the temporary sum
                                        sum += tempData[h] * dataVi[offsetY + h];
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
                            if (fracx == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracx = AVOID_OVERFLOW;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracy = AVOID_OVERFLOW;
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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort[k2];
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
            int srcRectY, RasterAccessor dst, RasterAccessor roi, RandomIter roiIter) {

        final float src_rect_x1 = src.getX();
        final float src_rect_y1 = src.getY();
        final float src_rect_x2 = src_rect_x1 + src.getWidth();
        final float src_rect_y2 = src_rect_y1 + src.getHeight();

        final float src_rect_x11 = src_rect_x1 + 1;
        final float src_rect_y11 = src_rect_y1 + 1;
        final float src_rect_x22 = src_rect_x2 - 2;
        final float src_rect_y22 = src_rect_y2 - 2;

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

        if (caseA) {
            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

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
                    if ((s_ix >= src_rect_x11) && (s_ix < (src_rect_x22))
                            && (s_iy >= (src_rect_y11)) && (s_iy < (src_rect_y22))) {
                        
                        // X and Y offset initialization
                        int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        int posx = (s_ix - srcRectX) * srcPixelStride;
                        int posy = (s_iy - srcRectY) * srcScanlineStride;

                        int pos = posx + posy;
                        
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            long sum = 0;

                            int result = 0;

                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                // Row temporary sum initialization
                                long temp = 0;
                                for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                    // Selection of one pixel
                                    int pixelValue = srcDataArrays[k2][pos + (z - 1)
                                            * srcPixelStride + (h - 1) * srcScanlineStride
                                            + bandOffsets[k2]];
                                    // Update of the temporary sum
                                    temp += (pixelValue * dataHi[offsetX + z]);
                                }
                                // Vertical sum update
                                sum += ((temp + round) >> precisionBits) * dataVi[offsetY + h];
                            }
                            // Interpolation
                            result = (int) ((sum + round) >> precisionBits);

                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = result;
                        }
                    } else if (setDestinationNoData) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt[k2];
                        }

                    }
                    // walk
                    if (fracx < fracdx1) {
                        s_ix += incx;
                        fracx += fracdx;
                        if (fracx == OVERFLOW) {
                            // Avoid overflow in the interpolation table
                            fracx = AVOID_OVERFLOW;
                        }
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                        if (fracy == OVERFLOW) {
                            // Avoid overflow in the interpolation table
                            fracy = AVOID_OVERFLOW;
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
                    dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

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

                    int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
                    int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt[k2];
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;

                        // If roiAccessor is present, the y position on the roi image is calculated
                        final int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt[k2];
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                final int[][] pixelKernel = new int[KERNEL_LINE_DIM][KERNEL_LINE_DIM];

                                int result = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                    for (int z = 0; z < KERNEL_LINE_DIM; z++) {
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
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt[k2];
                                } else {
                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                            // Update of the temporary sum
                                            tempSum += (pixelKernel[h][z] * dataHi[offsetX + z]);
                                        }
                                        // Vertical sum update
                                        sum += ((tempSum + round) >> precisionBits)
                                                * dataVi[offsetY + h];
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
                            if (fracx == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracx = AVOID_OVERFLOW;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracy = AVOID_OVERFLOW;
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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt[k2];
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
                    dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

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

                    int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
                    int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt[k2];
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;

                        // PixelPositions
                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (!roiBounds.contains(x0, y0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt[k2];
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                final int[][] pixelKernel = new int[KERNEL_LINE_DIM][KERNEL_LINE_DIM];

                                int result = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                    for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride];
                                        tmpROI += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0) & 0xff;
                                    }
                                }

                                // Control if the 16 pixel are outside the ROI
                                if (tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt[k2];
                                } else {
                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                            // Update of the temporary sum
                                            tempSum += (pixelKernel[h][z] * dataHi[offsetX + z]);
                                        }
                                        // Vertical sum update
                                        sum += ((tempSum + round) >> precisionBits)
                                                * dataVi[offsetY + h];
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
                            if (fracx == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracx = AVOID_OVERFLOW;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracy = AVOID_OVERFLOW;
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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt[k2];
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        } else if (caseC) {
            final long[][] pixelKernel = new long[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
            long[] sumArray = new long[KERNEL_LINE_DIM];
            long[] emptyArray = new long[KERNEL_LINE_DIM];

            short weight = 0;
            byte weightVert = 0;
            byte temp = 0;
            // Row temporary sum initialization
            long tempSum = 0;
            long sum = 0;
            // final result initialization
            int result = 0;
            // initial x value definition
            final double dst_min_x_d = dst_min_x + HALF_PIXEL;
            // Band data array creation
            int[] bandDataArray;

            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x_d, y + HALF_PIXEL);

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
                    if ((s_ix >= src_rect_x11) && (s_ix < (src_rect_x22))
                            && (s_iy >= (src_rect_y11)) && (s_iy < (src_rect_y22))) {
                        
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;
                        
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            bandDataArray = srcDataArrays[k2];
                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            // and check if every kernel pixel is a No Data
                            for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                    // Selection of one pixel
                                    pixelKernel[h][z] = bandDataArray[pos + (z - 1)
                                            * srcPixelStride + (h - 1) * srcScanlineStride];
                                    if (!noData.contains((int) pixelKernel[h][z])) {
                                        weight |= (1 << (4 * h + z));
                                    } else {
                                        weight &= (0xffff - (1 << 4 * h + z));
                                    }
                                }
                                temp = (byte) ((weight >> 4 * h) & 0x0F);
                                long[] tempData = bicubicInpainting(pixelKernel[h], temp,
                                        emptyArray);

                                tempSum = tempData[0] * dataHi[offsetX] + tempData[1]
                                        * dataHi[offsetX + 1] + tempData[2] * dataHi[offsetX + 2]
                                        + tempData[3] * dataHi[offsetX + 3];

                                if (temp > 0) {
                                    weightVert |= (1 << h);
                                } else {
                                    weightVert &= (0x0F - (1 << h));
                                }
                                sumArray[h] = ((tempSum + round) >> precisionBits);

                            }

                            // Control if the 16 pixel are all No Data
                            if (weight == 0) {
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt[k2];
                            } else {
                                long[] tempData = bicubicInpainting(sumArray, weightVert,
                                        emptyArray);
                                weight = 0;
                                weightVert = 0;
                                // Vertical sum update
                                sum = tempData[0] * dataVi[offsetY] + tempData[1]
                                        * dataVi[offsetY + 1] + tempData[2] * dataVi[offsetY + 2]
                                        + tempData[3] * dataVi[offsetY + 3];

                                // Interpolation
                                result = (int) ((sum + round) >> precisionBits);
                                sum = 0;
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = result;
                            }
                        }
                    } else if (setDestinationNoData) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt[k2];
                        }
                    }

                    // walk
                    if (fracx < fracdx1) {
                        s_ix += incx;
                        fracx += fracdx;
                        if (fracx == OVERFLOW) {
                            // Avoid overflow in the interpolation table
                            fracx = AVOID_OVERFLOW;
                        }
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                        if (fracy == OVERFLOW) {
                            // Avoid overflow in the interpolation table
                            fracy = AVOID_OVERFLOW;
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
                final long[][] pixelKernel = new long[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
                long[] sumArray = new long[KERNEL_LINE_DIM];
                long[] emptyArray = new long[KERNEL_LINE_DIM];

                short weight = 0;
                byte weightVert = 0;
                byte temp = 0;
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

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

                    int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
                    int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt[k2];
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

                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int pos = posx + posy;

                        if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt[k2];
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                int result = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                    for (int z = 0; z < KERNEL_LINE_DIM; z++) {
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
                                            weight |= (1 << (4 * h + z));
                                        } else {
                                            weight &= (0xffff - (1 << 4 * h + z));
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                if (weight == 0 || tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt[k2];
                                } else {

                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        temp = (byte) ((weight >> 4 * h) & 0x0F);
                                        long[] tempData = bicubicInpainting(pixelKernel[h], temp,
                                                emptyArray);
                                        for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                            // Update of the temporary sum
                                            tempSum += (tempData[z] * dataHi[offsetX + z]);
                                        }
                                        if (temp > 0) {
                                            weightVert |= (1 << h);
                                        } else {
                                            weightVert &= (0x0F - (1 << h));
                                        }
                                        sumArray[h] = ((tempSum + round) >> precisionBits);
                                    }

                                    long[] tempData = bicubicInpainting(sumArray, weightVert,
                                            emptyArray);
                                    weight = 0;
                                    weightVert = 0;
                                    // Vertical sum update
                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Update of the temporary sum
                                        sum += tempData[h] * dataVi[offsetY + h];
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
                            if (fracx == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracx = AVOID_OVERFLOW;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracy = AVOID_OVERFLOW;
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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt[k2];
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            } else {
                final long[][] pixelKernel = new long[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
                long[] sumArray = new long[KERNEL_LINE_DIM];
                long[] emptyArray = new long[KERNEL_LINE_DIM];

                short weight = 0;
                byte weightVert = 0;
                byte temp = 0;
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

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

                    int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
                    int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt[k2];
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

                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int pos = posx + posy;

                        if (!roiBounds.contains(x0, y0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt[k2];
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                long sum = 0;

                                int result = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                    for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride];

                                        tmpROI += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0) & 0xff;

                                        if (!noData.contains((int) pixelKernel[h][z])) {
                                            weight |= (1 << (4 * h + z));
                                        } else {
                                            weight &= (0xffff - (1 << 4 * h + z));
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                if (weight == 0 || tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt[k2];
                                } else {

                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Row temporary sum initialization
                                        long tempSum = 0;
                                        temp = (byte) ((weight >> 4 * h) & 0x0F);
                                        long[] tempData = bicubicInpainting(pixelKernel[h], temp,
                                                emptyArray);
                                        for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                            // Update of the temporary sum
                                            tempSum += (tempData[z] * dataHi[offsetX + z]);
                                        }
                                        if (temp > 0) {
                                            weightVert |= (1 << h);
                                        } else {
                                            weightVert &= (0x0F - (1 << h));
                                        }
                                        sumArray[h] = ((tempSum + round) >> precisionBits);
                                    }

                                    long[] tempData = bicubicInpainting(sumArray, weightVert,
                                            emptyArray);
                                    weight = 0;
                                    weightVert = 0;
                                    // Vertical sum update
                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Update of the temporary sum
                                        sum += tempData[h] * dataVi[offsetY + h];
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
                            if (fracx == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracx = AVOID_OVERFLOW;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracy = AVOID_OVERFLOW;
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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt[k2];
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
            int srcRectY, RasterAccessor dst, RasterAccessor roi, RandomIter roiIter) {

        final float src_rect_x1 = src.getX();
        final float src_rect_y1 = src.getY();
        final float src_rect_x2 = src_rect_x1 + src.getWidth();
        final float src_rect_y2 = src_rect_y1 + src.getHeight();

        final float src_rect_x11 = src_rect_x1 + 1;
        final float src_rect_y11 = src_rect_y1 + 1;
        final float src_rect_x22 = src_rect_x2 - 2;
        final float src_rect_y22 = src_rect_y2 - 2;

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

        if (caseA) {
            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

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

                    if ((s_ix >= src_rect_x11) && (s_ix < (src_rect_x22))
                            && (s_iy >= (src_rect_y11)) && (s_iy < (src_rect_y22))) {
                        
                        // X and Y offset initialization
                        int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        int posx = (s_ix - srcRectX) * srcPixelStride;
                        int posy = (s_iy - srcRectY) * srcScanlineStride;

                        int pos = posx + posy;
                        
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            float sum = 0;

                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                // Row temporary sum initialization
                                float temp = 0;
                                for (int z = 0; z < KERNEL_LINE_DIM; z++) {
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
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat[k2];
                        }

                    }
                    // walk
                    if (fracx < fracdx1) {
                        s_ix += incx;
                        fracx += fracdx;
                        if (fracx == OVERFLOW) {
                            // Avoid overflow in the interpolation table
                            fracx = AVOID_OVERFLOW;
                        }
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                        if (fracy == OVERFLOW) {
                            // Avoid overflow in the interpolation table
                            fracy = AVOID_OVERFLOW;
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
                    dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

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

                    int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
                    int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat[k2];
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;

                        // If roiAccessor is present, the y position on the roi image is calculated
                        final int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat[k2];
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                float sum = 0;

                                final float[][] pixelKernel = new float[KERNEL_LINE_DIM][KERNEL_LINE_DIM];

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                    for (int z = 0; z < KERNEL_LINE_DIM; z++) {
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
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat[k2];
                                } else {
                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Row temporary sum initialization
                                        float tempSum = 0;
                                        for (int z = 0; z < KERNEL_LINE_DIM; z++) {
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
                            if (fracx == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracx = AVOID_OVERFLOW;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracy = AVOID_OVERFLOW;
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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat[k2];
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
                    dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

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

                    int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
                    int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat[k2];
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;

                        // PixelPositions
                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (!roiBounds.contains(x0, y0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat[k2];
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                float sum = 0;

                                final float[][] pixelKernel = new float[KERNEL_LINE_DIM][KERNEL_LINE_DIM];

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                    for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride];
                                        tmpROI += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0) & 0xff;
                                    }
                                }

                                // Control if the 16 pixel are outside the ROI
                                if (tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat[k2];
                                } else {
                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Row temporary sum initialization
                                        float tempSum = 0;
                                        for (int z = 0; z < KERNEL_LINE_DIM; z++) {
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
                            if (fracx == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracx = AVOID_OVERFLOW;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracy = AVOID_OVERFLOW;
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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat[k2];
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        } else if (caseC) {
            final double[][] pixelKernel = new double[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
            double[] sumArray = new double[KERNEL_LINE_DIM];
            double[] emptyArray = new double[KERNEL_LINE_DIM];

            short weight = 0;
            byte weightVert = 0;
            byte temp = 0;
            // Row temporary sum initialization
            double tempSum = 0;
            double sum = 0;
            // initial x value definition
            final double dst_min_x_d = dst_min_x + HALF_PIXEL;
            // Band data array creation
            float[] bandDataArray;

            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x_d, y + HALF_PIXEL);

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

                    if ((s_ix >= src_rect_x11) && (s_ix < (src_rect_x22))
                            && (s_iy >= (src_rect_y11)) && (s_iy < (src_rect_y22))) {
                        
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;
                        
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            bandDataArray = srcDataArrays[k2];
                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            // and check if every kernel pixel is a No Data
                            for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                    // Selection of one pixel
                                    float value = bandDataArray[pos + (z - 1) * srcPixelStride
                                            + (h - 1) * srcScanlineStride];
                                    pixelKernel[h][z] = value;

                                    if (!noData.contains(value)) {
                                        weight |= (1 << (4 * h + z));
                                    } else {
                                        weight &= (0xffff - (1 << 4 * h + z));
                                    }
                                }
                                temp = (byte) ((weight >> 4 * h) & 0x0F);
                                double[] tempData = bicubicInpaintingDouble(pixelKernel[h], temp,
                                        emptyArray);

                                tempSum = tempData[0] * dataHf[offsetX] + tempData[1]
                                        * dataHf[offsetX + 1] + tempData[2] * dataHf[offsetX + 2]
                                        + tempData[3] * dataHf[offsetX + 3];

                                if (temp > 0) {
                                    weightVert |= (1 << h);
                                } else {
                                    weightVert &= (0x0F - (1 << h));
                                }
                                sumArray[h] = tempSum;

                            }

                            // Control if the 16 pixel are all No Data
                            if (weight == 0) {
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat[k2];
                            } else {
                                double[] tempData = bicubicInpaintingDouble(sumArray, weightVert,
                                        emptyArray);
                                weight = 0;
                                weightVert = 0;
                                // Vertical sum update
                                sum = tempData[0] * dataVf[offsetY] + tempData[1]
                                        * dataVf[offsetY + 1] + tempData[2] * dataVf[offsetY + 2]
                                        + tempData[3] * dataVf[offsetY + 3];

                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (float) sum;
                                // reset of the sum value
                                sum = 0;
                            }
                        }
                    } else if (setDestinationNoData) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat[k2];
                        }
                    }

                    // walk
                    if (fracx < fracdx1) {
                        s_ix += incx;
                        fracx += fracdx;
                        if (fracx == OVERFLOW) {
                            // Avoid overflow in the interpolation table
                            fracx = AVOID_OVERFLOW;
                        }
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                        if (fracy == OVERFLOW) {
                            // Avoid overflow in the interpolation table
                            fracy = AVOID_OVERFLOW;
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
                final double[][] pixelKernel = new double[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
                double[] sumArray = new double[KERNEL_LINE_DIM];
                double[] emptyArray = new double[KERNEL_LINE_DIM];

                short weight = 0;
                byte weightVert = 0;
                byte temp = 0;
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

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

                    int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
                    int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat[k2];
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

                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int pos = posx + posy;

                        if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat[k2];
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                double sum = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                    for (int z = 0; z < KERNEL_LINE_DIM; z++) {
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

                                        if (!noData.contains(value)) {
                                            weight |= (1 << (4 * h + z));
                                        } else {
                                            weight &= (0xffff - (1 << 4 * h + z));
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                if (weight == 0 || tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat[k2];
                                } else {

                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Row temporary sum initialization
                                        double tempSum = 0;
                                        temp = (byte) ((weight >> 4 * h) & 0x0F);
                                        double[] tempData = bicubicInpaintingDouble(pixelKernel[h],
                                                temp, emptyArray);
                                        for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                            // Update of the temporary sum
                                            tempSum += (tempData[z] * dataHf[offsetX + z]);
                                        }
                                        if (temp > 0) {
                                            weightVert |= (1 << h);
                                        } else {
                                            weightVert &= (0x0F - (1 << h));
                                        }
                                        sumArray[h] = tempSum;
                                    }

                                    double[] tempData = bicubicInpaintingDouble(sumArray,
                                            weightVert, emptyArray);
                                    weight = 0;
                                    weightVert = 0;
                                    // Vertical sum update
                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
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
                            if (fracx == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracx = AVOID_OVERFLOW;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracy = AVOID_OVERFLOW;
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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat[k2];
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            } else {
                final double[][] pixelKernel = new double[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
                double[] sumArray = new double[KERNEL_LINE_DIM];
                double[] emptyArray = new double[KERNEL_LINE_DIM];

                short weight = 0;
                byte weightVert = 0;
                byte temp = 0;
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

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

                    int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
                    int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat[k2];
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

                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int pos = posx + posy;

                        if (!roiBounds.contains(x0, y0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat[k2];
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                double sum = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                    for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                        // Selection of one pixel
                                        float value = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride];
                                        pixelKernel[h][z] = value;

                                        tmpROI += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0) & 0xff;

                                        if (!noData.contains(value)) {
                                            weight |= (1 << (4 * h + z));
                                        } else {
                                            weight &= (0xffff - (1 << 4 * h + z));
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                // Control if the 16 pixel are outside the ROI
                                if (weight == 0 || tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat[k2];
                                } else {

                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Row temporary sum initialization
                                        double tempSum = 0;
                                        temp = (byte) ((weight >> 4 * h) & 0x0F);
                                        double[] tempData = bicubicInpaintingDouble(pixelKernel[h],
                                                temp, emptyArray);
                                        for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                            // Update of the temporary sum
                                            tempSum += (tempData[z] * dataHf[offsetX + z]);
                                        }
                                        if (temp > 0) {
                                            weightVert |= (1 << h);
                                        } else {
                                            weightVert &= (0x0F - (1 << h));
                                        }
                                        sumArray[h] = tempSum;
                                    }

                                    double[] tempData = bicubicInpaintingDouble(sumArray,
                                            weightVert, emptyArray);
                                    weight = 0;
                                    weightVert = 0;
                                    // Vertical sum update
                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
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
                            if (fracx == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracx = AVOID_OVERFLOW;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracy = AVOID_OVERFLOW;
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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat[k2];
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
            int srcRectY, RasterAccessor dst, RasterAccessor roi, RandomIter roiIter) {

        final float src_rect_x1 = src.getX();
        final float src_rect_y1 = src.getY();
        final float src_rect_x2 = src_rect_x1 + src.getWidth();
        final float src_rect_y2 = src_rect_y1 + src.getHeight();

        final float src_rect_x11 = src_rect_x1 + 1;
        final float src_rect_y11 = src_rect_y1 + 1;
        final float src_rect_x22 = src_rect_x2 - 2;
        final float src_rect_y22 = src_rect_y2 - 2;

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

        if (caseA) {
            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                double s_x = src_pt.getX();
                double s_y = src_pt.getY();

                s_x -= 0.5;
                s_y -= 0.5;

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix * 1.0d;
                fracy = s_y - s_iy * 1.0d;

                for (int x = dst_min_x; x < dst_max_x; x++) {
                    
                    if ((s_ix >= src_rect_x11) && (s_ix < (src_rect_x22))
                            && (s_iy >= (src_rect_y11)) && (s_iy < (src_rect_y22))) {
                        
                        // X and Y offset initialization
                        int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        int posx = (s_ix - srcRectX) * srcPixelStride;
                        int posy = (s_iy - srcRectY) * srcScanlineStride;

                        int pos = posx + posy;
                        
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            double sum = 0;

                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                // Row temporary sum initialization
                                double temp = 0;
                                for (int z = 0; z < KERNEL_LINE_DIM; z++) {
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
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble[k2];
                        }

                    }
                    // walk
                    if (fracx < fracdx1) {
                        s_ix += incx;
                        fracx += fracdx;
                        if (fracx == OVERFLOW) {
                            // Avoid overflow in the interpolation table
                            fracx = AVOID_OVERFLOW;
                        }
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                        if (fracy == OVERFLOW) {
                            // Avoid overflow in the interpolation table
                            fracy = AVOID_OVERFLOW;
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
                    dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    double s_x = src_pt.getX();
                    double s_y = src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
                    int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble[k2];
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;

                        // If roiAccessor is present, the y position on the roi image is calculated
                        final int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        final int baseIndex = (posx / dst_num_bands) + posyROI;

                        if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble[k2];
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                double sum = 0;

                                final double[][] pixelKernel = new double[KERNEL_LINE_DIM][KERNEL_LINE_DIM];

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                    for (int z = 0; z < KERNEL_LINE_DIM; z++) {
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
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble[k2];
                                } else {
                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Row temporary sum initialization
                                        double tempSum = 0;
                                        for (int z = 0; z < KERNEL_LINE_DIM; z++) {
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
                            if (fracx == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracx = AVOID_OVERFLOW;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracy = AVOID_OVERFLOW;
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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble[k2];
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
                    dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    double s_x = src_pt.getX();
                    double s_y = src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
                    int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble[k2];
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;

                        // PixelPositions
                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (!roiBounds.contains(x0, y0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble[k2];
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                double sum = 0;

                                final double[][] pixelKernel = new double[KERNEL_LINE_DIM][KERNEL_LINE_DIM];

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                    for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride];
                                        tmpROI += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0) & 0xff;
                                    }
                                }

                                // Control if the 16 pixel are outside the ROI
                                if (tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble[k2];
                                } else {
                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Row temporary sum initialization
                                        double tempSum = 0;
                                        for (int z = 0; z < KERNEL_LINE_DIM; z++) {
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
                            if (fracx == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracx = AVOID_OVERFLOW;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracy = AVOID_OVERFLOW;
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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble[k2];
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        } else if (caseC) {
            final double[][] pixelKernel = new double[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
            double[] sumArray = new double[KERNEL_LINE_DIM];
            double[] emptyArray = new double[KERNEL_LINE_DIM];

            short weight = 0;
            byte weightVert = 0;
            byte temp = 0;
            // Row temporary sum initialization
            double tempSum = 0;
            double sum = 0;
            // initial x value definition
            final double dst_min_x_d = dst_min_x + HALF_PIXEL;
            // Band data array creation
            double[] bandDataArray;

            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x_d, y + HALF_PIXEL);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                double s_x = src_pt.getX();
                double s_y = src_pt.getY();

                s_x -= 0.5;
                s_y -= 0.5;

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix * 1.0d;
                fracy = s_y - s_iy * 1.0d;

                for (int x = dst_min_x; x < dst_max_x; x++) {
                    if ((s_ix >= src_rect_x11) && (s_ix < (src_rect_x22))
                            && (s_iy >= (src_rect_y11)) && (s_iy < (src_rect_y22))) {
                        
                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int posx = (s_ix - srcRectX) * srcPixelStride;
                        final int posy = (s_iy - srcRectY) * srcScanlineStride;

                        final int pos = posx + posy;
                        
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            bandDataArray = srcDataArrays[k2];
                            // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                            // and check if every kernel pixel is a No Data
                            for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                    // Selection of one pixel
                                    double value = bandDataArray[pos + (z - 1) * srcPixelStride
                                            + (h - 1) * srcScanlineStride];
                                    pixelKernel[h][z] = value;

                                    if (!noData.contains(value)) {
                                        weight |= (1 << (4 * h + z));
                                    } else {
                                        weight &= (0xffff - (1 << 4 * h + z));
                                    }
                                }
                                temp = (byte) ((weight >> 4 * h) & 0x0F);
                                double[] tempData = bicubicInpaintingDouble(pixelKernel[h], temp,
                                        emptyArray);

                                tempSum = tempData[0] * dataHd[offsetX] + tempData[1]
                                        * dataHd[offsetX + 1] + tempData[2] * dataHd[offsetX + 2]
                                        + tempData[3] * dataHd[offsetX + 3];

                                if (temp > 0) {
                                    weightVert |= (1 << h);
                                } else {
                                    weightVert &= (0x0F - (1 << h));
                                }
                                sumArray[h] = tempSum;

                            }

                            // Control if the 16 pixel are all No Data
                            if (weight == 0) {
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble[k2];
                            } else {
                                double[] tempData = bicubicInpaintingDouble(sumArray, weightVert,
                                        emptyArray);
                                weight = 0;
                                weightVert = 0;
                                // Vertical sum update
                                sum = tempData[0] * dataVd[offsetY] + tempData[1]
                                        * dataVd[offsetY + 1] + tempData[2] * dataVd[offsetY + 2]
                                        + tempData[3] * dataVd[offsetY + 3];

                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = sum;
                                // result reset
                                sum = 0;
                            }
                        }
                    } else if (setDestinationNoData) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++) {
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble[k2];
                        }
                    }

                    // walk
                    if (fracx < fracdx1) {
                        s_ix += incx;
                        fracx += fracdx;
                        if (fracx == OVERFLOW) {
                            // Avoid overflow in the interpolation table
                            fracx = AVOID_OVERFLOW;
                        }
                    } else {
                        s_ix += incx1;
                        fracx -= fracdx1;
                    }

                    if (fracy < fracdy1) {
                        s_iy += incy;
                        fracy += fracdy;
                        if (fracy == OVERFLOW) {
                            // Avoid overflow in the interpolation table
                            fracy = AVOID_OVERFLOW;
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
                final double[][] pixelKernel = new double[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
                double[] sumArray = new double[KERNEL_LINE_DIM];
                double[] emptyArray = new double[KERNEL_LINE_DIM];

                short weight = 0;
                byte weightVert = 0;
                byte temp = 0;
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    double s_x = src_pt.getX();
                    double s_y = src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
                    int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble[k2];
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

                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int pos = posx + posy;

                        if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble[k2];
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                double sum = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                    for (int z = 0; z < KERNEL_LINE_DIM; z++) {
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

                                        if (!noData.contains(value)) {
                                            weight |= (1 << (4 * h + z));
                                        } else {
                                            weight &= (0xffff - (1 << 4 * h + z));
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                if (weight == 0 || tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble[k2];
                                } else {

                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Row temporary sum initialization
                                        double tempSum = 0;
                                        temp = (byte) ((weight >> 4 * h) & 0x0F);
                                        double[] tempData = bicubicInpaintingDouble(pixelKernel[h],
                                                temp, emptyArray);
                                        for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                            // Update of the temporary sum
                                            tempSum += (tempData[z] * dataHd[offsetX + z]);
                                        }
                                        if (temp > 0) {
                                            weightVert |= (1 << h);
                                        } else {
                                            weightVert &= (0x0F - (1 << h));
                                        }
                                        sumArray[h] = tempSum;
                                    }

                                    double[] tempData = bicubicInpaintingDouble(sumArray,
                                            weightVert, emptyArray);
                                    weight = 0;
                                    weightVert = 0;
                                    // Vertical sum update
                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
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
                            if (fracx == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracx = AVOID_OVERFLOW;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracy = AVOID_OVERFLOW;
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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble[k2];
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            } else {
                final double[][] pixelKernel = new double[KERNEL_LINE_DIM][KERNEL_LINE_DIM];
                double[] sumArray = new double[KERNEL_LINE_DIM];
                double[] emptyArray = new double[KERNEL_LINE_DIM];

                short weight = 0;
                byte weightVert = 0;
                byte temp = 0;
                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + HALF_PIXEL, y + HALF_PIXEL);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    double s_x = src_pt.getX();
                    double s_y = src_pt.getY();

                    s_x -= 0.5;
                    s_y -= 0.5;

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix * 1.0d;
                    fracy = s_y - s_iy * 1.0d;

                    int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
                    int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble[k2];
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

                        // X and Y offset initialization
                        final int offsetX = KERNEL_LINE_DIM * (int) (shift * fracx);
                        final int offsetY = KERNEL_LINE_DIM * (int) (shift * fracy);

                        final int pos = posx + posy;

                        if (!roiBounds.contains(x0, y0)) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble[k2];
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                double sum = 0;

                                int tmpROI = 0;

                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by 1 on the y axis.
                                for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                    for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                        // Selection of one pixel
                                        double value = srcDataArrays[k2][pos + (z - 1)
                                                * srcPixelStride + (h - 1) * srcScanlineStride];
                                        pixelKernel[h][z] = value;

                                        tmpROI += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0) & 0xff;

                                        if (!noData.contains(value)) {
                                            weight |= (1 << (4 * h + z));
                                        } else {
                                            weight &= (0xffff - (1 << 4 * h + z));
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                // Control if the 16 pixel are outside the ROI
                                if (weight == 0 || tmpROI == 0) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble[k2];
                                } else {

                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
                                        // Row temporary sum initialization
                                        double tempSum = 0;
                                        temp = (byte) ((weight >> 4 * h) & 0x0F);
                                        double[] tempData = bicubicInpaintingDouble(pixelKernel[h],
                                                temp, emptyArray);
                                        for (int z = 0; z < KERNEL_LINE_DIM; z++) {
                                            // Update of the temporary sum
                                            tempSum += (tempData[z] * dataHd[offsetX + z]);
                                        }
                                        if (temp > 0) {
                                            weightVert |= (1 << h);
                                        } else {
                                            weightVert &= (0x0F - (1 << h));
                                        }
                                        sumArray[h] = tempSum;
                                    }

                                    double[] tempData = bicubicInpaintingDouble(sumArray,
                                            weightVert, emptyArray);
                                    weight = 0;
                                    weightVert = 0;
                                    // Vertical sum update
                                    for (int h = 0; h < KERNEL_LINE_DIM; h++) {
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
                            if (fracx == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracx = AVOID_OVERFLOW;
                            }
                        } else {
                            s_ix += incx1;
                            fracx -= fracdx1;
                        }

                        if (fracy < fracdy1) {
                            s_iy += incy;
                            fracy += fracdy;
                            if (fracy == OVERFLOW) {
                                // Avoid overflow in the interpolation table
                                fracy = AVOID_OVERFLOW;
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
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble[k2];
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        }
    }

    private long[] bicubicInpainting(long[] array, short weightSum, long[] emptyArray) {
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

    // This method is used for filling the no data values inside the interpolation kernel with the values of the adjacent pixels
    private double[] bicubicInpaintingDouble(double[] array, short weightSum, double[] emptyArray) {
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
