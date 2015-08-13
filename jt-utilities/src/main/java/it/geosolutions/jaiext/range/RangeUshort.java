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
 * This class is a subclass of the {@link Range} class handling unsigned short data.
 */
public class RangeUshort extends Range {

    public static RangeUshort FULL_RANGE = new RangeUshort(0, true, 65535, true);

    /** Minimum range bound */
    private final int minValue;

    /** Maximum range bound */
    private final int maxValue;

    /** Boolean indicating if the maximum bound is included */
    private final boolean isPoint;

    RangeUshort(int minValue, boolean minIncluded, int maxValue, boolean maxIncluded) {
        super(minIncluded, maxIncluded);
    	int valueMin = minValue & 0xFFFF;
        int valueMax = maxValue & 0xFFFF;
        
        if (minValue < maxValue) {
            this.minValue = valueMin;
            this.maxValue = valueMax;
            this.isPoint = false;
        } else if (minValue > maxValue) {
            this.minValue = valueMax;
            this.maxValue = valueMin;
            this.isPoint = false;
        } else {
            this.minValue = valueMin;
            this.maxValue = valueMin;
            this.isPoint = true;
            if (!minIncluded && !maxIncluded) {
                throw new IllegalArgumentException(
                        "Cannot create a single-point range without minimum and maximum "
                                + "bounds included");
            } else {
            	setMinIncluded(true);
            	setMaxIncluded(true);
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

            if (isMinIncluded()) {
                lower = valueUshort < minValue;
            } else {
                lower = valueUshort <= minValue;
            }

            if (isMaxIncluded()) {
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
    
    @Override
    public Number getMax() {
        return maxValue;
    }

    @Override
    public Number getMin() {
        return minValue;
    }
    
    public Number getMax(boolean isMaxIncluded) {
        int value = maxValue;
        if (isMaxIncluded != isMaxIncluded()) {
            value = (short) ImageUtilities.rool(getDataType().getClassValue(), value, isMaxIncluded ? -1 : +1);
        }
        return value;
    }
    
    public Number getMin(boolean isMinIncluded) {
        int value = minValue;
        if (isMinIncluded != isMinIncluded()) {
            value = (short) ImageUtilities.rool(getDataType().getClassValue(), value, isMinIncluded ? -1 : +1);
        }
        return value;
    }
    
    public Range union(Range other){
        if(this.contains(other)){
            return this;
        } else if(other.contains(this)){
            return other;
        }
        
        int min2 = other.getMin().intValue();
        int max2 = other.getMax().intValue();
        
        int finalMin = minValue;
        int finalMax = maxValue;
        
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
        
        return new RangeUshort((short)finalMin, minIncluded, (short)finalMax, maxIncluded);
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

        int minOther = other.getMin().intValue();
        int maxOther = other.getMax().intValue();

        int finalMin = minValue;
        int finalMax = maxValue;

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

        return new RangeUshort((short) finalMin, minIncluded, (short) finalMax, maxIncluded);
    }
}
