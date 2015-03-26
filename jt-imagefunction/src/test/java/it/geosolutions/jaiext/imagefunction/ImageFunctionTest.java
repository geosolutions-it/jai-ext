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
package it.geosolutions.jaiext.imagefunction;

import static org.junit.Assert.assertEquals;
import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;

import java.awt.Rectangle;

import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.iterator.RandomIter;

import org.junit.Test;

/**
 * Test class for the imagefunction operation
 * 
 * @author Nicola Lagomarsini GeoSolutions
 * 
 */
public class ImageFunctionTest extends TestBase {

    private static final double TOLERANCE = 0.01;

    @Test
    public void testImageFunction() {
        // Creating a NoData range
        Range nodata = RangeFactory.create(25.0f, 25.0f);
        // Definition of the translation/scale parameters
        float xTrans = 5;
        float yTrans = 5;

        float xScale = 2;
        float yScale = 2;
        // ROI definition
        ROI roi = new ROIShape(new Rectangle(0, 0, 15, 15));
        // Output value for NoData
        float destNoData = 30;
        // Image size
        int width = 128;
        int height = 128;
        // New dummy function for test
        ImageFunctionJAIEXT function = new DummyFunction();
        // Performing operation
        RenderedOp transformed = ImageFunctionDescriptor.create(function, width, height, xScale,
                yScale, xTrans, yTrans, roi, nodata, destNoData, null);
        // Checking if the result is correct
        checkNoDataROI(transformed, roi, nodata, xScale, yScale);
    }

    /**
     * Method for checking if NoData and ROI are correctly handled
     * 
     * @param finalimage
     * @param roi
     * @param nodata
     * @param deltaX
     * @param deltaY
     */
    private void checkNoDataROI(RenderedOp finalimage, ROI roi, Range nodata, float deltaX,
            float deltaY) {

        RandomIter roiIter = RandomIterFactory.create(roi.getAsImage(), null, true, true);
        Rectangle roiBounds = roi.getBounds();

        RandomIter destIter = RandomIterFactory.create(finalimage, null, true, true);
        // Start the iteration (we iterate only the first band)
        int w = finalimage.getWidth();
        int h = finalimage.getHeight();
        int minX = finalimage.getMinX();
        int minY = finalimage.getMinY();
        int maxX = minX + w;
        int maxY = minY + h;

        nodata = RangeFactory.convertToDoubleRange(nodata);

        double result = minX + minY;

        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {

                double dest = destIter.getSampleDouble(x, y, 0);

                boolean valid = true;

                // ROI Check
                if (roiBounds != null
                        && !(roiBounds.contains(x, y) && roiIter.getSample(x, y, 0) > 0)) {
                    valid = false;
                }

                // NoData Check
                if (nodata != null && nodata.contains(result)) {
                    valid = false;
                }
                if (!valid) {
                    assertEquals(30, dest, TOLERANCE);
                }

                result += deltaX;
            }
            result += deltaY;
        }
    }

    /**
     * Dummy implementation of the {@link ImageFunctionJAIEXT} interface used for testing the {@link ImageFunctionOpImage} class
     * 
     * @author Nicola Lagomarsini
     */
    public static class DummyFunction implements ImageFunctionJAIEXT {

        public void getElements(float arg0, float arg1, float arg2, float arg3, int arg4, int arg5,
                int arg6, float[] arg7, float[] arg8) {
            getElements(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, null, null, null, 0);
        }

        public void getElements(double arg0, double arg1, double arg2, double arg3, int arg4,
                int arg5, int arg6, double[] arg7, double[] arg8) {
            getElements(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, null, null, null, 0);
        }

        public int getNumElements() {
            return 1;
        }

        public boolean isComplex() {
            return false;
        }

        public void getElements(float startX, float startY, float deltaX, float deltaY, int countX,
                int countY, int element, float[] real, float[] imag, Rectangle destRect, ROI roi,
                Range nodata, float destNoData) {

            int index = 0;
            Rectangle roiBounds = roi != null ? roi.getBounds() : null;
            RandomIter it = roi != null ? RandomIterFactory.create(roi.getAsImage(), null, true,
                    true) : null;
            // Simple function.
            float result = startX + startY;
            int x0 = destRect.x;
            int y0 = destRect.y;
            for (int j = 0; j < countY; j++) {
                int y = y0 + j;
                for (int i = 0; i < countX; i++) {

                    int x = x0 + i;
                    // ROI Check
                    if (roiBounds != null && roiBounds.contains(x, y) && it.getSample(x, y, 0) > 0) {
                        // NoData check
                        if (nodata != null && !nodata.contains(result)) {
                            real[index++] = result;
                        } else {
                            real[index++] = destNoData;
                        }
                    } else {
                        real[index++] = destNoData;
                    }

                    result += deltaX;
                }
                result += deltaY;
            }
        }

        public void getElements(double startX, double startY, double deltaX, double deltaY,
                int countX, int countY, int element, double[] real, double[] imag,
                Rectangle destRect, ROI roi, Range nodata, float destNoData) {
            int index = 0;
            Rectangle roiBounds = roi != null ? roi.getBounds() : null;
            RandomIter it = roi != null ? RandomIterFactory.create(roi.getAsImage(), null, true,
                    true) : null;
            // Simple function.
            double result = startX + startY;
            int x0 = destRect.x;
            int y0 = destRect.y;
            for (int j = 0; j < countY; j++) {
                int y = y0 + j;
                for (int i = 0; i < countX; i++) {

                    int x = x0 + i;
                    // ROI Check
                    if (roiBounds != null && roiBounds.contains(x, y) && it.getSample(x, y, 0) > 0) {
                        // NoData check
                        if (nodata != null && !nodata.contains(result)) {
                            real[index++] = result;
                        } else {
                            real[index++] = destNoData;
                        }
                    } else {
                        real[index++] = destNoData;
                    }

                    result += deltaX;
                }
                result += deltaY;
            }
        }
    }
}
