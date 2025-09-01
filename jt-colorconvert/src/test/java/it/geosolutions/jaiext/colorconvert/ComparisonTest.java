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
package it.geosolutions.jaiext.colorconvert;

import it.geosolutions.jaiext.JAIExt;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;

import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class used for comparing the JAI ColorConvert operation with the JAI-EXT one. Users may define how many benchmark cycles
 * to do, how many not-benchmark cycles to do and other variables, like ROI/NoData use.
 * The parameters to define (as JVM options -D..)are:
 * <ul>
 * <li>JAI.Ext.BenchmarkCycles  indicating how many benchmark cycles must be executed</li>
 * <li>JAI.Ext.NotBenchmarkCycles  indicating how many cycles must be executed before doing the test</li>
 * <li>JAI.Ext.OldDescriptor(true/false)  indicating if the old JAI operation must be done</li>
 * <li>JAI.Ext.RangeUsed(true/false)  indicating if nodata check must be done (only for jai-ext)</li>
 * <li>JAI.Ext.ROIUsed(true/false)  indicating if roi check must be done (only for jai-ext)</li>
 * </ul>
 *
 * @author Nicola Lagomarsini geosolutions
 *
 */
public class ComparisonTest extends TestBase {

    @BeforeClass
    public static void init() {
        if (OLD_DESCRIPTOR)
            JAIExt.registerJAIDescriptor("ColorConvert");
    }

    @Override
    protected boolean supportDataType(int dataType) {
        if (dataType == DataBuffer.TYPE_SHORT)
            return false;
        else
            return super.supportDataType(dataType);
    }

    @Override
    public void testOperation(int dataType, TestRoiNoDataType testType) {
        Range range = getRange(dataType, testType);
        ROI roi = getROI(testType);
        RenderedImage testImage = createDefaultTestImage(dataType, 1, true);
        // ColorModel
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);

        ComponentColorModel colorModel = new ComponentColorModel(cs, false, false, Transparency.OPAQUE,
                dataType);
        // Image
        PlanarImage image = null;

        // creation of the image
        if (OLD_DESCRIPTOR) {
            image = javax.media.jai.operator.ColorConvertDescriptor.create(testImage,
                    colorModel, null);
        } else {
            image = ColorConvertDescriptor.create(testImage, colorModel, roi, range,
                    null, null);
        }
        finalizeTest(getSuffix(testType, null), dataType, image);
    }
}
