package it.geosolutions.jaiext.errordiffusion;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

import javax.media.jai.ImageLayout;
import javax.media.jai.KernelJAI;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.ROI;

import com.sun.media.jai.opimage.RIFUtil;

public class ErrorDiffusionRIF implements RenderedImageFactory {

    /** Constructor. */
    public ErrorDiffusionRIF() {
    }

    public RenderedImage create(ParameterBlock paramBlock, RenderingHints hints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);
        // Get the source image
        RenderedImage source = paramBlock.getRenderedSource(0);
        // Get the parameters
        LookupTableJAI lookupTable = (LookupTableJAI) paramBlock.getObjectParameter(0);
        KernelJAI kernel = (KernelJAI) paramBlock.getObjectParameter(1);
        ROI roi = (ROI) paramBlock.getObjectParameter(2);
        Range nodata = (Range) paramBlock.getObjectParameter(3);
        int destNoData =paramBlock.getIntParameter(4);

        return new ErrorDiffusionOpImage(source, hints, layout, lookupTable, kernel, roi, nodata,
                destNoData);
    }

}
