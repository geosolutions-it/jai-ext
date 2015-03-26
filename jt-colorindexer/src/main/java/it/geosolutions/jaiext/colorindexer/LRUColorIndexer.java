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
package it.geosolutions.jaiext.colorindexer;

import java.awt.image.IndexColorModel;

/**
 * A color indexer used when all we have is the target palette. Uses a LRU map to cache only the most recently used colors (the original image can
 * often have too many to practically keep in memory under concurrent load)
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class LRUColorIndexer implements ColorIndexer {
    IndexColorModel icm;

    ColorIndexer delegate;

    ColorMap cm;

    LRUColors lru;

    int maxSize;

    public LRUColorIndexer(IndexColorModel icm, int maxSize) {
        this.icm = icm;
        this.delegate = new SimpleColorIndexer(icm);
        this.cm = new ColorMap(maxSize);
        this.lru = new LRUColors();
        this.maxSize = maxSize;
    }

    public IndexColorModel toIndexColorModel() {
        return icm;
    }

    public int getClosestIndex(int r, int g, int b, int a) {
        int idx = cm.get(r, g, b, a);
        if (idx == -1) {
            idx = delegate.getClosestIndex(r, g, b, a);
            cm.put(r, g, b, a, idx);
            if (cm.size() > maxSize) {
                ColorEntry ce = lru.removeLast();
                int red = ColorUtils.red(ce.color);
                int green = ColorUtils.green(ce.color);
                int blue = ColorUtils.blue(ce.color);
                int alpha = ColorUtils.alpha(ce.color);
                cm.remove(red, green, blue, alpha);
                ce.color = ColorUtils.color(r, g, b, a);
                lru.add(ce);
            } else {
                int color = ColorUtils.color(r, g, b, a);
                lru.add(new ColorEntry(color, null, null));
            }
        }
        return idx;
    }

    /**
     * ColorIndexer element used for storing an rgba index
     */
    static final class ColorEntry {
        int color;

        ColorEntry previous;

        ColorEntry next;

        public ColorEntry(int color, ColorEntry previous, ColorEntry next) {
            this.color = color;
            this.previous = previous;
            this.next = next;
        }
    }

    /**
     * Class used for storing the {@link ColorEntry} instances using an LRU ordering
     */
    static final class LRUColors {
        ColorEntry first;

        ColorEntry last;

        ColorEntry removeLast() {
            if (last == null) {
                return null;
            }
            // remove the last one
            ColorEntry result = last;
            last = result.previous;
            // if it was the only one, clean up the first too
            if (last == null) {
                first = null;
            }
            return result;
        }

        void touch(int color) {
            // easy case, no moving needed
            if (last == null || last == first) {
                return;
            }

            ColorEntry result = null;
            for (ColorEntry entry = first; entry != null; entry = entry.next) {
                if (entry.color == color) {
                    result = entry;
                    break;
                }
            }

            // are we moving the last one to first?
            if (result == last) {
                last = result.previous;
                result.previous.next = null;
                result.previous = null;
                result.next = first;
                first = result;
            } else if (result != first) {
                result.previous.next = result.next;
                result.previous = null;
                result.next = first;
                first = result;
            }
        }

        void add(ColorEntry ce) {
            if (first == null) {
                ce.next = null;
                ce.previous = null;
                first = last = ce;
            } else {
                ce.next = first;
                ce.next.previous = ce;
                ce.previous = null;
                first = ce;
            }
        }
    }
}
