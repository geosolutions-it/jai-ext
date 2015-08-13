package it.geosolutions.jaiext.range;

import java.util.Arrays;

/**
 * Simple class containing the NoData value/s to pass as a property object
 * 
 * @author Nicola Lagomarsini GeoSolutions
 */
public class NoDataContainer {
    
    public static final String GC_NODATA = "GC_NODATA";
    
    private Range nodataR;
    
    private double[] array;
    
    private double singleValue;
    
    public NoDataContainer(NoDataContainer other) {
        this.nodataR = other.nodataR;
        this.singleValue = other.singleValue;
        this.array = other.array;
    }

    public NoDataContainer(Range nodataR) {
        this.nodataR = nodataR;
        this.singleValue = nodataR.getMin(true).doubleValue();
        this.array = new double[]{singleValue};
    }
    
    public NoDataContainer(double[] array) {
        this.singleValue = array[0];
        this.nodataR = RangeFactory.create(singleValue, singleValue);
        this.array = array;
    }
    
    public NoDataContainer(double singleValue) {
        this.nodataR = RangeFactory.create(singleValue, singleValue);
        this.singleValue = singleValue;
        this.array = new double[]{singleValue};
    }

    public double getAsSingleValue() {
        return singleValue;
    }

    public double[] getAsArray() {
        return array;
    }

    public Range getAsRange() {
        return nodataR;
    }

    @Override
    public String toString() {
        return "NoDataContainer [nodataR=" + nodataR + ", array=" + Arrays.toString(array)
                + ", singleValue=" + singleValue + "]";
    }
}
