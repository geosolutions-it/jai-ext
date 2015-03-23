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
package it.geosolutions.jaiext.imagefunction;

import it.geosolutions.jaiext.JAIExt;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;

import javax.media.jai.ImageFunction;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class used for comparing the JAI ImageFunction operation with the JAI-EXT one. Users may define how many benchmark cycles to do, how many
 * not-benchmark cycles to do and other variables, like ROI/NoData use. The parameters to define (as JVM options -D..)are:
 * <ul>
 * <li>JAI.Ext.BenchmarkCycles indicating how many benchmark cycles must be executed</li>
 * <li>JAI.Ext.NotBenchmarkCycles indicating how many cycles must be executed before doing the test</li>
 * <li>JAI.Ext.OldDescriptor(true/false) indicating if the old JAI operation must be done</li>
 * <li>JAI.Ext.RangeUsed(true/false) indicating if nodata check must be done (only for jai-ext)</li>
 * <li>JAI.Ext.ROIUsed(true/false) indicating if roi check must be done (only for jai-ext)</li>
 * </ul>
 * 
 * @author Nicola Lagomarsini geosolutions
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

    /** Boolean indicating if a ROI must be used */
    private final static boolean ROI_USED = Boolean.getBoolean("JAI.Ext.ROIUsed");

    /** No Data Range parameter */
    private static Range range;

    /** ROI Object used for testing */
    private static ROI roi;

    /** {@link ImageFunction} used in test */
    private static ImageFunctionJAIEXT function;

    /** Output image width */
    private static int width;

    /** Output image height */
    private static int height;

    /** X translation of input pixels */
    private static float xTrans;

    /** Y translation of input pixels */
    private static float yTrans;

    /** X scale of input pixels */
    private static float xScale;

    /** Y scale of input pixels */
    private static float yScale;

    @BeforeClass
    public static void init() {

        // Setting of the image filler parameter to true for a better image creation
        IMAGE_FILLER = true;
        // Images initialization
        byte noDataB = 100;
        short noDataUS = 100;
        short noDataS = 100;
        int noDataI = 100;
        float noDataF = 100;
        double noDataD = 100;
        // Image filler must be reset
        IMAGE_FILLER = false;

        // Range creation if selected
        if (RANGE_USED && !OLD_DESCRIPTOR) {
            switch (TEST_SELECTOR) {
            case DataBuffer.TYPE_BYTE:
                range = RangeFactory.create(noDataB, true, noDataB, true);
                break;
            case DataBuffer.TYPE_USHORT:
                range = RangeFactory.createU(noDataUS, true, noDataUS, true);
                break;
            case DataBuffer.TYPE_SHORT:
                range = RangeFactory.create(noDataS, true, noDataS, true);
                break;
            case DataBuffer.TYPE_INT:
                range = RangeFactory.create(noDataI, true, noDataI, true);
                break;
            case DataBuffer.TYPE_FLOAT:
                range = RangeFactory.create(noDataF, true, noDataF, true, true);
                break;
            case DataBuffer.TYPE_DOUBLE:
                range = RangeFactory.create(noDataD, true, noDataD, true, true);
                break;
            default:
                throw new IllegalArgumentException("Wrong data type");
            }
        }

        // ROI creation
        if (ROI_USED) {
            Rectangle rect = new Rectangle(0, 0, DEFAULT_WIDTH / 4, DEFAULT_HEIGHT / 4);
            roi = new ROIShape(rect);
        } else {
            roi = null;
        }

        // ImageFunction
        function = new ImageFunctionTest.DummyFunction();

        // size and other parameters
        width = 256;
        height = 256;
        xTrans = 2f;
        yTrans = 2f;
        xScale = 3f;
        yScale = 3f;

    }

    @Test
    public void testOperation() {

        // Image dataType
        int dataType = TEST_SELECTOR;

        // Descriptor string definition
        String description = "ImageFunction";

        if (OLD_DESCRIPTOR) {
            description = "Old " + description;
        } else {
            description = "New " + description;
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
        // Image
        PlanarImage imageCalculated = null;

        long mean = 0;
        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;

        // Cycle for calculating the mean, maximum and minimum calculation time
        for (int i = 0; i < totalCycles; i++) {

            // creation of the image
            if (OLD_DESCRIPTOR) {
                JAIExt.registerJAIDescriptor("ImageFunction");
                imageCalculated = javax.media.jai.operator.ImageFunctionDescriptor.create(
                        (ImageFunction) function, width, height, xScale, yScale, xTrans, yTrans,
                        null);
            } else {
                imageCalculated = ImageFunctionDescriptor.create((ImageFunction) function, width,
                        height, xScale, yScale, xTrans, yTrans, roi, range, 0f, null);
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

        // Final Image disposal
        if (imageCalculated instanceof RenderedOp) {
            ((RenderedOp) imageCalculated).dispose();
        }

    }
}
