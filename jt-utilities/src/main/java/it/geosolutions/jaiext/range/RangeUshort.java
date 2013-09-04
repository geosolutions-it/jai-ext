package it.geosolutions.jaiext.range;

/**
 * This class is a subclass of the {@link Range} class handling unsigned short data with minimum and maximum bounds.
 */
public class RangeUshort extends Range {

    /** Minimum range bound */
    private final int minValue;

    /** Maximum range bound */
    private final int maxValue;

    /** Boolean indicating if the minimum bound is included */
    private final boolean minIncluded;

    /** Boolean indicating if the maximum bound is included */
    private final boolean maxIncluded;

    RangeUshort(short minValue, boolean minIncluded, short maxValue, boolean maxIncluded) {
        if (minValue < maxValue) {
            this.minValue = minValue & 0xFFFF;
            this.maxValue = maxValue & 0xFFFF;
        } else {
            this.minValue = maxValue & 0xFFFF;
            this.maxValue = minValue & 0xFFFF;
        }
        this.minIncluded = minIncluded;
        this.maxIncluded = maxIncluded;
    }

    @Override
    public boolean contains(short value) {

        final int valueUshort = value & 0xFFFF;

        final boolean lower;
        final boolean upper;

        if (minIncluded) {
            lower = valueUshort < minValue;
        } else {
            lower = valueUshort <= minValue;
        }

        if (maxIncluded) {
            upper = valueUshort > maxValue;
        } else {
            upper = valueUshort >= maxValue;
        }

        return !lower && !upper;
    }

    @Override
    public DataType getDataType() {
        return DataType.USHORT;
    }
}
