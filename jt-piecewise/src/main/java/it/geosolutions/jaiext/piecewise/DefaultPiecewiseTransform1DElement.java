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
package it.geosolutions.jaiext.piecewise;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;

/**
 * Convenience implementation of the {@link DefaultPiecewiseTransform1DElement} .
 * 
 * @author Simone Giannecchini, GeoSolutions
 * 
 * @source $URL$
 */
public class DefaultPiecewiseTransform1DElement extends DefaultDomainElement1D implements
        PiecewiseTransform1DElement {

    /**
     * UID
     */
    private static final long serialVersionUID = 7422178060824402864L;

    /**
     * The math transform
     * 
     * @uml.property name="transform"
     */
    private MathTransformation transform;

    /**
     * Inverse {@link MathTransformation}
     */
    private MathTransformation inverse;

    private int hashCode = -1;

    /**
     * Builds up a {@link DefaultPiecewiseTransform1DElement} which maps a range to a constant value.
     * 
     * @param name for this {@link DomainElement1D}
     * @param inRange for this {@link DomainElement1D}
     * @param outVal for this {@link DefaultLinearPiecewiseTransform1DElement}
     * @throws IllegalArgumentException in case the input values are illegal.
     */
    public static DefaultPiecewiseTransform1DElement create(final CharSequence name,
            final Range inRange, final double value) {
        return new DefaultConstantPiecewiseTransformElement(name, inRange, value);
    }

    /**
     * Builds up a DefaultPiecewiseTransform1DElement which maps a range to a constant value.
     * 
     * @param name for this {@link DomainElement1D}
     * @param inRange for this {@link DomainElement1D}
     * @param outVal for this {@link DefaultLinearPiecewiseTransform1DElement}
     * @throws IllegalArgumentException in case the input values are illegal.
     */
    public static DefaultPiecewiseTransform1DElement create(final CharSequence name,
            final Range inRange, final byte value) {
        return new DefaultConstantPiecewiseTransformElement(name, inRange, value);
    }

    /**
     * Builds up a DefaultPiecewiseTransform1DElement which maps a range to a constant value.
     * 
     * @param name for this {@link DomainElement1D}
     * @param inRange for this {@link DomainElement1D}
     * @param outVal for this {@link DefaultLinearPiecewiseTransform1DElement}
     * @throws IllegalArgumentException in case the input values are illegal.
     */
    public static DefaultPiecewiseTransform1DElement create(final CharSequence name,
            final Range inRange, final int value) {
        return new DefaultConstantPiecewiseTransformElement(name, inRange, value);
    }

    /**
     * Constructor.
     * 
     * @param name for this {@link DefaultLinearPiecewiseTransform1DElement}.
     * @param inRange for this {@link DefaultLinearPiecewiseTransform1DElement}.
     * @param outRange for this {@link DefaultLinearPiecewiseTransform1DElement}.
     */
    public static DefaultPiecewiseTransform1DElement create(final CharSequence name,
            final Range inRange, final Range outRange) {
        return new DefaultLinearPiecewiseTransform1DElement(name, inRange, outRange);
    }

    /**
     * Creates a pass-through DefaultPiecewiseTransform1DElement.
     * 
     * @param name for this {@link DomainElement1D}.
     * @throws IllegalArgumentException
     */
    public static DefaultPiecewiseTransform1DElement create(final CharSequence name)
            throws IllegalArgumentException {
        return new DefaultPassthroughPiecewiseTransform1DElement(name, RangeFactory.create(
                Double.NEGATIVE_INFINITY, true, Double.POSITIVE_INFINITY, true, true));
    }

    /**
     * Creates a pass-through DefaultPiecewiseTransform1DElement.
     * 
     * @param name for this {@link DomainElement1D}.
     * @param valueRange for this {@link DomainElement1D}.
     * @throws IllegalArgumentException
     */
    public static DefaultPiecewiseTransform1DElement create(final CharSequence name,
            final Range valueRange) throws IllegalArgumentException {
        return new DefaultPassthroughPiecewiseTransform1DElement(name, valueRange);
    }

    /**
     * Protected constructor for {@link DomainElement1D}s that want to build their transform later on.
     * 
     * @param name for this {@link DomainElement1D}.
     * @param valueRange for this {@link DomainElement1D}.
     * @throws IllegalArgumentException
     */
    public DefaultPiecewiseTransform1DElement(CharSequence name, Range valueRange)
            throws IllegalArgumentException {
        super(name, valueRange);
    }

    /**
     * Public constructor for building a {@link DomainElement1D} which applies the specified transformation on the values that fall into its
     * definition range.
     * 
     * @param name for this {@link DomainElement1D}.
     * @param valueRange for this {@link DomainElement1D}.
     * @param transform for this {@link DomainElement1D}.
     * @throws IllegalArgumentException
     */
    public DefaultPiecewiseTransform1DElement(CharSequence name, Range valueRange,
            final MathTransformation transform) throws IllegalArgumentException {
        super(name, valueRange);
        // /////////////////////////////////////////////////////////////////////
        //
        // Initial checks
        //
        // /////////////////////////////////////////////////////////////////////
        PiecewiseUtilities.ensureNonNull("transform", transform);
        this.transform = transform;
    }

    /**
     * Getter for the underlying {@link MathTransformation} .
     * 
     * @return the underlying {@link MathTransformation} .
     * @uml.property name="transform"
     */
    protected synchronized MathTransformation getTransform() {
        return transform;
    }

    /**
     * Transforms the specified value.
     * 
     * @param value The value to transform.
     * @return the transformed value.
     * @throws TransformationException if the value can't be transformed.
     */
    public synchronized double transform(double value) throws TransformationException {

        if (transform == null)
            throw new IllegalStateException();

        if (contains(value))
            return transform.transform(value);

        throw new IllegalArgumentException("Provided value is not contained in this domain");
    }

    /**
     * Transforms the specified {@code ptSrc} and stores the result in {@code ptDst}.
     */
    public Position transform(final Position ptSrc, Position ptDst) throws TransformationException {
        if (ptDst == null) {
            ptDst = new Position();
        }
        ptDst.setOrdinatePosition(transform(ptSrc.getOrdinatePosition()));
        return ptDst;
    }

    /**
     * Returns the input transformation dimensions
     */
    public int getSourceDimensions() {
        return transform.getSourceDimensions();
    }

    /**
     * Returns the output transformation dimensions
     */
    public int getTargetDimensions() {
        return transform.getTargetDimensions();
    }

    /**
     * Returns the inverse of this {@link MathTransformation} instance
     */
    public synchronized MathTransformation inverse() throws NoninvertibleTransformException {
        if (inverse != null)
            return inverse;
        if (transform == null)
            throw new IllegalStateException();
        inverse = (MathTransformation) transform.inverseTransform();
        return inverse;
    }

    /**
     * Defines if the transformation is an identity
     */
    public boolean isIdentity() {
        return transform.isIdentity();

    }

    /**
     * @param mathTransform
     * @uml.property name="inverse"
     */
    protected synchronized void setInverse(MathTransformation mathTransform) {
        if (this.inverse == null)
            this.inverse = mathTransform;
        else
            throw new IllegalStateException();
    }

    /**
     * @param transform
     * @uml.property name="transform"
     */
    protected synchronized void setTransform(MathTransformation transform) {
        PiecewiseUtilities.ensureNonNull("transform", transform);
        if (this.transform == null)
            this.transform = transform;
        else
            throw new IllegalStateException();
    }

    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof DefaultPiecewiseTransform1DElement))
            return false;
        final DefaultPiecewiseTransform1DElement that = (DefaultPiecewiseTransform1DElement) obj;
        if (getEquivalenceClass() != (that.getEquivalenceClass()))
            return false;
        if (!PiecewiseUtilities.equals(transform, that.transform))
            return false;
        if (!PiecewiseUtilities.equals(inverse, that.inverse))
            return false;
        return super.equals(obj);

    }

    protected Class<?> getEquivalenceClass() {
        return DefaultPiecewiseTransform1DElement.class;
    }

    @Override
    public int hashCode() {
        if (hashCode >= 0)
            return hashCode;
        hashCode = 37;
        hashCode = PiecewiseUtilities.hash(transform, hashCode);
        hashCode = PiecewiseUtilities.hash(inverse, hashCode);
        hashCode = PiecewiseUtilities.hash(super.hashCode(), hashCode);
        return hashCode;
    }

    public static DefaultPiecewiseTransform1DElement create(String string, Range range,
            MathTransformation mathTransform1D) {
        return new DefaultPiecewiseTransform1DElement(string, range, mathTransform1D);
    }

}
