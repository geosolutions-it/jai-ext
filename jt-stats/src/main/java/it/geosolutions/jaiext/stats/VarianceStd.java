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
 * This subclass of {@link Statistics} is used for calculating the variance or the standard deviation of an image. These 2 operations are almost the
 * same, the difference is only at the final step when the second is returned by calculating the square root of the first parameter.
 */
public class VarianceStd extends Statistics {

    /** Boolean indicating if the final result is the variance the data, otherwise the standard deviation is returned */
    private boolean variance;

    /** Internal variable storing the sum of all samples */
    private double sumValues;

    /** Internal variable storing the sum of all the squared samples */
    private double sumSqrtValues;

    VarianceStd(boolean variance) {
        this.variance = variance;
        this.sumValues = 0;
        this.sumSqrtValues = 0;
        this.samples = 0;
        if (variance) {
            this.type = StatsType.VARIANCE;
        } else {
            this.type = StatsType.DEV_STD;
        }
    }

    /** This method returns the current state of the internal sum of the samples */
    private double getSumValues() {
        return sumValues;
    }

    /** This method returns the current state of the internal sum of the squared samples */
    private double getSumSqrtValues() {
        return sumSqrtValues;
    }

    @Override
    public void addSample(double sample) {
            sumValues += sample;
            sumSqrtValues += (sample * sample);
            samples++;
    }

    @Override
    protected synchronized void accumulateStats(Statistics stats) {
        checkSameStats(stats);
        VarianceStd vstd = (VarianceStd) stats;
        samples += vstd.getNumSamples();
        sumValues += vstd.getSumValues();
        sumSqrtValues += vstd.getSumSqrtValues();
    }

    @Override
    public Object getResult() {
        double varianceCalculated = (sumSqrtValues - (sumValues * sumValues) / samples)
                / (samples - 1);
        if (variance) {
            return varianceCalculated;
        } else {
            return Math.sqrt(varianceCalculated);
        }
    }

    @Override
    protected void clearStats() {
        this.sumValues = 0;
        this.sumSqrtValues = 0;
        this.samples = 0;
    }

    @Override
    public Long getNumSamples() {
        return Long.valueOf(samples);
    }
}
