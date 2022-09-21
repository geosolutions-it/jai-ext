/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2022 GeoSolutions


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

import javax.media.jai.RasterAccessor;
import java.awt.Rectangle;

/**
 * Provides access to a Raster pixels with a few optimizations:
 * <ul>
 *     <li>Extract the pixels only at the first time they are needed (lazy loading)</li>
 *     <li>Loads only the data at the intersection between source data and desired rectangle,
 *     without allocating padded pixels, using offsets internally</li>
 *     <li>Tries to reduce computations in pixel access to the minimum, e.g. moving some shared computations to the nextLine method for example</li>
 *     <li>Reduces to the minimum the amount of array access, for example, by providing a method to retrieve pixels made of just one band</li>
 * </ul>
 * <p>
 * Functionality wise, it's similar to a {@link javax.media.jai.iterator.RectIter}, but performs
 * raw access to the data banks rathern than going through the sample model.
 */
abstract class PixelIterator {
    protected final RasterAccessor rasterAccessor;
    protected final int bands;

    private final int sourceMinX;
    private final int sourceMinY;

    private final int sourceMaxX;
    private final int sourceMaxY;

    private final int destMinX;

    private final int[] lineOffsets;

    protected final int[] pixelOffsets;

    private final int pixelStride;

    private final int lineStride;

    protected int x;
    protected int y;

    private final boolean overlap;

    private boolean hasData;
    private boolean hasDataY;

    public PixelIterator(Rectangle sourceRect, Rectangle destRect,
                         RasterAccessor rasterAccessor) {
        this.rasterAccessor = rasterAccessor;

        this.lineStride = rasterAccessor.getScanlineStride();
        this.pixelStride = rasterAccessor.getPixelStride();
        int[] bandOffsets = rasterAccessor.getBandOffsets();

        this.bands = bandOffsets.length;
        this.lineOffsets = new int[bands];
        this.pixelOffsets = new int[bands];
        for (int b = 0; b < bands; b++) {
            this.pixelOffsets[b] = bandOffsets[b];
            this.lineOffsets[b] = bandOffsets[b] + lineStride;
        }

        // The overlap flag optimizes the common zoomed-in case where the source image
        // fully covers the output. The hasData flags check if the current pixel has data,
        // the Y component is updated only once per line so it's kept in a separate variable to
        // reduce updates
        overlap = sourceRect.equals(destRect);
        hasDataY = overlap || sourceRect.y == destRect.y;
        hasData = overlap || ((sourceRect.x == destRect.x) && hasDataY);

        // initialize positions
        x = destRect.x;
        y = destRect.y;


        // limits
        this.sourceMinX = sourceRect.x;
        this.sourceMinY = sourceRect.y;
        this.sourceMaxX = sourceRect.x + sourceRect.width;
        this.sourceMaxY = sourceRect.y + sourceRect.height;
        this.destMinX = destRect.x;
    }

    /**
     * Moves to the next pixel
     */
    public final void nextPixel() {
        // move the virtual pixel
        x++;
        // if the previous pixel had data, move on
        if (hasData) {
            for (int b = 0; b < pixelOffsets.length; b++) {
                this.pixelOffsets[b] += pixelStride;
            }
        }
        // check if the new pixel is intercepting actual data
        hasData = overlap || (hasDataY && x >= sourceMinX && x < sourceMaxX);
    }

    public final void nextLine() {
        // update the virtual cursor
        y++;
        x = destMinX;
        // the actual pixel offsets increment only past the first line of the input
        if (y > sourceMinY) {
            for (int b = 0; b < lineOffsets.length; b++) {
                this.pixelOffsets[b] = lineOffsets[b];
                lineOffsets[b] += lineStride;
            }
        }
        hasDataY = overlap || (y >= sourceMinY && y < sourceMaxY);
        hasData = overlap || (hasDataY && x >= sourceMinX && x < sourceMaxX);
    }

    /**
     * Returns true if the current pixel has data
     *
     * @return
     */
    public final boolean hasData() {
        return hasData;
    }

    /**
     * Returns true if the current reader cannot intercept source data anymore and can be
     * ignored in future iterations on the data
     *
     * @return
     */
    public final boolean isDone() {
        return y >= sourceMaxY || (y == (sourceMaxY - 1) && x >= sourceMaxX);
    }

    public static final class PixelIteratorByte extends PixelIterator {

        private byte[][] data;
        private final byte[] pixel;

        public PixelIteratorByte(Rectangle sourceRect, Rectangle destRect,
                                 RasterAccessor rasterAccessor) {
            super(sourceRect, destRect, rasterAccessor);
            this.pixel = new byte[this.bands];
        }

        public byte[] read() {
            byte[][] data = getData();
            for (int b = 0; b < this.pixel.length; b++) {
                pixel[b] = data[b][pixelOffsets[b]];
            }
            return pixel;
        }

        public byte readOne() {
            byte[][] data = getData();
            return data[0][pixelOffsets[0]];
        }

        private byte[][] getData() {
            if (data == null) data = rasterAccessor.getByteDataArrays();
            return data;
        }
    }

    public static final class PixelIteratorShort extends PixelIterator {

        private short[][] data;
        private final short[] pixel;

        public PixelIteratorShort(Rectangle sourceRect, Rectangle destRect,
                                  RasterAccessor rasterAccessor) {
            super(sourceRect, destRect, rasterAccessor);
            this.pixel = new short[this.bands];
        }

        public short[] read() {
            short[][] data = getData();
            for (int b = 0; b < this.pixel.length; b++) {
                pixel[b] = data[b][pixelOffsets[b]];
            }
            return pixel;
        }

        public short readOne() {
            short[][] data = getData();
            return data[0][pixelOffsets[0]];
        }

        private short[][] getData() {
            if (data == null) data = rasterAccessor.getShortDataArrays();
            return data;
        }
    }

    public static final class PixelIteratorInt extends PixelIterator {

        private int[][] data;
        private final int[] pixel;

        public PixelIteratorInt(Rectangle sourceRect, Rectangle destRect,
                                RasterAccessor rasterAccessor) {
            super(sourceRect, destRect, rasterAccessor);
            this.pixel = new int[this.bands];
        }

        public int[] read() {
            int[][] data = getData();
            for (int b = 0; b < this.pixel.length; b++) {
                pixel[b] = data[b][pixelOffsets[b]];
            }
            return pixel;
        }

        public int readOne() {
            int[][] data = getData();
            return data[0][pixelOffsets[0]];
        }

        private int[][] getData() {
            if (data == null) data = rasterAccessor.getIntDataArrays();
            return data;
        }

    }

    public static final class PixelIteratorFloat extends PixelIterator {

        private float[][] data;
        private final float[] pixel;

        public PixelIteratorFloat(Rectangle sourceRect, Rectangle destRect,
                                  RasterAccessor rasterAccessor) {
            super(sourceRect, destRect, rasterAccessor);
            this.pixel = new float[this.bands];
        }

        public float[] read() {
            float[][] data = getData();
            for (int b = 0; b < this.pixel.length; b++) {
                pixel[b] = data[b][pixelOffsets[b]];
            }
            return pixel;
        }

        public float readOne() {
            float[][] data = getData();
            return data[0][pixelOffsets[0]];
        }

        private float[][] getData() {
            if (data == null) data = rasterAccessor.getFloatDataArrays();
            return data;
        }

    }

    public static final class PixelIteratorDouble extends PixelIterator {

        private double[][] data;
        private final double[] pixel;

        public PixelIteratorDouble(Rectangle sourceRect, Rectangle destRect,
                                  RasterAccessor rasterAccessor) {
            super(sourceRect, destRect, rasterAccessor);
            this.pixel = new double[this.bands];
        }

        public double[] read() {
            double[][] data = getData();
            for (int b = 0; b < this.pixel.length; b++) {
                pixel[b] = data[b][pixelOffsets[b]];
            }
            return pixel;
        }

        public double readOne() {
            double[][] data = getData();
            return data[0][pixelOffsets[0]];
        }

        private double[][] getData() {
            if (data == null) data = rasterAccessor.getDoubleDataArrays();
            return data;
        }

    }
}
