package it.geosolutions.jaiext.zonal;

import java.util.Map;
import java.util.TreeMap;
import it.geosolutions.jaiext.stats.Statistics;
import it.geosolutions.jaiext.stats.Statistics.StatsType;
import it.geosolutions.jaiext.stats.StatsFactory;

public class ZoneGeometry {

    private final boolean classification;

    private final Map<Integer, Map<Integer, Statistics[]>> classifyStats;

    private final int idZone;

    private final int[] bands;

    private final StatsType[] stats;

    ZoneGeometry(int idZone, int[] bands, StatsType[] stats, boolean classification) {
        this.classification = classification;
        this.idZone = idZone;
        this.bands = bands;
        this.stats = stats;

        classifyStats = new TreeMap<Integer, Map<Integer, Statistics[]>>();

        for (Integer i : bands) {
            Map<Integer, Statistics[]> mapZone = new TreeMap<Integer, Statistics[]>();
            if (!classification) {
                Statistics[] statistics = new Statistics[stats.length];
                for (int st = 0; st < stats.length; st++) {
                    statistics[st] = StatsFactory.createSimpleStatisticsObjectFromInt(stats[st]
                            .getStatsId());
                }
                mapZone.put(0, statistics);
            }
            classifyStats.put(i, mapZone);
        }
    }

    public void add(double sample, int band, boolean isNaN) {
        add(sample, band, 0, isNaN);
    }

    public synchronized void add(double sample, int band, int zone, boolean isNaN) {

        Map<Integer, Statistics[]> mapZone = classifyStats.get(band);
        
        Statistics[] statistics = mapZone.get(zone);
        
        if(statistics == null){
            statistics = new Statistics[stats.length];
            for (int st = 0; st < stats.length; st++) {
                statistics[st] = StatsFactory.createSimpleStatisticsObjectFromInt(stats[st]
                        .getStatsId());
            }
        }
        
        for (int st = 0; st < stats.length; st++) {
            statistics[st].addSampleNaN(sample, true, isNaN);
        }
        
        mapZone.put(zone, statistics);
    }
    
    
    

}
