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
package it.geosolutions.jaiext.rlookup;

import java.util.Comparator;

/**
 * Compares LookupItems on the basis of their source value ranges.
 * 
 * @author Michael Bedward
 */
public class LookupItemComparator<T extends Number & Comparable<? super T>, U extends Number & Comparable<? super U>>
        implements Comparator<LookupItem<T, U>> {

    public int compare(LookupItem<T, U> item1, LookupItem<T, U> item2) {
        return item1.getRange().compare(item2.getRange());
    }
}
