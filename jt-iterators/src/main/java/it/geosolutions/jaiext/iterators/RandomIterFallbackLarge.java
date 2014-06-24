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
import java.util.Arrays;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RandomIter;
/**
 * Slight modified version of JAI {@link RandomIterFallbackNoCacheNoArray} that uses byte vectors to handle indexes rather than int vector. This way we use 4x4
 * times less memory in the iterator
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class RandomIterFallbackLarge implements RandomIter {

    protected RenderedImage im;

    protected Rectangle boundsRect;

    protected SampleModel sampleModel;

    protected int xID = Integer.MIN_VALUE;

    protected int yID = Integer.MIN_VALUE;

    protected int sampleModelTranslateX;

    protected int sampleModelTranslateY;

    protected DataBuffer dataBuffer = null;

    protected int boundsX;

    protected int boundsY;

    protected int[] xTiles;

    protected int[] yTiles;

    private int minTileX;

    private int minTileY;

    private int tileGridXOffset;

    private int tileHeight;

    private int tileGridYOffset;

    private int tileWidth;

    private int currentMaxXInclusive = Integer.MIN_VALUE;

    private int currentMaxYInclusive = Integer.MIN_VALUE;

    private int minx;

    private int miny;

    private int currentMinXInclusive = Integer.MAX_VALUE;

    private int currentMinYInclusive = Integer.MAX_VALUE;

    public RandomIterFallbackLarge(RenderedImage im, Rectangle bounds) {
        this.im = im;

        Rectangle imBounds = new Rectangle(im.getMinX(), im.getMinY(), im.getWidth(),
                im.getHeight());
        this.boundsRect = imBounds.intersection(bounds);
        this.sampleModel = im.getSampleModel();

        this.boundsX = boundsRect.x;
        this.boundsY = boundsRect.y;

        tileWidth = im.getTileWidth();
        tileGridXOffset = im.getTileGridXOffset();
        tileHeight = im.getTileHeight();
        tileGridYOffset = im.getTileGridYOffset();
        minTileX = im.getMinTileX();

        final int maxTileXExcluded = minTileX + im.getNumXTiles();
        minTileY = im.getMinTileY();

        final int maxTileYExcluded = minTileY + im.getNumYTiles();

        this.xTiles = new int[im.getNumXTiles()];
        this.yTiles = new int[im.getNumYTiles()];

        minx = im.getMinX();
        miny = im.getMinY();

        final int maxx = minx + im.getWidth() - 1;
        final int maxy = miny + im.getHeight() - 1;

        for (int xT = minTileX; xT < maxTileXExcluded; xT++) {
            xTiles[xT - minTileX] = PlanarImage.tileXToX(xT, tileGridXOffset, tileWidth)
                    + tileWidth - 1;
            xTiles[xT - minTileX] = Math.min(xTiles[xT], maxx);
        }
        for (int yT = minTileY; yT < maxTileYExcluded; yT++) {
            yTiles[yT - minTileY] = PlanarImage.tileYToY(yT, tileGridYOffset, tileHeight)
                    + tileHeight - 1;
            yTiles[yT - minTileY] = Math.min(yTiles[yT], maxy);
        }
    }

    /**
     * Sets dataBuffer to the correct buffer for the pixel (x, y) = (xLocal + boundsRect.x, yLocal + boundsRect.y).
     * 
     * @param xLocal the X coordinate in the local coordinate system.
     * @param yLocal the Y coordinate in the local coordinate system.
     */
    private void makeCurrent(int xLocal, int yLocal) {
        if ((xLocal <= currentMaxXInclusive) && (xLocal >= currentMinXInclusive)
                && (yLocal <= currentMaxYInclusive) && (yLocal >= currentMinYInclusive)) {
            return;
        }

        int xIDNew = Arrays.binarySearch(xTiles, xLocal);
        if (xIDNew < 0) {
            xIDNew += 1;
            xIDNew *= -1;
        }
        xIDNew += minTileX;

        int yIDNew = Arrays.binarySearch(yTiles, yLocal);
        if (yIDNew < 0) {
            yIDNew += 1;
            yIDNew *= -1;
        }
        yIDNew += minTileY;

        if ((xIDNew != xID) || (yIDNew != yID) || (dataBuffer == null)) {
            xID = xIDNew;
            yID = yIDNew;
            currentMaxXInclusive = xTiles[xID - minTileX];
            currentMaxYInclusive = yTiles[yID - minTileY];
            currentMinXInclusive = ((xID - minTileX - 1) >= 0) ? (xTiles[xID - minTileX - 1] + 1)
                    : minx;
            currentMinYInclusive = ((yID - minTileY - 1) >= 0) ? (yTiles[yID - minTileY - 1] + 1)
                    : miny;

            Raster tile = im.getTile(xID, yID);

            this.dataBuffer = tile.getDataBuffer();
            this.sampleModelTranslateX = tile.getSampleModelTranslateX();
            this.sampleModelTranslateY = tile.getSampleModelTranslateY();
        }
    }

    public int getSample(int x, int y, int b) {
        makeCurrent(x, y);

        return sampleModel.getSample(x - sampleModelTranslateX, y - sampleModelTranslateY, b,
                dataBuffer);
    }

    public float getSampleFloat(int x, int y, int b) {
        makeCurrent(x, y);

        return sampleModel.getSampleFloat(x - sampleModelTranslateX, y - sampleModelTranslateY, b,
                dataBuffer);
    }

    public double getSampleDouble(int x, int y, int b) {
        makeCurrent(x, y);

        return sampleModel.getSampleDouble(x - sampleModelTranslateX, y - sampleModelTranslateY, b,
                dataBuffer);
    }

    public int[] getPixel(int x, int y, int[] iArray) {
        makeCurrent(x, y);

        return sampleModel.getPixel(x - sampleModelTranslateX, y - sampleModelTranslateY, iArray,
                dataBuffer);
    }

    public float[] getPixel(int x, int y, float[] fArray) {
        makeCurrent(x, y);

        return sampleModel.getPixel(x - sampleModelTranslateX, y - sampleModelTranslateY, fArray,
                dataBuffer);
    }

    public double[] getPixel(int x, int y, double[] dArray) {
        makeCurrent(x, y);

        return sampleModel.getPixel(x - sampleModelTranslateX, y - sampleModelTranslateY, dArray,
                dataBuffer);
    }

    public void done() {
        xTiles = null;
        yTiles = null;
        dataBuffer = null;
    }
}
