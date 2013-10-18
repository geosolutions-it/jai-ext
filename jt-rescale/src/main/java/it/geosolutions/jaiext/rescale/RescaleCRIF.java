package it.geosolutions.jaiext.rescale;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;
import javax.media.jai.ROI;

import com.sun.media.jai.opimage.RIFUtil;

public class RescaleCRIF extends CRIFImpl {
    
    
    public RescaleCRIF(){
        super("Rescaling");
    }

    @Override
    public RenderedImage create(ParameterBlock pb, RenderingHints hints) {
        // Get ImageLayout from renderHints if present.
        ImageLayout layout = RIFUtil.getImageLayoutHint(hints);
        // Selection of the source
        RenderedImage source = pb.getRenderedSource(0);

        // Selection of the parameters
        double[] scales = (double[]) pb.getObjectParameter(0);
        double[] offsets = (double[]) pb.getObjectParameter(1);
        ROI roi = (ROI) pb.getObjectParameter(2);
        Range rangeND = (Range) pb.getObjectParameter(3);
        boolean useRoiAccessor = (Boolean) pb.getObjectParameter(4);
        double destinationNoData = pb.getDoubleParameter(5);
        // Creation of the new image
        return new RescaleOpImage(source, layout, hints, scales, offsets, destinationNoData, roi,
                rangeND, useRoiAccessor);
    }

}
