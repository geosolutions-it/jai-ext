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
package it.geosolutions.jaiext.mosaic;

import it.geosolutions.jaiext.JAIExt;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;

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
 * The MosaicType can be chosen by setting the JAI.Ext.MosaicBlend boolean JVM parameter: false for OVERLAY, true for BLEND.
 * The selection of the old or new descriptor must be done by setting to true or false the JVM parameter JAI.Ext.OldDescriptor.
 * If the user wants to use the accelerated code, the JVM parameter JAI.Ext.Acceleration must be set to true.
 */

public class ComparisonTest extends TestBase{
    /** Number of benchmark iterations (Default 1) */
    private final static Integer BENCHMARK_ITERATION = Integer.getInteger(
            "JAI.Ext.BenchmarkCycles", 1);

    /** Number of not benchmark iterations (Default 0) */
    private final static int NOT_BENCHMARK_ITERATION = Integer.getInteger(
            "JAI.Ext.NotBenchmarkCycles", 0);
    
    /** Boolean indicating if the old descriptor must be used */
    private final static boolean OLD_DESCRIPTOR = Boolean.getBoolean("JAI.Ext.OldDescriptor");
    
	/** Boolean indicating if the native acceleration must be used */
	private final static boolean NATIVE_ACCELERATION = Boolean
			.getBoolean("JAI.Ext.Acceleration");
    
    /** Boolean for selecting one of the 2 MosaicType(Default Overlay) */
    private final static boolean MOSAIC_TYPE = Boolean.getBoolean(
            "JAI.Ext.MosaicBlend");
    
    /** Boolean indicating if a No Data Range must be used */
    private final static boolean RANGE_USED = Boolean.getBoolean("JAI.Ext.RangeUsed");

    /** Value indicating No Data for the destination image */
    private static double destinationNoData = 0;

    /** Image to elaborate */
    private static RenderedImage[] images;

    /** RenderingHints used for selecting the borderExtender */
    private static RenderingHints hints;

	private static Range[] rangeND;

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
        
        //Range creation if selected
        rangeND= null;
        if(RANGE_USED){
            Range range = RangeFactory.create((byte)100,true,(byte)100,true);
            rangeND = new Range[]{range, range};
        }
        if(OLD_DESCRIPTOR){
            JAIExt.registerJAIDescriptor("Mosaic");
        }
    }

    @Test
    public void testNearestNewMosaicDescriptor() {
    	if(!OLD_DESCRIPTOR){
    		testMosaic(OLD_DESCRIPTOR, MOSAIC_TYPE);
    	}
    }

    @Test   
    public void testNearestOldMosaicDescriptor() {
    	if(OLD_DESCRIPTOR){
    		testMosaic(OLD_DESCRIPTOR, MOSAIC_TYPE);
    	}
    }

    public void testMosaic(boolean old, boolean blend) {

        MosaicType mosaicType;

        String description = "";

        if (old) {
            description = "Old Mosaic";
			if(NATIVE_ACCELERATION){
				description+=" accelerated ";   
				System.setProperty("com.sun.media.jai.disableMediaLib", "false");
			}else{
				System.setProperty("com.sun.media.jai.disableMediaLib", "true");
			}
        } else {
            description = "New Mosaic";
            System.setProperty("com.sun.media.jai.disableMediaLib", "true");
        }

        String mosaic = "";

        if (!blend) {
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

                imageMosaic = MosaicDescriptor.create(images, mosaicType, null, null, null, destnodata, rangeND, hints);
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
