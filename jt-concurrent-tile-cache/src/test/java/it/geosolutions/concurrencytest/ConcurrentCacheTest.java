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
package it.geosolutions.concurrencytest;

import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.stream.FileImageInputStream;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.TileCache;
import javax.media.jai.operator.ScaleDescriptor;

import org.junit.Test;

import com.sun.media.imageio.plugins.tiff.TIFFImageReadParam;
import com.sun.media.imageioimpl.plugins.tiff.TIFFImageReader;
import com.sun.media.imageioimpl.plugins.tiff.TIFFImageReaderSpi;
import com.sun.media.jai.util.SunTileCache;

import it.geosolutions.concurrent.ConcurrentTileCache;
import it.geosolutions.concurrent.ConcurrentTileCacheMultiMap;

public class ConcurrentCacheTest {

    public static final long DEFAULT_MEMORY_CAPACITY = 128 * 1024 * 1024;

    public static final int DEFAULT_MAX_REQUEST_PER_THREAD = 100;

    public static final int STARTING_REQUEST_PER_FIRST_THREAD = 1000;

    public static final int EXPONENT = 6;

    public final static Logger LOGGER = Logger.getLogger(ConcurrentCacheTest.class.toString());

    // diagnostics
    public static boolean DEFAULT_DIAGNOSTICS = false;

    // multiple simultaneous operations allowed
    public static boolean DEFAULT_MULTIOPERATIONS = false;

    // choice of the concurrency Level
    public static int DEFAULT_CONCURRENCY_LEVEL = 16;

    // Number of test per thread
    private int maxRequestPerThread;

    // latch for waiting all thread to finish
    private CountDownLatch latch;

    // variables for creating random image tile index
    private Random generator;

    private double minTileX;

    private double minTileY;

    private double maxTileX;

    private double maxTileY;

    // image to elaborate
    private RenderedImage image_;

    /** List of all used Caches */
    public enum CacheType {
        SUN_TILE_CACHE(0, "SunTileCache"), CONCURRENT_TILE_CACHE(1, "ConcurrentTileCache"), CONCURRENT_MULTIMAP_TILE_CACHE(
                2, "ConcurrentMultimapCache");

        private final int tileCache;

        private final String tileCacheString;

        CacheType(int value, String s) {
            this.tileCache = value;
            this.tileCacheString = s;
        }

        public int valueCache() {
            return tileCache;
        }

        public String cacheName() {
            return tileCacheString;
        }

    };

    /*
     * This test-class compares the functionality of the default Sun tilecache or the new implementation of the Concurrent tilecache. You should set
     * the boolean "concurrentOrDefault" to true or false for change the cache used. You can set the memory cache capacity.For the Concurrent tile
     * cache you also can change the concurrency level. The test performs up to 400 request with an increased number of threads from 1 to 64 and
     * calculate the throughput after running the test 10 times. For every run the test execute 1000 requests for the hotspot code compilation. When
     * you run the test it throws a ClassNotFoundException because it doesn't find the medialib accelerator but then continue working in pure java
     * mode.
     */

    // @Test
    public double[] testwriteImageAndWatchFlag(CacheType cacheUsed, int concurrencyLevel,
            long memoryCacheCapacity, RenderedImage img, String path, boolean diagnostics,
            boolean multipleOperations) throws IOException, InterruptedException {
        // sets the cache and is concurrency level and diagnostics if needed
        switch (cacheUsed) {
        case SUN_TILE_CACHE:
            SunTileCache sunCache = new SunTileCache();
            if (diagnostics) {
                sunCache.enableDiagnostics();
            }
            JAI.getDefaultInstance().setTileCache(sunCache);
            break;
        case CONCURRENT_TILE_CACHE:
            ConcurrentTileCache cTileCache = new ConcurrentTileCache();
            cTileCache.setConcurrencyLevel(concurrencyLevel);
            if (diagnostics) {
                cTileCache.enableDiagnostics();
            }
            JAI.getDefaultInstance().setTileCache(cTileCache);
            break;
        case CONCURRENT_MULTIMAP_TILE_CACHE:
            ConcurrentTileCacheMultiMap cmTileCache = new ConcurrentTileCacheMultiMap();
            cmTileCache.setConcurrencyLevel(concurrencyLevel);
            if (diagnostics) {
                cmTileCache.enableDiagnostics();
            }
            JAI.getDefaultInstance().setTileCache(cmTileCache);
            break;
        }
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(memoryCacheCapacity);
        JAI.getDefaultInstance().getTileScheduler().setParallelism(10);

        int threadMaxNumber = (int) Math.pow(2, EXPONENT);
        // throughput array for storing data
        double[] througputArray = new double[EXPONENT + 1];

        // Creation of the thread pool executor
        ThreadPoolExecutor pool = new ThreadPoolExecutor(threadMaxNumber, threadMaxNumber, 60,
                TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1000000));

        // Creation of a Rendered image to store the image
        RenderedImage image;

        generator = new Random();
        // Thoughput array index
        int index = 0;

        TIFFImageReader reader = null;
        final TIFFImageReadParam param;
        FileImageInputStream stream_in = null;

        try {

            if (path != null) {
                // Instantiation of the file-reader
                reader = (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();
                // Instantiation of the read-params
                param = new TIFFImageReadParam();
                final File inputFile = new File(path);
                // Instantiation of the imageinputstream and imageoutputstrem
                stream_in = new FileImageInputStream(inputFile);

                reader.setInput(stream_in);
                // Rendered image to store the image
                image = reader.readAsRenderedImage(0, param);
            } else {
                image = img;
            }

            // image elaboration
            image = ScaleDescriptor.create(image, Float.valueOf(2.0f), Float.valueOf(2.0f),
                    Float.valueOf(0.0f), Float.valueOf(0.0f), new InterpolationNearest(), null);

            image_ = ScaleDescriptor.create(image, Float.valueOf(3.5f), Float.valueOf(3.5f),
                    Float.valueOf(0.0f), Float.valueOf(0.0f), new InterpolationNearest(), null);

            // Saving of the tiles maximum and minimun index
            minTileX = image_.getMinTileX();
            minTileY = image_.getMinTileY();
            maxTileX = minTileX + image_.getNumXTiles();
            maxTileY = minTileY + image_.getNumYTiles();

            for (int s = 1; s <= threadMaxNumber; s = s * 2) {
                // latch for wait the completion of all threads
                latch = new CountDownLatch(s);
                // setting of the maximum number of request to do
                if (s <= 4) {

                    // at the first run one starting thread performs 1000 request
                    // allowing the hotspot
                    // to compile the thread instructions

                    if (s == 1) {
                        latch = new CountDownLatch(2);
                        maxRequestPerThread = STARTING_REQUEST_PER_FIRST_THREAD;
                        Worker firstThread = new Worker(multipleOperations);
                        WeigherPeriodic secondThread = new WeigherPeriodic(cacheUsed);
                        pool.execute(firstThread);
                        pool.execute(secondThread);
                        latch.await();
                        LOGGER.log(Level.INFO, "Starting Thread Executed");
                        latch = new CountDownLatch(s);
                    }

                    maxRequestPerThread = DEFAULT_MAX_REQUEST_PER_THREAD;
                } else {
                    // the total request number can grow until 400 then doesn't
                    // change
                    maxRequestPerThread = 4 * DEFAULT_MAX_REQUEST_PER_THREAD / s;
                    // maxRequestPerThread = DEFAULT_MAX_REQUEST_PER_THREAD;
                }

                // number of current threads
                int count = 1;
                long startTime = System.nanoTime();
                // generation of the threads
                while (count <= s) {
                    // random tile selection
                    final Worker prefetch = new Worker(multipleOperations);
                    // retrieving the task to the executor
                    pool.execute(prefetch);
                    count++;
                }
                latch.await();
                // the throughput is calculated as number of total request/ total
                // time(in seconds)
                double time = (System.nanoTime() - startTime) * (1E-9);
                througputArray[index] = (maxRequestPerThread * s) / time;
                LOGGER.log(Level.INFO, "Number of Threads: " + s);
                index++;

            }
        } finally {
            /*
             * All the readers, writers, and stream are closed even if the program throws an exception
             */
            try {
                if (stream_in != null) {
                    stream_in.flush();
                    stream_in.close();
                }
            } catch (Throwable t) {
                //
            }

            try {
                if (reader != null) {
                    reader.dispose();
                }
            } catch (Throwable t) {
                //
            }

        }

        // executor termination
        pool.shutdown();
        pool.awaitTermination(180, TimeUnit.SECONDS);

        return througputArray;

    }

    static public void main(String[] args) throws Exception {
        // initial settings
        // check if using the concurrent cache or default
        CacheType cacheUsed = CacheType.CONCURRENT_MULTIMAP_TILE_CACHE;
        // sets the concurrency level
        int concurrencyLevel = DEFAULT_CONCURRENCY_LEVEL;
        // sets the memory cache capacity
        long memoryCacheCapacity = DEFAULT_MEMORY_CAPACITY;
        // choice of using a synthetic or a real image
        boolean syntheticImage = false;
        // diagnostics
        boolean diagnosticEnabled = DEFAULT_DIAGNOSTICS;
        // multiple tiles operations
        boolean multipleOp = DEFAULT_MULTIOPERATIONS;
        // loaded data
        RenderedImage imageSynth = getSynthetic(1);
        String path = null;

        boolean mainData = args != null;
        if (mainData) {
            if (args.length > 0) {
                cacheUsed = CacheType.values()[Integer.parseInt(args[0])];

                if (cacheUsed !=  CacheType.SUN_TILE_CACHE) {
                    concurrencyLevel = Integer.parseInt(args[1]);
                    memoryCacheCapacity = Long.parseLong(args[2]);
                    diagnosticEnabled = Boolean.parseBoolean(args[3]);
                    multipleOp = Boolean.parseBoolean(args[4]);
                    syntheticImage = Boolean.parseBoolean(args[5]);
                    if (syntheticImage) {
                        imageSynth = getSynthetic(1);
                    } else {
                        path = args[6];
                    }
                } else {
                    memoryCacheCapacity = Long.parseLong(args[1]);
                    diagnosticEnabled = Boolean.parseBoolean(args[2]);
                    multipleOp = Boolean.parseBoolean(args[3]);
                    syntheticImage = Boolean.parseBoolean(args[4]);
                    if (syntheticImage) {
                        imageSynth = getSynthetic(1);
                    } else {
                        path = args[5];
                    }
                }
            }

        }
        // setting the logger
        LOGGER.setLevel(Level.FINE);

        FileHandler fileTxt = new FileHandler(
                "target/ConcurrentCacheTestLog.txt");
        // Log Handler no used for now
        // LOGGER.addHandler(fileTxt);

        // number of tests to do
        int numTest = 10;
        // array for storing the different throughput
        double[][] dataTest = new double[numTest][EXPONENT + 1];
        for (int f = 0; f < numTest; f++) {
            LOGGER.log(Level.INFO, "Test N�." + (f + 1));
            dataTest[f] = new ConcurrentCacheTest().testwriteImageAndWatchFlag(cacheUsed,
                    concurrencyLevel, memoryCacheCapacity, imageSynth, path, diagnosticEnabled,
                    multipleOp);

        }

        // showing the result
        String stringConcurrent = " with " + cacheUsed.tileCacheString;
        if (cacheUsed != CacheType.SUN_TILE_CACHE) {
            stringConcurrent += " and " + concurrencyLevel + " segments";
        }
        for (int f = 0; f < numTest; f++) {
            for (int g = 0; g < dataTest[f].length; g++) {
                LOGGER.log(Level.INFO,
                        "Throughput in test " + (f + 1) + " for " + (int) (Math.pow(2, g))
                                + " threads is " + (int) dataTest[f][g] + " requests/second"
                                + stringConcurrent);

            }
        }

    }

    private class Worker implements Runnable {

        private boolean multipleTestOperations;

        private Worker(boolean multipleTestOperations) {
            this.multipleTestOperations = multipleTestOperations;
        }

        // @Override
        public void run() {
            if (!multipleTestOperations) {
                int i = 0;
                while (i < maxRequestPerThread) {
                    double dataX = generator.nextDouble();
                    double dataY = generator.nextDouble();
                    final int tilex = (int) (dataX * (maxTileX - minTileX) + minTileX);
                    final int tiley = (int) (dataY * (maxTileY - minTileY) + minTileY);
                    image_.getTile(tilex, tiley);
                    i++;
                }
                JAI.getDefaultInstance().getTileCache().removeTiles(image_);

                latch.countDown();

            } else {
                JAI.getDefaultInstance().getTileCache().getTiles(image_);
                JAI.getDefaultInstance().getTileCache().removeTiles(image_);
                latch.countDown();

            }
        }

    }

    /** This Runnable is used for checking the cache weigh on runtime */
    private class WeigherPeriodic implements Runnable {

        private CacheType typeCache;

        private WeigherPeriodic(CacheType cacheUsed) {
            this.typeCache = cacheUsed;
        }

        // @Override
        public void run() {
            int i = 0;
            while (i < 10) {
                // get the current cache
                TileCache cache = JAI.getDefaultInstance().getTileCache();
                long memory = 0;
                switch (typeCache) {
                // select the tile cache type, for the last two types no memory usage is
                // calculated
                case SUN_TILE_CACHE:
                    SunTileCache sunCache = (SunTileCache) cache;
                    memory = sunCache.getCacheMemoryUsed();
                    break;
                case CONCURRENT_MULTIMAP_TILE_CACHE:
                    ConcurrentTileCacheMultiMap cmTileCache = (ConcurrentTileCacheMultiMap) cache;
                    memory = cmTileCache.getCacheMemoryUsed();
                    break;
                }

                LOGGER.log(Level.INFO, "Current Memory Used: " + memory / (1024) + " Kb");
                i++;
            }
            latch.countDown();

        }

    }

    // simple method for creating a synthetic grayscale image
    public static RenderedImage getSynthetic(final double maximum) {
        final float width = 1000;
        final float height = 1000;
        ParameterBlock pb = new ParameterBlock();
        pb.add(width);
        pb.add(height);
        pb.add(new Integer[] { 1 });
        // Create the constant operation.
        return JAI.create("constant", pb);

    }

    @Test
    public void emptyTest() {
    }

}
