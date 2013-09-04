package it.geosolutions.jaiext.range;

/**
 * This class is a subclass of the {@link Range} class handling short/unsigned short data into a single-point range.
 */
public class RangeShortPoint extends Range {

    /** Single Range value */
    private final short value;

    /** Boolean indicating if the data type is short or unsigned short */
    private final boolean isUshort;

    RangeShortPoint(short value, boolean isUshort) {
        this.value = value;
        this.isUshort = isUshort;
    }

    @Override
    public boolean contains(short value) {
        return this.value == value;
    }

    @Override
    public DataType getDataType() {
        if (isUshort) {
            return DataType.USHORT;
        } else {
            return DataType.SHORT;
        }
    }
}
