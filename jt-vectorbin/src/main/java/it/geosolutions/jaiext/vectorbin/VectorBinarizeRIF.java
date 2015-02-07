/* 
 *  Copyright (c) 2010-2011, Michael Bedward. All rights reserved. 
 *   
 *  Redistribution and use in source and binary forms, with or without modification, 
 *  are permitted provided that the following conditions are met: 
 *   
 *  - Redistributions of source code must retain the above copyright notice, this  
 *    list of conditions and the following disclaimer. 
 *   
 *  - Redistributions in binary form must reproduce the above copyright notice, this 
 *    list of conditions and the following disclaimer in the documentation and/or 
 *    other materials provided with the distribution.   
 *   
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR 
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygonal;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;

/**
 * The image factory for the VectorBinarize operator.
 * 
 * @author Michael Bedward
 * @author Andrea Aime
 * @since 1.1
 * @version $Id$
 */
public class VectorBinarizeRIF implements RenderedImageFactory {

    /**
     * Creates a new instance of VectorBinarizeOpImage in the rendered layer.
     *
     * @param paramBlock parameter block with parameters minx, miny, width
     *        height, geometry and coordtype
     *
     * @param renderHints optional rendering hints which may be used to pass an {@code ImageLayout}
     *        object containing a {@code SampleModel} to use when creating destination
     *        tiles
     */
    public RenderedImage create(ParameterBlock paramBlock,
            RenderingHints renderHints) {
        
        int minx = paramBlock.getIntParameter(VectorBinarizeDescriptor.MINX_ARG);
        int miny = paramBlock.getIntParameter(VectorBinarizeDescriptor.MINY_ARG);
        int width = paramBlock.getIntParameter(VectorBinarizeDescriptor.WIDTH_ARG);
        int height = paramBlock.getIntParameter(VectorBinarizeDescriptor.HEIGHT_ARG);
        
        Object obj = paramBlock.getObjectParameter(VectorBinarizeDescriptor.GEOM_ARG);
        PreparedGeometry pg = null;
        
        if (obj instanceof Polygonal) {
            // defensively copy the input Geometry
            Geometry g = (Geometry) ((Geometry)obj).clone();
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
        SampleModel sm = new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE, tileSize.width, tileSize.height, 1);
        
        boolean antiAliasing = VectorBinarizeOpImage.DEFAULT_ANTIALIASING; 
        Object antiAlias = paramBlock.getObjectParameter(VectorBinarizeDescriptor.ANTIALIASING_ARG);
        
        if (antiAlias != null && antiAlias instanceof Boolean){
            antiAliasing = ((Boolean) antiAlias).booleanValue();
        }

        
        return new VectorBinarizeOpImage(sm, renderHints, minx, miny, width, height, pg, antiAliasing);
    }
}
