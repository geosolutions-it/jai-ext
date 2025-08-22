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
package it.geosolutions.jaiext.nullop;

import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import org.junit.BeforeClass;
import org.junit.Test;

import it.geosolutions.jaiext.JAIExt;
import it.geosolutions.jaiext.testclasses.TestBase;

/**
 * This test class is used for compare the timing between the new Null operation and the old Jai version. If the user wants to change the number of
 * the benchmark cycles or of the not benchmark cycles, should only pass the new values to the JAI.Ext.BenchmarkCycles or JAI.Ext.NotBenchmarkCycles
 * parameters.If the user wants to use the Jai Null operation must pass to the JVM the JAI.Ext.OldDescriptor parameter set to true. For selecting a
 * specific data type the user must set the JAI.Ext.TestSelector JVM integer parameter to a number between 0 and 5 (where 0 means byte, 1 Ushort, 2
 * Short, 3 Integer, 4 Float and 5 Double).
 */
public class ComparisonTest extends TestBase {

    /** Number of benchmark iterations (Default 1) */
    private final static Integer BENCHMARK_ITERATION = Integer.getInteger(
            "JAI.Ext.BenchmarkCycles", 1);

    /** Number of not benchmark iterations (Default 0) */
    private final static int NOT_BENCHMARK_ITERATION = Integer.getInteger(
            "JAI.Ext.NotBenchmarkCycles", 0);

    // General method for showing calculation time of the 2 Null operators

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
        RenderedImage testImage = createDefaultTestImage(dataType, 1, false);


        // Descriptor string
        String description = "\n ";
        // String for final output
        String stat = "Null";

        // Control on which descriptor is used
        if (OLD_DESCRIPTOR) {
            description = "Old " + stat;
        } else {
            description = "New " + stat;
        }

        // Data type string
        String dataTypeString = getDataTypeString(dataType);

        // Total cycles number
        int totalCycles = BENCHMARK_ITERATION + NOT_BENCHMARK_ITERATION;
        // PlanarImage
        PlanarImage imageNull = null;
        // Initialization of the statistics
        long mean = 0;
        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;

        // Cycle for calculating the mean, maximum and minimum calculation time
        for (int i = 0; i < totalCycles; i++) {

            // creation of the image with the selected descriptor

            if (OLD_DESCRIPTOR) {
                JAIExt.registerJAIDescriptor("Null");
                // Old descriptor calculations
                imageNull = javax.media.jai.operator.NullDescriptor.create(testImage, null);
            } else {
                // New descriptor calculations
                imageNull = NullDescriptor.create(testImage, null);
            }

            // Total statistic calculation time
            long start;
            long end;
            start = System.nanoTime();
            imageNull.getTiles();
            end = System.nanoTime() - start;

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
        if (imageNull instanceof RenderedOp) {
            ((RenderedOp) imageNull).dispose();
        }

    }
}
