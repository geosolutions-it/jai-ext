/*
 * $RCSfile: ScaleBilinearOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:42 $
 * $State: Exp $
 */
package it.geosolutions.jaiext.roiaware.affine;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Map;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;

import com.sun.media.jai.util.Rational;

/**
 * An <code>OpImage</code> that performs bilinear interpolation scaling.
 *
 */
final class ScaleBilinearOpImage extends ScaleOpImage {

    /** The number of SubsampleBits */
    private int subsampleBits;

    /** Subsampling related variables */
    int one, shift2, round2;

    Rational half = new Rational(1, 2);
    long invScaleYInt, invScaleYFrac;
    long invScaleXInt, invScaleXFrac;

    /**
     * Constructs a ScaleBilinearOpImage from a RenderedImage source,
     * 
     * @param source a RenderedImage.
     * @param extender a BorderExtender, or null.
     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     * @param xScale scale factor along x axis.
     * @param yScale scale factor along y axis.
     * @param xTrans translation factor along x axis.
     * @param yTrans translation factor along y axis.
     * @param interp a Interpolation object to use for resampling.
     */
    public ScaleBilinearOpImage(RenderedImage source,
                                BorderExtender extender,
                                Map config,
                                ImageLayout layout,
                                float xScale,
                                float yScale,
                                float xTrans,
                                float yTrans,
                                Interpolation interp,
                                boolean useRoiAccessor) {
        super(source,
              layout,
              config,
              true,
              extender,
              interp,
              xScale,
              yScale,
              xTrans,
              yTrans, 
              useRoiAccessor);

	subsampleBits = interp.getSubsampleBitsH();

	// Number of subsampling positions
	one = 1 << subsampleBits;

	// Subsampling related variables
	shift2 = 2 * subsampleBits;
	round2 = 1 << (shift2 - 1);

	if (invScaleYRational.num > invScaleYRational.denom) {
	    invScaleYInt = invScaleYRational.num / invScaleYRational.denom;
	    invScaleYFrac = invScaleYRational.num % invScaleYRational.denom;
	} else {
	    invScaleYInt = 0;
	    invScaleYFrac = invScaleYRational.num;
	}

	if (invScaleXRational.num > invScaleXRational.denom) {
	    invScaleXInt = invScaleXRational.num / invScaleXRational.denom;
	    invScaleXFrac = invScaleXRational.num % invScaleXRational.denom;
	} else {
	    invScaleXInt = 0;
	    invScaleXFrac = invScaleXRational.num;
	}
	
    }
    
    /**
     * Performs scale operation on a specified rectangle. The sources are
     * cobbled.
     *
     * @param sources an array of source Rasters, guaranteed to provide all
     *                necessary source data for computing the output.
     * @param dest a WritableRaster tile containing the area to be computed.
     * @param destRect the rectangle within dest to be processed.
     */
    protected void computeRect(Raster [] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

	Raster source = sources[0];
	// Get the source rectangle
        Rectangle srcRect = source.getBounds();

        RasterAccessor srcAccessor = 
            new RasterAccessor(source, srcRect,
                               formatTags[0], getSourceImage(0).getColorModel());

        RasterAccessor dstAccessor = 
            new RasterAccessor(dest, destRect, formatTags[1], getColorModel());

	int dwidth = destRect.width;
	int dheight = destRect.height;
        int srcPixelStride = srcAccessor.getPixelStride();
	int srcScanlineStride = srcAccessor.getScanlineStride();

	int[] ypos = new int[dheight];
	int[] xpos = new int[dwidth];

	int   xfracvalues[] = null, yfracvalues[] = null;
	float xfracvaluesFloat[] = null, yfracvaluesFloat[] = null;

        switch (dstAccessor.getDataType()) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_INT:
	     yfracvalues = new int[dheight];
	     xfracvalues = new int[dwidth];
	     preComputePositionsInt(destRect, srcRect.x, srcRect.y,
			srcPixelStride, srcScanlineStride,
                xpos, ypos, xfracvalues, yfracvalues, -1, null);
	     break;

	case DataBuffer.TYPE_FLOAT:
	case DataBuffer.TYPE_DOUBLE:
	     yfracvaluesFloat = new float[dheight];
	     xfracvaluesFloat = new float[dwidth];
	     preComputePositionsFloat(destRect, srcRect.x, srcRect.y,
			srcPixelStride, srcScanlineStride,
			xpos, ypos, xfracvaluesFloat, yfracvaluesFloat);
             break;

        default:
            throw 
		new RuntimeException(JaiI18N.getString("OrderedDitherOpImage0"));
        }

        switch (dstAccessor.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(srcAccessor, destRect, dstAccessor,
			xpos, ypos, xfracvalues, yfracvalues);
            break;

        case DataBuffer.TYPE_SHORT:
            shortLoop(srcAccessor, destRect, dstAccessor,
			xpos, ypos, xfracvalues, yfracvalues);
            break;

        case DataBuffer.TYPE_USHORT:
            ushortLoop(srcAccessor, destRect, dstAccessor,
			xpos, ypos, xfracvalues, yfracvalues);
            break;

        case DataBuffer.TYPE_INT:
            intLoop(srcAccessor, destRect, dstAccessor,
			xpos, ypos, xfracvalues, yfracvalues);
            break;

	case DataBuffer.TYPE_FLOAT:
	    floatLoop(srcAccessor, destRect, dstAccessor,
			xpos, ypos, xfracvaluesFloat, yfracvaluesFloat);
            break;

	case DataBuffer.TYPE_DOUBLE:
	    doubleLoop(srcAccessor, destRect, dstAccessor,
			xpos, ypos, xfracvaluesFloat, yfracvaluesFloat);
            break;
        }

        // If the RasterAccessor object set up a temporary buffer for the 
        // op to write to, tell the RasterAccessor to write that data
        // to the raster no that we're done with it.
        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }
    
    /**
     * Performs scale operation on a specified rectangle. The sources are
     * cobbled.
     *
     * @param sources an array of source Rasters, guaranteed to provide all
     *                necessary source data for computing the output.
     * @param dest a WritableRaster tile containing the area to be computed.
     * @param destRect the rectangle within dest to be processed.
     */
    protected void computeRect(Raster [] sources,
                               WritableRaster dest,
                               Rectangle destRect, Raster[] rois) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

	Raster source = sources[0];
	Raster roi = rois[0];
	// Get the source rectangle
        Rectangle srcRect = source.getBounds();

        RasterAccessor srcAccessor = 
            new RasterAccessor(source, srcRect,
                               formatTags[0], getSourceImage(0).getColorModel());
        RasterAccessor roiAccessor = 
                new RasterAccessor(roi, srcRect, RasterAccessor.findCompatibleTags(new RenderedImage[]{srcROIImage}, srcROIImage)[0]
                		, srcROIImage.getColorModel());

        RasterAccessor dstAccessor = 
            new RasterAccessor(dest, destRect, formatTags[1], getColorModel());

	int dwidth = destRect.width;
	int dheight = destRect.height;
        int srcPixelStride = srcAccessor.getPixelStride();
	int srcScanlineStride = srcAccessor.getScanlineStride();
        int roiScanlineStride = roiAccessor.getScanlineStride();

	int[] ypos = new int[dheight];
        int[] yposRoi = new int[dheight];
	int[] xpos = new int[dwidth];

	int   xfracvalues[] = null, yfracvalues[] = null;
	float xfracvaluesFloat[] = null, yfracvaluesFloat[] = null;

        switch (dstAccessor.getDataType()) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_INT:
	     yfracvalues = new int[dheight];
	     xfracvalues = new int[dwidth];
	     preComputePositionsInt(destRect, srcRect.x, srcRect.y,
			srcPixelStride, srcScanlineStride,
                xpos, ypos, xfracvalues, yfracvalues, roiScanlineStride, yposRoi);
	     break;

	case DataBuffer.TYPE_FLOAT:
	case DataBuffer.TYPE_DOUBLE:
	     yfracvaluesFloat = new float[dheight];
	     xfracvaluesFloat = new float[dwidth];
	     preComputePositionsFloat(destRect, srcRect.x, srcRect.y,
			srcPixelStride, srcScanlineStride,
			xpos, ypos, xfracvaluesFloat, yfracvaluesFloat);
             break;

        default:
            throw 
		new RuntimeException(JaiI18N.getString("OrderedDitherOpImage0"));
        }

        switch (dstAccessor.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(srcAccessor, destRect, dstAccessor,
			xpos, ypos, xfracvalues, yfracvalues, roiAccessor, yposRoi);
            break;

        case DataBuffer.TYPE_SHORT:
            shortLoop(srcAccessor, destRect, dstAccessor,
			xpos, ypos, xfracvalues, yfracvalues);
            break;

        case DataBuffer.TYPE_USHORT:
            ushortLoop(srcAccessor, destRect, dstAccessor,
			xpos, ypos, xfracvalues, yfracvalues);
            break;

        case DataBuffer.TYPE_INT:
            intLoop(srcAccessor, destRect, dstAccessor,
			xpos, ypos, xfracvalues, yfracvalues);
            break;

	case DataBuffer.TYPE_FLOAT:
	    floatLoop(srcAccessor, destRect, dstAccessor,
			xpos, ypos, xfracvaluesFloat, yfracvaluesFloat);
            break;

	case DataBuffer.TYPE_DOUBLE:
	    doubleLoop(srcAccessor, destRect, dstAccessor,
			xpos, ypos, xfracvaluesFloat, yfracvaluesFloat);
            break;
        }

        // If the RasterAccessor object set up a temporary buffer for the 
        // op to write to, tell the RasterAccessor to write that data
        // to the raster no that we're done with it.
        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }

    private void preComputePositionsInt(Rectangle destRect, 
			int srcRectX, int srcRectY,
			int srcPixelStride, int srcScanlineStride,
			int xpos[], int ypos[],
			int xfracvalues[], int yfracvalues[],
        int roiScanlineStride, int[] yposRoi) {

	int dwidth = destRect.width;
	int dheight = destRect.height;

	// Loop variables based on the destination rectangle to be calculated.
	int dx = destRect.x;
	int dy = destRect.y;

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

	// Subtract 0.5
	syNum = 2 * syNum - syDenom;
	syDenom *= 2;

	// Separate the x source coordinate into integer and fractional part
	int srcYInt = Rational.floor(syNum , syDenom);
	long srcYFrac = syNum % syDenom;
	if (srcYInt < 0) {
	    srcYFrac = syDenom + srcYFrac;
	}

	// Normalize - Get a common denominator for the fracs of 
	// src and invScaleY
	long commonYDenom = syDenom * invScaleYRationalDenom;
	srcYFrac *= invScaleYRationalDenom;
	long newInvScaleYFrac = invScaleYFrac * syDenom;

	// Precalculate the x positions and store them in an array.
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

	// Subtract 0.5
	sxNum = 2 * sxNum - sxDenom;
	sxDenom *= 2;

	// Separate the x source coordinate into integer and fractional part
	int srcXInt = Rational.floor(sxNum , sxDenom);
	long srcXFrac = sxNum % sxDenom;
	if (srcXInt < 0) {
	    srcXFrac = sxDenom + srcXFrac;
	}

	// Normalize - Get a common denominator for the fracs of 
	// src and invScaleX
	long commonXDenom = sxDenom * invScaleXRationalDenom;
	srcXFrac *= invScaleXRationalDenom;
	long newInvScaleXFrac = invScaleXFrac * sxDenom;

	for (int i=0; i<dwidth; i++) {
	    xpos[i] = (srcXInt - srcRectX) * srcPixelStride; 
	    xfracvalues[i] = (int)(((float)srcXFrac/(float)commonXDenom) * one);

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

	for (int i = 0; i < dheight; i++) {

	    // Calculate the source position in the source data array.
	    ypos[i] = (srcYInt - srcRectY) * srcScanlineStride; 
            if (yposRoi != null) {
                yposRoi[i] = (srcYInt - srcRectY) * roiScanlineStride;
            }

	    // Calculate the yfrac value
	    yfracvalues[i] = (int)(((float)srcYFrac/(float)commonYDenom) * one);

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

    private void preComputePositionsFloat(Rectangle destRect, 
			int srcRectX, int srcRectY,
			int srcPixelStride, int srcScanlineStride,
			int xpos[], int ypos[],
			float xfracvaluesFloat[], float yfracvaluesFloat[]) {

	int dwidth = destRect.width;
	int dheight = destRect.height;

	// Loop variables based on the destination rectangle to be calculated.
	int dx = destRect.x;
	int dy = destRect.y;

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

	// Subtract 0.5
	syNum = 2 * syNum - syDenom;
	syDenom *= 2;

	// Separate the x source coordinate into integer and fractional part
	int srcYInt = Rational.floor(syNum , syDenom);
	long srcYFrac = syNum % syDenom;
	if (srcYInt < 0) {
	    srcYFrac = syDenom + srcYFrac;
	}

	// Normalize - Get a common denominator for the fracs of 
	// src and invScaleY
	long commonYDenom = syDenom * invScaleYRationalDenom;
	srcYFrac *= invScaleYRationalDenom;
	long newInvScaleYFrac = invScaleYFrac * syDenom;

	// Precalculate the x positions and store them in an array.
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

	// Subtract 0.5
	sxNum = 2 * sxNum - sxDenom;
	sxDenom *= 2;

	// Separate the x source coordinate into integer and fractional part
	int srcXInt = Rational.floor(sxNum , sxDenom);
	long srcXFrac = sxNum % sxDenom;
	if (srcXInt < 0) {
	    srcXFrac = sxDenom + srcXFrac;
	}

	// Normalize - Get a common denominator for the fracs of 
	// src and invScaleX
	long commonXDenom = sxDenom * invScaleXRationalDenom;
	srcXFrac *= invScaleXRationalDenom;
	long newInvScaleXFrac = invScaleXFrac * sxDenom;

	for (int i=0; i<dwidth; i++) {

	    xpos[i] = (srcXInt - srcRectX) * srcPixelStride; 
	    xfracvaluesFloat[i] = (float)srcXFrac/(float)commonXDenom;

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

	for (int i = 0; i < dheight; i++) {

	    // Calculate the source position in the source data array.
	    ypos[i] = (srcYInt - srcRectY) * srcScanlineStride; 

	    // Calculate the yfrac value
	    yfracvaluesFloat[i] = (float)srcYFrac/(float)commonYDenom;

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

    private void byteLoop(RasterAccessor src, Rectangle dstRect,
			  RasterAccessor dst, int xpos[], int ypos[],
			  int xfracvalues[], int yfracvalues[]) {
	
        int srcPixelStride = src.getPixelStride();
	int srcScanlineStride = src.getScanlineStride();
	int srcLastXDataPos   = (src.getWidth()-1) * srcPixelStride;

	int dwidth = dstRect.width;
	int dheight = dstRect.height;
        int dnumBands = dst.getNumBands();
        byte dstDataArrays[][] = dst.getByteDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
	
        byte srcDataArrays[][] = src.getByteDataArrays(); 
	int bandOffsets[] = src.getBandOffsets();

	int dstOffset = 0;

	/* Four surrounding pixels are needed for Bilinear interpolation.
	 * If the dest pixel to be calculated is at (dx, dy) then the
	 * actual source pixel (sx, sy) required is (dx/scaleX, dy/scaleY).
	 * The four pixels that surround it are at the positions:
	 * s00 = src(sxlow, sylow)
	 * s01 = src(sxhigh, sylow)
	 * s10 = src(sxlow, syhigh)
	 * s11 = src(sxhigh, syhigh)
	 * where sxlow = Math.floor(sx), sxhigh = Math.ceil(sx)
	 * and   sylow = Math.floor(sy), syhigh = Math.ceil(sy)
	 *
	 * The value of the destination pixel can now be calculated as:
	 * s0 = (s01 - s00)*xfrac + s00;
	 * s1 = (s11 - s10)*xfrac + s10;
	 * dst(x,y) = (s1 - s0)*yfrac + s0;
	 */
	

	// Precalculate the y positions and store them in an array.
	// SG old loop with no ROI
	if(!hasROI){
        	// Putting band loop outside
        	for (int k = 0; k < dnumBands; k++)  {
                    byte dstData[] = dstDataArrays[k];
                    byte srcData[] = srcDataArrays[k];
                    int dstScanlineOffset = dstBandOffsets[k];
        	    int bandOffset = bandOffsets[k];
        	    for (int j = 0; j < dheight; j++) {
        
                        int dstPixelOffset = dstScanlineOffset;
                        int yfrac = yfracvalues[j];
        		int posylow = ypos[j] + bandOffset;
        		int posyhigh = posylow + srcScanlineStride;
        
        		for (int i = 0; i < dwidth; i++) {
        		    int xfrac = xfracvalues[i];
        		    int posxlow = xpos[i];
        		    int posxhigh = posxlow + srcPixelStride;
        
        		    // Get the four surrounding pixel values
        		    int s00 = srcData[posxlow + posylow] & 0xff;
        		    int s01 = srcData[posxhigh + posylow] & 0xff;
        		    int s10 = srcData[posxlow + posyhigh] & 0xff;
        		    int s11 = srcData[posxhigh + posyhigh] & 0xff;
        		    
        		    // Perform the bilinear interpolation
        		    int s0 = (s01 - s00) * xfrac + (s00 << subsampleBits);
        		    int s1 = (s11 - s10) * xfrac + (s10 << subsampleBits);
        		    int s = ((s1 - s0) * yfrac + (s0 << subsampleBits) + round2) >> shift2;
        		    
        		    dstData[dstPixelOffset] = (byte)(s&0xff);		    
        		    dstPixelOffset += dstPixelStride;
                        }
                        dstScanlineOffset += dstScanlineStride;
        
                    }
                }

        } else {
            // SG using ROI
            
            // Putting band loop outside
            // FIXME we should reorganize this to avoid using roiiterator too much
            for (int k = 0; k < dnumBands; k++)  {
                
                byte dstData[] = dstDataArrays[k];
                byte srcData[] = srcDataArrays[k];
                int dstScanlineOffset = dstBandOffsets[k];
                int bandOffset = bandOffsets[k];
                
                for (int j = 0; j < dheight; j++) {
    
                    int dstPixelOffset = dstScanlineOffset;
                    int yfrac = yfracvalues[j];
                    int posylow = ypos[j] + bandOffset;
                    int posyhigh = posylow + srcScanlineStride;
    
                    for (int i = 0; i < dwidth; i++) {
                        int xfrac = xfracvalues[i];
                        int posxlow = xpos[i];
                        int posxhigh = posxlow + srcPixelStride;
    

                        // SG mapping source rect
                        int x0=src.getX()+xpos[i]/srcPixelStride;
                        int y0=src.getY()+ypos[j]/srcScanlineStride;
                        if(roiBounds.contains(x0, y0)){
                            
                            final int w00=roiIter.getSample(x0, y0, 0);
                            final int w01=roiIter.getSample(x0+1, y0, 0);
                            final int w10=roiIter.getSample(x0, y0+1, 0);
                            final int w11=roiIter.getSample(x0+1, y0+1, 0);
                            
                            if(!(w00==0&&w01==0&&w10==0&&w11==0)){
                            
                                // Get the four surrounding pixel values
                                int s00 = srcData[posxlow + posylow] & 0xff;
                                int s01 = srcData[posxhigh + posylow] & 0xff;
                                int s10 = srcData[posxlow + posyhigh] & 0xff;
                                int s11 = srcData[posxhigh + posyhigh] & 0xff;
                                int s = computeValue(s00, s01, s10, s11, w00, w01, w10, w11, xfrac, yfrac);
                                
                                // assing value     
                                dstData[dstPixelOffset] = (byte)(s); 
                                
                            } else {
                                // FIXME bkg values
                            }

                        } else {
                            // FIXME bkg values
                        }
                        dstPixelOffset += dstPixelStride;
                    }
                    dstScanlineOffset += dstScanlineStride;
    
                }
            }
        }    	
    }
    
    
    
    private void byteLoop(RasterAccessor src, Rectangle dstRect,
			  RasterAccessor dst, int xpos[], int ypos[],
        int[] xfracvalues, int[] yfracvalues, RasterAccessor roi, int[] yposRoi) {
	
      int srcPixelStride = src.getPixelStride();
	int srcScanlineStride = src.getScanlineStride();
	int roiScalineStride = roi.getScanlineStride();

	int dwidth = dstRect.width;
	int dheight = dstRect.height;
      int dnumBands = dst.getNumBands();
      byte dstDataArrays[][] = dst.getByteDataArrays();
      byte roiDataArray[] = roi.getByteDataArray(0);
      final int roiDataLength = roiDataArray.length;
      int dstBandOffsets[] = dst.getBandOffsets();
      int dstPixelStride = dst.getPixelStride();
      int dstScanlineStride = dst.getScanlineStride();
	
      byte srcDataArrays[][] = src.getByteDataArrays(); 
	int bandOffsets[] = src.getBandOffsets();


	/* Four surrounding pixels are needed for Bilinear interpolation.
	 * If the dest pixel to be calculated is at (dx, dy) then the
	 * actual source pixel (sx, sy) required is (dx/scaleX, dy/scaleY).
	 * The four pixels that surround it are at the positions:
	 * s00 = src(sxlow, sylow)
	 * s01 = src(sxhigh, sylow)
	 * s10 = src(sxlow, syhigh)
	 * s11 = src(sxhigh, syhigh)
	 * where sxlow = Math.floor(sx), sxhigh = Math.ceil(sx)
	 * and   sylow = Math.floor(sy), syhigh = Math.ceil(sy)
	 *
	 * The value of the destination pixel can now be calculated as:
	 * s0 = (s01 - s00)*xfrac + s00;
	 * s1 = (s11 - s10)*xfrac + s10;
	 * dst(x,y) = (s1 - s0)*yfrac + s0;
	 */
	

	// Precalculate the y positions and store them in an array.
	// SG old loop with no ROI
	if(!hasROI){
      	// Putting band loop outside
      	for (int k = 0; k < dnumBands; k++)  {
                  byte dstData[] = dstDataArrays[k];
                  byte srcData[] = srcDataArrays[k];
                  int dstScanlineOffset = dstBandOffsets[k];
      	    int bandOffset = bandOffsets[k];
      	    for (int j = 0; j < dheight; j++) {
      
                      int dstPixelOffset = dstScanlineOffset;
                      int yfrac = yfracvalues[j];
      		int posylow = ypos[j] + bandOffset;
      		int posyhigh = posylow + srcScanlineStride;
      
      		for (int i = 0; i < dwidth; i++) {
      		    int xfrac = xfracvalues[i];
      		    int posxlow = xpos[i];
      		    int posxhigh = posxlow + srcPixelStride;
      
      		    // Get the four surrounding pixel values
      		    int s00 = srcData[posxlow + posylow] & 0xff;
      		    int s01 = srcData[posxhigh + posylow] & 0xff;
      		    int s10 = srcData[posxlow + posyhigh] & 0xff;
      		    int s11 = srcData[posxhigh + posyhigh] & 0xff;
      		    int s0 = (s01 - s00) * xfrac + (s00 << subsampleBits);
	      		int s1 = (s11 - s10) * xfrac + (s10 << subsampleBits);
	      		int s = ((s1 - s0) * yfrac + (s0 << subsampleBits) + round2) >> shift2;
      		    
      		    dstData[dstPixelOffset] = (byte)(s&0xff);		    
      		    dstPixelOffset += dstPixelStride;
                      }
                      dstScanlineOffset += dstScanlineStride;
      
                  }
              }
	} else if(useRoiAccessor){
		int w00index = 0;
		int w01index = 0;
		int w10index = 0;
		int w11index = 0;
		int w00 = 0;
		int w01 = 0;
		int w10 = 0;
		int w11 = 0;
		int s00 = 0;
		int s01 = 0;
		int s10 = 0;
		int s11 = 0;
      	// Putting band loop outside
      	for (int k = 0; k < dnumBands; k++)  {
                  byte dstData[] = dstDataArrays[k];
                  byte srcData[] = srcDataArrays[k];
                  int dstScanlineOffset = dstBandOffsets[k];
      	    int bandOffset = bandOffsets[k];
      	    for (int j = 0; j < dheight; j++) {
      
                      int dstPixelOffset = dstScanlineOffset;
                      int yfrac = yfracvalues[j];
      		int posylow = ypos[j] + bandOffset;
      		int posyhigh = posylow + srcScanlineStride;
      
      		for (int i = 0; i < dwidth; i++) {
      		    int xfrac = xfracvalues[i];
      		    int posxlow = xpos[i];
      		    int posxhigh = posxlow + srcPixelStride;
      
      		    // Get the four surrounding pixel values
      		    s00 = srcData[posxlow + posylow] & 0xff;
      		    s01 = srcData[posxhigh + posylow] & 0xff;
      		    s10 = srcData[posxlow + posyhigh] & 0xff;
      		    s11 = srcData[posxhigh + posyhigh] & 0xff;
                        w00index = (posxlow / dnumBands) + (yposRoi[j]);
                        w01index = (posxlow / dnumBands) + 1 + (yposRoi[j]);
                        w10index = (posxlow / dnumBands) + (yposRoi[j]) + roiScalineStride;
                        w11index = (posxlow / dnumBands) + 1 + (yposRoi[j]) + roiScalineStride;
      		    w00 = w00index < roiDataLength ? roiDataArray[w00index] & 0xff : 0;
      		    w01 = w01index < roiDataLength ? roiDataArray[w01index] & 0xff : 0;
      		    w10 = w10index < roiDataLength ? roiDataArray[w10index] & 0xff : 0;
      		    w11 = w11index < roiDataLength ? roiDataArray[w11index] & 0xff : 0;
    		    if (w00 == 0 && w01 == 0 && w10 == 0 && w11 == 0){
    		    	dstPixelOffset += dstPixelStride;
    		    	continue;
    		    }
      		    int s = computeValue(s00, s01, s10, s11, w00, w01, w10, w11, xfrac, yfrac);
      		    
      		    dstData[dstPixelOffset] = (byte)(s&0xff);		    
      		    dstPixelOffset += dstPixelStride;
                      }
                      dstScanlineOffset += dstScanlineStride;
      
                  }
              }

      } else {
          // SG using ROI
          
          // Putting band loop outside
          // FIXME we should reorganize this to avoid using roiiterator too much
          for (int k = 0; k < dnumBands; k++)  {
              
              byte dstData[] = dstDataArrays[k];
              byte srcData[] = srcDataArrays[k];
              int dstScanlineOffset = dstBandOffsets[k];
              int bandOffset = bandOffsets[k];
              
              for (int j = 0; j < dheight; j++) {
  
                  int dstPixelOffset = dstScanlineOffset;
                  int yfrac = yfracvalues[j];
                  int posylow = ypos[j] + bandOffset;
                  int posyhigh = posylow + srcScanlineStride;
  
                  for (int i = 0; i < dwidth; i++) {
                      int xfrac = xfracvalues[i];
                      int posxlow = xpos[i];
                      int posxhigh = posxlow + srcPixelStride;
  

                      // SG mapping source rect
                      int x0=src.getX()+xpos[i]/srcPixelStride;
                      int y0=src.getY()+ypos[j]/srcScanlineStride;
                      if(roiBounds.contains(x0, y0)){
                          
                          final int w00=roiIter.getSample(x0, y0, 0);
                          final int w01=roiIter.getSample(x0+1, y0, 0);
                          final int w10=roiIter.getSample(x0, y0+1, 0);
                          final int w11=roiIter.getSample(x0+1, y0+1, 0);
                          
                          if(!(w00==0&&w01==0&&w10==0&&w11==0)){
                          
                              // Get the four surrounding pixel values
                              int s00 = srcData[posxlow + posylow] & 0xff;
                              int s01 = srcData[posxhigh + posylow] & 0xff;
                              int s10 = srcData[posxlow + posyhigh] & 0xff;
                              int s11 = srcData[posxhigh + posyhigh] & 0xff;
                              int s = computeValue(s00, s01, s10, s11, w00, w01, w10, w11, xfrac, yfrac);
                              
                              // assing value     
                              dstData[dstPixelOffset] = (byte)(s); 
                              
                          } else {
                              // FIXME bkg values
                          }

                      } else {
                          // FIXME bkg values
                      }
                      dstPixelOffset += dstPixelStride;
                  }
                  dstScanlineOffset += dstScanlineStride;
  
              }
          }
      }    	
  }

    private int computeValue(int s00, int s01, int s10, int s11, int w00,
			int w01, int w10, int w11, int xfrac, int yfrac) {
    	int s0 = 0;
    	int s1 = 0;
    	int s = 0;
    	if (w00 == 0 || w01 == 0 || w10 == 0 || w11 == 0) {
	    	      		    	
		      if( w00 == 0 && w01 == 0) {
		          	s0 = 0;
		      } else if (w00 == 0) { //w01 = 1
		      		s0 = (s01 << subsampleBits);
		      } else if (w01 == 0) {//w00 = 1
		      		s0 = (s00 << subsampleBits);//s00;
		      } else {// w00 = 1 & W01 = 1
		      		s0 = (s01 - s00) * xfrac + (s00 << subsampleBits);
		      }
		              
		      // lower value
		      
		      if (w10 == 0 && w11 == 0) {
		          	s1 = 0;
		      } else if (w10 == 0) { // w11 = 1
		      		s1 = (s11 << subsampleBits);
		      } else if (w11 == 0) { // w10 = 1
		      		s1 = (s10 << subsampleBits);// - (s10 * xfrac); //s10;
		      } else {
		      		s1 = (s11 - s10) * xfrac + (s10 << subsampleBits);
		      }
		          
		      if (w00 == 0 && w01 == 0) {
		          	s = ((s1 << subsampleBits) + round2)>> shift2;
		      } else {
		          	if (w10 == 0 && w11 == 0) {
		          		s= ((s0 << subsampleBits) + round2) >> shift2;
		          	} else {
		          		s = ((s1 - s0) * yfrac + (s0 << subsampleBits) + round2) >> shift2;
		          	}
		      }
	    } else {
	    
		    // Perform the bilinear interpolation
		    s0 = (s01 - s00) * xfrac + (s00 << subsampleBits);
		    s1 = (s11 - s10) * xfrac + (s10 << subsampleBits);
		    s = ((s1 - s0) * yfrac + (s0 << subsampleBits) + round2) >> shift2;
	    }
      	return s;
	}

	private void shortLoop(RasterAccessor src, Rectangle dstRect,
			  RasterAccessor dst, int xpos[], int ypos[],
			  int xfracvalues[], int yfracvalues[]) {
		
        int srcPixelStride = src.getPixelStride();
	int srcScanlineStride = src.getScanlineStride();
	int srcLastXDataPos   = (src.getWidth()-1) * srcPixelStride;

	int dwidth = dstRect.width;
	int dheight = dstRect.height;
        int dnumBands = dst.getNumBands();
        short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
	
        short srcDataArrays[][] = src.getShortDataArrays(); 
	int bandOffsets[] = src.getBandOffsets();

	int dstOffset = 0;
	int posylow, posyhigh, posxlow, posxhigh;
	int s00, s01, s10, s11, s0, s1, s;
	int xfrac, yfrac;
		
	// Putting band loop outside
	for (int k = 0; k < dnumBands; k++)  {
            short dstData[] = dstDataArrays[k];
            short srcData[] = srcDataArrays[k];
            int dstScanlineOffset = dstBandOffsets[k];
	    int bandOffset = bandOffsets[k];
	    for (int j = 0; j < dheight; j++) {
                int dstPixelOffset = dstScanlineOffset;
		yfrac = yfracvalues[j];
		posylow = ypos[j] + bandOffset;
		posyhigh = posylow + srcScanlineStride;

		for (int i = 0; i < dwidth; i++) {
		    xfrac = xfracvalues[i];
		    posxlow = xpos[i];
		    posxhigh = posxlow + srcPixelStride;

		    // Get the four surrounding pixel values
		    s00 = srcData[posxlow + posylow];
		    s01 = srcData[posxhigh + posylow];
		    s10 = srcData[posxlow + posyhigh];
		    s11 = srcData[posxhigh + posyhigh];
		    
		    // Perform the bilinear interpolation
		    s0 = (s01 - s00) * xfrac + (s00 << subsampleBits);
		    s1 = (s11 - s10) * xfrac + (s10 << subsampleBits);
		    s = ((s1 - s0) * yfrac + (s0 << subsampleBits) +
			 round2) >> shift2;
		    
		    dstData[dstPixelOffset] = (short)s;
                    dstPixelOffset += dstPixelStride;
                }
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void ushortLoop(RasterAccessor src, Rectangle dstRect,
			  RasterAccessor dst, int xpos[], int ypos[],
			  int xfracvalues[], int yfracvalues[]) {
	
        int srcPixelStride = src.getPixelStride();
	int srcScanlineStride = src.getScanlineStride();
	int srcLastXDataPos   = (src.getWidth()-1) * srcPixelStride;

	int dwidth = dstRect.width;
	int dheight = dstRect.height;
        int dnumBands = dst.getNumBands();
        short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
	
        short srcDataArrays[][] = src.getShortDataArrays(); 	
	int bandOffsets[] = src.getBandOffsets();

	int dstOffset = 0;
	int posylow, posyhigh, posxlow, posxhigh;
	int s00, s01, s10, s11, s0, s1, s;
	int xfrac, yfrac;
		
	// Putting band loop outside
	for (int k = 0; k < dnumBands; k++)  {
            short dstData[] = dstDataArrays[k];
            short srcData[] = srcDataArrays[k];
            int dstScanlineOffset = dstBandOffsets[k];
	    int bandOffset = bandOffsets[k];
	    for (int j = 0; j < dheight; j++) {
                int dstPixelOffset = dstScanlineOffset;
		yfrac = yfracvalues[j];
		posylow = ypos[j] + bandOffset;
		posyhigh = posylow + srcScanlineStride;

		for (int i = 0; i < dwidth; i++) {
		    xfrac = xfracvalues[i];
		    posxlow = xpos[i];
		    posxhigh = posxlow + srcPixelStride;

		    // Get the four surrounding pixel values
		    s00 = srcData[posxlow + posylow] & 0xffff;
		    s01 = srcData[posxhigh + posylow] & 0xffff;
		    s10 = srcData[posxlow + posyhigh] & 0xffff;
		    s11 = srcData[posxhigh + posyhigh] & 0xffff;
		    
		    // Perform the bilinear interpolation
		    s0 = (s01 - s00) * xfrac + (s00 << subsampleBits);
		    s1 = (s11 - s10) * xfrac + (s10 << subsampleBits);
		    s = ((s1 - s0) * yfrac + (s0 << subsampleBits) +
			 round2) >> shift2;
		    
		    dstData[dstPixelOffset] = (short)(s & 0xffff);
                    dstPixelOffset += dstPixelStride;
                }
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }
        
    // identical to byteLoops, except datatypes have changed.  clumsy,
    // but there's no other way in Java
    private void intLoop(RasterAccessor src, Rectangle dstRect,
			  RasterAccessor dst, int xpos[], int ypos[],
			  int xfracvalues[], int yfracvalues[]) {

        int srcPixelStride = src.getPixelStride();
	int srcScanlineStride = src.getScanlineStride();
	int srcLastXDataPos   = (src.getWidth()-1) * srcPixelStride;

	int dwidth = dstRect.width;
	int dheight = dstRect.height;
        int dnumBands = dst.getNumBands();
        int dstDataArrays[][] = dst.getIntDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
	
        int srcDataArrays[][] = src.getIntDataArrays(); 
	int bandOffsets[] = src.getBandOffsets();

	int dstOffset = 0;
	int posylow, posyhigh, posxlow, posxhigh;
	int s00, s10, s01, s11;
	long s0, s1;
	int xfrac, yfrac;
	int shift = 29 - subsampleBits;

	// Putting band loop outside
	for (int k = 0; k < dnumBands; k++)  {
            int dstData[] = dstDataArrays[k];
            int srcData[] = srcDataArrays[k];
            int dstScanlineOffset = dstBandOffsets[k];
	    int bandOffset = bandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int dstPixelOffset = dstScanlineOffset;
		yfrac = yfracvalues[j];
		posylow = ypos[j] + bandOffset;
		posyhigh = posylow + srcScanlineStride;

                for (int i = 0; i < dwidth; i++)  {
		    xfrac = xfracvalues[i];
		    posxlow = xpos[i];
		    posxhigh = posxlow + srcPixelStride;

		    // Get the four surrounding pixel values
		    s00 = srcData[posxlow + posylow];
		    s01 = srcData[posxhigh + posylow];
		    s10 = srcData[posxlow + posyhigh];
		    s11 = srcData[posxhigh + posyhigh];

		    // Perform the bilinear interpolation
		    if ((s00 | s10) >>> shift == 0) {
			if ((s01 | s11) >>> shift == 0) {
			    s0 = (s01 - s00) * xfrac + (s00 << subsampleBits);
			    s1 = (s11 - s10) * xfrac + (s10 << subsampleBits);
			} else {
			    s0 = ((long)s01 - s00) * xfrac + (s00 << subsampleBits);
			    s1 = ((long)s11 - s10) * xfrac + (s10 << subsampleBits);
			}
		    } else {
			s0 = ((long)s01 - s00) * xfrac + ((long)s00 << subsampleBits);
			s1 = ((long)s11 - s10) * xfrac + ((long)s10 << subsampleBits);
		    }

		    dstData[dstPixelOffset] = (int)(((s1 - s0) * yfrac +
						     (s0 << subsampleBits) +
						     round2) >> shift2);

                    dstPixelOffset += dstPixelStride;
                }
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }   

    // Interpolation for floating point samples done as specified by the
    // following formula:
    //        float s0 = (s01 - s00)*xfrac + s00;
    //        float s1 = (s11 - s10)*xfrac + s10;
    //        return (s1 - s0)*yfrac + s0;
    // Note that xfrac, yfrac are in the range [0.0F, 1.0F)

    private void floatLoop(RasterAccessor src, Rectangle dstRect,
			  RasterAccessor dst, int xpos[], int ypos[],
			  float xfracvaluesFloat[], float yfracvaluesFloat[]) {
	
        int srcPixelStride = src.getPixelStride();
	int srcScanlineStride = src.getScanlineStride();
	int srcLastXDataPos   = (src.getWidth()-1) * srcPixelStride;

	int dwidth = dstRect.width;
	int dheight = dstRect.height;
        int dnumBands = dst.getNumBands();
        float dstDataArrays[][] = dst.getFloatDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
	
	float srcDataArrays[][] = src.getFloatDataArrays(); 
	int bandOffsets[] = src.getBandOffsets();

	float s00, s01, s10, s11;
	float s0, s1;
	float xfrac, yfrac;
	int dstOffset = 0;	
	int posylow, posyhigh, posxlow, posxhigh;

	// Putting band loop outside
	for (int k = 0; k < dnumBands; k++)  {
            float dstData[] = dstDataArrays[k];
            float srcData[] = srcDataArrays[k];
            int dstScanlineOffset = dstBandOffsets[k];
	    int bandOffset = bandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int dstPixelOffset = dstScanlineOffset;
		yfrac = yfracvaluesFloat[j];
		posylow = ypos[j] + bandOffset;
		posyhigh = posylow + srcScanlineStride;

                for (int i = 0; i < dwidth; i++)  {
		    xfrac = xfracvaluesFloat[i];
		    posxlow = xpos[i];
		    posxhigh = posxlow + srcPixelStride;

		    // Get the four surrounding pixel values
		    s00 = srcData[posxlow + posylow];
		    s01 = srcData[posxhigh + posylow];
		    s10 = srcData[posxlow + posyhigh];
		    s11 = srcData[posxhigh + posyhigh];

		    // Perform the bilinear interpolation
		    s0 = (s01 - s00) * xfrac + s00;
		    s1 = (s11 - s10) * xfrac + s10;
				    	    
		    dstData[dstPixelOffset] = (s1 - s0) * yfrac + s0;

                    dstPixelOffset += dstPixelStride;
                }
                dstScanlineOffset += dstScanlineStride;
            }
        }	
    }   

    private void doubleLoop(RasterAccessor src, Rectangle dstRect,
			  RasterAccessor dst, int xpos[], int ypos[],
			  float xfracvaluesFloat[], float yfracvaluesFloat[]) {

        int srcPixelStride = src.getPixelStride();
	int srcScanlineStride = src.getScanlineStride();
	int srcLastXDataPos   = (src.getWidth()-1) * srcPixelStride;

	int dwidth = dstRect.width;
	int dheight = dstRect.height;
        int dnumBands = dst.getNumBands();
        double dstDataArrays[][] = dst.getDoubleDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
	
	double srcDataArrays[][] = src.getDoubleDataArrays(); 
	int bandOffsets[] = src.getBandOffsets();

	double s00, s01, s10, s11;
	double s0, s1;
	double xfrac, yfrac;
	int dstOffset = 0;	
	int posylow, posyhigh, posxlow, posxhigh;

	// Putting band loop outside
	for (int k = 0; k < dnumBands; k++)  {
            double dstData[] = dstDataArrays[k];
            double srcData[] = srcDataArrays[k];
            int dstScanlineOffset = dstBandOffsets[k];
	    int bandOffset = bandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int dstPixelOffset = dstScanlineOffset;
		yfrac = yfracvaluesFloat[j];
		posylow = ypos[j] + bandOffset;
		posyhigh = posylow + srcScanlineStride;

                for (int i = 0; i < dwidth; i++)  {
		    xfrac = xfracvaluesFloat[i];
		    posxlow = xpos[i];
		    posxhigh = posxlow + srcPixelStride;

		    // Get the four surrounding pixel values
		    s00 = srcData[posxlow + posylow];
		    s01 = srcData[posxhigh + posylow];
		    s10 = srcData[posxlow + posyhigh];
		    s11 = srcData[posxhigh + posyhigh];

		    // Perform the bilinear interpolation
		    s0 = (s01 - s00) * xfrac + s00;
		    s1 = (s11 - s10) * xfrac + s10;
				    	    
		    dstData[dstPixelOffset] = (s1 - s0) * yfrac + s0;

                    dstPixelOffset += dstPixelStride;
                }
                dstScanlineOffset += dstScanlineStride;
            }
        }	
    }   

 
}
