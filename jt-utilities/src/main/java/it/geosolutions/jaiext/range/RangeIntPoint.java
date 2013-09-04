package it.geosolutions.jaiext.range;

/**
 * This class is a subclass of the {@link Range} class handling Integer data into a single-point range.
 */
public class RangeIntPoint extends Range {
    /** Single Range value */
    private final int value;

    RangeIntPoint(int value) {
        this.value = value;
    }

    @Override
    public boolean contains(int value) {
        return this.value == value;
    }

    @Override
    public DataType getDataType() {
        return DataType.INTEGER;
    }
}
