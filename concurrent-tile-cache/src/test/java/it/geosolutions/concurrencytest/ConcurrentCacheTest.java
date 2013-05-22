package it.geosolutions.concurrencytest;

import it.geosolutions.concurrent.ConcurrentTileCache;
import java.awt.image.RenderedImage;
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
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ScaleDescriptor;
import org.junit.Test;
import com.sun.media.imageio.plugins.tiff.TIFFImageReadParam;
import com.sun.media.imageioimpl.plugins.tiff.TIFFImageReader;
import com.sun.media.imageioimpl.plugins.tiff.TIFFImageReaderSpi;

public class ConcurrentCacheTest {

private static final long DEFAULT_MEMORY_CAPACITY = 128 * 1024 * 1024;

private static final int DEFAULT_MAX_REQUEST_PER_THREAD = 100;

private static final int STARTING_REQUEST_PER_FIRST_THREAD = 1000;

private static final int EXPONENT = 6;

private final static Logger LOGGER = Logger.getLogger(ConcurrentCacheTest.class
        .toString());

// choice of which type of tilecache is used (ConcurrentTileCache if true,
// default otherwise)
private static boolean DEFAULT_CONCURRENT_ENABLE = true;

// choice of the concurrency Level
private static int DEFAULT_CONCURRENCY_LEVEL = 4;

private int maxRequestPerThread;

private CountDownLatch latch;

private Random generator;

private double minTileX;

private double minTileY;

// private double intervalX;

// private double intervalY;

private double maxTileX;

private double maxTileY;

private RenderedOp image_;

/*
 * This test-class compares the functionality of the default Sun tilecache or
 * the new implementation of the Concurrent tilecache. You should set the
 * boolean "concurrentOrDefault" to true or false for change the cache used. You
 * can set the memory cache capacity.For the Concurrent tile cache you also can
 * change the concurrency level. The test performs up to 400 request with an
 * increased number of threads from 1 to 64 and calculate the throughput after
 * running the test 10 times. For every run the test execute 1000 requests for
 * the hotspot code compilation
 */

//@Test
public double[] testwriteImageAndWatchFlag(boolean concurrentEnabled,
        int concurrencyLevel, long memoryCacheCapacity) throws IOException,
        InterruptedException {
    if (concurrentEnabled) {
        ConcurrentTileCache newCache = new ConcurrentTileCache();
        newCache.setConcurrencyLevel(concurrencyLevel);
        JAI.getDefaultInstance().setTileCache(newCache);

    }
    JAI.getDefaultInstance().getTileCache()
            .setMemoryCapacity(memoryCacheCapacity);
    JAI.getDefaultInstance().getTileScheduler().setParallelism(10);

    int threadMaxNumber = (int) Math.pow(2, EXPONENT);
    // throughput array for storing data
    double[] througputArray = new double[EXPONENT + 1];

    // Creation of the input file to read and the output file to write
    final File inputFile = new File(
            "src/test/resources/it/geosolutions/concurrent/test-data/write.tif");
    // Creation of the thread pool executor
    ThreadPoolExecutor pool = new ThreadPoolExecutor(threadMaxNumber,
            threadMaxNumber, 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(1000000));

    // ExecutorService pool = Executors.newFixedThreadPool(threadMaxNumber);

    // ExecutorService pool = Executors.newCachedThreadPool();

    generator = new Random();
    // Thoughput array index
    int index = 0;
    // Instantiation of the read-params
    final TIFFImageReadParam param = new TIFFImageReadParam();
    // Instantiation of the file-reader
    TIFFImageReader reader = (TIFFImageReader) new TIFFImageReaderSpi()
            .createReaderInstance();
    // Instantiation of the imageinputstream and imageoutputstrem
    FileImageInputStream stream_in = new FileImageInputStream(inputFile);
    try {
        // Setting the inputstream to the reader
        reader.setInput(stream_in);
        // Creation of a Rendered image to store the image
        RenderedImage image = reader.readAsRenderedImage(0, param);
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
        maxTileX = image_.getMaxTileX();
        maxTileY = image_.getMaxTileY();

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
    boolean concurrentOrDefault;
    int concurrencyLevel;
    long memoryCacheCapacity;

    if (args.length > 0) {
        concurrentOrDefault = Boolean.parseBoolean(args[0]);
        concurrencyLevel = Integer.parseInt(args[1]);
        memoryCacheCapacity = Long.parseLong(args[2]);

    } else {
        concurrentOrDefault = DEFAULT_CONCURRENT_ENABLE;
        concurrencyLevel = DEFAULT_CONCURRENCY_LEVEL;
        memoryCacheCapacity = DEFAULT_MEMORY_CAPACITY;

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
                concurrentOrDefault, concurrencyLevel, memoryCacheCapacity);

    }
    // showing the result
    String stringConcurrent = new String();
    if (concurrentOrDefault) {
        stringConcurrent = " with ConcurrentTileCache and " + concurrencyLevel
                + " segments";
    } else {
        stringConcurrent = " with SunTileCache";
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

@Override
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

}
