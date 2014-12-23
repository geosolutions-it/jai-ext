package it.geosolutions.jaiext.piecewise;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;

public class GenericPiecewiseDescriptor extends OperationDescriptorImpl {
    /**
                 * 
                 */
    private static final long serialVersionUID = 7954257625240335874L;

    /**
     * Construct the descriptor.
     */
    public GenericPiecewiseDescriptor() {
        super(new String[][] { { "GlobalName", GenericPiecewiseOpImage.OPERATION_NAME },
                { "LocalName", GenericPiecewiseOpImage.OPERATION_NAME }, { "Vendor", "it.geosolutions.jaiext" },
                { "Description", "Generic Piecewise Transformation" },
                { "DocURL", "" }, { "Version", "1.0" } },
                new String[] { RenderedRegistryMode.MODE_NAME }, 1, new String[] { "Domain1D", "bandIndex",
                         "roi", "nodata" }, // Argument names
                new Class[] { DefaultPiecewiseTransform1D.class, Integer.class,
                        javax.media.jai.ROI.class, it.geosolutions.jaiext.range.Range.class }, // Argument classes
                new Object[] { NO_PARAMETER_DEFAULT, new Integer(-1), null, null }, // Default values for parameters,
                null // No restriction on valid parameter values.
        );
    }

    public RenderedOp create(RenderedImage source0, DefaultPiecewiseTransform1D domain1D, Integer bandIndex,
            ROI roi, Range nodata, RenderingHints hints) {
        ParameterBlockJAI pb = new ParameterBlockJAI(GenericPiecewiseOpImage.OPERATION_NAME,
                RenderedRegistryMode.MODE_NAME);
        // Setting source
        pb.setSource(source0, 0);
        // Setting parameters
        pb.setParameter("Domain1D", domain1D);
        pb.setParameter("bandIndex", bandIndex);
        pb.setParameter("roi", roi);
        pb.setParameter("nodata", nodata);

        return JAI.create(GenericPiecewiseOpImage.OPERATION_NAME, pb, hints);
    }

    /**
     * Returns {@code true} if the parameters are valids. This implementation check that the number of bands in the source image is equals to the
     * number of supplied sample dimensions, and that all sample dimensions has piecewise.
     * 
     * @param modeName The mode name (usually "Rendered").
     * @param param The parameter block for the operation to performs.
     * @param message A buffer for formatting an error message if any.
     */

    @SuppressWarnings("unchecked")
    protected boolean validateParameters(final String modeName, final ParameterBlock param,
            final StringBuffer message) {
        if (!super.validateParameters(modeName, param, message)) {
            return false;
        }
        final RenderedImage source = (RenderedImage) param.getSource(0);
        final PiecewiseTransform1D lic = (PiecewiseTransform1D) param.getObjectParameter(0);
        if (lic == null)
            return false;
        final int numBands = source.getSampleModel().getNumBands();
        final int bandIndex = param.getIntParameter(1);
        if (bandIndex == -1)
                return true;
        if (bandIndex < 0 || bandIndex >= numBands) {
                return false;
        }
        return true;
    }
}
