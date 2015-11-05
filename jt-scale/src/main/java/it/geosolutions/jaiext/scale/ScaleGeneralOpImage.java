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
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationBicubic2;
import javax.media.jai.InterpolationTable;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.iterator.RandomIter;

import com.sun.media.jai.util.ImageUtil;
import com.sun.media.jai.util.Rational;

import it.geosolutions.jaiext.interpolators.InterpolationBicubic;
import it.geosolutions.jaiext.interpolators.InterpolationBilinear;
import it.geosolutions.jaiext.interpolators.InterpolationNearest;
import it.geosolutions.jaiext.interpolators.InterpolationNoData;
import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;

/**
 * This class is an extends the functionality of the ScaleOpImage class by adding the support for No Data values and by extending the ROI support for
 * all the image types and for binary images. The interpolation type performed by the scale operation is indicated by the Interpolation Object used.
 * For Nearest-Neighbor, Bilinear, Bicubic/Bicubic2 interpolation type, the new Interpolation class InterpolationNearest, InterpolationBilinear,
 * InterpolationBicubic should be used for having optimized calculation on the scale operation, ROI and No Data support. If these special
 * Interpolation objects are not used, the interpolation is performed by using the interpolate() method of the interpolator used in the selected
 * kernel, but without ROI and No Data support. Another main difference from the old Scale operations is the reduction of all the operations to one
 * singular class instead of having various different classes. 
 */

// @SuppressWarnings("unchecked")
public class ScaleGeneralOpImage extends ScaleOpImage {

    /** Nearest-Neighbor interpolator */
    protected InterpolationNearest interpN = null;

    /** Bilinear interpolator */
    protected InterpolationBilinear interpB = null;

    /** Bicubic interpolator */
    protected InterpolationBicubic interpBN = null;

    // Simple constructor used for interpolators different from InterpolationNearest2, InterpolationBilinear2, InterpolationBicubic
    public ScaleGeneralOpImage(RenderedImage source, ImageLayout layout, Map configuration,
            BorderExtender extender, Interpolation interp, float scaleX, float scaleY,
            float transX, float transY, boolean useRoiAccessor, Range nodata, double[] backgroundValues) {

        super(source, layout, configuration, true, extender, interp, scaleX, scaleY, transX,
                transY, useRoiAccessor, backgroundValues);     
        scaleOpInitialization(source,interp, nodata, backgroundValues);
    }
    
    private void scaleOpInitialization(RenderedImage source, Interpolation interp, Range nodata, double[] backgroundValues) {
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
        double[] destNod = null;
        if (backgroundValues != null && backgroundValues.length > 0){
        	destNod = backgroundValues;
		}
        if (interpolator instanceof InterpolationNearest) {
            isNearestNew=true;
            interpN = (InterpolationNearest) interpolator;
            this.interp=interpN;
            interpN.setROIBounds(roiBounds);
            if(destNod == null){
            	destNod = new double[]{interpN.getDestinationNoData()};
            }
        } else if (interpolator instanceof InterpolationBilinear) {
            isBilinearNew=true;
            interpB = (InterpolationBilinear) interpolator;
            this.interp=interpB;
            interpB.setROIBounds(roiBounds);
            if(destNod == null){
            	destNod = new double[]{interpB.getDestinationNoData()};
            }
        } else if (interpolator instanceof InterpolationBicubic) {
            isBicubicNew=true;
            interpBN = (InterpolationBicubic) interpolator;
            this.interp=interpBN;
            interpBN.setROIBounds(roiBounds);
            if(destNod == null){
            	destNod = new double[]{interpBN.getDestinationNoData()};
            }
        } else if (this.backgroundValues != null) {
        	destNod = this.backgroundValues;
        }

        if(destNod == null){
        	destNod = new double[]{0d};
        }

        this.destinationNoDataDouble = destNod;
        if(interpolator instanceof InterpolationNoData){
        	InterpolationNoData interpolationNoData = (InterpolationNoData)interpolator;
			interpolationNoData.setDestinationNoData(destNod[0]);
            if(nodata != null){
            	hasNoData = true;
            	interpolationNoData.setNoDataRange(nodata);
            }
            interpolationNoData.setUseROIAccessor(this.useRoiAccessor);
        }

        // subsample bits used for the bilinear and bicubic interpolation
        subsampleBits = interp.getSubsampleBitsH();

        // Internal precision required for position calculations
        one = 1 << subsampleBits;

        // Subsampling related variables
        shift2 = 2 * subsampleBits;
        round2 = 1 << (shift2 - 1);
        // Interpolation table used in the bicubic interpolation
        if (interpolator instanceof InterpolationTable) {
            InterpolationTable interpTable = (InterpolationTable) interpolator;
            precisionBits = interpTable.getPrecisionBits();
        }

        // Number of subsample positions
        one = 1 << subsampleBits;

        if (precisionBits > 0) {
            round = 1 << (precisionBits - 1);
        }

        // Get the width and height and padding of the Interpolation kernel.
        interp_width = interp.getWidth();
        interp_height = interp.getHeight();
        interp_left = interp.getLeftPadding();
        interp_top = interp.getTopPadding();

        SampleModel sm = source.getSampleModel();

        // Selection of the destination No Data
        switch (sm.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            destinationNoDataByte = new byte[1];
            destinationNoDataByte[0] = (byte) (((byte) destinationNoDataDouble[0]) & 0xff);
            break;
        case DataBuffer.TYPE_USHORT:
            destinationNoDataUShort = new short[1];
            destinationNoDataUShort[0] = (short) (((short) destinationNoDataDouble[0]) & 0xffff);
            break;
        case DataBuffer.TYPE_SHORT:
            destinationNoDataShort = new short[1];
            destinationNoDataShort[0] = (short) destinationNoDataDouble[0];
            break;
        case DataBuffer.TYPE_INT:
            destinationNoDataInt = new int[1];
            destinationNoDataInt[0] = (int) destinationNoDataDouble[0];
            break;
        case DataBuffer.TYPE_FLOAT:
            destinationNoDataFloat = new float[1];
            destinationNoDataFloat[0] = (float) destinationNoDataDouble[0];
            break;
        case DataBuffer.TYPE_DOUBLE:
            break;
        default:
            throw new IllegalArgumentException("Wrong data Type");
        }

        // Special case -- if the image is represented using
        // a MultiPixelPackedSampleModel and a byte, ushort,
        // or int DataBuffer we can access the pixel data directly.
        // Note that there is a potential loophole that has not been
        // resolved by Java2D as to whether the underlying DataBuffer
        // must be of one of the standard types. Here we make the
        // assumption that it will be -- we can't check without
        // forcing an actual tile to be computed.
        //
        isBinary = (sm instanceof MultiPixelPackedSampleModel)
                && (sm.getSampleSize(0) == 1)
                && (sm.getDataType() == DataBuffer.TYPE_BYTE
                        || sm.getDataType() == DataBuffer.TYPE_USHORT || sm.getDataType() == DataBuffer.TYPE_INT);

    }
    
    /** This method executes the scale operation on a selected region of the image */
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
        // Initialization of the x and y fractional position array
        Number xfracvalues[] = null, yfracvalues[] = null;

        // ROI support
        int[] yposRoi = null;
        // Scanline stride. It is used as integer because it can return null values
        Integer roiScanlineStride = null;
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

        // destination data type
        dataType = dest.getSampleModel().getDataType();
        // initialization of the x and y fractional values
        yfracvalues = new Number[dheight];
        xfracvalues = new Number[dwidth];
        // Private method for calculating the x and y positions, x and y fractional positions and y roi positions if present
        preComputePositions(destRect, srcRect.x, srcRect.y, srcPixelStride, srcScanlineStride,
                xpos, ypos, xfracvalues, yfracvalues, roiScanlineStride, yposRoi);
        // This methods differs only for the presence of the roi or if the image is a binary one
        if (isBinary) {
            computeLoopBynary(srcAccessor, source, dest, destRect, xpos, ypos,yposRoi, xfracvalues,
                    yfracvalues,roi,yposRoi,srcRect.x, srcRect.y, roiIter);
        } else {
            if (hasROI) {
                computeLoop(srcAccessor, destRect, dstAccessor, xpos, ypos, xfracvalues,
                        yfracvalues, roiAccessor, roiIter, yposRoi);
            } else {
                computeLoop(srcAccessor, destRect, dstAccessor, xpos, ypos, xfracvalues,
                        yfracvalues, null, null, null);
            }
        }

    }

    // This method precompute the integer and fractional position of every pixel
    private void preComputePositions(Rectangle destRect, int srcRectX, int srcRectY,
            int srcPixelStride, int srcScanlineStride, int xpos[], int ypos[],
            Number[] xfracvalues, Number[] yfracvalues, Integer roiScanlineStride, int[] yposRoi) {

        // Destination Rectangle position
        int dwidth = destRect.width;
        int dheight = destRect.height;

        // Loop variables based on the destination rectangle to be calculated.
        int dx = destRect.x;
        int dy = destRect.y;

        // Initially the y source value is calculated by the destination value and then performing the inverse
        // scale operation on it.
        long syNum = dy, syDenom = 1;

        // Subtract the X translation factor sy -= transY
        syNum = syNum * transYRationalDenom - transYRationalNum * syDenom;
        syDenom *= transYRationalDenom;

        // Add 0.5
        syNum = 2 * syNum + syDenom;
        syDenom *= 2;

        // Multply by invScaleX
        syNum *= invScaleYRationalNum;
        syDenom *= invScaleYRationalDenom;

        if(interpBN!=null|| interpB!=null
        		|| interpolator instanceof InterpolationBilinear 
        		|| interpolator instanceof InterpolationBicubic
        		|| interpolator instanceof InterpolationBicubic2){
            // Subtract 0.5
            syNum = 2 * syNum - syDenom;
            syDenom *= 2;
        }
        

        // Separate the y source coordinate into integer and fractional part
        int srcYInt = Rational.floor(syNum, syDenom);
        long srcYFrac = syNum % syDenom;
        if (srcYInt < 0) {
            srcYFrac = syDenom + srcYFrac;
        }

        // Normalize - Get a common denominator for the fracs of
        // src and invScaleY
        long commonYDenom = syDenom * invScaleYRationalDenom;
        srcYFrac *= invScaleYRationalDenom;
        long newInvScaleYFrac = invScaleYFrac * syDenom;

        // Initially the x source value is calculated by the destination value and then performing the inverse
        // scale operation on it.
        long sxNum = dx, sxDenom = 1;

        // Subtract the X translation factor sx -= transX
        sxNum = sxNum * transXRationalDenom - transXRationalNum * sxDenom;
        sxDenom *= transXRationalDenom;

        // Add 0.5
        sxNum = 2 * sxNum + sxDenom;
        sxDenom *= 2;

        // Multply by invScaleX
        sxNum *= invScaleXRationalNum;
        sxDenom *= invScaleXRationalDenom;

        
        
        if(interpBN!=null|| interpB!=null 
        		|| interpolator instanceof InterpolationBilinear 
        		|| interpolator instanceof InterpolationBicubic
        		|| interpolator instanceof InterpolationBicubic2){
        // Subtract 0.5
        sxNum = 2 * sxNum - sxDenom;
        sxDenom *= 2;
        }
        
        
        
        // Separate the x source coordinate into integer and fractional part
        int srcXInt = Rational.floor(sxNum, sxDenom);
        long srcXFrac = sxNum % sxDenom;
        if (srcXInt < 0) {
            srcXFrac = sxDenom + srcXFrac;            
        }

        // Normalize - Get a common denominator for the fracs of
        // src and invScaleX
        long commonXDenom = sxDenom * invScaleXRationalDenom;
        srcXFrac *= invScaleXRationalDenom;
        long newInvScaleXFrac = invScaleXFrac * sxDenom;

        // Store of the x positions
        for (int i = 0; i < dwidth; i++) {
            if(isBinary){
                xpos[i] = srcXInt;
            }else{
                xpos[i] = (srcXInt - srcRectX) * srcPixelStride;
            }            

            // Calculate the yfrac value
            if (dataType < DataBuffer.TYPE_FLOAT) {
                xfracvalues[i] = (int) (((1.0f *srcXFrac )/ commonXDenom) * one);
            } else {
                xfracvalues[i] = (1.0f * srcXFrac )/ commonXDenom;
            }
            // Move onto the next source pixel.

            // Add the integral part of invScaleX to the integral part
            // of srcX
            srcXInt += invScaleXInt;

            // Add the fractional part of invScaleX to the fractional part
            // of srcX
            srcXFrac += newInvScaleXFrac;

            // If the fractional part is now greater than equal to the
            // denominator, divide so as to reduce the numerator to be less
            // than the denominator and add the overflow to the integral part.
            if (srcXFrac >= commonXDenom) {
                srcXInt += 1;
                srcXFrac -= commonXDenom;
            }
        }
        // Store of the y positions
        for (int i = 0; i < dheight; i++) {

            // Calculate the source position in the source data array.
            if(isBinary){
                ypos[i] = srcYInt;
            }else{
                ypos[i] = (srcYInt - srcRectY) * srcScanlineStride;
            }
            
            // If roi is present, the y position roi value is calculated
            if (yposRoi != null) {
                if(isBinary){
                    yposRoi[i] = srcYInt;
                }else{
                    yposRoi[i] = (srcYInt - srcRectY) * roiScanlineStride;
                }                
            }

            // Calculate the yfrac value
            if (dataType < DataBuffer.TYPE_FLOAT) {
                yfracvalues[i] = (int) (((float) srcYFrac / (float) commonYDenom) * one);
            } else {
                yfracvalues[i] = (float) srcYFrac / (float) commonYDenom;
            }
            // Move onto the next source pixel.

            // Add the integral part of invScaleY to the integral part
            // of srcY
            srcYInt += invScaleYInt;

            // Add the fractional part of invScaleY to the fractional part
            // of srcY
            srcYFrac += newInvScaleYFrac;

            // If the fractional part is now greater than equal to the
            // denominator, divide so as to reduce the numerator to be less
            // than the denominator and add the overflow to the integral part.
            if (srcYFrac >= commonYDenom) {
                srcYInt += 1;
                srcYFrac -= commonYDenom;
            }
        }
    }

    // Method for calculating the destination pixels without using the roiAccessor
    private void computeLoop(RasterAccessor src, Rectangle dstRect, RasterAccessor dst, int[] xpos,
            int[] ypos, Number[] xfracvalues, Number[] yfracvalues, RasterAccessor roi, RandomIter roiIter,
            int[] yposRoi) {

        // Source PixelStride and ScanLineStride and bandOffsets
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
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
        // ONLY ONE OF THIS VALUES IS NOT NULL, THE OTHERS ARE ALL NULL
        byte[][] srcDataArraysByte = src.getByteDataArrays();
        short[][] srcDataArraysUshort = src.getShortDataArrays();
        short[][] srcDataArraysShort = src.getShortDataArrays();
        int[][] srcDataArraysInt = src.getIntDataArrays();
        float[][] srcDataArraysFloat = src.getFloatDataArrays();
        double[][] srcDataArraysDouble = src.getDoubleDataArrays();

        byte[][] dstDataArraysByte = dst.getByteDataArrays();
        short[][] dstDataArraysUshort = dst.getShortDataArrays();
        short[][] dstDataArraysShort = dst.getShortDataArrays();
        int[][] dstDataArraysInt = dst.getIntDataArrays();
        float[][] dstDataArraysFloat = dst.getFloatDataArrays();
        double[][] dstDataArraysDouble = dst.getDoubleDataArrays();

        // Destination and source data arrays (for a single band)
        byte[] dstDataByte = null;
        short[] dstDataUshort = null;
        short[] dstDataShort = null;
        int[] dstDataInt = null;
        float[] dstDataFloat = null;
        double[] dstDataDouble = null;

        byte[] srcDataByte = null;
        short[] srcDataUshort = null;
        short[] srcDataShort = null;
        int[] srcDataInt = null;
        float[] srcDataFloat = null;
        double[] srcDataDouble = null;

        // Array of samples required for the interpolation (Used only for general interpolators)
        int[][] samples = null;
        float[][] samplesf = null;
        double[][] samplesd = null;

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_INT:
            samples = new int[interp_height][interp_width];
            break;
        case DataBuffer.TYPE_FLOAT:
            samplesf = new float[interp_height][interp_width];
            break;
        case DataBuffer.TYPE_DOUBLE:
            samplesd = new double[interp_height][interp_width];
            break;
        default:
            break;
        }
        // Destination pixel returned
        Number s = null;
        // for all bands
        for (int k = 0; k < dnumBands; k++) {

            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                srcDataByte = srcDataArraysByte[k];
                dstDataByte = dstDataArraysByte[k];
                break;
            case DataBuffer.TYPE_USHORT:
                srcDataUshort = srcDataArraysUshort[k];
                dstDataUshort = dstDataArraysUshort[k];
                break;
            case DataBuffer.TYPE_SHORT:
                srcDataShort = srcDataArraysShort[k];
                dstDataShort = dstDataArraysShort[k];
                break;
            case DataBuffer.TYPE_INT:
                srcDataInt = srcDataArraysInt[k];
                dstDataInt = dstDataArraysInt[k];
                break;
            case DataBuffer.TYPE_FLOAT:
                srcDataFloat = srcDataArraysFloat[k];
                dstDataFloat = dstDataArraysFloat[k];
                break;
            case DataBuffer.TYPE_DOUBLE:
                srcDataDouble = srcDataArraysDouble[k];
                dstDataDouble = dstDataArraysDouble[k];
                break;
            default:
                break;
            }
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
                Integer posyROI = null;
                // if roi accessor is used, roi position is calculated
                if (yposRoi != null && roi != null) {
                    posyROI = yposRoi[j];
                }
                // cycle on the x values
                for (int i = 0; i < dwidth; i++) {
                    // x position selection
                    int posx = xpos[i];
                    // fractional values selection
                    Number[] fracValues = { xfracvalues[i], yfracvalues[j] };

                    if (interpBN != null) {
                        // Bicubic/Bicubic2 interpolation(must be set at the interpolator creation)
                        s = interpBN.interpolate(src, k, dnumBands, posx, posy, fracValues, posyROI, roi, roiIter, false);
                    } else if (interpB != null) {
                        // Bilinear interpolation
                        s = interpB.interpolate(src, k, dnumBands, posx, posy, fracValues, posyROI, roi, roiIter, false);
                    } else if (interpN != null) {
                        // Nearest-Neighbor interpolation
                        s = interpN.interpolate(src, k, dnumBands, posx, posy, posyROI, roi, roiIter, false);
                    } else if (interpolator != null) {

                        // GENERAL CASE WITH INTERPOLATORS DIFFERENT FROM THE ABOVE ONES
                        int start = interp_left * srcPixelStride + interp_top * srcScanlineStride;
                        start = posx + posy - start;
                        int countH = 0, countV = 0;
                        // loop on all the kernel pixels for saving the pixel values
                        for (int yloop = 0; yloop < interp_height; yloop++) {

                            int startY = start;

                            for (int xloop = 0; xloop < interp_width; xloop++) {
                                switch (dataType) {
                                case DataBuffer.TYPE_BYTE:
                                    samples[countV][countH++] = srcDataByte[start] & 0xff;
                                    break;
                                case DataBuffer.TYPE_USHORT:
                                    samples[countV][countH++] = srcDataUshort[start] & 0xffff;
                                    break;
                                case DataBuffer.TYPE_SHORT:
                                    samples[countV][countH++] = srcDataShort[start];
                                    break;
                                case DataBuffer.TYPE_INT:
                                    samples[countV][countH++] = srcDataInt[start];
                                    break;
                                case DataBuffer.TYPE_FLOAT:
                                    samplesf[countV][countH++] = srcDataFloat[start];
                                    break;
                                case DataBuffer.TYPE_DOUBLE:
                                    samplesd[countV][countH++] = srcDataDouble[start];
                                    break;
                                default:
                                    break;
                                }
                                start += srcPixelStride;
                            }
                            countV++;
                            countH = 0;
                            start = startY + srcScanlineStride;
                        }

                        // Perform the interpolation
                        switch (dataType) {
                        case DataBuffer.TYPE_BYTE:
                        case DataBuffer.TYPE_USHORT:
                        case DataBuffer.TYPE_SHORT:
                        case DataBuffer.TYPE_INT:
                            s = interp.interpolate(samples, fracValues[0].intValue(),
                                    fracValues[1].intValue());
                            break;
                        case DataBuffer.TYPE_FLOAT:
                            s = interp.interpolate(samplesf, fracValues[0].floatValue(),
                                    fracValues[1].floatValue());
                            break;
                        case DataBuffer.TYPE_DOUBLE:
                            s = interp.interpolate(samplesd, fracValues[0].floatValue(),
                                    fracValues[1].floatValue());
                            break;
                        default:
                            break;
                        }
                    } else {
                        throw new UnsupportedOperationException(
                                "Scale operation cannot be performed without an interpolator");
                    }
                    // The interpolated value is saved in the destination array
                    switch (dataType) {
                    case DataBuffer.TYPE_BYTE:
                        dstDataByte[dstPixelOffset] = (byte) (s.byteValue() & 0xff);
                        break;
                    case DataBuffer.TYPE_USHORT:
                        dstDataUshort[dstPixelOffset] = (short) (s.shortValue() & 0xffff);
                        break;
                    case DataBuffer.TYPE_SHORT:
                        dstDataShort[dstPixelOffset] = s.shortValue();
                        break;
                    case DataBuffer.TYPE_INT:
                        dstDataInt[dstPixelOffset] = s.intValue();
                        break;
                    case DataBuffer.TYPE_FLOAT:
                        dstDataFloat[dstPixelOffset] = s.floatValue();
                        break;
                    case DataBuffer.TYPE_DOUBLE:
                        dstDataDouble[dstPixelOffset] = s.doubleValue();
                        break;
                    default:
                        break;
                    }
                    // destination pixel offset update
                    dstPixelOffset += dstPixelStride;
                }
                // destination line offset update
                dstlineOffset += dstScanlineStride;

            }
        }

    }

    private void computeLoopBynary(RasterAccessor src, Raster source, WritableRaster dest,
            Rectangle destRect, int xvalues[], int yvalues[], int yvaluesROI[],Number[] xfracvalues,
            Number[] yfracvalues, Raster roi, int[] posYROI,int srcRectX,int srcRectY, RandomIter roiIter) {

        int dx = destRect.x;
        int dy = destRect.y;
        int dwidth = destRect.width;
        int dheight = destRect.height;

        MultiPixelPackedSampleModel sourceSM = (MultiPixelPackedSampleModel) source
                .getSampleModel();

        int sourceTransX = source.getSampleModelTranslateX();
        int sourceTransY = source.getSampleModelTranslateY();
        int sourceDataBitOffset = sourceSM.getDataBitOffset();
        int sourceScanlineStride = sourceSM.getScanlineStride();
        int sourcePixelStride = sourceSM.getPixelBitStride();

        
        MultiPixelPackedSampleModel roiSM=null;
        //int roiTransX =0;
        int roiTransY =0;
        //int roiDataBitOffset =0;
        int roiScanlineStride =0;
        DataBuffer roiDB =null;
        
        if(roi!=null){
            roiSM = (MultiPixelPackedSampleModel) roi.getSampleModel();

            //roiTransX = roi.getSampleModelTranslateX();
            roiTransY = roi.getSampleModelTranslateY();
            //roiDataBitOffset = roiSM.getDataBitOffset();
            roiScanlineStride = roiSM.getScanlineStride();
            
            roiDB = roi.getDataBuffer();
        }

        MultiPixelPackedSampleModel destSM = (MultiPixelPackedSampleModel) dest.getSampleModel();

        int destTransX = dest.getSampleModelTranslateX();
        int destTransY = dest.getSampleModelTranslateY();
        int destDataBitOffset = destSM.getDataBitOffset();
        int destScanlineStride = destSM.getScanlineStride();

        int[] sIntShortBytenum = new int[dwidth];
        int[] sshift = new int[dwidth];

        Number[] destData = null;
        Number[] sourceData = null;

        DataBuffer destDB = dest.getDataBuffer();
        DataBuffer sourceDB = source.getDataBuffer();


        byte[] sourceDataB = null;
        byte[] destDataB = null;

        short[] sourceDataS = null;
        short[] destDataS = null;

        int[] sourceDataI = null;
        int[] destDataI = null;
        
        int[] roiData = null;

        int bitshift = 0;
        int bitNum = 0;

        dataType=destSM.getDataType();
        
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            DataBufferByte destDBByte = (DataBufferByte) destDB;
            DataBufferByte sourceDBByte = (DataBufferByte) sourceDB;
            

            sourceDataB = sourceDBByte.getData();
            destDataB = destDBByte.getData();

            destData = new Number[destDataB.length];
            for (int ii = 0; ii < destDataB.length; ii++) {
                destData[ii] = destDataB[ii];
            }

            sourceData = new Number[sourceDataB.length];
            for (int ii = 0; ii < sourceDataB.length; ii++) {
                sourceData[ii] = sourceDataB[ii];
            }
            
            
            bitshift = 3;
            bitNum = 7;

            break;
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            DataBufferUShort destDBShort = (DataBufferUShort) destDB;
            DataBufferUShort sourceDBShort = (DataBufferUShort) sourceDB;
            
            
            sourceDataS = sourceDBShort.getData();
            destDataS = destDBShort.getData();

            destData = new Number[destDataS.length];
            for (int ii = 0; ii < destDataS.length; ii++) {
                destData[ii] = destDataS[ii];
            }

            sourceData = new Number[sourceDataS.length];
            for (int ii = 0; ii < sourceDataS.length; ii++) {
                sourceData[ii] = sourceDataS[ii];
            }


            
            bitshift = 4;
            bitNum = 15;

            break;
        case DataBuffer.TYPE_INT:
            DataBufferInt destDBInt = (DataBufferInt) destDB;
            DataBufferInt sourceDBInt = (DataBufferInt) sourceDB;
            

            sourceDataI = sourceDBInt.getData();
            destDataI = destDBInt.getData();

            destData = new Number[destDataI.length];
            for (int ii = 0; ii < destDataI.length; ii++) {
                destData[ii] = destDataI[ii];
            }

            sourceData = new Number[sourceDataI.length];
            for (int ii = 0; ii < sourceDataI.length; ii++) {
                sourceData[ii] = sourceDataI[ii];
            }

            bitshift = 5;
            bitNum = 31;

            break;
        }

        
        if(roi!=null){
        DataBufferByte roiDBByte=(DataBufferByte)roiDB;
        byte[] roiDataB=roiDBByte.getData();
        roiData =  new int[roiDataB.length];
        for (int ii = 0; ii < roiDataB.length; ii++) {
            roiData[ii] = roiDataB[ii];
        }
        }
        
        int sourceDBOffset = sourceDB.getOffset();
        int roiDBOffset=0;
        int destDBOffset = destDB.getOffset();

        if(roi!=null){
            roiDBOffset= roiDB.getOffset();
        }
        
        
        for (int i = 0; i < dwidth; i++) {
            int x = xvalues[i];
            int sbitnum = sourceDataBitOffset + (x - sourceTransX);
            sIntShortBytenum[i] = sbitnum >> bitshift;
            sshift[i] = bitNum - (sbitnum & bitNum);

        }

        int sourceYOffset;

        int s;

        int x, y;
        int yfrac, xfrac;

        int xNextBitNo;

        int destYOffset = (dy - destTransY) * destScanlineStride + destDBOffset;

        int dbitnum = destDataBitOffset + (dx - destTransX);

        int destByteShortIntNum;
        int destBitShift;

        int[] coordinates = new int[2];

        for (int j = 0; j < dheight; j++) {

            y = yvalues[j];
            yfrac = yfracvalues[j].intValue();
            sourceYOffset = (y - sourceTransY) * sourceScanlineStride + sourceDBOffset;
            dbitnum = destDataBitOffset + (dx - destTransX);
            
            int yROI =0;           
            int roiYOffset = 0;
            
            if(roi!=null){
                yROI = posYROI[j]; 
                roiYOffset = (yROI - roiTransY) * roiScanlineStride + roiDBOffset;
            }
                for (int i = 0; i < dwidth; i++) {
                    xfrac = xfracvalues[i].intValue();

                    x = xvalues[i];
                    coordinates[0] = src.getX() + (x-srcRectX)*sourcePixelStride;
                    coordinates[1] = src.getY() + ((y-srcRectY)*sourceScanlineStride) / sourceScanlineStride;

                    xNextBitNo = sourceDataBitOffset + (x + 1 - sourceTransX);

                    if(interpN !=null){
                        s = interpN.interpolateBinary(xNextBitNo,sourceData,sourceYOffset, sourceScanlineStride, coordinates,roiData, roiYOffset, roiScanlineStride, roiIter);
                    }else if (interpB != null) {

                        s = interpB.interpolateBinary(xNextBitNo, sourceData, xfrac, yfrac,
                                sourceYOffset, sourceScanlineStride, coordinates,roiData, roiYOffset, roiScanlineStride, roiIter);

                    } else if (interpBN != null) {

                        s = interpBN.interpolateBinary(xNextBitNo, sourceData, xfrac, yfrac,
                                sourceYOffset, sourceScanlineStride, coordinates, roiData, roiYOffset, roiScanlineStride, roiIter); 
                    } else {
                        throw new UnsupportedOperationException(
                                "Binary interpolation not supported by interpolator different from"
                                        + "the ones that belong to InterpolationNearest2, InterpolationBilinear2 or InterpolationBicubic"
                                        + "class.");
                    }

                    destByteShortIntNum = dbitnum >> bitshift;
                    destBitShift = bitNum - (dbitnum & bitNum);

                    if (s == 1) {

                        switch (dataType) {
                        case DataBuffer.TYPE_BYTE:
                            destDataB[destYOffset + destByteShortIntNum] |= (0x01 << destBitShift);
                            break;
                        case DataBuffer.TYPE_USHORT:
                        case DataBuffer.TYPE_SHORT:
                            destDataS[destYOffset + destByteShortIntNum] |= (0x01 << destBitShift);
                            break;
                        case DataBuffer.TYPE_INT:
                            destDataI[destYOffset + destByteShortIntNum] |= (0x01 << destBitShift);
                            break;
                        }
                    } else {

                        switch (dataType) {
                        case DataBuffer.TYPE_BYTE:
                            destDataB[destYOffset + destByteShortIntNum] &= (0xff - (0x01 << destBitShift));
                            break;
                        case DataBuffer.TYPE_USHORT:
                        case DataBuffer.TYPE_SHORT:
                            destDataS[destYOffset + destByteShortIntNum] &= (0xffff - (0x01 << destBitShift));
                            break;
                        case DataBuffer.TYPE_INT:
                            destDataI[destYOffset + destByteShortIntNum] &= (0xffffffff - (0x01 << destBitShift));
                            break;
                        }
                    }
                    dbitnum++;
                }
                destYOffset += destScanlineStride;
            }
        }
//    }

}
