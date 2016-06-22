/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2016 GeoSolutions


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
package it.geosolutions.jaiext;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;

import javax.media.jai.RenderedOp;
import javax.media.jai.operator.BandSelectDescriptor;

import org.junit.Test;

public class BufferedImageAdapterTest {

    @Test
    public void testBufferedImage() {
        BufferedImage bi = new BufferedImage(768, 768, BufferedImage.TYPE_4BYTE_ABGR);
        BufferedImageAdapter adapter = new BufferedImageAdapter(bi);
        // a buffered image is always stuck in 0,0
        assertEquals(0, adapter.getMinX());
        assertEquals(0, adapter.getMinY());
        assertEquals(0, adapter.getMinTileX());
        assertEquals(0, adapter.getMinTileY());
        assertEquals(0, adapter.getTileGridXOffset());
        assertEquals(0, adapter.getTileGridYOffset());
        RenderedOp op = BandSelectDescriptor.create(adapter, new int[1], null);
        // JAI did not need to wrap it
        assertSame(adapter, op.getSourceObject(0));
        // no NPE (we do not expect one here actually)
        assertNotNull(op.getTile(op.getMinTileX(), op.getMinTileY()));
    }
    
    @Test
    public void testSubimage() {
        BufferedImage bi = new BufferedImage(768, 768, BufferedImage.TYPE_4BYTE_ABGR);
        // get a subimage
        BufferedImage subimage = bi.getSubimage(0, 512, 256, 256);
        BufferedImageAdapter adapter = new BufferedImageAdapter(subimage);
        // a buffered image is always stuck in 0,0, even if it's a subimage
        assertEquals(0, adapter.getMinX());
        assertEquals(0, adapter.getMinY());
        assertEquals(0, adapter.getMinTileX());
        assertEquals(0, adapter.getMinTileY());
        assertEquals(0, adapter.getTileGridXOffset());
        assertEquals(0, adapter.getTileGridYOffset());
        RenderedOp op = BandSelectDescriptor.create(adapter, new int[1], null);
        // JAI did not need to wrap it
        assertSame(adapter, op.getSourceObject(0));
        // no NPE (RenderedImageAdapter would blow up here)
        assertNotNull(op.getTile(op.getMinTileX(), op.getMinTileY()));
    }
}
