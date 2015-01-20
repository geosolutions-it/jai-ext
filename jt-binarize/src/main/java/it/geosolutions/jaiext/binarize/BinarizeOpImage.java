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
package it.geosolutions.jaiext.binarize;

import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;

import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;

import javax.media.jai.ImageLayout;
import javax.media.jai.PackedImageData;
import javax.media.jai.PixelAccessor;
import javax.media.jai.PlanarImage;
import javax.media.jai.PointOpImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.UnpackedImageData;
import javax.media.jai.iterator.RandomIter;

import com.sun.media.jai.util.ImageUtil;
import com.sun.media.jai.util.JDKWorkarounds;

public class BinarizeOpImage extends PointOpImage {

    /** Constant indicating that the inner random iterators must pre-calculate an array of the image positions */
    public static final boolean ARRAY_CALC = true;

    /** Constant indicating that the inner random iterators must cache the current tile position */
    public static final boolean TILE_CACHED = true;

    /** Boolean indicating whether Nodata check must be done or not */
    private final boolean hasNoData;

    /** Input NoData Range */
    private Range noData;

    /** Input LookupTable used for Byte data in order to increase performances on nodata check */
    private boolean[] lut;

    /** Boolean indicating whether ROI check must be done or not */
    private final boolean hasROI;

    /** ROI object used for reducing the image active area */
    private ROI roi;

    /** Flag indicating that No ROI nor NoData are present */
    private final boolean caseA;

    /** Flag indicating that only ROI is present */
    private final boolean caseB;

    /** Flag indicating that only NoData range is present */
    private final boolean caseC;

    /** Rectangle defining the bounds of the input ROI */
    private final Rectangle roiBounds;

    /** {@link PlanarImage} representing ROI */
    private PlanarImage roiImage;

    /**
     * Lookup table for output bytes.
     */
    private static byte[] byteTable = new byte[] { (byte) 0x80, (byte) 0x40, (byte) 0x20,
            (byte) 0x10, (byte) 0x08, (byte) 0x04, (byte) 0x02, (byte) 0x01, };

    /**
     * bitsOn[j + (i<<3)] sets bits on from i to j
     */
    private static int[] bitsUp = null;

    /** The threshold. */
    private double threshold;

    /**
     * Constructor.
     * 
     * @param source The source image.
     * @param layout The destination image layout.
     * @param threshold The threshold value for binarization.
     */
    public BinarizeOpImage(RenderedImage source, Map config, ImageLayout layout, double threshold,
            ROI roi, Range nodata) {
        super(source, layoutHelper(source, layout, config), config, true);

        if (source.getSampleModel().getNumBands() != 1) {
            throw new IllegalArgumentException(JaiI18N.getString("BinarizeOpImage0"));
        }

        this.threshold = threshold;

        // Check if No Data control must be done
        if (nodata != null) {
            hasNoData = true;
            this.noData = nodata;
        } else {
            hasNoData = false;
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

        // Definition of the possible cases that can be found
        // caseA = no ROI nor No Data
        // caseB = ROI present but No Data not present
        // caseC = No Data present but ROI not present
        // Last case not defined = both ROI and No Data are present
        caseA = !hasNoData && !hasROI;
        caseB = !hasNoData && hasROI;
        caseC = hasNoData && !hasROI;

        // Getting datatype
        int dataType = source.getSampleModel().getDataType();
        if (dataType == DataBuffer.TYPE_BYTE) {
            initBooleanTable();
        }
    }

    // set the OpImage's SM to be MultiPixelPackedSampleModel
    private static ImageLayout layoutHelper(RenderedImage source, ImageLayout il, Map config) {

        ImageLayout layout = (il == null) ? new ImageLayout() : (ImageLayout) il.clone();

        SampleModel sm = layout.getSampleModel(source);
        if (!ImageUtil.isBinary(sm)) {
            sm = new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE, layout.getTileWidth(source),
                    layout.getTileHeight(source), 1);
            layout.setSampleModel(sm);
        }

        ColorModel cm = layout.getColorModel(null);
        if (cm == null || !JDKWorkarounds.areCompatibleDataModels(sm, cm)) {
            layout.setColorModel(ImageUtil.getCompatibleColorModel(sm, config));
        }

        return layout;
    }

    /**
     * Map the pixels inside a specified rectangle whose value is within a rang to a constant on a per-band basis.
     * 
     * @param sources Cobbled sources, guaranteed to provide all the source data necessary for computing the rectangle.
     * @param dest The tile containing the rectangle to be computed.
     * @param destRect The rectangle within the tile to be computed.
     */
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        // ROI check
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
            switch (sources[0].getSampleModel().getDataType()) {
            case DataBuffer.TYPE_BYTE:
                byteLoop(sources[0], dest, destRect, roiIter, roiContainsTile);
                break;
            case DataBuffer.TYPE_USHORT:
                ushortLoop(sources[0], dest, destRect, roiIter, roiContainsTile);
                break;
            case DataBuffer.TYPE_SHORT:
                shortLoop(sources[0], dest, destRect, roiIter, roiContainsTile);
                break;
            case DataBuffer.TYPE_INT:
                intLoop(sources[0], dest, destRect, roiIter, roiContainsTile);
                break;
            case DataBuffer.TYPE_FLOAT:
                floatLoop(sources[0], dest, destRect, roiIter, roiContainsTile);
                break;
            case DataBuffer.TYPE_DOUBLE:
                doubleLoop(sources[0], dest, destRect, roiIter, roiContainsTile);
                break;
            default:
                throw new RuntimeException(JaiI18N.getString("BinarizeOpImage1"));
            }
        }
    }

    private void byteLoop(Raster source, WritableRaster dest, Rectangle destRect,
            RandomIter roiIter, boolean roiContainsTile) {

        if (threshold <= 0.0D && (!hasROI || (hasROI && roiContainsTile)) && !hasNoData) {
            // every bit is 1
            setTo1(dest, destRect);
            return;
        } else if (threshold > 255.0D) {
            // every bit is zeros;
            return;
        }

        // computation can be done in integer
        // even though threshold is of double type
        // through a lookup table for byte case

        Rectangle srcRect = mapDestRect(destRect, 0); // should be identical to destRect

        PixelAccessor pa = new PixelAccessor(dest.getSampleModel(), null);
        PackedImageData pid = pa.getPackedPixels(dest, destRect, true, false);
        int offset = pid.offset;
        PixelAccessor srcPa = new PixelAccessor(source.getSampleModel(), null);

        UnpackedImageData srcImD = srcPa.getPixels(source, srcRect, DataBuffer.TYPE_BYTE, false);
        int srcOffset = srcImD.bandOffsets[0];
        byte[] srcData = ((byte[][]) srcImD.data)[0];
        int pixelStride = srcImD.pixelStride;

        int ind0 = pid.bitOffset;

        int x0 = 0;
        int y0 = 0;

        int srcX = source.getMinX();
        int srcY = source.getMinY();

        // Cycles
        if (hasROI && !roiContainsTile) {
            for (int h = 0; h < destRect.height; h++) {
                int indE = ind0 + destRect.width;
                for (int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride) {

                    x0 = srcX + b - ind0;
                    y0 = srcY + h;
                    // Check on the ROI
                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        continue;
                    }
                    // Using LUT in order to skip continuous NoData check
                    if (lut[(srcData[s] & 0xFF)]) {
                        pid.data[offset + (b >> 3)] |= byteTable[b % 8];
                    }
                }
                offset += pid.lineStride;
                srcOffset += srcImD.lineStride;
            }
        } else {
            for (int h = 0; h < destRect.height; h++) {
                int indE = ind0 + destRect.width;
                for (int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride) {
                    // Using LUT in order to skip continuous NoData check
                    if (lut[(srcData[s] & 0xFF)]) {
                        pid.data[offset + (b >> 3)] |= byteTable[b % 8];
                    }
                }
                offset += pid.lineStride;
                srcOffset += srcImD.lineStride;
            }
        }

        pa.setPackedPixels(pid);
    }

    private void ushortLoop(Raster source, WritableRaster dest, Rectangle destRect,
            RandomIter roiIter, boolean roiContainsTile) {

        if (threshold <= 0.0D && (!hasROI || (hasROI && roiContainsTile)) && !hasNoData) {
            // every bit is 1
            setTo1(dest, destRect);
            return;
        } else if (threshold > (double) (0xFFFF)) {
            // every bit is zeros;
            return;
        }

        int thresholdI = (int) (Math.ceil(threshold));
        // computation can be done in integer
        // even though threshold is of double type

        Rectangle srcRect = mapDestRect(destRect, 0); // should be identical to destRect

        PixelAccessor pa = new PixelAccessor(dest.getSampleModel(), null);
        PackedImageData pid = pa.getPackedPixels(dest, destRect, true, false);
        int offset = pid.offset;
        PixelAccessor srcPa = new PixelAccessor(source.getSampleModel(), null);

        UnpackedImageData srcImD = srcPa.getPixels(source, srcRect, DataBuffer.TYPE_USHORT, false);
        int srcOffset = srcImD.bandOffsets[0];
        short[] srcData = ((short[][]) srcImD.data)[0];
        int pixelStride = srcImD.pixelStride;

        int ind0 = pid.bitOffset;

        int x0 = 0;
        int y0 = 0;

        int srcX = source.getMinX();
        int srcY = source.getMinY();

        // Cycles
        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < destRect.height; h++) {
                int indE = ind0 + destRect.width;
                for (int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride) {
                    if ((srcData[s] & 0xFFFF) >= thresholdI) {
                        pid.data[offset + (b >> 3)] |= byteTable[b % 8];
                    }
                }
                offset += pid.lineStride;
                srcOffset += srcImD.lineStride;
            }
        } else if (caseB) {
            for (int h = 0; h < destRect.height; h++) {
                int indE = ind0 + destRect.width;
                for (int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride) {

                    x0 = srcX + b - ind0;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        continue;
                    }

                    if ((srcData[s] & 0xFFFF) >= thresholdI) {
                        pid.data[offset + (b >> 3)] |= byteTable[b % 8];
                    }
                }
                offset += pid.lineStride;
                srcOffset += srcImD.lineStride;
            }
        } else if (caseC || (hasNoData && hasROI & roiContainsTile)) {
            for (int h = 0; h < destRect.height; h++) {
                int indE = ind0 + destRect.width;
                for (int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride) {

                    if (noData.contains(srcData[s])) {
                        continue;
                    }

                    if ((srcData[s] & 0xFFFF) >= thresholdI) {
                        pid.data[offset + (b >> 3)] |= byteTable[b % 8];
                    }
                }
                offset += pid.lineStride;
                srcOffset += srcImD.lineStride;
            }
        } else {
            for (int h = 0; h < destRect.height; h++) {
                int indE = ind0 + destRect.width;
                for (int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride) {

                    x0 = srcX + b - ind0;
                    y0 = srcY + h;

                    if ((!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0))
                            || noData.contains(srcData[s])) {
                        continue;
                    }

                    if ((srcData[s] & 0xFFFF) >= thresholdI) {
                        pid.data[offset + (b >> 3)] |= byteTable[b % 8];
                    }
                }
                offset += pid.lineStride;
                srcOffset += srcImD.lineStride;
            }
        }

        pa.setPackedPixels(pid);
    }

    private void shortLoop(Raster source, WritableRaster dest, Rectangle destRect,
            RandomIter roiIter, boolean roiContainsTile) {

        if (threshold <= Short.MIN_VALUE && (!hasROI || (hasROI && roiContainsTile)) && !hasNoData) {
            // every bit is 1
            setTo1(dest, destRect);
            return;
        } else if (threshold > Short.MAX_VALUE) {
            // every bit is zeros;
            return;
        }

        short thresholdS = (short) (Math.ceil(threshold));
        // computation can be done in integer
        // even though threshold is of double type

        Rectangle srcRect = mapDestRect(destRect, 0); // should be identical to destRect

        PixelAccessor pa = new PixelAccessor(dest.getSampleModel(), null);
        PackedImageData pid = pa.getPackedPixels(dest, destRect, true, false);
        int offset = pid.offset;
        PixelAccessor srcPa = new PixelAccessor(source.getSampleModel(), null);

        UnpackedImageData srcImD = srcPa.getPixels(source, srcRect, DataBuffer.TYPE_SHORT, false);
        int srcOffset = srcImD.bandOffsets[0];
        short[] srcData = ((short[][]) srcImD.data)[0];
        int pixelStride = srcImD.pixelStride;

        int ind0 = pid.bitOffset;

        int x0 = 0;
        int y0 = 0;

        int srcX = source.getMinX();
        int srcY = source.getMinY();

        // Cycles
        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < destRect.height; h++) {
                int indE = ind0 + destRect.width;
                for (int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride) {
                    if ((srcData[s]) >= thresholdS) {
                        pid.data[offset + (b >> 3)] |= byteTable[b % 8];
                    }
                }
                offset += pid.lineStride;
                srcOffset += srcImD.lineStride;
            }
        } else if (caseB) {
            for (int h = 0; h < destRect.height; h++) {
                int indE = ind0 + destRect.width;
                for (int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride) {

                    x0 = srcX + b - ind0;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        continue;
                    }

                    if ((srcData[s]) >= thresholdS) {
                        pid.data[offset + (b >> 3)] |= byteTable[b % 8];
                    }
                }
                offset += pid.lineStride;
                srcOffset += srcImD.lineStride;
            }
        } else if (caseC || (hasNoData && hasROI & roiContainsTile)) {
            for (int h = 0; h < destRect.height; h++) {
                int indE = ind0 + destRect.width;
                for (int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride) {

                    if (noData.contains(srcData[s])) {
                        continue;
                    }

                    if ((srcData[s]) >= thresholdS) {
                        pid.data[offset + (b >> 3)] |= byteTable[b % 8];
                    }
                }
                offset += pid.lineStride;
                srcOffset += srcImD.lineStride;
            }
        } else {
            for (int h = 0; h < destRect.height; h++) {
                int indE = ind0 + destRect.width;
                for (int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride) {

                    x0 = srcX + b - ind0;
                    y0 = srcY + h;

                    if ((!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0))
                            || noData.contains(srcData[s])) {
                        continue;
                    }

                    if ((srcData[s]) >= thresholdS) {
                        pid.data[offset + (b >> 3)] |= byteTable[b % 8];
                    }
                }
                offset += pid.lineStride;
                srcOffset += srcImD.lineStride;
            }
        }

        pa.setPackedPixels(pid);
    }

    private void intLoop(Raster source, WritableRaster dest, Rectangle destRect,
            RandomIter roiIter, boolean roiContainsTile) {

        if (threshold <= Integer.MIN_VALUE && (!hasROI || (hasROI && roiContainsTile))
                && !hasNoData) {
            // every bit is 1
            setTo1(dest, destRect);
            return;
        } else if (threshold > Integer.MAX_VALUE) {
            // every bit is zeros;
            return;
        }

        // computation can be done in integer
        // even though threshold is of double type

        Rectangle srcRect = mapDestRect(destRect, 0); // should be identical to destRect

        PixelAccessor pa = new PixelAccessor(dest.getSampleModel(), null);
        PackedImageData pid = pa.getPackedPixels(dest, destRect, true, false);
        int offset = pid.offset;
        PixelAccessor srcPa = new PixelAccessor(source.getSampleModel(), null);

        UnpackedImageData srcImD = srcPa.getPixels(source, srcRect, DataBuffer.TYPE_INT, false);
        int srcOffset = srcImD.bandOffsets[0];
        int[] srcData = ((int[][]) srcImD.data)[0];
        int pixelStride = srcImD.pixelStride;

        int ind0 = pid.bitOffset;

        int x0 = 0;
        int y0 = 0;

        int srcX = source.getMinX();
        int srcY = source.getMinY();

        // Cycles
        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < destRect.height; h++) {
                int indE = ind0 + destRect.width;
                for (int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride) {
                    if ((srcData[s]) >= threshold) {
                        pid.data[offset + (b >> 3)] |= byteTable[b % 8];
                    }
                }
                offset += pid.lineStride;
                srcOffset += srcImD.lineStride;
            }
        } else if (caseB) {
            for (int h = 0; h < destRect.height; h++) {
                int indE = ind0 + destRect.width;
                for (int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride) {

                    x0 = srcX + b - ind0;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        continue;
                    }

                    if ((srcData[s]) >= threshold) {
                        pid.data[offset + (b >> 3)] |= byteTable[b % 8];
                    }
                }
                offset += pid.lineStride;
                srcOffset += srcImD.lineStride;
            }
        } else if (caseC || (hasNoData && hasROI & roiContainsTile)) {
            for (int h = 0; h < destRect.height; h++) {
                int indE = ind0 + destRect.width;
                for (int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride) {

                    if (noData.contains(srcData[s])) {
                        continue;
                    }

                    if ((srcData[s]) >= threshold) {
                        pid.data[offset + (b >> 3)] |= byteTable[b % 8];
                    }
                }
                offset += pid.lineStride;
                srcOffset += srcImD.lineStride;
            }
        } else {
            for (int h = 0; h < destRect.height; h++) {
                int indE = ind0 + destRect.width;
                for (int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride) {

                    x0 = srcX + b - ind0;
                    y0 = srcY + h;

                    if ((!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0))
                            || noData.contains(srcData[s])) {
                        continue;
                    }

                    if ((srcData[s]) >= threshold) {
                        pid.data[offset + (b >> 3)] |= byteTable[b % 8];
                    }
                }
                offset += pid.lineStride;
                srcOffset += srcImD.lineStride;
            }
        }

        pa.setPackedPixels(pid);
    }

    private void floatLoop(Raster source, WritableRaster dest, Rectangle destRect,
            RandomIter roiIter, boolean roiContainsTile) {

        // computation can be done in integer
        // even though threshold is of double type

        Rectangle srcRect = mapDestRect(destRect, 0); // should be identical to destRect

        PixelAccessor pa = new PixelAccessor(dest.getSampleModel(), null);
        PackedImageData pid = pa.getPackedPixels(dest, destRect, true, false);
        int offset = pid.offset;
        PixelAccessor srcPa = new PixelAccessor(source.getSampleModel(), null);

        UnpackedImageData srcImD = srcPa.getPixels(source, srcRect, DataBuffer.TYPE_FLOAT, false);
        int srcOffset = srcImD.bandOffsets[0];
        float[] srcData = ((float[][]) srcImD.data)[0];
        int pixelStride = srcImD.pixelStride;

        int ind0 = pid.bitOffset;

        int x0 = 0;
        int y0 = 0;

        int srcX = source.getMinX();
        int srcY = source.getMinY();

        // Cycles
        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < destRect.height; h++) {
                int indE = ind0 + destRect.width;
                for (int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride) {
                    if ((srcData[s]) >= threshold) {
                        pid.data[offset + (b >> 3)] |= byteTable[b % 8];
                    }
                }
                offset += pid.lineStride;
                srcOffset += srcImD.lineStride;
            }
        } else if (caseB) {
            for (int h = 0; h < destRect.height; h++) {
                int indE = ind0 + destRect.width;
                for (int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride) {

                    x0 = srcX + b - ind0;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        continue;
                    }

                    if ((srcData[s]) >= threshold) {
                        pid.data[offset + (b >> 3)] |= byteTable[b % 8];
                    }
                }
                offset += pid.lineStride;
                srcOffset += srcImD.lineStride;
            }
        } else if (caseC || (hasNoData && hasROI & roiContainsTile)) {
            for (int h = 0; h < destRect.height; h++) {
                int indE = ind0 + destRect.width;
                for (int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride) {

                    if (noData.contains(srcData[s])) {
                        continue;
                    }

                    if ((srcData[s]) >= threshold) {
                        pid.data[offset + (b >> 3)] |= byteTable[b % 8];
                    }
                }
                offset += pid.lineStride;
                srcOffset += srcImD.lineStride;
            }
        } else {
            for (int h = 0; h < destRect.height; h++) {
                int indE = ind0 + destRect.width;
                for (int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride) {

                    x0 = srcX + b - ind0;
                    y0 = srcY + h;

                    if ((!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0))
                            || noData.contains(srcData[s])) {
                        continue;
                    }

                    if ((srcData[s]) >= threshold) {
                        pid.data[offset + (b >> 3)] |= byteTable[b % 8];
                    }
                }
                offset += pid.lineStride;
                srcOffset += srcImD.lineStride;
            }
        }

        pa.setPackedPixels(pid);
    }

    private void doubleLoop(Raster source, WritableRaster dest, Rectangle destRect,
            RandomIter roiIter, boolean roiContainsTile) {

        // computation can be done in integer
        // even though threshold is of double type

        Rectangle srcRect = mapDestRect(destRect, 0); // should be identical to destRect

        PixelAccessor pa = new PixelAccessor(dest.getSampleModel(), null);
        PackedImageData pid = pa.getPackedPixels(dest, destRect, true, false);
        int offset = pid.offset;
        PixelAccessor srcPa = new PixelAccessor(source.getSampleModel(), null);

        UnpackedImageData srcImD = srcPa.getPixels(source, srcRect, DataBuffer.TYPE_DOUBLE, false);
        int srcOffset = srcImD.bandOffsets[0];
        double[] srcData = ((double[][]) srcImD.data)[0];
        int pixelStride = srcImD.pixelStride;

        int ind0 = pid.bitOffset;

        int x0 = 0;
        int y0 = 0;

        int srcX = source.getMinX();
        int srcY = source.getMinY();

        // Cycles
        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < destRect.height; h++) {
                int indE = ind0 + destRect.width;
                for (int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride) {
                    if ((srcData[s]) >= threshold) {
                        pid.data[offset + (b >> 3)] |= byteTable[b % 8];
                    }
                }
                offset += pid.lineStride;
                srcOffset += srcImD.lineStride;
            }
        } else if (caseB) {
            for (int h = 0; h < destRect.height; h++) {
                int indE = ind0 + destRect.width;
                for (int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride) {

                    x0 = srcX + b - ind0;
                    y0 = srcY + h;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        continue;
                    }

                    if ((srcData[s]) >= threshold) {
                        pid.data[offset + (b >> 3)] |= byteTable[b % 8];
                    }
                }
                offset += pid.lineStride;
                srcOffset += srcImD.lineStride;
            }
        } else if (caseC || (hasNoData && hasROI & roiContainsTile)) {
            for (int h = 0; h < destRect.height; h++) {
                int indE = ind0 + destRect.width;
                for (int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride) {

                    if (noData.contains(srcData[s])) {
                        continue;
                    }

                    if ((srcData[s]) >= threshold) {
                        pid.data[offset + (b >> 3)] |= byteTable[b % 8];
                    }
                }
                offset += pid.lineStride;
                srcOffset += srcImD.lineStride;
            }
        } else {
            for (int h = 0; h < destRect.height; h++) {
                int indE = ind0 + destRect.width;
                for (int b = ind0, s = srcOffset; b < indE; b++, s += pixelStride) {

                    x0 = srcX + b - ind0;
                    y0 = srcY + h;

                    if ((!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0))
                            || noData.contains(srcData[s])) {
                        continue;
                    }

                    if ((srcData[s]) >= threshold) {
                        pid.data[offset + (b >> 3)] |= byteTable[b % 8];
                    }
                }
                offset += pid.lineStride;
                srcOffset += srcImD.lineStride;
            }
        }

        pa.setPackedPixels(pid);
    }

    // set all bits in a rectangular region to be 1
    // need to be sure that paddings not changing
    private void setTo1(Raster dest, Rectangle destRect) {
        initBitsUp();
        PixelAccessor pa = new PixelAccessor(dest.getSampleModel(), null);
        PackedImageData pid = pa.getPackedPixels(dest, destRect, true, false);
        int offset = pid.offset;

        for (int h = 0; h < destRect.height; h++) {
            int ind0 = pid.bitOffset;
            int indE = ind0 + destRect.width - 1;
            if (indE < 8) {
                // the entire row in data[offset]
                pid.data[offset] = (byte) (pid.data[offset] | bitsUp[indE]); // (0<<3) + indE
            } else {
                // 1st byte
                pid.data[offset] = (byte) (pid.data[offset] | bitsUp[7]); // (0<<3) + 7
                // middle bytes
                for (int b = offset + 1; b <= offset + (indE - 7) / 8; b++) {
                    pid.data[b] = (byte) (0xff);
                }
                // last byte

                int remBits = indE % 8;
                if (remBits % 8 != 7) {
                    indE = offset + indE / 8;
                    pid.data[indE] = (byte) (pid.data[indE] | bitsUp[remBits]); // (0<<3)+remBits
                }
            }
            offset += pid.lineStride;
        }
        pa.setPackedPixels(pid);
    }

    // setting bits i to j to 1;
    // i <= j
    private static synchronized void initBitsUp() {

        if (bitsUp != null)
            return;

        bitsUp = new int[64];
        for (int i = 0; i < 8; i++) {
            for (int j = i; j < 8; j++) {
                int bi = (0x00ff) >> i;
                int bj = (0x00ff) << (7 - j);
                bitsUp[j + (i << 3)] = bi & bj;
            }
        }
    }

    private void initBooleanTable() {

        if (lut != null) {
            return;
        }

        short thresholdI = (short) Math.ceil(threshold);

        lut = new boolean[256];

        // Initialize table which do a check for nodata and the threshold
        for (int i = 0; i < 256; i++) {
            if (hasNoData && noData.contains((byte) i)) {
                lut[i] = false;
            } else {
                lut[i] = (i & 0xFF) >= thresholdI;
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
