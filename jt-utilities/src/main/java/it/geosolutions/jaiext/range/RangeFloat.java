package it.geosolutions.jaiext.range;

/**
 * This class is a subclass of the {@link Range} class handling float data with minimum and maximum bounds.
 */
public class RangeFloat extends Range {
    /** Minimum range bound */
    private final float minValue;

    /** Maximum range bound */
    private final float maxValue;

    /** Boolean indicating if the minimum bound is included */
    private final boolean minIncluded;

    /** Boolean indicating if the maximum bound is included */
    private final boolean maxIncluded;

    RangeFloat(float minValue, boolean minIncluded, float maxValue, boolean maxIncluded) {
        // If one of the 2 bound values is NaN an exception is thrown
        if (Float.isNaN(minValue) && Float.isNaN(maxValue)) {
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
    public boolean contains(float value) {
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
        return DataType.FLOAT;
    }

}
