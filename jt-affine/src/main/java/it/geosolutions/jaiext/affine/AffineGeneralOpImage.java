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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
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
import java.util.Arrays;
import java.util.Map;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.iterator.RandomIter;

import com.sun.media.jai.util.ImageUtil;

import it.geosolutions.jaiext.interpolators.InterpolationBicubic;
import it.geosolutions.jaiext.interpolators.InterpolationBilinear;
import it.geosolutions.jaiext.interpolators.InterpolationNearest;
import it.geosolutions.jaiext.interpolators.InterpolationNoData;
import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;

public class AffineGeneralOpImage extends AffineOpImage {

    /** ROI extender */
    final static BorderExtender roiExtender = BorderExtender
            .createInstance(BorderExtender.BORDER_ZERO);

    /** This field is not null only if it belongs to the InterpolationNearest class */
    private InterpolationNearest interpN;

    /** This field is not null only if it belongs to the InterpolationBilinear class */
    private InterpolationBilinear interpB;

    /** This field is not null only if it belongs to the InterpolationBicubicNew class */
    private InterpolationBicubic interpBN;

    /** Value describing if the image is a binary one */
    private boolean isBinary;

    /** Subsample bits used for calculating the fractional value between two points */
    private int subsampleBits;

    /** Interpolation object used for storing the interpolator defined by the user */
    private Interpolation interpolator;

    /** Shift value to multiply to the fractional value */
    private int shiftvalue;

    /** Value indicating if roi RasterAccessor should be used on computations */
    private boolean useROIAccessor;

    /** Destination No Data value */
    private double[] destinationNoData;

    /** Width of the interpolation kernel */
    private int interp_width;

    /** Height of the interpolation kernel */
    private int interp_height;

    /** Left padding of the interpolation kernel */
    private int interp_left;

    /** Top padding of the interpolation kernel */
    private int interp_top;

    /** Bottom padding of the interpolation kernel */
    private int interp_bottom;

    /** Right padding of the interpolation kernel */
    private int interp_right;

    /** Value indicating if destination No Data must be set if the pixel is outside the source rectangle */
    private boolean setDestinationNoData;

    /** Destination No Data value for binary image */
    private int black;

    /**
     * Constructor used for interpolator of the class InterpolationNearest
     * 
     * @param nodata
     */
    public AffineGeneralOpImage(RenderedImage source, BorderExtender extender, Map config,
            ImageLayout layout, AffineTransform transform, Interpolation interp,
            boolean useROIAccessor, double[] destinationNoData, boolean setDestinationNoData,
            Range nodata) {
        super(source, extender, configHelper(config, source), layout, transform, interp, destinationNoData);
        affineOpInitialization(source, interp, layout, useROIAccessor, setDestinationNoData, nodata, destinationNoData);
    }

    // Static method used only for binary images
    private static Map configHelper(Map configuration, RenderedImage source) {

        SampleModel sm = source.getSampleModel();

        boolean binaryImage = (sm instanceof MultiPixelPackedSampleModel)
                && (sm.getSampleSize(0) == 1)
                && (sm.getDataType() == DataBuffer.TYPE_BYTE
                        || sm.getDataType() == DataBuffer.TYPE_USHORT || sm.getDataType() == DataBuffer.TYPE_INT);
        if (binaryImage) {
            // Since this operation deals with packed binary data, we do not need
            // to expand the IndexColorModel
            Map config;

            if (configuration == null) {
                config = new RenderingHints(JAI.KEY_REPLACE_INDEX_COLOR_MODEL, Boolean.FALSE);
            } else {
                config = configuration;
                if (!(config.containsKey(JAI.KEY_REPLACE_INDEX_COLOR_MODEL))) {
                    RenderingHints hints = (RenderingHints) configuration;
                    config = (RenderingHints) hints.clone();
                    config.put(JAI.KEY_REPLACE_INDEX_COLOR_MODEL, Boolean.FALSE);
                }
            }

            return config;
        } else {
            return configuration;
        }
    }

    private void affineOpInitialization(RenderedImage source, Interpolation interp,
            ImageLayout layout, boolean useROIAccessor, boolean setDestinationNoData, Range nodata, double[] destNoData) {

        SampleModel sm = source.getSampleModel();

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

        // Propagate source's ColorModel and SampleModel but change tile size.
        if (isBinary) {
            if (layout != null) {
                colorModel = layout.getColorModel(source);
            } else {
                colorModel = source.getColorModel();
            }
            sampleModel = source.getSampleModel()
                    .createCompatibleSampleModel(tileWidth, tileHeight);
        } else {
            // If the source has an IndexColorModel, override the default setting
            // in OpImage. The dest shall have exactly the same SampleModel and
            // ColorModel as the source.
            // Note, in this case, the source should have an integral data type.
            ColorModel srcColorModel = source.getColorModel();
            if (srcColorModel instanceof IndexColorModel) {
                sampleModel = source.getSampleModel().createCompatibleSampleModel(tileWidth,
                        tileHeight);
                colorModel = srcColorModel;
            }
        }

        // Interpolator settings
        interpolator = interp;
        // If both roiBounds and roiIter are not null, they are used in calculation
        double[] destNod = null;
        if (destNoData != null && destNoData.length > 0) {
            destNod = destNoData;
        }
        if (interpolator instanceof InterpolationNearest) {
            interpN = (InterpolationNearest) interpolator;
            interpN.setROIBounds(roiBounds);
            if(destNod == null){
            	destNod = new double[]{interpN.getDestinationNoData()};
            }
        } else if (interpolator instanceof InterpolationBilinear) {
            interpB = (InterpolationBilinear) interpolator;
            interpB.setROIBounds(roiBounds);
            if(destNod == null){
            	destNod = new double[]{interpB.getDestinationNoData()};
            }
        } else if (interpolator instanceof InterpolationBicubic) {
            interpBN = (InterpolationBicubic) interpolator;
            interpBN.setROIBounds(roiBounds);
            if (destNod == null) {
                destNod = new double[]{interpN.getDestinationNoData()};
            }
        } else if (backgroundValues != null) {
            destNod = backgroundValues;
        }
        // Define number of bands
        int numBands = getSampleModel().getNumBands();
        if (destNod == null) {
            destNod = new double[numBands];
        }
        if(destNod.length < numBands){
            double[] tmp = new double[numBands]; 
            Arrays.fill(tmp, destNod[0]);
            destNod = tmp;
        }
        this.destinationNoData = destNod;
        if (interpolator instanceof InterpolationNoData) {
            InterpolationNoData interpolationNoData = (InterpolationNoData) interpolator;
            interpolationNoData.setDestinationNoData(destNod[0]);
            if (nodata != null) {
                hasNoData = true;
                interpolationNoData.setNoDataRange(nodata);
            }
            interpolationNoData.setUseROIAccessor(this.useROIAccessor);
        }

        // this value is used for binary images
        black = ((int) this.destinationNoData[0]) & 1;

        // subsample bits used for the bilinear and bicubic interpolation
        subsampleBits = interp.getSubsampleBitsH();

        // Internal precision required for position calculations
        shiftvalue = 1 << subsampleBits;

        // Interpolation Kernel dimensions
        interp_width = interp.getWidth();
        interp_height = interp.getHeight();
        interp_left = interp.getLeftPadding();
        interp_top = interp.getTopPadding();
        interp_right = interp_width - interp_left - 1;
        interp_bottom = interp_height - interp_top - 1;

        // ROIAccessor can be used only if the interpolator is one of the types:
        // InterpolationNearest, InterpolationBilinear, InterpolationBicubicNew.
        if (hasROI && (interpN != null || interpB != null || interpBN != null)) {
            this.useROIAccessor = useROIAccessor;
        } else {
            this.useROIAccessor = false;
        }
        // destination No Data set
        this.setDestinationNoData = setDestinationNoData;
    }

    public Raster computeTile(int tileX, int tileY) {
        //
        // Create a new WritableRaster to represent this tile.
        //
        Point org = new Point(tileXToX(tileX), tileYToY(tileY));
        WritableRaster dest = createWritableRaster(sampleModel, org);

        //
        // Clip output rectangle to image bounds.
        //
        Rectangle rect = new Rectangle(org.x, org.y, tileWidth, tileHeight);

        // Destination writable raster band number
        int dstBandNum = dest.getNumBands();

        // Destination No Data Array definition for filling the entire destination raster
        // if it is outside the source bounds
        double[] destinationNoDataArray = new double[dstBandNum];
        for (int i = 0; i < dstBandNum; i++) {
            destinationNoDataArray[i] = destinationNoData[i];
        }

        //
        // Clip destination tile against the writable destination
        // area. This is either the layout or a smaller area if
        // no extension is specified.
        //
        Rectangle destRect = rect.intersection(theDest);
        Rectangle destRect1 = rect.intersection(getBounds());
        if ((destRect.width <= 0) || (destRect.height <= 0)) {
            // No area to write
            if (setDestinationNoData) {
                ImageUtil.fillBackground(dest, destRect1, destinationNoDataArray);
            }
            return dest;
        }

        //
        // determine the source rectangle needed to compute the destRect
        //
        Rectangle srcRect = mapDestRect(destRect, 0);
        if (extender == null) {
            srcRect = srcRect.intersection(srcimg);
        } else {
            srcRect = srcRect.intersection(padimg);
        }

        if (!(srcRect.width > 0 && srcRect.height > 0)) {
            if (setDestinationNoData) {
                ImageUtil.fillBackground(dest, destRect1, destinationNoDataArray);
            }
            return dest;
        }

        if (!destRect1.equals(destRect)) {
            // beware that destRect1 contains destRect
            ImageUtil.fillBordersWithBackgroundValues(destRect1, destRect, dest,
                    destinationNoDataArray);
        }

        Raster[] sources = new Raster[1];
        Raster[] rois = new Raster[1];

        // SourceImage
        PlanarImage srcIMG = getSourceImage(0);
        // Get the source and ROI data
        if (extender == null) {
            sources[0] = srcIMG.getData(srcRect);
            
        } else {
            if (srcIMG.getBounds().contains(srcRect)) {
                sources[0] = srcIMG.getData(srcRect);
            } else {
                sources[0] = extendedIMG.getData(srcRect);
            }
        }

        // Compute the destination tile.
        computeRect(sources, dest, destRect);

        // Recycle the source tile
        if (getSourceImage(0).overlapsMultipleTiles(srcRect) && !isBinary) {
            recycleTile(sources[0]);
        }

        return dest;
    }

    /** Method for evaluating the destination image tile with ROI */
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
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
        RandomIter roiIter = null;

        // ROI calculation only if the roi raster is present
        if (hasROI) {
            if (useROIAccessor) {
                if(srcROIImage.getBounds().contains(srcRect)){
                    roi = srcROIImage.getData(srcRect);
                } else{
                    roi = srcROIImgExt.getData(srcRect);
                }
                roiAccessor = new RasterAccessor(roi, srcRect, RasterAccessor.findCompatibleTags(
                        new RenderedImage[] { srcROIImage }, srcROIImage)[0],
                        srcROIImage.getColorModel());
            } else {
                roiIter = RandomIterFactory.create(srcROIImgExt, roiRect, true, true);
            }
        }

        int dataType = dest.getSampleModel().getDataType();
        // If the image is not binary, then for every kind of dataType, the image affine transformation
        // is performed.
        if (!isBinary) {
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                byteLoop(dataType, srcAccessor, destRect, srcRectX, srcRectY, dstAccessor,
                        roiAccessor, roiIter);
                break;
            case DataBuffer.TYPE_INT:
                intLoop(dataType, srcAccessor, destRect, srcRectX, srcRectY, dstAccessor,
                        roiAccessor, roiIter);
                break;
            case DataBuffer.TYPE_SHORT:
                shortLoop(dataType, srcAccessor, destRect, srcRectX, srcRectY, dstAccessor,
                        roiAccessor, roiIter);
                break;
            case DataBuffer.TYPE_USHORT:
                ushortLoop(dataType, srcAccessor, destRect, srcRectX, srcRectY, dstAccessor,
                        roiAccessor, roiIter);
                break;
            case DataBuffer.TYPE_FLOAT:
                floatLoop(dataType, srcAccessor, destRect, srcRectX, srcRectY, dstAccessor,
                        roiAccessor, roiIter);
                break;
            case DataBuffer.TYPE_DOUBLE:
                doubleLoop(dataType, srcAccessor, destRect, srcRectX, srcRectY, dstAccessor,
                        roiAccessor, roiIter);
                break;
            }
        } else {
            dataType = source.getSampleModel().getDataType();
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                byteLoopBinary(dataType, srcAccessor, source, dest, destRect, roi, srcRectX,
                        srcRectY, roiIter);
                break;
            case DataBuffer.TYPE_INT:
                intLoopBinary(dataType, srcAccessor, source, dest, destRect, roi, srcRectX,
                        srcRectY, roiIter);
                break;
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
                ushortLoopBinary(dataType, srcAccessor, source, dest, destRect, roi, srcRectX,
                        srcRectY, roiIter);
                break;
            }
        }
        
        
        // If the RasterAccessor object set up a temporary buffer for the
        // op to write to, tell the RasterAccessor to write that data
        // to the raster, that we're done with it.
        if (dstAccessor.isDataCopy() && !isBinary) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }

    private void byteLoop(int dataType, RasterAccessor src, Rectangle destRect, int srcRectX,
            int srcRectY, RasterAccessor dst, RasterAccessor roiAccessor, RandomIter roiIter) {

        // Creation of the interpolation kernel for interpolators different from the Interpolation type:
        // InterpolationNearest, InterpolationBilinear, InterpolationBicubicNew.
        int[][] samples = new int[interp_height][interp_width];

        // Integral fractional values for a general interpolator
        int xfrac;
        int yfrac;

        // Source region
        float src_rect_x1 = src.getX();
        float src_rect_y1 = src.getY();
        float src_rect_x2 = src_rect_x1 + src.getWidth();
        float src_rect_y2 = src_rect_y1 + src.getHeight();

        // destination image pixel offset
        int dstPixelOffset;
        int dstOffset = 0;

        // Destination and source point stored as point2D object
        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

        // destination data arrays
        byte[][] dstDataArrays = dst.getByteDataArrays();
        // Destination band offsets array
        int[] dstBandOffsets = dst.getBandOffsets();
        // destination pixel stride
        int dstPixelStride = dst.getPixelStride();
        // destination scanline stride
        int dstScanlineStride = dst.getScanlineStride();

        // source image data array
        byte[][] srcDataArrays = src.getByteDataArrays();
        // source image band offsets array
        int[] bandOffsets = src.getBandOffsets();
        // source pixel stride
        int srcPixelStride = src.getPixelStride();
        // source scanline stride
        int srcScanlineStride = src.getScanlineStride();

        // destination band number
        int dst_num_bands = dst.getNumBands();

        // ROI scanline stride
        int roiScanlineStride = 0;
        if (roiAccessor != null) {
            roiScanlineStride = roiAccessor.getScanlineStride();
        }

        // Destination bounds
        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

        // ROI position initialization
        Integer posyROI = null;

        // Fractional array initialization
        Number[] fracValues = new Number[2];

        // Cycle on the destination image y bounds
        for (int y = dst_min_y; y < dst_max_y; y++) {
            // update of the destination pixel offset
            dstPixelOffset = dstOffset;

            // Backward map the first point in the line
            // The energy is at the (pt_x + 0.5, pt_y + 0.5)
            dst_pt.setLocation((double) dst_min_x + 0.5, (double) y + 0.5);
            // Backward mapping of the destination point position
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            float s_x = (float) src_pt.getX();
            float s_y = (float) src_pt.getY();

            // As per definition of bilinear or bicubic interpolation
            if (interpN == null || !(interp instanceof InterpolationNearest)) {
                s_x -= 0.5;
                s_y -= 0.5;
            }

            // Floor to get the integral coordinate
            int s_ix = (int) Math.floor(s_x);
            int s_iy = (int) Math.floor(s_y);

            // Calculation of the fractional values
            float fracx = s_x - (float) s_ix;
            float fracy = s_y - (float) s_iy;
            // Get the new frac values
            xfrac = (int) (fracx * shiftvalue);
            yfrac = (int) (fracy * shiftvalue);

            // Store of the fractional value inside an array
            if (dataType < DataBuffer.TYPE_FLOAT) {
                fracValues[0] = xfrac;
                fracValues[1] = yfrac;
            } else {
                fracValues[0] = fracx;
                fracValues[1] = fracy;
            }

            // Translate to/from SampleModel space & Raster space
            int posy = (s_iy - srcRectY) * srcScanlineStride;
            int posx = (s_ix - srcRectX) * srcPixelStride;

            // Conversion if the fractional values to integer ones for Nearest-Neighbor interpolation
            int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
            int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

            // If roiAccessor is present, the y position on the roi image is calculated
            if (roiAccessor != null) {
                posyROI = (s_iy - srcRectY) * roiScanlineStride;
            }

            // Cycle on the destination image x bounds
            for (int x = dst_min_x; x < dst_max_x; x++) {
                //
                // Check against the source rectangle
                //
                if ((s_ix >= src_rect_x1 + interp_left) && (s_ix < (src_rect_x2 - interp_right))
                        && (s_iy >= src_rect_y1 + interp_top)
                        && (s_iy < (src_rect_y2 - interp_bottom))) {
                    // Cycle on all the image bands
                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                        // initial value for the output sample
                        float result = 0;
                        int s = 0;

                        int posyy = posy + bandOffsets[k2];

                        // Control for using the defined interpolator
                        if (interpN != null) {
                            result = interpN.interpolate(src, k2, dst_num_bands, posx, posyy,
                                    posyROI, roiAccessor, roiIter, false).intValue();
                        } else if (interpB != null) {
                            result = interpB.interpolate(src, k2, dst_num_bands, posx, posyy,
                                    fracValues, posyROI, roiAccessor, roiIter, false).intValue();
                        } else if (interpBN != null) {
                            result = interpBN.interpolate(src, k2, dst_num_bands, posx, posyy,
                                    fracValues, posyROI, roiAccessor, roiIter, false).intValue();
                            // Case of general interpolator (ROI and No Data not supported)
                        } else {
                            // Source data array of the selected band
                            byte[] srcData = srcDataArrays[k2];
                            // Offset of the selected band
                            int tmp = bandOffsets[k2];

                            // Get the pixels required for this interpolation
                            int start = interp_left * srcPixelStride + interp_top
                                    * srcScanlineStride;
                            // Initial array index of the selected pixel
                            start = posx + posy - start;
                            // Initial value for the counter for filling the interpolation kernel
                            int countH = 0, countV = 0;
                            // cycle on the x and y directions
                            for (int i = 0; i < interp_height; i++) {
                                int startY = start;
                                for (int j = 0; j < interp_width; j++) {
                                    // interpolation kernel value addition
                                    samples[countV][countH++] = srcData[start + tmp] & 0xff;
                                    // update of the array index on the x direction
                                    start += srcPixelStride;
                                }
                                // Update of the counter on the y axis and reset of the one on the X axis
                                countV++;
                                countH = 0;
                                // update of the array index on the y direction
                                start = startY + srcScanlineStride;
                            }

                            // Get the new frac values
                            xfrac = (int) (fracx * shiftvalue);
                            yfrac = (int) (fracy * shiftvalue);

                            // Do the interpolation
                            result = interp.interpolate(samples, xfrac, yfrac);

                        }
                        if (interpN == null || !(interp instanceof InterpolationNearest)) {
                            // Clamp
                            if (result < 0.5f) {
                                s = 0;
                            } else if (result > 254.5f) {
                                s = 255;
                            } else {
                                s = (int) result;
                            }
                        } else {
                            s = (int) result;
                        }

                        // Write the result
                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (byte) (s & 0xff);
                    }
                    // If the pixel is outside bounds and is possible to set
                } else if (setDestinationNoData) {
                    for (int k = 0; k < dst_num_bands; k++) {
                        dstDataArrays[k][dstPixelOffset + dstBandOffsets[k]] = (byte) destinationNoData[k];
                    }

                }

                // walk
                if (interpN == null || !(interp instanceof InterpolationNearest)) {
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
                    // If interpolation Nearest is used, then the
                } else {
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
                }

                // Translate to/from SampleModel space & Raster space
                posy = (s_iy - srcRectY) * srcScanlineStride;
                posx = (s_ix - srcRectX) * srcPixelStride;

                if (roiAccessor != null) {
                    posyROI = (s_iy - srcRectY) * roiScanlineStride;
                }

                // Go to next pixel
                dstPixelOffset += dstPixelStride;
            }
            // Go to the next line in the destination rectangle
            dstOffset += dstScanlineStride;
        }
    }

    private void byteLoopBinary(int dataType, RasterAccessor src, Raster source,
            WritableRaster dest, Rectangle destRect, Raster roi, int srcRectX, int srcRectY, RandomIter roiIter) {

        float src_rect_x1 = source.getMinX();
        float src_rect_y1 = source.getMinY();
        float src_rect_x2 = src_rect_x1 + source.getWidth();
        float src_rect_y2 = src_rect_y1 + source.getHeight();

        MultiPixelPackedSampleModel sourceSM = (MultiPixelPackedSampleModel) source
                .getSampleModel();

        DataBufferByte sourceDB = (DataBufferByte) source.getDataBuffer();

        int sourceTransX = source.getSampleModelTranslateX();
        int sourceTransY = source.getSampleModelTranslateY();
        int sourceDataBitOffset = sourceSM.getDataBitOffset();
        int sourceScanlineStride = sourceSM.getScanlineStride();
        int sourcePixelStride = sourceSM.getPixelBitStride();

        MultiPixelPackedSampleModel destSM = (MultiPixelPackedSampleModel) dest.getSampleModel();

        DataBufferByte destDB = (DataBufferByte) dest.getDataBuffer();

        int destTransX = dest.getSampleModelTranslateX();
        int destTransY = dest.getSampleModelTranslateY();
        int destDataBitOffset = destSM.getDataBitOffset();
        int destScanlineStride = destSM.getScanlineStride();

        byte[] sourceData = sourceDB.getData();
        Number[] sourceDataNum = new Number[sourceData.length];
        for (int i = 0; i < sourceData.length; i++) {
            sourceDataNum[i] = sourceData[i];
        }
        int sourceDBOffset = sourceDB.getOffset();

        byte[] destData = destDB.getData();
        int destDBOffset = destDB.getOffset();

        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

        // Output value initialization
        int s = 0;

        // pixel value coordinates initialization
        int[] coordinates = new int[2];

        int[] roiData = null;

        int roiDBOffset = 0;

        MultiPixelPackedSampleModel roiSM = null;
        // int roiTransX =0;
        int roiTransY = 0;
        // int roiDataBitOffset =0;
        int roiScanlineStride = 0;

        if (roi != null) {
            roiSM = (MultiPixelPackedSampleModel) roi.getSampleModel();

            // roiTransX = roi.getSampleModelTranslateX();
            roiTransY = roi.getSampleModelTranslateY();
            // roiDataBitOffset = roiSM.getDataBitOffset();
            roiScanlineStride = roiSM.getScanlineStride();

            DataBuffer roiDB = roi.getDataBuffer();

            roiDBOffset = roiDB.getOffset();

            DataBufferByte roiDBByte = (DataBufferByte) roiDB;
            byte[] roiDataB = roiDBByte.getData();
            roiData = new int[roiDataB.length];
            for (int ii = 0; ii < roiDataB.length; ii++) {
                roiData[ii] = roiDataB[ii];
            }
        }

        for (int y = dst_min_y; y < dst_max_y; y++) {
            // Backward map the first point in the line
            // The energy is at the (pt_x + 0.5, pt_y + 0.5)
            dst_pt.setLocation((double) dst_min_x + 0.5, (double) y + 0.5);
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            float s_x = (float) src_pt.getX();
            float s_y = (float) src_pt.getY();

            // Floor to get the integral coordinate
            int s_ix = (int) Math.floor(s_x);
            int s_iy = (int) Math.floor(s_y);

            double fracx = s_x - (double) s_ix;
            double fracy = s_y - (double) s_iy;

            // Get the new frac values
            int xfrac = (int) (fracx * shiftvalue);
            int yfrac = (int) (fracy * shiftvalue);

            int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
            int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

            int destYOffset = (y - destTransY) * destScanlineStride + destDBOffset;
            int destXOffset = destDataBitOffset + (dst_min_x - destTransX);

            int sourceYOffset = (s_iy - sourceTransY) * sourceScanlineStride + sourceDBOffset;
            // int sourceXOffset = s_ix - sourceTransX + sourceDataBitOffset;

            int roiYOffset = (s_iy - roiTransY) * roiScanlineStride + roiDBOffset;

            // Cycle on the destination image x bounds
            for (int x = dst_min_x; x < dst_max_x; x++) {

                sourceYOffset = (s_iy - sourceTransY) * sourceScanlineStride + sourceDBOffset;
                roiYOffset = (s_iy - roiTransY) * roiScanlineStride + roiDBOffset;
                int dindex = 0;
                // int delement = 0;

                //
                // Check against the source rectangle
                //
                if ((s_ix >= src_rect_x1 + interp_left) && (s_ix < (src_rect_x2 - interp_right))
                        && (s_iy >= src_rect_y1 + interp_top)
                        && (s_iy < (src_rect_y2 - interp_bottom))) {

                    coordinates[0] = src.getX() + (s_ix - srcRectX) * sourcePixelStride;
                    ;
                    coordinates[1] = src.getY() + ((s_iy - srcRectY) * sourceScanlineStride)
                            / sourceScanlineStride;

                    int xNextBitNo = sourceDataBitOffset + (s_ix + 1 - sourceTransX);

                    if (interpN != null) {
                        s = interpN.interpolateBinary(xNextBitNo, sourceDataNum, sourceYOffset,
                                sourceScanlineStride, coordinates, roiData, roiYOffset,
                                roiScanlineStride, roiIter);
                    } else if (interpB != null) {
                        s = interpB.interpolateBinary(xNextBitNo, sourceDataNum, xfrac, yfrac,
                                sourceYOffset, sourceScanlineStride, coordinates, roiData,
                                roiYOffset, roiScanlineStride, roiIter);
                    } else if (interpBN != null) {
                        s = interpBN.interpolateBinary(xNextBitNo, sourceDataNum, xfrac, yfrac,
                                sourceYOffset, sourceScanlineStride, coordinates, roiData,
                                roiYOffset, roiScanlineStride, roiIter);
                    } else {
                        throw new UnsupportedOperationException(
                                "Binary interpolation not supported for interpolators different from"
                                        + "InterpolatorNearestNew,InterpolatorBinaryNew,InterpolatorBicubicNew");
                    }
                } else if (setDestinationNoData) {
                    s = black;
                }

                dindex = destYOffset + (destXOffset >> 3);
                int dshift = 7 - (destXOffset & 7);
                // delement = destData[dindex];
                // delement |= s << dshift;
                //
                // destData[dindex] = (byte) delement;

                if (s == 1) {
                    destData[dindex] |= (0x01 << dshift);
                } else {
                    destData[dindex] &= (0xff - (0x01 << dshift));
                }

                // walk
                if (interpN == null || !(interp instanceof InterpolationNearest)) {
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
                    // If interpolation Nearest is used, then the
                } else {
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
                }
                ++destXOffset;
            }
        }
    }

    private void ushortLoop(int dataType, RasterAccessor src, Rectangle destRect, int srcRectX,
            int srcRectY, RasterAccessor dst, RasterAccessor roiAccessor, RandomIter roiIter) {

        // Creation of the interpolation kernel for interpolators different from the Interpolation type:
        // InterpolationNearest, InterpolationBilinear, InterpolationBicubicNew.
        int[][] samples = new int[interp_height][interp_width];

        // Integral fractional values for a general interpolator
        int xfrac;
        int yfrac;

        // Source region
        float src_rect_x1 = src.getX();
        float src_rect_y1 = src.getY();
        float src_rect_x2 = src_rect_x1 + src.getWidth();
        float src_rect_y2 = src_rect_y1 + src.getHeight();

        // destination image pixel offset
        int dstPixelOffset;
        int dstOffset = 0;

        // Destination and source point stored as point2D object
        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

        // destination data arrays
        short[][] dstDataArrays = dst.getShortDataArrays();
        // Destination band offsets array
        int[] dstBandOffsets = dst.getBandOffsets();
        // destination pixel stride
        int dstPixelStride = dst.getPixelStride();
        // destination scanline stride
        int dstScanlineStride = dst.getScanlineStride();

        // source image data array
        short[][] srcDataArrays = src.getShortDataArrays();
        // source image band offsets array
        int[] bandOffsets = src.getBandOffsets();
        // source pixel stride
        int srcPixelStride = src.getPixelStride();
        // source scanline stride
        int srcScanlineStride = src.getScanlineStride();

        // destination band number
        int dst_num_bands = dst.getNumBands();

        // ROI scanline stride
        int roiScanlineStride = 0;
        if (roiAccessor != null) {
            roiScanlineStride = roiAccessor.getScanlineStride();
        }

        // Destination bounds
        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

        // ROI position initialization
        Integer posyROI = null;

        // Fractional array initialization
        Number[] fracValues = new Number[2];

        // Cycle on the destination image y bounds
        for (int y = dst_min_y; y < dst_max_y; y++) {
            // update of the destination pixel offset
            dstPixelOffset = dstOffset;

            // Backward map the first point in the line
            // The energy is at the (pt_x + 0.5, pt_y + 0.5)
            dst_pt.setLocation((double) dst_min_x + 0.5, (double) y + 0.5);
            // Backward mapping of the destination point position
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            float s_x = (float) src_pt.getX();
            float s_y = (float) src_pt.getY();

            // As per definition of bilinear or bicubic interpolation
            if (interpN == null || !(interp instanceof InterpolationNearest)) {
                s_x -= 0.5;
                s_y -= 0.5;
            }

            // Floor to get the integral coordinate
            int s_ix = (int) Math.floor(s_x);
            int s_iy = (int) Math.floor(s_y);

            // Calculation of the fractional values
            float fracx = s_x - (float) s_ix;
            float fracy = s_y - (float) s_iy;
            // Get the new frac values
            xfrac = (int) (fracx * shiftvalue);
            yfrac = (int) (fracy * shiftvalue);

            // Store of the fractional value inside an array
            if (dataType < DataBuffer.TYPE_FLOAT) {
                fracValues[0] = xfrac;
                fracValues[1] = yfrac;
            } else {
                fracValues[0] = fracx;
                fracValues[1] = fracy;
            }

            // Translate to/from SampleModel space & Raster space
            int posy = (s_iy - srcRectY) * srcScanlineStride;
            int posx = (s_ix - srcRectX) * srcPixelStride;

            // Conversion if the fractional values to integer ones for Nearest-Neighbor interpolation
            int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
            int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

            // If roiAccessor is present, the y position on the roi image is calculated
            if (roiAccessor != null) {
                posyROI = (s_iy - srcRectY) * roiScanlineStride;
            }

            // Cycle on the destination image x bounds
            for (int x = dst_min_x; x < dst_max_x; x++) {
                //
                // Check against the source rectangle
                //
                if ((s_ix >= src_rect_x1 + interp_left) && (s_ix < (src_rect_x2 - interp_right))
                        && (s_iy >= src_rect_y1 + interp_top)
                        && (s_iy < (src_rect_y2 - interp_bottom))) {
                    // Cycle on all the image bands
                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                        // initial value for the output sample
                        float result = 0;
                        int s = 0;

                        int posyy = posy + bandOffsets[k2];

                        // Control for using the defined interpolator
                        if (interpN != null) {
                            result = interpN.interpolate(src, k2, dst_num_bands, posx, posyy,
                                    posyROI, roiAccessor, roiIter, false).intValue();
                        } else if (interpB != null) {
                            result = interpB.interpolate(src, k2, dst_num_bands, posx, posyy,
                                    fracValues, posyROI, roiAccessor, roiIter, false).intValue();
                        } else if (interpBN != null) {
                            result = interpBN.interpolate(src, k2, dst_num_bands, posx, posyy,
                                    fracValues, posyROI, roiAccessor, roiIter, false).intValue();
                            // Case of general interpolator (ROI and No Data not supported)
                        } else {
                            // Source data array of the selected band
                            short[] srcData = srcDataArrays[k2];
                            // Offset of the selected band
                            int tmp = bandOffsets[k2];

                            // Get the pixels required for this interpolation
                            int start = interp_left * srcPixelStride + interp_top
                                    * srcScanlineStride;
                            // Initial array index of the selected pixel
                            start = posx + posy - start;
                            // Initial value for the counter for filling the interpolation kernel
                            int countH = 0, countV = 0;
                            // cycle on the x and y directions
                            for (int i = 0; i < interp_height; i++) {
                                int startY = start;
                                for (int j = 0; j < interp_width; j++) {
                                    // interpolation kernel value addition
                                    samples[countV][countH++] = srcData[start + tmp] & 0xffff;
                                    // update of the array index on the x direction
                                    start += srcPixelStride;
                                }
                                // Update of the counter on the y axis and reset of the one on the X axis
                                countV++;
                                countH = 0;
                                // update of the array index on the y direction
                                start = startY + srcScanlineStride;
                            }

                            // Get the new frac values
                            xfrac = (int) (fracx * shiftvalue);
                            yfrac = (int) (fracy * shiftvalue);

                            // Do the interpolation
                            result = interp.interpolate(samples, xfrac, yfrac);
                        }
                        if (interpN == null || !(interp instanceof InterpolationNearest)) {
                            // Clamp
                            if (result < 0.0f) {
                                s = 0;
                            } else if (result > USHORT_MAX_VALUE) {
                                s = USHORT_MAX_VALUE;
                            } else {
                                s = (int) result;
                            }
                        } else {
                            s = (int) result;
                        }

                        // Write the result
                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (short) (s & 0xffff);
                    }
                    // If the pixel is outside bounds and is possible to set
                } else if (setDestinationNoData) {
                    for (int k = 0; k < dst_num_bands; k++) {
                        dstDataArrays[k][dstPixelOffset + dstBandOffsets[k]] = (short) destinationNoData[k];
                    }
                }

                // walk
                if (interpN == null || !(interp instanceof InterpolationNearest)) {
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
                    // If interpolation Nearest is used, then the
                } else {
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
                }

                // Translate to/from SampleModel space & Raster space
                posy = (s_iy - srcRectY) * srcScanlineStride;
                posx = (s_ix - srcRectX) * srcPixelStride;

                if (roiAccessor != null) {
                    posyROI = (s_iy - srcRectY) * roiScanlineStride;
                }

                // Go to next pixel
                dstPixelOffset += dstPixelStride;
            }
            // Go to the next line in the destination rectangle
            dstOffset += dstScanlineStride;
        }
    }

    private void ushortLoopBinary(int dataType, RasterAccessor src, Raster source,
            WritableRaster dest, Rectangle destRect, Raster roi, int srcRectX, int srcRectY, RandomIter roiIter) {

        float src_rect_x1 = source.getMinX();
        float src_rect_y1 = source.getMinY();
        float src_rect_x2 = src_rect_x1 + source.getWidth();
        float src_rect_y2 = src_rect_y1 + source.getHeight();

        MultiPixelPackedSampleModel sourceSM = (MultiPixelPackedSampleModel) source
                .getSampleModel();

        DataBuffer sourceDBUS = source.getDataBuffer();
        DataBufferUShort sourceDB = (DataBufferUShort) sourceDBUS;

        int sourceTransX = source.getSampleModelTranslateX();
        int sourceTransY = source.getSampleModelTranslateY();
        int sourceDataBitOffset = sourceSM.getDataBitOffset();
        int sourceScanlineStride = sourceSM.getScanlineStride();
        int sourcePixelStride = sourceSM.getPixelBitStride();

        MultiPixelPackedSampleModel destSM = (MultiPixelPackedSampleModel) dest.getSampleModel();

        DataBufferUShort destDB = (DataBufferUShort) dest.getDataBuffer();

        int destTransX = dest.getSampleModelTranslateX();
        int destTransY = dest.getSampleModelTranslateY();
        int destDataBitOffset = destSM.getDataBitOffset();
        int destScanlineStride = destSM.getScanlineStride();

        short[] sourceData = sourceDB.getData();
        Number[] sourceDataNum = new Number[sourceData.length];
        for (int i = 0; i < sourceData.length; i++) {
            sourceDataNum[i] = sourceData[i];
        }
        int sourceDBOffset = sourceDB.getOffset();

        short[] destData = destDB.getData();
        int destDBOffset = destDB.getOffset();

        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

        // Output value initialization
        int s = 0;

        // pixel value coordinates initialization
        int[] coordinates = new int[2];

        int[] roiData = null;

        int roiDBOffset = 0;

        MultiPixelPackedSampleModel roiSM = null;
        // int roiTransX =0;
        int roiTransY = 0;
        // int roiDataBitOffset =0;
        int roiScanlineStride = 0;

        if (roi != null) {
            roiSM = (MultiPixelPackedSampleModel) roi.getSampleModel();

            // roiTransX = roi.getSampleModelTranslateX();
            roiTransY = roi.getSampleModelTranslateY();
            // roiDataBitOffset = roiSM.getDataBitOffset();
            roiScanlineStride = roiSM.getScanlineStride();

            DataBuffer roiDB = roi.getDataBuffer();

            roiDBOffset = roiDB.getOffset();

            DataBufferByte roiDBByte = (DataBufferByte) roiDB;
            byte[] roiDataB = roiDBByte.getData();
            roiData = new int[roiDataB.length];
            for (int ii = 0; ii < roiDataB.length; ii++) {
                roiData[ii] = roiDataB[ii];
            }
        }

        for (int y = dst_min_y; y < dst_max_y; y++) {
            // Backward map the first point in the line
            // The energy is at the (pt_x + 0.5, pt_y + 0.5)
            dst_pt.setLocation((double) dst_min_x + 0.5, (double) y + 0.5);
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            float s_x = (float) src_pt.getX();
            float s_y = (float) src_pt.getY();

            // Floor to get the integral coordinate
            int s_ix = (int) Math.floor(s_x);
            int s_iy = (int) Math.floor(s_y);

            double fracx = s_x - (double) s_ix;
            double fracy = s_y - (double) s_iy;

            // Get the new frac values
            int xfrac = (int) (fracx * shiftvalue);
            int yfrac = (int) (fracy * shiftvalue);

            int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
            int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

            int destYOffset = (y - destTransY) * destScanlineStride + destDBOffset;
            int destXOffset = destDataBitOffset + (dst_min_x - destTransX);

            int sourceYOffset = (s_iy - sourceTransY) * sourceScanlineStride + sourceDBOffset;
            // int sourceXOffset = s_ix - sourceTransX + sourceDataBitOffset;

            int roiYOffset = (s_iy - roiTransY) * roiScanlineStride + roiDBOffset;

            // Cycle on the destination image x bounds
            for (int x = dst_min_x; x < dst_max_x; x++) {

                sourceYOffset = (s_iy - sourceTransY) * sourceScanlineStride + sourceDBOffset;
                roiYOffset = (s_iy - roiTransY) * roiScanlineStride + roiDBOffset;

                int dindex = 0;
                int dshift;
                // int delement = 0;

                //
                // Check against the source rectangle
                //
                if ((s_ix >= src_rect_x1 + interp_left) && (s_ix < (src_rect_x2 - interp_right))
                        && (s_iy >= src_rect_y1 + interp_top)
                        && (s_iy < (src_rect_y2 - interp_bottom))) {

                    coordinates[0] = src.getX() + (s_ix - srcRectX) * sourcePixelStride;
                    ;
                    coordinates[1] = src.getY() + ((s_iy - srcRectY) * sourceScanlineStride)
                            / sourceScanlineStride;

                    int xNextBitNo = sourceDataBitOffset + (s_ix + 1 - sourceTransX);
                    if (interpN != null) {
                        s = interpN.interpolateBinary(xNextBitNo, sourceDataNum, sourceYOffset,
                                sourceScanlineStride, coordinates, roiData, roiYOffset,
                                roiScanlineStride, roiIter);
                    } else if (interpB != null) {
                        s = interpB.interpolateBinary(xNextBitNo, sourceDataNum, xfrac, yfrac,
                                sourceYOffset, sourceScanlineStride, coordinates, roiData,
                                roiYOffset, roiScanlineStride, roiIter);
                    } else if (interpBN != null) {
                        s = interpBN.interpolateBinary(xNextBitNo, sourceDataNum, xfrac, yfrac,
                                sourceYOffset, sourceScanlineStride, coordinates, roiData,
                                roiYOffset, roiScanlineStride, roiIter);
                    } else {
                        throw new UnsupportedOperationException(
                                "Binary interpolation not supported for interpolators different from"
                                        + "InterpolatorNearestNew,InterpolatorBinaryNew,InterpolatorBicubicNew");
                    }

                } else if (setDestinationNoData) {
                    s = black;
                }

                dindex = destYOffset + (destXOffset >> 4);
                dshift = 15 - (destXOffset & 15);
                // delement = destData[dindex];
                // delement |= s << dshift;
                //
                // destData[dindex] = (short) delement;

                if (s == 1) {
                    destData[dindex] |= (0x01 << dshift);
                } else {
                    destData[dindex] &= (0xffff - (0x01 << dshift));
                }

                // walk
                if (interpN == null || !(interp instanceof InterpolationNearest)) {
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
                    // If interpolation Nearest is used, then the
                } else {
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
                }
                ++destXOffset;
            }
        }
    }

    private void shortLoop(int dataType, RasterAccessor src, Rectangle destRect, int srcRectX,
            int srcRectY, RasterAccessor dst, RasterAccessor roiAccessor, RandomIter roiIter) {

        // Creation of the interpolation kernel for interpolators different from the Interpolation type:
        // InterpolationNearest, InterpolationBilinear, InterpolationBicubicNew.
        int[][] samples = new int[interp_height][interp_width];

        // Integral fractional values for a general interpolator
        int xfrac;
        int yfrac;

        // Source region
        float src_rect_x1 = src.getX();
        float src_rect_y1 = src.getY();
        float src_rect_x2 = src_rect_x1 + src.getWidth();
        float src_rect_y2 = src_rect_y1 + src.getHeight();

        // destination image pixel offset
        int dstPixelOffset;
        int dstOffset = 0;

        // Destination and source point stored as point2D object
        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

        // destination data arrays
        short[][] dstDataArrays = dst.getShortDataArrays();
        // Destination band offsets array
        int[] dstBandOffsets = dst.getBandOffsets();
        // destination pixel stride
        int dstPixelStride = dst.getPixelStride();
        // destination scanline stride
        int dstScanlineStride = dst.getScanlineStride();

        // source image data array
        short[][] srcDataArrays = src.getShortDataArrays();
        // source image band offsets array
        int[] bandOffsets = src.getBandOffsets();
        // source pixel stride
        int srcPixelStride = src.getPixelStride();
        // source scanline stride
        int srcScanlineStride = src.getScanlineStride();

        // destination band number
        int dst_num_bands = dst.getNumBands();

        // ROI scanline stride
        int roiScanlineStride = 0;
        if (roiAccessor != null) {
            roiScanlineStride = roiAccessor.getScanlineStride();
        }

        // Destination bounds
        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

        // ROI position initialization
        Integer posyROI = null;

        // Fractional array initialization
        Number[] fracValues = new Number[2];

        // Cycle on the destination image y bounds
        for (int y = dst_min_y; y < dst_max_y; y++) {
            // update of the destination pixel offset
            dstPixelOffset = dstOffset;

            // Backward map the first point in the line
            // The energy is at the (pt_x + 0.5, pt_y + 0.5)
            dst_pt.setLocation((double) dst_min_x + 0.5, (double) y + 0.5);
            // Backward mapping of the destination point position
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            float s_x = (float) src_pt.getX();
            float s_y = (float) src_pt.getY();

            // As per definition of bilinear or bicubic interpolation
            if (interpN == null || !(interp instanceof InterpolationNearest)) {
                s_x -= 0.5;
                s_y -= 0.5;
            }

            // Floor to get the integral coordinate
            int s_ix = (int) Math.floor(s_x);
            int s_iy = (int) Math.floor(s_y);

            // Calculation of the fractional values
            float fracx = s_x - (float) s_ix;
            float fracy = s_y - (float) s_iy;
            // Get the new frac values
            xfrac = (int) (fracx * shiftvalue);
            yfrac = (int) (fracy * shiftvalue);

            // Store of the fractional value inside an array
            if (dataType < DataBuffer.TYPE_FLOAT) {
                fracValues[0] = xfrac;
                fracValues[1] = yfrac;
            } else {
                fracValues[0] = fracx;
                fracValues[1] = fracy;
            }

            // Translate to/from SampleModel space & Raster space
            int posy = (s_iy - srcRectY) * srcScanlineStride;
            int posx = (s_ix - srcRectX) * srcPixelStride;

            // Conversion if the fractional values to integer ones for Nearest-Neighbor interpolation
            int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
            int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

            // If roiAccessor is present, the y position on the roi image is calculated
            if (roiAccessor != null) {
                posyROI = (s_iy - srcRectY) * roiScanlineStride;
            }

            // Cycle on the destination image x bounds
            for (int x = dst_min_x; x < dst_max_x; x++) {
                //
                // Check against the source rectangle
                //
                if ((s_ix >= src_rect_x1 + interp_left) && (s_ix < (src_rect_x2 - interp_right))
                        && (s_iy >= src_rect_y1 + interp_top)
                        && (s_iy < (src_rect_y2 - interp_bottom))) {
                    // Cycle on all the image bands
                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                        // initial value for the output sample
                        float result = 0;
                        int s = 0;

                        int posyy = posy + bandOffsets[k2];

                        // Control for using the defined interpolator
                        if (interpN != null) {
                            result = interpN.interpolate(src, k2, dst_num_bands, posx, posyy,
                                    posyROI, roiAccessor, roiIter, false).intValue();
                        } else if (interpB != null) {
                            result = interpB.interpolate(src, k2, dst_num_bands, posx, posyy,
                                    fracValues, posyROI, roiAccessor, roiIter, false).intValue();
                        } else if (interpBN != null) {
                            result = interpBN.interpolate(src, k2, dst_num_bands, posx, posyy,
                                    fracValues, posyROI, roiAccessor, roiIter, false).intValue();
                            // Case of general interpolator (ROI and No Data not supported)
                        } else {
                            // Source data array of the selected band
                            short[] srcData = srcDataArrays[k2];
                            // Offset of the selected band
                            int tmp = bandOffsets[k2];

                            // Get the pixels required for this interpolation
                            int start = interp_left * srcPixelStride + interp_top
                                    * srcScanlineStride;
                            // Initial array index of the selected pixel
                            start = posx + posy - start;
                            // Initial value for the counter for filling the interpolation kernel
                            int countH = 0, countV = 0;
                            // cycle on the x and y directions
                            for (int i = 0; i < interp_height; i++) {
                                int startY = start;
                                for (int j = 0; j < interp_width; j++) {
                                    // interpolation kernel value addition
                                    samples[countV][countH++] = srcData[start + tmp];
                                    // update of the array index on the x direction
                                    start += srcPixelStride;
                                }
                                // Update of the counter on the y axis and reset of the one on the X axis
                                countV++;
                                countH = 0;
                                // update of the array index on the y direction
                                start = startY + srcScanlineStride;
                            }

                            // Get the new frac values
                            xfrac = (int) (fracx * shiftvalue);
                            yfrac = (int) (fracy * shiftvalue);

                            // Do the interpolation
                            result = interp.interpolate(samples, xfrac, yfrac);
                        }

                        // Clamp
                        if (result < ((float) Short.MIN_VALUE)) {
                            s = Short.MIN_VALUE;
                        } else if (result > ((float) Short.MAX_VALUE)) {
                            s = Short.MAX_VALUE;
                        } else {
                            s = (int) result;
                        }
                        // Write the result
                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (short) s;
                    }
                    // If the pixel is outside bounds and is possible to set
                } else if (setDestinationNoData) {
                    for (int k = 0; k < dst_num_bands; k++) {
                        dstDataArrays[k][dstPixelOffset + dstBandOffsets[k]] = (short) destinationNoData[k];
                    }
                }

                // walk
                if (interpN == null || !(interp instanceof InterpolationNearest)) {
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
                    // If interpolation Nearest is used, then the
                } else {
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
                }

                // Translate to/from SampleModel space & Raster space
                posy = (s_iy - srcRectY) * srcScanlineStride;
                posx = (s_ix - srcRectX) * srcPixelStride;

                if (roiAccessor != null) {
                    posyROI = (s_iy - srcRectY) * roiScanlineStride;
                }

                // Go to next pixel
                dstPixelOffset += dstPixelStride;
            }
            // Go to the next line in the destination rectangle
            dstOffset += dstScanlineStride;
        }

    }

    private void intLoop(int dataType, RasterAccessor src, Rectangle destRect, int srcRectX,
            int srcRectY, RasterAccessor dst, RasterAccessor roiAccessor, RandomIter roiIter) {

        // Creation of the interpolation kernel for interpolators different from the Interpolation type:
        // InterpolationNearest, InterpolationBilinear, InterpolationBicubicNew.
        int[][] samples = new int[interp_height][interp_width];

        // Integral fractional values for a general interpolator
        int xfrac;
        int yfrac;

        // Source region
        float src_rect_x1 = src.getX();
        float src_rect_y1 = src.getY();
        float src_rect_x2 = src_rect_x1 + src.getWidth();
        float src_rect_y2 = src_rect_y1 + src.getHeight();

        // destination image pixel offset
        int dstPixelOffset;
        int dstOffset = 0;

        // Destination and source point stored as point2D object
        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

        // destination data arrays
        int[][] dstDataArrays = dst.getIntDataArrays();
        // Destination band offsets array
        int[] dstBandOffsets = dst.getBandOffsets();
        // destination pixel stride
        int dstPixelStride = dst.getPixelStride();
        // destination scanline stride
        int dstScanlineStride = dst.getScanlineStride();

        // source image data array
        int[][] srcDataArrays = src.getIntDataArrays();
        // source image band offsets array
        int[] bandOffsets = src.getBandOffsets();
        // source pixel stride
        int srcPixelStride = src.getPixelStride();
        // source scanline stride
        int srcScanlineStride = src.getScanlineStride();

        // destination band number
        int dst_num_bands = dst.getNumBands();

        // ROI scanline stride
        int roiScanlineStride = 0;
        if (roiAccessor != null) {
            roiScanlineStride = roiAccessor.getScanlineStride();
        }

        // Destination bounds
        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

        // ROI position initialization
        Integer posyROI = null;

        // Fractional array initialization
        Number[] fracValues = new Number[2];

        // Cycle on the destination image y bounds
        for (int y = dst_min_y; y < dst_max_y; y++) {
            // update of the destination pixel offset
            dstPixelOffset = dstOffset;

            // Backward map the first point in the line
            // The energy is at the (pt_x + 0.5, pt_y + 0.5)
            dst_pt.setLocation((double) dst_min_x + 0.5, (double) y + 0.5);
            // Backward mapping of the destination point position
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            float s_x = (float) src_pt.getX();
            float s_y = (float) src_pt.getY();

            // As per definition of bilinear or bicubic interpolation
            if (interpN == null || !(interp instanceof InterpolationNearest)) {
                s_x -= 0.5;
                s_y -= 0.5;
            }

            // Floor to get the integral coordinate
            int s_ix = (int) Math.floor(s_x);
            int s_iy = (int) Math.floor(s_y);

            // Calculation of the fractional values
            float fracx = s_x - (float) s_ix;
            float fracy = s_y - (float) s_iy;
            // Get the new frac values
            xfrac = (int) (fracx * shiftvalue);
            yfrac = (int) (fracy * shiftvalue);

            // Store of the fractional value inside an array
            if (dataType < DataBuffer.TYPE_FLOAT) {
                fracValues[0] = xfrac;
                fracValues[1] = yfrac;
            } else {
                fracValues[0] = fracx;
                fracValues[1] = fracy;
            }

            // Translate to/from SampleModel space & Raster space
            int posy = (s_iy - srcRectY) * srcScanlineStride;
            int posx = (s_ix - srcRectX) * srcPixelStride;

            // Conversion if the fractional values to integer ones for Nearest-Neighbor interpolation
            int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
            int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

            // If roiAccessor is present, the y position on the roi image is calculated
            if (roiAccessor != null) {
                posyROI = (s_iy - srcRectY) * roiScanlineStride;
            }

            // Cycle on the destination image x bounds
            for (int x = dst_min_x; x < dst_max_x; x++) {
                //
                // Check against the source rectangle
                //
                if ((s_ix >= src_rect_x1 + interp_left) && (s_ix < (src_rect_x2 - interp_right))
                        && (s_iy >= src_rect_y1 + interp_top)
                        && (s_iy < (src_rect_y2 - interp_bottom))) {
                    // Cycle on all the image bands
                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                        // initial value for the output sample
                        float result = 0;
                        int s = 0;

                        int posyy = posy + bandOffsets[k2];

                        // Control for using the defined interpolator
                        if (interpN != null) {
                            result = interpN.interpolate(src, k2, dst_num_bands, posx, posyy,
                                    posyROI, roiAccessor, roiIter, false).intValue();
                        } else if (interpB != null) {
                            result = interpB.interpolate(src, k2, dst_num_bands, posx, posyy,
                                    fracValues, posyROI, roiAccessor, roiIter, false).intValue();
                        } else if (interpBN != null) {
                            result = interpBN.interpolate(src, k2, dst_num_bands, posx, posyy,
                                    fracValues, posyROI, roiAccessor, roiIter, false).intValue();
                            // Case of general interpolator (ROI and No Data not supported)
                        } else {
                            // Source data array of the selected band
                            int[] srcData = srcDataArrays[k2];
                            // Offset of the selected band
                            int tmp = bandOffsets[k2];

                            // Get the pixels required for this interpolation
                            int start = interp_left * srcPixelStride + interp_top
                                    * srcScanlineStride;
                            // Initial array index of the selected pixel
                            start = posx + posy - start;
                            // Initial value for the counter for filling the interpolation kernel
                            int countH = 0, countV = 0;
                            // cycle on the x and y directions
                            for (int i = 0; i < interp_height; i++) {
                                int startY = start;
                                for (int j = 0; j < interp_width; j++) {
                                    // interpolation kernel value addition
                                    samples[countV][countH++] = srcData[start + tmp];
                                    // update of the array index on the x direction
                                    start += srcPixelStride;
                                }
                                // Update of the counter on the y axis and reset of the one on the X axis
                                countV++;
                                countH = 0;
                                // update of the array index on the y direction
                                start = startY + srcScanlineStride;
                            }

                            // Get the new frac values
                            xfrac = (int) (fracx * shiftvalue);
                            yfrac = (int) (fracy * shiftvalue);

                            // Do the interpolation
                            result = interp.interpolate(samples, xfrac, yfrac);
                        }

                        // Clamp
                        if (result < ((float) Integer.MIN_VALUE)) {
                            s = Integer.MIN_VALUE;
                        } else if (result > ((float) Integer.MAX_VALUE)) {
                            s = Integer.MAX_VALUE;
                        } else {
                            s = (int) result;
                        }
                        // Write the result
                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = s;
                    }
                    // If the pixel is outside bounds and is possible to set
                } else if (setDestinationNoData) {
                    for (int k = 0; k < dst_num_bands; k++) {
                        dstDataArrays[k][dstPixelOffset + dstBandOffsets[k]] = (int) destinationNoData[k];
                    }
                }

                // walk
                if (interpN == null || !(interp instanceof InterpolationNearest)) {
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
                    // If interpolation Nearest is used, then the
                } else {
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
                }

                // Translate to/from SampleModel space & Raster space
                posy = (s_iy - srcRectY) * srcScanlineStride;
                posx = (s_ix - srcRectX) * srcPixelStride;

                if (roiAccessor != null) {
                    posyROI = (s_iy - srcRectY) * roiScanlineStride;
                }

                // Go to next pixel
                dstPixelOffset += dstPixelStride;
            }
            // Go to the next line in the destination rectangle
            dstOffset += dstScanlineStride;
        }
    }

    private void intLoopBinary(int dataType, RasterAccessor src, Raster source,
            WritableRaster dest, Rectangle destRect, Raster roi, int srcRectX, int srcRectY, RandomIter roiIter) {

        float src_rect_x1 = source.getMinX();
        float src_rect_y1 = source.getMinY();
        float src_rect_x2 = src_rect_x1 + source.getWidth();
        float src_rect_y2 = src_rect_y1 + source.getHeight();

        MultiPixelPackedSampleModel sourceSM = (MultiPixelPackedSampleModel) source
                .getSampleModel();

        DataBufferInt sourceDB = (DataBufferInt) source.getDataBuffer();

        int sourceTransX = source.getSampleModelTranslateX();
        int sourceTransY = source.getSampleModelTranslateY();
        int sourceDataBitOffset = sourceSM.getDataBitOffset();
        int sourceScanlineStride = sourceSM.getScanlineStride();
        int sourcePixelStride = sourceSM.getPixelBitStride();

        MultiPixelPackedSampleModel destSM = (MultiPixelPackedSampleModel) dest.getSampleModel();

        DataBufferInt destDB = (DataBufferInt) dest.getDataBuffer();

        int destTransX = dest.getSampleModelTranslateX();
        int destTransY = dest.getSampleModelTranslateY();
        int destDataBitOffset = destSM.getDataBitOffset();
        int destScanlineStride = destSM.getScanlineStride();

        int[] sourceData = sourceDB.getData();
        Number[] sourceDataNum = new Number[sourceData.length];
        for (int i = 0; i < sourceData.length; i++) {
            sourceDataNum[i] = sourceData[i];
        }
        int sourceDBOffset = sourceDB.getOffset();

        int[] destData = destDB.getData();
        int destDBOffset = destDB.getOffset();

        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

        // Output value initialization
        int s = 0;

        // pixel value coordinates initialization
        int[] coordinates = new int[2];

        int[] roiData = null;

        int roiDBOffset = 0;

        MultiPixelPackedSampleModel roiSM = null;

        int roiTransY = 0;
        int roiScanlineStride = 0;

        if (roi != null) {
            roiSM = (MultiPixelPackedSampleModel) roi.getSampleModel();

            // roiTransX = roi.getSampleModelTranslateX();
            roiTransY = roi.getSampleModelTranslateY();
            // roiDataBitOffset = roiSM.getDataBitOffset();
            roiScanlineStride = roiSM.getScanlineStride();

            DataBuffer roiDB = roi.getDataBuffer();

            roiDBOffset = roiDB.getOffset();

            DataBufferByte roiDBByte = (DataBufferByte) roiDB;
            byte[] roiDataB = roiDBByte.getData();
            roiData = new int[roiDataB.length];
            for (int ii = 0; ii < roiDataB.length; ii++) {
                roiData[ii] = roiDataB[ii];
            }
        }

        for (int y = dst_min_y; y < dst_max_y; y++) {
            // Backward map the first point in the line
            // The energy is at the (pt_x + 0.5, pt_y + 0.5)
            dst_pt.setLocation((double) dst_min_x + 0.5, (double) y + 0.5);
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            float s_x = (float) src_pt.getX();
            float s_y = (float) src_pt.getY();

            // Floor to get the integral coordinate
            int s_ix = (int) Math.floor(s_x);
            int s_iy = (int) Math.floor(s_y);

            double fracx = s_x - (double) s_ix;
            double fracy = s_y - (double) s_iy;

            // Get the new frac values
            int xfrac = (int) (fracx * shiftvalue);
            int yfrac = (int) (fracy * shiftvalue);

            int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
            int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

            int destYOffset = (y - destTransY) * destScanlineStride + destDBOffset;
            int destXOffset = destDataBitOffset + (dst_min_x - destTransX);

            int sourceYOffset = (s_iy - sourceTransY) * sourceScanlineStride + sourceDBOffset;
            // int sourceXOffset = s_ix - sourceTransX + sourceDataBitOffset;

            int roiYOffset = (s_iy - roiTransY) * roiScanlineStride + roiDBOffset;

            // Cycle on the destination image x bounds
            for (int x = dst_min_x; x < dst_max_x; x++) {

                sourceYOffset = (s_iy - sourceTransY) * sourceScanlineStride + sourceDBOffset;
                roiYOffset = (s_iy - roiTransY) * roiScanlineStride + roiDBOffset;

                int dindex = 0;
                int dshift;
                // int delement = 0;

                //
                // Check against the source rectangle
                //
                if ((s_ix >= src_rect_x1 + interp_left) && (s_ix < (src_rect_x2 - interp_right))
                        && (s_iy >= src_rect_y1 + interp_top)
                        && (s_iy < (src_rect_y2 - interp_bottom))) {

                    coordinates[0] = src.getX() + (s_ix - srcRectX) * sourcePixelStride;
                    ;
                    coordinates[1] = src.getY() + ((s_iy - srcRectY) * sourceScanlineStride)
                            / sourceScanlineStride;

                    int xNextBitNo = sourceDataBitOffset + (s_ix + 1 - sourceTransX);
                    if (interpN != null) {
                        s = interpN.interpolateBinary(xNextBitNo, sourceDataNum, sourceYOffset,
                                sourceScanlineStride, coordinates, roiData, roiYOffset,
                                roiScanlineStride, roiIter);
                    } else if (interpB != null) {
                        s = interpB.interpolateBinary(xNextBitNo, sourceDataNum, xfrac, yfrac,
                                sourceYOffset, sourceScanlineStride, coordinates, roiData,
                                roiYOffset, roiScanlineStride, roiIter);
                    } else if (interpBN != null) {
                        s = interpBN.interpolateBinary(xNextBitNo, sourceDataNum, xfrac, yfrac,
                                sourceYOffset, sourceScanlineStride, coordinates, roiData,
                                roiYOffset, roiScanlineStride, roiIter);
                    } else {
                        throw new UnsupportedOperationException(
                                "Binary interpolation not supported for interpolators different from"
                                        + "InterpolatorNearestNew,InterpolatorBinaryNew,InterpolatorBicubicNew");
                    }

                } else if (setDestinationNoData) {
                    s = black;
                }

                dindex = destYOffset + (destXOffset >> 5);
                dshift = 31 - (destXOffset & 31);
                // delement = destData[dindex];
                // delement |= s << dshift;
                //
                // destData[dindex] = delement;

                if (s == 1) {
                    destData[dindex] |= (0x01 << dshift);
                } else {
                    destData[dindex] &= (0xffffffff - (0x01 << dshift));
                }

                // walk
                if (interpN == null || !(interp instanceof InterpolationNearest)) {
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
                    // If interpolation Nearest is used, then the
                } else {
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
                }
                ++destXOffset;
            }
        }
    }

    private void floatLoop(int dataType, RasterAccessor src, Rectangle destRect, int srcRectX,
            int srcRectY, RasterAccessor dst, RasterAccessor roiAccessor, RandomIter roiIter) {

        // Creation of the interpolation kernel for interpolators different from the Interpolation type:
        // InterpolationNearest, InterpolationBilinear, InterpolationBicubicNew.
        float[][] samples = new float[interp_height][interp_width];

        // Integral fractional values for a general interpolator
        int xfrac;
        int yfrac;

        // Source region
        float src_rect_x1 = src.getX();
        float src_rect_y1 = src.getY();
        float src_rect_x2 = src_rect_x1 + src.getWidth();
        float src_rect_y2 = src_rect_y1 + src.getHeight();

        // destination image pixel offset
        int dstPixelOffset;
        int dstOffset = 0;

        // Destination and source point stored as point2D object
        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

        // destination data arrays
        float[][] dstDataArrays = dst.getFloatDataArrays();
        // Destination band offsets array
        int[] dstBandOffsets = dst.getBandOffsets();
        // destination pixel stride
        int dstPixelStride = dst.getPixelStride();
        // destination scanline stride
        int dstScanlineStride = dst.getScanlineStride();

        // source image data array
        float[][] srcDataArrays = src.getFloatDataArrays();
        // source image band offsets array
        int[] bandOffsets = src.getBandOffsets();
        // source pixel stride
        int srcPixelStride = src.getPixelStride();
        // source scanline stride
        int srcScanlineStride = src.getScanlineStride();

        // destination band number
        int dst_num_bands = dst.getNumBands();

        // ROI scanline stride
        int roiScanlineStride = 0;
        if (roiAccessor != null) {
            roiScanlineStride = roiAccessor.getScanlineStride();
        }

        // Destination bounds
        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

        // ROI position initialization
        Integer posyROI = null;

        // Fractional array initialization
        Number[] fracValues = new Number[2];

        // Cycle on the destination image y bounds
        for (int y = dst_min_y; y < dst_max_y; y++) {
            // update of the destination pixel offset
            dstPixelOffset = dstOffset;

            // Backward map the first point in the line
            // The energy is at the (pt_x + 0.5, pt_y + 0.5)
            dst_pt.setLocation((double) dst_min_x + 0.5, (double) y + 0.5);
            // Backward mapping of the destination point position
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            float s_x = (float) src_pt.getX();
            float s_y = (float) src_pt.getY();

            // As per definition of bilinear or bicubic interpolation
            if (interpN == null || !(interp instanceof InterpolationNearest)) {
                s_x -= 0.5;
                s_y -= 0.5;
            }

            // Floor to get the integral coordinate
            int s_ix = (int) Math.floor(s_x);
            int s_iy = (int) Math.floor(s_y);

            // Calculation of the fractional values
            float fracx = s_x - (float) s_ix;
            float fracy = s_y - (float) s_iy;

            // Get the new frac values
            xfrac = (int) (fracx * shiftvalue);
            yfrac = (int) (fracy * shiftvalue);

            // Store of the fractional value inside an array
            if (dataType < DataBuffer.TYPE_FLOAT) {
                fracValues[0] = xfrac;
                fracValues[1] = yfrac;
            } else {
                fracValues[0] = fracx;
                fracValues[1] = fracy;
            }

            // Translate to/from SampleModel space & Raster space
            int posy = (s_iy - srcRectY) * srcScanlineStride;
            int posx = (s_ix - srcRectX) * srcPixelStride;

            // Conversion if the fractional values to integer ones for Nearest-Neighbor interpolation
            int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
            int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

            // If roiAccessor is present, the y position on the roi image is calculated
            if (roiAccessor != null) {
                posyROI = (s_iy - srcRectY) * roiScanlineStride;
            }

            // Cycle on the destination image x bounds
            for (int x = dst_min_x; x < dst_max_x; x++) {
                //
                // Check against the source rectangle
                //
                if ((s_ix >= src_rect_x1 + interp_left) && (s_ix < (src_rect_x2 - interp_right))
                        && (s_iy >= src_rect_y1 + interp_top)
                        && (s_iy < (src_rect_y2 - interp_bottom))) {
                    // Cycle on all the image bands
                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                        // initial value for the output sample
                        float s = 0;

                        int posyy = posy + bandOffsets[k2];

                        // Control for using the defined interpolator
                        if (interpN != null) {
                            s = interpN.interpolate(src, k2, dst_num_bands, posx, posyy, posyROI,
                                    roiAccessor, roiIter, false).floatValue();
                        } else if (interpB != null) {
                            s = interpB.interpolate(src, k2, dst_num_bands, posx, posyy,
                                    fracValues, posyROI, roiAccessor, roiIter, false).floatValue();
                        } else if (interpBN != null) {
                            s = interpBN.interpolate(src, k2, dst_num_bands, posx, posyy,
                                    fracValues, posyROI, roiAccessor, roiIter, false).floatValue();
                            // Case of general interpolator (ROI and No Data not supported)
                        } else {
                            // Source data array of the selected band
                            float[] srcData = srcDataArrays[k2];
                            // Offset of the selected band
                            int tmp = bandOffsets[k2];

                            // Get the pixels required for this interpolation
                            int start = interp_left * srcPixelStride + interp_top
                                    * srcScanlineStride;
                            // Initial array index of the selected pixel
                            start = posx + posy - start;
                            // Initial value for the counter for filling the interpolation kernel
                            int countH = 0, countV = 0;
                            // cycle on the x and y directions
                            for (int i = 0; i < interp_height; i++) {
                                int startY = start;
                                for (int j = 0; j < interp_width; j++) {
                                    // interpolation kernel value addition
                                    samples[countV][countH++] = srcData[start + tmp];
                                    // update of the array index on the x direction
                                    start += srcPixelStride;
                                }
                                // Update of the counter on the y axis and reset of the one on the X axis
                                countV++;
                                countH = 0;
                                // update of the array index on the y direction
                                start = startY + srcScanlineStride;
                            }

                            // Do the interpolation
                            s = interp.interpolate(samples, fracx, fracy);
                        }
                        // Write the result
                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = (float) s;
                    }
                    // If the pixel is outside bounds and is possible to set
                } else if (setDestinationNoData) {
                    for (int k = 0; k < dst_num_bands; k++) {
                        dstDataArrays[k][dstPixelOffset + dstBandOffsets[k]] = (float) destinationNoData[k];
                    }
                }

                // walk
                if (interpN == null || !(interp instanceof InterpolationNearest)) {
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
                    // If interpolation Nearest is used, then the
                } else {
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
                }

                // Translate to/from SampleModel space & Raster space
                posy = (s_iy - srcRectY) * srcScanlineStride;
                posx = (s_ix - srcRectX) * srcPixelStride;

                if (roiAccessor != null) {
                    posyROI = (s_iy - srcRectY) * roiScanlineStride;
                }

                // Go to next pixel
                dstPixelOffset += dstPixelStride;
            }
            // Go to the next line in the destination rectangle
            dstOffset += dstScanlineStride;
        }
    }

    private void doubleLoop(int dataType, RasterAccessor src, Rectangle destRect, int srcRectX,
            int srcRectY, RasterAccessor dst, RasterAccessor roiAccessor, RandomIter roiIter) {

        // Creation of the interpolation kernel for interpolators different from the Interpolation type:
        // InterpolationNearest, InterpolationBilinear, InterpolationBicubicNew.
        double[][] samples = new double[interp_height][interp_width];

        // Integral fractional values for a general interpolator
        int xfrac;
        int yfrac;

        // Source region
        float src_rect_x1 = src.getX();
        float src_rect_y1 = src.getY();
        float src_rect_x2 = src_rect_x1 + src.getWidth();
        float src_rect_y2 = src_rect_y1 + src.getHeight();

        // destination image pixel offset
        int dstPixelOffset;
        int dstOffset = 0;

        // Destination and source point stored as point2D object
        Point2D dst_pt = new Point2D.Float();
        Point2D src_pt = new Point2D.Float();

        // destination data arrays
        double[][] dstDataArrays = dst.getDoubleDataArrays();
        // Destination band offsets array
        int[] dstBandOffsets = dst.getBandOffsets();
        // destination pixel stride
        int dstPixelStride = dst.getPixelStride();
        // destination scanline stride
        int dstScanlineStride = dst.getScanlineStride();

        // source image data array
        double[][] srcDataArrays = src.getDoubleDataArrays();
        // source image band offsets array
        int[] bandOffsets = src.getBandOffsets();
        // source pixel stride
        int srcPixelStride = src.getPixelStride();
        // source scanline stride
        int srcScanlineStride = src.getScanlineStride();

        // destination band number
        int dst_num_bands = dst.getNumBands();

        // ROI scanline stride
        int roiScanlineStride = 0;
        if (roiAccessor != null) {
            roiScanlineStride = roiAccessor.getScanlineStride();
        }

        // Destination bounds
        int dst_min_x = destRect.x;
        int dst_min_y = destRect.y;
        int dst_max_x = destRect.x + destRect.width;
        int dst_max_y = destRect.y + destRect.height;

        // ROI position initialization
        Integer posyROI = null;

        // Fractional array initialization
        Number[] fracValues = new Number[2];

        // Cycle on the destination image y bounds
        for (int y = dst_min_y; y < dst_max_y; y++) {
            // update of the destination pixel offset
            dstPixelOffset = dstOffset;

            // Backward map the first point in the line
            // The energy is at the (pt_x + 0.5, pt_y + 0.5)
            dst_pt.setLocation((double) dst_min_x + 0.5, (double) y + 0.5);
            // Backward mapping of the destination point position
            mapDestPoint(dst_pt, src_pt);

            // Get the mapped source coordinates
            float s_x = (float) src_pt.getX();
            float s_y = (float) src_pt.getY();

            // As per definition of bilinear or bicubic interpolation
            if (interpN == null || !(interp instanceof InterpolationNearest)) {
                s_x -= 0.5;
                s_y -= 0.5;
            }

            // Floor to get the integral coordinate
            int s_ix = (int) Math.floor(s_x);
            int s_iy = (int) Math.floor(s_y);

            // Calculation of the fractional values
            float fracx = s_x - (float) s_ix;
            float fracy = s_y - (float) s_iy;

            // Get the new frac values
            xfrac = (int) (fracx * shiftvalue);
            yfrac = (int) (fracy * shiftvalue);

            // Store of the fractional value inside an array
            if (dataType < DataBuffer.TYPE_FLOAT) {
                fracValues[0] = xfrac;
                fracValues[1] = yfrac;
            } else {
                fracValues[0] = fracx;
                fracValues[1] = fracy;
            }

            // Translate to/from SampleModel space & Raster space
            int posy = (s_iy - srcRectY) * srcScanlineStride;
            int posx = (s_ix - srcRectX) * srcPixelStride;

            // Conversion if the fractional values to integer ones for Nearest-Neighbor interpolation
            int ifracx = (int) Math.floor(fracx * GEOM_FRAC_MAX);
            int ifracy = (int) Math.floor(fracy * GEOM_FRAC_MAX);

            // If roiAccessor is present, the y position on the roi image is calculated
            if (roiAccessor != null) {
                posyROI = (s_iy - srcRectY) * roiScanlineStride;
            }

            // Cycle on the destination image x bounds
            for (int x = dst_min_x; x < dst_max_x; x++) {
                //
                // Check against the source rectangle
                //
                if ((s_ix >= src_rect_x1 + interp_left) && (s_ix < (src_rect_x2 - interp_right))
                        && (s_iy >= src_rect_y1 + interp_top)
                        && (s_iy < (src_rect_y2 - interp_bottom))) {
                    // Cycle on all the image bands
                    for (int k2 = 0; k2 < dst_num_bands; k2++) {
                        // initial value for the output sample
                        double s = 0;

                        int posyy = posy + bandOffsets[k2];

                        // Control for using the defined interpolator
                        if (interpN != null) {
                            s = interpN.interpolate(src, k2, dst_num_bands, posx, posyy, posyROI,
                                    roiAccessor, roiIter, false).doubleValue();
                        } else if (interpB != null) {
                            s = interpB.interpolate(src, k2, dst_num_bands, posx, posyy,
                                    fracValues, posyROI, roiAccessor, roiIter, false).doubleValue();
                        } else if (interpBN != null) {
                            s = interpBN.interpolate(src, k2, dst_num_bands, posx, posyy,
                                    fracValues, posyROI, roiAccessor, roiIter, false).doubleValue();
                            // Case of general interpolator (ROI and No Data not supported)
                        } else {
                            // Source data array of the selected band
                            double[] srcData = srcDataArrays[k2];
                            // Offset of the selected band
                            int tmp = bandOffsets[k2];

                            // Get the pixels required for this interpolation
                            int start = interp_left * srcPixelStride + interp_top
                                    * srcScanlineStride;
                            // Initial array index of the selected pixel
                            start = posx + posy - start;
                            // Initial value for the counter for filling the interpolation kernel
                            int countH = 0, countV = 0;
                            // cycle on the x and y directions
                            for (int i = 0; i < interp_height; i++) {
                                int startY = start;
                                for (int j = 0; j < interp_width; j++) {
                                    // interpolation kernel value addition
                                    samples[countV][countH++] = srcData[start + tmp];
                                    // update of the array index on the x direction
                                    start += srcPixelStride;
                                }
                                // Update of the counter on the y axis and reset of the one on the X axis
                                countV++;
                                countH = 0;
                                // update of the array index on the y direction
                                start = startY + srcScanlineStride;
                            }

                            // Do the interpolation
                            s = interp.interpolate(samples, fracx, fracy);
                        }
                        // Write the result
                        dstDataArrays[k2][dstPixelOffset + dstBandOffsets[k2]] = s;
                    }
                    // If the pixel is outside bounds and is possible to set
                } else if (setDestinationNoData) {
                    for (int k = 0; k < dst_num_bands; k++) {
                        dstDataArrays[k][dstPixelOffset + dstBandOffsets[k]] = destinationNoData[k];
                    }
                }

                // walk
                if (interpN == null || !(interp instanceof InterpolationNearest)) {
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
                    // If interpolation Nearest is used, then the
                } else {
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
                }

                // Translate to/from SampleModel space & Raster space
                posy = (s_iy - srcRectY) * srcScanlineStride;
                posx = (s_ix - srcRectX) * srcPixelStride;

                if (roiAccessor != null) {
                    posyROI = (s_iy - srcRectY) * roiScanlineStride;
                }

                // Go to next pixel
                dstPixelOffset += dstPixelStride;
            }
            // Go to the next line in the destination rectangle
            dstOffset += dstScanlineStride;
        }
    }
}
