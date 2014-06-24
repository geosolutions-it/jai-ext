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

import it.geosolutions.jaiext.range.Range;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.Serializable;

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.iterator.RandomIter;

import com.sun.media.jai.util.DataBufferUtils;

/**
 * This abstract class defines the general methods of a LookupTable. This class contains all the table informations used by its direct subclasses for
 * doing the lookup operation. The Constructor methods are called by all the 4 subclasses(one for every integral data type). The set/unsetROI() and
 * set/unsetNoData() methods are used for setting or unsetting the ROI or No Data Range used by this table. ALl the get() methods are support methods
 * used for retrieve table information in a faster way. Lookup(), lookupFloat() and lookupDouble() are 3 methods that return the table data associated
 * with the selected input image. The lase method called lookup(Raster,WritableRaster,Rectangle) is abstract because its implementation depends on the
 * subClass data type.
 */

public abstract class LookupTable implements Serializable {

    /** The table data. */
    protected transient DataBuffer data;

    /** The band offset values */
    protected int[] tableOffsets;

    /** Destination no data for Byte images */
    protected byte destinationNoDataByte;

    /** Destination no data for Short/Ushort images */
    protected short destinationNoDataShort;

    /** Destination no data for Integer images */
    protected int destinationNoDataInt;

    /** Destination no data for Float images */
    protected float destinationNoDataFloat;

    /** Destination no data for Double images */
    protected double destinationNoDataDouble;

    /** Range object containing no data values */
    protected Range noData;

    /** Rectangle containing roi bounds */
    protected Rectangle roiBounds;

    /** Iterator used for iterating on the roi data */
    protected RandomIter roiIter;

    /** Boolean indicating if Roi RasterAccessor must be used */
    protected boolean useROIAccessor;

    /** ROI image */
    protected PlanarImage srcROIImage;

    /** Boolean indicating if the image contains No Data values */
    protected boolean hasNoData;

    /** Boolean indicating if the image contains a ROI */
    protected boolean hasROI;

    /**
     * Constructs a single-banded byte lookup table. The index offset is 0.
     * 
     * @param data The single-banded byte data.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }

        this.data = new DataBufferByte(data, data.length);
        this.initOffsets(1, 0);
    }

    /**
     * Constructs a single-banded byte lookup table with an index offset.
     * 
     * @param data The single-banded byte data.
     * @param offset The offset.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(byte[] data, int offset) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }

        this.initOffsets(1, offset);
        this.data = new DataBufferByte(data, data.length);
    }

    /**
     * Constructs a multi-banded byte lookup table. The index offset for each band is 0.
     * 
     * @param data The multi-banded byte data in [band][index] format.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTable(byte[][] data) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }

        this.initOffsets(data.length, 0);
        this.data = new DataBufferByte(data, data[0].length);
    }

    /**
     * Constructs a multi-banded byte lookup table where all bands have the same index offset.
     * 
     * @param data The multi-banded byte data in [band][index] format.
     * @param offset The common offset for all bands.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTable(byte[][] data, int offset) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }

        this.initOffsets(data.length, offset);
        this.data = new DataBufferByte(data, data[0].length);
    }

    /**
     * Constructs a multi-banded byte lookup table where each band has a different index offset.
     * 
     * @param data The multi-banded byte data in [band][index] format.
     * @param offsets The offsets for the bands.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(byte[][] data, int[] offsets) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }

        this.initOffsets(data.length, offsets);
        this.data = new DataBufferByte(data, data[0].length);
    }

    /**
     * Constructs a single-banded short or unsigned short lookup table. The index offset is 0.
     * 
     * @param data The single-banded short data.
     * @param isUShort True if data type is DataBuffer.TYPE_USHORT; false if data type is DataBuffer.TYPE_SHORT.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(short[] data, boolean isUShort) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }

        this.initOffsets(1, 0);
        if (isUShort) {
            this.data = new DataBufferUShort(data, data.length);
        } else {
            this.data = new DataBufferShort(data, data.length);
        }
    }

    /**
     * Constructs a single-banded short or unsigned short lookup table with an index offset.
     * 
     * @param data The single-banded short data.
     * @param offset The offset.
     * @param isUShort True if data type is DataBuffer.TYPE_USHORT; false if data type is DataBuffer.TYPE_SHORT.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(short[] data, int offset, boolean isUShort) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }

        this.initOffsets(1, offset);
        if (isUShort) {
            this.data = new DataBufferUShort(data, data.length);
        } else {
            this.data = new DataBufferShort(data, data.length);
        }
    }

    /**
     * Constructs a multi-banded short or unsigned short lookup table. The index offset for each band is 0.
     * 
     * @param data The multi-banded short data in [band][index] format.
     * @param isUShort True if data type is DataBuffer.TYPE_USHORT; false if data type is DataBuffer.TYPE_SHORT.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(short[][] data, boolean isUShort) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }

        this.initOffsets(data.length, 0);
        if (isUShort) {
            this.data = new DataBufferUShort(data, data[0].length);
        } else {
            this.data = new DataBufferShort(data, data[0].length);
        }
    }

    /**
     * Constructs a multi-banded short or unsigned short lookup table where all bands have the same index offset.
     * 
     * @param data The multi-banded short data in [band][index] format.
     * @param offset The common offset for all bands.
     * @param isUShort True if data type is DataBuffer.TYPE_USHORT; false if data type is DataBuffer.TYPE_SHORT.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(short[][] data, int offset, boolean isUShort) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }

        this.initOffsets(data.length, offset);
        if (isUShort) {
            this.data = new DataBufferUShort(data, data[0].length);
        } else {
            this.data = new DataBufferShort(data, data[0].length);
        }
    }

    /**
     * Constructs a multi-banded short or unsigned short lookup table where each band has a different index offset.
     * 
     * @param data The multi-banded short data in [band][index] format.
     * @param offsets The offsets for the bands.
     * @param isUShort True if data type is DataBuffer.TYPE_USHORT; false if data type is DataBuffer.TYPE_SHORT.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(short[][] data, int[] offsets, boolean isUShort) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }

        this.initOffsets(data.length, offsets);

        if (isUShort) {
            this.data = new DataBufferUShort(data, data[0].length);
        } else {
            this.data = new DataBufferShort(data, data[0].length);
        }
    }

    /**
     * Constructs a single-banded int lookup table. The index offset is 0.
     * 
     * @param data The single-banded int data.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(int[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }

        this.initOffsets(1, 0);
        this.data = new DataBufferInt(data, data.length);
    }

    /**
     * Constructs a single-banded int lookup table with an index offset.
     * 
     * @param data The single-banded int data.
     * @param offset The offset.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(int[] data, int offset) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }

        this.initOffsets(1, offset);
        this.data = new DataBufferInt(data, data.length);
    }

    /**
     * Constructs a multi-banded int lookup table. The index offset for each band is 0.
     * 
     * @param data The multi-banded int data in [band][index] format.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(int[][] data) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }

        this.initOffsets(data.length, 0);
        this.data = new DataBufferInt(data, data[0].length);
    }

    /**
     * Constructs a multi-banded int lookup table where all bands have the same index offset.
     * 
     * @param data The multi-banded int data in [band][index] format.
     * @param offset The common offset for all bands.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(int[][] data, int offset) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }

        this.initOffsets(data.length, offset);
        this.data = new DataBufferInt(data, data[0].length);
    }

    /**
     * Constructs a multi-banded int lookup table where each band has a different index offset.
     * 
     * @param data The multi-banded int data in [band][index] format.
     * @param offsets The offsets for the bands.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(int[][] data, int[] offsets) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }

        this.initOffsets(data.length, offsets);
        this.data = new DataBufferInt(data, data[0].length);
    }

    /**
     * Constructs a single-banded float lookup table. The index offset is 0.
     * 
     * @param data The single-banded float data.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(float[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }

        this.initOffsets(1, 0);
        this.data = DataBufferUtils.createDataBufferFloat(data, data.length);
    }

    /**
     * Constructs a single-banded float lookup table with an index offset.
     * 
     * @param data The single-banded float data.
     * @param offset The offset.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(float[] data, int offset) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }

        this.initOffsets(1, offset);
        this.data = DataBufferUtils.createDataBufferFloat(data, data.length);
    }

    /**
     * Constructs a multi-banded float lookup table. The index offset for each band is 0.
     * 
     * @param data The multi-banded float data in [band][index] format.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(float[][] data) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }

        this.initOffsets(data.length, 0);
        this.data = DataBufferUtils.createDataBufferFloat(data, data[0].length);
    }

    /**
     * Constructs a multi-banded float lookup table where all bands have the same index offset.
     * 
     * @param data The multi-banded float data in [band][index] format.
     * @param offset The common offset for all bands.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(float[][] data, int offset) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }

        this.initOffsets(data.length, offset);
        this.data = DataBufferUtils.createDataBufferFloat(data, data[0].length);
    }

    /**
     * Constructs a multi-banded float lookup table where each band has a different index offset.
     * 
     * @param data The multi-banded float data in [band][index] format.
     * @param offsets The offsets for the bands.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(float[][] data, int[] offsets) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }

        this.initOffsets(data.length, offsets);
        this.data = DataBufferUtils.createDataBufferFloat(data, data[0].length);
    }

    /**
     * Constructs a single-banded double lookup table. The index offset is 0.
     * 
     * @param data The single-banded double data.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(double[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }

        this.initOffsets(1, 0);
        this.data = DataBufferUtils.createDataBufferDouble(data, data.length);
    }

    /**
     * Constructs a single-banded double lookup table with an index offset.
     * 
     * @param data The single-banded double data.
     * @param offset The offset.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(double[] data, int offset) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }

        this.initOffsets(1, offset);
        this.data = DataBufferUtils.createDataBufferDouble(data, data.length);
    }

    /**
     * Constructs a multi-banded double lookup table. The index offset for each band is 0.
     * 
     * @param data The multi-banded double data in [band][index] format.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(double[][] data) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }

        this.initOffsets(data.length, 0);
        this.data = DataBufferUtils.createDataBufferDouble(data, data[0].length);
    }

    /**
     * Constructs a multi-banded double lookup table where all bands have the same index offset.
     * 
     * @param data The multi-banded double data in [band][index] format.
     * @param offset The common offset for all bands.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(double[][] data, int offset) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }

        this.initOffsets(data.length, offset);
        this.data = DataBufferUtils.createDataBufferDouble(data, data[0].length);
    }

    /**
     * Constructs a multi-banded double lookup table where each band has a different index offset.
     * 
     * @param data The multi-banded double data in [band][index] format.
     * @param offsets The offsets for the bands.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(double[][] data, int[] offsets) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }

        this.initOffsets(data.length, offsets);
        this.data = DataBufferUtils.createDataBufferDouble(data, data[0].length);
    }

    /**
     * Returns the table data as a DataBuffer.
     */
    public DataBuffer getData() {
        return data;
    }

    /**
     * Returns the byte table data in array format, or null if the table's data type is not byte.
     */
    public byte[][] getByteData() {
        return data instanceof DataBufferByte ? ((DataBufferByte) data).getBankData() : null;
    }

    /**
     * Returns the byte table data of a specific band in array format, or null if the table's data type is not byte.
     */
    public byte[] getByteData(int band) {
        return data instanceof DataBufferByte ? ((DataBufferByte) data).getData(band) : null;
    }

    /**
     * Returns the short table data in array format, or null if the table's data type is not short. This includes both signed and unsigned short table
     * data.
     * 
     */
    public short[][] getShortData() {
        if (data instanceof DataBufferUShort) {
            return ((DataBufferUShort) data).getBankData();
        } else if (data instanceof DataBufferShort) {
            return ((DataBufferShort) data).getBankData();
        } else {
            return null;
        }
    }

    /**
     * Returns the short table data of a specific band in array format, or null if the table's data type is not short.
     * 
     */
    public short[] getShortData(int band) {
        if (data instanceof DataBufferUShort) {
            return ((DataBufferUShort) data).getData(band);
        } else if (data instanceof DataBufferShort) {
            return ((DataBufferShort) data).getData(band);
        } else {
            return null;
        }
    }

    /**
     * Returns the integer table data in array format, or null if the table's data type is not int.
     * 
     */
    public int[][] getIntData() {
        return data instanceof DataBufferInt ? ((DataBufferInt) data).getBankData() : null;
    }

    /**
     * Returns the integer table data of a specific band in array format, or null if table's data type is not int.
     * 
     */
    public int[] getIntData(int band) {
        return data instanceof DataBufferInt ? ((DataBufferInt) data).getData(band) : null;
    }

    /**
     * Returns the float table data in array format, or null if the table's data type is not float.
     * 
     */
    public float[][] getFloatData() {
        return data.getDataType() == DataBuffer.TYPE_FLOAT ? DataBufferUtils.getBankDataFloat(data)
                : null;
    }

    /**
     * Returns the float table data of a specific band in array format, or null if table's data type is not float.
     * 
     */
    public float[] getFloatData(int band) {
        return data.getDataType() == DataBuffer.TYPE_FLOAT ? DataBufferUtils.getDataFloat(data,
                band) : null;
    }

    /**
     * Returns the double table data in array format, or null if the table's data type is not double.
     * 
     */
    public double[][] getDoubleData() {
        return data.getDataType() == DataBuffer.TYPE_DOUBLE ? DataBufferUtils
                .getBankDataDouble(data) : null;
    }

    /**
     * Returns the double table data of a specific band in array format, or null if table's data type is not double.
     * 
     */
    public double[] getDoubleData(int band) {
        return data.getDataType() == DataBuffer.TYPE_DOUBLE ? DataBufferUtils.getDataDouble(data,
                band) : null;
    }

    /** Returns the index offsets of entry 0 for all bands. */
    public int[] getOffsets() {
        return tableOffsets;
    }

    /**
     * Returns the index offset of entry 0 for the default band.
     * 
     */
    public int getOffset() {
        return tableOffsets[0];
    }

    /**
     * Returns the index offset of entry 0 for a specific band.
     * 
     */
    public int getOffset(int band) {
        return tableOffsets[band];
    }

    /** Returns the number of bands of the table. */
    public int getNumBands() {
        return data.getNumBanks();
    }

    /**
     * Returns the number of entries per band of the table.
     * 
     */
    public int getNumEntries() {
        return data.getSize();
    }

    /**
     * Returns the data type of the table data.
     * 
     */
    public int getDataType() {
        return data.getDataType();
    }

    /**
     * Returns the number of bands of the destination image, based on the number of bands of the source image and lookup table.
     * 
     * @param srcNumBands The number of bands of the source image.
     * @return the number of bands in destination image.
     */
    public int getDestNumBands(int srcNumBands) {
        int tblNumBands = getNumBands();
        return srcNumBands == 1 ? tblNumBands : srcNumBands;
    }

    /**
     * Returns a <code>SampleModel</code> suitable for holding the output of a lookup operation on the source data described by a given SampleModel
     * with this table. The width and height of the destination SampleModel are the same as that of the source. This method will return null if the
     * source SampleModel has a non-integral data type.
     * 
     * @param srcSampleModel The SampleModel of the source image.
     * 
     * @throws IllegalArgumentException if srcSampleModel is null.
     * @return sampleModel suitable for the destination image.
     */
    public SampleModel getDestSampleModel(SampleModel srcSampleModel) {
        if (srcSampleModel == null) {
            throw new IllegalArgumentException("Source SampleModel must be not null");
        }

        return getDestSampleModel(srcSampleModel, srcSampleModel.getWidth(),
                srcSampleModel.getHeight());
    }

    /**
     * Returns a <code>SampleModel</code> suitable for holding the output of a lookup operation on the source data described by a given SampleModel
     * with this table. This method will return null if the source SampleModel has a non-integral data type.
     * 
     * @param srcSampleModel The SampleModel of the source image.
     * @param width The width of the destination SampleModel.
     * @param height The height of the destination SampleModel.
     * 
     * @throws IllegalArgumentException if srcSampleModel is null.
     * @return sampleModel suitable for the destination image.
     */
    public SampleModel getDestSampleModel(SampleModel srcSampleModel, int width, int height) {
        if (srcSampleModel == null) {
            throw new IllegalArgumentException("Source SampleModel must be not null");
        }
        // Control if the source has non-integral data type
        if (!isIntegralDataType(srcSampleModel)) {
            return null;
        }
        // If the sample model is present, then a new component sample model is created
        return RasterFactory.createComponentSampleModel(srcSampleModel, getDataType(), width,
                height, getDestNumBands(srcSampleModel.getNumBands()));
    }

    /**
     * Validates data type. Returns true if it's one of the integral data types; false otherwise.
     * 
     * @throws IllegalArgumentException if sampleModel is null.
     */
    public boolean isIntegralDataType(SampleModel sampleModel) {
        if (sampleModel == null) {
            throw new IllegalArgumentException("SampleModel must be not null");
        }

        return isIntegralDataType(sampleModel.getTransferType());
    }

    /**
     * Returns <code>true</code> if the specified data type is an integral data type, such as byte, ushort, short, or int.
     */
    public boolean isIntegralDataType(int dataType) {
        if ((dataType == DataBuffer.TYPE_BYTE) || (dataType == DataBuffer.TYPE_USHORT)
                || (dataType == DataBuffer.TYPE_SHORT) || (dataType == DataBuffer.TYPE_INT)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Performs lookup on a given value belonging to a given source band, and returns the result as an int. NoData Range or ROI are not considered.
     * 
     * @param band The source band the value is from.
     * @param value The source value to be placed through the lookup table.
     */
    public int lookup(int band, int value) {
        return data.getElem(band, value - tableOffsets[band]);
    }

    /**
     * Performs lookup on a given value belonging to a given source band, and returns the result as a float. NoData Range or ROI are not considered.
     * 
     * @param band The source band the value is from.
     * @param value The source value to be placed through the lookup table.
     */
    public float lookupFloat(int band, int value) {
        return data.getElemFloat(band, value - tableOffsets[band]);
    }

    /**
     * Performs lookup on a given value belonging to a given source band, and returns the result as a double. NoData Range or ROI are not considered.
     * 
     * @param band The source band the value is from.
     * @param value The source value to be placed through the lookup table.
     */
    public double lookupDouble(int band, int value) {
        return data.getElemDouble(band, value - tableOffsets[band]);
    }

    /** This method sets the same table offset for all the bands */
    protected void initOffsets(int nbands, int offset) {
        tableOffsets = new int[nbands];
        for (int i = 0; i < nbands; i++) {
            tableOffsets[i] = offset;
        }
    }

    /** This method sets the table offset related to every band */
    protected void initOffsets(int nbands, int[] offset) {
        tableOffsets = new int[nbands];
        for (int i = 0; i < nbands; i++) {
            tableOffsets[i] = offset[i];
        }
    }

    /** This method sets destination no data used for No Data or ROI calculation */
    public void setDestinationNoData(double destinationNoData) {               
        // Selection of the table data type
        int dataType = getDataType();
        // Cast of the initial double value to that of the data type
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            destinationNoDataByte = (byte) ((byte) destinationNoData & 0xff);
            break;
        case DataBuffer.TYPE_USHORT:
            destinationNoDataShort = (short) ((short) destinationNoData & 0xffff);
            break;
        case DataBuffer.TYPE_SHORT:
            destinationNoDataShort = (short) destinationNoData;
            break;
        case DataBuffer.TYPE_INT:
            destinationNoDataInt = (int) destinationNoData;
            break;
        case DataBuffer.TYPE_FLOAT:
            destinationNoDataFloat = (float) destinationNoData;
            break;
        case DataBuffer.TYPE_DOUBLE:
            destinationNoDataDouble = destinationNoData;
            break;
        default:
            throw new IllegalArgumentException("Wrong data type");
        }
    }

    /** No Data flag is set to true and no data range is taken */
    public void setNoDataRange(Range noData) {
        this.noData = noData;
        this.hasNoData = true;
    }

    /** No Data flag is set to false and no data range is set to null */
    public void unsetNoData() {
        this.noData = null;
        this.hasNoData = false;
    }

    /** ROI flag is set to true and the ROI fields are all filled */
    public void setROIparams(Rectangle roiBounds, RandomIter roiIter, PlanarImage srcROIImage,
            boolean useROIAccessor) {
        this.hasROI = true;
        this.roiBounds = roiBounds;
        this.roiIter = roiIter;
        this.useROIAccessor = useROIAccessor;
        this.srcROIImage = srcROIImage;
    }

    /** ROI flag is set to flag and the ROI fields are all left empty */
    public void unsetROI() {
        this.hasROI = false;
        this.roiBounds = null;
        this.roiIter = null;
        this.srcROIImage = null;
        this.useROIAccessor = false;
    }

    /** Abstract method for calculating the destination tile from the source tile and an eventual ROI raster */
    protected abstract void lookup(Raster source, WritableRaster dst, Rectangle rect, Raster roi);

}
