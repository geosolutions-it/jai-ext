package it.geosolutions.jaiext.jiffleop;

import com.sun.media.jai.operator.ImageReadDescriptor;

import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;

import it.geosolutions.jaiext.jiffle.JiffleException;
import it.geosolutions.jaiext.utilities.ImageUtilities;

public class SentinelNDVI {

    public static void main(String[] args) throws IOException, JiffleException {
        String basePath =
                "/opt/gisData/evo-odas/coverave-view-hetero/multires-s2/S2A_MSIL1C_20170410T103021_N0204_R108_T32UNU_20170410T103020.SAFE/20170410T103021026Z_fullres_CC2.4251_T32UNU_";
        

        JAI jai = JAI.getDefaultInstance();
        jai.getTileCache().setMemoryCapacity(1024l * 1024 * 1024);

        // prepare inputs and outputs
        RenderedImage red = readImage(new File(basePath + "B04.tif"));
        RenderedImage nir = readImage(new File(basePath + "B08.tif"));

        ParameterBlockJAI pb = new ParameterBlockJAI("Jiffle");
        String script = "n = nir; r = red; res = (n - r) / (n + r);"; // HERE IS THE NDVI SCRIPT! 5.5 seconds!
        // String script = "res = (nir - red) / (nir + red);";
        pb.setParameter("script", script);
        pb.setParameter("destName", "res");
        pb.setParameter("sourceNames", new String[] {"red", "nir"});
        pb.setParameter("destType", DataBuffer.TYPE_FLOAT);
        pb.addSource(red);
        pb.addSource(nir);
        RenderedOp op = JAI.create("Jiffle", pb);
        
        // actually running the calculation
        final double pixels = (double) red.getWidth() * (double) red.getHeight();
        System.out.println(
                "Computing " + NumberFormat.getNumberInstance().format(pixels) + " pixels");

        for (int i = 0; i < 10; i++) {
            jai.getTileCache().flush();
            long start = System.currentTimeMillis();
            ImageIO.write(op, "TIF", new File("/tmp/ndvi.tif"));
            long end = System.currentTimeMillis();
            System.out.println("Computing and writing took " + (end - start) / 1000.0 + " secs"); 
        }
    }

    private static RenderedOp readImage(File file) throws IOException {
        FileImageInputStream stream = new FileImageInputStream(file);
        ImageReader reader = ImageIO.getImageReaders(stream).next();
        return ImageReadDescriptor.create(
                stream, 0, false, false, false, null, null, null, reader, null);
    }
}
