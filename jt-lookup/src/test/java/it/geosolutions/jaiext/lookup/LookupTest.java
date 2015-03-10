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

import static org.junit.Assert.*;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;
import it.geosolutions.rendered.viewer.RenderedImageBrowser;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;

import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This testclass is used for checking the functionality of the LookupOpImage class. The tests are divided in 5 groups:
 * <ul>
 * <li>Tests without ROI and without no Data</li>
 * <li>Tests with ROI (RoiAccessor not used) and without no Data</li>
 * <li>Tests with ROI (RoiAccessor used) and without no Data</li>
 * <li>Tests without ROI and with no Data</li>
 * <li>Tests with ROI (RoiAccessor used) and with no Data</li>
 * </ul>
 * 
 * All the tests are performed on all the image data types. If the source and destination images are byte images, then they can be printed to the
 * screen by setting the JVM parameter JAI.Ext.Interactive to true, and selecting one of the 5 test-groups with the JVM integer parameter
 * JAI.Ext.TestSelector. The user can choose between the following values:
 * <ul>
 * <li>0 No ROI only valid data</li>
 * <li>1 Roi RasterAccessor and only valid data</li>
 * <li>2 Roi without RasterAccessor and only valid data</li>
 * <li>3 Roi RasterAccessor and No data</li>
 * <li>5 No Data without ROI</li>
 * </ul>
 * 
 * If the image is not printed to screen, then the PlanarImage.getTiles() method is called for forcing the calculation of all the image tiles. When
 * all the tiles are calculated, other two test are done: the first tests if the ROI is well calculated, and the second checks that the image is
 * filled with values.
 */

public class LookupTest extends TestBase {
    /** Byte test image */
    private static RenderedImage testImageByte;

    /** Ushort test image */
    private static RenderedImage testImageUShort;

    /** Short test image */
    private static RenderedImage testImageShort;

    /** Integer test image */
    private static RenderedImage testImageInt;

    /** LookupTable from byte to byte */
    private static LookupTable byteToByteTable;

    /** LookupTable from byte to ushort */
    private static LookupTable byteToUshortTable;

    /** LookupTable from byte to short */
    private static LookupTable byteToShortTable;

    /** LookupTable from byte to int */
    private static LookupTable byteToIntTable;

    /** LookupTable from byte to float */
    private static LookupTable byteToFloatTable;

    /** LookupTable from byte to double */
    private static LookupTable byteToDoubleTable;

    /** LookupTable from ushort to byte */
    private static LookupTable ushortToByteTable;

    /** LookupTable from ushort to ushort */
    private static LookupTable ushortToUshortTable;

    /** LookupTable from ushort to short */
    private static LookupTable ushortToShortTable;

    /** LookupTable from ushort to int */
    private static LookupTable ushortToIntTable;

    /** LookupTable from ushort to float */
    private static LookupTable ushortToFloatTable;

    /** LookupTable from ushort to double */
    private static LookupTable ushortToDoubleTable;

    /** LookupTable from short to byte */
    private static LookupTable shortToByteTable;

    /** LookupTable from short to ushort */
    private static LookupTable shortToUshortTable;

    /** LookupTable from short to short */
    private static LookupTable shortToShortTable;

    /** LookupTable from short to int */
    private static LookupTable shortToIntTable;

    /** LookupTable from short to float */
    private static LookupTable shortToFloatTable;

    /** LookupTable from short to double */
    private static LookupTable shortToDoubleTable;

    /** LookupTable from int to byte */
    private static LookupTable intToByteTable;

    /** LookupTable from int to ushort */
    private static LookupTable intToUshortTable;

    /** LookupTable from int to short */
    private static LookupTable intToShortTable;

    /** LookupTable from int to int */
    private static LookupTable intToIntTable;

    /** LookupTable from int to float */
    private static LookupTable intToFloatTable;

    /** LookupTable from int to double */
    private static LookupTable intToDoubleTable;

    /** ROI used in tests */
    private static ROIShape roi;

    /** Destination No Data value used */
    private static double destinationNoDataValue;

    /** Byte no Data range */
    private static Range rangeB;

    /** UShort no Data range */
    private static Range rangeUS;

    /** Short no Data range */
    private static Range rangeS;

    /** Integer no Data range */
    private static Range rangeI;

    // Initial static method for preparing all the test data
    @BeforeClass
    public static void initialSetup() {
        // Setting of an input parameter to be always false, avoiding the image to be totally filled by values
        IMAGE_FILLER = false;
        // Images initialization
        byte noDataB = -100;
        short noDataUS = 100;
        short noDataS = -100;
        int noDataI = -100;
        // Test images creation
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

        byte[] dataByteB = new byte[arrayLength];
        short[] dataByteUS = new short[arrayLength];
        short[] dataByteS = new short[arrayLength];
        int[] dataByteI = new int[arrayLength];
        float[] dataByteF = new float[arrayLength];
        double[] dataByteD = new double[arrayLength];

        byte[] dataUShortB = new byte[arrayLength];
        short[] dataUShortUS = new short[arrayLength];
        short[] dataUShortS = new short[arrayLength];
        int[] dataUShortI = new int[arrayLength];
        float[] dataUShortF = new float[arrayLength];
        double[] dataUShortD = new double[arrayLength];

        byte[] dataShortB = new byte[arrayLength];
        short[] dataShortUS = new short[arrayLength];
        short[] dataShortS = new short[arrayLength];
        int[] dataShortI = new int[arrayLength];
        float[] dataShortF = new float[arrayLength];
        double[] dataShortD = new double[arrayLength];

        byte[] dataIntB = new byte[arrayLength];
        short[] dataIntUS = new short[arrayLength];
        short[] dataIntS = new short[arrayLength];
        int[] dataIntI = new int[arrayLength];
        float[] dataIntF = new float[arrayLength];
        double[] dataIntD = new double[arrayLength];
        // Array construction
        for (int i = 0; i < arrayLength; i++) {
            // byte-to-all arrays
            dataByteB[i] = 0;
            dataByteUS[i] = 0;
            dataByteS[i] = 0;
            dataByteI[i] = 0;
            dataByteF[i] = (i * 1.0f) / arrayLength;
            dataByteD[i] = (i * 1.0d) / arrayLength * 2;

            // ushort-to-all arrays
            dataUShortB[i] = 0;
            dataUShortUS[i] = 0;
            dataUShortS[i] = 0;
            dataUShortI[i] = 0;
            dataUShortF[i] = (i * 1.0f) / arrayLength;
            dataUShortD[i] = (i * 1.0d) / arrayLength * 2;

            // short-to-all arrays
            dataShortB[i] = 0;
            dataShortUS[i] = 0;
            dataShortS[i] = 0;
            dataShortI[i] = 0;
            dataShortF[i] = (i * 1.0f) / arrayLength;
            dataShortD[i] = (i * 1.0d) / arrayLength * 2;

            // int-to-all arrays
            dataIntB[i] = 0;
            dataIntUS[i] = 0;
            dataIntS[i] = 0;
            dataIntI[i] = 0;
            dataIntF[i] = (i * 1.0f) / arrayLength;
            dataIntD[i] = (i * 1.0d) / arrayLength * 2;

            int value = i + startValue;

            if (value == noDataI) {
                // short-to-all arrays
                dataShortB[i] = 50;
                dataShortUS[i] = 50;
                dataShortS[i] = 50;
                dataShortI[i] = 50;

                // int-to-all arrays
                dataIntB[i] = 50;
                dataIntUS[i] = 50;
                dataIntS[i] = 50;
                dataIntI[i] = 50;
             // byte-to-all arrays
                dataByteB[i] = 50;
                dataByteUS[i] = 50;
                dataByteS[i] = 50;
                dataByteI[i] = 50;
            }

            if (i == noDataUS) {
                // ushort-to-all arrays
                dataUShortB[i] = 50;
                dataUShortUS[i] = 50;
                dataUShortS[i] = 50;
                dataUShortI[i] = 50;
            }

        }

        // LookupTables creation
        byteToByteTable = new LookupTable(dataByteB, byteOffset);
        byteToUshortTable = new LookupTable(dataByteUS, byteOffset, true);
        byteToShortTable = new LookupTable(dataByteS, byteOffset, false);
        byteToIntTable = new LookupTable(dataByteI, byteOffset);
        byteToFloatTable = new LookupTable(dataByteF, byteOffset);
        byteToDoubleTable = new LookupTable(dataByteD, byteOffset);

        ushortToByteTable = new LookupTable(dataUShortB, ushortOffset);
        ushortToUshortTable = new LookupTable(dataUShortUS, ushortOffset, true);
        ushortToShortTable = new LookupTable(dataUShortS, ushortOffset, false);
        ushortToIntTable = new LookupTable(dataUShortI, ushortOffset);
        ushortToFloatTable = new LookupTable(dataUShortF, ushortOffset);
        ushortToDoubleTable = new LookupTable(dataUShortD, ushortOffset);

        shortToByteTable = new LookupTable(dataShortB, shortOffset);
        shortToUshortTable = new LookupTable(dataShortUS, shortOffset, true);
        shortToShortTable = new LookupTable(dataShortS, shortOffset, false);
        shortToIntTable = new LookupTable(dataShortI, shortOffset);
        shortToFloatTable = new LookupTable(dataShortF, shortOffset);
        shortToDoubleTable = new LookupTable(dataShortD, shortOffset);

        intToByteTable = new LookupTable(dataIntB, intOffset);
        intToUshortTable = new LookupTable(dataIntUS, intOffset, true);
        intToShortTable = new LookupTable(dataIntS, intOffset, false);
        intToIntTable = new LookupTable(dataIntI, intOffset);
        intToFloatTable = new LookupTable(dataIntF, intOffset);
        intToDoubleTable = new LookupTable(dataIntD, intOffset);
        // ROI creation
        Rectangle roiBounds = new Rectangle(0, 0, DEFAULT_WIDTH / 4, DEFAULT_HEIGHT / 4);
        roi = new ROIShape(roiBounds);
        // NoData creation
        rangeB = RangeFactory.create(noDataB, true, noDataB, true);
        rangeUS = RangeFactory.createU(noDataUS, true, noDataUS, true);
        rangeS = RangeFactory.create(noDataS, true, noDataS, true);
        rangeI = RangeFactory.create(noDataI, true, noDataI, true);
        // Destination No Data
        destinationNoDataValue = 255;
    }

    // No ROI tested; NoData not present
    @Test
    public void testByteToAllTypes() {
        boolean roiUsed = false;
        boolean noDataPresent = false;
        boolean useRoiAccessor = false;
        int dataType = DataBuffer.TYPE_BYTE;
        TestSelection selector = TestSelection.NO_ROI_ONLY_DATA;

        testOperation(testImageByte, byteToByteTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageByte, byteToUshortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageByte, byteToShortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageByte, byteToIntTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageByte, byteToFloatTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageByte, byteToDoubleTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);
    }

    @Test
    public void testUshortToAllTypes() {
        boolean roiUsed = false;
        boolean noDataPresent = false;
        boolean useRoiAccessor = false;
        int dataType = DataBuffer.TYPE_USHORT;
        TestSelection selector = TestSelection.NO_ROI_ONLY_DATA;

        testOperation(testImageUShort, ushortToByteTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageUShort, ushortToUshortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageUShort, ushortToShortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageUShort, ushortToIntTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageUShort, ushortToFloatTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageUShort, ushortToDoubleTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);
    }

    @Test
    public void testShortToAllTypes() {
        boolean roiUsed = false;
        boolean noDataPresent = false;
        boolean useRoiAccessor = false;
        int dataType = DataBuffer.TYPE_SHORT;
        TestSelection selector = TestSelection.NO_ROI_ONLY_DATA;

        testOperation(testImageShort, shortToByteTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageShort, shortToUshortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageShort, shortToShortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageShort, shortToIntTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageShort, shortToFloatTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageShort, shortToDoubleTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);
    }

    @Test
    public void testIntToAllTypes() {
        boolean roiUsed = false;
        boolean noDataPresent = false;
        boolean useRoiAccessor = false;
        int dataType = DataBuffer.TYPE_INT;
        TestSelection selector = TestSelection.NO_ROI_ONLY_DATA;

        testOperation(testImageInt, intToByteTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageInt, intToUshortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageInt, intToShortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageInt, intToIntTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageInt, intToFloatTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageInt, intToDoubleTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);
    }

    // ROI tested (RoiAccessor not used); NoData not present
    @Test
    public void testByteToAllTypesROIBounds() {
        boolean roiUsed = true;
        boolean noDataPresent = false;
        boolean useRoiAccessor = false;
        int dataType = DataBuffer.TYPE_BYTE;
        TestSelection selector = TestSelection.ROI_ONLY_DATA;

        testOperation(testImageByte, byteToByteTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageByte, byteToUshortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageByte, byteToShortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageByte, byteToIntTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageByte, byteToFloatTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageByte, byteToDoubleTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);
    }

    @Test
    public void testUshortToAllTypesROIBounds() {
        boolean roiUsed = true;
        boolean noDataPresent = false;
        boolean useRoiAccessor = false;
        int dataType = DataBuffer.TYPE_USHORT;
        TestSelection selector = TestSelection.ROI_ONLY_DATA;

        testOperation(testImageUShort, ushortToByteTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageUShort, ushortToUshortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageUShort, ushortToShortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageUShort, ushortToIntTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageUShort, ushortToFloatTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageUShort, ushortToDoubleTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);
    }

    @Test
    public void testShortToAllTypesROIBounds() {
        boolean roiUsed = true;
        boolean noDataPresent = false;
        boolean useRoiAccessor = false;
        int dataType = DataBuffer.TYPE_SHORT;
        TestSelection selector = TestSelection.ROI_ONLY_DATA;

        testOperation(testImageShort, shortToByteTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageShort, shortToUshortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageShort, shortToShortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageShort, shortToIntTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageShort, shortToFloatTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageShort, shortToDoubleTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);
    }

    @Test
    public void testIntToAllTypesROIBounds() {
        boolean roiUsed = true;
        boolean noDataPresent = false;
        boolean useRoiAccessor = false;
        int dataType = DataBuffer.TYPE_INT;
        TestSelection selector = TestSelection.ROI_ONLY_DATA;

        testOperation(testImageInt, intToByteTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageInt, intToUshortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageInt, intToShortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageInt, intToIntTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageInt, intToFloatTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageInt, intToDoubleTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);
    }

    // ROI tested (RoiAccessor used); NoData not present
    @Test
    public void testByteToAllTypesROIAccessor() {
        boolean roiUsed = true;
        boolean noDataPresent = false;
        boolean useRoiAccessor = true;
        int dataType = DataBuffer.TYPE_BYTE;
        TestSelection selector = TestSelection.ROI_ACCESSOR_ONLY_DATA;

        testOperation(testImageByte, byteToByteTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageByte, byteToUshortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageByte, byteToShortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageByte, byteToIntTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageByte, byteToFloatTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageByte, byteToDoubleTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);
    }

    @Test
    public void testUshortToAllTypesROIAccessor() {
        boolean roiUsed = true;
        boolean noDataPresent = false;
        boolean useRoiAccessor = true;
        int dataType = DataBuffer.TYPE_USHORT;
        TestSelection selector = TestSelection.ROI_ACCESSOR_ONLY_DATA;

        testOperation(testImageUShort, ushortToByteTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageUShort, ushortToUshortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageUShort, ushortToShortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageUShort, ushortToIntTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageUShort, ushortToFloatTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageUShort, ushortToDoubleTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);
    }

    @Test
    public void testShortToAllTypesROIAccessor() {
        boolean roiUsed = true;
        boolean noDataPresent = false;
        boolean useRoiAccessor = true;
        int dataType = DataBuffer.TYPE_SHORT;
        TestSelection selector = TestSelection.ROI_ACCESSOR_ONLY_DATA;

        testOperation(testImageShort, shortToByteTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageShort, shortToUshortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageShort, shortToShortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageShort, shortToIntTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageShort, shortToFloatTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageShort, shortToDoubleTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);
    }

    @Test
    public void testIntToAllTypesROIAccessor() {
        boolean roiUsed = true;
        boolean noDataPresent = false;
        boolean useRoiAccessor = true;
        int dataType = DataBuffer.TYPE_INT;
        TestSelection selector = TestSelection.ROI_ACCESSOR_ONLY_DATA;

        testOperation(testImageInt, intToByteTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageInt, intToUshortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageInt, intToShortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageInt, intToIntTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageInt, intToFloatTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageInt, intToDoubleTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);
    }

    // NoData tested; ROI not present
    @Test
    public void testByteToAllTypesNoData() {
        boolean roiUsed = false;
        boolean noDataPresent = true;
        boolean useRoiAccessor = false;
        int dataType = DataBuffer.TYPE_BYTE;
        TestSelection selector = TestSelection.NO_ROI_NO_DATA;

        testOperation(testImageByte, byteToByteTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageByte, byteToUshortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageByte, byteToShortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageByte, byteToIntTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageByte, byteToFloatTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageByte, byteToDoubleTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);
    }

    @Test
    public void testUshortToAllTypesNoData() {
        boolean roiUsed = false;
        boolean noDataPresent = true;
        boolean useRoiAccessor = false;
        int dataType = DataBuffer.TYPE_USHORT;
        TestSelection selector = TestSelection.NO_ROI_NO_DATA;

        testOperation(testImageUShort, ushortToByteTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageUShort, ushortToUshortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageUShort, ushortToShortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageUShort, ushortToIntTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageUShort, ushortToFloatTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageUShort, ushortToDoubleTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);
    }

    @Test
    public void testShortToAllTypesNoData() {
        boolean roiUsed = false;
        boolean noDataPresent = true;
        boolean useRoiAccessor = false;
        int dataType = DataBuffer.TYPE_SHORT;
        TestSelection selector = TestSelection.NO_ROI_NO_DATA;

        testOperation(testImageShort, shortToByteTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageShort, shortToUshortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageShort, shortToShortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageShort, shortToIntTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageShort, shortToFloatTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageShort, shortToDoubleTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);
    }

    @Test
    public void testIntToAllTypesNoData() {
        boolean roiUsed = false;
        boolean noDataPresent = true;
        boolean useRoiAccessor = false;
        int dataType = DataBuffer.TYPE_INT;
        TestSelection selector = TestSelection.NO_ROI_NO_DATA;

        testOperation(testImageInt, intToByteTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageInt, intToUshortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageInt, intToShortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageInt, intToIntTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageInt, intToFloatTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageInt, intToDoubleTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);
    }

    // NoData tested; ROI present (RoiAccessor used)
    @Test
    public void testByteToAllTypesFull() {
        boolean roiUsed = true;
        boolean noDataPresent = true;
        boolean useRoiAccessor = true;
        int dataType = DataBuffer.TYPE_BYTE;
        TestSelection selector = TestSelection.ROI_ACCESSOR_NO_DATA;

        testOperation(testImageByte, byteToByteTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageByte, byteToUshortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageByte, byteToShortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageByte, byteToIntTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageByte, byteToFloatTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageByte, byteToDoubleTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);
    }

    @Test
    public void testUshortToAllTypesFull() {
        boolean roiUsed = true;
        boolean noDataPresent = true;
        boolean useRoiAccessor = true;
        int dataType = DataBuffer.TYPE_USHORT;
        TestSelection selector = TestSelection.ROI_ACCESSOR_NO_DATA;

        testOperation(testImageUShort, ushortToByteTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageUShort, ushortToUshortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageUShort, ushortToShortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageUShort, ushortToIntTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageUShort, ushortToFloatTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageUShort, ushortToDoubleTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);
    }

    @Test
    public void testShortToAllTypesFull() {
        boolean roiUsed = true;
        boolean noDataPresent = true;
        boolean useRoiAccessor = true;
        int dataType = DataBuffer.TYPE_SHORT;
        TestSelection selector = TestSelection.ROI_ACCESSOR_NO_DATA;

        testOperation(testImageShort, shortToByteTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageShort, shortToUshortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageShort, shortToShortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageShort, shortToIntTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageShort, shortToFloatTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageShort, shortToDoubleTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);
    }

    @Test
    public void testIntToAllTypesFull() {
        boolean roiUsed = true;
        boolean noDataPresent = true;
        boolean useRoiAccessor = true;
        int dataType = DataBuffer.TYPE_INT;
        TestSelection selector = TestSelection.ROI_ACCESSOR_NO_DATA;

        testOperation(testImageInt, intToByteTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageInt, intToUshortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageInt, intToShortTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageInt, intToIntTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageInt, intToFloatTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);

        testOperation(testImageInt, intToDoubleTable, roiUsed, noDataPresent, useRoiAccessor,
                dataType, selector);
    }

    // This method is the general method used by all the test
    public void testOperation(RenderedImage img, LookupTable table, boolean roiUsed,
            boolean noDataUsed, boolean useRoiAccessor, int dataTypeInput, TestSelection selector) {

        // ROI data is added only if the roiUsed parameter is set to true
        ROI roiData = null;

        if (roiUsed) {
            roiData = roi;
        }
        // No Data Range data is added only if the noDataUsed parameter is set to true
        Range noDataRange = null;

        if (noDataUsed) {
            switch (dataTypeInput) {
            case DataBuffer.TYPE_BYTE:
                noDataRange = rangeB;
                break;
            case DataBuffer.TYPE_USHORT:
                noDataRange = rangeUS;
                break;
            case DataBuffer.TYPE_SHORT:
                noDataRange = rangeS;
                break;
            case DataBuffer.TYPE_INT:
                noDataRange = rangeI;
                break;
            default:
                throw new IllegalArgumentException("Wrong data type");
            }
        }
        // LookupDescriptor creation
        PlanarImage destinationIMG = LookupDescriptor.create(img, table, destinationNoDataValue,
                roiData, noDataRange, useRoiAccessor, null);

        if (INTERACTIVE && table.getDataType() == DataBuffer.TYPE_BYTE
                && TEST_SELECTOR == selector.getType() && dataTypeInput == DataBuffer.TYPE_BYTE) {
            // The image is shown to the screen
            RenderedImageBrowser.showChain(destinationIMG, false, roiUsed);
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Calculates all the image tiles
            destinationIMG.getTiles();
        }

        //DataType Test
        assertEquals(table.getDataType(),destinationIMG.getSampleModel().getDataType());
        
        // ROI test
        if (roiUsed) {
            // Selection of a tile inside the ROI
            Rectangle roiBounds = roi.getBounds();
            // Last ROI coordinates
            int roiMaxPosX = roiBounds.x + roiBounds.width - 1;
            int roiMaxPosY = roiBounds.y + roiBounds.height - 1;
            // Tile coordinates associated to these coordinates
            int tileX = destinationIMG.XToTileX(roiMaxPosX);
            int tileY = destinationIMG.YToTileY(roiMaxPosY);
            // Tile inside the ROI
            Raster roiTile = destinationIMG.getTile(tileX, tileY);
            // Tile data type
            int dataTypeROI = roiTile.getSampleModel().getDataType();

            // Control that a pixel is not a destination No Data
            switch (dataTypeROI) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_INT:
                int value = roiTile.getSample(roiTile.getMinX() + 2, roiTile.getMinY() + 1, 0);
                assertFalse(value == (int) destinationNoDataValue);
                break;
            case DataBuffer.TYPE_FLOAT:
                float valuef = roiTile.getSampleFloat(roiTile.getMinX() + 2, roiTile.getMinY() + 1,
                        0);
                assertFalse(valuef == (float) destinationNoDataValue);
                break;
            case DataBuffer.TYPE_DOUBLE:
                double valued = roiTile.getSampleDouble(roiTile.getMinX() + 2,
                        roiTile.getMinY() + 1, 0);

                assertFalse(valued == destinationNoDataValue);
                break;
            default:
                throw new IllegalArgumentException("Wrong data type");
            }
        }

        // Check minimum and maximum value for a tile
        // Selection of the upper left tile
        Raster simpleTile = destinationIMG.getTile(destinationIMG.getMinTileX(),
                destinationIMG.getMinTileY());
        // Tile dimensions
        int tileMinX = simpleTile.getMinX();
        int tileMinY = simpleTile.getMinY();
        int tileWidth = simpleTile.getWidth();
        int tileHeight = simpleTile.getHeight();
        // Tile dataType
        int dataType = simpleTile.getSampleModel().getDataType();
        // Search for the maximum and minimum value inside all the tile and then control
        // if they are equal.
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_INT:
            int minValue = Integer.MAX_VALUE;
            int maxValue = Integer.MIN_VALUE;

            for (int i = tileMinY; i < tileHeight + tileMinY; i++) {
                for (int j = tileMinX; j < tileWidth + tileMinX; j++) {
                    int value = simpleTile.getSample(j, i, 0);
                    if (value > maxValue) {
                        maxValue = value;
                    }

                    if (value < minValue) {
                        minValue = value;
                    }
                }
            }
            // Check if the values are not max and minimum value
            assertFalse(minValue == maxValue);
            assertFalse(minValue == Integer.MAX_VALUE);
            assertFalse(maxValue == Integer.MIN_VALUE);
            break;
        case DataBuffer.TYPE_FLOAT:
            float minValuef = Float.MAX_VALUE;
            float maxValuef = -Float.MAX_VALUE;

            for (int i = 0; i < tileHeight; i++) {
                for (int j = 0; j < tileWidth; j++) {
                    float valuef = simpleTile.getSampleFloat(j, i, 0);

                    if (Float.isNaN(valuef) || valuef == Float.POSITIVE_INFINITY
                            || valuef == Float.POSITIVE_INFINITY) {
                        valuef = 255;
                    }

                    if (valuef > maxValuef) {
                        maxValuef = valuef;
                    }

                    if (valuef < minValuef) {
                        minValuef = valuef;
                    }
                }
            }
            // Check if the values are not max and minimum value
            assertFalse(minValuef == maxValuef);
            assertFalse(minValuef == Float.MAX_VALUE);
            assertFalse(maxValuef == -Float.MAX_VALUE);
            break;
        case DataBuffer.TYPE_DOUBLE:
            double minValued = Double.MAX_VALUE;
            double maxValued = -Double.MAX_VALUE;

            for (int i = 0; i < tileHeight; i++) {
                for (int j = 0; j < tileWidth; j++) {
                    double valued = simpleTile.getSampleDouble(j, i, 0);

                    if (Double.isNaN(valued) || valued == Double.POSITIVE_INFINITY
                            || valued == Double.POSITIVE_INFINITY) {
                        valued = 255;
                    }

                    if (valued > maxValued) {
                        maxValued = valued;
                    }

                    if (valued < minValued) {
                        minValued = valued;
                    }
                }
            }
            // Check if the values are not max and minimum value
            assertFalse(minValued == maxValued);
            assertFalse(minValued == Double.MAX_VALUE);
            assertFalse(maxValued == -Double.MAX_VALUE);
            break;
        default:
            throw new IllegalArgumentException("Wrong data type");
        }
        
        //Final Image disposal
        if(destinationIMG instanceof RenderedOp){
            ((RenderedOp)destinationIMG).dispose();
        }
        
        
    }
}
