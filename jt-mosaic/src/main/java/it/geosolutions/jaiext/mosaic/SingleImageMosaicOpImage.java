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
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.RenderedImage;
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
import com.sun.media.jai.util.ImageUtil;

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
        ImageMosaicBean bean = imageBeans[0];

        // Skip tiles fully outside the ROI bounding box: pure background, no source read. The bbox
        // test is exact for disjointness and free (no ROI materialization), unlike ROI.contains,
        // which would force the lazy reprojection-derived ROI to be computed per tile.
        Raster roiRaster = null;
        ROI roi = roiPresent ? bean.getRoi() : null;
        if (roi != null) {
            if (!roi.getBounds().intersects(destRect)) {
                ImageUtil.fillBackground(dest, destRect, destinationNoDataDouble);
                return dest;
            }
            roiRaster = PlanarImage.wrapRenderedImage(bean.getRoiImage())
                    .getExtendedData(destRect, zeroBorderExtender);
        }

        // Extend the source over the whole tile: pixels outside the source are filled with the
        // nodata/background border value, so out-of-source and nodata collapse into one test.
        PlanarImage source = getSourceImage(0);
        Raster srcData = source.getExtendedData(destRect, sourceBorderExtender);
        RasterAccessor srcAcc = new RasterAccessorExt(srcData, destRect, bean.getRasterFormatTag(),
                bean.getColorModel(), getNumBands(), DataBuffer.TYPE_BYTE);
        RasterAccessor dstAcc = new RasterAccessor(dest, destRect, rasterFormatTag, null);

        overlayByteLoop(srcAcc, dstAcc, roiRaster, bean.getSourceNoData() != null, destRect);
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

        // Read the ROI straight from its raster, hoisted out of the inner loop. A bilevel ROI is
        // kept packed (1 bit/pixel, 8 pixels/byte, no expansion); a byte ROI is read from its array.
        boolean hasRoi = roi != null;
        boolean roiBinary = false;
        byte[] roiPacked = null;
        int roiRowBytes = 0;
        byte[] roiBytes = null;
        int roiLine = 0;
        int roiPix = 0;
        int roiBandOff = 0;
        if (hasRoi) {
            SampleModel roiSm = roi.getSampleModel();
            if (roiSm instanceof MultiPixelPackedSampleModel) {
                roiBinary = true;
                roiPacked = ImageUtil.getPackedBinaryData(roi, destRect);
                roiRowBytes = (w + 7) / 8; // getPackedBinaryData pads each row to a byte boundary
            } else {
                RasterFormatTag tag = new RasterFormatTag(roiSm,
                        RasterAccessor.findCompatibleTag(null, roiSm));
                RasterAccessor roiAcc = new RasterAccessor(roi, destRect, tag, null);
                roiBytes = roiAcc.getByteDataArrays()[0];
                roiLine = roiAcc.getScanlineStride();
                roiPix = roiAcc.getPixelStride();
                roiBandOff = roiAcc.getBandOffsets()[0];
            }
        }

        for (int y = 0; y < h; y++) {
            int sRow = y * sLine;
            int dRow = y * dLine;
            int roiRow = roiBinary ? y * roiRowBytes : roiBandOff + y * roiLine;
            for (int x = 0; x < w; x++) {
                int sCol = sRow + x * sPix;
                boolean background;
                if (hasRoi && roiZero(roiBinary, roiPacked, roiBytes, roiRow, roiPix, x)) {
                    background = true;
                } else if (!hasNoData) {
                    background = false;
                } else {
                    background = true;
                    for (int b = 0; b < bands; b++) {
                        if (lut[b][s[b][sBandOff[b] + sCol] & 0xFF]) {
                            background = false;
                            break;
                        }
                    }
                }
                int dCol = dRow + x * dPix;
                for (int b = 0; b < bands; b++) {
                    d[b][dBandOff[b] + dCol] =
                            background ? destinationNoDataByte[b] : s[b][sBandOff[b] + sCol];
                }
            }
        }
    }

    /** True if the ROI is zero (outside) at column x of the current row, reading bits or bytes. */
    private static boolean roiZero(boolean binary, byte[] packed, byte[] bytes, int rowOffset,
            int pixelStride, int x) {
        if (binary) {
            return ((packed[rowOffset + (x >> 3)] >> (7 - (x & 7))) & 1) == 0;
        }
        return bytes[rowOffset + x * pixelStride] == 0;
    }

    /** True if the mosaic parameters match the single byte OVERLAY source fast path. */
    public static boolean applies(ParameterBlock pb, MosaicType type, PlanarImage[] alphas) {
        return pb.getNumSources() == 1 && applies(pb.getRenderedSource(0), type, alphas);
    }

    /** True if a single source in OVERLAY mode with no alpha and byte data can use the fast path. */
    public static boolean applies(RenderedImage source, MosaicType type, PlanarImage[] alphas) {
        return source != null
                && type == MosaicDescriptor.MOSAIC_TYPE_OVERLAY
                && (alphas == null || alphas.length == 0 || alphas[0] == null)
                && source.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE;
    }
}
