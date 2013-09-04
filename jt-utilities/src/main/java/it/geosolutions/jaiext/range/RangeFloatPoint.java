package it.geosolutions.jaiext.range;


/**
 * This class is a subclass of the {@link Range} class handling byte data into a single-point range.
 */
public class RangeFloatPoint extends Range {

    /** Single Range value */
    private final float value;
    private final boolean isNaN; 

    RangeFloatPoint(float value) {
        if(Float.isNaN(value)){
            isNaN = true;
        }else{
            isNaN = false;
        }
        this.value = value;
    }

    @Override
    public boolean contains(float value) {
        if(isNaN){
            int valueInt = Float.floatToIntBits(value);
            int oldValueInt=Float.floatToIntBits(this.value);
            return valueInt == oldValueInt;
        }else{
            return this.value == value;
        }
    }

    @Override
    public DataType getDataType() {
        return DataType.FLOAT;
    }
}
