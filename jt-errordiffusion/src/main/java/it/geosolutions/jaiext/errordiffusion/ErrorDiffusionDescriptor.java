package it.geosolutions.jaiext.errordiffusion;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "ErrorDiffusion" operation.
 * 
 * <p>
 * The "ErrorDiffusion" operation performs color quantization by finding the nearest color to each pixel in a supplied color map and "diffusing" the
 * color quantization error below and to the right of the pixel.
 * 
 * <p>
 * <table border=1>
 * <caption>Resource List</caption>
 * <tr>
 * <th>Name</th>
 * <th>Value</th>
 * </tr>
 * <tr>
 * <td>GlobalName</td>
 * <td>ErrorDiffusion</td>
 * </tr>
 * <tr>
 * <td>LocalName</td>
 * <td>ErrorDiffusion</td>
 * </tr>
 * <tr>
 * <td>Vendor</td>
 * <td>com.sun.media.jai</td>
 * </tr>
 * <tr>
 * <td>Description</td>
 * <td>Performs error diffusion color quantization using a specified color map and error filter.</td>
 * </tr>
 * <tr>
 * <td>DocURL</td>
 * <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/ErrorDiffusionDescriptor.html</td>
 * </tr>
 * <tr>
 * <td>Version</td>
 * <td>1.0</td>
 * </tr>
 * <tr>
 * <td>arg0Desc</td>
 * <td>The color map.</td>
 * </tr>
 * <tr>
 * <td>arg1Desc</td>
 * <td>The error filter kernel.</td>
 * </tr>
 * </table>
 * </p>
 * 
 * <p>
 * <table border=1>
 * <caption>Parameter List</caption>
 * <tr>
 * <th>Name</th>
 * <th>Class Type</th>
 * <th>Default Value</th>
 * </tr>
 * <tr>
 * <td>colorMap</td>
 * <td>javax.media.jai.LookupTableJAI</td>
 * <td>NO_PARAMETER_DEFAULT</td>
 * <tr>
 * <td>errorKernel</td>
 * <td>javax.media.jai.KernelJAI</td>
 * <td>javax.media.jai.KernelJAI.ERROR_FILTER_FLOYD_STEINBERG</td>
 * </table>
 * </p>
 * 
 * @see javax.media.jai.LookupTableJAI
 * @see javax.media.jai.KernelJAI
 * @see javax.media.jai.ColorCube
 * @see javax.media.jai.OperationDescriptor
 */
public class ErrorDiffusionDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation and specify the parameter list for the "ErrorDiffusion" operation.
     */
    private static final String[][] resources = {
            { "GlobalName", "ErrorDiffusion" },
            { "LocalName", "ErrorDiffusion" },
            { "Vendor", "it.geosolutions.jaiext" },
            { "Description", JaiI18N.getString("ErrorDiffusionDescriptor0") },
            {
                    "DocURL",
                    "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/ErrorDiffusionDescriptor.html" },
            { "Version", JaiI18N.getString("DescriptorVersion") },
            { "arg0Desc", JaiI18N.getString("ErrorDiffusionDescriptor1") },
            { "arg1Desc", JaiI18N.getString("ErrorDiffusionDescriptor2") },
            { "arg2Desc", JaiI18N.getString("ErrorDiffusionDescriptor3") },
            { "arg3Desc", JaiI18N.getString("ErrorDiffusionDescriptor4") },
            { "arg4Desc", JaiI18N.getString("ErrorDiffusionDescriptor5") }, };

    /** The parameter names for the "ErrorDiffusion" operation. */
    private static final String[] paramNames = { "colorMap", "errorKernel", "roi", "nodata",
            "destNoData" };

    /** The parameter class types for the "ErrorDiffusion" operation. */
    private static final Class[] paramClasses = { javax.media.jai.LookupTableJAI.class,
            javax.media.jai.KernelJAI.class, javax.media.jai.ROI.class,
            it.geosolutions.jaiext.range.Range.class, Integer.class };

    /** The parameter default values for the "ErrorDiffusion" operation. */
    private static final Object[] paramDefaults = { NO_PARAMETER_DEFAULT,
            // Default error filter to Floyd-Steinberg.
            KernelJAI.ERROR_FILTER_FLOYD_STEINBERG, null, null, 0 };

    /** Constructor. */
    public ErrorDiffusionDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /**
     * Performs error diffusion color quantization using a specified color map and error filter.
     * 
     * <p>
     * Creates a <code>ParameterBlockJAI</code> from all supplied arguments except <code>hints</code> and invokes
     * {@link JAI#create(String,ParameterBlock,RenderingHints)}.
     * 
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderedOp
     * 
     * @param source0 <code>RenderedImage</code> source 0.
     * @param colorMap The color map.
     * @param errorKernel The error filter kernel. May be <code>null</code>.
     * @param roi The optional ROI to use in computation. May be <code>null</code>.
     * @param nodata A range used for checking if a pixel is nodata. May be <code>null</code>.
     * @param destNoData Value for the destination NoData to set (in the source image colorspace). May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use. May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>colorMap</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0, LookupTableJAI colorMap,
            KernelJAI errorKernel, ROI roi, Range nodata, double[] destNoData, RenderingHints hints) {
        ParameterBlockJAI pb = new ParameterBlockJAI("ErrorDiffusion",
                RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("colorMap", colorMap);
        pb.setParameter("errorKernel", errorKernel);
        pb.setParameter("roi", roi);
        pb.setParameter("nodata", nodata);
        pb.setParameter("destNoData", destNoData);

        return JAI.create("ErrorDiffusion", pb, hints);
    }

}
