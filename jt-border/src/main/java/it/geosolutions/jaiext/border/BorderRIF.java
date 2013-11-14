package it.geosolutions.jaiext.border;

import it.geosolutions.jaiext.range.Range;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import com.sun.media.jai.opimage.RIFUtil;

/**
 * A <code>RIF</code> supporting the "border" operation.
 * 
 */
public class BorderRIF implements RenderedImageFactory {

    /** Constructor. */
    public BorderRIF() {
    }

    /**
     * Creates a new instance of <code>BorderOpImage</code> in the rendered layer.
     * 
     * @param args The source image and the border information
     * @param hints Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock pb, RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);
        // Selection of the source
        RenderedImage source = pb.getRenderedSource(0);
        // Selection of the parameters
        int leftPad = pb.getIntParameter(0);
        int rightPad = pb.getIntParameter(1);
        int topPad = pb.getIntParameter(2);
        int bottomPad = pb.getIntParameter(3);
        BorderExtender type = (BorderExtender) pb.getObjectParameter(4);
        Range noData = (Range) pb.getObjectParameter(5);
        double destinationNoData = pb.getDoubleParameter(6);
        // Creation of the BorderOpImage instance
        return new BorderOpImage(source, renderHints, layout, leftPad, rightPad, topPad, bottomPad,
                type, noData, destinationNoData);

    }
}
