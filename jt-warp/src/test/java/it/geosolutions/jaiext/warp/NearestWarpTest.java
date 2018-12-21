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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.geom.AffineTransform;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Arrays;

import javax.media.jai.Interpolation;
import javax.media.jai.ROI;
import javax.media.jai.TiledImage;
import javax.media.jai.WarpAffine;
import javax.media.jai.operator.ConstantDescriptor;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import it.geosolutions.jaiext.JAIExt;

/**
 * Test class which extends the TestWarp class and executes all the tests with the nearest-neighbor interpolation.
 */
public class NearestWarpTest extends TestWarp {


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
        interpType = InterpolationType.NEAREST_INTERP;

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
     * Test ROI intersection check optimization
     */
    @Test
    public void testROIIntersect(){
        final int width = 48000;
        final int height = 36000;
        final int tileSize = 128;
        final int tileX = (width / tileSize) - 2;
        final int tileY = (height / tileSize) - 2;
        final int pixelsInTiles = tileSize * tileSize;
        final int pixelValue[] = new int[pixelsInTiles];
        final double backgroundValue = 99;
        final int validValue = 50;

        ComponentSampleModel tileSampleModel = new ComponentSampleModel(DataBuffer.TYPE_BYTE, width, height, 1, width,
                new int[] { 0 });
        RenderedImage source = new TiledImage(tileSampleModel, tileSize, tileSize);

        // Setting 2 tiles across the ROI edge
        WritableRaster rasterTile = (WritableRaster) source.getTile(tileX, tileY - 1);
        Arrays.fill(pixelValue, validValue);
        rasterTile.setPixels(tileX * tileSize, (tileY - 1) * tileSize, tileSize, tileSize,
                pixelValue);
        rasterTile = (WritableRaster) source.getTile(tileX - 1, tileY - 1);
        rasterTile.setPixels((tileX - 1) * tileSize, (tileY - 1) * tileSize, tileSize,
                tileSize, pixelValue);

        // Makes the ROI slightly smaller with respect to the source image
        final int roiWidth = tileX * tileSize;
        final int roiHeight = tileY * tileSize;
        RenderedImage roiSource = ConstantDescriptor.create(Float.valueOf(roiWidth), Float.valueOf(roiHeight),
                new Integer[] { 255 }, null);
        ROI roi = new ROI(roiSource);

        RenderedImage warpImage = new WarpNearestOpImage(source, null, null, new WarpAffine(new AffineTransform()),
                Interpolation.getInstance(Interpolation.INTERP_NEAREST), roi, null, new double[] { backgroundValue });

        long memBefore = (Runtime.getRuntime().freeMemory() / (1024 * 1024));
        long start = System.nanoTime();
        boolean outOfMemory = false;

        Raster outsideRoi = null;
        Raster insideRoi = null;
        try {
            // read a tile outside of the roi. that should be filled with background
            outsideRoi = warpImage.getTile(tileX, tileY - 1);
            // read a tile inside the roi. that should contain valid pixels
            insideRoi = warpImage.getTile(tileX - 1, tileY - 1);
        } catch (OutOfMemoryError oom) {
            outOfMemory = true;
        }
        long elapsedTimeInMilliseconds = (System.nanoTime() - start) / 1000000;
        long memAfter = (Runtime.getRuntime().freeMemory() / (1024 * 1024));

        // Without the optimization check on ROI intersection:
        // - Maven may throw an Out Of Memory Exception
        // - via Eclipse (not OOM), elapsed time would have been between
        // 2000 and 4000 milliseconds. With the fix it's almost immediate
        // Moreover, without the fix, the GC would have been invoked freeing much memory
        assertTrue(!outOfMemory && (elapsedTimeInMilliseconds < 500 || Math.abs(memAfter - memBefore) < 10));

        // Also check that the pixels outside the ROI contain background values
        // and pixels inside the ROI contain valid values.
        final byte pixelsInsideRoi[] = new byte[pixelsInTiles];
        final byte pixelsOutsideRoi[] = new byte[pixelsInTiles];

        outsideRoi.getDataElements((tileX) * tileSize, (tileY - 1) * tileSize, tileSize,
                tileSize, pixelsOutsideRoi);
        insideRoi.getDataElements((tileX - 1) * tileSize, (tileY - 1) * tileSize, tileSize,
                tileSize, pixelsInsideRoi);
        int backgroundPixels = 0;
        int validPixels = 0;
        for (int i = 0; i < pixelsInTiles; i++) {
            backgroundPixels += (pixelsOutsideRoi[i] == backgroundValue ? 1 : 0);
            validPixels += (pixelsInsideRoi[i] == validValue ? 1 : 0);
        }
        assertTrue(backgroundPixels == pixelsInTiles);
        assertTrue(validPixels == pixelsInTiles);
    }

    /**
     * Static method for disposing the test environment.
     */
    @AfterClass
    public static void finalStuff() {
        TestWarp.finalStuff();
    }

}
