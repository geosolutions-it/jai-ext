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
package it.geosolutions.jaiext.rescale;

import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.util.Map;
import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.PointOpImage;
import javax.media.jai.ROI;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.RenderedOp;
import javax.media.jai.iterator.RandomIter;
import com.sun.media.jai.util.ImageUtil;

/**
 * This class is used for rescaling the source image pixels with the given scale and offset factors. At the instantiation time this class checks if
 * the input parameters are suitable for the Rescale operation. If the image data type is Byte, the rescale operation on every pixel value is
 * pre-calculated and stored inside a byte array and the rescaling is effectively a simple lookup operation. For the other data types the Rescale
 * operation is performed at runtime. The rescale operation is executed for each tile independently. If input ROI or NoData values are founded, then
 * they are not rescaled, but the input destination No Data value is returned.
 */

public class RescaleOpImage extends PointOpImage {

    /** ROI extender */
    protected final static BorderExtender ROI_EXTENDER = BorderExtender
            .createInstance(BorderExtender.BORDER_ZERO);

    /** Scale factors for each band */
    private final double[] scaleFactors;

    /** Offset factors for each band */
    private final double[] offsetArray;

    /** Boolean indicating if a No Data Range is used */
    private final boolean hasNoData;

    /** Boolean indicating if a ROI object is used */
    private final boolean hasROI;

    /** No Data Range */
    private Range noData;

    /** ROI image */
    private PlanarImage srcROIImage;

    /** Rectangle containing ROI bounds */
    private Rectangle roiBounds;

    /** Boolean indicating if a ROI RasterAccessor should be used */
    private final boolean useROIAccessor;

    /** Boolean lookuptable used if no data are present */
    private boolean[] booleanLookupTable;

    /** Boolean indicating that there No Data and ROI are not used */
    private final boolean caseA;

    /** Boolean indicating that only ROI is used */
    private final boolean caseB;

    /** Boolean indicating that only No Data are used */
    private final boolean caseC;

    /** Precalculated rescale lookup table for fast computations */
    private byte[][] byteRescaleTable = null;

    /** Destination value for No Data byte */
    private byte destinationNoDataByte;

    /** Destination value for No Data ushort/short */
    private short destinationNoDataShort;

    /** Destination value for No Data int */
    private int destinationNoDataInt;

    /** Destination value for No Data float */
    private float destinationNoDataFloat;

    /** Destination value for No Data double */
    private double destinationNoDataDouble;

    /** Extended ROI image */
    private RenderedOp srcROIImgExt;

    public RescaleOpImage(RenderedImage source, ImageLayout layout, Map configuration,
            double[] valueScale, double[] valueOffsets, double destinationNoData, ROI roi,
            Range noData, boolean useROIAccessor) {
        super(source, layout, configuration, true);

        // Selection of the band number
        int numBands = getSampleModel().getNumBands();

        // Check if the constants number is equal to the band number
        // If they are not equal the first constant is used for all bands
        if (valueScale.length < numBands && valueScale.length >= 1) {
            this.scaleFactors = new double[numBands];
            for (int i = 0; i < numBands; i++) {
                this.scaleFactors[i] = valueScale[0];
            }
        } else if (valueScale.length < 1) {
            // In this case an exception is thrown
            throw new IllegalArgumentException(
                    "Input Scale factor array should have almost dimension 1");
        } else {
            // Else the constants are copied
            this.scaleFactors = valueScale;
        }

        // Check if the offsets number is equal to the band number
        // If they are not equal the first offset is used for all bands
        if (valueOffsets.length < numBands && valueOffsets.length >= 1) {
            this.offsetArray = new double[numBands];
            for (int i = 0; i < numBands; i++) {
                this.offsetArray[i] = valueOffsets[0];
            }
        } else if (valueOffsets.length < 1) {
            // In this case an exception is thrown
            throw new IllegalArgumentException("Input offset array should have almost dimension 1");
        } else {
            // Else the offsets are copied
            this.offsetArray = valueOffsets;
        }

        // Set flag to permit in-place operation.
        permitInPlaceOperation();

        // Check if No Data control must be done
        if (noData != null) {
            hasNoData = true;
            this.noData = noData;
        } else {
            hasNoData = false;
        }

        // Check if ROI control must be done
        if (roi != null) {
            hasROI = true;
            // Roi object
            ROI srcROI = roi;
            // Creation of a PlanarImage containing the ROI data
            srcROIImage = srcROI.getAsImage();
            // Source Bounds
            Rectangle srcRect = new Rectangle(source.getMinX(), source.getMinY(),
                    source.getWidth(), source.getHeight());
            // Padding of the input ROI image in order to avoid the call of the getExtendedData() method
            // ROI bounds are saved
            roiBounds = srcROIImage.getBounds();
            int deltaX0 = (roiBounds.x - srcRect.x);
            int leftP = deltaX0 > 0 ? deltaX0 : 0;
            int deltaY0 = (roiBounds.y - srcRect.y);
            int topP = deltaY0 > 0 ? deltaY0 : 0;
            int deltaX1 = (srcRect.x + srcRect.width - roiBounds.x + roiBounds.width);
            int rightP = deltaX1 > 0 ? deltaX1 : 0;
            int deltaY1 = (srcRect.y + srcRect.height - roiBounds.y + roiBounds.height);
            int bottomP = deltaY1 > 0 ? deltaY1 : 0;
            // Extend the ROI image
            ParameterBlock pb = new ParameterBlock();
            pb.setSource(srcROIImage, 0);
            pb.set(leftP, 0);
            pb.set(rightP, 1);
            pb.set(topP, 2);
            pb.set(bottomP, 3);
            pb.set(ROI_EXTENDER, 4);
            srcROIImgExt = JAI.create("border", pb);
            // The useRoiAccessor parameter is set
            this.useROIAccessor = useROIAccessor;
        } else {
            hasROI = false;
            this.useROIAccessor = false;
            roiBounds = null;
            srcROIImage = null;
        }

        // Image dataType
        int dataType = getSampleModel().getDataType();

        // Boolean indicating if the image data type is byte
        boolean isByte = dataType == DataBuffer.TYPE_BYTE;

        // Creation of a lookuptable containing the values to use for no data
        if (hasNoData && isByte) {
            booleanLookupTable = new boolean[256];
            for (int i = 0; i < booleanLookupTable.length; i++) {
                byte value = (byte) i;
                booleanLookupTable[i] = !noData.contains(value);
            }
        }

        if (isByte) {
            byteRescaleTable = new byte[numBands][256];

            // Initialize table which implements Rescale and clamping
            for (int b = 0; b < numBands; b++) {
                byte[] band = byteRescaleTable[b];
                double c = scaleFactors[b];
                double o = offsetArray[b];
                for (int i = 0; i < 256; i++) {
                    band[i] = ImageUtil.clampRoundByte(i * c + o);
                }
            }
        }

        // Definition of the possible cases that can be found
        // caseA = no ROI nor No Data
        // caseB = ROI present but No Data not present
        // caseC = No Data present but ROI not present
        // Last case not defined = both ROI and No Data are present
        caseA = !hasNoData && !hasROI;
        caseB = !hasNoData && hasROI;
        caseC = hasNoData && !hasROI;

        // DestinationNoData setting
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            destinationNoDataByte = ImageUtil.clampRoundByte(destinationNoData);
            break;
        case DataBuffer.TYPE_USHORT:
            destinationNoDataShort = ImageUtil.clampRoundUShort(destinationNoData);
            break;
        case DataBuffer.TYPE_SHORT:
            destinationNoDataShort = ImageUtil.clampRoundShort(destinationNoData);
            break;
        case DataBuffer.TYPE_INT:
            destinationNoDataInt = ImageUtil.clampRoundInt(destinationNoData);
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

    /**
     * Rescales to the pixel values within a specified rectangle.
     * 
     * @param sources Cobbled sources, guaranteed to provide all the source data necessary for computing the rectangle.
     * @param dest The tile containing the rectangle to be computed.
     * @param destRect The rectangle within the tile to be computed.
     */
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {

        Raster tile = sources[0];

        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        Rectangle srcRect = mapDestRect(destRect, 0);

        RasterAccessor srcAccessor = new RasterAccessor(tile, srcRect, formatTags[0],
                getSourceImage(0).getColorModel());
        RasterAccessor destAccessor = new RasterAccessor(dest, destRect, formatTags[1],
                getColorModel());

        // ROI calculations if roiAccessor is used
        RasterAccessor roiAccessor = null;
        RandomIter roiIter = null;
        if (useROIAccessor) {
            // Note that the getExtendedData() method is not called because the input images are padded.
            // For each image there is a check if the rectangle is contained inside the source image;
            // if this not happen, the data is taken from the padded image.
            Raster roiRaster = null;
            if (roiBounds.contains(srcRect)) {
                roiRaster = srcROIImage.getData(srcRect);
            } else {
                roiRaster = srcROIImgExt.getData(srcRect);
            }
            // creation of the rasterAccessor
            roiAccessor = new RasterAccessor(roiRaster, srcRect, RasterAccessor.findCompatibleTags(
                    new RenderedImage[] { srcROIImage }, srcROIImage)[0],
                    srcROIImage.getColorModel());
        } else if(hasROI) {
            roiIter = RandomIterFactory.create(srcROIImage, srcROIImage.getBounds(), true, true);
        }

        int dataType = destAccessor.getDataType();

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(srcAccessor, destAccessor, roiAccessor, roiIter);
            break;
        case DataBuffer.TYPE_USHORT:
            ushortLoop(srcAccessor, destAccessor, roiAccessor, roiIter);
            break;
        case DataBuffer.TYPE_SHORT:
            shortLoop(srcAccessor, destAccessor, roiAccessor, roiIter);
            break;
        case DataBuffer.TYPE_INT:
            intLoop(srcAccessor, destAccessor, roiAccessor, roiIter);
            break;
        case DataBuffer.TYPE_FLOAT:
            floatLoop(srcAccessor, destAccessor, roiAccessor, roiIter);
            break;
        case DataBuffer.TYPE_DOUBLE:
            doubleLoop(srcAccessor, destAccessor, roiAccessor, roiIter);
            break;
        default:
            throw new IllegalArgumentException("Wrong data type");
        }

        if (destAccessor.needsClamping()) {
            /* Further clamp down to underlying raster data type. */
            destAccessor.clampDataArrays();
        }
        destAccessor.copyDataToRaster();

    }

    private void byteLoop(RasterAccessor src, RasterAccessor dst, RasterAccessor roi, RandomIter roiIter) {

        // Setup of the initial parameters
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        byte[][] dstData = dst.getByteDataArrays();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        byte[][] srcData = src.getByteDataArrays();

        int srcX = src.getX();
        int srcY = src.getY();

        final byte[] roiDataArray;
        final int roiLineStride;
        final int roiDataLength;

        // If ROI RasterAccessor is used, some parameters must be set
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiLineStride = roi.getScanlineStride();
            roiDataLength = roiDataArray.length;
        } else {
            roiDataArray = null;
            roiLineStride = 0;
            roiDataLength = 0;
        }

        // NO ROI NO NODATA
        if (caseA) {
            // Cycle on all the bands
            for (int b = 0; b < dstBands; b++) {
                // creation of the line offsets
                int dstLineOffset = dstBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];
                // Selection of the rescale table already created for the selected band
                byte[] clamp = byteRescaleTable[b];
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;
                    // update of the line offsets
                    dstLineOffset += dstLineStride;
                    srcLineOffset += srcLineStride;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // Rescale operation
                        dstData[b][dstPixelOffset] = clamp[srcData[b][srcPixelOffset] & 0xFF];
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
            // ROI WITHOUT NODATA
        } else if (caseB) {
            // ROI RASTERACCESSOR USED
            if (useROIAccessor) {
                // Initial offsets
                int dstOffset = 0;
                int srcOffset = 0;
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstOffset;
                    int srcPixelOffset = srcOffset;
                    // Selection of the y position in the ROI array
                    int posyROI = y * roiLineStride;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // ROI index position
                        int windex = x + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // If the value is inside the ROI, then the Rescale operation is executed, else
                        // destination No Data is returned
                        if (w != 0) {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                // Rescale operation
                                byte[] clamp = byteRescaleTable[b];
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = clamp[srcData[b][srcPixelOffset
                                        + srcBandOffsets[b]] & 0xFF];
                            }
                        } else {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataByte;
                            }
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                    // update of the initial offsets
                    dstOffset += dstLineStride;
                    srcOffset += srcLineStride;
                }
                // ROI RASTERACCESSOR NOT USED
            } else {
                // Initial offsets
                int dstOffset = 0;
                int srcOffset = 0;
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstOffset;
                    int srcPixelOffset = srcOffset;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        // Check if the pixel is inside ROI
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0);
                            // If the pixel is inside the ROI, then the Rescale operation is executed, else
                            // destination No Data is returned
                            if (w != 0) {
                                // Cycle on all the bands
                                for (int b = 0; b < dstBands; b++) {
                                    // Rescale operation
                                    byte[] clamp = byteRescaleTable[b];
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = clamp[srcData[b][srcPixelOffset
                                            + srcBandOffsets[b]] & 0xFF];
                                }
                            } else {
                                // Cycle on all the bands
                                for (int b = 0; b < dstBands; b++) {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataByte;
                            }
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                    // update of the initial offsets
                    dstOffset += dstLineStride;
                    srcOffset += srcLineStride;
                }
            }
            // NODATA WITHOUT ROI
        } else if (caseC) {
            // Cycle on all the bands
            for (int b = 0; b < dstBands; b++) {
                // creation of the line offsets
                int dstLineOffset = dstBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];
                // Selection of the rescale table already created for the selected band
                byte[] clamp = byteRescaleTable[b];
                // Selection of the input band array
                byte[] bandDataIn = srcData[b];
                // Selection of the output band array
                byte[] bandDataOut = dstData[b];
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;
                    // update of the line offsets
                    dstLineOffset += dstLineStride;
                    srcLineOffset += srcLineStride;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // Selection of the value to calculate
                        int value = bandDataIn[srcPixelOffset] & 0xFF;
                        // Check if the value is not a NoData
                        if (booleanLookupTable[value]) {
                            // Rescale operation
                            bandDataOut[dstPixelOffset] = clamp[value];
                        } else {
                            // Else, destination No Data is set
                            bandDataOut[dstPixelOffset] = destinationNoDataByte;
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }

            // ROI AND NODATA
        } else {
            // ROI RASTERACCESSOR USED
            if (useROIAccessor) {
                // Initial offsets
                int dstOffset = 0;
                int srcOffset = 0;
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstOffset;
                    int srcPixelOffset = srcOffset;
                    // Selection of the y position in the ROI array
                    int posyROI = y * roiLineStride;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // ROI index position
                        int windex = x + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // If the value is inside the ROI, then the Rescale operation is executed, else
                        // destination No Data is returned
                        if (w != 0) {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                // Selection of the value to calculate
                                int value = srcData[b][srcPixelOffset + srcBandOffsets[b]] & 0xFF;
                                // Check if the value is not a NoData
                                if (booleanLookupTable[value]) {
                                    // Rescale operation
                                    byte[] clamp = byteRescaleTable[b];
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = clamp[value];
                                } else {
                                    // Else, destination No Data is set
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataByte;
                            }
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                    // update of the initial offsets
                    dstOffset += dstLineStride;
                    srcOffset += srcLineStride;
                }
                // ROI RASTERACCESSOR NOT USED
            } else {
                // Initial offsets
                int dstOffset = 0;
                int srcOffset = 0;
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstOffset;
                    int srcPixelOffset = srcOffset;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        // Check if the pixel is inside ROI
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0);
                            // If the pixel is inside the ROI, then the Rescale operation is executed, else
                            // destination No Data is returned
                            if (w != 0) {
                                // Cycle on all the bands
                                for (int b = 0; b < dstBands; b++) {
                                    // Selection of the value to calculate
                                    int value = srcData[b][srcPixelOffset + srcBandOffsets[b]] & 0xFF;
                                    // Check if the value is not a NoData
                                    if (booleanLookupTable[value]) {
                                        // Rescale operation
                                        byte[] clamp = byteRescaleTable[b];
                                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = clamp[value];
                                    } else {
                                        // Else, destination No Data is set
                                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataByte;
                                    }
                                }
                            } else {
                                // Cycle on all the bands
                                for (int b = 0; b < dstBands; b++) {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataByte;
                                }
                            }
                        } else {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataByte;
                            }
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                    // update of the initial offsets
                    dstOffset += dstLineStride;
                    srcOffset += srcLineStride;
                }
            }
        }
    }

    private void ushortLoop(RasterAccessor src, RasterAccessor dst, RasterAccessor roi, RandomIter roiIter) {

        // Setup of the initial parameters
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        short[][] dstData = dst.getShortDataArrays();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        short[][] srcData = src.getShortDataArrays();

        int srcX = src.getX();
        int srcY = src.getY();

        final byte[] roiDataArray;
        final int roiLineStride;
        final int roiDataLength;

        // If ROI RasterAccessor is used, some parameters must be set
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiLineStride = roi.getScanlineStride();
            roiDataLength = roiDataArray.length;
        } else {
            roiDataArray = null;
            roiLineStride = 0;
            roiDataLength = 0;
        }

        // NO ROI NO NODATA
        if (caseA) {
            // Cycle on all the bands
            for (int b = 0; b < dstBands; b++) {
                // selection of the rescale parameters
                double scale = scaleFactors[b];
                double offset = offsetArray[b];
                // creation of the line offsets
                int dstLineOffset = dstBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;
                    // update of the line offsets
                    dstLineOffset += dstLineStride;
                    srcLineOffset += srcLineStride;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // Rescale operation
                        dstData[b][dstPixelOffset] = ImageUtil
                                .clampRoundUShort((srcData[b][srcPixelOffset] & 0xFFFF) * scale
                                        + offset);
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
            // ROI WITHOUT NODATA
        } else if (caseB) {
            // ROI RASTERACCESSOR USED
            if (useROIAccessor) {
                // Initial offsets
                int dstOffset = 0;
                int srcOffset = 0;
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstOffset;
                    int srcPixelOffset = srcOffset;
                    // Selection of the y position in the ROI array
                    int posyROI = y * roiLineStride;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // ROI index position
                        int windex = x + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // If the value is inside the ROI, then the Rescale operation is executed, else
                        // destination No Data is returned
                        if (w != 0) {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                // Selection of the rescale parameters
                                double scale = scaleFactors[b];
                                double offset = offsetArray[b];
                                // Rescale operation
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                        .clampRoundUShort((srcData[b][srcPixelOffset
                                                + srcBandOffsets[b]] & 0xFFFF)
                                                * scale + offset);
                            }
                        } else {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                    // update of the initial offsets
                    dstOffset += dstLineStride;
                    srcOffset += srcLineStride;
                }
                // ROI RASTERACCESSOR NOT USED
            } else {
                // Initial offsets
                int dstOffset = 0;
                int srcOffset = 0;
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstOffset;
                    int srcPixelOffset = srcOffset;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        // Check if the pixel is inside ROI
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0);
                            // If the pixel is inside the ROI, then the Rescale operation is executed, else
                            // destination No Data is returned
                            if (w != 0) {
                                // Cycle on all the bands
                                for (int b = 0; b < dstBands; b++) {
                                    // Selection of the rescale parameters
                                    double scale = scaleFactors[b];
                                    double offset = offsetArray[b];
                                    // Rescale operation
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                            .clampRoundUShort((srcData[b][srcPixelOffset
                                                    + srcBandOffsets[b]] & 0xFFFF)
                                                    * scale + offset);
                                }
                            } else {
                                // Cycle on all the bands
                                for (int b = 0; b < dstBands; b++) {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataShort;
                                }
                            }
                        } else {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                    // update of the initial offsets
                    dstOffset += dstLineStride;
                    srcOffset += srcLineStride;
                }
            }
            // NODATA WITHOUT ROI
        } else if (caseC) {
            // Cycle on all the bands
            for (int b = 0; b < dstBands; b++) {
                // selection of the rescale parameters
                double scale = scaleFactors[b];
                double offset = offsetArray[b];
                // creation of the line offsets
                int dstLineOffset = dstBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];
                // Selection of the input band array
                short[] bandDataIn = srcData[b];
                // Selection of the output band array
                short[] bandDataOut = dstData[b];
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;
                    // update of the line offsets
                    dstLineOffset += dstLineStride;
                    srcLineOffset += srcLineStride;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // Selection of the value to calculate
                        short value = bandDataIn[srcPixelOffset];
                        // Check if the value is not a NoData
                        if (!noData.contains(value)) {
                            // Rescale operation
                            bandDataOut[dstPixelOffset] = ImageUtil
                                    .clampRoundUShort((value & 0xFFFF) * scale + offset);
                        } else {
                            // Else, destination No Data is set
                            bandDataOut[dstPixelOffset] = destinationNoDataShort;
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
            // ROI AND NODATA
        } else {
            // ROI RASTERACCESSOR USED
            if (useROIAccessor) {
                // Initial offsets
                int dstOffset = 0;
                int srcOffset = 0;
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstOffset;
                    int srcPixelOffset = srcOffset;
                    // Selection of the y position in the ROI array
                    int posyROI = y * roiLineStride;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // ROI index position
                        int windex = x + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // If the value is inside the ROI, then the Rescale operation is executed, else
                        // destination No Data is returned
                        if (w != 0) {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                // Selection of the value to calculate
                                short value = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                                // Check if the value is not a NoData
                                if (!noData.contains(value)) {
                                    // Rescale operation
                                    // Selection of the rescale parameters
                                    double scale = scaleFactors[b];
                                    double offset = offsetArray[b];
                                    // Rescale operation
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                            .clampRoundUShort((value & 0xFFFF) * scale + offset);
                                } else {
                                    // Else, destination No Data is set
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataShort;
                                }
                            }
                        } else {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                    // update of the initial offsets
                    dstOffset += dstLineStride;
                    srcOffset += srcLineStride;
                }
                // ROI RASTERACCESSOR NOT USED
            } else {
                // Initial offsets
                int dstOffset = 0;
                int srcOffset = 0;
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstOffset;
                    int srcPixelOffset = srcOffset;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        // Check if the pixel is inside ROI
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0);
                            // If the pixel is inside the ROI, then the Rescale operation is executed, else
                            // destination No Data is returned
                            if (w != 0) {
                                // Cycle on all the bands
                                for (int b = 0; b < dstBands; b++) {
                                    // Selection of the value to calculate
                                    short value = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                                    // Check if the value is not a NoData
                                    if (!noData.contains(value)) {
                                        // Rescale operation
                                        // Selection of the rescale parameters
                                        double scale = scaleFactors[b];
                                        double offset = offsetArray[b];
                                        // Rescale operation
                                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                                .clampRoundUShort((value & 0xFFFF) * scale + offset);
                                    } else {
                                        // Else, destination No Data is set
                                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataShort;
                                    }
                                }
                            } else {
                                // Cycle on all the bands
                                for (int b = 0; b < dstBands; b++) {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataShort;
                                }
                            }
                        } else {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                    // update of the initial offsets
                    dstOffset += dstLineStride;
                    srcOffset += srcLineStride;
                }
            }
        }
    }

    private void shortLoop(RasterAccessor src, RasterAccessor dst, RasterAccessor roi, RandomIter roiIter) {

        // Setup of the initial parameters
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        short[][] dstData = dst.getShortDataArrays();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        short[][] srcData = src.getShortDataArrays();

        int srcX = src.getX();
        int srcY = src.getY();

        final byte[] roiDataArray;
        final int roiLineStride;
        final int roiDataLength;

        // If ROI RasterAccessor is used, some parameters must be set
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiLineStride = roi.getScanlineStride();
            roiDataLength = roiDataArray.length;
        } else {
            roiDataArray = null;
            roiLineStride = 0;
            roiDataLength = 0;
        }

        // NO ROI NO NODATA
        if (caseA) {
            // Cycle on all the bands
            for (int b = 0; b < dstBands; b++) {
                // selection of the rescale parameters
                double scale = scaleFactors[b];
                double offset = offsetArray[b];
                // creation of the line offsets
                int dstLineOffset = dstBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;
                    // update of the line offsets
                    dstLineOffset += dstLineStride;
                    srcLineOffset += srcLineStride;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // Rescale operation
                        dstData[b][dstPixelOffset] = ImageUtil
                                .clampRoundShort((srcData[b][srcPixelOffset]) * scale + offset);
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
            // ROI WITHOUT NODATA
        } else if (caseB) {
            // ROI RASTERACCESSOR USED
            if (useROIAccessor) {
                // Initial offsets
                int dstOffset = 0;
                int srcOffset = 0;
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstOffset;
                    int srcPixelOffset = srcOffset;
                    // Selection of the y position in the ROI array
                    int posyROI = y * roiLineStride;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // ROI index position
                        int windex = x + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // If the value is inside the ROI, then the Rescale operation is executed, else
                        // destination No Data is returned
                        if (w != 0) {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                // Selection of the rescale parameters
                                double scale = scaleFactors[b];
                                double offset = offsetArray[b];
                                // Rescale operation
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                        .clampRoundShort((srcData[b][srcPixelOffset
                                                + srcBandOffsets[b]])
                                                * scale + offset);
                            }
                        } else {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                    // update of the initial offsets
                    dstOffset += dstLineStride;
                    srcOffset += srcLineStride;
                }
                // ROI RASTERACCESSOR NOT USED
            } else {
                // Initial offsets
                int dstOffset = 0;
                int srcOffset = 0;
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstOffset;
                    int srcPixelOffset = srcOffset;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        // Check if the pixel is inside ROI
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0);
                            // If the pixel is inside the ROI, then the Rescale operation is executed, else
                            // destination No Data is returned
                            if (w != 0) {
                                // Cycle on all the bands
                                for (int b = 0; b < dstBands; b++) {
                                    // Selection of the rescale parameters
                                    double scale = scaleFactors[b];
                                    double offset = offsetArray[b];
                                    // Rescale operation
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                            .clampRoundShort((srcData[b][srcPixelOffset
                                                    + srcBandOffsets[b]])
                                                    * scale + offset);
                                }
                            } else {
                                // Cycle on all the bands
                                for (int b = 0; b < dstBands; b++) {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataShort;
                                }
                            }
                        } else {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                    // update of the initial offsets
                    dstOffset += dstLineStride;
                    srcOffset += srcLineStride;
                }
            }
            // NODATA WITHOUT ROI
        } else if (caseC) {
            // Cycle on all the bands
            for (int b = 0; b < dstBands; b++) {
                // selection of the rescale parameters
                double scale = scaleFactors[b];
                double offset = offsetArray[b];
                // creation of the line offsets
                int dstLineOffset = dstBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];
                // Selection of the input band array
                short[] bandDataIn = srcData[b];
                // Selection of the output band array
                short[] bandDataOut = dstData[b];
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;
                    // update of the line offsets
                    dstLineOffset += dstLineStride;
                    srcLineOffset += srcLineStride;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // Selection of the value to calculate
                        short value = bandDataIn[srcPixelOffset];
                        // Check if the value is not a NoData
                        if (!noData.contains(value)) {
                            // Rescale operation
                            bandDataOut[dstPixelOffset] = ImageUtil.clampRoundShort((value) * scale
                                    + offset);
                        } else {
                            // Else, destination No Data is set
                            bandDataOut[dstPixelOffset] = destinationNoDataShort;
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }

            // ROI AND NODATA
        } else {
            // ROI RASTERACCESSOR USED
            if (useROIAccessor) {
                // Initial offsets
                int dstOffset = 0;
                int srcOffset = 0;
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstOffset;
                    int srcPixelOffset = srcOffset;
                    // Selection of the y position in the ROI array
                    int posyROI = y * roiLineStride;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // ROI index position
                        int windex = x + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // If the value is inside the ROI, then the Rescale operation is executed, else
                        // destination No Data is returned
                        if (w != 0) {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                // Selection of the value to calculate
                                short value = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                                // Check if the value is not a NoData
                                if (!noData.contains(value)) {
                                    // Rescale operation
                                    // Selection of the rescale parameters
                                    double scale = scaleFactors[b];
                                    double offset = offsetArray[b];
                                    // Rescale operation
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                            .clampRoundShort((value) * scale + offset);
                                } else {
                                    // Else, destination No Data is set
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataShort;
                                }
                            }
                        } else {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                    // update of the initial offsets
                    dstOffset += dstLineStride;
                    srcOffset += srcLineStride;
                }
                // ROI RASTERACCESSOR NOT USED
            } else {
                // Initial offsets
                int dstOffset = 0;
                int srcOffset = 0;
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstOffset;
                    int srcPixelOffset = srcOffset;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        // Check if the pixel is inside ROI
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0);
                            // If the pixel is inside the ROI, then the Rescale operation is executed, else
                            // destination No Data is returned
                            if (w != 0) {
                                // Cycle on all the bands
                                for (int b = 0; b < dstBands; b++) {
                                    // Selection of the value to calculate
                                    short value = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                                    // Check if the value is not a NoData
                                    if (!noData.contains(value)) {
                                        // Rescale operation
                                        // Selection of the rescale parameters
                                        double scale = scaleFactors[b];
                                        double offset = offsetArray[b];
                                        // Rescale operation
                                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                                .clampRoundShort((value) * scale + offset);
                                    } else {
                                        // Else, destination No Data is set
                                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataShort;
                                    }
                                }
                            } else {
                                // Cycle on all the bands
                                for (int b = 0; b < dstBands; b++) {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataShort;
                                }
                            }
                        } else {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataShort;
                            }
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                    // update of the initial offsets
                    dstOffset += dstLineStride;
                    srcOffset += srcLineStride;
                }
            }
        }
    }

    private void intLoop(RasterAccessor src, RasterAccessor dst, RasterAccessor roi, RandomIter roiIter) {

        // Setup of the initial parameters
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        int[][] dstData = dst.getIntDataArrays();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        int[][] srcData = src.getIntDataArrays();

        int srcX = src.getX();
        int srcY = src.getY();

        final byte[] roiDataArray;
        final int roiLineStride;
        final int roiDataLength;

        // If ROI RasterAccessor is used, some parameters must be set
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiLineStride = roi.getScanlineStride();
            roiDataLength = roiDataArray.length;
        } else {
            roiDataArray = null;
            roiLineStride = 0;
            roiDataLength = 0;
        }

        // NO ROI NO NODATA
        if (caseA) {
            // Cycle on all the bands
            for (int b = 0; b < dstBands; b++) {
                // selection of the rescale parameters
                double scale = scaleFactors[b];
                double offset = offsetArray[b];
                // creation of the line offsets
                int dstLineOffset = dstBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;
                    // update of the line offsets
                    dstLineOffset += dstLineStride;
                    srcLineOffset += srcLineStride;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // Rescale operation
                        dstData[b][dstPixelOffset] = ImageUtil
                                .clampRoundInt((srcData[b][srcPixelOffset]) * scale + offset);
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
            // ROI WITHOUT NODATA
        } else if (caseB) {
            // ROI RASTERACCESSOR USED
            if (useROIAccessor) {
                // Initial offsets
                int dstOffset = 0;
                int srcOffset = 0;
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstOffset;
                    int srcPixelOffset = srcOffset;
                    // Selection of the y position in the ROI array
                    int posyROI = y * roiLineStride;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // ROI index position
                        int windex = x + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // If the value is inside the ROI, then the Rescale operation is executed, else
                        // destination No Data is returned
                        if (w != 0) {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                // Selection of the rescale parameters
                                double scale = scaleFactors[b];
                                double offset = offsetArray[b];
                                // Rescale operation
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                        .clampRoundInt((srcData[b][srcPixelOffset
                                                + srcBandOffsets[b]])
                                                * scale + offset);
                            }
                        } else {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataInt;
                            }
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                    // update of the initial offsets
                    dstOffset += dstLineStride;
                    srcOffset += srcLineStride;
                }
                // ROI RASTERACCESSOR NOT USED
            } else {
                // Initial offsets
                int dstOffset = 0;
                int srcOffset = 0;
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstOffset;
                    int srcPixelOffset = srcOffset;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        // Check if the pixel is inside ROI
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0);
                            // If the pixel is inside the ROI, then the Rescale operation is executed, else
                            // destination No Data is returned
                            if (w != 0) {
                                // Cycle on all the bands
                                for (int b = 0; b < dstBands; b++) {
                                    // Selection of the rescale parameters
                                    double scale = scaleFactors[b];
                                    double offset = offsetArray[b];
                                    // Rescale operation
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                            .clampRoundInt((srcData[b][srcPixelOffset
                                                    + srcBandOffsets[b]])
                                                    * scale + offset);
                                }
                            } else {
                                // Cycle on all the bands
                                for (int b = 0; b < dstBands; b++) {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataInt;
                                }
                            }
                        } else {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataInt;
                            }
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                    // update of the initial offsets
                    dstOffset += dstLineStride;
                    srcOffset += srcLineStride;
                }
            }
            // NODATA WITHOUT ROI
        } else if (caseC) {
            // Cycle on all the bands
            for (int b = 0; b < dstBands; b++) {
                // selection of the rescale parameters
                double scale = scaleFactors[b];
                double offset = offsetArray[b];
                // creation of the line offsets
                int dstLineOffset = dstBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];
                // Selection of the input band array
                int[] bandDataIn = srcData[b];
                // Selection of the output band array
                int[] bandDataOut = dstData[b];
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;
                    // update of the line offsets
                    dstLineOffset += dstLineStride;
                    srcLineOffset += srcLineStride;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // Selection of the value to calculate
                        int value = bandDataIn[srcPixelOffset];
                        // Check if the value is not a NoData
                        if (!noData.contains(value)) {
                            // Rescale operation
                            bandDataOut[dstPixelOffset] = ImageUtil.clampRoundInt((value) * scale
                                    + offset);
                        } else {
                            // Else, destination No Data is set
                            bandDataOut[dstPixelOffset] = destinationNoDataInt;
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
            // ROI AND NODATA
        } else {
            // ROI RASTERACCESSOR USED
            if (useROIAccessor) {
                // Initial offsets
                int dstOffset = 0;
                int srcOffset = 0;
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstOffset;
                    int srcPixelOffset = srcOffset;
                    // Selection of the y position in the ROI array
                    int posyROI = y * roiLineStride;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // ROI index position
                        int windex = x + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // If the value is inside the ROI, then the Rescale operation is executed, else
                        // destination No Data is returned
                        if (w != 0) {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                // Selection of the value to calculate
                                int value = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                                // Check if the value is not a NoData
                                if (!noData.contains(value)) {
                                    // Rescale operation
                                    // Selection of the rescale parameters
                                    double scale = scaleFactors[b];
                                    double offset = offsetArray[b];
                                    // Rescale operation
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                            .clampRoundInt((value) * scale + offset);
                                } else {
                                    // Else, destination No Data is set
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataInt;
                                }
                            }
                        } else {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataInt;
                            }
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                    // update of the initial offsets
                    dstOffset += dstLineStride;
                    srcOffset += srcLineStride;
                }
                // ROI RASTERACCESSOR NOT USED
            } else {
                // Initial offsets
                int dstOffset = 0;
                int srcOffset = 0;
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstOffset;
                    int srcPixelOffset = srcOffset;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        // Check if the pixel is inside ROI
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0);
                            // If the pixel is inside the ROI, then the Rescale operation is executed, else
                            // destination No Data is returned
                            if (w != 0) {
                                // Cycle on all the bands
                                for (int b = 0; b < dstBands; b++) {
                                    // Selection of the value to calculate
                                    int value = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                                    // Check if the value is not a NoData
                                    if (!noData.contains(value)) {
                                        // Rescale operation
                                        // Selection of the rescale parameters
                                        double scale = scaleFactors[b];
                                        double offset = offsetArray[b];
                                        // Rescale operation
                                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = ImageUtil
                                                .clampRoundInt((value) * scale + offset);
                                    } else {
                                        // Else, destination No Data is set
                                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataInt;
                                    }
                                }
                            } else {
                                // Cycle on all the bands
                                for (int b = 0; b < dstBands; b++) {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataInt;
                                }
                            }
                        } else {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataInt;
                            }
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                    // update of the initial offsets
                    dstOffset += dstLineStride;
                    srcOffset += srcLineStride;
                }
            }
        }
    }

    private void floatLoop(RasterAccessor src, RasterAccessor dst, RasterAccessor roi, RandomIter roiIter) {

        // Setup of the initial parameters
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        float[][] dstData = dst.getFloatDataArrays();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        float[][] srcData = src.getFloatDataArrays();

        int srcX = src.getX();
        int srcY = src.getY();

        final byte[] roiDataArray;
        final int roiLineStride;
        final int roiDataLength;

        // If ROI RasterAccessor is used, some parameters must be set
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiLineStride = roi.getScanlineStride();
            roiDataLength = roiDataArray.length;
        } else {
            roiDataArray = null;
            roiLineStride = 0;
            roiDataLength = 0;
        }

        // NO ROI NO NODATA
        if (caseA) {
            // Cycle on all the bands
            for (int b = 0; b < dstBands; b++) {
                // selection of the rescale parameters
                double scale = scaleFactors[b];
                double offset = offsetArray[b];
                // creation of the line offsets
                int dstLineOffset = dstBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;
                    // update of the line offsets
                    dstLineOffset += dstLineStride;
                    srcLineOffset += srcLineStride;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // Rescale operation
                        dstData[b][dstPixelOffset] = (float) ((srcData[b][srcPixelOffset]) * scale + offset);
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
            // ROI WITHOUT NODATA
        } else if (caseB) {
            // ROI RASTERACCESSOR USED
            if (useROIAccessor) {
                // Initial offsets
                int dstOffset = 0;
                int srcOffset = 0;
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstOffset;
                    int srcPixelOffset = srcOffset;
                    // Selection of the y position in the ROI array
                    int posyROI = y * roiLineStride;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // ROI index position
                        int windex = x + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // If the value is inside the ROI, then the Rescale operation is executed, else
                        // destination No Data is returned
                        if (w != 0) {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                // Selection of the rescale parameters
                                double scale = scaleFactors[b];
                                double offset = offsetArray[b];
                                // Rescale operation
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = (float) ((srcData[b][srcPixelOffset
                                        + srcBandOffsets[b]])
                                        * scale + offset);
                            }
                        } else {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataFloat;
                            }
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                    // update of the initial offsets
                    dstOffset += dstLineStride;
                    srcOffset += srcLineStride;
                }
                // ROI RASTERACCESSOR NOT USED
            } else {
                // Initial offsets
                int dstOffset = 0;
                int srcOffset = 0;
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstOffset;
                    int srcPixelOffset = srcOffset;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        // Check if the pixel is inside ROI
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0);
                            // If the pixel is inside the ROI, then the Rescale operation is executed, else
                            // destination No Data is returned
                            if (w != 0) {
                                // Cycle on all the bands
                                for (int b = 0; b < dstBands; b++) {
                                    // Selection of the rescale parameters
                                    double scale = scaleFactors[b];
                                    double offset = offsetArray[b];
                                    // Rescale operation
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = (float) ((srcData[b][srcPixelOffset
                                            + srcBandOffsets[b]])
                                            * scale + offset);
                                }
                            } else {
                                // Cycle on all the bands
                                for (int b = 0; b < dstBands; b++) {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataFloat;
                                }
                            }
                        } else {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataFloat;
                            }
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                    // update of the initial offsets
                    dstOffset += dstLineStride;
                    srcOffset += srcLineStride;
                }
            }
            // NODATA WITHOUT ROI
        } else if (caseC) {
            // Cycle on all the bands
            for (int b = 0; b < dstBands; b++) {
                // selection of the rescale parameters
                double scale = scaleFactors[b];
                double offset = offsetArray[b];
                // creation of the line offsets
                int dstLineOffset = dstBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];
                // Selection of the input band array
                float[] bandDataIn = srcData[b];
                // Selection of the output band array
                float[] bandDataOut = dstData[b];
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;
                    // update of the line offsets
                    dstLineOffset += dstLineStride;
                    srcLineOffset += srcLineStride;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // Selection of the value to calculate
                        float value = bandDataIn[srcPixelOffset];
                        // Check if the value is not a NoData
                        if (!noData.contains(value)) {
                            // Rescale operation
                            bandDataOut[dstPixelOffset] = (float) (value * scale + offset);
                        } else {
                            // Else, destination No Data is set
                            bandDataOut[dstPixelOffset] = destinationNoDataFloat;
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
            // ROI AND NODATA
        } else {
            // ROI RASTERACCESSOR USED
            if (useROIAccessor) {
                // Initial offsets
                int dstOffset = 0;
                int srcOffset = 0;
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstOffset;
                    int srcPixelOffset = srcOffset;
                    // Selection of the y position in the ROI array
                    int posyROI = y * roiLineStride;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // ROI index position
                        int windex = x + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // If the value is inside the ROI, then the Rescale operation is executed, else
                        // destination No Data is returned
                        if (w != 0) {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                // Selection of the value to calculate
                                float value = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                                // Check if the value is not a NoData
                                if (!noData.contains(value)) {
                                    // Rescale operation
                                    // Selection of the rescale parameters
                                    double scale = scaleFactors[b];
                                    double offset = offsetArray[b];
                                    // Rescale operation
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = (float) (value
                                            * scale + offset);
                                } else {
                                    // Else, destination No Data is set
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataFloat;
                                }
                            }
                        } else {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataFloat;
                            }
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                    // update of the initial offsets
                    dstOffset += dstLineStride;
                    srcOffset += srcLineStride;
                }
                // ROI RASTERACCESSOR NOT USED
            } else {
                // Initial offsets
                int dstOffset = 0;
                int srcOffset = 0;
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstOffset;
                    int srcPixelOffset = srcOffset;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        // Check if the pixel is inside ROI
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0);
                            // If the pixel is inside the ROI, then the Rescale operation is executed, else
                            // destination No Data is returned
                            if (w != 0) {
                                // Cycle on all the bands
                                for (int b = 0; b < dstBands; b++) {
                                    // Selection of the value to calculate
                                    float value = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                                    // Check if the value is not a NoData
                                    if (!noData.contains(value)) {
                                        // Rescale operation
                                        // Selection of the rescale parameters
                                        double scale = scaleFactors[b];
                                        double offset = offsetArray[b];
                                        // Rescale operation
                                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = (float) (value
                                                * scale + offset);
                                    } else {
                                        // Else, destination No Data is set
                                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataFloat;
                                    }
                                }
                            } else {
                                // Cycle on all the bands
                                for (int b = 0; b < dstBands; b++) {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataFloat;
                                }
                            }
                        } else {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataFloat;
                            }
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                    // update of the initial offsets
                    dstOffset += dstLineStride;
                    srcOffset += srcLineStride;
                }
            }
        }
    }

    private void doubleLoop(RasterAccessor src, RasterAccessor dst, RasterAccessor roi, RandomIter roiIter) {

        // Setup of the initial parameters
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        double[][] dstData = dst.getDoubleDataArrays();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        double[][] srcData = src.getDoubleDataArrays();

        int srcX = src.getX();
        int srcY = src.getY();

        final byte[] roiDataArray;
        final int roiLineStride;
        final int roiDataLength;

        // If ROI RasterAccessor is used, some parameters must be set
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiLineStride = roi.getScanlineStride();
            roiDataLength = roiDataArray.length;
        } else {
            roiDataArray = null;
            roiLineStride = 0;
            roiDataLength = 0;
        }

        // NO ROI NO NODATA
        if (caseA) {
            // Cycle on all the bands
            for (int b = 0; b < dstBands; b++) {
                // selection of the rescale parameters
                double scale = scaleFactors[b];
                double offset = offsetArray[b];
                // creation of the line offsets
                int dstLineOffset = dstBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;
                    // update of the line offsets
                    dstLineOffset += dstLineStride;
                    srcLineOffset += srcLineStride;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // Rescale operation
                        dstData[b][dstPixelOffset] = (srcData[b][srcPixelOffset]) * scale + offset;
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
            // ROI WITHOUT NODATA
        } else if (caseB) {
            // ROI RASTERACCESSOR USED
            if (useROIAccessor) {
                // Initial offsets
                int dstOffset = 0;
                int srcOffset = 0;
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstOffset;
                    int srcPixelOffset = srcOffset;
                    // Selection of the y position in the ROI array
                    int posyROI = y * roiLineStride;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // ROI index position
                        int windex = x + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // If the value is inside the ROI, then the Rescale operation is executed, else
                        // destination No Data is returned
                        if (w != 0) {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                // Selection of the rescale parameters
                                double scale = scaleFactors[b];
                                double offset = offsetArray[b];
                                // Rescale operation
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = (srcData[b][srcPixelOffset
                                        + srcBandOffsets[b]])
                                        * scale + offset;
                            }
                        } else {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataDouble;
                            }
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                    // update of the initial offsets
                    dstOffset += dstLineStride;
                    srcOffset += srcLineStride;
                }
                // ROI RASTERACCESSOR NOT USED
            } else {
                // Initial offsets
                int dstOffset = 0;
                int srcOffset = 0;
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstOffset;
                    int srcPixelOffset = srcOffset;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        // Check if the pixel is inside ROI
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0);
                            // If the pixel is inside the ROI, then the Rescale operation is executed, else
                            // destination No Data is returned
                            if (w != 0) {
                                // Cycle on all the bands
                                for (int b = 0; b < dstBands; b++) {
                                    // Selection of the rescale parameters
                                    double scale = scaleFactors[b];
                                    double offset = offsetArray[b];
                                    // Rescale operation
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = (srcData[b][srcPixelOffset
                                            + srcBandOffsets[b]])
                                            * scale + offset;
                                }
                            } else {
                                // Cycle on all the bands
                                for (int b = 0; b < dstBands; b++) {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataDouble;
                                }
                            }
                        } else {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataDouble;
                            }
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                    // update of the initial offsets
                    dstOffset += dstLineStride;
                    srcOffset += srcLineStride;
                }
            }
            // NODATA WITHOUT ROI
        } else if (caseC) {
            // Cycle on all the bands
            for (int b = 0; b < dstBands; b++) {
                // selection of the rescale parameters
                double scale = scaleFactors[b];
                double offset = offsetArray[b];
                // creation of the line offsets
                int dstLineOffset = dstBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;
                    // update of the line offsets
                    dstLineOffset += dstLineStride;
                    srcLineOffset += srcLineStride;
                    // Selection of the input band array
                    double[] bandDataIn = srcData[b];
                    // Selection of the output band array
                    double[] bandDataOut = dstData[b];
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // Selection of the value to calculate
                        double value = bandDataIn[srcPixelOffset];
                        // Check if the value is not a NoData
                        if (!noData.contains(value)) {
                            // Rescale operation
                            bandDataOut[dstPixelOffset] = (value * scale) + offset;
                        } else {
                            // Else, destination No Data is set
                            bandDataOut[dstPixelOffset] = destinationNoDataDouble;
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                }
            }
            // ROI AND NODATA
        } else {
            // ROI RASTERACCESSOR USED
            if (useROIAccessor) {
                // Initial offsets
                int dstOffset = 0;
                int srcOffset = 0;
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstOffset;
                    int srcPixelOffset = srcOffset;
                    // Selection of the y position in the ROI array
                    int posyROI = y * roiLineStride;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // ROI index position
                        int windex = x + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // If the value is inside the ROI, then the Rescale operation is executed, else
                        // destination No Data is returned
                        if (w != 0) {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                // Selection of the value to calculate
                                double value = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                                // Check if the value is not a NoData
                                if (!noData.contains(value)) {
                                    // Rescale operation
                                    // Selection of the rescale parameters
                                    double scale = scaleFactors[b];
                                    double offset = offsetArray[b];
                                    // Rescale operation
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = (value * scale)
                                            + offset;
                                } else {
                                    // Else, destination No Data is set
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataDouble;
                                }
                            }
                        } else {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataDouble;
                            }
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                    // update of the initial offsets
                    dstOffset += dstLineStride;
                    srcOffset += srcLineStride;
                }
                // ROI RASTERACCESSOR NOT USED
            } else {
                // Initial offsets
                int dstOffset = 0;
                int srcOffset = 0;
                // Cycle on the y-axis
                for (int y = 0; y < dstHeight; y++) {
                    // creation of the pixel offsets
                    int dstPixelOffset = dstOffset;
                    int srcPixelOffset = srcOffset;
                    // Cycle on the x-axis
                    for (int x = 0; x < dstWidth; x++) {
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        // Check if the pixel is inside ROI
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0);
                            // If the pixel is inside the ROI, then the Rescale operation is executed, else
                            // destination No Data is returned
                            if (w != 0) {
                                // Cycle on all the bands
                                for (int b = 0; b < dstBands; b++) {
                                    // Selection of the value to calculate
                                    double value = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                                    // Check if the value is not a NoData
                                    if (!noData.contains(value)) {
                                        // Rescale operation
                                        // Selection of the rescale parameters
                                        double scale = scaleFactors[b];
                                        double offset = offsetArray[b];
                                        // Rescale operation
                                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = (value * scale)
                                                + offset;
                                    } else {
                                        // Else, destination No Data is set
                                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataDouble;
                                    }
                                }
                            } else {
                                // Cycle on all the bands
                                for (int b = 0; b < dstBands; b++) {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataDouble;
                                }
                            }
                        } else {
                            // Cycle on all the bands
                            for (int b = 0; b < dstBands; b++) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destinationNoDataDouble;
                            }
                        }
                        // update of the pixel offsets
                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;
                    }
                    // update of the initial offsets
                    dstOffset += dstLineStride;
                    srcOffset += srcLineStride;
                }
            }
        }
    }
    
    @Override
    public synchronized void dispose() {
        if(srcROIImgExt != null) {
            srcROIImgExt.dispose();
        }
        super.dispose();
    }
}
