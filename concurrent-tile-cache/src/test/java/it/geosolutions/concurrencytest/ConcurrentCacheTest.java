package it.geosolutions.concurrencytest;

import it.geosolutions.concurrent.ConcurrentTileCache;
import it.geosolutions.concurrentlinked.ConcurrentLinkedCache;

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
import javax.media.jai.operator.ScaleDescriptor;
import com.sun.media.imageio.plugins.tiff.TIFFImageReadParam;
import com.sun.media.imageioimpl.plugins.tiff.TIFFImageReader;
import com.sun.media.imageioimpl.plugins.tiff.TIFFImageReaderSpi;
import com.sun.media.jai.util.SunTileCache;

public class ConcurrentCacheTest {

private static final long DEFAULT_MEMORY_CAPACITY = 128 * 1024 * 1024;

private static final int DEFAULT_MAX_REQUEST_PER_THREAD = 100;

private static final int STARTING_REQUEST_PER_FIRST_THREAD = 1000;

private static final int EXPONENT = 6;

private final static Logger LOGGER = Logger.getLogger(ConcurrentCacheTest.class.toString());

// choice of which type of tilecache is used (ConcurrentLinkedCache,
// ConcurrentTileCache,
// default(sun implemented) otherwise)
private static int DEFAULT_CACHE_USED = 2;

// diagnostics
private static boolean DEFAULT_DIAGNOSTICS = false;

// choice of the concurrency Level
private static int DEFAULT_CONCURRENCY_LEVEL = 16;

private int maxRequestPerThread;

private CountDownLatch latch;

private Random generator;

private double minTileX;

private double minTileY;

private double maxTileX;

private double maxTileY;

private RenderedImage image_;

public enum Caches {
    SUN_TILE_CACHE(0, "SunTileCache"), CONCURRENT_TILE_CACHE(1,
            "ConcurrentTileCache"), CONCURRENT_LINKED_CACHE(2,
            "ConcurrentLinkedCache");

    private final int tileCache;

    private final String tileCacheString;

    Caches(int value, String s) {
        this.tileCache = value;
        this.tileCacheString = s;
    }

    public int valueCache() {
        return tileCache;
    }

    public String cacheName() {
        return tileCacheString;
    }

    public static String cacheString(int i) {
        Caches[] values = Caches.values();
        return values[i].cacheName();
    }

};

/*
 * This test-class compares the functionality of the default Sun tilecache or
 * the new implementation of the Concurrent tilecache. You should set the
 * boolean "concurrentOrDefault" to true or false for change the cache used. You
 * can set the memory cache capacity.For the Concurrent tile cache you also can
 * change the concurrency level. The test performs up to 400 request with an
 * increased number of threads from 1 to 64 and calculate the throughput after
 * running the test 10 times. For every run the test execute 1000 requests for
 * the hotspot code compilation. When you run the test it throws a
 * ClassNotFoundException because it doesn't find the medialib accelerator but
 * then continue working in pure java mode.
 */

// @Test
public double[] testwriteImageAndWatchFlag(int cacheUsed, int concurrencyLevel,
        long memoryCacheCapacity, RenderedImage img, String path,
        boolean diagnostics) throws IOException, InterruptedException {
    // sets the cache and is concurrency level and diagnostics if needed

    switch (cacheUsed) {
    case 0:
        SunTileCache sunCache = new SunTileCache();
        if (diagnostics) {
            sunCache.enableDiagnostics();
        }
        JAI.getDefaultInstance().setTileCache(sunCache);
        break;
    case 1:
        ConcurrentTileCache cTileCache = new ConcurrentTileCache();
        cTileCache.setConcurrencyLevel(concurrencyLevel);
        if (diagnostics) {
            cTileCache.enableDiagnostics();
        }
        JAI.getDefaultInstance().setTileCache(cTileCache);
        break;
    case 2:
        ConcurrentLinkedCache cLinkedCache = new ConcurrentLinkedCache();
        cLinkedCache.setConcurrencyLevel(concurrencyLevel);
        if (diagnostics) {
            cLinkedCache.enableDiagnostics();
        }
        JAI.getDefaultInstance().setTileCache(cLinkedCache);
        break;
    }
    JAI.getDefaultInstance().getTileCache()
            .setMemoryCapacity(memoryCacheCapacity);
    JAI.getDefaultInstance().getTileScheduler().setParallelism(10);

    int threadMaxNumber = (int) Math.pow(2, EXPONENT);
    // throughput array for storing data
    double[] througputArray = new double[EXPONENT + 1];

    // Creation of the thread pool executor
    ThreadPoolExecutor pool = new ThreadPoolExecutor(threadMaxNumber,
            threadMaxNumber, 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(1000000));

    // ExecutorService pool = Executors.newFixedThreadPool(threadMaxNumber);

    // ExecutorService pool = Executors.newCachedThreadPool();

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
            reader = (TIFFImageReader) new TIFFImageReaderSpi()
                    .createReaderInstance();
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
        image = ScaleDescriptor.create(image, Float.valueOf(2.0f),
                Float.valueOf(2.0f), Float.valueOf(0.0f), Float.valueOf(0.0f),
                new InterpolationNearest(), null);

        image_ = ScaleDescriptor.create(image, Float.valueOf(3.5f),
                Float.valueOf(3.5f), Float.valueOf(0.0f), Float.valueOf(0.0f),
                new InterpolationNearest(), null);

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
                    maxRequestPerThread = STARTING_REQUEST_PER_FIRST_THREAD;
                    Worker firstThread = new Worker();
                    pool.execute(firstThread);
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
                final Worker prefetch = new Worker();
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
         * All the readers, writers, and stream are closed even if the program
         * throws an exception
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
    int cacheUsed = DEFAULT_CACHE_USED;
    // sets the concurrency level
    int concurrencyLevel = DEFAULT_CONCURRENCY_LEVEL;
    // sets the memory cache capacity
    long memoryCacheCapacity = DEFAULT_MEMORY_CAPACITY;
    // choice of using a synthetic or a real image
    boolean syntheticImage = false;
    // diagnostics
    boolean diagnosticEnabled = DEFAULT_DIAGNOSTICS;
    // loaded data
    RenderedImage imageSynth = null;
    String path = null;

    if (args.length > 0) {
        cacheUsed = Integer.parseInt(args[0]);

        if (cacheUsed > 0) {
            concurrencyLevel = Integer.parseInt(args[1]);
            memoryCacheCapacity = Long.parseLong(args[2]);
            syntheticImage = Boolean.parseBoolean(args[3]);
            diagnosticEnabled = Boolean.parseBoolean(args[4]);
            if (syntheticImage) {
                imageSynth = getSynthetic(1);
            } else {
                path = args[5];
            }
        } else {
            memoryCacheCapacity = Long.parseLong(args[1]);
            syntheticImage = Boolean.parseBoolean(args[2]);
            diagnosticEnabled = Boolean.parseBoolean(args[3]);
            if (syntheticImage) {
                imageSynth = getSynthetic(1);
            } else {
                path = args[4];
            }
        }
    } else {
        imageSynth = getSynthetic(1);
    }
    // setting the logger
    LOGGER.setLevel(Level.FINE);

    FileHandler fileTxt = new FileHandler(
            "src/test/resources/it/geosolutions/logfiles/ConcurrentCacheTestLog.txt");
    LOGGER.addHandler(fileTxt);

    // number of tests to do
    int numTest = 10;
    // array for storing the different throughput
    double[][] dataTest = new double[numTest][EXPONENT + 1];
    for (int f = 0; f < numTest; f++) {
        LOGGER.log(Level.INFO, "Test N°." + (f + 1));
        dataTest[f] = new ConcurrentCacheTest().testwriteImageAndWatchFlag(
                cacheUsed, concurrencyLevel, memoryCacheCapacity, imageSynth,
                path, diagnosticEnabled);

    }

    // showing the result
    String stringConcurrent = " with " + Caches.cacheString(cacheUsed);
    if (cacheUsed > 0) {
        stringConcurrent += " and " + concurrencyLevel + " segments";
    }
    for (int f = 0; f < numTest; f++) {
        for (int g = 0; g < dataTest[f].length; g++) {
            LOGGER.log(Level.INFO, "Throughput in test " + (f + 1) + " for "
                    + (int) (Math.pow(2, g)) + " threads is "
                    + (int) dataTest[f][g] + " requests/second"
                    + stringConcurrent);

        }
    }

}

private class Worker implements Runnable {

// @Override
public void run() {
    int i = 0;
    while (i < maxRequestPerThread) {
        double dataX = generator.nextDouble();
        double dataY = generator.nextDouble();
        final int tilex = (int) (dataX * (maxTileX - minTileX) + minTileX);
        final int tiley = (int) (dataY * (maxTileY - minTileY) + minTileY);
        image_.getTile(tilex, tiley);
        i++;
    }

    latch.countDown();

}

}

// simple method for creating a synthetic grayscale image
public static RenderedImage getSynthetic(final double maximum) {
    final float width = 10000;
    final float height = 10000;
    ParameterBlock pb = new ParameterBlock();
    pb.add(width);
    pb.add(height);
    pb.add(new Integer[] { 1 });
    // Create the constant operation.
    return JAI.create("constant", pb);

}

}
