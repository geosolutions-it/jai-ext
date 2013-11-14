package it.geosolutions.jaiext.changematrix;

import it.geosolutions.jaiext.changematrix.ChangeMatrixDescriptor.ChangeMatrix;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
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

import javax.imageio.ImageIO;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConstantDescriptor;

import org.geotools.test.TestData;
import org.junit.BeforeClass;
import org.junit.Test;

public class ChangeMatrixTest extends org.junit.Assert {

    private static final int FIRST_CLASS_VALUE = 0;

    private static final int SECOND_CLASS_VALUE = 1;

    private static final int THIRD_CLASS_VALUE = 35;

    private static final int FOURTH_CLASS_VALUE = 36;

    private static final int FIFTH_CLASS_VALUE = 37;

    private static int pixelMultiplier;

    private static int pixelMultiplierBiggerThanShort;

    @BeforeClass
    public static void initialSetup() {

        pixelMultiplier = 100;

        pixelMultiplierBiggerThanShort = Short.MAX_VALUE;

    }

    /**
     * No exceptions if the SPI is properly registered
     */
    @Test
    public void testSPI() {
        new ParameterBlockJAI("ChangeMatrix");

    }

    @Test
    public void testMultipleBands() {
        final Set<Integer> classes = new HashSet<Integer>();
        classes.add(FIRST_CLASS_VALUE);
        classes.add(SECOND_CLASS_VALUE);
        classes.add(THIRD_CLASS_VALUE);
        classes.add(FOURTH_CLASS_VALUE);
        classes.add(FIFTH_CLASS_VALUE);
        final ChangeMatrix cm = new ChangeMatrix(classes);

        BufferedImage reference = new BufferedImage(800, 600, BufferedImage.TYPE_3BYTE_BGR);
        RenderedOp now = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Byte[] { Byte.valueOf((byte) 0) }, null);
        final ParameterBlockJAI pbj = new ParameterBlockJAI("ChangeMatrix");
        pbj.addSource(reference);
        pbj.addSource(now);
        pbj.setParameter("result", cm);
        pbj.setParameter("pixelMultiplier", pixelMultiplier);
        final RenderedOp result = JAI.create("ChangeMatrix", pbj, null);
        try {
            result.getWidth();
            assertTrue("we should have got an exception as the image types have multiple bands!",
                    false);
        } catch (Exception e) {
            // fine we get an exception
        }
    }

    @Test
    public void testDifferentTypes() {
        final Set<Integer> classes = new HashSet<Integer>();
        classes.add(FIRST_CLASS_VALUE);
        classes.add(SECOND_CLASS_VALUE);
        classes.add(THIRD_CLASS_VALUE);
        classes.add(FOURTH_CLASS_VALUE);
        classes.add(FIFTH_CLASS_VALUE);
        final ChangeMatrix cm = new ChangeMatrix(classes);

        RenderedOp reference = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Integer[] { Integer.valueOf(1) }, null);
        RenderedOp now = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Byte[] { Byte.valueOf((byte) 0) }, null);
        final ParameterBlockJAI pbj = new ParameterBlockJAI("ChangeMatrix");
        pbj.addSource(reference);
        pbj.addSource(now);
        pbj.setParameter("result", cm);
        pbj.setParameter("pixelMultiplier", pixelMultiplier);
        final RenderedOp result = JAI.create("ChangeMatrix", pbj, null);
        try {
            result.getWidth();
            assertTrue("we should have got an exception as the image types are different!", false);
        } catch (Exception e) {
            // fine we get an exception
        }
    }

    @Test
    public void testDifferentDimensions() {
        final Set<Integer> classes = new HashSet<Integer>();
        classes.add(FIRST_CLASS_VALUE);
        classes.add(SECOND_CLASS_VALUE);
        classes.add(THIRD_CLASS_VALUE);
        classes.add(FOURTH_CLASS_VALUE);
        classes.add(FIFTH_CLASS_VALUE);
        final ChangeMatrix cm = new ChangeMatrix(classes);

        RenderedOp reference = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Integer[] { Integer.valueOf(1) }, null);
        RenderedOp now = ConstantDescriptor.create(Float.valueOf(700), Float.valueOf(600),
                new Integer[] { Integer.valueOf(0) }, null);
        final ParameterBlockJAI pbj = new ParameterBlockJAI("ChangeMatrix");
        pbj.addSource(reference);
        pbj.addSource(now);
        pbj.setParameter("result", cm);
        pbj.setParameter("pixelMultiplier", pixelMultiplier);
        final RenderedOp result = JAI.create("ChangeMatrix", pbj, null);
        try {
            result.getWidth();
            assertTrue("we should have got an eception as the image types are different!", false);
        } catch (Exception e) {
            // fine we get an exception
        }
    }

    @Test
    public void testFloatTypes() {
        final Set<Integer> classes = new HashSet<Integer>();
        classes.add(FIRST_CLASS_VALUE);
        classes.add(SECOND_CLASS_VALUE);
        classes.add(THIRD_CLASS_VALUE);
        classes.add(FOURTH_CLASS_VALUE);
        classes.add(FIFTH_CLASS_VALUE);
        final ChangeMatrix cm = new ChangeMatrix(classes);

        RenderedOp reference = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Float[] { Float.valueOf(1.0f) }, null);
        RenderedOp now = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Float[] { Float.valueOf(0.0f) }, null);
        final ParameterBlockJAI pbj = new ParameterBlockJAI("ChangeMatrix");
        pbj.addSource(reference);
        pbj.addSource(now);
        pbj.setParameter("result", cm);
        pbj.setParameter("pixelMultiplier", pixelMultiplier);
        final RenderedOp result = JAI.create("ChangeMatrix", pbj, null);
        try {
            result.getWidth();
            assertTrue("we should have got an eception as the image types are Float!", false);
        } catch (Exception e) {
            // fine we get an exception
        }
    }

    @Test
    public void testDoubleTypes() {
        final Set<Integer> classes = new HashSet<Integer>();
        classes.add(FIRST_CLASS_VALUE);
        classes.add(SECOND_CLASS_VALUE);
        classes.add(THIRD_CLASS_VALUE);
        classes.add(FOURTH_CLASS_VALUE);
        classes.add(FIFTH_CLASS_VALUE);
        final ChangeMatrix cm = new ChangeMatrix(classes);

        RenderedOp reference = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Double[] { Double.valueOf(1.0) }, null);
        RenderedOp now = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Double[] { Double.valueOf(0.0) }, null);
        final ParameterBlockJAI pbj = new ParameterBlockJAI("ChangeMatrix");
        pbj.addSource(reference);
        pbj.addSource(now);
        pbj.setParameter("result", cm);
        pbj.setParameter("pixelMultiplier", pixelMultiplier);
        final RenderedOp result = JAI.create("ChangeMatrix", pbj, null);
        try {
            result.getWidth();
            assertTrue("we should have got an eception as the image types are Double!", false);
        } catch (Exception e) {
            // fine we get an exception
        }
    }

    @Test
    public void testWrongPixelMultiplier() {
        final Set<Integer> classes = new HashSet<Integer>();
        classes.add(FIRST_CLASS_VALUE);
        classes.add(SECOND_CLASS_VALUE);
        classes.add(THIRD_CLASS_VALUE);
        classes.add(FOURTH_CLASS_VALUE);
        classes.add(FIFTH_CLASS_VALUE);
        final ChangeMatrix cm = new ChangeMatrix(classes);

        RenderedOp reference = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Integer[] { Integer.valueOf(1) }, null);
        RenderedOp now = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Integer[] { Integer.valueOf(0) }, null);
        final ParameterBlockJAI pbj = new ParameterBlockJAI("ChangeMatrix");
        pbj.addSource(reference);
        pbj.addSource(now);
        pbj.setParameter("result", cm);
        pbj.setParameter("pixelMultiplier", FIRST_CLASS_VALUE);
        final RenderedOp result = JAI.create("ChangeMatrix", pbj, null);
        try {
            result.getTiles();
            assertTrue(
                    "we should have got an eception as the pixelMultiplier is smaller than the value of the greatest class!",
                    false);
        } catch (Exception e) {
            // fine we get an exception
        }
    }

    
    @Test
    public void testNegativePixelMultiplier() {
        final Set<Integer> classes = new HashSet<Integer>();
        classes.add(FIRST_CLASS_VALUE);
        classes.add(SECOND_CLASS_VALUE);
        classes.add(THIRD_CLASS_VALUE);
        classes.add(FOURTH_CLASS_VALUE);
        classes.add(FIFTH_CLASS_VALUE);
        final ChangeMatrix cm = new ChangeMatrix(classes);

        RenderedOp reference = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Integer[] { Integer.valueOf(1) }, null);
        RenderedOp now = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Integer[] { Integer.valueOf(0) }, null);
        final ParameterBlockJAI pbj = new ParameterBlockJAI("ChangeMatrix");
        pbj.addSource(reference);
        pbj.addSource(now);
        pbj.setParameter("result", cm);
        pbj.setParameter("pixelMultiplier", -FIFTH_CLASS_VALUE);
        final RenderedOp result = JAI.create("ChangeMatrix", pbj, null);
        try {
            result.getTiles();
            assertTrue(
                    "we should have got an eception as the pixelMultiplier is negative!",
                    false);
        } catch (Exception e) {
            // fine we get an exception
        }
    }
    
    @Test
    // @Ignore
    public void completeTestByteDatatype() throws Exception {

        final File file0;
        final File file6;
        try {
            file0 = TestData.file(SpeedChangeMatrixTest.class, "clc2000_L3_100m_small.tif");
            file6 = TestData.file(SpeedChangeMatrixTest.class, "clc2006_L3_100m_small.tif");
        } catch (FileNotFoundException f) {
            throw new IllegalArgumentException("Input files are not present!");
        } catch (IOException f) {
            throw new IllegalArgumentException("Input files are not present!");
        }

        final Set<Integer> classes = new HashSet<Integer>();
        classes.add(FIRST_CLASS_VALUE);
        classes.add(SECOND_CLASS_VALUE);
        classes.add(THIRD_CLASS_VALUE);
        classes.add(FOURTH_CLASS_VALUE);
        classes.add(FIFTH_CLASS_VALUE);
        final ChangeMatrix cm = new ChangeMatrix(classes);

        final RenderedOp source = JAI.create("ImageRead", file6);// new
                                                                 // File("d:/data/unina/clc2006_L3_100m.tif"));
        final RenderedOp reference = JAI.create("ImageRead", file0);// new
                                                                    // File("d:/data/unina/clc2000_L3_100m.tif"));

        final ImageLayout layout = new ImageLayout();
        layout.setTileHeight(256).setTileWidth(100);
        final RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);
        final ParameterBlockJAI pbj = new ParameterBlockJAI("ChangeMatrix");
        pbj.addSource(reference);
        pbj.addSource(source);
        pbj.setParameter("result", cm);
        pbj.setParameter("pixelMultiplier", pixelMultiplier);
        final RenderedOp result = JAI.create("ChangeMatrix", pbj, hints);
        result.getWidth();

        final Queue<Point> tiles = new ArrayBlockingQueue<Point>(result.getNumXTiles()
                * result.getNumYTiles());
        for (int i = 0; i < result.getNumXTiles(); i++) {
            for (int j = 0; j < result.getNumYTiles(); j++) {
                tiles.add(new Point(i, j));
            }
        }
        final CountDownLatch sem = new CountDownLatch(result.getNumXTiles() * result.getNumYTiles());
        ExecutorService ex = Executors.newFixedThreadPool(10);
        for (final Point tile : tiles) {
            ex.execute(new Runnable() {

                public void run() {
                    result.getTile(tile.x, tile.y);
                    sem.countDown();
                }
            });
        }
        sem.await();
        cm.freeze(); // stop changing the computations! If we did not do this new
                     // values would be accumulated as the file was written

        // CONTROL ON THE IMAGE PIXEL VALUES

        int minX = result.getMinX();
        int minY = result.getMinY();

        int maxX = result.getMaxX();
        int maxY = result.getMaxY();

        int resultValue = 0;
        int resultExpected = 0;
        int referenceValue = 0;
        int sourceValue = 0;

        Raster referenceTile;
        Raster sourceTile;
        Raster resultTile;

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                // Selection of the tiles associated with this position
                referenceTile = reference.getTile(reference.XToTileX(x), reference.YToTileY(y));
                sourceTile = source.getTile(source.XToTileX(x), source.YToTileY(y));
                resultTile = result.getTile(result.XToTileX(x), result.YToTileY(y));
                // Selection of the value associated with this position
                resultValue = resultTile.getSample(x, y, 0);
                referenceValue = referenceTile.getSample(x, y, 0);
                sourceValue = sourceTile.getSample(x, y, 0);
                // Calculation
                resultExpected = referenceValue + (pixelMultiplier * sourceValue);
                // Test
                assertEquals(resultExpected, resultValue);
            }
        }

        // try to write the resulting image before disposing the sources
        final File out = File.createTempFile("chm", "result.tif");
        out.deleteOnExit();
        ImageIO.write(result, "tiff", out);

        result.dispose();
        source.dispose();
        reference.dispose();

        // check values of the change matrix
        assertEquals(88022, cm.retrievePairOccurrences(0, 0));
        assertEquals(0, cm.retrievePairOccurrences(0, 35));
        assertEquals(0, cm.retrievePairOccurrences(0, 1));
        assertEquals(0, cm.retrievePairOccurrences(0, 36));
        assertEquals(0, cm.retrievePairOccurrences(0, 37));
        assertEquals(0, cm.retrievePairOccurrences(35, 0));
        assertEquals(36, cm.retrievePairOccurrences(35, 35));
        assertEquals(0, cm.retrievePairOccurrences(35, 1));
        assertEquals(0, cm.retrievePairOccurrences(35, 36));
        assertEquals(0, cm.retrievePairOccurrences(35, 37));
        assertEquals(0, cm.retrievePairOccurrences(1, 0));
        assertEquals(0, cm.retrievePairOccurrences(1, 35));
        assertEquals(18, cm.retrievePairOccurrences(1, 1));
        assertEquals(1, cm.retrievePairOccurrences(1, 36));
        assertEquals(0, cm.retrievePairOccurrences(1, 37));
        assertEquals(0, cm.retrievePairOccurrences(36, 0));
        assertEquals(1, cm.retrievePairOccurrences(36, 35));
        assertEquals(1, cm.retrievePairOccurrences(36, 1));
        assertEquals(6930, cm.retrievePairOccurrences(36, 36));
        assertEquals(58, cm.retrievePairOccurrences(36, 37));
        assertEquals(3, cm.retrievePairOccurrences(37, 0));
        assertEquals(1, cm.retrievePairOccurrences(37, 35));
        assertEquals(0, cm.retrievePairOccurrences(37, 1));
        assertEquals(129, cm.retrievePairOccurrences(37, 36));
        assertEquals(1720, cm.retrievePairOccurrences(37, 37));

        // // spit out results
        // for(Integer ref: classes){
        // for(Integer now: classes){
        // System.out.println("["+ref+","+now+"]("+cm.retrievePairOccurrences(ref,
        // now)+")");
        // }
        // }
    }

    @Test
    public void testROI1() throws Exception {
        final File file0;
        final File file6;
        try {
            file0 = TestData.file(SpeedChangeMatrixTest.class, "clc2000_L3_100m_small.tif");
            file6 = TestData.file(SpeedChangeMatrixTest.class, "clc2006_L3_100m_small.tif");
        } catch (FileNotFoundException f) {
            throw new IllegalArgumentException("Input files are not present!");
        } catch (IOException f) {
            throw new IllegalArgumentException("Input files are not present!");
        }

        final Set<Integer> classes = new HashSet<Integer>();
        classes.add(FIRST_CLASS_VALUE);
        classes.add(SECOND_CLASS_VALUE);
        classes.add(THIRD_CLASS_VALUE);
        classes.add(FOURTH_CLASS_VALUE);
        classes.add(FIFTH_CLASS_VALUE);
        final ChangeMatrix cm = new ChangeMatrix(classes);

        final RenderedOp source = JAI.create("ImageRead", file6);// new
                                                                 // File("d:/data/unina/clc2006_L3_100m.tif"));
        final RenderedOp reference = JAI.create("ImageRead", file0);// new
                                                                    // File("d:/data/unina/clc2000_L3_100m.tif"));

        // create roi
        final Rectangle roi = new Rectangle(reference.getBounds());
        roi.setBounds(roi.x, roi.y, roi.width / 2, roi.height / 2);

        final ImageLayout layout = new ImageLayout();
        layout.setTileHeight(512).setTileWidth(512);
        final RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);
        final ParameterBlockJAI pbj = new ParameterBlockJAI("ChangeMatrix");
        pbj.addSource(reference);
        pbj.addSource(source);
        pbj.setParameter("result", cm);
        pbj.setParameter("roi", new ROIShape(roi));
        pbj.setParameter("pixelMultiplier", pixelMultiplier);
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
        final CountDownLatch sem = new CountDownLatch(result.getNumXTiles() * result.getNumYTiles());
        ExecutorService ex = Executors.newFixedThreadPool(10);
        for (final Point tile : tiles) {
            ex.execute(new Runnable() {

                public void run() {
                    result.getTile(tile.x, tile.y);
                    sem.countDown();
                }
            });
        }
        sem.await();
        cm.freeze(); // stop changing the computations! If we did not do this new
                     // values would be accumulated as the file was written

        // try to write the resulting image before disposing the sources
        final File out = File.createTempFile("chm", "result.tif");
        out.deleteOnExit();
        ImageIO.write(result, "tiff", out);

        result.dispose();
        source.dispose();
        reference.dispose();

        // check values of the change matrix
        assertEquals(14700, cm.retrievePairOccurrences(0, 0));
        assertEquals(0, cm.retrievePairOccurrences(0, 35));
        assertEquals(0, cm.retrievePairOccurrences(0, 1));
        assertEquals(0, cm.retrievePairOccurrences(0, 36));
        assertEquals(0, cm.retrievePairOccurrences(0, 37));
        assertEquals(0, cm.retrievePairOccurrences(35, 0));
        assertEquals(0, cm.retrievePairOccurrences(35, 35));
        assertEquals(0, cm.retrievePairOccurrences(35, 1));
        assertEquals(0, cm.retrievePairOccurrences(35, 36));
        assertEquals(0, cm.retrievePairOccurrences(35, 37));
        assertEquals(0, cm.retrievePairOccurrences(1, 0));
        assertEquals(0, cm.retrievePairOccurrences(1, 35));
        assertEquals(9, cm.retrievePairOccurrences(1, 1));
        assertEquals(1, cm.retrievePairOccurrences(1, 36));
        assertEquals(0, cm.retrievePairOccurrences(1, 37));
        assertEquals(0, cm.retrievePairOccurrences(36, 0));
        assertEquals(0, cm.retrievePairOccurrences(36, 35));
        assertEquals(1, cm.retrievePairOccurrences(36, 1));
        assertEquals(3625, cm.retrievePairOccurrences(36, 36));
        assertEquals(24, cm.retrievePairOccurrences(36, 37));
        assertEquals(0, cm.retrievePairOccurrences(37, 0));
        assertEquals(0, cm.retrievePairOccurrences(37, 35));
        assertEquals(0, cm.retrievePairOccurrences(37, 1));
        assertEquals(47, cm.retrievePairOccurrences(37, 36));
        assertEquals(889, cm.retrievePairOccurrences(37, 37));

    }

    @Test
    public void testROI2() throws Exception {
        final File file0;
        final File file6;
        try {
            file0 = TestData.file(SpeedChangeMatrixTest.class, "clc2000_L3_100m_small.tif");
            file6 = TestData.file(SpeedChangeMatrixTest.class, "clc2006_L3_100m_small.tif");
        } catch (FileNotFoundException f) {
            throw new IllegalArgumentException("Input files are not present!");
        } catch (IOException f) {
            throw new IllegalArgumentException("Input files are not present!");
        }

        final Set<Integer> classes = new HashSet<Integer>();
        classes.add(FIRST_CLASS_VALUE);
        classes.add(SECOND_CLASS_VALUE);
        classes.add(THIRD_CLASS_VALUE);
        classes.add(FOURTH_CLASS_VALUE);
        classes.add(FIFTH_CLASS_VALUE);
        final ChangeMatrix cm = new ChangeMatrix(classes);

        final RenderedOp source = JAI.create("ImageRead", file6);// new
                                                                 // File("d:/data/unina/clc2006_L3_100m.tif"));
        final RenderedOp reference = JAI.create("ImageRead", file0);// new
                                                                    // File("d:/data/unina/clc2000_L3_100m.tif"));

        // create roi
        final Rectangle roi = new Rectangle(reference.getBounds());
        roi.setBounds(roi.width / 2 - roi.width / 4, roi.height / 2 - roi.height / 4,
                roi.width / 4, roi.height / 4);

        final ImageLayout layout = new ImageLayout();
        layout.setTileHeight(512).setTileWidth(512);
        final RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);
        final ParameterBlockJAI pbj = new ParameterBlockJAI("ChangeMatrix");
        pbj.addSource(reference);
        pbj.addSource(source);
        pbj.setParameter("result", cm);
        pbj.setParameter("roi", new ROIShape(roi));
        pbj.setParameter("pixelMultiplier", pixelMultiplier);
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
        final CountDownLatch sem = new CountDownLatch(result.getNumXTiles() * result.getNumYTiles());
        ExecutorService ex = Executors.newFixedThreadPool(10);
        for (final Point tile : tiles) {
            ex.execute(new Runnable() {

                public void run() {
                    result.getTile(tile.x, tile.y);
                    sem.countDown();
                }
            });
        }
        sem.await();
        cm.freeze(); // stop changing the computations! If we did not do this new
                     // values would be accumulated as the file was written

        // try to write the resulting image before disposing the sources
        final File out = File.createTempFile("chm", "result.tif");
        out.deleteOnExit();
        ImageIO.write(result, "tiff", out);

        result.dispose();
        source.dispose();
        reference.dispose();

        // check values of the change matrix
        assertEquals(3180, cm.retrievePairOccurrences(0, 0));
        assertEquals(0, cm.retrievePairOccurrences(0, 35));
        assertEquals(0, cm.retrievePairOccurrences(0, 1));
        assertEquals(0, cm.retrievePairOccurrences(0, 36));
        assertEquals(0, cm.retrievePairOccurrences(0, 37));
        assertEquals(0, cm.retrievePairOccurrences(35, 0));
        assertEquals(0, cm.retrievePairOccurrences(35, 35));
        assertEquals(0, cm.retrievePairOccurrences(35, 1));
        assertEquals(0, cm.retrievePairOccurrences(35, 36));
        assertEquals(0, cm.retrievePairOccurrences(35, 37));
        assertEquals(0, cm.retrievePairOccurrences(1, 0));
        assertEquals(0, cm.retrievePairOccurrences(1, 35));
        assertEquals(2, cm.retrievePairOccurrences(1, 1));
        assertEquals(1, cm.retrievePairOccurrences(1, 36));
        assertEquals(0, cm.retrievePairOccurrences(1, 37));
        assertEquals(0, cm.retrievePairOccurrences(36, 0));
        assertEquals(0, cm.retrievePairOccurrences(36, 35));
        assertEquals(0, cm.retrievePairOccurrences(36, 1));
        assertEquals(1059, cm.retrievePairOccurrences(36, 36));
        assertEquals(6, cm.retrievePairOccurrences(36, 37));
        assertEquals(0, cm.retrievePairOccurrences(37, 0));
        assertEquals(0, cm.retrievePairOccurrences(37, 35));
        assertEquals(0, cm.retrievePairOccurrences(37, 1));
        assertEquals(36, cm.retrievePairOccurrences(37, 36));
        assertEquals(325, cm.retrievePairOccurrences(37, 37));

    }

    @Test
    // @Ignore
    public void completeTestShortDatatype() throws Exception {
        final File file0;
        final File file6;
        try {
            file0 = TestData.file(SpeedChangeMatrixTest.class, "clc2000_L3_100m_small_short.tif");
            file6 = TestData.file(SpeedChangeMatrixTest.class, "clc2006_L3_100m_small_short.tif");
        } catch (FileNotFoundException f) {
            throw new IllegalArgumentException("Input files are not present!");
        } catch (IOException f) {
            throw new IllegalArgumentException("Input files are not present!");
        }

        final Set<Integer> classes = new HashSet<Integer>();
        classes.add(FIRST_CLASS_VALUE);
        classes.add(SECOND_CLASS_VALUE);
        classes.add(THIRD_CLASS_VALUE);
        classes.add(FOURTH_CLASS_VALUE);
        classes.add(FIFTH_CLASS_VALUE);
        final ChangeMatrix cm = new ChangeMatrix(classes);

        final RenderedOp source = JAI.create("ImageRead", file6);// new
        // File("d:/data/unina/clc2006_L3_100m.tif"));
        final RenderedOp reference = JAI.create("ImageRead", file0);// new
        // File("d:/data/unina/clc2000_L3_100m.tif"));

        final ImageLayout layout = new ImageLayout();
        layout.setTileHeight(256).setTileWidth(100);
        final RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);
        final ParameterBlockJAI pbj = new ParameterBlockJAI("ChangeMatrix");
        pbj.addSource(reference);
        pbj.addSource(source);
        pbj.setParameter("result", cm);
        pbj.setParameter("pixelMultiplier", pixelMultiplier);
        final RenderedOp result = JAI.create("ChangeMatrix", pbj, hints);
        result.getWidth();

        final Queue<Point> tiles = new ArrayBlockingQueue<Point>(result.getNumXTiles()
                * result.getNumYTiles());
        for (int i = 0; i < result.getNumXTiles(); i++) {
            for (int j = 0; j < result.getNumYTiles(); j++) {
                tiles.add(new Point(i, j));
            }
        }
        final CountDownLatch sem = new CountDownLatch(result.getNumXTiles() * result.getNumYTiles());
        ExecutorService ex = Executors.newFixedThreadPool(10);
        for (final Point tile : tiles) {
            ex.execute(new Runnable() {

                public void run() {
                    result.getTile(tile.x, tile.y);
                    sem.countDown();
                }
            });
        }
        sem.await();
        cm.freeze(); // stop changing the computations! If we did not do this new
                     // values would be accumulated as the file was written

        // CONTROL ON THE IMAGE PIXEL VALUES

        int minX = result.getMinX();
        int minY = result.getMinY();

        int maxX = result.getMaxX();
        int maxY = result.getMaxY();

        int resultValue = 0;
        int resultExpected = 0;
        int referenceValue = 0;
        int sourceValue = 0;

        Raster referenceTile;
        Raster sourceTile;
        Raster resultTile;

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                // Selection of the tiles associated with this position
                referenceTile = reference.getTile(reference.XToTileX(x), reference.YToTileY(y));
                sourceTile = source.getTile(source.XToTileX(x), source.YToTileY(y));
                resultTile = result.getTile(result.XToTileX(x), result.YToTileY(y));
                // Selection of the value associated with this position
                resultValue = resultTile.getSample(x, y, 0);
                referenceValue = referenceTile.getSample(x, y, 0);
                sourceValue = sourceTile.getSample(x, y, 0);
                // Calculation
                resultExpected = referenceValue + (pixelMultiplier * sourceValue);
                // Test
                assertEquals(resultExpected, resultValue);
            }
        }

        // try to write the resulting image before disposing the sources
        final File out = File.createTempFile("chm", "result.tif");
        out.deleteOnExit();
        ImageIO.write(result, "tiff", out);

        result.dispose();
        source.dispose();
        reference.dispose();

        // check values of the change matrix
        assertEquals(88022, cm.retrievePairOccurrences(0, 0));
        assertEquals(0, cm.retrievePairOccurrences(0, 35));
        assertEquals(0, cm.retrievePairOccurrences(0, 1));
        assertEquals(0, cm.retrievePairOccurrences(0, 36));
        assertEquals(0, cm.retrievePairOccurrences(0, 37));
        assertEquals(0, cm.retrievePairOccurrences(35, 0));
        assertEquals(36, cm.retrievePairOccurrences(35, 35));
        assertEquals(0, cm.retrievePairOccurrences(35, 1));
        assertEquals(0, cm.retrievePairOccurrences(35, 36));
        assertEquals(0, cm.retrievePairOccurrences(35, 37));
        assertEquals(0, cm.retrievePairOccurrences(1, 0));
        assertEquals(0, cm.retrievePairOccurrences(1, 35));
        assertEquals(18, cm.retrievePairOccurrences(1, 1));
        assertEquals(1, cm.retrievePairOccurrences(1, 36));
        assertEquals(0, cm.retrievePairOccurrences(1, 37));
        assertEquals(0, cm.retrievePairOccurrences(36, 0));
        assertEquals(1, cm.retrievePairOccurrences(36, 35));
        assertEquals(1, cm.retrievePairOccurrences(36, 1));
        assertEquals(6930, cm.retrievePairOccurrences(36, 36));
        assertEquals(58, cm.retrievePairOccurrences(36, 37));
        assertEquals(3, cm.retrievePairOccurrences(37, 0));
        assertEquals(1, cm.retrievePairOccurrences(37, 35));
        assertEquals(0, cm.retrievePairOccurrences(37, 1));
        assertEquals(129, cm.retrievePairOccurrences(37, 36));
        assertEquals(1720, cm.retrievePairOccurrences(37, 37));

        // // spit out results
        // for(Integer ref: classes){
        // for(Integer now: classes){
        // System.out.println("["+ref+","+now+"]("+cm.retrievePairOccurrences(ref,
        // now)+")");
        // }
        // }

    }

    @Test
    // @Ignore
    public void completeTestIntDatatype() throws Exception {
        final File file0;
        final File file6;
        try {
            file0 = TestData.file(SpeedChangeMatrixTest.class, "clc2000_L3_100m_smaller_int.tif");
            file6 = TestData.file(SpeedChangeMatrixTest.class, "clc2006_L3_100m_smaller_int.tif");
        } catch (FileNotFoundException f) {
            throw new IllegalArgumentException("Input files are not present!");
        } catch (IOException f) {
            throw new IllegalArgumentException("Input files are not present!");
        }

        final Set<Integer> classes = new HashSet<Integer>();
        classes.add(FIRST_CLASS_VALUE);
        classes.add(SECOND_CLASS_VALUE);
        classes.add(THIRD_CLASS_VALUE);
        classes.add(FOURTH_CLASS_VALUE);
        classes.add(FIFTH_CLASS_VALUE);
        final ChangeMatrix cm = new ChangeMatrix(classes);

        final RenderedOp source = JAI.create("ImageRead", file6);// new
        // File("d:/data/unina/clc2006_L3_100m.tif"));
        final RenderedOp reference = JAI.create("ImageRead", file0);// new
        // File("d:/data/unina/clc2000_L3_100m.tif"));

        final ImageLayout layout = new ImageLayout();
        layout.setTileHeight(256).setTileWidth(100);
        final RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);
        final ParameterBlockJAI pbj = new ParameterBlockJAI("ChangeMatrix");
        pbj.addSource(reference);
        pbj.addSource(source);
        pbj.setParameter("result", cm);
        pbj.setParameter("pixelMultiplier", pixelMultiplier);
        final RenderedOp result = JAI.create("ChangeMatrix", pbj, hints);
        result.getWidth();

        final Queue<Point> tiles = new ArrayBlockingQueue<Point>(result.getNumXTiles()
                * result.getNumYTiles());
        for (int i = 0; i < result.getNumXTiles(); i++) {
            for (int j = 0; j < result.getNumYTiles(); j++) {
                tiles.add(new Point(i, j));
            }
        }
        final CountDownLatch sem = new CountDownLatch(result.getNumXTiles() * result.getNumYTiles());
        ExecutorService ex = Executors.newFixedThreadPool(10);
        for (final Point tile : tiles) {
            ex.execute(new Runnable() {

                public void run() {
                    result.getTile(tile.x, tile.y);
                    sem.countDown();
                }
            });
        }
        sem.await();
        cm.freeze(); // stop changing the computations! If we did not do this new
                     // values would be accumulated as the file was written

        // CONTROL ON THE IMAGE PIXEL VALUES

        int minX = result.getMinX();
        int minY = result.getMinY();

        int maxX = result.getMaxX();
        int maxY = result.getMaxY();

        int resultValue = 0;
        int resultExpected = 0;
        int referenceValue = 0;
        int sourceValue = 0;

        Raster referenceTile;
        Raster sourceTile;
        Raster resultTile;

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                // Selection of the tiles associated with this position
                referenceTile = reference.getTile(reference.XToTileX(x), reference.YToTileY(y));
                sourceTile = source.getTile(source.XToTileX(x), source.YToTileY(y));
                resultTile = result.getTile(result.XToTileX(x), result.YToTileY(y));
                // Selection of the value associated with this position
                resultValue = resultTile.getSample(x, y, 0);
                referenceValue = referenceTile.getSample(x, y, 0);
                sourceValue = sourceTile.getSample(x, y, 0);
                // Calculation
                resultExpected = referenceValue + (pixelMultiplier * sourceValue);
                // Test
                assertEquals(resultExpected, resultValue);
            }
        }

        // try to write the resulting image before disposing the sources
        final File out = File.createTempFile("chm", "result.tif");
        out.deleteOnExit();
        ImageIO.write(result, "tiff", out);

        result.dispose();
        source.dispose();
        reference.dispose();

        // check values of the change matrix
        assertEquals(22021, cm.retrievePairOccurrences(0, 0));
        assertEquals(0, cm.retrievePairOccurrences(0, 35));
        assertEquals(0, cm.retrievePairOccurrences(0, 1));
        assertEquals(0, cm.retrievePairOccurrences(0, 36));
        assertEquals(0, cm.retrievePairOccurrences(0, 37));
        assertEquals(0, cm.retrievePairOccurrences(35, 0));
        assertEquals(8, cm.retrievePairOccurrences(35, 35));
        assertEquals(0, cm.retrievePairOccurrences(35, 1));
        assertEquals(0, cm.retrievePairOccurrences(35, 36));
        assertEquals(0, cm.retrievePairOccurrences(35, 37));
        assertEquals(0, cm.retrievePairOccurrences(1, 0));
        assertEquals(0, cm.retrievePairOccurrences(1, 35));
        assertEquals(5, cm.retrievePairOccurrences(1, 1));
        assertEquals(0, cm.retrievePairOccurrences(1, 36));
        assertEquals(0, cm.retrievePairOccurrences(1, 37));
        assertEquals(0, cm.retrievePairOccurrences(36, 0));
        assertEquals(0, cm.retrievePairOccurrences(36, 35));
        assertEquals(0, cm.retrievePairOccurrences(36, 1));
        assertEquals(1722, cm.retrievePairOccurrences(36, 36));
        assertEquals(11, cm.retrievePairOccurrences(36, 37));
        assertEquals(1, cm.retrievePairOccurrences(37, 0));
        assertEquals(0, cm.retrievePairOccurrences(37, 35));
        assertEquals(0, cm.retrievePairOccurrences(37, 1));
        assertEquals(32, cm.retrievePairOccurrences(37, 36));
        assertEquals(429, cm.retrievePairOccurrences(37, 37));

        // // spit out results
        // for(Integer ref: classes){
        // for(Integer now: classes){
        // System.out.println("["+ref+","+now+"]("+cm.retrievePairOccurrences(ref,
        // now)+")");
        // }
        // }

    }

    @Test
    // @Ignore
    public void completeTestByteToIntDatatype() throws Exception {

        final Set<Integer> classes = new HashSet<Integer>();
        classes.add(FIRST_CLASS_VALUE);
        classes.add(SECOND_CLASS_VALUE);
        classes.add(THIRD_CLASS_VALUE);
        classes.add(FOURTH_CLASS_VALUE);
        classes.add(FIFTH_CLASS_VALUE);
        final ChangeMatrix cm = new ChangeMatrix(classes);

        RenderedOp reference = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Byte[] { Byte.valueOf((byte) 1) }, null);
        RenderedOp source = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Byte[] { Byte.valueOf((byte) 0) }, null);

        final ImageLayout layout = new ImageLayout();
        layout.setTileHeight(256).setTileWidth(100);
        final RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);
        final ParameterBlockJAI pbj = new ParameterBlockJAI("ChangeMatrix");
        pbj.addSource(reference);
        pbj.addSource(source);
        pbj.setParameter("result", cm);
        pbj.setParameter("pixelMultiplier", pixelMultiplierBiggerThanShort);
        final RenderedOp result = JAI.create("ChangeMatrix", pbj, hints);
        result.getWidth();

        final Queue<Point> tiles = new ArrayBlockingQueue<Point>(result.getNumXTiles()
                * result.getNumYTiles());
        for (int i = 0; i < result.getNumXTiles(); i++) {
            for (int j = 0; j < result.getNumYTiles(); j++) {
                tiles.add(new Point(i, j));
            }
        }
        final CountDownLatch sem = new CountDownLatch(result.getNumXTiles() * result.getNumYTiles());
        ExecutorService ex = Executors.newFixedThreadPool(10);
        for (final Point tile : tiles) {
            ex.execute(new Runnable() {

                public void run() {
                    result.getTile(tile.x, tile.y);
                    sem.countDown();
                }
            });
        }
        sem.await();
        cm.freeze(); // stop changing the computations! If we did not do this new
                     // values would be accumulated as the file was written

        // Control on the final data type
        
        
        // CONTROL ON THE IMAGE PIXEL VALUES

        int minX = result.getMinX();
        int minY = result.getMinY();

        int maxX = result.getMaxX();
        int maxY = result.getMaxY();

        int resultValue = 0;
        int resultExpected = 0;
        int referenceValue = 0;
        int sourceValue = 0;

        Raster referenceTile;
        Raster sourceTile;
        Raster resultTile;

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                // Selection of the tiles associated with this position
                referenceTile = reference.getTile(reference.XToTileX(x), reference.YToTileY(y));
                sourceTile = source.getTile(source.XToTileX(x), source.YToTileY(y));
                resultTile = result.getTile(result.XToTileX(x), result.YToTileY(y));
                // Selection of the value associated with this position
                resultValue = resultTile.getSample(x, y, 0);
                referenceValue = referenceTile.getSample(x, y, 0);
                sourceValue = sourceTile.getSample(x, y, 0);
                // Calculation
                resultExpected = referenceValue + (pixelMultiplier * sourceValue);
                // Test
                assertEquals(resultExpected, resultValue);
            }
        }

        // try to write the resulting image before disposing the sources
        final File out = File.createTempFile("chm", "result.tif");
        out.deleteOnExit();
        ImageIO.write(result, "tiff", out);

        result.dispose();
        source.dispose();
        reference.dispose();
    }
    
    @Test
    // @Ignore
    public void completeTestShortToIntDatatype() throws Exception {

        final Set<Integer> classes = new HashSet<Integer>();
        classes.add(FIRST_CLASS_VALUE);
        classes.add(SECOND_CLASS_VALUE);
        classes.add(THIRD_CLASS_VALUE);
        classes.add(FOURTH_CLASS_VALUE);
        classes.add(FIFTH_CLASS_VALUE);
        final ChangeMatrix cm = new ChangeMatrix(classes);

        RenderedOp reference = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Short[] { Short.valueOf((short) 1) }, null);
        RenderedOp source = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Short[] { Short.valueOf((short) 0) }, null);

        final ImageLayout layout = new ImageLayout();
        layout.setTileHeight(256).setTileWidth(100);
        final RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);
        final ParameterBlockJAI pbj = new ParameterBlockJAI("ChangeMatrix");
        pbj.addSource(reference);
        pbj.addSource(source);
        pbj.setParameter("result", cm);
        pbj.setParameter("pixelMultiplier", pixelMultiplierBiggerThanShort);
        final RenderedOp result = JAI.create("ChangeMatrix", pbj, hints);
        result.getWidth();

        final Queue<Point> tiles = new ArrayBlockingQueue<Point>(result.getNumXTiles()
                * result.getNumYTiles());
        for (int i = 0; i < result.getNumXTiles(); i++) {
            for (int j = 0; j < result.getNumYTiles(); j++) {
                tiles.add(new Point(i, j));
            }
        }
        final CountDownLatch sem = new CountDownLatch(result.getNumXTiles() * result.getNumYTiles());
        ExecutorService ex = Executors.newFixedThreadPool(10);
        for (final Point tile : tiles) {
            ex.execute(new Runnable() {

                public void run() {
                    result.getTile(tile.x, tile.y);
                    sem.countDown();
                }
            });
        }
        sem.await();
        cm.freeze(); // stop changing the computations! If we did not do this new
                     // values would be accumulated as the file was written

        // CONTROL ON THE IMAGE PIXEL VALUES

        int minX = result.getMinX();
        int minY = result.getMinY();

        int maxX = result.getMaxX();
        int maxY = result.getMaxY();

        int resultValue = 0;
        int resultExpected = 0;
        int referenceValue = 0;
        int sourceValue = 0;

        Raster referenceTile;
        Raster sourceTile;
        Raster resultTile;

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                // Selection of the tiles associated with this position
                referenceTile = reference.getTile(reference.XToTileX(x), reference.YToTileY(y));
                sourceTile = source.getTile(source.XToTileX(x), source.YToTileY(y));
                resultTile = result.getTile(result.XToTileX(x), result.YToTileY(y));
                // Selection of the value associated with this position
                resultValue = resultTile.getSample(x, y, 0);
                referenceValue = referenceTile.getSample(x, y, 0);
                sourceValue = sourceTile.getSample(x, y, 0);
                // Calculation
                resultExpected = referenceValue + (pixelMultiplier * sourceValue);
                // Test
                assertEquals(resultExpected, resultValue);
            }
        }

        // try to write the resulting image before disposing the sources
        final File out = File.createTempFile("chm", "result.tif");
        out.deleteOnExit();
        ImageIO.write(result, "tiff", out);

        result.dispose();
        source.dispose();
        reference.dispose();
    }
    
    @Test
    public void testDataTypeShortFromByte() {
        final Set<Integer> classes = new HashSet<Integer>();
        classes.add(FIRST_CLASS_VALUE);
        classes.add(SECOND_CLASS_VALUE);
        classes.add(THIRD_CLASS_VALUE);
        classes.add(FOURTH_CLASS_VALUE);
        classes.add(FIFTH_CLASS_VALUE);
        final ChangeMatrix cm = new ChangeMatrix(classes);

        RenderedOp reference = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Byte[] { Byte.valueOf((byte) 1) }, null);
        RenderedOp now = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Byte[] { Byte.valueOf((byte) 0) }, null);
        final ParameterBlockJAI pbj = new ParameterBlockJAI("ChangeMatrix");
        pbj.addSource(reference);
        pbj.addSource(now);
        pbj.setParameter("result", cm);
        pbj.setParameter("pixelMultiplier", pixelMultiplier);
        final RenderedOp result = JAI.create("ChangeMatrix", pbj, null);
        result.getWidth();
        // Data Type calculation
        int initialDataType = reference.getSampleModel().getDataType();
        int finalDataType = result.getSampleModel().getDataType();
        assertEquals("The initial data type should have been Byte!", initialDataType,
                DataBuffer.TYPE_BYTE);
        assertEquals("The final data type should have been Short!", finalDataType,
                DataBuffer.TYPE_SHORT);
    }

    @Test
    public void testDataTypeIntFromByte() {
        final Set<Integer> classes = new HashSet<Integer>();
        classes.add(FIRST_CLASS_VALUE);
        classes.add(SECOND_CLASS_VALUE);
        classes.add(THIRD_CLASS_VALUE);
        classes.add(FOURTH_CLASS_VALUE);
        classes.add(FIFTH_CLASS_VALUE);
        final ChangeMatrix cm = new ChangeMatrix(classes);

        RenderedOp reference = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Byte[] { Byte.valueOf((byte) 1) }, null);
        RenderedOp now = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Byte[] { Byte.valueOf((byte) 0) }, null);
        final ParameterBlockJAI pbj = new ParameterBlockJAI("ChangeMatrix");
        pbj.addSource(reference);
        pbj.addSource(now);
        pbj.setParameter("result", cm);
        pbj.setParameter("pixelMultiplier", pixelMultiplierBiggerThanShort);
        final RenderedOp result = JAI.create("ChangeMatrix", pbj, null);
        result.getWidth();
        // Data Type calculation
        int initialDataType = reference.getSampleModel().getDataType();
        int finalDataType = result.getSampleModel().getDataType();
        assertEquals("The initial data type should have been Byte!", initialDataType,
                DataBuffer.TYPE_BYTE);
        assertEquals("The final data type should have been Integer!", finalDataType,
                DataBuffer.TYPE_INT);
    }

    @Test
    public void testDataTypeIntFromShort() {
        final Set<Integer> classes = new HashSet<Integer>();
        classes.add(FIRST_CLASS_VALUE);
        classes.add(SECOND_CLASS_VALUE);
        classes.add(THIRD_CLASS_VALUE);
        classes.add(FOURTH_CLASS_VALUE);
        classes.add(FIFTH_CLASS_VALUE);
        final ChangeMatrix cm = new ChangeMatrix(classes);

        RenderedOp reference = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Short[] { Short.valueOf((short) 1) }, null);
        RenderedOp now = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Short[] { Short.valueOf((short) 0) }, null);
        final ParameterBlockJAI pbj = new ParameterBlockJAI("ChangeMatrix");
        pbj.addSource(reference);
        pbj.addSource(now);
        pbj.setParameter("result", cm);
        pbj.setParameter("pixelMultiplier", pixelMultiplierBiggerThanShort);
        final RenderedOp result = JAI.create("ChangeMatrix", pbj, null);
        result.getWidth();
        // Data Type calculation
        int initialDataType = reference.getSampleModel().getDataType();
        int finalDataType = result.getSampleModel().getDataType();
        assertEquals("The initial data type should have been Short!", initialDataType,
                DataBuffer.TYPE_USHORT);
        assertEquals("The final data type should have been Integer!", finalDataType,
                DataBuffer.TYPE_INT);
    }
}
