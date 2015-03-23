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
package it.geosolutions.jaiext.classifier;

import it.geosolutions.jaiext.piecewise.DefaultPiecewiseTransform1DElement;

import java.awt.Color;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.util.Arrays;

/**
 * Utility class for doing useful ColorMap operations
 * 
 * @author Nicola Lagomarsini geosolutions
 * 
 */
public class ColorMapUtilities {

    /**
     * Small number for rounding errors.
     */
    private static final double EPS = 1E-6;

    /**
     * Default ARGB code.
     */
    static final int[] DEFAULT_ARGB = { 0xFF000000, 0xFFFFFFFF };

    /**
     * Makes sure that an argument is non-null.
     * 
     * @param name Argument name.
     * @param object User argument.
     * @throws IllegalArgumentException if {@code object} is null.
     */
    static void ensureNonNull(final String name, final Object object)
            throws IllegalArgumentException {
        if (object == null) {
            throw new IllegalArgumentException("Argument: " + name + " is Null");
        }
    }

    /**
     * Compare two doubles taking into account NaN
     */
    static int compare(final double v1, final double v2) {
        if (Double.isNaN(v1) && Double.isNaN(v2)) {
            final long bits1 = Double.doubleToRawLongBits(v1);
            final long bits2 = Double.doubleToRawLongBits(v2);
            if (bits1 < bits2)
                return -1;
            if (bits1 > bits2)
                return +1;
        }
        return Double.compare(v1, v2);
    }

    /**
     * Check that all the output values for the various {@link DefaultConstantPiecewiseTransformElement} are equal.
     * 
     * @param preservingElements array of {@link DefaultConstantPiecewiseTransformElement}s.
     * @return the array of {@link DefaultConstantPiecewiseTransformElement}s if the check is successful.
     * @throws IllegalArgumentException in case the check is unsuccessful.
     */
    static DefaultPiecewiseTransform1DElement[] checkPreservingElements(
            LinearColorMapElement[] preservingElements) {
        if (preservingElements != null) {
            double outval = Double.NaN;
            Color color = null;
            for (int i = 0; i < preservingElements.length; i++) {
                // the no data element must be a linear transform mapping to a single value
                if (!(preservingElements[i] instanceof ConstantColorMapElement))
                    throw new IllegalArgumentException(
                            "The element must be a ConstantColorMapElement");
                final ConstantColorMapElement nc = (ConstantColorMapElement) preservingElements[i];
                if (nc.getColors().length != 1)
                    throw new IllegalArgumentException("Color size must be 1");
                if (i == 0) {
                    outval = nc.getOutputMaximum();
                    color = nc.getColors()[0];
                } else {
                    if (compare(outval, nc.getOutputMaximum()) != 0)
                        throw new IllegalArgumentException("Wrong Color value");
                    if (!color.equals(nc.getColors()[0]))
                        throw new IllegalArgumentException("Wrong Color value");
                }

            }
        }
        return preservingElements;
    }

    /**
     * Copies {@code colors} into array {@code ARGB} from index {@code lower} inclusive to index {@code upper} exclusive. If {@code upper-lower} is
     * not equals to the length of {@code colors} array, then colors will be interpolated.
     * <p>
     * <b>Note:</b> Profiling shows that this method is a "hot spot". It needs to be fast, which is why the implementation is not as straight-forward
     * as it could.
     * 
     * @param colors Colors to copy into the {@code ARGB} array.
     * @param ARGB Array of integer to write ARGB values to.
     * @param lower Index (inclusive) of the first element of {@code ARGB} to change.
     * @param upper Index (exclusive) of the last element of {@code ARGB} to change.
     */
    @SuppressWarnings("fallthrough")
    public static void expand(final Color[] colors, final int[] ARGB, final int lower,
            final int upper) {
        /*
         * Trivial cases.
         */
        switch (colors.length) {
        case 1:
            Arrays.fill(ARGB, lower, upper, colors[0].getRGB()); // fall through
        case 0:
            return; // Note: getRGB() is really getARGB()
        }
        switch (upper - lower) {
        case 1:
            ARGB[lower] = colors[0].getRGB(); // fall through
        case 0:
            return; // Note: getRGB() is really getARGB()
        }
        /*
         * Prepares the coefficients for the iteration. The non-final ones will be updated inside the loop.
         */
        final double scale = (double) (colors.length - 1) / (double) (upper - 1 - lower);
        final int maxBase = colors.length - 2;
        double index = 0;
        int base = 0;
        for (int i = lower;;) {
            final int C0 = colors[base + 0].getRGB();
            final int C1 = colors[base + 1].getRGB();
            final int A0 = (C0 >>> 24) & 0xFF, A1 = ((C1 >>> 24) & 0xFF) - A0;
            final int R0 = (C0 >>> 16) & 0xFF, R1 = ((C1 >>> 16) & 0xFF) - R0;
            final int G0 = (C0 >>> 8) & 0xFF, G1 = ((C1 >>> 8) & 0xFF) - G0;
            final int B0 = (C0) & 0xFF, B1 = ((C1) & 0xFF) - B0;
            final int oldBase = base;
            do {
                final double delta = index - base;
                ARGB[i] = (roundByte(A0 + delta * A1) << 24) | (roundByte(R0 + delta * R1) << 16)
                        | (roundByte(G0 + delta * G1) << 8) | (roundByte(B0 + delta * B1));
                if (++i == upper) {
                    return;
                }
                index = (i - lower) * scale;
                base = Math.min(maxBase, (int) (index + EPS)); // Really want rounding toward 0.
            } while (base == oldBase);
        }
    }

    /**
     * Rounds a float value and clamp the result between 0 and 255 inclusive.
     */
    public static int roundByte(final double value) {
        return (int) Math.min(Math.max(Math.round(value), 0), 255);
    }

    /**
     * Returns a bit count for an {@link IndexColorModel} mapping {@code mapSize} colors. It is guaranteed that the following relation is hold:
     * 
     * <center>
     * 
     * <pre>
     * (1 &lt;&lt; getBitCount(mapSize)) &gt;= mapSize
     * </pre>
     * 
     * </center>
     */
    public static int getBitCount(final int mapSize) {
        int max = mapSize - 1;
        if (max <= 1) {
            return 1;
        }
        int count = 0;
        do {
            count++;
            max >>= 1;
        } while (max != 0);
        assert (1 << count) >= mapSize : mapSize;
        assert (1 << (count - 1)) < mapSize : mapSize;
        return count;
    }

    /**
     * Returns a suggested type for an {@link IndexColorModel} of {@code mapSize} colors. This method returns {@link DataBuffer#TYPE_BYTE} or
     * {@link DataBuffer#TYPE_USHORT}.
     */
    public static int getTransferType(final int mapSize) {
        return (mapSize <= 256) ? DataBuffer.TYPE_BYTE : DataBuffer.TYPE_USHORT;
    }
}
