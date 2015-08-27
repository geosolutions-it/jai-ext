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
package it.geosolutions.jaiext.binarize;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ContextualRenderedImageFactory;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;
import javax.media.jai.ROI;

import com.sun.media.jai.opimage.RIFUtil;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;

/**
 * {@link ContextualRenderedImageFactory} implementation used for creating a new {@link BinarizeOpImage} instance.
 */
public class BinarizeCRIF extends CRIFImpl {

    /** Constructor. */
    public BinarizeCRIF() {
        super("Binarize");
    }

    /**
     * Creates a new instance of <code>BinarizeOpImage</code> in the rendered layer.
     * 
     * @param pb The source image and the input parameters.
     * @param hints Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock pb, RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);

        // Getting source
        RenderedImage src = pb.getRenderedSource(0);
        // Getting parameters
        double threshold = pb.getDoubleParameter(0);
        ROI roi = (ROI) pb.getObjectParameter(1);
        Range nodata = (Range) pb.getObjectParameter(2);
        nodata = RangeFactory.convert(nodata, src.getSampleModel().getDataType());

        return new BinarizeOpImage(src, renderHints, layout, threshold, roi, nodata);
    }

}
