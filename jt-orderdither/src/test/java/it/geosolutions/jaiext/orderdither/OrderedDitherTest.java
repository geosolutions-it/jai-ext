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
package it.geosolutions.jaiext.orderdither;

import static org.junit.Assert.assertEquals;
import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.util.Arrays;

import javax.media.jai.ColorCube;
import javax.media.jai.KernelJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.iterator.RandomIter;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit test for testing OrderedDither operation with ROI and No Data.
 */
public class OrderedDitherTest extends TestBase {
    /** Tolerance value for double comparison */
    public static final double TOLERANCE = 0.01d;

    /** Images used for testing */
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

    /** Input ROI */
    private static ROI roiObject;

    /** Output value for NoData */
    private static double destNoData;

    @BeforeClass
    public static void initialSetup() {
        byte noDataB = 50;
        short noDataS = 50;
        int noDataI = 50;
        float noDataF = 50;
        double noDataD = 50;

        testImages = new RenderedImage[7];

        IMAGE_FILLER = true;
        testImages[DataBuffer.TYPE_BYTE] = createTestImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataB, false, 1, (byte) (64));
        testImages[DataBuffer.TYPE_DOUBLE + 1] = createTestImage(DataBuffer.TYPE_BYTE,
                DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataB, false, 3, (byte) (64));
        testImages[DataBuffer.TYPE_USHORT] = createTestImage(DataBuffer.TYPE_USHORT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataS, false, 1, (short) (Short.MAX_VALUE / 4));
        testImages[DataBuffer.TYPE_SHORT] = createTestImage(DataBuffer.TYPE_SHORT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataS, false, 1, (short) (-49));
        testImages[DataBuffer.TYPE_INT] = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataI, false, 1, (int) (105));
        testImages[DataBuffer.TYPE_FLOAT] = createTestImage(DataBuffer.TYPE_FLOAT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataF, false, 1, (float) ((255 / 2) * 5));
        testImages[DataBuffer.TYPE_DOUBLE] = createTestImage(DataBuffer.TYPE_DOUBLE, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, noDataD, false, 1, (255 / 7) * 13);
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

        destNoData = 10;
    }

    @Test
    public void testValidData() {
        boolean roiUsed = false;
        boolean noDataUsed = false;

        for (int i = 0; i < 6; i++) {
            if (i == 0) {
                testType(testImages[i], noDataUsed, roiUsed, true, true);
                testType(testImages[i], noDataUsed, roiUsed, true, false);
            }
            testType(testImages[i], noDataUsed, roiUsed, false, false);
        }
    }

    @Test
    public void testRoi() {
        boolean roiUsed = true;
        boolean noDataUsed = false;

        for (int i = 0; i < 6; i++) {
            if (i == 0) {
                testType(testImages[i], noDataUsed, roiUsed, true, true);
                testType(testImages[i], noDataUsed, roiUsed, true, false);
            }
            testType(testImages[i], noDataUsed, roiUsed, false, false);
        }
    }

    @Test
    public void testNoData() {
        boolean roiUsed = false;
        boolean noDataUsed = true;

        for (int i = 0; i < 6; i++) {
            if (i == 0) {
                testType(testImages[i], noDataUsed, roiUsed, true, true);
                testType(testImages[i], noDataUsed, roiUsed, true, false);
            }
            testType(testImages[i], noDataUsed, roiUsed, false, false);
        }
    }

    @Test
    public void testRoiNoData() {
        boolean roiUsed = true;
        boolean noDataUsed = true;

        for (int i = 0; i < 6; i++) {
            if (i == 0) {
                testType(testImages[i], noDataUsed, roiUsed, true, true);
                testType(testImages[i], noDataUsed, roiUsed, true, false);
            }
            testType(testImages[i], noDataUsed, roiUsed, false, false);
        }
    }

    private void testType(RenderedImage src, boolean nodataUsed, boolean roiUsed,
            boolean byteOptimized, boolean opt3p3) {
        if (byteOptimized && opt3p3) {
            src = testImages[DataBuffer.TYPE_DOUBLE + 1];
        }
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
        // ROI setting
        ROI roi;

        if (roiUsed) {
            roi = roiObject;
        } else {
            roi = null;
        }

        // Getting the ColorMap
        ColorCube colorMap;
        if (byteOptimized && opt3p3) {
            colorMap = ColorCube.BYTE_855;
        } else {
            colorMap = ColorCube.createColorCube(dataType, new int[] { 8 });
        }

        // Getting the dithering mask
        int numBands = src.getSampleModel().getNumBands();
        KernelJAI[] k = new KernelJAI[numBands];
        int width;
        int height;
        float[] data;
        // Different Mask in case of Byte optimization
        if (byteOptimized) {
            width = 8;
            height = 8;
            data = new float[width * height];
            Arrays.fill(data, 0.5f);
            k[0] = new KernelJAI(width, height, data);
            if (opt3p3) {
                k[1] = new KernelJAI(width, height, data);
                k[2] = new KernelJAI(width, height, data);
            }
        } else {
            width = 64;
            height = 64;
            data = new float[width * height];
            Arrays.fill(data, 0.5f);
            k[0] = new KernelJAI(width, height, data);
        }

        // Ordered Dither operation
        RenderedOp orderedDither = OrderedDitherDescriptor.create(src, colorMap, k, null, roi,
                noData, destNoData + colorMap.getAdjustedOffset());

        checkNoDataROI(orderedDither, src, roi, noData, colorMap);

        // Disposal of the output image
        orderedDither.dispose();
    }

    /**
     * Method for checking if ROI and NoData are handled correctly
     * 
     * @param finalimage
     * @param image
     * @param roi
     * @param nodata
     * @param colorCube
     */
    private void checkNoDataROI(RenderedOp finalimage, RenderedImage image, ROI roi, Range nodata,
            ColorCube colorCube) {
        // Ensure the dimensions are the same
        assertEquals(finalimage.getMinX(), image.getMinX());
        assertEquals(finalimage.getMinY(), image.getMinY());
        assertEquals(finalimage.getWidth(), image.getWidth());
        assertEquals(finalimage.getHeight(), image.getHeight());

        boolean roiExists = roi != null;
        boolean nodataExists = nodata != null;
        // Simply ensure no exception is thrown
        if (!nodataExists && !roiExists) {
            finalimage.getTiles();
        }

        if (nodataExists) {
            nodata = RangeFactory.convertToDoubleRange(nodata);
        }
        RandomIter roiIter = null;
        Rectangle roiBounds = null;
        if (roiExists) {
            PlanarImage roiIMG = roi.getAsImage();
            roiIter = RandomIterFactory.create(roiIMG, finalimage.getBounds(), true, true);
            roiBounds = roi.getBounds();
        }
        // Else check ROI and NoData
        RandomIter sourceIter = RandomIterFactory.create(image, null, true, true);
        RandomIter destIter = RandomIterFactory.create(finalimage, null, true, true);
        // Start the iteration (we iterate only the first band)
        int w = image.getWidth();
        int h = image.getHeight();
        int minX = image.getMinX();
        int minY = image.getMinY();
        int maxX = minX + w;
        int maxY = minY + h;

        double noDataValue = destNoData + colorCube.getAdjustedOffset();

        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {

                double src = sourceIter.getSampleDouble(x, y, 0);
                double dest = destIter.getSampleDouble(x, y, 0);

                boolean valid = true;

                // ROI Check
                if (roiExists && !(roiBounds.contains(x, y) && roiIter.getSample(x, y, 0) > 0)) {
                    valid = false;
                }

                // NoData Check
                if (nodataExists && nodata.contains(src)) {
                    valid = false;
                }
                if (!valid) {
                    assertEquals(noDataValue, dest, TOLERANCE);
                }
            }
        }
    }
}
