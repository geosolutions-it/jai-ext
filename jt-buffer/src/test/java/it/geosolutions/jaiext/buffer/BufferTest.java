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
package it.geosolutions.jaiext.buffer;

import it.geosolutions.jaiext.algebra.AlgebraDescriptor;
import it.geosolutions.jaiext.algebra.AlgebraDescriptor.Operator;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.stats.Statistics;
import it.geosolutions.jaiext.stats.Statistics.StatsType;
import it.geosolutions.jaiext.stats.StatisticsDescriptor;
import it.geosolutions.jaiext.testclasses.TestBase;
import it.geosolutions.rendered.viewer.RenderedImageBrowser;

import java.awt.Rectangle;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.media.jai.BorderExtender;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class for the buffer operation
 */
public class BufferTest extends TestBase {

    public static final int DEFAULT_IMG_NUM = 6;

    public static final int DEFAULT_TILE_WIDTH = 32;

    public static final int DEFAULT_TILE_HEIGHT = 32;

    private static final int INPUT = 1;

    private static final int OUTPUT = 100;

    private static final double EXPECTED_RESULT = 0.0d;

    private static final double TOLERANCE = 0.0001d;

    private static TiledImage[] images;

    private static List<ROI> rois;

    private static double noDataValue;

    private static int leftPad;

    private static int rightPad;

    private static int topPad;

    private static int bottomPad;

    private static int type;

    private static double pixelArea;

    private static BorderExtender extender;

    private static TiledImage imageFinal;

    @BeforeClass
    public static void initialSetup() {
        images = new TiledImage[DEFAULT_IMG_NUM];

        images[0] = createImage(DataBuffer.TYPE_BYTE, INPUT);
        images[1] = createImage(DataBuffer.TYPE_USHORT, INPUT);
        images[2] = createImage(DataBuffer.TYPE_SHORT, INPUT);
        images[3] = createImage(DataBuffer.TYPE_INT, INPUT);
        images[4] = createImage(DataBuffer.TYPE_FLOAT, INPUT);
        images[5] = createImage(DataBuffer.TYPE_DOUBLE, INPUT);

        imageFinal = createImage(DataBuffer.TYPE_INT, OUTPUT);

        rois = new ArrayList<ROI>(1);
        rois.add(new ROIShape(new Rectangle(10, 10, 10, 10)));

        leftPad = 10;
        rightPad = 10;
        topPad = 10;
        bottomPad = 10;

        type = DataBuffer.TYPE_INT;

        extender = BufferDescriptor.DEFAULT_EXTENDER;

        noDataValue = 0.0d;

        pixelArea = 1.0d;
    }

    @Test
    public void testImages() {
        boolean noDataUsed = false;
        testImage(images[0], noDataUsed);
        testImage(images[1], noDataUsed);
        testImage(images[2], noDataUsed);
        testImage(images[3], noDataUsed);
        testImage(images[4], noDataUsed);
        testImage(images[5], noDataUsed);
    }

    @Test
    public void testImagesNoData() {
        boolean noDataUsed = true;
        testImage(images[0], noDataUsed);
        testImage(images[1], noDataUsed);
        testImage(images[2], noDataUsed);
        testImage(images[3], noDataUsed);
        testImage(images[4], noDataUsed);
        testImage(images[5], noDataUsed);
    }

    @AfterClass
    public static void finalStuff() {
        images[0].dispose();
        images[1].dispose();
        images[2].dispose();
        images[3].dispose();
        images[4].dispose();
        images[5].dispose();
    }

    private void testImage(RenderedImage source, boolean noData) {

        Range noDataRange = null;

        int dataType = source.getSampleModel().getDataType();

        if (noData) {
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_INT:
                noDataRange = RangeFactory.create((int) noDataValue, true, (int) noDataValue, true);
                break;
            case DataBuffer.TYPE_FLOAT:
                noDataRange = RangeFactory.create((float) noDataValue, true, (float) noDataValue,
                        true, true);
                break;
            case DataBuffer.TYPE_DOUBLE:
                noDataRange = RangeFactory.create(noDataValue, true, noDataValue, true, true);
                break;
            default:
                throw new IllegalArgumentException("Wrong data type");
            }

        }

        RenderedOp dest = BufferDescriptor.create(source, extender, leftPad, rightPad, topPad,
                bottomPad, rois, noDataRange, noDataValue, null, type, pixelArea, null);

        if (INTERACTIVE && dataType == DataBuffer.TYPE_BYTE) {
            RenderedImageBrowser.showChain(dest);
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Calculations of all the values
            dest.getTiles();
        }
        // Difference between image calculated and expected
        RenderedOp difference = AlgebraDescriptor.create(Operator.SUBTRACT, null, null,
                DataBuffer.TYPE_INT, null, dest, imageFinal);
        // Calculation of the mean (it should be all zeros)
        StatsType[] stats = new StatsType[] { StatsType.MEAN };
        RenderedOp statsIMG = StatisticsDescriptor.create(difference, 1, 1, null, null, false,
                null, stats, null);
        Statistics mean = ((Statistics[][]) statsIMG.getProperty(Statistics.STATS_PROPERTY))[0][0];

        double meanValue = (Double) mean.getResult();

        Assert.assertEquals(EXPECTED_RESULT, meanValue, TOLERANCE);

        statsIMG.dispose();
        difference.dispose();
        dest.dispose();
    }

    private static TiledImage createImage(int dataType, int value) {

        SampleModel sm = new ComponentSampleModel(dataType, DEFAULT_WIDTH, DEFAULT_HEIGHT, 1,
                DEFAULT_WIDTH, new int[] { 0 });

        TiledImage image = new TiledImage(sm, DEFAULT_TILE_WIDTH, DEFAULT_TILE_HEIGHT);

        int minX = 10;
        int maxX = 20;

        int minY = 10;
        int maxY = 20;

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                image.setSample(x, y, 0, value);
            }
        }
        return image;
    }

}
