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

import static org.junit.Assert.*;

import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;

import javax.media.jai.BorderExtender;
import javax.media.jai.RenderedOp;

import org.junit.BeforeClass;
import org.junit.Test;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;
import it.geosolutions.rendered.viewer.RenderedImageBrowser;

/**
 * This test is used for checking if the Border operation is performed correctly. The tests are executed for all the JAI allowed data types, with and
 * withou No Data and with one of these BorderExtender objects:
 * <ul>
 * <li>BorderExtenderZero</li>
 * <li>BorderExtenderCopy</li>
 * <li>BorderExtenderReflect</li>
 * <li>BorderExtenderWrap</li>
 * </ul>
 * 
 * If the user wants to see the result of the Border operation must set the following JVM parameters:
 * <ul>
 * <li>JAI.Ext.Interactive=true</li>
 * <li>JAI.Ext.RangeUsed=true(for showing the calculation with NoData, false otherwise)</li>
 * <li>JAI.Ext.TestSelector=0/1/2/3(Each value is associated with one of the 4 possible BorderExtender types)</li>
 * </ul>
 * 
 * @author geosolutions
 * 
 */
public class BorderTest extends TestBase {

    /** Tolerance value used for double comparison */
    private static final double TOLERANCE = 0.1d;

    /** Boolean indicating if No Data calculation must be showed(useful only if INTERACTIVE value is set to true) */
    public static boolean RANGE_USED = Boolean.getBoolean("JAI.Ext.RangeUsed");

    /** Input test images */
    private static RenderedImage[] sourceIMG;

    /** Range associated with the Byte image */
    private static Range noDataByte;

    /** Range associated with the UShort image */
    private static Range noDataUShort;

    /** Range associated with the Short image */
    private static Range noDataShort;

    /** Range associated with the Integer image */
    private static Range noDataInt;

    /** Range associated with the Float image */
    private static Range noDataFloat;

    /** Range associated with the Double image */
    private static Range noDataDouble;

    /** BorderExtender array */
    private static BorderExtender[] extender;

    /** Output value for the No Data */
    private static double destNoData;

    /** Left padding parameter */
    private static int leftPad;

    /** Right padding parameter */
    private static int rightPad;

    /** Top padding parameter */
    private static int topPad;

    /** Bottom padding parameter */
    private static int bottomPad;

    @BeforeClass
    public static void initialSetup() {

        // No Data values
        byte noDataB = 50;
        short noDataU = 50;
        short noDataS = 50;
        int noDataI = 50;
        float noDataF = 50f;
        double noDataD = 50d;
        // Range parameters
        boolean minIncluded = true;
        boolean maxIncluded = true;
        boolean nanIncluded = true;

        noDataByte = RangeFactory.create(noDataB, minIncluded, noDataB, maxIncluded);
        noDataUShort = RangeFactory.createU(noDataU, minIncluded, noDataU, maxIncluded);
        noDataShort = RangeFactory.create(noDataS, minIncluded, noDataS, maxIncluded);
        noDataInt = RangeFactory.create(noDataI, minIncluded, noDataI, maxIncluded);
        noDataFloat = RangeFactory.create(noDataF, minIncluded, noDataF, maxIncluded, nanIncluded);
        noDataDouble = RangeFactory.create(noDataD, minIncluded, noDataD, maxIncluded, nanIncluded);

        // Image array creation
        sourceIMG = new RenderedImage[DataBuffer.TYPE_DOUBLE + 1];

        // Image filler parameter must be set to true for creating images without only few values different from 0
        IMAGE_FILLER = true;

        sourceIMG[DataBuffer.TYPE_BYTE] = createTestImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataB, false, 1);
        sourceIMG[DataBuffer.TYPE_USHORT] = createTestImage(DataBuffer.TYPE_USHORT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataU, false, 1);
        sourceIMG[DataBuffer.TYPE_SHORT] = createTestImage(DataBuffer.TYPE_SHORT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataS, false, 1);
        sourceIMG[DataBuffer.TYPE_INT] = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataI, false, 1);
        sourceIMG[DataBuffer.TYPE_FLOAT] = createTestImage(DataBuffer.TYPE_FLOAT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataF, false, 1);
        sourceIMG[DataBuffer.TYPE_DOUBLE] = createTestImage(DataBuffer.TYPE_DOUBLE, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataD, false, 1);

        IMAGE_FILLER = false;

        // Creation of the BorderExtender array
        extender = new BorderExtender[BorderExtender.BORDER_WRAP + 1];

        extender[BorderExtender.BORDER_ZERO] = BorderExtender
                .createInstance(BorderExtender.BORDER_ZERO);
        extender[BorderExtender.BORDER_COPY] = BorderExtender
                .createInstance(BorderExtender.BORDER_COPY);
        extender[BorderExtender.BORDER_REFLECT] = BorderExtender
                .createInstance(BorderExtender.BORDER_REFLECT);
        extender[BorderExtender.BORDER_WRAP] = BorderExtender
                .createInstance(BorderExtender.BORDER_WRAP);

        // Destination NoData value
        destNoData = 100d;

        // Padding values
        leftPad = 2;
        rightPad = 2;
        topPad = 2;
        bottomPad = 2;

    }

    // This method tests the Border operation using the BorderExtenderZero with and without NoData
    @Test
    public void testBorderZero() {

        boolean noDataRangeUsed = false;
        int borderType = BorderExtender.BORDER_ZERO;

        testBorder(DataBuffer.TYPE_BYTE, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_USHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_SHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_INT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_FLOAT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_DOUBLE, noDataRangeUsed, borderType);

        noDataRangeUsed = true;
        testBorder(DataBuffer.TYPE_BYTE, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_USHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_SHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_INT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_FLOAT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_DOUBLE, noDataRangeUsed, borderType);
    }

    // This method tests the Border operation using the BorderExtenderCopy with and without NoData
    @Test
    public void testBorderCopy() {

        boolean noDataRangeUsed = false;
        int borderType = BorderExtender.BORDER_COPY;

        testBorder(DataBuffer.TYPE_BYTE, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_USHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_SHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_INT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_FLOAT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_DOUBLE, noDataRangeUsed, borderType);

        noDataRangeUsed = true;
        testBorder(DataBuffer.TYPE_BYTE, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_USHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_SHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_INT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_FLOAT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_DOUBLE, noDataRangeUsed, borderType);
    }

    // This method tests the Border operation using the BorderExtenderReflect with and without NoData
    @Test
    public void testBorderReflect() {

        boolean noDataRangeUsed = false;
        int borderType = BorderExtender.BORDER_REFLECT;

        testBorder(DataBuffer.TYPE_BYTE, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_USHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_SHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_INT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_FLOAT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_DOUBLE, noDataRangeUsed, borderType);

        noDataRangeUsed = true;
        testBorder(DataBuffer.TYPE_BYTE, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_USHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_SHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_INT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_FLOAT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_DOUBLE, noDataRangeUsed, borderType);
    }

    // This method tests the Border operation using the BorderExtenderWrap with and without NoData
    @Test
    public void testBorderWrap() {

        boolean noDataRangeUsed = false;
        int borderType = BorderExtender.BORDER_WRAP;

        testBorder(DataBuffer.TYPE_BYTE, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_USHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_SHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_INT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_FLOAT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_DOUBLE, noDataRangeUsed, borderType);

        noDataRangeUsed = true;
        testBorder(DataBuffer.TYPE_BYTE, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_USHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_SHORT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_INT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_FLOAT, noDataRangeUsed, borderType);
        testBorder(DataBuffer.TYPE_DOUBLE, noDataRangeUsed, borderType);
    }

    private void testBorder(int dataType, boolean noDataRangeUsed, int borderType) {
        // Selection of the source
        RenderedImage source = sourceIMG[dataType];
        // Selection of the BorderExtender
        BorderExtender extend = extender[borderType];

        // The precalculated NoData Range is used, if selected by the related boolean.
        Range noDataRange;

        if (noDataRangeUsed) {
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                noDataRange = noDataByte;
                break;
            case DataBuffer.TYPE_USHORT:
                noDataRange = noDataUShort;
                break;
            case DataBuffer.TYPE_SHORT:
                noDataRange = noDataShort;
                break;
            case DataBuffer.TYPE_INT:
                noDataRange = noDataInt;
                break;
            case DataBuffer.TYPE_FLOAT:
                noDataRange = noDataFloat;
                break;
            case DataBuffer.TYPE_DOUBLE:
                noDataRange = noDataDouble;
                break;
            default:
                throw new IllegalArgumentException("Wrong data type");
            }
        } else {
            noDataRange = null;
        }
        // Border operation execution
        RenderedOp borderIMG = BorderDescriptor.create(source, leftPad, rightPad, topPad,
                bottomPad, extend, noDataRange, destNoData, null);

        // If the parameters are set, the image is printed to the screen
        if (INTERACTIVE && dataType == DataBuffer.TYPE_BYTE && TEST_SELECTOR == borderType
                && noDataRangeUsed == RANGE_USED) {
            RenderedImageBrowser.showChain(borderIMG, false, false);

            try {
                System.in.read();
            } catch (IOException e) {

            }
        } else {
            // Else all the image tiles are calculated
            borderIMG.getTiles();
        }

        // Calculation of all the variables for iterating on the image borders
        int minX = borderIMG.getMinX();
        int minY = borderIMG.getMinY();

        int maxXPadding = minX + leftPad;
        int maxYPadding = minY + topPad;

        int maxX = borderIMG.getMaxX();
        int maxY = borderIMG.getMaxY();

        int width = DEFAULT_WIDTH / 2;
        int height = DEFAULT_HEIGHT / 2;

        int widthReduced = width - rightPad;
        int heightReduced = height - bottomPad;

        int topPaddingBorder = topPad + minY;
        int leftPaddingBorder = leftPad + minX;

        int xIndex = 0;
        int yIndex = 0;

        int tileX = 0;
        int tileY = 0;

        int xWrap = 0;
        int yWrap = 0;

        // Test for the top padding
        for (int x = minX; x < width; x++) {

            tileX = borderIMG.XToTileX(x);

            if (x > leftPad && x < widthReduced) {

                for (int y = minY; y < topPaddingBorder; y++) {

                    tileY = borderIMG.YToTileY(y);

                    Raster tile = borderIMG.getTile(tileX, tileY);

                    double value = tile.getSampleDouble(x, y, 0);

                    switch (borderType) {
                    case BorderExtender.BORDER_ZERO:
                        assertEquals(value, 0, TOLERANCE);
                        break;
                    case BorderExtender.BORDER_COPY:
                        double valueCopy = tile.getSampleDouble(x, maxYPadding, 0);
                        if (noDataRangeUsed) {
                            if (noDataDouble.contains(valueCopy)) {
                                assertEquals(value, destNoData, TOLERANCE);
                            } else {
                                assertEquals(value, valueCopy, TOLERANCE);
                            }
                        } else {
                            assertEquals(value, valueCopy, TOLERANCE);
                        }
                        break;
                    case BorderExtender.BORDER_REFLECT:
                        yIndex = y - minY;
                        double valueReflect = tile.getSampleDouble(x, (topPad - yIndex - 1), 0);
                        if (noDataRangeUsed) {
                            if (noDataDouble.contains(valueReflect)) {
                                assertEquals(value, destNoData, TOLERANCE);
                            } else {
                                assertEquals(value, valueReflect, TOLERANCE);
                            }
                        } else {
                            assertEquals(value, valueReflect, TOLERANCE);
                        }
                        break;
                    case BorderExtender.BORDER_WRAP:
                        yIndex = topPad - (y - minY);

                        yWrap = maxY - bottomPad - yIndex;

                        tileY = borderIMG.YToTileY(yWrap);

                        Raster tileWrap = borderIMG.getTile(tileX, tileY);

                        double valueWrap = tileWrap.getSampleDouble(x, yWrap, 0);
                        if (noDataRangeUsed) {
                            if (noDataDouble.contains(valueWrap)) {
                                assertEquals(value, destNoData, TOLERANCE);
                            } else {
                                assertEquals(value, valueWrap, TOLERANCE);
                            }
                        } else {
                            assertEquals(value, valueWrap, TOLERANCE);
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Wrong BorderExtender type");
                    }
                }
            }
        }

        // Test for the left padding
        for (int y = minY; y < height; y++) {

            tileY = borderIMG.YToTileY(y);

            if (y > topPad && y < heightReduced) {

                for (int x = minX; x < leftPaddingBorder; x++) {

                    tileX = borderIMG.XToTileX(x);

                    Raster tile = borderIMG.getTile(tileX, tileY);

                    double value = tile.getSampleDouble(x, y, 0);

                    switch (borderType) {
                    case BorderExtender.BORDER_ZERO:
                        assertEquals(value, 0, TOLERANCE);
                        break;
                    case BorderExtender.BORDER_COPY:
                        double valueCopy = tile.getSampleDouble(maxXPadding, y, 0);
                        if (noDataRangeUsed) {
                            if (noDataDouble.contains(valueCopy)) {
                                assertEquals(value, destNoData, TOLERANCE);
                            } else {
                                assertEquals(value, valueCopy, TOLERANCE);
                            }
                        } else {
                            assertEquals(value, valueCopy, TOLERANCE);
                        }
                        break;
                    case BorderExtender.BORDER_REFLECT:
                        xIndex = x - minX;
                        double valueReflect = tile.getSampleDouble((leftPad - xIndex - 1), y, 0);
                        if (noDataRangeUsed) {
                            if (noDataDouble.contains(valueReflect)) {
                                assertEquals(value, destNoData, TOLERANCE);
                            } else {
                                assertEquals(value, valueReflect, TOLERANCE);
                            }
                        } else {
                            assertEquals(value, valueReflect, TOLERANCE);
                        }
                        break;
                    case BorderExtender.BORDER_WRAP:
                        xIndex = leftPad - (x - minX);

                        xWrap = maxX - rightPad - xIndex;

                        tileX = borderIMG.XToTileX(xWrap);

                        Raster tileWrap = borderIMG.getTile(tileX, tileY);

                        double valueWrap = tileWrap.getSampleDouble(xWrap, y, 0);
                        if (noDataRangeUsed) {
                            if (noDataDouble.contains(valueWrap)) {
                                assertEquals(value, destNoData, TOLERANCE);
                            } else {
                                assertEquals(value, valueWrap, TOLERANCE);
                            }
                        } else {
                            assertEquals(value, valueWrap, TOLERANCE);
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Wrong BorderExtender type");
                    }
                }
            }
        }
    }
}
