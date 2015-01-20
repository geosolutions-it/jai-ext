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
package it.geosolutions.jaiext.bandselect;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderableImage;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderableOp;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderableRegistryMode;
import javax.media.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "BandSelect" operation.
 *
 * <p> The BandSelect operation chooses <code>N</code> bands from a
 * rendered or renderable source image and copies the pixel data of
 * these bands to the destination image in the order specified.  The
 * <code>bandIndices</code> parameter specifies the source band
 * indices, and its size (<code>bandIndices.length</code>) determines
 * the number of bands of the destination image.  The destination
 * image may have ay number of bands, and a particular band of the
 * source image may be repeated in the destination image by specifying
 * it multiple times in the <code>bandIndices</code> parameter.
 *
 * <p> Each of the <code>bandIndices</code> value should be a valid
 * band index number of the source image. For example, if the source
 * only has two bands, then 1 is a valid band index, but 3 is not. The
 * first band is numbered 0.
 *
 * <p> The destination pixel values are defined by the pseudocode:
 * <pre>
 * dst[x][y][b] = src[x][y][bandIndices[b]];
 * </pre>
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>BandSelect</td></tr>
 * <tr><td>LocalName</td>   <td>BandSelect</td></tr>
 * <tr><td>Vendor</td>      <td>com.sun.media.jai</td></tr>
 * <tr><td>Description</td> <td>Selects n number of bands from
 *                              an image.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/BandSelectDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The indices of the selected bands.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>        <th>Class Type</th>
 *                          <th>Default Value</th></tr>
 * <tr><td>bandIndices</td> <td>int[]</td>
 *                          <td>NO_PARAMETER_DEFAULT</td>
 * </table></p>
 *
 * @see javax.media.jai.OperationDescriptor
 */
public class BandSelectDescriptor extends OperationDescriptorImpl {

    /** serialVersionUID */
    private static final long serialVersionUID = -8249015686542274642L;

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "BandSelect"},
        {"LocalName",   "BandSelect"},
        {"Vendor",      "it.geosolutions.jaiext"},
        {"Description", JaiI18N.getString("BandSelectDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/BandSelectDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("BandSelectDescriptor1")}
    };

    /** The parameter class list for this operation. */
    private static final Class<?>[] parametersClasses = {
        int[].class
    };

    /** The parameter name list for this operation. */
    private static final String[] parametersNames = {
        "bandIndices"
    };

    /** The parameter default value list for this operation. */
    private static final Object[] parametersDefaults = {
        NO_PARAMETER_DEFAULT
    };

    private static final String[] supportedModes = { "rendered", "renderable" };

    /** Constructor. */
    public BandSelectDescriptor() {
        super(resources, supportedModes, 1, parametersNames, parametersClasses, parametersDefaults,
                null);
    }

    /**
     * Validates the input source and parameters.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method checks that "bandIndices" has a
     * length of at least 1 and does not contain any values less than
     * 0 or greater than the number of source bands minus 1.
     */
    public boolean validateArguments(String modeName, ParameterBlock args, StringBuffer message) {
        if (!super.validateArguments(modeName, args, message)) {
            return false;
        }

        if (!modeName.equalsIgnoreCase("rendered")) {
            return true;
        }

        int[] indices = (int[])args.getObjectParameter(0);
        if (indices.length < 1) {
            message.append(getName() + " " +
                           JaiI18N.getString("BandSelectDescriptor2"));
            return false;
        }

        RenderedImage src = args.getRenderedSource(0);

        int bands = src.getSampleModel().getNumBands();
        for (int i = 0; i < indices.length; i++) {
            if (indices[i] < 0 || indices[i] >= bands) {
                message.append(getName() + " " +
                               JaiI18N.getString("BandSelectDescriptor3"));
                return false;
            }
        }

        return true;
    }


    /**
     * Selects n number of bands from an image.
     *
     * <p>Creates a <code>ParameterBlockJAI</code> from all
     * supplied arguments except <code>hints</code> and invokes
     * {@link JAI#create(String,ParameterBlock,RenderingHints)}.
     *
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderedOp
     *
     * @param source0 <code>RenderedImage</code> source 0.
     * @param bandIndices The indices of the selected bands.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>bandIndices</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    int[] bandIndices,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("BandSelect",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("bandIndices", bandIndices);

        return JAI.create("BandSelect", pb, hints);
    }

    /**
     * Selects n number of bands from an image.
     *
     * <p>Creates a <code>ParameterBlockJAI</code> from all
     * supplied arguments except <code>hints</code> and invokes
     * {@link JAI#createRenderable(String,ParameterBlock,RenderingHints)}.
     *
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderableOp
     *
     * @param source0 <code>RenderableImage</code> source 0.
     * @param bandIndices The indices of the selected bands.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>bandIndices</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(RenderableImage source0,
                                                int[] bandIndices,
                                                RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("BandSelect",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("bandIndices", bandIndices);

        return JAI.createRenderable("BandSelect", pb, hints);
    }
}
