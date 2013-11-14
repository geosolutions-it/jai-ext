package it.geosolutions.jaiext.contrastenhancement;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;

import com.sun.media.jai.opimage.RIFUtil;


public class SquareRootStretchCRIF extends CRIFImpl
{

    /** Constructor. */
    public SquareRootStretchCRIF()
    {
        super("SquareRootStretch");
    }

    /**
     * Creates a new instance of <code>SquareRootStretchOpImage</code> in the
     * rendered layer.
     *
     * @param args   The source image and the factors.
     * @param hints  Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock args,
        RenderingHints renderHints)
    {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);


        return new SquareRootStretchOpImage(args.getRenderedSource(0),
                renderHints,
                layout, (int[]) args.getObjectParameter(0), (int[]) args.getObjectParameter(1), (int[]) args.getObjectParameter(2), (int[]) args.getObjectParameter(3));
    }
}
