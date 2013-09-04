package it.geosolutions.jaiext.range;


/**
 * This class is a subclass of the {@link Range} class handling byte data into a single-point range.
 */
public class RangeBytePoint extends Range {

    /** Single Range value */
    final private byte value;

    RangeBytePoint(byte value) {
        this.value = value;
    }

    @Override
    public boolean contains(byte value) {
        return this.value == value;
    }

    @Override
    public DataType getDataType() {
        return DataType.BYTE;
    }

}
