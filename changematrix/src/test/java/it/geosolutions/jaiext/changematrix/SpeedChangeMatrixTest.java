package it.geosolutions.jaiext.changematrix;

import it.geosolutions.jaiext.changematrix.ChangeMatrixDescriptor.ChangeMatrix;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.io.File;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.Histogram;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.TileCache;
import org.junit.BeforeClass;
import org.junit.Test;
import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;

/**
 * This test-class is used for testing images in the directory
 * src/test/resources/it/geosolutions/jaiext/changematrix/test-data-expanded. If
 * the data are not present, the test are not performed and a WARNING is shown,
 * or the user can change the source image directory to test-data. After the
 * addition of the images, the user can change some parameters:
 * DEFAULT_THREAD_NUMBER,which indicates the number of thread retrieving the
 * result image-tiles; DEFAULT_TILE_HEIGHT, the height of every tile of the 2
 * initial images; DEFAULT_TILE_WIDTH, the width of every tile of the 2 initial
 * images; NUM_CYCLES_BENCH, the number of the benchmark cycles for every test;
 * NUM_CYCLES_WARP, the number of the initial cycles for every test that are not
 * considered inside the statistics. The initial method startUp is used for reading
 * the 2 images, expanding the tileCache, fill the tile cache with the tile of both
 * images and calculation of the Histogram of the images for the changeMatrix. The first
 * test calculates the changematrix from the source and reference images and only the result
 * image tiles are removed from the cache. The second test simply adds the tiles of the 2 
 * images to the cache and then remove them from it, continuously.
 */
public class SpeedChangeMatrixTest extends AbstractBenchmark {

	private final static String REFERENCE_PATH_FOR_TESTS = "./src/test/resources/it/geosolutions/jaiext/changematrix/test-data-expanded/";

	private final static int DEFAULT_THREAD_NUMBER = 10;

	private final static int DEFAULT_TILE_HEIGHT = 512;

	private final static int DEFAULT_TILE_WIDTH = 512;

	private final static SetupClass initializationSetup = new SetupClass();

	static boolean file0Present;

	static boolean file6Present;

	private Logger logger = Logger.getLogger(SpeedChangeMatrixTest.class
			.getName());

	// The first NUM_CYCLES_WARP cycles are not considered because they simply
	// allows the
	// Java Hotspot to compile the code. Then the other NUM_CYCLES_BENCH cycles
	// are calculated
	private static final int NUM_CYCLES_BENCH = 3;
	private static final int NUM_CYCLES_WARP = 1;

	@BeforeClass
	public static void startUp() {
		// Source and reference images acquisition
		final File file0 = new File(REFERENCE_PATH_FOR_TESTS,
				"clc2000_L3_100m.tif");
		final File file6 = new File(REFERENCE_PATH_FOR_TESTS,
				"clc2006_L3_100m.tif");

		file0Present = file0.isFile();
		file6Present = file6.isFile();

		if (file0Present && file6Present) {
			int tileH = DEFAULT_TILE_HEIGHT;
			int tileW = DEFAULT_TILE_WIDTH;

			// Tile dimension settings
			ImageLayout layout = new ImageLayout();
			layout.setTileHeight(tileH).setTileWidth(tileW);
			RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT,
					layout);

			// TileCache used by JAI
			TileCache defaultTileCache = JAI.getDefaultInstance()
					.getTileCache();

			// Setting of the tile cache memory capacity
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

			// ParameterBlock creation
			final ParameterBlockJAI pbjHist = new ParameterBlockJAI("Histogram");
			pbjHist.addSource(source);

			RenderedOp histogramIMG = JAI.create("Histogram", pbjHist, hints);
			Histogram histogram = (Histogram) histogramIMG
					.getProperty("Histogram");

			final ParameterBlockJAI pbjHistRef = new ParameterBlockJAI(
					"Histogram");
			pbjHistRef.addSource(reference);

			RenderedOp histogramIMGRef = JAI
					.create("Histogram", pbjHist, hints);
			Histogram histogramRef = (Histogram) histogramIMGRef
					.getProperty("Histogram");

			initializationSetup.setRenderingHints(hints);
			initializationSetup.setSource(source);
			initializationSetup.setReference(reference);

			// Histogram storage
			initializationSetup.setHistSource(histogram);
			initializationSetup.setHistRef(histogramRef);
		}
	}

	@Test
	@BenchmarkOptions(benchmarkRounds = NUM_CYCLES_BENCH, warmupRounds = NUM_CYCLES_WARP)
	public void testImageDefaultParametersNoROI() throws InterruptedException {

		if (file0Present && file6Present) {
			RenderedOp reference = (RenderedOp) initializationSetup
					.getReference();
			RenderedOp source = (RenderedOp) initializationSetup.getSource();

			RenderingHints hints = initializationSetup.getRenderingHints();

			Histogram histogram = initializationSetup.getHistSource();
			Histogram histogramRef = initializationSetup.getHistRef();

			ExecutorService ex = Executors
					.newFixedThreadPool(DEFAULT_THREAD_NUMBER);

			// ChangeMatrix creation
			final Set<Integer> classes = new HashSet<Integer>();
			// Setting of the classes

			int[] bins = histogram.getBins(0);
			int[] binsRef = histogramRef.getBins(0);

			// Setting of all the source classes
			for (int i = 0; i < bins.length; i++) {
				classes.add(bins[i]);
			}

			// Setting of all the reference classes
			for (int i = 0; i < binsRef.length; i++) {
				classes.add(bins[i]);
			}

			final ChangeMatrix cm = new ChangeMatrix(classes);

			// ParameterBlock creation
			final ParameterBlockJAI pbj = new ParameterBlockJAI("ChangeMatrix");
			pbj.addSource(reference);
			pbj.addSource(source);
			pbj.setParameter("result", cm);
			final RenderedOp result = JAI.create("ChangeMatrix", pbj, hints);
			result.getWidth();

			// force computation
			final Queue<Point> tiles = new ArrayBlockingQueue<Point>(
					result.getNumXTiles() * result.getNumYTiles());
			for (int i = 0; i < result.getNumXTiles(); i++) {
				for (int j = 0; j < result.getNumYTiles(); j++) {
					tiles.add(new Point(i, j));
				}
			}
			// Cycle for calculating all the changeMatrix statistics
			final CountDownLatch sem = new CountDownLatch(result.getNumXTiles()
					* result.getNumYTiles());
			for (final Point tile : tiles) {
				ex.execute(new Runnable() {

					@Override
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

			System.out.println("test");

		}
	}

	@Test
	@BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
	public void testImageTileCacheUse() throws InterruptedException {

		if (file0Present && file6Present) {

			RenderedOp reference = (RenderedOp) initializationSetup
					.getReference();
			RenderedOp source = (RenderedOp) initializationSetup.getSource();
			// cache flushing
			JAI.getDefaultInstance().getTileCache().flush();

			int numCyclesTot = NUM_CYCLES_BENCH + NUM_CYCLES_WARP;
			long mean = 0;
			long max = 0;
			long min = 0;

			for (int i = 0; i < numCyclesTot; i++) {

				long start = System.nanoTime();
				reference.getTiles();
				source.getTiles();

				// cache flushing
				JAI.getDefaultInstance().getTileCache().flush();

				long end = System.nanoTime();

				// Time calculation
				if (i > NUM_CYCLES_WARP) {
					long time = (end - start);
					if (i == NUM_CYCLES_WARP + 1) {
						max = time;
						min = time;
					}

					if (max < time) {
						max = time;
					}

					if (min > time) {
						min = time;
					}

					mean = mean + time;
					long meanValue = (long) ((mean / (i - NUM_CYCLES_WARP)) / 1E6);

					System.out.println("mean = " + meanValue);
					System.out.println("max = " + max / 1E6);
					System.out.println("min = " + min / 1E6);
				}

				System.out.println("test cache");
			}
		} else {
			logger.log(Level.WARNING,
					"Images are not present, computation not performed");
		}
	}

	
	
	
	
	/** 
	 * This class is a JavaBean used for storing the image information and then passed them to the 2 test-method
	 * The stored information are: the source and reference image, the renderingHints related to the 2 images and 
	 * the histograms of the 2 images.
	 */
	private static class SetupClass {

		private RenderingHints hints;

		private RenderedImage source;

		private RenderedImage reference;

		private Histogram histSource;

		private Histogram histRef;

		public Histogram getHistSource() {
			return histSource;
		}

		public void setHistSource(Histogram histSource) {
			this.histSource = histSource;
		}

		public Histogram getHistRef() {
			return histRef;
		}

		public void setHistRef(Histogram histRef) {
			this.histRef = histRef;
		}

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
