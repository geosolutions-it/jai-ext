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
package it.geosolutions.jaiext.border;

import it.geosolutions.jaiext.JAIExt;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test class is used for compare the timing between the new and the old versions of the Border operator . If the user wants to change the number
 * of the benchmark cycles or of the not benchmark cycles, should only pass the new values to the JAI.Ext.BenchmarkCycles or
 * JAI.Ext.NotBenchmarkCycles parameters. The selection of the old or new descriptor must be done by setting to true or false the JVM parameter
 * JAI.Ext.OldDescriptor. No Data Range can be used by simply setting to true the JAI.Ext.RangeUsed JVM parameter. For selecting which BorderExtender 
 * to use the user must set a value from 0 to 3 to the JVM Integer parameter JAI.Ext.BorderType.
 */
public class ComparisonTest extends TestBase {

    /** Number of benchmark iterations (Default 1) */
    private final static Integer BENCHMARK_ITERATION = Integer.getInteger(
            "JAI.Ext.BenchmarkCycles", 1);

    /** Number of not benchmark iterations (Default 0) */
    private final static int NOT_BENCHMARK_ITERATION = Integer.getInteger(
            "JAI.Ext.NotBenchmarkCycles", 0);

    /** Left padding parameter */
    private static int leftPad;

    /** Right padding parameter */
    private static int rightPad;

    /** Top padding parameter */
    private static int topPad;

    /** Bottom padding parameter */
    private static int bottomPad;

    /** Output value for No Data*/
    private static double destNoData;

    @BeforeClass
    public static void initialSetup() {

        // Border dimensions setting
        leftPad = 2;
        rightPad = 2;
        topPad = 2;
        bottomPad = 2;

        // destination No Data
        destNoData = 100d;
    }
    @Test
    public void testByte() {
        testOperation(DataBuffer.TYPE_BYTE);
    }
    @Test
    public void testShort() {
        testOperation(DataBuffer.TYPE_SHORT);
    }
    @Test
    public void testUShort() {
        testOperation(DataBuffer.TYPE_USHORT);
    }
    @Test
    public void testInt() {
        testOperation(DataBuffer.TYPE_INT);
    }
    @Test
    public void testFloat() {
        testOperation(DataBuffer.TYPE_FLOAT);
    }
    @Test
    public void testDouble() {
        testOperation(DataBuffer.TYPE_DOUBLE);
    }

    public void testOperation(int dataType) {
        boolean old = OLD_DESCRIPTOR;
        for (int borderType = 0 ; borderType < 4; borderType++) {
            RenderedImage image = createDefaultTestImage(dataType, 1, true);
            Range range = getRange(dataType);
            // Border Extender used

            BorderExtender extender = BorderExtender.createInstance(borderType);
            String suffix = extender.getClass().getSimpleName()
                    .replaceFirst("^BorderExtender", "");
            suffix = suffix.substring(0,1).toUpperCase() + suffix.substring(1);
            // Descriptor string definition
            String description = "Border";

            if (old) {
                description = "Old " + description;
            } else {
                description = "New " + description;
            }

            // Data type string
            String dataTypeString = getDataTypeString(dataType);

            // Total cycles number
            int totalCycles = BENCHMARK_ITERATION + NOT_BENCHMARK_ITERATION;
            // Image
            PlanarImage imageBorder = null;

            long mean = 0;
            long max = Long.MIN_VALUE;
            long min = Long.MAX_VALUE;

            // Cycle for calculating the mean, maximum and minimum calculation time
            for (int i = 0; i < totalCycles; i++) {

                // creation of the image
                if (old) {
                    JAIExt.registerJAIDescriptor("Border");
                    imageBorder = javax.media.jai.operator.BorderDescriptor.create(image, leftPad,
                            rightPad, topPad, bottomPad, extender, null);
                } else {
                    imageBorder = BorderDescriptor.create(image, leftPad, rightPad, topPad, bottomPad,
                            extender, range, destNoData, null);
                }

                // Total calculation time
                long start = System.nanoTime();
                imageBorder.getTiles();
                long end = System.nanoTime() - start;

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
                // For every cycle the cache is flushed such that all the tiles must be recalculates
                JAI.getDefaultInstance().getTileCache().flush();
            }
            // Mean values
            double meanValue = mean / BENCHMARK_ITERATION * 1E-6;

            // Max and Min values stored as double
            double maxD = max * 1E-6;
            double minD = min * 1E-6;
            System.out.println(dataTypeString);
            // Comparison between the mean times
            // Output print of the
            System.out.println("\nMean value for " + description + "Descriptor : " + meanValue
                    + " msec.");
            System.out.println("Maximum value for " + description + "Descriptor : " + maxD + " msec.");
            System.out.println("Minimum value for " + description + "Descriptor : " + minD + " msec.");
            finalizeTest(suffix, imageBorder);
        }

    }
}
