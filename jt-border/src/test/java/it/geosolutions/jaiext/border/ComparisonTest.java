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
package it.geosolutions.jaiext.border;

import it.geosolutions.jaiext.JAIExt;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test class is used for compare the timing between the new and the old versions of the Border operator . If the user wants to change the number
 * of the benchmark cycles or of the not benchmark cycles, should only pass the new values to the JAI.Ext.BenchmarkCycles or
 * JAI.Ext.NotBenchmarkCycles parameters. The selection of the old or new descriptor must be done by setting to true or false the JVM parameter
 * JAI.Ext.OldDescriptor. No Data Range can be used by simply setting to true the JAI.Ext.RangeUsed JVM parameter. For selecting which BorderExtender
 * to use the user must set a value from 0 to 3 to the JVM Integer parameter JAI.Ext.BorderType.
 */
public class ComparisonTest extends TestBase {

    /**
     * Left padding parameter
     */
    private static int leftPad;

    /**
     * Right padding parameter
     */
    private static int rightPad;

    /**
     * Top padding parameter
     */
    private static int topPad;

    /**
     * Bottom padding parameter
     */
    private static int bottomPad;

    /**
     * Output value for No Data
     */
    private static double destNoData;

    @BeforeClass
    public static void initialSetup() {
        if (OLD_DESCRIPTOR)
            JAIExt.registerJAIDescriptor("Border");
        // Border dimensions setting
        leftPad = 2;
        rightPad = 2;
        topPad = 2;
        bottomPad = 2;

        // destination No Data
        destNoData = 100d;
    }

    public void testOperation(int dataType, TestRoiNoDataType testType) {
        for (int borderType = 0; borderType < 4; borderType++) {
            RenderedImage testImage = createDefaultTestImage(dataType, 1, true);
            Range range = getRange(dataType, testType);
            BorderExtender extender = BorderExtender.createInstance(borderType);
            String suffix = extender.getClass().getSimpleName()
                    .replaceFirst("^BorderExtender", "");
            suffix = suffix.substring(0, 1).toUpperCase() + suffix.substring(1);

            // Image
            PlanarImage image = null;
            if (OLD_DESCRIPTOR) {
                image = javax.media.jai.operator.BorderDescriptor.create(testImage, leftPad,
                        rightPad, topPad, bottomPad, extender, null);
            } else {
                image = BorderDescriptor.create(testImage, leftPad, rightPad, topPad, bottomPad,
                        extender, range, destNoData, null);
            }
            finalizeTest(getSuffix(testType, suffix), dataType, image);
        }

    }
}
