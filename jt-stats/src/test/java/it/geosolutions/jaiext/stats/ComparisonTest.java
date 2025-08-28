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
import it.geosolutions.jaiext.testclasses.TestBase;

import java.awt.image.RenderedImage;

import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test class is used for compare the timing between the new StatisticalDescriptor operation and the old JAI version. Roi or NoData range can be
 * used by setting to true JAI.Ext.ROIUsed or JAI.Ext.RangeUsed JVM boolean parameters are set to true. If the user wants to change the number of the
 * benchmark cycles or of the not benchmark cycles, should only pass the new values to the JAI.Ext.BenchmarkCycles or JAI.Ext.NotBenchmarkCycles
 * parameters.If the user want to use the old descriptor must pass to the JVM the JAI.Ext.OldDescriptor parameter set to true. For selecting a
 * specific data type the user must set the JAI.Ext.TestSelector JVM integer parameter to a number between 0 and 5 (where 0 means byte, 1 Ushort, 2
 * Short, 3 Integer, 4 Float and 5 Double). The possible statistics to calculate can be chosen by setting the JVM Integer parameter JAI.Ext.Statistic
 * to the associated value:
 * <ul>
 * <li>Mean 0</li>
 * <li>Extrema 1</li>
 * <li>Histogram 2</li>
 * </ul>
 */

public class ComparisonTest extends TestBase {

    /**
     * Horizontal subsampling parameter
     */
    private static int xPeriod;

    /**
     * Vertical subsampling parameter
     */
    private static int yPeriod;

    /**
     * Array with band indexes
     */
    private static int[] bands;

    /**
     * Array indicating the number of bins for each band
     */
    private static int[] numBins;

    /**
     * Array indicating the maximum bounds for each band
     */
    private static double[] maxBounds;

    /**
     * Array indicating the minimum bounds for each band
     */
    private static double[] minBounds;

    // Initial static method for preparing all the test data
    @BeforeClass
    public static void initialSetup() {
        // Band definition
        bands = new int[]{0, 1, 2};

        // Period Definitions
        xPeriod = 1;
        yPeriod = 1;

        // Histogram variables definition
        numBins = new int[]{5};
        maxBounds = new double[]{3};
        minBounds = new double[]{-3};
    }


    @Test
    public void testDataTypesWithRoi() {
        if (!OLD_DESCRIPTOR)
            testAllTypes(TestRoiNoDataType.ROI);
    }

    @Test
    public void testDataTypesWithNoData() {
        if (!OLD_DESCRIPTOR)
            testAllTypes(TestRoiNoDataType.NODATA);
    }

    @Test
    public void testDataTypesWithBoth() {
        if (!OLD_DESCRIPTOR)
            testAllTypes(TestRoiNoDataType.BOTH);
    }

    @Override
    public void testOperation(int dataType, TestRoiNoDataType testType) {
        Range range = getRange(dataType, testType);
        ROI roi = getROI(testType);
        RenderedImage testImage = createDefaultTestImage(dataType, 1, true);
        int[] numBinsTest = null;
        double[] maxBoundsTest = null;
        double[] minBoundsTest = null;
        StatsType[] arrayStats = null;

        String stat = "";
        for (int statistic = 0; statistic < 3; statistic++) {
            if (statistic == 0) {
                stat += "Mean";
                arrayStats = new StatsType[]{StatsType.MEAN};
            } else if (statistic == 1) {
                stat += "Extrema";
                arrayStats = new StatsType[]{StatsType.EXTREMA};
            } else if (statistic == 2) {
                stat += "Histogram";
                arrayStats = new StatsType[]{StatsType.HISTOGRAM};
                numBinsTest = numBins;
                maxBoundsTest = maxBounds;
                minBoundsTest = minBounds;
            }

            // Image dataType
            PlanarImage image = null;

            // creation of the image with the selected descriptor

            if (OLD_DESCRIPTOR) {
                if (statistic == 0) {
                    image = javax.media.jai.operator.MeanDescriptor.create(testImage, roi,
                            xPeriod, yPeriod, null);
                } else if (statistic == 1) {
                    image = javax.media.jai.operator.ExtremaDescriptor.create(testImage, roi,
                            xPeriod, yPeriod, false, 1, null);
                } else if (statistic == 2) {
                    image = javax.media.jai.operator.HistogramDescriptor.create(testImage,
                            roi, xPeriod, yPeriod, numBinsTest, minBoundsTest, maxBoundsTest, null);
                }
            } else {
                image = StatisticsDescriptor.create(testImage, xPeriod, yPeriod, roi, range,
                        false, bands, arrayStats, minBoundsTest, maxBoundsTest, numBinsTest, null);
            }

            finalizeTest(getSuffix(testType, stat), dataType, image);
        }

    }
}
