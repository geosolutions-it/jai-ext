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
package it.geosolutions.jaiext.algebra;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ROI;
import javax.media.jai.RenderableOp;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderableRegistryMode;
import javax.media.jai.registry.RenderedRegistryMode;

import com.sun.media.jai.util.ImageUtil;

public class AlgebraDescriptor extends OperationDescriptorImpl {

    public enum Operator {
        SUM(0, 0, true) {
            @Override
            public byte calculate(byte... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] += values[i];
                    }
                }
                return values[0];
            }

            @Override
            public short calculate(boolean isUshort, short... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] += values[i];
                    }
                }
                return values[0];
            }

            @Override
            public short calculate(short... values) {
                return calculate(false, values);
            }

            @Override
            public int calculate(int... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] += values[i];
                    }
                }
                return values[0];
            }

            @Override
            public float calculate(float... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] += values[i];
                    }
                }
                return values[0];
            }

            @Override
            public double calculate(double... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] += values[i];
                    }
                }
                return values[0];
            }

            @Override
            public long calculateL(long... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] += values[i];
                    }
                }
                return values[0];
            }
        },
        SUBTRACT(1, 0, true) {
            @Override
            public byte calculate(byte... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] -= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public short calculate(boolean isUshort, short... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] -= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public short calculate(short... values) {
                return calculate(false, values);
            }

            @Override
            public int calculate(int... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] -= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public float calculate(float... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] -= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public double calculate(double... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] -= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public long calculateL(long... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] -= values[i];
                    }
                }
                return values[0];
            }
        },
        MULTIPLY(2, 1, true) {
            @Override
            public byte calculate(byte... values) {
                long temp = 1;
                if (values.length > 1) {
                    for (int i = 0; i < values.length; i++) {
                        temp *= values[i];
                    }
                    if (temp > Byte.MAX_VALUE) {
                        values[0] = Byte.MAX_VALUE;
                    } else if (temp < Byte.MIN_VALUE) {
                        values[0] = Byte.MIN_VALUE;
                    } else {
                        values[0] = (byte) temp;
                    }
                }
                return values[0];
            }

            @Override
            public short calculate(boolean isUshort, short... values) {
                long temp = 1;
                if (values.length > 1) {
                    for (int i = 0; i < values.length; i++) {
                        temp *= values[i];
                    }
                    if (isUshort) {
                        if (temp > USHORT_MAX_VALUE) {
                            values[0] = (short) USHORT_MAX_VALUE;
                        } else if (temp < 0) {
                            values[0] = 0;
                        } else {
                            values[0] = (short) temp;
                        }
                    } else {
                        if (temp > Short.MAX_VALUE) {
                            values[0] = Short.MAX_VALUE;
                        } else if (temp < Short.MIN_VALUE) {
                            values[0] = Short.MIN_VALUE;
                        } else {
                            values[0] = (short) temp;
                        }
                    }
                }
                return values[0];
            }

            @Override
            public short calculate(short... values) {
                return calculate(false, values);
            }

            @Override
            public int calculate(int... values) {
                long temp = 1;
                if (values.length > 1) {
                    for (int i = 0; i < values.length; i++) {
                        temp *= values[i];
                    }
                    if (temp > Integer.MAX_VALUE) {
                        values[0] = Integer.MAX_VALUE;
                    } else if (temp < Integer.MIN_VALUE) {
                        values[0] = Integer.MIN_VALUE;
                    } else {
                        values[0] = (int) temp;
                    }
                }
                return values[0];
            }

            @Override
            public float calculate(float... values) {
                double temp = 1;
                if (values.length > 1) {
                    for (int i = 0; i < values.length; i++) {
                        temp *= values[i];
                    }
                    if (temp > Float.MAX_VALUE) {
                        values[0] = Float.MAX_VALUE;
                    } else if (temp < -Float.MAX_VALUE) {
                        values[0] = -Float.MAX_VALUE;
                    } else {
                        values[0] = (float) temp;
                    }
                }
                return values[0];
            }

            @Override
            public double calculate(double... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] *= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public long calculateL(long... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] *= values[i];
                    }
                }
                return values[0];
            }
        },
        DIVIDE(3, 1, true) {
            @Override
            public byte calculate(byte... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        if (values[i] == 0 && values[0] >= 0) {
                            values[0] = Byte.MAX_VALUE;
                        } else if (values[i] == 0 && values[0] < 0) {
                            values[0] = Byte.MIN_VALUE;
                        } else {
                            values[0] /= values[i];
                        }
                    }
                }
                return values[0];
            }

            @Override
            public short calculate(boolean isUshort, short... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        if (values[i] == 0 && values[0] >= 0) {
                            values[0] = (short) (isUshort ? USHORT_MAX_VALUE : Short.MAX_VALUE);
                        } else if (values[i] == 0 && values[0] < 0) {
                            values[0] = isUshort ? 0 : Short.MIN_VALUE;
                        } else {
                            values[0] /= values[i];
                        }
                    }
                }
                return values[0];
            }

            @Override
            public short calculate(short... values) {
                return calculate(false, values);
            }

            @Override
            public int calculate(int... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        if (values[i] == 0 && values[0] >= 0) {
                            values[0] = Integer.MAX_VALUE;
                        } else if (values[i] == 0 && values[0] < 0) {
                            values[0] = Integer.MIN_VALUE;
                        } else {
                            values[0] /= values[i];
                        }
                    }
                }
                return values[0];
            }

            @Override
            public float calculate(float... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] /= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public double calculate(double... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] /= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public long calculateL(long... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        if (values[i] == 0 && values[0] >= 0) {
                            values[0] = Long.MAX_VALUE;
                        } else if (values[i] == 0 && values[0] < 0) {
                            values[0] = Long.MIN_VALUE;
                        } else {
                            values[0] /= values[i];
                        }
                    }
                }
                return values[0];
            }
        },
        AND(4, Long.MAX_VALUE, true) {

            @Override
            public byte calculate(byte... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] &= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public short calculate(boolean isUshort, short... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] &= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public short calculate(short... values) {
                return calculate(false, values);
            }

            @Override
            public int calculate(int... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] &= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public float calculate(float... values) {
                throw new UnsupportedOperationException(
                        "Float data type is not supported for this operation");
            }

            @Override
            public double calculate(double... values) {
                throw new UnsupportedOperationException(
                        "Double data type is not supported for this operation");
            }

            @Override
            public long calculateL(long... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] &= values[i];
                    }
                }
                return values[0];
            }

            public boolean isDataTypeSupported(int dataType) {
                return dataType != DataBuffer.TYPE_FLOAT && dataType != DataBuffer.TYPE_DOUBLE;
            }
        },
        OR(5, 0, true) {

            @Override
            public byte calculate(byte... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] |= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public short calculate(boolean isUshort, short... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] |= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public short calculate(short... values) {
                return calculate(false, values);
            }

            @Override
            public int calculate(int... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] |= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public float calculate(float... values) {
                throw new UnsupportedOperationException(
                        "Float data type is not supported for this operation");

            }

            @Override
            public double calculate(double... values) {
                throw new UnsupportedOperationException(
                        "Double data type is not supported for this operation");

            }

            @Override
            public long calculateL(long... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] |= values[i];
                    }
                }
                return values[0];
            }

            public boolean isDataTypeSupported(int dataType) {
                return dataType != DataBuffer.TYPE_FLOAT && dataType != DataBuffer.TYPE_DOUBLE;
            }
        },
        NOT(6, 0, false) {

            @Override
            public byte calculate(byte... values) {
                return (byte) ~values[0];

            }

            @Override
            public short calculate(boolean isUshort, short... values) {
                return (short) ~values[0];
            }

            @Override
            public short calculate(short... values) {
                return calculate(false, values);
            }

            @Override
            public int calculate(int... values) {
                return ~values[0];
            }

            @Override
            public float calculate(float... values) {
                throw new UnsupportedOperationException(
                        "Float data type is not supported for this operation");
            }

            @Override
            public double calculate(double... values) {
                throw new UnsupportedOperationException(
                        "Double data type is not supported for this operation");
            }

            @Override
            public long calculateL(long... values) {
                return ~values[0];
            }

            public boolean isDataTypeSupported(int dataType) {
                return dataType != DataBuffer.TYPE_FLOAT && dataType != DataBuffer.TYPE_DOUBLE;
            }
        },
        XOR(7, 0, true) {

            @Override
            public byte calculate(byte... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] ^= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public short calculate(boolean isUshort, short... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] ^= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public short calculate(short... values) {
                return calculate(false, values);
            }

            @Override
            public int calculate(int... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] ^= values[i];
                    }
                }
                return values[0];
            }

            @Override
            public float calculate(float... values) {
                throw new UnsupportedOperationException(
                        "Float data type is not supported for this operation");

            }

            @Override
            public double calculate(double... values) {
                throw new UnsupportedOperationException(
                        "Double data type is not supported for this operation");

            }

            @Override
            public long calculateL(long... values) {
                if (values.length > 1) {
                    for (int i = 1; i < values.length; i++) {
                        values[0] ^= values[i];
                    }
                }
                return values[0];
            }

            public boolean isDataTypeSupported(int dataType) {
                return dataType != DataBuffer.TYPE_FLOAT && dataType != DataBuffer.TYPE_DOUBLE;
            }
        },
        EXP(8, 0, false) {

            private byte[] byteTable;

            protected void initialization() {

                if (byteTable != null)
                    return;

                byteTable = new byte[0x100];

                /*
                 * exp(5) = 148.4131591... exp(6) = 403.4287935... Calculate up to 5 and set the rest to the maximum value.
                 */
                byteTable[0] = 1;

                for (int i = 1; i < 6; i++) {
                    byteTable[i] = (byte) (Math.exp(i) + 0.5);
                }

                for (int i = 6; i < 0x100; i++) {
                    byteTable[i] = (byte) ImageUtil.BYTE_MASK;
                }
            }

            @Override
            public byte calculate(byte... values) {
                return byteTable[values[0] & ImageUtil.BYTE_MASK];
            }

            @Override
            public short calculate(boolean isUshort, short... values) {
                double value = values[0];
                if (isUshort) {
                    value = values[0] & ImageUtil.USHORT_MASK;
                    if (value == 0) {
                        return 1;
                    } else if (value > USHORT_UPPER_BOUND) {
                        return (short) ImageUtil.USHORT_MASK;
                    } else {
                        return (short) (Math.exp(value) + 0.5);
                    }
                } else {
                    if (value < LOWER_BOUND) {
                        return 0;
                    } else if (value == 0) {
                        return 1;
                    } else if (value > SHORT_UPPER_BOUND) {
                        return Short.MAX_VALUE;
                    } else {
                        return (short) (Math.exp(value) + 0.5);
                    }
                }
            }

            @Override
            public short calculate(short... values) {
                return calculate(false, values);
            }

            @Override
            public int calculate(int... values) {
                double value = values[0];
                if (value < LOWER_BOUND) {
                    return 0;
                } else if (value == 0) {
                    return 1;
                } else if (value > INT_UPPER_BOUND) {
                    return Integer.MAX_VALUE;
                } else {
                    return (int) (Math.exp(value) + 0.5);
                }
            }

            @Override
            public float calculate(float... values) {
                return (float) Math.exp(values[0]);
            }

            @Override
            public double calculate(double... values) {
                return Math.exp(values[0]);
            }

            @Override
            public long calculateL(long... values) {
                double value = values[0];
                if (value < LOWER_BOUND) {
                    return 0;
                } else if (value == 0) {
                    return 1;
                } else if (value > LONG_UPPER_BOUND) {
                    return Long.MAX_VALUE;
                } else {
                    return (long) (Math.exp(value) + 0.5);
                }
            }

            @Override
            public boolean isUshortSupported() {
                return true;
            }
        },
        LOG(9, 0, false) {

            private byte[] byteTable;

            protected void initialization() {

                if (byteTable != null)
                    return;

                byteTable = new byte[0x100];

                byteTable[0] = 0; // minimum byte value
                byteTable[1] = 0;

                for (int i = 2; i < 0x100; i++) {
                    byteTable[i] = (byte) (Math.log(i) + 0.5);
                }
            }

            @Override
            public byte calculate(byte... values) {
                return byteTable[values[0] & ImageUtil.BYTE_MASK];
            }

            @Override
            public short calculate(boolean isUshort, short... values) {
                return (short) Math
                        .log((isUshort ? values[0] & ImageUtil.USHORT_MASK : values[0]) + 0.5);
            }

            @Override
            public short calculate(short... values) {
                return calculate(false, values);
            }

            @Override
            public int calculate(int... values) {
                double value = values[0];
                if (value > 0) {
                    return (int) (Math.log(value) + 0.5);
                } else if (value == 0) {
                    return 0;
                } else {
                    return -1;
                }
            }

            @Override
            public float calculate(float... values) {
                return (float) Math.log(values[0]);
            }

            @Override
            public double calculate(double... values) {
                return Math.log(values[0]);
            }

            @Override
            public long calculateL(long... values) {
                double value = values[0];
                if (value > 0) {
                    return (long) (Math.log(value) + 0.5);
                } else if (value == 0) {
                    return 0;
                } else {
                    return -1;
                }
            }

            @Override
            public boolean isUshortSupported() {
                return true;
            }
        },
        ABSOLUTE(10, 0, false) {

            @Override
            public byte calculate(byte... values) {
                return values[0];
            }

            @Override
            public short calculate(boolean isUshort, short... values) {
                if (isUshort) {
                    return values[0];
                }
                short value = values[0];
                if ((value != Short.MIN_VALUE) && (value & Short.MIN_VALUE) != 0) {
                    // negative value
                    return (short) -values[0];
                } else {
                    // It is either the minimum of short
                    // or a positive number;
                    return values[0];
                }
            }

            @Override
            public short calculate(short... values) {
                return calculate(false, values);
            }

            @Override
            public int calculate(int... values) {
                int value = values[0];
                if ((value != Integer.MIN_VALUE) && (value & Integer.MIN_VALUE) != 0) {
                    // negative value
                    return -values[0];
                } else {
                    // It is either the minimum of integer
                    // or a positive number;
                    return values[0];
                }
            }

            @Override
            public float calculate(float... values) {
                if (values[0] <= 0.0f) {
                    return 0.0f - values[0];
                } else {
                    return values[0];
                }
            }

            @Override
            public double calculate(double... values) {
                if (values[0] <= 0.0d) {
                    return 0.0d - values[0];
                } else {
                    return values[0];
                }
            }

            @Override
            public long calculateL(long... values) {
                long value = values[0];
                if ((value != Long.MIN_VALUE) && (value & Long.MIN_VALUE) != 0) {
                    // negative value
                    return -values[0];
                } else {
                    // It is either the minimum of long
                    // or a positive number;
                    return values[0];
                }
            }

            @Override
            public boolean isUshortSupported() {
                return true;
            }
        },
        INVERT(11, 0, false) {

            @Override
            public byte calculate(byte... values) {
                return (byte) (255 - (values[0] & 0xFF));
            }

            @Override
            public short calculate(boolean isUshort, short... values) {
                return (short) (isUshort ? USHORT_MAX_VALUE - values[0]
                        : (Short.MAX_VALUE - values[0]));
            }

            @Override
            public short calculate(short... values) {
                return calculate(false, values);
            }

            @Override
            public int calculate(int... values) {
                return Integer.MAX_VALUE - values[0];
            }

            @Override
            public float calculate(float... values) {
                throw new UnsupportedOperationException(
                        "Float data type is not supported for this operation");
            }

            @Override
            public double calculate(double... values) {
                throw new UnsupportedOperationException(
                        "Double data type is not supported for this operation");
            }

            @Override
            public long calculateL(long... values) {
                return Long.MAX_VALUE - values[0];
            }

            @Override
            public boolean isUshortSupported() {
                return true;
            }

            public boolean isDataTypeSupported(int dataType) {
                return dataType != DataBuffer.TYPE_FLOAT && dataType != DataBuffer.TYPE_DOUBLE;
            }
        }, DIVIDE_INTO(12, 0, true) {
            @Override
            public byte calculate(byte... values) {
                if (values.length > 1) {
                    if(values.length == 2){
                        if (values[0] == 0 && values[1] >= 0) {
                            return Byte.MAX_VALUE;
                        } else if (values[0] == 0 && values[1] < 0) {
                            return Byte.MIN_VALUE;
                        } else {
                            return (byte) (values[1] / values[0]);
                        }
                    } else {
                        int result = 0;
                        int first = values.length - 1;
                        for (int i = first; i <= 0; i--) {
                            if(i == 0){
                                return (byte) result;
                            } else if(i == first){
                                result = values[i];
                            }else{
                                if (values[i - 1] == 0 && values[i] >= 0) {
                                    result = Byte.MAX_VALUE;
                                } else if (values[i - 1] == 0 && values[i] < 0) {
                                    result = Byte.MIN_VALUE;
                                } else {
                                    result = result / values[i - 1];
                                }
                            }
                        }
                    }
                }
                return values[0];
            }

            @Override
            public short calculate(boolean isUshort, short... values) {
                if (values.length > 1) {
                    if(values.length == 2){
                        if (values[0] == 0 && values[1] >= 0) {
                            return (short) (isUshort ? USHORT_MAX_VALUE : Short.MAX_VALUE);
                        } else if (values[0] == 0 && values[1] < 0) {
                            return (short) (isUshort ? USHORT_MAX_VALUE : Short.MAX_VALUE);
                        } else {
                            return (short) (values[1] / values[0]);
                        }
                    } else {
                        int result = 0;
                        int first = values.length - 1;
                        for (int i = first; i <= 0; i--) {
                            if(i == 0){
                                return (short) result;
                            } else if(i == first){
                                result = values[i];
                            }else{
                                if (values[i - 1] == 0 && values[i] >= 0) {
                                    result = (short) (isUshort ? USHORT_MAX_VALUE : Short.MAX_VALUE);
                                } else if (values[i - 1] == 0 && values[i] < 0) {
                                    result = (short) (isUshort ? USHORT_MAX_VALUE : Short.MAX_VALUE);
                                } else {
                                    result = result / values[i - 1];
                                }
                            }
                        }
                    }
                }
                return values[0];
            }

            @Override
            public short calculate(short... values) {
                return calculate(false, values);
            }

            @Override
            public int calculate(int... values) {
                if (values.length > 1) {
                    if(values.length == 2){
                        if (values[0] == 0 && values[1] >= 0) {
                            return Integer.MAX_VALUE;
                        } else if (values[0] == 0 && values[1] < 0) {
                            return Integer.MIN_VALUE;
                        } else {
                            return (int) (values[1] / values[0]);
                        }
                    } else {
                        long result = 0;
                        int first = values.length - 1;
                        for (int i = first; i <= 0; i--) {
                            if(i == 0){
                                return (int) result;
                            } else if(i == first){
                                result = values[i];
                            }else{
                                if (values[i - 1] == 0 && values[i] >= 0) {
                                    result = Integer.MAX_VALUE;
                                } else if (values[i - 1] == 0 && values[i] < 0) {
                                    result = Integer.MIN_VALUE;
                                } else {
                                    result = result / values[i - 1];
                                }
                            }
                        }
                    }
                }
                return values[0];
            }

            @Override
            public float calculate(float... values) {
                if (values.length > 1) {
                    if(values.length == 2){
                        return values[1]/values[0];
                    } else {
                        double result = 0;
                        int first = values.length - 1;
                        for (int i = first; i <= 0; i--) {
                            if(i == 0){
                                return (float) result;
                            } else if(i == first){
                                result = values[i];
                            }else{
                                result = result / values[i - 1];
                            }
                        }
                    }
                }
                return values[0];
            }

            @Override
            public double calculate(double... values) {
                if (values.length > 1) {
                    if(values.length == 2){
                        return values[1]/values[0];
                    } else {
                        double result = 0;
                        int first = values.length - 1;
                        for (int i = first; i <= 0; i--) {
                            if(i == 0){
                                return result;
                            } else if(i == first){
                                result = values[i];
                            }else{
                                result = result / values[i - 1];
                            }
                        }
                    }
                }
                return values[0];
            }

            @Override
            public long calculateL(long... values) {
                if (values.length > 1) {
                    if(values.length == 2){
                        if (values[0] == 0 && values[1] >= 0) {
                            return Long.MAX_VALUE;
                        } else if (values[0] == 0 && values[1] < 0) {
                            return Long.MIN_VALUE;
                        } else {
                            return (values[1] / values[0]);
                        }
                    } else {
                        long result = 0;
                        int first = values.length - 1;
                        for (int i = first; i <= 0; i--) {
                            if(i == 0){
                                return (int) result;
                            } else if(i == first){
                                result = values[i];
                            }else{
                                if (values[i - 1] == 0 && values[i] >= 0) {
                                    result = Long.MAX_VALUE;
                                } else if (values[i - 1] == 0 && values[i] < 0) {
                                    result = Long.MIN_VALUE;
                                } else {
                                    result = result / values[i - 1];
                                }
                            }
                        }
                    }
                }
                return values[0];
            }
        },SUBTRACT_FROM(13, 0, true) {
            @Override
            public byte calculate(byte... values) {
                int length = values.length;
                byte result = values[length - 1];
                if (length > 1) {
                    for (int i = length -2; i >= 0; i--) {
                        result -= values[i];
                    }
                }
                return result;
            }

            @Override
            public short calculate(boolean isUshort, short... values) {
                int length = values.length;
                short result = values[length - 1];
                if (length > 1) {
                    for (int i = length -2; i >= 0; i--) {
                        result -= values[i];
                    }
                }
                return result;
            }

            @Override
            public short calculate(short... values) {
                return calculate(false, values);
            }

            @Override
            public int calculate(int... values) {
                int length = values.length;
                int result = values[length - 1];
                if (length > 1) {
                    for (int i = length -2; i >= 0; i--) {
                        result -= values[i];
                    }
                }
                return result;
            }

            @Override
            public float calculate(float... values) {
                int length = values.length;
                float result = values[length - 1];
                if (length > 1) {
                    for (int i = length -2; i >= 0; i--) {
                        result -= values[i];
                    }
                }
                return result;
            }

            @Override
            public double calculate(double... values) {
                int length = values.length;
                double result = values[length - 1];
                if (length > 1) {
                    for (int i = length -2; i >= 0; i--) {
                        result -= values[i];
                    }
                }
                return result;
            }

            @Override
            public long calculateL(long... values) {
                int length = values.length;
                long result = values[length - 1];
                if (length > 1) {
                    for (int i = length -2; i >= 0; i--) {
                        result -= values[i];
                    }
                }
                return result;
            }
        };

        private final double nullValue;

        private final int type;

        private final boolean supportsMultipleValues;

        private Operator(int type, double nullValue, boolean supportsMultipleValues) {
            this.nullValue = nullValue;
            this.type = type;
            this.supportsMultipleValues = supportsMultipleValues;
            initialization();
        }

        protected void initialization() {
        }

        /**
         * The largest unsigned short to get a non-overflowed exponential result. i.e. cloeset to 65536. exp(11) = 59874.14171, exp(12) = 162754.7914
         */
        private static int USHORT_UPPER_BOUND = 11;

        /**
         * The largest short to get a non-overflowed exponential result. i.e. closest to 32767. exp(10) = 22026.46579, exp(11) = 59874.14171
         */
        public static int SHORT_UPPER_BOUND = 10;

        /**
         * The largest int to get a non-overflowed exponential result. i.e. closest to 2**31-1 = 2147483647. exp(21) = 1318815734, exp(22) =
         * 3584912846.
         */
        public static int INT_UPPER_BOUND = 21;

        /**
         * The largest int to get a non-overflowed exponential result. i.e. closest to 2**63-1
         */
        public static int LONG_UPPER_BOUND = 43;

        /**
         * The smallest integer to get a non-zero exponential result is 0. i.e. exp(0) = 1; exp(-1) = 0.367879441, which will be stored as 0. all
         * other negative values will result in 0.
         */
        public static int LOWER_BOUND = 0;

        /**
         * Ushort maximum allowed value
         */
        public static int USHORT_MAX_VALUE = Short.MAX_VALUE - Short.MIN_VALUE;

        public abstract byte calculate(byte... values);

        public abstract short calculate(short... values);

        public abstract short calculate(boolean isUshort, short... values);

        public abstract int calculate(int... values);

        public abstract float calculate(float... values);

        public abstract double calculate(double... values);

        public abstract long calculateL(long... values);

        public boolean isDataTypeSupported(int dataType) {
            return true;
        }

        public boolean supportsMultipleValues() {
            return supportsMultipleValues;
        }

        public boolean isUshortSupported() {
            return false;
        }

        public double getNullValue() {
            return nullValue;
        }

        public int getType() {
            return type;
        }

    }

    public final static int OPERATION_INDEX = 0;

    public final static int ROI_INDEX = 1;

    public final static int RANGE_INDEX = 2;

    public final static int DEST_NODATA_INDEX = 3;

    /**
     * The resource strings that provide the general documentation and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
            { "GlobalName", "algebric" },
            { "LocalName", "algebric" },
            { "Vendor", "it.geosolutions.jaiext" },
            { "Description",
                    "This class executes the operation selected by the user on each pixel of the source images " },
            { "DocURL", "Not Defined" }, { "Version", "1.0" },
            { "arg0Desc", "Operation to execute" }, { "arg1Desc", "ROI object used" },
            { "arg2Desc", "No Data Range used" }, { "arg3Desc", "Output value for No Data" }, };

    /**
     * Input Parameter name
     */
    private static final String[] paramNames = { "operation", "roi", "noData", "destinationNoData" };

    /**
     * Input Parameter class
     */
    private static final Class[] paramClasses = { Operator.class, javax.media.jai.ROI.class,
            it.geosolutions.jaiext.range.Range.class, Double.class };

    /**
     * Input Parameter default values
     */
    private static final Object[] paramDefaults = { null, null, null, 0d };

    /** Constructor. */
    public AlgebraDescriptor() {
        super(resources, paramClasses, paramNames, paramDefaults);
    }

    /** Returns <code>true</code> since renderable operation is supported. */
    public boolean isRenderableSupported() {
        return true;
    }

    /**
     * Executes the selected operation on an image array.
     * 
     * <p>
     * Creates a <code>ParameterBlockJAI</code> from all supplied arguments except <code>hints</code> and invokes
     * {@link JAI#create(String,ParameterBlock,RenderingHints)}.
     * 
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderedOp
     * 
     * @param op operation to execute
     * @param roi optional ROI object
     * @param optional nodata range for checking nodata
     * @param destinationNoData value to set for destination NoData
     * @param sources <code>RenderedImage</code> sources.
     * @param hints The <code>RenderingHints</code> to use. May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source</code> are <code>0</code>.
     */
    public static RenderedOp create(Operator op, ROI roi, Range noData, double destinationNoData,
            RenderingHints hints, RenderedImage... sources) {

        ParameterBlockJAI pb = new ParameterBlockJAI("algebric", RenderedRegistryMode.MODE_NAME);

        int numSources = sources.length;

        for (int i = 0; i < numSources; i++) {
            RenderedImage img = sources[i];
            if (img != null) {
                pb.setSource(img, i);
            }
        }

        if (pb.getNumSources() == 0) {
            throw new IllegalArgumentException("The input images are Null");
        }

        pb.setParameter("operation", op);
        pb.setParameter("roi", roi);
        pb.setParameter("noData", noData);
        pb.setParameter("destinationNoData", destinationNoData);

        return JAI.create("algebric", pb, hints);
    }

    /**
     * Executes the selected operation on an image array.
     * 
     * <p>
     * Creates a <code>ParameterBlockJAI</code> from all supplied arguments except <code>hints</code> and invokes
     * {@link JAI#createRenderable(String,ParameterBlock,RenderingHints)}.
     * 
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderableOp
     * 
     * @param op operation to execute
     * @param roi optional ROI object
     * @param optional nodata range for checking nodata
     * @param destinationNoData value to set for destination NoData
     * @param sources <code>RenderableImage</code> sources.
     * @param hints The <code>RenderingHints</code> to use. May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>source</code> are <code>0</code>.
     */
    public static RenderableOp createRenderable(Operator op, ROI roi, Range noData,
            double destinationNoData, RenderingHints hints, RenderableImage... sources) {

        ParameterBlockJAI pb = new ParameterBlockJAI("algebric", RenderableRegistryMode.MODE_NAME);

        int numSources = sources.length;

        for (int i = 0; i < numSources; i++) {
            RenderableImage img = sources[i];
            if (img != null) {
                pb.setSource(img, i);
            }
        }

        if (pb.getNumSources() == 0) {
            throw new IllegalArgumentException("The input images are Null");
        }

        pb.setParameter("operation", op);
        pb.setParameter("roi", roi);
        pb.setParameter("noData", noData);
        pb.setParameter("destinationNoData", destinationNoData);

        return JAI.createRenderable("algebric", pb, hints);
    }
}
