package it.geosolutions.jaiext.scale;

import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import it.geosolutions.jaiext.interpolators.InterpolationBicubic;
import it.geosolutions.jaiext.interpolators.InterpolationBilinear;
import it.geosolutions.jaiext.interpolators.InterpolationNearest;

import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;

import org.geotools.test.TestData;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * This test class is used for compare the timing between the new Nearest,Bilinear and Bicubic interpolators and their JAI version on the scale
 * operation. No Roi or No Data range are used. If the user wants to change the number of the benchmark cycles or of the not benchmark cycles, should
 * only pass the new values to the JAI.Ext.BenchmarkCycles or JAI.Ext.NotBenchmarkCycles parameters. Inside this test class the first 12 tests are
 * executed in the same manner:
 * <ul>
 * <li>Selection of the interpolator (Standard or New)</li>
 * <li>Image Magnification\Reduction</li>
 * <li>statistic calculation (if the cycle belongs to the benchmark cycles)</li>
 * </ul>
 * 
 * The second 12 tests are quite different because the interpolator used is always one of the 3 JAI interpolators but the other operations are
 * similar:
 * <ul>
 * </ul>
 * <ul>
 * <li>Selection of the descriptor (new ScaleDescriptor or old ScaleDescriptor)</li>
 * <li>Image Magnification\Reduction</li>
 * <li>statistic calculation (if the cycle belongs to the benchmark cycles)</li>
 * </ul>
 * 
 */
public class ComparisonTest{

    /** Number of benchmark iterations (Default 1) */
    private final static Integer BENCHMARK_ITERATION = Integer.getInteger(
            "JAI.Ext.BenchmarkCycles", 1);

    /** Number of not benchmark iterations (Default 0) */
    private final static int NOT_BENCHMARK_ITERATION = Integer.getInteger(
            "JAI.Ext.NotBenchmarkCycles", 0);
    
    /** Default subsampling bits used for the bilinear and bicubic interpolation */
    private final static int DEFAULT_SUBSAMPLE_BITS = 8;

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
        //String file = "../jt-utilities/src/test/resources/it/geosolutions/jaiext/images/testImageLittle.tif";
        File file = TestData.file(ComparisonTest.class, "testImageLittle.tif");
        pbj.setParameter("Input", file);
        image = JAI.create("ImageRead", pbj);
        
        hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                BorderExtender.createInstance(BorderExtender.BORDER_COPY));
        
        // Interpolators instantiation
        interpNearOld = new javax.media.jai.InterpolationNearest();
        interpNearNew = new InterpolationNearest(null, false, destinationNoData, dataType);

        interpBilOld = new javax.media.jai.InterpolationBilinear(DEFAULT_SUBSAMPLE_BITS);
        interpBilNew = new InterpolationBilinear(DEFAULT_SUBSAMPLE_BITS, null, false,
                destinationNoData, dataType);

        interpBicOld = new javax.media.jai.InterpolationBicubic(DEFAULT_SUBSAMPLE_BITS);
        interpBicNew = new InterpolationBicubic(DEFAULT_SUBSAMPLE_BITS, null, false, dataType,
                dataType, false, DEFAULT_PRECISION_BITS);
    }

    // First 12 test. INTERPOLATOR TESTS
    @Test
    @Ignore
    public void testNearestInterp() {
        testInterpolators(interpNearNew, true, false, false);
    }

    @Test
    @Ignore
    public void testNearestInterpStandard() {
        testInterpolators(interpNearOld, true, false, true);
    }

    @Test
    @Ignore
    public void testBilinearInterp() {
        testInterpolators(interpBilNew, true, false, false);
    }

    @Test
    @Ignore
    public void testBilinearInterpStandard() {
        testInterpolators(interpBilOld, true, false, true);
    }

    @Test
    @Ignore
    public void testBicubicInterp() {
        testInterpolators(interpBicNew, true, false, false);
    }

    @Test
    @Ignore
    public void testBicubicInterpStandard() {
        testInterpolators(interpBicOld, true, false, true);
    }

    @Test
    @Ignore
    public void testNearestInterpReduction() {
        testInterpolators(interpNearNew, false, false, false);
    }

    @Test
    @Ignore
    public void testNearestInterpStandardReduction() {
        testInterpolators(interpNearOld, false, false, true);
    }

    @Test
    @Ignore
    public void testBilinearInterpReduction() {
        testInterpolators(interpBilNew, false, false, false);
    }

    @Test
    @Ignore
    public void testBilinearInterpStandardReduction() {
        testInterpolators(interpBilOld, false, false, true);
    }

    @Test
    @Ignore
    public void testBicubicInterpReduction() {
        testInterpolators(interpBicNew, false, false, false);
    }

    @Test
    @Ignore
    public void testBicubicInterpStandardReduction() {
        testInterpolators(interpBicOld, false, false, true);
    }

    // Last 12 test. DESCRIPTOR TESTS

    @Test
    @Ignore
    public void testNearestNewScaleDescriptor() {
        testInterpolators(interpNearNew, true, true, false);
    }

    @Test
    @Ignore
    public void testNearestOldScaleDescriptor() {
        testInterpolators(interpNearOld, true, true, true);
    }

    @Test
    @Ignore
    public void testBilinearNewScaleDescriptor() {
        testInterpolators(interpBilNew, true, true, false);
    }

    @Test
    @Ignore
    public void testBilinearOldScaleDescriptor() {
        testInterpolators(interpBilOld, true, true, true);
    }

    @Test
    public void testBicubicNewScaleDescriptor() {
        testInterpolators(interpBicNew, true, true, false);
    }

    @Test
    public void testBicubicOldScaleDescriptor() {
        testInterpolators(interpBicOld, true, true, true);
    }

    @Test
    @Ignore
    public void testNearestNewScaleDescriptorReduction() {
        testInterpolators(interpNearNew, false, true, false);
    }

    @Test
    @Ignore
    public void testNearestOldScaleDescriptorReduction() {
        testInterpolators(interpNearOld, false, true, true);
    }

    @Test
    @Ignore
    public void testBilinearNewScaleDescriptorReduction() {
        testInterpolators(interpBilNew, false, true, false);
    }

    @Test
    @Ignore
    public void testBilinearOldScaleDescriptorReduction() {
        testInterpolators(interpBilOld, false, true, true);
    }

    @Test
    @Ignore
    public void testBicubicNewScaleDescriptorReduction() {
        testInterpolators(interpBicNew, false, true, false);
    }

    @Test
    @Ignore
    public void testBicubicOldScaleDescriptorReduction() {
        testInterpolators(interpBicOld, false, true, true);
    }

    public void testInterpolators(Interpolation interp, boolean magnification,
            boolean testDescriptor, boolean old) {

        float scaleX;
        float scaleY;

        String reduction = "";

        if (!magnification) {
            reduction = "Reduction";
            scaleX = 1 / xScale;
            scaleY = 1 / yScale;
        } else {
            reduction = "Magnification";
            scaleX = xScale;
            scaleY = yScale;
        }

        String description = "";

        if (testDescriptor) {
            if (old) {
                description = "Old Scale";
            } else {
                description = "New Scale";
            }
        } else {
            if (!old) {
                description = "New";
            }
        }

        String interpType = "";

        if (interp instanceof InterpolationBilinear || interp instanceof InterpolationBilinear) {
            interpType = "Bilinear";
        } else if (interp instanceof InterpolationBicubic
                || interp instanceof InterpolationBicubic) {
            interpType = "Bicubic";
        } else if (interp instanceof InterpolationNearest
                || interp instanceof InterpolationNearest) {
            interpType = "Nearest";
        }
        // Total cycles number
        int totalCycles = BENCHMARK_ITERATION + NOT_BENCHMARK_ITERATION;
        // Image with the interpolator
        PlanarImage imageScale;
        
        long mean = 0;
        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;

        // Cycle for calculating the mean, maximum and minimum calculation time
        for (int i = 0; i < totalCycles; i++) {

            // creation of the image with the selected interpolator
            if (testDescriptor) {
                if (old) {
                    imageScale = javax.media.jai.operator.ScaleDescriptor.create(image, scaleX, scaleY, xTrans, yTrans,
                            interp, hints);
                } else {
                    imageScale = ScaleDescriptor.create(image, scaleX, scaleY, xTrans, yTrans,
                            interp, null, false, hints);
                }
            } else {
                imageScale = ScaleDescriptor.create(image, scaleX, scaleY, xTrans, yTrans,
                        interp, null, false, hints);
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
        if (testDescriptor) {
            // Output print of the
            System.out.println("\n" + interpType);
            System.out.println("\nMean value for " + description + "Descriptor : " + meanValue
                    + " msec.");
            System.out.println("Maximum value for " + description + "Descriptor : " + maxD
                    + " msec.");
            System.out.println("Minimum value for " + description + "Descriptor : " + minD
                    + " msec.");
        } else {
            // Output print of the
            System.out.println("\nMean value for Interpolator" + interpType + description + " : "
                    + meanValue + " msec.");
            System.out.println("Maximum value for Interpolator" + interpType + description + " : "
                    + maxD + " msec.");
            System.out.println("Minimum value for Interpolator" + interpType + description + " : "
                    + minD + " msec.");
        }

    }
}
