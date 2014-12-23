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
package it.geosolutions.jaiext.bandcombine;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
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
 * An <code>OperationDescriptor</code> describing the "BandCombine" operation.
 * 
 * <p>
 * The BandCombing operation computes a set of arbitrary linear combinations of the bands of a rendered or renderable source image, using a specified
 * matrix. The matrix must a number of rows equal to the number of desired destination bands and a number of columns equal to the number of source
 * bands plus one. In other words, the array may be constructed using the syntax:
 * 
 * <pre>
 * double[][] matrix = new double[destBands][sourceBands + 1];
 * </pre>
 * 
 * <p>
 * The number of source bands used to determine the matrix dimensions is given by <code>source.getSampleModel().getNumBands()</code> regardless of the
 * type of <code>ColorModel</code> the source has.
 * 
 * <p>
 * This descriptor also provides support for optional ROIs or nodata in the source image. The support for these features can be achieved by skipping
 * the data which are not valid during computations.
 * 
 * <p>
 * The extra column in the matrix contains constant values each of which is added to the respective band of the destination. The transformation is
 * therefore defined by the pseudocode:
 * 
 * <pre>
 * // s = source pixel
 * // d = destination pixel
 * for (int i = 0; i &lt; destBands; i++) {
 *     d[i] = matrix[i][sourceBands];
 *     for (int j = 0; j &lt; sourceBands; j++) {
 *         d[i] += matrix[i][j] * s[j];
 *     }
 * }
 * </pre>
 * 
 * <p>
 * If the result of the computation underflows/overflows the minimum/maximum value supported by the destination image, then it will be clamped to the
 * minimum/maximum value respectively.
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
 * <td>BandCombine</td>
 * </tr>
 * <tr>
 * <td>LocalName</td>
 * <td>BandCombine</td>
 * </tr>
 * <tr>
 * <td>Vendor</td>
 * <td>it.geosolutions.jaiext</td>
 * </tr>
 * <tr>
 * <td>Description</td>
 * <td>Performs arbitrary interband linear combination using a specified matrix.</td>
 * </tr>
 * <tr>
 * <td>DocURL</td>
 * <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/BandCombineDescriptor.html</td>
 * </tr>
 * <tr>
 * <td>Version</td>
 * <td>1.0</td>
 * </tr>
 * <tr>
 * <td>arg0Desc</td>
 * <td>Matrix used in computation.</td>
 * </tr>
 * <td>arg1Desc</td>
 * <td>Optional ROI object to use in computation.</td>
 * </tr>
 * <td>arg2Desc</td>
 * <td>Optional Range of NoData values to use in computation.</td>
 * </tr>
 * <td>arg3Desc</td>
 * <td>Destination no data value used when all the pixel band values are NoData.</td>
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
 * <td>matrix</td>
 * <td>double[][]</td>
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
 * <td>double</td>
 * <td>0</td>
 * </table>
 * </p>
 * 
 */
public class BandCombineDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that specify the parameter list for this operation.
     */
    private static final String[][] resources = {
            { "GlobalName", "BandCombine" },
            { "LocalName", "BandCombine" },
            { "Vendor", "it.geosolutions.jaiext" },
            { "Description", JaiI18N.getString("BandCombineDescriptor0") },
            {
                    "DocURL",
                    "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/BandCombineDescriptor.html" },
            { "Version", JaiI18N.getString("DescriptorVersion") },
            { "arg0Desc", JaiI18N.getString("BandCombineDescriptor2") },
            { "arg1Desc", JaiI18N.getString("BandCombineDescriptor3") },
            { "arg2Desc", JaiI18N.getString("BandCombineDescriptor4") },
            { "arg3Desc", JaiI18N.getString("BandCombineDescriptor5") }};

    /** The parameter class list */
    private static final Class[] paramClasses = { double[][].class, javax.media.jai.ROI.class,
            it.geosolutions.jaiext.range.Range.class, Double.class };

    /** The parameter name list */
    private static final String[] paramNames = { "matrix", "roi", "nodata", "destNoData" };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = { NO_PARAMETER_DEFAULT, null, null, 0d };

    private static final String[] supportedModes = { "rendered", "renderable" };

    /** Constructor. */
    public BandCombineDescriptor() {
        super(resources, supportedModes, 1, paramNames, paramClasses, paramDefaults, null);
    }

    /**
     * Validates the input source and parameters.
     * 
     * <p>
     * In addition to the standard checks performed by the superclass method, this method checks that "matrix" has at least 1 row and (source bands +
     * 1) columns.
     * 
     * <p>
     * The number of source bands is considered to be equal to <code>source.getSampleModel().getNumBands()</code>.
     */
    public boolean validateArguments(String modeName, ParameterBlock args, StringBuffer message) {
        if (!super.validateArguments(modeName, args, message)) {
            return false;
        }
        // Check is made only for Rendered sources
        if (!modeName.equalsIgnoreCase("rendered"))
            return true;

        RenderedImage src = args.getRenderedSource(0);
        // Check on the Matrix dimensions
        double[][] matrix = (double[][]) args.getObjectParameter(0);
        SampleModel sm = src.getSampleModel();
        int rowLength = sm.getNumBands() + 1;

        if (matrix.length < 1) {
            message.append(getName() + ": " + JaiI18N.getString("BandCombineDescriptor1"));
            return false;
        }

        for (int i = 0; i < matrix.length; i++) {
            if (matrix[i].length != rowLength) {
                message.append(getName() + ": " + JaiI18N.getString("BandCombineDescriptor1"));
                return false;
            }
        }

        return true;
    }

    /**
     * Performs arbitrary interband linear combination using a specified matrix and checking ROI and NoData if defined.
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
     * @param matrix The matrix specifying the band combination.
     * @param roi Optional ROI object used in computation
     * @param nodata Optional range object used for checking the presence of nodata
     * @param destinationNoData value for replacing the source nodata values
     * @param hints The <code>RenderingHints</code> to use. May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     */
    public static RenderedOp create(RenderedImage source0, double[][] matrix, ROI roi,
            Range nodata, double destinationNoData, RenderingHints hints) {
        ParameterBlockJAI pb = new ParameterBlockJAI("BandCombine", RenderedRegistryMode.MODE_NAME);
        // Setting source
        pb.setSource("source0", source0);
        // Setting parameters
        pb.setParameter("matrix", matrix);
        pb.setParameter("roi", roi);
        pb.setParameter("nodata", nodata);
        pb.setParameter("destNoData", destinationNoData);
        
        return JAI.create("BandCombine", pb, hints);
    }

    /**
     * Performs arbitrary interband linear combination using a specified matrix and checking ROI and NoData if defined.
     * 
     * <p>
     * Creates a <code>ParameterBlockJAI</code> from all supplied arguments except <code>hints</code> and invokes
     * {@link JAI#createRenderable(String,ParameterBlock,RenderingHints)}.
     * 
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderableOp
     * 
     * @param source0 <code>RenderableImage</code> source 0.
     * @param matrix The matrix specifying the band combination.
     * @param roi Optional ROI object used in computation
     * @param nodata Optional range object used for checking the presence of nodata
     * @param destinationNoData value for replacing the source nodata values
     * @param hints The <code>RenderingHints</code> to use. May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     */
    public static RenderableOp createRenderable(RenderableImage source0, double[][] matrix,
            ROI roi, Range nodata, double destinationNoData, RenderingHints hints) {
        ParameterBlockJAI pb = new ParameterBlockJAI("BandCombine",
                RenderableRegistryMode.MODE_NAME);
        // Setting source
        pb.setSource("source0", source0);
        // Setting parameters
        pb.setParameter("matrix", matrix);
        pb.setParameter("roi", roi);
        pb.setParameter("nodata", nodata);
        pb.setParameter("destNoData", destinationNoData);

        return JAI.createRenderable("BandCombine", pb, hints);
    }
}
