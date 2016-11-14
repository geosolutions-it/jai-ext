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

import java.awt.Point;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.TileCache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.cache.Weigher;
import com.sun.media.jai.util.CacheDiagnostics;

/**
 * This implementation of the TileCache class uses a Guava Cache and a multimap in order to provide a better concurrency handling. The first object
 * contains all the cached tiles while the second one contains the mapping of the tile keys for each image. This class implements
 * {@link CacheDiagnostics} in order to get the statistics associated to the {@link TileCache}. The user can define the cache memory capacity, the
 * concurrency level (which indicates in how many segments the cache must be divided), the threshold of the total memory to use and a boolean
 * indicating if the diagnostic must be enabled.
 * 
 * @author Nicola Lagomarsini GeoSolutions S.A.S.
 * 
 */
public class ConcurrentTileCacheMultiMap extends Observable implements TileCache, CacheDiagnostics {

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

    /**
     * A concurrent multimap used for mapping the tile keys for each image
     */
    private ConcurrentHashMap<Object, Set<Object>> multimap;

    /** The memory capacity of the cache. */
    private long memoryCacheCapacity;

    /** The concurrency level of the cache. */
    private int concurrencyLevel;

    /** The amount of memory to keep after memory control */
    private float memoryCacheThreshold = DEFAULT_MEMORY_THRESHOLD;

    /** diagnosticEnabled enable/disable */
    private volatile boolean diagnosticEnabled = DEFAULT_DIAGNOSTIC;

    /**
     * Logger to use for reporting the informations about the TileCache operations.
     */
    private final static Logger LOGGER = Logger.getLogger(ConcurrentTileCacheMultiMap.class
            .toString());

    /**
     * Memory overhead in bytes for tracking the tiles used by each image
     */
    private static final long TILE_TRACKING_OVERHEAD = 50;

    public ConcurrentTileCacheMultiMap() {
        this(DEFAULT_MEMORY_CACHE, DEFAULT_DIAGNOSTIC, DEFAULT_MEMORY_THRESHOLD,
                DEFAULT_CONCURRENCY_LEVEL);
    }

    public ConcurrentTileCacheMultiMap(long memoryCacheCapacity, boolean diagnostic,
            float mem_threshold, int concurrencyLevel) {
        if (memoryCacheCapacity < 0) {
            throw new IllegalArgumentException("Memory capacity too small");
        }
        this.memoryCacheThreshold = mem_threshold;
        this.diagnosticEnabled = diagnostic;
        this.memoryCacheCapacity = memoryCacheCapacity;
        this.concurrencyLevel = concurrencyLevel;

        // cache creation
        cacheObject = buildCache();

        // multimap creation
        multimap = new ConcurrentHashMap<Object, Set<Object>>();
    }

    /** Add a new tile to the cache */
    public void add(RenderedImage owner, int tileX, int tileY, Raster data) {
        add(owner, tileX, tileY, data, null);
    }

    /** Add a new tile to the cache */
    public void add(RenderedImage owner, int tileX, int tileY, Raster data, Object tileCacheMetric) {
        // when computation fails this method is called with a null raster,
        // avoid logging an extra NPE
        if(data == null) {
            return;
        }
        
        // This tile is not in the cache; create a new CachedTileImpl.
        // else just update.

        // Key associated to the image
        Object imageKey = CachedTileImpl.hashKey(owner);

        // old tile
        CachedTileImpl cti;
        // create a new tile
        CachedTileImpl cti_new = new CachedTileImpl(owner, tileX, tileY, data, tileCacheMetric);

        if (diagnosticEnabled) {
            // if the tile is already cached
            cti = (CachedTileImpl) cacheObject.asMap().putIfAbsent(cti_new.key, cti_new);
            synchronized (cacheObject) {
                if (cti != null) {
                    cti.updateTileTimeStamp();
                    cti.setAction(Actions.SUBSTITUTION_FROM_ADD);
                    setChanged();
                    notifyObservers(cti);
                }

                // Update the tile action in order to notify it to the observers
                cti_new.setAction(Actions.ADDITION);
                setChanged();
                notifyObservers(cti_new);
                updateMultiMap(cti_new.key, imageKey);
            }
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Added new Tile Image key " + imageKey);
            }
            // new tile insertion
            cacheObject.asMap().putIfAbsent(cti_new.key, cti_new);
            // Atomically adds a new Map if needed and then adds a new tile inside the MultiMap.
            updateMultiMap(cti_new.key, imageKey);
        }
    }

    private long getTileSize(CachedTileImpl cti) {
        return cti.getTileSize() + TILE_TRACKING_OVERHEAD;
    }

    /** Removes the selected tile from the cache */
    public void remove(RenderedImage owner, int tileX, int tileY) {
        // Calculation of the tile key
        Object key = CachedTileImpl.hashKey(owner, tileX, tileY);
        // remove operation
        removeTileByKey(key);
    }

    /** Retrieves the selected tile from the cache */
    public Raster getTile(RenderedImage owner, int tileX, int tileY) {
        // Calculation of the tile key
        Object key = CachedTileImpl.hashKey(owner, tileX, tileY);
        // Get operation
        return getTileFromKey(key);
    }

    /**
     * Retrieves an array of all tiles in the cache which are owned by the image. May be <code>null</code> if there were no tiles in the cache. The
     * array contains no null entries.
     */
    public Raster[] getTiles(RenderedImage owner) {
        // instantiation of the result array
        Raster[] tilesData = null;

        // Calculation of the key associated to the image
        Object imageKey = CachedTileImpl.hashKey(owner);

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Getting image Tiles Image key " + imageKey);
        }
        // Selection of the tile keys for the image
        Set<Object> keys = multimap.get(imageKey);

        // If no key is found then a null object is returned
        if (keys == null || keys.isEmpty()) {
            return tilesData;
        }

        // Else it is created an iterator on the tile keys
        Iterator<Object> it = keys.iterator();
        // Another check on the iterator
        if (it.hasNext()) {
            // arbitrarily set a temporary vector size
            Vector<Raster> tempData = new Vector<Raster>(10, 20);
            // cycle through all the tile keys present in the multimap and check if they are in the
            // cache...
            while (it.hasNext()) {
                Object key = it.next();
                // get the tile from the key
                Raster rasterTile = getTileFromKey(key);

                // ...then add to the vector if present
                if (rasterTile != null) {
                    tempData.add(rasterTile);
                }
            }
            // Vector size
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

        // Calculation of the key associated to the image
        Object imageKey = CachedTileImpl.hashKey(owner);

        if (diagnosticEnabled) {
            // Selection of the keys associated to the image and removal of each of them
            Set<Object> keys = multimap.remove(imageKey);
            if (keys != null) {
                Iterator<Object> it = keys.iterator();
                while (it.hasNext()) {
                    Object key = it.next();
                    removeTileByKey(key);
                }
            }
        } else {
            // Get the keys associated to the image and remove them
            Set<Object> keys = multimap.remove(imageKey);
            if (keys != null) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Removing image Tiles Image key " + imageKey);
                }
                cacheObject.invalidateAll(keys);
            }
        }
    }

    /**
     * Adds all tiles in the Point array which are owned by the image.
     */
    public void addTiles(RenderedImage owner, Point[] tileIndices, Raster[] tiles,
            Object tileCacheMetric) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Addeding Tiles");
        }
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

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Getting Tiles at the selected positions");
        }
        // cycle through the array for getting tiles
        for (int i = 0; i < tilesData.length; i++) {
            int tileX = tileIndices[i].x;
            int tileY = tileIndices[i].y;

            Raster rasterData = getTile(owner, tileX, tileY);
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
    public void flush() {
        synchronized (cacheObject) {
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
                // Invalidation of all the keys of the cache
                cacheObject.invalidateAll();
            }

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Flushing cache");
            }

            // Cache creation
            cacheObject = buildCache();
            // multimap creation
            multimap = new ConcurrentHashMap<Object, Set<Object>>();
        }
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
    public void setMemoryCapacity(long memoryCacheCapacity) {
        synchronized (cacheObject) {
            if (memoryCacheCapacity < 0) {
                throw new IllegalArgumentException("Memory capacity too small");
            } else {
                this.memoryCacheCapacity = memoryCacheCapacity;
                // The flush is done in order to rebuild the cache with the new settings
                flush();
            }
        }
    }

    /** Retrieve the cache memory capacity */
    public long getMemoryCapacity() {
        return memoryCacheCapacity;
    }

    /** Sets the cache memory threshold and then flush and rebuild the cache */
    public void setMemoryThreshold(float mt) {
        synchronized (cacheObject) {
            if (mt < 0.0F || mt > 1.0F) {
                throw new IllegalArgumentException("Memory threshold should be between 0 and 1");
            } else {
                memoryCacheThreshold = mt;
                // The flush is done in order to rebuild the cache with the new settings
                flush();

            }
        }
    }

    /** Retrieve the cache memory threshold */
    public float getMemoryThreshold() {
        return memoryCacheThreshold;
    }

    /** Sets the cache ConcurrencyLevel and then flush and rebuild the cache */
    public void setConcurrencyLevel(int concurrency) {
        synchronized (cacheObject) {
            if (concurrency < 1) {
                throw new IllegalArgumentException("ConcurrencyLevel must be at least 1");
            } else {
                concurrencyLevel = concurrency;
                // The flush is done in order to rebuild the cache with the new settings
                flush();

            }
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
    public void disableDiagnostics() {
        synchronized (cacheObject) {
            diagnosticEnabled = false;
            // The flush is done in order to rebuild the cache with the new settings
            flush();
        }
    }

    /** Enables diagnosticEnabled for the observers */
    public void enableDiagnostics() {
        synchronized (cacheObject) {
            diagnosticEnabled = true;
            // The flush is done in order to rebuild the cache with the new settings
            flush();
        }
    }

    /** Retrieves the hit count from the cache statistics */
    public long getCacheHitCount() {
        if (diagnosticEnabled) {
            return cacheObject.stats().hitCount();
        }
        return 0;
    }

    /** Retrieves the current memory size of the cache */
    public long getCacheMemoryUsed() {
        Iterator<Object> keys = cacheObject.asMap().keySet().iterator();
        long memoryUsed = 0;
        while (keys.hasNext()) {
            Object key = keys.next();
            CachedTileImpl cti = (CachedTileImpl) cacheObject.getIfPresent(key);
            memoryUsed += getTileSize(cti);
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

    /**
     * Creation of a listener to use for handling the removed tiles
     * 
     * @param diagnostic
     * @return
     */
    private RemovalListener<Object, CachedTileImpl> createListener(final boolean diagnostic) {
        return new RemovalListener<Object, CachedTileImpl>() {
            public void onRemoval(RemovalNotification<Object, CachedTileImpl> n) {
                // if a tile is manually removed, the diagnosticEnabled already consider
                // it in
                // the remove() method

                if (diagnostic) {
                    synchronized (cacheObject) {
                        CachedTileImpl cti = n.getValue();
                        // Update of the tile action
                        if (n.wasEvicted()) {
                            cti.setAction(Actions.REMOVAL_FROM_EVICTION);
                        } else {
                            cti.setAction(Actions.MANUAL_REMOVAL);
                        }
                        // Removal from the multimap
                        removeTileFromMultiMap(cti);
                        setChanged();
                        notifyObservers(cti);
                    }
                } else {
                    CachedTileImpl cti = n.getValue();
                    if (n.getCause() == RemovalCause.SIZE) {
                        // Logging if the tile is removed because the size is exceeded
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.fine("Removing from MultiMap for size");
                        }
                    }
                    removeTileFromMultiMap(cti);
                }
            }
        };
    }

    /**
     * Method for removing the tile keys from the multimap. If the KeySet associated to the image is empty, it is removed from the multimap.
     * 
     * @param cti
     */
    private void removeTileFromMultiMap(CachedTileImpl cti) {
        // Tile key
        Object key = cti.getKey();
        // Image key
        Object imageKey = cti.getImageKey();
        // KeySet associated to the image
        Set<Object> tileKeys = multimap.get(imageKey);

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Removing tile from MultiMap Image key " + imageKey);
        }

        if (tileKeys != null) {
            // Removal of the keys
            tileKeys.remove(key);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Removed Tile Image key " + imageKey);
            }
            // If the KeySet is empty then it is removed from the multimap
            if (tileKeys.isEmpty()) {
                multimap.remove(imageKey);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Removed image SET Image key " + imageKey);
                }
            }
        }
    }

    /** Private cache creation method */
    private Cache<Object, CachedTileImpl> buildCache() {
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
        builder.maximumWeight((long) (memoryCacheCapacity * memoryCacheThreshold))
                .concurrencyLevel(concurrencyLevel).weigher(new Weigher<Object, CachedTileImpl>() {
                    public int weigh(Object o, CachedTileImpl cti) {
                        return (int) getTileSize(cti);
                    }
                });
        // Setting of the listener
        builder.removalListener(createListener(diagnosticEnabled));
        // Enable statistics only when the diagnostic flag is set to true;
        if(diagnosticEnabled){
            builder.recordStats();
        }
        
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Building Cache");
        }

        return builder.build();
    }

    /**
     * Update of the multimap when a tile is added.
     * 
     * @param key
     * @param imageKey
     */
    private void updateMultiMap(Object key, Object imageKey) {
        Set<Object> tileKeys = null;
        // Check if the multimap contains the keys for the image
        tileKeys = multimap.get(imageKey);
        if (tileKeys == null) {
            // If no key is present then a new KeySet is created and then added to the multimap
            tileKeys = new ConcurrentSkipListSet<Object>();
            Set<Object> previousTileKeys = multimap.putIfAbsent(imageKey, tileKeys);
            if(previousTileKeys != null) {
                tileKeys = previousTileKeys;
            }
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Created new Set for the image Image key " + imageKey);
            }
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Added Tile to the set Image key " + imageKey);
        }
        // Finally the tile key is added.
        tileKeys.add(key);
    }

    /**
     * Removes the tile associated to the key.
     * 
     * @param key
     */
    private void removeTileByKey(Object key) {
        // check if the tile is still in cache
        CachedTileImpl cti = (CachedTileImpl) cacheObject.getIfPresent(key);
        // if so the tile is deleted (even if another thread write on it)
        if (cti != null) {
            if (diagnosticEnabled) {
                synchronized (cacheObject) {
                    // Upgrade the tile action
                    cti.setAction(Actions.ABOUT_TO_REMOVAL);
                    setChanged();
                    notifyObservers(cti);
                    // Removal of the tile
                    cti = (CachedTileImpl) cacheObject.asMap().remove(key);
                    if (cti != null) {
                        // Upgrade the tile action
                        cti.setAction(Actions.MANUAL_REMOVAL);
                        setChanged();
                        notifyObservers(cti);
                    }
                }
            } else {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Removed Tile Image key " + cti.getImageKey());
                }
                // Discard the tile from the cache
                cacheObject.invalidate(key);
            }
        }
    }

    /**
     * Gets the tile associated to the key.
     * 
     * @param key
     * @return
     */
    private Raster getTileFromKey(Object key) {
        Raster tileData = null;
        // check if the tile is present
        CachedTileImpl cti = (CachedTileImpl) cacheObject.asMap().get(key);
        // If not tile is found, null is returned
        if (cti == null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Null Tile returned");
            }
            return null;
        }
        if (diagnosticEnabled) {
            synchronized (cacheObject) {

                // Update last-access time for diagnosticEnabled
                cti.updateTileTimeStamp();
                cti.setAction(Actions.UPDATING_TILE_FROM_GETTILE);
                setChanged();
                notifyObservers(cti);
            }
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Get the selected tile Image key " + cti.getImageKey());
        }
        // return the selected tile
        tileData = cti.getTile();
        return tileData;
    }
}
