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
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.MosaicType;
import javax.media.jai.operator.NullDescriptor;
import javax.media.jai.operator.TranslateDescriptor;

import it.geosolutions.jaiext.utilities.TestImageDumper;
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

public class ComparisonTest extends TestBase {

    /**
     * Value indicating No Data for the destination image
     */
    private static double destinationNoData = 0;

    /**
     * Image to elaborate
     */
    private static RenderedImage[] images;

    /**
     * RenderingHints used for selecting the borderExtender
     */
    private static RenderingHints hints;

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

        if (OLD_DESCRIPTOR) {
            JAIExt.registerJAIDescriptor("Mosaic");
        }
    }

    @Test
    public void testNearestMosaicOverlayDescriptor() {
        testMosaic(javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY);
    }

    @Test
    public void testNearestMosaicBlendDescriptor() {
        testMosaic(javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_BLEND);
    }


    public void testMosaic(MosaicType mosaicType) {
        String suffix = mosaicType == javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY ? "Overlay" : "Blend";
        Range range = getRange(DataBuffer.TYPE_BYTE, TestRoiNoDataType.NODATA);
        Range[] rangeND = new Range[]{range, range};
        PlanarImage image = null;
        if (OLD_DESCRIPTOR) {
            // background values and threshold
            double[] background = {destinationNoData, destinationNoData};
            double[][] threshold = {{0}, {0}};
            image = javax.media.jai.operator.MosaicDescriptor.create(images, mosaicType,
                    null, null, threshold, background, hints);
        } else {
            double[] destnodata = {destinationNoData, destinationNoData};
            image = MosaicDescriptor.create(images, mosaicType, null, null, null, destnodata, rangeND, hints);
        }
        finalizeTest(suffix, DataBuffer.TYPE_BYTE, image);
    }

    public static RenderedImage getSyntheticImage(byte value) {
        final float width = 512;
        final float height = 512;
        ParameterBlock pb = new ParameterBlock();
        Byte[] array = new Byte[]{value, (byte) (value + 1), (byte) (value + 2)};
        pb.add(width);
        pb.add(height);
        pb.add(array);
        // Create the constant operation.
        return JAI.create("constant", pb);
    }
}
