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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;

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
    
    @Test
    public void testGetData() {
        // color black only one part
        BufferedImage bi = new BufferedImage(768, 768, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics = bi.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(512, 512, 256, 256);
        graphics.dispose();
        
        // get a subimage where it's fully black
        BufferedImage solidSubimage = bi.getSubimage(512, 512, 256, 256);
        assertSolidTile(solidSubimage, 255);
        
        // get one that's fully transparent instead
        BufferedImage transparentSubimage = bi.getSubimage(256, 256, 256, 256);
        assertSolidTile(transparentSubimage, 0);
    }

    private void assertSolidTile(BufferedImage subimage, int expectedValue) {
        // adapt
        BufferedImageAdapter adapter = new BufferedImageAdapter(subimage);
        // check getting a tile is fully black
        Raster raster = adapter.getTile(0, 0);
        checkSolidTile(raster, expectedValue);
        // check getData
        raster = adapter.getData();
        checkSolidTile(raster, expectedValue);
    }

    private void checkSolidTile(Raster raster, int expectdValue) {
        int[] pixel = new int[4];
        assertEquals(256, raster.getWidth());
        assertEquals(256, raster.getHeight());
        for(int i = raster.getMinX(); i < raster.getHeight(); i++) {
            for(int j = raster.getMinY(); i < raster.getWidth(); i++) {
                raster.getPixel(j, i, pixel);
                assertEquals(expectdValue, pixel[0]);
                assertEquals(expectdValue, pixel[1]);
                assertEquals(expectdValue, pixel[2]);
                assertEquals(expectdValue, pixel[3]);
            }
        }
        
        
    }
}
