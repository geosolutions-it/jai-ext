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



/**
 * This interface extends the {@link DomainElement1D} interface in order to add 
 * the capabilities to perform 1D transformations on its values. Note that to do
 * so it also extends the OGC {@link MathTransformation} interface.
 * 
 * 
 * @author Simone Giannecchini, GeoSolutions
 * @see MathTransformation
 * @see DomainElement1D
 * 
 *
 *
 *
 * @source $URL$
 */
public interface PiecewiseTransform1DElement extends DomainElement1D {
    
    /**
     * Transforms the specified value.
     *
     * @param value The value to transform.
     * @return the transformed value.
     * @throws TransformationException if the value can't be transformed.
     */
    double transform(double value) throws TransformationException;

}
