package it.geosolutions.jaiext.range;

/**
 * This class is a subclass of the {@link Range} class handling Short data with minimum and maximum bounds.
 */
public class RangeShort extends Range {
    /** Minimum range bound */
    private final short minValue;

    /** Maximum range bound */
    private final short maxValue;

    /** Boolean indicating if the minimum bound is included */
    private final boolean minIncluded;

    /** Boolean indicating if the maximum bound is included */
    private final boolean maxIncluded;

    RangeShort(short minValue, boolean minIncluded, short maxValue, boolean maxIncluded) {
        if (minValue < maxValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
        } else {
            this.minValue = maxValue;
            this.maxValue = minValue;
        }
        this.minIncluded = minIncluded;
        this.maxIncluded = maxIncluded;
    }

    @Override
    public boolean contains(short value) {
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

    @Override
    public DataType getDataType() {
        return DataType.SHORT;
    }

}
