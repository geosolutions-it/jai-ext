package it.geosolutions.jaiext.clamp;

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
 * A <code>CRIF</code> supporting the "Clamp" operation on rendered and renderable images.
 */
public class ClampCRIF extends CRIFImpl {

    /** Constructor. */
    public ClampCRIF() {
        super("clampop");
    }

    public RenderedImage create(ParameterBlock paramBlock, RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        // Parameters
        Range nodata = (Range) paramBlock.getObjectParameter(0);
        double destinationNoData = paramBlock.getDoubleParameter(1);

        ROI roi = (ROI) paramBlock.getObjectParameter(2);

        return new ClampOpImage(paramBlock.getRenderedSource(0), renderHints, nodata, roi,
                destinationNoData, layout, (double[]) paramBlock.getObjectParameter(3),
                (double[]) paramBlock.getObjectParameter(4));

    }
}
