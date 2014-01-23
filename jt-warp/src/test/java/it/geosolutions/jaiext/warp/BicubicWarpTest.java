package it.geosolutions.jaiext.warp;

import java.awt.geom.AffineTransform;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

import javax.media.jai.WarpAffine;

import org.junit.BeforeClass;

public class BicubicWarpTest extends TestWarp{

    @BeforeClass
    public static void setup() {
        // Definition of the Warp Object
        AffineTransform transform = AffineTransform.getRotateInstance(Math
                .toRadians(ANGLE_ROTATION));
        transform.concatenate(AffineTransform.getTranslateInstance(0, -DEFAULT_HEIGHT));
        warpObj = new WarpAffine(transform);

        // Definition of the input data types

        noDataValueB = 55;
        noDataValueU = 55;
        noDataValueS = 55;
        noDataValueI = 55;
        noDataValueF = 55;
        noDataValueD = 55;

        // Array creation
        images = new RenderedImage[NUM_IMAGES];
        // Setting of the imageFiller parameter to true, storing inside a variable its initial value
        boolean imageToFill = IMAGE_FILLER;
        IMAGE_FILLER = true;
        // Creation of the images
        images[0] = createTestImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataValueB, false);
        images[1] = createTestImage(DataBuffer.TYPE_USHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataValueU, false);
        images[2] = createTestImage(DataBuffer.TYPE_SHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataValueS, false);
        images[3] = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataValueI, false);
        images[4] = createTestImage(DataBuffer.TYPE_FLOAT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataValueF, false);
        images[5] = createTestImage(DataBuffer.TYPE_DOUBLE, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataValueD, false);
        // Setting of the image filler to its initial value
        IMAGE_FILLER = imageToFill;
        // Interpolation type
        interpType = InterpolationType.BICUBIC_INTERP;

    }
}
