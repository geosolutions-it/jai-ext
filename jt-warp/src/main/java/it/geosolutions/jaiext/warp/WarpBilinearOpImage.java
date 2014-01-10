/*
 *    JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    (C) 2012, GeoSolutions
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    either version 3 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package it.geosolutions.jaiext.warp;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.Warp;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

/**
 * An <code>OpImage</code> implementing the general "Warp" operation as
 * described in <code>javax.media.jai.operator.WarpDescriptor</code>.
 * It supports the bilinear interpolation.
 *
 * @since EA2
 * @see javax.media.jai.Warp
 * @see javax.media.jai.WarpOpImage
 * @see javax.media.jai.operator.WarpDescriptor
 * @see WarpRIF
 *
 */
@SuppressWarnings("unchecked")
final class WarpBilinearOpImage extends WarpOpImage {

    /** Color table representing source's IndexColorModel. */
    private byte[][] ctable = null;

    private static final int TILE_EXTENDER = 1;

    private static final boolean ARRAY_CALC = true;

    private static final boolean TILE_CACHED = true;

    private byte destinationNoDataByte;

    private short destinationNoDataShort;

    private int destinationNoDataInt;

    private float destinationNoDataFloat;

    private byte[] byteLookupTable;

    private double destinationNoDataDouble;
    
    
    
    
    //TODO ELIMINATE THIS 3 PARAMETERS ASSOCIATED TO THE ROI
    private PlanarImage roImage;
    
    private RandomIter iterRoi;

    private Rectangle roiBounds;
    
    /**
     * Constructs a ROIAwareWarpBilinearOpImage.
     *
     * @param source  The source image.
     * @param extender A BorderExtender, or null.
     * @param layout  The destination image layout.
     * @param warp    An object defining the warp algorithm.
     * @param interp  An object describing the interpolation method.
     */
    public WarpBilinearOpImage(final RenderedImage source,
                               final BorderExtender extender,
                               final Map<?,?> config,
                               final ImageLayout layout,
                               final Warp warp,
                               final Interpolation interp,
                               final ROI sourceROI) {
        super(source,
              layout,
              config,
              false,
              extender,
              interp,
              warp,
              null,
              sourceROI);
        
        /*
         * If the source has IndexColorModel, get the RGB color table.
         * Note, in this case, the source should have an integral data type.
         * And dest always has data type byte.
         */
        final ColorModel srcColorModel = source.getColorModel();
        if (srcColorModel instanceof IndexColorModel) {
            final IndexColorModel icm = (IndexColorModel)srcColorModel;
            ctable = new byte[3][icm.getMapSize()];
            icm.getReds(ctable[0]);
            icm.getGreens(ctable[1]);
            icm.getBlues(ctable[2]);
        }
        
        /*
         * Selection of a destinationNoData value for each datatype
         */
        destinationNoDataDouble = backgroundValues[0];
        SampleModel sm = source.getSampleModel();
        // Source image data Type
        int srcDataType = sm.getDataType();

        switch (srcDataType) {
        case DataBuffer.TYPE_BYTE:
            destinationNoDataByte = (byte) (((byte) destinationNoDataDouble) & 0xff);
            // Creation of a lookuptable containing the values to use for no data
            if (hasNoData) {
                for (int i = 0; i < byteLookupTable.length; i++) {
                    byte value = (byte) i;
                    if (noDataRange.contains(value)) {
                        if (setBackground) {
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
            destinationNoDataShort = (short) (((short) destinationNoDataDouble) & 0xffff);
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

    protected void computeRectByte(final PlanarImage src, final RasterAccessor dst, final ROI roiTile) {
        RandomIter iterSource;
        if(extender != null) {
            final Rectangle bounds = new Rectangle(src.getMinX(), src.getMinY(),src.getWidth() + 1,src.getHeight() + 1);
            iterSource = RandomIterFactory.create(src.getExtendedData(bounds,extender),bounds);
        } else {
            iterSource = RandomIterFactory.create(src, src.getBounds());
        }
        final int minX = src.getMinX();
        final int maxX = src.getMaxX() -
            (extender != null ? 0 : 1); // Right padding
        final int minY = src.getMinY();
        final int maxY = src.getMaxY() -
            (extender != null ? 0 : 1); // Bottom padding

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int lineStride = dst.getScanlineStride();
        final int pixelStride = dst.getPixelStride();
        final int[] bandOffsets = dst.getBandOffsets();
        final byte[][] data = dst.getByteDataArrays();

        final float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

	final byte[] backgroundByte = new byte[dstBands];
	for (int i = 0; i < dstBands; i++)
	    backgroundByte[i] = (byte)backgroundValues[i];

        if (ctable == null) {	// source does not have IndexColorModel
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1,
                              warpData);
                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    final float sx = warpData[count++];
                    final float sy = warpData[count++];

                    final int xint = floor(sx);
                    final int yint = floor(sy);
                    final float xfrac = sx - xint;
                    final float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX ||
                        yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset+bandOffsets[b]] =
                                    backgroundByte[b];
                            }
                        }
                    } else {
                        
                        if(!hasROI){
                            //
                            // NO ROI
                            // 
                            for (int b = 0; b < dstBands; b++) {
                                int s00 = iterSource.getSample(xint, yint, b) & 0xFF;
                                int s01 = iterSource.getSample(xint+1, yint, b) & 0xFF;
                                int s10 = iterSource.getSample(xint, yint+1, b) & 0xFF;
                                int s11 = iterSource.getSample(xint+1, yint+1, b) & 0xFF;

                                float s0 = (s01 - s00) * xfrac + s00;
                                float s1 = (s11 - s10) * xfrac + s10;
                                float s = (s1 - s0) * yfrac + s0;

                                data[b][pixelOffset+bandOffsets[b]] = (byte)s;
                            }
                        }else{
                            //
                            // ROI
                            //
                            if(!roiBounds.contains(xint,yint)){
                                for (int b = 0; b < dstBands; b++){
                                    data[b][pixelOffset+bandOffsets[b]]=backgroundByte[b];
                                }
                                
                            } else {                        
                                // checks with roi
                                final int w00 = iterRoi.getSample(xint, yint, 0) ;
                                final int w01 = iterRoi.getSample(xint+1, yint, 0) ;
                                final int w10 = iterRoi.getSample(xint, yint+1, 0) ;
                                final int w11 = iterRoi.getSample(xint+1, yint+1, 0) ;
                                if(w00==0&&w01==0&&w10==0&&w11==0){ // SG should not happen
                                    for (int b = 0; b < dstBands; b++){
                                        data[b][pixelOffset+bandOffsets[b]]=backgroundByte[b];
                                    }
                                } else {
                                                        
                                    for (int b = 0; b < dstBands; b++) {
                                        final int s00 = iterSource.getSample(xint, yint, b) & 0xFF;
                                        final int s01 = iterSource.getSample(xint+1, yint, b) & 0xFF;
                                        final int s10 = iterSource.getSample(xint, yint+1, b) & 0xFF;
                                        final int s11 = iterSource.getSample(xint+1, yint+1, b) & 0xFF;
                                        
                                        // upper value
                                        final float s0;
                                        if(w00==0&&w01==0)
                                            s0=Float.NaN;
                                        else
                                            s0= (s01*w01*(w00==0?1/xfrac:1) - s00*w00*(w01==0?0:1)) * (xfrac) + s00*w00;
                                                
                                        // lower value
                                        final float s1;
                                        if(w10==0&&w11==0)
                                            s1=Float.NaN;
                                        else
                                            s1= (s11*w11*(w10==0?1/xfrac:1) - s10*w10*(w11==0?0:1)) * (xfrac) + s10*w10;
                                        
            
                                        final float s;
                                        if(Float.isNaN(s0))
                                            s=s1;
                                        else
                                            if(Float.isNaN(s1))
                                                s=s0;
                                            else
                                                s = (s1 - s0) * yfrac + s0;
            
                                        data[b][pixelOffset+bandOffsets[b]] = (byte)s;    
                                    }
                                }
                            }
                        }  
                    }
                    
                    // next desination pixel
                    pixelOffset += pixelStride;
                } // COLS LOOP
            } // ROWS LOOP
        } else {	// source has IndexColorModel
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1,
                              warpData);
                int count = 0;
                for (int w = 0; w < dstWidth; w++) {
                    final float sx = warpData[count++];
                    final float sy = warpData[count++];

                    final int xint = floor(sx);
                    final int yint = floor(sy);
                    final float xfrac = sx - xint;
                    final float yfrac = sy - yint;

                    if (xint < minX || xint >= maxX ||
                        yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset+bandOffsets[b]] =backgroundByte[b];
                            }
                        }
                    } else {
                        //
                        // ROI
                        //
                        if(!roiBounds.contains(xint,yint)){
                            if (setBackground) {
                                for (int b = 0; b < dstBands; b++){
                                    data[b][pixelOffset+bandOffsets[b]]=backgroundByte[b];
                                }
                            }
                        } else {    
                            // checks with roi
                            final int w00 = iterRoi.getSample(xint, yint, 0) ;
                            final int w01 = iterRoi.getSample(xint+1, yint, 0) ;
                            final int w10 = iterRoi.getSample(xint, yint+1, 0) ;
                            final int w11 = iterRoi.getSample(xint+1, yint+1, 0) ;
                            if(w00==0&&w01==0&&w10==0&&w11==0){
                                for (int b = 0; b < dstBands; b++){
                                    data[b][pixelOffset+bandOffsets[b]]=backgroundByte[b];
                                }
                            } else {
                            
                                for (int b = 0; b < dstBands; b++) {
                                    final byte[] t = ctable[b];
                                    
                                    final int s00 = t[iterSource.getSample(xint, yint, 0) & 0xFF] & 0xFF;
                                    final int s01 = t[iterSource.getSample(xint+1, yint, 0) & 0xFF] & 0xFF;
                                    final int s10 = t[iterSource.getSample(xint, yint+1, 0) & 0xFF] & 0xFF;
                                    final int s11 = t[iterSource.getSample(xint+1, yint+1, 0) & 0xFF] & 0xFF;
        
                                    // upper value
                                    final float s0;
                                    if(w00==0&&w01==0)
                                        s0=Float.NaN;
                                    else
                                        s0= (s01*w01*(w00==0?1/xfrac:1) - s00*w00*(w01==0?0:1)) * (xfrac) + s00*w00;
                                            
                                    // lower value
                                    final float s1;
                                    if(w10==0&&w11==0)
                                        s1=Float.NaN;
                                    else
                                        s1= (s11*w11*(w10==0?1/xfrac:1) - s10*w10*(w11==0?0:1)) * (xfrac) + s10*w10;
                                    
    
                                    final float s;
                                    if(Float.isNaN(s0))
                                        s=s1;
                                    else
                                        if(Float.isNaN(s1))
                                            s=s0;
                                        else
                                            s = (s1 - s0) * yfrac + s0;
                                    data[b][pixelOffset+bandOffsets[b]] = (byte)s;  
                                }
                            }
                        }
                    }
                    pixelOffset += pixelStride;
                }
            }
        }
        iterSource.done();
    }

    protected void computeRectUShort(final PlanarImage src, final RasterAccessor dst, final ROI roiTile) {
        RandomIter iter;
        if(extender != null) {
            final Rectangle bounds = new Rectangle(src.getMinX(), src.getMinY(),
                                             src.getWidth() + 1,
                                             src.getHeight() + 1);
            iter = RandomIterFactory.create(src.getExtendedData(bounds,
                                                                extender),
                                            bounds);
        } else {
            iter = RandomIterFactory.create(src, src.getBounds());
        }

        final int minX = src.getMinX();
        final int maxX = src.getMaxX() -
            (extender != null ? 0 : 1); // Right padding
        final int minY = src.getMinY();
        final int maxY = src.getMaxY() -
            (extender != null ? 0 : 1); // Bottom padding

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int lineStride = dst.getScanlineStride();
        final int pixelStride = dst.getPixelStride();
        final int[] bandOffsets = dst.getBandOffsets();
        final short[][] data = dst.getShortDataArrays();

        final float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

	final short[] backgroundUShort = new short[dstBands];
	for (int i = 0; i < dstBands; i++)
	    backgroundUShort[i] = (short)backgroundValues[i];

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1,
                          warpData);
            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                final float sx = warpData[count++];
                final float sy = warpData[count++];

                final int xint = floor(sx);
                final int yint = floor(sy);
                final float xfrac = sx - xint;
                final float yfrac = sy - yint;

                if (xint < minX || xint >= maxX ||
                    yint < minY || yint >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundUShort[b];
                        }
                    }
                } else {
                    for (int b = 0; b < dstBands; b++) {
                        final int s00 = iter.getSample(xint, yint, b) & 0xFFFF;
                        final int s01 = iter.getSample(xint+1, yint, b) & 0xFFFF;
                        final int s10 = iter.getSample(xint, yint+1, b) & 0xFFFF;
                        final int s11 = iter.getSample(xint+1, yint+1, b) & 0xFFFF;

                        final float s0 = (s01 - s00) * xfrac + s00;
                        final float s1 = (s11 - s10) * xfrac + s10;
                        final float s = (s1 - s0) * yfrac + s0;

                        data[b][pixelOffset+bandOffsets[b]] = (short)s;
                    }
                }

                pixelOffset += pixelStride;
            }
        }
        iter.done();
    }

    protected void computeRectShort(final PlanarImage src, final RasterAccessor dst, final ROI roiTile) {
        RandomIter iter;
        if(extender != null) {
            final Rectangle bounds = new Rectangle(src.getMinX(), src.getMinY(),
                                             src.getWidth() + 1,
                                             src.getHeight() + 1);
            iter = RandomIterFactory.create(src.getExtendedData(bounds,
                                                                extender),
                                            bounds);
        } else {
            iter = RandomIterFactory.create(src, src.getBounds());
        }

        final int minX = src.getMinX();
        final int maxX = src.getMaxX() -
            (extender != null ? 0 : 1); // Right padding
        final int minY = src.getMinY();
        final int maxY = src.getMaxY() -
            (extender != null ? 0 : 1); // Bottom padding

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int lineStride = dst.getScanlineStride();
        final int pixelStride = dst.getPixelStride();
        final int[] bandOffsets = dst.getBandOffsets();
        final short[][] data = dst.getShortDataArrays();

        final float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

        final short[] backgroundShort = new short[dstBands];
	for (int i = 0; i < dstBands; i++)
	    backgroundShort[i] = (short)backgroundValues[i];

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1,
                          warpData);
            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                final float sx = warpData[count++];
                final float sy = warpData[count++];

                final int xint = floor(sx);
                final int yint = floor(sy);
                final float xfrac = sx - xint;
                final float yfrac = sy - yint;

                if (xint < minX || xint >= maxX ||
                    yint < minY || yint >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundShort[b];
                        }
                    }
                } else {
                    for (int b = 0; b < dstBands; b++) {
                        final int s00 = iter.getSample(xint, yint, b);
                        final int s01 = iter.getSample(xint+1, yint, b);
                        final int s10 = iter.getSample(xint, yint+1, b);
                        final int s11 = iter.getSample(xint+1, yint+1, b);

                        final float s0 = (s01 - s00) * xfrac + s00;
                        final float s1 = (s11 - s10) * xfrac + s10;
                        final float s = (s1 - s0) * yfrac + s0;

                        data[b][pixelOffset+bandOffsets[b]] = (short)s;
                    }
                }

                pixelOffset += pixelStride;
            }
        }
        iter.done();
    }

    protected void computeRectInt(final PlanarImage src, final RasterAccessor dst, final ROI roiTile) {
        RandomIter iter;
        if(extender != null) {
            final Rectangle bounds = new Rectangle(src.getMinX(), src.getMinY(),
                                             src.getWidth() + 1,
                                             src.getHeight() + 1);
            iter = RandomIterFactory.create(src.getExtendedData(bounds,
                                                                extender),
                                            bounds);
        } else {
            iter = RandomIterFactory.create(src, src.getBounds());
        }

        final int minX = src.getMinX();
        final int maxX = src.getMaxX() -
            (extender != null ? 0 : 1); // Right padding
        final int minY = src.getMinY();
        final int maxY = src.getMaxY() -
            (extender != null ? 0 : 1); // Bottom padding

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int lineStride = dst.getScanlineStride();
        final int pixelStride = dst.getPixelStride();
        final int[] bandOffsets = dst.getBandOffsets();
        final int[][] data = dst.getIntDataArrays();

        final float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

	final int[] backgroundInt = new int[dstBands];
	for (int i = 0; i < dstBands; i++)
	    backgroundInt[i] = (int)backgroundValues[i];

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1,
                          warpData);
            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                final float sx = warpData[count++];
                final float sy = warpData[count++];

                final int xint = floor(sx);
                final int yint = floor(sy);
                final float xfrac = sx - xint;
                final float yfrac = sy - yint;

                if (xint < minX || xint >= maxX ||
                    yint < minY || yint >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundInt[b];
                        }
                    }
                } else {
                    for (int b = 0; b < dstBands; b++) {
                        final int s00 = iter.getSample(xint, yint, b);
                        final int s01 = iter.getSample(xint+1, yint, b);
                        final int s10 = iter.getSample(xint, yint+1, b);
                        final int s11 = iter.getSample(xint+1, yint+1, b);

                        final float s0 = (s01 - s00) * xfrac + s00;
                        final float s1 = (s11 - s10) * xfrac + s10;
                        final float s = (s1 - s0) * yfrac + s0;

                        data[b][pixelOffset+bandOffsets[b]] = (int)s;
                    }
                }

                pixelOffset += pixelStride;
            }
        }
        iter.done();
    }

    protected void computeRectFloat(final PlanarImage src, final RasterAccessor dst, final ROI roiTile) {
        RandomIter iter;
        if(extender != null) {
            final Rectangle bounds = new Rectangle(src.getMinX(), src.getMinY(),
                                             src.getWidth() + 1,
                                             src.getHeight() + 1);
            iter = RandomIterFactory.create(src.getExtendedData(bounds,
                                                                extender),
                                            bounds);
        } else {
            iter = RandomIterFactory.create(src, src.getBounds());
        }

        final int minX = src.getMinX();
        final int maxX = src.getMaxX() -
            (extender != null ? 0 : 1); // Right padding
        final int minY = src.getMinY();
        final int maxY = src.getMaxY() -
            (extender != null ? 0 : 1); // Bottom padding

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int lineStride = dst.getScanlineStride();
        final int pixelStride = dst.getPixelStride();
        final int[] bandOffsets = dst.getBandOffsets();
        final float[][] data = dst.getFloatDataArrays();

        final float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

	final float[] backgroundFloat = new float[dstBands];
	for (int i = 0; i < dstBands; i++)
	    backgroundFloat[i] = (float)backgroundValues[i];

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1,
                          warpData);
            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                final float sx = warpData[count++];
                final float sy = warpData[count++];

                final int xint = floor(sx);
                final int yint = floor(sy);
                final float xfrac = sx - xint;
                final float yfrac = sy - yint;

                if (xint < minX || xint >= maxX ||
                    yint < minY || yint >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundFloat[b];
                        }
                    }
                } else {
                    for (int b = 0; b < dstBands; b++) {
                        final float s00 = iter.getSampleFloat(xint, yint, b);
                        final float s01 = iter.getSampleFloat(xint+1, yint, b);
                        final float s10 = iter.getSampleFloat(xint, yint+1, b);
                        final float s11 = iter.getSampleFloat(xint+1, yint+1, b);

                        final float s0 = (s01 - s00) * xfrac + s00;
                        final float s1 = (s11 - s10) * xfrac + s10;
                        final float s = (s1 - s0) * yfrac + s0;

                        data[b][pixelOffset+bandOffsets[b]] = s;
                    }
                }

                pixelOffset += pixelStride;
            }
        }
        iter.done();
    }

    protected void computeRectDouble(final PlanarImage src, final RasterAccessor dst, final ROI roiTile) {
        RandomIter iter;
        if(extender != null) {
            final Rectangle bounds = new Rectangle(src.getMinX(), src.getMinY(),
                                             src.getWidth() + 1,
                                             src.getHeight() + 1);
            iter = RandomIterFactory.create(src.getExtendedData(bounds,
                                                                extender),
                                            bounds);
        } else {
            iter = RandomIterFactory.create(src, src.getBounds());
        }

        final int minX = src.getMinX();
        final int maxX = src.getMaxX() -
            (extender != null ? 0 : 1); // Right padding
        final int minY = src.getMinY();
        final int maxY = src.getMaxY() -
            (extender != null ? 0 : 1); // Bottom padding

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        final int lineStride = dst.getScanlineStride();
        final int pixelStride = dst.getPixelStride();
        final int[] bandOffsets = dst.getBandOffsets();
        final double[][] data = dst.getDoubleDataArrays();

        final float[] warpData = new float[2 * dstWidth];

        int lineOffset = 0;

        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            warp.warpRect(dst.getX(), dst.getY()+h, dstWidth, 1,
                          warpData);
            int count = 0;
            for (int w = 0; w < dstWidth; w++) {
                final float sx = warpData[count++];
                final float sy = warpData[count++];

                final int xint = floor(sx);
                final int yint = floor(sy);
                final float xfrac = sx - xint;
                final float yfrac = sy - yint;

                if (xint < minX || xint >= maxX ||
                    yint < minY || yint >= maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset+bandOffsets[b]] =
                                backgroundValues[b];
                        }
                    }
                } else {
                    for (int b = 0; b < dstBands; b++) {
                        final double s00 = iter.getSampleDouble(xint, yint, b);
                        final double s01 = iter.getSampleDouble(xint+1, yint, b);
                        final double s10 = iter.getSampleDouble(xint, yint+1, b);
                        final double s11 = iter.getSampleDouble(xint+1, yint+1, b);

                        final double s0 = (s01 - s00) * xfrac + s00;
                        final double s1 = (s11 - s10) * xfrac + s10;
                        final double s = (s1 - s0) * yfrac + s0;

                        data[b][pixelOffset+bandOffsets[b]] = s;
                    }
                }

                pixelOffset += pixelStride;
            }
        }
        iter.done();
    }

    /** Returns the "floor" value of a float. */
    private static final int floor(final float f) {
        return f >= 0 ? (int)f : (int)f - 1;
    }
}
