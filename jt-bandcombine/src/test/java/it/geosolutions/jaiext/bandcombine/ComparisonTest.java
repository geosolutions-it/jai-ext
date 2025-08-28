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
package it.geosolutions.jaiext.bandcombine;

import it.geosolutions.jaiext.JAIExt;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.testclasses.TestBase;

import java.awt.image.RenderedImage;

import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;

import org.junit.BeforeClass;
import org.junit.Test;

public class ComparisonTest extends TestBase {

    /**
     * Matrix used for the band combination
     */
    private static double[][] matrix;

    @BeforeClass
    public static void initialSetup() {

        if (OLD_DESCRIPTOR)
            JAIExt.registerJAIDescriptor("BandCombine");

        // Matrix creation
        matrix = new double[2][4];
        for (int i = 0; i < matrix[0].length; i++) {
            matrix[0][i] = i - 1;
            matrix[1][i] = i + 1;
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
        RenderedImage testImage = createDefaultTestImage(dataType, 3, true);
        PlanarImage image = null;

        // creation of the image
        if (OLD_DESCRIPTOR) {

            image = javax.media.jai.operator.BandCombineDescriptor.create(testImage, matrix,
                    null);
        } else {
            image = BandCombineDescriptor.create(testImage, matrix, roi, range,
                    destinationNoData, null);
        }

        finalizeTest(getSuffix(testType, null), dataType, image);
    }
}
