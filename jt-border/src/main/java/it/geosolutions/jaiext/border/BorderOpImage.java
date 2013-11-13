/*
 * $RCSfile: BorderOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:16 $
 * $State: Exp $
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
 * be filled in a variety of ways specified by the border type parameter, defined in <code>javax.media.jai.operator.BorderDescriptor</code>:
 * <ul>
 * <li>it may be extended with zeros (BORDER_ZERO_FILL);
 * <li>it may be extended with a constant set of values (BORDER_CONST_FILL);
 * <li) it may be created by copying the edge and corner pixels (BORDER_EXTEND);
 * <li>it may be created by reflection about the edges of the image (BORDER_REFLECT); or,
 * <li>it may be extended by "wrapping" the image plane toroidally, that is, joining opposite edges of the image.
 * </ul>
 * 
 * <p>
 * When choosing the <code>BORDER_CONST_FILL</code> option, an array of constants must be supplied. The array must have at least one element, in which
 * case this same constant is applied to all image bands. Or, it may have a different constant entry for each cooresponding band. For all other border
 * types, this <code>constants</code> parameter may be <code>null</code>.
 * 
 * <p>
 * The layout information for this image may be specified via the <code>layout</code> parameter. However, due to the nature of this operation, the
 * <code>minX</code>, <code>minY</code>, <code>width</code>, and <code>height</code>, if specified, will be ignored. They will be calculated based on
 * the source's dimensions and the padding values. Likewise, the <code>SampleModel</code> and </code>ColorModel</code> hints will be ignored.
 * 
 * @see javax.media.jai.OpImage
 * @see javax.media.jai.operator.BorderDescriptor
 * @see BorderRIF
 * 
 */
final class BorderOpImage extends OpImage {
    /**
     * The <code>BorderExtender</code> object used to extend the source data.
     */
    protected BorderExtender extender;

    private Range noData;

    private final boolean hasNoData;

    private byte destNoDataByte;

    private short destNoDataShort;

    private int destNoDataInt;

    private float destNoDataFloat;

    private double destNoDataDouble;

    private byte[] byteLookupTable;

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
     * @param constants The constants used with border type <code>BorderDescriptor.BORDER_CONST_FILL</code>, stored as reference.
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
            // Else the whole array is used
            this.noData = noData;
            // No Data are present, so associated flaw is set to true
            this.hasNoData = true;
            // Destination No Data value is clamped to the image data type
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                this.destNoDataByte = ImageUtil.clampRoundByte(destinationNoData);

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
                this.destNoDataShort = ImageUtil.clampRoundUShort(destinationNoData);
                break;
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
        } else {
            this.noData = null;
            this.hasNoData = false;
        }

        this.extender = extender;
        
        boolean notZeroExtender = !(extender instanceof BorderExtenderZero);
        
        checkBorders = hasNoData && notZeroExtender;
        
    }

    /**
     * Sets up the image layout information for this Op. The minX, minY, width, and height are calculated based on the source's dimension and padding
     * values. Any of these values specified in the layout parameter is ignored. All other variables are taken from the layout parameter or inherited
     * from the source.
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
     * Returns a conservative estimate of the destination region that can potentially be affected by the pixels of a rectangle of a given source.
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

        // Extend the data.
        // getSourceImage(0).copyExtendedData(dest, extender);
        PlanarImage sourceImage = getSourceImage(0);

        Raster sourceTile = getSourceImage(0).getTile(tileX, tileY);

        Rectangle destRect = dest.getBounds();

        Rectangle sourceRect = mapDestRect(destRect, 0);

        SampleModel[] sampleModels = { sourceImage.getSampleModel() };

        int tagID = RasterAccessor.findCompatibleTag(sampleModels, dest.getSampleModel());

        RasterFormatTag srcTag = new RasterFormatTag(sampleModels[0], tagID);
        RasterFormatTag dstTag = new RasterFormatTag(dest.getSampleModel(), tagID);

        RasterAccessor src = new RasterAccessor(sourceTile, sourceRect, srcTag,
                sourceImage.getColorModel());
        RasterAccessor dst = new RasterAccessor(dest, sourceRect, dstTag, null);

        int dataType = dest.getSampleModel().getDataType();

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(src, dst);

            if (dst.isDataCopy()) {
                dst.clampDataArrays();
                dst.copyDataToRaster();
            }

            // Extend the Raster.
            extender.extend(dest, sourceImage);

            // Check if the borders contains noData
            if (checkBorders) {
                fillNoDataByte(dest);
            }
            break;
        case DataBuffer.TYPE_USHORT:
            ushortLoop(src, dst);
            
         // Extend the Raster.
            extender.extend(dest, sourceImage);

            // Check if the borders contains noData
            if (checkBorders) {
                fillNoDataUshort(dest);
            }
            break;
        case DataBuffer.TYPE_SHORT:
            shortLoop(src, dst);
         // Extend the Raster.
            extender.extend(dest, sourceImage);

            // Check if the borders contains noData
            if (checkBorders) {
                fillNoDataShort(dest);
            }
            break;
        case DataBuffer.TYPE_INT:
            intLoop(src, dst);
         // Extend the Raster.
            extender.extend(dest, sourceImage);

            // Check if the borders contains noData
            if (checkBorders) {
                fillNoDataInt(dest);
            }
            break;
        case DataBuffer.TYPE_FLOAT:
            floatLoop(src, dst);
         // Extend the Raster.
            extender.extend(dest, sourceImage);

            // Check if the borders contains noData
            if (checkBorders) {
                fillNoDataFloat(dest);
            }
            break;
        case DataBuffer.TYPE_DOUBLE:
            doubleLoop(src, dst);
         // Extend the Raster.
            extender.extend(dest, sourceImage);

            // Check if the borders contains noData
            if (checkBorders) {
                fillNoDataDouble(dest);
            }
            break;
        default:
            throw new IllegalArgumentException("Wrong data type");

        }

        return dest;
    }

    private void byteLoop(RasterAccessor src, RasterAccessor dst) {

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

                        d[dstPixelOffset] = byteLookupTable[value];

                        dstPixelOffset += dstPixelStride;
                        srcPixelOffset += srcPixelStride;

                    }
                    dstLineOffset += dstLineStride;
                    srcLineOffset += srcLineStride;
                }
            }
        } else {
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

    private void fillNoDataByte(WritableRaster dest) {

        Rectangle srcBounds = getSourceImage(0).getBounds();

        Rectangle destBounds = dest.getBounds();

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

                        dest.setSample(i, j, b, byteLookupTable[sample]);
                    }
                }
            }
        }
    }

    private void fillNoDataUshort(WritableRaster dest) {

        Rectangle srcBounds = getSourceImage(0).getBounds();

        Rectangle destBounds = dest.getBounds();

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
                        
                        if(noData.contains(sampleShort)){ 
                            dest.setSample(i, j, b, destNoDataShort);
                        }else{
                            dest.setSample(i, j, b, sampleShort);
                        }
                    }
                }
            }
        }
    }

    private void fillNoDataShort(WritableRaster dest) {

        Rectangle srcBounds = getSourceImage(0).getBounds();

        Rectangle destBounds = dest.getBounds();

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
                        
                        if(noData.contains(sample)){ 
                            dest.setSample(i, j, b, destNoDataShort);
                        }else{
                            dest.setSample(i, j, b, sample);
                        }
                    }
                }
            }
        }
    }

    private void fillNoDataInt(WritableRaster dest) {

        Rectangle srcBounds = getSourceImage(0).getBounds();

        Rectangle destBounds = dest.getBounds();

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
                        
                        if(noData.contains(sample)){ 
                            dest.setSample(i, j, b, destNoDataInt);
                        }else{
                            dest.setSample(i, j, b, sample);
                        }
                    }
                }
            }
        }
    }

    private void fillNoDataFloat(WritableRaster dest) {
        
        Rectangle srcBounds = getSourceImage(0).getBounds();

        Rectangle destBounds = dest.getBounds();

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
                        
                        if(noData.contains(sample)){ 
                            dest.setSample(i, j, b, destNoDataFloat);
                        }else{
                            dest.setSample(i, j, b, sample);
                        }
                    }
                }
            }
        }
    }

    private void fillNoDataDouble(WritableRaster dest) {



        Rectangle srcBounds = getSourceImage(0).getBounds();

        Rectangle destBounds = dest.getBounds();

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
                        
                        if(noData.contains(sample)){ 
                            dest.setSample(i, j, b, destNoDataDouble);
                        }else{
                            dest.setSample(i, j, b, sample);
                        }
                    }
                }
            }
        }
    }
}
