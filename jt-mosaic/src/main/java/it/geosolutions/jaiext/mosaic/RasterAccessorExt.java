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
import java.awt.image.BandedSampleModel;
import java.awt.image.ColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.Arrays;

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
        int numBands = rft.getNumBands();
        if (isGray(theColorModel, numBands) && (rft.getFormatTagID() & GRAY_EXPANSION_MASK) == GRAY_TO_RGB) {
            int newNumBands = targetBands;
            boolean sourceAlpha = theColorModel.hasAlpha();
            int newBandDataOffsets[] = new int[newNumBands];
            for (int i = 0; i < newBandDataOffsets.length; i++) {
                newBandDataOffsets[i] = this.bandDataOffsets[0];
            }
            int newBandOffsets[] = new int[newNumBands];
            for (int i = 0; i < newBandOffsets.length; i++) {
                newBandOffsets[i] = this.bandOffsets[0];
            }
            // if there is a source alpha, retain it
            if (sourceAlpha) {
                newBandDataOffsets[newBandDataOffsets.length - 1] = this.bandDataOffsets[bandDataOffsets.length - 1];
                newBandOffsets[newBandOffsets.length - 1] = this.bandOffsets[bandOffsets.length - 1];
            }

            switch (formatTagID & DATATYPE_MASK) {
            case DataBuffer.TYPE_BYTE:
                byte byteDataArray[] = byteDataArrays[0];
                byteDataArrays = new byte[newNumBands][];
                for (int i = 0; i < newNumBands; i++) {
                    byteDataArrays[i] = byteDataArray;
                }
                // did we go from opaque to translucent?
                if (numBands == 1 && newNumBands == 4) {
                    byte[] alpha = new byte[byteDataArray.length];
                    Arrays.fill(alpha, (byte) 255);
                    byteDataArrays[3] = alpha;
                }

                break;

            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
                short shortDataArray[] = shortDataArrays[0];
                shortDataArrays = new short[newNumBands][];
                for (int i = 0; i < newNumBands; i++) {
                    shortDataArrays[i] = shortDataArray;
                }
                // did we go from opaque to translucent?
                if (numBands == 1 && newNumBands == 4) {
                    short[] alpha = new short[shortDataArray.length];
                    Arrays.fill(alpha, Short.MAX_VALUE);
                    shortDataArrays[3] = alpha;
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
                // did we go from opaque to translucent?
                if (numBands == 1 && newNumBands == 4) {
                    int[] alpha = new int[intDataArray.length];
                    Arrays.fill(alpha, Integer.MAX_VALUE);
                    intDataArrays[3] = alpha;
                }

                break;

            case DataBuffer.TYPE_FLOAT:
                float floatDataArray[] = floatDataArrays[0];
                floatDataArrays = new float[newNumBands][];
                for (int i = 0; i < newNumBands; i++) {
                    floatDataArrays[i] = floatDataArray;
                }
                // did we go from opaque to translucent? however, don't think it's possible
                // to represent an alpha channel as float...

                break;

            case DataBuffer.TYPE_DOUBLE:
                double doubleDataArray[] = doubleDataArrays[0];
                doubleDataArrays = new double[newNumBands][];
                for (int i = 0; i < newNumBands; i++) {
                    doubleDataArrays[i] = doubleDataArray;
                }
                // did we go from opaque to translucent? however, don't think it's possible
                // to represent an alpha channel as double...

                break;

            }
            this.numBands = newNumBands;
            this.bandDataOffsets = newBandDataOffsets;
            this.bandOffsets = newBandDataOffsets;
        } else {
            SampleModel sampleModel = raster.getSampleModel();
            if (numBands == 1
                    && (rft.getFormatTagID() & GRAY_EXPANSION_MASK) == GRAY_SCALE) {
                int sourceDataType = sampleModel.getDataType();
                if (targetDataType == DataBuffer.TYPE_USHORT
                        && sourceDataType == DataBuffer.TYPE_BYTE) {
                    for (int i = 0; i < intDataArrays.length; i++) {
                        int[] pixels = intDataArrays[i];
                        for (int j = 0; j < pixels.length; j++) {
                            pixels[j] = byteToShort(pixels[j]);
                        }
                    }
                } else if (!(targetDataType == DataBuffer.TYPE_DOUBLE && sourceDataType == DataBuffer.TYPE_FLOAT)) {
                    // the case cited in the if is alredy covered during data extraction, there is no transformation
                    // to be done. If that's not the case instead, throw an exception
                    throw new IllegalArgumentException("Cannot perform gray rescaling from data " +
                            "type "
                            + sourceDataType + " to data type " + targetDataType);
                }
            } else if (numBands == 3 && targetBands == 4 && sampleModel.getDataType() == DataBuffer.TYPE_BYTE) {
                // RGB to RGBA, assuming the output is ordered as RGBA (which is what the mosaic layout does
                // in case of color expansion
                if (sampleModel instanceof ComponentSampleModel) {
                    // turn it into a component representation
                    byte[][] newDataArrays = new byte[targetBands][];
                    for (int i = 0; i < numBands; i++) {
                        newDataArrays[i] = new byte[raster.getWidth() * raster.getHeight()];
                    }
                    // scan the original array and redistribute
                    int width = this.getWidth();
                    int offset = this.scanlineStride - (width * numBands);
                    int interleavedPos = 0;
                    int pos = 0;
                    int height = this.getHeight();
                    int[] bandOffsets = ((ComponentSampleModel) sampleModel).getBandOffsets();
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            for (int b = 0; b < numBands; b++) {
                                newDataArrays[b][pos] = this.byteDataArrays[b][interleavedPos + bandOffsets[b]];
                            }
                            pos++;
                        }
                        interleavedPos += offset;
                    }
                    byte[] alpha = new byte[raster.getWidth() * raster.getHeight()];
                    Arrays.fill(alpha, (byte) 255);
                    newDataArrays[3] = alpha;

                    this.byteDataArrays = newDataArrays;
                    int newBandDataOffsets[] = new int[targetBands];
                    for (int i = 0; i < newBandDataOffsets.length; i++) {
                        newBandDataOffsets[i] = 0;
                    }
                    int newBandOffsets[] = new int[targetBands];
                    for (int i = 0; i < newBandOffsets.length; i++) {
                        newBandOffsets[i] = 0;
                    }
                    this.bandOffsets = newBandOffsets;
                    this.bandDataOffsets = newBandDataOffsets;
                    this.scanlineStride = width;
                    this.pixelStride = 1;
                    this.numBands = 4;
                } else {
                    throw new IllegalArgumentException("Expansion from RGB to RGBA on this sample model not supported: " + sampleModel);
                }
            }
        }
    }

    static boolean isGray(ColorModel theColorModel, int numBands) {
        return numBands == 1 || (numBands == 2 && theColorModel.hasAlpha());
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
                int numBands = src.getSampleModel().getNumBands();
                if ((numBands == 1 || numBands == 2) 
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
            } else if (targetDataType == DataBuffer.TYPE_DOUBLE && sourceDataType == DataBuffer.TYPE_FLOAT) {
                float min = (float) noData.getMin().doubleValue();
                float max = (float) noData.getMax().doubleValue();
                return RangeFactory.create(min, noData.isMinIncluded(), max, noData.isMaxIncluded());
            } else if (targetDataType == DataBuffer.TYPE_FLOAT && sourceDataType == DataBuffer.TYPE_DOUBLE) {
                double min = noData.getMin().floatValue();
                double max = noData.getMax().floatValue();
                return RangeFactory.create(min, noData.isMinIncluded(), max, noData.isMaxIncluded());
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
