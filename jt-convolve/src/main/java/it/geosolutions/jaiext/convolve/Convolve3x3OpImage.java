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
package it.geosolutions.jaiext.convolve;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.KernelJAI;
import javax.media.jai.ROI;
import javax.media.jai.RasterAccessor;
import javax.media.jai.iterator.RandomIter;

import com.sun.media.jai.util.ImageUtil;

public class Convolve3x3OpImage extends ConvolveOpImage {

    float tables[][] = new float[9][256];

    public Convolve3x3OpImage(RenderedImage source, BorderExtender extender, RenderingHints hints,
            ImageLayout l, KernelJAI kernel, ROI roi, Range noData, double destinationNoData,
            boolean skipNoData) {
        super(source, extender, hints, l, kernel, roi, noData, destinationNoData, skipNoData);

        this.kernel = kernel;
        if ((kernel.getWidth() != 3) || (kernel.getHeight() != 3) || (kernel.getXOrigin() != 1)
                || (kernel.getYOrigin() != 1)) {
            throw new RuntimeException(JaiI18N.getString("Convolve3x3OpImage0"));
        }

        if (sampleModel.getDataType() == DataBuffer.TYPE_BYTE) {
            float kdata[] = kernel.getKernelData();
            float k0 = kdata[0], k1 = kdata[1], k2 = kdata[2], k3 = kdata[3], k4 = kdata[4], k5 = kdata[5], k6 = kdata[6], k7 = kdata[7], k8 = kdata[8];

            for (int j = 0; j < 256; j++) {
                byte b = (byte) j;
                float f = (float) j;
                // noData Check
                tables[0][b + 128] = hasNoData && noData.contains(b) ? 0.5f : k0 * f + 0.5f;
                tables[1][b + 128] = hasNoData && noData.contains(b) ? 0 : k1 * f;
                tables[2][b + 128] = hasNoData && noData.contains(b) ? 0 : k2 * f;
                tables[3][b + 128] = hasNoData && noData.contains(b) ? 0 : k3 * f;
                tables[4][b + 128] = hasNoData && noData.contains(b) ? 0 : k4 * f;
                tables[5][b + 128] = hasNoData && noData.contains(b) ? 0 : k5 * f;
                tables[6][b + 128] = hasNoData && noData.contains(b) ? 0 : k6 * f;
                tables[7][b + 128] = hasNoData && noData.contains(b) ? 0 : k7 * f;
                tables[8][b + 128] = hasNoData && noData.contains(b) ? 0 : k8 * f;
            }
        }
    }

    @Override
    protected void byteLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        // cache these out to avoid an array access per kernel value
        float t0[] = tables[0], t1[] = tables[1], t2[] = tables[2], t3[] = tables[3], t4[] = tables[4], t5[] = tables[5], t6[] = tables[6], t7[] = tables[7], t8[] = tables[8];

        byte dstDataArrays[][] = dst.getByteDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        byte srcDataArrays[][] = src.getByteDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // precalculate offsets
        int centerScanlineOffset = srcScanlineStride;
        int bottomScanlineOffset = srcScanlineStride * 2;
        int middlePixelOffset = dnumBands;
        int rightPixelOffset = dnumBands * 2;

        // X,Y positions
        int x0 = 0;
        int y0 = 0;
        int srcX = src.getX();
        int srcY = src.getY();

        // ROI And NoData Check
        if (hasROI) {

            for (int k = 0; k < dnumBands; k++) {
                byte dstData[] = dstDataArrays[k];
                byte srcData[] = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < dheight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;

                    y0 = srcY + j;

                    for (int i = 0; i < dwidth; i++) {

                        x0 = srcX + i;
                        // ROI Check
                        boolean inROI = false;

                        for (int y = 0; y < kh && !inROI; y++) {
                            int yI = y0 + y;
                            for (int x = 0; x < kw && !inROI; x++) {
                                int xI = x0 + x;
                                if (roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0) {
                                    inROI = true;
                                }
                            }
                        }

                        byte b0 = srcData[srcPixelOffset];
                        byte b1 = srcData[srcPixelOffset + middlePixelOffset];
                        byte b2 = srcData[srcPixelOffset + rightPixelOffset];
                        byte b3 = srcData[srcPixelOffset + centerScanlineOffset];
                        byte b4 = srcData[srcPixelOffset + centerScanlineOffset + middlePixelOffset];
                        byte b5 = srcData[srcPixelOffset + centerScanlineOffset + rightPixelOffset];
                        byte b6 = srcData[srcPixelOffset + bottomScanlineOffset];
                        byte b7 = srcData[srcPixelOffset + bottomScanlineOffset + middlePixelOffset];
                        byte b8 = srcData[srcPixelOffset + bottomScanlineOffset + rightPixelOffset];
                        float f = t0[128 + b0] + t1[128 + b1] + t2[128 + b2] + t3[128 + b3]
                                + t4[128 + b4] + t5[128 + b5] + t6[128 + b6] + t7[128 + b7]
                                + t8[128 + b8];
                        boolean isValid = true;
                        // Check if nodata must be skipped
                        if (skipNoData) {
                            isValid = lut[b0] && lut[b1] && lut[b2] && lut[b3] && lut[b4]
                                    && lut[b5] && lut[b6] && lut[b7] && lut[b8];
                        }

                        if (isValid && inROI) {
                            // Clamping data
                            dstData[dstPixelOffset] = ImageUtil.clampRoundByte(f);
                        } else {
                            dstData[dstPixelOffset] = destNoDataByte;
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
            // Only NoData Check
        } else {
            for (int k = 0; k < dnumBands; k++) {
                byte dstData[] = dstDataArrays[k];
                byte srcData[] = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < dheight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;
                    for (int i = 0; i < dwidth; i++) {
                        byte b0 = srcData[srcPixelOffset];
                        byte b1 = srcData[srcPixelOffset + middlePixelOffset];
                        byte b2 = srcData[srcPixelOffset + rightPixelOffset];
                        byte b3 = srcData[srcPixelOffset + centerScanlineOffset];
                        byte b4 = srcData[srcPixelOffset + centerScanlineOffset + middlePixelOffset];
                        byte b5 = srcData[srcPixelOffset + centerScanlineOffset + rightPixelOffset];
                        byte b6 = srcData[srcPixelOffset + bottomScanlineOffset];
                        byte b7 = srcData[srcPixelOffset + bottomScanlineOffset + middlePixelOffset];
                        byte b8 = srcData[srcPixelOffset + bottomScanlineOffset + rightPixelOffset];
                        float f = t0[128 + b0] + t1[128 + b1] + t2[128 + b2] + t3[128 + b3]
                                + t4[128 + b4] + t5[128 + b5] + t6[128 + b6] + t7[128 + b7]
                                + t8[128 + b8];
                        boolean isValid = true;
                        // Check if nodata must be skipped
                        if (skipNoData) {
                            isValid = lut[b0] && lut[b1] && lut[b2] && lut[b3] && lut[b4]
                                    && lut[b5] && lut[b6] && lut[b7] && lut[b8];
                        }

                        if (isValid) {
                            // Clamping data
                            dstData[dstPixelOffset] = ImageUtil.clampRoundByte(f);
                        } else {
                            dstData[dstPixelOffset] = destNoDataByte;
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
        }
    }

    @Override
    protected void shortLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        short srcDataArrays[][] = src.getShortDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // precalcaculate offsets
        int centerScanlineOffset = srcScanlineStride;
        int bottomScanlineOffset = srcScanlineStride * 2;
        int middlePixelOffset = dnumBands;
        int rightPixelOffset = dnumBands * 2;

        float kdata[] = kernel.getKernelData();
        float k0 = kdata[0], k1 = kdata[1], k2 = kdata[2], k3 = kdata[3], k4 = kdata[4], k5 = kdata[5], k6 = kdata[6], k7 = kdata[7], k8 = kdata[8];

        // X,Y positions
        int x0 = 0;
        int y0 = 0;
        int srcX = src.getX();
        int srcY = src.getY();

        // Valid data
        if (caseA || (caseB && roiContainsTile)) {
            for (int k = 0; k < dnumBands; k++) {
                short dstData[] = dstDataArrays[k];
                short srcData[] = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < dheight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;
                    for (int i = 0; i < dwidth; i++) {
                        short s0 = srcData[srcPixelOffset];
                        short s1 = srcData[srcPixelOffset + middlePixelOffset];
                        short s2 = srcData[srcPixelOffset + rightPixelOffset];
                        short s3 = srcData[srcPixelOffset + centerScanlineOffset];
                        short s4 = srcData[srcPixelOffset + centerScanlineOffset
                                + middlePixelOffset];
                        short s5 = srcData[srcPixelOffset + centerScanlineOffset + rightPixelOffset];
                        short s6 = srcData[srcPixelOffset + bottomScanlineOffset];
                        short s7 = srcData[srcPixelOffset + bottomScanlineOffset
                                + middlePixelOffset];
                        short s8 = srcData[srcPixelOffset + bottomScanlineOffset + rightPixelOffset];
                        float f = k0 * s0 + k1 * s1 + k2 * s2 + k3 * s3 + k4 * s4 + k5 * s5 + k6
                                * s6 + k7 * s7 + k8 * s8;

                        dstData[dstPixelOffset] = ImageUtil.clampRoundShort(f);
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
            // ROI Check
        } else if (caseB) {
            for (int k = 0; k < dnumBands; k++) {
                short dstData[] = dstDataArrays[k];
                short srcData[] = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < dheight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;

                    y0 = srcY + j;

                    for (int i = 0; i < dwidth; i++) {

                        x0 = srcX + i;

                        boolean inROI = false;
                        // ROI Check
                        for (int y = 0; y < kh && !inROI; y++) {
                            int yI = y0 + y;
                            for (int x = 0; x < kw && !inROI; x++) {
                                int xI = x0 + x;
                                if (roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0) {
                                    inROI = true;
                                }
                            }
                        }

                        short s0 = srcData[srcPixelOffset];
                        short s1 = srcData[srcPixelOffset + middlePixelOffset];
                        short s2 = srcData[srcPixelOffset + rightPixelOffset];
                        short s3 = srcData[srcPixelOffset + centerScanlineOffset];
                        short s4 = srcData[srcPixelOffset + centerScanlineOffset
                                + middlePixelOffset];
                        short s5 = srcData[srcPixelOffset + centerScanlineOffset + rightPixelOffset];
                        short s6 = srcData[srcPixelOffset + bottomScanlineOffset];
                        short s7 = srcData[srcPixelOffset + bottomScanlineOffset
                                + middlePixelOffset];
                        short s8 = srcData[srcPixelOffset + bottomScanlineOffset + rightPixelOffset];
                        float f = k0 * s0 + k1 * s1 + k2 * s2 + k3 * s3 + k4 * s4 + k5 * s5 + k6
                                * s6 + k7 * s7 + k8 * s8;

                        if (inROI) {
                            dstData[dstPixelOffset] = ImageUtil.clampRoundShort(f);
                        } else {
                            dstData[dstPixelOffset] = destNoDataShort;
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
            // NoData Check
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int k = 0; k < dnumBands; k++) {
                short dstData[] = dstDataArrays[k];
                short srcData[] = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < dheight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;
                    for (int i = 0; i < dwidth; i++) {
                        short s0 = srcData[srcPixelOffset];
                        short s1 = srcData[srcPixelOffset + middlePixelOffset];
                        short s2 = srcData[srcPixelOffset + rightPixelOffset];
                        short s3 = srcData[srcPixelOffset + centerScanlineOffset];
                        short s4 = srcData[srcPixelOffset + centerScanlineOffset
                                + middlePixelOffset];
                        short s5 = srcData[srcPixelOffset + centerScanlineOffset + rightPixelOffset];
                        short s6 = srcData[srcPixelOffset + bottomScanlineOffset];
                        short s7 = srcData[srcPixelOffset + bottomScanlineOffset
                                + middlePixelOffset];
                        short s8 = srcData[srcPixelOffset + bottomScanlineOffset + rightPixelOffset];

                        boolean isValid = true;
                        // Boolean indicating NoData values
                        boolean nod0 = noData.contains(s0);
                        boolean nod1 = noData.contains(s1);
                        boolean nod2 = noData.contains(s2);
                        boolean nod3 = noData.contains(s3);
                        boolean nod4 = noData.contains(s4);
                        boolean nod5 = noData.contains(s5);
                        boolean nod6 = noData.contains(s6);
                        boolean nod7 = noData.contains(s7);
                        boolean nod8 = noData.contains(s8);
                        // Check if nodata must be skipped
                        if (skipNoData) {
                            isValid = !(nod0 || nod1 || nod2 || nod3 || nod4 || nod5 || nod6
                                    || nod7 || nod8);
                        }
                        // NoData Check
                        if (isValid) {
                            float f = k0 * (nod0 ? 0 : s0) + k1 * (nod1 ? 0 : s1) + k2
                                    * (nod2 ? 0 : s2) + k3 * (nod3 ? 0 : s3) + k4 * (nod4 ? 0 : s4)
                                    + k5 * (nod5 ? 0 : s5) + k6 * (nod6 ? 0 : s6) + k7
                                    * (nod7 ? 0 : s7) + k8 * (nod8 ? 0 : s8);
                            // Clamping data
                            dstData[dstPixelOffset] = ImageUtil.clampRoundShort(f);
                        } else {
                            dstData[dstPixelOffset] = destNoDataShort;
                        }
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
            // ROI and NoData Check
        } else {
            for (int k = 0; k < dnumBands; k++) {
                short dstData[] = dstDataArrays[k];
                short srcData[] = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < dheight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;

                    y0 = srcY + j;

                    for (int i = 0; i < dwidth; i++) {

                        x0 = srcX + i;

                        boolean inROI = false;
                        // ROI Check
                        for (int y = 0; y < kh && !inROI; y++) {
                            int yI = y0 + y;
                            for (int x = 0; x < kw && !inROI; x++) {
                                int xI = x0 + x;
                                if (roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0) {
                                    inROI = true;
                                }
                            }
                        }

                        short s0 = srcData[srcPixelOffset];
                        short s1 = srcData[srcPixelOffset + middlePixelOffset];
                        short s2 = srcData[srcPixelOffset + rightPixelOffset];
                        short s3 = srcData[srcPixelOffset + centerScanlineOffset];
                        short s4 = srcData[srcPixelOffset + centerScanlineOffset
                                + middlePixelOffset];
                        short s5 = srcData[srcPixelOffset + centerScanlineOffset + rightPixelOffset];
                        short s6 = srcData[srcPixelOffset + bottomScanlineOffset];
                        short s7 = srcData[srcPixelOffset + bottomScanlineOffset
                                + middlePixelOffset];
                        short s8 = srcData[srcPixelOffset + bottomScanlineOffset + rightPixelOffset];

                        boolean isValid = true;
                        // Boolean indicating NoData values
                        boolean nod0 = noData.contains(s0);
                        boolean nod1 = noData.contains(s1);
                        boolean nod2 = noData.contains(s2);
                        boolean nod3 = noData.contains(s3);
                        boolean nod4 = noData.contains(s4);
                        boolean nod5 = noData.contains(s5);
                        boolean nod6 = noData.contains(s6);
                        boolean nod7 = noData.contains(s7);
                        boolean nod8 = noData.contains(s8);
                        // Check if nodata must be skipped
                        if (skipNoData) {
                            isValid = !(nod0 || nod1 || nod2 || nod3 || nod4 || nod5 || nod6
                                    || nod7 || nod8);
                        }
                        if (inROI && isValid) {
                            float f = k0 * (nod0 ? 0 : s0) + k1 * (nod1 ? 0 : s1) + k2
                                    * (nod2 ? 0 : s2) + k3 * (nod3 ? 0 : s3) + k4 * (nod4 ? 0 : s4)
                                    + k5 * (nod5 ? 0 : s5) + k6 * (nod6 ? 0 : s6) + k7
                                    * (nod7 ? 0 : s7) + k8 * (nod8 ? 0 : s8);
                            // Clamping data
                            dstData[dstPixelOffset] = ImageUtil.clampRoundShort(f);
                        } else {
                            dstData[dstPixelOffset] = destNoDataShort;
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
        }
    }

    @Override
    protected void intLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int dstDataArrays[][] = dst.getIntDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        int srcDataArrays[][] = src.getIntDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // precalcaculate offsets
        int centerScanlineOffset = srcScanlineStride;
        int bottomScanlineOffset = srcScanlineStride * 2;
        int middlePixelOffset = dnumBands;
        int rightPixelOffset = dnumBands * 2;

        float kdata[] = kernel.getKernelData();
        float k0 = kdata[0], k1 = kdata[1], k2 = kdata[2], k3 = kdata[3], k4 = kdata[4], k5 = kdata[5], k6 = kdata[6], k7 = kdata[7], k8 = kdata[8];

        // X,Y positions
        int x0 = 0;
        int y0 = 0;
        int srcX = src.getX();
        int srcY = src.getY();

        // Valid data
        if (caseA || (caseB && roiContainsTile)) {
            for (int k = 0; k < dnumBands; k++) {
                int dstData[] = dstDataArrays[k];
                int srcData[] = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < dheight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;
                    for (int i = 0; i < dwidth; i++) {
                        int s0 = srcData[srcPixelOffset];
                        int s1 = srcData[srcPixelOffset + middlePixelOffset];
                        int s2 = srcData[srcPixelOffset + rightPixelOffset];
                        int s3 = srcData[srcPixelOffset + centerScanlineOffset];
                        int s4 = srcData[srcPixelOffset + centerScanlineOffset + middlePixelOffset];
                        int s5 = srcData[srcPixelOffset + centerScanlineOffset + rightPixelOffset];
                        int s6 = srcData[srcPixelOffset + bottomScanlineOffset];
                        int s7 = srcData[srcPixelOffset + bottomScanlineOffset + middlePixelOffset];
                        int s8 = srcData[srcPixelOffset + bottomScanlineOffset + rightPixelOffset];
                        float f = k0 * s0 + k1 * s1 + k2 * s2 + k3 * s3 + k4 * s4 + k5 * s5 + k6
                                * s6 + k7 * s7 + k8 * s8;

                        dstData[dstPixelOffset] = ImageUtil.clampRoundInt(f);
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
            // ROI Check
        } else if (caseB) {
            for (int k = 0; k < dnumBands; k++) {
                int dstData[] = dstDataArrays[k];
                int srcData[] = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < dheight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;

                    y0 = srcY + j;

                    for (int i = 0; i < dwidth; i++) {

                        x0 = srcX + i;

                        boolean inROI = false;
                        // ROI Check
                        for (int y = 0; y < kh && !inROI; y++) {
                            int yI = y0 + y;
                            for (int x = 0; x < kw && !inROI; x++) {
                                int xI = x0 + x;
                                if (roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0) {
                                    inROI = true;
                                }
                            }
                        }

                        int s0 = srcData[srcPixelOffset];
                        int s1 = srcData[srcPixelOffset + middlePixelOffset];
                        int s2 = srcData[srcPixelOffset + rightPixelOffset];
                        int s3 = srcData[srcPixelOffset + centerScanlineOffset];
                        int s4 = srcData[srcPixelOffset + centerScanlineOffset + middlePixelOffset];
                        int s5 = srcData[srcPixelOffset + centerScanlineOffset + rightPixelOffset];
                        int s6 = srcData[srcPixelOffset + bottomScanlineOffset];
                        int s7 = srcData[srcPixelOffset + bottomScanlineOffset + middlePixelOffset];
                        int s8 = srcData[srcPixelOffset + bottomScanlineOffset + rightPixelOffset];
                        float f = k0 * s0 + k1 * s1 + k2 * s2 + k3 * s3 + k4 * s4 + k5 * s5 + k6
                                * s6 + k7 * s7 + k8 * s8;

                        if (inROI) {
                            dstData[dstPixelOffset] = ImageUtil.clampRoundInt(f);
                        } else {
                            dstData[dstPixelOffset] = destNoDataInt;
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
            // NoData Check
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int k = 0; k < dnumBands; k++) {
                int dstData[] = dstDataArrays[k];
                int srcData[] = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < dheight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;
                    for (int i = 0; i < dwidth; i++) {
                        int s0 = srcData[srcPixelOffset];
                        int s1 = srcData[srcPixelOffset + middlePixelOffset];
                        int s2 = srcData[srcPixelOffset + rightPixelOffset];
                        int s3 = srcData[srcPixelOffset + centerScanlineOffset];
                        int s4 = srcData[srcPixelOffset + centerScanlineOffset + middlePixelOffset];
                        int s5 = srcData[srcPixelOffset + centerScanlineOffset + rightPixelOffset];
                        int s6 = srcData[srcPixelOffset + bottomScanlineOffset];
                        int s7 = srcData[srcPixelOffset + bottomScanlineOffset + middlePixelOffset];
                        int s8 = srcData[srcPixelOffset + bottomScanlineOffset + rightPixelOffset];

                        boolean isValid = true;
                        // Boolean indicating NoData values
                        boolean nod0 = noData.contains(s0);
                        boolean nod1 = noData.contains(s1);
                        boolean nod2 = noData.contains(s2);
                        boolean nod3 = noData.contains(s3);
                        boolean nod4 = noData.contains(s4);
                        boolean nod5 = noData.contains(s5);
                        boolean nod6 = noData.contains(s6);
                        boolean nod7 = noData.contains(s7);
                        boolean nod8 = noData.contains(s8);
                        // Check if nodata must be skipped
                        if (skipNoData) {
                            isValid = !(nod0 || nod1 || nod2 || nod3 || nod4 || nod5 || nod6
                                    || nod7 || nod8);
                        }
                        if (isValid) {
                            float f = k0 * (nod0 ? 0 : s0) + k1 * (nod1 ? 0 : s1) + k2
                                    * (nod2 ? 0 : s2) + k3 * (nod3 ? 0 : s3) + k4 * (nod4 ? 0 : s4)
                                    + k5 * (nod5 ? 0 : s5) + k6 * (nod6 ? 0 : s6) + k7
                                    * (nod7 ? 0 : s7) + k8 * (nod8 ? 0 : s8);
                            // Clamping data
                            dstData[dstPixelOffset] = ImageUtil.clampRoundInt(f);
                        } else {
                            dstData[dstPixelOffset] = destNoDataInt;
                        }
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
            // ROI and No Data Check
        } else {
            for (int k = 0; k < dnumBands; k++) {
                int dstData[] = dstDataArrays[k];
                int srcData[] = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < dheight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;

                    y0 = srcY + j;

                    for (int i = 0; i < dwidth; i++) {

                        x0 = srcX + i;

                        boolean inROI = false;
                        // ROI Check
                        for (int y = 0; y < kh && !inROI; y++) {
                            int yI = y0 + y;
                            for (int x = 0; x < kw && !inROI; x++) {
                                int xI = x0 + x;
                                if (roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0) {
                                    inROI = true;
                                }
                            }
                        }

                        int s0 = srcData[srcPixelOffset];
                        int s1 = srcData[srcPixelOffset + middlePixelOffset];
                        int s2 = srcData[srcPixelOffset + rightPixelOffset];
                        int s3 = srcData[srcPixelOffset + centerScanlineOffset];
                        int s4 = srcData[srcPixelOffset + centerScanlineOffset + middlePixelOffset];
                        int s5 = srcData[srcPixelOffset + centerScanlineOffset + rightPixelOffset];
                        int s6 = srcData[srcPixelOffset + bottomScanlineOffset];
                        int s7 = srcData[srcPixelOffset + bottomScanlineOffset + middlePixelOffset];
                        int s8 = srcData[srcPixelOffset + bottomScanlineOffset + rightPixelOffset];

                        boolean isValid = true;
                        // Boolean indicating NoData values
                        boolean nod0 = noData.contains(s0);
                        boolean nod1 = noData.contains(s1);
                        boolean nod2 = noData.contains(s2);
                        boolean nod3 = noData.contains(s3);
                        boolean nod4 = noData.contains(s4);
                        boolean nod5 = noData.contains(s5);
                        boolean nod6 = noData.contains(s6);
                        boolean nod7 = noData.contains(s7);
                        boolean nod8 = noData.contains(s8);
                        // Check if nodata must be skipped
                        if (skipNoData) {
                            isValid = !(nod0 || nod1 || nod2 || nod3 || nod4 || nod5 || nod6
                                    || nod7 || nod8);
                        }
                        if (inROI && isValid) {
                            float f = k0 * (nod0 ? 0 : s0) + k1 * (nod1 ? 0 : s1) + k2
                                    * (nod2 ? 0 : s2) + k3 * (nod3 ? 0 : s3) + k4 * (nod4 ? 0 : s4)
                                    + k5 * (nod5 ? 0 : s5) + k6 * (nod6 ? 0 : s6) + k7
                                    * (nod7 ? 0 : s7) + k8 * (nod8 ? 0 : s8);
                            // Clamping data
                            dstData[dstPixelOffset] = ImageUtil.clampRoundInt(f);
                        } else {
                            dstData[dstPixelOffset] = destNoDataInt;
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
        }
    }

    @Override
    protected void ushortLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
        throw new UnsupportedOperationException(JaiI18N.getString("Convolve3x3OpImage1"));
    }

    @Override
    protected void floatLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
        throw new UnsupportedOperationException(JaiI18N.getString("Convolve3x3OpImage1"));
    }

    @Override
    protected void doubleLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
        throw new UnsupportedOperationException(JaiI18N.getString("Convolve3x3OpImage1"));
    }

}
