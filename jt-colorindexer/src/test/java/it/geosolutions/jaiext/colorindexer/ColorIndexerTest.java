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
package it.geosolutions.jaiext.colorindexer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import it.geosolutions.jaiext.bandcombine.BandCombineDescriptor;
import it.geosolutions.jaiext.bandmerge.BandMergeDescriptor;
import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.lookup.LookupDescriptor;
import it.geosolutions.jaiext.lookup.LookupTableWrapper;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.stats.Statistics;
import it.geosolutions.jaiext.stats.Statistics.StatsType;
import it.geosolutions.jaiext.stats.StatisticsDescriptor;
import it.geosolutions.jaiext.testclasses.TestBase;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.Arrays;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;
import javax.media.jai.iterator.RandomIter;

import org.junit.Test;

/**
 * Testing color indexer operation.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Andrea Aime, GeoSolutions
 * 
 * @source $URL$
 */
public class ColorIndexerTest extends TestBase {

    private static final double TOLERANCE = 0.01d;

    @Test
    public void test2BandsBug() {
        // build a transparent image
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_BYTE_GRAY);
        RenderedOp img = BandMergeDescriptor.create(null, 0, false, null, image, image);

        image = img.getAsBufferedImage();

        // create a palette out of it
        RenderedImage indexed = quantize(image);
        assertTrue(indexed.getColorModel() instanceof IndexColorModel);
        IndexColorModel icm = (IndexColorModel) indexed.getColorModel();

        // png encoder go mad if they get a one element palette, we need at
        // least two
        assertEquals(2, icm.getMapSize());
    }

    private RenderedImage quantize(RenderedImage image) {
        return quantize(image, null, null, 0);
    }

    private RenderedImage quantize(RenderedImage image, ROI roi, Range nodata, int destNoData) {
        Quantizer q = new Quantizer(256);
        ColorIndexer indexer = q.buildColorIndexer(image);
        RenderedImage indexed = ColorIndexerDescriptor.create(image, indexer, roi, nodata,
                destNoData, null);

        checkNoDataROI(indexed, image, roi, nodata, destNoData);

        return indexed;
    }

    @Test
    public void testOneColorBug() {
        // build a transparent image
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_4BYTE_ABGR);

        // create a palette out of it
        RenderedImage indexed = quantize(image);
        assertTrue(indexed.getColorModel() instanceof IndexColorModel);
        IndexColorModel icm = (IndexColorModel) indexed.getColorModel();

        // png encoder go mad if they get a one element palette, we need at
        // least two
        assertEquals(2, icm.getMapSize());
    }

    @Test
    public void testGrayColorNoData() {
        // Testing color indexing with Nodata
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = image.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 20, 20);
        g.setColor(new Color(20, 20, 20)); // A dark gray
        g.fillRect(20, 20, 20, 20);
        g.setColor(new Color(200, 200, 200)); // A light gray
        g.fillRect(0, 20, 20, 20);
        g.dispose();
        RenderedImage indexed = quantize(image, null, RangeFactory.create((byte) 255, (byte) 255),
                1);
        assertTrue(indexed.getColorModel() instanceof IndexColorModel);
        IndexColorModel icm = (IndexColorModel) indexed.getColorModel();
        assertEquals(4, icm.getMapSize()); // Black background, white fill,
                                           // light gray fill, dark gray fill =
                                           // 4 colors

    }

    @Test
    public void testTranslatedImage() throws Exception {
        BufferedImage bi = new BufferedImage(256, 256, BufferedImage.TYPE_BYTE_GRAY);
        TiledImage image = new TiledImage(0, 0, 256, 256, 1, 1, bi.getSampleModel()
                .createCompatibleSampleModel(256, 256), bi.getColorModel());
        Graphics g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 20, 20);
        g.setColor(new Color(20, 20, 20)); // A dark gray
        g.fillRect(20, 20, 20, 20);
        g.setColor(new Color(200, 200, 200)); // A light gray
        g.fillRect(0, 20, 20, 20);
        g.dispose();
        RenderedImage indexed = quantize(image);
        assertTrue(indexed.getColorModel() instanceof IndexColorModel);
        IndexColorModel icm = (IndexColorModel) indexed.getColorModel();
        assertEquals(4, icm.getMapSize()); // Black background, white fill,
                                           // light gray fill, dark gray fill =
                                           // 4 colors
    }

    @Test
    public void testFourColorROI() {
        // Testing color indexing with ROI
        // build a transparent image
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics g = image.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 10, 10);
        g.setColor(Color.RED);
        g.fillRect(10, 0, 10, 10);
        g.setColor(Color.BLUE);
        g.fillRect(20, 0, 10, 10);
        g.setColor(Color.GREEN);
        g.fillRect(30, 0, 10, 10);
        g.dispose();

        //
        // create a palette out of it
        //
        RenderedImage indexed = quantize(image, new ROIShape(new Rectangle(10, 0, 10, 10)), null,
                10);
        assertTrue(indexed.getColorModel() instanceof IndexColorModel);
        IndexColorModel icm = (IndexColorModel) indexed.getColorModel();

        // make sure we have 4 colors + transparent one
        assertEquals(5, icm.getMapSize());
    }

    @Test
    public void testTranslatedImageTileGridROINoData() {
        // Testing color indexing with Nodata and ROI
        BufferedImage image_ = new BufferedImage(256, 256, BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = image_.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(236, 236, 20, 20);
        g.setColor(new Color(80, 80, 80)); // A dark gray
        g.fillRect(216, 216, 20, 20);
        g.setColor(new Color(200, 200, 200)); // A light gray
        g.fillRect(216, 236, 20, 20);
        g.dispose();

        TiledImage image = new TiledImage(0, 0, 256, 256, 128, 128, image_.getColorModel()
                .createCompatibleSampleModel(256, 256), image_.getColorModel());
        image.set(image_);

        Range range = RangeFactory.create((byte) 255, (byte) 255);
        ROIShape roi = new ROIShape(new Rectangle(0, 0, 20, 20));
        RenderedImage indexed = quantize(image, roi, range, 1);
        assertTrue(indexed.getColorModel() instanceof IndexColorModel);
        IndexColorModel icm = (IndexColorModel) indexed.getColorModel();
        assertEquals(4, icm.getMapSize()); // Black background, white fill,
                                           // light gray fill, dark gray fill =
                                           // 4 colors

        // check image not black
        RenderedImage component = forceComponentColorModel(indexed);

        final double[][] coeff = new double[1][5];
        Arrays.fill(coeff[0], 0, 4, 1.0 / 4);
        component = BandCombineDescriptor.create(component, coeff, null, null, destinationNoData,
                null);

        StatsType[] stats = new StatsType[] { StatsType.EXTREMA };
        component = StatisticsDescriptor.create(component, 1, 1, null, null, false,
                new int[] { 0 }, stats, null);
        Statistics stat = ((Statistics[][]) component.getProperty(Statistics.STATS_PROPERTY))[0][0];
        double[] result = (double[]) stat.getResult();
        final double minimum = result[0];
        final double maximum = result[1];

        assertFalse(Math.abs(maximum - minimum) < TOLERANCE);
    }

    private RenderedImage forceComponentColorModel(RenderedImage image) {
        final IndexColorModel icm = (IndexColorModel) image.getColorModel();
        final SampleModel sm = image.getSampleModel();
        final int datatype = sm.getDataType();
        final boolean alpha = icm.hasAlpha();
        // Definition of the lookup table
        final int numDestinationBands = 4;
        LookupTableJAI lut = null;

        final byte data[][] = new byte[numDestinationBands][icm.getMapSize()];
        icm.getReds(data[0]);
        icm.getGreens(data[1]);
        icm.getBlues(data[2]);
        icm.getAlphas(data[3]);

        lut = new LookupTableJAI(data);
        // Layout creation
        final ImageLayout layout = new ImageLayout(image);
        final RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);

        int[] bits = new int[numDestinationBands];
        // bits per component
        for (int i = 0; i < numDestinationBands; i++)
            bits[i] = sm.getSampleSize(i);
        final ComponentColorModel destinationColorModel = new ComponentColorModel(
                ColorSpace.getInstance(ColorSpace.CS_sRGB), bits, alpha, image.getColorModel()
                        .isAlphaPremultiplied(), alpha ? Transparency.TRANSLUCENT
                        : Transparency.OPAQUE, datatype);
        final SampleModel destinationSampleModel = destinationColorModel
                .createCompatibleSampleModel(image.getWidth(), image.getHeight());
        layout.setColorModel(destinationColorModel);
        layout.setSampleModel(destinationSampleModel);
        // Lookup Operations
        image = LookupDescriptor.create(image, new LookupTableWrapper(lut), 0, null, null, false,
                hints);

        return image;
    }

    /**
     * Checking if NoData and ROI are defined correctly
     * 
     * @param indexed
     * @param image
     * @param roi
     * @param nodata
     * @param destNoData
     */
    private void checkNoDataROI(RenderedImage indexed, RenderedImage image, ROI roi, Range nodata,
            int destNoData) {
        // Ensure the dimensions are the same
        assertEquals(indexed.getMinX(), image.getMinX());
        assertEquals(indexed.getMinY(), image.getMinY());
        assertEquals(indexed.getWidth(), image.getWidth());
        assertEquals(indexed.getHeight(), image.getHeight());

        boolean roiExists = roi != null;
        boolean nodataExists = nodata != null;
        // Simply ensure no exception is thrown
        if (!nodataExists && !roiExists) {
            PlanarImage.wrapRenderedImage(indexed).getTiles();
            return;
        }

        if (nodataExists) {
            nodata = RangeFactory.convertToDoubleRange(nodata);
        }
        RandomIter roiIter = null;
        Rectangle roiBounds = null;
        if (roiExists) {
            PlanarImage roiIMG = roi.getAsImage();
            roiIter = RandomIterFactory.create(roiIMG, null, true, true);
            roiBounds = roi.getBounds();
        }

        // Else check ROI and NoData
        RandomIter sourceIter = RandomIterFactory.create(image, null, true, true);
        RandomIter destIter = RandomIterFactory.create(indexed, null, true, true);
        // Start the iteration (we iterate only the first band)
        int w = image.getWidth();
        int h = image.getHeight();
        int minX = image.getMinX();
        int minY = image.getMinY();
        int maxX = minX + w;
        int maxY = minY + h;
        int limx = minX - image.getTileGridXOffset();
        int limy = minY - image.getTileGridYOffset();
        Rectangle translated = new Rectangle(limx, limy, w, h);

        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {

                double src = sourceIter.getSampleDouble(x, y, 0);
                double dest = destIter.getSampleDouble(x, y, 0);

                boolean valid = true;

                // ROI Check
                if (roiExists && !(roiBounds.contains(x, y) && roiIter.getSample(x, y, 0) > 0)
                        && translated.contains(x, y)) {
                    valid = false;
                }

                // NoData Check
                if (nodataExists && nodata.contains(src)) {
                    valid = false;
                }
                if (!valid) {
                    assertEquals(destNoData, dest, TOLERANCE);
                }
            }
        }
    }
}
