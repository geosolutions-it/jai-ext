package it.geosolutions.jaiext.scale;

import it.geosolutions.jaiext.interpolators.InterpolationBicubic;

import java.awt.Rectangle;
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

import org.jaitools.numeric.Range;

public class ScaleBicubicOpImage extends ScaleOpImage {

    /** Bicubic interpolator */
    protected InterpolationBicubic interpBN = null;

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

    private final byte[] byteLookupTable = new byte[255];

    public ScaleBicubicOpImage(RenderedImage source, ImageLayout layout, Map configuration,
            BorderExtender extender, Interpolation interp, float scaleX, float scaleY,
            float transX, float transY, boolean useRoiAccessor) {
        super(source, layout, configuration, true, extender, interp, scaleX, scaleY, transX, transY, useRoiAccessor);
        scaleOpInitialization(source, interp);
    }

    private void scaleOpInitialization(RenderedImage source, Interpolation interp) {
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

        SampleModel sm = source.getSampleModel();
        // Source image data Type
        int srcDataType = sm.getDataType();

        // selection of the inverse scale parameters both for the x and y axis
        if (invScaleXRational.num > invScaleXRational.denom) {
            invScaleXInt = invScaleXRational.num / invScaleXRational.denom;
            invScaleXFrac = invScaleXRational.num % invScaleXRational.denom;
        } else {
            invScaleXInt = 0;
            invScaleXFrac = invScaleXRational.num;
        }

        if (invScaleYRational.num > invScaleYRational.denom) {
            invScaleYInt = invScaleYRational.num / invScaleYRational.denom;
            invScaleYFrac = invScaleYRational.num % invScaleYRational.denom;
        } else {
            invScaleYInt = 0;
            invScaleYFrac = invScaleYRational.num;
        }

        // Interpolator settings
        interpolator = interp;

        if (interpolator instanceof InterpolationBicubic) {

            isBicubicNew = true;
            interpBN = (InterpolationBicubic) interpolator;
            this.interp = interpBN;

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

            interpBN.setROIdata(roiBounds, roiIter);
            noData = interpBN.getNoDataRange();
            precisionBits = interpBN.getPrecisionBits();

            if (noData != null) {
                hasNoData = true;
                destinationNoDataDouble = interpBN.getDestinationNoData();
                if ((dataType == DataBuffer.TYPE_FLOAT || dataType == DataBuffer.TYPE_DOUBLE)) {
                    // If the range goes from -Inf to Inf No Data is NaN
                    if (!noData.isPoint() && noData.isMaxInf() && noData.isMinNegInf()) {
                        isRangeNaN = true;
                        // If the range is a positive infinite point isPositiveInf flag is set
                    } else if (noData.isPoint() && noData.isMaxInf() && noData.isMinInf()) {
                        isPositiveInf = true;
                        // If the range is a negative infinite point isNegativeInf flag is set
                    } else if (noData.isPoint() && noData.isMaxNegInf() && noData.isMinNegInf()) {
                        isNegativeInf = true;
                    }
                }
            } else if (hasROI) {
                destinationNoDataDouble = interpBN.getDestinationNoData();
            }
        }
        // subsample bits used for the bilinear and bicubic interpolation
        subsampleBits = interp.getSubsampleBitsH();

        if (precisionBits > 0) {
            round = 1 << (precisionBits - 1);
        }

        // Number of subsample positions
        one = 1 << subsampleBits;

        // Get the width and height and padding of the Interpolation kernel.
        interp_width = interp.getWidth();
        interp_height = interp.getHeight();
        interp_left = interp.getLeftPadding();
        interp_top = interp.getTopPadding();

        // Selection of the destination No Data
        switch (srcDataType) {
        case DataBuffer.TYPE_BYTE:
            destinationNoDataByte = (byte) (((byte) destinationNoDataDouble) & 0xff);

            if (hasNoData) {

                Range<Byte> noDataByte = ((Range<Byte>) noData);

                for (int i = 0; i < byteLookupTable.length; i++) {
                    byte value = (byte) i;
                    if (noDataByte.contains(value)) {
                        byteLookupTable[i] = destinationNoDataByte;
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

    @Override
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        computeRect(sources, dest, destRect, null);
    }

    @Override
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect,
            Raster[] rois) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();
        // Only one source raster is used
        Raster source = sources[0];

        // Get the source rectangle
        Rectangle srcRect = source.getBounds();

        // SRC and destination accessors are used for simplifying calculations
        RasterAccessor srcAccessor = new RasterAccessor(source, srcRect, formatTags[0],
                getSourceImage(0).getColorModel());

        RasterAccessor dstAccessor = new RasterAccessor(dest, destRect, formatTags[1],
                getColorModel());

        // Destination rectangle dimensions
        int dwidth = destRect.width;
        int dheight = destRect.height;
        // From the rasterAccessor are calculated the pixelStride and the scanLineStride
        int srcPixelStride = srcAccessor.getPixelStride();
        int srcScanlineStride = srcAccessor.getScanlineStride();
        // Initialization of the x and y position array
        int[] xpos = new int[dwidth];
        int[] ypos = new int[dheight];

        // ROI support
        int[] yposRoi = null;
        // Scanline stride. It is used as integer because it can return null values
        int roiScanlineStride = 0;
        // Roi rasterAccessor initialization
        RasterAccessor roiAccessor = null;
        // Roi raster initialization
        Raster roi = null;

        // ROI calculation only if the roi raster is present
        if (useRoiAccessor) {
            // Selection of the roi raster
            roi = rois[0];
            // creation of the rasterAccessor
            roiAccessor = new RasterAccessor(roi, srcRect, RasterAccessor.findCompatibleTags(
                    new RenderedImage[] { srcROIImage }, srcROIImage)[0],
                    srcROIImage.getColorModel());
            // ROI scanlinestride
            roiScanlineStride = roiAccessor.getScanlineStride();
            // Initialization of the roi y position array
            yposRoi = new int[dheight];

        }

        // Initialization of the x and y fractional array
        int[] xfracValues = new int[dwidth];
        int[] yfracValues = new int[dheight];

        // Initialization of the x and y fractional array
        float[] xfracValuesFloat = new float[dwidth];
        float[] yfracValuesFloat = new float[dheight];
        // destination data type
        dataType = dest.getSampleModel().getDataType();

        if (dataType < DataBuffer.TYPE_FLOAT) {
            preComputePositionsInt(destRect, srcRect.x, srcRect.y, srcPixelStride,
                    srcScanlineStride, xpos, ypos, xfracValues, yfracValues, roiScanlineStride,
                    yposRoi);
        } else {
            preComputePositionsFloat(destRect, srcRect.x, srcRect.y, srcPixelStride,
                    srcScanlineStride, xpos, ypos, xfracValuesFloat, yfracValuesFloat,
                    roiScanlineStride, yposRoi);
        }

        // This methods differs only for the presence of the roi or if the image is a binary one

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(srcAccessor, destRect, dstAccessor, xpos, ypos, xfracValues, yfracValues,
                    roiAccessor, yposRoi, roiScanlineStride);
            break;
        case DataBuffer.TYPE_USHORT:
            ushortLoop(srcAccessor, destRect, dstAccessor, xpos, ypos, xfracValues, yfracValues,
                    roiAccessor, yposRoi, roiScanlineStride);
            break;
        case DataBuffer.TYPE_SHORT:
            shortLoop(srcAccessor, destRect, dstAccessor, xpos, ypos, xfracValues, yfracValues,
                    roiAccessor, yposRoi, roiScanlineStride);
            break;
        case DataBuffer.TYPE_INT:
            intLoop(srcAccessor, destRect, dstAccessor, xpos, ypos, xfracValues, yfracValues,
                    roiAccessor, yposRoi, roiScanlineStride);
            break;
        case DataBuffer.TYPE_FLOAT:
            floatLoop(srcAccessor, destRect, dstAccessor, xpos, ypos, xfracValuesFloat,
                    yfracValuesFloat, roiAccessor, yposRoi, roiScanlineStride);
            break;
        case DataBuffer.TYPE_DOUBLE:
            doubleLoop(srcAccessor, destRect, dstAccessor, xpos, ypos, xfracValuesFloat,
                    yfracValuesFloat, roiAccessor, yposRoi, roiScanlineStride);
            break;
        }

    }

    private void byteLoop(RasterAccessor src, Rectangle dstRect, RasterAccessor dst, int[] xpos,
            int[] ypos, int[] xfrac, int[] yfrac, RasterAccessor roi, int[] yposRoi,
            int roiScanlineStride) {

        // BandOffsets
        final int srcScanlineStride = src.getScanlineStride();
        final int srcPixelStride = src.getPixelStride();
        final int bandOffsets[] = src.getBandOffsets();
        // Destination rectangle dimensions
        final int dwidth = dstRect.width;
        final int dheight = dstRect.height;
        // Destination image band numbers
        final int dnumBands = dst.getNumBands();
        // Destination bandOffsets, PixelStride and ScanLineStride
        final int dstBandOffsets[] = dst.getBandOffsets();
        final int dstPixelStride = dst.getPixelStride();
        final int dstScanlineStride = dst.getScanlineStride();

        // Destination and source data arrays (for all bands)
        final byte[][] srcDataArrays = src.getByteDataArrays();

        final byte[][] dstDataArrays = dst.getByteDataArrays();

        final byte[] roiDataArray;
        final int roiDataLength;
        if (useRoiAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
        } else {
            roiDataArray = null;
            roiDataLength = 0;
        }

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;
        if (caseA) {
            // for all bands
            for (int k = 0; k < dnumBands; k++) {

                final byte[] srcData = srcDataArrays[k];
                final byte[] dstData = dstDataArrays[k];
                // Line and band Offset initialization
                int dstlineOffset = dstBandOffsets[k];
                int bandOffset = bandOffsets[k];
                // cycle on the y values
                for (int j = 0; j < dheight; j++) {
                    // pixel offset initialization
                    int dstPixelOffset = dstlineOffset;
                    // y position selection
                    int posy = ypos[j] + bandOffset;
                    // Y offset initialization
                    int offsetY = 4 * yfrac[j];

                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        int posx = xpos[i];
                        int pos = posx + posy;

                        long sum = 0;

                        int s = 0;
                        // X offset initialization
                        int offsetX = 4 * xfrac[i];
                        // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                        for (int h = 0; h < 4; h++) {
                            // Row temporary sum initialization
                            long temp = 0;
                            for (int z = 0; z < 4; z++) {
                                // Selection of one pixel
                                int pixelValue = srcData[pos + (z - 1) * srcPixelStride + (h - 1)
                                        * srcScanlineStride] & 0xff;
                                // Update of the temporary sum
                                temp += (pixelValue * (long) dataHi[offsetX + z]);
                            }
                            // Vertical sum update
                            sum += ((temp + round) >> precisionBits) * (long) dataVi[offsetY + h];
                        }
                        // Interpolation
                        s = (int) ((sum + round) >> precisionBits);

                        // Clamp
                        if (s > 255) {
                            s = 255;
                        } else if (s < 0) {
                            s = 0;
                        }

                        // The interpolated value is saved in the destination array
                        dstData[dstPixelOffset] = (byte) (s & 0xff);

                        // destination pixel offset update
                        dstPixelOffset += dstPixelStride;
                    }
                    // destination line offset update
                    dstlineOffset += dstScanlineStride;
                }
            }
        } else {
            if (caseB) {
                if (useRoiAccessor) {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {
                        final byte[] srcData = srcDataArrays[k];
                        final byte[] dstData = dstDataArrays[k];
                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            int posy = ypos[j] + bandOffset;
                            // Y offset initialization
                            int offsetY = 4 * yfrac[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                int pos = posx + posy;

                                int[][] pixelKernel = new int[4][4];

                                final int baseIndex = (posx / dnumBands) + (yposRoi[j]);

                                // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                // Otherwise it takes the related value.
                                if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataByte;
                                } else {
                                    int temp = 0;
                                    // X offset initialization
                                    int offsetX = 4 * xfrac[i];
                                    // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                    // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                    // and by roiscanlinestride on the y axis.
                                    for (int h = 0; h < 4; h++) {
                                        for (int z = 0; z < 4; z++) {
                                            // Selection of one pixel
                                            pixelKernel[h][z] = srcData[pos + (z - 1)
                                                    * srcPixelStride + (h - 1) * srcScanlineStride] & 0xff;
                                            int index = baseIndex - 1 + z + (h - 1)
                                                    * (roiScanlineStride);
                                            if (index < roiDataLength) {
                                                // Update of the weight sum
                                                temp += ((byte) (roiDataArray[index] & 0xff) != 0 ? 1
                                                        : 0);
                                            }
                                        }
                                    }
                                    // Control if the 16 pixel are outside the ROI
                                    if (temp == 0) {
                                        dstData[dstPixelOffset] = destinationNoDataByte;
                                    } else {
                                        long sum = 0;
                                        int s = 0;

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
                                        s = (int) ((sum + round) >> precisionBits);

                                        // Clamp
                                        if (s > 255) {
                                            s = 255;
                                        } else if (s < 0) {
                                            s = 0;
                                        }

                                        dstData[dstPixelOffset] = (byte) (s & 0xff);
                                    }
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {
                        final byte[] srcData = srcDataArrays[k];
                        final byte[] dstData = dstDataArrays[k];
                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            int posy = ypos[j] + bandOffset;
                            // Y offset initialization
                            int offsetY = 4 * yfrac[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                int pos = posx + posy;

                                // PixelPositions
                                int x0 = src.getX() + posx / srcPixelStride;
                                int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                int[][] pixelKernel = new int[4][4];

                                // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                // Otherwise it takes the related value.
                                if (roiBounds.contains(x0, y0)) {

                                    int temp = 0;
                                    // X offset initialization
                                    int offsetX = 4 * xfrac[i];
                                    // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                    // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                    // and by roiscanlinestride on the y axis.
                                    for (int h = 0; h < 4; h++) {
                                        for (int z = 0; z < 4; z++) {
                                            // Selection of one pixel
                                            pixelKernel[h][z] = srcData[pos + (z - 1)
                                                    * srcPixelStride + (h - 1) * srcScanlineStride] & 0xff;
                                            temp += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0) & 0xff;
                                        }
                                    }
                                    // Control if the 16 pixel are outside the ROI
                                    if (temp == 0) {
                                        dstData[dstPixelOffset] = destinationNoDataByte;
                                    } else {
                                        long sum = 0;
                                        int s = 0;

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
                                        s = (int) ((sum + round) >> precisionBits);

                                        // Clamp
                                        if (s > 255) {
                                            s = 255;
                                        } else if (s < 0) {
                                            s = 0;
                                        }

                                        dstData[dstPixelOffset] = (byte) (s & 0xff);
                                    }
                                } else {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataByte;
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }

                }
            } else {
                if (caseC) {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {

                        final byte[] srcData = srcDataArrays[k];
                        final byte[] dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            int posy = ypos[j] + bandOffset;
                            // Y offset initialization
                            int offsetY = 4 * yfrac[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                int pos = posx + posy;

                                long[][] pixelKernel = new long[4][4];
                                int[][] weightArray = new int[4][4];
                                int[] weightArrayVertical = new int[4];

                                // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                // Otherwise it takes the related value.

                                int temp = 0;
                                // X offset initialization
                                int offsetX = 4 * xfrac[i];
                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by roiscanlinestride on the y axis.
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcData[pos + (z - 1) * srcPixelStride
                                                + (h - 1) * srcScanlineStride] & 0xff;

                                        if (byteLookupTable[(int) pixelKernel[h][z]] != destinationNoDataByte) {
                                            temp++;
                                            weightArray[h][z] = 1;
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                if (temp == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataByte;
                                } else {

                                    long[] sumArray = new long[4];

                                    long sum = 0;
                                    int s = 0;

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
                                    s = (int) ((sum + round) >> precisionBits);

                                    // Clamp
                                    if (s > 255) {
                                        s = 255;
                                    } else if (s < 0) {
                                        s = 0;
                                    }

                                    dstData[dstPixelOffset] = (byte) (s & 0xff);
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else if (hasROI && hasNoData) {
                    if (useRoiAccessor) {
                        // for all bands
                        for (int k = 0; k < dnumBands; k++) {

                            final byte[] srcData = srcDataArrays[k];
                            final byte[] dstData = dstDataArrays[k];
                            // Line and band Offset initialization
                            int dstlineOffset = dstBandOffsets[k];
                            int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                int posy = ypos[j] + bandOffset;
                                // Y offset initialization
                                int offsetY = 4 * yfrac[j];
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];

                                    int pos = posx + posy;

                                    long[][] pixelKernel = new long[4][4];
                                    int[][] weightArray = new int[4][4];
                                    int[] weightArrayVertical = new int[4];

                                    // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                    // Otherwise it takes the related value.

                                    final int baseIndex = (posx / dnumBands) + (yposRoi[j]);

                                    // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                    // Otherwise it takes the related value.
                                    if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                                        dstData[dstPixelOffset] = destinationNoDataByte;
                                    } else {
                                        int tempND = 0;
                                        int tempROI = 0;
                                        // X offset initialization
                                        int offsetX = 4 * xfrac[i];
                                        // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                        // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                        // and by roiscanlinestride on the y axis.
                                        for (int h = 0; h < 4; h++) {
                                            for (int z = 0; z < 4; z++) {
                                                // Selection of one pixel
                                                pixelKernel[h][z] = srcData[pos + (z - 1)
                                                        * srcPixelStride + (h - 1)
                                                        * srcScanlineStride] & 0xff;

                                                int index = baseIndex - 1 + z + (h - 1)
                                                        * (roiScanlineStride);
                                                if (index < roiDataLength) {
                                                    // Update of the weight sum
                                                    tempROI += ((byte) (roiDataArray[index] & 0xff) != 0 ? 1
                                                            : 0);
                                                }

                                                if (byteLookupTable[(int) pixelKernel[h][z]] != destinationNoDataByte) {
                                                    tempND++;
                                                    weightArray[h][z] = 1;
                                                }
                                            }
                                        }
                                        // Control if the 16 pixel are outside the ROI
                                        if (tempND == 0 || tempROI == 0) {
                                            dstData[dstPixelOffset] = destinationNoDataByte;
                                        } else {

                                            long[] sumArray = new long[4];

                                            long sum = 0;
                                            int s = 0;

                                            for (int h = 0; h < 4; h++) {
                                                // Row temporary sum initialization
                                                long tempSum = 0;
                                                long[] tempData = bicubicInpainting(pixelKernel[h],
                                                        weightArray[h], null);
                                                for (int z = 0; z < 4; z++) {
                                                    // Update of the temporary sum
                                                    tempSum += (tempData[z] * (long) dataHi[offsetX
                                                            + z]);
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
                                            s = (int) ((sum + round) >> precisionBits);

                                            // Clamp
                                            if (s > 255) {
                                                s = 255;
                                            } else if (s < 0) {
                                                s = 0;
                                            }

                                            dstData[dstPixelOffset] = (byte) (s & 0xff);
                                        }

                                    }
                                    // destination pixel offset update
                                    dstPixelOffset += dstPixelStride;
                                }
                                // destination line offset update
                                dstlineOffset += dstScanlineStride;
                            }
                        }
                    } else {
                        // for all bands
                        for (int k = 0; k < dnumBands; k++) {

                            final byte[] srcData = srcDataArrays[k];
                            final byte[] dstData = dstDataArrays[k];
                            // Line and band Offset initialization
                            int dstlineOffset = dstBandOffsets[k];
                            int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                int posy = ypos[j] + bandOffset;
                                // Y offset initialization
                                int offsetY = 4 * yfrac[j];
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];

                                    // PixelPositions
                                    int x0 = src.getX() + posx / srcPixelStride;
                                    int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {

                                        int pos = posx + posy;

                                        long[][] pixelKernel = new long[4][4];
                                        int[][] weightArray = new int[4][4];
                                        int[] weightArrayVertical = new int[4];

                                        // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                        // Otherwise it takes the related value.

                                        int tempND = 0;
                                        int tempROI = 0;
                                        // X offset initialization
                                        int offsetX = 4 * xfrac[i];
                                        // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                        // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                        // and by roiscanlinestride on the y axis.
                                        for (int h = 0; h < 4; h++) {
                                            for (int z = 0; z < 4; z++) {
                                                // Selection of one pixel
                                                pixelKernel[h][z] = srcData[pos + (z - 1)
                                                        * srcPixelStride + (h - 1)
                                                        * srcScanlineStride] & 0xff;

                                                tempROI += roiIter.getSample(x0 + h - 1,
                                                        y0 + z - 1, 0) & 0xff;

                                                if (byteLookupTable[(int) pixelKernel[h][z]] != destinationNoDataByte) {
                                                    tempND++;
                                                    weightArray[h][z] = 1;
                                                }
                                            }
                                        }

                                        // Control if the 16 pixel are outside the ROI
                                        if (tempND == 0 || tempROI == 0) {
                                            dstData[dstPixelOffset] = destinationNoDataByte;
                                        } else {

                                            long[] sumArray = new long[4];

                                            long sum = 0;
                                            int s = 0;

                                            for (int h = 0; h < 4; h++) {
                                                // Row temporary sum initialization
                                                long tempSum = 0;
                                                long[] tempData = bicubicInpainting(pixelKernel[h],
                                                        weightArray[h], null);
                                                for (int z = 0; z < 4; z++) {
                                                    // Update of the temporary sum
                                                    tempSum += (tempData[z] * (long) dataHi[offsetX
                                                            + z]);
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
                                            s = (int) ((sum + round) >> precisionBits);

                                            // Clamp
                                            if (s > 255) {
                                                s = 255;
                                            } else if (s < 0) {
                                                s = 0;
                                            }

                                            dstData[dstPixelOffset] = (byte) (s & 0xff);
                                        }

                                    } else {
                                        dstData[dstPixelOffset] = destinationNoDataByte;
                                    }

                                    // destination pixel offset update
                                    dstPixelOffset += dstPixelStride;
                                }
                                // destination line offset update
                                dstlineOffset += dstScanlineStride;
                            }
                        }
                    }
                }
            }
        }
    }

    private void ushortLoop(RasterAccessor src, Rectangle dstRect, RasterAccessor dst, int[] xpos,
            int[] ypos, int[] xfrac, int[] yfrac, RasterAccessor roi, int[] yposRoi,
            int roiScanlineStride) {

        // BandOffsets
        final int srcScanlineStride = src.getScanlineStride();
        final int srcPixelStride = src.getPixelStride();
        final int bandOffsets[] = src.getBandOffsets();
        // Destination rectangle dimensions
        final int dwidth = dstRect.width;
        final int dheight = dstRect.height;
        // Destination image band numbers
        final int dnumBands = dst.getNumBands();
        // Destination bandOffsets, PixelStride and ScanLineStride
        final int dstBandOffsets[] = dst.getBandOffsets();
        final int dstPixelStride = dst.getPixelStride();
        final int dstScanlineStride = dst.getScanlineStride();

        // Destination and source data arrays (for all bands)
        final short[][] srcDataArrays = src.getShortDataArrays();

        final short[][] dstDataArrays = dst.getShortDataArrays();

        final byte[] roiDataArray;
        final int roiDataLength;
        if (useRoiAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
        } else {
            roiDataArray = null;
            roiDataLength = 0;
        }

        Range<Short> rangeND = (Range<Short>) noData;

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;
        if (caseA) {
            // for all bands
            for (int k = 0; k < dnumBands; k++) {

                final short[] srcData = srcDataArrays[k];
                final short[] dstData = dstDataArrays[k];
                // Line and band Offset initialization
                int dstlineOffset = dstBandOffsets[k];
                int bandOffset = bandOffsets[k];
                // cycle on the y values
                for (int j = 0; j < dheight; j++) {
                    // pixel offset initialization
                    int dstPixelOffset = dstlineOffset;
                    // y position selection
                    int posy = ypos[j] + bandOffset;
                    // Y offset initialization
                    int offsetY = 4 * yfrac[j];

                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        int posx = xpos[i];
                        int pos = posx + posy;

                        long sum = 0;

                        int s = 0;
                        // X offset initialization
                        int offsetX = 4 * xfrac[i];
                        // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                        for (int h = 0; h < 4; h++) {
                            // Row temporary sum initialization
                            long temp = 0;
                            for (int z = 0; z < 4; z++) {
                                // Selection of one pixel
                                int pixelValue = srcData[pos + (z - 1) * srcPixelStride + (h - 1)
                                        * srcScanlineStride] & 0xffff;
                                // Update of the temporary sum
                                temp += (pixelValue * (long) dataHi[offsetX + z]);
                            }
                            // Vertical sum update
                            sum += ((temp + round) >> precisionBits) * (long) dataVi[offsetY + h];
                        }
                        // Interpolation
                        s = (int) ((sum + round) >> precisionBits);

                        // Clamp
                        if (s > 65536) {
                            s = 65536;
                        } else if (s < 0) {
                            s = 0;
                        }

                        // The interpolated value is saved in the destination array
                        dstData[dstPixelOffset] = (short) (s & 0xffff);

                        // destination pixel offset update
                        dstPixelOffset += dstPixelStride;
                    }
                    // destination line offset update
                    dstlineOffset += dstScanlineStride;
                }
            }
        } else {
            if (caseB) {
                if (useRoiAccessor) {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {
                        final short[] srcData = srcDataArrays[k];
                        final short[] dstData = dstDataArrays[k];
                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            int posy = ypos[j] + bandOffset;
                            // Y offset initialization
                            int offsetY = 4 * yfrac[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                int pos = posx + posy;

                                int[][] pixelKernel = new int[4][4];

                                final int baseIndex = (posx / dnumBands) + (yposRoi[j]);

                                // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                // Otherwise it takes the related value.
                                if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataUShort;
                                } else {
                                    int temp = 0;
                                    // X offset initialization
                                    int offsetX = 4 * xfrac[i];
                                    // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                    // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                    // and by roiscanlinestride on the y axis.
                                    for (int h = 0; h < 4; h++) {
                                        for (int z = 0; z < 4; z++) {
                                            // Selection of one pixel
                                            pixelKernel[h][z] = srcData[pos + (z - 1)
                                                    * srcPixelStride + (h - 1) * srcScanlineStride] & 0xffff;
                                            int index = baseIndex - 1 + z + (h - 1)
                                                    * (roiScanlineStride);
                                            if (index < roiDataLength) {
                                                // Update of the weight sum
                                                temp += ((short) (roiDataArray[index] & 0xffff) != 0 ? 1
                                                        : 0);
                                            }
                                        }
                                    }
                                    // Control if the 16 pixel are outside the ROI
                                    if (temp == 0) {
                                        dstData[dstPixelOffset] = destinationNoDataUShort;
                                    } else {
                                        long sum = 0;
                                        int s = 0;

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
                                        s = (int) ((sum + round) >> precisionBits);

                                        // Clamp
                                        if (s > 65536) {
                                            s = 65536;
                                        } else if (s < 0) {
                                            s = 0;
                                        }

                                        dstData[dstPixelOffset] = (short) (s & 0xffff);
                                    }
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {
                        final short[] srcData = srcDataArrays[k];
                        final short[] dstData = dstDataArrays[k];
                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            int posy = ypos[j] + bandOffset;
                            // Y offset initialization
                            int offsetY = 4 * yfrac[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                int pos = posx + posy;

                                // PixelPositions
                                int x0 = src.getX() + posx / srcPixelStride;
                                int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                int[][] pixelKernel = new int[4][4];

                                // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                // Otherwise it takes the related value.
                                if (roiBounds.contains(x0, y0)) {

                                    int temp = 0;
                                    // X offset initialization
                                    int offsetX = 4 * xfrac[i];
                                    // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                    // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                    // and by roiscanlinestride on the y axis.
                                    for (int h = 0; h < 4; h++) {
                                        for (int z = 0; z < 4; z++) {
                                            // Selection of one pixel
                                            pixelKernel[h][z] = srcData[pos + (z - 1)
                                                    * srcPixelStride + (h - 1) * srcScanlineStride] & 0xffff;
                                            temp += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0) & 0xffff;
                                        }
                                    }
                                    // Control if the 16 pixel are outside the ROI
                                    if (temp == 0) {
                                        dstData[dstPixelOffset] = destinationNoDataUShort;
                                    } else {
                                        long sum = 0;
                                        int s = 0;

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
                                        s = (int) ((sum + round) >> precisionBits);

                                        // Clamp
                                        if (s > 65536) {
                                            s = 65536;
                                        } else if (s < 0) {
                                            s = 0;
                                        }

                                        dstData[dstPixelOffset] = (short) (s & 0xffff);
                                    }
                                } else {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataUShort;
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }

                }
            } else {
                if (caseC) {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {

                        final short[] srcData = srcDataArrays[k];
                        final short[] dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            int posy = ypos[j] + bandOffset;
                            // Y offset initialization
                            int offsetY = 4 * yfrac[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                int pos = posx + posy;

                                long[][] pixelKernel = new long[4][4];
                                int[][] weightArray = new int[4][4];
                                int[] weightArrayVertical = new int[4];

                                // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                // Otherwise it takes the related value.

                                int temp = 0;
                                // X offset initialization
                                int offsetX = 4 * xfrac[i];
                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by roiscanlinestride on the y axis.
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcData[pos + (z - 1) * srcPixelStride
                                                + (h - 1) * srcScanlineStride] & 0xffff;

                                        if (!rangeND.contains((short) pixelKernel[h][z])) {
                                            temp++;
                                            weightArray[h][z] = 1;
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                if (temp == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataUShort;
                                } else {

                                    long[] sumArray = new long[4];

                                    long sum = 0;
                                    int s = 0;

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
                                    s = (int) ((sum + round) >> precisionBits);

                                    // Clamp
                                    if (s > 65536) {
                                        s = 65536;
                                    } else if (s < 0) {
                                        s = 0;
                                    }

                                    dstData[dstPixelOffset] = (short) (s & 0xffff);
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else if (hasROI && hasNoData) {
                    if (useRoiAccessor) {
                        // for all bands
                        for (int k = 0; k < dnumBands; k++) {

                            final short[] srcData = srcDataArrays[k];
                            final short[] dstData = dstDataArrays[k];
                            // Line and band Offset initialization
                            int dstlineOffset = dstBandOffsets[k];
                            int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                int posy = ypos[j] + bandOffset;
                                // Y offset initialization
                                int offsetY = 4 * yfrac[j];
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];

                                    int pos = posx + posy;

                                    long[][] pixelKernel = new long[4][4];
                                    int[][] weightArray = new int[4][4];
                                    int[] weightArrayVertical = new int[4];

                                    // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                    // Otherwise it takes the related value.

                                    final int baseIndex = (posx / dnumBands) + (yposRoi[j]);

                                    // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                    // Otherwise it takes the related value.
                                    if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                                        dstData[dstPixelOffset] = destinationNoDataUShort;
                                    } else {
                                        int tempND = 0;
                                        int tempROI = 0;
                                        // X offset initialization
                                        int offsetX = 4 * xfrac[i];
                                        // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                        // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                        // and by roiscanlinestride on the y axis.
                                        for (int h = 0; h < 4; h++) {
                                            for (int z = 0; z < 4; z++) {
                                                // Selection of one pixel
                                                pixelKernel[h][z] = srcData[pos + (z - 1)
                                                        * srcPixelStride + (h - 1)
                                                        * srcScanlineStride] & 0xffff;

                                                int index = baseIndex - 1 + z + (h - 1)
                                                        * (roiScanlineStride);
                                                if (index < roiDataLength) {
                                                    // Update of the weight sum
                                                    tempROI += ((short) (roiDataArray[index] & 0xffff) != 0 ? 1
                                                            : 0);
                                                }

                                                if (!rangeND.contains((short) pixelKernel[h][z])) {
                                                    tempND++;
                                                    weightArray[h][z] = 1;
                                                }
                                            }
                                        }
                                        // Control if the 16 pixel are outside the ROI
                                        if (tempND == 0 || tempROI == 0) {
                                            dstData[dstPixelOffset] = destinationNoDataUShort;
                                        } else {

                                            long[] sumArray = new long[4];

                                            long sum = 0;
                                            int s = 0;

                                            for (int h = 0; h < 4; h++) {
                                                // Row temporary sum initialization
                                                long tempSum = 0;
                                                long[] tempData = bicubicInpainting(pixelKernel[h],
                                                        weightArray[h], null);
                                                for (int z = 0; z < 4; z++) {
                                                    // Update of the temporary sum
                                                    tempSum += (tempData[z] * (long) dataHi[offsetX
                                                            + z]);
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
                                            s = (int) ((sum + round) >> precisionBits);

                                            // Clamp
                                            if (s > 65536) {
                                                s = 65536;
                                            } else if (s < 0) {
                                                s = 0;
                                            }

                                            dstData[dstPixelOffset] = (short) (s & 0xffff);
                                        }

                                    }
                                    // destination pixel offset update
                                    dstPixelOffset += dstPixelStride;
                                }
                                // destination line offset update
                                dstlineOffset += dstScanlineStride;
                            }
                        }
                    } else {
                        // for all bands
                        for (int k = 0; k < dnumBands; k++) {

                            final short[] srcData = srcDataArrays[k];
                            final short[] dstData = dstDataArrays[k];
                            // Line and band Offset initialization
                            int dstlineOffset = dstBandOffsets[k];
                            int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                int posy = ypos[j] + bandOffset;
                                // Y offset initialization
                                int offsetY = 4 * yfrac[j];
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];

                                    // PixelPositions
                                    int x0 = src.getX() + posx / srcPixelStride;
                                    int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {

                                        int pos = posx + posy;

                                        long[][] pixelKernel = new long[4][4];
                                        int[][] weightArray = new int[4][4];
                                        int[] weightArrayVertical = new int[4];

                                        // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                        // Otherwise it takes the related value.

                                        int tempND = 0;
                                        int tempROI = 0;
                                        // X offset initialization
                                        int offsetX = 4 * xfrac[i];
                                        // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                        // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                        // and by roiscanlinestride on the y axis.
                                        for (int h = 0; h < 4; h++) {
                                            for (int z = 0; z < 4; z++) {
                                                // Selection of one pixel
                                                pixelKernel[h][z] = srcData[pos + (z - 1)
                                                        * srcPixelStride + (h - 1)
                                                        * srcScanlineStride] & 0xffff;

                                                tempROI += roiIter.getSample(x0 + h - 1,
                                                        y0 + z - 1, 0) & 0xffff;

                                                if (!rangeND.contains((short) pixelKernel[h][z])) {
                                                    tempND++;
                                                    weightArray[h][z] = 1;
                                                }
                                            }
                                        }

                                        // Control if the 16 pixel are outside the ROI
                                        if (tempND == 0 || tempROI == 0) {
                                            dstData[dstPixelOffset] = destinationNoDataUShort;
                                        } else {

                                            long[] sumArray = new long[4];

                                            long sum = 0;
                                            int s = 0;

                                            for (int h = 0; h < 4; h++) {
                                                // Row temporary sum initialization
                                                long tempSum = 0;
                                                long[] tempData = bicubicInpainting(pixelKernel[h],
                                                        weightArray[h], null);
                                                for (int z = 0; z < 4; z++) {
                                                    // Update of the temporary sum
                                                    tempSum += (tempData[z] * (long) dataHi[offsetX
                                                            + z]);
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
                                            s = (int) ((sum + round) >> precisionBits);

                                            // Clamp
                                            if (s > 65536) {
                                                s = 65536;
                                            } else if (s < 0) {
                                                s = 0;
                                            }

                                            dstData[dstPixelOffset] = (short) (s & 0xffff);
                                        }

                                    } else {
                                        dstData[dstPixelOffset] = destinationNoDataUShort;
                                    }

                                    // destination pixel offset update
                                    dstPixelOffset += dstPixelStride;
                                }
                                // destination line offset update
                                dstlineOffset += dstScanlineStride;
                            }
                        }
                    }
                }
            }
        }
    }

    private void shortLoop(RasterAccessor src, Rectangle dstRect,
            RasterAccessor dst, int[] xpos, int[] ypos, int[] xfrac,
            int[] yfrac, RasterAccessor roi, int[] yposRoi, int roiScanlineStride) {

        // BandOffsets
        final int srcScanlineStride = src.getScanlineStride();
        final int srcPixelStride = src.getPixelStride();
        final int bandOffsets[] = src.getBandOffsets();
        // Destination rectangle dimensions
        final int dwidth = dstRect.width;
        final int dheight = dstRect.height;
        // Destination image band numbers
        final int dnumBands = dst.getNumBands();
        // Destination bandOffsets, PixelStride and ScanLineStride
        final int dstBandOffsets[] = dst.getBandOffsets();
        final int dstPixelStride = dst.getPixelStride();
        final int dstScanlineStride = dst.getScanlineStride();

        // Destination and source data arrays (for all bands)
        final short[][] srcDataArrays = src.getShortDataArrays();

        final short[][] dstDataArrays = dst.getShortDataArrays();

        final byte[] roiDataArray;
        final int roiDataLength;
        if (useRoiAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
        } else {
            roiDataArray = null;
            roiDataLength = 0;
        }

        Range<Short> rangeND = (Range<Short>) noData;

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;
        if (caseA) {
            // for all bands
            for (int k = 0; k < dnumBands; k++) {

                final short[] srcData = srcDataArrays[k];
                final short[] dstData = dstDataArrays[k];
                // Line and band Offset initialization
                int dstlineOffset = dstBandOffsets[k];
                int bandOffset = bandOffsets[k];
                // cycle on the y values
                for (int j = 0; j < dheight; j++) {
                    // pixel offset initialization
                    int dstPixelOffset = dstlineOffset;
                    // y position selection
                    int posy = ypos[j] + bandOffset;
                    // Y offset initialization
                    int offsetY = 4 * yfrac[j];

                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        int posx = xpos[i];
                        int pos = posx + posy;

                        long sum = 0;

                        int s = 0;
                        // X offset initialization
                        int offsetX = 4 * xfrac[i];
                        // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                        for (int h = 0; h < 4; h++) {
                            // Row temporary sum initialization
                            long temp = 0;
                            for (int z = 0; z < 4; z++) {
                                // Selection of one pixel
                                int pixelValue = srcData[pos + (z - 1) * srcPixelStride + (h - 1)
                                        * srcScanlineStride];
                                // Update of the temporary sum
                                temp += (pixelValue * (long) dataHi[offsetX + z]);
                            }
                            // Vertical sum update
                            sum += ((temp + round) >> precisionBits) * (long) dataVi[offsetY + h];
                        }
                        // Interpolation
                        s = (int) ((sum + round) >> precisionBits);

                        // Clamp
                        if (s > Short.MAX_VALUE) {
                            s = Short.MAX_VALUE;
                        } else if (s < Short.MIN_VALUE) {
                            s = Short.MIN_VALUE;
                        }

                        // The interpolated value is saved in the destination array
                        dstData[dstPixelOffset] = (short) s;

                        // destination pixel offset update
                        dstPixelOffset += dstPixelStride;
                    }
                    // destination line offset update
                    dstlineOffset += dstScanlineStride;
                }
            }
        } else {
            if (caseB) {
                if (useRoiAccessor) {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {
                        final short[] srcData = srcDataArrays[k];
                        final short[] dstData = dstDataArrays[k];
                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            int posy = ypos[j] + bandOffset;
                            // Y offset initialization
                            int offsetY = 4 * yfrac[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                int pos = posx + posy;

                                int[][] pixelKernel = new int[4][4];

                                final int baseIndex = (posx / dnumBands) + (yposRoi[j]);

                                // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                // Otherwise it takes the related value.
                                if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataShort;
                                } else {
                                    int temp = 0;
                                    // X offset initialization
                                    int offsetX = 4 * xfrac[i];
                                    // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                    // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                    // and by roiscanlinestride on the y axis.
                                    for (int h = 0; h < 4; h++) {
                                        for (int z = 0; z < 4; z++) {
                                            // Selection of one pixel
                                            pixelKernel[h][z] = srcData[pos + (z - 1)
                                                    * srcPixelStride + (h - 1) * srcScanlineStride];
                                            int index = baseIndex - 1 + z + (h - 1)
                                                    * (roiScanlineStride);
                                            if (index < roiDataLength) {
                                                // Update of the weight sum
                                                temp += ((short) (roiDataArray[index]) != 0 ? 1
                                                        : 0);
                                            }
                                        }
                                    }
                                    // Control if the 16 pixel are outside the ROI
                                    if (temp == 0) {
                                        dstData[dstPixelOffset] = destinationNoDataShort;
                                    } else {
                                        long sum = 0;
                                        int s = 0;

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
                                        s = (int) ((sum + round) >> precisionBits);

                                        // Clamp
                                        if (s > Short.MAX_VALUE) {
                                            s = Short.MAX_VALUE;
                                        } else if (s < Short.MIN_VALUE) {
                                            s = Short.MIN_VALUE;
                                        }

                                        dstData[dstPixelOffset] = (short) s;
                                    }
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {
                        final short[] srcData = srcDataArrays[k];
                        final short[] dstData = dstDataArrays[k];
                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            int posy = ypos[j] + bandOffset;
                            // Y offset initialization
                            int offsetY = 4 * yfrac[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                int pos = posx + posy;

                                // PixelPositions
                                int x0 = src.getX() + posx / srcPixelStride;
                                int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                int[][] pixelKernel = new int[4][4];

                                // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                // Otherwise it takes the related value.
                                if (roiBounds.contains(x0, y0)) {

                                    int temp = 0;
                                    // X offset initialization
                                    int offsetX = 4 * xfrac[i];
                                    // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                    // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                    // and by roiscanlinestride on the y axis.
                                    for (int h = 0; h < 4; h++) {
                                        for (int z = 0; z < 4; z++) {
                                            // Selection of one pixel
                                            pixelKernel[h][z] = srcData[pos + (z - 1)
                                                    * srcPixelStride + (h - 1) * srcScanlineStride];
                                            temp += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0);
                                        }
                                    }
                                    // Control if the 16 pixel are outside the ROI
                                    if (temp == 0) {
                                        dstData[dstPixelOffset] = destinationNoDataShort;
                                    } else {
                                        long sum = 0;
                                        int s = 0;

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
                                        s = (int) ((sum + round) >> precisionBits);

                                        // Clamp
                                        if (s > Short.MAX_VALUE) {
                                            s = Short.MAX_VALUE;
                                        } else if (s < Short.MIN_VALUE) {
                                            s = Short.MIN_VALUE;
                                        }

                                        dstData[dstPixelOffset] = (short) s;
                                    }
                                } else {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataShort;
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }

                }
            } else {
                if (caseC) {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {

                        final short[] srcData = srcDataArrays[k];
                        final short[] dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            int posy = ypos[j] + bandOffset;
                            // Y offset initialization
                            int offsetY = 4 * yfrac[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                int pos = posx + posy;

                                long[][] pixelKernel = new long[4][4];
                                int[][] weightArray = new int[4][4];
                                int[] weightArrayVertical = new int[4];

                                // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                // Otherwise it takes the related value.

                                int temp = 0;
                                // X offset initialization
                                int offsetX = 4 * xfrac[i];
                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by roiscanlinestride on the y axis.
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcData[pos + (z - 1) * srcPixelStride
                                                + (h - 1) * srcScanlineStride];

                                        if (!rangeND.contains((short) pixelKernel[h][z])) {
                                            temp++;
                                            weightArray[h][z] = 1;
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                if (temp == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataShort;
                                } else {

                                    long[] sumArray = new long[4];

                                    long sum = 0;
                                    int s = 0;

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
                                    s = (int) ((sum + round) >> precisionBits);

                                    // Clamp
                                    if (s > Short.MAX_VALUE) {
                                        s = Short.MAX_VALUE;
                                    } else if (s < Short.MIN_VALUE) {
                                        s = Short.MIN_VALUE;
                                    }

                                    dstData[dstPixelOffset] = (short) s;
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else if (hasROI && hasNoData) {
                    if (useRoiAccessor) {
                        // for all bands
                        for (int k = 0; k < dnumBands; k++) {

                            final short[] srcData = srcDataArrays[k];
                            final short[] dstData = dstDataArrays[k];
                            // Line and band Offset initialization
                            int dstlineOffset = dstBandOffsets[k];
                            int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                int posy = ypos[j] + bandOffset;
                                // Y offset initialization
                                int offsetY = 4 * yfrac[j];
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];

                                    int pos = posx + posy;

                                    long[][] pixelKernel = new long[4][4];
                                    int[][] weightArray = new int[4][4];
                                    int[] weightArrayVertical = new int[4];

                                    // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                    // Otherwise it takes the related value.

                                    final int baseIndex = (posx / dnumBands) + (yposRoi[j]);

                                    // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                    // Otherwise it takes the related value.
                                    if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                                        dstData[dstPixelOffset] = destinationNoDataShort;
                                    } else {
                                        int tempND = 0;
                                        int tempROI = 0;
                                        // X offset initialization
                                        int offsetX = 4 * xfrac[i];
                                        // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                        // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                        // and by roiscanlinestride on the y axis.
                                        for (int h = 0; h < 4; h++) {
                                            for (int z = 0; z < 4; z++) {
                                                // Selection of one pixel
                                                pixelKernel[h][z] = srcData[pos + (z - 1)
                                                        * srcPixelStride + (h - 1)
                                                        * srcScanlineStride];

                                                int index = baseIndex - 1 + z + (h - 1)
                                                        * (roiScanlineStride);
                                                if (index < roiDataLength) {
                                                    // Update of the weight sum
                                                    tempROI += ((short) (roiDataArray[index]) != 0 ? 1
                                                            : 0);
                                                }

                                                if (!rangeND.contains((short) pixelKernel[h][z])) {
                                                    tempND++;
                                                    weightArray[h][z] = 1;
                                                }
                                            }
                                        }
                                        // Control if the 16 pixel are outside the ROI
                                        if (tempND == 0 || tempROI == 0) {
                                            dstData[dstPixelOffset] = destinationNoDataShort;
                                        } else {

                                            long[] sumArray = new long[4];

                                            long sum = 0;
                                            int s = 0;

                                            for (int h = 0; h < 4; h++) {
                                                // Row temporary sum initialization
                                                long tempSum = 0;
                                                long[] tempData = bicubicInpainting(pixelKernel[h],
                                                        weightArray[h], null);
                                                for (int z = 0; z < 4; z++) {
                                                    // Update of the temporary sum
                                                    tempSum += (tempData[z] * (long) dataHi[offsetX
                                                            + z]);
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
                                            s = (int) ((sum + round) >> precisionBits);

                                            // Clamp
                                            if (s > Short.MAX_VALUE) {
                                                s = Short.MAX_VALUE;
                                            } else if (s < Short.MIN_VALUE) {
                                                s = Short.MIN_VALUE;
                                            }

                                            dstData[dstPixelOffset] = (short) s;
                                        }

                                    }
                                    // destination pixel offset update
                                    dstPixelOffset += dstPixelStride;
                                }
                                // destination line offset update
                                dstlineOffset += dstScanlineStride;
                            }
                        }
                    } else {
                        // for all bands
                        for (int k = 0; k < dnumBands; k++) {

                            final short[] srcData = srcDataArrays[k];
                            final short[] dstData = dstDataArrays[k];
                            // Line and band Offset initialization
                            int dstlineOffset = dstBandOffsets[k];
                            int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                int posy = ypos[j] + bandOffset;
                                // Y offset initialization
                                int offsetY = 4 * yfrac[j];
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];

                                    // PixelPositions
                                    int x0 = src.getX() + posx / srcPixelStride;
                                    int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {

                                        int pos = posx + posy;

                                        long[][] pixelKernel = new long[4][4];
                                        int[][] weightArray = new int[4][4];
                                        int[] weightArrayVertical = new int[4];

                                        // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                        // Otherwise it takes the related value.

                                        int tempND = 0;
                                        int tempROI = 0;
                                        // X offset initialization
                                        int offsetX = 4 * xfrac[i];
                                        // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                        // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                        // and by roiscanlinestride on the y axis.
                                        for (int h = 0; h < 4; h++) {
                                            for (int z = 0; z < 4; z++) {
                                                // Selection of one pixel
                                                pixelKernel[h][z] = srcData[pos + (z - 1)
                                                        * srcPixelStride + (h - 1)
                                                        * srcScanlineStride];

                                                tempROI += roiIter.getSample(x0 + h - 1,
                                                        y0 + z - 1, 0);

                                                if (!rangeND.contains((short) pixelKernel[h][z])) {
                                                    tempND++;
                                                    weightArray[h][z] = 1;
                                                }
                                            }
                                        }

                                        // Control if the 16 pixel are outside the ROI
                                        if (tempND == 0 || tempROI == 0) {
                                            dstData[dstPixelOffset] = destinationNoDataShort;
                                        } else {

                                            long[] sumArray = new long[4];

                                            long sum = 0;
                                            int s = 0;

                                            for (int h = 0; h < 4; h++) {
                                                // Row temporary sum initialization
                                                long tempSum = 0;
                                                long[] tempData = bicubicInpainting(pixelKernel[h],
                                                        weightArray[h], null);
                                                for (int z = 0; z < 4; z++) {
                                                    // Update of the temporary sum
                                                    tempSum += (tempData[z] * (long) dataHi[offsetX
                                                            + z]);
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
                                            s = (int) ((sum + round) >> precisionBits);

                                            // Clamp
                                            if (s > Short.MAX_VALUE) {
                                                s = Short.MAX_VALUE;
                                            } else if (s < Short.MIN_VALUE) {
                                                s = Short.MIN_VALUE;
                                            }

                                            dstData[dstPixelOffset] = (short) s;
                                        }

                                    } else {
                                        dstData[dstPixelOffset] = destinationNoDataShort;
                                    }

                                    // destination pixel offset update
                                    dstPixelOffset += dstPixelStride;
                                }
                                // destination line offset update
                                dstlineOffset += dstScanlineStride;
                            }
                        }
                    }
                }
            }
        }
    }

    private void intLoop(RasterAccessor src, Rectangle dstRect,
            RasterAccessor dst, int[] xpos, int[] ypos, int[] xfrac,
            int[] yfrac, RasterAccessor roi, int[] yposRoi, int roiScanlineStride) {

        // BandOffsets
        final int srcScanlineStride = src.getScanlineStride();
        final int srcPixelStride = src.getPixelStride();
        final int bandOffsets[] = src.getBandOffsets();
        // Destination rectangle dimensions
        final int dwidth = dstRect.width;
        final int dheight = dstRect.height;
        // Destination image band numbers
        final int dnumBands = dst.getNumBands();
        // Destination bandOffsets, PixelStride and ScanLineStride
        final int dstBandOffsets[] = dst.getBandOffsets();
        final int dstPixelStride = dst.getPixelStride();
        final int dstScanlineStride = dst.getScanlineStride();

        // Destination and source data arrays (for all bands)
        final int[][] srcDataArrays = src.getIntDataArrays();

        final int[][] dstDataArrays = dst.getIntDataArrays();

        final byte[] roiDataArray;
        final int roiDataLength;
        if (useRoiAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
        } else {
            roiDataArray = null;
            roiDataLength = 0;
        }

        Range<Integer> rangeND = (Range<Integer>) noData;

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;
        if (caseA) {
            // for all bands
            for (int k = 0; k < dnumBands; k++) {

                final int[] srcData = srcDataArrays[k];
                final int[] dstData = dstDataArrays[k];
                // Line and band Offset initialization
                int dstlineOffset = dstBandOffsets[k];
                int bandOffset = bandOffsets[k];
                // cycle on the y values
                for (int j = 0; j < dheight; j++) {
                    // pixel offset initialization
                    int dstPixelOffset = dstlineOffset;
                    // y position selection
                    int posy = ypos[j] + bandOffset;
                    // Y offset initialization
                    int offsetY = 4 * yfrac[j];

                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        int posx = xpos[i];
                        int pos = posx + posy;

                        long sum = 0;

                        int s = 0;
                        // X offset initialization
                        int offsetX = 4 * xfrac[i];
                        // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                        for (int h = 0; h < 4; h++) {
                            // Row temporary sum initialization
                            long temp = 0;
                            for (int z = 0; z < 4; z++) {
                                // Selection of one pixel
                                int pixelValue = srcData[pos + (z - 1) * srcPixelStride + (h - 1)
                                        * srcScanlineStride];
                                // Update of the temporary sum
                                temp += (pixelValue * (long) dataHi[offsetX + z]);
                            }
                            // Vertical sum update
                            sum += ((temp + round) >> precisionBits) * (long) dataVi[offsetY + h];
                        }
                        // Interpolation
                        s = (int) ((sum + round) >> precisionBits);

                        // The interpolated value is saved in the destination array
                        dstData[dstPixelOffset] = s;

                        // destination pixel offset update
                        dstPixelOffset += dstPixelStride;
                    }
                    // destination line offset update
                    dstlineOffset += dstScanlineStride;
                }
            }
        } else {
            if (caseB) {
                if (useRoiAccessor) {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {
                        final int[] srcData = srcDataArrays[k];
                        final int[] dstData = dstDataArrays[k];
                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            int posy = ypos[j] + bandOffset;
                            // Y offset initialization
                            int offsetY = 4 * yfrac[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                int pos = posx + posy;

                                int[][] pixelKernel = new int[4][4];

                                final int baseIndex = (posx / dnumBands) + (yposRoi[j]);

                                // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                // Otherwise it takes the related value.
                                if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataInt;
                                } else {
                                    int temp = 0;
                                    // X offset initialization
                                    int offsetX = 4 * xfrac[i];
                                    // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                    // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                    // and by roiscanlinestride on the y axis.
                                    for (int h = 0; h < 4; h++) {
                                        for (int z = 0; z < 4; z++) {
                                            // Selection of one pixel
                                            pixelKernel[h][z] = srcData[pos + (z - 1)
                                                    * srcPixelStride + (h - 1) * srcScanlineStride];
                                            int index = baseIndex - 1 + z + (h - 1)
                                                    * (roiScanlineStride);
                                            if (index < roiDataLength) {
                                                // Update of the weight sum
                                                temp += ((roiDataArray[index]) != 0 ? 1
                                                        : 0);
                                            }
                                        }
                                    }
                                    // Control if the 16 pixel are outside the ROI
                                    if (temp == 0) {
                                        dstData[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        long sum = 0;
                                        int s = 0;

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
                                        s = (int) ((sum + round) >> precisionBits);

                                        dstData[dstPixelOffset] = s;
                                    }
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {
                        final int[] srcData = srcDataArrays[k];
                        final int[] dstData = dstDataArrays[k];
                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            int posy = ypos[j] + bandOffset;
                            // Y offset initialization
                            int offsetY = 4 * yfrac[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                int pos = posx + posy;

                                // PixelPositions
                                int x0 = src.getX() + posx / srcPixelStride;
                                int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                int[][] pixelKernel = new int[4][4];

                                // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                // Otherwise it takes the related value.
                                if (roiBounds.contains(x0, y0)) {

                                    int temp = 0;
                                    // X offset initialization
                                    int offsetX = 4 * xfrac[i];
                                    // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                    // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                    // and by roiscanlinestride on the y axis.
                                    for (int h = 0; h < 4; h++) {
                                        for (int z = 0; z < 4; z++) {
                                            // Selection of one pixel
                                            pixelKernel[h][z] = srcData[pos + (z - 1)
                                                    * srcPixelStride + (h - 1) * srcScanlineStride];
                                            temp += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0);
                                        }
                                    }
                                    // Control if the 16 pixel are outside the ROI
                                    if (temp == 0) {
                                        dstData[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        long sum = 0;
                                        int s = 0;

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
                                        s = (int) ((sum + round) >> precisionBits);

                                        dstData[dstPixelOffset] =  s;
                                    }
                                } else {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataInt;
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }

                }
            } else {
                if (caseC) {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {

                        final int[] srcData = srcDataArrays[k];
                        final int[] dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            int posy = ypos[j] + bandOffset;
                            // Y offset initialization
                            int offsetY = 4 * yfrac[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                int pos = posx + posy;

                                long[][] pixelKernel = new long[4][4];
                                int[][] weightArray = new int[4][4];
                                int[] weightArrayVertical = new int[4];

                                // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                // Otherwise it takes the related value.

                                int temp = 0;
                                // X offset initialization
                                int offsetX = 4 * xfrac[i];
                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by roiscanlinestride on the y axis.
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcData[pos + (z - 1) * srcPixelStride
                                                + (h - 1) * srcScanlineStride];

                                        if (!rangeND.contains((int) pixelKernel[h][z])) {
                                            temp++;
                                            weightArray[h][z] = 1;
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                if (temp == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataShort;
                                } else {

                                    long[] sumArray = new long[4];

                                    long sum = 0;
                                    int s = 0;

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
                                    s = (int) ((sum + round) >> precisionBits);

                                    dstData[dstPixelOffset] = s;
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else if (hasROI && hasNoData) {
                    if (useRoiAccessor) {
                        // for all bands
                        for (int k = 0; k < dnumBands; k++) {

                            final int[] srcData = srcDataArrays[k];
                            final int[] dstData = dstDataArrays[k];
                            // Line and band Offset initialization
                            int dstlineOffset = dstBandOffsets[k];
                            int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                int posy = ypos[j] + bandOffset;
                                // Y offset initialization
                                int offsetY = 4 * yfrac[j];
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];

                                    int pos = posx + posy;

                                    long[][] pixelKernel = new long[4][4];
                                    int[][] weightArray = new int[4][4];
                                    int[] weightArrayVertical = new int[4];

                                    // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                    // Otherwise it takes the related value.

                                    final int baseIndex = (posx / dnumBands) + (yposRoi[j]);

                                    // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                    // Otherwise it takes the related value.
                                    if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                                        dstData[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        int tempND = 0;
                                        int tempROI = 0;
                                        // X offset initialization
                                        int offsetX = 4 * xfrac[i];
                                        // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                        // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                        // and by roiscanlinestride on the y axis.
                                        for (int h = 0; h < 4; h++) {
                                            for (int z = 0; z < 4; z++) {
                                                // Selection of one pixel
                                                pixelKernel[h][z] = srcData[pos + (z - 1)
                                                        * srcPixelStride + (h - 1)
                                                        * srcScanlineStride];

                                                int index = baseIndex - 1 + z + (h - 1)
                                                        * (roiScanlineStride);
                                                if (index < roiDataLength) {
                                                    // Update of the weight sum
                                                    tempROI += ((roiDataArray[index]) != 0 ? 1
                                                            : 0);
                                                }

                                                if (!rangeND.contains((int) pixelKernel[h][z])) {
                                                    tempND++;
                                                    weightArray[h][z] = 1;
                                                }
                                            }
                                        }
                                        // Control if the 16 pixel are outside the ROI
                                        if (tempND == 0 || tempROI == 0) {
                                            dstData[dstPixelOffset] = destinationNoDataInt;
                                        } else {

                                            long[] sumArray = new long[4];

                                            long sum = 0;
                                            int s = 0;

                                            for (int h = 0; h < 4; h++) {
                                                // Row temporary sum initialization
                                                long tempSum = 0;
                                                long[] tempData = bicubicInpainting(pixelKernel[h],
                                                        weightArray[h], null);
                                                for (int z = 0; z < 4; z++) {
                                                    // Update of the temporary sum
                                                    tempSum += (tempData[z] * (long) dataHi[offsetX
                                                            + z]);
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
                                            s = (int) ((sum + round) >> precisionBits);

                                            dstData[dstPixelOffset] = s;
                                        }

                                    }
                                    // destination pixel offset update
                                    dstPixelOffset += dstPixelStride;
                                }
                                // destination line offset update
                                dstlineOffset += dstScanlineStride;
                            }
                        }
                    } else {
                        // for all bands
                        for (int k = 0; k < dnumBands; k++) {

                            final int[] srcData = srcDataArrays[k];
                            final int[] dstData = dstDataArrays[k];
                            // Line and band Offset initialization
                            int dstlineOffset = dstBandOffsets[k];
                            int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                int posy = ypos[j] + bandOffset;
                                // Y offset initialization
                                int offsetY = 4 * yfrac[j];
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];

                                    // PixelPositions
                                    int x0 = src.getX() + posx / srcPixelStride;
                                    int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {

                                        int pos = posx + posy;

                                        long[][] pixelKernel = new long[4][4];
                                        int[][] weightArray = new int[4][4];
                                        int[] weightArrayVertical = new int[4];

                                        // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                        // Otherwise it takes the related value.

                                        int tempND = 0;
                                        int tempROI = 0;
                                        // X offset initialization
                                        int offsetX = 4 * xfrac[i];
                                        // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                        // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                        // and by roiscanlinestride on the y axis.
                                        for (int h = 0; h < 4; h++) {
                                            for (int z = 0; z < 4; z++) {
                                                // Selection of one pixel
                                                pixelKernel[h][z] = srcData[pos + (z - 1)
                                                        * srcPixelStride + (h - 1)
                                                        * srcScanlineStride];

                                                tempROI += roiIter.getSample(x0 + h - 1,
                                                        y0 + z - 1, 0);

                                                if (!rangeND.contains((int) pixelKernel[h][z])) {
                                                    tempND++;
                                                    weightArray[h][z] = 1;
                                                }
                                            }
                                        }

                                        // Control if the 16 pixel are outside the ROI
                                        if (tempND == 0 || tempROI == 0) {
                                            dstData[dstPixelOffset] = destinationNoDataInt;
                                        } else {

                                            long[] sumArray = new long[4];

                                            long sum = 0;
                                            int s = 0;

                                            for (int h = 0; h < 4; h++) {
                                                // Row temporary sum initialization
                                                long tempSum = 0;
                                                long[] tempData = bicubicInpainting(pixelKernel[h],
                                                        weightArray[h], null);
                                                for (int z = 0; z < 4; z++) {
                                                    // Update of the temporary sum
                                                    tempSum += (tempData[z] * (long) dataHi[offsetX
                                                            + z]);
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
                                            s = (int) ((sum + round) >> precisionBits);

                                            dstData[dstPixelOffset] = s;
                                        }

                                    } else {
                                        dstData[dstPixelOffset] = destinationNoDataInt;
                                    }

                                    // destination pixel offset update
                                    dstPixelOffset += dstPixelStride;
                                }
                                // destination line offset update
                                dstlineOffset += dstScanlineStride;
                            }
                        }
                    }
                }
            }
        }
    }

    private void floatLoop(RasterAccessor src, Rectangle dstRect,
            RasterAccessor dst, int[] xpos, int[] ypos, float[] xfrac,
            float[] yfrac, RasterAccessor roi, int[] yposRoi,
            int roiScanlineStride) {

        // BandOffsets
        final int srcScanlineStride = src.getScanlineStride();
        final int srcPixelStride = src.getPixelStride();
        final int bandOffsets[] = src.getBandOffsets();
        // Destination rectangle dimensions
        final int dwidth = dstRect.width;
        final int dheight = dstRect.height;
        // Destination image band numbers
        final int dnumBands = dst.getNumBands();
        // Destination bandOffsets, PixelStride and ScanLineStride
        final int dstBandOffsets[] = dst.getBandOffsets();
        final int dstPixelStride = dst.getPixelStride();
        final int dstScanlineStride = dst.getScanlineStride();

        // Destination and source data arrays (for all bands)
        final float[][] srcDataArrays = src.getFloatDataArrays();

        final float[][] dstDataArrays = dst.getFloatDataArrays();

        final byte[] roiDataArray;
        final int roiDataLength;
        if (useRoiAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
        } else {
            roiDataArray = null;
            roiDataLength = 0;
        }

        Range<Float> rangeND = (Range<Float>) noData;

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;
        if (caseA) {
            // for all bands
            for (int k = 0; k < dnumBands; k++) {

                final float[] srcData = srcDataArrays[k];
                final float[] dstData = dstDataArrays[k];
                // Line and band Offset initialization
                int dstlineOffset = dstBandOffsets[k];
                int bandOffset = bandOffsets[k];
                // cycle on the y values
                for (int j = 0; j < dheight; j++) {
                    // pixel offset initialization
                    int dstPixelOffset = dstlineOffset;
                    // y position selection
                    int posy = ypos[j] + bandOffset;
                    // Y offset initialization
                    int offsetY = (int) (4 * yfrac[j]);

                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        int posx = xpos[i];
                        int pos = posx + posy;

                        double sum = 0;
                        // X offset initialization
                        int offsetX = (int) (4 * xfrac[i]);
                        // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                        for (int h = 0; h < 4; h++) {
                            // Row temporary sum initialization
                            double temp = 0;
                            for (int z = 0; z < 4; z++) {
                                // Selection of one pixel
                                float pixelValue = srcData[pos + (z - 1) * srcPixelStride + (h - 1)
                                        * srcScanlineStride];
                                // Update of the temporary sum
                                temp += (pixelValue * dataHf[offsetX + z]);
                            }
                            // Vertical sum update
                            sum += temp *dataVf[offsetY + h];
                        }

                        // Clamp
                        if (sum > Float.MAX_VALUE) {
                            sum = Float.MAX_VALUE;
                        } else if (sum < -Float.MAX_VALUE) {
                            sum = -Float.MAX_VALUE;
                        }

                        // The interpolated value is saved in the destination array
                        dstData[dstPixelOffset] = (float) sum;

                        // destination pixel offset update
                        dstPixelOffset += dstPixelStride;
                    }
                    // destination line offset update
                    dstlineOffset += dstScanlineStride;
                }
            }
        } else {
            if (caseB) {
                if (useRoiAccessor) {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {
                        final float[] srcData = srcDataArrays[k];
                        final float[] dstData = dstDataArrays[k];
                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            int posy = ypos[j] + bandOffset;
                            // Y offset initialization
                            int offsetY = (int) (4 * yfrac[j]);
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                int pos = posx + posy;

                                float[][] pixelKernel = new float[4][4];

                                final int baseIndex = (posx / dnumBands) + (yposRoi[j]);

                                // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                // Otherwise it takes the related value.
                                if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataFloat;
                                } else {
                                    int temp = 0;
                                    // X offset initialization
                                    int offsetX = (int) (4 * xfrac[i]);
                                    // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                    // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                    // and by roiscanlinestride on the y axis.
                                    for (int h = 0; h < 4; h++) {
                                        for (int z = 0; z < 4; z++) {
                                            // Selection of one pixel
                                            pixelKernel[h][z] = srcData[pos + (z - 1)
                                                    * srcPixelStride + (h - 1) * srcScanlineStride];
                                            int index = baseIndex - 1 + z + (h - 1)
                                                    * (roiScanlineStride);
                                            if (index < roiDataLength) {
                                                // Update of the weight sum
                                                temp += ((roiDataArray[index]) != 0 ? 1
                                                        : 0);
                                            }
                                        }
                                    }
                                    // Control if the 16 pixel are outside the ROI
                                    if (temp == 0) {
                                        dstData[dstPixelOffset] = destinationNoDataFloat;
                                    } else {
                                        double sum = 0;

                                        for (int h = 0; h < 4; h++) {
                                            // Row temporary sum initialization
                                            double tempSum = 0;
                                            for (int z = 0; z < 4; z++) {
                                                // Update of the temporary sum
                                                tempSum += (pixelKernel[h][z] * dataHf[offsetX
                                                        + z]);
                                            }
                                            // Vertical sum update
                                            sum += tempSum * dataVf[offsetY + h];
                                        }

                                        // Clamp
                                        if (sum > Float.MAX_VALUE) {
                                            sum = Float.MAX_VALUE;
                                        } else if (sum < -Float.MAX_VALUE) {
                                            sum = -Float.MAX_VALUE;
                                        }

                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = (float) sum;
                                    }
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {
                        final float[] srcData = srcDataArrays[k];
                        final float[] dstData = dstDataArrays[k];
                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            int posy = ypos[j] + bandOffset;
                            // Y offset initialization
                            int offsetY = (int) (4 * yfrac[j]);
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                int pos = posx + posy;

                                // PixelPositions
                                int x0 = src.getX() + posx / srcPixelStride;
                                int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                float[][] pixelKernel = new float[4][4];

                                // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                // Otherwise it takes the related value.
                                if (roiBounds.contains(x0, y0)) {

                                    int temp = 0;
                                    // X offset initialization
                                    int offsetX = (int) (4 * xfrac[i]);
                                    // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                    // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                    // and by roiscanlinestride on the y axis.
                                    for (int h = 0; h < 4; h++) {
                                        for (int z = 0; z < 4; z++) {
                                            // Selection of one pixel
                                            pixelKernel[h][z] = srcData[pos + (z - 1)
                                                    * srcPixelStride + (h - 1) * srcScanlineStride];
                                            temp += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0);
                                        }
                                    }
                                    // Control if the 16 pixel are outside the ROI
                                    if (temp == 0) {
                                        dstData[dstPixelOffset] = destinationNoDataFloat;
                                    } else {
                                        double sum = 0;

                                        for (int h = 0; h < 4; h++) {
                                            // Row temporary sum initialization
                                            double tempSum = 0;
                                            for (int z = 0; z < 4; z++) {
                                                // Update of the temporary sum
                                                tempSum += (pixelKernel[h][z] * dataHf[offsetX
                                                        + z]);
                                            }
                                            // Vertical sum update
                                            sum += tempSum * dataVf[offsetY + h];
                                        }

                                        // Clamp
                                        if (sum > Float.MAX_VALUE) {
                                            sum = Float.MAX_VALUE;
                                        } else if (sum < -Float.MAX_VALUE) {
                                            sum = -Float.MAX_VALUE;
                                        }

                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = (float) sum;
                                    }
                                } else {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataFloat;
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }

                }
            } else {
                if (caseC) {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {

                        final float[] srcData = srcDataArrays[k];
                        final float[] dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            int posy = ypos[j] + bandOffset;
                            // Y offset initialization
                            int offsetY = (int) (4 * yfrac[j]);
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                int pos = posx + posy;

                                double[][] pixelKernel = new double[4][4];
                                int[][] weightArray = new int[4][4];
                                int[] weightArrayVertical = new int[4];

                                // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                // Otherwise it takes the related value.

                                int temp = 0;
                                // X offset initialization
                                int offsetX = (int) (4 * xfrac[i]);
                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by roiscanlinestride on the y axis.
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcData[pos + (z - 1) * srcPixelStride
                                                + (h - 1) * srcScanlineStride];

                                        if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                            if(pixelKernel[h][z] == Float.NEGATIVE_INFINITY || pixelKernel[h][z] == Float.NEGATIVE_INFINITY ||Double.isNaN(pixelKernel[h][z])){
                                             // The destination no data value is saved in the destination array
                                                weightArray[h][z] = 0;
                                            }else{
                                                temp++;
                                                weightArray[h][z] = 1;
                                            }
                                        }else if (rangeND.contains((float) pixelKernel[h][z])) {
                                            weightArray[h][z] = 0;
                                        }else{
                                            temp++;
                                            weightArray[h][z] = 1;
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                if (temp == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataFloat;
                                } else {

                                    double[] sumArray = new double[4];

                                    double sum = 0;

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

                                    // Clamp
                                    if (sum > Float.MAX_VALUE) {
                                        sum = Float.MAX_VALUE;
                                    } else if (sum < -Float.MAX_VALUE) {
                                        sum = -Float.MAX_VALUE;
                                    }

                                    // The interpolated value is saved in the destination array
                                    dstData[dstPixelOffset] = (float) sum;
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else if (hasROI && hasNoData) {
                    if (useRoiAccessor) {
                        // for all bands
                        for (int k = 0; k < dnumBands; k++) {

                            final float[] srcData = srcDataArrays[k];
                            final float[] dstData = dstDataArrays[k];
                            // Line and band Offset initialization
                            int dstlineOffset = dstBandOffsets[k];
                            int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                int posy = ypos[j] + bandOffset;
                                // Y offset initialization
                                int offsetY = (int) (4 * yfrac[j]);
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];

                                    int pos = posx + posy;

                                    double[][] pixelKernel = new double[4][4];
                                    int[][] weightArray = new int[4][4];
                                    int[] weightArrayVertical = new int[4];

                                    // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                    // Otherwise it takes the related value.

                                    final int baseIndex = (posx / dnumBands) + (yposRoi[j]);

                                    // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                    // Otherwise it takes the related value.
                                    if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                                        dstData[dstPixelOffset] = destinationNoDataFloat;
                                    } else {
                                        int tempND = 0;
                                        int tempROI = 0;
                                        // X offset initialization
                                        int offsetX = (int) (4 * xfrac[i]);
                                        // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                        // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                        // and by roiscanlinestride on the y axis.
                                        for (int h = 0; h < 4; h++) {
                                            for (int z = 0; z < 4; z++) {
                                                // Selection of one pixel
                                                pixelKernel[h][z] = srcData[pos + (z - 1)
                                                        * srcPixelStride + (h - 1)
                                                        * srcScanlineStride];

                                                int index = baseIndex - 1 + z + (h - 1)
                                                        * (roiScanlineStride);
                                                if (index < roiDataLength) {
                                                    // Update of the weight sum
                                                    tempROI += ((roiDataArray[index]) != 0 ? 1
                                                            : 0);
                                                }

                                                if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                                    if(pixelKernel[h][z] == Float.NEGATIVE_INFINITY || pixelKernel[h][z] == Float.NEGATIVE_INFINITY ||Double.isNaN(pixelKernel[h][z])){
                                                     // The destination no data value is saved in the destination array
                                                        weightArray[h][z] = 0;
                                                    }else{
                                                        tempND++;
                                                        weightArray[h][z] = 1;
                                                    }
                                                }else if (rangeND.contains((float) pixelKernel[h][z])) {
                                                    weightArray[h][z] = 0;
                                                }else{
                                                    tempND++;
                                                    weightArray[h][z] = 1;
                                                }
                                            }
                                        }
                                        // Control if the 16 pixel are outside the ROI
                                        if (tempND == 0 || tempROI == 0) {
                                            dstData[dstPixelOffset] = destinationNoDataFloat;
                                        } else {

                                            double[] sumArray = new double[4];

                                            double sum = 0;

                                            for (int h = 0; h < 4; h++) {
                                                // Row temporary sum initialization
                                                double tempSum = 0;
                                                double[] tempData = bicubicInpaintingDouble(pixelKernel[h],
                                                        weightArray[h], null);
                                                for (int z = 0; z < 4; z++) {
                                                    // Update of the temporary sum
                                                    tempSum += (tempData[z] * dataHf[offsetX
                                                            + z]);
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

                                            // Clamp
                                            if (sum > Float.MAX_VALUE) {
                                                sum = Float.MAX_VALUE;
                                            } else if (sum < -Float.MAX_VALUE) {
                                                sum = -Float.MAX_VALUE;
                                            }

                                            // The interpolated value is saved in the destination array
                                            dstData[dstPixelOffset] = (float) sum;
                                        }

                                    }
                                    // destination pixel offset update
                                    dstPixelOffset += dstPixelStride;
                                }
                                // destination line offset update
                                dstlineOffset += dstScanlineStride;
                            }
                        }
                    } else {
                        // for all bands
                        for (int k = 0; k < dnumBands; k++) {

                            final float[] srcData = srcDataArrays[k];
                            final float[] dstData = dstDataArrays[k];
                            // Line and band Offset initialization
                            int dstlineOffset = dstBandOffsets[k];
                            int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                int posy = ypos[j] + bandOffset;
                                // Y offset initialization
                                int offsetY = (int) (4 * yfrac[j]);
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];

                                    // PixelPositions
                                    int x0 = src.getX() + posx / srcPixelStride;
                                    int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {

                                        int pos = posx + posy;

                                        double[][] pixelKernel = new double[4][4];
                                        int[][] weightArray = new int[4][4];
                                        int[] weightArrayVertical = new int[4];

                                        // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                        // Otherwise it takes the related value.

                                        int tempND = 0;
                                        int tempROI = 0;
                                        // X offset initialization
                                        int offsetX = (int) (4 * xfrac[i]);
                                        // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                        // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                        // and by roiscanlinestride on the y axis.
                                        for (int h = 0; h < 4; h++) {
                                            for (int z = 0; z < 4; z++) {
                                                // Selection of one pixel
                                                pixelKernel[h][z] = srcData[pos + (z - 1)
                                                        * srcPixelStride + (h - 1)
                                                        * srcScanlineStride];

                                                tempROI += roiIter.getSample(x0 + h - 1,
                                                        y0 + z - 1, 0);

                                                if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                                    if(pixelKernel[h][z] == Float.NEGATIVE_INFINITY || pixelKernel[h][z] == Float.NEGATIVE_INFINITY ||Double.isNaN(pixelKernel[h][z])){
                                                     // The destination no data value is saved in the destination array
                                                        weightArray[h][z] = 0;
                                                    }else{
                                                        tempND++;
                                                        weightArray[h][z] = 1;
                                                    }
                                                }else if (rangeND.contains((float) pixelKernel[h][z])) {
                                                    weightArray[h][z] = 0;
                                                }else{
                                                    tempND++;
                                                    weightArray[h][z] = 1;
                                                }
                                            }
                                        }

                                        // Control if the 16 pixel are outside the ROI
                                        if (tempND == 0 || tempROI == 0) {
                                            dstData[dstPixelOffset] = destinationNoDataFloat;
                                        } else {

                                            double[] sumArray = new double[4];

                                            double sum = 0;

                                            for (int h = 0; h < 4; h++) {
                                                // Row temporary sum initialization
                                                double tempSum = 0;
                                                double[] tempData = bicubicInpaintingDouble(pixelKernel[h],
                                                        weightArray[h], null);
                                                for (int z = 0; z < 4; z++) {
                                                    // Update of the temporary sum
                                                    tempSum += (tempData[z] * dataHf[offsetX
                                                            + z]);
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

                                            // Clamp
                                            if (sum > Float.MAX_VALUE) {
                                                sum = Float.MAX_VALUE;
                                            } else if (sum < -Float.MAX_VALUE) {
                                                sum = -Float.MAX_VALUE;
                                            }

                                            // The interpolated value is saved in the destination array
                                            dstData[dstPixelOffset] = (float) sum;
                                        }

                                    } else {
                                        dstData[dstPixelOffset] = destinationNoDataFloat;
                                    }

                                    // destination pixel offset update
                                    dstPixelOffset += dstPixelStride;
                                }
                                // destination line offset update
                                dstlineOffset += dstScanlineStride;
                            }
                        }
                    }
                }
            }
        }
    }

    private void doubleLoop(RasterAccessor src, Rectangle dstRect,
            RasterAccessor dst, int[] xpos, int[] ypos, float[] xfrac,
            float[] yfrac, RasterAccessor roi, int[] yposRoi,
            int roiScanlineStride) {

        // BandOffsets
        final int srcScanlineStride = src.getScanlineStride();
        final int srcPixelStride = src.getPixelStride();
        final int bandOffsets[] = src.getBandOffsets();
        // Destination rectangle dimensions
        final int dwidth = dstRect.width;
        final int dheight = dstRect.height;
        // Destination image band numbers
        final int dnumBands = dst.getNumBands();
        // Destination bandOffsets, PixelStride and ScanLineStride
        final int dstBandOffsets[] = dst.getBandOffsets();
        final int dstPixelStride = dst.getPixelStride();
        final int dstScanlineStride = dst.getScanlineStride();

        // Destination and source data arrays (for all bands)
        final double[][] srcDataArrays = src.getDoubleDataArrays();

        final double[][] dstDataArrays = dst.getDoubleDataArrays();

        final byte[] roiDataArray;
        final int roiDataLength;
        if (useRoiAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
        } else {
            roiDataArray = null;
            roiDataLength = 0;
        }

        Range<Double> rangeND = (Range<Double>) noData;

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;
        if (caseA) {
            // for all bands
            for (int k = 0; k < dnumBands; k++) {

                final double[] srcData = srcDataArrays[k];
                final double[] dstData = dstDataArrays[k];
                // Line and band Offset initialization
                int dstlineOffset = dstBandOffsets[k];
                int bandOffset = bandOffsets[k];
                // cycle on the y values
                for (int j = 0; j < dheight; j++) {
                    // pixel offset initialization
                    int dstPixelOffset = dstlineOffset;
                    // y position selection
                    int posy = ypos[j] + bandOffset;
                    // Y offset initialization
                    int offsetY = (int) (4 * yfrac[j]);

                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        int posx = xpos[i];
                        int pos = posx + posy;

                        double sum = 0;
                        // X offset initialization
                        int offsetX = (int) (4 * xfrac[i]);
                        // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                        for (int h = 0; h < 4; h++) {
                            // Row temporary sum initialization
                            double temp = 0;
                            for (int z = 0; z < 4; z++) {
                                // Selection of one pixel
                                double pixelValue = srcData[pos + (z - 1) * srcPixelStride + (h - 1)
                                        * srcScanlineStride];
                                // Update of the temporary sum
                                temp += (pixelValue * dataHd[offsetX + z]);
                            }
                            // Vertical sum update
                            sum += temp *dataVd[offsetY + h];
                        }

                        // The interpolated value is saved in the destination array
                        dstData[dstPixelOffset] = sum;

                        // destination pixel offset update
                        dstPixelOffset += dstPixelStride;
                    }
                    // destination line offset update
                    dstlineOffset += dstScanlineStride;
                }
            }
        } else {
            if (caseB) {
                if (useRoiAccessor) {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {
                        final double[] srcData = srcDataArrays[k];
                        final double[] dstData = dstDataArrays[k];
                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            int posy = ypos[j] + bandOffset;
                            // Y offset initialization
                            int offsetY = (int) (4 * yfrac[j]);
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                int pos = posx + posy;

                                double[][] pixelKernel = new double[4][4];

                                final int baseIndex = (posx / dnumBands) + (yposRoi[j]);

                                // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                // Otherwise it takes the related value.
                                if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataDouble;
                                } else {
                                    int temp = 0;
                                    // X offset initialization
                                    int offsetX = (int) (4 * xfrac[i]);
                                    // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                    // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                    // and by roiscanlinestride on the y axis.
                                    for (int h = 0; h < 4; h++) {
                                        for (int z = 0; z < 4; z++) {
                                            // Selection of one pixel
                                            pixelKernel[h][z] = srcData[pos + (z - 1)
                                                    * srcPixelStride + (h - 1) * srcScanlineStride];
                                            int index = baseIndex - 1 + z + (h - 1)
                                                    * (roiScanlineStride);
                                            if (index < roiDataLength) {
                                                // Update of the weight sum
                                                temp += ((roiDataArray[index]) != 0 ? 1
                                                        : 0);
                                            }
                                        }
                                    }
                                    // Control if the 16 pixel are outside the ROI
                                    if (temp == 0) {
                                        dstData[dstPixelOffset] = destinationNoDataDouble;
                                    } else {
                                        double sum = 0;

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

                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = sum;
                                    }
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {
                        final double[] srcData = srcDataArrays[k];
                        final double[] dstData = dstDataArrays[k];
                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            int posy = ypos[j] + bandOffset;
                            // Y offset initialization
                            int offsetY = (int) (4 * yfrac[j]);
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                int pos = posx + posy;

                                // PixelPositions
                                int x0 = src.getX() + posx / srcPixelStride;
                                int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                double[][] pixelKernel = new double[4][4];

                                // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                // Otherwise it takes the related value.
                                if (roiBounds.contains(x0, y0)) {

                                    int temp = 0;
                                    // X offset initialization
                                    int offsetX = (int) (4 * xfrac[i]);
                                    // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                    // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                    // and by roiscanlinestride on the y axis.
                                    for (int h = 0; h < 4; h++) {
                                        for (int z = 0; z < 4; z++) {
                                            // Selection of one pixel
                                            pixelKernel[h][z] = srcData[pos + (z - 1)
                                                    * srcPixelStride + (h - 1) * srcScanlineStride];
                                            temp += roiIter.getSample(x0 + h - 1, y0 + z - 1, 0);
                                        }
                                    }
                                    // Control if the 16 pixel are outside the ROI
                                    if (temp == 0) {
                                        dstData[dstPixelOffset] = destinationNoDataDouble;
                                    } else {
                                        double sum = 0;

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

                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = sum;
                                    }
                                } else {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataDouble;
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }

                }
            } else {
                if (caseC) {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {

                        final double[] srcData = srcDataArrays[k];
                        final double[] dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            int posy = ypos[j] + bandOffset;
                            // Y offset initialization
                            int offsetY = (int) (4 * yfrac[j]);
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                int pos = posx + posy;

                                double[][] pixelKernel = new double[4][4];
                                int[][] weightArray = new int[4][4];
                                int[] weightArrayVertical = new int[4];

                                // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                // Otherwise it takes the related value.

                                int temp = 0;
                                // X offset initialization
                                int offsetX = (int) (4 * xfrac[i]);
                                // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                // and by roiscanlinestride on the y axis.
                                for (int h = 0; h < 4; h++) {
                                    for (int z = 0; z < 4; z++) {
                                        // Selection of one pixel
                                        pixelKernel[h][z] = srcData[pos + (z - 1) * srcPixelStride
                                                + (h - 1) * srcScanlineStride];

                                        if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                            if(pixelKernel[h][z] == Double.NEGATIVE_INFINITY || pixelKernel[h][z] == Double.NEGATIVE_INFINITY ||Double.isNaN(pixelKernel[h][z])){
                                             // The destination no data value is saved in the destination array
                                                weightArray[h][z] = 0;
                                            }else{
                                                temp++;
                                                weightArray[h][z] = 1;
                                            }
                                        }else if (rangeND.contains( pixelKernel[h][z])) {
                                            weightArray[h][z] = 0;
                                        }else{
                                            temp++;
                                            weightArray[h][z] = 1;
                                        }
                                    }
                                }
                                // Control if the 16 pixel are outside the ROI
                                if (temp == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataDouble;
                                } else {

                                    double[] sumArray = new double[4];

                                    double sum = 0;

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

                                    // The interpolated value is saved in the destination array
                                    dstData[dstPixelOffset] = sum;
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else if (hasROI && hasNoData) {
                    if (useRoiAccessor) {
                        // for all bands
                        for (int k = 0; k < dnumBands; k++) {

                            final double[] srcData = srcDataArrays[k];
                            final double[] dstData = dstDataArrays[k];
                            // Line and band Offset initialization
                            int dstlineOffset = dstBandOffsets[k];
                            int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                int posy = ypos[j] + bandOffset;
                                // Y offset initialization
                                int offsetY = (int) (4 * yfrac[j]);
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];

                                    int pos = posx + posy;

                                    double[][] pixelKernel = new double[4][4];
                                    int[][] weightArray = new int[4][4];
                                    int[] weightArrayVertical = new int[4];

                                    // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                    // Otherwise it takes the related value.

                                    final int baseIndex = (posx / dnumBands) + (yposRoi[j]);

                                    // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                    // Otherwise it takes the related value.
                                    if (baseIndex > roiDataLength || roiDataArray[baseIndex] == 0) {
                                        dstData[dstPixelOffset] = destinationNoDataDouble;
                                    } else {
                                        int tempND = 0;
                                        int tempROI = 0;
                                        // X offset initialization
                                        int offsetX = (int) (4 * xfrac[i]);
                                        // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                        // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                        // and by roiscanlinestride on the y axis.
                                        for (int h = 0; h < 4; h++) {
                                            for (int z = 0; z < 4; z++) {
                                                // Selection of one pixel
                                                pixelKernel[h][z] = srcData[pos + (z - 1)
                                                        * srcPixelStride + (h - 1)
                                                        * srcScanlineStride];

                                                int index = baseIndex - 1 + z + (h - 1)
                                                        * (roiScanlineStride);
                                                if (index < roiDataLength) {
                                                    // Update of the weight sum
                                                    tempROI += ((roiDataArray[index]) != 0 ? 1
                                                            : 0);
                                                }

                                                if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                                    if(pixelKernel[h][z] == Double.NEGATIVE_INFINITY || pixelKernel[h][z] == Double.NEGATIVE_INFINITY ||Double.isNaN(pixelKernel[h][z])){
                                                     // The destination no data value is saved in the destination array
                                                        weightArray[h][z] = 0;
                                                    }else{
                                                        tempND++;
                                                        weightArray[h][z] = 1;
                                                    }
                                                }else if (rangeND.contains(pixelKernel[h][z])) {
                                                    weightArray[h][z] = 0;
                                                }else{
                                                    tempND++;
                                                    weightArray[h][z] = 1;
                                                }
                                            }
                                        }
                                        // Control if the 16 pixel are outside the ROI
                                        if (tempND == 0 || tempROI == 0) {
                                            dstData[dstPixelOffset] = destinationNoDataDouble;
                                        } else {

                                            double[] sumArray = new double[4];

                                            double sum = 0;

                                            for (int h = 0; h < 4; h++) {
                                                // Row temporary sum initialization
                                                double tempSum = 0;
                                                double[] tempData = bicubicInpaintingDouble(pixelKernel[h],
                                                        weightArray[h], null);
                                                for (int z = 0; z < 4; z++) {
                                                    // Update of the temporary sum
                                                    tempSum += (tempData[z] * dataHd[offsetX
                                                            + z]);
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
                                            // The interpolated value is saved in the destination array
                                            dstData[dstPixelOffset] = sum;
                                        }

                                    }
                                    // destination pixel offset update
                                    dstPixelOffset += dstPixelStride;
                                }
                                // destination line offset update
                                dstlineOffset += dstScanlineStride;
                            }
                        }
                    } else {
                        // for all bands
                        for (int k = 0; k < dnumBands; k++) {

                            final double[] srcData = srcDataArrays[k];
                            final double[] dstData = dstDataArrays[k];
                            // Line and band Offset initialization
                            int dstlineOffset = dstBandOffsets[k];
                            int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                int posy = ypos[j] + bandOffset;
                                // Y offset initialization
                                int offsetY = (int) (4 * yfrac[j]);
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];

                                    // PixelPositions
                                    int x0 = src.getX() + posx / srcPixelStride;
                                    int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {

                                        int pos = posx + posy;

                                        double[][] pixelKernel = new double[4][4];
                                        int[][] weightArray = new int[4][4];
                                        int[] weightArrayVertical = new int[4];

                                        // Check if the selected index belongs to the roi data array: if it is not present, the weight is 0,
                                        // Otherwise it takes the related value.

                                        int tempND = 0;
                                        int tempROI = 0;
                                        // X offset initialization
                                        int offsetX = (int) (4 * xfrac[i]);
                                        // Cycle through all the 16 kernel pixel and calculation of the interpolated value
                                        // and cycle for filling all the ROI index by shifting of 1 on the x axis
                                        // and by roiscanlinestride on the y axis.
                                        for (int h = 0; h < 4; h++) {
                                            for (int z = 0; z < 4; z++) {
                                                // Selection of one pixel
                                                pixelKernel[h][z] = srcData[pos + (z - 1)
                                                        * srcPixelStride + (h - 1)
                                                        * srcScanlineStride];

                                                tempROI += roiIter.getSample(x0 + h - 1,
                                                        y0 + z - 1, 0);

                                                if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                                    if(pixelKernel[h][z] == Double.NEGATIVE_INFINITY || pixelKernel[h][z] == Double.NEGATIVE_INFINITY ||Double.isNaN(pixelKernel[h][z])){
                                                     // The destination no data value is saved in the destination array
                                                        weightArray[h][z] = 0;
                                                    }else{
                                                        tempND++;
                                                        weightArray[h][z] = 1;
                                                    }
                                                }else if (rangeND.contains(pixelKernel[h][z])) {
                                                    weightArray[h][z] = 0;
                                                }else{
                                                    tempND++;
                                                    weightArray[h][z] = 1;
                                                }
                                            }
                                        }

                                        // Control if the 16 pixel are outside the ROI
                                        if (tempND == 0 || tempROI == 0) {
                                            dstData[dstPixelOffset] = destinationNoDataDouble;
                                        } else {

                                            double[] sumArray = new double[4];

                                            double sum = 0;

                                            for (int h = 0; h < 4; h++) {
                                                // Row temporary sum initialization
                                                double tempSum = 0;
                                                double[] tempData = bicubicInpaintingDouble(pixelKernel[h],
                                                        weightArray[h], null);
                                                for (int z = 0; z < 4; z++) {
                                                    // Update of the temporary sum
                                                    tempSum += (tempData[z] * dataHd[offsetX
                                                            + z]);
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

                                            // The interpolated value is saved in the destination array
                                            dstData[dstPixelOffset] = sum;
                                        }

                                    } else {
                                        dstData[dstPixelOffset] = destinationNoDataDouble;
                                    }

                                    // destination pixel offset update
                                    dstPixelOffset += dstPixelStride;
                                }
                                // destination line offset update
                                dstlineOffset += dstScanlineStride;
                            }
                        }
                    }
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

    //This method is used for filling the no data values inside the interpolation kernel with the values of the adjacent pixels
    private double[] bicubicInpaintingDouble(double[] array, int[] weightArray, int[] weight0){
        double s_ = array[0];
        double s0 = array[1];
        double s1 = array[2];
        double s2 = array[3];
        
        if(weightArray == null){
                weightArray=new int[4];
                if(s_==0 && weight0[0]==0){
                        weightArray[0]=0;
                }else{
                        weightArray[0]=1;
                }
                if(s0==0 && weight0[1]==0){
                        weightArray[1]=0;
                }else{
                        weightArray[1]=1;
                }
                if(s1==0 && weight0[2]==0){
                        weightArray[2]=0;
                }else{
                        weightArray[2]=1;
                }
                if(s2==0 && weight0[3]==0){
                        weightArray[3]=0;
                }else{
                        weightArray[3]=1;
                }
        }
        
        //empty array containing the final values of the selected 4 pixels
        double[] emptyArray=new double[4];
        
        //Calculation of the number of data
        int sum = weightArray[0] + weightArray[1] + weightArray[2] + weightArray[3];
        // mean value used in calculations
        double meanValue=0;
        switch(sum){
        // All the 4 pixels are no data, an array of 0 data is returned
        case 0:
                return emptyArray;
                // Only one pixel is a valid data, all the pixel of the line have the same value.
        case 1:
                double validData=0;
                if(weightArray[0]==1){
                        validData=s_;
                }else if(weightArray[1]==1){
                        validData=s0;
                }else if(weightArray[2]==1){
                        validData=s1;
                }else{
                        validData=s2;
                }               
                emptyArray[0]=validData;
                emptyArray[1]=validData;
                emptyArray[2]=validData;
                emptyArray[3]=validData;                
                return emptyArray;
                // Only 2 pixels are valid data. If the No Data are on the border, they takes the value of the adjacent pixel,
        // else , they take an average of the 2 neighbor pixels with valid data. A String representation is provided for a better 
                // comprehension. 0 is no Data and x is valid data.
        case 2:
                
                // 0 0 x x
                if(weightArray[0]==0 && weightArray[1]==0){
                        emptyArray[0]=s1;
                        emptyArray[1]=s1;
                        emptyArray[2]=s1;
                        emptyArray[3]=s2;
                // 0 x 0 x
                }else if(weightArray[0]==0 && weightArray[2]==0){
                        meanValue= (s0 + s2)/2;
                        emptyArray[0]=s0;
                        emptyArray[1]=s0;
                        emptyArray[2]=meanValue;
                        emptyArray[3]=s2;
                // 0 x x 0
                }else if(weightArray[0]==0 && weightArray[3]==0){
                        emptyArray[0]=s0;
                        emptyArray[1]=s0;
                        emptyArray[2]=s1;
                        emptyArray[3]=s1;
                // x 0 0 x
                }else if(weightArray[1]==0 && weightArray[2]==0){
                        meanValue= (s_ + s2)/2;
                        emptyArray[0]=s_;
                        emptyArray[1]=meanValue;
                        emptyArray[2]=meanValue;
                        emptyArray[3]=s2;
                // x 0 x 0
                }else if(weightArray[1]==0 && weightArray[3]==0){
                        meanValue= (s_ + s1)/2;
                        emptyArray[0]=s_;
                        emptyArray[1]=meanValue;
                        emptyArray[2]=s1;
                        emptyArray[3]=s1;
                // x x 0 0
                }else{
                        emptyArray[0]=s_;
                        emptyArray[1]=s0;
                        emptyArray[2]=s0;
                        emptyArray[3]=s0;
                }
                return emptyArray;
        // Only one pixel is a No Data. If it is at the boundaries, then it replicates the value
        // of the adjacent pixel, else if takes an average of the 2 neighbor pixels.A String representation is provided for a better 
        // comprehension. 0 is no Data and x is valid data.
        case 3:                 
                // 0 x x x
                if(weightArray[0]==0){
                        emptyArray[0]=s0;
                        emptyArray[1]=s0;
                        emptyArray[2]=s1;
                        emptyArray[3]=s2;
                // x 0 x x
                }else if(weightArray[1]==0){
                        meanValue= (s_ + s1)/2;
                        emptyArray[0]=s_;
                        emptyArray[1]=meanValue;
                        emptyArray[2]=s1;
                        emptyArray[3]=s2;
                // x x 0 x
                }else if(weightArray[2]==0){
                        meanValue= (s0 + s2)/2;
                        emptyArray[0]=s_;
                        emptyArray[1]=s0;
                        emptyArray[2]=meanValue;
                        emptyArray[3]=s2;
                // x x x 0
                }else{
                        emptyArray[0]=s_;
                        emptyArray[1]=s0;
                        emptyArray[2]=s1;
                        emptyArray[3]=s1;
                } 
                return emptyArray;
                // Absence of No Data, the pixels are returned.
        case 4:
                emptyArray[0]=s_;
                emptyArray[1]=s0;
                emptyArray[2]=s1;
                emptyArray[3]=s2;               
                return emptyArray;
                default:
                        throw new IllegalArgumentException("The input array cannot have more than 4 pixels");
        }
    }    
}
