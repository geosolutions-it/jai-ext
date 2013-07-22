package it.geosolutions.jaiext.roiaware.scale;

import static org.junit.Assert.assertEquals;
import it.geosolutions.jaiext.roiaware.interpolators.InterpolationBicubicNew;
import it.geosolutions.jaiext.roiaware.interpolators.InterpolationBilinearNew;
import it.geosolutions.jaiext.roiaware.interpolators.InterpolationNearestNew;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.stream.FileImageInputStream;
import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROIShape;
import org.geotools.renderedimage.viewer.RenderedImageBrowser;
import org.junit.Test;
import com.sun.media.imageioimpl.plugins.tiff.TIFFImageReader;
import com.sun.media.imageioimpl.plugins.tiff.TIFFImageReaderSpi;

public class ImageRGBTest extends TestScale {

    private int imageWidth;

    private int imageHeigth;
    
    
    @Test
    public void testInterpolation() throws Throwable {

        boolean bicubic2Disabled = true;
        boolean useROIAccessor = true;
        boolean roiUsed = true;

        TIFFImageReader reader = null;

        FileImageInputStream stream_in = null;

        try {

            // Instantiation of the file-reader
            reader = (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();

            File inputFile = new File(
                    "../utilities/src/test/resources/it/geosolutions/jaiext/roiaware/images/testImageLittle.tif");
            // Instantiation of the imageinputstream and imageoutputstrem
            stream_in = new FileImageInputStream(inputFile);

            // Setting the inputstream to the reader
            reader.setInput(stream_in);
            // Creation of a Renderedimage to store the image
            RenderedImage image = reader.readAsRenderedImage(0, null);

            imageWidth = image.getWidth();
            imageHeigth = image.getHeight();

            int dataType = image.getSampleModel().getDataType();

            testImage(image, useROIAccessor,roiUsed, bicubic2Disabled, dataType,
                    InterpolationType.NEAREST_INTERP);

            testImage(image, useROIAccessor,roiUsed, bicubic2Disabled, dataType,
                    InterpolationType.BILINEAR_INTERP);

            testImage(image, useROIAccessor,roiUsed, bicubic2Disabled, dataType,
                    InterpolationType.BICUBIC_INTERP);

        } finally {
            try {
                if (reader != null) {
                    reader.dispose();
                }
            } catch (Exception e) {
            }

            try {
                if (stream_in != null) {
                    stream_in.flush();
                    stream_in.close();
                }
            } catch (Exception e) {
            }

        }

    }

    protected ROIShape roiCreation() {
        int roiHeight = imageHeigth / 2;
        int roiWidth = imageWidth / 2;

        Rectangle roiBound = new Rectangle(0, 0, roiWidth, roiHeight);

        ROIShape roi = new ROIShape(roiBound);
        return roi;
    }

    private void testImage(RenderedImage sourceImage, boolean useROIAccessor,boolean roiUsed,
            boolean bicubic2Disabled, int dataType, InterpolationType interpType) {

        // Hints are used only with roiAccessor
        RenderingHints hints = null;
        // ROI creation
        ROIShape roi = null;
        if(roiUsed){
            if (useROIAccessor) {
                hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                        BorderExtender.createInstance(BorderExtender.BORDER_ZERO));
            }
            roi=roiCreation();
        }else{
            useROIAccessor=false;
        }



        // Interpolator initialization
        Interpolation interp = null;

        // Interpolators
        switch (interpType) {
        case NEAREST_INTERP:
            // Nearest-Neighbor
            interp = new InterpolationNearestNew(null, useROIAccessor, destinationNoData, dataType);
            break;
        case BILINEAR_INTERP:
            // Bilinear
            interp = new InterpolationBilinearNew(DEFAULT_SUBSAMPLE_BITS, null, useROIAccessor,
                    destinationNoData, dataType);
            if(hints!=null){
                hints.add(new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(BorderExtender.BORDER_COPY)));
            } else {
                hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(BorderExtender.BORDER_COPY));
            }

            break;
        case BICUBIC_INTERP:
            // Bicubic
            interp = new InterpolationBicubicNew(DEFAULT_SUBSAMPLE_BITS, null, useROIAccessor,
                    destinationNoData, dataType, bicubic2Disabled, DEFAULT_PRECISION_BITS);
            if(hints!=null){
                hints.add(new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(BorderExtender.BORDER_COPY)));
            }else {
                hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(BorderExtender.BORDER_COPY));
            }
            break;
        default:
            throw new IllegalArgumentException("..."); 
        }

        // Scale operation
        RenderedImage destinationIMG = ScaleNoDataDescriptor.create(sourceImage, scaleX, scaleY,
                transX, transY, interp, roi, useROIAccessor, hints);

        if (INTERACTIVE && TEST_SELECTOR == interpType.getType()) {
            RenderedImageBrowser.showChain(destinationIMG, false, roiUsed);
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            
            // Forcing to retrieve an array of all the image tiles
            // image tile calculation for searching possible errors
            ((PlanarImage)destinationIMG).getTiles();
        }


        // Control if the scale operation has been correctly performed
        // width
        assertEquals((int) (imageWidth  * scaleX), destinationIMG.getWidth());
        // height
        assertEquals((int) (imageHeigth * scaleY), destinationIMG.getHeight());

    }
}
