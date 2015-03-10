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
package it.geosolutions.jaiext.buffer;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import java.util.List;
import java.util.Map;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.ROI;
import javax.media.jai.RasterFactory;

import com.sun.media.jai.opimage.RIFUtil;
import com.sun.media.jai.util.JDKWorkarounds;

public class BufferRIF implements RenderedImageFactory {

    public RenderedImage create(ParameterBlock pb, RenderingHints renderHints) {

        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);

        RenderedImage src = pb.getRenderedSource(0);

        Integer type = (Integer)pb.getIntParameter(9);
        
        if(type != null && !(layout == null && type == src.getSampleModel().getDataType())){

            // Create or clone the ImageLayout.
            if(layout == null) {
                layout = new ImageLayout(src);
            } else {
                layout = (ImageLayout)layout.clone();
            }

            boolean isDataTypeChange = false;

            // Get prospective destination SampleModel.
            SampleModel sampleModel = layout.getSampleModel(src);

            // Create a new SampleModel if the type is not as desired.
            if (sampleModel.getDataType() != type) {
                int tileWidth = layout.getTileWidth(src);
                int tileHeight = layout.getTileHeight(src);
                int numBands = src.getSampleModel().getNumBands();

                SampleModel csm =
                    RasterFactory.createComponentSampleModel(sampleModel,
                                                             type,
                                                             tileWidth,
                                                             tileHeight,
                                                             numBands);

                layout.setSampleModel(csm);
                isDataTypeChange = true;
            }


            // Check ColorModel.
            ColorModel colorModel = layout.getColorModel(null);
            if (colorModel != null
                    && !JDKWorkarounds.areCompatibleDataModels(layout.getSampleModel(src),
                            colorModel)) {
                // Clear the mask bit if incompatible.
                layout.unsetValid(ImageLayout.COLOR_MODEL_MASK);
            }

            // Check whether anything but the ColorModel is changing.
            if (layout.getSampleModel(src) == src.getSampleModel() &&
                layout.getMinX(src) == src.getMinX() &&
                layout.getMinY(src) == src.getMinY() &&
                layout.getWidth(src) == src.getWidth() &&
                layout.getHeight(src) == src.getHeight() &&
                layout.getTileWidth(src) == src.getTileWidth() &&
                layout.getTileHeight(src) == src.getTileHeight() &&
                layout.getTileGridXOffset(src) == src.getTileGridXOffset() &&
                layout.getTileGridYOffset(src) == src.getTileGridYOffset()) {

                if (layout.getColorModel(src) != src.getColorModel()) {
                    // Remove TileCache hint from RenderingHints if present.
                    if (renderHints != null && renderHints.containsKey(JAI.KEY_TILE_CACHE)) {
                        renderHints = new RenderingHints((Map) renderHints);
                        renderHints.remove(JAI.KEY_TILE_CACHE);
                    }
                }
            }

            if (isDataTypeChange == true) {

                // Add JAI.KEY_REPLACE_INDEX_COLOR_MODEL hint to renderHints
                if (renderHints == null) {
                    renderHints = 
                        new RenderingHints(JAI.KEY_REPLACE_INDEX_COLOR_MODEL,
                                           Boolean.TRUE);
                    
                } else if (!renderHints.containsKey(
                                            JAI.KEY_REPLACE_INDEX_COLOR_MODEL)) {
                    // If the user specified a value for this hint, we don't
                    // want to change that
                    renderHints.put(JAI.KEY_REPLACE_INDEX_COLOR_MODEL, 
                                    Boolean.TRUE);
                }
            }
        }

        BorderExtender extender = (BorderExtender) pb.getObjectParameter(0);

        int leftPadding = pb.getIntParameter(1);
        int rightPadding = pb.getIntParameter(2);
        int topPadding = pb.getIntParameter(3);
        int bottomPadding = pb.getIntParameter(4);

        List<ROI> rois = (List<ROI>) pb.getObjectParameter(5);
        Range noData = (Range) pb.getObjectParameter(6);
        double destinationNoData = pb.getDoubleParameter(7);
        Double valueToCount = (Double) pb.getObjectParameter(8);
        double pixelArea = pb.getDoubleParameter(10);

        return new BufferOpImage(src, layout, renderHints, extender, leftPadding, rightPadding,
                topPadding, bottomPadding, rois, noData, destinationNoData, valueToCount, pixelArea);
    }
}
