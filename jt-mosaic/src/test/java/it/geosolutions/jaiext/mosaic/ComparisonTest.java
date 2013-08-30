package it.geosolutions.jaiext.mosaic;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.MosaicType;
import javax.media.jai.operator.NullDescriptor;
import javax.media.jai.operator.TranslateDescriptor;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * This test class is used for compare the timing between the new MosaicDescriptor and . No Roi, No Data range or Alpha channels are used. If the user
 * wants to change the number of the benchmark cycles or of the not benchmark cycles, should only pass the new values to the JAI.Ext.BenchmarkCycles
 * or JAI.Ext.NotBenchmarkCycles parameters. Inside this test class the tests are executed in the same manner:
 * <ul>
 * <li>Selection of the descriptor (New or Old MosaicDescriptor)</li>
 * <li>Selection of the mosaic type (Overlay or Blend)</li>
 * <li>statistic calculation (if the cycle belongs to the benchmark cycles)</li>
 * </ul>
 * 
 */

public class ComparisonTest {
    /** Number of benchmark iterations (Default 1) */
    private final static Integer BENCHMARK_ITERATION = Integer.getInteger(
            "JAI.Ext.BenchmarkCycles", 1);

    /** Number of not benchmark iterations (Default 0) */
    private final static int NOT_BENCHMARK_ITERATION = Integer.getInteger(
            "JAI.Ext.NotBenchmarkCycles", 0);

    /** Value indicating No Data for the destination image */
    private static double destinationNoData = 0;

    /** Image to elaborate */
    private static RenderedImage[] images;

    /** RenderingHints used for selecting the borderExtender */
    private static RenderingHints hints;
    
    private static ImageMosaicBean[] beanArray = new ImageMosaicBean[2];

    @BeforeClass
    public static void initialSetup() {
        // Selection of the images
        // image creation
        byte value1 = 50;
        byte value2 = 100;
        RenderedImage image1 = getSyntheticImage(value1);
        RenderedImage image2 = getSyntheticImage(value2);
        int width = image1.getWidth();
        // layout creation (same height of the source images, doubled width)
        ImageLayout layout = new ImageLayout(0, 0, image1.getWidth() + image2.getWidth(),
                image1.getHeight());

        hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);

        // translation of the first image
        RenderedImage image3 = TranslateDescriptor.create(image1, width * 0.1F, 0F, null, hints);
        // No op for the second image
        RenderedImage image4 = NullDescriptor.create(image2, hints);
        // array creation
        images = new RenderedImage[2];
        images[0] = image4;
        images[1] = image3;
        
        ImageMosaicBean bean0 = new ImageMosaicBean();
        bean0.setImage(images[0]);
        ImageMosaicBean bean1 = new ImageMosaicBean();
        bean1.setImage(images[1]);
        
        beanArray[0] = bean0;
        beanArray[1] = bean1;
        
    }

    @Test
    public void testNearestNewMosaicDescriptorOverlay() {
        testMosaic(false, true);
    }

    @Test   
    public void testNearestOldMosaicDescriptorOverlay() {
        testMosaic(true, true);
    }

    @Test
    @Ignore
    public void testNearestNewMosaicDescriptorBlend() {
        testMosaic(false, false);
    }

    @Test
    @Ignore
    public void testNearestOldMosaicDescriptorBlend() {
        testMosaic(true, false);
    }

    public void testMosaic(boolean old, boolean overlay) {

        MosaicType mosaicType;

        String description = "";

        if (old) {
            description = "Old Mosaic";
            System.setProperty("com.sun.media.jai.disableMediaLib", "false");
        } else {
            description = "New Mosaic";
            System.setProperty("com.sun.media.jai.disableMediaLib", "true");
        }

        String mosaic = "";

        if (overlay) {
            mosaicType = javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY;
            mosaic = "Mosaic Type Overlay";
        } else {
            mosaicType = javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_BLEND;
            mosaic = "Mosaic Type Blend";
        }

        // Total cycles number
        int totalCycles = BENCHMARK_ITERATION + NOT_BENCHMARK_ITERATION;
        // Image with the interpolator
        PlanarImage imageMosaic = null;

        long mean = 0;
        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;

        // Cycle for calculating the mean, maximum and minimum calculation time
        for (int i = 0; i < totalCycles; i++) {

            // creation of the image with the selected interpolator

            if (old) {
                // background values and threshold
                double[] background = { destinationNoData, destinationNoData };
                double[][] threshold = { { 0 }, { 0 } };
                imageMosaic = javax.media.jai.operator.MosaicDescriptor.create(images, mosaicType,
                        null, null, threshold, background, hints);
            } else {


                double[] destnodata = { destinationNoData, destinationNoData };

                imageMosaic = MosaicDescriptor.create(images, beanArray, mosaicType, destnodata, hints);
            }

            // Total calculation time
            long start = System.nanoTime();
            imageMosaic.getTiles();
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
        System.out.println("\n" + mosaic);
        // Output print of the
        System.out.println("\nMean value for " + description + "Descriptor : " + meanValue
                + " msec.");
        System.out.println("Maximum value for " + description + "Descriptor : " + maxD + " msec.");
        System.out.println("Minimum value for " + description + "Descriptor : " + minD + " msec.");

        //Final Image disposal
        if(imageMosaic instanceof RenderedOp){
            ((RenderedOp)imageMosaic).dispose();
        }
        
    }

    public static RenderedImage getSyntheticImage(byte value) {
        final float width = 512;
        final float height = 512;
        ParameterBlock pb = new ParameterBlock();
        Byte[] array = new Byte[] { value, (byte) (value + 1), (byte) (value + 2) };
        pb.add(width);
        pb.add(height);
        pb.add(array);
        // Create the constant operation.
        return JAI.create("constant", pb);
    }
}
