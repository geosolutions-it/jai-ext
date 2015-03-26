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
package it.geosolutions.jaiext.piecewise;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.utilities.ImageUtilities;
import java.util.Arrays;

/**
 * Convenience class to group utilities methods for {@link DomainElement1D} and {@link Domain1D} implmentations.
 * 
 * @author Simone Giannecchini, GeoSolutions.
 * 
 */
public class PiecewiseUtilities {

    /**
     * Private Constructor, it cannot be instantiated
     */
    private PiecewiseUtilities() {
    }

    /**
     * Checks whether or not two DomainElement1Ds input range overlaps
     * 
     * @param domainElements to be checked
     * @param idx index to start with
     */
    public static void domainElementsOverlap(DomainElement1D[] domainElements, int idx) {
        // Two domain elements have overlapping ranges;
        // Format an error message...............
        final Range range1 = domainElements[idx - 1].getRange();
        final Range range2 = domainElements[idx].getRange();
        final Number[] args = new Number[] { range1.getMin(), range1.getMax(), range2.getMin(),
                range2.getMax() };
        String[] results = new String[4];
        for (int j = 0; j < args.length; j++) {
            final double value = (args[j]).doubleValue();
            if (Double.isNaN(value)) {
                String hex = Long.toHexString(Double.doubleToRawLongBits(value));
                results[j] = "NaN(" + hex + ')';
            } else {
                results[j] = value + "";
            }

        }
        throw new IllegalArgumentException("Provided ranges are overlapping:" + results[0] + " : "
                + results[1] + " / " + results[2] + " : " + results[3]);
    }

    /**
     * Makes sure that an argument is non-null.
     * 
     * @param name Argument name.
     * @param object User argument.
     * @throws IllegalArgumentException if {@code object} is null.
     */
    public static void ensureNonNull(final String name, final Object object)
            throws IllegalArgumentException {
        if (object == null) {
            throw new IllegalArgumentException("Input object is null");
        }
    }

    /**
     * Array binary search taking into account the fact that the input value to search can be NaN
     * 
     * Note: This method is not private in order to allows testing by {@link }.
     */
    public static int binarySearch(final double[] array, final double val) {
        int low = 0;
        int high = array.length - 1;
        final boolean keyIsNaN = Double.isNaN(val);
        while (low <= high) {
            final int mid = (low + high) >> 1;
            final double midVal = array[mid];
            if (midVal < val) { // Neither val is NaN, midVal is smaller
                low = mid + 1;
                continue;
            }
            if (midVal > val) { // Neither val is NaN, midVal is larger
                high = mid - 1;
                continue;
            }
            /*
             * The following is an adaptation of evaluator's comments for bug #4471414
             * (http://developer.java.sun.com/developer/bugParade/bugs/4471414. html). Extract from evaluator's comment:
             * 
             * [This] code is not guaranteed to give the desired results because of laxity in IEEE 754 regarding NaN values. There are actually two
             * types of NaNs, signaling NaNs and quiet NaNs. Java doesn't support the features necessary to reliably distinguish the two. However, the
             * relevant point is that copying a signaling NaN may (or may not, at the implementors discretion) yield a quiet NaN -- a NaN with a
             * different bit pattern (IEEE 754 6.2). Therefore, on IEEE 754 compliant platforms it may be impossible to find a signaling NaN stored in
             * an array since a signaling NaN passed as an argument to binarySearch may get replaced by a quiet NaN.
             */
            final long midRawBits = Double.doubleToRawLongBits(midVal);
            final long keyRawBits = Double.doubleToRawLongBits(val);
            if (midRawBits == keyRawBits) {
                return mid; // key found
            }
            final boolean midIsNaN = Double.isNaN(midVal);
            final boolean adjustLow;
            if (keyIsNaN) {
                // If (mid,key)==(!NaN, NaN): mid is lower.
                // If two NaN arguments, compare NaN bits.
                adjustLow = (!midIsNaN || midRawBits < keyRawBits);
            } else {
                // If (mid,key)==(NaN, !NaN): mid is greater.
                // Otherwise, case for (-0.0, 0.0) and (0.0, -0.0).
                adjustLow = (!midIsNaN && midRawBits < keyRawBits);
            }
            if (adjustLow)
                low = mid + 1;
            else
                high = mid - 1;
        }
        return -(low + 1); // key not found.
    }

    /**
     * Comparison between two double values
     */
    public static int compare(final double v1, final double v2) {
        if (Double.isNaN(v1) && Double.isNaN(v2)) {
            final long bits1 = Double.doubleToRawLongBits(v1);
            final long bits2 = Double.doubleToRawLongBits(v2);
            if (bits1 < bits2)
                return -1;
            if (bits1 > bits2)
                return +1;
        }
        return Double.compare(v1, v2);
    }

    /**
     * Checks if the array is sorted
     */
    public static boolean isSorted(final DefaultDomainElement1D[] domains) {
        if (domains == null)
            return true;
        for (int i = 1; i < domains.length; i++) {
            final DefaultDomainElement1D d1 = domains[i];
            assert !(d1.getInputMinimum() > d1.getInputMaximum()) : d1;
            final DefaultDomainElement1D d0 = domains[i - 1];
            assert !(d0.getInputMinimum() > d0.getInputMaximum()) : d0;
            if (compare(d0.getInputMaximum(), d1.getInputMinimum()) > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a {@code double} value for the specified number. If {@code direction} is non-zero, then this method will returns the closest
     * representable number of type {@code type} before or after the double value.
     * 
     * @param type The range element class. {@code number} must be an instance of this class (this will not be checked).
     * @param number The number to transform to a {@code double} value.
     * @param direction -1 to return the previous representable number, +1 to return the next representable number, or 0 to return the number with no
     *        change.
     */
    public static double doubleValue(final Class<? extends Number> type, final Number number,
            final int direction) {
        assert (direction >= -1) && (direction <= +1) : direction;
        return ImageUtilities.rool(type, number.doubleValue(), direction);
    }

    /**
     * Returns a linear transform with the supplied scale and offset values.
     * 
     * @param scale The scale factor. May be 0 for a constant transform.
     * @param offset The offset value. May be NaN.
     */
    public static MathTransformation createLinearTransform1D(final double scale, final double offset) {
        return SingleDimensionTransformation.create(scale, offset);
    }

    /**
     * Create a linear transform mapping values from {@code sampleValueRange} to {@code geophysicsValueRange}.
     */
    public static MathTransformation createLinearTransform1D(final Range sourceRange,
            final Range destinationRange) {
        final Class<? extends Number> sType = sourceRange.getDataType().getClassValue();
        final Class<? extends Number> dType = destinationRange.getDataType().getClassValue();
        /*
         * First, find the direction of the adjustment to apply to the ranges if we wanted all values to be inclusives. Then, check if the adjustment
         * is really needed: if the values of both ranges are inclusive or exclusive, then there is no need for an adjustment before computing the
         * coefficient of a linear relation.
         */
        int sMinInc = sourceRange.isMinIncluded() ? 0 : +1;
        int sMaxInc = sourceRange.isMaxIncluded() ? 0 : -1;
        int dMinInc = destinationRange.isMinIncluded() ? 0 : +1;
        int dMaxInc = destinationRange.isMaxIncluded() ? 0 : -1;

        /*
         * Now, extracts the minimal and maximal values and computes the linear coefficients.
         */
        final double minSource = doubleValue(sType, sourceRange.getMin(), sMinInc);
        final double maxSource = doubleValue(sType, sourceRange.getMax(), sMaxInc);
        final double minDestination = doubleValue(dType, destinationRange.getMin(), dMinInc);
        final double maxDestination = doubleValue(dType, destinationRange.getMax(), dMaxInc);

        // /////////////////////////////////////////////////////////////////
        //
        // optimizations
        //
        // /////////////////////////////////////////////////////////////////
        // //
        //
        // If the output range is a single value let's create a constant
        // transform
        //
        // //
        if (PiecewiseUtilities.compare(minDestination, maxDestination) == 0)
            return SingleDimensionTransformation.create(0, minDestination);

        // //
        //
        // If the input range is a single value this transform ca be created
        // only if we map to another single value
        //
        // //
        if (PiecewiseUtilities.compare(minSource, maxSource) == 0)
            throw new IllegalArgumentException("Impossible to map a single value to a range.");

        double scale = (maxDestination - minDestination) / (maxSource - minSource);
        // /////////////////////////////////////////////////////////////////
        //
        // Take into account the fact that the maxSample and the minSample can
        // be
        // similar hence we have a constant transformation.
        //
        // /////////////////////////////////////////////////////////////////
        if (Double.isNaN(scale))
            scale = 0;
        final double offset = minDestination - scale * minSource;
        return createLinearTransform1D(scale, offset);
    }

    /**
     * Returns a hash code for the specified object, which may be an array. This method returns one of the following values:
     * <p>
     * <ul>
     * <li>If the supplied object is {@code null}, then this method returns 0.</li>
     * <li>Otherwise if the object is an array of objects, then {@link Arrays#deepHashCode(Object[])} is invoked.</li>
     * <li>Otherwise if the object is an array of primitive type, then the corresponding {@link Arrays#hashCode(double[]) Arrays.hashCode(...)} method
     * is invoked.</li>
     * <li>Otherwise {@link Object#hashCode()} is invoked.
     * <li>
     * </ul>
     * <p>
     * This method should be invoked <strong>only</strong> if the object type is declared exactly as {@code Object}, not as some subtype like
     * {@code Object[]}, {@code String} or {@code float[]}. In the later cases, use the appropriate {@link Arrays} method instead.
     * 
     * @param object The object to compute hash code. May be {@code null}.
     * @return The hash code of the given object.
     */
    public static int deepHashCode(final Object object) {
        if (object == null) {
            return 0;
        }
        if (object instanceof Object[]) {
            return Arrays.deepHashCode((Object[]) object);
        }
        if (object instanceof double[]) {
            return Arrays.hashCode((double[]) object);
        }
        if (object instanceof float[]) {
            return Arrays.hashCode((float[]) object);
        }
        if (object instanceof long[]) {
            return Arrays.hashCode((long[]) object);
        }
        if (object instanceof int[]) {
            return Arrays.hashCode((int[]) object);
        }
        if (object instanceof short[]) {
            return Arrays.hashCode((short[]) object);
        }
        if (object instanceof byte[]) {
            return Arrays.hashCode((byte[]) object);
        }
        if (object instanceof char[]) {
            return Arrays.hashCode((char[]) object);
        }
        if (object instanceof boolean[]) {
            return Arrays.hashCode((boolean[]) object);
        }
        return object.hashCode();
    }

    /**
     * A prime number used for hash code computation.
     */
    private static final int PRIME_NUMBER = 37;

    /**
     * Alters the given seed with the hash code value computed from the given value. The givan object may be null. This method do <strong>not</strong>
     * iterates recursively in array elements. If array needs to be hashed, use one of {@link Arrays} method or {@link #deepHashCode deepHashCode}
     * instead.
     * <p>
     * <b>Note on assertions:</b> There is no way to ensure at compile time that this method is not invoked with an array argument, while doing so
     * would usually be a program error. Performing a systematic argument check would impose a useless overhead for correctly implemented
     * {@link Object#hashCode} methods. As a compromise we perform this check at runtime only if assertions are enabled. Using assertions for argument
     * check in a public API is usually a deprecated practice, but we make an exception for this particular method.
     * 
     * @param value The value whose hash code to compute, or {@code null}.
     * @param seed The hash code value computed so far. If this method is invoked for the first field, then any arbitrary value (preferably different
     *        for each class) is okay.
     * @return An updated hash code value.
     * @throws AssertionError If assertions are enabled and the given value is an array.
     */
    public static int hash(Object value, int seed) throws AssertionError {
        seed *= PRIME_NUMBER;
        if (value != null) {
            assert !value.getClass().isArray() : value;
            seed += value.hashCode();
        }
        return seed;
    }

    /**
     * Alters the given seed with the hash code value computed from the given value.
     * 
     * @param value The value whose hash code to compute.
     * @param seed The hash code value computed so far. If this method is invoked for the first field, then any arbitrary value (preferably different
     *        for each class) is okay.
     * @return An updated hash code value.
     */
    public static int hash(double value, int seed) {
        return hash(Double.doubleToLongBits(value), seed);
    }

    /**
     * Alters the given seed with the hash code value computed from the given value. {@code byte} and {@code short} primitive types are handled by
     * this method as well through implicit widening conversion.
     * 
     * @param value The value whose hash code to compute.
     * @param seed The hash code value computed so far. If this method is invoked for the first field, then any arbitrary value (preferably different
     *        for each class) is okay.
     * @return An updated hash code value.
     */
    public static int hash(long value, int seed) {
        return seed * PRIME_NUMBER + (((int) value) ^ ((int) (value >>> 32)));
    }

    /**
     * Returns {@code true} if the given doubles are equals. Positive and negative zero are considered different, while a NaN value is considered
     * equal to other NaN values.
     * 
     * @param o1 The first value to compare.
     * @param o2 The second value to compare.
     * @return {@code true} if both values are equal.
     * 
     * @see Double#equals
     */
    public static boolean equals(double o1, double o2) {
        if (Double.doubleToLongBits(o1) == Double.doubleToLongBits(o2))
            return true;

        double tol = getTolerance();
        final double min = o1 - Math.signum(o1) * o1 * tol;
        final double max = o1 + Math.signum(o1) * o1 * tol;
        return min <= o2 && o2 <= max;
    }

    public static boolean equals(Object object1, Object object2) {
        assert object1 == null || !object1.getClass().isArray() : object1;
        assert object2 == null || !object2.getClass().isArray() : object2;
        return (object1 == object2) || (object1 != null && object1.equals(object2));
    }

    /**
     * Gathers the tolerance for floating point comparisons
     * 
     * @return The tolerance set in the JVM properties, or its default value if not set
     */
    private static double getTolerance() {
        Double tol = Double.parseDouble(System.getProperty("jaiext.piecewise.tolerance"));
        if (tol == null) {
            return 0.0d;
        }
        return tol;
    }

    public static boolean equals(Range outputRange, Range outputRange2) {
        return outputRange.equals(outputRange2);
    }
}
