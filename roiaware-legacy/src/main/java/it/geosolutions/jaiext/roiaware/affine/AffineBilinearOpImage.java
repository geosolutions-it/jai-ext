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
package it.geosolutions.jaiext.roiaware.affine;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
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

/**
 * An OpImage subclass that performs bilinear Affine mapping
 */
final class AffineBilinearOpImage extends AffineOpImage {

    /**
     * Constructs an AffineBilinearOpImage from a RenderedImage source,
     *
     * @param source a RenderedImage.
     * @param extender a BorderExtender, or null.
     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     * @param interp an Interpolation object to use for resampling
     * @param transform the desired AffineTransform.
     */
    public AffineBilinearOpImage(RenderedImage source,
                                 BorderExtender extender,
                                 Map config,
                                 ImageLayout layout,
                                 AffineTransform transform,
                                 Interpolation interp,
                                 double[] backgroundValues) {
        super(source,
              extender,
              config,
              layout,
              transform,
              interp,
              backgroundValues);
    }

    /**
     * Performs an affine transform on a specified rectangle. The sources are
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

        Rectangle srcRect = source.getBounds();

        int srcRectX = srcRect.x;
        int srcRectY = srcRect.y;

        //
        // Get data for the source rectangle & the destination rectangle
        // In the first version source Rectangle is the whole source
        // image always.
        //
        // See if we can cache the source to avoid multiple rasteraccesors
        //
        RasterAccessor srcAccessor =
            new RasterAccessor(source,
                               srcRect,
                               formatTags[0],
                               getSourceImage(0).getColorModel());
        RasterAccessor dstAccessor =
            new RasterAccessor(dest,
                               destRect,
                               formatTags[1],
                               getColorModel());

        switch (dstAccessor.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(srcAccessor,
                     destRect,
                     srcRectX,
                     srcRectY,
                     dstAccessor);
            break;

        case DataBuffer.TYPE_INT:
            intLoop(srcAccessor,
                    destRect,
                    srcRectX,
                    srcRectY,
                    dstAccessor);
            break;

        case DataBuffer.TYPE_SHORT:
            shortLoop(srcAccessor,
                      destRect,
                      srcRectX,
                      srcRectY,
                      dstAccessor);
            break;

        case DataBuffer.TYPE_USHORT:
            ushortLoop(srcAccessor,
                       destRect,
                       srcRectX,
                       srcRectY,
                       dstAccessor);
            break;

        case DataBuffer.TYPE_FLOAT:
            floatLoop(srcAccessor,
                      destRect,
                      srcRectX,
                      srcRectY,
                      dstAccessor);
	    break;

        case DataBuffer.TYPE_DOUBLE:
            doubleLoop(srcAccessor,
                       destRect,
                       srcRectX,
                       srcRectY,
                       dstAccessor);
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

    private void byteLoop(RasterAccessor src,
                          Rectangle destRect,
                          int srcRectX,
                          int srcRectY,
                          RasterAccessor dst) {

        float src_rect_x1 = src.getX();
        float src_rect_y1 = src.getY();
        float src_rect_x2 = src_rect_x1 + src.getWidth();
        float src_rect_y2 = src_rect_y1 + src.getHeight();


        int dstPixelOffset;
        int dstOffset = 0;

        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        byte dstDataArrays[][] = dst.getByteDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        byte srcDataArrays[][] = src.getByteDataArrays();
        int bandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        int dst_num_bands = dst.getNumBands();

        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

	byte[] backgroundByte = new byte[dst_num_bands];
	for (int i = 0; i < dst_num_bands; i++)
	    backgroundByte[i] = (byte)backgroundValues[i];

        // SG old loop with no ROI
        if(hasROI){
            for (int y = dst_min_y; y < dst_max_y; y++)  {
    
                dstPixelOffset = dstOffset;
    
                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation((double)dst_min_x + 0.5,
                                   (double)y + 0.5);
                mapDestPoint(dst_pt, src_pt);
    
                // Get the mapped source coordinates
                float s_x = (float)src_pt.getX();
                float s_y = (float)src_pt.getY();
    
                // As per definition of bilinear interpolation
                s_x -= 0.5;
                s_y -= 0.5;
    
                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);
    
                float fracx = s_x - (float)s_ix;
                float fracy = s_y - (float)s_iy;
    
                // Translate to/from SampleModel space & Raster space
                int pylow = (s_iy - srcRectY) * srcScanlineStride;
                int pxlow = (s_ix - srcRectX) * srcPixelStride;
                int pyhigh = pylow + srcScanlineStride;
                int pxhigh = pxlow + srcPixelStride;
    
                int tmp00 = pxlow + pylow;
                int tmp01 = pxhigh + pylow;
                int tmp10 = pxlow + pyhigh;
                int tmp11 = pxhigh + pyhigh;
    
                for (int x = dst_min_x; x < dst_max_x; x++)  {
                    //
                    // Check against the source rectangle
                    //
                    if ((s_ix >= src_rect_x1) &&
                        (s_ix < (src_rect_x2 - 1)) &&
                        (s_iy >= src_rect_y1) &&
                        (s_iy < (src_rect_y2 - 1))) {
                        for (int k2=0; k2 < dst_num_bands; k2++) {
                            //
                            // Get the 4 neighbourhood pixels
                            //
                            byte tmp_row[];
                            int tmp_col;
    
                            // Get to the right row
                            tmp_row = srcDataArrays[k2];
    
                            // Position at the bandOffset
                            tmp_col = bandOffsets[k2];
    
                            int s00 = tmp_row[tmp00 + tmp_col] & 0xff;
                            int s01 = tmp_row[tmp01 + tmp_col] & 0xff;
                            int s10 = tmp_row[tmp10 + tmp_col] & 0xff;
                            int s11 = tmp_row[tmp11 + tmp_col] & 0xff;
    
                            // Weighted Average of these 4 pixels
                            float s0 = (float) s00 + ((float) (s01 - s00) * fracx);
                            float s1 = (float) s10 + ((float) (s11 - s10) * fracx);
    
                            float tmp = s0 + ((s1 - s0) * fracy);
    
                            // Round
                            final int s;
                            if (tmp < 0.5F) {
                                s = 0;
                            } else if (tmp > 254.5F) {
                                s = 255;
                            } else {
                                s = (int) (tmp + 0.5F);
                            }
    
                            // Write the result
                            dstDataArrays[k2]
                                [dstPixelOffset+dstBandOffsets[k2]] =
                                (byte) (s & 0xff);
                        }
                    } else if (setBackground) {
    		    for (int k=0; k < dst_num_bands; k++)
    			dstDataArrays[k][dstPixelOffset+dstBandOffsets[k]] =
    			    backgroundByte[k];
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
    
                    // Translate to/from SampleModel space & Raster space
                    pylow = (s_iy - srcRectY) * srcScanlineStride;
                    pxlow = (s_ix - srcRectX) * srcPixelStride;
                    pyhigh = pylow + srcScanlineStride;
                    pxhigh = pxlow + srcPixelStride;
    
                    tmp00 = pxlow + pylow;
                    tmp01 = pxhigh + pylow;
                    tmp10 = pxlow + pyhigh;
                    tmp11 = pxhigh + pyhigh;
    
                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }
    
                // Go to the next line in the destination rectangle
                dstOffset += dstScanlineStride;
            }
        } else {
            //
            // ROI
            //
            for (int y = dst_min_y; y < dst_max_y; y++)  {
                
                dstPixelOffset = dstOffset;
    
                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation((double)dst_min_x + 0.5,(double)y + 0.5);
                mapDestPoint(dst_pt, src_pt);
    
                // Get the mapped source coordinates
                float s_x = (float)src_pt.getX();
                float s_y = (float)src_pt.getY();
    
                // As per definition of bilinear interpolation
                s_x -= 0.5;
                s_y -= 0.5;
    
                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);
    
                float fracx = s_x - (float)s_ix;
                float fracy = s_y - (float)s_iy;
    
                // Translate to/from SampleModel space & Raster space
                int pylow = (s_iy - srcRectY) * srcScanlineStride;
                int pxlow = (s_ix - srcRectX) * srcPixelStride;
                int pyhigh = pylow + srcScanlineStride;
                int pxhigh = pxlow + srcPixelStride;
                
                int tmp00 = pxlow + pylow;
                int tmp01 = pxhigh + pylow;
                int tmp10 = pxlow + pyhigh;
                int tmp11 = pxhigh + pyhigh;
    
                for (int x = dst_min_x; x < dst_max_x; x++)  {

                    // SG mapping source coordinate for using the ROI
                    int x0=src.getX()+pxlow/srcPixelStride;
                    int y0=src.getY()+pylow/srcScanlineStride;   
                    if(!roiBounds.contains(x0, y0)){
                        if (setBackground) {
                            for (int k=0; k < dst_num_bands; k++)
                                dstDataArrays[k][dstPixelOffset+dstBandOffsets[k]] =backgroundByte[k];
                            }       
                    } else {
                        // WITHIN ROI BOUNDS
                        final int w00=roiIter.getSample(x0, y0, 0);
                        final int w01=roiIter.getSample(x0+1, y0, 0);
                        final int w10=roiIter.getSample(x0, y0+1, 0);
                        final int w11=roiIter.getSample(x0+1, y0+1, 0);
                        if(w00==0&&w01==0&&w10==0&&w11==0){
                            if (setBackground) {
                                for (int k=0; k < dst_num_bands; k++)
                                    dstDataArrays[k][dstPixelOffset+dstBandOffsets[k]] =
                                        backgroundByte[k];
                                }
                        } else {
                            //
                            // Check against the source rectangle
                            //
                            if ((s_ix >= src_rect_x1) &&
                                (s_ix < (src_rect_x2 - 1)) &&
                                (s_iy >= src_rect_y1) &&
                                (s_iy < (src_rect_y2 - 1))) {
                                for (int k2=0; k2 < dst_num_bands; k2++) {
                                    //
                                    // Get the 4 neighbourhood pixels
                                    //
                                    byte tmp_row[];
                                    int tmp_col;
            
                                    // Get to the right row
                                    tmp_row = srcDataArrays[k2];
            
                                    // Position at the bandOffset
                                    tmp_col = bandOffsets[k2];
            
                                    int s00 = tmp_row[tmp00 + tmp_col] & 0xff;
                                    int s01 = tmp_row[tmp01 + tmp_col] & 0xff;
                                    int s10 = tmp_row[tmp10 + tmp_col] & 0xff;
                                    int s11 = tmp_row[tmp11 + tmp_col] & 0xff;
            
                                    // Weighted Average of these 4 pixels
                                    
                                    // upper value
                                    final float s0;
                                    if(w00==0&&w01==0)
                                        s0=Float.NaN;
                                    else
                                        s0= (s01*w01*(w00==0?1/fracx:1) - s00*w00*(w01==0?0:1)) * (fracx) + s00*w00;
                                            
                                    // lower value
                                    final float s1;
                                    if(w10==0&&w11==0)
                                        s1=Float.NaN;
                                    else
                                        s1= (s11*w11*(w10==0?1/fracx:1) - s10*w10*(w11==0?0:1)) * (fracx) + s10*w10;
                                    
        
                                    float s;
                                    if(Float.isNaN(s0))
                                        s=s1;
                                    else
                                        if(Float.isNaN(s1))
                                            s=s0;
                                        else
                                            s = (s1 - s0) * fracy + s0;
                                    
                                    // Round
                                    if (s < 0.5F) {
                                        s = 0;
                                    } else if (s > 254.5F) {
                                        s = 255;
                                    } else {
                                        s = (int) (s + 0.5F);
                                    }
            
                                    // Write the result
                                    dstDataArrays[k2]
                                        [dstPixelOffset+dstBandOffsets[k2]] =(byte) (((int)s) & 0xff);
                                }
                            } else 
                                if (setBackground) {
                                    for (int k=0; k < dst_num_bands; k++)
                                        dstDataArrays[k][dstPixelOffset+dstBandOffsets[k]] =backgroundByte[k];
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
    
                    // Translate to/from SampleModel space & Raster space
                    pylow = (s_iy - srcRectY) * srcScanlineStride;
                    pxlow = (s_ix - srcRectX) * srcPixelStride;
                    pyhigh = pylow + srcScanlineStride;
                    pxhigh = pxlow + srcPixelStride;
    
                    tmp00 = pxlow + pylow;
                    tmp01 = pxhigh + pylow;
                    tmp10 = pxlow + pyhigh;
                    tmp11 = pxhigh + pyhigh;
    
                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }
    
                // Go to the next line in the destination rectangle
                dstOffset += dstScanlineStride;
            }
        }
    }

    private void intLoop(RasterAccessor src,
                         Rectangle destRect,
                         int srcRectX,
                         int srcRectY,
                         RasterAccessor dst) {

        float src_rect_x1 = src.getX();
        float src_rect_y1 = src.getY();
        float src_rect_x2 = src_rect_x1 + src.getWidth();
        float src_rect_y2 = src_rect_y1 + src.getHeight();

        float s_x, s_y;

        float fracx, fracy;

        int pxlow, pylow, pxhigh, pyhigh;

        int s, s00, s01, s10, s11;
        float s0, s1;
        float tmp;

        int dstPixelOffset;
        int dstOffset = 0;

        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int dstDataArrays[][] = dst.getIntDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        int srcDataArrays[][] = src.getIntDataArrays();
        int bandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        int dst_num_bands = dst.getNumBands();

        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

	int[] backgroundInt = new int[dst_num_bands];
	for (int i = 0; i < dst_num_bands; i++)
	    backgroundInt[i] = (int)backgroundValues[i];

        for (int y = dst_min_y; y < dst_max_y; y++)  {

            dstPixelOffset = dstOffset;

            // Backward map the first point in the line
            dst_pt.setLocation((double)dst_min_x + 0.5,
                               (double)y + 0.5);
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            s_x = (float)src_pt.getX();
            s_y = (float)src_pt.getY();

            // As per definition of bilinear interpolation
            s_x -= 0.5;
            s_y -= 0.5;

            // Floor to get the integral coordinate
            int s_ix = (int) Math.floor(s_x);
            int s_iy = (int) Math.floor(s_y);

            fracx = s_x - (float)s_ix;
            fracy = s_y - (float)s_iy;

            // Translate to/from SampleModel space & Raster space
            pylow = (s_iy - srcRectY) * srcScanlineStride;
            pxlow = (s_ix - srcRectX) * srcPixelStride;
            pyhigh = pylow + srcScanlineStride;
            pxhigh = pxlow + srcPixelStride;

            int tmp00 = pxlow + pylow;
            int tmp01 = pxhigh + pylow;
            int tmp10 = pxlow + pyhigh;
            int tmp11 = pxhigh + pyhigh;

            for (int x = dst_min_x; x < dst_max_x; x++)  {
                //
                // Check against the source rectangle
                //
                if ((s_ix >= src_rect_x1) &&
                    (s_ix < (src_rect_x2 - 1)) &&
                    (s_iy >= src_rect_y1) &&
                    (s_iy < (src_rect_y2 - 1))) {
                    for (int k2=0; k2 < dst_num_bands; k2++) {
                        //
                        // Get the 4 neighbourhood pixels
                        //
                        int tmp_row[];
                        int tmp_col;

                        // Get to the right row
                        tmp_row = srcDataArrays[k2];

                        // Position at the bandOffset
                        tmp_col = bandOffsets[k2];

                        s00 = tmp_row[tmp00 + tmp_col];
                        s01 = tmp_row[tmp01 + tmp_col];
                        s10 = tmp_row[tmp10 + tmp_col];
                        s11 = tmp_row[tmp11 + tmp_col];

                        // Weighted Average of these 4 pixels
                        s0 = (float) s00 + ((float) (s01 - s00) * fracx);
                        s1 = (float) s10 + ((float) (s11 - s10) * fracx);

                        tmp = s0 + ((s1 - s0) * fracy);

                        // Round
                        if (tmp < (float) Integer.MIN_VALUE) {
                            s = Integer.MIN_VALUE;
                        } else if (tmp > (float) Integer.MAX_VALUE) {
                            s = Integer.MAX_VALUE;
                        } else if (tmp > 0) {
                            s = (int) (tmp + 0.5F);
                        } else {
                            s = (int) (tmp - 0.5F);
                        }

                        // Write the result
                        dstDataArrays[k2]
                            [dstPixelOffset+dstBandOffsets[k2]] = s;
                    }
                } else if (setBackground) {
		    for (int k=0; k < dst_num_bands; k++)
			dstDataArrays[k][dstPixelOffset+dstBandOffsets[k]] =
			    backgroundInt[k];
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

                // Translate to/from SampleModel space & Raster space
                pylow = (s_iy - srcRectY) * srcScanlineStride;
                pxlow = (s_ix - srcRectX) * srcPixelStride;
                pyhigh = pylow + srcScanlineStride;
                pxhigh = pxlow + srcPixelStride;

                tmp00 = pxlow + pylow;
                tmp01 = pxhigh + pylow;
                tmp10 = pxlow + pyhigh;
                tmp11 = pxhigh + pyhigh;

                dstPixelOffset += dstPixelStride;
            }

            dstOffset += dstScanlineStride;
        }
    }

    private void shortLoop(RasterAccessor src,
                           Rectangle destRect,
                           int srcRectX,
                           int srcRectY,
                           RasterAccessor dst) {

        float src_rect_x1 = src.getX();
        float src_rect_y1 = src.getY();
        float src_rect_x2 = src_rect_x1 + src.getWidth();
        float src_rect_y2 = src_rect_y1 + src.getHeight();

        float s_x, s_y;

        float fracx, fracy;

        int pxlow, pylow, pxhigh, pyhigh;

        int s, s00, s01, s10, s11;
        float s0, s1;
        float tmp;

        int dstPixelOffset;
        int dstOffset = 0;

        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        short srcDataArrays[][] = src.getShortDataArrays();
        int bandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        int dst_num_bands = dst.getNumBands();

        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

	short[] backgroundShort = new short[dst_num_bands];
	for (int i = 0; i < dst_num_bands; i++)
	    backgroundShort[i] = (short)backgroundValues[i];

        for (int y = dst_min_y; y < dst_max_y; y++)  {

            dstPixelOffset = dstOffset;

            // Backward map the first point in the line
            dst_pt.setLocation((double)dst_min_x + 0.5,
                               (double)y + 0.5);
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            s_x = (float)src_pt.getX();
            s_y = (float)src_pt.getY();

            // As per definition of bilinear interpolation
            s_x -= 0.5;
            s_y -= 0.5;

            // Floor to get the integral coordinate
            int s_ix = (int) Math.floor(s_x);
            int s_iy = (int) Math.floor(s_y);

            fracx = s_x - (float)s_ix;
            fracy = s_y - (float)s_iy;

            // Translate to/from SampleModel space & Raster space
            pylow = (s_iy - srcRectY) * srcScanlineStride;
            pxlow = (s_ix - srcRectX) * srcPixelStride;
            pyhigh = pylow + srcScanlineStride;
            pxhigh = pxlow + srcPixelStride;

            int tmp00 = pxlow + pylow;
            int tmp01 = pxhigh + pylow;
            int tmp10 = pxlow + pyhigh;
            int tmp11 = pxhigh + pyhigh;

            for (int x = dst_min_x; x < dst_max_x; x++)  {
                //
                // Check against the source rectangle
                //
                if ((s_ix >= src_rect_x1) &&
                    (s_ix < (src_rect_x2 - 1)) &&
                    (s_iy >= src_rect_y1) &&
                    (s_iy < (src_rect_y2 - 1))) {
                    for (int k2=0; k2 < dst_num_bands; k2++) {
                        //
                        // Get the 4 neighbourhood pixels
                        //
                        short tmp_row[];
                        int tmp_col;

                        // Get to the right row
                        tmp_row = srcDataArrays[k2];

                        // Position at the bandOffset
                        tmp_col = bandOffsets[k2];

                        s00 = tmp_row[tmp00 + tmp_col];
                        s01 = tmp_row[tmp01 + tmp_col];
                        s10 = tmp_row[tmp10 + tmp_col];
                        s11 = tmp_row[tmp11 + tmp_col];

                        // Weighted Average of these 4 pixels
                        s0 = (float) s00 + ((float) (s01 - s00) * fracx);
                        s1 = (float) s10 + ((float) (s11 - s10) * fracx);
                        tmp = s0 + ((s1 - s0) * fracy);

                        // Round
                        if (tmp < ((float) Short.MIN_VALUE)) {
                            s = Short.MIN_VALUE;
                        } else if (tmp > ((float) Short.MAX_VALUE)) {
                            s = Short.MAX_VALUE;
                        } else if (tmp > 0 ) {
                            s = (int) (tmp + 0.5F);
                        } else {
                            s = (int) (tmp - 0.5F);
                        }

                        // Write the result
                        dstDataArrays[k2]
                            [dstPixelOffset+dstBandOffsets[k2]] = (short)(s);
                    }
                } else if (setBackground) {
		    for (int k=0; k < dst_num_bands; k++)
			dstDataArrays[k][dstPixelOffset+dstBandOffsets[k]] =
			    backgroundShort[k];
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

                // Translate to/from SampleModel space & Raster space
                pylow = (s_iy - srcRectY) * srcScanlineStride;
                pxlow = (s_ix - srcRectX) * srcPixelStride;
                pyhigh = pylow + srcScanlineStride;
                pxhigh = pxlow + srcPixelStride;

                tmp00 = pxlow + pylow;
                tmp01 = pxhigh + pylow;
                tmp10 = pxlow + pyhigh;
                tmp11 = pxhigh + pyhigh;

                dstPixelOffset += dstPixelStride;
            }

            dstOffset += dstScanlineStride;
        }
    }

    private void ushortLoop(RasterAccessor src,
                            Rectangle destRect,
                            int srcRectX,
                            int srcRectY,
                            RasterAccessor dst) {

        float src_rect_x1 = src.getX();
        float src_rect_y1 = src.getY();
        float src_rect_x2 = src_rect_x1 + src.getWidth();
        float src_rect_y2 = src_rect_y1 + src.getHeight();

        float s_x, s_y;

        float fracx, fracy;

        int pxlow, pylow, pxhigh, pyhigh;

        int s, s00, s01, s10, s11;
        float s0, s1;
        float tmp;

        int dstPixelOffset;
        int dstOffset = 0;

        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        short srcDataArrays[][] = src.getShortDataArrays();
        int bandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        int dst_num_bands = dst.getNumBands();

        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

	short[] backgroundUShort = new short[dst_num_bands];
	for (int i = 0; i < dst_num_bands; i++)
	    backgroundUShort[i] = (short)backgroundValues[i];

        for (int y = dst_min_y; y < dst_max_y; y++)  {

            dstPixelOffset = dstOffset;

            // Backward map the first point in the line
            dst_pt.setLocation((double)dst_min_x + 0.5,
                               (double)y + 0.5);
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            s_x = (float)src_pt.getX();
            s_y = (float)src_pt.getY();

            // As per definition of bilinear interpolation
            s_x -= 0.5;
            s_y -= 0.5;

            // Floor to get the integral coordinate
            int s_ix = (int) Math.floor(s_x);
            int s_iy = (int) Math.floor(s_y);

            fracx = s_x - (float)s_ix;
            fracy = s_y - (float)s_iy;

            // Translate to/from SampleModel space & Raster space
            pylow = (s_iy - srcRectY) * srcScanlineStride;
            pxlow = (s_ix - srcRectX) * srcPixelStride;
            pyhigh = pylow + srcScanlineStride;
            pxhigh = pxlow + srcPixelStride;

            int tmp00 = pxlow + pylow;
            int tmp01 = pxhigh + pylow;
            int tmp10 = pxlow + pyhigh;
            int tmp11 = pxhigh + pyhigh;

            for (int x = dst_min_x; x < dst_max_x; x++)  {
                //
                // Check against the source rectangle
                //
                if ((s_ix >= src_rect_x1) &&
                    (s_ix < (src_rect_x2 - 1)) &&
                    (s_iy >= src_rect_y1) &&
                    (s_iy < (src_rect_y2 - 1))) {
                    for (int k2=0; k2 < dst_num_bands; k2++) {
                        //
                        // Get the 4 neighbourhood pixels
                        //
                        short tmp_row[];
                        int tmp_col;

                        // Get to the right row
                        tmp_row = srcDataArrays[k2];

                        // Position at the bandOffset
                        tmp_col = bandOffsets[k2];

                        s00 = tmp_row[tmp00 + tmp_col] & 0xffff;
                        s01 = tmp_row[tmp01 + tmp_col] & 0xffff;
                        s10 = tmp_row[tmp10 + tmp_col] & 0xffff;
                        s11 = tmp_row[tmp11 + tmp_col] & 0xffff;

                        // Weighted Average of these 4 pixels
                        s0 = (float) s00 + ((float) (s01 - s00) * fracx);
                        s1 = (float) s10 + ((float) (s11 - s10) * fracx);
                        tmp = s0 + ((s1 - s0) * fracy);

                        // Round
                        if (tmp < 0.0) {
                            s = 0;
                        } else if (tmp > (float)(USHORT_MAX)) {
                            s = (int) (USHORT_MAX);
                        } else {
                            s = (int) (tmp + 0.5F);
                        }

                        // Write the result
                        dstDataArrays[k2]
                            [dstPixelOffset+dstBandOffsets[k2]] =
                            (short)(s & 0xFFFF);
                    }
                } else if (setBackground) {
		    for (int k=0; k < dst_num_bands; k++)
			dstDataArrays[k][dstPixelOffset+dstBandOffsets[k]] =
			    backgroundUShort[k];
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

                // Translate to/from SampleModel space & Raster space
                pylow = (s_iy - srcRectY) * srcScanlineStride;
                pxlow = (s_ix - srcRectX) * srcPixelStride;
                pyhigh = pylow + srcScanlineStride;
                pxhigh = pxlow + srcPixelStride;

                tmp00 = pxlow + pylow;
                tmp01 = pxhigh + pylow;
                tmp10 = pxlow + pyhigh;
                tmp11 = pxhigh + pyhigh;

                dstPixelOffset += dstPixelStride;
            }

            dstOffset += dstScanlineStride;
        }
    }

    private void floatLoop(RasterAccessor src,
                           Rectangle destRect,
                           int srcRectX,
                           int srcRectY,
                           RasterAccessor dst) {

        float src_rect_x1 = src.getX();
        float src_rect_y1 = src.getY();
        float src_rect_x2 = src_rect_x1 + src.getWidth();
        float src_rect_y2 = src_rect_y1 + src.getHeight();

        float s_x, s_y;

        float fracx, fracy;

        int pxlow, pylow, pxhigh, pyhigh;

        float s, s00, s01, s10, s11;
        float s0, s1;

        int dstPixelOffset;
        int dstOffset = 0;

        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        float dstDataArrays[][] = dst.getFloatDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        float srcDataArrays[][] = src.getFloatDataArrays();
        int bandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        int dst_num_bands = dst.getNumBands();

        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

	float[] backgroundFloat = new float[dst_num_bands];
	for (int i = 0; i < dst_num_bands; i++)
	    backgroundFloat[i] = (float)backgroundValues[i];

        for (int y = dst_min_y; y < dst_max_y; y++)  {

            dstPixelOffset = dstOffset;

            // Backward map the first point in the line
            dst_pt.setLocation((double)dst_min_x + 0.5,
                               (double)y + 0.5);
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            s_x = (float)src_pt.getX();
            s_y = (float)src_pt.getY();

            // As per definition of bilinear interpolation
            s_x -= 0.5;
            s_y -= 0.5;

            // Floor to get the integral coordinate
            int s_ix = (int) Math.floor(s_x);
            int s_iy = (int) Math.floor(s_y);

            fracx = s_x - (float)s_ix;
            fracy = s_y - (float)s_iy;

            // Translate to/from SampleModel space & Raster space
            pylow = (s_iy - srcRectY) * srcScanlineStride;
            pxlow = (s_ix - srcRectX) * srcPixelStride;
            pyhigh = pylow + srcScanlineStride;
            pxhigh = pxlow + srcPixelStride;

            int tmp00 = pxlow + pylow;
            int tmp01 = pxhigh + pylow;
            int tmp10 = pxlow + pyhigh;
            int tmp11 = pxhigh + pyhigh;

            for (int x = dst_min_x; x < dst_max_x; x++)  {
                //
                // Check against the source rectangle
                //
                if ((s_ix >= src_rect_x1) &&
                    (s_ix < (src_rect_x2 - 1)) &&
                    (s_iy >= src_rect_y1) &&
                    (s_iy < (src_rect_y2 - 1))) {
                    for (int k2=0; k2 < dst_num_bands; k2++) {
                        //
                        // Get the 4 neighbourhood pixels
                        //
                        float tmp_row[];
                        int tmp_col;

                        // Get to the right row
                        tmp_row = srcDataArrays[k2];

                        // Position at the bandOffset
                        tmp_col = bandOffsets[k2];

                        s00 = tmp_row[tmp00 + tmp_col];
                        s01 = tmp_row[tmp01 + tmp_col];
                        s10 = tmp_row[tmp10 + tmp_col];
                        s11 = tmp_row[tmp11 + tmp_col];

                        // Weighted Average of these 4 pixels
                        s0 = s00 + ((s01 - s00) * fracx);
                        s1 = s10 + ((s11 - s10) * fracx);
                        s = s0 + ((s1 - s0) * fracy);

                        // Write the result
                        dstDataArrays[k2]
                            [dstPixelOffset+dstBandOffsets[k2]] = s;
                    }
                } else if (setBackground) {
		    for (int k=0; k < dst_num_bands; k++)
			dstDataArrays[k][dstPixelOffset+dstBandOffsets[k]] =
			    backgroundFloat[k];
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

                // Translate to/from SampleModel space & Raster space
                pylow = (s_iy - srcRectY) * srcScanlineStride;
                pxlow = (s_ix - srcRectX) * srcPixelStride;
                pyhigh = pylow + srcScanlineStride;
                pxhigh = pxlow + srcPixelStride;

                tmp00 = pxlow + pylow;
                tmp01 = pxhigh + pylow;
                tmp10 = pxlow + pyhigh;
                tmp11 = pxhigh + pyhigh;

                dstPixelOffset += dstPixelStride;
            }

            dstOffset += dstScanlineStride;
        }
    }

    private void doubleLoop(RasterAccessor src,
                            Rectangle destRect,
                            int srcRectX,
                            int srcRectY,
                            RasterAccessor dst) {

        float src_rect_x1 = src.getX();
        float src_rect_y1 = src.getY();
        float src_rect_x2 = src_rect_x1 + src.getWidth();
        float src_rect_y2 = src_rect_y1 + src.getHeight();

        float s_x, s_y;

        double fracx, fracy;

        int pxlow, pylow, pxhigh, pyhigh;

        double s, s00, s01, s10, s11;
        double s0, s1;

        int dstPixelOffset;
        int dstOffset = 0;

        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        double dstDataArrays[][] = dst.getDoubleDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        double srcDataArrays[][] = src.getDoubleDataArrays();
        int bandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        int dst_num_bands = dst.getNumBands();

        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

        for (int y = dst_min_y; y < dst_max_y; y++)  {

            dstPixelOffset = dstOffset;

            // Backward map the first point in the line
            dst_pt.setLocation((double)dst_min_x + 0.5,
                               (double)y + 0.5);
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            s_x = (float)src_pt.getX();
            s_y = (float)src_pt.getY();

            // As per definition of bilinear interpolation
            s_x -= 0.5;
            s_y -= 0.5;

            // Floor to get the integral coordinate
            int s_ix = (int) Math.floor(s_x);
            int s_iy = (int) Math.floor(s_y);

            fracx = s_x - (float)s_ix;
            fracy = s_y - (float)s_iy;

            // Translate to/from SampleModel space & Raster space
            pylow = (s_iy - srcRectY) * srcScanlineStride;
            pxlow = (s_ix - srcRectX) * srcPixelStride;
            pyhigh = pylow + srcScanlineStride;
            pxhigh = pxlow + srcPixelStride;

            int tmp00 = pxlow + pylow;
            int tmp01 = pxhigh + pylow;
            int tmp10 = pxlow + pyhigh;
            int tmp11 = pxhigh + pyhigh;

            for (int x = dst_min_x; x < dst_max_x; x++)  {
                //
                // Check against the source rectangle
                //
                if ((s_ix >= src_rect_x1) &&
                    (s_ix < (src_rect_x2 - 1)) &&
                    (s_iy >= src_rect_y1) &&
                    (s_iy < (src_rect_y2 - 1))) {
                    for (int k2=0; k2 < dst_num_bands; k2++) {
                        //
                        // Get the 4 neighbourhood pixels
                        //
                        double tmp_row[];
                        int tmp_col;

                        // Get to the right row
                        tmp_row = srcDataArrays[k2];

                        // Position at the bandOffset
                        tmp_col = bandOffsets[k2];

                        s00 = tmp_row[tmp00 + tmp_col];
                        s01 = tmp_row[tmp01 + tmp_col];
                        s10 = tmp_row[tmp10 + tmp_col];
                        s11 = tmp_row[tmp11 + tmp_col];

                        // Weighted Average of these 4 pixels
                        s0 = s00 + ((s01 - s00) * fracx);
                        s1 = s10 + ((s11 - s10) * fracx);
                        s = s0 + ((s1 - s0) * fracy);

                        // Write the result
                        dstDataArrays[k2]
                            [dstPixelOffset+dstBandOffsets[k2]] = s;
                    }
                } else if (setBackground) {
		    for (int k=0; k < dst_num_bands; k++)
			dstDataArrays[k][dstPixelOffset+dstBandOffsets[k]] =
			    backgroundValues[k];
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

                // Translate to/from SampleModel space & Raster space
                pylow = (s_iy - srcRectY) * srcScanlineStride;
                pxlow = (s_ix - srcRectX) * srcPixelStride;
                pyhigh = pylow + srcScanlineStride;
                pxhigh = pxlow + srcPixelStride;

                tmp00 = pxlow + pylow;
                tmp01 = pxhigh + pylow;
                tmp10 = pxlow + pyhigh;
                tmp11 = pxhigh + pyhigh;

                dstPixelOffset += dstPixelStride;
            }

            dstOffset += dstScanlineStride;
        }
    }
}
