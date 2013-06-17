package it.geosolutions.opimage;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.media.jai.BorderExtender;
import javax.media.jai.BorderExtenderConstant;
import javax.media.jai.ImageLayout;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.operator.MosaicDescriptor;
import javax.media.jai.operator.MosaicType;

import org.jaitools.numeric.Range;

import com.sun.media.jai.util.ImageUtil;

/**
 * This class takes an array of <code>RenderedImage</code> and creates a mosaic of them, checking if the source pixels represent no data values.
 */
public class MosaicNoDataOpImage extends OpImage {
    /**
     * Default value for the destination image if every pixel in the same location is a no data
     */
    public static final Number[] DEFAULT_DESTINATION_NO_DATA_VALUE = { 0 };

    /** mosaic type selected */
    private MosaicType mosaicTypeSelected;

    /** Number of bands for every image */
    private int numBands;

    /** Bean used for storing image data, ROI, alpha channel, Nodata Range */
    private ImageMosaicBean[] imageBeans;

    /** Boolean for checking if the ROI is used in the mosaic */
    private boolean roiPresent;

    /**
     * Boolean for checking if the alpha channel is used only for bitmask or for weighting every pixel with is alpha value associated
     */
    private boolean isAlphaBitmaskUsed;

    /** Boolean for checking if alpha channel is used in the mosaic */
    private boolean alphaPresent;

    /** Border extender for the source data */
    private BorderExtender sourceBorderExtender;

    /** Border extender for the ROI or alpha channel data */
    private BorderExtender zeroBorderExtender;

    /**
     * No data values for the destination image if the pixel of the same location are no Data
     */
    private Number[] destinationNoData;

    /** Enumerator for the type of mosaic weigher */
    public enum WeightType {
        WEIGHT_TYPE_ALPHA(1), WEIGHT_TYPE_ROI(2), WEIGHT_TYPE_NODATA(3);

        private final int value;

        WeightType(int num) {
            this.value = num;
        }

        public int valueData() {
            return value;
        }

    }

    /** Static method for providing a valid layout to the OpImage constructor */
    private static final ImageLayout checkLayout(List sources, ImageLayout layout) {

        // Variable Initialization
        RenderedImage sourceImage = null;
        SampleModel targetSampleModel = null;
        ColorModel targetColorModel = null;

        // Source number
        int numSources = sources.size();

        if (numSources > 0) {
            // The sample model and the color model are taken from the first image
            sourceImage = (RenderedImage) sources.get(0);
            targetSampleModel = sourceImage.getSampleModel();
            targetColorModel = sourceImage.getColorModel();
        } else if (layout != null // If there is no Images check the validity of the layout
                && layout.isValid(ImageLayout.WIDTH_MASK | ImageLayout.HEIGHT_MASK
                        | ImageLayout.SAMPLE_MODEL_MASK)) {
            // The sample model and the color model are taken from layout.
            targetSampleModel = layout.getSampleModel(null);
            if (targetSampleModel == null) {
                throw new IllegalArgumentException("No sample model present");
            }
        } else {// Not valid layout
            throw new IllegalArgumentException("Layout not valid");
        }

        // Datatype, band number and sample size are taken from sample model
        int dataType = targetSampleModel.getDataType();
        int bandNumber = targetSampleModel.getNumBands();
        int sampleSize = targetSampleModel.getSampleSize(0);

        // If the sample size is not the same it throws an IllegalArgumentException
        for (int i = 1; i < bandNumber; i++) {
            if (targetSampleModel.getSampleSize(i) != sampleSize) {
                throw new IllegalArgumentException("Sample size is not the same for every band");
            }
        }

        // If the source number is less than one the layout is cloned and returned
        if (numSources < 1) {
            return (ImageLayout) layout.clone();
        }

        // All the source image are checked if datatype, band number
        // and sample size are equal to those of the first image
        for (int i = 1; i < numSources; i++) {
            RenderedImage sourceData = (RenderedImage) sources.get(i);
            SampleModel sourceSampleModel = sourceData.getSampleModel();

            if (sourceSampleModel.getDataType() != dataType) {
                throw new IllegalArgumentException("Data type is not the same for every source");
            } else if (sourceSampleModel.getNumBands() != bandNumber) {
                throw new IllegalArgumentException("Bands number is not the same for every source");
            }

            for (int j = 0; j < bandNumber; j++) {
                if (sourceSampleModel.getSampleSize(j) != sampleSize) {
                    throw new IllegalArgumentException("Sample size is not the same for every band");
                }
            }
        }

        // If the layout is null a new one is created, else it is cloned. This new
        // layout
        // is the layout for all the images
        ImageLayout mosaicLayout = layout == null ? new ImageLayout() : (ImageLayout) layout
                .clone();

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
            mosaicBounds.setBounds(sourceImage.getMinX(), sourceImage.getMinY(),
                    sourceImage.getWidth(), sourceImage.getHeight());
            for (int i = 1; i < numSources; i++) {
                RenderedImage source = (RenderedImage) sources.get(i);
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

        // This control checks if the new layout is valid
        if (mosaicLayout.isValid(ImageLayout.SAMPLE_MODEL_MASK)) {
            SampleModel destSampleModel = mosaicLayout.getSampleModel(null);

            // If the destination image sample model has a band number or data type
            // or sample size from
            // those of the first image, the new layout sample model is unset.
            boolean unsetSampleModel = destSampleModel.getNumBands() != bandNumber
                    || destSampleModel.getDataType() != dataType;
            for (int i = 0; !unsetSampleModel && i < bandNumber; i++) {
                if (destSampleModel.getSampleSize(i) != sampleSize) {
                    unsetSampleModel = true;
                }
            }
            if (unsetSampleModel) {
                mosaicLayout.unsetValid(ImageLayout.SAMPLE_MODEL_MASK);
            }
        }

        return mosaicLayout;
    }

    /**
     * This constructor takes the source images, the layout, the rendering hints, and the parameters and initialize variables.
     */
    public MosaicNoDataOpImage(List sources, ImageLayout layout, Map renderingHints,
            ImageMosaicBean[] images, MosaicType mosaicTypeSelected, Number[] destinationNoData) {
        // OpImage constructor
        super((Vector) sources, checkLayout(sources, layout), renderingHints, true);
        // Checking if the source image size is equal to the java bean size
        if (sources.size() != images.length) {
            throw new IllegalArgumentException("Source and images must have the same length");
        }
        // Type of data used for every image
        int dataType = sampleModel.getDataType();

        // for (int i = 0; i < images.length; i++) {
        // if (images[i].getSourceNoData() == null) {
        // // If there is no data range inside an Image Bean, it is
        // // automatically added to it
        // switch (dataType) {
        // case DataBuffer.TYPE_BYTE:
        // images[i].setSourceNoData(new Range<Byte>(Byte.MIN_VALUE, true, Byte.MIN_VALUE,
        // true));
        // break;
        // case DataBuffer.TYPE_USHORT:
        // images[i].setSourceNoData(new Range<Short>((short) 0, true, (short) 0, true));
        // break;
        // case DataBuffer.TYPE_SHORT:
        // images[i].setSourceNoData(new Range<Short>(Short.MIN_VALUE, true,
        // Short.MIN_VALUE, true));
        // break;
        // case DataBuffer.TYPE_INT:
        // images[i].setSourceNoData(new Range<Integer>(Integer.MIN_VALUE, true,
        // Integer.MIN_VALUE, true));
        // break;
        // case DataBuffer.TYPE_FLOAT:
        // images[i].setSourceNoData(new Range<Float>(Float.MIN_VALUE, true,
        // Float.MIN_VALUE, true));
        // break;
        // case DataBuffer.TYPE_DOUBLE:
        // images[i].setSourceNoData(new Range<Double>(Double.MIN_VALUE, true,
        // Double.MIN_VALUE, true));
        // break;
        // default:
        //
        // }
        // }
        // }

        // Stores the data passed by the parameterBlock
        this.numBands = sampleModel.getNumBands();
        int numSources = getNumSources();
        this.mosaicTypeSelected = mosaicTypeSelected;
        this.imageBeans = images;
        this.roiPresent = false;
        this.alphaPresent = false;

        // This list contains the aplha channel for every source image (if present)
        List<PlanarImage> alphaList = new ArrayList<PlanarImage>();

        // This cycle is used for checking if every alpha channel is single banded
        // and has the same
        // sample model of the source images
        for (int i = 0; i < numSources; i++) {
            PlanarImage alpha = imageBeans[i].getAlphaChannel();
            alphaList.add(alpha);
            ROI imageROI = imageBeans[i].getImageRoi();
            if (alpha != null) {
                alphaPresent = true;
                SampleModel alphaSampleModel = alpha.getSampleModel();

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
            // If even only one ROI is present, this boolean is set to True
            if (imageROI != null) {
                roiPresent = true;
            }

        }

        // isAlphaBitmaskUsed is false only if there is an alpha channel for every
        // image
        this.isAlphaBitmaskUsed = !(mosaicTypeSelected == MosaicDescriptor.MOSAIC_TYPE_BLEND
                && alphaPresent && !(alphaList.size() < numSources));
        if (!this.isAlphaBitmaskUsed) {
            for (int i = 0; i < numSources; i++) {
                if (alphaList.get(i) == null) {
                    this.isAlphaBitmaskUsed = true;
                    break;
                }
            }
        }

        // Value for filling the image border
        double sourceExtensionBorder;
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            sourceExtensionBorder = 0.0;
            break;
        case DataBuffer.TYPE_USHORT:
            sourceExtensionBorder = 0.0;
            break;
        case DataBuffer.TYPE_SHORT:
            sourceExtensionBorder = Short.MIN_VALUE;
            break;
        case DataBuffer.TYPE_INT:
            sourceExtensionBorder = Integer.MIN_VALUE;
            break;
        case DataBuffer.TYPE_FLOAT:
            sourceExtensionBorder = -Float.MAX_VALUE;
            break;
        case DataBuffer.TYPE_DOUBLE:
        default:
            sourceExtensionBorder = -Double.MAX_VALUE;
        }

        // BorderExtender used for filling the image border with the above
        // sourceExtensionBorder
        this.sourceBorderExtender = sourceExtensionBorder == 0.0 ? BorderExtender
                .createInstance(BorderExtender.BORDER_ZERO) : new BorderExtenderConstant(
                new double[] { sourceExtensionBorder });

        // BorderExtender used for filling the ROI or alpha images border values.
        if (alphaPresent || roiPresent) {
            this.zeroBorderExtender = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);
        }

        // Stores the destination no data values.
        if (destinationNoData == null) {
            this.destinationNoData = DEFAULT_DESTINATION_NO_DATA_VALUE;
        } else {
            this.destinationNoData = new Number[numBands];
        }
        if (destinationNoData.length < numBands) {
            Arrays.fill(this.destinationNoData, destinationNoData[0]);
        } else {

            System.arraycopy(destinationNoData, 0, this.destinationNoData, 0, numBands);
        }

    }

    /**
     * This method overrides the OpImage compute tile method and calculates the mosaic operation for the selected tile.
     */
    public Raster computeTile(int tileX, int tileY) {
        // The destination raster is created as WritableRaster
        WritableRaster destRaster = createWritableRaster(sampleModel, new Point(tileXToX(tileX),
                tileYToY(tileY)));

        // This method calculates the tile active area.
        Rectangle destRectangle = getTileRect(tileX, tileY);
        // Stores the source image number
        int numSources = getNumSources();
        // Initialization of a new RasterBean for passing all the raster information
        // to the compute rect method
        RasterBean[] rasterBeanArray = new RasterBean[numSources];
        // The previous array is filled with the source raster data
        for (int i = 0; i < numSources; i++) {
            PlanarImage source = getSourceImage(i);
            Rectangle srcRect = mapDestRect(destRectangle, i);
            Raster data = srcRect != null && srcRect.isEmpty() ? null : source.getExtendedData(
                    destRectangle, sourceBorderExtender);

            // Raster bean initialization
            RasterBean tempBean = new RasterBean();
            tempBean.setDataRaster(data);
            tempBean.setSourceNoDataRangeRaster(imageBeans[i].getSourceNoData());
            rasterBeanArray[i] = tempBean;
            if (data != null) {
                PlanarImage alpha = imageBeans[i].getAlphaChannel();
                if (alphaPresent && alpha != null) {
                    rasterBeanArray[i].setAlphaRaster(alpha.getExtendedData(destRectangle,
                            zeroBorderExtender));
                }

                ROI roi = imageBeans[i].getImageRoi();
                if (roiPresent && roi != null) {
                    rasterBeanArray[i].setRoiRaster(roi.getAsImage().getExtendedData(destRectangle,
                            zeroBorderExtender));
                }
            }

        }
        // For the given source destination rasters, the mosaic is calculated
        computeRect(rasterBeanArray, destRaster, destRectangle);

        // Tile recycling if the Recycle is present
        for (int i = 0; i < numSources; i++) {
            Raster sourceData = rasterBeanArray[i].getDataRaster();
            if (sourceData != null) {
                PlanarImage source = getSourceImage(i);

                if (source.overlapsMultipleTiles(sourceData.getBounds())) {
                    recycleTile(sourceData);
                }
            }
        }

        return destRaster;

    }

    private void computeRect(RasterBean[] rasterBeanArray, WritableRaster destRaster,
            Rectangle destRectangle) {

        int sourcesNumber = rasterBeanArray.length;
        // Put all non-null sources in a list.
        ArrayList<Raster> listRasterSource = new ArrayList<Raster>(sourcesNumber);
        for (int i = 0; i < sourcesNumber; i++) {
            if (rasterBeanArray[i].getDataRaster() != null) {
                listRasterSource.add(rasterBeanArray[i].getDataRaster());
            }
        }

        // Fill with the destinationNoData and return if no sources.
        int notNullSources = listRasterSource.size();
        if (notNullSources == 0) {
            // conversion from numeric to double
            double[] noSourceFiller = new double[destinationNoData.length];
            for (int i = 0; i < destinationNoData.length; i++) {
                noSourceFiller[i] = destinationNoData[i].doubleValue();
            }
            ImageUtil.fillBackground(destRaster, destRectangle, noSourceFiller);
            return;
        }

        // All the sample models are stored for using a compatible RasterAccessor
        // Format Tag ID
        SampleModel[] sourceSampleModels = new SampleModel[notNullSources];
        for (int i = 0; i < notNullSources; i++) {
            sourceSampleModels[i] = ((Raster) listRasterSource.get(i)).getSampleModel();
        }

        // The best compatible formaTagID is returned from the sources and
        // destination sample models
        int rasterAccessFormatTagID = RasterAccessor.findCompatibleTag(sourceSampleModels,
                destRaster.getSampleModel());

        // Creates source accessors bean array (a new bean)
        RasterBeanAccessor[] sourceAccessorsArrayBean = new RasterBeanAccessor[sourcesNumber];
        // The above array is filled with image data, roi, alpha and no data ranges
        for (int i = 0; i < sourcesNumber; i++) {
            // RasterAccessorBean temporary file
            RasterBeanAccessor helpAccessor = new RasterBeanAccessor();
            if (rasterBeanArray[i].getDataRaster() != null) {
                RasterFormatTag formatTag = new RasterFormatTag(rasterBeanArray[i].getDataRaster()
                        .getSampleModel(), rasterAccessFormatTagID);

                helpAccessor.setDataRasterAccessor(new RasterAccessor(rasterBeanArray[i]
                        .getDataRaster(), destRectangle, formatTag, null));

            }
            Raster alphaRaster = rasterBeanArray[i].getAlphaRaster();
            if (alphaRaster != null) {

                SampleModel alphaSampleModel = alphaRaster.getSampleModel();
                int alphaFormatTagID = RasterAccessor.findCompatibleTag(null, alphaSampleModel);
                RasterFormatTag alphaFormatTag = new RasterFormatTag(alphaSampleModel,
                        alphaFormatTagID);
                helpAccessor.setAlphaRasterAccessor(new RasterAccessor(alphaRaster, destRectangle,
                        alphaFormatTag, imageBeans[i].getAlphaChannel().getColorModel()));
            }

            helpAccessor.setRoiRaster(rasterBeanArray[i].getRoiRaster());
            helpAccessor.setSourceNoDataRangeRasterAccessor(rasterBeanArray[i]
                    .getSourceNoDataRangeRaster());

            sourceAccessorsArrayBean[i] = helpAccessor;

        }

        // Create dest accessor.
        RasterAccessor destinationAccessor = new RasterAccessor(destRaster, destRectangle,
                new RasterFormatTag(destRaster.getSampleModel(), rasterAccessFormatTagID), null);
        // This method calculates the mosaic of the source images and stores the
        // result in the destination
        // accessor
        computeRectType(sourceAccessorsArrayBean, destinationAccessor,
                destinationAccessor.getDataType());

        // the data are copied back to the destination raster
        destinationAccessor.copyDataToRaster();

    }

    private void computeRectType(RasterBeanAccessor[] srcBean, RasterAccessor dst, int dataType) {
        // Stores the source number
        int sourcesNumber = srcBean.length;

        // From every source all the LineStride, PixelStride, LineOffsets,
        // PixelOffsets and Band Offset are initialized
        int[] srcLineStride = new int[sourcesNumber];
        int[] srcPixelStride = new int[sourcesNumber];
        int[][] srcBandOffsets = new int[sourcesNumber][];
        int[] sLineOffsets = new int[sourcesNumber];
        int[] sPixelOffsets = new int[sourcesNumber];

        // Source data creation with null values(for collect all the arrays tha are
        // not used)
        byte[][][] srcDataByte = null;
        short[][][] srcDataUShort = null;
        short[][][] srcDataShort = null;
        int[][][] srcDataInt = null;
        float[][][] srcDataFloat = null;
        double[][][] srcDataDouble = null;
        // Alpha Channel creation
        byte[][][] alfaDataByte = null;
        short[][][] alfaDataUShort = null;
        short[][][] alfaDataShort = null;
        int[][][] alfaDataInt = null;
        float[][][] alfaDataFloat = null;
        double[][][] alfaDataDouble = null;
        // Destination data creation
        byte[][] dstDataByte = null;
        short[][] dstDataUShort = null;
        short[][] dstDataShort = null;
        int[][] dstDataInt = null;
        float[][] dstDataFloat = null;
        double[][] dstDataDouble = null;
        // Source data per band creation
        byte[][] sBandDataByte = null;
        short[][] sBandDataUShort = null;
        short[][] sBandDataShort = null;
        int[][] sBandDataInt = null;
        float[][] sBandDataFloat = null;
        double[][] sBandDataDouble = null;
        // Alpha data per band creation
        byte[][] aBandDataByte = null;
        short[][] aBandDataUShort = null;
        short[][] aBandDataShort = null;
        int[][] aBandDataInt = null;
        float[][] aBandDataFloat = null;
        double[][] aBandDataDouble = null;

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
        int[] alfaLineStride = null;
        int[] alfaPixelStride = null;
        int[][] alfaBandOffsets = null;
        int[] aLineOffsets = null;
        int[] aPixelOffsets = null;

        if (alphaPresentinRaster) {
            // The above alpha arrays are allocated only if the alpha channel is
            // present
            alfaLineStride = new int[sourcesNumber];
            alfaPixelStride = new int[sourcesNumber];
            alfaBandOffsets = new int[sourcesNumber][];
            aLineOffsets = new int[sourcesNumber];
            aPixelOffsets = new int[sourcesNumber];
        }

        // All the source arrays are initialized only for one type of data
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            srcDataByte = new byte[sourcesNumber][][];
            dstDataByte = dst.getByteDataArrays();
            sBandDataByte = new byte[sourcesNumber][];

            if (alphaPresentinRaster) {
                alfaDataByte = new byte[sourcesNumber][][];
                aBandDataByte = new byte[sourcesNumber][];
            }
            break;
        case DataBuffer.TYPE_USHORT:
            srcDataUShort = new short[sourcesNumber][][];
            dstDataUShort = dst.getShortDataArrays();
            sBandDataUShort = new short[sourcesNumber][];

            if (alphaPresentinRaster) {
                alfaDataUShort = new short[sourcesNumber][][];
                aBandDataUShort = new short[sourcesNumber][];
            }
            break;
        case DataBuffer.TYPE_SHORT:
            srcDataShort = new short[sourcesNumber][][];
            dstDataShort = dst.getShortDataArrays();
            sBandDataShort = new short[sourcesNumber][];

            if (alphaPresentinRaster) {
                alfaDataShort = new short[sourcesNumber][][];
                aBandDataShort = new short[sourcesNumber][];
            }
            break;
        case DataBuffer.TYPE_INT:
            srcDataInt = new int[sourcesNumber][][];
            dstDataInt = dst.getIntDataArrays();
            sBandDataInt = new int[sourcesNumber][];

            if (alphaPresentinRaster) {
                alfaDataInt = new int[sourcesNumber][][];
                aBandDataInt = new int[sourcesNumber][];
            }
            break;
        case DataBuffer.TYPE_FLOAT:
            srcDataFloat = new float[sourcesNumber][][];
            dstDataFloat = dst.getFloatDataArrays();
            sBandDataFloat = new float[sourcesNumber][];

            if (alphaPresentinRaster) {
                alfaDataFloat = new float[sourcesNumber][][];
                aBandDataFloat = new float[sourcesNumber][];
            }
            break;
        case DataBuffer.TYPE_DOUBLE:
            srcDataDouble = new double[sourcesNumber][][];
            dstDataDouble = dst.getDoubleDataArrays();
            sBandDataDouble = new double[sourcesNumber][];

            if (alphaPresentinRaster) {
                alfaDataDouble = new double[sourcesNumber][][];
                aBandDataDouble = new double[sourcesNumber][];
            }
            break;
        }
        // The above arrays are filled with the data from the Java Raster
        // AcessorBean.
        for (int i = 0; i < sourcesNumber; i++) {
            if (srcBean[i].getDataRasterAccessor() != null) {
                srcLineStride[i] = srcBean[i].getDataRasterAccessor().getScanlineStride();
                srcPixelStride[i] = srcBean[i].getDataRasterAccessor().getPixelStride();
                srcBandOffsets[i] = srcBean[i].getDataRasterAccessor().getBandOffsets();

                switch (dataType) {
                case DataBuffer.TYPE_BYTE:
                    srcDataByte[i] = srcBean[i].getDataRasterAccessor().getByteDataArrays();
                    if (alphaPresentinRaster & srcBean[i].getAlphaRasterAccessor() != null) {
                        alfaDataByte[i] = srcBean[i].getAlphaRasterAccessor().getByteDataArrays();
                        alfaBandOffsets[i] = srcBean[i].getAlphaRasterAccessor().getBandOffsets();
                    }
                    break;
                case DataBuffer.TYPE_USHORT:
                    srcDataUShort[i] = srcBean[i].getDataRasterAccessor().getShortDataArrays();
                    if (alphaPresentinRaster & srcBean[i].getAlphaRasterAccessor() != null) {
                        alfaDataUShort[i] = srcBean[i].getAlphaRasterAccessor()
                                .getShortDataArrays();
                        alfaBandOffsets[i] = srcBean[i].getAlphaRasterAccessor().getBandOffsets();
                    }
                    break;
                case DataBuffer.TYPE_SHORT:
                    srcDataShort[i] = srcBean[i].getDataRasterAccessor().getShortDataArrays();
                    if (alphaPresentinRaster & srcBean[i].getAlphaRasterAccessor() != null) {
                        alfaDataShort[i] = srcBean[i].getAlphaRasterAccessor().getShortDataArrays();
                        alfaBandOffsets[i] = srcBean[i].getAlphaRasterAccessor().getBandOffsets();
                    }
                    break;
                case DataBuffer.TYPE_INT:
                    srcDataInt[i] = srcBean[i].getDataRasterAccessor().getIntDataArrays();
                    if (alphaPresentinRaster & srcBean[i].getAlphaRasterAccessor() != null) {
                        alfaDataInt[i] = srcBean[i].getAlphaRasterAccessor().getIntDataArrays();
                        alfaBandOffsets[i] = srcBean[i].getAlphaRasterAccessor().getBandOffsets();
                    }
                    break;
                case DataBuffer.TYPE_FLOAT:
                    srcDataFloat[i] = srcBean[i].getDataRasterAccessor().getFloatDataArrays();
                    if (alphaPresentinRaster & srcBean[i].getAlphaRasterAccessor() != null) {
                        alfaDataFloat[i] = srcBean[i].getAlphaRasterAccessor().getFloatDataArrays();
                        alfaBandOffsets[i] = srcBean[i].getAlphaRasterAccessor().getBandOffsets();
                    }
                    break;
                case DataBuffer.TYPE_DOUBLE:
                    srcDataDouble[i] = srcBean[i].getDataRasterAccessor().getDoubleDataArrays();
                    if (alphaPresentinRaster & srcBean[i].getAlphaRasterAccessor() != null) {
                        alfaDataDouble[i] = srcBean[i].getAlphaRasterAccessor()
                                .getDoubleDataArrays();
                        alfaBandOffsets[i] = srcBean[i].getAlphaRasterAccessor().getBandOffsets();
                    }
                    break;
                }
            }
        }

        // Destination information are taken from the destination accessor
        int dstMinX = dst.getX();
        int dstMinY = dst.getY();
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstMaxX = dstMinX + dstWidth;
        int dstMaxY = dstMinY + dstHeight;
        int dstBands = dst.getNumBands();
        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();

        // Weight type arrays can have different weight types if ROI or alpha
        // channel are present or not
        int[] weightTypesUsed = new int[sourcesNumber];
        for (int i = 0; i < sourcesNumber; i++) {
            weightTypesUsed[i] = WeightType.WEIGHT_TYPE_NODATA.valueData();
            if (srcBean[i].getAlphaRasterAccessor() != null) {
                weightTypesUsed[i] = WeightType.WEIGHT_TYPE_ALPHA.valueData();
            } else if (roiPresent && imageBeans[i].getImageRoi() != null) {
                weightTypesUsed[i] = WeightType.WEIGHT_TYPE_ROI.valueData();
            }
        }

        // COMPUTATION LEVEL

        for (int b = 0; b < dstBands; b++) { // For all the Bands
            // The data value are taken for every band
            for (int s = 0; s < sourcesNumber; s++) {
                if (srcBean[s].getDataRasterAccessor() != null) {

                    switch (dataType) {
                    case DataBuffer.TYPE_BYTE:
                        sBandDataByte[s] = srcDataByte[s][b];
                        break;
                    case DataBuffer.TYPE_USHORT:
                        sBandDataUShort[s] = srcDataUShort[s][b];
                        break;
                    case DataBuffer.TYPE_SHORT:
                        sBandDataShort[s] = srcDataShort[s][b];
                        break;
                    case DataBuffer.TYPE_INT:
                        sBandDataInt[s] = srcDataInt[s][b];
                        break;
                    case DataBuffer.TYPE_FLOAT:
                        sBandDataFloat[s] = srcDataFloat[s][b];
                        break;
                    case DataBuffer.TYPE_DOUBLE:
                        sBandDataDouble[s] = srcDataDouble[s][b];
                        break;
                    }
                    // The offset is initialized
                    sLineOffsets[s] = srcBandOffsets[s][b];
                }
                if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA.valueData()) {
                    // The alpha value are taken only from the first band (this
                    // happens because the raster
                    // accessor provides the data array with the band data even if
                    // the alpha channel has only
                    // one band.
                    switch (dataType) {
                    case DataBuffer.TYPE_BYTE:
                        aBandDataByte[s] = alfaDataByte[s][0];
                        break;
                    case DataBuffer.TYPE_USHORT:
                        aBandDataUShort[s] = alfaDataUShort[s][0];
                        break;
                    case DataBuffer.TYPE_SHORT:
                        aBandDataShort[s] = alfaDataShort[s][0];
                        break;
                    case DataBuffer.TYPE_INT:
                        aBandDataInt[s] = alfaDataInt[s][0];
                        break;
                    case DataBuffer.TYPE_FLOAT:
                        aBandDataFloat[s] = alfaDataFloat[s][0];
                        break;
                    case DataBuffer.TYPE_DOUBLE:
                        aBandDataDouble[s] = alfaDataDouble[s][0];
                        break;
                    }

                    aLineOffsets[s] = alfaBandOffsets[s][0];
                }
            }

            // The destination data band are first created and then initialized only
            // for
            // selected dataType
            byte[] dBandDataByte = null;
            short[] dBandDataUShort = null;
            short[] dBandDataShort = null;
            int[] dBandDataInt = null;
            float[] dBandDataFloat = null;
            double[] dBandDataDouble = null;

            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                dBandDataByte = dstDataByte[b];
                break;
            case DataBuffer.TYPE_USHORT:
                dBandDataUShort = dstDataUShort[b];
                break;
            case DataBuffer.TYPE_SHORT:
                dBandDataShort = dstDataShort[b];
                break;
            case DataBuffer.TYPE_INT:
                dBandDataInt = dstDataInt[b];
                break;
            case DataBuffer.TYPE_FLOAT:
                dBandDataFloat = dstDataFloat[b];
                break;
            case DataBuffer.TYPE_DOUBLE:
                dBandDataDouble = dstDataDouble[b];
                break;
            }
            // the destination lineOffset is initialized
            int dLineOffset = dstBandOffsets[b];

            if (mosaicTypeSelected == MosaicDescriptor.MOSAIC_TYPE_OVERLAY) {
                for (int dstY = dstMinY; dstY < dstMaxY; dstY++) { // For all the Y
                                                                   // values
                    // Source and pixel Offset are initialized and Source and alpha
                    // line offset are
                    // translated (cycle accross all the sources)
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
                            if (srcBean[s].getDataRasterAccessor() == null)
                                continue;
                            // The source valuse are initialized only for the switch
                            // method
                            byte sourceValueByte = 0;
                            short sourceValueUShort = 0;
                            short sourceValueShort = 0;
                            int sourceValueInt = 0;
                            float sourceValueFloat = 0;
                            double sourceValueDouble = 0;

                            switch (dataType) {
                            case DataBuffer.TYPE_BYTE:
                                sourceValueByte = sBandDataByte[s][sPixelOffsets[s]];
                                break;
                            case DataBuffer.TYPE_USHORT:
                                sourceValueUShort = sBandDataUShort[s][sPixelOffsets[s]];
                                break;
                            case DataBuffer.TYPE_SHORT:
                                sourceValueShort = sBandDataShort[s][sPixelOffsets[s]];
                                break;
                            case DataBuffer.TYPE_INT:
                                sourceValueInt = sBandDataInt[s][sPixelOffsets[s]];
                                break;
                            case DataBuffer.TYPE_FLOAT:
                                sourceValueFloat = sBandDataFloat[s][sPixelOffsets[s]];
                                break;
                            case DataBuffer.TYPE_DOUBLE:
                                sourceValueDouble = sBandDataDouble[s][sPixelOffsets[s]];
                                break;
                            }

                            sPixelOffsets[s] += srcPixelStride[s];

                            // the flag checks if the pixel is a noData
                            boolean isData = true;

                            switch (dataType) {
                            case DataBuffer.TYPE_BYTE:

                                Range<Byte> noDataRangeByte = ((Range<Byte>) srcBean[s]
                                        .getSourceNoDataRangeRasterAccessor());
                                if (noDataRangeByte != null) {
                                    isData = !noDataRangeByte
                                            .contains((byte) (sourceValueByte & 0xff));
                                }
                                break;
                            case DataBuffer.TYPE_USHORT:
                                Range<Short> noDataRangeUShort = ((Range<Short>) srcBean[s]
                                        .getSourceNoDataRangeRasterAccessor());
                                if (noDataRangeUShort != null) {
                                    isData = !noDataRangeUShort
                                            .contains((short) (sourceValueUShort & 0xffff));
                                }
                                break;
                            case DataBuffer.TYPE_SHORT:
                                Range<Short> noDataRangeShort = ((Range<Short>) srcBean[s]
                                        .getSourceNoDataRangeRasterAccessor());
                                if (noDataRangeShort != null) {
                                    isData = !noDataRangeShort.contains(sourceValueShort);
                                }
                                break;
                            case DataBuffer.TYPE_INT:
                                Range<Integer> noDataRangeInt = ((Range<Integer>) srcBean[s]
                                        .getSourceNoDataRangeRasterAccessor());
                                if (noDataRangeInt != null) {
                                    isData = !noDataRangeInt.contains(sourceValueInt);
                                }
                                break;
                            case DataBuffer.TYPE_FLOAT:
                                Range<Float> noDataRangeFloat = ((Range<Float>) srcBean[s]
                                        .getSourceNoDataRangeRasterAccessor());
                                if (noDataRangeFloat != null) {
                                    isData = !noDataRangeFloat.contains(sourceValueFloat);
                                }
                                break;
                            case DataBuffer.TYPE_DOUBLE:
                                Range<Double> noDataRangeDouble = ((Range<Double>) srcBean[s]
                                        .getSourceNoDataRangeRasterAccessor());
                                if (noDataRangeDouble != null) {
                                    isData = !noDataRangeDouble.contains(sourceValueDouble);
                                }
                                break;
                            }

                            if (!isData) {
                                setDestinationFlag = false;
                            } else {

                                switch (weightTypesUsed[s]) {
                                case 1:
                                    switch (dataType) {
                                    case DataBuffer.TYPE_BYTE:
                                        setDestinationFlag = aBandDataByte[s][aPixelOffsets[s]] != 0;
                                        break;
                                    case DataBuffer.TYPE_USHORT:
                                        setDestinationFlag = aBandDataUShort[s][aPixelOffsets[s]] != 0;
                                        break;
                                    case DataBuffer.TYPE_SHORT:
                                        setDestinationFlag = aBandDataShort[s][aPixelOffsets[s]] != 0;
                                        break;
                                    case DataBuffer.TYPE_INT:
                                        setDestinationFlag = aBandDataInt[s][aPixelOffsets[s]] != 0;
                                        break;
                                    case DataBuffer.TYPE_FLOAT:
                                        setDestinationFlag = aBandDataFloat[s][aPixelOffsets[s]] != 0;
                                        break;
                                    case DataBuffer.TYPE_DOUBLE:
                                        setDestinationFlag = aBandDataDouble[s][aPixelOffsets[s]] != 0;
                                        break;
                                    }

                                    aPixelOffsets[s] += alfaPixelStride[s];
                                    break;
                                case 2:
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

                                switch (dataType) {
                                case DataBuffer.TYPE_BYTE:
                                    dBandDataByte[dPixelOffset] = sourceValueByte;
                                    break;
                                case DataBuffer.TYPE_USHORT:
                                    dBandDataUShort[dPixelOffset] = sourceValueUShort;
                                    break;
                                case DataBuffer.TYPE_SHORT:
                                    dBandDataShort[dPixelOffset] = sourceValueShort;
                                    break;
                                case DataBuffer.TYPE_INT:
                                    dBandDataInt[dPixelOffset] = sourceValueInt;
                                    break;
                                case DataBuffer.TYPE_FLOAT:
                                    dBandDataFloat[dPixelOffset] = sourceValueFloat;
                                    break;
                                case DataBuffer.TYPE_DOUBLE:
                                    dBandDataDouble[dPixelOffset] = sourceValueDouble;
                                    break;
                                }

                                for (int k = s + 1; k < sourcesNumber; k++) {
                                    if (srcBean[k].getDataRasterAccessor() != null) {
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

                            switch (dataType) {
                            case DataBuffer.TYPE_BYTE:
                                dBandDataByte[dPixelOffset] = destinationNoData[b].byteValue();
                                break;
                            case DataBuffer.TYPE_USHORT:
                                dBandDataUShort[dPixelOffset] = destinationNoData[b].shortValue();
                                break;
                            case DataBuffer.TYPE_SHORT:
                                dBandDataShort[dPixelOffset] = destinationNoData[b].shortValue();
                                break;
                            case DataBuffer.TYPE_INT:
                                dBandDataInt[dPixelOffset] = destinationNoData[b].intValue();
                                break;
                            case DataBuffer.TYPE_FLOAT:
                                dBandDataFloat[dPixelOffset] = destinationNoData[b].floatValue();
                                break;
                            case DataBuffer.TYPE_DOUBLE:
                                dBandDataDouble[dPixelOffset] = destinationNoData[b].doubleValue();
                                break;
                            }
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
                        if (weightTypesUsed[s] == WeightType.WEIGHT_TYPE_ALPHA.valueData()) {
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
                            if (srcBean[s].getDataRasterAccessor() == null)
                                continue;
                            // The source valuse are initialized only for the switch
                            // method
                            byte sourceValueByte = 0;
                            short sourceValueUShort = 0;
                            short sourceValueShort = 0;
                            int sourceValueInt = 0;
                            float sourceValueFloat = 0;
                            double sourceValueDouble = 0;

                            switch (dataType) {
                            case DataBuffer.TYPE_BYTE:
                                sourceValueByte = sBandDataByte[s][sPixelOffsets[s]];
                                break;
                            case DataBuffer.TYPE_USHORT:
                                sourceValueUShort = sBandDataUShort[s][sPixelOffsets[s]];
                                break;
                            case DataBuffer.TYPE_SHORT:
                                sourceValueShort = sBandDataShort[s][sPixelOffsets[s]];
                                break;
                            case DataBuffer.TYPE_INT:
                                sourceValueInt = sBandDataInt[s][sPixelOffsets[s]];
                                break;
                            case DataBuffer.TYPE_FLOAT:
                                sourceValueFloat = sBandDataFloat[s][sPixelOffsets[s]];
                                break;
                            case DataBuffer.TYPE_DOUBLE:
                                sourceValueDouble = sBandDataDouble[s][sPixelOffsets[s]];
                                break;
                            }
                            // Offset update
                            sPixelOffsets[s] += srcPixelStride[s];
                            // The weight is calculated for every pixel
                            double weight = 0.0F;

                            boolean isData = true;

                            // If no alpha channel or Roi is present, the weight
                            // is set to 1 or 0 if the pixel has
                            // or not a No Data value
                            switch (dataType) {
                            case DataBuffer.TYPE_BYTE:

                                Range<Byte> noDataRangeByte = ((Range<Byte>) srcBean[s]
                                        .getSourceNoDataRangeRasterAccessor());
                                if (noDataRangeByte != null) {
                                    isData = !noDataRangeByte
                                            .contains((byte) (sourceValueByte & 0xff));
                                }

                                break;
                            case DataBuffer.TYPE_USHORT:

                                Range<Short> noDataRangeUShort = ((Range<Short>) srcBean[s]
                                        .getSourceNoDataRangeRasterAccessor());
                                if (noDataRangeUShort != null) {
                                    isData = !noDataRangeUShort
                                            .contains((short) (sourceValueUShort & 0xffff));
                                }
                                break;
                            case DataBuffer.TYPE_SHORT:

                                Range<Short> noDataRangeShort = ((Range<Short>) srcBean[s]
                                        .getSourceNoDataRangeRasterAccessor());
                                if (noDataRangeShort != null) {
                                    isData = !noDataRangeShort.contains(sourceValueShort);
                                }

                                break;
                            case DataBuffer.TYPE_INT:

                                Range<Integer> noDataRangeInt = ((Range<Integer>) srcBean[s]
                                        .getSourceNoDataRangeRasterAccessor());
                                if (noDataRangeInt != null) {
                                    isData = !noDataRangeInt.contains(sourceValueInt);
                                }
                                break;
                            case DataBuffer.TYPE_FLOAT:

                                Range<Float> noDataRangeFloat = ((Range<Float>) srcBean[s]
                                        .getSourceNoDataRangeRasterAccessor());
                                if (noDataRangeFloat != null) {
                                    isData = !noDataRangeFloat.contains(sourceValueFloat);
                                }
                                break;
                            case DataBuffer.TYPE_DOUBLE:

                                Range<Double> noDataRangeDouble = ((Range<Double>) srcBean[s]
                                        .getSourceNoDataRangeRasterAccessor());
                                if (noDataRangeDouble != null) {
                                    isData = !noDataRangeDouble.contains(sourceValueDouble);
                                }
                                break;
                            }

                            if (!isData) {
                                weight = 0F;
                            } else {

                                switch (weightTypesUsed[s]) {
                                case 1:
                                    switch (dataType) {
                                    case DataBuffer.TYPE_BYTE:
                                        weight = (aBandDataByte[s][aPixelOffsets[s]] & 0xff);
                                        if (weight > 0.0F && isAlphaBitmaskUsed) {
                                            weight = 1.0F;
                                        } else {
                                            weight /= 255.0F;
                                        }
                                        break;
                                    case DataBuffer.TYPE_USHORT:
                                        weight = (aBandDataUShort[s][aPixelOffsets[s]] & 0xffff);
                                        if (weight > 0.0F && isAlphaBitmaskUsed) {
                                            weight = 1.0F;
                                        } else {
                                            weight /= 65535.0F;
                                        }
                                        break;
                                    case DataBuffer.TYPE_SHORT:
                                        weight = (aBandDataShort[s][aPixelOffsets[s]] & 0xffff);
                                        if (weight > 0.0F && isAlphaBitmaskUsed) {
                                            weight = 1.0F;
                                        } else {
                                            weight /= (double) Short.MAX_VALUE;
                                            ;
                                        }
                                        break;
                                    case DataBuffer.TYPE_INT:
                                        weight = (aBandDataInt[s][aPixelOffsets[s]]);
                                        if (weight > 0.0F && isAlphaBitmaskUsed) {
                                            weight = 1.0F;
                                        } else {
                                            weight /= Integer.MAX_VALUE;
                                        }
                                        break;
                                    case DataBuffer.TYPE_FLOAT:
                                        weight = (aBandDataFloat[s][aPixelOffsets[s]]);
                                        if (weight > 0.0F && isAlphaBitmaskUsed) {
                                            weight = 1.0F;
                                        }
                                        break;
                                    case DataBuffer.TYPE_DOUBLE:
                                        weight = (aBandDataDouble[s][aPixelOffsets[s]]);
                                        if (weight > 0.0F && isAlphaBitmaskUsed) {
                                            weight = 1.0F;
                                        }
                                        break;
                                    }

                                    aPixelOffsets[s] += alfaPixelStride[s];
                                    break;
                                case 2:
                                    weight = srcBean[s].getRoiRaster().getSample(dstX, dstY, 0) > 0 ? 1.0F
                                            : 0.0F;

                                    break;

                                default:
                                    weight = 1.0F;
                                }
                            }
                            // The above calculated weight are added to the
                            // numerator and denominator

                            switch (dataType) {
                            case DataBuffer.TYPE_BYTE:
                                numerator += (weight * (sourceValueByte & 0xff));
                                break;
                            case DataBuffer.TYPE_USHORT:
                                numerator += (weight * (sourceValueUShort & 0xffff));
                                break;
                            case DataBuffer.TYPE_SHORT:
                                numerator += (weight * (sourceValueShort));
                                break;
                            case DataBuffer.TYPE_INT:
                                numerator += (weight * (sourceValueInt));
                                break;
                            case DataBuffer.TYPE_FLOAT:
                                numerator += (weight * (sourceValueFloat));
                                break;
                            case DataBuffer.TYPE_DOUBLE:
                                numerator += (weight * (sourceValueDouble));
                                break;
                            }

                            denominator += weight;
                        }

                        // If the weighted sum is 0 the destination pixel value
                        // takes the destination no data.
                        // If the sum is not 0 the value is added to the related
                        // destination pixel

                        if (denominator == 0.0) {
                            switch (dataType) {
                            case DataBuffer.TYPE_BYTE:
                                dBandDataByte[dPixelOffset] = destinationNoData[b].byteValue();
                                break;
                            case DataBuffer.TYPE_USHORT:
                                dBandDataUShort[dPixelOffset] = destinationNoData[b].shortValue();
                                break;
                            case DataBuffer.TYPE_SHORT:
                                dBandDataShort[dPixelOffset] = destinationNoData[b].shortValue();
                                break;
                            case DataBuffer.TYPE_INT:
                                dBandDataInt[dPixelOffset] = destinationNoData[b].intValue();
                                break;
                            case DataBuffer.TYPE_FLOAT:
                                dBandDataFloat[dPixelOffset] = destinationNoData[b].floatValue();
                                break;
                            case DataBuffer.TYPE_DOUBLE:
                                dBandDataDouble[dPixelOffset] = destinationNoData[b].doubleValue();
                                break;
                            }

                        } else {

                            switch (dataType) {
                            case DataBuffer.TYPE_BYTE:
                                dBandDataByte[dPixelOffset] = ImageUtil.clampRoundByte(numerator
                                        / denominator);
                                break;
                            case DataBuffer.TYPE_USHORT:
                                dBandDataUShort[dPixelOffset] = ImageUtil
                                        .clampRoundUShort(numerator / denominator);
                                break;
                            case DataBuffer.TYPE_SHORT:
                                dBandDataShort[dPixelOffset] = ImageUtil.clampRoundShort(numerator
                                        / denominator);
                                break;
                            case DataBuffer.TYPE_INT:
                                dBandDataInt[dPixelOffset] = ImageUtil.clampRoundInt(numerator
                                        / denominator);
                                break;
                            case DataBuffer.TYPE_FLOAT:
                                dBandDataFloat[dPixelOffset] = (float) (numerator / denominator);

                                break;
                            case DataBuffer.TYPE_DOUBLE:
                                dBandDataDouble[dPixelOffset] = numerator / denominator;

                                break;
                            }
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

    /** Java bean for saving all the raster informations */
    private static class RasterBean {
        // Raster of image data
        private Raster dataRaster;

        // alpha raster data
        private Raster alphaRaster;

        // Roi raster data
        private Raster roiRaster;

        // No data range
        private Range sourceNoDataRangeRaster;

        // The methods below are setter and getter for every field as requested for the
        // java beans

        public Raster getDataRaster() {
            return dataRaster;
        }

        public void setDataRaster(Raster dataRaster) {
            this.dataRaster = dataRaster;
        }

        public Raster getAlphaRaster() {
            return alphaRaster;
        }

        public void setAlphaRaster(Raster alphaRaster) {
            this.alphaRaster = alphaRaster;
        }

        public Raster getRoiRaster() {
            return roiRaster;
        }

        public void setRoiRaster(Raster roiRaster) {
            this.roiRaster = roiRaster;
        }

        public Range getSourceNoDataRangeRaster() {
            return sourceNoDataRangeRaster;
        }

        public void setSourceNoDataRangeRaster(Range sourceNoDataRangeRaster) {
            this.sourceNoDataRangeRaster = sourceNoDataRangeRaster;
        }

        // No-argument constructor as requested for the java beans
        RasterBean() {

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
