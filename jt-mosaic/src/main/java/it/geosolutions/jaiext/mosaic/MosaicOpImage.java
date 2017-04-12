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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
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

import com.sun.media.jai.util.ImageUtil;

import it.geosolutions.jaiext.lookup.LookupTable;
import it.geosolutions.jaiext.lookup.LookupTableFactory;
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
    private MosaicType mosaicTypeSelected;

    /** Number of bands for every image */
    private int numBands;

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
    private BorderExtender sourceBorderExtender;

    /** Border extender for the ROI or alpha channel data */
    private BorderExtender zeroBorderExtender;

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
    protected byte[][][] byteLookupTable;

    /** Boolean array indicating which source images has No Data and not */
    private final boolean[] hasNoData;

    /** The format tag for the destination image */
    private RasterFormatTag rasterFormatTag;

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
                    setRGBLayout(result, first);
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
            setRGBLayout(result, first);
        }

        return result;
    }

    private static void setRGBLayout(ImageLayout result, RenderedImage reference) {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        int[] nBits = { 8, 8, 8 };
        ColorModel cm = new ComponentColorModel(cs, nBits, false, false, Transparency.OPAQUE,
                DataBuffer.TYPE_BYTE);
        SampleModel sm = cm.createCompatibleSampleModel(reference.getSampleModel().getWidth(),
                reference.getSampleModel().getWidth());
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
        if (destinationNoData == null) {
            this.destinationNoDataDouble = DEFAULT_DESTINATION_NO_DATA_VALUE;
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                this.destinationNoDataInt = new int[numSources];
                Arrays.fill(this.destinationNoDataInt, Integer.MIN_VALUE);
                this.destinationNoDataByte = new byte[numSources];
                Arrays.fill(this.destinationNoDataByte, (byte) 0);
                break;
            case DataBuffer.TYPE_USHORT:
                this.destinationNoDataInt = new int[numSources];
                Arrays.fill(this.destinationNoDataInt, Integer.MIN_VALUE);
                this.destinationNoDataUShort = new short[numSources];
                Arrays.fill(this.destinationNoDataUShort, (short) 0);
                break;
            case DataBuffer.TYPE_SHORT:
                this.destinationNoDataInt = new int[numSources];
                Arrays.fill(this.destinationNoDataInt, Integer.MIN_VALUE);
                this.destinationNoDataShort = new short[numSources];
                Arrays.fill(this.destinationNoDataShort, Short.MIN_VALUE);
                break;
            case DataBuffer.TYPE_INT:
                this.destinationNoDataInt = new int[numSources];
                Arrays.fill(this.destinationNoDataInt, Integer.MIN_VALUE);
                break;
            case DataBuffer.TYPE_FLOAT:
                this.destinationNoDataFloat = new float[numSources];
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

        hasNoData = new boolean[numSources];

        // This list contains the alpha channel for every source image (if present)
        List<PlanarImage> alphaList = new ArrayList<PlanarImage>();

        // NoDataRangeByte initialization
        byteLookupTable = new byte[numSources][numBands][256];

        // Init the imagemosaic bean
        imageBeans = new ImageMosaicBean[numSources];
        for (int i = 0; i < numSources; i++) {
            imageBeans[i] = new ImageMosaicBean();
        }

        // This cycle is used for checking if every alpha channel is single banded
        // and has the same sample model of the source images. Also wrap all the source,
        // Alpha and ROI images(if present) with a Border operator in order to avoid
        // the call of the getExtendedData() method.
        RasterFormatTag[] tags = getRasterFormatTags();
        for (int i = 0; i < numSources; i++) {
            // Selection of the i-th source.
            RenderedImage img = getSourceImage(i);
            // Calculation of the padding
            int[] padding = calculatePadding(img, totalBounds);
            // Extend the Source image if padding is defined
            if (padding != null) {
                ParameterBlock pb = new ParameterBlock();
                pb.setSource(img, 0);
                pb.set(padding[0], 0);
                pb.set(padding[1], 1);
                pb.set(padding[2], 2);
                pb.set(padding[3], 3);
                pb.set(sourceBorderExtender, 4);
                // Setting of the padded source to the associated bean
                RenderedOp create = JAI.create("border", pb);
                imageBeans[i].setImage(create);
            } else {
                imageBeans[i].setImage(img);
            }
            // Selection of the alpha channel
            PlanarImage alpha = alphaExists ? alphaImgs[i] : null;
            alphaList.add(alpha);
            ROI roi = roiExists ? rois[i] : null;
            if (alpha != null) {
                alphaPresent = true;
                SampleModel alphaSampleModel = alpha.getSampleModel();
                // Padding of the alpha channel
                // Calculate padding
                int[] pads = calculatePadding(alpha, totalBounds);
                // Extend the alpha image if padding is defined
                if (pads != null) {
                    ParameterBlock pb = new ParameterBlock();
                    pb.setSource(alpha, 0);
                    pb.set(pads[0], 0);
                    pb.set(pads[1], 1);
                    pb.set(pads[2], 2);
                    pb.set(pads[3], 3);
                    pb.set(zeroBorderExtender, 4);
                    // Setting of the padded alpha to the associated bean
                    RenderedOp create = JAI.create("border", pb);
                    imageBeans[i].setAlphaChannel(create);
                } else {
                    imageBeans[i].setAlphaChannel(alpha);
                }

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
                // Padding of the ROI image
                // Calculate padding
                int[] pads = calculatePadding(roiIMG, totalBounds);
                // Extend the roi image if padding is defined
                if (pads != null) {
                    ParameterBlock pb = new ParameterBlock();
                    pb.setSource(roiIMG, 0);
                    pb.set(pads[0], 0);
                    pb.set(pads[1], 1);
                    pb.set(pads[2], 2);
                    pb.set(pads[3], 3);
                    pb.set(zeroBorderExtender, 4);
                    // Setting of the padded ROI image to the associated bean
                    RenderedOp create = JAI.create("border", pb);
                    imageBeans[i].setRoiImage(create);
                } else {
                    imageBeans[i].setRoiImage(roiIMG);
                }
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
                    hasNoData[i] = true;
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
                        hasNoData[i] = false;
                    } else if (dataType == DataBuffer.TYPE_BYTE) {
                        // selection of the no data range for byte values
                        Range noDataByte = expandedNoDataRage;

                        // The lookup table is filled with the related no data or valid data for
                        // every
                        // value
                        for (int b = 0; b < numBands; b++) {
                            for (int z = 0; z < byteLookupTable[i][0].length; z++) {
                                if (noDataByte != null && noDataByte.contains(z)) {
                                    byteLookupTable[i][b][z] = destinationNoDataByte[b];
                                } else {
                                    byteLookupTable[i][b][z] = (byte) z;
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
     * Method for calculating the padding between the total mosaic bounds and the bounds of the
     * input source.
     * 
     * @param src
     * @param totalBounds
     * @return
     */
    private int[] calculatePadding(RenderedImage src, Rectangle totalBounds) {
        // Calculation of the padding to set in order toexpand the input
        // image to the Rectangle dimensions
        int deltaX0 = (src.getMinX() - totalBounds.x);
        int leftP = deltaX0 > 0 ? deltaX0 : 0;
        int deltaY0 = (src.getMinY() - totalBounds.y);
        int topP = deltaY0 > 0 ? deltaY0 : 0;
        int deltaX1 = (totalBounds.x + totalBounds.width - src.getMinX() - src.getWidth());
        int rightP = deltaX1 > 0 ? deltaX1 : 0;
        int deltaY1 = (totalBounds.y + totalBounds.height - src.getMinY() - src.getHeight());
        int bottomP = deltaY1 > 0 ? deltaY1 : 0;
        if ((leftP + rightP + topP + bottomP) == 0) {
            // no padding return null
            return null;
        }
        return new int[] { leftP, rightP, topP, bottomP };
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
                if (source.getBounds().contains(destRectangle)) {
                    data = source.getData(destRectangle);
                } else {
                    data = imageBeans[i].getImage().getData(destRectangle);
                }
            }
            // Raster bean initialization
            // If the data are present then we can check if Alpha and ROI are present
            if (data != null) {
                sourceRasters[intersectingSourceCount] = data;
                sourceTags[intersectingSourceCount] = imageBeans[i].getRasterFormatTag();
                sourceColorModels[intersectingSourceCount] = imageBeans[i].getColorModel();
                noDataRanges[intersectingSourceCount] = imageBeans[i].getSourceNoData();

                // Get the Alpha data from the padded alpha image if present
                PlanarImage alpha = imageBeans[i].getAlphaChannel();
                if (alphaPresent && alpha != null) {
                    alphaRasters[intersectingSourceCount] = alpha.getData(destRectangle);
                    alphaChannelColorModels[intersectingSourceCount] = imageBeans[i].getAlphaChannel().getColorModel();
                }

                // Get the ROI data from the padded ROI image if present
                RenderedImage roi = imageBeans[i].getRoiImage();
                if (roiPresent && roi != null) {
                    roiRasters[intersectingSourceCount] = roi.getData(destRectangle);
                }
                
                intersectingSourceCount++;
            }

        }
        
        // For the given source destination rasters, the mosaic is calculated
        computeRect(sourceRasters, sourceTags, sourceColorModels, destRaster, destRectangle,
                alphaRasters, roiRasters, noDataRanges, alphaChannelColorModels, intersectingSourceCount);

        // Tile recycling if the Recycle is present
        for (int i = 0; i < intersectingSourceCount; i++) {
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

    private void computeRect(Raster[] sourceRasters, RasterFormatTag[] rasterFormatTags,
            ColorModel[] sourceColorModels, WritableRaster destRaster, Rectangle destRectangle,
            Raster[] alphaRasters, Raster[] roiRasters, Range[] noDataRanges, ColorModel[] alphaChannelColorModels, int sourcesNumber) {

        // if all null, just return a constant image
        if (sourcesNumber == 0) {
            ImageUtil.fillBackground(destRaster, destRectangle, destinationNoDataDouble);
            return;
        }

        // Creates source accessors bean array (a new bean)
        RasterBeanAccessor[] sourceAccessorsArrayBean = new RasterBeanAccessor[sourcesNumber];
        // The above array is filled with image data, roi, alpha and no data ranges
        for (int i = 0; i < sourcesNumber; i++) {
            // RasterAccessorBean temporary file
            RasterBeanAccessor helpAccessor = new RasterBeanAccessor();
            if (sourceRasters[i] != null) {
                helpAccessor.setDataRasterAccessor(new RasterAccessorExt(sourceRasters[i],
                        destRectangle, rasterFormatTags[i], sourceColorModels[i], getNumBands(),
                        getSampleModel().getDataType()));

            }
            Raster alphaRaster = alphaRasters[i];
            if (alphaRaster != null) {
                SampleModel alphaSampleModel = alphaRaster.getSampleModel();
                int alphaFormatTagID = RasterAccessor.findCompatibleTag(null, alphaSampleModel);
                RasterFormatTag alphaFormatTag = new RasterFormatTag(alphaSampleModel,
                        alphaFormatTagID);
                helpAccessor.setAlphaRasterAccessor(new RasterAccessor(alphaRaster, destRectangle,
                        alphaFormatTag, alphaChannelColorModels[i]));
            }

            helpAccessor.setRoiRaster(roiRasters[i]);
            helpAccessor.setSourceNoDataRangeRasterAccessor(noDataRanges[i]);

            sourceAccessorsArrayBean[i] = helpAccessor;
        }

        // Create dest accessor.
        RasterAccessor destinationAccessor = new RasterAccessor(destRaster, destRectangle,
                rasterFormatTag, null);
        // This method calculates the mosaic of the source images and stores the
        // result in the destination
        // accessor

        int dataType = destinationAccessor.getDataType();

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(sourceAccessorsArrayBean, destinationAccessor);
            break;
        case DataBuffer.TYPE_USHORT:
            ushortLoop(sourceAccessorsArrayBean, destinationAccessor);
            break;
        case DataBuffer.TYPE_SHORT:
            shortLoop(sourceAccessorsArrayBean, destinationAccessor);
            break;
        case DataBuffer.TYPE_INT:
            intLoop(sourceAccessorsArrayBean, destinationAccessor);
            break;
        case DataBuffer.TYPE_FLOAT:
            floatLoop(sourceAccessorsArrayBean, destinationAccessor);
            break;
        case DataBuffer.TYPE_DOUBLE:
            doubleLoop(sourceAccessorsArrayBean, destinationAccessor);
            break;
        }
        // the data are copied back to the destination raster
        destinationAccessor.copyDataToRaster();

    }

    private void byteLoop(RasterBeanAccessor[] srcBean, RasterAccessor dst) {

        // Stores the source number
        final int sourcesNumber = srcBean.length;

        // From every source all the LineStride, PixelStride, LineOffsets,
        // PixelOffsets and Band Offset are initialized
        final int[] srcLineStride = new int[sourcesNumber];
        final int[] srcPixelStride = new int[sourcesNumber];
        final int[][] srcBandOffsets = new int[sourcesNumber][];
        final int[] sLineOffsets = new int[sourcesNumber];
        final int[] sPixelOffsets = new int[sourcesNumber];

        // Source data creation with null values
        final byte[][][] srcDataByte = new byte[sourcesNumber][][];
        // Alpha Channel creation
        final byte[][][] alfaDataByte;
        // Destination data creation
        final byte[][] dstDataByte = dst.getByteDataArrays();
        // Source data per band creation
        final byte[][] sBandDataByte = new byte[sourcesNumber][];
        // Alpha data per band creation
        final byte[][] aBandDataByte;

        // Check if the alpha is used in the selected raster.
        boolean alphaPresentinRaster = false;
        for (int i = 0; i < sourcesNumber; i++) {
            if (srcBean[i].getAlphaRasterAccessor() != null) {
                alphaPresentinRaster = true;
                break;
            }
        }

        // LineStride, PixelStride, BandOffset, LineOffset, PixelOffset for the
        // alpha channel
        final int[] alfaLineStride;
        final int[] alfaPixelStride;
        final int[][] alfaBandOffsets;
        final int[] aLineOffsets;
        final int[] aPixelOffsets;

        if (alphaPresentinRaster) {
            // The above alpha arrays are allocated only if the alpha channel is
            // present
            alfaLineStride = new int[sourcesNumber];
            alfaPixelStride = new int[sourcesNumber];
            alfaBandOffsets = new int[sourcesNumber][];
            aLineOffsets = new int[sourcesNumber];
            aPixelOffsets = new int[sourcesNumber];
            alfaDataByte = new byte[sourcesNumber][][];
            aBandDataByte = new byte[sourcesNumber][];
        } else {
            alfaLineStride = null;
            alfaPixelStride = null;
            alfaBandOffsets = null;
            aLineOffsets = null;
            aPixelOffsets = null;

            alfaDataByte = null;
            aBandDataByte = null;
        }

        // Weight type arrays can have different weight types if ROI or alpha
        // channel are present or not
        final WeightType[] weightTypesUsed = new WeightType[sourcesNumber];

        // The above arrays are filled with the data from the Java Raster
        // AcessorBean.
        for (int i = 0; i < sourcesNumber; i++) {
            weightTypesUsed[i] = WeightType.WEIGHT_TYPE_NODATA;
            final RasterAccessor dataRA = srcBean[i].getDataRasterAccessor();
            if (dataRA != null) {
                srcLineStride[i] = dataRA.getScanlineStride();
                srcPixelStride[i] = dataRA.getPixelStride();
                srcBandOffsets[i] = dataRA.getBandOffsets();
                // Data retrieval
                srcDataByte[i] = dataRA.getByteDataArrays();
                final RasterAccessor alphaRA = srcBean[i].getAlphaRasterAccessor();
                if (alphaPresentinRaster & alphaRA != null) {
                    alfaDataByte[i] = alphaRA.getByteDataArrays();
                    alfaBandOffsets[i] = alphaRA.getBandOffsets();
                    alfaPixelStride[i] = alphaRA.getPixelStride();
                    alfaLineStride[i] = alphaRA.getScanlineStride();
                }
                if (alphaRA != null) {
                    // If alpha channel is present alpha weight type is used
                    weightTypesUsed[i] = WeightType.WEIGHT_TYPE_ALPHA;
                } else if (roiPresent && srcBean[i].getRoiRaster() != null) {
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

        // COMPUTATION LEVEL

        for (int b = 0; b < dstBands; b++) { // For all the Bands
            // The data value are taken for every band
            for (int s = 0; s < sourcesNumber; s++) {
                if (srcBean[s].getDataRasterAccessor() != null) {
                    // source band data
                    sBandDataByte[s] = srcDataByte[s][b];
                    // The offset is initialized
                    sLineOffsets[s] = srcBandOffsets[s][b];
                }
                if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                    // The alpha value are taken only from the first band (this
                    // happens because the raster
                    // accessor provides the data array with the band data even if
                    // the alpha channel has only
                    // one band.
                    aBandDataByte[s] = alfaDataByte[s][0];
                    aLineOffsets[s] = alfaBandOffsets[s][0];
                }
            }

            // The destination data band are selected
            byte[] dBandDataByte = dstDataByte[b];
            // the destination lineOffset is initialized
            int dLineOffset = dstBandOffsets[b];

            if (mosaicTypeSelected == MosaicDescriptor.MOSAIC_TYPE_OVERLAY) {
                for (int dstY = dstMinY; dstY < dstMaxY; dstY++) { // For all the Y
                                                                   // values
                    // Source line Offset and pixel Offset,
                    // Alpha line Offset and pixel Offset are initialized
                    for (int s = 0; s < sourcesNumber; s++) {
                        if (srcBean[s].getDataRasterAccessor() != null) {
                            sPixelOffsets[s] = sLineOffsets[s];
                            sLineOffsets[s] += srcLineStride[s];
                        }
                        if (srcBean[s].getAlphaRasterAccessor() != null) {
                            aPixelOffsets[s] = aLineOffsets[s];
                            aLineOffsets[s] += alfaLineStride[s];
                        }
                    }

                    // The same operation is performed for the destination offsets
                    int dPixelOffset = dLineOffset;
                    dLineOffset += dstLineStride;

                    for (int dstX = dstMinX; dstX < dstMaxX; dstX++) { // For all
                                                                       // the X
                                                                       // values

                        // The destination flag is initialized to false and changes
                        // to true only
                        // if one pixel alpha channel is not 0 or falls into an
                        // image ROI or is not a NoData
                        boolean setDestinationFlag = false;

                        for (int s = 0; s < sourcesNumber; s++) {
                            final RasterAccessor dataRA = srcBean[s].getDataRasterAccessor();
                            if (dataRA == null) {
                                continue;
                            }
                            // The source valuse are initialized only for the switch
                            // method
                            int sourceValueByte = sBandDataByte[s][sPixelOffsets[s]];
                            // Offset update
                            sPixelOffsets[s] += srcPixelStride[s];

                            // the flag checks if the pixel is a noData
                            boolean isData = true;
                            if (hasNoData[s]) {
                                isData = !(byteLookupTable[s][b][sourceValueByte
                                        & 0xFF] == destinationNoDataByte[b]);
                            }

                            if (!isData) {
                                setDestinationFlag = false;
                                if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                                    aPixelOffsets[s] += alfaPixelStride[s];
                                }
                            } else {

                                switch (weightTypesUsed[s]) {
                                case WEIGHT_TYPE_ALPHA:
                                    setDestinationFlag = aBandDataByte[s][aPixelOffsets[s]] != 0;

                                    aPixelOffsets[s] += alfaPixelStride[s];
                                    break;
                                case WEIGHT_TYPE_ROI:
                                    setDestinationFlag = srcBean[s].getRoiRaster().getSample(dstX,
                                            dstY, 0) > 0;
                                    break;
                                default:
                                    setDestinationFlag = true;

                                }
                            }
                            // If the flag is True, the related source pixel is
                            // saved in the
                            // destination one and exit from the cycle after
                            // incrementing the offset
                            if (setDestinationFlag) {
                                dBandDataByte[dPixelOffset] = (byte) (sourceValueByte & 0xff);

                                for (int k = s + 1; k < sourcesNumber; k++) {
                                    if (dataRA != null) {
                                        sPixelOffsets[k] += srcPixelStride[k];
                                    }
                                    if (srcBean[k].getAlphaRasterAccessor() != null) {
                                        aPixelOffsets[k] += alfaPixelStride[k];
                                    }
                                }
                                break;
                            }
                        }
                        // If the flag is false for every source, the destinationb
                        // no data value is
                        // set to the related destination pixel and then updates the
                        // offset
                        if (!setDestinationFlag) {
                            dBandDataByte[dPixelOffset] = destinationNoDataByte[b];
                        }

                        dPixelOffset += dstPixelStride;
                    }
                }
            } else { // the mosaicType is MOSAIC_TYPE_BLEND
                for (int dstY = dstMinY; dstY < dstMaxY; dstY++) {
                    // Source and pixel Offset are initialized and Source and alpha
                    // line offset are
                    // translated (cycle accross all the sources)
                    for (int s = 0; s < sourcesNumber; s++) {
                        if (srcBean[s].getDataRasterAccessor() != null) {
                            sPixelOffsets[s] = sLineOffsets[s];
                            sLineOffsets[s] += srcLineStride[s];
                        }
                        if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                            aPixelOffsets[s] = aLineOffsets[s];
                            aLineOffsets[s] += alfaLineStride[s];
                        }
                    }

                    // The same operation is performed for the destination offsets
                    int dPixelOffset = dLineOffset;
                    dLineOffset += dstLineStride;

                    for (int dstX = dstMinX; dstX < dstMaxX; dstX++) {

                        // In the blending operation the destination pixel value is
                        // calculated
                        // as sum of the weighted source pixel / sum of weigth.
                        double numerator = 0.0;
                        double denominator = 0.0;

                        for (int s = 0; s < sourcesNumber; s++) {
                            if (srcBean[s].getDataRasterAccessor() == null) {
                                continue;
                            }

                            // The source valuse are initialized only for the switch
                            // method
                            int sourceValueByte = sBandDataByte[s][sPixelOffsets[s]];
                            // Offset update
                            sPixelOffsets[s] += srcPixelStride[s];
                            // The weight is calculated for every pixel
                            double weight = 0.0F;

                            boolean isData = true;

                            // If no alpha channel or Roi is present, the weight
                            // is set to 1 or 0 if the pixel has
                            // or not a No Data value
                            if (hasNoData[s]) {
                                isData = !(byteLookupTable[s][b][sourceValueByte
                                        & 0xFF] == destinationNoDataByte[b]);
                            }
                            if (!isData) {
                                weight = 0F;
                                if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                                    aPixelOffsets[s] += alfaPixelStride[s];
                                }
                            } else {
                                switch (weightTypesUsed[s]) {
                                case WEIGHT_TYPE_ALPHA:
                                    weight = (aBandDataByte[s][aPixelOffsets[s]] & 0xff);
                                    if (weight > 0.0F && isAlphaBitmaskUsed) {
                                        weight = 1.0F;
                                    } else {
                                        weight /= 255.0F;
                                    }
                                    aPixelOffsets[s] += alfaPixelStride[s];
                                    break;
                                case WEIGHT_TYPE_ROI:
                                    weight = srcBean[s].getRoiRaster().getSample(dstX, dstY, 0) > 0
                                            ? 1.0F : 0.0F;
                                    break;
                                default:
                                    weight = 1.0F;
                                }
                            }
                            // The above calculated weight are added to the
                            // numerator and denominator
                            numerator += (weight * (sourceValueByte & 0xff));

                            denominator += weight;
                        }

                        // If the weighted sum is 0 the destination pixel value
                        // takes the destination no data.
                        // If the sum is not 0 the value is added to the related
                        // destination pixel

                        if (denominator == 0.0) {
                            dBandDataByte[dPixelOffset] = destinationNoDataByte[b];

                        } else {
                            dBandDataByte[dPixelOffset] = ImageUtil
                                    .clampRoundByte(numerator / denominator);
                        }
                        // Offset update
                        dPixelOffset += dstPixelStride;
                    }
                }
            }
        }
    }

    private void ushortLoop(RasterBeanAccessor[] srcBean, RasterAccessor dst) {

        // Stores the source number
        final int sourcesNumber = srcBean.length;

        // From every source all the LineStride, PixelStride, LineOffsets,
        // PixelOffsets and Band Offset are initialized
        final int[] srcLineStride = new int[sourcesNumber];
        final int[] srcPixelStride = new int[sourcesNumber];
        final int[][] srcBandOffsets = new int[sourcesNumber][];
        final int[] sLineOffsets = new int[sourcesNumber];
        final int[] sPixelOffsets = new int[sourcesNumber];

        // Source data creation with null values
        final short[][][] srcDataUshort = new short[sourcesNumber][][];
        // Alpha Channel creation
        final short[][][] alfaDataUshort;
        // Destination data creation
        final short[][] dstDataUshort = dst.getShortDataArrays();
        // Source data per band creation
        final short[][] sBandDataUshort = new short[sourcesNumber][];
        // Alpha data per band creation
        final short[][] aBandDataUshort;

        // Check if the alpha is used in the selected raster.
        boolean alphaPresentinRaster = false;
        for (int i = 0; i < sourcesNumber; i++) {
            if (srcBean[i].getAlphaRasterAccessor() != null) {
                alphaPresentinRaster = true;
                break;
            }
        }

        // LineStride, PixelStride, BandOffset, LineOffset, PixelOffset for the
        // alpha channel
        final int[] alfaLineStride;
        final int[] alfaPixelStride;
        final int[][] alfaBandOffsets;
        final int[] aLineOffsets;
        final int[] aPixelOffsets;

        if (alphaPresentinRaster) {
            // The above alpha arrays are allocated only if the alpha channel is
            // present
            alfaLineStride = new int[sourcesNumber];
            alfaPixelStride = new int[sourcesNumber];
            alfaBandOffsets = new int[sourcesNumber][];
            aLineOffsets = new int[sourcesNumber];
            aPixelOffsets = new int[sourcesNumber];
            alfaDataUshort = new short[sourcesNumber][][];
            aBandDataUshort = new short[sourcesNumber][];
        } else {
            alfaLineStride = null;
            alfaPixelStride = null;
            alfaBandOffsets = null;
            aLineOffsets = null;
            aPixelOffsets = null;

            alfaDataUshort = null;
            aBandDataUshort = null;
        }

        // Weight type arrays can have different weight types if ROI or alpha
        // channel are present or not
        final WeightType[] weightTypesUsed = new WeightType[sourcesNumber];

        // The above arrays are filled with the data from the Java Raster
        // AcessorBean.
        for (int i = 0; i < sourcesNumber; i++) {
            weightTypesUsed[i] = WeightType.WEIGHT_TYPE_NODATA;
            final RasterAccessor dataRA = srcBean[i].getDataRasterAccessor();
            if (dataRA != null) {
                srcLineStride[i] = dataRA.getScanlineStride();
                srcPixelStride[i] = dataRA.getPixelStride();
                srcBandOffsets[i] = dataRA.getBandOffsets();
                // Data retrieval
                srcDataUshort[i] = dataRA.getShortDataArrays();
                final RasterAccessor alphaRA = srcBean[i].getAlphaRasterAccessor();
                if (alphaPresentinRaster & alphaRA != null) {
                    alfaDataUshort[i] = alphaRA.getShortDataArrays();
                    alfaBandOffsets[i] = alphaRA.getBandOffsets();
                    alfaPixelStride[i] = alphaRA.getPixelStride();
                    alfaLineStride[i] = alphaRA.getScanlineStride();
                }
                if (alphaRA != null) {
                    // If alpha channel is present alpha weight type is used
                    weightTypesUsed[i] = WeightType.WEIGHT_TYPE_ALPHA;
                } else if (roiPresent && srcBean[i].getRoiRaster() != null) {
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

        // COMPUTATION LEVEL

        for (int b = 0; b < dstBands; b++) { // For all the Bands
            // The data value are taken for every band
            for (int s = 0; s < sourcesNumber; s++) {
                if (srcBean[s].getDataRasterAccessor() != null) {
                    // source band data
                    sBandDataUshort[s] = srcDataUshort[s][b];
                    // The offset is initialized
                    sLineOffsets[s] = srcBandOffsets[s][b];
                }
                if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                    // The alpha value are taken only from the first band (this
                    // happens because the raster
                    // accessor provides the data array with the band data even if
                    // the alpha channel has only
                    // one band.
                    aBandDataUshort[s] = alfaDataUshort[s][0];
                    aLineOffsets[s] = alfaBandOffsets[s][0];
                }
            }

            // The destination data band are selected
            short[] dBandDataUshort = dstDataUshort[b];
            ;
            // the destination lineOffset is initialized
            int dLineOffset = dstBandOffsets[b];

            if (mosaicTypeSelected == MosaicDescriptor.MOSAIC_TYPE_OVERLAY) {
                for (int dstY = dstMinY; dstY < dstMaxY; dstY++) { // For all the Y
                                                                   // values
                    // Source line Offset and pixel Offset,
                    // Alpha line Offset and pixel Offset are initialized
                    for (int s = 0; s < sourcesNumber; s++) {
                        if (srcBean[s].getDataRasterAccessor() != null) {
                            sPixelOffsets[s] = sLineOffsets[s];
                            sLineOffsets[s] += srcLineStride[s];
                        }
                        if (srcBean[s].getAlphaRasterAccessor() != null) {
                            aPixelOffsets[s] = aLineOffsets[s];
                            aLineOffsets[s] += alfaLineStride[s];
                        }
                    }

                    // The same operation is performed for the destination offsets
                    int dPixelOffset = dLineOffset;
                    dLineOffset += dstLineStride;

                    for (int dstX = dstMinX; dstX < dstMaxX; dstX++) { // For all
                                                                       // the X
                                                                       // values

                        // The destination flag is initialized to false and changes
                        // to true only
                        // if one pixel alpha channel is not 0 or falls into an
                        // image ROI or is not a NoData
                        boolean setDestinationFlag = false;

                        for (int s = 0; s < sourcesNumber; s++) {
                            final RasterAccessor dataRA = srcBean[s].getDataRasterAccessor();
                            if (dataRA == null) {
                                continue;
                            }
                            // The source valuse are initialized only for the switch
                            // method
                            short value = sBandDataUshort[s][sPixelOffsets[s]];
                            int sourceValueUshort = (value & 0xffff);
                            // Offset update
                            sPixelOffsets[s] += srcPixelStride[s];

                            // the flag checks if the pixel is a noData
                            boolean isData = true;
                            if (hasNoData[s]) {
                                Range noDataRangeUShort = (srcBean[s]
                                        .getSourceNoDataRangeRasterAccessor());
                                ;
                                isData = !noDataRangeUShort.contains(sourceValueUshort);
                            }

                            if (!isData) {
                                setDestinationFlag = false;
                                if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                                    aPixelOffsets[s] += alfaPixelStride[s];
                                }
                            } else {

                                switch (weightTypesUsed[s]) {
                                case WEIGHT_TYPE_ALPHA:
                                    setDestinationFlag = aBandDataUshort[s][aPixelOffsets[s]] != 0;

                                    aPixelOffsets[s] += alfaPixelStride[s];
                                    break;
                                case WEIGHT_TYPE_ROI:
                                    setDestinationFlag = srcBean[s].getRoiRaster().getSample(dstX,
                                            dstY, 0) > 0;
                                    break;
                                default:
                                    setDestinationFlag = true;

                                }
                            }
                            // If the flag is True, the related source pixel is
                            // saved in the
                            // destination one and exit from the cycle after
                            // incrementing the offset
                            if (setDestinationFlag) {
                                dBandDataUshort[dPixelOffset] = value;

                                for (int k = s + 1; k < sourcesNumber; k++) {
                                    if (dataRA != null) {
                                        sPixelOffsets[k] += srcPixelStride[k];
                                    }
                                    if (srcBean[k].getAlphaRasterAccessor() != null) {
                                        aPixelOffsets[k] += alfaPixelStride[k];
                                    }
                                }
                                break;
                            }
                        }
                        // If the flag is false for every source, the destinationb
                        // no data value is
                        // set to the related destination pixel and then updates the
                        // offset
                        if (!setDestinationFlag) {
                            dBandDataUshort[dPixelOffset] = destinationNoDataUShort[b];
                        }

                        dPixelOffset += dstPixelStride;
                    }
                }
            } else { // the mosaicType is MOSAIC_TYPE_BLEND
                for (int dstY = dstMinY; dstY < dstMaxY; dstY++) {
                    // Source and pixel Offset are initialized and Source and alpha
                    // line offset are
                    // translated (cycle accross all the sources)
                    for (int s = 0; s < sourcesNumber; s++) {
                        if (srcBean[s].getDataRasterAccessor() != null) {
                            sPixelOffsets[s] = sLineOffsets[s];
                            sLineOffsets[s] += srcLineStride[s];
                        }
                        if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                            aPixelOffsets[s] = aLineOffsets[s];
                            aLineOffsets[s] += alfaLineStride[s];
                        }
                    }

                    // The same operation is performed for the destination offsets
                    int dPixelOffset = dLineOffset;
                    dLineOffset += dstLineStride;

                    for (int dstX = dstMinX; dstX < dstMaxX; dstX++) {

                        // In the blending operation the destination pixel value is
                        // calculated
                        // as sum of the weighted source pixel / sum of weigth.
                        double numerator = 0.0;
                        double denominator = 0.0;

                        for (int s = 0; s < sourcesNumber; s++) {
                            if (srcBean[s].getDataRasterAccessor() == null) {
                                continue;
                            }

                            // The source valuse are initialized only for the switch
                            // method
                            short sourceValueUshort = (short) (sBandDataUshort[s][sPixelOffsets[s]]
                                    & 0xffff);
                            // Offset update
                            sPixelOffsets[s] += srcPixelStride[s];
                            // The weight is calculated for every pixel
                            double weight = 0.0F;

                            boolean isData = true;

                            // If no alpha channel or Roi is present, the weight
                            // is set to 1 or 0 if the pixel has
                            // or not a No Data value
                            if (hasNoData[s]) {
                                Range noDataRangeUShort = (srcBean[s]
                                        .getSourceNoDataRangeRasterAccessor());
                                isData = !noDataRangeUShort.contains(sourceValueUshort);
                            }
                            if (!isData) {
                                weight = 0F;
                                if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                                    aPixelOffsets[s] += alfaPixelStride[s];
                                }
                            } else {
                                switch (weightTypesUsed[s]) {
                                case WEIGHT_TYPE_ALPHA:
                                    weight = (aBandDataUshort[s][aPixelOffsets[s]] & 0xffff);
                                    if (weight > 0.0F && isAlphaBitmaskUsed) {
                                        weight = 1.0F;
                                    } else {
                                        weight /= 255.0F;
                                    }
                                    aPixelOffsets[s] += alfaPixelStride[s];
                                    break;
                                case WEIGHT_TYPE_ROI:
                                    weight = srcBean[s].getRoiRaster().getSample(dstX, dstY, 0) > 0
                                            ? 1.0F : 0.0F;
                                    break;
                                default:
                                    weight = 1.0F;
                                }
                            }
                            // The above calculated weight are added to the
                            // numerator and denominator
                            numerator += (weight * (sourceValueUshort));

                            denominator += weight;
                        }

                        // If the weighted sum is 0 the destination pixel value
                        // takes the destination no data.
                        // If the sum is not 0 the value is added to the related
                        // destination pixel

                        if (denominator == 0.0) {
                            dBandDataUshort[dPixelOffset] = destinationNoDataUShort[b];

                        } else {
                            dBandDataUshort[dPixelOffset] = ImageUtil
                                    .clampRoundUShort(numerator / denominator);
                        }
                        // Offset update
                        dPixelOffset += dstPixelStride;
                    }
                }
            }
        }
    }

    private void shortLoop(RasterBeanAccessor[] srcBean, RasterAccessor dst) {

        // Stores the source number
        final int sourcesNumber = srcBean.length;

        // From every source all the LineStride, PixelStride, LineOffsets,
        // PixelOffsets and Band Offset are initialized
        final int[] srcLineStride = new int[sourcesNumber];
        final int[] srcPixelStride = new int[sourcesNumber];
        final int[][] srcBandOffsets = new int[sourcesNumber][];
        final int[] sLineOffsets = new int[sourcesNumber];
        final int[] sPixelOffsets = new int[sourcesNumber];

        // Source data creation with null values
        final short[][][] srcDataShort = new short[sourcesNumber][][];
        // Alpha Channel creation
        final short[][][] alfaDataShort;
        // Destination data creation
        final short[][] dstDataShort = dst.getShortDataArrays();
        // Source data per band creation
        final short[][] sBandDataShort = new short[sourcesNumber][];
        // Alpha data per band creation
        final short[][] aBandDataShort;

        // Check if the alpha is used in the selected raster.
        boolean alphaPresentinRaster = false;
        for (int i = 0; i < sourcesNumber; i++) {
            if (srcBean[i].getAlphaRasterAccessor() != null) {
                alphaPresentinRaster = true;
                break;
            }
        }

        // LineStride, PixelStride, BandOffset, LineOffset, PixelOffset for the
        // alpha channel
        final int[] alfaLineStride;
        final int[] alfaPixelStride;
        final int[][] alfaBandOffsets;
        final int[] aLineOffsets;
        final int[] aPixelOffsets;

        if (alphaPresentinRaster) {
            // The above alpha arrays are allocated only if the alpha channel is
            // present
            alfaLineStride = new int[sourcesNumber];
            alfaPixelStride = new int[sourcesNumber];
            alfaBandOffsets = new int[sourcesNumber][];
            aLineOffsets = new int[sourcesNumber];
            aPixelOffsets = new int[sourcesNumber];
            alfaDataShort = new short[sourcesNumber][][];
            aBandDataShort = new short[sourcesNumber][];
        } else {
            alfaLineStride = null;
            alfaPixelStride = null;
            alfaBandOffsets = null;
            aLineOffsets = null;
            aPixelOffsets = null;

            alfaDataShort = null;
            aBandDataShort = null;
        }

        // Weight type arrays can have different weight types if ROI or alpha
        // channel are present or not
        final WeightType[] weightTypesUsed = new WeightType[sourcesNumber];

        // The above arrays are filled with the data from the Java Raster
        // AcessorBean.
        for (int i = 0; i < sourcesNumber; i++) {
            weightTypesUsed[i] = WeightType.WEIGHT_TYPE_NODATA;
            final RasterAccessor dataRA = srcBean[i].getDataRasterAccessor();
            if (dataRA != null) {
                srcLineStride[i] = dataRA.getScanlineStride();
                srcPixelStride[i] = dataRA.getPixelStride();
                srcBandOffsets[i] = dataRA.getBandOffsets();
                // Data retrieval
                srcDataShort[i] = dataRA.getShortDataArrays();
                final RasterAccessor alphaRA = srcBean[i].getAlphaRasterAccessor();
                if (alphaPresentinRaster & alphaRA != null) {
                    alfaDataShort[i] = alphaRA.getShortDataArrays();
                    alfaBandOffsets[i] = alphaRA.getBandOffsets();
                    alfaPixelStride[i] = alphaRA.getPixelStride();
                    alfaLineStride[i] = alphaRA.getScanlineStride();
                }
                if (alphaRA != null) {
                    // If alpha channel is present alpha weight type is used
                    weightTypesUsed[i] = WeightType.WEIGHT_TYPE_ALPHA;
                } else if (roiPresent && srcBean[i].getRoiRaster() != null) {
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

        // COMPUTATION LEVEL

        for (int b = 0; b < dstBands; b++) { // For all the Bands
            // The data value are taken for every band
            for (int s = 0; s < sourcesNumber; s++) {
                if (srcBean[s].getDataRasterAccessor() != null) {
                    // source band data
                    sBandDataShort[s] = srcDataShort[s][b];
                    // The offset is initialized
                    sLineOffsets[s] = srcBandOffsets[s][b];
                }
                if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                    // The alpha value are taken only from the first band (this
                    // happens because the raster
                    // accessor provides the data array with the band data even if
                    // the alpha channel has only
                    // one band.
                    aBandDataShort[s] = alfaDataShort[s][0];
                    aLineOffsets[s] = alfaBandOffsets[s][0];
                }
            }

            // The destination data band are selected
            short[] dBandDataShort = dstDataShort[b];
            // the destination lineOffset is initialized
            int dLineOffset = dstBandOffsets[b];

            if (mosaicTypeSelected == MosaicDescriptor.MOSAIC_TYPE_OVERLAY) {
                for (int dstY = dstMinY; dstY < dstMaxY; dstY++) { // For all the Y
                                                                   // values
                    // Source line Offset and pixel Offset,
                    // Alpha line Offset and pixel Offset are initialized
                    for (int s = 0; s < sourcesNumber; s++) {
                        if (srcBean[s].getDataRasterAccessor() != null) {
                            sPixelOffsets[s] = sLineOffsets[s];
                            sLineOffsets[s] += srcLineStride[s];
                        }
                        if (srcBean[s].getAlphaRasterAccessor() != null) {
                            aPixelOffsets[s] = aLineOffsets[s];
                            aLineOffsets[s] += alfaLineStride[s];
                        }
                    }

                    // The same operation is performed for the destination offsets
                    int dPixelOffset = dLineOffset;
                    dLineOffset += dstLineStride;

                    for (int dstX = dstMinX; dstX < dstMaxX; dstX++) { // For all
                                                                       // the X
                                                                       // values

                        // The destination flag is initialized to false and changes
                        // to true only
                        // if one pixel alpha channel is not 0 or falls into an
                        // image ROI or is not a NoData
                        boolean setDestinationFlag = false;

                        for (int s = 0; s < sourcesNumber; s++) {
                            final RasterAccessor dataRA = srcBean[s].getDataRasterAccessor();
                            if (dataRA == null) {
                                continue;
                            }
                            // The source valuse are initialized only for the switch
                            // method
                            short sourceValueShort = sBandDataShort[s][sPixelOffsets[s]];
                            // Offset update
                            sPixelOffsets[s] += srcPixelStride[s];

                            // the flag checks if the pixel is a noData
                            boolean isData = true;
                            if (hasNoData[s]) {
                                Range noDataRangeShort = (srcBean[s]
                                        .getSourceNoDataRangeRasterAccessor());
                                isData = !noDataRangeShort.contains(sourceValueShort);
                            }

                            if (!isData) {
                                setDestinationFlag = false;
                                if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                                    aPixelOffsets[s] += alfaPixelStride[s];
                                }
                            } else {

                                switch (weightTypesUsed[s]) {
                                case WEIGHT_TYPE_ALPHA:
                                    setDestinationFlag = aBandDataShort[s][aPixelOffsets[s]] != 0;

                                    aPixelOffsets[s] += alfaPixelStride[s];
                                    break;
                                case WEIGHT_TYPE_ROI:
                                    setDestinationFlag = srcBean[s].getRoiRaster().getSample(dstX,
                                            dstY, 0) > 0;
                                    break;
                                default:
                                    setDestinationFlag = true;

                                }
                            }
                            // If the flag is True, the related source pixel is
                            // saved in the
                            // destination one and exit from the cycle after
                            // incrementing the offset
                            if (setDestinationFlag) {
                                dBandDataShort[dPixelOffset] = sourceValueShort;

                                for (int k = s + 1; k < sourcesNumber; k++) {
                                    if (dataRA != null) {
                                        sPixelOffsets[k] += srcPixelStride[k];
                                    }
                                    if (srcBean[k].getAlphaRasterAccessor() != null) {
                                        aPixelOffsets[k] += alfaPixelStride[k];
                                    }
                                }
                                break;
                            }
                        }
                        // If the flag is false for every source, the destinationb
                        // no data value is
                        // set to the related destination pixel and then updates the
                        // offset
                        if (!setDestinationFlag) {
                            dBandDataShort[dPixelOffset] = destinationNoDataShort[b];
                        }

                        dPixelOffset += dstPixelStride;
                    }
                }
            } else { // the mosaicType is MOSAIC_TYPE_BLEND
                for (int dstY = dstMinY; dstY < dstMaxY; dstY++) {
                    // Source and pixel Offset are initialized and Source and alpha
                    // line offset are
                    // translated (cycle accross all the sources)
                    for (int s = 0; s < sourcesNumber; s++) {
                        if (srcBean[s].getDataRasterAccessor() != null) {
                            sPixelOffsets[s] = sLineOffsets[s];
                            sLineOffsets[s] += srcLineStride[s];
                        }
                        if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                            aPixelOffsets[s] = aLineOffsets[s];
                            aLineOffsets[s] += alfaLineStride[s];
                        }
                    }

                    // The same operation is performed for the destination offsets
                    int dPixelOffset = dLineOffset;
                    dLineOffset += dstLineStride;

                    for (int dstX = dstMinX; dstX < dstMaxX; dstX++) {

                        // In the blending operation the destination pixel value is
                        // calculated
                        // as sum of the weighted source pixel / sum of weigth.
                        double numerator = 0.0;
                        double denominator = 0.0;

                        for (int s = 0; s < sourcesNumber; s++) {
                            if (srcBean[s].getDataRasterAccessor() == null) {
                                continue;
                            }

                            // The source valuse are initialized only for the switch
                            // method
                            short sourceValueShort = sBandDataShort[s][sPixelOffsets[s]];
                            // Offset update
                            sPixelOffsets[s] += srcPixelStride[s];
                            // The weight is calculated for every pixel
                            double weight = 0.0F;

                            boolean isData = true;

                            // If no alpha channel or Roi is present, the weight
                            // is set to 1 or 0 if the pixel has
                            // or not a No Data value
                            if (hasNoData[s]) {
                                Range noDataRangeShort = (srcBean[s]
                                        .getSourceNoDataRangeRasterAccessor());
                                isData = !noDataRangeShort.contains(sourceValueShort);
                            }
                            if (!isData) {
                                weight = 0F;
                                if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                                    aPixelOffsets[s] += alfaPixelStride[s];
                                }
                            } else {
                                switch (weightTypesUsed[s]) {
                                case WEIGHT_TYPE_ALPHA:
                                    weight = (aBandDataShort[s][aPixelOffsets[s]]);
                                    if (weight > 0.0F && isAlphaBitmaskUsed) {
                                        weight = 1.0F;
                                    } else {
                                        weight /= 255.0F;
                                    }
                                    aPixelOffsets[s] += alfaPixelStride[s];
                                    break;
                                case WEIGHT_TYPE_ROI:
                                    weight = srcBean[s].getRoiRaster().getSample(dstX, dstY, 0) > 0
                                            ? 1.0F : 0.0F;
                                    break;
                                default:
                                    weight = 1.0F;
                                }
                            }
                            // The above calculated weight are added to the
                            // numerator and denominator
                            numerator += (weight * (sourceValueShort));

                            denominator += weight;
                        }

                        // If the weighted sum is 0 the destination pixel value
                        // takes the destination no data.
                        // If the sum is not 0 the value is added to the related
                        // destination pixel

                        if (denominator == 0.0) {
                            dBandDataShort[dPixelOffset] = destinationNoDataShort[b];

                        } else {
                            dBandDataShort[dPixelOffset] = ImageUtil
                                    .clampRoundShort(numerator / denominator);
                        }
                        // Offset update
                        dPixelOffset += dstPixelStride;
                    }
                }
            }
        }
    }

    private void intLoop(RasterBeanAccessor[] srcBean, RasterAccessor dst) {

        // Stores the source number
        final int sourcesNumber = srcBean.length;

        // From every source all the LineStride, PixelStride, LineOffsets,
        // PixelOffsets and Band Offset are initialized
        final int[] srcLineStride = new int[sourcesNumber];
        final int[] srcPixelStride = new int[sourcesNumber];
        final int[][] srcBandOffsets = new int[sourcesNumber][];
        final int[] sLineOffsets = new int[sourcesNumber];
        final int[] sPixelOffsets = new int[sourcesNumber];

        // Source data creation with null values
        final int[][][] srcDataInt = new int[sourcesNumber][][];
        // Alpha Channel creation
        final int[][][] alfaDataInt;
        // Destination data creation
        final int[][] dstDataInt = dst.getIntDataArrays();
        // Source data per band creation
        final int[][] sBandDataInt = new int[sourcesNumber][];
        // Alpha data per band creation
        final int[][] aBandDataInt;

        // Check if the alpha is used in the selected raster.
        boolean alphaPresentinRaster = false;
        for (int i = 0; i < sourcesNumber; i++) {
            if (srcBean[i].getAlphaRasterAccessor() != null) {
                alphaPresentinRaster = true;
                break;
            }
        }

        // LineStride, PixelStride, BandOffset, LineOffset, PixelOffset for the
        // alpha channel
        final int[] alfaLineStride;
        final int[] alfaPixelStride;
        final int[][] alfaBandOffsets;
        final int[] aLineOffsets;
        final int[] aPixelOffsets;

        if (alphaPresentinRaster) {
            // The above alpha arrays are allocated only if the alpha channel is
            // present
            alfaLineStride = new int[sourcesNumber];
            alfaPixelStride = new int[sourcesNumber];
            alfaBandOffsets = new int[sourcesNumber][];
            aLineOffsets = new int[sourcesNumber];
            aPixelOffsets = new int[sourcesNumber];
            alfaDataInt = new int[sourcesNumber][][];
            aBandDataInt = new int[sourcesNumber][];
        } else {
            alfaLineStride = null;
            alfaPixelStride = null;
            alfaBandOffsets = null;
            aLineOffsets = null;
            aPixelOffsets = null;

            alfaDataInt = null;
            aBandDataInt = null;
        }

        // Weight type arrays can have different weight types if ROI or alpha
        // channel are present or not
        final WeightType[] weightTypesUsed = new WeightType[sourcesNumber];

        // The above arrays are filled with the data from the Java Raster
        // AcessorBean.
        for (int i = 0; i < sourcesNumber; i++) {
            weightTypesUsed[i] = WeightType.WEIGHT_TYPE_NODATA;
            final RasterAccessor dataRA = srcBean[i].getDataRasterAccessor();
            if (dataRA != null) {
                srcLineStride[i] = dataRA.getScanlineStride();
                srcPixelStride[i] = dataRA.getPixelStride();
                srcBandOffsets[i] = dataRA.getBandOffsets();
                // Data retrieval
                srcDataInt[i] = dataRA.getIntDataArrays();
                final RasterAccessor alphaRA = srcBean[i].getAlphaRasterAccessor();
                if (alphaPresentinRaster & alphaRA != null) {
                    alfaDataInt[i] = alphaRA.getIntDataArrays();
                    alfaBandOffsets[i] = alphaRA.getBandOffsets();
                    alfaPixelStride[i] = alphaRA.getPixelStride();
                    alfaLineStride[i] = alphaRA.getScanlineStride();
                }
                if (alphaRA != null) {
                    // If alpha channel is present alpha weight type is used
                    weightTypesUsed[i] = WeightType.WEIGHT_TYPE_ALPHA;
                } else if (roiPresent && srcBean[i].getRoiRaster() != null) {
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

        // COMPUTATION LEVEL

        for (int b = 0; b < dstBands; b++) { // For all the Bands
            // The data value are taken for every band
            for (int s = 0; s < sourcesNumber; s++) {
                if (srcBean[s].getDataRasterAccessor() != null) {
                    // source band data
                    sBandDataInt[s] = srcDataInt[s][b];
                    // The offset is initialized
                    sLineOffsets[s] = srcBandOffsets[s][b];
                }
                if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                    // The alpha value are taken only from the first band (this
                    // happens because the raster
                    // accessor provides the data array with the band data even if
                    // the alpha channel has only
                    // one band.
                    aBandDataInt[s] = alfaDataInt[s][0];
                    aLineOffsets[s] = alfaBandOffsets[s][0];
                }
            }

            // The destination data band are selected
            int[] dBandDataInt = dstDataInt[b];
            // the destination lineOffset is initialized
            int dLineOffset = dstBandOffsets[b];

            if (mosaicTypeSelected == MosaicDescriptor.MOSAIC_TYPE_OVERLAY) {
                for (int dstY = dstMinY; dstY < dstMaxY; dstY++) { // For all the Y
                                                                   // values
                    // Source line Offset and pixel Offset,
                    // Alpha line Offset and pixel Offset are initialized
                    for (int s = 0; s < sourcesNumber; s++) {
                        if (srcBean[s].getDataRasterAccessor() != null) {
                            sPixelOffsets[s] = sLineOffsets[s];
                            sLineOffsets[s] += srcLineStride[s];
                        }
                        if (srcBean[s].getAlphaRasterAccessor() != null) {
                            aPixelOffsets[s] = aLineOffsets[s];
                            aLineOffsets[s] += alfaLineStride[s];
                        }
                    }

                    // The same operation is performed for the destination offsets
                    int dPixelOffset = dLineOffset;
                    dLineOffset += dstLineStride;

                    for (int dstX = dstMinX; dstX < dstMaxX; dstX++) { // For all
                                                                       // the X
                                                                       // values

                        // The destination flag is initialized to false and changes
                        // to true only
                        // if one pixel alpha channel is not 0 or falls into an
                        // image ROI or is not a NoData
                        boolean setDestinationFlag = false;

                        for (int s = 0; s < sourcesNumber; s++) {
                            final RasterAccessor dataRA = srcBean[s].getDataRasterAccessor();
                            if (dataRA == null) {
                                continue;
                            }
                            // The source valuse are initialized only for the switch
                            // method
                            int sourceValueInt = sBandDataInt[s][sPixelOffsets[s]];
                            // Offset update
                            sPixelOffsets[s] += srcPixelStride[s];

                            // the flag checks if the pixel is a noData
                            boolean isData = true;
                            if (hasNoData[s]) {
                                Range noDataRangeInt = (srcBean[s]
                                        .getSourceNoDataRangeRasterAccessor());
                                isData = !noDataRangeInt.contains(sourceValueInt);
                            }

                            if (!isData) {
                                setDestinationFlag = false;
                                if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                                    aPixelOffsets[s] += alfaPixelStride[s];
                                }
                            } else {

                                switch (weightTypesUsed[s]) {
                                case WEIGHT_TYPE_ALPHA:
                                    setDestinationFlag = aBandDataInt[s][aPixelOffsets[s]] != 0;

                                    aPixelOffsets[s] += alfaPixelStride[s];
                                    break;
                                case WEIGHT_TYPE_ROI:
                                    setDestinationFlag = srcBean[s].getRoiRaster().getSample(dstX,
                                            dstY, 0) > 0;
                                    break;
                                default:
                                    setDestinationFlag = true;

                                }
                            }
                            // If the flag is True, the related source pixel is
                            // saved in the
                            // destination one and exit from the cycle after
                            // incrementing the offset
                            if (setDestinationFlag) {
                                dBandDataInt[dPixelOffset] = sourceValueInt;

                                for (int k = s + 1; k < sourcesNumber; k++) {
                                    if (dataRA != null) {
                                        sPixelOffsets[k] += srcPixelStride[k];
                                    }
                                    if (srcBean[k].getAlphaRasterAccessor() != null) {
                                        aPixelOffsets[k] += alfaPixelStride[k];
                                    }
                                }
                                break;
                            }
                        }
                        // If the flag is false for every source, the destinationb
                        // no data value is
                        // set to the related destination pixel and then updates the
                        // offset
                        if (!setDestinationFlag) {
                            dBandDataInt[dPixelOffset] = destinationNoDataInt[b];
                        }

                        dPixelOffset += dstPixelStride;
                    }
                }
            } else { // the mosaicType is MOSAIC_TYPE_BLEND
                for (int dstY = dstMinY; dstY < dstMaxY; dstY++) {
                    // Source and pixel Offset are initialized and Source and alpha
                    // line offset are
                    // translated (cycle accross all the sources)
                    for (int s = 0; s < sourcesNumber; s++) {
                        if (srcBean[s].getDataRasterAccessor() != null) {
                            sPixelOffsets[s] = sLineOffsets[s];
                            sLineOffsets[s] += srcLineStride[s];
                        }
                        if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                            aPixelOffsets[s] = aLineOffsets[s];
                            aLineOffsets[s] += alfaLineStride[s];
                        }
                    }

                    // The same operation is performed for the destination offsets
                    int dPixelOffset = dLineOffset;
                    dLineOffset += dstLineStride;

                    for (int dstX = dstMinX; dstX < dstMaxX; dstX++) {

                        // In the blending operation the destination pixel value is
                        // calculated
                        // as sum of the weighted source pixel / sum of weigth.
                        double numerator = 0.0;
                        double denominator = 0.0;

                        for (int s = 0; s < sourcesNumber; s++) {
                            if (srcBean[s].getDataRasterAccessor() == null) {
                                continue;
                            }

                            // The source valuse are initialized only for the switch
                            // method
                            int sourceValueInt = sBandDataInt[s][sPixelOffsets[s]];
                            // Offset update
                            sPixelOffsets[s] += srcPixelStride[s];
                            // The weight is calculated for every pixel
                            double weight = 0.0F;

                            boolean isData = true;

                            // If no alpha channel or Roi is present, the weight
                            // is set to 1 or 0 if the pixel has
                            // or not a No Data value
                            if (hasNoData[s]) {
                                Range noDataRangeInt = (srcBean[s]
                                        .getSourceNoDataRangeRasterAccessor());
                                isData = !noDataRangeInt.contains(sourceValueInt);
                            }
                            if (!isData) {
                                weight = 0F;
                                if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                                    aPixelOffsets[s] += alfaPixelStride[s];
                                }
                            } else {
                                switch (weightTypesUsed[s]) {
                                case WEIGHT_TYPE_ALPHA:
                                    weight = (aBandDataInt[s][aPixelOffsets[s]]);
                                    if (weight > 0.0F && isAlphaBitmaskUsed) {
                                        weight = 1.0F;
                                    } else {
                                        weight /= 255.0F;
                                    }
                                    aPixelOffsets[s] += alfaPixelStride[s];
                                    break;
                                case WEIGHT_TYPE_ROI:
                                    weight = srcBean[s].getRoiRaster().getSample(dstX, dstY, 0) > 0
                                            ? 1.0F : 0.0F;
                                    break;
                                default:
                                    weight = 1.0F;
                                }
                            }
                            // The above calculated weight are added to the
                            // numerator and denominator
                            numerator += (weight * (sourceValueInt));

                            denominator += weight;
                        }

                        // If the weighted sum is 0 the destination pixel value
                        // takes the destination no data.
                        // If the sum is not 0 the value is added to the related
                        // destination pixel

                        if (denominator == 0.0) {
                            dBandDataInt[dPixelOffset] = destinationNoDataInt[b];

                        } else {
                            dBandDataInt[dPixelOffset] = ImageUtil
                                    .clampRoundInt(numerator / denominator);
                        }
                        // Offset update
                        dPixelOffset += dstPixelStride;
                    }
                }
            }
        }
    }

    private void floatLoop(RasterBeanAccessor[] srcBean, RasterAccessor dst) {

        // Stores the source number
        final int sourcesNumber = srcBean.length;

        // From every source all the LineStride, PixelStride, LineOffsets,
        // PixelOffsets and Band Offset are initialized
        final int[] srcLineStride = new int[sourcesNumber];
        final int[] srcPixelStride = new int[sourcesNumber];
        final int[][] srcBandOffsets = new int[sourcesNumber][];
        final int[] sLineOffsets = new int[sourcesNumber];
        final int[] sPixelOffsets = new int[sourcesNumber];

        // Source data creation with null values
        final float[][][] srcDataFloat = new float[sourcesNumber][][];
        // Alpha Channel creation
        final float[][][] alfaDataFloat;
        // Destination data creation
        final float[][] dstDataFloat = dst.getFloatDataArrays();
        // Source data per band creation
        final float[][] sBandDataFloat = new float[sourcesNumber][];
        // Alpha data per band creation
        final float[][] aBandDataFloat;

        // Check if the alpha is used in the selected raster.
        boolean alphaPresentinRaster = false;
        for (int i = 0; i < sourcesNumber; i++) {
            if (srcBean[i].getAlphaRasterAccessor() != null) {
                alphaPresentinRaster = true;
                break;
            }
        }

        // LineStride, PixelStride, BandOffset, LineOffset, PixelOffset for the
        // alpha channel
        final int[] alfaLineStride;
        final int[] alfaPixelStride;
        final int[][] alfaBandOffsets;
        final int[] aLineOffsets;
        final int[] aPixelOffsets;

        if (alphaPresentinRaster) {
            // The above alpha arrays are allocated only if the alpha channel is
            // present
            alfaLineStride = new int[sourcesNumber];
            alfaPixelStride = new int[sourcesNumber];
            alfaBandOffsets = new int[sourcesNumber][];
            aLineOffsets = new int[sourcesNumber];
            aPixelOffsets = new int[sourcesNumber];
            alfaDataFloat = new float[sourcesNumber][][];
            aBandDataFloat = new float[sourcesNumber][];
        } else {
            alfaLineStride = null;
            alfaPixelStride = null;
            alfaBandOffsets = null;
            aLineOffsets = null;
            aPixelOffsets = null;

            alfaDataFloat = null;
            aBandDataFloat = null;
        }

        // Weight type arrays can have different weight types if ROI or alpha
        // channel are present or not
        final WeightType[] weightTypesUsed = new WeightType[sourcesNumber];

        // The above arrays are filled with the data from the Java Raster
        // AcessorBean.
        for (int i = 0; i < sourcesNumber; i++) {
            weightTypesUsed[i] = WeightType.WEIGHT_TYPE_NODATA;
            final RasterAccessor dataRA = srcBean[i].getDataRasterAccessor();
            if (dataRA != null) {
                srcLineStride[i] = dataRA.getScanlineStride();
                srcPixelStride[i] = dataRA.getPixelStride();
                srcBandOffsets[i] = dataRA.getBandOffsets();
                // Data retrieval
                srcDataFloat[i] = dataRA.getFloatDataArrays();
                final RasterAccessor alphaRA = srcBean[i].getAlphaRasterAccessor();
                if (alphaPresentinRaster & alphaRA != null) {
                    alfaDataFloat[i] = alphaRA.getFloatDataArrays();
                    alfaBandOffsets[i] = alphaRA.getBandOffsets();
                    alfaPixelStride[i] = alphaRA.getPixelStride();
                    alfaLineStride[i] = alphaRA.getScanlineStride();
                }
                if (alphaRA != null) {
                    // If alpha channel is present alpha weight type is used
                    weightTypesUsed[i] = WeightType.WEIGHT_TYPE_ALPHA;
                } else if (roiPresent && srcBean[i].getRoiRaster() != null) {
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

        // COMPUTATION LEVEL

        for (int b = 0; b < dstBands; b++) { // For all the Bands
            // The data value are taken for every band
            for (int s = 0; s < sourcesNumber; s++) {
                if (srcBean[s].getDataRasterAccessor() != null) {
                    // source band data
                    sBandDataFloat[s] = srcDataFloat[s][b];
                    // The offset is initialized
                    sLineOffsets[s] = srcBandOffsets[s][b];
                }
                if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                    // The alpha value are taken only from the first band (this
                    // happens because the raster
                    // accessor provides the data array with the band data even if
                    // the alpha channel has only
                    // one band.
                    aBandDataFloat[s] = alfaDataFloat[s][0];
                    aLineOffsets[s] = alfaBandOffsets[s][0];
                }
            }

            // The destination data band are selected
            float[] dBandDataFloat = dstDataFloat[b];
            ;
            // the destination lineOffset is initialized
            int dLineOffset = dstBandOffsets[b];

            if (mosaicTypeSelected == MosaicDescriptor.MOSAIC_TYPE_OVERLAY) {
                for (int dstY = dstMinY; dstY < dstMaxY; dstY++) { // For all the Y
                                                                   // values
                    // Source line Offset and pixel Offset,
                    // Alpha line Offset and pixel Offset are initialized
                    for (int s = 0; s < sourcesNumber; s++) {
                        if (srcBean[s].getDataRasterAccessor() != null) {
                            sPixelOffsets[s] = sLineOffsets[s];
                            sLineOffsets[s] += srcLineStride[s];
                        }
                        if (srcBean[s].getAlphaRasterAccessor() != null) {
                            aPixelOffsets[s] = aLineOffsets[s];
                            aLineOffsets[s] += alfaLineStride[s];
                        }
                    }

                    // The same operation is performed for the destination offsets
                    int dPixelOffset = dLineOffset;
                    dLineOffset += dstLineStride;

                    for (int dstX = dstMinX; dstX < dstMaxX; dstX++) { // For all
                                                                       // the X
                                                                       // values

                        // The destination flag is initialized to false and changes
                        // to true only
                        // if one pixel alpha channel is not 0 or falls into an
                        // image ROI or is not a NoData
                        boolean setDestinationFlag = false;

                        for (int s = 0; s < sourcesNumber; s++) {
                            final RasterAccessor dataRA = srcBean[s].getDataRasterAccessor();
                            if (dataRA == null) {
                                continue;
                            }
                            // The source valuse are initialized only for the switch
                            // method
                            float sourceValueFloat = sBandDataFloat[s][sPixelOffsets[s]];
                            // Offset update
                            sPixelOffsets[s] += srcPixelStride[s];

                            // the flag checks if the pixel is a noData
                            boolean isData = true;
                            if (hasNoData[s]) {
                                Range noDataRangeFloat = (srcBean[s]
                                        .getSourceNoDataRangeRasterAccessor());
                                if (noDataRangeFloat != null) {
                                    isData = !(noDataRangeFloat.contains(sourceValueFloat));
                                }
                            }

                            if (!isData) {
                                setDestinationFlag = false;
                                if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                                    aPixelOffsets[s] += alfaPixelStride[s];
                                }
                            } else {

                                switch (weightTypesUsed[s]) {
                                case WEIGHT_TYPE_ALPHA:
                                    setDestinationFlag = aBandDataFloat[s][aPixelOffsets[s]] != 0;

                                    aPixelOffsets[s] += alfaPixelStride[s];
                                    break;
                                case WEIGHT_TYPE_ROI:
                                    setDestinationFlag = srcBean[s].getRoiRaster().getSample(dstX,
                                            dstY, 0) > 0;
                                    break;
                                default:
                                    setDestinationFlag = true;

                                }
                            }
                            // If the flag is True, the related source pixel is
                            // saved in the
                            // destination one and exit from the cycle after
                            // incrementing the offset
                            if (setDestinationFlag) {
                                dBandDataFloat[dPixelOffset] = sourceValueFloat;

                                for (int k = s + 1; k < sourcesNumber; k++) {
                                    if (dataRA != null) {
                                        sPixelOffsets[k] += srcPixelStride[k];
                                    }
                                    if (srcBean[k].getAlphaRasterAccessor() != null) {
                                        aPixelOffsets[k] += alfaPixelStride[k];
                                    }
                                }
                                break;
                            }
                        }
                        // If the flag is false for every source, the destinationb
                        // no data value is
                        // set to the related destination pixel and then updates the
                        // offset
                        if (!setDestinationFlag) {
                            dBandDataFloat[dPixelOffset] = destinationNoDataFloat[b];
                        }

                        dPixelOffset += dstPixelStride;
                    }
                }
            } else { // the mosaicType is MOSAIC_TYPE_BLEND
                for (int dstY = dstMinY; dstY < dstMaxY; dstY++) {
                    // Source and pixel Offset are initialized and Source and alpha
                    // line offset are
                    // translated (cycle accross all the sources)
                    for (int s = 0; s < sourcesNumber; s++) {
                        if (srcBean[s].getDataRasterAccessor() != null) {
                            sPixelOffsets[s] = sLineOffsets[s];
                            sLineOffsets[s] += srcLineStride[s];
                        }
                        if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                            aPixelOffsets[s] = aLineOffsets[s];
                            aLineOffsets[s] += alfaLineStride[s];
                        }
                    }

                    // The same operation is performed for the destination offsets
                    int dPixelOffset = dLineOffset;
                    dLineOffset += dstLineStride;

                    for (int dstX = dstMinX; dstX < dstMaxX; dstX++) {

                        // In the blending operation the destination pixel value is
                        // calculated
                        // as sum of the weighted source pixel / sum of weigth.
                        double numerator = 0.0;
                        double denominator = 0.0;

                        for (int s = 0; s < sourcesNumber; s++) {
                            if (srcBean[s].getDataRasterAccessor() == null) {
                                continue;
                            }

                            // The source valuse are initialized only for the switch
                            // method
                            float sourceValueFloat = sBandDataFloat[s][sPixelOffsets[s]];
                            // Offset update
                            sPixelOffsets[s] += srcPixelStride[s];
                            // The weight is calculated for every pixel
                            double weight = 0.0F;

                            boolean isData = true;

                            // If no alpha channel or Roi is present, the weight
                            // is set to 1 or 0 if the pixel has
                            // or not a No Data value
                            if (hasNoData[s]) {
                                Range noDataRangeFloat = (srcBean[s]
                                        .getSourceNoDataRangeRasterAccessor());
                                if (noDataRangeFloat != null) {
                                    isData = !(noDataRangeFloat.contains(sourceValueFloat));
                                }
                            }
                            if (!isData) {
                                weight = 0F;
                                if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                                    aPixelOffsets[s] += alfaPixelStride[s];
                                }
                            } else {
                                switch (weightTypesUsed[s]) {
                                case WEIGHT_TYPE_ALPHA:
                                    weight = aBandDataFloat[s][aPixelOffsets[s]];
                                    if (weight > 0.0F && isAlphaBitmaskUsed) {
                                        weight = 1.0F;
                                    } else {
                                        weight /= 255.0F;
                                    }
                                    aPixelOffsets[s] += alfaPixelStride[s];
                                    break;
                                case WEIGHT_TYPE_ROI:
                                    weight = srcBean[s].getRoiRaster().getSample(dstX, dstY, 0) > 0
                                            ? 1.0F : 0.0F;
                                    break;
                                default:
                                    weight = 1.0F;
                                }
                            }
                            // The above calculated weight are added to the
                            // numerator and denominator
                            if (isData) {
                                numerator += (weight * (sourceValueFloat));
                            }

                            denominator += weight;
                        }

                        // If the weighted sum is 0 the destination pixel value
                        // takes the destination no data.
                        // If the sum is not 0 the value is added to the related
                        // destination pixel

                        if (denominator == 0.0) {
                            dBandDataFloat[dPixelOffset] = destinationNoDataFloat[b];

                        } else {
                            dBandDataFloat[dPixelOffset] = ImageUtil
                                    .clampFloat(numerator / denominator);
                        }
                        // Offset update
                        dPixelOffset += dstPixelStride;
                    }
                }
            }
        }
    }

    private void doubleLoop(RasterBeanAccessor[] srcBean, RasterAccessor dst) {

        // Stores the source number
        final int sourcesNumber = srcBean.length;

        // From every source all the LineStride, PixelStride, LineOffsets,
        // PixelOffsets and Band Offset are initialized
        final int[] srcLineStride = new int[sourcesNumber];
        final int[] srcPixelStride = new int[sourcesNumber];
        final int[][] srcBandOffsets = new int[sourcesNumber][];
        final int[] sLineOffsets = new int[sourcesNumber];
        final int[] sPixelOffsets = new int[sourcesNumber];

        // Source data creation with null values
        final double[][][] srcDataDouble = new double[sourcesNumber][][];
        // Alpha Channel creation
        final double[][][] alfaDataDouble;
        // Destination data creation
        final double[][] dstDataDouble = dst.getDoubleDataArrays();
        // Source data per band creation
        final double[][] sBandDataDouble = new double[sourcesNumber][];
        // Alpha data per band creation
        final double[][] aBandDataDouble;

        // Check if the alpha is used in the selected raster.
        boolean alphaPresentinRaster = false;
        for (int i = 0; i < sourcesNumber; i++) {
            if (srcBean[i].getAlphaRasterAccessor() != null) {
                alphaPresentinRaster = true;
                break;
            }
        }

        // LineStride, PixelStride, BandOffset, LineOffset, PixelOffset for the
        // alpha channel
        final int[] alfaLineStride;
        final int[] alfaPixelStride;
        final int[][] alfaBandOffsets;
        final int[] aLineOffsets;
        final int[] aPixelOffsets;

        if (alphaPresentinRaster) {
            // The above alpha arrays are allocated only if the alpha channel is
            // present
            alfaLineStride = new int[sourcesNumber];
            alfaPixelStride = new int[sourcesNumber];
            alfaBandOffsets = new int[sourcesNumber][];
            aLineOffsets = new int[sourcesNumber];
            aPixelOffsets = new int[sourcesNumber];
            alfaDataDouble = new double[sourcesNumber][][];
            aBandDataDouble = new double[sourcesNumber][];
        } else {
            alfaLineStride = null;
            alfaPixelStride = null;
            alfaBandOffsets = null;
            aLineOffsets = null;
            aPixelOffsets = null;

            alfaDataDouble = null;
            aBandDataDouble = null;
        }

        // Weight type arrays can have different weight types if ROI or alpha
        // channel are present or not
        final WeightType[] weightTypesUsed = new WeightType[sourcesNumber];

        // The above arrays are filled with the data from the Java Raster
        // AcessorBean.
        for (int i = 0; i < sourcesNumber; i++) {
            weightTypesUsed[i] = WeightType.WEIGHT_TYPE_NODATA;
            final RasterAccessor dataRA = srcBean[i].getDataRasterAccessor();
            if (dataRA != null) {
                srcLineStride[i] = dataRA.getScanlineStride();
                srcPixelStride[i] = dataRA.getPixelStride();
                srcBandOffsets[i] = dataRA.getBandOffsets();
                // Data retrieval
                srcDataDouble[i] = dataRA.getDoubleDataArrays();
                final RasterAccessor alphaRA = srcBean[i].getAlphaRasterAccessor();
                if (alphaPresentinRaster & alphaRA != null) {
                    alfaDataDouble[i] = alphaRA.getDoubleDataArrays();
                    alfaBandOffsets[i] = alphaRA.getBandOffsets();
                    alfaPixelStride[i] = alphaRA.getPixelStride();
                    alfaLineStride[i] = alphaRA.getScanlineStride();
                }
                if (alphaRA != null) {
                    // If alpha channel is present alpha weight type is used
                    weightTypesUsed[i] = WeightType.WEIGHT_TYPE_ALPHA;
                } else if (roiPresent && srcBean[i].getRoiRaster() != null) {
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

        // COMPUTATION LEVEL

        for (int b = 0; b < dstBands; b++) { // For all the Bands
            // The data value are taken for every band
            for (int s = 0; s < sourcesNumber; s++) {
                if (srcBean[s].getDataRasterAccessor() != null) {
                    // source band data
                    sBandDataDouble[s] = srcDataDouble[s][b];
                    // The offset is initialized
                    sLineOffsets[s] = srcBandOffsets[s][b];
                }
                if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                    // The alpha value are taken only from the first band (this
                    // happens because the raster
                    // accessor provides the data array with the band data even if
                    // the alpha channel has only
                    // one band.
                    aBandDataDouble[s] = alfaDataDouble[s][0];
                    aLineOffsets[s] = alfaBandOffsets[s][0];
                }
            }

            // The destination data band are selected
            double[] dBandDataDouble = dstDataDouble[b];
            // the destination lineOffset is initialized
            int dLineOffset = dstBandOffsets[b];

            if (mosaicTypeSelected == MosaicDescriptor.MOSAIC_TYPE_OVERLAY) {
                for (int dstY = dstMinY; dstY < dstMaxY; dstY++) { // For all the Y
                                                                   // values
                    // Source line Offset and pixel Offset,
                    // Alpha line Offset and pixel Offset are initialized
                    for (int s = 0; s < sourcesNumber; s++) {
                        if (srcBean[s].getDataRasterAccessor() != null) {
                            sPixelOffsets[s] = sLineOffsets[s];
                            sLineOffsets[s] += srcLineStride[s];
                        }
                        if (srcBean[s].getAlphaRasterAccessor() != null) {
                            aPixelOffsets[s] = aLineOffsets[s];
                            aLineOffsets[s] += alfaLineStride[s];
                        }
                    }

                    // The same operation is performed for the destination offsets
                    int dPixelOffset = dLineOffset;
                    dLineOffset += dstLineStride;

                    for (int dstX = dstMinX; dstX < dstMaxX; dstX++) { // For all
                                                                       // the X
                                                                       // values

                        // The destination flag is initialized to false and changes
                        // to true only
                        // if one pixel alpha channel is not 0 or falls into an
                        // image ROI or is not a NoData
                        boolean setDestinationFlag = false;

                        for (int s = 0; s < sourcesNumber; s++) {
                            final RasterAccessor dataRA = srcBean[s].getDataRasterAccessor();
                            if (dataRA == null) {
                                continue;
                            }
                            // The source valuse are initialized only for the switch
                            // method
                            double sourceValueDouble = sBandDataDouble[s][sPixelOffsets[s]];
                            // Offset update
                            sPixelOffsets[s] += srcPixelStride[s];

                            // the flag checks if the pixel is a noData
                            boolean isData = true;
                            if (hasNoData[s]) {
                                Range noDataRangeDouble = (srcBean[s]
                                        .getSourceNoDataRangeRasterAccessor());
                                if (noDataRangeDouble != null) {
                                    isData = !(noDataRangeDouble.contains(sourceValueDouble));
                                }
                            }

                            if (!isData) {
                                setDestinationFlag = false;
                                if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                                    aPixelOffsets[s] += alfaPixelStride[s];
                                }
                            } else {

                                switch (weightTypesUsed[s]) {
                                case WEIGHT_TYPE_ALPHA:
                                    setDestinationFlag = aBandDataDouble[s][aPixelOffsets[s]] != 0;

                                    aPixelOffsets[s] += alfaPixelStride[s];
                                    break;
                                case WEIGHT_TYPE_ROI:
                                    setDestinationFlag = srcBean[s].getRoiRaster().getSample(dstX,
                                            dstY, 0) > 0;
                                    break;
                                default:
                                    setDestinationFlag = true;

                                }
                            }
                            // If the flag is True, the related source pixel is
                            // saved in the
                            // destination one and exit from the cycle after
                            // incrementing the offset
                            if (setDestinationFlag) {
                                dBandDataDouble[dPixelOffset] = sourceValueDouble;

                                for (int k = s + 1; k < sourcesNumber; k++) {
                                    if (dataRA != null) {
                                        sPixelOffsets[k] += srcPixelStride[k];
                                    }
                                    if (srcBean[k].getAlphaRasterAccessor() != null) {
                                        aPixelOffsets[k] += alfaPixelStride[k];
                                    }
                                }
                                break;
                            }
                        }
                        // If the flag is false for every source, the destinationb
                        // no data value is
                        // set to the related destination pixel and then updates the
                        // offset
                        if (!setDestinationFlag) {
                            dBandDataDouble[dPixelOffset] = destinationNoDataDouble[b];
                        }

                        dPixelOffset += dstPixelStride;
                    }
                }
            } else { // the mosaicType is MOSAIC_TYPE_BLEND
                for (int dstY = dstMinY; dstY < dstMaxY; dstY++) {
                    // Source and pixel Offset are initialized and Source and alpha
                    // line offset are
                    // translated (cycle accross all the sources)
                    for (int s = 0; s < sourcesNumber; s++) {
                        if (srcBean[s].getDataRasterAccessor() != null) {
                            sPixelOffsets[s] = sLineOffsets[s];
                            sLineOffsets[s] += srcLineStride[s];
                        }
                        if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                            aPixelOffsets[s] = aLineOffsets[s];
                            aLineOffsets[s] += alfaLineStride[s];
                        }
                    }

                    // The same operation is performed for the destination offsets
                    int dPixelOffset = dLineOffset;
                    dLineOffset += dstLineStride;

                    for (int dstX = dstMinX; dstX < dstMaxX; dstX++) {

                        // In the blending operation the destination pixel value is
                        // calculated
                        // as sum of the weighted source pixel / sum of weigth.
                        double numerator = 0.0;
                        double denominator = 0.0;

                        for (int s = 0; s < sourcesNumber; s++) {
                            if (srcBean[s].getDataRasterAccessor() == null) {
                                continue;
                            }

                            // The source valuse are initialized only for the switch
                            // method
                            double sourceValueDouble = sBandDataDouble[s][sPixelOffsets[s]];
                            // Offset update
                            sPixelOffsets[s] += srcPixelStride[s];
                            // The weight is calculated for every pixel
                            double weight = 0.0F;

                            boolean isData = true;

                            // If no alpha channel or Roi is present, the weight
                            // is set to 1 or 0 if the pixel has
                            // or not a No Data value
                            if (hasNoData[s]) {
                                Range noDataRangeDouble = (srcBean[s]
                                        .getSourceNoDataRangeRasterAccessor());
                                if (noDataRangeDouble != null) {
                                    isData = !(noDataRangeDouble.contains(sourceValueDouble));
                                }
                            }
                            if (!isData) {
                                weight = 0F;
                                if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA) {
                                    aPixelOffsets[s] += alfaPixelStride[s];
                                }
                            } else {
                                switch (weightTypesUsed[s]) {
                                case WEIGHT_TYPE_ALPHA:
                                    weight = aBandDataDouble[s][aPixelOffsets[s]];
                                    if (weight > 0.0F && isAlphaBitmaskUsed) {
                                        weight = 1.0F;
                                    } else {
                                        weight /= 255.0F;
                                    }
                                    aPixelOffsets[s] += alfaPixelStride[s];
                                    break;
                                case WEIGHT_TYPE_ROI:
                                    weight = srcBean[s].getRoiRaster().getSample(dstX, dstY, 0) > 0
                                            ? 1.0F : 0.0F;
                                    break;
                                default:
                                    weight = 1.0F;
                                }
                            }
                            // The above calculated weight are added to the
                            // numerator and denominator
                            if (isData) {
                                numerator += (weight * (sourceValueDouble));
                            }

                            denominator += weight;
                        }

                        // If the weighted sum is 0 the destination pixel value
                        // takes the destination no data.
                        // If the sum is not 0 the value is added to the related
                        // destination pixel

                        if (denominator == 0.0) {
                            dBandDataDouble[dPixelOffset] = destinationNoDataDouble[b];

                        } else {
                            dBandDataDouble[dPixelOffset] = numerator / denominator;
                        }
                        // Offset update
                        dPixelOffset += dstPixelStride;
                    }
                }
            }
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
        private Range sourceNoDataRangeRasterAccessor;

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

        public Range getSourceNoDataRangeRasterAccessor() {
            return sourceNoDataRangeRasterAccessor;
        }

        public void setSourceNoDataRangeRasterAccessor(Range sourceNoDataRangeRasterAccessor) {
            this.sourceNoDataRangeRasterAccessor = sourceNoDataRangeRasterAccessor;
        }

    }

}
