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
package it.geosolutions.jaiext.artifacts;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

import javax.media.jai.ImageLayout;
import javax.media.jai.ROI;
import javax.media.jai.RasterFactory;

import com.sun.media.jai.opimage.RIFUtil;
import com.sun.media.jai.util.ImageUtil;

/**
 * The image factory for the {@link ArtifactsFilterOpImage} operation.
 *
 *
 * @source $URL$
 */
public class ArtifactsFilterRIF implements RenderedImageFactory {

    /** Constructor */
    public ArtifactsFilterRIF() {
    }

    /**
     * Create a new instance of ArtifactsFilterOpImage in the rendered layer.
     *
     * @param paramBlock specifies the source image, and the following parameters: 
     * "roi", "backgroundValues", "threshold", "filterSize", "nodata"
     *
     * @param renderingHints optional RenderingHints object
     */
    public RenderedImage create(ParameterBlock paramBlock, RenderingHints renderingHints) {
        // Extracting source
        final RenderedImage dataImage = paramBlock.getRenderedSource(0);
        // Getting the Layout
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderingHints);
        if (layout == null) { 
            layout = new ImageLayout();
        }
        // Getting the parameters
        final int threshold = (Integer) paramBlock.getObjectParameter(ArtifactsFilterDescriptor.THRESHOLD_ARG);

        final int filterSize = (Integer) paramBlock.getObjectParameter(ArtifactsFilterDescriptor.FILTERSIZE_ARG);

        final double[] bgValues = (double[]) paramBlock.getObjectParameter(ArtifactsFilterDescriptor.BACKGROUND_ARG);

        SampleModel sm = layout.getSampleModel(null);
        // SampleModel preparation
        if (sm == null) {
            final SampleModel dataSampleModel = dataImage.getSampleModel();
            final int dataType = dataSampleModel.getDataType();
            sm = RasterFactory.createComponentSampleModel(dataSampleModel, dataType,
                    dataImage.getWidth(), dataImage.getHeight(), dataSampleModel.getNumBands());

            layout.setSampleModel(sm);
            if (layout.getColorModel(null) != null) {
                final ColorModel cm = ImageUtil.getCompatibleColorModel(sm, renderingHints);
                layout.setColorModel(cm);
            }
        }
        // Getting ROI and NoData
        final ROI roi = (ROI) paramBlock.getObjectParameter(ArtifactsFilterDescriptor.ROI_ARG);
        
        final Range nodata = (Range) paramBlock.getObjectParameter(ArtifactsFilterDescriptor.NODATA_ARG);

        return new ArtifactsFilterOpImage(dataImage, layout, renderingHints, roi, 
                bgValues, threshold, filterSize, nodata);
    }
}

