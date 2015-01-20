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
package it.geosolutions.jaiext.binarize;

import static org.junit.Assert.assertEquals;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;

import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit test for testing Binarize operation with ROI and No Data.
 */
public class BinarizeTest extends TestBase {
    /** Array of inut test data */
    private static RenderedImage[] testImages;

    /** NoData Range for Byte */
    private static Range noDataByte;

    /** NoData Range for Ushort */
    private static Range noDataUShort;

    /** NoData Range for Short */
    private static Range noDataShort;

    /** NoData Range for Int */
    private static Range noDataInt;

    /** NoData Range for Float */
    private static Range noDataFloat;

    /** NoData Range for Double */
    private static Range noDataDouble;

    /** ROI object used for tests */
    private static ROI roiObject;

    /** Threshold used for binarization */
    private static double[] thresholds;

    @BeforeClass
    public static void initialSetup() {
        byte noDataB = 50;
        short noDataS = 50;
        int noDataI = 50;
        float noDataF = 50;
        double noDataD = 50;

        // thresholds
        thresholds = new double[6];
        thresholds[0] = 63;
        thresholds[1] = Short.MAX_VALUE / 4;
        thresholds[2] = -49;
        thresholds[3] = 105;
        thresholds[4] = (255 / 2) * 5;
        thresholds[5] = (255 / 7) * 13;

        // Image creation
        testImages = new RenderedImage[6];

        IMAGE_FILLER = true;
        testImages[DataBuffer.TYPE_BYTE] = createTestImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataB, false, 1, (byte) (thresholds[DataBuffer.TYPE_BYTE] + 1));
        testImages[DataBuffer.TYPE_USHORT] = createTestImage(DataBuffer.TYPE_USHORT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataS, false, 1, (short) (thresholds[DataBuffer.TYPE_USHORT] + 1));
        testImages[DataBuffer.TYPE_SHORT] = createTestImage(DataBuffer.TYPE_SHORT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataS, false, 1, (short) (thresholds[DataBuffer.TYPE_SHORT] + 1));
        testImages[DataBuffer.TYPE_INT] = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataI, false, 1, (int) (thresholds[DataBuffer.TYPE_INT] + 1));
        testImages[DataBuffer.TYPE_FLOAT] = createTestImage(DataBuffer.TYPE_FLOAT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataF, false, 1, (float) (thresholds[DataBuffer.TYPE_FLOAT] + 1));
        testImages[DataBuffer.TYPE_DOUBLE] = createTestImage(DataBuffer.TYPE_DOUBLE, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataD, false, 1, thresholds[DataBuffer.TYPE_DOUBLE] + 1);
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

        for (int i = 0; i < 6; i++) {
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
        // Setting threshold
        double threshold = thresholds[dataType];

        ROI roi;

        if (roiUsed) {
            roi = roiObject;
        } else {
            roi = null;
        }

        // binarized
        RenderedOp binarized = BinarizeDescriptor.create(src, threshold, roi, noData, null);

        int tileWidth = binarized.getTileWidth();
        int tileHeight = binarized.getTileHeight();
        int minTileX = binarized.getMinTileX();
        int minTileY = binarized.getMinTileY();
        int numXTiles = binarized.getNumXTiles();
        int numYTiles = binarized.getNumYTiles();
        int maxTileX = minTileX + numXTiles;
        int maxTileY = minTileY + numYTiles;
        // Ensure same size
        assertEquals(binarized.getWidth(), src.getWidth());
        assertEquals(binarized.getHeight(), src.getHeight());
        assertEquals(binarized.getMinX(), src.getMinX());
        assertEquals(binarized.getMinY(), src.getMinY());
        assertEquals(minTileX, src.getMinTileX());
        assertEquals(minTileY, src.getMinTileY());
        assertEquals(numXTiles, src.getNumXTiles());
        assertEquals(numYTiles, src.getNumYTiles());
        assertEquals(tileWidth, src.getTileWidth());
        assertEquals(tileHeight, src.getTileHeight());

        // Check if the binarization is correct
        for (int tileX = minTileX; tileX < maxTileX; tileX++) {
            for (int tileY = minTileY; tileY < maxTileY; tileY++) {
                Raster tile = binarized.getTile(tileX, tileY);
                Raster srcTile = src.getTile(tileX, tileY);

                int minX = tile.getMinX();
                int minY = tile.getMinY();
                int maxX = minX + tileWidth - 1;
                int maxY = minY + tileHeight - 1;

                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        double sample = srcTile.getSampleDouble(x, y, 0);
                        int result = tile.getSample(x, y, 0);
                        boolean over = result > 0;
                        // Check if the data can be accepted
                        boolean isValidData = (!roiUsed || (roiUsed && roiObject.contains(x, y)))
                                && (!nodataUsed || (nodataUsed && !noDataDouble.contains(sample)));
                        if (isValidData) {
                            assertEquals(over, sample >= threshold);
                        } else {
                            assertEquals(over, false);
                        }
                    }
                }
            }
        }

        // Disposal of the output image
        binarized.dispose();
    }
}
