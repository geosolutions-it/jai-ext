package it.geosolutions.jaiext.lookup;

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

import org.jaitools.numeric.Range;

import com.sun.media.jai.util.DataBufferUtils;



public abstract class LookupTable implements Serializable{

    /** The table data. */
    protected transient DataBuffer data;

    /** The band offset values */
    protected int[] tableOffsets;

    protected byte destinationNoDataByte;

    protected short destinationNoDataShort;

    protected int destinationNoDataInt;

    protected float destinationNoDataFloat;

    protected double destinationNoDataDouble;

    protected Range noData;

    protected Rectangle roiBounds;

    protected RandomIter roiIter;

    protected boolean useROIAccessor;

    protected PlanarImage srcROIImage;

    protected boolean hasNoData;

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

        if (!isIntegralDataType(srcSampleModel)) {
            return null; // source has non-integral data type
        }

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

    protected void initOffsets(int nbands, int offset) {
        tableOffsets = new int[nbands];
        for (int i = 0; i < nbands; i++) {
            tableOffsets[i] = offset;
        }
    }

    protected void initOffsets(int nbands, int[] offset) {
        tableOffsets = new int[nbands];
        for (int i = 0; i < nbands; i++) {
            tableOffsets[i] = offset[i];
        }
    }

    public void setDestinationNoData(double destinationNoData) {
        int dataType = getDataType();
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

    public void setNoDataRange(Range noData) {
        this.noData = noData;
        this.hasNoData = true;
    }

    public void setROIparams(Rectangle roiBounds, RandomIter roiIter, PlanarImage srcROIImage,
            boolean useROIAccessor) {
        this.hasROI = true;
        this.roiBounds = roiBounds;
        this.roiIter = roiIter;
        this.useROIAccessor = useROIAccessor;
        this.srcROIImage = srcROIImage;
    }
    
    protected abstract void lookup(Raster source, WritableRaster dst, Rectangle rect, Raster roi);
    
    
}
