/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2014 - 2015 GeoSolutions


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

import java.awt.image.DataBuffer;
import java.util.ArrayList;
import java.util.List;

import it.geosolutions.jaiext.range.Range.DataType;

/**
 * This class is a factory class which creates a {@link Range} object for the specific data type.
 * This Range can have 2 bounds or be a single-point range. If the 2 bound values are equal and
 * almost one of them is included, then a single-point range is created, else an exception is
 * thrown. If the minimum bound value is bigger than the maximum value, then the 2 numbers are
 * inverted at the Range creation time.
 */
public class RangeFactory {

    private static final double TOLERANCE = 1E-6;

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
    public static Range create(int minValue, boolean minIncluded, int maxValue,
            boolean maxIncluded) {
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

    public static Range convertToFloatRange(Range input) {
        // If already double do nothing
        if (input instanceof RangeFloat) {
            return input;
        }
        // Otherwise get minimum and maximum values and convert it
        float min = input.getMin().floatValue();
        float max = input.getMax().floatValue();

        boolean minIncluded = input.isMinIncluded();
        boolean maxIncluded = input.isMaxIncluded();

        boolean nanIncluded = input.isNanIncluded();

        // New Double range
        return new RangeFloat(min, minIncluded, max, maxIncluded, nanIncluded);
    }

    public static Range convert(Range input, int dataType) {
        if (input == null) {
            return null;
        }
        // If already double do nothing
        if (input.getDataType().getDataType() == dataType) {
            return input;
        }

        boolean minIncluded = input.isMinIncluded();
        boolean maxIncluded = input.isMaxIncluded();

        Number min = input.getMin();
        Number max = input.getMax();

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            return new RangeByte(min.byteValue(), minIncluded, max.byteValue(), maxIncluded);
        case DataBuffer.TYPE_USHORT:
            return new RangeUshort(min.shortValue(), minIncluded, max.shortValue(), maxIncluded);
        case DataBuffer.TYPE_SHORT:
            return new RangeShort(min.shortValue(), minIncluded, max.shortValue(), maxIncluded);
        case DataBuffer.TYPE_INT:
            return new RangeInt(min.intValue(), minIncluded, max.intValue(), maxIncluded);
        case DataBuffer.TYPE_FLOAT:
            return new RangeFloat(min.floatValue(), minIncluded, max.floatValue(), maxIncluded,
                    input.isNanIncluded());
        case DataBuffer.TYPE_DOUBLE:
            return new RangeDouble(min.floatValue(), minIncluded, max.floatValue(), maxIncluded,
                    input.isNanIncluded());
        default:
            return null;
        }
    }

    public static Range convertToByteRange(Range input) {
        // If already double do nothing
        if (input instanceof RangeByte) {
            return input;
        }
        // Otherwise get minimum and maximum values and convert it
        byte min = input.getMin().byteValue();
        byte max = input.getMax().byteValue();

        boolean minIncluded = input.isMinIncluded();
        boolean maxIncluded = input.isMaxIncluded();

        // New Double range
        return new RangeByte(min, minIncluded, max, maxIncluded);
    }

    public static List<Range> subtract(Range r1, Range r2) {
        // Creation of the Range List
        List<Range> list = new ArrayList<Range>();

        // Populating the list
        /*
         * Check for equality between inputs
         */
        if (r1.equals(r2)) {
            return list; // empty list
        }

        Range common = intersect(r1, r2);

        /*
         * Check for no overlap between inputs
         */
        if (common == null) {
            list.add(r2);
            return list;
        }

        /*
         * Check if r1 enclosed r2
         */
        if (common.equals(r2)) {
            return list; // empty list
        }

        // Checks on the minimum/maximum
        double min1 = r1.getMin().doubleValue();
        double min2 = r2.getMin().doubleValue();
        double max1 = r1.getMax().doubleValue();
        double max2 = r2.getMax().doubleValue();

        // Checks on the comparison between the min and max
        boolean minmin = equals(min1, min2);
        boolean maxmax = equals(max1, max2);
        boolean minmax = equals(min1, max2);
        boolean maxmin = equals(max1, min2);

        // Case 0a) min1 equals to max2
        if (minmax) {
            if (r1.isMinIncluded()) {
                Range r = RangeFactory.create(min2, r2.isMinIncluded(), max2, false);
                list.add(r);
                return list;
            } else {
                list.add(r2);
                return list;
            }
        }
        // Case 0b) min2 equals to max1
        if (maxmin) {
            if (r1.isMaxIncluded()) {
                Range r = RangeFactory.create(min2, false, max2, r2.isMinIncluded());
                list.add(r);
                return list;
            } else {
                list.add(r2);
                return list;
            }
        }

        // Case 1) equal minimums and different max values
        if (minmin && max2 > max1 && !maxmax) {
            Range r = RangeFactory.create(max1, !r1.isMaxIncluded(), max2, r2.isMaxIncluded());
            list.add(r);
            return list;
        }
        // Case 2) equal maximum and different min values
        if (maxmax && min2 < min1 && !minmin) {
            Range r = RangeFactory.create(min2, r2.isMinIncluded(), min1, !r1.isMinIncluded());
            list.add(r);
            return list;
        }
        // Case 3) r2 on the left and r1 on the right
        if (min2 < min1 && max2 < max1) {
            Range r = RangeFactory.create(min2, r2.isMinIncluded(), min1, !r1.isMinIncluded());
            list.add(r);
            return list;
        }

        // Case 4) r1 on the left and r2 on the right
        if (min2 > min1 && max2 > max1) {
            Range r = RangeFactory.create(max1, !r1.isMaxIncluded(), max2, r2.isMaxIncluded());
            list.add(r);
            return list;
        }

        // Case 5) r1 contained in r2 (two ranges)
        if (min2 < min1 && max2 > max1) {
            Range r1New = RangeFactory.create(min2, r2.isMinIncluded(), min1, !r1.isMinIncluded());
            Range r2New = RangeFactory.create(max1, !r1.isMaxIncluded(), max2, r2.isMaxIncluded());
            list.add(r1New);
            list.add(r2New);
            return list;
        }

        return list;
    }

    public static Range intersect(Range r1, Range r2) {
        // Initial checks
        if (r1.contains(r2)) {
            return r2;
        }

        if (r2.contains(r1)) {
            return r1;
        }
        // Checks on the bounds

        double min1 = r1.getMin().doubleValue();
        double min2 = r2.getMin().doubleValue();
        double max1 = r1.getMax().doubleValue();
        double max2 = r2.getMax().doubleValue();

        // Checks on the comparison between the min and max
        boolean minmin = equals(min1, min2);
        boolean maxmax = equals(max1, max2);
        boolean minmax = equals(min1, max2);
        boolean maxmin = equals(max1, min2);

        // Check on the single point comparison
        if (minmax && r1.isMinIncluded() && r2.isMaxIncluded()) {
            return RangeFactory.create(min1, min1);
        }
        if (maxmin && r1.isMaxIncluded() && r2.isMinIncluded()) {
            return RangeFactory.create(min2, min2);
        }

        if ((min1 > max2 || min2 > max1)) {
            return null;
        }
        // More precise checks
        boolean min1Used = min1 > min2;
        boolean max1Used = max1 < max2;

        double minN = min1Used || minmin ? min1 : min2;
        double maxN = max1Used || maxmax ? max1 : max2;

        boolean minIncluded = (minmin && r1.isMinIncluded() && r2.isMinIncluded())
                || (min1Used ? r1.isMinIncluded() : r2.isMinIncluded());

        boolean maxIncluded = (maxmax && r1.isMaxIncluded() && r2.isMaxIncluded())
                || (max1Used ? r1.isMaxIncluded() : r2.isMaxIncluded());

        return RangeFactory.create(minN, minIncluded, maxN, maxIncluded);
    }

    public static boolean equals(double d1, double d2) {
        return Math.abs(d1 - d2) < TOLERANCE;
    }

    /**
     * Casts the give range to a target data type, or returns the original range in case the range
     * and target data type are the same
     * 
     * @param range
     * @param targetType
     * @return
     */
    public static Range cast(Range range, DataType targetType) {
        if (range.getDataType() == targetType) {
            return range;
        }

        switch (targetType) {
        case BYTE:
            return RangeFactory.create(range.getMin().byteValue(), range.getMax().byteValue());

        case SHORT:
            return RangeFactory.create(range.getMin().shortValue(), range.getMax().shortValue());

        case USHORT:
            return RangeFactory.create((short) (range.getMin().intValue() & 0xFFF),
                    (short) (range.getMax().shortValue() & 0xFFF));

        case INTEGER:
            return RangeFactory.create(range.getMin().intValue(), range.getMax().intValue());

        case LONG:
            return RangeFactory.create(range.getMin().longValue(), range.getMax().longValue());

        case FLOAT:
            return RangeFactory.create(range.getMin().floatValue(), range.getMax().floatValue());

        case DOUBLE:
            return RangeFactory.create(range.getMin().doubleValue(), range.getMax().doubleValue());

        default:
            throw new IllegalArgumentException("Unexpected data type " + targetType);
        }
    }
}
