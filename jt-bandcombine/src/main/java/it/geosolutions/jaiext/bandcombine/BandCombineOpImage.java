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
package it.geosolutions.jaiext.bandcombine;

import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.Map;

import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.PointOpImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFactory;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.iterator.RandomIter;

import com.sun.media.jai.util.ImageUtil;
import com.sun.media.jai.util.JDKWorkarounds;

/**
 * An <code>OpImage</code> implementing the "BandCombine" operation taking into account the presence of ROI and NoData.
 * 
 * <p>
 * This <code>OpImage</code> performs the arbitrary interband linear combination of an image using the specified matrix. The width of the matrix must
 * be one larger that the number of bands in the source image. The height of the matrix must be equal to the number of bands in the destination image.
 * Because the matrix can be of arbitrary size, this function can be used to produce a destination image with a different number of bands from the
 * source image.
 * <p>
 * The destination image is formed by performing a matrix- multiply operation between the bands of the source image and the specified matrix. The
 * extra column of values is a constant that is added after the matrix-multiply operation takes place.
 * 
 * If an input sample is outside ROI or it is a NoData, it will be skipped during computation.
 * 
 */
public class BandCombineOpImage extends PointOpImage {

    /** Constant indicating that the inner random iterators must pre-calculate an array of the image positions */
    public static final boolean ARRAY_CALC = true;

    /** Constant indicating that the inner random iterators must cache the current tile position */
    public static final boolean TILE_CACHED = true;

    /** Boolean indicating that NoData must be checked */
    private final boolean hasNoData;

    /** NoData Range element */
    private Range noData;

    /** LookupTable used for checking if an input byte sample is a NoData */
    private boolean[] lut;

    /** Boolean indicating that ROI must be checked */
    private final boolean hasROI;

    /** ROI element */
    private ROI roi;

    /** Boolean indicating that no roi and no data check must be done */
    private final boolean caseA;

    /** Boolean indicating that only roi check must be done */
    private final boolean caseB;

    /** Boolean indicating that only no data check must be done */
    private final boolean caseC;

    /** ROI bounds as a Shape */
    private final Rectangle roiBounds;

    /** ROI related image */
    private PlanarImage roiImage;

    /** Matrix used for doing band combination */
    private double[][] matrix;

    /** Destination No Data value for Byte sources */
    private byte destNoDataByte;

    /** Destination No Data value for Short sources */
    private short destNoDataShort;

    /** Destination No Data value for Integer sources */
    private int destNoDataInt;

    /** Destination No Data value for Float sources */
    private float destNoDataFloat;

    /** Destination No Data value for Double sources */
    private double destNoDataDouble;

    /**
     * Constructs a new instance of the {@link BandCombineOpImage}.
     * 
     * @param source The source image.
     * @param layout The destination image layout.
     * @param matrix The matrix of values used to perform the linear combination.
     * @param roi ROI object
     * @param destinationNoData
     * @param nodata No Data Range used for checking if a pixel is a NoData.
     * @param destinationNoData value for replacing the source nodata values
     */
    public BandCombineOpImage(RenderedImage source, Map config, ImageLayout layout,
            double[][] matrix, ROI roi, Range noData, double destinationNoData) {
        super(source, layout, config, true);
        // Setting matrix
        this.matrix = matrix;

        int numBands = matrix.length; // matrix height is dst numBands
        if (getSampleModel().getNumBands() != numBands) {
            sampleModel = RasterFactory.createComponentSampleModel(sampleModel,
                    sampleModel.getDataType(), tileWidth, tileHeight, numBands);

            if (colorModel != null
                    && !JDKWorkarounds.areCompatibleDataModels(sampleModel, colorModel)) {
                colorModel = ImageUtil.getCompatibleColorModel(sampleModel, config);
            }
        }
        // Check if ROI control must be done
        if (roi != null) {
            hasROI = true;
            // Roi object
            this.roi = roi;
            roiBounds = roi.getBounds();
        } else {
            hasROI = false;
            this.roi = null;
            roiBounds = null;
        }

        // Check if No Data control must be done
        if (noData != null) {
            hasNoData = true;
            this.noData = noData;
        } else {
            hasNoData = false;
        }

        // Getting datatype
        int dataType = source.getSampleModel().getDataType();

        // Destination No Data value is clamped to the image data type
        this.destNoDataDouble = destinationNoData;
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            this.destNoDataByte = ImageUtil.clampRoundByte(destinationNoData);
            break;
        case DataBuffer.TYPE_USHORT:
            this.destNoDataShort = ImageUtil.clampRoundUShort(destinationNoData);
            break;
        case DataBuffer.TYPE_SHORT:
            this.destNoDataShort = ImageUtil.clampRoundShort(destinationNoData);
            break;
        case DataBuffer.TYPE_INT:
            this.destNoDataInt = ImageUtil.clampRoundInt(destinationNoData);
            break;
        case DataBuffer.TYPE_FLOAT:
            this.destNoDataFloat = ImageUtil.clampFloat(destinationNoData);
            break;
        case DataBuffer.TYPE_DOUBLE:
            break;
        default:
            throw new IllegalArgumentException("Wrong image data type");
        }

        // Definition of the possible cases that can be found
        // caseA = no ROI nor No Data
        // caseB = ROI present but No Data not present
        // caseC = No Data present but ROI not present
        // Last case not defined = both ROI and No Data are present
        caseA = !hasNoData && !hasROI;
        caseB = !hasNoData && hasROI;
        caseC = hasNoData && !hasROI;

        if (hasNoData && dataType == DataBuffer.TYPE_BYTE) {
            initBooleanNoDataTable();
        }
    }

    /**
     * Performs linear combination of source image with matrix. If ROI and NoData are present they will be checked.
     * 
     * @param sources Cobbled sources, guaranteed to provide all the source data necessary for computing the rectangle.
     * @param dest The tile containing the rectangle to be computed.
     * @param destRect The rectangle within the tile to be computed.
     */
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        RasterAccessor s = new RasterAccessor(sources[0], destRect, formatTags[0],
                getSourceImage(0).getColorModel());
        RasterAccessor d = new RasterAccessor(dest, destRect, formatTags[1], getColorModel());

        // ROI fields
        ROI roiTile = null;

        RandomIter roiIter = null;

        boolean roiContainsTile = false;
        boolean roiDisjointTile = false;

        // ROI check
        if (hasROI) {
            Rectangle srcRectExpanded = mapDestRect(destRect, 0);
            // The tile dimension is extended for avoiding border errors
            srcRectExpanded.setRect(srcRectExpanded.getMinX() - 1, srcRectExpanded.getMinY() - 1,
                    srcRectExpanded.getWidth() + 2, srcRectExpanded.getHeight() + 2);
            roiTile = roi.intersect(new ROIShape(srcRectExpanded));
            // Check if the Tile bounds intersects the roi otherwise, the computation is skipped
            if (!roiBounds.intersects(srcRectExpanded)) {
                roiDisjointTile = true;
            } else {
                roiContainsTile = roiTile.contains(srcRectExpanded);
                if (!roiContainsTile) {
                    if (!roiTile.intersects(srcRectExpanded)) {
                        roiDisjointTile = true;
                    } else {
                        PlanarImage roiIMG = getImage();
                        roiIter = RandomIterFactory.create(roiIMG, null, TILE_CACHED, ARRAY_CALC);
                    }
                }
            }
        }

        if (!hasROI || !roiDisjointTile) {
            switch (d.getDataType()) {
            case DataBuffer.TYPE_BYTE:
                computeRectByte(s, d, roiIter, roiContainsTile);
                break;
            case DataBuffer.TYPE_USHORT:
                computeRectUShort(s, d, roiIter, roiContainsTile);
                break;
            case DataBuffer.TYPE_SHORT:
                computeRectShort(s, d, roiIter, roiContainsTile);
                break;
            case DataBuffer.TYPE_INT:
                computeRectInt(s, d, roiIter, roiContainsTile);
                break;
            case DataBuffer.TYPE_FLOAT:
                computeRectFloat(s, d, roiIter, roiContainsTile);
                break;
            case DataBuffer.TYPE_DOUBLE:
                computeRectDouble(s, d, roiIter, roiContainsTile);
                break;
            }
            if (d.isDataCopy()) {
                d.clampDataArrays();
                d.copyDataToRaster();
            }
        } else {
            // Setting all as NoData
            double[] backgroundValues = new double[s.getNumBands()];
            Arrays.fill(backgroundValues, destNoDataDouble);
            ImageUtil.fillBackground(dest, destRect, backgroundValues);
        }
    }

    private void computeRectByte(RasterAccessor s, RasterAccessor d, RandomIter roiIter,
            boolean roiContainsTile) {
        // Input parameters
        int srcLineStride = s.getScanlineStride();
        int srcPixelStride = s.getPixelStride();
        int srcBands = s.getNumBands();
        int[] srcBandOffsets = s.getBandOffsets();
        byte[][] srcData = s.getByteDataArrays();

        int dstWidth = d.getWidth();
        int dstHeight = d.getHeight();
        int dstBands = d.getNumBands();
        int dstLineStride = d.getScanlineStride();
        int dstPixelStride = d.getPixelStride();
        int[] dstBandOffsets = d.getBandOffsets();
        byte[][] dstData = d.getByteDataArrays();

        int srcLineOffset = 0;
        int dstLineOffset = 0;

        int x0 = 0;
        int y0 = 0;

        int srcX = s.getX();
        int srcY = s.getY();

        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {

                    for (int b = 0; b < dstBands; b++) {
                        // Result initialization
                        float sum = 0.0F;
                        // Coeffs for the Band b
                        double[] mat = matrix[b];
                        // Cycle on the src bands and calculation of the combination
                        for (int k = 0; k < srcBands; k++) {
                            sum += (float) mat[k]
                                    * (float) (srcData[k][srcPixelOffset + srcBandOffsets[k]] & 0xFF);
                        }
                        sum += (float) mat[srcBands];
                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                .clampRoundByte(sum);
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;
            }
        } else if (caseB) {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {
                    // ROI check
                    x0 = srcX + w;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        // Setting destination NoData
                        for (int b = 0; b < dstBands; b++) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataByte;
                        }
                        // Updating offset
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }

                    for (int b = 0; b < dstBands; b++) {
                        // Result initialization
                        float sum = 0.0F;
                        // Coeffs for the Band b
                        double[] mat = matrix[b];
                        // Cycle on the src bands and calculation of the combination
                        for (int k = 0; k < srcBands; k++) {
                            sum += (float) mat[k]
                                    * (float) (srcData[k][srcPixelOffset + srcBandOffsets[k]] & 0xFF);
                        }
                        sum += (float) mat[srcBands];
                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                .clampRoundByte(sum);
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;
            }
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {

                    for (int b = 0; b < dstBands; b++) {
                        // Result initialization
                        float sum = 0.0F;
                        // Coeffs for the Band b
                        double[] mat = matrix[b];
                        // Boolean variable indicating that all the samples are nodata
                        boolean valid = false;
                        // Cycle on the src bands and calculation of the combination
                        for (int k = 0; k < srcBands; k++) {
                            byte sample = srcData[k][srcPixelOffset + srcBandOffsets[k]];
                            if (lut[sample & 0xFF]) {
                                valid = true;
                                sum += (float) mat[k] * (float) (sample & 0xFF);
                            }
                        }
                        if (valid) {
                            sum += (float) mat[srcBands];
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                    .clampRoundByte(sum);
                        } else {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataByte;
                        }
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;
            }
        } else {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {
                    // ROI check
                    x0 = srcX + w;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        // Setting destination NoData
                        for (int b = 0; b < dstBands; b++) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataByte;
                        }
                        // Updating offset
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }

                    for (int b = 0; b < dstBands; b++) {
                        // Result initialization
                        float sum = 0.0F;
                        // Coeffs for the Band b
                        double[] mat = matrix[b];
                        // Boolean variable indicating that all the samples are nodata
                        boolean valid = false;
                        // Cycle on the src bands and calculation of the combination
                        for (int k = 0; k < srcBands; k++) {
                            byte sample = srcData[k][srcPixelOffset + srcBandOffsets[k]];
                            if (lut[sample & 0xFF]) {
                                valid = true;
                                sum += (float) mat[k] * (float) (sample & 0xFF);
                            }
                        }
                        if (valid) {
                            sum += (float) mat[srcBands];
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                    .clampRoundByte(sum);
                        } else {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataByte;
                        }
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;
            }
        }
    }

    private void computeRectUShort(RasterAccessor s, RasterAccessor d, RandomIter roiIter,
            boolean roiContainsTile) {
        // Input parameters
        int srcLineStride = s.getScanlineStride();
        int srcPixelStride = s.getPixelStride();
        int srcBands = s.getNumBands();
        int[] srcBandOffsets = s.getBandOffsets();
        short[][] srcData = s.getShortDataArrays();

        int dstWidth = d.getWidth();
        int dstHeight = d.getHeight();
        int dstBands = d.getNumBands();
        int dstLineStride = d.getScanlineStride();
        int dstPixelStride = d.getPixelStride();
        int[] dstBandOffsets = d.getBandOffsets();
        short[][] dstData = d.getShortDataArrays();

        int srcLineOffset = 0;
        int dstLineOffset = 0;

        int x0 = 0;
        int y0 = 0;

        int srcX = s.getX();
        int srcY = s.getY();

        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {

                    for (int b = 0; b < dstBands; b++) {
                        // Result initialization
                        float sum = 0.0F;
                        // Coeffs for the Band b
                        double[] mat = matrix[b];
                        // Cycle on the src bands and calculation of the combination
                        for (int k = 0; k < srcBands; k++) {
                            sum += (float) mat[k]
                                    * (float) (srcData[k][srcPixelOffset + srcBandOffsets[k]] & 0xFFFF);
                        }
                        sum += (float) mat[srcBands];
                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                .clampRoundUShort(sum);
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;
            }
        } else if (caseB) {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {
                    // ROI check
                    x0 = srcX + w;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        // Setting destination NoData
                        for (int b = 0; b < dstBands; b++) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataShort;
                        }
                        // Updating offset
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }

                    for (int b = 0; b < dstBands; b++) {
                        // Result initialization
                        float sum = 0.0F;
                        // Coeffs for the Band b
                        double[] mat = matrix[b];
                        // Cycle on the src bands and calculation of the combination
                        for (int k = 0; k < srcBands; k++) {
                            sum += (float) mat[k]
                                    * (float) (srcData[k][srcPixelOffset + srcBandOffsets[k]] & 0xFFFF);
                        }
                        sum += (float) mat[srcBands];
                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                .clampRoundUShort(sum);
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;
            }
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {

                    for (int b = 0; b < dstBands; b++) {
                        // Result initialization
                        float sum = 0.0F;
                        // Coeffs for the Band b
                        double[] mat = matrix[b];
                        // Boolean variable indicating that all the samples are nodata
                        boolean valid = false;
                        // Cycle on the src bands and calculation of the combination
                        for (int k = 0; k < srcBands; k++) {
                            short sample = srcData[k][srcPixelOffset + srcBandOffsets[k]];
                            if (!noData.contains(sample)) {
                                valid = true;
                                sum += (float) mat[k] * (float) (sample & 0xFFFF);
                            }
                        }
                        if (valid) {
                            sum += (float) mat[srcBands];
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                    .clampRoundUShort(sum);
                        } else {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataShort;
                        }
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;
            }
        } else {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {
                    // ROI check
                    x0 = srcX + w;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        // Setting destination NoData
                        for (int b = 0; b < dstBands; b++) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataShort;
                        }
                        // Updating offset
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }

                    for (int b = 0; b < dstBands; b++) {
                        // Result initialization
                        float sum = 0.0F;
                        // Coeffs for the Band b
                        double[] mat = matrix[b];
                        // Boolean variable indicating that all the samples are nodata
                        boolean valid = false;
                        // Cycle on the src bands and calculation of the combination
                        for (int k = 0; k < srcBands; k++) {
                            short sample = srcData[k][srcPixelOffset + srcBandOffsets[k]];
                            if (!noData.contains(sample)) {
                                valid = true;
                                sum += (float) mat[k] * (float) (sample & 0xFFFF);
                            }
                        }
                        if (valid) {
                            sum += (float) mat[srcBands];
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                    .clampRoundUShort(sum);
                        } else {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataShort;
                        }
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;
            }
        }
    }

    private void computeRectShort(RasterAccessor s, RasterAccessor d, RandomIter roiIter,
            boolean roiContainsTile) {
        // Input parameters
        int srcLineStride = s.getScanlineStride();
        int srcPixelStride = s.getPixelStride();
        int srcBands = s.getNumBands();
        int[] srcBandOffsets = s.getBandOffsets();
        short[][] srcData = s.getShortDataArrays();

        int dstWidth = d.getWidth();
        int dstHeight = d.getHeight();
        int dstBands = d.getNumBands();
        int dstLineStride = d.getScanlineStride();
        int dstPixelStride = d.getPixelStride();
        int[] dstBandOffsets = d.getBandOffsets();
        short[][] dstData = d.getShortDataArrays();

        int srcLineOffset = 0;
        int dstLineOffset = 0;

        int x0 = 0;
        int y0 = 0;

        int srcX = s.getX();
        int srcY = s.getY();

        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {

                    for (int b = 0; b < dstBands; b++) {
                        // Result initialization
                        float sum = 0.0F;
                        // Coeffs for the Band b
                        double[] mat = matrix[b];
                        // Cycle on the src bands and calculation of the combination
                        for (int k = 0; k < srcBands; k++) {
                            sum += (float) mat[k]
                                    * (float) (srcData[k][srcPixelOffset + srcBandOffsets[k]]);
                        }
                        sum += (float) mat[srcBands];
                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                .clampRoundShort(sum);
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;
            }
        } else if (caseB) {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {
                    // ROI check
                    x0 = srcX + w;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        // Setting destination NoData
                        for (int b = 0; b < dstBands; b++) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataShort;
                        }
                        // Updating offset
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }

                    for (int b = 0; b < dstBands; b++) {
                        // Result initialization
                        float sum = 0.0F;
                        // Coeffs for the Band b
                        double[] mat = matrix[b];
                        // Cycle on the src bands and calculation of the combination
                        for (int k = 0; k < srcBands; k++) {
                            sum += (float) mat[k]
                                    * (float) (srcData[k][srcPixelOffset + srcBandOffsets[k]]);
                        }
                        sum += (float) mat[srcBands];
                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                .clampRoundShort(sum);
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;
            }
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {

                    for (int b = 0; b < dstBands; b++) {
                        // Result initialization
                        float sum = 0.0F;
                        // Coeffs for the Band b
                        double[] mat = matrix[b];
                        // Boolean variable indicating that all the samples are nodata
                        boolean valid = false;
                        // Cycle on the src bands and calculation of the combination
                        for (int k = 0; k < srcBands; k++) {
                            short sample = srcData[k][srcPixelOffset + srcBandOffsets[k]];
                            if (!noData.contains(sample)) {
                                valid = true;
                                sum += (float) mat[k] * (float) sample;
                            }
                        }
                        if (valid) {
                            sum += (float) mat[srcBands];
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                    .clampRoundShort(sum);
                        } else {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataShort;
                        }
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;
            }
        } else {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {
                    // ROI check
                    x0 = srcX + w;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        // Setting destination NoData
                        for (int b = 0; b < dstBands; b++) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataShort;
                        }
                        // Updating offset
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }

                    for (int b = 0; b < dstBands; b++) {
                        // Result initialization
                        float sum = 0.0F;
                        // Coeffs for the Band b
                        double[] mat = matrix[b];
                        // Boolean variable indicating that all the samples are nodata
                        boolean valid = false;
                        // Cycle on the src bands and calculation of the combination
                        for (int k = 0; k < srcBands; k++) {
                            short sample = srcData[k][srcPixelOffset + srcBandOffsets[k]];
                            if (!noData.contains(sample)) {
                                valid = true;
                                sum += (float) mat[k] * (float) sample;
                            }
                        }
                        if (valid) {
                            sum += (float) mat[srcBands];
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                    .clampRoundShort(sum);
                        } else {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataShort;
                        }
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;
            }
        }
    }

    private void computeRectInt(RasterAccessor s, RasterAccessor d, RandomIter roiIter,
            boolean roiContainsTile) {
        // Input parameters
        int srcLineStride = s.getScanlineStride();
        int srcPixelStride = s.getPixelStride();
        int srcBands = s.getNumBands();
        int[] srcBandOffsets = s.getBandOffsets();
        int[][] srcData = s.getIntDataArrays();

        int dstWidth = d.getWidth();
        int dstHeight = d.getHeight();
        int dstBands = d.getNumBands();
        int dstLineStride = d.getScanlineStride();
        int dstPixelStride = d.getPixelStride();
        int[] dstBandOffsets = d.getBandOffsets();
        int[][] dstData = d.getIntDataArrays();

        int srcLineOffset = 0;
        int dstLineOffset = 0;

        int x0 = 0;
        int y0 = 0;

        int srcX = s.getX();
        int srcY = s.getY();

        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {

                    for (int b = 0; b < dstBands; b++) {
                        // Result initialization
                        float sum = 0.0F;
                        // Coeffs for the Band b
                        double[] mat = matrix[b];
                        // Cycle on the src bands and calculation of the combination
                        for (int k = 0; k < srcBands; k++) {
                            sum += (float) mat[k]
                                    * (float) (srcData[k][srcPixelOffset + srcBandOffsets[k]]);
                        }
                        sum += (float) mat[srcBands];
                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                .clampRoundInt(sum);
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;
            }
        } else if (caseB) {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {
                    // ROI check
                    x0 = srcX + w;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        // Setting destination NoData
                        for (int b = 0; b < dstBands; b++) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataInt;
                        }
                        // Updating offset
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }

                    for (int b = 0; b < dstBands; b++) {
                        // Result initialization
                        float sum = 0.0F;
                        // Coeffs for the Band b
                        double[] mat = matrix[b];
                        // Cycle on the src bands and calculation of the combination
                        for (int k = 0; k < srcBands; k++) {
                            sum += (float) mat[k]
                                    * (float) (srcData[k][srcPixelOffset + srcBandOffsets[k]]);
                        }
                        sum += (float) mat[srcBands];
                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                .clampRoundInt(sum);
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;
            }
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {

                    for (int b = 0; b < dstBands; b++) {
                        // Result initialization
                        float sum = 0.0F;
                        // Coeffs for the Band b
                        double[] mat = matrix[b];
                        // Boolean variable indicating that all the samples are nodata
                        boolean valid = false;
                        // Cycle on the src bands and calculation of the combination
                        for (int k = 0; k < srcBands; k++) {
                            int sample = srcData[k][srcPixelOffset + srcBandOffsets[k]];
                            if (!noData.contains(sample)) {
                                valid = true;
                                sum += (float) mat[k] * (float) sample;
                            }
                        }
                        if (valid) {
                            sum += (float) mat[srcBands];
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                    .clampRoundInt(sum);
                        } else {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataInt;
                        }
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;
            }
        } else {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {
                    // ROI check
                    x0 = srcX + w;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        // Setting destination NoData
                        for (int b = 0; b < dstBands; b++) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataInt;
                        }
                        // Updating offset
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }

                    for (int b = 0; b < dstBands; b++) {
                        // Result initialization
                        float sum = 0.0F;
                        // Coeffs for the Band b
                        double[] mat = matrix[b];
                        // Boolean variable indicating that all the samples are nodata
                        boolean valid = false;
                        // Cycle on the src bands and calculation of the combination
                        for (int k = 0; k < srcBands; k++) {
                            int sample = srcData[k][srcPixelOffset + srcBandOffsets[k]];
                            if (!noData.contains(sample)) {
                                valid = true;
                                sum += (float) mat[k] * (float) sample;
                            }
                        }
                        if (valid) {
                            sum += (float) mat[srcBands];
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                    .clampRoundInt(sum);
                        } else {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataInt;
                        }
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;
            }
        }
    }

    private void computeRectFloat(RasterAccessor s, RasterAccessor d, RandomIter roiIter,
            boolean roiContainsTile) {
        // Input parameters
        int srcLineStride = s.getScanlineStride();
        int srcPixelStride = s.getPixelStride();
        int srcBands = s.getNumBands();
        int[] srcBandOffsets = s.getBandOffsets();
        float[][] srcData = s.getFloatDataArrays();

        int dstWidth = d.getWidth();
        int dstHeight = d.getHeight();
        int dstBands = d.getNumBands();
        int dstLineStride = d.getScanlineStride();
        int dstPixelStride = d.getPixelStride();
        int[] dstBandOffsets = d.getBandOffsets();
        float[][] dstData = d.getFloatDataArrays();

        int srcLineOffset = 0;
        int dstLineOffset = 0;

        int x0 = 0;
        int y0 = 0;

        int srcX = s.getX();
        int srcY = s.getY();

        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {

                    for (int b = 0; b < dstBands; b++) {
                        // Result initialization
                        float sum = 0.0F;
                        // Coeffs for the Band b
                        double[] mat = matrix[b];
                        // Cycle on the src bands and calculation of the combination
                        for (int k = 0; k < srcBands; k++) {
                            sum += (float) mat[k]
                                    * (float) (srcData[k][srcPixelOffset + srcBandOffsets[k]]);
                        }
                        sum += (float) mat[srcBands];
                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = sum;
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;
            }
        } else if (caseB) {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {
                    // ROI check
                    x0 = srcX + w;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        // Setting destination NoData
                        for (int b = 0; b < dstBands; b++) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataFloat;
                        }
                        // Updating offset
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }

                    for (int b = 0; b < dstBands; b++) {
                        // Result initialization
                        float sum = 0.0F;
                        // Coeffs for the Band b
                        double[] mat = matrix[b];
                        // Cycle on the src bands and calculation of the combination
                        for (int k = 0; k < srcBands; k++) {
                            sum += (float) mat[k]
                                    * (float) (srcData[k][srcPixelOffset + srcBandOffsets[k]]);
                        }
                        sum += (float) mat[srcBands];
                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = sum;
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;
            }
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {

                    for (int b = 0; b < dstBands; b++) {
                        // Result initialization
                        float sum = 0.0F;
                        // Coeffs for the Band b
                        double[] mat = matrix[b];
                        // Boolean variable indicating that all the samples are nodata
                        boolean valid = false;
                        // Cycle on the src bands and calculation of the combination
                        for (int k = 0; k < srcBands; k++) {
                            float sample = srcData[k][srcPixelOffset + srcBandOffsets[k]];
                            if (!noData.contains(sample)) {
                                valid = true;
                                sum += (float) mat[k] * (float) sample;
                            }
                        }
                        if (valid) {
                            sum += (float) mat[srcBands];
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = sum;
                        } else {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataFloat;
                        }
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;
            }
        } else {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {
                    // ROI check
                    x0 = srcX + w;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        // Setting destination NoData
                        for (int b = 0; b < dstBands; b++) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataFloat;
                        }
                        // Updating offset
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }

                    for (int b = 0; b < dstBands; b++) {
                        // Result initialization
                        float sum = 0.0F;
                        // Coeffs for the Band b
                        double[] mat = matrix[b];
                        // Boolean variable indicating that all the samples are nodata
                        boolean valid = false;
                        // Cycle on the src bands and calculation of the combination
                        for (int k = 0; k < srcBands; k++) {
                            float sample = srcData[k][srcPixelOffset + srcBandOffsets[k]];
                            if (!noData.contains(sample)) {
                                valid = true;
                                sum += (float) mat[k] * (float) sample;
                            }
                        }
                        if (valid) {
                            sum += (float) mat[srcBands];
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = sum;
                        } else {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataFloat;
                        }
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;
            }
        }
    }

    private void computeRectDouble(RasterAccessor s, RasterAccessor d, RandomIter roiIter,
            boolean roiContainsTile) {
        // Input parameters
        int srcLineStride = s.getScanlineStride();
        int srcPixelStride = s.getPixelStride();
        int srcBands = s.getNumBands();
        int[] srcBandOffsets = s.getBandOffsets();
        double[][] srcData = s.getDoubleDataArrays();

        int dstWidth = d.getWidth();
        int dstHeight = d.getHeight();
        int dstBands = d.getNumBands();
        int dstLineStride = d.getScanlineStride();
        int dstPixelStride = d.getPixelStride();
        int[] dstBandOffsets = d.getBandOffsets();
        double[][] dstData = d.getDoubleDataArrays();

        int srcLineOffset = 0;
        int dstLineOffset = 0;

        int x0 = 0;
        int y0 = 0;

        int srcX = s.getX();
        int srcY = s.getY();

        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {

                    for (int b = 0; b < dstBands; b++) {
                        // Result initialization
                        double sum = 0.0D;
                        // Coeffs for the Band b
                        double[] mat = matrix[b];
                        // Cycle on the src bands and calculation of the combination
                        for (int k = 0; k < srcBands; k++) {
                            sum += mat[k] * (srcData[k][srcPixelOffset + srcBandOffsets[k]]);
                        }
                        sum += mat[srcBands];
                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = sum;
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;
            }
        } else if (caseB) {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {
                    // ROI check
                    x0 = srcX + w;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        // Setting destination NoData
                        for (int b = 0; b < dstBands; b++) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataDouble;
                        }
                        // Updating offset
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }

                    for (int b = 0; b < dstBands; b++) {
                        // Result initialization
                        double sum = 0.0D;
                        // Coeffs for the Band b
                        double[] mat = matrix[b];
                        // Cycle on the src bands and calculation of the combination
                        for (int k = 0; k < srcBands; k++) {
                            sum += mat[k] * (srcData[k][srcPixelOffset + srcBandOffsets[k]]);
                        }
                        sum += (float) mat[srcBands];
                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = sum;
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;
            }
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {

                    for (int b = 0; b < dstBands; b++) {
                        // Result initialization
                        double sum = 0.0D;
                        // Coeffs for the Band b
                        double[] mat = matrix[b];
                        // Boolean variable indicating that all the samples are nodata
                        boolean valid = false;
                        // Cycle on the src bands and calculation of the combination
                        for (int k = 0; k < srcBands; k++) {
                            double sample = srcData[k][srcPixelOffset + srcBandOffsets[k]];
                            if (!noData.contains(sample)) {
                                valid = true;
                                sum += mat[k] * sample;
                            }
                        }
                        if (valid) {
                            sum += mat[srcBands];
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = sum;
                        } else {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataDouble;
                        }
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;
            }
        } else {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {
                    // ROI check
                    x0 = srcX + w;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        // Setting destination NoData
                        for (int b = 0; b < dstBands; b++) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataDouble;
                        }
                        // Updating offset
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }

                    for (int b = 0; b < dstBands; b++) {
                        // Result initialization
                        double sum = 0.0D;
                        // Coeffs for the Band b
                        double[] mat = matrix[b];
                        // Boolean variable indicating that all the samples are nodata
                        boolean valid = false;
                        // Cycle on the src bands and calculation of the combination
                        for (int k = 0; k < srcBands; k++) {
                            double sample = srcData[k][srcPixelOffset + srcBandOffsets[k]];
                            if (!noData.contains(sample)) {
                                valid = true;
                                sum += (float) mat[k] * (float) sample;
                            }
                        }
                        if (valid) {
                            sum += mat[srcBands];
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = sum;
                        } else {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataDouble;
                        }
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;
            }
        }
    }

    /**
     * Private method used for generating the Boolean Lookup table used for checking if a Byte data is a NoData
     */
    private void initBooleanNoDataTable() {
        // Initialization of the boolean lookup table
        lut = new boolean[256];

        // Fill the lookuptable
        for (int i = 0; i < 256; i++) {
            boolean result = true;
            if (noData.contains((byte) i)) {
                result = false;
            }
            lut[i] = result;
        }
    }

    /**
     * This method provides a lazy initialization of the image associated to the ROI. The method uses the Double-checked locking in order to maintain
     * thread-safety
     * 
     * @return
     */
    private PlanarImage getImage() {
        PlanarImage img = roiImage;
        if (img == null) {
            synchronized (this) {
                img = roiImage;
                if (img == null) {
                    roiImage = img = roi.getAsImage();
                }
            }
        }
        return img;
    }
}
