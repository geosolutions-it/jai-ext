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
package it.geosolutions.jaiext.lookup;

import it.geosolutions.jaiext.JAIExt;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;

import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

import javax.media.jai.JAI;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test class is used for compare the timing between the new LookupDescriptor and the its old JAI version. No Roi or No Data range are used. If
 * the user wants to change the number of the benchmark cycles or of the not benchmark cycles, should only pass the new values to the
 * JAI.Ext.BenchmarkCycles or JAI.Ext.NotBenchmarkCycles parameters.If the user want to use the old LookupDescriptor must pass to the JVM the
 * JAI.Ext.OldDescriptor parameter set to true. For selecting a specific data type the user must set the JAI.Ext.TestSelector JVM integer parameter to
 * a number between 0 and 3 (where 0 means byte, 1 Ushort, 2 Short, 3 Integer). Inside this test class the various tests are executed in the same
 * manner:
 * <ul>
 * <li>Selection of the LookupDescriptor(Old or New)</li>
 * <li>Selection of the source image(4 different dataType)</li>
 * <li>statistic calculation (if the cycle belongs to the benchmark cycles)</li>
 * </ul>
 * The selection of the old or new descriptor must be done by setting to true or false the JVM parameter JAI.Ext.OldDescriptor. If the user wants to
 * use the accelerated code, the JVM parameter JAI.Ext.Acceleration must be set to true.
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

    /** Boolean indicating if the native acceleration must be used */
    private final static boolean NATIVE_ACCELERATION = Boolean.getBoolean("JAI.Ext.Acceleration");

    /** Boolean indicating if a No Data Range must be used */
    private final static boolean RANGE_USED = Boolean.getBoolean("JAI.Ext.RangeUsed");

    /** Destination No Data value */
    private static double destinationNoDataValue;

    /** Byte image */
    private static RenderedImage testImageByte;

    /** Unsigned Short image */
    private static RenderedImage testImageUShort;

    /** Short image */
    private static RenderedImage testImageShort;

    /** Integer image */
    private static RenderedImage testImageInt;

    /** LookupTable from byte to byte */
    private static LookupTable byteToByteTableNew;

    /** LookupTable from ushort to byte */
    private static LookupTable ushortToByteTableNew;

    /** LookupTable from short to byte */
    private static LookupTable shortToByteTableNew;

    /** LookupTable from int to byte */
    private static LookupTable intToByteTableNew;

    /** LookupTableJAI from byte to byte */
    private static LookupTableJAI byteToByteTableOld;

    /** LookupTableJAI from ushort to byte */
    private static LookupTableJAI ushortToByteTableOld;

    /** LookupTableJAI from short to byte */
    private static LookupTableJAI shortToByteTableOld;

    /** LookupTableJAI from int to byte */
    private static LookupTableJAI intToByteTableOld;

    private static Range rangeND;

    // Initial static method for preparing all the test data
    @BeforeClass
    public static void initialSetup() {
        // Setting of the imaage filler parameter to false for a faster image creation
        IMAGE_FILLER = false;
        // Images initialization
        // Byte Range goes from 0 to 255
        byte noDataB = -100;
        short noDataUS = 100;
        short noDataS = -100;
        int noDataI = -100;
        // Image creations
        testImageByte = createTestImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataB, false);
        testImageUShort = createTestImage(DataBuffer.TYPE_USHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataUS, false);
        testImageShort = createTestImage(DataBuffer.TYPE_SHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataS, false);
        testImageInt = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataI,
                false);
        // Offset creation
        int byteOffset = 0;
        int ushortOffset = 0;
        int shortOffset = noDataS;
        int intOffset = noDataI;

        // Array Lookup creation
        int arrayLength = 201;
        int startValue = -100;
        // Array Initialization
        byte[] dataByteB = new byte[arrayLength];
        byte[] dataUShortB = new byte[arrayLength];
        byte[] dataShortB = new byte[arrayLength];
        byte[] dataIntB = new byte[arrayLength];
        // Construction of the various arrays
        for (int i = 0; i < arrayLength; i++) {
            dataByteB[i] = 0;
            dataUShortB[i] = 0;
            dataShortB[i] = 0;
            dataIntB[i] = 0;

            int value = i + startValue;

            if (value == noDataI) {
                dataShortB[i] = 50;
                dataIntB[i] = 50;
                dataByteB[i] = 50;
            }

            if (i == noDataUS) {
                // ushort-to-all arrays
                dataUShortB[i] = 50; 
            }

        }

        // LookupTables creation
        byteToByteTableNew = new LookupTable(dataByteB, byteOffset);

        ushortToByteTableNew = new LookupTable(dataUShortB, ushortOffset);

        shortToByteTableNew = new LookupTable(dataShortB, shortOffset);

        intToByteTableNew = new LookupTable(dataIntB, intOffset);

        byteToByteTableOld = new LookupTableJAI(dataByteB, byteOffset);

        ushortToByteTableOld = new LookupTableJAI(dataUShortB, ushortOffset);

        shortToByteTableOld = new LookupTableJAI(dataShortB, shortOffset);

        intToByteTableOld = new LookupTableJAI(dataIntB, intOffset);

        // Destination No Data
        destinationNoDataValue = 50;
        
        //Range creation if selected
        rangeND= null;
        if(RANGE_USED && !OLD_DESCRIPTOR){
            switch(TEST_SELECTOR){
            case DataBuffer.TYPE_BYTE:
                rangeND = RangeFactory.create((byte)100,true,(byte)100,true);
                break;
            case DataBuffer.TYPE_USHORT:
                rangeND = RangeFactory.createU((short)100,true,(short)100,true);
                break;
            case DataBuffer.TYPE_SHORT:
                rangeND = RangeFactory.create((short)100,true,(short)100,true);
                break;
            case DataBuffer.TYPE_INT:
                rangeND = RangeFactory.create(100,true,100,true);
                break;
                default:
                    throw new IllegalArgumentException("Wrong data type");
            }
            
        }
        
        if(OLD_DESCRIPTOR){
            JAIExt.registerJAIDescriptor("lookup");
        }
        
        
    }

    @Test
    public void testNewLookupDescriptorByte() {
        if (TEST_SELECTOR == DataBuffer.TYPE_BYTE && !OLD_DESCRIPTOR) {
            testLookup(testImageByte, DataBuffer.TYPE_BYTE, byteToByteTableNew,rangeND, OLD_DESCRIPTOR);
        }
    }

    @Test
    public void testOldLookupDescriptorByte() {
        if (TEST_SELECTOR == DataBuffer.TYPE_BYTE && OLD_DESCRIPTOR) {
            testLookup(testImageByte, DataBuffer.TYPE_BYTE, byteToByteTableOld,rangeND, OLD_DESCRIPTOR);
        }
    }

    @Test
    public void testNewLookupDescriptorUShort() {
        if (TEST_SELECTOR == DataBuffer.TYPE_USHORT && !OLD_DESCRIPTOR) {
            testLookup(testImageUShort, DataBuffer.TYPE_USHORT, ushortToByteTableNew,rangeND,
                    OLD_DESCRIPTOR);
        }
    }

    @Test
    public void testOldLookupDescriptorUShort() {
        if (TEST_SELECTOR == DataBuffer.TYPE_USHORT && OLD_DESCRIPTOR) {
            testLookup(testImageUShort, DataBuffer.TYPE_USHORT, ushortToByteTableOld,rangeND,
                    OLD_DESCRIPTOR);
        }
    }

    @Test
    public void testNewLookupDescriptorShort() {
        if (TEST_SELECTOR == DataBuffer.TYPE_SHORT && !OLD_DESCRIPTOR) {
            testLookup(testImageShort, DataBuffer.TYPE_SHORT, shortToByteTableNew,rangeND, OLD_DESCRIPTOR);
        }
    }

    @Test
    public void testOldLookupDescriptorShort() {
        if (TEST_SELECTOR == DataBuffer.TYPE_SHORT && OLD_DESCRIPTOR) {
            testLookup(testImageShort, DataBuffer.TYPE_SHORT, shortToByteTableOld,rangeND, OLD_DESCRIPTOR);
        }
    }

    @Test
    public void testNewLookupDescriptorInt() {
        if (TEST_SELECTOR == DataBuffer.TYPE_INT && !OLD_DESCRIPTOR) {
            testLookup(testImageInt, DataBuffer.TYPE_INT, intToByteTableNew,rangeND, OLD_DESCRIPTOR);
        }
    }

    @Test
    public void testOldLookupDescriptorInt() {
        if (TEST_SELECTOR == DataBuffer.TYPE_INT && OLD_DESCRIPTOR) {
            testLookup(testImageInt, DataBuffer.TYPE_INT, intToByteTableOld,rangeND, OLD_DESCRIPTOR);
        }
    }

    // General method for showing calculation time of the 2 LookupDescriptors
    public void testLookup(RenderedImage testIMG, int dataType, Object table,Range rangeND, boolean old) {
        // Descriptor string
        String description = "\n ";

        if (old) {
            description = "Old Lookup";
            if (NATIVE_ACCELERATION) {
                description += " accelerated ";
                System.setProperty("com.sun.media.jai.disableMediaLib", "false");
            } else {
                System.setProperty("com.sun.media.jai.disableMediaLib", "true");
            }
        } else {
            description = "New Lookup";
            System.setProperty("com.sun.media.jai.disableMediaLib", "true");
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
        }

        // Total cycles number
        int totalCycles = BENCHMARK_ITERATION + NOT_BENCHMARK_ITERATION;
        // PlanarImage
        PlanarImage imageLookup = null;
        // Initialization of the statistics
        long mean = 0;
        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;

        // Cycle for calculating the mean, maximum and minimum calculation time
        for (int i = 0; i < totalCycles; i++) {

            // creation of the image with the selected descriptor

            if (old) {
                imageLookup = javax.media.jai.operator.LookupDescriptor.create(testIMG,
                        (LookupTableJAI) table, null);
            } else {
                imageLookup = LookupDescriptor.create(testIMG, (LookupTable) table,
                        destinationNoDataValue, null, rangeND, false, null);
            }

            // Total calculation time
            long start = System.nanoTime();
            imageLookup.getTiles();
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
        // Comparison between the mean times
        System.out.println(dataTypeString);
        // Output print
        System.out.println("\nMean value for " + description + "Descriptor : " + meanValue
                + " msec.");
        System.out.println("Maximum value for " + description + "Descriptor : " + maxD + " msec.");
        System.out.println("Minimum value for " + description + "Descriptor : " + minD + " msec.");
        // Final Image disposal
        if (imageLookup instanceof RenderedOp) {
            ((RenderedOp) imageLookup).dispose();
        }

    }
}
