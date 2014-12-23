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
package it.geosolutions.jaiext.translate;

import static org.junit.Assert.assertEquals;
import it.geosolutions.jaiext.testclasses.TestBase;

import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;

import org.junit.Test;

/**
 * This test class is used for checking the functionality of the TranslateIntOpImage class. The first test consists in a translation of an integer
 * value, for all the 6 different types of image. For checking if the translation has been correctly performed the first image pixel is considered: if
 * this pixel position plus the translation X and Y values is equal to the destination image pixel value, then the translation is performed. The
 * second test checks that if the translation parameters are not integral, then an exception is thrown.
 */
public class TranslateTest extends TestBase {

    @Test
    public void testTranslation() {

        float xTrans = -3f;
        float yTrans = -3f;

        int dataType = DataBuffer.TYPE_BYTE;
        testType(xTrans, yTrans, dataType);

        dataType = DataBuffer.TYPE_USHORT;
        testType(xTrans, yTrans, dataType);

        dataType = DataBuffer.TYPE_SHORT;
        testType(xTrans, yTrans, dataType);

        dataType = DataBuffer.TYPE_INT;
        testType(xTrans, yTrans, dataType);

        dataType = DataBuffer.TYPE_FLOAT;
        testType(xTrans, yTrans, dataType);

        dataType = DataBuffer.TYPE_DOUBLE;
        testType(xTrans, yTrans, dataType);
    }

    private void testType(float xTrans, float yTrans, int dataType) {

        RenderedImage testIMG = null;

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            byte imageValueB = 127;
            testIMG = createTestImage(dataType, DEFAULT_WIDTH, DEFAULT_HEIGHT, imageValueB, false);
            break;
        case DataBuffer.TYPE_USHORT:
            short imageValueUS = (short) 65500;
            testIMG = createTestImage(dataType, DEFAULT_WIDTH, DEFAULT_HEIGHT, imageValueUS, false);
            break;
        case DataBuffer.TYPE_SHORT:
            short imageValueS = -300;
            testIMG = createTestImage(dataType, DEFAULT_WIDTH, DEFAULT_HEIGHT, imageValueS, false);
            break;
        case DataBuffer.TYPE_INT:
            int imageValueI = Integer.MAX_VALUE - 1;
            testIMG = createTestImage(dataType, DEFAULT_WIDTH, DEFAULT_HEIGHT, imageValueI, false);
            break;
        case DataBuffer.TYPE_FLOAT:
            float imageValueF = 50.1f;
            testIMG = createTestImage(dataType, DEFAULT_WIDTH, DEFAULT_HEIGHT, imageValueF, false);
            break;
        case DataBuffer.TYPE_DOUBLE:
            double imageValueD = Double.MAX_VALUE;
            testIMG = createTestImage(dataType, DEFAULT_WIDTH, DEFAULT_HEIGHT, imageValueD, false);
            break;
        default:
            throw new IllegalArgumentException("Wrong data type");
        }

        // Translated image
        PlanarImage translatedIMG = TranslateDescriptor.create(testIMG, xTrans, yTrans, null, null);

        translatedIMG.getTiles();

        double actualX = translatedIMG.getMinX();
        double actualY = translatedIMG.getMinY();

        double expectedX = testIMG.getMinX() + xTrans;
        double expectedY = testIMG.getMinY() + yTrans;

        double tolerance = 0.1f;

        assertEquals(expectedX, actualX, tolerance);
        assertEquals(expectedY, actualY, tolerance);
        
        //Final Image disposal
        if(translatedIMG instanceof RenderedOp){
            ((RenderedOp)translatedIMG).dispose();
        }
        
    }

    @Test
    public void testException() {

        float xTrans = -3.2f;
        float yTrans = 0;

        int dataType = DataBuffer.TYPE_BYTE;

        byte imageValueB = 127;
        RenderedImage testIMG = createTestImage(dataType, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                imageValueB, false);
        try {
            // Translated image
            PlanarImage translatedIMG = TranslateDescriptor.create(testIMG, xTrans, yTrans, null, null);
        } catch (Exception e) {
            String exception = "Translate Operation can be used only for integral displacements. If"
                    + "a layout is present, the translate operation cannot deal with";
            assertEquals(exception, e.getMessage());
        }

    }
}
