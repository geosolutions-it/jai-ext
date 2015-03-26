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
 * This interface extends the {@link DomainElement1D} interface in order to add the capabilities to perform 1D transformations on its values. Note
 * that to do so it also extends the OGC {@link MathTransformation} interface.
 * 
 * 
 * @author Simone Giannecchini, GeoSolutions
 * @see MathTransformation
 * @see DomainElement1D
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
