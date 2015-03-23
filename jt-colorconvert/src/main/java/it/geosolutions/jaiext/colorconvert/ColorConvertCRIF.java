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
package it.geosolutions.jaiext.colorconvert;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ContextualRenderedImageFactory;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;
import javax.media.jai.ROI;

import com.sun.media.jai.opimage.RIFUtil;

/**
 * {@link ContextualRenderedImageFactory} implementation used for creating new {@link ColorConvertOpImage} instances.
 * 
 * @author Nicola Lagomarsini geosolutions
 * 
 */
public class ColorConvertCRIF extends CRIFImpl {

    /** Constructor. */
    public ColorConvertCRIF() {
        super("colorconvert");
    }

    /**
     * Creates a new instance of <code>ColorConvertOpImage</code>.
     * 
     * @param args The source image, destination ColorModel and other optional parameters.
     * @param hints Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock pb, RenderingHints renderingHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderingHints);
        // Getting source
        RenderedImage source = (RenderedImage) pb.getSource(0);

        // Getting Parameters
        ColorModel cm = (ColorModel) pb.getObjectParameter(0);
        ROI roi = (ROI) pb.getObjectParameter(1);
        Range nodata = (Range) pb.getObjectParameter(2);
        double[] destNoData = (double[]) pb.getObjectParameter(3);

        // Generating the source image
        return new ColorConvertOpImage(source, renderingHints, layout, cm, nodata, roi, destNoData);
    }
}
