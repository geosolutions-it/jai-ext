/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2018 GeoSolutions
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.geosolutions.jaiext.jiffleop;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

import javax.media.jai.RenderedOp;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import it.geosolutions.jaiext.jiffle.runtime.BandTransform;
import it.geosolutions.jaiext.testclasses.TestBase;
import it.geosolutions.jaiext.utilities.ImageUtilities;

public class JiffleOpTest extends TestBase {
    
    @Test
    public void testCopyDefaults() {
        RenderedImage src = buildTestImage(10, 10);
        RenderedOp op = JiffleDescriptor.create(new RenderedImage[] {src}, null, null,
                "dest = src;", null, null, null, null, null);
        assertCopy(src, op, DataBuffer.TYPE_DOUBLE);
    }

    @Test
    public void testCopyRemappedDefaults() {
        RenderedImage src = buildTestImage(10, 10);
        BandTransform transform = (x, y, b) -> 0;
        RenderedOp op = JiffleDescriptor.create(new RenderedImage[] {src}, null, null,
                "dest = src[10];", null, null, null, new BandTransform[] {transform}, null);
        assertCopy(src, op, DataBuffer.TYPE_DOUBLE);
    }

    @Test
    public void testCopyNonDefaults() {
        RenderedImage src = buildTestImage(10, 10);
        RenderedOp op = JiffleDescriptor.create(new RenderedImage[] {src}, new String[] {"a"}, "b",
                "b = a;", null, DataBuffer.TYPE_BYTE, null, null, null);
        assertCopy(src, op, DataBuffer.TYPE_BYTE);
    }

    @Test
    public void testSum() {
        RenderedImage src1 = buildTestImage(10, 10);
        RenderedImage src2 = buildTestImage(10, 10);
        RenderedOp op = JiffleDescriptor.create(new RenderedImage[] {src1, src2}, new String[] {"a", "b"}, "res",
                "res = a + b;", null, DataBuffer.TYPE_INT, null, null, null);

        // check same size and expected 
        assertEquals(src1.getMinX(), op.getMinX());
        assertEquals(src1.getWidth(), op.getWidth());
        assertEquals(src1.getMinY(), op.getMinY());
        assertEquals(src1.getHeight(), op.getHeight());
        assertEquals(DataBuffer.TYPE_INT, op.getSampleModel().getDataType());

        RandomIter srcIter = RandomIterFactory.create(src1, null);
        RandomIter opIter = RandomIterFactory.create(op, null);
        for(int y = src1.getMinY(); y < src1.getMinY() + src1.getHeight(); y++) {
            for(int x = src1.getMinX(); x < src1.getMinX() + src1.getWidth(); x++) {
                double expected = srcIter.getSampleDouble(x, y, 0) * 2;
                double actual = opIter.getSampleDouble(x, y, 0);
                assertEquals(expected, actual, 0d);
            }
        }
    }

    private void assertCopy(RenderedImage src, RenderedOp op, int dataType) {
        // check it's a copy with the expected values
        assertEquals(src.getMinX(), op.getMinX());
        assertEquals(src.getWidth(), op.getWidth());
        assertEquals(src.getMinY(), op.getMinY());
        assertEquals(src.getHeight(), op.getHeight());
        assertEquals(dataType, op.getSampleModel().getDataType());

        RandomIter srcIter = RandomIterFactory.create(src, null);
        RandomIter opIter = RandomIterFactory.create(op, null);
        for(int y = src.getMinY(); y < src.getMinY() + src.getHeight(); y++) {
            for(int x = src.getMinX(); x < src.getMinX() + src.getWidth(); x++) {
                double expected = srcIter.getSampleDouble(x, y, 0);
                double actual = opIter.getSampleDouble(x, y, 0);
                assertEquals(expected, actual, 0d);
            }
        }
    }

    private RenderedImage buildTestImage(int width, int height) {
        Number[] values = new Number[width * height];
        for (int i = 0; i < values.length; i++) {
            values[i] = Byte.valueOf((byte) i);
        }
        return ImageUtilities.createImageFromArray(values, width, height);
    }
}
