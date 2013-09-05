package it.geosolutions.jaiext.range;

/**
 * This class is a subclass of the {@link Range} class handling byte data with minimum and maximum bounds. This
 * class considers the byte range from 0 to 255 because JAI byte images are all inside this range.
 */
public class RangeByte extends Range {

    /** Minimum range bound */
    private final int minValue;

    /** Maximum range bound */
    private final int maxValue;

    /** Boolean indicating if the minimum bound is included */
    private final boolean minIncluded;

    /** Boolean indicating if the maximum bound is included */
    private final boolean maxIncluded;

    RangeByte(byte minValue, boolean minIncluded, byte maxValue, boolean maxIncluded) {
        int valueMin = minValue & 0xFF;
        int valueMax = maxValue & 0xFF;
        
        if (minValue < maxValue) {
            this.minValue = valueMin;
            this.maxValue = valueMax;
        } else {
            this.minValue = valueMax;
            this.maxValue = valueMin;
        }
        this.minIncluded = minIncluded;
        this.maxIncluded = maxIncluded;
    }

    @Override
    public boolean contains(byte value) {

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
        return DataType.BYTE;
    }

}
