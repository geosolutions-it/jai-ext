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
 * The {@link DefaultPassthroughPiecewiseTransform1DElement} identically maps input values to the output ones.
 * 
 * Such DomainElement1D can be used in cases when only No-Data have been specified, allowing us to create a convenience domain element for the other
 * values.
 * 
 * @author Simone Giannecchini, GeoSolutions
 * @author Alessio Fabiani, GeoSolutions
 */
public class DefaultPassthroughPiecewiseTransform1DElement extends
        DefaultPiecewiseTransform1DElement implements PiecewiseTransform1DElement {

    /**
     * A generated Serial Version UID.
     */
    private static final long serialVersionUID = -2420723761115130075L;

    /**
     * Protected constructor for {@link DomainElement1D}s that want to build their transform later on.
     * 
     * @param name for this {@link DomainElement1D}.
     * @throws IllegalArgumentException
     */
    public DefaultPassthroughPiecewiseTransform1DElement(CharSequence name)
            throws IllegalArgumentException {
        super(name, RangeFactory.create(Double.NEGATIVE_INFINITY, true, Double.POSITIVE_INFINITY,
                true, true));
    }

    /**
     * Protected constructor for {@link DomainElement1D}s that want to build their transform later on.
     * 
     * @param name for this {@link DomainElement1D}.
     * @param valueRange for this {@link DomainElement1D}.
     * @throws IllegalArgumentException
     */
    public DefaultPassthroughPiecewiseTransform1DElement(CharSequence name, final Range valueRange)
            throws IllegalArgumentException {
        super(name, valueRange);
    }

    /**
     * Transforms the specified value.
     * 
     * @param value The value to transform.
     * @return the transformed value.
     * @throws TransformationException if the value can't be transformed.
     */
    public double transform(double value) throws TransformationException {
        if (checkContainment(value))
            return value;
        throw new IllegalArgumentException("Wrong value:" + value);
    }

    private boolean checkContainment(double value) throws TransformationException {
        return contains(value);

    }

    /**
     * Transforms the specified {@code ptSrc} and stores the result in {@code ptDst}.
     */
    public Position transform(final Position ptSrc, Position ptDst) throws TransformationException {
        PiecewiseUtilities.ensureNonNull("DirectPosition", ptSrc);
        if (ptDst == null) {
            ptDst = new Position();
        }
        final double value = ptSrc.getOrdinatePosition();
        checkContainment(value);
        ptDst.setOrdinatePosition(transform(value));
        return ptDst;
    }

    /*
     * (non-Javadoc)
     * 
     * @see DefaultPiecewiseTransform1DElement#getSourceDimensions()
     */
    public int getSourceDimensions() {
        return 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see DefaultPiecewiseTransform1DElement#getTargetDimensions()
     */
    public int getTargetDimensions() {
        return 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see DefaultPiecewiseTransform1DElement#inverse()
     */
    public MathTransformation inverse() throws NoninvertibleTransformException {
        return SingleDimensionTransformation.IDENTITY;
    }

    /*
     * (non-Javadoc)
     * 
     * @see DefaultPiecewiseTransform1DElement#isIdentity()
     */
    public boolean isIdentity() {
        return true;

    }

}
