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
package it.geosolutions.jaiext.artifacts;

import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.Map;

import javax.media.jai.BorderExtender;
import javax.media.jai.BorderExtenderConstant;
import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.PointOpImage;
import javax.media.jai.ROI;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.operator.BandCombineDescriptor;

import com.sun.media.jai.util.ImageUtil;

/**
 * An Artifacts Filter operation.
 * 
 * Given an input image and a ROI, transform the pixels along the inner BORDER of the ROI, if less than a specified Luminance threshold value, to a
 * mean of all sourrounding pixels within ROI, having Luminance greater than threshold. It should be pointed out that users may specify a NoData Range
 * to use in order to avoid to calculate NoData values.
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 * @author Simone Giannecchini, GeoSolutions SAS
 * 
 * 
 * 
 * @source $URL$
 */
@SuppressWarnings("unchecked")
public final class ArtifactsFilterOpImage extends PointOpImage {

    /** Constant indicating that the inner random iterators must pre-calculate an array of the image positions */
    public static final boolean ARRAY_CALC = true;

    /** Constant indicating that the inner random iterators must cache the current tile position */
    public static final boolean TILE_CACHED = true;

    public enum DataTypeCalculator {
        BYTE {
            @Override
            public boolean isNoData(Range nodata, Number value) {
                return nodata.contains(value.byteValue());
            }
        },
        SHORT {
            @Override
            public boolean isNoData(Range nodata, Number value) {
                return nodata.contains(value.shortValue());
            }
        },
        INTEGER {
            @Override
            public boolean isNoData(Range nodata, Number value) {
                return nodata.contains(value.intValue());
            }
        },
        FLOAT {
            @Override
            public boolean isNoData(Range nodata, Number value) {
                return nodata.contains(value.floatValue());
            }
        },
        DOUBLE {
            @Override
            public boolean isNoData(Range nodata, Number value) {
                return nodata.contains(value.doubleValue());
            }
        };

        public abstract boolean isNoData(Range nodata, Number value);

        public static void computeValueAtOnce(int[][] values, int valueCount, int[] val,
                int numBands) {
            for (int k = 0; k < numBands; k++) {
                val[k] = computeValueBands(values, valueCount, k);
            }
        }

        public static void computeValueAtOnce(float[][] values, int valueCount, float[] val,
                int numBands) {
            for (int k = 0; k < numBands; k++) {
                val[k] = computeValueBands(values, valueCount, k);
            }
        }

        public static void computeValueAtOnce(double[][] values, int valueCount, double[] val,
                int numBands) {
            for (int k = 0; k < numBands; k++) {
                val[k] = computeValueBands(values, valueCount, k);
            }
        }

        private static int computeValueBands(int[][] data, int valueCount, int band) {
            int left = 0;
            int right = valueCount - 1;
            int target = valueCount / 2;

            while (true) {
                int oleft = left;
                int oright = right;
                int mid = data[(left + right) / 2][band];
                do {
                    while (data[left][band] < mid) {
                        left++;
                    }
                    while (mid < data[right][band]) {
                        right--;
                    }
                    if (left <= right) {
                        int tmp = data[left][band];
                        data[left][band] = data[right][band];
                        data[right][band] = tmp;
                        left++;
                        right--;
                    }
                } while (left <= right);
                if (oleft < right && right >= target) {
                    left = oleft;
                } else if (left < oright && left <= target) {
                    right = oright;
                } else {
                    return data[target][band];
                }
            }
        }

        private static float computeValueBands(float[][] data, int valueCount, int band) {
            int left = 0;
            int right = valueCount - 1;
            int target = valueCount / 2;

            while (true) {
                int oleft = left;
                int oright = right;
                float mid = data[(left + right) / 2][band];
                do {
                    while (data[left][band] < mid) {
                        left++;
                    }
                    while (mid < data[right][band]) {
                        right--;
                    }
                    if (left <= right) {
                        float tmp = data[left][band];
                        data[left][band] = data[right][band];
                        data[right][band] = tmp;
                        left++;
                        right--;
                    }
                } while (left <= right);
                if (oleft < right && right >= target) {
                    left = oleft;
                } else if (left < oright && left <= target) {
                    right = oright;
                } else {
                    return data[target][band];
                }
            }
        }

        private static double computeValueBands(double[][] data, int valueCount, int band) {
            int left = 0;
            int right = valueCount - 1;
            int target = valueCount / 2;

            while (true) {
                int oleft = left;
                int oright = right;
                double mid = data[(left + right) / 2][band];
                do {
                    while (data[left][band] < mid) {
                        left++;
                    }
                    while (mid < data[right][band]) {
                        right--;
                    }
                    if (left <= right) {
                        double tmp = data[left][band];
                        data[left][band] = data[right][band];
                        data[right][band] = tmp;
                        left++;
                        right--;
                    }
                } while (left <= right);
                if (oleft < right && right >= target) {
                    left = oleft;
                } else if (left < oright && left <= target) {
                    right = oright;
                } else {
                    return data[target][band];
                }
            }
        }
    }

    /**
     * Helper class used for accessing ROI data
     */
    class RoiAccessor {
        RandomIter iterator;

        ROI roi;

        PlanarImage image;

        int minX;

        int minY;

        int w;

        int h;

        /**
         * @param iterator
         * @param roiAccessor
         * @param image
         * @param minX
         * @param minY
         * @param w
         * @param h
         */
        public RoiAccessor(RandomIter iterator, ROI roi, PlanarImage image, int minX, int minY,
                int w, int h) {
            super();
            this.iterator = iterator;
            this.roi = roi;
            this.image = image;
            this.minX = minX;
            this.minY = minY;
            this.w = w;
            this.h = h;
        }

        public void dispose() {
            image.dispose();
            iterator.done();
            roi = null;
        }

    }

    private final static double RGB_TO_GRAY_MATRIX[][] = { { 0.114, 0.587, 0.299, 0 } };

    private final double[] backgroundValues;

    private final int numBands;

    private final BorderExtender sourceExtender;

    private int filterSize;

    private ROI thresholdRoi;

    private ROI sourceROI;

    private PlanarImage thresholdRoiImg;

    private PlanarImage sourceROIimg;

    private final boolean hasNoData;

    private Range nodata;

    private DataTypeCalculator calculator;

    /**
     * Base constructor for a {@link PixelRestorationOpImage}
     * 
     * @param source the input {@link RenderedImage}
     * @param layout the optional {@link ImageLayout}
     * @param config
     * @param sourceROI a {@link ROI} representing pixels to be restored.
     * @param backgroundValues the value of the background pixel values.
     * @param threshold luminance threshold
     * @param filterSize size of the filter
     * @param nodata Range for the input NoData to check
     */
    public ArtifactsFilterOpImage(final RenderedImage source, final ImageLayout layout,
            final Map<?, ?> config, final ROI sourceROI, double[] backgroundValues,
            final int threshold, final int filterSize, final Range nodata) {
        super(source, layout, config, true);

        RenderedImage inputRI = source;
        // iter = RandomIterFactory.create(inputRI, null);
        final int tr = inputRI.getColorModel().getTransparency();
        // Set the band count.
        this.numBands = sampleModel.getNumBands();

        this.filterSize = filterSize;

        // Save the ROI array.
        this.sourceROI = sourceROI;
        thresholdRoi = null;
        if (sourceROI != null) {
            RenderedImage image = inputRI;
            if (threshold != Integer.MAX_VALUE) {
                if (numBands == 3) {
                    image = BandCombineDescriptor.create(image, RGB_TO_GRAY_MATRIX, null);
                } else {
                    // do we have transparency
                    // combination matrix

                    final double fillValue = tr == Transparency.OPAQUE ? 1.0 / numBands
                            : 1.0 / (numBands - 1);

                    final double[][] matrix = new double[1][numBands + 1];
                    for (int i = 0; i < numBands; i++) {
                        matrix[0][i] = fillValue;
                    }

                    image = BandCombineDescriptor.create(image, matrix, null);
                }
                thresholdRoi = new ROI(image, threshold);
                thresholdRoi = thresholdRoi.intersect(sourceROI);
            }
        }

        // Copy the background values per the specification.
        this.backgroundValues = new double[numBands];
        if (backgroundValues == null) {
            backgroundValues = new double[] { 0.0 };
        }

        if (backgroundValues.length < numBands) {
            Arrays.fill(this.backgroundValues, backgroundValues[0]);
        } else {
            System.arraycopy(backgroundValues, 0, this.backgroundValues, 0, numBands);
        }

        hasNoData = nodata != null;
        if (hasNoData) {
            this.nodata = nodata;
        }

        final int dataType = sampleModel.getDataType();
        DataTypeCalculator calc = null;

        // Determine constant value for source BORDER extension.
        double sourceExtensionConstant;
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            sourceExtensionConstant = 0.0;
            calc = DataTypeCalculator.BYTE;
            break;
        case DataBuffer.TYPE_USHORT:
            sourceExtensionConstant = 0.0;
            calc = DataTypeCalculator.SHORT;
            break;
        case DataBuffer.TYPE_SHORT:
            sourceExtensionConstant = Short.MIN_VALUE;
            calc = DataTypeCalculator.SHORT;
            break;
        case DataBuffer.TYPE_INT:
            sourceExtensionConstant = Integer.MIN_VALUE;
            calc = DataTypeCalculator.INTEGER;
            break;
        case DataBuffer.TYPE_FLOAT:
            sourceExtensionConstant = -Float.MAX_VALUE;
            calc = DataTypeCalculator.FLOAT;
            break;
        case DataBuffer.TYPE_DOUBLE:
        default:
            sourceExtensionConstant = -Double.MAX_VALUE;
            calc = DataTypeCalculator.DOUBLE;
        }
        this.calculator = calc;
        this.sourceExtender = sourceExtensionConstant == 0.0 ? BorderExtender
                .createInstance(BorderExtender.BORDER_ZERO) : new BorderExtenderConstant(
                new double[] { sourceExtensionConstant });
    }

    private RoiAccessor buildRoiAccessor(boolean threshold) {
        if (threshold) {
            if (thresholdRoi != null) {
                final PlanarImage roiImage = getROIThresholdImage();
                final RandomIter roiIter = RandomIterFactory.create(roiImage, null, TILE_CACHED,
                        ARRAY_CALC);
                final int minRoiX = roiImage.getMinX();
                final int minRoiY = roiImage.getMinY();
                final int roiW = roiImage.getWidth();
                final int roiH = roiImage.getHeight();
                return new RoiAccessor(roiIter, thresholdRoi, roiImage, minRoiX, minRoiY, roiW,
                        roiH);
            }
        } else {
            if (sourceROI != null) {
                final PlanarImage roiImage = getROIImage();
                final RandomIter roiIter = RandomIterFactory.create(roiImage, null, TILE_CACHED,
                        ARRAY_CALC);
                final int minRoiX = roiImage.getMinX();
                final int minRoiY = roiImage.getMinY();
                final int roiW = roiImage.getWidth();
                final int roiH = roiImage.getHeight();
                return new RoiAccessor(roiIter, sourceROI, roiImage, minRoiX, minRoiY, roiW, roiH);
            }
        }
        return null;
    }

    @Override
    public Raster computeTile(final int tileX, final int tileY) {
        // Create a new Raster.
        final WritableRaster dest = createWritableRaster(sampleModel, new Point(tileXToX(tileX),
                tileYToY(tileY)));

        // Determine the active area; tile intersects with image's bounds.
        final Rectangle destRect = getTileRect(tileX, tileY);

        final int numSources = getNumSources();

        Raster rasterSources = null;

        // Cobble areas
        for (int i = 0; i < numSources; i++) {
            final PlanarImage source = getSourceImage(i);
            final Rectangle srcRect = mapDestRect(destRect, i);

            // If srcRect is empty, set the Raster for this source to
            // null; otherwise pass srcRect to getData(). If srcRect
            // is null, getData() will return a Raster containing the
            // data of the entire source image.
            rasterSources = srcRect != null && srcRect.isEmpty() ? null : source.getExtendedData(
                    destRect, sourceExtender);

        }

        computeRect(rasterSources, dest, destRect);

        final Raster sourceData = rasterSources;
        if (sourceData != null) {
            final PlanarImage source = getSourceImage(0);

            // Recycle the source tile
            if (source.overlapsMultipleTiles(sourceData.getBounds())) {
                recycleTile(sourceData);
            }
        }

        return dest;
    }

    private void computeRect(final Raster source, final WritableRaster destinationRaster,
            final Rectangle destRect) {
        // Clear the background and return if no sources.
        if (source == null) {
            ImageUtil.fillBackground(destinationRaster, destRect, backgroundValues);
            return;
        }

        // Determine the format tag id.
        final SampleModel[] sourceSM = new SampleModel[] { source.getSampleModel() };
        final int formatTagID = RasterAccessor.findCompatibleTag(sourceSM,
                destinationRaster.getSampleModel());

        // Create dest accessor.
        final RasterAccessor rasterAccessor = new RasterAccessor(destinationRaster, destRect,
                new RasterFormatTag(destinationRaster.getSampleModel(), formatTagID), null);

        final int dataType = rasterAccessor.getDataType();
        // Branch to data type-specific method.
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            computeRectByte(source, rasterAccessor);
            break;
        case DataBuffer.TYPE_USHORT:
            computeRectShort(source, rasterAccessor, true);
            break;
        case DataBuffer.TYPE_SHORT:
            computeRectShort(source, rasterAccessor, false);
            break;
        case DataBuffer.TYPE_INT:
            computeRectInt(source, rasterAccessor);
            break;
        case DataBuffer.TYPE_FLOAT:
            computeRectFloat(source, rasterAccessor);
            break;
        case DataBuffer.TYPE_DOUBLE:
            computeRectDouble(source, rasterAccessor);
            break;
        default:
            throw new UnsupportedOperationException(
                    "The following datatype isn't actually supported " + dataType);
        }

        rasterAccessor.copyDataToRaster();
    }

    /**
     * Compute operation for the provided dest.
     * 
     * @param dest
     */
    private void computeRectByte(Raster source, RasterAccessor dest) {
        int dwidth = dest.getWidth();
        int dheight = dest.getHeight();
        int dnumBands = dest.getNumBands();

        byte dstDataArrays[][] = dest.getByteDataArrays();
        int dstBandOffsets[] = dest.getBandOffsets();
        int dstPixelStride = dest.getPixelStride();
        int dstScanlineStride = dest.getScanlineStride();

        final int x = dest.getX();
        final int y = dest.getY();

        int valuess[][] = new int[filterSize * filterSize][dnumBands];
        int min = -(filterSize / 2);
        int max = filterSize / 2;
        int dstPixelOffset[] = new int[dnumBands];
        int dstScanlineOffset[] = new int[dnumBands];
        int val[] = new int[dnumBands];
        int valueCount = 0;
        boolean readOriginalValues = false;
        // Iterator on the source tile
        RandomIter iter = RandomIterFactory.create(source, null, TILE_CACHED, ARRAY_CALC);

        // Setting ROI accessors
        RoiAccessor roiAccessor = buildRoiAccessor(false);
        RoiAccessor thresholdRoiAccessor = buildRoiAccessor(true);

        for (int k = 0; k < dnumBands; k++) {
            dstScanlineOffset[k] = dstBandOffsets[k];
        }

        if (hasNoData) {
            for (int j = 0; j < dheight; j++) {
                for (int k = 0; k < dnumBands; k++) {
                    dstPixelOffset[k] = dstScanlineOffset[k];
                }

                for (int i = 0; i < dwidth; i++) {
                    valueCount = 0;
                    readOriginalValues = false;
                    for (int k = 0; k < dnumBands; k++) {
                        val[k] = Integer.MIN_VALUE;
                    }

                    // //
                    //
                    // Pixels outside the ROI will be forced to background color
                    //
                    // //
                    boolean insideRoi = contains(roiAccessor, x + i, y + j);

                    if (insideRoi) {

                        // //
                        //
                        // Artifact filtering is applied only on ROI BORDER
                        //
                        // //
                        // boolean isBorder = isBorder(roiAccessor, x+i, y+j);
                        // if (isBorder) {

                        // //
                        //
                        // If the actual pixel luminance is less then the threshold value,
                        // filter it
                        //
                        // //
                        if (!contains(thresholdRoiAccessor, x + i, y + j)) {

                            // //
                            //
                            // Checking sourrounding pixels
                            //
                            // //
                            for (int u = min; u <= max; u++) {
                                for (int v = min; v <= max; v++) {
                                    boolean set = false;

                                    // //
                                    //
                                    // Neighbour pixel is in ROI and its luminance value is greater than
                                    // the threshold. Then use it for computation
                                    //
                                    // //
                                    if (/* contains(roiAccessor, x+i+v, y+j+u) && */contains(
                                            thresholdRoiAccessor, x + i + v, y + j + u)) {
                                        set = true;
                                    }
                                    if (set) {
                                        int[] data = new int[valuess[0].length];
                                        iter.getPixel(x + i + v, y + j + u, data);
                                        boolean isValid = isValid(data);
                                        if (isValid) {
                                            valuess[valueCount++] = data;
                                        }
                                    }

                                }
                            }

                            if (valueCount == 0) {
                                // Last attempt to get more valid pixels by looking at the borders
                                for (int u = min - 1; u <= max + 1; u += (filterSize + 1)) {
                                    for (int v = min - 1; v <= max + 1; v += (filterSize + 1)) {
                                        boolean set = false;
                                        if (/* contains(roiAccessor, x+i+v, y+j+u) && */contains(
                                                thresholdRoiAccessor, x + i + v, y + j + u)) {
                                            set = true;
                                        }
                                        if (set) {
                                            int[] data = new int[valuess[0].length];
                                            iter.getPixel(x + i + v, y + j + u, data);
                                            boolean isValid = isValid(data);
                                            if (isValid) {
                                                valuess[valueCount++] = data;
                                            }
                                        }
                                    }
                                }
                            }
                            // //
                            //
                            // Compute filter value from sourrounding pixel values
                            //
                            // //
                            if (valueCount > 0) {
                                DataTypeCalculator.computeValueAtOnce(valuess, valueCount, val,
                                        numBands);
                            } else {
                                readOriginalValues = true;
                            }
                        } else {
                            readOriginalValues = true;
                        }
                        for (int k = 0; k < dnumBands; k++) {
                            dstDataArrays[k][dstPixelOffset[k]] = (byte) val[k];
                        }
                    } else {
                        readOriginalValues = true;
                    }
                    if (readOriginalValues) {
                        for (int k = 0; k < dnumBands; k++) {
                            val[k] = (int) iter.getSample(x + i, y + j, k) & 0xff;
                            if (nodata.contains((byte) val[k])) {
                                dstDataArrays[k][dstPixelOffset[k]] = 0;
                            } else {
                                dstDataArrays[k][dstPixelOffset[k]] = (byte) val[k];
                            }
                        }
                    }

                    for (int k = 0; k < dnumBands; k++) {
                        dstPixelOffset[k] += dstPixelStride;
                    }
                }
                for (int k = 0; k < dnumBands; k++) {
                    dstScanlineOffset[k] += dstScanlineStride;
                }
            }
        } else {
            for (int j = 0; j < dheight; j++) {
                for (int k = 0; k < dnumBands; k++) {
                    dstPixelOffset[k] = dstScanlineOffset[k];
                }

                for (int i = 0; i < dwidth; i++) {
                    valueCount = 0;
                    readOriginalValues = false;
                    for (int k = 0; k < dnumBands; k++) {
                        val[k] = Integer.MIN_VALUE;
                    }

                    // //
                    //
                    // Pixels outside the ROI will be forced to background color
                    //
                    // //
                    boolean insideRoi = contains(roiAccessor, x + i, y + j);

                    if (insideRoi) {

                        // //
                        //
                        // Artifact filtering is applied only on ROI BORDER
                        //
                        // //
                        // boolean isBorder = isBorder(roiAccessor, x+i, y+j);
                        // if (isBorder) {

                        // //
                        //
                        // If the actual pixel luminance is less then the threshold value,
                        // filter it
                        //
                        // //
                        if (!contains(thresholdRoiAccessor, x + i, y + j)) {

                            // //
                            //
                            // Checking sourrounding pixels
                            //
                            // //
                            for (int u = min; u <= max; u++) {
                                for (int v = min; v <= max; v++) {
                                    boolean set = false;

                                    // //
                                    //
                                    // Neighbour pixel is in ROI and its luminance value is greater than
                                    // the threshold. Then use it for computation
                                    //
                                    // //
                                    if (/* contains(roiAccessor, x+i+v, y+j+u) && */contains(
                                            thresholdRoiAccessor, x + i + v, y + j + u)) {
                                        set = true;
                                    }
                                    if (set) {
                                        iter.getPixel(x + i + v, y + j + u, valuess[valueCount++]);
                                    }

                                }
                            }

                            if (valueCount == 0) {
                                // Last attempt to get more valid pixels by looking at the borders
                                for (int u = min - 1; u <= max + 1; u += (filterSize + 1)) {
                                    for (int v = min - 1; v <= max + 1; v += (filterSize + 1)) {
                                        boolean set = false;
                                        if (/* contains(roiAccessor, x+i+v, y+j+u) && */contains(
                                                thresholdRoiAccessor, x + i + v, y + j + u)) {
                                            set = true;
                                        }
                                        if (set) {
                                            iter.getPixel(x + i + v, y + j + u,
                                                    valuess[valueCount++]);
                                        }
                                    }
                                }
                            }
                            // //
                            //
                            // Compute filter value from sourrounding pixel values
                            //
                            // //
                            if (valueCount > 0) {
                                DataTypeCalculator.computeValueAtOnce(valuess, valueCount, val,
                                        numBands);
                            } else {
                                readOriginalValues = true;
                            }
                        } else {
                            readOriginalValues = true;
                        }
                        for (int k = 0; k < dnumBands; k++) {
                            dstDataArrays[k][dstPixelOffset[k]] = (byte) val[k];
                        }
                    } else {
                        readOriginalValues = true;
                    }
                    if (readOriginalValues) {
                        for (int k = 0; k < dnumBands; k++) {
                            val[k] = (int) iter.getSample(x + i, y + j, k) & 0xff;
                            dstDataArrays[k][dstPixelOffset[k]] = (byte) val[k];
                        }
                    }

                    for (int k = 0; k < dnumBands; k++) {
                        dstPixelOffset[k] += dstPixelStride;
                    }
                }
                for (int k = 0; k < dnumBands; k++) {
                    dstScanlineOffset[k] += dstScanlineStride;
                }
            }
        }

        if (roiAccessor != null) {
            roiAccessor.dispose();
            roiAccessor = null;
        }

        if (thresholdRoiAccessor != null) {
            thresholdRoiAccessor.dispose();
            thresholdRoiAccessor = null;
        }
    }

    /**
     * Compute operation for the provided dest.
     * 
     * @param dest
     * @param isUshort
     */
    private void computeRectShort(Raster source, RasterAccessor dest, boolean isUshort) {
        int dwidth = dest.getWidth();
        int dheight = dest.getHeight();
        int dnumBands = dest.getNumBands();

        short dstDataArrays[][] = dest.getShortDataArrays();
        int dstBandOffsets[] = dest.getBandOffsets();
        int dstPixelStride = dest.getPixelStride();
        int dstScanlineStride = dest.getScanlineStride();

        final int x = dest.getX();
        final int y = dest.getY();

        int valuess[][] = new int[filterSize * filterSize][dnumBands];
        int min = -(filterSize / 2);
        int max = filterSize / 2;
        int dstPixelOffset[] = new int[dnumBands];
        int dstScanlineOffset[] = new int[dnumBands];
        int val[] = new int[dnumBands];
        int valueCount = 0;
        boolean readOriginalValues = false;
        // Iterator on the source tile
        RandomIter iter = RandomIterFactory.create(source, null, TILE_CACHED, ARRAY_CALC);

        // Setting ROI accessors
        RoiAccessor roiAccessor = buildRoiAccessor(false);
        RoiAccessor thresholdRoiAccessor = buildRoiAccessor(true);

        for (int k = 0; k < dnumBands; k++) {
            dstScanlineOffset[k] = dstBandOffsets[k];
        }

        short nodataValue = isUshort ? 0 : Short.MIN_VALUE;

        if (hasNoData) {
            for (int j = 0; j < dheight; j++) {
                for (int k = 0; k < dnumBands; k++) {
                    dstPixelOffset[k] = dstScanlineOffset[k];
                }

                for (int i = 0; i < dwidth; i++) {
                    valueCount = 0;
                    readOriginalValues = false;
                    for (int k = 0; k < dnumBands; k++) {
                        val[k] = Integer.MIN_VALUE;
                    }

                    boolean insideRoi = contains(roiAccessor, x + i, y + j);

                    if (insideRoi) {

                        if (!contains(thresholdRoiAccessor, x + i, y + j)) {

                            for (int u = min; u <= max; u++) {
                                for (int v = min; v <= max; v++) {
                                    boolean set = false;

                                    if (/* contains(roiAccessor, x+i+v, y+j+u) && */contains(
                                            thresholdRoiAccessor, x + i + v, y + j + u)) {
                                        set = true;
                                    }
                                    if (set) {
                                        int[] data = new int[valuess[0].length];
                                        iter.getPixel(x + i + v, y + j + u, data);
                                        boolean isValid = isValid(data);
                                        if (isValid) {
                                            valuess[valueCount++] = data;
                                        }
                                    }

                                }
                            }

                            if (valueCount == 0) {
                                // Last attempt to get more valid pixels by looking at the borders
                                for (int u = min - 1; u <= max + 1; u += (filterSize + 1)) {
                                    for (int v = min - 1; v <= max + 1; v += (filterSize + 1)) {
                                        boolean set = false;
                                        if (/* contains(roiAccessor, x+i+v, y+j+u) && */contains(
                                                thresholdRoiAccessor, x + i + v, y + j + u)) {
                                            set = true;
                                        }
                                        if (set) {
                                            int[] data = new int[valuess[0].length];
                                            iter.getPixel(x + i + v, y + j + u, data);
                                            boolean isValid = isValid(data);
                                            if (isValid) {
                                                valuess[valueCount++] = data;
                                            }
                                        }
                                    }
                                }
                            }
                            if (valueCount > 0) {
                                DataTypeCalculator.computeValueAtOnce(valuess, valueCount, val,
                                        numBands);
                            } else {
                                readOriginalValues = true;
                            }
                        } else {
                            readOriginalValues = true;
                        }
                        for (int k = 0; k < dnumBands; k++) {
                            dstDataArrays[k][dstPixelOffset[k]] = (short) val[k];
                        }
                    } else {
                        readOriginalValues = true;
                    }
                    if (readOriginalValues) {
                        for (int k = 0; k < dnumBands; k++) {
                            val[k] = (int) iter.getSample(x + i, y + j, k);
                            if (nodata.contains((short) val[k])) {
                                dstDataArrays[k][dstPixelOffset[k]] = nodataValue;
                            } else {
                                dstDataArrays[k][dstPixelOffset[k]] = (short) val[k];
                            }
                        }
                    }

                    for (int k = 0; k < dnumBands; k++) {
                        dstPixelOffset[k] += dstPixelStride;
                    }
                }
                for (int k = 0; k < dnumBands; k++) {
                    dstScanlineOffset[k] += dstScanlineStride;
                }
            }
        } else {
            for (int j = 0; j < dheight; j++) {
                for (int k = 0; k < dnumBands; k++) {
                    dstPixelOffset[k] = dstScanlineOffset[k];
                }

                for (int i = 0; i < dwidth; i++) {
                    valueCount = 0;
                    readOriginalValues = false;
                    for (int k = 0; k < dnumBands; k++) {
                        val[k] = Integer.MIN_VALUE;
                    }

                    boolean insideRoi = contains(roiAccessor, x + i, y + j);

                    if (insideRoi) {

                        if (!contains(thresholdRoiAccessor, x + i, y + j)) {

                            for (int u = min; u <= max; u++) {
                                for (int v = min; v <= max; v++) {
                                    boolean set = false;

                                    if (contains(thresholdRoiAccessor, x + i + v, y + j + u)) {
                                        set = true;
                                    }
                                    if (set) {
                                        iter.getPixel(x + i + v, y + j + u, valuess[valueCount++]);
                                    }

                                }
                            }

                            if (valueCount == 0) {
                                // Last attempt to get more valid pixels by looking at the borders
                                for (int u = min - 1; u <= max + 1; u += (filterSize + 1)) {
                                    for (int v = min - 1; v <= max + 1; v += (filterSize + 1)) {
                                        boolean set = false;
                                        if (contains(thresholdRoiAccessor, x + i + v, y + j + u)) {
                                            set = true;
                                        }
                                        if (set) {
                                            iter.getPixel(x + i + v, y + j + u,
                                                    valuess[valueCount++]);
                                        }
                                    }
                                }
                            }
                            if (valueCount > 0) {
                                DataTypeCalculator.computeValueAtOnce(valuess, valueCount, val,
                                        numBands);
                            } else {
                                readOriginalValues = true;
                            }
                        } else {
                            readOriginalValues = true;
                        }
                        for (int k = 0; k < dnumBands; k++) {
                            dstDataArrays[k][dstPixelOffset[k]] = (short) val[k];
                        }
                    } else {
                        readOriginalValues = true;
                    }
                    if (readOriginalValues) {
                        for (int k = 0; k < dnumBands; k++) {
                            val[k] = iter.getSample(x + i, y + j, k);
                            dstDataArrays[k][dstPixelOffset[k]] = (short) val[k];
                        }
                    }

                    for (int k = 0; k < dnumBands; k++) {
                        dstPixelOffset[k] += dstPixelStride;
                    }
                }
                for (int k = 0; k < dnumBands; k++) {
                    dstScanlineOffset[k] += dstScanlineStride;
                }
            }
        }

        if (roiAccessor != null) {
            roiAccessor.dispose();
            roiAccessor = null;
        }

        if (thresholdRoiAccessor != null) {
            thresholdRoiAccessor.dispose();
            thresholdRoiAccessor = null;
        }
    }

    /**
     * Compute operation for the provided dest.
     * 
     * @param dest
     */
    private void computeRectInt(Raster source, RasterAccessor dest) {
        int dwidth = dest.getWidth();
        int dheight = dest.getHeight();
        int dnumBands = dest.getNumBands();

        int dstDataArrays[][] = dest.getIntDataArrays();
        int dstBandOffsets[] = dest.getBandOffsets();
        int dstPixelStride = dest.getPixelStride();
        int dstScanlineStride = dest.getScanlineStride();

        final int x = dest.getX();
        final int y = dest.getY();

        int valuess[][] = new int[filterSize * filterSize][dnumBands];
        int min = -(filterSize / 2);
        int max = filterSize / 2;
        int dstPixelOffset[] = new int[dnumBands];
        int dstScanlineOffset[] = new int[dnumBands];
        int val[] = new int[dnumBands];
        int valueCount = 0;
        boolean readOriginalValues = false;
        // Iterator on the source tile
        RandomIter iter = RandomIterFactory.create(source, null, TILE_CACHED, ARRAY_CALC);

        // Setting ROI accessors
        RoiAccessor roiAccessor = buildRoiAccessor(false);
        RoiAccessor thresholdRoiAccessor = buildRoiAccessor(true);

        for (int k = 0; k < dnumBands; k++) {
            dstScanlineOffset[k] = dstBandOffsets[k];
        }

        if (hasNoData) {
            for (int j = 0; j < dheight; j++) {
                for (int k = 0; k < dnumBands; k++) {
                    dstPixelOffset[k] = dstScanlineOffset[k];
                }

                for (int i = 0; i < dwidth; i++) {
                    valueCount = 0;
                    readOriginalValues = false;
                    for (int k = 0; k < dnumBands; k++) {
                        val[k] = Integer.MIN_VALUE;
                    }

                    boolean insideRoi = contains(roiAccessor, x + i, y + j);

                    if (insideRoi) {

                        if (!contains(thresholdRoiAccessor, x + i, y + j)) {

                            for (int u = min; u <= max; u++) {
                                for (int v = min; v <= max; v++) {
                                    boolean set = false;

                                    if (contains(thresholdRoiAccessor, x + i + v, y + j + u)) {
                                        set = true;
                                    }
                                    if (set) {
                                        int[] data = new int[valuess[0].length];
                                        iter.getPixel(x + i + v, y + j + u, data);
                                        boolean isValid = isValid(data);
                                        if (isValid) {
                                            valuess[valueCount++] = data;
                                        }
                                    }

                                }
                            }

                            if (valueCount == 0) {
                                // Last attempt to get more valid pixels by looking at the borders
                                for (int u = min - 1; u <= max + 1; u += (filterSize + 1)) {
                                    for (int v = min - 1; v <= max + 1; v += (filterSize + 1)) {
                                        boolean set = false;
                                        if (contains(thresholdRoiAccessor, x + i + v, y + j + u)) {
                                            set = true;
                                        }
                                        if (set) {
                                            int[] data = new int[valuess[0].length];
                                            iter.getPixel(x + i + v, y + j + u, data);
                                            boolean isValid = isValid(data);
                                            if (isValid) {
                                                valuess[valueCount++] = data;
                                            }
                                        }
                                    }
                                }
                            }
                            if (valueCount > 0) {
                                DataTypeCalculator.computeValueAtOnce(valuess, valueCount, val,
                                        numBands);
                            } else {
                                readOriginalValues = true;
                            }
                        } else {
                            readOriginalValues = true;
                        }
                        for (int k = 0; k < dnumBands; k++) {
                            dstDataArrays[k][dstPixelOffset[k]] = val[k];
                        }
                    } else {
                        readOriginalValues = true;
                    }
                    if (readOriginalValues) {
                        for (int k = 0; k < dnumBands; k++) {
                            val[k] = (int) iter.getSample(x + i, y + j, k);
                            if (nodata.contains(val[k])) {
                                dstDataArrays[k][dstPixelOffset[k]] = Integer.MIN_VALUE;
                            } else {
                                dstDataArrays[k][dstPixelOffset[k]] = val[k];
                            }
                        }
                    }

                    for (int k = 0; k < dnumBands; k++) {
                        dstPixelOffset[k] += dstPixelStride;
                    }
                }
                for (int k = 0; k < dnumBands; k++) {
                    dstScanlineOffset[k] += dstScanlineStride;
                }
            }
        } else {
            for (int j = 0; j < dheight; j++) {
                for (int k = 0; k < dnumBands; k++) {
                    dstPixelOffset[k] = dstScanlineOffset[k];
                }

                for (int i = 0; i < dwidth; i++) {
                    valueCount = 0;
                    readOriginalValues = false;
                    for (int k = 0; k < dnumBands; k++) {
                        val[k] = Integer.MIN_VALUE;
                    }

                    boolean insideRoi = contains(roiAccessor, x + i, y + j);

                    if (insideRoi) {

                        if (!contains(thresholdRoiAccessor, x + i, y + j)) {

                            for (int u = min; u <= max; u++) {
                                for (int v = min; v <= max; v++) {
                                    boolean set = false;

                                    if (contains(thresholdRoiAccessor, x + i + v, y + j + u)) {
                                        set = true;
                                    }
                                    if (set) {
                                        iter.getPixel(x + i + v, y + j + u, valuess[valueCount++]);
                                    }

                                }
                            }

                            if (valueCount == 0) {
                                // Last attempt to get more valid pixels by looking at the borders
                                for (int u = min - 1; u <= max + 1; u += (filterSize + 1)) {
                                    for (int v = min - 1; v <= max + 1; v += (filterSize + 1)) {
                                        boolean set = false;
                                        if (contains(thresholdRoiAccessor, x + i + v, y + j + u)) {
                                            set = true;
                                        }
                                        if (set) {
                                            iter.getPixel(x + i + v, y + j + u,
                                                    valuess[valueCount++]);
                                        }
                                    }
                                }
                            }

                            if (valueCount > 0) {
                                DataTypeCalculator.computeValueAtOnce(valuess, valueCount, val,
                                        numBands);
                            } else {
                                readOriginalValues = true;
                            }
                        } else {
                            readOriginalValues = true;
                        }
                        for (int k = 0; k < dnumBands; k++) {
                            dstDataArrays[k][dstPixelOffset[k]] = val[k];
                        }
                    } else {
                        readOriginalValues = true;
                    }
                    if (readOriginalValues) {
                        for (int k = 0; k < dnumBands; k++) {
                            val[k] = iter.getSample(x + i, y + j, k);
                            dstDataArrays[k][dstPixelOffset[k]] = val[k];
                        }
                    }

                    for (int k = 0; k < dnumBands; k++) {
                        dstPixelOffset[k] += dstPixelStride;
                    }
                }
                for (int k = 0; k < dnumBands; k++) {
                    dstScanlineOffset[k] += dstScanlineStride;
                }
            }
        }

        if (roiAccessor != null) {
            roiAccessor.dispose();
            roiAccessor = null;
        }

        if (thresholdRoiAccessor != null) {
            thresholdRoiAccessor.dispose();
            thresholdRoiAccessor = null;
        }
    }

    /**
     * Compute operation for the provided dest.
     * 
     * @param dest
     */
    private void computeRectFloat(Raster source, RasterAccessor dest) {
        int dwidth = dest.getWidth();
        int dheight = dest.getHeight();
        int dnumBands = dest.getNumBands();

        float dstDataArrays[][] = dest.getFloatDataArrays();
        int dstBandOffsets[] = dest.getBandOffsets();
        int dstPixelStride = dest.getPixelStride();
        int dstScanlineStride = dest.getScanlineStride();

        final int x = dest.getX();
        final int y = dest.getY();

        float valuess[][] = new float[filterSize * filterSize][dnumBands];
        int min = -(filterSize / 2);
        int max = filterSize / 2;
        int dstPixelOffset[] = new int[dnumBands];
        int dstScanlineOffset[] = new int[dnumBands];
        float val[] = new float[dnumBands];
        int valueCount = 0;
        boolean readOriginalValues = false;
        // Iterator on the source tile
        RandomIter iter = RandomIterFactory.create(source, null, TILE_CACHED, ARRAY_CALC);

        // Setting ROI accessors
        RoiAccessor roiAccessor = buildRoiAccessor(false);
        RoiAccessor thresholdRoiAccessor = buildRoiAccessor(true);

        for (int k = 0; k < dnumBands; k++) {
            dstScanlineOffset[k] = dstBandOffsets[k];
        }

        if (hasNoData) {
            for (int j = 0; j < dheight; j++) {
                for (int k = 0; k < dnumBands; k++) {
                    dstPixelOffset[k] = dstScanlineOffset[k];
                }

                for (int i = 0; i < dwidth; i++) {
                    valueCount = 0;
                    readOriginalValues = false;
                    for (int k = 0; k < dnumBands; k++) {
                        val[k] = -Float.MAX_VALUE;
                    }

                    boolean insideRoi = contains(roiAccessor, x + i, y + j);

                    if (insideRoi) {

                        if (!contains(thresholdRoiAccessor, x + i, y + j)) {

                            for (int u = min; u <= max; u++) {
                                for (int v = min; v <= max; v++) {
                                    boolean set = false;

                                    if (contains(thresholdRoiAccessor, x + i + v, y + j + u)) {
                                        set = true;
                                    }
                                    if (set) {
                                        float[] data = new float[valuess[0].length];
                                        iter.getPixel(x + i + v, y + j + u, data);
                                        boolean isValid = isValid(data);
                                        if (isValid) {
                                            valuess[valueCount++] = data;
                                        }
                                    }

                                }
                            }

                            if (valueCount == 0) {
                                // Last attempt to get more valid pixels by looking at the borders
                                for (int u = min - 1; u <= max + 1; u += (filterSize + 1)) {
                                    for (int v = min - 1; v <= max + 1; v += (filterSize + 1)) {
                                        boolean set = false;
                                        if (contains(thresholdRoiAccessor, x + i + v, y + j + u)) {
                                            set = true;
                                        }
                                        if (set) {
                                            float[] data = new float[valuess[0].length];
                                            iter.getPixel(x + i + v, y + j + u, data);
                                            boolean isValid = isValid(data);
                                            if (isValid) {
                                                valuess[valueCount++] = data;
                                            }
                                        }
                                    }
                                }
                            }
                            if (valueCount > 0) {
                                DataTypeCalculator.computeValueAtOnce(valuess, valueCount, val,
                                        numBands);
                            } else {
                                readOriginalValues = true;
                            }
                        } else {
                            readOriginalValues = true;
                        }
                        for (int k = 0; k < dnumBands; k++) {
                            dstDataArrays[k][dstPixelOffset[k]] = val[k];
                        }
                    } else {
                        readOriginalValues = true;
                    }
                    if (readOriginalValues) {
                        for (int k = 0; k < dnumBands; k++) {
                            val[k] = iter.getSampleFloat(x + i, y + j, k);
                            if (nodata.contains(val[k])) {
                                dstDataArrays[k][dstPixelOffset[k]] = -Float.MAX_VALUE;
                            } else {
                                dstDataArrays[k][dstPixelOffset[k]] = val[k];
                            }
                        }
                    }

                    for (int k = 0; k < dnumBands; k++) {
                        dstPixelOffset[k] += dstPixelStride;
                    }
                }
                for (int k = 0; k < dnumBands; k++) {
                    dstScanlineOffset[k] += dstScanlineStride;
                }
            }
        } else {
            for (int j = 0; j < dheight; j++) {
                for (int k = 0; k < dnumBands; k++) {
                    dstPixelOffset[k] = dstScanlineOffset[k];
                }

                for (int i = 0; i < dwidth; i++) {
                    valueCount = 0;
                    readOriginalValues = false;
                    for (int k = 0; k < dnumBands; k++) {
                        val[k] = -Float.MAX_VALUE;
                    }

                    boolean insideRoi = contains(roiAccessor, x + i, y + j);

                    if (insideRoi) {

                        if (!contains(thresholdRoiAccessor, x + i, y + j)) {

                            for (int u = min; u <= max; u++) {
                                for (int v = min; v <= max; v++) {
                                    boolean set = false;

                                    if (contains(thresholdRoiAccessor, x + i + v, y + j + u)) {
                                        set = true;
                                    }
                                    if (set) {
                                        iter.getPixel(x + i + v, y + j + u, valuess[valueCount++]);
                                    }

                                }
                            }

                            if (valueCount == 0) {
                                // Last attempt to get more valid pixels by looking at the borders
                                for (int u = min - 1; u <= max + 1; u += (filterSize + 1)) {
                                    for (int v = min - 1; v <= max + 1; v += (filterSize + 1)) {
                                        boolean set = false;
                                        if (contains(thresholdRoiAccessor, x + i + v, y + j + u)) {
                                            set = true;
                                        }
                                        if (set) {
                                            iter.getPixel(x + i + v, y + j + u,
                                                    valuess[valueCount++]);
                                        }
                                    }
                                }
                            }
                            if (valueCount > 0) {
                                DataTypeCalculator.computeValueAtOnce(valuess, valueCount, val,
                                        numBands);
                            } else {
                                readOriginalValues = true;
                            }
                        } else {
                            readOriginalValues = true;
                        }
                        for (int k = 0; k < dnumBands; k++) {
                            dstDataArrays[k][dstPixelOffset[k]] = val[k];
                        }
                    } else {
                        readOriginalValues = true;
                    }
                    if (readOriginalValues) {
                        for (int k = 0; k < dnumBands; k++) {
                            val[k] = iter.getSampleFloat(x + i, y + j, k);
                            dstDataArrays[k][dstPixelOffset[k]] = val[k];
                        }
                    }

                    for (int k = 0; k < dnumBands; k++) {
                        dstPixelOffset[k] += dstPixelStride;
                    }
                }
                for (int k = 0; k < dnumBands; k++) {
                    dstScanlineOffset[k] += dstScanlineStride;
                }
            }
        }

        if (roiAccessor != null) {
            roiAccessor.dispose();
            roiAccessor = null;
        }

        if (thresholdRoiAccessor != null) {
            thresholdRoiAccessor.dispose();
            thresholdRoiAccessor = null;
        }
    }

    /**
     * Compute operation for the provided dest.
     * 
     * @param dest
     */
    private void computeRectDouble(Raster source, RasterAccessor dest) {
        int dwidth = dest.getWidth();
        int dheight = dest.getHeight();
        int dnumBands = dest.getNumBands();

        double dstDataArrays[][] = dest.getDoubleDataArrays();
        int dstBandOffsets[] = dest.getBandOffsets();
        int dstPixelStride = dest.getPixelStride();
        int dstScanlineStride = dest.getScanlineStride();

        final int x = dest.getX();
        final int y = dest.getY();

        double valuess[][] = new double[filterSize * filterSize][dnumBands];
        int min = -(filterSize / 2);
        int max = filterSize / 2;
        int dstPixelOffset[] = new int[dnumBands];
        int dstScanlineOffset[] = new int[dnumBands];
        double val[] = new double[dnumBands];
        int valueCount = 0;
        boolean readOriginalValues = false;
        // Iterator on the source tile
        RandomIter iter = RandomIterFactory.create(source, null, TILE_CACHED, ARRAY_CALC);

        // Setting ROI accessors
        RoiAccessor roiAccessor = buildRoiAccessor(false);
        RoiAccessor thresholdRoiAccessor = buildRoiAccessor(true);

        for (int k = 0; k < dnumBands; k++) {
            dstScanlineOffset[k] = dstBandOffsets[k];
        }

        if (hasNoData) {
            for (int j = 0; j < dheight; j++) {
                for (int k = 0; k < dnumBands; k++) {
                    dstPixelOffset[k] = dstScanlineOffset[k];
                }

                for (int i = 0; i < dwidth; i++) {
                    valueCount = 0;
                    readOriginalValues = false;
                    for (int k = 0; k < dnumBands; k++) {
                        val[k] = -Double.MAX_VALUE;
                    }

                    boolean insideRoi = contains(roiAccessor, x + i, y + j);

                    if (insideRoi) {

                        if (!contains(thresholdRoiAccessor, x + i, y + j)) {

                            for (int u = min; u <= max; u++) {
                                for (int v = min; v <= max; v++) {
                                    boolean set = false;

                                    if (contains(thresholdRoiAccessor, x + i + v, y + j + u)) {
                                        set = true;
                                    }
                                    if (set) {
                                        double[] data = new double[valuess[0].length];
                                        iter.getPixel(x + i + v, y + j + u, data);
                                        boolean isValid = isValid(data);
                                        if (isValid) {
                                            valuess[valueCount++] = data;
                                        }
                                    }

                                }
                            }

                            if (valueCount == 0) {
                                // Last attempt to get more valid pixels by looking at the borders
                                for (int u = min - 1; u <= max + 1; u += (filterSize + 1)) {
                                    for (int v = min - 1; v <= max + 1; v += (filterSize + 1)) {
                                        boolean set = false;
                                        if (contains(thresholdRoiAccessor, x + i + v, y + j + u)) {
                                            set = true;
                                        }
                                        if (set) {
                                            double[] data = new double[valuess[0].length];
                                            iter.getPixel(x + i + v, y + j + u, data);
                                            boolean isValid = isValid(data);
                                            if (isValid) {
                                                valuess[valueCount++] = data;
                                            }
                                        }
                                    }
                                }
                            }
                            if (valueCount > 0) {
                                DataTypeCalculator.computeValueAtOnce(valuess, valueCount, val,
                                        numBands);
                            } else {
                                readOriginalValues = true;
                            }
                        } else {
                            readOriginalValues = true;
                        }
                        for (int k = 0; k < dnumBands; k++) {
                            dstDataArrays[k][dstPixelOffset[k]] = val[k];
                        }
                    } else {
                        readOriginalValues = true;
                    }
                    if (readOriginalValues) {
                        for (int k = 0; k < dnumBands; k++) {
                            val[k] = iter.getSampleDouble(x + i, y + j, k);
                            if (nodata.contains(val[k])) {
                                dstDataArrays[k][dstPixelOffset[k]] = -Double.MAX_VALUE;
                            } else {
                                dstDataArrays[k][dstPixelOffset[k]] = val[k];
                            }
                        }
                    }

                    for (int k = 0; k < dnumBands; k++) {
                        dstPixelOffset[k] += dstPixelStride;
                    }
                }
                for (int k = 0; k < dnumBands; k++) {
                    dstScanlineOffset[k] += dstScanlineStride;
                }
            }
        } else {
            for (int j = 0; j < dheight; j++) {
                for (int k = 0; k < dnumBands; k++) {
                    dstPixelOffset[k] = dstScanlineOffset[k];
                }

                for (int i = 0; i < dwidth; i++) {
                    valueCount = 0;
                    readOriginalValues = false;
                    for (int k = 0; k < dnumBands; k++) {
                        val[k] = -Double.MAX_VALUE;
                    }

                    boolean insideRoi = contains(roiAccessor, x + i, y + j);

                    if (insideRoi) {

                        if (!contains(thresholdRoiAccessor, x + i, y + j)) {

                            for (int u = min; u <= max; u++) {
                                for (int v = min; v <= max; v++) {
                                    boolean set = false;

                                    if (contains(thresholdRoiAccessor, x + i + v, y + j + u)) {
                                        set = true;
                                    }
                                    if (set) {
                                        iter.getPixel(x + i + v, y + j + u, valuess[valueCount++]);
                                    }

                                }
                            }

                            if (valueCount == 0) {
                                // Last attempt to get more valid pixels by looking at the borders
                                for (int u = min - 1; u <= max + 1; u += (filterSize + 1)) {
                                    for (int v = min - 1; v <= max + 1; v += (filterSize + 1)) {
                                        boolean set = false;
                                        if (contains(thresholdRoiAccessor, x + i + v, y + j + u)) {
                                            set = true;
                                        }
                                        if (set) {
                                            iter.getPixel(x + i + v, y + j + u,
                                                    valuess[valueCount++]);
                                        }
                                    }
                                }
                            }
                            if (valueCount > 0) {
                                DataTypeCalculator.computeValueAtOnce(valuess, valueCount, val,
                                        numBands);
                            } else {
                                readOriginalValues = true;
                            }
                        } else {
                            readOriginalValues = true;
                        }
                        for (int k = 0; k < dnumBands; k++) {
                            dstDataArrays[k][dstPixelOffset[k]] = val[k];
                        }
                    } else {
                        readOriginalValues = true;
                    }
                    if (readOriginalValues) {
                        for (int k = 0; k < dnumBands; k++) {
                            val[k] = iter.getSampleDouble(x + i, y + j, k);
                            dstDataArrays[k][dstPixelOffset[k]] = val[k];
                        }
                    }

                    for (int k = 0; k < dnumBands; k++) {
                        dstPixelOffset[k] += dstPixelStride;
                    }
                }
                for (int k = 0; k < dnumBands; k++) {
                    dstScanlineOffset[k] += dstScanlineStride;
                }
            }
        }

        if (roiAccessor != null) {
            roiAccessor.dispose();
            roiAccessor = null;
        }

        if (thresholdRoiAccessor != null) {
            thresholdRoiAccessor.dispose();
            thresholdRoiAccessor = null;
        }
    }

    /**
     * 
     * @param roiAccessor
     * @param x
     * @param y
     * @return
     */
    private final boolean contains(RoiAccessor roiAccessor, int x, int y) {
        return (x >= roiAccessor.minX && x < roiAccessor.minX + roiAccessor.w)
                && (y >= roiAccessor.minY && y < roiAccessor.minY + roiAccessor.h)
                && (roiAccessor.iterator.getSample(x, y, 0) >= 1);
    }

    /**
     * 
     */
    public void dispose() {
        super.dispose();
    }

    private boolean isValid(int[] data) {
        boolean valid = true;
        for (int i = 0; i < data.length; i++) {
            int value = data[i];
            if (calculator.isNoData(nodata, value)) {
                valid = false;
                break;
            }
        }
        return valid;
    }

    private boolean isValid(float[] data) {
        boolean valid = true;
        for (int i = 0; i < data.length; i++) {
            float value = data[i];
            if (calculator.isNoData(nodata, value)) {
                valid = false;
                break;
            }
        }
        return valid;
    }

    private boolean isValid(double[] data) {
        boolean valid = true;
        for (int i = 0; i < data.length; i++) {
            double value = data[i];
            if (calculator.isNoData(nodata, value)) {
                valid = false;
                break;
            }
        }
        return valid;
    }

    /**
     * This method provides a lazy initialization of the image associated to the ROI. The method uses the Double-checked locking in order to maintain
     * thread-safety
     * 
     * @return
     */
    private PlanarImage getROIImage() {
        PlanarImage img = sourceROIimg;
        if (img == null) {
            synchronized (this) {
                img = sourceROIimg;
                if (img == null) {
                    sourceROIimg = img = sourceROI.getAsImage();
                }
            }
        }
        return img;
    }

    /**
     * This method provides a lazy initialization of the image associated to the ROI with the Threshold. The method uses the Double-checked locking in
     * order to maintain thread-safety
     * 
     * @return
     */
    private PlanarImage getROIThresholdImage() {
        PlanarImage img = thresholdRoiImg;
        if (img == null) {
            synchronized (this) {
                img = thresholdRoiImg;
                if (img == null) {
                    thresholdRoiImg = img = thresholdRoi.getAsImage();
                }
            }
        }
        return img;
    }
}
