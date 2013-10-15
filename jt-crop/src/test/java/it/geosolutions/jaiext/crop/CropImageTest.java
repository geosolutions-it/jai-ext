package it.geosolutions.jaiext.crop;

import static org.junit.Assert.*;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.stats.Statistics;
import it.geosolutions.jaiext.stats.Statistics.StatsType;
import it.geosolutions.jaiext.stats.StatisticsDescriptor;
import it.geosolutions.jaiext.testclasses.TestBase;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.TileCache;
import javax.media.jai.operator.SubtractDescriptor;

import org.geotools.renderedimage.viewer.RenderedImageBrowser;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.media.jai.util.SunTileCache;

public class CropImageTest extends TestBase {

    private final static byte noDataValue = 50;

    private static RenderedImage source;

    private static double[] destNoData;

    @BeforeClass
    public static void initialSetup() {
        source = buildSource();
        destNoData = new double[] { 127 };
    }

    @Test
    public void testCropImagePB() {

        ParameterBlock pb = buildParameterBlock(source, false, false, false);

        ParameterBlock pbNew = buildParameterBlock(source, true, false, false);

        RenderedOp cropped = JAI.create("crop", pb);
        RenderedOp jaiextCropped = JAI.create("CropNoData", pbNew);
        assertImageEquals(cropped, jaiextCropped);
        
        if (INTERACTIVE && TEST_SELECTOR == 0) {
            RenderedImageBrowser.showChain(jaiextCropped, false, false);
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Test
    public void testCropImageROI() {

        ParameterBlock pb = buildParameterBlock(source, false, false, false);

        ParameterBlock pbNew = buildParameterBlock(source, true, true, false);

        RenderedOp cropped = JAI.create("crop", pb);
        RenderedOp jaiextCropped = JAI.create("CropNoData", pbNew);

        Rectangle boundOld = cropped.getBounds();

        Rectangle boundNew = jaiextCropped.getBounds();

        boolean contained = boundNew.getMinX() >= boundOld.getMinX()
                && boundNew.getMinY() >= boundOld.getMinY()
                && boundNew.getMaxX() <= boundOld.getMaxX()
                && boundNew.getMaxY() <= boundOld.getMaxY();

        assertTrue(contained);
        
        if (INTERACTIVE && TEST_SELECTOR == 1) {
            RenderedImageBrowser.showChain(jaiextCropped, false, true);
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Test
    public void testCropImageROINoData() {

        ParameterBlock pb = buildParameterBlock(source, false, false, false);

        ParameterBlock pbNew = buildParameterBlock(source, true, true, true);

        RenderedOp cropped = JAI.create("crop", pb);
        RenderedOp jaiextCropped = JAI.create("CropNoData", pbNew);

        Rectangle boundOld = cropped.getBounds();

        Rectangle boundNew = jaiextCropped.getBounds();

        boolean contained = boundNew.getMinX() >= boundOld.getMinX()
                && boundNew.getMinY() >= boundOld.getMinY()
                && boundNew.getMaxX() <= boundOld.getMaxX()
                && boundNew.getMaxY() <= boundOld.getMaxY();

        assertTrue(contained);

        int tileMinX = jaiextCropped.getMinTileX();
        int tileMinY = jaiextCropped.getMinTileY();

        Raster tile = jaiextCropped.getTile(tileMinX, tileMinY);

        int tileMinXpix = tile.getMinX();
        int tileMinYpix = tile.getMinY();

        int tileMaxXpix = tile.getWidth() + tileMinXpix;
        int tileMaxYpix = tile.getHeight() + tileMinYpix;

        boolean destinationNoDataFound = false;

        for (int i = tileMinXpix; i < tileMaxXpix; i++) {
            for (int j = tileMinYpix; j < tileMaxYpix; j++) {

                int value = tile.getSample(i, j, 0);

                if (value == (int) destNoData[0]) {
                    destinationNoDataFound = true;
                    break;
                }
            }
            if (destinationNoDataFound) {
                break;
            }
        }

        if (INTERACTIVE && TEST_SELECTOR == 2) {
            RenderedImageBrowser.showChain(jaiextCropped, false, true);
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Test
    public void testTileCache() {
        TileCache tc = new SunTileCache();
        RenderingHints hints = new RenderingHints(JAI.KEY_TILE_CACHE, tc);

        ParameterBlock pb = buildParameterBlock(source, true, false, false);

        RenderedOp jaiextCropped = JAI.create("CropNoData", pb, hints);
        jaiextCropped.getColorModel(); // force to compute the image
        assertSame(tc, jaiextCropped.getRenderingHint(JAI.KEY_TILE_CACHE));
    }

    @Test
    public void testNullTileCache() {
        RenderingHints hints = new RenderingHints(JAI.KEY_TILE_CACHE, null);

        ParameterBlock pb = buildParameterBlock(source, true, false, false);

        RenderedOp jaiCropped = JAI.create("CropNoData", pb, hints);
        jaiCropped.getColorModel(); // force to compute the image
        assertNull(jaiCropped.getRenderingHint(JAI.KEY_TILE_CACHE));
    }

    @Test
    public void testNullTileCacheDescriptor() {
        RenderingHints hints = new RenderingHints(JAI.KEY_TILE_CACHE, null);

        RenderedOp cropped = CropDescriptor.create(source, 10f, 10f, 20f, 20f, null, null, null,
                hints);
        cropped.getColorModel(); // force to compute the image
        assertNull(cropped.getRenderingHint(JAI.KEY_TILE_CACHE));
    }

    private static RenderedImage buildSource() {
        RenderedImage source = createTestImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataValue, false);
        return source;
    }

    private void assertImageEquals(RenderedOp first, RenderedOp second) {
        RenderedOp difference = SubtractDescriptor.create(first, second, null);
        // RenderedOp stats = ExtremaDescriptor.create(difference, null, 1, 1, false, 1, null);

        StatsType[] statsType = new StatsType[] { StatsType.EXTREMA };

        RenderedOp stats = StatisticsDescriptor.create(difference, 1, 1, null, null, false,
                new int[] { 0, 1, 2 }, statsType, null);

        Statistics[][] results = (Statistics[][]) stats.getProperty(Statistics.STATS_PROPERTY);

        for (int i = 0; i < results.length; i++) {
            double[] data = (double[]) results[i][0].getResult();
            assertEquals(data[0], data[1], 0.0);
        }
    }

    private ParameterBlock buildParameterBlock(RenderedImage source, boolean newDescriptor,
            boolean roiUsed, boolean noDataUsed) {
        ParameterBlockJAI pb;
        if (newDescriptor) {
            pb = new ParameterBlockJAI("CropNoData");
        } else {
            pb = new ParameterBlockJAI("crop");
        }

        pb.setSource("source0", source);

        pb.setParameter("x", (float) 0);
        pb.setParameter("y", (float) 0);
        pb.setParameter("width", (float) 20);
        pb.setParameter("height", (float) 20);
        if (newDescriptor) {
            if (roiUsed) {
                ROI roi = new ROIShape(new Rectangle(5, 5, 10, 10));
                pb.setParameter("ROI", roi);
            }
            if (noDataUsed) {
                Range noData = RangeFactory.create(noDataValue, true, noDataValue, true);
                pb.setParameter("NoData", noData);
                pb.setParameter("destNoData", destNoData);
            }

        }
        return pb;
    }

    // UNSUPPORTED OPERATIONS
    @Override
    protected void testGlobal(boolean useROIAccessor, boolean isBinary, boolean bicubic2Disabled,
            boolean noDataRangeUsed, boolean roiPresent, InterpolationType interpType,
            TestSelection testSelect, ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported in this test class");
    }

    @Override
    protected <T extends Number & Comparable<? super T>> void testImage(int dataType,
            T noDataValue, boolean useROIAccessor, boolean isBinary, boolean bicubic2Disabled,
            boolean noDataRangeUsed, boolean roiPresent, InterpolationType interpType,
            TestSelection testSelect, ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported in this test class");
    }

    @Override
    protected <T extends Number & Comparable<? super T>> void testImageAffine(
            RenderedImage sourceImage, int dataType, T noDataValue, boolean useROIAccessor,
            boolean isBinary, boolean bicubic2Disabled, boolean noDataRangeUsed,
            boolean roiPresent, boolean setDestinationNoData, TransformationType transformType,
            InterpolationType interpType, TestSelection testSelect, ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported in this test class");
    }

    @Override
    protected void testGlobalAffine(boolean useROIAccessor, boolean isBinary,
            boolean bicubic2Disabled, boolean noDataRangeUsed, boolean roiPresent,
            boolean setDestinationNoData, InterpolationType interpType, TestSelection testSelect,
            ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported in this test class");
    }

}
