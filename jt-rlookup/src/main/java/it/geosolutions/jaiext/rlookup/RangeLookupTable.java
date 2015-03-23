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
import it.geosolutions.jaiext.range.RangeFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A lookup table for the RangeLookup operation. It holds a collection of source value ranges, each mapped to a destination value. Instances of this
 * class are immutable.
 * <p>
 * Use the associated Builder class to construct a new table:
 * 
 * <pre>
 * <code>
 * // type parameters indicate lookup (source) and result 
 * // (destination) types
 * RangeLookupTable.Builder&lt;Double, Integer&gt; builder = RangeLookupTable.builder();
 * 
 * // map all values &lt;= 0 to -1 and all values &gt; 0 to 1
 * builder.add(Range.create(Double.NEGATIVE_INFINITY, false, 0.0, true), -1)
 *        .add(Range.create(0.0, false, Double.POSITIVE_INFINITY, false), 1);
 * 
 * RangeLookupTable&lt;Double, Integer&gt; table = builder.build();
 * </code>
 * </pre>
 * 
 * @param <T> type of the lookup (source) value range
 * @param <U> type of the result (destination) value
 * 
 * @author Michael Bedward
 * @author Simone Giannecchini, GeoSolutions
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RangeLookupTable<T extends Number & Comparable<? super T>, U extends Number & Comparable<? super U>> {

    private final List<LookupItem<T, U>> items;

    /**
     * Private constructor called from the Builder's build method.
     */
    private RangeLookupTable(Builder builder) {
        this.items = new ArrayList<LookupItem<T, U>>(builder.items);

        // Sort the lookup items on the basis of their source ranges
        Collections.sort(this.items, new LookupItemComparator<T, U>());
    }

    /**
     * Finds the LookupItem containing the given source value.
     * 
     * @param srcValue source image value
     * 
     * @return the LookupItem containing the source value or null if no matching item exists
     */
    public LookupItem<T, U> getLookupItem(T srcValue) {
        if (items.isEmpty()) {
            return null;

        } else {
            /*
             * Binary search for source value in items sorted by source range
             */
            int lo = 0;
            int hi = items.size() - 1;
            while (hi >= lo) {
                // update mid position, avoiding int overflow
                int mid = lo + (hi - lo) / 2;

                LookupItem<T, U> item = items.get(mid);
                Range r = item.getRange();

                if (r.containsN(srcValue)) {
                    return item;

                } else if (!Double.isInfinite(r.getMin().doubleValue())
                        && Double.compare(srcValue.doubleValue(), r.getMin().doubleValue()) <= 0) {
                    hi = mid - 1;

                } else {
                    lo = mid + 1;
                }
            }

            return null; // no match
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (LookupItem item : items) {
            sb.append(item).append("; ");
        }
        return sb.toString();
    }

    /**
     * Package private method called by {@link RangeLookupRIF}.
     * 
     * @return an unmodifiable view of the lookup table items
     */
    List<LookupItem<T, U>> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Builder to create an immutable lookup table.
     * 
     * @param <T> lookup (source) value type
     * @param <U> result (destination) valuetype
     */
    public static class Builder<T extends Number & Comparable<? super T>, U extends Number & Comparable<? super U>> {
        private final List<LookupItem<T, U>> items;

        /**
         * Creates a new builder.
         */
        public Builder() {
            this.items = new ArrayList<LookupItem<T, U>>();
        }

        /**
         * Creates a new table that will hold the lookup items added to this builder.
         * 
         * @return a new table instance
         */
        public RangeLookupTable<T, U> build() {
            return new RangeLookupTable<T, U>(this);
        }

        /**
         * Adds a new lookup defined by a range of source values mapping to a result value.
         * 
         * A new lookup range that overlaps one or more previously set ranges will be truncated or split into non-overlapping intervals. For example,
         * if the lookup [5, 10] => 1 has previously been set, and a new lookup [0, 20] => 2 is added, then the following lookups will result:
         * 
         * <pre>
         *     [0, 5) => 2
         *     [5, 10] => 1
         *     (10, 20] => 2
         * </pre>
         * 
         * Where a new range is completely overlapped by existing ranges it will be ignored.
         * <p>
         * 
         * Note that it is possible to end up with unintended gaps in lookup coverage. If the first range in the above example had been the half-open
         * interval (5, 10] rather than the closed interval [5, 10] then the following would have resulted:
         * 
         * <pre>
         *     [0, 5) => 2
         *     (5, 10] => 1
         *     (10, 20] => 2
         * </pre>
         * 
         * In this case the value 5 would not be matched.
         * 
         * @param srcRange the source value range
         * @param resultValue the destination value
         */
        public Builder add(Range srcRange, U resultValue) {
            if (srcRange == null || resultValue == null) {
                throw new IllegalArgumentException("arguments must not be null");
            }

            // Check for overlap with existing ranges
            for (LookupItem item : items) {
                if (srcRange.intersects(item.getRange())) {
                    List<Range> diffs = RangeFactory.subtract(item.getRange(), srcRange);
                    for (Range diff : diffs) {
                        add(diff, resultValue);
                    }
                    return this;
                }
            }

            items.add(new LookupItem<T, U>(srcRange, resultValue));
            return this;
        }

    }

}
