/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
*    http://www.geo-solutions.it/
*    Copyright 2026 GeoSolutions


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

import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RandomIter;

/**
 * Cached random iterator specialized for byte {@link ComponentSampleModel} sources: reads the backing
 * {@code byte[]} arrays directly instead of going through {@link java.awt.image.SampleModel#getSample}. The
 * current tile is kept and only re-fetched when the requested pixel leaves its bounds, so the common warp
 * access pattern (many reads inside one tile) pays no per-pixel tile lookup.
 */
public class RandomIterComponentByte implements RandomIter {

    /** True when the factory can use this iterator for the given source. */
    public static boolean applies(RenderedImage im) {
        return im.getSampleModel() instanceof ComponentSampleModel
                && im.getSampleModel().getDataType() == java.awt.image.DataBuffer.TYPE_BYTE;
    }

    private final RenderedImage im;

    private final int tileWidth;
    private final int tileHeight;
    private final int tileGridXOffset;
    private final int tileGridYOffset;

    private final int pixelStride;
    private final int scanlineStride;
    private final int[] bandOffsets;
    private final int[] bankIndices;

    /** Backing arrays for the current tile, one per band (may share the same array across bands). */
    private final byte[][] bankData;
    /** Per-band base offset: DataBuffer bank offset plus band offset, minus sample-model translate. */
    private final int[] bandBase;

    private int scanlineBase;

    // Half-open bounds of the current tile; a request outside triggers a tile switch.
    private int curMinX = Integer.MAX_VALUE;
    private int curMaxX = Integer.MIN_VALUE;
    private int curMinY = Integer.MAX_VALUE;
    private int curMaxY = Integer.MIN_VALUE;

    public RandomIterComponentByte(RenderedImage im) {
        this.im = im;
        this.tileWidth = im.getTileWidth();
        this.tileHeight = im.getTileHeight();
        this.tileGridXOffset = im.getTileGridXOffset();
        this.tileGridYOffset = im.getTileGridYOffset();

        ComponentSampleModel sm = (ComponentSampleModel) im.getSampleModel();
        this.pixelStride = sm.getPixelStride();
        this.scanlineStride = sm.getScanlineStride();
        this.bandOffsets = sm.getBandOffsets();
        this.bankIndices = sm.getBankIndices();
        this.bankData = new byte[bandOffsets.length][];
        this.bandBase = new int[bandOffsets.length];
    }

    /**
     * Fetches the tile holding (x, y), caches its arrays and bounds, and precomputes the per-band base
     * offset so {@link #getSample} is a single array read.
     */
    private void switchTile(int x, int y) {
        int tileX = PlanarImage.XToTileX(x, tileGridXOffset, tileWidth);
        int tileY = PlanarImage.YToTileY(y, tileGridYOffset, tileHeight);
        Raster tile = im.getTile(tileX, tileY);

        DataBufferByte db = (DataBufferByte) tile.getDataBuffer();
        int transX = tile.getSampleModelTranslateX();
        int transY = tile.getSampleModelTranslateY();
        int[] dbOffsets = db.getOffsets();
        for (int b = 0; b < bandBase.length; b++) {
            bankData[b] = db.getData(bankIndices[b]);
            bandBase[b] = dbOffsets[bankIndices[b]] + bandOffsets[b] - transX * pixelStride;
        }
        this.scanlineBase = -transY * scanlineStride;

        this.curMinX = PlanarImage.tileXToX(tileX, tileGridXOffset, tileWidth);
        this.curMinY = PlanarImage.tileYToY(tileY, tileGridYOffset, tileHeight);
        this.curMaxX = curMinX + tileWidth;
        this.curMaxY = curMinY + tileHeight;
    }

    public int getSample(int x, int y, int b) {
        if (x < curMinX || x >= curMaxX || y < curMinY || y >= curMaxY) {
            switchTile(x, y);
        }
        int index = scanlineBase + y * scanlineStride + x * pixelStride + bandBase[b];
        return bankData[b][index] & 0xFF;
    }

    public float getSampleFloat(int x, int y, int b) {
        return getSample(x, y, b);
    }

    public double getSampleDouble(int x, int y, int b) {
        return getSample(x, y, b);
    }

    public int[] getPixel(int x, int y, int[] iArray) {
        if (iArray == null) {
            iArray = new int[bandOffsets.length];
        }
        for (int b = 0; b < bandOffsets.length; b++) {
            iArray[b] = getSample(x, y, b);
        }
        return iArray;
    }

    public float[] getPixel(int x, int y, float[] fArray) {
        if (fArray == null) {
            fArray = new float[bandOffsets.length];
        }
        for (int b = 0; b < bandOffsets.length; b++) {
            fArray[b] = getSample(x, y, b);
        }
        return fArray;
    }

    public double[] getPixel(int x, int y, double[] dArray) {
        if (dArray == null) {
            dArray = new double[bandOffsets.length];
        }
        for (int b = 0; b < bandOffsets.length; b++) {
            dArray[b] = getSample(x, y, b);
        }
        return dArray;
    }

    public void done() {
        for (int b = 0; b < bankData.length; b++) {
            bankData[b] = null;
        }
    }
}
