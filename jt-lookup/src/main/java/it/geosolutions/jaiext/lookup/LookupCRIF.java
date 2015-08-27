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
package it.geosolutions.jaiext.lookup;

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
 * Simple class that provides the RenderedImage create operation by calling the LookupOpImage. The input parameters are: ParameterBlock,
 * RenderingHints. The first one stores all the parameters, the second stores eventual hints used for changing the image settings. The create method
 * returns a new instance of the LookupOpImage with the selected parameters.
 */

public class LookupCRIF extends CRIFImpl {

    /**
     * Creates a new instance of <code>LookupOpImage</code>.
     * 
     * @param pb The operation parameters.
     * @param hints Image RenderingHints.
     */
    public RenderedImage create(ParameterBlock pb, RenderingHints renderHints) {
        // Get ImageLayout from renderHints if present.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        // Get the source image
        RenderedImage source = pb.getRenderedSource(0);
        // Get the image params
        LookupTable table = (LookupTable) pb.getObjectParameter(0);
        double destinationNoData = pb.getDoubleParameter(1);
        ROI roi = (ROI) pb.getObjectParameter(2);
        Range noData = (Range) pb.getObjectParameter(3);
        noData = RangeFactory.convert(noData, source.getSampleModel().getDataType());
        boolean useRoiAccessor = (Boolean) pb.getObjectParameter(4);
        // Creation of the lookup image
        return new LookupOpImage(source, layout, renderHints, table, destinationNoData, roi,
                noData, useRoiAccessor);
    }
}
