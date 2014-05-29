package it.geosolutions.concurrent;

import it.geosolutions.concurrent.ConcurrentTileCache.Actions;

import java.awt.Point;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Observable;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.media.jai.TileCache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.cache.Weigher;
import com.sun.media.jai.util.CacheDiagnostics;

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
    private Cache<Object, Set<Object>> multimap;

    /** The memory capacity of the cache. */
    private long memoryCacheCapacity;

    /** The concurrency level of the cache. */
    private int concurrencyLevel;

    /** The amount of memory to keep after memory control */
    private float memoryCacheThreshold = DEFAULT_MEMORY_THRESHOLD;

    /** diagnosticEnabled enable/disable */
    private volatile boolean diagnosticEnabled = DEFAULT_DIAGNOSTIC;

    /**
     * The listener is used for receiving notification about the removal of a tile
     */
    private final RemovalListener<Object, CachedTileImpl> listener;

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

        // Listener creation
        listener = createListener(diagnostic);

        // cache creation
        cacheObject = buildCache();

        // multimap creation
        multimap = CacheBuilder.newBuilder().concurrencyLevel(concurrencyLevel).build();
    }

    /** Add a new tile to the cache */
    public void add(RenderedImage owner, int tileX, int tileY, Raster data) {
        add(owner, tileX, tileY, data, null);
    }

    /** Add a new tile to the cache */
    public void add(RenderedImage owner, int tileX, int tileY, Raster data, Object tileCacheMetric) {
        // This tile is not in the cache; create a new CachedTileImpl.
        // else just update.
        Object key = CachedTileImpl.hashKey(owner, tileX, tileY);
        // Key associated to the image
        Object imageKey = CachedTileImpl.hashKey(owner);
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
                }

                cti_new.setAction(Actions.ADDITION);
                setChanged();
                notifyObservers(cti_new);
                updateMultiMap(key, imageKey);
            }
        } else {
            // new tile insertion
            cacheObject.put(key, cti_new);
            // Atomically adds a new Map if needed and then adds a new tile inside the MultiMap.
            updateMultiMap(key, imageKey);
        }
    }

    /** Removes the selected tile from the cache */
    public void remove(RenderedImage owner, int tileX, int tileY) {
        // Calculation of the tile key
        Object key = CachedTileImpl.hashKey(owner, tileX, tileY);
        // remove operation
        removeTileFromKey(key);
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

        Set<Object> keys = multimap.getIfPresent(imageKey);

        if (keys == null || keys.isEmpty()) {
            return tilesData;
        }

        Iterator<Object> it = keys.iterator();

        if (it.hasNext()) {
            // arbitrarily set a temporary vector size
            Vector<Raster> tempData = new Vector<Raster>(10, 20);
            // cycle through all the tile keys present in the multimap and check if they are in the
            // cache...
            while (it.hasNext()) {
                Object key = it.next();
                Raster rasterTile = getTileFromKey(key);

                // ...then add to the vector if present
                if (rasterTile != null) {
                    tempData.add(rasterTile);
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

        // Calculation of the key associated to the image
        Object imageKey = CachedTileImpl.hashKey(owner);

        if (diagnosticEnabled) {
            synchronized (this) {
                Set<Object> keys = multimap.getIfPresent(imageKey);
                if (keys != null) {
                    Iterator<Object> it = keys.iterator();
                    while (it.hasNext()) {
                        Object key = it.next();
                        removeTileFromKey(key);
                    }
                }
            }
        } else {
            // Get the keys associated to the image and remove them
            Set<Object> keys = multimap.getIfPresent(imageKey);
            if (keys != null) {
                cacheObject.invalidateAll(keys);
                // Then remove the multimap
                multimap.invalidate(imageKey);
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

        // multimap creation
        multimap = CacheBuilder.newBuilder().concurrencyLevel(concurrencyLevel).build();

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
        Iterator<Object> keys = multimap.asMap().keySet().iterator();
        long memoryUsed = 0;
        while (keys.hasNext()) {
            Object keyImage = keys.next();
            Set<Object> tileKeys = multimap.getIfPresent(keyImage);
            if (tileKeys != null) {
                int numTiles = tileKeys.size();
                Iterator<Object> iterator = tileKeys.iterator();
                if (numTiles > 0) {
                    CachedTileImpl cti = null;
                    while (cti == null && iterator.hasNext()) {
                        cti = cacheObject.getIfPresent(iterator.next());
                    }
                    if (cti != null) {
                        memoryUsed += (cti.getTileSize() * numTiles);
                    }
                }
            }
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

    private RemovalListener<Object, CachedTileImpl> createListener(final boolean diagnostic) {
        return new RemovalListener<Object, CachedTileImpl>() {
            public void onRemoval(RemovalNotification<Object, CachedTileImpl> n) {
                // if a tile is manually removed, the diagnosticEnabled already consider
                // it in
                // the remove() method

                if (diagnostic) {
                    synchronized (this) {
                        if (n.wasEvicted()) {
                            CachedTileImpl cti = n.getValue();
                            cti.setAction(Actions.REMOVAL_FROM_EVICTION);
                            removeFromMultiMap(cti);
                            setChanged();
                            notifyObservers(cti);
                        }
                    }
                } else {
                    if (n.wasEvicted()) {
                        CachedTileImpl cti = n.getValue();
                        removeFromMultiMap(cti);
                    }
                }
            }
        };
    }

    private void removeFromMultiMap(CachedTileImpl cti) {
        Object key = cti.getKey();
        Object imageKey = cti.getImageKey();
        Set<Object> tileKeys = multimap.getIfPresent(imageKey);
        if (tileKeys != null) {
            tileKeys.remove(key);
            if (tileKeys.isEmpty()) {
                multimap.invalidate(imageKey);
            }
        }
    }

    /** Private cache creation method */
    private Cache<Object, CachedTileImpl> buildCache() {
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
        builder.maximumWeight((long) (memoryCacheCapacity * memoryCacheThreshold))
                .concurrencyLevel(concurrencyLevel).weigher(new Weigher<Object, CachedTileImpl>() {
                    public int weigh(Object o, CachedTileImpl cti) {
                        return (int) cti.getTileSize();
                    }
                });
        builder.removalListener(listener);
        if (diagnosticEnabled) {
            builder.recordStats();
        }

        return builder.build();
    }

    private void updateMultiMap(Object key, Object imageKey) {
        Set<Object> tileKeys = multimap.getIfPresent(imageKey);
        synchronized (this) {
            if (tileKeys == null) {
                tileKeys = new ConcurrentSkipListSet<Object>();
                multimap.put(imageKey, tileKeys);
            }
        }
        tileKeys.add(key);
    }

    private void removeTileFromKey(Object key) {
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

    private Raster getTileFromKey(Object key) {
        Raster tileData = null;
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
}
