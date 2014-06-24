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

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.List;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import org.jaitools.media.jai.zonalstats.ZonalStats;
import org.jaitools.numeric.Range.Type;
import org.jaitools.numeric.Statistic;
import org.junit.BeforeClass;
import org.junit.Test;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.stats.Statistics.StatsType;
import it.geosolutions.jaiext.testclasses.TestBase;

/**
 * This test class is used for compare the timing between the new ZonalStats operation and the its old JaiTools version. NoData range can be used by
 * setting to true the JAI.Ext.RangeUsed JVM boolean parameters. If the user wants to change the number of the benchmark cycles or of the not
 * benchmark cycles, should only pass the new values to the JAI.Ext.BenchmarkCycles or JAI.Ext.NotBenchmarkCycles parameters.If the user want to use
 * the JaiTools ZonalStats operation must pass to the JVM the JAI.Ext.OldDescriptor parameter set to true. For selecting a specific data type the user
 * must set the JAI.Ext.TestSelector JVM integer parameter to a number between 0 and 5 (where 0 means byte, 1 Ushort, 2 Short, 3 Integer, 4 Float and
 * 5 Double). The test is made on a list of 10 geometries. The statistics calculated are:
 * <ul>
 * <li>Mean</li>
 * <li>Sum</li>
 * <li>Max</li>
 * <li>Min</li>
 * <li>Extrema</li>
 * <li>Variance</li>
 * <li>Standard Deviation</li>
 * <li>Median</li>
 * </ul>
 * The user can choose if the classifier must be used by setting to true the JVM parameter JAI.Ext.Classifier.
 * 
 */
public class ComparisonTest extends TestBase {

    /** Number of benchmark iterations (Default 1) */
    private final static Integer BENCHMARK_ITERATION = Integer.getInteger(
            "JAI.Ext.BenchmarkCycles", 1);

    /** Number of not benchmark iterations (Default 0) */
    private final static int NOT_BENCHMARK_ITERATION = Integer.getInteger(
            "JAI.Ext.NotBenchmarkCycles", 0);

    /** Boolean indicating if the old descriptor must be used */
    private final static boolean OLD_DESCRIPTOR = Boolean.getBoolean("JAI.Ext.OldDescriptor");

    /** Boolean indicating if a No Data Range must be used */
    private final static boolean RANGE_USED = Boolean.getBoolean("JAI.Ext.RangeUsed");

    /** Boolean indicating if a classifier image must be used */
    private static final boolean CLASSIFIER = Boolean.getBoolean("JAI.Ext.Classifier");

    /** Source test image */
    private static RenderedImage testImage;

    /** No Data Range for Byte */
    private static Range rangeNDByte;

    /** No Data Range for UShort */
    private static Range rangeNDUSHort;

    /** No Data Range for Short */
    private static Range rangeNDShort;

    /** No Data Range for Integer */
    private static Range rangeNDInteger;

    /** No Data Range for Float */
    private static Range rangeNDFloat;

    /** No Data Range for Double */
    private static Range rangeNDDouble;

    /** Array indicating the statistics to calculate */
    private static StatsType[] arrayStats;

    /** Array with band indexes */
    private static int[] bands;

    /** Array with band indexes (Integer format) */
    private static Integer[] bandsInteger;

    /** Array indicating the number of bins for each band */
    private static int[] numBins;

    /** Array indicating the maximum bounds for each band */
    private static double[] maxBounds;

    /** Array indicating the minimum bounds for each band */
    private static double[] minBounds;

    /** Classifier image */
    private static RenderedImage classifier;

    /** List of all the input geometries */
    private static List<ROI> roilist;

    /** List of all the NoData Ranges (Old ZonalStatsDescriptor) */
    private static List<org.jaitools.numeric.Range<Double>> noDataRanges;

    /** Array indicating the statistics to calculate (Old ZonalStatsDescriptor) */
    private static Statistic[] statsUsed;

    // Initial static method for preparing all the test data
    @BeforeClass
    public static void initialSetup() {
        // Setting of the image filler parameter to true for a better image creation
        IMAGE_FILLER = true;
        // Images initialization values
        byte noDataB = 100;
        short noDataUS = 100;
        short noDataS = 100;
        int noDataI = 100;
        float noDataF = 100;
        double noDataD = 100;
        // Image creations
        switch (TEST_SELECTOR) {
        case DataBuffer.TYPE_BYTE:
            testImage = createTestImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                    noDataB, false);
            break;
        case DataBuffer.TYPE_USHORT:
            testImage = createTestImage(DataBuffer.TYPE_USHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                    noDataUS, false);
            break;
        case DataBuffer.TYPE_SHORT:
            testImage = createTestImage(DataBuffer.TYPE_SHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                    noDataS, false);
            break;
        case DataBuffer.TYPE_INT:
            testImage = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                    noDataI, false);
            break;
        case DataBuffer.TYPE_FLOAT:
            testImage = createTestImage(DataBuffer.TYPE_FLOAT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                    noDataF, false);
            break;
        case DataBuffer.TYPE_DOUBLE:
            testImage = createTestImage(DataBuffer.TYPE_DOUBLE, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                    noDataD, false);
            break;
        default:
            throw new IllegalArgumentException("Wrong data type");
        }
        // Image filler must be reset
        IMAGE_FILLER = false;

        // Classifier creation
        if (CLASSIFIER) {
            classifier = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH, DEFAULT_HEIGHT, 2,
                    false);
        } else {
            classifier = null;
        }

        // Range creation if selected
        if (RANGE_USED) {
            switch (TEST_SELECTOR) {
            case DataBuffer.TYPE_BYTE:
                rangeNDByte = RangeFactory.create(noDataB, true, noDataB, true);
                break;
            case DataBuffer.TYPE_USHORT:
                rangeNDUSHort = RangeFactory.createU(noDataUS, true, noDataUS, true);
                break;
            case DataBuffer.TYPE_SHORT:
                rangeNDShort = RangeFactory.create(noDataS, true, noDataS, true);
                break;
            case DataBuffer.TYPE_INT:
                rangeNDInteger = RangeFactory.create(noDataI, true, noDataI, true);
                break;
            case DataBuffer.TYPE_FLOAT:
                rangeNDFloat = RangeFactory.create(noDataF, true, noDataF, true, true);
                break;
            case DataBuffer.TYPE_DOUBLE:
                rangeNDDouble = RangeFactory.create(noDataD, true, noDataD, true, true);
                break;
            default:
                throw new IllegalArgumentException("Wrong data type");
            }

            // RangeList for old descriptor
            noDataRanges = new ArrayList<org.jaitools.numeric.Range<Double>>();
            noDataRanges.add(new org.jaitools.numeric.Range<Double>(noDataD, true, noDataD, true));

        }

        // Statistic types definition for new descriptor
        arrayStats = new StatsType[] { StatsType.MEAN, StatsType.SUM, StatsType.MAX, StatsType.MIN,
                StatsType.EXTREMA, StatsType.VARIANCE, StatsType.DEV_STD, StatsType.MEDIAN };
        // Statistic types definition for old descriptor
        statsUsed = new Statistic[] { Statistic.MEAN, Statistic.SUM, Statistic.MAX, Statistic.MIN,
                Statistic.RANGE, Statistic.VARIANCE, Statistic.SDEV, Statistic.MEDIAN };

        // Band definition
        bands = new int[] { 0, 1 };
        bandsInteger = new Integer[] { 0, 1 };

        // Histogram variables definition
        numBins = new int[] { 5 };
        maxBounds = new double[] { 3 };
        minBounds = new double[] { -3 };

        int initial_value = 10;
        int final_value = 30;

        int dimension = (final_value - initial_value);

        int interval = dimension / 10;

        // ROI LIST
        // roi list creation
        roilist = new ArrayList<ROI>();
        // addition of the various geometries
        for (int i = 0; i < 10; i++) {
            ROI roiGeom = new ROIShape(new Rectangle(initial_value, final_value, dimension,
                    dimension));
            roilist.add(roiGeom);
            // Translation of the geometry (with overlap)
            initial_value += interval;
            final_value += interval;
        }
    }

    // General method for showing calculation time of the 2 ZonalStats operators
    @Test
    public void testZonalStatsDescriptor() {
        // Range object (New Descriptor)
        Range rangeND = null;
        // Image data types
        int dataType = TEST_SELECTOR;

        // Descriptor string
        String description = "\n ";
        String propertyName = "";
        // String for final output
        String stat = "ZonalStats";

        // Control if the acceleration should be used for the old descriptor
        if (OLD_DESCRIPTOR) {
            propertyName += org.jaitools.media.jai.zonalstats.ZonalStatsDescriptor.ZONAL_STATS_PROPERTY;
            description = "Old " + stat;
            // Control if the Range should be used for the new descriptor
        } else {
            propertyName += ZonalStatsDescriptor.ZS_PROPERTY;
            description = "New " + stat;
        }
        // Range creation if requested
        if (RANGE_USED) {
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                rangeND = rangeNDByte;
                break;
            case DataBuffer.TYPE_USHORT:
                rangeND = rangeNDUSHort;
                break;
            case DataBuffer.TYPE_SHORT:
                rangeND = rangeNDShort;
                break;
            case DataBuffer.TYPE_INT:
                rangeND = rangeNDInteger;
                break;
            case DataBuffer.TYPE_FLOAT:
                rangeND = rangeNDFloat;
                break;
            case DataBuffer.TYPE_DOUBLE:
                rangeND = rangeNDDouble;
                break;
            default:
                throw new IllegalArgumentException("Wrong data type");
            }
        }

        // Data type string
        String dataTypeString = "";

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            dataTypeString += "Byte";
            break;
        case DataBuffer.TYPE_USHORT:
            dataTypeString += "UShort";
            break;
        case DataBuffer.TYPE_SHORT:
            dataTypeString += "Short";
            break;
        case DataBuffer.TYPE_INT:
            dataTypeString += "Integer";
            break;
        case DataBuffer.TYPE_FLOAT:
            dataTypeString += "Float";
            break;
        case DataBuffer.TYPE_DOUBLE:
            dataTypeString += "Double";
            break;
        default:
            throw new IllegalArgumentException("Wrong data type");
        }

        // Total cycles number
        int totalCycles = BENCHMARK_ITERATION + NOT_BENCHMARK_ITERATION;
        // PlanarImage
        PlanarImage imageStats = null;
        // Initialization of the statistics
        long mean = 0;
        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;

        // Cycle for calculating the mean, maximum and minimum calculation time
        for (int i = 0; i < totalCycles; i++) {
            // ZonalStats list creation (1 for every geometry)
            List<ZonalStats> listZs = new ArrayList<ZonalStats>();
            // ZonalStatsOpImage creation (1 for every geometry)
            List<RenderedImage> listIMG = new ArrayList<RenderedImage>();

            // creation of the image with the selected descriptor

            if (OLD_DESCRIPTOR) {
                for (ROI roiGeom : roilist) {
                    // Old descriptor calculations
                    PlanarImage geomIMG = new org.jaitools.media.jai.zonalstats.ZonalStatsOpImage(
                            testImage, classifier, null, null, statsUsed, bandsInteger, roiGeom,
                            null, null, Type.EXCLUDE, false, noDataRanges);
                    listIMG.add(geomIMG);
                }
            } else {
                // New descriptor calculations
                imageStats = ZonalStatsDescriptor.create(testImage, classifier, null, roilist,
                        rangeND,null, false, bands, arrayStats, minBounds, maxBounds, numBins, null, false, null);
            }

            // Total statistic calculation time
            long start;
            long end;
            if (OLD_DESCRIPTOR) {
                start = System.nanoTime();
                for (RenderedImage image : listIMG) {
                    ZonalStats zs = (ZonalStats) image.getProperty(propertyName);
                }
                end = System.nanoTime() - start;

            } else {
                start = System.nanoTime();
                imageStats.getProperty(propertyName);
                end = System.nanoTime() - start;
            }

            // If the the first NOT_BENCHMARK_ITERATION cycles has been done, then the mean, maximum and minimum values are stored
            if (i > NOT_BENCHMARK_ITERATION - 1) {
                if (i == NOT_BENCHMARK_ITERATION) {
                    mean = end;
                } else {
                    mean = mean + end;
                }

                if (end > max) {
                    max = end;
                }

                if (end < min) {
                    min = end;
                }
            }
            // For every cycle the cache is flushed such that all the tiles must be recalculated
            JAI.getDefaultInstance().getTileCache().flush();
        }
        // Mean values
        double meanValue = mean / BENCHMARK_ITERATION * 1E-6;

        // Max and Min values stored as double
        double maxD = max * 1E-6;
        double minD = min * 1E-6;
        // Comparison between the mean times
        System.out.println(dataTypeString);
        // Output print
        System.out.println("\nMean value for " + description + "Descriptor : " + meanValue
                + " msec.");
        System.out.println("Maximum value for " + description + "Descriptor : " + maxD + " msec.");
        System.out.println("Minimum value for " + description + "Descriptor : " + minD + " msec.");
        // Final Image disposal
        if (imageStats instanceof RenderedOp) {
            ((RenderedOp) imageStats).dispose();
        }

    }
}
