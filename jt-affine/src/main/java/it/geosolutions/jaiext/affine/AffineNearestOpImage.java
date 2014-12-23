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

import it.geosolutions.jaiext.interpolators.InterpolationNearest;
import it.geosolutions.jaiext.range.Range;

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
import java.util.Map;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;

public class AffineNearestOpImage extends AffineOpImage {

    /** Nearest-Neighbor interpolator */
    protected InterpolationNearest interpN = null;
    /**Byte lookuptable used if no data are present*/
    protected final byte[] byteLookupTable = new byte[256];

    /** ROI extender */
    final static BorderExtender roiExtender = BorderExtender
            .createInstance(BorderExtender.BORDER_ZERO);

    /** Value indicating if destination No Data must be set if the pixel is outside the source rectangle */
    private boolean setDestinationNoData;

    public AffineNearestOpImage(RenderedImage source, BorderExtender extender, Map config,
            ImageLayout layout, AffineTransform transform, Interpolation interp,boolean setDestinationNoData,
            boolean useROIAccessor, Range nodata) {
        super(source, extender, config, layout, transform, interp, null);
        affineOpInitialization(source, interp, layout,useROIAccessor, setDestinationNoData);
    }

    private void affineOpInitialization(RenderedImage source, Interpolation interp,
            ImageLayout layout, boolean useROIAccessor, boolean setDestinationNoData) {

        SampleModel sm = source.getSampleModel();

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

        // Source image data Type
        int srcDataType = sm.getDataType();

        // If both roiBounds and roiIter are not null, they are used in calculation
        if (interp instanceof InterpolationNearest) {
            interpN = (InterpolationNearest) interp;
            this.interp = interpN;
            interpN.setROIdata(roiBounds, roiIter);
            noData = interpN.getNoDataRange();
            this.useROIAccessor = false;
            if (noData != null) {
                hasNoData = true;
                destinationNoDataDouble = interpN.getDestinationNoData();               
            } else if (hasROI) {
                destinationNoDataDouble = interpN.getDestinationNoData();
                this.useROIAccessor = useROIAccessor;
            }
        }

        //Creation of the destination background values
        int srcNumBands= source.getSampleModel().getNumBands();
        double[] background=new double[srcNumBands];
        for(int i = 0; i<srcNumBands;i++){
            background[i]=destinationNoDataDouble;
        }       
        this.backgroundValues=background;
        
        // destination No Data set
        this.setDestinationNoData = setDestinationNoData;
        this.setBackground=setDestinationNoData;
        // Selection of the destination No Data
        switch (srcDataType) {
        case DataBuffer.TYPE_BYTE:
            destinationNoDataByte = (byte) (((byte) destinationNoDataDouble) & 0xff);
            // Creation of a lookuptable containing the values to use for no data
            if (hasNoData) {

                for (int i = 0; i < byteLookupTable.length; i++) {
                    byte value = (byte) i;
                    if (noData.contains(value)) {
                        if(setDestinationNoData){
                            byteLookupTable[i] = destinationNoDataByte;
                        }else{
                            byteLookupTable[i]=0;
                        }
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
        computeRect(sources, dest, destRect, null);
    }

    /** Method for evaluating the destination image tile with ROI */
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect,
            Raster[] rois) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();
        // Source image
        Raster source = sources[0];
        // Source rectangle
        Rectangle srcRect = source.getBounds();
        // Src upper left pixel coordinates
        int srcRectX = srcRect.x;
        int srcRectY = srcRect.y;

        //
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

        // ROI calculation only if the roi raster is present
        if (useROIAccessor) {
            // Selection of the roi raster
            roi = rois[0];
            // creation of the rasterAccessor
            roiAccessor = new RasterAccessor(roi, srcRect, RasterAccessor.findCompatibleTags(
                    new RenderedImage[] { srcROIImage }, srcROIImage)[0],
                    srcROIImage.getColorModel());
        }

        int dataType = dest.getSampleModel().getDataType();
        // If the image is not binary, then for every kind of dataType, the image affine transformation
        // is performed.

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(dataType, srcAccessor, destRect, srcRectX, srcRectY, dstAccessor, roiAccessor);
            break;
        case DataBuffer.TYPE_INT:
            intLoop(dataType, srcAccessor, destRect, srcRectX, srcRectY, dstAccessor, roiAccessor);
            break;
        case DataBuffer.TYPE_SHORT:
            shortLoop(dataType, srcAccessor, destRect, srcRectX, srcRectY, dstAccessor, roiAccessor);
            break;
        case DataBuffer.TYPE_USHORT:
            ushortLoop(dataType, srcAccessor, destRect, srcRectX, srcRectY, dstAccessor,
                    roiAccessor);
            break;
        case DataBuffer.TYPE_FLOAT:
            floatLoop(dataType, srcAccessor, destRect, srcRectX, srcRectY, dstAccessor, roiAccessor);
            break;
        case DataBuffer.TYPE_DOUBLE:
            doubleLoop(dataType, srcAccessor, destRect, srcRectX, srcRectY, dstAccessor,
                    roiAccessor);
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
            int srcRectY, RasterAccessor dst, RasterAccessor roi) {

        final float src_rect_x1 = src.getX();
        final float src_rect_y1 = src.getY();
        final float src_rect_x2 = src_rect_x1 + src.getWidth();
        final float src_rect_y2 = src_rect_y1 + src.getHeight();

        float s_x, s_y;

        int src_pos;

        double fracx, fracy;

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

        final int incxStride = incx * srcPixelStride;
        final int incx1Stride = incx1 * srcPixelStride;
        final int incyStride = incy * srcScanlineStride;
        final int incy1Stride = incy1 * srcScanlineStride;

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
                dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                s_x = (float) src_pt.getX();
                s_y = (float) src_pt.getY();

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix*1.0d;
                fracy = s_y - s_iy*1.0d;

                int ifracx = (int) Math.floor(fracx * geom_frac_max);
                int ifracy = (int) Math.floor(fracy * geom_frac_max);

                // Compute clipMinX, clipMinY
                javax.media.jai.util.Range clipRange = performScanlineClipping(
                        src_rect_x1,
                        src_rect_y1,
                        // Last point in the source is
                        // x2 = x1 + width - 1
                        // y2 = y1 + height - 1
                        src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy, dst_min_x,
                        dst_max_x, 0, 0, 0, 0);
                int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                // Advance s_ix, s_iy, ifracx, ifracy
                Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                        ifracx, ifracy);
                s_ix = startPts[0].x;
                s_iy = startPts[0].y;
                ifracx = startPts[1].x;
                ifracy = startPts[1].y;

                // Translate to/from SampleModel space & Raster space
                src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX)
                        * srcPixelStride;

                if (setDestinationNoData) {
                    for (int x = dst_min_x; x < clipMinX; x++) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++)
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                        dstPixelOffset += dstPixelStride;
                    }
                } else{
                 // Advance to first pixel
                    dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;
                }
                    

                for (int x = clipMinX; x < clipMaxX; x++) {
                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = srcDataArrays[k2][src_pos
                                + bandOffsets[k2]];
                    }
                    // walk
                    if (ifracx < ifracdx1) {
                        src_pos += incxStride;
                        ifracx += ifracdx;
                    } else {
                        src_pos += incx1Stride;
                        ifracx -= ifracdx1;
                    }

                    if (ifracy < ifracdy1) {
                        src_pos += incyStride;
                        ifracy += ifracdy;
                    } else {
                        src_pos += incy1Stride;
                        ifracy -= ifracdy1;
                    }

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }

                if (setDestinationNoData && clipMinX <= clipMaxX) {
                    for (int x = clipMaxX; x < dst_max_x; x++) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++)
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                        dstPixelOffset += dstPixelStride;
                    }
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
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    s_x = (float) src_pt.getX();
                    s_y = (float) src_pt.getY();

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix*1.0d;
                    fracy = s_y - s_iy*1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx,
                            ifracy, dst_min_x, dst_max_x, 0, 0, 0, 0);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;
                    ifracx = startPts[1].x;
                    ifracy = startPts[1].y;

                    // Translate to/from SampleModel space & Raster space
                    src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX) * srcPixelStride;

                    

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // Translate to/from SampleModel space & Raster space
                        int posy = (s_iy - srcRectY) * srcScanlineStride;
                        int posx = (s_ix - srcRectX) * srcPixelStride;
                        // If roiAccessor is present, the y position on the roi image is calculated
                        int posyROI = (s_iy - srcRectY) * roiScanlineStride;
                        
                        src_pos = posx+posy;
                        
                        int windex = (posx / dst_num_bands) + posyROI;

                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                        if (w == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                // The interpolated value is saved in the destination array
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = srcDataArrays[k2][src_pos
                                        + bandOffsets[k2]];
                            }
                        }
                        // walk
                        if (ifracx < ifracdx1) {
                            s_ix += incx;
                            ifracx += ifracdx;
                        } else {
                            s_ix += incx1;
                            ifracx -= ifracdx1;
                        }
                        if (ifracy < ifracdy1) {
                            s_iy += incy;
                            ifracy += ifracdy;
                        } else {
                            s_iy += incy1;
                            ifracy -= ifracdy1;
                        }
                        
                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
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
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    s_x = (float) src_pt.getX();
                    s_y = (float) src_pt.getY();

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix*1.0d;
                    fracy = s_y - s_iy*1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(
                            src_rect_x1,
                            src_rect_y1,
                            // Last point in the source is
                            // x2 = x1 + width - 1
                            // y2 = y1 + height - 1
                            src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 0, 0, 0);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;
                    ifracx = startPts[1].x;
                    ifracy = startPts[1].y;

                    // Translate to/from SampleModel space & Raster space
                    src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX) * srcPixelStride;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // Translate to/from SampleModel space & Raster space
                        int posy = (s_iy - srcRectY) * srcScanlineStride;
                        int posx = (s_ix - srcRectX) * srcPixelStride;
                        
                        src_pos = posx+posy;

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (roiBounds.contains(x0, y0)) {

                            int w = roiIter.getSample(x0, y0, 0) & 0xff;
                            if (w == 0) {
                                if (setDestinationNoData) {
                                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                                    }
                                }
                            } else {
                                // The interpolated value is saved in the destination array
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = srcDataArrays[k2][src_pos
                                            + bandOffsets[k2]];
                                }
                            }
                        } else if (setDestinationNoData) {
                            // The destination no data value is saved in the destination array
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                            }
                        }

                        // walk
                        if (ifracx < ifracdx1) {
                            s_ix += incx;
                            ifracx += ifracdx;
                        } else {
                            s_ix += incx1;
                            ifracx -= ifracdx1;
                        }
                        if (ifracy < ifracdy1) {
                            s_iy += incy;
                            ifracy += ifracdy;
                        } else {
                            s_iy += incy1;
                            ifracy -= ifracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        } else if (caseC) {
            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                s_x = (float) src_pt.getX();
                s_y = (float) src_pt.getY();

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix*1.0d;
                fracy = s_y - s_iy*1.0d;

                int ifracx = (int) Math.floor(fracx * geom_frac_max);
                int ifracy = (int) Math.floor(fracy * geom_frac_max);

                // Compute clipMinX, clipMinY
                javax.media.jai.util.Range clipRange = performScanlineClipping(
                        src_rect_x1,
                        src_rect_y1,
                        // Last point in the source is
                        // x2 = x1 + width - 1
                        // y2 = y1 + height - 1
                        src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy, dst_min_x,
                        dst_max_x, 0, 0, 0, 0);
                int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                // Advance s_ix, s_iy, ifracx, ifracy
                Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                        ifracx, ifracy);
                s_ix = startPts[0].x;
                s_iy = startPts[0].y;
                ifracx = startPts[1].x;
                ifracy = startPts[1].y;

                // Translate to/from SampleModel space & Raster space
                int posy = (s_iy - srcRectY) * srcScanlineStride;
                src_pos = posy + (s_ix - srcRectX) * srcPixelStride;

                if (setDestinationNoData) {
                    for (int x = dst_min_x; x < clipMinX; x++) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++)
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                        dstPixelOffset += dstPixelStride;
                    }
                } else
                    // Advance to first pixel
                    dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                for (int x = clipMinX; x < clipMaxX; x++) {
                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                        int value = srcDataArrays[k2][src_pos + bandOffsets[k2]];
                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = byteLookupTable[value&0xFF];
                    }

                    // walk
                    if (ifracx < ifracdx1) {
                        src_pos += incxStride;
                        ifracx += ifracdx;
                    } else {
                        src_pos += incx1Stride;
                        ifracx -= ifracdx1;
                    }

                    if (ifracy < ifracdy1) {
                        src_pos += incyStride;
                        ifracy += ifracdy;
                    } else {
                        src_pos += incy1Stride;
                        ifracy -= ifracdy1;
                    }

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }

                if (setDestinationNoData && clipMinX <= clipMaxX) {
                    for (int x = clipMaxX; x < dst_max_x; x++) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++)
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                        dstPixelOffset += dstPixelStride;
                    }
                }
                // Go to the next line in the destination rectangle
                dstOffset += dstScanlineStride;
            }
        } else {
            if (useROIAccessor) {

                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    s_x = (float) src_pt.getX();
                    s_y = (float) src_pt.getY();

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix*1.0d;
                    fracy = s_y - s_iy*1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(
                            src_rect_x1,
                            src_rect_y1,
                            // Last point in the source is
                            // x2 = x1 + width - 1
                            // y2 = y1 + height - 1
                            src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 0, 0, 0);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;
                    ifracx = startPts[1].x;
                    ifracy = startPts[1].y;

                    // Translate to/from SampleModel space & Raster space
                    src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX) * srcPixelStride;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {
                        // Translate to/from SampleModel space & Raster space
                        int posy = (s_iy - srcRectY) * srcScanlineStride;
                        int posx = (s_ix - srcRectX) * srcPixelStride;
                        // If roiAccessor is present, the y position on the roi image is calculated
                        int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        src_pos = posx+posy;
                        
                        int windex = (posx / dst_num_bands) + posyROI;

                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                        if (w == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                // The interpolated value is saved in the destination array
                                int value = srcDataArrays[k2][src_pos + bandOffsets[k2]];
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = byteLookupTable[value&0xFF];
                            }
                        }

                     // walk
                        if (ifracx < ifracdx1) {
                            s_ix += incx;
                            ifracx += ifracdx;
                        } else {
                            s_ix += incx1;
                            ifracx -= ifracdx1;
                        }
                        if (ifracy < ifracdy1) {
                            s_iy += incy;
                            ifracy += ifracdy;
                        } else {
                            s_iy += incy1;
                            ifracy -= ifracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
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
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    s_x = (float) src_pt.getX();
                    s_y = (float) src_pt.getY();

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix*1.0d;
                    fracy = s_y - s_iy*1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(
                            src_rect_x1,
                            src_rect_y1,
                            // Last point in the source is
                            // x2 = x1 + width - 1
                            // y2 = y1 + height - 1
                            src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 0, 0, 0);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;
                    ifracx = startPts[1].x;
                    ifracy = startPts[1].y;

                    // Translate to/from SampleModel space & Raster space
                    src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX) * srcPixelStride;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // Translate to/from SampleModel space & Raster space
                        int posy = (s_iy - srcRectY) * srcScanlineStride;
                        int posx = (s_ix - srcRectX) * srcPixelStride;

                        src_pos = posx+posy;

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (roiBounds.contains(x0, y0)) {

                            int w = roiIter.getSample(x0, y0, 0) & 0xff;
                            if (w == 0) {
                                if (setDestinationNoData) {
                                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                                    }
                                }
                            } else {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    // The interpolated value is saved in the destination array
                                    int value = srcDataArrays[k2][src_pos + bandOffsets[k2]];
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = byteLookupTable[value&0xFF];
                                }
                            }
                        } else if (setDestinationNoData) {
                            // The destination no data value is saved in the destination array
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                            }
                        }
                        // walk
                        if (ifracx < ifracdx1) {
                            s_ix += incx;
                            ifracx += ifracdx;
                        } else {
                            s_ix += incx1;
                            ifracx -= ifracdx1;
                        }
                        if (ifracy < ifracdy1) {
                            s_iy += incy;
                            ifracy += ifracdy;
                        } else {
                            s_iy += incy1;
                            ifracy -= ifracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataByte;
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        }
    }

    private void intLoop(int dataType, RasterAccessor src, Rectangle destRect,
            int srcRectX, int srcRectY, RasterAccessor dst, RasterAccessor roi) {


        final float src_rect_x1 = src.getX();
        final float src_rect_y1 = src.getY();
        final float src_rect_x2 = src_rect_x1 + src.getWidth();
        final float src_rect_y2 = src_rect_y1 + src.getHeight();

        float s_x, s_y;

        int src_pos;

        double fracx, fracy;

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

        final int incxStride = incx * srcPixelStride;
        final int incx1Stride = incx1 * srcPixelStride;
        final int incyStride = incy * srcScanlineStride;
        final int incy1Stride = incy1 * srcScanlineStride;

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
                dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                s_x = (float) src_pt.getX();
                s_y = (float) src_pt.getY();

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix*1.0d;
                fracy = s_y - s_iy*1.0d;

                int ifracx = (int) Math.floor(fracx * geom_frac_max);
                int ifracy = (int) Math.floor(fracy * geom_frac_max);

                // Compute clipMinX, clipMinY
                javax.media.jai.util.Range clipRange = performScanlineClipping(
                        src_rect_x1,
                        src_rect_y1,
                        // Last point in the source is
                        // x2 = x1 + width - 1
                        // y2 = y1 + height - 1
                        src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy, dst_min_x,
                        dst_max_x, 0, 0, 0, 0);
                int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                // Advance s_ix, s_iy, ifracx, ifracy
                Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                        ifracx, ifracy);
                s_ix = startPts[0].x;
                s_iy = startPts[0].y;
                ifracx = startPts[1].x;
                ifracy = startPts[1].y;

                // Translate to/from SampleModel space & Raster space
                src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX)
                        * srcPixelStride;

                if (setDestinationNoData) {
                    for (int x = dst_min_x; x < clipMinX; x++) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++)
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                        dstPixelOffset += dstPixelStride;
                    }
                } else{
                 // Advance to first pixel
                    dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;
                }
                    

                for (int x = clipMinX; x < clipMaxX; x++) {
                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = srcDataArrays[k2][src_pos
                                + bandOffsets[k2]];
                    }
                    // walk
                    if (ifracx < ifracdx1) {
                        src_pos += incxStride;
                        ifracx += ifracdx;
                    } else {
                        src_pos += incx1Stride;
                        ifracx -= ifracdx1;
                    }

                    if (ifracy < ifracdy1) {
                        src_pos += incyStride;
                        ifracy += ifracdy;
                    } else {
                        src_pos += incy1Stride;
                        ifracy -= ifracdy1;
                    }

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }

                if (setDestinationNoData && clipMinX <= clipMaxX) {
                    for (int x = clipMaxX; x < dst_max_x; x++) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++)
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                        dstPixelOffset += dstPixelStride;
                    }
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
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    s_x = (float) src_pt.getX();
                    s_y = (float) src_pt.getY();

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix*1.0d;
                    fracy = s_y - s_iy*1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx,
                            ifracy, dst_min_x, dst_max_x, 0, 0, 0, 0);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;
                    ifracx = startPts[1].x;
                    ifracy = startPts[1].y;

                    // Translate to/from SampleModel space & Raster space
                    src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX) * srcPixelStride;

                    

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // Translate to/from SampleModel space & Raster space
                        int posy = (s_iy - srcRectY) * srcScanlineStride;
                        int posx = (s_ix - srcRectX) * srcPixelStride;
                        // If roiAccessor is present, the y position on the roi image is calculated
                        int posyROI = (s_iy - srcRectY) * roiScanlineStride;
                        
                        src_pos = posx+posy;
                        
                        int windex = (posx / dst_num_bands) + posyROI;

                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                        if (w == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                // The interpolated value is saved in the destination array
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = srcDataArrays[k2][src_pos
                                        + bandOffsets[k2]];
                            }
                        }
                        // walk
                        if (ifracx < ifracdx1) {
                            s_ix += incx;
                            ifracx += ifracdx;
                        } else {
                            s_ix += incx1;
                            ifracx -= ifracdx1;
                        }
                        if (ifracy < ifracdy1) {
                            s_iy += incy;
                            ifracy += ifracdy;
                        } else {
                            s_iy += incy1;
                            ifracy -= ifracdy1;
                        }
                        
                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
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
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    s_x = (float) src_pt.getX();
                    s_y = (float) src_pt.getY();

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix*1.0d;
                    fracy = s_y - s_iy*1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(
                            src_rect_x1,
                            src_rect_y1,
                            // Last point in the source is
                            // x2 = x1 + width - 1
                            // y2 = y1 + height - 1
                            src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 0, 0, 0);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;
                    ifracx = startPts[1].x;
                    ifracy = startPts[1].y;

                    // Translate to/from SampleModel space & Raster space
                    src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX) * srcPixelStride;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // Translate to/from SampleModel space & Raster space
                        int posy = (s_iy - srcRectY) * srcScanlineStride;
                        int posx = (s_ix - srcRectX) * srcPixelStride;
                        
                        src_pos = posx+posy;

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (roiBounds.contains(x0, y0)) {

                            int w = roiIter.getSample(x0, y0, 0) & 0xff;
                            if (w == 0) {
                                if (setDestinationNoData) {
                                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                                    }
                                }
                            } else {
                                // The interpolated value is saved in the destination array
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = srcDataArrays[k2][src_pos
                                            + bandOffsets[k2]];
                                }
                            }
                        } else if (setDestinationNoData) {
                            // The destination no data value is saved in the destination array
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                            }
                        }

                        // walk
                        if (ifracx < ifracdx1) {
                            s_ix += incx;
                            ifracx += ifracdx;
                        } else {
                            s_ix += incx1;
                            ifracx -= ifracdx1;
                        }
                        if (ifracy < ifracdy1) {
                            s_iy += incy;
                            ifracy += ifracdy;
                        } else {
                            s_iy += incy1;
                            ifracy -= ifracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        } else if (caseC) {
            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                s_x = (float) src_pt.getX();
                s_y = (float) src_pt.getY();

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix*1.0d;
                fracy = s_y - s_iy*1.0d;

                int ifracx = (int) Math.floor(fracx * geom_frac_max);
                int ifracy = (int) Math.floor(fracy * geom_frac_max);

                // Compute clipMinX, clipMinY
                javax.media.jai.util.Range clipRange = performScanlineClipping(
                        src_rect_x1,
                        src_rect_y1,
                        // Last point in the source is
                        // x2 = x1 + width - 1
                        // y2 = y1 + height - 1
                        src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy, dst_min_x,
                        dst_max_x, 0, 0, 0, 0);
                int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                // Advance s_ix, s_iy, ifracx, ifracy
                Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                        ifracx, ifracy);
                s_ix = startPts[0].x;
                s_iy = startPts[0].y;
                ifracx = startPts[1].x;
                ifracy = startPts[1].y;

                // Translate to/from SampleModel space & Raster space
                int posy = (s_iy - srcRectY) * srcScanlineStride;
                src_pos = posy + (s_ix - srcRectX) * srcPixelStride;

                if (setDestinationNoData) {
                    for (int x = dst_min_x; x < clipMinX; x++) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++)
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                        dstPixelOffset += dstPixelStride;
                    }
                } else
                    // Advance to first pixel
                    dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                for (int x = clipMinX; x < clipMaxX; x++) {
                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                        int value = srcDataArrays[k2][src_pos + bandOffsets[k2]];
                        if(noData.contains(value)){
                            if(setDestinationNoData){
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                            }
                        }else{
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] =value;
                        }
                        
                    }

                    // walk
                    if (ifracx < ifracdx1) {
                        src_pos += incxStride;
                        ifracx += ifracdx;
                    } else {
                        src_pos += incx1Stride;
                        ifracx -= ifracdx1;
                    }

                    if (ifracy < ifracdy1) {
                        src_pos += incyStride;
                        ifracy += ifracdy;
                    } else {
                        src_pos += incy1Stride;
                        ifracy -= ifracdy1;
                    }

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }

                if (setDestinationNoData && clipMinX <= clipMaxX) {
                    for (int x = clipMaxX; x < dst_max_x; x++) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++)
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                        dstPixelOffset += dstPixelStride;
                    }
                }
                // Go to the next line in the destination rectangle
                dstOffset += dstScanlineStride;
            }
        } else {
            if (useROIAccessor) {

                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    s_x = (float) src_pt.getX();
                    s_y = (float) src_pt.getY();

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix*1.0d;
                    fracy = s_y - s_iy*1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(
                            src_rect_x1,
                            src_rect_y1,
                            src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 0, 0, 0);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;
                    ifracx = startPts[1].x;
                    ifracy = startPts[1].y;

                    // Translate to/from SampleModel space & Raster space
                    src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX) * srcPixelStride;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {
                        // Translate to/from SampleModel space & Raster space
                        int posy = (s_iy - srcRectY) * srcScanlineStride;
                        int posx = (s_ix - srcRectX) * srcPixelStride;
                        // If roiAccessor is present, the y position on the roi image is calculated
                        int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        src_pos = posx+posy;
                        
                        int windex = (posx / dst_num_bands) + posyROI;

                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                        if (w == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                // The interpolated value is saved in the destination array
                                int value = srcDataArrays[k2][src_pos + bandOffsets[k2]];
                                if(noData.contains(value)){
                                    if(setDestinationNoData){
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                                    }
                                }else{
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] =value;
                                }
                            }
                        }

                     // walk
                        if (ifracx < ifracdx1) {
                            s_ix += incx;
                            ifracx += ifracdx;
                        } else {
                            s_ix += incx1;
                            ifracx -= ifracdx1;
                        }
                        if (ifracy < ifracdy1) {
                            s_iy += incy;
                            ifracy += ifracdy;
                        } else {
                            s_iy += incy1;
                            ifracy -= ifracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
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
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    s_x = (float) src_pt.getX();
                    s_y = (float) src_pt.getY();

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix*1.0d;
                    fracy = s_y - s_iy*1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(
                            src_rect_x1,
                            src_rect_y1,
                            // Last point in the source is
                            // x2 = x1 + width - 1
                            // y2 = y1 + height - 1
                            src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 0, 0, 0);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;
                    ifracx = startPts[1].x;
                    ifracy = startPts[1].y;

                    // Translate to/from SampleModel space & Raster space
                    src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX) * srcPixelStride;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // Translate to/from SampleModel space & Raster space
                        int posy = (s_iy - srcRectY) * srcScanlineStride;
                        int posx = (s_ix - srcRectX) * srcPixelStride;

                        src_pos = posx+posy;

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (roiBounds.contains(x0, y0)) {

                            int w = roiIter.getSample(x0, y0, 0) & 0xff;
                            if (w == 0) {
                                if (setDestinationNoData) {
                                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                                    }
                                }
                            } else {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    // The interpolated value is saved in the destination array
                                    int value = srcDataArrays[k2][src_pos + bandOffsets[k2]];
                                    if(noData.contains(value)){
                                        if(setDestinationNoData){
                                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                                        }
                                    }else{
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] =value;
                                    }
                                }
                            }
                        } else if (setDestinationNoData) {
                            // The destination no data value is saved in the destination array
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                            }
                        }
                        // walk
                        if (ifracx < ifracdx1) {
                            s_ix += incx;
                            ifracx += ifracdx;
                        } else {
                            s_ix += incx1;
                            ifracx -= ifracdx1;
                        }
                        if (ifracy < ifracdy1) {
                            s_iy += incy;
                            ifracy += ifracdy;
                        } else {
                            s_iy += incy1;
                            ifracy -= ifracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataInt;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        }
    }

    private void shortLoop(int dataType, RasterAccessor src, Rectangle destRect,
            int srcRectX, int srcRectY, RasterAccessor dst, RasterAccessor roi) {


        final float src_rect_x1 = src.getX();
        final float src_rect_y1 = src.getY();
        final float src_rect_x2 = src_rect_x1 + src.getWidth();
        final float src_rect_y2 = src_rect_y1 + src.getHeight();

        float s_x, s_y;

        int src_pos;

        double fracx, fracy;

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

        final int incxStride = incx * srcPixelStride;
        final int incx1Stride = incx1 * srcPixelStride;
        final int incyStride = incy * srcScanlineStride;
        final int incy1Stride = incy1 * srcScanlineStride;

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
                dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                s_x = (float) src_pt.getX();
                s_y = (float) src_pt.getY();

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix*1.0d;
                fracy = s_y - s_iy*1.0d;

                int ifracx = (int) Math.floor(fracx * geom_frac_max);
                int ifracy = (int) Math.floor(fracy * geom_frac_max);

                // Compute clipMinX, clipMinY
                javax.media.jai.util.Range clipRange = performScanlineClipping(
                        src_rect_x1,
                        src_rect_y1,
                        // Last point in the source is
                        // x2 = x1 + width - 1
                        // y2 = y1 + height - 1
                        src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy, dst_min_x,
                        dst_max_x, 0, 0, 0, 0);
                int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                // Advance s_ix, s_iy, ifracx, ifracy
                Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                        ifracx, ifracy);
                s_ix = startPts[0].x;
                s_iy = startPts[0].y;
                ifracx = startPts[1].x;
                ifracy = startPts[1].y;

                // Translate to/from SampleModel space & Raster space
                src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX)
                        * srcPixelStride;

                if (setDestinationNoData) {
                    for (int x = dst_min_x; x < clipMinX; x++) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++)
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                        dstPixelOffset += dstPixelStride;
                    }
                } else{
                 // Advance to first pixel
                    dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;
                }
                    

                for (int x = clipMinX; x < clipMaxX; x++) {
                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = srcDataArrays[k2][src_pos
                                + bandOffsets[k2]];
                    }
                    // walk
                    if (ifracx < ifracdx1) {
                        src_pos += incxStride;
                        ifracx += ifracdx;
                    } else {
                        src_pos += incx1Stride;
                        ifracx -= ifracdx1;
                    }

                    if (ifracy < ifracdy1) {
                        src_pos += incyStride;
                        ifracy += ifracdy;
                    } else {
                        src_pos += incy1Stride;
                        ifracy -= ifracdy1;
                    }

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }

                if (setDestinationNoData && clipMinX <= clipMaxX) {
                    for (int x = clipMaxX; x < dst_max_x; x++) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++)
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                        dstPixelOffset += dstPixelStride;
                    }
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
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    s_x = (float) src_pt.getX();
                    s_y = (float) src_pt.getY();

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix*1.0d;
                    fracy = s_y - s_iy*1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx,
                            ifracy, dst_min_x, dst_max_x, 0, 0, 0, 0);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;
                    ifracx = startPts[1].x;
                    ifracy = startPts[1].y;

                    // Translate to/from SampleModel space & Raster space
                    src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX) * srcPixelStride;

                    

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // Translate to/from SampleModel space & Raster space
                        int posy = (s_iy - srcRectY) * srcScanlineStride;
                        int posx = (s_ix - srcRectX) * srcPixelStride;
                        // If roiAccessor is present, the y position on the roi image is calculated
                        int posyROI = (s_iy - srcRectY) * roiScanlineStride;
                        
                        src_pos = posx+posy;
                        
                        int windex = (posx / dst_num_bands) + posyROI;

                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                        if (w == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                // The interpolated value is saved in the destination array
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = srcDataArrays[k2][src_pos
                                        + bandOffsets[k2]];
                            }
                        }
                        // walk
                        if (ifracx < ifracdx1) {
                            s_ix += incx;
                            ifracx += ifracdx;
                        } else {
                            s_ix += incx1;
                            ifracx -= ifracdx1;
                        }
                        if (ifracy < ifracdy1) {
                            s_iy += incy;
                            ifracy += ifracdy;
                        } else {
                            s_iy += incy1;
                            ifracy -= ifracdy1;
                        }
                        
                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
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
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    s_x = (float) src_pt.getX();
                    s_y = (float) src_pt.getY();

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix*1.0d;
                    fracy = s_y - s_iy*1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(
                            src_rect_x1,
                            src_rect_y1,
                            // Last point in the source is
                            // x2 = x1 + width - 1
                            // y2 = y1 + height - 1
                            src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 0, 0, 0);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;
                    ifracx = startPts[1].x;
                    ifracy = startPts[1].y;

                    // Translate to/from SampleModel space & Raster space
                    src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX) * srcPixelStride;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // Translate to/from SampleModel space & Raster space
                        int posy = (s_iy - srcRectY) * srcScanlineStride;
                        int posx = (s_ix - srcRectX) * srcPixelStride;
                        
                        src_pos = posx+posy;

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (roiBounds.contains(x0, y0)) {

                            int w = roiIter.getSample(x0, y0, 0) & 0xff;
                            if (w == 0) {
                                if (setDestinationNoData) {
                                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                                    }
                                }
                            } else {
                                // The interpolated value is saved in the destination array
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = srcDataArrays[k2][src_pos
                                            + bandOffsets[k2]];
                                }
                            }
                        } else if (setDestinationNoData) {
                            // The destination no data value is saved in the destination array
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                            }
                        }

                        // walk
                        if (ifracx < ifracdx1) {
                            s_ix += incx;
                            ifracx += ifracdx;
                        } else {
                            s_ix += incx1;
                            ifracx -= ifracdx1;
                        }
                        if (ifracy < ifracdy1) {
                            s_iy += incy;
                            ifracy += ifracdy;
                        } else {
                            s_iy += incy1;
                            ifracy -= ifracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        } else if (caseC) {
            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                s_x = (float) src_pt.getX();
                s_y = (float) src_pt.getY();

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix*1.0d;
                fracy = s_y - s_iy*1.0d;

                int ifracx = (int) Math.floor(fracx * geom_frac_max);
                int ifracy = (int) Math.floor(fracy * geom_frac_max);

                // Compute clipMinX, clipMinY
                javax.media.jai.util.Range clipRange = performScanlineClipping(
                        src_rect_x1,
                        src_rect_y1,
                        // Last point in the source is
                        // x2 = x1 + width - 1
                        // y2 = y1 + height - 1
                        src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy, dst_min_x,
                        dst_max_x, 0, 0, 0, 0);
                int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                // Advance s_ix, s_iy, ifracx, ifracy
                Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                        ifracx, ifracy);
                s_ix = startPts[0].x;
                s_iy = startPts[0].y;
                ifracx = startPts[1].x;
                ifracy = startPts[1].y;

                // Translate to/from SampleModel space & Raster space
                int posy = (s_iy - srcRectY) * srcScanlineStride;
                src_pos = posy + (s_ix - srcRectX) * srcPixelStride;

                if (setDestinationNoData) {
                    for (int x = dst_min_x; x < clipMinX; x++) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++)
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                        dstPixelOffset += dstPixelStride;
                    }
                } else
                    // Advance to first pixel
                    dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                for (int x = clipMinX; x < clipMaxX; x++) {
                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                        short value = srcDataArrays[k2][src_pos + bandOffsets[k2]];
                        if(noData.contains(value)){
                            if(setDestinationNoData){
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                            }
                        }else{
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] =value;
                        }
                        
                    }

                    // walk
                    if (ifracx < ifracdx1) {
                        src_pos += incxStride;
                        ifracx += ifracdx;
                    } else {
                        src_pos += incx1Stride;
                        ifracx -= ifracdx1;
                    }

                    if (ifracy < ifracdy1) {
                        src_pos += incyStride;
                        ifracy += ifracdy;
                    } else {
                        src_pos += incy1Stride;
                        ifracy -= ifracdy1;
                    }

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }

                if (setDestinationNoData && clipMinX <= clipMaxX) {
                    for (int x = clipMaxX; x < dst_max_x; x++) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++)
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                        dstPixelOffset += dstPixelStride;
                    }
                }
                // Go to the next line in the destination rectangle
                dstOffset += dstScanlineStride;
            }
        } else {
            if (useROIAccessor) {

                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    s_x = (float) src_pt.getX();
                    s_y = (float) src_pt.getY();

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix*1.0d;
                    fracy = s_y - s_iy*1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(
                            src_rect_x1,
                            src_rect_y1,
                            // Last point in the source is
                            // x2 = x1 + width - 1
                            // y2 = y1 + height - 1
                            src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 0, 0, 0);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;
                    ifracx = startPts[1].x;
                    ifracy = startPts[1].y;

                    // Translate to/from SampleModel space & Raster space
                    src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX) * srcPixelStride;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {
                        // Translate to/from SampleModel space & Raster space
                        int posy = (s_iy - srcRectY) * srcScanlineStride;
                        int posx = (s_ix - srcRectX) * srcPixelStride;
                        // If roiAccessor is present, the y position on the roi image is calculated
                        int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        src_pos = posx+posy;
                        
                        int windex = (posx / dst_num_bands) + posyROI;

                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                        if (w == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                // The interpolated value is saved in the destination array
                                short value = srcDataArrays[k2][src_pos + bandOffsets[k2]];
                                if(noData.contains(value)){
                                    if(setDestinationNoData){
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                                    }
                                }else{
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] =value;
                                }
                            }
                        }

                     // walk
                        if (ifracx < ifracdx1) {
                            s_ix += incx;
                            ifracx += ifracdx;
                        } else {
                            s_ix += incx1;
                            ifracx -= ifracdx1;
                        }
                        if (ifracy < ifracdy1) {
                            s_iy += incy;
                            ifracy += ifracdy;
                        } else {
                            s_iy += incy1;
                            ifracy -= ifracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
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
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    s_x = (float) src_pt.getX();
                    s_y = (float) src_pt.getY();

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix*1.0d;
                    fracy = s_y - s_iy*1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(
                            src_rect_x1,
                            src_rect_y1,
                            // Last point in the source is
                            // x2 = x1 + width - 1
                            // y2 = y1 + height - 1
                            src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 0, 0, 0);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;
                    ifracx = startPts[1].x;
                    ifracy = startPts[1].y;

                    // Translate to/from SampleModel space & Raster space
                    src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX) * srcPixelStride;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // Translate to/from SampleModel space & Raster space
                        int posy = (s_iy - srcRectY) * srcScanlineStride;
                        int posx = (s_ix - srcRectX) * srcPixelStride;

                        src_pos = posx+posy;

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (roiBounds.contains(x0, y0)) {

                            int w = roiIter.getSample(x0, y0, 0) & 0xff;
                            if (w == 0) {
                                if (setDestinationNoData) {
                                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                                    }
                                }
                            } else {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    // The interpolated value is saved in the destination array
                                    short value = srcDataArrays[k2][src_pos + bandOffsets[k2]];
                                    if(noData.contains(value)){
                                        if(setDestinationNoData){
                                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                                        }
                                    }else{
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] =value;
                                    }
                                }
                            }
                        } else if (setDestinationNoData) {
                            // The destination no data value is saved in the destination array
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                            }
                        }
                        // walk
                        if (ifracx < ifracdx1) {
                            s_ix += incx;
                            ifracx += ifracdx;
                        } else {
                            s_ix += incx1;
                            ifracx -= ifracdx1;
                        }
                        if (ifracy < ifracdy1) {
                            s_iy += incy;
                            ifracy += ifracdy;
                        } else {
                            s_iy += incy1;
                            ifracy -= ifracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        }
    }

    private void ushortLoop(int dataType, RasterAccessor src, Rectangle destRect,
            int srcRectX, int srcRectY, RasterAccessor dst, RasterAccessor roi) {


        final float src_rect_x1 = src.getX();
        final float src_rect_y1 = src.getY();
        final float src_rect_x2 = src_rect_x1 + src.getWidth();
        final float src_rect_y2 = src_rect_y1 + src.getHeight();

        float s_x, s_y;

        int src_pos;

        double fracx, fracy;

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

        final int incxStride = incx * srcPixelStride;
        final int incx1Stride = incx1 * srcPixelStride;
        final int incyStride = incy * srcScanlineStride;
        final int incy1Stride = incy1 * srcScanlineStride;

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
                dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                s_x = (float) src_pt.getX();
                s_y = (float) src_pt.getY();

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix*1.0d;
                fracy = s_y - s_iy*1.0d;

                int ifracx = (int) Math.floor(fracx * geom_frac_max);
                int ifracy = (int) Math.floor(fracy * geom_frac_max);

                // Compute clipMinX, clipMinY
                javax.media.jai.util.Range clipRange = performScanlineClipping(
                        src_rect_x1,
                        src_rect_y1,
                        // Last point in the source is
                        // x2 = x1 + width - 1
                        // y2 = y1 + height - 1
                        src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy, dst_min_x,
                        dst_max_x, 0, 0, 0, 0);
                int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                // Advance s_ix, s_iy, ifracx, ifracy
                Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                        ifracx, ifracy);
                s_ix = startPts[0].x;
                s_iy = startPts[0].y;
                ifracx = startPts[1].x;
                ifracy = startPts[1].y;

                // Translate to/from SampleModel space & Raster space
                src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX)
                        * srcPixelStride;

                if (setDestinationNoData) {
                    for (int x = dst_min_x; x < clipMinX; x++) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++)
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                        dstPixelOffset += dstPixelStride;
                    }
                } else{
                 // Advance to first pixel
                    dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;
                }
                    

                for (int x = clipMinX; x < clipMaxX; x++) {
                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = srcDataArrays[k2][src_pos
                                + bandOffsets[k2]];
                    }
                    // walk
                    if (ifracx < ifracdx1) {
                        src_pos += incxStride;
                        ifracx += ifracdx;
                    } else {
                        src_pos += incx1Stride;
                        ifracx -= ifracdx1;
                    }

                    if (ifracy < ifracdy1) {
                        src_pos += incyStride;
                        ifracy += ifracdy;
                    } else {
                        src_pos += incy1Stride;
                        ifracy -= ifracdy1;
                    }

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }

                if (setDestinationNoData && clipMinX <= clipMaxX) {
                    for (int x = clipMaxX; x < dst_max_x; x++) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++)
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                        dstPixelOffset += dstPixelStride;
                    }
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
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    s_x = (float) src_pt.getX();
                    s_y = (float) src_pt.getY();

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix*1.0d;
                    fracy = s_y - s_iy*1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx,
                            ifracy, dst_min_x, dst_max_x, 0, 0, 0, 0);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;
                    ifracx = startPts[1].x;
                    ifracy = startPts[1].y;

                    // Translate to/from SampleModel space & Raster space
                    src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX) * srcPixelStride;

                    

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // Translate to/from SampleModel space & Raster space
                        int posy = (s_iy - srcRectY) * srcScanlineStride;
                        int posx = (s_ix - srcRectX) * srcPixelStride;
                        // If roiAccessor is present, the y position on the roi image is calculated
                        int posyROI = (s_iy - srcRectY) * roiScanlineStride;
                        
                        src_pos = posx+posy;
                        
                        int windex = (posx / dst_num_bands) + posyROI;

                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                        if (w == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                // The interpolated value is saved in the destination array
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = srcDataArrays[k2][src_pos
                                        + bandOffsets[k2]];
                            }
                        }
                        // walk
                        if (ifracx < ifracdx1) {
                            s_ix += incx;
                            ifracx += ifracdx;
                        } else {
                            s_ix += incx1;
                            ifracx -= ifracdx1;
                        }
                        if (ifracy < ifracdy1) {
                            s_iy += incy;
                            ifracy += ifracdy;
                        } else {
                            s_iy += incy1;
                            ifracy -= ifracdy1;
                        }
                        
                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
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
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    s_x = (float) src_pt.getX();
                    s_y = (float) src_pt.getY();

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix*1.0d;
                    fracy = s_y - s_iy*1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(
                            src_rect_x1,
                            src_rect_y1,
                            // Last point in the source is
                            // x2 = x1 + width - 1
                            // y2 = y1 + height - 1
                            src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 0, 0, 0);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;
                    ifracx = startPts[1].x;
                    ifracy = startPts[1].y;

                    // Translate to/from SampleModel space & Raster space
                    src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX) * srcPixelStride;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // Translate to/from SampleModel space & Raster space
                        int posy = (s_iy - srcRectY) * srcScanlineStride;
                        int posx = (s_ix - srcRectX) * srcPixelStride;
                        
                        src_pos = posx+posy;

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (roiBounds.contains(x0, y0)) {

                            int w = roiIter.getSample(x0, y0, 0) & 0xff;
                            if (w == 0) {
                                if (setDestinationNoData) {
                                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                                    }
                                }
                            } else {
                                // The interpolated value is saved in the destination array
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = srcDataArrays[k2][src_pos
                                            + bandOffsets[k2]];
                                }
                            }
                        } else if (setDestinationNoData) {
                            // The destination no data value is saved in the destination array
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                            }
                        }

                        // walk
                        if (ifracx < ifracdx1) {
                            s_ix += incx;
                            ifracx += ifracdx;
                        } else {
                            s_ix += incx1;
                            ifracx -= ifracdx1;
                        }
                        if (ifracy < ifracdy1) {
                            s_iy += incy;
                            ifracy += ifracdy;
                        } else {
                            s_iy += incy1;
                            ifracy -= ifracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        } else if (caseC) {
            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                s_x = (float) src_pt.getX();
                s_y = (float) src_pt.getY();

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix*1.0d;
                fracy = s_y - s_iy*1.0d;

                int ifracx = (int) Math.floor(fracx * geom_frac_max);
                int ifracy = (int) Math.floor(fracy * geom_frac_max);

                // Compute clipMinX, clipMinY
                javax.media.jai.util.Range clipRange = performScanlineClipping(
                        src_rect_x1,
                        src_rect_y1,
                        // Last point in the source is
                        // x2 = x1 + width - 1
                        // y2 = y1 + height - 1
                        src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy, dst_min_x,
                        dst_max_x, 0, 0, 0, 0);
                int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                // Advance s_ix, s_iy, ifracx, ifracy
                Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                        ifracx, ifracy);
                s_ix = startPts[0].x;
                s_iy = startPts[0].y;
                ifracx = startPts[1].x;
                ifracy = startPts[1].y;

                // Translate to/from SampleModel space & Raster space
                int posy = (s_iy - srcRectY) * srcScanlineStride;
                src_pos = posy + (s_ix - srcRectX) * srcPixelStride;

                if (setDestinationNoData) {
                    for (int x = dst_min_x; x < clipMinX; x++) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++)
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                        dstPixelOffset += dstPixelStride;
                    }
                } else
                    // Advance to first pixel
                    dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                for (int x = clipMinX; x < clipMaxX; x++) {
                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                        short value = (short) (srcDataArrays[k2][src_pos + bandOffsets[k2]] & 0xffff);
                        if(noData.contains(value)){
                            if(setDestinationNoData){
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                            }
                        }else{
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] =value;
                        }
                        
                    }

                    // walk
                    if (ifracx < ifracdx1) {
                        src_pos += incxStride;
                        ifracx += ifracdx;
                    } else {
                        src_pos += incx1Stride;
                        ifracx -= ifracdx1;
                    }

                    if (ifracy < ifracdy1) {
                        src_pos += incyStride;
                        ifracy += ifracdy;
                    } else {
                        src_pos += incy1Stride;
                        ifracy -= ifracdy1;
                    }

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }

                if (setDestinationNoData && clipMinX <= clipMaxX) {
                    for (int x = clipMaxX; x < dst_max_x; x++) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++)
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                        dstPixelOffset += dstPixelStride;
                    }
                }
                // Go to the next line in the destination rectangle
                dstOffset += dstScanlineStride;
            }
        } else {
            if (useROIAccessor) {

                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    s_x = (float) src_pt.getX();
                    s_y = (float) src_pt.getY();

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix*1.0d;
                    fracy = s_y - s_iy*1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(
                            src_rect_x1,
                            src_rect_y1,
                            // Last point in the source is
                            // x2 = x1 + width - 1
                            // y2 = y1 + height - 1
                            src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 0, 0, 0);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;
                    ifracx = startPts[1].x;
                    ifracy = startPts[1].y;

                    // Translate to/from SampleModel space & Raster space
                    src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX) * srcPixelStride;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {
                        // Translate to/from SampleModel space & Raster space
                        int posy = (s_iy - srcRectY) * srcScanlineStride;
                        int posx = (s_ix - srcRectX) * srcPixelStride;
                        // If roiAccessor is present, the y position on the roi image is calculated
                        int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        src_pos = posx+posy;
                        
                        int windex = (posx / dst_num_bands) + posyROI;

                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                        if (w == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                // The interpolated value is saved in the destination array
                                short value = (short) (srcDataArrays[k2][src_pos + bandOffsets[k2]] & 0xffff);
                                if(noData.contains(value)){
                                    if(setDestinationNoData){
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                                    }
                                }else{
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] =value;
                                }
                            }
                        }

                     // walk
                        if (ifracx < ifracdx1) {
                            s_ix += incx;
                            ifracx += ifracdx;
                        } else {
                            s_ix += incx1;
                            ifracx -= ifracdx1;
                        }
                        if (ifracy < ifracdy1) {
                            s_iy += incy;
                            ifracy += ifracdy;
                        } else {
                            s_iy += incy1;
                            ifracy -= ifracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
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
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    s_x = (float) src_pt.getX();
                    s_y = (float) src_pt.getY();

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix*1.0d;
                    fracy = s_y - s_iy*1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(
                            src_rect_x1,
                            src_rect_y1,
                            // Last point in the source is
                            // x2 = x1 + width - 1
                            // y2 = y1 + height - 1
                            src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 0, 0, 0);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;
                    ifracx = startPts[1].x;
                    ifracy = startPts[1].y;

                    // Translate to/from SampleModel space & Raster space
                    src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX) * srcPixelStride;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // Translate to/from SampleModel space & Raster space
                        int posy = (s_iy - srcRectY) * srcScanlineStride;
                        int posx = (s_ix - srcRectX) * srcPixelStride;

                        src_pos = posx+posy;

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (roiBounds.contains(x0, y0)) {

                            int w = roiIter.getSample(x0, y0, 0) & 0xff;
                            if (w == 0) {
                                if (setDestinationNoData) {
                                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                                    }
                                }
                            } else {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    // The interpolated value is saved in the destination array
                                    short value = (short) (srcDataArrays[k2][src_pos + bandOffsets[k2]] & 0xffff);
                                    if(noData.contains(value)){
                                        if(setDestinationNoData){
                                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                                        }
                                    }else{
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] =value;
                                    }
                                }
                            }
                        } else if (setDestinationNoData) {
                            // The destination no data value is saved in the destination array
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                            }
                        }
                        // walk
                        if (ifracx < ifracdx1) {
                            s_ix += incx;
                            ifracx += ifracdx;
                        } else {
                            s_ix += incx1;
                            ifracx -= ifracdx1;
                        }
                        if (ifracy < ifracdy1) {
                            s_iy += incy;
                            ifracy += ifracdy;
                        } else {
                            s_iy += incy1;
                            ifracy -= ifracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataUShort;
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        }
    }

    private void floatLoop(int dataType, RasterAccessor src, Rectangle destRect,
            int srcRectX, int srcRectY, RasterAccessor dst, RasterAccessor roi) {


        final float src_rect_x1 = src.getX();
        final float src_rect_y1 = src.getY();
        final float src_rect_x2 = src_rect_x1 + src.getWidth();
        final float src_rect_y2 = src_rect_y1 + src.getHeight();

        float s_x, s_y;

        int src_pos;

        double fracx, fracy;

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

        final int incxStride = incx * srcPixelStride;
        final int incx1Stride = incx1 * srcPixelStride;
        final int incyStride = incy * srcScanlineStride;
        final int incy1Stride = incy1 * srcScanlineStride;

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
                dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                s_x = (float) src_pt.getX();
                s_y = (float) src_pt.getY();

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix*1.0d;
                fracy = s_y - s_iy*1.0d;

                int ifracx = (int) Math.floor(fracx * geom_frac_max);
                int ifracy = (int) Math.floor(fracy * geom_frac_max);

                // Compute clipMinX, clipMinY
                javax.media.jai.util.Range clipRange = performScanlineClipping(
                        src_rect_x1,
                        src_rect_y1,
                        // Last point in the source is
                        // x2 = x1 + width - 1
                        // y2 = y1 + height - 1
                        src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy, dst_min_x,
                        dst_max_x, 0, 0, 0, 0);
                int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                // Advance s_ix, s_iy, ifracx, ifracy
                Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                        ifracx, ifracy);
                s_ix = startPts[0].x;
                s_iy = startPts[0].y;
                ifracx = startPts[1].x;
                ifracy = startPts[1].y;

                // Translate to/from SampleModel space & Raster space
                src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX)
                        * srcPixelStride;

                if (setDestinationNoData) {
                    for (int x = dst_min_x; x < clipMinX; x++) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++)
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                        dstPixelOffset += dstPixelStride;
                    }
                } else{
                 // Advance to first pixel
                    dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;
                }
                    

                for (int x = clipMinX; x < clipMaxX; x++) {
                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = srcDataArrays[k2][src_pos
                                + bandOffsets[k2]];
                    }
                    // walk
                    if (ifracx < ifracdx1) {
                        src_pos += incxStride;
                        ifracx += ifracdx;
                    } else {
                        src_pos += incx1Stride;
                        ifracx -= ifracdx1;
                    }

                    if (ifracy < ifracdy1) {
                        src_pos += incyStride;
                        ifracy += ifracdy;
                    } else {
                        src_pos += incy1Stride;
                        ifracy -= ifracdy1;
                    }

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }

                if (setDestinationNoData && clipMinX <= clipMaxX) {
                    for (int x = clipMaxX; x < dst_max_x; x++) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++)
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                        dstPixelOffset += dstPixelStride;
                    }
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
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    s_x = (float) src_pt.getX();
                    s_y = (float) src_pt.getY();

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix*1.0d;
                    fracy = s_y - s_iy*1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx,
                            ifracy, dst_min_x, dst_max_x, 0, 0, 0, 0);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;
                    ifracx = startPts[1].x;
                    ifracy = startPts[1].y;

                    // Translate to/from SampleModel space & Raster space
                    src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX) * srcPixelStride;

                    

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // Translate to/from SampleModel space & Raster space
                        int posy = (s_iy - srcRectY) * srcScanlineStride;
                        int posx = (s_ix - srcRectX) * srcPixelStride;
                        // If roiAccessor is present, the y position on the roi image is calculated
                        int posyROI = (s_iy - srcRectY) * roiScanlineStride;
                        
                        src_pos = posx+posy;
                        
                        int windex = (posx / dst_num_bands) + posyROI;

                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                        if (w == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                // The interpolated value is saved in the destination array
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = srcDataArrays[k2][src_pos
                                        + bandOffsets[k2]];
                            }
                        }
                        // walk
                        if (ifracx < ifracdx1) {
                            s_ix += incx;
                            ifracx += ifracdx;
                        } else {
                            s_ix += incx1;
                            ifracx -= ifracdx1;
                        }
                        if (ifracy < ifracdy1) {
                            s_iy += incy;
                            ifracy += ifracdy;
                        } else {
                            s_iy += incy1;
                            ifracy -= ifracdy1;
                        }
                        
                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
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
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    s_x = (float) src_pt.getX();
                    s_y = (float) src_pt.getY();

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix*1.0d;
                    fracy = s_y - s_iy*1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(
                            src_rect_x1,
                            src_rect_y1,
                            // Last point in the source is
                            // x2 = x1 + width - 1
                            // y2 = y1 + height - 1
                            src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 0, 0, 0);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;
                    ifracx = startPts[1].x;
                    ifracy = startPts[1].y;

                    // Translate to/from SampleModel space & Raster space
                    src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX) * srcPixelStride;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // Translate to/from SampleModel space & Raster space
                        int posy = (s_iy - srcRectY) * srcScanlineStride;
                        int posx = (s_ix - srcRectX) * srcPixelStride;
                        
                        src_pos = posx+posy;

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (roiBounds.contains(x0, y0)) {

                            int w = roiIter.getSample(x0, y0, 0) & 0xff;
                            if (w == 0) {
                                if (setDestinationNoData) {
                                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                                    }
                                }
                            } else {
                                // The interpolated value is saved in the destination array
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = srcDataArrays[k2][src_pos
                                            + bandOffsets[k2]];
                                }
                            }
                        } else if (setDestinationNoData) {
                            // The destination no data value is saved in the destination array
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                            }
                        }

                        // walk
                        if (ifracx < ifracdx1) {
                            s_ix += incx;
                            ifracx += ifracdx;
                        } else {
                            s_ix += incx1;
                            ifracx -= ifracdx1;
                        }
                        if (ifracy < ifracdy1) {
                            s_iy += incy;
                            ifracy += ifracdy;
                        } else {
                            s_iy += incy1;
                            ifracy -= ifracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        } else if (caseC) {
            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                s_x = (float) src_pt.getX();
                s_y = (float) src_pt.getY();

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix*1.0d;
                fracy = s_y - s_iy*1.0d;

                int ifracx = (int) Math.floor(fracx * geom_frac_max);
                int ifracy = (int) Math.floor(fracy * geom_frac_max);

                // Compute clipMinX, clipMinY
                javax.media.jai.util.Range clipRange = performScanlineClipping(
                        src_rect_x1,
                        src_rect_y1,
                        // Last point in the source is
                        // x2 = x1 + width - 1
                        // y2 = y1 + height - 1
                        src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy, dst_min_x,
                        dst_max_x, 0, 0, 0, 0);
                int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                // Advance s_ix, s_iy, ifracx, ifracy
                Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                        ifracx, ifracy);
                s_ix = startPts[0].x;
                s_iy = startPts[0].y;
                ifracx = startPts[1].x;
                ifracy = startPts[1].y;

                // Translate to/from SampleModel space & Raster space
                int posy = (s_iy - srcRectY) * srcScanlineStride;
                src_pos = posy + (s_ix - srcRectX) * srcPixelStride;

                if (setDestinationNoData) {
                    for (int x = dst_min_x; x < clipMinX; x++) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++)
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                        dstPixelOffset += dstPixelStride;
                    }
                } else
                    // Advance to first pixel
                    dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                for (int x = clipMinX; x < clipMaxX; x++) {
                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                        float value = srcDataArrays[k2][src_pos + bandOffsets[k2]];
                        if (noData.contains(value)) {
                            // The destination no data value is saved in the destination array
                            if(setDestinationNoData){
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                            }
                        } else {
                            // The interpolated value is saved in the destination array
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] =value;
                        }                        
                    }

                    // walk
                    if (ifracx < ifracdx1) {
                        src_pos += incxStride;
                        ifracx += ifracdx;
                    } else {
                        src_pos += incx1Stride;
                        ifracx -= ifracdx1;
                    }

                    if (ifracy < ifracdy1) {
                        src_pos += incyStride;
                        ifracy += ifracdy;
                    } else {
                        src_pos += incy1Stride;
                        ifracy -= ifracdy1;
                    }

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }

                if (setDestinationNoData && clipMinX <= clipMaxX) {
                    for (int x = clipMaxX; x < dst_max_x; x++) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++)
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                        dstPixelOffset += dstPixelStride;
                    }
                }
                // Go to the next line in the destination rectangle
                dstOffset += dstScanlineStride;
            }
        } else {
            if (useROIAccessor) {

                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    s_x = (float) src_pt.getX();
                    s_y = (float) src_pt.getY();

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix*1.0d;
                    fracy = s_y - s_iy*1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(
                            src_rect_x1,
                            src_rect_y1,
                            // Last point in the source is
                            // x2 = x1 + width - 1
                            // y2 = y1 + height - 1
                            src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 0, 0, 0);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;
                    ifracx = startPts[1].x;
                    ifracy = startPts[1].y;

                    // Translate to/from SampleModel space & Raster space
                    src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX) * srcPixelStride;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {
                        // Translate to/from SampleModel space & Raster space
                        int posy = (s_iy - srcRectY) * srcScanlineStride;
                        int posx = (s_ix - srcRectX) * srcPixelStride;
                        // If roiAccessor is present, the y position on the roi image is calculated
                        int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        src_pos = posx+posy;
                        
                        int windex = (posx / dst_num_bands) + posyROI;

                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                        if (w == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                // The interpolated value is saved in the destination array
                                float value = srcDataArrays[k2][src_pos + bandOffsets[k2]];
                                if (noData.contains(value)) {
                                    // The destination no data value is saved in the destination array
                                    if(setDestinationNoData){
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                                    }
                                } else {
                                    // The interpolated value is saved in the destination array
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] =value;
                                }  
                            }
                        }
                     // walk
                        if (ifracx < ifracdx1) {
                            s_ix += incx;
                            ifracx += ifracdx;
                        } else {
                            s_ix += incx1;
                            ifracx -= ifracdx1;
                        }
                        if (ifracy < ifracdy1) {
                            s_iy += incy;
                            ifracy += ifracdy;
                        } else {
                            s_iy += incy1;
                            ifracy -= ifracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
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
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    s_x = (float) src_pt.getX();
                    s_y = (float) src_pt.getY();

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix*1.0d;
                    fracy = s_y - s_iy*1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(
                            src_rect_x1,
                            src_rect_y1,
                            // Last point in the source is
                            // x2 = x1 + width - 1
                            // y2 = y1 + height - 1
                            src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 0, 0, 0);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;
                    ifracx = startPts[1].x;
                    ifracy = startPts[1].y;

                    // Translate to/from SampleModel space & Raster space
                    src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX) * srcPixelStride;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // Translate to/from SampleModel space & Raster space
                        int posy = (s_iy - srcRectY) * srcScanlineStride;
                        int posx = (s_ix - srcRectX) * srcPixelStride;

                        src_pos = posx+posy;

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (roiBounds.contains(x0, y0)) {

                            int w = roiIter.getSample(x0, y0, 0) & 0xff;
                            if (w == 0) {
                                if (setDestinationNoData) {
                                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                                    }
                                }
                            } else {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    // The interpolated value is saved in the destination array
                                    float value = srcDataArrays[k2][src_pos + bandOffsets[k2]];
                                    if (noData.contains(value)) {
                                        // The destination no data value is saved in the destination array
                                        if(setDestinationNoData){
                                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                                        }
                                    } else {
                                        // The interpolated value is saved in the destination array
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] =value;
                                    }  
                                }
                            }
                        } else if (setDestinationNoData) {
                            // The destination no data value is saved in the destination array
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                            }
                        }
                        // walk
                        if (ifracx < ifracdx1) {
                            s_ix += incx;
                            ifracx += ifracdx;
                        } else {
                            s_ix += incx1;
                            ifracx -= ifracdx1;
                        }
                        if (ifracy < ifracdy1) {
                            s_iy += incy;
                            ifracy += ifracdy;
                        } else {
                            s_iy += incy1;
                            ifracy -= ifracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        }
    }

    private void doubleLoop(int dataType, RasterAccessor src, Rectangle destRect,
            int srcRectX, int srcRectY, RasterAccessor dst, RasterAccessor roi) {


        final float src_rect_x1 = src.getX();
        final float src_rect_y1 = src.getY();
        final float src_rect_x2 = src_rect_x1 + src.getWidth();
        final float src_rect_y2 = src_rect_y1 + src.getHeight();

        float s_x, s_y;

        int src_pos;

        double fracx, fracy;

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

        final int incxStride = incx * srcPixelStride;
        final int incx1Stride = incx1 * srcPixelStride;
        final int incyStride = incy * srcScanlineStride;
        final int incy1Stride = incy1 * srcScanlineStride;

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
                dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                s_x = (float) src_pt.getX();
                s_y = (float) src_pt.getY();

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix*1.0d;
                fracy = s_y - s_iy*1.0d;

                int ifracx = (int) Math.floor(fracx * geom_frac_max);
                int ifracy = (int) Math.floor(fracy * geom_frac_max);

                // Compute clipMinX, clipMinY
                javax.media.jai.util.Range clipRange = performScanlineClipping(
                        src_rect_x1,
                        src_rect_y1,
                        // Last point in the source is
                        // x2 = x1 + width - 1
                        // y2 = y1 + height - 1
                        src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy, dst_min_x,
                        dst_max_x, 0, 0, 0, 0);
                int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                // Advance s_ix, s_iy, ifracx, ifracy
                Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                        ifracx, ifracy);
                s_ix = startPts[0].x;
                s_iy = startPts[0].y;
                ifracx = startPts[1].x;
                ifracy = startPts[1].y;

                // Translate to/from SampleModel space & Raster space
                src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX)
                        * srcPixelStride;

                if (setDestinationNoData) {
                    for (int x = dst_min_x; x < clipMinX; x++) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++)
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                        dstPixelOffset += dstPixelStride;
                    }
                } else{
                 // Advance to first pixel
                    dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;
                }
                    

                for (int x = clipMinX; x < clipMaxX; x++) {
                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = srcDataArrays[k2][src_pos
                                + bandOffsets[k2]];
                    }
                    // walk
                    if (ifracx < ifracdx1) {
                        src_pos += incxStride;
                        ifracx += ifracdx;
                    } else {
                        src_pos += incx1Stride;
                        ifracx -= ifracdx1;
                    }

                    if (ifracy < ifracdy1) {
                        src_pos += incyStride;
                        ifracy += ifracdy;
                    } else {
                        src_pos += incy1Stride;
                        ifracy -= ifracdy1;
                    }

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }

                if (setDestinationNoData && clipMinX <= clipMaxX) {
                    for (int x = clipMaxX; x < dst_max_x; x++) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++)
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                        dstPixelOffset += dstPixelStride;
                    }
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
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    s_x = (float) src_pt.getX();
                    s_y = (float) src_pt.getY();

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix*1.0d;
                    fracy = s_y - s_iy*1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(src_rect_x1,
                            src_rect_y1, src_rect_x2, src_rect_y2, s_ix, s_iy, ifracx,
                            ifracy, dst_min_x, dst_max_x, 0, 0, 0, 0);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;
                    ifracx = startPts[1].x;
                    ifracy = startPts[1].y;

                    // Translate to/from SampleModel space & Raster space
                    src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX) * srcPixelStride;

                    

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // Translate to/from SampleModel space & Raster space
                        int posy = (s_iy - srcRectY) * srcScanlineStride;
                        int posx = (s_ix - srcRectX) * srcPixelStride;
                        // If roiAccessor is present, the y position on the roi image is calculated
                        int posyROI = (s_iy - srcRectY) * roiScanlineStride;
                        
                        src_pos = posx+posy;
                        
                        int windex = (posx / dst_num_bands) + posyROI;

                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                        if (w == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                // The interpolated value is saved in the destination array
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = srcDataArrays[k2][src_pos
                                        + bandOffsets[k2]];
                            }
                        }
                        // walk
                        if (ifracx < ifracdx1) {
                            s_ix += incx;
                            ifracx += ifracdx;
                        } else {
                            s_ix += incx1;
                            ifracx -= ifracdx1;
                        }
                        if (ifracy < ifracdy1) {
                            s_iy += incy;
                            ifracy += ifracdy;
                        } else {
                            s_iy += incy1;
                            ifracy -= ifracdy1;
                        }
                        
                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
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
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    s_x = (float) src_pt.getX();
                    s_y = (float) src_pt.getY();

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix*1.0d;
                    fracy = s_y - s_iy*1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(
                            src_rect_x1,
                            src_rect_y1,
                            // Last point in the source is
                            // x2 = x1 + width - 1
                            // y2 = y1 + height - 1
                            src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 0, 0, 0);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;
                    ifracx = startPts[1].x;
                    ifracy = startPts[1].y;

                    // Translate to/from SampleModel space & Raster space
                    src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX) * srcPixelStride;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // Translate to/from SampleModel space & Raster space
                        int posy = (s_iy - srcRectY) * srcScanlineStride;
                        int posx = (s_ix - srcRectX) * srcPixelStride;
                        
                        src_pos = posx+posy;

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (roiBounds.contains(x0, y0)) {

                            int w = roiIter.getSample(x0, y0, 0) & 0xff;
                            if (w == 0) {
                                if (setDestinationNoData) {
                                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                                    }
                                }
                            } else {
                                // The interpolated value is saved in the destination array
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = srcDataArrays[k2][src_pos
                                            + bandOffsets[k2]];
                                }
                            }
                        } else if (setDestinationNoData) {
                            // The destination no data value is saved in the destination array
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                            }
                        }

                        // walk
                        if (ifracx < ifracdx1) {
                            s_ix += incx;
                            ifracx += ifracdx;
                        } else {
                            s_ix += incx1;
                            ifracx -= ifracdx1;
                        }
                        if (ifracy < ifracdy1) {
                            s_iy += incy;
                            ifracy += ifracdy;
                        } else {
                            s_iy += incy1;
                            ifracy -= ifracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                            dstPixelOffset += dstPixelStride;
                        }
                    }

                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        } else if (caseC) {
            for (int y = dst_min_y; y < dst_max_y; y++) {
                dstPixelOffset = dstOffset;

                // Backward map the first point in the line
                // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                mapDestPoint(dst_pt, src_pt);

                // Get the mapped source coordinates
                s_x = (float) src_pt.getX();
                s_y = (float) src_pt.getY();

                // Floor to get the integral coordinate
                int s_ix = (int) Math.floor(s_x);
                int s_iy = (int) Math.floor(s_y);

                fracx = s_x - s_ix*1.0d;
                fracy = s_y - s_iy*1.0d;

                int ifracx = (int) Math.floor(fracx * geom_frac_max);
                int ifracy = (int) Math.floor(fracy * geom_frac_max);

                // Compute clipMinX, clipMinY
                javax.media.jai.util.Range clipRange = performScanlineClipping(
                        src_rect_x1,
                        src_rect_y1,
                        // Last point in the source is
                        // x2 = x1 + width - 1
                        // y2 = y1 + height - 1
                        src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy, dst_min_x,
                        dst_max_x, 0, 0, 0, 0);
                int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                // Advance s_ix, s_iy, ifracx, ifracy
                Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                        ifracx, ifracy);
                s_ix = startPts[0].x;
                s_iy = startPts[0].y;
                ifracx = startPts[1].x;
                ifracy = startPts[1].y;

                // Translate to/from SampleModel space & Raster space
                int posy = (s_iy - srcRectY) * srcScanlineStride;
                src_pos = posy + (s_ix - srcRectX) * srcPixelStride;

                if (setDestinationNoData) {
                    for (int x = dst_min_x; x < clipMinX; x++) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++)
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                        dstPixelOffset += dstPixelStride;
                    }
                } else
                    // Advance to first pixel
                    dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                for (int x = clipMinX; x < clipMaxX; x++) {
                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                        double value = srcDataArrays[k2][src_pos + bandOffsets[k2]];
                        if (noData.contains(value)) {
                            // The destination no data value is saved in the destination array
                            if(setDestinationNoData){
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataFloat;
                            }
                        } else {
                            // The interpolated value is saved in the destination array
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] =value;
                        }  
                    }

                    // walk
                    if (ifracx < ifracdx1) {
                        src_pos += incxStride;
                        ifracx += ifracdx;
                    } else {
                        src_pos += incx1Stride;
                        ifracx -= ifracdx1;
                    }

                    if (ifracy < ifracdy1) {
                        src_pos += incyStride;
                        ifracy += ifracdy;
                    } else {
                        src_pos += incy1Stride;
                        ifracy -= ifracdy1;
                    }

                    // Go to next pixel
                    dstPixelOffset += dstPixelStride;
                }

                if (setDestinationNoData && clipMinX <= clipMaxX) {
                    for (int x = clipMaxX; x < dst_max_x; x++) {
                        for (int k2 = 0; k2 < dst_num_bands; k2++)
                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                        dstPixelOffset += dstPixelStride;
                    }
                }
                // Go to the next line in the destination rectangle
                dstOffset += dstScanlineStride;
            }
        } else {
            if (useROIAccessor) {

                for (int y = dst_min_y; y < dst_max_y; y++) {
                    dstPixelOffset = dstOffset;

                    // Backward map the first point in the line
                    // The energy is at the (pt_x + 0.5, pt_y + 0.5)
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    s_x = (float) src_pt.getX();
                    s_y = (float) src_pt.getY();

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix*1.0d;
                    fracy = s_y - s_iy*1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(
                            src_rect_x1,
                            src_rect_y1,
                            // Last point in the source is
                            // x2 = x1 + width - 1
                            // y2 = y1 + height - 1
                            src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 0, 0, 0);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;
                    ifracx = startPts[1].x;
                    ifracy = startPts[1].y;

                    // Translate to/from SampleModel space & Raster space
                    src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX) * srcPixelStride;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {
                        // Translate to/from SampleModel space & Raster space
                        int posy = (s_iy - srcRectY) * srcScanlineStride;
                        int posx = (s_ix - srcRectX) * srcPixelStride;
                        // If roiAccessor is present, the y position on the roi image is calculated
                        int posyROI = (s_iy - srcRectY) * roiScanlineStride;

                        src_pos = posx+posy;
                        
                        int windex = (posx / dst_num_bands) + posyROI;

                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                        if (w == 0) {
                            if (setDestinationNoData) {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                                }
                            }
                        } else {
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                // The interpolated value is saved in the destination array
                                double value = srcDataArrays[k2][src_pos + bandOffsets[k2]];
                                if (noData.contains(value)) {
                                    // The destination no data value is saved in the destination array
                                    if(setDestinationNoData){
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                                    }
                                } else {
                                    // The interpolated value is saved in the destination array
                                    dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] =value;
                                }  
                            }
                        }

                     // walk
                        if (ifracx < ifracdx1) {
                            s_ix += incx;
                            ifracx += ifracdx;
                        } else {
                            s_ix += incx1;
                            ifracx -= ifracdx1;
                        }
                        if (ifracy < ifracdy1) {
                            s_iy += incy;
                            ifracy += ifracdy;
                        } else {
                            s_iy += incy1;
                            ifracy -= ifracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
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
                    dst_pt.setLocation(dst_min_x + 0.5d, y + 0.5d);

                    mapDestPoint(dst_pt, src_pt);

                    // Get the mapped source coordinates
                    s_x = (float) src_pt.getX();
                    s_y = (float) src_pt.getY();

                    // Floor to get the integral coordinate
                    int s_ix = (int) Math.floor(s_x);
                    int s_iy = (int) Math.floor(s_y);

                    fracx = s_x - s_ix*1.0d;
                    fracy = s_y - s_iy*1.0d;

                    int ifracx = (int) Math.floor(fracx * geom_frac_max);
                    int ifracy = (int) Math.floor(fracy * geom_frac_max);

                    // Compute clipMinX, clipMinY
                    javax.media.jai.util.Range clipRange = performScanlineClipping(
                            src_rect_x1,
                            src_rect_y1,
                            // Last point in the source is
                            // x2 = x1 + width - 1
                            // y2 = y1 + height - 1
                            src_rect_x2 , src_rect_y2 , s_ix, s_iy, ifracx, ifracy,
                            dst_min_x, dst_max_x, 0, 0, 0, 0);
                    int clipMinX = ((Integer) clipRange.getMinValue()).intValue();
                    int clipMaxX = ((Integer) clipRange.getMaxValue()).intValue();

                    // Advance s_ix, s_iy, ifracx, ifracy
                    Point[] startPts = advanceToStartOfScanline(dst_min_x, clipMinX, s_ix, s_iy,
                            ifracx, ifracy);
                    s_ix = startPts[0].x;
                    s_iy = startPts[0].y;
                    ifracx = startPts[1].x;
                    ifracy = startPts[1].y;

                    // Translate to/from SampleModel space & Raster space
                    src_pos = (s_iy - srcRectY) * srcScanlineStride + (s_ix - srcRectX) * srcPixelStride;

                    if (setDestinationNoData) {
                        for (int x = dst_min_x; x < clipMinX; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                            dstPixelOffset += dstPixelStride;
                        }
                    } else
                        // Advance to first pixel
                        dstPixelOffset += (clipMinX - dst_min_x) * dstPixelStride;

                    for (int x = clipMinX; x < clipMaxX; x++) {

                        // Translate to/from SampleModel space & Raster space
                        int posy = (s_iy - srcRectY) * srcScanlineStride;
                        int posx = (s_ix - srcRectX) * srcPixelStride;

                        src_pos = posx+posy;

                        int x0 = src.getX() + posx / srcPixelStride;
                        int y0 = src.getY() + posy / srcScanlineStride;

                        if (roiBounds.contains(x0, y0)) {

                            int w = roiIter.getSample(x0, y0, 0) & 0xff;
                            if (w == 0) {
                                if (setDestinationNoData) {
                                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                                    }
                                }
                            } else {
                                for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                    // The interpolated value is saved in the destination array
                                    double value = srcDataArrays[k2][src_pos + bandOffsets[k2]];
                                    if (noData.contains(value)) {
                                        // The destination no data value is saved in the destination array
                                        if(setDestinationNoData){
                                            dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                                        }
                                    } else {
                                        // The interpolated value is saved in the destination array
                                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] =value;
                                    }  
                                }
                            }
                        } else if (setDestinationNoData) {
                            // The destination no data value is saved in the destination array
                            for (int k2 = 0; k2 < dst_num_bands; k2++) {
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                            }
                        }
                        // walk
                        if (ifracx < ifracdx1) {
                            s_ix += incx;
                            ifracx += ifracdx;
                        } else {
                            s_ix += incx1;
                            ifracx -= ifracdx1;
                        }
                        if (ifracy < ifracdy1) {
                            s_iy += incy;
                            ifracy += ifracdy;
                        } else {
                            s_iy += incy1;
                            ifracy -= ifracdy1;
                        }

                        // Go to next pixel
                        dstPixelOffset += dstPixelStride;
                    }

                    if (setDestinationNoData && clipMinX <= clipMaxX) {
                        for (int x = clipMaxX; x < dst_max_x; x++) {
                            for (int k2 = 0; k2 < dst_num_bands; k2++)
                                dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = destinationNoDataDouble;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                    // Go to the next line in the destination rectangle
                    dstOffset += dstScanlineStride;
                }
            }
        }
    }
}
