package it.geosolutions.jaiext.stats;

import it.geosolutions.jaiext.stats.Statistics.StatsType;

/**
 * This factory class is used for creating all the possible {@link Statistics} subclasses. The first 7 methods give the possibility to create the
 * chosen statistic object without giving any parameter. The last method could be used for creating the statistic object by selecting its Id, defined
 * inside the {@link StatsType}.
 */
public class StatsFactory {

    //Private empty constructor for avoiding instantiation of the class.
    private StatsFactory() {
    };

    /** This method returns a statistic object for calculating the Mean of an Image*/
    public static Statistics createMeanObject() {
        return new MeanSum(false);
    }

    /** This method returns a statistic object for calculating the Sum of all the pixels of an Image*/
    public static Statistics createSumObject() {
        return new MeanSum(true);
    }

    /** This method returns a statistic object for calculating the Maximum of all the pixels of an Image*/
    public static Statistics createMaxObject() {
        return new Max();
    }

    /** This method returns a statistic object for calculating the Minimum of all the pixels of an Image*/
    public static Statistics createMinObject() {
        return new Min();
    }

    /** This method returns a statistic object for calculating the Extrema of all the pixels of an Image*/
    public static Statistics createExtremaObject() {
        return new Extrema();
    }

    /** This method returns a statistic object for calculating the Variance of an Image*/
    public static Statistics createVarianceObject() {
        return new VarianceStd(true);
    }

    /** This method returns a statistic object for calculating the Standard Deviation of an Image*/
    public static Statistics createDevStdObject() {
        return new VarianceStd(false);
    }
    
    /** This method returns a statistic object for calculating the Histogram of an Image*/
    public static Statistics createHistogramObject(int numBins, double minBound, double maxBound) {
        return new HistogramMode(numBins,minBound,maxBound,true);
    }
    
    /** This method returns a statistic object for calculating the Mode of an Image*/
    public static Statistics createModeObject(int numBins, double minBound, double maxBound) {
        return new HistogramMode(numBins,minBound,maxBound,false);
    }
    
    /** This method returns a statistic object for calculating the Median of an Image*/
    public static Statistics createMedianObject(double minBound, double maxBound) {
        return new Median(minBound,maxBound);
    }

    /** This method returns the simple statistic object associated to the Id returned*/
    public static Statistics createSimpleStatisticsObjectFromInt(int value) {
        // Selection of the related StatsType
        StatsType type = StatsType.values()[value];
        // Creation of the statistical object
        switch (type) {
        case MEAN:
            return createMeanObject();
        case SUM:
            return createSumObject();
        case MAX:
            return createMaxObject();
        case MIN:
            return createMinObject();
        case EXTREMA:
            return createExtremaObject();
        case VARIANCE:
            return createVarianceObject();
        case DEV_STD:
            return createDevStdObject();
        default:
            throw new IllegalArgumentException("Wrong StatsType object selected");
        }
    }
    
    /** This method returns the complex statistic object associated to the Id returned*/
    public static Statistics createComplexStatisticsObjectFromInt(int value, double minBound, double maxBound, int numBins) {
        // Selection of the related StatsType
        StatsType type = StatsType.values()[value];
        // Creation of the statistical object
        switch (type) {
        case HISTOGRAM:
            return createHistogramObject(numBins, minBound, maxBound);
        case MODE:
            return createModeObject(numBins, minBound, maxBound);
        case MEDIAN:
            return createMedianObject(minBound, maxBound);
        default:
            throw new IllegalArgumentException("Wrong StatsType object selected");
        }
    }
}
