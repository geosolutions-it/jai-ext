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
 * 
 * @since 2.7.2
 */
final class CropOpImage extends PointOpImage {

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
        if (tileX >= getMinTileX() && tileX <= getMaxTileX() //  
                && tileY >= getMinTileY() && tileY <= getMaxTileY()) {
            return getSourceImage(0).getTile(tileX, tileY);
        } else {
            return null;
        }
    }
}
