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
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.TiledImage;
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
        // A ROI backed by a byte image, as GeoTools ROIGeometry produces for a reprojection valid
        // area: exercises the byte array ROI read rather than the packed bit one. Plain ROI is not
        // enough, its getAsImage binarizes even a byte mask, so getAsImage is overridden here.
        BufferedImage mask = new BufferedImage(W, H, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                mask.getRaster().setSample(x, y, 0, (x + y) % 3 == 0 ? 0 : 255);
            }
        }
        final PlanarImage byteMask = PlanarImage.wrapRenderedImage(mask);
        ROI roi = new ROI(byteMask, 1) {
            @Override
            public PlanarImage getAsImage() {
                return byteMask;
            }
        };
        assertSameAsGeneric(new ROI[] {roi}, null);
    }

    /**
     * A ROI covering the whole destination cannot exclude any pixel, so passing it must produce the
     * same output as passing none. CropCRIF relies on this: it used to hand the mosaic a ROI equal
     * to the crop bounds, which only forced the per-pixel ROI branch plus a rasterization of the
     * shape.
     */
    @Test
    public void testFullBoundsRoiSameAsNoRoi() {
        ROI fullBounds = new ROIShape(new Rectangle(0, 0, W, H));
        Range black = RangeFactory.create((byte) 0, true, (byte) 0, true);
        assertSamePixels(
                render(new ROI[] {fullBounds}, new Range[] {black}, layout()),
                render(null, new Range[] {black}, layout()));
        assertSamePixels(render(new ROI[] {fullBounds}, null, layout()), render(null, null, layout()));
    }

    /**
     * {@link #testFullBoundsRoiSameAsNoRoi} with the bounds offset from the tile grid, so edge tiles
     * hang over them and the source is larger than the destination on every side. This is the shape
     * CropCRIF actually produces, and the one case where the two could conceivably differ.
     */
    @Test
    public void testOffsetBoundsRoiSameAsNoRoi() {
        Rectangle cropBounds = new Rectangle(10, 5, 70, 50);
        ImageLayout layout = new ImageLayout(cropBounds.x, cropBounds.y, cropBounds.width,
                cropBounds.height);
        // tile grid anchored at the origin, not at the crop corner: edge tiles straddle the bounds
        layout.setTileGridXOffset(0);
        layout.setTileGridYOffset(0);
        layout.setTileWidth(32);
        layout.setTileHeight(32);

        ROI boundsRoi = new ROIShape(cropBounds);
        Range black = RangeFactory.create((byte) 0, true, (byte) 0, true);
        assertSamePixels(cropBounds, render(new ROI[] {boundsRoi}, new Range[] {black}, layout),
                render(null, new Range[] {black}, layout));
        assertSamePixels(cropBounds, render(new ROI[] {boundsRoi}, null, layout),
                render(null, null, layout));
    }

    /**
     * A destination larger than the source on every side: the pixels the source does not cover must
     * get the destination nodata band by band, the same values the generic mosaic writes there.
     */
    @Test
    public void testDestinationLargerThanSource() {
        Range black = RangeFactory.create((byte) 0, true, (byte) 0, true);
        ROI roi = new ROIShape(new Rectangle(20, 15, 50, 40));
        assertSameAsGeneric(largerLayout(), null, null);
        assertSameAsGeneric(largerLayout(), null, new Range[] {black});
        assertSameAsGeneric(largerLayout(), new ROI[] {roi}, new Range[] {black});
    }

    /**
     * A ROI whose image is neither packed bits nor bytes: the tile loop cannot read it, so the fast
     * path must hand the tile back to the generic mosaic instead of failing.
     */
    @Test
    public void testUnreadableRoiFallsBackToGeneric() {
        Range black = RangeFactory.create((byte) 0, true, (byte) 0, true);
        assertSameAsGeneric(new ROI[] {ushortRoi()}, new Range[] {black});
    }

    /** A single band ushort mask, the shape no known ROI takes, wrapped so getAsImage keeps it. */
    private ROI ushortRoi() {
        TiledImage mask = new TiledImage(0, 0, W, H, 0, 0,
                new PixelInterleavedSampleModel(DataBuffer.TYPE_USHORT, 16, 16, 1, 16, new int[] {0}),
                null);
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                mask.setSample(x, y, 0, x >= 20 && x < 70 && y >= 15 && y < 55 ? 1 : 0);
            }
        }
        return new ROI(mask, 1) {
            @Override
            public PlanarImage getAsImage() {
                return mask;
            }
        };
    }

    private void assertSameAsGeneric(ROI[] rois, Range[] noDatas) {
        assertSameAsGeneric(layout(), rois, noDatas);
    }

    /** Runs the comparison over every source layout, the tile loop indexes each one differently. */
    private void assertSameAsGeneric(ImageLayout layout, ROI[] rois, Range[] noDatas) {
        for (RenderedImage source : sources()) {
            String kind = source.getSampleModel().getClass().getSimpleName() + " "
                    + source.getSampleModel().getNumBands() + " bands at " + source.getMinX() + ","
                    + source.getMinY();
            Rectangle area = new Rectangle(layout.getMinX(null), layout.getMinY(null),
                    layout.getWidth(null), layout.getHeight(null));
            assertSamePixels(kind, area, generic(source, layout, rois, noDatas).getData(),
                    fast(source, layout, rois, noDatas).getData());
        }
    }

    /** Renders the fast path over the plain three band source, with the given ROI and nodata. */
    private Raster render(ROI[] rois, Range[] noDatas, ImageLayout layout) {
        return fast(threeBandSource(), layout, rois, noDatas).getData();
    }

    private MosaicOpImage generic(RenderedImage source, ImageLayout layout, ROI[] rois,
            Range[] noDatas) {
        return new MosaicOpImage(asList(source), layout, null, MosaicDescriptor.MOSAIC_TYPE_OVERLAY,
                null, rois, null, destNoData(), noDatas);
    }

    private MosaicOpImage fast(RenderedImage source, ImageLayout layout, ROI[] rois,
            Range[] noDatas) {
        return new SingleImageMosaicOpImage(asList(source), layout, null,
                MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, rois, null, destNoData(), noDatas);
    }

    /** MosaicOpImage casts the source list to Vector, no other List will do. */
    private static Vector<RenderedImage> asList(RenderedImage source) {
        Vector<RenderedImage> sources = new Vector<>();
        sources.add(source);
        return sources;
    }

    /** Small tiles, so several computeTile calls run including partially covered ones. */
    private ImageLayout layout() {
        ImageLayout layout = new ImageLayout(0, 0, W, H);
        layout.setTileWidth(32);
        layout.setTileHeight(32);
        return layout;
    }

    /** Wraps the source with a 20 pixel margin, so every side has tiles the source misses. */
    private ImageLayout largerLayout() {
        ImageLayout layout = new ImageLayout(-20, -20, W + 40, H + 40);
        layout.setTileWidth(32);
        layout.setTileHeight(32);
        return layout;
    }

    /**
     * A different value per band, so a background written band-blind cannot pass unnoticed. Longer
     * than any source used here: MosaicOpImage keeps the first values and drops the rest.
     */
    private double[] destNoData() {
        return new double[] {200, 128, 255, 64};
    }

    private void assertSamePixels(Raster expected, Raster actual) {
        assertSamePixels("", new Rectangle(0, 0, W, H), expected, actual);
    }

    private void assertSamePixels(Rectangle area, Raster expected, Raster actual) {
        assertSamePixels("", area, expected, actual);
    }

    private void assertSamePixels(String kind, Rectangle area, Raster expected, Raster actual) {
        assertEquals(kind + " band count", expected.getNumBands(), actual.getNumBands());
        for (int y = area.y; y < area.y + area.height; y++) {
            for (int x = area.x; x < area.x + area.width; x++) {
                assertSamePixel(kind, expected, actual, x, y);
            }
        }
    }

    private void assertSamePixel(String kind, Raster expected, Raster actual, int x, int y) {
        for (int b = 0; b < expected.getNumBands(); b++) {
            assertEquals(kind + " pixel " + x + "," + y + " band " + b,
                    expected.getSample(x, y, b), actual.getSample(x, y, b));
        }
    }

    /**
     * The source layouts whose data the tile loop reaches through different accessor offsets: pixel
     * interleaved, single band, palette, sub-byte packed palette, and banded with an origin and a
     * tile grid of its own. With a single source the destination sample model is the source one, so
     * each entry changes the destination layout as well.
     */
    private List<RenderedImage> sources() {
        return Arrays.asList(threeBandSource(), graySource(), paletteSource(), fourBitSource(),
                bandedSource());
    }

    /**
     * Four bit palette source, the shape a paletted GeoTIFF read at its native depth takes. Its
     * sample model reports a byte data type, but the samples are packed several to a byte, so
     * RasterAccessor unpacks them into int arrays and the fast tile loop has to hand the tile back
     * to the generic mosaic.
     */
    private RenderedImage fourBitSource() {
        byte[] r = new byte[16];
        byte[] g = new byte[16];
        byte[] b = new byte[16];
        for (int i = 0; i < 16; i++) {
            r[i] = (byte) (i * 16);
            g[i] = (byte) (255 - i * 16);
            b[i] = (byte) ((i * 7) % 256);
        }
        IndexColorModel icm = new IndexColorModel(4, 16, r, g, b);
        BufferedImage bi = new BufferedImage(W, H, BufferedImage.TYPE_BYTE_BINARY, icm);
        fill(bi, 1);
        return PlanarImage.wrapRenderedImage(bi);
    }

    /** Single band gray, so the loop runs with one band and a pixel stride of one. */
    private RenderedImage graySource() {
        BufferedImage bi = new BufferedImage(W, H, BufferedImage.TYPE_BYTE_GRAY);
        fill(bi, 1);
        return PlanarImage.wrapRenderedImage(bi);
    }

    /**
     * Palette source: the nodata lookup is built over palette indexes, not colors. A single source is
     * never expanded to RGB, MosaicOpImage keeps its sample model as the destination one.
     */
    private RenderedImage paletteSource() {
        // a colored palette, a gray ramp would need no expansion and read back as a single band
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];
        for (int i = 0; i < 256; i++) {
            r[i] = (byte) i;
            g[i] = (byte) (255 - i);
            b[i] = (byte) ((i * 7) % 256);
        }
        IndexColorModel icm = new IndexColorModel(8, 256, r, g, b);
        BufferedImage bi = new BufferedImage(W, H, BufferedImage.TYPE_BYTE_INDEXED, icm);
        fill(bi, 1);
        return PlanarImage.wrapRenderedImage(bi);
    }

    /**
     * Three bands in separate banks, origin away from the destination origin and 16 pixel tiles
     * against the 32 pixel destination ones, so the accessor offsets cannot come out right by
     * accident.
     */
    private RenderedImage bandedSource() {
        // TiledImage takes its tile size from the sample model, hence the 16 pixel one here
        BandedSampleModel sm = new BandedSampleModel(DataBuffer.TYPE_BYTE, 16, 16, 3);
        TiledImage image = new TiledImage(7, 3, W, H, 7, 3, sm, null);
        for (int y = 3; y < H + 3; y++) {
            for (int x = 7; x < W + 7; x++) {
                boolean blackBlock = x >= 60 && x < 90 && y >= 10 && y < 50;
                int v = blackBlock ? 0 : Math.max(1, (x + y) % 256);
                image.setSample(x, y, 0, v);
                image.setSample(x, y, 1, blackBlock ? 0 : (v + 30) % 256);
                image.setSample(x, y, 2, blackBlock ? 0 : (v + 60) % 256);
            }
        }
        return image;
    }

    /** Gradient with a zero block that the nodata range catches, on every band. */
    private void fill(BufferedImage bi, int bands) {
        WritableRaster raster = bi.getRaster();
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                boolean blackBlock = x >= 60 && x < 90 && y >= 10 && y < 50;
                int v = blackBlock ? 0 : Math.max(1, (x + y) % 256);
                for (int b = 0; b < bands; b++) {
                    raster.setSample(x, y, b, blackBlock ? 0 : (v + 30 * b) % 256);
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
