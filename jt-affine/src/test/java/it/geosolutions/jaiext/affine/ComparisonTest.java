package it.geosolutions.jaiext.affine;

import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
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
import javax.media.jai.RenderedOp;

import org.geotools.test.TestData;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test class is used for compare the timing between the new
 * Nearest,Bilinear and Bicubic interpolators and their JAI version on the
 * affine operation. No Roi or No Data range are used. If the user wants to
 * change the number of the benchmark cycles or of the not benchmark cycles,
 * should only pass the new values to the JAI.Ext.BenchmarkCycles or
 * JAI.Ext.NotBenchmarkCycles parameters. The tests are quite different because
 * the interpolator used can be one of the 3 JAI interpolators but the other
 * operations are similar:
 * <ul>
 * <li>Selection of the descriptor (new or old AffineDescriptor)</li>
 * <li>Image Transformation</li>
 * <li>statistic calculation (if the cycle belongs to the benchmark cycles).</li>
 * </ul>
 * The selection of the old or new descriptor must be done by setting to true or
 * false the JVM parameter JAI.Ext.OldDescriptor. The interpolator can be
 * chosen by passing the JAI.Ext.TestSelector Integer JVM parameter: 0 for
 * nearest interpolation, 1 for bilinear, 2 for bicubic. The transformation used 
 * is selected by passing the JVM integral parameter JAI.Ext.TransformationSelector,
 * with 0 that indicates rotation, 1 scale, 2 combination of them. If the user wants 
 * to use the accelerated code, the JVM parameter JAI.Ext.Acceleration must be set to true.
 */
public class ComparisonTest {

	/** Number of benchmark iterations (Default 1) */
	private final static int BENCHMARK_ITERATION = Integer.getInteger(
			"JAI.Ext.BenchmarkCycles", 1);

	/** Number of not benchmark iterations (Default 0) */
	private final static int NOT_BENCHMARK_ITERATION = Integer.getInteger(
			"JAI.Ext.NotBenchmarkCycles", 0);

	/** Boolean indicating if the old descriptor must be used */
	private final static boolean OLD_DESCRIPTOR = Boolean
			.getBoolean("JAI.Ext.OldDescriptor");
	
	/** Boolean indicating if the native acceleration must be used */
	private final static boolean NATIVE_ACCELERATION = Boolean
			.getBoolean("JAI.Ext.Acceleration");
	
    /** Integer indicating which operation should be used (Default 0)*/
    public static Integer TRANSFORMATION_SELECTOR = Integer
            .getInteger("JAI.Ext.TransformationSelector",0);

	/** Index for selecting one of the 3 interpolators(Default 0) */
	private final static int TEST_SELECTOR = Integer.getInteger(
			"JAI.Ext.TestSelector", 0);

	/** Default subsampling bits used for the bilinear and bicubic interpolation */
	private final static int DEFAULT_SUBSAMPLE_BITS = 8;

	/** Default precision bits used in the bicubic calculation */
	private final static int DEFAULT_PRECISION_BITS = 8;

	/** Value indicating No Data for the destination image */
	private static double destinationNoData = 0;

	/** Rotation used */
	private static AffineTransform rotateTransform;
	
	/** Translation used */
	private static AffineTransform translateTransform;
	
	/** Scale used */
	private static AffineTransform scaleTransform;

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

		// Interpolators instantiation
		interpNearOld = new javax.media.jai.InterpolationNearest();
		interpNearNew = new InterpolationNearest(null, false,
				destinationNoData, dataType);

		interpBilOld = new javax.media.jai.InterpolationBilinear(
				DEFAULT_SUBSAMPLE_BITS);
		interpBilNew = new InterpolationBilinear(DEFAULT_SUBSAMPLE_BITS, null,
				false, destinationNoData, dataType);

		interpBicOld = new javax.media.jai.InterpolationBicubic(
				DEFAULT_SUBSAMPLE_BITS);
		interpBicNew = new InterpolationBicubic(DEFAULT_SUBSAMPLE_BITS, null,
				false, destinationNoData, dataType, false,
				DEFAULT_PRECISION_BITS);

		// Selection of the RGB image
		ParameterBlockJAI pbj = new ParameterBlockJAI("ImageRead");
		// String file =
		// "../jt-utilities/src/test/resources/it/geosolutions/jaiext/images/testImageLittle.tif";
		File file = TestData.file(ComparisonTest.class, "testImageLittle.tif");
		pbj.setParameter("Input", file);
		image = JAI.create("ImageRead", pbj);

		hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
				BorderExtender.createInstance(BorderExtender.BORDER_COPY));

		// 45� degrees rotation
		double theta = Math.PI / 4;
		rotateTransform = AffineTransform.getRotateInstance(theta);
		// 100 px translation
		translateTransform = AffineTransform.getTranslateInstance(100, 0);
		// 2 x scale
		scaleTransform=AffineTransform.getScaleInstance(1.5f, 1.5f);
	}

	@Test
	public void testNearestNewAffineDescriptor() {
		if (TEST_SELECTOR == 0 && !OLD_DESCRIPTOR) {
			testInterpolators(interpNearNew, OLD_DESCRIPTOR);
		}
	}

	@Test
	public void testNearestOldAffineDescriptor() {
		if (TEST_SELECTOR == 0 && OLD_DESCRIPTOR) {
			testInterpolators(interpNearOld, OLD_DESCRIPTOR);
		}
	}

	@Test
	public void testBilinearNewAffineDescriptor() {
		if (TEST_SELECTOR == 1 && !OLD_DESCRIPTOR) {
			testInterpolators(interpBilNew, OLD_DESCRIPTOR);
		}
	}

	@Test
	public void testBilinearOldAffineDescriptor() {
		if (TEST_SELECTOR == 1 && OLD_DESCRIPTOR) {
			testInterpolators(interpBilOld, OLD_DESCRIPTOR);
		}
	}

	@Test
	public void testBicubicNewAffineDescriptor() {
		if (TEST_SELECTOR == 2 && !OLD_DESCRIPTOR) {
			testInterpolators(interpBicNew, OLD_DESCRIPTOR);
		}
	}

	@Test
	public void testBicubicOldAffineDescriptor() {
		if (TEST_SELECTOR == 2 && OLD_DESCRIPTOR) {
			testInterpolators(interpBicOld, OLD_DESCRIPTOR);
		}
	}

	public void testInterpolators(Interpolation interp, boolean old) {

		String interpType = "";

		if (interp instanceof javax.media.jai.InterpolationBilinear
				|| interp instanceof InterpolationBilinear) {
			interpType = "Bilinear";
		} else if (interp instanceof javax.media.jai.InterpolationBicubic
				|| interp instanceof InterpolationBicubic) {
			interpType = "Bicubic";
		} else if (interp instanceof javax.media.jai.InterpolationNearest
				|| interp instanceof InterpolationNearest) {
			interpType = "Nearest";
		}

		String description = "";

		if (old) {
			description = "Old Affine";
			if(NATIVE_ACCELERATION){
				description+=" accelerated ";   
				System.setProperty("com.sun.media.jai.disableMediaLib", "false");
			}else{
				System.setProperty("com.sun.media.jai.disableMediaLib", "true");
			}
		} else {
			description = "New Affine";
			System.setProperty("com.sun.media.jai.disableMediaLib", "true");
		}

		AffineTransform transform = new AffineTransform();
		
		
		switch (TRANSFORMATION_SELECTOR) {
		case 0:
			transform.concatenate(rotateTransform);
			break;
		case 1:
			transform.concatenate(scaleTransform);
			break;
		case 2:
			transform.concatenate(rotateTransform);
			transform.concatenate(scaleTransform);
			transform.concatenate(translateTransform);
			break;
		default:
			throw new IllegalArgumentException("Wrong transformation value");
		}
		
		
		// Destination no data used by the affine operation with the classic
		// bilinear interpolator
		double[] destinationNoDataArray = { destinationNoData,
				destinationNoData, destinationNoData };
		// Total cycles number
		int totalCycles = BENCHMARK_ITERATION + NOT_BENCHMARK_ITERATION;
		// Image with the interpolator
		PlanarImage imageAffine = null;

		long mean = 0;
		long max = Long.MIN_VALUE;
		long min = Long.MAX_VALUE;

		// Cycle for calculating the mean, maximum and minimum calculation time
		for (int i = 0; i < totalCycles; i++) {

			// creation of the image with the selected interpolator

			if (old) {
				imageAffine = javax.media.jai.operator.AffineDescriptor
						.create(image, transform, interp,
								destinationNoDataArray, hints);
			} else {
				imageAffine = AffineDescriptor.create(image, transform, interp,
						destinationNoDataArray, null, false, false, hints);
			}

			// Total calculation time
			long start = System.nanoTime();
			imageAffine.getTiles();
			long end = System.nanoTime() - start;

			// If the the first NOT_BENCHMARK_ITERATION cycles has been done,
			// then the mean, maximum and minimum values are stored
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
			// For every cycle the cache is flushed such that all the tiles must
			// be recalculates
			JAI.getDefaultInstance().getTileCache().flush();
		}
		// Mean values
		double meanValue = mean / BENCHMARK_ITERATION * 1E-6;

		// Max and Min values stored as double
		double maxD = max * 1E-6;
		double minD = min * 1E-6;
		// Comparison between the mean times
			// Output print of the
			System.out.println("\n" + interpType);
			System.out.println("\nMean value for " + description
					+ "Descriptor : " + meanValue + " msec.");
			System.out.println("Maximum value for " + description
					+ "Descriptor : " + maxD + " msec.");
			System.out.println("Minimum value for " + description
					+ "Descriptor : " + minD + " msec.");
		
		// Final Image disposal
		if (imageAffine instanceof RenderedOp) {
			((RenderedOp) imageAffine).dispose();
		}

	}
}
