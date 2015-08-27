/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2015 GeoSolutions


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

import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;

import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;

/**
 * Extends the RasterAcessor to handle more data type transformation cases
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class RasterAccessorExt extends RasterAccessor {

    /**
     * Value indicating how far GRAY_EXPANSION_MASK_SHIFT info is shifted to avoid interfering with
     * the data type info.
     */
    private static final int GRAY_EXPANSION_MASK_SHIFT = 11;

    /** Value indicating how many bits the GRAY_EXPANSION_MASK is */
    private static final int GRAY_EXPANSION_MASK_SIZE = 1;

    /** The bits of a FormatTag associated with the gray expansion. */
    public static final int GRAY_EXPANSION_MASK = 3 << GRAY_EXPANSION_MASK_SHIFT;

    /** Flag indicating the gray bands should not be expanded */
    public static final int UNEXPANDED = 0x00 << GRAY_EXPANSION_MASK_SHIFT;

    /** Flag indicating the gray bands should be expanded to RGB */
    public static final int GRAY_TO_RGB = 0X01 << GRAY_EXPANSION_MASK_SHIFT;

    /** Flag indicating the gray bands should be rescaled to the target type */
    public static final int GRAY_SCALE = 0X02 << GRAY_EXPANSION_MASK_SHIFT;

    public RasterAccessorExt(Raster raster, Rectangle rect, RasterFormatTag rft,
            ColorModel theColorModel, int targetBands, int targetDataType) {
        super(raster, rect, rft, theColorModel);

        // gray to multiband expansion
        if (rft.getNumBands() == 1 && (rft.getFormatTagID() & GRAY_EXPANSION_MASK) == GRAY_TO_RGB) {
            int newNumBands = targetBands;
            // all zero, we are just replicating the arrays
            int newBandDataOffsets[] = new int[newNumBands];
            for (int i = 0; i < newBandDataOffsets.length; i++) {
                newBandDataOffsets[i] = this.bandDataOffsets[0];
            }
            int newBandOffsets[] = new int[newNumBands];
            for (int i = 0; i < newBandOffsets.length; i++) {
                newBandOffsets[i] = this.bandOffsets[0];
            }

            switch (formatTagID & DATATYPE_MASK) {
            case DataBuffer.TYPE_BYTE:
                byte byteDataArray[] = byteDataArrays[0];
                byteDataArrays = new byte[newNumBands][];
                for (int i = 0; i < newNumBands; i++) {
                    byteDataArrays[i] = byteDataArray;
                }

                break;

            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
                short shortDataArray[] = shortDataArrays[0];
                shortDataArrays = new short[newNumBands][];
                for (int i = 0; i < newNumBands; i++) {
                    shortDataArrays[i] = shortDataArray;
                }

                break;

            case DataBuffer.TYPE_INT:
                // in case of mixed color models, gray ushort and byte, the RasterAccessor
                // sets the tag to int, but we need to crush the data down from ushort to byte
                // before expanding the gray

                int intDataArray[] = intDataArrays[0];
                if (raster.getDataBuffer().getDataType() == DataBuffer.TYPE_USHORT
                        && targetDataType == DataBuffer.TYPE_BYTE) {
                    final int length = intDataArray.length;
                    for (int i = 0; i < length; i++) {
                        int sample = intDataArray[i];
                        intDataArray[i] = shortToByte(sample);
                    }
                }
                intDataArrays = new int[newNumBands][];
                for (int i = 0; i < newNumBands; i++) {
                    intDataArrays[i] = intDataArray;
                }

                break;

            case DataBuffer.TYPE_FLOAT:
                float floatDataArray[] = floatDataArrays[0];
                floatDataArrays = new float[newNumBands][];
                for (int i = 0; i < newNumBands; i++) {
                    floatDataArrays[i] = floatDataArray;
                }

                break;

            case DataBuffer.TYPE_DOUBLE:
                double doubleDataArray[] = doubleDataArrays[0];
                doubleDataArrays = new double[newNumBands][];
                for (int i = 0; i < newNumBands; i++) {
                    doubleDataArrays[i] = doubleDataArray;
                }

                break;

            }
            this.numBands = newNumBands;
            this.bandDataOffsets = newBandDataOffsets;
            this.bandOffsets = newBandDataOffsets;
        } else if (rft.getNumBands() == 1
                && (rft.getFormatTagID() & GRAY_EXPANSION_MASK) == GRAY_SCALE) {
            int sourceDataType = raster.getSampleModel().getDataType();
            if (targetDataType == DataBuffer.TYPE_USHORT
                    && sourceDataType == DataBuffer.TYPE_BYTE) {
                for (int i = 0; i < intDataArrays.length; i++) {
                    int[] pixels = intDataArrays[i];
                    for (int j = 0; j < pixels.length; j++) {
                        pixels[j] = byteToShort(pixels[j]);
                    }
                }
            } else {
                throw new IllegalArgumentException("Cannot perform gray rescaling from data type "
                        + sourceDataType + " to data type " + targetDataType);
            }
        }
    }

    /**
     * Rescales a ushort sample down into the byte range
     * 
     * @param sample
     * @return
     */
    private static final int shortToByte(int sample) {
        return (int) Math.round((sample / 65536d) * 255);
    }

    /**
     * Rescales a byte to the ushort range
     * 
     * @param theByte
     * @return
     */
    private static final int byteToShort(int theByte) {
        double d = theByte;
        return (int) Math.round((d / 255) * 65535);
    }

    /**
     * Finds the appropriate tags for the constructor, based on the SampleModel and ColorModel of
     * all the source and destination.
     *
     * @param srcs The operations sources; may be <code>null</code> which is taken to be equivalent
     *        to zero sources.
     * @param dst The operation destination.
     * @return An array containing <code>RasterFormatTag</code>s for the sources in the first
     *         src.length elements and a <code>RasterFormatTag</code> for the destination in the
     *         last element.
     * @throws NullPointerException if <code>dst</code> is <code>null</code>.
     */
    public static RasterFormatTag[] findCompatibleTags(RenderedImage srcs[], RenderedImage dst) {
        RasterFormatTag[] tags = RasterAccessor.findCompatibleTags(srcs, dst);
        // check if we need to perform gray expansion
        if (dst.getSampleModel().getNumBands() > 1) {
            for (int i = 0; i < srcs.length; i++) {
                RenderedImage src = srcs[i];
                if (src.getSampleModel().getNumBands() == 1
                        && !(src.getColorModel() instanceof IndexColorModel)) {
                    tags[i] = applyMask(src, tags[i], GRAY_TO_RGB);
                }
            }
        } else if (dst.getSampleModel().getNumBands() == 1
                && !(dst.getColorModel() instanceof IndexColorModel)) {
            int destinationDataType = dst.getSampleModel().getDataType();
            for (int i = 0; i < srcs.length; i++) {
                RenderedImage src = srcs[i];

                int sourceDataType = src.getSampleModel().getDataType();
                if (destinationDataType != sourceDataType) {
                    tags[i] = applyMask(src, tags[i], GRAY_SCALE);
                }
            }
        }

        return tags;
    }

    private static RasterFormatTag applyMask(RenderedImage src, RasterFormatTag oldTag, int mask) {
        int tagId = oldTag.getFormatTagID() | mask;
        RasterFormatTag newTag = new RasterFormatTag(src.getSampleModel(), tagId);
        return newTag;
    }

    public static Range expandNoData(Range noData, RasterFormatTag rft, RenderedImage sourceImage,
            RenderedImage destImage) {
        int formatTagID = rft.getFormatTagID();

        // handle gray to gray (we get the output sample model as the RasterAccessor sets the
        // target data type to int in case of mixed sample models, but eventually things work out
        int targetDataType = destImage.getSampleModel().getDataType();
        int formatDataType = formatTagID & DATATYPE_MASK;
        int sourceDataType = sourceImage.getSampleModel().getDataType();
        int destinationDataType = destImage.getSampleModel().getDataType();
        if (rft.getNumBands() == 1 && (rft.getFormatTagID() & GRAY_EXPANSION_MASK) == GRAY_TO_RGB
                && formatDataType == DataBuffer.TYPE_INT && sourceDataType == DataBuffer.TYPE_USHORT
                && destinationDataType == DataBuffer.TYPE_BYTE) {
            int min = noData.getMin().intValue();
            int max = noData.getMax().intValue();
            byte scaledMin = (byte) (shortToByte(min) & 0xFF);
            byte scaledMax = (byte) (shortToByte(max) & 0xFF);
            return RangeFactory.create(scaledMin, noData.isMinIncluded(), scaledMax,
                    noData.isMaxIncluded());
        } else if (rft.getNumBands() == 1
                && (rft.getFormatTagID() & GRAY_EXPANSION_MASK) == GRAY_SCALE) {
            if (targetDataType == DataBuffer.TYPE_USHORT
                    && sourceDataType == DataBuffer.TYPE_BYTE) {
                int min = noData.getMin().intValue();
                int max = noData.getMax().intValue();
                int expandedMin = byteToShort(min);
                int expandedMax = byteToShort(max);
                return RangeFactory.create(expandedMin, noData.isMinIncluded(), expandedMax,
                        noData.isMaxIncluded());
            } else {
                throw new IllegalArgumentException("Cannot perform gray rescaling from data type "
                        + sourceDataType + " to data type " + targetDataType);
            }
        }

        return noData;
    }

    /**
     * Returns true if palette expansion is required
     * 
     * @param sourceImage
     * @param formatTagID
     * @return
     */
    static boolean isPaletteExpansionRequired(RenderedImage sourceImage, int formatTagID) {
        return (formatTagID & EXPANSION_MASK) == EXPANDED
                && sourceImage.getColorModel() instanceof IndexColorModel;
    }

}
