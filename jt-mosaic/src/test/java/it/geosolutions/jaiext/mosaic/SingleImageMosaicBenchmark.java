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

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.operator.MosaicDescriptor;
import org.junit.Ignore;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * JMH benchmark comparing {@link SingleImageMosaicOpImage} against the generic {@link MosaicOpImage}
 * on the single byte OVERLAY source case, for each ROI backing the tile loop reads differently. One
 * operation composites a whole 1024x1024 three band image, four 512px tiles.
 *
 * <p>Run it with {@code mvn test -pl jt-mosaic -Dtest=SingleImageMosaicBenchmark#run} after
 * commenting out the {@link Ignore}; the six combinations take about 50 seconds. The printed JMH
 * table is the output, there is nothing to assert. Run it before and after a change to the tile loop
 * to see what the change cost or gained.
 *
 * <p>Last measured on JDK 8, ms/op with the JMH 99.9% error, two runs:
 *
 * <pre>
 * roi              generic                       fast                    speedup
 * NONE           12.98 +- 1.08  12.80 +- 1.28   8.15 +- 0.46  8.59 +- 1.28  1.5x
 * PACKED         15.08 +- 2.92  14.66 +- 1.76   9.82 +- 2.06  9.29 +- 1.10  1.6x
 * BYTE_COMPONENT 12.97 +- 0.68  12.58 +- 0.40   7.40 +- 2.21  7.60 +- 1.21  1.7x
 * </pre>
 *
 * <p>A variant splitting the tile loop per case, the way the rest of jai-ext splits its loops, was
 * measured against the single loop and landed inside the noise (a few percent either way depending
 * on the ROI), so the single loop stays.
 *
 * <p>Absolute values track the machine and the error stays around 5%, so only differences well over
 * 10% mean anything. One tried variant measured slower and was dropped: normalizing both ROI layouts
 * to one byte per pixel before the loop cost 15% on the fast path, the extra pass and allocation
 * being worse than the per-pixel branch they removed.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
// each op allocates 3MB of tile rasters, a small heap turns GC into most of the variance
@Fork(value = 1, jvmArgs = {"-Xmx1g"})
@State(Scope.Benchmark)
public class SingleImageMosaicBenchmark {

    private static final int SIZE = 1024;
    private static final int TILE = 512;

    /** The three ways the tile loop can read a ROI: not at all, packed bits, one byte per pixel. */
    public enum RoiKind {
        NONE {
            ROI[] create() {
                return null;
            }
        },
        /** {@link ROIShape} rasterizes to a bilevel, packed image. */
        PACKED {
            ROI[] create() {
                return new ROI[] {new ROIShape(new Rectangle(100, 100, SIZE - 300, SIZE - 300))};
            }
        },
        /**
         * A byte mask kept byte backed. Plain {@link ROI} binarizes in {@link ROI#getAsImage()}, even
         * when built over a byte image, so only a subclass that overrides it (GeoTools ROIGeometry)
         * reaches the byte reading branch of the tile loop.
         */
        BYTE_COMPONENT {
            ROI[] create() {
                final PlanarImage mask = PlanarImage.wrapRenderedImage(byteMask());
                return new ROI[] {new ROI(mask, 1) {
                    @Override
                    public PlanarImage getAsImage() {
                        return mask;
                    }
                }};
            }
        };

        abstract ROI[] create();

        static RenderedImage byteMask() {
            BufferedImage mask = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_BYTE_GRAY);
            WritableRaster raster = mask.getRaster();
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    boolean inside = x > 100 && x < SIZE - 200 && y > 100 && y < SIZE - 200;
                    raster.setSample(x, y, 0, inside ? 255 : 0);
                }
            }
            return mask;
        }
    }

    @Param({"NONE", "PACKED", "BYTE_COMPONENT"})
    public RoiKind roi;

    private MosaicOpImage generic;

    private MosaicOpImage fast;

    /**
     * Both images are built once: {@link MosaicOpImage#computeTile} allocates a fresh raster on
     * every call and never consults the tile cache, so no operation reuses another one's work.
     */
    @Setup
    public void setup() {
        RenderedImage source = source();
        ROI[] rois = roi.create();
        generic = build(false, source, rois);
        fast = build(true, source, rois);
    }

    @Benchmark
    public void generic(Blackhole bh) {
        computeAllTiles(generic, bh);
    }

    @Benchmark
    public void fast(Blackhole bh) {
        computeAllTiles(fast, bh);
    }

    private static void computeAllTiles(MosaicOpImage mosaic, Blackhole bh) {
        for (int tx = 0; tx < mosaic.getNumXTiles(); tx++) {
            for (int ty = 0; ty < mosaic.getNumYTiles(); ty++) {
                bh.consume(mosaic.computeTile(tx, ty));
            }
        }
    }

    private MosaicOpImage build(boolean useFast, RenderedImage source, ROI[] rois) {
        // MosaicOpImage casts the source list to Vector, no other List will do
        Vector<RenderedImage> sources = new Vector<>();
        sources.add(source);
        ImageLayout layout = new ImageLayout(0, 0, SIZE, SIZE);
        layout.setTileWidth(TILE);
        layout.setTileHeight(TILE);
        double[] destNoData = {255, 255, 255};
        // nodata is always on: it is the case the crop operation brings to the mosaic
        Range[] noDatas = {RangeFactory.create((byte) 0, true, (byte) 0, true)};
        if (useFast) {
            return new SingleImageMosaicOpImage(sources, layout, null,
                    MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, rois, null, destNoData, noDatas);
        }
        return new MosaicOpImage(sources, layout, null, MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null,
                rois, null, destNoData, noDatas);
    }

    /** Three band byte image with a gradient and a black block that the nodata range catches. */
    private RenderedImage source() {
        BufferedImage bi = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_3BYTE_BGR);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                boolean blackBlock = x >= 600 && x < 900 && y >= 100 && y < 500;
                int v = blackBlock ? 0 : Math.max(1, (x + y) % 256);
                bi.getRaster().setSample(x, y, 0, v);
                bi.getRaster().setSample(x, y, 1, blackBlock ? 0 : (v + 30) % 256);
                bi.getRaster().setSample(x, y, 2, blackBlock ? 0 : (v + 60) % 256);
            }
        }
        return PlanarImage.wrapRenderedImage(bi);
    }

    @Test
    @Ignore("manual benchmark, prints timings rather than asserting")
    public void run() throws RunnerException {
        new Runner(new OptionsBuilder().include(getClass().getName()).build()).run();
    }
}
