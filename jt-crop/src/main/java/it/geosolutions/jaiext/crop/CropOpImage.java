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
package it.geosolutions.jaiext.crop;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.Map;

import javax.media.jai.ImageLayout;
import javax.media.jai.PointOpImage;

/**
 * An alternative implementation of JAI Crop that respects the tile cache and tile scheduler
 * specified in the rendering hints.
 * 
 * This is a clean room implementation (there are no others ways in which a Crop can be implemented,
 * so it looks a lot like the original one)
 */
public class CropOpImage extends PointOpImage {

    private static ImageLayout layoutHelper(RenderedImage source, float originX, float originY,
            float width, float height) {
        Rectangle bounds = new Rectangle2D.Float(originX, originY, width, height).getBounds();

        return new ImageLayout(bounds.x, bounds.y, bounds.width, bounds.height, source
                .getTileGridXOffset(), source.getTileGridYOffset(), source.getTileWidth(), source
                .getTileHeight(), source.getSampleModel(), source.getColorModel());

    }

    /**
     * Construct a CropOpImage.
     */
    public CropOpImage(RenderedImage source, float originX, float originY, float width,
            float height, Map configuration) {
        super(source, layoutHelper(source, originX, originY, width, height), configuration, false);
    }

    /**
     * We just relay the original tiles, or return null
     */
    public boolean computesUniqueTiles() {
        return false;
    }

    /**
     * Make sure the tile scheduler ends up calling getTile
     */
    public Raster computeTile(int tileX, int tileY) {
        return getTile(tileX, tileY);
    }

    /**
     * Returns a tile. Either it's fully inside, and thus we relay the original tile (in-tile
     * cutting will be performed by the image layout), or we return null
     */
    public Raster getTile(int tileX, int tileY) {
        if (tileX >= getMinTileX() && tileX <= getMaxTileX() 
                && tileY >= getMinTileY() && tileY <= getMaxTileY()) {
            return getSourceImage(0).getTile(tileX, tileY);
        } else {
            return null;
        }
    }
}
