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
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RandomIter;

/**
 * Cached random iterator specialized for byte {@link ComponentSampleModel} sources: reads the backing
 * {@code byte[]} arrays directly instead of going through {@link SampleModel#getSample}. The current
 * tile is kept and only re-fetched when the requested pixel leaves its bounds, so the common warp
 * access pattern (many reads inside one tile) pays no per-pixel tile lookup.
 *
 * <p>Reads are cheap here, tile switches are not. That fits how warp, scale and affine read: the
 * source point moves a little at a time, so one tile serves many reads. A caller that jumps to a new
 * tile on almost every read should build a {@link RandomIterFallbackByte} instead, because
 * {@link RandomIterFactory} always picks this one for a byte source. RandomIterBenchmark, in the test
 * sources, measures both cases.
 */
public class RandomIterComponentByte implements RandomIter {

    /** True when the factory can use this iterator for the given source. */
    public static boolean applies(RenderedImage im) {
        SampleModel sm = im.getSampleModel();
        return sm instanceof ComponentSampleModel && sm.getDataType() == DataBuffer.TYPE_BYTE;
    }

    private final RenderedImage im;

    private final int tileWidth;
    private final int tileHeight;
    private final int tileGridXOffset;
    private final int tileGridYOffset;
    private final int imageMinX;
    private final int imageMinY;
    private final int imageMaxX;
    private final int imageMaxY;

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
        this.imageMinX = im.getMinX();
        this.imageMinY = im.getMinY();
        this.imageMaxX = im.getMinX() + im.getWidth();
        this.imageMaxY = im.getMinY() + im.getHeight();

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
        // A tile at the edge can extend past the image, and that extra area holds no image data, so
        // being inside a tile is not enough. Checked here, so only tile switches pay for it. The
        // other iterators fail on the same reads, from their tile index arrays.
        if (x < imageMinX || x >= imageMaxX || y < imageMinY || y >= imageMaxY) {
            throw new ArrayIndexOutOfBoundsException(
                    "Point " + x + ", " + y + " is outside the image bounds");
        }
        int tileX = PlanarImage.XToTileX(x, tileGridXOffset, tileWidth);
        int tileY = PlanarImage.YToTileY(y, tileGridYOffset, tileHeight);
        Raster tile = im.getTile(tileX, tileY);

        DataBufferByte db = (DataBufferByte) tile.getDataBuffer();
        int transX = tile.getSampleModelTranslateX();
        int transY = tile.getSampleModelTranslateY();
        int[] dbOffsets = db.getOffsets();
        // one getBankData call, not one getData per band: each getData marks the buffer untrackable,
        // which costs more than it saves, see RandomIterBenchmark
        byte[][] banks = db.getBankData();
        for (int b = 0; b < bandBase.length; b++) {
            bankData[b] = banks[bankIndices[b]];
            bandBase[b] = dbOffsets[bankIndices[b]] + bandOffsets[b] - transX * pixelStride;
        }
        this.scanlineBase = -transY * scanlineStride;

        // Clipped to the image, so a read past the edge leaves this window and comes back through
        // the check above, instead of reading the empty area of an edge tile.
        this.curMinX = PlanarImage.tileXToX(tileX, tileGridXOffset, tileWidth);
        this.curMinY = PlanarImage.tileYToY(tileY, tileGridYOffset, tileHeight);
        this.curMaxX = Math.min(curMinX + tileWidth, imageMaxX);
        this.curMaxY = Math.min(curMinY + tileHeight, imageMaxY);
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
