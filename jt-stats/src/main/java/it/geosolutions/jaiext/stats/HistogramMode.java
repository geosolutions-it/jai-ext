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

import com.google.common.util.concurrent.AtomicDouble;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;

/**
 * This subclass of {@link Statistics} is used for calculating the Histogram or the Mode of an image. These 2 operations are almost the same, the
 * difference is only at the final step when the histogram returns an array containing the number of pixels for every bin while the mode returns only
 * the most populated bean. This operation is achieved with the help of an AtomicDouble array, for avoiding thread-safety issues. There is no
 * statistic accumulation because multiple Histogram objects could bring to an {@link OutOfMemoryError}.
 */
public class HistogramMode extends Statistics {
    /** Boolean indicating if Histogram operation must be performed */
    private final boolean histogramStat;

    /** Array number of bins */
    private final int numBins;

    /** Range of all the bins array */
    private final Range interval;

    /** Size of one bin */
    private final double binInterval;

    /** Minimum bound of the array */
    private final double minBound;

    /** Array containing all the bins */
    private final AtomicDouble[] bins;

    HistogramMode(int numBins, double minBound, double maxBound, boolean histogramStat) {
        // Setting of the parameters
        this.histogramStat = histogramStat;
        this.numBins = numBins;
        // If the array bounds are infinite, the half of minimum and maximum values are taken
        if (minBound == Double.NEGATIVE_INFINITY) {
            minBound = -Double.MAX_VALUE / 2;
        }
        if (maxBound == Double.POSITIVE_INFINITY) {
            maxBound = Double.MAX_VALUE / 2;
        }
        this.interval = RangeFactory.create(minBound, true, maxBound, false, false);
        this.binInterval = (maxBound - minBound) / numBins;
        this.minBound = minBound;
        // Creation of the bin array
        this.bins = new AtomicDouble[numBins];
        for (int i = 0; i < numBins; i++) {
            bins[i] = new AtomicDouble(0);
        }
        // Definition of the statsType
        if (histogramStat) {
            this.type = StatsType.HISTOGRAM;
        } else {
            this.type = StatsType.MODE;
        }
    }

    @Override
    public void addSample(double sample) {
        samples++;
        if (interval.contains(sample)) {
            // Selection of the index
            int index = getIndex(sample);
            // Update of the bin count
            bins[index].addAndGet(1);
        }
    }

    @Override
    protected void accumulateStats(Statistics stats) {
        throw new UnsupportedOperationException("Histogram statistics cannot be accumulated");
    }

    @Override
    public Object getResult() {
        if (histogramStat) {
            // If the operation is Histogram, the result is returned as a double array
            double[] array = new double[numBins];
            for (int i = 0; i < numBins; i++) {
                array[i] = bins[i].doubleValue();
            }
            return array;
        } else {
            // If the operation is Mode, the most present value is returned
            double max = 0;
            int indexMax = 0;
            for (int i = 0; i < numBins; i++) {
                if (bins[i].doubleValue() > max) {
                    max = bins[i].doubleValue();
                    indexMax = i;
                }
            }

            if (max == 0) {
                return indexMax * 1.0d;
            } else {
                return indexMax + minBound;
            }
        }
    }

    @Override
    public Long getNumSamples() {
        return Long.valueOf(samples);
    }
    
    @Override
    protected synchronized void clearStats() {
        // All the bins are set to 0
        for (int i = 0; i < numBins; i++) {
            bins[i] = new AtomicDouble(0);
        }
    }

    /** Private method for calculating the bin-index associated to the sample */
    private int getIndex(double sample) {
        int index = (int) ((sample - minBound) / binInterval);
        return index;
    }
}
