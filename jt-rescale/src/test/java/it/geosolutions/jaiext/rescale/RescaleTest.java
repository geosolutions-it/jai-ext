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
package it.geosolutions.jaiext.rescale;

import static org.junit.Assert.*;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;

import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;

import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.media.jai.util.ImageUtil;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;
import it.geosolutions.rendered.viewer.RenderedImageBrowser;

/**
 * This test class is used for checking the functionalities of the Rescale operation on all the possible data types. The various tests are performed
 * by taking the destination image tile, and comparing it with the related source tile pixels. The comparison is made by taking each source pixel,
 * rescaling it and ensuring that it is equal to the associated destination pixel. If the pixel is outside the ROI or it is a NoData, then it is
 * skipped. These tests are done with and without No Data, with and without ROI, and, if ROI is present, with and without ROI RasterAccessor. If the
 * user wants to see the result of the operation in a particular condition must set to true the JVM parameter JAI.Ext.Interactive and associate to the
 * JVM parameter JAI.Ext.TestSelector an integer associated with the operation:
 * <ul>
 * <li>0 without ROI and NoData</li>
 * <li>1 with ROI RasterAccessor and without NoData</li>
 * <li>2 with ROI and without NoData</li>
 * <li>3 with ROI RasterAccessor and with NoData</li>
 * <li>4 without ROI and with NoData</li>
 * <li>5 with ROI and NoData</li>
 * </ul>
 */
public class RescaleTest extends TestBase {

    /** Tolerance value used for comparison between double and float */
    private static final double TOLERANCE = 0.01d;

    /** Source images array, one for each data type */
    private static RenderedImage[] sourceIMG;

    /** Roi data used for test */
    private static ROI roi;

    /** No data value for Byte data */
    private static byte noDataB;

    /** No data value for UShort data */
    private static short noDataU;

    /** No data value for Short data */
    private static short noDataS;

    /** No data value for Integer data */
    private static int noDataI;

    /** No data value for Float data */
    private static float noDataF;

    /** No data value for Double data */
    private static double noDataD;

    /** Range of NoData for Byte values */
    private static Range noDataByte;

    /** Range of NoData for UShort values */
    private static Range noDataUShort;

    /** Range of NoData for Short values */
    private static Range noDataShort;

    /** Range of NoData for Integer values */
    private static Range noDataInt;

    /** Range of NoData for Float values */
    private static Range noDataFloat;

    /** Range of NoData for Double values */
    private static Range noDataDouble;

    /** Scale factors used */
    private static double[] scales;

    /** Offset parameters used */
    private static double[] offsets;

    /** Destination No Data used for replacing data where the input vale is a NoData */
    private static double destNoData;

    /** ROI Bounds used for checking if each pixel is inside or outside the ROI */
    private static Rectangle roiBounds;

    @BeforeClass
    public static void initialSetup() {

        // ROI creation
        roiBounds = new Rectangle(5, 5, DEFAULT_WIDTH / 4, DEFAULT_HEIGHT / 4);
        roi = new ROIShape(roiBounds);

        // No Data Range creation

        // No Data values
        noDataB = 50;
        noDataU = 50;
        noDataS = 50;
        noDataI = 50;
        noDataF = 50;
        noDataD = 50;

        boolean minIncluded = true;
        boolean maxIncluded = true;

        noDataByte = RangeFactory.create(noDataB, minIncluded, noDataB, maxIncluded);
        noDataUShort = RangeFactory.createU(noDataU, minIncluded, noDataU, maxIncluded);
        noDataShort = RangeFactory.create(noDataS, minIncluded, noDataS, maxIncluded);
        noDataInt = RangeFactory.create(noDataI, minIncluded, noDataI, maxIncluded);
        noDataFloat = RangeFactory.create(noDataF, minIncluded, noDataF, maxIncluded, true);
        noDataDouble = RangeFactory.create(noDataD, minIncluded, noDataD, maxIncluded, true);

        // Image creations
        IMAGE_FILLER = true;

        sourceIMG = new RenderedImage[6];

        sourceIMG[0] = createTestImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataB, false);
        sourceIMG[1] = createTestImage(DataBuffer.TYPE_USHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataU, false);
        sourceIMG[2] = createTestImage(DataBuffer.TYPE_SHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataS, false);
        sourceIMG[3] = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataI,
                false);
        sourceIMG[4] = createTestImage(DataBuffer.TYPE_FLOAT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataF, false);
        sourceIMG[5] = createTestImage(DataBuffer.TYPE_DOUBLE, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataD, false);

        IMAGE_FILLER = false;

        // Scale values
        scales = new double[] { 10, 20, 30 };

        // Offset values
        offsets = new double[] { 0, 1, 2 };

        // Destination No Data
        destNoData = 0.0d;
    }

    // This test checks if the Rescale is correct in absence of No Data and ROI
    @Test
    public void testNoRangeNoRoi() {
        boolean roiUsed = false;
        boolean noDataRangeUsed = false;
        boolean useROIAccessor = false;
        TestSelection select = TestSelection.NO_ROI_ONLY_DATA;

        // Byte data Type
        testRescale(sourceIMG[0], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Ushort data Type
        testRescale(sourceIMG[1], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Short data Type
        testRescale(sourceIMG[2], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Integer data Type
        testRescale(sourceIMG[3], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Float data Type
        testRescale(sourceIMG[4], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Double data Type
        testRescale(sourceIMG[5], roiUsed, noDataRangeUsed, useROIAccessor, select);
    }

    // This test checks if the Rescale is correct in absence of No Data but with ROI (ROI RasterAccessor not used)
    @Test
    public void testRoiBounds() {
        boolean roiUsed = true;
        boolean noDataRangeUsed = false;
        boolean useROIAccessor = false;
        TestSelection select = TestSelection.ROI_ONLY_DATA;

        // Byte data Type
        testRescale(sourceIMG[0], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Ushort data Type
        testRescale(sourceIMG[1], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Short data Type
        testRescale(sourceIMG[2], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Integer data Type
        testRescale(sourceIMG[3], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Float data Type
        testRescale(sourceIMG[4], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Double data Type
        testRescale(sourceIMG[5], roiUsed, noDataRangeUsed, useROIAccessor, select);
    }

    // This test checks if the Rescale is correct in absence of No Data but with ROI (ROI RasterAccessor used)
    @Test
    public void testRoiAccessor() {
        boolean roiUsed = true;
        boolean noDataRangeUsed = false;
        boolean useROIAccessor = true;
        TestSelection select = TestSelection.ROI_ACCESSOR_ONLY_DATA;

        // Byte data Type
        testRescale(sourceIMG[0], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Ushort data Type
        testRescale(sourceIMG[1], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Short data Type
        testRescale(sourceIMG[2], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Integer data Type
        testRescale(sourceIMG[3], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Float data Type
        testRescale(sourceIMG[4], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Double data Type
        testRescale(sourceIMG[5], roiUsed, noDataRangeUsed, useROIAccessor, select);
    }

    // This test checks if the Rescale is correct in absence of ROI but with No Data
    @Test
    public void testNoData() {
        boolean roiUsed = false;
        boolean noDataRangeUsed = true;
        boolean useROIAccessor = false;
        TestSelection select = TestSelection.NO_ROI_NO_DATA;

        // Byte data Type
        testRescale(sourceIMG[0], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Ushort data Type
        testRescale(sourceIMG[1], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Short data Type
        testRescale(sourceIMG[2], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Integer data Type
        testRescale(sourceIMG[3], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Float data Type
        testRescale(sourceIMG[4], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Double data Type
        testRescale(sourceIMG[5], roiUsed, noDataRangeUsed, useROIAccessor, select);
    }

    // This test checks if the Rescale is correct in presence of No Data and ROI (ROI RasterAccessor not used)
    @Test
    public void testRoiNoData() {
        boolean roiUsed = true;
        boolean noDataRangeUsed = true;
        boolean useROIAccessor = false;
        TestSelection select = TestSelection.ROI_NO_DATA;

        // Byte data Type
        testRescale(sourceIMG[0], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Ushort data Type
        testRescale(sourceIMG[1], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Short data Type
        testRescale(sourceIMG[2], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Integer data Type
        testRescale(sourceIMG[3], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Float data Type
        testRescale(sourceIMG[4], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Double data Type
        testRescale(sourceIMG[5], roiUsed, noDataRangeUsed, useROIAccessor, select);
    }

    // This test checks if the Rescale is correct in presence of No Data and ROI (ROI RasterAccessor used)
    @Test
    public void testRoiAccessorNoData() {
        boolean roiUsed = true;
        boolean noDataRangeUsed = true;
        boolean useROIAccessor = true;
        TestSelection select = TestSelection.ROI_ACCESSOR_NO_DATA;

        // Byte data Type
        testRescale(sourceIMG[0], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Ushort data Type
        testRescale(sourceIMG[1], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Short data Type
        testRescale(sourceIMG[2], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Integer data Type
        testRescale(sourceIMG[3], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Float data Type
        testRescale(sourceIMG[4], roiUsed, noDataRangeUsed, useROIAccessor, select);
        // Double data Type
        testRescale(sourceIMG[5], roiUsed, noDataRangeUsed, useROIAccessor, select);
    }

    public void testRescale(RenderedImage source, boolean roiUsed, boolean noDataUsed,
            boolean useRoiAccessor, TestSelection select) {

        // The precalculated roi is used, if selected by the related boolean.
        ROI roiData;

        if (roiUsed) {
            roiData = roi;
        } else {
            roiData = null;
        }

        // The precalculated NoData Range is used, if selected by the related boolean.
        Range noDataRange;
        // Image data type
        int dataType = source.getSampleModel().getDataType();

        if (noDataUsed) {

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

        // Rescale operation
        PlanarImage rescaled = RescaleDescriptor.create(source, scales, offsets, roiData,
                noDataRange, useRoiAccessor, destNoData, null);

        // Display Image
        if (INTERACTIVE && TEST_SELECTOR == select.getType()) {
            RenderedImageBrowser.showChain(rescaled, false, roiUsed);
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            // Calculation of all the image tiles
            rescaled.getTiles();
        }

        // Rescale control on the first band
        int tileMinX = rescaled.getMinTileX();
        int tileMinY = rescaled.getMinTileY();
        // Selection of the source and destination first tile
        Raster tileDest = rescaled.getTile(tileMinX, tileMinY);
        Raster tileSource = source.getTile(tileMinX, tileMinY);

        int tileMinXpix = tileDest.getMinX();
        int tileMinYpix = tileDest.getMinY();

        int tileMaxXpix = tileDest.getWidth() + tileMinXpix;
        int tileMaxYpix = tileDest.getHeight() + tileMinYpix;

        double scaleFactor = scales[0];
        double offset = offsets[0];
        // loop through the tile pixels
        for (int i = tileMinXpix; i < tileMaxXpix; i++) {
            for (int j = tileMinYpix; j < tileMaxYpix; j++) {

                switch (dataType) {
                case DataBuffer.TYPE_BYTE:
                    // selection of the rescaled pixel
                    byte destValueB = (byte) tileDest.getSample(i, j, 0);
                    // rescale operation on the source pixel
                    int srcValueB = tileSource.getSample(i, j, 0) & 0xFF;
                    byte calculationB = ImageUtil.clampRoundByte(srcValueB * scaleFactor + offset);
                    // comparison
                    if (roiUsed && noDataUsed) {
                        if (roiBounds.contains(i, j) && !noDataRange.contains((byte) srcValueB)) {
                            assertEquals(calculationB, destValueB);
                        }
                    } else if (roiUsed) {
                        if (roiBounds.contains(i, j)) {
                            assertEquals(calculationB, destValueB);
                        }
                    } else if (noDataUsed) {
                        if (!noDataRange.contains((byte) srcValueB)) {
                            assertEquals(calculationB, destValueB);
                        }
                    } else {
                        assertEquals(calculationB, destValueB);
                    }
                    break;
                case DataBuffer.TYPE_USHORT:
                    short destValueU = (short) tileDest.getSample(i, j, 0);
                    int srcValueU = tileSource.getSample(i, j, 0) & 0xFFFF;
                    short calculationU = ImageUtil.clampRoundUShort(srcValueU * scaleFactor
                            + offset);
                    if (roiUsed && noDataUsed) {
                        if (roiBounds.contains(i, j) && !noDataRange.contains((short) srcValueU)) {
                            assertEquals(calculationU, destValueU);
                        }
                    } else if (roiUsed) {
                        if (roiBounds.contains(i, j)) {
                            assertEquals(calculationU, destValueU);
                        }
                    } else if (noDataUsed) {
                        if (!noDataRange.contains((short) srcValueU)) {
                            assertEquals(calculationU, destValueU);
                        }
                    } else {
                        assertEquals(calculationU, destValueU);
                    }
                    break;
                case DataBuffer.TYPE_SHORT:
                    short destValueS = (short) tileDest.getSample(i, j, 0);
                    short srcValueS = (short) tileSource.getSample(i, j, 0);
                    short calculationS = ImageUtil
                            .clampRoundShort(srcValueS * scaleFactor + offset);
                    if (roiUsed && noDataUsed) {
                        if (roiBounds.contains(i, j) && !noDataRange.contains(srcValueS)) {
                            assertEquals(calculationS, destValueS);
                        }
                    } else if (roiUsed) {
                        if (roiBounds.contains(i, j)) {
                            assertEquals(calculationS, destValueS);
                        }
                    } else if (noDataUsed) {
                        if (!noDataRange.contains(srcValueS)) {
                            assertEquals(calculationS, destValueS);
                        }
                    } else {
                        assertEquals(calculationS, destValueS);
                    }
                    break;
                case DataBuffer.TYPE_INT:
                    int destValueI = tileDest.getSample(i, j, 0);
                    int srcValueI = tileSource.getSample(i, j, 0);
                    int calculationI = ImageUtil.clampRoundInt(srcValueI * scaleFactor + offset);
                    if (roiUsed && noDataUsed) {
                        if (roiBounds.contains(i, j) && !noDataRange.contains(srcValueI)) {
                            assertEquals(calculationI, destValueI);
                        }
                    } else if (roiUsed) {
                        if (roiBounds.contains(i, j)) {
                            assertEquals(calculationI, destValueI);
                        }
                    } else if (noDataUsed) {
                        if (!noDataRange.contains(srcValueI)) {
                            assertEquals(calculationI, destValueI);
                        }
                    } else {
                        assertEquals(calculationI, destValueI);
                    }
                    break;
                case DataBuffer.TYPE_FLOAT:
                    float destValueF = tileDest.getSampleFloat(i, j, 0);
                    float srcValueF = tileSource.getSampleFloat(i, j, 0);
                    float calculationF = (float) ((srcValueF * scaleFactor) + offset);
                    if (roiUsed && noDataUsed) {
                        if (roiBounds.contains(i, j) && !noDataRange.contains(srcValueF)) {
                            assertEquals(calculationF, destValueF, TOLERANCE);
                        }
                    } else if (roiUsed) {
                        if (roiBounds.contains(i, j)) {
                            assertEquals(calculationF, destValueF, TOLERANCE);
                        }
                    } else if (noDataUsed) {
                        if (!noDataRange.contains(srcValueF)) {
                            assertEquals(calculationF, destValueF, TOLERANCE);
                        }
                    } else {
                        assertEquals(calculationF, destValueF, TOLERANCE);
                    }
                    break;
                case DataBuffer.TYPE_DOUBLE:
                    double destValueD = tileDest.getSampleDouble(i, j, 0);
                    double srcValueD = tileSource.getSampleDouble(i, j, 0);
                    double calculationD = ((srcValueD * scaleFactor) + offset);
                    if (roiUsed && noDataUsed) {
                        if (roiBounds.contains(i, j) && !noDataRange.contains(srcValueD)) {
                            assertEquals(calculationD, destValueD, TOLERANCE);
                        }
                    } else if (roiUsed) {
                        if (roiBounds.contains(i, j)) {
                            assertEquals(calculationD, destValueD, TOLERANCE);
                        }
                    } else if (noDataUsed) {
                        if (!noDataRange.contains(srcValueD)) {
                            assertEquals(calculationD, destValueD, TOLERANCE);
                        }
                    } else {
                        assertEquals(calculationD, destValueD, TOLERANCE);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Wrong data type");
                }
            }
        }
    }
}
