package it.geosolutions.jaiext.range;


/**
 * This class is a subclass of the {@link Range} class handling Long data.
 */
public class RangeLong extends Range {

    
    /** Minimum range bound */
    private final long minValue;

    /** Maximum range bound */
    private final long maxValue;

    /** Boolean indicating if the minimum bound is included */
    private final boolean minIncluded;

    /** Boolean indicating if the maximum bound is included */
    private final boolean maxIncluded;
    
    /** Boolean indicating if the maximum bound is included */
    private final boolean isPoint;

    RangeLong(long minValue, boolean minIncluded, long maxValue, boolean maxIncluded) {
        
        if (minValue < maxValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.isPoint = false;
            this.minIncluded = minIncluded;
            this.maxIncluded = maxIncluded;
        } else if (minValue > maxValue) {
            this.minValue = maxValue;
            this.maxValue = minValue;
            this.isPoint = false;
            this.minIncluded = minIncluded;
            this.maxIncluded = maxIncluded;
        } else {
            this.minValue = minValue;
            this.maxValue = minValue;
            this.isPoint = true;
            if (!minIncluded && !maxIncluded) {
                throw new IllegalArgumentException(
                        "Cannot create a single-point range without minimum and maximum "
                                + "bounds included");
            } else {
                this.minIncluded = true;
                this.maxIncluded = true;
            }
        }
    }
    
    @Override
    public boolean contains(long value) {
        
        if (isPoint) {
            return this.minValue == value;
        } else {
            final boolean lower;
            final boolean upper;

            if (minIncluded) {
                lower = value < minValue;
            } else {
                lower = value <= minValue;
            }

            if (maxIncluded) {
                upper = value > maxValue;
            } else {
                upper = value >= maxValue;
            }

            return !lower && !upper;
        }
    }
    
    @Override
    public DataType getDataType() {
        return DataType.LONG;
    }

    @Override
    public boolean isPoint() {
        return isPoint;
    }
    
    @Override
    public Number getMax() {
        return maxValue;
    }

    @Override
    public Number getMin() {
        return minValue;
    }

}
