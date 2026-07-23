/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
*    http://www.geo-solutions.it/
*    Copyright 2026 GeoSolutions

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
package it.geosolutions.jaiext.mosaic;

import it.geosolutions.jaiext.range.Range;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.util.List;
import java.util.Map;
import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.operator.MosaicDescriptor;
import javax.media.jai.operator.MosaicType;

/**
 * Fast mosaic for the frequent terminal case of a single byte source composited in OVERLAY mode.
 * Produces the same output as {@link MosaicOpImage} but walks the destination with direct raster
 * arrays instead of a per-pixel {@link PixelIterator}, which dominates the generic loop. The RIF
 * instantiates it only when the case applies; all setup (ROI, nodata lookup, background) is inherited
 * unchanged from {@link MosaicOpImage}.
 */
public class SingleImageMosaicOpImage extends MosaicOpImage {

    public SingleImageMosaicOpImage(List sources, ImageLayout layout, Map renderingHints,
            MosaicType mosaicTypeSelected, PlanarImage[] alphaImgs, ROI[] rois, double[][] thresholds,
            double[] destinationNoData, Range[] noDatas) {
        super(sources, layout, renderingHints, mosaicTypeSelected, alphaImgs, rois, thresholds,
                destinationNoData, noDatas);
    }

    /** Computes a tile from the single source, applying ROI and nodata as background. */
    @Override
    public Raster computeTile(int tileX, int tileY) {
        WritableRaster dest = createWritableRaster(getSampleModel(),
                new Point(tileXToX(tileX), tileYToY(tileY)));
        Rectangle destRect = getTileRect(tileX, tileY);

        // Extend the source over the whole tile: pixels outside the source are filled with the
        // nodata/background border value, so out-of-source and nodata collapse into one test.
        PlanarImage source = getSourceImage(0);
        Raster srcData = source.getExtendedData(destRect, sourceBorderExtender);

        ImageMosaicBean bean = imageBeans[0];
        RasterAccessor srcAcc = new RasterAccessorExt(srcData, destRect, bean.getRasterFormatTag(),
                bean.getColorModel(), getNumBands(), DataBuffer.TYPE_BYTE);
        RasterAccessor dstAcc = new RasterAccessor(dest, destRect, rasterFormatTag, null);

        Raster roiRaster = null;
        if (roiPresent && bean.getRoiImage() != null) {
            roiRaster = PlanarImage.wrapRenderedImage(bean.getRoiImage())
                    .getExtendedData(destRect, zeroBorderExtender);
        }
        boolean hasNoData = bean.getSourceNoData() != null;

        overlayByteLoop(srcAcc, dstAcc, roiRaster, hasNoData, destRect);
        dstAcc.copyDataToRaster();
        return dest;
    }

    private void overlayByteLoop(RasterAccessor src, RasterAccessor dst, Raster roi,
            boolean hasNoData, Rectangle destRect) {
        int bands = dst.getNumBands();
        byte[][] s = src.getByteDataArrays();
        byte[][] d = dst.getByteDataArrays();
        int[] sBandOff = src.getBandOffsets();
        int[] dBandOff = dst.getBandOffsets();
        int sLine = src.getScanlineStride();
        int sPix = src.getPixelStride();
        int dLine = dst.getScanlineStride();
        int dPix = dst.getPixelStride();
        int w = dst.getWidth();
        int h = dst.getHeight();
        boolean[][] lut = byteLookupTable[0];

        for (int y = 0; y < h; y++) {
            int sRow = y * sLine;
            int dRow = y * dLine;
            for (int x = 0; x < w; x++) {
                int sCol = sRow + x * sPix;
                boolean background = pixelIsBackground(s, sBandOff, sCol, bands, lut, hasNoData, roi,
                        destRect.x + x, destRect.y + y);
                int dCol = dRow + x * dPix;
                for (int b = 0; b < bands; b++) {
                    d[b][dBandOff[b] + dCol] =
                            background ? destinationNoDataByte[b] : s[b][sBandOff[b] + sCol];
                }
            }
        }
    }

    /** True when the source pixel must be replaced by the background (outside ROI or nodata). */
    private boolean pixelIsBackground(byte[][] s, int[] sBandOff, int sCol, int bands,
            boolean[][] lut, boolean hasNoData, Raster roi, int absX, int absY) {
        if (roi != null && roi.getSample(absX, absY, 0) == 0) {
            return true;
        }
        if (!hasNoData) {
            return false;
        }
        for (int b = 0; b < bands; b++) {
            if (lut[b][s[b][sBandOff[b] + sCol] & 0xFF]) {
                return false;
            }
        }
        return true;
    }

    /** True if the mosaic parameters match the single byte OVERLAY source fast path. */
    static boolean applies(ParameterBlock pb, MosaicType type, PlanarImage[] alphas) {
        return pb.getNumSources() == 1
                && type == MosaicDescriptor.MOSAIC_TYPE_OVERLAY
                && (alphas == null || alphas.length == 0 || alphas[0] == null)
                && pb.getRenderedSource(0).getSampleModel().getDataType() == DataBuffer.TYPE_BYTE;
    }
}
