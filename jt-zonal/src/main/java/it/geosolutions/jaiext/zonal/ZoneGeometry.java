package it.geosolutions.jaiext.zonal;

import java.util.Map;
import java.util.TreeMap;
import it.geosolutions.jaiext.stats.Statistics;
import it.geosolutions.jaiext.stats.Statistics.StatsType;
import it.geosolutions.jaiext.stats.StatsFactory;

public class ZoneGeometry {

    private final boolean classification;

    private final Map<Integer, Map<Integer, Statistics[]>> classifyStats;

    private final StatsType[] stats;

    private double[] minBounds;

    private double[] maxBounds;

    private int[] numbins;
    
    ZoneGeometry(int[] bands, StatsType[] stats, boolean classification, double[] minBounds,
            double[] maxBounds, int[] numbins) {
        
        this.classification = classification;
        this.stats = stats;
        this.minBounds = minBounds;
        this.maxBounds = maxBounds;
        this.numbins = numbins;

        classifyStats = new TreeMap<Integer, Map<Integer, Statistics[]>>();

        for (int i : bands) {
            Map<Integer, Statistics[]> mapZone = new TreeMap<Integer, Statistics[]>();
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
                mapZone.put(0, statistics);
            }
            classifyStats.put(i, mapZone);
        }
    }

    public void add(double sample, int band, int zone, boolean isNaN) {

        Map<Integer, Statistics[]> mapZone = classifyStats.get(band);

        Statistics[] statistics = mapZone.get(zone);

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

        for (int st = 0; st < stats.length; st++) {
            statistics[st].addSampleNaN(sample, true, isNaN);
        }

        mapZone.put(zone, statistics);

        classifyStats.put(band, mapZone);
    }

    public Statistics[] getStatsPerBandPerZone(int band, int zone) {
        Map<Integer, Statistics[]> resultAllZone = classifyStats.get(band);
        Statistics[] statistics = resultAllZone.get(zone);
        return statistics;
    }

    public int getNumZones() {
        Map<Integer, Statistics[]> resultAllZone = classifyStats.get(0);
        return resultAllZone.size();
    }

    public Object getStatsPerBand(int band) {
        Map<Integer, Statistics[]> resultAllZone = classifyStats.get(band);
        return resultAllZone;
    }

    public void clear() {
        classifyStats.clear();
    }

}
