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
package it.geosolutions.jaiext.vectorbin;

import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygonal;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

/**
 * The image factory for the VectorBinarize operator.
 * 
 * @author Michael Bedward
 * @author Andrea Aime
 */
public class VectorBinarizeRIF implements RenderedImageFactory {

    /**
     * Creates a new instance of VectorBinarizeOpImage in the rendered layer.
     * 
     * @param paramBlock parameter block with parameters minx, miny, width height, geometry and coordtype
     * 
     * @param renderHints optional rendering hints which may be used to pass an {@code ImageLayout} object containing a {@code SampleModel} to use
     *        when creating destination tiles
     */
    public RenderedImage create(ParameterBlock paramBlock, RenderingHints renderHints) {

        int minx = paramBlock.getIntParameter(VectorBinarizeDescriptor.MINX_ARG);
        int miny = paramBlock.getIntParameter(VectorBinarizeDescriptor.MINY_ARG);
        int width = paramBlock.getIntParameter(VectorBinarizeDescriptor.WIDTH_ARG);
        int height = paramBlock.getIntParameter(VectorBinarizeDescriptor.HEIGHT_ARG);

        Object obj = paramBlock.getObjectParameter(VectorBinarizeDescriptor.GEOM_ARG);
        PreparedGeometry pg = null;

        if (obj instanceof Polygonal) {
            // defensively copy the input Geometry
            Geometry g = (Geometry) ((Geometry) obj).clone();
            pg = PreparedGeometryFactory.prepare(g);

        } else if (obj instanceof PreparedGeometry) {
            pg = (PreparedGeometry) obj;
        } else {
            throw new IllegalArgumentException("The geometry must be a JTS polygon or multipolygon");
        }

        // get the tile size from the image layout
        Dimension tileSize = null;
        if (renderHints != null && renderHints.containsKey(JAI.KEY_IMAGE_LAYOUT)) {
            ImageLayout il = (ImageLayout) renderHints.get(JAI.KEY_IMAGE_LAYOUT);
            if (il != null) {
                tileSize = new Dimension(il.getTileWidth(null), il.getTileHeight(null));
            }
        }
        if (tileSize == null) {
            tileSize = JAI.getDefaultTileSize();
        }
        // sample model wise we only build bw images
        SampleModel sm = new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE, tileSize.width,
                tileSize.height, 1);

        boolean antiAliasing = VectorBinarizeOpImage.DEFAULT_ANTIALIASING;
        Object antiAlias = paramBlock.getObjectParameter(VectorBinarizeDescriptor.ANTIALIASING_ARG);

        if (antiAlias != null && antiAlias instanceof Boolean) {
            antiAliasing = ((Boolean) antiAlias).booleanValue();
        }

        return new VectorBinarizeOpImage(sm, renderHints, minx, miny, width, height, pg,
                antiAliasing);
    }
}
