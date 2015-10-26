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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.Serializable;

import javax.media.jai.LookupTableJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFactory;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.iterator.RandomIter;

import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;

/**
 * This abstract class defines the general methods of a LookupTable. This class contains all the table informations used by its direct subclasses for
 * doing the lookup operation. The Constructor methods are called by all the 4 subclasses(one for every integral data type). The set/unsetROI() and
 * set/unsetNoData() methods are used for setting or unsetting the ROI or No Data Range used by this table. ALl the get() methods are support methods
 * used for retrieve table information in a faster way. Lookup(), lookupFloat() and lookupDouble() are 3 methods that return the table data associated
 * with the selected input image. The lase method called lookup(Raster,WritableRaster,Rectangle) is abstract because its implementation depends on the
 * subClass data type.
 */

public class LookupTable extends LookupTableJAI implements Serializable {

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
        // Cast of the initial double value to that of the data type
            destinationNoDataByte = (byte) ((byte) destinationNoData & 0xff);
            destinationNoDataShort = (short) ((short) destinationNoData & 0xffff);
            destinationNoDataShort = (short) destinationNoData;
            destinationNoDataInt = (int) destinationNoData;
            destinationNoDataFloat = (float) destinationNoData;
            destinationNoDataDouble = destinationNoData;
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
    public void setROIparams(Rectangle roiBounds, PlanarImage srcROIImage,
            boolean useROIAccessor) {
        this.hasROI = true;
        this.roiBounds = roiBounds;
        this.useROIAccessor = useROIAccessor;
        this.srcROIImage = srcROIImage;
    }

    /** ROI flag is set to flag and the ROI fields are all left empty */
    public void unsetROI() {
        this.hasROI = false;
        this.roiBounds = null;
        this.srcROIImage = null;
        this.useROIAccessor = false;
    }

    /** Abstract method for calculating the destination tile from the source tile and an eventual ROI raster */
    protected void lookup(Raster source, WritableRaster dst, Rectangle rect, Raster roi) {
        
        
        // Validate source.
        if (source == null) {
            throw new IllegalArgumentException("Source data must be present");
        }

        // If the image data type is not integral an exception is thrown
        SampleModel srcSampleModel = source.getSampleModel();
        if (!isIntegralDataType(srcSampleModel)) {
            throw new IllegalArgumentException("Only integral data type are handled");
        }

        // Validate rectangle.
        if (rect == null) {
            rect = source.getBounds();
        } else {
            rect = rect.intersection(source.getBounds());
        }

        if (dst != null) {
            rect = rect.intersection(dst.getBounds());
        }

        // Validate destination.
        SampleModel dstSampleModel;
        if (dst == null) { // create dst according to table
            dstSampleModel = getDestSampleModel(srcSampleModel, rect.width, rect.height);
            dst = RasterFactory.createWritableRaster(dstSampleModel, new Point(rect.x, rect.y));
        } else {
            dstSampleModel = dst.getSampleModel();

            if (dstSampleModel.getTransferType() != getDataType()
                    || dstSampleModel.getNumBands() != getDestNumBands(srcSampleModel.getNumBands())) {
                throw new IllegalArgumentException(
                        "Destination image must have the same data type and band number of the Table");
            }
        }

        // Add bit support?
        int sTagID = RasterAccessor.findCompatibleTag(null, srcSampleModel);
        int dTagID = RasterAccessor.findCompatibleTag(null, dstSampleModel);

        RasterFormatTag sTag = new RasterFormatTag(srcSampleModel,sTagID);
        RasterFormatTag dTag = new RasterFormatTag(dstSampleModel,dTagID);

        RasterAccessor s = new RasterAccessor(source, rect, sTag, null);
        RasterAccessor d = new RasterAccessor(dst, rect, dTag, null);
        
        // Roi rasterAccessor initialization
        RasterAccessor roiAccessor = null;
        RandomIter roiIter = null;
        // ROI calculation only if the roi raster is present
        if (useROIAccessor) {
            // Get the source rectangle
            Rectangle srcRect = source.getBounds();
            // creation of the rasterAccessor
            roiAccessor = new RasterAccessor(roi, srcRect, RasterAccessor.findCompatibleTags(
                    new RenderedImage[] { srcROIImage }, srcROIImage)[0],
                    srcROIImage.getColorModel());
        } else if(hasROI) {
            roiIter = RandomIterFactory.create(srcROIImage, srcROIImage.getBounds(), true, true);
        }

        int srcNumBands = s.getNumBands();
        int srcDataType = s.getDataType();

        int tblNumBands = getNumBands();
        int tblDataType = getDataType();

        int dstWidth = d.getWidth();
        int dstHeight = d.getHeight();
        int dstNumBands = d.getNumBands();
        int dstDataType = d.getDataType();

        // Source information.
        int srcLineStride = s.getScanlineStride();
        int srcPixelStride = s.getPixelStride();
        int[] srcBandOffsets = s.getBandOffsets();

        byte[][] bSrcData = s.getByteDataArrays();
        short[][] sSrcData = s.getShortDataArrays();
        int[][] iSrcData = s.getIntDataArrays();

        if (srcNumBands < dstNumBands) {
            int offset0 = srcBandOffsets[0];
            srcBandOffsets = new int[dstNumBands];
            for (int i = 0; i < dstNumBands; i++) {
                srcBandOffsets[i] = offset0;
            }

            switch (srcDataType) {
            case DataBuffer.TYPE_BYTE:
                byte[] bData0 = bSrcData[0];
                bSrcData = new byte[dstNumBands][];
                for (int i = 0; i < dstNumBands; i++) {
                    bSrcData[i] = bData0;
                }
                break;
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
                short[] sData0 = sSrcData[0];
                sSrcData = new short[dstNumBands][];
                for (int i = 0; i < dstNumBands; i++) {
                    sSrcData[i] = sData0;
                }
                break;
            case DataBuffer.TYPE_INT:
                int[] iData0 = iSrcData[0];
                iSrcData = new int[dstNumBands][];
                for (int i = 0; i < dstNumBands; i++) {
                    iSrcData[i] = iData0;
                }
                break;
            }
        }

        // Table information.
        int[] tblOffsets = getOffsets();

        byte[][] bTblData = getByteData();
        short[][] sTblData = getShortData();
        int[][] iTblData = getIntData();
        float[][] fTblData = getFloatData();
        double[][] dTblData = getDoubleData();

        if (tblNumBands < dstNumBands) {
            int offset0 = tblOffsets[0];
            tblOffsets = new int[dstNumBands];
            for (int i = 0; i < dstNumBands; i++) {
                tblOffsets[i] = offset0;
            }

            switch (tblDataType) {
            case DataBuffer.TYPE_BYTE:
                byte[] bData0 = bTblData[0];
                bTblData = new byte[dstNumBands][];
                for (int i = 0; i < dstNumBands; i++) {
                    bTblData[i] = bData0;
                }
                break;
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
                short[] sData0 = sTblData[0];
                sTblData = new short[dstNumBands][];
                for (int i = 0; i < dstNumBands; i++) {
                    sTblData[i] = sData0;
                }
                break;
            case DataBuffer.TYPE_INT:
                int[] iData0 = iTblData[0];
                iTblData = new int[dstNumBands][];
                for (int i = 0; i < dstNumBands; i++) {
                    iTblData[i] = iData0;
                }
                break;
            case DataBuffer.TYPE_FLOAT:
                float[] fData0 = fTblData[0];
                fTblData = new float[dstNumBands][];
                for (int i = 0; i < dstNumBands; i++) {
                    fTblData[i] = fData0;
                }
                break;
            case DataBuffer.TYPE_DOUBLE:
                double[] dData0 = dTblData[0];
                dTblData = new double[dstNumBands][];
                for (int i = 0; i < dstNumBands; i++) {
                    dTblData[i] = dData0;
                }
            }
        }

        // Destination information.
        int dstLineStride = d.getScanlineStride();
        int dstPixelStride = d.getPixelStride();
        int[] dstBandOffsets = d.getBandOffsets();

        byte[][] bDstData = d.getByteDataArrays();
        short[][] sDstData = d.getShortDataArrays();
        int[][] iDstData = d.getIntDataArrays();
        float[][] fDstData = d.getFloatDataArrays();
        double[][] dDstData = d.getDoubleDataArrays();

        switch (dstDataType) {
        case DataBuffer.TYPE_BYTE:
            switch (srcDataType) {
            case DataBuffer.TYPE_BYTE:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, bSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, bDstData,
                       tblOffsets, bTblData, roiAccessor, roiIter, rect);
                break;

            case DataBuffer.TYPE_USHORT:
                lookupU(srcLineStride, srcPixelStride,
                        srcBandOffsets, sSrcData,
                        dstWidth, dstHeight, dstNumBands,
                        dstLineStride, dstPixelStride,
                        dstBandOffsets, bDstData,
                        tblOffsets, bTblData, roiAccessor, roiIter, rect);
                break;

            case DataBuffer.TYPE_SHORT:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, sSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, bDstData,
                       tblOffsets, bTblData, roiAccessor, roiIter, rect);
                break;

            case DataBuffer.TYPE_INT:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, iSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, bDstData,
                       tblOffsets, bTblData, roiAccessor, roiIter, rect);
                break;
            }
            break;

        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            switch (srcDataType) {
            case DataBuffer.TYPE_BYTE:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, bSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, sDstData,
                       tblOffsets, sTblData, roiAccessor, roiIter, rect);
                break;

            case DataBuffer.TYPE_USHORT:
                lookupU(srcLineStride, srcPixelStride,
                        srcBandOffsets, sSrcData,
                        dstWidth, dstHeight, dstNumBands,
                        dstLineStride, dstPixelStride,
                        dstBandOffsets, sDstData,
                        tblOffsets, sTblData, roiAccessor, roiIter, rect);
                break;

            case DataBuffer.TYPE_SHORT:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, sSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, sDstData,
                       tblOffsets, sTblData, roiAccessor, roiIter, rect);
                break;

            case DataBuffer.TYPE_INT:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, iSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, sDstData,
                       tblOffsets, sTblData, roiAccessor, roiIter, rect);
                break;
            }
            break;

        case DataBuffer.TYPE_INT:
            switch (srcDataType) {
            case DataBuffer.TYPE_BYTE:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, bSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, iDstData,
                       tblOffsets, iTblData, roiAccessor, roiIter, rect);
                break;

            case DataBuffer.TYPE_USHORT:
                lookupU(srcLineStride, srcPixelStride,
                        srcBandOffsets, sSrcData,
                        dstWidth, dstHeight, dstNumBands,
                        dstLineStride, dstPixelStride,
                        dstBandOffsets, iDstData,
                        tblOffsets, iTblData, roiAccessor, roiIter, rect);
                break;

            case DataBuffer.TYPE_SHORT:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, sSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, iDstData,
                       tblOffsets, iTblData, roiAccessor, roiIter, rect);
                break;

            case DataBuffer.TYPE_INT:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, iSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, iDstData,
                       tblOffsets, iTblData, roiAccessor, roiIter, rect);
                break;
            }
            break;

        case DataBuffer.TYPE_FLOAT:
            switch (srcDataType) {
            case DataBuffer.TYPE_BYTE:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, bSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, fDstData,
                       tblOffsets, fTblData, roiAccessor, roiIter, rect);
                break;

            case DataBuffer.TYPE_USHORT:
                lookupU(srcLineStride, srcPixelStride,
                        srcBandOffsets, sSrcData,
                        dstWidth, dstHeight, dstNumBands,
                        dstLineStride, dstPixelStride,
                        dstBandOffsets, fDstData,
                        tblOffsets, fTblData, roiAccessor, roiIter, rect);
                break;

            case DataBuffer.TYPE_SHORT:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, sSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, fDstData,
                       tblOffsets, fTblData, roiAccessor, roiIter, rect);
                break;

            case DataBuffer.TYPE_INT:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, iSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, fDstData,
                       tblOffsets, fTblData, roiAccessor, roiIter, rect);
                break;
            }
            break;

        case DataBuffer.TYPE_DOUBLE:
            switch (srcDataType) {
            case DataBuffer.TYPE_BYTE:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, bSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, dDstData,
                       tblOffsets, dTblData, roiAccessor, roiIter, rect);
                break;

            case DataBuffer.TYPE_USHORT:
                lookupU(srcLineStride, srcPixelStride,
                        srcBandOffsets, sSrcData,
                        dstWidth, dstHeight, dstNumBands,
                        dstLineStride, dstPixelStride,
                        dstBandOffsets, dDstData,
                        tblOffsets, dTblData, roiAccessor, roiIter, rect);
                break;

            case DataBuffer.TYPE_SHORT:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, sSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, dDstData,
                       tblOffsets, dTblData, roiAccessor, roiIter, rect);
                break;

            case DataBuffer.TYPE_INT:
                lookup(srcLineStride, srcPixelStride,
                       srcBandOffsets, iSrcData,
                       dstWidth, dstHeight, dstNumBands,
                       dstLineStride, dstPixelStride,
                       dstBandOffsets, dDstData,
                       tblOffsets, dTblData, roiAccessor, roiIter, rect);
                break;
            }
            break;
        }

        d.copyDataToRaster();

        //return dst;
        
    }


    // byte to byte
    private void lookup(int srcLineStride, int srcPixelStride, int[] srcBandOffsets,
            byte[][] bSrcData, int dstWidth, int dstHeight, int dstNumBands, int dstLineStride,
            int dstPixelStride, int[] dstBandOffsets, byte[][] bDstData, int[] tblOffsets,
            byte[][] bTblData, RasterAccessor roi, RandomIter roiIter, Rectangle destRect) {

        // Destination image bounds
        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        // ROI parameters
        int roiLineStride = 0;
        byte[] roiDataArray = null;
        int roiDataLength = 0;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiLineStride = roi.getScanlineStride();
        }

        // Boolean indicating the possible situations: with or without ROI,
        // with or without No Data, and a special case when table data are not present
        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;

        if (caseA) {

            // Cycle on all the bands
            for (int b = 0; b < dstNumBands; b++) {
                // Selection of the band arrays
                final byte[] s = bSrcData[b];
                final byte[] d = bDstData[b];
                final byte[] t = bTblData[b];
                // Selection of the line offsets
                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                // Cycle on all the y dimension
                for (int h = 0; h < dstHeight; h++) {
                    // Setting of the source and destination pixel offset(is updated for iterating on all the source and destination
                    // array)
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;
                    // Update of the line offsets
                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;
                    // Cycle on all the x dimension
                    for (int w = 0; w < dstWidth; w++) {
                        // Output value is taken from the table array
                        d[dstPixelOffset] = t[(s[srcPixelOffset]&0xFF) - tblOffset];
                        // Update of the source and destination pixel offsets
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else if (caseB) {
            if (useROIAccessor) {
                // Cycle on all the bands
                for (int b = 0; b < dstNumBands; b++) {
                    // Selection of the band arrays
                    byte[] s = bSrcData[b];
                    byte[] d = bDstData[b];
                    byte[] t = bTblData[b];
                    // Selection of the line offsets
                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    // Cycle on all the y dimension
                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        // Setting of the source and destination pixel offset(is updated for iterating on all the source and destination
                        // array)
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;
                        // Update of the line offsets
                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;
                        // Calculation of the y roi position
                        int posyROI = (y - dst_min_y) * roiLineStride;
                        // Cycle on all the x dimension
                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            // Calculation of the x position
                            int posx = (x - dst_min_x) * srcPixelStride;

                            // Calculation of the roi data array index
                            int windex = (posx / dstNumBands) + posyROI;
                            // From the selected index the value is taken
                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                            // If the roi value is 0 the value is outside the ROI, else the table value
                            // is taken
                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataByte;
                            } else {
                                d[dstPixelOffset] = t[(s[srcPixelOffset]&0xFF) - tblOffset];
                            }
                            // Update of the source and destination pixel offsets
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                // Cycle on all the bands
                for (int b = 0; b < dstNumBands; b++) {
                    // Selection of the band arrays
                    byte[] s = bSrcData[b];
                    byte[] d = bDstData[b];
                    byte[] t = bTblData[b];
                    // Selection of the line offsets
                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    // Cycle on all the y dimension
                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        // Setting of the source and destination pixel offset(is updated for iterating on all the source and destination
                        // array)
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;
                        // Update of the line offsets
                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;
                        // Cycle on all the x dimension
                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            // If the sample is inside ROI bounds
                            if (roiBounds.contains(x, y)) {
                                // ROI pixel value is calculated
                                int w = roiIter.getSample(x, y, 0);
                                // if is 0 means that the pixel is outside the ROI, else the table data is taken
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataByte;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset]&0xFF) - tblOffset];
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataByte;
                            }
                            // Update of the source and destination pixel offsets
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            }
        } else if (caseC) {
            // Cycle on all the bands
            for (int b = 0; b < dstNumBands; b++) {
                // Selection of the band arrays
                byte[] s = bSrcData[b];
                byte[] d = bDstData[b];
                byte[] t = bTblData[b];
                // Selection of the line offsets
                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                // Cycle on all the y dimension
                for (int y = 0; y < dstHeight; y++) {
                    // Setting of the source and destination pixel offset(is updated for iterating on all the source and destination
                    // array)
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;
                    // Update of the line offsets
                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;
                    // Cycle on all the x dimension
                    for (int x = 0; x < dstWidth; x++) {
                        // If the value is a not a noData, the table value is stored
                        byte value = (s[srcPixelOffset]);
                        if (noData.contains(value)) {
                            d[dstPixelOffset] = destinationNoDataByte;
                        } else {
                            d[dstPixelOffset] = t[value&0xFF - tblOffset];
                        }
                        // Update of the source and destination pixel offsets
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else {
            if (useROIAccessor) {
                // Cycle on all the bands
                for (int b = 0; b < dstNumBands; b++) {
                    // Selection of the band arrays
                    byte[] s = bSrcData[b];
                    byte[] d = bDstData[b];
                    byte[] t = bTblData[b];
                    // Selection of the line offsets
                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    // Cycle on all the y dimension
                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        // Setting of the source and destination pixel offset(is updated for iterating on all the source and destination
                        // array)
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;
                        // Update of the line offsets
                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;
                        // Calculation of the y roi position
                        int posyROI = (y - dst_min_y) * roiLineStride;
                        // Cycle on all the x dimension
                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            // Calculation of the x position
                            int posx = (x - dst_min_x) * srcPixelStride;
                            // Calculation of the roi data array index
                            int windex = (posx / dstNumBands) + posyROI;
                            // From the selected index the value is taken
                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                            // If the roi value is 0 the value is outside the ROI, else the table value
                            // is taken
                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataByte;
                            } else {
                                // If the value is a not a noData, the table value is stored
                                byte value = (s[srcPixelOffset]);
                                if (noData.contains(value)) {
                                    d[dstPixelOffset] = destinationNoDataByte;
                                } else {
                                    d[dstPixelOffset] = t[value&0xFF - tblOffset];
                                }
                            }
                            // Update of the source and destination pixel offsets
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                // Cycle on all the bands
                for (int b = 0; b < dstNumBands; b++) {
                    // Selection of the band arrays
                    byte[] s = bSrcData[b];
                    byte[] d = bDstData[b];
                    byte[] t = bTblData[b];
                    // Selection of the line offsets
                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];
                    // Cycle on all the y dimension
                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        // Setting of the source and destination pixel offset(is updated for iterating on all the source and destination
                        // array)
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;
                        // Update of the line offsets
                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;
                        // Cycle on all the x dimension
                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            // If the sample is inside ROI bounds
                            if (roiBounds.contains(x, y)) {
                                // ROI pixel value is calculated
                                int w = roiIter.getSample(x, y, 0);
                                // if is 0 means that the pixel is outside the ROI, else the table data is taken
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataByte;
                                } else {
                                    // If the value is a not a noData, the table value is stored
                                    byte value = (s[srcPixelOffset]);
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataByte;
                                    } else {
                                        d[dstPixelOffset] = t[value&0xFF - tblOffset];
                                    }
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataByte;
                            }
                            // Update of the source and destination pixel offsets
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            }
        }
    }

    // byte to ushort/short
    private void lookup(int srcLineStride, int srcPixelStride, int[] srcBandOffsets,
            byte[][] bSrcData, int dstWidth, int dstHeight, int dstNumBands, int dstLineStride,
            int dstPixelStride, int[] dstBandOffsets, short[][] sDstData, int[] tblOffsets,
            short[][] sTblData, RasterAccessor roi, RandomIter roiIter, Rectangle destRect) {

        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        int roiLineStride = 0;
        byte[] roiDataArray = null;
        int roiDataLength = 0;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiLineStride = roi.getScanlineStride();
        }


        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;

        if (caseA) {
            for (int b = 0; b < dstNumBands; b++) {
                final byte[] s = bSrcData[b];
                final short[] d = sDstData[b];
                final short[] t = sTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < dstWidth; w++) {
                        d[dstPixelOffset] = t[(s[srcPixelOffset]&0xFF) - tblOffset];

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else if (caseB) {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    byte[] s = bSrcData[b];
                    short[] d = sDstData[b];
                    short[] t = sTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataShort;
                            } else {
                                d[dstPixelOffset] = t[(s[srcPixelOffset]&0xFF) - tblOffset];
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    byte[] s = bSrcData[b];
                    short[] d = sDstData[b];
                    short[] t = sTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataShort;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset]&0xFF) - tblOffset];
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataShort;
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            }
        } else if (caseC) {
            for (int b = 0; b < dstNumBands; b++) {
                byte[] s = bSrcData[b];
                short[] d = sDstData[b];
                short[] t = sTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int y = 0; y < dstHeight; y++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int x = 0; x < dstWidth; x++) {
                        byte value = (s[srcPixelOffset]);
                        if (noData.contains(value)) {
                            d[dstPixelOffset] = destinationNoDataShort;
                        } else {
                            d[dstPixelOffset] = t[value&0xFF - tblOffset];
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    byte[] s = bSrcData[b];
                    short[] d = sDstData[b];
                    short[] t = sTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataShort;
                            } else {
                                byte value = (s[srcPixelOffset]);
                                if (noData.contains(value)) {
                                    d[dstPixelOffset] = destinationNoDataShort;
                                } else {
                                    d[dstPixelOffset] = t[value&0xFF - tblOffset];
                                }
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    byte[] s = bSrcData[b];
                    short[] d = sDstData[b];
                    short[] t = sTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataShort;
                                } else {
                                    byte value = (s[srcPixelOffset]);
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataShort;
                                    } else {
                                        d[dstPixelOffset] = t[value&0xFF - tblOffset];
                                    }
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataShort;
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            }
        }
    }

    // byte to int
    private void lookup(int srcLineStride, int srcPixelStride, int[] srcBandOffsets,
            byte[][] bSrcData, int dstWidth, int dstHeight, int dstNumBands, int dstLineStride,
            int dstPixelStride, int[] dstBandOffsets, int[][] iDstData, int[] tblOffsets,
            int[][] iTblData, RasterAccessor roi, RandomIter roiIter, Rectangle destRect) {

        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        int roiLineStride = 0;
        byte[] roiDataArray = null;
        int roiDataLength = 0;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiLineStride = roi.getScanlineStride();
        }

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;
        final boolean caseNull = iTblData == null;

        if (caseA) {
            if (caseNull) {
                for (int b = 0; b < dstNumBands; b++) {
                    final byte[] s = bSrcData[b];
                    final int[] d = iDstData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];

                    for (int h = 0; h < dstHeight; h++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int w = 0; w < dstWidth; w++) {
                            d[dstPixelOffset] = getData().getElem(b, (s[srcPixelOffset]&0xFF));

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    final byte[] s = bSrcData[b];
                    final int[] d = iDstData[b];
                    final int[] t = iTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int h = 0; h < dstHeight; h++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int w = 0; w < dstWidth; w++) {
                            d[dstPixelOffset] = t[(s[srcPixelOffset]&0xFF) - tblOffset];

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            }
        } else if (caseB) {
            if (useROIAccessor) {
                if (caseNull) {
                    for (int b = 0; b < dstNumBands; b++) {
                        final byte[] s = bSrcData[b];
                        final int[] d = iDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            int posyROI = (y - dst_min_y) * roiLineStride;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                int posx = (x - dst_min_x) * srcPixelStride;

                                int windex = (posx / dstNumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                } else {
                                    d[dstPixelOffset] = getData().getElem(b, (s[srcPixelOffset]&0xFF));
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                } else {
                    for (int b = 0; b < dstNumBands; b++) {
                        byte[] s = bSrcData[b];
                        int[] d = iDstData[b];
                        int[] t = iTblData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];
                        int tblOffset = tblOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            int posyROI = (y - dst_min_y) * roiLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                int posx = (x - dst_min_x) * srcPixelStride;

                                int windex = (posx / dstNumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset]&0xFF) - tblOffset];
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                }
            } else {
                if (caseNull) {
                    for (int b = 0; b < dstNumBands; b++) {
                        final byte[] s = bSrcData[b];
                        final int[] d = iDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, 0);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        d[dstPixelOffset] = getData().getElem(b,
                                                (s[srcPixelOffset]&0xFF));
                                    }
                                } else {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                } else {
                    for (int b = 0; b < dstNumBands; b++) {
                        byte[] s = bSrcData[b];
                        int[] d = iDstData[b];
                        int[] t = iTblData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];
                        int tblOffset = tblOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {
                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, 0);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        d[dstPixelOffset] = t[(s[srcPixelOffset]&0xFF)
                                                - tblOffset];
                                    }
                                } else {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                }
            }
        } else if (caseC) {
            if (caseNull) {
                for (int b = 0; b < dstNumBands; b++) {
                    final byte[] s = bSrcData[b];
                    final int[] d = iDstData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];

                    for (int y = 0; y < dstHeight; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = 0; x < dstWidth; x++) {

                            byte value = (s[srcPixelOffset]);
                            if (noData.contains(value)) {
                                d[dstPixelOffset] = destinationNoDataInt;
                            } else {
                                d[dstPixelOffset] = getData().getElem(b, (s[srcPixelOffset]&0xFF));
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    byte[] s = bSrcData[b];
                    int[] d = iDstData[b];
                    int[] t = iTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = 0; y < dstHeight; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = 0; x < dstWidth; x++) {
                            byte value = (s[srcPixelOffset]);
                            if (noData.contains(value)) {
                                d[dstPixelOffset] = destinationNoDataInt;
                            } else {
                                d[dstPixelOffset] = t[value&0xFF - tblOffset];
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            }
        } else {
            if (useROIAccessor) {
                if (caseNull) {
                    for (int b = 0; b < dstNumBands; b++) {
                        final byte[] s = bSrcData[b];
                        final int[] d = iDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            int posyROI = (y - dst_min_y) * roiLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                int posx = (x - dst_min_x) * srcPixelStride;

                                int windex = (posx / dstNumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                } else {
                                    byte value = (s[srcPixelOffset]);
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        d[dstPixelOffset] = getData().getElem(b,
                                                (s[srcPixelOffset]&0xFF));
                                    }
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                } else {
                    for (int b = 0; b < dstNumBands; b++) {
                        byte[] s = bSrcData[b];
                        int[] d = iDstData[b];
                        int[] t = iTblData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];
                        int tblOffset = tblOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            int posyROI = (y - dst_min_y) * roiLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                int posx = (x - dst_min_x) * srcPixelStride;

                                int windex = (posx / dstNumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                } else {
                                    byte value = (s[srcPixelOffset]);
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        d[dstPixelOffset] = t[value&0xFF - tblOffset];
                                    }
                                }
                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                }
            } else {
                if (caseNull) {
                    for (int b = 0; b < dstNumBands; b++) {
                        final byte[] s = bSrcData[b];
                        final int[] d = iDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, 0);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        byte value = (s[srcPixelOffset]);
                                        if (noData.contains(value)) {
                                            d[dstPixelOffset] = destinationNoDataInt;
                                        } else {
                                            d[dstPixelOffset] = getData().getElem(b,
                                                    (s[srcPixelOffset]) & 0xFF);
                                        }
                                    }
                                } else {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                }
                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                } else {
                    for (int b = 0; b < dstNumBands; b++) {
                        byte[] s = bSrcData[b];
                        int[] d = iDstData[b];
                        int[] t = iTblData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];
                        int tblOffset = tblOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, 0);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        byte value = (s[srcPixelOffset]);
                                        if (noData.contains(value)) {
                                            d[dstPixelOffset] = destinationNoDataInt;
                                        } else {
                                            d[dstPixelOffset] = t[value&0xFF - tblOffset];
                                        }
                                    }
                                } else {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                }
                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                }
            }
        }
    }

    // byte to float
    private void lookup(int srcLineStride, int srcPixelStride, int[] srcBandOffsets,
            byte[][] bSrcData, int dstWidth, int dstHeight, int dstNumBands, int dstLineStride,
            int dstPixelStride, int[] dstBandOffsets, float[][] fDstData, int[] tblOffsets,
            float[][] fTblData, RasterAccessor roi, RandomIter roiIter, Rectangle destRect) {

        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        int roiLineStride = 0;
        byte[] roiDataArray = null;
        int roiDataLength = 0;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiLineStride = roi.getScanlineStride();
        }

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;

        if (caseA) {
            for (int b = 0; b < dstNumBands; b++) {
                final byte[] s = bSrcData[b];
                final float[] d = fDstData[b];
                final float[] t = fTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < dstWidth; w++) {
                        d[dstPixelOffset] = t[(s[srcPixelOffset]&0xFF) - tblOffset];

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else if (caseB) {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    byte[] s = bSrcData[b];
                    float[] d = fDstData[b];
                    float[] t = fTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataFloat;
                            } else {
                                d[dstPixelOffset] = t[(s[srcPixelOffset]&0xFF) - tblOffset];
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    byte[] s = bSrcData[b];
                    float[] d = fDstData[b];
                    float[] t = fTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataFloat;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset]&0xFF) - tblOffset];
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataFloat;
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            }
        } else if (caseC) {
            for (int b = 0; b < dstNumBands; b++) {
                byte[] s = bSrcData[b];
                float[] d = fDstData[b];
                float[] t = fTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int y = 0; y < dstHeight; y++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int x = 0; x < dstWidth; x++) {
                        byte value = (s[srcPixelOffset]);
                        if (noData.contains(value)) {
                            d[dstPixelOffset] = destinationNoDataFloat;
                        } else {
                            d[dstPixelOffset] = t[value&0xFF - tblOffset];
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    byte[] s = bSrcData[b];
                    float[] d = fDstData[b];
                    float[] t = fTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataFloat;
                            } else {
                                byte value = (s[srcPixelOffset]);
                                if (noData.contains(value)) {
                                    d[dstPixelOffset] = destinationNoDataFloat;
                                } else {
                                    d[dstPixelOffset] = t[value&0xFF - tblOffset];
                                }
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    byte[] s = bSrcData[b];
                    float[] d = fDstData[b];
                    float[] t = fTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataFloat;
                                } else {
                                    byte value = (s[srcPixelOffset]);
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataFloat;
                                    } else {
                                        d[dstPixelOffset] = t[value&0xFF - tblOffset];
                                    }
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataFloat;
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            }
        }
    }

    // byte to double
    private void lookup(int srcLineStride, int srcPixelStride, int[] srcBandOffsets,
            byte[][] bSrcData, int dstWidth, int dstHeight, int dstNumBands, int dstLineStride,
            int dstPixelStride, int[] dstBandOffsets, double[][] dDstData, int[] tblOffsets,
            double[][] dTblData, RasterAccessor roi, RandomIter roiIter, Rectangle destRect) {

        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        int roiLineStride = 0;
        byte[] roiDataArray = null;
        int roiDataLength = 0;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiLineStride = roi.getScanlineStride();
        }
        
        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;

        if (caseA) {
            for (int b = 0; b < dstNumBands; b++) {
                final byte[] s = bSrcData[b];
                final double[] d = dDstData[b];
                final double[] t = dTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < dstWidth; w++) {
                        d[dstPixelOffset] = t[(s[srcPixelOffset]&0xFF) - tblOffset];

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else if (caseB) {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    byte[] s = bSrcData[b];
                    double[] d = dDstData[b];
                    double[] t = dTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataDouble;
                            } else {
                                d[dstPixelOffset] = t[(s[srcPixelOffset]&0xFF) - tblOffset];
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    byte[] s = bSrcData[b];
                    double[] d = dDstData[b];
                    double[] t = dTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataDouble;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset]&0xFF) - tblOffset];
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataDouble;
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            }
        } else if (caseC) {
            for (int b = 0; b < dstNumBands; b++) {
                byte[] s = bSrcData[b];
                double[] d = dDstData[b];
                double[] t = dTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int y = 0; y < dstHeight; y++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int x = 0; x < dstWidth; x++) {
                        byte value = (s[srcPixelOffset]);
                        if (noData.contains(value)) {
                            d[dstPixelOffset] = destinationNoDataDouble;
                        } else {
                            d[dstPixelOffset] = t[value&0xFF - tblOffset];
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    byte[] s = bSrcData[b];
                    double[] d = dDstData[b];
                    double[] t = dTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataDouble;
                            } else {
                                byte value = (s[srcPixelOffset]);
                                if (noData.contains(value)) {
                                    d[dstPixelOffset] = destinationNoDataDouble;
                                } else {
                                    d[dstPixelOffset] = t[value&0xFF - tblOffset];
                                }
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    byte[] s = bSrcData[b];
                    double[] d = dDstData[b];
                    double[] t = dTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataDouble;
                                } else {
                                    byte value = (s[srcPixelOffset]);
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataDouble;
                                    } else {
                                        d[dstPixelOffset] = t[value&0xFF - tblOffset];
                                    }
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataDouble;
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            }
        }
    }
    

    // ushort to byte
    private void lookupU(int srcLineStride, int srcPixelStride, int[] srcBandOffsets,
            short[][] sSrcData, int dstWidth, int dstHeight, int dstNumBands, int dstLineStride,
            int dstPixelStride, int[] dstBandOffsets, byte[][] bDstData, int[] tblOffsets,
            byte[][] bTblData, RasterAccessor roi, RandomIter roiIter, Rectangle destRect) {

        // Destination image bounds
        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        // ROI parameters
        int roiLineStride = 0;
        byte[] roiDataArray = null;
        int roiDataLength = 0;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiLineStride = roi.getScanlineStride();
        }

        // Boolean indicating the possible situations: with or without ROI,
        // with or without No Data, and a special case when table data are not present
        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;

        if (caseA) {
            // Cycle on all the bands
            for (int b = 0; b < dstNumBands; b++) {
                final short[] s = sSrcData[b];
                final byte[] d = bDstData[b];
                final byte[] t = bTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];
                // Cycle on all the y dimension
                for (int h = 0; h < dstHeight; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;
                    // Cycle on all the x dimension
                    for (int w = 0; w < dstWidth; w++) {
                        // Output value is taken from the table array
                        d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else if (caseB) {
            if (useROIAccessor) {
                // Cycle on all the bands
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    byte[] d = bDstData[b];
                    byte[] t = bTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];
                    // Cycle on all the y dimension
                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;
                        // Cycle on all the x dimension
                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            // Calculation of the x position
                            int posx = (x - dst_min_x) * srcPixelStride;
                            // Calculation of the roi data array index
                            int windex = (posx / dstNumBands) + posyROI;
                            // From the selected index the value is taken
                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                            // If the roi value is 0 the value is outside the ROI, else the table value
                            // is taken

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataByte;
                            } else {
                                d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                // Cycle on all the bands
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    byte[] d = bDstData[b];
                    byte[] t = bTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];
                    // Cycle on all the y dimension
                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;
                        // Cycle on all the x dimension
                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            // If the sample is inside ROI bounds
                            if (roiBounds.contains(x, y)) {
                                // ROI pixel value is calculated
                                int w = roiIter.getSample(x, y, 0);
                                // if is 0 means that the pixel is outside the ROI, else the table data is taken
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataByte;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataByte;
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            }
        } else if (caseC) {
            // Cycle on all the bands
            for (int b = 0; b < dstNumBands; b++) {
                short[] s = sSrcData[b];
                byte[] d = bDstData[b];
                byte[] t = bTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];
                // Cycle on all the y dimension
                for (int y = 0; y < dstHeight; y++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;
                    // Cycle on all the x dimension
                    for (int x = 0; x < dstWidth; x++) {
                        // If the value is a not a noData, the table value is stored
                        short value = (short) (s[srcPixelOffset] & 0xFFFF);
                        if (noData.contains(value)) {
                            d[dstPixelOffset] = destinationNoDataByte;
                        } else {
                            d[dstPixelOffset] = t[value - tblOffset];
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else {
            if (useROIAccessor) {
                // Cycle on all the bands
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    byte[] d = bDstData[b];
                    byte[] t = bTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];
                    // Cycle on all the y dimension
                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;
                        // Cycle on all the x dimension
                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            // Calculation of the x position
                            int posx = (x - dst_min_x) * srcPixelStride;
                            // Calculation of the roi data array index
                            int windex = (posx / dstNumBands) + posyROI;
                            // From the selected index the value is taken
                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                            // If the roi value is 0 the value is outside the ROI, else the table value
                            // is taken

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataByte;
                            } else {
                                // If the value is a not a noData, the table value is stored
                                short value = (short) (s[srcPixelOffset] & 0xFFFF);
                                if (noData.contains(value)) {
                                    d[dstPixelOffset] = destinationNoDataByte;
                                } else {
                                    d[dstPixelOffset] = t[value - tblOffset];
                                }
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                // Cycle on all the bands
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    byte[] d = bDstData[b];
                    byte[] t = bTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];
                    // Cycle on all the y dimension
                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;
                        // Cycle on all the x dimension
                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            // If the sample is inside ROI bounds
                            if (roiBounds.contains(x, y)) {
                                // ROI pixel value is calculated
                                int w = roiIter.getSample(x, y, 0);
                                // if is 0 means that the pixel is outside the ROI, else the table data is taken
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataByte;
                                } else {
                                    // If the value is a not a noData, the table value is stored
                                    short value = (short) (s[srcPixelOffset] & 0xFFFF);
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataByte;
                                    } else {
                                        d[dstPixelOffset] = t[value - tblOffset];
                                    }
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataByte;
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            }
        }
    }

    // ushort to ushort/short
    private void lookupU(int srcLineStride, int srcPixelStride,
            int[] srcBandOffsets, short[][] sSrcData, int dstWidth, int dstHeight, int dstNumBands,
            int dstLineStride, int dstPixelStride, int[] dstBandOffsets, short[][] sDstData,
            int[] tblOffsets, short[][] sTblData, RasterAccessor roi, RandomIter roiIter, Rectangle destRect) {

        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        int roiLineStride = 0;
        byte[] roiDataArray = null;
        int roiDataLength = 0;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiLineStride = roi.getScanlineStride();
        }

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;

        if (caseA) {
            for (int b = 0; b < dstNumBands; b++) {
                final short[] s = sSrcData[b];
                final short[] d = sDstData[b];
                final short[] t = sTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < dstWidth; w++) {
                        d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else if (caseB) {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    short[] d = sDstData[b];
                    short[] t = sTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataShort;
                            } else {
                                d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    short[] d = sDstData[b];
                    short[] t = sTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataShort;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataShort;
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            }
        } else if (caseC) {
            for (int b = 0; b < dstNumBands; b++) {
                short[] s = sSrcData[b];
                short[] d = sDstData[b];
                short[] t = sTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int y = 0; y < dstHeight; y++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int x = 0; x < dstWidth; x++) {
                        short value = (short) (s[srcPixelOffset] & 0xFFFF);
                        if (noData.contains(value)) {
                            d[dstPixelOffset] = destinationNoDataShort;
                        } else {
                            d[dstPixelOffset] = t[value - tblOffset];
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    short[] d = sDstData[b];
                    short[] t = sTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataShort;
                            } else {
                                short value = (short) (s[srcPixelOffset] & 0xFFFF);
                                if (noData.contains(value)) {
                                    d[dstPixelOffset] = destinationNoDataShort;
                                } else {
                                    d[dstPixelOffset] = t[value - tblOffset];
                                }
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    short[] d = sDstData[b];
                    short[] t = sTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataShort;
                                } else {
                                    short value = (short) (s[srcPixelOffset] & 0xFFFF);
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataShort;
                                    } else {
                                        d[dstPixelOffset] = t[value - tblOffset];
                                    }
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataShort;
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            }
        }
    }

    // ushort to int
    private void lookupU(int srcLineStride, int srcPixelStride, int[] srcBandOffsets,
            short[][] sSrcData, int dstWidth, int dstHeight, int dstNumBands, int dstLineStride,
            int dstPixelStride, int[] dstBandOffsets, int[][] iDstData, int[] tblOffsets,
            int[][] iTblData, RasterAccessor roi, RandomIter roiIter, Rectangle destRect) {

        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        int roiLineStride = 0;
        byte[] roiDataArray = null;
        int roiDataLength = 0;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiLineStride = roi.getScanlineStride();
        }

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;
        final boolean caseNull = iTblData == null;

        if (caseA) {
            if (caseNull) {
                for (int b = 0; b < dstNumBands; b++) {
                    final short[] s = sSrcData[b];
                    final int[] d = iDstData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];

                    for (int h = 0; h < dstHeight; h++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int w = 0; w < dstWidth; w++) {
                            d[dstPixelOffset] = getData().getElem(b, s[srcPixelOffset] & 0xFFFF);

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    final short[] s = sSrcData[b];
                    final int[] d = iDstData[b];
                    final int[] t = iTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int h = 0; h < dstHeight; h++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int w = 0; w < dstWidth; w++) {
                            d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            }
        } else if (caseB) {
            if (useROIAccessor) {
                if (caseNull) {
                    for (int b = 0; b < dstNumBands; b++) {
                        final short[] s = sSrcData[b];
                        final int[] d = iDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            int posyROI = (y - dst_min_y) * roiLineStride;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                int posx = (x - dst_min_x) * srcPixelStride;

                                int windex = (posx / dstNumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                } else {
                                    d[dstPixelOffset] = getData().getElem(b, s[srcPixelOffset] & 0xFFFF);
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                } else {
                    for (int b = 0; b < dstNumBands; b++) {
                        short[] s = sSrcData[b];
                        int[] d = iDstData[b];
                        int[] t = iTblData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];
                        int tblOffset = tblOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            int posyROI = (y - dst_min_y) * roiLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                int posx = (x - dst_min_x) * srcPixelStride;

                                int windex = (posx / dstNumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                }
            } else {
                if (caseNull) {
                    for (int b = 0; b < dstNumBands; b++) {
                        final short[] s = sSrcData[b];
                        final int[] d = iDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, 0);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        d[dstPixelOffset] = getData().getElem(b,
                                                s[srcPixelOffset] & 0xFFFF);
                                    }
                                } else {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                } else {
                    for (int b = 0; b < dstNumBands; b++) {
                        short[] s = sSrcData[b];
                        int[] d = iDstData[b];
                        int[] t = iTblData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];
                        int tblOffset = tblOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {
                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, 0);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF)
                                                - tblOffset];
                                    }
                                } else {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                }
            }
        } else if (caseC) {
            if (caseNull) {
                for (int b = 0; b < dstNumBands; b++) {
                    final short[] s = sSrcData[b];
                    final int[] d = iDstData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];

                    for (int y = 0; y < dstHeight; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = 0; x < dstWidth; x++) {

                            short value = (short) (s[srcPixelOffset] & 0xFFFF);
                            if (noData.contains(value)) {
                                d[dstPixelOffset] = destinationNoDataInt;
                            } else {
                                d[dstPixelOffset] = getData().getElem(b, value);
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    int[] d = iDstData[b];
                    int[] t = iTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = 0; y < dstHeight; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = 0; x < dstWidth; x++) {
                            short value = (short) (s[srcPixelOffset] & 0xFFFF);
                            if (noData.contains(value)) {
                                d[dstPixelOffset] = destinationNoDataInt;
                            } else {
                                d[dstPixelOffset] = t[value - tblOffset];
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            }
        } else {
            if (useROIAccessor) {
                if (caseNull) {
                    for (int b = 0; b < dstNumBands; b++) {
                        final short[] s = sSrcData[b];
                        final int[] d = iDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            int posyROI = (y - dst_min_y) * roiLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                int posx = (x - dst_min_x) * srcPixelStride;

                                int windex = (posx / dstNumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                } else {
                                    short value = (short) (s[srcPixelOffset] & 0xFFFF);
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        d[dstPixelOffset] = getData().getElem(b, value);
                                    }
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                } else {
                    for (int b = 0; b < dstNumBands; b++) {
                        short[] s = sSrcData[b];
                        int[] d = iDstData[b];
                        int[] t = iTblData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];
                        int tblOffset = tblOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            int posyROI = (y - dst_min_y) * roiLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                int posx = (x - dst_min_x) * srcPixelStride;

                                int windex = (posx / dstNumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                } else {
                                    short value = (short) (s[srcPixelOffset] & 0xFFFF);
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        d[dstPixelOffset] = t[value - tblOffset];
                                    }
                                }
                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                }
            } else {
                if (caseNull) {
                    for (int b = 0; b < dstNumBands; b++) {
                        final short[] s = sSrcData[b];
                        final int[] d = iDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, 0);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        short value = (short) (s[srcPixelOffset] & 0xFFFF);
                                        if (noData.contains(value)) {
                                            d[dstPixelOffset] = destinationNoDataInt;
                                        } else {
                                            d[dstPixelOffset] = getData().getElem(b, value);
                                        }
                                    }
                                } else {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                }
                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                } else {
                    for (int b = 0; b < dstNumBands; b++) {
                        short[] s = sSrcData[b];
                        int[] d = iDstData[b];
                        int[] t = iTblData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];
                        int tblOffset = tblOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, 0);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        short value = (short) (s[srcPixelOffset] & 0xFFFF);
                                        if (noData.contains(value)) {
                                            d[dstPixelOffset] = destinationNoDataInt;
                                        } else {
                                            d[dstPixelOffset] = t[value - tblOffset];
                                        }
                                    }
                                } else {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                }
                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                }
            }
        }
    }

    // ushort to float
    private void lookupU(int srcLineStride, int srcPixelStride, int[] srcBandOffsets,
            short[][] sSrcData, int dstWidth, int dstHeight, int dstNumBands, int dstLineStride,
            int dstPixelStride, int[] dstBandOffsets, float[][] fDstData, int[] tblOffsets,
            float[][] fTblData, RasterAccessor roi, RandomIter roiIter, Rectangle destRect) {

        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        int roiLineStride = 0;
        byte[] roiDataArray = null;
        int roiDataLength = 0;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiLineStride = roi.getScanlineStride();
        }

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;

        if (caseA) {
            for (int b = 0; b < dstNumBands; b++) {
                final short[] s = sSrcData[b];
                final float[] d = fDstData[b];
                final float[] t = fTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < dstWidth; w++) {
                        d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else if (caseB) {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    float[] d = fDstData[b];
                    float[] t = fTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataFloat;
                            } else {
                                d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    float[] d = fDstData[b];
                    float[] t = fTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataFloat;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataFloat;
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            }
        } else if (caseC) {
            for (int b = 0; b < dstNumBands; b++) {
                short[] s = sSrcData[b];
                float[] d = fDstData[b];
                float[] t = fTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int y = 0; y < dstHeight; y++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int x = 0; x < dstWidth; x++) {
                        short value = (short) (s[srcPixelOffset] & 0xFFFF);
                        if (noData.contains(value)) {
                            d[dstPixelOffset] = destinationNoDataFloat;
                        } else {
                            d[dstPixelOffset] = t[value - tblOffset];
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    float[] d = fDstData[b];
                    float[] t = fTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataFloat;
                            } else {
                                short value = (short) (s[srcPixelOffset] & 0xFFFF);
                                if (noData.contains(value)) {
                                    d[dstPixelOffset] = destinationNoDataFloat;
                                } else {
                                    d[dstPixelOffset] = t[value - tblOffset];
                                }
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    float[] d = fDstData[b];
                    float[] t = fTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataFloat;
                                } else {
                                    short value = (short) (s[srcPixelOffset] & 0xFFFF);
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataFloat;
                                    } else {
                                        d[dstPixelOffset] = t[value - tblOffset];
                                    }
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataFloat;
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            }
        }
    }

    // ushort to double
    private void lookupU(int srcLineStride, int srcPixelStride, int[] srcBandOffsets,
            short[][] sSrcData, int dstWidth, int dstHeight, int dstNumBands, int dstLineStride,
            int dstPixelStride, int[] dstBandOffsets, double[][] dDstData, int[] tblOffsets,
            double[][] dTblData, RasterAccessor roi, RandomIter roiIter, Rectangle destRect) {

        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        int roiLineStride = 0;
        byte[] roiDataArray = null;
        int roiDataLength = 0;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiLineStride = roi.getScanlineStride();
        }

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;

        if (caseA) {
            for (int b = 0; b < dstNumBands; b++) {
                final short[] s = sSrcData[b];
                final double[] d = dDstData[b];
                final double[] t = dTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < dstWidth; w++) {
                        d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else if (caseB) {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    double[] d = dDstData[b];
                    double[] t = dTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataDouble;
                            } else {
                                d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    double[] d = dDstData[b];
                    double[] t = dTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataDouble;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataDouble;
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            }
        } else if (caseC) {
            for (int b = 0; b < dstNumBands; b++) {
                short[] s = sSrcData[b];
                double[] d = dDstData[b];
                double[] t = dTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int y = 0; y < dstHeight; y++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int x = 0; x < dstWidth; x++) {
                        short value = (short) (s[srcPixelOffset] & 0xFFFF);
                        if (noData.contains(value)) {
                            d[dstPixelOffset] = destinationNoDataDouble;
                        } else {
                            d[dstPixelOffset] = t[value - tblOffset];
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    double[] d = dDstData[b];
                    double[] t = dTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataDouble;
                            } else {
                                short value = (short) (s[srcPixelOffset] & 0xFFFF);
                                if (noData.contains(value)) {
                                    d[dstPixelOffset] = destinationNoDataDouble;
                                } else {
                                    d[dstPixelOffset] = t[value - tblOffset];
                                }
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    double[] d = dDstData[b];
                    double[] t = dTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataDouble;
                                } else {
                                    short value = (short) (s[srcPixelOffset] & 0xFFFF);
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataDouble;
                                    } else {
                                        d[dstPixelOffset] = t[value - tblOffset];
                                    }
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataDouble;
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            }
        }
    }
    

    // short to byte
    private void lookup(int srcLineStride, int srcPixelStride, int[] srcBandOffsets,
            short[][] sSrcData, int dstWidth, int dstHeight, int dstNumBands, int dstLineStride,
            int dstPixelStride, int[] dstBandOffsets, byte[][] bDstData, int[] tblOffsets,
            byte[][] bTblData, RasterAccessor roi, RandomIter roiIter, Rectangle destRect) {

        // Destination image bounds
        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        // ROI parameters
        int roiLineStride = 0;
        byte[] roiDataArray = null;
        int roiDataLength = 0;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiLineStride = roi.getScanlineStride();
        }

        // Boolean indicating the possible situations: with or without ROI,
        // with or without No Data, and a special case when table data are not present
        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;

        if (caseA) {
            // Cycle on all the bands
            for (int b = 0; b < dstNumBands; b++) {
                final short[] s = sSrcData[b];
                final byte[] d = bDstData[b];
                final byte[] t = bTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];
                // Cycle on all the y dimension
                for (int h = 0; h < dstHeight; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;
                    // Cycle on all the x dimension
                    for (int w = 0; w < dstWidth; w++) {
                        // Output value is taken from the table array
                        d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else if (caseB) {
            if (useROIAccessor) {
                // Cycle on all the bands
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    byte[] d = bDstData[b];
                    byte[] t = bTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];
                    // Cycle on all the y dimension
                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;
                        // Cycle on all the x dimension
                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            // Calculation of the x position
                            int posx = (x - dst_min_x) * srcPixelStride;
                            // Calculation of the roi data array index
                            int windex = (posx / dstNumBands) + posyROI;
                            // From the selected index the value is taken
                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                            // If the roi value is 0 the value is outside the ROI, else the table value
                            // is taken

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataByte;
                            } else {
                                d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                // Cycle on all the bands
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    byte[] d = bDstData[b];
                    byte[] t = bTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];
                    // Cycle on all the y dimension
                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;
                        // Cycle on all the x dimension
                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            // If the sample is inside ROI bounds
                            if (roiBounds.contains(x, y)) {
                                // ROI pixel value is calculated
                                int w = roiIter.getSample(x, y, 0);
                                // if is 0 means that the pixel is outside the ROI, else the table data is taken
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataByte;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataByte;
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            }
        } else if (caseC) {
            // Cycle on all the bands
            for (int b = 0; b < dstNumBands; b++) {
                short[] s = sSrcData[b];
                byte[] d = bDstData[b];
                byte[] t = bTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];
                // Cycle on all the y dimension
                for (int y = 0; y < dstHeight; y++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;
                    // Cycle on all the x dimension
                    for (int x = 0; x < dstWidth; x++) {
                        // If the value is a not a noData, the table value is stored
                        short value = s[srcPixelOffset];
                        if (noData.contains(value)) {
                            d[dstPixelOffset] = destinationNoDataByte;
                        } else {
                            d[dstPixelOffset] = t[value - tblOffset];
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else {
            if (useROIAccessor) {
                // Cycle on all the bands
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    byte[] d = bDstData[b];
                    byte[] t = bTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];
                    // Cycle on all the y dimension
                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;
                        // Cycle on all the x dimension
                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            // Calculation of the x position
                            int posx = (x - dst_min_x) * srcPixelStride;
                            // Calculation of the roi data array index
                            int windex = (posx / dstNumBands) + posyROI;
                            // From the selected index the value is taken
                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                            // If the roi value is 0 the value is outside the ROI, else the table value
                            // is taken

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataByte;
                            } else {
                                // If the value is a not a noData, the table value is stored
                                short value = s[srcPixelOffset];
                                if (noData.contains(value)) {
                                    d[dstPixelOffset] = destinationNoDataByte;
                                } else {
                                    d[dstPixelOffset] = t[value - tblOffset];
                                }
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                // Cycle on all the bands
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    byte[] d = bDstData[b];
                    byte[] t = bTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];
                    // Cycle on all the y dimension
                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;
                        // Cycle on all the x dimension
                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            // If the sample is inside ROI bounds
                            if (roiBounds.contains(x, y)) {
                                // ROI pixel value is calculated
                                int w = roiIter.getSample(x, y, 0);
                                // if is 0 means that the pixel is outside the ROI, else the table data is taken
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataByte;
                                } else {
                                    // If the value is a not a noData, the table value is stored
                                    short value = s[srcPixelOffset];
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataByte;
                                    } else {
                                        d[dstPixelOffset] = t[value - tblOffset];
                                    }
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataByte;
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            }
        }
    }

    // short to ushort/short
    private void lookup(int srcLineStride, int srcPixelStride,
            int[] srcBandOffsets, short[][] sSrcData, int dstWidth, int dstHeight, int dstNumBands,
            int dstLineStride, int dstPixelStride, int[] dstBandOffsets, short[][] sDstData,
            int[] tblOffsets, short[][] sTblData, RasterAccessor roi, RandomIter roiIter, Rectangle destRect) {

        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        int roiLineStride = 0;
        byte[] roiDataArray = null;
        int roiDataLength = 0;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiLineStride = roi.getScanlineStride();
        }

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;

        if (caseA) {
            for (int b = 0; b < dstNumBands; b++) {
                final short[] s = sSrcData[b];
                final short[] d = sDstData[b];
                final short[] t = sTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < dstWidth; w++) {
                        d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else if (caseB) {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    short[] d = sDstData[b];
                    short[] t = sTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataShort;
                            } else {
                                d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    short[] d = sDstData[b];
                    short[] t = sTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataShort;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataShort;
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            }
        } else if (caseC) {
            for (int b = 0; b < dstNumBands; b++) {
                short[] s = sSrcData[b];
                short[] d = sDstData[b];
                short[] t = sTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int y = 0; y < dstHeight; y++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int x = 0; x < dstWidth; x++) {
                        short value = s[srcPixelOffset];
                        if (noData.contains(value)) {
                            d[dstPixelOffset] = destinationNoDataShort;
                        } else {
                            d[dstPixelOffset] = t[value - tblOffset];
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    short[] d = sDstData[b];
                    short[] t = sTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataShort;
                            } else {
                                short value = s[srcPixelOffset];
                                if (noData.contains(value)) {
                                    d[dstPixelOffset] = destinationNoDataShort;
                                } else {
                                    d[dstPixelOffset] = t[value - tblOffset];
                                }
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    short[] d = sDstData[b];
                    short[] t = sTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataShort;
                                } else {
                                    short value = s[srcPixelOffset];
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataShort;
                                    } else {
                                        d[dstPixelOffset] = t[value - tblOffset];
                                    }
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataShort;
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            }
        }
    }

    // short to int
    private void lookup(int srcLineStride, int srcPixelStride, int[] srcBandOffsets,
            short[][] sSrcData, int dstWidth, int dstHeight, int dstNumBands, int dstLineStride,
            int dstPixelStride, int[] dstBandOffsets, int[][] iDstData, int[] tblOffsets,
            int[][] iTblData, RasterAccessor roi, RandomIter roiIter, Rectangle destRect) {

        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        int roiLineStride = 0;
        byte[] roiDataArray = null;
        int roiDataLength = 0;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiLineStride = roi.getScanlineStride();
        }

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;
        final boolean caseNull = iTblData == null;

        if (caseA) {
            if (caseNull) {
                for (int b = 0; b < dstNumBands; b++) {
                    final short[] s = sSrcData[b];
                    final int[] d = iDstData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];

                    for (int h = 0; h < dstHeight; h++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int w = 0; w < dstWidth; w++) {
                            d[dstPixelOffset] = getData().getElem(b, s[srcPixelOffset]);

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    final short[] s = sSrcData[b];
                    final int[] d = iDstData[b];
                    final int[] t = iTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int h = 0; h < dstHeight; h++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int w = 0; w < dstWidth; w++) {
                            d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            }
        } else if (caseB) {
            if (useROIAccessor) {
                if (caseNull) {
                    for (int b = 0; b < dstNumBands; b++) {
                        final short[] s = sSrcData[b];
                        final int[] d = iDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            int posyROI = (y - dst_min_y) * roiLineStride;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                int posx = (x - dst_min_x) * srcPixelStride;

                                int windex = (posx / dstNumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                } else {
                                    d[dstPixelOffset] = getData().getElem(b, s[srcPixelOffset]);
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                } else {
                    for (int b = 0; b < dstNumBands; b++) {
                        short[] s = sSrcData[b];
                        int[] d = iDstData[b];
                        int[] t = iTblData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];
                        int tblOffset = tblOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            int posyROI = (y - dst_min_y) * roiLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                int posx = (x - dst_min_x) * srcPixelStride;

                                int windex = (posx / dstNumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                }
            } else {
                if (caseNull) {
                    for (int b = 0; b < dstNumBands; b++) {
                        final short[] s = sSrcData[b];
                        final int[] d = iDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, 0);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        d[dstPixelOffset] = getData().getElem(b, s[srcPixelOffset]);
                                    }
                                } else {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                } else {
                    for (int b = 0; b < dstNumBands; b++) {
                        short[] s = sSrcData[b];
                        int[] d = iDstData[b];
                        int[] t = iTblData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];
                        int tblOffset = tblOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {
                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, 0);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];
                                    }
                                } else {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                }
            }
        } else if (caseC) {
            if (caseNull) {
                for (int b = 0; b < dstNumBands; b++) {
                    final short[] s = sSrcData[b];
                    final int[] d = iDstData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];

                    for (int y = 0; y < dstHeight; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = 0; x < dstWidth; x++) {

                            short value = s[srcPixelOffset];
                            if (noData.contains(value)) {
                                d[dstPixelOffset] = destinationNoDataInt;
                            } else {
                                d[dstPixelOffset] = getData().getElem(b, value);
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    int[] d = iDstData[b];
                    int[] t = iTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = 0; y < dstHeight; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = 0; x < dstWidth; x++) {
                            short value = s[srcPixelOffset];
                            if (noData.contains(value)) {
                                d[dstPixelOffset] = destinationNoDataInt;
                            } else {
                                d[dstPixelOffset] = t[value - tblOffset];
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            }
        } else {
            if (useROIAccessor) {
                if (caseNull) {
                    for (int b = 0; b < dstNumBands; b++) {
                        final short[] s = sSrcData[b];
                        final int[] d = iDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            int posyROI = (y - dst_min_y) * roiLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                int posx = (x - dst_min_x) * srcPixelStride;

                                int windex = (posx / dstNumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                } else {
                                    short value = s[srcPixelOffset];
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        d[dstPixelOffset] = getData().getElem(b, value);
                                    }
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                } else {
                    for (int b = 0; b < dstNumBands; b++) {
                        short[] s = sSrcData[b];
                        int[] d = iDstData[b];
                        int[] t = iTblData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];
                        int tblOffset = tblOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            int posyROI = (y - dst_min_y) * roiLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                int posx = (x - dst_min_x) * srcPixelStride;

                                int windex = (posx / dstNumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                } else {
                                    short value = s[srcPixelOffset];
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        d[dstPixelOffset] = t[value - tblOffset];
                                    }
                                }
                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                }
            } else {
                if (caseNull) {
                    for (int b = 0; b < dstNumBands; b++) {
                        final short[] s = sSrcData[b];
                        final int[] d = iDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, 0);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        short value = s[srcPixelOffset];
                                        if (noData.contains(value)) {
                                            d[dstPixelOffset] = destinationNoDataInt;
                                        } else {
                                            d[dstPixelOffset] = getData().getElem(b, value);
                                        }
                                    }
                                } else {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                }
                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                } else {
                    for (int b = 0; b < dstNumBands; b++) {
                        short[] s = sSrcData[b];
                        int[] d = iDstData[b];
                        int[] t = iTblData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];
                        int tblOffset = tblOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, 0);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        short value = s[srcPixelOffset];
                                        if (noData.contains(value)) {
                                            d[dstPixelOffset] = destinationNoDataInt;
                                        } else {
                                            d[dstPixelOffset] = t[value - tblOffset];
                                        }
                                    }
                                } else {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                }
                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                }
            }
        }
    }

    // short to float
    private void lookup(int srcLineStride, int srcPixelStride, int[] srcBandOffsets,
            short[][] sSrcData, int dstWidth, int dstHeight, int dstNumBands, int dstLineStride,
            int dstPixelStride, int[] dstBandOffsets, float[][] fDstData, int[] tblOffsets,
            float[][] fTblData, RasterAccessor roi, RandomIter roiIter, Rectangle destRect) {

        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        int roiLineStride = 0;
        byte[] roiDataArray = null;
        int roiDataLength = 0;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiLineStride = roi.getScanlineStride();
        }

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;

        if (caseA) {
            for (int b = 0; b < dstNumBands; b++) {
                final short[] s = sSrcData[b];
                final float[] d = fDstData[b];
                final float[] t = fTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < dstWidth; w++) {
                        d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else if (caseB) {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    float[] d = fDstData[b];
                    float[] t = fTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataFloat;
                            } else {
                                d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    float[] d = fDstData[b];
                    float[] t = fTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataFloat;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataFloat;
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            }
        } else if (caseC) {
            for (int b = 0; b < dstNumBands; b++) {
                short[] s = sSrcData[b];
                float[] d = fDstData[b];
                float[] t = fTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int y = 0; y < dstHeight; y++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int x = 0; x < dstWidth; x++) {
                        short value = s[srcPixelOffset];
                        if (noData.contains(value)) {
                            d[dstPixelOffset] = destinationNoDataFloat;
                        } else {
                            d[dstPixelOffset] = t[value - tblOffset];
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    float[] d = fDstData[b];
                    float[] t = fTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataFloat;
                            } else {
                                short value = s[srcPixelOffset];
                                if (noData.contains(value)) {
                                    d[dstPixelOffset] = destinationNoDataFloat;
                                } else {
                                    d[dstPixelOffset] = t[value - tblOffset];
                                }
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    float[] d = fDstData[b];
                    float[] t = fTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataFloat;
                                } else {
                                    short value = s[srcPixelOffset];
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataFloat;
                                    } else {
                                        d[dstPixelOffset] = t[value - tblOffset];
                                    }
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataFloat;
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            }
        }
    }

    // short to double
    private void lookup(int srcLineStride, int srcPixelStride, int[] srcBandOffsets,
            short[][] sSrcData, int dstWidth, int dstHeight, int dstNumBands, int dstLineStride,
            int dstPixelStride, int[] dstBandOffsets, double[][] dDstData, int[] tblOffsets,
            double[][] dTblData, RasterAccessor roi, RandomIter roiIter, Rectangle destRect) {

        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        int roiLineStride = 0;
        byte[] roiDataArray = null;
        int roiDataLength = 0;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiLineStride = roi.getScanlineStride();
        }

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;

        if (caseA) {
            for (int b = 0; b < dstNumBands; b++) {
                final short[] s = sSrcData[b];
                final double[] d = dDstData[b];
                final double[] t = dTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < dstWidth; w++) {
                        d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else if (caseB) {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    double[] d = dDstData[b];
                    double[] t = dTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataDouble;
                            } else {
                                d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    double[] d = dDstData[b];
                    double[] t = dTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataDouble;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataDouble;
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            }
        } else if (caseC) {
            for (int b = 0; b < dstNumBands; b++) {
                short[] s = sSrcData[b];
                double[] d = dDstData[b];
                double[] t = dTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int y = 0; y < dstHeight; y++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int x = 0; x < dstWidth; x++) {
                        short value = s[srcPixelOffset];
                        if (noData.contains(value)) {
                            d[dstPixelOffset] = destinationNoDataDouble;
                        } else {
                            d[dstPixelOffset] = t[value - tblOffset];
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    double[] d = dDstData[b];
                    double[] t = dTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataDouble;
                            } else {
                                short value = s[srcPixelOffset];
                                if (noData.contains(value)) {
                                    d[dstPixelOffset] = destinationNoDataDouble;
                                } else {
                                    d[dstPixelOffset] = t[value - tblOffset];
                                }
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    double[] d = dDstData[b];
                    double[] t = dTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataDouble;
                                } else {
                                    short value = s[srcPixelOffset];
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataDouble;
                                    } else {
                                        d[dstPixelOffset] = t[value - tblOffset];
                                    }
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataDouble;
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            }
        }
    }
    

    // int to byte
    private void lookup(int srcLineStride, int srcPixelStride, int[] srcBandOffsets,
            int[][] iSrcData, int dstWidth, int dstHeight, int dstNumBands, int dstLineStride,
            int dstPixelStride, int[] dstBandOffsets, byte[][] bDstData, int[] tblOffsets,
            byte[][] bTblData, RasterAccessor roi, RandomIter roiIter, Rectangle destRect) {

        // Destination image bounds
        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        // ROI parameters
        int roiLineStride = 0;
        byte[] roiDataArray = null;
        int roiDataLength = 0;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiLineStride = roi.getScanlineStride();
        }

        // Boolean indicating the possible situations: with or without ROI,
        // with or without No Data, and a special case when table data are not present
        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;

        if (caseA) {
            // Cycle on all the bands
            for (int b = 0; b < dstNumBands; b++) {
                final int[] s = iSrcData[b];
                final byte[] d = bDstData[b];
                final byte[] t = bTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];
                // Cycle on all the y dimension
                for (int h = 0; h < dstHeight; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;
                    // Cycle on all the x dimension
                    for (int w = 0; w < dstWidth; w++) {
                        // Output value is taken from the table array
                        d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else if (caseB) {
            if (useROIAccessor) {
                // Cycle on all the bands
                for (int b = 0; b < dstNumBands; b++) {
                    int[] s = iSrcData[b];
                    byte[] d = bDstData[b];
                    byte[] t = bTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];
                    // Cycle on all the y dimension
                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;
                        // Cycle on all the x dimension
                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            // Calculation of the x position
                            int posx = (x - dst_min_x) * srcPixelStride;
                            // Calculation of the roi data array index
                            int windex = (posx / dstNumBands) + posyROI;
                            // From the selected index the value is taken
                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                            // If the roi value is 0 the value is outside the ROI, else the table value
                            // is taken
                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataByte;
                            } else {
                                d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            } else {
                // Cycle on all the bands
                for (int b = 0; b < dstNumBands; b++) {
                    int[] s = iSrcData[b];
                    byte[] d = bDstData[b];
                    byte[] t = bTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];
                    // Cycle on all the y dimension
                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;
                        // Cycle on all the x dimension
                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            // If the sample is inside ROI bounds
                            if (roiBounds.contains(x, y)) {
                                // ROI pixel value is calculated
                                int w = roiIter.getSample(x, y, 0);
                                // if is 0 means that the pixel is outside the ROI, else the table data is taken
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataByte;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataByte;
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            }
        } else if (caseC) {
            // Cycle on all the bands
            for (int b = 0; b < dstNumBands; b++) {
                int[] s = iSrcData[b];
                byte[] d = bDstData[b];
                byte[] t = bTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];
                // Cycle on all the y dimension
                for (int y = 0; y < dstHeight; y++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;
                    // Cycle on all the x dimension
                    for (int x = 0; x < dstWidth; x++) {
                        // If the value is a not a noData, the table value is stored
                        int value = s[srcPixelOffset];
                        if (noData.contains(value)) {
                            d[dstPixelOffset] = destinationNoDataByte;
                        } else {
                            d[dstPixelOffset] = t[value - tblOffset];
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else {
            if (useROIAccessor) {
                // Cycle on all the bands
                for (int b = 0; b < dstNumBands; b++) {
                    int[] s = iSrcData[b];
                    byte[] d = bDstData[b];
                    byte[] t = bTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];
                    // Cycle on all the y dimension
                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;
                        // Cycle on all the x dimension
                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            // Calculation of the x position
                            int posx = (x - dst_min_x) * srcPixelStride;
                            // Calculation of the roi data array index
                            int windex = (posx / dstNumBands) + posyROI;
                            // From the selected index the value is taken
                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                            // If the roi value is 0 the value is outside the ROI, else the table value
                            // is taken
                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataByte;
                            } else {
                                // If the value is a not a noData, the table value is stored
                                int value = s[srcPixelOffset];
                                if (noData.contains(value)) {
                                    d[dstPixelOffset] = destinationNoDataByte;
                                } else {
                                    d[dstPixelOffset] = t[value - tblOffset];
                                }
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                // Cycle on all the bands
                for (int b = 0; b < dstNumBands; b++) {
                    int[] s = iSrcData[b];
                    byte[] d = bDstData[b];
                    byte[] t = bTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];
                    // Cycle on all the y dimension
                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;
                        // Cycle on all the x dimension
                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            // If the sample is inside ROI bounds
                            if (roiBounds.contains(x, y)) {
                                // ROI pixel value is calculated
                                int w = roiIter.getSample(x, y, 0);
                                // if is 0 means that the pixel is outside the ROI, else the table data is taken
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataByte;
                                } else {
                                    // If the value is a not a noData, the table value is stored
                                    int value = s[srcPixelOffset];
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataByte;
                                    } else {
                                        d[dstPixelOffset] = t[value - tblOffset];
                                    }
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataByte;
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            }
        }
    }

    // int to ushort/short
    private void lookup(int srcLineStride, int srcPixelStride,
            int[] srcBandOffsets, int[][] iSrcData, int dstWidth, int dstHeight, int dstNumBands,
            int dstLineStride, int dstPixelStride, int[] dstBandOffsets, short[][] sDstData,
            int[] tblOffsets, short[][] sTblData, RasterAccessor roi, RandomIter roiIter, Rectangle destRect) {

        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        int roiLineStride = 0;
        byte[] roiDataArray = null;
        int roiDataLength = 0;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiLineStride = roi.getScanlineStride();
        }

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;

        if (caseA) {
            for (int b = 0; b < dstNumBands; b++) {
                final int[] s = iSrcData[b];
                final short[] d = sDstData[b];
                final short[] t = sTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < dstWidth; w++) {
                        d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else if (caseB) {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    int[] s = iSrcData[b];
                    short[] d = sDstData[b];
                    short[] t = sTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataShort;
                            } else {
                                d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    int[] s = iSrcData[b];
                    short[] d = sDstData[b];
                    short[] t = sTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataShort;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataShort;
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            }
        } else if (caseC) {
            for (int b = 0; b < dstNumBands; b++) {
                int[] s = iSrcData[b];
                short[] d = sDstData[b];
                short[] t = sTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int y = 0; y < dstHeight; y++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int x = 0; x < dstWidth; x++) {
                        int value = s[srcPixelOffset];
                        if (noData.contains(value)) {
                            d[dstPixelOffset] = destinationNoDataShort;
                        } else {
                            d[dstPixelOffset] = t[value - tblOffset];
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    int[] s = iSrcData[b];
                    short[] d = sDstData[b];
                    short[] t = sTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataShort;
                            } else {
                                int value = s[srcPixelOffset];
                                if (noData.contains(value)) {
                                    d[dstPixelOffset] = destinationNoDataShort;
                                } else {
                                    d[dstPixelOffset] = t[value - tblOffset];
                                }
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    int[] s = iSrcData[b];
                    short[] d = sDstData[b];
                    short[] t = sTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataShort;
                                } else {
                                    int value = s[srcPixelOffset];
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataShort;
                                    } else {
                                        d[dstPixelOffset] = t[value - tblOffset];
                                    }
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataShort;
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            }
        }
    }

    // int to int
    private void lookup(int srcLineStride, int srcPixelStride, int[] srcBandOffsets,
            int[][] iSrcData, int dstWidth, int dstHeight, int dstNumBands, int dstLineStride,
            int dstPixelStride, int[] dstBandOffsets, int[][] iDstData, int[] tblOffsets,
            int[][] iTblData, RasterAccessor roi, RandomIter roiIter, Rectangle destRect) {

        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        int roiLineStride = 0;
        byte[] roiDataArray = null;
        int roiDataLength = 0;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiLineStride = roi.getScanlineStride();
        }

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;
        final boolean caseNull = iTblData == null;

        if (caseA) {
            if (caseNull) {
                for (int b = 0; b < dstNumBands; b++) {
                    final int[] s = iSrcData[b];
                    final int[] d = iDstData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];

                    for (int h = 0; h < dstHeight; h++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int w = 0; w < dstWidth; w++) {
                            d[dstPixelOffset] = getData().getElem(b, s[srcPixelOffset]);

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    final int[] s = iSrcData[b];
                    final int[] d = iDstData[b];
                    final int[] t = iTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int h = 0; h < dstHeight; h++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int w = 0; w < dstWidth; w++) {
                            d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            }
        } else if (caseB) {
            if (useROIAccessor) {
                if (caseNull) {
                    for (int b = 0; b < dstNumBands; b++) {
                        final int[] s = iSrcData[b];
                        final int[] d = iDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            int posyROI = (y - dst_min_y) * roiLineStride;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                int posx = (x - dst_min_x) * srcPixelStride;

                                int windex = (posx / dstNumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                } else {
                                    d[dstPixelOffset] = getData().getElem(b, s[srcPixelOffset]);
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                } else {
                    for (int b = 0; b < dstNumBands; b++) {
                        int[] s = iSrcData[b];
                        int[] d = iDstData[b];
                        int[] t = iTblData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];
                        int tblOffset = tblOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            int posyROI = (y - dst_min_y) * roiLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                int posx = (x - dst_min_x) * srcPixelStride;

                                int windex = (posx / dstNumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                }
            } else {
                if (caseNull) {
                    for (int b = 0; b < dstNumBands; b++) {
                        final int[] s = iSrcData[b];
                        final int[] d = iDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, 0);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        d[dstPixelOffset] = getData().getElem(b, s[srcPixelOffset]);
                                    }
                                } else {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                } else {
                    for (int b = 0; b < dstNumBands; b++) {
                        int[] s = iSrcData[b];
                        int[] d = iDstData[b];
                        int[] t = iTblData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];
                        int tblOffset = tblOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {
                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, 0);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];
                                    }
                                } else {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                }
            }
        } else if (caseC) {
            if (caseNull) {
                for (int b = 0; b < dstNumBands; b++) {
                    final int[] s = iSrcData[b];
                    final int[] d = iDstData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];

                    for (int y = 0; y < dstHeight; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = 0; x < dstWidth; x++) {

                            int value = s[srcPixelOffset];
                            if (noData.contains(value)) {
                                d[dstPixelOffset] = destinationNoDataInt;
                            } else {
                                d[dstPixelOffset] = getData().getElem(b, value);
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    int[] s = iSrcData[b];
                    int[] d = iDstData[b];
                    int[] t = iTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = 0; y < dstHeight; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = 0; x < dstWidth; x++) {
                            int value = s[srcPixelOffset];
                            if (noData.contains(value)) {
                                d[dstPixelOffset] = destinationNoDataInt;
                            } else {
                                d[dstPixelOffset] = t[value - tblOffset];
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            }
        } else {
            if (useROIAccessor) {
                if (caseNull) {
                    for (int b = 0; b < dstNumBands; b++) {
                        final int[] s = iSrcData[b];
                        final int[] d = iDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            int posyROI = (y - dst_min_y) * roiLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                int posx = (x - dst_min_x) * srcPixelStride;

                                int windex = (posx / dstNumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                } else {
                                    int value = s[srcPixelOffset];
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        d[dstPixelOffset] = getData().getElem(b, value);
                                    }
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                } else {
                    for (int b = 0; b < dstNumBands; b++) {
                        int[] s = iSrcData[b];
                        int[] d = iDstData[b];
                        int[] t = iTblData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];
                        int tblOffset = tblOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            int posyROI = (y - dst_min_y) * roiLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                int posx = (x - dst_min_x) * srcPixelStride;

                                int windex = (posx / dstNumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                } else {
                                    int value = s[srcPixelOffset];
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        d[dstPixelOffset] = t[value - tblOffset];
                                    }
                                }
                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                }
            } else {
                if (caseNull) {
                    for (int b = 0; b < dstNumBands; b++) {
                        final int[] s = iSrcData[b];
                        final int[] d = iDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, 0);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        int value = s[srcPixelOffset];
                                        if (noData.contains(value)) {
                                            d[dstPixelOffset] = destinationNoDataInt;
                                        } else {
                                            d[dstPixelOffset] = getData().getElem(b, value);
                                        }
                                    }
                                } else {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                }
                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                } else {
                    for (int b = 0; b < dstNumBands; b++) {
                        int[] s = iSrcData[b];
                        int[] d = iDstData[b];
                        int[] t = iTblData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];
                        int tblOffset = tblOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, 0);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        int value = s[srcPixelOffset];
                                        if (noData.contains(value)) {
                                            d[dstPixelOffset] = destinationNoDataInt;
                                        } else {
                                            d[dstPixelOffset] = t[value - tblOffset];
                                        }
                                    }
                                } else {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                }
                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                }
            }
        }
    }

    // int to float
    private void lookup(int srcLineStride, int srcPixelStride, int[] srcBandOffsets,
            int[][] iSrcData, int dstWidth, int dstHeight, int dstNumBands, int dstLineStride,
            int dstPixelStride, int[] dstBandOffsets, float[][] fDstData, int[] tblOffsets,
            float[][] fTblData, RasterAccessor roi, RandomIter roiIter, Rectangle destRect) {

        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        int roiLineStride = 0;
        byte[] roiDataArray = null;
        int roiDataLength = 0;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiLineStride = roi.getScanlineStride();
        }

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;

        if (caseA) {
            for (int b = 0; b < dstNumBands; b++) {
                final int[] s = iSrcData[b];
                final float[] d = fDstData[b];
                final float[] t = fTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < dstWidth; w++) {
                        d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else if (caseB) {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    int[] s = iSrcData[b];
                    float[] d = fDstData[b];
                    float[] t = fTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataFloat;
                            } else {
                                d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    int[] s = iSrcData[b];
                    float[] d = fDstData[b];
                    float[] t = fTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataFloat;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataFloat;
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            }
        } else if (caseC) {
            for (int b = 0; b < dstNumBands; b++) {
                int[] s = iSrcData[b];
                float[] d = fDstData[b];
                float[] t = fTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int y = 0; y < dstHeight; y++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int x = 0; x < dstWidth; x++) {
                        int value = s[srcPixelOffset];
                        if (noData.contains(value)) {
                            d[dstPixelOffset] = destinationNoDataFloat;
                        } else {
                            d[dstPixelOffset] = t[value - tblOffset];
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    int[] s = iSrcData[b];
                    float[] d = fDstData[b];
                    float[] t = fTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataFloat;
                            } else {
                                int value = s[srcPixelOffset];
                                if (noData.contains(value)) {
                                    d[dstPixelOffset] = destinationNoDataFloat;
                                } else {
                                    d[dstPixelOffset] = t[value - tblOffset];
                                }
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    int[] s = iSrcData[b];
                    float[] d = fDstData[b];
                    float[] t = fTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataFloat;
                                } else {
                                    int value = s[srcPixelOffset];
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataFloat;
                                    } else {
                                        d[dstPixelOffset] = t[value - tblOffset];
                                    }
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataFloat;
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            }
        }
    }

    // int to double
    private void lookup(int srcLineStride, int srcPixelStride, int[] srcBandOffsets,
            int[][] iSrcData, int dstWidth, int dstHeight, int dstNumBands, int dstLineStride,
            int dstPixelStride, int[] dstBandOffsets, double[][] dDstData, int[] tblOffsets,
            double[][] dTblData, RasterAccessor roi, RandomIter roiIter, Rectangle destRect) {

        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        int roiLineStride = 0;
        byte[] roiDataArray = null;
        int roiDataLength = 0;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiLineStride = roi.getScanlineStride();
        }

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;

        if (caseA) {
            for (int b = 0; b < dstNumBands; b++) {
                final int[] s = iSrcData[b];
                final double[] d = dDstData[b];
                final double[] t = dTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < dstWidth; w++) {
                        d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else if (caseB) {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    int[] s = iSrcData[b];
                    double[] d = dDstData[b];
                    double[] t = dTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataDouble;
                            } else {
                                d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    int[] s = iSrcData[b];
                    double[] d = dDstData[b];
                    double[] t = dTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataDouble;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset]) - tblOffset];
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataDouble;
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            }
        } else if (caseC) {
            for (int b = 0; b < dstNumBands; b++) {
                int[] s = iSrcData[b];
                double[] d = dDstData[b];
                double[] t = dTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int y = 0; y < dstHeight; y++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int x = 0; x < dstWidth; x++) {
                        int value = s[srcPixelOffset];
                        if (noData.contains(value)) {
                            d[dstPixelOffset] = destinationNoDataDouble;
                        } else {
                            d[dstPixelOffset] = t[value - tblOffset];
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    int[] s = iSrcData[b];
                    double[] d = dDstData[b];
                    double[] t = dTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataDouble;
                            } else {
                                int value = s[srcPixelOffset];
                                if (noData.contains(value)) {
                                    d[dstPixelOffset] = destinationNoDataDouble;
                                } else {
                                    d[dstPixelOffset] = t[value - tblOffset];
                                }
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    int[] s = iSrcData[b];
                    double[] d = dDstData[b];
                    double[] t = dTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataDouble;
                                } else {
                                    int value = s[srcPixelOffset];
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataDouble;
                                    } else {
                                        d[dstPixelOffset] = t[value - tblOffset];
                                    }
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataDouble;
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            }
        }
    }
}
