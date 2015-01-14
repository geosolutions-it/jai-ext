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

public class ConvolveGeneralOpImage extends ConvolveOpImage {

    public ConvolveGeneralOpImage(RenderedImage source, BorderExtender extender,
            RenderingHints hints, ImageLayout l, KernelJAI kernel, ROI roi, Range noData,
            double destinationNoData) {
        super(source, extender, hints, l, kernel, roi, noData, destinationNoData);
    }

    @Override
    protected void byteLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
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

        int x0 = 0;
        int y0 = 0;
        int srcX = src.getX();
        int srcY = src.getY();

        if (caseA || (caseB && roiContainsTile)) {

            int srcScanlineOffset = 0;
            int dstScanlineOffset = 0;

            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                y0 = srcY + j;

                for (int i = 0; i < dwidth; i++) {

                    x0 = srcX + i;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSampleDouble(x0, y0, 0) > 0)) {
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                        continue;
                    }

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

                        int val = (int) f;
                        if (val < 0) {
                            val = 0;
                        } else if (val > 255) {
                            val = 255;
                        }
                        dstData[dstPixelOffset + dstBandOffsets[k]] = (byte) val;
                    }
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        } else if (caseB) {
            for (int k = 0; k < dnumBands; k++) {
                byte dstData[] = dstDataArrays[k];
                byte srcData[] = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];
                for (int j = 0; j < dheight; j++) {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;

                    for (int i = 0; i < dwidth; i++) {
                        float f = 0.5F;
                        int kernelVerticalOffset = 0;
                        int imageVerticalOffset = srcPixelOffset;
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

                        int val = (int) f;
                        if (val < 0) {
                            val = 0;
                        } else if (val > 255) {
                            val = 255;
                        }
                        dstData[dstPixelOffset] = (byte) val;
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {

        } else {

        }

    }

    @Override
    protected void ushortLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
    }

    @Override
    protected void shortLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
    }

    @Override
    protected void intLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
    }

    @Override
    protected void floatLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
    }

    @Override
    protected void doubleLoop(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
    }
}
