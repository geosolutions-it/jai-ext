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
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RandomIter;


/**
 * Slight modified version of JAI {@link RandomIterFallbackNoCacheNoArray} that uses int vectors to handle indexes. It is useful when we have so many tiles that we
 * cannot actually use byte indexes.
 * 
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class RandomIterFallbackInt implements RandomIter {

    protected RenderedImage im;

    protected Rectangle boundsRect;

    protected SampleModel sampleModel;

    protected int xID;

    protected int yID;

    protected int sampleModelTranslateX;

    protected int sampleModelTranslateY;

    protected DataBuffer dataBuffer = null;

    protected int boundsX;

    protected int boundsY;

    protected int[] xTiles;

    protected int[] yTiles;

    public RandomIterFallbackInt(RenderedImage im, Rectangle bounds) {
        this.im = im;

        Rectangle imBounds = new Rectangle(im.getMinX(), im.getMinY(), im.getWidth(),
                im.getHeight());
        this.boundsRect = imBounds.intersection(bounds);
        this.sampleModel = im.getSampleModel();

        int x = boundsRect.x;
        int y = boundsRect.y;
        int width = boundsRect.width;
        int height = boundsRect.height;

        this.boundsX = boundsRect.x;
        this.boundsY = boundsRect.y;
        this.xTiles = new int[width];
        this.yTiles = new int[height];

        int tileWidth = im.getTileWidth();
        int tileGridXOffset = im.getTileGridXOffset();
        int minTileX = PlanarImage.XToTileX(x, tileGridXOffset, tileWidth);
        int offsetX = x - PlanarImage.tileXToX(minTileX, tileGridXOffset, tileWidth);
        int tileX = minTileX;

        for (int i = 0; i < width; i++) {
            xTiles[i] = tileX;
            ++offsetX;
            if (offsetX == tileWidth) {
                ++tileX;
                offsetX = 0;
            }
        }

        int tileHeight = im.getTileHeight();
        int tileGridYOffset = im.getTileGridYOffset();
        int minTileY = PlanarImage.YToTileY(y, tileGridYOffset, tileHeight);
        int offsetY = y - PlanarImage.tileYToY(minTileY, tileGridYOffset, tileHeight);
        int tileY = minTileY;

        for (int i = 0; i < height; i++) {
            yTiles[i] = tileY;
            ++offsetY;
            if (offsetY == tileHeight) {
                ++tileY;
                offsetY = 0;
            }
        }
    }

    /**
     * Sets dataBuffer to the correct buffer for the pixel (x, y) = (xLocal + boundsRect.x, yLocal + boundsRect.y).
     * 
     * @param xLocal the X coordinate in the local coordinate system.
     * @param yLocal the Y coordinate in the local coordinate system.
     */
    private void makeCurrent(int xLocal, int yLocal) {
        int xIDNew = xTiles[xLocal];
        int yIDNew = yTiles[yLocal];

        if ((xIDNew != xID) || (yIDNew != yID) || (dataBuffer == null)) {
            xID = xIDNew;
            yID = yIDNew;

            Raster tile = im.getTile(xID, yID);

            this.dataBuffer = tile.getDataBuffer();
            this.sampleModelTranslateX = tile.getSampleModelTranslateX();
            this.sampleModelTranslateY = tile.getSampleModelTranslateY();
        }
    }

    public int getSample(int x, int y, int b) {
        makeCurrent(x - boundsX, y - boundsY);

        return sampleModel.getSample(x - sampleModelTranslateX, y - sampleModelTranslateY, b,
                dataBuffer);
    }

    public float getSampleFloat(int x, int y, int b) {
        makeCurrent(x - boundsX, y - boundsY);

        return sampleModel.getSampleFloat(x - sampleModelTranslateX, y - sampleModelTranslateY, b,
                dataBuffer);
    }

    public double getSampleDouble(int x, int y, int b) {
        makeCurrent(x - boundsX, y - boundsY);

        return sampleModel.getSampleDouble(x - sampleModelTranslateX, y - sampleModelTranslateY, b,
                dataBuffer);
    }

    public int[] getPixel(int x, int y, int[] iArray) {
        makeCurrent(x - boundsX, y - boundsY);

        return sampleModel.getPixel(x - sampleModelTranslateX, y - sampleModelTranslateY, iArray,
                dataBuffer);
    }

    public float[] getPixel(int x, int y, float[] fArray) {
        makeCurrent(x - boundsX, y - boundsY);

        return sampleModel.getPixel(x - sampleModelTranslateX, y - sampleModelTranslateY, fArray,
                dataBuffer);
    }

    public double[] getPixel(int x, int y, double[] dArray) {
        makeCurrent(x - boundsX, y - boundsY);

        return sampleModel.getPixel(x - sampleModelTranslateX, y - sampleModelTranslateY, dArray,
                dataBuffer);
    }

    public void done() {
        xTiles = null;
        yTiles = null;
        dataBuffer = null;
    }
}
