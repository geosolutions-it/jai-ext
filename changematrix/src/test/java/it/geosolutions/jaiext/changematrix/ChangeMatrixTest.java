package it.geosolutions.jaiext.changematrix;

import it.geosolutions.jaiext.changematrix.ChangeMatrixDescriptor.ChangeMatrix;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
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

import org.junit.Test;

public class ChangeMatrixTest extends org.junit.Assert {

    private final static String REFERENCE_PATH_FOR_TESTS = "./src/test/resources/it/geosolutions/jaiext/changematrix/test-data/";
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
        classes.add(0);
        classes.add(1);
        classes.add(35);
        classes.add(36);
        classes.add(37);
        final ChangeMatrix cm = new ChangeMatrix(classes);

        BufferedImage reference = new BufferedImage(800, 600, BufferedImage.TYPE_3BYTE_BGR);
        RenderedOp now = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Byte[] { Byte.valueOf((byte) 0) }, null);
        final ParameterBlockJAI pbj = new ParameterBlockJAI("ChangeMatrix");
        pbj.addSource(reference);
        pbj.addSource(now);
        pbj.setParameter("result", cm);
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
        classes.add(0);
        classes.add(1);
        classes.add(35);
        classes.add(36);
        classes.add(37);
        final ChangeMatrix cm = new ChangeMatrix(classes);

        RenderedOp reference = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Integer[] { Integer.valueOf(1) }, null);
        RenderedOp now = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Byte[] { Byte.valueOf((byte) 0) }, null);
        final ParameterBlockJAI pbj = new ParameterBlockJAI("ChangeMatrix");
        pbj.addSource(reference);
        pbj.addSource(now);
        pbj.setParameter("result", cm);
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
        classes.add(0);
        classes.add(1);
        classes.add(35);
        classes.add(36);
        classes.add(37);
        final ChangeMatrix cm = new ChangeMatrix(classes);

        RenderedOp reference = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Integer[] { Integer.valueOf(1) }, null);
        RenderedOp now = ConstantDescriptor.create(Float.valueOf(700), Float.valueOf(600),
                new Integer[] { Integer.valueOf(0) }, null);
        final ParameterBlockJAI pbj = new ParameterBlockJAI("ChangeMatrix");
        pbj.addSource(reference);
        pbj.addSource(now);
        pbj.setParameter("result", cm);
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
        classes.add(0);
        classes.add(1);
        classes.add(35);
        classes.add(36);
        classes.add(37);
        final ChangeMatrix cm = new ChangeMatrix(classes);

        RenderedOp reference = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Float[] { Float.valueOf(1.0f) }, null);
        RenderedOp now = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Float[] { Float.valueOf(0.0f) }, null);
        final ParameterBlockJAI pbj = new ParameterBlockJAI("ChangeMatrix");
        pbj.addSource(reference);
        pbj.addSource(now);
        pbj.setParameter("result", cm);
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
        classes.add(0);
        classes.add(1);
        classes.add(35);
        classes.add(36);
        classes.add(37);
        final ChangeMatrix cm = new ChangeMatrix(classes);

        RenderedOp reference = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Double[] { Double.valueOf(1.0) }, null);
        RenderedOp now = ConstantDescriptor.create(Float.valueOf(800), Float.valueOf(600),
                new Double[] { Double.valueOf(0.0) }, null);
        final ParameterBlockJAI pbj = new ParameterBlockJAI("ChangeMatrix");
        pbj.addSource(reference);
        pbj.addSource(now);
        pbj.setParameter("result", cm);
        final RenderedOp result = JAI.create("ChangeMatrix", pbj, null);
        try {
            result.getWidth();
            assertTrue("we should have got an eception as the image types are Double!", false);
        } catch (Exception e) {
            // fine we get an exception
        }
    }

    @Test
    // @Ignore
    public void completeTestByteDatatype() throws Exception {
        final File file0 =  new File(REFERENCE_PATH_FOR_TESTS, "clc2000_L3_100m_small.tif");
        final File file6 =  new File(REFERENCE_PATH_FOR_TESTS, "clc2006_L3_100m_small.tif");
        final Set<Integer> classes = new HashSet<Integer>();
        classes.add(0);
        classes.add(1);
        classes.add(35);
        classes.add(36);
        classes.add(37);
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

                @Override
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
        final File out =  File.createTempFile("chm", "result.tif");
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
        final File file0 =  new File(REFERENCE_PATH_FOR_TESTS, "clc2000_L3_100m_small.tif");
        final File file6 =  new File(REFERENCE_PATH_FOR_TESTS, "clc2006_L3_100m_small.tif");

        final Set<Integer> classes = new HashSet<Integer>();
        classes.add(0);
        classes.add(1);
        classes.add(35);
        classes.add(36);
        classes.add(37);
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

                @Override
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
        final File out =  File.createTempFile("chm", "result.tif");
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
        final File file0 =  new File(REFERENCE_PATH_FOR_TESTS, "clc2000_L3_100m_small.tif");
        final File file6 =  new File(REFERENCE_PATH_FOR_TESTS, "clc2006_L3_100m_small.tif");

        final Set<Integer> classes = new HashSet<Integer>();
        classes.add(0);
        classes.add(1);
        classes.add(35);
        classes.add(36);
        classes.add(37);
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

                @Override
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
        final File out =  File.createTempFile("chm", "result.tif");
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
        final File file0 =  new File(REFERENCE_PATH_FOR_TESTS, "clc2000_L3_100m_small_short.tif");
        final File file6 =  new File(REFERENCE_PATH_FOR_TESTS, "clc2006_L3_100m_small_short.tif");

        final Set<Integer> classes = new HashSet<Integer>();
        classes.add(0);
        classes.add(1);
        classes.add(35);
        classes.add(36);
        classes.add(37);
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

                @Override
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
        final File out =  File.createTempFile("chm", "result.tif");
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
        final File file0 =  new File(REFERENCE_PATH_FOR_TESTS, "clc2000_L3_100m_smaller_int.tif");
        final File file6 =  new File(REFERENCE_PATH_FOR_TESTS, "clc2006_L3_100m_smaller_int.tif");

        final Set<Integer> classes = new HashSet<Integer>();
        classes.add(0);
        classes.add(1);
        classes.add(35);
        classes.add(36);
        classes.add(37);
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

                @Override
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
        final File out =  File.createTempFile("chm", "result.tif");
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

}
