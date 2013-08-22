package it.geosolutions.jaiext.mosaic;

import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

import javax.media.jai.operator.MosaicType;

import com.sun.media.jai.opimage.RIFUtil;

/**
 * Simple class that provides the RenderedImage create operation by calling the MosaicOpImage2. The input parameters are: ParameterBlock,
 * RenderingHints. The first one stores all the mosaic parameters, the second stores eventual hints used for changing the image settings. The only one
 * method of this class returns a new instance of the MosaicOpImage2 operation.
 */
public class MosaicRIF implements RenderedImageFactory {

    /**
     * This method implements the RenderedImageFactory create method and return the MosaicOpImage2 using the parameters defined by the parameterBlock
     */
    public RenderedImage create(ParameterBlock paramBlock, RenderingHints hints) {
        return new MosaicOpImage(paramBlock.getSources(), RIFUtil.getImageLayoutHint(hints),
                hints, (ImageMosaicBean[]) paramBlock.getObjectParameter(0),
                (MosaicType) paramBlock.getObjectParameter(1), (double[])paramBlock.getObjectParameter(2));
        // return new MosaicOpImage2(paramBlock.getSources(), RIFUtil.getImageLayoutHint(hints),
        // hints, (ImageMosaicBean[]) paramBlock.getObjectParameter(0),
        // (MosaicType) paramBlock.getObjectParameter(1),
        // (Number[]) paramBlock.getObjectParameter(2));

    }

}
