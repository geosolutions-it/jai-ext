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
package it.geosolutions.jaiext.errordiffusion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.stats.Statistics;
import it.geosolutions.jaiext.stats.Statistics.StatsType;
import it.geosolutions.jaiext.stats.StatisticsDescriptor;
import it.geosolutions.jaiext.testclasses.TestBase;
import it.geosolutions.jaiext.testclasses.TestData;
import it.geosolutions.rendered.viewer.RenderedImageBrowser;

import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.media.jai.ColorCube;
import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RasterFactory;
import javax.media.jai.RenderedOp;
import javax.media.jai.iterator.RandomIter;

import org.junit.Test;

/**
 * Test class for the ErrorDiffusion operation
 * 
 * @author Nicola Lagomarsini geosolutions
 * 
 */
public class ErrorDiffusionTest extends TestBase {

    private static final double TOLERANCE = 0.01d;

    /**
     * Synthetic with Short Sample Model
     * 
     * @throws IOException
     */
    @Test
    public void testSyntheticShort() throws IOException {
        // Create simple lookuptable
        float[] data = new float[256];
        for (int i = 0; i < 256; i++) {
            data[i] = i;
        }
        LookupTableJAI lt = new LookupTableJAI(data);
        // Create the Kernel
        KernelJAI k = KernelJAI.GRADIENT_MASK_SOBEL_VERTICAL;

        final BufferedImage image = getSyntheticShortImage();

        ParameterBlockJAI pbj = new ParameterBlockJAI("ErrorDiffusion");
        pbj.addSource(image);
        pbj.setParameter("errorKernel", k);
        pbj.setParameter("colorMap", lt);
        RenderedOp finalimage = JAI.create("ErrorDiffusion", pbj);

        if (INTERACTIVE)
            RenderedImageBrowser.showChain(finalimage, false, false, null);
        else
            finalimage.getTiles();
        // Check NoData and ROI
        checkNoDataROI(finalimage, image, null, null);
        finalimage.dispose();

        // ROI creation
        ROI roi = new ROIShape(new Rectangle(image.getMinX() + 5, image.getMinY() + 5,
                image.getWidth() / 4, image.getHeight() / 4));

        Range nodata = RangeFactory.create((short) 5, (short) 5);

        // ROI
        pbj = new ParameterBlockJAI("ErrorDiffusion");
        pbj.addSource(image);
        pbj.setParameter("errorKernel", k);
        pbj.setParameter("colorMap", lt);
        pbj.setParameter("roi", roi);
        finalimage = JAI.create("ErrorDiffusion", pbj);

        if (INTERACTIVE)
            RenderedImageBrowser.showChain(finalimage, false, false, null);
        else
            finalimage.getTiles();
        // Check NoData and ROI
        checkNoDataROI(finalimage, image, roi, null);
        finalimage.dispose();

        // NODATA
        pbj = new ParameterBlockJAI("ErrorDiffusion");
        pbj.addSource(image);
        pbj.setParameter("errorKernel", k);
        pbj.setParameter("colorMap", lt);
        pbj.setParameter("nodata", nodata);
        finalimage = JAI.create("ErrorDiffusion", pbj);

        if (INTERACTIVE)
            RenderedImageBrowser.showChain(finalimage, false, false, null);
        else
            finalimage.getTiles();
        // Check NoData and ROI
        checkNoDataROI(finalimage, image, null, nodata);
        finalimage.dispose();

        // NODATA AND ROI
        pbj = new ParameterBlockJAI("ErrorDiffusion");
        pbj.addSource(image);
        pbj.setParameter("errorKernel", k);
        pbj.setParameter("colorMap", lt);
        pbj.setParameter("roi", roi);
        pbj.setParameter("nodata", nodata);
        finalimage = JAI.create("ErrorDiffusion", pbj);

        if (INTERACTIVE)
            RenderedImageBrowser.showChain(finalimage, false, false, null);
        else
            finalimage.getTiles();
        // Check NoData and ROI
        checkNoDataROI(finalimage, image, roi, nodata);
        finalimage.dispose();
    }

    /**
     * Method for checking if ROI and NoData are properly set
     * 
     * @param finalimage
     * @param image
     * @param roi
     * @param nodata
     */
    private void checkNoDataROI(RenderedOp finalimage, RenderedImage image, ROI roi, Range nodata) {
        // Ensure the dimensions are the same
        assertEquals(finalimage.getMinX(), image.getMinX());
        assertEquals(finalimage.getMinY(), image.getMinY());
        assertEquals(finalimage.getWidth(), image.getWidth());
        assertEquals(finalimage.getHeight(), image.getHeight());

        boolean roiExists = roi != null;
        boolean nodataExists = nodata != null;

        // If ROI and NoData are not present we only check that maximum and minimum are not equals
        if (!roiExists && !nodataExists) {
            StatsType[] stats = new StatsType[] { StatsType.EXTREMA };
            RenderedOp calculated = StatisticsDescriptor.create(finalimage, 1, 1, null, null,
                    false, new int[] { 0 }, stats, null);
            Statistics stat = ((Statistics[][]) calculated.getProperty(Statistics.STATS_PROPERTY))[0][0];
            double[] result = (double[]) stat.getResult();
            final double minimum = result[0];
            final double maximum = result[1];
            assertTrue(minimum < maximum);
            return;
        }
        if (nodataExists) {
            nodata = RangeFactory.convertToDoubleRange(nodata);
        }
        RandomIter roiIter = null;
        Rectangle roiBounds = null;
        if (roiExists) {
            PlanarImage roiIMG = roi.getAsImage();
            roiIter = RandomIterFactory.create(roiIMG, finalimage.getBounds(), true, true);
            roiBounds = roi.getBounds();
        }
        // Else check ROI and NoData
        RandomIter sourceIter = RandomIterFactory.create(image, null, true, true);
        RandomIter destIter = RandomIterFactory.create(finalimage, null, true, true);
        // Start the iteration (we iterate only the first band)
        int w = image.getWidth();
        int h = image.getHeight();
        int minX = image.getMinX();
        int minY = image.getMinY();
        int maxX = minX + w;
        int maxY = minY + h;

        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {

                double src = sourceIter.getSampleDouble(x, y, 0);
                double dest = destIter.getSampleDouble(x, y, 0);

                boolean valid = true;

                // ROI Check
                if (roiExists && !(roiBounds.contains(x, y) && roiIter.getSample(x, y, 0) > 0)) {
                    valid = false;
                }

                // NoData Check
                if (nodataExists && nodata.contains(src)) {
                    valid = false;
                }
                if (!valid) {
                    assertEquals(0d, dest, TOLERANCE);
                }
            }
        }
    }

    /**
     * Synthetic image with Short Sample Model
     * 
     * @return {@linkplain BufferedImage}
     */
    private BufferedImage getSyntheticShortImage() {
        final int width = 256;
        final int height = 256;
        final WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_USHORT,
                width, height, 1, null);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                raster.setSample(x, y, 0, (x + y));
            }
        }
        final ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                false, false, Transparency.OPAQUE, DataBuffer.TYPE_USHORT);
        final BufferedImage image = new BufferedImage(cm, raster, false, null);
        return image;
    }

    /**
     * Spearfish test-case.
     * 
     * @throws IOException
     */
    @Test
    public void testTiff() throws IOException {

        ColorCube lt = ColorCube.BYTE_496;
        KernelJAI k = KernelJAI.ERROR_FILTER_FLOYD_STEINBERG;

        final RenderedImage image = getTestTiff();
        ParameterBlockJAI pbj = new ParameterBlockJAI("ErrorDiffusion");
        pbj.addSource(image);
        pbj.setParameter("errorKernel", k);
        pbj.setParameter("colorMap", lt);
        RenderedOp finalimage = JAI.create("ErrorDiffusion", pbj);

        if (INTERACTIVE)
            RenderedImageBrowser.showChain(finalimage, false, false, null);
        else
            finalimage.getTiles();
        // Check NoData and ROI
        checkNoDataROI(finalimage, image, null, null);
        finalimage.dispose();

        // ROI creation
        ROI roi = new ROIShape(new Rectangle(image.getMinX() + 5, image.getMinY() + 5,
                image.getWidth() / 4, image.getHeight() / 4));

        Range nodata = RangeFactory.create((byte) 5, (byte) 5);

        // ROI
        pbj = new ParameterBlockJAI("ErrorDiffusion");
        pbj.addSource(image);
        pbj.setParameter("errorKernel", k);
        pbj.setParameter("colorMap", lt);
        pbj.setParameter("roi", roi);
        finalimage = JAI.create("ErrorDiffusion", pbj);

        if (INTERACTIVE)
            RenderedImageBrowser.showChain(finalimage, false, false, null);
        else
            finalimage.getTiles();
        // Check NoData and ROI
        checkNoDataROI(finalimage, image, roi, null);
        finalimage.dispose();

        // NODATA
        pbj = new ParameterBlockJAI("ErrorDiffusion");
        pbj.addSource(image);
        pbj.setParameter("errorKernel", k);
        pbj.setParameter("colorMap", lt);
        pbj.setParameter("nodata", nodata);
        finalimage = JAI.create("ErrorDiffusion", pbj);

        if (INTERACTIVE)
            RenderedImageBrowser.showChain(finalimage, false, false, null);
        else
            finalimage.getTiles();
        // Check NoData and ROI
        checkNoDataROI(finalimage, image, null, nodata);
        finalimage.dispose();

        // NODATA AND ROI
        pbj = new ParameterBlockJAI("ErrorDiffusion");
        pbj.addSource(image);
        pbj.setParameter("errorKernel", k);
        pbj.setParameter("colorMap", lt);
        pbj.setParameter("roi", roi);
        pbj.setParameter("nodata", nodata);
        finalimage = JAI.create("ErrorDiffusion", pbj);

        if (INTERACTIVE)
            RenderedImageBrowser.showChain(finalimage, false, false, null);
        else
            finalimage.getTiles();
        // Check NoData and ROI
        checkNoDataROI(finalimage, image, roi, nodata);
        finalimage.dispose();

    }

    /**
     * Building an image based on Spearfish data.
     * 
     * @return {@linkplain BufferedImage}
     * 
     * @throws IOException
     * @throws FileNotFoundException
     */
    private RenderedImage getTestTiff() throws IOException, FileNotFoundException {
        File spearfish = TestData.file(this, "test.tif");
        RenderedOp image = JAI.create("ImageRead", spearfish);
        return image;
    }

}
