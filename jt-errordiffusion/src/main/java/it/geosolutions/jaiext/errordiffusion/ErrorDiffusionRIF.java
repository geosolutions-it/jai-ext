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
package it.geosolutions.jaiext.errordiffusion;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

import javax.media.jai.ImageLayout;
import javax.media.jai.KernelJAI;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.ROI;

import com.sun.media.jai.opimage.RIFUtil;

/**
 * {@link RenderedImageFactory} implementation for the Error diffusion operation
 * 
 * @author Nicola Lagomarsini geosolutions
 *
 */
public class ErrorDiffusionRIF implements RenderedImageFactory {

    /** Constructor. */
    public ErrorDiffusionRIF() {
    }

    public RenderedImage create(ParameterBlock paramBlock, RenderingHints hints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);
        // Get the source image
        RenderedImage source = paramBlock.getRenderedSource(0);
        // Get the parameters
        LookupTableJAI lookupTable = (LookupTableJAI) paramBlock.getObjectParameter(0);
        KernelJAI kernel = (KernelJAI) paramBlock.getObjectParameter(1);
        ROI roi = (ROI) paramBlock.getObjectParameter(2);
        Range nodata = (Range) paramBlock.getObjectParameter(3);
        int destNoData =paramBlock.getIntParameter(4);

        return new ErrorDiffusionOpImage(source, hints, layout, lookupTable, kernel, roi, nodata,
                destNoData);
    }

}
