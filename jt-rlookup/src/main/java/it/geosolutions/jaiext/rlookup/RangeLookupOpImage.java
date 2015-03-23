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
package it.geosolutions.jaiext.rlookup;

import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Map;

import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.PointOpImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.iterator.RandomIter;

import com.sun.media.jai.util.ImageUtil;

/**
 * This is a variation on the JAI {@linkplain javax.media.jai.LookupDescriptor}. It works with a {@linkplain RangeLookupTable} object in which each
 * entry maps a source image value range to a destination image value. Optional {@link ROI}s may be used in computations.
 * 
 * @see RangeLookupDescriptor
 * 
 * @author Michael Bedward
 * @author Simone Giannecchini, GeoSolutions
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RangeLookupOpImage extends PointOpImage {

    /**
     * Constant indicating that the inner random iterators must pre-calculate an array of the image positions
     */
    public static final boolean ARRAY_CALC = true;

    /**
     * Constant indicating that the inner random iterators must cache the current tile position
     */
    public static final boolean TILE_CACHED = true;

    /** {@link RangeLookupTable} object used for the operation */
    private final RangeLookupTable table;

    /** Value to set for pixel outside {@link RangeLookupTable} range or outside ROI */
    private final Number defaultValue;

    /** Optional {@link ROI} used in computation */
    private final ROI roi;

    /** Boolean indicating if the default value is defined */
    private final boolean hasDefault;

    /** Boolean indicating if ROI is defined */
    private boolean hasROI;

    /** ROI bounds, used for a fast ROI check */
    private Rectangle roiBounds;

    /** {@link PlanarImage} which represents the binarized ROI */
    private PlanarImage roiImage;

    /**
     * Constructor
     * 
     * @param source a RenderedImage.
     * @param config configurable attributes of the image
     * 
     * @param layout an ImageLayout optionally containing the tile grid layout, SampleModel, and ColorModel, or null.
     * 
     * @param table an instance of RangeLookupTable that defines the mappings from source value ranges to destination values
     * 
     * @param defaultValue either a value to use for all unmatched source values or null to indicate that unmatched values should pass-through to the
     *        destination
     * @param roi {@link ROI} object used for masking image areas.
     * 
     * @see RangeLookupDescriptor
     */
    public RangeLookupOpImage(RenderedImage source, Map config, ImageLayout layout,
            RangeLookupTable table, Number defaultValue, ROI roi) {

        super(source, layout, config, true);

        this.table = table;
        this.defaultValue = defaultValue;
        this.hasDefault = defaultValue != null;
        this.roi = roi;
        hasROI = roi != null;
        if (hasROI) {
            roiBounds = roi.getBounds();
        }
    }

    /**
     * Do lookups for the specified destination rectangle
     * 
     * @param sources an array of source Rasters
     * @param dest a WritableRaster tile containing the area to be computed.
     * @param destRect the rectangle within dest to be processed.
     */
    @Override
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        RasterFormatTag[] formatTags = getFormatTags();

        Raster source = sources[0];
        Rectangle srcRect = mapDestRect(destRect, 0);

        ROI roiTile = null;

        RandomIter roiIter = null;

        boolean roiContainsTile = false;
        boolean roiDisjointTile = false;

        // If a ROI is present, then only the part contained inside the current
        // tile bounds is taken.
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

        if (roiDisjointTile) {
            int numBands = dest.getNumBands();
            double[] destNoData = new double[numBands];
            double nodata = hasDefault ? defaultValue.doubleValue() : 0d;
            for (int i = 0; i < numBands; i++) {
                destNoData[i] = nodata;
            }
            ImageUtil.fillBackground(dest, destRect, destNoData);
            return;
        }

        RasterAccessor srcAcc = new RasterAccessor(source, srcRect, formatTags[0],
                getSourceImage(0).getColorModel());

        RasterAccessor destAcc = new RasterAccessor(dest, destRect, formatTags[1], getColorModel());

        doLookup(srcAcc, destAcc, roiIter, roiContainsTile);
    }

    private void doLookup(RasterAccessor srcAcc, RasterAccessor destAcc, RandomIter roiIter,
            boolean roiContainsTile) {

        switch (destAcc.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            lookupAsByteData(srcAcc, destAcc, roiIter, roiContainsTile);
            break;
        case DataBuffer.TYPE_INT:
            lookupAsIntData(srcAcc, destAcc, roiIter, roiContainsTile);
            break;
        case DataBuffer.TYPE_SHORT:
            lookupAsShortData(srcAcc, destAcc, roiIter, roiContainsTile);
            break;
        case DataBuffer.TYPE_USHORT:
            lookupAsUShortData(srcAcc, destAcc, roiIter, roiContainsTile);
            break;
        case DataBuffer.TYPE_FLOAT:
            lookupAsFloatData(srcAcc, destAcc, roiIter, roiContainsTile);
            break;
        case DataBuffer.TYPE_DOUBLE:
            lookupAsDoubleData(srcAcc, destAcc, roiIter, roiContainsTile);
            break;
        }

        if (destAcc.isDataCopy()) {
            destAcc.clampDataArrays();
            destAcc.copyDataToRaster();
        }

    }

    private void lookupAsByteData(RasterAccessor srcAcc, RasterAccessor destAcc,
            RandomIter roiIter, boolean roiContainsTile) {
        byte srcData[][] = srcAcc.getByteDataArrays();
        byte destData[][] = destAcc.getByteDataArrays();

        int destWidth = destAcc.getWidth();
        int destHeight = destAcc.getHeight();
        int destBands = destAcc.getNumBands();

        int[] dstBandOffsets = destAcc.getBandOffsets();
        int dstPixelStride = destAcc.getPixelStride();
        int dstScanlineStride = destAcc.getScanlineStride();

        int[] srcBandOffsets = srcAcc.getBandOffsets();
        int srcPixelStride = srcAcc.getPixelStride();
        int srcScanlineStride = srcAcc.getScanlineStride();

        Range lastRange = null;

        byte typedDefaultValue = hasDefault ? defaultValue.byteValue() : Byte.MIN_VALUE;
        byte destinationValue = typedDefaultValue;

        if (hasROI && !roiContainsTile) {

            int srcScanlineOffset = 0;
            int dstScanlineOffset = 0;
            for (int j = 0, destY = destAcc.getY(); j < destHeight; j++, destY++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0, destX = destAcc.getX(); i < destWidth; i++, destX++) {

                    if (!(roiBounds.contains(destX, destY) && roiIter.getSample(destX, destY, 0) > 0)) {
                        for (int k = 0; k < destBands; k++) {
                            // input value
                            byte val = hasDefault ? typedDefaultValue
                                    : (byte) (srcData[k][srcPixelOffset + srcBandOffsets[k]] & 0xff);

                            destData[k][dstPixelOffset + dstBandOffsets[k]] = val;
                        }
                    } else {
                        for (int k = 0; k < destBands; k++) {
                            // input value
                            byte val = (byte) (srcData[k][srcPixelOffset + srcBandOffsets[k]] & 0xff);

                            // === destination value
                            if (lastRange == null || !lastRange.contains(val)) {
                                // nullify the current rane
                                lastRange = null;

                                // get a new one if the value falls within some
                                LookupItem item = table.getLookupItem(val);
                                if (item != null) {
                                    lastRange = item.getRange();
                                    destinationValue = item.getValue().byteValue();
                                } else {
                                    // no match: set destination to default value (if defined)
                                    // or source value
                                    destinationValue = hasDefault ? typedDefaultValue : val;
                                }
                            }
                            destData[k][dstPixelOffset + dstBandOffsets[k]] = destinationValue;
                        }
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }

        } else {
            for (int k = 0; k < destBands; k++) {
                byte destBandData[] = destData[k];
                byte srcBandData[] = srcData[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < destHeight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;

                    for (int i = 0; i < destWidth; i++) {
                        // input value
                        byte val = (byte) (srcBandData[srcPixelOffset] & 0xff);

                        // === destination value
                        if (lastRange == null || !lastRange.contains(val)) {
                            // nullify the current rane
                            lastRange = null;

                            // get a new one if the value falls within some
                            LookupItem item = table.getLookupItem(val);
                            if (item != null) {
                                lastRange = item.getRange();
                                destinationValue = item.getValue().byteValue();
                            } else {
                                // no match: set destination to default value (if defined)
                                // or source value
                                destinationValue = hasDefault ? typedDefaultValue : val;
                            }
                        }
                        destBandData[dstPixelOffset] = destinationValue;
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
        }
    }

    private void lookupAsShortData(RasterAccessor srcAcc, RasterAccessor destAcc,
            RandomIter roiIter, boolean roiContainsTile) {
        short srcData[][] = srcAcc.getShortDataArrays();
        short destData[][] = destAcc.getShortDataArrays();
        int destWidth = destAcc.getWidth();
        int destHeight = destAcc.getHeight();
        int destBands = destAcc.getNumBands();

        int[] dstBandOffsets = destAcc.getBandOffsets();
        int dstPixelStride = destAcc.getPixelStride();
        int dstScanlineStride = destAcc.getScanlineStride();

        int[] srcBandOffsets = srcAcc.getBandOffsets();
        int srcPixelStride = srcAcc.getPixelStride();
        int srcScanlineStride = srcAcc.getScanlineStride();

        Range lastRange = null;

        short typedDefaultValue = hasDefault ? defaultValue.shortValue() : Short.MIN_VALUE;
        short destinationValue = typedDefaultValue;

        if (hasROI && !roiContainsTile) {

            int srcScanlineOffset = 0;
            int dstScanlineOffset = 0;
            for (int j = 0, destY = destAcc.getY(); j < destHeight; j++, destY++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0, destX = destAcc.getX(); i < destWidth; i++, destX++) {

                    if (!(roiBounds.contains(destX, destY) && roiIter.getSample(destX, destY, 0) > 0)) {
                        for (int k = 0; k < destBands; k++) {
                            // input value
                            short val = hasDefault ? typedDefaultValue : (srcData[k][srcPixelOffset
                                    + srcBandOffsets[k]]);

                            destData[k][dstPixelOffset + dstBandOffsets[k]] = val;
                        }
                    } else {
                        for (int k = 0; k < destBands; k++) {
                            // input value
                            short val = (srcData[k][srcPixelOffset + srcBandOffsets[k]]);

                            // === destination value
                            if (lastRange == null || !lastRange.contains(val)) {
                                // nullify the current rane
                                lastRange = null;

                                // get a new one if the value falls within some
                                LookupItem item = table.getLookupItem(val);
                                if (item != null) {
                                    lastRange = item.getRange();
                                    destinationValue = item.getValue().shortValue();
                                } else {
                                    // no match: set destination to default value (if defined)
                                    // or source value
                                    destinationValue = hasDefault ? typedDefaultValue : val;
                                }
                            }
                            destData[k][dstPixelOffset + dstBandOffsets[k]] = destinationValue;
                        }
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }

        } else {
            for (int k = 0; k < destBands; k++) {
                short destBandData[] = destData[k];
                short srcBandData[] = srcData[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < destHeight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;

                    for (int i = 0; i < destWidth; i++) {
                        // input value
                        short val = (srcBandData[srcPixelOffset]);

                        // === destination value
                        if (lastRange == null || !lastRange.contains(val)) {
                            // nullify the current rane
                            lastRange = null;

                            // get a new one if the value falls within some
                            LookupItem item = table.getLookupItem(val);
                            if (item != null) {
                                lastRange = item.getRange();
                                destinationValue = item.getValue().shortValue();
                            } else {
                                // no match: set destination to default value (if defined)
                                // or source value
                                destinationValue = hasDefault ? typedDefaultValue : val;
                            }
                        }
                        destBandData[dstPixelOffset] = destinationValue;
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
        }
    }

    private void lookupAsUShortData(RasterAccessor srcAcc, RasterAccessor destAcc,
            RandomIter roiIter, boolean roiContainsTile) {
        short srcData[][] = srcAcc.getShortDataArrays();
        short destData[][] = destAcc.getShortDataArrays();
        int destWidth = destAcc.getWidth();
        int destHeight = destAcc.getHeight();
        int destBands = destAcc.getNumBands();

        int[] dstBandOffsets = destAcc.getBandOffsets();
        int dstPixelStride = destAcc.getPixelStride();
        int dstScanlineStride = destAcc.getScanlineStride();

        int[] srcBandOffsets = srcAcc.getBandOffsets();
        int srcPixelStride = srcAcc.getPixelStride();
        int srcScanlineStride = srcAcc.getScanlineStride();

        Range lastRange = null;

        short typedDefaultValue = hasDefault ? defaultValue.shortValue() : 0;
        short destinationValue = typedDefaultValue;

        if (hasROI && !roiContainsTile) {

            int srcScanlineOffset = 0;
            int dstScanlineOffset = 0;
            for (int j = 0, destY = destAcc.getY(); j < destHeight; j++, destY++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0, destX = destAcc.getX(); i < destWidth; i++, destX++) {

                    if (!(roiBounds.contains(destX, destY) && roiIter.getSample(destX, destY, 0) > 0)) {
                        for (int k = 0; k < destBands; k++) {
                            // input value
                            int val = hasDefault ? typedDefaultValue
                                    : srcData[k][srcPixelOffset] & 0xffff;

                            destData[k][dstPixelOffset + dstBandOffsets[k]] = (short) val;
                        }
                    } else {
                        for (int k = 0; k < destBands; k++) {
                            // input value
                            int val = srcData[k][srcPixelOffset] & 0xffff;

                            // === destination value
                            if (lastRange == null || !lastRange.contains(val)) {
                                // nullify the current rane
                                lastRange = null;

                                // get a new one if the value falls within some
                                LookupItem item = table.getLookupItem(val);
                                if (item != null) {
                                    lastRange = item.getRange();
                                    destinationValue = item.getValue().shortValue();
                                } else {
                                    // no match: set destination to default value (if defined)
                                    // or source value
                                    destinationValue = (hasDefault ? typedDefaultValue
                                            : (short) val);
                                }
                            }
                            destData[k][dstPixelOffset + dstBandOffsets[k]] = destinationValue;
                        }
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }

        } else {
            for (int k = 0; k < destBands; k++) {
                short destBandData[] = destData[k];
                short srcBandData[] = srcData[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < destHeight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;

                    for (int i = 0; i < destWidth; i++) {
                        // input value
                        int val = srcBandData[srcPixelOffset] & 0xffff;

                        // === destination value
                        if (lastRange == null || !lastRange.contains(val)) {
                            // nullify the current rane
                            lastRange = null;

                            // get a new one if the value falls within some
                            LookupItem item = table.getLookupItem(val);
                            if (item != null) {
                                lastRange = item.getRange();
                                destinationValue = item.getValue().shortValue();
                            } else {
                                // no match: set destination to default value (if defined)
                                // or source value
                                destinationValue = hasDefault ? typedDefaultValue : (short) val;
                            }
                        }
                        destBandData[dstPixelOffset] = destinationValue;
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
        }
    }

    private void lookupAsIntData(RasterAccessor srcAcc, RasterAccessor destAcc, RandomIter roiIter,
            boolean roiContainsTile) {
        int srcData[][] = srcAcc.getIntDataArrays();
        int destData[][] = destAcc.getIntDataArrays();
        int destWidth = destAcc.getWidth();
        int destHeight = destAcc.getHeight();
        int destBands = destAcc.getNumBands();

        int[] dstBandOffsets = destAcc.getBandOffsets();
        int dstPixelStride = destAcc.getPixelStride();
        int dstScanlineStride = destAcc.getScanlineStride();

        int[] srcBandOffsets = srcAcc.getBandOffsets();
        int srcPixelStride = srcAcc.getPixelStride();
        int srcScanlineStride = srcAcc.getScanlineStride();

        Range lastRange = null;

        int typedDefaultValue = hasDefault ? defaultValue.intValue() : Integer.MIN_VALUE;
        int destinationValue = typedDefaultValue;

        if (hasROI && !roiContainsTile) {

            int srcScanlineOffset = 0;
            int dstScanlineOffset = 0;
            for (int j = 0, destY = destAcc.getY(); j < destHeight; j++, destY++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0, destX = destAcc.getX(); i < destWidth; i++, destX++) {

                    if (!(roiBounds.contains(destX, destY) && roiIter.getSample(destX, destY, 0) > 0)) {
                        for (int k = 0; k < destBands; k++) {
                            // input value
                            int val = hasDefault ? typedDefaultValue : (srcData[k][srcPixelOffset
                                    + srcBandOffsets[k]]);

                            destData[k][dstPixelOffset + dstBandOffsets[k]] = val;
                        }
                    } else {
                        for (int k = 0; k < destBands; k++) {
                            // input value
                            int val = (srcData[k][srcPixelOffset + srcBandOffsets[k]]);

                            // === destination value
                            if (lastRange == null || !lastRange.contains(val)) {
                                // nullify the current rane
                                lastRange = null;

                                // get a new one if the value falls within some
                                LookupItem item = table.getLookupItem(val);
                                if (item != null) {
                                    lastRange = item.getRange();
                                    destinationValue = item.getValue().intValue();
                                } else {
                                    // no match: set destination to default value (if defined)
                                    // or source value
                                    destinationValue = hasDefault ? typedDefaultValue : val;
                                }
                            }
                            destData[k][dstPixelOffset + dstBandOffsets[k]] = destinationValue;
                        }
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }

        } else {
            for (int k = 0; k < destBands; k++) {
                int destBandData[] = destData[k];
                int srcBandData[] = srcData[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < destHeight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;

                    for (int i = 0; i < destWidth; i++) {
                        // input value
                        int val = (srcBandData[srcPixelOffset]);

                        // === destination value
                        if (lastRange == null || !lastRange.contains(val)) {
                            // nullify the current rane
                            lastRange = null;

                            // get a new one if the value falls within some
                            LookupItem item = table.getLookupItem(val);
                            if (item != null) {
                                lastRange = item.getRange();
                                destinationValue = item.getValue().intValue();
                            } else {
                                // no match: set destination to default value (if defined)
                                // or source value
                                destinationValue = hasDefault ? typedDefaultValue : val;
                            }
                        }
                        destBandData[dstPixelOffset] = destinationValue;
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
        }
    }

    private void lookupAsFloatData(RasterAccessor srcAcc, RasterAccessor destAcc,
            RandomIter roiIter, boolean roiContainsTile) {
        float srcData[][] = srcAcc.getFloatDataArrays();
        float destData[][] = destAcc.getFloatDataArrays();
        int destWidth = destAcc.getWidth();
        int destHeight = destAcc.getHeight();
        int destBands = destAcc.getNumBands();

        int[] dstBandOffsets = destAcc.getBandOffsets();
        int dstPixelStride = destAcc.getPixelStride();
        int dstScanlineStride = destAcc.getScanlineStride();

        int[] srcBandOffsets = srcAcc.getBandOffsets();
        int srcPixelStride = srcAcc.getPixelStride();
        int srcScanlineStride = srcAcc.getScanlineStride();

        Range lastRange = null;

        float typedDefaultValue = hasDefault ? defaultValue.floatValue() : Float.NaN;
        float destinationValue = typedDefaultValue;

        if (hasROI && !roiContainsTile) {

            int srcScanlineOffset = 0;
            int dstScanlineOffset = 0;
            for (int j = 0, destY = destAcc.getY(); j < destHeight; j++, destY++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0, destX = destAcc.getX(); i < destWidth; i++, destX++) {

                    if (!(roiBounds.contains(destX, destY) && roiIter.getSample(destX, destY, 0) > 0)) {
                        for (int k = 0; k < destBands; k++) {
                            // input value
                            float val = hasDefault ? typedDefaultValue : (srcData[k][srcPixelOffset
                                    + srcBandOffsets[k]]);

                            destData[k][dstPixelOffset + dstBandOffsets[k]] = val;
                        }
                    } else {
                        for (int k = 0; k < destBands; k++) {
                            // input value
                            float val = (srcData[k][srcPixelOffset + srcBandOffsets[k]]);

                            // === destination value
                            if (lastRange == null || !lastRange.contains(val)) {
                                // nullify the current rane
                                lastRange = null;

                                // get a new one if the value falls within some
                                LookupItem item = table.getLookupItem(val);
                                if (item != null) {
                                    lastRange = item.getRange();
                                    destinationValue = item.getValue().floatValue();
                                } else {
                                    // no match: set destination to default value (if defined)
                                    // or source value
                                    destinationValue = hasDefault ? typedDefaultValue : val;
                                }
                            }
                            destData[k][dstPixelOffset + dstBandOffsets[k]] = destinationValue;
                        }
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }

        } else {
            for (int k = 0; k < destBands; k++) {
                float destBandData[] = destData[k];
                float srcBandData[] = srcData[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < destHeight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;

                    for (int i = 0; i < destWidth; i++) {
                        // input value
                        float val = (srcBandData[srcPixelOffset]);

                        // === destination value
                        if (lastRange == null || !lastRange.contains(val)) {
                            // nullify the current rane
                            lastRange = null;

                            // get a new one if the value falls within some
                            LookupItem item = table.getLookupItem(val);
                            if (item != null) {
                                lastRange = item.getRange();
                                destinationValue = item.getValue().floatValue();
                            } else {
                                // no match: set destination to default value (if defined)
                                // or source value
                                destinationValue = hasDefault ? typedDefaultValue : val;
                            }
                        }
                        destBandData[dstPixelOffset] = destinationValue;
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
        }
    }

    private void lookupAsDoubleData(RasterAccessor srcAcc, RasterAccessor destAcc,
            RandomIter roiIter, boolean roiContainsTile) {
        double srcData[][] = srcAcc.getDoubleDataArrays();
        double destData[][] = destAcc.getDoubleDataArrays();

        int destWidth = destAcc.getWidth();
        int destHeight = destAcc.getHeight();
        int destBands = destAcc.getNumBands();

        int[] dstBandOffsets = destAcc.getBandOffsets();
        int dstPixelStride = destAcc.getPixelStride();
        int dstScanlineStride = destAcc.getScanlineStride();

        int[] srcBandOffsets = srcAcc.getBandOffsets();
        int srcPixelStride = srcAcc.getPixelStride();
        int srcScanlineStride = srcAcc.getScanlineStride();

        Range lastRange = null;

        double typedDefaultValue = hasDefault ? defaultValue.doubleValue() : Double.NaN;
        double destinationValue = typedDefaultValue;

        if (hasROI && !roiContainsTile) {

            int srcScanlineOffset = 0;
            int dstScanlineOffset = 0;
            for (int j = 0, destY = destAcc.getY(); j < destHeight; j++, destY++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0, destX = destAcc.getX(); i < destWidth; i++, destX++) {

                    if (!(roiBounds.contains(destX, destY) && roiIter.getSample(destX, destY, 0) > 0)) {
                        for (int k = 0; k < destBands; k++) {
                            // input value
                            double val = hasDefault ? typedDefaultValue
                                    : (srcData[k][srcPixelOffset + srcBandOffsets[k]]);

                            destData[k][dstPixelOffset + dstBandOffsets[k]] = val;
                        }
                    } else {
                        for (int k = 0; k < destBands; k++) {
                            // input value
                            double val = (srcData[k][srcPixelOffset + srcBandOffsets[k]]);

                            // === destination value
                            if (lastRange == null || !lastRange.contains(val)) {
                                // nullify the current rane
                                lastRange = null;

                                // get a new one if the value falls within some
                                LookupItem item = table.getLookupItem(val);
                                if (item != null) {
                                    lastRange = item.getRange();
                                    destinationValue = item.getValue().doubleValue();
                                } else {
                                    // no match: set destination to default value (if defined)
                                    // or source value
                                    destinationValue = hasDefault ? typedDefaultValue : val;
                                }
                            }
                            destData[k][dstPixelOffset + dstBandOffsets[k]] = destinationValue;
                        }
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }

        } else {
            for (int k = 0; k < destBands; k++) {
                double destBandData[] = destData[k];
                double srcBandData[] = srcData[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < destHeight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;

                    for (int i = 0; i < destWidth; i++) {
                        // input value
                        double val = (srcBandData[srcPixelOffset]);

                        // === destination value
                        if (lastRange == null || !lastRange.contains(val)) {
                            // nullify the current rane
                            lastRange = null;

                            // get a new one if the value falls within some
                            LookupItem item = table.getLookupItem(val);
                            if (item != null) {
                                lastRange = item.getRange();
                                destinationValue = item.getValue().doubleValue();
                            } else {
                                // no match: set destination to default value (if defined)
                                // or source value
                                destinationValue = hasDefault ? typedDefaultValue : val;
                            }
                        }
                        destBandData[dstPixelOffset] = destinationValue;
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
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
