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
 * A tool transforming a generic RGBA color into an index into a palette represented by a IndexedColorModel
 * 
 * @author Andrea Aime - GeoSolutions
 */
public interface ColorIndexer {

    /**
     * @return the {@link IndexColorModel} related to the {@link ColorIndexer} instance
     */
    public IndexColorModel toIndexColorModel();

    /**
     * @return the closest integer palette value related to the input rgba elements
     */
    public int getClosestIndex(int r, int g, int b, int a);
}
