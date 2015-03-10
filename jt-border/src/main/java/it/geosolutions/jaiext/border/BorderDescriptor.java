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
package it.geosolutions.jaiext.border;

import it.geosolutions.jaiext.range.Range;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.OperationNode;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "Border" operation.
 * 
 * <p>
 * The Border operation adds a border around a rendered image. The size of the border is specified in pixels by the left, right, top, and bottom
 * padding parameters, corresponding to the four sides of the source image. These paddings may not be less than 0.
 * 
 * <p>
 * The pixel values of the added border area will be set according to the algorithm of the <code>BorderExtender</code> passed as a parameter. The
 * <code>BorderExtender</code>s provide the ability to extend the border by:
 * <ul>
 * <li>filling it with zeros (<code>BorderExtenderZero</code>);
 * <li>filling it with constants (<code>BorderExtenderConstant</code>);
 * <li>copying the edge and corner pixels (<code>BorderExtenderCopy</code>);
 * <li>reflecting about the edges of the image (<code>BorderExtenderReflect</code>); or,
 * <li>"wrapping" the image plane toroidally, that is, joining opposite edges of the image (<code>BorderExtenderWrap</code>).
 * </ul>
 * 
 * <p>
 * If No Data are present the user can provide a Range of No Data for handling the No Data values and a Destination No Data value for setting the
 * output No Data value.
 * </p>
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
 * <td>Border</td>
 * </tr>
 * <tr>
 * <td>LocalName</td>
 * <td>Border</td>
 * </tr>
 * <tr>
 * <td>Vendor</td>
 * <td>it.geosolutions.jaiext</td>
 * </tr>
 * <tr>
 * <td>Description</td>
 * <td>Operation which adds borders to the input image.</td>
 * </tr>
 * <tr>
 * <td>DocURL</td>
 * <td>Not Defined</td>
 * </tr>
 * <tr>
 * <td>Version</td>
 * <td>1.0</td>
 * </tr>
 * <tr>
 * <td>arg0Desc</td>
 * <td>Image's left padding.</td>
 * </tr>
 * <tr>
 * <td>arg1Desc</td>
 * <td>Image's right padding.</td>
 * </tr>
 * <tr>
 * <td>arg2Desc</td>
 * <td>Image's top padding.</td>
 * </tr>
 * <tr>
 * <td>arg3Desc</td>
 * <td>Image's bottom padding.</td>
 * </tr>
 * <tr>
 * <td>arg4Desc</td>
 * <td>Border Extender used.</td>
 * </tr>
 * <tr>
 * <td>arg5Desc</td>
 * <td>No Data Range used.</td>
 * </tr>
 * <tr>
 * <td>arg6Desc</td>
 * <td>Destination No Data value.</td>
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
 * <td>leftPad</td>
 * <td>java.lang.Integer</td>
 * <td>0</td>
 * <tr>
 * <td>rightPad</td>
 * <td>java.lang.Integer</td>
 * <td>0</td>
 * <tr>
 * <td>topPad</td>
 * <td>java.lang.Integer</td>
 * <td>0</td>
 * <tr>
 * <td>bottomPad</td>
 * <td>java.lang.Integer</td>
 * <td>0</td>
 * <tr>
 * <td>type</td>
 * <td>javax.media.jai.BorderExtender</td>
 * <td>javax.media.jai.BorderExtenderZero</td>
 * <tr>
 * <td>noData</td>
 * <td>it.geosolutions.jaiext.range.Range</td>
 * <td>null</td>
 * <tr>
 * <tr>
 * <td>destNoData</td>
 * <td>java.lang.Double</td>
 * <td>0</td>
 * </table>
 * </p>
 * 
 */
public class BorderDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation and specify the parameter list for this operation.
     */
    private static final String[][] resources = { { "GlobalName", "Border" },
            { "LocalName", "Border" }, { "Vendor", "it.geosolutions.jaiext" },
            { "Description", "Operation which adds borders to the input image" },
            { "DocURL", "Not Defined" }, { "Version", "1.0" },
            { "arg0Desc", "Image's left padding" }, { "arg1Desc", "Image's right padding" },
            { "arg2Desc", "Image's top padding" }, { "arg3Desc", "Image's bottom padding" },
            { "arg4Desc", "Border Extender used" }, { "arg5Desc", "No Data Range used" },
            { "arg6Desc", "Destination No Data value" } };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = { "leftPad", "rightPad", "topPad", "bottomPad",
            "type", "noData", "destNoData" };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = { java.lang.Integer.class, java.lang.Integer.class,
            java.lang.Integer.class, java.lang.Integer.class, javax.media.jai.BorderExtender.class,
            it.geosolutions.jaiext.range.Range.class, java.lang.Double.class };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = { 0, 0, 0, 0,
            BorderExtender.createInstance(BorderExtender.BORDER_ZERO), null, 0d };

    /** Constructor. */
    public BorderDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /**
     * Calculates the region over which two distinct renderings of the "Border" operation may be expected to differ.
     * 
     * <p>
     * The operation returns a <code>Shape</code> or <code>null</code> in the rendered mode and <code>null</code> in all other modes.
     * 
     * @param modeName The name of the mode.
     * @param oldParamBlock The previous sources and parameters.
     * @param oldHints The previous hints.
     * @param newParamBlock The current sources and parameters.
     * @param newHints The current hints.
     * @param node The affected node in the processing chain (ignored).
     * 
     * @return The region over which the data of two renderings of this operation may be expected to be invalid or <code>null</code> if there is no
     *         common region of validity. A non-<code>null</code> empty region indicates that the operation would produce identical data over the
     *         bounds of the old rendering although perhaps not over the area occupied by the <i>tiles</i> of the old rendering.
     * 
     * @throws IllegalArgumentException if <code>modeName</code> is <code>null</code> or if the operation requires either sources or parameters and
     *         either <code>oldParamBlock</code> or <code>newParamBlock</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>oldParamBlock</code> or <code>newParamBlock</code> do not contain sufficient sources or parameters
     *         for the operation in question.
     */
    public Object getInvalidRegion(String modeName, ParameterBlock oldParamBlock,
            RenderingHints oldHints, ParameterBlock newParamBlock, RenderingHints newHints,
            OperationNode node) {
        if ((modeName == null)
                || ((getNumSources() > 0 || getNumParameters() > 0) && (oldParamBlock == null || newParamBlock == null))) {

            throw new IllegalArgumentException("Some parameters are null");
        }

        int numSources = getNumSources();

        if ((numSources > 0)
                && (oldParamBlock.getNumSources() != numSources || newParamBlock.getNumSources() != numSources)) {

            throw new IllegalArgumentException("The number of source images is different");

        }

        int numParams = getParameterListDescriptor(modeName).getNumParameters();

        if ((numParams > 0)
                && (oldParamBlock.getNumParameters() != numParams || newParamBlock
                        .getNumParameters() != numParams)) {

            throw new IllegalArgumentException("The number of input parameters is different");
        }

        // Return null if the RenderingHints, source, left padding, or
        // top padding changed.
        if (!modeName.equalsIgnoreCase(RenderedRegistryMode.MODE_NAME)
                || (oldHints == null && newHints != null) || (oldHints != null && newHints == null)
                || (oldHints != null && !oldHints.equals(newHints))
                || !oldParamBlock.getSource(0).equals(newParamBlock.getSource(0))
                || oldParamBlock.getIntParameter(0) != // left pad
                newParamBlock.getIntParameter(0) || oldParamBlock.getIntParameter(2) != // top pad
                newParamBlock.getIntParameter(2)) {
            return null;
        }

        Shape invalidRegion = null;

        if (!oldParamBlock.getObjectParameter(4).equals(newParamBlock.getObjectParameter(4))) {
            // BorderExtender changed.

            // Get source and the left and top padding.
            RenderedImage src = oldParamBlock.getRenderedSource(0);
            int leftPad = oldParamBlock.getIntParameter(0);
            int topPad = oldParamBlock.getIntParameter(2);

            // Get source bounds.
            Rectangle srcBounds = new Rectangle(src.getMinX(), src.getMinY(), src.getWidth(),
                    src.getHeight());

            // Get destination bounds.
            Rectangle dstBounds = new Rectangle(srcBounds.x - leftPad, srcBounds.y - topPad,
                    srcBounds.width + leftPad + oldParamBlock.getIntParameter(1), srcBounds.height
                            + topPad + oldParamBlock.getIntParameter(3));

            // Determine invalid area by subtracting source bounds.
            Area invalidArea = new Area(dstBounds);
            invalidArea.subtract(new Area(srcBounds));
            invalidRegion = invalidArea;

        } else if ((newParamBlock.getIntParameter(1) < // new R < old R
                oldParamBlock.getIntParameter(1) && newParamBlock.getIntParameter(3) <= // new B <= old B
                oldParamBlock.getIntParameter(3))
                || (newParamBlock.getIntParameter(3) < // new B < old B
                oldParamBlock.getIntParameter(3) && newParamBlock.getIntParameter(1) <= // new R <= old R
                oldParamBlock.getIntParameter(1))) {
            // One or both right and bottom padding decreased.

            // Get source and the left and top padding.
            RenderedImage src = oldParamBlock.getRenderedSource(0);
            int leftPad = oldParamBlock.getIntParameter(0);
            int topPad = oldParamBlock.getIntParameter(2);

            // Get source bounds.
            Rectangle srcBounds = new Rectangle(src.getMinX(), src.getMinY(), src.getWidth(),
                    src.getHeight());

            // Get old destination bounds.
            Rectangle oldBounds = new Rectangle(srcBounds.x - leftPad, srcBounds.y - topPad,
                    srcBounds.width + leftPad + oldParamBlock.getIntParameter(1), srcBounds.height
                            + topPad + oldParamBlock.getIntParameter(3));

            // Get new destination bounds.
            Rectangle newBounds = new Rectangle(srcBounds.x - leftPad, srcBounds.y - topPad,
                    srcBounds.width + leftPad + newParamBlock.getIntParameter(1), srcBounds.height
                            + topPad + newParamBlock.getIntParameter(3));

            // Determine invalid area by subtracting new from old bounds.
            Area invalidArea = new Area(oldBounds);
            invalidArea.subtract(new Area(newBounds));
            invalidRegion = invalidArea;

        } else {
            // Either nothing changed or one or both of the right and bottom
            // padding was increased.
            invalidRegion = new Rectangle();
        }

        return invalidRegion;
    }

    /**
     * Adds a border around an image.
     * 
     * <p>
     * Creates a <code>ParameterBlockJAI</code> from all supplied arguments except <code>hints</code> and invokes
     * {@link JAI#create(String,ParameterBlock,RenderingHints)}.
     * 
     * 
     * @param source0 <code>RenderedImage</code> source 0.
     * @param leftPad The image's left padding. May be <code>null</code>.
     * @param rightPad The image's right padding. May be <code>null</code>.
     * @param topPad The image's top padding. May be <code>null</code>.
     * @param bottomPad The image's bottom padding. May be <code>null</code>.
     * @param type The border type. May be <code>null</code>.
     * @param noData No Data Range used. May be <code>null</code>.
     * @param destinationNoData Value for the output No Data.
     * @param hints The <code>RenderingHints</code> to use. May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0, int leftPad, int rightPad, int topPad,
            int bottomPad, BorderExtender type, Range noData, double destinationNoData,
            RenderingHints hints) {
        ParameterBlockJAI pb = new ParameterBlockJAI("Border", RenderedRegistryMode.MODE_NAME);
        // Setting of the source
        pb.setSource("source0", source0);
        // Setting of the parameters
        pb.setParameter("leftPad", leftPad);
        pb.setParameter("rightPad", rightPad);
        pb.setParameter("topPad", topPad);
        pb.setParameter("bottomPad", bottomPad);
        pb.setParameter("type", type);
        pb.setParameter("noData", noData);
        pb.setParameter("destNoData", destinationNoData);

        return JAI.create("Border", pb, hints);
    }
}
