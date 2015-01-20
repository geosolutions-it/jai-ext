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
package it.geosolutions.jaiext.bandselect;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;

import com.sun.media.jai.opimage.RIFUtil;

/**
 * A <code>CRIF</code> supporting the "BandSelect2" operation in the
 * rendered and renderable image layers.
 *
 * @author Simone Giannecchini, GeoSolutions SAS 
 * @since 1.0
 */
public class BandSelectCRIF extends CRIFImpl {

    /** Constructor. */
    public BandSelectCRIF() {
        super("BandSelect2");
    }

    /**
     * Creates a new instance of <code>BandSelectOpImage</code>
     * in the rendered layer.
     *
     * @param args   The source image and the constants.
     * @param hints  Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);

        return new BandSelectOpImage(args.getRenderedSource(0),
                                     renderHints,
                                     layout,
                                     (int[])args.getObjectParameter(0));
    }
}

