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
package it.geosolutions.jaiext.algebra;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.AbsoluteDescriptor;
import javax.media.jai.operator.AddDescriptor;
import javax.media.jai.operator.AndDescriptor;
import javax.media.jai.operator.DivideDescriptor;
import javax.media.jai.operator.ExpDescriptor;
import javax.media.jai.operator.InvertDescriptor;
import javax.media.jai.operator.LogDescriptor;
import javax.media.jai.operator.MultiplyDescriptor;
import javax.media.jai.operator.NotDescriptor;
import javax.media.jai.operator.OrDescriptor;
import javax.media.jai.operator.SubtractDescriptor;
import javax.media.jai.operator.XorDescriptor;

import org.junit.BeforeClass;
import org.junit.Test;
import it.geosolutions.jaiext.algebra.AlgebraDescriptor.Operator;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;


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
    
    /** Number associated with the type of BorderExtender to use*/
    private static final int OPERATION_TYPE = Integer.getInteger("JAI.Ext.OperationType", 0);

    /** Number associated with the type of BorderExtender to use*/
    private static final int NUM_IMAGES = Integer.getInteger("JAI.Ext.NumImages", 2);
    
    /** Number associated with the type of BorderExtender to use*/
    private static final int NUM_BANDS = 1;
    
    /** Image to elaborate */
    private static RenderedImage[] images;

    /** No Data Range parameter */
    private static Range range;

    /** Output value for No Data*/
    private static double destNoData;

    private static ROIShape roi;

    private static Operator op;

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
        images = new RenderedImage[NUM_IMAGES];
        
        switch (TEST_SELECTOR) {
        case DataBuffer.TYPE_BYTE:
            for(int i = 0; i < NUM_IMAGES; i++){
                images[i] = createTestImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataB,
                        false,NUM_BANDS);
            }            
            break;
        case DataBuffer.TYPE_USHORT:
            for(int i = 0; i < NUM_IMAGES; i++){
                images[i] = createTestImage(DataBuffer.TYPE_USHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataUS,
                        false,NUM_BANDS);
            }
            break;
        case DataBuffer.TYPE_SHORT:
            for(int i = 0; i < NUM_IMAGES; i++){
                images[i] = createTestImage(DataBuffer.TYPE_SHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataS,
                        false,NUM_BANDS);
            }
            break;
        case DataBuffer.TYPE_INT:
            for(int i = 0; i < NUM_IMAGES; i++){
                images[i] = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataI,
                        false,NUM_BANDS);
            }
            break;
        case DataBuffer.TYPE_FLOAT:
            for(int i = 0; i < NUM_IMAGES; i++){
                images[i] = createTestImage(DataBuffer.TYPE_FLOAT, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataF,
                        false,NUM_BANDS);
            }
            break;
        case DataBuffer.TYPE_DOUBLE:
            for(int i = 0; i < NUM_IMAGES; i++){
                images[i] = createTestImage(DataBuffer.TYPE_DOUBLE, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataD,
                        false,NUM_BANDS);
            }
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
        if(OPERATION_TYPE < Operator.values().length){
            op = Operator.values()[OPERATION_TYPE];
        }else{
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
        switch(op){
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
        case ABSOLUTE:
            description = "Absolute";
            break;
        case AND:
            description = "And";
            break;
        case EXP:
            description = "Exp";
            break;
        case INVERT:
            description = "Invert";
            break;
        case LOG:
            description = "Log";
            break;
        case NOT:
            description = "Not";
            break;
        case DIVIDE_INTO:
            description = "DivideInto";
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
                
                RenderedImage firstOp;
                
                switch(op){
                case SUM:
                    firstOp = images[0];
                    for(int j = 1; j < NUM_IMAGES; j++){
                        firstOp = AddDescriptor.create(firstOp, images[j], null);                        
                    }
                    imageCalculated = (PlanarImage) firstOp;
                    break;
                case SUBTRACT:
                    firstOp = images[0];
                    for(int j = 1; j < NUM_IMAGES; j++){
                        firstOp = SubtractDescriptor.create(firstOp, images[j], null);                        
                    }
                    imageCalculated = (PlanarImage) firstOp;
                    break;
                case MULTIPLY:
                    firstOp = images[0];
                    for(int j = 1; j < NUM_IMAGES; j++){
                        firstOp = MultiplyDescriptor.create(firstOp, images[j], null);                        
                    }
                    imageCalculated = (PlanarImage) firstOp;
                    break;
                case DIVIDE:
                    firstOp = images[0];
                    for(int j = 1; j < NUM_IMAGES; j++){
                        firstOp = DivideDescriptor.create(firstOp, images[j], null);                        
                    }
                    imageCalculated = (PlanarImage) firstOp;
                    break;
                case ABSOLUTE:
                    imageCalculated = (PlanarImage) AbsoluteDescriptor.create(images[0], null);;
                    break;
                case AND:
                    firstOp = images[0];
                    for(int j = 1; j < NUM_IMAGES; j++){
                        firstOp = AndDescriptor.create(firstOp, images[j], null);                        
                    }
                    imageCalculated = (PlanarImage) firstOp;
                    break;
                case OR:
                    firstOp = images[0];
                    for(int j = 1; j < NUM_IMAGES; j++){
                        firstOp = OrDescriptor.create(firstOp, images[j], null);                        
                    }
                    imageCalculated = (PlanarImage) firstOp;
                    break;
                case XOR:
                    firstOp = images[0];
                    for(int j = 1; j < NUM_IMAGES; j++){
                        firstOp = XorDescriptor.create(firstOp, images[j], null);                        
                    }
                    imageCalculated = (PlanarImage) firstOp;
                    break;
                case NOT:
                    imageCalculated = (PlanarImage) NotDescriptor.create(images[0], null);
                    break;
                case EXP:
                    imageCalculated = (PlanarImage) ExpDescriptor.create(images[0], null);
                    break;
                case INVERT:
                    imageCalculated = (PlanarImage) InvertDescriptor.create(images[0], null);
                    break;
                case LOG:
                    imageCalculated = (PlanarImage) LogDescriptor.create(images[0], null);
                    break;
                }                
            } else {
                imageCalculated = AlgebraDescriptor.create(op, roi, range, destNoData, null, images);
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
