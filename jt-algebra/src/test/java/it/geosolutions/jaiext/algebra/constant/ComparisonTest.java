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
package it.geosolutions.jaiext.algebra.constant;

import it.geosolutions.jaiext.algebra.AlgebraDescriptor.Operator;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.AddConstDescriptor;
import javax.media.jai.operator.DivideByConstDescriptor;
import javax.media.jai.operator.MultiplyConstDescriptor;
import javax.media.jai.operator.SubtractConstDescriptor;

import org.junit.BeforeClass;
import org.junit.Test;

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

    /** Number associated with the type of BorderExtender to use */
    private static final int OPERATION_TYPE = Integer.getInteger("JAI.Ext.OperationType", 0);

    /** Number associated with the type of BorderExtender to use */
    private static final int NUM_BANDS = 1;

    /** Image to elaborate */
    private static RenderedImage image;

    /** No Data Range parameter */
    private static Range range;

    /** Output value for No Data */
    private static double destNoData;

    private static ROIShape roi;

    private static Operator op;

    private static double[] consts;

    @BeforeClass
    public static void initialSetup() {

        // Setting of the image filler parameter to true for a better image creation
        IMAGE_FILLER = true;
        // Images initialization
        byte noDataB = 100;
        short noDataUS = 100;
        short noDataS = 100;
        int noDataI = 100;
        float noDataF = 100;
        double noDataD = 100;
        // Image creation
        image = null;

        // Constants
        consts = new double[] { 5 };

        switch (TEST_SELECTOR) {
        case DataBuffer.TYPE_BYTE:
            image = createTestImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataB,
                    false, NUM_BANDS);
            break;
        case DataBuffer.TYPE_USHORT:
            image = createTestImage(DataBuffer.TYPE_USHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                    noDataUS, false, NUM_BANDS);
            break;
        case DataBuffer.TYPE_SHORT:
            image = createTestImage(DataBuffer.TYPE_SHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataS,
                    false, NUM_BANDS);
            break;
        case DataBuffer.TYPE_INT:
            image = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataI,
                    false, NUM_BANDS);
            break;
        case DataBuffer.TYPE_FLOAT:
            image = createTestImage(DataBuffer.TYPE_FLOAT, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataF,
                    false, NUM_BANDS);
            break;
        case DataBuffer.TYPE_DOUBLE:
            image = createTestImage(DataBuffer.TYPE_DOUBLE, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataD,
                    false, NUM_BANDS);
            break;
        default:
            throw new IllegalArgumentException("Wrong data type");
        }
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

        // Operation used
        if (OPERATION_TYPE < Operator.values().length) {
            op = Operator.values()[OPERATION_TYPE];
        } else {
            throw new IllegalArgumentException("Operation not defined");
        }
        // destination No Data
        destNoData = 100d;
    }

    @Test
    public void testOperation() {

        // Image dataType
        int dataType = TEST_SELECTOR;

        // Descriptor string definition
        String description = "";
        switch (op) {
        case SUM:
            description = "Add";
            break;
        case SUBTRACT:
            description = "Subtract";
            break;
        case MULTIPLY:
            description = "Multiply";
            break;
        case DIVIDE:
            description = "Divide";
            break;
        case OR:
            description = "or";
            break;
        case XOR:
            description = "Xor";
            break;
        case AND:
            description = "And";
            break;
        default:
            break;
        }

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

                switch (op) {
                case SUM:
                    imageCalculated = AddConstDescriptor.create(image, consts, null);
                    break;
                case SUBTRACT:
                    imageCalculated = SubtractConstDescriptor.create(image, consts, null);
                    break;
                case MULTIPLY:
                    imageCalculated = MultiplyConstDescriptor.create(image, consts, null);
                    break;
                case DIVIDE:
                    imageCalculated = DivideByConstDescriptor.create(image, consts, null);
                    break;
                case DIVIDE_INTO:
                    imageCalculated = DivideByConstDescriptor.create(image, consts, null);
                    break;
                case AND:
                    imageCalculated = DivideByConstDescriptor.create(image, consts, null);
                    break;
                case OR:
                    imageCalculated = DivideByConstDescriptor.create(image, consts, null);
                    break;
                case XOR:
                    imageCalculated = DivideByConstDescriptor.create(image, consts, null);
                    break;
                default:
                    break;
                }
            } else {
                imageCalculated = OperationConstDescriptor.create(image, consts, op, roi, range,
                        destNoData, null);
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
