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

import it.geosolutions.jaiext.interpolators.InterpolationNearest;
import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;

public class ScaleNearestOpImage extends ScaleOpImage {

    /** Nearest-Neighbor interpolator */
    protected InterpolationNearest interpN = null;

    /** Byte lookuptable used if no data are present */
    protected byte[][] byteLookupTable;

    public ScaleNearestOpImage(RenderedImage source, ImageLayout layout, Map configuration,
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
        if (srcColorModel instanceof IndexColorModel) {
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
        if (backgroundValues != null && backgroundValues.length > 0){
            destNod = backgroundValues;
        }
        if (interp instanceof InterpolationNearest) {
            interpN = (InterpolationNearest) interp;
            this.interp = interpN;
            interpN.setROIBounds(roiBounds);
            if(nod == null){
            	nod = interpN.getNoDataRange();
            }
            if(destNod == null){
            	destNod = new double[]{interpN.getDestinationNoData()};
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
                byte value = (byte) i;
                for (int b = 0; b < numBands; b++) {
                    if (noData.contains(value)) {
                        byteLookupTable[b][i] = destinationNoDataByte[b];
                    } else {
                        byteLookupTable[b][i] = value;
                    }
                }
            }
        }

        //Definition of the possible cases that can be found
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

        preComputePositionsInt(destRect, srcRect.x, srcRect.y, srcPixelStride, srcScanlineStride,
                xpos, ypos, xfracValues, yfracValues, roiScanlineStride, yposRoi);
        // destination data type
        dataType = dest.getSampleModel().getDataType();

        // This methods differs only for the presence of the roi or if the image is a binary one

        // if is binary
        // computeLoopBynary(srcAccessor, source, dest, destRect, xpos, ypos,yposRoi, xfracvalues,
        // yfracvalues,roi,yposRoi,srcRect.x, srcRect.y);

        switch (dstAccessor.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(srcAccessor, srcRect, destRect, dstAccessor, xpos, ypos, roiAccessor, yposRoi, roiIter);
            break;
        case DataBuffer.TYPE_USHORT:
            ushortLoop(srcAccessor, destRect, dstAccessor, xpos, ypos, roiAccessor, yposRoi, roiIter);
            break;
        case DataBuffer.TYPE_SHORT:
            shortLoop(srcAccessor, destRect, dstAccessor, xpos, ypos, roiAccessor, yposRoi, roiIter);
            break;
        case DataBuffer.TYPE_INT:
            intLoop(srcAccessor, destRect, dstAccessor, xpos, ypos, roiAccessor, yposRoi, roiIter);
            break;
        case DataBuffer.TYPE_FLOAT:
            floatLoop(srcAccessor, destRect, dstAccessor, xpos, ypos, roiAccessor, yposRoi, roiIter);
            break;
        case DataBuffer.TYPE_DOUBLE:
            doubleLoop(srcAccessor, destRect, dstAccessor, xpos, ypos, roiAccessor, yposRoi, roiIter);
            break;
        }

    }

    private void byteLoop(RasterAccessor src, Rectangle srcRect, Rectangle dstRect,
            RasterAccessor dst, int[] xpos, int[] ypos, RasterAccessor roi, int[] yposRoi, RandomIter roiIter) {

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
                        // The interpolated value is saved in the destination array
                        dstData[dstPixelOffset] = srcData[pos];

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
                            // roi y position initialization
                            int posyROI = yposRoi[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                int pos = posx + posy;

                                int windex = (posx / dnumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataByte[k];
                                } else {
                                    // The interpolated value is saved in the destination array
                                    dstData[dstPixelOffset] = srcData[pos];
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
                                int pos = posx + posy;
                                // PixelPositions
                                int x0 = src.getX() + posx / srcPixelStride;
                                int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                if (roiBounds.contains(x0, y0)) {
                                    int w = roiIter.getSample(x0, y0, 0) & 0xff;
                                    if (w == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataByte[k];
                                    } else {
                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = srcData[pos];
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

                                int value = srcData[pos];

                                dstData[dstPixelOffset] = byteLookupTable[k][value&0xFF];

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
                            int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                int posy = ypos[j] + bandOffset;
                                // roi y position initialization
                                int posyROI = yposRoi[j];
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];

                                    int pos = posx + posy;

                                    int value = srcData[pos];

                                    if (byteLookupTable[k][value&0xFF] == destinationNoDataByte[k]) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataByte[k];
                                    } else {
                                        int windex = (posx / dnumBands) + posyROI;
                                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff
                                                : 0;

                                        if (w == 0) {
                                            // The destination no data value is saved in the destination array
                                            dstData[dstPixelOffset] = destinationNoDataByte[k];
                                        } else {
                                            // The interpolated value is saved in the destination array
                                            dstData[dstPixelOffset] = byteLookupTable[k][value&0xFF];
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
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];
                                    int pos = posx + posy;
                                    int value = srcData[pos];

                                    if (byteLookupTable[k][value&0xFF] == destinationNoDataByte[k]) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataByte[k];
                                    } else {
                                        // PixelPositions
                                        int x0 = src.getX() + posx / srcPixelStride;
                                        int y0 = src.getY() + (posy - bandOffset)
                                                / srcScanlineStride;

                                        if (roiBounds.contains(x0, y0)) {
                                            int w = roiIter.getSample(x0, y0, 0) & 0xff;
                                            if (w == 0) {
                                                // The destination no data value is saved in the destination array
                                                dstData[dstPixelOffset] = destinationNoDataByte[k];
                                            } else {
                                                // The interpolated value is saved in the destination array
                                                dstData[dstPixelOffset] = byteLookupTable[k][value&0xFF];
                                            }
                                        } else {
                                            // The destination no data value is saved in the destination array
                                            dstData[dstPixelOffset] = destinationNoDataByte[k];
                                        }
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
            int[] ypos, RasterAccessor roi, int[] yposRoi, RandomIter roiIter) {

        // BandOffsets
        int srcScanlineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int bandOffsets[] = src.getBandOffsets();
        // Destination rectangle dimensions
        int dwidth = dstRect.width;
        int dheight = dstRect.height;
        // Destination image band numbers
        int dnumBands = dst.getNumBands();
        // Destination bandOffsets, PixelStride and ScanLineStride
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        // Destination and source data arrays (for all bands)
        short[][] srcDataArrays = src.getShortDataArrays();

        short[][] dstDataArrays = dst.getShortDataArrays();

        // Destination and source data arrays (for a single band)
        short[] dstData = null;
        short[] srcData = null;

        byte[] roiDataArray = null;
        int roiDataLength = 0;

        int w, posx, posy, pos, posyROI, windex;

        if (useRoiAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
        }

        if (caseA) {
            // for all bands
            for (int k = 0; k < dnumBands; k++) {

                srcData = srcDataArrays[k];
                dstData = dstDataArrays[k];
                // Line and band Offset initialization
                int dstlineOffset = dstBandOffsets[k];
                int bandOffset = bandOffsets[k];
                // cycle on the y values
                for (int j = 0; j < dheight; j++) {
                    // pixel offset initialization
                    int dstPixelOffset = dstlineOffset;
                    // y position selection
                    posy = ypos[j] + bandOffset;
                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        posx = xpos[i];

                        pos = posx + posy;
                        // The interpolated value is saved in the destination array
                        dstData[dstPixelOffset] = srcData[pos];

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
                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];
                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // roi y position initialization
                            posyROI = yposRoi[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                windex = (posx / dnumBands) + posyROI;
                                w = windex < roiDataLength ? roiDataArray[windex] & 0xffff : 0;

                                if (w == 0) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataUShort[k];
                                } else {
                                    // The interpolated value is saved in the destination array
                                    dstData[dstPixelOffset] = srcData[pos];
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

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];
                                pos = posx + posy;
                                // PixelPositions
                                int x0 = src.getX() + posx / srcPixelStride;
                                int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                if (roiBounds.contains(x0, y0)) {
                                    w = roiIter.getSample(x0, y0, 0) & 0xffff;
                                    if (w == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataUShort[k];
                                    } else {
                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = srcData[pos];
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
            } else if (caseC) {
                // for all bands
                for (int k = 0; k < dnumBands; k++) {

                    srcData = srcDataArrays[k];
                    dstData = dstDataArrays[k];

                    // Line and band Offset initialization
                    int dstlineOffset = dstBandOffsets[k];
                    int bandOffset = bandOffsets[k];
                    // cycle on the y values
                    for (int j = 0; j < dheight; j++) {
                        // pixel offset initialization
                        int dstPixelOffset = dstlineOffset;
                        // y position selection
                        posy = ypos[j] + bandOffset;
                        // cycle on the x values
                        for (int i = 0; i < dwidth; i++) {
                            // x position selection
                            posx = xpos[i];

                            pos = posx + posy;

                            short value = srcData[pos];

                            if (noData.contains(value)) {
                                // The destination no data value is saved in the destination array
                                dstData[dstPixelOffset] = destinationNoDataUShort[k];
                            } else {
                                // The interpolated value is saved in the destination array
                                dstData[dstPixelOffset] = value;
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

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // roi y position initialization
                            posyROI = yposRoi[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                short value = srcData[pos];

                                if (noData.contains(value)) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataUShort[k];
                                } else {
                                    windex = (posx / dnumBands) + posyROI;
                                    w = windex < roiDataLength ? roiDataArray[windex] & 0xffff : 0;

                                    if (w == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataUShort[k];
                                    } else {
                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = value;
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

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                short value = srcData[pos];

                                if (noData.contains(value)) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataUShort[k];
                                } else {
                                    // PixelPositions
                                    int x0 = src.getX() + posx / srcPixelStride;
                                    int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {
                                        w = roiIter.getSample(x0, y0, 0) & 0xffff;
                                        if (w == 0) {
                                            // The destination no data value is saved in the destination array
                                            dstData[dstPixelOffset] = destinationNoDataUShort[k];
                                        } else {
                                            // The interpolated value is saved in the destination array
                                            dstData[dstPixelOffset] = value;
                                        }
                                    } else {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataUShort[k];
                                    }
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

    private void shortLoop(RasterAccessor src, Rectangle dstRect, RasterAccessor dst, int[] xpos,
            int[] ypos, RasterAccessor roi, int[] yposRoi, RandomIter roiIter) {

        // BandOffsets
        int srcScanlineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int bandOffsets[] = src.getBandOffsets();
        // Destination rectangle dimensions
        int dwidth = dstRect.width;
        int dheight = dstRect.height;
        // Destination image band numbers
        int dnumBands = dst.getNumBands();
        // Destination bandOffsets, PixelStride and ScanLineStride
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        // Destination and source data arrays (for all bands)
        short[][] srcDataArrays = src.getShortDataArrays();

        short[][] dstDataArrays = dst.getShortDataArrays();

        // Destination and source data arrays (for a single band)
        short[] dstData = null;
        short[] srcData = null;

        byte[] roiDataArray = null;
        int roiDataLength = 0;

        int w, posx, posy, pos, posyROI, windex;

        if (useRoiAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
        }

        if (caseA) {
            // for all bands
            for (int k = 0; k < dnumBands; k++) {

                srcData = srcDataArrays[k];
                dstData = dstDataArrays[k];
                // Line and band Offset initialization
                int dstlineOffset = dstBandOffsets[k];
                int bandOffset = bandOffsets[k];
                // cycle on the y values
                for (int j = 0; j < dheight; j++) {
                    // pixel offset initialization
                    int dstPixelOffset = dstlineOffset;
                    // y position selection
                    posy = ypos[j] + bandOffset;
                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        posx = xpos[i];

                        pos = posx + posy;
                        // The interpolated value is saved in the destination array
                        dstData[dstPixelOffset] = srcData[pos];

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
                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];
                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // roi y position initialization
                            posyROI = yposRoi[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                windex = (posx / dnumBands) + posyROI;
                                w = windex < roiDataLength ? roiDataArray[windex] : 0;

                                if (w == 0) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataShort[k];
                                } else {
                                    // The interpolated value is saved in the destination array
                                    dstData[dstPixelOffset] = srcData[pos];
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

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];
                                pos = posx + posy;
                                // PixelPositions
                                int x0 = src.getX() + posx / srcPixelStride;
                                int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                if (roiBounds.contains(x0, y0)) {
                                    w = roiIter.getSample(x0, y0, 0);
                                    if (w == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataShort[k];
                                    } else {
                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = srcData[pos];
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
            } else if (caseC) {
                // for all bands
                for (int k = 0; k < dnumBands; k++) {

                    srcData = srcDataArrays[k];
                    dstData = dstDataArrays[k];

                    // Line and band Offset initialization
                    int dstlineOffset = dstBandOffsets[k];
                    int bandOffset = bandOffsets[k];
                    // cycle on the y values
                    for (int j = 0; j < dheight; j++) {
                        // pixel offset initialization
                        int dstPixelOffset = dstlineOffset;
                        // y position selection
                        posy = ypos[j] + bandOffset;
                        // cycle on the x values
                        for (int i = 0; i < dwidth; i++) {
                            // x position selection
                            posx = xpos[i];

                            pos = posx + posy;

                            short value = srcData[pos];

                            if (noData.contains(value)) {
                                // The destination no data value is saved in the destination array
                                dstData[dstPixelOffset] = destinationNoDataShort[k];
                            } else {
                                // The interpolated value is saved in the destination array
                                dstData[dstPixelOffset] = value;
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

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // roi y position initialization
                            posyROI = yposRoi[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                short value = srcData[pos];

                                if (noData.contains(value)) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataShort[k];
                                } else {
                                    windex = (posx / dnumBands) + posyROI;
                                    w = windex < roiDataLength ? roiDataArray[windex] : 0;

                                    if (w == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataShort[k];
                                    } else {
                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = value;
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

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                short value = srcData[pos];

                                if (noData.contains(value)) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataShort[k];
                                } else {
                                    // PixelPositions
                                    int x0 = src.getX() + posx / srcPixelStride;
                                    int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {
                                        w = roiIter.getSample(x0, y0, 0);
                                        if (w == 0) {
                                            // The destination no data value is saved in the destination array
                                            dstData[dstPixelOffset] = destinationNoDataShort[k];
                                        } else {
                                            // The interpolated value is saved in the destination array
                                            dstData[dstPixelOffset] = value;
                                        }
                                    } else {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataShort[k];
                                    }
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

    private void intLoop(RasterAccessor src, Rectangle dstRect, RasterAccessor dst, int[] xpos,
            int[] ypos, RasterAccessor roi, int[] yposRoi, RandomIter roiIter) {
        // BandOffsets
        int srcScanlineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int bandOffsets[] = src.getBandOffsets();
        // Destination rectangle dimensions
        int dwidth = dstRect.width;
        int dheight = dstRect.height;
        // Destination image band numbers
        int dnumBands = dst.getNumBands();
        // Destination bandOffsets, PixelStride and ScanLineStride
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        // Destination and source data arrays (for all bands)
        int[][] srcDataArrays = src.getIntDataArrays();

        int[][] dstDataArrays = dst.getIntDataArrays();

        // Destination and source data arrays (for a single band)
        int[] dstData = null;
        int[] srcData = null;

        byte[] roiDataArray = null;
        int roiDataLength = 0;

        int w, posx, posy, pos, posyROI, windex;

        if (useRoiAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
        }

        if (caseA) {
            // for all bands
            for (int k = 0; k < dnumBands; k++) {

                srcData = srcDataArrays[k];
                dstData = dstDataArrays[k];
                // Line and band Offset initialization
                int dstlineOffset = dstBandOffsets[k];
                int bandOffset = bandOffsets[k];
                // cycle on the y values
                for (int j = 0; j < dheight; j++) {
                    // pixel offset initialization
                    int dstPixelOffset = dstlineOffset;
                    // y position selection
                    posy = ypos[j] + bandOffset;
                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        posx = xpos[i];

                        pos = posx + posy;
                        // The interpolated value is saved in the destination array
                        dstData[dstPixelOffset] = srcData[pos];

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
                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];
                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // roi y position initialization
                            posyROI = yposRoi[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                windex = (posx / dnumBands) + posyROI;
                                w = windex < roiDataLength ? roiDataArray[windex] : 0;

                                if (w == 0) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataInt[k];
                                } else {
                                    // The interpolated value is saved in the destination array
                                    dstData[dstPixelOffset] = srcData[pos];
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

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];
                                pos = posx + posy;
                                // PixelPositions
                                int x0 = src.getX() + posx / srcPixelStride;
                                int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                if (roiBounds.contains(x0, y0)) {
                                    w = roiIter.getSample(x0, y0, 0);
                                    if (w == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataInt[k];
                                    } else {
                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = srcData[pos];
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
            } else if (caseC) {
                // for all bands
                for (int k = 0; k < dnumBands; k++) {

                    srcData = srcDataArrays[k];
                    dstData = dstDataArrays[k];

                    // Line and band Offset initialization
                    int dstlineOffset = dstBandOffsets[k];
                    int bandOffset = bandOffsets[k];
                    // cycle on the y values
                    for (int j = 0; j < dheight; j++) {
                        // pixel offset initialization
                        int dstPixelOffset = dstlineOffset;
                        // y position selection
                        posy = ypos[j] + bandOffset;
                        // cycle on the x values
                        for (int i = 0; i < dwidth; i++) {
                            // x position selection
                            posx = xpos[i];

                            pos = posx + posy;

                            int value = srcData[pos];

                            if (noData.contains(value)) {
                                // The destination no data value is saved in the destination array
                                dstData[dstPixelOffset] = destinationNoDataInt[k];
                            } else {
                                // The interpolated value is saved in the destination array
                                dstData[dstPixelOffset] = value;
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

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // roi y position initialization
                            posyROI = yposRoi[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                int value = srcData[pos];

                                if (noData.contains(value)) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataInt[k];
                                } else {
                                    windex = (posx / dnumBands) + posyROI;
                                    w = windex < roiDataLength ? roiDataArray[windex] : 0;

                                    if (w == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataInt[k];
                                    } else {
                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = value;
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

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                int value = srcData[pos];

                                if (noData.contains(value)) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataInt[k];
                                } else {
                                    // PixelPositions
                                    int x0 = src.getX() + posx / srcPixelStride;
                                    int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {
                                        w = roiIter.getSample(x0, y0, 0);
                                        if (w == 0) {
                                            // The destination no data value is saved in the destination array
                                            dstData[dstPixelOffset] = destinationNoDataInt[k];
                                        } else {
                                            // The interpolated value is saved in the destination array
                                            dstData[dstPixelOffset] = value;
                                        }
                                    } else {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataInt[k];
                                    }
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

    private void floatLoop(RasterAccessor src, Rectangle dstRect, RasterAccessor dst, int[] xpos,
            int[] ypos, RasterAccessor roi, int[] yposRoi, RandomIter roiIter) {

        // BandOffsets
        int srcScanlineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int bandOffsets[] = src.getBandOffsets();
        // Destination rectangle dimensions
        int dwidth = dstRect.width;
        int dheight = dstRect.height;
        // Destination image band numbers
        int dnumBands = dst.getNumBands();
        // Destination bandOffsets, PixelStride and ScanLineStride
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        // Destination and source data arrays (for all bands)
        float[][] srcDataArrays = src.getFloatDataArrays();

        float[][] dstDataArrays = dst.getFloatDataArrays();

        // Destination and source data arrays (for a single band)
        float[] dstData = null;
        float[] srcData = null;

        byte[] roiDataArray = null;
        int roiDataLength = 0;

        int w, posx, posy, pos, posyROI, windex;

        if (useRoiAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
        }

        if (caseA) {
            // for all bands
            for (int k = 0; k < dnumBands; k++) {

                srcData = srcDataArrays[k];
                dstData = dstDataArrays[k];
                // Line and band Offset initialization
                int dstlineOffset = dstBandOffsets[k];
                int bandOffset = bandOffsets[k];
                // cycle on the y values
                for (int j = 0; j < dheight; j++) {
                    // pixel offset initialization
                    int dstPixelOffset = dstlineOffset;
                    // y position selection
                    posy = ypos[j] + bandOffset;
                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        posx = xpos[i];

                        pos = posx + posy;
                        // The interpolated value is saved in the destination array
                        dstData[dstPixelOffset] = srcData[pos];

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
                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];
                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // roi y position initialization
                            posyROI = yposRoi[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                windex = (posx / dnumBands) + posyROI;
                                w = windex < roiDataLength ? roiDataArray[windex] : 0;

                                if (w == 0) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataFloat[k];
                                } else {
                                    // The interpolated value is saved in the destination array
                                    dstData[dstPixelOffset] = srcData[pos];
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

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];
                                pos = posx + posy;
                                // PixelPositions
                                int x0 = src.getX() + posx / srcPixelStride;
                                int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                if (roiBounds.contains(x0, y0)) {
                                    w = roiIter.getSample(x0, y0, 0);
                                    if (w == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataFloat[k];
                                    } else {
                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = srcData[pos];
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
            } else if (caseC) {
                // for all bands
                for (int k = 0; k < dnumBands; k++) {

                    srcData = srcDataArrays[k];
                    dstData = dstDataArrays[k];

                    // Line and band Offset initialization
                    int dstlineOffset = dstBandOffsets[k];
                    int bandOffset = bandOffsets[k];
                    // cycle on the y values
                    for (int j = 0; j < dheight; j++) {
                        // pixel offset initialization
                        int dstPixelOffset = dstlineOffset;
                        // y position selection
                        posy = ypos[j] + bandOffset;
                        // cycle on the x values
                        for (int i = 0; i < dwidth; i++) {
                            // x position selection
                            posx = xpos[i];

                            pos = posx + posy;

                            float value = srcData[pos];

                            if (noData.contains(value)) {
                                // The destination no data value is saved in the destination array
                                dstData[dstPixelOffset] = destinationNoDataFloat[k];
                            } else {
                                // The interpolated value is saved in the destination array
                                dstData[dstPixelOffset] = value;
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

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // roi y position initialization
                            posyROI = yposRoi[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                float value = srcData[pos];

                                if (noData.contains(value)) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataFloat[k];
                                } else {
                                    windex = (posx / dnumBands) + posyROI;
                                    w = windex < roiDataLength ? roiDataArray[windex] : 0;

                                    if (w == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataFloat[k];
                                    } else {
                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = value;
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

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                float value = srcData[pos];

                                if (noData.contains(value)) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataFloat[k];
                                } else {
                                    // PixelPositions
                                    int x0 = src.getX() + posx / srcPixelStride;
                                    int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {
                                        w = roiIter.getSample(x0, y0, 0);
                                        if (w == 0) {
                                            // The destination no data value is saved in the destination array
                                            dstData[dstPixelOffset] = destinationNoDataFloat[k];
                                        } else {
                                            // The interpolated value is saved in the destination array
                                            dstData[dstPixelOffset] = value;
                                        }
                                    } else {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataFloat[k];
                                    }
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

    private void doubleLoop(RasterAccessor src, Rectangle dstRect, RasterAccessor dst, int[] xpos,
            int[] ypos, RasterAccessor roi, int[] yposRoi, RandomIter roiIter) {

        // BandOffsets
        int srcScanlineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int bandOffsets[] = src.getBandOffsets();
        // Destination rectangle dimensions
        int dwidth = dstRect.width;
        int dheight = dstRect.height;
        // Destination image band numbers
        int dnumBands = dst.getNumBands();
        // Destination bandOffsets, PixelStride and ScanLineStride
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        // Destination and source data arrays (for all bands)
        double[][] srcDataArrays = src.getDoubleDataArrays();

        double[][] dstDataArrays = dst.getDoubleDataArrays();

        // Destination and source data arrays (for a single band)
        double[] dstData = null;
        double[] srcData = null;

        byte[] roiDataArray = null;
        int roiDataLength = 0;

        int w, posx, posy, pos, posyROI, windex;

        if (useRoiAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
        }

        if (caseA) {
            // for all bands
            for (int k = 0; k < dnumBands; k++) {

                srcData = srcDataArrays[k];
                dstData = dstDataArrays[k];
                // Line and band Offset initialization
                int dstlineOffset = dstBandOffsets[k];
                int bandOffset = bandOffsets[k];
                // cycle on the y values
                for (int j = 0; j < dheight; j++) {
                    // pixel offset initialization
                    int dstPixelOffset = dstlineOffset;
                    // y position selection
                    posy = ypos[j] + bandOffset;
                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        posx = xpos[i];

                        pos = posx + posy;
                        // The interpolated value is saved in the destination array
                        dstData[dstPixelOffset] = srcData[pos];

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
                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];
                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // roi y position initialization
                            posyROI = yposRoi[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                windex = (posx / dnumBands) + posyROI;
                                w = windex < roiDataLength ? roiDataArray[windex] : 0;

                                if (w == 0) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataDouble[k];
                                } else {
                                    // The interpolated value is saved in the destination array
                                    dstData[dstPixelOffset] = srcData[pos];
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

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];
                                pos = posx + posy;
                                // PixelPositions
                                int x0 = src.getX() + posx / srcPixelStride;
                                int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                if (roiBounds.contains(x0, y0)) {
                                    w = roiIter.getSample(x0, y0, 0);
                                    if (w == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataDouble[k];
                                    } else {
                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = srcData[pos];
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
            } else if (caseC) {
                // for all bands
                for (int k = 0; k < dnumBands; k++) {

                    srcData = srcDataArrays[k];
                    dstData = dstDataArrays[k];

                    // Line and band Offset initialization
                    int dstlineOffset = dstBandOffsets[k];
                    int bandOffset = bandOffsets[k];
                    // cycle on the y values
                    for (int j = 0; j < dheight; j++) {
                        // pixel offset initialization
                        int dstPixelOffset = dstlineOffset;
                        // y position selection
                        posy = ypos[j] + bandOffset;
                        // cycle on the x values
                        for (int i = 0; i < dwidth; i++) {
                            // x position selection
                            posx = xpos[i];

                            pos = posx + posy;

                            double value = srcData[pos];

                            if (noData.contains(value)) {
                                // The destination no data value is saved in the destination array
                                dstData[dstPixelOffset] = destinationNoDataDouble[k];
                            } else {
                                // The interpolated value is saved in the destination array
                                dstData[dstPixelOffset] = value;
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

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // roi y position initialization
                            posyROI = yposRoi[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                double value = srcData[pos];

                                if (noData.contains(value)) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataDouble[k];
                                } else {
                                    windex = (posx / dnumBands) + posyROI;
                                    w = windex < roiDataLength ? roiDataArray[windex] : 0;

                                    if (w == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataDouble[k];
                                    } else {
                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = value;
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

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                double value = srcData[pos];

                                if (noData.contains(value)) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataDouble[k];
                                } else {
                                    // PixelPositions
                                    int x0 = src.getX() + posx / srcPixelStride;
                                    int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {
                                        w = roiIter.getSample(x0, y0, 0);
                                        if (w == 0) {
                                            // The destination no data value is saved in the destination array
                                            dstData[dstPixelOffset] = destinationNoDataDouble[k];
                                        } else {
                                            // The interpolated value is saved in the destination array
                                            dstData[dstPixelOffset] = value;
                                        }
                                    } else {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataDouble[k];
                                    }
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
