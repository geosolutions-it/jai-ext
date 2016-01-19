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

import java.awt.image.DataBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract class used for checking if a selected value is inside the selected Range. All the methods throw an {@link UnsupportedOperationException}
 * but for every subclass one of these methods is overridden with the correct functionality. These 6 methods are different only for the data type
 * used. In this way it is possible to reach a better performance by using primitive variables than generic. All the subclasses can contain a Range
 * composed by a minimum and a maximum or a single-point Range. For Double and Float data type the NaN data can be used only with a single-point
 * Range.
 */
public abstract class Range {

    private static final Logger LOGGER = Logger.getLogger(Range.class.toString());

    private static final int PRIME_NUMBER = 37;

    public enum DataType {
        BYTE(Byte.class, DataBuffer.TYPE_BYTE), USHORT(Short.class, DataBuffer.TYPE_USHORT), SHORT(
                Short.class, DataBuffer.TYPE_SHORT), INTEGER(Integer.class, DataBuffer.TYPE_INT), FLOAT(
                Float.class, DataBuffer.TYPE_FLOAT), DOUBLE(Double.class, DataBuffer.TYPE_DOUBLE),
        // FIXME LONG VALUE CAN BE CHANGED IF LONG DATA TYPE TAKES ANOTHER VALUE
        LONG(Long.class, DataBuffer.TYPE_DOUBLE + 1);

        private Class<? extends Number> classType;

        private int dataType;

        private DataType(Class<? extends Number> classType, int dataType) {
            this.classType = classType;
            this.dataType = dataType;
        }

        public Class<? extends Number> getClassValue() {
            return classType;
        }

        public int getDataType() {
            return dataType;
        }

        public static int dataTypeFromClass(Class<?> classType) {
            if (classType == BYTE.getClassValue()) {
                return BYTE.getDataType();
            } else if (classType == SHORT.getClassValue()) {
                return SHORT.getDataType();
            } else if (classType == INTEGER.getClassValue()) {
                return INTEGER.getDataType();
            } else if (classType == FLOAT.getClassValue()) {
                return FLOAT.getDataType();
            } else if (classType == DOUBLE.getClassValue()) {
                return DOUBLE.getDataType();
            } else if (classType == LONG.getClassValue()) {
                return LONG.getDataType();
            } else {
                throw new IllegalArgumentException(
                        "This class does not belong to the already existing classes");
            }
        }

        public static Class<? extends Number> classFromType(int dataType) {
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                return Byte.class;
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
                return Short.class;
            case DataBuffer.TYPE_INT:
                return Integer.class;
            case DataBuffer.TYPE_FLOAT:
                return Float.class;
            case DataBuffer.TYPE_DOUBLE:
                return Double.class;
            case DataBuffer.TYPE_DOUBLE + 1:
                return Long.class;
            default:
                return null;
            }
        }
    }

    protected boolean isMinIncluded;

    protected boolean isMaxIncluded;

    Range(boolean isMinIncluded, boolean isMaxIncluded) {
        this.isMaxIncluded = isMaxIncluded;
        this.isMinIncluded = isMinIncluded;
    }

    /** Method for checking if a byte value is contained inside the Range */
    public boolean contains(byte value) {
        return containsN(value);
    }

    /**
     * Method for checking if a short/ushort value is contained inside the Range
     */
    public boolean contains(short value) {
        return containsN(value);
    }

    /** Method for checking if an integer value is contained inside the Range */
    public boolean contains(int value) {
        return containsN(value);
    }

    /** Method for checking if a float value is contained inside the Range */
    public boolean contains(float value) {
        return containsN(value);
    }

    /** Method for checking if a double value is contained inside the Range */
    public boolean contains(double value) {
        return containsN(value);
    }

    /** Method for checking if a long value is contained inside the Range */
    public boolean contains(long value) {
        return containsN(value);
    }

    /** Method for checking if a Generic value is contained inside the Range */
    public <T extends Number> boolean containsN(T value) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Wrong data type tested: Input: " + value.getClass()
                    + ", output: " + this.getDataType().getClassValue());
        }
        int dataType = this.getDataType().getDataType();
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            return contains(value.byteValue());
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            return contains(value.shortValue());
        case DataBuffer.TYPE_INT:
            return contains(value.intValue());
        case DataBuffer.TYPE_FLOAT:
            return contains(value.floatValue());
        case DataBuffer.TYPE_DOUBLE:
            return contains(value.doubleValue());
        case DataBuffer.TYPE_DOUBLE + 1:
            return contains(value.longValue());
        default:
            throw new IllegalArgumentException("Wrong data type");
        }
    }

    public boolean contains(Range other) {
        // NaN checks
        if (this.isNaN() && !other.isNaN()) {
            return false;
        }
        if (other.isNaN()) {
            if (this.isNanIncluded()) {
                return true;
            } else {
                return false;
            }
        }

        double min1 = this.getMin().doubleValue();
        double max1 = this.getMax().doubleValue();

        double min2 = other.getMin().doubleValue();
        double max2 = other.getMax().doubleValue();

        // Simple check
        boolean minContains = min1 < min2;
        boolean maxContains = max1 > max2;

        if (minContains && maxContains) {
            return true;
        }

        boolean minEquals = min1 == min2;
        boolean maxEquals = max1 == max2;

        // Check on equal bounds
        if (!minContains && minEquals) {
            minContains = this.isMinIncluded() && other.isMinIncluded();
        }
        if (!maxContains && maxEquals) {
            maxContains = this.isMaxIncluded() && other.isMaxIncluded();
        }

        if (minContains && maxContains) {
            return true;
        }

        return false;
    }

    public boolean intersects(Range other) {
        // Check if one of them is contained into the other
        if (this.contains(other) || other.contains(this)) {
            return true;
        }

        double min1 = this.getMin().doubleValue();
        double max1 = this.getMax().doubleValue();

        double min2 = other.getMin().doubleValue();
        double max2 = other.getMax().doubleValue();

        // Check the bounds
        boolean minCheck = this.isMinIncluded() && other.isMaxIncluded() ? min1 <= max2
                : min1 < max2;
        boolean maxCheck = this.isMaxIncluded() && other.isMinIncluded() ? max1 >= min2
                : max1 > min2;

        return minCheck && maxCheck;
    }

    public abstract Range intersection(Range other);

    public abstract Range union(Range other);

    /** Returns the Range data Type */
    public abstract DataType getDataType();

    /** Indicates if the Range is a degenerated single point Range or not */
    public abstract boolean isPoint();

    /** Returns the maximum bound of the Range */
    public abstract Number getMax();

    /** Returns the minimum bound of the Range */
    public abstract Number getMin();

    /**
     * Returns the maximum bound of the Range or the nearest one if not included
     */
    public abstract Number getMax(boolean isMaxIncluded);

    /**
     * Returns the minimum bound of the Range or the nearest one if not included
     */
    public abstract Number getMin(boolean isMinIncluded);

    /** Returns true if the current Range accepts NaN values */
    public boolean isNanIncluded() {
        return true;
    }

    /**
     * Returns true if and only if the current Range is a point representing NaN
     */
    public boolean isNaN() {
        return false;
    }

    /** Returns the a boolean indicating if the Maximum bound is included */
    public boolean isMaxIncluded() {
        return isMaxIncluded;
    }

    /** Returns the a boolean indicating if the Minimum bound is included */
    public boolean isMinIncluded() {
        return isMinIncluded;
    }

    /** Sets the isMinIncluded parameter */
    protected void setMinIncluded(boolean isMinIncluded) {
        this.isMinIncluded = isMinIncluded;
    }

    /** Sets the isMaxIncluded parameter */
    protected void setMaxIncluded(boolean isMaxIncluded) {
        this.isMaxIncluded = isMaxIncluded;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (super.equals(obj)) {
            return true;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Range)) {
            return false;
        }
        Range r1 = (Range) obj;
        if (r1.getDataType() != this.getDataType()) {
            return false;
        }
        if (r1.isPoint() != this.isPoint()) {
            return false;
        }
        if (r1.isMaxIncluded() != this.isMaxIncluded()) {
            return false;
        }
        if (r1.isPoint() != this.isMinIncluded()) {
            return false;
        }
        if (r1.getMin().doubleValue() != this.getMin().doubleValue()) {
            return false;
        }
        if (r1.getMax().doubleValue() != this.getMax().doubleValue()) {
            return false;
        }

        if (r1.isNanIncluded() != this.isNanIncluded()) {
            return false;
        }

        return true;
    }

    public int compare(Range other) {
        if (this.equals(other)) {
            return 0;
        }
        double min1 = this.getMin().doubleValue();
        double min2 = other.getMin().doubleValue();
        double max1 = this.getMax().doubleValue();
        double max2 = other.getMax().doubleValue();

        // Different minimum
        if (!RangeFactory.equals(min1, min2)) {
            if (min1 < min2) {
                return -1;
            } else {
                return 1;
            }
        } else {
            // Check if they are included
            if (this.isMinIncluded() && other.isMinIncluded()) {
                // Equal max
                if (RangeFactory.equals(max1, max2)) {
                    if (this.isMaxIncluded() && other.isMaxIncluded()) {
                        return 0;
                    } else if (this.isMaxIncluded()) {
                        return 1;
                    } else {
                        return -1;
                    }
                } else {
                    if (max1 < max2) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            } else {
                if (this.isMinIncluded()) {
                    return -1;
                } else {
                    return 1;
                }
            }

        }
    }

    @Override
    public int hashCode() {
        int result = 37;
            result += getDataType().getClass().hashCode();
            result = hash(isMaxIncluded, result);
            result = hash(isMinIncluded, result);
            result = hash(getMax().doubleValue(), result);
            result = hash(getMin().doubleValue(), result);
        return result;
    }
    
    public static int hash(boolean value, int seed) {
        // Use the same values than Boolean.hashCode()
        return seed * PRIME_NUMBER + (value ? 1231 : 1237);
    }
    
    public static int hash(double value, int seed) {
        return hash(Double.doubleToLongBits(value), seed);
    }
    
    public static int hash(long value, int seed) {
        return seed * PRIME_NUMBER + (((int) value) ^ ((int) (value >>> 32)));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        if (isMinIncluded()) {
            sb.append("[");
        } else {
            sb.append("(");
        }
        sb.append(getMin()).append(", ").append(getMax());
        if (isMaxIncluded()) {
            sb.append("]");
        } else {
            sb.append(")");
        }
        return sb.toString();
    }
}
