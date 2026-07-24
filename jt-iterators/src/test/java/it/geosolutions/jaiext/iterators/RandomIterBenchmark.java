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
package it.geosolutions.jaiext.iterators;

import java.awt.image.DataBuffer;
import java.awt.image.PixelInterleavedSampleModel;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.media.jai.TiledImage;
import javax.media.jai.iterator.RandomIter;
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
 * JMH benchmark comparing {@link RandomIterComponentByte} against the {@link RandomIterFallbackByte}
 * it takes over from in {@link RandomIterFactory}, over the access patterns the resampling operations
 * produce. One operation reads three bands of {@link #READS} points from a 1024x1024 image tiled at
 * 256, so a full pass crosses every tile.
 *
 * <p>Run it with {@code mvn test -pl jt-iterators -Dtest=RandomIterBenchmark#run} after commenting
 * out the {@link Ignore}; the six combinations take about a minute. The printed JMH table is the
 * output, there is nothing to assert.
 *
 * <p>The patterns, all with the same number of reads. The first two are how the operations read, the
 * third is a stress case:
 *
 * <ul>
 * <li>SCAN: row by row, like a scale or a warp that reduced to an affine, which is what small tiles
 * do. Almost every read stays in the cached tile.
 * <li>WARP: row by row, with each point moved by a few pixels at random. Harder than the real thing:
 * an inverse projection moves smoothly from one pixel to the next, it does not jump about.
 * <li>RANDOM: anywhere in the image, so almost every read changes tile. No operation reads this way,
 * source points always come from a continuous transform. It is here to show the worst case. A caller
 * that did read this way should use {@link RandomIterFallbackByte}.
 * </ul>
 *
 * <p>Last measured on JDK 8, ms/op with the JMH 99.9% error, two runs:
 *
 * <pre>
 * pattern       fallback                    component byte              ratio
 * SCAN      9.98 +- 0.25  10.05 +- 0.91    4.75 +- 0.25   4.61 +- 0.18   2.1x faster
 * WARP      9.99 +- 0.37   9.99 +- 0.25    9.14 +- 0.47   8.97 +- 0.25   1.1x faster
 * RANDOM   21.52 +- 4.83  21.64 +- 1.99   38.27 +- 4.21  38.62 +- 12.31  1.8x SLOWER
 * </pre>
 *
 * <p>Reads are cheaper here, tile switches are not, so this iterator wins when many reads share a
 * tile and loses when they do not. Three guesses about the RANDOM row were measured and dropped: both
 * iterators fetch the same number of tiles (245753 over the same points), they allocate about the
 * same, and using a shift instead of a division for the tile index changes nothing. What is left is
 * the work done on each switch, which the fallback skips because it looks the tile up on every read
 * from its index tables, as stock JAI does.
 *
 * <p>The SCAN ratio mixes two changes, the cached tile and the direct array read, and this benchmark
 * does not tell them apart. Times depend on the machine, so only differences well over 10% count.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1, jvmArgs = {"-Xmx1g"})
@State(Scope.Benchmark)
public class RandomIterBenchmark {

    private static final int SIZE = 1024;
    private static final int TILE = 256;
    private static final int BANDS = 3;

    /** Points read per operation, enough to cross every tile several times. */
    private static final int READS = 1 << 20;

    /** The access patterns the resampling operations produce, see the class javadoc. */
    public enum Pattern {
        SCAN {
            void fill(int[] xs, int[] ys, Random random) {
                for (int i = 0; i < xs.length; i++) {
                    xs[i] = i % SIZE;
                    ys[i] = (i / SIZE) % SIZE;
                }
            }
        },
        WARP {
            void fill(int[] xs, int[] ys, Random random) {
                for (int i = 0; i < xs.length; i++) {
                    xs[i] = clamp(i % SIZE + random.nextInt(7) - 3);
                    ys[i] = clamp((i / SIZE) % SIZE + random.nextInt(7) - 3);
                }
            }
        },
        RANDOM {
            void fill(int[] xs, int[] ys, Random random) {
                for (int i = 0; i < xs.length; i++) {
                    xs[i] = random.nextInt(SIZE);
                    ys[i] = random.nextInt(SIZE);
                }
            }
        };

        abstract void fill(int[] xs, int[] ys, Random random);

        static int clamp(int v) {
            return Math.max(0, Math.min(SIZE - 1, v));
        }
    }

    @Param({"SCAN", "WARP", "RANDOM"})
    public Pattern pattern;

    private TiledImage image;

    private int[] xs;

    private int[] ys;

    /** Coordinates are precomputed: an operation must measure the reads, not their generation. */
    @Setup
    public void setup() {
        int[] bandOffsets = new int[BANDS];
        for (int b = 0; b < BANDS; b++) {
            bandOffsets[b] = b;
        }
        image = new TiledImage(0, 0, SIZE, SIZE, 0, 0, new PixelInterleavedSampleModel(
                DataBuffer.TYPE_BYTE, TILE, TILE, BANDS, TILE * BANDS, bandOffsets), null);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                for (int b = 0; b < BANDS; b++) {
                    image.setSample(x, y, b, (x * 7 + y * 13 + b * 3) & 0xFF);
                }
            }
        }
        xs = new int[READS];
        ys = new int[READS];
        // fixed seed: every run reads the same points, in the same order
        pattern.fill(xs, ys, new Random(42));
    }

    @Benchmark
    public void fallback(Blackhole bh) {
        readAll(new RandomIterFallbackByte(image, image.getBounds()), bh);
    }

    @Benchmark
    public void componentByte(Blackhole bh) {
        readAll(new RandomIterComponentByte(image), bh);
    }

    private void readAll(RandomIter iter, Blackhole bh) {
        int sum = 0;
        for (int i = 0; i < READS; i++) {
            int x = xs[i];
            int y = ys[i];
            for (int b = 0; b < BANDS; b++) {
                sum += iter.getSample(x, y, b);
            }
        }
        bh.consume(sum);
        iter.done();
    }

    @Test
    @Ignore("manual benchmark, prints timings rather than asserting")
    public void run() throws RunnerException {
        new Runner(new OptionsBuilder().include(getClass().getName()).build()).run();
    }
}
