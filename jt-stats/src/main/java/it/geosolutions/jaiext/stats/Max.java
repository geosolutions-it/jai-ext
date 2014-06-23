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
 * This subclass of {@link Statistics} is used for calculating the maximum of an image.
 */
public class Max extends Statistics {

    /** Internal variable storing the Maximum of all samples */
    private double max;

    Max() {
        this.max = Double.NEGATIVE_INFINITY;
        this.type = StatsType.MAX;
    }

    /** This method returns the current state of the Maximum value */
    private double getMax() {
        return max;
    }

    @Override
    public void addSample(double sample) {
        samples++;
        if (sample > max) {
            max = sample;
        }

    }

    @Override
    protected synchronized void accumulateStats(Statistics stats) {
        checkSameStats(stats);
        Max maxStats = (Max) stats;
        double maxNew = maxStats.getMax();
        if (maxNew > max) {
            max = maxNew;
        }
        samples += stats.getNumSamples();

    }

    @Override
    public Object getResult() {
        return max;
    }

    @Override
    public Long getNumSamples() {
        return Long.valueOf(samples);
    }

    @Override
    protected void clearStats() {
        this.max = Double.NEGATIVE_INFINITY;
    }

}
