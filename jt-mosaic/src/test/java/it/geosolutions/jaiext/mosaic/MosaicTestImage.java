package it.geosolutions.jaiext.mosaic;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.operator.NullDescriptor;
import javax.media.jai.operator.TranslateDescriptor;
import org.geotools.renderedimage.viewer.RenderedImageBrowser;
import org.junit.Test;


/**
 * Simple class for displaying a mosaic of 2 images with the first image is translated by half of his dimension. The mosaic operation used can be the
 * old or the new version. The purpose of this class is to help the reader to understand the behavior of the Mosaic operation. For displaying the
 * mosaic, the RenderedImageBrowser clas is used. There are no ROIs, Alpha channels or No Data. For selecting the new MosaicDescriptor, the
 * JAI.Ext.NewDescriptor boolean must be set to true, fale for the old descriptor. For printing the result to the screen the JAI.Ext.Interactive
 * parameter must be set to true.
 * 
 */
public class MosaicTestImage {

    private static final boolean INTERACTIVE = Boolean.getBoolean("JAI.Ext.Interactive");

    private static final boolean NEW_DESCRIPTOR = Boolean.getBoolean("JAI.Ext.NewDescriptor");

    public static RenderedImage getSyntheticByte(byte value) {
        final float width = 256;
        final float height = 256;
        ParameterBlock pb = new ParameterBlock();
        Byte[] array = new Byte[] { value };
        pb.add(width);
        pb.add(height);
        pb.add(array);
        // Create the constant operation.
        return JAI.create("constant", pb);

    }

    @Test
    public void testOldMosaicOperation() {
        if (NEW_DESCRIPTOR) {
            testSimpleMosaicOperation(NEW_DESCRIPTOR);
        }
    }

    @Test
    public void testNewMosaicOperation() {
        if (!NEW_DESCRIPTOR) {
            testSimpleMosaicOperation(!NEW_DESCRIPTOR);
        }
    }

    public void testSimpleMosaicOperation(boolean newDescriptor) {
        // image creation
        RenderedImage image1 = getSyntheticByte((byte) 99);
        RenderedImage image2 = getSyntheticByte((byte) 15);
        int width = image1.getWidth();
        // layout creation (same height of the source images, doubled width)
        ImageLayout layout = new ImageLayout(0, 0, image1.getWidth() + image2.getWidth(),
                image1.getHeight());

        RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);
        // background values and threshold
        double[] background = { 0 };
        double[][] threshold = { { 0 }, { 0 } };
        // translation of the first image
        RenderedImage image3 = TranslateDescriptor.create(image1, width * 0.1F, 0F, null, hints);
        // No op for the second image
        RenderedImage image4 = NullDescriptor.create(image2, hints);
        // array creation
        RenderedImage[] sources = { image4, image3 };
        RenderedImage image5;
        if (newDescriptor) {
            ImageMosaicBean[] beanArray = new ImageMosaicBean[2];
            ImageMosaicBean bean1 = new ImageMosaicBean();
            bean1.setImage(image4);
            ImageMosaicBean bean2 = new ImageMosaicBean();
            bean2.setImage(image3);

            beanArray[0] = bean1;
            beanArray[1] = bean2;

            image5 = MosaicDescriptor.create(sources, beanArray,
                    javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY, background,
                    hints);
        } else {
            image5 = javax.media.jai.operator.MosaicDescriptor.create(sources,
                    javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null,
                    threshold, background, hints);
        }
        // Operations for showing the mosaic image
        if (INTERACTIVE) {
            RenderedImageBrowser.showChain(image5, false, false);
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
