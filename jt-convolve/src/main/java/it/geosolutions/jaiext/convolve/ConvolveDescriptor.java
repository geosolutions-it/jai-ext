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
package it.geosolutions.jaiext.convolve;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PropertyGenerator;
import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;

import com.sun.media.jai.util.AreaOpPropertyGenerator;

/**
 * An <code>OperationDescriptor</code> describing the "Convolve" operation.
 * 
 * <p>
 * Convolution is a spatial operation that computes each output sample by multiplying elements of a kernel with the samples surrounding a particular
 * source sample.
 * 
 * <p>
 * For each destination sample, the kernel is rotated 180 degrees and its "key element," or origin, is placed over the source pixel corresponding with
 * the destination pixel. The kernel elements are multiplied with the source pixels beneath them, and the resulting products are summed together to
 * produce the destination sample value.
 * 
 * <p> This operation is able to check if each input pixel is contained inside the provided ROI and if it is not a NoData value. If a pixel in the kernel 
 * is outside ROI or a NoData, the related Kernel value is not calculated and destination No Data value is returned.
 * 
 * <p>
 * Pseudocode for the convolution operation on a single sample dst[x][y] is as follows, assuming the kernel is of size width x height and has already
 * been rotated through 180 degrees. The kernel's Origin element is located at position (xOrigin, yOrigin):
 * 
 * <pre>
 * dst[x][y] = 0;
 * for (int i = -xOrigin; i &lt; -xOrigin + width; i++) {
 *     for (int j = -yOrigin; j &lt; -yOrigin + height; j++) {
 *         dst[x][y] += src[x + i][y + j] * kernel[xOrigin + i][yOrigin + j];
 *     }
 * }
 * </pre>
 * 
 * <p>
 * Convolution, like any neighborhood operation, leaves a band of pixels around the edges undefined. For example, for a 3x3 kernel only four kernel
 * elements and four source pixels contribute to the convolution pixel at the corners of the source image. Pixels that do not allow the full kernel to
 * be applied to the source are not included in the destination image. A "Border" operation may be used to add an appropriate border to the source
 * image in order to avoid shrinkage of the image boundaries.
 * 
 * <p>
 * The kernel may not be bigger in any dimension than the image data.
 * 
 * It should be noted that this operation automatically adds a value of <code>Boolean.TRUE</code> for the
 * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> to the given <code>configuration</code> so that the operation is performed on the pixel values
 * instead of being performed on the indices into the color map if the source(s) have an <code>IndexColorModel</code>. This addition will take place
 * only if a value for the <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> has not already been provided by the user. Note that the
 * <code>configuration</code> Map is cloned before the new hint is added to it. The operation can be smart about the value of the
 * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> <code>RenderingHints</code>, i.e. while the default value for the
 * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> is <code>Boolean.TRUE</code>, in some cases the operator could set the default.
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
 * <td>Convolve</td>
 * </tr>
 * <tr>
 * <td>LocalName</td>
 * <td>Convolve</td>
 * </tr>
 * <tr>
 * <td>Vendor</td>
 * <td>it.geosolutions.jaiext</td>
 * </tr>
 * <tr>
 * <td>Description</td>
 * <td>Convolves the source image with the input kernel.</td>
 * </tr>
 * <tr>
 * <td>DocURL</td>
 * <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/ConvolveDescriptor.html</td>
 * </tr>
 * <tr>
 * <td>Version</td>
 * <td>1.0</td>
 * </tr>
 * <tr>
 * <td>arg0Desc</td>
 * <td>Input convolution kernel.</td>
 * </tr>
 * <tr>
 * <td>arg1Desc</td>
 * <td>Optional ROI object to use in computation.</td>
 * </tr>
 * <tr>
 * <td>arg2Desc</td>
 * <td>Optional Range of NoData values to use in computation.</td>
 * </tr>
 * <tr>
 * <td>arg3Desc</td>
 * <td>Destination No Data value used when the computation cannot be performed.</td>
 * </tr>
 * <tr>
 * <td>arg4Desc</td>
 * <td>Boolean indicating if kernels with NoData must be skipped from computation.</td>
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
 * <td>kernel</td>
 * <td>javax.media.jai.KernelJAI</td>
 * <td>NO_PARAMETER_DEFAULT</td>
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
 * <td>Double</td>
 * <td>0</td>
 * <tr>
 * <td>skipNoData</td>
 * <td>Boolean</td>
 * <td>true</td>
 * </table>
 * </p>
 */
public class ConvolveDescriptor extends OperationDescriptorImpl {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    /**
     * The resource strings that provide the general documentation and specify the parameter list for a Convolve operation.
     */
    private static final String[][] resources = {
            { "GlobalName", "Convolve" },
            { "LocalName", "Convolve" },
            { "Vendor", "it.geosolutions.jaiext" },
            { "Description", JaiI18N.getString("ConvolveDescriptor0") },
            {
                    "DocURL",
                    "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/ConvolveDescriptor.html" },
            { "Version", JaiI18N.getString("DescriptorVersion") },
            { "arg0Desc", JaiI18N.getString("ConvolveDescriptor1") },
            { "arg1Desc", JaiI18N.getString("ConvolveDescriptor2") },
            { "arg2Desc", JaiI18N.getString("ConvolveDescriptor3") },
            { "arg3Desc", JaiI18N.getString("ConvolveDescriptor4") },
            { "arg5Desc", JaiI18N.getString("ConvolveDescriptor5") }};

    /** The parameter names for the Convolve operation. */
    private static final String[] paramNames = { "kernel", "roi", "nodata", "destNoData", "skipNoData" };

    /** The parameter class types for the Convolve operation. */
    private static final Class[] paramClasses = { javax.media.jai.KernelJAI.class,
            javax.media.jai.ROI.class, it.geosolutions.jaiext.range.Range.class,
            Double.class, Boolean.class
    };

    /** The parameter default values for the Convolve operation. */
    private static final Object[] paramDefaults = { NO_PARAMETER_DEFAULT, null,
        null, 0d, true};

    /** Constructor. */
    public ConvolveDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /**
     * Returns an array of <code>PropertyGenerators</code> implementing property inheritance for the "Convolve" operation.
     * 
     * @return An array of property generators.
     */
    public PropertyGenerator[] getPropertyGenerators() {
        PropertyGenerator[] pg = new PropertyGenerator[1];
        pg[0] = new AreaOpPropertyGenerator();
        return pg;
    }

    /**
     * Performs kernel-based convolution on an image.
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
     * @param kernel The convolution kernel.
     * @param roi Optional ROI to use in computation
     * @param nodata Optional nodata Range to check for NoData
     * @param destNoData Double value used for setting destination No Data value when it is not possible to 
     * calculate the convolved result
     * @param skipNoData Boolean indicating if kernels with NoData must be skipped from computation
     * @param hints The <code>RenderingHints</code> to use. May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>kernel</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0, KernelJAI kernel, 
            ROI roi, Range nodata, double destNoData, boolean skipNoData, RenderingHints hints) {
        ParameterBlockJAI pb = new ParameterBlockJAI("Convolve", RenderedRegistryMode.MODE_NAME);
        // Setting sources
        pb.setSource("source0", source0);
        // Setting params
        pb.setParameter("kernel", kernel);
        pb.setParameter("roi", roi);
        pb.setParameter("nodata", nodata);
        pb.setParameter("destNoData", destNoData);
        pb.setParameter("skipNoData", skipNoData);

        return JAI.create("Convolve", pb, hints);
    }
}
