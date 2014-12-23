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
package it.geosolutions.jaiext.translate;

import it.geosolutions.jaiext.JAIExt;

import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;

import org.junit.BeforeClass;
import org.junit.Test;


/**
 * This test class is used for compare the timing between the new and the old versions of the translate descriptor . If the user wants to change the
 * number of the benchmark cycles or of the not benchmark cycles, should only pass the new values to the JAI.Ext.BenchmarkCycles or
 * JAI.Ext.NotBenchmarkCycles parameters. Inside this test class the 2 tests are executed in the same manner:
 * <ul>
 * <li>Selection of the Descriptor</li>
 * <li>Image Translation</li>
 * <li>statistic calculation (if the cycle belongs to the benchmark cycles)</li>
 * </ul>
 * 
 * The selection of the old or new descriptor must be done by setting to true or false the JVM parameter JAI.Ext.OldDescriptor.
 * 
 */
public class ComparisonTest {

    /** Number of benchmark iterations (Default 1) */
    private final static Integer BENCHMARK_ITERATION = Integer.getInteger(
            "JAI.Ext.BenchmarkCycles", 1);

    /** Number of not benchmark iterations (Default 0) */
    private final static int NOT_BENCHMARK_ITERATION = Integer.getInteger(
            "JAI.Ext.NotBenchmarkCycles", 0);

    /** Boolean indicating if the old descriptor must be used */
    private final static boolean OLD_DESCRIPTOR = Boolean.getBoolean("JAI.Ext.OldDescriptor");
    
    /** Image to elaborate */
    private static RenderedImage image;

    /** X translation parameter */
    private static float transX;

    /** Y translation parameter */
    private static float transY;

    /** JAI nearest Interpolator */
    private static javax.media.jai.InterpolationNearest interpNearOld;

    @BeforeClass
    public static void initialSetup() throws FileNotFoundException, IOException {
        // Selection of the image
        image = getSyntheticImage((byte) 100);

        // Interpolators instantiation
        interpNearOld = new javax.media.jai.InterpolationNearest();

        if(OLD_DESCRIPTOR){
            JAIExt.registerJAIDescriptor("Translate");
        }
    }

    @Test
    public void testNewTranslationDescriptor() {
        if (!OLD_DESCRIPTOR) {
        	testTranslation(null);
        }
    }

    @Test
    public void testOldTranslationDescriptor() {
    	if (OLD_DESCRIPTOR) {
    		testTranslation(interpNearOld);
    	}
    }

    public void testTranslation(Interpolation interp) {

        String description = "";

        boolean old = interp != null;

        if (old) {
            description = "Old Translate";
            System.setProperty("com.sun.media.jai.disableMediaLib", "false");
        } else {
            description = "New Translate";
            System.setProperty("com.sun.media.jai.disableMediaLib", "true");
        }
        
        // Total cycles number
        int totalCycles = BENCHMARK_ITERATION + NOT_BENCHMARK_ITERATION;
        // Image
        PlanarImage imageTranslate = null;

        long mean = 0;
        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;

        // Cycle for calculating the mean, maximum and minimum calculation time
        for (int i = 0; i < totalCycles; i++) {

            // creation of the image with the selected interpolator
            if (old) {
                imageTranslate = javax.media.jai.operator.TranslateDescriptor.create(image, transX,
                        transY, interp, null);
            } else {
                imageTranslate = TranslateDescriptor.create(image, transX, transY, null, null);
            }

            // Total calculation time
            long start = System.nanoTime();
            imageTranslate.getTiles();
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
        // Output print of the
        System.out.println("\nMean value for " + description + "Descriptor : " + meanValue
                + " msec.");
        System.out.println("Maximum value for " + description + "Descriptor : " + maxD + " msec.");
        System.out.println("Minimum value for " + description + "Descriptor : " + minD + " msec.");
        
        //Final Image disposal
        if(imageTranslate instanceof RenderedOp){
            ((RenderedOp)imageTranslate).dispose();
        }

    }

    public static RenderedImage getSyntheticImage(byte value) {
        final float width = 256;
        final float height = 256;
        ParameterBlock pb = new ParameterBlock();
        Byte[] array = new Byte[] { value, (byte) (value + 1), (byte) (value + 2) };
        pb.add(width);
        pb.add(height);
        pb.add(array);
        // Create the constant operation.
        return JAI.create("constant", pb);
    }

}
