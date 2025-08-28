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
package it.geosolutions.jaiext.imagefunction;

import it.geosolutions.jaiext.JAIExt;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.testclasses.TestBase;

import javax.media.jai.ImageFunction;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class used for comparing the JAI ImageFunction operation with the JAI-EXT one. Users may define how many benchmark cycles to do, how many
 * not-benchmark cycles to do and other variables, like ROI/NoData use. The parameters to define (as JVM options -D..)are:
 * <ul>
 * <li>JAI.Ext.BenchmarkCycles indicating how many benchmark cycles must be executed</li>
 * <li>JAI.Ext.NotBenchmarkCycles indicating how many cycles must be executed before doing the test</li>
 * <li>JAI.Ext.OldDescriptor(true/false) indicating if the old JAI operation must be done</li>
 * <li>JAI.Ext.RangeUsed(true/false) indicating if nodata check must be done (only for jai-ext)</li>
 * <li>JAI.Ext.ROIUsed(true/false) indicating if roi check must be done (only for jai-ext)</li>
 * </ul>
 *
 * @author Nicola Lagomarsini geosolutions
 *
 */
public class ComparisonTest extends TestBase {

    /**
     * {@link ImageFunction} used in test
     */
    private static ImageFunctionJAIEXT function;

    /**
     * Output image width
     */
    private static int width;

    /**
     * Output image height
     */
    private static int height;

    /**
     * X translation of input pixels
     */
    private static float xTrans;

    /**
     * Y translation of input pixels
     */
    private static float yTrans;

    /**
     * X scale of input pixels
     */
    private static float xScale;

    /**
     * Y scale of input pixels
     */
    private static float yScale;

    @BeforeClass
    public static void init() {

        // ImageFunction
        function = new ImageFunctionTest.DummyFunction();

        // size and other parameters
        width = 256;
        height = 256;
        xTrans = 2f;
        yTrans = 2f;
        xScale = 3f;
        yScale = 3f;

        if (OLD_DESCRIPTOR) {
            JAIExt.registerJAIDescriptor("ImageFunction");
        }

    }

    @Test
    public void testDataTypesWithRoi() {
        if (!OLD_DESCRIPTOR)
            testAllTypes(TestRoiNoDataType.ROI);
    }

    @Test
    public void testDataTypesWithNoData() {
        if (!OLD_DESCRIPTOR)
            testAllTypes(TestRoiNoDataType.NODATA);
    }

    @Test
    public void testDataTypesWithBoth() {
        if (!OLD_DESCRIPTOR)
            testAllTypes(TestRoiNoDataType.BOTH);
    }

    @Override
    public void testOperation(int dataType, TestRoiNoDataType testType) {
        Range range = getRange(dataType, testType);
        ROI roi = getROI(testType);

        // Image
        PlanarImage image = null;

        if (OLD_DESCRIPTOR) {
            JAIExt.registerJAIDescriptor("ImageFunction");
            image = javax.media.jai.operator.ImageFunctionDescriptor.create(
                    function, width, height, xScale, yScale, xTrans, yTrans,
                    null);
        } else {
            image = ImageFunctionDescriptor.create(function, width,
                    height, xScale, yScale, xTrans, yTrans, roi, range, 0f, null);
        }

        finalizeTest(getSuffix(testType, null), dataType, image);

    }
}
