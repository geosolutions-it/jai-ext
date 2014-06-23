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
 * This abstract class is used for containing some of the possible statistical operators used by the {@link StatisticsOpImage} class. Every
 * statistical operator is defined by its {@link StatsType} and if 2 operators are equal, they can be combined into one with the "accumulateStats()"
 * method. This method checks if the 2 operators belong to the same type and then sum the statistics. For updating the statistics 2 different methods
 * are used, one for data types without NaN values and the other for data types with them. Finally the result is returned as Object in the case that
 * multiple results are calculated.
 */
public abstract class Statistics {

    /**
     * This enum is used for organizing the various kinds of statistics and giving them an identifier used by the {@link StatsFactory} create methods.
     */
    public enum StatsType {
        MEAN(0),
        SUM(1),
        MAX(2),
        MIN(3),
        EXTREMA(4),
        VARIANCE(5),
        DEV_STD(6),
        HISTOGRAM(7),
        MODE(8),
        MEDIAN(9);

        private int id;

        StatsType(int id) {
            this.id = id;
        }

        public int getStatsId() {
            return id;
        }
    }

    /** Statistics property name */
    public final static String STATS_PROPERTY = "JAI-EXT.stats";
    
    /** Variable indicating the statistic used */
    protected StatsType type;
    
    /** Internal variable storing the number of all samples */
    protected long samples;

    /** This method returns the statistical type of the object instance */
    protected StatsType getStatsType() {
        return type;
    }

    /** This method checks if the provided Statistics object belong to the same subclass of the current object
     * 
     * @param stats Statistics object to compare
     */
    protected void checkSameStats(Statistics stats) {
        if (stats.getStatsType() != type) {
            throw new IllegalArgumentException("These statistics are not the same");
        }
    }

    /**
     * This method adds a Double value to the statistics and updates them
     * 
     * @param sample sample value used for updating statistics
     */
    public abstract void addSample(double sample);

    /** This method is used for accumulating the statistics from another Statistics object 
     * 
     * @param stats Statistics object to add to the current object
     */
    protected abstract void accumulateStats(Statistics stats);

    /** This method returns the statistic result */
    public abstract Object getResult();
    
    /** This method is used for clearing the results */
    protected abstract void clearStats();
    
    /** This method returns the number of samples calculated */
    public abstract Long getNumSamples();
}
