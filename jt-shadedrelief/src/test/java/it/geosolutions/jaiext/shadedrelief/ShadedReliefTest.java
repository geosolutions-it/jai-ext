/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2018 GeoSolutions


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
package it.geosolutions.jaiext.shadedrelief;

import static org.junit.Assert.assertEquals;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.logging.Logger;

import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;

import org.junit.BeforeClass;
import org.junit.Test;

import it.geosolutions.rendered.viewer.RenderedImageBrowser;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.util.ArrayList;
import java.util.List;
import javax.media.jai.JAI;
import org.junit.AfterClass;

public class ShadedReliefTest extends TestBase {
    /** Logger */
    private Logger logger = Logger.getLogger(ShadedReliefTest.class.getName());


    private static final double DEFAULT_Z = 1;
    private static final double DEFAULT_SCALE = 1;
    private static final double DEFAULT_ALTITUDE = 64;
    private static final double DEFAULT_AZIMUTH = 160;

    /** Input data used for testing */
    private static RenderedImage[] testImages;

    private static Range[] srcNoData;

    /** Value to set as Output NoData */
    private static double dstNoData;

    /** needed for some debug logs */
    private static String[] typeName;

    /** ROI used in tests */
    private static ROI roiObject;

    private static List<Integer> typesToTest;

    private static TestPoint[] testPoints;

    @BeforeClass
    public static void initialSetup() {

        double min = 10;
        double max = 100;

        // Image Creation
        testImages = new RenderedImage[6];

        testImages[DataBuffer.TYPE_BYTE] = createTestPyramidImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH, DEFAULT_HEIGHT, min, max, 0);
        testImages[DataBuffer.TYPE_USHORT] = createTestPyramidImage(DataBuffer.TYPE_USHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT, min, max, 0);
        testImages[DataBuffer.TYPE_SHORT] = createTestPyramidImage(DataBuffer.TYPE_SHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT, min, max, 0);
        testImages[DataBuffer.TYPE_INT] = createTestPyramidImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH, DEFAULT_HEIGHT, min, max, 0);
        testImages[DataBuffer.TYPE_FLOAT] = createTestPyramidImage(DataBuffer.TYPE_FLOAT, DEFAULT_WIDTH, DEFAULT_HEIGHT, min, max, 0);
        testImages[DataBuffer.TYPE_DOUBLE] = createTestPyramidImage(DataBuffer.TYPE_DOUBLE, DEFAULT_WIDTH,  DEFAULT_HEIGHT, min, max, 0);

        int w2 = DEFAULT_WIDTH / 2;
        int w4 = DEFAULT_WIDTH / 4;
        int w34 = 3 * DEFAULT_WIDTH / 4;
        int h2 = DEFAULT_HEIGHT / 2;
        int h4 = DEFAULT_HEIGHT / 4;
        int h34 = 3 * DEFAULT_HEIGHT / 4;

        testPoints = new TestPoint[]{
            new TestPoint(w2, h4, 236d),
            new TestPoint(w34, h2, 189d),
            new TestPoint(w2, h34, 88d),
            new TestPoint(w4, h2, 135d),
        };

        // source NoData definition
        final byte noDataB = 50;
        final short noDataS = 50;
        final int noDataI = 50;
        final float noDataF = 50;
        final double noDataD = 50;

        // No Data Ranges
        boolean minIncluded = true;
        boolean maxIncluded = true;

        srcNoData = new Range[6];
        srcNoData[DataBuffer.TYPE_BYTE] = RangeFactory.create(noDataB, minIncluded, noDataB, maxIncluded);;
        srcNoData[DataBuffer.TYPE_USHORT] = RangeFactory.createU(noDataS, minIncluded, noDataS, maxIncluded);
        srcNoData[DataBuffer.TYPE_SHORT] = RangeFactory.create(noDataS, minIncluded, noDataS, maxIncluded);
        srcNoData[DataBuffer.TYPE_INT] = RangeFactory.create(noDataI, minIncluded, noDataI, maxIncluded);
        srcNoData[DataBuffer.TYPE_FLOAT] = RangeFactory.create(noDataF, minIncluded, noDataF, maxIncluded, true);
        srcNoData[DataBuffer.TYPE_DOUBLE] = RangeFactory.create(noDataD, minIncluded, noDataD, maxIncluded, true);

        typeName = new String[6];
        typeName[DataBuffer.TYPE_BYTE] = "Byte";
        typeName[DataBuffer.TYPE_DOUBLE] = "Double";
        typeName[DataBuffer.TYPE_FLOAT] = "Float";
        typeName[DataBuffer.TYPE_INT] = "Int";
        typeName[DataBuffer.TYPE_SHORT] = "Short";
        typeName[DataBuffer.TYPE_USHORT] = "UShort";

        // ROI creation
        Rectangle roiBounds = new Rectangle(
                DEFAULT_WIDTH / 6, DEFAULT_HEIGHT / 4,
                DEFAULT_WIDTH / 6, DEFAULT_HEIGHT / 2);
        roiObject = new ROIShape(roiBounds);

        // Destination No Data
        dstNoData = 100.0;

        // allow to restrict tests to only defined datatypes
        typesToTest = new ArrayList<>();
        typesToTest.add(DataBuffer.TYPE_BYTE);
        typesToTest.add(DataBuffer.TYPE_USHORT);
        typesToTest.add(DataBuffer.TYPE_SHORT);
        typesToTest.add(DataBuffer.TYPE_INT);
        typesToTest.add(DataBuffer.TYPE_FLOAT);
        typesToTest.add(DataBuffer.TYPE_DOUBLE);
    }

    @AfterClass
    public static void after() {
        // useful for a breakpoint
        System.out.println("Test class completed");
    }

    @Test
    public void test() {

        boolean roiUsed = false;
        boolean nodataUsed = false;

        double resx = 1;
        double resy = 1;
        double vex = DEFAULT_Z;
        double ves = DEFAULT_SCALE;
        double alt = DEFAULT_ALTITUDE;
        double az = DEFAULT_AZIMUTH;
        ShadedReliefAlgorithm algorithm = ShadedReliefAlgorithm.DEFAULT;

        ROI roi = null;

        for (Integer i : typesToTest) {
            RenderedOp shaded = ShadedReliefDescriptor.create(testImages[i], roi, null, dstNoData, resx, resy, vex, ves, alt, az, algorithm, null);
            check(i, testImages[i], shaded, roiUsed, roi, nodataUsed, srcNoData[i]);
            shaded.dispose();
        }
    }

    @Test
    public void testROI() {

        boolean roiUsed = true;
        boolean nodataUsed = false;
        ROI roi = roiObject;

        double resx = 1;
        double resy = 1;
        double vex = DEFAULT_Z;
        double ves = DEFAULT_SCALE;
        double alt = DEFAULT_ALTITUDE;
        double az = DEFAULT_AZIMUTH;
        ShadedReliefAlgorithm algorithm = ShadedReliefAlgorithm.DEFAULT;

        // shadedrelief  operation
        for (Integer i : typesToTest) {
            RenderedOp shaded = ShadedReliefDescriptor.create(testImages[i], roi, null, dstNoData, resx, resy, vex, ves, alt, az, algorithm, null);
            check(i, testImages[i], shaded, roiUsed, roi, nodataUsed, srcNoData[i]);
            shaded.dispose();
        }
    }

    @Test
    public void testNoData() {

        boolean roiUsed = false;
        boolean nodataUsed = true;
        ROI roi = null;

        double resx = 1;
        double resy = 1;
        double vex = DEFAULT_Z;
        double ves = DEFAULT_SCALE;
        double alt = DEFAULT_ALTITUDE;
        double az = DEFAULT_AZIMUTH;
        ShadedReliefAlgorithm algorithm = ShadedReliefAlgorithm.DEFAULT;

        for (Integer i : typesToTest) {
            RenderedOp shaded = ShadedReliefDescriptor.create(testImages[i], roi, srcNoData[i], dstNoData, resx, resy, vex, ves, alt, az, algorithm, null);
            check(i, testImages[i], shaded, roiUsed, roi, nodataUsed, srcNoData[i]);
            shaded.dispose();
        }
    }

    @Test
    public void testROInoData() {

        boolean roiUsed = true;
        boolean nodataUsed = true;
        ROI roi = roiObject;

        double resx = 1;
        double resy = 1;
        double vex = DEFAULT_Z;
        double ves = DEFAULT_SCALE;
        double alt = DEFAULT_ALTITUDE;
        double az = DEFAULT_AZIMUTH;
        ShadedReliefAlgorithm algorithm = ShadedReliefAlgorithm.DEFAULT;

        for (Integer i : typesToTest) {
            RenderedOp shaded = ShadedReliefDescriptor.create(testImages[i], roi, srcNoData[i], dstNoData, resx, resy, vex, ves, alt, az, algorithm, null);
            check(i, testImages[i], shaded, roiUsed, roi, nodataUsed, srcNoData[i]);
            shaded.dispose();
        }
    }

    private void check(int type, RenderedImage src, RenderedOp shaded, boolean roiUsed,
           ROI roi, boolean nodataUsed, Range noData) {

        int tileWidth = shaded.getTileWidth();
        int tileHeight = shaded.getTileHeight();
        int minTileX = shaded.getMinTileX();
        int minTileY = shaded.getMinTileY();
        int numXTiles = shaded.getNumXTiles();
        int numYTiles = shaded.getNumYTiles();
        int maxTileX = minTileX + numXTiles;
        int maxTileY = minTileY + numYTiles;

        // Ensure same size
        assertEquals(shaded.getWidth(), src.getWidth());
        assertEquals(shaded.getHeight(), src.getHeight());
        assertEquals(shaded.getMinX(), src.getMinX());
        assertEquals(shaded.getMinY(), src.getMinY());
        assertEquals(minTileX, src.getMinTileX());
        assertEquals(minTileY, src.getMinTileY());
        assertEquals(numXTiles, src.getNumXTiles());
        assertEquals(numYTiles, src.getNumYTiles());
        assertEquals(tileWidth, src.getTileWidth());
        assertEquals(tileHeight, src.getTileHeight());

        int srcBands = src.getSampleModel().getNumBands();
        int dstBands = shaded.getNumBands();
        assertEquals(srcBands, dstBands);


        if( 0 == 1 ) { // enable this block to display the images
            String title = typeName[type] + " " +(roiUsed? "ROI": "NOroi") + " " + (nodataUsed? "NODATA": "NOnodata");

            RenderedImage showSrc = src;
            RenderedImage showDst = shaded;
            if(type == DataBuffer.TYPE_USHORT ||
                    type == DataBuffer.TYPE_SHORT ||
                    type == DataBuffer.TYPE_FLOAT ||
                    type == DataBuffer.TYPE_DOUBLE ||
                    type == DataBuffer.TYPE_INT ) {
                showSrc = rescale(showSrc, null);
                showDst = rescale(showDst, null);
            }

            //RenderedImageBrowser.showChain(showSrc, false, roiUsed, "SRC " + title);
            RenderedImageBrowser.showChain(showDst, false, roiUsed, "SHADED " + title);
        }

        for (TestPoint point : testPoints) {
            double val = shaded.getTile(0,0).getSampleDouble(point.x, point.y, 0);
            boolean isInRoi = !roiUsed || (roiUsed && roi.contains(point.x, point.y));

            double expected = isInRoi ? point.value : dstNoData;

            boolean ok = (Double.compare(expected, val) == 0) ||
                    Math.abs(expected - val) < 0.5;

            System.out.println(
                    (roiUsed? "ROI": "   ") + " " + (nodataUsed? "NODATA": "      ")
                    + "  TYPE " + typeName[type] + " x:" + point.x + " y:" + point.y + " v:" + point.value +" --> " + val + " "
                    + (isInRoi ? " IN ROI" : "")  + " "
                    + (ok ? " OK" : "BAD!! nd=" + dstNoData)
                    );

            assertEquals("Bad shaded value", expected, val, 0.5d);
        }
    }

    /** Simple method for image creation */
    public static RenderedImage createTestPyramidImage(int dataType, //
            int width, int height,
            Number valueAtBase, Number valueAtTop,
            Number noDataValue) {

        final SampleModel sm = new ComponentSampleModel(dataType, width, height, 1, width, new int[] {0});
        ColorModel cm = TiledImage.createColorModel(sm);
        if(cm == null) {
            // workaround for SHORT type
            if (dataType == DataBuffer.TYPE_SHORT) {
                ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
                cm = new ComponentColorModel(cs, false, false, Transparency.OPAQUE, DataBuffer.TYPE_SHORT);
            }
            else
                throw new IllegalStateException("NO COLOR MODEL");
        }

        // Create the constant operation.
        TiledImage used = new TiledImage(0, 0, width, height, 0, 0, sm, cm);

        final int b = 0;

        for (int x = 0; x <= width/2; x++) {
            for (int y = 0; y < height/2; y++) {

                Double value = 0d;
                value = Math.min((double)x,(double)y);

                setSample(dataType, used, x, y, value);
                setSample(dataType, used, width-x-1, y, value);
                setSample(dataType, used, width-x-1, height-y-1, value);
                setSample(dataType, used, x, height-y-1, value);
            }
        }

        return used;
    }

    private static void setSample(int dataType, TiledImage used, int x, int y, Double value) {
        final int b = 0;

        switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                used.setSample(x, y, b, value.byteValue());
                break;
            case DataBuffer.TYPE_USHORT:
                used.setSample(x, y, b, value.shortValue());
                break;
            case DataBuffer.TYPE_SHORT:
                used.setSample(x, y, b, value.shortValue());
                break;
            case DataBuffer.TYPE_INT:
                used.setSample(x, y, b, value.intValue());
                break;
            case DataBuffer.TYPE_FLOAT:
                used.setSample(x, y, b, value.floatValue());
                break;
            case DataBuffer.TYPE_DOUBLE:
                used.setSample(x, y, b, value);
                break;
            default:
                throw new IllegalArgumentException("Wrong data type");
        }
    }

    static RenderedImage rescale(RenderedImage image, ROI roi) {

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image); // The source image

        if (roi != null)
            pb.add(roi); // The region of the image to scan

        // Perform the extrema operation on the source image
        RenderedOp op = JAI.create("extrema", pb);

        // Retrieve both the maximum and minimum pixel value
        double[][] extrema = (double[][]) op.getProperty("extrema");

        final double[] scale = new double[] { (255) / (extrema[1][0] - extrema[0][0]) };
        final double[] offset = new double[] { ((255) * extrema[0][0])
                / (extrema[0][0] - extrema[1][0]) };

        // Preparing to rescaling values
        ParameterBlock pbRescale = new ParameterBlock();
        pbRescale.add(scale);
        pbRescale.add(offset);
        pbRescale.addSource(image);
        RenderedOp rescaledImage = JAI.create("Rescale", pbRescale);

        ParameterBlock pbConvert = new ParameterBlock();
        pbConvert.addSource(rescaledImage);
        pbConvert.add(DataBuffer.TYPE_BYTE);
        RenderedOp destImage = JAI.create("format", pbConvert);

        return destImage;
    }

    static class TestPoint {
        int x;
        int y;
        Double value;

        public TestPoint(int x, int y, Double value) {
            this.x = x;
            this.y = y;
            this.value = value;
        }

    }
}
