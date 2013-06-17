package it.geosolutions.opimage;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import javax.media.jai.operator.MosaicType;
import com.sun.media.jai.opimage.RIFUtil;

/**
 * Simple class that provides the RenderedImage create operation by calling the
 * MosaicNoDataOpImage
 */
public class MosaicNoDataRIF implements RenderedImageFactory {

/**
 * This method implements the RenderedImageFactory create method and return the
 * MosaicNoDataOpImage using the parameters defined by the parameterBlock
 */
public RenderedImage create(ParameterBlock paramBlock, RenderingHints hints) {
    return new MosaicNoDataOpImage(paramBlock.getSources(),
            RIFUtil.getImageLayoutHint(hints), hints,
            (ImageMosaicBean[]) paramBlock.getObjectParameter(0),
            (MosaicType) paramBlock.getObjectParameter(1),
            (Number[]) paramBlock.getObjectParameter(2));
}

}
