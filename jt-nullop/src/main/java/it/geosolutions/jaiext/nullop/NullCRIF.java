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
 * <code>PropertyGenerator</code>s. Another use of this class is related to the fact that can be put at the sink of a RenderedOp chain and can cache
 * the tiles of the final resulted image without caching the tiles of the previous calculations.
 * 
 */
public class NullCRIF extends CRIFImpl {

    /**
     * Image returned by <code>RenderedImageFactory.create()</code> when there are ono sources.
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
     * there are no soures.
     * 
     * @throws ClassCastException if there are sources and the source at index zero is not a <code>RenderedImage</code>.
     */
    public RenderedImage create(ParameterBlock pb, RenderingHints renderHints) {

        RenderedImage source = pb.getRenderedSource(0);

        ImageLayout layout = (ImageLayout) renderHints.get(JAI.KEY_IMAGE_LAYOUT);

        if (source == null) {
            return getSourcelessImage();
        }

        return new NullOpImage(source, layout, renderHints);
    }
}
