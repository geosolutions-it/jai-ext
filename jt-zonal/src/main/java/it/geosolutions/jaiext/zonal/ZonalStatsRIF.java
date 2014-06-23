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
package it.geosolutions.jaiext.zonal;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.stats.Statistics.StatsType;

import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import java.util.List;

import javax.media.jai.ImageLayout;
import javax.media.jai.ROI;

import com.sun.media.jai.opimage.RIFUtil;

/**
 * This class is a RenderedImageFactory called by the JAI.create() method when the "Zonal" operation is requested. When called, the create() method
 * takes the parameterBlock passed in input, unpacks it and then returns a new instance of the {@link ZonalStatsOpImage}.
 */
public class ZonalStatsRIF implements RenderedImageFactory {

    public RenderedImage create(ParameterBlock pb, RenderingHints hints) {

        // Selection of the source
        RenderedImage source = pb.getRenderedSource(0);
        // Selection of the layout
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);
        // Selection of the classifier
        RenderedImage classifier = (RenderedImage) pb.getObjectParameter(0);
        // Selection of the parameters
        AffineTransform transform;
        if (classifier != null) {
            transform = (AffineTransform) pb.getObjectParameter(1);
        } else {
            transform = null;
        }
        List<ROI> roilist = (List<ROI>) pb.getObjectParameter(2);
        Range noData = (Range) pb.getObjectParameter(3);
        ROI mask = (ROI) pb.getObjectParameter(4);
        boolean useROIAccessor = (Boolean) pb.getObjectParameter(5);
        int[] bands = (int[]) pb.getObjectParameter(6);
        StatsType[] statsTypes = (StatsType[]) pb.getObjectParameter(7);
        double[] minBound = (double[]) pb.getObjectParameter(8);
        double[] maxBound = (double[]) pb.getObjectParameter(9);
        int[] numBins = (int[]) pb.getObjectParameter(10);
        List<Range> rangeList = (List<Range>) pb.getObjectParameter(11);
        boolean localStats = (Boolean) pb.getObjectParameter(12);

        // Image creation
        return new ZonalStatsOpImage(source, layout, hints, classifier, transform, roilist, noData,
                mask, useROIAccessor, bands, statsTypes, minBound, maxBound, numBins, rangeList,
                localStats);
    }

}
