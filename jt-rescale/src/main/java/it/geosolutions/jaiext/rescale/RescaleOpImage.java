package it.geosolutions.jaiext.rescale;

import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Map;
import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.PointOpImage;
import javax.media.jai.ROI;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.iterator.RandomIter;
import com.sun.media.jai.util.ImageUtil;

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

    /** Random Iterator used iterating on the ROI data */
    private RandomIter roiIter;

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

    public RescaleOpImage(RenderedImage source, ImageLayout layout, Map configuration,
            double[] valueScale, double[] valueOffsets, double destinationNoData, ROI roi,
            Range noData, boolean useROIAccessor) {
        super(source, layout, configuration, true);

        // Selection of the band number
        int numBands = getSampleModel().getNumBands();

        // Check if the constants number is equal to the band number
        // If they are not equal the first constant is used for all bands
        if (valueScale.length < numBands) {
            this.scaleFactors = new double[numBands];
            for (int i = 0; i < numBands; i++) {
                this.scaleFactors[i] = valueScale[0];
            }
        } else {
            // Else the constants are copied
            this.scaleFactors = valueScale;
        }

        // Check if the offsets number is equal to the band number
        // If they are not equal the first offset is used for all bands
        if (valueOffsets.length < numBands) {
            this.offsetArray = new double[numBands];
            for (int i = 0; i < numBands; i++) {
                this.offsetArray[i] = valueOffsets[0];
            }
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
            // ROI image bounds calculation
            final Rectangle rect = new Rectangle(srcROIImage.getBounds());
            // Roi image data store
            Raster data = srcROIImage.getData(rect);
            // Creation of a RandomIterator for selecting random pixel inside the ROI
            roiIter = RandomIterFactory.create(data, data.getBounds(), false, true);
            // ROI bounds are saved
            roiBounds = srcROIImage.getBounds();
            // The useRoiAccessor parameter is set
            this.useROIAccessor = useROIAccessor;
        } else {
            hasROI = false;
            this.useROIAccessor = false;
            roiBounds = null;
            roiIter = null;
            srcROIImage = null;
        }

        // Image dataType
        int dataType = source.getSampleModel().getDataType();

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
        if (useROIAccessor) {
            Raster roiRaster = srcROIImage.getExtendedData(srcRect, ROI_EXTENDER);
            // creation of the rasterAccessor
            roiAccessor = new RasterAccessor(roiRaster, srcRect, RasterAccessor.findCompatibleTags(
                    new RenderedImage[] { srcROIImage }, srcROIImage)[0],
                    srcROIImage.getColorModel());
        }

        int dataType = destAccessor.getDataType();

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(srcAccessor, destAccessor, roiAccessor);
            break;
        case DataBuffer.TYPE_USHORT:
            ushortLoop(srcAccessor, destAccessor, roiAccessor);
            break;
        case DataBuffer.TYPE_SHORT:
            shortLoop(srcAccessor, destAccessor, roiAccessor);
            break;
        case DataBuffer.TYPE_INT:
            intLoop(srcAccessor, destAccessor, roiAccessor);
            break;
        case DataBuffer.TYPE_FLOAT:
            floatLoop(srcAccessor, destAccessor, roiAccessor);
            break;
        case DataBuffer.TYPE_DOUBLE:
            doubleLoop(srcAccessor, destAccessor, roiAccessor);
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

    private void byteLoop(RasterAccessor src, RasterAccessor dst, RasterAccessor roi) {

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
                    // Cycle on all the bands
                    for (int b = 0; b < dstBands; b++) {
                        // Selection of the value to calculate
                        byte value = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                        // Check if the value is not a NoData
                        if (booleanLookupTable[value & 0xFF]) {
                            // Rescale operation
                            byte[] clamp = byteRescaleTable[b];
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = clamp[value & 0xFF];
                        } else {
                            // Else, destination No Data is set
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
                                byte value = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                                // Check if the value is not a NoData
                                if (booleanLookupTable[value & 0xFF]) {
                                    // Rescale operation
                                    byte[] clamp = byteRescaleTable[b];
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = clamp[value & 0xFF];
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
                                    byte value = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                                    // Check if the value is not a NoData
                                    if (booleanLookupTable[value & 0xFF]) {
                                        // Rescale operation
                                        byte[] clamp = byteRescaleTable[b];
                                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = clamp[value & 0xFF];
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

    private void ushortLoop(RasterAccessor src, RasterAccessor dst, RasterAccessor roi) {

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
                                        .clampRoundUShort((srcData[b][srcPixelOffset + srcBandOffsets[b]] & 0xFFFF)
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
                                            .clampRoundUShort((srcData[b][srcPixelOffset + srcBandOffsets[b]] & 0xFFFF)
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
                    // update of the pixel offsets
                    dstPixelOffset += dstPixelStride;
                    srcPixelOffset += srcPixelStride;
                }
                // update of the initial offsets
                dstOffset += dstLineStride;
                srcOffset += srcLineStride;
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

    private void shortLoop(RasterAccessor src, RasterAccessor dst, RasterAccessor roi) {

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
                                        .clampRoundShort((srcData[b][srcPixelOffset + srcBandOffsets[b]]) * scale
                                                + offset);
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
                                            .clampRoundShort((srcData[b][srcPixelOffset + srcBandOffsets[b]]) * scale
                                                    + offset);
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
                    // update of the pixel offsets
                    dstPixelOffset += dstPixelStride;
                    srcPixelOffset += srcPixelStride;
                }
                // update of the initial offsets
                dstOffset += dstLineStride;
                srcOffset += srcLineStride;
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

    private void intLoop(RasterAccessor src, RasterAccessor dst, RasterAccessor roi) {

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
                                        .clampRoundInt((srcData[b][srcPixelOffset + srcBandOffsets[b]]) * scale
                                                + offset);
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
                                            .clampRoundInt((srcData[b][srcPixelOffset + srcBandOffsets[b]]) * scale
                                                    + offset);
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
                    // update of the pixel offsets
                    dstPixelOffset += dstPixelStride;
                    srcPixelOffset += srcPixelStride;
                }
                // update of the initial offsets
                dstOffset += dstLineStride;
                srcOffset += srcLineStride;
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

    private void floatLoop(RasterAccessor src, RasterAccessor dst, RasterAccessor roi) {

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
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = (float) ((srcData[b][srcPixelOffset + srcBandOffsets[b]])
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
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = (float) ((srcData[b][srcPixelOffset + srcBandOffsets[b]])
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
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = (float) (value * scale + offset);
                        } else {
                            // Else, destination No Data is set
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
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = (float) (value * scale + offset);
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
                                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = (float) (value * scale + offset);
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

    private void doubleLoop(RasterAccessor src, RasterAccessor dst, RasterAccessor roi) {

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
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = (srcData[b][srcPixelOffset + srcBandOffsets[b]]) * scale
                                        + offset;
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
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = (srcData[b][srcPixelOffset + srcBandOffsets[b]])
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
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = (value * scale) + offset;
                        } else {
                            // Else, destination No Data is set
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
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = (value * scale) + offset;
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
                                        dstData[b][dstPixelOffset + dstBandOffsets[b]] = (value * scale) + offset;
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
}
