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
package it.geosolutions.jaiext.iterators.nl;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RandomIter;

/**
 * Modified version of JAI {@link RandomIterFallbackNoCacheNoArray} that uses a ThreadLocal object for storing the current tile used in iterations.
 */
public class RandomIterFallbackLocal implements RandomIter {

    private final RenderedImage im;

    private final Rectangle boundsRect;

    private final SampleModel sampleModel;

    private final int boundsX;

    private final int boundsY;

    private final int tileWidth;

    private final int tileHeight;

    private final int tileGridXOffset;

    private final int tileGridYOffset;

    private static final ThreadLocal<CurrentTile> iterator = new ThreadLocal<CurrentTile>() {

        // Creation of an initial empty bean
        @Override
        protected CurrentTile initialValue() {
            CurrentTile beanIt = new CurrentTile();
            // The first raster is set to null
            beanIt.tile = null;
            // The tile coordinates are set to (0,0);
            beanIt.tileX = 0;
            beanIt.tileY = 0;
            return beanIt;
        }
    };

    public RandomIterFallbackLocal(RenderedImage im, Rectangle bounds) {
        this.im = im;
        Rectangle imBounds = new Rectangle(im.getMinX(), im.getMinY(), im.getWidth(),
                im.getHeight());
        this.boundsRect = imBounds.intersection(bounds);
        this.sampleModel = im.getSampleModel();

        this.boundsX = boundsRect.x;
        this.boundsY = boundsRect.y;

        this.tileWidth = im.getTileWidth();
        this.tileHeight = im.getTileHeight();
        this.tileGridXOffset = im.getTileGridXOffset();
        this.tileGridYOffset = im.getTileGridYOffset();

    }

    /**
     * Sets dataBuffer to the correct buffer for the pixel (x, y) = (xLocal + boundsRect.x, yLocal + boundsRect.y).
     * 
     * @param xLocal the X coordinate in the local coordinate system.
     * @param yLocal the Y coordinate in the local coordinate system.
     */
    private Raster makeCurrent(int xLocal, int yLocal) {
        // get the tile coordinates
        final int tileXNew = PlanarImage.XToTileX(xLocal, tileGridXOffset, tileWidth);
        final int tileYNew = PlanarImage.YToTileY(yLocal, tileGridYOffset, tileHeight);
        // get the threadLocal object
        final CurrentTile beanOld = iterator.get();
        // get the bean objects
        final Raster tileOld = beanOld.tile;
        final int tileXOld = beanOld.tileX;
        final int tileYOld = beanOld.tileY;
        // if the raster is not defined or the tile coordinates are changed a new tile
        // with new coordinates is calculated
        if (tileOld == null || (tileXNew != tileXOld || tileYNew != tileYOld)) {
            Raster tileNew = im.getTile(tileXNew, tileYNew);
            CurrentTile beanNew = new CurrentTile();
            beanNew.tile = tileNew;
            beanNew.tileX = tileXNew;
            beanNew.tileY = tileYNew;
            // The new tile is returned
            return tileNew;
            // Else the old tile is returned
        } else {
            return tileOld;
        }
    }

    public int getSample(int x, int y, int b) {
        // get tile
        final Raster tile = makeCurrent(x - boundsX, y - boundsY);

        // get value
        final int sampleModelTranslateX = tile.getSampleModelTranslateX();
        final int sampleModelTranslateY = tile.getSampleModelTranslateY();

        return sampleModel.getSample(x - sampleModelTranslateX, y - sampleModelTranslateY, b,
                tile.getDataBuffer());
    }

    public float getSampleFloat(int x, int y, int b) {
        // get tile
        final Raster tile = makeCurrent(x - boundsX, y - boundsY);

        // get value
        final int sampleModelTranslateX = tile.getSampleModelTranslateX();
        final int sampleModelTranslateY = tile.getSampleModelTranslateY();

        return sampleModel.getSampleFloat(x - sampleModelTranslateX, y - sampleModelTranslateY, b,
                tile.getDataBuffer());
    }

    public double getSampleDouble(int x, int y, int b) {
        // get tile
        final Raster tile = makeCurrent(x - boundsX, y - boundsY);

        // get value
        final int sampleModelTranslateX = tile.getSampleModelTranslateX();
        final int sampleModelTranslateY = tile.getSampleModelTranslateY();

        return sampleModel.getSampleDouble(x - sampleModelTranslateX, y - sampleModelTranslateY, b,
                tile.getDataBuffer());
    }

    public int[] getPixel(int x, int y, int[] iArray) {
        // get tile
        final Raster tile = makeCurrent(x - boundsX, y - boundsY);

        // get value
        final int sampleModelTranslateX = tile.getSampleModelTranslateX();
        final int sampleModelTranslateY = tile.getSampleModelTranslateY();

        return sampleModel.getPixel(x - sampleModelTranslateX, y - sampleModelTranslateY, iArray,
                tile.getDataBuffer());
    }

    public float[] getPixel(int x, int y, float[] fArray) {
        // get tile
        final Raster tile = makeCurrent(x - boundsX, y - boundsY);

        // get value
        final int sampleModelTranslateX = tile.getSampleModelTranslateX();
        final int sampleModelTranslateY = tile.getSampleModelTranslateY();

        return sampleModel.getPixel(x - sampleModelTranslateX, y - sampleModelTranslateY, fArray,
                tile.getDataBuffer());
    }

    public double[] getPixel(int x, int y, double[] dArray) {
        // get tile
        final Raster tile = makeCurrent(x - boundsX, y - boundsY);

        // get value
        final int sampleModelTranslateX = tile.getSampleModelTranslateX();
        final int sampleModelTranslateY = tile.getSampleModelTranslateY();

        return sampleModel.getPixel(x - sampleModelTranslateX, y - sampleModelTranslateY, dArray,
                tile.getDataBuffer());
    }

    public void done() {
        iterator.remove();
    }

    /**
     * This class is used only by the threadLocal to store the value of the current tile used and its coordinates. By using getters and setters all
     * the inner parameters can be retrieved or changed
     */
    private static class CurrentTile {

        /** Data tile */
        private Raster tile;

        /** Tile X coordinate */
        private int tileX;

        /** Tile Y coordinate */
        private int tileY;

        private CurrentTile() {
        }
    }
}
