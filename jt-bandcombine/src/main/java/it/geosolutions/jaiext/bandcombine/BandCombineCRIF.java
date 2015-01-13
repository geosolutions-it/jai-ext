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
package it.geosolutions.jaiext.bandcombine;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;
import javax.media.jai.ROI;

import com.sun.media.jai.opimage.RIFUtil;

/**
 * CRIF implementation used for creating a new BandCombineOpImage instance.
 */
public class BandCombineCRIF extends CRIFImpl {

    public BandCombineCRIF() {
        super("BandCombine");
    }

    @Override
    public RenderedImage create(ParameterBlock pb, RenderingHints hints) {
        // Extracting the Layout
        ImageLayout l = RIFUtil.getImageLayoutHint(hints);

        // Extract Source
        RenderedImage source = pb.getRenderedSource(0);

        // Extract Parameters
        double[][] matrix = (double[][]) pb.getObjectParameter(0);
        ROI roi = (ROI) pb.getObjectParameter(1);
        Range nodata = (Range) pb.getObjectParameter(2);
        double destinationNoData = pb.getDoubleParameter(3);

        return new BandCombineOpImage(source, hints, l, matrix, roi, nodata, destinationNoData);
    }

}
