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
package it.geosolutions.jaiext.rescale;

import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;

import org.junit.BeforeClass;
import org.junit.Test;

import it.geosolutions.jaiext.JAIExt;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.testclasses.TestBase;

/**
 * This test class is used for compare the timing between the new and the old versions of the Rescale operator. If the user wants to change the number
 * of the benchmark cycles or of the not benchmark cycles, should only pass the new values to the JAI.Ext.BenchmarkCycles or
 * JAI.Ext.NotBenchmarkCycles parameters. The selection of the old or new descriptor must be done by setting to true or false the JVM parameter
 * JAI.Ext.OldDescriptor. For the old descriptor it is possible to use the MediaLibAcceleration by setting to true the JVM parameter
 * JAI.Ext.Acceleration. ROI or No Data Range can be used by simply setting to true the JAI.Ext.ROIUsed and JAI.Ext.RangeUsed JVM parameters. The
 * results are printed to the screen.
 */
public class ComparisonTest extends TestBase {

    /**
     * Scale factors
     */
    private static double[] scales;

    /**
     * Offset values
     */
    private static double[] offsets;

    /**
     * Destination No Data value used when an input data is a No Data value
     */
    private static double destNoData;

    @BeforeClass
    public static void initialSetup() {
        // Rescale Factors
        scales = new double[]{2, 4, 8};
        offsets = new double[]{1, 2, 3};

        // Destination No Data
        destNoData = 0.0d;

        if (OLD_DESCRIPTOR)
            JAIExt.registerJAIDescriptor("Rescale");
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
        Range range = getRange(dataType, testType);
        ROI roi = getROI(testType);
        RenderedImage testImage = createDefaultTestImage(dataType, 1, true);

        PlanarImage image = null;

        // creation of the image
        if (OLD_DESCRIPTOR) {
        image = javax.media.jai.operator.RescaleDescriptor.create(testImage, scales,
                    offsets, null);
        } else {
            image = RescaleDescriptor.create(testImage, scales, offsets, roi, range, false,
                    destNoData, null);
        }
        finalizeTest(getSuffix(testType, null), dataType, image);
    }
}
