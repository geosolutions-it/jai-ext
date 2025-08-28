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
package it.geosolutions.jaiext.nullop;

import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import org.junit.BeforeClass;
import org.junit.Test;

import it.geosolutions.jaiext.JAIExt;
import it.geosolutions.jaiext.testclasses.TestBase;

/**
 * This test class is used for compare the timing between the new Null operation and the old Jai version. If the user wants to change the number of
 * the benchmark cycles or of the not benchmark cycles, should only pass the new values to the JAI.Ext.BenchmarkCycles or JAI.Ext.NotBenchmarkCycles
 * parameters.If the user wants to use the Jai Null operation must pass to the JVM the JAI.Ext.OldDescriptor parameter set to true. For selecting a
 * specific data type the user must set the JAI.Ext.TestSelector JVM integer parameter to a number between 0 and 5 (where 0 means byte, 1 Ushort, 2
 * Short, 3 Integer, 4 Float and 5 Double).
 */
public class ComparisonTest extends TestBase {

    @BeforeClass
    public static void initialSetup() {
        if (OLD_DESCRIPTOR) {
            JAIExt.registerJAIDescriptor("Null");
        }

    }

    @Test
    @Override
    public void testBase() {
        // nullop doesn't need to test all data types
    }

    @Override
    public void testOperation(int dataType, TestRoiNoDataType testType) {
        RenderedImage testImage = createDefaultTestImage(dataType, 1, false);
        PlanarImage image;

        // creation of the image with the selected descriptor

        if (OLD_DESCRIPTOR) {
            // Old descriptor calculations
            image = javax.media.jai.operator.NullDescriptor.create(testImage, null);
        } else {
            // New descriptor calculations
            image = NullDescriptor.create(testImage, null);
        }

        finalizeTest(getSuffix(testType, null), dataType, image);
    }
}
