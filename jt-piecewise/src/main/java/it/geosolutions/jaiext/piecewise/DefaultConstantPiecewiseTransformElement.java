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

import java.io.Serializable;

/**
 * Convenience implementation of a {@link PiecewiseTransform1DElement} that can be used to
 * map single values or an interval to a single output value.
 * 
 * @author Simone Giannecchini, GeoSolutions
 * 
 */
class DefaultConstantPiecewiseTransformElement extends DefaultLinearPiecewiseTransform1DElement implements
		PiecewiseTransform1DElement, Comparable<DomainElement1D>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6704840161747974131L;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            for this {@link DomainElement1D}
	 * @param inRange
	 *            for this {@link DomainElement1D}
	 * @param outVal
	 *            for this {@link DefaultLinearPiecewiseTransform1DElement}
	 * @throws IllegalArgumentException
	 *             in case the input values are illegal.
	 */
	DefaultConstantPiecewiseTransformElement(CharSequence name,
			final Range inRange, final double outVal)
			throws IllegalArgumentException {
		super(name, inRange, RangeFactory.create(outVal, true, outVal, true, true));

	}


	/**
	 * Constructor.
	 * 
	 * @param name
	 *            for this {@link DomainElement1D}
	 * @param inRange
	 *            for this {@link DomainElement1D}
	 * @param outVal
	 *            for this {@link DefaultLinearPiecewiseTransform1DElement}
	 * @throws IllegalArgumentException
	 *             in case the input values are illegal.
	 */
	 DefaultConstantPiecewiseTransformElement(CharSequence name,
			final Range inRange, final int outVal)
			throws IllegalArgumentException {
		super(name, inRange, RangeFactory.create(outVal, true, outVal, true));
	}


	/**
	 * Constructor.
	 * 
	 * @param name
	 *            for this {@link DomainElement1D}
	 * @param inRange
	 *            for this {@link DomainElement1D}
	 * @param outVal
	 *            for this {@link DefaultLinearPiecewiseTransform1DElement}
	 * @throws IllegalArgumentException
	 *             in case the input values are illegal.
	 */
	DefaultConstantPiecewiseTransformElement(CharSequence name,
			final Range inRange, final byte outVal)
			throws IllegalArgumentException {
		super(name, inRange, RangeFactory.create(outVal, true, outVal, true));
	}

	/**
	 * The transformation we are specifying here is not always invertible, well,
	 * to be honest, strictly speaking it never really is. However when the
	 * underlying transformation is a 1:1 mapping we can invert it.
	 */
	public MathTransformation inverse() throws NoninvertibleTransformException {
		if (this.getInputMinimum() == getInputMaximum())
			return SingleDimensionTransformation.create(0, getInputMinimum());
		throw new UnsupportedOperationException("Inverse operation is unsupported for Constant Transform");

	}

}
