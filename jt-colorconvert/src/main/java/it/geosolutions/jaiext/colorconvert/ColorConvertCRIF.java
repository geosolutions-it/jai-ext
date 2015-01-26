package it.geosolutions.jaiext.colorconvert;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;
import javax.media.jai.ROI;

import com.sun.media.jai.opimage.RIFUtil;

public class ColorConvertCRIF extends CRIFImpl {

    /** Constructor. */
    public ColorConvertCRIF() {
        super("colorconvert");
    }

    /**
     * Creates a new instance of <code>ColorConvertOpImage</code> in the
     * rendered layer.
     *
     * @param args   The source image and the destination ColorModel.
     * @param hints  Optionally contains destination image layout.
     */
    public RenderedImage create(ParameterBlock pb,
                                RenderingHints renderingHints){
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderingHints);      
        // Getting source
        RenderedImage source = (RenderedImage) pb.getSource(0);
        
        // Getting Parameters
        ColorModel cm = (ColorModel) pb.getObjectParameter(0);
        ROI roi = (ROI) pb.getObjectParameter(1);
        Range nodata = (Range) pb.getObjectParameter(2);
        double[] destNoData = (double[]) pb.getObjectParameter(3);
        

        return null;
    }

}
