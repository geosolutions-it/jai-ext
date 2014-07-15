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
package it.geosolutions.jaiext;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ContextualRenderedImageFactory;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;
import javax.media.jai.NullOpImage;
import javax.media.jai.OpImage;

import com.sun.media.jai.opimage.RIFUtil;

/**
 * Dummy {@link ContextualRenderedImageFactory} for the "Scale" operation. Internally it calls the "Null" operation.
 * 
 * @author Nicola Lagomarsini - GeoSolutions
 * 
 */
public class DummyScaleCRIF extends CRIFImpl {

    /** Constructor. */
    public DummyScaleCRIF() {
        super("scale");
    }

    /**
     * Creates a new Image in the rendered layer. This method satisfies the implementation of RIF.
     * 
     * @param paramBlock The source image.
     */
    public RenderedImage create(ParameterBlock paramBlock, RenderingHints renderHints) {

        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        // Get the source
        RenderedImage source = paramBlock.getRenderedSource(0);
        // returns a new Image
        return new NullOpImage(source, layout, renderHints, OpImage.OP_COMPUTE_BOUND);
    }
}
