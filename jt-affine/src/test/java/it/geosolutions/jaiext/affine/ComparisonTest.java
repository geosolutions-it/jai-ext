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
package it.geosolutions.jaiext.affine;


import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.FileNotFoundException;
import java.io.IOException;

import it.geosolutions.jaiext.JAIExt;
import it.geosolutions.jaiext.range.Range;

import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;

import it.geosolutions.jaiext.testclasses.TestBase;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test class is used for compare the timing between the new Nearest,Bilinear and Bicubic interpolators and their JAI version on the affine
 * operation. No Roi or No Data range are used. If the user wants to change the number of the benchmark cycles or of the not benchmark cycles, should
 * only pass the new values to the JAI.Ext.BenchmarkCycles or JAI.Ext.NotBenchmarkCycles parameters. The tests are quite different because the
 * interpolator used can be one of the 3 JAI interpolators but the other operations are similar:
 * <ul>
 * <li>Selection of the descriptor (new or old AffineDescriptor)</li>
 * <li>Image Transformation</li>
 * <li>statistic calculation (if the cycle belongs to the benchmark cycles).</li>
 * </ul>
 * The selection of the old or new descriptor must be done by setting to true or false the JVM parameter JAI.Ext.OldDescriptor. The interpolator can
 * be chosen by passing the JAI.Ext.TestSelector Integer JVM parameter: 0 for nearest interpolation, 1 for bilinear, 2 for bicubic. The transformation
 * used is selected by passing the JVM integral parameter JAI.Ext.TransformationSelector, with 0 that indicates rotation, 1 scale, 2 combination of
 * them. If the user wants to use the accelerated code, the JVM parameter JAI.Ext.Acceleration must be set to true.
 */
public class ComparisonTest extends TestBase {

    /**
     * Default subsampling bits used for the bilinear and bicubic interpolation
     */
    private final static int DEFAULT_SUBSAMPLE_BITS = 8;

    /**
     * Default precision bits used in the bicubic calculation
     */
    private final static int DEFAULT_PRECISION_BITS = 8;

    /**
     * Value indicating No Data for the destination image
     */
    private static double destinationNoData = 0;

    /**
     * Rotation used
     */
    private static AffineTransform rotateTransform;

    /**
     * Translation used
     */
    private static AffineTransform translateTransform;

    /**
     * Scale used
     */
    private static AffineTransform scaleTransform;

    /**
     * RenderingHints used for selecting the borderExtender
     */
    private static RenderingHints hints;

    private static int[] weight;

    @BeforeClass
    public static void initialSetup() throws FileNotFoundException, IOException {
        // Selection of the RGB image

        hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                BorderExtender.createInstance(BorderExtender.BORDER_COPY));

        // 45� degrees rotation
        double theta = Math.PI / 4;
        rotateTransform = AffineTransform.getRotateInstance(theta);
        // 100 px translation
        translateTransform = AffineTransform.getTranslateInstance(100, 0);
        // 2 x scale
        scaleTransform = AffineTransform.getScaleInstance(1.5f, 1.5f);

        weight = new int[4];

        double rnd = Math.random();

        if (rnd >= 0 & rnd < 0.25d) {
            weight[0] = 0;
            weight[1] = 1;
            weight[2] = 0;
            weight[3] = 0;
        } else if (rnd >= 0.25d & rnd < 0.5d) {
            weight[0] = 0;
            weight[1] = 1;
            weight[2] = 0;
            weight[3] = 1;
        } else if (rnd >= 0.25d & rnd < 0.5d) {
            weight[0] = 1;
            weight[1] = 1;
            weight[2] = 0;
            weight[3] = 0;
        } else {
            weight[0] = 1;
            weight[1] = 1;
            weight[2] = 1;
            weight[3] = 1;
        }

        if (OLD_DESCRIPTOR) {
            JAIExt.registerJAIDescriptor("Affine");
        }
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
        RenderedImage testImage = createDefaultTestImage(dataType, 1, true);
        Interpolation interpolation;
        String suffix = "";
        PlanarImage image = null;
        Range range = getRange(dataType, testType);
        ROI roi = getROI(testType);
        for (int is = 0; is <= 3; is++) {
            interpolation = getInterpolation(dataType, is, range, destinationNoData);
            suffix = getInterpolationSuffix(is);
            AffineTransform transform = new AffineTransform();
            for (int itx = 0; itx < 3; itx++) {
                switch (itx) {
                    case 0:
                        transform.concatenate(rotateTransform);
                        break;
                    case 1:
                        transform.concatenate(scaleTransform);
                        break;
                    case 2:
                        transform.concatenate(rotateTransform);
                        transform.concatenate(scaleTransform);
                        transform.concatenate(translateTransform);
                        break;
                    default:
                        throw new IllegalArgumentException("Wrong transformation value");
                }


                // Destination no data used by the affine operation with the classic
                // bilinear interpolator
                double[] destinationNoDataArray = {destinationNoData, destinationNoData, destinationNoData};

                if (OLD_DESCRIPTOR) {
                    image = javax.media.jai.operator.AffineDescriptor.create(testImage, transform,
                            interpolation, destinationNoDataArray, hints);
                } else {
                    image = AffineDescriptor.create(testImage, transform, interpolation,
                            destinationNoDataArray, roi, false, false, range, hints);
                }
            }
            finalizeTest(getSuffix(testType, suffix), dataType, image);
        }
    }


}
