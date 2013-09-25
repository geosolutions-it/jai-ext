package it.geosolutions.jaiext.stats;

/**
 * This abstract class is used for containing some of the possible statistical operators used by the {@link SimpleStatisticsOpImage} class. Every
 * statistical operator is defined by its {@link StatsType} and if 2 operators are equal, they can be combined into one with the "accumulateStats()"
 * method. This method checks if the 2 operators belong to the same type and then sum the statistics. For updating the statistics 2 different methods
 * are used, one for data types without NaN values and the other fir data types with them. Finally the result is returned as Object in the case that
 * multiple results are calculated.
 */
public abstract class Statistics {

    /**
     * This enum is used for organizing the various kinds of statistics and giving them an identifier used by the {@link StatsFactory} create methods.
     */
    public enum StatsType {
        MEAN(0), SUM(1), MAX(2), MIN(3), EXTREMA(4), VARIANCE(5), DEV_STD(6);

        private int id;

        StatsType(int id) {
            this.id = id;
        }

        int getStatsId() {
            return id;
        }
    }

    /** Variable indicating the statistic used */
    protected StatsType type;

    /** This method returns the statistical type of the object instance */
    protected StatsType getStatsType() {
        return type;
    }

    /** This method checks if the 2 Statistics objects belong to the same subclass */
    protected void checkSameStats(Statistics stats) {
        if (stats.getStatsType() != type) {
            throw new IllegalArgumentException("These statistics are not the same");
        }
    }

    /** This method add a Double value to the statistics and give informations about no Data */
    protected abstract void addSampleNoNaN(double sample, boolean isData);

    /** This method add a Double value to the statistics and give informations about no Data and NaN */
    protected abstract void addSampleNaN(double sample, boolean isData, boolean isNaN);

    /** This method is used for accumulating the statistics from another Statistics object */
    protected abstract void accumulateStats(Statistics stats);

    /** This method returns the statistic result */
    public abstract Object getResult();
}
