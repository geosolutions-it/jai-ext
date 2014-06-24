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
package it.geosolutions.jaiext.nullop;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;

/**
 * A <code>ContextualRenderedImageFactory</code> representing an operation which performs no processing of its image source(s) per se, i.e., a no-op.
 * 
 * <p>
 * The primary use of this image factory is as a utility class in implementing operations which generate only non-image data via the use of
 * <code>PropertyGenerator</code>s. Another use of this class is the possibility to put this OpImage at the sink of a RenderedOp chain for caching the
 * tiles of the last OpImage without caching the tiles of the previous calculations.
 * 
 */
public class NullCRIF extends CRIFImpl {

    /**
     * Image returned by <code>RenderedImageFactory.create()</code> when there are no sources.
     */
    private static RenderedImage sourcelessImage = null;

    /**
     * Constructs a <code>NullCRIF</code>. The <code>operationName</code> in the superclass is set to <code>null</code>.
     */
    public NullCRIF() {
        super();
    }

    /**
     * Sets the value of the <code>RenderedImage</code> to be returned by the <code>RenderedImageFactory.create()</code> method when there are no
     * sources in the <code>ParameterBlock</code>.
     * 
     * @param a <code>RenderedImage</code> or <code>null</code>.
     */
    public static final synchronized void setSourcelessImage(RenderedImage im) {
        sourcelessImage = im;
    }

    /**
     * Gets the value of the RenderedImage to be returned by the RIF.create() method when there are no sources in the <code>ParameterBlock</code>.
     * 
     * @return a <code>RenderedImage</code> or <code>null</code>.
     */
    public static final synchronized RenderedImage getSourcelessImage() {
        return sourcelessImage;
    }

    /**
     * Returns the first source in the source list in the <code>ParameterBlock</code> or the value returned by <code>getSourcelessImage()</code> if
     * there are no sources.
     * 
     * @throws ClassCastException if there are sources and the source at index zero is not a <code>RenderedImage</code>.
     */
    public RenderedImage create(ParameterBlock pb, RenderingHints renderHints) {
        // Selection of the first source
        RenderedImage source = pb.getRenderedSource(0);
        // Selection of the optional layout object
        ImageLayout layout = (ImageLayout) renderHints.get(JAI.KEY_IMAGE_LAYOUT);
        // If no source is defined, then the "sourceless" image is taken, if present
        if (source == null) {
            return getSourcelessImage();
        }

        // A new instance of the NullOpImage is created
        return new NullOpImage(source, layout, renderHints);
    }
}
