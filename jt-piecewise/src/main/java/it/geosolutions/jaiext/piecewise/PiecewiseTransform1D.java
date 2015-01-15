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


import javax.xml.crypto.dsig.TransformException;


/**
 * The {@link PiecewiseTransform1D} interface extends the {@link Domain1D} adding transformation capabilities to it.
 * 
 * @author Simone Giannecchini, GeoSolutions.
 * 
 * 
 * 
 * 
 * @source $URL$
 */
public interface PiecewiseTransform1D<T extends PiecewiseTransform1DElement> extends Domain1D<T> {

    /**
     * Transforms the specified value.
     * 
     * @param value The value to transform.
     * @return the transformed value.
     * @throws TransformationException if the value can't be transformed.
     */
    double transform(double value) throws TransformationException;

    /**
     * Indicates whether or not this {@link PiecewiseTransform1D} has a default value which will be returned when asked to transform a value outside
     * the valid domain elements.
     * 
     * @return a <code>boolean</code> to indicate whether or not this {@link PiecewiseTransform1D} has a default value.
     */
    public boolean hasDefaultValue();

    /**
     * The default value which will be returned when asked to transform a value outside the valid domain elements.
     * 
     * <p>
     * In case {@link #hasDefaultValue()} return <code>false</code> this value has no meaning.
     * 
     * @return The default value which will be returned when asked to transform a value outside the valid domain elements.
     */
    public double getDefaultValue();
    
    /**
     * Transforms the specified {@code ptSrc} and stores the result in
     * {@code ptDst}. If {@code ptDst} is {@code null}, a new
     * {@link DirectPosition} object is allocated and then the result of the
     * transformation is stored in this object. In either case, {@code ptDst},
     * which contains the transformed point, is returned for convenience.
     * If {@code ptSrc} and {@code ptDst} are the same object,
     * the input point is correctly overwritten with the transformed point.
     *
     * @param  ptSrc the specified coordinate point to be transformed.
     * @param  ptDst the specified coordinate point that stores the result of transforming
     *         {@code ptSrc}, or {@code null}.
     * @return the coordinate point after transforming {@code ptSrc} and storing the result
     *         in {@code ptDst}, or a newly created point if {@code ptDst} was null.
     * @throws MismatchedDimensionException if {@code ptSrc} or
     *         {@code ptDst} doesn't have the expected dimension.
     * @throws TransformException if the point can't be transformed.
     */
    DirectPosition transform(DirectPosition ptSrc, DirectPosition ptDst)
            throws MismatchedDimensionException, TransformationException;
}
