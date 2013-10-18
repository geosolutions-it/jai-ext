package it.geosolutions.jaiext.rescale;

import it.geosolutions.jaiext.range.Range;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderableImage;
import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PropertyGenerator;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderableOp;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderableRegistryMode;
import javax.media.jai.registry.RenderedRegistryMode;
import com.sun.media.jai.util.PropertyGeneratorImpl;

/**
 * This class is used for retrieving an eventual ROI object passed to the source image by calling the getProperty() method. This method checks if the
 * ROI is present and if so, its bounds are intersected with the source images bounds, and then passed as a result. If no property was found an
 * Undefined Property object is returned.
 */

class RescalePropertyGenerator extends PropertyGeneratorImpl {

    /** Constructor. */
    public RescalePropertyGenerator() {
        super(new String[] { "ROI" }, new Class[] { ROI.class }, new Class[] { RenderedOp.class });
    }

    /**
     * Returns the ROI saved as a property.
     */
    public Object getProperty(String name, Object opNode) {
        validate(name, opNode);

        if (opNode instanceof RenderedOp && name.equalsIgnoreCase("roi")) {
            RenderedOp op = (RenderedOp) opNode;

            ParameterBlock pb = op.getParameterBlock();

            // Retrieve the rendered source image and its ROI.
            RenderedImage src = pb.getRenderedSource(0);
            Object property = src.getProperty("ROI");
            if (property == null || property.equals(java.awt.Image.UndefinedProperty)
                    || !(property instanceof ROI)) {
                return java.awt.Image.UndefinedProperty;
            }

            ROI srcROI = (ROI) property;
            // Determine the effective source bounds.
            Rectangle srcBounds = null;

            srcBounds = new Rectangle(src.getMinX(), src.getMinY(), src.getWidth(), src.getHeight());

            // If necessary, clip the ROI to the effective source bounds.
            if (!srcBounds.contains(srcROI.getBounds())) {
                srcROI = srcROI.intersect(new ROIShape(srcBounds));
            }

            // Saves the destination ROI.
            ROI dstROI = srcROI;

            // Return the clipped ROI.
            return dstROI;
        }
        return java.awt.Image.UndefinedProperty;
    }
}

/**
 * An <code>OperationDescriptor</code> describing the "Rescale" operation.
 * 
 * <p>
 * The "Rescale" operation takes a rendered or renderable source image and maps the pixel values of an image from one range to another range by
 * multiplying each pixel value by one of a set of constants and then adding another constant to the result of the multiplication. If the number of
 * constants supplied is less than the number of bands of the destination, then the constant from entry 0 is applied to all the bands. Otherwise, a
 * constant from a different entry is applied to each band. There must be at least one entry in each of the contants and offsets arrays.
 * 
 * <p>
 * The destination pixel values are defined by the pseudocode:
 * 
 * <pre>
 * constant = (constants.length &lt; dstNumBands) ? constants[0] : constants[b];
 * offset = (offsets.length &lt; dstNumBands) ? offsets[0] : offsets[b];
 * 
 * dst[x][y][b] = src[x][y][b] * constant + offset;
 * </pre>
 * 
 * <p>
 * The pixel arithmetic is performed using the data type of the destination image. By default, the destination will have the same data type as the
 * source image unless an <code>ImageLayout</code> containing a <code>SampleModel</code> with a different data type is supplied as a rendering hint.
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
 * <td>Rescale</td>
 * </tr>
 * <tr>
 * <td>LocalName</td>
 * <td>Rescale</td>
 * </tr>
 * <tr>
 * <td>Vendor</td>
 * <td>com.sun.media.jai</td>
 * </tr>
 * <tr>
 * <td>Description</td>
 * <td>Maps the pixels values of an image from one range to another range.</td>
 * </tr>
 * <tr>
 * <td>DocURL</td>
 * <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/RescaleDescriptor.html</td>
 * </tr>
 * <tr>
 * <td>Version</td>
 * <td>1.0</td>
 * </tr>
 * <tr>
 * <td>arg0Desc</td>
 * <td>The per-band constants to multiply by.</td>
 * </tr>
 * <tr>
 * <td>arg1Desc</td>
 * <td>The per-band offsets to be added.</td>
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
 * <td>constants</td>
 * <td>double[]</td>
 * <td>{1.0}</td>
 * <tr>
 * <td>offsets</td>
 * <td>double[]</td>
 * <td>{0.0}</td>
 * </table>
 * </p>
 * 
 * @see javax.media.jai.OperationDescriptor
 */
public class RescaleDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation and specify the parameter list for this operation.
     */
    private static final String[][] resources = { 
            { "GlobalName", "Rescaling" },
            { "LocalName", "Rescaling" }, 
            { "Vendor", "it.geosolutions.jaiext.roiaware" },
            { "Description", "Operation which converts the image dynamics to another dynamyc"}, 
            { "DocURL", "Not Defined" }, 
            { "Version", "1.0" },
            { "arg0Desc", "Scale factors used for rescaling values" },
            { "arg1Desc", "Offset factors used for rescaling values" },
            { "arg2Desc", "ROI object used" }, 
            { "arg3Desc", "No Data Range used" },
            { "arg4Desc", "Boolean checking if ROI RasterAccessor is used" },
            { "arg5Desc", "Destination No Data value" } };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = { double[].class, double[].class,
            javax.media.jai.ROI.class, it.geosolutions.jaiext.range.Range.class, Boolean.class,
            Double.class };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = { "scale", "offset", "ROI", "noData",
            "useRoiAccessor", "destNoData" };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = { new double[] { 1.0 }, new double[] { 0.0 },
            null, null, false, 0.0d
    };

    /** Constructor. */
    public RescaleDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /** Returns <code>true</code> since renderable operation is supported. */
    public boolean isRenderableSupported() {
        return true;
    }

    /**
     * Returns an array of <code>PropertyGenerators</code> implementing property inheritance for the "Rescale" operation
     * 
     * @return An array of property generators.
     */
    public PropertyGenerator[] getPropertyGenerators() {
        PropertyGenerator[] pg = new PropertyGenerator[1];
        pg[0] = new RescalePropertyGenerator();
        return pg;
    }

    /**
     * Maps the pixels values of an image from one range to another range.
     * 
     * <p>
     * Creates a <code>ParameterBlockJAI</code> from all supplied arguments except <code>hints</code> and invokes
     * {@link JAI#create(String,ParameterBlock,RenderingHints)}.
     * 
     * @param source0 <code>RenderedImage</code> source 0.
     * @param scales The per-band scale factors to multiply by.
     * @param offsets The per-band offsets to be added.
     * @param roi Optional ROI used for computations.
     * @param noData Optional No Data range used for computations.
     * @param useROIAccessor Boolean indicating if ROI RasterAccessor must be used.
     * @param destinationNoData Destination value for No Data.
     * @param hints The <code>RenderingHints</code> to use.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0, double[] scales, double[] offsets,
            ROI roi, Range rangeND, boolean useRoiAccessor, double destNoData, RenderingHints hints) {
        // Creation of the parameterBlock object associated to the operation
        ParameterBlockJAI pb = new ParameterBlockJAI("Rescaling", RenderedRegistryMode.MODE_NAME);
        // Setting of the source
        pb.setSource("source0", source0);
        // Setting of the parameters
        pb.setParameter("scale", scales);
        pb.setParameter("offset", offsets);
        pb.setParameter("ROI", roi);
        pb.setParameter("noData", rangeND);
        pb.setParameter("useRoiAccessor", useRoiAccessor);
        pb.setParameter("destNoData", destNoData);

        return JAI.create("Rescaling", pb, hints);
    }

    /**
     * Maps the pixels values of an image from one range to another range.
     * 
     * <p>
     * Creates a <code>ParameterBlockJAI</code> from all supplied arguments except <code>hints</code> and invokes
     * {@link JAI#createRenderable(String, ParameterBlock, RenderingHints)}.
     * 
     * @param source0 <code>RenderedImage</code> source 0.
     * @param scales The per-band scale factors to multiply by.
     * @param offsets The per-band offsets to be added.
     * @param roi Optional ROI used for computations.
     * @param noData Optional No Data range used for computations.
     * @param useROIAccessor Boolean indicating if ROI RasterAccessor must be used.
     * @param destinationNoData Destination value for No Data.
     * @param hints The <code>RenderingHints</code> to use.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(RenderableImage source0, double[] scales,
            double[] offsets, ROI roi, Range rangeND, boolean useRoiAccessor, double destNoData,
            RenderingHints hints) {
        // Creation of the parameterBlock object associated to the operation
        ParameterBlockJAI pb = new ParameterBlockJAI("Rescaling", RenderableRegistryMode.MODE_NAME);
        // Setting of the source
        pb.setSource("source0", source0);
        // Setting of the parameters
        pb.setParameter("scale", scales);
        pb.setParameter("offset", offsets);
        pb.setParameter("ROI", roi);
        pb.setParameter("noData", rangeND);
        pb.setParameter("useRoiAccessor", useRoiAccessor);
        pb.setParameter("destNoData", destNoData);

        return JAI.createRenderable("Rescaling", pb, hints);
    }

}
