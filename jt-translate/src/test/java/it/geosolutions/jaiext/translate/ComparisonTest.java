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

import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;

import it.geosolutions.jaiext.testclasses.TestBase;
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
 * <p>
 * The selection of the old or new descriptor must be done by setting to true or false the JVM parameter JAI.Ext.OldDescriptor.
 *
 */
public class ComparisonTest extends TestBase {

    /**
     * X translation parameter
     */
    private static float transX;

    /**
     * Y translation parameter
     */
    private static float transY;

    /**
     * JAI nearest Interpolator
     */
    private static javax.media.jai.InterpolationNearest interpNearOld;

    @BeforeClass
    public static void initialSetup() throws FileNotFoundException, IOException {

        // Interpolators instantiation
        interpNearOld = new javax.media.jai.InterpolationNearest();

        if (OLD_DESCRIPTOR) {
            JAIExt.registerJAIDescriptor("Translate");
        }
    }

    @Test
    @Override
    public void testBase() {
        // Translate doesn't need to test all data types
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
        RenderedImage testImage = getSyntheticImage((byte) 100);
        PlanarImage image = null;

        // creation of the image with the selected interpolator
        if (OLD_DESCRIPTOR) {
            image = javax.media.jai.operator.TranslateDescriptor.create(testImage, transX,
                    transY, interp, null);
        } else {
            image = TranslateDescriptor.create(testImage, transX, transY, null, null);
        }

        finalizeTest(null, DataBuffer.TYPE_BYTE, image);
    }

    public static RenderedImage getSyntheticImage(byte value) {
        final float width = 256;
        final float height = 256;
        ParameterBlock pb = new ParameterBlock();
        Byte[] array = new Byte[]{value, (byte) (value + 1), (byte) (value + 2)};
        pb.add(width);
        pb.add(height);
        pb.add(array);
        // Create the constant operation.
        return JAI.create("constant", pb);
    }

}
