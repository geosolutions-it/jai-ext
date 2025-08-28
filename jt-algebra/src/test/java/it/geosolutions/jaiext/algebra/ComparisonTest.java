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
package it.geosolutions.jaiext.algebra;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.AbsoluteDescriptor;
import javax.media.jai.operator.AddDescriptor;
import javax.media.jai.operator.AndDescriptor;
import javax.media.jai.operator.DivideDescriptor;
import javax.media.jai.operator.ExpDescriptor;
import javax.media.jai.operator.InvertDescriptor;
import javax.media.jai.operator.LogDescriptor;
import javax.media.jai.operator.MaxDescriptor;
import javax.media.jai.operator.MinDescriptor;
import javax.media.jai.operator.MultiplyDescriptor;
import javax.media.jai.operator.NotDescriptor;
import javax.media.jai.operator.OrDescriptor;
import javax.media.jai.operator.SubtractDescriptor;
import javax.media.jai.operator.XorDescriptor;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import it.geosolutions.jaiext.algebra.AlgebraDescriptor.Operator;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;


public class ComparisonTest extends TestBase {

    /**
     * Number associated with the type of BorderExtender to use
     */
    private static final int NUM_IMAGES = Integer.getInteger("JAI.Ext.NumImages", 2);

    /**
     * Number associated with the type of BorderExtender to use
     */
    private static final int NUM_BANDS = 1;

    /**
     * Output value for No Data
     */
    private static double destNoData;

    private static ROIShape roi;

    @BeforeClass
    public static void initialSetup() {

        // destination No Data
        destNoData = 100d;
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
        RenderedImage testImages[] = new RenderedImage[NUM_IMAGES];

        for (int i = 0; i < NUM_IMAGES; i++) {
            testImages[i] = createDefaultTestImage(dataType, NUM_BANDS, true);
        }
        Range range = getRange(dataType, testType);
        ROI roi = getROI(testType);
        Operator op;
        Operator[] operations = Operator.values();
        for (int o = 0; o < operations.length; o++) {
            op = operations[o];

            // Descriptor string definition
            String operation = "";
            switch (op) {
                case SUM:
                    operation = "Add";
                    break;
                case SUBTRACT:
                    operation = "Subtract";
                    break;
                case MULTIPLY:
                    operation = "Multiply";
                    break;
                case DIVIDE:
                    operation = "Divide";
                    break;
                case OR:
                    operation = "Or";
                    break;
                case XOR:
                    operation = "Xor";
                    break;
                case ABSOLUTE:
                    operation = "Absolute";
                    break;
                case AND:
                    operation = "And";
                    break;
                case EXP:
                    operation = "Exp";
                    break;
                case INVERT:
                    operation = "Invert";
                    break;
                case LOG:
                    operation = "Log";
                    break;
                case NOT:
                    operation = "Not";
                    break;
                case DIVIDE_INTO:
                    operation = "DivideInto";
                    break;
                case SUBTRACT_FROM:
                    operation = "SubtractFrom";
                    break;
                case MAX:
                    operation = "Max";
                    break;
                case MIN:
                    operation = "Min";
                    break;
            }

            PlanarImage image = null;

            // creation of the image
            if (OLD_DESCRIPTOR) {

                RenderedImage firstOp;

                switch (op) {
                    case SUM:
                        firstOp = testImages[0];
                        for (int j = 1; j < NUM_IMAGES; j++) {
                            firstOp = AddDescriptor.create(firstOp, testImages[j], null);
                        }
                        image = (PlanarImage) firstOp;
                        break;
                    case SUBTRACT:
                        firstOp = testImages[0];
                        for (int j = 1; j < NUM_IMAGES; j++) {
                            firstOp = SubtractDescriptor.create(firstOp, testImages[j], null);
                        }
                        image = (PlanarImage) firstOp;
                        break;
                    case MULTIPLY:
                        firstOp = testImages[0];
                        for (int j = 1; j < NUM_IMAGES; j++) {
                            firstOp = MultiplyDescriptor.create(firstOp, testImages[j], null);
                        }
                        image = (PlanarImage) firstOp;
                        break;
                    case DIVIDE:
                        firstOp = testImages[0];
                        for (int j = 1; j < NUM_IMAGES; j++) {
                            firstOp = DivideDescriptor.create(firstOp, testImages[j], null);
                        }
                        image = (PlanarImage) firstOp;
                        break;
                    case ABSOLUTE:
                        image = AbsoluteDescriptor.create(testImages[0], null);
                        break;
                    case AND:
                        firstOp = testImages[0];
                        for (int j = 1; j < NUM_IMAGES; j++) {
                            firstOp = AndDescriptor.create(firstOp, testImages[j], null);
                        }
                        image = (PlanarImage) firstOp;
                        break;
                    case OR:
                        firstOp = testImages[0];
                        for (int j = 1; j < NUM_IMAGES; j++) {
                            firstOp = OrDescriptor.create(firstOp, testImages[j], null);
                        }
                        image = (PlanarImage) firstOp;
                        break;
                    case XOR:
                        firstOp = testImages[0];
                        for (int j = 1; j < NUM_IMAGES; j++) {
                            firstOp = XorDescriptor.create(firstOp, testImages[j], null);
                        }
                        image = (PlanarImage) firstOp;
                        break;
                    case NOT:
                        image = NotDescriptor.create(testImages[0], null);
                        break;
                    case EXP:
                        image = ExpDescriptor.create(testImages[0], null);
                        break;
                    case INVERT:
                        image = InvertDescriptor.create(testImages[0], null);
                        break;
                    case LOG:
                        image = LogDescriptor.create(testImages[0], null);
                        break;
                    case DIVIDE_INTO:
                        firstOp = testImages[NUM_IMAGES - 1];
                        for (int j = NUM_IMAGES - 2; j >= 0; j--) {
                            firstOp = DivideDescriptor.create(firstOp, testImages[j], null);
                        }
                        image = (PlanarImage) firstOp;
                        break;
                    case SUBTRACT_FROM:
                        firstOp = testImages[NUM_IMAGES - 1];
                        for (int j = NUM_IMAGES - 2; j >= 0; j--) {
                            firstOp = SubtractDescriptor.create(firstOp, testImages[j], null);
                        }
                        image = (PlanarImage) firstOp;
                        break;
                    case MAX:
                        firstOp = testImages[0];
                        for (int j = 1; j < NUM_IMAGES; j++) {
                            firstOp = MaxDescriptor.create(firstOp, testImages[j], null);
                        }
                        image = (PlanarImage) firstOp;
                        break;
                    case MIN:
                        firstOp = testImages[0];
                        for (int j = 1; j < NUM_IMAGES; j++) {
                            firstOp = MinDescriptor.create(firstOp, testImages[j], null);
                        }
                        image = (PlanarImage) firstOp;
                        break;
                }
            } else {
                image = AlgebraDescriptor.create(op, roi, range, destNoData, null, testImages);
            }
            finalizeTest(getSuffix(testType, operation), dataType, image);
        }
    }
}
