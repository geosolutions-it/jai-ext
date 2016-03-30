/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2014 - 2016 GeoSolutions


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
package it.geosolutions.jaiext.classifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import it.geosolutions.jaiext.piecewise.TransformationException;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;
import it.geosolutions.jaiext.testclasses.TestData;
import it.geosolutions.rendered.viewer.RenderedImageBrowser;

import java.awt.Color;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;
import javax.xml.crypto.dsig.TransformException;

import org.junit.Test;

/**
 * Test class for the RasterClassifier operation
 * 
 * @author Simone Giannecchini, GeoSolutions
 * @author Nicola Lagomarsini, GeoSolutions
 * 
 * @source $URL$
 */
public class RasterClassifierTest extends TestBase {

    private static final int TEST_NUM = 1;

    /**
     * Test with a synthetic image with Double Sample Model
     * 
     * @throws IOException
     */
    @Test
    public void testSyntheticDouble() throws IOException {

        // /////////////////////////////////////////////////////////////////////
        //
        // This test uses a Double datatype raster. We are also using some
        // synthetic data where there is no NoData.
        //
        // /////////////////////////////////////////////////////////////////////

        // /////////////////////////////////////////////////////////////////////
        //
        // Set the pixel values. Because we use only one tile with one band,
        // the
        // code below is pretty similar to the code we would have if we were
        // just setting the values in a matrix.
        //
        // /////////////////////////////////////////////////////////////////////
        final BufferedImage image = getSyntheticDoubleImage();
        for (int i = 0; i < TEST_NUM; i++) {
            // /////////////////////////////////////////////////////////////////////
            //
            // Build the categories
            //
            // /////////////////////////////////////////////////////////////////////
            LinearColorMap list = buildCategories();

            // Operation creation
            final ParameterBlockJAI pbj = new ParameterBlockJAI(
                    RasterClassifierOpImage.OPERATION_NAME);
            pbj.addSource(image);
            pbj.setParameter("Domain1D", list);
            final RenderedOp finalimage = JAI.create(RasterClassifierOpImage.OPERATION_NAME, pbj);

            if (INTERACTIVE)
                RenderedImageBrowser.showChain(finalimage, false, false, null);
            else
                finalimage.getTiles();
            finalimage.dispose();
        }
    }

    @Test
    public void testHugeDataset() throws IOException {
        final int width = 20000;
        final int height = 20000;
        ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), false, false, Transparency.OPAQUE, DataBuffer.TYPE_FLOAT);
        SampleModel sm = cm.createCompatibleSampleModel(512, 512);
        PlanarImage image = new TiledImage(0, 0, width, height, 0, 0, sm, cm);
        LinearColorMap list = buildCategories();
        final ParameterBlockJAI pbj = new ParameterBlockJAI(RasterClassifierOpImage.OPERATION_NAME);
        pbj.addSource(image);
        pbj.setParameter("Domain1D", list);
        final RenderedOp finalImage = JAI.create(RasterClassifierOpImage.OPERATION_NAME, pbj);
        boolean success = true;
        try {
            
            Raster raster = finalImage.getTile(0, 0);
            assertNotNull(raster);
        } catch (RuntimeException ie) {
            // Before the getData fix the getTiles call was throwing an Exception
            // due to OOM on allocating that big raster
            success = false;
        }
        assertTrue(success);
    }

    /**
     * Synthetic Image with Double Sample Model
     * 
     * @return {@linkplain BufferedImage}
     */
    private BufferedImage getSyntheticDoubleImage() {
        final int width = 500;
        final int height = 500;
        // Create the raster
        final WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_DOUBLE,
                width, height, 1, null);
        // Define the elements
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                raster.setSample(x, y, 0, (x + y));
            }
        }
        // Define the colormodel
        final ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                false, false, Transparency.OPAQUE, DataBuffer.TYPE_DOUBLE);
        // Create the image
        final BufferedImage image = new BufferedImage(cm, raster, false, null);
        return image;
    }

    /**
     * Test with a synthetic image with Float Sample Model
     * 
     * @throws IOException
     */
    @Test
    public void testSyntheticFloat() throws IOException {

        // /////////////////////////////////////////////////////////////////////
        //
        // This test uses a Float datatype raster. We are also using some
        // synthetic data where there is no NoData.
        //
        // /////////////////////////////////////////////////////////////////////

        // /////////////////////////////////////////////////////////////////////
        //
        // Set the pixel values. Because we use only one tile with one band,
        // the
        // code below is pretty similar to the code we would have if we were
        // just setting the values in a matrix.
        //
        // /////////////////////////////////////////////////////////////////////
        final BufferedImage image = getSyntheticFloatImage();
        for (int i = 0; i < TEST_NUM; i++) {
            // /////////////////////////////////////////////////////////////////////
            //
            // Build the categories
            //
            // /////////////////////////////////////////////////////////////////////
            LinearColorMap list = buildCategories();

            final ParameterBlockJAI pbj = new ParameterBlockJAI(
                    RasterClassifierOpImage.OPERATION_NAME);
            pbj.addSource(image);
            pbj.setParameter("Domain1D", list);
            final RenderedOp finalimage = JAI.create(RasterClassifierOpImage.OPERATION_NAME, pbj);

            if (INTERACTIVE)
                RenderedImageBrowser.showChain(finalimage, false, false, null);
            else
                finalimage.getTiles();
            finalimage.dispose();
        }
    }

    private LinearColorMap buildCategories() {
        final LinearColorMapElement c0 = LinearColorMapElement.create("c0", Color.BLACK,
                RangeFactory.create(Double.NEGATIVE_INFINITY, false, 10, true), 0);

        final LinearColorMapElement c1 = LinearColorMapElement.create("c2", Color.blue,
                RangeFactory.create(10.0f, false, 100.0f, true), 1);

        final LinearColorMapElement c3 = LinearColorMapElement.create("c3", Color.green,
                RangeFactory.create(100.0f, false, 300.0f, true), 2);

        final LinearColorMapElement c4 = LinearColorMapElement.create("c4", new Color[] {
                Color.green, Color.red }, RangeFactory.create(300.0f, false, 400.0f, true),
                RangeFactory.create(3, 1000));

        final LinearColorMapElement c5 = LinearColorMapElement.create("c5", new Color[] {
                Color.red, Color.white }, RangeFactory.create(400.0f, false, 1000.0f, true),
                RangeFactory.create(1001, 2000));

        final LinearColorMapElement c6 = LinearColorMapElement.create("c6", Color.red, 1001.0f,
                2001);

        final LinearColorMapElement c7 = LinearColorMapElement.create("nodata", new Color(0, 0,
                0, 0), RangeFactory.create(Double.NaN, Double.NaN), 2201);

        final LinearColorMap list = new LinearColorMap("", new LinearColorMapElement[] { c0,
                c1, c3, c4, c5, c6 }, new LinearColorMapElement[] { c7 });
        return list;
    }

    /**
     * Building a synthetic image upon a float Sample Model.
     * 
     * @return {@linkplain BufferedImage}
     */
    private BufferedImage getSyntheticFloatImage() {
        final int width = 500;
        final int height = 500;
        // Define the Raster
        final WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT,
                width, height, 1, null);
        // Populate raster
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                raster.setSample(x, y, 0, (x + y));
            }
        }
        // Define the colormodel
        final ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                false, false, Transparency.OPAQUE, DataBuffer.TYPE_FLOAT);
        // Create the image
        final BufferedImage image = new BufferedImage(cm, raster, false, null);
        return image;
    }

    /**
     * NoData only test-case.
     * 
     * @throws IOException
     * @throws TransformException
     */
    @Test
    public void testNoDataOnly() throws IOException, TransformationException {

        // /////////////////////////////////////////////////////////////////////
        //
        // We are covering here a case that can often be verified, i.e. the case
        // when only NoData values are known and thus explicitly mapped by the
        // user to a defined nodata DomainElement, but not the same for the
        // others.
        // In such case we want CategoryLists automatically map unknown data to
        // a Passthrough DomainElement, which identically maps raster data to
        // category
        // data.
        //
        // /////////////////////////////////////////////////////////////////////

        for (int i = 0; i < TEST_NUM; i++) {

            final LinearColorMapElement n0 = LinearColorMapElement.create("nodata", new Color(0, 0,
                    0, 0), RangeFactory.create(Double.NaN, Double.NaN), 9999);

            final LinearColorMap list = new LinearColorMap("", new LinearColorMapElement[] { n0 });

            double testNum = Math.random();
            boolean exceptionThrown = false;
            try {
                assertEquals(list.transform(testNum), testNum, 0.0);
            } catch (Exception e) {
                exceptionThrown = true;
            }
            assertTrue(exceptionThrown);
            assertEquals(list.transform(Double.NaN), 9999, 0.0);
        }
    }

    /**
     * Spearfish test-case.
     * 
     * @throws IOException
     */
    @Test
    public void testSpearfish() throws IOException {

        // /////////////////////////////////////////////////////////////////////
        //
        // This test is quite standard since the NoData category specified
        // is for NoData values since the input file is a GRASS file
        // where the missing values are represented by 255. The only strange thing
        // that we try here is that we map two different classes to the same
        // color with the same index.
        //
        // /////////////////////////////////////////////////////////////////////
        final RenderedImage image = getSpearfhisDemo();

        for (int i = 0; i < TEST_NUM; i++) {
            final LinearColorMapElement c0 = LinearColorMapElement.create("c0", Color.yellow,
                    RangeFactory.create(0, true, 11, true), 5);

            final LinearColorMapElement c1 = LinearColorMapElement.create("c2", Color.blue,
                    RangeFactory.create(11, false, 12, true), 1);

            final LinearColorMapElement c3 = LinearColorMapElement.create("c3", Color.green,
                    RangeFactory.create(12, false, 14, true), 7);

            final LinearColorMapElement c4 = LinearColorMapElement.create("c4", Color.blue,
                    RangeFactory.create(14, false, 16, true), 1);

            final LinearColorMapElement c5 = LinearColorMapElement.create("c4", Color.CYAN,
                    RangeFactory.create(16, false, 255, false), 11);

            final LinearColorMapElement c6 = LinearColorMapElement.create("nodata", new Color(0, 0,
                    0, 0), RangeFactory.create(255, 255), 0);

            final LinearColorMap list = new LinearColorMap("", new LinearColorMapElement[] { c0,
                    c1, c3, c4, c5 }, new LinearColorMapElement[] { c6 });

            final ParameterBlockJAI pbj = new ParameterBlockJAI(
                    RasterClassifierOpImage.OPERATION_NAME);
            pbj.addSource(image);
            pbj.setParameter("Domain1D", list);
            final RenderedOp finalimage = JAI.create(RasterClassifierOpImage.OPERATION_NAME, pbj);

            if (INTERACTIVE)
                RenderedImageBrowser.showChain(finalimage, false, false, null);
            else
                finalimage.getTiles();
            finalimage.dispose();
        }
    }

    /**
     * Building an image based on Spearfish data.
     * 
     * @return {@linkplain BufferedImage}
     * 
     * @throws IOException
     * @throws FileNotFoundException
     */
    private RenderedImage getSpearfhisDemo() throws IOException, FileNotFoundException {
        File spearfish = TestData.file(this, "spearfish.png");
        RenderedOp image = JAI.create("ImageRead", spearfish);
        return image;
    }

    /**
     * SWAN test-case. We generate an image similar to the SWAN dataset.
     * 
     * @throws IOException
     */
    @Test
    public void testSWAN() throws IOException {
        // /////////////////////////////////////////////////////////////////////
        //
        // This test is interesting since it can be used to simulate the
        // case where someone specifies a ColorMap that overlaps with the native
        // NoData value.
        //
        // /////////////////////////////////////////////////////////////////////
        final RenderedImage image = getSWANData();

        for (int i = 0; i < TEST_NUM; i++) {
            final LinearColorMapElement c0 = LinearColorMapElement.create("c0", Color.green,
                    RangeFactory.create(Double.NEGATIVE_INFINITY, 0.3), 51);

            final LinearColorMapElement c1 = LinearColorMapElement.create("c2", Color.yellow,
                    RangeFactory.create(0.3, false, 0.6, true), 1);

            final LinearColorMapElement c1b = LinearColorMapElement.create("c2", Color.BLACK,
                    RangeFactory.create(0.3, false, 0.6, true), 1);
            final LinearColorMapElement c1c = LinearColorMapElement.create("c2", Color.yellow,
                    RangeFactory.create(0.3, false, 0.6, true), 1);
            assertFalse(c1.equals(c1b));
            assertTrue(c1.equals(c1c));

            final LinearColorMapElement c3 = LinearColorMapElement.create("c3", Color.red,
                    RangeFactory.create(0.60, false, 0.90, true), 2);

            final LinearColorMapElement c4 = LinearColorMapElement.create("c4", Color.BLUE,
                    RangeFactory.create(0.9, false, Double.POSITIVE_INFINITY, true), 3);

            final LinearColorMapElement nodata = LinearColorMapElement.create("nodata", new Color(
                    0, 0, 0, 0), RangeFactory.create(-9.0, -9.0), 4);

            final LinearColorMap list = new LinearColorMap("testSWAN", new LinearColorMapElement[] {
                    c0, c1, c3, c4 }, new LinearColorMapElement[] { nodata }, new Color(0, 0, 0));

            assertEquals(list.getSourceDimensions(), 1);
            assertEquals(list.getTargetDimensions(), 1);
            assertEquals(list.getName().toString(), "testSWAN");
            assertNotNull(c0.toString());

            final ParameterBlockJAI pbj = new ParameterBlockJAI(
                    RasterClassifierOpImage.OPERATION_NAME);
            pbj.addSource(image);
            pbj.setParameter("Domain1D", list);

            boolean exceptionThrown = false;
            try {
                // //
                // forcing a bad band selection ...
                // //
                pbj.setParameter("bandIndex", new Integer(2));
                final RenderedOp d = JAI.create(RasterClassifierOpImage.OPERATION_NAME, pbj);
                d.getTiles();
                // we should not be here!
            } catch (Exception e) {
                exceptionThrown = true;
                // //
                // ... ok, Exception wanted!
                // //
            }
            assertTrue(exceptionThrown);

            pbj.setParameter("bandIndex", new Integer(0));
            final RenderedOp finalimage = JAI.create(RasterClassifierOpImage.OPERATION_NAME, pbj);
            if (INTERACTIVE)
                RenderedImageBrowser.showChain(finalimage, false, false, null);
            else
                finalimage.getTiles();
            finalimage.dispose();
        }
    }

    /**
     * SWAN test-case. We generate an image similar to the SWAN dataset.
     * 
     * @throws IOException
     */
    @Test
    public void testSWANwithGap() throws IOException {

        // /////////////////////////////////////////////////////////////////////
        //
        // This test is interesting since it can be used to simulate the
        // case where someone specifies a ColorMap that overlaps with the native
        // NoData value. For this SWAN data the NoData value is -9.0 and no
        // NaN which falls right into the first category.
        //
        // We overcome this problem by simply giving higher priority to the
        // NoData category over the other categories when doing a search for
        // the right category given a certain value. This force us to
        // first evaluate the no data category and then evaluate a possible
        // provided overlapping value.
        //
        // This test is also interesting since we create a color map by
        // providing output indexes that are not ordered and also that are not
        // all contained in a closed natural interval. As you can notice by
        // inspecting the different classes below there is an index, 51, which
        // is way outside the range of the others.
        //
        // /////////////////////////////////////////////////////////////////////
        final RenderedImage image = getSWANData();

        for (int i = 0; i < TEST_NUM; i++) {
            final LinearColorMapElement c0 = LinearColorMapElement.create("c0", Color.green,
                    RangeFactory.create(Double.NEGATIVE_INFINITY, 0.3), 51);

            final LinearColorMapElement c1 = LinearColorMapElement.create("c2", Color.yellow,
                    RangeFactory.create(0.3, false, 0.6, true), 1);

            final LinearColorMapElement c3 = LinearColorMapElement.create("c3", Color.red,
                    RangeFactory.create(0.70, false, 0.90, true), 2);

            final LinearColorMapElement c4 = LinearColorMapElement.create("c4", Color.BLUE,
                    RangeFactory.create(0.9, false, Double.POSITIVE_INFINITY, true), 3);

            final LinearColorMapElement nodata = LinearColorMapElement.create("nodata", Color.red,
                    RangeFactory.create(-9.0, -9.0), 4);

            final LinearColorMap list = new LinearColorMap("testSWAN", new LinearColorMapElement[] {
                    c0, c1, c3, c4 }, new LinearColorMapElement[] { nodata }, new Color(0, 0, 0, 0));

            final ParameterBlockJAI pbj = new ParameterBlockJAI(
                    RasterClassifierOpImage.OPERATION_NAME);
            pbj.addSource(image);
            pbj.setParameter("Domain1D", list);

            try {
                // //
                // forcing a bad band selection ...
                // //
                pbj.setParameter("bandIndex", new Integer(2));
                final RenderedOp d = JAI.create(RasterClassifierOpImage.OPERATION_NAME, pbj);
                d.getTiles();
                // we should not be here!
                assertTrue(false);
            } catch (Exception e) {
                // //
                // ... ok, Exception wanted!
                // //
            }

            pbj.setParameter("bandIndex", new Integer(0));
            final RenderedOp finalimage = JAI.create(RasterClassifierOpImage.OPERATION_NAME, pbj);
            final IndexColorModel icm = (IndexColorModel) finalimage.getColorModel();
            assertEquals(icm.getRed(4), 255);
            assertEquals(icm.getRed(2), 255);

            if (INTERACTIVE)
                RenderedImageBrowser.showChain(finalimage, false, false, null);
            else
                finalimage.getTiles();
            finalimage.dispose();
        }
    }

    /**
     * Building an image simulating SWAN data.
     * 
     * @return {@linkplain BufferedImage}
     * 
     */
    private RenderedImage getSWANData() {
        final int width = 500;
        final int height = 500;
        // Build the raster
        final WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_DOUBLE,
                width, height, 1, null);
        // Populate the raster
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x == y || x == -y) {
                    raster.setSample(x, y, 0, -9.0);
                } else {
                    raster.setSample(x, y, 0, Math.random() * 5 - 5);
                }
            }
        }
        // Define the colormodel
        final ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                false, false, Transparency.OPAQUE, DataBuffer.TYPE_DOUBLE);
        // Create the image
        final BufferedImage image = new BufferedImage(cm, raster, false, null);
        return image;
    }
}
