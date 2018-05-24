/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2018 GeoSolutions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.geosolutions.jaiext.shadedrelief;

import com.sun.media.jai.opimage.RIFUtil;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import javax.media.jai.ImageLayout;
import javax.media.jai.ROI;

/**
 * ShadedRelief processing RenderedImageFactory.
 *
 */
public class ShadedReliefRIF implements RenderedImageFactory {

    public ShadedReliefRIF() {}

    public RenderedImage create(ParameterBlock pb, RenderingHints hints) {
        // Getting the Layout
        ImageLayout l = RIFUtil.getImageLayoutHint(hints);

        // Getting source
        RenderedImage img = pb.getRenderedSource(0);

        // Getting parameters
        int paramIndex = 0;
        ROI roi = (ROI) pb.getObjectParameter(paramIndex++);
        //        Range nodata = (Range) pb.getObjectParameter(paramIndex++);
        Double nodata = (Double) pb.getObjectParameter(paramIndex++);
        double destinationNoData = pb.getDoubleParameter(paramIndex++);
        double resX = pb.getDoubleParameter(paramIndex++);
        double resY = pb.getDoubleParameter(paramIndex++);
        double verticalExaggeration = pb.getDoubleParameter(paramIndex++);
        double verticalScale = pb.getDoubleParameter(paramIndex++);
        double altitude = pb.getDoubleParameter(paramIndex++);
        double azimuth = pb.getDoubleParameter(paramIndex++);
        ShadedReliefAlgorithm algorithm =
                (ShadedReliefAlgorithm) pb.getObjectParameter(paramIndex++);

        return new ShadedReliefOpImage(
                img,
                hints,
                l,
                roi,
                nodata,
                destinationNoData,
                resX,
                resY,
                verticalExaggeration,
                verticalScale,
                altitude,
                azimuth,
                algorithm);
    }
}
