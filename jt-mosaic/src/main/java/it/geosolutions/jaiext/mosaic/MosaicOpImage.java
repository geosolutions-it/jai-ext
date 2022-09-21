/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
*    http://www.geo-solutions.it/
*    Copyright 2014 - 2015 GeoSolutions


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

import com.sun.media.jai.util.ImageUtil;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.PackedColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.media.jai.BorderExtender;
import javax.media.jai.BorderExtenderConstant;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.MosaicDescriptor;
import javax.media.jai.operator.MosaicType;

import it.geosolutions.jaiext.lookup.LookupTable;
import it.geosolutions.jaiext.lookup.LookupTableFactory;
import it.geosolutions.jaiext.mosaic.PixelIterator.PixelIteratorByte;
import it.geosolutions.jaiext.mosaic.PixelIterator.PixelIteratorDouble;
import it.geosolutions.jaiext.mosaic.PixelIterator.PixelIteratorFloat;
import it.geosolutions.jaiext.mosaic.PixelIterator.PixelIteratorInt;
import it.geosolutions.jaiext.mosaic.PixelIterator.PixelIteratorShort;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;

/**
 * This class takes an array of <code>RenderedImage</code> and creates a mosaic of them. If the
 * image pixels are No Data values, they are not calculated and the MosaicOpimage searches for the
 * pixels of the other source images in the same location. If all the pixels in the same location
 * are No Data, the destination image pixel will be a destination No Data value. This feature is
 * combined with the ROI support and alpha channel support(leaved unchanged). No Data support has
 * been added both in the BLEND and OVERLAY mosaic type. The MosaicOpimage behavior is equal to that
 * of the old MosaicOpimage, the only difference is the No Data support. The input values of the
 * first one are different because a Java Bean is used for storing all of them in a unique block
 * instead of different variables as the second one. This Java Bean is described in the
 * ImageMosaicBean class. Inside this class, other Java Beans are used for simplifying the image
 * data transport between the various method.
 */
// @SuppressWarnings("unchecked")
public class MosaicOpImage extends OpImage {
    /**
     * Default value for the destination image if every pixel in the same location is a no data
     */
    public static final double[] DEFAULT_DESTINATION_NO_DATA_VALUE = { 0 };

    /** mosaic type selected */
    private final MosaicType mosaicTypeSelected;

    /** Number of bands for every image */
    private final int numBands;

    /** Bean used for storing image data, ROI, alpha channel, Nodata Range */
    private final ImageMosaicBean[] imageBeans;

    /** Boolean for checking if the ROI is used in the mosaic */
    private boolean roiPresent;

    /**
     * Boolean for checking if the alpha channel is used only for bitmask or for weighting every
     * pixel with is alpha value associated
     */
    private boolean isAlphaBitmaskUsed;

    /** Boolean for checking if alpha channel is used in the mosaic */
    private boolean alphaPresent;

    /** Border extender for the source data */
    private final BorderExtender sourceBorderExtender;

    /** Border extender for the ROI or alpha channel data */
    private final BorderExtender zeroBorderExtender;

    /**
     * No data values for the destination image if the pixel of the same location are no Data (Byte)
     */
    private byte[] destinationNoDataByte;

    /**
     * No data values for the destination image if the pixel of the same location are no Data
     * (UShort)
     */
    private short[] destinationNoDataUShort;

    /**
     * No data values for the destination image if the pixel of the same location are no Data
     * (Short)
     */
    private short[] destinationNoDataShort;

    /**
     * No data values for the destination image if the pixel of the same location are no Data
     * (Integer)
     */
    private int[] destinationNoDataInt;

    /**
     * No data values for the destination image if the pixel of the same location are no Data
     * (Float)
     */
    private float[] destinationNoDataFloat;

    /**
     * No data values for the destination image if the pixel of the same location are no Data
     * (Double)
     */
    private double[] destinationNoDataDouble;

    /**
     * Table used for checking no data values. The first index indicates the source, the second the
     * band, the third the value
     */
    private final boolean[][][] byteLookupTable;

    /** The format tag for the destination image */
    private final RasterFormatTag rasterFormatTag;

    /** Enumerator for the type of mosaic weigher */
    public enum WeightType {
        WEIGHT_TYPE_ALPHA, WEIGHT_TYPE_ROI, WEIGHT_TYPE_NODATA;

    }

    /**
     * Static method for providing a valid layout to the OpImage constructor
     * 
     * @param noDatas
     */
    private static final ImageLayout checkLayout(List sources, ImageLayout layout,
            Range[] noDatas) {

        // Variable Initialization
        SampleModel targetSampleModel = null;
        ColorModel targetColorModel = null;

        // Source number
        int numSources = sources.size();
        if (numSources > 0) {
            // The sample model and the color model are taken from the first image
            ImageLayout tmp = getTargetSampleColorModel(sources, noDatas);
            targetColorModel = tmp.getColorModel(null);
            targetSampleModel = tmp.getSampleModel(null);
        } else if (layout != null // If there is no Images check the validity of the layout
                && layout.isValid(ImageLayout.WIDTH_MASK | ImageLayout.HEIGHT_MASK
                        | ImageLayout.SAMPLE_MODEL_MASK)) {
            // The sample model and the color model are taken from layout, we don't replace it
            targetSampleModel = layout.getSampleModel(null);
        } else {// Not valid layout
            throw new IllegalArgumentException("Layout not valid");
        }

        // get color and sample model
        if (targetSampleModel == null) {
            throw new IllegalArgumentException("No sample model present");
        }

        // If the source number is less than one the layout is cloned and returned
        if (numSources < 1) {
            return (ImageLayout) layout.clone();
        }

        // If the layout is null a new one is created, else it is cloned. This new
        // layout
        // is the layout for all the images
        ImageLayout mosaicLayout = layout == null ? new ImageLayout()
                : (ImageLayout) layout.clone();

        // A new Rectangle is calculated for storing the union of all the image
        // bounds
        Rectangle mosaicBounds = new Rectangle();
        // If the mosaic is valid his bounds are set to the new mosaicLayout
        if (mosaicLayout.isValid(ImageLayout.MIN_X_MASK | ImageLayout.MIN_Y_MASK
                | ImageLayout.WIDTH_MASK | ImageLayout.HEIGHT_MASK)) {
            mosaicBounds.setBounds(mosaicLayout.getMinX(null), mosaicLayout.getMinY(null),
                    mosaicLayout.getWidth(null), mosaicLayout.getHeight(null));
            // If the layout is not valid the mosaic bounds are calculated from
            // every image bounds
        } else if (numSources > 0) {
            RenderedImage source = (RenderedImage) sources.get(0);
            mosaicBounds.setBounds(source.getMinX(), source.getMinY(), source.getWidth(),
                    source.getHeight());
            for (int i = 1; i < numSources; i++) {
                source = (RenderedImage) sources.get(i);
                Rectangle sourceBounds = new Rectangle(source.getMinX(), source.getMinY(),
                        source.getWidth(), source.getHeight());
                mosaicBounds = mosaicBounds.union(sourceBounds);
            }
        }

        // The mosaic bounds are stored in the new layout
        mosaicLayout.setMinX(mosaicBounds.x);
        mosaicLayout.setMinY(mosaicBounds.y);
        mosaicLayout.setWidth(mosaicBounds.width);
        mosaicLayout.setHeight(mosaicBounds.height);
        mosaicLayout.setSampleModel(targetSampleModel);
        if (targetColorModel != null) {
            mosaicLayout.setColorModel(targetColorModel);
        }

        return mosaicLayout;
    }

    private static ImageLayout getTargetSampleColorModel(List sources, Range[] noDatas) {
        final int numSources = sources.size();

        // get first image as reference
        RenderedImage first = (RenderedImage) sources.get(0);
        ColorModel firstColorModel = first.getColorModel();
        SampleModel firstSampleModel = first.getSampleModel();

        // starting point image layout
        ImageLayout result = new ImageLayout();
        result.setSampleModel(firstSampleModel);
        // easy case
        if (numSources == 1) {
            result.setColorModel(firstColorModel);
            return result;
        }

        // See if they all are the same
        int firstDataType = firstSampleModel.getDataType();
        int firstBands = firstSampleModel.getNumBands();
        int firstSampleSize = firstSampleModel.getSampleSize()[0];
        boolean heterogeneous = false;
        boolean hasIndexedColorModels = firstColorModel instanceof IndexColorModel;
        boolean hasComponentColorModels = firstColorModel instanceof ComponentColorModel;
        boolean hasPackedColorModels = firstColorModel instanceof PackedColorModel;
        boolean hasUnrecognizedColorModels = !hasComponentColorModels && !hasIndexedColorModels
                && !hasPackedColorModels;
        boolean hasUnsupportedTypes = false;
        int maxBands = firstBands;
        for (int i = 1; i < numSources; i++) {
            RenderedImage source = (RenderedImage) sources.get(i);
            SampleModel sourceSampleModel = source.getSampleModel();
            ColorModel sourceColorModel = source.getColorModel();
            int sourceBands = sourceSampleModel.getNumBands();
            int sourceDataType = sourceSampleModel.getDataType();
            if (sourceDataType == DataBuffer.TYPE_UNDEFINED) {
                hasUnsupportedTypes = true;
            }

            if (sourceBands > maxBands) {
                maxBands = sourceBands;
            }

            if (sourceColorModel instanceof IndexColorModel) {
                hasIndexedColorModels = true;
            } else if (sourceColorModel instanceof ComponentColorModel) {
                hasComponentColorModels = true;
            } else if (sourceColorModel instanceof PackedColorModel) {
                hasPackedColorModels = true;
            } else {
                hasUnrecognizedColorModels = true;
            }

            if (sourceDataType != firstDataType || sourceBands != firstBands) {
                heterogeneous = true;
            }

            for (int j = 0; j < sourceBands; j++) {
                if (sourceSampleModel.getSampleSize(j) != firstSampleSize) {
                    heterogeneous = true;
                }
            }
        }
        // see how many types we're dealing with
        int colorModelsTypes = (hasIndexedColorModels ? 1 : 0) + (hasComponentColorModels ? 1 : 0)
                + (hasPackedColorModels ? 1 : 0);
        // if uniform, we have it easy
        if (!heterogeneous && colorModelsTypes == 1) {
            if (hasIndexedColorModels) {
                boolean uniformPalettes = hasUniformPalettes(sources, noDatas);
                if (!uniformPalettes) {
                    // force RGB expansion
                    setRGBLayout(result, first, hasAlpha(sources));
                }
            }
            return result;
        }

        if (hasUnrecognizedColorModels || hasUnsupportedTypes) {
            throw new IllegalArgumentException("Cannot mosaic the input images, "
                    + "the mix of provided color and sample models is not supported");
        }
        
        // all gray?
        if (maxBands == 1 && !hasIndexedColorModels) {
            // push for the largest type
            SampleModel sm = firstSampleModel;
            for (int i = 1; i < numSources; i++) {
                RenderedImage source = (RenderedImage) sources.get(i);
                SampleModel sourceSampleModel = source.getSampleModel();
                int sourceDataType = sourceSampleModel.getDataType();
                if (sourceDataType > sm.getDataType()) {
                    sm = sourceSampleModel;
                }
            }
            result.setSampleModel(sm);
        } else {
            // take a leap of faith and assume we can manage this with a RGB output
            setRGBLayout(result, first, hasAlpha(sources));
        }

        return result;
    }

    private static boolean hasAlpha(List sources) {
        int numSources = sources.size();
        boolean hasAlphaBand = false;
        for (int i = 1; i < numSources; i++) {
            RenderedImage source = (RenderedImage) sources.get(i);
            SampleModel sourceSampleModel = source.getSampleModel();
            ColorModel sourceColorModel = source.getColorModel();
            hasAlphaBand |=
                    sourceSampleModel.getNumBands() > 1
                            && sourceColorModel.hasAlpha()
                            && sourceColorModel.getTransparency() == ColorModel.TRANSLUCENT;
        }
        return hasAlphaBand;
    }

    private static void setRGBLayout(
            ImageLayout result, RenderedImage reference, boolean hasAlpha) {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        ColorModel cm;
        if (hasAlpha) {
            int[] nBits = new int[] {8, 8, 8, 8};
            cm =
                    new ComponentColorModel(
                            cs, nBits, true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
        } else {
            int[] nBits = new int[] {8, 8, 8};
            cm =
                    new ComponentColorModel(
                            cs, nBits, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        }
        SampleModel sampleModel = reference.getSampleModel();
        SampleModel sm =
                cm.createCompatibleSampleModel(sampleModel.getWidth(), sampleModel.getHeight());
        result.setColorModel(cm);
        result.setSampleModel(sm);
    }

    private static boolean hasUniformPalettes(List sources, Range[] noDatas) {
        // all indexed, but are the palettes the same?
        RenderedImage first = (RenderedImage) sources.get(0);
        Range firstNoData = noDatas != null ? noDatas[0] : null;
        IndexColorModel reference = (IndexColorModel) first.getColorModel();
        int mapSize = reference.getMapSize();
        byte[] reference_reds = new byte[mapSize];
        byte[] reference_greens = new byte[mapSize];
        byte[] reference_blues = new byte[mapSize];
        byte[] reference_alphas = new byte[mapSize];
        byte[] reds = new byte[mapSize];
        byte[] greens = new byte[mapSize];
        byte[] blues = new byte[mapSize];
        byte[] alphas = new byte[mapSize];
        reference.getReds(reference_reds);
        reference.getGreens(reference_greens);
        reference.getBlues(reference_blues);
        reference.getAlphas(reference_alphas);
        boolean uniformPalettes = true;
        final int numSources = sources.size();
        for (int i = 1; i < numSources; i++) {
            RenderedImage source = (RenderedImage) sources.get(i);

            // we need the nodata to be uniform too, if we want to avoid color expansion
            Range noData = noDatas == null ? null : noDatas[i];
            if (firstNoData == null) {
                if (noData != null) {
                    return false;
                }
            } else {
                if (noData == null || !noData.equals(firstNoData)) {
                    return false;
                }
            }

            IndexColorModel sourceColorModel = (IndexColorModel) source.getColorModel();

            // check the basics
            if (reference.getNumColorComponents() != sourceColorModel.getNumColorComponents()) {
                throw new IllegalArgumentException("Cannot mosaic togheter images with index "
                        + "color models having different numbers of color components:\n "
                        + reference + "\n" + sourceColorModel);
            }

            // if not the same color space, then we need to expand
            if (!reference.getColorSpace().equals(reference.getColorSpace())) {
                return false;
            }

            if (!sourceColorModel.equals(reference) || sourceColorModel.getMapSize() != mapSize) {
                uniformPalettes = false;
                break;
            }
            // the above does not compare the rgb(a) arrays, do it
            sourceColorModel.getReds(reds);
            sourceColorModel.getGreens(greens);
            sourceColorModel.getBlues(blues);
            sourceColorModel.getAlphas(alphas);
            if (!Arrays.equals(reds, reference_reds) || !Arrays.equals(greens, reference_greens)
                    || !Arrays.equals(blues, reference_blues)
                    || !Arrays.equals(alphas, reference_alphas)) {
                uniformPalettes = false;
                break;
            }
        }
        return uniformPalettes;
    }

    /**
     * This constructor takes the source images, the layout, the rendering hints, and the parameters
     * and initialize variables.
     */
    public MosaicOpImage(List sources, ImageLayout layout, Map renderingHints,
            MosaicType mosaicTypeSelected, PlanarImage[] alphaImgs, ROI[] rois,
            double[][] thresholds, double[] destinationNoData, Range[] noDatas) {
        // OpImage constructor
        super((Vector) sources, checkLayout(sources, layout, noDatas), renderingHints, true);

        // Stores the data passed by the parameterBlock
        this.numBands = sampleModel.getNumBands();
        int numSources = getNumSources();
        this.mosaicTypeSelected = mosaicTypeSelected;
        this.roiPresent = false;
        this.alphaPresent = false;

        // boolean indicating if ROI, alpha bands and nodata are present
        boolean roiExists = rois != null;
        boolean alphaExists = alphaImgs != null;
        boolean nodataExists = noDatas != null;

        // Checks on the size
        if (roiExists && rois.length != numSources) {
            throw new IllegalArgumentException("roi number is not equal to the source number");
        }
        if (alphaExists && alphaImgs.length != numSources) {
            throw new IllegalArgumentException(
                    "alpha bands number is not equal to the source number");
        }
        if (nodataExists && noDatas.length != numSources) {
            throw new IllegalArgumentException("no data number is not equal to the source number");
        }
        // Definition of the Bounds
        Rectangle totalBounds = getBounds();

        // Type of data used for every image
        int dataType = sampleModel.getDataType();

        // BorderExtender used for filling the ROI or alpha images border values.
        this.zeroBorderExtender = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);

        // Stores the destination no data values.
        if (destinationNoData == null || destinationNoData.length == 0) {
            this.destinationNoDataDouble = DEFAULT_DESTINATION_NO_DATA_VALUE;
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                this.destinationNoDataInt = new int[numBands];
                Arrays.fill(this.destinationNoDataInt, Integer.MIN_VALUE);
                this.destinationNoDataByte = new byte[numBands];
                Arrays.fill(this.destinationNoDataByte, (byte) 0);
                break;
            case DataBuffer.TYPE_USHORT:
                this.destinationNoDataInt = new int[numBands];
                Arrays.fill(this.destinationNoDataInt, Integer.MIN_VALUE);
                this.destinationNoDataUShort = new short[numBands];
                Arrays.fill(this.destinationNoDataUShort, (short) 0);
                break;
            case DataBuffer.TYPE_SHORT:
                this.destinationNoDataInt = new int[numBands];
                Arrays.fill(this.destinationNoDataInt, Integer.MIN_VALUE);
                this.destinationNoDataShort = new short[numBands];
                Arrays.fill(this.destinationNoDataShort, Short.MIN_VALUE);
                break;
            case DataBuffer.TYPE_INT:
                this.destinationNoDataInt = new int[numBands];
                Arrays.fill(this.destinationNoDataInt, Integer.MIN_VALUE);
                break;
            case DataBuffer.TYPE_FLOAT:
                this.destinationNoDataFloat = new float[numBands];
                Arrays.fill(this.destinationNoDataFloat, -Float.MAX_VALUE);
                break;
            case DataBuffer.TYPE_DOUBLE:
                Arrays.fill(this.destinationNoDataDouble, -Double.MAX_VALUE);
                break;
            default:
                throw new IllegalArgumentException("Wrong data Type");
            }
        } else {
            this.destinationNoDataDouble = new double[numBands];
            if (destinationNoData.length < numBands) {
                Arrays.fill(this.destinationNoDataDouble, destinationNoData[0]);
            } else {
                System.arraycopy(destinationNoData, 0, this.destinationNoDataDouble, 0, numBands);
            }
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                this.destinationNoDataByte = new byte[numBands];
                if (destinationNoData.length < numBands) {
                    Arrays.fill(this.destinationNoDataByte, (byte) destinationNoData[0]);
                } else {
                    for (int i = 0; i < numBands; i++) {
                        this.destinationNoDataByte[i] = (byte) (destinationNoData[i]);
                    }
                }
                this.destinationNoDataInt = new int[numBands];
                for (int i = 0; i < destinationNoDataInt.length; i++) {
                    destinationNoDataInt[i] = destinationNoDataByte[i];
                }
                break;
            case DataBuffer.TYPE_USHORT:
                this.destinationNoDataUShort = new short[numBands];
                if (destinationNoData.length < numBands) {
                    Arrays.fill(this.destinationNoDataUShort,
                            (short) ((short) (destinationNoData[0]) & 0xffff));
                } else {
                    for (int i = 0; i < numBands; i++) {
                        this.destinationNoDataUShort[i] = (short) ((short) (destinationNoData[i])
                                & 0xffff);
                    }
                }
                this.destinationNoDataInt = new int[numBands];
                for (int i = 0; i < destinationNoDataInt.length; i++) {
                    destinationNoDataInt[i] = destinationNoDataUShort[i];
                }
                break;
            case DataBuffer.TYPE_SHORT:
                this.destinationNoDataShort = new short[numBands];
                if (destinationNoData.length < numBands) {
                    Arrays.fill(this.destinationNoDataShort, (short) destinationNoData[0]);
                } else {
                    for (int i = 0; i < numBands; i++) {
                        this.destinationNoDataShort[i] = (short) destinationNoData[i];
                    }
                }
                this.destinationNoDataInt = new int[numBands];
                for (int i = 0; i < destinationNoDataInt.length; i++) {
                    destinationNoDataInt[i] = destinationNoDataShort[i];
                }
                break;
            case DataBuffer.TYPE_INT:
                this.destinationNoDataInt = new int[numBands];
                if (destinationNoData.length < numBands) {
                    Arrays.fill(this.destinationNoDataInt, (int) destinationNoData[0]);
                } else {
                    for (int i = 0; i < numBands; i++) {
                        this.destinationNoDataInt[i] = (int) destinationNoData[i];
                    }
                }
                break;
            case DataBuffer.TYPE_FLOAT:
                this.destinationNoDataFloat = new float[numBands];
                if (destinationNoData.length < numBands) {
                    Arrays.fill(this.destinationNoDataFloat, (float) destinationNoData[0]);
                } else {
                    for (int i = 0; i < numBands; i++) {
                        this.destinationNoDataFloat[i] = (float) destinationNoData[i];
                    }
                }
                break;
            case DataBuffer.TYPE_DOUBLE:
                break;
            default:
                throw new IllegalArgumentException("Wrong data Type");
            }
        }

        // Value for filling the image border
        double sourceExtensionBorder = (noDatas != null && noDatas.length > 0 && noDatas[0] != null)
                ? noDatas[0].getMin().doubleValue() : destinationNoDataDouble[0];

        // BorderExtender used for filling the image border with the above
        // sourceExtensionBorder
        this.sourceBorderExtender = sourceExtensionBorder == 0.0
                ? BorderExtender.createInstance(BorderExtender.BORDER_ZERO)
                : new BorderExtenderConstant(new double[] { sourceExtensionBorder });

        // This list contains the alpha channel for every source image (if present)
        List<PlanarImage> alphaList = new ArrayList<PlanarImage>();

        // NoDataRangeByte initialization
        byteLookupTable = new boolean[numSources][numBands][256];

        // Init the imagemosaic bean
        imageBeans = new ImageMosaicBean[numSources];
        for (int i = 0; i < numSources; i++) {
            imageBeans[i] = new ImageMosaicBean();
        }

        // This cycle is used for checking if every alpha channel is single banded
        // and has the same sample model of the source images.
        RasterFormatTag[] tags = getRasterFormatTags();
        for (int i = 0; i < numSources; i++) {
            // Selection of the i-th source.
            PlanarImage img = getSourceImage(i);
            // Calculation of the padding
            imageBeans[i].setImage(img);
            // Selection of the alpha channel
            PlanarImage alpha = alphaExists ? alphaImgs[i] : null;
            alphaList.add(alpha);
            ROI roi = roiExists ? rois[i] : null;
            if (alpha != null) {
                alphaPresent = true;
                SampleModel alphaSampleModel = alpha.getSampleModel();
                imageBeans[i].setAlphaChannel(alpha);
  
                if (alphaSampleModel.getNumBands() != 1) {
                    throw new IllegalArgumentException("Alpha bands number must be 1");
                } else if (alphaSampleModel.getDataType() != sampleModel.getDataType()) {
                    throw new IllegalArgumentException(
                            "Alpha sample model dataType and Source sample model "
                                    + "dataTypes must be equal");
                } else if (alphaSampleModel.getSampleSize(0) != sampleModel.getSampleSize(0)) {
                    throw new IllegalArgumentException(
                            "Alpha sample model sampleSize and Source sample model "
                                    + "sampleSize must be equal");
                }
            }
            // If even only one ROI is present, the roiPresent flag is set to True
            if (roi != null) {
                roiPresent = true;
                RenderedImage roiIMG = roi.getAsImage();
                imageBeans[i].setRoiImage(roiIMG);
                imageBeans[i].setRoi(roi);
            }

            // set the raster tag
            imageBeans[i].setRasterFormatTag(tags[i]);

            // prepare to handle nodata, if any is available
            Range noDataRange = nodataExists ? noDatas[i] : null;
            if (noDataRange != null) {

                RenderedImage image = imageBeans[i].getImage();
                Range expandedNoDataRage = RasterAccessorExt.expandNoData(noDataRange, tags[i],
                        image, this);
                // convert the range to the target type, we might find it's null and if not,
                // we'll get an optimized contains method to play against
                int formatDataType = tags[i].getFormatTagID() & RasterAccessorExt.DATATYPE_MASK;
                Range convertedNoDataRange = RangeFactory.convert(expandedNoDataRage,
                        formatDataType);

                if (convertedNoDataRange != null) {
                    imageBeans[i].setSourceNoData(convertedNoDataRange);
                    
                    if (RasterAccessorExt.isPaletteExpansionRequired(image,
                            tags[i].getFormatTagID())) {
                        // no way to guarantee that the index in the palette can be turned into a
                        // RGB
                        // nodata, just a few cases where it breaks:
                        // - the palette has a color in the nodata element that's a valid one
                        // - the palette uses a color that's the output nodata as a valid value
                        // We turn the nodata into a ROI, and if the ROI is already set, we
                        // intersect it

                        // force a binary image
                        ImageLayout il = new ImageLayout();
                        byte[] arr = { (byte) 0, (byte) 0xff };
                        ColorModel binaryCm = new IndexColorModel(1, 2, arr, arr, arr);
                        SampleModel binarySm = new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE,
                                image.getWidth(), image.getHeight(), 1);
                        il.setColorModel(binaryCm);
                        il.setSampleModel(binarySm);
                        RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, il);
                        hints.put(JAI.KEY_TRANSFORM_ON_COLORMAP, false);

                        LookupTableJAI lt = buildNoDataLookupTable(dataType, noDataRange);

                        ParameterBlock pb = new ParameterBlock();
                        pb.setSource(image, 0);
                        pb.set(lt, 0);
                        RenderedOp noDataMask = JAI.create("lookup", pb, hints);
                        noDataMask.getTile(0, 0);

                        ROI noDataRoi = new ROI(noDataMask);
                        if (imageBeans[i].getRoi() == null) {
                            roiPresent = true;
                            imageBeans[i].setRoi(noDataRoi);
                            imageBeans[i].setRoiImage(noDataRoi.getAsImage());
                        } else {
                            ROI intersection = noDataRoi.intersect(imageBeans[i].getRoi());
                            imageBeans[i].setRoi(intersection);
                            imageBeans[i].setRoiImage(intersection.getAsImage());
                        }
                        // we transformed the nodata into the ROI
                        imageBeans[i].setSourceNoData(null);
                    } else if (dataType == DataBuffer.TYPE_BYTE) {
                        // selection of the no data range for byte values
                        Range noDataByte = expandedNoDataRage;

                        // The lookup table is filled with the related no data or valid data for
                        // every value
                        for (int b = 0; b < numBands; b++) {
                            for (int z = 0; z < byteLookupTable[i][0].length; z++) {
                                if (noDataByte != null && noDataByte.contains(z)) {
                                    byteLookupTable[i][b][z] = false;
                                } else {
                                    byteLookupTable[i][b][z] = true;
                                }
                            }
                        }
                    }
                }
            }

        }

        // compute the destination tag
        rasterFormatTag = tags[getNumSources()];

        if (!this.isAlphaBitmaskUsed) {
            for (int i = 0; i < numSources; i++) {
                if (alphaList.get(i) == null) {
                    this.isAlphaBitmaskUsed = true;
                    break;
                }
            }
        }
    }

    private LookupTable buildNoDataLookupTable(int dataType, Range noDataRange) {
        byte[] table;
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            table = new byte[256];
            for (int i = 0; i < table.length; i++) {
                if (noDataRange.contains(i)) {
                    table[i] = 0;
                } else {
                    table[i] = 1;
                }
            }
            break;
        case DataBuffer.TYPE_USHORT:
            table = new byte[65536];
            for (int i = 0; i < table.length; i++) {
                if (noDataRange.contains(i)) {
                    table[i] = 0;
                } else {
                    table[i] = 1;
                }
            }
            break;
        default:
            throw new IllegalArgumentException(
                    "Unable to handle a index color model based on data type " + dataType);
        }

        return LookupTableFactory.create(table);
    }

    private RasterFormatTag[] getRasterFormatTags() {
        final int numSources = getNumSources();
        RenderedImage[] sources = new RenderedImage[numSources];
        for (int i = 0; i < numSources; i++) {
            sources[i] = getSourceImage(i);
        }
        return RasterAccessorExt.findCompatibleTags(sources, this);
    }

    /**
     * This method overrides the OpImage compute tile method and calculates the mosaic operation for
     * the selected tile.
     */
    public Raster computeTile(int tileX, int tileY) {
        // The destination raster is created as WritableRaster
        WritableRaster destRaster = createWritableRaster(sampleModel,
                new Point(tileXToX(tileX), tileYToY(tileY)));

        // This method calculates the tile active area.
        Rectangle destRectangle = getTileRect(tileX, tileY);
        // Stores the source image number
        int numSources = getNumSources();
        // Initialization of a new RasterBean for passing all the raster information
        // to the compute rect method
        Raster[] sourceRasters = new Raster[numSources];
        Rectangle[] sourceRectangles = new Rectangle[numSources];
        RasterFormatTag[] sourceTags = new RasterFormatTag[numSources];
        ColorModel[] sourceColorModels = new ColorModel[numSources];
        Raster[] alphaRasters = new Raster[numSources];
        Raster[] roiRasters = new Raster[numSources];
        Range[] noDataRanges = new Range[numSources];
        ColorModel[] alphaChannelColorModels = new ColorModel[numSources];
        // The previous array is filled with the source raster data
        int intersectingSourceCount = 0;

        for (int i = 0; i < numSources; i++) {
            PlanarImage source = getSourceImage(i);
            Rectangle srcRect = mapDestRect(destRectangle, i);
            Raster data = null;
            // First, check if the source mapped rectangle is not empty
            if (!(srcRect != null && srcRect.isEmpty())) {
                // Get the source data from the source or the padded image.
                data = source.getData(srcRect);   
            }
            // Raster bean initialization
            // If the data are present then we can check if Alpha and ROI are present
            if (data != null) {
                sourceRasters[intersectingSourceCount] = data;
                sourceRectangles[intersectingSourceCount] = srcRect != null && !srcRect.equals(destRectangle) ? srcRect : null;
                sourceTags[intersectingSourceCount] = imageBeans[i].getRasterFormatTag();
                sourceColorModels[intersectingSourceCount] = imageBeans[i].getColorModel();
                noDataRanges[intersectingSourceCount] = imageBeans[i].getSourceNoData();

                // Get the Alpha data from the padded alpha image if present
                PlanarImage alpha = imageBeans[i].getAlphaChannel();
                if (alphaPresent && alpha != null) {
                    alphaRasters[intersectingSourceCount] = alpha.getData(srcRect);
                    alphaChannelColorModels[intersectingSourceCount] = imageBeans[i].getAlphaChannel().getColorModel();
                }

                // Get the ROI data from the padded ROI image if present
                RenderedImage roi = imageBeans[i].getRoiImage();
                if (roiPresent && roi != null) {
                    roiRasters[intersectingSourceCount] =  PlanarImage.wrapRenderedImage(roi).getExtendedData(destRectangle, zeroBorderExtender);
                }
                
                intersectingSourceCount++;
            }

        }
        
        // For the given source destination rasters, the mosaic is calculated
        computeRect(sourceRasters, sourceRectangles, sourceTags, sourceColorModels, destRaster, destRectangle,
                alphaRasters, roiRasters, noDataRanges, alphaChannelColorModels, intersectingSourceCount);

        // Tile recycling if the Recycle is present
        for (int i = 0; i < numSources; i++) {
            Raster sourceData = sourceRasters[i];
            if (sourceData != null) {
                PlanarImage source = getSourceImage(i);

                if (source.overlapsMultipleTiles(sourceData.getBounds())) {
                    recycleTile(sourceData);
                }
            }
        }

        return destRaster;

    }

    private void computeRect(Raster[] sourceRasters, Rectangle[] sourceRectangles, RasterFormatTag[] rasterFormatTags,
            ColorModel[] sourceColorModels, WritableRaster destRaster, Rectangle destRectangle,
            Raster[] alphaRasters, Raster[] roiRasters, Range[] noDataRanges, ColorModel[] alphaChannelColorModels, int sourcesNumber) {
        // if all null, just return a constant image
        if (sourcesNumber == 0) {
            ImageUtil.fillBackground(destRaster, destRectangle, destinationNoDataDouble);
            return;
        }

        // Create dest accessor.
        RasterAccessor destinationAccessor = new RasterAccessor(destRaster, destRectangle,
                rasterFormatTag, null);
        int destinationDataType = destinationAccessor.getDataType();

        // Creates source accessors bean array (a new bean)
        RasterBeanAccessor[] sourceAccessorsArrayBean = new RasterBeanAccessor[sourcesNumber];
        // The above array is filled with image data, roi, alpha and no data ranges
        for (int i = 0; i < sourcesNumber; i++) {
            // RasterAccessorBean temporary file
            RasterBeanAccessor helpAccessor = new RasterBeanAccessor();
            helpAccessor.setBounds(sourceRectangles[i]);
            Rectangle raRect = sourceRectangles[i] != null ? sourceRectangles[i] : destRectangle;
            int dataType = getSampleModel().getDataType();
            if (sourceRasters[i] != null) {
                helpAccessor.setDataRasterAccessor(new RasterAccessorExt(sourceRasters[i],
                        raRect, rasterFormatTags[i], sourceColorModels[i], getNumBands(),
                        dataType));
            }
            Raster alphaRaster = alphaRasters[i];
            if (alphaRaster != null) {
                SampleModel alphaSampleModel = alphaRaster.getSampleModel();
                int alphaFormatTagID = RasterAccessor.findCompatibleTag(null, alphaSampleModel);
                RasterFormatTag alphaFormatTag = new RasterFormatTag(alphaSampleModel,
                        alphaFormatTagID);
                helpAccessor.setAlphaRasterAccessor(new RasterAccessor(alphaRaster, raRect,
                        alphaFormatTag, alphaChannelColorModels[i]));
            }

            helpAccessor.setRoiRaster(roiRasters[i]);
            helpAccessor.setSourceNoDataRange(noDataRanges[i]);

            sourceAccessorsArrayBean[i] = helpAccessor;
        }

        switch (destinationDataType) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(sourceAccessorsArrayBean, destinationAccessor, destRectangle);
            break;
        case DataBuffer.TYPE_USHORT:
            ushortLoop(sourceAccessorsArrayBean, destinationAccessor, destRectangle);
            break;
        case DataBuffer.TYPE_SHORT:
            shortLoop(sourceAccessorsArrayBean, destinationAccessor, destRectangle);
            break;
        case DataBuffer.TYPE_INT:
            intLoop(sourceAccessorsArrayBean, destinationAccessor, destRectangle);
            break;
        case DataBuffer.TYPE_FLOAT:
            floatLoop(sourceAccessorsArrayBean, destinationAccessor, destRectangle);
            break;
        case DataBuffer.TYPE_DOUBLE:
            doubleLoop(sourceAccessorsArrayBean, destinationAccessor, destRectangle);
            break;
        }
        // the data are copied back to the destination raster
        destinationAccessor.copyDataToRaster();

    }

    private void byteLoop(final RasterBeanAccessor[] srcBeans, final RasterAccessor dst, final Rectangle destBounds) {
        // Stores the source number
        final int sourcesNumber = srcBeans.length;

        // Get the pixel readers for source and alpha
        final PixelIteratorByte[] srcIterators = new PixelIteratorByte[sourcesNumber];
        final PixelIteratorByte[] alphaIterators = new PixelIteratorByte[sourcesNumber];

        // Destination data creation
        final byte[][] dstDataByte = dst.getByteDataArrays();

        // Check if the alpha is used in the selected raster.
        final boolean alphaPresentinRaster = hasAlpha(srcBeans, sourcesNumber);

        // Weight type arrays can have different weight types if ROI or alpha
        // channel are present or not
        final WeightType[] weightTypesUsed = new WeightType[sourcesNumber];

        // The above arrays are filled with the data from the Java Raster
        // AcessorBean.
        for (int i = 0; i < sourcesNumber; i++) {
            weightTypesUsed[i] = WeightType.WEIGHT_TYPE_NODATA;
            final RasterAccessor dataRA = srcBeans[i].getDataRasterAccessor();
            if (dataRA != null) {
                srcIterators[i] = new PixelIteratorByte(getSourceRect(srcBeans, destBounds, i), destBounds, dataRA);
                // Data retrieval
                final RasterAccessor alphaRA = srcBeans[i].getAlphaRasterAccessor();
                if (alphaPresentinRaster & alphaRA != null) {
                    alphaIterators[i] = new PixelIteratorByte(getSourceRect(srcBeans, destBounds, i), destBounds, alphaRA);
                }
                if (alphaRA != null) {
                    // If alpha channel is present alpha weight type is used
                    weightTypesUsed[i] = WeightType.WEIGHT_TYPE_ALPHA;
                } else if (roiPresent && srcBeans[i].getRoiRaster() != null) {
                    // Else if ROI is present, then roi weight type is used
                    weightTypesUsed[i] = WeightType.WEIGHT_TYPE_ROI;
                }
            }
        }

        // Destination information are taken from the destination accessor
        final int dstMinX = dst.getX();
        final int dstMinY = dst.getY();
        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstMaxX = dstMinX + dstWidth;
        final int dstMaxY = dstMinY + dstHeight;
        final int dstBands = dst.getNumBands();
        final int dstLineStride = dst.getScanlineStride();
        final int dstPixelStride = dst.getPixelStride();
        final int[] dstBandOffsets = dst.getBandOffsets();

        // In the blending operation the destination pixel value is
        // calculated as sum of the weighted source pixel / sum of weigth.
        final double[] numerator = new double[dstBands];
        final double[] denominator = new double[dstBands];

        // COMPUTATION LEVEL

        // The destination data band are selected
        final byte[][] dBandDataByteS = dstDataByte;
        // the destination lineOffset is initialized
        final int[] dLineOffsetS = new int[dstBands];
        final int[] dPixelOffsetS = new int[dstBands];
        for (int b = 0; b < dstBands; b++) {
            dLineOffsetS[b] = dstBandOffsets[b];
        }

        if (mosaicTypeSelected == MosaicDescriptor.MOSAIC_TYPE_OVERLAY) {
            for (int dstY = dstMinY; dstY < dstMaxY; dstY++) { // For all the Y
                // Update destination offsets
                for (int b = 0; b < dstBands; b++) {
                    dPixelOffsetS[b] = dLineOffsetS[b];
                    dLineOffsetS[b] += dstLineStride;
                }

                for (int dstX = dstMinX; dstX < dstMaxX; dstX++) { // For all the X values

                    // The destination flag is initialized to false and changes to true only
                    // if one pixel alpha channel is not 0 or falls into an image ROI or 
                    // is not a NoData
                    boolean setDestinationFlag = false;

                    for (int s = 0; s < sourcesNumber; s++) {
                        final RasterBeanAccessor srcBean = srcBeans[s];
                        final PixelIteratorByte srcReader = srcIterators[s];
                        if (srcReader == null) {
                            continue;
                        }
                        final PixelIteratorByte alphaIterator = alphaIterators[s]; 

                        if (!srcReader.hasData()) {
                            srcReader.nextPixel();
                            if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                                alphaIterator.nextPixel();
                            }
                            continue;
                        }
                        boolean skipPixel = false;
                        switch (weightTypesUsed[s]) {
                            case WEIGHT_TYPE_ALPHA:
                                skipPixel = alphaIterator.readOne() == 0;
                                alphaIterator.nextPixel();
                                break;
                            case WEIGHT_TYPE_ROI:
                                skipPixel = srcBean.getRoiRaster().getSample(dstX, dstY, 0) == 0;
                                break;
                        }
                        if (skipPixel) {
                            srcReader.nextPixel();
                            continue;
                        }

                        // The source values are initialized only for the switch method
                        setDestinationFlag = srcBean.getSourceNoDataRange() == null;
                        
                        byte[] pixel = srcReader.read();
                        for (int b = 0; b < dstBands; b++) {
                            byte value = pixel[b];
                            if (!setDestinationFlag && byteLookupTable[s][b][value & 0xFF]) {
                                setDestinationFlag = true;
                            }
                        }
                        srcReader.nextPixel();

                        // If the flag is True, the related source pixel is saved in the
                        // destination one and exit from the cycle after incrementing the offset
                        if (setDestinationFlag) {
                            for (int b = 0; b < dstBands; b++) {
                                dBandDataByteS[b][dPixelOffsetS[b]] = pixel[b];
                            }
                            skipRemainingReaders(s, sourcesNumber, srcIterators, alphaIterators);
                            break;
                        }
                    }
                    // If the flag is false for every source, the destination no data value is
                    // set to the related destination pixel and then updates the offset
                    for (int b = 0; b < dstBands; b++) {
                        if (!setDestinationFlag) {
                            dBandDataByteS[b][dPixelOffsetS[b]] = destinationNoDataByte[b];
                        }
                        dPixelOffsetS[b] += dstPixelStride;
                    }
                }
                // move sources to the next line
                moveToNextLine(sourcesNumber, srcIterators, alphaIterators);
            }
        } else { // the mosaicType is MOSAIC_TYPE_BLEND
            for (int dstY = dstMinY; dstY < dstMaxY; dstY++) {
                // Update destination offsets
                for (int b = 0; b < dstBands; b++) {
                    dPixelOffsetS[b] = dLineOffsetS[b];
                    dLineOffsetS[b] += dstLineStride;
                }
                for (int dstX = dstMinX; dstX < dstMaxX; dstX++) {
                    Arrays.fill(numerator, 0);
                    Arrays.fill(denominator, 0);

                    for (int s = 0; s < sourcesNumber; s++) {
                        final PixelIteratorByte srcReader = srcIterators[s];
                        if (srcReader == null) continue;
                        final PixelIteratorByte alphaIterator = alphaIterators[s];

                        if (!srcReader.hasData()) {
                            // just move forward the offsets
                            srcReader.nextPixel();
                            if (alphaIterator != null) alphaIterator.nextPixel();
                            continue;
                        }

                        // The source values are initialized only for the switch method
                        byte[] pixel = srcReader.read();
                        srcReader.nextPixel();

                        // The weight is calculated for every pixel
                        double weight = 0.0F;
                        int dataCount = dstBands;

                        // If no alpha channel or Roi is present, the weight
                        // is set to 1 or 0 if the pixel has or not a No Data value
                        if (srcBeans[s].getSourceNoDataRange()!=null) {
                            for (int b = 0; b < dstBands; b++) {
                                if (!byteLookupTable[s][b][pixel[b] & 0xFF]) {
                                    dataCount--;
                                }
                            }
                        }
                        if (dataCount == 0) {
                            weight = 0F;
                            if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                                alphaIterator.nextPixel();
                            }
                        } else {
                            switch (weightTypesUsed[s]) {
                            case WEIGHT_TYPE_ALPHA:
                                weight = alphaIterator.readOne();
                                if (weight > 0.0F && isAlphaBitmaskUsed) {
                                    weight = 1.0F;
                                } else {
                                    weight /= 255.0F;
                                }
                                alphaIterator.nextPixel();
                                break;
                            case WEIGHT_TYPE_ROI:
                                weight = srcBeans[s].getRoiRaster().getSample(dstX, dstY, 0) > 0
                                        ? 1.0F
                                        : 0.0F;
                                break;
                            default:
                                weight = 1.0F;
                            }
                        }
                        // The above calculated weight are added to the
                        // numerator and denominator
                        for (int b = 0; b < dstBands; b++) {
                            numerator[b] += (weight * (pixel[b] & 0xff));
                            denominator[b] += weight;
                        }
                    }

                    // If the weighted sum is 0 the destination pixel value
                    // takes the destination no data.
                    // If the sum is not 0 the value is added to the related destination pixel
                    double denominatorSum = 0;
                    for (int b = 0; b < dstBands; b++) {
                        denominatorSum += denominator[b];
                    }

                    for (int b = 0; b < dstBands; b++) {
                        if (denominatorSum == 0.0) {
                            dBandDataByteS[b][dPixelOffsetS[b]] = destinationNoDataByte[b];
                        } else {
                            dBandDataByteS[b][dPixelOffsetS[b]] = ImageUtil
                                    .clampRoundByte(numerator[b] / denominator[b]);
                        }
                        // Offset update
                        dPixelOffsetS[b] += dstPixelStride;
                    }
                    
                }
                moveToNextLine(sourcesNumber, srcIterators, alphaIterators);
            }
        }
    }

    private static void skipRemainingReaders(int start, int sourcesNumber, PixelIterator[] srcIterators,
                                             PixelIterator[] alphaIterators) {
        for (int k = start + 1; k < sourcesNumber; k++) {
            final PixelIterator srk = srcIterators[k];
            if (srk != null) srk.nextPixel();
            final PixelIterator ark = alphaIterators[k];
            if (ark != null) ark.nextPixel();
        }
    }

    private static void moveToNextLine(int sourcesNumber, PixelIterator[] srcReaders,
                                       PixelIterator[] alphaIterators) {
        for (int s = 0; s < sourcesNumber; s++) {
            final PixelIterator srcReader = srcReaders[s];
            if (srcReader != null) {
                srcReader.nextLine();
                // if there are no more pixels to process, skip all the pixel position processing
                if (srcReader.isDone()) {
                    srcReaders[s] = null;
                    alphaIterators[s] = null;
                } else {
                    final PixelIterator alphaIterator = alphaIterators[s];
                    if (alphaIterator != null) alphaIterator.nextLine();
                }
            }
        }
    }

    private static Rectangle getSourceRect(RasterBeanAccessor[] srcBeans, Rectangle destBounds, int i) {
        return srcBeans[i].getBounds() != null ? srcBeans[i].getBounds() : destBounds;
    }

    private boolean hasAlpha(RasterBeanAccessor[] srcBeans, int sourcesNumber) {
        for (int i = 0; i < sourcesNumber; i++) {
            if (srcBeans[i].getAlphaRasterAccessor() != null) {
                return true;
            }
        }
        return false;
    }

    private void ushortLoop(final RasterBeanAccessor[] srcBeans, final RasterAccessor dst, final Rectangle destBounds) {

        // Stores the source number
        final int sourcesNumber = srcBeans.length;

        // Get the pixel readers for source and alpha
        final PixelIteratorShort[] srcIterators = new PixelIteratorShort[sourcesNumber];
        final PixelIteratorShort[] alphaIterators = new PixelIteratorShort[sourcesNumber];

        // Destination data creation
        final short[][] dstDataUshort = dst.getShortDataArrays();

        // Check if the alpha is used in the selected raster.
        final boolean hasAlpha = hasAlpha(srcBeans, sourcesNumber);

        // Weight type arrays can have different weight types if ROI or alpha
        // channel are present or not
        final WeightType[] weightTypesUsed = new WeightType[sourcesNumber];

        // The above arrays are filled with the data from the Java Raster
        // AcessorBean.
        for (int i = 0; i < sourcesNumber; i++) {
            weightTypesUsed[i] = WeightType.WEIGHT_TYPE_NODATA;
            final RasterAccessor dataRA = srcBeans[i].getDataRasterAccessor();
            if (dataRA != null) {
                srcIterators[i] = new PixelIteratorShort(getSourceRect(srcBeans, destBounds, i), destBounds, dataRA);
                final RasterAccessor alphaRA = srcBeans[i].getAlphaRasterAccessor();
                if (hasAlpha & alphaRA != null) {
                    alphaIterators[i] = new PixelIteratorShort(getSourceRect(srcBeans, destBounds, i), destBounds, alphaRA);
                }
                if (alphaRA != null) {
                    // If alpha channel is present alpha weight type is used
                    weightTypesUsed[i] = WeightType.WEIGHT_TYPE_ALPHA;
                } else if (roiPresent && srcBeans[i].getRoiRaster() != null) {
                    // Else if ROI is present, then roi weight type is used
                    weightTypesUsed[i] = WeightType.WEIGHT_TYPE_ROI;
                }
            }
        }

        // Destination information are taken from the destination accessor
        final int dstMinX = dst.getX();
        final int dstMinY = dst.getY();
        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstMaxX = dstMinX + dstWidth;
        final int dstMaxY = dstMinY + dstHeight;
        final int dstBands = dst.getNumBands();
        final int dstLineStride = dst.getScanlineStride();
        final int dstPixelStride = dst.getPixelStride();
        final int[] dstBandOffsets = dst.getBandOffsets();

        // In the blending operation the destination pixel value is
        // calculated as sum of the weighted source pixel / sum of weigth.
        double[] numerator = new double[dstBands];
        double[] denominator = new double[dstBands];
        int[] sourceValueUshortS = new int[dstBands];

        // COMPUTATION LEVEL

        // The destination data band are selected
        short[][] dBandDataUshortS = dstDataUshort;
        // the destination lineOffset is initialized
        int[] dLineOffsetS = new int[dstBands];
        int[] dPixelOffsetS = new int[dstBands];
        for (int b = 0; b < dstBands; b++) {
            dLineOffsetS[b] = dstBandOffsets[b];
        }

        if (mosaicTypeSelected == MosaicDescriptor.MOSAIC_TYPE_OVERLAY) {
            for (int dstY = dstMinY; dstY < dstMaxY; dstY++) { // For all the Y
                
                // Update the destination offsets
                for (int b = 0; b < dstBands; b++) {
                    dPixelOffsetS[b] = dLineOffsetS[b];
                    dLineOffsetS[b] += dstLineStride;
                }

                for (int dstX = dstMinX; dstX < dstMaxX; dstX++) { // For all the X values

                    // The destination flag is initialized to false and changes to true only
                    // if one pixel alpha channel is not 0 or falls into an image ROI or 
                    // is not a NoData
                    boolean setDestinationFlag = false;

                    for (int s = 0; s < sourcesNumber; s++) {
                        final RasterBeanAccessor srcBean = srcBeans[s];
                        final PixelIteratorShort srcReader = srcIterators[s];
                        if (srcReader == null) {
                            continue;
                        }
                        final PixelIteratorShort alphaIterator = alphaIterators[s];

                        if (!srcReader.hasData()) {
                            srcReader.nextPixel();
                            if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                                alphaIterator.nextPixel();
                            }
                            continue;
                        }

                        boolean skipPixel = false;
                        switch (weightTypesUsed[s]) {
                            case WEIGHT_TYPE_ALPHA:
                                skipPixel = alphaIterator.readOne() == 0;
                                alphaIterator.nextPixel();

                                break;
                            case WEIGHT_TYPE_ROI:
                                skipPixel = srcBean.getRoiRaster().getSample(dstX, dstY, 0) == 0;
                                break;
                        }
                        if (skipPixel) {
                            srcReader.nextPixel();
                            continue;
                        }

                        // The source values are initialized only for the switch method
                        Range noDataRangeUShort = srcBean.getSourceNoDataRange();
                        setDestinationFlag = noDataRangeUShort == null;
                        short[] pixel = srcReader.read();
                        for (int b = 0; b < dstBands; b++) {
                            short value = pixel[b];
                            sourceValueUshortS[b] = (value & 0xffff);
                            if (!setDestinationFlag && !noDataRangeUShort.contains(sourceValueUshortS[b])) {
                                setDestinationFlag = true;
                            }
                        }
                        srcReader.nextPixel();

                        // If the flag is True, the related source pixel is saved in the
                        // destination one and exit from the cycle after incrementing the offset
                        if (setDestinationFlag) {
                            for (int b = 0; b < dstBands; b++) {
                                dBandDataUshortS[b][dPixelOffsetS[b]] = pixel[b];
                            }
                            skipRemainingReaders(s, sourcesNumber, srcIterators, alphaIterators);
                            break;
                        }
                    }
                    // If the flag is false for every source, the destination no data value is
                    // set to the related destination pixel and then updates the offset
                    for (int b = 0; b < dstBands; b++) {
                        if (!setDestinationFlag) {
                            dBandDataUshortS[b][dPixelOffsetS[b]] = destinationNoDataUShort[b];
                        }

                        dPixelOffsetS[b] += dstPixelStride;
                    }
                }

                moveToNextLine(sourcesNumber, srcIterators, alphaIterators);
            }
        } else { // the mosaicType is MOSAIC_TYPE_BLEND
            for (int dstY = dstMinY; dstY < dstMaxY; dstY++) {
                // Update the destination offsets
                for (int b = 0; b < dstBands; b++) {
                    dPixelOffsetS[b] = dLineOffsetS[b];
                    dLineOffsetS[b] += dstLineStride;
                }
                for (int dstX = dstMinX; dstX < dstMaxX; dstX++) {
                    Arrays.fill(numerator, 0);
                    Arrays.fill(denominator, 0);
                    
                    for (int s = 0; s < sourcesNumber; s++) {
                        final PixelIteratorShort srcReader = srcIterators[s];
                        if (srcReader == null) continue;
                        final PixelIteratorShort alphaIterator = alphaIterators[s];

                        if (!srcReader.hasData()) {
                            // just move forward the offsets
                            srcReader.nextPixel();
                            if (alphaIterator != null) alphaIterator.nextPixel();
                            continue;
                        }

                        // The source values are initialized only for the switch method
                        short[] pixel = srcReader.read();
                        for (int b = 0; b < dstBands; b++) {
                            sourceValueUshortS[b] = pixel[b];
                        }
                        srcReader.nextPixel();

                        // The weight is calculated for every pixel
                        double weight = 0.0F;
                        int dataCount = dstBands;

                        // If no alpha channel or Roi is present, the weight
                        // is set to 1 or 0 if the pixel has or not a No Data value
                        Range noDataRangeUShort = srcBeans[s].getSourceNoDataRange();
                        if (noDataRangeUShort != null) {
                            for (int b = 0; b < dstBands; b++) {
                                if (noDataRangeUShort.contains(sourceValueUshortS[b])) {
                                    dataCount--;
                                }
                            }
                        }
                        if (dataCount == 0) {
                            weight = 0F;
                            if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                                alphaIterator.nextPixel();
                            }
                        } else {
                            switch (weightTypesUsed[s]) {
                            case WEIGHT_TYPE_ALPHA:
                                weight = alphaIterator.readOne() & 0xff;
                                if (weight > 0.0F && isAlphaBitmaskUsed) {
                                    weight = 1.0F;
                                } else {
                                    weight /= 255.0F;
                                }
                                alphaIterator.nextPixel();
                                break;
                            case WEIGHT_TYPE_ROI:
                                weight = srcBeans[s].getRoiRaster().getSample(dstX, dstY, 0) > 0
                                        ? 1.0F
                                        : 0.0F;
                                break;
                            default:
                                weight = 1.0F;
                            }
                        }
                        // The above calculated weight are added to the
                        // numerator and denominator
                        for (int b = 0; b < dstBands; b++) {
                            numerator[b] += (weight * (sourceValueUshortS[b]));
                            denominator[b] += weight;
                        }
                    }

                    // If the weighted sum is 0 the destination pixel value
                    // takes the destination no data.
                    // If the sum is not 0 the value is added to the related
                    // destination pixel
                    double denominatorSum = 0;
                    for (int b = 0; b < dstBands; b++) {
                        denominatorSum += denominator[b];
                    }

                    for (int b = 0; b < dstBands; b++) {
                        if (denominatorSum == 0.0) {
                            dBandDataUshortS[b][dPixelOffsetS[b]] = destinationNoDataUShort[b];

                        } else {
                            dBandDataUshortS[b][dPixelOffsetS[b]] = ImageUtil
                                    .clampRoundUShort(numerator[b] / denominator[b]);
                        }
                        // Offset update
                        dPixelOffsetS[b] += dstPixelStride;
                    }
                }
                moveToNextLine(sourcesNumber, srcIterators, alphaIterators);
            }
        }
    }

    private void shortLoop(RasterBeanAccessor[] srcBeans, RasterAccessor dst, Rectangle destBounds) {

        // Stores the source number
        final int sourcesNumber = srcBeans.length;

        // Get the pixel readers for source and alpha
        final PixelIteratorShort[] srcIterators = new PixelIteratorShort[sourcesNumber];
        final PixelIteratorShort[] alphaIterators = new PixelIteratorShort[sourcesNumber];

        // Destination data creation
        final short[][] dstDataShort = dst.getShortDataArrays();

        // Check if the alpha is used in the selected raster.
        final boolean hasAlpha = hasAlpha(srcBeans, sourcesNumber);

        // Weight type arrays can have different weight types if ROI or alpha
        // channel are present or not
        final WeightType[] weightTypesUsed = new WeightType[sourcesNumber];

        // The above arrays are filled with the data from the Java Raster
        // AcessorBean.
        for (int i = 0; i < sourcesNumber; i++) {
            weightTypesUsed[i] = WeightType.WEIGHT_TYPE_NODATA;
            final RasterAccessor dataRA = srcBeans[i].getDataRasterAccessor();
            if (dataRA != null) {
                srcIterators[i] = new PixelIteratorShort(getSourceRect(srcBeans, destBounds, i), destBounds, dataRA);
                final RasterAccessor alphaRA = srcBeans[i].getAlphaRasterAccessor();
                if (hasAlpha & alphaRA != null) {
                    alphaIterators[i] = new PixelIteratorShort(getSourceRect(srcBeans, destBounds, i), destBounds, alphaRA);
                }

                if (alphaRA != null) {
                    // If alpha channel is present alpha weight type is used
                    weightTypesUsed[i] = WeightType.WEIGHT_TYPE_ALPHA;
                } else if (roiPresent && srcBeans[i].getRoiRaster() != null) {
                    // Else if ROI is present, then roi weight type is used
                    weightTypesUsed[i] = WeightType.WEIGHT_TYPE_ROI;
                }
            }
        }

        // Destination information are taken from the destination accessor
        final int dstMinX = dst.getX();
        final int dstMinY = dst.getY();
        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstMaxX = dstMinX + dstWidth;
        final int dstMaxY = dstMinY + dstHeight;
        final int dstBands = dst.getNumBands();
        final int dstLineStride = dst.getScanlineStride();
        final int dstPixelStride = dst.getPixelStride();
        final int[] dstBandOffsets = dst.getBandOffsets();
        // In the blending operation the destination pixel value is
        // calculated as sum of the weighted source pixel / sum of weigth.
        double[] numerator = new double[dstBands];
        double[] denominator = new double[dstBands];
        short[] sourceValueShortS = new short[dstBands];

        // COMPUTATION LEVEL
        
        // The destination data band are selected
        short[][] dBandDataShortS = dstDataShort;
        // the destination lineOffset is initialized
        int[] dLineOffsetS = new int[dstBands];
        int[] dPixelOffsetS = new int[dstBands];
        for (int b = 0; b < dstBands; b++) {
            dLineOffsetS[b] = dstBandOffsets[b];
        }

        if (mosaicTypeSelected == MosaicDescriptor.MOSAIC_TYPE_OVERLAY) {
            for (int dstY = dstMinY; dstY < dstMaxY; dstY++) { // For all the Y values
                // Update destination offsets
                for (int b = 0; b < dstBands; b++) {
                    dPixelOffsetS[b] = dLineOffsetS[b];
                    dLineOffsetS[b] += dstLineStride;
                }

                for (int dstX = dstMinX; dstX < dstMaxX; dstX++) { // For all the X values

                    // The destination flag is initialized to false and changes to true only
                    // if one pixel alpha channel is not 0 or falls into an image ROI or 
                    // is not a NoData
                    boolean setDestinationFlag = false;

                    for (int s = 0; s < sourcesNumber; s++) {
                        final RasterBeanAccessor srcBean = srcBeans[s];
                        final PixelIteratorShort srcReader = srcIterators[s];
                        if (srcReader == null) {
                            continue;
                        }
                        final PixelIteratorShort alphaIterator = alphaIterators[s];

                        if (!srcReader.hasData()) {
                            srcReader.nextPixel();
                            if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                                alphaIterator.nextPixel();
                            }
                            continue;
                        }

                        boolean skipPixel = false;
                        switch (weightTypesUsed[s]) {
                            case WEIGHT_TYPE_ALPHA:
                                skipPixel = alphaIterator.readOne() == 0;
                                alphaIterator.nextPixel();
                                break;
                            case WEIGHT_TYPE_ROI:
                                skipPixel = srcBean.getRoiRaster().getSample(dstX, dstY, 0) == 0;
                                break;
                        }
                        if (skipPixel) {
                            srcReader.nextPixel();
                            continue;
                        }

                        // Load source values and check nodata if needed
                        Range noDataRangeShort = srcBean.getSourceNoDataRange();
                        setDestinationFlag = noDataRangeShort == null;
                        short[] pixel = srcReader.read();
                        for (int b = 0; b < dstBands; b++) {
                            short value = pixel[b];
                            if (!setDestinationFlag && !noDataRangeShort.contains(value)) {
                                setDestinationFlag = true;
                            }
                            sourceValueShortS[b] = value;
                        }
                        srcReader.nextPixel();
                        
                        // If the flag is True, the related source pixel is saved in the
                        // destination one and exit from the cycle after incrementing the offset
                        if (setDestinationFlag) {
                            for (int b = 0; b < dstBands; b++) {
                                dBandDataShortS[b][dPixelOffsetS[b]] = sourceValueShortS[b];
                            }
                            skipRemainingReaders(s, sourcesNumber, srcIterators, alphaIterators);
                            break;
                        }
                    }
                    // If the flag is false for every source, the destination no data value is
                    // set to the related destination pixel and then updates the offset
                    for (int b = 0; b < dstBands; b++) {
                        if (!setDestinationFlag) {
                            dBandDataShortS[b][dPixelOffsetS[b]] = destinationNoDataShort[b];
                        }

                        dPixelOffsetS[b] += dstPixelStride;
                    }
                }
                moveToNextLine(sourcesNumber, srcIterators, alphaIterators);
            }
        } else { // the mosaicType is MOSAIC_TYPE_BLEND
            for (int dstY = dstMinY; dstY < dstMaxY; dstY++) {
                // Update the destination offsets
                for (int b = 0; b < dstBands; b++) {
                    dPixelOffsetS[b] = dLineOffsetS[b];
                    dLineOffsetS[b] += dstLineStride;
                }
                for (int dstX = dstMinX; dstX < dstMaxX; dstX++) {
                    Arrays.fill(numerator, 0);
                    Arrays.fill(denominator, 0);
                    
                    for (int s = 0; s < sourcesNumber; s++) {
                        final PixelIteratorShort srcIterator = srcIterators[s];
                        if (srcIterator == null) continue;
                        final PixelIteratorShort alphaIterator = alphaIterators[s];

                        if (!srcIterator.hasData()) {
                            // just move forward the offsets
                            srcIterator.nextPixel();
                            if (alphaIterator != null) alphaIterator.nextPixel();
                            continue;
                        }

                        // The source values are initialized only for the switch method
                        short[] pixel = srcIterator.read();
                        for (int b = 0; b < dstBands; b++) {
                            sourceValueShortS[b] = pixel[b];
                        }
                        srcIterator.nextPixel();

                        // The weight is calculated for every pixel
                        double weight = 0.0F;
                        int dataCount = dstBands;

                        // If no alpha channel or Roi is present, the weight
                        // is set to 1 or 0 if the pixel has or not a No Data value
                        Range noDataRangeShort = srcBeans[s].getSourceNoDataRange();
                        if (noDataRangeShort != null) {
                            for (int b = 0; b < dstBands; b++) {
                                if (noDataRangeShort.contains(sourceValueShortS[b])) {
                                    dataCount--;
                                }
                            }
                        }
                        if (dataCount == 0) {
                            weight = 0F;
                            if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                                alphaIterator.nextPixel();
                            }
                        } else {
                            switch (weightTypesUsed[s]) {
                            case WEIGHT_TYPE_ALPHA:
                                weight = alphaIterator.readOne() & 0xff;
                                if (weight > 0.0F && isAlphaBitmaskUsed) {
                                    weight = 1.0F;
                                } else {
                                    weight /= 255.0F;
                                }
                                alphaIterator.nextPixel();
                                break;
                            case WEIGHT_TYPE_ROI:
                                weight = srcBeans[s].getRoiRaster().getSample(dstX, dstY, 0) > 0
                                        ? 1.0F
                                        : 0.0F;
                                break;
                            default:
                                weight = 1.0F;
                            }
                        }
                        // The above calculated weight are added to the
                        // numerator and denominator
                        for (int b = 0; b < dstBands; b++) {
                            numerator[b] += (weight * (sourceValueShortS[b]));
                            denominator[b] += weight;
                        }
                    }

                    // If the weighted sum is 0 the destination pixel value
                    // takes the destination no data.
                    // If the sum is not 0 the value is added to the related
                    // destination pixel
                    double denominatorSum = 0;
                    for (int b = 0; b < dstBands; b++) {
                        denominatorSum += denominator[b];
                    }

                    for (int b = 0; b < dstBands; b++) {
                        if (denominatorSum == 0.0) {
                            dBandDataShortS[b][dPixelOffsetS[b]] = destinationNoDataShort[b];

                        } else {
                            dBandDataShortS[b][dPixelOffsetS[b]] = ImageUtil
                                    .clampRoundShort(numerator[b] / denominator[b]);
                        }
                        // Offset update
                        dPixelOffsetS[b] += dstPixelStride;
                    }
                }
                moveToNextLine(sourcesNumber, srcIterators, alphaIterators);
            }
        }
    }

    private void intLoop(RasterBeanAccessor[] srcBeans, RasterAccessor dst, Rectangle destBounds) {

        // Stores the source number
        final int sourcesNumber = srcBeans.length;

        // Get the pixel readers for source and alpha
        final PixelIteratorInt[] srcIterators = new PixelIteratorInt[sourcesNumber];
        final PixelIteratorInt[] alphaIterators = new PixelIteratorInt[sourcesNumber];

        // Destination data creation
        final int[][] dstDataInt = dst.getIntDataArrays();

        // Check if the alpha is used in the selected raster.
        final boolean hasAlpha = hasAlpha(srcBeans, sourcesNumber);

        // Weight type arrays can have different weight types if ROI or alpha
        // channel are present or not
        final WeightType[] weightTypesUsed = new WeightType[sourcesNumber];

        // The above arrays are filled with the data from the Java Raster
        // AcessorBean.
        for (int i = 0; i < sourcesNumber; i++) {
            weightTypesUsed[i] = WeightType.WEIGHT_TYPE_NODATA;
            final RasterAccessor dataRA = srcBeans[i].getDataRasterAccessor();
            if (dataRA != null) {
                srcIterators[i] = new PixelIteratorInt(getSourceRect(srcBeans, destBounds, i), destBounds, dataRA);
                final RasterAccessor alphaRA = srcBeans[i].getAlphaRasterAccessor();
                if (hasAlpha & alphaRA != null) {
                    alphaIterators[i] = new PixelIteratorInt(getSourceRect(srcBeans, destBounds, i), destBounds, alphaRA);
                }

                if (alphaRA != null) {
                    // If alpha channel is present alpha weight type is used
                    weightTypesUsed[i] = WeightType.WEIGHT_TYPE_ALPHA;
                } else if (roiPresent && srcBeans[i].getRoiRaster() != null) {
                    // Else if ROI is present, then roi weight type is used
                    weightTypesUsed[i] = WeightType.WEIGHT_TYPE_ROI;
                }
            }
        }

        // Destination information are taken from the destination accessor
        final int dstMinX = dst.getX();
        final int dstMinY = dst.getY();
        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstMaxX = dstMinX + dstWidth;
        final int dstMaxY = dstMinY + dstHeight;
        final int dstBands = dst.getNumBands();
        final int dstLineStride = dst.getScanlineStride();
        final int dstPixelStride = dst.getPixelStride();
        final int[] dstBandOffsets = dst.getBandOffsets();
        // In the blending operation the destination pixel value is
        // calculated as sum of the weighted source pixel / sum of weigth.
        double[] numerator = new double[dstBands];
        double[] denominator = new double[dstBands];


        // COMPUTATION LEVEL

        // The destination data band are selected
        int[][] dBandDataIntS = dstDataInt;
        // the destination lineOffset is initialized
        int[] dLineOffsetS = new int[dstBands];
        int[] dPixelOffsetS = new int[dstBands];
        for (int b = 0; b < dstBands; b++) {
            dLineOffsetS[b] = dstBandOffsets[b];
        }

        if (mosaicTypeSelected == MosaicDescriptor.MOSAIC_TYPE_OVERLAY) {
            for (int dstY = dstMinY; dstY < dstMaxY; dstY++) { // For all the Y
                                                               // values
                // Update destination offsets
                for (int b = 0; b < dstBands; b++) {
                    dPixelOffsetS[b] = dLineOffsetS[b];
                    dLineOffsetS[b] += dstLineStride;
                }

                for (int dstX = dstMinX; dstX < dstMaxX; dstX++) { // For all the X values

                    // The destination flag is initialized to false and changes to true only
                    // if one pixel alpha channel is not 0 or falls into an image ROI or 
                    // is not a NoData
                    boolean setDestinationFlag = false;

                    for (int s = 0; s < sourcesNumber; s++) {
                        final RasterBeanAccessor srcBean = srcBeans[s];
                        final PixelIteratorInt srcIterator = srcIterators[s];
                        if (srcIterator == null) {
                            continue;
                        }
                        final PixelIteratorInt alphaIterator = alphaIterators[s];

                        if (!srcIterator.hasData()) {
                            srcIterator.nextPixel();
                            if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                                alphaIterator.nextPixel();
                            }
                            continue;
                        }

                        boolean skipPixel = false;
                        switch (weightTypesUsed[s]) {
                            case WEIGHT_TYPE_ALPHA:
                                skipPixel = alphaIterator.readOne() == 0;
                                alphaIterator.nextPixel();

                                break;
                            case WEIGHT_TYPE_ROI:
                                skipPixel = srcBean.getRoiRaster().getSample(dstX, dstY, 0) == 0;
                                break;
                        }
                        if (skipPixel) {
                            srcIterator.nextPixel();
                            continue;
                        }

                        // Load source values and check nodata if needed
                        Range noDataRangeInt = srcBean.getSourceNoDataRange();
                        setDestinationFlag = noDataRangeInt == null;
                        int[] pixel = srcIterator.read();
                        for (int b = 0; b < dstBands; b++) {
                            int value = pixel[b];
                            if (!setDestinationFlag && !noDataRangeInt.contains(value)) {
                                setDestinationFlag = true;
                            }
                        }
                        srcIterator.nextPixel();

                        // If the flag is True, the related source pixel is saved in the
                        // destination one and exit from the cycle after incrementing the offset
                        if (setDestinationFlag) {
                            for (int b = 0; b < dstBands; b++) {
                                dBandDataIntS[b][dPixelOffsetS[b]] = pixel[b];
                            }
                            skipRemainingReaders(s, sourcesNumber, srcIterators, alphaIterators);
                            break;
                        }
                    }
                    // If the flag is false for every source, the destination no data value is
                    // set to the related destination pixel and then updates the offset
                    for (int b = 0; b < dstBands; b++) {
                        if (!setDestinationFlag) {
                            dBandDataIntS[b][dPixelOffsetS[b]] = destinationNoDataInt[b];
                        }

                        dPixelOffsetS[b] += dstPixelStride;
                    }
                }
                moveToNextLine(sourcesNumber, srcIterators, alphaIterators);
            }
        } else { // the mosaicType is MOSAIC_TYPE_BLEND
            for (int dstY = dstMinY; dstY < dstMaxY; dstY++) {
                // Update the destination offsets
                for (int b = 0; b < dstBands; b++) {
                    dPixelOffsetS[b] = dLineOffsetS[b];
                    dLineOffsetS[b] += dstLineStride;
                }
                for (int dstX = dstMinX; dstX < dstMaxX; dstX++) {
                    Arrays.fill(numerator, 0);
                    Arrays.fill(denominator, 0);
                    
                    for (int s = 0; s < sourcesNumber; s++) {
                        final PixelIteratorInt srcIterator = srcIterators[s];
                        if (srcIterator == null) continue;
                        final PixelIteratorInt alphaIterator = alphaIterators[s];

                        if (!srcIterator.hasData()) {
                            // just move forward the offsets
                            srcIterator.nextPixel();
                            if (alphaIterator != null) alphaIterator.nextPixel();
                            continue;
                        }

                        // The source values are initialized only for the switch method
                        int[] pixel = srcIterator.read();
                        srcIterator.nextPixel();

                        // The weight is calculated for every pixel
                        double weight = 0.0F;
                        int dataCount = dstBands;

                        // If no alpha channel or Roi is present, the weight
                        // is set to 1 or 0 if the pixel has or not a No Data value
                        Range noDataRangeInt = srcBeans[s].getSourceNoDataRange();
                        if (noDataRangeInt != null) {
                            for (int b = 0; b < dstBands; b++) {
                                if (noDataRangeInt.contains(pixel[b])) {
                                    dataCount--;
                                }
                            }
                        }
                        if (dataCount == 0) {
                            weight = 0F;
                            if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                                alphaIterator.nextPixel();
                            }
                        } else {
                            switch (weightTypesUsed[s]) {
                            case WEIGHT_TYPE_ALPHA:
                                weight = alphaIterator.readOne() & 0xff;
                                if (weight > 0.0F && isAlphaBitmaskUsed) {
                                    weight = 1.0F;
                                } else {
                                    weight /= 255.0F;
                                }
                                alphaIterator.nextPixel();
                                break;
                            case WEIGHT_TYPE_ROI:
                                weight = srcBeans[s].getRoiRaster().getSample(dstX, dstY, 0) > 0
                                        ? 1.0F
                                        : 0.0F;
                                break;
                            default:
                                weight = 1.0F;
                            }
                        }
                        // The above calculated weight are added to the
                        // numerator and denominator
                        for (int b = 0; b < dstBands; b++) {
                            numerator[b] += (weight * (pixel[b]));
                            denominator[b] += weight;
                        }
                    }

                    // If the weighted sum is 0 the destination pixel value
                    // takes the destination no data.
                    // If the sum is not 0 the value is added to the related
                    // destination pixel
                    double denominatorSum = 0;
                    for (int b = 0; b < dstBands; b++) {
                        denominatorSum += denominator[b];
                    }

                    for (int b = 0; b < dstBands; b++) {
                        if (denominatorSum == 0.0) {
                            dBandDataIntS[b][dPixelOffsetS[b]] = destinationNoDataInt[b];

                        } else {
                            dBandDataIntS[b][dPixelOffsetS[b]] = ImageUtil
                                    .clampRoundInt(numerator[b] / denominator[b]);
                        }
                        // Offset update
                        dPixelOffsetS[b] += dstPixelStride;
                    }
                }
                moveToNextLine(sourcesNumber, srcIterators, alphaIterators);
            }
        }
    }

    private void floatLoop(RasterBeanAccessor[] srcBeans, RasterAccessor dst, Rectangle destBounds) {

        // Stores the source number
        final int sourcesNumber = srcBeans.length;

        // Get the pixel readers for source and alpha
        final PixelIteratorFloat[] srcIterators = new PixelIteratorFloat[sourcesNumber];
        final PixelIteratorFloat[] alphaIterators = new PixelIteratorFloat[sourcesNumber];

        // Destination data creation
        final float[][] dstDataFloat = dst.getFloatDataArrays();

        // Check if the alpha is used in the selected raster.
        final boolean hasAlpha = hasAlpha(srcBeans, sourcesNumber);

        // Weight type arrays can have different weight types if ROI or alpha
        // channel are present or not
        final WeightType[] weightTypesUsed = new WeightType[sourcesNumber];

        // The above arrays are filled with the data from the Java Raster
        // AcessorBean.
        for (int i = 0; i < sourcesNumber; i++) {
            weightTypesUsed[i] = WeightType.WEIGHT_TYPE_NODATA;
            final RasterAccessor dataRA = srcBeans[i].getDataRasterAccessor();
            if (dataRA != null) {
                srcIterators[i] = new PixelIteratorFloat(getSourceRect(srcBeans, destBounds, i), destBounds, dataRA);
                final RasterAccessor alphaRA = srcBeans[i].getAlphaRasterAccessor();
                if (hasAlpha & alphaRA != null) {
                    alphaIterators[i] = new PixelIteratorFloat(getSourceRect(srcBeans, destBounds, i), destBounds, alphaRA);
                }
                if (alphaRA != null) {
                    // If alpha channel is present alpha weight type is used
                    weightTypesUsed[i] = WeightType.WEIGHT_TYPE_ALPHA;
                } else if (roiPresent && srcBeans[i].getRoiRaster() != null) {
                    // Else if ROI is present, then roi weight type is used
                    weightTypesUsed[i] = WeightType.WEIGHT_TYPE_ROI;
                }
            }
        }

        // Destination information are taken from the destination accessor
        final int dstMinX = dst.getX();
        final int dstMinY = dst.getY();
        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstMaxX = dstMinX + dstWidth;
        final int dstMaxY = dstMinY + dstHeight;
        final int dstBands = dst.getNumBands();
        final int dstLineStride = dst.getScanlineStride();
        final int dstPixelStride = dst.getPixelStride();
        final int[] dstBandOffsets = dst.getBandOffsets();
        // In the blending operation the destination pixel value is
        // calculated as sum of the weighted source pixel / sum of weigth.
        double[] numerator = new double[dstBands];
        double[] denominator = new double[dstBands];

        // COMPUTATION LEVEL
        // The destination data band are selected
        float[][] dBandDataFloatS = dstDataFloat;
        // the destination lineOffset is initialized
        int[] dLineOffsetS = new int[dstBands];
        int[] dPixelOffsetS = new int[dstBands];
        for (int b = 0; b < dstBands; b++) {
            dLineOffsetS[b] = dstBandOffsets[b];
        }

        if (mosaicTypeSelected == MosaicDescriptor.MOSAIC_TYPE_OVERLAY) {
            for (int dstY = dstMinY; dstY < dstMaxY; dstY++) { // For all the Y values

                // Update destination offsets
                for (int b = 0; b < dstBands; b++) {
                    dPixelOffsetS[b] = dLineOffsetS[b];
                    dLineOffsetS[b] += dstLineStride;
                }

                for (int dstX = dstMinX; dstX < dstMaxX; dstX++) { // For all the X values

                    // The destination flag is initialized to false and changes to true only
                    // if one pixel alpha channel is not 0 or falls into an image ROI or 
                    // is not a NoData
                    boolean setDestinationFlag = false;
                    for (int s = 0; s < sourcesNumber; s++) {
                        final RasterBeanAccessor srcBean = srcBeans[s];
                        final PixelIteratorFloat srcIterator = srcIterators[s];
                        if (srcIterator == null) {
                            continue;
                        }
                        final PixelIteratorFloat alphaIterator = alphaIterators[s];
                        
                        if (!srcIterator.hasData()) {
                            srcIterator.nextPixel();
                            if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                                alphaIterator.nextPixel();
                            }
                            continue;
                        }

                        boolean skipPixel = false;
                        switch (weightTypesUsed[s]) {
                            case WEIGHT_TYPE_ALPHA:
                                skipPixel = alphaIterator.readOne() == 0;
                                alphaIterator.nextPixel();
                                break;
                            case WEIGHT_TYPE_ROI:
                                skipPixel = srcBean.getRoiRaster().getSample(dstX, dstY, 0) == 0;
                                break;
                        }
                        if (skipPixel) {
                            srcIterator.nextPixel();
                            continue;
                        }

                        // Load source values and check nodata if needed
                        Range noDataRangeFloat = srcBean.getSourceNoDataRange();
                        setDestinationFlag = noDataRangeFloat == null;
                        float[] pixel = srcIterator.read();
                        for (int b = 0; b < dstBands; b++) {
                            float value = pixel[b];
                            if (!setDestinationFlag && !noDataRangeFloat.contains(value)) {
                                setDestinationFlag = true;
                            }
                        }
                        srcIterator.nextPixel();
                        
                        
                        // If the flag is True, the related source pixel is saved in the
                        // destination one and exit from the cycle after incrementing the offset
                        if (setDestinationFlag) {
                            for (int b = 0; b < dstBands; b++) {
                                dBandDataFloatS[b][dPixelOffsetS[b]] = pixel[b];
                            }
                            skipRemainingReaders(s, sourcesNumber, srcIterators, alphaIterators);
                            break;
                        }
                    }
                    // If the flag is false for every source, the destination no data value is
                    // set to the related destination pixel and then updates the offset
                    for (int b = 0; b < dstBands; b++) {
                        if (!setDestinationFlag) {
                            dBandDataFloatS[b][dPixelOffsetS[b]] = destinationNoDataFloat[b];
                        }

                        dPixelOffsetS[b] += dstPixelStride;
                    }
                }
                moveToNextLine(sourcesNumber, srcIterators, alphaIterators);
            }
        } else { // the mosaicType is MOSAIC_TYPE_BLEND
            for (int dstY = dstMinY; dstY < dstMaxY; dstY++) {
                // Update destination offsets
                for (int b = 0; b < dstBands; b++) {
                    dPixelOffsetS[b] = dLineOffsetS[b];
                    dLineOffsetS[b] += dstLineStride;
                }
                for (int dstX = dstMinX; dstX < dstMaxX; dstX++) {
                    Arrays.fill(numerator, 0);
                    Arrays.fill(denominator, 0);

                    for (int s = 0; s < sourcesNumber; s++) {
                        final PixelIteratorFloat srcIterator = srcIterators[s];
                        if (srcIterator == null) continue;
                        final PixelIteratorFloat alphaIterator = alphaIterators[s];

                        if (!srcIterator.hasData()) {
                            // just move forward the offsets
                            srcIterator.nextPixel();
                            if (alphaIterator != null) alphaIterator.nextPixel();
                            continue;
                        }


                        // The source values are initialized only for the switch method
                        float[] pixel = srcIterator.read();
                        srcIterator.nextPixel();
                        
                        // The weight is calculated for every pixel
                        double weight = 0.0F;
                        int dataCount = dstBands;

                        // If no alpha channel or Roi is present, the weight
                        // is set to 1 or 0 if the pixel has or not a No Data value
                        Range noDataRangeFloat = srcBeans[s].getSourceNoDataRange();
                        if (noDataRangeFloat != null) {
                            for (int b = 0; b < dstBands; b++) {
                                if (noDataRangeFloat.contains(pixel[b])) {
                                    dataCount--;
                                }
                            }
                        }
                        if (dataCount == 0) {
                            weight = 0F;
                            if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                                alphaIterator.nextPixel();
                            }
                        } else {
                            switch (weightTypesUsed[s]) {
                            case WEIGHT_TYPE_ALPHA:
                                weight = alphaIterator.readOne();
                                if (weight > 0.0F && isAlphaBitmaskUsed) {
                                    weight = 1.0F;
                                } else {
                                    weight /= 255.0F;
                                }
                                alphaIterator.nextPixel();
                                break;
                            case WEIGHT_TYPE_ROI:
                                weight = srcBeans[s].getRoiRaster().getSample(dstX, dstY, 0) > 0
                                        ? 1.0F
                                        : 0.0F;
                                break;
                            default:
                                weight = 1.0F;
                            }
                        }
                        // The above calculated weight are added to the
                        // numerator and denominator
                        for (int b = 0; b < dstBands; b++) {
                            numerator[b] += (weight > 0.0F ? (weight * (pixel[b])) : 0);
                            denominator[b] += weight;
                        }
                    }

                    // If the weighted sum is 0 the destination pixel value
                    // takes the destination no data.
                    // If the sum is not 0 the value is added to the related
                    // destination pixel
                    double denominatorSum = 0;
                    for (int b = 0; b < dstBands; b++) {
                        denominatorSum += denominator[b];
                    }

                    for (int b = 0; b < dstBands; b++) {
                        if (denominatorSum == 0.0) {
                            dBandDataFloatS[b][dPixelOffsetS[b]] = destinationNoDataFloat[b];

                        } else {
                            dBandDataFloatS[b][dPixelOffsetS[b]] = ImageUtil
                                    .clampFloat(numerator[b] / denominator[b]);
                        }
                        // Offset update
                        dPixelOffsetS[b] += dstPixelStride;
                    }
                }
                moveToNextLine(sourcesNumber, srcIterators, alphaIterators);
            }
        }
    }

    private void doubleLoop(RasterBeanAccessor[] srcBeans, RasterAccessor dst, Rectangle destBounds) {

        // Stores the source number
        final int sourcesNumber = srcBeans.length;

        // Get the pixel readers for source and alpha
        final PixelIteratorDouble[] srcIterators = new PixelIteratorDouble[sourcesNumber];
        final PixelIteratorDouble[] alphaIterators = new PixelIteratorDouble[sourcesNumber];

        // Destination data creation
        final double[][] dstDataDouble = dst.getDoubleDataArrays();
        
        // Check if the alpha is used in the selected raster.
        final boolean hasAlpha = hasAlpha(srcBeans, sourcesNumber);

        // Weight type arrays can have different weight types if ROI or alpha
        // channel are present or not
        final WeightType[] weightTypesUsed = new WeightType[sourcesNumber];

        // The above arrays are filled with the data from the Java Raster
        // AcessorBean.
        for (int i = 0; i < sourcesNumber; i++) {
            weightTypesUsed[i] = WeightType.WEIGHT_TYPE_NODATA;
            final RasterAccessor dataRA = srcBeans[i].getDataRasterAccessor();
            if (dataRA != null) {
                srcIterators[i] = new PixelIteratorDouble(getSourceRect(srcBeans, destBounds, i), destBounds, dataRA);
                final RasterAccessor alphaRA = srcBeans[i].getAlphaRasterAccessor();
                if (hasAlpha & alphaRA != null) {
                    alphaIterators[i] = new PixelIteratorDouble(getSourceRect(srcBeans, destBounds, i), destBounds, alphaRA);
                }
                if (alphaRA != null) {
                    // If alpha channel is present alpha weight type is used
                    weightTypesUsed[i] = WeightType.WEIGHT_TYPE_ALPHA;
                } else if (roiPresent && srcBeans[i].getRoiRaster() != null) {
                    // Else if ROI is present, then roi weight type is used
                    weightTypesUsed[i] = WeightType.WEIGHT_TYPE_ROI;
                }
            }
        }

        // Destination information are taken from the destination accessor
        final int dstMinX = dst.getX();
        final int dstMinY = dst.getY();
        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int dstMaxX = dstMinX + dstWidth;
        final int dstMaxY = dstMinY + dstHeight;
        final int dstBands = dst.getNumBands();
        final int dstLineStride = dst.getScanlineStride();
        final int dstPixelStride = dst.getPixelStride();
        final int[] dstBandOffsets = dst.getBandOffsets();
        // In the blending operation the destination pixel value is
        // calculated as sum of the weighted source pixel / sum of weigth.
        double[] numerator = new double[dstBands];
        double[] denominator = new double[dstBands];

        // COMPUTATION LEVEL
        // The destination data band are selected
        double[][] dBandDataDoubleS = dstDataDouble;
        // the destination lineOffset is initialized
        int[] dLineOffsetS = new int[dstBands];
        int[] dPixelOffsetS = new int[dstBands];
        for (int b = 0; b < dstBands; b++) {
            dLineOffsetS[b] = dstBandOffsets[b];
        }

        if (mosaicTypeSelected == MosaicDescriptor.MOSAIC_TYPE_OVERLAY) {
            for (int dstY = dstMinY; dstY < dstMaxY; dstY++) { // For all the Y
                // Update destination offsets
                for (int b = 0; b < dstBands; b++) {
                    dPixelOffsetS[b] = dLineOffsetS[b];
                    dLineOffsetS[b] += dstLineStride;
                }

                for (int dstX = dstMinX; dstX < dstMaxX; dstX++) { // For all the X values

                    // The destination flag is initialized to false and changes to true only
                    // if one pixel alpha channel is not 0 or falls into an image ROI or 
                    // is not a NoData
                    boolean setDestinationFlag = false;

                    for (int s = 0; s < sourcesNumber; s++) {
                        final RasterBeanAccessor srcBean = srcBeans[s];
                        final PixelIteratorDouble srcIterator = srcIterators[s];
                        if (srcIterator == null) {
                            continue;
                        }
                        final PixelIteratorDouble alphaIterator = alphaIterators[s];

                        if (!srcIterator.hasData()) {
                            srcIterator.nextPixel();
                            if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                                alphaIterator.nextPixel();
                            }
                            continue;
                        }

                        boolean skipPixel = false;
                        switch (weightTypesUsed[s]) {
                            case WEIGHT_TYPE_ALPHA:
                                skipPixel = alphaIterator.readOne() == 0d;
                                alphaIterator.nextPixel();
                                break;
                            case WEIGHT_TYPE_ROI:
                                skipPixel = srcBean.getRoiRaster().getSample(dstX, dstY, 0) == 0;
                                break;
                        }
                        if (skipPixel) {
                            srcIterator.nextPixel();
                            continue;
                        }

                        // Load source values and check nodata if needed
                        Range noDataRangeInt = srcBean.getSourceNoDataRange();
                        setDestinationFlag = noDataRangeInt == null;
                        double[] pixel = srcIterator.read();
                        for (int b = 0; b < dstBands; b++) {
                            double value = pixel[b];
                            if (!setDestinationFlag && !noDataRangeInt.contains(value)) {
                                setDestinationFlag = true;
                            }
                        }
                        srcIterator.nextPixel();
                        
                        // If the flag is True, the related source pixel is saved in the
                        // destination one and exit from the cycle after incrementing the offset
                        if (setDestinationFlag) {
                            for (int b = 0; b < dstBands; b++) {
                                dBandDataDoubleS[b][dPixelOffsetS[b]] = pixel[b];
                            }
                            skipRemainingReaders(s, sourcesNumber, srcIterators, alphaIterators);
                            break;
                        }
                    }
                    // If the flag is false for every source, the destination no data value is
                    // set to the related destination pixel and then updates the offset
                    for (int b = 0; b < dstBands; b++) {
                        if (!setDestinationFlag) {
                            dBandDataDoubleS[b][dPixelOffsetS[b]] = destinationNoDataDouble[b];
                        }

                        dPixelOffsetS[b] += dstPixelStride;
                    }
                }
                moveToNextLine(sourcesNumber, srcIterators, alphaIterators);
            }
        } else { // the mosaicType is MOSAIC_TYPE_BLEND
            for (int dstY = dstMinY; dstY < dstMaxY; dstY++) {
                // Updatedestination offsets
                for (int b = 0; b < dstBands; b++) {
                    dPixelOffsetS[b] = dLineOffsetS[b];
                    dLineOffsetS[b] += dstLineStride;
                }
                for (int dstX = dstMinX; dstX < dstMaxX; dstX++) {
                    Arrays.fill(numerator, 0);
                    Arrays.fill(denominator, 0);                    
                    
                    for (int s = 0; s < sourcesNumber; s++) {
                        final PixelIteratorDouble srcIterator = srcIterators[s];
                        if (srcIterator == null) continue;
                        final PixelIteratorDouble alphaIterator = alphaIterators[s];

                        if (!srcIterator.hasData()) {
                            // just move forward the offsets
                            srcIterator.nextPixel();
                            if (alphaIterator != null) alphaIterator.nextPixel();
                            continue;
                        }

                        // The source values are initialized only for the switch method
                        double[] pixel = srcIterator.read();
                        srcIterator.nextPixel();

                        // The weight is calculated for every pixel
                        double weight = 0.0F;
                        int dataCount = dstBands;

                        // If no alpha channel or Roi is present, the weight
                        // is set to 1 or 0 if the pixel has or not a No Data value
                        Range noDataRangeDouble = srcBeans[s].getSourceNoDataRange();
                        if (noDataRangeDouble != null) {
                            for (int b = 0; b < dstBands; b++) {
                                if (noDataRangeDouble.contains(pixel[b])) {
                                    dataCount--;
                                }
                            }
                        }
                        if (dataCount == 0) {
                            weight = 0F;
                            if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                                alphaIterator.nextPixel();
                            }
                        } else {
                            switch (weightTypesUsed[s]) {
                            case WEIGHT_TYPE_ALPHA:
                                weight = alphaIterator.readOne();
                                if (weight > 0.0F && isAlphaBitmaskUsed) {
                                    weight = 1.0F;
                                } else {
                                    weight /= 255.0F;
                                }
                                alphaIterator.nextPixel();
                                break;
                            case WEIGHT_TYPE_ROI:
                                weight = srcBeans[s].getRoiRaster().getSample(dstX, dstY, 0) > 0
                                        ? 1.0F
                                        : 0.0F;
                                break;
                            default:
                                weight = 1.0F;
                            }
                        }
                        // The above calculated weight are added to the
                        // numerator and denominator
                        for (int b = 0; b < dstBands; b++) {
                            numerator[b] += (weight > 0.0F ? (weight * (pixel[b])) : 0);
                            denominator[b] += weight;
                        }
                    }

                    // If the weighted sum is 0 the destination pixel value
                    // takes the destination no data.
                    // If the sum is not 0 the value is added to the related
                    // destination pixel
                    double denominatorSum = 0;
                    for (int b = 0; b < dstBands; b++) {
                        denominatorSum += denominator[b];
                    }

                    for (int b = 0; b < dstBands; b++) {
                        if (denominatorSum == 0.0) {
                            dBandDataDoubleS[b][dPixelOffsetS[b]] = destinationNoDataDouble[b];
                        } else {
                            dBandDataDoubleS[b][dPixelOffsetS[b]] = (numerator[b] / denominator[b]);
                        }
                        // Offset update
                        dPixelOffsetS[b] += dstPixelStride;

                    }
                }
                moveToNextLine(sourcesNumber, srcIterators, alphaIterators);
            }
        }
    }

    private void skipPixel(int dstBands, int[] srcPixelStride, int s, int[] sPixelOffsets) {
        // just move forward the offsets
        for (int b = 0; b < dstBands; b++) {
            // Offset update
            sPixelOffsets[b] += srcPixelStride[s];
        }
    }

    // These methods simplyoverride the OpImage mapDestRect and mapSourceRect method
    @Override
    public Rectangle mapDestRect(Rectangle destRectangle, int sourceRasterIndex) {
        if (destRectangle == null) {
            throw new IllegalArgumentException("Destination rectangle is not defined");
        }

        if (sourceRasterIndex < 0 || sourceRasterIndex >= getNumSources()) {
            throw new IllegalArgumentException(
                    "Source index must be between 0 and source dimension-1");
        }

        return destRectangle.intersection(getSourceImage(sourceRasterIndex).getBounds());
    }

    @Override
    public Rectangle mapSourceRect(Rectangle sourceRectangle, int sourceRasterIndex) {
        if (sourceRectangle == null) {
            throw new IllegalArgumentException("Destination rectangle is not defined");
        }

        if (sourceRasterIndex < 0 || sourceRasterIndex >= getNumSources()) {
            throw new IllegalArgumentException(
                    "Source index must be between 0 and source dimension-1");
        }

        return sourceRectangle.intersection(getBounds());

    }
    
    @Override
    public synchronized void dispose() {
        if(imageBeans != null) {
            // each of these might have been extended, make sure 
            // to dispose all of them (the super.dispose() will dispose
            // the sources, which eventually will re-dispose some images,
            // but that should be fine)
            for (ImageMosaicBean bean : imageBeans) {
                dispose(bean.getImage());
                dispose(bean.getRoiImage());
                dispose(bean.getAlphaChannel());
            }
        }
        super.dispose();
    }

    private void dispose(RenderedImage image) {
        if(image instanceof RenderedOp) {
            ((RenderedOp) image).dispose();
        }
    }

    /** Java bean for saving all the rasterAccessor informations */
    private static class RasterBeanAccessor {
        
        
        
        // RasterAccessor of image data
        private RasterAccessor dataRasterAccessor;

        // alpha rasterAccessor data
        private RasterAccessor alphaRasterAccessor;

        // Roi raster data
        private Raster roiRaster;

        // No data range
        private Range sourceNoDataRange;
        private Rectangle bounds;

        // No-argument constructor as requested for the java beans
        RasterBeanAccessor() {
        }

        // The methods below are setter and getter for every field as requested for the
        // java beans
        public RasterAccessor getDataRasterAccessor() {
            return dataRasterAccessor;
        }

        public void setDataRasterAccessor(RasterAccessor dataRasterAccessor) {
            this.dataRasterAccessor = dataRasterAccessor;
        }

        public RasterAccessor getAlphaRasterAccessor() {
            return alphaRasterAccessor;
        }

        public void setAlphaRasterAccessor(RasterAccessor alphaRasterAccessor) {
            this.alphaRasterAccessor = alphaRasterAccessor;
        }

        public Raster getRoiRaster() {
            return roiRaster;
        }

        public void setRoiRaster(Raster roiRaster) {
            this.roiRaster = roiRaster;
        }

        public Range getSourceNoDataRange() {
            return sourceNoDataRange;
        }

        public void setSourceNoDataRange(Range sourceNoDataRange) {
            this.sourceNoDataRange = sourceNoDataRange;
        }

        public void setBounds(Rectangle bounds) {
            this.bounds = bounds;
        }

        public Rectangle getBounds() {
            return bounds;
        }
    }

}
