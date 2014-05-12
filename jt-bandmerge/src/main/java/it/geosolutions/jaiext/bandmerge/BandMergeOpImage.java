package it.geosolutions.jaiext.bandmerge;

import it.geosolutions.jaiext.range.Range;

import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.List;
import java.util.Vector;
import javax.media.jai.ImageLayout;
import javax.media.jai.PointOpImage;
import java.util.Map;
import javax.media.jai.PixelAccessor;
import javax.media.jai.UnpackedImageData;
import javax.media.jai.RasterFactory;

import com.sun.media.jai.util.ImageUtil;
import com.sun.media.jai.util.JDKWorkarounds;

/**
 * An <code>OpImage</code> implementing the "BandMerge" operation as described in {@link BandMergeDescriptor}.
 * 
 * <p>
 * This <code>OpImage</code> merges the pixel values of two or more source images.
 * 
 * The data type <code>byte</code> is treated as unsigned, with maximum value as 255 and minimum value as 0.
 * 
 * There is no attempt to rescale binary images to the approapriate gray levels, such as 255 or 0. A lookup should be performed first if so desired.
 * 
 * If No Data are present, they can be handled if the user provides an array of No Data Range objects and a double value for the destination No Data.
 * 
 */
class BandMergeOpImage extends PointOpImage {

    /** List of ColorModels required for IndexColorModel support */
    ColorModel[] colorModels;

    /** Array containing all the No Data Ranges */
    private final Range[] noData;

    /** Boolean indicating if No Data are present */
    private final boolean hasNoData;

    /** Destination No Data value used for Byte images */
    private byte destNoDataByte;

    /** Destination No Data value used for Short/Unsigned Short images */
    private short destNoDataShort;

    /** Destination No Data value used for Integer images */
    private int destNoDataInt;

    /** Destination No Data value used for Float images */
    private float destNoDataFloat;

    /** Destination No Data value used for Double images */
    private double destNoDataDouble;

    /**
     * Constructs a <code>BandMergeOpImage</code>.
     * 
     * <p>
     * The <code>layout</code> parameter may optionally contain the tile grid layout, sample model, and/or color model. The image dimension is
     * determined by the intersection of the bounding boxes of the source images.
     * 
     * <p>
     * The image layout of the first source image, <code>source1</code>, is used as the fallback for the image layout of the destination image. The
     * destination number of bands is the sum of all source image bands.
     * 
     * @param sources <code>List</code> of sources.
     * @param config Configurable attributes of the image including configuration variables indexed by <code>RenderingHints.Key</code>s and image
     *        properties indexed by <code>String</code>s or <code>CaselessStringKey</code>s. This is simply forwarded to the superclass constructor.
     * @param noData Array of No Data Range.
     * @param destinationNoData output value for No Data.
     * @param layout The destination image layout.
     */
    public BandMergeOpImage(List sources, Map config, Range[] noData, double destinationNoData,
            ImageLayout layout) {

        super(vectorize(sources), layoutHelper(sources, layout), config, true);

        // Set flag to permit in-place operation.
        permitInPlaceOperation();

        // get ColorModels for IndexColorModel support
        int numSrcs = sources.size();
        colorModels = new ColorModel[numSrcs];

        for (int i = 0; i < numSrcs; i++) {
            colorModels[i] = ((RenderedImage) sources.get(i)).getColorModel();
        }
        // Destination Image data Type
        int dataType = getSampleModel().getDataType();

        // If No Data are present
        if (noData != null) {
            // If the length of the array is different from that of the sources
            // the first Range is used for all the images
            if (noData.length != numSrcs) {
                Range firstNoData = noData[0];

                this.noData = new Range[numSrcs];

                for (int i = 0; i < numSrcs; i++) {
                    this.noData[i] = firstNoData;
                }
            } else {
                // Else the whole array is used
                this.noData = noData;
            }
            // No Data are present, so associated flaw is set to true
            this.hasNoData = true;
            // Destination No Data value is clamped to the image data type
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                this.destNoDataByte = ImageUtil.clampRoundByte(destinationNoData);
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
    }

    /**
     * This method takes in input the list of all the sources and calculates the total number of bands of the destination image.
     * 
     * @param sources List of the source images
     * @return the total number of the destination bands
     */
    private static int totalNumBands(List sources) {
        // Initialization
        int total = 0;

        // Cycle on all the sources
        for (int i = 0; i < sources.size(); i++) {
            RenderedImage image = (RenderedImage) sources.get(i);

            // If the source ColorModel is IndexColorModel, then the bands are defined by its components
            if (image.getColorModel() instanceof IndexColorModel) {
                total += image.getColorModel().getNumComponents();
                // Else the bands are defined from the SampleModel
            } else {
                total += image.getSampleModel().getNumBands();
            }
        }
        // Total bands number
        return total;
    }

    private static ImageLayout layoutHelper(List sources, ImageLayout il) {

        // If the layout is not defined, a new one is created, else is cloned
        ImageLayout layout = (il == null) ? new ImageLayout() : (ImageLayout) il.clone();
        // Number of input sources
        int numSources = sources.size();

        // dest data type is the maximum of transfertype of source image
        // utilizing the monotonicity of data types.

        // dest number of bands = sum of source bands
        int destNumBands = totalNumBands(sources);

        int destDataType = DataBuffer.TYPE_BYTE; // initialize
        RenderedImage srci = (RenderedImage) sources.get(0);
        // Destination Bounds are taken from the first image
        Rectangle destBounds = new Rectangle(srci.getMinX(), srci.getMinY(), srci.getWidth(),
                srci.getHeight());
        // Cycle on all the images
        for (int i = 0; i < numSources; i++) {
            // Selection of a source
            srci = (RenderedImage) sources.get(i);
            // Intersection of the initial bounds with the source bounds
            destBounds = destBounds.intersection(new Rectangle(srci.getMinX(), srci.getMinY(), srci
                    .getWidth(), srci.getHeight()));
            // Selection of the source TransferType
            int typei = srci.getSampleModel().getTransferType();

            // NOTE: this depends on JDK ordering
            destDataType = typei > destDataType ? typei : destDataType;
        }

        // Definition of the Layout
        layout.setMinX(destBounds.x);
        layout.setMinY(destBounds.y);
        layout.setWidth(destBounds.width);
        layout.setHeight(destBounds.height);
        
        // First image sampleModel
        SampleModel sm = layout.getSampleModel((RenderedImage) sources.get(0));

        // Creation of a new SampleModel with the new settings
        if (sm.getNumBands() < destNumBands) {
            int[] destOffsets = new int[destNumBands];

            for (int i = 0; i < destNumBands; i++) {
                destOffsets[i] = i;
            }

            // determine the proper width and height to use
            int destTileWidth = sm.getWidth();
            int destTileHeight = sm.getHeight();
            if (layout.isValid(ImageLayout.TILE_WIDTH_MASK)) {
                destTileWidth = layout.getTileWidth((RenderedImage) sources.get(0));
            }
            if (layout.isValid(ImageLayout.TILE_HEIGHT_MASK)) {
                destTileHeight = layout.getTileHeight((RenderedImage) sources.get(0));
            }

            sm = RasterFactory.createComponentSampleModel(sm, destDataType, destTileWidth,
                    destTileHeight, destNumBands);

            layout.setSampleModel(sm);
        }

        // Selection of a colormodel associated with the layout
        ColorModel cm = layout.getColorModel(null);

        if (cm != null && !JDKWorkarounds.areCompatibleDataModels(sm, cm)) {
            // Clear the mask bit if incompatible.
            layout.unsetValid(ImageLayout.COLOR_MODEL_MASK);
        }

        return layout;
    }

    /**
     * BandMerges the pixel values of multiple source images within a specified rectangle.
     * 
     * @param sources Cobbled sources, guaranteed to provide all the source data necessary for computing the rectangle.
     * @param dest The tile containing the rectangle to be computed.
     * @param destRect The rectangle within the tile to be computed.
     */
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        // Destination data type
        int destType = dest.getTransferType();
        // Loop on the image raster
        switch (destType) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(sources, dest, destRect);
            break;
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT:
            shortLoop(sources, dest, destRect);
            break;
        case DataBuffer.TYPE_INT:
            intLoop(sources, dest, destRect);
            break;
        case DataBuffer.TYPE_FLOAT:
            floatLoop(sources, dest, destRect);
            break;
        case DataBuffer.TYPE_DOUBLE:
            doubleLoop(sources, dest, destRect);
            break;
        default:
            throw new RuntimeException("Wrong image data type");
        }
    }

    private void byteLoop(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        // Source number
        int nSrcs = sources.length;
        // Bands associated with each sources
        int[] snbands = new int[nSrcs];
        // PixelAccessor array for each source
        PixelAccessor[] pas = new PixelAccessor[nSrcs];
        for (int i = 0; i < nSrcs; i++) {
            pas[i] = new PixelAccessor(sources[i].getSampleModel(), colorModels[i]);

            if (colorModels[i] instanceof IndexColorModel) {
                snbands[i] = colorModels[i].getNumComponents();
            } else {
                snbands[i] = sources[i].getNumBands();
            }
        }

        // Destination bands
        int dnbands = dest.getNumBands();
        // Destination data type
        int destType = dest.getTransferType();
        // PixelAccessor associated with the destination raster
        PixelAccessor d = new PixelAccessor(dest.getSampleModel(), null);

        UnpackedImageData dimd = d.getPixels(dest, destRect, destType, true);

        // Destination data values
        byte[][] dstdata = (byte[][]) dimd.data;

        // NO DATA PRESENT
        if (hasNoData) {
            // Cycle on all the sources
            for (int sindex = 0, db = 0; sindex < nSrcs; sindex++) {

                // Source data
                UnpackedImageData simd = colorModels[sindex] instanceof IndexColorModel ? pas[sindex]
                        .getComponents(sources[sindex], destRect, sources[sindex].getSampleModel()
                                .getTransferType()) : pas[sindex].getPixels(sources[sindex],
                        destRect, sources[sindex].getSampleModel().getTransferType(), false);

                // Source and Destination parameters
                int srcPixelStride = simd.pixelStride;
                int srcLineStride = simd.lineStride;
                int dstPixelStride = dimd.pixelStride;
                int dstLineStride = dimd.lineStride;
                int dRectWidth = destRect.width;

                // Cycle on each source bands
                for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                    if (db >= dnbands) {
                        // exceeding destNumBands; should not have happened
                        break;
                    }

                    // Source and destination data array
                    byte[] dstdatabandb = dstdata[db];
                    byte[][] srcdata = (byte[][]) simd.data;
                    byte[] srcdatabandsb = srcdata[sb];
                    int srcstart = simd.bandOffsets[sb];
                    int dststart = dimd.bandOffsets[db];
                    // Cycle on the y-axis
                    for (int y = 0; y < destRect.height; y++, srcstart += srcLineStride, dststart += dstLineStride) {
                        // Cycle on the x-axis
                        for (int i = 0, srcpos = srcstart, dstpos = dststart; i < dRectWidth; i++, srcpos += srcPixelStride, dstpos += dstPixelStride) {
                            // No Data control
                            if (noData[sindex].contains(srcdatabandsb[srcpos])) {
                                dstdatabandb[dstpos] = destNoDataByte;
                            } else {
                                dstdatabandb[dstpos] = srcdatabandsb[srcpos];
                            }
                        }
                    }
                }
            }
            // NO DATA NOT PRESENT
        } else {
            // Cycle on all the sources
            for (int sindex = 0, db = 0; sindex < nSrcs; sindex++) {

                UnpackedImageData simd = colorModels[sindex] instanceof IndexColorModel ? pas[sindex]
                        .getComponents(sources[sindex], destRect, sources[sindex].getSampleModel()
                                .getTransferType()) : pas[sindex].getPixels(sources[sindex],
                        destRect, sources[sindex].getSampleModel().getTransferType(), false);

                int srcPixelStride = simd.pixelStride;
                int srcLineStride = simd.lineStride;
                int dstPixelStride = dimd.pixelStride;
                int dstLineStride = dimd.lineStride;
                int dRectWidth = destRect.width;
                // Cycle on each source bands
                for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                    if (db >= dnbands) {
                        // exceeding destNumBands; should not have happened
                        break;
                    }

                    byte[] dstdatabandb = dstdata[db];
                    byte[][] srcdata = (byte[][]) simd.data;
                    byte[] srcdatabandsb = srcdata[sb];
                    int srcstart = simd.bandOffsets[sb];
                    int dststart = dimd.bandOffsets[db];

                    // Cycle on the y-axis
                    for (int y = 0; y < destRect.height; y++, srcstart += srcLineStride, dststart += dstLineStride) {
                        // Cycle on the x-axis
                        for (int i = 0, srcpos = srcstart, dstpos = dststart; i < dRectWidth; i++, srcpos += srcPixelStride, dstpos += dstPixelStride) {

                            dstdatabandb[dstpos] = srcdatabandsb[srcpos];
                        }
                    }
                }
            }
        }

        d.setPixels(dimd);
    }

    private void shortLoop(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        // Source number
        int nSrcs = sources.length;
        // Bands associated with each sources
        int[] snbands = new int[nSrcs];
        // PixelAccessor array for each source
        PixelAccessor[] pas = new PixelAccessor[nSrcs];

        boolean isUshort = getSampleModel().getDataType() == DataBuffer.TYPE_USHORT;

        for (int i = 0; i < nSrcs; i++) {
            pas[i] = new PixelAccessor(sources[i].getSampleModel(), colorModels[i]);

            if (colorModels[i] instanceof IndexColorModel) {
                snbands[i] = colorModels[i].getNumComponents();
            } else {
                snbands[i] = sources[i].getNumBands();
            }
        }

        // Destination bands
        int dnbands = dest.getNumBands();
        // Destination data type
        int destType = dest.getTransferType();
        // PixelAccessor associated with the destination raster
        PixelAccessor d = new PixelAccessor(dest.getSampleModel(), null);

        UnpackedImageData dimd = d.getPixels(dest, destRect, destType, true);

        // Destination data values
        short[][] dstdata = (short[][]) dimd.data;

        // NO DATA PRESENT
        if (hasNoData) {
            // Cycle on all the sources
            for (int sindex = 0, db = 0; sindex < nSrcs; sindex++) {

                UnpackedImageData simd = colorModels[sindex] instanceof IndexColorModel ? pas[sindex]
                        .getComponents(sources[sindex], destRect, sources[sindex].getSampleModel()
                                .getTransferType()) : pas[sindex].getPixels(sources[sindex],
                        destRect, sources[sindex].getSampleModel().getTransferType(), false);

                int srcPixelStride = simd.pixelStride;
                int srcLineStride = simd.lineStride;
                int dstPixelStride = dimd.pixelStride;
                int dstLineStride = dimd.lineStride;
                int dRectWidth = destRect.width;
                // Cycle on each source bands
                for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                    if (db < dnbands) {
                        short[][] srcdata = (short[][]) simd.data;
                        int srcstart = simd.bandOffsets[sb];
                        int dststart = dimd.bandOffsets[db];
                        // Cycle on the y-axis
                        for (int y = 0; y < destRect.height; y++, srcstart += srcLineStride, dststart += dstLineStride) {
                            // Cycle on the x-axis
                            for (int i = 0, srcpos = srcstart, dstpos = dststart; i < dRectWidth; i++, srcpos += srcPixelStride, dstpos += dstPixelStride) {
                                // No Data control
                                if (noData[sindex].contains(srcdata[sb][srcpos])) {
                                    if (isUshort) {
                                        dstdata[db][dstpos] = destNoDataShort;
                                    } else {
                                        dstdata[db][dstpos] = destNoDataShort;
                                    }
                                } else {
                                    dstdata[db][dstpos] = srcdata[sb][srcpos];
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Cycle on all the sources
            for (int sindex = 0, db = 0; sindex < nSrcs; sindex++) {

                UnpackedImageData simd = colorModels[sindex] instanceof IndexColorModel ? pas[sindex]
                        .getComponents(sources[sindex], destRect, sources[sindex].getSampleModel()
                                .getTransferType()) : pas[sindex].getPixels(sources[sindex],
                        destRect, sources[sindex].getSampleModel().getTransferType(), false);

                int srcPixelStride = simd.pixelStride;
                int srcLineStride = simd.lineStride;
                int dstPixelStride = dimd.pixelStride;
                int dstLineStride = dimd.lineStride;
                int dRectWidth = destRect.width;
                // Cycle on each source bands
                for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                    if (db < dnbands) {
                        short[][] srcdata = (short[][]) simd.data;
                        int srcstart = simd.bandOffsets[sb];
                        int dststart = dimd.bandOffsets[db];
                        // Cycle on the y-axis
                        for (int y = 0; y < destRect.height; y++, srcstart += srcLineStride, dststart += dstLineStride) {
                            // Cycle on the x-axis
                            for (int i = 0, srcpos = srcstart, dstpos = dststart; i < dRectWidth; i++, srcpos += srcPixelStride, dstpos += dstPixelStride) {

                                dstdata[db][dstpos] = srcdata[sb][srcpos];
                            }
                        }
                    }
                }
            }
        }
        d.setPixels(dimd);
    }

    private void intLoop(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        // Source number
        int nSrcs = sources.length;
        // Bands associated with each sources
        int[] snbands = new int[nSrcs];
        // PixelAccessor array for each source
        PixelAccessor[] pas = new PixelAccessor[nSrcs];

        for (int i = 0; i < nSrcs; i++) {
            pas[i] = new PixelAccessor(sources[i].getSampleModel(), colorModels[i]);

            if (colorModels[i] instanceof IndexColorModel) {
                snbands[i] = colorModels[i].getNumComponents();
            } else {
                snbands[i] = sources[i].getNumBands();
            }
        }

        // Destination bands
        int dnbands = dest.getNumBands();
        // Destination data type
        int destType = dest.getTransferType();
        // PixelAccessor associated with the destination raster
        PixelAccessor d = new PixelAccessor(dest.getSampleModel(), null);

        UnpackedImageData dimd = d.getPixels(dest, destRect, destType, true);
        // Destination data values
        int[][] dstdata = (int[][]) dimd.data;

        // NO DATA PRESENT
        if (hasNoData) {
            // Cycle on all the sources
            for (int sindex = 0, db = 0; sindex < nSrcs; sindex++) {

                UnpackedImageData simd = colorModels[sindex] instanceof IndexColorModel ? pas[sindex]
                        .getComponents(sources[sindex], destRect, sources[sindex].getSampleModel()
                                .getTransferType()) : pas[sindex].getPixels(sources[sindex],
                        destRect, sources[sindex].getSampleModel().getTransferType(), false);

                int srcPixelStride = simd.pixelStride;
                int srcLineStride = simd.lineStride;
                int dstPixelStride = dimd.pixelStride;
                int dstLineStride = dimd.lineStride;
                int dRectWidth = destRect.width;
                // Cycle on each source bands
                for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                    if (db < dnbands) {
                        int[][] srcdata = (int[][]) simd.data;
                        int srcstart = simd.bandOffsets[sb];
                        int dststart = dimd.bandOffsets[db];
                        // Cycle on the y-axis
                        for (int y = 0; y < destRect.height; y++, srcstart += srcLineStride, dststart += dstLineStride) {
                            // Cycle on the x-axis
                            for (int i = 0, srcpos = srcstart, dstpos = dststart; i < dRectWidth; i++, srcpos += srcPixelStride, dstpos += dstPixelStride) {
                                // No Data control
                                if (noData[sindex].contains(srcdata[sb][srcpos])) {
                                    dstdata[db][dstpos] = destNoDataInt;
                                } else {
                                    dstdata[db][dstpos] = srcdata[sb][srcpos];
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Cycle on all the sources
            for (int sindex = 0, db = 0; sindex < nSrcs; sindex++) {

                UnpackedImageData simd = colorModels[sindex] instanceof IndexColorModel ? pas[sindex]
                        .getComponents(sources[sindex], destRect, sources[sindex].getSampleModel()
                                .getTransferType()) : pas[sindex].getPixels(sources[sindex],
                        destRect, sources[sindex].getSampleModel().getTransferType(), false);

                int srcPixelStride = simd.pixelStride;
                int srcLineStride = simd.lineStride;
                int dstPixelStride = dimd.pixelStride;
                int dstLineStride = dimd.lineStride;
                int dRectWidth = destRect.width;
                // Cycle on each source bands
                for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                    if (db < dnbands) {
                        int[][] srcdata = (int[][]) simd.data;
                        int srcstart = simd.bandOffsets[sb];
                        int dststart = dimd.bandOffsets[db];
                        // Cycle on the y-axis
                        for (int y = 0; y < destRect.height; y++, srcstart += srcLineStride, dststart += dstLineStride) {
                            // Cycle on the x-axis
                            for (int i = 0, srcpos = srcstart, dstpos = dststart; i < dRectWidth; i++, srcpos += srcPixelStride, dstpos += dstPixelStride) {

                                dstdata[db][dstpos] = srcdata[sb][srcpos];
                            }
                        }
                    }
                }
            }
        }
        d.setPixels(dimd);
    }

    private void floatLoop(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        // Source number
        int nSrcs = sources.length;
        // Bands associated with each sources
        int[] snbands = new int[nSrcs];
        // PixelAccessor array for each source
        PixelAccessor[] pas = new PixelAccessor[nSrcs];

        for (int i = 0; i < nSrcs; i++) {
            pas[i] = new PixelAccessor(sources[i].getSampleModel(), colorModels[i]);

            if (colorModels[i] instanceof IndexColorModel) {
                snbands[i] = colorModels[i].getNumComponents();
            } else {
                snbands[i] = sources[i].getNumBands();
            }
        }

        // Destination bands
        int dnbands = dest.getNumBands();
        // Destination data type
        int destType = dest.getTransferType();
        // PixelAccessor associated with the destination raster
        PixelAccessor d = new PixelAccessor(dest.getSampleModel(), null);

        UnpackedImageData dimd = d.getPixels(dest, destRect, destType, true);
        // Destination data values
        float[][] dstdata = (float[][]) dimd.data;

        // NO DATA PRESENT
        if (hasNoData) {
            // Cycle on all the sources
            for (int sindex = 0, db = 0; sindex < nSrcs; sindex++) {

                UnpackedImageData simd = colorModels[sindex] instanceof IndexColorModel ? pas[sindex]
                        .getComponents(sources[sindex], destRect, sources[sindex].getSampleModel()
                                .getTransferType()) : pas[sindex].getPixels(sources[sindex],
                        destRect, sources[sindex].getSampleModel().getTransferType(), false);

                int srcPixelStride = simd.pixelStride;
                int srcLineStride = simd.lineStride;
                int dstPixelStride = dimd.pixelStride;
                int dstLineStride = dimd.lineStride;
                int dRectWidth = destRect.width;
                // Cycle on each source bands
                for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                    if (db < dnbands) {
                        float[][] srcdata = (float[][]) simd.data;
                        int srcstart = simd.bandOffsets[sb];
                        int dststart = dimd.bandOffsets[db];
                        // Cycle on the y-axis
                        for (int y = 0; y < destRect.height; y++, srcstart += srcLineStride, dststart += dstLineStride) {
                            // Cycle on the x-axis
                            for (int i = 0, srcpos = srcstart, dstpos = dststart; i < dRectWidth; i++, srcpos += srcPixelStride, dstpos += dstPixelStride) {
                                // No Data control
                                if (noData[sindex].contains(srcdata[sb][srcpos])) {
                                    dstdata[db][dstpos] = destNoDataFloat;
                                } else {
                                    dstdata[db][dstpos] = srcdata[sb][srcpos];
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Cycle on all the sources
            for (int sindex = 0, db = 0; sindex < nSrcs; sindex++) {

                UnpackedImageData simd = colorModels[sindex] instanceof IndexColorModel ? pas[sindex]
                        .getComponents(sources[sindex], destRect, sources[sindex].getSampleModel()
                                .getTransferType()) : pas[sindex].getPixels(sources[sindex],
                        destRect, sources[sindex].getSampleModel().getTransferType(), false);

                int srcPixelStride = simd.pixelStride;
                int srcLineStride = simd.lineStride;
                int dstPixelStride = dimd.pixelStride;
                int dstLineStride = dimd.lineStride;
                int dRectWidth = destRect.width;
                // Cycle on each source bands
                for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                    if (db < dnbands) {
                        float[][] srcdata = (float[][]) simd.data;
                        int srcstart = simd.bandOffsets[sb];
                        int dststart = dimd.bandOffsets[db];
                        // Cycle on the y-axis
                        for (int y = 0; y < destRect.height; y++, srcstart += srcLineStride, dststart += dstLineStride) {
                            // Cycle on the x-axis
                            for (int i = 0, srcpos = srcstart, dstpos = dststart; i < dRectWidth; i++, srcpos += srcPixelStride, dstpos += dstPixelStride) {

                                dstdata[db][dstpos] = srcdata[sb][srcpos];
                            }
                        }
                    }
                }
            }
        }

        d.setPixels(dimd);
    }

    private void doubleLoop(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        // Source number
        int nSrcs = sources.length;
        // Bands associated with each sources
        int[] snbands = new int[nSrcs];
        // PixelAccessor array for each source
        PixelAccessor[] pas = new PixelAccessor[nSrcs];

        for (int i = 0; i < nSrcs; i++) {
            pas[i] = new PixelAccessor(sources[i].getSampleModel(), colorModels[i]);

            if (colorModels[i] instanceof IndexColorModel) {
                snbands[i] = colorModels[i].getNumComponents();
            } else {
                snbands[i] = sources[i].getNumBands();
            }
        }

        // Destination bands
        int dnbands = dest.getNumBands();
        // Destination data type
        int destType = dest.getTransferType();
        // PixelAccessor associated with the destination raster
        PixelAccessor d = new PixelAccessor(dest.getSampleModel(), null);

        UnpackedImageData dimd = d.getPixels(dest, destRect, destType, true);
        // Destination data values
        double[][] dstdata = (double[][]) dimd.data;

        // NO DATA PRESENT
        if (hasNoData) {
            // Cycle on all the sources
            for (int sindex = 0, db = 0; sindex < nSrcs; sindex++) {

                UnpackedImageData simd = colorModels[sindex] instanceof IndexColorModel ? pas[sindex]
                        .getComponents(sources[sindex], destRect, sources[sindex].getSampleModel()
                                .getTransferType()) : pas[sindex].getPixels(sources[sindex],
                        destRect, sources[sindex].getSampleModel().getTransferType(), false);

                int srcPixelStride = simd.pixelStride;
                int srcLineStride = simd.lineStride;
                int dstPixelStride = dimd.pixelStride;
                int dstLineStride = dimd.lineStride;
                int dRectWidth = destRect.width;
                // Cycle on each source bands
                for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                    if (db < dnbands) {
                        double[][] srcdata = (double[][]) simd.data;
                        int srcstart = simd.bandOffsets[sb];
                        int dststart = dimd.bandOffsets[db];
                        // Cycle on the y-axis
                        for (int y = 0; y < destRect.height; y++, srcstart += srcLineStride, dststart += dstLineStride) {
                            // Cycle on the x-axis
                            for (int i = 0, srcpos = srcstart, dstpos = dststart; i < dRectWidth; i++, srcpos += srcPixelStride, dstpos += dstPixelStride) {
                                // No Data control
                                if (noData[sindex].contains(srcdata[sb][srcpos])) {
                                    dstdata[db][dstpos] = destNoDataDouble;
                                } else {
                                    dstdata[db][dstpos] = srcdata[sb][srcpos];
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Cycle on all the sources
            for (int sindex = 0, db = 0; sindex < nSrcs; sindex++) {

                UnpackedImageData simd = colorModels[sindex] instanceof IndexColorModel ? pas[sindex]
                        .getComponents(sources[sindex], destRect, sources[sindex].getSampleModel()
                                .getTransferType()) : pas[sindex].getPixels(sources[sindex],
                        destRect, sources[sindex].getSampleModel().getTransferType(), false);

                int srcPixelStride = simd.pixelStride;
                int srcLineStride = simd.lineStride;
                int dstPixelStride = dimd.pixelStride;
                int dstLineStride = dimd.lineStride;
                int dRectWidth = destRect.width;
                // Cycle on each source bands
                for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                    if (db < dnbands) {
                        double[][] srcdata = (double[][]) simd.data;
                        int srcstart = simd.bandOffsets[sb];
                        int dststart = dimd.bandOffsets[db];
                        // Cycle on the y-axis
                        for (int y = 0; y < destRect.height; y++, srcstart += srcLineStride, dststart += dstLineStride) {
                            // Cycle on the x-axis
                            for (int i = 0, srcpos = srcstart, dstpos = dststart; i < dRectWidth; i++, srcpos += srcPixelStride, dstpos += dstPixelStride) {

                                dstdata[db][dstpos] = srcdata[sb][srcpos];
                            }
                        }
                    }
                }
            }
        }
        d.setPixels(dimd);
    }

    /**
     * This method takes in input a List of Objects and creates a vector from its elements
     * 
     * @param sources list of the input sources
     * @return a vector of all the input list
     */
    private static Vector vectorize(List sources) {
        if (sources instanceof Vector) {
            return (Vector) sources;
        } else {
            Vector vector = new Vector(sources.size());
            for (Object element : sources) {
                vector.add(element);
            }
            return vector;
        }
    }
}
