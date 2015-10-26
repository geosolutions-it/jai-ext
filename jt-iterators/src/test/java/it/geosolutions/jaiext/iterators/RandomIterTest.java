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
package it.geosolutions.jaiext.iterators;

import static org.junit.Assert.assertEquals;

import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;

import javax.media.jai.TiledImage;
import javax.media.jai.iterator.RandomIter;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * J-unit TestClass used for checking the functionalities of all the iterators. The tests called testRandomIterXXX creates a RandomIter with 3
 * different data types and with 3 different RandomIter types:
 * <ul>
 * <li>With or without cached tiles.</li>
 * <li>With or without position array pre calculation (If this array is not calculated the tiles are always not cached).</li>
 * </ul>
 * 
 * The evaluating the capability of the various type of RandomIter to get the correct samples or pixels, with the help of the old kind of RandomIter,
 * an array of 4 pixels has been stored for every data type and every RandomIter type. Other 3 tests are used for showing the calculation speed of the
 * selected RandomIter type(With or without cache, with or without array precalc). This RandomIter can be selected by passing the integral parameter
 * JAI.Ext.TestSelector to the JVM(0 = cached tiles, pre calculation; 1 = no cached tiles, pre calculation, 2 = no cached tiles, no pre calculation).
 * The speed tests can be tuned by changing the number of the benchmark and not benchmark cycles with the 2 parameters JAI.Ext.BenchmarkCycles and
 * JAI.Ext.NotBenchmarkCycles (default 1 and 0 respectively).
 * 
 */
public class RandomIterTest {

    /** Number of benchmark iterations (Default 1) */
    private final static Integer BENCHMARK_ITERATION = Integer.getInteger(
            "JAI.Ext.BenchmarkCycles", 1);

    /** Number of not benchmark iterations (Default 0) */
    private final static int NOT_BENCHMARK_ITERATION = Integer.getInteger(
            "JAI.Ext.NotBenchmarkCycles", 0);

    /** Test selector for the speed test */
    private final static int TEST_SELECTOR = Integer.getInteger("JAI.Ext.TestSelector",0);
    
    /** Test selector for the speed test */
    private final static boolean SUBSEQUENCY = Boolean.getBoolean("JAI.Ext.Subsequency");

    /** expected values from the RandomIterFallbackByte on an Integer image */
    private static int[] valueArrayByte;

    /** expected values from the RandomIterFallbackShort on an Integer image */
    private static int[] valueArrayShort;

    /** expected values from the RandomIterFallbackInt on an Integer image */
    private static int[] valueArrayInt;

    /** expected values from the RandomIterFallbackByte on a Float image */
    private static float[] valueArrayByteIMGFloat;

    /** expected values from the RandomIterFallbackShort on a Float image */
    private static float[] valueArrayShortIMGFloat;

    /** expected values from the RandomIterFallbackInt on a Float image */
    private static float[] valueArrayIntIMGFloat;

    /** expected values from the RandomIterFallbackByte on a Double image */
    private static double[] valueArrayByteIMGDouble;

    /** expected values from the RandomIterFallbackShort on a Double image */
    private static double[] valueArrayShortIMGDouble;

    /** expected values from the RandomIterFallbackInt on a Double image */
    private static double[] valueArrayIntIMGDouble;

    /** Indexes of the expected values */
    private static int[][] indexArray = new int[][] {{ 1, 5 }, { 40, 5 }, { 80, 5 }, { 250, 5 }};

    /** Integral test image for RandomIterFallbackByte */
    private static RenderedImage testImageByte;

    /** Integral test image for RandomIterFallbackShort */
    private static RenderedImage testImageShort;

    /** Integral test image for RandomIterFallbackInt */
    private static RenderedImage testImageInt;

    /** Float test image for RandomIterFallbackByte */
    private static RenderedImage testImageByteIMGFloat;

    /** Float test image for RandomIterFallbackShort */
    private static RenderedImage testImageShortIMGFloat;

    /** Float test image for RandomIterFallbackInt */
    private static RenderedImage testImageIntIMGFloat;

    /** Double test image for RandomIterFallbackByte */
    private static RenderedImage testImageByteIMGDouble;

    /** Double test image for RandomIterFallbackShort */
    private static RenderedImage testImageShortIMGDouble;

    /** Double test image for RandomIterFallbackInt */
    private static RenderedImage testImageIntIMGDouble;

    /** JAI RandomIter used for integral image */
    private static RandomIter iterByte;

    /** JAI RandomIter used for integral image */
    private static RandomIter iterShort;

    /** JAI RandomIter used for integral image */
    private static RandomIter iterInt;

    /** JAI RandomIter used for float image */
    private static RandomIter iterByteIMGFloat;

    /** JAI RandomIter used for float image */
    private static RandomIter iterShortIMGFloat;

    /** JAI RandomIter used for float image */
    private static RandomIter iterIntIMGFloat;

    /** JAI RandomIter used for double image */
    private static RandomIter iterByteIMGDouble;

    /** JAI RandomIter used for double image */
    private static RandomIter iterShortIMGDouble;

    /** JAI RandomIter used for double image */
    private static RandomIter iterIntIMGDouble;

    /** Variable used as tolerance for comparing double or float values */
    private final double DELTA = 0.01d;

    @BeforeClass
    public static void imagePreparation() {

        // IMAGE CREATIONS
        testImageByte = createTestImage(DataBuffer.TYPE_INT, 254, 254, 64, 64);

        testImageByteIMGFloat = createTestImage(DataBuffer.TYPE_FLOAT, 254, 254, 64, 64);

        testImageByteIMGDouble = createTestImage(DataBuffer.TYPE_DOUBLE, 254, 254, 64, 64);

        testImageShort = createTestImage(DataBuffer.TYPE_INT, 512, 254, 2, 64);

        testImageShortIMGFloat = createTestImage(DataBuffer.TYPE_INT, 512, 254, 2, 64);

        testImageShortIMGDouble = createTestImage(DataBuffer.TYPE_INT, 512, 254, 2, 64);

        testImageInt = createTestImage(DataBuffer.TYPE_INT, Short.MAX_VALUE * 2, 254, 1, 64);

        testImageIntIMGFloat = createTestImage(DataBuffer.TYPE_INT, Short.MAX_VALUE * 2, 254, 1, 64);

        testImageIntIMGDouble = createTestImage(DataBuffer.TYPE_INT, Short.MAX_VALUE * 2, 1024, 1,
                64);

        // JAI INTERPOLATORS CREATIONS. USED ONLY FOR CALCULATING THE EXPECTED VALUES.
        iterByte = javax.media.jai.iterator.RandomIterFactory.create(testImageByte, null);

        iterShort = javax.media.jai.iterator.RandomIterFactory.create(testImageShort, null);

        iterInt = javax.media.jai.iterator.RandomIterFactory.create(testImageInt, null);

        iterByteIMGFloat = javax.media.jai.iterator.RandomIterFactory.create(testImageByteIMGFloat,
                null);

        iterShortIMGFloat = javax.media.jai.iterator.RandomIterFactory.create(
                testImageShortIMGFloat, null);

        iterIntIMGFloat = javax.media.jai.iterator.RandomIterFactory.create(testImageIntIMGFloat,
                null);

        iterByteIMGDouble = javax.media.jai.iterator.RandomIterFactory.create(
                testImageByteIMGDouble, null);

        iterShortIMGDouble = javax.media.jai.iterator.RandomIterFactory.create(
                testImageShortIMGDouble, null);

        iterIntIMGDouble = javax.media.jai.iterator.RandomIterFactory.create(testImageIntIMGDouble,
                null);

        // INITIALIZATION AND SAVING OF THE EXPECTED VALUES.
        valueArrayByte = new int[4];

        valueArrayShort = new int[4];

        valueArrayInt = new int[4];

        valueArrayByteIMGFloat = new float[4];

        valueArrayShortIMGFloat = new float[4];

        valueArrayIntIMGFloat = new float[4];

        valueArrayByteIMGDouble = new double[4];

        valueArrayShortIMGDouble = new double[4];

        valueArrayIntIMGDouble = new double[4];

        // Store of the image data calculated
        for (int i = 0; i < indexArray.length; i++) {
            int x = indexArray[i][0];
            int y = indexArray[i][1];

            valueArrayByte[i] = iterByte.getSample(x, y, 0);

            valueArrayShort[i] = iterShort.getSample(x, y, 0);

            valueArrayInt[i] = iterInt.getSample(x, y, 0);

            valueArrayByteIMGFloat[i] = iterByteIMGFloat.getSampleFloat(x, y, 0);

            valueArrayShortIMGFloat[i] = iterShortIMGFloat.getSampleFloat(x, y, 0);

            valueArrayIntIMGFloat[i] = iterIntIMGFloat.getSampleFloat(x, y, 0);

            valueArrayByteIMGDouble[i] = iterByteIMGDouble.getSampleDouble(x, y, 0);

            valueArrayShortIMGDouble[i] = iterShortIMGDouble.getSampleDouble(x, y, 0);

            valueArrayIntIMGDouble[i] = iterIntIMGDouble.getSampleDouble(x, y, 0);
        }
    }

    // This tests is used for the RandomIterFallbackByte iterator on Integral,Float and Double images.
    @Test
    public void testRandomIterByte() {
        testRandomIterInt(testImageByte, valueArrayByte, true, true);

        testRandomIterFloat(testImageByteIMGFloat, valueArrayByteIMGFloat, true, true);

        testRandomIterDouble(testImageByteIMGDouble, valueArrayByteIMGDouble, true, true);
    }

    // This tests is used for the RandomIterFallbackByteNoCache iterator on Integral,Float and Double images.
    @Test
    public void testRandomIterByteNoCache() {
        testRandomIterInt(testImageByte, valueArrayByte, false, true);

        testRandomIterFloat(testImageByteIMGFloat, valueArrayByteIMGFloat, false, true);

        testRandomIterDouble(testImageByteIMGDouble, valueArrayByteIMGDouble, false, true);
    }

    // This tests is used for the RandomIterFallbackShort iterator on Integral,Float and Double images.
    @Test
    public void testRandomIterShort() {
        testRandomIterInt(testImageShort, valueArrayShort, true, true);

        testRandomIterFloat(testImageShortIMGFloat, valueArrayShortIMGFloat, true, true);

        testRandomIterDouble(testImageShortIMGDouble, valueArrayShortIMGDouble, true, true);
    }

    // This tests is used for the RandomIterFallbackShortNoCache iterator on Integral,Float and Double images.
    @Test
    public void testRandomIterShortNoCache() {
        testRandomIterInt(testImageShort, valueArrayShort, false, true);

        testRandomIterFloat(testImageShortIMGFloat, valueArrayShortIMGFloat, false, true);

        testRandomIterDouble(testImageShortIMGDouble, valueArrayShortIMGDouble, false, true);
    }

    // This tests is used for the RandomIterFallbackInt iterator on Integral,Float and Double images.
    @Test
    public void testRandomIterInt() {
        testRandomIterInt(testImageInt, valueArrayInt, true, true);

        testRandomIterFloat(testImageIntIMGFloat, valueArrayIntIMGFloat, true, true);

        testRandomIterDouble(testImageIntIMGDouble, valueArrayIntIMGDouble, true, true);
    }

    // This tests is used for the RandomIterFallbackIntNoCache iterator on Integral,Float and Double images.
    @Test
    public void testRandomIterIntNoCache() {
        testRandomIterInt(testImageInt, valueArrayInt, false, true);

        testRandomIterFloat(testImageIntIMGFloat, valueArrayIntIMGFloat, false, true);

        testRandomIterDouble(testImageIntIMGDouble, valueArrayIntIMGDouble, false, true);
    }

    // This tests is used for the RandomIterFallbackNoCacheNoArray iterator on Integral,Float and Double images.
    @Test
    public void testRandomIterNoArrayCalculation() {
        testRandomIterInt(testImageInt, valueArrayInt, false, false);

        testRandomIterFloat(testImageIntIMGFloat, valueArrayIntIMGFloat, false, false);

        testRandomIterDouble(testImageIntIMGDouble, valueArrayIntIMGDouble, false, false);
    }

    // This tests is used for the RandomIterFallbackByte speed test on an integral image.
    @Test
    public void testSpeed() {
        if (TEST_SELECTOR == 0) {
            testIteratorSpeed(testImageByte, true, true,SUBSEQUENCY);
        }
    }

    // This tests is used for the RandomIterFallbackByteNoCache speed test on an integral image.
    @Test
    public void testSpeedNoCached() {
        if (TEST_SELECTOR == 1) {
            testIteratorSpeed(testImageByte, false, true,SUBSEQUENCY);
        }
    }

    // This tests is used for the RandomIterFallbackNoCacheNoArray speed test on an integral image.
    @Test
    public void testSpeedNoArray() {
        if (TEST_SELECTOR == 2) {
            testIteratorSpeed(testImageByte, false, false,SUBSEQUENCY);
        }
    }

    /** Simple method for image creation */
    public static RenderedImage createTestImage(int dataType, int width, int height, int tileW,
            int tileH) {

        // parameter block initialization

        int imageDim = width * height;

        final SampleModel sm;

        int numBands = 1;

        sm = new ComponentSampleModel(dataType, width, height, 3, width, new int[] { 0, imageDim,
                imageDim * 2 });

        // Create the constant operation.
        TiledImage used = new TiledImage(sm, tileW, tileH);

        for (int b = 0; b < numBands; b++) {
            for (int j = 0; j < width; j++) {
                for (int k = 0; k < height; k++) {
                    if (k < 255 && j < 255) {
                        switch (dataType) {
                        case DataBuffer.TYPE_BYTE:
                        case DataBuffer.TYPE_USHORT:
                        case DataBuffer.TYPE_SHORT:
                        case DataBuffer.TYPE_INT:
                            int value = (int) (Math.random() * 10);
                            used.setSample(j, k, b, value);
                            break;
                        case DataBuffer.TYPE_FLOAT:
                            float valuef = (float) (Math.random() * 10);
                            used.setSample(j, k, b, valuef);
                            break;
                        case DataBuffer.TYPE_DOUBLE:
                            double valued = Math.random() * 10;
                            used.setSample(j, k, b, valued);
                            break;
                        default:
                            throw new IllegalArgumentException("Wrong data type");
                        }
                    }
                }
            }
        }
        return used;
    }

    /** Method for testing the selected input RandomIter on an Integral image */
    public void testRandomIterInt(RenderedImage img, int[] valueArray, boolean cachedTiles,
            boolean arrayCalculation) {
        RandomIter iter = RandomIterFactory.create(img, null, cachedTiles, arrayCalculation);
        int[] array = new int[3];
        // Store of the image data calculated
        for (int i = 0; i < indexArray.length; i++) {
            int x = indexArray[i][0];
            int y = indexArray[i][1];
            int valueExpected = iter.getSample(x, y, 0);
            assertEquals(valueExpected, valueArray[i]);

            int valueExpectedArray = iter.getPixel(x, y, array)[0];
            assertEquals(valueExpectedArray, valueArray[i]);
        }
    }

    /** Method for testing the selected input RandomIter on a Float image */
    public void testRandomIterFloat(RenderedImage img, float[] valueArray, boolean cachedTiles,
            boolean arrayCalculation) {
        RandomIter iter = RandomIterFactory.create(img, null, cachedTiles, arrayCalculation);
        float[] array = new float[3];
        // Store of the image data calculated
        for (int i = 0; i < indexArray.length; i++) {
            int x = indexArray[i][0];
            int y = indexArray[i][1];
            float valueExpected = iter.getSampleFloat(x, y, 0);
            assertEquals(valueExpected, valueArray[i], DELTA);

            float valueExpectedArray = iter.getPixel(x, y, array)[0];
            assertEquals(valueExpectedArray, valueArray[i], DELTA);
        }
    }

    /** Method for testing the selected input RandomIter on a Double image */
    public void testRandomIterDouble(RenderedImage img, double[] valueArray, boolean cachedTiles,
            boolean arrayCalculation) {
        RandomIter iter = RandomIterFactory.create(img, null, cachedTiles, arrayCalculation);
        double[] array = new double[3];
        // Store of the image data calculated
        for (int i = 0; i < indexArray.length; i++) {
            int x = indexArray[i][0];
            int y = indexArray[i][1];
            double valueExpected = iter.getSampleDouble(x, y, 0);
            assertEquals(valueExpected, valueArray[i], DELTA);

            double valueExpectedArray = iter.getPixel(x, y, array)[0];
            assertEquals(valueExpectedArray, valueArray[i], DELTA);
        }
    }

    /** Method for testing the one of the 3 types of RandomIter on the selected image */
    public void testIteratorSpeed(RenderedImage img, boolean cachedTiles, boolean arrayCalculation, boolean subsequentIterator) {
        // RandomIter used
        RandomIter iter = RandomIterFactory.create(img, null, cachedTiles, arrayCalculation);

        String descriptor = "";
        // String used for describing the result
        if (arrayCalculation) {
            descriptor += "ArrayCalculation ";
            if (cachedTiles) {
                descriptor += "Cached Tiles";
            } else {
                descriptor += "No Cached Tiles";
            }
        } else {
            descriptor += "No ArrayCalculation ";
        }

        int totalCycles = NOT_BENCHMARK_ITERATION + BENCHMARK_ITERATION;
        // Initialization of the statistics
        long mean = 0;
        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;
        // Image dimensions
        int imgMinX = testImageByte.getMinX();
        int imgMinY = testImageByte.getMinY();
        int imgWidth = testImageByte.getWidth();
        int imgHeight = testImageByte.getHeight();

        int x = imgMinX;
        int y = imgMinY;
        
        int xStep = (imgWidth-1)/totalCycles;
        int yStep = (imgHeight-1)/totalCycles;
        
        
        for (int i = 0; i < totalCycles; i++) {
            if(subsequentIterator){
                // random pixel position
                x += xStep;
                y += yStep; 
            }else{
                // random pixel position
                x = (int) (Math.random() * imgWidth + imgMinX);
                y = (int) (Math.random() * imgHeight + imgMinY);
            }

            // selection of the sample
            long start = System.nanoTime();
            iter.getSample(x, y, 0);
            long end = System.nanoTime() - start;

            // If the the first NOT_BENCHMARK_ITERATION cycles has been done, then the mean, maximum and minimum values are stored
            if (i > NOT_BENCHMARK_ITERATION - 1) {
                if (i == NOT_BENCHMARK_ITERATION) {
                    mean = end;
                } else {
                    mean = mean + end;
                }

                if (end > max) {
                    max = end;
                }

                if (end < min) {
                    min = end;
                }
            }
        }
        // Mean values
        double meanValue = mean / BENCHMARK_ITERATION;

        // Max and Min values stored as double
        double maxD = max;
        double minD = min;
        // Comparison between the mean times

        // Output
        System.out.println("\nMean value for RandomIterator with " + descriptor + " : " + meanValue
                + " nsec.");
        System.out.println("Maximum value for RandomIterator with " + descriptor + " : " + maxD
                + " nsec.");
        System.out.println("Minimum value for RandomIterator with " + descriptor + " : " + minD
                + " nsec.");

        iter.done();
    }

}
