package it.geosolutions.jaiext.zonal;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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
    private final Map<Integer, Map<Integer, Statistics[]>> statsContainer;

    /** Array indicating which statistics must be calculated */
    private final StatsType[] stats;

    /** Array indicating the minimum bounds for each band */
    private double[] minBounds;

    /** Array indicating the maximum bounds for each band */
    private double[] maxBounds;

    /** Array indicating the number of bins for each band */
    private int[] numbins;

    ZoneGeometry(int[] bands, StatsType[] stats, boolean classification, double[] minBounds,
            double[] maxBounds, int[] numbins) {

        // Setting of the parameters
        this.classification = classification;
        this.stats = stats;
        this.minBounds = minBounds;
        this.maxBounds = maxBounds;
        this.numbins = numbins;
        // creation of the new map associated with this ZoneGeometry instance
        statsContainer = new TreeMap<Integer, Map<Integer, Statistics[]>>();
        // Cicle on all the selected bands for creating the band inner map elements
        for (int i : bands) {
            Map<Integer, Statistics[]> mapClass = new TreeMap<Integer, Statistics[]>();
            // If the classifier is not present, the statistics objects are created at the ZoneGeometry
            // instantiation
            if (!classification) {
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
                mapClass.put(0, statistics);
            }
            statsContainer.put(i, mapClass);
        }
    }

    public synchronized void  add(double sample, int band, int classId, boolean isNaN) {
        // Selection of the map associated with the band indicated by the index
        Map<Integer, Statistics[]> mapClass = statsContainer.get(band);
        // Selection of the Statistics array associated with the zone indicated by the index
        // (always 0 if the classifier is not present)
        Statistics[] statistics = mapClass.get(classId);
        // if the classifier is present and a new Class is founded, then a new statistics object is created
        if (classification && statistics == null) {
            statistics = new Statistics[stats.length];
            for (int st = 0; st < stats.length; st++) {
                int statId = stats[st].getStatsId();
                if (statId <= 6) {
                    statistics[st] = StatsFactory.createSimpleStatisticsObjectFromInt(statId);
                } else {
                    statistics[st] = StatsFactory.createComplexStatisticsObjectFromInt(statId,
                            minBounds[band], maxBounds[band], numbins[band]);
                }
            }
        }

        // Update of the statistics
        for (int st = 0; st < stats.length; st++) {
            statistics[st].addSampleNaN(sample, true, isNaN);
        }

        // The updated statistics are inserted in the related containers
        mapClass.put(classId, statistics);

        statsContainer.put(band, mapClass);
    }

    /**
     * Utility method for having the Statistics of a specific band inside a specific zone class
     */
    public Statistics[] getStatsPerBandPerClass(int band, int classId) {
        Map<Integer, Statistics[]> resultAllClass = statsContainer.get(band);
        Statistics[] statistics = resultAllClass.get(classId);
        return statistics;
    }

    /**
     * Utility method for having the Statistics of a specific band if no classifier is used
     */
    public Statistics[] getStatsPerBandNoClassifier(int band) {
        Map<Integer, Statistics[]> resultAllClass = statsContainer.get(band);
        Statistics[] statistics = resultAllClass.get(0);
        return statistics;
    }

    /**
     * Utility method indicating the number of classes
     */
    public int getNumClass() {
        Map<Integer, Statistics[]> resultAllClass = statsContainer.get(0);
        return resultAllClass.size();
    }
    
    /**
     * Utility method indicating the index of all the classes
     */
    public Set<Integer> getClasses() {
        Map<Integer, Statistics[]> resultAllClass = statsContainer.get(0);
        Set<Integer> classes = resultAllClass.keySet();
        
        TreeSet<Integer> orderedSet = new TreeSet<Integer>(classes);
        
        return java.util.Collections.unmodifiableSet(orderedSet); 
    }

    /**
     * Utility method for having all the zone-class statistics for a selected band.
     */
    public Map<Integer, Statistics[]> getStatsPerBand(int band) {
        Map<Integer, Statistics[]> resultAllClass = statsContainer.get(band);
        return resultAllClass;
    }

    /**
     * Utility method for having all ZoneGeometry statistics.
     */
    public Map<Integer, Map<Integer, Statistics[]>> getTotalStats() {
        return new TreeMap<Integer, Map<Integer, Statistics[]>>(statsContainer);
    }

    /** Simple method for clearing all the image statistics */
    public void clear() {
        statsContainer.clear();
    }
}
