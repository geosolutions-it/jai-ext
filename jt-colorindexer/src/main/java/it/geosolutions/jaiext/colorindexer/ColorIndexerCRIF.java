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
package it.geosolutions.jaiext.colorindexer;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ContextualRenderedImageFactory;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.CRIFImpl;
import javax.media.jai.ROI;

/**
 * {@link ContextualRenderedImageFactory} used for creating a new {@link ColorIndexerOpImage} instance
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * 
 * @source $URL$
 */
public class ColorIndexerCRIF extends CRIFImpl {

    public RenderedImage create(ParameterBlock pb, RenderingHints hints) {
        // Getting Source
        final RenderedImage image = (RenderedImage) pb.getSource(0);
        // Getting parameters
        final ColorIndexer indeder = (ColorIndexer) pb.getObjectParameter(0);
        final ROI roi = (ROI) pb.getObjectParameter(1);
        final Range nodata = (Range) pb.getObjectParameter(2);
        final int destNoData = pb.getIntParameter(3);
        // Creating a new OpImage
        return new ColorIndexerOpImage(image, indeder, roi, nodata, destNoData, hints);
    }

}
