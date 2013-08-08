package it.geosolutions.jaiext.affine;

import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import it.geosolutions.jaiext.interpolators.InterpolationBicubicNew;
import it.geosolutions.jaiext.interpolators.InterpolationBilinearNew;
import it.geosolutions.jaiext.interpolators.InterpolationNearestNew;

import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationBicubic;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.AffineDescriptor;

import org.geotools.test.TestData;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test class is used for compare the timing between the new Nearest,Bilinear and Bicubic interpolators and their JAI version on the affine
 * operation. No Roi or No Data range are used. If the user wants to change the number of the benchmark cycles or of the not benchmark cycles, should
 * only pass the new values to the JAI.Ext.BenchmarkCycles or JAI.Ext.NotBenchmarkCycles parameters. Inside this test class the first 6 tests are
 * executed in the same manner:
 * <ul>
 * <li>Selection of the interpolator (Standard or New)</li>
 * <li>Image Rotation</li>
 * <li>statistic calculation (if the cycle belongs to the benchmark cycles)</li>
 * </ul>
 * 
 * The second 6 tests are quite different because the interpolator used is always one of the 3 JAI interpolators but the other operations are similar:
 * <ul>
 * </ul>
 * <ul>
 * <li>Selection of the descriptor (AffineDescriptor or AffineDataDescriptor)</li>
 * <li>Image Rotation</li>
 * <li>statistic calculation (if the cycle belongs to the benchmark cycles)</li>
 * </ul>
 * 
 */
public class ComparisonTest {

    /** Number of benchmark iterations (Default 1) */
    private final static int BENCHMARK_ITERATION = Integer.getInteger("JAI.Ext.BenchmarkCycles", 1);

    /** Number of not benchmark iterations (Default 0) */
    private final static int NOT_BENCHMARK_ITERATION = Integer.getInteger(
            "JAI.Ext.NotBenchmarkCycles", 0);

    /** Default subsampling bits used for the bilinear and bicubic interpolation */
    private final static int DEFAULT_SUBSAMPLE_BITS = 8;

    /** Default precision bits used in the bicubic calculation */
    private final static int DEFAULT_PRECISION_BITS = 8;

    /** Value indicating No Data for the destination image */
    private static double destinationNoData = 0;

    /** Transformation used */
    private static AffineTransform transform;

    /** JAI nearest Interpolator */
    private static InterpolationNearest interpNearOld;

    /** New nearest Interpolator */
    private static InterpolationNearestNew interpNearNew;

    /** JAI bilinear Interpolator */
    private static InterpolationBilinear interpBilOld;

    /** New bilinear Interpolator */
    private static InterpolationBilinearNew interpBilNew;

    /** JAI bicubic Interpolator */
    private static InterpolationBicubic interpBicOld;

    /** New bicubic Interpolator */
    private static InterpolationBicubicNew interpBicNew;

    /** Image to elaborate */
    private static RenderedImage image;

    /** RenderingHints used for selecting the borderExtender */
    private static RenderingHints hints;

    @BeforeClass
    public static void initialSetup() throws FileNotFoundException, IOException {
        int dataType = DataBuffer.TYPE_BYTE;

        // Interpolators instantiation
        interpNearOld = new InterpolationNearest();
        interpNearNew = new InterpolationNearestNew(null, false, destinationNoData, dataType);

        interpBilOld = new InterpolationBilinear(DEFAULT_SUBSAMPLE_BITS);
        interpBilNew = new InterpolationBilinearNew(DEFAULT_SUBSAMPLE_BITS, null, false,
                destinationNoData, dataType);

        interpBicOld = new InterpolationBicubic(DEFAULT_SUBSAMPLE_BITS);
        interpBicNew = new InterpolationBicubicNew(DEFAULT_SUBSAMPLE_BITS, null, false, dataType,
                dataType, false, DEFAULT_PRECISION_BITS);

        // Selection of the RGB image
        ParameterBlockJAI pbj = new ParameterBlockJAI("ImageRead");
        //String file = "../jt-utilities/src/test/resources/it/geosolutions/jaiext/images/testImageLittle.tif";
        File file = TestData.file(ComparisonTest.class, "testImageLittle.tif");
        pbj.setParameter("Input", file);
        image = JAI.create("ImageRead", pbj);

        hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                BorderExtender.createInstance(BorderExtender.BORDER_COPY));

        // 45° degrees rotation
        double theta = Math.PI / 4;
        transform = AffineTransform.getRotateInstance(theta);
    }

    @Test
    public void testNearestInterp() {
        testInterpolators(interpNearNew, false, false);
    }

    @Test
    public void testNearestInterpStandard() {
        testInterpolators(interpNearOld, false, true);
    }

    @Test
    public void testBilinearInterp() {
        testInterpolators(interpBilNew, false, false);
    }

    @Test
    public void testBilinearInterpStandard() {
        testInterpolators(interpBilNew, false, true);
    }

    @Test
    public void testBicubicInterp() {
        testInterpolators(interpBicNew, false, false);
    }

    @Test
    public void testBicubicInterpStandard() {
        testInterpolators(interpBicOld, false, true);
    }

    @Test
    public void testNearestScaleDataDescriptor() {
        testInterpolators(interpNearOld, true, false);
    }

    @Test
    public void testNearestScaleDescriptor() {
        testInterpolators(interpNearOld, true, true);
    }

    @Test
    public void testBilinearScaleDataDescriptor() {
        testInterpolators(interpBilOld, true, false);
    }

    @Test
    public void testBilinearScaleDescriptor() {
        testInterpolators(interpBilOld, true, true);
    }

    @Test
    public void testBicubicScaleDataDescriptor() {
        testInterpolators(interpBicOld, true, false);
    }

    @Test
    public void testBicubicScaleDescriptor() {
        testInterpolators(interpBicOld, true, true);
    }

    public void testInterpolators(Interpolation interp, boolean testDescriptor, boolean old) {

        String interpType = "";

        if (interp instanceof InterpolationBilinear || interp instanceof InterpolationBilinearNew) {
            interpType = "Bilinear";
        } else if (interp instanceof InterpolationBicubic
                || interp instanceof InterpolationBicubicNew) {
            interpType = "Bicubic";
        } else if (interp instanceof InterpolationNearest
                || interp instanceof InterpolationNearestNew) {
            interpType = "Nearest";
        }

        String description = "";

        if (testDescriptor) {
            if (old) {
                description = "Affine";
            } else {
                description = "AffineData";
            }
        } else {
            if (!old) {
                description = "New";
            }
        }

        // Destination no data used by the affine operation with the classic bilinear interpolator
        double[] destinationNoDataArray = { destinationNoData, destinationNoData, destinationNoData };
        // Total cycles number
        int totalCycles = BENCHMARK_ITERATION + NOT_BENCHMARK_ITERATION;
        // Image with the interpolator
        PlanarImage imageAffine;

        long mean = 0;
        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;

        // Cycle for calculating the mean, maximum and minimum calculation time
        for (int i = 0; i < totalCycles; i++) {

            // creation of the image with the selected interpolator
            if (testDescriptor) {
                if (old) {
                    imageAffine = AffineDescriptor.create(image, transform, interp,
                            destinationNoDataArray, hints);
                } else {
                    imageAffine = AffineDataDescriptor.create(image, transform, interp,
                            destinationNoDataArray, null, false, false, hints);
                }
            } else {
                if (old) {
                    imageAffine = AffineDataDescriptor.create(image, transform, interp,
                            destinationNoDataArray, null, false, false, hints);
                } else {
                    imageAffine = AffineDataDescriptor.create(image, transform, interp, null, null,
                            false, false, hints);
                }
            }
            // Total calculation time
            long start = System.nanoTime();
            imageAffine.getTiles();
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
        double meanValue = mean / BENCHMARK_ITERATION * 1E-9;

        // Max and Min values stored as double
        double maxD = max * 1E-9;
        double minD = min * 1E-9;
        // Comparison between the mean times
        if (testDescriptor) {
            // Output print of the
            System.out.println("\n" + interpType);
            System.out.println("\nMean value for " + description + "Descriptor : " + meanValue
                    + " sec.");
            System.out.println("Maximum value for " + description + "Descriptor : " + maxD
                    + " sec.");
            System.out.println("Minimum value for " + description + "Descriptor : " + minD
                    + " sec.");
        } else {
            // Output print of the
            System.out.println("\nMean value for Interpolator" + interpType + description + " : "
                    + meanValue + " sec.");
            System.out.println("Maximum value for Interpolator" + interpType + description + " : "
                    + maxD + " sec.");
            System.out.println("Minimum value for Interpolator" + interpType + description + " : "
                    + minD + " sec.");
        }

    }
}
