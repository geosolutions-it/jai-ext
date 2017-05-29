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
package it.geosolutions.jaiext.warp;

import it.geosolutions.jaiext.border.BorderDescriptor;
import it.geosolutions.jaiext.interpolators.InterpolationNoData;
import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
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
import javax.media.jai.RenderedOp;
import javax.media.jai.Warp;
import javax.media.jai.iterator.RandomIter;

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

    /** Quantity used for extending the input tile dimensions */
    protected static final int TILE_EXTENDER = 1;

    /** Constant indicating that the inner random iterators must pre-calculate an array of the image positions */
    protected static final boolean ARRAY_CALC = true;

    /** Constant indicating that the inner random iterators must cache the current tile position */
    protected static final boolean TILE_CACHED = true;

    /** Current ROI object */
    protected final ROI roi;

    /** Boolean indicating if ROI is used */
    protected final boolean hasROI;

    /** Boolean indicating if NoData values are present */
    protected final boolean hasNoData;

    /** Boolean indicating absence of both NoData and ROI */
    protected final boolean caseA;

    /** Boolean indicating absence of NoData and presence of ROI */
    protected final boolean caseB;

    /** Boolean indicating absence of ROI and presence of NoData */
    protected final boolean caseC;

    /** Current NoData Range object */
    protected Range noDataRange;

    /** Boolean indicating the presence of a border extender */
    protected boolean extended;

    /** Hints used for the calculations*/
    private RenderingHints hints;

    /** Left padding */
    protected int leftPad;

    /** Right padding */
    protected int rightPad;

    /** Top padding */
    protected int topPad;

    /** Bottom padding */
    protected int bottomPad;
    
    /** Image associated to the ROI*/
    protected volatile PlanarImage roiImage;

    /** Rectangle associated to the ROI bounds*/
    protected Rectangle roiBounds;

    public WarpOpImage(final RenderedImage source, final ImageLayout layout,
            final Map<?, ?> configuration, final boolean cobbleSources,
            final BorderExtender extender, final Interpolation interp, final Warp warp,
            final double[] backgroundValues, final ROI roi, final Range noData) {
        super(source, layout, configuration, cobbleSources, extender, interp, warp,
                prepareBackground(source, layout, interp, backgroundValues));

        // Get the ROI as image
        this.roi = roi;
        hasROI = roi != null;
        
        // Control on the ROI
        if(hasROI){
            roiBounds = roi.getBounds();
        }
        
        hasNoData = (interp instanceof InterpolationNoData)
                && (((InterpolationNoData) interp).getNoDataRange() != null) || noData != null;

        if (hasNoData) {
            if(interp instanceof InterpolationNoData){
                noDataRange = ((InterpolationNoData) interp).getNoDataRange();
            }            
            if (noDataRange == null) {
                noDataRange = noData;
            }
        } else {
            noDataRange = null;
        }

        // Definition of the possible cases that can be found
        // caseA = no ROI nor No Data
        // caseB = ROI present but No Data not present
        // caseC = No Data present but ROI not present
        // Last case not defined = both ROI and No Data are present
        caseA = !hasROI && !hasNoData;
        caseB = hasROI && !hasNoData;
        caseC = !hasROI && hasNoData;

        // Extender check
        extended = extender != null;
        
        // Save RenderingHints
        if(configuration instanceof RenderingHints){
            this.hints = (RenderingHints)configuration;
        }
        
        // Definition of the Padding
        leftPad = 0;
        rightPad = 0;
        topPad = 0;
        bottomPad = 0;
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
     * If ROI is present, then the source mapped rectangle is checked if it intersects the input ROI; if this condition is not satisfied, then the
     * tile is not elaborated.
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

    /**
     * Warps a rectangle. If ROI is present, the intersection between ROI and tile bounds is calculated; The result ROI will be used for calculations
     * inside the computeRect() method.
     */
    protected void computeRect(final PlanarImage[] sources, final WritableRaster dest,
            final Rectangle destRect) {
        // Retrieve format tags.
        final RasterFormatTag[] formatTags = getFormatTags();

        final RasterAccessor dst = new RasterAccessor(dest, destRect, formatTags[1], getColorModel());

        ROI roiTile = null;
        
        RandomIter roiIter = null;

        boolean roiContainsTile = false;
        boolean roiDisjointTile = false;
        
        // If a ROI is present, then only the part contained inside the current tile bounds is taken.
        if (hasROI) {
            Rectangle srcRectExpanded = mapDestRect(destRect, 0);
            // The tile dimension is extended for avoiding border errors
            srcRectExpanded.setRect(
                    srcRectExpanded.getMinX() - leftPad, 
                    srcRectExpanded.getMinY() - topPad, 
                    srcRectExpanded.getWidth() + rightPad + leftPad, 
                    srcRectExpanded.getHeight() + bottomPad + topPad);
            roiTile = roi.intersect(new ROIShape(srcRectExpanded));
            
            if(!roiBounds.intersects(srcRectExpanded)) {
                roiDisjointTile = true;
            } else {
                roiContainsTile = roiTile.contains(srcRectExpanded);
                if (!roiContainsTile) {
                    if (!roiTile.intersects(srcRectExpanded)) {
                        roiDisjointTile = true;
                    }else{
                        PlanarImage roiIMG = getImage();
                        roiIter = RandomIterFactory.create(roiIMG, null, TILE_CACHED, ARRAY_CALC);
                    }
                }
            }
        }

        if (!hasROI || !roiDisjointTile) {
            switch (dst.getDataType()) {
            case DataBuffer.TYPE_BYTE:
                computeRectByte(sources[0], dst, roiIter, roiContainsTile);
                break;
            case DataBuffer.TYPE_USHORT:
                computeRectUShort(sources[0], dst, roiIter, roiContainsTile);
                break;
            case DataBuffer.TYPE_SHORT:
                computeRectShort(sources[0], dst, roiIter, roiContainsTile);
                break;
            case DataBuffer.TYPE_INT:
                computeRectInt(sources[0], dst, roiIter, roiContainsTile);
                break;
            case DataBuffer.TYPE_FLOAT:
                computeRectFloat(sources[0], dst, roiIter, roiContainsTile);
                break;
            case DataBuffer.TYPE_DOUBLE:
                computeRectDouble(sources[0], dst, roiIter, roiContainsTile);
                break;
            }
            // After the calculations, the output data are copied into the WritableRaster
            if (dst.isDataCopy()) {
                dst.clampDataArrays();
                dst.copyDataToRaster();
            }
        } else {
            // If the tile is outside the ROI, then the destination Raster is set to backgroundValues
            if (setBackground) {
                ImageUtil.fillBackground(dest, destRect, backgroundValues);
            }
        }
    }

    /**
     * This method provides a lazy initialization of the image associated to the ROI.
     * The method uses the Double-checked locking in order to maintain thread-safety
     * 
     * @return
     */
    private PlanarImage getImage() {
        PlanarImage img = roiImage;
        if (img == null) {
            synchronized (this) {
                img = roiImage;
                if (img == null) {
                    roiImage = img = roi.getAsImage();
                }
            }
        }
        return img;
    }

    /**
     * Computation of the Warp operation on Byte images
     * 
     * @param src
     * @param dst
     * @param roiIter
     * @param roiContainsTile 
     */
    protected abstract void computeRectByte(final PlanarImage src, final RasterAccessor dst,
            final RandomIter roiIter, boolean roiContainsTile);

    /**
     * Computation of the Warp operation on UShort images
     * 
     * @param src
     * @param dst
     * @param roiIter
     * @param roiContainsTile 
     */
    protected abstract void computeRectUShort(final PlanarImage src, final RasterAccessor dst,
            final RandomIter roiIter, boolean roiContainsTile);

    /**
     * Computation of the Warp operation on Short images
     * 
     * @param src
     * @param dst
     * @param roiIter
     * @param roiContainsTile 
     */
    protected abstract void computeRectShort(final PlanarImage src, final RasterAccessor dst,
            final RandomIter roiIter, boolean roiContainsTile);

    /**
     * Computation of the Warp operation on Integer images
     * 
     * @param src
     * @param dst
     * @param roiIter
     * @param roiContainsTile 
     */
    protected abstract void computeRectInt(final PlanarImage src, final RasterAccessor dst,
            final RandomIter roiIter, boolean roiContainsTile);

    /**
     * Computation of the Warp operation on Float images
     * 
     * @param src
     * @param dst
     * @param roiIter
     * @param roiContainsTile 
     */
    protected abstract void computeRectFloat(final PlanarImage src, final RasterAccessor dst,
            final RandomIter roiIter, boolean roiContainsTile);

    /**
     * Computation of the Warp operation on Double images
     * 
     * @param src
     * @param dst
     * @param roiIter
     * @param roiContainsTile 
     */
    protected abstract void computeRectDouble(final PlanarImage src, final RasterAccessor dst,
            final RandomIter roiIter, boolean roiContainsTile);

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
            if (layout != null) {
                sm = layout.getSampleModel(source);
            } else {
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
    
    /** Returns the "floor" value of a float. */
    public static final int floor(final float f) {
        return f >= 0 ? (int) f : (int) f - 1;
    }
    

    /** Returns the "round" value of a float. */
    public static final int round(final float f) {
        return f >= 0 ? (int) (f + 0.5F) : (int) (f - 0.5F);
    }
    
    /**
     * Returns a RandomIterator on the input image.
     * 
     * @param src
     * @return
     */
    protected RandomIter getRandomIterator(final PlanarImage src, BorderExtender extender) {
        return getRandomIterator(src, 0, 1, 0, 1, extender);
    }

    /**
     * Returns a RandomIterator on the input image. Also it handles padding if present.
     * 
     * @param src
     * @return
     */
    protected RandomIter getRandomIterator(final PlanarImage src, int leftPad, int rightPad,
            int topPad, int bottomPad, BorderExtender extender) {
        RandomIter iterSource;
        if (extended) {
            RenderedOp op = BorderDescriptor.create(src, leftPad, rightPad, topPad, bottomPad,
                    extender, noDataRange, backgroundValues != null ? backgroundValues[0] : 0d, hints);
            iterSource = RandomIterFactory.create(op, op.getBounds(), TILE_CACHED, ARRAY_CALC);
        } else {
            iterSource = RandomIterFactory.create(src, src.getBounds(), TILE_CACHED, ARRAY_CALC);
        }
        return iterSource;
    }
}
