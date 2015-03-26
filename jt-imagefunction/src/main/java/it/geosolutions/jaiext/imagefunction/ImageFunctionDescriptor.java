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
package it.geosolutions.jaiext.imagefunction;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.ImageFunction;
import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PropertyGenerator;
import javax.media.jai.ROI;
import javax.media.jai.RenderableOp;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;

import com.sun.media.jai.util.PropertyGeneratorImpl;

/**
 * This property generator computes the properties for the operation "ImageFunction" dynamically.
 */
class ImageFunctionPropertyGenerator extends PropertyGeneratorImpl {

    /** Constructor. */
    public ImageFunctionPropertyGenerator() {
        super(new String[] { "COMPLEX" }, new Class[] { Boolean.class }, new Class[] {
                RenderedOp.class, RenderableOp.class });
    }

    /**
     * Returns the specified property.
     * 
     * @param name Property name.
     * @param opNode Operation node.
     */
    public Object getProperty(String name, Object opNode) {
        validate(name, opNode);

        if (name.equalsIgnoreCase("complex")) {
            if (opNode instanceof RenderedOp) {
                RenderedOp op = (RenderedOp) opNode;
                ParameterBlock pb = op.getParameterBlock();
                ImageFunction imFunc = (ImageFunction) pb.getObjectParameter(0);
                return imFunc.isComplex() ? Boolean.TRUE : Boolean.FALSE;
            } else if (opNode instanceof RenderableOp) {
                RenderableOp op = (RenderableOp) opNode;
                ParameterBlock pb = op.getParameterBlock();
                ImageFunction imFunc = (ImageFunction) pb.getObjectParameter(0);
                return imFunc.isComplex() ? Boolean.TRUE : Boolean.FALSE;
            }
        }

        return java.awt.Image.UndefinedProperty;
    }
}

/**
 * An <code>OperationDescriptor</code> describing the "ImageFunction" operation.
 * 
 * <p>
 * The "ImageFunction" operation generates an image on the basis of a functional description provided by an object which is an instance of a class
 * which implements the <code>ImageFunction</code> interface. The <i>(x,y)</i> coordinates passed to the <code>getElements()</code> methods of the
 * <code>ImageFunction</code> object are derived by applying an optional translation and scaling to the X- and Y-coordinates of the image. The image
 * X- and Y-coordinates as usual depend on the values of the minimum X- and Y- coordinates of the image which need not be zero. Specifically, the
 * function coordinates passed to <code>getElements()</code> are calculated from the image coordinates as:
 * 
 * <pre>
 * functionX = xScale * (imageX - xTrans);
 * functionY = yScale * (imageY - yTrans);
 * </pre>
 * 
 * This implies that the pixel at coordinates <i>(xTrans,yTrans)</i> will be assigned the value of the function at <i>(0,0)</i>.
 * 
 * <p>
 * The number of bands in the destination image must be equal to the value returned by the <code>getNumElements()</code> method of the
 * <code>ImageFunction</code> unless the <code>isComplex()</code> method of the <code>ImageFunction</code> returns <code>true</code> in which case it
 * will be twice that. The data type of the destination image is determined by the <code>SampleModel</code> specified by an <code>ImageLayout</code>
 * object provided via a hint. If no layout hint is provided, the data type will default to single-precision floating point. The double precision
 * floating point form of the <code>getElements()</code> method of the <code>ImageFunction</code> will be invoked if and only if the data type is
 * specified to be <code>double</code>. For all other data types the single precision form of <code>getElements()</code> will be invoked and the
 * destination sample values will be clamped to the data type of the image.
 * 
 * <p>
 * The width and height of the image are provided explicitely as parameters. These values override the width and height specified via an
 * <code>ImageLayout</code> if such is provided.
 * 
 * <p>
 * "ImageFunction" defines a PropertyGenerator that sets the "COMPLEX" property of the image to <code>java.lang.Boolean.TRUE</code> or
 * <code>java.lang.Boolean.FALSE</code> depending on whether the <code>isComplex()</code> method of the <code>ImageFunction</code> parameter returns
 * <code>true</code> or <code>false</code>, respectively. This property may be retrieved by calling the <code>getProperty()</code> method with
 * "COMPLEX" as the property name.
 * 
 * <p>
 * It should be pointed out that users can define a valid area using an input {@link ROI} object, and also can define NoData values by using an input
 * NoData {@link Range}.
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
 * <td>ImageFunction</td>
 * </tr>
 * <tr>
 * <td>LocalName</td>
 * <td>ImageFunction</td>
 * </tr>
 * <tr>
 * <td>Vendor</td>
 * <td>it.geosolutions.jaiext</td>
 * </tr>
 * <tr>
 * <td>Description</td>
 * <td>Generates an image from a functional description.</td>
 * </tr>
 * <tr>
 * <td>DocURL</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>Version</td>
 * <td>1.0</td>
 * </tr>
 * <tr>
 * <td>arg0Desc</td>
 * <td>The function object.</td>
 * </tr>
 * <tr>
 * <td>arg1Desc</td>
 * <td>The image width.</td>
 * </tr>
 * <tr>
 * <td>arg2Desc</td>
 * <td>The image height.</td>
 * </tr>
 * <tr>
 * <td>arg3Desc</td>
 * <td>The X scale factor.</td>
 * </tr>
 * <tr>
 * <td>arg4Desc</td>
 * <td>The Y scale factor.</td>
 * </tr>
 * <tr>
 * <td>arg5Desc</td>
 * <td>The X translation.</td>
 * </tr>
 * <tr>
 * <td>arg6Desc</td>
 * <td>The Y translation.</td>
 * </tr>
 * <tr>
 * <td>arg7Desc</td>
 * <td>The ROI object.</td>
 * </tr>
 * <tr>
 * <td>arg8Desc</td>
 * <td>The NoData range object.</td>
 * </tr>
 * <tr>
 * <td>arg9Desc</td>
 * <td>The value for destination NoData.</td>
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
 * <td>function</td>
 * <td>javax.media.jai.ImageFunction</td>
 * <td>NO_PARAMETER_DEFAULT</td>
 * <tr>
 * <td>width</td>
 * <td>java.lang.Integer</td>
 * <td>NO_PARAMETER_DEFAULT</td>
 * <tr>
 * <td>height</td>
 * <td>java.lang.Integer</td>
 * <td>NO_PARAMETER_DEFAULT</td>
 * <tr>
 * <td>xScale</td>
 * <td>java.lang.Float</td>
 * <td>1.0F</td>
 * <tr>
 * <td>yScale</td>
 * <td>java.lang.Float</td>
 * <td>1.0F</td>
 * <tr>
 * <td>xTrans</td>
 * <td>java.lang.Float</td>
 * <td>0.0F</td>
 * <tr>
 * <td>yTrans</td>
 * <td>java.lang.Float</td>
 * <td>0.0F</td>
 * <tr>
 * <td>roi</td>
 * <td>javax.media.jai.ROI</td>
 * <td>null</td>
 * <tr>
 * <td>nodata</td>
 * <td>it.geosolutions.jaiext.range.Range</td>
 * <td>null</td>
 * <tr>
 * <td>destNoData</td>
 * <td>java.lang.Float</td>
 * <td>0.0F</td>
 * </table>
 * </p>
 * 
 */
public class ImageFunctionDescriptor extends OperationDescriptorImpl {

    /** Constructor. */
    public ImageFunctionDescriptor() {
        super(new String[][] { { "GlobalName", "ImageFunction" }, { "LocalName", "ImageFunction" },
                { "Vendor", "it.geosolutions.jaiext" },
                { "Description", JaiI18N.getString("ImageFunctionDescriptor0") }, { "DocURL", "" },
                { "Version", JaiI18N.getString("DescriptorVersion") },
                { "arg0Desc", JaiI18N.getString("ImageFunctionDescriptor1") },
                { "arg1Desc", JaiI18N.getString("ImageFunctionDescriptor2") },
                { "arg2Desc", JaiI18N.getString("ImageFunctionDescriptor3") },
                { "arg3Desc", JaiI18N.getString("ImageFunctionDescriptor4") },
                { "arg4Desc", JaiI18N.getString("ImageFunctionDescriptor5") },
                { "arg5Desc", JaiI18N.getString("ImageFunctionDescriptor6") },
                { "arg6Desc", JaiI18N.getString("ImageFunctionDescriptor7") },
                { "arg6Desc", JaiI18N.getString("ImageFunctionDescriptor8") },
                { "arg6Desc", JaiI18N.getString("ImageFunctionDescriptor9") },
                { "arg6Desc", JaiI18N.getString("ImageFunctionDescriptor10") } }, 0, new Class[] {
                javax.media.jai.ImageFunction.class, java.lang.Integer.class,
                java.lang.Integer.class, java.lang.Float.class, java.lang.Float.class,
                java.lang.Float.class, java.lang.Float.class, javax.media.jai.ROI.class,
                it.geosolutions.jaiext.range.Range.class, java.lang.Float.class }, new String[] {
                "function", "width", "height", "xScale", "yScale", "xTrans", "yTrans", "roi",
                "nodata", "destNoData" }, new Object[] { NO_PARAMETER_DEFAULT,
                NO_PARAMETER_DEFAULT, NO_PARAMETER_DEFAULT, new Float(1.0F), new Float(1.0F), // unity scale
                new Float(0.0F), new Float(0.0F), // zero translation
                null, null, new Float(0.0F) });
    }

    /**
     * Returns an array of <code>PropertyGenerators</code> implementing property inheritance for the "ImageFunction" operation.
     * 
     * @return An array of property generators.
     */
    public PropertyGenerator[] getPropertyGenerators() {
        PropertyGenerator[] pg = new PropertyGenerator[1];
        pg[0] = new ImageFunctionPropertyGenerator();
        return pg;
    }

    /**
     * Generates an image from a functional description.
     * 
     * <p>
     * Creates a <code>ParameterBlockJAI</code> from all supplied arguments except <code>hints</code> and invokes
     * {@link JAI#create(String,ParameterBlock,RenderingHints)}.
     * 
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderedOp
     * 
     * @param function The functional description.
     * @param width The image width.
     * @param height The image height.
     * @param xScale The X scale factor. May be <code>null</code>.
     * @param yScale The Y scale factor. May be <code>null</code>.
     * @param xTrans The X translation. May be <code>null</code>.
     * @param yTrans The Y translation. May be <code>null</code>.
     * @param roi The optional ROI . May be <code>null</code>.
     * @param nodata The optional NoData Range used for checking invalid values May be <code>null</code>.
     * @param destNoData The Y translation.
     * @param hints The <code>RenderingHints</code> to use. May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>function</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>width</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>height</code> is <code>null</code>.
     */
    public static RenderedOp create(ImageFunction function, Integer width, Integer height,
            Float xScale, Float yScale, Float xTrans, Float yTrans, ROI roi, Range nodata,
            float destNoData, RenderingHints hints) {
        // Creating the parameter block
        ParameterBlockJAI pb = new ParameterBlockJAI("ImageFunction",
                RenderedRegistryMode.MODE_NAME);
        // Setting the parameters (No Source is needed)
        pb.setParameter("function", function);
        pb.setParameter("width", width);
        pb.setParameter("height", height);
        pb.setParameter("xScale", xScale);
        pb.setParameter("yScale", yScale);
        pb.setParameter("xTrans", xTrans);
        pb.setParameter("yTrans", yTrans);
        pb.setParameter("roi", roi);
        pb.setParameter("nodata", nodata);
        pb.setParameter("destNoData", destNoData);

        return JAI.create("ImageFunction", pb, hints);
    }

}
