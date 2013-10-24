package it.geosolutions.jaiext.stats;

/**
 * This subclass of {@link Statistics} is used for calculating the minimum of an image.
 */
public class Min extends Statistics {

    /** Internal variable storing the Minimum of all samples */
    private double min;

    Min() {
        this.min = Double.POSITIVE_INFINITY;
        this.type = StatsType.MIN;
    }

    /** This method returns the current state of the Minimum value */
    private double getMin() {
        return min;
    }

    @Override
    public void addSample(double sample) {
            if (sample < min) {
                min = sample;
            }
    }

    @Override
    protected synchronized void accumulateStats(Statistics stats) {
        checkSameStats(stats);
        Min minStats = (Min) stats;
        double minNew = minStats.getMin();
        if (minNew < min) {
            min = minNew;
        }
    }

    @Override
    public Object getResult() {
        return min;
    }

    @Override
    protected void clearStats() {
        this.min = Double.POSITIVE_INFINITY;
    }

}
