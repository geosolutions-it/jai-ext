/*
 * $RCSfile: ScaleNearestOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:43 $
 * $State: Exp $
 */
package it.geosolutions.jaiext.roiaware.affine;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
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
 * An OpImage subclass that performs nearest-neighbor scaling.
 *
 */
final class ScaleNearestOpImage extends ScaleOpImage {

    long invScaleXInt, invScaleXFrac;
    long invScaleYInt, invScaleYFrac;

    /**
     * Constructs a ScaleNearestOpImage from a RenderedImage source,
     * 
     * @param source a RenderedImage.
     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     * @param xScale scale factor along x axis.
     * @param yScale scale factor along y axis.
     * @param xTrans translation factor along x axis.
     * @param yTrans translation factor along y axis.
     * @param interp an Interpolation object to use for resampling.
     */
    public ScaleNearestOpImage(RenderedImage source,
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

        // If the source has an IndexColorModel, override the default setting
        // in OpImage. The dest shall have exactly the same SampleModel and
        // ColorModel as the source.
        // Note, in this case, the source should have an integral data type.
        ColorModel srcColorModel = source.getColorModel();
        if (srcColorModel instanceof IndexColorModel) {
             sampleModel = source.getSampleModel().createCompatibleSampleModel(
                                                   tileWidth, tileHeight);
             colorModel = srcColorModel;
        }

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
    }

    /**
     * Performs a scale operation on a specified rectangle. The sources are
     * cobbled.
     *
     * @param sources  an array of source Rasters, guaranteed to provide all
     *                 necessary source data for computing the output.
     * @param dest     a WritableRaster  containing the area to be computed.
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

	int srcRectX = srcRect.x;
	int srcRectY = srcRect.y;

        RasterAccessor srcAccessor =
	    new RasterAccessor(source, srcRect, formatTags[0],
			       getSource(0).getColorModel());

        RasterAccessor dstAccessor = 
            new RasterAccessor(dest, destRect, formatTags[1], getColorModel());

        int srcScanlineStride = srcAccessor.getScanlineStride();
        int srcPixelStride = srcAccessor.getPixelStride();

	// Destination rectangle dimensions.
	int dx = destRect.x;
	int dy = destRect.y;
	int dwidth = destRect.width;
	int dheight = destRect.height;

	// Precalculate the x positions and store them in an array.
	int[] xvalues = new int[dwidth];

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

	// Separate the x source coordinate into integer and fractional part
	// int part is floor(sx), frac part is sx - floor(sx)
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

	for (int i = 0; i < dwidth; i++) {

	    // Calculate the position
	    xvalues[i] = (srcXInt - srcRectX) * srcPixelStride; 

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

	// Precalculate the y positions and store them in an array.       
	int[] yvalues = new int[dheight];

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

	for (int i = 0; i < dheight; i++) {

	    // Calculate the position
	    yvalues[i] = (srcYInt - srcRectY) * srcScanlineStride; 

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

        switch (dstAccessor.getDataType()) {

        case DataBuffer.TYPE_BYTE:
            byteLoop(srcAccessor, destRect, dstAccessor, xvalues, yvalues);
            break;

        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT:
            shortLoop(srcAccessor, destRect, dstAccessor, xvalues, yvalues);
            break;

        case DataBuffer.TYPE_INT:
            intLoop(srcAccessor, destRect, dstAccessor, xvalues, yvalues);
            break;

	case DataBuffer.TYPE_FLOAT:
	    floatLoop(srcAccessor, destRect, dstAccessor, xvalues, yvalues);
	    break;
 
	case DataBuffer.TYPE_DOUBLE:
	    doubleLoop(srcAccessor, destRect, dstAccessor, xvalues, yvalues);
	    break;

        default:
            throw new 
		RuntimeException(JaiI18N.getString("OrderedDitherOpImage0"));
        }

        // If the RasterAccessor object set up a temporary buffer for the 
        // op to write to, tell the RasterAccessor to write that data
        // to the raster no that we're done with it.
        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }
    

    private void byteLoop(RasterAccessor src, Rectangle dstRect,
			  RasterAccessor dst, int xvalues[], int yvalues[]) {
        
    	int srcPixelStride = src.getPixelStride();
    	int srcScanlineStride = src.getScanlineStride();
    	int srcLastXDataPos   = (src.getWidth()-1) * srcPixelStride;
    	int dwidth = dstRect.width;
    	int dheight = dstRect.height;

	// Get destination related variables.
        byte dstDataArrays[][] = dst.getByteDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
        int dnumBands = dst.getNumBands();

	// Get source related variables.
	int bandOffsets[] = src.getBandOffsets();
        byte srcDataArrays[][] = src.getByteDataArrays(); 

	int dstPixelOffset;
	int dstOffset = 0;
	int posy, posx, pos;

	int dstScanlineOffset;
	// For each band
	if (!hasROI){
		for (int k = 0; k < dnumBands; k++) {
		    byte dstData[] = dstDataArrays[k];
		    byte srcData[] = srcDataArrays[k];
		    int bandOffset = bandOffsets[k];
		    dstScanlineOffset = dstBandOffsets[k];
		    for (int j = 0; j < dheight; j++)  {
			dstPixelOffset = dstScanlineOffset;
			posy = yvalues[j] + bandOffset;
			for (int i = 0; i < dwidth; i++)  {
			    posx = xvalues[i];
			    pos = posx + posy;
			    dstData[dstPixelOffset] = srcData[pos];
			    dstPixelOffset += dstPixelStride;
			}
			dstScanlineOffset += dstScanlineStride;
		    } 
		}
	} else {
		for (int k = 0; k < dnumBands; k++)  {
            
            byte dstData[] = dstDataArrays[k];
            byte srcData[] = srcDataArrays[k];
            dstScanlineOffset = dstBandOffsets[k];
            int bandOffset = bandOffsets[k];
            
            for (int j = 0; j < dheight; j++) {

                dstPixelOffset = dstScanlineOffset;
                int posylow = yvalues[j] + bandOffset;

                for (int i = 0; i < dwidth; i++) {
                    int posxlow = xvalues[i];

                    // SG mapping source rect
                    int x0 = src.getX() + xvalues[i]/srcPixelStride;
                    int y0 = src.getY() + yvalues[j]/srcScanlineStride;
                    if (roiBounds.contains(x0, y0)){
                    	final int wx= roiIter.getSample(x0, y0, 0)& 0xFF;	
                    	final boolean insideROI=wx==1;
                    	if (insideROI) {
                    		dstData[dstPixelOffset] = srcData[posxlow + posylow];
                    	}
                    }    
                    dstPixelOffset += dstPixelStride;
                }
                dstScanlineOffset += dstScanlineStride;

            }
        }
	}
    }
    
    /**
     * Performs a scale operation on a specified rectangle. The sources are
     * cobbled.
     *
     * @param sources  an array of source Rasters, guaranteed to provide all
     *                 necessary source data for computing the output.
     * @param dest     a WritableRaster  containing the area to be computed.
     * @param destRect the rectangle within dest to be processed.
     */
    protected void computeRect(Raster [] sources,
                               WritableRaster dest,
                               Rectangle destRect,
                               Raster [] rois) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

	Raster source = sources[0];
	Raster roi = rois[0];

	// Get the source rectangle
        Rectangle srcRect = source.getBounds();

	int srcRectX = srcRect.x;
	int srcRectY = srcRect.y;

        RasterAccessor srcAccessor =
	    new RasterAccessor(source, srcRect, formatTags[0],
			       getSource(0).getColorModel());
        
        RasterAccessor roiAccessor = 
                new RasterAccessor(roi, srcRect, RasterAccessor.findCompatibleTags(
                		new RenderedImage[]{srcROIImage}, srcROIImage)[0], srcROIImage.getColorModel());


        RasterAccessor dstAccessor = 
            new RasterAccessor(dest, destRect, formatTags[1], getColorModel());

        int srcScanlineStride = srcAccessor.getScanlineStride();
        int roiScanlineStride = roiAccessor.getScanlineStride();
        int srcPixelStride = srcAccessor.getPixelStride();

	// Destination rectangle dimensions.
	int dx = destRect.x;
	int dy = destRect.y;
	int dwidth = destRect.width;
	int dheight = destRect.height;

	// Precalculate the x positions and store them in an array.
	int[] xvalues = new int[dwidth];

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

	// Separate the x source coordinate into integer and fractional part
	// int part is floor(sx), frac part is sx - floor(sx)
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

	for (int i = 0; i < dwidth; i++) {

	    // Calculate the position
	    xvalues[i] = (srcXInt - srcRectX) * srcPixelStride; 

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

        // Precalculate the y positions and store them in an array.
        int[] yvalues = new int[dheight];
        int[] yvaluesRoi = new int[dheight];

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

	for (int i = 0; i < dheight; i++) {

	    // Calculate the position
	    yvalues[i] = (srcYInt - srcRectY) * srcScanlineStride; 
            yvaluesRoi[i] = (srcYInt - srcRectY) * roiScanlineStride;

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

        switch (dstAccessor.getDataType()) {

        case DataBuffer.TYPE_BYTE:
            byteLoop(srcAccessor, destRect, dstAccessor, xvalues, yvalues, roiAccessor, yvaluesRoi);
            break;

        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT:
            shortLoop(srcAccessor, destRect, dstAccessor, xvalues, yvalues);
            break;

        case DataBuffer.TYPE_INT:
            intLoop(srcAccessor, destRect, dstAccessor, xvalues, yvalues);
            break;

	case DataBuffer.TYPE_FLOAT:
	    floatLoop(srcAccessor, destRect, dstAccessor, xvalues, yvalues);
	    break;
 
	case DataBuffer.TYPE_DOUBLE:
	    doubleLoop(srcAccessor, destRect, dstAccessor, xvalues, yvalues);
	    break;

        default:
            throw new 
		RuntimeException(JaiI18N.getString("OrderedDitherOpImage0"));
        }

        // If the RasterAccessor object set up a temporary buffer for the 
        // op to write to, tell the RasterAccessor to write that data
        // to the raster no that we're done with it.
        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }
    private void byteLoop(RasterAccessor src, Rectangle dstRect,
        RasterAccessor dst, int[] xvalues, int[] yvalues,
        RasterAccessor roi, int[] yvaluesRoi) {
      
  	int srcPixelStride = src.getPixelStride();
  	int srcScanlineStride = src.getScanlineStride();
  	int dwidth = dstRect.width;
  	int dheight = dstRect.height;

	// Get destination related variables.
      byte dstDataArrays[][] = dst.getByteDataArrays();
      byte roiDataArray[] = roi.getByteDataArray(0);
      final int roiDataLength = roiDataArray.length;
      int dstBandOffsets[] = dst.getBandOffsets();
      int dstPixelStride = dst.getPixelStride();
      int dstScanlineStride = dst.getScanlineStride();
      int dnumBands = dst.getNumBands();

	// Get source related variables.
	int bandOffsets[] = src.getBandOffsets();
      byte srcDataArrays[][] = src.getByteDataArrays(); 

	int dstPixelOffset;
	int posy, posx, pos;

	int dstScanlineOffset;
	// For each band
	if (!hasROI){
		for (int k = 0; k < dnumBands; k++) {
		    byte dstData[] = dstDataArrays[k];
		    byte srcData[] = srcDataArrays[k];
		    int bandOffset = bandOffsets[k];
		    dstScanlineOffset = dstBandOffsets[k];
		    for (int j = 0; j < dheight; j++)  {
			dstPixelOffset = dstScanlineOffset;
			posy = yvalues[j] + bandOffset;
			for (int i = 0; i < dwidth; i++)  {
			    posx = xvalues[i];
			    pos = posx + posy;
			    dstData[dstPixelOffset] = srcData[pos];
			    dstPixelOffset += dstPixelStride;
			}
			dstScanlineOffset += dstScanlineStride;
		    } 
		}
	} else if (useRoiAccessor) {
		int windex = 0;
		int w = 0;
		for (int k = 0; k < dnumBands; k++) {
		    byte dstData[] = dstDataArrays[k];
		    byte srcData[] = srcDataArrays[k];
		    int bandOffset = bandOffsets[k];
		    dstScanlineOffset = dstBandOffsets[k];
		    for (int j = 0; j < dheight; j++)  {
			dstPixelOffset = dstScanlineOffset;
			posy = yvalues[j] + bandOffset;
			for (int i = 0; i < dwidth; i++)  {
			    posx = xvalues[i];
			    pos = posx + posy;
                        windex = (posx / dnumBands) + (yvaluesRoi[j]);
			    w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
			    if (w == 0) {
    		    	dstPixelOffset += dstPixelStride;
    		    	continue;
    		    }
			    dstData[dstPixelOffset] = srcData[pos];
			    dstPixelOffset += dstPixelStride;
			}
			dstScanlineOffset += dstScanlineStride;
		    } 
		}
	
	} else {
		for (int k = 0; k < dnumBands; k++)  {
          
          byte dstData[] = dstDataArrays[k];
          byte srcData[] = srcDataArrays[k];
          dstScanlineOffset = dstBandOffsets[k];
          int bandOffset = bandOffsets[k];
          
          for (int j = 0; j < dheight; j++) {

              dstPixelOffset = dstScanlineOffset;
              int posylow = yvalues[j] + bandOffset;

              for (int i = 0; i < dwidth; i++) {
                  int posxlow = xvalues[i];


                  // SG mapping source rect
                  int x0=src.getX()+xvalues[i]/srcPixelStride;
                  int y0=src.getY()+yvalues[j]/srcScanlineStride;
                  if(!roiBounds.contains(x0, y0)){
                  	
                  } else {
                  	final int wx= roiIter.getSample(x0, y0, 0)& 0xFF;	
                  	final boolean insideROI=wx==1;
                  	if (insideROI) {
                  		dstData[dstPixelOffset] = srcData[posxlow + posylow];
                  	}
                  }    
                  dstPixelOffset += dstPixelStride;
              }
              dstScanlineOffset += dstScanlineStride;

          }
      }
	}
  }

    private void shortLoop(RasterAccessor src, Rectangle dstRect,
			   RasterAccessor dst, int xvalues[], int yvalues[]) {

	int dwidth = dstRect.width;
	int dheight = dstRect.height;

	// Get destination related variables.
        short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
        int dnumBands = dst.getNumBands();
	
	// Get source related variables.
	int bandOffsets[] = src.getBandOffsets();
        short srcDataArrays[][] = src.getShortDataArrays(); 

	int dstPixelOffset;
	int dstOffset = 0;
	int posy, posx, pos;

	int dstScanlineOffset;
	// For each band
	for (int k = 0; k < dnumBands; k++) {
	    short dstData[] = dstDataArrays[k];
	    short srcData[] = srcDataArrays[k];
	    int bandOffset = bandOffsets[k];
	    dstScanlineOffset = dstBandOffsets[k];
	    for (int j = 0; j < dheight; j++)  {
		dstPixelOffset = dstScanlineOffset;
		posy = yvalues[j] + bandOffset;
		for (int i = 0; i < dwidth; i++)  {
		    posx = xvalues[i];
		    pos = posx + posy;
		    dstData[dstPixelOffset] = srcData[pos];
		    dstPixelOffset += dstPixelStride;
		}
		dstScanlineOffset += dstScanlineStride;
	    } 
	}
    }

    // identical to byteLoops, except datatypes have changed.  clumsy,
    // but there's no other way in Java
    private void intLoop(RasterAccessor src, Rectangle dstRect,
			 RasterAccessor dst, int xvalues[], int yvalues[]) {

	int dwidth = dstRect.width;
	int dheight = dstRect.height;

        int dnumBands = dst.getNumBands(); 
	int dstDataArrays[][] = dst.getIntDataArrays();
	int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
	int dstScanlineStride = dst.getScanlineStride();
	
	int bandOffsets[] = src.getBandOffsets();
	int srcDataArrays[][] = src.getIntDataArrays();

	int dstPixelOffset;
	int dstOffset = 0;
	int posy, posx, pos;

	int dstScanlineOffset;
	// For each band
	for (int k = 0; k < dnumBands; k++) {
	    int dstData[] = dstDataArrays[k];
	    int srcData[] = srcDataArrays[k];
	    int bandOffset = bandOffsets[k];
	    dstScanlineOffset = dstBandOffsets[k];
	    for (int j = 0; j < dheight; j++)  {
		dstPixelOffset = dstScanlineOffset;
		posy = yvalues[j] + bandOffset;
		for (int i = 0; i < dwidth; i++)  {
		    posx = xvalues[i];
		    pos = posx + posy;
		    dstData[dstPixelOffset] = srcData[pos];
		    dstPixelOffset += dstPixelStride;
		}
		dstScanlineOffset += dstScanlineStride;
	    } 
	}
    }

    // identical to byteLoop, except datatypes have changed.  clumsy,
    // but there's no other way in Java
    private void floatLoop(RasterAccessor src, Rectangle dstRect,
			   RasterAccessor dst, int xvalues[], int yvalues[]) {

	int dwidth = dstRect.width;
	int dheight = dstRect.height;

        int dnumBands = dst.getNumBands();
	float dstDataArrays[][] = dst.getFloatDataArrays();
	int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
	int dstScanlineStride = dst.getScanlineStride();
	
	float srcDataArrays[][] = src.getFloatDataArrays();
	int bandOffsets[] = src.getBandOffsets();

	int dstPixelOffset;
	int dstOffset = 0;
	int posy, posx, pos;

	int dstScanlineOffset;
	// For each band
	for (int k = 0; k < dnumBands; k++) {
	    float dstData[] = dstDataArrays[k];
	    float srcData[] = srcDataArrays[k];
	    int bandOffset = bandOffsets[k];
	    dstScanlineOffset = dstBandOffsets[k];
	    for (int j = 0; j < dheight; j++)  {
		dstPixelOffset = dstScanlineOffset;
		posy = yvalues[j] + bandOffset;
		for (int i = 0; i < dwidth; i++)  {
		    posx = xvalues[i];
		    pos = posx + posy;
		    dstData[dstPixelOffset] = srcData[pos];
		    dstPixelOffset += dstPixelStride;
		}
		dstScanlineOffset += dstScanlineStride;
	    } 
	}
    }

    // identical to byteLoop, except datatypes have changed.  clumsy,
    // but there's no other way in Java
    private void doubleLoop(RasterAccessor src, Rectangle dstRect,
			    RasterAccessor dst, int xvalues[], int yvalues[]) {

	int dwidth = dstRect.width;
	int dheight = dstRect.height;

        int dnumBands = dst.getNumBands(); 
	double dstDataArrays[][] = dst.getDoubleDataArrays();
	int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
	int dstScanlineStride = dst.getScanlineStride();
	
	int bandOffsets[] = src.getBandOffsets();
	double srcDataArrays[][] = src.getDoubleDataArrays();

	int dstPixelOffset;
	int dstOffset = 0;
	int posy, posx, pos;

	int dstScanlineOffset;
	// For each band
	for (int k = 0; k < dnumBands; k++) {
	    double dstData[] = dstDataArrays[k];
	    double srcData[] = srcDataArrays[k];
	    int bandOffset = bandOffsets[k];
	    dstScanlineOffset = dstBandOffsets[k];
	    for (int j = 0; j < dheight; j++)  {
		dstPixelOffset = dstScanlineOffset;
		posy = yvalues[j] + bandOffset;
		for (int i = 0; i < dwidth; i++)  {
		    posx = xvalues[i];
		    pos = posx + posy;
		    dstData[dstPixelOffset] = srcData[pos];
		    dstPixelOffset += dstPixelStride;
		}
		dstScanlineOffset += dstScanlineStride;
	    } 
	}
    }

}
