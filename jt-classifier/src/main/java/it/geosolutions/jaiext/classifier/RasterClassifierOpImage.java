/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2014 - 2016 GeoSolutions


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
package it.geosolutions.jaiext.classifier;

import it.geosolutions.jaiext.piecewise.GenericPiecewiseOpImage;
import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.ROI;

/**
 * This class provides a few initialization method used for implementing the RasterClassier operation, which is an extension of the GenericPiecewise
 * one.
 * 
 * @author Nicola Lagomarsini geosolutions
 * 
 */
public class RasterClassifierOpImage<T extends ColorMapTransformElement> extends
        GenericPiecewiseOpImage<T> {

    /**
     * The operation name.
     */
    public static final String OPERATION_NAME = "RasterClassifier";

    public RasterClassifierOpImage(RenderedImage image, ColorMapTransform<T> lic,
            ImageLayout layout, Integer bandIndex, ROI roi, Range nodata, RenderingHints hints) {
        super(image, lic, prepareLayout(image, layout, lic), bandIndex, roi, nodata,
                prepareHints(hints), true);
        this.isByteData = false;
    }

    /**
     * Prepare the {@link RenderingHints} for the final image disabling direct colormap operations
     * 
     * @param hints The {@link RenderingHints} used in the operation
     * @return a {@link RenderingHints} object with a key for disabling Colormap operations
     */
    private static RenderingHints prepareHints(RenderingHints hints) {
        RenderingHints h = null;
        if (hints == null) {
            h = new RenderingHints(JAI.KEY_TRANSFORM_ON_COLORMAP, false);
        } else {
            h = (RenderingHints) hints.clone();
            h.put(JAI.KEY_TRANSFORM_ON_COLORMAP, false);
        }

        return h;
    }

    /**
     * Prepare the {@link ImageLayout} for the final image by building the {@link ColorModel} from the input
     * 
     * @param image the image to classify.
     * @param layout a proposed layout.
     * @param lic the pieces we are asked to use.
     * @return a layout suitable for the image that we'll create after this
     */
    private static <T extends ColorMapTransformElement> ImageLayout prepareLayout(
            RenderedImage image, ImageLayout layout, ColorMapTransform<T> lic) {
        // //
        //
        // Get the final color model from the pieces and from that one
        // create the sample model
        //
        // ///
        final ColorModel finalColorModel = lic.getColorModel();
        // create a good sample model for the output raster
        final SampleModel finalSampleModel = lic
                .getSampleModel(image.getWidth(), image.getHeight());
        if (layout == null)
            layout = new ImageLayout();
        layout.setColorModel(finalColorModel);
        layout.setSampleModel(finalSampleModel);
        return layout;
    }
}
