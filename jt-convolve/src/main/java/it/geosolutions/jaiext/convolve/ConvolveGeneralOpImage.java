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
import java.awt.image.RenderedImage;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.KernelJAI;
import javax.media.jai.ROI;
import javax.media.jai.RasterAccessor;
import javax.media.jai.iterator.RandomIter;

import com.sun.media.jai.util.ImageUtil;

public class ConvolveGeneralOpImage extends ConvolveOpImage {

    public ConvolveGeneralOpImage(RenderedImage source, BorderExtender extender,
            RenderingHints hints, ImageLayout l, KernelJAI kernel, ROI roi, Range noData,
            double destinationNoData, boolean skipNoData) {
        super(source, extender, hints, l, kernel, roi, noData, destinationNoData, skipNoData);
    }

    @Override
    protected void byteLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
        // Parameter definition
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        float[] kdata = kernel.getKernelData();
        int kw = kernel.getWidth();
        int kh = kernel.getHeight();

        byte dstDataArrays[][] = dst.getByteDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        byte srcDataArrays[][] = src.getByteDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
        // Definition of the positions parameters
        int x0 = 0;
        int y0 = 0;
        int srcX = src.getX();
        int srcY = src.getY();

        int srcScanlineOffset = 0;
        int dstScanlineOffset = 0;

        // Only valid data
        if (caseA || (caseB && roiContainsTile)) {

            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++) {

                    // Cycle on the bands
                    for (int k = 0; k < dnumBands; k++) {
                        byte dstData[] = dstDataArrays[k];
                        byte srcData[] = srcDataArrays[k];

                        float f = 0.5F;
                        int kernelVerticalOffset = 0;
                        int imageVerticalOffset = srcPixelOffset + srcBandOffsets[k];
                        for (int u = 0; u < kh; u++) {
                            int imageOffset = imageVerticalOffset;

                            for (int v = 0; v < kw; v++) {
                                f += ((int) srcData[imageOffset] & 0xff)
                                        * kdata[kernelVerticalOffset + v];

                                imageOffset += srcPixelStride;
                            }
                            kernelVerticalOffset += kw;
                            imageVerticalOffset += srcScanlineStride;
                        }

                        dstData[dstPixelOffset + dstBandOffsets[k]] = ImageUtil.clampRoundByte(f);
                    }
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
            // ROI check
        } else if (caseB) {
            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                y0 = srcY + j;

                for (int i = 0; i < dwidth; i++) {

                    x0 = srcX + i;
                    // Cycle on the bands
                    for (int k = 0; k < dnumBands; k++) {
                        byte dstData[] = dstDataArrays[k];
                        byte srcData[] = srcDataArrays[k];

                        float f = 0.5F;
                        int kernelVerticalOffset = 0;
                        int imageVerticalOffset = srcPixelOffset + srcBandOffsets[k];
                        // ROI check on the other kernel values
                        boolean inRoi = false;

                        for (int u = 0; u < kh; u++) {
                            int imageOffset = imageVerticalOffset;

                            int yI = y0 + u;

                            for (int v = 0; v < kw; v++) {

                                int xI = x0 + v;
                                // Check if all the pixel kernel belongs to the
                                // ROI
                                if ((roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0)) {
                                    f += ((int) srcData[imageOffset] & 0xff)
                                            * kdata[kernelVerticalOffset + v];
                                    inRoi = true;
                                }

                                imageOffset += srcPixelStride;
                            }
                            kernelVerticalOffset += kw;
                            imageVerticalOffset += srcScanlineStride;
                        }

                        if (!inRoi) {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destNoDataByte;
                        } else {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = ImageUtil
                                    .clampRoundByte(f);
                        }
                    }
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
            // NoData Check
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++) {

                    // Cycle on the bands
                    for (int k = 0; k < dnumBands; k++) {
                        byte dstData[] = dstDataArrays[k];
                        byte srcData[] = srcDataArrays[k];

                        float f = 0.5F;
                        int kernelVerticalOffset = 0;
                        int imageVerticalOffset = srcPixelOffset + srcBandOffsets[k];
                        boolean valid = true;
                        for (int u = 0; u < kh && valid; u++) {
                            int imageOffset = imageVerticalOffset;

                            for (int v = 0; v < kw && valid; v++) {

                                // Check on the nodata
                                int value = (int) srcData[imageOffset] & 0xff;
                                if (valid && lut[value]) {
                                    f += value * kdata[kernelVerticalOffset + v];
                                } else if (skipNoData) {
                                    // if skipNoData is set to true
                                    // other computations are skipped
                                    valid = false;
                                }

                                imageOffset += srcPixelStride;
                            }
                            kernelVerticalOffset += kw;
                            imageVerticalOffset += srcScanlineStride;
                        }
                        if (valid) {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = ImageUtil
                                    .clampRoundByte(f);
                        } else {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destNoDataByte;
                        }
                    }
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
            // ROI and NoData Check
        } else {
            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                y0 = srcY + j;

                for (int i = 0; i < dwidth; i++) {

                    x0 = srcX + i;
                    // Cycle on the bands
                    for (int k = 0; k < dnumBands; k++) {
                        byte dstData[] = dstDataArrays[k];
                        byte srcData[] = srcDataArrays[k];

                        float f = 0.5F;
                        int kernelVerticalOffset = 0;
                        int imageVerticalOffset = srcPixelOffset + srcBandOffsets[k];
                        // ROI check on the other kernel values
                        boolean inRoi = false;
                        boolean valid = true;
                        for (int u = 0; u < kh && valid; u++) {
                            int imageOffset = imageVerticalOffset;

                            int yI = y0 + u;

                            for (int v = 0; v < kw && valid; v++) {

                                int xI = x0 + v;
                                // Check if all the pixel kernel belongs to the
                                // ROI
                                if ((roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0)) {
                                    int value = (int) srcData[imageOffset] & 0xff;
                                    if (valid && lut[value]) {
                                        f += value * kdata[kernelVerticalOffset + v];
                                    } else if (skipNoData) {
                                        // if skipNoData is set to true
                                        // other computations are skipped
                                        valid = false;
                                    }
                                    f += ((int) srcData[imageOffset] & 0xff)
                                            * kdata[kernelVerticalOffset + v];
                                    inRoi = true;
                                }

                                imageOffset += srcPixelStride;
                            }
                            kernelVerticalOffset += kw;
                            imageVerticalOffset += srcScanlineStride;
                        }

                        if (inRoi && valid) {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = ImageUtil
                                    .clampRoundByte(f);
                        } else {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destNoDataByte;
                        }
                    }
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    @Override
    protected void ushortLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
        // Parameter definition
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        float[] kdata = kernel.getKernelData();
        int kw = kernel.getWidth();
        int kh = kernel.getHeight();

        short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        short srcDataArrays[][] = src.getShortDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
        // Definition of the positions parameters
        int x0 = 0;
        int y0 = 0;
        int srcX = src.getX();
        int srcY = src.getY();

        int srcScanlineOffset = 0;
        int dstScanlineOffset = 0;

        // Only valid data
        if (caseA || (caseB && roiContainsTile)) {

            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++) {

                    // Cycle on the bands
                    for (int k = 0; k < dnumBands; k++) {
                        short dstData[] = dstDataArrays[k];
                        short srcData[] = srcDataArrays[k];

                        float f = 0.5F;
                        int kernelVerticalOffset = 0;
                        int imageVerticalOffset = srcPixelOffset + srcBandOffsets[k];
                        for (int u = 0; u < kh; u++) {
                            int imageOffset = imageVerticalOffset;

                            for (int v = 0; v < kw; v++) {
                                f += (srcData[imageOffset] & 0xffff)
                                        * kdata[kernelVerticalOffset + v];

                                imageOffset += srcPixelStride;
                            }
                            kernelVerticalOffset += kw;
                            imageVerticalOffset += srcScanlineStride;
                        }

                        dstData[dstPixelOffset + dstBandOffsets[k]] = ImageUtil.clampRoundUShort(f);
                    }
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
            // ROI check
        } else if (caseB) {
            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                y0 = srcY + j;

                for (int i = 0; i < dwidth; i++) {

                    x0 = srcX + i;
                    // Cycle on the bands
                    for (int k = 0; k < dnumBands; k++) {
                        short dstData[] = dstDataArrays[k];
                        short srcData[] = srcDataArrays[k];

                        float f = 0.5F;
                        int kernelVerticalOffset = 0;
                        int imageVerticalOffset = srcPixelOffset + srcBandOffsets[k];
                        // ROI check on the other kernel values
                        boolean inRoi = false;

                        for (int u = 0; u < kh; u++) {
                            int imageOffset = imageVerticalOffset;

                            int yI = y0 + u;

                            for (int v = 0; v < kw; v++) {

                                int xI = x0 + v;
                                // Check if all the pixel kernel belongs to the
                                // ROI
                                if ((roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0)) {
                                    f += (srcData[imageOffset] & 0xffff)
                                            * kdata[kernelVerticalOffset + v];
                                    inRoi = true;
                                }

                                imageOffset += srcPixelStride;
                            }
                            kernelVerticalOffset += kw;
                            imageVerticalOffset += srcScanlineStride;
                        }

                        if (!inRoi) {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destNoDataShort;
                        } else {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = ImageUtil
                                    .clampRoundUShort(f);
                        }
                    }
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
            // NoData Check
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++) {

                    // Cycle on the bands
                    for (int k = 0; k < dnumBands; k++) {
                        short dstData[] = dstDataArrays[k];
                        short srcData[] = srcDataArrays[k];

                        float f = 0.5F;
                        int kernelVerticalOffset = 0;
                        int imageVerticalOffset = srcPixelOffset + srcBandOffsets[k];
                        boolean valid = true;
                        for (int u = 0; u < kh && valid; u++) {
                            int imageOffset = imageVerticalOffset;

                            for (int v = 0; v < kw && valid; v++) {

                                // Check on the nodata
                                short value = srcData[imageOffset];
                                if (valid && noData.contains(value)) {
                                    f += (value & 0xffff) * kdata[kernelVerticalOffset + v];
                                } else if (skipNoData) {
                                    // if skipNoData is set to true
                                    // other computations are skipped
                                    valid = false;
                                }

                                imageOffset += srcPixelStride;
                            }
                            kernelVerticalOffset += kw;
                            imageVerticalOffset += srcScanlineStride;
                        }
                        if (valid) {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = ImageUtil
                                    .clampRoundUShort(f);
                        } else {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destNoDataShort;
                        }
                    }
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
            // ROI and NoData Check
        } else {
            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                y0 = srcY + j;

                for (int i = 0; i < dwidth; i++) {

                    x0 = srcX + i;
                    // Cycle on the bands
                    for (int k = 0; k < dnumBands; k++) {
                        short dstData[] = dstDataArrays[k];
                        short srcData[] = srcDataArrays[k];

                        float f = 0.5F;
                        int kernelVerticalOffset = 0;
                        int imageVerticalOffset = srcPixelOffset + srcBandOffsets[k];
                        // ROI check on the other kernel values
                        boolean inRoi = false;
                        boolean valid = true;
                        for (int u = 0; u < kh && valid; u++) {
                            int imageOffset = imageVerticalOffset;

                            int yI = y0 + u;

                            for (int v = 0; v < kw && valid; v++) {

                                int xI = x0 + v;
                                // Check if all the pixel kernel belongs to the
                                // ROI
                                if ((roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0)) {
                                    short value = srcData[imageOffset];
                                    if (valid && noData.contains(value)) {
                                        f += (value & 0xFFFF) * kdata[kernelVerticalOffset + v];
                                    } else if (skipNoData) {
                                        // if skipNoData is set to true
                                        // other computations are skipped
                                        valid = false;
                                    }
                                    f += ((int) srcData[imageOffset] & 0xff)
                                            * kdata[kernelVerticalOffset + v];
                                    inRoi = true;
                                }

                                imageOffset += srcPixelStride;
                            }
                            kernelVerticalOffset += kw;
                            imageVerticalOffset += srcScanlineStride;
                        }

                        if (inRoi && valid) {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = ImageUtil
                                    .clampRoundUShort(f);
                        } else {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destNoDataShort;
                        }
                    }
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }

    }

    @Override
    protected void shortLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
        // Parameter definition
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        float[] kdata = kernel.getKernelData();
        int kw = kernel.getWidth();
        int kh = kernel.getHeight();

        short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        short srcDataArrays[][] = src.getShortDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
        // Definition of the positions parameters
        int x0 = 0;
        int y0 = 0;
        int srcX = src.getX();
        int srcY = src.getY();

        int srcScanlineOffset = 0;
        int dstScanlineOffset = 0;

        // Only valid data
        if (caseA || (caseB && roiContainsTile)) {

            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++) {

                    // Cycle on the bands
                    for (int k = 0; k < dnumBands; k++) {
                        short dstData[] = dstDataArrays[k];
                        short srcData[] = srcDataArrays[k];

                        float f = 0.5F;
                        int kernelVerticalOffset = 0;
                        int imageVerticalOffset = srcPixelOffset + srcBandOffsets[k];
                        for (int u = 0; u < kh; u++) {
                            int imageOffset = imageVerticalOffset;

                            for (int v = 0; v < kw; v++) {
                                f += (srcData[imageOffset]) * kdata[kernelVerticalOffset + v];

                                imageOffset += srcPixelStride;
                            }
                            kernelVerticalOffset += kw;
                            imageVerticalOffset += srcScanlineStride;
                        }

                        dstData[dstPixelOffset + dstBandOffsets[k]] = ImageUtil.clampRoundShort(f);
                    }
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
            // ROI check
        } else if (caseB) {
            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                y0 = srcY + j;

                for (int i = 0; i < dwidth; i++) {

                    x0 = srcX + i;
                    // Cycle on the bands
                    for (int k = 0; k < dnumBands; k++) {
                        short dstData[] = dstDataArrays[k];
                        short srcData[] = srcDataArrays[k];

                        float f = 0.5F;
                        int kernelVerticalOffset = 0;
                        int imageVerticalOffset = srcPixelOffset + srcBandOffsets[k];
                        // ROI check on the other kernel values
                        boolean inRoi = false;

                        for (int u = 0; u < kh; u++) {
                            int imageOffset = imageVerticalOffset;

                            int yI = y0 + u;

                            for (int v = 0; v < kw; v++) {

                                int xI = x0 + v;
                                // Check if all the pixel kernel belongs to the
                                // ROI
                                if ((roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0)) {
                                    f += (srcData[imageOffset]) * kdata[kernelVerticalOffset + v];
                                    inRoi = true;
                                }

                                imageOffset += srcPixelStride;
                            }
                            kernelVerticalOffset += kw;
                            imageVerticalOffset += srcScanlineStride;
                        }

                        if (!inRoi) {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destNoDataShort;
                        } else {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = ImageUtil
                                    .clampRoundShort(f);
                        }
                    }
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
            // NoData Check
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++) {

                    // Cycle on the bands
                    for (int k = 0; k < dnumBands; k++) {
                        short dstData[] = dstDataArrays[k];
                        short srcData[] = srcDataArrays[k];

                        float f = 0.5F;
                        int kernelVerticalOffset = 0;
                        int imageVerticalOffset = srcPixelOffset + srcBandOffsets[k];
                        boolean valid = true;
                        for (int u = 0; u < kh && valid; u++) {
                            int imageOffset = imageVerticalOffset;

                            for (int v = 0; v < kw && valid; v++) {

                                // Check on the nodata
                                short value = srcData[imageOffset];
                                if (valid && noData.contains(value)) {
                                    f += (value) * kdata[kernelVerticalOffset + v];
                                } else if (skipNoData) {
                                    // if skipNoData is set to true
                                    // other computations are skipped
                                    valid = false;
                                }

                                imageOffset += srcPixelStride;
                            }
                            kernelVerticalOffset += kw;
                            imageVerticalOffset += srcScanlineStride;
                        }
                        if (valid) {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = ImageUtil
                                    .clampRoundShort(f);
                        } else {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destNoDataShort;
                        }
                    }
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
            // ROI and NoData Check
        } else {
            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                y0 = srcY + j;

                for (int i = 0; i < dwidth; i++) {

                    x0 = srcX + i;
                    // Cycle on the bands
                    for (int k = 0; k < dnumBands; k++) {
                        short dstData[] = dstDataArrays[k];
                        short srcData[] = srcDataArrays[k];

                        float f = 0.5F;
                        int kernelVerticalOffset = 0;
                        int imageVerticalOffset = srcPixelOffset + srcBandOffsets[k];
                        // ROI check on the other kernel values
                        boolean inRoi = false;
                        boolean valid = true;
                        for (int u = 0; u < kh && valid; u++) {
                            int imageOffset = imageVerticalOffset;

                            int yI = y0 + u;

                            for (int v = 0; v < kw && valid; v++) {

                                int xI = x0 + v;
                                // Check if all the pixel kernel belongs to the
                                // ROI
                                if ((roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0)) {
                                    short value = srcData[imageOffset];
                                    if (valid && noData.contains(value)) {
                                        f += (value) * kdata[kernelVerticalOffset + v];
                                    } else if (skipNoData) {
                                        // if skipNoData is set to true
                                        // other computations are skipped
                                        valid = false;
                                    }
                                    f += ((int) srcData[imageOffset] & 0xff)
                                            * kdata[kernelVerticalOffset + v];
                                    inRoi = true;
                                }

                                imageOffset += srcPixelStride;
                            }
                            kernelVerticalOffset += kw;
                            imageVerticalOffset += srcScanlineStride;
                        }

                        if (inRoi && valid) {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = ImageUtil
                                    .clampRoundShort(f);
                        } else {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destNoDataShort;
                        }
                    }
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    @Override
    protected void intLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
        // Parameter definition
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        float[] kdata = kernel.getKernelData();
        int kw = kernel.getWidth();
        int kh = kernel.getHeight();

        int dstDataArrays[][] = dst.getIntDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        int srcDataArrays[][] = src.getIntDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
        // Definition of the positions parameters
        int x0 = 0;
        int y0 = 0;
        int srcX = src.getX();
        int srcY = src.getY();

        int srcScanlineOffset = 0;
        int dstScanlineOffset = 0;

        // Only valid data
        if (caseA || (caseB && roiContainsTile)) {

            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++) {

                    // Cycle on the bands
                    for (int k = 0; k < dnumBands; k++) {
                        int dstData[] = dstDataArrays[k];
                        int srcData[] = srcDataArrays[k];

                        float f = 0.5F;
                        int kernelVerticalOffset = 0;
                        int imageVerticalOffset = srcPixelOffset + srcBandOffsets[k];
                        for (int u = 0; u < kh; u++) {
                            int imageOffset = imageVerticalOffset;

                            for (int v = 0; v < kw; v++) {
                                f += (srcData[imageOffset]) * kdata[kernelVerticalOffset + v];

                                imageOffset += srcPixelStride;
                            }
                            kernelVerticalOffset += kw;
                            imageVerticalOffset += srcScanlineStride;
                        }

                        dstData[dstPixelOffset + dstBandOffsets[k]] = ImageUtil.clampRoundInt(f);
                    }
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
            // ROI check
        } else if (caseB) {
            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                y0 = srcY + j;

                for (int i = 0; i < dwidth; i++) {

                    x0 = srcX + i;
                    // Cycle on the bands
                    for (int k = 0; k < dnumBands; k++) {
                        int dstData[] = dstDataArrays[k];
                        int srcData[] = srcDataArrays[k];

                        float f = 0.5F;
                        int kernelVerticalOffset = 0;
                        int imageVerticalOffset = srcPixelOffset + srcBandOffsets[k];
                        // ROI check on the other kernel values
                        boolean inRoi = false;

                        for (int u = 0; u < kh; u++) {
                            int imageOffset = imageVerticalOffset;

                            int yI = y0 + u;

                            for (int v = 0; v < kw; v++) {

                                int xI = x0 + v;
                                // Check if all the pixel kernel belongs to the
                                // ROI
                                if ((roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0)) {
                                    f += (srcData[imageOffset]) * kdata[kernelVerticalOffset + v];
                                    inRoi = true;
                                }

                                imageOffset += srcPixelStride;
                            }
                            kernelVerticalOffset += kw;
                            imageVerticalOffset += srcScanlineStride;
                        }

                        if (!inRoi) {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destNoDataInt;
                        } else {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = ImageUtil
                                    .clampRoundInt(f);
                        }
                    }
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
            // NoData Check
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++) {

                    // Cycle on the bands
                    for (int k = 0; k < dnumBands; k++) {
                        int dstData[] = dstDataArrays[k];
                        int srcData[] = srcDataArrays[k];

                        float f = 0.5F;
                        int kernelVerticalOffset = 0;
                        int imageVerticalOffset = srcPixelOffset + srcBandOffsets[k];
                        boolean valid = true;
                        for (int u = 0; u < kh && valid; u++) {
                            int imageOffset = imageVerticalOffset;

                            for (int v = 0; v < kw && valid; v++) {

                                // Check on the nodata
                                int value = srcData[imageOffset];
                                if (valid && noData.contains(value)) {
                                    f += (value) * kdata[kernelVerticalOffset + v];
                                } else if (skipNoData) {
                                    // if skipNoData is set to true
                                    // other computations are skipped
                                    valid = false;
                                }

                                imageOffset += srcPixelStride;
                            }
                            kernelVerticalOffset += kw;
                            imageVerticalOffset += srcScanlineStride;
                        }
                        if (valid) {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = ImageUtil
                                    .clampRoundInt(f);
                        } else {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destNoDataInt;
                        }
                    }
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
            // ROI and NoData Check
        } else {
            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                y0 = srcY + j;

                for (int i = 0; i < dwidth; i++) {

                    x0 = srcX + i;
                    // Cycle on the bands
                    for (int k = 0; k < dnumBands; k++) {
                        int dstData[] = dstDataArrays[k];
                        int srcData[] = srcDataArrays[k];

                        float f = 0.5F;
                        int kernelVerticalOffset = 0;
                        int imageVerticalOffset = srcPixelOffset + srcBandOffsets[k];
                        // ROI check on the other kernel values
                        boolean inRoi = false;
                        boolean valid = true;
                        for (int u = 0; u < kh && valid; u++) {
                            int imageOffset = imageVerticalOffset;

                            int yI = y0 + u;

                            for (int v = 0; v < kw && valid; v++) {

                                int xI = x0 + v;
                                // Check if all the pixel kernel belongs to the
                                // ROI
                                if ((roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0)) {
                                    int value = srcData[imageOffset];
                                    if (valid && noData.contains(value)) {
                                        f += (value) * kdata[kernelVerticalOffset + v];
                                    } else if (skipNoData) {
                                        // if skipNoData is set to true
                                        // other computations are skipped
                                        valid = false;
                                    }
                                    f += ((int) srcData[imageOffset] & 0xff)
                                            * kdata[kernelVerticalOffset + v];
                                    inRoi = true;
                                }

                                imageOffset += srcPixelStride;
                            }
                            kernelVerticalOffset += kw;
                            imageVerticalOffset += srcScanlineStride;
                        }

                        if (inRoi && valid) {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = ImageUtil
                                    .clampRoundInt(f);
                        } else {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destNoDataInt;
                        }
                    }
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    @Override
    protected void floatLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
        // Parameter definition
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        float[] kdata = kernel.getKernelData();
        int kw = kernel.getWidth();
        int kh = kernel.getHeight();

        float dstDataArrays[][] = dst.getFloatDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        float srcDataArrays[][] = src.getFloatDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
        // Definition of the positions parameters
        int x0 = 0;
        int y0 = 0;
        int srcX = src.getX();
        int srcY = src.getY();

        int srcScanlineOffset = 0;
        int dstScanlineOffset = 0;

        // Only valid data
        if (caseA || (caseB && roiContainsTile)) {

            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++) {

                    // Cycle on the bands
                    for (int k = 0; k < dnumBands; k++) {
                        float dstData[] = dstDataArrays[k];
                        float srcData[] = srcDataArrays[k];

                        float f = 0.5F;
                        int kernelVerticalOffset = 0;
                        int imageVerticalOffset = srcPixelOffset + srcBandOffsets[k];
                        for (int u = 0; u < kh; u++) {
                            int imageOffset = imageVerticalOffset;

                            for (int v = 0; v < kw; v++) {
                                f += (srcData[imageOffset]) * kdata[kernelVerticalOffset + v];

                                imageOffset += srcPixelStride;
                            }
                            kernelVerticalOffset += kw;
                            imageVerticalOffset += srcScanlineStride;
                        }

                        dstData[dstPixelOffset + dstBandOffsets[k]] = f;
                    }
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
            // ROI check
        } else if (caseB) {
            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                y0 = srcY + j;

                for (int i = 0; i < dwidth; i++) {

                    x0 = srcX + i;
                    // Cycle on the bands
                    for (int k = 0; k < dnumBands; k++) {
                        float dstData[] = dstDataArrays[k];
                        float srcData[] = srcDataArrays[k];

                        float f = 0.5F;
                        int kernelVerticalOffset = 0;
                        int imageVerticalOffset = srcPixelOffset + srcBandOffsets[k];
                        // ROI check on the other kernel values
                        boolean inRoi = false;

                        for (int u = 0; u < kh; u++) {
                            int imageOffset = imageVerticalOffset;

                            int yI = y0 + u;

                            for (int v = 0; v < kw; v++) {

                                int xI = x0 + v;
                                // Check if all the pixel kernel belongs to the
                                // ROI
                                if ((roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0)) {
                                    f += (srcData[imageOffset]) * kdata[kernelVerticalOffset + v];
                                    inRoi = true;
                                }

                                imageOffset += srcPixelStride;
                            }
                            kernelVerticalOffset += kw;
                            imageVerticalOffset += srcScanlineStride;
                        }

                        if (!inRoi) {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destNoDataFloat;
                        } else {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = f;
                        }
                    }
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
            // NoData Check
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++) {

                    // Cycle on the bands
                    for (int k = 0; k < dnumBands; k++) {
                        float dstData[] = dstDataArrays[k];
                        float srcData[] = srcDataArrays[k];

                        float f = 0.5F;
                        int kernelVerticalOffset = 0;
                        int imageVerticalOffset = srcPixelOffset + srcBandOffsets[k];
                        boolean valid = true;
                        for (int u = 0; u < kh && valid; u++) {
                            int imageOffset = imageVerticalOffset;

                            for (int v = 0; v < kw && valid; v++) {

                                // Check on the nodata
                                float value = srcData[imageOffset];
                                if (valid && noData.contains(value)) {
                                    f += (value) * kdata[kernelVerticalOffset + v];
                                } else if (skipNoData) {
                                    // if skipNoData is set to true
                                    // other computations are skipped
                                    valid = false;
                                }

                                imageOffset += srcPixelStride;
                            }
                            kernelVerticalOffset += kw;
                            imageVerticalOffset += srcScanlineStride;
                        }
                        if (valid) {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = f;
                        } else {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destNoDataFloat;
                        }
                    }
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
            // ROI and NoData Check
        } else {
            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                y0 = srcY + j;

                for (int i = 0; i < dwidth; i++) {

                    x0 = srcX + i;
                    // Cycle on the bands
                    for (int k = 0; k < dnumBands; k++) {
                        float dstData[] = dstDataArrays[k];
                        float srcData[] = srcDataArrays[k];

                        float f = 0.5F;
                        int kernelVerticalOffset = 0;
                        int imageVerticalOffset = srcPixelOffset + srcBandOffsets[k];
                        // ROI check on the other kernel values
                        boolean inRoi = false;
                        boolean valid = true;
                        for (int u = 0; u < kh && valid; u++) {
                            int imageOffset = imageVerticalOffset;

                            int yI = y0 + u;

                            for (int v = 0; v < kw && valid; v++) {

                                int xI = x0 + v;
                                // Check if all the pixel kernel belongs to the
                                // ROI
                                if ((roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0)) {
                                    float value = srcData[imageOffset];
                                    if (valid && noData.contains(value)) {
                                        f += (value) * kdata[kernelVerticalOffset + v];
                                    } else if (skipNoData) {
                                        // if skipNoData is set to true
                                        // other computations are skipped
                                        valid = false;
                                    }
                                    f += ((int) srcData[imageOffset] & 0xff)
                                            * kdata[kernelVerticalOffset + v];
                                    inRoi = true;
                                }

                                imageOffset += srcPixelStride;
                            }
                            kernelVerticalOffset += kw;
                            imageVerticalOffset += srcScanlineStride;
                        }

                        if (inRoi && valid) {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = f;
                        } else {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destNoDataFloat;
                        }
                    }
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    @Override
    protected void doubleLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
        // Parameter definition
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        float[] kdata = kernel.getKernelData();
        int kw = kernel.getWidth();
        int kh = kernel.getHeight();

        double dstDataArrays[][] = dst.getDoubleDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        double srcDataArrays[][] = src.getDoubleDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
        // Definition of the positions parameters
        int x0 = 0;
        int y0 = 0;
        int srcX = src.getX();
        int srcY = src.getY();

        int srcScanlineOffset = 0;
        int dstScanlineOffset = 0;

        // Only valid data
        if (caseA || (caseB && roiContainsTile)) {

            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++) {

                    // Cycle on the bands
                    for (int k = 0; k < dnumBands; k++) {
                        double dstData[] = dstDataArrays[k];
                        double srcData[] = srcDataArrays[k];

                        double f = 0.5D;
                        int kernelVerticalOffset = 0;
                        int imageVerticalOffset = srcPixelOffset + srcBandOffsets[k];
                        for (int u = 0; u < kh; u++) {
                            int imageOffset = imageVerticalOffset;

                            for (int v = 0; v < kw; v++) {
                                f += (srcData[imageOffset]) * kdata[kernelVerticalOffset + v];

                                imageOffset += srcPixelStride;
                            }
                            kernelVerticalOffset += kw;
                            imageVerticalOffset += srcScanlineStride;
                        }

                        dstData[dstPixelOffset + dstBandOffsets[k]] = f;
                    }
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
            // ROI check
        } else if (caseB) {
            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                y0 = srcY + j;

                for (int i = 0; i < dwidth; i++) {

                    x0 = srcX + i;
                    // Cycle on the bands
                    for (int k = 0; k < dnumBands; k++) {
                        double dstData[] = dstDataArrays[k];
                        double srcData[] = srcDataArrays[k];

                        double f = 0.5D;
                        int kernelVerticalOffset = 0;
                        int imageVerticalOffset = srcPixelOffset + srcBandOffsets[k];
                        // ROI check on the other kernel values
                        boolean inRoi = false;

                        for (int u = 0; u < kh; u++) {
                            int imageOffset = imageVerticalOffset;

                            int yI = y0 + u;

                            for (int v = 0; v < kw; v++) {

                                int xI = x0 + v;
                                // Check if all the pixel kernel belongs to the
                                // ROI
                                if ((roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0)) {
                                    f += (srcData[imageOffset]) * kdata[kernelVerticalOffset + v];
                                    inRoi = true;
                                }

                                imageOffset += srcPixelStride;
                            }
                            kernelVerticalOffset += kw;
                            imageVerticalOffset += srcScanlineStride;
                        }

                        if (!inRoi) {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destNoDataDouble;
                        } else {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = f;
                        }
                    }
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
            // NoData Check
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++) {

                    // Cycle on the bands
                    for (int k = 0; k < dnumBands; k++) {
                        double dstData[] = dstDataArrays[k];
                        double srcData[] = srcDataArrays[k];

                        double f = 0.5D;
                        int kernelVerticalOffset = 0;
                        int imageVerticalOffset = srcPixelOffset + srcBandOffsets[k];
                        boolean valid = true;
                        for (int u = 0; u < kh && valid; u++) {
                            int imageOffset = imageVerticalOffset;

                            for (int v = 0; v < kw && valid; v++) {

                                // Check on the nodata
                                double value = srcData[imageOffset];
                                if (valid && noData.contains(value)) {
                                    f += (value) * kdata[kernelVerticalOffset + v];
                                } else if (skipNoData) {
                                    // if skipNoData is set to true
                                    // other computations are skipped
                                    valid = false;
                                }

                                imageOffset += srcPixelStride;
                            }
                            kernelVerticalOffset += kw;
                            imageVerticalOffset += srcScanlineStride;
                        }
                        if (valid) {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = f;
                        } else {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destNoDataDouble;
                        }
                    }
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
            // ROI and NoData Check
        } else {
            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                y0 = srcY + j;

                for (int i = 0; i < dwidth; i++) {

                    x0 = srcX + i;
                    // Cycle on the bands
                    for (int k = 0; k < dnumBands; k++) {
                        double dstData[] = dstDataArrays[k];
                        double srcData[] = srcDataArrays[k];

                        double f = 0.5D;
                        int kernelVerticalOffset = 0;
                        int imageVerticalOffset = srcPixelOffset + srcBandOffsets[k];
                        // ROI check on the other kernel values
                        boolean inRoi = false;
                        boolean valid = true;
                        for (int u = 0; u < kh && valid; u++) {
                            int imageOffset = imageVerticalOffset;

                            int yI = y0 + u;

                            for (int v = 0; v < kw && valid; v++) {

                                int xI = x0 + v;
                                // Check if all the pixel kernel belongs to the
                                // ROI
                                if ((roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0)) {
                                    double value = srcData[imageOffset];
                                    if (valid && noData.contains(value)) {
                                        f += (value) * kdata[kernelVerticalOffset + v];
                                    } else if (skipNoData) {
                                        // if skipNoData is set to true
                                        // other computations are skipped
                                        valid = false;
                                    }
                                    f += ((int) srcData[imageOffset] & 0xff)
                                            * kdata[kernelVerticalOffset + v];
                                    inRoi = true;
                                }

                                imageOffset += srcPixelStride;
                            }
                            kernelVerticalOffset += kw;
                            imageVerticalOffset += srcScanlineStride;
                        }

                        if (inRoi && valid) {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = f;
                        } else {
                            dstData[dstPixelOffset + dstBandOffsets[k]] = destNoDataDouble;
                        }
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
