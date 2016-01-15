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
 * This subclass of {@link Statistics} is used for calculating the mean or the sum of an image. These 2 operations are almost the same, the difference
 * is only at the final step when the sum is divided by the total number of samples for returning the mean value.
 */
public class MeanSum extends Statistics {

    /** Boolean indicating if the final result is a sum of all the data, otherwise the mean value is returned */
    private boolean simpleSum;

    /** Internal variable storing the sum of all samples */
    private double sumValues;

    /** Internal variable storing the number of all samples */
    private long samples;

    MeanSum(boolean simpleSum) {
        this.simpleSum = simpleSum;
        this.sumValues = 0;
        this.samples = 0;
        if (simpleSum) {
            this.type = StatsType.SUM;
        } else {
            this.type = StatsType.MEAN;
        }
    }

    /** This method returns the current state of the internal sum of the samples */
    private double getSumValues() {
        return sumValues;
    }

    @Override
    public void addSample(double sample) {
            sumValues += sample;
            samples++;
    }

    @Override
    protected synchronized void accumulateStats(Statistics stats) {
        checkSameStats(stats);
        MeanSum msum = (MeanSum) stats;
        samples += msum.getNumSamples();
        sumValues += msum.getSumValues();
    }

    @Override
    public Object getResult() {
        if (simpleSum) {
            return sumValues;
        } else {
            if(samples==1 || sumValues == 0d){
                return sumValues; 
            }else{
                return sumValues / samples;
            }                      
        }
    }

    @Override
    public Long getNumSamples() {
        return Long.valueOf(samples);
    }
    
    @Override
    protected void clearStats() {
        this.sumValues = 0;
        this.samples = 0;
    }
}
