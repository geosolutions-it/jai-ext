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
package it.geosolutions.jaiext.mosaic;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.operator.MosaicType;

import com.sun.media.jai.opimage.RIFUtil;

/**
 * Simple class that provides the RenderedImage create operation by calling the MosaicOpImage. The input parameters are: ParameterBlock,
 * RenderingHints. The first one stores all the mosaic parameters, the second stores eventual hints used for changing the image settings. The only one
 * method of this class returns a new instance of the MosaicOpImage operation.
 */
public class MosaicRIF implements RenderedImageFactory {

    /**
     * This method implements the RenderedImageFactory create method and return the MosaicOpImage using the parameters defined by the parameterBlock
     */
    public RenderedImage create(ParameterBlock paramBlock, RenderingHints hints) {
        return
                new MosaicOpImage(paramBlock.getSources(),
                                  RIFUtil.getImageLayoutHint(hints),
                                  hints,
                                  (MosaicType)paramBlock.getObjectParameter(0),
                                  (PlanarImage[])paramBlock.getObjectParameter(1),
                                  (ROI[])paramBlock.getObjectParameter(2),
                                  (double[][])paramBlock.getObjectParameter(3),
                                  (double[])paramBlock.getObjectParameter(4),
                                  (Range[])paramBlock.getObjectParameter(5));
    }

}
