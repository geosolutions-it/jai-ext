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
package it.geosolutions.jaiext.utilities;

import java.util.List;

import java.awt.*;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.ChoiceFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.media.jai.JAI;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PixelAccessor;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;
import javax.media.jai.UnpackedImageData;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;

import com.sun.media.imageioimpl.common.PackageUtil;

public class ImageUtilities {

    /**
     * {@code true} if JAI media lib is available.
     */
    private static final boolean mediaLibAvailable;
    static {

        // do we wrappers at hand?
        boolean mediaLib = false;
        Class mediaLibImage = null;
        try {
            mediaLibImage = Class.forName("com.sun.medialib.mlib.Image");
        } catch (ClassNotFoundException e) {
        }
        mediaLib = (mediaLibImage != null);

        // npw check if we either wanted to disable explicitly and if we
        // installed the native libs
        if (mediaLib) {

            try {
                // explicit disable
                mediaLib = !Boolean.getBoolean("com.sun.media.jai.disableMediaLib");

                // native libs installed
                if (mediaLib) {
                    final Class mImage = mediaLibImage;
                    mediaLib = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                        public Boolean run() {
                            try {
                                // get the method
                                final Class params[] = {};
                                Method method = mImage.getDeclaredMethod("isAvailable", params);

                                // invoke
                                final Object paramsObj[] = {};

                                final Object o = mImage.newInstance();
                                return (Boolean) method.invoke(o, paramsObj);
                            } catch (Throwable e) {
                                return false;
                            }
                        }
                    });
                }
            } catch (Throwable e) {
                // Because the property com.sun.media.jai.disableMediaLib isn't
                // defined as public, the users shouldn't know it. In most of
                // the cases, it isn't defined, and thus no access permission
                // is granted to it in the policy file. When JAI is utilized in
                // a security environment, AccessControlException will be
                // thrown.
                // In this case, we suppose that the users would like to use
                // medialib accelaration. So, the medialib won't be disabled.

                // The fix of 4531501

                mediaLib = false;
            }

        }

        mediaLibAvailable = mediaLib;
    }

    /**
     * Tells me whether or not the native libraries for JAI are active or not.
     * 
     * @return <code>false</code> in case the JAI native libs are not in the path, <code>true</code> otherwise.
     */
    public static boolean isMediaLibAvailable() {
        return mediaLibAvailable;
    }

    /**
     * Tells me whether or not the native libraries for JAI/ImageIO are active or not.
     * 
     * @return <code>false</code> in case the JAI/ImageIO native libs are not in the path, <code>true</code> otherwise.
     */
    public static boolean isCLibAvailable() {
        return PackageUtil.isCodecLibAvailable();
    }

    /**
     * Returns the next or previous representable number. If {@code amount} is equals to {@code 0}, then this method returns the {@code value}
     * unchanged. Otherwise, The operation performed depends on the specified {@code type}:
     * <ul>
     * <li>
     * <p>
     * If the {@code type} is {@link Double}, then this method is equivalent to invoking {@link #previous(double)} if {@code amount} is equals to
     * {@code -1}, or invoking {@link #next(double)} if {@code amount} is equals to {@code +1}. If {@code amount} is smaller than {@code -1} or
     * greater than {@code +1}, then this method invokes {@link #previous(double)} or {@link #next(double)} in a loop for {@code abs(amount)} times.
     * </p>
     * </li>
     * 
     * <li>
     * <p>
     * If the {@code type} is {@link Float}, then this method is equivalent to invoking {@link #previous(float)} if {@code amount} is equals to
     * {@code -1}, or invoking {@link #next(float)} if {@code amount} is equals to {@code +1}. If {@code amount} is smaller than {@code -1} or greater
     * than {@code +1}, then this method invokes {@link #previous(float)} or {@link #next(float)} in a loop for {@code abs(amount)} times.
     * </p>
     * </li>
     * 
     * <li>
     * <p>
     * If the {@code type} is an {@linkplain #isInteger integer}, then invoking this method is equivalent to computing {@code value + amount}.
     * </p>
     * </li>
     * </ul>
     * 
     * @param type The type. Should be the class of {@link Double}, {@link Float} , {@link Long}, {@link Integer}, {@link Short} or {@link Byte} .
     * @param value The number to rool.
     * @param amount -1 to return the previous representable number, +1 to return the next representable number, or 0 to return the number with no
     *        change.
     * @return One of previous or next representable number as a {@code double}.
     * @throws IllegalArgumentException if {@code type} is not one of supported types.
     */
    public static double rool(final Class type, double value, int amount)
            throws IllegalArgumentException {
        if (Double.class.equals(type)) {
            if (amount < 0) {
                do {
                    value = previous(value);
                } while (++amount != 0);
            } else if (amount != 0) {
                do {
                    value = next(value);
                } while (--amount != 0);
            }
            return value;
        }
        if (Float.class.equals(type)) {
            float vf = (float) value;
            if (amount < 0) {
                do {
                    vf = next(vf, false);
                } while (++amount != 0);
            } else if (amount != 0) {
                do {
                    vf = next(vf, true);
                } while (--amount != 0);
            }
            return vf;
        }
        if (isInteger(type)) {
            return value + amount;
        }
        throw new IllegalArgumentException("Unsupported DataType: " + type);
    }

    /**
     * Returns {@code true} if the specified {@code type} is one of integer types. Integer types includes {@link Long}, {@link Integer}, {@link Short}
     * and {@link Byte}.
     * 
     * @param type The type to test (may be {@code null}).
     * @return {@code true} if {@code type} is the class {@link Long}, {@link Integer}, {@link Short} or {@link Byte}.
     * 
     * @deprecated Moved to {@link Classes}.
     */
    @Deprecated
    public static boolean isInteger(final Class<?> type) {
        return type != null && Long.class.equals(type) || Integer.class.equals(type)
                || Short.class.equals(type) || Byte.class.equals(type);
    }

    /**
     * Finds the least double greater than <var>f</var>. If {@code NaN}, returns same value.
     * 
     * @see java.text.ChoiceFormat#nextDouble
     * 
     * @todo Remove this method when we will be allowed to use Java 6.
     */
    public static double next(final double f) {
        return ChoiceFormat.nextDouble(f);
    }

    /**
     * Finds the greatest double less than <var>f</var>. If {@code NaN}, returns same value.
     * 
     * @see java.text.ChoiceFormat#previousDouble
     * 
     * @todo Remove this method when we will be allowed to use Java 6.
     */
    public static double previous(final double f) {
        return ChoiceFormat.previousDouble(f);
    }

    private static float next(final float f, final boolean positive) {
        final int SIGN = 0x80000000;
        final int POSITIVEINFINITY = 0x7F800000;

        // Filter out NaN's
        if (Float.isNaN(f)) {
            return f;
        }

        // Zero's are also a special case
        if (f == 0f) {
            final float smallestPositiveFloat = Float.intBitsToFloat(1);
            return (positive) ? smallestPositiveFloat : -smallestPositiveFloat;
        }

        // If entering here, d is a nonzero value.
        // Hold all bits in a int for later use.
        final int bits = Float.floatToIntBits(f);

        // Strip off the sign bit.
        int magnitude = bits & ~SIGN;

        // If next float away from zero, increase magnitude.
        // Else decrease magnitude
        if ((bits > 0) == positive) {
            if (magnitude != POSITIVEINFINITY) {
                magnitude++;
            }
        } else {
            magnitude--;
        }

        // Restore sign bit and return.
        final int signbit = bits & SIGN;
        return Float.intBitsToFloat(magnitude | signbit);
    }

    /**
     * Fill the specified rectangle of <code>raster</code> with the provided background values. Suppose the raster is initialized to 0. Thus, for
     * binary data, if the provided background values are 0, do nothing.
     */
    public static void fillBackground(WritableRaster raster, Rectangle rect,
            double[] backgroundValues) {
        rect = rect.intersection(raster.getBounds());
        // int numBands = raster.getSampleModel().getNumBands();
        SampleModel sm = raster.getSampleModel();
        PixelAccessor accessor = new PixelAccessor(sm, null);

        if (isBinary(sm)) {
            // fill binary data
            byte value = (byte) (((int) backgroundValues[0]) & 1);
            if (value == 0)
                return;
            int rectX = rect.x;
            int rectY = rect.y;
            int rectWidth = rect.width;
            int rectHeight = rect.height;

            int dx = rectX - raster.getSampleModelTranslateX();
            int dy = rectY - raster.getSampleModelTranslateY();

            DataBuffer dataBuffer = raster.getDataBuffer();
            MultiPixelPackedSampleModel mpp = (MultiPixelPackedSampleModel) sm;
            int lineStride = mpp.getScanlineStride();
            int eltOffset = dataBuffer.getOffset() + mpp.getOffset(dx, dy);
            int bitOffset = mpp.getBitOffset(dx);

            switch (sm.getDataType()) {
            case DataBuffer.TYPE_BYTE: {
                byte[] data = ((DataBufferByte) dataBuffer).getData();
                int bits = bitOffset & 7;
                int otherBits = (bits == 0) ? 0 : 8 - bits;

                byte mask = (byte) (255 >> bits);
                int lineLength = (rectWidth - otherBits) / 8;
                int bits1 = (rectWidth - otherBits) & 7;
                byte mask1 = (byte) (255 << (8 - bits1));
                // If operating within a single byte, merge masks into one
                // and don't apply second mask after while loop
                if (lineLength == 0) {
                    mask &= mask1;
                    bits1 = 0;
                }

                for (int y = 0; y < rectHeight; y++) {
                    int start = eltOffset;
                    int end = start + lineLength;
                    if (bits != 0)
                        data[start++] |= mask;
                    while (start < end)
                        data[start++] = (byte) 255;
                    if (bits1 != 0)
                        data[start] |= mask1;
                    eltOffset += lineStride;
                }
                break;
            }
            case DataBuffer.TYPE_USHORT: {
                short[] data = ((DataBufferUShort) dataBuffer).getData();
                int bits = bitOffset & 15;
                int otherBits = (bits == 0) ? 0 : 16 - bits;

                short mask = (short) (65535 >> bits);
                int lineLength = (rectWidth - otherBits) / 16;
                int bits1 = (rectWidth - otherBits) & 15;
                short mask1 = (short) (65535 << (16 - bits1));
                // If operating within a single byte, merge masks into one
                // and don't apply second mask after while loop
                if (lineLength == 0) {
                    mask &= mask1;
                    bits1 = 0;
                }

                for (int y = 0; y < rectHeight; y++) {
                    int start = eltOffset;
                    int end = start + lineLength;
                    if (bits != 0)
                        data[start++] |= mask;
                    while (start < end)
                        data[start++] = (short) 0xFFFF;
                    if (bits1 != 0)
                        data[start++] |= mask1;
                    eltOffset += lineStride;
                }
                break;
            }
            case DataBuffer.TYPE_INT: {
                int[] data = ((DataBufferInt) dataBuffer).getData();
                int bits = bitOffset & 31;
                int otherBits = (bits == 0) ? 0 : 32 - bits;

                int mask = 0xFFFFFFFF >> bits;
                int lineLength = (rectWidth - otherBits) / 32;
                int bits1 = (rectWidth - otherBits) & 31;
                int mask1 = 0xFFFFFFFF << (32 - bits1);
                // If operating within a single byte, merge masks into one
                // and don't apply second mask after while loop
                if (lineLength == 0) {
                    mask &= mask1;
                    bits1 = 0;
                }

                for (int y = 0; y < rectHeight; y++) {
                    int start = eltOffset;
                    int end = start + lineLength;
                    if (bits != 0)
                        data[start++] |= mask;
                    while (start < end)
                        data[start++] = 0xFFFFFFFF;
                    if (bits1 != 0)
                        data[start++] |= mask1;
                    eltOffset += lineStride;
                }
                break;
            }

            }
        } else {
            int srcSampleType = accessor.sampleType == PixelAccessor.TYPE_BIT ? DataBuffer.TYPE_BYTE
                    : accessor.sampleType;
            UnpackedImageData uid = accessor.getPixels(raster, rect, srcSampleType, false);
            rect = uid.rect;
            int lineStride = uid.lineStride;
            int pixelStride = uid.pixelStride;

            switch (uid.type) {
            case DataBuffer.TYPE_BYTE:
                byte[][] bdata = uid.getByteData();
                for (int b = 0; b < accessor.numBands; b++) {
                    byte value = (byte) backgroundValues[b];
                    byte[] bd = bdata[b];
                    int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

                    for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineStride) {
                        int lastPixel = lo + rect.width * pixelStride;
                        for (int po = lo; po < lastPixel; po += pixelStride) {
                            bd[po] = value;
                        }
                    }
                }
                break;
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
                short[][] sdata = uid.getShortData();
                for (int b = 0; b < accessor.numBands; b++) {
                    short value = (short) backgroundValues[b];
                    short[] sd = sdata[b];
                    int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

                    for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineStride) {
                        int lastPixel = lo + rect.width * pixelStride;
                        for (int po = lo; po < lastPixel; po += pixelStride) {
                            sd[po] = value;
                        }
                    }
                }
                break;
            case DataBuffer.TYPE_INT:
                int[][] idata = uid.getIntData();
                for (int b = 0; b < accessor.numBands; b++) {
                    int value = (int) backgroundValues[b];
                    int[] id = idata[b];
                    int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

                    for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineStride) {
                        int lastPixel = lo + rect.width * pixelStride;
                        for (int po = lo; po < lastPixel; po += pixelStride) {
                            id[po] = value;
                        }
                    }
                }
                break;
            case DataBuffer.TYPE_FLOAT:
                float[][] fdata = uid.getFloatData();
                for (int b = 0; b < accessor.numBands; b++) {
                    float value = (float) backgroundValues[b];
                    float[] fd = fdata[b];
                    int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

                    for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineStride) {
                        int lastPixel = lo + rect.width * pixelStride;
                        for (int po = lo; po < lastPixel; po += pixelStride) {
                            fd[po] = value;
                        }
                    }
                }
                break;
            case DataBuffer.TYPE_DOUBLE:
                double[][] ddata = uid.getDoubleData();
                for (int b = 0; b < accessor.numBands; b++) {
                    double value = backgroundValues[b];
                    double[] dd = ddata[b];
                    int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

                    for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineStride) {
                        int lastPixel = lo + rect.width * pixelStride;
                        for (int po = lo; po < lastPixel; po += pixelStride) {
                            dd[po] = value;
                        }
                    }
                }
                break;
            }
        }
    }

    /**
     * Check whether a <code>SampleModel</code> represents a binary data set, i.e., a single band of data with one bit per pixel packed into a
     * <code>MultiPixelPackedSampleModel</code>.
     */
    public static boolean isBinary(SampleModel sm) {
        return sm instanceof MultiPixelPackedSampleModel
                && ((MultiPixelPackedSampleModel) sm).getPixelBitStride() == 1
                && sm.getNumBands() == 1;
    }

    /**
     * Creates a new TiledImage object with a single band of constant value.
     * The data type of the image corresponds to the class of {@code value}.
     *
     * @param width image width in pixels
     *
     * @param height image height in pixels
     *
     * @param value the constant value to fill the image
     *
     * @return a new TiledImage object
     */
    public static TiledImage createConstantImage(int width, int height, Number value) {
        return createConstantImage(width, height, new Number[] {value});
    }

    /**
     * Creates a new TiledImage object with a single band of constant value.
     * The data type of the image corresponds to the class of {@code value}.
     *
     * @param minx minimum image X ordinate
     *
     * @param miny minimum image Y ordinate
     *
     * @param width image width in pixels
     *
     * @param height image height in pixels
     *
     * @param value the constant value to fill the image
     *
     * @return a new TiledImage object
     */
    public static TiledImage createConstantImage(int minx, int miny, int width, int height, Number value) {
        return createConstantImage(minx, miny, width, height, new Number[] {value});
    }

    /**
     * Creates a new TiledImage object with one or more bands of constant value.
     * The number of bands in the output image corresponds to the length of
     * the input values array and the data type of the image corresponds to the
     * {@code Number} class used.
     *
     * @param width image width in pixels
     *
     * @param height image height in pixels
     *
     * @param values array of values (must contain at least one element)
     *
     * @return a new TiledImage object
     */
    public static TiledImage createConstantImage(int width, int height, Number[] values) {
        return createConstantImage(0, 0, width, height, values);
    }

    /**
     * Creates a new TiledImage object with one or more bands of constant value.
     * The number of bands in the output image corresponds to the length of
     * the input values array and the data type of the image corresponds to the
     * {@code Number} class used.
     *
     * @param minx minimum image X ordinate
     *
     * @param miny minimum image Y ordinate
     *
     * @param width image width in pixels
     *
     * @param height image height in pixels
     *
     * @param values array of values (must contain at least one element)
     *
     * @return a new TiledImage object
     */

    public static TiledImage createConstantImage(int minx, int miny, int width, int height, Number[] values) {
        Dimension tileSize = JAI.getDefaultTileSize();
        return createConstantImage(minx, miny, width, height, tileSize.width, tileSize.height, values);
    }
    /**
     * Creates a new TiledImage object with one or more bands of constant value.
     * The number of bands in the output image corresponds to the length of
     * the input values array and the data type of the image corresponds to the
     * {@code Number} class used.
     *
     * @param minx minimum image X ordinate
     *
     * @param miny minimum image Y ordinate
     *
     * @param width image width
     *
     * @param height image height
     *
     * @param tileWidth width of image tiles
     *
     * @param tileHeight height of image tiles
     *
     * @param values array of values (must contain at least one element)
     *
     * @return a new TiledImage object
     */
    public static TiledImage createConstantImage(int minx, int miny, int width, int height,
            int tileWidth, int tileHeight, Number[] values) {
        if (values == null || values.length < 1) {
            throw new IllegalArgumentException("values array must contain at least 1 value");
        }

        final int numBands = values.length;

        double[] doubleValues = null;
        float[] floatValues = null;
        int[] intValues = null;
        Object typedValues = null;
        int dataType = DataBuffer.TYPE_UNDEFINED;

        if (values[0] instanceof Double) {
            doubleValues = new double[values.length];
            dataType = DataBuffer.TYPE_DOUBLE;
            for (int i = 0; i < numBands; i++) doubleValues[i] = (Double) values[i];
            typedValues = doubleValues;

        } else if (values[0] instanceof Float) {
            floatValues = new float[values.length];
            dataType = DataBuffer.TYPE_FLOAT;
            for (int i = 0; i < numBands; i++) floatValues[i] = (Float) values[i];
            typedValues = floatValues;

        } else if (values[0] instanceof Integer) {
            intValues = new int[values.length];
            dataType = DataBuffer.TYPE_INT;
            for (int i = 0; i < numBands; i++) intValues[i] = (Integer) values[i];
            typedValues = intValues;

        } else if (values[0] instanceof Short) {
            intValues = new int[values.length];
            dataType = DataBuffer.TYPE_SHORT;
            for (int i = 0; i < numBands; i++) intValues[i] = (Short) values[i];
            typedValues = intValues;

        } else if (values[0] instanceof Byte) {
            intValues = new int[values.length];
            dataType = DataBuffer.TYPE_BYTE;
            for (int i = 0; i < numBands; i++) intValues[i] = (Byte) values[i];
            typedValues = intValues;

        } else {
            throw new UnsupportedOperationException("Unsupported data type: " +
                    values[0].getClass().getName());
        }

        SampleModel sm = RasterFactory.createPixelInterleavedSampleModel(
                dataType, tileWidth, tileHeight, numBands);

        ColorModel cm = PlanarImage.createColorModel(sm);

        TiledImage tImg = new TiledImage(minx, miny, width, height, 0, 0, sm, cm);

        WritableRaster tile0 = null;
        int tileW = 0, tileH = 0;
        for (int tileY = tImg.getMinTileY(); tileY <= tImg.getMaxTileY(); tileY++) {
            for (int tileX = tImg.getMinTileX(); tileX <= tImg.getMaxTileX(); tileX++) {
                WritableRaster raster = tImg.getWritableTile(tileX, tileY);
                WritableRaster child = raster.createWritableTranslatedChild(0, 0);

                if (tile0 == null) {
                    tile0 = child;
                    tileW = tile0.getWidth();
                    tileH = tile0.getHeight();
                    fillRaster(tile0, tileW, tileH, dataType, typedValues);
                } else {
                    child.setDataElements(0, 0, tile0);
                }
                tImg.releaseWritableTile(tileX, tileY);
            }
        }

        return tImg;
    }

    /**
     * Creates a new single-band TiledImage with the provided values. The
     * {@code array} argument must be of length {@code width} x {@code height}.
     *
     * @param array a 1D array of values for the image
     * @param width image width
     * @param height image height
     * @return the new image
     */
    public static TiledImage createImageFromArray(Number[] array, int width, int height) {
        if (array == null) {
            throw new IllegalArgumentException("array must be non-null");
        }

        if (array.length == 0 || array.length != width * height) {
            throw new IllegalArgumentException(
                    "The array must be non-empty and have length width x height");
        }

        Number val = array[0];
        TiledImage img = createConstantImage(width, height, val);

        if (array[0] instanceof Double) {
            fillImageAsDouble(img, array, width, height);

        } else if (array[0] instanceof Float) {
            fillImageAsFloat(img, array, width, height);

        } else if (array[0] instanceof Integer) {
            fillImageAsInt(img, array, width, height);

        } else if (array[0] instanceof Short) {
            fillImageAsShort(img, array, width, height);

        } else if (array[0] instanceof Byte) {
            fillImageAsByte(img, array, width, height);

        } else {
            throw new UnsupportedOperationException("Unsupported data type: " +
                    array[0].getClass().getName());
        }

        return img;
    }

    private static void fillImageAsDouble(TiledImage img, Number[] array, int width, int height) {
        int k = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setSample(x, y, 0, array[k++].doubleValue());
            }
        }
    }

    private static void fillImageAsFloat(TiledImage img, Number[] array, int width, int height) {
        int k = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setSample(x, y, 0, array[k++].floatValue());
            }
        }
    }

    private static void fillImageAsInt(TiledImage img, Number[] array, int width, int height) {
        int k = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setSample(x, y, 0, array[k++].intValue());
            }
        }
    }

    private static void fillImageAsShort(TiledImage img, Number[] array, int width, int height) {
        int k = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setSample(x, y, 0, array[k++].shortValue());
            }
        }
    }

    private static void fillImageAsByte(TiledImage img, Number[] array, int width, int height) {
        int k = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setSample(x, y, 0, array[k++].byteValue());
            }
        }
    }

    /**
     * Create a set of colours using a simple colour ramp algorithm in the HSB colour space.
     *
     * @param numColours number of colours required
     *
     * @return an array of colours sampled from the HSB space.
     */
    public static Color[] createRampColours(int numColours) {
        return createRampColours(numColours, 0.8f, 0.8f);
    }

    /**
     * Create a set of colours using a simple colour ramp algorithm in the HSB colour space.
     *
     * @param numColours number of colours required
     *
     * @param saturation the saturation of all colours (between 0 and 1)
     *
     * @param brightness the brightness of all colours (between 0 and 1)
     *
     * @return an array of colours sampled from the HSB space between the start and end hues
     */
    public static Color[] createRampColours(int numColours, float saturation, float brightness) {
        return createRampColours(numColours, 0.0f, 1.0f, saturation, brightness);
    }

    /**
     * Create a set of colours using a simple colour ramp algorithm in the HSB colour space.
     * All float arguments should be values between 0 and 1.
     *
     * @param numColours number of colours required
     *
     * @param startHue the starting hue
     *
     * @param endHue the ending hue
     *
     * @param saturation the saturation of all colours
     *
     * @param brightness the brightness of all colours
     *
     * @return an array of colours sampled from the HSB space between the start and end hues
     */
    public static Color[] createRampColours(int numColours, float startHue, float endHue,
            float saturation, float brightness) {

        Color[] colors = new Color[numColours];

        final float increment = numColours > 1 ? (endHue - startHue) / (float)(numColours - 1) : 0f;
        float hue = startHue;
        for (int i = 0; i < numColours; i++) {
            int rgb = Color.HSBtoRGB(hue, saturation, brightness);
            colors[i] = new Color(rgb);
            hue += increment;
        }

        return colors;
    }

    /**
     * Creates a proxy RGB display image for the given data image. The data image should be
     * of integral data type. Only the first band of multi-band images will be used.
     *
     * @param dataImg the data image
     *
     * @param colourTable a lookup table giving colours for each data image value
     *
     * @return a new RGB image
     */
    public static RenderedImage createDisplayImage(RenderedImage dataImg, Map<Integer, Color> colourTable) {

        if (colourTable.size() > 256) {
            throw new IllegalArgumentException("Number of colours can't be more than 256");
        }

        Integer maxKey = null;
        Integer minKey = null;
        for (Integer key : colourTable.keySet()) {
            if (minKey == null) {
                minKey = maxKey = key;

            } else if (key < minKey) {
                minKey = key;
            } else if (key > maxKey) {
                maxKey = key;
            }
        }

        ParameterBlockJAI pb = null;
        RenderedImage lookupImg = dataImg;
        byte[][] lookup = null;
        int offset = 0;

        if (minKey < 0 || maxKey > 255) {
            lookupImg = createConstantImage(dataImg.getWidth(), dataImg.getHeight(), Integer.valueOf(0));

            SortedMap<Integer, Integer> keyTable = new TreeMap<>();
            int k = 0;
            for (Integer key : colourTable.keySet()) {
                keyTable.put(key, k++);
            }

            WritableRectIter iter = RectIterFactory.createWritable((TiledImage)lookupImg, null);
            do {
                do {
                    do {
                        iter.setSample( keyTable.get(iter.getSample()) );
                    } while (!iter.nextPixelDone());
                    iter.startPixels();
                } while (!iter.nextLineDone());
                iter.startLines();
            } while (!iter.nextBandDone());

            lookup = new byte[3][colourTable.size()];
            for (Integer key : keyTable.keySet()) {
                int index = keyTable.get(key);
                int colour = colourTable.get(key).getRGB();
                lookup[0][index] = (byte) ((colour & 0x00ff0000) >> 16);
                lookup[1][index] = (byte) ((colour & 0x0000ff00) >> 8);
                lookup[2][index] = (byte) (colour & 0x000000ff);
            }

        } else {
            lookup = new byte[3][maxKey - minKey + 1];
            offset = minKey;

            for (Integer key : colourTable.keySet()) {
                int colour = colourTable.get(key).getRGB();
                lookup[0][key - offset] = (byte) ((colour & 0x00ff0000) >> 16);
                lookup[1][key - offset] = (byte) ((colour & 0x0000ff00) >> 8);
                lookup[2][key - offset] = (byte) (colour & 0x000000ff);
            }
        }

        pb = new ParameterBlockJAI("Lookup");
        pb.setSource("source0", lookupImg);
        pb.setParameter("table", new LookupTableJAI(lookup, offset));
        RenderedOp displayImg = JAI.create("Lookup", pb);

        return displayImg;
    }

    /**
     * Get the bands of a multi-band image as a list of single-band images. This can
     * be used, for example, to separate the result image returned by the KernelStats
     * operator into separate result images.
     *
     * @param img the multi-band image
     * @return a List of new single-band images
     */
    public static List<RenderedImage> getBandsAsImages(RenderedImage img) {
        List<RenderedImage> images = new ArrayList<>();

        if (img != null) {
            int numBands = img.getSampleModel().getNumBands();
            for (int band = 0; band < numBands; band++) {
                ParameterBlockJAI pb = new ParameterBlockJAI("BandSelect");
                pb.setSource("source0", img);
                pb.setParameter("bandindices", new int[]{band});
                RenderedImage bandImg = JAI.create("BandSelect", pb);
                images.add(bandImg);
            }
        }

        return images;
    }

    /**
     * Get the specified bands of a multi-band image as a list of single-band images. This can
     * be used, for example, to separate the result image returned by the KernelStats
     * operator into separate result images.
     *
     * @param img the multi-band image
     * @param bandIndices a Collection of Integer indices in the range 0 <= i < number of bands
     * @return a List of new single-band images
     */
    public static List<RenderedImage> getBandsAsImages(RenderedImage img, Collection<Integer> bandIndices) {
        List<RenderedImage> images = new ArrayList<>();

        if (img != null) {
            int numBands = img.getSampleModel().getNumBands();
            SortedSet<Integer> sortedIndices = new TreeSet<>();
            sortedIndices.addAll(bandIndices);

            if (sortedIndices.first() < 0 || sortedIndices.last() >= numBands) {
                throw new IllegalArgumentException("band index out of bounds");
            }

            for (Integer band : sortedIndices) {
                ParameterBlockJAI pb = new ParameterBlockJAI("BandSelect");
                pb.setSource("source0", img);
                pb.setParameter("bandindices", new int[]{band});
                RenderedImage bandImg = JAI.create("BandSelect", pb);
                images.add(bandImg);
            }
        }

        return images;
    }

    private static void fillRaster(WritableRaster wr, int w, int h, int dataType, Object typedValues) {
        switch (dataType) {
            case DataBuffer.TYPE_DOUBLE:
            {
                double[] values = (double[]) typedValues;
                fillRasterDouble(wr, w, h, values);
            }
            break;

            case DataBuffer.TYPE_FLOAT:
            {
                float[] values = (float[]) typedValues;
                fillRasterFloat(wr, w, h, values);
            }
            break;

            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_INT:
            {
                int[] values = (int[]) typedValues;
                fillRasterInt(wr, w, h, values);
            }
            break;
        }
    }

    private static void fillRasterDouble(WritableRaster wr, int w, int h, double[] values) {
        WritableRaster child = wr.createWritableTranslatedChild(0, 0);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                child.setPixel(x, y, values);
            }
        }
    }

    private static void fillRasterFloat(WritableRaster wr, int w, int h, float[] values) {
        WritableRaster child = wr.createWritableTranslatedChild(0, 0);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                child.setPixel(x, y, values);
            }
        }
    }

    private static void fillRasterInt(WritableRaster wr, int w, int h, int[] values) {
        WritableRaster child = wr.createWritableTranslatedChild(0, 0);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                child.setPixel(x, y, values);
            }
        }
    }

}
