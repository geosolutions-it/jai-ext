package it.geosolutions.jaiext.convolve;

import it.geosolutions.jaiext.range.Range;

import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.KernelJAI;
import javax.media.jai.ROI;

//import com.sun.media.jai.opimage.Convolve3x3OpImage;
//import com.sun.media.jai.opimage.ConvolveOpImage;
import com.sun.media.jai.opimage.RIFUtil;
//import com.sun.media.jai.opimage.SeparableConvolveOpImage;

public class ConvolveRIF implements RenderedImageFactory {
    
    public ConvolveRIF() {
    }

    public RenderedImage create(ParameterBlock pb, RenderingHints hints) {
        // Getting the Layout
        ImageLayout l = RIFUtil.getImageLayoutHint(hints);
        // Get BorderExtender from renderHints if present.
        BorderExtender extender = RIFUtil.getBorderExtenderHint(hints);
        // Getting source
        RenderedImage img = pb.getRenderedSource(0);
        // Getting parameters
        KernelJAI kernel = (KernelJAI) pb.getObjectParameter(0);
        ROI roi = (ROI) pb.getObjectParameter(1);
        Range nodata = (Range)pb.getObjectParameter(2);
        double destinationNoData = pb.getDoubleParameter(3);

        kernel = kernel.getRotatedKernel();

        int dataType = 
           pb.getRenderedSource(0).getSampleModel().getDataType();
        boolean dataTypeOk = (dataType == DataBuffer.TYPE_BYTE ||
                              dataType == DataBuffer.TYPE_SHORT ||
                              dataType == DataBuffer.TYPE_INT);
        return new ConvolveGeneralOpImage(img, extender, hints, l, kernel, roi, nodata, destinationNoData);
//        if (kernel.getWidth() == 3 && kernel.getHeight() == 3 &&
//            kernel.getXOrigin() == 1 && kernel.getYOrigin() == 1 &&
//            dataTypeOk) {
//            return new Convolve3x3OpImage(img,
//                                          extender,
//                                          hints,
//                                          l,
//                                          kernel);
//        } else if (kernel.isSeparable()) {
//           return new SeparableConvolveOpImage(img,
//                                               extender,
//                                               hints,
//                                               l,
//                                               kernel);
//
//        } else {
//            return new ConvolveOpImage(img,
//                                       extender,
//                                       hints,
//                                       l,
//                                       kernel);
//        }
    }

}
