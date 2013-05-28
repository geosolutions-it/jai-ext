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

/* Simple constructor*/
ConcurrentLinkedCache() {
    this(DEFAULT_MEMORY_CACHE, DEFAULT_CONCURRENCY_LEVEL,
            DEFAULT_MEMORY_THRESHOLD, DEFAULT_DIAGNOSTIC);
}

/* Parameterized constructor*/
ConcurrentLinkedCache(long memoryCacheCapacity, int concurrencyLevel,
        float memoryCacheThreshold, boolean diagnosticEnabled) {
    this.concurrencyLevel = concurrencyLevel;
    this.memoryCacheCapacity = memoryCacheCapacity;
    this.diagnosticEnabled = diagnosticEnabled;
    this.memoryCacheThreshold = memoryCacheThreshold;
    /* the cache instantiation is done in the buildLinkedCache() method*/
    cacheObject = buildLinkedCache();
    /* the counters are set to 0*/
    hitNumber = 0;
    missNumber = 0;
}


private ConcurrentLinkedHashMap<Object, CachedTileImpl> buildLinkedCache() {
    ConcurrentLinkedHashMap.Builder<Object, CachedTileImpl> builder = new ConcurrentLinkedHashMap.Builder<Object, CachedTileImpl>();
    builder.concurrencyLevel(concurrencyLevel)
            .maximumWeightedCapacity(
                    (long) (memoryCacheCapacity * memoryCacheThreshold))
            .weigher(new Weigher<CachedTileImpl>() {
                public int weightOf(CachedTileImpl tile) {
                    return (int) ((CachedTileImpl) tile).getTileSize();
                }
            });
    if (diagnosticEnabled) {
        builder.listener(listener);
    }
    return builder.build();
}


public void add(RenderedImage image, int xTile, int yTile, Raster dataTile) {
    this.add(image, xTile, yTile, dataTile, null);
}


public void add(RenderedImage image, int xTile, int yTile, Raster dataTile,
        Object tileMetric) {
    Object key = CachedTileImpl.hashKey(image, xTile, yTile);
    CachedTileImpl newValue = new CachedTileImpl(image, xTile, yTile, dataTile,
            tileMetric);
    if (diagnosticEnabled) {
        synchronized (this) {
            newValue.setAction(Actions.ADDITION);
            setChanged();
            notifyObservers(newValue);
            CachedTileImpl oldValue = cacheObject.put(key, newValue);
            if (oldValue != null) {
                hitNumber++;
                oldValue.updateTileTimeStamp();
                oldValue.setAction(Actions.SUBSTITUTION_FROM_ADD);
                setChanged();
                notifyObservers(oldValue);
                return;
            }
        }
    } else {
        cacheObject.put(key, newValue);
    }
}


public void addTiles(RenderedImage image, Point[] positions,
        Raster[] dataTiles, Object tileMetric) {
    for (int i = 0; i < positions.length; i++) {
        int xIndex = positions[i].x;
        int yIndex = positions[i].y;
        add(image, xIndex, yIndex, dataTiles[i], tileMetric);
    }
}


public void flush() {
    if (diagnosticEnabled) {
        synchronized (this) {
            CachedTileImpl oldValue;
            Iterator<Object> iter = cacheObject.keySet().iterator();
            while (iter.hasNext()) {
                Object key = iter.next();
                oldValue = cacheObject.remove(key);
                oldValue.setAction(Actions.REMOVAL_FROM_FLUSH);
                oldValue.updateTileTimeStamp();
                setChanged();
                notifyObservers(oldValue);
            }
            hitNumber = 0;
            missNumber = 0;
        }
    } else {
        cacheObject.clear();
    }
    cacheObject = buildLinkedCache();
}


public long getMemoryCapacity() {
    return memoryCacheCapacity;
}


public float getMemoryThreshold() {
    return memoryCacheThreshold;
}


public Raster getTile(RenderedImage image, int xTile, int yTile) {
    Object key = CachedTileImpl.hashKey(image, xTile, yTile);
    if (diagnosticEnabled) {
        synchronized (this) {
            CachedTileImpl oldValue = cacheObject.get(key);
            if (oldValue != null) {
                hitNumber++;
                oldValue.setAction(Actions.UPDATING_TILE_FROM_GETTILE);
                oldValue.updateTileTimeStamp();
                setChanged();
                notifyObservers(oldValue);
                return oldValue.getTile();
            } else {
                missNumber++;
                return null;
            }
        }
    } else {
        CachedTileImpl oldValue = cacheObject.get(key);
        if (oldValue.getTile() != null) {
            return oldValue.getTile();
        } else {
            return null;
        }
    }
}


public Raster[] getTiles(RenderedImage image) {
    ArrayList<Raster> data = new ArrayList<Raster>();
    int numTx = image.getNumXTiles();
    int numTy = image.getNumYTiles();
    int lengthArray = Math.max(numTy, numTx);
    Point[] coordinates = new Point[lengthArray];
    Raster[] tileWithNull = getTiles(image, coordinates);
    for (int z = 0; z < tileWithNull.length; z++) {
        if (tileWithNull[z] != null) {
            data.add(tileWithNull[z]);
        }
    }
    return (Raster[]) data.toArray();
}


public Raster[] getTiles(RenderedImage image, Point[] positions) {
    Raster[] tileData = new Raster[positions.length];
    if (diagnosticEnabled) {
        synchronized (this) {
            for (int j = 0; j < positions.length; j++) {
                int xTile = positions[j].x;
                int yTile = positions[j].y;
                tileData[j] = getTile(image, xTile, yTile);
            }
        }
    } else {
        for (int j = 0; j < positions.length; j++) {
            int xTile = positions[j].x;
            int yTile = positions[j].y;
            Object key = CachedTileImpl.hashKey(image, xTile, yTile);
            tileData[j] = cacheObject.get(key).getTile();
        }
    }
    return tileData;
}


public void remove(RenderedImage image, int xTile, int yTile) {
    Object key = CachedTileImpl.hashKey(image, xTile, yTile);
    if (diagnosticEnabled) {
        synchronized (this) {
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
        cacheObject.remove(key);
    }
}


public void removeTiles(RenderedImage image) {
    int minTx = image.getMinTileX();
    int minTy = image.getMinTileY();
    int maxTx = minTx + image.getNumXTiles();
    int maxTy = minTy + image.getNumYTiles();
    if (diagnosticEnabled) {
        synchronized (this) {
            removeAllImageTiles(image, minTx, maxTx, minTy, maxTy);
        }
    } else {
        removeAllImageTiles(image, minTx, maxTx, minTy, maxTy);
    }
}


private void removeAllImageTiles(RenderedImage image, int minX, int maxX,
        int minY, int maxY) {
    for (int y = minY; y < maxY; y++) {
        for (int x = minX; x < maxX; x++) {
            remove(image, x, y);
        }
    }
}


public synchronized void setMemoryCapacity(long memoryCacheCapacity) {
    if (memoryCacheCapacity < 0) {
        throw new IllegalArgumentException("Memory capacity too small");
    } else {
        this.memoryCacheCapacity = memoryCacheCapacity;
        flush();
    }

}


public synchronized void setMemoryThreshold(float memoryCacheThreshold) {
    if (memoryCacheThreshold < 0 || memoryCacheThreshold > 1) {
        throw new IllegalArgumentException(
                "Memory threshold must be between 0 and 1");
    } else {
        this.memoryCacheThreshold = memoryCacheThreshold;
        flush();
    }

}


public synchronized void disableDiagnostics() {
    this.diagnosticEnabled = false;
}


public synchronized void enableDiagnostics() {
    this.diagnosticEnabled = true;
}


public synchronized void resetCounts() {
    flush();
}


public long getCacheHitCount() {
    return hitNumber;
}


public long getCacheMemoryUsed() {
    return cacheObject.weightedSize();
}


public long getCacheMissCount() {
    return missNumber;
}


public long getCacheTileCount() {
    return cacheObject.size();
}

/* Not supported*/
public void setTileCapacity(int arg0) {
    throw new UnsupportedOperationException("Deprecated Operation");

}

/* Not supported*/
public int getTileCapacity() {
    throw new UnsupportedOperationException("Deprecated Operation");
}

/* Not supported*/
public Comparator getTileComparator() {
    throw new UnsupportedOperationException("Comparator not supported");
}

/* Not supported*/
public void setTileComparator(Comparator arg0) {
    throw new UnsupportedOperationException("Comparator not supported");

}

/* Not supported*/
public void memoryControl() {
    throw new UnsupportedOperationException("Memory Control not supported");
}

}
