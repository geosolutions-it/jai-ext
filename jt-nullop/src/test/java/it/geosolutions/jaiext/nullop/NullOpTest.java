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
package it.geosolutions.jaiext.nullop;

import static org.junit.Assert.*;
import it.geosolutions.jaiext.testclasses.TestBase;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import javax.media.jai.RenderedOp;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test class is used for evaluating the functionalities of the NullOpImage class. This purpose is achieved by creating an image and checking if
 * the source and destination pixels in the same location are equals. The operation is performed on all the JAI possible data types. No ROI, nor No
 * Data are present.
 * 
 * @author geosolutions
 * 
 */
public class NullOpTest extends TestBase {

    /** Tolerance value used for comparison between double values */
    private final static double TOLERANCE = 0.1d;

    /** Total number of images used */
    private static final int IMAGE_NUMBER = 6;

    /** Array containing all the test-images */
    private static RenderedImage[] testImage;

    @BeforeClass
    public static void initialSetup() {
        // Initialization of the image array
        testImage = new RenderedImage[IMAGE_NUMBER];

        // Selection of values for filling the input images
        byte valueB = 50;
        short valueS = 50;
        int valueI = 50;
        float valueF = 50;
        double valueD = 50;

        // Setting of this parameter to true for allowing a complete filling of the images
        IMAGE_FILLER = true;

        // Creation of the images
        testImage[DataBuffer.TYPE_BYTE] = createTestImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, valueB, false, 1);
        testImage[DataBuffer.TYPE_USHORT] = createTestImage(DataBuffer.TYPE_USHORT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, valueS, false, 1);
        testImage[DataBuffer.TYPE_SHORT] = createTestImage(DataBuffer.TYPE_SHORT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, valueS, false, 1);
        testImage[DataBuffer.TYPE_INT] = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, valueI, false, 1);
        testImage[DataBuffer.TYPE_FLOAT] = createTestImage(DataBuffer.TYPE_FLOAT, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, valueF, false, 1);
        testImage[DataBuffer.TYPE_DOUBLE] = createTestImage(DataBuffer.TYPE_DOUBLE, DEFAULT_WIDTH,
                DEFAULT_HEIGHT, valueD, false, 1);

        IMAGE_FILLER = false;
    }

    @Test
    public void testNullOperation() {
        // Test on all the images, each one with a different data type
        singleNullOpTest(testImage[DataBuffer.TYPE_BYTE]);
        singleNullOpTest(testImage[DataBuffer.TYPE_USHORT]);
        singleNullOpTest(testImage[DataBuffer.TYPE_SHORT]);
        singleNullOpTest(testImage[DataBuffer.TYPE_INT]);
        singleNullOpTest(testImage[DataBuffer.TYPE_FLOAT]);
        singleNullOpTest(testImage[DataBuffer.TYPE_DOUBLE]);
    }

    /**
     * Test which performs the Null operation on the source image and checks if the image pixels of a single tile are equals
     * 
     * @param source image to test
     */
    private void singleNullOpTest(RenderedImage source) {

        // Null operation
        RenderedOp nullImage = NullDescriptor.create(source, null);

        // Tile indexes
        int minTileX = nullImage.getMinTileX();
        int minTileY = nullImage.getMinTileY();

        // Selection of the upper-left tile of the null image
        Raster upperLeftTile = nullImage.getTile(minTileX, minTileY);
     // Selection of the upper-left tile of the source image
        Raster upperLeftTileOld = source.getTile(minTileX, minTileY);
        // New Tile bounds
        int minX = upperLeftTile.getMinX();
        int minY = upperLeftTile.getMinY();
        int maxX = upperLeftTile.getWidth() + minX;
        int maxY = upperLeftTile.getHeight() + minY;
        
        // Old Tile bounds
        int minXOld = upperLeftTileOld.getMinX();
        int minYOld = upperLeftTileOld.getMinY();
        int maxXOld = upperLeftTileOld.getWidth() + minXOld;
        int maxYOld = upperLeftTileOld.getHeight() + minYOld;
        
        // Check if the bounds of the 2 tiles are equals
        assertEquals(minXOld, minX);
        assertEquals(minYOld, minY);
        assertEquals(maxXOld, maxX);
        assertEquals(maxYOld, maxY);

        // Cycle on the x-axis
        for (int x = minX; x < maxX; x++) {
            // Cycle on the y-axis
            for (int y = minY; y < maxY; y++) {
                // New value
                double value = upperLeftTile.getSampleDouble(x, y, 0);
                // Old value
                double valueOld = upperLeftTileOld.getSampleDouble(x, y, 0);
                // Check if the 2 values are equals
                assertEquals(value, valueOld, TOLERANCE);
            }
        }
    }
}
