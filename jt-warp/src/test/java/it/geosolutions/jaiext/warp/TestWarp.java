package it.geosolutions.jaiext.warp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import it.geosolutions.jaiext.interpolators.InterpolationBicubic;
import it.geosolutions.jaiext.interpolators.InterpolationBilinear;
import it.geosolutions.jaiext.interpolators.InterpolationNearest;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.stats.Statistics;
import it.geosolutions.jaiext.stats.Statistics.StatsType;
import it.geosolutions.jaiext.stats.StatisticsDescriptor;
import it.geosolutions.jaiext.testclasses.TestBase;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.IOException;

import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;
import javax.media.jai.Warp;

import org.geotools.renderedimage.viewer.RenderedImageBrowser;
import org.junit.AfterClass;
import org.junit.Test;

public class TestWarp extends TestBase {

    protected final static double ANGLE_ROTATION = 45d;

    protected static Warp warpObj;

    protected static final int NUM_IMAGES = 6;

    protected static RenderedImage[] images;

    protected static byte noDataValueB;

    protected static InterpolationType interpType;

    protected static short noDataValueU;

    protected static short noDataValueS;

    protected static int noDataValueI;

    protected static float noDataValueF;

    protected static double noDataValueD;

    public void testWarp(RenderedImage source, boolean noDataUsed, boolean roiUsed, Warp warpObj,
            Number noDataValue, InterpolationType interpType, TestSelection testSelect) {

        // Image data type
        int dataType = source.getSampleModel().getDataType();

        // No Data Range
        Range noDataRange = null;

        if (noDataUsed) {
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                noDataRange = RangeFactory.create(noDataValue.byteValue(), true,
                        noDataValue.byteValue(), true);
                break;
            case DataBuffer.TYPE_USHORT:
                noDataRange = RangeFactory.create(noDataValue.shortValue(), true,
                        noDataValue.shortValue(), true);
                break;
            case DataBuffer.TYPE_SHORT:
                noDataRange = RangeFactory.create(noDataValue.shortValue(), true,
                        noDataValue.shortValue(), true);
                break;
            case DataBuffer.TYPE_INT:
                noDataRange = RangeFactory.create(noDataValue.intValue(), true,
                        noDataValue.intValue(), true);
                break;
            case DataBuffer.TYPE_FLOAT:
                noDataRange = RangeFactory.create(noDataValue.floatValue(), true,
                        noDataValue.floatValue(), true, true);
                break;
            case DataBuffer.TYPE_DOUBLE:
                noDataRange = RangeFactory.create(noDataValue.doubleValue(), true,
                        noDataValue.doubleValue(), true, true);
                break;
            default:
                throw new IllegalArgumentException("Wrong data type");
            }
        }

        // ROI
        ROI roi = null;

        if (roiUsed) {
            roi = roiCreation();
        }

        // Hints used for image expansion
        RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                BorderExtender.createInstance(BorderExtender.BORDER_COPY));

        RenderedOp destinationIMG = null;

        //Interpolator variable
        Interpolation interp;
        
        // Interpolators
        switch (interpType) {
        case NEAREST_INTERP:
            // Nearest-Neighbor
            interp = new InterpolationNearest(noDataRange, false, destinationNoData, dataType);
            break;
        case BILINEAR_INTERP:
            // Bilinear
            interp = new InterpolationBilinear(DEFAULT_SUBSAMPLE_BITS, noDataRange, false,
                    destinationNoData, dataType);

            break;
        case BICUBIC_INTERP:
            // Bicubic
            interp = new InterpolationBicubic(DEFAULT_SUBSAMPLE_BITS, noDataRange, false,
                    destinationNoData, dataType, true, DEFAULT_PRECISION_BITS);

            break;
        case GENERAL_INTERP:
            // Bicubic
            interp = new javax.media.jai.InterpolationBicubic(DEFAULT_SUBSAMPLE_BITS);

            break;
        default:
            throw new IllegalArgumentException("Wrong interpolation type");
        }

        // Warp operation
        destinationIMG = WarpDescriptor.create(source, warpObj, interp, null, roi, hints);

        if (INTERACTIVE && dataType == DataBuffer.TYPE_BYTE
                && TEST_SELECTOR == testSelect.getType()) {
            RenderedImageBrowser.showChain(destinationIMG, false, roiUsed);
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Forcing to retrieve an array of all the image tiles
            destinationIMG.getTiles();
        }

        // Control if the operation has been correctly performed.

        // Check if the final image dimensions are more than 0
        assertTrue(destinationIMG.getHeight() > 0);
        assertTrue(destinationIMG.getWidth() > 0);

        // Check if the finalImage dimensions are correct
        Rectangle inputRect = new Rectangle(source.getMinX(), source.getMinY(), source.getWidth(),
                source.getHeight());

        Rectangle outputRect = warpObj.mapSourceRect(inputRect);

        assertEquals(destinationIMG.getMinX(), outputRect.x);
        assertEquals(destinationIMG.getMinY(), outputRect.y);
        assertEquals(destinationIMG.getHeight(), outputRect.width);
        assertEquals(destinationIMG.getWidth(), outputRect.height);

        // Control if the final image has some data
        StatsType[] stats = new StatsType[] { StatsType.MEAN };
        Range noData;
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            noData = RangeFactory.create((byte) destinationNoData, true, (byte) destinationNoData,
                    true);
            break;
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            noData = RangeFactory.create((short) destinationNoData, true,
                    (short) destinationNoData, true);
            break;
        case DataBuffer.TYPE_INT:
            noData = RangeFactory.create((int) destinationNoData, true, (int) destinationNoData,
                    true);
            break;
        case DataBuffer.TYPE_FLOAT:
            noData = RangeFactory.create((float) destinationNoData, true,
                    (float) destinationNoData, true, true);
            break;
        case DataBuffer.TYPE_DOUBLE:
            noData = RangeFactory.create(destinationNoData, true, destinationNoData, true, true);
            break;
        default:
            throw new IllegalArgumentException("Wrong data type");
        }
        RenderedOp statistics = StatisticsDescriptor.create(destinationIMG, 1, 1, roi, noData,
                false, new int[] { 0 }, stats, null, null, null, hints);

        Statistics[][] result = (Statistics[][]) statistics.getProperty(Statistics.STATS_PROPERTY);
        // Image disposal
        destinationIMG.dispose();
        double mean = (Double) result[0][0].getResult();
        long num = (long) result[0][0].getNumSamples();

        assertTrue(num > 0);
        assertTrue(mean != destinationNoData);

        // Image disposal
        statistics.dispose();
    }

    @Test
    public void testImage() {
        boolean roiUsed = false;
        boolean noDataUsed = false;
        TestSelection testSelect = TestSelection.NO_ROI_ONLY_DATA;

        testWarp(images[0], noDataUsed, roiUsed, warpObj, noDataValueB, interpType, testSelect);
        testWarp(images[1], noDataUsed, roiUsed, warpObj, noDataValueU, interpType, testSelect);
        testWarp(images[2], noDataUsed, roiUsed, warpObj, noDataValueS, interpType, testSelect);
        testWarp(images[3], noDataUsed, roiUsed, warpObj, noDataValueI, interpType, testSelect);
        testWarp(images[4], noDataUsed, roiUsed, warpObj, noDataValueF, interpType, testSelect);
        testWarp(images[5], noDataUsed, roiUsed, warpObj, noDataValueD, interpType, testSelect);
    }

    @Test
    public void testImageROI() {
        boolean roiUsed = true;
        boolean noDataUsed = false;
        TestSelection testSelect = TestSelection.ROI_ONLY_DATA;

        testWarp(images[0], noDataUsed, roiUsed, warpObj, noDataValueB, interpType, testSelect);
        testWarp(images[1], noDataUsed, roiUsed, warpObj, noDataValueU, interpType, testSelect);
        testWarp(images[2], noDataUsed, roiUsed, warpObj, noDataValueS, interpType, testSelect);
        testWarp(images[3], noDataUsed, roiUsed, warpObj, noDataValueI, interpType, testSelect);
        testWarp(images[4], noDataUsed, roiUsed, warpObj, noDataValueF, interpType, testSelect);
        testWarp(images[5], noDataUsed, roiUsed, warpObj, noDataValueD, interpType, testSelect);
    }

    @Test
    public void testImageNoData() {
        boolean roiUsed = false;
        boolean noDataUsed = true;
        TestSelection testSelect = TestSelection.NO_ROI_NO_DATA;

        testWarp(images[0], noDataUsed, roiUsed, warpObj, noDataValueB, interpType, testSelect);
        testWarp(images[1], noDataUsed, roiUsed, warpObj, noDataValueU, interpType, testSelect);
        testWarp(images[2], noDataUsed, roiUsed, warpObj, noDataValueS, interpType, testSelect);
        testWarp(images[3], noDataUsed, roiUsed, warpObj, noDataValueI, interpType, testSelect);
        testWarp(images[4], noDataUsed, roiUsed, warpObj, noDataValueF, interpType, testSelect);
        testWarp(images[5], noDataUsed, roiUsed, warpObj, noDataValueD, interpType, testSelect);
    }

    @Test
    public void testImageNoDataROI() {
        boolean roiUsed = true;
        boolean noDataUsed = true;
        TestSelection testSelect = TestSelection.ROI_NO_DATA;

        testWarp(images[0], noDataUsed, roiUsed, warpObj, noDataValueB, interpType, testSelect);
        testWarp(images[1], noDataUsed, roiUsed, warpObj, noDataValueU, interpType, testSelect);
        testWarp(images[2], noDataUsed, roiUsed, warpObj, noDataValueS, interpType, testSelect);
        testWarp(images[3], noDataUsed, roiUsed, warpObj, noDataValueI, interpType, testSelect);
        testWarp(images[4], noDataUsed, roiUsed, warpObj, noDataValueF, interpType, testSelect);
        testWarp(images[5], noDataUsed, roiUsed, warpObj, noDataValueD, interpType, testSelect);
    }

    @AfterClass
    public static void finalStuff() {
        // Creation of the images
        if (images != null) {
            ((TiledImage) images[0]).dispose();
            ((TiledImage) images[1]).dispose();
            ((TiledImage) images[2]).dispose();
            ((TiledImage) images[3]).dispose();
            ((TiledImage) images[4]).dispose();
            ((TiledImage) images[5]).dispose();
        }
    }

}
