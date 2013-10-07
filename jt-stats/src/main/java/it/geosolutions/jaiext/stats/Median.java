package it.geosolutions.jaiext.stats;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.stats.Statistics.StatsType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Median extends Statistics {

    
    private List<Double> listData;

    private final Range interval;
    
    Median(double minBound, double maxBound){
        this.interval = RangeFactory.create(minBound, true, maxBound, false, false);
        this.listData = Collections.synchronizedList(new ArrayList<Double>());
        this.type = StatsType.MEDIAN;
    }
    
    @Override
    public void addSampleNoNaN(double sample, boolean isData) {
        if (isData && interval.contains(sample)) {
            listData.add(sample);
        }
    }

    @Override
    public void addSampleNaN(double sample, boolean isData, boolean isNaN) {
        if (isData && !isNaN && interval.contains(sample)) {
            listData.add(sample);
        }
    }

    @Override
    protected void accumulateStats(Statistics stats) {
        throw new UnsupportedOperationException("Median statistics cannot be accumulated");
    }

    @Override
    public Object getResult() {
        Collections.sort(listData);
        int listSize = listData.size();
        if(listSize == 0){
            return Double.NaN;
        }else if(listSize == 1){
            return listData.get(0);
        }else{
            int halfSize = listSize/2;
            double halfValue= listData.get(halfSize);
            if(listData.size()%2==1){
                return halfValue;
            }else{
                return (halfValue + listData.get(halfSize+1))/2;
            }
        }
    }

    @Override
    protected void clearStats() {
        listData = Collections.synchronizedList(new ArrayList<Double>());
    }
}
