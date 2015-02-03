package it.geosolutions.jaiext.orderdither;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

import javax.media.jai.ColorCube;
import javax.media.jai.ImageLayout;
import javax.media.jai.KernelJAI;
import javax.media.jai.ROI;

import com.sun.media.jai.opimage.RIFUtil;

public class OrderedDitherRIF implements RenderedImageFactory {

    public RenderedImage create(ParameterBlock paramBlock, RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        // Getting source
        RenderedImage source = paramBlock.getRenderedSource(0);
        // Getting the parameters
        ColorCube colorMap = (ColorCube) paramBlock.getObjectParameter(0);
        KernelJAI[] ditherMask = (KernelJAI[]) paramBlock.getObjectParameter(1);
        ROI roi = (ROI) paramBlock.getObjectParameter(2);
        Range nodata = (Range) paramBlock.getObjectParameter(3);
        double destNoData = paramBlock.getDoubleParameter(4);

        return new OrderedDitherOpImage(source, renderHints, layout, colorMap, ditherMask, roi,
                nodata, destNoData);
    }

}
