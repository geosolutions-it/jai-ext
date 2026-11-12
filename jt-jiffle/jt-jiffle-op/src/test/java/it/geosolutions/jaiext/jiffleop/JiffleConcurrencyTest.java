/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2026 GeoSolutions
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.geosolutions.jaiext.jiffleop;

import static org.junit.Assert.assertArrayEquals;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;

import org.junit.Test;

public class JiffleConcurrencyTest {

    private static final String SCRIPT = "h = src;\n"
            + "h = con(isnan(h) || h <= -32767, 0, h);\n"
            + "v = round((h + 10000) * 10);\n"
            + "dest[0] = floor(v / 65536);\n"
            + "dest[1] = floor(v / 256) % 256;\n"
            + "dest[2] = v % 256;\n";

    private static final int SIZE = 512;

    private static final int TILE = 64;

    @Test
    public void testConcurrentGetTile() throws Exception {
        RenderedImage src = buildSource();
        Raster reference = buildJiffle(src).getData();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            for (int round = 0; round < 5; round++) {
                RenderedOp op = buildJiffle(src);
                List<Future<Raster>> futures = new ArrayList<>();
                for (int ty = 0; ty < SIZE / TILE; ty++) {
                    for (int tx = 0; tx < SIZE / TILE; tx++) {
                        final int x = tx, y = ty;
                        futures.add(pool.submit(() -> op.getTile(x, y)));
                    }
                }
                for (Future<Raster> future : futures) {
                    Raster tile = future.get();
                    int minX = tile.getMinX(), minY = tile.getMinY();
                    assertArrayEquals("tile at " + minX + "," + minY,
                            reference.getPixels(minX, minY, TILE, TILE, (int[]) null),
                            tile.getPixels(minX, minY, TILE, TILE, (int[]) null));
                }
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private static RenderedImage buildSource() {
        SampleModel sm = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_FLOAT, TILE, TILE, 1);
        TiledImage image = new TiledImage(0, 0, SIZE, SIZE, 0, 0, sm, PlanarImage.createColorModel(sm));
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                image.setSample(x, y, 0, (x * 7 + y * 3) % 900 - 300 + (x % 5 == 0 ? 0.25f : 0));
            }
        }
        return image;
    }

    private static RenderedOp buildJiffle(RenderedImage src) {
        RenderingHints hints = new RenderingHints(JAI.KEY_TILE_CACHE, null);
        hints.put(JAI.KEY_IMAGE_LAYOUT, new ImageLayout().setTileWidth(TILE).setTileHeight(TILE));
        return JiffleDescriptor.create(new RenderedImage[] {src}, new String[] {"src"}, "dest", SCRIPT,
                new Rectangle(0, 0, SIZE, SIZE), DataBuffer.TYPE_BYTE, 3, null, null, null, hints);
    }
}
