package it.geosolutions.jaiext.range;

/**
 * This class is a subclass of the {@link Range} class handling double data into a single-point range.
 */
public class RangeDoublePoint extends Range {

    /** Single Range value */
    private final double value;
    private final boolean isNaN;

    RangeDoublePoint(double value) {
        if(Double.isNaN(value)){
            isNaN = true;
        }else{
            isNaN = false;
        }
        this.value = value;
    }

    @Override
    public boolean contains(double value) {
        if(isNaN){
            long valueInt = Double.doubleToLongBits(value);
            long oldValueInt=Double.doubleToLongBits(this.value);
            return valueInt == oldValueInt;
        }else{
            return this.value == value;
        }
    }

    @Override
    public DataType getDataType() {
        return DataType.DOUBLE;
    }
}
