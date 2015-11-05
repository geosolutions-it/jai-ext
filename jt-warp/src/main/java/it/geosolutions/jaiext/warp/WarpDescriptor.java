/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
*    http://www.geo-solutions.it/
*    Copyright 2014 GeoSolutions


* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at

* http://www.apache.org/licenses/LICENSE-2.0

* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package it.geosolutions.jaiext.warp;

import it.geosolutions.jaiext.interpolators.InterpolationBicubic;
import it.geosolutions.jaiext.interpolators.InterpolationBilinear;
import it.geosolutions.jaiext.range.Range;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.BorderExtender;
import javax.media.jai.GeometricOpImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptor;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.OperationRegistry;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.PropertyGenerator;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.Warp;
import javax.media.jai.operator.ConstantDescriptor;
import javax.media.jai.registry.RenderedRegistryMode;

import org.jaitools.imageutils.ImageLayout2;

import com.sun.media.jai.util.PropertyGeneratorImpl;

/**
 * This property generator computes the properties for the operation "Warp" dynamically.
 */
class WarpPropertyGenerator extends PropertyGeneratorImpl {

    /** Constructor. */
    public WarpPropertyGenerator() {
        super(new String[] { "ROI" }, new Class[] { ROI.class }, new Class[] { RenderedOp.class });
    }

    /**
     * Returns the specified property.
     * 
     * @param name Property name.
     * @param opNode Operation node.
     */
    public Object getProperty(String name, Object opNode) {
        validate(name, opNode);

        if (opNode instanceof RenderedOp && name.equalsIgnoreCase("roi")) {
            RenderedOp op = (RenderedOp) opNode;

            ParameterBlock pb = op.getParameterBlock();

            // Retrieve the rendered source image and its ROI.
            RenderedImage src = (RenderedImage) pb.getRenderedSource(0);
            Object property = src.getProperty("ROI");
            if (property == null || property.equals(java.awt.Image.UndefinedProperty)
                    || !(property instanceof ROI)) {
                // Check on the parameterBlock
                if(pb.getObjectParameter(3) != null){
                    property = pb.getObjectParameter(3);
                }else{
                    return java.awt.Image.UndefinedProperty;
                }
            }

            // Return undefined also if source ROI is empty.
            ROI srcROI = (ROI) property;
            if (srcROI.getBounds().isEmpty()) {
                return java.awt.Image.UndefinedProperty;
            }

            // Retrieve the Interpolation object.
            Interpolation interp = (Interpolation) pb.getObjectParameter(1);

            // Determine the effective source bounds.
            Rectangle srcBounds = null;
            PlanarImage dst = op.getRendering();
            if (dst instanceof GeometricOpImage
                    && ((GeometricOpImage) dst).getBorderExtender() == null) {
                srcBounds = new Rectangle(src.getMinX() + interp.getLeftPadding(), src.getMinY()
                        + interp.getTopPadding(), src.getWidth() - interp.getWidth() + 1,
                        src.getHeight() - interp.getHeight() + 1);
            } else {
                srcBounds = new Rectangle(src.getMinX(), src.getMinY(), src.getWidth(),
                        src.getHeight());
            }

            // If necessary, clip the ROI to the effective source bounds.
            if (!srcBounds.contains(srcROI.getBounds())) {
                srcROI = srcROI.intersect(new ROIShape(srcBounds));
            }

            // Retrieve the Warp object.
            Warp warp = (Warp) pb.getObjectParameter(0);

            // Setting constant image to be warped as a ROI
            Rectangle dstBounds = op.getBounds();

            // Setting layout of the constant image
            ImageLayout2 layout = new ImageLayout2();
            int minx = (int) srcBounds.getMinX();
            int miny = (int) srcBounds.getMinY();
            int w = (int) srcBounds.getWidth();
            int h = (int) srcBounds.getHeight();
            layout.setMinX(minx);
            layout.setMinY(miny);
            layout.setWidth(w);
            layout.setHeight(h);
            layout.setTileWidth(src.getTileWidth());
            layout.setTileHeight(src.getTileHeight());
            RenderingHints hints = op.getRenderingHints();
            hints.add(new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout));

            final PlanarImage constantImage = ConstantDescriptor.create(new Float(w), new Float(h),
                    new Byte[] { (byte) 255 }, hints);

            // Warping constant Image to get warped roi.
            final BorderExtender extender = BorderExtender
                    .createInstance(BorderExtender.BORDER_COPY);
            PlanarImage roiImage = null;

            // Make sure to specify tileCache, tileScheduler, tileRecyclier, by cloning hints.
            RenderingHints warpingHints = op.getRenderingHints();
            warpingHints.remove(JAI.KEY_IMAGE_LAYOUT);

            // Creating warped roi by the same way (Warp, Interpolation, source ROI) we warped the
            // input image.
            if (interp instanceof InterpolationBilinear || interp instanceof javax.media.jai.InterpolationBilinear) {
                roiImage = new WarpBilinearOpImage(constantImage, extender, warpingHints,
                        null, warp, interp, srcROI,null, null);
            } else if(interp instanceof InterpolationBicubic || interp instanceof javax.media.jai.InterpolationBicubic) {
                roiImage = new WarpBicubicOpImage(constantImage, extender, warpingHints,
                        null, warp, interp, srcROI,null, null);
            } else {
                roiImage = new WarpNearestOpImage(constantImage, warpingHints, null, warp,
                        interp, srcROI,null, null);
            }

            ROI dstROI = new ROI(roiImage, 1);

            // If necessary, clip the warped ROI to the destination bounds.
            if (!dstBounds.contains(dstROI.getBounds())) {
                dstROI = dstROI.intersect(new ROIShape(dstBounds));
            }

            // Return the warped and possibly clipped ROI.
            return dstROI;
        }

        return java.awt.Image.UndefinedProperty;
    }
}

/**
 * An <code>OperationDescriptor</code> describing the "Warp" operation.
 * 
 * <p>
 * The "Warp" operation performs (possibly filtered) general warping on an image.
 * 
 * <p>
 * The destination bounds may be specified by an {@link ImageLayout} hint provided via a {@link RenderingHints} supplied to the operation. If no
 * bounds are so specified, then the destination bounds will be set to the minimum bounding rectangle of the forward mapped source bounds calculated
 * using {@link Warp#mapSourceRect(Rectangle)} or, failing that, {@link Warp#mapSourcePoint(Point2D)} applied to the vertices of the source bounds. If
 * forward mapping by both methods is not viable, then an approximate affine mapping will be created and used to determine the destination bounds by
 * forward mapping the source bounds. If this approach also fails, then the destination bounds will be set to the source bounds.
 * 
 * <p>
 * "Warp" defines a PropertyGenerator that performs an identical transformation on the "ROI" property of the source image, which can be retrieved by
 * calling the <code>getProperty</code> method with "ROI" as the property name.
 * 
 * <p>
 * The parameter, "backgroundValues", is defined to fill the background with the user-specified background values. These background values will be
 * translated into background colors by the <code>ColorModel</code> when the image is displayed. With the default value, <code>{0.0}</code>, of this
 * parameter, the background pixels are filled with 0s. If the provided values are out of the data range of the destination image, they will be clamped into the
 * proper range. If the interpolation object implements "InterpolationNoData", then backgroundValues can be taken from the interpolation object.
 * 
 * <p>
 * It should be noted that this operation automatically adds a value of <code>Boolean.TRUE</code> for the
 * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> to the given <code>configuration</code> so that the operation is performed on the pixel values
 * instead of being performed on the indices into the color map if the source(s) have an <code>IndexColorModel</code>. This addition will take place
 * only if a value for the <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> has not already been provided by the user. Note that the
 * <code>configuration</code> Map is cloned before the new hint is added to it. The operation can be smart about the value of the
 * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> <code>RenderingHints</code>, i.e. while the default value for the
 * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> is <code>Boolean.TRUE</code>, in some cases the operator could set the default.
 * 
 * <p>
 * An optional ROI object can be passed to the descriptor. Also NoData can be defined with a Range object; NoData Range is taken from the interpolation object
 * if it implements the "InterpolationNoData" interface, else it is taken from the input parameter.
 * 
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
 * <td>Warp</td>
 * </tr>
 * <tr>
 * <td>LocalName</td>
 * <td>Warp</td>
 * </tr>
 * <tr>
 * <td>Vendor</td>
 * <td>it.geosolutions.jaiext</td>
 * </tr>
 * <tr>
 * <td>Description</td>
 * <td>Warps an image according to a specified Warp object, handling NoData and ROI.</td>
 * </tr>
 * <tr>
 * <td>DocURL</td>
 * <td>Not defined</td>
 * </tr>
 * <tr>
 * <td>Version</td>
 * <td>1.0</td>
 * </tr>
 * <tr>
 * <td>arg0Desc</td>
 * <td>The Warp object.</td>
 * </tr>
 * <tr>
 * <td>arg1Desc</td>
 * <td>The interpolation method.</td>
 * </tr>
 * <tr>
 * <td>arg2Desc</td>
 * <td>ROI object used.</td>
 * </tr>
 * <tr>
 * <td>arg3Desc</td>
 * <td>Background Values.</td>
 * </tr>
 * <tr>
 * <td>arg4Desc</td>
 * <td>NoData Range used.</td>
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
 * <td>warp</td>
 * <td>javax.media.jai.Warp</td>
 * <td>NO_PARAMETER_DEFAULT</td>
 * <tr>
 * <td>interpolation</td>
 * <td>javax.media.jai.Interpolation</td>
 * <td>null</td>
 * <tr>
 * <td>backgroundValues</td>
 * <td>double[]</td>
 * <td>{0.0}</td>
 * <tr>
 * <td>roi</td>
 * <td>javax.media.jai.ROI</td>
 * <td>null</td>
 * <tr>
 * <td>nodata</td>
 * <td>it.geosolutions.jaiext.range.Range</td>
 * <td>null</td>
 * </table>
 * </p>
 * 
 * @see javax.media.jai.Interpolation
 * @see javax.media.jai.Warp
 * @see javax.media.jai.OperationDescriptor
 */
public class WarpDescriptor extends OperationDescriptorImpl {

    private final static Logger LOGGER = Logger.getLogger(WarpDescriptor.class.toString());

    /**
     * Registers this descriptor if it is not already registered.
     * 
     * @return <code>true</code> in case the operation succeds, <code>false</code> otherwise.
     * 
     */
    public static final boolean register() {
        OperationRegistry registry = JAI.getDefaultInstance().getOperationRegistry();
        try {
            final OperationDescriptor op = new WarpDescriptor();
            final String descName = op.getName();

            if (registry.getDescriptor(RenderedRegistryMode.MODE_NAME, descName) != null)
                return false;

            registry.registerDescriptor(op);

            final RenderedImageFactory rif = new WarpRIF();
            registry.registerFactory(RenderedRegistryMode.MODE_NAME, descName,
                    "it.geosolutions.jaiext", rif);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
        }
        return false;

    }

    /**
     * The resource strings that provide the general documentation and specify the parameter list for the "Warp" operation.
     */
    private static final String[][] resources = {
            { "GlobalName", "Warp" },
            { "LocalName", "Warp" },
            { "Vendor", "it.geosolutions.jaiext" },
            { "Description", "Warps an image according to a specified Warp object." },
            {
                    "DocURL",
                    "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/ROIAwareWarpDescriptor.html" },
            { "Version", JaiI18N.getString("DescriptorVersion") },
            { "arg0Desc", JaiI18N.getString("WarpDescriptor1") },
            { "arg1Desc", JaiI18N.getString("WarpDescriptor2") },
            { "arg2Desc", JaiI18N.getString("WarpDescriptor3") },
            { "arg3Desc", JaiI18N.getString("WarpDescriptor4") },
            { "arg4Desc", JaiI18N.getString("WarpDescriptor5") }
            };

    /** The parameter names for the "Warp" operation. */
    private static final String[] paramNames = { "warp", "interpolation", "backgroundValues", "roi", "nodata" };

    /** The parameter class types for the "Warp" operation. */
    private static final Class[] paramClasses = { javax.media.jai.Warp.class,
            javax.media.jai.Interpolation.class, double[].class, javax.media.jai.ROI.class, it.geosolutions.jaiext.range.Range.class };

    /** The parameter default values for the "Warp" operation. */
    private static final Object[] paramDefaults = { NO_PARAMETER_DEFAULT,
            Interpolation.getInstance(Interpolation.INTERP_NEAREST), null, null, null };

    /** Constructor. */
    public WarpDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /**
     * Returns an array of <code>PropertyGenerators</code> implementing property inheritance for the "Warp" operation.
     * 
     * @return An array of property generators.
     */
    public PropertyGenerator[] getPropertyGenerators() {
        PropertyGenerator[] pg = new PropertyGenerator[1];
        pg[0] = new WarpPropertyGenerator();
        return pg;
    }

    /**
     * Warps an image according to a specified Warp object.
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
     * @param warp The warp object.
     * @param interpolation The interpolation method. May be <code>null</code>.
     * @param backgroundValues The user-specified background values. May be <code>null</code>.
     * @param sourceROI ROI object used in calculations. May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use. May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>warp</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0, Warp warp, Interpolation interpolation,
            double[] backgroundValues, ROI sourceROI, RenderingHints hints) {

        return create(source0, warp, interpolation, backgroundValues, sourceROI, null, hints);
    }
    
    /**
     * Warps an image according to a specified Warp object. NoData Range can be added
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
     * @param warp The warp object.
     * @param interpolation The interpolation method. May be <code>null</code>.
     * @param backgroundValues The user-specified background values. May be <code>null</code>.
     * @param sourceROI ROI object used in calculations. May be <code>null</code>.
     * @param noData NoData Range used in calculations. May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use. May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>warp</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0, Warp warp, Interpolation interpolation,
            double[] backgroundValues, ROI sourceROI, Range noData, RenderingHints hints) {
        ParameterBlockJAI pb = new ParameterBlockJAI("Warp", RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);
        pb.setParameter("warp", warp);
        pb.setParameter("interpolation", interpolation);
        if(backgroundValues!=null){
            pb.setParameter("backgroundValues", backgroundValues);
        }
        if (sourceROI != null) {
            pb.setParameter("roi", sourceROI);
        }
        
        if(noData!=null){
            pb.setParameter("nodata", noData);
        }

        return JAI.create("Warp", pb, hints);
    }
}
