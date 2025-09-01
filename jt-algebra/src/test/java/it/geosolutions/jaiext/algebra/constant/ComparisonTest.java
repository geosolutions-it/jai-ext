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
package it.geosolutions.jaiext.algebra.constant;

import it.geosolutions.jaiext.algebra.AlgebraDescriptor.Operator;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.AddConstDescriptor;
import javax.media.jai.operator.AndConstDescriptor;
import javax.media.jai.operator.DivideByConstDescriptor;
import javax.media.jai.operator.MultiplyConstDescriptor;
import javax.media.jai.operator.OrConstDescriptor;
import javax.media.jai.operator.SubtractConstDescriptor;
import javax.media.jai.operator.XorConstDescriptor;

import org.junit.BeforeClass;
import org.junit.Test;

import static it.geosolutions.jaiext.algebra.AlgebraDescriptor.Operator.AND;
import static it.geosolutions.jaiext.algebra.AlgebraDescriptor.Operator.DIVIDE;
import static it.geosolutions.jaiext.algebra.AlgebraDescriptor.Operator.MULTIPLY;
import static it.geosolutions.jaiext.algebra.AlgebraDescriptor.Operator.OR;
import static it.geosolutions.jaiext.algebra.AlgebraDescriptor.Operator.SUBTRACT;
import static it.geosolutions.jaiext.algebra.AlgebraDescriptor.Operator.SUM;
import static it.geosolutions.jaiext.algebra.AlgebraDescriptor.Operator.XOR;

public class ComparisonTest extends TestBase {

    /**
     * Output value for No Data
     */
    private static double destNoData;

    private static double[] constsD;
    private static int[] constsI;

    private static Operator OPERATIONS[] = {SUM, SUBTRACT, MULTIPLY, DIVIDE, OR, XOR, AND};

    @BeforeClass
    public static void initialSetup() {

        // Constants
        constsD = new double[]{5};
        constsI = new int[]{5};

        // destination No Data
        destNoData = 100d;
    }

    @Test
    public void testDataTypes() {
        testAllTypes(TestRoiNoDataType.NONE);
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
        String dataTypeString = getDataTypeString(dataType);
        Range range = getRange(dataType, testType);
        ROI roi = getROI(testType);
        RenderedImage testImage = createDefaultTestImage(dataType, 1, true);
        Operator op;
        for (int o = 0; o < OPERATIONS.length; o++) {
            op = OPERATIONS[o];

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
                case AND:
                    operation = "And";
                    break;
                default:
                    break;
            }
            if ("Or".equals(operation) || "Xor".equals(operation) || "And".equals(operation)) {
                if (dataType == DataBuffer.TYPE_FLOAT || dataType == DataBuffer.TYPE_DOUBLE) {
                    // logical operations are not supported on float and double
                    continue;
                }
            }
            String description = getDescription(operation);
            PlanarImage image = null;


            // creation of the image
            if (OLD_DESCRIPTOR) {

                switch (op) {
                    case SUM:
                        image = AddConstDescriptor.create(testImage, constsD, null);
                        break;
                    case SUBTRACT:
                        image = SubtractConstDescriptor.create(testImage, constsD, null);
                        break;
                    case MULTIPLY:
                        image = MultiplyConstDescriptor.create(testImage, constsD, null);
                        break;
                    case DIVIDE:
                        image = DivideByConstDescriptor.create(testImage, constsD, null);
                        break;
                    case AND:
                        image = AndConstDescriptor.create(testImage, constsI, null);
                        break;
                    case OR:
                        image = OrConstDescriptor.create(testImage, constsI, null);
                        break;
                    case XOR:
                        image = XorConstDescriptor.create(testImage, constsI, null);
                        break;
                    default:
                        break;
                }
            } else {
                image = OperationConstDescriptor.create(testImage, constsD, op, roi, range,
                        destNoData, null);
            }

            System.out.println(description + "_" + dataTypeString);
            finalizeTest(getSuffix(testType, operation), dataType, image);
        }
    }
}
