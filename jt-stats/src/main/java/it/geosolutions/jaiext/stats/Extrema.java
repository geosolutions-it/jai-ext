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

/**
 * This subclass of {@link Statistics} is used for calculating the maximum and minimum of an image.
 */
public class Extrema extends Statistics {

    /** Internal variable storing the Maximum of all samples */
    private double max;

    /** Internal variable storing the Minimum of all samples */
    private double min;

    Extrema() {
        this.max = Double.NEGATIVE_INFINITY;
        this.min = Double.POSITIVE_INFINITY;
        this.type = StatsType.EXTREMA;
    }

    /** This method returns the current state of the Maximum value */
    private double getMax() {
        return max;
    }

    /** This method returns the current state of the Minimum value */
    private double getMin() {
        return min;
    }

    @Override
    public void addSample(double sample) {
        if (sample > max) {
            max = sample;
        }
        if (sample < min) {
            min = sample;
        }

        samples++;

    }

    @Override
    protected void accumulateStats(Statistics stats) {
        checkSameStats(stats);
        Extrema exStats = (Extrema) stats;
        double maxNew = exStats.getMax();
        double minNew = exStats.getMin();
        if (maxNew > max) {
            max = maxNew;
        }
        if (minNew < min) {
            min = minNew;
        }

        samples += stats.getNumSamples();
    }

    @Override
    public Object getResult() {
        double[] extrema = { min, max };
        return extrema;
    }

    @Override
    public Long getNumSamples() {
        return Long.valueOf(samples);
    }

    @Override
    protected void clearStats() {
        this.max = Double.NEGATIVE_INFINITY;
        this.min = Double.POSITIVE_INFINITY;
    }

}
