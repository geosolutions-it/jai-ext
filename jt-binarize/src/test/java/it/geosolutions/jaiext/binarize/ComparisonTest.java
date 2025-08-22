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
package it.geosolutions.jaiext.binarize;

import it.geosolutions.jaiext.JAIExt;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;

import it.geosolutions.jaiext.utilities.TestImageDumper;
import org.junit.BeforeClass;
import org.junit.Test;

public class ComparisonTest extends TestBase {

    /** Number of benchmark iterations (Default 1) */
    private final static Integer BENCHMARK_ITERATION = Integer.getInteger(
            "JAI.Ext.BenchmarkCycles", 1);

    /** Number of not benchmark iterations (Default 0) */
    private final static int NOT_BENCHMARK_ITERATION = Integer.getInteger(
            "JAI.Ext.NotBenchmarkCycles", 0);

    /** Boolean indicating if a ROI must be used */
    private final static boolean ROI_USED = Boolean.getBoolean("JAI.Ext.ROIUsed");

    /** Source band number */
    private static final int NUM_BANDS = 1;

    /** ROI Object used for testing */
    private static ROI roi;

    /** Threshold used for image binarization */
    private static double[] thresholds;

    @BeforeClass
    public static void initialSetup() {

        // Setting of the image filler parameter to true for a better image creation
        IMAGE_FILLER = true;

        // Threshold definition
        // thresholds
        thresholds = new double[6];
        thresholds[0] = 63;
        thresholds[1] = Short.MAX_VALUE / 4;
        thresholds[2] = -49;
        thresholds[3] = 105;
        thresholds[4] = (255 / 2) * 5;
        thresholds[5] = (255 / 7) * 13;


        // Image filler must be reset
        IMAGE_FILLER = false;

        // ROI creation
        if (ROI_USED) {
            Rectangle rect = new Rectangle(0, 0, DEFAULT_WIDTH / 4, DEFAULT_HEIGHT / 4);
            roi = new ROIShape(rect);
        } else {
            roi = null;
        }
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
        RenderedImage image = createDefaultTestImage(dataType, NUM_BANDS, false);
        Range range = getRange(dataType);

        // Descriptor string definition
        String description = "Binarize";

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
        PlanarImage imageCalculated = null;

        long mean = 0;
        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;

        // Cycle for calculating the mean, maximum and minimum calculation time
        for (int i = 0; i < totalCycles; i++) {

            // creation of the image
            if (old) {
                JAIExt.registerJAIDescriptor("Binarize");
                imageCalculated = javax.media.jai.operator.BinarizeDescriptor.create(image,
                        thresholds[dataType], null);
            } else {
                imageCalculated = BinarizeDescriptor.create(image, thresholds[dataType], roi,
                        range, null);
            }

            // Total calculation time
            long start = System.nanoTime();
            imageCalculated.getTiles();
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
        finalizeTest(null, imageCalculated);
    }
}
