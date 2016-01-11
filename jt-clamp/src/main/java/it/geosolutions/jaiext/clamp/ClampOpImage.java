/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2016 GeoSolutions


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
package it.geosolutions.jaiext.clamp;

import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;

import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.media.jai.ImageLayout;
import javax.media.jai.PixelAccessor;
import javax.media.jai.PlanarImage;
import javax.media.jai.PointOpImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFactory;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.UnpackedImageData;
import javax.media.jai.iterator.RandomIter;

import com.sun.media.jai.util.ImageUtil;
import com.sun.media.jai.util.JDKWorkarounds;

public class ClampOpImage extends PointOpImage {
    /** The lower bound, one for each band. */
    private final double[] low;

    /** The upper bound, one for each band. */
    private final double[] high;

    /** List of ColorModels required for IndexColorModel support */
    ColorModel colorModel;

    /** Array containing all the No Data Ranges */
    private Range noData;

    /** Boolean indicating if ROI is present */
    private final boolean hasROI;

    /** ROI bounds as a Shape */
    private Rectangle roiBounds;

    /** Boolean indicating if No Data are present */
    private final boolean hasNoData;

    /** Destination No Data value used for Byte images */
    private byte destNoDataByte;

    /** Destination No Data value used for Short/Unsigned Short images */
    private short destNoDataShort;

    /** Destination No Data value used for Integer images */
    private int destNoDataInt;

    /** Destination No Data value used for Float images */
    private float destNoDataFloat;

    /** Destination No Data value used for Double images */
    private double destNoDataDouble;

    /** Boolean indicating if No Data and ROI are not used */
    protected boolean caseA;

    /** Boolean indicating if only the ROI is used */
    protected boolean caseB;

    /** Boolean indicating if only the No Data are used */
    protected boolean caseC;

    private ROI roi;

    public ClampOpImage(RenderedImage source, Map config, Range noData, ROI roi,
            double destinationNoData, ImageLayout layout, double[] low, double[] high) {
        super(source, layout, config, true);

        // Set flag to permit in-place operation.
        permitInPlaceOperation();

        int numBands = source.getSampleModel().getNumBands();
        // if arrays contain only one element
        if (low.length == 1 && high.length == 1) {
            this.low = new double[numBands];
            this.high = new double[numBands];
            for (int i = 0; i < numBands; i++) {
                this.low[i] = low[0];
                this.high[i] = high[0];
            }
        } else {
            this.low = (double[]) low.clone();
            this.high = (double[]) high.clone();
        }

        colorModel = source.getColorModel();

        // Destination Image data Type
        int dataType = source.getSampleModel().getDataType();

        // Destination No Data value is clamped to the image data type
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            this.destNoDataByte = ImageUtil.clampRoundByte(destinationNoData);
            break;
        case DataBuffer.TYPE_USHORT:
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
            this.destNoDataDouble = destinationNoData;
            break;
        default:
            throw new IllegalArgumentException("Wrong image data type");
        }

        // Check if ROI control must be done
        if (roi != null) {
            hasROI = true;
            // ROI object
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

        // Definition of the possible cases that can be found
        // caseA = no ROI nor No Data
        // caseB = ROI present but No Data not present
        // caseC = No Data present but ROI not present
        // Last case not defined = both ROI and No Data are present
        caseA = !hasROI && !hasNoData;
        caseB = hasROI && !hasNoData;
        caseC = !hasROI && hasNoData;

    }

    /**
     * @param sources Cobbled sources, guaranteed to provide all the source data necessary for computing the rectangle.
     * @param dest The tile containing the rectangle to be computed.
     * @param destRect The rectangle within the tile to be computed.
     */
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        // Destination data type
        int destType = dest.getTransferType();

        // ROI fields
        ROI roiTile = null;

        boolean roiContainsTile = false; // roi contains sources
        boolean roiDisjointTile = false; // source no intersects the roi

        // ROI check
        if (hasROI) {
            Rectangle srcRectExpanded = mapDestRect(destRect, 0);
            // The tile dimension is extended for avoiding border errors
            srcRectExpanded.setRect(srcRectExpanded.getMinX() - 1, srcRectExpanded.getMinY() - 1,
                    srcRectExpanded.getWidth() + 2, srcRectExpanded.getHeight() + 2);
            roiTile = roi.intersect(new ROIShape(srcRectExpanded));
            // Check if the Tile bounds intersects the roi otherwise the computation is skipped
            if (!roiBounds.intersects(srcRectExpanded)) {
                roiDisjointTile = true;
            } else {
                roiContainsTile = roiTile.contains(srcRectExpanded);
                if (!roiContainsTile) {
                    if (!roiTile.intersects(srcRectExpanded)) {
                        roiDisjointTile = true;
                    }
                }
            }
        }

        if (!hasROI || !roiDisjointTile) {
            // Loop on the image raster
            RasterFormatTag[] formatTags = getFormatTags();
            Rectangle srcRect = mapDestRect(destRect, 0);
            RasterAccessor src = new RasterAccessor(sources[0], srcRect, formatTags[0],
                    getSourceImage(0).getColorModel());
            RasterAccessor dst = new RasterAccessor(dest, destRect, formatTags[1], getColorModel());

            // System.out.println(destType);
            switch (dst.getDataType()) {
            case DataBuffer.TYPE_BYTE:
                byteLoop(src, dst, roiTile, roiContainsTile);
                break;
            case DataBuffer.TYPE_USHORT:
                ushortLoop(src, dst, roiTile, roiContainsTile);
                break;
            case DataBuffer.TYPE_SHORT:
                shortLoop(src, dst, roiTile, roiContainsTile);
                break;
            case DataBuffer.TYPE_INT:
                intLoop(src, dst, roiTile, roiContainsTile);
                break;
            case DataBuffer.TYPE_FLOAT:
                floatLoop(src, dst, roiTile, roiContainsTile);
                break;
            case DataBuffer.TYPE_DOUBLE:
                doubleLoop(src, dst, roiTile, roiContainsTile);
                break;
            default:
                throw new RuntimeException("Wrong image data type");
            }
            dst.copyDataToRaster();
        } else {
            // Setting all as NoData
            int numBands = getSampleModel().getNumBands();
            double[] background = new double[numBands];
            switch (destType) {
            case DataBuffer.TYPE_BYTE:
                Arrays.fill(background, destNoDataByte);
                break;
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
                Arrays.fill(background, destNoDataShort);
                break;
            case DataBuffer.TYPE_INT:
                Arrays.fill(background, destNoDataInt);
                break;
            case DataBuffer.TYPE_FLOAT:
                Arrays.fill(background, destNoDataFloat);
                break;
            case DataBuffer.TYPE_DOUBLE:
                Arrays.fill(background, destNoDataDouble);
                break;
            default:
                throw new RuntimeException("Wrong image data type");
            }

            ImageUtil.fillBackground(dest, destRect, background);
        }

    }

    private void byteLoop(RasterAccessor src, RasterAccessor dst, ROI roiTile,
            boolean roiContainsTile) {

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        byte[][] srcData = src.getByteDataArrays();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();
        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        byte[][] dstData = dst.getByteDataArrays();

        int srcLineOffset = 0;
        int dstLineOffset = 0;

        int x0 = 0;
        int y0 = 0;

        int srcX = src.getX();
        int srcY = src.getY();

        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {

                    for (int b = 0; b < dstBands; b++) {
                        double lo = low[b];
                        double hi = high[b];

                        byte flo = (byte) lo;
                        byte fhi = (byte) hi;

                        byte sample = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                        if (sample < flo) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = flo;
                        } else if (sample > fhi) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = fhi;
                        } else {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = sample;
                        }

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

                    x0 = srcX + w;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0))) {
                        for (int b = 0; b < dstBands; b++) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataByte;
                        }

                    } else {
                        for (int b = 0; b < dstBands; b++) {
                            double lo = low[b];
                            double hi = high[b];

                            byte flo = (byte) lo;
                            byte fhi = (byte) hi;

                            byte sample = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                            if (sample < flo) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = flo;
                            } else if (sample > fhi) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = fhi;
                            } else {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = sample;
                            }

                        }
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
                        byte sample = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                        if (!noData.contains(sample)) {
                            double lo = low[b];
                            double hi = high[b];

                            byte flo = (byte) lo;
                            byte fhi = (byte) hi;

                            if (sample < flo) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = flo;
                            } else if (sample > fhi) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = fhi;
                            } else {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = sample;
                            }
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

                    x0 = srcX + w;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0))) {
                        for (int b = 0; b < dstBands; b++) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataByte;
                        }

                    } else {
                        for (int b = 0; b < dstBands; b++) {
                            byte sample = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                            if (!noData.contains(sample)) {
                                double lo = low[b];
                                double hi = high[b];

                                byte flo = (byte) lo;
                                byte fhi = (byte) hi;

                                if (sample < flo) {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = flo;
                                } else if (sample > fhi) {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = fhi;
                                } else {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = sample;
                                }
                            } else {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataByte;
                            }

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

    private void ushortLoop(RasterAccessor src, RasterAccessor dst, ROI roiTile,
            boolean roiContainsTile) {
        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        short[][] srcData = src.getShortDataArrays();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();
        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        short[][] dstData = dst.getShortDataArrays();

        int srcLineOffset = 0;
        int dstLineOffset = 0;

        int x0 = 0;
        int y0 = 0;

        int srcX = src.getX();
        int srcY = src.getY();

        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {

                    for (int b = 0; b < dstBands; b++) {
                        double lo = low[b];
                        double hi = high[b];

                        short flo = (short) lo;
                        short fhi = (short) hi;

                        short sample = ImageUtil.clampRoundUShort(srcData[b][srcPixelOffset
                                + srcBandOffsets[b]]);
                        if (sample < flo) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = flo;
                        } else if (sample > fhi) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = fhi;
                        } else {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = sample;
                        }

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

                    x0 = srcX + w;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0))) {
                        for (int b = 0; b < dstBands; b++) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataShort;
                        }

                    } else {
                        for (int b = 0; b < dstBands; b++) {
                            double lo = low[b];
                            double hi = high[b];

                            short flo = (short) lo;
                            short fhi = (short) hi;

                            short sample = ImageUtil.clampRoundUShort(srcData[b][srcPixelOffset
                                    + srcBandOffsets[b]]);
                            if (sample < flo) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = flo;
                            } else if (sample > fhi) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = fhi;
                            } else {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = sample;
                            }

                        }
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
                        short sample = ImageUtil.clampRoundUShort(srcData[b][srcPixelOffset
                                + srcBandOffsets[b]]);
                        if (!noData.contains(sample)) {
                            double lo = low[b];
                            double hi = high[b];

                            short flo = (short) lo;
                            short fhi = (short) hi;

                            if (sample < flo) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = flo;
                            } else if (sample > fhi) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = fhi;
                            } else {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = sample;
                            }
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

                    x0 = srcX + w;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0))) {
                        for (int b = 0; b < dstBands; b++) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataShort;
                        }

                    } else {
                        for (int b = 0; b < dstBands; b++) {
                            short sample = ImageUtil.clampRoundUShort(srcData[b][srcPixelOffset
                                    + srcBandOffsets[b]]);
                            if (!noData.contains(sample)) {
                                double lo = low[b];
                                double hi = high[b];

                                short flo = (short) lo;
                                short fhi = (short) hi;

                                if (sample < flo) {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = flo;
                                } else if (sample > fhi) {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = fhi;
                                } else {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = sample;
                                }
                            } else {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataShort;
                            }

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

    private void shortLoop(RasterAccessor src, RasterAccessor dst, ROI roiTile,
            boolean roiContainsTile) {
        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        short[][] srcData = src.getShortDataArrays();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();
        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        short[][] dstData = dst.getShortDataArrays();

        int srcLineOffset = 0;
        int dstLineOffset = 0;

        int x0 = 0;
        int y0 = 0;

        int srcX = src.getX();
        int srcY = src.getY();

        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {

                    for (int b = 0; b < dstBands; b++) {
                        double lo = low[b];
                        double hi = high[b];

                        short flo = (short) lo;
                        short fhi = (short) hi;

                        short sample = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                        if (sample < flo) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = flo;
                        } else if (sample > fhi) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = fhi;
                        } else {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = sample;
                        }

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

                    x0 = srcX + w;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0))) {
                        for (int b = 0; b < dstBands; b++) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataShort;
                        }

                    } else {
                        for (int b = 0; b < dstBands; b++) {
                            double lo = low[b];
                            double hi = high[b];

                            short flo = (short) lo;
                            short fhi = (short) hi;

                            short sample = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                            if (sample < flo) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = flo;
                            } else if (sample > fhi) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = fhi;
                            } else {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = sample;
                            }

                        }
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
                        short sample = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                        if (!noData.contains(sample)) {
                            double lo = low[b];
                            double hi = high[b];

                            short flo = (short) lo;
                            short fhi = (short) hi;

                            if (sample < flo) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = flo;
                            } else if (sample > fhi) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = fhi;
                            } else {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = sample;
                            }
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

                    x0 = srcX + w;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0))) {
                        for (int b = 0; b < dstBands; b++) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataShort;
                        }

                    } else {
                        for (int b = 0; b < dstBands; b++) {
                            short sample = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                            if (!noData.contains(sample)) {
                                double lo = low[b];
                                double hi = high[b];

                                short flo = (short) lo;
                                short fhi = (short) hi;

                                if (sample < flo) {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = flo;
                                } else if (sample > fhi) {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = fhi;
                                } else {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = sample;
                                }
                            } else {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataShort;
                            }

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

    private void intLoop(RasterAccessor src, RasterAccessor dst, ROI roiTile,
            boolean roiContainsTile) {
        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        int[][] srcData = src.getIntDataArrays();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();
        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        int[][] dstData = dst.getIntDataArrays();

        int srcLineOffset = 0;
        int dstLineOffset = 0;

        int x0 = 0;
        int y0 = 0;

        int srcX = src.getX();
        int srcY = src.getY();

        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {

                    for (int b = 0; b < dstBands; b++) {
                        double lo = low[b];
                        double hi = high[b];

                        int flo = (int) lo;
                        int fhi = (int) hi;

                        int sample = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                        if (sample < flo) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = flo;
                        } else if (sample > fhi) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = fhi;
                        } else {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = sample;
                        }

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

                    x0 = srcX + w;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0))) {
                        for (int b = 0; b < dstBands; b++) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataInt;
                        }

                    } else {
                        for (int b = 0; b < dstBands; b++) {
                            double lo = low[b];
                            double hi = high[b];

                            int flo = (int) lo;
                            int fhi = (int) hi;

                            int sample = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                            if (sample < flo) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = flo;
                            } else if (sample > fhi) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = fhi;
                            } else {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = sample;
                            }

                        }
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
                        int sample = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                        if (!noData.contains(sample)) {
                            double lo = low[b];
                            double hi = high[b];

                            int flo = (int) lo;
                            int fhi = (int) hi;

                            if (sample < flo) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = flo;
                            } else if (sample > fhi) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = fhi;
                            } else {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = sample;
                            }
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

                    x0 = srcX + w;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0))) {
                        for (int b = 0; b < dstBands; b++) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataInt;
                        }

                    } else {
                        for (int b = 0; b < dstBands; b++) {
                            int sample = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                            if (!noData.contains(sample)) {
                                double lo = low[b];
                                double hi = high[b];

                                int flo = (int) lo;
                                int fhi = (int) hi;

                                if (sample < flo) {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = flo;
                                } else if (sample > fhi) {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = fhi;
                                } else {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = sample;
                                }
                            } else {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataInt;
                            }

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

    private void floatLoop(RasterAccessor src, RasterAccessor dst, ROI roiTile,
            boolean roiContainsTile) {
        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        float[][] srcData = src.getFloatDataArrays();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();
        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        float[][] dstData = dst.getFloatDataArrays();

        int srcLineOffset = 0;
        int dstLineOffset = 0;

        int x0 = 0;
        int y0 = 0;

        int srcX = src.getX();
        int srcY = src.getY();

        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {

                    for (int b = 0; b < dstBands; b++) {
                        double lo = low[b];
                        double hi = high[b];

                        float flo = (float) lo;
                        float fhi = (float) hi;

                        float sample = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                        if (sample < flo) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = flo;
                        } else if (sample > fhi) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = fhi;
                        } else {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = sample;
                        }

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

                    x0 = srcX + w;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0))) {
                        for (int b = 0; b < dstBands; b++) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataFloat;
                        }

                    } else {
                        for (int b = 0; b < dstBands; b++) {
                            double lo = low[b];
                            double hi = high[b];

                            float flo = (float) lo;
                            float fhi = (float) hi;

                            float sample = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                            if (sample < flo) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = flo;
                            } else if (sample > fhi) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = fhi;
                            } else {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = sample;
                            }

                        }
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
                        float sample = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                        if (!noData.contains(sample)) {
                            double lo = low[b];
                            double hi = high[b];

                            float flo = (float) lo;
                            float fhi = (float) hi;

                            if (sample < flo) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = flo;
                            } else if (sample > fhi) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = fhi;
                            } else {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = sample;
                            }
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

                    boolean valid = false;

                    x0 = srcX + w;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0))) {
                        for (int b = 0; b < dstBands; b++) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataFloat;
                        }

                    } else {
                        for (int b = 0; b < dstBands; b++) {
                            float sample = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                            if (!noData.contains(sample)) {
                                double lo = low[b];
                                double hi = high[b];

                                float flo = (float) lo;
                                float fhi = (float) hi;

                                if (sample < flo) {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = flo;
                                } else if (sample > fhi) {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = fhi;
                                } else {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = sample;
                                }
                            } else {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataFloat;
                            }

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

    private void doubleLoop(RasterAccessor src, RasterAccessor dst, ROI roiTile,
            boolean roiContainsTile) {
        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        double[][] srcData = src.getDoubleDataArrays();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();
        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        double[][] dstData = dst.getDoubleDataArrays();

        int srcLineOffset = 0;
        int dstLineOffset = 0;

        int x0 = 0;
        int y0 = 0;

        int srcX = src.getX();
        int srcY = src.getY();

        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {

                    for (int b = 0; b < dstBands; b++) {
                        double lo = low[b];
                        double hi = high[b];

                        double flo = (double) lo;
                        double fhi = (double) hi;

                        double sample = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                        if (sample < flo) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = flo;
                        } else if (sample > fhi) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = fhi;
                        } else {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = sample;
                        }

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

                    x0 = srcX + w;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0))) {
                        for (int b = 0; b < dstBands; b++) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataDouble;
                        }

                    } else {
                        for (int b = 0; b < dstBands; b++) {
                            double lo = low[b];
                            double hi = high[b];

                            double flo = (double) lo;
                            double fhi = (double) hi;

                            double sample = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                            if (sample < flo) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = flo;
                            } else if (sample > fhi) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = fhi;
                            } else {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = sample;
                            }

                        }
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
                        double sample = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                        if (!noData.contains(sample)) {
                            double lo = low[b];
                            double hi = high[b];

                            double flo = (double) lo;
                            double fhi = (double) hi;

                            if (sample < flo) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = flo;
                            } else if (sample > fhi) {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = fhi;
                            } else {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = sample;
                            }
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

                    x0 = srcX + w;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0))) {
                        for (int b = 0; b < dstBands; b++) {
                            dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataDouble;
                        }

                    } else {
                        for (int b = 0; b < dstBands; b++) {
                            double sample = srcData[b][srcPixelOffset + srcBandOffsets[b]];
                            if (!noData.contains(sample)) {
                                double lo = low[b];
                                double hi = high[b];

                                double flo = (double) lo;
                                double fhi = (double) hi;
                                if (sample < flo) {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = flo;
                                } else if (sample > fhi) {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = fhi;
                                } else {
                                    dstData[b][dstPixelOffset + dstBandOffsets[b]] = sample;
                                }
                            } else {
                                dstData[b][dstPixelOffset + dstBandOffsets[b]] = destNoDataDouble;
                            }

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
}
