package it.geosolutions.jaiext.translate;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;
import com.sun.media.jai.opimage.RIFUtil;

public class TranslateCRIF extends CRIFImpl {

    private static final float TOLERANCE = 0.01F;

    /** Constructor. */
    public TranslateCRIF() {
        super("translate");
    }

    @Override
    public RenderedImage create(ParameterBlock parameterBlock, RenderingHints hints) {

        RenderedImage source = parameterBlock.getRenderedSource(0);
        float xTrans = parameterBlock.getFloatParameter(0);
        float yTrans = parameterBlock.getFloatParameter(1);

        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);

        // If there is a layout hint, TranslateIntOpImage can't deal with it
        if ((Math.abs(xTrans - (int) xTrans) < TOLERANCE)
                && (Math.abs(yTrans - (int) yTrans) < TOLERANCE) && layout == null) {
            return new TranslateIntOpImage(source, hints, (int) xTrans, (int) yTrans);
        }

        return null;

    }

}
