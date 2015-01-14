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

	public ConvolveGeneralOpImage(RenderedImage source,
			BorderExtender extender, RenderingHints hints, ImageLayout l,
			KernelJAI kernel, ROI roi, Range noData, double destinationNoData) {
		super(source, extender, hints, l, kernel, roi, noData,
				destinationNoData);
	}

	@Override
	protected void byteLoop(RasterAccessor src, RasterAccessor dst,
			RandomIter roiIter, boolean roiContainsTile) {
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

		int srcScanlineOffset = 0;
		int dstScanlineOffset = 0;

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
						int imageVerticalOffset = srcPixelOffset
								+ srcBandOffsets[k];
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

						dstData[dstPixelOffset + dstBandOffsets[k]] = ImageUtil
								.clampRoundByte(f);
					}
					srcPixelOffset += srcPixelStride;
					dstPixelOffset += dstPixelStride;
				}
				srcScanlineOffset += srcScanlineStride;
				dstScanlineOffset += dstScanlineStride;
			}
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
						int imageVerticalOffset = srcPixelOffset
								+ srcBandOffsets[k];
						// ROI check on the other kernel values
						boolean inRoi = false;

						for (int u = 0; u < kh; u++) {
							int imageOffset = imageVerticalOffset;

							int yI = y0 + u;

							for (int v = 0; v < kw; v++) {

								int xI = x0 + v;
								// Check if all the pixel kernel belongs to the
								// ROI
								if ((roiBounds.contains(xI, yI) && roiIter
										.getSample(xI, yI, 0) > 0)) {
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

		} else if (caseC || (hasROI && hasNoData && roiContainsTile)) {

		} else {

		}

	}

	@Override
	protected void ushortLoop(RasterAccessor src, RasterAccessor dst,
			RandomIter roiIter, boolean roiContainsTile) {
	}

	@Override
	protected void shortLoop(RasterAccessor src, RasterAccessor dst,
			RandomIter roiIter, boolean roiContainsTile) {
	}

	@Override
	protected void intLoop(RasterAccessor src, RasterAccessor dst,
			RandomIter roiIter, boolean roiContainsTile) {
	}

	@Override
	protected void floatLoop(RasterAccessor src, RasterAccessor dst,
			RandomIter roiIter, boolean roiContainsTile) {
	}

	@Override
	protected void doubleLoop(RasterAccessor src, RasterAccessor dst,
			RandomIter roiIter, boolean roiContainsTile) {
	}
}
