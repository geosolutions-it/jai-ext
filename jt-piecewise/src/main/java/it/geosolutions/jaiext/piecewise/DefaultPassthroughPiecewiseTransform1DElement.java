/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2005-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package it.geosolutions.jaiext.piecewise;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;


/**
 * The {@link DefaultPassthroughPiecewiseTransform1DElement} identically maps
 * input values to the output ones.
 * 
 * Such DomainElement1D can be used in cases when only No-Data have been specified,
 * allowing us to create a convenience domain element for the other values.
 * 
 * @author Simone Giannecchini, GeoSolutions
 * @author Alessio Fabiani, GeoSolutions
 */
class DefaultPassthroughPiecewiseTransform1DElement extends DefaultPiecewiseTransform1DElement implements
		PiecewiseTransform1DElement {

	/**
	 * A generated Serial Version UID.
	 */
	private static final long serialVersionUID = -2420723761115130075L;

	/**
	 * Protected constructor for {@link DomainElement1D}s that want to build their
	 * transform later on.
	 * 
	 * @param name
	 *            for this {@link DomainElement1D}.
	 * @throws IllegalArgumentException
	 */
	DefaultPassthroughPiecewiseTransform1DElement(CharSequence name)
			throws IllegalArgumentException {
		super(name,  RangeFactory.create(Double.NEGATIVE_INFINITY, true,Double.POSITIVE_INFINITY, true, true));
	}

	/**
	 * Protected constructor for {@link DomainElement1D}s that want to build their
	 * transform later on.
	 * 
	 * @param name
	 *            for this {@link DomainElement1D}.
	 * @param valueRange
	 *            for this {@link DomainElement1D}.
	 * @throws IllegalArgumentException
	 */
	DefaultPassthroughPiecewiseTransform1DElement(CharSequence name, final Range valueRange)
			throws IllegalArgumentException {
		super(name,  valueRange);
	}

	/**
	 * Transforms the specified value.
	 * 
	 * @param value
	 *            The value to transform.
	 * @return the transformed value.
	 * @throws TransformationException
	 *             if the value can't be transformed.
	 */
	public double transform(double value)
			throws TransformationException {
		if(checkContainment(value))
			return value;
		throw new IllegalArgumentException("Wrong value:" + value);
	}

	private boolean checkContainment(double value)
		throws TransformationException{
		return contains(value);
		
	}

	/**
	 * Transforms the specified {@code ptSrc} and stores the result in
	 * {@code ptDst}.
	 */
	public Position transform(final Position ptSrc,
			Position ptDst) throws TransformationException {
		PiecewiseUtilities.ensureNonNull("DirectPosition", ptSrc);
		if (ptDst == null) {
			ptDst = new Position();
		}
		final double value=ptSrc.getOrdinate();
		checkContainment(value);
		ptDst.setOrdinate(transform(value));
		return ptDst;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opengis.referencing.operation.MathTransform#getSourceDimensions()
	 */
	public int getSourceDimensions() {
		return 1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opengis.referencing.operation.MathTransform#getTargetDimensions()
	 */
	public int getTargetDimensions() {
		return 1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opengis.referencing.operation.MathTransform#inverse()
	 */
	public MathTransformation inverse()
			throws NoninvertibleTransformException {
		return SingleDimensionTransformation.IDENTITY;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opengis.referencing.operation.MathTransform#isIdentity()
	 */
	public boolean isIdentity() {
		return true;

	}

}
