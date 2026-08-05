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
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
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
 * arrays instead of a per-pixel {@code PixelIterator}, which dominates the generic loop. All setup
 * (ROI, nodata lookup, background) is inherited unchanged; use
 * {@link MosaicOpImage#create} rather than instantiating this directly.
 */
class SingleImageMosaicOpImage extends MosaicOpImage {

    /** False when the ROI image is one the tile loop cannot read, see {@link #canReadRoi}. */
    private final boolean roiReadable;

    /** False when source or destination data does not come out as byte arrays, see {@link #byteAccessible}. */
    private final boolean byteAccessors;

    SingleImageMosaicOpImage(List sources, ImageLayout layout, Map renderingHints,
            MosaicType mosaicTypeSelected, PlanarImage[] alphaImgs, ROI[] rois, double[][] thresholds,
            double[] destinationNoData, Range[] noDatas) {
        super(sources, layout, renderingHints, mosaicTypeSelected, alphaImgs, rois, thresholds,
                destinationNoData, noDatas);
        RenderedImage roiImage = roiPresent ? imageBeans[0].getRoiImage() : null;
        this.roiReadable = roiImage == null || canReadRoi(roiImage);
        this.byteAccessors = byteAccessible(imageBeans[0].getRasterFormatTag())
                && byteAccessible(rasterFormatTag);
    }

    /**
     * True when RasterAccessor hands out the data of this tag as byte arrays. A sub-byte packed
     * layout (a 4 bit palette, say) has a byte data type but gets unpacked into int arrays, so the
     * sample model type is not the answer here.
     */
    private static boolean byteAccessible(RasterFormatTag tag) {
        return (tag.getFormatTagID() & RasterAccessorExt.DATATYPE_MASK) == DataBuffer.TYPE_BYTE;
    }

    /**
     * True if the tile loop can read the ROI straight from its raster: bilevel packed bits, or one
     * byte per pixel. Everything else (a ROI subclass returning a short or int image, say) goes back
     * to the generic mosaic, which reads any type through Raster.getSample.
     */
    private static boolean canReadRoi(RenderedImage roiImage) {
        SampleModel sm = roiImage.getSampleModel();
        return sm instanceof MultiPixelPackedSampleModel
                || (sm instanceof ComponentSampleModel && sm.getDataType() == DataBuffer.TYPE_BYTE);
    }

    /** Computes a tile from the single source, applying ROI and nodata as background. */
    @Override
    public Raster computeTile(int tileX, int tileY) {
        if (!roiReadable || !byteAccessors) {
            return super.computeTile(tileX, tileY);
        }
        WritableRaster dest = createWritableRaster(getSampleModel(),
                new Point(tileXToX(tileX), tileYToY(tileY)));
        Rectangle destRect = getTileRect(tileX, tileY);
        ImageMosaicBean bean = imageBeans[0];

        PlanarImage source = getSourceImage(0);
        Rectangle srcRect = destRect.intersection(source.getBounds());
        RasterAccessor dstAcc = new RasterAccessor(dest, destRect, rasterFormatTag, null);

        // Tiles fully outside the ROI bounding box are pure background, no source read at all. The
        // bbox test is exact for disjointness and free (no ROI materialization), unlike ROI.contains,
        // which would force the lazy reprojection-derived ROI to be computed per tile.
        ROI roi = roiPresent ? bean.getRoi() : null;
        if (roi != null && !roi.getBounds().intersects(destRect)) {
            srcRect = new Rectangle();
        }

        // The source may not cover the whole tile: the pixels it misses take the destination nodata
        // band by band, which a border extender could not do (it carries a single value).
        if (!srcRect.equals(destRect)) {
            fillBackground(dstAcc);
            // in case it's empty, bail out early
            if (srcRect.isEmpty()) {
                dstAcc.copyDataToRaster();
                return dest;
            }
        }

        Raster roiRaster = null;
        if (roi != null) {
            roiRaster = PlanarImage.wrapRenderedImage(bean.getRoiImage())
                    .getExtendedData(srcRect, zeroBorderExtender);
        }

        Raster srcData = source.getData(srcRect);
        RasterAccessor srcAcc = new RasterAccessorExt(srcData, srcRect, bean.getRasterFormatTag(),
                bean.getColorModel(), getNumBands(), DataBuffer.TYPE_BYTE);

        overlayByteLoop(srcAcc, dstAcc, roiRaster, bean.getSourceNoData() != null, srcRect);
        dstAcc.copyDataToRaster();

        // Give the scratch raster back for reuse, as the generic mosaic does. Only when the rect
        // spans several source tiles: in that case getData allocated and copied, otherwise it
        // returned a view over live cached tile data, which must not be recycled.
        if (source.overlapsMultipleTiles(srcData.getBounds())) {
            recycleTile(srcData);
        }
        return dest;
    }

    /** Fills the whole accessor area with the per band destination nodata. */
    private void fillBackground(RasterAccessor dst) {
        byte[][] d = dst.getByteDataArrays();
        int[] bandOff = dst.getBandOffsets();
        for (int b = 0; b < d.length; b++) {
            fillBand(d[b], bandOff[b], dst, destinationNoDataByte[b]);
        }
    }

    private static void fillBand(byte[] data, int bandOffset, RasterAccessor dst, byte value) {
        int line = dst.getScanlineStride();
        int pix = dst.getPixelStride();
        int w = dst.getWidth();
        for (int y = 0; y < dst.getHeight(); y++) {
            int row = bandOffset + y * line;
            for (int x = 0; x < w; x++) {
                data[row + x * pix] = value;
            }
        }
    }

    /** Composites srcRect, a sub area of the destination accessor, the rest is already background. */
    private void overlayByteLoop(RasterAccessor src, RasterAccessor dst, Raster roi,
            boolean hasNoData, Rectangle srcRect) {
        int bands = dst.getNumBands();
        byte[][] s = src.getByteDataArrays();
        byte[][] d = dst.getByteDataArrays();
        int[] sBandOff = src.getBandOffsets();
        int[] dBandOff = dst.getBandOffsets();
        int sLine = src.getScanlineStride();
        int sPix = src.getPixelStride();
        int dLine = dst.getScanlineStride();
        int dPix = dst.getPixelStride();
        int w = srcRect.width;
        int h = srcRect.height;
        // offset of the composited area inside the destination accessor
        int dBase = (srcRect.y - dst.getY()) * dLine + (srcRect.x - dst.getX()) * dPix;
        boolean[][] lut = byteLookupTable[0];

        // Read the ROI straight from its raster, set up once before the loops. A bilevel ROI is
        // kept packed (1 bit/pixel, 8 pixels/byte, no expansion); a byte ROI is read from its array.
        // Normalizing both to a byte per pixel would read better but measured 15% slower, see
        // SingleImageMosaicBenchmark.
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
                roiPacked = ImageUtil.getPackedBinaryData(roi, srcRect);
                roiRowBytes = (w + 7) / 8; // getPackedBinaryData pads each row to a byte boundary
            } else {
                RasterFormatTag tag = new RasterFormatTag(roiSm,
                        RasterAccessor.findCompatibleTag(null, roiSm));
                RasterAccessor roiAcc = new RasterAccessor(roi, srcRect, tag, null);
                roiBytes = roiAcc.getByteDataArrays()[0];
                roiLine = roiAcc.getScanlineStride();
                roiPix = roiAcc.getPixelStride();
                roiBandOff = roiAcc.getBandOffsets()[0];
            }
        }

        for (int y = 0; y < h; y++) {
            int sRow = y * sLine;
            int dRow = dBase + y * dLine;
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

    /** True for a single byte source in OVERLAY mode with no alpha: the case this class handles. */
    static boolean applies(List sources, MosaicType type, PlanarImage[] alphas) {
        if (sources.size() != 1 || type != MosaicDescriptor.MOSAIC_TYPE_OVERLAY
                || !(alphas == null || alphas.length == 0 || alphas[0] == null)) {
            return false;
        }
        RenderedImage source = (RenderedImage) sources.get(0);
        return source.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE;
    }
}
