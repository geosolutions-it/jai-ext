package it.geosolutions.jaiext.zonal;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.stats.Statistics.StatsType;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.List;
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

class ZonalStatsPropertyGenerator extends PropertyGeneratorImpl {

    /** Constructor. */
    public ZonalStatsPropertyGenerator() {
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
 * An <code>OperationDescriptor</code> describing the Zonal Statistics operation.
 * 
 * <p>
 * The ZonalStats operation takes in input a source image, an optional classifier image and a list of geometries on which the selected statistics are
 * calculated. These statistics are defined by the input {@link StatsType} array. The calculations can handle ROI or NoData. It is important to
 * remember that the classifier must be of integral data type.The possible statistics are:
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
 * <td>it.geosolutions.jaiext.roiaware</td>
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
 * <td>ROI object used.</td>
 * </tr>
 * <tr>
 * <td>arg4Desc</td>
 * <td>No Data Range used.</td>
 * </tr>
 * <tr>
 * <td>arg5Desc</td>
 * <td>Boolean checking if ROI RasterAccessor is used.</td>
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
            { "Vendor", "it.geosolutions.jaiext.roiaware" },
            { "Description",
                    "Image operator for calculating statistics on various Geometries supporting ROI and No Data" },
            { "DocURL", "Not defined" },
            { "Version", "1.0" },
            { "arg0Desc", "Classifier image" },
            { "arg1Desc",
                    "Transformation object used for mapping the Source image to the classifier" },
            { "arg2Desc", "List of all the geometries to analyze" },
            { "arg3Desc", "ROI object used" },
            { "arg4Desc", "No Data Range used" },
            { "arg5Desc", "Boolean checking if ROI RasterAccessor is used" },
            { "arg6Desc", "Array containing the indexes of the bands to calculate" },
            { "arg7Desc",
                    "Array indicating which statistical operations must be performed on all the selected bands" },
            { "arg8Desc",
                    "Array indicating the minimum bounds for complex statistics on all the selected bands" },
            { "arg9Desc",
                    "Array indicating the maximum bounds for complex statistics on all the selected bands" },
            { "arg10Desc",
                    "Array indicating the number of bins for complex statistics on all the selected bands" }

    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = { RenderedImage.class, AffineTransform.class,
            java.util.List.class, javax.media.jai.ROI.class,
            it.geosolutions.jaiext.range.Range.class, java.lang.Boolean.class, int[].class,
            it.geosolutions.jaiext.stats.Statistics.StatsType[].class, double[].class,
            double[].class, int[].class };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = { "classifier", "transform", "roilist", "ROI",
            "noData", "useRoiAccessor", "bands", "stats", "minbound", "maxbound", "numbin" };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = { null, null, null, null, null, false,
            new int[] { 0 }, null, null, null, null };

    public ZonalStatsDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /**
     * Returns an array of <code>PropertyGenerators</code> implementing property inheritance for the "Stats" operation
     * 
     * @return An array of property generators.
     */
    public PropertyGenerator[] getPropertyGenerators() {
        PropertyGenerator[] pg = new PropertyGenerator[1];
        pg[0] = new ZonalStatsPropertyGenerator();
        return pg;
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
     * @param ROI Roi object on which the calculation are performed.
     * @param NoData No Data range used for calculation.
     * @param useRoiAccessor Boolean indicating if ROI RasterAccessor must be used.
     * @param bands Array indicating which band to consider.
     * @param stats Array indicating which statistics to consider.
     * @param minBound Array indicating minimum bounds for complex computations.
     * @param maxBound Array indicating maximum bounds for complex computations.
     * @param numBins Array indicating the number of bins for complex computations.
     * @param hints The <code>RenderingHints</code> to use.
     * @return The <code>RenderedOp</code> source image.
     * @throws IllegalArgumentException if <code>source</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source, RenderedImage classifier,
            AffineTransform transform, List<ROI> roilist, ROI roi, Range noData,
            boolean useRoiAccessor, int[] bands, StatsType[] stats, double[] minBound,
            double[] maxBound, int[] numBins, RenderingHints hints) {
        // Creation of a parameterBlockJAI containing all the operation parameters
        ParameterBlockJAI pb = new ParameterBlockJAI("Zonal", RenderedRegistryMode.MODE_NAME);
        // Source image
        pb.setSource(source, 0);
        // Image parameters
        pb.setParameter("classifier", classifier);
        pb.setParameter("transform", transform);
        pb.setParameter("roilist", roilist);
        pb.setParameter("ROI", roi);
        pb.setParameter("NoData", noData);
        pb.setParameter("useRoiAccessor", useRoiAccessor);
        pb.setParameter("bands", bands);
        pb.setParameter("stats", stats);
        pb.setParameter("minbound", minBound);
        pb.setParameter("maxbound", maxBound);
        pb.setParameter("numbin", numBins);

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
     * @param ROI Roi object on which the calculation are performed.
     * @param NoData No Data range used for calculation.
     * @param useRoiAccessor Boolean indicating if ROI RasterAccessor must be used.
     * @param bands Array indicating which band to consider.
     * @param stats Array indicating which statistics to consider.
     * @return The <code>RenderedOp</code> source image.
     * @throws IllegalArgumentException if <code>source</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source, RenderedImage classifier,
            AffineTransform transform, List<ROI> roilist, ROI roi, Range noData,
            boolean useRoiAccessor, int[] bands, StatsType[] stats, RenderingHints hints) {
        return create(source, classifier, transform, roilist, roi, noData, useRoiAccessor, bands,
                stats, null, null, null, hints);
    }

}
