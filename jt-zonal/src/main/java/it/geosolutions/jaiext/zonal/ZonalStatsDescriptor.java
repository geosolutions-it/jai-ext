package it.geosolutions.jaiext.zonal;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.stats.Statistics.StatsType;

import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.util.List;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;

public class ZonalStatsDescriptor extends OperationDescriptorImpl {

    /** Statistics property name */
    public final static String ZONAL_STATS_PROPERTY = "JAI-EXT.zonalstats";

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

    public static RenderedOp create(RenderedImage source, RenderedImage classifier,
            AffineTransform transform, List<ROI> roilist, ROI roi, Range noData,
            boolean useRoiAccessor, int[] bands, StatsType[] stats, RenderingHints hints) {
        return create(source, classifier, transform, roilist, roi, noData, useRoiAccessor, bands, stats,
                null, null, null, hints);
    }

}
