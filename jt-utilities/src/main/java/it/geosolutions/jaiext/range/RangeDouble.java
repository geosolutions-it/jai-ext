/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
*    http://www.geo-solutions.it/
*    Copyright 2014 GeoSolutions


* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at

* http://www.apache.org/licenses/LICENSE-2.0

* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package it.geosolutions.jaiext.range;

import it.geosolutions.jaiext.utilities.ImageUtilities;


/**
 * This class is a subclass of the {@link Range} class handling double data.
 */
public class RangeDouble extends Range {

    public static RangeDouble FULL_RANGE = new RangeDouble(Double.NEGATIVE_INFINITY, true,
            Double.POSITIVE_INFINITY, true, true);

    /** Minimum range bound */
    private final double minValue;

    /** Maximum range bound */
    private final double maxValue;
    
    /** If the Range is degenerated and it is a NaN value, then this value is taken as a Long */
    private final long intValue;
    
    /** Boolean indicating if the maximum bound is included */
    private final boolean isPoint;

    /** Boolean indicating the presence of No Data, only used for degenerated Range(single-point) */
    private final boolean isNaN;
    
    /** Boolean indicating if No Data in should be considered always inside or outside the Range (only for non-degenerated Ranges) */
    private final boolean nanIncluded;

    RangeDouble(double minValue, boolean minIncluded, double maxValue, boolean maxIncluded,boolean nanIncluded) {        // If one of the 2 bound values is NaN an exception is thrown
        super(minIncluded, maxIncluded);
    	if (Double.isNaN(minValue) && !Double.isNaN(maxValue) || !Double.isNaN(minValue) && Double.isNaN(maxValue)) {
            throw new UnsupportedOperationException(
                    "NaN values can only be set inside a single-point Range");
        }else if (minValue < maxValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.isPoint = false;
            this.isNaN = false;
            this.intValue=0;
            this.nanIncluded=nanIncluded;
        } else if (minValue > maxValue) {
            this.minValue = maxValue;
            this.maxValue = minValue;
            this.isPoint = false;
            this.isNaN = false;
            this.intValue=0;
            this.nanIncluded=nanIncluded;
        } else {
            this.minValue = minValue;
            this.maxValue = minValue;
            this.isPoint = true;
            this.nanIncluded=false;
            if (Double.isNaN(minValue)) {
                this.isNaN = true;
                this.intValue=Double.doubleToLongBits(minValue); 
            } else {
                this.isNaN = false;
                this.intValue=0;
            }
            if (!minIncluded && !maxIncluded) {
                throw new IllegalArgumentException(
                        "Cannot create a single-point range without minimum and maximum "
                                + "bounds included");
            } else {
                setMinIncluded(true);
                setMaxIncluded(true);
            }
        }}

    @Override
    public boolean contains(double value) {
        if (isPoint) {
            if (isNaN) {
                long valueLong = Double.doubleToLongBits(value);
                return valueLong == intValue;
            } else {
                return this.minValue == value;
            }
        }  else if(nanIncluded){
            final boolean lower;
            final boolean upper;

            if (isMinIncluded()) {
                lower = value < minValue;
            } else {
                lower = value <= minValue;
            }

            if (isMaxIncluded()) {
                upper = value > maxValue;
            } else {
                upper = value >= maxValue;
            }

            return !lower && !upper;
        }else{
            final boolean notLower;
            final boolean notUpper;

            if (isMinIncluded()) {
                notLower = value >= minValue;
            } else {
                notLower = value > minValue;
            }

            if (isMaxIncluded()) {
                notUpper = value <= maxValue;
            } else {
                notUpper = value < maxValue;
            }

            return notLower && notUpper;
        }
    }

    @Override
    public DataType getDataType() {
        return DataType.DOUBLE;
    }
    
    @Override
    public boolean isPoint() {
        return isPoint;
    }
    
    @Override
    public Number getMax() {
        return maxValue;
    }

    @Override
    public Number getMin() {
        return minValue;
    }
    
    public Number getMax(boolean isMaxIncluded) {
        double value = maxValue;
        if (isMaxIncluded != isMaxIncluded()) {
            value = ImageUtilities.rool(getDataType().getClassValue(), value, isMaxIncluded ? -1 : +1);
        }
        return value;
    }
    
    public Number getMin(boolean isMinIncluded) {
        double value = minValue;
        if (isMinIncluded != isMinIncluded()) {
            value = ImageUtilities.rool(getDataType().getClassValue(), value, isMinIncluded ? -1 : +1);
        }
        return value;
    }
    
    public boolean isNanIncluded() {
        return nanIncluded;
    }
    
    public boolean isNaN(){
        return isNaN;
    }
    
    public Range union(Range other){
        if(this.contains(other)){
            return this;
        } else if(other.contains(this)){
            return other;
        }
        
        double min2 = other.getMin().doubleValue();
        double max2 = other.getMax().doubleValue();
        
        double finalMin = minValue;
        double finalMax = maxValue;
        
        boolean minIncluded = isMinIncluded();
        boolean maxIncluded = isMaxIncluded();
        
        if(min2 < minValue){
            finalMin = min2;
            minIncluded = other.isMinIncluded();
        } else if(min2 == minValue){
            minIncluded |= other.isMinIncluded();
        }
        if(max2 > maxValue){
            finalMax = max2;
            maxIncluded = other.isMaxIncluded();
        } else if(max2 == maxValue){
            maxIncluded |= other.isMaxIncluded();
        }
        
        boolean isNaNIncluded = this.isNaN() || other.isNaN() || this.isNanIncluded() || other.isNanIncluded();
        
        return new RangeDouble(finalMin, minIncluded, finalMax, maxIncluded, isNaNIncluded);
    }

    @Override
    public Range intersection(Range other) {
        if (other.getDataType() == getDataType()) {
            if (other.contains(this)) {
                return this;
            } else if (this.contains(other)) {
                return other;
            }
        }

        double minOther = other.getMin().doubleValue();
        double maxOther = other.getMax().doubleValue();

        double finalMin = minValue;
        double finalMax = maxValue;

        boolean minIncluded = isMinIncluded();
        boolean maxIncluded = isMaxIncluded();

        if (minOther > minValue) {
            finalMin = minOther;
            minIncluded = other.isMinIncluded();
        } else if (minOther == minValue) {
            minIncluded &= other.isMinIncluded();
        }
        if (maxOther < maxValue) {
            finalMax = maxOther;
            maxIncluded = other.isMaxIncluded();
        } else if (maxOther == maxValue) {
            maxIncluded &= other.isMaxIncluded();
        }

        if (finalMax < finalMin || (finalMax == finalMin && !minIncluded && !maxIncluded)) {
            return null;
        }

        boolean isNaNIncluded = this.isNaN() && other.isNaN() && this.isNanIncluded()
                && other.isNanIncluded();

        return new RangeDouble(finalMin, minIncluded, finalMax, maxIncluded, isNaNIncluded);
    }
}
