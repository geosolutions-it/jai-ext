package it.geosolutions.jaiext.format;

import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

import javax.media.jai.RenderedOp;

import org.junit.Test;
import static org.junit.Assert.*;

import it.geosolutions.jaiext.testclasses.TestBase;

/**
 * Simple test class ensuring that the {@link FormatDescriptor} and {@link FormatCRIF} classes behaves correcly.
 * 
 * @author Nicola Lagomarsini
 */
public class FormatTest extends TestBase {

    @Test
    public void testImages() {
        // Create test image
        RenderedImage image = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                13, false);
        // Convert the image to all the dataTypes and ensure that the conversion is correct
        for (int i = 0; i < 6; i++) {
            testFormat(image, i);
        }

    }

    /**
     * Simple method for checking if an image has been converted to the new format
     * 
     * @param image
     * @param dataType
     */
    private void testFormat(RenderedImage image, int dataType) {
        RenderedOp test = FormatDescriptor.create(image, dataType, null);
        assertEquals(test.getSampleModel().getDataType(), dataType);
    }
}
