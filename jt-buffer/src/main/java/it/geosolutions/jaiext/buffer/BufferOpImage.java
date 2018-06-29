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
package it.geosolutions.jaiext.buffer;

import it.geosolutions.jaiext.iterators.RandomIterFactory;
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
import javax.media.jai.iterator.RandomIter;

import com.sun.media.jai.util.ImageUtil;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.strtree.STRtree;

public class BufferOpImage extends AreaOpImage {

    public static final int TILE_EXTENDER = 1;

    public static final int USHORT_MAX_VALUE = Short.MAX_VALUE - Short.MIN_VALUE;

    public static final boolean TILE_CACHED = true;

    public static final boolean ARRAY_CALC = true;

    /**
     * Spatial index for fast accessing the geometries that contain the selected pixel
     */
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

    private final double pixelArea;

    public BufferOpImage(RenderedImage source, ImageLayout layout, Map configuration,
            BorderExtender extender, int leftPadding, int rightPadding, int topPadding,
            int bottomPadding, List<ROI> rois, Range noData, double destinationNoDataDouble,
            Double valueToCount, double pixelArea) {
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
        // Get PixelArea
        this.pixelArea = pixelArea;

        counter = valueToCount != null;
        this.valueToCountD = counter ? valueToCount : 0;

        SampleModel sm = getSampleModel();
        // Source image data Type
        int dstDataType = sm.getDataType();

        // Check if the input NoData Range is equal to that of the final image data type except for Double and Float Images
        if (hasNoData) {
            int rangeType = noData.getDataType().getDataType();
            if (dstDataType != rangeType
                    && !(rangeType == DataBuffer.TYPE_FLOAT || rangeType == DataBuffer.TYPE_DOUBLE)) {
                throw new IllegalArgumentException(
                        "Input Range must have the same data type of the final image");
            }
        }

        // Boolean indicating that no calcolations must be done if the value to
        // count is equal to nodata
        skipCalculations = false;

        switch (dstDataType) {
        case DataBuffer.TYPE_BYTE:
            destinationNoDataByte = (byte) (((byte) destinationNoDataDouble) & 0xff);
            // Creation of a lookuptable containing the values to use for no
            // data
            if (hasNoData) {
                booleanLookupTable = new boolean[256];
                for (int i = 0; i < booleanLookupTable.length; i++) {
                    byte value = (byte) i;
                    booleanLookupTable[i] = !!noData.contains(value);
                }
            }
            if (counter) {
                valueToCountB = valueToCount.byteValue();
                if (!noData.contains(valueToCountB)) {
                    skipCalculations = true;
                }
            }
            break;
        case DataBuffer.TYPE_USHORT:
            destinationNoDataShort = (short) (((short) destinationNoDataDouble) & 0xffff);
            if (counter) {
                valueToCountS = valueToCount.shortValue();
                if (!noData.contains(valueToCountS)) {
                    skipCalculations = true;
                }
            }
            break;
        case DataBuffer.TYPE_SHORT:
            destinationNoDataShort = (short) destinationNoDataDouble;
            if (counter) {
                valueToCountS = valueToCount.shortValue();
                if (!noData.contains(valueToCountS)) {
                    skipCalculations = true;
                }
            }
            break;
        case DataBuffer.TYPE_INT:
            destinationNoDataInt = (int) destinationNoDataDouble;
            if (counter) {
                valueToCountI = valueToCount.intValue();
                if (!noData.contains(valueToCountI)) {
                    skipCalculations = true;
                }
            }
            break;
        case DataBuffer.TYPE_FLOAT:
            destinationNoDataFloat = (float) destinationNoDataDouble;
            if (counter) {
                valueToCountF = valueToCount.floatValue();
                if (!noData.contains(valueToCountF)) {
                    skipCalculations = true;
                }
            }
            break;
        case DataBuffer.TYPE_DOUBLE:
            this.destinationNoDataDouble = destinationNoDataDouble;
            if (counter) {
                if (!noData.contains(valueToCountD)) {
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

            // Insertion of the zones to the spatial index and union of the
            // bounds for every ROI/Zone object
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
                byteLoop(source, srcRect, srcAccessor, dstAccessor);
                break;
            case DataBuffer.TYPE_INT:
                intLoop(source, srcRect, srcAccessor, dstAccessor);
                break;
            case DataBuffer.TYPE_SHORT:
                shortLoop(source, srcRect, srcAccessor, dstAccessor);
                break;
            case DataBuffer.TYPE_USHORT:
                ushortLoop(source, srcRect, srcAccessor, dstAccessor);
                break;
            case DataBuffer.TYPE_FLOAT:
                floatLoop(source, srcRect, srcAccessor, dstAccessor);
                break;
            case DataBuffer.TYPE_DOUBLE:
                doubleLoop(source, srcRect, srcAccessor, dstAccessor);
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
            // If the tile is outside the ROI, then the destination Raster is
            // set to backgroundValues
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

    private void byteLoop(Raster ras, Rectangle srcRect, RasterAccessor src, RasterAccessor dst) {

        RandomIter iter = RandomIterFactory.create(ras, srcRect, TILE_CACHED, ARRAY_CALC);

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int dstX = dst.getX();
        int dstY = dst.getY();

        byte dstDataArrays[][] = dst.getByteDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        int dstScanlineOffset = 0;

        // Both ROI and NoData
        if (hasNoData) {

            for (int j = 0; j < dheight; j++) {
                int dstPixelOffset = dstScanlineOffset;

                int y0 = dstY + j;

                for (int i = 0; i < dwidth; i++) {

                    int x0 = dstX + i;

                    // check on containment
                    if (hasROI && !union.contains(x0, y0)) {
                        for (int k = 0; k < dnumBands; k++) {
                            byte dstData[] = dstDataArrays[k];
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destinationNoDataByte;                            
                        }
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }

                    // if every geometry really contains the selected point
                    boolean contains = true;

                    if (hasROI) {
                        contains = checkInROI(y0, x0);
                    }

                    if (!contains) {
                        for (int k = 0; k < dnumBands; k++) {
                            byte dstData[] = dstDataArrays[k];
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destinationNoDataByte;                            
                        }
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }
                    for (int k = 0; k < dnumBands; k++) {
                        byte dstData[] = dstDataArrays[k];
                        int value = 0;
                        boolean isValidData = false;

                        int xStart = x0 - leftPadding;
                        int yStart = y0 - topPadding;

                        if (counter) {
                            for (int u = 0; u < kHeight; u++) {
                                for (int v = 0; v < kWidth; v++) {
                                    int data = iter.getSample(xStart + v, yStart + u, k) & 0xFF;
                                    byte dataB = (byte) data;
                                    if (booleanLookupTable[data]) {
                                        if (dataB == valueToCountB) {
                                            value++;
                                            isValidData = booleanLookupTable[data];
                                        }
                                    }
                                }
                            }
                        } else {
                            for (int u = 0; u < kHeight; u++) {
                                for (int v = 0; v < kWidth; v++) {
                                    int data = iter.getSample(xStart + v, yStart + u, k) & 0xFF;

                                    if (booleanLookupTable[data]) {
                                        value += data;
                                        isValidData = booleanLookupTable[data];
                                    }
                                }
                            }
                        }

                        value *= pixelArea;

                        if (value < 0) {
                            value = 0;
                        } else if (value > 255) {
                            value = 255;
                        } else if (!isValidData) {
                            value = destinationNoDataByte;
                        }

                        dstData[dstPixelOffset + dstBandOffsets[k]] = (byte) value;
                    }

                    dstPixelOffset += dstPixelStride;

                }
                dstScanlineOffset += dstScanlineStride;
            }
            // No NODATA
        } else {

            for (int j = 0; j < dheight; j++) {
                int dstPixelOffset = dstScanlineOffset;

                int y0 = dstY + j;

                for (int i = 0; i < dwidth; i++) {

                    int x0 = dstX + i;

                    // check on containment
                    if (hasROI && !union.contains(x0, y0)) {
                        for (int k = 0; k < dnumBands; k++) {
                            byte dstData[] = dstDataArrays[k];
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destinationNoDataByte;                            
                        }
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }

                    // if every geometry really contains the selected point
                    boolean contains = true;

                    if (hasROI) {
                        contains = checkInROI(y0, x0);
                    }

                    if (!contains) {
                        for (int k = 0; k < dnumBands; k++) {
                            byte dstData[] = dstDataArrays[k];
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destinationNoDataByte;
                        }
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }
                    for (int k = 0; k < dnumBands; k++) {
                        byte dstData[] = dstDataArrays[k];
                        int value = 0;

                        int xStart = x0 - leftPadding;
                        int yStart = y0 - topPadding;

                        if (counter) {
                            for (int u = 0; u < kHeight; u++) {
                                for (int v = 0; v < kWidth; v++) {
                                    byte data = (byte) (iter.getSample(xStart + v, yStart + u, k) & 0xFF);
                                    if (data == valueToCountB) {
                                        value++;
                                    }
                                }
                            }
                        } else {
                            for (int u = 0; u < kHeight; u++) {
                                for (int v = 0; v < kWidth; v++) {
                                    int data = iter.getSample(xStart + v, yStart + u, k) & 0xFF;
                                    value += data;
                                }
                            }
                        }

                        value *= pixelArea;

                        if (value < 0) {
                            value = 0;
                        } else if (value > 255) {
                            value = 255;
                        }

                        dstData[dstPixelOffset + dstBandOffsets[k]] = (byte) value;
                    }

                    dstPixelOffset += dstPixelStride;

                }
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void ushortLoop(Raster ras, Rectangle srcRect, RasterAccessor src, RasterAccessor dst) {

        RandomIter iter = RandomIterFactory.create(ras, srcRect, TILE_CACHED, ARRAY_CALC);

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int dstX = dst.getX();
        int dstY = dst.getY();

        short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        int dstScanlineOffset = 0;

        // Both ROI and NoData
        if (hasNoData) {

            for (int j = 0; j < dheight; j++) {
                int dstPixelOffset = dstScanlineOffset;

                int y0 = dstY + j;

                for (int i = 0; i < dwidth; i++) {

                    int x0 = dstX + i;

                    // check on containment
                    if (hasROI && !union.contains(x0, y0)) {
                        for (int k = 0; k < dnumBands; k++) {
                            short dstData[] = dstDataArrays[k];
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destinationNoDataShort;
                        }
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }

                    // if every geometry really contains the selected point
                    boolean contains = true;

                    if (hasROI) {
                        contains = checkInROI(y0, x0);
                    }

                    if (!contains) {
                        for (int k = 0; k < dnumBands; k++) {
                            short dstData[] = dstDataArrays[k];
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destinationNoDataShort;
                        }
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }
                    for (int k = 0; k < dnumBands; k++) {
                        short dstData[] = dstDataArrays[k];
                        int value = 0;
                        boolean isValidData = false;

                        int xStart = x0 - leftPadding;
                        int yStart = y0 - topPadding;

                        if (counter) {
                            for (int u = 0; u < kHeight; u++) {
                                for (int v = 0; v < kWidth; v++) {
                                    int data = iter.getSample(xStart + v, yStart + u, k) & 0xFFFF;
                                    short dataS = (short) data;
                                    boolean valid = !noData.contains(dataS);
                                    if (valid) {
                                        if (dataS == valueToCountS) {
                                            value++;
                                            isValidData = valid;
                                        }
                                    }
                                }
                            }
                        } else {
                            for (int u = 0; u < kHeight; u++) {
                                for (int v = 0; v < kWidth; v++) {
                                    int data = iter.getSample(xStart + v, yStart + u, k) & 0xFFFF;
                                    short dataS = (short) data;
                                    boolean valid = !noData.contains(dataS);
                                    if (valid) {
                                        value += data;
                                        isValidData = valid;
                                    }
                                }
                            }
                        }

                        value *= pixelArea;

                        if (value < 0) {
                            value = 0;
                        } else if (value > USHORT_MAX_VALUE) {
                            value = USHORT_MAX_VALUE;
                        } else if (!isValidData) {
                            value = destinationNoDataShort;
                        }

                        dstData[dstPixelOffset + dstBandOffsets[k]] = (short) value;
                    }

                    dstPixelOffset += dstPixelStride;

                }
                dstScanlineOffset += dstScanlineStride;
            }
            // No NODATA
        } else {

            for (int j = 0; j < dheight; j++) {
                int dstPixelOffset = dstScanlineOffset;

                int y0 = dstY + j;

                for (int i = 0; i < dwidth; i++) {

                    int x0 = dstX + i;

                    // check on containment
                    if (hasROI && !union.contains(x0, y0)) {
                        for (int k = 0; k < dnumBands; k++) {
                            short dstData[] = dstDataArrays[k];
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destinationNoDataShort;
                        }
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }

                    // if every geometry really contains the selected point
                    boolean contains = true;

                    if (hasROI) {
                        contains = checkInROI(y0, x0);
                    }

                    if (!contains) {
                        for (int k = 0; k < dnumBands; k++) {
                            short dstData[] = dstDataArrays[k];
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destinationNoDataShort;
                        }
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }
                    for (int k = 0; k < dnumBands; k++) {
                        short dstData[] = dstDataArrays[k];
                        int value = 0;

                        int xStart = x0 - leftPadding;
                        int yStart = y0 - topPadding;

                        if (counter) {
                            for (int u = 0; u < kHeight; u++) {
                                for (int v = 0; v < kWidth; v++) {
                                    short data = (short) (iter.getSample(xStart + v, yStart + u, k) & 0xFFFF);
                                    if (data == valueToCountS) {
                                        value++;
                                    }
                                }
                            }
                        } else {
                            for (int u = 0; u < kHeight; u++) {
                                for (int v = 0; v < kWidth; v++) {
                                    int data = iter.getSample(xStart + v, yStart + u, k) & 0xFFFF;
                                    value += data;
                                }
                            }
                        }

                        value *= pixelArea;

                        if (value < 0) {
                            value = 0;
                        } else if (value > USHORT_MAX_VALUE) {
                            value = USHORT_MAX_VALUE;
                        }

                        dstData[dstPixelOffset + dstBandOffsets[k]] = (short) value;
                    }

                    dstPixelOffset += dstPixelStride;

                }
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void shortLoop(Raster ras, Rectangle srcRect, RasterAccessor src, RasterAccessor dst) {

        RandomIter iter = RandomIterFactory.create(ras, srcRect, TILE_CACHED, ARRAY_CALC);

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int dstX = dst.getX();
        int dstY = dst.getY();

        short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        int dstScanlineOffset = 0;

        // Both ROI and NoData
        if (hasNoData) {

            for (int j = 0; j < dheight; j++) {
                int dstPixelOffset = dstScanlineOffset;

                int y0 = dstY + j;

                for (int i = 0; i < dwidth; i++) {

                    int x0 = dstX + i;

                    // check on containment
                    if (hasROI && !union.contains(x0, y0)) {
                        for (int k = 0; k < dnumBands; k++) {
                            short dstData[] = dstDataArrays[k];
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destinationNoDataShort;
                        }
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }

                    // if every geometry really contains the selected point
                    boolean contains = true;

                    if (hasROI) {
                        contains = checkInROI(y0, x0);
                    }

                    if (!contains) {
                        for (int k = 0; k < dnumBands; k++) {
                            short dstData[] = dstDataArrays[k];
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destinationNoDataShort;
                        }
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }
                    for (int k = 0; k < dnumBands; k++) {
                        short dstData[] = dstDataArrays[k];
                        int value = 0;
                        boolean isValidData = false;

                        int xStart = x0 - leftPadding;
                        int yStart = y0 - topPadding;

                        if (counter) {
                            for (int u = 0; u < kHeight; u++) {
                                for (int v = 0; v < kWidth; v++) {
                                    short data = (short) iter.getSample(xStart + v, yStart + u, k);
                                    boolean valid = !noData.contains(data);
                                    if (valid) {
                                        if (data == valueToCountS) {
                                            value++;
                                            isValidData = valid;
                                        }
                                    }
                                }
                            }
                        } else {
                            for (int u = 0; u < kHeight; u++) {
                                for (int v = 0; v < kWidth; v++) {
                                    short data = (short) iter.getSample(xStart + v, yStart + u, k);
                                    boolean valid = !noData.contains(data);
                                    if (valid) {
                                        value += data;
                                        isValidData = valid;
                                    }
                                }
                            }
                        }

                        value *= pixelArea;

                        if (value < Short.MIN_VALUE) {
                            value = Short.MIN_VALUE;
                        } else if (value > Short.MAX_VALUE) {
                            value = Short.MAX_VALUE;
                        } else if (!isValidData) {
                            value = destinationNoDataShort;
                        }

                        dstData[dstPixelOffset + dstBandOffsets[k]] = (short) value;
                    }

                    dstPixelOffset += dstPixelStride;

                }
                dstScanlineOffset += dstScanlineStride;
            }
            // No NODATA
        } else {

            for (int j = 0; j < dheight; j++) {
                int dstPixelOffset = dstScanlineOffset;

                int y0 = dstY + j;

                for (int i = 0; i < dwidth; i++) {

                    int x0 = dstX + i;

                    // check on containment
                    if (hasROI && !union.contains(x0, y0)) {
                        for (int k = 0; k < dnumBands; k++) {
                            short dstData[] = dstDataArrays[k];
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destinationNoDataShort;
                        }
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }

                    // if every geometry really contains the selected point
                    boolean contains = true;

                    if (hasROI) {
                        contains = checkInROI(y0, x0);
                    }

                    if (!contains) {
                        for (int k = 0; k < dnumBands; k++) {
                            short dstData[] = dstDataArrays[k];
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destinationNoDataShort;
                        }
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }
                    for (int k = 0; k < dnumBands; k++) {
                        short dstData[] = dstDataArrays[k];
                        int value = 0;

                        int xStart = x0 - leftPadding;
                        int yStart = y0 - topPadding;

                        if (counter) {
                            for (int u = 0; u < kHeight; u++) {
                                for (int v = 0; v < kWidth; v++) {
                                    short data = (short) (iter.getSample(xStart + v, yStart + u, k));
                                    if (data == valueToCountS) {
                                        value++;
                                    }
                                }
                            }
                        } else {
                            for (int u = 0; u < kHeight; u++) {
                                for (int v = 0; v < kWidth; v++) {
                                    int data = iter.getSample(xStart + v, yStart + u, k);
                                    value += data;
                                }
                            }
                        }

                        value *= pixelArea;

                        if (value < Short.MIN_VALUE) {
                            value = Short.MIN_VALUE;
                        } else if (value > Short.MAX_VALUE) {
                            value = Short.MAX_VALUE;
                        }

                        dstData[dstPixelOffset + dstBandOffsets[k]] = (short) value;
                    }

                    dstPixelOffset += dstPixelStride;

                }
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void intLoop(Raster ras, Rectangle srcRect, RasterAccessor src, RasterAccessor dst) {

        RandomIter iter = RandomIterFactory.create(ras, srcRect, TILE_CACHED, ARRAY_CALC);

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int dstX = dst.getX();
        int dstY = dst.getY();

        int dstDataArrays[][] = dst.getIntDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        int dstScanlineOffset = 0;

        // Both ROI and NoData
        if (hasNoData) {

            for (int j = 0; j < dheight; j++) {
                int dstPixelOffset = dstScanlineOffset;

                int y0 = dstY + j;

                for (int i = 0; i < dwidth; i++) {

                    int x0 = dstX + i;

                    // check on containment
                    if (hasROI && !union.contains(x0, y0)) {
                        for (int k = 0; k < dnumBands; k++) {
                            int dstData[] = dstDataArrays[k];
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destinationNoDataInt;                            
                        }
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }

                    // if every geometry really contains the selected point
                    boolean contains = true;

                    if (hasROI) {
                        contains = checkInROI(y0, x0);
                    }

                    if (!contains) {
                        for (int k = 0; k < dnumBands; k++) {
                            int dstData[] = dstDataArrays[k];
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destinationNoDataInt;                            
                        }
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }
                    for (int k = 0; k < dnumBands; k++) {
                        int dstData[] = dstDataArrays[k];
                        long value = 0;
                        boolean isValidData = false;

                        int xStart = x0 - leftPadding;
                        int yStart = y0 - topPadding;

                        if (counter) {
                            for (int u = 0; u < kHeight; u++) {
                                for (int v = 0; v < kWidth; v++) {
                                    int data = iter.getSample(xStart + v, yStart + u, k);
                                    boolean valid = !noData.contains(data);
                                    if (valid) {
                                        if (data == valueToCountI) {
                                            value++;
                                            isValidData = valid;
                                        }
                                    }
                                }
                            }
                        } else {
                            for (int u = 0; u < kHeight; u++) {
                                for (int v = 0; v < kWidth; v++) {
                                    int data = iter.getSample(xStart + v, yStart + u, k);
                                    boolean valid = !noData.contains(data);
                                    if (valid) {
                                        value += data;
                                        isValidData = valid;
                                    }
                                }
                            }
                        }

                        value *= pixelArea;

                        if (value < Integer.MIN_VALUE) {
                            value = Integer.MIN_VALUE;
                        } else if (value > Integer.MAX_VALUE) {
                            value = Integer.MAX_VALUE;
                        } else if (!isValidData) {
                            value = destinationNoDataInt;
                        }

                        dstData[dstPixelOffset + dstBandOffsets[k]] = (int) value;
                    }

                    dstPixelOffset += dstPixelStride;

                }
                dstScanlineOffset += dstScanlineStride;
            }
            // No NODATA
        } else {

            for (int j = 0; j < dheight; j++) {
                int dstPixelOffset = dstScanlineOffset;

                int y0 = dstY + j;

                for (int i = 0; i < dwidth; i++) {

                    int x0 = dstX + i;

                    // check on containment
                    if (hasROI && !union.contains(x0, y0)) {
                        for (int k = 0; k < dnumBands; k++) {
                            int dstData[] = dstDataArrays[k];
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destinationNoDataInt;                            
                        }
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }

                    // if every geometry really contains the selected point
                    boolean contains = true;

                    if (hasROI) {
                        contains = checkInROI(y0, x0);
                    }

                    if (!contains) {
                        for (int k = 0; k < dnumBands; k++) {
                            int dstData[] = dstDataArrays[k];
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destinationNoDataInt;                            
                        }
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }
                    for (int k = 0; k < dnumBands; k++) {
                        int dstData[] = dstDataArrays[k];
                        long value = 0;

                        int xStart = x0 - leftPadding;
                        int yStart = y0 - topPadding;

                        if (counter) {
                            for (int u = 0; u < kHeight; u++) {
                                for (int v = 0; v < kWidth; v++) {
                                    int data = iter.getSample(xStart + v, yStart + u, k);
                                    if (data == valueToCountI) {
                                        value++;
                                    }
                                }
                            }
                        } else {
                            for (int u = 0; u < kHeight; u++) {
                                for (int v = 0; v < kWidth; v++) {
                                    int data = iter.getSample(xStart + v, yStart + u, k);
                                    value += data;
                                }
                            }
                        }

                        value *= pixelArea;

                        if (value < Integer.MIN_VALUE) {
                            value = Integer.MIN_VALUE;
                        } else if (value > Integer.MAX_VALUE) {
                            value = Integer.MAX_VALUE;
                        }

                        dstData[dstPixelOffset + dstBandOffsets[k]] = (int) value;
                    }

                    dstPixelOffset += dstPixelStride;

                }
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void floatLoop(Raster ras, Rectangle srcRect, RasterAccessor src, RasterAccessor dst) {

        RandomIter iter = RandomIterFactory.create(ras, srcRect, TILE_CACHED, ARRAY_CALC);

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int dstX = dst.getX();
        int dstY = dst.getY();

        float dstDataArrays[][] = dst.getFloatDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        int dstScanlineOffset = 0;

        // Both ROI and NoData
        if (hasNoData) {

            for (int j = 0; j < dheight; j++) {
                int dstPixelOffset = dstScanlineOffset;

                int y0 = dstY + j;

                for (int i = 0; i < dwidth; i++) {

                    int x0 = dstX + i;

                    // check on containment
                    if (hasROI && !union.contains(x0, y0)) {
                        for (int k = 0; k < dnumBands; k++) {
                            float dstData[] = dstDataArrays[k];
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destinationNoDataFloat;                            
                        }
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }

                    // if every geometry really contains the selected point
                    boolean contains = true;

                    if (hasROI) {
                        contains = checkInROI(y0, x0);
                    }

                    if (!contains) {
                        for (int k = 0; k < dnumBands; k++) {
                            float dstData[] = dstDataArrays[k];
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destinationNoDataFloat;
                        }
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }
                    for (int k = 0; k < dnumBands; k++) {
                        float dstData[] = dstDataArrays[k];
                        double value = 0;
                        boolean isValidData = false;

                        int xStart = x0 - leftPadding;
                        int yStart = y0 - topPadding;

                        if (counter) {
                            for (int u = 0; u < kHeight; u++) {
                                for (int v = 0; v < kWidth; v++) {
                                    float data = iter.getSampleFloat(xStart + v, yStart + u, k);
                                    boolean valid = !noData.contains(data);
                                    if (valid) {
                                        if (data == valueToCountF) {
                                            value++;
                                            isValidData = valid;
                                        }
                                    }
                                }
                            }
                        } else {
                            for (int u = 0; u < kHeight; u++) {
                                for (int v = 0; v < kWidth; v++) {
                                    float data = iter.getSampleFloat(xStart + v, yStart + u, k);
                                    boolean valid = !noData.contains(data);
                                    if (valid) {
                                        value += data;
                                        isValidData = valid;
                                    }
                                }
                            }
                        }

                        value *= pixelArea;

                        if (value < -Float.MAX_VALUE) {
                            value = -Float.MAX_VALUE;
                        } else if (value > Float.MAX_VALUE) {
                            value = Float.MAX_VALUE;
                        } else if (!isValidData) {
                            value = destinationNoDataFloat;
                        }

                        dstData[dstPixelOffset + dstBandOffsets[k]] = (float) value;
                    }

                    dstPixelOffset += dstPixelStride;

                }
                dstScanlineOffset += dstScanlineStride;
            }
            // No NODATA
        } else {

            for (int j = 0; j < dheight; j++) {
                int dstPixelOffset = dstScanlineOffset;

                int y0 = dstY + j;

                for (int i = 0; i < dwidth; i++) {

                    int x0 = dstX + i;

                    // check on containment
                    if (hasROI && !union.contains(x0, y0)) {
                        for (int k = 0; k < dnumBands; k++) {
                            float dstData[] = dstDataArrays[k];
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destinationNoDataFloat;                            
                        }
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }

                    // if every geometry really contains the selected point
                    boolean contains = true;

                    if (hasROI) {
                        contains = checkInROI(y0, x0);
                    }

                    if (!contains) {
                        for (int k = 0; k < dnumBands; k++) {
                            float dstData[] = dstDataArrays[k];
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destinationNoDataFloat;                            
                        }
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }
                    for (int k = 0; k < dnumBands; k++) {
                        float dstData[] = dstDataArrays[k];
                        double value = 0;

                        int xStart = x0 - leftPadding;
                        int yStart = y0 - topPadding;

                        if (counter) {
                            for (int u = 0; u < kHeight; u++) {
                                for (int v = 0; v < kWidth; v++) {
                                    float data = iter.getSampleFloat(xStart + v, yStart + u, k);
                                    if (data == valueToCountF) {
                                        value++;
                                    }
                                }
                            }
                        } else {
                            for (int u = 0; u < kHeight; u++) {
                                for (int v = 0; v < kWidth; v++) {
                                    float data = iter.getSampleFloat(xStart + v, yStart + u, k);
                                    value += data;
                                }
                            }
                        }

                        value *= pixelArea;

                        if (value < -Float.MAX_VALUE) {
                            value = -Float.MAX_VALUE;
                        } else if (value > Float.MAX_VALUE) {
                            value = Float.MAX_VALUE;
                        }

                        dstData[dstPixelOffset + dstBandOffsets[k]] = (float) value;
                    }

                    dstPixelOffset += dstPixelStride;

                }
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void doubleLoop(Raster ras, Rectangle srcRect, RasterAccessor src, RasterAccessor dst) {

        RandomIter iter = RandomIterFactory.create(ras, srcRect, TILE_CACHED, ARRAY_CALC);

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int dstX = dst.getX();
        int dstY = dst.getY();

        double dstDataArrays[][] = dst.getDoubleDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        int dstScanlineOffset = 0;

        // Both ROI and NoData
        if (hasNoData) {

            for (int j = 0; j < dheight; j++) {
                int dstPixelOffset = dstScanlineOffset;

                int y0 = dstY + j;

                for (int i = 0; i < dwidth; i++) {

                    int x0 = dstX + i;

                    // check on containment
                    if (hasROI && !union.contains(x0, y0)) {
                        for (int k = 0; k < dnumBands; k++) {
                            double dstData[] = dstDataArrays[k];
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destinationNoDataDouble;                            
                        }
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }

                    // if every geometry really contains the selected point
                    boolean contains = true;

                    if (hasROI) {
                        contains = checkInROI(y0, x0);
                    }

                    if (!contains) {
                        for (int k = 0; k < dnumBands; k++) {
                            double dstData[] = dstDataArrays[k];
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destinationNoDataDouble;
                        }
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }
                    for (int k = 0; k < dnumBands; k++) {
                        double dstData[] = dstDataArrays[k];
                        double value = 0;
                        boolean isValidData = false;

                        int xStart = x0 - leftPadding;
                        int yStart = y0 - topPadding;

                        if (counter) {
                            for (int u = 0; u < kHeight; u++) {
                                for (int v = 0; v < kWidth; v++) {
                                    double data = iter.getSampleDouble(xStart + v, yStart + u, k);
                                    boolean valid = !noData.contains(data);
                                    if (valid) {
                                        if (data == valueToCountD) {
                                            value++;
                                            isValidData = valid;
                                        }
                                    }
                                }
                            }
                        } else {
                            for (int u = 0; u < kHeight; u++) {
                                for (int v = 0; v < kWidth; v++) {
                                    double data = iter.getSampleDouble(xStart + v, yStart + u, k);
                                    boolean valid = !noData.contains(data);
                                    if (valid) {
                                        value += data;
                                        isValidData = valid;
                                    }
                                }
                            }
                        }

                        value *= pixelArea;

                        if (!isValidData) {
                            value = destinationNoDataDouble;
                        }

                        dstData[dstPixelOffset + dstBandOffsets[k]] = value;
                    }

                    dstPixelOffset += dstPixelStride;

                }
                dstScanlineOffset += dstScanlineStride;
            }
            // No NODATA
        } else {

            for (int j = 0; j < dheight; j++) {
                int dstPixelOffset = dstScanlineOffset;

                int y0 = dstY + j;

                for (int i = 0; i < dwidth; i++) {

                    int x0 = dstX + i;

                    // check on containment
                    if (hasROI && !union.contains(x0, y0)) {
                        for (int k = 0; k < dnumBands; k++) {
                            double dstData[] = dstDataArrays[k];
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destinationNoDataDouble;                            
                        }
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }

                    // if every geometry really contains the selected point
                    boolean contains = true;

                    if (hasROI) {
                        contains = checkInROI(y0, x0);
                    }

                    if (!contains) {
                        for (int k = 0; k < dnumBands; k++) {
                            double dstData[] = dstDataArrays[k];
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destinationNoDataDouble;
                        }
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }
                    for (int k = 0; k < dnumBands; k++) {
                        double dstData[] = dstDataArrays[k];
                        double value = 0;

                        int xStart = x0 - leftPadding;
                        int yStart = y0 - topPadding;

                        if (counter) {
                            for (int u = 0; u < kHeight; u++) {
                                for (int v = 0; v < kWidth; v++) {
                                    double data = iter.getSampleDouble(xStart + v, yStart + u, k);
                                    if (data == valueToCountD) {
                                        value++;
                                    }
                                }
                            }
                        } else {
                            for (int u = 0; u < kHeight; u++) {
                                for (int v = 0; v < kWidth; v++) {
                                    double data = iter.getSampleDouble(xStart + v, yStart + u, k);
                                    value += data;
                                }
                            }
                        }

                        value *= pixelArea;

                        dstData[dstPixelOffset + dstBandOffsets[k]] = value;
                    }

                    dstPixelOffset += dstPixelStride;

                }
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private boolean checkInROI(int y0, int x0) {
        boolean contains;
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
        return contains;
    }

}
