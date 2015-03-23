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

/**
 * The {@link PiecewiseTransform1D} interface extends the {@link Domain1D} adding transformation capabilities to it.
 * 
 * @author Simone Giannecchini, GeoSolutions.
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
}
