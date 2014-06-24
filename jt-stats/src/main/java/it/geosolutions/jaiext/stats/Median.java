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
package it.geosolutions.jaiext.stats;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This subclass of {@link Statistics} is used for calculating the median of an image. This operation is performed by saving alist of all the pixel
 * values. When the result is requested, the list is sorted and then the middle value is taken. If the list size is even, then the mean of the 2
 * middle values.
 */
public class Median extends Statistics {
    /** List of the image pixels */
    private List<Double> listData;

    /** Range of the accepted values */
    private final Range interval;

    Median(double minBound, double maxBound) {
        // If the array bounds are infinite, the minimum and maximum values are taken
        if (minBound == Double.NEGATIVE_INFINITY) {
            minBound = -Double.MAX_VALUE;
        }
        if (maxBound == Double.POSITIVE_INFINITY) {
            maxBound = Double.MAX_VALUE;
        }
        // Setting of the parameters
        this.interval = RangeFactory.create(minBound, true, maxBound, false, false);
        this.listData = Collections.synchronizedList(new ArrayList<Double>());
        this.type = StatsType.MEDIAN;
    }

    @Override
    public void addSample(double sample) {
        samples++;
        if (interval.contains(sample)) {
            listData.add(sample);
        }
    }

    @Override
    protected void accumulateStats(Statistics stats) {
        throw new UnsupportedOperationException("Median statistics cannot be accumulated");
    }

    @Override
    public Object getResult() {
        // Sorting of the data
        Collections.sort(listData);
        // Calculation of the list size
        int listSize = listData.size();
        if (listSize == 0) {
            // If no value is saved, then the Double.NaN is returned
            return Double.NaN;
        } else if (listSize == 1) {
            // If the size is one, then the value is returned
            return listData.get(0);
        } else {
            // If the middle value is 1 it is returned
            // else an average of the 2 middle value is returned
            int halfSize = listSize / 2;
            double halfValue = listData.get(halfSize);
            if (listData.size() % 2 == 1) {
                return halfValue;
            } else {
                return (halfValue + listData.get(halfSize + 1)) / 2;
            }
        }
    }

    @Override
    public Long getNumSamples() {
        return Long.valueOf(samples);
    }
    
    @Override
    protected void clearStats() {
        // The list is cleared by creating a new empty list
        listData = Collections.synchronizedList(new ArrayList<Double>());
    }
}
