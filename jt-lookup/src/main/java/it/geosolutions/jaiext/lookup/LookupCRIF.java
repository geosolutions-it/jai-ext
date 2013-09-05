package it.geosolutions.jaiext.lookup;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;
import javax.media.jai.ROI;

import com.sun.media.jai.opimage.RIFUtil;

/**
 * Simple class that provides the RenderedImage create operation by calling the LookupOpImage. The input parameters are: ParameterBlock,
 * RenderingHints. The first one stores all the parameters, the second stores eventual hints used for changing the image settings. The create method
 * returns a new instance of the LookupOpImage with the selected parameters.
 */

public class LookupCRIF extends CRIFImpl {

    /**
     * Creates a new instance of <code>LookupOpImage</code>.
     * 
     * @param pb The operation parameters.
     * @param hints Image RenderingHints.
     */
    public RenderedImage create(ParameterBlock pb, RenderingHints renderHints) {
        // Get ImageLayout from renderHints if present.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        // Get the source image
        RenderedImage source = pb.getRenderedSource(0);
        // Get the image params
        LookupTable table = (LookupTable) pb.getObjectParameter(0);
        double destinationNoData = pb.getDoubleParameter(1);
        ROI roi = (ROI) pb.getObjectParameter(2);
        Range noData = (Range) pb.getObjectParameter(3);
        boolean useRoiAccessor = (Boolean) pb.getObjectParameter(4);
        // Creation of the lookup image
        return new LookupOpImage(source, layout, renderHints, table, destinationNoData, roi,
                noData, useRoiAccessor);
    }
}
