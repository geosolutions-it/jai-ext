package it.geosolutions.jaiext.stats;

/**
 * This subclass of {@link Statistics} is used for calculating the maximum of an image.
 */
public class Max extends Statistics {

    /** Internal variable storing the Maximum of all samples */
    private double max;

    Max() {
        this.max = Double.NEGATIVE_INFINITY;
        this.type = StatsType.MAX;
    }

    /** This method returns the current state of the Maximum value */
    private double getMax() {
        return max;
    }

    @Override
    public void addSample(double sample) {
            if (sample > max) {
                max = sample;
            }
    }

    @Override
    protected synchronized void accumulateStats(Statistics stats) {
        checkSameStats(stats);
        Max maxStats = (Max) stats;
        double maxNew = maxStats.getMax();
        if (maxNew > max) {
            max = maxNew;
        }
    }

    @Override
    public Object getResult() {
        return max;
    }

    @Override
    protected void clearStats() {
        this.max = Double.NEGATIVE_INFINITY;
    }

}
