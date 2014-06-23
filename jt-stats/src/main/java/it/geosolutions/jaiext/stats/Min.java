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
 * This subclass of {@link Statistics} is used for calculating the minimum of an image.
 */
public class Min extends Statistics {

    /** Internal variable storing the Minimum of all samples */
    private double min;

    Min() {
        this.min = Double.POSITIVE_INFINITY;
        this.type = StatsType.MIN;
    }

    /** This method returns the current state of the Minimum value */
    private double getMin() {
        return min;
    }

    @Override
    public void addSample(double sample) {
            samples++;
            if (sample < min) {
                min = sample;
            }
    }

    @Override
    protected synchronized void accumulateStats(Statistics stats) {
        checkSameStats(stats);
        Min minStats = (Min) stats;
        double minNew = minStats.getMin();
        if (minNew < min) {
            min = minNew;
        }
        samples += stats.getNumSamples();
    }

    @Override
    public Object getResult() {
        return min;
    }

    @Override
    protected void clearStats() {
        this.min = Double.POSITIVE_INFINITY;
    }
    
    @Override
    public Long getNumSamples() {
        return Long.valueOf(samples);
    }

}
