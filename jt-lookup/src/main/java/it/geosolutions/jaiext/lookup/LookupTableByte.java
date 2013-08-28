package it.geosolutions.jaiext.lookup;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFactory;
import javax.media.jai.RasterFormatTag;

import org.jaitools.numeric.Range;

/** This class is an extension of the abstract class LookupTable handling byte data types */
public class LookupTableByte extends LookupTable {

    public LookupTableByte(byte[] data) {
        super(data);
    }

    public LookupTableByte(byte[] data, int offset) {
        super(data, offset);
    }

    public LookupTableByte(byte[][] data) {
        super(data);
    }

    public LookupTableByte(byte[][] data, int offset) {
        super(data, offset);
    }

    public LookupTableByte(byte[][] data, int[] offsets) {
        super(data, offsets);
    }

    public LookupTableByte(short[] data, boolean isUShort) {
        super(data, isUShort);
    }

    public LookupTableByte(short[] data, int offset, boolean isUShort) {
        super(data, offset, isUShort);
    }

    public LookupTableByte(short[][] data, boolean isUShort) {
        super(data, isUShort);
    }

    public LookupTableByte(short[][] data, int offset, boolean isUShort) {
        super(data, offset, isUShort);
    }

    public LookupTableByte(short[][] data, int[] offsets, boolean isUShort) {
        super(data, offsets, isUShort);
    }

    public LookupTableByte(int[] data) {
        super(data);
    }

    public LookupTableByte(int[] data, int offset) {
        super(data, offset);
    }

    public LookupTableByte(int[][] data) {
        super(data);
    }

    public LookupTableByte(int[][] data, int offset) {
        super(data, offset);
    }

    public LookupTableByte(int[][] data, int[] offsets) {
        super(data, offsets);
    }

    public LookupTableByte(float[] data) {
        super(data);
    }

    public LookupTableByte(float[] data, int offset) {
        super(data, offset);
    }

    public LookupTableByte(float[][] data) {
        super(data);
    }

    public LookupTableByte(float[][] data, int offset) {
        super(data, offset);
    }

    public LookupTableByte(float[][] data, int[] offsets) {
        super(data, offsets);
    }

    public LookupTableByte(double[] data) {
        super(data);
    }

    public LookupTableByte(double[] data, int offset) {
        super(data, offset);
    }

    public LookupTableByte(double[][] data) {
        super(data);
    }

    public LookupTableByte(double[][] data, int offset) {
        super(data, offset);
    }

    public LookupTableByte(double[][] data, int[] offsets) {
        super(data, offsets);
    }

    /**
     * Performs table lookup on a source Byte Raster, writing the result into a supplied WritableRaster. The destination must have a data type and
     * SampleModel appropriate to the results of the lookup operation. The table lookup operation is performed within a specified rectangle. If ROI or
     * no Data are present then they are taken into account.
     * 
     * <p>
     * The <code>dst</code> argument may be null, in which case a new WritableRaster is created using the appropriate SampleModel.
     * 
     * <p>
     * The rectangle of interest may be null, in which case the operation will be performed on the intersection of the source and destination bounding
     * rectangles.
     * 
     * @param source A Raster containing the source pixel data.
     * @param dst The WritableRaster to be computed, or null. If supplied, its data type and number of bands must be suitable for the source and
     *        lookup table.
     * @param rect The rectangle within the tile to be computed. If rect is null, the intersection of the source and destination bounds will be used.
     *        Otherwise, it will be clipped to the intersection of the source and destination bounds.
     */
    protected void lookup(Raster source, WritableRaster dst, Rectangle rect, Raster roi) {
        // Validate source.
        if (source == null) {
            throw new IllegalArgumentException("Source data must be present");
        }

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

        // Creation of the raster accessors for iterating on the source and destination tile
        int sTagID = RasterAccessor.findCompatibleTag(null, srcSampleModel);
        int dTagID = RasterAccessor.findCompatibleTag(null, dstSampleModel);

        RasterFormatTag sTag = new RasterFormatTag(srcSampleModel, sTagID);
        RasterFormatTag dTag = new RasterFormatTag(dstSampleModel, dTagID);

        RasterAccessor s = new RasterAccessor(source, rect, sTag, null);
        RasterAccessor d = new RasterAccessor(dst, rect, dTag, null);

        // Roi rasterAccessor initialization
        RasterAccessor roiAccessor = null;
        // ROI calculation only if the roi raster is present
        if (useROIAccessor) {
            // Get the source rectangle
            Rectangle srcRect = source.getBounds();
            // creation of the rasterAccessor
            roiAccessor = new RasterAccessor(roi, srcRect, RasterAccessor.findCompatibleTags(
                    new RenderedImage[] { srcROIImage }, srcROIImage)[0],
                    srcROIImage.getColorModel());
        }

        int srcNumBands = s.getNumBands();

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

        if (srcNumBands < dstNumBands) {
            int offset0 = srcBandOffsets[0];
            srcBandOffsets = new int[dstNumBands];
            for (int i = 0; i < dstNumBands; i++) {
                srcBandOffsets[i] = offset0;
            }
            byte[] bData0 = bSrcData[0];
            bSrcData = new byte[dstNumBands][];
            for (int i = 0; i < dstNumBands; i++) {
                bSrcData[i] = bData0;
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
            lookup(srcLineStride, srcPixelStride, srcBandOffsets, bSrcData, dstWidth, dstHeight,
                    dstNumBands, dstLineStride, dstPixelStride, dstBandOffsets, bDstData,
                    tblOffsets, bTblData, roiAccessor, rect);
            break;

        case DataBuffer.TYPE_USHORT:
            lookup(true, srcLineStride, srcPixelStride, srcBandOffsets, bSrcData, dstWidth,
                    dstHeight, dstNumBands, dstLineStride, dstPixelStride, dstBandOffsets,
                    sDstData, tblOffsets, sTblData, roiAccessor, rect);
        case DataBuffer.TYPE_SHORT:
            lookup(false, srcLineStride, srcPixelStride, srcBandOffsets, bSrcData, dstWidth,
                    dstHeight, dstNumBands, dstLineStride, dstPixelStride, dstBandOffsets,
                    sDstData, tblOffsets, sTblData, roiAccessor, rect);

            break;

        case DataBuffer.TYPE_INT:
            lookup(srcLineStride, srcPixelStride, srcBandOffsets, bSrcData, dstWidth, dstHeight,
                    dstNumBands, dstLineStride, dstPixelStride, dstBandOffsets, iDstData,
                    tblOffsets, iTblData, roiAccessor, rect);
            break;

        case DataBuffer.TYPE_FLOAT:
            lookup(srcLineStride, srcPixelStride, srcBandOffsets, bSrcData, dstWidth, dstHeight,
                    dstNumBands, dstLineStride, dstPixelStride, dstBandOffsets, fDstData,
                    tblOffsets, fTblData, roiAccessor, rect);
            break;

        case DataBuffer.TYPE_DOUBLE:
            lookup(srcLineStride, srcPixelStride, srcBandOffsets, bSrcData, dstWidth, dstHeight,
                    dstNumBands, dstLineStride, dstPixelStride, dstBandOffsets, dDstData,
                    tblOffsets, dTblData, roiAccessor, rect);
            break;
        }

        d.copyDataToRaster();
    }

    // byte to byte
    private void lookup(int srcLineStride, int srcPixelStride, int[] srcBandOffsets,
            byte[][] bSrcData, int dstWidth, int dstHeight, int dstNumBands, int dstLineStride,
            int dstPixelStride, int[] dstBandOffsets, byte[][] bDstData, int[] tblOffsets,
            byte[][] bTblData, RasterAccessor roi, Rectangle destRect) {

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

        Range<Byte> rangeND = (Range<Byte>) noData;

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;
        final boolean caseNull = bTblData == null;

        if (caseA) {
            if (caseNull) {
                for (int b = 0; b < dstNumBands; b++) {
                    final byte[] s = bSrcData[b];
                    final byte[] d = bDstData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];

                    for (int h = 0; h < dstHeight; h++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int w = 0; w < dstWidth; w++) {
                            d[dstPixelOffset] = (byte) (data.getElem(b, (s[srcPixelOffset] & 0xFF)) & 0xFF);

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    final byte[] s = bSrcData[b];
                    final byte[] d = bDstData[b];
                    final byte[] t = bTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int h = 0; h < dstHeight; h++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int w = 0; w < dstWidth; w++) {
                            d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFF) - tblOffset];

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
                        final byte[] d = bDstData[b];

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
                                    d[dstPixelOffset] = destinationNoDataByte;
                                } else {
                                    d[dstPixelOffset] = (byte) (data.getElem(b,
                                            (s[srcPixelOffset] & 0xFF)) & 0xFF);
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                } else {
                    for (int b = 0; b < dstNumBands; b++) {
                        byte[] s = bSrcData[b];
                        byte[] d = bDstData[b];
                        byte[] t = bTblData[b];

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
                                    d[dstPixelOffset] = destinationNoDataByte;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFF) - tblOffset];
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
                        final byte[] d = bDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, b);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataByte;
                                    } else {
                                        d[dstPixelOffset] = (byte) (data.getElem(b,
                                                (s[srcPixelOffset] & 0xFF)) & 0xFF);
                                    }
                                } else {
                                    d[dstPixelOffset] = destinationNoDataByte;
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                } else {
                    for (int b = 0; b < dstNumBands; b++) {
                        byte[] s = bSrcData[b];
                        byte[] d = bDstData[b];
                        byte[] t = bTblData[b];

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
                                    int w = roiIter.getSample(x, y, b);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataByte;
                                    } else {
                                        d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFF)
                                                - tblOffset];
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
        } else if (caseC) {
            if (caseNull) {
                for (int b = 0; b < dstNumBands; b++) {
                    final byte[] s = bSrcData[b];
                    final byte[] d = bDstData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];

                    for (int y = 0; y < dstHeight; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = 0; x < dstWidth; x++) {

                            byte value = (byte) (s[srcPixelOffset] & 0xFF);
                            if (rangeND.contains(value)) {
                                d[dstPixelOffset] = destinationNoDataByte;
                            } else {
                                d[dstPixelOffset] = (byte) (data.getElem(b,
                                        (s[srcPixelOffset] & 0xFF)) & 0xFF);
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    byte[] s = bSrcData[b];
                    byte[] d = bDstData[b];
                    byte[] t = bTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = 0; y < dstHeight; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = 0; x < dstWidth; x++) {
                            byte value = (byte) (s[srcPixelOffset] & 0xFF);
                            if (rangeND.contains(value)) {
                                d[dstPixelOffset] = destinationNoDataByte;
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
                        final byte[] s = bSrcData[b];
                        final byte[] d = bDstData[b];

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
                                    d[dstPixelOffset] = destinationNoDataByte;
                                } else {
                                    byte value = (byte) (s[srcPixelOffset] & 0xFF);
                                    if (rangeND.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataByte;
                                    } else {
                                        d[dstPixelOffset] = (byte) (data.getElem(b,
                                                (s[srcPixelOffset] & 0xFF)) & 0xFF);
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
                        byte[] d = bDstData[b];
                        byte[] t = bTblData[b];

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
                                    d[dstPixelOffset] = destinationNoDataByte;
                                } else {
                                    byte value = (byte) (s[srcPixelOffset] & 0xFF);
                                    if (rangeND.contains(value)) {
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
                }
            } else {
                if (caseNull) {
                    for (int b = 0; b < dstNumBands; b++) {
                        final byte[] s = bSrcData[b];
                        final byte[] d = bDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, b);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataByte;
                                    } else {
                                        byte value = (byte) (s[srcPixelOffset] & 0xFF);
                                        if (rangeND.contains(value)) {
                                            d[dstPixelOffset] = destinationNoDataByte;
                                        } else {
                                            d[dstPixelOffset] = (byte) (data.getElem(b,
                                                    (s[srcPixelOffset] & 0xFF)) & 0xFF);
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
                } else {
                    for (int b = 0; b < dstNumBands; b++) {
                        byte[] s = bSrcData[b];
                        byte[] d = bDstData[b];
                        byte[] t = bTblData[b];

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
                                    int w = roiIter.getSample(x, y, b);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataByte;
                                    } else {
                                        byte value = (byte) (s[srcPixelOffset] & 0xFF);
                                        if (rangeND.contains(value)) {
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
    }

    // byte to ushort/short
    private void lookup(boolean isUshort, int srcLineStride, int srcPixelStride,
            int[] srcBandOffsets, byte[][] bSrcData, int dstWidth, int dstHeight, int dstNumBands,
            int dstLineStride, int dstPixelStride, int[] dstBandOffsets, short[][] sDstData,
            int[] tblOffsets, short[][] sTblData, RasterAccessor roi, Rectangle destRect) {

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

        Range<Byte> rangeND = (Range<Byte>) noData;

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;
        final boolean caseNull = sTblData == null;

        if (caseA) {
            if (caseNull) {
                for (int b = 0; b < dstNumBands; b++) {
                    final byte[] s = bSrcData[b];
                    final short[] d = sDstData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];

                    for (int h = 0; h < dstHeight; h++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int w = 0; w < dstWidth; w++) {
                            if (isUshort) {
                                d[dstPixelOffset] = (short) (data.getElem(b,
                                        (s[srcPixelOffset] & 0xFF)) & 0xFFFF);
                            } else {
                                d[dstPixelOffset] = (short) (data.getElem(b,
                                        (s[srcPixelOffset] & 0xFF)));
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            } else {
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
                            d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFF) - tblOffset];

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
                        final short[] d = sDstData[b];

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
                                    d[dstPixelOffset] = destinationNoDataShort;
                                } else {
                                    if (isUshort) {
                                        d[dstPixelOffset] = (short) (data.getElem(b,
                                                (s[srcPixelOffset] & 0xFF)) & 0xFFFF);
                                    } else {
                                        d[dstPixelOffset] = (short) (data.getElem(b,
                                                (s[srcPixelOffset] & 0xFF)));
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

                            int posyROI = (y - dst_min_y) * roiLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                int posx = (x - dst_min_x) * srcPixelStride;

                                int windex = (posx / dstNumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataShort;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFF) - tblOffset];
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
                        final short[] d = sDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, b);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataShort;
                                    } else {
                                        if (isUshort) {
                                            d[dstPixelOffset] = (short) (data.getElem(b,
                                                    (s[srcPixelOffset] & 0xFF)) & 0xFFFF);
                                        } else {
                                            d[dstPixelOffset] = (short) (data.getElem(b,
                                                    (s[srcPixelOffset] & 0xFF)));
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
                                    int w = roiIter.getSample(x, y, b);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataShort;
                                    } else {
                                        d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFF)
                                                - tblOffset];
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
        } else if (caseC) {
            if (caseNull) {
                for (int b = 0; b < dstNumBands; b++) {
                    final byte[] s = bSrcData[b];
                    final short[] d = sDstData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];

                    for (int y = 0; y < dstHeight; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = 0; x < dstWidth; x++) {

                            byte value = (byte) (s[srcPixelOffset] & 0xFF);
                            if (rangeND.contains(value)) {
                                d[dstPixelOffset] = destinationNoDataShort;
                            } else {
                                if (isUshort) {
                                    d[dstPixelOffset] = (short) (data.getElem(b,
                                            (s[srcPixelOffset] & 0xFF)) & 0xFFFF);
                                } else {
                                    d[dstPixelOffset] = (short) (data.getElem(b,
                                            (s[srcPixelOffset] & 0xFF)));
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

                    for (int y = 0; y < dstHeight; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = 0; x < dstWidth; x++) {
                            byte value = (byte) (s[srcPixelOffset] & 0xFF);
                            if (rangeND.contains(value)) {
                                d[dstPixelOffset] = destinationNoDataShort;
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
                        final byte[] s = bSrcData[b];
                        final short[] d = sDstData[b];

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
                                    d[dstPixelOffset] = destinationNoDataShort;
                                } else {
                                    byte value = (byte) (s[srcPixelOffset] & 0xFF);
                                    if (rangeND.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataShort;
                                    } else {
                                        if (isUshort) {
                                            d[dstPixelOffset] = (short) (data.getElem(b,
                                                    (s[srcPixelOffset] & 0xFF)) & 0xFFFF);
                                        } else {
                                            d[dstPixelOffset] = (short) (data.getElem(b,
                                                    (s[srcPixelOffset] & 0xFF)));
                                        }
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

                            int posyROI = (y - dst_min_y) * roiLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                int posx = (x - dst_min_x) * srcPixelStride;

                                int windex = (posx / dstNumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataShort;
                                } else {
                                    byte value = (byte) (s[srcPixelOffset] & 0xFF);
                                    if (rangeND.contains(value)) {
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
                }
            } else {
                if (caseNull) {
                    for (int b = 0; b < dstNumBands; b++) {
                        final byte[] s = bSrcData[b];
                        final short[] d = sDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, b);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataShort;
                                    } else {
                                        byte value = (byte) (s[srcPixelOffset] & 0xFF);
                                        if (rangeND.contains(value)) {
                                            d[dstPixelOffset] = destinationNoDataShort;
                                        } else {
                                            if (isUshort) {
                                                d[dstPixelOffset] = (short) (data.getElem(b,
                                                        (s[srcPixelOffset] & 0xFF)) & 0xFFFF);
                                            } else {
                                                d[dstPixelOffset] = (short) (data.getElem(b,
                                                        (s[srcPixelOffset] & 0xFF)));
                                            }
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
                                    int w = roiIter.getSample(x, y, b);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataShort;
                                    } else {
                                        byte value = (byte) (s[srcPixelOffset] & 0xFF);
                                        if (rangeND.contains(value)) {
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
    }

    // byte to int
    private void lookup(int srcLineStride, int srcPixelStride, int[] srcBandOffsets,
            byte[][] bSrcData, int dstWidth, int dstHeight, int dstNumBands, int dstLineStride,
            int dstPixelStride, int[] dstBandOffsets, int[][] iDstData, int[] tblOffsets,
            int[][] iTblData, RasterAccessor roi, Rectangle destRect) {

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

        Range<Byte> rangeND = (Range<Byte>) noData;

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
                            d[dstPixelOffset] = data.getElem(b, s[srcPixelOffset] & 0xFF);

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
                            d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFF) - tblOffset];

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
                                    d[dstPixelOffset] = data.getElem(b, s[srcPixelOffset] & 0xFF);
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
                                    d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFF) - tblOffset];
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
                                    int w = roiIter.getSample(x, y, b);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        d[dstPixelOffset] = data.getElem(b,
                                                s[srcPixelOffset] & 0xFF);
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
                                    int w = roiIter.getSample(x, y, b);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFF)
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

                            byte value = (byte) (s[srcPixelOffset] & 0xFF);
                            if (rangeND.contains(value)) {
                                d[dstPixelOffset] = destinationNoDataInt;
                            } else {
                                d[dstPixelOffset] = data.getElem(b, s[srcPixelOffset] & 0xFF);
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
                            byte value = (byte) (s[srcPixelOffset] & 0xFF);
                            if (rangeND.contains(value)) {
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
                                    byte value = (byte) (s[srcPixelOffset] & 0xFF);
                                    if (rangeND.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        d[dstPixelOffset] = data.getElem(b,
                                                s[srcPixelOffset] & 0xFF);
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
                                    byte value = (byte) (s[srcPixelOffset] & 0xFF);
                                    if (rangeND.contains(value)) {
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
                                    int w = roiIter.getSample(x, y, b);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        byte value = (byte) (s[srcPixelOffset] & 0xFF);
                                        if (rangeND.contains(value)) {
                                            d[dstPixelOffset] = destinationNoDataInt;
                                        } else {
                                            d[dstPixelOffset] = data.getElem(b,
                                                    s[srcPixelOffset] & 0xFF);
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
                                    int w = roiIter.getSample(x, y, b);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        byte value = (byte) (s[srcPixelOffset] & 0xFF);
                                        if (rangeND.contains(value)) {
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

    // byte to float
    private void lookup(int srcLineStride, int srcPixelStride, int[] srcBandOffsets,
            byte[][] bSrcData, int dstWidth, int dstHeight, int dstNumBands, int dstLineStride,
            int dstPixelStride, int[] dstBandOffsets, float[][] fDstData, int[] tblOffsets,
            float[][] fTblData, RasterAccessor roi, Rectangle destRect) {

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

        Range<Byte> rangeND = (Range<Byte>) noData;

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;
        final boolean caseNull = fTblData == null;

        if (caseA) {
            if (caseNull) {
                for (int b = 0; b < dstNumBands; b++) {
                    final byte[] s = bSrcData[b];
                    final float[] d = fDstData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];

                    for (int h = 0; h < dstHeight; h++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int w = 0; w < dstWidth; w++) {
                            d[dstPixelOffset] = data.getElemFloat(b, (s[srcPixelOffset] & 0xFF));

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            } else {
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
                            d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFF) - tblOffset];

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
                        final float[] d = fDstData[b];

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
                                    d[dstPixelOffset] = destinationNoDataFloat;
                                } else {
                                    d[dstPixelOffset] = data.getElemFloat(b,
                                            (s[srcPixelOffset] & 0xFF));
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

                            int posyROI = (y - dst_min_y) * roiLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                int posx = (x - dst_min_x) * srcPixelStride;

                                int windex = (posx / dstNumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataFloat;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFF) - tblOffset];
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
                        final float[] d = fDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, b);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataFloat;
                                    } else {
                                        d[dstPixelOffset] = data.getElemFloat(b,
                                                (s[srcPixelOffset] & 0xFF));
                                    }
                                } else {
                                    d[dstPixelOffset] = destinationNoDataFloat;
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
                                    int w = roiIter.getSample(x, y, b);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataFloat;
                                    } else {
                                        d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFF)
                                                - tblOffset];
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
        } else if (caseC) {
            if (caseNull) {
                for (int b = 0; b < dstNumBands; b++) {
                    final byte[] s = bSrcData[b];
                    final float[] d = fDstData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];

                    for (int y = 0; y < dstHeight; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = 0; x < dstWidth; x++) {

                            byte value = (byte) (s[srcPixelOffset] & 0xFF);
                            if (rangeND.contains(value)) {
                                d[dstPixelOffset] = destinationNoDataFloat;
                            } else {
                                d[dstPixelOffset] = data
                                        .getElemFloat(b, (s[srcPixelOffset] & 0xFF));
                                ;
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

                    for (int y = 0; y < dstHeight; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = 0; x < dstWidth; x++) {
                            byte value = (byte) (s[srcPixelOffset] & 0xFF);
                            if (rangeND.contains(value)) {
                                d[dstPixelOffset] = destinationNoDataFloat;
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
                        final byte[] s = bSrcData[b];
                        final float[] d = fDstData[b];

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
                                    d[dstPixelOffset] = destinationNoDataFloat;
                                } else {
                                    byte value = (byte) (s[srcPixelOffset] & 0xFF);
                                    if (rangeND.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataFloat;
                                    } else {
                                        d[dstPixelOffset] = data.getElemFloat(b,
                                                (s[srcPixelOffset] & 0xFF));
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

                            int posyROI = (y - dst_min_y) * roiLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                int posx = (x - dst_min_x) * srcPixelStride;

                                int windex = (posx / dstNumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataFloat;
                                } else {
                                    byte value = (byte) (s[srcPixelOffset] & 0xFF);
                                    if (rangeND.contains(value)) {
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
                }
            } else {
                if (caseNull) {
                    for (int b = 0; b < dstNumBands; b++) {
                        final byte[] s = bSrcData[b];
                        final float[] d = fDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, b);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataFloat;
                                    } else {
                                        byte value = (byte) (s[srcPixelOffset] & 0xFF);
                                        if (rangeND.contains(value)) {
                                            d[dstPixelOffset] = destinationNoDataFloat;
                                        } else {
                                            d[dstPixelOffset] = data.getElemFloat(b,
                                                    (s[srcPixelOffset] & 0xFF));
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
                                    int w = roiIter.getSample(x, y, b);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataFloat;
                                    } else {
                                        byte value = (byte) (s[srcPixelOffset] & 0xFF);
                                        if (rangeND.contains(value)) {
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
    }

    // byte to double
    private void lookup(int srcLineStride, int srcPixelStride, int[] srcBandOffsets,
            byte[][] bSrcData, int dstWidth, int dstHeight, int dstNumBands, int dstLineStride,
            int dstPixelStride, int[] dstBandOffsets, double[][] dDstData, int[] tblOffsets,
            double[][] dTblData, RasterAccessor roi, Rectangle destRect) {

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

        Range<Byte> rangeND = (Range<Byte>) noData;

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;
        final boolean caseNull = dTblData == null;

        if (caseA) {
            if (caseNull) {
                for (int b = 0; b < dstNumBands; b++) {
                    final byte[] s = bSrcData[b];
                    final double[] d = dDstData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];

                    for (int h = 0; h < dstHeight; h++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int w = 0; w < dstWidth; w++) {
                            d[dstPixelOffset] = data.getElemDouble(b, (s[srcPixelOffset] & 0xFF));

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            } else {
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
                            d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFF) - tblOffset];

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
                        final double[] d = dDstData[b];

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
                                    d[dstPixelOffset] = destinationNoDataDouble;
                                } else {
                                    d[dstPixelOffset] = data.getElemDouble(b,
                                            (s[srcPixelOffset] & 0xFF));
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

                            int posyROI = (y - dst_min_y) * roiLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                int posx = (x - dst_min_x) * srcPixelStride;

                                int windex = (posx / dstNumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataDouble;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFF) - tblOffset];
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
                        final double[] d = dDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, b);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataDouble;
                                    } else {
                                        d[dstPixelOffset] = data.getElemDouble(b,
                                                (s[srcPixelOffset] & 0xFF));
                                    }
                                } else {
                                    d[dstPixelOffset] = destinationNoDataDouble;
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
                                    int w = roiIter.getSample(x, y, b);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataDouble;
                                    } else {
                                        d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFF)
                                                - tblOffset];
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
        } else if (caseC) {
            if (caseNull) {
                for (int b = 0; b < dstNumBands; b++) {
                    final byte[] s = bSrcData[b];
                    final double[] d = dDstData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];

                    for (int y = 0; y < dstHeight; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = 0; x < dstWidth; x++) {

                            byte value = (byte) (s[srcPixelOffset] & 0xFF);
                            if (rangeND.contains(value)) {
                                d[dstPixelOffset] = destinationNoDataDouble;
                            } else {
                                d[dstPixelOffset] = data.getElemDouble(b,
                                        (s[srcPixelOffset] & 0xFF));
                                ;
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

                    for (int y = 0; y < dstHeight; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = 0; x < dstWidth; x++) {
                            byte value = (byte) (s[srcPixelOffset] & 0xFF);
                            if (rangeND.contains(value)) {
                                d[dstPixelOffset] = destinationNoDataDouble;
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
                        final byte[] s = bSrcData[b];
                        final double[] d = dDstData[b];

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
                                    d[dstPixelOffset] = destinationNoDataDouble;
                                } else {
                                    byte value = (byte) (s[srcPixelOffset] & 0xFF);
                                    if (rangeND.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataDouble;
                                    } else {
                                        d[dstPixelOffset] = data.getElemDouble(b,
                                                (s[srcPixelOffset] & 0xFF));
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

                            int posyROI = (y - dst_min_y) * roiLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                int posx = (x - dst_min_x) * srcPixelStride;

                                int windex = (posx / dstNumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataDouble;
                                } else {
                                    byte value = (byte) (s[srcPixelOffset] & 0xFF);
                                    if (rangeND.contains(value)) {
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
                }
            } else {
                if (caseNull) {
                    for (int b = 0; b < dstNumBands; b++) {
                        final byte[] s = bSrcData[b];
                        final double[] d = dDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, b);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataDouble;
                                    } else {
                                        byte value = (byte) (s[srcPixelOffset] & 0xFF);
                                        if (rangeND.contains(value)) {
                                            d[dstPixelOffset] = destinationNoDataDouble;
                                        } else {
                                            d[dstPixelOffset] = data.getElemDouble(b,
                                                    (s[srcPixelOffset] & 0xFF));
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
                                    int w = roiIter.getSample(x, y, b);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataDouble;
                                    } else {
                                        byte value = (byte) (s[srcPixelOffset] & 0xFF);
                                        if (rangeND.contains(value)) {
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
}
