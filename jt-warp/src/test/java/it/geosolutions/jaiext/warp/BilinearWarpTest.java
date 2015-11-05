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

import java.awt.geom.AffineTransform;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

import javax.media.jai.WarpAffine;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import it.geosolutions.jaiext.JAIExt;

/**
 * Test class which extends the TestWarp class and executes all the tests with the bilinear interpolation.
 */
public class BilinearWarpTest extends TestWarp{

    /**
     * Static method for preparing the test environment.
     */
    @BeforeClass
    public static void setup() {
        JAIExt.initJAIEXT();
        // Definition of the Warp Object
        AffineTransform transform = AffineTransform.getRotateInstance(Math
                .toRadians(ANGLE_ROTATION));
        transform.concatenate(AffineTransform.getTranslateInstance(0, -DEFAULT_HEIGHT));
        warpObj = new WarpAffine(transform);

        // Definition of the input data types

        noDataValueB = 55;
        noDataValueU = 55;
        noDataValueS = 55;
        noDataValueI = 55;
        noDataValueF = 55;
        noDataValueD = 55;

        // Array creation
        images = new RenderedImage[NUM_IMAGES];
        // Setting of the imageFiller parameter to true, storing inside a variable its initial value
        boolean imageToFill = IMAGE_FILLER;
        IMAGE_FILLER = true;
        // Creation of the images
        images[0] = createTestImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataValueB, false);
        images[1] = createTestImage(DataBuffer.TYPE_USHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataValueU, false);
        images[2] = createTestImage(DataBuffer.TYPE_SHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataValueS, false);
        images[3] = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataValueI, false);
        images[4] = createTestImage(DataBuffer.TYPE_FLOAT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataValueF, false);
        images[5] = createTestImage(DataBuffer.TYPE_DOUBLE, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataValueD, false);
        // Setting of the image filler to its initial value
        IMAGE_FILLER = imageToFill;
        // Interpolation type
        interpType = InterpolationType.BILINEAR_INTERP;

    }
    
    @Test
    public void testImage() {
        super.testImage(interpType);
    }

    @Test
    public void testImageROI() {
        super.testImageROI(interpType);
    }

    @Test
    public void testImageNoData() {
        super.testImageNoData(interpType);
    }

    @Test
    public void testImageNoDataROI() {
        super.testImageNoDataROI(interpType);
    }

    /**
     * Static method for disposing the test environment.
     */
    @AfterClass
    public static void finalStuff() {
        TestWarp.finalStuff();
    }
}
