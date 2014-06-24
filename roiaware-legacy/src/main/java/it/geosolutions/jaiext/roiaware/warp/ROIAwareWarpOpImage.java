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
package it.geosolutions.jaiext.roiaware.warp;

import it.geosolutions.jaiext.iterator.RandomIterFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Map;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.Warp;
import javax.media.jai.WarpOpImage;
import javax.media.jai.iterator.RandomIter;

import com.sun.media.jai.util.ImageUtil;
/**
 * Subclass of {@link WarpOpImage} that makes use of the provided ROI.
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
@SuppressWarnings("unchecked")
public abstract class ROIAwareWarpOpImage extends WarpOpImage {
    
    /** {@link BorderExtender} instance for extending roi.*/
    protected final static BorderExtender ZERO_EXTENDER = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);

    @Override
    public synchronized void dispose() {

        // dispose roiImage if present
        if(roiImage!=null){ 
            roiImage.dispose();
            iterRoi.done();
        }
        super.dispose();
    }

    protected final ROI roi;
    
    protected final PlanarImage roiImage;

    protected final RandomIter iterRoi;

    protected final boolean hasROI;

    protected final Rectangle roiBounds;

    public ROIAwareWarpOpImage(final RenderedImage source,
            final ImageLayout layout,
            final Map<?,?> configuration,
            final boolean cobbleSources,
            final BorderExtender extender,
            final Interpolation interp,
            final Warp warp,
            final double[] backgroundValues,
            final ROI roi) {
        super(source, layout, configuration, cobbleSources, extender, interp, warp, backgroundValues);

        // Get the ROI as image
        this.roi = roi;   
        hasROI = roi!=null;
        if (hasROI) {
            roiImage = roi.getAsImage();
            int l = interp == null ? 0 : interp.getLeftPadding();
            int r = interp == null ? 0 : interp.getRightPadding();
            int t = interp == null ? 0 : interp.getTopPadding();
            int b = interp == null ? 0 : interp.getBottomPadding();
            final Rectangle rect = new Rectangle(
                    roiImage.getMinX()-l,
                    roiImage.getMinY()-t,
                    roiImage.getWidth()+l+r,
                    roiImage.getHeight()+t+b);
            final Raster data = roiImage.getExtendedData(rect,ZERO_EXTENDER);
            
            iterRoi = RandomIterFactory.create(data,data.getBounds()) ;   
            roiBounds = roiImage.getBounds();
        } else {
            roiImage = null;
            iterRoi = null;
            roiBounds = null;
        }
    }
    
    /**
     * Computes a tile.  A new <code>WritableRaster</code> is created to
     * represent the requested tile.  Its width and height equals to this
     * image's tile width and tile height respectively.  This method
     * assumes that the requested tile either intersects or is within
     * the bounds of this image.
     *
     * <p> Whether or not this method performs source cobbling is determined
     * by the <code>cobbleSources</code> variable set at construction time.
     * If <code>cobbleSources</code> is <code>true</code>, cobbling is
     * performed on the source for areas that intersect multiple tiles,
     * and <code>computeRect(Raster[], WritableRaster, Rectangle)</code>
     * is called to perform the actual computation.  Otherwise,
     * <code>computeRect(PlanarImage[], WritableRaster, Rectangle)</code>
     * is called to perform the actual computation.
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
       final Rectangle destRect = new Rectangle(org.x, org.y,
                 tileWidth, tileHeight).intersection(computableBounds);

       if (destRect.isEmpty()) {
           if (setBackground) {
               ImageUtil.fillBackground(dest, destRect, backgroundValues);
           }
           return dest;        // tile completely outside of computable bounds
       }

       // get the source image and check if we falls outside its bounds
       final PlanarImage source = getSourceImage(0);
       final Rectangle srcRect = mapDestRect(destRect, 0);
       if (!srcRect.intersects(source.getBounds())) {
           if (setBackground) {
               ImageUtil.fillBackground(dest, destRect, backgroundValues);
           }
           return dest;        // outside of source bounds
       }

       // are we outside the roi
       if(roi!=null&&!roi.intersects(srcRect)){
           if (setBackground) {
               ImageUtil.fillBackground(dest, destRect, backgroundValues);
           }
           return dest;        // outside of source roi
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
   
}
