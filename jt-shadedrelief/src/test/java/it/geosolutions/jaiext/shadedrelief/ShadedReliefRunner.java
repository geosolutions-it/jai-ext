package it.geosolutions.jaiext.shadedrelief;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.jai.RenderedOp;

import it.geosolutions.jaiext.JAIExt;

public class ShadedReliefRunner {

    public static void main(String[] args) throws IOException {
        JAIExt.initJAIEXT(true, true);
        BufferedImage image =
                ImageIO.read(
                        new File(
                                "/home/aaime/devel/gs_localdata/training-workshop/data/boulder/srtm_boulder.tiff"));
        ShadedReliefAlgorithm algorithm = ShadedReliefAlgorithm.DEFAULT;
        RenderedOp shaded =
                ShadedReliefDescriptor.create(
                        image, null, -32768d, Double.NaN, 1, 1, 10000, 111120, 64, 126, algorithm, null);
        ImageIO.write(shaded, "tif", new File("/tmp/srtm_boulder_shaded.tif"));
    }

}
