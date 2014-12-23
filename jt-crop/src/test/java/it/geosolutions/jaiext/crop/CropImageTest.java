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
package it.geosolutions.jaiext.crop;

import static org.junit.Assert.*;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.stats.Statistics;
import it.geosolutions.jaiext.stats.Statistics.StatsType;
import it.geosolutions.jaiext.stats.StatisticsDescriptor;
import it.geosolutions.jaiext.testclasses.TestBase;
import it.geosolutions.rendered.viewer.RenderedImageBrowser;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.TileCache;
import javax.media.jai.operator.SubtractDescriptor;

import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.media.jai.util.SunTileCache;

/**
 * This test class compares the old Crop operation implementation with the new one. The optional presence of No Data and ROI is tested. Also the
 * capability of the new Crop operator to maintain (if present) the hints related to the tile cache is tested. This class can also show the result of
 * the crop operation without No Data and Roi, with ROI only and with ROI and NoData together by setting the JVM JAI.Ext interactive parameter to
 * true, and setting the JVM integer parameter respectively to 0,1 or 2.
 */
public class CropImageTest extends TestBase {
    /** No data value used for the tests */
    private final static byte noDataValue = 50;

    /** Source image */
    private static RenderedImage source;

    /** Destination no data array to use for filling the source NoData Values */
    private static double[] destNoData;

    @BeforeClass
    public static void initialSetup() {
        source = createTestImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataValue,
                false);
        destNoData = new double[] { 127 };
    }

    @Test
    public void testCropImagePB() {
        // Parameterblock creation
        ParameterBlock pb = buildParameterBlock(source, false, false, false);

        ParameterBlock pbNew = buildParameterBlock(source, true, false, false);

        // Images creation
        RenderedOp cropped = JAI.create("crop", pb);
        RenderedOp jaiextCropped = JAI.create("Crop", pbNew);
        // Test on the selected image
        assertImageEquals(cropped, jaiextCropped);

        // Display Image
        if (INTERACTIVE && TEST_SELECTOR == 0) {
            RenderedImageBrowser.showChain(jaiextCropped, false, false);
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Test
    public void testCropImageROI() {
        // Parameterblock creation
        ParameterBlock pb = buildParameterBlock(source, false, false, false);

        ParameterBlock pbNew = buildParameterBlock(source, true, true, false);
        // Images creation
        RenderedOp cropped = JAI.create("crop", pb);
        RenderedOp jaiextCropped = JAI.create("Crop", pbNew);
        // Check if the presence of the ROI reduces the image bounds
        Rectangle boundOld = cropped.getBounds();

        Rectangle boundNew = jaiextCropped.getBounds();

        boolean contained = boundNew.getMinX() >= boundOld.getMinX()
                && boundNew.getMinY() >= boundOld.getMinY()
                && boundNew.getMaxX() <= boundOld.getMaxX()
                && boundNew.getMaxY() <= boundOld.getMaxY();

        assertTrue(contained);
        // Display Image
        if (INTERACTIVE && TEST_SELECTOR == 1) {
            RenderedImageBrowser.showChain(jaiextCropped, false, true);
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Test
    public void testCropImageROINoData() {
        // Parameterblock creation
        ParameterBlock pb = buildParameterBlock(source, false, false, false);

        ParameterBlock pbNew = buildParameterBlock(source, true, true, true);
        // Images creation
        RenderedOp cropped = JAI.create("crop", pb);
        RenderedOp jaiextCropped = JAI.create("Crop", pbNew);

        Rectangle boundOld = cropped.getBounds();

        Rectangle boundNew = jaiextCropped.getBounds();
        // Check if the presence of the ROI reduces the image bounds
        boolean contained = boundNew.getMinX() >= boundOld.getMinX()
                && boundNew.getMinY() >= boundOld.getMinY()
                && boundNew.getMaxX() <= boundOld.getMaxX()
                && boundNew.getMaxY() <= boundOld.getMaxY();

        assertTrue(contained);
        // Check if the NoData values are taken into account
        int tileMinX = jaiextCropped.getMinTileX();
        int tileMinY = jaiextCropped.getMinTileY();

        Raster tile = jaiextCropped.getTile(tileMinX, tileMinY);

        int tileMinXpix = tile.getMinX();
        int tileMinYpix = tile.getMinY();

        int tileMaxXpix = tile.getWidth() + tileMinXpix;
        int tileMaxYpix = tile.getHeight() + tileMinYpix;

        boolean destinationNoDataFound = false;

        for (int i = tileMinXpix; i < tileMaxXpix; i++) {
            for (int j = tileMinYpix; j < tileMaxYpix; j++) {

                int value = tile.getSample(i, j, 0);

                if (value == (int) destNoData[0]) {
                    destinationNoDataFound = true;
                    break;
                }
            }
            if (destinationNoDataFound) {
                break;
            }
        }
        // Display Image
        if (INTERACTIVE && TEST_SELECTOR == 2) {
            RenderedImageBrowser.showChain(jaiextCropped, false, true);
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Test
    public void testTileCache() {
        // Creation of the Tile Cache
        TileCache tc = new SunTileCache();
        // Addition of the TileCache hints
        RenderingHints hints = new RenderingHints(JAI.KEY_TILE_CACHE, tc);
        // Parameterblock creation
        ParameterBlock pb = buildParameterBlock(source, true, false, false);
        // Crop operation
        RenderedOp jaiextCropped = JAI.create("Crop", pb, hints);
        // force to compute the image
        jaiextCropped.getColorModel();
        // Check if the Tile Cache used is the same
        assertSame(tc, jaiextCropped.getRenderingHint(JAI.KEY_TILE_CACHE));
    }

    @Test
    public void testNullTileCache() {
        // Addition of Null TileCache hints
        RenderingHints hints = new RenderingHints(JAI.KEY_TILE_CACHE, null);
        // Parameterblock creation
        ParameterBlock pb = buildParameterBlock(source, true, false, false);
        // Crop operation
        RenderedOp jaiCropped = JAI.create("Crop", pb, hints);
        // force to compute the image
        jaiCropped.getColorModel();
        // Check if the Tile Cache is not present
        assertNull(jaiCropped.getRenderingHint(JAI.KEY_TILE_CACHE));
    }

    @Test
    public void testNullTileCacheDescriptor() {
        // Addition of Null TileCache hints
        RenderingHints hints = new RenderingHints(JAI.KEY_TILE_CACHE, null);
        // Crop operation(with the descriptor)
        RenderedOp cropped = CropDescriptor.create(source, 10f, 10f, 20f, 20f, null, null, null,
                hints);
        // force to compute the image
        cropped.getColorModel();
        // Check if the Tile Cache is not present
        assertNull(cropped.getRenderingHint(JAI.KEY_TILE_CACHE));
    }

    // Utility method for checking if two images are equals
    private void assertImageEquals(RenderedOp first, RenderedOp second) {
        // Calculation of the image difference
        RenderedOp difference = SubtractDescriptor.create(first, second, null);
        // Selection of the Statistics
        StatsType[] statsType = new StatsType[] { StatsType.EXTREMA };
        // Calculation of the statistics
        RenderedOp stats = StatisticsDescriptor.create(difference, 1, 1, null, null, false,
                new int[] { 0, 1, 2 }, statsType, null);

        Statistics[][] results = (Statistics[][]) stats.getProperty(Statistics.STATS_PROPERTY);
        // Check if the Maximum and minimum for each band are equals
        for (int i = 0; i < results.length; i++) {
            double[] data = (double[]) results[i][0].getResult();
            assertEquals(data[0], data[1], 0.0);
        }
    }

    // Utility method for creating the image parameter blocks
    private ParameterBlock buildParameterBlock(RenderedImage source, boolean newDescriptor,
            boolean roiUsed, boolean noDataUsed) {
        // Creation of the parameterBlock associated with its operation
        ParameterBlockJAI pb;
        if (newDescriptor) {
            pb = new ParameterBlockJAI("Crop");
        } else {
            pb = new ParameterBlockJAI("crop");
        }
        // Setting of the source
        pb.setSource("source0", source);
        // Setting of the parameters
        pb.setParameter("x", (float) 0);
        pb.setParameter("y", (float) 0);
        pb.setParameter("width", (float) 20);
        pb.setParameter("height", (float) 20);
        if (newDescriptor) {
            // If ROI is present, then it is added
            if (roiUsed) {
                ROI roi = new ROIShape(new Rectangle(5, 5, 10, 10));
                pb.setParameter("ROI", roi);
            }
            // If NoData is present, then it is added
            if (noDataUsed) {
                Range noData = RangeFactory.create(noDataValue, true, noDataValue, true);
                pb.setParameter("NoData", noData);
                pb.setParameter("destNoData", destNoData);
            }

        }
        return pb;
    }
}
