package it.geosolutions.jaiext.stats;

/**
 * This subclass of {@link Statistics} is used for calculating the variance or the standard deviation of an image. These 2 operations are almost the
 * same, the difference is only at the final step when the second is returned by calculating the square root of the first parameter.
 */
public class VarianceStd extends Statistics {

    /** Boolean indicating if the final result is the variance the data, otherwise the standard deviation is returned */
    private boolean variance;

    /** Internal variable storing the sum of all samples */
    private double sumValues;

    /** Internal variable storing the sum of all the squared samples */
    private double sumSqrtValues;

    /** Internal variable storing the number of all samples */
    private long samples;

    VarianceStd(boolean variance) {
        this.variance = variance;
        this.sumValues = 0;
        this.sumSqrtValues = 0;
        this.samples = 0;
        if (variance) {
            this.type = StatsType.VARIANCE;
        } else {
            this.type = StatsType.DEV_STD;
        }
    }

    /** This method returns the current state of the internal sum of the samples */
    private double getSumValues() {
        return sumValues;
    }

    /** This method returns the current state of the internal sum of the squared samples */
    private double getSumSqrtValues() {
        return sumSqrtValues;
    }

    /** This method returns the current state of the samples number */
    private long getNumberOfSamples() {
        return samples;
    }

    @Override
    protected void addSampleNoNaN(double sample, boolean isData) {
        if (isData) {
            sumValues += sample;
            sumSqrtValues += (sample * sample);
            samples++;
        }
    }

    @Override
    protected void addSampleNaN(double sample, boolean isData, boolean isNaN) {
        if (isData && !isNaN) {
            sumValues += sample;
            sumSqrtValues += (sample * sample);
            samples++;
        }
    }

    @Override
    protected synchronized void accumulateStats(Statistics stats) {
        checkSameStats(stats);
        VarianceStd vstd = (VarianceStd) stats;
        samples += vstd.getNumberOfSamples();
        sumValues += vstd.getSumValues();
        sumSqrtValues += vstd.getSumSqrtValues();
    }

    @Override
    public Object getResult() {
        double varianceCalculated = (sumSqrtValues - (sumValues * sumValues) / samples)
                / (samples - 1);
        if (variance) {
            return varianceCalculated;
        } else {
            return Math.sqrt(varianceCalculated);
        }
    }

}
