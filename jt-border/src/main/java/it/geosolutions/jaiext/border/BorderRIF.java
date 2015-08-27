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
package it.geosolutions.jaiext.border;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;

import com.sun.media.jai.opimage.RIFUtil;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;

/**
 * A <code>RIF</code> supporting the "border" operation.
 * 
 */
public class BorderRIF implements RenderedImageFactory {

    /** Constructor. */
    public BorderRIF() {
    }

    /**
     * Creates a new instance of <code>BorderOpImage</code> in the rendered layer.
     * 
     * @param args The source image and the border information
     * @param hints Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock pb, RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        // Selection of the source
        RenderedImage source = pb.getRenderedSource(0);
        // Selection of the parameters
        int leftPad = pb.getIntParameter(0);
        int rightPad = pb.getIntParameter(1);
        int topPad = pb.getIntParameter(2);
        int bottomPad = pb.getIntParameter(3);
        BorderExtender type = (BorderExtender) pb.getObjectParameter(4);
        Range noData = (Range) pb.getObjectParameter(5);
        noData = RangeFactory.convert(noData, source.getSampleModel().getDataType());
        double destinationNoData = pb.getDoubleParameter(6);
        // Creation of the BorderOpImage instance
        return new BorderOpImage(source, renderHints, layout, leftPad, rightPad, topPad, bottomPad,
                type, noData, destinationNoData);

    }
}
