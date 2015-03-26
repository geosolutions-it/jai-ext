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

import it.geosolutions.jaiext.range.Range;

/**
 * Used by {@link RangeLookupTable} to associate a source value lookup range with a destination value.
 * 
 * @param <T> type of the source range
 * @param <U> type of the destination value
 * 
 * @author Michael Bedward
 * @author Simone Giannecchini, GeoSolutions
 */
public class LookupItem<T extends Number & Comparable<? super T>, U extends Number & Comparable<? super U>> {

    /** Lookup range */
    private final Range range;

    /** Return value */
    private final U value;

    /**
     * Creates a new instance.
     * 
     * @param range the source value lookup range
     * @param value the destination value associated with this range
     * @throws IllegalArgumentException if either arg is {@code null}
     */
    public LookupItem(Range range, U value) {
        if (range == null || value == null) {
            throw new IllegalArgumentException("Both range and value must be non-null");
        }
        this.range = range;
        this.value = value;
    }

    /**
     * Gets the source value lookup range.
     * 
     * @return the range
     */
    public Range getRange() {
        return range;
    }

    /**
     * Gets the destination value.
     * 
     * @return the value
     */
    public U getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        result = prime * result + ((range == null) ? 0 : range.hashCode());
        return result;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LookupItem other = (LookupItem) obj;
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        if (range == null) {
            if (other.range != null) {
                return false;
            }
        } else if (!range.equals(other.range)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return range.toString() + " => " + value;
    }

}
