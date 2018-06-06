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
package it.geosolutions.jaiext.shadedrelief;

import it.geosolutions.imageio.utilities.ImageIOUtilities;
import static it.geosolutions.imageio.utilities.ImageIOUtilities.visualize;
import static org.junit.Assert.assertEquals;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.logging.Logger;

import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static it.geosolutions.jaiext.testclasses.TestBase.IMAGE_FILLER;
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

public class ShadedReliefTest extends TestBase {
    /** Logger */
    private Logger logger = Logger.getLogger(ShadedReliefTest.class.getName());

    /** Tolerance parameter used in comparison */
    private static final double TOLERANCE = 0.1d;

    /** Input data used for testing */
    private static RenderedImage[] testImages;

    private static Range[] noData;
    private static String[] typeName;


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

    private static List<Integer> typesToTest;

//    public static Object getNoData(int type) {
//
//        switch(type) {
//            case DataBuffer.TYPE_BYTE:
//                return noDataB;
//            case DataBuffer.TYPE_DOUBLE:
//                return noDataD;
//            case DataBuffer.TYPE_FLOAT:
//                return noDataF;
//            case DataBuffer.TYPE_INT:
//                return noDataI;
//            case DataBuffer.TYPE_SHORT:
//                return noDataS;
//            case DataBuffer.TYPE_USHORT:
//                return noDataS;
//            default:
//                throw new IllegalArgumentException("Unknown datatype " + type);
//        }
//    }

    private static double noDataD = 50;

    @BeforeClass
    public static void initialSetup() {
        // NoData definition
        byte noDataB = 50;
        short noDataS = 50;
        int noDataI = 50;
        float noDataF = 50;
        //double noDataD = 50;

        double min = 10;
        double max = 100;

        // Image Creation
        testImages = new RenderedImage[6];

        IMAGE_FILLER = true;
        testImages[DataBuffer.TYPE_BYTE] = createTestPyramidImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH, DEFAULT_HEIGHT, min, max, 0); // 0
        testImages[DataBuffer.TYPE_USHORT] = createTestPyramidImage(DataBuffer.TYPE_USHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT, min, max, 0); // 1
        testImages[DataBuffer.TYPE_SHORT] = createTestPyramidImage(DataBuffer.TYPE_SHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT, min, max, 0); // 2
        testImages[DataBuffer.TYPE_INT] = createTestPyramidImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH, DEFAULT_HEIGHT, min, max, 0); // 3
        testImages[DataBuffer.TYPE_FLOAT] = createTestPyramidImage(DataBuffer.TYPE_FLOAT, DEFAULT_WIDTH, DEFAULT_HEIGHT, min, max, 0); // 4
        testImages[DataBuffer.TYPE_DOUBLE] = createTestPyramidImage(DataBuffer.TYPE_DOUBLE, DEFAULT_WIDTH,  DEFAULT_HEIGHT, min, max, 0); // 5
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

        noData = new Range[6];
        noData[DataBuffer.TYPE_BYTE] = noDataByte;
        noData[DataBuffer.TYPE_DOUBLE] = noDataDouble;
        noData[DataBuffer.TYPE_FLOAT] = noDataFloat;
        noData[DataBuffer.TYPE_INT] = noDataInt;
        noData[DataBuffer.TYPE_SHORT] = noDataShort;
        noData[DataBuffer.TYPE_USHORT] = noDataUShort;

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
        destNoData = 100.0;

        typesToTest = new ArrayList<>();
//        typesToTest.add(DataBuffer.TYPE_BYTE);
//        typesToTest.add(DataBuffer.TYPE_USHORT);
//        typesToTest.add(DataBuffer.TYPE_SHORT);
//        typesToTest.add(DataBuffer.TYPE_INT);
//        typesToTest.add(DataBuffer.TYPE_FLOAT);
        typesToTest.add(DataBuffer.TYPE_DOUBLE);

    }

    @Test
    public void test() {

        boolean roiUsed = false;
        boolean nodataUsed = false;
        final Double nullNoData = null;

        double resx = 1;
        double resy = 1;
        double vex = 1; // verticalExaggeration
        double ves = 1; // verticalScale
        double alt = 1; // altitude
        double az = 1; // azimuth
        ShadedReliefAlgorithm algorithm = ShadedReliefAlgorithm.DEFAULT;

        ROI roi = null;

        for (Integer i : typesToTest) {
            RenderedOp shaded = ShadedReliefDescriptor.create(testImages[i], roi, nullNoData, destNoData, resx, resy, vex, ves, alt, az, algorithm, null);
            check(i, testImages[i], shaded, roiUsed, roi, nodataUsed, noData[i]);
            shaded.dispose();
        }
    }

    @Test
    public void testROI() {

        boolean roiUsed = true;
        boolean nodataUsed = false;
        final Double nullNoData = null;
        ROI roi = roiObject;

        double resx = 1;
        double resy = 1;
        double vex = 1; // verticalExaggeration
        double ves = 1; // verticalScale
        double alt = 1; // altitude
        double az = 1; // azimuth
        ShadedReliefAlgorithm algorithm = ShadedReliefAlgorithm.DEFAULT;

        // shadedrelief  operation
        for (Integer i : typesToTest) {
            RenderedOp shaded = ShadedReliefDescriptor.create(testImages[i], roi, nullNoData, destNoData, resx, resy, vex, ves, alt, az, algorithm, null);
            check(i, testImages[i], shaded, roiUsed, roi, nodataUsed, noData[i]);
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
        double vex = 1; // verticalExaggeration
        double ves = 1; // verticalScale
        double alt = 1; // altitude
        double az = 1; // azimuth
        ShadedReliefAlgorithm algorithm = ShadedReliefAlgorithm.DEFAULT;

        for (Integer i : typesToTest) {
            RenderedOp shaded = ShadedReliefDescriptor.create(testImages[i], roi, noDataD, destNoData, resx, resy, vex, ves, alt, az, algorithm, null);
            check(i, testImages[i], shaded, roiUsed, roi, nodataUsed, noData[i]);
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
        double vex = 1; // verticalExaggeration
        double ves = 1; // verticalScale
        double alt = 1; // altitude
        double az = 1; // azimuth
        ShadedReliefAlgorithm algorithm = ShadedReliefAlgorithm.DEFAULT;

        for (Integer i : typesToTest) {
            RenderedOp shaded = ShadedReliefDescriptor.create(testImages[i], roi, noDataD, destNoData, resx, resy, vex, ves, alt, az, algorithm, null);
            check(i, testImages[i], shaded, roiUsed, roi, nodataUsed, noData[i]);
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


        String title = typeName[type] + " " +(roiUsed? "ROI": "NOroi") + " " + (nodataUsed? "NODATA": "NOnodata");

//        ImageIOUtilities.visualize(src, "SRC " + title, true);
//        ImageIOUtilities.visualize(shaded, "SHADED " + title, true);
//        try {

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

        RenderedImageBrowser.showChain(showSrc, false, roiUsed, "SRC " + title);
        RenderedImageBrowser.showChain(showDst, false, roiUsed, "SHADED " + title);
//        } catch (RuntimeException e) {
//            System.out.println("Error on " + title + " ["+e.getMessage()+"]");
//            throw e;
//        }
//        CloseWaiter waiter = new CloseWaiter();
//        waiter.add(f1);
//        waiter.add(f2);
//        waiter.waitAll();

        boolean valid = true;
        // Check on all the pixels if they have been calculate correctly
        for (int tileX = minTileX; tileX < maxTileX; tileX++) {
            for (int tileY = minTileY; tileY < maxTileY; tileY++) {
                Raster tile = shaded.getTile(tileX, tileY);
                Raster srcTile = src.getTile(tileX, tileY);

                int minX = tile.getMinX();
                int minY = tile.getMinY();
                int maxX = minX + tileWidth - 1;
                int maxY = minY + tileHeight - 1;

                int minXsrc = srcTile.getMinX();
                int minYsrc = srcTile.getMinY();

                int xsrc = minXsrc;

//                for (int x = minX; x <= maxX; x++) {
//
//                    int ysrc = minYsrc;
//                    for (int y = minY; y <= maxY; y++) {
//
//                        boolean isValidRoi = !roiUsed || (roiUsed && roi.contains(x, y));
//
//                        if (isValidRoi) {
//                            for (int b = 0; b < dstBands; b++) {
//                                // Getting the result
//                                double samplesource = srcTile.getSampleDouble(xsrc, ysrc, b);
//                                double result = tile.getSampleDouble(x, y, b);
//
//                                if (nodataUsed && noData.contains(samplesource)) {
//                                    assertEquals(result, destNoData, TOLERANCE);
//                                } else if (result < low[b] || result > high[b])
//                                        valid = false;
//                                logger.log(Level.FINE, "srcsample: " + samplesource
//                                        + " dstsample: " + tile.getSampleDouble(x, y, b) + " low: "
//                                        + low[b] + " high: " + high[b]);
//                            }
//
//                        } else {
//                            for (int b = 0; b < dstBands; b++) {
//
//                                assertEquals(tile.getSampleDouble(x, y, b), destNoData, TOLERANCE);
//                            }
//                        }
//
//                        ysrc++;
//                    }
//                    xsrc++;
//                }
            }
        }
        Assert.assertTrue(valid);
    }

    /** Simple method for image creation */
    public static RenderedImage createTestPyramidImage(int dataType, //
            int width, int height,
            Number minValue, Number maxValue,
            Number noDataValue) {
        // parameter block initialization

        final Number validData = null;
        int tileW = width / 8;
        int tileH = height / 8;
        int imageDim = width * height;



        // This values could be used for fill all the image
//        byte valueB = validData != null ? validData.byteValue() : 64;
//        short valueUS = validData != null ? validData.shortValue() : Short.MAX_VALUE / 4;
//        short valueS = validData != null ? validData.shortValue() : -50;
//        int valueI = validData != null ? validData.intValue() : 100;
//        float valueF = validData != null ? validData.floatValue() : (255 / 2) * 5f;
//        double valueD = validData != null ? validData.doubleValue() : (255 / 1) * 4d;

//        boolean fillImage = IMAGE_FILLER;


//        Byte noDataByte = null;
//        Short noDataUShort = null;
//        Short noDataValueShort = null;
//        Integer noDataValueInteger = null;
//        Float noDataValueFloat = null;
//        Double noDataValueDouble = null;

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
//        TiledImage used = new TiledImage(sm, tileW, tileH);
        TiledImage used = new TiledImage(0, 0, width, height, 0, 0, sm, cm);

//        TiledImage used = new TiledImage(0, 0, width, height, 0, 0, sm, colorModel);

//        switch (dataType) {
//        case DataBuffer.TYPE_BYTE:
//            noDataByte = (Byte) noDataValue;
//            break;
//        case DataBuffer.TYPE_USHORT:
//            noDataUShort = (Short) noDataValue;
//            break;
//        case DataBuffer.TYPE_SHORT:
//            noDataValueShort = (Short) noDataValue;
//            break;
//        case DataBuffer.TYPE_INT:
//            noDataValueInteger = (Integer) noDataValue;
//            break;
//        case DataBuffer.TYPE_FLOAT:
//            noDataValueFloat = (Float) noDataValue;
//            break;
//        case DataBuffer.TYPE_DOUBLE:
//            noDataValueDouble = (Double) noDataValue;
//            break;
//        default:
//            throw new IllegalArgumentException("Wrong data type");
//        }

        int imgBinX=width/4;
        int imgBinY=height/4;

        int imgBinWidth=imgBinX + width/4;
        int imgBinHeight=imgBinY + height/4;

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
}
