package it.geosolutions.jaiext.range;


/**
 * This class is a subclass of the {@link Range} class handling double data with minimum and maximum bounds.
 */
public class RangeDouble extends Range {

    /** Minimum range bound */
    private final double minValue;

    /** Maximum range bound */
    private final double maxValue;

    /** Boolean indicating if the minimum bound is included */
    private final boolean minIncluded;

    /** Boolean indicating if the maximum bound is included */
    private final boolean maxIncluded;

    RangeDouble(double minValue, boolean minIncluded, double maxValue, boolean maxIncluded) {
        // If one of the 2 bound values is NaN an exception is thrown
        if (Double.isNaN(minValue) && Double.isNaN(maxValue)) {
            throw new UnsupportedOperationException(
                    "NaN values can only be set inside a single-point Range");
        }
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
    public boolean contains(double value) {
        final boolean lower;
        final boolean upper;

        if (minIncluded) {
            lower = value >= minValue;
        } else {
            lower = value > minValue;
        }

        if (maxIncluded) {
            upper = value <= maxValue;
        } else {
            upper = value < maxValue;
        }

        return lower && upper;
    }

    @Override
    public DataType getDataType() {
        return DataType.DOUBLE;
    }
}
