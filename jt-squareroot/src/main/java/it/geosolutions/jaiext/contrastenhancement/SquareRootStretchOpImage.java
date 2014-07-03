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
package it.geosolutions.jaiext.contrastenhancement;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Map;

import javax.media.jai.ImageLayout;
import javax.media.jai.PointOpImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;

import com.sun.media.jai.util.ImageUtil;


/**
 * An <code>OpImage</code> implementing a square root stretch operation.
 *
 * <p>This <code>OpImage</code> does this operation on the pixels values:
 *
 * dest = SQRT(((src/(inMax - inMin)) * outMax) + outMin)
 */
final class SquareRootStretchOpImage extends PointOpImage
{

    /** The inputMin value */
    protected int[] inputMin;

    /** The inputMax value */
    protected int[] inputMax;

    /** The outputMin value */
    protected int[] outputMin;

    /** The outputMax value */
    protected int[] outputMax;


    /**
     * Constructor.
     *
     * @param source     The source image.
     * @param layout     The destination image layout.
     * @param inputMin
     */
    public SquareRootStretchOpImage(RenderedImage source,
        Map config,
        ImageLayout layout,
        int[] inputMin,
        int[] inputMax,
        int[] outputMin,
        int[] outputMax)
    {
        super(source, layout, config, true);

        int numBands = getSampleModel().getNumBands();

        if (inputMin.length < numBands)
        {
            this.inputMin = new int[numBands];
            for (int i = 0; i < numBands; i++)
            {
                this.inputMin[i] = inputMin[0];
            }
        }
        else
        {
            this.inputMin = (int[]) inputMin.clone();
        }

        if (inputMax.length < numBands)
        {
            this.inputMax = new int[numBands];
            for (int i = 0; i < numBands; i++)
            {
                this.inputMax[i] = inputMax[0];
            }
        }
        else
        {
            this.inputMax = (int[]) inputMax.clone();
        }


        if (outputMin.length < numBands)
        {
            this.outputMin = new int[numBands];
            for (int i = 0; i < numBands; i++)
            {
                this.outputMin[i] = outputMin[0];
            }
        }
        else
        {
            this.outputMin = (int[]) outputMin.clone();
        }


        if (outputMax.length < numBands)
        {
            this.outputMax = new int[numBands];
            for (int i = 0; i < numBands; i++)
            {
                this.outputMax[i] = outputMax[0];
            }
        }
        else
        {
            this.outputMax = (int[]) outputMax.clone();
        }


        // Set flag to permit in-place operation.
        permitInPlaceOperation();

    }


    /**
     * Compute the square root stretching
     *
     *
     * @param sources   Cobbled sources, guaranteed to provide all the
     *                  source data necessary for computing the rectangle.
     * @param dest      The tile containing the rectangle to be computed.
     * @param destRect  The rectangle within the tile to be computed.
     */
    protected void computeRect(Raster[] sources,
        WritableRaster dest,
        Rectangle destRect)
    {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        Rectangle srcRect = mapDestRect(destRect, 0);

        RasterAccessor dst = new RasterAccessor(dest, destRect,
                formatTags[1], getColorModel());
        RasterAccessor src = new RasterAccessor(sources[0], srcRect,
                formatTags[0],
                getSource(0).getColorModel());

        switch (dst.getDataType())
        {
        case DataBuffer.TYPE_BYTE:
            computeRectByte(src, dst);
            break;
        case DataBuffer.TYPE_USHORT:
            computeRectUShort(src, dst);
            break;
        case DataBuffer.TYPE_SHORT:
            computeRectShort(src, dst);
            break;
        case DataBuffer.TYPE_INT:
            computeRectInt(src, dst);
            break;
//        case DataBuffer.TYPE_FLOAT:
//            computeRectFloat(src, dst);
//            break;
//        case DataBuffer.TYPE_DOUBLE:
//            computeRectDouble(src, dst);
//            break;
        }

        if (dst.needsClamping())
        {

            /* Further clamp down to underlying raster data type. */
            dst.clampDataArrays();
        }
        dst.copyDataToRaster();
    }

    private void computeRectByte(RasterAccessor src,
        RasterAccessor dst)
    {
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        byte[][] dstData = dst.getByteDataArrays();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        byte[][] srcData = src.getByteDataArrays();

        for (int b = 0; b < dstBands; b++)
        {
            float c = (float) inputMin[b];
            byte[] d = dstData[b];
            byte[] s = srcData[b];

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            for (int h = 0; h < dstHeight; h++)
            {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                for (int w = 0; w < dstWidth; w++)
                {
                    d[dstPixelOffset] = ImageUtil.clampRoundByte(
                            (s[srcPixelOffset] & 0xFF) * c);

                    dstPixelOffset += dstPixelStride;
                    srcPixelOffset += srcPixelStride;
                }
            }
        }
    }

    private void computeRectUShort(RasterAccessor src,
        RasterAccessor dst)
    {
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        short[][] dstData = dst.getShortDataArrays();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        short[][] srcData = src.getShortDataArrays();

        int val = 0;
        for (int b = 0; b < dstBands; b++)
        {
            short[] d = dstData[b];
            short[] s = srcData[b];

            int inMin = inputMin[b];
            int inMax = inputMax[b];
            int outMin = outputMin[b];
            int outMax = outputMax[b];

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            for (int h = 0; h < dstHeight; h++)
            {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                for (int w = 0; w < dstWidth; w++)
                {
                    val = ((s[srcPixelOffset] >= inMin) ? (s[srcPixelOffset] - inMin) : 0);
                    d[dstPixelOffset] = ImageUtil.clampRoundUShort((Math.sqrt((val / (inMax - inMin))) * outMax) + outMin);
                    dstPixelOffset += dstPixelStride;
                    srcPixelOffset += srcPixelStride;
                }
            }
        }
    }

    private void computeRectShort(RasterAccessor src,
        RasterAccessor dst)
    {
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        short[][] dstData = dst.getShortDataArrays();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        short[][] srcData = src.getShortDataArrays();
        double val = 0;
        for (int b = 0; b < dstBands; b++)
        {
            int inMin = inputMin[b];
            int inMax = inputMax[b];
            int outMin = outputMin[b];
            int outMax = outputMax[b];

            short[] d = dstData[b];
            short[] s = srcData[b];

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            for (int h = 0; h < dstHeight; h++)
            {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                for (int w = 0; w < dstWidth; w++)
                {
                    val = ((s[srcPixelOffset] >= inMin) ? (s[srcPixelOffset] - inMin) : 0);

                    d[dstPixelOffset] = ImageUtil.clampRoundShort((Math.sqrt((val / (inMax - inMin))) * outMax) + outMin);

                    dstPixelOffset += dstPixelStride;
                    srcPixelOffset += srcPixelStride;
                }
            }
        }
    }

    private void computeRectInt(RasterAccessor src,
        RasterAccessor dst)
    {
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        int[][] dstData = dst.getIntDataArrays();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        int[][] srcData = src.getIntDataArrays();

        double val = 0;
        for (int b = 0; b < dstBands; b++)
        {
            int inMin = inputMin[b];
            int inMax = inputMax[b];
            int outMin = outputMin[b];
            int outMax = outputMax[b];

            int[] d = dstData[b];
            int[] s = srcData[b];

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            for (int h = 0; h < dstHeight; h++)
            {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                for (int w = 0; w < dstWidth; w++)
                {
                    val = ((s[srcPixelOffset] >= inMin) ? (s[srcPixelOffset] - inMin) : 0);

                    d[dstPixelOffset] = ImageUtil.clampRoundInt((Math.sqrt((val / (inMax - inMin))) * outMax) + outMin);

                    dstPixelOffset += dstPixelStride;
                    srcPixelOffset += srcPixelStride;
                }
            }
        }
    }

//    private void computeRectFloat(RasterAccessor src,
//                                  RasterAccessor dst) {
//        int dstWidth = dst.getWidth();
//        int dstHeight = dst.getHeight();
//        int dstBands = dst.getNumBands();
//
//        int dstLineStride = dst.getScanlineStride();
//        int dstPixelStride = dst.getPixelStride();
//        int[] dstBandOffsets = dst.getBandOffsets();
//        float[][] dstData = dst.getFloatDataArrays();
//
//        int srcLineStride = src.getScanlineStride();
//        int srcPixelStride = src.getPixelStride();
//        int[] srcBandOffsets = src.getBandOffsets();
//        float[][] srcData = src.getFloatDataArrays();
//
//        for (int b = 0; b < dstBands; b++) {
//            double c = inputMin[b];
//            float[] d = dstData[b];
//            float[] s = srcData[b];
//
//            int dstLineOffset = dstBandOffsets[b];
//            int srcLineOffset = srcBandOffsets[b];
//
//            for (int h = 0; h < dstHeight; h++) {
//                int dstPixelOffset = dstLineOffset;
//                int srcPixelOffset = srcLineOffset;
//
//                dstLineOffset += dstLineStride;
//                srcLineOffset += srcLineStride;
//
//                for (int w = 0; w < dstWidth; w++) {
//                    d[dstPixelOffset] = ImageUtil.clampFloat(s[srcPixelOffset] * c);
//
//                    dstPixelOffset += dstPixelStride;
//                    srcPixelOffset += srcPixelStride;
//                }
//            }
//        }
//    }
//
//    private void computeRectDouble(RasterAccessor src,
//                                   RasterAccessor dst) {
//        int dstWidth = dst.getWidth();
//        int dstHeight = dst.getHeight();
//        int dstBands = dst.getNumBands();
//
//        int dstLineStride = dst.getScanlineStride();
//        int dstPixelStride = dst.getPixelStride();
//        int[] dstBandOffsets = dst.getBandOffsets();
//        double[][] dstData = dst.getDoubleDataArrays();
//
//        int srcLineStride = src.getScanlineStride();
//        int srcPixelStride = src.getPixelStride();
//        int[] srcBandOffsets = src.getBandOffsets();
//        double[][] srcData = src.getDoubleDataArrays();
//
//        for (int b = 0; b < dstBands; b++) {
//            double c = inputMin[b];
//            double[] d = dstData[b];
//            double[] s = srcData[b];
//
//            int dstLineOffset = dstBandOffsets[b];
//            int srcLineOffset = srcBandOffsets[b];
//
//            for (int h = 0; h < dstHeight; h++) {
//                int dstPixelOffset = dstLineOffset;
//                int srcPixelOffset = srcLineOffset;
//
//                dstLineOffset += dstLineStride;
//                srcLineOffset += srcLineStride;
//
//                for (int w = 0; w < dstWidth; w++) {
//                    d[dstPixelOffset] = s[srcPixelOffset] * c;
//
//                    dstPixelOffset += dstPixelStride;
//                    srcPixelOffset += srcPixelStride;
//                }
//            }
//        }
//    }
}
