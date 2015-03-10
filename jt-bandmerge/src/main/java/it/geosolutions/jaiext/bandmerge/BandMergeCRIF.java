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
package it.geosolutions.jaiext.bandmerge;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.ArrayList;
import java.util.List;

import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;
import javax.media.jai.ROI;

import com.sun.media.jai.opimage.RIFUtil;

/**
 * A <code>CRIF</code> supporting the "BandMerge" operation on rendered and renderable images.
 */
public class BandMergeCRIF extends CRIFImpl {

    /** Constructor. */
    public BandMergeCRIF() {
        super("bandmergeOp");
    }

    /**
     * Creates a new instance of <code>BandMergeOpImage</code> in the rendered layer.
     * 
     * @param paramBlock The two or more source images to be "Merged" together; if No Data are present, also a NoData Range and a double value for the
     *        destination no data are present.
     * @param renderHints Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock paramBlock, RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        // Get the number of the sources
        int numSources = paramBlock.getNumSources();
        // Creation of a source ArrayList (better than a Vector)
        List sources = new ArrayList(numSources);

        // Addition of the sources to the List
        for (int i = 0; i < numSources; i++) {
            sources.add(paramBlock.getSource(i));
        }

        // Parameters
        Range[] nodata = (Range[]) paramBlock.getObjectParameter(0);
        double destinationNoData = paramBlock.getDoubleParameter(1);

        // Transformation Object
        List<AffineTransform> transform = (List<AffineTransform>) paramBlock.getObjectParameter(2);
        // ROI object
        ROI roi = (ROI) paramBlock.getObjectParameter(3);
        // Last band is an alpha band?
        boolean setAlpha = (Boolean) paramBlock.getObjectParameter(4);
        // If the transformations are present, then they are used with the ExtendedBandMergeOpImage
        if (transform != null && !transform.isEmpty()) {
            return new ExtendedBandMergeOpImage(sources, transform, renderHints, nodata, roi,
                    destinationNoData, setAlpha, layout);
        } else {
            return new BandMergeOpImage(sources, renderHints, nodata, roi, destinationNoData, setAlpha, layout);
        }
    }
}
