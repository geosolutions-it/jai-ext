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
package it.geosolutions.jaiext.stats;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.stats.Statistics.StatsType;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PropertyGenerator;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;

import com.sun.media.jai.util.PropertyGeneratorImpl;

/**
 * This class is used for retrieving an eventual ROI object passed to the source image by calling the getProperty() method. This method checks if the
 * ROI is present and if so, its bounds are intersected with the source images bounds, and then passed as a result. If no property was found an
 * Undefined Property object is returned.
 */

class StatisticsPropertyGenerator extends PropertyGeneratorImpl {

    /** Constructor. */
    public StatisticsPropertyGenerator() {
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
 * An <code>OperationDescriptor</code> describing various "Statistical" operation supporting ROI and NoData.
 * 
 * <p>
 * The SimpleStats operation takes in input a source image and an array indicating which bands to analyze and which kind of statistics can be done.
 * More statistics can be computed inside the same image. The possible statistics are:
 * </p>
 * 
 * <p>
 * <ul>
 * <li>Mean</li>
 * <li>Sum</li>
 * <li>Maximum</li>
 * <li>Minimum</li>
 * <li>Extrema</li>
 * <li>Variance</li>
 * <li>Standard Deviation</li>
 * <li>Histogram</li>
 * <li>Mode</li>
 * <li>Median</li>
 * </ul>
 * </p>
 * 
 * <p>
 * The source can have all the possible JAI accepted data types. The statistical calculations are performed on every tile and stored inside an object
 * which is a subclass of the "Statistics" class. For avoiding concurrency issues various techniques are used: for simple statistics, which does not
 * request an array for storing the values, local statistics are calculated and then accumulated in a synchronized block; for complex statistics,
 * other techniques are used. The statistical results are returned by calling the getProperty() method. The statistics are calculated only the first
 * time for avoiding unnecessary calculations. With this setup an advantage is taken by using the internal JAI MultiThreading.
 * </p>
 * 
 * <p>
 * When the results are returned by the getProperty() method as a 2-D array, the user must only select the first index related to one band and the
 * second index related to a specific computation, as defined at the image creation, and then calling the getResult() method.
 * </p>
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
 * <td>Stats</td>
 * </tr>
 * <tr>
 * <td>LocalName</td>
 * <td>Stats</td>
 * </tr>
 * <tr>
 * <td>Vendor</td>
 * <td>it.geosolutions.jaiext</td>
 * </tr>
 * <tr>
 * <td>Description</td>
 * <td>Image operator for calculating simple image statistics like mean supporting ROI and No Data.</td>
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
 * <td>Horizontal subsampling parameter.</td>
 * </tr>
 * <tr>
 * <td>arg1Desc</td>
 * <td>Vertical subsampling parameter.</td>
 * </tr>
 * <tr>
 * <td>arg2Desc</td>
 * <td>ROI object used.</td>
 * </tr>
 * <tr>
 * <td>arg3Desc</td>
 * <td>No Data Range used.</td>
 * </tr>
 * <tr>
 * <td>arg4Desc</td>
 * <td>Boolean checking if ROI RasterAccessor is used.</td>
 * </tr>
 * <tr>
 * <td>arg5Desc</td>
 * <td>Array containing the indexes of the bands to calculate.</td>
 * </tr>
 * <tr>
 * <td>arg6Desc</td>
 * <td>Array indicating which statistical operations must be performed on all the selected bands.</td>
 * </tr>
 * <td>arg7Desc</td>
 * <td>Array indicating the minimum bounds for each statistic types (if needed).</td>
 * </tr>
 * <td>arg8Desc</td>
 * <td>Array indicating the maximum bounds for each statistic types (if needed).</td>
 * </tr> 
 * <td>arg9Desc</td>
 * <td>Array indicating the number of bins for each statistic types (if needed).</td>
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
 * <td>xPeriod</td>
 * <td>Integer</td>
 * <td>1</td>
 * <tr>
 * <td>yPeriod</td>
 * <td>Integer</td>
 * <td>1</td>
 * <tr>
 * <td>ROI</td>
 * <td>ROI</td>
 * <td>null</td>
 * <tr>
 * <td>noData</td>
 * <td>it.geosolutions.jaiext.range.Range</td>
 * <td>null</td>
 * <tr>
 * <td>useRoiAccessor</td>
 * <td>Boolean</td>
 * <td>False</td>
 * <tr>
 * <td>bands</td>
 * <td>int[]</td>
 * <td>null</td>
 * <tr>
 * <td>stats</td>
 * <td>it.geosolutions.jaiext.stats.Statistics.StatsType[]</td>
 * <td>null</td>
 * <tr>
 * <td>lowValue</td>
 * <td>double[]</td>
 * <td>null</td>
 * <tr>
 * <td>highValue</td>
 * <td>double[]</td>
 * <td>null</td>
 * <tr>
 * <td>numBins</td>
 * <td>int[]</td>
 * <td>null</td>
 * <tr>
 * </table>
 * </p>
 * 
 */
public class StatisticsDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
            { "GlobalName", "Stats" },
            { "LocalName", "Stats" },
            { "Vendor", "it.geosolutions.jaiext" },
            { "Description",
                    "Image operator for calculating simple image statistics like mean supporting ROI and No Data" },
            { "DocURL", "Not defined" },
            { "Version", "1.0" },
            { "arg0Desc", "Horizontal subsampling parameter" },
            { "arg1Desc", "Vertical subsampling parameter" },
            { "arg2Desc", "ROI object used" },
            { "arg3Desc", "No Data Range used" },
            { "arg4Desc", "Boolean checking if ROI RasterAccessor is used" },
            { "arg5Desc", "Array containing the indexes of the bands to calculate" },
            { "arg6Desc",
                    "Array indicating which statistical operations must be performed on all the selected bands" },
            { "arg7Desc",
                    "Array indicating the minimum bounds for each statistic types (if needed)" },
            { "arg8Desc",
                    "Array indicating the maximum bounds for each statistic types (if needed)" },
            { "arg9Desc",
                    "Array indicating the number of bins for each statistic types (if needed)" } };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = { java.lang.Integer.class, java.lang.Integer.class,
            javax.media.jai.ROI.class, it.geosolutions.jaiext.range.Range.class,
            java.lang.Boolean.class, int[].class,
            it.geosolutions.jaiext.stats.Statistics.StatsType[].class, double[].class,
            double[].class, int[].class, };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = { "xPeriod", "yPeriod", "ROI", "noData",
            "useRoiAccessor", "bands", "stats", "lowValue", "highValue", "numBins" };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = { 1, 1, null, null, false, new int[] { 0 }, null,
        new double[] {0.0},
        new double[] {256.0},
        new int[] {256}};

    public StatisticsDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /**
     * Returns an array of <code>PropertyGenerators</code> implementing property inheritance for the "Stats" operation
     * 
     * @return An array of property generators.
     */
    public PropertyGenerator[] getPropertyGenerators() {
        PropertyGenerator[] pg = new PropertyGenerator[1];
        pg[0] = new StatisticsPropertyGenerator();
        return pg;
    }

    /**
     * Performs a statistical operation on an image defined by its "stats type" parameter.
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
     * @param xPeriod Horizontal subsampling.
     * @param yPeriod Vertical subsampling.
     * @param ROI Roi object on which the calculation are performed.
     * @param NoData No Data range used for calculation.
     * @param useRoiAccessor Boolean indicating if ROI RasterAccessor must be used.
     * @param bands Array indicating which band to consider.
     * @param stats Array indicating which statistics to consider.
     * @param minBounds Array indicating the minimum bounds for each statistic types .
     * @param maxBounds Array indicating the maximum bounds for each statistic types.
     * @param numBins Array indicating the number of bins for each statistic types.
     * @param hints The <code>RenderingHints</code> to use.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0, int xPeriod, int yPeriod, ROI roi,
            Range noData, boolean useRoiAccessor, int[] bands, StatsType[] stats,
            double[] minBounds, double[] maxBounds, int[] numBins, RenderingHints hints) {
        // Creation of a parameterBlockJAI containing all the operation parameters
        ParameterBlockJAI pb = new ParameterBlockJAI("Stats", RenderedRegistryMode.MODE_NAME);
        // Source image
        pb.setSource("source0", source0);
        // Image parameters
        pb.setParameter("xPeriod", xPeriod);
        pb.setParameter("yPeriod", yPeriod);
        pb.setParameter("ROI", roi);
        pb.setParameter("NoData", noData);
        pb.setParameter("useRoiAccessor", useRoiAccessor);
        pb.setParameter("bands", bands);
        pb.setParameter("stats", stats);
        if (minBounds != null && maxBounds != null && numBins != null) {
            pb.setParameter("lowValue", minBounds);
            pb.setParameter("highValue", maxBounds);
            pb.setParameter("numBins", numBins);
        } else if (minBounds != null || maxBounds != null || numBins != null) {
            throw new IllegalArgumentException("bounds and bins must be declared together");
        }
        // RenderedImage creation
        return JAI.create("Stats", pb, hints);
    }

    /**
     * Performs a statistical operation on an image defined by its "stats type" parameter.
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
     * @param xPeriod Horizontal subsampling.
     * @param yPeriod Vertical subsampling.
     * @param ROI Roi object on which the calculation are performed.
     * @param NoData No Data range used for calculation.
     * @param useRoiAccessor Boolean indicating if ROI RasterAccessor must be used.
     * @param bands Array indicating which band to consider.
     * @param stats Array indicating which statistics to consider.
     * @param hints The <code>RenderingHints</code> to use.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0, int xPeriod, int yPeriod, ROI roi,
            Range noData, boolean useRoiAccessor, int[] bands, StatsType[] stats,
            RenderingHints hints) {
        return create(source0, xPeriod, yPeriod, roi, noData, useRoiAccessor, bands, stats, null,
                null, null, hints);
    }

}
