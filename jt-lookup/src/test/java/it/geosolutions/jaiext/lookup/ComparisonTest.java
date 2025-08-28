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
import it.geosolutions.jaiext.testclasses.TestBase;

import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

import javax.media.jai.JAI;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.PlanarImage;

import org.junit.BeforeClass;
import org.junit.Ignore;
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

    /**
     * Destination No Data value
     */
    private static double destinationNoDataValue;

    /**
     * Byte image
     */
    private static RenderedImage testImageByte;

    /**
     * Unsigned Short image
     */
    private static RenderedImage testImageUShort;

    /**
     * Short image
     */
    private static RenderedImage testImageShort;

    /**
     * Integer image
     */
    private static RenderedImage testImageInt;

    /**
     * LookupTable from byte to byte
     */
    private static LookupTable byteToByteTableNew;

    /**
     * LookupTable from ushort to byte
     */
    private static LookupTable ushortToByteTableNew;

    /**
     * LookupTable from short to byte
     */
    private static LookupTable shortToByteTableNew;

    /**
     * LookupTable from int to byte
     */
    private static LookupTable intToByteTableNew;

    /**
     * LookupTableJAI from byte to byte
     */
    private static LookupTableJAI byteToByteTableOld;

    /**
     * LookupTableJAI from ushort to byte
     */
    private static LookupTableJAI ushortToByteTableOld;

    /**
     * LookupTableJAI from short to byte
     */
    private static LookupTableJAI shortToByteTableOld;

    /**
     * LookupTableJAI from int to byte
     */
    private static LookupTableJAI intToByteTableOld;

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

        if (OLD_DESCRIPTOR) {
            JAIExt.registerJAIDescriptor("Lookup");
        }
    }

    @Override
    protected boolean supportDataType(int dataType) {
        if (dataType == DataBuffer.TYPE_SHORT || dataType == DataBuffer.TYPE_FLOAT
                || dataType == DataBuffer.TYPE_DOUBLE)
            return false;
        else
            return super.supportDataType(dataType);
    }

    // General method for showing calculation time of the 2 LookupDescriptors
    public void testOperation(int dataType, TestRoiNoDataType testType) {
        Range rangeND = getRange(dataType, testType);
        LookupTable table = null;
        LookupTableJAI tableJai = null;
        if (OLD_DESCRIPTOR) {
            switch (dataType) {
                case DataBuffer.TYPE_BYTE:
                    tableJai = byteToByteTableOld;
                    break;
                case DataBuffer.TYPE_USHORT:
                    tableJai = ushortToByteTableOld;
                    break;
                case DataBuffer.TYPE_SHORT:
                    tableJai = shortToByteTableOld;
                    break;
                case DataBuffer.TYPE_INT:
                    tableJai = intToByteTableOld;
                    break;
                default:
                    throw new IllegalArgumentException("DataType not supported");
            }
        } else {

            switch (dataType) {
                case DataBuffer.TYPE_BYTE:
                    table = byteToByteTableNew;
                    break;
                case DataBuffer.TYPE_USHORT:
                    table = ushortToByteTableNew;
                    break;
                case DataBuffer.TYPE_SHORT:
                    table = shortToByteTableNew;
                    break;
                case DataBuffer.TYPE_INT:
                    table = intToByteTableNew;
                    break;
                default:
                    throw new IllegalArgumentException("DataType not supported");
            }
        }

        RenderedImage testImage;
        switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                testImage = testImageByte;
                break;
            case DataBuffer.TYPE_USHORT:
                testImage = testImageUShort;
                break;
            case DataBuffer.TYPE_SHORT:
                testImage = testImageShort;
                break;
            case DataBuffer.TYPE_INT:
                testImage = testImageInt;
                break;
            default:
                throw new IllegalArgumentException("DataType not supported");
        }


        // PlanarImage
        PlanarImage image = null;
        if (OLD_DESCRIPTOR) {
            image = javax.media.jai.operator.LookupDescriptor.create(testImage,
                    tableJai, null);
        } else {
            image = LookupDescriptor.create(testImage, table,
                    destinationNoDataValue, null, rangeND, false, null);
        }
        finalizeTest(null, dataType, image);
    }

}
