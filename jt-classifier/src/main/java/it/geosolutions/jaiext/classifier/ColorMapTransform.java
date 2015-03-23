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

import it.geosolutions.jaiext.piecewise.PiecewiseTransform1D;

import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.SampleModel;

/**
 * A {@link ColorMapTransform} is a special sub-interface of {@link PiecewiseTransform1D} that can be used to render raw data.
 * 
 * @author Simone Giannecchini, GeoSolutions.
 * 
 * @source $URL$
 */
public interface ColorMapTransform<T extends ColorMapTransformElement> extends
        PiecewiseTransform1D<T> {
    /**
     * Retrieve the {@link ColorModel} associated to this {@link ColorMapTransform}.
     * 
     * @return
     */
    public IndexColorModel getColorModel();

    /**
     * Retrieve the {@link SampleModel} associated to this {@link ColorMapTransform}.
     * 
     * @return
     */
    public SampleModel getSampleModel(final int width, final int height);

}
