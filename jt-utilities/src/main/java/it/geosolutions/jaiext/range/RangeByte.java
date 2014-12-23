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
 * This class is a subclass of the {@link Range} class handling byte data.
 */
public class RangeByte extends Range {

    /** Minimum range bound */
    private final int minValue;

    /** Maximum range bound */
    private final int maxValue;

    /** Boolean indicating if the maximum bound is included */
    private final boolean isPoint;

    RangeByte(byte minValue, boolean minIncluded, byte maxValue, boolean maxIncluded) {
    	super(minIncluded, maxIncluded);
    	int minValueInt = minValue & 0xFF;
    	int maxValueInt = maxValue & 0xFF;
    	
        if (minValueInt < maxValueInt) {
            this.minValue = minValueInt;
            this.maxValue = maxValueInt;
            this.isPoint = false;
        } else if (minValueInt > maxValueInt) {
            this.minValue = maxValueInt;
            this.maxValue = minValueInt;
            this.isPoint = false;
        } else {
            this.minValue = minValueInt;
            this.maxValue = minValueInt;
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
    public boolean contains(byte value) { 
        int valueI = value & 0xFF;
        if (isPoint) {
            return this.minValue == valueI;
        } else {
            final boolean lower;
            final boolean upper;

            if (isMinIncluded()) {
                lower = valueI < minValue;
            } else {
                lower = valueI <= minValue;
            }

            if (isMaxIncluded()) {
                upper = valueI > maxValue;
            } else {
                upper = valueI >= maxValue;
            }

            return !lower && !upper;
        }
    }

    @Override
    public DataType getDataType() {
        return DataType.BYTE;
    }
    
    @Override
    public boolean isPoint() {
        return isPoint;
    }

    @Override
    public Number getMax() {
        return maxValue;
    }
    
    public Number getMax(boolean isMaxIncluded) {
        int value = maxValue;
        if (isMaxIncluded != isMaxIncluded()) {
            value = (int) ImageUtilities.rool(getDataType().getClassValue(), value, isMaxIncluded ? -1 : +1);
        }
        return value;
    }
    
    public Number getMin(boolean isMinIncluded) {
        int value = minValue;
        if (isMinIncluded != isMinIncluded()) {
            value = (int) ImageUtilities.rool(getDataType().getClassValue(), value, isMinIncluded ? -1 : +1);
        }
        return value;
    }

    @Override
    public Number getMin() {
        return minValue;
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
        
        return new RangeByte((byte)finalMin, minIncluded, (byte)finalMax, maxIncluded);
    }
}
