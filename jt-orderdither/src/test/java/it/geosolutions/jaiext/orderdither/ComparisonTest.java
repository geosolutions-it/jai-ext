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
package it.geosolutions.jaiext.orderdither;

import it.geosolutions.jaiext.JAIExt;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.util.Arrays;

import javax.media.jai.ColorCube;
import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class used for comparing the JAI OrderedDither operation with the JAI-EXT one. Users may define how many benchmark cycles to do, how many
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
    private static final int NUM_BANDS = 3;

    /**
     * ColorMap used for testing
     */
    private static ColorCube colorMap;

    /**
     * Dithering mask used for testing
     */
    private static KernelJAI[] ditherMask;

    @BeforeClass
    public static void init() {

        // Definition of the colorcube
        colorMap = ColorCube.BYTE_496;
        // Definition of the Kernels
        // Getting the dithering mask
        ditherMask = new KernelJAI[NUM_BANDS];
        int width = 64;
        int height = 64;
        float[] data = new float[width * height];
        Arrays.fill(data, 0.5f);
        ditherMask[0] = new KernelJAI(width, height, data);
        ditherMask[1] = new KernelJAI(width, height, data);
        ditherMask[2] = new KernelJAI(width, height, data);
    }

    @Override
    protected boolean supportDataType(int dataType) {
        if (dataType == DataBuffer.TYPE_SHORT)
            return false;
        else
            return super.supportDataType(dataType);
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
        String description = getDescription("OrderedDither");
        String dataTypeString = getDataTypeString(dataType);
        Range range = getRange(dataType, testType);
        ROI roi = getROI(testType);
        RenderedImage testImage = createDefaultTestImage(dataType, 1, true);

        PlanarImage image = null;

        // creation of the image
        if (OLD_DESCRIPTOR) {
            JAIExt.registerJAIDescriptor("OrderedDither");
            image = javax.media.jai.operator.OrderedDitherDescriptor.create(testImage,
                    colorMap, ditherMask, null);
        } else {
            image = OrderedDitherDescriptor.create(testImage, colorMap, ditherMask, null,
                    roi, range, 100d);
        }
        System.out.println(description + "_" + dataTypeString);
        finalizeTest(getSuffix(testType, null), dataType, image);

    }
}
