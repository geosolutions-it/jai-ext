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
package it.geosolutions.jaiext.bandmerge;

import java.awt.image.RenderedImage;
import java.util.Vector;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;

import org.junit.BeforeClass;
import org.junit.Test;

import it.geosolutions.jaiext.JAIExt;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.testclasses.TestBase;

/**
 * This test class is used for compare the timing between the new BandMerge operation and the its old Jai version. NoData range can be used by setting
 * to true the JAI.Ext.RangeUsed JVM boolean parameters. If the user wants to change the number of the benchmark cycles or of the not benchmark
 * cycles, should only pass the new values to the JAI.Ext.BenchmarkCycles or JAI.Ext.NotBenchmarkCycles parameters.If the user want to use the Jai
 * BandMerge operation must pass to the JVM the JAI.Ext.OldDescriptor parameter set to true. For selecting a specific data type the user must set the
 * JAI.Ext.TestSelector JVM integer parameter to a number between 0 and 5 (where 0 means byte, 1 Ushort, 2 Short, 3 Integer, 4 Float and 5 Double).
 * The test is made on a group of 4 images.
 */
public class ComparisonTest extends TestBase {

    /**
     * Final image band number
     */
    private static final int BAND_NUMBER = 4;

    /**
     * Destination No Data value
     */
    private static double destNoData;

    // Initial static method for preparing all the test data
    @BeforeClass
    public static void initialSetup() {
        // Destination No Data
        destNoData = 50d;
        if (OLD_DESCRIPTOR) {
            JAIExt.registerJAIDescriptor("bandmerge");
        }

    }

    @Test
    public void testDataTypesWithNoData() {
        if (!OLD_DESCRIPTOR)
            testAllTypes(TestRoiNoDataType.NODATA);
    }

    @Override
    public void testOperation(int dataType, TestRoiNoDataType testType) {
        Range[] range = new Range[]{getRange(dataType, testType)};
        ROI roi = getROI(testType);
        RenderedImage[] testImage = new RenderedImage[BAND_NUMBER];
        for (int i = 0; i < BAND_NUMBER; i++) {
            testImage[i] = createDefaultTestImage(dataType, 1, true);
        }

        // Image data types
        PlanarImage image = null;

        // Vector of sources (used for Old BandMerge operation)
        Vector vec = new Vector(testImage.length);

        for (RenderedImage img : testImage) {
            vec.add(img);
        }

        ParameterBlockJAI pbj = new ParameterBlockJAI("bandmerge");
        pbj.setSources(vec);


        // creation of the image with the selected descriptor

        if (OLD_DESCRIPTOR) {
            // Old descriptor calculations
            image = JAI.create("bandmerge", pbj, null);
        } else {
            // New descriptor calculations
            image = BandMergeDescriptor.create(range, destNoData, false, null, testImage);
        }


        for (int band = 0; band < BAND_NUMBER; band++) {
            ((TiledImage) testImage[band]).dispose();
        }

    }

}
