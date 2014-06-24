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

import it.geosolutions.jaiext.algebra.AlgebraDescriptor.Operator;
import it.geosolutions.jaiext.range.Range;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import javax.media.jai.ImageLayout;
import javax.media.jai.PointOpImage;
import javax.media.jai.ROI;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.RasterFactory;
import java.util.Map;
import java.util.Vector;

import com.sun.media.jai.util.ImageUtil;
import com.sun.media.jai.util.JDKWorkarounds;

/**
 * OLDER VERSION OF THE AlgebraOpImage CLASS USED ONLY FOR FUTURE OPTIMIZATIONS 
 * 
 */
public final class AlgebraOpImage2 extends PointOpImage {

    private final boolean hasNoData;

    private Range noData;

    private byte[] byteLookupTable;

    private boolean[] booleanLookupTable;

    private final boolean hasROI;

    private ROI roi;

    private final boolean caseA;

    private final boolean caseB;

    private final boolean caseC;

    private Operator op;

    private final int numSrc;

    private byte destNoDataByte;

    private short destNoDataShort;

    private int destNoDataInt;

    private float destNoDataFloat;

    private double destNoDataDouble;

    private byte nullValueByte;

    private short nullValueShort;

    private int nullValueInt;

    private float nullValueFloat;

    private double nullValueDouble;

    /**
     * Constructs an <code>AddOpImage</code>.
     * 
     * <p>
     * The <code>layout</code> parameter may optionally contains the tile grid layout, sample model, and/or color model. The image dimension is
     * determined by the intersection of the bounding boxes of the two source images.
     * 
     * <p>
     * The image layout of the first source image, <code>source1</code>, is used as the fall-back for the image layout of the destination image. Any
     * layout parameters not specified in the <code>layout</code> argument are set to the same value as that of <code>source1</code>.
     * 
     * @param source1 The first source image.
     * @param source2 The second source image.
     * @param layout The destination image layout.
     * @param useROIAccessor
     */
    public AlgebraOpImage2(Map config, ImageLayout layout, Operator op, ROI srcROI, Range noData,
            double destinationNoData, RenderedImage... sources) {
        super(vectorize(sources), layout, config, true);

        if (op == null) {
            throw new IllegalArgumentException("Operation Not Defined");
        } else {
            this.op = op;
        }

        // Get the source band counts.
        numSrc = sources.length;

        // DataType check
        int srcDataType = sources[0].getSampleModel().getDataType();

        int dataType = getSampleModel().getDataType();

        for (RenderedImage img : sources) {
            if (img.getSampleModel().getDataType() != srcDataType) {
                throw new IllegalArgumentException("Images must have the same data type");
            }
        }

        int[] numBandsSrc = new int[numSrc];

        boolean srcBandsToFill = false;

        int numBands0 = sources[0].getSampleModel().getNumBands();

        for (int i = 1; i < numSrc; i++) {
            numBandsSrc[i] = sources[i].getSampleModel().getNumBands();
            if (numBandsSrc[i] != numBands0) {
                srcBandsToFill = true;
                break;
            }

        }

        // Handle the special case of adding a single band image to
        // each band of a multi-band image.
        int numBandsDst;
        if (layout != null && layout.isValid(ImageLayout.SAMPLE_MODEL_MASK)) {
            SampleModel sm = layout.getSampleModel(null);
            numBandsDst = sm.getNumBands();

            // One of the sources must be single-banded and the other must
            // have at most the number of bands in the SampleModel hint.
            if (numBandsDst > 1 && srcBandsToFill) {
                // Clamp the destination band count to the number of
                // bands in the multi-band source.

                int minBandsNum = Integer.MAX_VALUE;

                for (int i = 0; i < numSrc; i++) {

                    int bands = numBandsSrc[i];

                    if (bands < minBandsNum) {
                        minBandsNum = bands;
                    }
                }

                numBandsDst = Math.min(minBandsNum, numBandsDst);

                // Create a new SampleModel if necessary.
                if (numBandsDst != sampleModel.getNumBands()) {
                    sampleModel = RasterFactory.createComponentSampleModel(sm,
                            sampleModel.getTransferType(), sampleModel.getWidth(),
                            sampleModel.getHeight(), numBandsDst);

                    if (colorModel != null
                            && !JDKWorkarounds.areCompatibleDataModels(sampleModel, colorModel)) {
                        colorModel = ImageUtil.getCompatibleColorModel(sampleModel, config);
                    }
                }
            }
        }

        // Destination No Data value is clamped to the image data type
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            this.destNoDataByte = ImageUtil.clampRoundByte(destinationNoData);
            this.nullValueByte = ImageUtil.clampRoundByte(op.getNullValue());
            break;
        case DataBuffer.TYPE_USHORT:
            this.destNoDataShort = ImageUtil.clampRoundUShort(destinationNoData);
            this.nullValueShort = ImageUtil.clampRoundUShort(op.getNullValue());
            break;
        case DataBuffer.TYPE_SHORT:
            this.destNoDataShort = ImageUtil.clampRoundShort(destinationNoData);
            this.nullValueShort = ImageUtil.clampRoundShort(op.getNullValue());
            break;
        case DataBuffer.TYPE_INT:
            this.destNoDataInt = ImageUtil.clampRoundInt(destinationNoData);
            this.nullValueInt = ImageUtil.clampRoundInt(op.getNullValue());
            break;
        case DataBuffer.TYPE_FLOAT:
            this.destNoDataFloat = ImageUtil.clampFloat(destinationNoData);
            this.nullValueFloat = ImageUtil.clampFloat(op.getNullValue());
            break;
        case DataBuffer.TYPE_DOUBLE:
            this.destNoDataDouble = destinationNoData;
            this.nullValueDouble = op.getNullValue();
            break;
        default:
            throw new IllegalArgumentException("Wrong image data type");
        }

        // Check if No Data control must be done
        if (noData != null) {

            hasNoData = true;
            this.noData = noData;
            // Creation of a lookuptable containing the values to use for no data
            if (dataType == DataBuffer.TYPE_BYTE) {
                booleanLookupTable = new boolean[256];
                byteLookupTable = new byte[256];

                for (int i = 0; i < byteLookupTable.length; i++) {
                    byte value = (byte) i;

                    booleanLookupTable[i] = !noData.contains(value);

                    if (booleanLookupTable[i]) {
                        byteLookupTable[i] = value;
                    } else {
                        byteLookupTable[i] = nullValueByte;
                    }
                }
            }

        } else {
            hasNoData = false;
        }

        // Check if ROI control must be done
        if (srcROI != null) {
            hasROI = true;
            // Roi object
            roi = srcROI;
        } else {
            hasROI = false;
            roi = null;
        }

        // Definition of the possible cases that can be found
        // caseA = no ROI nor No Data
        // caseB = ROI present but No Data not present
        // caseC = No Data present but ROI not present
        // Last case not defined = both ROI and No Data are present
        caseA = !hasNoData && !hasROI;
        caseB = !hasNoData && hasROI;
        caseC = hasNoData && !hasROI;

        // Set flag to permit in-place operation.
        permitInPlaceOperation();
    }

    /**
     * Adds the pixel values of two source images within a specified rectangle.
     * 
     * @param sources Cobbled sources, guaranteed to provide all the source data necessary for computing the rectangle.
     * @param dest The tile containing the rectangle to be computed.
     * @param destRect The rectangle within the tile to be computed.
     */
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {

        if(!hasROI || (hasROI && roi.intersects(destRect))){
            // Retrieve format tags.
            RasterFormatTag[] formatTags = getFormatTags();

            RasterAccessor[] rasterArray = new RasterAccessor[numSrc];

            for (int i = 0; i < numSrc; i++) {
                rasterArray[i] = new RasterAccessor(sources[i], destRect, formatTags[i],
                        getSourceImage(i).getColorModel());
            }

            RasterAccessor d = new RasterAccessor(dest, destRect, formatTags[numSrc], getColorModel());

            switch (d.getDataType()) {
            case DataBuffer.TYPE_BYTE:
                computeRectByte(rasterArray, d);
                break;
            case DataBuffer.TYPE_USHORT:
                computeRectUShort(rasterArray, d);
                break;
            case DataBuffer.TYPE_SHORT:
                computeRectShort(rasterArray, d);
                break;
            case DataBuffer.TYPE_INT:
                computeRectInt(rasterArray, d);
                break;
            case DataBuffer.TYPE_FLOAT:
                computeRectFloat(rasterArray, d);
                break;
            case DataBuffer.TYPE_DOUBLE:
                computeRectDouble(rasterArray, d);
                break;
            }

            if (d.needsClamping()) {
                d.clampDataArrays();
            }
            d.copyDataToRaster();
        }else{
            int numBands = dest.getNumBands();
            int destDataType = dest.getSampleModel().getDataType();
            double[] destNoData = new double[numBands];
            for(int i = 0; i < numBands; i++){
                switch (destDataType) {
                case DataBuffer.TYPE_BYTE:
                    destNoData[i] = destNoDataByte;
                    break;
                case DataBuffer.TYPE_USHORT:
                case DataBuffer.TYPE_SHORT:
                    destNoData[i] = destNoDataShort;
                    break;
                case DataBuffer.TYPE_INT:
                    destNoData[i] = destNoDataInt;
                    break;
                case DataBuffer.TYPE_FLOAT:
                    destNoData[i] = destNoDataFloat;
                    break;
                case DataBuffer.TYPE_DOUBLE:
                    destNoData[i] = destNoDataDouble;
                    break;
                }               
            }
            ImageUtil.fillBackground(dest, destRect, destNoData);
        }

    }

    private void computeRectByte(RasterAccessor[] rasterArray, RasterAccessor dst) {

        int[] srcLineStride = new int[numSrc];
        int[] srcPixelStride = new int[numSrc];
        int[][] srcBandOffsets = new int[numSrc][];

        int[] srcLineOffset = new int[numSrc];
        int[] srcPixelOffset = new int[numSrc];

        for (int i = 0; i < numSrc; i++) {
            srcLineStride[i] = rasterArray[i].getScanlineStride();
            srcPixelStride[i] = rasterArray[i].getPixelStride();
            srcBandOffsets[i] = rasterArray[i].getBandOffsets();

        }

        int result = 0;
        int[] inputData = new int[numSrc];

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int bands = dst.getNumBands();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        byte[][] dData = dst.getByteDataArrays();

        byte[][] srcData = new byte[numSrc][];

        byte[] d;

        int x0 = 0;
        int y0 = 0;

        int srcX = rasterArray[0].getX();
        int srcY = rasterArray[0].getY();

        if (caseA) {
            for (int b = 0; b < bands; b++) {
                for (int i = 0; i < numSrc; i++) {
                    srcData[i] = rasterArray[i].getByteDataArray(b);
                    srcLineOffset[i] = srcBandOffsets[i][b];
                }
                d = dData[b];

                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    for (int i = 0; i < numSrc; i++) {
                        srcPixelOffset[i] = srcLineOffset[i];
                        srcLineOffset[i] += srcLineStride[i];
                    }

                    int dPixelOffset = dLineOffset;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {

                        for (int i = 0; i < numSrc; i++) {
                            inputData[i] = srcData[i][srcPixelOffset[i]] & 0xFF;
                            srcPixelOffset[i] += srcPixelStride[i];
                        }
                        //
                        // The next two lines are a fast way to do
                        // an operation with saturation on U8 elements.
                        // It eliminates the need to do clamping.
                        //
                        result = op.calculate(inputData);

                        d[dPixelOffset] = (byte) ((((result << 23) >> 31) | result) & 0xFF);

                        dPixelOffset += dPixelStride;
                    }
                }
            }
        } else if (caseB) {
            for (int b = 0; b < bands; b++) {
                for (int i = 0; i < numSrc; i++) {
                    srcData[i] = rasterArray[i].getByteDataArray(b);
                    srcLineOffset[i] = srcBandOffsets[i][b];
                }
                d = dData[b];

                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    for (int i = 0; i < numSrc; i++) {
                        srcPixelOffset[i] = srcLineOffset[i];
                        srcLineOffset[i] += srcLineStride[i];
                    }

                    int dPixelOffset = dLineOffset;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {

                        for (int i = 0; i < numSrc; i++) {
                            inputData[i] = srcData[i][srcPixelOffset[i]] & 0xFF;
                            srcPixelOffset[i] += srcPixelStride[i];
                        }

                        x0 = srcX + w;
                        y0 = srcY + h;

                        if (!roi.contains(x0, y0)) {
                            d[dPixelOffset] = destNoDataByte;
                            dPixelOffset += dPixelStride;
                            continue;
                        }

                        //
                        // The next two lines are a fast way to do
                        // an operation with saturation on U8 elements.
                        // It eliminates the need to do clamping.
                        //
                        result = op.calculate(inputData);

                        d[dPixelOffset] = (byte) ((((result << 23) >> 31) | result) & 0xFF);

                        dPixelOffset += dPixelStride;
                    }
                }
            }
        } else if (caseC) {
            int sourceValue = 0;
            boolean isValidData = false;
            for (int b = 0; b < bands; b++) {
                for (int i = 0; i < numSrc; i++) {
                    srcData[i] = rasterArray[i].getByteDataArray(b);
                    srcLineOffset[i] = srcBandOffsets[i][b];
                }
                d = dData[b];

                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    for (int i = 0; i < numSrc; i++) {
                        srcPixelOffset[i] = srcLineOffset[i];
                        srcLineOffset[i] += srcLineStride[i];
                    }

                    int dPixelOffset = dLineOffset;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {

                        isValidData = false;

                        for (int i = 0; i < numSrc; i++) {
                            sourceValue = srcData[i][srcPixelOffset[i]] & 0xFF;

                            isValidData |= booleanLookupTable[sourceValue];

                            inputData[i] = byteLookupTable[sourceValue] & 0xFF;

                            srcPixelOffset[i] += srcPixelStride[i];
                        }

                        if (!isValidData) {
                            d[dPixelOffset] = destNoDataByte;
                            dPixelOffset += dPixelStride;
                            continue;
                        }

                        //
                        // The next two lines are a fast way to do
                        // an operation with saturation on U8 elements.
                        // It eliminates the need to do clamping.
                        //
                        result = op.calculate(inputData);

                        d[dPixelOffset] = (byte) ((((result << 23) >> 31) | result) & 0xFF);

                        dPixelOffset += dPixelStride;
                    }
                }
            }
        } else {
            int sourceValue = 0;
            boolean isValidData = false;
            for (int b = 0; b < bands; b++) {
                for (int i = 0; i < numSrc; i++) {
                    srcData[i] = rasterArray[i].getByteDataArray(b);
                    srcLineOffset[i] = srcBandOffsets[i][b];
                }
                d = dData[b];

                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    for (int i = 0; i < numSrc; i++) {
                        srcPixelOffset[i] = srcLineOffset[i];
                        srcLineOffset[i] += srcLineStride[i];
                    }

                    int dPixelOffset = dLineOffset;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {

                        isValidData = false;

                        x0 = srcX + w;
                        y0 = srcY + h;

                        if (!roi.contains(x0, y0)) {
                            for (int i = 0; i < numSrc; i++) {
                                srcPixelOffset[i] += srcPixelStride[i];
                            }
                            d[dPixelOffset] = destNoDataByte;
                            dPixelOffset += dPixelStride;
                            continue;
                        }

                        for (int i = 0; i < numSrc; i++) {
                            sourceValue = srcData[i][srcPixelOffset[i]] & 0xFF;

                            isValidData |= booleanLookupTable[sourceValue];

                            inputData[i] = byteLookupTable[sourceValue] & 0xFF;

                            srcPixelOffset[i] += srcPixelStride[i];
                        }

                        if (!isValidData) {
                            d[dPixelOffset] = destNoDataByte;
                            dPixelOffset += dPixelStride;
                            continue;
                        }

                        //
                        // The next two lines are a fast way to do
                        // an operation with saturation on U8 elements.
                        // It eliminates the need to do clamping.
                        //
                        result = op.calculate(inputData);

                        d[dPixelOffset] = (byte) ((((result << 23) >> 31) | result) & 0xFF);

                        dPixelOffset += dPixelStride;
                    }
                }
            }
        }
    }

    private void computeRectUShort(RasterAccessor[] rasterArray, RasterAccessor dst) {

        int[] srcLineStride = new int[numSrc];
        int[] srcPixelStride = new int[numSrc];
        int[][] srcBandOffsets = new int[numSrc][];

        int[] srcLineOffset = new int[numSrc];
        int[] srcPixelOffset = new int[numSrc];

        for (int i = 0; i < numSrc; i++) {
            srcLineStride[i] = rasterArray[i].getScanlineStride();
            srcPixelStride[i] = rasterArray[i].getPixelStride();
            srcBandOffsets[i] = rasterArray[i].getBandOffsets();

        }

        int result = 0;
        int[] inputData = new int[numSrc];

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int bands = dst.getNumBands();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        short[][] dData = dst.getShortDataArrays();

        short[][] srcData = new short[numSrc][];

        short[] d;

        int x0 = 0;
        int y0 = 0;

        int srcX = rasterArray[0].getX();
        int srcY = rasterArray[0].getY();

        short sourceValue = 0;
        boolean isValidData = false;

        if (caseA) {
            for (int b = 0; b < bands; b++) {
                for (int i = 0; i < numSrc; i++) {
                    srcData[i] = rasterArray[i].getShortDataArray(b);
                    srcLineOffset[i] = srcBandOffsets[i][b];
                }
                d = dData[b];

                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    for (int i = 0; i < numSrc; i++) {
                        srcPixelOffset[i] = srcLineOffset[i];
                        srcLineOffset[i] += srcLineStride[i];
                    }

                    int dPixelOffset = dLineOffset;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {

                        for (int i = 0; i < numSrc; i++) {
                            inputData[i] = srcData[i][srcPixelOffset[i]] & 0xFFFF;
                            srcPixelOffset[i] += srcPixelStride[i];
                        }
                        result = op.calculate(inputData);

                        d[dPixelOffset] = ImageUtil.clampUShort(result);

                        dPixelOffset += dPixelStride;
                    }
                }
            }
        } else if (caseB) {
            for (int b = 0; b < bands; b++) {
                for (int i = 0; i < numSrc; i++) {
                    srcData[i] = rasterArray[i].getShortDataArray(b);
                    srcLineOffset[i] = srcBandOffsets[i][b];
                }
                d = dData[b];

                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    for (int i = 0; i < numSrc; i++) {
                        srcPixelOffset[i] = srcLineOffset[i];
                        srcLineOffset[i] += srcLineStride[i];
                    }

                    int dPixelOffset = dLineOffset;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {

                        for (int i = 0; i < numSrc; i++) {
                            inputData[i] = srcData[i][srcPixelOffset[i]] & 0xFFFF;
                            srcPixelOffset[i] += srcPixelStride[i];
                        }

                        x0 = srcX + w;
                        y0 = srcY + h;

                        if (!roi.contains(x0, y0)) {
                            d[dPixelOffset] = destNoDataShort;
                            dPixelOffset += dPixelStride;
                            continue;
                        }
                        result = op.calculate(inputData);

                        d[dPixelOffset] = ImageUtil.clampUShort(result);

                        dPixelOffset += dPixelStride;
                    }
                }
            }
        } else if (caseC) {
            for (int b = 0; b < bands; b++) {
                for (int i = 0; i < numSrc; i++) {
                    srcData[i] = rasterArray[i].getShortDataArray(b);
                    srcLineOffset[i] = srcBandOffsets[i][b];
                }
                d = dData[b];

                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    for (int i = 0; i < numSrc; i++) {
                        srcPixelOffset[i] = srcLineOffset[i];
                        srcLineOffset[i] += srcLineStride[i];
                    }

                    int dPixelOffset = dLineOffset;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {

                        isValidData = false;

                        for (int i = 0; i < numSrc; i++) {
                            sourceValue = srcData[i][srcPixelOffset[i]];

                            if (!noData.contains(sourceValue)) {
                                inputData[i] = sourceValue & 0xFFFF;
                                isValidData = true;
                            } else {
                                inputData[i] = nullValueShort;
                            }

                            srcPixelOffset[i] += srcPixelStride[i];
                        }

                        if (!isValidData) {
                            d[dPixelOffset] = destNoDataShort;
                            dPixelOffset += dPixelStride;
                            continue;
                        }
                        result = op.calculate(inputData);

                        d[dPixelOffset] = ImageUtil.clampUShort(result);

                        dPixelOffset += dPixelStride;
                    }
                }
            }
        } else {
            for (int b = 0; b < bands; b++) {
                for (int i = 0; i < numSrc; i++) {
                    srcData[i] = rasterArray[i].getShortDataArray(b);
                    srcLineOffset[i] = srcBandOffsets[i][b];
                }
                d = dData[b];

                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    for (int i = 0; i < numSrc; i++) {
                        srcPixelOffset[i] = srcLineOffset[i];
                        srcLineOffset[i] += srcLineStride[i];
                    }

                    int dPixelOffset = dLineOffset;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {

                        isValidData = false;

                        x0 = srcX + w;
                        y0 = srcY + h;

                        if (!roi.contains(x0, y0)) {
                            for (int i = 0; i < numSrc; i++) {
                                srcPixelOffset[i] += srcPixelStride[i];
                            }
                            d[dPixelOffset] = destNoDataShort;
                            dPixelOffset += dPixelStride;
                            continue;
                        }

                        for (int i = 0; i < numSrc; i++) {
                            sourceValue = srcData[i][srcPixelOffset[i]];

                            if (!noData.contains(sourceValue)) {
                                inputData[i] = sourceValue & 0xFFFF;
                                isValidData = true;
                            } else {
                                inputData[i] = nullValueShort;
                            }

                            srcPixelOffset[i] += srcPixelStride[i];
                        }

                        if (!isValidData) {
                            d[dPixelOffset] = destNoDataShort;
                            dPixelOffset += dPixelStride;
                            continue;
                        }
                        result = op.calculate(inputData);

                        d[dPixelOffset] = ImageUtil.clampUShort(result);

                        dPixelOffset += dPixelStride;
                    }
                }
            }
        }
    }

    private void computeRectShort(RasterAccessor[] rasterArray, RasterAccessor dst) {

        int[] srcLineStride = new int[numSrc];
        int[] srcPixelStride = new int[numSrc];
        int[][] srcBandOffsets = new int[numSrc][];

        int[] srcLineOffset = new int[numSrc];
        int[] srcPixelOffset = new int[numSrc];

        for (int i = 0; i < numSrc; i++) {
            srcLineStride[i] = rasterArray[i].getScanlineStride();
            srcPixelStride[i] = rasterArray[i].getPixelStride();
            srcBandOffsets[i] = rasterArray[i].getBandOffsets();

        }

        int result = 0;
        int[] inputData = new int[numSrc];

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int bands = dst.getNumBands();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        short[][] dData = dst.getShortDataArrays();

        short[][] srcData = new short[numSrc][];

        short[] d;

        int x0 = 0;
        int y0 = 0;

        int srcX = rasterArray[0].getX();
        int srcY = rasterArray[0].getY();

        short sourceValue = 0;
        boolean isValidData = false;

        if (caseA) {
            for (int b = 0; b < bands; b++) {
                for (int i = 0; i < numSrc; i++) {
                    srcData[i] = rasterArray[i].getShortDataArray(b);
                    srcLineOffset[i] = srcBandOffsets[i][b];
                }
                d = dData[b];

                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    for (int i = 0; i < numSrc; i++) {
                        srcPixelOffset[i] = srcLineOffset[i];
                        srcLineOffset[i] += srcLineStride[i];
                    }

                    int dPixelOffset = dLineOffset;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {

                        for (int i = 0; i < numSrc; i++) {
                            inputData[i] = srcData[i][srcPixelOffset[i]];
                            srcPixelOffset[i] += srcPixelStride[i];
                        }
                        result = op.calculate(inputData);

                        d[dPixelOffset] = ImageUtil.clampShort(result);

                        dPixelOffset += dPixelStride;
                    }
                }
            }
        } else if (caseB) {
            for (int b = 0; b < bands; b++) {
                for (int i = 0; i < numSrc; i++) {
                    srcData[i] = rasterArray[i].getShortDataArray(b);
                    srcLineOffset[i] = srcBandOffsets[i][b];
                }
                d = dData[b];

                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    for (int i = 0; i < numSrc; i++) {
                        srcPixelOffset[i] = srcLineOffset[i];
                        srcLineOffset[i] += srcLineStride[i];
                    }

                    int dPixelOffset = dLineOffset;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {

                        for (int i = 0; i < numSrc; i++) {
                            inputData[i] = srcData[i][srcPixelOffset[i]];
                            srcPixelOffset[i] += srcPixelStride[i];
                        }

                        x0 = srcX + w;
                        y0 = srcY + h;

                        if (!roi.contains(x0, y0)) {
                            d[dPixelOffset] = destNoDataShort;
                            dPixelOffset += dPixelStride;
                            continue;
                        }
                        result = op.calculate(inputData);

                        d[dPixelOffset] = ImageUtil.clampShort(result);

                        dPixelOffset += dPixelStride;
                    }
                }
            }
        } else if (caseC) {
            for (int b = 0; b < bands; b++) {
                for (int i = 0; i < numSrc; i++) {
                    srcData[i] = rasterArray[i].getShortDataArray(b);
                    srcLineOffset[i] = srcBandOffsets[i][b];
                }
                d = dData[b];

                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    for (int i = 0; i < numSrc; i++) {
                        srcPixelOffset[i] = srcLineOffset[i];
                        srcLineOffset[i] += srcLineStride[i];
                    }

                    int dPixelOffset = dLineOffset;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {

                        isValidData = false;

                        for (int i = 0; i < numSrc; i++) {
                            sourceValue = srcData[i][srcPixelOffset[i]];

                            if (!noData.contains(sourceValue)) {
                                inputData[i] = sourceValue;
                                isValidData = true;
                            } else {
                                inputData[i] = nullValueShort;
                            }

                            srcPixelOffset[i] += srcPixelStride[i];
                        }

                        if (!isValidData) {
                            d[dPixelOffset] = destNoDataShort;
                            dPixelOffset += dPixelStride;
                            continue;
                        }
                        result = op.calculate(inputData);

                        d[dPixelOffset] = ImageUtil.clampShort(result);

                        dPixelOffset += dPixelStride;
                    }
                }
            }
        } else {
            for (int b = 0; b < bands; b++) {
                for (int i = 0; i < numSrc; i++) {
                    srcData[i] = rasterArray[i].getShortDataArray(b);
                    srcLineOffset[i] = srcBandOffsets[i][b];
                }
                d = dData[b];

                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    for (int i = 0; i < numSrc; i++) {
                        srcPixelOffset[i] = srcLineOffset[i];
                        srcLineOffset[i] += srcLineStride[i];
                    }

                    int dPixelOffset = dLineOffset;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {

                        isValidData = false;

                        x0 = srcX + w;
                        y0 = srcY + h;

                        if (!roi.contains(x0, y0)) {
                            for (int i = 0; i < numSrc; i++) {
                                srcPixelOffset[i] += srcPixelStride[i];
                            }
                            d[dPixelOffset] = destNoDataShort;
                            dPixelOffset += dPixelStride;
                            continue;
                        }

                        for (int i = 0; i < numSrc; i++) {
                            sourceValue = srcData[i][srcPixelOffset[i]];

                            if (!noData.contains(sourceValue)) {
                                inputData[i] = sourceValue;
                                isValidData = true;
                            } else {
                                inputData[i] = nullValueShort;
                            }

                            srcPixelOffset[i] += srcPixelStride[i];
                        }

                        if (!isValidData) {
                            d[dPixelOffset] = destNoDataShort;
                            dPixelOffset += dPixelStride;
                            continue;
                        }
                        result = op.calculate(inputData);

                        d[dPixelOffset] = ImageUtil.clampShort(result);

                        dPixelOffset += dPixelStride;
                    }
                }
            }
        }
    }

    private void computeRectInt(RasterAccessor[] rasterArray, RasterAccessor dst) {

        int[] srcLineStride = new int[numSrc];
        int[] srcPixelStride = new int[numSrc];
        int[][] srcBandOffsets = new int[numSrc][];

        int[] srcLineOffset = new int[numSrc];
        int[] srcPixelOffset = new int[numSrc];

        for (int i = 0; i < numSrc; i++) {
            srcLineStride[i] = rasterArray[i].getScanlineStride();
            srcPixelStride[i] = rasterArray[i].getPixelStride();
            srcBandOffsets[i] = rasterArray[i].getBandOffsets();

        }

        long[] inputData = new long[numSrc];

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int bands = dst.getNumBands();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        int[][] dData = dst.getIntDataArrays();

        long result = 0;
        int[][] srcData = new int[numSrc][];

        int[] d;

        int x0 = 0;
        int y0 = 0;

        int srcX = rasterArray[0].getX();
        int srcY = rasterArray[0].getY();

        int sourceValue = 0;
        boolean isValidData = false;

        if (caseA) {
            for (int b = 0; b < bands; b++) {
                for (int i = 0; i < numSrc; i++) {
                    srcData[i] = rasterArray[i].getIntDataArray(b);
                    srcLineOffset[i] = srcBandOffsets[i][b];
                }
                d = dData[b];

                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    for (int i = 0; i < numSrc; i++) {
                        srcPixelOffset[i] = srcLineOffset[i];
                        srcLineOffset[i] += srcLineStride[i];
                    }

                    int dPixelOffset = dLineOffset;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {

                        for (int i = 0; i < numSrc; i++) {
                            inputData[i] = srcData[i][srcPixelOffset[i]];
                            srcPixelOffset[i] += srcPixelStride[i];
                        }
                        result = op.calculate(inputData);

                        d[dPixelOffset] = ImageUtil.clampInt(result);

                        dPixelOffset += dPixelStride;
                    }
                }
            }
        } else if (caseB) {
            for (int b = 0; b < bands; b++) {
                for (int i = 0; i < numSrc; i++) {
                    srcData[i] = rasterArray[i].getIntDataArray(b);
                    srcLineOffset[i] = srcBandOffsets[i][b];
                }
                d = dData[b];

                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    for (int i = 0; i < numSrc; i++) {
                        srcPixelOffset[i] = srcLineOffset[i];
                        srcLineOffset[i] += srcLineStride[i];
                    }

                    int dPixelOffset = dLineOffset;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {

                        for (int i = 0; i < numSrc; i++) {
                            inputData[i] = srcData[i][srcPixelOffset[i]];
                            srcPixelOffset[i] += srcPixelStride[i];
                        }

                        x0 = srcX + w;
                        y0 = srcY + h;

                        if (!roi.contains(x0, y0)) {
                            d[dPixelOffset] = destNoDataInt;
                            dPixelOffset += dPixelStride;
                            continue;
                        }
                        result = op.calculate(inputData);

                        d[dPixelOffset] = ImageUtil.clampInt(result);

                        dPixelOffset += dPixelStride;
                    }
                }
            }
        } else if (caseC) {
            for (int b = 0; b < bands; b++) {
                for (int i = 0; i < numSrc; i++) {
                    srcData[i] = rasterArray[i].getIntDataArray(b);
                    srcLineOffset[i] = srcBandOffsets[i][b];
                }
                d = dData[b];

                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    for (int i = 0; i < numSrc; i++) {
                        srcPixelOffset[i] = srcLineOffset[i];
                        srcLineOffset[i] += srcLineStride[i];
                    }

                    int dPixelOffset = dLineOffset;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {

                        isValidData = false;

                        for (int i = 0; i < numSrc; i++) {
                            sourceValue = srcData[i][srcPixelOffset[i]];

                            if (!noData.contains(sourceValue)) {
                                inputData[i] = sourceValue;
                                isValidData = true;
                            } else {
                                inputData[i] = nullValueInt;
                            }

                            srcPixelOffset[i] += srcPixelStride[i];
                        }

                        if (!isValidData) {
                            d[dPixelOffset] = destNoDataInt;
                            dPixelOffset += dPixelStride;
                            continue;
                        }
                        result = op.calculate(inputData);

                        d[dPixelOffset] = ImageUtil.clampInt(result);

                        dPixelOffset += dPixelStride;
                    }
                }
            }
        } else {
            for (int b = 0; b < bands; b++) {
                for (int i = 0; i < numSrc; i++) {
                    srcData[i] = rasterArray[i].getIntDataArray(b);
                    srcLineOffset[i] = srcBandOffsets[i][b];
                }
                d = dData[b];

                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    for (int i = 0; i < numSrc; i++) {
                        srcPixelOffset[i] = srcLineOffset[i];
                        srcLineOffset[i] += srcLineStride[i];
                    }

                    int dPixelOffset = dLineOffset;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {

                        isValidData = false;

                        x0 = srcX + w;
                        y0 = srcY + h;

                        if (!roi.contains(x0, y0)) {
                            for (int i = 0; i < numSrc; i++) {
                                srcPixelOffset[i] += srcPixelStride[i];
                            }
                            d[dPixelOffset] = destNoDataInt;
                            dPixelOffset += dPixelStride;
                            continue;
                        }

                        for (int i = 0; i < numSrc; i++) {
                            sourceValue = srcData[i][srcPixelOffset[i]];

                            if (!noData.contains(sourceValue)) {
                                inputData[i] = sourceValue;
                                isValidData = true;
                            } else {
                                inputData[i] = nullValueInt;
                            }

                            srcPixelOffset[i] += srcPixelStride[i];
                        }

                        if (!isValidData) {
                            d[dPixelOffset] = destNoDataInt;
                            dPixelOffset += dPixelStride;
                            continue;
                        }
                        result = op.calculate(inputData);

                        d[dPixelOffset] = ImageUtil.clampInt(result);

                        dPixelOffset += dPixelStride;
                    }
                }
            }
        }
    }

    private void computeRectFloat(RasterAccessor[] rasterArray, RasterAccessor dst) {

        int[] srcLineStride = new int[numSrc];
        int[] srcPixelStride = new int[numSrc];
        int[][] srcBandOffsets = new int[numSrc][];

        int[] srcLineOffset = new int[numSrc];
        int[] srcPixelOffset = new int[numSrc];

        for (int i = 0; i < numSrc; i++) {
            srcLineStride[i] = rasterArray[i].getScanlineStride();
            srcPixelStride[i] = rasterArray[i].getPixelStride();
            srcBandOffsets[i] = rasterArray[i].getBandOffsets();

        }

        float[] inputData = new float[numSrc];

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int bands = dst.getNumBands();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        float[][] dData = dst.getFloatDataArrays();

        float[][] srcData = new float[numSrc][];

        float[] d;

        int x0 = 0;
        int y0 = 0;

        int srcX = rasterArray[0].getX();
        int srcY = rasterArray[0].getY();

        float sourceValue = 0;
        boolean isValidData = false;

        if (caseA) {
            for (int b = 0; b < bands; b++) {
                for (int i = 0; i < numSrc; i++) {
                    srcData[i] = rasterArray[i].getFloatDataArray(b);
                    srcLineOffset[i] = srcBandOffsets[i][b];
                }
                d = dData[b];

                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    for (int i = 0; i < numSrc; i++) {
                        srcPixelOffset[i] = srcLineOffset[i];
                        srcLineOffset[i] += srcLineStride[i];
                    }

                    int dPixelOffset = dLineOffset;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {

                        for (int i = 0; i < numSrc; i++) {
                            inputData[i] = srcData[i][srcPixelOffset[i]];
                            srcPixelOffset[i] += srcPixelStride[i];
                        }
                        d[dPixelOffset] = op.calculate(inputData);

                        dPixelOffset += dPixelStride;
                    }
                }
            }
        } else if (caseB) {
            for (int b = 0; b < bands; b++) {
                for (int i = 0; i < numSrc; i++) {
                    srcData[i] = rasterArray[i].getFloatDataArray(b);
                    srcLineOffset[i] = srcBandOffsets[i][b];
                }
                d = dData[b];

                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    for (int i = 0; i < numSrc; i++) {
                        srcPixelOffset[i] = srcLineOffset[i];
                        srcLineOffset[i] += srcLineStride[i];
                    }

                    int dPixelOffset = dLineOffset;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {

                        for (int i = 0; i < numSrc; i++) {
                            inputData[i] = srcData[i][srcPixelOffset[i]];
                            srcPixelOffset[i] += srcPixelStride[i];
                        }

                        x0 = srcX + w;
                        y0 = srcY + h;

                        if (!roi.contains(x0, y0)) {
                            d[dPixelOffset] = destNoDataFloat;
                            dPixelOffset += dPixelStride;
                            continue;
                        }
                        d[dPixelOffset] = op.calculate(inputData);

                        dPixelOffset += dPixelStride;
                    }
                }
            }
        } else if (caseC) {
            for (int b = 0; b < bands; b++) {
                for (int i = 0; i < numSrc; i++) {
                    srcData[i] = rasterArray[i].getFloatDataArray(b);
                    srcLineOffset[i] = srcBandOffsets[i][b];
                }
                d = dData[b];

                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    for (int i = 0; i < numSrc; i++) {
                        srcPixelOffset[i] = srcLineOffset[i];
                        srcLineOffset[i] += srcLineStride[i];
                    }

                    int dPixelOffset = dLineOffset;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {

                        isValidData = false;

                        for (int i = 0; i < numSrc; i++) {
                            sourceValue = srcData[i][srcPixelOffset[i]];

                            if (!noData.contains(sourceValue)) {
                                inputData[i] = sourceValue;
                                isValidData = true;
                            } else {
                                inputData[i] = nullValueFloat;
                            }

                            srcPixelOffset[i] += srcPixelStride[i];
                        }

                        if (!isValidData) {
                            d[dPixelOffset] = destNoDataFloat;
                            dPixelOffset += dPixelStride;
                            continue;
                        }
                        d[dPixelOffset] = op.calculate(inputData);

                        dPixelOffset += dPixelStride;
                    }
                }
            }
        } else {
            for (int b = 0; b < bands; b++) {
                for (int i = 0; i < numSrc; i++) {
                    srcData[i] = rasterArray[i].getFloatDataArray(b);
                    srcLineOffset[i] = srcBandOffsets[i][b];
                }
                d = dData[b];

                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    for (int i = 0; i < numSrc; i++) {
                        srcPixelOffset[i] = srcLineOffset[i];
                        srcLineOffset[i] += srcLineStride[i];
                    }

                    int dPixelOffset = dLineOffset;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {

                        isValidData = false;

                        x0 = srcX + w;
                        y0 = srcY + h;

                        if (!roi.contains(x0, y0)) {
                            for (int i = 0; i < numSrc; i++) {
                                srcPixelOffset[i] += srcPixelStride[i];
                            }
                            d[dPixelOffset] = destNoDataFloat;
                            dPixelOffset += dPixelStride;
                            continue;
                        }

                        for (int i = 0; i < numSrc; i++) {
                            sourceValue = srcData[i][srcPixelOffset[i]];

                            if (!noData.contains(sourceValue)) {
                                inputData[i] = sourceValue;
                                isValidData = true;
                            } else {
                                inputData[i] = nullValueFloat;
                            }

                            srcPixelOffset[i] += srcPixelStride[i];
                        }

                        if (!isValidData) {
                            d[dPixelOffset] = destNoDataFloat;
                            dPixelOffset += dPixelStride;
                            continue;
                        }
                        d[dPixelOffset] = op.calculate(inputData);

                        dPixelOffset += dPixelStride;
                    }
                }
            }
        }
    }

    private void computeRectDouble(RasterAccessor[] rasterArray, RasterAccessor dst) {

        int[] srcLineStride = new int[numSrc];
        int[] srcPixelStride = new int[numSrc];
        int[][] srcBandOffsets = new int[numSrc][];

        int[] srcLineOffset = new int[numSrc];
        int[] srcPixelOffset = new int[numSrc];

        for (int i = 0; i < numSrc; i++) {
            srcLineStride[i] = rasterArray[i].getScanlineStride();
            srcPixelStride[i] = rasterArray[i].getPixelStride();
            srcBandOffsets[i] = rasterArray[i].getBandOffsets();

        }

        double[] inputData = new double[numSrc];

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int bands = dst.getNumBands();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        double[][] dData = dst.getDoubleDataArrays();

        double[][] srcData = new double[numSrc][];

        double[] d;

        int x0 = 0;
        int y0 = 0;

        int srcX = rasterArray[0].getX();
        int srcY = rasterArray[0].getY();

        double sourceValue = 0;
        boolean isValidData = false;

        if (caseA) {
            for (int b = 0; b < bands; b++) {
                for (int i = 0; i < numSrc; i++) {
                    srcData[i] = rasterArray[i].getDoubleDataArray(b);
                    srcLineOffset[i] = srcBandOffsets[i][b];
                }
                d = dData[b];

                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    for (int i = 0; i < numSrc; i++) {
                        srcPixelOffset[i] = srcLineOffset[i];
                        srcLineOffset[i] += srcLineStride[i];
                    }

                    int dPixelOffset = dLineOffset;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {

                        for (int i = 0; i < numSrc; i++) {
                            inputData[i] = srcData[i][srcPixelOffset[i]];
                            srcPixelOffset[i] += srcPixelStride[i];
                        }
                        d[dPixelOffset] = op.calculate(inputData);

                        dPixelOffset += dPixelStride;
                    }
                }
            }
        } else if (caseB) {
            for (int b = 0; b < bands; b++) {
                for (int i = 0; i < numSrc; i++) {
                    srcData[i] = rasterArray[i].getDoubleDataArray(b);
                    srcLineOffset[i] = srcBandOffsets[i][b];
                }
                d = dData[b];

                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    for (int i = 0; i < numSrc; i++) {
                        srcPixelOffset[i] = srcLineOffset[i];
                        srcLineOffset[i] += srcLineStride[i];
                    }

                    int dPixelOffset = dLineOffset;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {

                        for (int i = 0; i < numSrc; i++) {
                            inputData[i] = srcData[i][srcPixelOffset[i]];
                            srcPixelOffset[i] += srcPixelStride[i];
                        }

                        x0 = srcX + w;
                        y0 = srcY + h;

                        if (!roi.contains(x0, y0)) {
                            d[dPixelOffset] = destNoDataDouble;
                            dPixelOffset += dPixelStride;
                            continue;
                        }
                        d[dPixelOffset] = op.calculate(inputData);

                        dPixelOffset += dPixelStride;
                    }
                }
            }
        } else if (caseC) {
            for (int b = 0; b < bands; b++) {
                for (int i = 0; i < numSrc; i++) {
                    srcData[i] = rasterArray[i].getDoubleDataArray(b);
                    srcLineOffset[i] = srcBandOffsets[i][b];
                }
                d = dData[b];

                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    for (int i = 0; i < numSrc; i++) {
                        srcPixelOffset[i] = srcLineOffset[i];
                        srcLineOffset[i] += srcLineStride[i];
                    }

                    int dPixelOffset = dLineOffset;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {

                        isValidData = false;

                        for (int i = 0; i < numSrc; i++) {
                            sourceValue = srcData[i][srcPixelOffset[i]];

                            if (!noData.contains(sourceValue)) {
                                inputData[i] = sourceValue;
                                isValidData = true;
                            } else {
                                inputData[i] = nullValueDouble;
                            }

                            srcPixelOffset[i] += srcPixelStride[i];
                        }

                        if (!isValidData) {
                            d[dPixelOffset] = destNoDataDouble;
                            dPixelOffset += dPixelStride;
                            continue;
                        }
                        d[dPixelOffset] = op.calculate(inputData);

                        dPixelOffset += dPixelStride;
                    }
                }
            }
        } else {
            for (int b = 0; b < bands; b++) {
                for (int i = 0; i < numSrc; i++) {
                    srcData[i] = rasterArray[i].getDoubleDataArray(b);
                    srcLineOffset[i] = srcBandOffsets[i][b];
                }
                d = dData[b];

                int dLineOffset = dBandOffsets[b];

                for (int h = 0; h < dheight; h++) {
                    for (int i = 0; i < numSrc; i++) {
                        srcPixelOffset[i] = srcLineOffset[i];
                        srcLineOffset[i] += srcLineStride[i];
                    }

                    int dPixelOffset = dLineOffset;
                    dLineOffset += dLineStride;

                    for (int w = 0; w < dwidth; w++) {

                        isValidData = false;

                        x0 = srcX + w;
                        y0 = srcY + h;

                        if (!roi.contains(x0, y0)) {
                            for (int i = 0; i < numSrc; i++) {
                                srcPixelOffset[i] += srcPixelStride[i];
                            }
                            d[dPixelOffset] = destNoDataDouble;
                            dPixelOffset += dPixelStride;
                            continue;
                        }

                        for (int i = 0; i < numSrc; i++) {
                            sourceValue = srcData[i][srcPixelOffset[i]];

                            if (!noData.contains(sourceValue)) {
                                inputData[i] = sourceValue;
                                isValidData = true;
                            } else {
                                inputData[i] = nullValueDouble;
                            }

                            srcPixelOffset[i] += srcPixelStride[i];
                        }

                        if (!isValidData) {
                            d[dPixelOffset] = destNoDataDouble;
                            dPixelOffset += dPixelStride;
                            continue;
                        }
                        d[dPixelOffset] = op.calculate(inputData);

                        dPixelOffset += dPixelStride;
                    }
                }
            }
        }
    }

    private static Vector vectorize(RenderedImage[] sources) {

        Vector vec = new Vector(sources.length);

        for (RenderedImage image : sources) {
            if (image != null) {
                vec.add(image);
            }

        }

        if (vec.isEmpty()) {
            return null;
        }

        return vec;
    }
}
