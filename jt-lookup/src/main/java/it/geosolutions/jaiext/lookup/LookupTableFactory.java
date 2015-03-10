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
package it.geosolutions.jaiext.lookup;

import java.awt.image.DataBuffer;

import javax.media.jai.LookupTableJAI;

public class LookupTableFactory {
    /**
     * Constructs a single-banded byte lookup table. The index offset is 0.
     * 
     * @param data The single-banded byte data.
     * @throws IllegalArgumentException if data is null.
     */
    public static LookupTable create(byte[] data) {
        return new LookupTable(data);
    }

    /**
     * Constructs a single-banded byte lookup table with an index offset.
     * 
     * @param data The single-banded byte data.
     * @param offset The offset.
     * @throws IllegalArgumentException if data is null.
     */
    public static LookupTable create(byte[] data, int offset) {
        return new LookupTable(data, offset);
    }

    /**
     * Constructs a multi-banded byte lookup table. The index offset for each band is 0.
     * 
     * @param data The multi-banded byte data in [band][index] format.
     * @throws IllegalArgumentException if data is null.
     */
    public static LookupTable create(byte[][] data) {
        return new LookupTable(data);
    }

    /**
     * Constructs a multi-banded byte lookup table where all bands have the same index offset.
     * 
     * @param data The multi-banded byte data in [band][index] format.
     * @param offset The common offset for all bands.
     * @throws IllegalArgumentException if data is null.
     */
    public static LookupTable create(byte[][] data, int offset) {
        return new LookupTable(data, offset);
    }

    /**
     * Constructs a multi-banded byte lookup table where each band has a different index offset.
     * 
     * @param data The multi-banded byte data in [band][index] format.
     * @param offsets The offsets for the bands.
     * @throws IllegalArgumentException if data is null.
     */
    public static LookupTable create(byte[][] data, int[] offsets) {
        return new LookupTable(data, offsets);
    }

    /**
     * Constructs a single-banded short or unsigned short lookup table. The index offset is 0.
     * 
     * @param data The single-banded short data.
     * @param isUShort True if data type is DataBuffer.TYPE_USHORT; false if data type is DataBuffer.TYPE_SHORT.
     * @throws IllegalArgumentException if data is null.
     */
    public static LookupTable create(short[] data, boolean isUShort) {
        return new LookupTable(data, isUShort);
    }

    /**
     * Constructs a single-banded short or unsigned short lookup table with an index offset.
     * 
     * @param data The single-banded short data.
     * @param offset The offset.
     * @param isUShort True if data type is DataBuffer.TYPE_USHORT; false if data type is DataBuffer.TYPE_SHORT.
     * @throws IllegalArgumentException if data is null.
     */
    public static LookupTable create(short[] data, int offset, boolean isUShort) {
        return new LookupTable(data, offset, isUShort);
    }

    /**
     * Constructs a multi-banded short or unsigned short lookup table. The index offset for each band is 0.
     * 
     * @param data The multi-banded short data in [band][index] format.
     * @param isUShort True if data type is DataBuffer.TYPE_USHORT; false if data type is DataBuffer.TYPE_SHORT.
     * @throws IllegalArgumentException if data is null.
     */
    public static LookupTable create(short[][] data, boolean isUShort) {
        return new LookupTable(data, isUShort);
    }

    /**
     * Constructs a multi-banded short or unsigned short lookup table where all bands have the same index offset.
     * 
     * @param data The multi-banded short data in [band][index] format.
     * @param offset The common offset for all bands.
     * @param isUShort True if data type is DataBuffer.TYPE_USHORT; false if data type is DataBuffer.TYPE_SHORT.
     * @throws IllegalArgumentException if data is null.
     */
    public static LookupTable create(short[][] data, int offset, boolean isUShort) {
        return new LookupTable(data, offset, isUShort);
    }

    /**
     * Constructs a multi-banded short or unsigned short lookup table where each band has a different index offset.
     * 
     * @param data The multi-banded short data in [band][index] format.
     * @param offsets The offsets for the bands.
     * @param isUShort True if data type is DataBuffer.TYPE_USHORT; false if data type is DataBuffer.TYPE_SHORT.
     * @throws IllegalArgumentException if data is null.
     */
    public static LookupTable create(short[][] data, int[] offsets, boolean isUShort) {
        return new LookupTable(data, offsets, isUShort);
    }

    /**
     * Constructs a single-banded int lookup table. The index offset is 0.
     * 
     * @param data The single-banded int data.
     * @throws IllegalArgumentException if data is null.
     */
    public static LookupTable create(int[] data) {
        return new LookupTable(data);
    }

    /**
     * Constructs a single-banded int lookup table with an index offset.
     * 
     * @param data The single-banded int data.
     * @param offset The offset.
     * @throws IllegalArgumentException if data is null.
     */
    public static LookupTable create(int[] data, int offset) {
        return new LookupTable(data, offset);
    }

    /**
     * Constructs a multi-banded int lookup table. The index offset for each band is 0.
     * 
     * @param data The multi-banded int data in [band][index] format.
     * @throws IllegalArgumentException if data is null.
     */
    public static LookupTable create(int[][] data) {
        return new LookupTable(data);
    }

    /**
     * Constructs a multi-banded int lookup table where all bands have the same index offset.
     * 
     * @param data The multi-banded int data in [band][index] format.
     * @param offset The common offset for all bands.
     * @throws IllegalArgumentException if data is null.
     */
    public static LookupTable create(int[][] data, int offset) {
        return new LookupTable(data, offset);
    }

    /**
     * Constructs a multi-banded int lookup table where each band has a different index offset.
     * 
     * @param data The multi-banded int data in [band][index] format.
     * @param offsets The offsets for the bands.
     * @throws IllegalArgumentException if data is null.
     */
    public static LookupTable create(int[][] data, int[] offsets) {
        return new LookupTable(data, offsets);
    }

    /**
     * Constructs a single-banded float lookup table. The index offset is 0.
     * 
     * @param data The single-banded float data.
     * @throws IllegalArgumentException if data is null.
     */
    public static LookupTable create(float[] data) {
        return new LookupTable(data);
    }

    /**
     * Constructs a single-banded float lookup table with an index offset.
     * 
     * @param data The single-banded float data.
     * @param offset The offset.
     * @throws IllegalArgumentException if data is null.
     */
    public static LookupTable create(float[] data, int offset) {
        return new LookupTable(data, offset);
    }

    /**
     * Constructs a multi-banded float lookup table. The index offset for each band is 0.
     * 
     * @param data The multi-banded float data in [band][index] format.
     * @throws IllegalArgumentException if data is null.
     */
    public static LookupTable create(float[][] data) {
        return new LookupTable(data);
    }

    /**
     * Constructs a multi-banded float lookup table where all bands have the same index offset.
     * 
     * @param data The multi-banded float data in [band][index] format.
     * @param offset The common offset for all bands.
     * @throws IllegalArgumentException if data is null.
     */
    public static LookupTable create(float[][] data, int offset) {
        return new LookupTable(data, offset);
    }

    /**
     * Constructs a multi-banded float lookup table where each band has a different index offset.
     * 
     * @param data The multi-banded float data in [band][index] format.
     * @param offsets The offsets for the bands.
     * @throws IllegalArgumentException if data is null.
     */
    public static LookupTable create(float[][] data, int[] offsets) {
        return new LookupTable(data, offsets);
    }

    /**
     * Constructs a single-banded double lookup table. The index offset is 0.
     * 
     * @param data The single-banded double data.
     * @throws IllegalArgumentException if data is null.
     */
    public static LookupTable create(double[] data) {
        return new LookupTable(data);
    }

    /**
     * Constructs a single-banded double lookup table with an index offset.
     * 
     * @param data The single-banded double data.
     * @param offset The offset.
     * @throws IllegalArgumentException if data is null.
     */
    public static LookupTable create(double[] data, int offset) {
        return new LookupTable(data, offset);
    }

    /**
     * Constructs a multi-banded double lookup table. The index offset for each band is 0.
     * 
     * @param data The multi-banded double data in [band][index] format.
     * @throws IllegalArgumentException if data is null.
     */
    public static LookupTable create(double[][] data) {
        return new LookupTable(data);
    }

    /**
     * Constructs a multi-banded double lookup table where all bands have the same index offset.
     * 
     * @param data The multi-banded double data in [band][index] format.
     * @param offset The common offset for all bands.
     * @throws IllegalArgumentException if data is null.
     */
    public static LookupTable create(double[][] data, int offset) {
        return new LookupTable(data, offset);
    }

    /**
     * Constructs a multi-banded double lookup table where each band has a different index offset.
     * 
     * @param data The multi-banded double data in [band][index] format.
     * @param offsets The offsets for the bands.
     * @throws IllegalArgumentException if data is null.
     */
    public static LookupTable create(double[][] data, int[] offsets) {
        return new LookupTable(data, offsets);
    }
    
    /**
     * Constructs a multi-banded lookup table from another one.
     * 
     * @param table The multi-banded lookupTable.
     * @throws IllegalArgumentException if data is null.
     */
    public static LookupTable create(LookupTableJAI table) {
        int tableDataType = table.getDataType();
        int[] offsets = table.getOffsets();
            switch (tableDataType) {
            case DataBuffer.TYPE_BYTE:
                return new LookupTable(table.getByteData(), offsets);
            case DataBuffer.TYPE_USHORT:
                return new LookupTable(table.getShortData(), offsets, true);
            case DataBuffer.TYPE_SHORT:
                return new LookupTable(table.getShortData(), offsets, false);
            case DataBuffer.TYPE_INT:
                return new LookupTable(table.getByteData(), offsets);
            case DataBuffer.TYPE_FLOAT:
                return new LookupTable(table.getFloatData(), offsets);
            case DataBuffer.TYPE_DOUBLE:
                return new LookupTable(table.getDoubleData(), offsets);
            default:
                throw new IllegalArgumentException("Wrong image dataType");
            }
    }
}
