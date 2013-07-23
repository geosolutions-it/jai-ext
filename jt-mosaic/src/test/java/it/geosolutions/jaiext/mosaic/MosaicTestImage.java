package it.geosolutions.jaiext.mosaic;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.operator.MosaicDescriptor;
import javax.media.jai.operator.NullDescriptor;
import javax.media.jai.operator.TranslateDescriptor;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

import com.sun.media.jai.widget.DisplayJAI;

/**
 * Simple class for displaying a mosaic of 2 images with the first image is translated by half of his dimension. The mosaic operation used is the old
 * one. The purpose of this class is to help the reader to understand the behavior of the Mosaic operation. For displaying the mosaic, a JFrame object
 * is used. There is no ROI, Alpha channel or No Data. By changing the threshold value is possible to see the related changes in the mosaic image: the
 * result could be one of the two image pixels or the background value.
 * 
 */
public class MosaicTestImage {

    static public void main(String[] args) {
        new MosaicTestImage().testSimpleMosaicOperation();
    }

    public static RenderedImage getSyntheticDouble(double value) {
        final float width = 512;
        final float height = 512;
        ParameterBlock pb = new ParameterBlock();
        Double[] array = new Double[] { value };
        pb.add(width);
        pb.add(height);
        pb.add(array);
        // Create the constant operation.
        return JAI.create("constant", pb);

    }

    public void testSimpleMosaicOperation() {
        // image creation
        RenderedImage image1 = getSyntheticDouble((double) 9999);
        RenderedImage image2 = getSyntheticDouble((double) 1500);
        int height = image1.getHeight();
        int width = image1.getWidth();
        // layout creation (same height of the source images, doubled width)
        ImageLayout layout = new ImageLayout(0, 0, image1.getWidth() + image2.getWidth(),
                image1.getHeight());

        RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);
        // background values and threshold
        double[] background = { 0 };
        double[][] threshold = { { 1000 }, { 1000 } };
        // translation of the first image
        RenderedImage image3 = TranslateDescriptor.create(image1, width * 0.1F, 0F, null, hints);
        // No op for the second image
        RenderedImage image4 = NullDescriptor.create(image2, hints);
        // array creation
        RenderedImage[] sources = { image4, image3 };
        RenderedImage image5 = MosaicDescriptor.create(sources,
                MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null, threshold, background, hints);
        // Operations for showing the mosaic image
        JFrame frame = new JFrame();
        frame.setTitle("Image5");
        // Get the JFrame's ContentPane.
        Container contentPane = frame.getContentPane();
        contentPane.setLayout(new BorderLayout());
        // Create an instance of DisplayJAI.
        DisplayJAI dj = new DisplayJAI(image5);
        // Add to the JFrame's ContentPane an instance of JScrollPane containing the
        // DisplayJAI instance.
        contentPane.add(new JScrollPane(dj), BorderLayout.CENTER);
        // Set the closing operation so the application is finished.
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 600); // adjust the frame size.
        frame.setVisible(true);
    }

}
