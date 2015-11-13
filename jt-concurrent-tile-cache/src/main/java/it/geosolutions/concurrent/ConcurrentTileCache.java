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

import java.awt.Point;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Observable;
import java.util.Vector;

import javax.media.jai.TileCache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.cache.Weigher;
import com.sun.media.jai.util.CacheDiagnostics;

public class ConcurrentTileCache extends Observable implements TileCache, CacheDiagnostics {

    /** The default memory threshold of the cache. */
    public static final float DEFAULT_MEMORY_THRESHOLD = 0.75F;

    /** The default memory capacity of the cache (16 MB). */
    public static final long DEFAULT_MEMORY_CACHE = 16L * 1024L * 1024L;

    /** The default diagnostic settings */
    public static final boolean DEFAULT_DIAGNOSTIC = false;

    /** The default concurrency settings */
    public static final int DEFAULT_CONCURRENCY_LEVEL = 4;

    /**
     * The tile cache. A Guava Cache is used to cache the tiles. The "key" is a <code>Object</code>. The "value" is a CachedTileImpl.
     */
    private Cache<Object, CachedTileImpl> cacheObject;

    /** The memory capacity of the cache. */
    private long memoryCacheCapacity;

    /** The concurrency level of the cache. */
    private int concurrencyLevel;

    /** The amount of memory to keep after memory control */
    private float memoryCacheThreshold = DEFAULT_MEMORY_THRESHOLD;

    /** diagnosticEnabled enable/disable */
    private volatile boolean diagnosticEnabled = DEFAULT_DIAGNOSTIC;

    /**
     * The listener is used for receiving notification about the removal of a tile for size constraints
     */
    private final RemovalListener<Object, CachedTileImpl> listener = new RemovalListener<Object, CachedTileImpl>() {
        public void onRemoval(RemovalNotification<Object, CachedTileImpl> n) {
            // if a tile is manually removed, the diagnosticEnabled already consider
            // it in
            // the remove() method

            if (diagnosticEnabled) {
                synchronized (this) {
                    if (n.wasEvicted() && n.getCause() == RemovalCause.SIZE) {
                        CachedTileImpl cti = n.getValue();
                        cti.setAction(Actions.REMOVAL_FROM_EVICTION);
                        setChanged();
                        notifyObservers(cti);
                    }
                }

            }
        }
    };

    // diagnostic actions
    /** A list of all the possible diagnostic actions */
    public enum Actions {
        ADDITION(0), MANUAL_REMOVAL(1), REMOVAL_FROM_FLUSH(2), REMOVAL_FROM_EVICTION(
                3), SUBSTITUTION_FROM_ADD(4), UPDATING_TILE_FROM_GETTILE(5), ABOUT_TO_REMOVAL(6);

        private final int action;

        Actions(int value) {
            this.action = value;
        }

        public int valueAction() {
            return action;
        }

    };

    /** Private cache creation method */
    private Cache<Object, CachedTileImpl> buildCache() {
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
        builder.maximumWeight((long) (memoryCacheCapacity * memoryCacheThreshold))
                .concurrencyLevel(concurrencyLevel).weigher(new Weigher<Object, CachedTileImpl>() {
                    public int weigh(Object o, CachedTileImpl cti) {
                        return (int) cti.getTileSize();
                    }
                });

        return builder.build();

    }

    public ConcurrentTileCache() {
        this(DEFAULT_MEMORY_CACHE, DEFAULT_DIAGNOSTIC, DEFAULT_MEMORY_THRESHOLD,
                DEFAULT_CONCURRENCY_LEVEL);
    }

    public ConcurrentTileCache(long memoryCacheCapacity, boolean diagnostic, float mem_threshold,
            int concurrencyLevel) {
        if (memoryCacheCapacity < 0) {
            throw new IllegalArgumentException("Memory capacity too small");
        }
        this.memoryCacheThreshold = mem_threshold;
        this.diagnosticEnabled = diagnostic;
        this.memoryCacheCapacity = memoryCacheCapacity;
        this.concurrencyLevel = concurrencyLevel;

        // cache creation
        cacheObject = buildCache();
    }

    /** Add a new tile to the cache */
    public void add(RenderedImage owner, int tileX, int tileY, Raster data) {
        add(owner, tileX, tileY, data, null);
    }

    /** Add a new tile to the cache */
    public void add(RenderedImage owner, int tileX, int tileY, Raster data,
            Object tileCacheMetric) {
        // when computation fails this method is called with a null raster,
        // avoid logging an extra NPE
        if(data == null) {
            return;
        }
        
        // This tile is not in the cache; create a new CachedTileImpl.
        // else just update.
        Object key = CachedTileImpl.hashKey(owner, tileX, tileY);
        // old tile
        CachedTileImpl cti;
        // create a new tile
        CachedTileImpl cti_new = new CachedTileImpl(owner, tileX, tileY, data, tileCacheMetric);

        // if the tile is already cached
        if (diagnosticEnabled) {
            cti = (CachedTileImpl) cacheObject.asMap().put(key, cti_new);
            synchronized (this) {
                if (cti != null) {
                    cti.updateTileTimeStamp();
                    cti.setAction(Actions.SUBSTITUTION_FROM_ADD);
                    setChanged();
                    notifyObservers(cti);
                    return;
                }

                cti_new.setAction(Actions.ADDITION);
                setChanged();
                notifyObservers(cti_new);
            }
        } else {
            // new tile insertion
            cacheObject.put(key, cti_new);

        }
    }

    /** Removes the selected tile from the cache */
    public void remove(RenderedImage owner, int tileX, int tileY) {
        Object key = CachedTileImpl.hashKey(owner, tileX, tileY);
        // check if the tile is still in cache
        CachedTileImpl cti = (CachedTileImpl) cacheObject.getIfPresent(key);
        // if so the tile is deleted (even if another thread write on it)
        if (cti != null) {
            if (diagnosticEnabled) {
                synchronized (this) {
                    cti.setAction(Actions.ABOUT_TO_REMOVAL);
                    setChanged();
                    notifyObservers(cti);

                    cti = (CachedTileImpl) cacheObject.asMap().remove(key);
                    if (cti != null) {
                        cti.setAction(Actions.MANUAL_REMOVAL);
                        setChanged();
                        notifyObservers(cti);

                    }

                }
            } else {
                cacheObject.invalidate(key);
            }

        }

    }

    /** Retrieves the selected tile from the cache */
    public Raster getTile(RenderedImage owner, int tileX, int tileY) {
        // instantiation of the result raster
        Raster tileData = null;

        Object key = CachedTileImpl.hashKey(owner, tileX, tileY);
        // check if the tile is present
        CachedTileImpl cti = (CachedTileImpl) cacheObject.getIfPresent(key);
        if (cti == null) {
            return null;
        }
        if (diagnosticEnabled) {
            synchronized (this) {

                // Update last-access time for diagnosticEnabled
                cti.updateTileTimeStamp();
                cti.setAction(Actions.UPDATING_TILE_FROM_GETTILE);
                setChanged();
                notifyObservers(cti);
            }
        }
        // return the selected tile
        tileData = cti.getTile();
        return tileData;
    }

    /**
     * Retrieves an array of all tiles in the cache which are owned by the image. May be <code>null</code> if there were no tiles in the cache. The
     * array contains no null entries.
     */
    public Raster[] getTiles(RenderedImage owner) {
        // instantiation of the result array
        Raster[] tilesData = null;
        // total number of tiles present in the cache
        int tileCount = (int) cacheObject.size();

        int size = Math.min(owner.getNumXTiles() * owner.getNumYTiles(), tileCount);

        if (size > 0) {
            int minTx = owner.getMinTileX();
            int minTy = owner.getMinTileY();
            int maxTx = minTx + owner.getNumXTiles();
            int maxTy = minTy + owner.getNumYTiles();

            // arbitrarily set a temporary vector size
            Vector<Raster> tempData = new Vector<Raster>(10, 20);
            // cycle through all the tile in the image and check if they are in the
            // cache...
            for (int y = minTy; y < maxTy; y++) {
                for (int x = minTx; x < maxTx; x++) {

                    Raster rasterTile = getTile(owner, x, y);

                    // ...then add to the vector if present
                    if (rasterTile != null) {
                        tempData.add(rasterTile);
                    }
                }
            }

            int tmpsize = tempData.size();
            if (tmpsize > 0) {
                tilesData = (Raster[]) tempData.toArray(new Raster[tmpsize]);
            }
        }

        return tilesData;

    }

    /**
     * Removes all tiles in the cache which are owned by the image.
     */
    public void removeTiles(RenderedImage owner) {

        if (diagnosticEnabled) {
            int minTx = owner.getMinTileX();
            int minTy = owner.getMinTileY();
            int maxTx = minTx + owner.getNumXTiles();
            int maxTy = minTy + owner.getNumYTiles();

            for (int y = minTy; y < maxTy; y++) {
                for (int x = minTx; x < maxTx; x++) {
                    remove(owner, x, y);
                }
            }
        } else {
            int minTx = owner.getMinTileX();
            int minTy = owner.getMinTileY();
            int maxTx = minTx + owner.getNumXTiles();
            int maxTy = minTy + owner.getNumYTiles();

            for (int y = minTy; y < maxTy; y++) {
                for (int x = minTx; x < maxTx; x++) {
                    remove(owner, x, y);
                }
            }
        }
    }

    /**
     * Adds all tiles in the Point array which are owned by the image.
     */
    public void addTiles(RenderedImage owner, Point[] tileIndices, Raster[] tiles,
            Object tileCacheMetric) {
        // cycle through the array for adding tiles
        for (int i = 0; i < tileIndices.length; i++) {
            int tileX = tileIndices[i].x;
            int tileY = tileIndices[i].y;
            Raster tile = tiles[i];
            add(owner, tileX, tileY, tile, tileCacheMetric);

        }

    }

    /**
     * Retrieves an array of tiles in the cache which are specified by the Point array and owned by the image. May be <code>null</code> if there were
     * not in the cache. The array contains null entries.
     */
    public Raster[] getTiles(RenderedImage owner, Point[] tileIndices) {
        // instantiation of the array
        Raster[] tilesData = new Raster[tileIndices.length];
        // cycle thorugh the array for getting tiles
        for (int i = 0; i < tilesData.length; i++) {
            int tileX = tileIndices[i].x;
            int tileY = tileIndices[i].y;

            Raster rasterData = (Raster) getTile(owner, tileX, tileY);
            // even if the tile is not present it is inserted in the array
            if (rasterData == null) {
                tilesData[i] = null;

            } else {
                // found tile in cache
                tilesData[i] = rasterData;
            }
        }

        return tilesData;
    }

    /** Removes all tiles present in the cache without checking for the image owner */
    public synchronized void flush() {
        // It is necessary to clear all the elements
        // from the old cache.
        if (diagnosticEnabled) {
            // Creation of an iterator for accessing to every tile in the cache
            Iterator<Object> keys = cacheObject.asMap().keySet().iterator();
            // cycle across the cache for removing and updating every tile
            while (keys.hasNext()) {
                Object key = keys.next();
                CachedTileImpl cti = (CachedTileImpl) cacheObject.asMap().remove(key);

                // diagnosticEnabled

                cti.setAction(Actions.REMOVAL_FROM_FLUSH);
                setChanged();
                notifyObservers(cti);

            }
        } else {
            cacheObject.invalidateAll();
        }
        // cache.invalidateAll();
        cacheObject = buildCache();

    }

    /**
     * Not Supported
     * 
     * @throws UnsupportedOperationException
     */
    public void memoryControl() {
        throw new UnsupportedOperationException("Memory Control not supported");

    }

    /**
     * Not Supported
     * 
     * @throws UnsupportedOperationException
     */
    public void setTileCapacity(int tileCapacity) {
        throw new UnsupportedOperationException("Deprecated Operation");
    }

    /**
     * Not Supported
     * 
     * @throws UnsupportedOperationException
     */
    public int getTileCapacity() {
        throw new UnsupportedOperationException("Deprecated Operation");
    }

    /** Sets the cache memory capacity and then flush and rebuild the cache */
    public synchronized void setMemoryCapacity(long memoryCacheCapacity) {
        if (memoryCacheCapacity < 0) {
            throw new IllegalArgumentException("Memory capacity too small");
        } else {
            this.memoryCacheCapacity = memoryCacheCapacity;
            flush();

        }

    }

    /** Retrieve the cache memory capacity */
    public long getMemoryCapacity() {
        return memoryCacheCapacity;
    }

    /** Sets the cache memory threshold and then flush and rebuild the cache */
    public synchronized void setMemoryThreshold(float mt) {
        if (mt < 0.0F || mt > 1.0F) {
            throw new IllegalArgumentException("Memory threshold should be between 0 and 1");
        } else {
            memoryCacheThreshold = mt;
            flush();

        }

    }

    /** Retrieve the cache memory threshold */
    public float getMemoryThreshold() {
        return memoryCacheThreshold;
    }

    /** Sets the cache ConcurrencyLevel and then flush and rebuild the cache */
    public synchronized void setConcurrencyLevel(int concurrency) {
        if (concurrency < 1) {
            throw new IllegalArgumentException("ConcurrencyLevel must be at least 1");
        } else {
            concurrencyLevel = concurrency;
            flush();

        }

    }

    /** Retrieve the cache concurrency level */
    public int getConcurrencyLevel() {
        return concurrencyLevel;
    }

    /**
     * Not Supported
     * 
     * @throws UnsupportedOperationException
     */
    public void setTileComparator(Comparator comparator) {
        throw new UnsupportedOperationException("Comparator not supported");

    }

    /**
     * Not Supported
     * 
     * @throws UnsupportedOperationException
     */
    public Comparator getTileComparator() {
        throw new UnsupportedOperationException("Comparator not supported");
    }

    /** Disables diagnosticEnabled for the observers */
    public synchronized void disableDiagnostics() {
        diagnosticEnabled = false;
        flush();

    }

    /** Enables diagnosticEnabled for the observers */
    public synchronized void enableDiagnostics() {
        diagnosticEnabled = true;
        flush();

    }

    /** Retrieves the hit count from the cache statistics */
    public long getCacheHitCount() {
        if (diagnosticEnabled) {
            return cacheObject.stats().hitCount();
        }
        return 0;
    }

    /** Retrieves the current memory size of the cache */
    public synchronized long getCacheMemoryUsed() {
        Iterator<Object> keys = cacheObject.asMap().keySet().iterator();
        long memoryUsed = 0;
        while (keys.hasNext()) {
            Object key = keys.next();
            CachedTileImpl cti = (CachedTileImpl) cacheObject.getIfPresent(key);
            memoryUsed += cti.getTileSize();
        }
        return memoryUsed;
    }

    /** Retrieves the miss count from the cache statistics */
    public long getCacheMissCount() {
        if (diagnosticEnabled) {
            return cacheObject.stats().missCount();
        }
        return 0;

    }

    /** Retrieves the number of tiles in the cache */
    public long getCacheTileCount() {
        return cacheObject.size();
    }

    /**
     * Not Supported
     * 
     * @throws UnsupportedOperationException
     */
    public void resetCounts() {
        throw new UnsupportedOperationException("Operation not supported");
    }

}
