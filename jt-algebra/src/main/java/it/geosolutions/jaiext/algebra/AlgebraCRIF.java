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
package it.geosolutions.jaiext.algebra;

import it.geosolutions.jaiext.algebra.AlgebraDescriptor.Operator;
import it.geosolutions.jaiext.range.Range;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;
import javax.media.jai.ROI;
import com.sun.media.jai.opimage.RIFUtil;

public class AlgebraCRIF extends CRIFImpl {

    /** Constructor. */
    public AlgebraCRIF() {
        super("algebric");
    }

    /**
     * Creates a new instance of <code>AlgebraOpImage</code> in the rendered layer. This method satisfies the implementation of RIF.
     * 
     * @param paramBlock The two source images to be added.
     * @param renderHints Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock pb, RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);

        int numSrc = pb.getNumSources();

        RenderedImage[] sources = new RenderedImage[numSrc];

        for (int i = 0; i < numSrc; i++) {
            sources[i] = pb.getRenderedSource(i);
        }

        Operator op = (Operator) pb.getObjectParameter(0);
        ROI roi = (ROI) pb.getObjectParameter(1);
        Range noData = (Range) pb.getObjectParameter(2);
        double destinationNoData = pb.getDoubleParameter(3);

        return new AlgebraOpImage(renderHints, layout, op, roi, noData, destinationNoData, sources);
    }

}
