package it.geosolutions.jaiext.stats;

import com.google.common.util.concurrent.AtomicDouble;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;

public class HistogramMode extends Statistics {
    
    private final boolean histogramStat;

    private final int numBins;

    private final Range interval;

    private final double binInterval;

    private final double minBound;

    private final AtomicDouble[] bins;

    HistogramMode(int numBins, double minBound, double maxBound, boolean histogramStat) {
        this.histogramStat = histogramStat;
        this.numBins = numBins;
        this.interval = RangeFactory.create(minBound, true, maxBound, false, false);
        this.binInterval = (maxBound - minBound) / numBins;
        this.minBound = minBound;
        this.bins = new AtomicDouble[numBins];
        for (int i = 0; i < numBins; i++) {
            bins[i] = new AtomicDouble(0);
        }
        if(histogramStat){
            this.type = StatsType.HISTOGRAM;
        }else{
            this.type = StatsType.MODE;
        }
        
    }

    @Override
    public void addSampleNoNaN(double sample, boolean isData) {
        if (isData && interval.contains(sample)) {
            int index = getIndex(sample);
            bins[index].addAndGet(1);
        }
    }

    @Override
    public void addSampleNaN(double sample, boolean isData, boolean isNaN) {
        if (isData && !isNaN && interval.contains(sample)) {
            int index = getIndex(sample);
            bins[index].addAndGet(1);
        }
    }

    @Override
    protected void accumulateStats(Statistics stats) {
        throw new UnsupportedOperationException("Histogram statistics cannot be accumulated");
    }

    @Override
    public Object getResult() {
        if(histogramStat){
            double[] array = new double[numBins];
            for(int i = 0; i< numBins ; i++){
                array[i] = bins[i].doubleValue();
            }
            return array;
        }else{
            double max = 0;
            int indexMax = 0;
            for(int i = 0; i<numBins;i++){
                if(bins[i].doubleValue()>max){
                    max = bins[i].doubleValue();
                    indexMax = i;
                }
            }
            
            if(max == 0){
                return indexMax*1.0d;
            }else{
                return indexMax+minBound;
            }
        }        
    }

    @Override
    protected synchronized void clearStats() {
        for (int i = 0; i < numBins; i++) {
            bins[i] = new AtomicDouble(0);
        }
    }

    private int getIndex(double sample) {
        int index = (int) ((sample - minBound) / binInterval);
        return index;
    }
}
