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
        if (minValue == maxValue && !minIncluded && !maxIncluded) {
            throw new IllegalArgumentException(
                    "Cannot create a single-point range without minimum and maximum "
                            + "bounds included");
        } else if (minValue == maxValue && (minIncluded || maxIncluded)) {
            return new RangeBytePoint(minValue);
        } else {
            return new RangeByte(minValue, minIncluded, maxValue, maxIncluded);
        }
    }

    public static Range create(byte value) {
        return new RangeBytePoint(value);
    }

    // Ushort data
    public static Range createU(short minValue, boolean minIncluded, short maxValue,
            boolean maxIncluded) {
        if (minValue == maxValue && !minIncluded && !maxIncluded) {
            throw new IllegalArgumentException(
                    "Cannot create a single-point range without minimum and maximum "
                            + "bounds included");
        } else if (minValue == maxValue && (minIncluded || maxIncluded)) {
            return new RangeShortPoint(minValue, true);
        } else {
            return new RangeUshort(minValue, minIncluded, maxValue, maxIncluded);
        }
    }

    public static Range createU(short value) {
        return new RangeShortPoint(value, true);
    }

    // Short data
    public static Range create(short minValue, boolean minIncluded, short maxValue,
            boolean maxIncluded) {
        if (minValue == maxValue && !minIncluded && !maxIncluded) {
            throw new IllegalArgumentException(
                    "Cannot create a single-point range without minimum and maximum "
                            + "bounds included");
        } else if (minValue == maxValue && (minIncluded || maxIncluded)) {
            return new RangeShortPoint(minValue, false);
        } else {
            return new RangeShort(minValue, minIncluded, maxValue, maxIncluded);
        }
    }

    // Both Ushort and Short
    public static Range create(short value) {
        return new RangeShortPoint(value, false);
    }

    // Integer data
    public static Range create(int minValue, boolean minIncluded, int maxValue, boolean maxIncluded) {
        if (minValue == maxValue && !minIncluded && !maxIncluded) {
            throw new IllegalArgumentException(
                    "Cannot create a single-point range without minimum and maximum "
                            + "bounds included");
        } else if (minValue == maxValue && (minIncluded || maxIncluded)) {
            return new RangeIntPoint(minValue);
        } else {
            return new RangeInt(minValue, minIncluded, maxValue, maxIncluded);
        }
    }

    public static Range create(int value) {
        return new RangeIntPoint(value);
    }

    // Float data
    public static Range create(float minValue, boolean minIncluded, float maxValue,
            boolean maxIncluded) {
        if (minValue == maxValue && !minIncluded && !maxIncluded) {
            throw new IllegalArgumentException(
                    "Cannot create a single-point range without minimum and maximum "
                            + "bounds included");
        } else if (minValue == maxValue && (minIncluded || maxIncluded)) {
            return new RangeFloatPoint(minValue);
        } else {
            return new RangeFloat(minValue, minIncluded, maxValue, maxIncluded);
        }
    }

    public static Range create(float value) {
        return new RangeFloatPoint(value);
    }

    // Double data
    public static Range create(double minValue, boolean minIncluded, double maxValue,
            boolean maxIncluded) {
        if (minValue == maxValue && !minIncluded && !maxIncluded) {
            throw new IllegalArgumentException(
                    "Cannot create a single-point range without minimum and maximum "
                            + "bounds included");
        } else if (minValue == maxValue && (minIncluded || maxIncluded)) {
            return new RangeDoublePoint(minValue);
        } else {
            return new RangeDouble(minValue, minIncluded, maxValue, maxIncluded);
        }
    }

    public static Range create(double value) {
        return new RangeDoublePoint(value);
    }

}
