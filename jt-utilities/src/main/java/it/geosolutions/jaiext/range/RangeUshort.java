package it.geosolutions.jaiext.range;

/**
 * This class is a subclass of the {@link Range} class handling unsigned short data.
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

    /** Boolean indicating if the maximum bound is included */
    private final boolean isPoint;

    RangeUshort(short minValue, boolean minIncluded, short maxValue, boolean maxIncluded) {
        int valueMin = minValue & 0xFFFF;
        int valueMax = maxValue & 0xFFFF;
        
        if (minValue < maxValue) {
            this.minValue = valueMin;
            this.maxValue = valueMax;
            this.isPoint = false;
            this.minIncluded = minIncluded;
            this.maxIncluded = maxIncluded;
        } else if (minValue > maxValue) {
            this.minValue = valueMax;
            this.maxValue = valueMin;
            this.isPoint = false;
            this.minIncluded = minIncluded;
            this.maxIncluded = maxIncluded;
        } else {
            this.minValue = valueMin;
            this.maxValue = valueMin;
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
    public boolean contains(short value) {
       
        final int valueUshort = value & 0xFFFF;
        
        if (isPoint) {
            return this.minValue == valueUshort;
        } else {
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
    }

    @Override
    public DataType getDataType() {
        return DataType.USHORT;
    }
    
    
    @Override
    public boolean isPoint() {
        return isPoint;
    }
}
