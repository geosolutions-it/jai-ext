package it.geosolutions.jaiext.stats;

/**
 * This subclass of {@link Statistics} is used for calculating the maximum and minimum of an image.
 */
public class Extrema extends Statistics {

    /** Internal variable storing the Maximum of all samples */
    private double max;

    /** Internal variable storing the Minimum of all samples */
    private double min;

    Extrema() {
        this.max = Double.NEGATIVE_INFINITY;
        this.min = Double.POSITIVE_INFINITY;
        this.type = StatsType.EXTREMA;
    }

    /** This method returns the current state of the Maximum value */
    private double getMax() {
        return max;
    }

    /** This method returns the current state of the Minimum value */
    private double getMin() {
        return min;
    }

    @Override
    protected void addSampleNoNaN(double sample, boolean isData) {
        if (isData) {
            if (sample > max) {
                max = sample;
            }
            if (sample < min) {
                min = sample;
            }
        }
    }

    @Override
    protected void addSampleNaN(double sample, boolean isData, boolean isNaN) {
        if (isData && !isNaN) {
            if (sample > max) {
                max = sample;
            }
            if (sample < min) {
                min = sample;
            }
        }
    }

    @Override
    protected void accumulateStats(Statistics stats) {
        checkSameStats(stats);
        Extrema exStats = (Extrema) stats;
        double maxNew = exStats.getMax();
        double minNew = exStats.getMin();
        if (maxNew > max) {
            max = maxNew;
        }
        if (minNew < min) {
            min = minNew;
        }
    }

    @Override
    public Object getResult() {
        double[] extrema = { min, max };
        return extrema;
    }

    @Override
    protected void clearStats() {
        this.max = Double.NEGATIVE_INFINITY;
        this.min = Double.POSITIVE_INFINITY;
    }

}
