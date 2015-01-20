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
package it.geosolutions.jaiext.algebra.constant;

import it.geosolutions.jaiext.algebra.AlgebraDescriptor.Operator;
import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.Map;

import javax.media.jai.ColormapOpImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.iterator.RandomIter;

import com.sun.media.jai.util.ImageUtil;

/**
 * An <code>OpImage</code> implementing any operation defined by the {@link Operator} enum on an image with a constant value array.
 * 
 * <p>
 * This <code>OpImage</code> executes the operation on the pixel values of the source image on a per-band basis.
 * <p>
 * 
 * The value of the pixel (x, y) in the destination image is defined as:
 * 
 * <pre>
 * for (b = 0; b &lt; numBands; b++) {
 *     dst[y][x][b] = op.calculate(src1[y][x][b],const[b]);
 * }
 * </pre>
 * 
 * <p>
 * If the result of the operation overflows/underflows the maximum/minimum value supported by the destination image, then it will be clamped to the
 * maximum/minimum value respectively. The data type <code>byte</code> is treated as unsigned, with maximum value as 255 and minimum value as 0.
 * 
 * 
 */
public final class OperationConstOpImage extends ColormapOpImage {

    /** Constant indicating that the inner random iterators must pre-calculate an array of the image positions */
    public static final boolean ARRAY_CALC = true;

    /** Constant indicating that the inner random iterators must cache the current tile position */
    public static final boolean TILE_CACHED = true;

    // private final static Logger LOGGER = Logger.getLogger(OperationConstOpImage.class.toString());

    private final boolean hasNoData;

    private Range noData;

    private byte[][] byteLookupTable;

    private final boolean hasROI;

    private ROI roi;

    private final boolean caseA;

    private final boolean caseB;

    private final boolean caseC;

    private Operator op;

    private byte destNoDataByte;

    private short destNoDataShort;

    private int destNoDataInt;

    private float destNoDataFloat;

    private double destNoDataDouble;

    private double[] constants;

    private Rectangle roiBounds;

    private PlanarImage roiImage;

    /**
     * Constructs an <code>OperationConstOpImage</code>.
     * 
     * <p>
     * The <code>layout</code> parameter may optionally contains the tile grid layout, sample model, and/or color model. The image dimension is
     * determined by the intersection of the bounding boxes of the two source images.
     * 
     * <p>
     * The image layout of the first source image, <code>source1</code>, is used as the fall-back for the image layout of the destination image. Any
     * layout parameters not specified in the <code>layout</code> argument are set to the same value as that of <code>source1</code>.
     * 
     * @param source the source image
     * @param config the hints
     * @param layout The destination image layout.
     * @param op Operation selected
     * @param constants the constants values to use during the operations
     * @param srcROI ROI used for reducing computation Area
     * @param noData NoData Range used for checking noData
     * @param destinationNoData value for replacing the source nodata values
     */
    public OperationConstOpImage(RenderedImage source, Map config, ImageLayout layout, Operator op,
            double[] constants, ROI srcROI, Range noData, double destinationNoData) {
        super(source, layout, config, true);

        if (op == null) {
            throw new IllegalArgumentException("Operation Not Defined");
        } else {
            this.op = op;
            if (!op.supportsMultipleValues()) {
                throw new IllegalArgumentException("Wrong Operation Defined");
            }
        }

        // DataType check
        int srcDataType = source.getSampleModel().getDataType();

        int dataType = getSampleModel().getDataType();

        // DataType check for the operation
        if (!op.isDataTypeSupported(srcDataType)) {
            throw new IllegalArgumentException("This operation does not support DataType: "
                    + srcDataType);
        }

        // Check on the Constants value
        if (constants == null || constants.length == 0) {
            throw new IllegalArgumentException("Constants not defined");
        }

        int numBands = getSampleModel().getNumBands();
        // If constants size is smaller than the bands number, we take only the first value
        if (constants.length < numBands) {
            this.constants = new double[numBands];
            for (int i = 0; i < numBands; i++) {
                this.constants[i] = constants[0];
            }
        } else {
            this.constants = (double[]) constants.clone();
        }

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

        // Check if No Data control must be done
        if (noData != null) {
            hasNoData = true;
            this.noData = noData;
        } else {
            hasNoData = false;
        }

        // Check if ROI control must be done
        if (srcROI != null) {
            hasROI = true;
            // Roi object
            roi = srcROI;
            roiBounds = roi.getBounds();
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
        // Permit colormap operation
        initializeColormapOperation();

        if (dataType == DataBuffer.TYPE_BYTE) {
            initByteTable();
        }
    }

    private void initByteTable() {

        if (byteLookupTable != null) {
            return;
        }

        int nbands = constants.length;

        byteLookupTable = new byte[nbands][256];

        boolean supportsFloat = op.isDataTypeSupported(DataBuffer.TYPE_FLOAT);

        // Initialize table which implements the operation and clamp
        for (int band = 0; band < nbands; band++) {
            int k = ImageUtil.clampRoundInt(constants[band]);
            float kF = (float) constants[band];
            byte[] t = byteLookupTable[band];
            for (int i = 0; i < 256; i++) {
                if (hasNoData && noData.contains((byte) i)) {
                    t[i] = destNoDataByte;
                } else {
                    if (supportsFloat) {
                        t[i] = ImageUtil.clampRoundByte(op.calculate(i, kF));
                    } else {
                        t[i] = ImageUtil.clampByte(op.calculate(i, k));
                    }
                }
            }
        }
    }

    /**
     * Computes the final pixel from the source image within a specified rectangle.
     * 
     * @param sources Cobbled sources, guaranteed to provide all the source data necessary for computing the rectangle.
     * @param dest The tile containing the rectangle to be computed.
     * @param destRect The rectangle within the tile to be computed.
     */
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        // Retrieve format tags.
        final RasterFormatTag[] formatTags = getFormatTags();

        RasterAccessor src = new RasterAccessor(sources[0], destRect, formatTags[0],
                getSourceImage(0).getColorModel());

        final RasterAccessor dst = new RasterAccessor(dest, destRect, formatTags[1],
                getColorModel());

        ROI roiTile = null;

        RandomIter roiIter = null;

        boolean roiContainsTile = false;
        boolean roiDisjointTile = false;

        // If a ROI is present, then only the part contained inside the current tile bounds is taken.
        if (hasROI) {
            Rectangle srcRectExpanded = mapDestRect(destRect, 0);
            // The tile dimension is extended for avoiding border errors
            srcRectExpanded.setRect(srcRectExpanded.getMinX() - 1, srcRectExpanded.getMinY() - 1,
                    srcRectExpanded.getWidth() + 2, srcRectExpanded.getHeight() + 2);
            roiTile = roi.intersect(new ROIShape(srcRectExpanded));

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
            switch (dst.getDataType()) {
            case DataBuffer.TYPE_BYTE:
                computeRectByte(src, dst, roiIter, roiContainsTile);
                break;
            case DataBuffer.TYPE_USHORT:
                computeRectUShort(src, dst, roiIter, roiContainsTile);
                break;
            case DataBuffer.TYPE_SHORT:
                computeRectShort(src, dst, roiIter, roiContainsTile);
                break;
            case DataBuffer.TYPE_INT:
                computeRectInt(src, dst, roiIter, roiContainsTile);
                break;
            case DataBuffer.TYPE_FLOAT:
                computeRectFloat(src, dst, roiIter, roiContainsTile);
                break;
            case DataBuffer.TYPE_DOUBLE:
                computeRectDouble(src, dst, roiIter, roiContainsTile);
                break;
            }
            // After the calculations, the output data are copied into the WritableRaster
            if (dst.isDataCopy()) {
                dst.clampDataArrays();
                dst.copyDataToRaster();
            }
        } else {
            // If the tile is outside the ROI, then the destination Raster is set to backgroundValues
            double[] bkg = new double[dest.getSampleModel().getNumBands()];
            Arrays.fill(bkg, destNoDataDouble);
            ImageUtil.fillBackground(dest, destRect, bkg);
        }
    }

    private void computeRectByte(RasterAccessor src, RasterAccessor dst, final RandomIter roiIter,
            boolean roiContainsTile) {

        // Initial settings
        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        byte[][] srcData = src.getByteDataArrays();

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        byte[][] dData = dst.getByteDataArrays();

        int x0 = 0;
        int y0 = 0;

        int srcX = src.getX();
        int srcY = src.getY();

        if (hasROI && !roiContainsTile) {
            for (int b = 0; b < dstBands; b++) {
                byte[] d = dData[b];
                byte[] s = srcData[b];
                byte[] bandTable = byteLookupTable[b];

                int dstLineOffset = dBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    dstLineOffset += dLineStride;
                    srcLineOffset += srcLineStride;

                    for (int w = 0; w < dstWidth; w++) {

                        x0 = srcX + w;
                        y0 = srcY + h;

                        if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                            d[dstPixelOffset] = destNoDataByte;
                            dstPixelOffset += dPixelStride;
                            srcPixelOffset += srcPixelStride;
                            continue;
                        }

                        d[dstPixelOffset] = bandTable[s[srcPixelOffset] & 0xFF];

                        dstPixelOffset += dPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
        } else {
            for (int b = 0; b < dstBands; b++) {
                byte[] d = dData[b];
                byte[] s = srcData[b];
                byte[] bandTable = byteLookupTable[b];

                int dstLineOffset = dBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    dstLineOffset += dLineStride;
                    srcLineOffset += srcLineStride;

                    for (int w = 0; w < dstWidth; w++) {

                        d[dstPixelOffset] = bandTable[s[srcPixelOffset] & 0xFF];

                        dstPixelOffset += dPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
        }
    }

    private void computeRectUShort(RasterAccessor src, RasterAccessor dst,
            final RandomIter roiIter, boolean roiContainsTile) {

        // Initial settings
        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        short[][] srcData = src.getShortDataArrays();

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        short[][] dData = dst.getShortDataArrays();

        int x0 = 0;
        int y0 = 0;

        int srcX = src.getX();
        int srcY = src.getY();

        boolean supportsFloat = op.isDataTypeSupported(DataBuffer.TYPE_FLOAT);

        if (caseA || (caseB && roiContainsTile)) {
            for (int b = 0; b < dstBands; b++) {
                int c = ImageUtil.clampRoundInt(constants[b]);
                float cf = (float) constants[b];
                short[] d = dData[b];
                short[] s = srcData[b];

                int dstLineOffset = dBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    dstLineOffset += dLineStride;
                    srcLineOffset += srcLineStride;

                    for (int w = 0; w < dstWidth; w++) {

                        if (op.isUshortSupported()) {
                            if (supportsFloat) {
                                d[dstPixelOffset] = (short) op.calculate(s[srcPixelOffset], cf);
                            } else {
                                d[dstPixelOffset] = (short) op.calculate(s[srcPixelOffset], c);
                            }
                        } else {
                            if (supportsFloat) {
                                d[dstPixelOffset] = ImageUtil.clampRoundUShort(op.calculate(
                                        s[srcPixelOffset] & 0xFFFF, cf));
                            } else {
                                d[dstPixelOffset] = ImageUtil.clampUShort(op.calculate(
                                        s[srcPixelOffset] & 0xFFFF, c));
                            }
                        }

                        dstPixelOffset += dPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
        } else if (caseB) {
            for (int b = 0; b < dstBands; b++) {
                int c = ImageUtil.clampRoundInt(constants[b]);
                float cf = (float) constants[b];
                short[] d = dData[b];
                short[] s = srcData[b];

                int dstLineOffset = dBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    dstLineOffset += dLineStride;
                    srcLineOffset += srcLineStride;

                    for (int w = 0; w < dstWidth; w++) {

                        x0 = srcX + w;
                        y0 = srcY + h;

                        if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                            d[dstPixelOffset] = destNoDataShort;
                            dstPixelOffset += dPixelStride;
                            srcPixelOffset += srcPixelStride;
                            continue;
                        }

                        if (op.isUshortSupported()) {
                            if (supportsFloat) {
                                d[dstPixelOffset] = (short) op.calculate(s[srcPixelOffset], cf);
                            } else {
                                d[dstPixelOffset] = (short) op.calculate(s[srcPixelOffset], c);
                            }
                        } else {
                            if (supportsFloat) {
                                d[dstPixelOffset] = ImageUtil.clampRoundUShort(op.calculate(
                                        s[srcPixelOffset] & 0xFFFF, cf));
                            } else {
                                d[dstPixelOffset] = ImageUtil.clampUShort(op.calculate(
                                        s[srcPixelOffset] & 0xFFFF, c));
                            }
                        }

                        dstPixelOffset += dPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int b = 0; b < dstBands; b++) {
                int c = ImageUtil.clampRoundInt(constants[b]);
                float cf = (float) constants[b];
                short[] d = dData[b];
                short[] s = srcData[b];

                int dstLineOffset = dBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    dstLineOffset += dLineStride;
                    srcLineOffset += srcLineStride;

                    for (int w = 0; w < dstWidth; w++) {

                        if (noData.contains(s[srcPixelOffset])) {
                            d[dstPixelOffset] = destNoDataShort;
                            dstPixelOffset += dPixelStride;
                            srcPixelOffset += srcPixelStride;
                            continue;
                        }

                        if (op.isUshortSupported()) {
                            if (supportsFloat) {
                                d[dstPixelOffset] = (short) op.calculate(s[srcPixelOffset], cf);
                            } else {
                                d[dstPixelOffset] = (short) op.calculate(s[srcPixelOffset], c);
                            }
                        } else {
                            if (supportsFloat) {
                                d[dstPixelOffset] = ImageUtil.clampRoundUShort(op.calculate(
                                        s[srcPixelOffset] & 0xFFFF, cf));
                            } else {
                                d[dstPixelOffset] = ImageUtil.clampUShort(op.calculate(
                                        s[srcPixelOffset] & 0xFFFF, c));
                            }
                        }

                        dstPixelOffset += dPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
        } else {
            for (int b = 0; b < dstBands; b++) {
                int c = ImageUtil.clampRoundInt(constants[b]);
                float cf = (float) constants[b];
                short[] d = dData[b];
                short[] s = srcData[b];

                int dstLineOffset = dBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    dstLineOffset += dLineStride;
                    srcLineOffset += srcLineStride;

                    for (int w = 0; w < dstWidth; w++) {

                        x0 = srcX + w;
                        y0 = srcY + h;

                        if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)
                                || noData.contains(s[srcPixelOffset])) {
                            d[dstPixelOffset] = destNoDataShort;
                            dstPixelOffset += dPixelStride;
                            srcPixelOffset += srcPixelStride;
                            continue;
                        }

                        if (op.isUshortSupported()) {
                            if (supportsFloat) {
                                d[dstPixelOffset] = (short) op.calculate(s[srcPixelOffset], cf);
                            } else {
                                d[dstPixelOffset] = (short) op.calculate(s[srcPixelOffset], c);
                            }
                        } else {
                            if (supportsFloat) {
                                d[dstPixelOffset] = ImageUtil.clampRoundUShort(op.calculate(
                                        s[srcPixelOffset] & 0xFFFF, cf));
                            } else {
                                d[dstPixelOffset] = ImageUtil.clampUShort(op.calculate(
                                        s[srcPixelOffset] & 0xFFFF, c));
                            }
                        }

                        dstPixelOffset += dPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
        }
    }

    private void computeRectShort(RasterAccessor src, RasterAccessor dst, final RandomIter roiIter,
            boolean roiContainsTile) {

        // Initial settings
        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        short[][] srcData = src.getShortDataArrays();

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        short[][] dData = dst.getShortDataArrays();

        int x0 = 0;
        int y0 = 0;

        int srcX = src.getX();
        int srcY = src.getY();

        boolean supportsFloat = op.isDataTypeSupported(DataBuffer.TYPE_FLOAT);

        if (caseA || (caseB && roiContainsTile)) {
            for (int b = 0; b < dstBands; b++) {
                int c = ImageUtil.clampRoundInt(constants[b]);
                float cf = (float) constants[b];
                short[] d = dData[b];
                short[] s = srcData[b];

                int dstLineOffset = dBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    dstLineOffset += dLineStride;
                    srcLineOffset += srcLineStride;

                    for (int w = 0; w < dstWidth; w++) {

                        if (supportsFloat) {
                            d[dstPixelOffset] = ImageUtil.clampRoundShort(op.calculate(
                                    s[srcPixelOffset], cf));
                        } else {
                            d[dstPixelOffset] = ImageUtil.clampShort(op.calculate(
                                    s[srcPixelOffset], c));
                        }

                        dstPixelOffset += dPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
        } else if (caseB) {
            for (int b = 0; b < dstBands; b++) {
                int c = ImageUtil.clampRoundInt(constants[b]);
                float cf = (float) constants[b];
                short[] d = dData[b];
                short[] s = srcData[b];

                int dstLineOffset = dBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    dstLineOffset += dLineStride;
                    srcLineOffset += srcLineStride;

                    for (int w = 0; w < dstWidth; w++) {

                        x0 = srcX + w;
                        y0 = srcY + h;

                        if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                            d[dstPixelOffset] = destNoDataShort;
                            dstPixelOffset += dPixelStride;
                            srcPixelOffset += srcPixelStride;
                            continue;
                        }

                        if (supportsFloat) {
                            d[dstPixelOffset] = ImageUtil.clampRoundShort(op.calculate(
                                    s[srcPixelOffset], cf));
                        } else {
                            d[dstPixelOffset] = ImageUtil.clampShort(op.calculate(
                                    s[srcPixelOffset], c));
                        }

                        dstPixelOffset += dPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int b = 0; b < dstBands; b++) {
                int c = ImageUtil.clampRoundInt(constants[b]);
                float cf = (float) constants[b];
                short[] d = dData[b];
                short[] s = srcData[b];

                int dstLineOffset = dBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    dstLineOffset += dLineStride;
                    srcLineOffset += srcLineStride;

                    for (int w = 0; w < dstWidth; w++) {

                        if (noData.contains(s[srcPixelOffset])) {
                            d[dstPixelOffset] = destNoDataShort;
                            dstPixelOffset += dPixelStride;
                            srcPixelOffset += srcPixelStride;
                            continue;
                        }

                        if (supportsFloat) {
                            d[dstPixelOffset] = ImageUtil.clampRoundShort(op.calculate(
                                    s[srcPixelOffset], cf));
                        } else {
                            d[dstPixelOffset] = ImageUtil.clampShort(op.calculate(
                                    s[srcPixelOffset], c));
                        }

                        dstPixelOffset += dPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
        } else {
            for (int b = 0; b < dstBands; b++) {
                int c = ImageUtil.clampRoundInt(constants[b]);
                float cf = (float) constants[b];
                short[] d = dData[b];
                short[] s = srcData[b];

                int dstLineOffset = dBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    dstLineOffset += dLineStride;
                    srcLineOffset += srcLineStride;

                    for (int w = 0; w < dstWidth; w++) {

                        x0 = srcX + w;
                        y0 = srcY + h;

                        if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)
                                || noData.contains(s[srcPixelOffset])) {
                            d[dstPixelOffset] = destNoDataShort;
                            dstPixelOffset += dPixelStride;
                            srcPixelOffset += srcPixelStride;
                            continue;
                        }

                        if (supportsFloat) {
                            d[dstPixelOffset] = ImageUtil.clampRoundShort(op.calculate(
                                    s[srcPixelOffset], cf));
                        } else {
                            d[dstPixelOffset] = ImageUtil.clampShort(op.calculate(
                                    s[srcPixelOffset], c));
                        }

                        dstPixelOffset += dPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
        }
    }

    private void computeRectInt(RasterAccessor src, RasterAccessor dst, final RandomIter roiIter,
            boolean roiContainsTile) {

        // Initial settings
        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        int[][] srcData = src.getIntDataArrays();

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        int[][] dData = dst.getIntDataArrays();

        int x0 = 0;
        int y0 = 0;

        int srcX = src.getX();
        int srcY = src.getY();

        boolean supportsDouble = op.isDataTypeSupported(DataBuffer.TYPE_DOUBLE);

        if (caseA || (caseB && roiContainsTile)) {
            for (int b = 0; b < dstBands; b++) {
                long c = ImageUtil.clampRoundInt(constants[b]);
                int[] d = dData[b];
                int[] s = srcData[b];

                int dstLineOffset = dBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    dstLineOffset += dLineStride;
                    srcLineOffset += srcLineStride;

                    for (int w = 0; w < dstWidth; w++) {

                        if (supportsDouble) {
                            d[dstPixelOffset] = ImageUtil.clampRoundInt(op.calculate(
                                    s[srcPixelOffset], constants[b]));
                        } else {
                            d[dstPixelOffset] = ImageUtil.clampInt(op.calculateL(s[srcPixelOffset],
                                    c));
                        }

                        dstPixelOffset += dPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
        } else if (caseB) {
            for (int b = 0; b < dstBands; b++) {
                long c = ImageUtil.clampRoundInt(constants[b]);
                int[] d = dData[b];
                int[] s = srcData[b];

                int dstLineOffset = dBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    dstLineOffset += dLineStride;
                    srcLineOffset += srcLineStride;

                    for (int w = 0; w < dstWidth; w++) {

                        x0 = srcX + w;
                        y0 = srcY + h;

                        if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                            d[dstPixelOffset] = destNoDataInt;
                            dstPixelOffset += dPixelStride;
                            srcPixelOffset += srcPixelStride;
                            continue;
                        }

                        if (supportsDouble) {
                            d[dstPixelOffset] = ImageUtil.clampRoundInt(op.calculate(
                                    s[srcPixelOffset], constants[b]));
                        } else {
                            d[dstPixelOffset] = ImageUtil.clampInt(op.calculateL(s[srcPixelOffset],
                                    c));
                        }

                        dstPixelOffset += dPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int b = 0; b < dstBands; b++) {
                long c = ImageUtil.clampRoundInt(constants[b]);
                int[] d = dData[b];
                int[] s = srcData[b];

                int dstLineOffset = dBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    dstLineOffset += dLineStride;
                    srcLineOffset += srcLineStride;

                    for (int w = 0; w < dstWidth; w++) {

                        if (noData.contains(s[srcPixelOffset])) {
                            d[dstPixelOffset] = destNoDataInt;
                            dstPixelOffset += dPixelStride;
                            srcPixelOffset += srcPixelStride;
                            continue;
                        }

                        if (supportsDouble) {
                            d[dstPixelOffset] = ImageUtil.clampRoundInt(op.calculate(
                                    s[srcPixelOffset], constants[b]));
                        } else {
                            d[dstPixelOffset] = ImageUtil.clampInt(op.calculateL(s[srcPixelOffset],
                                    c));
                        }

                        dstPixelOffset += dPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
        } else {
            for (int b = 0; b < dstBands; b++) {
                long c = ImageUtil.clampRoundInt(constants[b]);
                int[] d = dData[b];
                int[] s = srcData[b];

                int dstLineOffset = dBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    dstLineOffset += dLineStride;
                    srcLineOffset += srcLineStride;

                    for (int w = 0; w < dstWidth; w++) {

                        x0 = srcX + w;
                        y0 = srcY + h;

                        if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)
                                || noData.contains(s[srcPixelOffset])) {
                            d[dstPixelOffset] = destNoDataInt;
                            dstPixelOffset += dPixelStride;
                            srcPixelOffset += srcPixelStride;
                            continue;
                        }

                        if (supportsDouble) {
                            d[dstPixelOffset] = ImageUtil.clampRoundInt(op.calculate(
                                    s[srcPixelOffset], constants[b]));
                        } else {
                            d[dstPixelOffset] = ImageUtil.clampInt(op.calculateL(s[srcPixelOffset],
                                    c));
                        }

                        dstPixelOffset += dPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
        }
    }

    private void computeRectFloat(RasterAccessor src, RasterAccessor dst, final RandomIter roiIter,
            boolean roiContainsTile) {

        // Initial settings
        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        float[][] srcData = src.getFloatDataArrays();

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        float[][] dData = dst.getFloatDataArrays();

        int x0 = 0;
        int y0 = 0;

        int srcX = src.getX();
        int srcY = src.getY();

        if (caseA || (caseB && roiContainsTile)) {
            for (int b = 0; b < dstBands; b++) {
                float[] d = dData[b];
                float[] s = srcData[b];

                int dstLineOffset = dBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    dstLineOffset += dLineStride;
                    srcLineOffset += srcLineStride;

                    for (int w = 0; w < dstWidth; w++) {

                        d[dstPixelOffset] = ImageUtil.clampFloat(op.calculate(s[srcPixelOffset],
                                constants[b]));

                        dstPixelOffset += dPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
        } else if (caseB) {
            for (int b = 0; b < dstBands; b++) {
                float[] d = dData[b];
                float[] s = srcData[b];

                int dstLineOffset = dBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    dstLineOffset += dLineStride;
                    srcLineOffset += srcLineStride;

                    for (int w = 0; w < dstWidth; w++) {

                        x0 = srcX + w;
                        y0 = srcY + h;

                        if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                            d[dstPixelOffset] = destNoDataFloat;
                            dstPixelOffset += dPixelStride;
                            srcPixelOffset += srcPixelStride;
                            continue;
                        }

                        d[dstPixelOffset] = ImageUtil.clampFloat(op.calculate(s[srcPixelOffset],
                                constants[b]));

                        dstPixelOffset += dPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int b = 0; b < dstBands; b++) {
                float[] d = dData[b];
                float[] s = srcData[b];

                int dstLineOffset = dBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    dstLineOffset += dLineStride;
                    srcLineOffset += srcLineStride;

                    for (int w = 0; w < dstWidth; w++) {

                        if (noData.contains(s[srcPixelOffset])) {
                            d[dstPixelOffset] = destNoDataFloat;
                            dstPixelOffset += dPixelStride;
                            srcPixelOffset += srcPixelStride;
                            continue;
                        }

                        d[dstPixelOffset] = ImageUtil.clampFloat(op.calculate(s[srcPixelOffset],
                                constants[b]));

                        dstPixelOffset += dPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
        } else {
            for (int b = 0; b < dstBands; b++) {
                float[] d = dData[b];
                float[] s = srcData[b];

                int dstLineOffset = dBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    dstLineOffset += dLineStride;
                    srcLineOffset += srcLineStride;

                    for (int w = 0; w < dstWidth; w++) {

                        x0 = srcX + w;
                        y0 = srcY + h;

                        if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)
                                || noData.contains(s[srcPixelOffset])) {
                            d[dstPixelOffset] = destNoDataFloat;
                            dstPixelOffset += dPixelStride;
                            srcPixelOffset += srcPixelStride;
                            continue;
                        }

                        d[dstPixelOffset] = ImageUtil.clampFloat(op.calculate(s[srcPixelOffset],
                                constants[b]));

                        dstPixelOffset += dPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
        }
    }

    private void computeRectDouble(RasterAccessor src, RasterAccessor dst,
            final RandomIter roiIter, boolean roiContainsTile) {

        // Initial settings
        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        double[][] srcData = src.getDoubleDataArrays();

        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstBands = dst.getNumBands();

        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        double[][] dData = dst.getDoubleDataArrays();

        int x0 = 0;
        int y0 = 0;

        int srcX = src.getX();
        int srcY = src.getY();

        if (caseA || (caseB && roiContainsTile)) {
            for (int b = 0; b < dstBands; b++) {
                double[] d = dData[b];
                double[] s = srcData[b];

                int dstLineOffset = dBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    dstLineOffset += dLineStride;
                    srcLineOffset += srcLineStride;

                    for (int w = 0; w < dstWidth; w++) {

                        d[dstPixelOffset] = op.calculate(s[srcPixelOffset], constants[b]);

                        dstPixelOffset += dPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
        } else if (caseB) {
            for (int b = 0; b < dstBands; b++) {
                double[] d = dData[b];
                double[] s = srcData[b];

                int dstLineOffset = dBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    dstLineOffset += dLineStride;
                    srcLineOffset += srcLineStride;

                    for (int w = 0; w < dstWidth; w++) {

                        x0 = srcX + w;
                        y0 = srcY + h;

                        if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                            d[dstPixelOffset] = destNoDataDouble;
                            dstPixelOffset += dPixelStride;
                            srcPixelOffset += srcPixelStride;
                            continue;
                        }

                        d[dstPixelOffset] = op.calculate(s[srcPixelOffset], constants[b]);

                        dstPixelOffset += dPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int b = 0; b < dstBands; b++) {
                double[] d = dData[b];
                double[] s = srcData[b];

                int dstLineOffset = dBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    dstLineOffset += dLineStride;
                    srcLineOffset += srcLineStride;

                    for (int w = 0; w < dstWidth; w++) {

                        if (noData.contains(s[srcPixelOffset])) {
                            d[dstPixelOffset] = destNoDataDouble;
                            dstPixelOffset += dPixelStride;
                            srcPixelOffset += srcPixelStride;
                            continue;
                        }

                        d[dstPixelOffset] = op.calculate(s[srcPixelOffset], constants[b]);

                        dstPixelOffset += dPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
        } else {
            for (int b = 0; b < dstBands; b++) {
                double[] d = dData[b];
                double[] s = srcData[b];

                int dstLineOffset = dBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    dstLineOffset += dLineStride;
                    srcLineOffset += srcLineStride;

                    for (int w = 0; w < dstWidth; w++) {

                        x0 = srcX + w;
                        y0 = srcY + h;

                        if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)
                                || noData.contains(s[srcPixelOffset])) {
                            d[dstPixelOffset] = destNoDataDouble;
                            dstPixelOffset += dPixelStride;
                            srcPixelOffset += srcPixelStride;
                            continue;
                        }

                        d[dstPixelOffset] = op.calculate(s[srcPixelOffset], constants[b]);

                        dstPixelOffset += dPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
        }
    }

    @Override
    protected void transformColormap(byte[][] colormap) {
        initByteTable();

        for (int b = 0; b < 3; b++) {
            byte[] map = colormap[b];
            byte[] luTable = byteLookupTable[b >= byteLookupTable.length ? 0 : b];
            int mapSize = map.length;

            for (int i = 0; i < mapSize; i++) {
                map[i] = luTable[(map[i] & 0xFF)];
            }
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
