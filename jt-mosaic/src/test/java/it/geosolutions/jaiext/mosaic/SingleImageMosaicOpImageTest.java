/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
*    http://www.geo-solutions.it/
*    Copyright 2026 GeoSolutions

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
package it.geosolutions.jaiext.mosaic;

import static org.junit.Assert.assertEquals;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.Vector;
import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.operator.MosaicDescriptor;
import org.junit.Test;

/**
 * Verifies that {@link SingleImageMosaicOpImage} produces exactly the same output as the generic
 * {@link MosaicOpImage} for the single byte OVERLAY source case, with and without ROI and nodata.
 */
public class SingleImageMosaicOpImageTest {

    private static final int W = 100;
    private static final int H = 80;

    @Test
    public void testPlainCopy() {
        assertSameAsGeneric(null, null);
    }

    @Test
    public void testRoiToBackground() {
        ROI roi = new ROIShape(new Rectangle(20, 15, 50, 40));
        assertSameAsGeneric(new ROI[] {roi}, null);
    }

    @Test
    public void testNoDataToBackground() {
        Range black = RangeFactory.create((byte) 0, true, (byte) 0, true);
        assertSameAsGeneric(null, new Range[] {black});
    }

    @Test
    public void testRoiContainingAndDisjointTiles() {
        // ROI aligned so some 32px tiles fall fully inside (contains: ROI test skipped), some fully
        // outside (disjoint: pure background), some straddle (per-pixel). Exercises all branches.
        ROI roi = new ROIShape(new Rectangle(0, 0, 64, 64));
        Range black = RangeFactory.create((byte) 0, true, (byte) 0, true);
        assertSameAsGeneric(new ROI[] {roi}, new Range[] {black});
    }

    @Test
    public void testRoiAndNoData() {
        ROI roi = new ROIShape(new Rectangle(20, 15, 50, 40));
        Range black = RangeFactory.create((byte) 0, true, (byte) 0, true);
        assertSameAsGeneric(new ROI[] {roi}, new Range[] {black});
    }

    @Test
    public void testByteBackedRoi() {
        // A ROI backed by a byte (ComponentSampleModel) image, as produced by reprojection valid
        // areas: exercises the byte-array ROI read rather than the packed-bit one.
        BufferedImage mask = new BufferedImage(W, H, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                mask.getRaster().setSample(x, y, 0, (x + y) % 3 == 0 ? 0 : 255);
            }
        }
        ROI roi = new ROI(mask, 1);
        assertSameAsGeneric(new ROI[] {roi}, null);
    }

    private void assertSameAsGeneric(ROI[] rois, Range[] noDatas) {
        RenderedImage source = threeBandSource();
        Vector<RenderedImage> sources = new Vector<>();
        sources.add(source);
        double[] destNoData = {255, 255, 255};
        // small tiles so several computeTile calls run, including partially covered ones
        ImageLayout layout = new ImageLayout(0, 0, W, H);
        layout.setTileWidth(32);
        layout.setTileHeight(32);

        MosaicOpImage generic = new MosaicOpImage(sources, layout, null,
                MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, rois, null, destNoData, noDatas);
        SingleImageMosaicOpImage fast = new SingleImageMosaicOpImage(sources, layout, null,
                MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, rois, null, destNoData, noDatas);

        Raster expected = generic.getData();
        Raster actual = fast.getData();
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                for (int b = 0; b < 3; b++) {
                    assertEquals("pixel " + x + "," + y + " band " + b,
                            expected.getSample(x, y, b), actual.getSample(x, y, b));
                }
            }
        }
    }

    /** Three band byte image with a gradient and a black (0,0,0) block that acts as nodata. */
    private RenderedImage threeBandSource() {
        BufferedImage bi = new BufferedImage(W, H, BufferedImage.TYPE_3BYTE_BGR);
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                boolean blackBlock = x >= 60 && x < 90 && y >= 10 && y < 50;
                int v = blackBlock ? 0 : Math.max(1, (x + y) % 256);
                bi.getRaster().setSample(x, y, 0, v);
                bi.getRaster().setSample(x, y, 1, blackBlock ? 0 : (v + 30) % 256);
                bi.getRaster().setSample(x, y, 2, blackBlock ? 0 : (v + 60) % 256);
            }
        }
        return PlanarImage.wrapRenderedImage(bi);
    }
}
