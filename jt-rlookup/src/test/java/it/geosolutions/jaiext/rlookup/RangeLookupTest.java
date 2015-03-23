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
package it.geosolutions.jaiext.rlookup;

import static org.junit.Assert.assertEquals;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for the RangeLookup operation.
 * 
 * @author Michael Bedward
 */
public class RangeLookupTest extends TestBase {
    /** Image Size */
    private static final int WIDTH = 10;

    /** Tolerance value for double comparison */
    private static final double TOLERANCE = 1E-6;

    /** Images to test */
    private static RenderedImage[] images;

    /** ROI used for testing */
    private static ROIShape roi;

    /** Default value */
    private static Double defaultV;

    @BeforeClass
    public static void initialSetup() {
        images = new RenderedImage[6];
        IMAGE_FILLER = true;
        images[0] = createTestImage(DataBuffer.TYPE_BYTE, WIDTH, WIDTH, (byte) 0, false);
        images[1] = createTestImage(DataBuffer.TYPE_USHORT, WIDTH, WIDTH, (short) 0, false);
        images[2] = createTestImage(DataBuffer.TYPE_SHORT, WIDTH, WIDTH, (short) 0, false);
        images[3] = createTestImage(DataBuffer.TYPE_INT, WIDTH, WIDTH, 0, false);
        images[4] = createTestImage(DataBuffer.TYPE_FLOAT, WIDTH, WIDTH, 0f, false);
        images[5] = createTestImage(DataBuffer.TYPE_DOUBLE, WIDTH, WIDTH, 0d, false);

        IMAGE_FILLER = false;

        roi = new ROIShape(new Rectangle(2, 2, 3, 3));
        defaultV = new Double(13);
    }

    @Test
    public void byteToByte() {
        Byte[] breaks = { 2, 4, 6, 8 };
        Byte[] values = { 0, 1, 2, 3, 4 };
        RenderedImage srcImg = images[0];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_BYTE);
    }

    @Test
    public void byteToByteROI() {
        Byte[] breaks = { 2, 4, 6, 8 };
        Byte[] values = { 0, 1, 2, 3, 4 };
        RenderedImage srcImg = images[0];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_BYTE, roi, defaultV);
    }

    @Test
    public void byteToShort() {
        Byte[] breaks = { 2, 4, 6, 8 };
        Short[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = images[0];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_SHORT);
    }

    @Test
    public void byteToInt() {
        Byte[] breaks = { 2, 4, 6, 8 };
        Integer[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = images[0];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_INT);
    }

    @Test
    public void byteToFloat() {
        Byte[] breaks = { 2, 4, 6, 8 };
        Float[] values = { -50f, -10f, 0f, 10f, 50f };
        RenderedImage srcImg = images[0];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_FLOAT);
    }

    @Test
    public void byteToDouble() {
        Byte[] breaks = { 2, 4, 6, 8 };
        Double[] values = { -50d, -10d, 0d, 10d, 50d };
        RenderedImage srcImg = images[0];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_DOUBLE);
    }

    @Test
    public void shortToByte() {
        Short[] breaks = { 2, 4, 6, 8 };
        Byte[] values = { 0, 1, 2, 3, 4 };
        RenderedImage srcImg = images[2];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_BYTE);
    }

    @Test
    public void shortToShort() {
        Short[] breaks = { 2, 4, 6, 8 };
        Short[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = images[2];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_SHORT);
    }

    @Test
    public void shortToShortROI() {
        Short[] breaks = { 2, 4, 6, 8 };
        Short[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = images[2];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_SHORT, roi, defaultV);
    }

    @Test
    public void shortToInt() {
        Short[] breaks = { 2, 4, 6, 8 };
        Integer[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = images[2];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_INT);
    }

    @Test
    public void shortToFloat() {
        Short[] breaks = { 2, 4, 6, 8 };
        Float[] values = { -50f, -10f, 0f, 10f, 50f };
        RenderedImage srcImg = images[2];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_FLOAT);
    }

    @Test
    public void shortToDouble() {
        Short[] breaks = { 2, 4, 6, 8 };
        Double[] values = { -50d, -10d, 0d, 10d, 50d };
        RenderedImage srcImg = images[2];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_DOUBLE);
    }

    @Test
    public void ushortToByte() {
        Short[] breaks = { 2, 4, 6, 8 };
        Byte[] values = { 0, 1, 2, 3, 4 };
        RenderedImage srcImg = images[1];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_BYTE);
    }

    @Test
    public void ushortToShort() {
        Short[] breaks = { 2, 4, 6, 8 };
        Short[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = images[1];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_SHORT);
    }

    @Test
    public void ushortToInt() {
        Short[] breaks = { 2, 4, 6, 8 };
        Integer[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = images[1];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_INT);
    }

    @Test
    public void ushortToFloat() {
        Short[] breaks = { 2, 4, 6, 8 };
        Float[] values = { -50f, -10f, 0f, 10f, 50f };
        RenderedImage srcImg = images[1];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_FLOAT);
    }

    @Test
    public void ushortToDouble() {
        Short[] breaks = { 2, 4, 6, 8 };
        Double[] values = { -50d, -10d, 0d, 10d, 50d };
        RenderedImage srcImg = images[1];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_DOUBLE);
    }

    @Test
    public void intToByte() {
        Integer[] breaks = { 2, 4, 6, 8 };
        Byte[] values = { 0, 1, 2, 3, 4 };
        RenderedImage srcImg = images[3];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_BYTE);
    }

    @Test
    public void intToShort() {
        Integer[] breaks = { 2, 4, 6, 8 };
        Short[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = images[3];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_SHORT);
    }

    @Test
    public void intToInt() {
        Integer[] breaks = { 2, 4, 6, 8 };
        Integer[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = images[3];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_INT);
    }

    @Test
    public void intToIntROI() {
        Integer[] breaks = { 2, 4, 6, 8 };
        Integer[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = images[3];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_INT, roi, defaultV);
    }

    @Test
    public void intToFloat() {
        Integer[] breaks = { 2, 4, 6, 8 };
        Float[] values = { -50f, -10f, 0f, 10f, 50f };
        RenderedImage srcImg = images[3];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_FLOAT);
    }

    @Test
    public void intToDouble() {
        Integer[] breaks = { 2, 4, 6, 8 };
        Double[] values = { -50d, -10d, 0d, 10d, 50d };
        RenderedImage srcImg = images[3];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_DOUBLE);
    }

    @Test
    public void floatToByte() {
        Float[] breaks = { 2f, 4f, 6f, 8f };
        Byte[] values = { 0, 1, 2, 3, 4 };
        RenderedImage srcImg = images[4];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_BYTE);
    }

    @Test
    public void floatToShort() {
        Float[] breaks = { 2f, 4f, 6f, 8f };
        Short[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = images[4];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_SHORT);
    }

    @Test
    public void floatToInt() {
        Float[] breaks = { 2f, 4f, 6f, 8f };
        Integer[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = images[4];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_INT);
    }

    @Test
    public void floatToFloat() {
        Float[] breaks = { 2f, 4f, 6f, 8f };
        Float[] values = { -50f, -10f, 0f, 10f, 50f };
        RenderedImage srcImg = images[4];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_FLOAT);
    }

    @Test
    public void floatToFloatROI() {
        Float[] breaks = { 2f, 4f, 6f, 8f };
        Float[] values = { -50f, -10f, 0f, 10f, 50f };
        RenderedImage srcImg = images[4];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_FLOAT, roi, defaultV);
    }

    @Test
    public void floatToDouble() {
        Float[] breaks = { 2f, 4f, 6f, 8f };
        Double[] values = { -50d, -10d, 0d, 10d, 50d };
        RenderedImage srcImg = images[4];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_DOUBLE);
    }

    @Test
    public void doubleToByte() {
        Double[] breaks = { 2d, 4d, 6d, 8d };
        Byte[] values = { 0, 1, 2, 3, 4 };
        RenderedImage srcImg = images[5];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_BYTE);
    }

    @Test
    public void doubleToShort() {
        Double[] breaks = { 2d, 4d, 6d, 8d };
        Short[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = images[5];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_SHORT);
    }

    @Test
    public void doubleToInt() {
        Double[] breaks = { 2d, 4d, 6d, 8d };
        Integer[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = images[5];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_INT);
    }

    @Test
    public void doubleToFloat() {
        Double[] breaks = { 2d, 4d, 6d, 8d };
        Float[] values = { -50f, -10f, 0f, 10f, 50f };
        RenderedImage srcImg = images[5];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_FLOAT);
    }

    @Test
    public void doubleToDouble() {
        Double[] breaks = { 2d, 4d, 6d, 8d };
        Double[] values = { -50d, -10d, 0d, 10d, 50d };
        RenderedImage srcImg = images[5];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_DOUBLE);
    }

    @Test
    public void doubleToDoubleROI() {
        Double[] breaks = { 2d, 4d, 6d, 8d };
        Double[] values = { -50d, -10d, 0d, 10d, 50d };
        RenderedImage srcImg = images[5];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_DOUBLE, roi, defaultV);
    }

    @Test
    public void shortToShortWithNoNegativeValues() throws Exception {
        Short[] breaks = { 2, 4, 6, 8 };
        Short[] values = { 0, 1, 2, 3, 4 };
        RenderedImage srcImg = images[2];

        // The destination image shoule be TYPE_USHORT
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_USHORT);
    }

    @Test
    public void ushortToUShort() throws Exception {

        Short[] breaks = { 2, 4, 6, 8 };
        Short[] values = { 0, 1, 2, 3, 4 };
        RenderedImage srcImg = images[1];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_USHORT);
    }

    @Test
    public void ushortToUShortROI() throws Exception {

        Short[] breaks = { 2, 4, 6, 8 };
        Short[] values = { 0, 1, 2, 3, 4 };
        RenderedImage srcImg = images[1];
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_USHORT, roi, defaultV);
    }

    @Test
    public void ushortSourceWithNegativeDestValues() throws Exception {
        Short[] breaks = { 2, 4, 6, 8 };
        Short[] values = { -50, -10, 0, 10, 50 };
        RenderedImage srcImg = images[1];

        // The destination image should be TYPE_SHORT
        assertLookup(breaks, values, srcImg, DataBuffer.TYPE_SHORT);
    }

    /**
     * Runs the lookup operation and tests destination image values.
     * 
     * @param breaks source image breakpoints
     * @param values lookup values
     * @param srcImg source image
     * @param destType expected destination image data type
     */
    private <T extends Number & Comparable<? super T>, U extends Number & Comparable<? super U>> void assertLookup(
            T[] breaks, U[] values, RenderedImage srcImg, int destType) {
        assertLookup(breaks, values, srcImg, destType, null, null);
    }

    private <T extends Number & Comparable<? super T>, U extends Number & Comparable<? super U>> void assertLookup(
            T[] breaks, U[] values, RenderedImage srcImg, int destType, ROI roi, Double defaultValue) {

        RangeLookupTable<T, U> table = createTable(breaks, values);
        RenderedOp destImg = doOp(srcImg, table, roi, defaultValue);

        // check data type
        assertEquals(destType, destImg.getSampleModel().getDataType());
        assertImageValues(srcImg, table, destImg, roi, defaultValue);
    }

    public static <T extends Number & Comparable<? super T>, U extends Number & Comparable<? super U>> RangeLookupTable<T, U> createTable(
            T[] breaks, U[] values) {
        final int N = breaks.length;
        if (values.length != N + 1) {
            throw new IllegalArgumentException(
                    "values array length should be breaks array length + 1");
        }

        RangeLookupTable.Builder<T, U> builder = new RangeLookupTable.Builder<T, U>();
        Range r;

        r = RangeFactory.create(Double.NEGATIVE_INFINITY, false, breaks[0].doubleValue(), false);
        builder.add(r, values[0]);

        for (int i = 1; i < N; i++) {
            r = RangeFactory.create(breaks[i - 1].doubleValue(), true, breaks[i].doubleValue(),
                    false);
            builder.add(r, values[i]);
        }

        r = RangeFactory.create(breaks[N - 1].doubleValue(), true, Double.POSITIVE_INFINITY, false);
        builder.add(r, values[N]);

        return builder.build();
    }

    private RenderedOp doOp(RenderedImage srcImg, RangeLookupTable table, ROI roi,
            Double defaultValue) {
        ParameterBlockJAI pb = new ParameterBlockJAI("RLookup");
        pb.setSource("source0", srcImg);
        pb.setParameter("table", table);
        pb.setParameter("roi", roi);
        pb.setParameter("default", defaultValue);
        return JAI.create("RLookup", pb);
    }

    /**
     * Tests that a destination image contains expected values given the source image and lookup table.
     * 
     * @param srcImg source image
     * @param table lookup table
     * @param destImg destination image
     */
    private void assertImageValues(RenderedImage srcImg, RangeLookupTable table,
            RenderedImage destImg, ROI roi, Double defaultValue) {

        final int srcType = srcImg.getSampleModel().getDataType();

        final int destType = destImg.getSampleModel().getDataType();

        RectIter srcIter = RectIterFactory.create(srcImg, null);
        RectIter destIter = RectIterFactory.create(destImg, null);

        int x = 0;
        int y = 0;
        boolean roiExists = roi != null;
        Rectangle bounds = roiExists ? roi.getBounds() : null;

        do {
            do {
                Number srcVal = getSourceImageValue(srcIter, srcType);
                Number expectedVal = table.getLookupItem(srcVal).getValue();
                // ROI Check
                if (roiExists && !(bounds.contains(x, y) && roi.contains(x, y))) {
                    expectedVal = defaultValue;
                }

                switch (destType) {
                case DataBuffer.TYPE_BYTE:
                    assertEquals(expectedVal.byteValue(), (byte) destIter.getSample());
                    break;
                case DataBuffer.TYPE_SHORT:
                    assertEquals(expectedVal.shortValue(), (short) destIter.getSample());
                    break;
                case DataBuffer.TYPE_INT:
                    assertEquals(expectedVal.intValue(), destIter.getSample());
                    break;
                case DataBuffer.TYPE_FLOAT:
                    assertEquals(expectedVal.floatValue(), destIter.getSampleFloat(), TOLERANCE);
                    break;
                case DataBuffer.TYPE_DOUBLE:
                    assertEquals(expectedVal.doubleValue(), destIter.getSampleDouble(), TOLERANCE);
                    break;
                }

                srcIter.nextPixelDone();
                x++;
            } while (!destIter.nextPixelDone());

            srcIter.nextLineDone();
            srcIter.startPixels();
            destIter.startPixels();
            y++;
            x = 0;
        } while (!destIter.nextLineDone());
    }

    /**
     * Helper method for {@link #assertImageValues}.
     * 
     * @param srcIter source image iterator
     * @param srcType source image data type
     * 
     * @return source image value as a Number
     */
    private Number getSourceImageValue(RectIter srcIter, int srcType) {
        Number val = null;
        switch (srcType) {
        case DataBuffer.TYPE_BYTE:
            val = (byte) (srcIter.getSample() & 0xff);
            break;

        case DataBuffer.TYPE_SHORT:
            val = (short) srcIter.getSample();
            break;

        case DataBuffer.TYPE_USHORT:
            val = (short) (srcIter.getSample() & 0xffff);
            break;

        case DataBuffer.TYPE_INT:
            val = srcIter.getSample();
            break;

        case DataBuffer.TYPE_FLOAT:
            val = srcIter.getSampleFloat();
            break;

        case DataBuffer.TYPE_DOUBLE:
            val = (short) srcIter.getSampleDouble();
            break;

        default:
            throw new IllegalArgumentException("Unknown image type");
        }
        return val;
    }

}
