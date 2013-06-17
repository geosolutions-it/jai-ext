package it.geosolutions.test;

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
    RenderedImage image1 = getSyntheticDouble((double) 9999);
    RenderedImage image2 = getSyntheticDouble((double) 1500);
    int height = image1.getHeight();
    int width = image1.getWidth();
    // ImageLayout layout = new ImageLayout();
    // layout.setHeight((int) height);
    // layout.setWidth((int) (width * (1.5F)));
    ImageLayout layout = new ImageLayout(0, 0, image1.getWidth()
            + image2.getWidth(), image1.getHeight());
    // layout.setTileHeight((int) height / 16);
    // layout.setTileWidth((int) (width * (1.5F)) / 16);
    RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);
    RenderingHints renderingHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT,
            layout);
    double[] background = { 0 };
    double[][] threshold = { { 1000 }, { 1000 } };

    RenderedImage image3 = TranslateDescriptor.create(image1, width * 0.1F, 0F,
            null, hints);
    RenderedImage image4 = NullDescriptor.create(image2, hints);

    RenderedImage[] sources = { image4, image3 };
    RenderedImage image5 = MosaicDescriptor.create(sources,
            MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null, threshold,
            background, hints);

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
