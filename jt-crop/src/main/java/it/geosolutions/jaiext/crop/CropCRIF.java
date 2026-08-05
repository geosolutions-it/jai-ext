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
package it.geosolutions.jaiext.crop;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.ROI;
import javax.media.jai.operator.MosaicDescriptor;

import com.sun.media.jai.opimage.RIFUtil;

import it.geosolutions.jaiext.mosaic.MosaicOpImage;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;

/**
 * The image factory for the Crop operator.
 * 
 * @author Andrea Aime
 */
public class CropCRIF implements RenderedImageFactory {

    public CropCRIF() {
    }

    /**
     * Creates a new instance of {@link CropOpImage} in the rendered layer.
     * 
     * @param paramBlock
     *            parameter block with parameters minx, miny, width height
     * 
     * @param renderHints
     *            optional rendering hints which may be used to pass down a tile scheduler and tile
     *            cache
     */
    public RenderedImage create(ParameterBlock paramBlock, RenderingHints renderingHints) {
        RenderedImage image = (RenderedImage) paramBlock.getSource(0);
        float x = paramBlock.getFloatParameter(CropDescriptor.X_ARG);
        float y = paramBlock.getFloatParameter(CropDescriptor.Y_ARG);
        float width = paramBlock.getFloatParameter(CropDescriptor.WIDTH_ARG);
        float height = paramBlock.getFloatParameter(CropDescriptor.HEIGHT_ARG);
        ROI roi = (ROI) paramBlock.getObjectParameter(CropDescriptor.ROI_ARG);
        Range noData = (Range) paramBlock.getObjectParameter(CropDescriptor.NO_DATA_ARG);
        noData = RangeFactory.convert(noData, image.getSampleModel().getDataType());
        double[] destNoData = (double[]) paramBlock.getObjectParameter(CropDescriptor.DEST_NO_DATA_ARG);
        
        // only leave tile cache and tile scheduler (we can't instantiate directly RenderingHints
        // as it won't allow for a null tile cache, even if the rest of JAI handles that peachy
        Map<Key, Object> tmp = new HashMap<RenderingHints.Key, Object>();
        for (Object key : renderingHints.keySet()) {
            if (key == JAI.KEY_TILE_CACHE || key == JAI.KEY_TILE_SCHEDULER) {
                tmp.put((Key) key, renderingHints.get(key));
            }
        }
        RenderingHints local = new RenderingHints(tmp);
        // selection of the layout
        ImageLayout layout = RIFUtil.getImageLayoutHint(local);
        // Creation of a Rectangle object containing the starting bounds
        Rectangle bounds = new Rectangle2D.Float(x, y, width, height).getBounds();
        // Initialization of the final bounds
        Rectangle finalBounds = bounds;
        // A ROI reduces the final bounds to the intersection of the two. Only its bounds
        // contribute, the shape itself is not carried any further.
        if (roi != null) {
            Rectangle roiBounds = roi.getBounds();
            // an empty intersection has no image to describe it, report it here rather than let
            // the layout fail later on a non-positive width
            if (!finalBounds.intersects(roiBounds)) {
                throw new IllegalArgumentException("Crop bounds " + finalBounds
                        + " and ROI bounds " + roiBounds + " are disjoint, the crop would be empty");
            }
            finalBounds = finalBounds.intersection(roiBounds);
        }
        // The final bounds coordinates are taken
        x = (float) finalBounds.getMinX();
        y = (float) finalBounds.getMinY();
        width = (float) finalBounds.getWidth();
        height = (float) finalBounds.getHeight();

        // If noData are present, the MosaicOpImage is used instead of the crop
        if (noData != null) {
            // The source image is taken as a list of data
            List<RenderedImage> listSrc = new Vector<RenderedImage>();            
            listSrc.add(image);

            // layout settings
            if (layout == null) {
                layout = new ImageLayout();
            }
            layout.setHeight(finalBounds.height);
            layout.setWidth(finalBounds.width);
            layout.setMinX(finalBounds.x);
            layout.setMinY(finalBounds.y);

            // No ROI is passed: the destination is exactly the crop bounds, so a ROI covering them
            // could never exclude a pixel, while it would rasterize the shape and push every tile
            // onto the slower per-pixel ROI branch.
            return MosaicOpImage.create(listSrc, layout, local,
                    MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null, null, destNoData,
                    new Range[] { noData });
        }

        // If noData are not present, then the crop operation is performed
        return new CropOpImage(image, x, y, width, height, local);
    }
}
