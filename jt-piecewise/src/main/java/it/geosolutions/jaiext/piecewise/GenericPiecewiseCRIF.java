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
package it.geosolutions.jaiext.piecewise;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;
import javax.media.jai.ROI;

import com.sun.media.jai.opimage.RIFUtil;

/**
 * {@link RenderedImageFactory} used for generating {@link GenericPiecewiseOpImage} instances
 * 
 * @author Nicola Lagomarsini Geosolutions
 * 
 */
public class GenericPiecewiseCRIF extends CRIFImpl {

    public GenericPiecewiseCRIF() {
        super(GenericPiecewiseOpImage.OPERATION_NAME);
    }

    public RenderedImage create(ParameterBlock pb, RenderingHints hints) {
        // Getting ImageLayout
        ImageLayout l = RIFUtil.getImageLayoutHint(hints);
        // Getting Source
        RenderedImage source = pb.getRenderedSource(0);
        // Extracting Parameters
        final PiecewiseTransform1D lic = (PiecewiseTransform1D) pb.getObjectParameter(0);
        final Integer bandIndex = pb.getIntParameter(1);
        final ROI roi = (ROI) pb.getObjectParameter(2);
        final Range nodata = (Range) pb.getObjectParameter(3);

        return new GenericPiecewiseOpImage<PiecewiseTransform1DElement>(source, lic, l, bandIndex,
                roi, nodata, hints, true);
    }

}
