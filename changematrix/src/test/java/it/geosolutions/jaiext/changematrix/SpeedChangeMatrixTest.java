package it.geosolutions.jaiext.changematrix;

import it.geosolutions.jaiext.changematrix.ChangeMatrixDescriptor.ChangeMatrix;

import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.TileCache;

import org.geotools.test.TestData;
import org.junit.BeforeClass;
import org.junit.Test;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;

/**
 * This test-class is used for testing images in the directory defined by the variable "REFERENCE_PATH_FOR_TESTS" . If the data are not present, a
 * WARNING is shown and the source image directory is set to src/test/resources/it/geosolutions/jaiext/changematrix/test-data. After the addition of
 * the images, the user can change some parameters: DEFAULT_THREAD_NUMBER,which indicates the number of thread retrieving the result image-tiles;
 * DEFAULT_TILE_HEIGHT, the height of every tile of the 2 initial images; DEFAULT_TILE_WIDTH, the width of every tile of the 2 initial images;
 * NUM_CYCLES_BENCH, the number of the benchmark cycles for every test; NUM_CYCLES_WARM, the number of the initial cycles for every test that are not
 * considered inside the statistics. The initial method startUp is used for reading the 2 images, expanding the tileCache, fill the tile cache with
 * the tile of both images and calculation of the Histogram of the images for the changeMatrix. The test calculates the changematrix from the source
 * and reference images and only the result image tiles are removed from the cache.
 */
public class SpeedChangeMatrixTest extends AbstractBenchmark {

    private static final int MAX_CLASS = 45;

    // private final static String REFERENCE_PATH_FOR_TESTS = "d:/data/unina";
    private final static String REFERENCE_PATH_FOR_TESTS = "/home/giuliano/work/Projects/LIFE_Project/LUC_gpgpu/rasterize";

    private final static int DEFAULT_THREAD_NUMBER = 10;

    private final static int DEFAULT_TILE_HEIGHT = 512;

    private final static int DEFAULT_TILE_WIDTH = 512;

    private final static SetupClass initializationSetup = new SetupClass();

    // The first NUM_CYCLES_WARM cycles are not considered because they simply
    // allows the
    // Java Hotspot to compile the code. Then the other NUM_CYCLES_BENCH cycles
    // are calculated
    private static final int NUM_CYCLES_BENCH = 50;

    private static final int NUM_CYCLES_WARM = 20;

    @BeforeClass
    public static void startUp() {
        // Source and reference images acquisition

        File file0 = new File(REFERENCE_PATH_FOR_TESTS, "clc2000_L3_100m.tif");
        File file6 = new File(REFERENCE_PATH_FOR_TESTS, "clc2006_L3_100m.tif");

        if (!file0.exists() || !file0.canRead() || !file6.exists() || !file6.canRead()) {
            System.err
                    .println("WARNING : Input files in ReferencedPath not present, test images are taken from TEST-DATA directory");

            try {
                file0 = TestData.file(SpeedChangeMatrixTest.class, "clc2000_L3_100m_small.tif");
                file6 = TestData.file(SpeedChangeMatrixTest.class, "clc2006_L3_100m_small.tif");
            } catch (FileNotFoundException f) {
                throw new IllegalArgumentException("Input files are not present!");
            } catch (IOException f) {
                throw new IllegalArgumentException("Input files are not present!");
            }

            if (!file0.exists() || !file0.canRead() || !file6.exists() || !file6.canRead()) {
                throw new IllegalArgumentException("Input files are not present!");
            }

        }

        int tileH = DEFAULT_TILE_HEIGHT;
        int tileW = DEFAULT_TILE_WIDTH;

        // Tile dimension settings
        ImageLayout layout = new ImageLayout();
        layout.setTileHeight(tileH).setTileWidth(tileW);
        RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);

        // TileCache used by JAI
        TileCache defaultTileCache = JAI.getDefaultInstance().getTileCache();

        // Setting of the tile cache memory capacity 800M
        defaultTileCache.setMemoryCapacity(800 * 1024 * 1024);
        defaultTileCache.setMemoryThreshold(1.0f);
        JAI.getDefaultInstance().setTileCache(defaultTileCache);
        // new TCTool((SunTileCache)defaultTileCache);

        // Forcing the cache to store the source and reference tiles

        final ParameterBlockJAI pbj = new ParameterBlockJAI("ImageRead");
        pbj.setParameter("Input", file0);

        RenderedOp source = JAI.create("ImageRead", pbj, hints);

        pbj.setParameter("Input", file6);
        RenderedOp reference = JAI.create("ImageRead", pbj, hints);

        // Cache tile filling
        source.getTiles();
        reference.getTiles();

        initializationSetup.setRenderingHints(hints);
        initializationSetup.setSource(source);
        initializationSetup.setReference(reference);

    }

    @Test
    @BenchmarkOptions(benchmarkRounds = NUM_CYCLES_BENCH, warmupRounds = NUM_CYCLES_WARM)
    public void testImageDefault() throws InterruptedException {

        RenderedOp reference = (RenderedOp) initializationSetup.getReference();
        RenderedOp source = (RenderedOp) initializationSetup.getSource();

        RenderingHints hints = initializationSetup.getRenderingHints();

        // executor service for scheduling the computation threads.
        ExecutorService ex = Executors.newFixedThreadPool(DEFAULT_THREAD_NUMBER);

        // ChangeMatrix creation
        final Set<Integer> classes = new HashSet<Integer>();
        for (int i = 0; i < MAX_CLASS; i++) {
            classes.add(i);
        }

        final ChangeMatrix cm = new ChangeMatrix(classes);

        // ParameterBlock creation
        final ParameterBlockJAI pbj = new ParameterBlockJAI("ChangeMatrix");
        pbj.addSource(reference);
        pbj.addSource(source);
        pbj.setParameter("result", cm);
        // Setting of the pixel multiplier
        pbj.setParameter("pixelMultiplier", MAX_CLASS + 1);
        final RenderedOp result = JAI.create("ChangeMatrix", pbj, hints);
        result.getWidth();

        // force computation
        final Queue<Point> tiles = new ArrayBlockingQueue<Point>(result.getNumXTiles()
                * result.getNumYTiles());
        for (int i = 0; i < result.getNumXTiles(); i++) {
            for (int j = 0; j < result.getNumYTiles(); j++) {
                tiles.add(new Point(i, j));
            }
        }
        // Cycle for calculating all the changeMatrix statistics
        final CountDownLatch sem = new CountDownLatch(result.getNumXTiles() * result.getNumYTiles());
        for (final Point tile : tiles) {
            ex.execute(new Runnable() {

                public void run() {
                    result.getTile(tile.x, tile.y);
                    sem.countDown();
                }
            });
        }
        sem.await();
        cm.freeze(); // stop changing the computations! If we did not do
                     // this new
                     // values would be accumulated as the file was
                     // written

        // cache flushing
        result.dispose();
    }

    /**
     * This class is a JavaBean used for storing the image information and then passed them to the 2 test-method The stored information are: the
     * source and reference image, the renderingHints related to the 2 images.
     */
    private static class SetupClass {

        private RenderingHints hints;

        private RenderedImage source;

        private RenderedImage reference;

        public RenderingHints getRenderingHints() {
            return hints;
        }

        public void setRenderingHints(RenderingHints hints) {
            this.hints = hints;
        }

        public RenderedImage getSource() {
            return source;
        }

        public void setSource(RenderedImage source) {
            this.source = source;
        }

        public RenderedImage getReference() {
            return reference;
        }

        public void setReference(RenderedImage reference) {
            this.reference = reference;
        }

        SetupClass() {
        }
    }

}
