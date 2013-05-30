package it.geosolutions.concurrentlinked;

import it.geosolutions.concurrent.CachedTileImpl;
import it.geosolutions.concurrent.ConcurrentTileCache.Actions;
import java.awt.Point;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Observable;
import javax.media.jai.TileCache;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.EvictionListener;
import com.googlecode.concurrentlinkedhashmap.Weigher;
import com.sun.media.jai.util.CacheDiagnostics;

public class ConcurrentLinkedCache extends Observable implements TileCache,
        CacheDiagnostics {

/** Default value for memory threshold */
public static final float DEFAULT_MEMORY_THRESHOLD = 0.75F;

/** Default value for cache memory */
public static final long DEFAULT_MEMORY_CACHE = 16L * 1024L * 1024L;

/** Default boolean for diagnostic mode */
public static final boolean DEFAULT_DIAGNOSTIC = false;

/** Default ConcurrentLinkedHashMap concurrency level */
public static final int DEFAULT_CONCURRENCY_LEVEL = 16;

/** The real cache */
private ConcurrentLinkedHashMap<Object, CachedTileImpl> cacheObject;

/** Cache memory capacity */
private long memoryCacheCapacity = DEFAULT_MEMORY_CACHE;

/** concurrency level of the ConcurrentLinkedHashMap */
private int concurrencyLevel = DEFAULT_CONCURRENCY_LEVEL;

/** Cache memory threshold (typically 0.75F) */
private float memoryCacheThreshold = DEFAULT_MEMORY_THRESHOLD;

/**
 * Current diagnostic mode (enabled or not). This variable is set to volatile
 * for visibility
 */
private volatile boolean diagnosticEnabled = DEFAULT_DIAGNOSTIC;

/** Current number of cache miss */
private int missNumber;

/** Current number of cache hits */
private int hitNumber;

/**
 * Eviction listener for catching the tile evicted by the
 * ConcurrentLinkedHashMap and, if diagnostic is enabled, the eviction is
 * notified to the observer
 */
private EvictionListener<Object, CachedTileImpl> listener = new EvictionListener<Object, CachedTileImpl>() {

    public void onEviction(Object key, CachedTileImpl oldValue) {
        if (diagnosticEnabled) {
            synchronized (this) {
                oldValue.updateTileTimeStamp();
                oldValue.setAction(Actions.REMOVAL_FROM_EVICTION);
                setChanged();
                notifyObservers(oldValue);
            }

        }
    }

};

/* Simple constructor */
public ConcurrentLinkedCache() {
    this(DEFAULT_MEMORY_CACHE, DEFAULT_CONCURRENCY_LEVEL,
            DEFAULT_MEMORY_THRESHOLD, DEFAULT_DIAGNOSTIC);
}

/* Parameterized constructor */
public ConcurrentLinkedCache(long memoryCacheCapacity, int concurrencyLevel,
        float memoryCacheThreshold, boolean diagnosticEnabled) {
    this.concurrencyLevel = concurrencyLevel;
    this.memoryCacheCapacity = memoryCacheCapacity;
    this.diagnosticEnabled = diagnosticEnabled;
    this.memoryCacheThreshold = memoryCacheThreshold;
    /* the cache instantiation is done in the buildLinkedCache() method */
    cacheObject = buildLinkedCache();
    /* the counters are set to 0 */
    hitNumber = 0;
    missNumber = 0;
    System.err.println("Using ConcurrentLinkedCache");
}

/** Private method for building the cache */
private ConcurrentLinkedHashMap<Object, CachedTileImpl> buildLinkedCache() {
    /* Builder instantiation */
    ConcurrentLinkedHashMap.Builder<Object, CachedTileImpl> builder = new ConcurrentLinkedHashMap.Builder<Object, CachedTileImpl>();
    builder.concurrencyLevel(concurrencyLevel)
            .maximumWeightedCapacity(
                    (long) (memoryCacheCapacity * memoryCacheThreshold))
            /* The weigher is used for weighing every entry */
            .weigher(new Weigher<CachedTileImpl>() {
                public int weightOf(CachedTileImpl tile) {
                    return (int) ((CachedTileImpl) tile).getTileSize();
                }
            });
    /* Listener is used only with diagnostic */
    if (diagnosticEnabled) {
        builder.listener(listener);
    }
    /* Cache creation */
    return builder.build();
}

public void add(RenderedImage image, int xTile, int yTile, Raster dataTile) {
    this.add(image, xTile, yTile, dataTile, null);
}

/**
 * This method adds a new tile in cache and, if diagnostic is enabled, notify
 * observers
 */
public void add(RenderedImage image, int xTile, int yTile, Raster dataTile,
        Object tileMetric) {
    /* Tile key calculation */
    Object key = CachedTileImpl.hashKey(image, xTile, yTile);
    /* New tile creation */
    CachedTileImpl newValue = new CachedTileImpl(image, xTile, yTile, dataTile,tileMetric);
    /* If diagnostic is enabled the tile status is changed */
    if (diagnosticEnabled) {
        synchronized (this) {
            /* Updates the new tile status and notifies the observers */
            newValue.setAction(Actions.ADDITION);
            setChanged();
            notifyObservers(newValue);
            /* Puts the new value in cache and takes the old one */
            CachedTileImpl oldValue = cacheObject.put(key, newValue);
            if (oldValue != null) {
                hitNumber++;
                /* Update the old tile status and notify the observers */
                oldValue.updateTileTimeStamp();
                oldValue.setAction(Actions.SUBSTITUTION_FROM_ADD);
                setChanged();
                notifyObservers(oldValue);
                return;
            }
        }
    } else {
        /* Simply put the value in cache */
        cacheObject.put(key, newValue);
    }
}

/** This method add an array of tiles at given positions */
public void addTiles(RenderedImage image, Point[] positions,
        Raster[] dataTiles, Object tileMetric) {
    for (int i = 0; i < positions.length; i++) {
        int xIndex = positions[i].x;
        int yIndex = positions[i].y;
        add(image, xIndex, yIndex, dataTiles[i], tileMetric);
    }
}

/**
 * This method flushes the cache and, if diagnostic is enabled, reset the
 * counters and, for every tile, update tile status
 */
public synchronized void flush() {
    if (diagnosticEnabled) {
            /*
             * An iterator of all the key in the cache is used for updating
             * every tile and the removing it
             */
            CachedTileImpl oldValue;
            Iterator<Object> iter = cacheObject.keySet().iterator();
            while (iter.hasNext()) {
                oldValue = cacheObject.remove(iter.next());
                oldValue.setAction(Actions.REMOVAL_FROM_FLUSH);
                oldValue.updateTileTimeStamp();
                setChanged();
                notifyObservers(oldValue);
            }
            /* Counter reset */
            hitNumber = 0;
            missNumber = 0;
    } else {
        /* Simple cache clearing */
        cacheObject.clear();
    }
    /* The cache is rebuilt */
    cacheObject = buildLinkedCache();
}

/** This method gets the current cache memory capacity */
public long getMemoryCapacity() {
    return memoryCacheCapacity;
}

/** This method gets the current cache memory threshold */
public float getMemoryThreshold() {
    return memoryCacheThreshold;
}

/**
 * This method gets a tile raster from his (x,y) tile coordinates and the image
 * reference
 */
public Raster getTile(RenderedImage image, int xTile, int yTile) {
    /* Key creation */
    Object key = CachedTileImpl.hashKey(image, xTile, yTile);
    if (diagnosticEnabled) {
        synchronized (this) {
            /*
             * In diagnostic mode the oldvalue, if present, is updated and
             * retrieved
             */
            CachedTileImpl oldValue = cacheObject.get(key);
            if (oldValue != null) {
                /* if the tile is present the hit number is increased */
                hitNumber++;
                oldValue.setAction(Actions.UPDATING_TILE_FROM_GETTILE);
                oldValue.updateTileTimeStamp();
                /* Observers notifications */
                setChanged();
                notifyObservers(oldValue);
                return oldValue.getTile();
            } else {
                /* if the tile is not present the miss number is increased */
                missNumber++;
                return null;
            }
        }
    } else {
        /* The tile is returned if present, else null returned */
        CachedTileImpl oldValue = cacheObject.get(key);
        if (oldValue != null) {
            return oldValue.getTile();
        } else {
            return null;
        }
    }
}

/** All the tile of the specific image are returned */
public Raster[] getTiles(RenderedImage image) {    
    // instantiation of the result array
    Raster[] tilesData = null;
    // total number of tiles present in the cache
    int tileCount = (int) cacheObject.size();

    int size = Math.min(image.getNumXTiles() * image.getNumYTiles(), tileCount);

    if (size > 0) {
        int minTx = image.getMinTileX();
        int minTy = image.getMinTileY();
        int maxTx = minTx + image.getNumXTiles();
        int maxTy = minTy + image.getNumYTiles();

        // arbitrarily set a temporary vector size
        ArrayList<Raster> tempData = new ArrayList<Raster>();
        // cycle through all the tile in the image and check if they are in the
        // cache...
        for (int y = minTy; y < maxTy; y++) {
            for (int x = minTx; x < maxTx; x++) {

                Raster rasterTile = getTile(image, x, y);

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

/** This method returns an array of tiles at the given positions */
public Raster[] getTiles(RenderedImage image, Point[] positions) {
    /* Initialization of an array of rasters */
    Raster[] tileData = new Raster[positions.length];
    if (diagnosticEnabled) {
        synchronized (this) {
            /*
             * If the diagnostic mode is enabled, the tiles are returned from
             * the method getTile which updates the tile status
             */
            for (int j = 0; j < positions.length; j++) {
                int xTile = positions[j].x;
                int yTile = positions[j].y;
                tileData[j] = getTile(image, xTile, yTile);
            }
        }
    } else {
        /*
         * Else, they are simply returned by the ConcurrentLinkedHashMap.get()
         * method
         */
        for (int j = 0; j < positions.length; j++) {
            int xTile = positions[j].x;
            int yTile = positions[j].y;
            Object key = CachedTileImpl.hashKey(image, xTile, yTile);
            tileData[j] = cacheObject.get(key).getTile();
        }
    }
    return tileData;
}

/** This method removes the specified tile and notify it to the observers */
public void remove(RenderedImage image, int xTile, int yTile) {
    /* Tile key calculation */
    Object key = CachedTileImpl.hashKey(image, xTile, yTile);
    if (diagnosticEnabled) {
        synchronized (this) {
            /*
             * In diagnostic mode this method check if the old tile was present
             * and if so update its status and notify it to the observers, and
             * removes it
             */
            CachedTileImpl oldValue = cacheObject.get(key);
            if (oldValue != null) {
                oldValue.updateTileTimeStamp();
                oldValue.setAction(Actions.ABOUT_TO_REMOVAL);
                setChanged();
                notifyObservers(oldValue);
                cacheObject.remove(key);
                oldValue.updateTileTimeStamp();
                oldValue.setAction(Actions.MANUAL_REMOVAL);
                setChanged();
                notifyObservers(oldValue);
            }
        }
    } else {
        /* The tile is removed without checking if it is present or not */
        cacheObject.remove(key);
    }
}

/** This method removes all the tiles that belong to the specified image */
public void removeTiles(RenderedImage image) {
    /* Image tile coordinates */
    int minTx = image.getMinTileX();
    int minTy = image.getMinTileY();
    int maxTx = minTx + image.getNumXTiles();
    int maxTy = minTy + image.getNumYTiles();
    if (diagnosticEnabled) {
        synchronized (this) {
            /*
             * This method is the same for both the diagnostic or non-diagnostic
             * mode the difference is the sincronized block
             */
            removeAllImageTiles(image, minTx, maxTx, minTy, maxTy);
        }
    } else {
        removeAllImageTiles(image, minTx, maxTx, minTy, maxTy);
    }
}

/** This method cycles through the image eliminating all of its tiles */
private void removeAllImageTiles(RenderedImage image, int minX, int maxX,
        int minY, int maxY) {
    for (int y = minY; y < maxY; y++) {
        for (int x = minX; x < maxX; x++) {
            remove(image, x, y);
        }
    }
}

/** This method sets the memory capacity, then flush and rebuild the cache */
public synchronized void setMemoryCapacity(long memoryCacheCapacity) {
    if (memoryCacheCapacity < 0) {
        throw new IllegalArgumentException("Memory capacity too small");
    } else {
        this.memoryCacheCapacity = memoryCacheCapacity;
        flush();
    }

}

/** This method sets the memory threshold, then flush and rebuild the cache */
public synchronized void setMemoryThreshold(float memoryCacheThreshold) {
    if (memoryCacheThreshold < 0 || memoryCacheThreshold > 1) {
        throw new IllegalArgumentException(
                "Memory threshold must be between 0 and 1");
    } else {
        this.memoryCacheThreshold = memoryCacheThreshold;
        flush();
    }

}

/**
 * This method sets the cache ConcurrencyLevel and then flush and rebuild the
 * cache
 */
public synchronized void setConcurrencyLevel(int concurrency) {
    if (concurrency < 1) {
        throw new IllegalArgumentException(
                "ConcurrencyLevel must be at least 1");
    } else {
        concurrencyLevel = concurrency;
        flush();

    }

}

/** Diagnostic is disabled, then the cache is flushed and rebuilt */
public synchronized void disableDiagnostics() {
    this.diagnosticEnabled = false;
    flush();
}

/** Diagnostic is enabled, then the cache is flushed and rebuilt */
public synchronized void enableDiagnostics() {
    this.diagnosticEnabled = true;
    flush();
}

/** The counters are set to 0 when the cache is flushed */
public synchronized void resetCounts() {
    flush();
}

/** This method returns the number of cache hits */
public long getCacheHitCount() {
    return hitNumber;
}

/** This method returns the cache weighed size */
public long getCacheMemoryUsed() {
    return cacheObject.weightedSize();
}

/** This method returns the number of cache miss */
public long getCacheMissCount() {
    return missNumber;
}

/** This method returns the number of tile present in the cache */
public long getCacheTileCount() {
    return cacheObject.size();
}

/** This method returns the cache concurrency level */
public int getConcurrencyLevel() {
    return concurrencyLevel;
}

/** Not supported */
public void setTileCapacity(int arg0) {
    throw new UnsupportedOperationException("Deprecated Operation");

}

/** Not supported */
public int getTileCapacity() {
    throw new UnsupportedOperationException("Deprecated Operation");
}

/** Not supported */
public Comparator getTileComparator() {
    throw new UnsupportedOperationException("Comparator not supported");
}

/** Not supported */
public void setTileComparator(Comparator arg0) {
    throw new UnsupportedOperationException("Comparator not supported");
}

/** Not supported */
public void memoryControl() {
    throw new UnsupportedOperationException("Memory Control not supported");
}

}
