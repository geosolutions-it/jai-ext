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
public class ComparisonTest2 extends TestScale2 {
    /**
     * Value indicating No Data for the destination image
     */
    private static double destinationNoData = 0;

    /**
     * Translation parameter on the X axis
     */
    private double xTrans = 0;

    /**
     * Translation parameter on the Y axis
     */
    private double yTrans = 0;

    /**
     * Scale parameter on the X axis
     */
    private double xScale = 1.5f;

    /**
     * Scale parameter on the Y axis
     */
    private double yScale = 1.5f;

    /**
     * RenderingHints used for selecting the borderExtender
     */
    private static RenderingHints hints;

    @BeforeClass
    public static void initialSetup() throws FileNotFoundException, IOException {

        hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                BorderExtender.createInstance(BorderExtender.BORDER_COPY));

        if (OLD_DESCRIPTOR) {
            JAIExt.registerJAIDescriptor("Scale");
        }
    }

    @Override
    public void testOperation(int dataType, TestRoiNoDataType testType) {
        RenderedImage testImage = createDefaultTestImage(dataType, 1, true);
        Interpolation interpolation;
        String suffix = "";
        Range range = getRange(dataType, testType);
        for (int is = 0; is <= 3; is++) {
            interpolation = getInterpolation(dataType, is, range, destinationNoData);
            suffix = getInterpolationSuffix(is);
            double scaleX = xScale;
            double scaleY = yScale;
            PlanarImage image = null;

            // creation of the image with the selected interpolator
            if (OLD_DESCRIPTOR) {
                image = javax.media.jai.operator.ScaleDescriptor.create(image, (float) scaleX, (float) scaleY,
                        (float) xTrans, (float) yTrans, interpolation, hints);
            } else {
                image = Scale2Descriptor.create(image, scaleX, scaleY, xTrans, yTrans, interpolation,
                        null, false, null, null, hints);
            }

            finalizeTest(getSuffix(testType, suffix), dataType, image);
        }
    }
}
