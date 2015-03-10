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

import static org.junit.Assert.assertEquals;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;

import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.media.jai.util.ImageUtil;

import it.geosolutions.jaiext.algebra.AlgebraDescriptor.Operator;
import it.geosolutions.jaiext.algebra.constant.OperationConstDescriptor;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;

public class OperationConstTest extends TestBase {

    private static final int NUM_TYPES = 6;

    private static final int DEFAULT_WIDTH_REDUCED = DEFAULT_WIDTH / 2;

    private static final int DEFAULT_HEIGHT_REDUCED = DEFAULT_HEIGHT / 2;

    private static final double TOLERANCE = 0.1d;

    private static RenderedImage[] testImages;

    private static Range noDataByte;

    private static Range noDataUShort;

    private static Range noDataShort;

    private static Range noDataInt;

    private static Range noDataFloat;

    private static Range noDataDouble;

    private static int destNoData;

    private static ROI roiObject;

    private static double[] doubleConsts;

    @BeforeClass
    public static void initialSetup() {

        byte noDataB = 50;
        short noDataS = 50;
        int noDataI = 50;
        float noDataF = 50;
        double noDataD = 50;

        testImages = new RenderedImage[NUM_TYPES];

        IMAGE_FILLER = true;
        testImages[DataBuffer.TYPE_BYTE] = createTestImage(DataBuffer.TYPE_BYTE,
                DEFAULT_WIDTH_REDUCED, DEFAULT_HEIGHT_REDUCED, noDataB, false, 3);
        testImages[DataBuffer.TYPE_USHORT] = createTestImage(DataBuffer.TYPE_USHORT,
                DEFAULT_WIDTH_REDUCED, DEFAULT_HEIGHT_REDUCED, noDataS, false, 3);
        testImages[DataBuffer.TYPE_SHORT] = createTestImage(DataBuffer.TYPE_SHORT,
                DEFAULT_WIDTH_REDUCED, DEFAULT_HEIGHT_REDUCED, noDataS, false, 3);
        testImages[DataBuffer.TYPE_INT] = createTestImage(DataBuffer.TYPE_INT,
                DEFAULT_WIDTH_REDUCED, DEFAULT_HEIGHT_REDUCED, noDataI, false, 3);
        testImages[DataBuffer.TYPE_FLOAT] = createTestImage(DataBuffer.TYPE_FLOAT,
                DEFAULT_WIDTH_REDUCED, DEFAULT_HEIGHT_REDUCED, noDataF, false, 3);
        testImages[DataBuffer.TYPE_DOUBLE] = createTestImage(DataBuffer.TYPE_DOUBLE,
                DEFAULT_WIDTH_REDUCED, DEFAULT_HEIGHT_REDUCED, noDataD, false, 3);
        IMAGE_FILLER = false;

        // No Data Ranges
        boolean minIncluded = true;
        boolean maxIncluded = true;

        noDataByte = RangeFactory.create(noDataB, minIncluded, noDataB, maxIncluded);
        noDataUShort = RangeFactory.createU(noDataS, minIncluded, noDataS, maxIncluded);
        noDataShort = RangeFactory.create(noDataS, minIncluded, noDataS, maxIncluded);
        noDataInt = RangeFactory.create(noDataI, minIncluded, noDataI, maxIncluded);
        noDataFloat = RangeFactory.create(noDataF, minIncluded, noDataF, maxIncluded, true);
        noDataDouble = RangeFactory.create(noDataD, minIncluded, noDataD, maxIncluded, true);

        doubleConsts = new double[] { 10.5, 15.5, 20.5 };

        // Destination No Data
        destNoData = 100;

        // ROI creation
        Rectangle roiBounds = new Rectangle(5, 5, DEFAULT_WIDTH_REDUCED / 4,
                DEFAULT_HEIGHT_REDUCED / 4);
        roiObject = new ROIShape(roiBounds);
    }

    @Test
    public void testNoROINoNoData() {

        boolean roiUsed = false;
        boolean noDataUsed = false;

        for (int i = 0; i < 6; i++) {
            runTests(i, noDataUsed, roiUsed);
        }
    }

    @Test
    public void testOnlyNoData() {

        boolean roiUsed = false;
        boolean noDataUsed = true;

        for (int i = 0; i < 6; i++) {
            runTests(i, noDataUsed, roiUsed);
        }
    }

    @Test
    public void testOnlyROI() {

        boolean roiUsed = true;
        boolean noDataUsed = false;

        for (int i = 0; i < 6; i++) {
            runTests(i, noDataUsed, roiUsed);
        }
    }

    @Test
    public void testROIAndNoData() {

        boolean roiUsed = true;
        boolean noDataUsed = true;

        for (int i = 0; i < 6; i++) {
            runTests(i, noDataUsed, roiUsed);
        }
    }

    private void runTests(int dataType, boolean noDataUsed, boolean roiUsed) {
        testOperation(testImages[dataType], Operator.SUM, noDataUsed, roiUsed);
        testOperation(testImages[dataType], Operator.SUBTRACT, noDataUsed, roiUsed);
        testOperation(testImages[dataType], Operator.MULTIPLY, noDataUsed, roiUsed);
        testOperation(testImages[dataType], Operator.DIVIDE, noDataUsed, roiUsed);
        testOperation(testImages[dataType], Operator.DIVIDE_INTO, noDataUsed, roiUsed);
        if (dataType != DataBuffer.TYPE_FLOAT && dataType != DataBuffer.TYPE_DOUBLE) {
            testOperation(testImages[dataType], Operator.AND, noDataUsed, roiUsed);
            testOperation(testImages[dataType], Operator.OR, noDataUsed, roiUsed);
            testOperation(testImages[dataType], Operator.XOR, noDataUsed, roiUsed);
        }
    }

    private void testOperation(RenderedImage source, Operator op, boolean noDataUsed,
            boolean roiUsed) {
        // Optional No Data Range used
        Range noData;
        // Source image data type
        int dataType = source.getSampleModel().getDataType();
        // If no Data are present, the No Data Range associated is used
        if (noDataUsed) {

            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                noData = noDataByte;
                break;
            case DataBuffer.TYPE_USHORT:
                noData = noDataUShort;
                break;
            case DataBuffer.TYPE_SHORT:
                noData = noDataShort;
                break;
            case DataBuffer.TYPE_INT:
                noData = noDataInt;
                break;
            case DataBuffer.TYPE_FLOAT:
                noData = noDataFloat;
                break;
            case DataBuffer.TYPE_DOUBLE:
                noData = noDataDouble;
                break;
            default:
                throw new IllegalArgumentException("Wrong data type");
            }
        } else {
            noData = null;
        }

        ROI roi;

        if (roiUsed) {
            roi = roiObject;
        } else {
            roi = null;
        }

        // operation
        RenderedOp calculated = OperationConstDescriptor.create(source, doubleConsts, op, roi,
                noData, destNoData, null);
        // Check
        testOperation(calculated, source, roi, noData, op);

        // Disposal of the output image
        calculated.dispose();
    }

    private void testOperation(RenderedOp calculated, RenderedImage source, ROI roi, Range noData,
            Operator op) {

        boolean roiUsed = roi != null;
        boolean noDataUsed = noData != null;

        // Upper-Left tile indexes
        int minTileX = calculated.getMinTileX();
        int minTileY = calculated.getMinTileY();
        // Raster object
        Raster upperLeftTile = calculated.getTile(minTileX, minTileY);
        // Tile bounds
        int minX = upperLeftTile.getMinX();
        int minY = upperLeftTile.getMinY();
        int maxX = upperLeftTile.getWidth() + minX;
        int maxY = upperLeftTile.getHeight() + minY;
        // Source Raster
        Raster sourceRaster = source.getTile(minTileX, minTileY);

        // Old band value
        double valueOld = 0;

        double value = 0;

        double sample = 0;

        boolean isValidData = false;
        
        boolean supportsDouble = op.isDataTypeSupported(DataBuffer.TYPE_DOUBLE);

        int dataType = calculated.getSampleModel().getDataType();
        int numBands = calculated.getSampleModel().getNumBands();

        // Cycle on all the tile Bands
        for (int b = 0; b < numBands; b++) {
            // Cycle on the y-axis
            for (int x = minX; x < maxX; x++) {
                // Cycle on the x-axis
                for (int y = minY; y < maxY; y++) {
                    // Calculated value
                    value = upperLeftTile.getSampleDouble(x, y, b);
                    sample = sourceRaster.getSampleDouble(x, y, b);

                    valueOld = 0;

                    isValidData = (!roiUsed || roiUsed && roi.contains(x, y))
                            && (!noDataUsed || (noDataUsed && !noDataDouble.contains(sample)));

                    if (isValidData) {
                        if(supportsDouble){
                            valueOld = op.calculate(sample, doubleConsts[b]);
                        } else {
                            valueOld = op.calculate((int)sample, ImageUtil.clampRoundInt(doubleConsts[b]));
                        }
                        
                        switch (dataType) {
                        case DataBuffer.TYPE_BYTE:
                            //valueOld = op.calculate(sample, constB);
                            valueOld = ImageUtil.clampRoundByte(valueOld);
                            value = (byte) (((((int) value << 23) >> 31) | (int) value) & 0xFF);
                            break;
                        case DataBuffer.TYPE_USHORT:
                            valueOld = ImageUtil.clampRoundUShort(valueOld) & 0xFFFF;
//                            valueOld = ImageUtil.clampUShort(op.calculate(((int) sample & 0xFFFF),
//                                    (int) doubleConsts[b]));
                            break;
                        case DataBuffer.TYPE_SHORT:
                            valueOld = ImageUtil.clampRoundShort(valueOld);
//                            valueOld = ImageUtil.clampShort(op.calculate((short) sample,
//                                    (short) doubleConsts[b]));
                            break;
                        case DataBuffer.TYPE_INT:
                            valueOld = ImageUtil.clampRoundInt(valueOld);
//                            valueOld = ImageUtil.clampInt(op.calculate((long) sample,
//                                    (long) doubleConsts[b]));
                            break;
                        case DataBuffer.TYPE_FLOAT:
                            valueOld = ImageUtil.clampFloat(valueOld);
//                            valueOld = ImageUtil.clampFloat(op.calculate((float) sample,
//                                    (float) doubleConsts[b]));
                            break;
                        case DataBuffer.TYPE_DOUBLE:
                            //valueOld = op.calculate(sample, doubleConsts[b]);
                            break;
                        default:
                            break;
                        }
                        assertEquals(value, valueOld, TOLERANCE);
                    } else {
                        assertEquals(value, destNoData, TOLERANCE);
                    }
                }
            }
        }
    }
}
