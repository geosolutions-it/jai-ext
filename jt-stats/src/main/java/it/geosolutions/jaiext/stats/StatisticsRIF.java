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
package it.geosolutions.jaiext.stats;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

import javax.media.jai.ImageLayout;
import javax.media.jai.ROI;

import com.sun.media.jai.opimage.RIFUtil;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.stats.Statistics.StatsType;

/**
 * Simple class that provides the RenderedImage create operation by calling a subclass of the {@link StatisticsOpImage}. The input parameters are:
 * ParameterBlock, RenderingHints. The first one stores all the input parameters, the second stores eventual hints used for changing the image
 * settings. The create() method could return a new instance of the {@link SimpleStatsOpImage} operation or a new instance of the
 * {@link ComplexStatsOpImage}.
 */
public class StatisticsRIF implements RenderedImageFactory {

    public RenderedImage create(ParameterBlock pb, RenderingHints hints) {
        // Selection of the source
        RenderedImage source = pb.getRenderedSource(0);
        // Selection of the layout
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);
        // Selection of the parameters
        int xPeriod = pb.getIntParameter(0);
        int yPeriod = pb.getIntParameter(1);
        ROI roi = (ROI) pb.getObjectParameter(2);
        Range noData = (Range) pb.getObjectParameter(3);
        noData = RangeFactory.convert(noData, source.getSampleModel().getDataType());
        boolean useROIAccessor = (Boolean) pb.getObjectParameter(4);
        int[] bands = (int[]) pb.getObjectParameter(5);
        StatsType[] statsTypes = (StatsType[]) pb.getObjectParameter(6);

        // Control which subclass of the StatisticsOpImage must be called
        boolean isSimpleStat = true;

        for (int i = 0; i < statsTypes.length; i++) {
            if (statsTypes[i].getStatsId() > 6) {
                isSimpleStat = false;
                break;
            }
        }

        // Creation of the OpImage
        if (isSimpleStat) {
            return new SimpleStatsOpImage(source, xPeriod, yPeriod, roi, noData,
                    useROIAccessor, bands, statsTypes);
        } else {
            // Selection of the bounds parameters
            double[] minBounds = (double[]) pb.getObjectParameter(7);
            double[] maxBounds = (double[]) pb.getObjectParameter(8);
            int[] numBins = (int[]) pb.getObjectParameter(9);
            return new ComplexStatsOpImage(source, xPeriod, yPeriod, roi, noData,
                    useROIAccessor, bands, statsTypes, minBounds, maxBounds, numBins);
        }
    }

}
