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

/**
 * This class is a factory class which creates a {@link Range} object for the specific data type. This Range can have 2 bounds or be a single-point
 * range. If the 2 bound values are equal and almost one of them is included, then a single-point range is created, else an exception is thrown. If
 * the minimum bound value is bigger than the maximum value, then the 2 numbers are inverted at the Range creation time.
 */
public class RangeFactory {

    // Private Constructor for avoiding a new factory instantiation
    private RangeFactory() {
    }

    // Byte data
    public static Range create(byte minValue, boolean minIncluded, byte maxValue,
            boolean maxIncluded) {
        return new RangeByte(minValue, minIncluded, maxValue, maxIncluded);
    }

    // Ushort data
    public static Range createU(short minValue, boolean minIncluded, short maxValue,
            boolean maxIncluded) {
        return new RangeUshort(minValue, minIncluded, maxValue, maxIncluded);
    }

    // Short data
    public static Range create(short minValue, boolean minIncluded, short maxValue,
            boolean maxIncluded) {
        return new RangeShort(minValue, minIncluded, maxValue, maxIncluded);
    }

    // Integer data
    public static Range create(int minValue, boolean minIncluded, int maxValue, boolean maxIncluded) {
        return new RangeInt(minValue, minIncluded, maxValue, maxIncluded);
    }

    // Float data
    public static Range create(float minValue, boolean minIncluded, float maxValue,
            boolean maxIncluded, boolean nanIncluded) {
        return new RangeFloat(minValue, minIncluded, maxValue, maxIncluded, nanIncluded);
    }

    // Double data
    public static Range create(double minValue, boolean minIncluded, double maxValue,
            boolean maxIncluded, boolean nanIncluded) {
        return new RangeDouble(minValue, minIncluded, maxValue, maxIncluded, nanIncluded);
    }

    // Byte data
    public static Range create(byte minValue, byte maxValue) {
        return new RangeByte(minValue, true, maxValue, true);
    }

    // Ushort data
    public static Range createU(short minValue, short maxValue) {
        return new RangeUshort(minValue, true, maxValue, true);
    }

    // Short data
    public static Range create(short minValue, short maxValue) {
        return new RangeShort(minValue, true, maxValue, true);
    }

    // Integer data
    public static Range create(int minValue, int maxValue) {
        return new RangeInt(minValue, true, maxValue, true);
    }

    // Float data
    public static Range create(float minValue, float maxValue) {
        return new RangeFloat(minValue, true, maxValue, true, false);
    }

    // Double data
    public static Range create(double minValue, double maxValue) {
        return new RangeDouble(minValue, true, maxValue, true, false);
    }

    // Float data
    public static Range create(float minValue, boolean minIncluded, float maxValue,
            boolean maxIncluded) {
        return new RangeFloat(minValue, minIncluded, maxValue, maxIncluded, false);
    }

    // Double data
    public static Range create(double minValue, boolean minIncluded, double maxValue,
            boolean maxIncluded) {
        return new RangeDouble(minValue, minIncluded, maxValue, maxIncluded, false);
    }

    // Long data
    public static Range create(long minValue, boolean minIncluded, long maxValue,
            boolean maxIncluded) {
        return new RangeLong(minValue, minIncluded, maxValue, maxIncluded);
    }

    public static Range convertToDoubleRange(Range input) {
        // If already double do nothing
        if (input instanceof RangeDouble) {
            return input;
        }
        // Otherwise get minimum and maximum values and convert it
        double min = input.getMin().doubleValue();
        double max = input.getMax().doubleValue();

        boolean minIncluded = input.isMinIncluded();
        boolean maxIncluded = input.isMaxIncluded();

        boolean nanIncluded = input.isNanIncluded();

        // New Double range
        return new RangeDouble(min, minIncluded, max, maxIncluded, nanIncluded);
    }
}
