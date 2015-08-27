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
package it.geosolutions.jaiext.rescale;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;
import javax.media.jai.ROI;

import com.sun.media.jai.opimage.RIFUtil;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;

/**
 * This RenderedImageFactory class is called by the JAI.create("Rescaling") method for returning a new instance of the RescaleOpImage class. The
 * function of this class is to unpack the input parameterBlock, take every parameter and then pass them to a new RescaleOpImage instance.
 */
public class RescaleCRIF extends CRIFImpl {

    public RescaleCRIF() {
        super("Rescaling");
    }

    @Override
    public RenderedImage create(ParameterBlock pb, RenderingHints hints) {
        // Get ImageLayout from renderHints if present.
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);
        // Selection of the source
        RenderedImage source = pb.getRenderedSource(0);

        // Selection of the parameters
        double[] scales = (double[]) pb.getObjectParameter(0);
        double[] offsets = (double[]) pb.getObjectParameter(1);
        ROI roi = (ROI) pb.getObjectParameter(2);
        Range noData = (Range) pb.getObjectParameter(3);
        noData = RangeFactory.convert(noData, source.getSampleModel().getDataType());
        boolean useRoiAccessor = (Boolean) pb.getObjectParameter(4);
        double destinationNoData = pb.getDoubleParameter(5);
        // Creation of the new image
        return new RescaleOpImage(source, layout, hints, scales, offsets, destinationNoData, roi,
                noData, useRoiAccessor);
    }

}
