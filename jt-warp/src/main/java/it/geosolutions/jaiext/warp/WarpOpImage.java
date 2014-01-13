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

import it.geosolutions.jaiext.interpolators.InterpolationNoData;
import it.geosolutions.jaiext.range.Range;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.Map;
import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.Warp;
import com.sun.media.jai.util.ImageUtil;

/**
 * Subclass of {@link WarpOpImage} that makes use of the provided ROI and NoData.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * 
 */
@SuppressWarnings("unchecked")
public abstract class WarpOpImage extends javax.media.jai.WarpOpImage {

    /** {@link BorderExtender} instance for extending roi. */
    protected final static BorderExtender ZERO_EXTENDER = BorderExtender
            .createInstance(BorderExtender.BORDER_ZERO);
    /** Quantity used for extending the input tile dimensions*/
    protected static final int TILE_EXTENDER = 1;
    
    protected static final boolean ARRAY_CALC = true;

    protected static final boolean TILE_CACHED = true;
    
    protected byte destinationNoDataByte;

    protected short destinationNoDataShort;

    protected int destinationNoDataInt;

    protected float destinationNoDataFloat;
    
    protected double destinationNoDataDouble;
    
    protected final ROI roi;

    protected final boolean hasROI;

    protected final boolean hasNoData;

    protected final boolean caseA;

    protected final boolean caseB;

    protected final boolean caseC;

    protected final Range noDataRange;
    
    protected boolean extended; 

    public WarpOpImage(final RenderedImage source, final ImageLayout layout,
            final Map<?, ?> configuration, final boolean cobbleSources,
            final BorderExtender extender, final Interpolation interp, final Warp warp,
            final double[] backgroundValues, final ROI roi) {
        super(source, layout, configuration, cobbleSources, extender, interp, warp,
                prepareBackground(source, layout, interp, backgroundValues));

        // Get the ROI as image
        this.roi = roi;
        hasROI = roi != null;
        hasNoData = (interp instanceof InterpolationNoData)
                && (((InterpolationNoData) interp).getNoDataRange() != null);
        
        if(hasNoData){
            noDataRange = ((InterpolationNoData) interp).getNoDataRange();
        }else{
            noDataRange = null;
        }
        
      //Definition of the possible cases that can be found
        // caseA = no ROI nor No Data
        // caseB = ROI present but No Data not present
        // caseC = No Data present but ROI not present
        // Last case not defined = both ROI and No Data are present        
        caseA = !hasROI && !hasNoData;
        caseB = hasROI && !hasNoData;
        caseC = !hasROI && hasNoData;
        
        
        //Extender check
        extended = extender != null;
    }

    /**
     * Computes a tile. A new <code>WritableRaster</code> is created to represent the requested tile. Its width and height equals to this image's tile
     * width and tile height respectively. This method assumes that the requested tile either intersects or is within the bounds of this image.
     * 
     * <p>
     * Whether or not this method performs source cobbling is determined by the <code>cobbleSources</code> variable set at construction time. If
     * <code>cobbleSources</code> is <code>true</code>, cobbling is performed on the source for areas that intersect multiple tiles, and
     * <code>computeRect(Raster[], WritableRaster, Rectangle)</code> is called to perform the actual computation. Otherwise,
     * <code>computeRect(PlanarImage[], WritableRaster, Rectangle)</code> is called to perform the actual computation.
     * 
     * @param tileX The X index of the tile.
     * @param tileY The Y index of the tile.
     * 
     * @return The tile as a <code>Raster</code>.
     */
    @Override
    public Raster computeTile(final int tileX, final int tileY) {

        // The origin of the tile.
        final Point org = new Point(tileXToX(tileX), tileYToY(tileY));

        // Create a new WritableRaster to represent this tile.
        final WritableRaster dest = createWritableRaster(sampleModel, org);

        // Find the intersection between this tile and the writable bounds.
        final Rectangle destRect = new Rectangle(org.x, org.y, tileWidth, tileHeight)
                .intersection(computableBounds);

        if (destRect.isEmpty()) {
            if (setBackground) {
                ImageUtil.fillBackground(dest, destRect, backgroundValues);
            }
            return dest; // tile completely outside of computable bounds
        }

        // get the source image and check if we falls outside its bounds
        final PlanarImage source = getSourceImage(0);
        final Rectangle srcRect = mapDestRect(destRect, 0);
        if (!srcRect.intersects(source.getBounds())) {
            if (setBackground) {
                ImageUtil.fillBackground(dest, destRect, backgroundValues);
            }
            return dest; // outside of source bounds
        }

        // are we outside the roi
        if (roi != null && !roi.intersects(srcRect)) {
            if (setBackground) {
                ImageUtil.fillBackground(dest, destRect, backgroundValues);
            }
            return dest; // outside of source roi
        }

        // This image only has one source.
        if (cobbleSources) {
            // FIXME
            throw new UnsupportedOperationException();

        } else {
            final PlanarImage[] srcs = { source };
            computeRect(srcs, dest, destRect);
        }

        return dest;
    }

    /** Warps a rectangle. */
    protected void computeRect(final PlanarImage[] sources, final WritableRaster dest,
            final Rectangle destRect) {
        // Retrieve format tags.
        final RasterFormatTag[] formatTags = getFormatTags();

        final RasterAccessor d = new RasterAccessor(dest, destRect, formatTags[1], getColorModel());

        ROI roiTile = null;

        // If a ROI is present, then only the part contained inside the current tile bounds is taken.
        if (hasROI) {
            Rectangle srcRectExpanded = mapDestRect(destRect, 0); 
            // The tile dimension is extended for avoiding border errors
            srcRectExpanded.grow(TILE_EXTENDER, TILE_EXTENDER);
            roiTile = roi.intersect(new ROIShape(srcRectExpanded));
        }

        if (!hasROI || !roiTile.getBounds().isEmpty()) {
            switch (d.getDataType()) {
            case DataBuffer.TYPE_BYTE:
                computeRectByte(sources[0], d, roiTile);
                break;
            case DataBuffer.TYPE_USHORT:
                computeRectUShort(sources[0], d, roiTile);
                break;
            case DataBuffer.TYPE_SHORT:
                computeRectShort(sources[0], d, roiTile);
                break;
            case DataBuffer.TYPE_INT:
                computeRectInt(sources[0], d, roiTile);
                break;
            case DataBuffer.TYPE_FLOAT:
                computeRectFloat(sources[0], d, roiTile);
                break;
            case DataBuffer.TYPE_DOUBLE:
                computeRectDouble(sources[0], d, roiTile);
                break;
            }

            if (d.isDataCopy()) {
                d.clampDataArrays();
                d.copyDataToRaster();
            }
        } else {
            // If the tile is outside the ROI, then the destination Raster is set to backgroundValues
            if (setBackground) {
                ImageUtil.fillBackground(dest, destRect, backgroundValues);
            }
        }
    }
    
    
    protected abstract void computeRectByte(final PlanarImage src, final RasterAccessor dst, final ROI roiTile);
    
    protected abstract void computeRectUShort(final PlanarImage src, final RasterAccessor dst, final ROI roiTile);
    
    protected abstract void computeRectShort(final PlanarImage src, final RasterAccessor dst, final ROI roiTile);
    
    protected abstract void computeRectInt(final PlanarImage src, final RasterAccessor dst, final ROI roiTile);
    
    protected abstract void computeRectFloat(final PlanarImage src, final RasterAccessor dst, final ROI roiTile);
    
    protected abstract void computeRectDouble(final PlanarImage src, final RasterAccessor dst, final ROI roiTile);
    
    
    /**
     * Utility method used for creating an array of background values from a single value, taken from the interpolator. If the interpolation object is
     * not an instance of InterpolationNoData, then the optional background values array is taken. If the array is null, then an array with 0 value is
     * used.
     * 
     * @param source
     * @param layout
     * @param interp
     * @param backgroundValues
     * @return
     */
    public static double[] prepareBackground(final RenderedImage source, ImageLayout layout,
            Interpolation interp, double[] backgroundValues) {
        // If the interpolator is an instance of InterpolationNoData, the background value is taken from the interpolator
        if (interp instanceof InterpolationNoData) {
            SampleModel sm;
            if(layout!=null){
                sm = layout.getSampleModel(source);
            }else{
                sm = source.getSampleModel();
            }
            int numBands = sm.getNumBands();

            double[] destinationNoData = new double[numBands];

            Arrays.fill(destinationNoData, ((InterpolationNoData) interp).getDestinationNoData());

            return destinationNoData;
            // Else, check if an array is present
        } else if (backgroundValues != null) {
            return backgroundValues;
            // Else an input array with 0 value is returned
        } else {
            return new double[] { 0.0d };
        }

    }

}
