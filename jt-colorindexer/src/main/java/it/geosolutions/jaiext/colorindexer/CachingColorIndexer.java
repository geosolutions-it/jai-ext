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
 * Wraps around another palette and adds last match caching. This speeds up significantly lookups on maps that have large areas with constant color
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class CachingColorIndexer implements ColorIndexer {
    /** {@link ColorIndexer} wrapped by this instance*/
    ColorIndexer delegate;

    /** Last used element of its colorcomponent */
    int lr, lg, lb, la;

    /** Last used colormap index */
    int idx = -1;

    public CachingColorIndexer(ColorIndexer delegate) {
        this.delegate = delegate;
    }

    public IndexColorModel toIndexColorModel() {
        return delegate.toIndexColorModel();
    }

    public int getClosestIndex(int r, int g, int b, int a) {
        // Cecking if it is the same colours
        synchronized (this) {
            if (r == lr && g == lg && b == lb && a == la && idx >= 0) {
                return idx;
            }
        }
        // Otherwise get a new one
        int delegateIdx = delegate.getClosestIndex(r, g, b, a);

        synchronized (this) {
            lr = r;
            lg = g;
            lb = b;
            la = a;
            idx = delegateIdx;
        }

        return delegateIdx;
    }

}
