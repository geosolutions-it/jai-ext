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

import it.geosolutions.jaiext.piecewise.DefaultLinearPiecewiseTransform1DElement;
import it.geosolutions.jaiext.piecewise.MathTransformation;
import it.geosolutions.jaiext.piecewise.PiecewiseTransform1DElement;
import it.geosolutions.jaiext.piecewise.PiecewiseUtilities;
import it.geosolutions.jaiext.range.Range;

import java.awt.Color;
import java.awt.image.IndexColorModel;
import java.util.Arrays;

/**
 * This {@link LinearColorMapElement} is a special implementation of both {@link PiecewiseTransform1DElement} and {@link ColorMapTransformElement}
 * which can be used to do various types of classifications on raster. Specifically the supported types of classifications are unique values,
 * classified and color ramps.
 * <p>
 * The supported types of classifications are {@link LinearColorMapType#TYPE_RAMP} , {@link LinearColorMapType#TYPE_VALUES} and
 * {@link LinearColorMapType#TYPE_INTERVALS} .
 * 
 * @see LinearColorMap
 * @see LinearColorMap.LinearColorMapType
 * @author Simone Giannecchini, GeoSolutions
 * 
 * @source $URL$
 */
public class LinearColorMapElement extends DefaultLinearPiecewiseTransform1DElement implements
        PiecewiseTransform1DElement, ColorMapTransformElement {

    /**
     * UID
     */
    private static final long serialVersionUID = 2216106857184603629L;

    /**
     * {@link Color} s associated to this {@link ColorMapTransformElement} .
     * 
     * @uml.property name="colors"
     */
    private Color[] colors;

    private int hashCode = -1;

    public static LinearColorMapElement create(final CharSequence name, final Color[] colors,
            final Range valueRange, final Range sampleRange) throws IllegalArgumentException {
        return new LinearColorMapElement(name, colors, valueRange, sampleRange);
    }

    public static LinearColorMapElement create(CharSequence name, final Color color,
            final Range inRange, final int outVal) throws IllegalArgumentException {
        return new ConstantColorMapElement(name, color, inRange, outVal);
    }

    /**
     * @see LinearColorMapElement#ClassificationCategory(CharSequence, Color[], NumberRange, NumberRange)
     */
    public static LinearColorMapElement create(final CharSequence name, final Color color,
            final short value, final int sample) throws IllegalArgumentException {
        return new ConstantColorMapElement(name, color, value, sample);
    }

    /**
     * @see LinearColorMapElement#ClassificationCategory(CharSequence, Color[], NumberRange, NumberRange)
     */
    public static LinearColorMapElement create(final CharSequence name, final Color color,
            final int value, final int sample) throws IllegalArgumentException {
        return new ConstantColorMapElement(name, color, value, sample);
    }

    /**
     * @see LinearColorMapElement#ClassificationCategory(CharSequence, Color[], NumberRange, NumberRange)
     */
    public static LinearColorMapElement create(final CharSequence name, final Color color,
            final float value, final int sample) throws IllegalArgumentException {
        return new ConstantColorMapElement(name, color, value, sample);
    }

    /**
     * @see LinearColorMapElement#ClassificationCategory(CharSequence, Color[], NumberRange, NumberRange)
     */
    public static LinearColorMapElement create(final CharSequence name, final Color color,
            final double value, final int sample) throws IllegalArgumentException {
        return new ConstantColorMapElement(name, color, value, sample);
    }

    /**
     * Constructor for a {@link LinearColorMapElement}. It allows users to build a category which is able to map values into integer sample values for
     * further rendering using and {@link IndexColorModel}.
     * 
     * <strong>NOTE</strong> Due to the limitations of the {@link IndexColorModel} we can accept as valid ranges only those that fit between 0 -65535.
     * 
     * @param name for this {@link DomainElement1D}.
     * @param colors to use when rendering values belonging to this {@link DomainElement1D}
     * @param valueRange the input range for this category.
     * @param sampleRange the sample range for this category. It will be used as indexes for the final color map.
     * @throws IllegalArgumentException in case the output range does not respect {@link IndexColorModel} limitations.
     */
    LinearColorMapElement(final CharSequence name, final Color[] colors, final Range valueRange,
            final Range sampleRange) throws IllegalArgumentException {
        super(name, valueRange, checkSampleRange(sampleRange));
        // //@todo check this test
        // final int inEquals = ColorMapUtilities.compare(getInputMaximum(), getInputMinimum());
        // final int outEquals = ColorMapUtilities.compare(getOutputMaximum(), getOutputMinimum());
        // if (inEquals == 0 && outEquals == 0)
        // this.type = LinearColorMap.LinearColorMapType.TYPE_VALUES;
        // else if (outEquals == 0)
        // this.type = LinearColorMap.LinearColorMapType.TYPE_INTERVALS;
        // else {
        // if (isIdentity())
        //
        // this.type = LinearColorMap.LinearColorMapType.TYPE_VALUES;
        // else
        // this.type = LinearColorMap.LinearColorMapType.TYPE_RAMP;
        // }
        // /////////////////////////////////////////////////////////////////////
        //
        // Initialise fields for visualization
        //
        // /////////////////////////////////////////////////////////////////////
        this.colors = new Color[colors.length];
        System.arraycopy(colors, 0, this.colors, 0, colors.length);

    }

    /**
     * This method is responsible for performing a few checks on the provided range in order to make sure we are talking about a valid range for
     * building an {@link IndexColorModel}.
     * 
     * @param numberRange the range to use for mapping values to colors.
     * @return the input {@link NumberRange} if everything goes well.
     * @see IndexColorModel
     */
    private static Range checkSampleRange(Range numberRange) {
        if (numberRange == null)
            throw new IllegalArgumentException();
        final Class<?> elementClass = numberRange.getDataType().getClassValue();
        if (!elementClass.equals(Integer.class) && !elementClass.equals(Byte.class)
                && !elementClass.equals(Short.class))
            throw new IllegalArgumentException(
                    "The following Range cannot be used for mapping value to colours");
        if (numberRange.getMin().intValue() < 0 || numberRange.getMax().intValue() > 65535)
            throw new IndexOutOfBoundsException(
                    "Input Range bounds are outside the available color representation");
        return numberRange;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.geotools.renderer.lite.gridcoverage2d.Category#equals(java.lang.Object)
     */
    public boolean equals(final Object object) {
        if (this == object)
            return true;
        if (!(object instanceof LinearColorMapElement))
            return false;
        final LinearColorMapElement that = (LinearColorMapElement) object;
        if (getEquivalenceClass() != that.getEquivalenceClass())
            return false;
        if (Arrays.equals(this.getColors(), that.getColors()))
            return true;
        return super.equals(that);
    }

    /**
     * Returns the set of colors for this category. Change to the returned array will not affect this category.
     * 
     * @see GridSampleDimension#getColorModel
     * @uml.property name="colors"
     */
    public Color[] getColors() {
        return (Color[]) colors.clone();
    }

    /**
     * Gives access to the internal {@link MathTransform1D}.
     * 
     * @return the internal {@link MathTransform1D}.
     */
    MathTransformation accessTransform() {
        return getTransform();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.geotools.referencing.piecewise.DefaultLinearPiecewiseTransform1DElement#toString()
     */
    public String toString() {
        final StringBuilder buffer = new StringBuilder(super.toString());
        buffer.append("\n").append("colors=");
        for (int i = 0; (colors != null) && i < colors.length; i++) {
            buffer.append(colors[i]);
            if (i + 1 < colors.length)
                buffer.append(",");
        }

        return buffer.toString();
    }

    protected Class<?> getEquivalenceClass() {
        return LinearColorMapElement.class;
    }

    @Override
    public int hashCode() {
        if (hashCode >= 0)
            return hashCode;
        hashCode = 37;
        hashCode = PiecewiseUtilities.hash(colors, hashCode);
        hashCode = PiecewiseUtilities.hash(super.hashCode(), hashCode);
        return hashCode;
    }

}
