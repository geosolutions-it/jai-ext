package it.geosolutions.jaiext.lookup;

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
    private static LookupTableByte byteToByteTableNew;

    /** LookupTable from ushort to byte */
    private static LookupTableUShort ushortToByteTableNew;

    /** LookupTable from short to byte */
    private static LookupTableShort shortToByteTableNew;

    /** LookupTable from int to byte */
    private static LookupTableInt intToByteTableNew;

    /** LookupTableJAI from byte to byte */
    private static LookupTableJAI byteToByteTableOld;

    /** LookupTableJAI from ushort to byte */
    private static LookupTableJAI ushortToByteTableOld;

    /** LookupTableJAI from short to byte */
    private static LookupTableJAI shortToByteTableOld;

    /** LookupTableJAI from int to byte */
    private static LookupTableJAI intToByteTableOld;

    // Initial static method for preparing all the test data
    @BeforeClass
    public static void initialSetup() {
        // Setting of the imaage filler parameter to false for a faster image creation
        IMAGE_FILLER = false;
        // Images initialization
        // Byte Range goes from 0 to 255
        byte noDataB = (byte) 156;
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
            }

            if (i == noDataUS) {
                // ushort-to-all arrays
                dataUShortB[i] = 50;
            }

            if (i == (noDataB & 0xFF)) {
                // byte-to-all arrays
                dataByteB[i] = 50;
            }

        }

        // LookupTables creation
        byteToByteTableNew = new LookupTableByte(dataByteB, byteOffset);

        ushortToByteTableNew = new LookupTableUShort(dataUShortB, ushortOffset);

        shortToByteTableNew = new LookupTableShort(dataShortB, shortOffset);

        intToByteTableNew = new LookupTableInt(dataIntB, intOffset);

        byteToByteTableOld = new LookupTableJAI(dataByteB, byteOffset);

        ushortToByteTableOld = new LookupTableJAI(dataUShortB, ushortOffset);

        shortToByteTableOld = new LookupTableJAI(dataShortB, shortOffset);

        intToByteTableOld = new LookupTableJAI(dataIntB, intOffset);

        // Destination No Data
        destinationNoDataValue = 50;
    }

    @Test
    public void testNewLookupDescriptorByte() {
        if (TEST_SELECTOR == DataBuffer.TYPE_BYTE && !OLD_DESCRIPTOR) {
            testLookup(testImageByte, DataBuffer.TYPE_BYTE, byteToByteTableNew, OLD_DESCRIPTOR);
        }
    }

    @Test
    public void testOldLookupDescriptorByte() {
        if (TEST_SELECTOR == DataBuffer.TYPE_BYTE && OLD_DESCRIPTOR) {
            testLookup(testImageByte, DataBuffer.TYPE_BYTE, byteToByteTableOld, OLD_DESCRIPTOR);
        }
    }

    @Test
    public void testNewLookupDescriptorUShort() {
        if (TEST_SELECTOR == DataBuffer.TYPE_USHORT && !OLD_DESCRIPTOR) {
            testLookup(testImageUShort, DataBuffer.TYPE_USHORT, ushortToByteTableNew,
                    OLD_DESCRIPTOR);
        }
    }

    @Test
    public void testOldLookupDescriptorUShort() {
        if (TEST_SELECTOR == DataBuffer.TYPE_USHORT && OLD_DESCRIPTOR) {
            testLookup(testImageUShort, DataBuffer.TYPE_USHORT, ushortToByteTableOld,
                    OLD_DESCRIPTOR);
        }
    }

    @Test
    public void testNewLookupDescriptorShort() {
        if (TEST_SELECTOR == DataBuffer.TYPE_SHORT && !OLD_DESCRIPTOR) {
            testLookup(testImageShort, DataBuffer.TYPE_SHORT, shortToByteTableNew, OLD_DESCRIPTOR);
        }
    }

    @Test
    public void testOldLookupDescriptorShort() {
        if (TEST_SELECTOR == DataBuffer.TYPE_SHORT && OLD_DESCRIPTOR) {
            testLookup(testImageShort, DataBuffer.TYPE_SHORT, shortToByteTableOld, OLD_DESCRIPTOR);
        }
    }

    @Test
    public void testNewLookupDescriptorInt() {
        if (TEST_SELECTOR == DataBuffer.TYPE_INT && !OLD_DESCRIPTOR) {
            testLookup(testImageInt, DataBuffer.TYPE_INT, intToByteTableNew, OLD_DESCRIPTOR);
        }
    }

    @Test
    public void testOldLookupDescriptorInt() {
        if (TEST_SELECTOR == DataBuffer.TYPE_INT && OLD_DESCRIPTOR) {
            testLookup(testImageInt, DataBuffer.TYPE_INT, intToByteTableOld, OLD_DESCRIPTOR);
        }
    }

    // General method for showing calculation time of the 2 LookupDescriptors
    public void testLookup(RenderedImage testIMG, int dataType, Object table, boolean old) {
        // Descriptor string
        String description = "\n ";

        if (old) {
            description = "Old Lookup";
            System.setProperty("com.sun.media.jai.disableMediaLib", "false");
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
                        destinationNoDataValue, null, null, false, null);
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
        //Final Image disposal
        if(imageLookup instanceof RenderedOp){
            ((RenderedOp)imageLookup).dispose();
        }
        

    }

    // UNSUPPORTED OPERATIONS
    @Override
    protected void testGlobal(boolean useROIAccessor, boolean isBinary, boolean bicubic2Disabled,
            boolean noDataRangeUsed, boolean roiPresent, InterpolationType interpType,
            TestSelection testSelect, ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    protected <T extends Number & Comparable<? super T>> void testImage(int dataType,
            T noDataValue, boolean useROIAccessor, boolean isBinary, boolean bicubic2Disabled,
            boolean noDataRangeUsed, boolean roiPresent, InterpolationType interpType,
            TestSelection testSelect, ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    protected <T extends Number & Comparable<? super T>> void testImageAffine(
            RenderedImage sourceImage, int dataType, T noDataValue, boolean useROIAccessor,
            boolean isBinary, boolean bicubic2Disabled, boolean noDataRangeUsed,
            boolean roiPresent, boolean setDestinationNoData, TransformationType transformType,
            InterpolationType interpType, TestSelection testSelect, ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    protected void testGlobalAffine(boolean useROIAccessor, boolean isBinary,
            boolean bicubic2Disabled, boolean noDataRangeUsed, boolean roiPresent,
            boolean setDestinationNoData, InterpolationType interpType, TestSelection testSelect,
            ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported");
    }

}
