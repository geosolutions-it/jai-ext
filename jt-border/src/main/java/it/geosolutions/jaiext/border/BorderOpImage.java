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
package it.geosolutions.jaiext.border;

import it.geosolutions.jaiext.range.Range;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import javax.media.jai.BorderExtender;
import javax.media.jai.BorderExtenderZero;
import javax.media.jai.ImageLayout;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;

import com.sun.media.jai.util.ImageUtil;

import java.util.Map;

/**
 * An <code>OpImage</code> implementing the "border" operation.
 * 
 * <p>
 * It adds a border around a source image. The size of the border is specified by the left, right, top, and bottom padding parameters. The border may
 * be filled in a variety of ways specified by the border type parameter:
 * <ul>
 * <li>it may be extended with zeros (BORDER_ZERO_FILL);
 * <li>it may be extended with a constant set of values (BORDER_CONST_FILL);
 * <li) it may be created by copying the edge and corner pixels (BORDER_EXTEND);
 * <li>it may be created by reflection about the edges of the image (BORDER_REFLECT); or,
 * <li>it may be extended by "wrapping" the image plane toroidally, that is, joining opposite edges of the image.
 * </ul>
 * 
 * <p>
 * When choosing the <code>BORDER_CONST_FILL</code> option, an array of constants must be supplied to the extender. The array must have at least one
 * element, in which case this same constant is applied to all image bands. Or, it may have a different constant entry for each corresponding band.
 * For all other border types, this <code>constants</code> parameter may be <code>null</code>.
 * 
 * <p>
 * The layout information for this image may be specified via the <code>layout</code> parameter. However, due to the nature of this operation, the
 * <code>minX</code>, <code>minY</code>, <code>width</code>, and <code>height</code>, if specified, will be ignored. They will be calculated based on
 * the source's dimensions and the padding values. Likewise, the <code>SampleModel</code> and </code>ColorModel</code> hints will be ignored.
 * 
 * If No Data are present, an optional No Data Range and a double value for the output No Data can be provided for avoiding to fill the Borders with
 * No Data.
 * 
 */
public class BorderOpImage extends OpImage {
    /**
     * The <code>BorderExtender</code> object used to extend the source data.
     */
    protected BorderExtender extender;

    /** No Data Range */
    private Range noData;

    /** Boolean indicating if the No Data are present */
    private final boolean hasNoData;

    /** Output No Data for Byte images */
    private byte destNoDataByte;

    /** Output No Data for Short/UShort images */
    private short destNoDataShort;

    /** Output No Data for Integer images */
    private int destNoDataInt;

    /** Output No Data for Float images */
    private float destNoDataFloat;

    /** Output No Data for Double images */
    private double destNoDataDouble;

    /** LookupTable used for handling No Data in Byte images */
    private byte[] byteLookupTable;

    /** Boolean indicating if the Borders must be checked for No Data */
    private final boolean checkBorders;

    /**
     * Constructor.
     * 
     * @param source The source image.
     * @param layout The destination image layout.
     * @param leftPad The amount of padding to the left of the source.
     * @param rightPad The amount of padding to the right of the source.
     * @param topPad The amount of padding to the top of the source.
     * @param bottomPad The amount of padding to the bottom of the source.
     * @param type The border type.
     * @param noData optional NoData Range.
     * @param destinationNoData value for replacing input No Data values
     */
    public BorderOpImage(RenderedImage source, Map config, ImageLayout layout, int leftPad,
            int rightPad, int topPad, int bottomPad, BorderExtender extender, Range noData,
            double destinationNoData) {
        super(vectorize(source),
                layoutHelper(layout, source, leftPad, rightPad, topPad, bottomPad), config, true);

        // Destination Image data Type
        int dataType = getSampleModel().getDataType();

        // If No Data are present
        if (noData != null) {
            this.noData = noData;
            // No Data are present, so associated flag is set to true
            this.hasNoData = true;
            // Destination No Data value is clamped to the image data type
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                // Destination NoData clamping
                this.destNoDataByte = ImageUtil.clampRoundByte(destinationNoData);
                // Creation of the Lookup Table for No Data
                byteLookupTable = new byte[256];

                int lookupTableLenght = byteLookupTable.length;

                for (int i = 0; i < lookupTableLenght; i++) {

                    byte value = (byte) i;

                    if (noData.contains(value)) {
                        byteLookupTable[i] = destNoDataByte;
                    } else {
                        byteLookupTable[i] = value;
                    }
                }
                break;
            case DataBuffer.TYPE_USHORT:
                // Destination NoData clamping
                this.destNoDataShort = ImageUtil.clampRoundUShort(destinationNoData);
                break;
            case DataBuffer.TYPE_SHORT:
                // Destination NoData clamping
                this.destNoDataShort = ImageUtil.clampRoundShort(destinationNoData);
                break;
            case DataBuffer.TYPE_INT:
                // Destination NoData clamping
                this.destNoDataInt = ImageUtil.clampRoundInt(destinationNoData);
                break;
            case DataBuffer.TYPE_FLOAT:
                // Destination NoData clamping
                this.destNoDataFloat = ImageUtil.clampFloat(destinationNoData);
                break;
            case DataBuffer.TYPE_DOUBLE:
                // Destination NoData clamping
                this.destNoDataDouble = destinationNoData;
                break;
            default:
                throw new IllegalArgumentException("Wrong image data type");
            }
        } else {
            this.noData = null;
            this.hasNoData = false;
        }
        // Border Extender used
        this.extender = extender;

        boolean notZeroExtender = !(extender instanceof BorderExtenderZero);
        // Check if the No Data check on the borders must be done
        checkBorders = hasNoData && notZeroExtender;

    }

    /**
     * Sets up the image layout information for this Operation. The minX, minY, width, and height are calculated based on the source's dimension and
     * padding values. Any of these values specified in the layout parameter is ignored. All other variables are taken from the layout parameter or
     * inherited from the source.
     */
    private static ImageLayout layoutHelper(ImageLayout layout, RenderedImage source, int leftPad,
            int rightPad, int topPad, int bottomPad) {
        ImageLayout il = layout == null ? new ImageLayout() : (ImageLayout) layout.clone();

        // Set the image bounds according to the padding.
        il.setMinX(source.getMinX() - leftPad);
        il.setMinY(source.getMinY() - topPad);
        il.setWidth(source.getWidth() + leftPad + rightPad);
        il.setHeight(source.getHeight() + topPad + bottomPad);

        // Set tile grid offset to minimize the probability that a
        // tile's bounds does not intersect the source image bounds.
        if (!il.isValid(ImageLayout.TILE_GRID_X_OFFSET_MASK)) {
            il.setTileGridXOffset(il.getMinX(null));
        }

        if (!il.isValid(ImageLayout.TILE_GRID_Y_OFFSET_MASK)) {
            il.setTileGridYOffset(il.getMinY(null));
        }

        // Force inheritance of source image SampleModel and ColorModel.
        il.setSampleModel(source.getSampleModel());
        il.setColorModel(source.getColorModel());

        return il;
    }

    /**
     * Returns an estimate of the destination region that can potentially be affected by the pixels of a rectangle of a given source.
     * 
     * @param sourceRect the Rectangle in source coordinates.
     * @param sourceIndex the index of the source image.
     * @return a Rectangle indicating the potentially affected destination region. or null if the region is unknown.
     * @throws IllegalArgumentException if the source index is negative or greater than that of the last source.
     * @throws IllegalArgumentException if sourceRect is null.
     */
    public Rectangle mapSourceRect(Rectangle sourceRect, int sourceIndex) {

        if (sourceRect == null) {
            throw new IllegalArgumentException("Source Rectangle Not Defined");
        }

        if (sourceIndex < 0 || sourceIndex >= getNumSources()) {
            throw new IllegalArgumentException("Source index Out Of Bounds");
        }

        return new Rectangle(sourceRect);
    }

    /**
     * Returns a conservative estimate of the region of a specified source that is required in order to compute the pixels of a given destination
     * rectangle.
     * 
     * @param destRect the Rectangle in destination coordinates.
     * @param sourceIndex the index of the source image.
     * @return a Rectangle indicating the required source region.
     * @throws IllegalArgumentException if the source index is negative or greater than that of the last source.
     * @throws IllegalArgumentException if destRect is null.
     */
    public Rectangle mapDestRect(Rectangle destRect, int sourceIndex) {

        if (destRect == null) {
            throw new IllegalArgumentException("Destination Rectangle Not Defined");
        }

        if (sourceIndex < 0 || sourceIndex >= getNumSources()) {
            throw new IllegalArgumentException("Source index Out Of Bounds");
        }

        Rectangle srcBounds = getSourceImage(0).getBounds();
        return destRect.intersection(srcBounds);
    }

    /** Computes the pixel values for the specified tile. */
    public Raster computeTile(int tileX, int tileY) {
        // Create a new Raster.
        WritableRaster dest = createTile(tileX, tileY);
        // Destination Raster data type
        int dataType = dest.getSampleModel().getDataType();

        // Source Image
        PlanarImage sourceImage = getSourceImage(0);

        // Source image bounds
        Rectangle imageBounds = sourceImage.getBounds();

        // Destination raster bounds
        Rectangle destRect = dest.getBounds();

        // If image bounds contains all the destination bounds
        if (imageBounds.contains(destRect)) {
            copyRasterData(dest, sourceImage);
        } else {
            // Get the intersection of the Raster and image bounds.
            Rectangle isect = imageBounds.intersection(destRect);

            if (!isect.isEmpty()) {
                // Copy image data into the dest Raster.
                WritableRaster isectRaster = dest.createWritableChild(isect.x, isect.y,
                        isect.width, isect.height, isect.x, isect.y, null);
                copyRasterData(isectRaster, sourceImage);
            }

            // Extend the Raster.
            extender.extend(dest, sourceImage);

            // Check if the borders contains noData
            if (checkBorders) {
                switch (dataType) {
                case DataBuffer.TYPE_BYTE:
                    fillNoDataByte(dest);
                    break;
                case DataBuffer.TYPE_USHORT:
                    fillNoDataUshort(dest);
                    break;
                case DataBuffer.TYPE_SHORT:
                    fillNoDataShort(dest);
                    break;
                case DataBuffer.TYPE_INT:
                    fillNoDataInt(dest);
                    break;
                case DataBuffer.TYPE_FLOAT:
                    fillNoDataFloat(dest);
                    break;
                case DataBuffer.TYPE_DOUBLE:
                    fillNoDataDouble(dest);
                    break;
                default:
                    throw new IllegalArgumentException("Wrong data type");
                }
            }
        }
        return dest;
    }

    /**
     * Method used copying the source image data inside the destination Raster
     * 
     * @param dest
     * @param sourceImage
     * @return
     */
    private WritableRaster copyRasterData(WritableRaster dest, PlanarImage sourceImage) {
        // Source and destination bounds
        Rectangle destRect = dest.getBounds();

        Rectangle imageBounds = sourceImage.getBounds();

        Rectangle region = destRect.intersection(imageBounds);

        if (region.isEmpty()) { // Raster is outside of image's boundary
            return dest;
        }
        // Data Type
        int dataType = dest.getSampleModel().getDataType();
        // Cycle on all the source tiles that intersect with the destination raster
        int startTileX = sourceImage.XToTileX(region.x);
        int startTileY = sourceImage.YToTileY(region.y);
        int endTileX = sourceImage.XToTileX(region.x + region.width - 1);
        int endTileY = sourceImage.YToTileY(region.y + region.height - 1);

        SampleModel[] sampleModels = { sourceImage.getSampleModel() };
        int tagID = RasterAccessor.findCompatibleTag(sampleModels, dest.getSampleModel());

        RasterFormatTag srcTag = new RasterFormatTag(sampleModels[0], tagID);
        RasterFormatTag dstTag = new RasterFormatTag(dest.getSampleModel(), tagID);

        for (int ty = startTileY; ty <= endTileY; ty++) {
            for (int tx = startTileX; tx <= endTileX; tx++) {
                Raster tile = sourceImage.getTile(tx, ty);

                Rectangle subRegion = region.intersection(tile.getBounds());
                // RasterAccessor associated with the input and output tiles
                RasterAccessor src = new RasterAccessor(tile, subRegion, srcTag,
                        sourceImage.getColorModel());
                RasterAccessor dst = new RasterAccessor(dest, subRegion, dstTag, null);
                // Elaboration of the RasterAccessors
                switch (dataType) {
                case DataBuffer.TYPE_BYTE:
                    byteLoop(src, dst);
                    break;
                case DataBuffer.TYPE_USHORT:
                    ushortLoop(src, dst);
                    break;
                case DataBuffer.TYPE_SHORT:
                    shortLoop(src, dst);
                    break;
                case DataBuffer.TYPE_INT:
                    intLoop(src, dst);
                    break;
                case DataBuffer.TYPE_FLOAT:
                    floatLoop(src, dst);
                    break;
                case DataBuffer.TYPE_DOUBLE:
                    doubleLoop(src, dst);
                    break;
                default:
                    throw new IllegalArgumentException("Wrong data type");
                }

                if (dst.isDataCopy()) {
                    dst.clampDataArrays();
                    dst.copyDataToRaster();
                }
            }
        }
        return dest;
    }

    private void byteLoop(RasterAccessor src, RasterAccessor dst) {
        // RasterAccessor definitions
        int srcPixelStride = src.getPixelStride();
        int srcLineStride = src.getScanlineStride();
        int[] srcBandOffsets = src.getBandOffsets();

        int dstPixelStride = dst.getPixelStride();
        int dstLineStride = dst.getScanlineStride();
        int[] dstBandOffsets = dst.getBandOffsets();

        int width = dst.getWidth();
        int height = dst.getHeight();
        int bands = dst.getNumBands();

        byte[][] bSrcData = src.getByteDataArrays();
        byte[][] bDstData = dst.getByteDataArrays();

        if (hasNoData) {
            // Cycle on the image bands
            for (int b = 0; b < bands; b++) {
                byte[] s = bSrcData[b];
                byte[] d = bDstData[b];

                int dstLineOffset = dstBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int y = 0; y < height; y++) {

                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    for (int x = 0; x < width; x++) {

                        int value = s[srcPixelOffset] & 0xFF;
                        // No Data check
                        d[dstPixelOffset] = byteLookupTable[value];

                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;

                    }
                    dstLineOffset += dstLineStride;
                    srcLineOffset += srcLineStride;
                }
            }
        } else {
            // Cycle on the image bands
            for (int b = 0; b < bands; b++) {
                byte[] s = bSrcData[b];
                byte[] d = bDstData[b];

                int dstLineOffset = dstBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int y = 0; y < height; y++) {

                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    for (int x = 0; x < width; x++) {

                        d[dstPixelOffset] = s[srcPixelOffset];

                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;

                    }
                    dstLineOffset += dstLineStride;
                    srcLineOffset += srcLineStride;
                }
            }
        }
    }

    private void ushortLoop(RasterAccessor src, RasterAccessor dst) {
        // RasterAccessor definitions
        int srcPixelStride = src.getPixelStride();
        int srcLineStride = src.getScanlineStride();
        int[] srcBandOffsets = src.getBandOffsets();

        int dstPixelStride = dst.getPixelStride();
        int dstLineStride = dst.getScanlineStride();
        int[] dstBandOffsets = dst.getBandOffsets();

        int width = dst.getWidth();
        int height = dst.getHeight();
        int bands = dst.getNumBands();

        short[][] bSrcData = src.getShortDataArrays();
        short[][] bDstData = dst.getShortDataArrays();

        if (hasNoData) {
            // Cycle on the image bands
            for (int b = 0; b < bands; b++) {
                short[] s = bSrcData[b];
                short[] d = bDstData[b];

                int dstLineOffset = dstBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int y = 0; y < height; y++) {

                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    for (int x = 0; x < width; x++) {

                        int value = s[srcPixelOffset] & 0xFFFF;

                        short valueShort = (short) value;
                        // No Data check
                        if (noData.contains(valueShort)) {
                            d[dstPixelOffset] = destNoDataShort;
                        } else {
                            d[dstPixelOffset] = valueShort;
                        }

                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;

                    }
                    dstLineOffset += dstLineStride;
                    srcLineOffset += srcLineStride;
                }
            }
        } else {
            // Cycle on the image bands
            for (int b = 0; b < bands; b++) {
                short[] s = bSrcData[b];
                short[] d = bDstData[b];

                int dstLineOffset = dstBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int y = 0; y < height; y++) {

                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    for (int x = 0; x < width; x++) {

                        d[dstPixelOffset] = s[srcPixelOffset];

                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;

                    }
                    dstLineOffset += dstLineStride;
                    srcLineOffset += srcLineStride;
                }
            }
        }
    }

    private void shortLoop(RasterAccessor src, RasterAccessor dst) {
        // RasterAccessor definitions
        int srcPixelStride = src.getPixelStride();
        int srcLineStride = src.getScanlineStride();
        int[] srcBandOffsets = src.getBandOffsets();

        int dstPixelStride = dst.getPixelStride();
        int dstLineStride = dst.getScanlineStride();
        int[] dstBandOffsets = dst.getBandOffsets();

        int width = dst.getWidth();
        int height = dst.getHeight();
        int bands = dst.getNumBands();

        short[][] bSrcData = src.getShortDataArrays();
        short[][] bDstData = dst.getShortDataArrays();

        if (hasNoData) {
            // Cycle on the image bands
            for (int b = 0; b < bands; b++) {
                short[] s = bSrcData[b];
                short[] d = bDstData[b];

                int dstLineOffset = dstBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int y = 0; y < height; y++) {

                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    for (int x = 0; x < width; x++) {

                        short value = s[srcPixelOffset];
                        // No Data check
                        if (noData.contains(value)) {
                            d[dstPixelOffset] = destNoDataShort;
                        } else {
                            d[dstPixelOffset] = value;
                        }

                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;

                    }
                    dstLineOffset += dstLineStride;
                    srcLineOffset += srcLineStride;
                }
            }
        } else {
            // Cycle on the image bands
            for (int b = 0; b < bands; b++) {
                short[] s = bSrcData[b];
                short[] d = bDstData[b];

                int dstLineOffset = dstBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int y = 0; y < height; y++) {

                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    for (int x = 0; x < width; x++) {

                        d[dstPixelOffset] = s[srcPixelOffset];

                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;

                    }
                    dstLineOffset += dstLineStride;
                    srcLineOffset += srcLineStride;
                }
            }
        }
    }

    private void intLoop(RasterAccessor src, RasterAccessor dst) {
        // RasterAccessor definitions
        int srcPixelStride = src.getPixelStride();
        int srcLineStride = src.getScanlineStride();
        int[] srcBandOffsets = src.getBandOffsets();

        int dstPixelStride = dst.getPixelStride();
        int dstLineStride = dst.getScanlineStride();
        int[] dstBandOffsets = dst.getBandOffsets();

        int width = dst.getWidth();
        int height = dst.getHeight();
        int bands = dst.getNumBands();

        int[][] bSrcData = src.getIntDataArrays();
        int[][] bDstData = dst.getIntDataArrays();

        if (hasNoData) {
            // Cycle on the image bands
            for (int b = 0; b < bands; b++) {
                int[] s = bSrcData[b];
                int[] d = bDstData[b];

                int dstLineOffset = dstBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int y = 0; y < height; y++) {

                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    for (int x = 0; x < width; x++) {

                        int value = s[srcPixelOffset];
                        // No Data check
                        if (noData.contains(value)) {
                            d[dstPixelOffset] = destNoDataInt;
                        } else {
                            d[dstPixelOffset] = value;
                        }

                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;

                    }
                    dstLineOffset += dstLineStride;
                    srcLineOffset += srcLineStride;
                }
            }
        } else {
            // Cycle on the image bands
            for (int b = 0; b < bands; b++) {
                int[] s = bSrcData[b];
                int[] d = bDstData[b];

                int dstLineOffset = dstBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int y = 0; y < height; y++) {

                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    for (int x = 0; x < width; x++) {

                        d[dstPixelOffset] = s[srcPixelOffset];

                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;

                    }
                    dstLineOffset += dstLineStride;
                    srcLineOffset += srcLineStride;
                }
            }
        }
    }

    private void floatLoop(RasterAccessor src, RasterAccessor dst) {
        // RasterAccessor definitions
        int srcPixelStride = src.getPixelStride();
        int srcLineStride = src.getScanlineStride();
        int[] srcBandOffsets = src.getBandOffsets();

        int dstPixelStride = dst.getPixelStride();
        int dstLineStride = dst.getScanlineStride();
        int[] dstBandOffsets = dst.getBandOffsets();

        int width = dst.getWidth();
        int height = dst.getHeight();
        int bands = dst.getNumBands();

        float[][] bSrcData = src.getFloatDataArrays();
        float[][] bDstData = dst.getFloatDataArrays();

        if (hasNoData) {
            // Cycle on the image bands
            for (int b = 0; b < bands; b++) {
                float[] s = bSrcData[b];
                float[] d = bDstData[b];

                int dstLineOffset = dstBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int y = 0; y < height; y++) {

                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    for (int x = 0; x < width; x++) {

                        float value = s[srcPixelOffset];
                        // No Data check
                        if (noData.contains(value)) {
                            d[dstPixelOffset] = destNoDataFloat;
                        } else {
                            d[dstPixelOffset] = value;
                        }

                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;

                    }
                    dstLineOffset += dstLineStride;
                    srcLineOffset += srcLineStride;
                }
            }
        } else {
            // Cycle on the image bands
            for (int b = 0; b < bands; b++) {
                float[] s = bSrcData[b];
                float[] d = bDstData[b];

                int dstLineOffset = dstBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int y = 0; y < height; y++) {

                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    for (int x = 0; x < width; x++) {

                        d[dstPixelOffset] = s[srcPixelOffset];

                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;

                    }
                    dstLineOffset += dstLineStride;
                    srcLineOffset += srcLineStride;
                }
            }
        }
    }

    private void doubleLoop(RasterAccessor src, RasterAccessor dst) {
        // RasterAccessor definitions
        int srcPixelStride = src.getPixelStride();
        int srcLineStride = src.getScanlineStride();
        int[] srcBandOffsets = src.getBandOffsets();

        int dstPixelStride = dst.getPixelStride();
        int dstLineStride = dst.getScanlineStride();
        int[] dstBandOffsets = dst.getBandOffsets();

        int width = dst.getWidth();
        int height = dst.getHeight();
        int bands = dst.getNumBands();

        double[][] bSrcData = src.getDoubleDataArrays();
        double[][] bDstData = dst.getDoubleDataArrays();

        if (hasNoData) {
            // Cycle on the image bands
            for (int b = 0; b < bands; b++) {
                double[] s = bSrcData[b];
                double[] d = bDstData[b];

                int dstLineOffset = dstBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int y = 0; y < height; y++) {

                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    for (int x = 0; x < width; x++) {

                        double value = s[srcPixelOffset];
                        // No Data check
                        if (noData.contains(value)) {
                            d[dstPixelOffset] = destNoDataDouble;
                        } else {
                            d[dstPixelOffset] = value;
                        }

                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;

                    }
                    dstLineOffset += dstLineStride;
                    srcLineOffset += srcLineStride;
                }
            }
        } else {
            // Cycle on the image bands
            for (int b = 0; b < bands; b++) {
                double[] s = bSrcData[b];
                double[] d = bDstData[b];

                int dstLineOffset = dstBandOffsets[b];
                int srcLineOffset = srcBandOffsets[b];

                for (int y = 0; y < height; y++) {

                    int dstPixelOffset = dstLineOffset;
                    int srcPixelOffset = srcLineOffset;

                    for (int x = 0; x < width; x++) {

                        d[dstPixelOffset] = s[srcPixelOffset];

                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;

                    }
                    dstLineOffset += dstLineStride;
                    srcLineOffset += srcLineStride;
                }
            }
        }
    }

    /**
     * Inner method used for checking if the Borders contains No Data and substituting them. (Byte images).
     * 
     * @param dest
     */
    private void fillNoDataByte(WritableRaster dest) {

        Rectangle srcBounds = getSourceImage(0).getBounds();

        Rectangle destBounds = dest.getBounds();
        // The operation is performed only on the image borders
        if (!srcBounds.contains(destBounds) && !(destBounds.intersection(srcBounds).isEmpty())) {

            int minX = dest.getMinX();
            int minY = dest.getMinY();

            int maxX = minX + dest.getWidth();
            int maxY = minY + dest.getHeight();

            int numBands = dest.getNumBands();

            for (int i = minX; i < maxX; i++) {
                for (int j = minY; j < maxY; j++) {

                    for (int b = 0; b < numBands; b++) {

                        int sample = dest.getSample(i, j, b) & 0xFF;
                        // No Data check
                        dest.setSample(i, j, b, byteLookupTable[sample]);
                    }
                }
            }
        }
    }

    /**
     * Inner method used for checking if the Borders contains No Data and substituting them. (UShort images).
     * 
     * @param dest
     */
    private void fillNoDataUshort(WritableRaster dest) {

        Rectangle srcBounds = getSourceImage(0).getBounds();

        Rectangle destBounds = dest.getBounds();
        // The operation is performed only on the image borders
        if (!srcBounds.contains(destBounds) && !(destBounds.intersection(srcBounds).isEmpty())) {

            int minX = dest.getMinX();
            int minY = dest.getMinY();

            int maxX = minX + dest.getWidth();
            int maxY = minY + dest.getHeight();

            int numBands = dest.getNumBands();

            for (int i = minX; i < maxX; i++) {
                for (int j = minY; j < maxY; j++) {

                    for (int b = 0; b < numBands; b++) {

                        int sample = dest.getSample(i, j, b) & 0xFFFF;

                        short sampleShort = (short) sample;
                        // No Data check
                        if (noData.contains(sampleShort)) {
                            dest.setSample(i, j, b, destNoDataShort);
                        } else {
                            dest.setSample(i, j, b, sampleShort);
                        }
                    }
                }
            }
        }
    }

    /**
     * Inner method used for checking if the Borders contains No Data and substituting them. (Short images).
     * 
     * @param dest
     */
    private void fillNoDataShort(WritableRaster dest) {

        Rectangle srcBounds = getSourceImage(0).getBounds();

        Rectangle destBounds = dest.getBounds();
        // The operation is performed only on the image borders
        if (!srcBounds.contains(destBounds) && !(destBounds.intersection(srcBounds).isEmpty())) {

            int minX = dest.getMinX();
            int minY = dest.getMinY();

            int maxX = minX + dest.getWidth();
            int maxY = minY + dest.getHeight();

            int numBands = dest.getNumBands();

            for (int i = minX; i < maxX; i++) {
                for (int j = minY; j < maxY; j++) {

                    for (int b = 0; b < numBands; b++) {

                        short sample = (short) dest.getSample(i, j, b);
                        // No Data check
                        if (noData.contains(sample)) {
                            dest.setSample(i, j, b, destNoDataShort);
                        } else {
                            dest.setSample(i, j, b, sample);
                        }
                    }
                }
            }
        }
    }

    /**
     * Inner method used for checking if the Borders contains No Data and substituting them. (Integer images).
     * 
     * @param dest
     */
    private void fillNoDataInt(WritableRaster dest) {

        Rectangle srcBounds = getSourceImage(0).getBounds();

        Rectangle destBounds = dest.getBounds();
        // The operation is performed only on the image borders
        if (!srcBounds.contains(destBounds) && !(destBounds.intersection(srcBounds).isEmpty())) {

            int minX = dest.getMinX();
            int minY = dest.getMinY();

            int maxX = minX + dest.getWidth();
            int maxY = minY + dest.getHeight();

            int numBands = dest.getNumBands();

            for (int i = minX; i < maxX; i++) {
                for (int j = minY; j < maxY; j++) {

                    for (int b = 0; b < numBands; b++) {

                        int sample = dest.getSample(i, j, b);
                        // No Data check
                        if (noData.contains(sample)) {
                            dest.setSample(i, j, b, destNoDataInt);
                        } else {
                            dest.setSample(i, j, b, sample);
                        }
                    }
                }
            }
        }
    }

    /**
     * Inner method used for checking if the Borders contains No Data and substituting them. (Float images).
     * 
     * @param dest
     */
    private void fillNoDataFloat(WritableRaster dest) {

        Rectangle srcBounds = getSourceImage(0).getBounds();

        Rectangle destBounds = dest.getBounds();
        // The operation is performed only on the image borders
        if (!srcBounds.contains(destBounds) && !(destBounds.intersection(srcBounds).isEmpty())) {

            int minX = dest.getMinX();
            int minY = dest.getMinY();

            int maxX = minX + dest.getWidth();
            int maxY = minY + dest.getHeight();

            int numBands = dest.getNumBands();

            for (int i = minX; i < maxX; i++) {
                for (int j = minY; j < maxY; j++) {

                    for (int b = 0; b < numBands; b++) {

                        float sample = dest.getSampleFloat(i, j, b);
                        // No Data check
                        if (noData.contains(sample)) {
                            dest.setSample(i, j, b, destNoDataFloat);
                        } else {
                            dest.setSample(i, j, b, sample);
                        }
                    }
                }
            }
        }
    }

    /**
     * Inner method used for checking if the Borders contains No Data and substituting them. (Double images).
     * 
     * @param dest
     */
    private void fillNoDataDouble(WritableRaster dest) {

        Rectangle srcBounds = getSourceImage(0).getBounds();

        Rectangle destBounds = dest.getBounds();
        // The operation is performed only on the image borders
        if (!srcBounds.contains(destBounds) && !(destBounds.intersection(srcBounds).isEmpty())) {

            int minX = dest.getMinX();
            int minY = dest.getMinY();

            int maxX = minX + dest.getWidth();
            int maxY = minY + dest.getHeight();

            int numBands = dest.getNumBands();

            for (int i = minX; i < maxX; i++) {
                for (int j = minY; j < maxY; j++) {

                    for (int b = 0; b < numBands; b++) {

                        double sample = dest.getSampleDouble(i, j, b);
                        // No Data check
                        if (noData.contains(sample)) {
                            dest.setSample(i, j, b, destNoDataDouble);
                        } else {
                            dest.setSample(i, j, b, sample);
                        }
                    }
                }
            }
        }
    }
}
