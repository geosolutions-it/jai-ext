/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2016 GeoSolutions


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
package it.geosolutions.jaiext.threshold;

import static org.junit.Assert.assertEquals;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;
import it.geosolutions.rendered.viewer.RenderedImageBrowser;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ThresholdTest extends TestBase {
    /** Logger */
    private Logger logger = Logger.getLogger(ThresholdTest.class.getName());

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
                DEFAULT_HEIGHT, noDataB, false, 3, 255); // 0
        testImages[DataBuffer.TYPE_USHORT] = createTestImage(DataBuffer.TYPE_USHORT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataS, false, 3); // 1
        testImages[DataBuffer.TYPE_SHORT] = createTestImage(DataBuffer.TYPE_SHORT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataS, false, 3); // 2
        testImages[DataBuffer.TYPE_INT] = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataI, false, 3); // 3
        testImages[DataBuffer.TYPE_FLOAT] = createTestImage(DataBuffer.TYPE_FLOAT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataF, false, 3); // 4
        testImages[DataBuffer.TYPE_DOUBLE] = createTestImage(DataBuffer.TYPE_DOUBLE, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataD, false, 3); // 5
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

        // Destination No Data
        destNoData = 100.0;

    }

    @Test
    public void test() {

        boolean roiUsed = false;
        boolean nodataUsed = false;
        Range noData = null;
        ROI roi = null;

        double[] low = new double[] { 0.0, 20.0, 0.0 };
        double[] high = new double[] { 10.0, 80.0, 100.0 };
        double[] constant = new double[] { 255, 255, 255 };

        // Threshold operation
        RenderedOp[] threshold = new RenderedOp[6];
        threshold[0] = ThresholdDescriptor.create(noData, destNoData, roi, low, high, constant,
                null, testImages[0]);
        threshold[1] = ThresholdDescriptor.create(noData, destNoData, roi, low, high, constant,
                null, testImages[1]);
        threshold[2] = ThresholdDescriptor.create(noData, destNoData, roi, low, high, constant,
                null, testImages[2]);
        threshold[3] = ThresholdDescriptor.create(noData, destNoData, roi, low, high, constant,
                null, testImages[3]);
        threshold[4] = ThresholdDescriptor.create(noData, destNoData, roi, low, high, constant,
                null, testImages[4]);
        threshold[5] = ThresholdDescriptor.create(noData, destNoData, roi, low, high, constant,
                null, testImages[5]);

        check(testImages[0], threshold[0], low, high, constant, roiUsed, roi, nodataUsed, noData);
        check(testImages[1], threshold[1], low, high, constant, roiUsed, roi, nodataUsed, noData);
        check(testImages[2], threshold[2], low, high, constant, roiUsed, roi, nodataUsed, noData);
        check(testImages[3], threshold[3], low, high, constant, roiUsed, roi, nodataUsed, noData);
        check(testImages[4], threshold[4], low, high, constant, roiUsed, roi, nodataUsed, noData);
        check(testImages[5], threshold[5], low, high, constant, roiUsed, roi, nodataUsed,
                noDataDouble);

        // Operations for showing the image
        if (INTERACTIVE) {
            RenderedImageBrowser.showChain(threshold[1].createInstance(), false, false);
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Disposal of the output image
        for (int i = 0; i < 6; i++)
            threshold[i].dispose();
    }

    @Test
    public void testNoData() {

        boolean roiUsed = false;
        boolean nodataUsed = true;
        ROI roi = null;

        double[] low = new double[] { 0.0, 20.0, 0.0 };
        double[] high = new double[] { 10.0, 80.0, 100.0 };
        double[] constant = new double[] { 100, 100, 100 };

        // Threshold operation
        RenderedOp[] threshold = new RenderedOp[6];
        threshold[0] = ThresholdDescriptor.create(noDataByte, destNoData, roi, low, high, constant,
                null, testImages[0]);
        threshold[1] = ThresholdDescriptor.create(noDataUShort, destNoData, roi, low, high,
                constant, null, testImages[1]);
        threshold[2] = ThresholdDescriptor.create(noDataShort, destNoData, roi, low, high, constant,
                null, testImages[2]);
        threshold[3] = ThresholdDescriptor.create(noDataInt, destNoData, roi, low, high, constant,
                null, testImages[3]);
        threshold[4] = ThresholdDescriptor.create(noDataFloat, destNoData, roi, low, high, constant,
                null, testImages[4]);
        threshold[5] = ThresholdDescriptor.create(noDataDouble, destNoData, roi, low, high,
                constant, null, testImages[5]);

        check(testImages[0], threshold[0], low, high, constant, roiUsed, roi, nodataUsed,
                noDataByte);
        check(testImages[1], threshold[1], low, high, constant, roiUsed, roi, nodataUsed,
                noDataUShort);
        check(testImages[2], threshold[2], low, high, constant, roiUsed, roi, nodataUsed,
                noDataShort);
        check(testImages[3], threshold[3], low, high, constant, roiUsed, roi, nodataUsed,
                noDataInt);
        check(testImages[4], threshold[4], low, high, constant, roiUsed, roi, nodataUsed,
                noDataFloat);
        check(testImages[5], threshold[5], low, high, constant, roiUsed, roi, nodataUsed,
                noDataDouble);

        // Disposal of the output image
        for (int i = 0; i < 6; i++)
            threshold[i].dispose();
    }

    @Test
    public void testROI() {

        boolean roiUsed = true;
        boolean nodataUsed = false;
        ROI roi = roiObject;
        Range noData = null;

        double[] low = new double[] { 0.0, 20.0, 0.0 };
        double[] high = new double[] { 10.0, 80.0, 100.0 };
        double[] constant = new double[] { 255, 255, 255 };

        // Threshold operation
        RenderedOp[] threshold = new RenderedOp[6];
        threshold[0] = ThresholdDescriptor.create(noData, destNoData, roi, low, high, constant,
                null, testImages[0]);
        threshold[1] = ThresholdDescriptor.create(noData, destNoData, roi, low, high, constant,
                null, testImages[1]);
        threshold[2] = ThresholdDescriptor.create(noData, destNoData, roi, low, high, constant,
                null, testImages[2]);
        threshold[3] = ThresholdDescriptor.create(noData, destNoData, roi, low, high, constant,
                null, testImages[3]);
        threshold[4] = ThresholdDescriptor.create(noData, destNoData, roi, low, high, constant,
                null, testImages[4]);
        threshold[5] = ThresholdDescriptor.create(noData, destNoData, roi, low, high, constant,
                null, testImages[5]);

        check(testImages[0], threshold[0], low, high, constant, roiUsed, roi, nodataUsed, noData);
        check(testImages[1], threshold[1], low, high, constant, roiUsed, roi, nodataUsed, noData);
        check(testImages[2], threshold[2], low, high, constant, roiUsed, roi, nodataUsed, noData);
        check(testImages[3], threshold[3], low, high, constant, roiUsed, roi, nodataUsed, noData);
        check(testImages[4], threshold[4], low, high, constant, roiUsed, roi, nodataUsed, noData);
        check(testImages[5], threshold[5], low, high, constant, roiUsed, roi, nodataUsed, noData);

        // Disposal of the output image
        for (int i = 0; i < 6; i++)
            threshold[i].dispose();
    }

    @Test
    public void testROINoData() {

        boolean roiUsed = true;
        boolean nodataUsed = true;
        ROI roi = roiObject;

        double[] low = new double[] { 0.0, 20.0, 0.0 };
        double[] high = new double[] { 10.0, 80.0, 100.0 };
        double[] constant = new double[] { 255, 255, 255 };

        // Threshold operation
        RenderedOp[] threshold = new RenderedOp[6];
        threshold[0] = ThresholdDescriptor.create(noDataByte, destNoData, roi, low, high, constant,
                null, testImages[0]);
        threshold[1] = ThresholdDescriptor.create(noDataUShort, destNoData, roi, low, high,
                constant, null, testImages[1]);
        threshold[2] = ThresholdDescriptor.create(noDataShort, destNoData, roi, low, high, constant,
                null, testImages[2]);
        threshold[3] = ThresholdDescriptor.create(noDataInt, destNoData, roi, low, high, constant,
                null, testImages[3]);
        threshold[4] = ThresholdDescriptor.create(noDataFloat, destNoData, roi, low, high, constant,
                null, testImages[4]);
        threshold[5] = ThresholdDescriptor.create(noDataDouble, destNoData, roi, low, high,
                constant, null, testImages[5]);

        check(testImages[0], threshold[0], low, high, constant, roiUsed, roi, nodataUsed,
                noDataByte);
        check(testImages[1], threshold[1], low, high, constant, roiUsed, roi, nodataUsed,
                noDataUShort);
        check(testImages[2], threshold[2], low, high, constant, roiUsed, roi, nodataUsed,
                noDataShort);
        check(testImages[3], threshold[3], low, high, constant, roiUsed, roi, nodataUsed,
                noDataInt);
        check(testImages[4], threshold[4], low, high, constant, roiUsed, roi, nodataUsed,
                noDataFloat);
        check(testImages[5], threshold[5], low, high, constant, roiUsed, roi, nodataUsed,
                noDataDouble);

        // Disposal of the output image
        for (int i = 0; i < 6; i++)
            threshold[i].dispose();
    }

    @Test
    public void testParam() {

        boolean roiUsed = false;
        boolean nodataUsed = false;
        ROI roi = null;
        Range noData = null;

        double[] low = new double[] { 0.0 };
        double[] high = new double[] { 10.0 };
        double[] constant = new double[] { 255 };

        // arrays created for check the final image, ThresholdOpImage create similar arrays
        // from the input arrays
        double[] lowC = new double[] { 0.0, 0.0, 0.0 };
        double[] highC = new double[] { 10.0, 10.0, 10.0 };
        double[] constantC = new double[] { 255, 255, 255 };

        // Threshold operation
        RenderedOp[] threshold = new RenderedOp[6];
        threshold[0] = ThresholdDescriptor.create(noData, destNoData, roi, low, high, constant,
                null, testImages[0]);
        threshold[1] = ThresholdDescriptor.create(noData, destNoData, roi, low, high, constant,
                null, testImages[1]);
        threshold[2] = ThresholdDescriptor.create(noData, destNoData, roi, low, high, constant,
                null, testImages[2]);
        threshold[3] = ThresholdDescriptor.create(noData, destNoData, roi, low, high, constant,
                null, testImages[3]);
        threshold[4] = ThresholdDescriptor.create(noData, destNoData, roi, low, high, constant,
                null, testImages[4]);
        threshold[5] = ThresholdDescriptor.create(noData, destNoData, roi, low, high, constant,
                null, testImages[5]);

        check(testImages[0], threshold[0], lowC, highC, constantC, roiUsed, roi, nodataUsed,
                noData);
        check(testImages[1], threshold[1], lowC, highC, constantC, roiUsed, roi, nodataUsed,
                noData);
        check(testImages[2], threshold[2], lowC, highC, constantC, roiUsed, roi, nodataUsed,
                noData);
        check(testImages[3], threshold[3], lowC, highC, constantC, roiUsed, roi, nodataUsed,
                noData);
        check(testImages[4], threshold[4], lowC, highC, constantC, roiUsed, roi, nodataUsed,
                noData);
        check(testImages[5], threshold[5], lowC, highC, constantC, roiUsed, roi, nodataUsed,
                noData);

        // Disposal of the output image
        for (int i = 0; i < 6; i++)
            threshold[i].dispose();
    }

    // wrong number of parameters - an IllegalArgumentException is expected
    @Test(expected = IllegalArgumentException.class)
    public void textExceptionParam() {

        boolean roiUsed = false;
        boolean nodataUsed = false;
        ROI roi = null;
        Range noData = null;

        double[] low = new double[] { 10.0, 20.0 };
        double[] high = new double[] { 10.0 };
        double[] constant = new double[] { 255 };

        // arrays created for check the final image, ThresholdOpImage create similar arrays
        // from the input arrays
        double[] lowC = new double[] { 0.0, 0.0, 0.0 };
        double[] highC = new double[] { 10.0, 10.0, 10.0 };
        double[] constantC = new double[] { 255, 255, 255 };

        // Threshold operation
        RenderedOp[] threshold = new RenderedOp[6];
        threshold[0] = ThresholdDescriptor.create(noData, destNoData, roi, low, high, constant,
                null, testImages[0]);
    }

    // wrong number of parameters - an IllegalArgumentException is expected
    @Test(expected = IllegalArgumentException.class)
    public void textExceptionParam2() {

        boolean roiUsed = false;
        boolean nodataUsed = false;
        ROI roi = null;
        Range noData = null;

        double[] low = new double[] {};
        double[] high = new double[] { 10.0 };
        double[] constant = new double[] { 255 };

        // arrays created for check the final image, ThresholdOpImage create similar arrays
        // from the input arrays
        double[] lowC = new double[] { 0.0, 0.0, 0.0 };
        double[] highC = new double[] { 10.0, 10.0, 10.0 };
        double[] constantC = new double[] { 255, 255, 255 };

        // Threshold operation
        RenderedOp[] threshold = new RenderedOp[6];
        threshold[0] = ThresholdDescriptor.create(noData, destNoData, roi, low, high, constant,
                null, testImages[0]);
    }

    // low value > high value - an IllegalArgumentException is expected
    @Test(expected = IllegalArgumentException.class)
    public void textExceptionParam3() {

        boolean roiUsed = false;
        boolean nodataUsed = false;
        ROI roi = null;
        Range noData = null;

        double[] low = new double[] { 50 };
        double[] high = new double[] { 20 };
        double[] constant = new double[] { 255 };

        // arrays created for check the final image, ThresholdOpImage create similar arrays
        // from the input arrays
        double[] lowC = new double[] { 0.0, 0.0, 0.0 };
        double[] highC = new double[] { 10.0, 10.0, 10.0 };
        double[] constantC = new double[] { 255, 255, 255 };

        // Threshold operation
        RenderedOp[] threshold = new RenderedOp[6];
        threshold[0] = ThresholdDescriptor.create(noData, destNoData, roi, low, high, constant,
                null, testImages[0]);
    }

    // source image is null - an IllegalArgumentException is expected
    @Test(expected = IllegalArgumentException.class)
    public void textExceptionParam4() {

        boolean roiUsed = false;
        boolean nodataUsed = false;
        ROI roi = null;
        Range noData = null;

        double[] low = new double[] { 50 };
        double[] high = new double[] { 20 };
        double[] constant = new double[] { 255 };

        // arrays created for check the final image, ThresholdOpImage create similar arrays
        // from the input arrays
        double[] lowC = new double[] { 0.0, 0.0, 0.0 };
        double[] highC = new double[] { 10.0, 10.0, 10.0 };
        double[] constantC = new double[] { 255, 255, 255 };

        // Threshold operation
        RenderedOp[] threshold = new RenderedOp[6];
        threshold[0] = ThresholdDescriptor.create(noData, destNoData, roi, low, high, constant,
                null, null);
    }

    private void check(RenderedImage src, RenderedOp dest, double[] low, double[] high,
            double[] con, boolean roiUsed, ROI roi, boolean nodataUsed, Range noData) {
        int tileWidth = dest.getTileWidth();
        int tileHeight = dest.getTileHeight();
        int minTileX = dest.getMinTileX();
        int minTileY = dest.getMinTileY();
        int numXTiles = dest.getNumXTiles();
        int numYTiles = dest.getNumYTiles();
        int maxTileX = minTileX + numXTiles;
        int maxTileY = minTileY + numYTiles;
        // Ensure same size
        assertEquals(dest.getWidth(), src.getWidth());
        assertEquals(dest.getHeight(), src.getHeight());
        assertEquals(dest.getMinX(), src.getMinX());
        assertEquals(dest.getMinY(), src.getMinY());
        assertEquals(minTileX, src.getMinTileX());
        assertEquals(minTileY, src.getMinTileY());
        assertEquals(numXTiles, src.getNumXTiles());
        assertEquals(numYTiles, src.getNumYTiles());
        assertEquals(tileWidth, src.getTileWidth());
        assertEquals(tileHeight, src.getTileHeight());

        int srcBands = src.getSampleModel().getNumBands();
        int dstBands = dest.getNumBands();
        assertEquals(srcBands, dstBands);

        boolean valid = true;
        // Check on all the pixels if they have been calculate correctly
        for (int tileX = minTileX; tileX < maxTileX; tileX++) {
            for (int tileY = minTileY; tileY < maxTileY; tileY++) {
                Raster tile = dest.getTile(tileX, tileY);
                Raster srcTile = src.getTile(tileX, tileY);

                int minX = tile.getMinX();
                int minY = tile.getMinY();
                int maxX = minX + tileWidth - 1;
                int maxY = minY + tileHeight - 1;

                int minXsrc = srcTile.getMinX();
                int minYsrc = srcTile.getMinY();

                int xsrc = minXsrc;

                for (int x = minX; x <= maxX; x++) {

                    int ysrc = minYsrc;
                    for (int y = minY; y <= maxY; y++) {

                        boolean isValidRoi = !roiUsed || (roiUsed && roi.contains(x, y));

                        if (isValidRoi) {
                            for (int b = 0; b < dstBands; b++) {
                                // Getting the result
                                double samplesource = srcTile.getSampleDouble(xsrc, ysrc, b);
                                double result = tile.getSampleDouble(x, y, b);

                                if (nodataUsed && noData.contains(samplesource)) {
                                    assertEquals(result, destNoData, TOLERANCE);
                                    // if samplesource is in the range, result should be equal to con[b]
                                } else if (samplesource >= low[b] && samplesource <= high[b]) {
                                    if (result != con[b]) {
                                        valid = false;
                                    }
                                }
                                logger.log(Level.FINE,
                                        "srcsample: " + samplesource + " dstsample: "
                                                + tile.getSampleDouble(x, y, b) + " low: " + low[b]
                                                + " high: " + high[b] + " constant: " + con[b]);
                            }

                        } else {
                            for (int b = 0; b < dstBands; b++) {
                                assertEquals(tile.getSampleDouble(x, y, b), destNoData, TOLERANCE);
                            }
                        }

                        ysrc++;
                    }
                    xsrc++;
                }
            }
        }
    }
}
