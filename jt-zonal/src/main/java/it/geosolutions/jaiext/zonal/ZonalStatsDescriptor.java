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
package it.geosolutions.jaiext.zonal;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.stats.Statistics.StatsType;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.List;
import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the Zonal Statistics operation.
 * 
 * <p>
 * The ZonalStats operation takes in input a source image, an optional classifier image and a list of geometries on which the selected statistics are
 * calculated. These statistics are defined by the input {@link StatsType} array. The calculations can NoData. It is important to remember that the
 * classifier must be of integral data type.The possible statistics are:
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
 * It is important to remember that if the Median, Mode or Histogram operations must be executed, even their Bounds and Bin numbers must be defined.
 * The source can have all the possible JAI accepted data types. The statistical calculations are performed on every tile with an adequate
 * synchronization and stored inside an instance of the {@link ZoneGeometry} class. For avoiding concurrency issues, the statistic calculation is done
 * in a synchronized block. The statistical results are returned by calling the getProperty() method. The calculation happens only the first time for
 * avoiding unnecessary loss of time.
 * </p>
 * 
 * <p>
 * The results are returned by the getProperty() method as a List<ZoneGeometry>. Every item contains the results for the related geometry. These
 * results are stored as a Map<Integer, Map<Integer, Statistics[]>. The inner map contains the results for every zone indicated by the optional
 * classifier object (if the classifier is not present, the statistics are stored inside the 0-key item). The outer map contains the results for every
 * band. The user must only select the band, the zone(if present) and the index associated to the desired Statistics object and then call the
 * getResult() method for having the result.
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
 * <td>Zonal</td>
 * </tr>
 * <tr>
 * <td>LocalName</td>
 * <td>Zonal</td>
 * </tr>
 * <tr>
 * <td>Vendor</td>
 * <td>it.geosolutions.jaiext</td>
 * </tr>
 * <tr>
 * <td>Description</td>
 * <td>Image operator for calculating statistics on different image zones supporting ROI and No Data.</td>
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
 * <td>Classifier image.</td>
 * </tr>
 * <tr>
 * <td>arg1Desc</td>
 * <td>Transformation object used for mapping the Source image to the classifier.</td>
 * </tr>
 * <tr>
 * <td>arg2Desc</td>
 * <td>List of all the geometries to analyze.</td>
 * </tr>
 * <tr>
 * <td>arg3Desc</td>
 * <td>No Data Range used.</td>
 * </tr>
 * <tr>
 * <td>arg4Desc</td>
 * <td>ROI Object used.</td>
 * </tr>
 * <tr>
 * <td>arg5Desc</td>
 * <td>Boolean indicatin if the ROI RasterAccessor should be used during computations.</td>
 * </tr>
 * <tr>
 * <td>arg6Desc</td>
 * <td>Array containing the indexes of the bands to calculate.</td>
 * </tr>
 * <tr>
 * <td>arg7Desc</td>
 * <td>Array indicating which statistical operations must be performed on all the selected bands.</td>
 * </tr>
 * <tr>
 * <td>arg8Desc</td>
 * <td>Array indicating the minimum bounds for complex statistics on all the selected bands.</td>
 * </tr>
 * <tr>
 * <td>arg9Desc</td>
 * <td>Array indicating the maximum bounds for complex statistics on all the selected bands.</td>
 * </tr>
 * <tr>
 * <td>arg10Desc</td>
 * <td>Array indicating the number of bins for complex statistics on all the selected bands.</td>
 * </tr>
 * <tr>
 * <td>arg11Desc</td>
 * <td>List of the possible ranges for dividing the statistics.</td>
 * </tr>
 * <tr>
 * <td>arg12Desc</td>
 * <td>Boolean indicating if the results must be calculated for each range.</td>
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
 * <td>classifier</td>
 * <td>RenderedImage</td>
 * <td>null</td>
 * <tr>
 * <td>transform</td>
 * <td>AffineTransform</td>
 * <td>null</td>
 * <tr>
 * <td>roilist</td>
 * <td>List</td>
 * <td>null</td>
 * <tr>
 * <td>noData</td>
 * <td>it.geosolutions.jaiext.range.Range</td>
 * <td>null</td>
 * <tr>
 * <td>mask</td>
 * <td>javax.media.jai.ROI</td>
 * <td>null</td>
 * <tr>
 * <td>useROIAccessor</td>
 * <td>Boolean</td>
 * <td>false</td>
 * <tr>
 * <td>bands</td>
 * <td>int[]</td>
 * <td>null</td>
 * <tr>
 * <td>stats</td>
 * <td>it.geosolutions.jaiext.stats.Statistics.StatsType[]</td>
 * <td>null</td>
 * <tr>
 * <td>minbound</td>
 * <td>double[]</td>
 * <td>null</td>
 * <tr>
 * <td>maxbound</td>
 * <td>double[]</td>
 * <td>null</td>
 * <tr>
 * <td>numbin</td>
 * <td>int[]</td>
 * <td>null</td>
 * <tr>
 * <td>rangeData</td>
 * <td>List</td>
 * <td>null</td>
 * <tr>
 * <td>localStats</td>
 * <td>Boolean</td>
 * <td>false</td>
 * </table>
 * </p>
 * 
 */
public class ZonalStatsDescriptor extends OperationDescriptorImpl {

    /** Zonal Statistics property name */
    public final static String ZS_PROPERTY = "JAI-EXT.zonalstats";

    /**
     * The resource strings that provide the general documentation and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
            { "GlobalName", "Zonal" },
            { "LocalName", "Zonal" },
            { "Vendor", "it.geosolutions.jaiext" },
            { "Description",
                    "Image operator for calculating statistics on various Geometries supporting ROI and No Data" },
            { "DocURL", "Not defined" },
            { "Version", "1.0" },
            { "arg0Desc", "Classifier image" },
            { "arg1Desc",
                    "Transformation object used for mapping the Source image to the classifier" },
            { "arg2Desc", "List of all the geometries to analyze" },
            { "arg3Desc", "No Data Range used" },
            { "arg4Desc", "ROI Object used" },
            { "arg5Desc",
                    "Boolean indicatin if the ROI RasterAccessor should be used during computations" },
            { "arg6Desc", "Array containing the indexes of the bands to calculate" },
            { "arg7Desc",
                    "Array indicating which statistical operations must be performed on all the selected bands" },
            { "arg8Desc",
                    "Array indicating the minimum bounds for complex statistics on all the selected bands" },
            { "arg9Desc",
                    "Array indicating the maximum bounds for complex statistics on all the selected bands" },
            { "arg10Desc",
                    "Array indicating the number of bins for complex statistics on all the selected bands" },
            { "arg11Desc", "List of the possible ranges for dividing the statistics" },
            { "arg12Desc", "Boolean indicating if the results must be calculated for each range" }

    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = { RenderedImage.class, AffineTransform.class,
            java.util.List.class, it.geosolutions.jaiext.range.Range.class,
            javax.media.jai.ROI.class, Boolean.class, int[].class,
            it.geosolutions.jaiext.stats.Statistics.StatsType[].class, double[].class,
            double[].class, int[].class, java.util.List.class, Boolean.class };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = { "classifier", "transform", "roilist", "noData",
            "mask", "useROIAccessor", "bands", "stats", "minbound", "maxbound", "numbin",
            "rangeData", "localStats" };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = { null, null, null, null, null, false,
            new int[] { 0 }, null, null, null, null, null, false };

    public ZonalStatsDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /**
     * Performs statistical operations on different image zones defined by the input geometry list.
     * 
     * <p>
     * Creates a <code>ParameterBlockJAI</code> from all supplied arguments except <code>hints</code> and invokes
     * {@link JAI#create(String,ParameterBlock,RenderingHints)}.
     * 
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderedOp
     * 
     * @param source <code>RenderedImage</code> source image.
     * @param classifier <code>RenderedImage</code> optional classifier image(Integral dataType).
     * @param transform affine transformation used for mapping source image on the classifier.
     * @param roilist list of all the geometries.
     * @param NoData No Data range used for calculation.
     * @param mask optional mask for reducing the computations on a selected ROI.
     * @param useROIAccessor boolean indicating if a ROI RasterAccessor should be used during computations with the mask.
     * @param bands Array indicating which band to consider.
     * @param stats Array indicating which statistics to consider.
     * @param minBound Array indicating minimum bounds for complex computations.
     * @param maxBound Array indicating maximum bounds for complex computations.
     * @param numBins Array indicating the number of bins for complex computations.
     * @param rangeData List of the possible range to calculate the statistics.
     * @param localStats Boolean indicating if the statistics must be stored for each range.
     * @param hints The <code>RenderingHints</code> to use.
     * @return The <code>RenderedOp</code> source image.
     * @throws IllegalArgumentException if <code>source</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source, RenderedImage classifier,
            AffineTransform transform, List<ROI> roilist, Range noData, ROI mask,
            boolean useROIAccessor, int[] bands, StatsType[] stats, double[] minBound,
            double[] maxBound, int[] numBins, List<Range> rangeData, boolean localStats,
            RenderingHints hints) {
        // Creation of a parameterBlockJAI containing all the operation parameters
        ParameterBlockJAI pb = new ParameterBlockJAI("Zonal", RenderedRegistryMode.MODE_NAME);
        // Source image
        pb.setSource(source, 0);
        // Image parameters
        pb.setParameter("classifier", classifier);
        pb.setParameter("transform", transform);
        pb.setParameter("roilist", roilist);
        pb.setParameter("NoData", noData);
        pb.setParameter("mask", mask);
        pb.setParameter("useROIAccessor", useROIAccessor);
        pb.setParameter("bands", bands);
        pb.setParameter("stats", stats);
        pb.setParameter("minbound", minBound);
        pb.setParameter("maxbound", maxBound);
        pb.setParameter("numbin", numBins);
        pb.setParameter("rangeData", rangeData);
        pb.setParameter("localStats", localStats);

        // RenderedImage creation
        return JAI.create("Zonal", pb, hints);
    }

    /**
     * Performs statistical operations on different image zones defined by the input geometry list.
     * 
     * <p>
     * Creates a <code>ParameterBlockJAI</code> from all supplied arguments except <code>hints</code> and invokes
     * {@link JAI#create(String,ParameterBlock,RenderingHints)}.
     * 
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderedOp
     * 
     * @param source <code>RenderedImage</code> source image.
     * @param classifier <code>RenderedImage</code> optional classifier image(Integral dataType).
     * @param transform affine transformation used for mapping source image on the classifier.
     * @param roilist list of all the geometries.
     * @param NoData No Data range used for calculation.
     * @param mask optional mask for reducing the computations on a selected ROI.
     * @param useROIAccessor boolean indicating if a ROI RasterAccessor should be used during computations with the mask.
     * @param bands Array indicating which band to consider.
     * @param stats Array indicating which statistics to consider.
     * @param rangeData List of the possible range to calculate the statistics.
     * @param localStats Boolean indicating if the statistics must be stored for each range.
     * @return The <code>RenderedOp</code> source image.
     * @throws IllegalArgumentException if <code>source</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source, RenderedImage classifier,
            AffineTransform transform, List<ROI> roilist, Range noData, ROI mask,
            boolean useROIAccessor, int[] bands, StatsType[] stats, List<Range> rangeData,
            boolean localStats, RenderingHints hints) {
        return create(source, classifier, transform, roilist, noData, mask, useROIAccessor, bands,
                stats, null, null, null, rangeData, localStats, hints);
    }

}
