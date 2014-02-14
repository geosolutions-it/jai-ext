package it.geosolutions.jaiext.buffer;

import it.geosolutions.jaiext.range.Range;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.media.jai.AreaOpImage;
import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;

import com.sun.media.jai.util.ImageUtil;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.STRtree;

public class BufferOpImage extends AreaOpImage {

    public static final int TILE_EXTENDER = 1;

    public static final int USHORT_MAX_VALUE = Short.MAX_VALUE - Short.MIN_VALUE;

    /** Spatial index for fast accessing the geometries that contain the selected pixel */
    private final STRtree spatialIndex = new STRtree();

    private boolean hasROI;

    private List<ROI> rois;

    private Range noData;

    private final boolean hasNoData;

    private boolean setBackground;

    private boolean[] booleanLookupTable;

    private byte destinationNoDataByte;

    private short destinationNoDataShort;

    private int destinationNoDataInt;

    private float destinationNoDataFloat;

    private double destinationNoDataDouble;

    private byte valueToCountB;

    private short valueToCountS;

    private int valueToCountI;

    private float valueToCountF;

    private double valueToCountD;

    private boolean counter;

    private Rectangle union;

    private int kWidth;

    private int kHeight;

    private boolean skipCalculations;

    public BufferOpImage(RenderedImage source, ImageLayout layout, Map configuration,
            BorderExtender extender, int leftPadding, int rightPadding, int topPadding,
            int bottomPadding, List<ROI> rois, Range noData, double destinationNoDataDouble,
            Double valueToCount) {
        super(source, layout, configuration, true, extender, leftPadding, rightPadding, topPadding,
                bottomPadding);

        // Padding Values
        kWidth = leftPadding + rightPadding + 1;
        kHeight = topPadding + bottomPadding + 1;

        // Get the ROI
        this.rois = rois;
        // Get NoData
        this.noData = noData;
        hasROI = rois != null && !rois.isEmpty();
        hasNoData = noData != null;

        this.valueToCountD = valueToCount;
        counter = valueToCount != null;

        SampleModel sm = source.getSampleModel();
        // Source image data Type
        int srcDataType = sm.getDataType();

        // Boolean indicating that no calcolations must be done if the value to count is equal to nodata
        skipCalculations = false;

        switch (srcDataType) {
        case DataBuffer.TYPE_BYTE:
            destinationNoDataByte = (byte) (((byte) destinationNoDataDouble) & 0xff);
            // Creation of a lookuptable containing the values to use for no data
            if (hasNoData) {
                booleanLookupTable = new boolean[256];
                for (int i = 0; i < booleanLookupTable.length; i++) {
                    byte value = (byte) i;
                    booleanLookupTable[i] = !noData.contains(value);
                }
            }
            if (counter) {
                valueToCountB = valueToCount.byteValue();
                if (noData.contains(valueToCountB)) {
                    skipCalculations = true;
                }
            }
            break;
        case DataBuffer.TYPE_USHORT:
            destinationNoDataShort = (short) (((short) destinationNoDataDouble) & 0xffff);
            if (counter) {
                valueToCountS = valueToCount.shortValue();
                if (noData.contains(valueToCountS)) {
                    skipCalculations = true;
                }
            }
            break;
        case DataBuffer.TYPE_SHORT:
            destinationNoDataShort = (short) destinationNoDataDouble;
            if (counter) {
                valueToCountS = valueToCount.shortValue();
                if (noData.contains(valueToCountS)) {
                    skipCalculations = true;
                }
            }
            break;
        case DataBuffer.TYPE_INT:
            destinationNoDataInt = (int) destinationNoDataDouble;
            if (counter) {
                valueToCountI = valueToCount.intValue();
                if (noData.contains(valueToCountI)) {
                    skipCalculations = true;
                }
            }
            break;
        case DataBuffer.TYPE_FLOAT:
            destinationNoDataFloat = (float) destinationNoDataDouble;
            if (counter) {
                valueToCountF = valueToCount.floatValue();
                if (noData.contains(valueToCountF)) {
                    skipCalculations = true;
                }
            }
            break;
        case DataBuffer.TYPE_DOUBLE:
            this.destinationNoDataDouble = destinationNoDataDouble;
            if (counter) {
                if (noData.contains(valueToCountD)) {
                    skipCalculations = true;
                }
            }
            break;
        default:
            throw new IllegalArgumentException("Wrong data Type");
        }

        // Check if the rois are present. Otherwise the entire image buffer
        // is calculated
        if (rois == null || rois.isEmpty()) {

            this.rois = new ArrayList<ROI>();
            ROI roi = new ROIShape(getBounds());
            this.rois.add(roi);
            // Bounds Union
            union = new Rectangle(getBounds());

            double minX = union.getMinX();
            double maxX = union.getMaxX();
            double minY = union.getMinY();
            double maxY = union.getMaxY();
            Envelope env = new Envelope(minX, maxX, minY, maxY);
            // Addition to the geometries list
            spatialIndex.insert(env, roi);
        } else {
            // Bounds Union
            union = new Rectangle(rois.get(0).getBounds());

            // Insertion of the zones to the spatial index and union of the bounds for every ROI/Zone object
            for (ROI roi : rois) {
                // Spatial index creation
                Rectangle rect = roi.getBounds();
                double minX = rect.getMinX();
                double maxX = rect.getMaxX();
                double minY = rect.getMinY();
                double maxY = rect.getMaxY();
                Envelope env = new Envelope(minX, maxX, minY, maxY);
                // Union
                union = union.union(rect);
                // Addition to the geometries list
                spatialIndex.insert(env, roi);
            }
            // Sets of the roi list
            this.rois = rois;
        }

        // Building of the spatial index
        // Coordinate object creation for the spatial indexing
        Coordinate p1 = new Coordinate(0, 0);
        // Envelope associated to the coordinate object
        Envelope searchEnv = new Envelope(p1);
        // Query on the geometry list
        spatialIndex.query(searchEnv);
    }

    /**
     * Calculates the buffer on the defined raster
     * 
     * @param sources an array of source Rasters, guaranteed to provide all necessary source data for computing the output.
     * @param dest a WritableRaster tile containing the area to be computed.
     * @param destRect the rectangle within dest to be processed.
     */
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        Raster source = sources[0];
        Rectangle srcRect = mapDestRect(destRect, 0);

        RasterAccessor srcAccessor = new RasterAccessor(source, srcRect, formatTags[0],
                getSourceImage(0).getColorModel());
        RasterAccessor dstAccessor = new RasterAccessor(dest, destRect, formatTags[1],
                getColorModel());

        // Check if the tile is inside the geometry bound-union
        if (!hasROI || union.intersects(destRect) && !skipCalculations) {

            switch (dstAccessor.getDataType()) {
            case DataBuffer.TYPE_BYTE:
                byteLoop(srcAccessor, dstAccessor);
                break;
            case DataBuffer.TYPE_INT:
                intLoop(srcAccessor, dstAccessor);
                break;
            case DataBuffer.TYPE_SHORT:
                shortLoop(srcAccessor, dstAccessor);
                break;
            case DataBuffer.TYPE_USHORT:
                ushortLoop(srcAccessor, dstAccessor);
                break;
            case DataBuffer.TYPE_FLOAT:
                floatLoop(srcAccessor, dstAccessor);
                break;
            case DataBuffer.TYPE_DOUBLE:
                doubleLoop(srcAccessor, dstAccessor);
                break;

            default:
                throw new IllegalArgumentException("Wrong data type");
            }

            // If the RasterAccessor object set up a temporary buffer for the
            // op to write to, tell the RasterAccessor to write that data
            // to the raster no that we're done with it.
            if (dstAccessor.isDataCopy()) {
                dstAccessor.clampDataArrays();
                dstAccessor.copyDataToRaster();
            }

        } else {
            // If the tile is outside the ROI, then the destination Raster is set to backgroundValues
            if (setBackground) {
                int numBands = getNumBands();
                double[] background = new double[numBands];
                for (int i = 0; i < numBands; i++) {
                    background[i] = destinationNoDataDouble;
                }
                ImageUtil.fillBackground(dest, destRect, background);
            }
        }
    }

    private void byteLoop(RasterAccessor src, RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int dstX = dst.getX();
        int dstY = dst.getY();

        byte dstDataArrays[][] = dst.getByteDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        byte srcDataArrays[][] = src.getByteDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        int srcScanlineOffset = 0;
        int dstScanlineOffset = 0;

        // Both ROI and NoData
        if (!hasNoData) {

            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                int y0 = dstY + j;

                for (int i = 0; i < dwidth; i++) {

                    int x0 = dstX + i;

                    // check on containment
                    if (hasROI && !union.contains(x0, y0)) {
                        for (int k = 0; k < dnumBands; k++) {
                            byte dstData[] = dstDataArrays[k];
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destinationNoDataByte;
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                        continue;
                    }

                    // if every geometry really contains the selected point
                    boolean contains = true;

                    if (hasROI) {
                        // Coordinate object creation for the spatial indexing
                        Coordinate p1 = new Coordinate(x0, y0);
                        // Envelope associated to the coordinate object
                        Envelope searchEnv = new Envelope(p1);
                        // Query on the geometry list
                        List<ROI> roiList = spatialIndex.query(searchEnv);

                        contains = false;
                        // Cycle on all the geometries found
                        for (ROI roi : roiList) {

                            synchronized (this) { // HACK
                                contains = roi.contains(x0, y0);
                            }
                            if (contains) {
                                break;
                            }
                        }
                    }

                    if (!contains) {
                        for (int k = 0; k < dnumBands; k++) {
                            byte dstData[] = dstDataArrays[k];
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destinationNoDataByte;
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                            continue;
                        }


                        for (int k = 0; k < dnumBands; k++) {
                            byte srcData[] = dstDataArrays[k];
                            byte dstData[] = dstDataArrays[k];

                            int value = 0;
                            boolean isValidData = false;
                            
                            if (counter) {
                                int imageVerticalOffset = srcPixelOffset;
                                for (int u = 0; u < kHeight; u++) {
                                    int imageOffset = imageVerticalOffset;
                                    for (int v = 0; v < kWidth; v++) {
                                        byte data = srcData[imageOffset + srcBandOffsets[k]];

                                        if (booleanLookupTable[data & 0xFF]) {
                                            if (data == valueToCountB) {
                                                value++;
                                                isValidData |= booleanLookupTable[data & 0xFF];
                                            }
                                        }

                                        imageOffset += srcPixelStride;
                                    }
                                    imageVerticalOffset += srcScanlineStride;
                                }
                            } else {
                                int imageVerticalOffset = srcPixelOffset;
                                for (int u = 0; u < kHeight; u++) {
                                    int imageOffset = imageVerticalOffset;
                                    for (int v = 0; v < kWidth; v++) {
                                        byte data = srcData[imageOffset + srcBandOffsets[k]];

                                        if (booleanLookupTable[data & 0xFF]) {
                                            value += data;
                                            isValidData |= booleanLookupTable[data & 0xFF];
                                        }

                                        imageOffset += srcPixelStride;
                                    }
                                    imageVerticalOffset += srcScanlineStride;
                                }
                            }

                            if (value < 0) {
                                value = 0;
                            } else if (value > 255) {
                                value = 255;
                            } else if (!isValidData) {
                                value = destinationNoDataByte;
                            }

                            dstData[dstPixelOffset + dstBandOffsets[k]] = (byte) value;
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }

                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
            // No NODATA
        } else {


                for (int j = 0; j < dheight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;

                    int y0 = dstY + j;

                    for (int i = 0; i < dwidth; i++) {

                        int x0 = dstX + i;

                        // check on containment
                        if (hasROI && !union.contains(x0, y0)) {
                            dstData[dstPixelOffset] = destinationNoDataByte;
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                            continue;
                        }

                        // if every geometry really contains the selected point
                        boolean contains = true;

                        if (hasROI) {
                            // Coordinate object creation for the spatial indexing
                            Coordinate p1 = new Coordinate(x0, y0);
                            // Envelope associated to the coordinate object
                            Envelope searchEnv = new Envelope(p1);
                            // Query on the geometry list
                            List<ROI> roiList = spatialIndex.query(searchEnv);

                            contains = false;
                            // Cycle on all the geometries found
                            for (ROI roi : roiList) {

                                synchronized (this) { // HACK
                                    contains = roi.contains(x0, y0);
                                }
                                if (contains) {
                                    break;
                                }
                            }
                        }

                        if (!contains) {
                            dstData[dstPixelOffset] = destinationNoDataByte;
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                            continue;
                        }
                        for (int k = 0; k < dnumBands; k++) {
                            byte dstData[] = dstDataArrays[k];
                            byte srcData[] = srcDataArrays[k];
                        }
                        
                        int value = 0;

                        if (counter) {
                            int imageVerticalOffset = srcPixelOffset;
                            for (int u = 0; u < kHeight; u++) {
                                int imageOffset = imageVerticalOffset;
                                for (int v = 0; v < kWidth; v++) {
                                    byte data = srcData[imageOffset];

                                    if (data == valueToCountB) {
                                        value++;
                                    }

                                    imageOffset += srcPixelStride;
                                }
                                imageVerticalOffset += srcScanlineStride;
                            }
                        } else {
                            int imageVerticalOffset = srcPixelOffset;
                            for (int u = 0; u < kHeight; u++) {
                                int imageOffset = imageVerticalOffset;
                                for (int v = 0; v < kWidth; v++) {
                                    value += (srcData[imageOffset]);
                                    imageOffset += srcPixelStride;
                                }
                                imageVerticalOffset += srcScanlineStride;
                            }

                            if (value < 0) {
                                value = 0;
                            } else if (value > 255) {
                                value = 255;
                            }
                        }

                        dstData[dstPixelOffset] = (byte) value;
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
        }

    }

    private void ushortLoop(RasterAccessor src, RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int dstX = dst.getX();
        int dstY = dst.getY();

        short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        short srcDataArrays[][] = src.getShortDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // Both ROI and NoData
        if (!hasNoData) {

            for (int k = 0; k < dnumBands; k++) {
                short dstData[] = dstDataArrays[k];
                short srcData[] = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < dheight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;

                    int y0 = dstY + j;

                    for (int i = 0; i < dwidth; i++) {

                        int x0 = dstX + i;

                        // check on containment
                        if (hasROI && !union.contains(x0, y0)) {
                            dstData[dstPixelOffset] = destinationNoDataShort;
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                            continue;
                        }

                        // if every geometry really contains the selected point
                        boolean contains = true;

                        if (hasROI) {
                            // Coordinate object creation for the spatial indexing
                            Coordinate p1 = new Coordinate(x0, y0);
                            // Envelope associated to the coordinate object
                            Envelope searchEnv = new Envelope(p1);
                            // Query on the geometry list
                            List<ROI> roiList = spatialIndex.query(searchEnv);

                            contains = false;
                            // Cycle on all the geometries found
                            for (ROI roi : roiList) {

                                synchronized (this) { // HACK
                                    contains = roi.contains(x0, y0);
                                }
                                if (contains) {
                                    break;
                                }
                            }
                        }

                        if (!contains) {
                            dstData[dstPixelOffset] = destinationNoDataShort;
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                            continue;
                        }

                        int value = 0;
                        boolean isValidData = false;

                        if (counter) {
                            int imageVerticalOffset = srcPixelOffset;
                            for (int u = 0; u < kHeight; u++) {
                                int imageOffset = imageVerticalOffset;
                                for (int v = 0; v < kWidth; v++) {
                                    short data = (short) (srcData[imageOffset] & 0xFFFF);
                                    boolean valid = noData.contains(data);
                                    if (valid) {
                                        if (data == valueToCountS) {
                                            value++;
                                            isValidData |= valid;
                                        }
                                    }

                                    imageOffset += srcPixelStride;
                                }
                                imageVerticalOffset += srcScanlineStride;
                            }
                        } else {
                            int imageVerticalOffset = srcPixelOffset;
                            for (int u = 0; u < kHeight; u++) {
                                int imageOffset = imageVerticalOffset;
                                for (int v = 0; v < kWidth; v++) {
                                    short data = srcData[imageOffset];
                                    boolean valid = noData.contains(data);

                                    if (valid) {
                                        value += data & 0xFFFF;
                                        isValidData |= valid;
                                    }

                                    imageOffset += srcPixelStride;
                                }
                                imageVerticalOffset += srcScanlineStride;
                            }

                            if (value < 0) {
                                value = 0;
                            } else if (value > USHORT_MAX_VALUE) {
                                value = USHORT_MAX_VALUE;
                            } else if (!isValidData) {
                                value = destinationNoDataShort;
                            }
                        }

                        dstData[dstPixelOffset] = (short) value;
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
            // No NODATA
        } else {

            for (int k = 0; k < dnumBands; k++) {
                short dstData[] = dstDataArrays[k];
                short srcData[] = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < dheight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;

                    int y0 = dstY + j;

                    for (int i = 0; i < dwidth; i++) {

                        int x0 = dstX + i;

                        // check on containment
                        if (hasROI && !union.contains(x0, y0)) {
                            dstData[dstPixelOffset] = destinationNoDataShort;
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                            continue;
                        }

                        // if every geometry really contains the selected point
                        boolean contains = true;

                        if (hasROI) {
                            // Coordinate object creation for the spatial indexing
                            Coordinate p1 = new Coordinate(x0, y0);
                            // Envelope associated to the coordinate object
                            Envelope searchEnv = new Envelope(p1);
                            // Query on the geometry list
                            List<ROI> roiList = spatialIndex.query(searchEnv);

                            contains = false;
                            // Cycle on all the geometries found
                            for (ROI roi : roiList) {

                                synchronized (this) { // HACK
                                    contains = roi.contains(x0, y0);
                                }
                                if (contains) {
                                    break;
                                }
                            }
                        }

                        if (!contains) {
                            dstData[dstPixelOffset] = destinationNoDataShort;
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                            continue;
                        }

                        int value = 0;

                        if (counter) {
                            int imageVerticalOffset = srcPixelOffset;
                            for (int u = 0; u < kHeight; u++) {
                                int imageOffset = imageVerticalOffset;
                                for (int v = 0; v < kWidth; v++) {
                                    short data = (short) (srcData[imageOffset] & 0xFFFF);

                                    if (data == valueToCountS) {
                                        value++;
                                    }

                                    imageOffset += srcPixelStride;
                                }
                                imageVerticalOffset += srcScanlineStride;
                            }
                        } else {
                            int imageVerticalOffset = srcPixelOffset;
                            for (int u = 0; u < kHeight; u++) {
                                int imageOffset = imageVerticalOffset;
                                for (int v = 0; v < kWidth; v++) {
                                    value += (srcData[imageOffset] & 0xFFFF);
                                    imageOffset += srcPixelStride;
                                }
                                imageVerticalOffset += srcScanlineStride;
                            }

                            if (value < 0) {
                                value = 0;
                            } else if (value > USHORT_MAX_VALUE) {
                                value = USHORT_MAX_VALUE;
                            }
                        }

                        dstData[dstPixelOffset] = (short) value;
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
        }
    }

    private void shortLoop(RasterAccessor src, RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int dstX = dst.getX();
        int dstY = dst.getY();

        short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        short srcDataArrays[][] = src.getShortDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // Both ROI and NoData
        if (!hasNoData) {

            for (int k = 0; k < dnumBands; k++) {
                short dstData[] = dstDataArrays[k];
                short srcData[] = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < dheight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;

                    int y0 = dstY + j;

                    for (int i = 0; i < dwidth; i++) {

                        int x0 = dstX + i;

                        // check on containment
                        if (hasROI && !union.contains(x0, y0)) {
                            dstData[dstPixelOffset] = destinationNoDataShort;
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                            continue;
                        }

                        // if every geometry really contains the selected point
                        boolean contains = true;

                        if (hasROI) {
                            // Coordinate object creation for the spatial indexing
                            Coordinate p1 = new Coordinate(x0, y0);
                            // Envelope associated to the coordinate object
                            Envelope searchEnv = new Envelope(p1);
                            // Query on the geometry list
                            List<ROI> roiList = spatialIndex.query(searchEnv);

                            contains = false;
                            // Cycle on all the geometries found
                            for (ROI roi : roiList) {

                                synchronized (this) { // HACK
                                    contains = roi.contains(x0, y0);
                                }
                                if (contains) {
                                    break;
                                }
                            }
                        }

                        if (!contains) {
                            dstData[dstPixelOffset] = destinationNoDataShort;
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                            continue;
                        }

                        int value = 0;
                        boolean isValidData = false;

                        if (counter) {
                            int imageVerticalOffset = srcPixelOffset;
                            for (int u = 0; u < kHeight; u++) {
                                int imageOffset = imageVerticalOffset;
                                for (int v = 0; v < kWidth; v++) {
                                    short data = srcData[imageOffset];
                                    boolean valid = noData.contains(data);
                                    if (valid) {
                                        if (data == valueToCountS) {
                                            value++;
                                            isValidData |= valid;
                                        }
                                    }

                                    imageOffset += srcPixelStride;
                                }
                                imageVerticalOffset += srcScanlineStride;
                            }
                        } else {
                            int imageVerticalOffset = srcPixelOffset;
                            for (int u = 0; u < kHeight; u++) {
                                int imageOffset = imageVerticalOffset;
                                for (int v = 0; v < kWidth; v++) {
                                    short data = srcData[imageOffset];
                                    boolean valid = noData.contains(data);

                                    if (valid) {
                                        value += data;
                                        isValidData |= valid;
                                    }

                                    imageOffset += srcPixelStride;
                                }
                                imageVerticalOffset += srcScanlineStride;
                            }

                            if (value < Short.MIN_VALUE) {
                                value = Short.MIN_VALUE;
                            } else if (value > Short.MAX_VALUE) {
                                value = Short.MAX_VALUE;
                            } else if (!isValidData) {
                                value = destinationNoDataShort;
                            }
                        }

                        dstData[dstPixelOffset] = (short) value;
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
            // No NODATA
        } else {

            for (int k = 0; k < dnumBands; k++) {
                short dstData[] = dstDataArrays[k];
                short srcData[] = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < dheight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;

                    int y0 = dstY + j;

                    for (int i = 0; i < dwidth; i++) {

                        int x0 = dstX + i;

                        // check on containment
                        if (hasROI && !union.contains(x0, y0)) {
                            dstData[dstPixelOffset] = destinationNoDataShort;
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                            continue;
                        }

                        // if every geometry really contains the selected point
                        boolean contains = true;

                        if (hasROI) {
                            // Coordinate object creation for the spatial indexing
                            Coordinate p1 = new Coordinate(x0, y0);
                            // Envelope associated to the coordinate object
                            Envelope searchEnv = new Envelope(p1);
                            // Query on the geometry list
                            List<ROI> roiList = spatialIndex.query(searchEnv);

                            contains = false;
                            // Cycle on all the geometries found
                            for (ROI roi : roiList) {

                                synchronized (this) { // HACK
                                    contains = roi.contains(x0, y0);
                                }
                                if (contains) {
                                    break;
                                }
                            }
                        }

                        if (!contains) {
                            dstData[dstPixelOffset] = destinationNoDataShort;
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                            continue;
                        }

                        int value = 0;

                        if (counter) {
                            int imageVerticalOffset = srcPixelOffset;
                            for (int u = 0; u < kHeight; u++) {
                                int imageOffset = imageVerticalOffset;
                                for (int v = 0; v < kWidth; v++) {
                                    short data = srcData[imageOffset];

                                    if (data == valueToCountS) {
                                        value++;
                                    }

                                    imageOffset += srcPixelStride;
                                }
                                imageVerticalOffset += srcScanlineStride;
                            }
                        } else {
                            int imageVerticalOffset = srcPixelOffset;
                            for (int u = 0; u < kHeight; u++) {
                                int imageOffset = imageVerticalOffset;
                                for (int v = 0; v < kWidth; v++) {
                                    value += srcData[imageOffset];
                                    imageOffset += srcPixelStride;
                                }
                                imageVerticalOffset += srcScanlineStride;
                            }

                            if (value < Short.MIN_VALUE) {
                                value = Short.MIN_VALUE;
                            } else if (value > Short.MAX_VALUE) {
                                value = Short.MAX_VALUE;
                            }
                        }

                        dstData[dstPixelOffset] = (short) value;
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
        }
    }

    private void intLoop(RasterAccessor src, RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int dstX = dst.getX();
        int dstY = dst.getY();

        int dstDataArrays[][] = dst.getIntDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        int srcDataArrays[][] = src.getIntDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // Both ROI and NoData
        if (!hasNoData) {

            for (int k = 0; k < dnumBands; k++) {
                int dstData[] = dstDataArrays[k];
                int srcData[] = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < dheight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;

                    int y0 = dstY + j;

                    for (int i = 0; i < dwidth; i++) {

                        int x0 = dstX + i;

                        // check on containment
                        if (hasROI && !union.contains(x0, y0)) {
                            dstData[dstPixelOffset] = destinationNoDataInt;
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                            continue;
                        }

                        // if every geometry really contains the selected point
                        boolean contains = true;

                        if (hasROI) {
                            // Coordinate object creation for the spatial indexing
                            Coordinate p1 = new Coordinate(x0, y0);
                            // Envelope associated to the coordinate object
                            Envelope searchEnv = new Envelope(p1);
                            // Query on the geometry list
                            List<ROI> roiList = spatialIndex.query(searchEnv);

                            contains = false;
                            // Cycle on all the geometries found
                            for (ROI roi : roiList) {

                                synchronized (this) { // HACK
                                    contains = roi.contains(x0, y0);
                                }
                                if (contains) {
                                    break;
                                }
                            }
                        }

                        if (!contains) {
                            dstData[dstPixelOffset] = destinationNoDataInt;
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                            continue;
                        }

                        long value = 0;
                        boolean isValidData = false;

                        if (counter) {
                            int imageVerticalOffset = srcPixelOffset;
                            for (int u = 0; u < kHeight; u++) {
                                int imageOffset = imageVerticalOffset;
                                for (int v = 0; v < kWidth; v++) {
                                    int data = srcData[imageOffset];
                                    boolean valid = noData.contains(data);
                                    if (valid) {
                                        if (data == valueToCountI) {
                                            value++;
                                            isValidData |= valid;
                                        }
                                    }

                                    imageOffset += srcPixelStride;
                                }
                                imageVerticalOffset += srcScanlineStride;
                            }
                        } else {
                            int imageVerticalOffset = srcPixelOffset;
                            for (int u = 0; u < kHeight; u++) {
                                int imageOffset = imageVerticalOffset;
                                for (int v = 0; v < kWidth; v++) {
                                    int data = srcData[imageOffset];
                                    boolean valid = noData.contains(data);

                                    if (valid) {
                                        value += data;
                                        isValidData |= valid;
                                    }

                                    imageOffset += srcPixelStride;
                                }
                                imageVerticalOffset += srcScanlineStride;
                            }

                            if (value < Integer.MIN_VALUE) {
                                value = Integer.MIN_VALUE;
                            } else if (value > Integer.MAX_VALUE) {
                                value = Integer.MAX_VALUE;
                            } else if (!isValidData) {
                                value = destinationNoDataInt;
                            }
                        }

                        dstData[dstPixelOffset] = (int) value;
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
            // No NODATA
        } else {

            for (int k = 0; k < dnumBands; k++) {
                int dstData[] = dstDataArrays[k];
                int srcData[] = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < dheight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;

                    int y0 = dstY + j;

                    for (int i = 0; i < dwidth; i++) {

                        int x0 = dstX + i;

                        // check on containment
                        if (hasROI && !union.contains(x0, y0)) {
                            dstData[dstPixelOffset] = destinationNoDataInt;
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                            continue;
                        }

                        // if every geometry really contains the selected point
                        boolean contains = true;

                        if (hasROI) {
                            // Coordinate object creation for the spatial indexing
                            Coordinate p1 = new Coordinate(x0, y0);
                            // Envelope associated to the coordinate object
                            Envelope searchEnv = new Envelope(p1);
                            // Query on the geometry list
                            List<ROI> roiList = spatialIndex.query(searchEnv);

                            contains = false;
                            // Cycle on all the geometries found
                            for (ROI roi : roiList) {

                                synchronized (this) { // HACK
                                    contains = roi.contains(x0, y0);
                                }
                                if (contains) {
                                    break;
                                }
                            }
                        }

                        if (!contains) {
                            dstData[dstPixelOffset] = destinationNoDataInt;
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                            continue;
                        }

                        long value = 0;

                        if (counter) {
                            int imageVerticalOffset = srcPixelOffset;
                            for (int u = 0; u < kHeight; u++) {
                                int imageOffset = imageVerticalOffset;
                                for (int v = 0; v < kWidth; v++) {
                                    int data = srcData[imageOffset];

                                    if (data == valueToCountI) {
                                        value++;
                                    }

                                    imageOffset += srcPixelStride;
                                }
                                imageVerticalOffset += srcScanlineStride;
                            }
                        } else {
                            int imageVerticalOffset = srcPixelOffset;
                            for (int u = 0; u < kHeight; u++) {
                                int imageOffset = imageVerticalOffset;
                                for (int v = 0; v < kWidth; v++) {
                                    value += srcData[imageOffset];
                                    imageOffset += srcPixelStride;
                                }
                                imageVerticalOffset += srcScanlineStride;
                            }

                            if (value < Integer.MIN_VALUE) {
                                value = Integer.MIN_VALUE;
                            } else if (value > Integer.MAX_VALUE) {
                                value = Integer.MAX_VALUE;
                            }
                        }

                        dstData[dstPixelOffset] = (int) value;
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
        }
    }

    private void floatLoop(RasterAccessor src, RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int dstX = dst.getX();
        int dstY = dst.getY();

        float dstDataArrays[][] = dst.getFloatDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        float srcDataArrays[][] = src.getFloatDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // Both ROI and NoData
        if (!hasNoData) {

            for (int k = 0; k < dnumBands; k++) {
                float dstData[] = dstDataArrays[k];
                float srcData[] = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < dheight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;

                    int y0 = dstY + j;

                    for (int i = 0; i < dwidth; i++) {

                        int x0 = dstX + i;

                        // check on containment
                        if (hasROI && !union.contains(x0, y0)) {
                            dstData[dstPixelOffset] = destinationNoDataFloat;
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                            continue;
                        }

                        // if every geometry really contains the selected point
                        boolean contains = true;

                        if (hasROI) {
                            // Coordinate object creation for the spatial indexing
                            Coordinate p1 = new Coordinate(x0, y0);
                            // Envelope associated to the coordinate object
                            Envelope searchEnv = new Envelope(p1);
                            // Query on the geometry list
                            List<ROI> roiList = spatialIndex.query(searchEnv);

                            contains = false;
                            // Cycle on all the geometries found
                            for (ROI roi : roiList) {

                                synchronized (this) { // HACK
                                    contains = roi.contains(x0, y0);
                                }
                                if (contains) {
                                    break;
                                }
                            }
                        }

                        if (!contains) {
                            dstData[dstPixelOffset] = destinationNoDataFloat;
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                            continue;
                        }

                        double value = 0;
                        boolean isValidData = false;

                        if (counter) {
                            int imageVerticalOffset = srcPixelOffset;
                            for (int u = 0; u < kHeight; u++) {
                                int imageOffset = imageVerticalOffset;
                                for (int v = 0; v < kWidth; v++) {
                                    float data = srcData[imageOffset];
                                    boolean valid = noData.contains(data);
                                    if (valid) {
                                        if (data == valueToCountF) {
                                            value++;
                                            isValidData |= valid;
                                        }
                                    }

                                    imageOffset += srcPixelStride;
                                }
                                imageVerticalOffset += srcScanlineStride;
                            }
                        } else {
                            int imageVerticalOffset = srcPixelOffset;
                            for (int u = 0; u < kHeight; u++) {
                                int imageOffset = imageVerticalOffset;
                                for (int v = 0; v < kWidth; v++) {
                                    float data = srcData[imageOffset];
                                    boolean valid = noData.contains(data);

                                    if (valid) {
                                        value += data;
                                        isValidData |= valid;
                                    }

                                    imageOffset += srcPixelStride;
                                }
                                imageVerticalOffset += srcScanlineStride;
                            }

                            if (value < -Float.MAX_VALUE) {
                                value = -Float.MAX_VALUE;
                            } else if (value > Float.MAX_VALUE) {
                                value = Float.MAX_VALUE;
                            } else if (!isValidData) {
                                value = destinationNoDataFloat;
                            }
                        }

                        dstData[dstPixelOffset] = (float) value;
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
            // No NODATA
        } else {

            for (int k = 0; k < dnumBands; k++) {
                float dstData[] = dstDataArrays[k];
                float srcData[] = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < dheight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;

                    int y0 = dstY + j;

                    for (int i = 0; i < dwidth; i++) {

                        int x0 = dstX + i;

                        // check on containment
                        if (hasROI && !union.contains(x0, y0)) {
                            dstData[dstPixelOffset] = destinationNoDataFloat;
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                            continue;
                        }

                        // if every geometry really contains the selected point
                        boolean contains = true;

                        if (hasROI) {
                            // Coordinate object creation for the spatial indexing
                            Coordinate p1 = new Coordinate(x0, y0);
                            // Envelope associated to the coordinate object
                            Envelope searchEnv = new Envelope(p1);
                            // Query on the geometry list
                            List<ROI> roiList = spatialIndex.query(searchEnv);

                            contains = false;
                            // Cycle on all the geometries found
                            for (ROI roi : roiList) {

                                synchronized (this) { // HACK
                                    contains = roi.contains(x0, y0);
                                }
                                if (contains) {
                                    break;
                                }
                            }
                        }

                        if (!contains) {
                            dstData[dstPixelOffset] = destinationNoDataFloat;
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                            continue;
                        }

                        double value = 0;

                        if (counter) {
                            int imageVerticalOffset = srcPixelOffset;
                            for (int u = 0; u < kHeight; u++) {
                                int imageOffset = imageVerticalOffset;
                                for (int v = 0; v < kWidth; v++) {
                                    float data = srcData[imageOffset];

                                    if (data == valueToCountF) {
                                        value++;
                                    }

                                    imageOffset += srcPixelStride;
                                }
                                imageVerticalOffset += srcScanlineStride;
                            }
                        } else {
                            int imageVerticalOffset = srcPixelOffset;
                            for (int u = 0; u < kHeight; u++) {
                                int imageOffset = imageVerticalOffset;
                                for (int v = 0; v < kWidth; v++) {
                                    value += srcData[imageOffset];
                                    imageOffset += srcPixelStride;
                                }
                                imageVerticalOffset += srcScanlineStride;
                            }

                            if (value < -Float.MAX_VALUE) {
                                value = -Float.MAX_VALUE;
                            } else if (value > Float.MAX_VALUE) {
                                value = Float.MAX_VALUE;
                            }
                        }

                        dstData[dstPixelOffset] = (float) value;
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
        }
    }

    private void doubleLoop(RasterAccessor src, RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int dstX = dst.getX();
        int dstY = dst.getY();

        double dstDataArrays[][] = dst.getDoubleDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        double srcDataArrays[][] = src.getDoubleDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // Both ROI and NoData
        if (!hasNoData) {

            for (int k = 0; k < dnumBands; k++) {
                double dstData[] = dstDataArrays[k];
                double srcData[] = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < dheight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;

                    int y0 = dstY + j;

                    for (int i = 0; i < dwidth; i++) {

                        int x0 = dstX + i;

                        // check on containment
                        if (hasROI && !union.contains(x0, y0)) {
                            dstData[dstPixelOffset] = destinationNoDataDouble;
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                            continue;
                        }

                        // if every geometry really contains the selected point
                        boolean contains = true;

                        if (hasROI) {
                            // Coordinate object creation for the spatial indexing
                            Coordinate p1 = new Coordinate(x0, y0);
                            // Envelope associated to the coordinate object
                            Envelope searchEnv = new Envelope(p1);
                            // Query on the geometry list
                            List<ROI> roiList = spatialIndex.query(searchEnv);

                            contains = false;
                            // Cycle on all the geometries found
                            for (ROI roi : roiList) {

                                synchronized (this) { // HACK
                                    contains = roi.contains(x0, y0);
                                }
                                if (contains) {
                                    break;
                                }
                            }
                        }

                        if (!contains) {
                            dstData[dstPixelOffset] = destinationNoDataDouble;
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                            continue;
                        }

                        double value = 0;
                        boolean isValidData = false;

                        if (counter) {
                            int imageVerticalOffset = srcPixelOffset;
                            for (int u = 0; u < kHeight; u++) {
                                int imageOffset = imageVerticalOffset;
                                for (int v = 0; v < kWidth; v++) {
                                    double data = srcData[imageOffset];
                                    boolean valid = noData.contains(data);
                                    if (valid) {
                                        if (data == valueToCountD) {
                                            value++;
                                            isValidData |= valid;
                                        }
                                    }

                                    imageOffset += srcPixelStride;
                                }
                                imageVerticalOffset += srcScanlineStride;
                            }
                        } else {
                            int imageVerticalOffset = srcPixelOffset;
                            for (int u = 0; u < kHeight; u++) {
                                int imageOffset = imageVerticalOffset;
                                for (int v = 0; v < kWidth; v++) {
                                    double data = srcData[imageOffset];
                                    boolean valid = noData.contains(data);

                                    if (valid) {
                                        value += data;
                                        isValidData |= valid;
                                    }

                                    imageOffset += srcPixelStride;
                                }
                                imageVerticalOffset += srcScanlineStride;
                            }

                            if (!isValidData) {
                                value = destinationNoDataDouble;
                            }
                        }

                        dstData[dstPixelOffset] = value;
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
            // No NODATA
        } else {

            for (int k = 0; k < dnumBands; k++) {
                double dstData[] = dstDataArrays[k];
                double srcData[] = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < dheight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;

                    int y0 = dstY + j;

                    for (int i = 0; i < dwidth; i++) {

                        int x0 = dstX + i;

                        // check on containment
                        if (hasROI && !union.contains(x0, y0)) {
                            dstData[dstPixelOffset] = destinationNoDataDouble;
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                            continue;
                        }

                        // if every geometry really contains the selected point
                        boolean contains = true;

                        if (hasROI) {
                            // Coordinate object creation for the spatial indexing
                            Coordinate p1 = new Coordinate(x0, y0);
                            // Envelope associated to the coordinate object
                            Envelope searchEnv = new Envelope(p1);
                            // Query on the geometry list
                            List<ROI> roiList = spatialIndex.query(searchEnv);

                            contains = false;
                            // Cycle on all the geometries found
                            for (ROI roi : roiList) {

                                synchronized (this) { // HACK
                                    contains = roi.contains(x0, y0);
                                }
                                if (contains) {
                                    break;
                                }
                            }
                        }

                        if (!contains) {
                            dstData[dstPixelOffset] = destinationNoDataDouble;
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                            continue;
                        }

                        double value = 0;

                        if (counter) {
                            int imageVerticalOffset = srcPixelOffset;
                            for (int u = 0; u < kHeight; u++) {
                                int imageOffset = imageVerticalOffset;
                                for (int v = 0; v < kWidth; v++) {
                                    double data = srcData[imageOffset];

                                    if (data == valueToCountD) {
                                        value++;
                                    }

                                    imageOffset += srcPixelStride;
                                }
                                imageVerticalOffset += srcScanlineStride;
                            }
                        } else {
                            int imageVerticalOffset = srcPixelOffset;
                            for (int u = 0; u < kHeight; u++) {
                                int imageOffset = imageVerticalOffset;
                                for (int v = 0; v < kWidth; v++) {
                                    value += srcData[imageOffset];
                                    imageOffset += srcPixelStride;
                                }
                                imageVerticalOffset += srcScanlineStride;
                            }

                        }

                        dstData[dstPixelOffset] = value;
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
        }
    }
}
