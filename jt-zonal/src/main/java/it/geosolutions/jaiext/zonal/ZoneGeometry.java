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
package it.geosolutions.jaiext.zonal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.media.jai.ROI;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.stats.Statistics;
import it.geosolutions.jaiext.stats.Statistics.StatsType;
import it.geosolutions.jaiext.stats.StatsFactory;

/**
 * This class is used for storing the statistics associated to a specific geometry. All the statistics are organized inside Map objects. The
 * "statsContainer" object contains a number of items, each one for every band. Every item object contains the statistics array for every Class, if the
 * classifier is present, or only for the Class 0 if not. The statistics object are created at the initialization time if the classifier is not
 * present, otherwise they are created when a new Class is founded.
 */

public class ZoneGeometry {

    /** Boolean indicating if the classifier is present */
    private final boolean classification;

    /** Map containing all the statistics for every band and for every Class */
    private final Map<Integer, Map<Integer, Map<Range, Statistics[]>>> statsContainer;

    /** Array indicating which statistics must be calculated */
    private final StatsType[] stats;

    /** Array indicating the minimum bounds for each band */
    private double[] minBounds;

    /** Array indicating the maximum bounds for each band */
    private double[] maxBounds;

    /** Array indicating the number of bins for each band */
    private int[] numbins;
    
    /** Geometry associated to the selected zone*/
    private final ROI roi;

    private List<Range> ranges;

    ZoneGeometry(ROI roi, List<Range> ranges, int[] bands, StatsType[] stats, boolean classification, double[] minBounds,
            double[] maxBounds, int[] numbins) {

        // Setting of the parameters
        this.classification = classification;
        this.stats = stats;
        this.minBounds = minBounds;
        this.maxBounds = maxBounds;
        this.numbins = numbins;
        this.roi = roi;
        this.ranges = ranges;
        
        // creation of the new map associated with this ZoneGeometry instance
        statsContainer = new TreeMap<Integer, Map<Integer, Map<Range, Statistics[]>>>();
        // Cicle on all the selected bands for creating the band inner map elements
        for (int i : bands) {
            Map<Integer, Map<Range, Statistics[]>> mapClass = new TreeMap<Integer, Map<Range, Statistics[]>>();
            // If the classifier is not present, the statistics objects are created at the ZoneGeometry
            // instantiation
            if (!classification) {
                Map<Range, Statistics[]> mapRange = new HashMap<Range, Statistics[]>();
                
                for(Range inputRange : ranges){
                    
                    
                    Statistics[] statistics = new Statistics[stats.length];
                    for (int st = 0; st < stats.length; st++) {
                        int statId = stats[st].getStatsId();
                        if (statId <= 6) {
                            statistics[st] = StatsFactory.createSimpleStatisticsObjectFromInt(statId);
                        } else {
                            statistics[st] = StatsFactory.createComplexStatisticsObjectFromInt(statId,
                                    minBounds[i], maxBounds[i], numbins[i]);
                        }
                    }                    
                    mapRange.put(inputRange, statistics);
                }                
                mapClass.put(0, mapRange);
            }
            statsContainer.put(i, mapClass);
        }
    }

    public synchronized void  add(double sample, int band, int classId, Range dataRange) {
        // Selection of the map associated with the band indicated by the index
        Map<Integer, Map<Range, Statistics[]>> mapClass = statsContainer.get(band);
        // Selection of the Map associated with the zone indicated by the index
        // (always 0 if the classifier is not present)
        Map<Range, Statistics[]> mapRange = mapClass.get(classId);

        // if the classifier is present and a new Class is founded, then a new statistics object is created
        if (classification && mapRange == null) {
            mapRange = new HashMap<Range, Statistics[]>();
            Statistics[] statistics = new Statistics[stats.length];

            for (int st = 0; st < stats.length; st++) {
                int statId = stats[st].getStatsId();
                if (statId <= 6) {
                    statistics[st] = StatsFactory.createSimpleStatisticsObjectFromInt(statId);
                } else {
                    statistics[st] = StatsFactory.createComplexStatisticsObjectFromInt(statId,
                            minBounds[band], maxBounds[band], numbins[band]);
                }
            }

            // The updated statistics are inserted in the related containers
            mapRange.put(dataRange, statistics);
            // Insertion of the MapRange if not present
            mapClass.put(classId, mapRange);
            
        }else{
            // Selection of the 
            Statistics[] statistics = mapRange.get(dataRange);
            
            // Update of the statistics
            for (int st = 0; st < stats.length; st++) {
                statistics[st].addSample(sample);
            }
            
        }
    }

    /**
     * Utility method for having the Statistics of a specific band inside a specific zone class and a specific Range
     */
    public Statistics[] getStatsPerBandPerClassPerRange(int band, int classId, Range range) {
        Statistics[] statistics = statsContainer.get(band).get(classId).get(range);
        return statistics;
    }

    /**
     * Utility method for having the Statistics of a specific band if no classifier is used 
     */
    public Statistics[] getStatsPerBandNoClassifier(int band, Range range) {
        Statistics[] statistics = statsContainer.get(band).get(0).get(range);                
        return statistics;
    }
   
    
    /**
     * Utility method for having the Statistics of a specific band if no classifier and no Range are used 
     */
    public Statistics[] getStatsPerBandNoClassifierNoRange(int band) {
        
       Range fullRange = statsContainer.get(band).get(0).keySet().iterator().next();
        
        Statistics[] statistics = statsContainer.get(band).get(0).get(fullRange);                
        return statistics;
    }
    
    /**
     * Utility method for having the Statistics of a specific band if classifier is used but no range is present 
     */
    public Statistics[] getStatsPerBandNoRange(int band, int classId) {
        Statistics[] statistics = statsContainer.get(band).get(classId).get(ranges.get(0));             
        return statistics;
    }
    
    /**
     * Utility method for having all the zone-class statistics for a selected band.
     */
    public Map<Integer, Map<Range, Statistics[]>> getStatsPerBand(int band) {
        Map<Integer, Map<Range, Statistics[]>> resultAllClass = statsContainer.get(band);
        return resultAllClass;
    }
    
    /**
     * Utility method for having all the zone-class statistics for a selected band.
     */
    public Map<Range, Statistics[]> getStatsPerBandPerClass(int band, int classId) {
        Map<Range, Statistics[]> resultPerClass = statsContainer.get(band).get(classId);
        return resultPerClass;
    }
    

    /**
     * Utility method indicating the number of classes
     */
    public int getNumClass() {
        Map<Integer, Map<Range, Statistics[]>> resultAllClass = statsContainer.get(0);
        return resultAllClass.size();
    }
    
    
    public List<Range> getRanges(){
        return ranges;
    }
    
    /**
     * Utility method indicating the index of all the classes
     */
    public Set<Integer> getClasses() {
        Map<Integer, Map<Range, Statistics[]>> resultAllClass = statsContainer.get(0);
        Set<Integer> classes = resultAllClass.keySet();
        
        TreeSet<Integer> orderedSet = new TreeSet<Integer>(classes);
        
        return java.util.Collections.unmodifiableSet(orderedSet); 
    }

    /**
     * Utility method for having all ZoneGeometry statistics.
     */
    public Map<Integer, Map<Integer, Map<Range, Statistics[]>>> getTotalStats() {
        return new TreeMap<Integer, Map<Integer, Map<Range, Statistics[]>>>(statsContainer);
    }
    
    /**
     * Utility method for finding the zone associated geometry.
     */
    public ROI getROI(){
        return roi;        
    }
    

    /** Simple method for clearing all the image statistics */
    public void clear() {
        statsContainer.clear();
    }
    
    
    static class StatsPerRange{
        private Statistics[] stats;
        
        private List<Range> rangeList;
                
        StatsPerRange() {
            rangeList = new ArrayList<Range>();
        }
        
        public Statistics[] getStats() {
            return stats;
        }

        public void setStats(Statistics[] stats) {
            this.stats = stats;
        }

        public List<Range> getRangeList() {
            return rangeList;
        }
        
        public void addRange(Range r){
            rangeList.add(r);
        }
        
        public void addRanges(List<Range> list){
            rangeList=list;
        }       
    }
}
