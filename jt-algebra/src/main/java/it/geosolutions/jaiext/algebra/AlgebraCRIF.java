package it.geosolutions.jaiext.algebra;

import it.geosolutions.jaiext.algebra.AlgebraDescriptor.Operator;
import it.geosolutions.jaiext.range.Range;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;
import javax.media.jai.ROI;
import com.sun.media.jai.opimage.RIFUtil;

public class AlgebraCRIF extends CRIFImpl {

    /** Constructor. */
    public AlgebraCRIF() {
        super("algebric");
    }

    /**
     * Creates a new instance of <code>AddOpImage</code> in the rendered layer. This method satisfies the implementation of RIF.
     * 
     * @param paramBlock The two source images to be added.
     * @param renderHints Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock pb, RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);

        int numSrc = pb.getNumSources();

        RenderedImage[] sources = new RenderedImage[numSrc];

        for (int i = 0; i < numSrc; i++) {
            sources[i] = pb.getRenderedSource(i);
        }

        Operator op = (Operator) pb.getObjectParameter(0);
        ROI roi = (ROI) pb.getObjectParameter(1);
        Range noData = (Range) pb.getObjectParameter(2);
        double destinationNoData = pb.getDoubleParameter(3);

        return new AlgebraOpImage(renderHints, layout, op, roi, noData, destinationNoData, sources);
    }

}
