/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2022 GeoSolutions


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

import it.geosolutions.jaiext.mosaic.PixelIterator.PixelIteratorByte;
import org.junit.Before;
import org.junit.Test;

import javax.media.jai.RasterFormatTag;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.CropDescriptor;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PixelIteratorTest {

    private static final int SIZE = 4;
    private static final int BASE_BLUE = 0;
    private static final int BASE_GREEN = 20;
    private static final int BASE_RED = 40;
    private BufferedImage image;

    @Before
    public void buildSampleImage() {
        this.image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_3BYTE_BGR);
        int[] v = {BASE_BLUE, BASE_GREEN, BASE_RED};
        WritableRaster raster = this.image.getWritableTile(BASE_BLUE, BASE_BLUE);
        for (int r = BASE_BLUE; r < SIZE; r++) {
            for (int c = BASE_BLUE; c < SIZE; c++) {
                raster.setPixel(c, r, v);
                v[0]++;
                v[1]++;
                v[2]++;
            }
        }
    }

    @Test
    public void testAligned() {
        Rectangle bounds = image.getData().getBounds();
        RasterFormatTag[] tags =
                RasterAccessorExt.findCompatibleTags(new RenderedImage[]{image}, image);
        PixelIteratorByte reader = new PixelIteratorByte(bounds, bounds,
                new RasterAccessorExt(image.getRaster(), bounds, tags[0],
                        image.getColorModel(),
                        3, DataBuffer.TYPE_BYTE));
        byte[] expected = {BASE_BLUE, BASE_GREEN, BASE_RED};
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                assertTrue(reader.hasData());
                assertFalse(reader.isDone());
                byte[] px = reader.read();
                assertArrayEquals(expected, px);
                expected[0]++;
                expected[1]++;
                expected[2]++;
                reader.nextPixel();
            }
            reader.nextLine();
        }
    }

    @Test
    public void testSubImage() {
        RenderedOp cropped = CropDescriptor.create(image, 1f, 1f, 2f, 2f, null);
        Rectangle destBounds = image.getData().getBounds();
        Rectangle sourceBounds = cropped.getBounds();
        RasterFormatTag[] tags =
                RasterAccessorExt.findCompatibleTags(new RenderedImage[]{cropped}, image);
        PixelIteratorByte iterator = new PixelIteratorByte(sourceBounds, destBounds,
                new RasterAccessorExt(cropped.getData(), sourceBounds, tags[0],
                        image.getColorModel(),
                        3, DataBuffer.TYPE_BYTE));

        // first row, fully missing
        for (int c = 0; c < SIZE; c++) {
            assertFalse(iterator.hasData());
            assertFalse(iterator.isDone());
            iterator.nextPixel();
        }
        iterator.nextLine();

        // second row, data in the middle
        // first pixel, no data
        assertFalse(iterator.hasData());
        assertFalse(iterator.isDone());
        iterator.nextPixel();

        // second pixel, has data
        assertTrue(iterator.hasData());
        assertArrayEquals(new byte[]{BASE_BLUE + 5, BASE_GREEN + 5, BASE_RED + 5},
                iterator.read());
        assertFalse(iterator.isDone());
        iterator.nextPixel();

        // third pixel, has data
        assertTrue(iterator.hasData());
        assertArrayEquals(new byte[]{BASE_BLUE + 6, BASE_GREEN + 6, BASE_RED + 6},
                iterator.read());
        assertFalse(iterator.isDone());
        iterator.nextPixel();

        // fourth pixel, no data
        assertFalse(iterator.hasData());
        assertFalse(iterator.isDone());
        iterator.nextPixel();
        iterator.nextLine();
        
        // third dow row, data in the middle
        // first pixel, no data
        assertFalse(iterator.hasData());
        assertFalse(iterator.isDone());
        iterator.nextPixel();

        // second pixel, has data
        assertTrue(iterator.hasData());
        assertArrayEquals(new byte[]{BASE_BLUE + 9, BASE_GREEN + 9, BASE_RED + 9},
                iterator.read());
        assertFalse(iterator.isDone());
        iterator.nextPixel();

        // third pixel, has data
        assertTrue(iterator.hasData());
        assertArrayEquals(new byte[]{BASE_BLUE + 10, BASE_GREEN + 10, BASE_RED + 10},
                iterator.read());
        assertFalse(iterator.isDone());
        iterator.nextPixel();

        // fourth pixel, no data and we're done
        assertFalse(iterator.hasData());
        assertTrue(iterator.isDone());
        iterator.nextPixel();
        iterator.nextLine();

        // last row, fully missing
        for (int c = 0; c < SIZE; c++) {
            assertFalse(iterator.hasData());
            assertTrue(iterator.isDone());
            iterator.nextPixel();
        }
    }
}