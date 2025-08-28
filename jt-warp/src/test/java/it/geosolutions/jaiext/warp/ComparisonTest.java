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
package it.geosolutions.jaiext.warp;

import it.geosolutions.jaiext.ConcurrentOperationRegistry;
import it.geosolutions.jaiext.JAIExt;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationBicubic;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.Warp;
import javax.media.jai.WarpAffine;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test class is used for compare the timing between the new Warp operation and the old JAI version. Roi or NoData range can be used by setting
 * to true JAI.Ext.ROIUsed or JAI.Ext.RangeUsed JVM boolean parameters are set to true. If the user wants to change the number of the benchmark cycles
 * or of the not benchmark cycles, should only pass the new values to the JAI.Ext.BenchmarkCycles or JAI.Ext.NotBenchmarkCycles parameters.If the user
 * want to use the old version must pass to the JVM the JAI.Ext.OldDescriptor parameter set to true. For selecting a specific data type the user must
 * set the JAI.Ext.TestSelector JVM integer parameter to a number between 0 and 5 (where 0 means byte, 1 Ushort, 2 Short, 3 Integer, 4 Float and 5
 * Double). Interpolation type can be set with the JVM parameter JAI.Ext.InterpSelector set to 0(nearest), 1(bilinear), 2(bicubic), 3(general).
 */
public class ComparisonTest extends TestWarp {

    /**
     * Destination No Data value used when an input data is a No Data value
     */
    private static double destNoData;

    /**
     * Warp Object
     */
    private static Warp warpObj;

    /**
     * Background values to use
     */
    private static double[] backgroundValues;

    @BeforeClass
    public static void initialSetup() {
        JAIExt.initJAIEXT();
        if (OLD_DESCRIPTOR)
            JAIExt.registerJAIDescriptor("Warp");

        // Definition of the Warp Object
        AffineTransform transform = AffineTransform.getRotateInstance(Math
                .toRadians(ANGLE_ROTATION));
        transform.concatenate(AffineTransform.getTranslateInstance(0, -DEFAULT_HEIGHT));
        warpObj = new WarpAffine(transform);

        // Destination No Data
        destNoData = 0.0d;
        // Background Values
        backgroundValues = new double[]{0};
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
        RenderedImage testImage = createDefaultTestImage(dataType, 1, false);

        // Definition of the interpolation
        Interpolation interpolation;
        String suffix = "";
        PlanarImage image = null;
        for (int is = 0; is <= 3; is++) {
            interpolation = getInterpolation(dataType, is, range, destinationNoData);
            suffix = getInterpolationSuffix(is);

            // creation of the image
            if (OLD_DESCRIPTOR) {
                image = javax.media.jai.operator.WarpDescriptor.create(testImage, warpObj,
                        interpolation, backgroundValues, null);
            } else {
                image = WarpDescriptor.create(testImage, warpObj, interpolation, backgroundValues,
                        roi, range,null);
            }

            finalizeTest(getSuffix(testType, suffix), dataType, image);
        }
    }
}
