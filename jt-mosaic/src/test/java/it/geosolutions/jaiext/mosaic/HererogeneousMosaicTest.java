/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
*    http://www.geo-solutions.it/
*    Copyright 2015 GeoSolutions


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
package it.geosolutions.jaiext.mosaic;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Arrays;

import javax.media.jai.InterpolationNearest;
import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.TranslateDescriptor;

import org.junit.BeforeClass;
import org.junit.Test;

import it.geosolutions.jaiext.JAIExt;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;

public class HererogeneousMosaicTest {

    @BeforeClass
    public static void setupJaiExt() {
        JAIExt.initJAIEXT();
    }

    @Test
    public void testBGRIndexed() {
        BufferedImage redIndexed = buildIndexed(Color.RED);
        RenderedOp redIndexedTranslated = TranslateDescriptor.create(redIndexed, 50f, 0f,
                new InterpolationNearest(), null);
        BufferedImage whiteAbgr = buildBGR(Color.WHITE);

        RenderedOp mosaic = MosaicDescriptor.create(
                new RenderedImage[] { whiteAbgr, redIndexedTranslated },
                javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null, null,
                null, null, null);

        checkMixedWhiteRed(mosaic);
    }

    @Test
    public void testIndexedBinary() {
        BufferedImage redIndexed = buildIndexed(Color.RED);
        RenderedOp redIndexedTranslated = TranslateDescriptor.create(redIndexed, 50f, 0f,
                new InterpolationNearest(), null);
        BufferedImage whiteBinary = buildBinary(Color.WHITE);

        RenderedOp mosaic = MosaicDescriptor.create(
                new RenderedImage[] { whiteBinary, redIndexedTranslated },
                javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null, null,
                null, null, null);

        checkMixedWhiteRed(mosaic);
    }

    @Test
    public void testBinaryBGR() {
        BufferedImage redIndexed = buildIndexed(Color.RED);
        RenderedOp redIndexedTranslated = TranslateDescriptor.create(redIndexed, 50f, 0f,
                new InterpolationNearest(), null);
        BufferedImage whiteBinary = buildBinary(Color.WHITE);

        RenderedOp mosaic = MosaicDescriptor.create(
                new RenderedImage[] { redIndexedTranslated, whiteBinary },
                javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null, null,
                null, null, null);

        checkMixedRedWhite(mosaic);
    }

    @Test
    public void testIndexedBGR() {
        BufferedImage redIndexed = buildIndexed(Color.RED);
        RenderedOp redIndexedTranslated = TranslateDescriptor.create(redIndexed, 50f, 0f,
                new InterpolationNearest(), null);
        BufferedImage whiteAbgr = buildBGR(Color.WHITE);

        RenderedOp mosaic = MosaicDescriptor.create(
                new RenderedImage[] { redIndexedTranslated, whiteAbgr },
                javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null, null,
                null, null, null);

        // RenderedImageBrowser.showChain(mosaic);

        checkMixedRedWhite(mosaic);
    }

    @Test
    public void testBinaryIndexed() {
        BufferedImage redIndexed = buildIndexed(Color.RED);
        RenderedOp redIndexedTranslated = TranslateDescriptor.create(redIndexed, 50f, 0f,
                new InterpolationNearest(), null);
        BufferedImage whiteBinary = buildBinary(Color.WHITE);

        RenderedOp mosaic = MosaicDescriptor.create(
                new RenderedImage[] { redIndexedTranslated, whiteBinary },
                javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null, null,
                null, null, null);

        // RenderedImageBrowser.showChain(mosaic);

        checkMixedRedWhite(mosaic);
    }

    @Test
    public void testRGBGray() {
        BufferedImage blueRgb = buildBGR(Color.BLUE);
        RenderedOp blueRgbTranslated = TranslateDescriptor.create(blueRgb, 50f, 0f,
                new InterpolationNearest(), null);
        BufferedImage gray = buildByteGray(Color.GRAY);

        RenderedOp mosaic = MosaicDescriptor.create(new RenderedImage[] { blueRgbTranslated, gray },
                javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null, null,
                null, null, null);

        // RenderedImageBrowser.showChain(mosaic);

        // check it's RGB
        assertRGB(mosaic);

        // check the physical extent
        assertExtent(mosaic, 0, 0, 150, 100);

        // make sure we preserved the gray
        int[] pixel = new int[3];
        mosaic.getData().getPixel(10, 10, pixel);
        assertArrayEquals(new int[] { 128, 128, 128 }, pixel);
        // verify the blue is still there
        mosaic.getData().getPixel(60, 10, pixel);
        assertArrayEquals(new int[] { 0, 0, 255 }, pixel);
    }

    @Test
    public void testIndexedGray() {
        BufferedImage blueIndexed = buildIndexed(Color.BLUE);
        RenderedOp blueIndexedTranslated = TranslateDescriptor.create(blueIndexed, 50f, 0f,
                new InterpolationNearest(), null);
        BufferedImage gray = buildByteGray(Color.GRAY);

        RenderedOp mosaic = MosaicDescriptor.create(
                new RenderedImage[] { blueIndexedTranslated, gray },
                javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null, null,
                null, null, null);

        // RenderedImageBrowser.showChain(mosaic);

        // check it's RGB
        assertRGB(mosaic);

        // check the physical extent
        assertExtent(mosaic, 0, 0, 150, 100);

        // make sure we preserved the gray
        int[] pixel = new int[3];
        mosaic.getData().getPixel(10, 10, pixel);
        assertArrayEquals(new int[] { 128, 128, 128 }, pixel);
        // verify the blue is still there
        mosaic.getData().getPixel(60, 10, pixel);
        assertArrayEquals(new int[] { 0, 0, 255 }, pixel);
    }

    @Test
    public void testGrayIndexed() {
        BufferedImage blueIndexed = buildIndexed(Color.BLUE);
        RenderedOp blueIndexedTranslated = TranslateDescriptor.create(blueIndexed, 50f, 0f,
                new InterpolationNearest(), null);
        BufferedImage gray = buildByteGray(Color.GRAY);

        RenderedOp mosaic = MosaicDescriptor.create(
                new RenderedImage[] { gray, blueIndexedTranslated },
                javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null, null,
                null, null, null);

        // RenderedImageBrowser.showChain(mosaic);

        // check it's RGB
        assertRGB(mosaic);

        // check the physical extent
        assertExtent(mosaic, 0, 0, 150, 100);

        // make sure we preserved the gray
        int[] pixel = new int[3];
        mosaic.getData().getPixel(10, 10, pixel);
        assertArrayEquals(new int[] { 128, 128, 128 }, pixel);
        // verify the blue is still there
        mosaic.getData().getPixel(120, 10, pixel);
        assertArrayEquals(new int[] { 0, 0, 255 }, pixel);
    }

    @Test
    public void testGrayRGB() {
        BufferedImage blueRgb = buildBGR(Color.BLUE);
        RenderedOp blueRgbTranslated = TranslateDescriptor.create(blueRgb, 50f, 0f,
                new InterpolationNearest(), null);
        BufferedImage gray = buildByteGray(Color.GRAY);

        RenderedOp mosaic = MosaicDescriptor.create(new RenderedImage[] { gray, blueRgbTranslated },
                javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null, null,
                null, null, null);

        // RenderedImageBrowser.showChain(mosaic);

        // check it's RGB
        assertRGB(mosaic);

        // check the physical extent
        assertExtent(mosaic, 0, 0, 150, 100);

        // make sure we preserved the gray
        int[] pixel = new int[3];
        mosaic.getData().getPixel(10, 10, pixel);
        assertArrayEquals(new int[] { 128, 128, 128 }, pixel);
        // verify the blue is still there
        mosaic.getData().getPixel(120, 10, pixel);
        assertArrayEquals(new int[] { 0, 0, 255 }, pixel);
    }

    @Test
    public void testGrayByteShort() {
        BufferedImage white = buildByteGray(Color.WHITE);
        RenderedOp whiteTranslated = TranslateDescriptor.create(white, 50f, 0f,
                new InterpolationNearest(), null);
        BufferedImage ushortGray = buildUShortGray(Color.GRAY);

        RenderedOp mosaic = MosaicDescriptor.create(
                new RenderedImage[] { whiteTranslated, ushortGray },
                javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null, null,
                null, null, null);

        // RenderedImageBrowser.showChain(mosaic);

        // check it's gray
        assertGray(mosaic, DataBuffer.TYPE_USHORT);

        // check the physical extent
        assertExtent(mosaic, 0, 0, 150, 100);

        // make sure we preserved the gray
        int[] pixel = new int[1];
        mosaic.getData().getPixel(10, 10, pixel);
        assertArrayEquals(new int[] { 32896 }, pixel);
        // verify the white is still there
        mosaic.getData().getPixel(120, 10, pixel);
        assertArrayEquals(new int[] { 65535 }, pixel);
    }

    @Test
    public void testGrayShortRGB() {
        BufferedImage gray = buildUShortGray(Color.GRAY);
        RenderedOp grayTranslated = TranslateDescriptor.create(gray, 50f, 0f,
                new InterpolationNearest(), null);
        BufferedImage blue = buildBGR(Color.BLUE);

        RenderedOp mosaic = MosaicDescriptor.create(new RenderedImage[] { grayTranslated, blue },
                javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null, null,
                null, null, null);

        // RenderedImageBrowser.showChain(mosaic);

        // check it's RGB
        assertRGB(mosaic);

        // check the physical extent
        assertExtent(mosaic, 0, 0, 150, 100);

        // make sure we preserved the blue
        int[] pixel = new int[3];
        mosaic.getData().getPixel(10, 10, pixel);
        assertArrayEquals(new int[] { 0, 0, 255 }, pixel);
        // verify the gray is still there and has been scaled down
        mosaic.getData().getPixel(120, 10, pixel);
        assertArrayEquals(new int[] { 128, 128, 128 }, pixel);
    }

    @Test
    public void testEteroPalettes() {
        BufferedImage blueIndexed = buildIndexed(Color.BLUE, Color.BLUE, Color.GREEN, Color.WHITE);
        BufferedImage whiteIndexed = buildIndexed(Color.WHITE, Color.GRAY, Color.GREEN,
                Color.WHITE);
        RenderedOp blueIndexedTranslated = TranslateDescriptor.create(blueIndexed, 50f, 0f,
                new InterpolationNearest(), null);

        RenderedOp mosaic = MosaicDescriptor.create(
                new RenderedImage[] { whiteIndexed, blueIndexedTranslated },
                javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null, null,
                null, null, null);

        // RenderedImageBrowser.showChain(mosaic);

        // check it's RGB
        assertRGB(mosaic);

        // check the physical extent
        assertExtent(mosaic, 0, 0, 150, 100);

        // make sure we preserved the white
        int[] pixel = new int[3];
        mosaic.getData().getPixel(10, 10, pixel);
        assertArrayEquals(new int[] { 255, 255, 255 }, pixel);
        // verify the blue is still there
        mosaic.getData().getPixel(120, 10, pixel);
        assertArrayEquals(new int[] { 0, 0, 255 }, pixel);
    }

    @Test
    public void testSamePalettes() {
        BufferedImage blueIndexed = buildIndexed(Color.BLUE, Color.BLUE, Color.GREEN, Color.WHITE);
        BufferedImage greenIndexed = buildIndexed(Color.GREEN, Color.BLUE, Color.GREEN,
                Color.WHITE);
        RenderedOp blueIndexedTranslated = TranslateDescriptor.create(blueIndexed, 50f, 0f,
                new InterpolationNearest(), null);

        RenderedOp mosaic = MosaicDescriptor.create(
                new RenderedImage[] { greenIndexed, blueIndexedTranslated },
                javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null, null,
                null, null, null);

        // RenderedImageBrowser.showChain(mosaic);

        // check it's paletted
        assertByteIndexed(mosaic);

        // check the physical extent
        assertExtent(mosaic, 0, 0, 150, 100);

        // make sure we preserved the white
        int[] pixel = new int[1];
        mosaic.getData().getPixel(10, 10, pixel);
        assertArrayEquals(new int[] { 1 }, pixel);
        // verify the blue is still there
        mosaic.getData().getPixel(120, 10, pixel);
        assertArrayEquals(new int[] { 0 }, pixel);
    }

    @Test
    public void testIndexedNoDataRBG() {
        // build an indexed image, Blue for the first 50 px, red for the other 50
        IndexColorModel icm = buildIndexColorModel(Color.BLUE, Color.RED);
        BufferedImage bi = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_INDEXED, icm);
        Graphics2D gr = bi.createGraphics();
        gr.setColor(Color.BLUE);
        gr.fillRect(0, 0, 50, 100);
        gr.setColor(Color.RED);
        gr.fillRect(50, 0, 50, 100);
        gr.dispose();

        // create a ROI, split vertically between black and white
        BufferedImage biROI = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_BINARY);
        gr = biROI.createGraphics();
        gr.setColor(Color.BLACK);
        gr.fillRect(0, 0, 100, 50);
        gr.setColor(Color.WHITE);
        gr.fillRect(0, 50, 100, 50);
        gr.dispose();

        // create a RGB Image, yellow
        BufferedImage yellow = buildBGR(Color.YELLOW);

        // mosaic them, with palette expansion
        RenderedOp mosaic = MosaicDescriptor.create(new RenderedImage[] { bi, yellow },
                javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null,
                new ROI[] { new ROI(biROI), null }, null, null,
                new Range[] { RangeFactory.create((byte) 0, (byte) 0), null }, null);

        // it has been expanded
        assertRGB(mosaic);

        // RenderedImageBrowser.showChain(mosaic);

        // check top left quadrant, should be yellow, we have both nodata and outside of ROI
        int[] pixel = new int[3];
        mosaic.getData().getPixel(10, 10, pixel);
        assertArrayEquals(new int[] { 255, 255, 0 }, pixel);
        // check top right quadrant, should be yellow, we have data but outside of ROI
        mosaic.getData().getPixel(75, 10, pixel);
        assertArrayEquals(new int[] { 255, 255, 0 }, pixel);
        // check bottom left quadrant, should be yellow, we have nodata even if inside the ROI
        mosaic.getData().getPixel(25, 75, pixel);
        assertArrayEquals(new int[] { 255, 255, 0 }, pixel);
        // check bottom right quadrant, should be red, we have data and it's inside the ROI
        mosaic.getData().getPixel(75, 75, pixel);
        assertArrayEquals(new int[] { 255, 0, 0 }, pixel);

    }

    @Test
    public void testTwoIndexedNoSameNoData() {
        // build an indexed image, Blue for the first 50 px, red for the other 50
        IndexColorModel icm = buildIndexColorModel(Color.BLUE, Color.RED);
        BufferedImage splitVertical = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_INDEXED,
                icm);
        Graphics2D gr = splitVertical.createGraphics();
        gr.setColor(Color.BLUE);
        gr.fillRect(0, 0, 50, 100);
        gr.setColor(Color.RED);
        gr.fillRect(50, 0, 50, 100);
        gr.dispose();

        // same as above, but split horizontally (blue on top, red bottom)
        BufferedImage splitHorizontal = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_INDEXED,
                icm);
        gr = splitHorizontal.createGraphics();
        gr.setColor(Color.BLUE);
        gr.fillRect(0, 0, 100, 50);
        gr.setColor(Color.RED);
        gr.fillRect(0, 50, 100, 50);
        gr.dispose();

        // mosaic them, the different nodata will force a palette expansion
        Range noDataBlue = RangeFactory.create((byte) 0, (byte) 0);
        Range noDataRed = RangeFactory.create((byte) 1, (byte) 1);
        RenderedOp mosaic = MosaicDescriptor.create(
                new RenderedImage[] { splitVertical, splitHorizontal },
                javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null, null,
                new double[] { 0, 0, 0 }, new Range[] { noDataBlue, noDataRed }, null);

        // it has been expanded
        assertRGB(mosaic);

        // RenderedImageBrowser.showChain(mosaic);

        // check top left quadrant, should be blue (it's blue in both)
        int[] pixel = new int[3];
        mosaic.getData().getPixel(10, 10, pixel);
        assertArrayEquals(new int[] { 0, 0, 255 }, pixel);
        // check top right quadrant, should be red (red is data in the first image)
        mosaic.getData().getPixel(75, 10, pixel);
        assertArrayEquals(new int[] { 255, 0, 0 }, pixel);
        // check bottom left quadrant, should be black, it's no data in both
        mosaic.getData().getPixel(25, 75, pixel);
        assertArrayEquals(new int[] { 0, 0, 0 }, pixel);
        // check bottom right quadrant, should be red, it's red in the first image
        mosaic.getData().getPixel(75, 75, pixel);
        assertArrayEquals(new int[] { 255, 0, 0 }, pixel);
    }

    @Test
    public void testTwoIndexedSameNoData() {
        // build an indexed image, Blue for the first 50 px, red for the other 50
        IndexColorModel icm = buildIndexColorModel(Color.BLUE, Color.RED);
        BufferedImage splitVertical = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_INDEXED,
                icm);
        Graphics2D gr = splitVertical.createGraphics();
        gr.setColor(Color.BLUE);
        gr.fillRect(0, 0, 50, 100);
        gr.setColor(Color.RED);
        gr.fillRect(50, 0, 50, 100);
        gr.dispose();

        // same as above, but split horizontally (blue on top, red bottom)
        BufferedImage splitHorizontal = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_INDEXED,
                icm);
        gr = splitHorizontal.createGraphics();
        gr.setColor(Color.BLUE);
        gr.fillRect(0, 0, 100, 50);
        gr.setColor(Color.RED);
        gr.fillRect(0, 50, 100, 50);
        gr.dispose();

        // mosaic them, with palette expansion
        Range noDataBlue = RangeFactory.create((byte) 0, (byte) 0);
        RenderedOp mosaic = MosaicDescriptor.create(
                new RenderedImage[] { splitVertical, splitHorizontal },
                javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null, null,
                null, new Range[] { noDataBlue, noDataBlue }, null);

        // it has been expanded
        assertByteIndexed(mosaic);

        // RenderedImageBrowser.showChain(mosaic);

        // check top left quadrant, should be blue (it's blue in both)
        int[] pixel = new int[1];
        mosaic.getData().getPixel(10, 10, pixel);
        assertArrayEquals(new int[] { 0 }, pixel);
        // check top right quadrant, should be red, one is red, and blue is nodata
        mosaic.getData().getPixel(75, 10, pixel);
        assertArrayEquals(new int[] { 1 }, pixel);
        // check bottom left quadrant, should be red, one is red, and blue is nodata
        mosaic.getData().getPixel(25, 75, pixel);
        assertArrayEquals(new int[] { 1 }, pixel);
        // check bottom right quadrant, should be red, one is red, and blue is nodata
        mosaic.getData().getPixel(75, 75, pixel);
        assertArrayEquals(new int[] { 1 }, pixel);

    }

    @Test
    public void testGrayExpandNoData() {
        // an 8 bit image with left half at 10, right halt at 50
        BufferedImage im8bit = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster8bit = im8bit.getRaster();
        int[] fill = new int[50 * 100];
        Arrays.fill(fill, 10);
        raster8bit.setSamples(0, 0, 50, 100, 0, fill);
        Arrays.fill(fill, 50);
        raster8bit.setSamples(50, 0, 50, 100, 0, fill);

        // and now a ushort one, top half 1000, bottom half 30000
        BufferedImage im16bit = new BufferedImage(100, 100, BufferedImage.TYPE_USHORT_GRAY);
        WritableRaster raster16bit = im16bit.getRaster();
        Arrays.fill(fill, 1000);
        raster16bit.setSamples(0, 0, 100, 50, 0, fill);
        Arrays.fill(fill, 30000);
        raster16bit.setSamples(0, 50, 100, 50, 0, fill);

        // mosaic setting the nodata
        Range noData10 = RangeFactory.create((byte) 10, (byte) 10);
        Range noData1000 = RangeFactory.create(1000, 1000);
        RenderedOp mosaic = MosaicDescriptor.create(new RenderedImage[] { im8bit, im16bit },
                javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null, null,
                new double[] { 0 }, new Range[] { noData10, noData1000 }, null);

        // RenderedImageBrowser.showChain(mosaic);

        assertGray(mosaic, DataBuffer.TYPE_USHORT);

        // check top left quadrant, should 0, the output nodata
        int[] pixel = new int[1];
        mosaic.getData().getPixel(10, 10, pixel);
        assertArrayEquals(new int[] { 0 }, pixel);
        // check top right quadrant, should be 50 expanded to ushort
        mosaic.getData().getPixel(75, 10, pixel);
        assertArrayEquals(new int[] { 12850 }, pixel);
        // check bottom left quadrant, should be 30000
        mosaic.getData().getPixel(25, 75, pixel);
        assertArrayEquals(new int[] { 30000 }, pixel);
        // check bottom right quadrant, should be 50 expanded to ushort
        mosaic.getData().getPixel(75, 75, pixel);
        assertArrayEquals(new int[] { 12850 }, pixel);
    }

    @Test
    public void testGray8BitIntoRGB() {
        // an 8 bit image with left half at 10, right half at 50
        BufferedImage im8bit = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster8bit = im8bit.getRaster();
        int[] fill = new int[50 * 100];
        Arrays.fill(fill, 10);
        raster8bit.setSamples(0, 0, 50, 100, 0, fill);
        Arrays.fill(fill, 50);
        raster8bit.setSamples(50, 0, 50, 100, 0, fill);

        BufferedImage yellow = buildBGR(Color.YELLOW);

        // mosaic setting the nodata
        Range noData10 = RangeFactory.create((byte) 10, (byte) 10);
        RenderedOp mosaic = MosaicDescriptor.create(new RenderedImage[] { im8bit, yellow },
                javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null, null,
                new double[] { 0 }, new Range[] { noData10, null }, null);

        assertRGB(mosaic);

        // check top left quadrant, should be yellow
        int[] pixel = new int[3];
        mosaic.getData().getPixel(10, 10, pixel);
        assertArrayEquals(new int[] { 255, 255, 0 }, pixel);
        // check top right quadrant, should be 50 expanded to RGB
        mosaic.getData().getPixel(75, 10, pixel);
        assertArrayEquals(new int[] { 50, 50, 50 }, pixel);
        // check bottom left quadrant, should be yellow
        mosaic.getData().getPixel(25, 75, pixel);
        assertArrayEquals(new int[] { 255, 255, 0 }, pixel);
        // check bottom right quadrant, should be 50 expanded to RGB
        mosaic.getData().getPixel(75, 75, pixel);
        assertArrayEquals(new int[] { 50, 50, 50 }, pixel);
    }

    @Test
    public void testGray16BitIntoRGB() {
        // a ushort one, top half 1000, bottom half 30000
        BufferedImage im16bit = new BufferedImage(100, 100, BufferedImage.TYPE_USHORT_GRAY);
        WritableRaster raster16bit = im16bit.getRaster();
        int[] fill = new int[50 * 100];
        Arrays.fill(fill, 1000);
        raster16bit.setSamples(0, 0, 100, 50, 0, fill);
        Arrays.fill(fill, 30000);
        raster16bit.setSamples(0, 50, 100, 50, 0, fill);

        // mosaic setting the nodata

        BufferedImage yellow = buildBGR(Color.YELLOW);

        // mosaic setting the nodata
        Range noData1000 = RangeFactory.create(1000, 1000);
        RenderedOp mosaic = MosaicDescriptor.create(new RenderedImage[] { im16bit, yellow },
                javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null, null,
                new double[] { 0 }, new Range[] { noData1000, null }, null);

        // RenderedImageBrowser.showChain(mosaic);

        assertRGB(mosaic);

        // check top left quadrant, should be yellow (nodata in the gray one)
        int[] pixel = new int[3];
        mosaic.getData().getPixel(10, 10, pixel);
        assertArrayEquals(new int[] { 255, 255, 0 }, pixel);
        // check top right quadrant, should be yellow (nodata in the gray one)
        mosaic.getData().getPixel(75, 10, pixel);
        assertArrayEquals(new int[] { 255, 255, 0 }, pixel);
        // check bottom left quadrant, should be 30000 scaled down to byte
        mosaic.getData().getPixel(25, 75, pixel);
        assertArrayEquals(new int[] { 117, 117, 117 }, pixel);
        // check bottom right quadrant, should be 30000 scaled down to byte
        mosaic.getData().getPixel(75, 75, pixel);
        assertArrayEquals(new int[] { 117, 117, 117 }, pixel);
    }

    private void assertExtent(RenderedOp mosaic, int minX, int minY, int maxX, int maxY) {
        assertEquals(minX, mosaic.getMinX());
        assertEquals(minY, mosaic.getMinY());
        assertEquals(maxX, mosaic.getMaxX());
        assertEquals(maxY, mosaic.getMaxY());
    }

    private void assertRGB(RenderedOp mosaic) {
        SampleModel sm = mosaic.getSampleModel();
        assertEquals(3, sm.getNumBands());
        assertEquals(DataBuffer.TYPE_BYTE, sm.getDataType());
        ColorModel cm = mosaic.getColorModel();
        assertEquals(3, cm.getNumColorComponents());
        assertThat(cm, instanceOf(ComponentColorModel.class));
    }

    private void assertGray(RenderedOp mosaic, int dataType) {
        SampleModel sm = mosaic.getSampleModel();
        assertEquals(1, sm.getNumBands());
        assertEquals(dataType, sm.getDataType());
        ColorModel cm = mosaic.getColorModel();
        assertThat(cm, instanceOf(ComponentColorModel.class));
    }

    private void assertByteIndexed(RenderedOp mosaic) {
        SampleModel sm = mosaic.getSampleModel();
        assertEquals(1, sm.getNumBands());
        assertEquals(DataBuffer.TYPE_BYTE, sm.getDataType());
        ColorModel cm = mosaic.getColorModel();
        assertEquals(3, cm.getNumColorComponents());
        assertThat(cm, instanceOf(IndexColorModel.class));
    }

    private void checkMixedWhiteRed(RenderedOp mosaic) {
        assertRGB(mosaic);

        assertExtent(mosaic, 0, 0, 150, 100);

        // make sure we preserved the white
        int[] pixel = new int[3];
        mosaic.getData().getPixel(10, 10, pixel);
        assertArrayEquals(new int[] { 255, 255, 255 }, pixel);
        // verify the red has been expanded
        mosaic.getData().getPixel(115, 10, pixel);
        assertArrayEquals(new int[] { 255, 0, 0 }, pixel);
    }

    private void checkMixedRedWhite(RenderedOp mosaic) {
        assertRGB(mosaic);

        assertExtent(mosaic, 0, 0, 150, 100);

        // make sure we preserved the white
        int[] pixel = new int[3];
        mosaic.getData().getPixel(10, 10, pixel);
        assertArrayEquals(new int[] { 255, 255, 255 }, pixel);
        // verify the red has been expanded
        mosaic.getData().getPixel(60, 10, pixel);
        assertArrayEquals(new int[] { 255, 0, 0 }, pixel);
    }

    private BufferedImage buildIndexed(Color color) {
        BufferedImage bi = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_INDEXED);
        Graphics2D gr = bi.createGraphics();
        gr.setColor(color);
        gr.fillRect(0, 0, 100, 100);
        gr.dispose();
        return bi;
    }

    private BufferedImage buildIndexed(Color color, Color... palette) {
        IndexColorModel icm = buildIndexColorModel(palette);
        BufferedImage bi = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_INDEXED, icm);
        Graphics2D gr = bi.createGraphics();
        gr.setColor(color);
        gr.fillRect(0, 0, 100, 100);
        gr.dispose();
        return bi;
    }

    private IndexColorModel buildIndexColorModel(Color... palette) {
        int size = palette.length;
        byte[] r = new byte[size];
        byte[] g = new byte[size];
        byte[] b = new byte[size];
        for (int i = 0; i < size; i++) {
            Color c = palette[i];
            r[i] = (byte) (c.getRed() & 0xFF);
            g[i] = (byte) (c.getGreen() & 0xFF);
            b[i] = (byte) (c.getBlue() & 0xFF);
        }
        IndexColorModel icm = new IndexColorModel(8, size, r, g, b);
        return icm;
    }

    private BufferedImage buildBinary(Color color) {
        BufferedImage bi = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D gr = bi.createGraphics();
        gr.setColor(color);
        gr.fillRect(0, 0, 100, 100);
        gr.dispose();
        return bi;
    }

    private BufferedImage buildBGR(Color color) {
        BufferedImage bi = new BufferedImage(100, 100, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D gr = bi.createGraphics();
        gr.setColor(color);
        gr.fillRect(0, 0, 100, 100);
        gr.dispose();
        return bi;
    }

    private BufferedImage buildByteGray(Color color) {
        BufferedImage bi = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D gr = bi.createGraphics();
        gr.setColor(color);
        gr.fillRect(0, 0, 100, 100);
        gr.dispose();
        return bi;
    }

    private BufferedImage buildUShortGray(Color color) {
        BufferedImage bi = new BufferedImage(100, 100, BufferedImage.TYPE_USHORT_GRAY);
        Graphics2D gr = bi.createGraphics();
        gr.setColor(color);
        gr.fillRect(0, 0, 100, 100);
        gr.dispose();
        return bi;
    }

}
