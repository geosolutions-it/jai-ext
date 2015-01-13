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
package it.geosolutions.jaiext.scale;

import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import it.geosolutions.jaiext.JAIExt;
import it.geosolutions.jaiext.interpolators.InterpolationBicubic;
import it.geosolutions.jaiext.interpolators.InterpolationBilinear;
import it.geosolutions.jaiext.interpolators.InterpolationNearest;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;
import it.geosolutions.jaiext.testclasses.TestData;

import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test class is used for compare the timing between the new Nearest,Bilinear and Bicubic interpolators and their JAI version on the scale
 * operation. No Roi or No Data range are used. If the user wants to change the number of the benchmark cycles or of the not benchmark cycles, should
 * only pass the new values to the JAI.Ext.BenchmarkCycles or JAI.Ext.NotBenchmarkCycles parameters. The tests are quite different because the
 * interpolator used is always one of the 3 JAI interpolators but the other operations are similar:
 * <ul>
 * </ul>
 * <ul>
 * <li>Selection of the descriptor (new ScaleDescriptor or old ScaleDescriptor)</li>
 * <li>Selection of the interpolator (Standard or New)</li>
 * <li>Image Magnification\Reduction</li>
 * <li>statistic calculation (if the cycle belongs to the benchmark cycles)</li>
 * </ul>
 * The interpolator can be chosen by passing the JAI.Ext.TestSelector Integer JVM parameter: 0 for nearest interpolation, 1 for bilinear, 2 for
 * bicubic. The selection of the old or new descriptor must be done by setting to true or false the JVM parameter JAI.Ext.OldDescriptor. If the user
 * wants to use the accelerated code, the JVM parameter JAI.Ext.Acceleration must be set to true.
 */
public class ComparisonTest extends TestBase{

    /** Number of benchmark iterations (Default 1) */
    private final static Integer BENCHMARK_ITERATION = Integer.getInteger(
            "JAI.Ext.BenchmarkCycles", 1);

    /** Number of not benchmark iterations (Default 0) */
    private final static int NOT_BENCHMARK_ITERATION = Integer.getInteger(
            "JAI.Ext.NotBenchmarkCycles", 0);

    /** Index for selecting one of the 3 interpolators(Default 0) */
    private final static int TEST_SELECTOR = Integer.getInteger("JAI.Ext.TestSelector", 0);

    /** Boolean indicating if the native acceleration must be used */
    private final static boolean NATIVE_ACCELERATION = Boolean.getBoolean("JAI.Ext.Acceleration");

    /** Boolean indicating if the image should be reduced instead of increased */
    public static Boolean IMAGE_REDUCTION = Boolean.getBoolean("JAI.Ext.ImageReduction");

    /** Default subsampling bits used for the bilinear and bicubic interpolation */
    private final static int DEFAULT_SUBSAMPLE_BITS = 8;

    /** Boolean indicating if the old descriptor must be used */
    private final static boolean OLD_DESCRIPTOR = Boolean.getBoolean("JAI.Ext.OldDescriptor");
    
    /** Boolean indicating if a No Data Range must be used */
    private final static boolean RANGE_USED = Boolean.getBoolean("JAI.Ext.RangeUsed");

    /** Default precision bits used in the bicubic calculation */
    private final static int DEFAULT_PRECISION_BITS = 8;

    /** Value indicating No Data for the destination image */
    private static double destinationNoData = 0;

    /** Translation parameter on the X axis */
    private float xTrans = 0;

    /** Translation parameter on the Y axis */
    private float yTrans = 0;

    /** Scale parameter on the X axis */
    private float xScale = 1.5f;

    /** Scale parameter on the Y axis */
    private float yScale = 1.5f;

    /** JAI nearest Interpolator */
    private static javax.media.jai.InterpolationNearest interpNearOld;

    /** New nearest Interpolator */
    private static InterpolationNearest interpNearNew;

    /** JAI bilinear Interpolator */
    private static javax.media.jai.InterpolationBilinear interpBilOld;

    /** New bilinear Interpolator */
    private static InterpolationBilinear interpBilNew;

    /** JAI bicubic Interpolator */
    private static javax.media.jai.InterpolationBicubic interpBicOld;

    /** New bicubic Interpolator */
    private static InterpolationBicubic interpBicNew;

    /** Image to elaborate */
    private static RenderedImage image;

    /** RenderingHints used for selecting the borderExtender */
    private static RenderingHints hints;

    @BeforeClass
    public static void initialSetup() throws FileNotFoundException, IOException {
        int dataType = DataBuffer.TYPE_BYTE;

        // Selection of the RGB image
        ParameterBlockJAI pbj = new ParameterBlockJAI("ImageRead");
        // String file = "../jt-utilities/src/test/resources/it/geosolutions/jaiext/images/testImageLittle.tif";
        File file = TestData.file(ComparisonTest.class, "testImageLittle.tif");
        pbj.setParameter("Input", file);
        image = JAI.create("ImageRead", pbj);

        hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                BorderExtender.createInstance(BorderExtender.BORDER_COPY));

        //Range creation if selected
        Range rangeND= null;
        if(RANGE_USED){
            rangeND = RangeFactory.create((byte)100,true,(byte)100,true);
        }
        
        
        // Interpolators instantiation
        interpNearOld = new javax.media.jai.InterpolationNearest();
        interpNearNew = new InterpolationNearest(rangeND, false, destinationNoData, dataType);

        interpBilOld = new javax.media.jai.InterpolationBilinear(DEFAULT_SUBSAMPLE_BITS);
        interpBilNew = new InterpolationBilinear(DEFAULT_SUBSAMPLE_BITS, rangeND, false,
                destinationNoData, dataType);

        interpBicOld = new javax.media.jai.InterpolationBicubic(DEFAULT_SUBSAMPLE_BITS);
        interpBicNew = new InterpolationBicubic(DEFAULT_SUBSAMPLE_BITS, rangeND, false, dataType,
                dataType, false, DEFAULT_PRECISION_BITS);
        
        if(OLD_DESCRIPTOR){
            JAIExt.registerJAIDescriptor("Scale");
        }
    }

    @Test
    public void testNearestNewScaleDescriptor() {
        if (!OLD_DESCRIPTOR && TEST_SELECTOR == 0) {
            testInterpolators(interpNearNew, IMAGE_REDUCTION, OLD_DESCRIPTOR);
        }
    }

    @Test
    public void testNearestOldScaleDescriptor() {
        if (OLD_DESCRIPTOR && TEST_SELECTOR == 0) {
            testInterpolators(interpNearOld, IMAGE_REDUCTION, OLD_DESCRIPTOR);
        }
    }

    @Test
    public void testBilinearNewScaleDescriptor() {
        if (!OLD_DESCRIPTOR && TEST_SELECTOR == 1) {
            testInterpolators(interpBilNew, IMAGE_REDUCTION, OLD_DESCRIPTOR);
        }
    }

    @Test
    public void testBilinearOldScaleDescriptor() {
        if (OLD_DESCRIPTOR && TEST_SELECTOR == 1) {
            testInterpolators(interpBilOld, IMAGE_REDUCTION, OLD_DESCRIPTOR);
        }
    }

    @Test
    public void testBicubicNewScaleDescriptor() {
        if (!OLD_DESCRIPTOR && TEST_SELECTOR == 2) {
            testInterpolators(interpBicNew, IMAGE_REDUCTION, OLD_DESCRIPTOR);
        }
    }

    @Test
    public void testBicubicOldScaleDescriptor() {
        if (OLD_DESCRIPTOR && TEST_SELECTOR == 2) {
            testInterpolators(interpBicOld, IMAGE_REDUCTION, OLD_DESCRIPTOR);
        }
    }

    public void testInterpolators(Interpolation interp, boolean reductionBoolean, boolean old) {

        float scaleX;
        float scaleY;

        String reduction = "";

        if (reductionBoolean) {
            reduction = "Reduction";
            scaleX = 1 / xScale;
            scaleY = 1 / yScale;
        } else {
            reduction = "Magnification";
            scaleX = xScale;
            scaleY = yScale;
        }

        String description = "";

        if (old) {
            description = "Old Scale";
            if (NATIVE_ACCELERATION) {
                description += " accelerated ";
                System.setProperty("com.sun.media.jai.disableMediaLib", "false");
            } else {
                System.setProperty("com.sun.media.jai.disableMediaLib", "true");
            }
        } else {
            description = "New Scale";
            System.setProperty("com.sun.media.jai.disableMediaLib", "true");
        }

        String interpType = "";

        if (interp instanceof InterpolationBilinear
                || interp instanceof javax.media.jai.InterpolationBilinear) {
            interpType = "Bilinear";
        } else if (interp instanceof InterpolationBicubic
                || interp instanceof javax.media.jai.InterpolationBicubic) {
            interpType = "Bicubic";
        } else if (interp instanceof InterpolationNearest
                || interp instanceof javax.media.jai.InterpolationNearest) {
            interpType = "Nearest";
        }
        // Total cycles number
        int totalCycles = BENCHMARK_ITERATION + NOT_BENCHMARK_ITERATION;
        // Image with the interpolator
        PlanarImage imageScale = null;

        long mean = 0;
        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;

        // Cycle for calculating the mean, maximum and minimum calculation time
        for (int i = 0; i < totalCycles; i++) {

            // creation of the image with the selected interpolator
            if (old) {
                imageScale = javax.media.jai.operator.ScaleDescriptor.create(image, scaleX, scaleY,
                        xTrans, yTrans, interp, hints);
            } else {
                imageScale = ScaleDescriptor.create(image, scaleX, scaleY, xTrans, yTrans, interp,
                        null, false, null, null, hints);
            }

            // Total calculation time
            long start = System.nanoTime();
            imageScale.getTiles();
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
            // For every cycle the cache is flushed such that all the tiles must be recalculates
            JAI.getDefaultInstance().getTileCache().flush();
        }
        // Mean values
        double meanValue = mean / BENCHMARK_ITERATION * 1E-6;

        // Max and Min values stored as double
        double maxD = max * 1E-6;
        double minD = min * 1E-6;
        // Comparison between the mean times
        System.out.println("\n" + reduction);
        // Output print of the
        System.out.println("\n" + interpType);
        System.out.println("\nMean value for " + description + "Descriptor : " + meanValue
                + " msec.");
        System.out.println("Maximum value for " + description + "Descriptor : " + maxD + " msec.");
        System.out.println("Minimum value for " + description + "Descriptor : " + minD + " msec.");
        // Final Image disposal
        if (imageScale instanceof RenderedOp) {
            ((RenderedOp) imageScale).dispose();
        }
    }
}
