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
package it.geosolutions.jaiext.bandcombine;

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

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;

/**
 * Unit test for testing the BandCombine operation with NoData and ROI
 */
public class BandCombineTest extends TestBase {
    /** Tolerance parameter used in comparison */
    private static final double TOLERANCE = 0.1d;

    /** Input data used for testing */
    private static RenderedImage[] testImages;

    /** NoData Range for Byte dataType */
    private static Range noDataByte;

    /** NoData Range for Ushort dataType */
    private static Range noDataUShort;

    /** NoData Range for Short dataType */
    private static Range noDataShort;

    /** NoData Range for Int dataType */
    private static Range noDataInt;

    /** NoData Range for Float dataType */
    private static Range noDataFloat;

    /** NoData Range for Double dataType */
    private static Range noDataDouble;

    /** ROI used in tests */
    private static ROI roiObject;

    /** Matrix for band combine */
    private static double[][] matrix;

    /** Value to set as Output NoData */
    private static double destNoData;

    @BeforeClass
    public static void initialSetup() {
        // NoData definition
        byte noDataB = 50;
        short noDataS = 50;
        int noDataI = 50;
        float noDataF = 50;
        double noDataD = 50;

        // Image Creation
        testImages = new RenderedImage[6];

        IMAGE_FILLER = true;
        testImages[DataBuffer.TYPE_BYTE] = createTestImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataB, false, 3);
        testImages[DataBuffer.TYPE_USHORT] = createTestImage(DataBuffer.TYPE_USHORT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataS, false, 3);
        testImages[DataBuffer.TYPE_SHORT] = createTestImage(DataBuffer.TYPE_SHORT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataS, false, 3);
        testImages[DataBuffer.TYPE_INT] = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataI, false, 3);
        testImages[DataBuffer.TYPE_FLOAT] = createTestImage(DataBuffer.TYPE_FLOAT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataF, false, 3);
        testImages[DataBuffer.TYPE_DOUBLE] = createTestImage(DataBuffer.TYPE_DOUBLE, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataD, false, 3);
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

        // ROI creation
        Rectangle roiBounds = new Rectangle(5, 5, DEFAULT_WIDTH / 4, DEFAULT_HEIGHT / 4);
        roiObject = new ROIShape(roiBounds);

        // Matrix creation
        matrix = new double[2][4];
        for (int i = 0; i < matrix[0].length; i++) {
            matrix[0][i] = i - 1;
            matrix[1][i] = i + 1;
        }
    }

    @Test
    public void testValidData() {
        boolean roiUsed = false;
        boolean noDataUsed = false;

        for (int i = 0; i < 6; i++) {
            testType(testImages[i], noDataUsed, roiUsed);
        }
    }

    @Test
    public void testRoi() {
        boolean roiUsed = true;
        boolean noDataUsed = false;

        for (int i = 0; i < 6; i++) {
            testType(testImages[i], noDataUsed, roiUsed);
        }
    }

    @Test
    public void testNoData() {
        boolean roiUsed = false;
        boolean noDataUsed = true;

        for (int i = 1; i < 6; i++) {
            testType(testImages[i], noDataUsed, roiUsed);
        }
    }

    @Test
    public void testRoiNoData() {
        boolean roiUsed = true;
        boolean noDataUsed = true;

        for (int i = 0; i < 6; i++) {
            testType(testImages[i], noDataUsed, roiUsed);
        }
    }

    private void testType(RenderedImage src, boolean nodataUsed, boolean roiUsed) {
        // Optional No Data Range used
        Range noData;
        // Source image data type
        int dataType = src.getSampleModel().getDataType();
        // If no Data are present, the No Data Range associated is used
        if (nodataUsed) {

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

        // BandCombined result
        RenderedOp combined = BandCombineDescriptor.create(src, matrix, roi, noData,
                destinationNoData, null);

        int tileWidth = combined.getTileWidth();
        int tileHeight = combined.getTileHeight();
        int minTileX = combined.getMinTileX();
        int minTileY = combined.getMinTileY();
        int numXTiles = combined.getNumXTiles();
        int numYTiles = combined.getNumYTiles();
        int maxTileX = minTileX + numXTiles;
        int maxTileY = minTileY + numYTiles;
        // Ensure same size
        assertEquals(combined.getWidth(), src.getWidth());
        assertEquals(combined.getHeight(), src.getHeight());
        assertEquals(combined.getMinX(), src.getMinX());
        assertEquals(combined.getMinY(), src.getMinY());
        assertEquals(minTileX, src.getMinTileX());
        assertEquals(minTileY, src.getMinTileY());
        assertEquals(numXTiles, src.getNumXTiles());
        assertEquals(numYTiles, src.getNumYTiles());
        assertEquals(tileWidth, src.getTileWidth());
        assertEquals(tileHeight, src.getTileHeight());

        int srcBands = src.getSampleModel().getNumBands();
        int dstBands = combined.getNumBands();

        // Ensure a correct band size
        assertEquals(dstBands, matrix.length);

        // Check on all the pixels if they have been calculate correctly
        for (int tileX = minTileX; tileX < maxTileX; tileX++) {
            for (int tileY = minTileY; tileY < maxTileY; tileY++) {
                Raster tile = combined.getTile(tileX, tileY);
                Raster srcTile = src.getTile(tileX, tileY);

                int minX = tile.getMinX();
                int minY = tile.getMinY();
                int maxX = minX + tileWidth - 1;
                int maxY = minY + tileHeight - 1;

                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {

                        boolean isValidRoi = !roiUsed || (roiUsed && roiObject.contains(x, y));

                        if (isValidRoi) {
                            for (int b = 0; b < dstBands; b++) {
                                // Getting the result
                                double result = tile.getSampleDouble(x, y, b);

                                // Calculating the expected result from sources
                                boolean valid = false;
                                double calculated = 0;

                                for (int i = 0; i < srcBands; i++) {
                                    double sample = srcTile.getSampleDouble(x, y, i);
                                    boolean isValidData = !nodataUsed
                                            || (nodataUsed && !noDataDouble.contains(sample));
                                    valid |= isValidData;
                                    if (isValidData) {
                                        switch (dataType) {
                                        case DataBuffer.TYPE_BYTE:
                                            calculated += ((int) sample & 0xFF) * matrix[b][i];
                                            break;
                                        case DataBuffer.TYPE_USHORT:
                                            calculated += ((int) sample & 0xFFFF) * matrix[b][i];
                                            break;
                                        case DataBuffer.TYPE_SHORT:
                                        case DataBuffer.TYPE_INT:
                                        case DataBuffer.TYPE_FLOAT:
                                        case DataBuffer.TYPE_DOUBLE:
                                            calculated += sample * matrix[b][i];
                                            break;
                                        default:
                                            break;
                                        }
                                    }
                                }

                                if (valid) {
                                    calculated += matrix[b][srcBands];
                                    switch (dataType) {
                                    case DataBuffer.TYPE_BYTE:
                                        calculated = ImageUtil.clampRoundByte(calculated);
                                        result = ImageUtil.clampRoundByte(result);
                                        break;
                                    case DataBuffer.TYPE_USHORT:
                                        calculated = ImageUtil.clampRoundUShort(calculated);
                                        result = ImageUtil.clampRoundUShort(result);
                                        break;
                                    case DataBuffer.TYPE_SHORT:
                                        calculated = ImageUtil.clampRoundShort(calculated);
                                        result = ImageUtil.clampRoundShort(result);
                                        break;
                                    case DataBuffer.TYPE_INT:
                                        calculated = ImageUtil.clampRoundInt(calculated);
                                        result = ImageUtil.clampRoundInt(result);
                                        break;
                                    case DataBuffer.TYPE_FLOAT:
                                        calculated = (float) calculated;
                                        calculated = (float) result;
                                        break;
                                    case DataBuffer.TYPE_DOUBLE:
                                        break;
                                    default:
                                        break;
                                    }
                                    assertEquals(result, calculated, TOLERANCE);
                                } else {
                                    assertEquals(result, destNoData, TOLERANCE);
                                }
                            }
                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                assertEquals(tile.getSampleDouble(x, y, b), destNoData, TOLERANCE);
                            }
                        }
                    }
                }
            }
        }

        // Disposal of the output image
        combined.dispose();
    }
}
