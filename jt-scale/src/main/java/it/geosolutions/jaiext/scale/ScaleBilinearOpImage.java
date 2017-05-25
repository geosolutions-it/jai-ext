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
package it.geosolutions.jaiext.scale;

import java.awt.Rectangle;
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

import it.geosolutions.jaiext.interpolators.InterpolationBilinear;
import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.NoDataContainer;
import it.geosolutions.jaiext.range.Range;

public class ScaleBilinearOpImage extends ScaleOpImage {
    
    /** boolean indicating if the data type is DataBuffer.TYPE_INT*/
    protected boolean dataINT = false;

    /** Byte lookuptable used if no data are present */
    protected byte[][] byteLookupTable;

    /** Bilinear interpolator */
    protected InterpolationBilinear interpB = null;

    // Use weighted contribute only if the fraction is greater than the threshold.
    private final static int FRACTION_THRESHOLD_I = 128;
    private final static int FULL_WEIGHT_SHIFT = 8; // a*256 = a<<8

    private final static double FRACTION_THRESHOLD_D = 0.5d;

    public ScaleBilinearOpImage(RenderedImage source, ImageLayout layout, Map configuration,
            BorderExtender extender, Interpolation interp, float scaleX, float scaleY,
            float transX, float transY, boolean useRoiAccessor, Range nodata, double[] backgroundValues) {
        super(source, layout, configuration, true, extender, interp, scaleX, scaleY, transX,
                transY, useRoiAccessor, backgroundValues);
        scaleOpInitialization(source, interp, nodata, backgroundValues, useRoiAccessor);
    }

    private void scaleOpInitialization(RenderedImage source, Interpolation interp, Range nodata, double[] backgroundValues, boolean useRoiAccessor) {
        // If the source has an IndexColorModel, override the default setting
        // in OpImage. The dest shall have exactly the same SampleModel and
        // ColorModel as the source.
        // Note, in this case, the source should have an integral data type.
        ColorModel srcColorModel = source.getColorModel();
        if (srcColorModel instanceof IndexColorModel && ImageUtil.isBinary(source.getSampleModel())) {
            sampleModel = source.getSampleModel()
                    .createCompatibleSampleModel(tileWidth, tileHeight);
            colorModel = srcColorModel;
        }

        SampleModel sm = source.getSampleModel();
        // Source image data Type
        int srcDataType = sm.getDataType();
        // NumBands
        int numBands = getSampleModel().getNumBands();

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

        // If both roiBounds and roiIter are not null, they are used in calculation
        Range nod = nodata;
        double[] destNod = null;
        double destinationNoData = (backgroundValues != null && backgroundValues.length > 0)?
                backgroundValues[0] : (nod != null? nod.getMin().doubleValue() : Double.NaN);
        if (!Double.isNaN(destinationNoData)) {
            destNod = new double[]{destinationNoData};
        }
        if (interp instanceof InterpolationBilinear) {
            isBilinearNew = true;
            interpB = (InterpolationBilinear) interp;
            this.interp = interpB;
            interpB.setROIBounds(roiBounds);
            if(nod == null){
            	nod = interpB.getNoDataRange();
            }
            if(destNod == null){
            	destNod = new double[]{interpB.getDestinationNoData()};
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
            this.useRoiAccessor = useRoiAccessor;
        }
        // subsample bits used for the bilinear and bicubic interpolation
        subsampleBits = interp.getSubsampleBitsH();

        // Internal precision required for position calculations
        one = 1 << subsampleBits;

        // Subsampling related variables
        shift = 29 - subsampleBits;
        shift2 = 2 * subsampleBits;
        round2 = 1 << (shift2 - 1);

        // Number of subsample positions
        one = 1 << subsampleBits;

        // Get the width and height and padding of the Interpolation kernel.
        interp_width = interp.getWidth();
        interp_height = interp.getHeight();
        interp_left = interp.getLeftPadding();
        interp_top = interp.getTopPadding();

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
        // Creation of a lookuptable containing the values to use for no data
        if (hasNoData) {
            // Creation of a lookuptable containing the values to use for no data
            byteLookupTable = new byte[numBands][256];
            for (int i = 0; i < byteLookupTable[0].length; i++) {
                for (int b = 0; b < numBands; b++) {
                    if (noData.contains(i)) {
                        byteLookupTable[b][i] = destinationNoDataByte[b];
                    } else {
                        byteLookupTable[b][i] = (byte) i;
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

    @Override
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
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
        RandomIter roiIter = null;

        // ROI calculation only if the roi raster is present
        if (hasROI) {
            if (useRoiAccessor) {
                if(srcROIImage.getBounds().contains(srcRect)){
                    roi = srcROIImage.getData(srcRect);
                } else {
                    roi = srcROIImgExt.getData(srcRect);
                }
                // creation of the rasterAccessor
                roiAccessor = new RasterAccessor(roi, srcRect, RasterAccessor.findCompatibleTags(
                        new RenderedImage[] { srcROIImage }, srcROIImage)[0],
                        srcROIImage.getColorModel());
                // ROI scanlinestride
                roiScanlineStride = roiAccessor.getScanlineStride();
                // Initialization of the roi y position array
                yposRoi = new int[dheight];
            } else {
                roiIter = RandomIterFactory.create(srcROIImgExt, roiRect, true, true);
            }
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

        switch (dstAccessor.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(srcAccessor, destRect, dstAccessor, xpos, ypos, xfracValues, yfracValues,
                    roiAccessor, yposRoi, roiScanlineStride, roiIter);
            break;
        case DataBuffer.TYPE_USHORT:
            ushortLoop(srcAccessor, destRect, dstAccessor, xpos, ypos, xfracValues, yfracValues,
                    roiAccessor, yposRoi, roiScanlineStride, roiIter);
            break;
        case DataBuffer.TYPE_SHORT:
            shortLoop(srcAccessor, destRect, dstAccessor, xpos, ypos, xfracValues, yfracValues,
                    roiAccessor, yposRoi, roiScanlineStride, roiIter);
            break;
        case DataBuffer.TYPE_INT:
            intLoop(srcAccessor, destRect, dstAccessor, xpos, ypos, xfracValues, yfracValues,
                    roiAccessor, yposRoi, roiScanlineStride, roiIter);
            break;
        case DataBuffer.TYPE_FLOAT:
            floatLoop(srcAccessor, destRect, dstAccessor, xpos, ypos, xfracValuesFloat,
                    yfracValuesFloat, roiAccessor, yposRoi, roiScanlineStride, roiIter);
            break;
        case DataBuffer.TYPE_DOUBLE:
            doubleLoop(srcAccessor, destRect, dstAccessor, xpos, ypos, xfracValuesFloat,
                    yfracValuesFloat, roiAccessor, yposRoi, roiScanlineStride, roiIter);
            break;
        }

    }

    private void byteLoop(RasterAccessor src, Rectangle dstRect, RasterAccessor dst, int[] xpos,
            int[] ypos, int[] xfrac, int[] yfrac, RasterAccessor roi, int[] yposRoi,
            int roiScanlineStride, RandomIter roiIter) {

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

        if (caseA) {
            // for all bands
            for (int k = 0; k < dnumBands; k++) {

                final byte[] srcData = srcDataArrays[k];
                final byte[] dstData = dstDataArrays[k];
                // Line and band Offset initialization
                int dstlineOffset = dstBandOffsets[k];
                final int bandOffset = bandOffsets[k];
                // cycle on the y values
                for (int j = 0; j < dheight; j++) {
                    // pixel offset initialization
                    int dstPixelOffset = dstlineOffset;
                    // y position selection
                    final int posy = ypos[j] + bandOffset;
                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        final int posx = xpos[i];
                        final int pos = posx + posy;

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
                        final int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            final int posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                final int posx = xpos[i];

                                final int pos = posx + posy;

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
                                    dstData[dstPixelOffset] = destinationNoDataByte[k];
                                } else if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataByte[k];
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
                        final int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            final int posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                final int posx = xpos[i];
                                // PixelPositions
                                final int x0 = src.getX() + posx / srcPixelStride;
                                final int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                if (roiBounds.contains(x0, y0)) {

                                    final int w00 = roiIter.getSample(x0, y0, 0);
                                    final int w01 = roiIter.getSample(x0 + 1, y0, 0);
                                    final int w10 = roiIter.getSample(x0, y0 + 1, 0);
                                    final int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0);

                                    if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataByte[k];
                                    } else {
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
                                    }
                                } else {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataByte[k];
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

                    int w00 = 0;
                    int w01 = 0;
                    int w10 = 0;
                    int w11 = 0;

                    int s00;
                    int s01;
                    int s10;
                    int s11;

                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {

                        final byte[] srcData = srcDataArrays[k];
                        final byte[] dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        final int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            final int posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                final int posx = xpos[i];

                                // Get the four surrounding pixel values
                                s00 = srcData[posx + posy] & 0xff;
                                s01 = srcData[posx + srcPixelStride + posy] & 0xff;
                                s10 = srcData[posx + posy + srcScanlineStride] & 0xff;
                                s11 = srcData[posx + srcPixelStride + posy
                                        + srcScanlineStride] & 0xff;

                                w00 = byteLookupTable[k][s00] == destinationNoDataByte[k] ? 0 : 1;
                                w01 = byteLookupTable[k][s01] == destinationNoDataByte[k] ? 0 : 1;
                                w10 = byteLookupTable[k][s10] == destinationNoDataByte[k] ? 0 : 1;
                                w11 = byteLookupTable[k][s11] == destinationNoDataByte[k] ? 0 : 1;

                                if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataByte[k];
                                } else {
                                    // compute value
                                    dstData[dstPixelOffset] = (byte) (computeValue(s00, s01, s10,
                                            s11, w00, w01, w10, w11, xfrac[i], yfrac[j], k) & 0xff);
                                }

                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else {
                    if (useRoiAccessor) {
                        // for all bands
                        for (int k = 0; k < dnumBands; k++) {

                            final byte[] srcData = srcDataArrays[k];
                            final byte[] dstData = dstDataArrays[k];
                            // Line and band Offset initialization
                            int dstlineOffset = dstBandOffsets[k];
                            final int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                final int posy = ypos[j] + bandOffset;
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    final int posx = xpos[i];

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
                                        dstData[dstPixelOffset] = destinationNoDataByte[k];
                                    } else if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataByte[k];
                                    } else {

                                        w00 = byteLookupTable[k][s00] == destinationNoDataByte[k] ? 0 : 1;
                                        w01 = byteLookupTable[k][s01] == destinationNoDataByte[k] ? 0 : 1;
                                        w10 = byteLookupTable[k][s10] == destinationNoDataByte[k] ? 0 : 1;
                                        w11 = byteLookupTable[k][s11] == destinationNoDataByte[k] ? 0 : 1;

                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = (byte) (computeValue(s00, s01,
                                                s10, s11, w00, w01, w10, w11, xfrac[i], yfrac[j], k) & 0xff);
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
                            final int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                final int posy = ypos[j] + bandOffset;
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    final int posx = xpos[i];

                                    // PixelPositions
                                    final int x0 = src.getX() + posx / srcPixelStride;
                                    final int y0 = src.getY() + (posy - bandOffset)
                                            / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {

                                        int w00 = roiIter.getSample(x0, y0, 0);
                                        int w01 = roiIter.getSample(x0 + 1, y0, 0);
                                        int w10 = roiIter.getSample(x0, y0 + 1, 0);
                                        int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0);

                                        if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                            // The destination no data value is saved in the destination array
                                            dstData[dstPixelOffset] = destinationNoDataByte[k];
                                        } else {

                                            // Get the four surrounding pixel values
                                            final int s00 = srcData[posx + posy] & 0xff;
                                            final int s01 = srcData[posx + srcPixelStride + posy] & 0xff;
                                            final int s10 = srcData[posx + posy + srcScanlineStride] & 0xff;
                                            final int s11 = srcData[posx + srcPixelStride + posy
                                                    + srcScanlineStride] & 0xff;

                                            w00 = byteLookupTable[k][s00] == destinationNoDataByte[k] ? 0 : 1;
                                            w01 = byteLookupTable[k][s01] == destinationNoDataByte[k] ? 0 : 1;
                                            w10 = byteLookupTable[k][s10] == destinationNoDataByte[k] ? 0 : 1;
                                            w11 = byteLookupTable[k][s11] == destinationNoDataByte[k] ? 0 : 1;

                                            // compute value
                                            dstData[dstPixelOffset] = (byte) (computeValue(s00,
                                                    s01, s10, s11, w00, w01, w10, w11, xfrac[i],
                                                    yfrac[j], k) & 0xff);
                                        }
                                    } else {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataByte[k];
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
            int roiScanlineStride, RandomIter roiIter) {
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

        if (caseA) {
            // for all bands
            for (int k = 0; k < dnumBands; k++) {

                final short[] srcData = srcDataArrays[k];
                final short[] dstData = dstDataArrays[k];
                // Line and band Offset initialization
                int dstlineOffset = dstBandOffsets[k];
                final int bandOffset = bandOffsets[k];
                // cycle on the y values
                for (int j = 0; j < dheight; j++) {
                    // pixel offset initialization
                    int dstPixelOffset = dstlineOffset;
                    // y position selection
                    final int posy = ypos[j] + bandOffset;
                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        final int posx = xpos[i];
                        final int pos = posx + posy;

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
                        final int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            final int posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                final int posx = xpos[i];

                                final int pos = posx + posy;

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
                                    dstData[dstPixelOffset] = destinationNoDataUShort[k];
                                } else if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataUShort[k];
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
                        final int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            final int posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                final int posx = xpos[i];
                                // PixelPositions
                                final int x0 = src.getX() + posx / srcPixelStride;
                                final int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                if (roiBounds.contains(x0, y0)) {

                                    final int w00 = roiIter.getSample(x0, y0, 0);
                                    final int w01 = roiIter.getSample(x0 + 1, y0, 0);
                                    final int w10 = roiIter.getSample(x0, y0 + 1, 0);
                                    final int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0);

                                    if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataUShort[k];

                                    } else {
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
                                    }
                                } else {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataUShort[k];
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
                        final int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            final int posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                final int posx = xpos[i];

                                // Get the four surrounding pixel values
                                final short s00 = (short) (srcData[posx + posy] & 0xffff);
                                final short s01 = (short) (srcData[posx + srcPixelStride + posy] & 0xffff);
                                final short s10 = (short) (srcData[posx + posy + srcScanlineStride] & 0xffff);
                                final short s11 = (short) (srcData[posx + srcPixelStride + posy
                                        + srcScanlineStride] & 0xffff);

                                int w00 = noData.contains(s00) ? 0 : 1;
                                int w01 = noData.contains(s01) ? 0 : 1;
                                int w10 = noData.contains(s10) ? 0 : 1;
                                int w11 = noData.contains(s11) ? 0 : 1;

                                if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataUShort[k];
                                } else {
                                    // compute value
                                    dstData[dstPixelOffset] = (short) (computeValue(s00, s01, s10,
                                            s11, w00, w01, w10, w11, xfrac[i], yfrac[j], k) & 0xffff);
                                }

                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else {
                    if (useRoiAccessor) {
                        // for all bands
                        for (int k = 0; k < dnumBands; k++) {

                            final short[] srcData = srcDataArrays[k];
                            final short[] dstData = dstDataArrays[k];
                            // Line and band Offset initialization
                            int dstlineOffset = dstBandOffsets[k];
                            final int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                final int posy = ypos[j] + bandOffset;
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    final int posx = xpos[i];

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
                                        dstData[dstPixelOffset] = destinationNoDataUShort[k];
                                    } else if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataUShort[k];
                                    } else {
                                        w00 = noData.contains(s00) ? 0 : 1;
                                        w01 = noData.contains(s01) ? 0 : 1;
                                        w10 = noData.contains(s10) ? 0 : 1;
                                        w11 = noData.contains(s11) ? 0 : 1;

                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = (short) (computeValue(s00, s01,
                                                s10, s11, w00, w01, w10, w11, xfrac[i], yfrac[j], k) & 0xffff);
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
                            final int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                final int posy = ypos[j] + bandOffset;
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    final int posx = xpos[i];

                                    // PixelPositions
                                    final int x0 = src.getX() + posx / srcPixelStride;
                                    final int y0 = src.getY() + (posy - bandOffset)
                                            / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {

                                        int w00 = roiIter.getSample(x0, y0, 0);
                                        int w01 = roiIter.getSample(x0 + 1, y0, 0);
                                        int w10 = roiIter.getSample(x0, y0 + 1, 0);
                                        int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0);

                                        if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                            // The destination no data value is saved in the destination array
                                            dstData[dstPixelOffset] = destinationNoDataUShort[k];
                                        } else {
                                            // Get the four surrounding pixel values
                                            final short s00 = (short) (srcData[posx + posy] & 0xffff);
                                            final short s01 = (short) (srcData[posx
                                                    + srcPixelStride + posy] & 0xffff);
                                            final short s10 = (short) (srcData[posx + posy
                                                    + srcScanlineStride] & 0xffff);
                                            final short s11 = (short) (srcData[posx
                                                    + srcPixelStride + posy + srcScanlineStride] & 0xffff);

                                            w00 = noData.contains(s00) ? 0 : 1;
                                            w01 = noData.contains(s01) ? 0 : 1;
                                            w10 = noData.contains(s10) ? 0 : 1;
                                            w11 = noData.contains(s11) ? 0 : 1;

                                            // compute value
                                            dstData[dstPixelOffset] = (short) (computeValue(s00,
                                                    s01, s10, s11, w00, w01, w10, w11, xfrac[i],
                                                    yfrac[j], k) & 0xffff);
                                        }
                                    } else {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataUShort[k];
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
            int roiScanlineStride, RandomIter roiIter) {

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

        if (caseA) {
            // for all bands
            for (int k = 0; k < dnumBands; k++) {

                final short[] srcData = srcDataArrays[k];
                final short[] dstData = dstDataArrays[k];
                // Line and band Offset initialization
                int dstlineOffset = dstBandOffsets[k];
                final int bandOffset = bandOffsets[k];
                // cycle on the y values
                for (int j = 0; j < dheight; j++) {
                    // pixel offset initialization
                    int dstPixelOffset = dstlineOffset;
                    // y position selection
                    final int posy = ypos[j] + bandOffset;
                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        final int posx = xpos[i];
                        final int pos = posx + posy;

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
                        final int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            final int posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                final int posx = xpos[i];

                                final int pos = posx + posy;

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
                                    dstData[dstPixelOffset] = destinationNoDataShort[k];
                                } else if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataShort[k];
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
                        final int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            final int posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                final int posx = xpos[i];
                                // PixelPositions
                                final int x0 = src.getX() + posx / srcPixelStride;
                                final int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                if (roiBounds.contains(x0, y0)) {

                                    final int w00 = roiIter.getSample(x0, y0, 0);
                                    final int w01 = roiIter.getSample(x0 + 1, y0, 0);
                                    final int w10 = roiIter.getSample(x0, y0 + 1, 0);
                                    final int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0);

                                    if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataShort[k];
                                    } else {
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
                                    }
                                } else {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataShort[k];
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
                        final int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            final int posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                final int posx = xpos[i];

                                // Get the four surrounding pixel values
                                final short s00 = srcData[posx + posy];
                                final short s01 = srcData[posx + srcPixelStride + posy];
                                final short s10 = srcData[posx + posy + srcScanlineStride];
                                final short s11 = srcData[posx + srcPixelStride + posy
                                        + srcScanlineStride];

                                int w00 = noData.contains(s00) ? 0 : 1;
                                int w01 = noData.contains(s01) ? 0 : 1;
                                int w10 = noData.contains(s10) ? 0 : 1;
                                int w11 = noData.contains(s11) ? 0 : 1;

                                if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataShort[k];
                                } else {
                                    // compute value
                                    dstData[dstPixelOffset] = (short) (computeValue(s00, s01, s10,
                                            s11, w00, w01, w10, w11, xfrac[i], yfrac[j], k));
                                }

                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else {
                    if (useRoiAccessor) {
                        // for all bands
                        for (int k = 0; k < dnumBands; k++) {

                            final short[] srcData = srcDataArrays[k];
                            final short[] dstData = dstDataArrays[k];
                            // Line and band Offset initialization
                            int dstlineOffset = dstBandOffsets[k];
                            final int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                final int posy = ypos[j] + bandOffset;
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    final int posx = xpos[i];

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
                                        dstData[dstPixelOffset] = destinationNoDataShort[k];
                                    } else if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataShort[k];
                                    } else {

                                        w00 = noData.contains(s00) ? 0 : 1;
                                        w01 = noData.contains(s01) ? 0 : 1;
                                        w10 = noData.contains(s10) ? 0 : 1;
                                        w11 = noData.contains(s11) ? 0 : 1;

                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = (short) (computeValue(s00, s01,
                                                s10, s11, w00, w01, w10, w11, xfrac[i], yfrac[j], k));
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
                            final int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                final int posy = ypos[j] + bandOffset;
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    final int posx = xpos[i];

                                    // PixelPositions
                                    final int x0 = src.getX() + posx / srcPixelStride;
                                    final int y0 = src.getY() + (posy - bandOffset)
                                            / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {

                                        int w00 = roiIter.getSample(x0, y0, 0);
                                        int w01 = roiIter.getSample(x0 + 1, y0, 0);
                                        int w10 = roiIter.getSample(x0, y0 + 1, 0);
                                        int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0);

                                        if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                            // The destination no data value is saved in the destination array
                                            dstData[dstPixelOffset] = destinationNoDataShort[k];
                                        } else {
                                            // Get the four surrounding pixel values
                                            final short s00 = srcData[posx + posy];
                                            final short s01 = srcData[posx + srcPixelStride + posy];
                                            final short s10 = srcData[posx + posy
                                                    + srcScanlineStride];
                                            final short s11 = srcData[posx + srcPixelStride + posy
                                                    + srcScanlineStride];

                                            w00 = noData.contains(s00) ? 0 : 1;
                                            w01 = noData.contains(s01) ? 0 : 1;
                                            w10 = noData.contains(s10) ? 0 : 1;
                                            w11 = noData.contains(s11) ? 0 : 1;

                                            // compute value
                                            dstData[dstPixelOffset] = (short) (computeValue(s00,
                                                    s01, s10, s11, w00, w01, w10, w11, xfrac[i],
                                                    yfrac[j], k));
                                        }
                                    } else {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataShort[k];
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
            int roiScanlineStride, RandomIter roiIter) {

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

        if (caseA) {
            // for all bands
            for (int k = 0; k < dnumBands; k++) {

                final int[] srcData = srcDataArrays[k];
                final int[] dstData = dstDataArrays[k];
                // Line and band Offset initialization
                int dstlineOffset = dstBandOffsets[k];
                final int bandOffset = bandOffsets[k];
                // cycle on the y values
                for (int j = 0; j < dheight; j++) {
                    // pixel offset initialization
                    int dstPixelOffset = dstlineOffset;
                    // y position selection
                    final int posy = ypos[j] + bandOffset;
                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        final int posx = xpos[i];
                        final int pos = posx + posy;

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
                        final int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            final int posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                final int posx = xpos[i];

                                final int pos = posx + posy;

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
                                    dstData[dstPixelOffset] = destinationNoDataInt[k];
                                } else if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataInt[k];
                                } else {
                                    // The interpolated value is saved in the destination array
                                    dstData[dstPixelOffset] = (computeValue(s00, s01, s10, s11, 1,
                                            1, 1, 1, xfrac[i], yfrac[j], k));
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
                        final int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            final int posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                final int posx = xpos[i];
                                // PixelPositions
                                final int x0 = src.getX() + posx / srcPixelStride;
                                final int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                if (roiBounds.contains(x0, y0)) {

                                    final int w00 = roiIter.getSample(x0, y0, 0);
                                    final int w01 = roiIter.getSample(x0 + 1, y0, 0);
                                    final int w10 = roiIter.getSample(x0, y0 + 1, 0);
                                    final int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0);

                                    if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataInt[k];
                                    } else {
                                        // Get the four surrounding pixel values
                                        final int s00 = srcData[posx + posy];
                                        final int s01 = srcData[posx + srcPixelStride + posy];
                                        final int s10 = srcData[posx + posy + srcScanlineStride];
                                        final int s11 = srcData[posx + srcPixelStride + posy
                                                + srcScanlineStride];
                                        // compute value
                                        dstData[dstPixelOffset] = (computeValue(s00, s01, s10, s11,
                                                1, 1, 1, 1, xfrac[i], yfrac[j], k));
                                    }
                                } else {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataInt[k];
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
                        final int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            final int posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                final int posx = xpos[i];

                                // Get the four surrounding pixel values
                                final int s00 = srcData[posx + posy];
                                final int s01 = srcData[posx + srcPixelStride + posy];
                                final int s10 = srcData[posx + posy + srcScanlineStride];
                                final int s11 = srcData[posx + srcPixelStride + posy
                                        + srcScanlineStride];

                                int w00 = noData.contains(s00) ? 0 : 1;
                                int w01 = noData.contains(s01) ? 0 : 1;
                                int w10 = noData.contains(s10) ? 0 : 1;
                                int w11 = noData.contains(s11) ? 0 : 1;

                                if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataInt[k];
                                } else {
                                    // compute value
                                    dstData[dstPixelOffset] = (computeValue(s00, s01, s10, s11,
                                            w00, w01, w10, w11, xfrac[i], yfrac[j], k));
                                }

                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else {
                    if (useRoiAccessor) {
                        // for all bands
                        for (int k = 0; k < dnumBands; k++) {

                            final int[] srcData = srcDataArrays[k];
                            final int[] dstData = dstDataArrays[k];
                            // Line and band Offset initialization
                            int dstlineOffset = dstBandOffsets[k];
                            final int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                final int posy = ypos[j] + bandOffset;
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    final int posx = xpos[i];

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
                                        dstData[dstPixelOffset] = destinationNoDataInt[k];
                                    } else if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataInt[k];
                                    } else {
                                        w00 = noData.contains(s00) ? 0 : 1;
                                        w01 = noData.contains(s01) ? 0 : 1;
                                        w10 = noData.contains(s10) ? 0 : 1;
                                        w11 = noData.contains(s11) ? 0 : 1;

                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = (computeValue(s00, s01, s10, s11,
                                                w00, w01, w10, w11, xfrac[i], yfrac[j], k));
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
                            final int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                final int posy = ypos[j] + bandOffset;
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    final int posx = xpos[i];

                                    // PixelPositions
                                    final int x0 = src.getX() + posx / srcPixelStride;
                                    final int y0 = src.getY() + (posy - bandOffset)
                                            / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {

                                        int w00 = roiIter.getSample(x0, y0, 0);
                                        int w01 = roiIter.getSample(x0 + 1, y0, 0);
                                        int w10 = roiIter.getSample(x0, y0 + 1, 0);
                                        int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0);

                                        if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                            // The destination no data value is saved in the destination array
                                            dstData[dstPixelOffset] = destinationNoDataInt[k];
                                        } else {
                                            // Get the four surrounding pixel values
                                            final int s00 = srcData[posx + posy];
                                            final int s01 = srcData[posx + srcPixelStride + posy];
                                            final int s10 = srcData[posx + posy + srcScanlineStride];
                                            final int s11 = srcData[posx + srcPixelStride + posy
                                                    + srcScanlineStride];

                                            w00 = noData.contains(s00) ? 0 : 1;
                                            w01 = noData.contains(s01) ? 0 : 1;
                                            w10 = noData.contains(s10) ? 0 : 1;
                                            w11 = noData.contains(s11) ? 0 : 1;

                                            // compute value
                                            dstData[dstPixelOffset] = (computeValue(s00, s01, s10,
                                                    s11, w00, w01, w10, w11, xfrac[i], yfrac[j], k));
                                        }
                                    } else {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataInt[k];
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
            int roiScanlineStride, RandomIter roiIter) {

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

        if (caseA) {
            // for all bands
            for (int k = 0; k < dnumBands; k++) {

                final float[] srcData = srcDataArrays[k];
                final float[] dstData = dstDataArrays[k];
                // Line and band Offset initialization
                int dstlineOffset = dstBandOffsets[k];
                final int bandOffset = bandOffsets[k];
                // cycle on the y values
                for (int j = 0; j < dheight; j++) {
                    // pixel offset initialization
                    int dstPixelOffset = dstlineOffset;
                    // y position selection
                    final int posy = ypos[j] + bandOffset;
                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        final int posx = xpos[i];
                        final int pos = posx + posy;

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
                        final int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            final int posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                final int posx = xpos[i];

                                final int pos = posx + posy;

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
                                    dstData[dstPixelOffset] = destinationNoDataFloat[k];
                                } else if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataFloat[k];
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
                        final int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            final int posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                final int posx = xpos[i];
                                // PixelPositions
                                final int x0 = src.getX() + posx / srcPixelStride;
                                final int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                if (roiBounds.contains(x0, y0)) {

                                    final int w00 = roiIter.getSample(x0, y0, 0);
                                    final int w01 = roiIter.getSample(x0 + 1, y0, 0);
                                    final int w10 = roiIter.getSample(x0, y0 + 1, 0);
                                    final int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0);

                                    if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataFloat[k];
                                    } else {

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
                                    }
                                } else {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataFloat[k];
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
                        final int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            final int posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                final int posx = xpos[i];

                                // Get the four surrounding pixel values
                                final float s00 = srcData[posx + posy];
                                final float s01 = srcData[posx + srcPixelStride + posy];
                                final float s10 = srcData[posx + posy + srcScanlineStride];
                                final float s11 = srcData[posx + srcPixelStride + posy
                                        + srcScanlineStride];

                                int w00 = noData.contains(s00) ? 0 : 1;
                                int w01 = noData.contains(s01) ? 0 : 1;
                                int w10 = noData.contains(s10) ? 0 : 1;
                                int w11 = noData.contains(s11) ? 0 : 1;

                                if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataFloat[k];
                                } else {
                                    // compute value
                                    dstData[dstPixelOffset] = (float) computeValueDouble(s00, s01,
                                            s10, s11, w00, w01, w10, w11, xfrac[i], yfrac[j],
                                            dataType, k);
                                }

                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else {
                    if (useRoiAccessor) {
                        // for all bands
                        for (int k = 0; k < dnumBands; k++) {

                            final float[] srcData = srcDataArrays[k];
                            final float[] dstData = dstDataArrays[k];
                            // Line and band Offset initialization
                            int dstlineOffset = dstBandOffsets[k];
                            final int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                final int posy = ypos[j] + bandOffset;
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    final int posx = xpos[i];

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
                                        dstData[dstPixelOffset] = destinationNoDataFloat[k];
                                    } else if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataFloat[k];
                                    } else {

                                        w00 = noData.contains(s00) ? 0 : 1;
                                        w01 = noData.contains(s01) ? 0 : 1;
                                        w10 = noData.contains(s10) ? 0 : 1;
                                        w11 = noData.contains(s11) ? 0 : 1;

                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = (float) computeValueDouble(s00,
                                                s01, s10, s11, w00, w01, w10, w11, xfrac[i],
                                                yfrac[j], dataType, k);
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
                            final int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                final int posy = ypos[j] + bandOffset;
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    final int posx = xpos[i];

                                    // PixelPositions
                                    final int x0 = src.getX() + posx / srcPixelStride;
                                    final int y0 = src.getY() + (posy - bandOffset)
                                            / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {

                                        int w00 = roiIter.getSample(x0, y0, 0);
                                        int w01 = roiIter.getSample(x0 + 1, y0, 0);
                                        int w10 = roiIter.getSample(x0, y0 + 1, 0);
                                        int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0);

                                        if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                            // The destination no data value is saved in the destination array
                                            dstData[dstPixelOffset] = destinationNoDataFloat[k];
                                        } else {
                                            // Get the four surrounding pixel values
                                            final float s00 = srcData[posx + posy];
                                            final float s01 = srcData[posx + srcPixelStride + posy];
                                            final float s10 = srcData[posx + posy
                                                    + srcScanlineStride];
                                            final float s11 = srcData[posx + srcPixelStride + posy
                                                    + srcScanlineStride];

                                            w00 = noData.contains(s00) ? 0 : 1;
                                            w01 = noData.contains(s01) ? 0 : 1;
                                            w10 = noData.contains(s10) ? 0 : 1;
                                            w11 = noData.contains(s11) ? 0 : 1;

                                            // compute value
                                            dstData[dstPixelOffset] = (float) computeValueDouble(
                                                    s00, s01, s10, s11, w00, w01, w10, w11,
                                                    xfrac[i], yfrac[j], dataType, k);
                                        }
                                    } else {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataFloat[k];
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
            int roiScanlineStride, RandomIter roiIter) {

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

        if (caseA) {
            // for all bands
            for (int k = 0; k < dnumBands; k++) {

                final double[] srcData = srcDataArrays[k];
                final double[] dstData = dstDataArrays[k];
                // Line and band Offset initialization
                int dstlineOffset = dstBandOffsets[k];
                final int bandOffset = bandOffsets[k];
                // cycle on the y values
                for (int j = 0; j < dheight; j++) {
                    // pixel offset initialization
                    int dstPixelOffset = dstlineOffset;
                    // y position selection
                    final int posy = ypos[j] + bandOffset;
                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        final int posx = xpos[i];
                        final int pos = posx + posy;

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
                        final int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            final int posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                final int posx = xpos[i];

                                final int pos = posx + posy;

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
                                    dstData[dstPixelOffset] = destinationNoDataDouble[k];
                                } else if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataDouble[k];
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
                        final int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            final int posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                final int posx = xpos[i];
                                // PixelPositions
                                final int x0 = src.getX() + posx / srcPixelStride;
                                final int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                if (roiBounds.contains(x0, y0)) {

                                    final int w00 = roiIter.getSample(x0, y0, 0);
                                    final int w01 = roiIter.getSample(x0 + 1, y0, 0);
                                    final int w10 = roiIter.getSample(x0, y0 + 1, 0);
                                    final int w11 = roiIter.getSample(x0 + 1, y0 + 1, 0);

                                    if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataDouble[k];
                                    } else {
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
                                    }
                                } else {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataDouble[k];
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
                        final int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            final int posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                final int posx = xpos[i];

                                // Get the four surrounding pixel values
                                final double s00 = srcData[posx + posy];
                                final double s01 = srcData[posx + srcPixelStride + posy];
                                final double s10 = srcData[posx + posy + srcScanlineStride];
                                final double s11 = srcData[posx + srcPixelStride + posy
                                        + srcScanlineStride];

                                int w00 = noData.contains(s00) ? 0 : 1;
                                int w01 = noData.contains(s01) ? 0 : 1;
                                int w10 = noData.contains(s10) ? 0 : 1;
                                int w11 = noData.contains(s11) ? 0 : 1;

                                if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                    dstData[dstPixelOffset] = destinationNoDataDouble[k];
                                } else {
                                    // compute value
                                    dstData[dstPixelOffset] = computeValueDouble(s00, s01, s10,
                                            s11, w00, w01, w10, w11, xfrac[i], yfrac[j], dataType, k);
                                }

                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else {
                    if (useRoiAccessor) {
                        // for all bands
                        for (int k = 0; k < dnumBands; k++) {

                            final double[] srcData = srcDataArrays[k];
                            final double[] dstData = dstDataArrays[k];
                            // Line and band Offset initialization
                            int dstlineOffset = dstBandOffsets[k];
                            final int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                final int posy = ypos[j] + bandOffset;
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    final int posx = xpos[i];

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
                                        dstData[dstPixelOffset] = destinationNoDataDouble[k];
                                    } else if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataDouble[k];
                                    } else {

                                        w00 = noData.contains(s00) ? 0 : 1;
                                        w01 = noData.contains(s01) ? 0 : 1;
                                        w10 = noData.contains(s10) ? 0 : 1;
                                        w11 = noData.contains(s11) ? 0 : 1;
                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = computeValueDouble(s00, s01, s10,
                                                s11, w00, w01, w10, w11, xfrac[i], yfrac[j],
                                                dataType, k);
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
                            final int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                final int posy = ypos[j] + bandOffset;
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    final int posx = xpos[i];

                                    // PixelPositions
                                    final int x0 = src.getX() + posx / srcPixelStride;
                                    final int y0 = src.getY() + (posy - bandOffset)
                                            / srcScanlineStride;

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

                                            w00 = noData.contains(s00) ? 0 : 1;
                                            w01 = noData.contains(s01) ? 0 : 1;
                                            w10 = noData.contains(s10) ? 0 : 1;
                                            w11 = noData.contains(s11) ? 0 : 1;

                                            // compute value
                                            dstData[dstPixelOffset] = computeValueDouble(s00, s01,
                                                    s10, s11, w00, w01, w10, w11, xfrac[i],
                                                    yfrac[j], dataType, k);

                                        } else {
                                            // The destination no data value is saved in the destination array
                                            dstData[dstPixelOffset] = destinationNoDataDouble[k];
                                        }
                                    } else {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataDouble[k];
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
            int w11, int xfrac, int yfrac, int k) {

        int s0 = 0;
        int s1 = 0;
        int s = 0;

        long s0L = 0;
        long s1L = 0;

        // Complementary values of the fractional part
        int xfracCompl = one - xfrac;
        int yfracCompl = one - yfrac;

        // Boolean indicating if a pixel weight is 0
        boolean w00z = w00 == 0;
        boolean w01z = w01 == 0;
        boolean w10z = w10 == 0;
        boolean w11z = w11 == 0;

        // Boolean indicating if 2 same line-pixel weights are 0
        boolean w0z = w00z && w01z;
        boolean w1z = w10z && w11z;

        if (w0z && w1z) {
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                return destinationNoDataByte[k];
            case DataBuffer.TYPE_USHORT:
                return destinationNoDataUShort[k];
            case DataBuffer.TYPE_SHORT:
                return destinationNoDataShort[k];
            case DataBuffer.TYPE_INT:
                return destinationNoDataInt[k];
            }
        }

        int shift = 29 - subsampleBits;
        // For Integer value is possible that a bitshift of "subsampleBits" could shift over the integer bit number
        // so the samples, in this case, are expanded to Long.
        boolean s0Long = ((s00 | s10) >>> shift == 0);
        boolean s1Long = ((s01 | s11) >>> shift == 0);

        // boolean indicating if the data type is DataBuffer.TYPE_INT
        boolean dataINT = dataType == DataBuffer.TYPE_INT;
        // All the possible weight combination are checked
        if (w00z || w01z || w10z || w11z) {
            // For integers is even considered the case when the integers are expanded to longs
            if (dataINT) {

                if (w0z) {
                    s0L = 0;
                } else if (w00z) {// w01 = 1
                    s0L = s01 * xfrac;
                } else if (w01z) {// w00 = 1
                    s0L = s00 * xfracCompl;
                } else {// w00 = 1 & W01 = 1
                    if (s0Long) {
                        if (s1Long) {
                            s0L = (s01 - s00) * xfrac + (s00 << subsampleBits);
                        } else {
                            s0L = (1L * s01 - s00) * xfrac + (1L * s00 << subsampleBits);
                        }
                    } else {
                        s0L = (1L * s01 - s00) * xfrac + (1L * s00 << subsampleBits);
                    }
                }

                // lower value

                if (w1z) {
                    s1L = 0;
                } else if (w10z) { // w11 = 1
                    s1L = s11 * xfrac;
                } else if (w11z) { // w10 = 1 // - (s10 * xfrac); //s10;
                    s1L = s10 * xfracCompl;
                } else {
                    if (s0Long) {
                        if (s1Long) {
                            s1L = (s11 - s10) * xfrac + (s10 << subsampleBits);
                        } else {
                            s1L = (1L * s11 - s10) * xfrac + (1L * s10 << subsampleBits);
                        }
                    } else {
                        s1L = (1L * s11 - s10) * xfrac + (1L * s10 << subsampleBits);
                    }
                }

                if (w0z) {
                    s = (int) ((s1L * yfrac + round2) >> shift2);
                } else {
                    if (w1z) {
                        s = (int) ((s0L * yfracCompl + round2) >> shift2);
                    } else {
                        s = (int) (((s1L - s0L) * yfrac + (s0L << subsampleBits) + round2) >> shift2);
                    }
                }

            } else {

                // S00 .......... S01
                //  .              .
                //  .              .
                //  .              .
                //  .              .
                //  .         *    .   <- yfrac
                //  .              .
                //  .              .
                // S10 .......... S11
                //
                //            ^
                //            |
                //          xfrac

                // bilinear interpolation does 2 interpolations along X and then interpolates
                // the results along y.
                // In case any of these interpolations involves a noData source pixel, 
                // the value of the other pixel will be only used in case the frac component
                // is nearest to that pixel. Otherwise, the result will be noData too.
                // To give an example: in case S01 is noData, S00 will not contribute to the 
                // output value since it is too far from the position.
                // This avoids having shaded dark edges going out of the original valid bounds

                // Whether S*1 Pixel will fully contribute if opposite S*0 pixel is nodata
                final boolean xt1 = xfrac >= FRACTION_THRESHOLD_I;

                // Whether S*0 Pixel will fully contribute if opposite S*1 pixel is nodata
                final boolean xt0 = xfracCompl >= FRACTION_THRESHOLD_I;

                // Whether Previous horizontal interpolation on S1* pixels will contribute
                final boolean yt1 = yfrac >= FRACTION_THRESHOLD_I;

                // Whether Previous horizontal interpolation on S0* pixels will contribute
                final boolean yt0 = yfracCompl >= FRACTION_THRESHOLD_I;

                // First horizontal interpolation
                if (w0z) {
                    s0 = 0;
                } else if (w00z) { // w01 = 1
                    s0 = xt1 ? s01 << FULL_WEIGHT_SHIFT : 0;
                } else if (w01z) {// w00 = 1
                    s0 = xt0 ? s00 << FULL_WEIGHT_SHIFT : 0;
                } else {// w00 = 1 & W01 = 1
                    s0 = (s01 - s00) * xfrac + (s00 << subsampleBits);
                }

                // Second horizontal interpolation
                if (w1z) {
                    s1 = 0;
                } else if (w10z) { // w11 = 1
                    s1 = xt1 ? s11 << FULL_WEIGHT_SHIFT : 0;
                } else if (w11z) { // w10 = 1
                    s1 = xt0 ? s10 << FULL_WEIGHT_SHIFT : 0;
                } else {
                    s1 = (s11 - s10) * xfrac + (s10 << subsampleBits);
                }

                // Combining threshold weights with the nodata flags
                w00z &= xt0;
                w01z &= xt1;
                w10z &= xt0;
                w11z &= xt1;

                // Vertical interpolation
                if (w0z || w00z || w01z) {
                    s = yt1 && !w1z && !w10z && !w11z
                            ? (((s1 << FULL_WEIGHT_SHIFT) + round2) >> shift2)
                            : destinationNoDataInt[k];
                } else if (w1z || w10z || w11z) {
                    s = yt0 && !w0z && !w00z && !w01z
                            ? (((s0 << FULL_WEIGHT_SHIFT) + round2) >> shift2)
                            : destinationNoDataInt[k];
                } else {
                    s = (((s1 - s0) * yfrac + (s0 << subsampleBits) + round2) >> shift2);
                }
            }
        } else {
            // Perform the bilinear interpolation
            if (dataINT) {
                if (s0Long) {
                    if (s1Long) {
                        s0 = (s01 - s00) * xfrac + (s00 << subsampleBits);
                        s1 = (s11 - s10) * xfrac + (s10 << subsampleBits);
                        s = ((s1 - s0) * yfrac + (s0 << subsampleBits) + round2) >> shift2;
                    } else {
                        s0L = (1L * s01 - s00) * xfrac + (s00 << subsampleBits);
                        s1L = (1L * s11 - s10) * xfrac + (s10 << subsampleBits);
                        s = (int) (((s1L - s0L) * yfrac + (s0L << subsampleBits) + round2) >> shift2);
                    }
                } else {
                    s0L = (1L * s01 - s00) * xfrac + (1L * s00 << subsampleBits);
                    s1L = (1L * s11 - s10) * xfrac + (1L * s10 << subsampleBits);
                    s = (int) (((s1L - s0L) * yfrac + (s0L << subsampleBits) + round2) >> shift2);
                }
            } else {
                s0 = (s01 - s00) * xfrac + (s00 << subsampleBits);
                s1 = (s11 - s10) * xfrac + (s10 << subsampleBits);
                s = ((s1 - s0) * yfrac + (s0 << subsampleBits) + round2) >> shift2;
            }
        }

        return s;
    }

    /* Private method for calculate bilinear interpolation for float/double dataType */
    private double computeValueDouble(double s00, double s01, double s10, double s11, double w00,
            double w01, double w10, double w11, double xfrac, double yfrac, int dataType, int k) {

        double s0 = 0;
        double s1 = 0;
        double s = 0;

        // Boolean indicating if a pixel weight is 0
        boolean w00z = w00 == 0;
        boolean w01z = w01 == 0;
        boolean w10z = w10 == 0;
        boolean w11z = w11 == 0;

        if (w00z && w01z && w10z && w11z) {
            switch (dataType) {
            case DataBuffer.TYPE_FLOAT:
                return destinationNoDataFloat[k];
            case DataBuffer.TYPE_DOUBLE:
                return destinationNoDataDouble[k];
            }
        }

        // Boolean indicating if 2 same line-pixel weights are 0
        final boolean w0z = w00z && w01z;
        final boolean w1z = w10z && w11z;

        // Complementary values of the fractional part
        final double xfracCompl = 1 - xfrac;
        final double yfracCompl = 1 - yfrac;

        if (w00z || w01z || w10z || w11z) {

            // S00 .......... S01
            //  .              .
            //  .              .
            //  .              .
            //  .              .
            //  .         *    .   <- yfrac
            //  .              .
            //  .              .
            // S10 .......... S11
            //
            //            ^
            //            |
            //          xfrac

            final boolean xt1 = xfrac >= FRACTION_THRESHOLD_D;
            final boolean xt0 = xfracCompl >= FRACTION_THRESHOLD_D;
            final boolean yt1 = yfrac >= FRACTION_THRESHOLD_D;
            final boolean yt0 = yfracCompl >= FRACTION_THRESHOLD_D;

            // First horizontal interpolation
            if (w0z) {
                s0 = 0;
            } else if (w00z) { // w01 = 1
                s0 = xt1 ? s01 : 0;
            } else if (w01z) {// w00 = 1
                s0 = xt0 ? s00 : 0;// s00;
            } else {// w00 = 1 & W01 = 1
                s0 = (s01 - s00) * xfrac + s00;
            }

            // Second horizontal interpolation
            if (w1z) {
                s1 = 0;
            } else if (w10z) { // w11 = 1
                s1 = xt1 ? s11 : 0;
            } else if (w11z) { // w10 = 1
                s1 = xt0 ? s10 : 0;// - (s10 * xfrac); //s10;
            } else {
                s1 = (s11 - s10) * xfrac + s10;
            }

            // Combining threshold weights with the zero flags
            w00z &= !xt1;
            w01z &= !xt0;
            w10z &= !xt1;
            w11z &= !xt0;

            // Vertical interpolation
            if (w0z || w00z || w01z) {
                s = yt1 && !w1z && !w10z && !w11z ? s1 : dataType == DataBuffer.TYPE_FLOAT ? destinationNoDataFloat[k] : destinationNoDataDouble[k];
            } else if (w1z || w10z || w11z) {
                s = yt0 && !w0z && !w00z && !w01z ? s0 : dataType == DataBuffer.TYPE_FLOAT ? destinationNoDataFloat[k] : destinationNoDataDouble[k];
            } else {
                s = (s1 - s0) * yfrac + s0;
            }
        } else {

            // Perform the bilinear interpolation because all the weight are not 0.
            s0 = (s01 - s00) * xfrac + s00;
            s1 = (s11 - s10) * xfrac + s10;
            s = (s1 - s0) * yfrac + s0;
        }

        // Simple conversion for float dataType.
        return s;
    }

}
