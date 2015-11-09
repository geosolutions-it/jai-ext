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
package it.geosolutions.concurrent;

import it.geosolutions.concurrent.ConcurrentTileCache.Actions;

import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.lang.ref.WeakReference;
import java.math.BigInteger;

import javax.media.jai.CachedTile;
import javax.media.jai.PlanarImage;
import javax.media.jai.remote.SerializableRenderedImage;

/**
 * This class is used by ConcurrentTileCache to create an object that includes all the information associated with a tile, and is put into the cache.
 */
public final class CachedTileImpl implements CachedTile {
    
    /*
     * The shallow size of this object, the weak reference, the typical big integer keys
     */
    private static final long CACHED_TILE_OVERHEAD = 64 + 32 + 96 + 96;

    final Raster tile; // the tile

    final WeakReference owner; // the RenderedImage of this tile

    final int tileX; // tile X index

    final int tileY; // tile Y index

    final Object tileCacheMetric; // Metric for weighting tile computation cost

    private long timeStamp; // the last time this tile is accessed (if diagnosticEnable==false it is set only at the creation time)

    final Object key; // the key used to hash this tile
    
    private final Object imageKey; // Key of the associated image

    final long tileSize; // the memory of this tile in bytes

    private Actions action; // every action done by the tile cache

    /**
     * Constructor that takes a tile cache metric
     * 
     * @since 1.1
     */
    public CachedTileImpl(RenderedImage owner, int tileX, int tileY, Raster tile,
            Object tileCacheMetric) {

        this.owner = new WeakReference(owner);
        this.tile = tile;
        this.tileX = tileX;
        this.tileY = tileY;

        this.tileCacheMetric = tileCacheMetric; // may be null

        key = hashKey(owner, tileX, tileY);
        
        imageKey = hashKey(owner);

        DataBuffer db = tile.getDataBuffer();
        tileSize = db.getDataTypeSize(db.getDataType()) / 8L * db.getSize() * db.getNumBanks() + CACHED_TILE_OVERHEAD;
        updateTileTimeStamp();

    }

    /**
     * Returns the key associated to the tile.
     * @return
     */
    public Object getKey() {
        return key;
    }

    /**
     * Returns the key associate to the tile owner
     * @return
     */
    public Object getImageKey() {
        return imageKey;
    }
    
    /**
     * Returns the hash table "key" as a <code>Object</code> for this tile.
     */
    public static Object hashKey(RenderedImage owner, int tileX, int tileY) {
        long idx = tileY * (long) owner.getNumXTiles() + tileX;

        BigInteger imageID = null;
        if (owner instanceof PlanarImage)
            imageID = (BigInteger) ((PlanarImage) owner).getImageID();
        else if (owner instanceof SerializableRenderedImage)
            imageID = (BigInteger) ((SerializableRenderedImage) owner).getImageID();

        if (imageID != null) {
            byte[] buf = imageID.toByteArray();
            int length = buf.length;
            byte[] buf1 = new byte[length + 8];
            System.arraycopy(buf, 0, buf1, 0, length);
            for (int i = 7, j = 0; i >= 0; i--, j += 8)
                buf1[length++] = (byte) (idx >> j);
            return new BigInteger(buf1);
        }

        idx = idx & 0x00000000ffffffffL;
        return Long.valueOf((((long) owner.hashCode() << 32) | idx));
    }

    /**
     * Returns the hash table "key" as a <code>Object</code> for this image.
     */
    public static Object hashKey(RenderedImage owner) {

        BigInteger imageID = null;
        if (owner instanceof PlanarImage)
            imageID = (BigInteger) ((PlanarImage) owner).getImageID();
        else if (owner instanceof SerializableRenderedImage)
            imageID = (BigInteger) ((SerializableRenderedImage) owner).getImageID();

        if (imageID != null) {
            byte[] buf = imageID.toByteArray();
            return new BigInteger(buf);
        }

        return owner.hashCode();
    }

    /** Returns the value of the cached tile. */
    public Raster getTile() {
        return tile;
    }

    /** Returns the owner of the cached tile. */
    public RenderedImage getOwner() {
        return (RenderedImage) owner.get();
    }

    /** Returns the current time stamp */
    public long getTileTimeStamp() {
        return timeStamp;
    }

    /** Returns the tileCacheMetric object */
    public Object getTileCacheMetric() {
        return tileCacheMetric;
    }

    /** Returns the tile memory size */
    public long getTileSize() {
        return tileSize;
    }

    /**
     * Returns information about the status of the tile
     */
    public int getAction() {
        return action.valueAction();
    }

    /** Sets the status of the tile */
    public void setAction(Actions newAction) {
        action = newAction;
    }

    /** Sets the timestamp to the new current value */
    public void updateTileTimeStamp() {
        timeStamp = System.currentTimeMillis();
    }
}
