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
 * Palette that re-uses the ColorMap used to build the palette itsel to speedup the lookups. When there is no shift every color found in the map can
 * be also found in the color map.
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class MappedColorIndexer implements ColorIndexer {

    byte[][] colors;

    ColorMap colorMap;

    int shift;

    SimpleColorIndexer delegate;

    /**
     * Builds a new {@link MappedColorIndexer}
     * 
     * @param colors The target palette
     * @param colorMap The color map used to build the palette, mapping from shifted colors to the palette index
     * @param shift The bit shift applied while building the palette
     */
    public MappedColorIndexer(byte[][] colors, ColorMap colorMap, int shift) {
        this.colors = colors;
        this.shift = shift;
        this.colorMap = colorMap;
        this.delegate = new SimpleColorIndexer(colors);
    }

    public IndexColorModel toIndexColorModel() {
        return delegate.toIndexColorModel();
    }

    public int getClosestIndex(final int r, final int g, final int b, final int a) {
        int sr = r;
        int sg = g;
        int sb = b;
        int sa = a;
        if (shift > 0) {
            sr = r >> shift;
            sg = g >> shift;
            sb = b >> shift;
            sa = a >> shift;
        }
        if (a <= PackedHistogram.ALPHA_THRESHOLD) {
            sr = 255;
            sg = 255;
            sb = 255;
            sa = 0;
        }
        // Concurrent get and put operation
        synchronized (colorMap) {
            int idx = colorMap.get(sr, sg, sb, sa);
            if (idx < 0) {
                idx = delegate.getClosestIndex(r, g, b, a);
                colorMap.put(sr, sg, sb, sa, idx);
            }
            return idx;
        }
    }

}
