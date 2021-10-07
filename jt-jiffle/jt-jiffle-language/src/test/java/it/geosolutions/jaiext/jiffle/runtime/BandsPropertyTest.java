/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2021 GeoSolutions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.geosolutions.jaiext.jiffle.runtime;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

import java.awt.image.RenderedImage;
import java.util.HashMap;
import java.util.Map;

import javax.media.jai.TiledImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

import it.geosolutions.jaiext.jiffle.Jiffle;
import it.geosolutions.jaiext.utilities.ImageUtilities;

public class BandsPropertyTest extends RuntimeTestBase {

    @Test
    public void testCopyBandsDirect() throws Exception {
        String script = "maxBand = src->bands - 1; foreach (b in 0:maxBand) { dst[b] = src[b];}";
        RenderedImage srcImg = ImageUtilities.createConstantImage(10, 10, new Integer[] {1, 2, 3});

        Map<String, Jiffle.ImageRole> imageParams = new HashMap<>();
        imageParams.put("dst", Jiffle.ImageRole.DEST);
        imageParams.put("src", Jiffle.ImageRole.SOURCE);

        // test the direct runtime
        Jiffle jiffle = new Jiffle(script, imageParams);
        JiffleDirectRuntime runtime = jiffle.getRuntimeInstance();
        TiledImage destImg =
                ImageUtilities.createConstantImage(
                        srcImg.getMinX(),
                        srcImg.getMinY(),
                        srcImg.getWidth(),
                        srcImg.getHeight(),
                        new Double[] {0d, 0d, 0d});
        runtime.setSourceImage("src", srcImg);
        runtime.setDestinationImage("dst", destImg);
        runtime.evaluateAll(nullListener);

        // scroll over pixels, assure they have been copied
        RectIter destIter = RectIterFactory.create(destImg, null);
        RectIter srcIter = RectIterFactory.create(srcImg, null);
        do {
            do {
                for (int b = 0; b < srcImg.getSampleModel().getNumBands(); b++) {
                    assertEquals(srcIter.getSampleDouble(b), destIter.getSampleDouble(b), TOL);
                }

                destIter.nextPixelDone();
            } while (!srcIter.nextPixelDone());

            srcIter.startPixels();
            destIter.startPixels();
            destIter.nextLineDone();

        } while (!srcIter.nextLineDone());
    }

    @Test
    public void testCopyBandsIndirect() throws Exception {
        String script = "maxBand = src->bands - 1; foreach (b in 0:maxBand) { dst[b] = src[b];}";
        RenderedImage srcImg = ImageUtilities.createConstantImage(10, 10, new Integer[] {1, 2, 3});

        Map<String, Jiffle.ImageRole> imageParams = new HashMap<>();
        imageParams.put("dst", Jiffle.ImageRole.DEST);
        imageParams.put("src", Jiffle.ImageRole.SOURCE);

        // test the indirect runtime
        Jiffle jiffle = new Jiffle(script, imageParams);
        JiffleIndirectRuntime runtime =
                (JiffleIndirectRuntime) jiffle.getRuntimeInstance(Jiffle.RuntimeModel.INDIRECT);
        runtime.setSourceImage("src", srcImg);

        // scroll over pixels and evaluate via indirect runtime
        int numBands = srcImg.getSampleModel().getNumBands();
        double[] expected = new double[numBands];
        double[] actual = new double[numBands];
        RectIter srcIter = RectIterFactory.create(srcImg, null);
        int x = srcImg.getMinX(), y = srcImg.getMinY();
        do {
            do {
                runtime.evaluate(x, y, actual);
                for (int b = 0; b < numBands; b++) expected[b] = srcIter.getSampleDouble(b);
                Assert.assertArrayEquals(actual, expected, TOL);
                x++;
                if (x >= (srcImg.getMinX() + srcImg.getWidth())) {
                    x = srcImg.getMinX();
                    y++;
                }
            } while (!srcIter.nextPixelDone());

            srcIter.startPixels();
        } while (!srcIter.nextLineDone());
    }

    @Test
    public void testSumBands() throws Exception {
        String script =
                "sum = 0;\n"
                        + "goodBands = 0;\n"
                        + "maxBand = src->bands - 1;\n"
                        + "foreach (b in 0:maxBand) {\n"
                        + "    value = src[b];\n"
                        + "    if (value != -9999.0) {\n"
                        + "       sum += value;\n"
                        + "       goodBands += 1;\n"
                        + "    }\n"
                        + "    dst[b] = value;\n"
                        + "}\n"
                        + "if (goodBands == 0) {\n"
                        + "    // calculated values at the end of the output\n"
                        + "    dst[maxBand + 1] = -9999.0;\n"
                        + "    dst[maxBand + 2] = -9999.0;\n"
                        + "} else {\n"
                        + "    dst[maxBand + 1] = sum;\n"
                        + "    dst[maxBand + 2] = sum / goodBands;\n"
                        + "}";
        TiledImage srcImg =
                ImageUtilities.createConstantImage(10, 10, new Integer[] {1, 2, 3, -9999});
        srcImg.setSample(0, 0, 0, -9999);
        srcImg.setSample(0, 0, 1, -9999);
        srcImg.setSample(0, 0, 2, -9999);
        

        Map<String, Jiffle.ImageRole> imageParams = new HashMap<>();
        imageParams.put("dst", Jiffle.ImageRole.DEST);
        imageParams.put("src", Jiffle.ImageRole.SOURCE);

        // test the direct runtime
        Jiffle jiffle = new Jiffle(script, imageParams);
        JiffleDirectRuntime runtime = jiffle.getRuntimeInstance();
        TiledImage destImg =
                ImageUtilities.createConstantImage(
                        srcImg.getMinX(),
                        srcImg.getMinY(),
                        srcImg.getWidth(),
                        srcImg.getHeight(),
                        new Double[] {0d, 0d, 0d, 0d, 0d, 0d});
        runtime.setSourceImage("src", srcImg);
        runtime.setDestinationImage("dst", destImg);
        runtime.evaluateAll(nullListener);

        // scroll over pixels, assure they have been copied
        RectIter destIter = RectIterFactory.create(destImg, null);
        RectIter srcIter = RectIterFactory.create(srcImg, null);
        boolean firstPixel = true;
        do {
            do {
                for (int b = 0; b < 3; b++) {
                    assertEquals(srcIter.getSampleDouble(b), destIter.getSampleDouble(b), TOL);
                }
                if (firstPixel) {
                    assertEquals(-9999, destIter.getSampleDouble(4), TOL);
                    assertEquals(-9999, destIter.getSampleDouble(5), TOL);
                    firstPixel = false;
                } else {
                    // check sum and average, one value has been skipped as NODATA
                    assertEquals(6, destIter.getSampleDouble(4), TOL);
                    assertEquals(2, destIter.getSampleDouble(5), TOL);
                }

                destIter.nextPixelDone();
            } while (!srcIter.nextPixelDone());

            srcIter.startPixels();
            destIter.startPixels();
            destIter.nextLineDone();

        } while (!srcIter.nextLineDone());
    }
}
