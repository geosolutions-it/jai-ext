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
package it.geosolutions.jaiext.binarize;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderableImage;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ROI;
import javax.media.jai.RenderableOp;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderableRegistryMode;
import javax.media.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "Binarize" operation.
 *
 * <p> The "Binarize" operation takes one rendered or renderable single-banded
 * source image and a threshold value and applies a thresholding operation to
 * the produce a bilevel image. Users can define ROI on where the pixel must be
 * calculated, keeping external pixel to 0. Also it is possible to define a nodata
 * variable for setting NoData values always to 0.
 *
 * <p> By default the destination image bounds are equal to those of the
 * source image.  The <code>SampleModel</code> of the destination image is
 * an instance of <code>MultiPixelPackedSampleModel</code>.
 * 
 * <p> The pseudocode for "Binarize" is as follows:
 * <pre> 
 *      dst(x, y) = src(x, y) >= threshold ? 1 : 0;
 * </pre>
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>Binarize</td></tr>
 * <tr><td>LocalName</td>   <td>Binarize</td></tr>
 * <tr><td>Vendor</td>      <td>it.geosolutions.jaiext</td></tr>
 * <tr><td>Description</td> <td>Thresholds a single banded image into a bilevel image.<td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/BinarizeDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.1</td></tr>
 * <tr><td>arg0Desc</td>    <td>The threshold value.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The ROI value.</td></tr>
 * <tr><td>arg2Desc</td>    <td>The nodata value.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>      <th>Class Type</th>
 *                        <th>Default Value</th></tr>
 * <tr><td>threshold</td> <td>java.lang.Double</td>
 *                        <td>NO_PARAMETER_DEFAULT</td>
 * <tr><td>roi</td> <td>javax.media.jai.ROI</td>
 *                        <td>null</td>
 * <tr><td>nodata</td> <td>it.geosolutions.jaiext.range.Range</td>
 *                        <td>null</td>                        
 * </table></p>
 */
public class BinarizeDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     */
    private static final String[][] resources = {
        {"GlobalName",  "Binarize"},
        {"LocalName",   "Binarize"},
        {"Vendor",      "it.geosolutions.jaiext"},
        {"Description", JaiI18N.getString("BinarizeDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/BinarizeDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("BinarizeDescriptor2")},
        {"arg1Desc",    JaiI18N.getString("BinarizeDescriptor3")},
        {"arg2Desc",    JaiI18N.getString("BinarizeDescriptor4")}
    };

    /** The parameter name list*/
    private static final String[] paramNames = {
        "threshold", "roi", "nodata"
    };

    /**
     * The parameter class list for this operation.
     */
    private static final Class[] paramClasses = {
        java.lang.Double.class, javax.media.jai.ROI.class, it.geosolutions.jaiext.range.Range.class
    };

    /** The parameter default value list*/
    private static final Object[] paramDefaults = {
        NO_PARAMETER_DEFAULT, null, null
    };

    private static final String[] supportedModes = {
        "rendered",
        "renderable"
    };

    /** Constructor. */
    public BinarizeDescriptor() {
        super(resources, supportedModes, 1,
                paramNames, paramClasses, paramDefaults, null);
    }

    /**
     * Validates the input source.
     *
     * <p> It also checks if the input image is single banded
     */
    protected boolean validateSources(String modeName,
                                      ParameterBlock args,
                                      StringBuffer msg) {
        if (!super.validateSources(modeName, args, msg)) {
            return false;
        }
        // No check for not-rendered modes
        if (!modeName.equalsIgnoreCase("rendered"))
            return true;

        RenderedImage source = (RenderedImage)(args.getSource(0));
        int numBands = source.getSampleModel().getNumBands();
        if (numBands != 1){
            msg.append(getName() + " " +
                           JaiI18N.getString("BinarizeDescriptor1"));
            return false;         
        }

        return true;
    }


    /**
     * Binarize an image from a threshold value, taking into account the presence of ROI and NoData
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
     * @param threshold It must be of type java.lang.Double.
     * @param roi ROI object.
     * @param nodata NoData Range to use in calculation.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>threshold</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    Double threshold,
                                    ROI roi,
                                    Range nodata,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Binarize",
                                  RenderedRegistryMode.MODE_NAME);
        // Setting source
        pb.setSource("source0", source0);
        // Setting parameter
        pb.setParameter("threshold", threshold);
        pb.setParameter("roi", roi);
        pb.setParameter("nodata", nodata);
        // Creating the RenderedOp
        return JAI.create("Binarize", pb, hints);
    }

    /**
     * Binarize an image from a threshold value, taking into account the presence of ROI and NoData
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
     * @param threshold Argment must be of type java.lang.Double.
     * @param roi ROI object.
     * @param nodata NoData Range to use in calculation.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>threshold</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(RenderableImage source0,
                                                Double threshold,
                                                ROI roi,
                                                Range nodata,
                                                RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Binarize",
                                  RenderableRegistryMode.MODE_NAME);
        // Setting source
        pb.setSource("source0", source0);
        // Setting parameters
        pb.setParameter("threshold", threshold);
        pb.setParameter("roi", roi);
        pb.setParameter("nodata", nodata);
        // Creating the Renderable parameter
        return JAI.createRenderable("Binarize", pb, hints);
    }
}
