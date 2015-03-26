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
package it.geosolutions.jaiext.orderdither;

import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;

import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import javax.media.jai.ColorCube;
import javax.media.jai.ImageLayout;
import javax.media.jai.KernelJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.PointOpImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFactory;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.iterator.RandomIter;

import com.sun.media.jai.util.ImageUtil;
import com.sun.media.jai.util.JDKWorkarounds;

/**
 * An <code>OpImage</code> implementing the ordered dither operation as described in <code>OrderedDitherDescriptor</code>.
 * 
 * <p>
 * This <code>OpImage</code> performs dithering of its source image into a single band image using a specified color cube and dither mask. Optional
 * ROI and NoData will be taken into account during processing.
 */
public class OrderedDitherOpImage extends PointOpImage {

    /**
     * Constant indicating that the inner random iterators must pre-calculate an array of the image positions
     */
    public static final boolean ARRAY_CALC = true;

    /**
     * Constant indicating that the inner random iterators must cache the current tile position
     */
    public static final boolean TILE_CACHED = true;

    /**
     * Flag indicating that the generic implementation is used.
     */
    private static final int TYPE_OD_GENERAL = 0;

    /**
     * Flag indicating that the optimized three-band implementation is used (byte data only).
     */
    private static final int TYPE_OD_BYTE_LUT_3BAND = 1;

    /**
     * Flag indicating that the optimized N-band implementation is used (byte data only).
     */
    private static final int TYPE_OD_BYTE_LUT_NBAND = 2;

    /**
     * Maximim dither LUT size: 16x16 4-band byte dither mask.
     */
    private static final int DITHER_LUT_LENGTH_MAX = 16 * 16 * 4 * 256;

    /**
     * The maximum number of elements in the <code>DitherLUT</code> cache.
     */
    private static final int DITHER_LUT_CACHE_LENGTH_MAX = 4;

    /**
     * A cache of <code>SoftReference</code>s to <code>DitherLUT</code> inner class instances.
     */
    private static ArrayList ditherLUTCache = new ArrayList(DITHER_LUT_CACHE_LENGTH_MAX);

    /**
     * Flag indicating the implementation to be used.
     */
    private int odType = TYPE_OD_GENERAL;

    /**
     * The number of bands in the source image.
     */
    protected int numBands;

    /**
     * The array of color cube dimensions-less-one.
     */
    protected int[] dims;

    /**
     * The array of color cube multipliers.
     */
    protected int[] mults;

    /**
     * The adjusted offset of the color cube.
     */
    protected int adjustedOffset;

    /**
     * The width of the dither mask.
     */
    protected int maskWidth;

    /**
     * The height of the dither mask.
     */
    protected int maskHeight;

    /**
     * The dither mask matrix scaled by 255.
     */
    protected byte[][] maskDataByte;

    /**
     * The dither mask matrix scaled to USHORT range.
     */
    protected int[][] maskDataInt;

    /**
     * The dither mask matrix scaled to "unsigned int" range.
     */
    protected long[][] maskDataLong;

    /**
     * The dither mask matrix.
     */
    protected float[][] maskDataFloat;

    /**
     * An inner class instance representing a dither lookup table. Used for byte data only when the table size is within a specified limit.
     */
    protected DitherLUT odLUT = null;

    /** Boolean parameter indicating whether NoData range is present */
    private final boolean hasNoData;

    /** Boolean parameter indicating whether ROI is present */
    private final boolean hasROI;

    /** Nodata Range used for checking if input nodata are present */
    private Range nodata;

    /** ROI object used for reducing computation area */
    private ROI roi;

    /** Rectangle containing ROI bounds */
    private Rectangle roiBounds;

    /** Boolean indicating if No Data and ROI are not used */
    private boolean caseA;

    /** Boolean indicating if only the ROI is used */
    private boolean caseB;

    /** Boolean indicating if only the No Data are used */
    private boolean caseC;

    /** {@link PlanarImage} containg ROI data */
    private PlanarImage roiImage;

    /** Output value for NoData */
    private double destNoData;

    /** LookupTable used for a quick check on the input NoData */
    private boolean[] lut;

    /**
     * Force the destination image to be single-banded.
     */
    private static ImageLayout layoutHelper(ImageLayout layout, RenderedImage source,
            ColorCube colorMap) {
        ImageLayout il;
        if (layout == null) {
            il = new ImageLayout(source);
        } else {
            il = (ImageLayout) layout.clone();
        }

        // Get the SampleModel.
        SampleModel sm = il.getSampleModel(source);

        // Ensure an appropriate SampleModel.
        if (colorMap.getNumBands() == 1 && colorMap.getNumEntries() == 2
                && !ImageUtil.isBinary(il.getSampleModel(source))) {
            sm = new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE, il.getTileWidth(source),
                    il.getTileHeight(source), 1);
            il.setSampleModel(sm);
        }

        // Make sure that this OpImage is single-banded.
        if (sm.getNumBands() != 1) {
            // TODO: Force to SHORT or USHORT if FLOAT or DOUBLE?
            sm = RasterFactory.createComponentSampleModel(sm, sm.getTransferType(), sm.getWidth(),
                    sm.getHeight(), 1);
            il.setSampleModel(sm);

            // Clear the ColorModel mask if needed.
            ColorModel cm = il.getColorModel(null);
            if (cm != null && !JDKWorkarounds.areCompatibleDataModels(sm, cm)) {
                // Clear the mask bit if incompatible.
                il.unsetValid(ImageLayout.COLOR_MODEL_MASK);
            }
        }

        // Set an IndexColorModel on the image if:
        // a. none is provided in the layout;
        // b. source, destination, and colormap have byte data type;
        // c. the colormap has 3 bands; and
        // d. the source ColorModel is either null or is non-null
        // and has a ColorSpace equal to CS_sRGB.
        if ((layout == null || !il.isValid(ImageLayout.COLOR_MODEL_MASK))
                && source.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE
                && il.getSampleModel(null).getDataType() == DataBuffer.TYPE_BYTE
                && colorMap.getDataType() == DataBuffer.TYPE_BYTE && colorMap.getNumBands() == 3) {
            ColorModel cm = source.getColorModel();
            if (cm == null || (cm != null && cm.getColorSpace().isCS_sRGB())) {
                int size = colorMap.getNumEntries();
                byte[][] cmap = new byte[3][256];
                for (int i = 0; i < 3; i++) {
                    byte[] band = cmap[i];
                    byte[] data = colorMap.getByteData(i);
                    int offset = colorMap.getOffset(i);
                    int end = offset + size;
                    for (int j = 0; j < offset; j++) {
                        band[j] = (byte) 0;
                    }
                    for (int j = offset; j < end; j++) {
                        band[j] = data[j - offset];
                    }
                    for (int j = end; j < 256; j++) {
                        band[j] = (byte) 0xFF;
                    }
                }

                il.setColorModel(new IndexColorModel(8, 256, cmap[0], cmap[1], cmap[2]));
            }
        }

        return il;
    }

    /**
     * Constructs an OrderedDitherOpImage object. May be used to convert a single- or multi-band image into a single-band image with a color map.
     * 
     * <p>
     * The image dimensions are derived from the source image. The tile grid layout, SampleModel, and ColorModel may optionally be specified by an
     * ImageLayout object.
     * 
     * @param source A RenderedImage.
     * @param layout An ImageLayout optionally containing the tile grid layout, SampleModel, and ColorModel, or null.
     * @param colorMap The color map to use which must have a number of bands equal to the number of bands in the source image. The offset of this
     *        <code>ColorCube</code> must be the same for all bands.
     * @param ditherMask An an array of <code>KernelJAI</code> objects the dimension of which must equal the number of bands in the source image. The
     *        <i>n</i>th element of the array contains a <code>KernelJAI</code> object which represents the dither mask matrix for the corresponding
     *        band. All <code>KernelJAI</code> objects in the array must have the same dimensions and contain floating point values between 0.0F and
     *        1.0F.
     * @param roi Optional {@link ROI} used for masking raster areas
     * @param nodata NoData {@link Range} used for masking unwanted pixel values
     * @param destNoData Value to set as background
     */
    public OrderedDitherOpImage(RenderedImage source, Map config, ImageLayout layout,
            ColorCube colorMap, KernelJAI[] ditherMask, ROI roi, Range nodata, double destNoData) {
        // Construct as a PointOpImage.
        super(source, layoutHelper(layout, source, colorMap), config, true);

        // Initialize the instance variables derived from the color map.
        numBands = colorMap.getNumBands();
        mults = (int[]) colorMap.getMultipliers().clone();
        dims = (int[]) colorMap.getDimsLessOne().clone();
        adjustedOffset = colorMap.getAdjustedOffset();

        // Initialize the instance variables derived from the dither mask.
        maskWidth = ditherMask[0].getWidth();
        maskHeight = ditherMask[0].getHeight();

        // Check for NoData
        hasNoData = nodata != null;
        if (hasNoData) {
            this.nodata = nodata;
        }

        // Check for ROI
        hasROI = roi != null;
        if (hasROI) {
            this.roi = roi;
            this.roiBounds = roi.getBounds();
        }

        // Define the destination NoData value
        if ((hasROI || hasNoData) && destNoData < adjustedOffset) {
            throw new IllegalArgumentException(
                    "Destination NoData must be greater than the adjustedOffset value");
        }
        this.destNoData = destNoData;

        // Definition of the possible cases that can be found
        // caseA = no ROI nor No Data
        // caseB = ROI present but No Data not present
        // caseC = No Data present but ROI not present
        // Last case not defined = both ROI and No Data are present
        caseA = !hasROI && !hasNoData;
        caseB = hasROI && !hasNoData;
        caseC = !hasROI && hasNoData;

        // Initialize the data required to effect the operation.
        // XXX Postpone until first invocation of computeRect()?
        initializeDitherData(sampleModel.getTransferType(), ditherMask);

        // Set flag to permit in-place operation.
        permitInPlaceOperation();

        // Init the nodata lookup table for byte data
        if (hasNoData && sampleModel.getTransferType() == DataBuffer.TYPE_BYTE) {
            initNoDataLUT();
        }
    }

    /**
     * Initialization of the LookupTable used for checking if a byte value is a NoData
     */
    private void initNoDataLUT() {
        // init the table
        lut = new boolean[256];
        // Populate it
        for (int i = 0; i < 256; i++) {
            byte b = (byte) i;
            lut[i] = !nodata.contains(b);
        }
    }

    /**
     * An inner class represting a lookup table to be used in the optimized implementations of ordered dithering of byte data.
     */
    class DitherLUT {
        // Clones of color cube and dither mask data used to create the
        // dithering lookup table.
        private int[] dimsCache;

        private int[] multsCache;

        private byte[][] maskDataCache;

        // Stride values of the dither lookup table.
        public int ditherLUTBandStride;

        public int ditherLUTRowStride;

        public int ditherLUTColStride;

        // The dither lookup table.
        public byte[] ditherLUT;

        /**
         * Create an inner class object representing an ordered dither lookup table for byte data.
         * 
         * @param dims The color cube dimensions less one.
         * @param mults The color cube multipliers.
         * @param maskData The dither mask data scaled to byte range.
         */
        DitherLUT(int[] dims, int[] mults, byte[][] maskData) {
            // Clone the constructor parameters.
            dimsCache = (int[]) dims.clone();
            multsCache = (int[]) mults.clone();
            maskDataCache = new byte[maskData.length][];
            for (int i = 0; i < maskData.length; i++) {
                maskDataCache[i] = (byte[]) maskData[i].clone();
            }

            // Set dither lookup table stride values.
            ditherLUTColStride = 256;
            ditherLUTRowStride = maskWidth * ditherLUTColStride;
            ditherLUTBandStride = maskHeight * ditherLUTRowStride;

            //
            // Construct the big dither table. If indexed as a
            // multi-dimensional array this would be equivalent to:
            //
            // ditherLUT[band][ditherRow][ditherColumn][grayLevel]
            //
            // where ditherRow, Col are modulo the dither mask size.
            //
            // To minimize the table construction cost, precalculate
            // the bin value for a given band and gray level. Then use
            // the dithermask threshold value to determine whether to bump
            // the value up one level. Thus most of the work is done in the
            // outer loops, with a simple comparison left for the inner loop.
            //
            ditherLUT = new byte[numBands * ditherLUTBandStride];

            int pDithBand = 0;
            int maskSize2D = maskWidth * maskHeight;
            for (int band = 0; band < numBands; band++) {
                int step = dims[band];
                int delta = mults[band];
                byte[] maskDataBand = maskData[band];
                int sum = 0;
                for (int gray = 0; gray < 256; gray++) {
                    int tmp = sum;
                    int frac = (int) (tmp & 0xff);
                    int bin = tmp >> 8;
                    int lowVal = bin * delta;
                    int highVal = lowVal + delta;
                    int pDith = pDithBand + gray;
                    for (int dcount = 0; dcount < maskSize2D; dcount++) {
                        int threshold = maskDataBand[dcount] & 0xff;
                        if (frac > threshold) {
                            ditherLUT[pDith] = (byte) (highVal & 0xff);
                        } else {
                            ditherLUT[pDith] = (byte) (lowVal & 0xff);
                        }
                        pDith += 256;
                    } // end dithermask entry
                    sum += step;
                } // end gray level
                pDithBand += ditherLUTBandStride;
            } // end band
        }

        /**
         * Determine whether the internal table of this <code>DitherLUT</code> is the same as that which would be generated using the supplied
         * parameters.
         * 
         * @param dims The color cube dimensions less one.
         * @param mults The color cube multipliers.
         * @param maskData The dither mask data scaled to byte range.
         * 
         * @return Value indicating equivalence of dither LUTs.
         */
        public boolean equals(int[] dims, int[] mults, byte[][] maskData) {
            // Check dimensions.
            if (dims.length != dimsCache.length) {
                return false;
            }

            for (int i = 0; i < dims.length; i++) {
                if (dims[i] != dimsCache[i])
                    return false;
            }

            // Check multipliers.
            if (mults.length != multsCache.length) {
                return false;
            }

            for (int i = 0; i < mults.length; i++) {
                if (mults[i] != multsCache[i])
                    return false;
            }

            // Check dither mask.
            if (maskData.length != maskDataByte.length) {
                return false;
            }

            for (int i = 0; i < maskData.length; i++) {
                if (maskData[i].length != maskDataCache[i].length)
                    return false;
                byte[] refData = maskDataCache[i];
                byte[] data = maskData[i];
                for (int j = 0; j < maskData[i].length; j++) {
                    if (data[j] != refData[j])
                        return false;
                }
            }

            return true;
        }
    } // End inner class DitherLUT.

    /**
     * Initialize data type-dependent fields including the dither mask data arrays and, for optimized byte cases, the dither lookup table object.
     * 
     * @param dataType The data type as defined in <code>DataBuffer</code>.
     * @param ditherMask The dither mask represented as an array of <code>KernelJAI</code> objects.
     */
    private void initializeDitherData(int dataType, KernelJAI[] ditherMask) {
        switch (dataType) {
        case DataBuffer.TYPE_BYTE: {
            maskDataByte = new byte[ditherMask.length][];
            for (int i = 0; i < maskDataByte.length; i++) {
                float[] maskData = ditherMask[i].getKernelData();
                maskDataByte[i] = new byte[maskData.length];
                for (int j = 0; j < maskData.length; j++) {
                    maskDataByte[i][j] = (byte) ((int) (maskData[j] * 255.0F) & 0xff);
                }
            }

            initializeDitherLUT();
        }
            break;

        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT: {
            int scaleFactor = (int) Short.MAX_VALUE - (int) Short.MIN_VALUE;
            maskDataInt = new int[ditherMask.length][];
            for (int i = 0; i < maskDataInt.length; i++) {
                float[] maskData = ditherMask[i].getKernelData();
                maskDataInt[i] = new int[maskData.length];
                for (int j = 0; j < maskData.length; j++) {
                    maskDataInt[i][j] = (int) (maskData[j] * scaleFactor);
                }
            }
        }
            break;

        case DataBuffer.TYPE_INT: {
            long scaleFactor = (long) Integer.MAX_VALUE - (long) Integer.MIN_VALUE;
            maskDataLong = new long[ditherMask.length][];
            for (int i = 0; i < maskDataLong.length; i++) {
                float[] maskData = ditherMask[i].getKernelData();
                maskDataLong[i] = new long[maskData.length];
                for (int j = 0; j < maskData.length; j++) {
                    maskDataLong[i][j] = (long) (maskData[j] * scaleFactor);
                }
            }
        }
            break;

        case DataBuffer.TYPE_FLOAT:
        case DataBuffer.TYPE_DOUBLE: {
            maskDataFloat = new float[ditherMask.length][];
            for (int i = 0; i < maskDataFloat.length; i++) {
                maskDataFloat[i] = ditherMask[i].getKernelData();
            }
        }
            break;

        default:
            throw new RuntimeException(JaiI18N.getString("OrderedDitherOpImage0"));
        }
    }

    /**
     * For byte data only, initialize the dither lookup table if it is small enough and set the type of ordered dither implementation to use.
     */
    private synchronized void initializeDitherLUT() {
        // Check whether a DitherLUT may be used.
        if (numBands * maskHeight * maskWidth * 256 > DITHER_LUT_LENGTH_MAX) {
            odType = TYPE_OD_GENERAL; // NB: This is superfluous.
            return;
        }

        // If execution has proceeded to this point then this is one of the
        // optimized cases so set the type flag accordingly.
        odType = numBands == 3 ? TYPE_OD_BYTE_LUT_3BAND : TYPE_OD_BYTE_LUT_NBAND;

        // Check whether an equivalent DitherLUT object already exists.
        int index = 0;
        while (index < ditherLUTCache.size()) {
            SoftReference lutRef = (SoftReference) ditherLUTCache.get(index);
            DitherLUT lut = (DitherLUT) lutRef.get();
            if (lut == null) {
                // The reference has been cleared: remove the Vector element
                // but do not increment the loop index.
                ditherLUTCache.remove(index);
            } else {
                if (lut.equals(dims, mults, maskDataByte)) {
                    // Found an equivalent DitherLUT so use it and exit loop.
                    odLUT = lut;
                    break;
                }
                // Move on to the next Vector element.
                index++;
            }
        }

        // Create a new DitherLUT if an equivalent one was not found.
        if (odLUT == null) {
            odLUT = new DitherLUT(dims, mults, maskDataByte);
            // Cache a reference to the DitherLUT if there is room.
            if (ditherLUTCache.size() < DITHER_LUT_CACHE_LENGTH_MAX) {
                ditherLUTCache.add(new SoftReference(odLUT));
            }
        }
    }

    /**
     * Computes a tile of the dithered destination image.
     * 
     * @param sources Cobbled sources, guaranteed to provide all the source data necessary for computing the rectangle.
     * @param dest The tile containing the rectangle to be computed.
     * @param destRect The rectangle within the tile to be computed.
     */
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {

        // ROI check
        ROI roiTile = null;

        RandomIter roiIter = null;

        boolean roiContainsTile = false;
        boolean roiDisjointTile = false;

        // If a ROI is present, then only the part contained inside the current
        // tile bounds is taken.
        if (hasROI) {
            Rectangle srcRectExpanded = mapDestRect(destRect, 0);
            // The tile dimension is extended for avoiding border errors
            srcRectExpanded.setRect(srcRectExpanded.getMinX() - 1, srcRectExpanded.getMinY() - 1,
                    srcRectExpanded.getWidth() + 2, srcRectExpanded.getHeight() + 2);
            roiTile = roi.intersect(new ROIShape(srcRectExpanded));

            if (!roiBounds.intersects(srcRectExpanded)) {
                roiDisjointTile = true;
            } else {
                roiContainsTile = roiTile.contains(srcRectExpanded);
                if (!roiContainsTile) {
                    if (!roiTile.intersects(srcRectExpanded)) {
                        roiDisjointTile = true;
                    } else {
                        PlanarImage roiIMG = getImage();
                        roiIter = RandomIterFactory.create(roiIMG, null, TILE_CACHED, ARRAY_CALC);
                    }
                }
            }
        }

        if (roiDisjointTile) {
            ImageUtil.fillBackground(dest, destRect, new double[] { destNoData });
            return;
        }

        // Set format tags
        RasterFormatTag[] formatTags = null;
        if (ImageUtil.isBinary(getSampleModel())
                && !ImageUtil.isBinary(getSourceImage(0).getSampleModel())) {
            // XXX Workaround for bug 4521097. This branch of the if-block
            // should be deleted once bug 4668327 is fixed.
            RenderedImage[] sourceArray = new RenderedImage[] { getSourceImage(0) };
            RasterFormatTag[] sourceTags = RasterAccessor.findCompatibleTags(sourceArray,
                    sourceArray[0]);
            RasterFormatTag[] destTags = RasterAccessor.findCompatibleTags(sourceArray, this);
            formatTags = new RasterFormatTag[] { sourceTags[0], destTags[1] };
        } else {
            // Retrieve format tags.
            formatTags = getFormatTags();
        }

        RasterAccessor src = new RasterAccessor(sources[0], destRect, formatTags[0],
                getSourceImage(0).getColorModel());
        RasterAccessor dst = new RasterAccessor(dest, destRect, formatTags[1], getColorModel());

        switch (src.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            computeRectByte(src, dst, roiIter, roiContainsTile);
            break;
        case DataBuffer.TYPE_USHORT:
            computeRectUShort(src, dst, roiIter, roiContainsTile);
            break;
        case DataBuffer.TYPE_SHORT:
            computeRectShort(src, dst, roiIter, roiContainsTile);
            break;
        case DataBuffer.TYPE_INT:
            computeRectInt(src, dst, roiIter, roiContainsTile);
            break;
        case DataBuffer.TYPE_FLOAT:
            computeRectFloat(src, dst, roiIter, roiContainsTile);
            break;
        case DataBuffer.TYPE_DOUBLE:
            computeRectDouble(src, dst, roiIter, roiContainsTile);
            break;
        default:
            throw new RuntimeException(JaiI18N.getString("OrderedDitherOpImage1"));
        }

        dst.copyDataToRaster();
    }

    /**
     * Computes a <code>Rectangle</code> of data for byte imagery.
     * 
     * @param roiContainsTile
     * @param roiIter
     */
    private void computeRectByte(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
        int sbands = src.getNumBands();
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int[] sBandOffsets = src.getBandOffsets();
        byte[][] sData = src.getByteDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int dBandOffset = dst.getBandOffset(0);
        byte[] dData = dst.getByteDataArray(0);

        int x0 = dst.getX();
        int xMod = x0 % maskWidth;
        int y0 = dst.getY();

        switch (odType) {
        case TYPE_OD_BYTE_LUT_3BAND:
        case TYPE_OD_BYTE_LUT_NBAND:
            // Optimized cases
            int[] srcLineOffsets = (int[]) sBandOffsets.clone();
            int[] srcPixelOffsets = (int[]) srcLineOffsets.clone();
            int dLineOffset = dBandOffset;

            for (int h = 0; h < dheight; h++) {
                int yMod = (y0 + h) % maskHeight;

                if (odType == TYPE_OD_BYTE_LUT_3BAND) {
                    computeLineByteLUT3(sData, srcPixelOffsets, sPixelStride, dData, dLineOffset,
                            dPixelStride, dwidth, xMod, yMod, x0, y0 + h, roiIter, roiContainsTile);
                } else {
                    computeLineByteLUTN(sData, srcPixelOffsets, sPixelStride, dData, dLineOffset,
                            dPixelStride, dwidth, xMod, yMod, x0, y0 + h, roiIter, roiContainsTile);
                }

                for (int i = 0; i < sbands; i++) {
                    srcLineOffsets[i] += sLineStride;
                    srcPixelOffsets[i] = srcLineOffsets[i];
                }
                dLineOffset += dLineStride;
            }

            break;
        case TYPE_OD_GENERAL:
        default:
            computeRectByteGeneral(sData, sBandOffsets, sLineStride, sPixelStride, dData,
                    dBandOffset, dLineStride, dPixelStride, dwidth, dheight, xMod, x0, y0, roiIter,
                    roiContainsTile);
        }
    }

    /**
     * Dithers a line of 3-band byte data using a DitherLUT.
     * 
     * @param y0
     * @param x0
     * @param h
     * @param roiContainsTile
     * @param roiIter
     */
    private void computeLineByteLUT3(byte[][] sData, int[] sPixelOffsets, int sPixelStride,
            byte[] dData, int dPixelOffset, int dPixelStride, int dwidth, int xMod, int yMod,
            int x0, int y, RandomIter roiIter, boolean roiContainsTile) {
        int ditherLUTBandStride = odLUT.ditherLUTBandStride;
        int ditherLUTRowStride = odLUT.ditherLUTRowStride;
        int ditherLUTColStride = odLUT.ditherLUTColStride;
        byte[] ditherLUT = odLUT.ditherLUT;

        int base = adjustedOffset;

        int dlut0 = yMod * ditherLUTRowStride;
        int dlut1 = dlut0 + ditherLUTBandStride;
        int dlut2 = dlut1 + ditherLUTBandStride;

        int dlutLimit = dlut0 + ditherLUTRowStride;

        int xDelta = xMod * ditherLUTColStride;
        int pDtab0 = dlut0 + xDelta;
        int pDtab1 = dlut1 + xDelta;
        int pDtab2 = dlut2 + xDelta;

        byte[] sData0 = sData[0];
        byte[] sData1 = sData[1];
        byte[] sData2 = sData[2];

        if (caseA || (caseB && roiContainsTile)) {
            for (int count = dwidth; count > 0; count--) {
                int idx = (ditherLUT[pDtab0 + (sData0[sPixelOffsets[0]] & 0xff)] & 0xff)
                        + (ditherLUT[pDtab1 + (sData1[sPixelOffsets[1]] & 0xff)] & 0xff)
                        + (ditherLUT[pDtab2 + (sData2[sPixelOffsets[2]] & 0xff)] & 0xff);

                dData[dPixelOffset] = (byte) ((idx + base) & 0xff);

                sPixelOffsets[0] += sPixelStride;
                sPixelOffsets[1] += sPixelStride;
                sPixelOffsets[2] += sPixelStride;

                dPixelOffset += dPixelStride;

                pDtab0 += ditherLUTColStride;

                if (pDtab0 >= dlutLimit) {
                    pDtab0 = dlut0;
                    pDtab1 = dlut1;
                    pDtab2 = dlut2;
                } else {
                    pDtab1 += ditherLUTColStride;
                    pDtab2 += ditherLUTColStride;
                }
            }
        } else if (caseB) {
            for (int count = dwidth; count > 0; count--) {

                int x = x0 + dwidth - count;
                // ROI check
                if (roiBounds.contains(x, y) && roiIter.getSample(x, y, 0) > 0) {
                    int idx = (ditherLUT[pDtab0 + (sData0[sPixelOffsets[0]] & 0xff)] & 0xff)
                            + (ditherLUT[pDtab1 + (sData1[sPixelOffsets[1]] & 0xff)] & 0xff)
                            + (ditherLUT[pDtab2 + (sData2[sPixelOffsets[2]] & 0xff)] & 0xff);

                    dData[dPixelOffset] = (byte) ((idx + base) & 0xff);
                } else {
                    dData[dPixelOffset] = (byte) ((int) destNoData & 0xff);
                }

                sPixelOffsets[0] += sPixelStride;
                sPixelOffsets[1] += sPixelStride;
                sPixelOffsets[2] += sPixelStride;

                dPixelOffset += dPixelStride;

                pDtab0 += ditherLUTColStride;

                if (pDtab0 >= dlutLimit) {
                    pDtab0 = dlut0;
                    pDtab1 = dlut1;
                    pDtab2 = dlut2;
                } else {
                    pDtab1 += ditherLUTColStride;
                    pDtab2 += ditherLUTColStride;
                }
            }
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int count = dwidth; count > 0; count--) {

                int src0 = sData0[sPixelOffsets[0]] & 0xff;
                int src1 = sData1[sPixelOffsets[1]] & 0xff;
                int src2 = sData2[sPixelOffsets[2]] & 0xff;
                // NoData check
                boolean valid = lut[src0] && lut[src1] && lut[src2];

                if (valid) {
                    int idx = (ditherLUT[pDtab0 + (src0)] & 0xff)
                            + (ditherLUT[pDtab1 + (src1)] & 0xff)
                            + (ditherLUT[pDtab2 + (src2)] & 0xff);

                    dData[dPixelOffset] = (byte) ((idx + base) & 0xff);
                } else {
                    dData[dPixelOffset] = (byte) ((int) destNoData & 0xff);
                }

                sPixelOffsets[0] += sPixelStride;
                sPixelOffsets[1] += sPixelStride;
                sPixelOffsets[2] += sPixelStride;

                dPixelOffset += dPixelStride;

                pDtab0 += ditherLUTColStride;

                if (pDtab0 >= dlutLimit) {
                    pDtab0 = dlut0;
                    pDtab1 = dlut1;
                    pDtab2 = dlut2;
                } else {
                    pDtab1 += ditherLUTColStride;
                    pDtab2 += ditherLUTColStride;
                }
            }
        } else {
            for (int count = dwidth; count > 0; count--) {

                int x = x0 + dwidth - count;

                int src0 = sData0[sPixelOffsets[0]] & 0xff;
                int src1 = sData1[sPixelOffsets[1]] & 0xff;
                int src2 = sData2[sPixelOffsets[2]] & 0xff;
                // NoData check
                boolean valid = lut[src0] && lut[src1] && lut[src2];
                // ROI check
                if (valid && (roiBounds.contains(x, y) && roiIter.getSample(x, y, 0) > 0)) {
                    int idx = (ditherLUT[pDtab0 + (src0)] & 0xff)
                            + (ditherLUT[pDtab1 + (src1)] & 0xff)
                            + (ditherLUT[pDtab2 + (src2)] & 0xff);

                    dData[dPixelOffset] = (byte) ((idx + base) & 0xff);
                } else {
                    dData[dPixelOffset] = (byte) ((int) destNoData & 0xff);
                }

                sPixelOffsets[0] += sPixelStride;
                sPixelOffsets[1] += sPixelStride;
                sPixelOffsets[2] += sPixelStride;

                dPixelOffset += dPixelStride;

                pDtab0 += ditherLUTColStride;

                if (pDtab0 >= dlutLimit) {
                    pDtab0 = dlut0;
                    pDtab1 = dlut1;
                    pDtab2 = dlut2;
                } else {
                    pDtab1 += ditherLUTColStride;
                    pDtab2 += ditherLUTColStride;
                }
            }
        }
    }

    /**
     * Dithers a line of N-band byte data using a DitherLUT.
     * 
     * @param y0
     * @param x0
     * @param h
     * @param roiContainsTile
     * @param roiIter
     */
    private void computeLineByteLUTN(byte[][] sData, int[] sPixelOffsets, int sPixelStride,
            byte[] dData, int dPixelOffset, int dPixelStride, int dwidth, int xMod, int yMod,
            int x0, int y, RandomIter roiIter, boolean roiContainsTile) {
        int ditherLUTBandStride = odLUT.ditherLUTBandStride;
        int ditherLUTRowStride = odLUT.ditherLUTRowStride;
        int ditherLUTColStride = odLUT.ditherLUTColStride;
        byte[] ditherLUT = odLUT.ditherLUT;

        int base = adjustedOffset;

        int dlutRow = yMod * ditherLUTRowStride;
        int dlutCol = dlutRow + xMod * ditherLUTColStride;
        int dlutLimit = dlutRow + ditherLUTRowStride;

        if (caseA || (caseB && roiContainsTile)) {
            for (int count = dwidth; count > 0; count--) {
                int dlutBand = dlutCol;
                int idx = base;
                for (int i = 0; i < numBands; i++) {
                    idx += (ditherLUT[dlutBand + (sData[i][sPixelOffsets[i]] & 0xff)] & 0xff);
                    dlutBand += ditherLUTBandStride;
                    sPixelOffsets[i] += sPixelStride;
                }

                dData[dPixelOffset] = (byte) (idx & 0xff);

                dPixelOffset += dPixelStride;

                dlutCol += ditherLUTColStride;

                if (dlutCol >= dlutLimit) {
                    dlutCol = dlutRow;
                }
            }
        } else if (caseB) {
            for (int count = dwidth; count > 0; count--) {
                int x = x0 + dwidth - count;

                if (roiBounds.contains(x, y) && roiIter.getSample(x, y, 0) > 0) {
                    int dlutBand = dlutCol;
                    int idx = base;
                    for (int i = 0; i < numBands; i++) {
                        idx += (ditherLUT[dlutBand + (sData[i][sPixelOffsets[i]] & 0xff)] & 0xff);
                        dlutBand += ditherLUTBandStride;
                        sPixelOffsets[i] += sPixelStride;
                    }

                    dData[dPixelOffset] = (byte) (idx & 0xff);
                } else {
                    for (int i = 0; i < numBands; i++) {
                        sPixelOffsets[i] += sPixelStride;
                    }
                    dData[dPixelOffset] = (byte) ((int) destNoData & 0xff);
                }

                dPixelOffset += dPixelStride;

                dlutCol += ditherLUTColStride;

                if (dlutCol >= dlutLimit) {
                    dlutCol = dlutRow;
                }
            }
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int count = dwidth; count > 0; count--) {
                int dlutBand = dlutCol;
                int idx = base;
                boolean valid = true;
                for (int i = 0; i < numBands; i++) {
                    int b = sData[i][sPixelOffsets[i]] & 0xff;
                    valid &= lut[b];
                    idx += (ditherLUT[dlutBand + b] & 0xff);
                    dlutBand += ditherLUTBandStride;
                    sPixelOffsets[i] += sPixelStride;
                }

                if (valid) {
                    dData[dPixelOffset] = (byte) (idx & 0xff);
                } else {
                    dData[dPixelOffset] = (byte) ((int) destNoData & 0xff);
                }

                dPixelOffset += dPixelStride;

                dlutCol += ditherLUTColStride;

                if (dlutCol >= dlutLimit) {
                    dlutCol = dlutRow;
                }
            }
        } else {
            for (int count = dwidth; count > 0; count--) {
                int x = x0 + dwidth - count;

                if (roiBounds.contains(x, y) && roiIter.getSample(x, y, 0) > 0) {
                    int dlutBand = dlutCol;
                    int idx = base;
                    boolean valid = true;
                    for (int i = 0; i < numBands; i++) {
                        int b = sData[i][sPixelOffsets[i]] & 0xff;
                        valid &= lut[b];
                        idx += (ditherLUT[dlutBand + b] & 0xff);
                        dlutBand += ditherLUTBandStride;
                        sPixelOffsets[i] += sPixelStride;
                    }

                    if (valid) {
                        dData[dPixelOffset] = (byte) (idx & 0xff);
                    } else {
                        dData[dPixelOffset] = (byte) ((int) destNoData & 0xff);
                    }
                } else {
                    for (int i = 0; i < numBands; i++) {
                        sPixelOffsets[i] += sPixelStride;
                    }
                    dData[dPixelOffset] = (byte) ((int) destNoData & 0xff);
                }

                dPixelOffset += dPixelStride;

                dlutCol += ditherLUTColStride;

                if (dlutCol >= dlutLimit) {
                    dlutCol = dlutRow;
                }
            }
        }

    }

    /**
     * Computes a <code>Rectangle</code> of data for byte imagery using the general, unoptimized algorithm.
     * 
     * @param y0
     * @param roiContainsTile
     * @param roiIter
     */
    private void computeRectByteGeneral(byte[][] sData, int[] sBandOffsets, int sLineStride,
            int sPixelStride, byte[] dData, int dBandOffset, int dLineStride, int dPixelStride,
            int dwidth, int dheight, int xMod, int x0, int y0, RandomIter roiIter,
            boolean roiContainsTile) {
        if (adjustedOffset > 0) {
            Arrays.fill(dData, (byte) (adjustedOffset & 0xff));
        }

        int sbands = sBandOffsets.length;
        int sLineOffset = 0;
        int dLineOffset = 0;
        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dheight; h++) {
                int yMod = (y0 + h) % maskHeight;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = yMod * maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                for (int w = 0; w < dwidth; w++) {
                    int bDIndex = dPixelOffset + dBandOffset;
                    for (int b = 0; b < sbands; b++) {
                        int tmp = (sData[b][sPixelOffset + sBandOffsets[b]] & 0xff) * dims[b];
                        int frac = (int) (tmp & 0xff);
                        tmp >>= 8;
                        if (frac > (int) (maskDataByte[b][maskIndex] & 0xff)) {
                            tmp++;
                        }

                        // Accumulate the value into the destination data array.
                        int result = (dData[bDIndex] & 0xff) + tmp * mults[b];
                        dData[bDIndex] = (byte) (result & 0xff);
                    }

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if (++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;
            }
        } else if (caseB) {
            for (int h = 0; h < dheight; h++) {
                int y = y0 + h;
                int yMod = y % maskHeight;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = yMod * maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                for (int w = 0; w < dwidth; w++) {
                    int x = x0 + w;

                    // ROI Check
                    int bDIndex = dPixelOffset + dBandOffset;
                    if (roiBounds.contains(x, y) && roiIter.getSample(x, y, 0) > 0) {
                        for (int b = 0; b < sbands; b++) {
                            int tmp = (sData[b][sPixelOffset + sBandOffsets[b]] & 0xff) * dims[b];
                            int frac = (int) (tmp & 0xff);
                            tmp >>= 8;
                            if (frac > (int) (maskDataByte[b][maskIndex] & 0xff)) {
                                tmp++;
                            }

                            // Accumulate the value into the destination data array.
                            int result = (dData[bDIndex] & 0xff) + tmp * mults[b];
                            dData[bDIndex] = (byte) (result & 0xff);
                        }
                    } else {
                        dData[bDIndex] = (byte) ((int) destNoData & 0xff);
                    }

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if (++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;
            }
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int h = 0; h < dheight; h++) {
                int yMod = (y0 + h) % maskHeight;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = yMod * maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                for (int w = 0; w < dwidth; w++) {

                    boolean valid = true;

                    int bDIndex = dPixelOffset + dBandOffset;
                    for (int b = 0; b < sbands && valid; b++) {
                        int value = sData[b][sPixelOffset + sBandOffsets[b]] & 0xff;
                        valid &= lut[value];
                        int tmp = (value) * dims[b];
                        int frac = (int) (tmp & 0xff);
                        tmp >>= 8;
                        if (frac > (int) (maskDataByte[b][maskIndex] & 0xff)) {
                            tmp++;
                        }

                        // Accumulate the value into the destination data array.
                        int result = (dData[bDIndex] & 0xff) + tmp * mults[b];
                        dData[bDIndex] = (byte) (result & 0xff);
                    }
                    if (!valid) {
                        dData[bDIndex] = (byte) ((int) destNoData & 0xff);
                    }

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if (++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;
            }
        } else {
            for (int h = 0; h < dheight; h++) {
                int y = y0 + h;
                int yMod = y % maskHeight;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = yMod * maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                for (int w = 0; w < dwidth; w++) {
                    int x = x0 + w;

                    // ROI Check
                    int bDIndex = dPixelOffset + dBandOffset;
                    if (roiBounds.contains(x, y) && roiIter.getSample(x, y, 0) > 0) {
                        boolean valid = true;

                        for (int b = 0; b < sbands && valid; b++) {
                            int value = sData[b][sPixelOffset + sBandOffsets[b]] & 0xff;
                            valid &= lut[value];
                            int tmp = (value) * dims[b];
                            int frac = (int) (tmp & 0xff);
                            tmp >>= 8;
                            if (frac > (int) (maskDataByte[b][maskIndex] & 0xff)) {
                                tmp++;
                            }

                            // Accumulate the value into the destination data array.
                            int result = (dData[bDIndex] & 0xff) + tmp * mults[b];
                            dData[bDIndex] = (byte) (result & 0xff);
                        }

                        if (!valid) {
                            dData[bDIndex] = (byte) ((int) destNoData & 0xff);
                        }
                    } else {
                        dData[bDIndex] = (byte) ((int) destNoData & 0xff);
                    }

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if (++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;
            }
        }

        if (adjustedOffset < 0) {
            // Shift the result by the adjusted offset of the color map.
            int length = dData.length;
            for (int i = 0; i < length; i++) {
                dData[i] = (byte) ((dData[i] & 0xff) + adjustedOffset);
            }
        }
    }

    /**
     * Computes a <code>Rectangle</code> of data for signed short imagery.
     * 
     * @param roiContainsTile
     * @param roiIter
     */
    private void computeRectShort(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
        int sbands = src.getNumBands();
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int[] sBandOffsets = src.getBandOffsets();
        short[][] sData = src.getShortDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int dBandOffset = dst.getBandOffset(0);
        short[] dData = dst.getShortDataArray(0);

        // Initialize the destination data to the color cube adjusted offset
        // to permit accumulation of the result for each band.
        if (adjustedOffset != 0) {
            Arrays.fill(dData, (short) (adjustedOffset & 0xffff));
        }

        int x0 = dst.getX();
        int xMod = x0 % maskWidth;
        int y0 = dst.getY();

        int sLineOffset = 0;
        int dLineOffset = 0;
        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dheight; h++) {
                int yMod = (y0 + h) % maskHeight;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = yMod * maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                for (int w = 0; w < dwidth; w++) {
                    int bDIndex = dPixelOffset + dBandOffset;
                    for (int b = 0; b < sbands; b++) {
                        int tmp = (sData[b][sPixelOffset + sBandOffsets[b]] - Short.MIN_VALUE)
                                * dims[b];
                        int frac = (int) (tmp & 0xffff);

                        // Accumulate the value into the destination data array.
                        int result = (int) (dData[bDIndex] & 0xffff) + (tmp >> 16) * mults[b];
                        if (frac > maskDataInt[b][maskIndex]) {
                            result += mults[b];
                        }
                        dData[bDIndex] = (short) (result & 0xffff);
                    }

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if (++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;
            }
        } else if (caseB) {
            for (int h = 0; h < dheight; h++) {
                int y = y0 + h;
                int yMod = y % maskHeight;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = yMod * maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                for (int w = 0; w < dwidth; w++) {
                    int x = x0 + w;

                    // ROI Check
                    int bDIndex = dPixelOffset + dBandOffset;
                    if (roiBounds.contains(x, y) && roiIter.getSample(x, y, 0) > 0) {
                        for (int b = 0; b < sbands; b++) {
                            int tmp = (sData[b][sPixelOffset + sBandOffsets[b]] - Short.MIN_VALUE)
                                    * dims[b];
                            int frac = (int) (tmp & 0xffff);

                            // Accumulate the value into the destination data array.
                            int result = (int) (dData[bDIndex] & 0xffff) + (tmp >> 16) * mults[b];
                            if (frac > maskDataInt[b][maskIndex]) {
                                result += mults[b];
                            }
                            dData[bDIndex] = (short) (result & 0xffff);
                        }
                    } else {
                        dData[bDIndex] = (short) ((int) destNoData & 0xffff);
                    }

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if (++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;
            }
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int h = 0; h < dheight; h++) {
                int yMod = (y0 + h) % maskHeight;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = yMod * maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                for (int w = 0; w < dwidth; w++) {

                    boolean valid = true;

                    int bDIndex = dPixelOffset + dBandOffset;
                    for (int b = 0; b < sbands && valid; b++) {
                        short value = sData[b][sPixelOffset + sBandOffsets[b]];
                        valid &= !nodata.contains(value);
                        int tmp = (value - Short.MIN_VALUE) * dims[b];
                        int frac = (int) (tmp & 0xffff);

                        // Accumulate the value into the destination data array.
                        int result = (int) (dData[bDIndex] & 0xffff) + (tmp >> 16) * mults[b];
                        if (frac > maskDataInt[b][maskIndex]) {
                            result += mults[b];
                        }
                        dData[bDIndex] = (short) (result & 0xffff);
                    }
                    if (!valid) {
                        dData[bDIndex] = (short) ((int) destNoData & 0xffff);
                    }

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if (++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;
            }
        } else {
            for (int h = 0; h < dheight; h++) {
                int y = y0 + h;
                int yMod = y % maskHeight;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = yMod * maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                for (int w = 0; w < dwidth; w++) {
                    int x = x0 + w;

                    // ROI Check
                    int bDIndex = dPixelOffset + dBandOffset;
                    if (roiBounds.contains(x, y) && roiIter.getSample(x, y, 0) > 0) {
                        boolean valid = true;

                        for (int b = 0; b < sbands && valid; b++) {
                            short value = sData[b][sPixelOffset + sBandOffsets[b]];
                            valid &= !nodata.contains(value);
                            int tmp = (value - Short.MIN_VALUE) * dims[b];
                            int frac = (int) (tmp & 0xffff);

                            // Accumulate the value into the destination data array.
                            int result = (int) (dData[bDIndex] & 0xffff) + (tmp >> 16) * mults[b];
                            if (frac > maskDataInt[b][maskIndex]) {
                                result += mults[b];
                            }
                            dData[bDIndex] = (short) (result & 0xffff);
                        }

                        if (!valid) {
                            dData[bDIndex] = (short) ((int) destNoData & 0xffff);
                        }
                    } else {
                        dData[bDIndex] = (short) ((int) destNoData & 0xffff);
                    }

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if (++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;
            }
        }
    }

    /**
     * Computes a <code>Rectangle</code> of data for unsigned short imagery.
     * 
     * @param roiContainsTile
     * @param roiIter
     */
    private void computeRectUShort(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
        int sbands = src.getNumBands();
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int[] sBandOffsets = src.getBandOffsets();
        short[][] sData = src.getShortDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int dBandOffset = dst.getBandOffset(0);
        short[] dData = dst.getShortDataArray(0);

        // Initialize the destination data to the color cube adjusted offset
        // to permit accumulation of the result for each band.
        if (adjustedOffset != 0) {
            Arrays.fill(dData, (short) (adjustedOffset & 0xffff));
        }

        int x0 = dst.getX();
        int xMod = x0 % maskWidth;
        int y0 = dst.getY();

        int sLineOffset = 0;
        int dLineOffset = 0;
        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dheight; h++) {
                int yMod = (y0 + h) % maskHeight;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = yMod * maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                for (int w = 0; w < dwidth; w++) {
                    int bDIndex = dPixelOffset + dBandOffset;
                    for (int b = 0; b < sbands; b++) {
                        int tmp = (sData[b][sPixelOffset + sBandOffsets[b]] & 0xffff) * dims[b];
                        int frac = (int) (tmp & 0xffff);

                        // Accumulate the value into the destination data array.
                        int result = (int) (dData[bDIndex] & 0xffff) + (tmp >> 16) * mults[b];
                        if (frac > maskDataInt[b][maskIndex]) {
                            result += mults[b];
                        }
                        dData[bDIndex] = (short) (result & 0xffff);
                    }

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if (++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;
            }
        } else if (caseB) {
            for (int h = 0; h < dheight; h++) {
                int y = y0 + h;
                int yMod = y % maskHeight;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = yMod * maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                for (int w = 0; w < dwidth; w++) {
                    int x = x0 + w;

                    // ROI Check
                    int bDIndex = dPixelOffset + dBandOffset;
                    if (roiBounds.contains(x, y) && roiIter.getSample(x, y, 0) > 0) {
                        for (int b = 0; b < sbands; b++) {
                            int tmp = (sData[b][sPixelOffset + sBandOffsets[b]] & 0xffff) * dims[b];
                            int frac = (int) (tmp & 0xffff);

                            // Accumulate the value into the destination data array.
                            int result = (int) (dData[bDIndex] & 0xffff) + (tmp >> 16) * mults[b];
                            if (frac > maskDataInt[b][maskIndex]) {
                                result += mults[b];
                            }
                            dData[bDIndex] = (short) (result & 0xffff);
                        }
                    } else {
                        dData[bDIndex] = (short) ((int) destNoData & 0xffff);
                    }

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if (++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;
            }
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int h = 0; h < dheight; h++) {
                int yMod = (y0 + h) % maskHeight;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = yMod * maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                for (int w = 0; w < dwidth; w++) {

                    boolean valid = true;

                    int bDIndex = dPixelOffset + dBandOffset;
                    for (int b = 0; b < sbands && valid; b++) {
                        short value = sData[b][sPixelOffset + sBandOffsets[b]];
                        valid &= !nodata.contains(value);
                        int tmp = (value & 0xffff) * dims[b];
                        int frac = (int) (tmp & 0xffff);

                        // Accumulate the value into the destination data array.
                        int result = (int) (dData[bDIndex] & 0xffff) + (tmp >> 16) * mults[b];
                        if (frac > maskDataInt[b][maskIndex]) {
                            result += mults[b];
                        }
                        dData[bDIndex] = (short) (result & 0xffff);
                    }
                    if (!valid) {
                        dData[bDIndex] = (short) ((int) destNoData & 0xffff);
                    }

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if (++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;
            }
        } else {
            for (int h = 0; h < dheight; h++) {
                int y = y0 + h;
                int yMod = y % maskHeight;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = yMod * maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                for (int w = 0; w < dwidth; w++) {
                    int x = x0 + w;

                    // ROI Check
                    int bDIndex = dPixelOffset + dBandOffset;
                    if (roiBounds.contains(x, y) && roiIter.getSample(x, y, 0) > 0) {
                        boolean valid = true;

                        for (int b = 0; b < sbands && valid; b++) {
                            short value = sData[b][sPixelOffset + sBandOffsets[b]];
                            valid &= !nodata.contains(value);
                            int tmp = (value & 0xffff) * dims[b];
                            int frac = (int) (tmp & 0xffff);

                            // Accumulate the value into the destination data array.
                            int result = (int) (dData[bDIndex] & 0xffff) + (tmp >> 16) * mults[b];
                            if (frac > maskDataInt[b][maskIndex]) {
                                result += mults[b];
                            }
                            dData[bDIndex] = (short) (result & 0xffff);
                        }

                        if (!valid) {
                            dData[bDIndex] = (short) ((int) destNoData & 0xffff);
                        }
                    } else {
                        dData[bDIndex] = (short) ((int) destNoData & 0xffff);
                    }

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if (++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;
            }
        }
    }

    /**
     * Computes a <code>Rectangle</code> of data for integer imagery.
     * 
     * @param roiContainsTile
     * @param roiIter
     */
    private void computeRectInt(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
        int sbands = src.getNumBands();
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int[] sBandOffsets = src.getBandOffsets();
        int[][] sData = src.getIntDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int dBandOffset = dst.getBandOffset(0);
        int[] dData = dst.getIntDataArray(0);

        // Initialize the destination data to the color cube adjusted offset
        // to permit accumulation of the result for each band.
        if (adjustedOffset != 0) {
            Arrays.fill(dData, adjustedOffset);
        }

        int x0 = dst.getX();
        int xMod = x0 % maskWidth;
        int y0 = dst.getY();

        int sLineOffset = 0;
        int dLineOffset = 0;
        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dheight; h++) {
                int yMod = (y0 + h) % maskHeight;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = yMod * maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                for (int w = 0; w < dwidth; w++) {
                    int bDIndex = dPixelOffset + dBandOffset;
                    for (int b = 0; b < sbands; b++) {
                        long tmp = ((long) sData[b][sPixelOffset + sBandOffsets[b]] - (long) Integer.MIN_VALUE)
                                * dims[b];
                        long frac = (long) (tmp & 0xffffffff);

                        // Accumulate the value into the destination data array.
                        int result = dData[bDIndex] + ((int) (tmp >> 32)) * mults[b];
                        if (frac > maskDataLong[b][maskIndex]) {
                            result += mults[b];
                        }
                        dData[bDIndex] = result;
                    }

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if (++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;
            }
        } else if (caseB) {
            for (int h = 0; h < dheight; h++) {
                int y = y0 + h;
                int yMod = y % maskHeight;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = yMod * maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                for (int w = 0; w < dwidth; w++) {
                    int x = x0 + w;

                    // ROI Check
                    int bDIndex = dPixelOffset + dBandOffset;
                    if (roiBounds.contains(x, y) && roiIter.getSample(x, y, 0) > 0) {
                        for (int b = 0; b < sbands; b++) {
                            long tmp = ((long) sData[b][sPixelOffset + sBandOffsets[b]] - (long) Integer.MIN_VALUE)
                                    * dims[b];
                            long frac = (long) (tmp & 0xffffffff);

                            // Accumulate the value into the destination data array.
                            int result = dData[bDIndex] + ((int) (tmp >> 32)) * mults[b];
                            if (frac > maskDataLong[b][maskIndex]) {
                                result += mults[b];
                            }
                            dData[bDIndex] = result;
                        }
                    } else {
                        dData[bDIndex] = (int) destNoData;
                    }

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if (++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;
            }
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int h = 0; h < dheight; h++) {
                int yMod = (y0 + h) % maskHeight;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = yMod * maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                for (int w = 0; w < dwidth; w++) {

                    boolean valid = true;

                    int bDIndex = dPixelOffset + dBandOffset;
                    for (int b = 0; b < sbands && valid; b++) {
                        int value = sData[b][sPixelOffset + sBandOffsets[b]];
                        valid &= !nodata.contains(value);
                        long tmp = ((long) value - (long) Integer.MIN_VALUE) * dims[b];
                        long frac = (long) (tmp & 0xffffffff);

                        // Accumulate the value into the destination data array.
                        int result = dData[bDIndex] + ((int) (tmp >> 32)) * mults[b];
                        if (frac > maskDataLong[b][maskIndex]) {
                            result += mults[b];
                        }
                        dData[bDIndex] = result;
                    }
                    if (!valid) {
                        dData[bDIndex] = (int) destNoData;
                    }

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if (++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;
            }
        } else {
            for (int h = 0; h < dheight; h++) {
                int y = y0 + h;
                int yMod = y % maskHeight;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = yMod * maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                for (int w = 0; w < dwidth; w++) {
                    int x = x0 + w;

                    // ROI Check
                    int bDIndex = dPixelOffset + dBandOffset;
                    if (roiBounds.contains(x, y) && roiIter.getSample(x, y, 0) > 0) {
                        boolean valid = true;

                        for (int b = 0; b < sbands && valid; b++) {
                            int value = sData[b][sPixelOffset + sBandOffsets[b]];
                            valid &= !nodata.contains(value);
                            long tmp = ((long) value - (long) Integer.MIN_VALUE) * dims[b];
                            long frac = (long) (tmp & 0xffffffff);

                            // Accumulate the value into the destination data array.
                            int result = dData[bDIndex] + ((int) (tmp >> 32)) * mults[b];
                            if (frac > maskDataLong[b][maskIndex]) {
                                result += mults[b];
                            }
                            dData[bDIndex] = result;
                        }

                        if (!valid) {
                            dData[bDIndex] = (int) destNoData;
                        }
                    } else {
                        dData[bDIndex] = (int) destNoData;
                    }

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if (++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;
            }
        }
    }

    /**
     * Computes a <code>Rectangle</code> of data for float imagery.
     * 
     * @param roiContainsTile
     * @param roiIter
     */
    private void computeRectFloat(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
        int sbands = src.getNumBands();
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int[] sBandOffsets = src.getBandOffsets();
        float[][] sData = src.getFloatDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int dBandOffset = dst.getBandOffset(0);
        float[] dData = dst.getFloatDataArray(0);

        // Initialize the destination data to the color cube adjusted offset
        // to permit accumulation of the result for each band.
        if (adjustedOffset != 0) {
            Arrays.fill(dData, (float) adjustedOffset);
        }

        int x0 = dst.getX();
        int xMod = x0 % maskWidth;
        int y0 = dst.getY();

        int sLineOffset = 0;
        int dLineOffset = 0;
        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dheight; h++) {
                int yMod = (y0 + h) % maskHeight;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = yMod * maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                for (int w = 0; w < dwidth; w++) {
                    int bDIndex = dPixelOffset + dBandOffset;
                    for (int b = 0; b < sbands; b++) {
                        int bIndex = sPixelOffset + sBandOffsets[b];
                        int tmp = (int) (sData[b][bIndex] * dims[b]);
                        float frac = sData[b][bIndex] * dims[b] - tmp;

                        // Accumulate the value into the destination data array.
                        float result = dData[bDIndex] + tmp * mults[b];
                        if (frac > maskDataFloat[b][maskIndex]) {
                            result += mults[b];
                        }
                        dData[bDIndex] = result;
                    }

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if (++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;
            }
        } else if (caseB) {
            for (int h = 0; h < dheight; h++) {
                int y = y0 + h;
                int yMod = y % maskHeight;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = yMod * maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                for (int w = 0; w < dwidth; w++) {
                    int x = x0 + w;

                    // ROI Check
                    int bDIndex = dPixelOffset + dBandOffset;
                    if (roiBounds.contains(x, y) && roiIter.getSample(x, y, 0) > 0) {
                        for (int b = 0; b < sbands; b++) {
                            int bIndex = sPixelOffset + sBandOffsets[b];
                            int tmp = (int) (sData[b][bIndex] * dims[b]);
                            float frac = sData[b][bIndex] * dims[b] - tmp;

                            // Accumulate the value into the destination data array.
                            float result = dData[bDIndex] + tmp * mults[b];
                            if (frac > maskDataFloat[b][maskIndex]) {
                                result += mults[b];
                            }
                            dData[bDIndex] = result;
                        }
                    } else {
                        dData[bDIndex] = (float) destNoData;
                    }

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if (++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;
            }
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int h = 0; h < dheight; h++) {
                int yMod = (y0 + h) % maskHeight;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = yMod * maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                for (int w = 0; w < dwidth; w++) {

                    boolean valid = true;

                    int bDIndex = dPixelOffset + dBandOffset;
                    for (int b = 0; b < sbands && valid; b++) {
                        int bIndex = sPixelOffset + sBandOffsets[b];
                        float value = sData[b][bIndex];
                        valid &= !nodata.contains(value);
                        int tmp = (int) (value * dims[b]);
                        float frac = value * dims[b] - tmp;

                        // Accumulate the value into the destination data array.
                        float result = dData[bDIndex] + tmp * mults[b];
                        if (frac > maskDataFloat[b][maskIndex]) {
                            result += mults[b];
                        }
                        dData[bDIndex] = result;
                    }
                    if (!valid) {
                        dData[bDIndex] = (float) destNoData;
                    }

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if (++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;
            }
        } else {
            for (int h = 0; h < dheight; h++) {
                int y = y0 + h;
                int yMod = y % maskHeight;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = yMod * maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                for (int w = 0; w < dwidth; w++) {
                    int x = x0 + w;

                    // ROI Check
                    int bDIndex = dPixelOffset + dBandOffset;
                    if (roiBounds.contains(x, y) && roiIter.getSample(x, y, 0) > 0) {
                        boolean valid = true;

                        for (int b = 0; b < sbands && valid; b++) {
                            int bIndex = sPixelOffset + sBandOffsets[b];
                            float value = sData[b][bIndex];
                            valid &= !nodata.contains(value);
                            int tmp = (int) (value * dims[b]);
                            float frac = value * dims[b] - tmp;

                            // Accumulate the value into the destination data array.
                            float result = dData[bDIndex] + tmp * mults[b];
                            if (frac > maskDataFloat[b][maskIndex]) {
                                result += mults[b];
                            }
                            dData[bDIndex] = result;
                        }

                        if (!valid) {
                            dData[bDIndex] = (float) destNoData;
                        }
                    } else {
                        dData[bDIndex] = (float) destNoData;
                    }

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if (++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;
            }
        }
    }

    /**
     * Computes a <code>Rectangle</code> of data for double imagery.
     * 
     * @param roiContainsTile
     * @param roiIter
     */
    private void computeRectDouble(RasterAccessor src, RasterAccessor dst, RandomIter roiIter,
            boolean roiContainsTile) {
        int sbands = src.getNumBands();
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int[] sBandOffsets = src.getBandOffsets();
        double[][] sData = src.getDoubleDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int dBandOffset = dst.getBandOffset(0);
        double[] dData = dst.getDoubleDataArray(0);

        // Initialize the destination data to the color cube adjusted offset
        // to permit accumulation of the result for each band.
        if (adjustedOffset != 0) {
            Arrays.fill(dData, (double) adjustedOffset);
        }

        int x0 = dst.getX();
        int xMod = x0 % maskWidth;
        int y0 = dst.getY();

        int sLineOffset = 0;
        int dLineOffset = 0;
        if (caseA || (caseB && roiContainsTile)) {
            for (int h = 0; h < dheight; h++) {
                int yMod = (y0 + h) % maskHeight;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = yMod * maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                for (int w = 0; w < dwidth; w++) {
                    int bDIndex = dPixelOffset + dBandOffset;
                    for (int b = 0; b < sbands; b++) {
                        int bIndex = sPixelOffset + sBandOffsets[b];
                        int tmp = (int) (sData[b][bIndex] * dims[b]);
                        float frac = (float) (sData[b][bIndex] * dims[b] - tmp);

                        // Accumulate the value into the destination data array.
                        double result = dData[bDIndex] + tmp * mults[b];
                        if (frac > maskDataFloat[b][maskIndex]) {
                            result += mults[b];
                        }
                        dData[bDIndex] = result;
                    }

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if (++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;
            }
        } else if (caseB) {
            for (int h = 0; h < dheight; h++) {
                int y = y0 + h;
                int yMod = y % maskHeight;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = yMod * maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                for (int w = 0; w < dwidth; w++) {
                    int x = x0 + w;

                    // ROI Check
                    int bDIndex = dPixelOffset + dBandOffset;
                    if (roiBounds.contains(x, y) && roiIter.getSample(x, y, 0) > 0) {
                        for (int b = 0; b < sbands; b++) {
                            int bIndex = sPixelOffset + sBandOffsets[b];
                            int tmp = (int) (sData[b][bIndex] * dims[b]);
                            float frac = (float) (sData[b][bIndex] * dims[b] - tmp);

                            // Accumulate the value into the destination data array.
                            double result = dData[bDIndex] + tmp * mults[b];
                            if (frac > maskDataFloat[b][maskIndex]) {
                                result += mults[b];
                            }
                            dData[bDIndex] = result;
                        }
                    } else {
                        dData[bDIndex] = destNoData;
                    }

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if (++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;
            }
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            for (int h = 0; h < dheight; h++) {
                int yMod = (y0 + h) % maskHeight;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = yMod * maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                for (int w = 0; w < dwidth; w++) {

                    boolean valid = true;

                    int bDIndex = dPixelOffset + dBandOffset;
                    for (int b = 0; b < sbands && valid; b++) {
                        int bIndex = sPixelOffset + sBandOffsets[b];
                        double value = sData[b][bIndex];
                        valid &= !nodata.contains(value);
                        int tmp = (int) (value * dims[b]);
                        float frac = (float) (value * dims[b] - tmp);

                        // Accumulate the value into the destination data array.
                        double result = dData[bDIndex] + tmp * mults[b];
                        if (frac > maskDataFloat[b][maskIndex]) {
                            result += mults[b];
                        }
                        dData[bDIndex] = result;
                    }
                    if (!valid) {
                        dData[bDIndex] = destNoData;
                    }

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if (++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;
            }
        } else {
            for (int h = 0; h < dheight; h++) {
                int y = y0 + h;
                int yMod = y % maskHeight;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = yMod * maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                for (int w = 0; w < dwidth; w++) {
                    int x = x0 + w;

                    // ROI Check
                    int bDIndex = dPixelOffset + dBandOffset;
                    if (roiBounds.contains(x, y) && roiIter.getSample(x, y, 0) > 0) {
                        boolean valid = true;

                        for (int b = 0; b < sbands && valid; b++) {
                            int bIndex = sPixelOffset + sBandOffsets[b];
                            double value = sData[b][bIndex];
                            valid &= !nodata.contains(value);
                            int tmp = (int) (value * dims[b]);
                            float frac = (float) (value * dims[b] - tmp);

                            // Accumulate the value into the destination data array.
                            double result = dData[bDIndex] + tmp * mults[b];
                            if (frac > maskDataFloat[b][maskIndex]) {
                                result += mults[b];
                            }
                            dData[bDIndex] = result;
                        }

                        if (!valid) {
                            dData[bDIndex] = destNoData;
                        }
                    } else {
                        dData[bDIndex] = destNoData;
                    }

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if (++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;
            }
        }
    }

    /**
     * This method provides a lazy initialization of the image associated to the ROI. The method uses the Double-checked locking in order to maintain
     * thread-safety
     * 
     * @return
     */
    private PlanarImage getImage() {
        PlanarImage img = roiImage;
        if (img == null) {
            synchronized (this) {
                img = roiImage;
                if (img == null) {
                    roiImage = img = roi.getAsImage();
                }
            }
        }
        return img;
    }
}
