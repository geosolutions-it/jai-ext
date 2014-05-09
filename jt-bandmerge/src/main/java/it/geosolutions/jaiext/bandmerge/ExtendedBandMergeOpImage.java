package it.geosolutions.jaiext.bandmerge;

import it.geosolutions.jaiext.range.Range;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
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
import javax.media.jai.PlanarImage;
import javax.media.jai.UnpackedImageData;
import javax.media.jai.RasterFactory;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

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
class ExtendedBandMergeOpImage extends PointOpImage {

    /** List of ColorModels required for IndexColorModel support */
    ColorModel[] colorModels;

    /** Array containing all the No Data Ranges */
    private final Range[] noData;

    /** Boolean indicating if No Data are present */
    private final boolean hasNoData;

    private final boolean transformations;

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

    private List<AffineTransform> transforms;

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
    public ExtendedBandMergeOpImage(List sources, List<AffineTransform> transforms, Map config,
            Range[] noData, double destinationNoData, ImageLayout layout) {

        super(vectorize(sources), layoutHelper(sources, layout), config, false);

        // Set flag to permit in-place operation.
        permitInPlaceOperation();

        // Initial Check on the source number and the related transformations
        if (transforms != null) {
            transformations = true;
            if (transforms.size() != sources.size()) {
                throw new IllegalArgumentException("Wrong Transformations number");
            }
            // Setting of the variable for the transformations
            this.transforms = transforms;
        } else {
            transformations = false;
        }

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
        boolean newLayout = il == null;
        ImageLayout layout = newLayout ? new ImageLayout() : (ImageLayout) il.clone();
        // Number of input sources
        int numSources = sources.size();

        // dest data type is the maximum of transfertype of source image
        // utilizing the monotonicity of data types.

        // dest number of bands = sum of source bands
        int destNumBands = totalNumBands(sources);

        int destDataType = DataBuffer.TYPE_BYTE; // initialize
        RenderedImage srci = (RenderedImage) sources.get(0);
        // Boolean indicating that the rectangle intersection must be calculated
        boolean intersect = true;

        Rectangle destBounds = null;
        // If the layout is already present and contains the final image dimensions, then these dimensions are used for the layout
        if (layout.isValid(ImageLayout.MIN_X_MASK) && layout.isValid(ImageLayout.MIN_Y_MASK)
                && layout.isValid(ImageLayout.WIDTH_MASK)
                && layout.isValid(ImageLayout.HEIGHT_MASK)) {
            destBounds = new Rectangle(layout.getMinX(null), layout.getMinY(null),
                    layout.getWidth(null), layout.getHeight(null));
            intersect = false;
            if (destBounds.isEmpty()) {
                destBounds = null;
                intersect = true;
            }
        }

        // Destination Bounds are taken from the first image(if not present in the layout)
        if (intersect) {
            destBounds = new Rectangle(srci.getMinX(), srci.getMinY(), srci.getWidth(),
                    srci.getHeight());
        }

        // Cycle on all the images
        for (int i = 0; i < numSources; i++) {
            // Selection of a source
            srci = (RenderedImage) sources.get(i);
            // Intersection of the initial bounds with the source bounds, if not already defined
            if (intersect) {
                destBounds = destBounds.intersection(new Rectangle(srci.getMinX(), srci.getMinY(),
                        srci.getWidth(), srci.getHeight()));
            }

            // Selection of the source TransferType
            int typei = srci.getSampleModel().getTransferType();

            // NOTE: this depends on JDK ordering
            destDataType = typei > destDataType ? typei : destDataType;
        }

        if (intersect) {
            // Definition of the Layout
            layout.setMinX(destBounds.x);
            layout.setMinY(destBounds.y);
            layout.setWidth(destBounds.width);
            layout.setHeight(destBounds.height);
        }

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
     * @param sources Source images.
     * @param dest The tile containing the rectangle to be computed.
     * @param destRect The rectangle within the tile to be computed.
     */
    protected void computeRect(PlanarImage[] sources, WritableRaster dest, Rectangle destRect) {
        // Destination data type
        int destType = dest.getTransferType();
        // Loop on the image raster
        switch (destType) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(sources, dest, destRect);
            break;
        case DataBuffer.TYPE_SHORT:
            ushortLoop(sources, dest, destRect);
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

    private void byteLoop(PlanarImage[] sources, WritableRaster dest, Rectangle destRect) {
        // Source number
        int nSrcs = sources.length;
        // Bands associated with each sources
        int[] snbands = new int[nSrcs];

        // Destination bands
        int dnbands = dest.getNumBands();
        // Destination data type
        int destType = dest.getTransferType();
        // PixelAccessor associated with the destination raster
        PixelAccessor d = new PixelAccessor(dest.getSampleModel(), null);

        UnpackedImageData dimd = d.getPixels(dest, destRect, destType, true);

        // Destination data values
        byte[][] dstdata = (byte[][]) dimd.data;

        int dstPixelStride = dimd.pixelStride;
        int dstLineStride = dimd.lineStride;

        RandomIter iter;
        // Source and Destination Point2D objects
        Point2D ptSrc = new Point2D.Double(0, 0);
        Point2D ptDst = new Point2D.Double(0, 0);
        // Destination object initial position
        final int minX = dest.getMinX();
        final int minY = dest.getMinY();

        // NO DATA PRESENT
        if (hasNoData) {
            // Cycle on all the sources
            for (int sindex = 0, db = 0; sindex < nSrcs; sindex++) {
                // Random Iterator for cycling on the sources
                iter = RandomIterFactory.create(sources[sindex], sources[sindex].getBounds());
                // Affine transformation for the selected source
                AffineTransform trans = transforms.get(sindex);
                // Source corners
                final int srcMinX = sources[sindex].getMinX();
                final int srcMinY = sources[sindex].getMinY();
                final int srcMaxX = sources[sindex].getMaxX();
                final int srcMaxY = sources[sindex].getMaxY();
                // Destination Line and Pixel offset initialization
                int dstLineOffset = 0;
                int dstPixelOffset = 0;

                // Cycle on the y-axis
                for (int y = 0; y < destRect.height; y++) {
                    dstPixelOffset = dstLineOffset;
                    // Cycle on the x-axis
                    for (int x = 0; x < destRect.height; x++) {
                        // Set the x,y destination pixel location
                        ptDst.setLocation(x + minX, y + minY);
                        // Map destination pixel to source pixel
                        trans.transform(ptDst, ptSrc);
                        // Source pixel indexes
                        int srcX = round(ptSrc.getX());
                        int srcY = round(ptSrc.getY());
                        // Check if the pixel is inside the source dimension
                        if (srcX < srcMinX || srcX >= srcMaxX || srcY < srcMinY || srcY >= srcMaxY) {
                            // Cycle on the bands
                            for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                                if (db >= dnbands) {
                                    // exceeding destNumBands; should not have happened
                                    break;
                                }
                                // Setting the no data value
                                dstdata[db][dstPixelOffset + dimd.getOffset(db)] = destNoDataByte;
                            }
                        } else {
                            // Cycle on the bands
                            for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                                if (db >= dnbands) {
                                    // exceeding destNumBands; should not have happened
                                    break;
                                }
                                // No Data control
                                byte pixelValue = (byte) (iter.getSample(srcX, srcY, sb) & 0xFF);
                                if (noData[sindex].contains(pixelValue)) {
                                    dstdata[db][dstPixelOffset + dimd.getOffset(db)] = destNoDataByte;
                                } else {
                                    // Setting the value
                                    dstdata[db][dstPixelOffset + dimd.getOffset(db)] = pixelValue;
                                }
                            }
                        }
                        dstPixelOffset += dstPixelStride;
                    }
                    dstLineOffset += dstLineStride;
                }
            }
            // NO DATA NOT PRESENT
        } else {
            // Cycle on all the sources
            for (int sindex = 0, db = 0; sindex < nSrcs; sindex++) {
                // Random Iterator for cycling on the sources
                iter = RandomIterFactory.create(sources[sindex], sources[sindex].getBounds());
                // Affine transformation for the selected source
                AffineTransform trans = transforms.get(sindex);
                // Source corners
                final int srcMinX = sources[sindex].getMinX();
                final int srcMinY = sources[sindex].getMinY();
                final int srcMaxX = sources[sindex].getMaxX();
                final int srcMaxY = sources[sindex].getMaxY();
                // Destination Line and Pixel offset initialization
                int dstLineOffset = 0;
                int dstPixelOffset = 0;

                // Cycle on the y-axis
                for (int y = 0; y < destRect.height; y++) {
                    dstPixelOffset = dstLineOffset;
                    // Cycle on the x-axis
                    for (int x = 0; x < destRect.height; x++) {
                        // Set the x,y destination pixel location
                        ptDst.setLocation(x + minX, y + minY);
                        // Map destination pixel to source pixel
                        trans.transform(ptDst, ptSrc);
                        // Source pixel indexes
                        int srcX = round(ptSrc.getX());
                        int srcY = round(ptSrc.getY());
                        // Check if the pixel is inside the source dimension
                        if (srcX < srcMinX || srcX >= srcMaxX || srcY < srcMinY || srcY >= srcMaxY) {
                            // Cycle on the bands
                            for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                                if (db >= dnbands) {
                                    // exceeding destNumBands; should not have happened
                                    break;
                                }
                                // Setting the no data value
                                dstdata[db][dstPixelOffset + dimd.getOffset(db)] = destNoDataByte;
                            }
                        } else {
                            // Cycle on the bands
                            for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                                if (db >= dnbands) {
                                    // exceeding destNumBands; should not have happened
                                    break;
                                }
                                // Setting the value
                                dstdata[db][dstPixelOffset + dimd.getOffset(db)] = (byte) (iter
                                        .getSample(srcX, srcY, sb) & 0xFF);
                            }
                        }
                        dstPixelOffset += dstPixelStride;
                    }
                    dstLineOffset += dstLineStride;
                }
            }
        }
        d.setPixels(dimd);
    }

    private void ushortLoop(PlanarImage[] sources, WritableRaster dest, Rectangle destRect) {
        // Source number
        int nSrcs = sources.length;
        // Bands associated with each sources
        int[] snbands = new int[nSrcs];

        // Destination bands
        int dnbands = dest.getNumBands();
        // Destination data type
        int destType = dest.getTransferType();
        // PixelAccessor associated with the destination raster
        PixelAccessor d = new PixelAccessor(dest.getSampleModel(), null);

        UnpackedImageData dimd = d.getPixels(dest, destRect, destType, true);

        // Destination data values
        short[][] dstdata = (short[][]) dimd.data;

        int dstPixelStride = dimd.pixelStride;
        int dstLineStride = dimd.lineStride;

        RandomIter iter;
        // Source and Destination Point2D objects
        Point2D ptSrc = new Point2D.Double(0, 0);
        Point2D ptDst = new Point2D.Double(0, 0);
        // Destination object initial position
        final int minX = dest.getMinX();
        final int minY = dest.getMinY();

        // NO DATA PRESENT
        if (hasNoData) {
            // Cycle on all the sources
            for (int sindex = 0, db = 0; sindex < nSrcs; sindex++) {
                // Random Iterator for cycling on the sources
                iter = RandomIterFactory.create(sources[sindex], sources[sindex].getBounds());
                // Affine transformation for the selected source
                AffineTransform trans = transforms.get(sindex);
                // Source corners
                final int srcMinX = sources[sindex].getMinX();
                final int srcMinY = sources[sindex].getMinY();
                final int srcMaxX = sources[sindex].getMaxX();
                final int srcMaxY = sources[sindex].getMaxY();
                // Destination Line and Pixel offset initialization
                int dstLineOffset = 0;
                int dstPixelOffset = 0;

                // Cycle on the y-axis
                for (int y = 0; y < destRect.height; y++) {
                    dstPixelOffset = dstLineOffset;
                    // Cycle on the x-axis
                    for (int x = 0; x < destRect.height; x++) {
                        // Set the x,y destination pixel location
                        ptDst.setLocation(x + minX, y + minY);
                        // Map destination pixel to source pixel
                        trans.transform(ptDst, ptSrc);
                        // Source pixel indexes
                        int srcX = round(ptSrc.getX());
                        int srcY = round(ptSrc.getY());
                        // Check if the pixel is inside the source dimension
                        if (srcX < srcMinX || srcX >= srcMaxX || srcY < srcMinY || srcY >= srcMaxY) {
                            // Cycle on the bands
                            for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                                if (db >= dnbands) {
                                    // exceeding destNumBands; should not have happened
                                    break;
                                }
                                // Setting the no data value
                                dstdata[db][dstPixelOffset + dimd.getOffset(db)] = destNoDataShort;
                            }
                        } else {
                            // Cycle on the bands
                            for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                                if (db >= dnbands) {
                                    // exceeding destNumBands; should not have happened
                                    break;
                                }
                                // No Data control
                                short pixelValue = (short) (iter.getSample(srcX, srcY, sb) & 0xFFFF);
                                if (noData[sindex].contains(pixelValue)) {
                                    dstdata[db][dstPixelOffset + dimd.getOffset(db)] = destNoDataShort;
                                } else {
                                    // Setting the value
                                    dstdata[db][dstPixelOffset + dimd.getOffset(db)] = pixelValue;
                                }
                            }
                        }
                        dstPixelOffset += dstPixelStride;
                    }
                    dstLineOffset += dstLineStride;
                }
            }
            // NO DATA NOT PRESENT
        } else {
            // Cycle on all the sources
            for (int sindex = 0, db = 0; sindex < nSrcs; sindex++) {
                // Random Iterator for cycling on the sources
                iter = RandomIterFactory.create(sources[sindex], sources[sindex].getBounds());
                // Affine transformation for the selected source
                AffineTransform trans = transforms.get(sindex);
                // Source corners
                final int srcMinX = sources[sindex].getMinX();
                final int srcMinY = sources[sindex].getMinY();
                final int srcMaxX = sources[sindex].getMaxX();
                final int srcMaxY = sources[sindex].getMaxY();
                // Destination Line and Pixel offset initialization
                int dstLineOffset = 0;
                int dstPixelOffset = 0;

                // Cycle on the y-axis
                for (int y = 0; y < destRect.height; y++) {
                    dstPixelOffset = dstLineOffset;
                    // Cycle on the x-axis
                    for (int x = 0; x < destRect.height; x++) {
                        // Set the x,y destination pixel location
                        ptDst.setLocation(x + minX, y + minY);
                        // Map destination pixel to source pixel
                        trans.transform(ptDst, ptSrc);
                        // Source pixel indexes
                        int srcX = round(ptSrc.getX());
                        int srcY = round(ptSrc.getY());
                        // Check if the pixel is inside the source dimension
                        if (srcX < srcMinX || srcX >= srcMaxX || srcY < srcMinY || srcY >= srcMaxY) {
                            // Cycle on the bands
                            for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                                if (db >= dnbands) {
                                    // exceeding destNumBands; should not have happened
                                    break;
                                }
                                // Setting the no data value
                                dstdata[db][dstPixelOffset + dimd.getOffset(db)] = destNoDataShort;
                            }
                        } else {
                            // Cycle on the bands
                            for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                                if (db >= dnbands) {
                                    // exceeding destNumBands; should not have happened
                                    break;
                                }
                                // Setting the value
                                dstdata[db][dstPixelOffset + dimd.getOffset(db)] = (short) (iter
                                        .getSample(srcX, srcY, sb) & 0xFFFF);
                            }
                        }
                        dstPixelOffset += dstPixelStride;
                    }
                    dstLineOffset += dstLineStride;
                }
            }
        }
        d.setPixels(dimd);
    }

    private void shortLoop(PlanarImage[] sources, WritableRaster dest, Rectangle destRect) {
        // Source number
        int nSrcs = sources.length;
        // Bands associated with each sources
        int[] snbands = new int[nSrcs];

        // Destination bands
        int dnbands = dest.getNumBands();
        // Destination data type
        int destType = dest.getTransferType();
        // PixelAccessor associated with the destination raster
        PixelAccessor d = new PixelAccessor(dest.getSampleModel(), null);

        UnpackedImageData dimd = d.getPixels(dest, destRect, destType, true);

        // Destination data values
        short[][] dstdata = (short[][]) dimd.data;

        int dstPixelStride = dimd.pixelStride;
        int dstLineStride = dimd.lineStride;

        RandomIter iter;
        // Source and Destination Point2D objects
        Point2D ptSrc = new Point2D.Double(0, 0);
        Point2D ptDst = new Point2D.Double(0, 0);
        // Destination object initial position
        final int minX = dest.getMinX();
        final int minY = dest.getMinY();

        // NO DATA PRESENT
        if (hasNoData) {
            // Cycle on all the sources
            for (int sindex = 0, db = 0; sindex < nSrcs; sindex++) {
                // Random Iterator for cycling on the sources
                iter = RandomIterFactory.create(sources[sindex], sources[sindex].getBounds());
                // Affine transformation for the selected source
                AffineTransform trans = transforms.get(sindex);
                // Source corners
                final int srcMinX = sources[sindex].getMinX();
                final int srcMinY = sources[sindex].getMinY();
                final int srcMaxX = sources[sindex].getMaxX();
                final int srcMaxY = sources[sindex].getMaxY();
                // Destination Line and Pixel offset initialization
                int dstLineOffset = 0;
                int dstPixelOffset = 0;

                // Cycle on the y-axis
                for (int y = 0; y < destRect.height; y++) {
                    dstPixelOffset = dstLineOffset;
                    // Cycle on the x-axis
                    for (int x = 0; x < destRect.height; x++) {
                        // Set the x,y destination pixel location
                        ptDst.setLocation(x + minX, y + minY);
                        // Map destination pixel to source pixel
                        trans.transform(ptDst, ptSrc);
                        // Source pixel indexes
                        int srcX = round(ptSrc.getX());
                        int srcY = round(ptSrc.getY());
                        // Check if the pixel is inside the source dimension
                        if (srcX < srcMinX || srcX >= srcMaxX || srcY < srcMinY || srcY >= srcMaxY) {
                            // Cycle on the bands
                            for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                                if (db >= dnbands) {
                                    // exceeding destNumBands; should not have happened
                                    break;
                                }
                                // Setting the no data value
                                dstdata[db][dstPixelOffset + dimd.getOffset(db)] = destNoDataShort;
                            }
                        } else {
                            // Cycle on the bands
                            for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                                if (db >= dnbands) {
                                    // exceeding destNumBands; should not have happened
                                    break;
                                }
                                // No Data control
                                short pixelValue = (short) (iter.getSample(srcX, srcY, sb));
                                if (noData[sindex].contains(pixelValue)) {
                                    dstdata[db][dstPixelOffset + dimd.getOffset(db)] = destNoDataShort;
                                } else {
                                    // Setting the value
                                    dstdata[db][dstPixelOffset + dimd.getOffset(db)] = pixelValue;
                                }
                            }
                        }
                        dstPixelOffset += dstPixelStride;
                    }
                    dstLineOffset += dstLineStride;
                }
            }
            // NO DATA NOT PRESENT
        } else {
            // Cycle on all the sources
            for (int sindex = 0, db = 0; sindex < nSrcs; sindex++) {
                // Random Iterator for cycling on the sources
                iter = RandomIterFactory.create(sources[sindex], sources[sindex].getBounds());
                // Affine transformation for the selected source
                AffineTransform trans = transforms.get(sindex);
                // Source corners
                final int srcMinX = sources[sindex].getMinX();
                final int srcMinY = sources[sindex].getMinY();
                final int srcMaxX = sources[sindex].getMaxX();
                final int srcMaxY = sources[sindex].getMaxY();
                // Destination Line and Pixel offset initialization
                int dstLineOffset = 0;
                int dstPixelOffset = 0;

                // Cycle on the y-axis
                for (int y = 0; y < destRect.height; y++) {
                    dstPixelOffset = dstLineOffset;
                    // Cycle on the x-axis
                    for (int x = 0; x < destRect.height; x++) {
                        // Set the x,y destination pixel location
                        ptDst.setLocation(x + minX, y + minY);
                        // Map destination pixel to source pixel
                        trans.transform(ptDst, ptSrc);
                        // Source pixel indexes
                        int srcX = round(ptSrc.getX());
                        int srcY = round(ptSrc.getY());
                        // Check if the pixel is inside the source dimension
                        if (srcX < srcMinX || srcX >= srcMaxX || srcY < srcMinY || srcY >= srcMaxY) {
                            // Cycle on the bands
                            for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                                if (db >= dnbands) {
                                    // exceeding destNumBands; should not have happened
                                    break;
                                }
                                // Setting the no data value
                                dstdata[db][dstPixelOffset + dimd.getOffset(db)] = destNoDataShort;
                            }
                        } else {
                            // Cycle on the bands
                            for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                                if (db >= dnbands) {
                                    // exceeding destNumBands; should not have happened
                                    break;
                                }
                                // Setting the value
                                dstdata[db][dstPixelOffset + dimd.getOffset(db)] = (short) (iter
                                        .getSample(srcX, srcY, sb));
                            }
                        }
                        dstPixelOffset += dstPixelStride;
                    }
                    dstLineOffset += dstLineStride;
                }
            }
        }
        d.setPixels(dimd);
    }

    private void intLoop(PlanarImage[] sources, WritableRaster dest, Rectangle destRect) {
        // Source number
        int nSrcs = sources.length;
        // Bands associated with each sources
        int[] snbands = new int[nSrcs];

        // Destination bands
        int dnbands = dest.getNumBands();
        // Destination data type
        int destType = dest.getTransferType();
        // PixelAccessor associated with the destination raster
        PixelAccessor d = new PixelAccessor(dest.getSampleModel(), null);

        UnpackedImageData dimd = d.getPixels(dest, destRect, destType, true);

        // Destination data values
        int[][] dstdata = (int[][]) dimd.data;

        int dstPixelStride = dimd.pixelStride;
        int dstLineStride = dimd.lineStride;

        RandomIter iter;
        // Source and Destination Point2D objects
        Point2D ptSrc = new Point2D.Double(0, 0);
        Point2D ptDst = new Point2D.Double(0, 0);
        // Destination object initial position
        final int minX = dest.getMinX();
        final int minY = dest.getMinY();

        // NO DATA PRESENT
        if (hasNoData) {
            // Cycle on all the sources
            for (int sindex = 0, db = 0; sindex < nSrcs; sindex++) {
                // Random Iterator for cycling on the sources
                iter = RandomIterFactory.create(sources[sindex], sources[sindex].getBounds());
                // Affine transformation for the selected source
                AffineTransform trans = transforms.get(sindex);
                // Source corners
                final int srcMinX = sources[sindex].getMinX();
                final int srcMinY = sources[sindex].getMinY();
                final int srcMaxX = sources[sindex].getMaxX();
                final int srcMaxY = sources[sindex].getMaxY();
                // Destination Line and Pixel offset initialization
                int dstLineOffset = 0;
                int dstPixelOffset = 0;

                // Cycle on the y-axis
                for (int y = 0; y < destRect.height; y++) {
                    dstPixelOffset = dstLineOffset;
                    // Cycle on the x-axis
                    for (int x = 0; x < destRect.height; x++) {
                        // Set the x,y destination pixel location
                        ptDst.setLocation(x + minX, y + minY);
                        // Map destination pixel to source pixel
                        trans.transform(ptDst, ptSrc);
                        // Source pixel indexes
                        int srcX = round(ptSrc.getX());
                        int srcY = round(ptSrc.getY());
                        // Check if the pixel is inside the source dimension
                        if (srcX < srcMinX || srcX >= srcMaxX || srcY < srcMinY || srcY >= srcMaxY) {
                            // Cycle on the bands
                            for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                                if (db >= dnbands) {
                                    // exceeding destNumBands; should not have happened
                                    break;
                                }
                                // Setting the no data value
                                dstdata[db][dstPixelOffset + dimd.getOffset(db)] = destNoDataInt;
                            }
                        } else {
                            // Cycle on the bands
                            for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                                if (db >= dnbands) {
                                    // exceeding destNumBands; should not have happened
                                    break;
                                }
                                // No Data control
                                int pixelValue = (iter.getSample(srcX, srcY, sb));
                                if (noData[sindex].contains(pixelValue)) {
                                    dstdata[db][dstPixelOffset + dimd.getOffset(db)] = destNoDataInt;
                                } else {
                                    // Setting the value
                                    dstdata[db][dstPixelOffset + dimd.getOffset(db)] = pixelValue;
                                }
                            }
                        }
                        dstPixelOffset += dstPixelStride;
                    }
                    dstLineOffset += dstLineStride;
                }
            }
            // NO DATA NOT PRESENT
        } else {
            // Cycle on all the sources
            for (int sindex = 0, db = 0; sindex < nSrcs; sindex++) {
                // Random Iterator for cycling on the sources
                iter = RandomIterFactory.create(sources[sindex], sources[sindex].getBounds());
                // Affine transformation for the selected source
                AffineTransform trans = transforms.get(sindex);
                // Source corners
                final int srcMinX = sources[sindex].getMinX();
                final int srcMinY = sources[sindex].getMinY();
                final int srcMaxX = sources[sindex].getMaxX();
                final int srcMaxY = sources[sindex].getMaxY();
                // Destination Line and Pixel offset initialization
                int dstLineOffset = 0;
                int dstPixelOffset = 0;

                // Cycle on the y-axis
                for (int y = 0; y < destRect.height; y++) {
                    dstPixelOffset = dstLineOffset;
                    // Cycle on the x-axis
                    for (int x = 0; x < destRect.height; x++) {
                        // Set the x,y destination pixel location
                        ptDst.setLocation(x + minX, y + minY);
                        // Map destination pixel to source pixel
                        trans.transform(ptDst, ptSrc);
                        // Source pixel indexes
                        int srcX = round(ptSrc.getX());
                        int srcY = round(ptSrc.getY());
                        // Check if the pixel is inside the source dimension
                        if (srcX < srcMinX || srcX >= srcMaxX || srcY < srcMinY || srcY >= srcMaxY) {
                            // Cycle on the bands
                            for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                                if (db >= dnbands) {
                                    // exceeding destNumBands; should not have happened
                                    break;
                                }
                                // Setting the no data value
                                dstdata[db][dstPixelOffset + dimd.getOffset(db)] = destNoDataInt;
                            }
                        } else {
                            // Cycle on the bands
                            for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                                if (db >= dnbands) {
                                    // exceeding destNumBands; should not have happened
                                    break;
                                }
                                // Setting the value
                                dstdata[db][dstPixelOffset + dimd.getOffset(db)] = (iter
                                        .getSample(srcX, srcY, sb));
                            }
                        }
                        dstPixelOffset += dstPixelStride;
                    }
                    dstLineOffset += dstLineStride;
                }
            }
        }
        d.setPixels(dimd);
    }

    private void floatLoop(PlanarImage[] sources, WritableRaster dest, Rectangle destRect) {
        // Source number
        int nSrcs = sources.length;
        // Bands associated with each sources
        int[] snbands = new int[nSrcs];

        // Destination bands
        int dnbands = dest.getNumBands();
        // Destination data type
        int destType = dest.getTransferType();
        // PixelAccessor associated with the destination raster
        PixelAccessor d = new PixelAccessor(dest.getSampleModel(), null);

        UnpackedImageData dimd = d.getPixels(dest, destRect, destType, true);

        // Destination data values
        float[][] dstdata = (float[][]) dimd.data;

        int dstPixelStride = dimd.pixelStride;
        int dstLineStride = dimd.lineStride;

        RandomIter iter;
        // Source and Destination Point2D objects
        Point2D ptSrc = new Point2D.Double(0, 0);
        Point2D ptDst = new Point2D.Double(0, 0);
        // Destination object initial position
        final int minX = dest.getMinX();
        final int minY = dest.getMinY();

        // NO DATA PRESENT
        if (hasNoData) {
            // Cycle on all the sources
            for (int sindex = 0, db = 0; sindex < nSrcs; sindex++) {
                // Random Iterator for cycling on the sources
                iter = RandomIterFactory.create(sources[sindex], sources[sindex].getBounds());
                // Affine transformation for the selected source
                AffineTransform trans = transforms.get(sindex);
                // Source corners
                final int srcMinX = sources[sindex].getMinX();
                final int srcMinY = sources[sindex].getMinY();
                final int srcMaxX = sources[sindex].getMaxX();
                final int srcMaxY = sources[sindex].getMaxY();
                // Destination Line and Pixel offset initialization
                int dstLineOffset = 0;
                int dstPixelOffset = 0;

                // Cycle on the y-axis
                for (int y = 0; y < destRect.height; y++) {
                    dstPixelOffset = dstLineOffset;
                    // Cycle on the x-axis
                    for (int x = 0; x < destRect.height; x++) {
                        // Set the x,y destination pixel location
                        ptDst.setLocation(x + minX, y + minY);
                        // Map destination pixel to source pixel
                        trans.transform(ptDst, ptSrc);
                        // Source pixel indexes
                        int srcX = round(ptSrc.getX());
                        int srcY = round(ptSrc.getY());
                        // Check if the pixel is inside the source dimension
                        if (srcX < srcMinX || srcX >= srcMaxX || srcY < srcMinY || srcY >= srcMaxY) {
                            // Cycle on the bands
                            for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                                if (db >= dnbands) {
                                    // exceeding destNumBands; should not have happened
                                    break;
                                }
                                // Setting the no data value
                                dstdata[db][dstPixelOffset + dimd.getOffset(db)] = destNoDataFloat;
                            }
                        } else {
                            // Cycle on the bands
                            for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                                if (db >= dnbands) {
                                    // exceeding destNumBands; should not have happened
                                    break;
                                }
                                // No Data control
                                float pixelValue = (iter.getSampleFloat(srcX, srcY, sb));
                                if (noData[sindex].contains(pixelValue)) {
                                    dstdata[db][dstPixelOffset + dimd.getOffset(db)] = destNoDataFloat;
                                } else {
                                    // Setting the value
                                    dstdata[db][dstPixelOffset + dimd.getOffset(db)] = pixelValue;
                                }
                            }
                        }
                        dstPixelOffset += dstPixelStride;
                    }
                    dstLineOffset += dstLineStride;
                }
            }
            // NO DATA NOT PRESENT
        } else {
            // Cycle on all the sources
            for (int sindex = 0, db = 0; sindex < nSrcs; sindex++) {
                // Random Iterator for cycling on the sources
                iter = RandomIterFactory.create(sources[sindex], sources[sindex].getBounds());
                // Affine transformation for the selected source
                AffineTransform trans = transforms.get(sindex);
                // Source corners
                final int srcMinX = sources[sindex].getMinX();
                final int srcMinY = sources[sindex].getMinY();
                final int srcMaxX = sources[sindex].getMaxX();
                final int srcMaxY = sources[sindex].getMaxY();
                // Destination Line and Pixel offset initialization
                int dstLineOffset = 0;
                int dstPixelOffset = 0;

                // Cycle on the y-axis
                for (int y = 0; y < destRect.height; y++) {
                    dstPixelOffset = dstLineOffset;
                    // Cycle on the x-axis
                    for (int x = 0; x < destRect.height; x++) {
                        // Set the x,y destination pixel location
                        ptDst.setLocation(x + minX, y + minY);
                        // Map destination pixel to source pixel
                        trans.transform(ptDst, ptSrc);
                        // Source pixel indexes
                        int srcX = round(ptSrc.getX());
                        int srcY = round(ptSrc.getY());
                        // Check if the pixel is inside the source dimension
                        if (srcX < srcMinX || srcX >= srcMaxX || srcY < srcMinY || srcY >= srcMaxY) {
                            // Cycle on the bands
                            for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                                if (db >= dnbands) {
                                    // exceeding destNumBands; should not have happened
                                    break;
                                }
                                // Setting the no data value
                                dstdata[db][dstPixelOffset + dimd.getOffset(db)] = destNoDataFloat;
                            }
                        } else {
                            // Cycle on the bands
                            for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                                if (db >= dnbands) {
                                    // exceeding destNumBands; should not have happened
                                    break;
                                }
                                // Setting the value
                                dstdata[db][dstPixelOffset + dimd.getOffset(db)] =  (iter
                                        .getSampleFloat(srcX, srcY, sb));
                            }
                        }
                        dstPixelOffset += dstPixelStride;
                    }
                    dstLineOffset += dstLineStride;
                }
            }
        }
        d.setPixels(dimd);
    }

    private void doubleLoop(PlanarImage[] sources, WritableRaster dest, Rectangle destRect) {
        // Source number
        int nSrcs = sources.length;
        // Bands associated with each sources
        int[] snbands = new int[nSrcs];

        // Destination bands
        int dnbands = dest.getNumBands();
        // Destination data type
        int destType = dest.getTransferType();
        // PixelAccessor associated with the destination raster
        PixelAccessor d = new PixelAccessor(dest.getSampleModel(), null);

        UnpackedImageData dimd = d.getPixels(dest, destRect, destType, true);

        // Destination data values
        double[][] dstdata = (double[][]) dimd.data;

        int dstPixelStride = dimd.pixelStride;
        int dstLineStride = dimd.lineStride;

        RandomIter iter;
        // Source and Destination Point2D objects
        Point2D ptSrc = new Point2D.Double(0, 0);
        Point2D ptDst = new Point2D.Double(0, 0);
        // Destination object initial position
        final int minX = dest.getMinX();
        final int minY = dest.getMinY();

        // NO DATA PRESENT
        if (hasNoData) {
            // Cycle on all the sources
            for (int sindex = 0, db = 0; sindex < nSrcs; sindex++) {
                // Random Iterator for cycling on the sources
                iter = RandomIterFactory.create(sources[sindex], sources[sindex].getBounds());
                // Affine transformation for the selected source
                AffineTransform trans = transforms.get(sindex);
                // Source corners
                final int srcMinX = sources[sindex].getMinX();
                final int srcMinY = sources[sindex].getMinY();
                final int srcMaxX = sources[sindex].getMaxX();
                final int srcMaxY = sources[sindex].getMaxY();
                // Destination Line and Pixel offset initialization
                int dstLineOffset = 0;
                int dstPixelOffset = 0;

                // Cycle on the y-axis
                for (int y = 0; y < destRect.height; y++) {
                    dstPixelOffset = dstLineOffset;
                    // Cycle on the x-axis
                    for (int x = 0; x < destRect.height; x++) {
                        // Set the x,y destination pixel location
                        ptDst.setLocation(x + minX, y + minY);
                        // Map destination pixel to source pixel
                        trans.transform(ptDst, ptSrc);
                        // Source pixel indexes
                        int srcX = round(ptSrc.getX());
                        int srcY = round(ptSrc.getY());
                        // Check if the pixel is inside the source dimension
                        if (srcX < srcMinX || srcX >= srcMaxX || srcY < srcMinY || srcY >= srcMaxY) {
                            // Cycle on the bands
                            for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                                if (db >= dnbands) {
                                    // exceeding destNumBands; should not have happened
                                    break;
                                }
                                // Setting the no data value
                                dstdata[db][dstPixelOffset + dimd.getOffset(db)] = destNoDataDouble;
                            }
                        } else {
                            // Cycle on the bands
                            for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                                if (db >= dnbands) {
                                    // exceeding destNumBands; should not have happened
                                    break;
                                }
                                // No Data control
                                double pixelValue = (iter.getSampleDouble(srcX, srcY, sb));
                                if (noData[sindex].contains(pixelValue)) {
                                    dstdata[db][dstPixelOffset + dimd.getOffset(db)] = destNoDataDouble;
                                } else {
                                    // Setting the value
                                    dstdata[db][dstPixelOffset + dimd.getOffset(db)] = pixelValue;
                                }
                            }
                        }
                        dstPixelOffset += dstPixelStride;
                    }
                    dstLineOffset += dstLineStride;
                }
            }
            // NO DATA NOT PRESENT
        } else {
            // Cycle on all the sources
            for (int sindex = 0, db = 0; sindex < nSrcs; sindex++) {
                // Random Iterator for cycling on the sources
                iter = RandomIterFactory.create(sources[sindex], sources[sindex].getBounds());
                // Affine transformation for the selected source
                AffineTransform trans = transforms.get(sindex);
                // Source corners
                final int srcMinX = sources[sindex].getMinX();
                final int srcMinY = sources[sindex].getMinY();
                final int srcMaxX = sources[sindex].getMaxX();
                final int srcMaxY = sources[sindex].getMaxY();
                // Destination Line and Pixel offset initialization
                int dstLineOffset = 0;
                int dstPixelOffset = 0;

                // Cycle on the y-axis
                for (int y = 0; y < destRect.height; y++) {
                    dstPixelOffset = dstLineOffset;
                    // Cycle on the x-axis
                    for (int x = 0; x < destRect.height; x++) {
                        // Set the x,y destination pixel location
                        ptDst.setLocation(x + minX, y + minY);
                        // Map destination pixel to source pixel
                        trans.transform(ptDst, ptSrc);
                        // Source pixel indexes
                        int srcX = round(ptSrc.getX());
                        int srcY = round(ptSrc.getY());
                        // Check if the pixel is inside the source dimension
                        if (srcX < srcMinX || srcX >= srcMaxX || srcY < srcMinY || srcY >= srcMaxY) {
                            // Cycle on the bands
                            for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                                if (db >= dnbands) {
                                    // exceeding destNumBands; should not have happened
                                    break;
                                }
                                // Setting the no data value
                                dstdata[db][dstPixelOffset + dimd.getOffset(db)] = destNoDataDouble;
                            }
                        } else {
                            // Cycle on the bands
                            for (int sb = 0; sb < snbands[sindex]; sb++, db++) {
                                if (db >= dnbands) {
                                    // exceeding destNumBands; should not have happened
                                    break;
                                }
                                // Setting the value
                                dstdata[db][dstPixelOffset + dimd.getOffset(db)] =  (iter
                                        .getSampleDouble(srcX, srcY, sb));
                            }
                        }
                        dstPixelOffset += dstPixelStride;
                    }
                    dstLineOffset += dstLineStride;
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

    /** Returns the "round" value of a float. */
    private static final int round(double f) {
        return f >= 0 ? (int) (f + 0.5F) : (int) (f - 0.5F);
    }
}
