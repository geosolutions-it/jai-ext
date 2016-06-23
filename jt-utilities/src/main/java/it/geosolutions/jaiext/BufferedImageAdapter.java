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

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Map;

import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedImageAdapter;

/**
 * a {@link RenderedImageAdapter} alternative that works around a bug in {@link BufferedImage} sub-images
 * @author Andrea Aime - GeoSolutions
 */
public class BufferedImageAdapter extends PlanarImage {

    private BufferedImage image;
    
    private static Map getProperties(BufferedImage bi) {
        String[] propertyNames = bi.getPropertyNames();
        if(propertyNames == null) {
            return null;
        }
        Map<String, Object> properties = new HashMap<String, Object>();
        for (String name : bi.getPropertyNames()) {
            properties.put(name, bi.getProperty(name));
        }
        return properties;
    }

    private static ImageLayout getImageLayout(BufferedImage im) {
        // a subimage generates a wrong tile layout, set it manually
        ImageLayout il = new ImageLayout(im);
        il.unsetTileLayout();
        il.setTileGridXOffset(0);
        il.setTileGridYOffset(0);
        il.setTileWidth(im.getWidth());
        il.setTileHeight(im.getHeight());
        return il;
    }

    public BufferedImageAdapter(BufferedImage image) {
        super(getImageLayout(image), null, getProperties(image));
        this.image = image;
    }

    @Override
    public Raster getTile(int tileX, int tileY) {
        if (tileX != 0 || tileY != 0) {
            return null;
        } else {
            return image.getTile(tileX, tileY);
        }
    }
    
    public Raster getData() {
        final WritableRaster raster = image.getRaster();
        if(raster.getParent() != null) {
            // have to force a copy, otherwise we are breaking the RenderedImage contract just
            // like BufferedImage does (the PNGJ writer actually has special code to work
            // around that bug)
            return image.getData(new Rectangle(0, 0, raster.getWidth(), raster.getHeight()));
        } else {
            return raster;
        }
    }

    public Raster getData(Rectangle rect) {
        return image.getData(rect);
    }

    public WritableRaster copyData(WritableRaster raster) {
        return image.copyData(raster);
    }

}
