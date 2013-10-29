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
            return sumValues / (samples - 1);
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
