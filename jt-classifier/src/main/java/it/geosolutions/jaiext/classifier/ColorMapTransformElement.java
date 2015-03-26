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

import it.geosolutions.jaiext.piecewise.PiecewiseTransform1DElement;

import java.awt.Color;

/**
 * {@link ColorMapTransformElement}s are a special type of {@link PiecewiseTransform1DElement}s that can be used to generate specific renderings as
 * the result of specific transformations applied to the input values.
 * 
 * <p>
 * A popular example is represented by a {@link DomainElement1D} used to classify values which means applying a color to all the pixels of an image
 * whose value falls in the {@link DomainElement1D}'s range.
 * 
 * @author Simone Giannecchini, GeoSolutions
 * 
 * 
 * @source $URL$
 */
public interface ColorMapTransformElement extends PiecewiseTransform1DElement {
    /**
     * Returns the set of colors for this category. Change to the returned array will not affect this category.
     * 
     * @see GridSampleDimension#getColorModel
     */
    public Color[] getColors();
}
