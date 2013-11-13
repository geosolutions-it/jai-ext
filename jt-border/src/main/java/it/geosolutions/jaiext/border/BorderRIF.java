/*
 * $RCSfile: BorderRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:16 $
 * $State: Exp $
 */
package it.geosolutions.jaiext.border;
import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import java.util.Map;
import javax.media.jai.operator.BorderDescriptor;

import com.sun.media.jai.opimage.PatternOpImage;
import com.sun.media.jai.opimage.RIFUtil;

/**
 * A <code>RIF</code> supporting the "border" operation in the
 * rendered image layer.
 *
 * @see java.awt.image.renderable.RenderedImageFactory
 * @see javax.media.jai.operator.BorderDescriptor
 * @see BorderOpImage
 *
 */
public class BorderRIF implements RenderedImageFactory {

    /** Constructor. */
    public BorderRIF() {}

    /**
     * Creates a new instance of <code>BorderOpImage</code>
     * in the rendered layer.
     *
     * @param args   The source image and the border information
     * @param hints  Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock pb,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        

        RenderedImage source = pb.getRenderedSource(0);
        int leftPad = pb.getIntParameter(0);
        int rightPad = pb.getIntParameter(1);
        int topPad = pb.getIntParameter(2);
        int bottomPad = pb.getIntParameter(3);
        BorderExtender type =
            (BorderExtender)pb.getObjectParameter(4);
        Range noData = (Range) pb.getObjectParameter(5);
        double destinationNoData = pb.getDoubleParameter(6);
        
        if (type ==
                BorderExtender.createInstance(BorderExtender.BORDER_WRAP)) {
                int minX = source.getMinX() - leftPad;
                int minY = source.getMinY() - topPad;
                int width = source.getWidth() + leftPad + rightPad;
                int height = source.getHeight() + topPad + bottomPad;

                return new PatternOpImage(source.getData(),
                                          source.getColorModel(),
                                          minX, minY,
                                          width, height);
            } else {
                return new BorderOpImage(source, renderHints, layout,
                        leftPad, rightPad, topPad, bottomPad,
                        type, noData, destinationNoData); 
            }
        

        
    }
}
