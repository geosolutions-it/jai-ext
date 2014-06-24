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
     * source. If the bounds parameter is null, the entire image will be used. If cachedTiles is set to true, the current tile used by the iterator
     * is cached. If arrayCalculation is set to true an initial array containing the tile position for every pixel is calculated.
     * 
     * @param im a read-only RenderedImage source.
     * @param bounds the bounding Rectangle for the iterator, or null.
     * @param cachedTiles flag indicating if tiles must be cached during iteration.
     * @param arrayCalculation flag indicating if tile positions must be pre-calculated.
     * @return a RandomIter allowing read-only access to the source.
     */
    public static RandomIter create(RenderedImage im, Rectangle bounds, boolean cachedTiles,
            boolean arrayCalculation) {
        if (bounds == null) {
            bounds = new Rectangle(im.getMinX(), im.getMinY(), im.getWidth(), im.getHeight());
        }
        if (arrayCalculation) {
            if (im.getMinTileX() >= Byte.MIN_VALUE
                    && (im.getMinTileX() + im.getNumXTiles() - 1) <= Byte.MAX_VALUE
                    && im.getMinTileY() >= Byte.MIN_VALUE
                    && (im.getMinTileY() + im.getNumYTiles() - 1) <= Byte.MAX_VALUE) {
                if (cachedTiles) {
                    return new RandomIterFallbackByte(im, bounds);
                } else {
                    return new RandomIterFallbackByteNoCache(im, bounds);
                }
            } else if (im.getMinTileX() >= Short.MIN_VALUE
                    && (im.getMinTileX() + im.getNumXTiles() - 1) <= Short.MAX_VALUE
                    && im.getMinTileY() >= Short.MIN_VALUE
                    && (im.getMinTileY() + im.getNumYTiles() - 1) <= Short.MAX_VALUE) {
                if (cachedTiles) {
                    return new RandomIterFallbackShort(im, bounds);
                } else {
                    return new RandomIterFallbackShortNoCache(im, bounds);
                }
            } else {
                if (cachedTiles) {
                    return new RandomIterFallbackInt(im, bounds);
                } else {
                    return new RandomIterFallbackIntNoCache(im, bounds);
                }
            }
        } else {
            return new RandomIterFallbackNoCacheNoArray(im, bounds);
        }

    }

    /**
     * Constructs and returns an instance of RandomIter suitable for iterating over the given bounding rectangle within the given Raster source. If
     * the bounds parameter is null, the entire Raster will be used. If cachedTiles is set to true, the current tile used by the iterator is cached.
     * If arrayCalculation is set to true an initial array containing the tile position for every pixel is calculated.
     * 
     * @param ras a read-only Raster source.
     * @param bounds the bounding Rectangle for the iterator, or null.
     * @param cachedTiles flag indicating if tiles must be cached during iteration.
     * @param arrayCalculation flag indicating if tile positions must be pre-calculated.
     * @return a RandomIter allowing read-only access to the source.
     */
    public static RandomIter create(Raster ras, Rectangle bounds, boolean cachedTiles,
            boolean arrayCalculation) {
        RenderedImage im = new WrapperRI(ras);

        return create(im, bounds, cachedTiles, arrayCalculation);
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
