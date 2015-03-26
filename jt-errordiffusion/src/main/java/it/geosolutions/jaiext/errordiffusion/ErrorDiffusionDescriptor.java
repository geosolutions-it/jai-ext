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
 * Users may also define a ROI and a NoData Range for reducing computation area or masking unwanted pixel values.
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
 * <td>it.geosolutions.jaiext</td>
 * </tr>
 * <tr>
 * <td>Description</td>
 * <td>Performs error diffusion color quantization using a specified color map and error filter.</td>
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
 * <td>Input colormap.</td>
 * </tr>
 * <tr>
 * <td>arg1Desc</td>
 * <td>Input errordiffusion kernel.</td>
 * </tr>
 * <tr>
 * <td>arg2Desc</td>
 * <td>Optional ROI object to use in computation.</td>
 * </tr>
 * <tr>
 * <td>arg3Desc</td>
 * <td>Optional Range of NoData values to use in computation.</td>
 * </tr>
 * <tr>
 * <td>arg4Desc</td>
 * <td>Destination No Data value used when the computation cannot be performed.</td>
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
 * <td>Integer</td>
 * <td>0</td>
 * </table>
 * </p>
 */
public class ErrorDiffusionDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation and specify the parameter list for the "ErrorDiffusion" operation.
     */
    private static final String[][] resources = { { "GlobalName", "ErrorDiffusion" },
            { "LocalName", "ErrorDiffusion" }, { "Vendor", "it.geosolutions.jaiext" },
            { "Description", JaiI18N.getString("ErrorDiffusionDescriptor0") }, { "DocURL", "" },
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
        // Setting source
        pb.setSource("source0", source0);
        // Setting parameters
        pb.setParameter("colorMap", colorMap);
        pb.setParameter("errorKernel", errorKernel);
        pb.setParameter("roi", roi);
        pb.setParameter("nodata", nodata);
        pb.setParameter("destNoData", destNoData);

        return JAI.create("ErrorDiffusion", pb, hints);
    }

}
