package it.geosolutions.jaiext.piecewise;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;
import javax.media.jai.ROI;

import com.sun.media.jai.opimage.RIFUtil;

public class GenericPiecewiseCRIF extends CRIFImpl {
    
    public GenericPiecewiseCRIF() {
        super(GenericPiecewiseOpImage.OPERATION_NAME);
    }

    public RenderedImage create(ParameterBlock pb, RenderingHints hints) {
        // Getting ImageLayout
        ImageLayout l = RIFUtil.getImageLayoutHint(hints);
        // Getting Source
        RenderedImage source = pb.getRenderedSource(0);
        // Extracting Parameters
        final PiecewiseTransform1D lic = (PiecewiseTransform1D) pb.getObjectParameter(0);
        final ROI roi = (ROI)pb.getObjectParameter(1);
        final Range nodata = (Range)pb.getObjectParameter(2);
        
        return null;
    }

}
