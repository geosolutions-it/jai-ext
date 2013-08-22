package it.geosolutions.jaiext.scale;

import it.geosolutions.jaiext.interpolators.InterpolationBilinear;

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

public class ScaleBilinearOpImage extends ScaleOpImage {
    /**Byte lookuptable used if no data are present*/
    protected final byte[] byteLookupTable = new byte[255];

    /** Bilinear interpolator */
    protected InterpolationBilinear interpB = null;

    public ScaleBilinearOpImage(RenderedImage source, ImageLayout layout, Map configuration,
            BorderExtender extender, Interpolation interp, float scaleX, float scaleY,
            float transX, float transY, boolean useRoiAccessor) {
        super(source, layout, configuration, true, extender, interp
                , scaleX, scaleY, transX, transY, useRoiAccessor);
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

        if (interpolator instanceof InterpolationBilinear) {
            isBilinearNew = true;
            interpB = (InterpolationBilinear) interpolator;
            this.interp = interpB;
            interpB.setROIdata(roiBounds, roiIter);
            noData = interpB.getNoDataRange();
            if (noData != null) {
                hasNoData = true;
                destinationNoDataDouble = interpB.getDestinationNoData();
                if ((srcDataType == DataBuffer.TYPE_FLOAT || srcDataType == DataBuffer.TYPE_DOUBLE)) {
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
                destinationNoDataDouble = interpB.getDestinationNoData();
            }
        }
        // subsample bits used for the bilinear and bicubic interpolation
        subsampleBits = interp.getSubsampleBitsH();

        // Internal precision required for position calculations
        one = 1 << subsampleBits;

        // Subsampling related variables
        shift2 = 2 * subsampleBits;
        round2 = 1 << (shift2 - 1);

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
                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        int posx = xpos[i];
                        int pos = posx + posy;

                        final int s00 = srcData[pos] & 0xff;
                        final int s01 = srcData[pos + srcPixelStride] & 0xff;
                        final int s10 = srcData[pos + srcScanlineStride] & 0xff;
                        final int s11 = srcData[pos + srcPixelStride + srcScanlineStride] & 0xff;

                        // Perform the bilinear interpolation
                        final int s0 = (s01 - s00) * xfrac[i] + (s00 << subsampleBits);
                        final int s1 = (s11 - s10) * xfrac[i] + (s10 << subsampleBits);
                        final int s = ((s1 - s0) * yfrac[j] + (s0 << subsampleBits) + round2) >> shift2;

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
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                int pos = posx + posy;

                                final int s00 = srcData[pos] & 0xff;
                                final int s01 = srcData[pos + srcPixelStride] & 0xff;
                                final int s10 = srcData[pos + srcScanlineStride] & 0xff;
                                final int s11 = srcData[pos + srcPixelStride + srcScanlineStride] & 0xff;

                                final int baseIndex = (posx / dnumBands) + (yposRoi[j]);

                                final int w00index = baseIndex;
                                final int w01index = baseIndex + 1;
                                final int w10index = baseIndex + roiScanlineStride;
                                final int w11index = baseIndex + 1 + roiScanlineStride;

                                final int w00 = w00index < roiDataLength ? roiDataArray[w00index] & 0xff
                                        : 0;
                                final int w01 = w01index < roiDataLength ? roiDataArray[w01index] & 0xff
                                        : 0;
                                final int w10 = w10index < roiDataLength ? roiDataArray[w10index] & 0xff
                                        : 0;
                                final int w11 = w11index < roiDataLength ? roiDataArray[w11index] & 0xff
                                        : 0;

                                if (baseIndex > roiDataLength || w00 == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataByte;
                                } else if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataByte;
                                } else {
                                    // Perform the bilinear interpolation
                                    final int s0 = (s01 - s00) * xfrac[i] + (s00 << subsampleBits);
                                    final int s1 = (s11 - s10) * xfrac[i] + (s10 << subsampleBits);
                                    final int s = ((s1 - s0) * yfrac[j] + (s0 << subsampleBits) + round2) >> shift2;

                                    // The interpolated value is saved in the destination array
                                    dstData[dstPixelOffset] = (byte) (s & 0xff);
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
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];
                                // PixelPositions
                                int x0 = src.getX() + posx / srcPixelStride;
                                int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                if (roiBounds.contains(x0, y0)) {

                                    final int w00 = roiIter.getSample(x0, y0, 0);
                                    final int w01 = roiIter.getSample(x0 + 1, y0, 0);
                                    final int w10 = roiIter.getSample(x0, y0 + 1, 0);
                                    final int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0);

                                    if (!(w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0)) {

                                        // Get the four surrounding pixel values
                                        final int s00 = srcData[posx + posy] & 0xff;
                                        final int s01 = srcData[posx + srcPixelStride + posy] & 0xff;
                                        final int s10 = srcData[posx + posy + srcScanlineStride] & 0xff;
                                        final int s11 = srcData[posx + srcPixelStride + posy
                                                + srcScanlineStride] & 0xff;
                                        // Perform the bilinear interpolation
                                        final int s0 = (s01 - s00) * xfrac[i]
                                                + (s00 << subsampleBits);
                                        final int s1 = (s11 - s10) * xfrac[i]
                                                + (s10 << subsampleBits);
                                        final int s = ((s1 - s0) * yfrac[j] + (s0 << subsampleBits) + round2) >> shift2;

                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = (byte) (s & 0xff);

                                    } else {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataByte;
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
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                // Get the four surrounding pixel values
                                final int s00 = srcData[posx + posy] & 0xff;
                                final int s01 = srcData[posx + srcPixelStride + posy] & 0xff;
                                final int s10 = srcData[posx + posy + srcScanlineStride] & 0xff;
                                final int s11 = srcData[posx + srcPixelStride + posy
                                        + srcScanlineStride] & 0xff;

                                int w00 = 1;
                                int w01 = 1;
                                int w10 = 1;
                                int w11 = 1;

                                if (byteLookupTable[s00] == destinationNoDataByte) {
                                    w00 = 0;
                                }
                                if (byteLookupTable[s01] == destinationNoDataByte) {
                                    w01 = 0;
                                }
                                if (byteLookupTable[s10] == destinationNoDataByte) {
                                    w10 = 0;
                                }
                                if (byteLookupTable[s11] == destinationNoDataByte) {
                                    w11 = 0;
                                }

                                if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataByte;
                                } else {
                                    // compute value
                                    dstData[dstPixelOffset] = (byte) (computeValue(s00, s01, s10,
                                            s11, w00, w01, w10, w11, xfrac[i], yfrac[j]) & 0xff);
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
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];

                                    // Get the four surrounding pixel values
                                    final int s00 = srcData[posx + posy] & 0xff;
                                    final int s01 = srcData[posx + srcPixelStride + posy] & 0xff;
                                    final int s10 = srcData[posx + posy + srcScanlineStride] & 0xff;
                                    final int s11 = srcData[posx + srcPixelStride + posy
                                            + srcScanlineStride] & 0xff;

                                    final int baseIndex = (posx / dnumBands) + (yposRoi[j]);

                                    final int w00index = baseIndex;
                                    final int w01index = baseIndex + 1;
                                    final int w10index = baseIndex + roiScanlineStride;
                                    final int w11index = baseIndex + 1 + roiScanlineStride;

                                    int w00 = w00index < roiDataLength ? roiDataArray[w00index] & 0xff
                                            : 0;
                                    int w01 = w01index < roiDataLength ? roiDataArray[w01index] & 0xff
                                            : 0;
                                    int w10 = w10index < roiDataLength ? roiDataArray[w10index] & 0xff
                                            : 0;
                                    int w11 = w11index < roiDataLength ? roiDataArray[w11index] & 0xff
                                            : 0;

                                    if (baseIndex > roiDataLength || w00 == 0) {
                                        dstData[dstPixelOffset] = destinationNoDataByte;
                                    } else if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataByte;
                                    } else {

                                        if (byteLookupTable[s00] == destinationNoDataByte) {
                                            w00 = 0;
                                        } else {
                                            w00 = 1;
                                        }
                                        if (byteLookupTable[s01] == destinationNoDataByte) {
                                            w01 = 0;
                                        } else {
                                            w01 = 1;
                                        }
                                        if (byteLookupTable[s10] == destinationNoDataByte) {
                                            w10 = 0;
                                        } else {
                                            w10 = 1;
                                        }
                                        if (byteLookupTable[s11] == destinationNoDataByte) {
                                            w11 = 0;
                                        } else {
                                            w11 = 1;
                                        }

                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = (byte) (computeValue(s00, s01,
                                                s10, s11, w00, w01, w10, w11, xfrac[i], yfrac[j]) & 0xff);
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
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];

                                    // PixelPositions
                                    int x0 = src.getX() + posx / srcPixelStride;
                                    int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {

                                        int w00 = roiIter.getSample(x0, y0, 0);
                                        int w01 = roiIter.getSample(x0 + 1, y0, 0);
                                        int w10 = roiIter.getSample(x0, y0 + 1, 0);
                                        int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0);

                                        if (!(w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0)) {

                                            // Get the four surrounding pixel values
                                            final int s00 = srcData[posx + posy] & 0xff;
                                            final int s01 = srcData[posx + srcPixelStride + posy] & 0xff;
                                            final int s10 = srcData[posx + posy + srcScanlineStride] & 0xff;
                                            final int s11 = srcData[posx + srcPixelStride + posy
                                                    + srcScanlineStride] & 0xff;

                                            if (byteLookupTable[s00] == destinationNoDataByte) {
                                                w00 = 0;
                                            } else {
                                                w00 = 1;
                                            }
                                            if (byteLookupTable[s01] == destinationNoDataByte) {
                                                w01 = 0;
                                            } else {
                                                w01 = 1;
                                            }
                                            if (byteLookupTable[s10] == destinationNoDataByte) {
                                                w10 = 0;
                                            } else {
                                                w10 = 1;
                                            }
                                            if (byteLookupTable[s11] == destinationNoDataByte) {
                                                w11 = 0;
                                            } else {
                                                w11 = 1;
                                            }

                                            // compute value
                                            dstData[dstPixelOffset] = (byte) (computeValue(s00,
                                                    s01, s10, s11, w00, w01, w10, w11, xfrac[i],
                                                    yfrac[j]) & 0xff);

                                        } else {
                                            // The destination no data value is saved in the destination array
                                            dstData[dstPixelOffset] = destinationNoDataByte;
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

        // No Data Range
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
                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        int posx = xpos[i];
                        int pos = posx + posy;

                        final int s00 = srcData[pos] & 0xffff;
                        final int s01 = srcData[pos + srcPixelStride] & 0xffff;
                        final int s10 = srcData[pos + srcScanlineStride] & 0xffff;
                        final int s11 = srcData[pos + srcPixelStride + srcScanlineStride] & 0xffff;

                        // Perform the bilinear interpolation
                        final int s0 = (s01 - s00) * xfrac[i] + (s00 << subsampleBits);
                        final int s1 = (s11 - s10) * xfrac[i] + (s10 << subsampleBits);
                        final int s = ((s1 - s0) * yfrac[j] + (s0 << subsampleBits) + round2) >> shift2;

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
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                int pos = posx + posy;

                                final int s00 = srcData[pos] & 0xffff;
                                final int s01 = srcData[pos + srcPixelStride] & 0xffff;
                                final int s10 = srcData[pos + srcScanlineStride] & 0xffff;
                                final int s11 = srcData[pos + srcPixelStride + srcScanlineStride] & 0xffff;

                                final int baseIndex = (posx / dnumBands) + (yposRoi[j]);

                                final int w00index = baseIndex;
                                final int w01index = baseIndex + 1;
                                final int w10index = baseIndex + roiScanlineStride;
                                final int w11index = baseIndex + 1 + roiScanlineStride;

                                final int w00 = w00index < roiDataLength ? roiDataArray[w00index] & 0xffff
                                        : 0;
                                final int w01 = w01index < roiDataLength ? roiDataArray[w01index] & 0xffff
                                        : 0;
                                final int w10 = w10index < roiDataLength ? roiDataArray[w10index] & 0xffff
                                        : 0;
                                final int w11 = w11index < roiDataLength ? roiDataArray[w11index] & 0xffff
                                        : 0;

                                if (baseIndex > roiDataLength || w00 == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataUShort;
                                } else if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataUShort;
                                } else {
                                    // Perform the bilinear interpolation
                                    final int s0 = (s01 - s00) * xfrac[i] + (s00 << subsampleBits);
                                    final int s1 = (s11 - s10) * xfrac[i] + (s10 << subsampleBits);
                                    final int s = ((s1 - s0) * yfrac[j] + (s0 << subsampleBits) + round2) >> shift2;

                                    // The interpolated value is saved in the destination array
                                    dstData[dstPixelOffset] = (short) (s & 0xffff);
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
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];
                                // PixelPositions
                                int x0 = src.getX() + posx / srcPixelStride;
                                int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                if (roiBounds.contains(x0, y0)) {

                                    final int w00 = roiIter.getSample(x0, y0, 0);
                                    final int w01 = roiIter.getSample(x0 + 1, y0, 0);
                                    final int w10 = roiIter.getSample(x0, y0 + 1, 0);
                                    final int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0);

                                    if (!(w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0)) {

                                        // Get the four surrounding pixel values
                                        final int s00 = srcData[posx + posy] & 0xffff;
                                        final int s01 = srcData[posx + srcPixelStride + posy] & 0xffff;
                                        final int s10 = srcData[posx + posy + srcScanlineStride] & 0xffff;
                                        final int s11 = srcData[posx + srcPixelStride + posy
                                                + srcScanlineStride] & 0xffff;
                                        // Perform the bilinear interpolation
                                        final int s0 = (s01 - s00) * xfrac[i]
                                                + (s00 << subsampleBits);
                                        final int s1 = (s11 - s10) * xfrac[i]
                                                + (s10 << subsampleBits);
                                        final int s = ((s1 - s0) * yfrac[j] + (s0 << subsampleBits) + round2) >> shift2;

                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = (short) (s & 0xffff);

                                    } else {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataUShort;
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
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                // Get the four surrounding pixel values
                                final short s00 = (short) (srcData[posx + posy] & 0xffff);
                                final short s01 = (short) (srcData[posx + srcPixelStride + posy] & 0xffff);
                                final short s10 = (short) (srcData[posx + posy + srcScanlineStride] & 0xffff);
                                final short s11 = (short) (srcData[posx + srcPixelStride + posy
                                        + srcScanlineStride] & 0xffff);

                                int w00 = 1;
                                int w01 = 1;
                                int w10 = 1;
                                int w11 = 1;

                                if (rangeND.contains(s00)) {
                                    w00 = 0;
                                }
                                if (rangeND.contains(s01)) {
                                    w01 = 0;
                                }
                                if (rangeND.contains(s10)) {
                                    w10 = 0;
                                }
                                if (rangeND.contains(s11)) {
                                    w11 = 0;
                                }

                                if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataUShort;
                                } else {
                                    // compute value
                                    dstData[dstPixelOffset] = (short) (computeValue(s00, s01, s10,
                                            s11, w00, w01, w10, w11, xfrac[i], yfrac[j]) & 0xffff);
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
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];

                                    // Get the four surrounding pixel values
                                    final short s00 = (short) (srcData[posx + posy] & 0xffff);
                                    final short s01 = (short) (srcData[posx + srcPixelStride + posy] & 0xffff);
                                    final short s10 = (short) (srcData[posx + posy
                                            + srcScanlineStride] & 0xffff);
                                    final short s11 = (short) (srcData[posx + srcPixelStride + posy
                                            + srcScanlineStride] & 0xffff);

                                    final int baseIndex = (posx / dnumBands) + (yposRoi[j]);

                                    final int w00index = baseIndex;
                                    final int w01index = baseIndex + 1;
                                    final int w10index = baseIndex + roiScanlineStride;
                                    final int w11index = baseIndex + 1 + roiScanlineStride;

                                    int w00 = w00index < roiDataLength ? roiDataArray[w00index] & 0xffff
                                            : 0;
                                    int w01 = w01index < roiDataLength ? roiDataArray[w01index] & 0xffff
                                            : 0;
                                    int w10 = w10index < roiDataLength ? roiDataArray[w10index] & 0xffff
                                            : 0;
                                    int w11 = w11index < roiDataLength ? roiDataArray[w11index] & 0xffff
                                            : 0;

                                    if (baseIndex > roiDataLength || w00 == 0) {
                                        dstData[dstPixelOffset] = destinationNoDataUShort;
                                    } else if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataUShort;
                                    } else {
                                        if (rangeND.contains(s00)) {
                                            w00 = 0;
                                        } else {
                                            w00 = 1;
                                        }
                                        if (rangeND.contains(s01)) {
                                            w01 = 0;
                                        } else {
                                            w01 = 1;
                                        }
                                        if (rangeND.contains(s10)) {
                                            w10 = 0;
                                        } else {
                                            w10 = 1;
                                        }
                                        if (rangeND.contains(s11)) {
                                            w11 = 0;
                                        } else {
                                            w11 = 1;
                                        }

                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = (short) (computeValue(s00, s01,
                                                s10, s11, w00, w01, w10, w11, xfrac[i], yfrac[j]) & 0xffff);
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
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];

                                    // PixelPositions
                                    int x0 = src.getX() + posx / srcPixelStride;
                                    int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {

                                        int w00 = roiIter.getSample(x0, y0, 0);
                                        int w01 = roiIter.getSample(x0 + 1, y0, 0);
                                        int w10 = roiIter.getSample(x0, y0 + 1, 0);
                                        int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0);

                                        if (!(w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0)) {

                                            // Get the four surrounding pixel values
                                            final short s00 = (short) (srcData[posx + posy] & 0xffff);
                                            final short s01 = (short) (srcData[posx
                                                    + srcPixelStride + posy] & 0xffff);
                                            final short s10 = (short) (srcData[posx + posy
                                                    + srcScanlineStride] & 0xffff);
                                            final short s11 = (short) (srcData[posx
                                                    + srcPixelStride + posy + srcScanlineStride] & 0xffff);
                                            if (rangeND.contains(s00)) {
                                                w00 = 0;
                                            } else {
                                                w00 = 1;
                                            }
                                            if (rangeND.contains(s01)) {
                                                w01 = 0;
                                            } else {
                                                w01 = 1;
                                            }
                                            if (rangeND.contains(s10)) {
                                                w10 = 0;
                                            } else {
                                                w10 = 1;
                                            }
                                            if (rangeND.contains(s11)) {
                                                w11 = 0;
                                            } else {
                                                w11 = 1;
                                            }

                                            // compute value
                                            dstData[dstPixelOffset] = (short) (computeValue(s00,
                                                    s01, s10, s11, w00, w01, w10, w11, xfrac[i],
                                                    yfrac[j]) & 0xffff);

                                        } else {
                                            // The destination no data value is saved in the destination array
                                            dstData[dstPixelOffset] = destinationNoDataUShort;
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
                }
            }
        }
    }

    private void shortLoop(RasterAccessor src, Rectangle dstRect, RasterAccessor dst, int[] xpos,
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

        // No Data Range
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
                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        int posx = xpos[i];
                        int pos = posx + posy;

                        final int s00 = srcData[pos];
                        final int s01 = srcData[pos + srcPixelStride];
                        final int s10 = srcData[pos + srcScanlineStride];
                        final int s11 = srcData[pos + srcPixelStride + srcScanlineStride];

                        // Perform the bilinear interpolation
                        final int s0 = (s01 - s00) * xfrac[i] + (s00 << subsampleBits);
                        final int s1 = (s11 - s10) * xfrac[i] + (s10 << subsampleBits);
                        final int s = ((s1 - s0) * yfrac[j] + (s0 << subsampleBits) + round2) >> shift2;

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
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                int pos = posx + posy;

                                final int s00 = srcData[pos];
                                final int s01 = srcData[pos + srcPixelStride];
                                final int s10 = srcData[pos + srcScanlineStride];
                                final int s11 = srcData[pos + srcPixelStride + srcScanlineStride];

                                final int baseIndex = (posx / dnumBands) + (yposRoi[j]);

                                final int w00index = baseIndex;
                                final int w01index = baseIndex + 1;
                                final int w10index = baseIndex + roiScanlineStride;
                                final int w11index = baseIndex + 1 + roiScanlineStride;

                                final int w00 = w00index < roiDataLength ? roiDataArray[w00index]
                                        : 0;
                                final int w01 = w01index < roiDataLength ? roiDataArray[w01index]
                                        : 0;
                                final int w10 = w10index < roiDataLength ? roiDataArray[w10index]
                                        : 0;
                                final int w11 = w11index < roiDataLength ? roiDataArray[w11index]
                                        : 0;

                                if (baseIndex > roiDataLength || w00 == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataShort;
                                } else if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataShort;
                                } else {
                                    // Perform the bilinear interpolation
                                    final int s0 = (s01 - s00) * xfrac[i] + (s00 << subsampleBits);
                                    final int s1 = (s11 - s10) * xfrac[i] + (s10 << subsampleBits);
                                    final int s = ((s1 - s0) * yfrac[j] + (s0 << subsampleBits) + round2) >> shift2;

                                    // The interpolated value is saved in the destination array
                                    dstData[dstPixelOffset] = (short) s;
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
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];
                                // PixelPositions
                                int x0 = src.getX() + posx / srcPixelStride;
                                int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                if (roiBounds.contains(x0, y0)) {

                                    final int w00 = roiIter.getSample(x0, y0, 0);
                                    final int w01 = roiIter.getSample(x0 + 1, y0, 0);
                                    final int w10 = roiIter.getSample(x0, y0 + 1, 0);
                                    final int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0);

                                    if (!(w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0)) {

                                        // Get the four surrounding pixel values
                                        final int s00 = srcData[posx + posy];
                                        final int s01 = srcData[posx + srcPixelStride + posy];
                                        final int s10 = srcData[posx + posy + srcScanlineStride];
                                        final int s11 = srcData[posx + srcPixelStride + posy
                                                + srcScanlineStride];
                                        // Perform the bilinear interpolation
                                        final int s0 = (s01 - s00) * xfrac[i]
                                                + (s00 << subsampleBits);
                                        final int s1 = (s11 - s10) * xfrac[i]
                                                + (s10 << subsampleBits);
                                        final int s = ((s1 - s0) * yfrac[j] + (s0 << subsampleBits) + round2) >> shift2;

                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = (short) s;

                                    } else {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataShort;
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
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                // Get the four surrounding pixel values
                                final short s00 = srcData[posx + posy];
                                final short s01 = srcData[posx + srcPixelStride + posy];
                                final short s10 = srcData[posx + posy + srcScanlineStride];
                                final short s11 = srcData[posx + srcPixelStride + posy
                                        + srcScanlineStride];

                                int w00 = 1;
                                int w01 = 1;
                                int w10 = 1;
                                int w11 = 1;

                                if (rangeND.contains(s00)) {
                                    w00 = 0;
                                }
                                if (rangeND.contains(s01)) {
                                    w01 = 0;
                                }
                                if (rangeND.contains(s10)) {
                                    w10 = 0;
                                }
                                if (rangeND.contains(s11)) {
                                    w11 = 0;
                                }

                                if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataShort;
                                } else {
                                    // compute value
                                    dstData[dstPixelOffset] = (short) (computeValue(s00, s01, s10,
                                            s11, w00, w01, w10, w11, xfrac[i], yfrac[j]));
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
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];

                                    // Get the four surrounding pixel values
                                    final short s00 = srcData[posx + posy];
                                    final short s01 = srcData[posx + srcPixelStride + posy];
                                    final short s10 = srcData[posx + posy + srcScanlineStride];
                                    final short s11 = srcData[posx + srcPixelStride + posy
                                            + srcScanlineStride];

                                    final int baseIndex = (posx / dnumBands) + (yposRoi[j]);

                                    final int w00index = baseIndex;
                                    final int w01index = baseIndex + 1;
                                    final int w10index = baseIndex + roiScanlineStride;
                                    final int w11index = baseIndex + 1 + roiScanlineStride;

                                    int w00 = w00index < roiDataLength ? roiDataArray[w00index] : 0;
                                    int w01 = w01index < roiDataLength ? roiDataArray[w01index] : 0;
                                    int w10 = w10index < roiDataLength ? roiDataArray[w10index] : 0;
                                    int w11 = w11index < roiDataLength ? roiDataArray[w11index] : 0;

                                    if (baseIndex > roiDataLength || w00 == 0) {
                                        dstData[dstPixelOffset] = destinationNoDataShort;
                                    } else if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataShort;
                                    } else {
                                        if (rangeND.contains(s00)) {
                                            w00 = 0;
                                        } else {
                                            w00 = 1;
                                        }
                                        if (rangeND.contains(s01)) {
                                            w01 = 0;
                                        } else {
                                            w01 = 1;
                                        }
                                        if (rangeND.contains(s10)) {
                                            w10 = 0;
                                        } else {
                                            w10 = 1;
                                        }
                                        if (rangeND.contains(s11)) {
                                            w11 = 0;
                                        } else {
                                            w11 = 1;
                                        }

                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = (short) (computeValue(s00, s01,
                                                s10, s11, w00, w01, w10, w11, xfrac[i], yfrac[j]));
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
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];

                                    // PixelPositions
                                    int x0 = src.getX() + posx / srcPixelStride;
                                    int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {

                                        int w00 = roiIter.getSample(x0, y0, 0);
                                        int w01 = roiIter.getSample(x0 + 1, y0, 0);
                                        int w10 = roiIter.getSample(x0, y0 + 1, 0);
                                        int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0);

                                        if (!(w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0)) {

                                            // Get the four surrounding pixel values
                                            final short s00 = srcData[posx + posy];
                                            final short s01 = srcData[posx + srcPixelStride + posy];
                                            final short s10 = srcData[posx + posy
                                                    + srcScanlineStride];
                                            final short s11 = srcData[posx + srcPixelStride + posy
                                                    + srcScanlineStride];

                                            if (rangeND.contains(s00)) {
                                                w00 = 0;
                                            } else {
                                                w00 = 1;
                                            }
                                            if (rangeND.contains(s01)) {
                                                w01 = 0;
                                            } else {
                                                w01 = 1;
                                            }
                                            if (rangeND.contains(s10)) {
                                                w10 = 0;
                                            } else {
                                                w10 = 1;
                                            }
                                            if (rangeND.contains(s11)) {
                                                w11 = 0;
                                            } else {
                                                w11 = 1;
                                            }

                                            // compute value
                                            dstData[dstPixelOffset] = (short) (computeValue(s00,
                                                    s01, s10, s11, w00, w01, w10, w11, xfrac[i],
                                                    yfrac[j]));

                                        } else {
                                            // The destination no data value is saved in the destination array
                                            dstData[dstPixelOffset] = destinationNoDataShort;
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
                }
            }
        }
    }

    private void intLoop(RasterAccessor src, Rectangle dstRect, RasterAccessor dst, int[] xpos,
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

        // No Data Range
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
                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        int posx = xpos[i];
                        int pos = posx + posy;

                        final int s00 = srcData[pos];
                        final int s01 = srcData[pos + srcPixelStride];
                        final int s10 = srcData[pos + srcScanlineStride];
                        final int s11 = srcData[pos + srcPixelStride + srcScanlineStride];

                        // Perform the bilinear interpolation
                        final int s0 = (s01 - s00) * xfrac[i] + (s00 << subsampleBits);
                        final int s1 = (s11 - s10) * xfrac[i] + (s10 << subsampleBits);
                        final int s = ((s1 - s0) * yfrac[j] + (s0 << subsampleBits) + round2) >> shift2;

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
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                int pos = posx + posy;

                                final int s00 = srcData[pos];
                                final int s01 = srcData[pos + srcPixelStride];
                                final int s10 = srcData[pos + srcScanlineStride];
                                final int s11 = srcData[pos + srcPixelStride + srcScanlineStride];

                                final int baseIndex = (posx / dnumBands) + (yposRoi[j]);

                                final int w00index = baseIndex;
                                final int w01index = baseIndex + 1;
                                final int w10index = baseIndex + roiScanlineStride;
                                final int w11index = baseIndex + 1 + roiScanlineStride;

                                final int w00 = w00index < roiDataLength ? roiDataArray[w00index]
                                        : 0;
                                final int w01 = w01index < roiDataLength ? roiDataArray[w01index]
                                        : 0;
                                final int w10 = w10index < roiDataLength ? roiDataArray[w10index]
                                        : 0;
                                final int w11 = w11index < roiDataLength ? roiDataArray[w11index]
                                        : 0;

                                if (baseIndex > roiDataLength || w00 == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataInt;
                                } else if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataInt;
                                } else {
                                    // The interpolated value is saved in the destination array
                                    dstData[dstPixelOffset] = (computeValue(s00, s01, s10, s11, 1,
                                            1, 1, 1, xfrac[i], yfrac[j]));
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
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];
                                // PixelPositions
                                int x0 = src.getX() + posx / srcPixelStride;
                                int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                if (roiBounds.contains(x0, y0)) {

                                    final int w00 = roiIter.getSample(x0, y0, 0);
                                    final int w01 = roiIter.getSample(x0 + 1, y0, 0);
                                    final int w10 = roiIter.getSample(x0, y0 + 1, 0);
                                    final int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0);

                                    if (!(w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0)) {

                                        // Get the four surrounding pixel values
                                        final int s00 = srcData[posx + posy];
                                        final int s01 = srcData[posx + srcPixelStride + posy];
                                        final int s10 = srcData[posx + posy + srcScanlineStride];
                                        final int s11 = srcData[posx + srcPixelStride + posy
                                                + srcScanlineStride];
                                        // compute value
                                        dstData[dstPixelOffset] = (computeValue(s00, s01, s10, s11,
                                                1, 1, 1, 1, xfrac[i], yfrac[j]));

                                    } else {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataInt;
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
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                // Get the four surrounding pixel values
                                final int s00 = srcData[posx + posy];
                                final int s01 = srcData[posx + srcPixelStride + posy];
                                final int s10 = srcData[posx + posy + srcScanlineStride];
                                final int s11 = srcData[posx + srcPixelStride + posy
                                        + srcScanlineStride];

                                int w00 = 1;
                                int w01 = 1;
                                int w10 = 1;
                                int w11 = 1;

                                if (rangeND.contains(s00)) {
                                    w00 = 0;
                                }
                                if (rangeND.contains(s01)) {
                                    w01 = 0;
                                }
                                if (rangeND.contains(s10)) {
                                    w10 = 0;
                                }
                                if (rangeND.contains(s11)) {
                                    w11 = 0;
                                }

                                if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataInt;
                                } else {
                                    // compute value
                                    dstData[dstPixelOffset] = (computeValue(s00, s01, s10, s11,
                                            w00, w01, w10, w11, xfrac[i], yfrac[j]));
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
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];

                                    // Get the four surrounding pixel values
                                    final int s00 = srcData[posx + posy];
                                    final int s01 = srcData[posx + srcPixelStride + posy];
                                    final int s10 = srcData[posx + posy + srcScanlineStride];
                                    final int s11 = srcData[posx + srcPixelStride + posy
                                            + srcScanlineStride];

                                    final int baseIndex = (posx / dnumBands) + (yposRoi[j]);

                                    final int w00index = baseIndex;
                                    final int w01index = baseIndex + 1;
                                    final int w10index = baseIndex + roiScanlineStride;
                                    final int w11index = baseIndex + 1 + roiScanlineStride;

                                    int w00 = w00index < roiDataLength ? roiDataArray[w00index] : 0;
                                    int w01 = w01index < roiDataLength ? roiDataArray[w01index] : 0;
                                    int w10 = w10index < roiDataLength ? roiDataArray[w10index] : 0;
                                    int w11 = w11index < roiDataLength ? roiDataArray[w11index] : 0;

                                    if (baseIndex > roiDataLength || w00 == 0) {
                                        dstData[dstPixelOffset] = destinationNoDataInt;
                                    } else if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        if (rangeND.contains(s00)) {
                                            w00 = 0;
                                        } else {
                                            w00 = 1;
                                        }
                                        if (rangeND.contains(s01)) {
                                            w01 = 0;
                                        } else {
                                            w01 = 1;
                                        }
                                        if (rangeND.contains(s10)) {
                                            w10 = 0;
                                        } else {
                                            w10 = 1;
                                        }
                                        if (rangeND.contains(s11)) {
                                            w11 = 0;
                                        } else {
                                            w11 = 1;
                                        }

                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = (computeValue(s00, s01, s10, s11,
                                                w00, w01, w10, w11, xfrac[i], yfrac[j]));
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
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];

                                    // PixelPositions
                                    int x0 = src.getX() + posx / srcPixelStride;
                                    int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {

                                        int w00 = roiIter.getSample(x0, y0, 0);
                                        int w01 = roiIter.getSample(x0 + 1, y0, 0);
                                        int w10 = roiIter.getSample(x0, y0 + 1, 0);
                                        int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0);

                                        if (!(w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0)) {

                                            // Get the four surrounding pixel values
                                            final int s00 = srcData[posx + posy];
                                            final int s01 = srcData[posx + srcPixelStride + posy];
                                            final int s10 = srcData[posx + posy + srcScanlineStride];
                                            final int s11 = srcData[posx + srcPixelStride + posy
                                                    + srcScanlineStride];

                                            if (rangeND.contains(s00)) {
                                                w00 = 0;
                                            } else {
                                                w00 = 1;
                                            }
                                            if (rangeND.contains(s01)) {
                                                w01 = 0;
                                            } else {
                                                w01 = 1;
                                            }
                                            if (rangeND.contains(s10)) {
                                                w10 = 0;
                                            } else {
                                                w10 = 1;
                                            }
                                            if (rangeND.contains(s11)) {
                                                w11 = 0;
                                            } else {
                                                w11 = 1;
                                            }

                                            // compute value
                                            dstData[dstPixelOffset] = (computeValue(s00, s01, s10,
                                                    s11, w00, w01, w10, w11, xfrac[i], yfrac[j]));

                                        } else {
                                            // The destination no data value is saved in the destination array
                                            dstData[dstPixelOffset] = destinationNoDataInt;
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
                }
            }
        }

    }

    private void floatLoop(RasterAccessor src, Rectangle dstRect, RasterAccessor dst, int[] xpos,
            int[] ypos, float[] xfrac, float[] yfrac, RasterAccessor roi, int[] yposRoi,
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

        // No Data Range
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
                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        int posx = xpos[i];
                        int pos = posx + posy;

                        final float s00 = srcData[pos];
                        final float s01 = srcData[pos + srcPixelStride];
                        final float s10 = srcData[pos + srcScanlineStride];
                        final float s11 = srcData[pos + srcPixelStride + srcScanlineStride];

                        // Perform the bilinear interpolation
                        final float s0 = (s01 - s00) * xfrac[i] + s00;
                        final float s1 = (s11 - s10) * xfrac[i] + s10;
                        final float s = (s1 - s0) * yfrac[j] + s0;

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
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                int pos = posx + posy;

                                final float s00 = srcData[pos];
                                final float s01 = srcData[pos + srcPixelStride];
                                final float s10 = srcData[pos + srcScanlineStride];
                                final float s11 = srcData[pos + srcPixelStride + srcScanlineStride];

                                final int baseIndex = (posx / dnumBands) + (yposRoi[j]);

                                final int w00index = baseIndex;
                                final int w01index = baseIndex + 1;
                                final int w10index = baseIndex + roiScanlineStride;
                                final int w11index = baseIndex + 1 + roiScanlineStride;

                                final int w00 = w00index < roiDataLength ? roiDataArray[w00index]
                                        : 0;
                                final int w01 = w01index < roiDataLength ? roiDataArray[w01index]
                                        : 0;
                                final int w10 = w10index < roiDataLength ? roiDataArray[w10index]
                                        : 0;
                                final int w11 = w11index < roiDataLength ? roiDataArray[w11index]
                                        : 0;

                                if (baseIndex > roiDataLength || w00 == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataFloat;
                                } else if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataFloat;
                                } else {
                                    // Perform the bilinear interpolation
                                    final float s0 = (s01 - s00) * xfrac[i] + s00;
                                    final float s1 = (s11 - s10) * xfrac[i] + s10;
                                    final float s = (s1 - s0) * yfrac[j] + s0;

                                    // The interpolated value is saved in the destination array
                                    dstData[dstPixelOffset] = s;
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
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];
                                // PixelPositions
                                int x0 = src.getX() + posx / srcPixelStride;
                                int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                if (roiBounds.contains(x0, y0)) {

                                    final int w00 = roiIter.getSample(x0, y0, 0);
                                    final int w01 = roiIter.getSample(x0 + 1, y0, 0);
                                    final int w10 = roiIter.getSample(x0, y0 + 1, 0);
                                    final int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0);

                                    if (!(w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0)) {

                                        // Get the four surrounding pixel values
                                        final float s00 = srcData[posx + posy];
                                        final float s01 = srcData[posx + srcPixelStride + posy];
                                        final float s10 = srcData[posx + posy + srcScanlineStride];
                                        final float s11 = srcData[posx + srcPixelStride + posy
                                                + srcScanlineStride];
                                        // Perform the bilinear interpolation
                                        final float s0 = (s01 - s00) * xfrac[i] + s00;
                                        final float s1 = (s11 - s10) * xfrac[i] + s10;
                                        final float s = (s1 - s0) * yfrac[j] + s0;

                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = s;

                                    } else {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataFloat;
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
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                // Get the four surrounding pixel values
                                final float s00 = srcData[posx + posy];
                                final float s01 = srcData[posx + srcPixelStride + posy];
                                final float s10 = srcData[posx + posy + srcScanlineStride];
                                final float s11 = srcData[posx + srcPixelStride + posy
                                        + srcScanlineStride];

                                int w00 = 1;
                                int w01 = 1;
                                int w10 = 1;
                                int w11 = 1;

                                if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                    if(s00 == Float.NEGATIVE_INFINITY || s00 == Float.NEGATIVE_INFINITY ||Float.isNaN(s00)){
                                     // The destination no data value is saved in the destination array
                                        w00 = 0;
                                    }                                
                                }else if (rangeND.contains(s00)) {
                                    w00 = 0;
                                }

                                if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                    if(s01 == Float.NEGATIVE_INFINITY || s01 == Float.NEGATIVE_INFINITY ||Float.isNaN(s01)){
                                     // The destination no data value is saved in the destination array
                                        w01 = 0;
                                    }                                
                                }else if (rangeND.contains(s01)) {
                                    w01 = 0;
                                }
                                                                
                                if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                    if(s10 == Float.NEGATIVE_INFINITY || s10 == Float.NEGATIVE_INFINITY ||Float.isNaN(s10)){
                                     // The destination no data value is saved in the destination array
                                        w10 = 0;
                                    }                                
                                }else if (rangeND.contains(s10)) {
                                    w10 = 0;
                                }
                                
                                if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                    if(s11 == Float.NEGATIVE_INFINITY || s11 == Float.NEGATIVE_INFINITY ||Float.isNaN(s11)){
                                     // The destination no data value is saved in the destination array
                                        w11 = 0;
                                    }                                
                                }else if (rangeND.contains(s11)) {
                                    w11 = 0;
                                }
                                
                                if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataFloat;
                                } else {
                                    // compute value
                                    dstData[dstPixelOffset] = (float) computeValueDouble(s00, s01,
                                            s10, s11, w00, w01, w10, w11, xfrac[i], yfrac[j],
                                            dataType);
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
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];

                                    // Get the four surrounding pixel values
                                    final float s00 = srcData[posx + posy];
                                    final float s01 = srcData[posx + srcPixelStride + posy];
                                    final float s10 = srcData[posx + posy + srcScanlineStride];
                                    final float s11 = srcData[posx + srcPixelStride + posy
                                            + srcScanlineStride];

                                    final int baseIndex = (posx / dnumBands) + (yposRoi[j]);

                                    final int w00index = baseIndex;
                                    final int w01index = baseIndex + 1;
                                    final int w10index = baseIndex + roiScanlineStride;
                                    final int w11index = baseIndex + 1 + roiScanlineStride;

                                    int w00 = w00index < roiDataLength ? roiDataArray[w00index] : 0;
                                    int w01 = w01index < roiDataLength ? roiDataArray[w01index] : 0;
                                    int w10 = w10index < roiDataLength ? roiDataArray[w10index] : 0;
                                    int w11 = w11index < roiDataLength ? roiDataArray[w11index] : 0;

                                    if (baseIndex > roiDataLength || w00 == 0) {
                                        dstData[dstPixelOffset] = destinationNoDataFloat;
                                    } else if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataFloat;
                                    } else {
                                        
                                        if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                            if(s00 == Float.NEGATIVE_INFINITY || s00 == Float.NEGATIVE_INFINITY ||Float.isNaN(s00)){
                                             // The destination no data value is saved in the destination array
                                                w00 = 0;
                                            }else {
                                                w00 = 1;
                                            }                                
                                        }else if (rangeND.contains(s00)) {
                                            w00 = 0;
                                        }else {
                                            w00 = 1;
                                        }

                                        if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                            if(s01 == Float.NEGATIVE_INFINITY || s01 == Float.NEGATIVE_INFINITY ||Float.isNaN(s01)){
                                             // The destination no data value is saved in the destination array
                                                w01 = 0;
                                            }else {
                                                w01 = 1;
                                            }                                
                                        }if (rangeND.contains(s01)) {
                                            w01 = 0;
                                        }else {
                                            w01 = 1;
                                        }
                                                                        
                                        if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                            if(s10 == Float.NEGATIVE_INFINITY || s10 == Float.NEGATIVE_INFINITY ||Float.isNaN(s10)){
                                             // The destination no data value is saved in the destination array
                                                w10 = 0;
                                            }else {
                                                w10 = 1;
                                            }                                
                                        }else if (rangeND.contains(s10)) {
                                            w10 = 0;
                                        }else {
                                            w10 = 1;
                                        }
                                        
                                        if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                            if(s11 == Float.NEGATIVE_INFINITY || s11 == Float.NEGATIVE_INFINITY ||Float.isNaN(s11)){
                                             // The destination no data value is saved in the destination array
                                                w11 = 0;
                                            }else {
                                                w11 = 1;
                                            }                                
                                        }else if (rangeND.contains(s11)) {
                                            w11 = 0;
                                        }else {
                                            w11 = 1;
                                        }
                                        
                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = (float) computeValueDouble(s00,
                                                s01, s10, s11, w00, w01, w10, w11, xfrac[i],
                                                yfrac[j], dataType);
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
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];

                                    // PixelPositions
                                    int x0 = src.getX() + posx / srcPixelStride;
                                    int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {

                                        int w00 = roiIter.getSample(x0, y0, 0);
                                        int w01 = roiIter.getSample(x0 + 1, y0, 0);
                                        int w10 = roiIter.getSample(x0, y0 + 1, 0);
                                        int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0);

                                        if (!(w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0)) {

                                            // Get the four surrounding pixel values
                                            final float s00 = srcData[posx + posy];
                                            final float s01 = srcData[posx + srcPixelStride + posy];
                                            final float s10 = srcData[posx + posy
                                                    + srcScanlineStride];
                                            final float s11 = srcData[posx + srcPixelStride + posy
                                                    + srcScanlineStride];

                                            if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                                if(s00 == Float.NEGATIVE_INFINITY || s00 == Float.NEGATIVE_INFINITY ||Float.isNaN(s00)){
                                                 // The destination no data value is saved in the destination array
                                                    w00 = 0;
                                                }else {
                                                    w00 = 1;
                                                }                                
                                            }else if (rangeND.contains(s00)) {
                                                w00 = 0;
                                            }else {
                                                w00 = 1;
                                            }

                                            if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                                if(s01 == Float.NEGATIVE_INFINITY || s01 == Float.NEGATIVE_INFINITY ||Float.isNaN(s01)){
                                                 // The destination no data value is saved in the destination array
                                                    w01 = 0;
                                                }else {
                                                    w01 = 1;
                                                }                                
                                            }if (rangeND.contains(s01)) {
                                                w01 = 0;
                                            }else {
                                                w01 = 1;
                                            }
                                                                            
                                            if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                                if(s10 == Float.NEGATIVE_INFINITY || s10 == Float.NEGATIVE_INFINITY ||Float.isNaN(s10)){
                                                 // The destination no data value is saved in the destination array
                                                    w10 = 0;
                                                }else {
                                                    w10 = 1;
                                                }                                
                                            }else if (rangeND.contains(s10)) {
                                                w10 = 0;
                                            }else {
                                                w10 = 1;
                                            }
                                            
                                            if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                                if(s11 == Float.NEGATIVE_INFINITY || s11 == Float.NEGATIVE_INFINITY ||Float.isNaN(s11)){
                                                 // The destination no data value is saved in the destination array
                                                    w11 = 0;
                                                }else {
                                                    w11 = 1;
                                                }                                
                                            }else if (rangeND.contains(s11)) {
                                                w11 = 0;
                                            }else {
                                                w11 = 1;
                                            }


                                            // compute value
                                            dstData[dstPixelOffset] = (float) computeValueDouble(
                                                    s00, s01, s10, s11, w00, w01, w10, w11,
                                                    xfrac[i], yfrac[j], dataType);

                                        } else {
                                            // The destination no data value is saved in the destination array
                                            dstData[dstPixelOffset] = destinationNoDataFloat;
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
                }
            }
        }
    }

    private void doubleLoop(RasterAccessor src, Rectangle dstRect, RasterAccessor dst, int[] xpos,
            int[] ypos, float[] xfrac, float[] yfrac, RasterAccessor roi, int[] yposRoi,
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

        // No Data Range
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
                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        int posx = xpos[i];
                        int pos = posx + posy;

                        final double s00 = srcData[pos];
                        final double s01 = srcData[pos + srcPixelStride];
                        final double s10 = srcData[pos + srcScanlineStride];
                        final double s11 = srcData[pos + srcPixelStride + srcScanlineStride];

                        // Perform the bilinear interpolation
                        final double s0 = (s01 - s00) * xfrac[i] + s00;
                        final double s1 = (s11 - s10) * xfrac[i] + s10;
                        final double s = (s1 - s0) * yfrac[j] + s0;

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
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                int pos = posx + posy;

                                final double s00 = srcData[pos];
                                final double s01 = srcData[pos + srcPixelStride];
                                final double s10 = srcData[pos + srcScanlineStride];
                                final double s11 = srcData[pos + srcPixelStride + srcScanlineStride];

                                final int baseIndex = (posx / dnumBands) + (yposRoi[j]);

                                final int w00index = baseIndex;
                                final int w01index = baseIndex + 1;
                                final int w10index = baseIndex + roiScanlineStride;
                                final int w11index = baseIndex + 1 + roiScanlineStride;

                                final int w00 = w00index < roiDataLength ? roiDataArray[w00index]
                                        : 0;
                                final int w01 = w01index < roiDataLength ? roiDataArray[w01index]
                                        : 0;
                                final int w10 = w10index < roiDataLength ? roiDataArray[w10index]
                                        : 0;
                                final int w11 = w11index < roiDataLength ? roiDataArray[w11index]
                                        : 0;

                                if (baseIndex > roiDataLength || w00 == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataDouble;
                                } else if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataDouble;
                                } else {
                                    // Perform the bilinear interpolation
                                    final double s0 = (s01 - s00) * xfrac[i] + s00;
                                    final double s1 = (s11 - s10) * xfrac[i] + s10;
                                    final double s = (s1 - s0) * yfrac[j] + s0;

                                    // The interpolated value is saved in the destination array
                                    dstData[dstPixelOffset] = s;
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
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];
                                // PixelPositions
                                int x0 = src.getX() + posx / srcPixelStride;
                                int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                if (roiBounds.contains(x0, y0)) {

                                    final int w00 = roiIter.getSample(x0, y0, 0);
                                    final int w01 = roiIter.getSample(x0 + 1, y0, 0);
                                    final int w10 = roiIter.getSample(x0, y0 + 1, 0);
                                    final int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0);

                                    if (!(w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0)) {

                                        // Get the four surrounding pixel values
                                        final double s00 = srcData[posx + posy];
                                        final double s01 = srcData[posx + srcPixelStride + posy];
                                        final double s10 = srcData[posx + posy + srcScanlineStride];
                                        final double s11 = srcData[posx + srcPixelStride + posy
                                                + srcScanlineStride];
                                        // Perform the bilinear interpolation
                                        final double s0 = (s01 - s00) * xfrac[i];
                                        final double s1 = (s11 - s10) * xfrac[i];
                                        final double s = (s1 - s0) * yfrac[j] + s0;

                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = s;

                                    } else {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataDouble;
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
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                // Get the four surrounding pixel values
                                final double s00 = srcData[posx + posy];
                                final double s01 = srcData[posx + srcPixelStride + posy];
                                final double s10 = srcData[posx + posy + srcScanlineStride];
                                final double s11 = srcData[posx + srcPixelStride + posy
                                        + srcScanlineStride];

                                int w00 = 1;
                                int w01 = 1;
                                int w10 = 1;
                                int w11 = 1;

                                if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                    if(s00 == Double.NEGATIVE_INFINITY || s00 == Double.NEGATIVE_INFINITY ||Double.isNaN(s00)){
                                     // The destination no data value is saved in the destination array
                                        w00 = 0;
                                    }                                
                                }else if (rangeND.contains(s00)) {
                                    w00 = 0;
                                }

                                if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                    if(s01 == Double.NEGATIVE_INFINITY || s01 == Double.NEGATIVE_INFINITY ||Double.isNaN(s01)){
                                     // The destination no data value is saved in the destination array
                                        w01 = 0;
                                    }                                
                                }else if (rangeND.contains(s01)) {
                                    w01 = 0;
                                }
                                                                
                                if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                    if(s10 == Double.NEGATIVE_INFINITY || s10 == Double.NEGATIVE_INFINITY ||Double.isNaN(s10)){
                                     // The destination no data value is saved in the destination array
                                        w10 = 0;
                                    }                                
                                }else if (rangeND.contains(s10)) {
                                    w10 = 0;
                                }
                                
                                if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                    if(s11 == Double.NEGATIVE_INFINITY || s11 == Double.NEGATIVE_INFINITY ||Double.isNaN(s11)){
                                     // The destination no data value is saved in the destination array
                                        w11 = 0;
                                    }                                
                                }else if (rangeND.contains(s11)) {
                                    w11 = 0;
                                }


                                if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataDouble;
                                } else {
                                    // compute value
                                    dstData[dstPixelOffset] = computeValueDouble(s00, s01, s10,
                                            s11, w00, w01, w10, w11, xfrac[i], yfrac[j], dataType);
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
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];

                                    // Get the four surrounding pixel values
                                    final double s00 = srcData[posx + posy];
                                    final double s01 = srcData[posx + srcPixelStride + posy];
                                    final double s10 = srcData[posx + posy + srcScanlineStride];
                                    final double s11 = srcData[posx + srcPixelStride + posy
                                            + srcScanlineStride];

                                    final int baseIndex = (posx / dnumBands) + (yposRoi[j]);

                                    final int w00index = baseIndex;
                                    final int w01index = baseIndex + 1;
                                    final int w10index = baseIndex + roiScanlineStride;
                                    final int w11index = baseIndex + 1 + roiScanlineStride;

                                    int w00 = w00index < roiDataLength ? roiDataArray[w00index] : 0;
                                    int w01 = w01index < roiDataLength ? roiDataArray[w01index] : 0;
                                    int w10 = w10index < roiDataLength ? roiDataArray[w10index] : 0;
                                    int w11 = w11index < roiDataLength ? roiDataArray[w11index] : 0;

                                    if (baseIndex > roiDataLength || w00 == 0) {
                                        dstData[dstPixelOffset] = destinationNoDataDouble;
                                    } else if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataDouble;
                                    } else {
                                        if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                            if(s00 == Double.NEGATIVE_INFINITY || s00 == Double.NEGATIVE_INFINITY ||Double.isNaN(s00)){
                                             // The destination no data value is saved in the destination array
                                                w00 = 0;
                                            }else {
                                                w00 = 1;
                                            }                                
                                        }else if (rangeND.contains(s00)) {
                                            w00 = 0;
                                        }else {
                                            w00 = 1;
                                        }

                                        if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                            if(s01 == Double.NEGATIVE_INFINITY || s01 == Double.NEGATIVE_INFINITY ||Double.isNaN(s01)){
                                             // The destination no data value is saved in the destination array
                                                w01 = 0;
                                            }else {
                                                w01 = 1;
                                            }                                
                                        }if (rangeND.contains(s01)) {
                                            w01 = 0;
                                        }else {
                                            w01 = 1;
                                        }
                                                                        
                                        if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                            if(s10 == Double.NEGATIVE_INFINITY || s10 == Double.NEGATIVE_INFINITY ||Double.isNaN(s10)){
                                             // The destination no data value is saved in the destination array
                                                w10 = 0;
                                            }else {
                                                w10 = 1;
                                            }                                
                                        }else if (rangeND.contains(s10)) {
                                            w10 = 0;
                                        }else {
                                            w10 = 1;
                                        }
                                        
                                        if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                            if(s11 == Double.NEGATIVE_INFINITY || s11 == Double.NEGATIVE_INFINITY ||Double.isNaN(s11)){
                                             // The destination no data value is saved in the destination array
                                                w11 = 0;
                                            }else {
                                                w11 = 1;
                                            }                                
                                        }else if (rangeND.contains(s11)) {
                                            w11 = 0;
                                        }else {
                                            w11 = 1;
                                        }


                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = computeValueDouble(s00, s01, s10,
                                                s11, w00, w01, w10, w11, xfrac[i], yfrac[j],
                                                dataType);
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
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];

                                    // PixelPositions
                                    int x0 = src.getX() + posx / srcPixelStride;
                                    int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {

                                        int w00 = roiIter.getSample(x0, y0, 0);
                                        int w01 = roiIter.getSample(x0 + 1, y0, 0);
                                        int w10 = roiIter.getSample(x0, y0 + 1, 0);
                                        int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0);

                                        if (!(w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0)) {

                                            // Get the four surrounding pixel values
                                            final double s00 = srcData[posx + posy];
                                            final double s01 = srcData[posx + srcPixelStride + posy];
                                            final double s10 = srcData[posx + posy
                                                    + srcScanlineStride];
                                            final double s11 = srcData[posx + srcPixelStride + posy
                                                    + srcScanlineStride];

                                            if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                                if(s00 == Double.NEGATIVE_INFINITY || s00 == Double.NEGATIVE_INFINITY ||Double.isNaN(s00)){
                                                 // The destination no data value is saved in the destination array
                                                    w00 = 0;
                                                }else {
                                                    w00 = 1;
                                                }                                
                                            }else if (rangeND.contains(s00)) {
                                                w00 = 0;
                                            }else {
                                                w00 = 1;
                                            }

                                            if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                                if(s01 == Double.NEGATIVE_INFINITY || s01 == Double.NEGATIVE_INFINITY ||Double.isNaN(s01)){
                                                 // The destination no data value is saved in the destination array
                                                    w01 = 0;
                                                }else {
                                                    w01 = 1;
                                                }                                
                                            }if (rangeND.contains(s01)) {
                                                w01 = 0;
                                            }else {
                                                w01 = 1;
                                            }
                                                                            
                                            if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                                if(s10 == Double.NEGATIVE_INFINITY || s10 == Double.NEGATIVE_INFINITY ||Double.isNaN(s10)){
                                                 // The destination no data value is saved in the destination array
                                                    w10 = 0;
                                                }else {
                                                    w10 = 1;
                                                }                                
                                            }else if (rangeND.contains(s10)) {
                                                w10 = 0;
                                            }else {
                                                w10 = 1;
                                            }
                                            
                                            if (isNegativeInf|| isPositiveInf|| isRangeNaN) {                                
                                                if(s11 == Double.NEGATIVE_INFINITY || s11 == Double.NEGATIVE_INFINITY ||Double.isNaN(s11)){
                                                 // The destination no data value is saved in the destination array
                                                    w11 = 0;
                                                }else {
                                                    w11 = 1;
                                                }                                
                                            }else if (rangeND.contains(s11)) {
                                                w11 = 0;
                                            }else {
                                                w11 = 1;
                                            }


                                            // compute value
                                            dstData[dstPixelOffset] = computeValueDouble(s00, s01,
                                                    s10, s11, w00, w01, w10, w11, xfrac[i],
                                                    yfrac[j], dataType);

                                        } else {
                                            // The destination no data value is saved in the destination array
                                            dstData[dstPixelOffset] = destinationNoDataDouble;
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
                }
            }
        }
    }

    /* Private method for calculate bilinear interpolation for byte, short/ushort, integer dataType */
    private int computeValue(int s00, int s01, int s10, int s11, int w00, int w01, int w10,
            int w11, int xfrac, int yfrac) {
        int s0 = 0;
        int s1 = 0;
        int s = 0;

        long s0L = 0;
        long s1L = 0;

        // Complementary values of the fractional part
        int xfracCompl = (int) Math.pow(2, subsampleBits) - xfrac;
        int yfracCompl = (int) Math.pow(2, subsampleBits) - yfrac;
        
        if(w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0){
            switch(dataType){
                case DataBuffer.TYPE_BYTE:
                    return destinationNoDataByte;            
                case DataBuffer.TYPE_USHORT:
                    return destinationNoDataUShort;     
                case DataBuffer.TYPE_SHORT:
                    return destinationNoDataShort;        
                case DataBuffer.TYPE_INT:
                    return destinationNoDataInt;
            }
        }

        int shift = 29 - subsampleBits;
        // For Integer value is possible that a bitshift of "subsampleBits" could shift over the integer bit number
        // so the samples, in this case, are expanded to Long.
        boolean s0Long = ((s00 | s10) >>> shift == 0);
        boolean s1Long = ((s01 | s11) >>> shift == 0);
        // Otherwise all the possible weight combination are checked
        if (w00 == 0 || w01 == 0 || w10 == 0 || w11 == 0) {
            // For integers is even considered the case when the integers are expanded to longs
            if (dataType == DataBuffer.TYPE_INT) {

                if (w00 == 0 && w01 == 0) {

                    s0L = 0;
                } else if (w00 == 0) { // w01 = 1
                    if (s1Long) {
                        s0L = -s01 * xfracCompl + (s01 << subsampleBits);
                    } else {
                        s0L = -s01 * xfracCompl + ((long) s01 << subsampleBits);
                    }
                } else if (w01 == 0) {// w00 = 1
                    if (s0Long) {
                        s0L = -s00 * xfrac + (s00 << subsampleBits);
                    } else {
                        s0L = -s00 * xfrac + ((long) s00 << subsampleBits);
                    }
                } else {// w00 = 1 & W01 = 1
                    if (s0Long) {
                        if (s1Long) {
                            s0L = (s01 - s00) * xfrac + (s00 << subsampleBits);
                        } else {
                            s0L = ((long) s01 - s00) * xfrac + (s00 << subsampleBits);
                        }
                    } else {
                        s0L = ((long) s01 - s00) * xfrac + ((long) s00 << subsampleBits);
                    }
                }

                // lower value

                if (w10 == 0 && w11 == 0) {
                    s1L = 0;
                } else if (w10 == 0) { // w11 = 1
                    if (s1Long) {
                        s1L = -s11 * xfracCompl + (s11 << subsampleBits);
                    } else {
                        s1L = -s11 * xfracCompl + ((long) s11 << subsampleBits);
                    }
                } else if (w11 == 0) { // w10 = 1
                    if (s0Long) {// - (s10 * xfrac); //s10;
                        s1L = -s10 * xfrac + (s10 << subsampleBits);
                    } else {
                        s1L = -s10 * xfrac + ((long) s10 << subsampleBits);
                    }
                } else {
                    if (s0Long) {
                        if (s1Long) {
                            s1L = (s11 - s10) * xfrac + (s10 << subsampleBits);
                        } else {
                            s1L = ((long) s11 - s10) * xfrac + (s10 << subsampleBits);
                        }
                    } else {
                        s1L = ((long) s11 - s10) * xfrac + ((long) s10 << subsampleBits);
                    }
                }
                if (w00 == 0 && w01 == 0) {
                    s = (int) (-s1L * yfracCompl + ((s1L << subsampleBits) + round2) >> shift2);
                } else {
                    if (w10 == 0 && w11 == 0) {
                        s = (int) (-s0L * yfrac + ((s0L << subsampleBits) + round2) >> shift2);
                    } else {
                        s = (int) (((s1L - s0L) * yfrac + (s0L << subsampleBits) + round2) >> shift2);
                    }
                }

            } else {
                // Interpolation for type byte, ushort, short
                if (w00 == 0 && w01 == 0) {
                    s0 = 0;
                } else if (w00 == 0) { // w01 = 1
                    s0 = -s01 * xfracCompl + (s01 << subsampleBits);
                } else if (w01 == 0) {// w00 = 1
                    s0 = -s00 * xfrac + (s00 << subsampleBits);// s00;
                } else {// w00 = 1 & W01 = 1
                    s0 = (s01 - s00) * xfrac + (s00 << subsampleBits);
                }

                // lower value

                if (w10 == 0 && w11 == 0) {
                    s1 = 0;
                } else if (w10 == 0) { // w11 = 1
                    s1 = -s11 * xfracCompl + (s11 << subsampleBits);
                } else if (w11 == 0) { // w10 = 1
                    s1 = -s10 * xfrac + (s10 << subsampleBits);// - (s10 * xfrac); //s10;
                } else {
                    s1 = (s11 - s10) * xfrac + (s10 << subsampleBits);
                }

                if (w00 == 0 && w01 == 0) {
                    s = (-s1 * yfracCompl + (s1 << subsampleBits) + round2) >> shift2;
                } else {
                    if (w10 == 0 && w11 == 0) {
                        s = (-s0 * yfrac + (s0 << subsampleBits) + round2) >> shift2;
                    } else {
                        s = ((s1 - s0) * yfrac + (s0 << subsampleBits) + round2) >> shift2;
                    }
                }

            }
        } else {
            // Perform the bilinear interpolation
            if (dataType == DataBuffer.TYPE_INT) {
                if (s0Long) {
                    if (s1Long) {
                        s0 = (s01 - s00) * xfrac + (s00 << subsampleBits);
                        s1 = (s11 - s10) * xfrac + (s10 << subsampleBits);
                        s = ((s1 - s0) * yfrac + (s0 << subsampleBits) + round2) >> shift2;
                    } else {
                        s0L = ((long) s01 - s00) * xfrac + (s00 << subsampleBits);
                        s1L = ((long) s11 - s10) * xfrac + (s10 << subsampleBits);
                        s = (int) (((s1L - s0L) * yfrac + (s0L << subsampleBits) + round2) >> shift2);
                    }
                } else {
                    s0L = ((long) s01 - s00) * xfrac + ((long) s00 << subsampleBits);
                    s1L = ((long) s11 - s10) * xfrac + ((long) s10 << subsampleBits);
                    s = (int) (((s1L - s0L) * yfrac + (s0L << subsampleBits) + round2) >> shift2);
                }
            } else {
                s0 = (s01 - s00) * xfrac + (s00 << subsampleBits);
                s1 = (s11 - s10) * xfrac + (s10 << subsampleBits);
                s = ((s1 - s0) * yfrac + (s0 << subsampleBits) + round2) >> shift2;
            }
        }

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            s = (byte) s & 0xff;
            break;
        case DataBuffer.TYPE_USHORT:
            s = (short) s & 0xffff;
            break;
        case DataBuffer.TYPE_SHORT:
            s = (short) s;
            break;
        default:
            break;
        }
        return s;
    }

    /* Private method for calculate bilinear interpolation for float/double dataType */
    private double computeValueDouble(double s00, double s01, double s10, double s11, double w00,
            double w01, double w10, double w11, double xfrac, double yfrac, int dataType) {

        double s0 = 0;
        double s1 = 0;
        double s = 0;

        // Complementary values of the fractional part
        double xfracCompl = 1 - xfrac;
        double yfracCompl = 1 - yfrac;

        if(w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0){
            switch(dataType){
                case DataBuffer.TYPE_FLOAT:
                    return destinationNoDataFloat;            
                case DataBuffer.TYPE_DOUBLE:
                    return destinationNoDataDouble;     
            }
        }
        
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

        // Simple conversion for float dataType.
        if (dataType == DataBuffer.TYPE_FLOAT) {
            return (float) s;
        } else {
            return s;
        }

    }

}
