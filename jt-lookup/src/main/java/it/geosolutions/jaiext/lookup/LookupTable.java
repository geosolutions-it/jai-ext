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
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.Serializable;

import javax.media.jai.LookupTableJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RandomIter;

/**
 * This abstract class defines the general methods of a LookupTable. This class contains all the table informations used by its direct subclasses for
 * doing the lookup operation. The Constructor methods are called by all the 4 subclasses(one for every integral data type). The set/unsetROI() and
 * set/unsetNoData() methods are used for setting or unsetting the ROI or No Data Range used by this table. ALl the get() methods are support methods
 * used for retrieve table information in a faster way. Lookup(), lookupFloat() and lookupDouble() are 3 methods that return the table data associated
 * with the selected input image. The lase method called lookup(Raster,WritableRaster,Rectangle) is abstract because its implementation depends on the
 * subClass data type.
 */

public abstract class LookupTable extends LookupTableJAI implements Serializable {

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
        super(data);
    }

    /**
     * Constructs a single-banded byte lookup table with an index offset.
     * 
     * @param data The single-banded byte data.
     * @param offset The offset.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(byte[] data, int offset) {
        super(data, offset);
    }

    /**
     * Constructs a multi-banded byte lookup table. The index offset for each band is 0.
     * 
     * @param data The multi-banded byte data in [band][index] format.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTable(byte[][] data) {
        super(data);
    }

    /**
     * Constructs a multi-banded byte lookup table where all bands have the same index offset.
     * 
     * @param data The multi-banded byte data in [band][index] format.
     * @param offset The common offset for all bands.
     * @throws IllegalArgumentException if data is null.
     */
    public LookupTable(byte[][] data, int offset) {
        super(data, offset);
    }

    /**
     * Constructs a multi-banded byte lookup table where each band has a different index offset.
     * 
     * @param data The multi-banded byte data in [band][index] format.
     * @param offsets The offsets for the bands.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(byte[][] data, int[] offsets) {
        super(data, offsets);
    }

    /**
     * Constructs a single-banded short or unsigned short lookup table. The index offset is 0.
     * 
     * @param data The single-banded short data.
     * @param isUShort True if data type is DataBuffer.TYPE_USHORT; false if data type is DataBuffer.TYPE_SHORT.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(short[] data, boolean isUShort) {
        super(data, isUShort);
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
        super(data, offset, isUShort);
    }

    /**
     * Constructs a multi-banded short or unsigned short lookup table. The index offset for each band is 0.
     * 
     * @param data The multi-banded short data in [band][index] format.
     * @param isUShort True if data type is DataBuffer.TYPE_USHORT; false if data type is DataBuffer.TYPE_SHORT.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(short[][] data, boolean isUShort) {
        super(data, isUShort);
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
        super(data, offset, isUShort);
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
        super(data, offsets, isUShort);
    }

    /**
     * Constructs a single-banded int lookup table. The index offset is 0.
     * 
     * @param data The single-banded int data.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(int[] data) {
        super(data);
    }

    /**
     * Constructs a single-banded int lookup table with an index offset.
     * 
     * @param data The single-banded int data.
     * @param offset The offset.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(int[] data, int offset) {
        super(data, offset);
    }

    /**
     * Constructs a multi-banded int lookup table. The index offset for each band is 0.
     * 
     * @param data The multi-banded int data in [band][index] format.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(int[][] data) {
        super(data);
    }

    /**
     * Constructs a multi-banded int lookup table where all bands have the same index offset.
     * 
     * @param data The multi-banded int data in [band][index] format.
     * @param offset The common offset for all bands.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(int[][] data, int offset) {
        super(data, offset);
    }

    /**
     * Constructs a multi-banded int lookup table where each band has a different index offset.
     * 
     * @param data The multi-banded int data in [band][index] format.
     * @param offsets The offsets for the bands.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(int[][] data, int[] offsets) {
        super(data, offsets);
    }

    /**
     * Constructs a single-banded float lookup table. The index offset is 0.
     * 
     * @param data The single-banded float data.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(float[] data) {
        super(data);
    }

    /**
     * Constructs a single-banded float lookup table with an index offset.
     * 
     * @param data The single-banded float data.
     * @param offset The offset.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(float[] data, int offset) {
        super(data, offset);
    }

    /**
     * Constructs a multi-banded float lookup table. The index offset for each band is 0.
     * 
     * @param data The multi-banded float data in [band][index] format.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(float[][] data) {
        super(data);
    }

    /**
     * Constructs a multi-banded float lookup table where all bands have the same index offset.
     * 
     * @param data The multi-banded float data in [band][index] format.
     * @param offset The common offset for all bands.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(float[][] data, int offset) {
        super(data, offset);
    }

    /**
     * Constructs a multi-banded float lookup table where each band has a different index offset.
     * 
     * @param data The multi-banded float data in [band][index] format.
     * @param offsets The offsets for the bands.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(float[][] data, int[] offsets) {
        super(data, offsets);
    }

    /**
     * Constructs a single-banded double lookup table. The index offset is 0.
     * 
     * @param data The single-banded double data.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(double[] data) {
        super(data);
    }

    /**
     * Constructs a single-banded double lookup table with an index offset.
     * 
     * @param data The single-banded double data.
     * @param offset The offset.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(double[] data, int offset) {
        super(data, offset);
    }

    /**
     * Constructs a multi-banded double lookup table. The index offset for each band is 0.
     * 
     * @param data The multi-banded double data in [band][index] format.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(double[][] data) {
        super(data);
    }

    /**
     * Constructs a multi-banded double lookup table where all bands have the same index offset.
     * 
     * @param data The multi-banded double data in [band][index] format.
     * @param offset The common offset for all bands.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(double[][] data, int offset) {
        super(data, offset);
    }

    /**
     * Constructs a multi-banded double lookup table where each band has a different index offset.
     * 
     * @param data The multi-banded double data in [band][index] format.
     * @param offsets The offsets for the bands.
     * @throws IllegalArgumentException if data is null.
     */
    protected LookupTable(double[][] data, int[] offsets) {
        super(data, offsets);
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
