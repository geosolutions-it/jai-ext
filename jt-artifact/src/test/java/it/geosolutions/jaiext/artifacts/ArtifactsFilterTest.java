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
package it.geosolutions.jaiext.artifacts;

import static org.junit.Assert.assertEquals;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.stats.Statistics;
import it.geosolutions.jaiext.stats.Statistics.StatsType;
import it.geosolutions.jaiext.stats.StatisticsDescriptor;
import it.geosolutions.jaiext.testclasses.TestBase;
import it.geosolutions.jaiext.testclasses.TestData;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.media.jai.JAI;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.FormatDescriptor;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class for the ArtifactsFilter operation
 * 
 * @source $URL$
 */
public class ArtifactsFilterTest extends TestBase {

    private static RenderedOp image;

    private static Range[] nodata;

    @BeforeClass
    public static void setupData() throws FileNotFoundException, IOException {
        image = JAI.create("ImageRead", TestData.file(ArtifactsFilterTest.class, "filter.tif"));

        // Creation of a NoData Array
        nodata = new Range[6];
        nodata[0] = RangeFactory.create((byte) 200, (byte) 200);
        nodata[1] = RangeFactory.createU((short) 200, (short) 200);
        nodata[2] = RangeFactory.create((short) 200, (short) 200);
        nodata[3] = RangeFactory.create(200, 200);
        nodata[4] = RangeFactory.create(200f, 200f);
        nodata[5] = RangeFactory.create(200d, 200d);

    }

    @Test
    public void testValidData() {
        for (int i = 0; i < 6; i++) {
            if (i == 2) {
                // No ColorModel for Short data
                continue;
            }
            testArtifact(i, image, null);
        }
    }

    @Test
    public void testNoData() {
        for (int i = 0; i < 6; i++) {
            if (i == 2) {
                // No ColorModel for Short data
                continue;
            }
            testArtifact(i, image, nodata[i]);
        }
    }

    private void testArtifact(int dataType, RenderedImage image, Range nodata) {
        image.getWidth();
        image = FormatDescriptor.create(image, dataType, null);
        StatsType[] stats = new StatsType[] { StatsType.HISTOGRAM };
        RenderedOp histogramOp = StatisticsDescriptor.create(image, 1, 1, null, null, false,
                new int[] { 0, 1, 2 }, stats, new double[] { 0 }, new double[] { 256 },
                new int[] { 256 }, null);
        int[][] bins = new int[3][256];
        Statistics[][] results = (Statistics[][]) histogramOp
                .getProperty(Statistics.STATS_PROPERTY);

        for (int i = 0; i < 3; i++) {
            double[] res = (double[]) results[i][0].getResult();
            for (int k = 0; k < 256; k++) {
                bins[i][k] = (int) res[k];
            }
        }

        assertEquals(bins[0][0], 4261);
        assertEquals(bins[1][0], 4261);
        assertEquals(bins[2][0], 4832);
        assertEquals(bins[0][20], 127); // This bin will disappear in the Histogram of the filtered image
        assertEquals(bins[1][20], 127); // This bin will disappear in the Histogram of the filtered image
        assertEquals(bins[2][20], 127); // This bin will disappear in the Histogram of the filtered image
        assertEquals(bins[0][180], 571);
        assertEquals(bins[0][200], 5041); // This bin will disappear in the Histogram of the filtered image (NoData check)
        assertEquals(bins[2][200], 5041); // This bin will disappear in the Histogram of the filtered image (NoData check)
        assertEquals(bins[1][255], 5612);

        assertEquals(bins[0][0] + bins[1][0] + bins[2][0] + bins[0][20] + bins[1][20] + bins[2][20]
                + bins[0][180] + bins[0][200] + bins[2][200] + bins[1][255], 100 * 100 * 3);

        // Image filtering
        ROI roi = new ROIShape(new Rectangle(14, 11, 75, 75));
        double[] backgroundValues = new double[] { 0.0d, 0.0d, 0.0d };
        RenderedImage filtered = ArtifactsFilterDescriptor.create(image, roi, backgroundValues, 30,
                3, nodata, null);
        histogramOp = StatisticsDescriptor.create(filtered, 1, 1, null, null, false, new int[] { 0,
                1, 2 }, stats, new double[] { 0 }, new double[] { 256 }, new int[] { 256 }, null);
        bins = new int[3][256];
        results = (Statistics[][]) histogramOp.getProperty(Statistics.STATS_PROPERTY);

        for (int i = 0; i < 3; i++) {
            double[] res = (double[]) results[i][0].getResult();
            for (int k = 0; k < 256; k++) {
                bins[i][k] = (int) res[k];
            }
        }
        // Check if NoData has effect
        if (nodata != null) {

            assertEquals(bins[0][0], dataType < DataBuffer.TYPE_SHORT ? 9302 : 4261);
            assertEquals(bins[1][0], 4261);
            assertEquals(bins[2][0], dataType < DataBuffer.TYPE_SHORT ? 9886 : 4845);
            assertEquals(bins[0][180], 584);
            assertEquals(bins[0][200], 0);
            assertEquals(bins[2][200], 0);
            assertEquals(bins[1][255], 5625);

        } else {

            assertEquals(bins[0][0], 4261);
            assertEquals(bins[1][0], 4261);
            assertEquals(bins[2][0], 4845);
            assertEquals(bins[0][180], 584);
            assertEquals(bins[0][200], 5041);
            assertEquals(bins[2][200], 5041);
            assertEquals(bins[1][255], 5625);

            assertEquals(bins[0][0] + bins[1][0] + bins[2][0] + bins[0][20] + bins[1][20]
                    + bins[2][20] + bins[0][180] + bins[0][200] + bins[2][200] + bins[1][255],
                    100 * 100 * 3);
        }
    }
}
