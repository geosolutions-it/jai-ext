package it.geosolutions.jaiext.classifier;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;
import javax.media.jai.ROI;

import com.sun.media.jai.opimage.RIFUtil;

public class RasterClassifierCRIF extends CRIFImpl {

    /**
     * Creates a {@link RenderedImage} representing the results of an imaging operation for a given {@link ParameterBlock} and {@link RenderingHints}.
     */
    public RenderedImage create(final ParameterBlock param, final RenderingHints hints) {
        // Getting Source
        final RenderedImage image = (RenderedImage) param.getSource(0);
        // Getting imageLayout
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);
        final ColorMapTransform<ColorMapTransformElement> lic = (ColorMapTransform<ColorMapTransformElement>) param
                .getObjectParameter(0);
        final int bandIndex = param.getIntParameter(1);
        ROI roi = (ROI) param.getObjectParameter(2);
        Range nodata = (Range) param.getObjectParameter(3);
        return new RasterClassifierOpImage(image, lic, layout, bandIndex, roi, nodata, hints);
    }

}
