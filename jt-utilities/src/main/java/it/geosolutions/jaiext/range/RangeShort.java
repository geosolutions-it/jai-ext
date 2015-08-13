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
 * This class is a subclass of the {@link Range} class handling Short data.
 */
public class RangeShort extends Range {

    public static RangeShort FULL_RANGE = new RangeShort(Short.MIN_VALUE, true, Short.MAX_VALUE,
            true);

    /** Minimum range bound */
    private final short minValue;

    /** Maximum range bound */
    private final short maxValue;
    
    /** Boolean indicating if the maximum bound is included */
    private final boolean isPoint;

    RangeShort(short minValue, boolean minIncluded, short maxValue, boolean maxIncluded) {
        super(minIncluded, maxIncluded);
        if (minValue < maxValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.isPoint = false;
        } else if (minValue > maxValue) {
            this.minValue = maxValue;
            this.maxValue = minValue;
            this.isPoint = false;
        } else {
            this.minValue = minValue;
            this.maxValue = minValue;
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
        if (isPoint) {
            return this.minValue == value;
        } else {
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
        }
    }

    @Override
    public DataType getDataType() {
        return DataType.SHORT;
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
        short value = maxValue;
        if (isMaxIncluded != isMaxIncluded()) {
            value = (short) ImageUtilities.rool(getDataType().getClassValue(), value, isMaxIncluded ? -1 : +1);
        }
        return value;
    }
    
    public Number getMin(boolean isMinIncluded) {
        short value = minValue;
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
        
        short min2 = other.getMin().shortValue();
        short max2 = other.getMax().shortValue();
        
        short finalMin = minValue;
        short finalMax = maxValue;
        
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
        
        return new RangeShort(finalMin, minIncluded, finalMax, maxIncluded);
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

        return new RangeShort((short) finalMin, minIncluded, (short) finalMax, maxIncluded);
    }
}
