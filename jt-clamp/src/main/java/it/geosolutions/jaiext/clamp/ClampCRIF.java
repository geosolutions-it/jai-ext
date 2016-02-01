/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2016 GeoSolutions


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
package it.geosolutions.jaiext.clamp;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.ArrayList;
import java.util.List;

import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;
import javax.media.jai.ROI;

import com.sun.media.jai.opimage.RIFUtil;

/**
 * A <code>CRIF</code> supporting the "Clamp" operation on rendered and renderable images.
 */
public class ClampCRIF extends CRIFImpl {

    /** Constructor. */
    public ClampCRIF() {
        super("clampop");
    }

    /**
     * Creates a new instance of <code>ThresholdOpImage</code> in the rendered layer.
     * 
     * @param paramBlock ParameterBlock with the source image and the input parameters.
     * @param hints Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock paramBlock, RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        // Parameters
        Range nodata = (Range) paramBlock.getObjectParameter(0);
        double destinationNoData = paramBlock.getDoubleParameter(1);

        ROI roi = (ROI) paramBlock.getObjectParameter(2);

        return new ClampOpImage(paramBlock.getRenderedSource(0), renderHints, nodata, roi,
                destinationNoData, layout, (double[]) paramBlock.getObjectParameter(3),
                (double[]) paramBlock.getObjectParameter(4));

    }
}
