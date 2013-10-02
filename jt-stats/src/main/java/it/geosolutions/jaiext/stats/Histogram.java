package it.geosolutions.jaiext.stats;

import com.google.common.util.concurrent.AtomicDouble;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;

public class Histogram extends Statistics {

    private final int numBins;

    private final Range interval;

    private final double binInterval;

    private final double minBound;

    private final AtomicDouble[] bins;

    Histogram(int numBins, double minBound, double maxBound) {
        this.numBins = numBins;
        this.interval = RangeFactory.create(minBound, true, maxBound, false, false);
        this.binInterval = (maxBound - minBound) / numBins;
        this.minBound = minBound;
        this.bins = new AtomicDouble[numBins];
        for (int i = 0; i < numBins; i++) {
            bins[i] = new AtomicDouble(0);
        }
    }

    @Override
    protected void addSampleNoNaN(double sample, boolean isData) {
        if (isData && interval.contains(sample)) {
            int index = getIndex(sample);
            bins[index].addAndGet(sample);
        }
    }

    @Override
    protected void addSampleNaN(double sample, boolean isData, boolean isNaN) {
        if (isData && !isNaN && interval.contains(sample)) {
            int index = getIndex(sample);
            bins[index].addAndGet(sample);
        }
    }

    @Override
    protected void accumulateStats(Statistics stats) {
        throw new UnsupportedOperationException("Histogram statistics cannot be accumulated");
    }

    @Override
    public Object getResult() {
        return bins.clone();
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
