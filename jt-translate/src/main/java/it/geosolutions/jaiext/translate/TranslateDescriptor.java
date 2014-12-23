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
package it.geosolutions.jaiext.translate;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PropertyGenerator;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;

import com.sun.media.jai.opimage.RIFUtil;
import com.sun.media.jai.util.PropertyGeneratorImpl;

class TranslatePropertyGenerator extends PropertyGeneratorImpl {

    /** Constructor. */
    public TranslatePropertyGenerator() {
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

            // Retrieve the translation values.
            float tx = pb.getFloatParameter(0);
            float ty = pb.getFloatParameter(1);

            // Create a transform representing the translation.
            AffineTransform transform = AffineTransform.getTranslateInstance((double) tx,
                    (double) ty);
            // Create the translated ROI.
            ROI dstROI = srcROI.transform(transform);

            // Retrieve the destination bounds.
            Rectangle dstBounds = op.getBounds();

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
 * An <code>OperationDescriptor</code> describing the "Translate" operation.
 * 
 * <p>
 * The "Translate" operation copies an image to a new location in the plane.
 * 
 * <p>
 * For each pixel (x, y) of the destination, the source value at the position (x - xTrans, y - yTrans) is constructed and written to the destination.
 * Both xTrans and yTrans must be integral, else an exception is thrown. No ROI or No Data are considered, if these features are requested the
 * it.geosolutions.jaiext.scale.ScaleDescriptor should be used.
 * 
 * <p>
 * It may be noted that the minX, minY, width and height hints as specified through the <code>JAI.KEY_IMAGE_LAYOUT</code> hint in the
 * <code>RenderingHints</code> object are not honored, as this operator calculates the destination image bounds itself. The other
 * <code>ImageLayout</code> hints, like tileWidth and tileHeight, however are honored.
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
 * "Translate" defines a PropertyGenerator that performs an identical transformation on the "ROI" property of the source image, which can be retrieved
 * by calling the <code>getProperty</code> method with "ROI" as the property name.
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
 * <td>Translate</td>
 * </tr>
 * <tr>
 * <td>LocalName</td>
 * <td>Translate</td>
 * </tr>
 * <tr>
 * <td>Vendor</td>
 * <td>it.geosolutions.jaiext</td>
 * </tr>
 * <tr>
 * <td>Description</td>
 * <td>Translate an Image on the X and Y axes.</td>
 * </tr>
 * <tr>
 * <td>DocURL</td>
 * <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/TranslateDescriptor.html</td>
 * </tr>
 * <tr>
 * <td>Version</td>
 * <td>1.0</td>
 * </tr>
 * <tr>
 * <td>arg0Desc</td>
 * <td>The X translation value.</td>
 * </tr>
 * <tr>
 * <td>arg1Desc</td>
 * <td>The Y translation value.</td>
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
 * <td>xTrans</td>
 * <td>java.lang.Float</td>
 * <td>0.0F</td>
 * <tr>
 * <td>yTrans</td>
 * <td>java.lang.Float</td>
 * <td>0.0F</td>
 * </table>
 * </p>
 * 
 * @see javax.media.jai.Interpolation
 * @see javax.media.jai.OperationDescriptor
 */
public class TranslateDescriptor extends OperationDescriptorImpl {

    private static final float TOLERANCE = 0.01F;
    
    /**
     * The resource strings that provide the general documentation and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
            { "GlobalName", "Translate" },
            { "LocalName", "Translate" },
            { "Vendor", "it.geosolutions.jaiext" },
            { "Description", JaiI18N.getString("TranslateDescriptor0") },
            {
                    "DocURL",
                    "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/TranslateDescriptor.html" },
            { "Version", JaiI18N.getString("DescriptorVersion") },
            { "arg0Desc", JaiI18N.getString("TranslateDescriptor1") },
            { "arg1Desc", JaiI18N.getString("TranslateDescriptor2") },
            { "arg2Desc", JaiI18N.getString("TranslateDescriptor3") }
            
    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = { java.lang.Float.class, java.lang.Float.class,
        javax.media.jai.Interpolation.class};

    /** The parameter name list for this operation. */
    private static final String[] paramNames = { "xTrans", "yTrans", "interpolation" };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = { new Float(0.0F), new Float(0.0F),
        Interpolation.getInstance(Interpolation.INTERP_NEAREST) };

    public TranslateDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    
    /**
     * Returns an array of <code>PropertyGenerators</code> implementing
     * property inheritance for the "Translate" operation
     *
     * @return  An array of property generators.
     */
    public PropertyGenerator[] getPropertyGenerators() {
        PropertyGenerator[] pg = new PropertyGenerator[1];
        pg[0] = new TranslatePropertyGenerator();
        return pg;
    }
    
    /**
     * Translate an image to a new location.
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
     * @param xTrans The displacement in X direction. May be <code>null</code>.
     * @param yTrans The displacement in Y direction. May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use. May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0, Float xTrans, Float yTrans,
             Interpolation interpolation, RenderingHints hints) {
        ParameterBlockJAI pb = new ParameterBlockJAI("Translate", RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("xTrans", xTrans);
        pb.setParameter("yTrans", yTrans);
        pb.setParameter("interpolation", interpolation);

        ImageLayout layout = RIFUtil.getImageLayoutHint(hints); 
        
        if ((Math.abs(xTrans - xTrans.intValue()) > TOLERANCE)
                || (Math.abs(yTrans - yTrans.intValue()) > TOLERANCE) || layout != null) {
            throw new UnsupportedOperationException("Translate Operation can be used only for integral displacements. If"
                    + "a layout is present, the translate operation cannot deal with");
        }
        return JAI.create("Translate", pb, hints);
    }
}
