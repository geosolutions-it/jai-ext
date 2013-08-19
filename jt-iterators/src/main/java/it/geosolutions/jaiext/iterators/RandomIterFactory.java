package it.geosolutions.jaiext.iterators;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

import com.sun.media.jai.iterator.WrapperRI;
import com.sun.media.jai.iterator.WrapperWRI;
import com.sun.media.jai.iterator.WritableRandomIterFallback;

/**
 * A factory class to instantiate instances of the RandomIter and WritableRandomIter interfaces on sources of type Raster, RenderedImage, and
 * WritableRenderedImage.
 * 
 * @see RandomIter
 * @see WritableRandomIter
 */
public class RandomIterFactory {

    /**
     * Constructs and returns an instance of RandomIter suitable for iterating over the given bounding rectangle within the given RenderedImage
     * source. If the bounds parameter is null, the entire image will be used.
     * 
     * @param im a read-only RenderedImage source.
     * @param bounds the bounding Rectangle for the iterator, or null.
     * @return a RandomIter allowing read-only access to the source.
     */
    public static RandomIter create(RenderedImage im, Rectangle bounds) {
        if (bounds == null) {
            bounds = new Rectangle(im.getMinX(), im.getMinY(), im.getWidth(), im.getHeight());
        }
        // return new RandomIterFallbackLarge(im, bounds);
        if (im.getMinTileX() >= Byte.MIN_VALUE
                && (im.getMinTileX() + im.getNumXTiles() - 1) <= Byte.MAX_VALUE
                && im.getMinTileY() >= Byte.MIN_VALUE
                && (im.getMinTileY() + im.getNumYTiles() - 1) <= Byte.MAX_VALUE)
            return new RandomIterFallbackByte(im, bounds);
        else
            return new RandomIterFallbackInt(im, bounds);
    }

    /**
     * Constructs and returns an instance of RandomIter suitable for iterating over the given bounding rectangle within the given Raster source. If
     * the bounds parameter is null, the entire Raster will be used.
     * 
     * @param ras a read-only Raster source.
     * @param bounds the bounding Rectangle for the iterator, or null.
     * @return a RandomIter allowing read-only access to the source.
     */
    public static RandomIter create(Raster ras, Rectangle bounds) {
        RenderedImage im = new WrapperRI(ras);

        return create(im, bounds);
    }

    /**
     * Constructs and returns an instance of WritableRandomIter suitable for iterating over the given bounding rectangle within the given
     * WritableRenderedImage source. If the bounds parameter is null, the entire image will be used.
     * 
     * @param im a WritableRenderedImage source.
     * @param bounds the bounding Rectangle for the iterator, or null.
     * @return a WritableRandomIter allowing read/write access to the source.
     */
    public static WritableRandomIter createWritable(WritableRenderedImage im, Rectangle bounds) {
        if (bounds == null) {
            bounds = new Rectangle(im.getMinX(), im.getMinY(), im.getWidth(), im.getHeight());
        }

        return new WritableRandomIterFallback(im, bounds);
    }

    /**
     * Constructs and returns an instance of WritableRandomIter suitable for iterating over the given bounding rectangle within the given
     * WritableRaster source. If the bounds parameter is null, the entire Raster will be used.
     * 
     * @param ras a WritableRaster source.
     * @param bounds the bounding Rectangle for the iterator, or null.
     * @return a WritableRandomIter allowing read/write access to the source.
     */
    public static WritableRandomIter createWritable(WritableRaster ras, Rectangle bounds) {
        WritableRenderedImage im = new WrapperWRI(ras);

        return createWritable(im, bounds);
    }

    /** Prevent this class from ever being instantiated. */
    private RandomIterFactory() {
    }
}
