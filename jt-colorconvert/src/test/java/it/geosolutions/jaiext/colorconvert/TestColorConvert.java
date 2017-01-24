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
package it.geosolutions.jaiext.colorconvert;

import static org.junit.Assert.assertEquals;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
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

import javax.media.jai.ColorSpaceJAI;
import javax.media.jai.IHSColorSpace;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RasterFactory;
import javax.media.jai.RenderedOp;

import it.geosolutions.jaiext.utilities.ImageLayout2;
import org.junit.Test;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;
import it.geosolutions.jaiext.testclasses.TestData;
import it.geosolutions.rendered.viewer.RenderedImageBrowser;

/**
 * Test class for the ColorConvert operation
 * 
 * @author Nicola Lagomarsini, GeoSolutions
 * 
 * 
 * @source $URL$
 */
public class TestColorConvert extends TestBase {

    /**
     * Synthetic with Short Sample Model
     * 
     * @throws IOException
     */
    @Test
    public void testSyntheticShort() throws IOException {

        // /////////////////////////////////////////////////////////////////////
        //
        // This test uses a Ushort Image datatype.
        //
        // /////////////////////////////////////////////////////////////////////
        final BufferedImage image = getSyntheticShortImage();

        ParameterBlockJAI pbj = new ParameterBlockJAI("ColorConvert");
        pbj.addSource(image);
        pbj.setParameter("colorModel", getJAICm());
        RenderedOp finalimage = JAI.create("ColorConvert", pbj);

        if (INTERACTIVE)
            RenderedImageBrowser.showChain(finalimage, false, false, null);
        else
            finalimage.getTiles();
        finalimage.dispose();

        // ROI creation
        ROI roi = new ROIShape(new Rectangle(image.getMinX() + 5, image.getMinY() + 5,
                image.getWidth() / 4, image.getHeight() / 4));

        Range nodata = RangeFactory.create((short) 5, (short) 5);

        // ROI
        pbj = new ParameterBlockJAI("ColorConvert");
        pbj.addSource(image);
        pbj.setParameter("colorModel", getJAICm());
        pbj.setParameter("roi", roi);
        finalimage = JAI.create("ColorConvert", pbj);

        if (INTERACTIVE)
            RenderedImageBrowser.showChain(finalimage, false, false, null);
        else
            finalimage.getTiles();
        finalimage.dispose();

        // NODATA
        pbj = new ParameterBlockJAI("ColorConvert");
        pbj.addSource(image);
        pbj.setParameter("colorModel", getJAICm());
        pbj.setParameter("nodata", nodata);
        finalimage = JAI.create("ColorConvert", pbj);

        if (INTERACTIVE)
            RenderedImageBrowser.showChain(finalimage, false, false, null);
        else
            finalimage.getTiles();
        finalimage.dispose();

        // NODATA AND ROI
        pbj = new ParameterBlockJAI("ColorConvert");
        pbj.addSource(image);
        pbj.setParameter("colorModel", getJAICm());
        pbj.setParameter("roi", roi);
        pbj.setParameter("nodata", nodata);
        finalimage = JAI.create("ColorConvert", pbj);

        if (INTERACTIVE)
            RenderedImageBrowser.showChain(finalimage, false, false, null);
        else
            finalimage.getTiles();
        finalimage.dispose();
    }

    /**
     * Synthetic with Float Sample Model!
     * 
     * @return {@linkplain BufferedImage}
     */
    private BufferedImage getSyntheticShortImage() {
        final int width = 500;
        final int height = 500;
        final WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_USHORT,
                width, height, 3, null);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                raster.setSample(x, y, 0, (x + y));
            }
        }
        final ColorModel cm = new ComponentColorModel(
                ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB), false, false,
                Transparency.OPAQUE, DataBuffer.TYPE_USHORT);
        final BufferedImage image = new BufferedImage(cm, raster, false, null);
        return image;
    }

    /**
     * Building a synthetic image upon a Byte sample-model.
     * 
     * @return {@linkplain BufferedImage}
     * @throws IOException
     */
    @Test
    public void testSyntheticByte() throws IOException {

        // /////////////////////////////////////////////////////////////////////
        //
        // This test uses a Byte Image datatype.
        //
        // /////////////////////////////////////////////////////////////////////

        BufferedImage image = getSyntheticByteImage();

        ParameterBlockJAI pbj = new ParameterBlockJAI("ColorConvert");
        pbj.addSource(image);
        pbj.setParameter("colorModel", getJAIEXTCm());
        RenderedOp finalimage = JAI.create("ColorConvert", pbj);

        if (INTERACTIVE)
            RenderedImageBrowser.showChain(finalimage, false, false, null);
        else
            finalimage.getTiles();
        finalimage.dispose();

        // ROI creation
        ROI roi = new ROIShape(new Rectangle(image.getMinX() + 5, image.getMinY() + 5,
                image.getWidth() / 4, image.getHeight() / 4));

        Range nodata = RangeFactory.create((byte) 5, (byte) 5);

        // ROI
        pbj = new ParameterBlockJAI("ColorConvert");
        pbj.addSource(image);
        pbj.setParameter("colorModel", getJAIEXTCm());
        pbj.setParameter("roi", roi);
        finalimage = JAI.create("ColorConvert", pbj);

        if (INTERACTIVE)
            RenderedImageBrowser.showChain(finalimage, false, false, null);
        else
            finalimage.getTiles();
        finalimage.dispose();

        // NODATA
        pbj = new ParameterBlockJAI("ColorConvert");
        pbj.addSource(image);
        pbj.setParameter("colorModel", getJAIEXTCm());
        pbj.setParameter("nodata", nodata);
        finalimage = JAI.create("ColorConvert", pbj);

        if (INTERACTIVE)
            RenderedImageBrowser.showChain(finalimage, false, false, null);
        else
            finalimage.getTiles();
        finalimage.dispose();

        // NODATA AND ROI
        pbj = new ParameterBlockJAI("ColorConvert");
        pbj.addSource(image);
        pbj.setParameter("colorModel", getJAIEXTCm());
        pbj.setParameter("roi", roi);
        pbj.setParameter("nodata", nodata);
        finalimage = JAI.create("ColorConvert", pbj);

        if (INTERACTIVE)
            RenderedImageBrowser.showChain(finalimage, false, false, null);
        else
            finalimage.getTiles();
        finalimage.dispose();
    }

    /**
     * Building a synthetic image upon a byte sample-model.
     * 
     * @return {@linkplain BufferedImage}
     * @throws IOException
     */
    @Test
    public void testSyntheticByteIHSColorSpace() throws IOException {

        // /////////////////////////////////////////////////////////////////////
        //
        // This test uses a Byte Image datatype. Input ColorSpace is IHS.
        //
        // /////////////////////////////////////////////////////////////////////

        final BufferedImage image = getSyntheticByteImageIHSColorSpace();

        ParameterBlockJAI pbj = new ParameterBlockJAI("ColorConvert");
        pbj.addSource(image);
        pbj.setParameter("colorModel", getNotJAICm());
        RenderedOp finalimage = JAI.create("ColorConvert", pbj);

        if (INTERACTIVE)
            RenderedImageBrowser.showChain(finalimage, false, false, null);
        else
            finalimage.getTiles();
        finalimage.dispose();

        // ROI creation
        ROI roi = new ROIShape(new Rectangle(image.getMinX() + 5, image.getMinY() + 5,
                image.getWidth() / 4, image.getHeight() / 4));

        Range nodata = RangeFactory.create((byte) 5, (byte) 5);

        // ROI
        pbj = new ParameterBlockJAI("ColorConvert");
        pbj.addSource(image);
        pbj.setParameter("colorModel", getNotJAICm());
        pbj.setParameter("roi", roi);
        finalimage = JAI.create("ColorConvert", pbj);

        if (INTERACTIVE)
            RenderedImageBrowser.showChain(finalimage, false, false, null);
        else
            finalimage.getTiles();
        finalimage.dispose();

        // NODATA
        pbj = new ParameterBlockJAI("ColorConvert");
        pbj.addSource(image);
        pbj.setParameter("colorModel", getNotJAICm());
        pbj.setParameter("nodata", nodata);
        finalimage = JAI.create("ColorConvert", pbj);

        if (INTERACTIVE)
            RenderedImageBrowser.showChain(finalimage, false, false, null);
        else
            finalimage.getTiles();
        finalimage.dispose();

        // NODATA AND ROI
        pbj = new ParameterBlockJAI("ColorConvert");
        pbj.addSource(image);
        pbj.setParameter("colorModel", getNotJAICm());
        pbj.setParameter("roi", roi);
        pbj.setParameter("nodata", nodata);
        finalimage = JAI.create("ColorConvert", pbj);

        if (INTERACTIVE)
            RenderedImageBrowser.showChain(finalimage, false, false, null);
        else
            finalimage.getTiles();
        finalimage.dispose();
    }

    /**
     * Building a synthetic image upon a FLOAT sample-model.
     * 
     * @return {@linkplain BufferedImage}
     */
    private BufferedImage getSyntheticByteImage() {
        final int width = 500;
        final int height = 500;
        final WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_BYTE, width,
                height, 3, null);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                raster.setSample(x, y, 0, (x + y));
            }
        }
        final ColorModel cm = new ComponentColorModel(
                ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB), false, false,
                Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        final BufferedImage image = new BufferedImage(cm, raster, false, null);
        return image;
    }

    /**
     * Building a synthetic image upon a FLOAT sample-model.
     * 
     * @return {@linkplain BufferedImage}
     */
    private BufferedImage getSyntheticByteImageIHSColorSpace() {
        final int width = 500;
        final int height = 500;
        final WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_BYTE, width,
                height, 3, null);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                raster.setSample(x, y, 0, (x + y));
            }
        }
        final ColorModel cm = new ComponentColorModel(new IHSColorSpaceJAIExt(), false, false,
                Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        final BufferedImage image = new BufferedImage(cm, raster, false, null);
        return image;
    }

    /**
     * Tiff image test-case.
     * 
     * @throws IOException
     */
    @Test
    public void testTiff() throws IOException {

        final RenderedImage image = getTestTiff();
        ParameterBlockJAI pbj = new ParameterBlockJAI("ColorConvert");
        pbj.addSource(image);
        pbj.setParameter("colorModel", getNotJAICm());
        RenderedOp finalimage = JAI.create("ColorConvert", pbj);

        if (INTERACTIVE)
            RenderedImageBrowser.showChain(finalimage, false, false, null);
        else
            finalimage.getTiles();
        finalimage.dispose();

        // ROI creation
        ROI roi = new ROIShape(new Rectangle(image.getMinX() + 5, image.getMinY() + 5,
                image.getWidth() / 4, image.getHeight() / 4));

        Range nodata = RangeFactory.create((byte) 5, (byte) 5);

        // ROI
        pbj = new ParameterBlockJAI("ColorConvert");
        pbj.addSource(image);
        pbj.setParameter("colorModel", getNotJAICm());
        pbj.setParameter("roi", roi);
        finalimage = JAI.create("ColorConvert", pbj);

        if (INTERACTIVE)
            RenderedImageBrowser.showChain(finalimage, false, false, null);
        else
            finalimage.getTiles();
        finalimage.dispose();

        // NODATA
        pbj = new ParameterBlockJAI("ColorConvert");
        pbj.addSource(image);
        pbj.setParameter("colorModel", getNotJAICm());
        pbj.setParameter("nodata", nodata);
        finalimage = JAI.create("ColorConvert", pbj);

        if (INTERACTIVE)
            RenderedImageBrowser.showChain(finalimage, false, false, null);
        else
            finalimage.getTiles();
        finalimage.dispose();

        // NODATA AND ROI
        pbj = new ParameterBlockJAI("ColorConvert");
        pbj.addSource(image);
        pbj.setParameter("colorModel", getNotJAICm());
        pbj.setParameter("roi", roi);
        pbj.setParameter("nodata", nodata);
        finalimage = JAI.create("ColorConvert", pbj);

        if (INTERACTIVE)
            RenderedImageBrowser.showChain(finalimage, false, false, null);
        else
            finalimage.getTiles();
        finalimage.dispose();
    }

    @Test
    public void testExpandGrayCaseC() {
        // create gray indexed image
        BufferedImage bi = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D gr = bi.createGraphics();
        gr.setColor(Color.GRAY);
        gr.fillRect(0, 0, 10, 10);
        gr.dispose();

        // create a RGB color model
        final ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                false, false, Transparency.OPAQUE, bi.getSampleModel().getDataType());

        // set the destination image layout
        final ImageLayout2 il = new ImageLayout2(bi);
        il.setColorModel(cm);
        il.setSampleModel(cm.createCompatibleSampleModel(bi.getWidth(), bi.getHeight()));
        RenderingHints ri = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, il);

        // perform color expansion
        ParameterBlockJAI pbj = new ParameterBlockJAI("ColorConvert");
        pbj.addSource(bi);
        pbj.setParameter("colorModel", cm);
        pbj.setParameter("noData", RangeFactory.create(-1, -1));
        RenderedOp finalimage = JAI.create("ColorConvert", pbj, ri);

        int[] pixel = new int[3];
        finalimage.getData().getPixel(0, 0, pixel);
        assertEquals(128, pixel[0]);
        assertEquals(128, pixel[1]);
        assertEquals(128, pixel[2]);
    }

    /**
     * Reading an image based on Spearfish data.
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

    /**
     * @return a ColorModel which contains an instance of {@link ColorSpaceJAIExt}
     */
    private ColorModel getJAIEXTCm() {
        ColorSpace cs = new IHSColorSpaceJAIExt();

        final ColorModel cm = new ComponentColorModel(cs, false, false, Transparency.OPAQUE,
                DataBuffer.TYPE_BYTE);

        return cm;
    }

    /**
     * @return a ColorModel which contains an instance of {@link ColorSpaceJAI}
     */
    private ColorModel getJAICm() {
        ColorSpace cs = IHSColorSpace.getInstance();

        final ColorModel cm = new ComponentColorModel(cs, false, false, Transparency.OPAQUE,
                DataBuffer.TYPE_USHORT);

        return cm;
    }

    /**
     * @return a ColorModel which contains an instance of {@link ColorSpace} different from {@link ColorSpaceJAIExt} and {@link ColorSpaceJAI}
     */
    private ColorModel getNotJAICm() {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);

        final ColorModel cm = new ComponentColorModel(cs, false, false, Transparency.OPAQUE,
                DataBuffer.TYPE_BYTE);
        return cm;
    }
}
