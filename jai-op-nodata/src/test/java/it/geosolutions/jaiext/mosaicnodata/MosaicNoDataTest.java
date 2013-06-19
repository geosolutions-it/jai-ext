package it.geosolutions.jaiext.mosaicnodata;

import static org.junit.Assert.*;
import it.geosolutions.jaiext.mosaicnodata.ImageMosaicBean;
import it.geosolutions.jaiext.mosaicnodata.MosaicNoDataDescriptor;
import it.geosolutions.jaiext.mosaicnodata.MosaicNoDataOpImage;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.io.Serializable;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROIShape;
import javax.media.jai.operator.MosaicDescriptor;
import javax.media.jai.operator.MosaicType;
import javax.media.jai.operator.TranslateDescriptor;
import org.jaitools.numeric.Range;
import org.junit.Test;

public class MosaicNoDataTest {

    // Default initialization set to false
    public final static boolean DEFAULT_SETUP_INITALIZATION = false;

    private final static MosaicType DEFAULT_MOSAIC_TYPE = MosaicDescriptor.MOSAIC_TYPE_OVERLAY;

    // Default ROI initialization
    public final static boolean DEFAULT_ROI_INITALIZATION = false;

    // Default Alpha initialization
    public final static boolean DEFAULT_ALPHA_INITALIZATION = false;

    // the initial setup is set to the default value
    private boolean initialSetup = DEFAULT_SETUP_INITALIZATION;

    // the initial ROI initialization is set to the default value
    private boolean initialROICreation = DEFAULT_ROI_INITALIZATION;

    // the initial ROI initialization is set to the default value
    private boolean initialAlphaCreation = DEFAULT_ALPHA_INITALIZATION;

    // the default image dimensions;
    public final static float DEFAULT_WIDTH = 512;

    public final static float DEFAULT_HEIGTH = 512;

    public final static Logger LOGGER = Logger.getLogger(MosaicNoDataTest.class.toString());

    // default tolerance for comparison
    public final static double DEFAULT_DELTA = 1.5d;

    // Bean container for 2 images with different no data - data combinations
    private MosaicBean[][] beanContainer;

    // Bean container for 3 images
    private MosaicBean[] beanContainer3Images;

    private RenderingHints hints;

    private boolean initialSetup3Images = DEFAULT_SETUP_INITALIZATION;

    // THIS TESTS ARE WITH 2 IMAGES: THE FIRST IS IN THE LEFT SIDE AND THE SECOND IS TRANSLATED
    // ON THE RIGHT BY HALF OF ITS WIDTH (HALF IMAGE OVERLAY)

    // Test No Data: image 1 and 2 only no data
    @Test
    public void testByteNoData1NoData2() {
        if (!initialSetup) {
            initalSetup();
        }
        //
        MosaicBean testBean = beanContainer[0][0];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecution(testBean, 1);
        //
        byte destinationNoData = testBean.getDestinationNoData()[0].byteValue();
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  destination no Data Value",
                destinationNoData, arrayPixel[0].byteValue());
        // Test for image with 3 band
        Number[] arrayPixel3Band = testExecution(testBean, 3);
        //
        byte[] destinationNoData3Band = new byte[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].byteValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].byteValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].byteValue();
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  destination no Data Value",
                destinationNoData3Band[0], arrayPixel3Band[0].byteValue());
        assertEquals("Pixel in the upper center must have the  destination no Data Value",
                destinationNoData3Band[1], arrayPixel3Band[1].byteValue());
        assertEquals("Pixel in the upper center must have the  destination no Data Value",
                destinationNoData3Band[2], arrayPixel3Band[2].byteValue());
    }

    @Test
    public void testUShortNoData1NoData2() {
        if (!initialSetup) {
            initalSetup();
        }
        //
        MosaicBean testBean = beanContainer[1][0];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecution(testBean, 1);
        //
        short destinationNoData = testBean.getDestinationNoData()[0].shortValue();
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  destination no Data Value",
                destinationNoData, arrayPixel[0].shortValue());
        // Test for image with 3 band
        Number[] arrayPixel3Band = testExecution(testBean, 3);
        //
        short[] destinationNoData3Band = new short[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].shortValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].shortValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].shortValue();
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  destination no Data Value",
                destinationNoData3Band[0], arrayPixel3Band[0].shortValue());
        assertEquals("Pixel in the upper center must have the  destination no Data Value",
                destinationNoData3Band[1], arrayPixel3Band[1].shortValue());
        assertEquals("Pixel in the upper center must have the  destination no Data Value",
                destinationNoData3Band[2], arrayPixel3Band[2].shortValue());
        //
    }

    @Test
    public void testShortNoData1NoData2() {
        if (!initialSetup) {
            initalSetup();
        }
        //
        MosaicBean testBean = beanContainer[2][0];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecution(testBean, 1);
        //
        short destinationNoData = testBean.getDestinationNoData()[0].shortValue();
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  destination no Data Value",
                destinationNoData, arrayPixel[0].shortValue());
        // Test for image with 3 band
        Number[] arrayPixel3Band = testExecution(testBean, 3);
        //
        short[] destinationNoData3Band = new short[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].shortValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].shortValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].shortValue();
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  destination no Data Value",
                destinationNoData3Band[0], arrayPixel3Band[0].shortValue());
        assertEquals("Pixel in the upper center must have the  destination no Data Value",
                destinationNoData3Band[1], arrayPixel3Band[1].shortValue());
        assertEquals("Pixel in the upper center must have the  destination no Data Value",
                destinationNoData3Band[2], arrayPixel3Band[2].shortValue());
        //
    }

    @Test
    public void testIntNoData1NoData2() {
        if (!initialSetup) {
            initalSetup();
        }
        //
        MosaicBean testBean = beanContainer[3][0];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecution(testBean, 1);
        //
        int destinationNoData = testBean.getDestinationNoData()[0].intValue();
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  destination no Data Value",
                destinationNoData, arrayPixel[0].intValue());
        // Test for image with 3 band
        Number[] arrayPixel3Band = testExecution(testBean, 3);
        //
        int[] destinationNoData3Band = new int[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].intValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].intValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].intValue();
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  destination no Data Value",
                destinationNoData3Band[0], arrayPixel3Band[0].intValue());
        assertEquals("Pixel in the upper center must have the  destination no Data Value",
                destinationNoData3Band[1], arrayPixel3Band[1].intValue());
        assertEquals("Pixel in the upper center must have the  destination no Data Value",
                destinationNoData3Band[2], arrayPixel3Band[2].intValue());
    }

    @Test
    public void testFloatNoData1NoData2() {
        if (!initialSetup) {
            initalSetup();
        }
        //
        MosaicBean testBean = beanContainer[3][0];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecution(testBean, 1);
        //
        float destinationNoData = testBean.getDestinationNoData()[0].floatValue();
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  destination no Data Value",
                destinationNoData, arrayPixel[0].floatValue(), DEFAULT_DELTA);
        // Test for image with 3 band
        Number[] arrayPixel3Band = testExecution(testBean, 3);
        //
        float[] destinationNoData3Band = new float[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].floatValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].floatValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].floatValue();
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  destination no Data Value",
                destinationNoData3Band[0], arrayPixel3Band[0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the  destination no Data Value",
                destinationNoData3Band[1], arrayPixel3Band[1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the  destination no Data Value",
                destinationNoData3Band[2], arrayPixel3Band[2].floatValue(), DEFAULT_DELTA);
        //
    }

    @Test
    public void testDoubleNoData1NoData2() {
        if (!initialSetup) {
            initalSetup();
        }
        //
        MosaicBean testBean = beanContainer[3][0];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecution(testBean, 1);
        //
        double destinationNoData = testBean.getDestinationNoData()[0].doubleValue();
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  destination no Data Value",
                destinationNoData, arrayPixel[0].doubleValue(), DEFAULT_DELTA);
        // Test for image with 3 band
        Number[] arrayPixel3Band = testExecution(testBean, 3);
        //
        double[] destinationNoData3Band = new double[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].doubleValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].doubleValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].doubleValue();
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  destination no Data Value",
                destinationNoData3Band[0], arrayPixel3Band[0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the  destination no Data Value",
                destinationNoData3Band[1], arrayPixel3Band[1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the  destination no Data Value",
                destinationNoData3Band[2], arrayPixel3Band[2].doubleValue(), DEFAULT_DELTA);
        //
    }

    // Test No Data: image 1 with data and image 2 with no data
    @Test
    public void testByteNoData1Data2() {
        //
        if (!initialSetup) {
            initalSetup();
        }
        //
        MosaicBean testBean = beanContainer[0][1];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecution(testBean, 1);
        byte valideValueSource2 = testBean.getSourceValidData()[1].byteValue();
        //
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  Source 2 value", valideValueSource2,
                arrayPixel[0].byteValue());
        // Test for image with 3 band
        Number[] arrayPixel3Band = testExecution(testBean, 3);
        //
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  Source 2 value", valideValueSource2,
                arrayPixel3Band[0].byteValue());
        assertEquals("Pixel in the upper center must have the  Source 2 value", valideValueSource2,
                arrayPixel3Band[1].byteValue());
        assertEquals("Pixel in the upper center must have the  Source 2 value", valideValueSource2,
                arrayPixel3Band[2].byteValue());
        //
    }

    @Test
    public void testUShortNoData1Data2() {
        //
        if (!initialSetup) {
            initalSetup();
        }
        //
        MosaicBean testBean = beanContainer[1][1];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecution(testBean, 1);
        short valideValueSource2 = testBean.getSourceValidData()[1].shortValue();
        //
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  Source 2 value", valideValueSource2,
                arrayPixel[0].shortValue());
        // Test for image with 3 band
        Number[] arrayPixel3Band = testExecution(testBean, 3);
        //
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  Source 2 value", valideValueSource2,
                arrayPixel3Band[0].shortValue());
        assertEquals("Pixel in the upper center must have the  Source 2 value", valideValueSource2,
                arrayPixel3Band[1].shortValue());
        assertEquals("Pixel in the upper center must have the  Source 2 value", valideValueSource2,
                arrayPixel3Band[2].shortValue());
        //
    }

    @Test
    public void testShortNoData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        //
        MosaicBean testBean = beanContainer[2][1];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecution(testBean, 1);
        short valideValueSource2 = testBean.getSourceValidData()[1].shortValue();
        //
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  Source 2 value", valideValueSource2,
                arrayPixel[0].shortValue());
        // Test for image with 3 band
        Number[] arrayPixel3Band = testExecution(testBean, 3);
        //
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  Source 2 value", valideValueSource2,
                arrayPixel3Band[0].shortValue());
        assertEquals("Pixel in the upper center must have the  Source 2 value", valideValueSource2,
                arrayPixel3Band[1].shortValue());
        assertEquals("Pixel in the upper center must have the  Source 2 value", valideValueSource2,
                arrayPixel3Band[2].shortValue());
        //
    }

    @Test
    public void testIntNoData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        //
        MosaicBean testBean = beanContainer[3][1];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecution(testBean, 1);
        int valideValueSource2 = testBean.getSourceValidData()[1].intValue();
        //
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  Source 2 value", valideValueSource2,
                arrayPixel[0].intValue());
        // Test for image with 3 band
        Number[] arrayPixel3Band = testExecution(testBean, 3);
        //
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  Source 2 value", valideValueSource2,
                arrayPixel3Band[0].intValue());
        assertEquals("Pixel in the upper center must have the  Source 2 value", valideValueSource2,
                arrayPixel3Band[1].intValue());
        assertEquals("Pixel in the upper center must have the  Source 2 value", valideValueSource2,
                arrayPixel3Band[2].intValue());
        //
    }

    @Test
    public void testFloatNoData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        //
        MosaicBean testBean = beanContainer[4][1];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecution(testBean, 1);
        float valideValueSource2 = testBean.getSourceValidData()[1].floatValue();
        //
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  Source 2 value", valideValueSource2,
                arrayPixel[0].floatValue(), DEFAULT_DELTA);
        // Test for image with 3 band
        Number[] arrayPixel3Band = testExecution(testBean, 3);
        //
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  Source 2 value", valideValueSource2,
                arrayPixel3Band[0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the  Source 2 value", valideValueSource2,
                arrayPixel3Band[1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the  Source 2 value", valideValueSource2,
                arrayPixel3Band[2].floatValue(), DEFAULT_DELTA);
        //
    }

    @Test
    public void testDoubleNoData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        //
        MosaicBean testBean = beanContainer[5][1];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecution(testBean, 1);
        double valideValueSource2 = testBean.getSourceValidData()[1].doubleValue();
        //
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  Source 2 value", valideValueSource2,
                arrayPixel[0].doubleValue(), DEFAULT_DELTA);
        // Test for image with 3 band
        Number[] arrayPixel3Band = testExecution(testBean, 3);
        //
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  Source 2 value", valideValueSource2,
                arrayPixel3Band[0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the  Source 2 value", valideValueSource2,
                arrayPixel3Band[1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the  Source 2 value", valideValueSource2,
                arrayPixel3Band[2].doubleValue(), DEFAULT_DELTA);
        //
    }

    // Test No data: image 1 and 2 with data
    @Test
    public void testByteData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        //
        MosaicBean testBean = beanContainer[0][2];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecution(testBean, 1);
        byte valideValueSource1 = testBean.getSourceValidData()[0].byteValue();
        //
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  Source 1 value", valideValueSource1,
                arrayPixel[0].byteValue());
        // Test for image with 3 band
        Number[] arrayPixel3Band = testExecution(testBean, 3);
        //
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  Source 1 value", valideValueSource1,
                arrayPixel3Band[0].byteValue());
        assertEquals("Pixel in the upper center must have the  Source 1 value", valideValueSource1,
                arrayPixel3Band[1].byteValue());
        assertEquals("Pixel in the upper center must have the  Source 1 value", valideValueSource1,
                arrayPixel3Band[2].byteValue());
        //
    }

    @Test
    public void testUShortData1Data2() {
        //
        if (!initialSetup) {
            initalSetup();
        }
        //
        MosaicBean testBean = beanContainer[1][2];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecution(testBean, 1);
        short valideValueSource1 = testBean.getSourceValidData()[0].shortValue();
        //
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  Source 1 value", valideValueSource1,
                arrayPixel[0].shortValue());
        // Test for image with 3 band
        Number[] arrayPixel3Band = testExecution(testBean, 3);
        //
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  Source 1 value", valideValueSource1,
                arrayPixel3Band[0].shortValue());
        assertEquals("Pixel in the upper center must have the  Source 1 value", valideValueSource1,
                arrayPixel3Band[1].shortValue());
        assertEquals("Pixel in the upper center must have the  Source 1 value", valideValueSource1,
                arrayPixel3Band[2].shortValue());
        //
    }

    @Test
    public void testShortData1Data2() {
        //
        if (!initialSetup) {
            initalSetup();
        }
        //
        MosaicBean testBean = beanContainer[2][2];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecution(testBean, 1);
        short valideValueSource1 = testBean.getSourceValidData()[0].shortValue();
        //
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  Source 1 value", valideValueSource1,
                arrayPixel[0].shortValue());
        // Test for image with 3 band
        Number[] arrayPixel3Band = testExecution(testBean, 3);
        //
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  Source 1 value", valideValueSource1,
                arrayPixel3Band[0].shortValue());
        assertEquals("Pixel in the upper center must have the  Source 1 value", valideValueSource1,
                arrayPixel3Band[1].shortValue());
        assertEquals("Pixel in the upper center must have the  Source 1 value", valideValueSource1,
                arrayPixel3Band[2].shortValue());
        //
    }

    @Test
    public void testIntData1Data2() {
        //
        if (!initialSetup) {
            initalSetup();
        }
        //
        MosaicBean testBean = beanContainer[3][2];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecution(testBean, 1);
        int valideValueSource1 = testBean.getSourceValidData()[0].intValue();
        //
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  Source 1 value", valideValueSource1,
                arrayPixel[0].intValue());
        // Test for image with 3 band
        Number[] arrayPixel3Band = testExecution(testBean, 3);
        //
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  Source 1 value", valideValueSource1,
                arrayPixel3Band[0].intValue());
        assertEquals("Pixel in the upper center must have the  Source 1 value", valideValueSource1,
                arrayPixel3Band[1].intValue());
        assertEquals("Pixel in the upper center must have the  Source 1 value", valideValueSource1,
                arrayPixel3Band[2].intValue());
        //
    }

    @Test
    public void testFloatData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        //
        MosaicBean testBean = beanContainer[4][2];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecution(testBean, 1);
        float valideValueSource1 = testBean.getSourceValidData()[0].floatValue();
        //
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  Source 1 value", valideValueSource1,
                arrayPixel[0].floatValue(), DEFAULT_DELTA);
        // Test for image with 3 band
        Number[] arrayPixel3Band = testExecution(testBean, 3);
        //
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  Source 1 value", valideValueSource1,
                arrayPixel3Band[0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the  Source 1 value", valideValueSource1,
                arrayPixel3Band[1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the  Source 1 value", valideValueSource1,
                arrayPixel3Band[2].floatValue(), DEFAULT_DELTA);
        //
    }

    @Test
    public void testDoubleData1Data2() {
        //
        if (!initialSetup) {
            initalSetup();
        }
        //
        MosaicBean testBean = beanContainer[5][2];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecution(testBean, 1);
        double valideValueSource1 = testBean.getSourceValidData()[0].doubleValue();
        //
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  Source 1 value", valideValueSource1,
                arrayPixel[0].doubleValue(), DEFAULT_DELTA);
        // Test for image with 3 band
        Number[] arrayPixel3Band = testExecution(testBean, 3);
        //
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper center must have the  Source 1 value", valideValueSource1,
                arrayPixel3Band[0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the  Source 1 value", valideValueSource1,
                arrayPixel3Band[1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the  Source 1 value", valideValueSource1,
                arrayPixel3Band[2].doubleValue(), DEFAULT_DELTA);
        //
    }

    // Test ROI: image 1 and 2 with data
    @Test
    public void testROIByteData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        if (!initialROICreation) {
            creationROI();
        }
        //
        MosaicBean testBean = beanContainer[0][2];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecutionROI(testBean, 1)[0];
        //
        byte destinationNoData = testBean.getDestinationNoData()[0].byteValue();
        byte valideValueSource1 = testBean.getSourceValidData()[0].byteValue();
        byte valideValueSource2 = testBean.getSourceValidData()[1].byteValue();
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData, arrayPixel[0].byteValue());
        assertEquals("Pixel in the upper first center must have the  second image Data Value",
                valideValueSource2, arrayPixel[1].byteValue());
        assertEquals("Pixel in the upper center must have the first image Data Value",
                valideValueSource1, arrayPixel[2].byteValue());
        //
        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecutionROI(testBean, 3);
        //
        byte[] destinationNoData3Band = new byte[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].byteValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].byteValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].byteValue();
        //
        //
        //
        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[0], arrayPixel3Band[0][0].byteValue());
        assertEquals("Pixel in the upper first center must have the  second image Data Value",
                valideValueSource2, arrayPixel3Band[0][1].byteValue());
        assertEquals("Pixel in the upper center must have the first image Data Value",
                valideValueSource1, arrayPixel3Band[0][2].byteValue());
        //
        // second band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[1], arrayPixel3Band[1][0].byteValue());
        assertEquals("Pixel in the upper first center must have the  second image Data Value",
                valideValueSource2, arrayPixel3Band[1][1].byteValue());
        assertEquals("Pixel in the upper center must have the first image Data Value",
                valideValueSource1, arrayPixel3Band[1][2].byteValue());
        //
        // third band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[2], arrayPixel3Band[2][0].byteValue());
        assertEquals("Pixel in the upper first center must have the  second image Data Value",
                valideValueSource2, arrayPixel3Band[2][1].byteValue());
        assertEquals("Pixel in the upper center must have the first image Data Value",
                valideValueSource1, arrayPixel3Band[2][2].byteValue());
        //
    }

    @Test
    public void testROIUShortData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        if (!initialROICreation) {
            creationROI();
        }
        //
        MosaicBean testBean = beanContainer[1][2];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecutionROI(testBean, 1)[0];
        //
        short destinationNoData = testBean.getDestinationNoData()[0].shortValue();
        short valideValueSource1 = testBean.getSourceValidData()[0].shortValue();
        short valideValueSource2 = testBean.getSourceValidData()[1].shortValue();
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData, arrayPixel[0].shortValue());
        assertEquals("Pixel in the upper first center must have the  second image Data Value",
                valideValueSource2, arrayPixel[1].shortValue());
        assertEquals("Pixel in the upper center must have the first image Data Value",
                valideValueSource1, arrayPixel[2].shortValue());
        //
        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecutionROI(testBean, 3);
        //
        short[] destinationNoData3Band = new short[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].shortValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].shortValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].shortValue();
        //
        //
        //
        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[0], arrayPixel3Band[0][0].shortValue());
        assertEquals("Pixel in the upper first center must have the  second image Data Value",
                valideValueSource2, arrayPixel3Band[0][1].shortValue());
        assertEquals("Pixel in the upper center must have the first image Data Value",
                valideValueSource1, arrayPixel3Band[0][2].shortValue());
        //
        // second band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[1], arrayPixel3Band[1][0].shortValue());
        assertEquals("Pixel in the upper first center must have the  second image Data Value",
                valideValueSource2, arrayPixel3Band[1][1].shortValue());
        assertEquals("Pixel in the upper center must have the first image Data Value",
                valideValueSource1, arrayPixel3Band[1][2].shortValue());
        //
        // third band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[2], arrayPixel3Band[2][0].shortValue());
        assertEquals("Pixel in the upper first center must have the  second image Data Value",
                valideValueSource2, arrayPixel3Band[2][1].shortValue());
        assertEquals("Pixel in the upper center must have the first image Data Value",
                valideValueSource1, arrayPixel3Band[2][2].shortValue());
        //
    }

    @Test
    public void testROIShortData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        if (!initialROICreation) {
            creationROI();
        }
        //
        MosaicBean testBean = beanContainer[2][2];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecutionROI(testBean, 1)[0];
        //
        short destinationNoData = testBean.getDestinationNoData()[0].shortValue();
        short valideValueSource1 = testBean.getSourceValidData()[0].shortValue();
        short valideValueSource2 = testBean.getSourceValidData()[1].shortValue();
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData, arrayPixel[0].shortValue());
        assertEquals("Pixel in the upper first center must have the  second image Data Value",
                valideValueSource2, arrayPixel[1].shortValue());
        assertEquals("Pixel in the upper center must have the first image Data Value",
                valideValueSource1, arrayPixel[2].shortValue());
        //
        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecutionROI(testBean, 3);
        //
        short[] destinationNoData3Band = new short[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].shortValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].shortValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].shortValue();
        //
        //
        //
        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[0], arrayPixel3Band[0][0].shortValue());
        assertEquals("Pixel in the upper first center must have the  second image Data Value",
                valideValueSource2, arrayPixel3Band[0][1].shortValue());
        assertEquals("Pixel in the upper center must have the first image Data Value",
                valideValueSource1, arrayPixel3Band[0][2].shortValue());
        //
        // second band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[1], arrayPixel3Band[1][0].shortValue());
        assertEquals("Pixel in the upper first center must have the  second image Data Value",
                valideValueSource2, arrayPixel3Band[1][1].shortValue());
        assertEquals("Pixel in the upper center must have the first image Data Value",
                valideValueSource1, arrayPixel3Band[1][2].shortValue());
        //
        // third band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[2], arrayPixel3Band[2][0].shortValue());
        assertEquals("Pixel in the upper first center must have the  second image Data Value",
                valideValueSource2, arrayPixel3Band[2][1].shortValue());
        assertEquals("Pixel in the upper center must have the first image Data Value",
                valideValueSource1, arrayPixel3Band[2][2].shortValue());
        //
    }

    @Test
    public void testROIIntData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        if (!initialROICreation) {
            creationROI();
        }
        //
        MosaicBean testBean = beanContainer[3][2];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecutionROI(testBean, 1)[0];
        //
        int destinationNoData = testBean.getDestinationNoData()[0].intValue();
        int valideValueSource1 = testBean.getSourceValidData()[0].intValue();
        int valideValueSource2 = testBean.getSourceValidData()[1].intValue();
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData, arrayPixel[0].intValue());
        assertEquals("Pixel in the upper first center must have the  second image Data Value",
                valideValueSource2, arrayPixel[1].intValue());
        assertEquals("Pixel in the upper center must have the first image Data Value",
                valideValueSource1, arrayPixel[2].intValue());
        //
        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecutionROI(testBean, 3);
        //
        int[] destinationNoData3Band = new int[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].intValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].intValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].intValue();
        //
        //
        //
        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[0], arrayPixel3Band[0][0].intValue());
        assertEquals("Pixel in the upper first center must have the  second image Data Value",
                valideValueSource2, arrayPixel3Band[0][1].intValue());
        assertEquals("Pixel in the upper center must have the first image Data Value",
                valideValueSource1, arrayPixel3Band[0][2].intValue());
        //
        // second band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[1], arrayPixel3Band[1][0].intValue());
        assertEquals("Pixel in the upper first center must have the  second image Data Value",
                valideValueSource2, arrayPixel3Band[1][1].intValue());
        assertEquals("Pixel in the upper center must have the first image Data Value",
                valideValueSource1, arrayPixel3Band[1][2].intValue());
        //
        // third band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[2], arrayPixel3Band[2][0].intValue());
        assertEquals("Pixel in the upper first center must have the  second image Data Value",
                valideValueSource2, arrayPixel3Band[2][1].intValue());
        assertEquals("Pixel in the upper center must have the first image Data Value",
                valideValueSource1, arrayPixel3Band[2][2].intValue());
        //
        //
    }

    @Test
    public void testROIFloatData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        if (!initialROICreation) {
            creationROI();
        }
        //
        MosaicBean testBean = beanContainer[4][2];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecutionROI(testBean, 1)[0];
        //
        float destinationNoData = testBean.getDestinationNoData()[0].floatValue();
        float valideValueSource1 = testBean.getSourceValidData()[0].floatValue();
        float valideValueSource2 = testBean.getSourceValidData()[1].floatValue();
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData, arrayPixel[0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper first center must have the  second image Data Value",
                valideValueSource2, arrayPixel[1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the first image Data Value",
                valideValueSource1, arrayPixel[2].floatValue(), DEFAULT_DELTA);
        //
        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecutionROI(testBean, 3);
        //
        float[] destinationNoData3Band = new float[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].floatValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].floatValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].floatValue();
        //
        //
        //
        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[0], arrayPixel3Band[0][0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper first center must have the  second image Data Value",
                valideValueSource2, arrayPixel3Band[0][1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the first image Data Value",
                valideValueSource1, arrayPixel3Band[0][2].floatValue(), DEFAULT_DELTA);
        //
        // second band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[1], arrayPixel3Band[1][0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper first center must have the  second image Data Value",
                valideValueSource2, arrayPixel3Band[1][1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the first image Data Value",
                valideValueSource1, arrayPixel3Band[1][2].floatValue(), DEFAULT_DELTA);
        //
        // third band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[2], arrayPixel3Band[2][0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper first center must have the  second image Data Value",
                valideValueSource2, arrayPixel3Band[2][1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the first image Data Value",
                valideValueSource1, arrayPixel3Band[2][2].floatValue(), DEFAULT_DELTA);
        //
        //
        //
    }

    @Test
    public void testROIDoubleData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        if (!initialROICreation) {
            creationROI();
        }
        //
        MosaicBean testBean = beanContainer[5][2];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecutionROI(testBean, 1)[0];
        //
        double destinationNoData = testBean.getDestinationNoData()[0].doubleValue();
        double valideValueSource1 = testBean.getSourceValidData()[0].doubleValue();
        double valideValueSource2 = testBean.getSourceValidData()[1].doubleValue();
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData, arrayPixel[0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper first center must have the  second image Data Value",
                valideValueSource2, arrayPixel[1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the first image Data Value",
                valideValueSource1, arrayPixel[2].doubleValue(), DEFAULT_DELTA);
        //
        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecutionROI(testBean, 3);
        //
        double[] destinationNoData3Band = new double[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].doubleValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].doubleValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].doubleValue();
        //
        //
        //
        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[0], arrayPixel3Band[0][0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper first center must have the  second image Data Value",
                valideValueSource2, arrayPixel3Band[0][1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the first image Data Value",
                valideValueSource1, arrayPixel3Band[0][2].doubleValue(), DEFAULT_DELTA);
        //
        // second band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[1], arrayPixel3Band[1][0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper first center must have the  second image Data Value",
                valideValueSource2, arrayPixel3Band[1][1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the first image Data Value",
                valideValueSource1, arrayPixel3Band[1][2].doubleValue(), DEFAULT_DELTA);
        //
        // third band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[2], arrayPixel3Band[2][0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper first center must have the  second image Data Value",
                valideValueSource2, arrayPixel3Band[2][1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the first image Data Value",
                valideValueSource1, arrayPixel3Band[2][2].doubleValue(), DEFAULT_DELTA);
        //
        //
        //
    }

    // Test ROI: image 1 with data and 2 with no data
    @Test
    public void testROIByteNoData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        if (!initialROICreation) {
            creationROI();
        }
        //
        MosaicBean testBean = beanContainer[0][1];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecutionROI(testBean, 1)[0];
        //
        byte destinationNoData = testBean.getDestinationNoData()[0].byteValue();
        //
        byte valideValueSource2 = testBean.getSourceValidData()[1].byteValue();
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData, arrayPixel[0].byteValue());
        assertEquals(
                "Pixel in the upper first center must have the  second image source Data Value",
                valideValueSource2, arrayPixel[1].byteValue());
        assertEquals("Pixel in the upper center must have the second image source Data Value",
                valideValueSource2, arrayPixel[2].byteValue());
        //
        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecutionROI(testBean, 3);
        //
        byte[] destinationNoData3Band = new byte[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].byteValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].byteValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].byteValue();
        //
        //
        //
        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[0], arrayPixel3Band[0][0].byteValue());
        assertEquals(
                "Pixel in the upper first center must have the  second image source Data Value",
                valideValueSource2, arrayPixel3Band[0][1].byteValue());
        assertEquals("Pixel in the upper center must have the second image source Data Value",
                valideValueSource2, arrayPixel3Band[0][2].byteValue());
        //
        // second band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[1], arrayPixel3Band[1][0].byteValue());
        assertEquals(
                "Pixel in the upper first center must have the  second image source Data Value",
                valideValueSource2, arrayPixel3Band[1][1].byteValue());
        assertEquals("Pixel in the upper center must have the second image source Data Value",
                valideValueSource2, arrayPixel3Band[1][2].byteValue());
        //
        // third band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[2], arrayPixel3Band[2][0].byteValue());
        assertEquals(
                "Pixel in the upper first center must have the  second image source Data Value",
                valideValueSource2, arrayPixel3Band[2][1].byteValue());
        assertEquals("Pixel in the upper center must have the second image source Data Value",
                valideValueSource2, arrayPixel3Band[2][2].byteValue());

    }

    @Test
    public void testROIUShortNoData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        if (!initialROICreation) {
            creationROI();
        }
        //
        MosaicBean testBean = beanContainer[1][1];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecutionROI(testBean, 1)[0];
        //
        short destinationNoData = testBean.getDestinationNoData()[0].shortValue();
        //
        short valideValueSource2 = testBean.getSourceValidData()[1].shortValue();
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData, arrayPixel[0].shortValue());
        assertEquals(
                "Pixel in the upper first center must have the  second image source Data Value",
                valideValueSource2, arrayPixel[1].shortValue());
        assertEquals("Pixel in the upper center must have the second image source Data Value",
                valideValueSource2, arrayPixel[2].shortValue());
        //
        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecutionROI(testBean, 3);
        //
        short[] destinationNoData3Band = new short[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].shortValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].shortValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].shortValue();
        //
        //
        //
        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[0], arrayPixel3Band[0][0].shortValue());
        assertEquals(
                "Pixel in the upper first center must have the  second image source Data Value",
                valideValueSource2, arrayPixel3Band[0][1].shortValue());
        assertEquals("Pixel in the upper center must have the second image source Data Value",
                valideValueSource2, arrayPixel3Band[0][2].shortValue());
        //
        // second band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[1], arrayPixel3Band[1][0].shortValue());
        assertEquals(
                "Pixel in the upper first center must have the  second image source Data Value",
                valideValueSource2, arrayPixel3Band[1][1].shortValue());
        assertEquals("Pixel in the upper center must have the second image source Data Value",
                valideValueSource2, arrayPixel3Band[1][2].shortValue());
        //
        // third band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[2], arrayPixel3Band[2][0].shortValue());
        assertEquals(
                "Pixel in the upper first center must have the  second image source Data Value",
                valideValueSource2, arrayPixel3Band[2][1].shortValue());
        assertEquals("Pixel in the upper center must have the second image source Data Value",
                valideValueSource2, arrayPixel3Band[2][2].shortValue());

    }

    @Test
    public void testROIShortNoData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        if (!initialROICreation) {
            creationROI();
        }
        //
        MosaicBean testBean = beanContainer[2][1];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecutionROI(testBean, 1)[0];
        //
        short destinationNoData = testBean.getDestinationNoData()[0].shortValue();
        //
        short valideValueSource2 = testBean.getSourceValidData()[1].shortValue();
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData, arrayPixel[0].shortValue());
        assertEquals(
                "Pixel in the upper first center must have the  second image source Data Value",
                valideValueSource2, arrayPixel[1].shortValue());
        assertEquals("Pixel in the upper center must have the second image source Data Value",
                valideValueSource2, arrayPixel[2].shortValue());
        //
        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecutionROI(testBean, 3);
        //
        short[] destinationNoData3Band = new short[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].shortValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].shortValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].shortValue();
        //
        //
        //
        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[0], arrayPixel3Band[0][0].shortValue());
        assertEquals(
                "Pixel in the upper first center must have the  second image source Data Value",
                valideValueSource2, arrayPixel3Band[0][1].shortValue());
        assertEquals("Pixel in the upper center must have the second image source Data Value",
                valideValueSource2, arrayPixel3Band[0][2].shortValue());
        //
        // second band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[1], arrayPixel3Band[1][0].shortValue());
        assertEquals(
                "Pixel in the upper first center must have the  second image source Data Value",
                valideValueSource2, arrayPixel3Band[1][1].shortValue());
        assertEquals("Pixel in the upper center must have the second image source Data Value",
                valideValueSource2, arrayPixel3Band[1][2].shortValue());
        //
        // third band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[2], arrayPixel3Band[2][0].shortValue());
        assertEquals(
                "Pixel in the upper first center must have the  second image source Data Value",
                valideValueSource2, arrayPixel3Band[2][1].shortValue());
        assertEquals("Pixel in the upper center must have the second image source Data Value",
                valideValueSource2, arrayPixel3Band[2][2].shortValue());
    }

    @Test
    public void testROIIntNoData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        if (!initialROICreation) {
            creationROI();
        }
        //
        MosaicBean testBean = beanContainer[3][1];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecutionROI(testBean, 1)[0];
        //
        int destinationNoData = testBean.getDestinationNoData()[0].intValue();
        //
        int valideValueSource2 = testBean.getSourceValidData()[1].intValue();
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData, arrayPixel[0].intValue());
        assertEquals(
                "Pixel in the upper first center must have the  second image source Data Value",
                valideValueSource2, arrayPixel[1].intValue());
        assertEquals("Pixel in the upper center must have the second image source Data Value",
                valideValueSource2, arrayPixel[2].intValue());
        //
        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecutionROI(testBean, 3);
        //
        int[] destinationNoData3Band = new int[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].intValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].intValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].intValue();
        //
        //
        //
        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[0], arrayPixel3Band[0][0].intValue());
        assertEquals(
                "Pixel in the upper first center must have the  second image source Data Value",
                valideValueSource2, arrayPixel3Band[0][1].intValue());
        assertEquals("Pixel in the upper center must have the second image source Data Value",
                valideValueSource2, arrayPixel3Band[0][2].intValue());
        //
        // second band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[1], arrayPixel3Band[1][0].intValue());
        assertEquals(
                "Pixel in the upper first center must have the  second image source Data Value",
                valideValueSource2, arrayPixel3Band[1][1].intValue());
        assertEquals("Pixel in the upper center must have the second image source Data Value",
                valideValueSource2, arrayPixel3Band[1][2].intValue());
        //
        // third band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[2], arrayPixel3Band[2][0].intValue());
        assertEquals(
                "Pixel in the upper first center must have the  second image source Data Value",
                valideValueSource2, arrayPixel3Band[2][1].intValue());
        assertEquals("Pixel in the upper center must have the second image source Data Value",
                valideValueSource2, arrayPixel3Band[2][2].intValue());

    }

    @Test
    public void testROIFloatNoData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        if (!initialROICreation) {
            creationROI();
        }
        //
        MosaicBean testBean = beanContainer[4][1];
        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecutionROI(testBean, 1)[0];
        //
        float destinationNoData = testBean.getDestinationNoData()[0].floatValue();
        //
        float valideValueSource2 = testBean.getSourceValidData()[1].floatValue();
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData, arrayPixel[0].floatValue(), DEFAULT_DELTA);
        assertEquals(
                "Pixel in the upper first center must have the  second image source Data Value",
                valideValueSource2, arrayPixel[1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the second image source Data Value",
                valideValueSource2, arrayPixel[2].floatValue(), DEFAULT_DELTA);
        //
        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecutionROI(testBean, 3);
        //
        float[] destinationNoData3Band = new float[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].floatValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].floatValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].floatValue();
        //
        //
        //
        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[0], arrayPixel3Band[0][0].floatValue(), DEFAULT_DELTA);
        assertEquals(
                "Pixel in the upper first center must have the  second image source Data Value",
                valideValueSource2, arrayPixel3Band[0][1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the second image source Data Value",
                valideValueSource2, arrayPixel3Band[0][2].floatValue(), DEFAULT_DELTA);
        //
        // second band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[1], arrayPixel3Band[1][0].floatValue(), DEFAULT_DELTA);
        assertEquals(
                "Pixel in the upper first center must have the  second image source Data Value",
                valideValueSource2, arrayPixel3Band[1][1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the second image source Data Value",
                valideValueSource2, arrayPixel3Band[1][2].floatValue(), DEFAULT_DELTA);
        //
        // third band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[2], arrayPixel3Band[2][0].floatValue(), DEFAULT_DELTA);
        assertEquals(
                "Pixel in the upper first center must have the  second image source Data Value",
                valideValueSource2, arrayPixel3Band[2][1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the second image source Data Value",
                valideValueSource2, arrayPixel3Band[2][2].floatValue(), DEFAULT_DELTA);
    }

    @Test
    public void testROIDoubleNoData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        if (!initialROICreation) {
            creationROI();
        }

        MosaicBean testBean = beanContainer[5][1];

        //
        // Test for image with 1 band
        Number[] arrayPixel = testExecutionROI(testBean, 1)[0];
        //
        double destinationNoData = testBean.getDestinationNoData()[0].doubleValue();
        //
        double valideValueSource2 = testBean.getSourceValidData()[1].doubleValue();
        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData, arrayPixel[0].doubleValue(), DEFAULT_DELTA);
        assertEquals(
                "Pixel in the upper first center must have the  second image source Data Value",
                valideValueSource2, arrayPixel[1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the second image source Data Value",
                valideValueSource2, arrayPixel[2].doubleValue(), DEFAULT_DELTA);
        //
        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecutionROI(testBean, 3);
        //
        double[] destinationNoData3Band = new double[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].doubleValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].doubleValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].doubleValue();
        //
        //
        //
        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[0], arrayPixel3Band[0][0].doubleValue(), DEFAULT_DELTA);
        assertEquals(
                "Pixel in the upper first center must have the  second image source Data Value",
                valideValueSource2, arrayPixel3Band[0][1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the second image source Data Value",
                valideValueSource2, arrayPixel3Band[0][2].doubleValue(), DEFAULT_DELTA);
        //
        // second band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[1], arrayPixel3Band[1][0].doubleValue(), DEFAULT_DELTA);
        assertEquals(
                "Pixel in the upper first center must have the  second image source Data Value",
                valideValueSource2, arrayPixel3Band[1][1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the second image source Data Value",
                valideValueSource2, arrayPixel3Band[1][2].doubleValue(), DEFAULT_DELTA);
        //
        // third band
        assertEquals("Pixel in the upper left must have the  destination no Data Value",
                destinationNoData3Band[2], arrayPixel3Band[2][0].doubleValue(), DEFAULT_DELTA);
        assertEquals(
                "Pixel in the upper first center must have the  second image source Data Value",
                valideValueSource2, arrayPixel3Band[2][1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the second image source Data Value",
                valideValueSource2, arrayPixel3Band[2][2].doubleValue(), DEFAULT_DELTA);
    }

    // Test Alpha: image 1 and image 2 with data
    @Test
    public void testAlphaByteData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        if (!initialAlphaCreation) {
            creationAlpha();
        }

        MosaicBean testBean = beanContainer[0][2];

        // Test for image with 1 band
        Number[] arrayPixel = testExecutionAlpha(testBean, 1)[0];

        byte valideValueSource1 = testBean.getSourceValidData()[0].byteValue();
        byte valideValueSource2 = testBean.getSourceValidData()[1].byteValue();

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel[0].byteValue());
        assertEquals("Pixel in the upper first center must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel[1].byteValue());
        assertEquals("Pixel in the upper center must have the  source 2 alpha Value",
                valideValueSource2, arrayPixel[2].byteValue());

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecutionAlpha(testBean, 3);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[0][0].byteValue());
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[0][1].byteValue());
        assertEquals("Pixel in the upper center must have the  source 2 alpha Value",
                valideValueSource2, arrayPixel3Band[0][2].byteValue());

        // second band
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[1][0].byteValue());
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[1][1].byteValue());
        assertEquals("Pixel in the upper center must have the  source 2 alpha Value",
                valideValueSource2, arrayPixel3Band[1][2].byteValue());

        // third band
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[2][0].byteValue());
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[2][1].byteValue());
        assertEquals("Pixel in the upper center must have the  source 2 alpha Value",
                valideValueSource2, arrayPixel3Band[2][2].byteValue());
    }

    @Test
    public void testAlphaUShortData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        if (!initialAlphaCreation) {
            creationAlpha();
        }

        MosaicBean testBean = beanContainer[1][2];

        // Test for image with 1 band
        Number[] arrayPixel = testExecutionAlpha(testBean, 1)[0];

        short valideValueSource1 = testBean.getSourceValidData()[0].shortValue();
        short valideValueSource2 = testBean.getSourceValidData()[1].shortValue();

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel[0].shortValue());
        assertEquals("Pixel in the upper first center must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel[1].shortValue());
        assertEquals("Pixel in the upper center must have the  source 2 alpha Value",
                valideValueSource2, arrayPixel[2].shortValue());

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecutionAlpha(testBean, 3);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[0][0].shortValue());
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[0][1].shortValue());
        assertEquals("Pixel in the upper center must have the  source 2 alpha Value",
                valideValueSource2, arrayPixel3Band[0][2].shortValue());

        // second band
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[1][0].shortValue());
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[1][1].shortValue());
        assertEquals("Pixel in the upper center must have the  source 2 alpha Value",
                valideValueSource2, arrayPixel3Band[1][2].shortValue());

        // third band
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[2][0].shortValue());
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[2][1].shortValue());
        assertEquals("Pixel in the upper center must have the  source 2 alpha Value",
                valideValueSource2, arrayPixel3Band[2][2].shortValue());
    }

    @Test
    public void testAlphaShortData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        if (!initialAlphaCreation) {
            creationAlpha();
        }

        MosaicBean testBean = beanContainer[2][2];

        // Test for image with 1 band
        Number[] arrayPixel = testExecutionAlpha(testBean, 1)[0];

        short valideValueSource1 = testBean.getSourceValidData()[0].shortValue();
        short valideValueSource2 = testBean.getSourceValidData()[1].shortValue();

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel[0].shortValue());
        assertEquals("Pixel in the upper first center must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel[1].shortValue());
        assertEquals("Pixel in the upper center must have the  source 2 alpha Value",
                valideValueSource2, arrayPixel[2].shortValue());

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecutionAlpha(testBean, 3);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[0][0].shortValue());
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[0][1].shortValue());
        assertEquals("Pixel in the upper center must have the  source 2 alpha Value",
                valideValueSource2, arrayPixel3Band[0][2].shortValue());

        // second band
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[1][0].shortValue());
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[1][1].shortValue());
        assertEquals("Pixel in the upper center must have the  source 2 alpha Value",
                valideValueSource2, arrayPixel3Band[1][2].shortValue());

        // third band
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[2][0].shortValue());
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[2][1].shortValue());
        assertEquals("Pixel in the upper center must have the  source 2 alpha Value",
                valideValueSource2, arrayPixel3Band[2][2].shortValue());
    }

    @Test
    public void testAlphaIntData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        if (!initialAlphaCreation) {
            creationAlpha();
        }

        MosaicBean testBean = beanContainer[3][2];

        // Test for image with 1 band
        Number[] arrayPixel = testExecutionAlpha(testBean, 1)[0];

        int valideValueSource1 = testBean.getSourceValidData()[0].intValue();
        int valideValueSource2 = testBean.getSourceValidData()[1].intValue();

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel[0].intValue());
        assertEquals("Pixel in the upper first center must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel[1].intValue());
        assertEquals("Pixel in the upper center must have the  source 2 alpha Value",
                valideValueSource2, arrayPixel[2].intValue());

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecutionAlpha(testBean, 3);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[0][0].intValue());
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[0][1].intValue());
        assertEquals("Pixel in the upper center must have the  source 2 alpha Value",
                valideValueSource2, arrayPixel3Band[0][2].intValue());

        // second band
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[1][0].intValue());
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[1][1].intValue());
        assertEquals("Pixel in the upper center must have the  source 2 alpha Value",
                valideValueSource2, arrayPixel3Band[1][2].intValue());

        // third band
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[2][0].intValue());
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[2][1].intValue());
        assertEquals("Pixel in the upper center must have the  source 2 alpha Value",
                valideValueSource2, arrayPixel3Band[2][2].intValue());
    }

    @Test
    public void testAlphaFloatData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        if (!initialAlphaCreation) {
            creationAlpha();
        }

        MosaicBean testBean = beanContainer[4][2];

        // Test for image with 1 band
        Number[] arrayPixel = testExecutionAlpha(testBean, 1)[0];

        float valideValueSource1 = testBean.getSourceValidData()[0].floatValue();
        float valideValueSource2 = testBean.getSourceValidData()[1].floatValue();

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel[0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper first center must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel[1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the  source 2 alpha Value",
                valideValueSource2, arrayPixel[2].floatValue(), DEFAULT_DELTA);

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecutionAlpha(testBean, 3);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[0][0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[0][1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the  source 2 alpha Value",
                valideValueSource2, arrayPixel3Band[0][2].floatValue(), DEFAULT_DELTA);

        // second band
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[1][0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[1][1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the  source 2 alpha Value",
                valideValueSource2, arrayPixel3Band[1][2].floatValue(), DEFAULT_DELTA);

        // third band
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[2][0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[2][1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the  source 2 alpha Value",
                valideValueSource2, arrayPixel3Band[2][2].floatValue(), DEFAULT_DELTA);
    }

    @Test
    public void testAlphaDoubleData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        if (!initialAlphaCreation) {
            creationAlpha();
        }

        MosaicBean testBean = beanContainer[5][2];

        // Test for image with 1 band
        Number[] arrayPixel = testExecutionAlpha(testBean, 1)[0];

        double valideValueSource1 = testBean.getSourceValidData()[0].doubleValue();
        double valideValueSource2 = testBean.getSourceValidData()[1].doubleValue();

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel[0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper first center must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel[1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the  source 2 alpha Value",
                valideValueSource2, arrayPixel[2].doubleValue(), DEFAULT_DELTA);

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecutionAlpha(testBean, 3);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[0][0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[0][1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the  source 2 alpha Value",
                valideValueSource2, arrayPixel3Band[0][2].doubleValue(), DEFAULT_DELTA);

        // second band
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[1][0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[1][1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the  source 2 alpha Value",
                valideValueSource2, arrayPixel3Band[1][2].doubleValue(), DEFAULT_DELTA);

        // third band
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[2][0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper left must have the  source 1 alpha Value",
                valideValueSource1, arrayPixel3Band[2][1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the  source 2 alpha Value",
                valideValueSource2, arrayPixel3Band[2][2].doubleValue(), DEFAULT_DELTA);
    }

    // Test Alpha: image 1 with data and image 2 with no data
    @Test
    public void testAlphaNoByteData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        if (!initialAlphaCreation) {
            creationAlpha();
        }

        MosaicBean testBean = beanContainer[0][1];

        // Test for image with 1 band
        Number[] arrayPixel = testExecutionAlpha(testBean, 1)[0];

        byte valideValueSource2 = testBean.getSourceValidData()[1].byteValue();

        byte destinationNoData = testBean.getDestinationNoData()[0].byteValue();

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData, arrayPixel[0].byteValue());
        assertEquals("Pixel in the upper first center must have the source 2 data Value",
                valideValueSource2, arrayPixel[1].byteValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel[2].byteValue());

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecutionAlpha(testBean, 3);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the  destination no data Value",
                destinationNoData, arrayPixel3Band[0][0].byteValue());
        assertEquals("Pixel in the upper left must have the source 2 data Valuee",
                valideValueSource2, arrayPixel3Band[0][1].byteValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[0][2].byteValue());

        // second band
        assertEquals("Pixel in the upper left must have the  destination no data Value",
                destinationNoData, arrayPixel3Band[1][0].byteValue());
        assertEquals("Pixel in the upper left must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][1].byteValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][2].byteValue());

        // third band
        assertEquals("Pixel in the upper left must have the  destination no data Value",
                destinationNoData, arrayPixel3Band[2][0].byteValue());
        assertEquals("Pixel in the upper left must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][1].byteValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][2].byteValue());
    }

    @Test
    public void testAlphaNoUShortData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        if (!initialAlphaCreation) {
            creationAlpha();
        }

        MosaicBean testBean = beanContainer[1][1];

        // Test for image with 1 band
        Number[] arrayPixel = testExecutionAlpha(testBean, 1)[0];

        short valideValueSource2 = testBean.getSourceValidData()[1].shortValue();

        short destinationNoData = testBean.getDestinationNoData()[0].shortValue();

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData, arrayPixel[0].shortValue());
        assertEquals("Pixel in the upper first center must have the source 2 data Value",
                valideValueSource2, arrayPixel[1].shortValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel[2].shortValue());

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecutionAlpha(testBean, 3);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the  destination no data Value",
                destinationNoData, arrayPixel3Band[0][0].shortValue());
        assertEquals("Pixel in the upper left must have the source 2 data Valuee",
                valideValueSource2, arrayPixel3Band[0][1].shortValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[0][2].shortValue());

        // second band
        assertEquals("Pixel in the upper left must have the  destination no data Value",
                destinationNoData, arrayPixel3Band[1][0].shortValue());
        assertEquals("Pixel in the upper left must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][1].shortValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][2].shortValue());

        // third band
        assertEquals("Pixel in the upper left must have the  destination no data Value",
                destinationNoData, arrayPixel3Band[2][0].shortValue());
        assertEquals("Pixel in the upper left must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][1].shortValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][2].shortValue());
    }

    @Test
    public void testAlphaNoShortData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        if (!initialAlphaCreation) {
            creationAlpha();
        }

        MosaicBean testBean = beanContainer[2][1];

        // Test for image with 1 band
        Number[] arrayPixel = testExecutionAlpha(testBean, 1)[0];

        short valideValueSource2 = testBean.getSourceValidData()[1].shortValue();

        short destinationNoData = testBean.getDestinationNoData()[0].shortValue();

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData, arrayPixel[0].shortValue());
        assertEquals("Pixel in the upper first center must have the source 2 data Value",
                valideValueSource2, arrayPixel[1].shortValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel[2].shortValue());

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecutionAlpha(testBean, 3);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the  destination no data Value",
                destinationNoData, arrayPixel3Band[0][0].shortValue());
        assertEquals("Pixel in the upper left must have the source 2 data Valuee",
                valideValueSource2, arrayPixel3Band[0][1].shortValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[0][2].shortValue());

        // second band
        assertEquals("Pixel in the upper left must have the  destination no data Value",
                destinationNoData, arrayPixel3Band[1][0].shortValue());
        assertEquals("Pixel in the upper left must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][1].shortValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][2].shortValue());

        // third band
        assertEquals("Pixel in the upper left must have the  destination no data Value",
                destinationNoData, arrayPixel3Band[2][0].shortValue());
        assertEquals("Pixel in the upper left must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][1].shortValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][2].shortValue());
    }

    @Test
    public void testAlphaIntNoData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        if (!initialAlphaCreation) {
            creationAlpha();
        }

        MosaicBean testBean = beanContainer[3][1];

        // Test for image with 1 band
        Number[] arrayPixel = testExecutionAlpha(testBean, 1)[0];

        int valideValueSource2 = testBean.getSourceValidData()[1].intValue();

        int destinationNoData = testBean.getDestinationNoData()[0].intValue();

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData, arrayPixel[0].intValue());
        assertEquals("Pixel in the upper first center must have the source 2 data Value",
                valideValueSource2, arrayPixel[1].intValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel[2].intValue());

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecutionAlpha(testBean, 3);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the  destination no data Value",
                destinationNoData, arrayPixel3Band[0][0].intValue());
        assertEquals("Pixel in the upper left must have the source 2 data Valuee",
                valideValueSource2, arrayPixel3Band[0][1].intValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[0][2].intValue());

        // second band
        assertEquals("Pixel in the upper left must have the  destination no data Value",
                destinationNoData, arrayPixel3Band[1][0].intValue());
        assertEquals("Pixel in the upper left must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][1].intValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][2].intValue());

        // third band
        assertEquals("Pixel in the upper left must have the  destination no data Value",
                destinationNoData, arrayPixel3Band[2][0].intValue());
        assertEquals("Pixel in the upper left must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][1].intValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][2].intValue());
    }

    @Test
    public void testAlphaFloatNoData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        if (!initialAlphaCreation) {
            creationAlpha();
        }

        MosaicBean testBean = beanContainer[4][1];

        // Test for image with 1 band
        Number[] arrayPixel = testExecutionAlpha(testBean, 1)[0];

        float valideValueSource2 = testBean.getSourceValidData()[1].floatValue();

        float destinationNoData = testBean.getDestinationNoData()[0].floatValue();

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData, arrayPixel[0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper first center must have the source 2 data Value",
                valideValueSource2, arrayPixel[1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel[2].floatValue(), DEFAULT_DELTA);

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecutionAlpha(testBean, 3);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the  destination no data Value",
                destinationNoData, arrayPixel3Band[0][0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper left must have the source 2 data Valuee",
                valideValueSource2, arrayPixel3Band[0][1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[0][2].floatValue(), DEFAULT_DELTA);

        // second band
        assertEquals("Pixel in the upper left must have the  destination no data Value",
                destinationNoData, arrayPixel3Band[1][0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper left must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][2].floatValue(), DEFAULT_DELTA);

        // third band
        assertEquals("Pixel in the upper left must have the  destination no data Value",
                destinationNoData, arrayPixel3Band[2][0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper left must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][2].floatValue(), DEFAULT_DELTA);
    }

    @Test
    public void testAlphaDoubleNoData1Data2() {
        if (!initialSetup) {
            initalSetup();
        }
        if (!initialAlphaCreation) {
            creationAlpha();
        }

        MosaicBean testBean = beanContainer[5][1];

        // Test for image with 1 band
        Number[] arrayPixel = testExecutionAlpha(testBean, 1)[0];

        double valideValueSource2 = testBean.getSourceValidData()[1].doubleValue();

        double destinationNoData = testBean.getDestinationNoData()[0].doubleValue();

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData, arrayPixel[0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper first center must have the source 2 data Value",
                valideValueSource2, arrayPixel[1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel[2].doubleValue(), DEFAULT_DELTA);

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecutionAlpha(testBean, 3);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the  destination no data Value",
                destinationNoData, arrayPixel3Band[0][0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper left must have the source 2 data Valuee",
                valideValueSource2, arrayPixel3Band[0][1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[0][2].doubleValue(), DEFAULT_DELTA);

        // second band
        assertEquals("Pixel in the upper left must have the  destination no data Value",
                destinationNoData, arrayPixel3Band[1][0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper left must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][2].doubleValue(), DEFAULT_DELTA);

        // third band
        assertEquals("Pixel in the upper left must have the  destination no data Value",
                destinationNoData, arrayPixel3Band[2][0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper left must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][2].doubleValue(), DEFAULT_DELTA);
    }

    // TEST WITH 3 IMAGES: IMAGE 1 WITH NO DATA IN THE UPPER LEFT, IMAGE 2 WITH ROI IN THE UPPER RIGTH,
    // IMAGE 3 WITH ALPHA CHANNEL IN THE LOWER LEFT

    @Test
    public void testByte3ImagesNoDataROIAlpha() {
        if (!initialSetup3Images) {
            initialSetup3image(false, true);
        }

        MosaicBean testBean = beanContainer3Images[0];

        // Test for image with 1 band
        Number[] arrayPixel = testExecution3Image(testBean, 1,true)[0];

        byte valideValueSource2 = testBean.getSourceValidData()[1].byteValue();
        byte valideValueSource3 = testBean.getSourceValidData()[2].byteValue();
        byte destinationNoData = testBean.getDestinationNoData()[0].byteValue();

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData, arrayPixel[0].byteValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel[1].byteValue());
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel[2].byteValue());
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel[3].byteValue());
        assertEquals("Pixel in the center must have the source 2 data Value", valideValueSource2,
                arrayPixel[4].byteValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel[5].byteValue());

        byte[] destinationNoData3Band = new byte[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].byteValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].byteValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].byteValue();

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecution3Image(testBean, 3,true);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[0], arrayPixel3Band[0][0].byteValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[0][1].byteValue());
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[0][2].byteValue());
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][3].byteValue());
        assertEquals("Pixel in the center must have the source 2 data Value", valideValueSource2,
                arrayPixel3Band[0][4].byteValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][5].byteValue());

        // second band

        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[1], arrayPixel3Band[1][0].byteValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][1].byteValue());
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[1][2].byteValue());
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][3].byteValue());
        assertEquals("Pixel in the center must have the source 2 data Value", valideValueSource2,
                arrayPixel3Band[1][4].byteValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][5].byteValue());

        // third band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[2], arrayPixel3Band[2][0].byteValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][1].byteValue());
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[2][2].byteValue());
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][3].byteValue());
        assertEquals("Pixel in the center must have the source 2 data Value", valideValueSource2,
                arrayPixel3Band[2][4].byteValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][5].byteValue());

    }

    @Test
    public void testUShort3ImagesNoDataROIAlpha() {
        if (!initialSetup3Images) {
            initialSetup3image(false, true);
        }

        MosaicBean testBean = beanContainer3Images[1];

        // Test for image with 1 band
        Number[] arrayPixel = testExecution3Image(testBean, 1,true)[0];

        short valideValueSource2 = testBean.getSourceValidData()[1].shortValue();
        short valideValueSource3 = testBean.getSourceValidData()[2].shortValue();
        short destinationNoData = testBean.getDestinationNoData()[0].shortValue();

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData, arrayPixel[0].shortValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel[1].shortValue());
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel[2].shortValue());
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel[3].shortValue());
        assertEquals("Pixel in the center must have the source 2 data Value", valideValueSource2,
                arrayPixel[4].shortValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel[5].shortValue());

        short[] destinationNoData3Band = new short[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].shortValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].shortValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].shortValue();

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecution3Image(testBean, 3,true);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[0], arrayPixel3Band[0][0].shortValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[0][1].shortValue());
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[0][2].shortValue());
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][3].shortValue());
        assertEquals("Pixel in the center must have the source 2 data Value", valideValueSource2,
                arrayPixel3Band[0][4].shortValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][5].shortValue());

        // second band

        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[1], arrayPixel3Band[1][0].shortValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][1].shortValue());
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[1][2].shortValue());
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][3].shortValue());
        assertEquals("Pixel in the center must have the source 2 data Value", valideValueSource2,
                arrayPixel3Band[1][4].shortValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][5].shortValue());

        // third band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[2], arrayPixel3Band[2][0].shortValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][1].shortValue());
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[2][2].shortValue());
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][3].shortValue());
        assertEquals("Pixel in the center must have the source 2 data Value", valideValueSource2,
                arrayPixel3Band[2][4].shortValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][5].shortValue());

    }

    @Test
    public void testShort3ImagesNoDataROIAlpha() {
        if (!initialSetup3Images) {
            initialSetup3image(false, true);
        }

        MosaicBean testBean = beanContainer3Images[2];

        // Test for image with 1 band
        Number[] arrayPixel = testExecution3Image(testBean, 1,true)[0];

        short valideValueSource2 = testBean.getSourceValidData()[1].shortValue();
        short valideValueSource3 = testBean.getSourceValidData()[2].shortValue();
        short destinationNoData = testBean.getDestinationNoData()[0].shortValue();

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData, arrayPixel[0].shortValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel[1].shortValue());
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel[2].shortValue());
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel[3].shortValue());
        assertEquals("Pixel in the center must have the source 2 data Value", valideValueSource2,
                arrayPixel[4].shortValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel[5].shortValue());

        short[] destinationNoData3Band = new short[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].shortValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].shortValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].shortValue();

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecution3Image(testBean, 3,true);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[0], arrayPixel3Band[0][0].shortValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[0][1].shortValue());
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[0][2].shortValue());
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][3].shortValue());
        assertEquals("Pixel in the center must have the source 2 data Value", valideValueSource2,
                arrayPixel3Band[0][4].shortValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][5].shortValue());

        // second band

        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[1], arrayPixel3Band[1][0].shortValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][1].shortValue());
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[1][2].shortValue());
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][3].shortValue());
        assertEquals("Pixel in the center must have the source 2 data Value", valideValueSource2,
                arrayPixel3Band[1][4].shortValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][5].shortValue());

        // third band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[2], arrayPixel3Band[2][0].shortValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][1].shortValue());
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[2][2].shortValue());
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][3].shortValue());
        assertEquals("Pixel in the center must have the source 2 data Value", valideValueSource2,
                arrayPixel3Band[2][4].shortValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][5].shortValue());
    }

    @Test
    public void testInt3ImagesNoDataROIAlpha() {
        if (!initialSetup3Images) {
            initialSetup3image(false, true);
        }

        MosaicBean testBean = beanContainer3Images[3];

        // Test for image with 1 band
        Number[] arrayPixel = testExecution3Image(testBean, 1,true)[0];

        int valideValueSource2 = testBean.getSourceValidData()[1].intValue();
        int valideValueSource3 = testBean.getSourceValidData()[2].intValue();
        int destinationNoData = testBean.getDestinationNoData()[0].intValue();

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData, arrayPixel[0].intValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel[1].intValue());
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel[2].intValue());
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel[3].intValue());
        assertEquals("Pixel in the center must have the source 2 data Value", valideValueSource2,
                arrayPixel[4].intValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel[5].intValue());

        int[] destinationNoData3Band = new int[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].intValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].intValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].intValue();

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecution3Image(testBean, 3,true);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[0], arrayPixel3Band[0][0].intValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[0][1].intValue());
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[0][2].intValue());
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][3].intValue());
        assertEquals("Pixel in the center must have the source 2 data Value", valideValueSource2,
                arrayPixel3Band[0][4].intValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][5].intValue());

        // second band

        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[1], arrayPixel3Band[1][0].intValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][1].intValue());
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[1][2].intValue());
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][3].intValue());
        assertEquals("Pixel in the center must have the source 2 data Value", valideValueSource2,
                arrayPixel3Band[1][4].intValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][5].intValue());

        // third band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[2], arrayPixel3Band[2][0].intValue());
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][1].intValue());
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[2][2].intValue());
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][3].intValue());
        assertEquals("Pixel in the center must have the source 2 data Value", valideValueSource2,
                arrayPixel3Band[2][4].intValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][5].intValue());

    }

    @Test
    public void testFloat3ImagesNoDataROIAlpha() {
        if (!initialSetup3Images) {
            initialSetup3image(false, true);
        }

        MosaicBean testBean = beanContainer3Images[4];

        // Test for image with 1 band
        Number[] arrayPixel = testExecution3Image(testBean, 1,true)[0];

        float valideValueSource2 = testBean.getSourceValidData()[1].floatValue();
        float valideValueSource3 = testBean.getSourceValidData()[2].floatValue();
        float destinationNoData = testBean.getDestinationNoData()[0].floatValue();

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData, arrayPixel[0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel[1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel[2].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel[3].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the source 2 data Value", valideValueSource2,
                arrayPixel[4].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel[5].floatValue(), DEFAULT_DELTA);

        float[] destinationNoData3Band = new float[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].floatValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].floatValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].floatValue();

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecution3Image(testBean, 3,true);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[0], arrayPixel3Band[0][0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[0][1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[0][2].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][3].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the source 2 data Value", valideValueSource2,
                arrayPixel3Band[0][4].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][5].floatValue(), DEFAULT_DELTA);

        // second band

        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[1], arrayPixel3Band[1][0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[1][2].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][3].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the source 2 data Value", valideValueSource2,
                arrayPixel3Band[1][4].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][5].floatValue(), DEFAULT_DELTA);

        // third band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[2], arrayPixel3Band[2][0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[2][2].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][3].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the source 2 data Value", valideValueSource2,
                arrayPixel3Band[2][4].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][5].floatValue(), DEFAULT_DELTA);

    }

    @Test
    public void testDouble3ImagesNoDataROIAlpha() {
        if (!initialSetup3Images) {
            initialSetup3image(false, true);
        }
        MosaicBean testBean = beanContainer3Images[5];

        // Test for image with 1 band
        Number[] arrayPixel = testExecution3Image(testBean, 1,true)[0];

        double valideValueSource2 = testBean.getSourceValidData()[1].doubleValue();
        double valideValueSource3 = testBean.getSourceValidData()[2].doubleValue();
        double destinationNoData = testBean.getDestinationNoData()[0].doubleValue();

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData, arrayPixel[0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel[1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel[2].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel[3].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the source 2 data Value", valideValueSource2,
                arrayPixel[4].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel[5].doubleValue(), DEFAULT_DELTA);

        double[] destinationNoData3Band = new double[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].doubleValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].doubleValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].doubleValue();

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecution3Image(testBean, 3,true);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[0], arrayPixel3Band[0][0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[0][1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[0][2].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][3].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the source 2 data Value", valideValueSource2,
                arrayPixel3Band[0][4].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][5].doubleValue(), DEFAULT_DELTA);

        // second band

        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[1], arrayPixel3Band[1][0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[1][2].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][3].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the source 2 data Value", valideValueSource2,
                arrayPixel3Band[1][4].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][5].doubleValue(), DEFAULT_DELTA);

        // third band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[2], arrayPixel3Band[2][0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[2][2].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][3].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the source 2 data Value", valideValueSource2,
                arrayPixel3Band[2][4].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][5].doubleValue(), DEFAULT_DELTA);

    }

    // TEST WITH 3 IMAGES: SAME POSITIONS OF THE THREE IMAGES ABOVE, NULL NO DATA, NO ROI OR ALPHA CHANNEL

    @Test
    public void testByteNullNoData() {
        initialSetup3image(true, false);

        MosaicBean testBean = beanContainer3Images[0];

        // Test for image with 1 band
        Number[] arrayPixel = testExecution3Image(testBean, 1,true)[0];

        byte valideValueSource1 = testBean.getSourceValidData()[0].byteValue();
        byte valideValueSource2 = testBean.getSourceValidData()[1].byteValue();
        byte valideValueSource3 = testBean.getSourceValidData()[2].byteValue();

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the source 1 data Value",
                valideValueSource1, arrayPixel[0].byteValue());
        assertEquals("Pixel in the upper center must have the source 1 data Value",
                valideValueSource1, arrayPixel[1].byteValue());
        assertEquals("Pixel in the upper right must have the source 2 data Value",
                valideValueSource2, arrayPixel[2].byteValue());
        assertEquals("Pixel in the center left must have the source 1 data Value",
                valideValueSource1, arrayPixel[3].byteValue());
        assertEquals("Pixel in the center must have the source 1 data Value", valideValueSource1,
                arrayPixel[4].byteValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel[5].byteValue());

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecution3Image(testBean, 3,true);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[0][0].byteValue());
        assertEquals("Pixel in the upper center must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[0][1].byteValue());
        assertEquals("Pixel in the upper right must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[0][2].byteValue());
        assertEquals("Pixel in the center left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[0][3].byteValue());
        assertEquals("Pixel in the center must have the source 1 data Value", valideValueSource1,
                arrayPixel3Band[0][4].byteValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][5].byteValue());

        // second band

        assertEquals("Pixel in the upper left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[1][0].byteValue());
        assertEquals("Pixel in the upper center must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[1][1].byteValue());
        assertEquals("Pixel in the upper right must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][2].byteValue());
        assertEquals("Pixel in the center left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[1][3].byteValue());
        assertEquals("Pixel in the center must have the source 1 data Value", valideValueSource1,
                arrayPixel3Band[1][4].byteValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][5].byteValue());

        // third band
        assertEquals("Pixel in the upper left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[2][0].byteValue());
        assertEquals("Pixel in the upper center must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[2][1].byteValue());
        assertEquals("Pixel in the upper right must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][2].byteValue());
        assertEquals("Pixel in the center left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[2][3].byteValue());
        assertEquals("Pixel in the center must have the source 1 data Value", valideValueSource1,
                arrayPixel3Band[2][4].byteValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][5].byteValue());
    }

    @Test
    public void testUShortNullNoData() {
        initialSetup3image(true, false);

        MosaicBean testBean = beanContainer3Images[1];

        // Test for image with 1 band
        Number[] arrayPixel = testExecution3Image(testBean, 1,true)[0];

        short valideValueSource1 = testBean.getSourceValidData()[0].shortValue();
        short valideValueSource2 = testBean.getSourceValidData()[1].shortValue();
        short valideValueSource3 = testBean.getSourceValidData()[2].shortValue();

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the source 1 data Value",
                valideValueSource1, arrayPixel[0].shortValue());
        assertEquals("Pixel in the upper center must have the source 1 data Value",
                valideValueSource1, arrayPixel[1].shortValue());
        assertEquals("Pixel in the upper right must have the source 2 data Value",
                valideValueSource2, arrayPixel[2].shortValue());
        assertEquals("Pixel in the center left must have the source 1 data Value",
                valideValueSource1, arrayPixel[3].shortValue());
        assertEquals("Pixel in the center must have the source 1 data Value", valideValueSource1,
                arrayPixel[4].shortValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel[5].shortValue());

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecution3Image(testBean, 3,true);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[0][0].shortValue());
        assertEquals("Pixel in the upper center must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[0][1].shortValue());
        assertEquals("Pixel in the upper right must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[0][2].shortValue());
        assertEquals("Pixel in the center left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[0][3].shortValue());
        assertEquals("Pixel in the center must have the source 1 data Value", valideValueSource1,
                arrayPixel3Band[0][4].shortValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][5].shortValue());

        // second band

        assertEquals("Pixel in the upper left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[1][0].shortValue());
        assertEquals("Pixel in the upper center must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[1][1].shortValue());
        assertEquals("Pixel in the upper right must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][2].shortValue());
        assertEquals("Pixel in the center left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[1][3].shortValue());
        assertEquals("Pixel in the center must have the source 1 data Value", valideValueSource1,
                arrayPixel3Band[1][4].shortValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][5].shortValue());

        // third band
        assertEquals("Pixel in the upper left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[2][0].shortValue());
        assertEquals("Pixel in the upper center must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[2][1].shortValue());
        assertEquals("Pixel in the upper right must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][2].shortValue());
        assertEquals("Pixel in the center left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[2][3].shortValue());
        assertEquals("Pixel in the center must have the source 1 data Value", valideValueSource1,
                arrayPixel3Band[2][4].shortValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][5].shortValue());
    }

    @Test
    public void testShortNullNoData() {
        initialSetup3image(true, false);

        MosaicBean testBean = beanContainer3Images[2];

        // Test for image with 1 band
        Number[] arrayPixel = testExecution3Image(testBean, 1,true)[0];

        short valideValueSource1 = testBean.getSourceValidData()[0].shortValue();
        short valideValueSource2 = testBean.getSourceValidData()[1].shortValue();
        short valideValueSource3 = testBean.getSourceValidData()[2].shortValue();

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the source 1 data Value",
                valideValueSource1, arrayPixel[0].shortValue());
        assertEquals("Pixel in the upper center must have the source 1 data Value",
                valideValueSource1, arrayPixel[1].shortValue());
        assertEquals("Pixel in the upper right must have the source 2 data Value",
                valideValueSource2, arrayPixel[2].shortValue());
        assertEquals("Pixel in the center left must have the source 1 data Value",
                valideValueSource1, arrayPixel[3].shortValue());
        assertEquals("Pixel in the center must have the source 1 data Value", valideValueSource1,
                arrayPixel[4].shortValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel[5].shortValue());

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecution3Image(testBean, 3,true);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[0][0].shortValue());
        assertEquals("Pixel in the upper center must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[0][1].shortValue());
        assertEquals("Pixel in the upper right must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[0][2].shortValue());
        assertEquals("Pixel in the center left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[0][3].shortValue());
        assertEquals("Pixel in the center must have the source 1 data Value", valideValueSource1,
                arrayPixel3Band[0][4].shortValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][5].shortValue());

        // second band

        assertEquals("Pixel in the upper left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[1][0].shortValue());
        assertEquals("Pixel in the upper center must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[1][1].shortValue());
        assertEquals("Pixel in the upper right must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][2].shortValue());
        assertEquals("Pixel in the center left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[1][3].shortValue());
        assertEquals("Pixel in the center must have the source 1 data Value", valideValueSource1,
                arrayPixel3Band[1][4].shortValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][5].shortValue());

        // third band
        assertEquals("Pixel in the upper left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[2][0].shortValue());
        assertEquals("Pixel in the upper center must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[2][1].shortValue());
        assertEquals("Pixel in the upper right must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][2].shortValue());
        assertEquals("Pixel in the center left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[2][3].shortValue());
        assertEquals("Pixel in the center must have the source 1 data Value", valideValueSource1,
                arrayPixel3Band[2][4].shortValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][5].shortValue());
    }

    @Test
    public void testIntNullNoData() {
        initialSetup3image(true, false);

        MosaicBean testBean = beanContainer3Images[3];

        // Test for image with 1 band
        Number[] arrayPixel = testExecution3Image(testBean, 1,true)[0];

        int valideValueSource1 = testBean.getSourceValidData()[0].intValue();
        int valideValueSource2 = testBean.getSourceValidData()[1].intValue();
        int valideValueSource3 = testBean.getSourceValidData()[2].intValue();

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the source 1 data Value",
                valideValueSource1, arrayPixel[0].intValue());
        assertEquals("Pixel in the upper center must have the source 1 data Value",
                valideValueSource1, arrayPixel[1].intValue());
        assertEquals("Pixel in the upper right must have the source 2 data Value",
                valideValueSource2, arrayPixel[2].intValue());
        assertEquals("Pixel in the center left must have the source 1 data Value",
                valideValueSource1, arrayPixel[3].intValue());
        assertEquals("Pixel in the center must have the source 1 data Value", valideValueSource1,
                arrayPixel[4].intValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel[5].intValue());

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecution3Image(testBean, 3,true);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[0][0].intValue());
        assertEquals("Pixel in the upper center must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[0][1].intValue());
        assertEquals("Pixel in the upper right must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[0][2].intValue());
        assertEquals("Pixel in the center left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[0][3].intValue());
        assertEquals("Pixel in the center must have the source 1 data Value", valideValueSource1,
                arrayPixel3Band[0][4].intValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][5].intValue());

        // second band

        assertEquals("Pixel in the upper left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[1][0].intValue());
        assertEquals("Pixel in the upper center must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[1][1].intValue());
        assertEquals("Pixel in the upper right must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][2].intValue());
        assertEquals("Pixel in the center left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[1][3].intValue());
        assertEquals("Pixel in the center must have the source 1 data Value", valideValueSource1,
                arrayPixel3Band[1][4].intValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][5].intValue());

        // third band
        assertEquals("Pixel in the upper left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[2][0].intValue());
        assertEquals("Pixel in the upper center must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[2][1].intValue());
        assertEquals("Pixel in the upper right must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][2].intValue());
        assertEquals("Pixel in the center left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[2][3].intValue());
        assertEquals("Pixel in the center must have the source 1 data Value", valideValueSource1,
                arrayPixel3Band[2][4].intValue());
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][5].intValue());
    }

    @Test
    public void testFloatNullNoData() {
        initialSetup3image(true, false);

        MosaicBean testBean = beanContainer3Images[4];

        // Test for image with 1 band
        Number[] arrayPixel = testExecution3Image(testBean, 1,true)[0];

        float valideValueSource1 = testBean.getSourceValidData()[0].floatValue();
        float valideValueSource2 = testBean.getSourceValidData()[1].floatValue();
        float valideValueSource3 = testBean.getSourceValidData()[2].floatValue();

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the source 1 data Value",
                valideValueSource1, arrayPixel[0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 1 data Value",
                valideValueSource1, arrayPixel[1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the source 2 data Value",
                valideValueSource2, arrayPixel[2].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 1 data Value",
                valideValueSource1, arrayPixel[3].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the source 1 data Value", valideValueSource1,
                arrayPixel[4].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel[5].floatValue(), DEFAULT_DELTA);

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecution3Image(testBean, 3,true);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[0][0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[0][1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[0][2].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[0][3].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the source 1 data Value", valideValueSource1,
                arrayPixel3Band[0][4].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][5].floatValue(), DEFAULT_DELTA);

        // second band

        assertEquals("Pixel in the upper left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[1][0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[1][1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][2].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[1][3].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the source 1 data Value", valideValueSource1,
                arrayPixel3Band[1][4].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][5].floatValue(), DEFAULT_DELTA);

        // third band
        assertEquals("Pixel in the upper left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[2][0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[2][1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][2].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[2][3].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the source 1 data Value", valideValueSource1,
                arrayPixel3Band[2][4].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][5].floatValue(), DEFAULT_DELTA);
    }

    @Test
    public void testDoubleNullNoData() {
        initialSetup3image(true, false);

        MosaicBean testBean = beanContainer3Images[5];

        // Test for image with 1 band
        Number[] arrayPixel = testExecution3Image(testBean, 1,true)[0];

        double valideValueSource1 = testBean.getSourceValidData()[0].doubleValue();
        double valideValueSource2 = testBean.getSourceValidData()[1].doubleValue();
        double valideValueSource3 = testBean.getSourceValidData()[2].doubleValue();

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the source 1 data Value",
                valideValueSource1, arrayPixel[0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 1 data Value",
                valideValueSource1, arrayPixel[1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the source 2 data Value",
                valideValueSource2, arrayPixel[2].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 1 data Value",
                valideValueSource1, arrayPixel[3].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the source 1 data Value", valideValueSource1,
                arrayPixel[4].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel[5].doubleValue(), DEFAULT_DELTA);

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecution3Image(testBean, 3,true);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[0][0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[0][1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[0][2].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[0][3].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the source 1 data Value", valideValueSource1,
                arrayPixel3Band[0][4].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][5].doubleValue(), DEFAULT_DELTA);

        // second band

        assertEquals("Pixel in the upper left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[1][0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[1][1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][2].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[1][3].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the source 1 data Value", valideValueSource1,
                arrayPixel3Band[1][4].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][5].doubleValue(), DEFAULT_DELTA);

        // third band
        assertEquals("Pixel in the upper left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[2][0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[2][1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][2].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 1 data Value",
                valideValueSource1, arrayPixel3Band[2][3].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the source 1 data Value", valideValueSource1,
                arrayPixel3Band[2][4].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][5].doubleValue(), DEFAULT_DELTA);
    }

    @Test
    public void testBLENDByte3Images(){
        if (!initialSetup3Images) {
            initialSetup3image(false, true);
        }

        MosaicBean testBean = beanContainer3Images[0];

        // Test for image with 1 band
        Number[] arrayPixel = testExecution3Image(testBean, 1,false)[0];

        byte valideValueSource2 = testBean.getSourceValidData()[1].byteValue();
        byte valideValueSource3 = testBean.getSourceValidData()[2].byteValue();
        //destination no data value
        byte destinationNoData = testBean.getDestinationNoData()[0].byteValue();
        //blended center value
        byte centerValue= (byte) ((valideValueSource2+valideValueSource3)/(byte)2);

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData, arrayPixel[0].byteValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel[1].byteValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel[2].byteValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel[3].byteValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the mix between the second and the third image data value",
                centerValue, arrayPixel[4].byteValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel[5].byteValue(), DEFAULT_DELTA);

        byte[] destinationNoData3Band = new byte[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].byteValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].byteValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].byteValue();

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecution3Image(testBean, 3,false);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[0], arrayPixel3Band[0][0].byteValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[0][1].byteValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[0][2].byteValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][3].byteValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the mix between the second and the third image data value",
                centerValue, arrayPixel3Band[0][4].byteValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][5].byteValue(), DEFAULT_DELTA);


        // second band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[1], arrayPixel3Band[1][0].byteValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][1].byteValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[1][2].byteValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][3].byteValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the mix between the second and the third image data value",
                centerValue, arrayPixel3Band[1][4].byteValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][5].byteValue(), DEFAULT_DELTA);

        // third band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[2], arrayPixel3Band[2][0].byteValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][1].byteValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[2][2].byteValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][3].byteValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the mix between the second and the third image data value",
                centerValue, arrayPixel3Band[2][4].byteValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][5].byteValue(), DEFAULT_DELTA);        
    }
    
    @Test
    public void testBLENDUShort3Images(){
        if (!initialSetup3Images) {
            initialSetup3image(false, true);
        }

        MosaicBean testBean = beanContainer3Images[1];

        // Test for image with 1 band
        Number[] arrayPixel = testExecution3Image(testBean, 1,false)[0];

        short valideValueSource2 = testBean.getSourceValidData()[1].shortValue();
        short valideValueSource3 = testBean.getSourceValidData()[2].shortValue();
        //destination no data value
        short destinationNoData = testBean.getDestinationNoData()[0].shortValue();
        //blended center value
        short centerValue= (short) ((valideValueSource2+valideValueSource3)/(short)2);

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData, arrayPixel[0].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel[1].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel[2].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel[3].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the mix between the second and the third image data value",
                centerValue, arrayPixel[4].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel[5].shortValue(), DEFAULT_DELTA);

        short[] destinationNoData3Band = new short[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].shortValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].shortValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].shortValue();

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecution3Image(testBean, 3,false);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[0], arrayPixel3Band[0][0].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[0][1].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[0][2].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][3].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the mix between the second and the third image data value",
                centerValue, arrayPixel3Band[0][4].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][5].shortValue(), DEFAULT_DELTA);


        // second band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[1], arrayPixel3Band[1][0].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][1].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[1][2].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][3].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the mix between the second and the third image data value",
                centerValue, arrayPixel3Band[1][4].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][5].shortValue(), DEFAULT_DELTA);

        // third band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[2], arrayPixel3Band[2][0].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][1].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[2][2].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][3].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the mix between the second and the third image data value",
                centerValue, arrayPixel3Band[2][4].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][5].shortValue(), DEFAULT_DELTA);      
    }
    
    @Test
    public void testBLENDShort3Images(){
        if (!initialSetup3Images) {
            initialSetup3image(false, true);
        }

        MosaicBean testBean = beanContainer3Images[2];

        // Test for image with 1 band
        Number[] arrayPixel = testExecution3Image(testBean, 1,false)[0];

        short valideValueSource2 = testBean.getSourceValidData()[1].shortValue();
        short valideValueSource3 = testBean.getSourceValidData()[2].shortValue();
        //destination no data value
        short destinationNoData = testBean.getDestinationNoData()[0].shortValue();
        //blended center value
        short centerValue= (short) ((valideValueSource2+valideValueSource3)/(short)2);

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData, arrayPixel[0].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel[1].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel[2].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel[3].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the mix between the second and the third image data value",
                centerValue, arrayPixel[4].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel[5].shortValue(), DEFAULT_DELTA);

        short[] destinationNoData3Band = new short[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].shortValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].shortValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].shortValue();

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecution3Image(testBean, 3,false);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[0], arrayPixel3Band[0][0].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[0][1].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[0][2].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][3].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the mix between the second and the third image data value",
                centerValue, arrayPixel3Band[0][4].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][5].shortValue(), DEFAULT_DELTA);


        // second band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[1], arrayPixel3Band[1][0].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][1].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[1][2].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][3].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the mix between the second and the third image data value",
                centerValue, arrayPixel3Band[1][4].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][5].shortValue(), DEFAULT_DELTA);

        // third band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[2], arrayPixel3Band[2][0].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][1].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[2][2].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][3].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the mix between the second and the third image data value",
                centerValue, arrayPixel3Band[2][4].shortValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][5].shortValue(), DEFAULT_DELTA);        
    }
    
    @Test
    public void testBLENDInt3Images(){
        if (!initialSetup3Images) {
            initialSetup3image(false, true);
        }

        MosaicBean testBean = beanContainer3Images[3];

        // Test for image with 1 band
        Number[] arrayPixel = testExecution3Image(testBean, 1,false)[0];

        int valideValueSource2 = testBean.getSourceValidData()[1].intValue();
        int valideValueSource3 = testBean.getSourceValidData()[2].intValue();
        //destination no data value
        int destinationNoData = testBean.getDestinationNoData()[0].intValue();
        //blended center value
        int centerValue= ((valideValueSource2+valideValueSource3)/2);

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData, arrayPixel[0].intValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel[1].intValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel[2].intValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel[3].intValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the mix between the second and the third image data value",
                centerValue, arrayPixel[4].intValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel[5].intValue(), DEFAULT_DELTA);

        int[] destinationNoData3Band = new int[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].intValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].intValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].intValue();

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecution3Image(testBean, 3,false);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[0], arrayPixel3Band[0][0].intValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[0][1].intValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[0][2].intValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][3].intValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the mix between the second and the third image data value",
                centerValue, arrayPixel3Band[0][4].intValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][5].intValue(), DEFAULT_DELTA);


        // second band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[1], arrayPixel3Band[1][0].intValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][1].intValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[1][2].intValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][3].intValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the mix between the second and the third image data value",
                centerValue, arrayPixel3Band[1][4].intValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][5].intValue(), DEFAULT_DELTA);

        // third band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[2], arrayPixel3Band[2][0].intValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][1].intValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[2][2].intValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][3].intValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the mix between the second and the third image data value",
                centerValue, arrayPixel3Band[2][4].intValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][5].intValue(), DEFAULT_DELTA);        
    }
    
    @Test
    public void testBLENDFloat3Images(){
        if (!initialSetup3Images) {
            initialSetup3image(false, true);
        }

        MosaicBean testBean = beanContainer3Images[4];

        // Test for image with 1 band
        Number[] arrayPixel = testExecution3Image(testBean, 1,false)[0];

        float valideValueSource2 = testBean.getSourceValidData()[1].floatValue();
        float valideValueSource3 = testBean.getSourceValidData()[2].floatValue();
        //destination no data value
        float destinationNoData = testBean.getDestinationNoData()[0].floatValue();
        //blended center value
        float centerValue= ((valideValueSource2+valideValueSource3)/2);

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData, arrayPixel[0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel[1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel[2].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel[3].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the mix between the second and the third image data value",
                centerValue, arrayPixel[4].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel[5].floatValue(), DEFAULT_DELTA);

        float[] destinationNoData3Band = new float[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].floatValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].floatValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].floatValue();

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecution3Image(testBean, 3,false);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[0], arrayPixel3Band[0][0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[0][1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[0][2].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][3].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the mix between the second and the third image data value",
                centerValue, arrayPixel3Band[0][4].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][5].floatValue(), DEFAULT_DELTA);


        // second band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[1], arrayPixel3Band[1][0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[1][2].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][3].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the mix between the second and the third image data value",
                centerValue, arrayPixel3Band[1][4].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][5].floatValue(), DEFAULT_DELTA);

        // third band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[2], arrayPixel3Band[2][0].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][1].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[2][2].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][3].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the mix between the second and the third image data value",
                centerValue, arrayPixel3Band[2][4].floatValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][5].floatValue(), DEFAULT_DELTA);        
    }
    
    @Test
    public void testBLENDDouble3Images(){
        if (!initialSetup3Images) {
            initialSetup3image(false, true);
        }

        MosaicBean testBean = beanContainer3Images[5];

        // Test for image with 1 band
        Number[] arrayPixel = testExecution3Image(testBean, 1,false)[0];

        double valideValueSource2 = testBean.getSourceValidData()[1].doubleValue();
        double valideValueSource3 = testBean.getSourceValidData()[2].doubleValue();
        //destination no data value
        double destinationNoData = testBean.getDestinationNoData()[0].doubleValue();
        //blended center value
        double centerValue= ((valideValueSource2+valideValueSource3)/2);

        // Check if the pixel has the correct value
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData, arrayPixel[0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel[1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel[2].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel[3].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the mix between the second and the third image data value",
                centerValue, arrayPixel[4].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel[5].doubleValue(), DEFAULT_DELTA);

        double[] destinationNoData3Band = new double[3];
        destinationNoData3Band[0] = testBean.getDestinationNoData()[0].doubleValue();
        destinationNoData3Band[1] = testBean.getDestinationNoData()[1].doubleValue();
        destinationNoData3Band[2] = testBean.getDestinationNoData()[2].doubleValue();

        // Test for image with 3 band
        Number[][] arrayPixel3Band = testExecution3Image(testBean, 3,false);

        // Check if the pixel has the correct value
        // first band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[0], arrayPixel3Band[0][0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[0][1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[0][2].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][3].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the mix between the second and the third image data value",
                centerValue, arrayPixel3Band[0][4].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[0][5].doubleValue(), DEFAULT_DELTA);


        // second band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[1], arrayPixel3Band[1][0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[1][1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[1][2].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][3].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the mix between the second and the third image data value",
                centerValue, arrayPixel3Band[1][4].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[1][5].doubleValue(), DEFAULT_DELTA);

        // third band
        assertEquals("Pixel in the upper left must have the destination no data Value",
                destinationNoData3Band[2], arrayPixel3Band[2][0].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper center must have the source 2 data Value",
                valideValueSource2, arrayPixel3Band[2][1].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the upper right must have the destination no data data Value",
                destinationNoData, arrayPixel3Band[2][2].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center left must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][3].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the center must have the mix between the second and the third image data value",
                centerValue, arrayPixel3Band[2][4].doubleValue(), DEFAULT_DELTA);
        assertEquals("Pixel in the lower center must have the source 3 data Value",
                valideValueSource3, arrayPixel3Band[2][5].doubleValue(), DEFAULT_DELTA);        
    }
    
    
    // EXCEPTION TEST CLASS. THIS TESTS ARE USED FOR CHECKING IF THE MOSAIC NODATA OPIMAGE
    // CLASS THROWS THE SELECTED EXCEPTIONS
    @Test
    public void testExceptionImagesLayoutNotValid() {
        initialSetup3image(true, false);

        MosaicBean testBean = beanContainer3Images[0];
        // start image array
        RenderedImage[] mosaicArray = new RenderedImage[3];

        // Creates an array for the destination band values
        Number[] destinationValues = { testBean.getDestinationNoData()[0] };

        // Updates the innerBean
        ImageMosaicBean[] helpBean = testBean.getInnerBean3Band();

        mosaicArray = new RenderedImage[0];

        // MosaicNoData operation
        RenderedImage image5 = MosaicNoDataDescriptor.create(mosaicArray, helpBean,
                DEFAULT_MOSAIC_TYPE, destinationValues, null);

        try {
            image5.getTile(0, 0);
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "IllegalArgumentException: Layout not valid");
        }

    }

    @Test
    public void testExceptionImagesNoSampleModelPresent() {
        initialSetup3image(true, false);

        ImageLayout layout = new ImageLayout();

        layout.setValid(ImageLayout.WIDTH_MASK);
        layout.setValid(ImageLayout.HEIGHT_MASK);
        layout.setValid(ImageLayout.SAMPLE_MODEL_MASK);
        layout.setWidth(4);
        layout.setHeight(4);

        MosaicBean testBean = beanContainer3Images[0];
        // start image array
        RenderedImage[] mosaicArray = new RenderedImage[3];

        // Creates an array for the destination band values
        Number[] destinationValues = { testBean.getDestinationNoData()[0] };

        // Setting of the new RenderingHints
        RenderingHints renderingHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);

        // Updates the innerBean
        ImageMosaicBean[] helpBean = testBean.getInnerBean3Band();
        helpBean[0].setImage(mosaicArray[0]);
        helpBean[1].setImage(mosaicArray[1]);
        helpBean[2].setImage(mosaicArray[2]);

        mosaicArray = new RenderedImage[0];

        // MosaicNoData operation
        RenderedImage image5 = MosaicNoDataDescriptor.create(mosaicArray, helpBean,
                DEFAULT_MOSAIC_TYPE, destinationValues, renderingHints);

        try {
            image5.getTile(0, 0);
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "IllegalArgumentException: No sample model present");
        }

    }

    @Test
    public void testExceptionImagesSameSampleSize() {
        initialSetup3image(true, false);

        MosaicBean testBean = beanContainer3Images[0];

        // start image array
        RenderedImage[] mosaicArray = new RenderedImage[3];

        // Creates an array for the destination band values
        Number[] destinationValues = { testBean.getDestinationNoData()[0] };

        // Updates the innerBean
        ImageMosaicBean[] helpBean = testBean.getInnerBean3Band();
        helpBean[0].setImage(mosaicArray[0]);
        helpBean[1].setImage(mosaicArray[1]);
        helpBean[2].setImage(mosaicArray[2]);

        ImageLayout layout = (ImageLayout) hints.get(JAI.KEY_IMAGE_LAYOUT);

        int width = 15;
        int heigth = 15;

        layout.setSampleModel(new SampleModel(DataBuffer.TYPE_INT, width, heigth, 2) {

            public void setSample(int x, int y, int b, int s, DataBuffer data) {

            }

            public void setDataElements(int x, int y, Object obj, DataBuffer data) {

            }

            public int getSampleSize(int band) {
                if (band == 0) {
                    return 1;
                } else if (band == 1) {
                    return 2;
                }
                return 0;

            }

            public int[] getSampleSize() {
                return new int[] { 1, 2 };
            }

            public int getSample(int x, int y, int b, DataBuffer data) {
                return 0;
            }

            public int getNumDataElements() {
                return 0;
            }

            public Object getDataElements(int x, int y, Object obj, DataBuffer data) {
                return null;
            }

            public SampleModel createSubsetSampleModel(int[] bands) {
                return null;
            }

            public DataBuffer createDataBuffer() {
                return null;
            }

            public SampleModel createCompatibleSampleModel(int w, int h) {
                return null;
            }
        });

        RenderingHints renderingHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);

        mosaicArray = new RenderedImage[0];

        // MosaicNoData operation
        RenderedImage image5 = MosaicNoDataDescriptor.create(mosaicArray, helpBean,
                DEFAULT_MOSAIC_TYPE, destinationValues, renderingHints);

        try {
            image5.getTile(0, 0);
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Sample size is not the same for every band");
        }

    }

    @Test
    public void testExceptionImagesSameBandNum() {
        initialSetup3image(true, false);

        MosaicBean testBean = beanContainer3Images[0];

        // start image array
        RenderedImage[] mosaicArray = new RenderedImage[2];

        // Creates an array for the destination band values
        Number[] destinationValues = { testBean.getDestinationNoData()[0] };

        // Updates the innerBean
        ImageMosaicBean[] helpBean = testBean.getInnerBean3Band();
        helpBean[0].setImage(mosaicArray[0]);
        helpBean[1].setImage(mosaicArray[1]);

        ImageLayout layout = (ImageLayout) hints.get(JAI.KEY_IMAGE_LAYOUT);
        RenderingHints renderingHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);

        mosaicArray[0] = getSyntheticUniformTypeImage(15, DataBuffer.TYPE_INT, 3);
        mosaicArray[1] = getSyntheticUniformTypeImage(15, DataBuffer.TYPE_INT, 1);

        // MosaicNoData operation
        RenderedImage image5 = MosaicNoDataDescriptor.create(mosaicArray, helpBean,
                DEFAULT_MOSAIC_TYPE, destinationValues, renderingHints);

        try {
            image5.getTile(0, 0);
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Bands number is not the same for every source");
        }

    }

    @Test
    public void testExceptionImagesSameSizeListAndBean() {
        initialSetup3image(true, false);

        MosaicBean testBean = beanContainer3Images[0];

        // start image array
        RenderedImage[] mosaicArray = new RenderedImage[2];

        // Creates an array for the destination band values
        Number[] destinationValues = { testBean.getDestinationNoData()[0] };

        ImageLayout layout = (ImageLayout) hints.get(JAI.KEY_IMAGE_LAYOUT);
        RenderingHints renderingHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);

        mosaicArray[0] = getSyntheticUniformTypeImage(15, DataBuffer.TYPE_INT, 1);
        mosaicArray[1] = getSyntheticUniformTypeImage(15, DataBuffer.TYPE_INT, 1);

        // Updates the innerBean
        ImageMosaicBean[] helpBean = new ImageMosaicBean[1];

        // MosaicNoData operation
        RenderedImage image5 = MosaicNoDataDescriptor.create(mosaicArray, helpBean,
                DEFAULT_MOSAIC_TYPE, destinationValues, renderingHints);

        try {
            image5.getTile(0, 0);
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Source and images must have the same length");
        }

    }

    @Test
    public void testExceptionImagesAlphaBandUnic() {
        initialSetup3image(true, true);

        MosaicBean testBean = beanContainer3Images[0];

        // start image array
        RenderedImage[] mosaicArray = testBean.getImage3Bands();

        // Creates an array for the destination band values
        Number[] destinationValues = { testBean.getDestinationNoData()[0] };

        ImageLayout layout = (ImageLayout) hints.get(JAI.KEY_IMAGE_LAYOUT);
        RenderingHints renderingHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);

        // Updates the innerBean
        ImageMosaicBean[] helpBean = testBean.getInnerBean3Band();
        PlanarImage newAlphaChannel = (PlanarImage) getSyntheticUniformTypeImage(15,
                DataBuffer.TYPE_INT, 3);
        helpBean[0].setAlphaChannel(newAlphaChannel);

        // MosaicNoData operation
        RenderedImage image5 = MosaicNoDataDescriptor.create(mosaicArray, helpBean,
                DEFAULT_MOSAIC_TYPE, destinationValues, renderingHints);

        try {
            image5.getTile(0, 0);
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Alpha bands number must be 1");
        }

    }

    @Test(expected = IllegalArgumentException.class)
    public void testExceptionImagesMapDestRectNullDest() {
        initialSetup3image(true, true);

        MosaicBean testBean = beanContainer3Images[0];

        // start image array
        RenderedImage[] mosaicArray = testBean.getImage3Bands();

        List mosaicList = new Vector();

        for (int i = 0; i < mosaicArray.length; i++) {
            mosaicList.add(mosaicArray[i]);
        }

        // Creates an array for the destination band values
        Number[] destinationValues = { testBean.getDestinationNoData()[0] };

        ImageLayout layout = (ImageLayout) hints.get(JAI.KEY_IMAGE_LAYOUT);
        RenderingHints renderingHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);

        // Updates the innerBean
        ImageMosaicBean[] helpBean = testBean.getInnerBean3Band();
        Rectangle rect = new MosaicNoDataOpImage(mosaicList, layout, renderingHints, helpBean,
                DEFAULT_MOSAIC_TYPE, destinationValues).mapDestRect(null, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExceptionImagesMapDestRectZeroIndex() {
        initialSetup3image(true, true);

        MosaicBean testBean = beanContainer3Images[0];

        // start image array
        RenderedImage[] mosaicArray = testBean.getImage3Bands();

        List mosaicList = new Vector();

        for (int i = 0; i < mosaicArray.length; i++) {
            mosaicList.add(mosaicArray[i]);
        }

        // Creates an array for the destination band values
        Number[] destinationValues = { testBean.getDestinationNoData()[0] };

        ImageLayout layout = (ImageLayout) hints.get(JAI.KEY_IMAGE_LAYOUT);
        RenderingHints renderingHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);

        // innerBean
        ImageMosaicBean[] helpBean = testBean.getInnerBean3Band();

        Rectangle testRect = new Rectangle(0, 0, 10, 10);
        Rectangle rect = new MosaicNoDataOpImage(mosaicList, layout, renderingHints, helpBean,
                DEFAULT_MOSAIC_TYPE, destinationValues).mapDestRect(testRect, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExceptionImagesMapDestRectOverIndex() {
        initialSetup3image(true, true);

        MosaicBean testBean = beanContainer3Images[0];

        // start image array
        RenderedImage[] mosaicArray = testBean.getImage3Bands();

        List mosaicList = new Vector();

        for (int i = 0; i < mosaicArray.length; i++) {
            mosaicList.add(mosaicArray[i]);
        }

        // Creates an array for the destination band values
        Number[] destinationValues = { testBean.getDestinationNoData()[0] };

        ImageLayout layout = (ImageLayout) hints.get(JAI.KEY_IMAGE_LAYOUT);
        RenderingHints renderingHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);

        // Updates the innerBean
        ImageMosaicBean[] helpBean = testBean.getInnerBean3Band();

        Rectangle testRect = new Rectangle(0, 0, 10, 10);
        Rectangle rect = new MosaicNoDataOpImage(mosaicList, layout, renderingHints, helpBean,
                DEFAULT_MOSAIC_TYPE, destinationValues).mapDestRect(testRect, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExceptionImagesMapSourceRectNullSource() {
        initialSetup3image(true, true);

        MosaicBean testBean = beanContainer3Images[0];

        // start image array
        RenderedImage[] mosaicArray = testBean.getImage3Bands();

        List mosaicList = new Vector();

        for (int i = 0; i < mosaicArray.length; i++) {
            mosaicList.add(mosaicArray[i]);
        }

        // Creates an array for the destination band values
        Number[] destinationValues = { testBean.getDestinationNoData()[0] };

        ImageLayout layout = (ImageLayout) hints.get(JAI.KEY_IMAGE_LAYOUT);
        RenderingHints renderingHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);

        // Updates the innerBean
        ImageMosaicBean[] helpBean = testBean.getInnerBean3Band();

        Rectangle rect = new MosaicNoDataOpImage(mosaicList, layout, renderingHints, helpBean,
                DEFAULT_MOSAIC_TYPE, destinationValues).mapSourceRect(null, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExceptionImagesMapSourceRectZeroIndex() {
        initialSetup3image(true, true);

        MosaicBean testBean = beanContainer3Images[0];

        // start image array
        RenderedImage[] mosaicArray = testBean.getImage3Bands();

        List mosaicList = new Vector();

        for (int i = 0; i < mosaicArray.length; i++) {
            mosaicList.add(mosaicArray[i]);
        }

        // Creates an array for the destination band values
        Number[] destinationValues = { testBean.getDestinationNoData()[0] };

        ImageLayout layout = (ImageLayout) hints.get(JAI.KEY_IMAGE_LAYOUT);
        RenderingHints renderingHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);

        // Updates the innerBean
        ImageMosaicBean[] helpBean = testBean.getInnerBean3Band();

        Rectangle testRect = new Rectangle(0, 0, 10, 10);
        Rectangle rect = new MosaicNoDataOpImage(mosaicList, layout, renderingHints, helpBean,
                DEFAULT_MOSAIC_TYPE, destinationValues).mapSourceRect(testRect, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExceptionImagesMapSourceRectOverIndex() {
        initialSetup3image(true, true);

        MosaicBean testBean = beanContainer3Images[0];

        // start image array
        RenderedImage[] mosaicArray = testBean.getImage3Bands();

        List mosaicList = new Vector();

        for (int i = 0; i < mosaicArray.length; i++) {
            mosaicList.add(mosaicArray[i]);
        }

        // Creates an array for the destination band values
        Number[] destinationValues = { testBean.getDestinationNoData()[0] };

        ImageLayout layout = (ImageLayout) hints.get(JAI.KEY_IMAGE_LAYOUT);
        RenderingHints renderingHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);

        // Updates the innerBean
        ImageMosaicBean[] helpBean = testBean.getInnerBean3Band();

        Rectangle testRect = new Rectangle(0, 0, 10, 10);
        Rectangle rect = new MosaicNoDataOpImage(mosaicList, layout, renderingHints, helpBean,
                DEFAULT_MOSAIC_TYPE, destinationValues).mapSourceRect(testRect, 10);
    }

    
    
    private void initialSetup3image(boolean nullNoData, boolean roiAlphaNotNull) {
        // check if the setup has been done. If so return.
        // creates all the image per datatype
        beanContainer3Images = new MosaicBean[6];
        // one sourceND or valid data for source(same for every band)
        Number[] sourceND = new Number[3];
        Number[] validData = new Number[3];
        // one destination ND value for band(destination is only one image)
        Number[] destinationND = new Number[3];
        // source no data values
        if (nullNoData) {
            sourceND[0] = null;
            sourceND[1] = null;
            sourceND[2] = null;
        } else {
            sourceND[0] = 50;
            sourceND[1] = 50;
            sourceND[2] = 50;

        }
        // source possible valid data
        validData[0] = 15;
        validData[1] = 65;
        validData[2] = 80;
        // destination no data
        destinationND[0] = 100;
        destinationND[1] = 100;
        destinationND[2] = 100;

        for (int i = 0; i < 6; i++) {
            switch (i) {
            case DataBuffer.TYPE_BYTE:

                // method for image creation
                beanContainer3Images[i] = imageSettings3Images(validData, sourceND, destinationND,
                        i, roiAlphaNotNull);
                break;
            case DataBuffer.TYPE_USHORT:
                // method for image creation
                beanContainer3Images[i] = imageSettings3Images(validData, sourceND, destinationND,
                        i, roiAlphaNotNull);
                break;
            case DataBuffer.TYPE_SHORT:
                if (nullNoData) {
                    sourceND[0] = null;
                    sourceND[1] = null;
                    sourceND[2] = null;
                } else {
                    sourceND[0] = 50;
                    sourceND[1] = 50;
                    sourceND[2] = 50;

                }
                // source possible valid data
                validData[0] = 15;
                validData[1] = 65;
                validData[2] = 80;
                // destination no data
                destinationND[0] = 100;
                destinationND[1] = 100;
                destinationND[2] = 100;
                // method for image creation
                beanContainer3Images[i] = imageSettings3Images(validData, sourceND, destinationND,
                        i, roiAlphaNotNull);
                break;
            case DataBuffer.TYPE_INT:
                // method for image creation
                beanContainer3Images[i] = imageSettings3Images(validData, sourceND, destinationND,
                        i, roiAlphaNotNull);
                break;
            case DataBuffer.TYPE_FLOAT:
                // method for image creation
                beanContainer3Images[i] = imageSettings3Images(validData, sourceND, destinationND,
                        i, roiAlphaNotNull);
                break;
            case DataBuffer.TYPE_DOUBLE:
                // method for image creation
                beanContainer3Images[i] = imageSettings3Images(validData, sourceND, destinationND,
                        i, roiAlphaNotNull);
                break;
            }
        }
        initialSetup3Images = true;
    }

    private MosaicBean imageSettings3Images(Number[] validData, Number[] sourceNodata,
            Number[] destinationNoDataValue, int dataType, boolean roiAlphaNotNull) {

        MosaicBean tempMosaicBean = new MosaicBean();

        RenderedImage image1 = null;
        RenderedImage image2 = null;
        RenderedImage image3 = null;

        RenderedImage image3Band1 = null;
        RenderedImage image3Band2 = null;
        RenderedImage image3Band3 = null;

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            if (sourceNodata[0] == null) {
                image1 = getSyntheticUniformTypeImage(validData[0].byteValue(), dataType, 1);
                image3Band1 = getSyntheticUniformTypeImage(validData[0].byteValue(), dataType, 3);
            } else {

                image1 = getSyntheticUniformTypeImage(sourceNodata[0].byteValue(), dataType, 1);
                image3Band1 = getSyntheticUniformTypeImage(sourceNodata[0].byteValue(), dataType, 3);
            }
            image2 = getSyntheticUniformTypeImage(validData[1].byteValue(), dataType, 1);
            image3 = getSyntheticUniformTypeImage(validData[2].byteValue(), dataType, 1);
            image3Band2 = getSyntheticUniformTypeImage(validData[1].byteValue(), dataType, 3);
            image3Band3 = getSyntheticUniformTypeImage(validData[2].byteValue(), dataType, 3);
            break;
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            if (sourceNodata[0] == null) {
                image1 = getSyntheticUniformTypeImage(validData[0].shortValue(), dataType, 1);
                image3Band1 = getSyntheticUniformTypeImage(validData[0].shortValue(), dataType, 3);
            } else {

                image1 = getSyntheticUniformTypeImage(sourceNodata[0].shortValue(), dataType, 1);
                image3Band1 = getSyntheticUniformTypeImage(sourceNodata[0].shortValue(), dataType,
                        3);
            }
            image2 = getSyntheticUniformTypeImage(validData[1].shortValue(), dataType, 1);
            image3 = getSyntheticUniformTypeImage(validData[2].shortValue(), dataType, 1);
            image3Band2 = getSyntheticUniformTypeImage(validData[1].shortValue(), dataType, 3);
            image3Band3 = getSyntheticUniformTypeImage(validData[2].shortValue(), dataType, 3);
            break;
        case DataBuffer.TYPE_INT:
            if (sourceNodata[0] == null) {
                image1 = getSyntheticUniformTypeImage(validData[0].intValue(), dataType, 1);
                image3Band1 = getSyntheticUniformTypeImage(validData[0].intValue(), dataType, 3);
            } else {

                image1 = getSyntheticUniformTypeImage(sourceNodata[0].intValue(), dataType, 1);
                image3Band1 = getSyntheticUniformTypeImage(sourceNodata[0].intValue(), dataType, 3);
            }
            image2 = getSyntheticUniformTypeImage(validData[1].intValue(), dataType, 1);
            image3 = getSyntheticUniformTypeImage(validData[2].intValue(), dataType, 1);
            image3Band2 = getSyntheticUniformTypeImage(validData[1].intValue(), dataType, 3);
            image3Band3 = getSyntheticUniformTypeImage(validData[2].intValue(), dataType, 3);

            break;
        case DataBuffer.TYPE_FLOAT:
            if (sourceNodata[0] == null) {
                image1 = getSyntheticUniformTypeImage(validData[0].floatValue(), dataType, 1);
                image3Band1 = getSyntheticUniformTypeImage(validData[0].floatValue(), dataType, 3);
            } else {

                image1 = getSyntheticUniformTypeImage(sourceNodata[0].floatValue(), dataType, 1);
                image3Band1 = getSyntheticUniformTypeImage(sourceNodata[0].floatValue(), dataType,
                        3);
            }
            image2 = getSyntheticUniformTypeImage(validData[1].floatValue(), dataType, 1);
            image3 = getSyntheticUniformTypeImage(validData[2].floatValue(), dataType, 1);
            image3Band2 = getSyntheticUniformTypeImage(validData[1].floatValue(), dataType, 3);
            image3Band3 = getSyntheticUniformTypeImage(validData[2].floatValue(), dataType, 3);
            break;
        case DataBuffer.TYPE_DOUBLE:
            if (sourceNodata[0] == null) {
                image1 = getSyntheticUniformTypeImage(validData[0].doubleValue(), dataType, 1);
                image3Band1 = getSyntheticUniformTypeImage(validData[0].doubleValue(), dataType, 3);
            } else {

                image1 = getSyntheticUniformTypeImage(sourceNodata[0].doubleValue(), dataType, 1);
                image3Band1 = getSyntheticUniformTypeImage(sourceNodata[0].doubleValue(), dataType,
                        3);
            }
            image2 = getSyntheticUniformTypeImage(validData[1].doubleValue(), dataType, 1);
            image3 = getSyntheticUniformTypeImage(validData[2].doubleValue(), dataType, 1);
            image3Band2 = getSyntheticUniformTypeImage(validData[1].doubleValue(), dataType, 3);
            image3Band3 = getSyntheticUniformTypeImage(validData[2].doubleValue(), dataType, 3);
            break;
        }

        RenderedImage[] array1Band = { image1, image2, image3 };
        RenderedImage[] array3Band = { image3Band1, image3Band2, image3Band3 };

        // new layout that double the image both in length and width
        ImageLayout layout = new ImageLayout(0, 0, array1Band[0].getWidth()
                + array1Band[1].getWidth(), array1Band[0].getHeight() + array1Band[1].getHeight());
        layout.setTileHeight(array1Band[0].getHeight() / 16);
        layout.setTileWidth(array1Band[0].getWidth() / 16);
        // create the rendering hints
        hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);

        // All the data are saved in a mosaic bean
        tempMosaicBean.setDestinationNoData(destinationNoDataValue);
        tempMosaicBean.setImage(array1Band);
        tempMosaicBean.setImage3Bands(array3Band);
        // This bean is an inner bean for the mosaicNoDataOperation
        ImageMosaicBean[] imagesBean1Band = new ImageMosaicBean[array1Band.length];
        ImageMosaicBean[] imagesBean3Band = new ImageMosaicBean[array1Band.length];

        for (int k = 0; k < array1Band.length; k++) {
            ImageMosaicBean helpBean1Band = new ImageMosaicBean();
            ImageMosaicBean helpBean3Band = new ImageMosaicBean();
            helpBean1Band.setImage(array1Band[k]);

            if (sourceNodata[k] != null) {
                switch (dataType) {
                case DataBuffer.TYPE_BYTE:
                    helpBean1Band.setSourceNoData(new Range<Byte>(sourceNodata[k].byteValue(),
                            true, sourceNodata[k].byteValue(), true));
                    helpBean3Band.setSourceNoData(new Range<Byte>(sourceNodata[k].byteValue(),
                            true, sourceNodata[k].byteValue(), true));
                    break;
                case DataBuffer.TYPE_USHORT:
                case DataBuffer.TYPE_SHORT:
                    helpBean1Band.setSourceNoData(new Range<Short>(sourceNodata[k].shortValue(),
                            true, sourceNodata[k].shortValue(), true));
                    helpBean3Band.setSourceNoData(new Range<Short>(sourceNodata[k].shortValue(),
                            true, sourceNodata[k].shortValue(), true));
                    break;
                case DataBuffer.TYPE_INT:
                    helpBean1Band.setSourceNoData(new Range<Integer>(sourceNodata[k].intValue(),
                            true, sourceNodata[k].intValue(), true));
                    helpBean3Band.setSourceNoData(new Range<Integer>(sourceNodata[k].intValue(),
                            true, sourceNodata[k].intValue(), true));
                    break;
                case DataBuffer.TYPE_FLOAT:
                    helpBean1Band.setSourceNoData(new Range<Float>(sourceNodata[k].floatValue(),
                            true, sourceNodata[k].floatValue(), true));
                    helpBean3Band.setSourceNoData(new Range<Float>(sourceNodata[k].floatValue(),
                            true, sourceNodata[k].floatValue(), true));
                    break;
                case DataBuffer.TYPE_DOUBLE:
                    helpBean1Band.setSourceNoData(new Range<Double>(sourceNodata[k].doubleValue(),
                            true, sourceNodata[k].doubleValue(), true));
                    helpBean3Band.setSourceNoData(new Range<Double>(sourceNodata[k].doubleValue(),
                            true, sourceNodata[k].doubleValue(), true));
                    break;
                }

            }

            if (roiAlphaNotNull) {

                // roi creation
                if (k == 1) {
                    int imageWidth = image1.getWidth();
                    int imageHeigth = image1.getHeight();

                    int startRoiXPixelImage1 = imageWidth / 2;
                    int startRoiYPixel = 0;

                    int roiWidth = imageWidth / 2;
                    int roiHeigth = imageHeigth;

                    // Creation of 2 ROI
                    Rectangle roiImage1 = new Rectangle(startRoiXPixelImage1, startRoiYPixel,
                            roiWidth, roiHeigth);
                    ROIShape roiData1 = new ROIShape(roiImage1);

                    // Only 1 ROI is set to the ImageMosaicBean for the second image

                    helpBean1Band.setImageRoi(roiData1);
                    helpBean3Band.setImageRoi(roiData1);

                } else {
                    helpBean1Band.setImageRoi(null);
                    helpBean3Band.setImageRoi(null);
                }

                // alpha channel creation
                if (k == 2) {
                    Number alphaChannelData = 30;
                    PlanarImage alphaChannel = null;
                    switch (dataType) {
                    case DataBuffer.TYPE_BYTE:
                        alphaChannel = (PlanarImage) getSyntheticUniformTypeImage(
                                alphaChannelData.byteValue(), dataType, 1);

                        break;
                    case DataBuffer.TYPE_USHORT:
                    case DataBuffer.TYPE_SHORT:
                        alphaChannel = (PlanarImage) getSyntheticUniformTypeImage(
                                alphaChannelData.shortValue(), dataType, 1);
                        break;
                    case DataBuffer.TYPE_INT:
                        alphaChannel = (PlanarImage) getSyntheticUniformTypeImage(
                                alphaChannelData.intValue(), dataType, 1);
                        break;
                    case DataBuffer.TYPE_FLOAT:
                        alphaChannel = (PlanarImage) getSyntheticUniformTypeImage(
                                alphaChannelData.floatValue(), dataType, 1);
                        break;
                    case DataBuffer.TYPE_DOUBLE:
                        alphaChannel = (PlanarImage) getSyntheticUniformTypeImage(
                                alphaChannelData.doubleValue(), dataType, 1);
                        break;
                    }

                    alphaChannel = TranslateDescriptor.create(alphaChannel, 0F,
                            (float) (alphaChannel.getHeight() / 2), null, hints);

                    helpBean1Band.setAlphaChannel(alphaChannel);
                    helpBean3Band.setAlphaChannel(alphaChannel);

                } else {
                    helpBean1Band.setAlphaChannel(null);
                    helpBean3Band.setAlphaChannel(null);
                }
            } else {
                helpBean1Band.setAlphaChannel(null);
                helpBean1Band.setImageRoi(null);
                helpBean3Band.setAlphaChannel(null);
                helpBean3Band.setImageRoi(null);
            }
            imagesBean1Band[k] = helpBean1Band;
            imagesBean3Band[k] = helpBean3Band;
        }
        tempMosaicBean.setInnerBean(imagesBean1Band);
        tempMosaicBean.setInnerBean3Band(imagesBean3Band);
        tempMosaicBean.setSourceValidData(validData);
        tempMosaicBean.setSourceNoData(sourceNodata);
        return tempMosaicBean;
    }

    private Number[][] testExecution3Image(MosaicBean testBean, int numBands, boolean overlay) {
        // Pre-allocated array for saving the pixel values
        Number[][] arrayPixel = new Number[3][6];

        if (numBands == 3) {
            // start image array
            RenderedImage[] sourceArray = testBean.getImage3Bands();
            // start image array
            RenderedImage[] mosaicArray = new RenderedImage[3];

            // Translation of the second image of half of its dimension
            // IMAGE ON THE UPPER RIGHT
            mosaicArray[1] = TranslateDescriptor.create(sourceArray[1],
                    (float) (sourceArray[1].getWidth() / 2), 0F, null, hints);
            // The First image is only put in the new layout
            // IMAGE ON THE UPPER LEFT
            mosaicArray[0] = TranslateDescriptor.create(sourceArray[0], 0F, 0F, null, hints);
            // IMAGE ON THE LOWER LEFT
            mosaicArray[2] = TranslateDescriptor.create(sourceArray[2], 0F,
                    (float) (sourceArray[2].getHeight() / 2), null, hints);

            // Updates the innerBean
            ImageMosaicBean[] helpBean = testBean.getInnerBean3Band();
            helpBean[0].setImage(mosaicArray[0]);
            helpBean[1].setImage(mosaicArray[1]);
            helpBean[2].setImage(mosaicArray[2]);

            // Creates an array for the destination band values
            Number[] destinationValues = testBean.getDestinationNoData();

            RenderedImage image5=null;
            if(overlay){
             // MosaicNoData operation
                image5 = MosaicNoDataDescriptor.create(mosaicArray, helpBean,
                        DEFAULT_MOSAIC_TYPE, destinationValues, hints);
            }else{
                image5 = MosaicNoDataDescriptor.create(mosaicArray, helpBean,
                        MosaicDescriptor.MOSAIC_TYPE_BLEND, destinationValues, hints);
            }
            

            int dataType = image5.getSampleModel().getDataType();

            int minXTile = image5.getMinTileX();
            int minYTile = image5.getMinTileY();
            int numXTile = image5.getNumXTiles();
            int numYTile = image5.getNumYTiles();
            int maxXTile = minXTile + (numXTile - 1);
            int maxYTile = minYTile + (numYTile - 1);

            // TILE SELECTION

            // minimun X coordinate tile selection
            int minXTileCoord = minXTile;

            // minimun Y coordinate tile selection
            int minYTileCoord = minYTile;

            // medium X coordinate between the first image and the second image
            int medXCoord12 = (maxXTile / 2) - 1;

            // medium Y coordinate between the first image and the third image
            int medYCoord13 = (maxYTile / 2) - 1;

            // medium X coordinate in the second image
            int medXCoord2 = (maxXTile / 2) + ((maxXTile) / 4 - 1);

            // medium Y coordinate in the third image
            int medYCoord3 = (maxYTile / 2) + ((maxYTile) / 4 - 1);

            // Selection of the upper left image tile
            Raster upperLeft = image5.getTile(minXTileCoord, minYTileCoord);

            // Selection of the upperCenter image tile
            Raster upperCenter = image5.getTile(medXCoord12, minYTileCoord);

            // Selection of the upperRight image tile
            Raster upperRight = image5.getTile(medXCoord2, minYTileCoord);

            // Selection of the centerLeft image tile
            Raster centerLeft = image5.getTile(minXTileCoord, medYCoord13);

            // selection of a tile in the middle of the total image
            Raster center = image5.getTile(medXCoord12, medYCoord13);

            // Selection of the lowerLeft image tile
            Raster lowerLeft = image5.getTile(minXTileCoord, medYCoord3);

            int[] arrayPixelInt = null;
            float[] arrayPixelFloat = null;
            double[] arrayPixelDouble = null;

            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_INT:
                arrayPixelInt = new int[6];

                // selection of the first pixel of the first image (no data)
                arrayPixel[0][0] = upperLeft.getPixel(upperLeft.getMinX(), upperLeft.getMinY(),
                        arrayPixelInt)[0];
                // selection of the pixel in ROI(second image) but no data(first image)
                arrayPixel[0][1] = upperCenter.getPixel(upperCenter.getMinX(),
                        upperCenter.getMinY(), arrayPixelInt)[0];
                // selection of the pixel not in ROI but in the second image
                arrayPixel[0][2] = upperRight.getPixel(upperRight.getMinX(), upperRight.getMinY(),
                        arrayPixelInt)[0];
                // selection of the pixel in alpha channel(third image) but no data(first image)
                arrayPixel[0][3] = centerLeft.getPixel(centerLeft.getMinX(), centerLeft.getMinY(),
                        arrayPixelInt)[0];
                // selection of the pixel in alpha channel(third image), in ROI(second image) but no data(first image)
                arrayPixel[0][4] = center.getPixel(center.getMinX(), center.getMinY(),
                        arrayPixelInt)[0];
                // selection of the pixel in alpha channel(third image)
                arrayPixel[0][5] = lowerLeft.getPixel(lowerLeft.getMinX(), lowerLeft.getMinY(),
                        arrayPixelInt)[0];

                // selection of the first pixel of the first image (no data)
                arrayPixel[1][0] = upperLeft.getPixel(upperLeft.getMinX(), upperLeft.getMinY(),
                        arrayPixelInt)[1];
                // selection of the pixel in ROI(second image) but no data(first image)
                arrayPixel[1][1] = upperCenter.getPixel(upperCenter.getMinX(),
                        upperCenter.getMinY(), arrayPixelInt)[1];
                // selection of the pixel not in ROI but in the second image
                arrayPixel[1][2] = upperRight.getPixel(upperRight.getMinX(), upperRight.getMinY(),
                        arrayPixelInt)[1];
                // selection of the pixel in alpha channel(third image) but no data(first image)
                arrayPixel[1][3] = centerLeft.getPixel(centerLeft.getMinX(), centerLeft.getMinY(),
                        arrayPixelInt)[1];
                // selection of the pixel in alpha channel(third image), in ROI(second image) but no data(first image)
                arrayPixel[1][4] = center.getPixel(center.getMinX(), center.getMinY(),
                        arrayPixelInt)[1];
                // selection of the pixel in alpha channel(third image)
                arrayPixel[1][5] = lowerLeft.getPixel(lowerLeft.getMinX(), lowerLeft.getMinY(),
                        arrayPixelInt)[1];

                // selection of the first pixel of the first image (no data)
                arrayPixel[2][0] = upperLeft.getPixel(upperLeft.getMinX(), upperLeft.getMinY(),
                        arrayPixelInt)[2];
                // selection of the pixel in ROI(second image) but no data(first image)
                arrayPixel[2][1] = upperCenter.getPixel(upperCenter.getMinX(),
                        upperCenter.getMinY(), arrayPixelInt)[2];
                // selection of the pixel not in ROI but in the second image
                arrayPixel[2][2] = upperRight.getPixel(upperRight.getMinX(), upperRight.getMinY(),
                        arrayPixelInt)[2];
                // selection of the pixel in alpha channel(third image) but no data(first image)
                arrayPixel[2][3] = centerLeft.getPixel(centerLeft.getMinX(), centerLeft.getMinY(),
                        arrayPixelInt)[2];
                // selection of the pixel in alpha channel(third image), in ROI(second image) but no data(first image)
                arrayPixel[2][4] = center.getPixel(center.getMinX(), center.getMinY(),
                        arrayPixelInt)[1];
                // selection of the pixel in alpha channel(third image)
                arrayPixel[2][5] = lowerLeft.getPixel(lowerLeft.getMinX(), lowerLeft.getMinY(),
                        arrayPixelInt)[2];

                break;
            case DataBuffer.TYPE_FLOAT:
                arrayPixelFloat = new float[6];

                // selection of the first pixel of the first image (no data)
                arrayPixel[0][0] = upperLeft.getPixel(upperLeft.getMinX(), upperLeft.getMinY(),
                        arrayPixelFloat)[0];
                // selection of the pixel in ROI(second image) but no data(first image)
                arrayPixel[0][1] = upperCenter.getPixel(upperCenter.getMinX(),
                        upperCenter.getMinY(), arrayPixelFloat)[0];
                // selection of the pixel not in ROI but in the second image
                arrayPixel[0][2] = upperRight.getPixel(upperRight.getMinX(), upperRight.getMinY(),
                        arrayPixelFloat)[0];
                // selection of the pixel in alpha channel(third image) but no data(first image)
                arrayPixel[0][3] = centerLeft.getPixel(centerLeft.getMinX(), centerLeft.getMinY(),
                        arrayPixelFloat)[0];
                // selection of the pixel in alpha channel(third image), in ROI(second image) but no data(first image)
                arrayPixel[0][4] = center.getPixel(center.getMinX(), center.getMinY(),
                        arrayPixelFloat)[0];
                // selection of the pixel in alpha channel(third image)
                arrayPixel[0][5] = lowerLeft.getPixel(lowerLeft.getMinX(), lowerLeft.getMinY(),
                        arrayPixelFloat)[0];

                // selection of the first pixel of the first image (no data)
                arrayPixel[1][0] = upperLeft.getPixel(upperLeft.getMinX(), upperLeft.getMinY(),
                        arrayPixelFloat)[1];
                // selection of the pixel in ROI(second image) but no data(first image)
                arrayPixel[1][1] = upperCenter.getPixel(upperCenter.getMinX(),
                        upperCenter.getMinY(), arrayPixelFloat)[1];
                // selection of the pixel not in ROI but in the second image
                arrayPixel[1][2] = upperRight.getPixel(upperRight.getMinX(), upperRight.getMinY(),
                        arrayPixelFloat)[1];
                // selection of the pixel in alpha channel(third image) but no data(first image)
                arrayPixel[1][3] = centerLeft.getPixel(centerLeft.getMinX(), centerLeft.getMinY(),
                        arrayPixelFloat)[1];
                // selection of the pixel in alpha channel(third image), in ROI(second image) but no data(first image)
                arrayPixel[1][4] = center.getPixel(center.getMinX(), center.getMinY(),
                        arrayPixelFloat)[1];
                // selection of the pixel in alpha channel(third image)
                arrayPixel[1][5] = lowerLeft.getPixel(lowerLeft.getMinX(), lowerLeft.getMinY(),
                        arrayPixelFloat)[1];

                // selection of the first pixel of the first image (no data)
                arrayPixel[2][0] = upperLeft.getPixel(upperLeft.getMinX(), upperLeft.getMinY(),
                        arrayPixelFloat)[2];
                // selection of the pixel in ROI(second image) but no data(first image)
                arrayPixel[2][1] = upperCenter.getPixel(upperCenter.getMinX(),
                        upperCenter.getMinY(), arrayPixelFloat)[2];
                // selection of the pixel not in ROI but in the second image
                arrayPixel[2][2] = upperRight.getPixel(upperRight.getMinX(), upperRight.getMinY(),
                        arrayPixelFloat)[2];
                // selection of the pixel in alpha channel(third image) but no data(first image)
                arrayPixel[2][3] = centerLeft.getPixel(centerLeft.getMinX(), centerLeft.getMinY(),
                        arrayPixelFloat)[2];
                // selection of the pixel in alpha channel(third image), in ROI(second image) but no data(first image)
                arrayPixel[2][4] = center.getPixel(center.getMinX(), center.getMinY(),
                        arrayPixelFloat)[1];
                // selection of the pixel in alpha channel(third image)
                arrayPixel[2][5] = lowerLeft.getPixel(lowerLeft.getMinX(), lowerLeft.getMinY(),
                        arrayPixelFloat)[2];
                break;
            case DataBuffer.TYPE_DOUBLE:
                arrayPixelDouble = new double[3];

                // selection of the first pixel of the first image (no data)
                arrayPixel[0][0] = upperLeft.getPixel(upperLeft.getMinX(), upperLeft.getMinY(),
                        arrayPixelDouble)[0];
                // selection of the pixel in ROI(second image) but no data(first image)
                arrayPixel[0][1] = upperCenter.getPixel(upperCenter.getMinX(),
                        upperCenter.getMinY(), arrayPixelDouble)[0];
                // selection of the pixel not in ROI but in the second image
                arrayPixel[0][2] = upperRight.getPixel(upperRight.getMinX(), upperRight.getMinY(),
                        arrayPixelDouble)[0];
                // selection of the pixel in alpha channel(third image) but no data(first image)
                arrayPixel[0][3] = centerLeft.getPixel(centerLeft.getMinX(), centerLeft.getMinY(),
                        arrayPixelDouble)[0];
                // selection of the pixel in alpha channel(third image), in ROI(second image) but no data(first image)
                arrayPixel[0][4] = center.getPixel(center.getMinX(), center.getMinY(),
                        arrayPixelDouble)[0];
                // selection of the pixel in alpha channel(third image)
                arrayPixel[0][5] = lowerLeft.getPixel(lowerLeft.getMinX(), lowerLeft.getMinY(),
                        arrayPixelDouble)[0];

                // selection of the first pixel of the first image (no data)
                arrayPixel[1][0] = upperLeft.getPixel(upperLeft.getMinX(), upperLeft.getMinY(),
                        arrayPixelDouble)[1];
                // selection of the pixel in ROI(second image) but no data(first image)
                arrayPixel[1][1] = upperCenter.getPixel(upperCenter.getMinX(),
                        upperCenter.getMinY(), arrayPixelDouble)[1];
                // selection of the pixel not in ROI but in the second image
                arrayPixel[1][2] = upperRight.getPixel(upperRight.getMinX(), upperRight.getMinY(),
                        arrayPixelDouble)[1];
                // selection of the pixel in alpha channel(third image) but no data(first image)
                arrayPixel[1][3] = centerLeft.getPixel(centerLeft.getMinX(), centerLeft.getMinY(),
                        arrayPixelDouble)[1];
                // selection of the pixel in alpha channel(third image), in ROI(second image) but no data(first image)
                arrayPixel[1][4] = center.getPixel(center.getMinX(), center.getMinY(),
                        arrayPixelDouble)[1];
                // selection of the pixel in alpha channel(third image)
                arrayPixel[1][5] = lowerLeft.getPixel(lowerLeft.getMinX(), lowerLeft.getMinY(),
                        arrayPixelDouble)[1];

                // selection of the first pixel of the first image (no data)
                arrayPixel[2][0] = upperLeft.getPixel(upperLeft.getMinX(), upperLeft.getMinY(),
                        arrayPixelDouble)[2];
                // selection of the pixel in ROI(second image) but no data(first image)
                arrayPixel[2][1] = upperCenter.getPixel(upperCenter.getMinX(),
                        upperCenter.getMinY(), arrayPixelDouble)[2];
                // selection of the pixel not in ROI but in the second image
                arrayPixel[2][2] = upperRight.getPixel(upperRight.getMinX(), upperRight.getMinY(),
                        arrayPixelDouble)[2];
                // selection of the pixel in alpha channel(third image) but no data(first image)
                arrayPixel[2][3] = centerLeft.getPixel(centerLeft.getMinX(), centerLeft.getMinY(),
                        arrayPixelDouble)[2];
                // selection of the pixel in alpha channel(third image), in ROI(second image) but no data(first image)
                arrayPixel[2][4] = center.getPixel(center.getMinX(), center.getMinY(),
                        arrayPixelDouble)[1];
                // selection of the pixel in alpha channel(third image)
                arrayPixel[2][5] = lowerLeft.getPixel(lowerLeft.getMinX(), lowerLeft.getMinY(),
                        arrayPixelDouble)[2];
                break;
            }

        } else {

            // start image array
            RenderedImage[] sourceArray = testBean.getImage();
            // start image array
            RenderedImage[] mosaicArray = new RenderedImage[3];

            // Creates an array for the destination band values
            Number[] destinationValues = { testBean.getDestinationNoData()[0] };

            // Translation of the second image of half of its dimension
            // IMAGE ON THE UPPER RIGHT
            mosaicArray[1] = TranslateDescriptor.create(sourceArray[1],
                    (float) (sourceArray[1].getWidth() / 2), 0F, null, hints);
            // The First image is only put in the new layout
            // IMAGE ON THE UPPER LEFT
            mosaicArray[0] = TranslateDescriptor.create(sourceArray[0], 0F, 0F, null, hints);
            // IMAGE ON THE LOWER LEFT
            mosaicArray[2] = TranslateDescriptor.create(sourceArray[2], 0F,
                    (float) (sourceArray[2].getHeight() / 2), null, hints);

            // Updates the innerBean
            ImageMosaicBean[] helpBean = testBean.getInnerBean3Band();
            helpBean[0].setImage(mosaicArray[0]);
            helpBean[1].setImage(mosaicArray[1]);
            helpBean[2].setImage(mosaicArray[2]);

            
            // MosaicNoData operation
            RenderedImage image5=null;
            if(overlay){
                // MosaicNoData operation
                   image5 = MosaicNoDataDescriptor.create(mosaicArray, helpBean,
                           DEFAULT_MOSAIC_TYPE, destinationValues, hints);
               }else{
                   image5 = MosaicNoDataDescriptor.create(mosaicArray, helpBean,
                           MosaicDescriptor.MOSAIC_TYPE_BLEND, destinationValues, hints);
               }
            

            int dataType = image5.getSampleModel().getDataType();

            int minXTile = image5.getMinTileX();
            int minYTile = image5.getMinTileY();
            int numXTile = image5.getNumXTiles();
            int numYTile = image5.getNumYTiles();
            int maxXTile = minXTile + (numXTile - 1);
            int maxYTile = minYTile + (numYTile - 1);

            // TILE SELECTION

            // TILE SELECTION

            // minimun X coordinate tile selection
            int minXTileCoord = minXTile;

            // minimun Y coordinate tile selection
            int minYTileCoord = minYTile;

            // medium X coordinate between the first image and the second image
            int medXCoord12 = (maxXTile / 2) - 1;

            // medium Y coordinate between the first image and the third image
            int medYCoord13 = (maxYTile / 2) - 1;

            // medium X coordinate in the second image
            int medXCoord2 = (maxXTile / 2) + ((maxXTile) / 4 - 1);

            // medium Y coordinate in the third image
            int medYCoord3 = (maxYTile / 2) + ((maxYTile) / 4 - 1);

            // Selection of the upper left image tile
            Raster upperLeft = image5.getTile(minXTileCoord, minYTileCoord);

            // Selection of the upperCenter image tile
            Raster upperCenter = image5.getTile(medXCoord12, minYTileCoord);

            // Selection of the upperRight image tile
            Raster upperRight = image5.getTile(medXCoord2, minYTileCoord);

            // Selection of the centerLeft image tile
            Raster centerLeft = image5.getTile(minXTileCoord, medYCoord13);

            // selection of a tile in the middle of the total image
            Raster center = image5.getTile(medXCoord12, medYCoord13);

            // Selection of the lowerLeft image tile
            Raster lowerLeft = image5.getTile(minXTileCoord, medYCoord3);

            int[] arrayPixelInt = null;
            float[] arrayPixelFloat = null;
            double[] arrayPixelDouble = null;

            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_INT:
                arrayPixelInt = new int[1];

                // selection of the first pixel of the first image (no data)
                arrayPixel[0][0] = upperLeft.getPixel(upperLeft.getMinX(), upperLeft.getMinY(),
                        arrayPixelInt)[0];
                // selection of the pixel in ROI(second image) but no data(first image)
                arrayPixel[0][1] = upperCenter.getPixel(upperCenter.getMinX(),
                        upperCenter.getMinY(), arrayPixelInt)[0];
                // selection of the pixel not in ROI but in the second image
                arrayPixel[0][2] = upperRight.getPixel(upperRight.getMinX(), upperRight.getMinY(),
                        arrayPixelInt)[0];
                // selection of the pixel in alpha channel(third image) but no data(first image)
                arrayPixel[0][3] = centerLeft.getPixel(centerLeft.getMinX(), centerLeft.getMinY(),
                        arrayPixelInt)[0];
                // selection of the pixel in alpha channel(third image), in ROI(second image) but no data(first image)
                arrayPixel[0][4] = center.getPixel(center.getMinX(), center.getMinY(),
                        arrayPixelInt)[0];
                // selection of the pixel in alpha channel(third image)
                arrayPixel[0][5] = lowerLeft.getPixel(lowerLeft.getMinX(), lowerLeft.getMinY(),
                        arrayPixelInt)[0];

                break;
            case DataBuffer.TYPE_FLOAT:
                arrayPixelFloat = new float[1];

                // selection of the first pixel of the first image (no data)
                arrayPixel[0][0] = upperLeft.getPixel(upperLeft.getMinX(), upperLeft.getMinY(),
                        arrayPixelFloat)[0];
                // selection of the pixel in ROI(second image) but no data(first image)
                arrayPixel[0][1] = upperCenter.getPixel(upperCenter.getMinX(),
                        upperCenter.getMinY(), arrayPixelFloat)[0];
                // selection of the pixel not in ROI but in the second image
                arrayPixel[0][2] = upperRight.getPixel(upperRight.getMinX(), upperRight.getMinY(),
                        arrayPixelFloat)[0];
                // selection of the pixel in alpha channel(third image) but no data(first image)
                arrayPixel[0][3] = centerLeft.getPixel(centerLeft.getMinX(), centerLeft.getMinY(),
                        arrayPixelFloat)[0];
                // selection of the pixel in alpha channel(third image), in ROI(second image) but no data(first image)
                arrayPixel[0][4] = center.getPixel(center.getMinX(), center.getMinY(),
                        arrayPixelFloat)[0];
                // selection of the pixel in alpha channel(third image)
                arrayPixel[0][5] = lowerLeft.getPixel(lowerLeft.getMinX(), lowerLeft.getMinY(),
                        arrayPixelFloat)[0];

                break;
            case DataBuffer.TYPE_DOUBLE:
                arrayPixelDouble = new double[1];
                // selection of the first pixel of the first image (no data)
                arrayPixel[0][0] = upperLeft.getPixel(upperLeft.getMinX(), upperLeft.getMinY(),
                        arrayPixelDouble)[0];
                // selection of the pixel in ROI(second image) but no data(first image)
                arrayPixel[0][1] = upperCenter.getPixel(upperCenter.getMinX(),
                        upperCenter.getMinY(), arrayPixelDouble)[0];
                // selection of the pixel not in ROI but in the second image
                arrayPixel[0][2] = upperRight.getPixel(upperRight.getMinX(), upperRight.getMinY(),
                        arrayPixelDouble)[0];
                // selection of the pixel in alpha channel(third image) but no data(first image)
                arrayPixel[0][3] = centerLeft.getPixel(centerLeft.getMinX(), centerLeft.getMinY(),
                        arrayPixelDouble)[0];
                // selection of the pixel in alpha channel(third image), in ROI(second image) but no data(first image)
                arrayPixel[0][4] = center.getPixel(center.getMinX(), center.getMinY(),
                        arrayPixelDouble)[0];
                // selection of the pixel in alpha channel(third image)
                arrayPixel[0][5] = lowerLeft.getPixel(lowerLeft.getMinX(), lowerLeft.getMinY(),
                        arrayPixelDouble)[0];
                break;

            }

        }

        return arrayPixel;
    }

    private Number[][] testExecutionAlpha(MosaicBean testBean, int numBands) {
        // Pre-allocated array for saving the pixel values
        Number[][] arrayPixel = new Number[3][3];

        if (numBands == 3) {
            // start image array
            RenderedImage[] sourceArray = testBean.getImage3Bands();
            // start image array
            RenderedImage[] mosaicArray = new RenderedImage[2];
            // Translation of the second image of half of its dimension
            // IMAGE ON THE RIGHT
            mosaicArray[1] = TranslateDescriptor.create(sourceArray[1],
                    (float) (sourceArray[1].getWidth() / 2), 0F, null, hints);
            // The First image is only put in the new layout
            // IMAGE ON THE LEFT
            mosaicArray[0] = TranslateDescriptor.create(sourceArray[0], 0F, 0F, null, hints);
            // Updates the innerBean
            ImageMosaicBean[] helpBean = testBean.getInnerBean3Band();
            helpBean[0].setImage(mosaicArray[0]);
            helpBean[1].setImage(mosaicArray[1]);
            // Creates an array for the destination band values
            Number[] destinationValues = testBean.getDestinationNoData();
            // MosaicNoData operation
            RenderedImage image5 = MosaicNoDataDescriptor.create(mosaicArray, helpBean,
                    DEFAULT_MOSAIC_TYPE, destinationValues, hints);

            int dataType = image5.getSampleModel().getDataType();

            int minXTile = image5.getMinTileX();
            int minYTile = image5.getMinTileY();
            int numXTile = image5.getNumXTiles();
            int maxXTile = minXTile + (numXTile - 1);

            // Selection of the first image tile
            Raster upperLeft = image5.getTile(0, 0);

            // selection of a tile in the middle of the total image
            Raster upperCenter = image5.getTile((maxXTile + 1) / 2 - 1, minYTile);

            int tileCenterWidth = upperCenter.getWidth();

            // selection of the first pixel of the tile
            int minXCoordCenter = upperCenter.getMinX() + (tileCenterWidth / 2);
            int minYCoordCenter = upperCenter.getMinY();

            // selection of the last tile of the second image
            Raster upperRightSecondImage = image5.getTile((maxXTile + 1) / 2 - 1 + (maxXTile + 1)
                    / 4, minYTile);

            int tileRightSecondImWidth = upperRightSecondImage.getWidth();

            // selection of the last pixel of the tile
            int lastXCoordRightSecondIm = upperRightSecondImage.getMinX() + tileRightSecondImWidth
                    - 1;
            int minYCoordRightSecondIm = upperRightSecondImage.getMinY();

            int[] arrayPixelInt = null;
            float[] arrayPixelFloat = null;
            double[] arrayPixelDouble = null;

            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_INT:
                arrayPixelInt = new int[3];

                // selection of the first pixel of the image
                arrayPixel[0][0] = upperLeft.getPixel(0, 0, arrayPixelInt)[0];
                // selection of the pixel in the second ROI but not in the first ROI
                arrayPixel[0][1] = upperCenter.getPixel(minXCoordCenter, minYCoordCenter,
                        arrayPixelInt)[0];
                // selection of the last pixel
                arrayPixel[0][2] = upperRightSecondImage.getPixel(lastXCoordRightSecondIm,
                        minYCoordRightSecondIm, arrayPixelInt)[0];

                arrayPixel[1][0] = upperLeft.getPixel(0, 0, arrayPixelInt)[1];
                // selection of the pixel in the second ROI but not in the first ROI
                arrayPixel[1][1] = upperCenter.getPixel(minXCoordCenter, minYCoordCenter,
                        arrayPixelInt)[1];
                // selection of the last pixel
                arrayPixel[1][2] = upperRightSecondImage.getPixel(lastXCoordRightSecondIm,
                        minYCoordRightSecondIm, arrayPixelInt)[1];

                arrayPixel[2][0] = upperLeft.getPixel(0, 0, arrayPixelInt)[2];
                // selection of the pixel in the second ROI but not in the first ROI
                arrayPixel[2][1] = upperCenter.getPixel(minXCoordCenter, minYCoordCenter,
                        arrayPixelInt)[2];
                // selection of the last pixel
                arrayPixel[2][2] = upperRightSecondImage.getPixel(lastXCoordRightSecondIm,
                        minYCoordRightSecondIm, arrayPixelInt)[2];

                break;
            case DataBuffer.TYPE_FLOAT:
                arrayPixelFloat = new float[3];

                // selection of the first pixel of the image
                arrayPixel[0][0] = upperLeft.getPixel(0, 0, arrayPixelInt)[0];
                // selection of the pixel in the second ROI but not in the first ROI
                arrayPixel[0][1] = upperCenter.getPixel(minXCoordCenter, minYCoordCenter,
                        arrayPixelFloat)[0];
                // selection of the last pixel
                arrayPixel[0][2] = upperRightSecondImage.getPixel(lastXCoordRightSecondIm,
                        minYCoordRightSecondIm, arrayPixelFloat)[0];

                // selection of the first pixel of the image
                arrayPixel[1][0] = upperLeft.getPixel(0, 0, arrayPixelInt)[1];
                // selection of the pixel in the second ROI but not in the first ROI
                arrayPixel[1][1] = upperCenter.getPixel(minXCoordCenter, minYCoordCenter,
                        arrayPixelFloat)[1];
                // selection of the last pixel
                arrayPixel[1][2] = upperRightSecondImage.getPixel(lastXCoordRightSecondIm,
                        minYCoordRightSecondIm, arrayPixelFloat)[1];

                // selection of the first pixel of the image
                arrayPixel[2][0] = upperLeft.getPixel(0, 0, arrayPixelInt)[2];
                // selection of the pixel in the second ROI but not in the first ROI
                arrayPixel[2][1] = upperCenter.getPixel(minXCoordCenter, minYCoordCenter,
                        arrayPixelFloat)[2];
                // selection of the last pixel
                arrayPixel[2][2] = upperRightSecondImage.getPixel(lastXCoordRightSecondIm,
                        minYCoordRightSecondIm, arrayPixelFloat)[2];
                break;
            case DataBuffer.TYPE_DOUBLE:
                arrayPixelDouble = new double[3];

                // selection of the first pixel of the image
                arrayPixel[0][0] = upperLeft.getPixel(0, 0, arrayPixelInt)[0];
                // selection of the pixel in the second ROI but not in the first ROI
                arrayPixel[0][1] = upperCenter.getPixel(minXCoordCenter, minYCoordCenter,
                        arrayPixelDouble)[0];
                // selection of the last pixel
                arrayPixel[0][2] = upperRightSecondImage.getPixel(lastXCoordRightSecondIm,
                        minYCoordRightSecondIm, arrayPixelDouble)[0];

                // selection of the first pixel of the image
                arrayPixel[1][0] = upperLeft.getPixel(0, 0, arrayPixelInt)[1];
                // selection of the pixel in the second ROI but not in the first ROI
                arrayPixel[1][1] = upperCenter.getPixel(minXCoordCenter, minYCoordCenter,
                        arrayPixelDouble)[1];
                // selection of the last pixel
                arrayPixel[1][2] = upperRightSecondImage.getPixel(lastXCoordRightSecondIm,
                        minYCoordRightSecondIm, arrayPixelDouble)[1];

                // selection of the first pixel of the image
                arrayPixel[2][0] = upperLeft.getPixel(0, 0, arrayPixelInt)[2];
                // selection of the pixel in the second ROI but not in the first ROI
                arrayPixel[2][1] = upperCenter.getPixel(minXCoordCenter, minYCoordCenter,
                        arrayPixelDouble)[2];
                // selection of the last pixel
                arrayPixel[2][2] = upperRightSecondImage.getPixel(lastXCoordRightSecondIm,
                        minYCoordRightSecondIm, arrayPixelDouble)[2];
                break;
            }

        } else {
            // start image array
            RenderedImage[] sourceArray = testBean.getImage();
            // start image array
            RenderedImage[] mosaicArray = new RenderedImage[2];
            // Translation of the second image of half of its dimension
            // IMAGE ON THE RIGHT
            mosaicArray[1] = TranslateDescriptor.create(sourceArray[1],
                    (float) (sourceArray[1].getWidth() / 2), 0F, null, hints);
            // The First image is only put in the new layout
            // IMAGE ON THE LEFT
            mosaicArray[0] = TranslateDescriptor.create(sourceArray[0], 0F, 0F, null, hints);
            // Updates the innerBean
            ImageMosaicBean[] helpBean = testBean.getInnerBean();
            helpBean[0].setImage(mosaicArray[0]);
            helpBean[1].setImage(mosaicArray[1]);
            // Creates an array for the destination band values
            Number[] destinationValues = { testBean.getDestinationNoData()[0] };
            // MosaicNoData operation
            RenderedImage image5 = MosaicNoDataDescriptor.create(mosaicArray, helpBean,
                    DEFAULT_MOSAIC_TYPE, destinationValues, hints);

            int dataType = image5.getSampleModel().getDataType();

            int minXTile = image5.getMinTileX();
            int minYTile = image5.getMinTileY();
            int numXTile = image5.getNumXTiles();
            int maxXTile = minXTile + (numXTile - 1);

            // Selection of the first image tile
            Raster upperLeft = image5.getTile(0, 0);

            // selection of a tile in the middle of the total image
            Raster upperCenter = image5.getTile((maxXTile + 1) / 2 - 1, minYTile);

            int tileCenterWidth = upperCenter.getWidth();

            // selection of the first pixel of the tile
            int minXCoordCenter = upperCenter.getMinX() + (tileCenterWidth / 2);
            int minYCoordCenter = upperCenter.getMinY();

            // selection of the center tile
            Raster upperRightSecondImage = image5.getTile((maxXTile + 1) / 2 - 1 + (maxXTile + 1)
                    / 4, minYTile);

            int tileRightSecondImWidth = upperRightSecondImage.getWidth();

            // selection of the last pixel of the tile
            int lastXCoordRightSecondIm = upperRightSecondImage.getMinX() + tileRightSecondImWidth
                    - 1;
            int minYCoordRightSecondIm = upperRightSecondImage.getMinY();

            int[] arrayPixelInt = null;
            float[] arrayPixelFloat = null;
            double[] arrayPixelDouble = null;

            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_INT:
                arrayPixelInt = new int[1];
                // selection of the first pixel of the image
                arrayPixel[0][0] = upperLeft.getPixel(0, 0, arrayPixelInt)[0];
                // selection of the pixel in the second ROI but not in the first ROI
                arrayPixel[0][1] = upperCenter.getPixel(minXCoordCenter, minYCoordCenter,
                        arrayPixelInt)[0];
                // selection of the last pixel
                arrayPixel[0][2] = upperRightSecondImage.getPixel(lastXCoordRightSecondIm,
                        minYCoordRightSecondIm, arrayPixelInt)[0];

                break;
            case DataBuffer.TYPE_FLOAT:
                arrayPixelFloat = new float[1];

                // selection of the first pixel of the image
                arrayPixel[0][0] = upperLeft.getPixel(0, 0, arrayPixelInt)[0];
                // selection of the pixel in the second ROI but not in the first ROI
                arrayPixel[0][1] = upperCenter.getPixel(minXCoordCenter, minYCoordCenter,
                        arrayPixelFloat)[0];
                // selection of the last pixel
                arrayPixel[0][2] = upperRightSecondImage.getPixel(lastXCoordRightSecondIm,
                        minYCoordRightSecondIm, arrayPixelFloat)[0];

            case DataBuffer.TYPE_DOUBLE:
                arrayPixelDouble = new double[1];

                // selection of the first pixel of the image
                arrayPixel[0][0] = upperLeft.getPixel(0, 0, arrayPixelInt)[0];
                // selection of the pixel in the second ROI but not in the first ROI
                arrayPixel[0][1] = upperCenter.getPixel(minXCoordCenter, minYCoordCenter,
                        arrayPixelDouble)[0];
                // selection of the last pixel
                arrayPixel[0][2] = upperRightSecondImage.getPixel(lastXCoordRightSecondIm,
                        minYCoordRightSecondIm, arrayPixelDouble)[0];

            }

        }

        return arrayPixel;
    }

    private Number[] testExecution(MosaicBean testBean, int numBands) {
        // Pre-allocated array for saving the pixel values
        Number[] arrayPixel = new Number[3];

        if (numBands == 3) {
            // start image array
            RenderedImage[] sourceArray = testBean.getImage3Bands();
            // start image array
            RenderedImage[] mosaicArray = new RenderedImage[2];
            // Translation of the second image of half of its dimension
            // IMAGE ON THE RIGHT
            mosaicArray[1] = TranslateDescriptor.create(sourceArray[1],
                    (float) (sourceArray[1].getWidth() / 2), 0F, null, hints);
            // The First image is only put in the new layout
            // IMAGE ON THE LEFT
            mosaicArray[0] = TranslateDescriptor.create(sourceArray[0], 0F, 0F, null, hints);
            // Updates the innerBean
            ImageMosaicBean[] helpBean = testBean.getInnerBean3Band();
            helpBean[0].setImage(mosaicArray[0]);
            helpBean[1].setImage(mosaicArray[1]);
            // Creates an array for the destination band values
            Number[] destinationValues = testBean.getDestinationNoData();
            // MosaicNoData operation
            RenderedImage image5 = MosaicNoDataDescriptor.create(mosaicArray, helpBean,
                    DEFAULT_MOSAIC_TYPE, destinationValues, hints);

            int minXTile = image5.getMinTileX();
            int minYTile = image5.getMinTileY();
            int numXTile = image5.getNumXTiles();
            int maxXTile = minXTile + (numXTile - 1);

            Raster upperCenter = image5.getTile(maxXTile / 2, minYTile);
            // Raster pixel coordinates
            int minXCoord = upperCenter.getMinX();
            int minYCoord = upperCenter.getMinY();
            // Raster dimension in pixel
            int numXPixel = upperCenter.getWidth();
            // Raster last pixel coordinates

            int mediumXPixelCoord = minXCoord + (numXPixel / 2);
            int mediumYPixelCoord = minYCoord;

            int[] arrayPixelInt = null;
            float[] arrayPixelFloat = null;
            double[] arrayPixelDouble = null;

            int dataType = image5.getSampleModel().getDataType();

            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_INT:
                arrayPixelInt = new int[3];
                arrayPixel[0] = upperCenter.getPixel(mediumXPixelCoord, mediumYPixelCoord,
                        arrayPixelInt)[0];
                arrayPixel[1] = upperCenter.getPixel(mediumXPixelCoord, mediumYPixelCoord,
                        arrayPixelInt)[1];
                arrayPixel[2] = upperCenter.getPixel(mediumXPixelCoord, mediumYPixelCoord,
                        arrayPixelInt)[2];
                break;
            case DataBuffer.TYPE_FLOAT:
                arrayPixelFloat = new float[3];
                arrayPixel[0] = upperCenter.getPixel(mediumXPixelCoord, mediumYPixelCoord,
                        arrayPixelFloat)[0];
                arrayPixel[1] = upperCenter.getPixel(mediumXPixelCoord, mediumYPixelCoord,
                        arrayPixelFloat)[1];
                arrayPixel[2] = upperCenter.getPixel(mediumXPixelCoord, mediumYPixelCoord,
                        arrayPixelFloat)[2];

            case DataBuffer.TYPE_DOUBLE:
                arrayPixelDouble = new double[3];
                arrayPixel[0] = upperCenter.getPixel(mediumXPixelCoord, mediumYPixelCoord,
                        arrayPixelDouble)[0];
                arrayPixel[1] = upperCenter.getPixel(mediumXPixelCoord, mediumYPixelCoord,
                        arrayPixelDouble)[1];
                arrayPixel[2] = upperCenter.getPixel(mediumXPixelCoord, mediumYPixelCoord,
                        arrayPixelDouble)[2];

            }
        } else {
            // start image array
            RenderedImage[] sourceArray = testBean.getImage();
            // start image array
            RenderedImage[] mosaicArray = new RenderedImage[2];
            // Translation of the second image of half of its dimension
            // IMAGE ON THE RIGHT
            mosaicArray[1] = TranslateDescriptor.create(sourceArray[1],
                    (float) (sourceArray[1].getWidth() / 2), 0F, null, hints);
            // The First image is only put in the new layout
            // IMAGE ON THE LEFT
            mosaicArray[0] = TranslateDescriptor.create(sourceArray[0], 0F, 0F, null, hints);
            // Updates the innerBean
            ImageMosaicBean[] helpBean = testBean.getInnerBean();
            helpBean[0].setImage(mosaicArray[0]);
            helpBean[1].setImage(mosaicArray[1]);
            // Creates an array for the destination band values
            Number[] destinationValues = { testBean.getDestinationNoData()[0] };
            // MosaicNoData operation
            RenderedImage image5 = MosaicNoDataDescriptor.create(mosaicArray, helpBean,
                    DEFAULT_MOSAIC_TYPE, destinationValues, hints);

            int minXTile = image5.getMinTileX();
            int minYTile = image5.getMinTileY();
            int numXTile = image5.getNumXTiles();
            int maxXTile = minXTile + (numXTile - 1);

            Raster upperCenter = image5.getTile(maxXTile / 2, minYTile);
            // Raster pixel coordinates
            int minXCoord = upperCenter.getMinX();
            int minYCoord = upperCenter.getMinY();
            // Raster dimension in pixel
            int numXPixel = upperCenter.getWidth();
            // Raster last pixel coordinates

            int mediumXPixelCoord = minXCoord + (numXPixel / 2);
            int mediumYPixelCoord = minYCoord;

            int[] arrayPixelInt = null;
            float[] arrayPixelFloat = null;
            double[] arrayPixelDouble = null;

            int dataType = image5.getSampleModel().getDataType();

            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_INT:
                arrayPixelInt = new int[1];
                arrayPixel[0] = upperCenter.getPixel(mediumXPixelCoord, mediumYPixelCoord,
                        arrayPixelInt)[0];
                break;
            case DataBuffer.TYPE_FLOAT:
                arrayPixelFloat = new float[1];
                arrayPixel[0] = upperCenter.getPixel(mediumXPixelCoord, mediumYPixelCoord,
                        arrayPixelFloat)[0];

            case DataBuffer.TYPE_DOUBLE:
                arrayPixelDouble = new double[1];
                arrayPixel[0] = upperCenter.getPixel(mediumXPixelCoord, mediumYPixelCoord,
                        arrayPixelDouble)[0];

            }

        }

        return arrayPixel;
    }

    private Number[][] testExecutionROI(MosaicBean testBean, int numBands) {
        // Pre-allocated array for saving the pixel values
        Number[][] arrayPixel = new Number[3][3];

        if (numBands == 3) {
            // start image array
            RenderedImage[] sourceArray = testBean.getImage3Bands();
            // start image array
            RenderedImage[] mosaicArray = new RenderedImage[2];
            // Translation of the second image of half of its dimension
            // IMAGE ON THE RIGHT
            mosaicArray[1] = TranslateDescriptor.create(sourceArray[1],
                    (float) (sourceArray[1].getWidth() / 2), 0F, null, hints);
            // The First image is only put in the new layout
            // IMAGE ON THE LEFT
            mosaicArray[0] = TranslateDescriptor.create(sourceArray[0], 0F, 0F, null, hints);
            // Updates the innerBean
            ImageMosaicBean[] helpBean = testBean.getInnerBean3Band();
            helpBean[0].setImage(mosaicArray[0]);
            helpBean[1].setImage(mosaicArray[1]);
            // Creates an array for the destination band values
            Number[] destinationValues = testBean.getDestinationNoData();
            // MosaicNoData operation
            RenderedImage image5 = MosaicNoDataDescriptor.create(mosaicArray, helpBean,
                    DEFAULT_MOSAIC_TYPE, destinationValues, hints);

            int dataType = image5.getSampleModel().getDataType();

            int minXTile = image5.getMinTileX();
            int minYTile = image5.getMinTileY();
            int numXTile = image5.getNumXTiles();
            int maxXTile = minXTile + (numXTile - 1);

            // Selection of the first image tile
            Raster upperLeft = image5.getTile(0, 0);
            // selection of a tile in the middle of the first image
            Raster upperFirstCenter = image5.getTile((maxXTile / 4) + 1, minYTile);
            // selection of the first pixel of the tile
            int minXCoordFirstCenter = upperFirstCenter.getMinX();
            int minYCoordFirstCenter = upperFirstCenter.getMinY();

            // selection of the center tile
            Raster upperCenter = image5.getTile(((maxXTile + 1) / 2) - 1, minYTile);

            int tileWidth = upperCenter.getWidth();

            // selection of the first pixel of the tile
            int minXCoordCenter = upperCenter.getMinX() + tileWidth - 1;
            int minYCoordCenter = upperCenter.getMinY();

            int[] arrayPixelInt = null;
            float[] arrayPixelFloat = null;
            double[] arrayPixelDouble = null;

            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_INT:
                arrayPixelInt = new int[3];

                // selection of the first pixel of the image
                arrayPixel[0][0] = upperLeft.getPixel(0, 0, arrayPixelInt)[0];
                // selection of the pixel in the second ROI but not in the first ROI
                arrayPixel[0][1] = upperFirstCenter.getPixel(minXCoordFirstCenter,
                        minYCoordFirstCenter, arrayPixelInt)[0];
                // selection of the pixel in both the 2 ROIs
                arrayPixel[0][2] = upperCenter.getPixel(minXCoordCenter, minYCoordCenter,
                        arrayPixelInt)[0];

                arrayPixel[1][0] = upperLeft.getPixel(0, 0, arrayPixelInt)[1];
                // selection of the pixel in the second ROI but not in the first ROI
                arrayPixel[1][1] = upperFirstCenter.getPixel(minXCoordFirstCenter,
                        minYCoordFirstCenter, arrayPixelInt)[1];
                // selection of the pixel in both the 2 ROIs
                arrayPixel[1][2] = upperCenter.getPixel(minXCoordCenter, minYCoordCenter,
                        arrayPixelInt)[1];

                arrayPixel[2][0] = upperLeft.getPixel(0, 0, arrayPixelInt)[2];
                // selection of the pixel in the second ROI but not in the first ROI
                arrayPixel[2][1] = upperFirstCenter.getPixel(minXCoordFirstCenter,
                        minYCoordFirstCenter, arrayPixelInt)[2];
                // selection of the pixel in both the 2 ROIs
                arrayPixel[2][2] = upperCenter.getPixel(minXCoordCenter, minYCoordCenter,
                        arrayPixelInt)[2];

                break;
            case DataBuffer.TYPE_FLOAT:
                arrayPixelFloat = new float[3];

                // selection of the first pixel of the image
                arrayPixel[0][0] = upperLeft.getPixel(0, 0, arrayPixelInt)[0];
                // selection of the pixel in the second ROI but not in the first ROI
                arrayPixel[0][1] = upperFirstCenter.getPixel(minXCoordFirstCenter,
                        minYCoordFirstCenter, arrayPixelFloat)[0];
                // selection of the pixel in both the 2 ROIs
                arrayPixel[0][2] = upperCenter.getPixel(minXCoordCenter, minYCoordCenter,
                        arrayPixelFloat)[0];

                // selection of the first pixel of the image
                arrayPixel[1][0] = upperLeft.getPixel(0, 0, arrayPixelInt)[1];
                // selection of the pixel in the second ROI but not in the first ROI
                arrayPixel[1][1] = upperFirstCenter.getPixel(minXCoordFirstCenter,
                        minYCoordFirstCenter, arrayPixelFloat)[1];
                // selection of the pixel in both the 2 ROIs
                arrayPixel[1][2] = upperCenter.getPixel(minXCoordCenter, minYCoordCenter,
                        arrayPixelFloat)[1];

                // selection of the first pixel of the image
                arrayPixel[2][0] = upperLeft.getPixel(0, 0, arrayPixelInt)[2];
                // selection of the pixel in the second ROI but not in the first ROI
                arrayPixel[2][1] = upperFirstCenter.getPixel(minXCoordFirstCenter,
                        minYCoordFirstCenter, arrayPixelFloat)[2];
                // selection of the pixel in both the 2 ROIs
                arrayPixel[2][2] = upperCenter.getPixel(minXCoordCenter, minYCoordCenter,
                        arrayPixelFloat)[2];
                break;
            case DataBuffer.TYPE_DOUBLE:
                arrayPixelDouble = new double[3];

                // selection of the first pixel of the image
                arrayPixel[0][0] = upperLeft.getPixel(0, 0, arrayPixelInt)[0];
                // selection of the pixel in the second ROI but not in the first ROI
                arrayPixel[0][1] = upperFirstCenter.getPixel(minXCoordFirstCenter,
                        minYCoordFirstCenter, arrayPixelDouble)[0];
                // selection of the pixel in both the 2 ROIs
                arrayPixel[0][2] = upperCenter.getPixel(minXCoordCenter, minYCoordCenter,
                        arrayPixelDouble)[0];

                // selection of the first pixel of the image
                arrayPixel[1][0] = upperLeft.getPixel(0, 0, arrayPixelInt)[1];
                // selection of the pixel in the second ROI but not in the first ROI
                arrayPixel[1][1] = upperFirstCenter.getPixel(minXCoordFirstCenter,
                        minYCoordFirstCenter, arrayPixelDouble)[1];
                // selection of the pixel in both the 2 ROIs
                arrayPixel[1][2] = upperCenter.getPixel(minXCoordCenter, minYCoordCenter,
                        arrayPixelDouble)[1];

                // selection of the first pixel of the image
                arrayPixel[2][0] = upperLeft.getPixel(0, 0, arrayPixelInt)[2];
                // selection of the pixel in the second ROI but not in the first ROI
                arrayPixel[2][1] = upperFirstCenter.getPixel(minXCoordFirstCenter,
                        minYCoordFirstCenter, arrayPixelDouble)[2];
                // selection of the pixel in both the 2 ROIs
                arrayPixel[2][2] = upperCenter.getPixel(minXCoordCenter, minYCoordCenter,
                        arrayPixelDouble)[2];
                break;
            }

        } else {
            // start image array
            RenderedImage[] sourceArray = testBean.getImage();
            // start image array
            RenderedImage[] mosaicArray = new RenderedImage[2];
            // Translation of the second image of half of its dimension
            // IMAGE ON THE RIGHT
            mosaicArray[1] = TranslateDescriptor.create(sourceArray[1],
                    (float) (sourceArray[1].getWidth() / 2), 0F, null, hints);
            // The First image is only put in the new layout
            // IMAGE ON THE LEFT
            mosaicArray[0] = TranslateDescriptor.create(sourceArray[0], 0F, 0F, null, hints);
            // Updates the innerBean
            ImageMosaicBean[] helpBean = testBean.getInnerBean();
            helpBean[0].setImage(mosaicArray[0]);
            helpBean[1].setImage(mosaicArray[1]);
            // Creates an array for the destination band values
            Number[] destinationValues = { testBean.getDestinationNoData()[0] };
            // MosaicNoData operation
            RenderedImage image5 = MosaicNoDataDescriptor.create(mosaicArray, helpBean,
                    DEFAULT_MOSAIC_TYPE, destinationValues, hints);

            int dataType = image5.getSampleModel().getDataType();

            int minXTile = image5.getMinTileX();
            int minYTile = image5.getMinTileY();
            int numXTile = image5.getNumXTiles();
            int maxXTile = minXTile + (numXTile - 1);

            // Selection of the first image tile
            Raster upperLeft = image5.getTile(0, 0);
            // selection of a tile in the middle of the first image
            Raster upperFirstCenter = image5.getTile((maxXTile / 4) + 1, minYTile);
            // selection of the first pixel of the tile
            int minXCoordFirstCenter = upperFirstCenter.getMinX();
            int minYCoordFirstCenter = upperFirstCenter.getMinY();
            // selection of the center tile
            Raster upperCenter = image5.getTile(((maxXTile + 1) / 2) - 1, minYTile);
            // selection of the first pixel of the tile
            int tileWidth = upperCenter.getWidth();

            int minXCoordCenter = upperCenter.getMinX() + tileWidth - 1;
            int minYCoordCenter = upperCenter.getMinY();

            int[] arrayPixelInt = null;
            float[] arrayPixelFloat = null;
            double[] arrayPixelDouble = null;

            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_INT:
                arrayPixelInt = new int[1];
                // selection of the first pixel of the image
                arrayPixel[0][0] = upperLeft.getPixel(0, 0, arrayPixelInt)[0];
                // selection of the pixel in the second ROI but not in the first ROI
                arrayPixel[0][1] = upperFirstCenter.getPixel(minXCoordFirstCenter,
                        minYCoordFirstCenter, arrayPixelInt)[0];
                // selection of the pixel in both the 2 ROIs
                arrayPixel[0][2] = upperCenter.getPixel(minXCoordCenter, minYCoordCenter,
                        arrayPixelInt)[0];

                break;
            case DataBuffer.TYPE_FLOAT:
                arrayPixelFloat = new float[1];

                // selection of the first pixel of the image
                arrayPixel[0][0] = upperLeft.getPixel(0, 0, arrayPixelInt)[0];
                // selection of the pixel in the second ROI but not in the first ROI
                arrayPixel[0][1] = upperFirstCenter.getPixel(minXCoordFirstCenter,
                        minYCoordFirstCenter, arrayPixelFloat)[0];
                // selection of the pixel in both the 2 ROIs
                arrayPixel[0][2] = upperCenter.getPixel(minXCoordCenter, minYCoordCenter,
                        arrayPixelFloat)[0];

            case DataBuffer.TYPE_DOUBLE:
                arrayPixelDouble = new double[1];

                // selection of the first pixel of the image
                arrayPixel[0][0] = upperLeft.getPixel(0, 0, arrayPixelInt)[0];
                // selection of the pixel in the second ROI but not in the first ROI
                arrayPixel[0][1] = upperFirstCenter.getPixel(minXCoordFirstCenter,
                        minYCoordFirstCenter, arrayPixelDouble)[0];
                // selection of the pixel in both the 2 ROIs
                arrayPixel[0][2] = upperCenter.getPixel(minXCoordCenter, minYCoordCenter,
                        arrayPixelDouble)[0];

            }

        }

        return arrayPixel;
    }

    private void creationAlpha() {
        if (initialAlphaCreation) {
            return;
        }

        Number[] alphaChannelData = new Number[2];
        alphaChannelData[0] = 30;
        alphaChannelData[1] = 70;

        PlanarImage alphaChannel0 = null;
        PlanarImage alphaChannel1 = null;
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 3; j++) {

                int dataType = i;
                switch (dataType) {
                case DataBuffer.TYPE_BYTE:
                    alphaChannel0 = (PlanarImage) getSyntheticUniformTypeImage(
                            alphaChannelData[0].byteValue(), dataType, 1);
                    alphaChannel1 = (PlanarImage) getSyntheticUniformTypeImage(
                            alphaChannelData[1].byteValue(), dataType, 1);
                    break;
                case DataBuffer.TYPE_USHORT:
                    alphaChannel0 = (PlanarImage) getSyntheticUniformTypeImage(
                            alphaChannelData[0].shortValue(), dataType, 1);
                    alphaChannel1 = (PlanarImage) getSyntheticUniformTypeImage(
                            alphaChannelData[1].shortValue(), dataType, 1);
                    break;
                case DataBuffer.TYPE_SHORT:
                    alphaChannel0 = (PlanarImage) getSyntheticUniformTypeImage(
                            alphaChannelData[0].shortValue(), dataType, 1);
                    alphaChannel1 = (PlanarImage) getSyntheticUniformTypeImage(
                            alphaChannelData[1].shortValue(), dataType, 1);
                    break;
                case DataBuffer.TYPE_INT:
                    alphaChannel0 = (PlanarImage) getSyntheticUniformTypeImage(
                            alphaChannelData[0].intValue(), dataType, 1);
                    alphaChannel1 = (PlanarImage) getSyntheticUniformTypeImage(
                            alphaChannelData[1].intValue(), dataType, 1);
                    break;
                case DataBuffer.TYPE_FLOAT:
                    alphaChannel0 = (PlanarImage) getSyntheticUniformTypeImage(
                            alphaChannelData[0].floatValue(), dataType, 1);
                    alphaChannel1 = (PlanarImage) getSyntheticUniformTypeImage(
                            alphaChannelData[1].floatValue(), dataType, 1);
                    break;
                case DataBuffer.TYPE_DOUBLE:
                    alphaChannel0 = (PlanarImage) getSyntheticUniformTypeImage(
                            alphaChannelData[0].doubleValue(), dataType, 1);
                    alphaChannel1 = (PlanarImage) getSyntheticUniformTypeImage(
                            alphaChannelData[1].doubleValue(), dataType, 1);
                    break;
                }

                alphaChannel1 = TranslateDescriptor.create(alphaChannel1,
                        (float) (alphaChannel1.getWidth() / 2), 0F, null, hints);

                ImageMosaicBean[] beanToModify = beanContainer[i][j].getInnerBean();
                ImageMosaicBean[] beanToModify3Band = beanContainer[i][j].getInnerBean3Band();
                // The 2 ROIs are set to the ImageMosaicBean
                beanToModify[0].setAlphaChannel(alphaChannel0);
                beanToModify[1].setAlphaChannel(alphaChannel1);
                beanToModify3Band[0].setAlphaChannel(alphaChannel0);
                beanToModify3Band[1].setAlphaChannel(alphaChannel1);

                beanContainer[i][j].setSourceAlphaChannel(alphaChannelData);

                // beanContainer[i][j].setInnerBean(beanToModify);
                // beanContainer[i][j].setInnerBean3Band(beanToModify3Band);

            }
        }

    }

    private void creationROI() {
        if (initialROICreation) {
            return;
        }

        int imageWidth = beanContainer[0][0].getImage()[0].getWidth();

        int startRoiXPixelImage1 = imageWidth - 2;
        int startRoiYPixel = 0;

        int roiWidth = imageWidth;
        int roiHeigth = 50;

        // Creation of 2 ROI
        Rectangle roiImage1 = new Rectangle(startRoiXPixelImage1, startRoiYPixel, roiWidth,
                roiHeigth);
        ROIShape roiData1 = new ROIShape(roiImage1);

        // the second ROI is translated towards the X direction
        int startRoiXPixelImage2 = imageWidth / 2;
        Rectangle roiImage2 = new Rectangle(startRoiXPixelImage2, startRoiYPixel, roiWidth,
                roiHeigth);
        ROIShape roiData2 = new ROIShape(roiImage2);

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 3; j++) {
                ImageMosaicBean[] beanToModify = beanContainer[i][j].getInnerBean();
                ImageMosaicBean[] beanToModify3Band = beanContainer[i][j].getInnerBean3Band();
                // The 2 ROIs are set to the ImageMosaicBean
                beanToModify[0].setImageRoi(roiData1);
                beanToModify[1].setImageRoi(roiData2);
                beanToModify3Band[0].setImageRoi(roiData1);
                beanToModify3Band[1].setImageRoi(roiData2);

                // beanContainer[i][j].setInnerBean(beanToModify);
                // beanContainer[i][j].setInnerBean3Band(beanToModify3Band);

            }
        }
    }

    private void initalSetup() {
        // check if the setup has been done. If so return.
        if (initialSetup) {
            return;
        }
        // creates all the image per datatype and with the
        // combination(nodata-nodata,data-nodata,data-data)
        beanContainer = new MosaicBean[6][3];
        Number[] sourceND = new Number[2];
        Number[] validData = new Number[2];
        Number[] destinationND = new Number[3];
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 3; j++) {
                switch (i) {
                case DataBuffer.TYPE_BYTE:
                    // source no data values
                    sourceND[0] = (byte) 50;
                    sourceND[1] = (byte) 50;
                    // source possible valid data
                    validData[0] = (byte) 15;
                    validData[1] = (byte) 80;
                    // destination no data
                    destinationND[0] = (byte) 100;
                    destinationND[1] = (byte) 100;
                    destinationND[2] = (byte) 100;
                    // method for image creation
                    beanContainer[i][j] = imageSettings(validData, sourceND, destinationND, i, j);
                    break;
                case DataBuffer.TYPE_USHORT:
                    sourceND[0] = (short) 50;
                    sourceND[1] = (short) 50;
                    validData[0] = (short) 15;
                    validData[1] = (short) 80;
                    destinationND[0] = (short) 100;
                    destinationND[1] = (short) 100;
                    destinationND[2] = (short) 100;
                    beanContainer[i][j] = imageSettings(validData, sourceND, destinationND, i, j);
                    break;
                case DataBuffer.TYPE_SHORT:
                    sourceND[0] = (short) 50;
                    sourceND[1] = (short) 50;
                    validData[0] = (short) 15;
                    validData[1] = (short) 80;
                    destinationND[0] = (short) 100;
                    destinationND[1] = (short) 100;
                    destinationND[2] = (short) 100;
                    beanContainer[i][j] = imageSettings(validData, sourceND, destinationND, i, j);
                    break;
                case DataBuffer.TYPE_INT:
                    sourceND[0] = (int) 50;
                    sourceND[1] = (int) 50;
                    validData[0] = (int) 15;
                    validData[1] = (int) 80;
                    destinationND[0] = (int) 100;
                    destinationND[1] = (int) 100;
                    destinationND[2] = (int) 100;
                    beanContainer[i][j] = imageSettings(validData, sourceND, destinationND, i, j);
                    break;
                case DataBuffer.TYPE_FLOAT:
                    sourceND[0] = 50f;
                    sourceND[1] = 50f;
                    validData[0] = 15f;
                    validData[1] = 80f;
                    destinationND[0] = 100f;
                    destinationND[1] = 100f;
                    destinationND[2] = 100f;
                    beanContainer[i][j] = imageSettings(validData, sourceND, destinationND, i, j);
                    break;
                case DataBuffer.TYPE_DOUBLE:
                    sourceND[0] = 50d;
                    sourceND[1] = 50d;
                    validData[0] = 15d;
                    validData[1] = 80d;
                    destinationND[0] = 100d;
                    destinationND[1] = 100d;
                    destinationND[2] = 100d;
                    beanContainer[i][j] = imageSettings(validData, sourceND, destinationND, i, j);
                    break;
                }
            }
        }
        initialSetup = true;
    }

    private MosaicBean imageSettings(Number[] validData, Number[] sourceNodata,
            Number[] destinationNoDataValue, int dataType, int imageDataNoData) {

        MosaicBean tempMosaicBean = new MosaicBean();

        RenderedImage image1 = null;
        RenderedImage image2 = null;

        RenderedImage image3Band1 = null;
        RenderedImage image3Band2 = null;
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            switch (imageDataNoData) {
            case 0:
                // no data-no data
                image1 = getSyntheticUniformTypeImage(sourceNodata[0].byteValue(), dataType, 1);
                image2 = getSyntheticUniformTypeImage(sourceNodata[1].byteValue(), dataType, 1);
                image3Band1 = getSyntheticUniformTypeImage(sourceNodata[0].byteValue(), dataType, 3);
                image3Band2 = getSyntheticUniformTypeImage(sourceNodata[1].byteValue(), dataType, 3);
                break;
            case 1:
                // no data- data
                image1 = getSyntheticUniformTypeImage(sourceNodata[0].byteValue(), dataType, 1);
                image2 = getSyntheticUniformTypeImage(validData[1].byteValue(), dataType, 1);
                image3Band1 = getSyntheticUniformTypeImage(sourceNodata[0].byteValue(), dataType, 3);
                image3Band2 = getSyntheticUniformTypeImage(validData[1].byteValue(), dataType, 3);
                break;
            case 2:
                // data-data
                image1 = getSyntheticUniformTypeImage(validData[0].byteValue(), dataType, 1);
                image2 = getSyntheticUniformTypeImage(validData[1].byteValue(), dataType, 1);
                image3Band1 = getSyntheticUniformTypeImage(validData[0].byteValue(), dataType, 3);
                image3Band2 = getSyntheticUniformTypeImage(validData[1].byteValue(), dataType, 3);
                break;
            }

            break;
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            switch (imageDataNoData) {
            case 0:
                // no data-no data
                image1 = getSyntheticUniformTypeImage(sourceNodata[0].shortValue(), dataType, 1);
                image2 = getSyntheticUniformTypeImage(sourceNodata[1].shortValue(), dataType, 1);
                image3Band1 = getSyntheticUniformTypeImage(sourceNodata[0].shortValue(), dataType,
                        3);
                image3Band2 = getSyntheticUniformTypeImage(sourceNodata[1].shortValue(), dataType,
                        3);
                break;
            case 1:
                // data-no data
                image1 = getSyntheticUniformTypeImage(sourceNodata[0].shortValue(), dataType, 1);
                image2 = getSyntheticUniformTypeImage(validData[1].shortValue(), dataType, 1);
                image3Band1 = getSyntheticUniformTypeImage(sourceNodata[0].shortValue(), dataType,
                        3);
                image3Band2 = getSyntheticUniformTypeImage(validData[1].shortValue(), dataType, 3);
                break;
            case 2:
                // data-data
                image1 = getSyntheticUniformTypeImage(validData[0].shortValue(), dataType, 1);
                image2 = getSyntheticUniformTypeImage(validData[1].shortValue(), dataType, 1);
                image3Band1 = getSyntheticUniformTypeImage(validData[0].shortValue(), dataType, 3);
                image3Band2 = getSyntheticUniformTypeImage(validData[1].shortValue(), dataType, 3);
                break;
            }
            break;
        case DataBuffer.TYPE_INT:
            switch (imageDataNoData) {
            case 0:
                // no data-no data
                image1 = getSyntheticUniformTypeImage(sourceNodata[0].intValue(), dataType, 1);
                image2 = getSyntheticUniformTypeImage(sourceNodata[1].intValue(), dataType, 1);
                image3Band1 = getSyntheticUniformTypeImage(sourceNodata[0].intValue(), dataType, 3);
                image3Band2 = getSyntheticUniformTypeImage(sourceNodata[1].intValue(), dataType, 3);
                break;
            case 1:
                // data-no data
                image1 = getSyntheticUniformTypeImage(sourceNodata[0].intValue(), dataType, 1);
                image2 = getSyntheticUniformTypeImage(validData[1].intValue(), dataType, 1);
                image3Band1 = getSyntheticUniformTypeImage(sourceNodata[0].intValue(), dataType, 3);
                image3Band2 = getSyntheticUniformTypeImage(validData[1].intValue(), dataType, 3);
                break;
            case 2:
                // data-data
                image1 = getSyntheticUniformTypeImage(validData[0].intValue(), dataType, 1);
                image2 = getSyntheticUniformTypeImage(validData[1].intValue(), dataType, 1);
                image3Band1 = getSyntheticUniformTypeImage(validData[0].intValue(), dataType, 3);
                image3Band2 = getSyntheticUniformTypeImage(validData[1].intValue(), dataType, 3);
                break;
            }
            break;
        case DataBuffer.TYPE_FLOAT:
            switch (imageDataNoData) {
            case 0:
                // no data-no data
                image1 = getSyntheticUniformTypeImage(sourceNodata[0].floatValue(), dataType, 1);
                image2 = getSyntheticUniformTypeImage(sourceNodata[1].floatValue(), dataType, 1);
                image3Band1 = getSyntheticUniformTypeImage(sourceNodata[0].floatValue(), dataType,
                        3);
                image3Band2 = getSyntheticUniformTypeImage(sourceNodata[1].floatValue(), dataType,
                        3);
                break;
            case 1:
                // data-no data
                image1 = getSyntheticUniformTypeImage(sourceNodata[0].floatValue(), dataType, 1);
                image2 = getSyntheticUniformTypeImage(validData[1].floatValue(), dataType, 1);
                image3Band1 = getSyntheticUniformTypeImage(sourceNodata[0].floatValue(), dataType,
                        3);
                image3Band2 = getSyntheticUniformTypeImage(validData[1].floatValue(), dataType, 3);
                break;
            case 2:
                // data-data
                image1 = getSyntheticUniformTypeImage(validData[0].floatValue(), dataType, 1);
                image2 = getSyntheticUniformTypeImage(validData[1].floatValue(), dataType, 1);
                image3Band1 = getSyntheticUniformTypeImage(validData[0].floatValue(), dataType, 3);
                image3Band2 = getSyntheticUniformTypeImage(validData[1].floatValue(), dataType, 3);
                break;
            }
            break;
        case DataBuffer.TYPE_DOUBLE:
            switch (imageDataNoData) {
            case 0:
                // no data-no data
                image1 = getSyntheticUniformTypeImage(sourceNodata[0].doubleValue(), dataType, 1);
                image2 = getSyntheticUniformTypeImage(sourceNodata[1].doubleValue(), dataType, 1);
                image3Band1 = getSyntheticUniformTypeImage(sourceNodata[0].doubleValue(), dataType,
                        3);
                image3Band2 = getSyntheticUniformTypeImage(sourceNodata[1].doubleValue(), dataType,
                        3);
                break;
            case 1:
                // data-no data
                image1 = getSyntheticUniformTypeImage(sourceNodata[0].doubleValue(), dataType, 1);
                image2 = getSyntheticUniformTypeImage(validData[1].doubleValue(), dataType, 1);
                image3Band1 = getSyntheticUniformTypeImage(sourceNodata[0].doubleValue(), dataType,
                        3);
                image3Band2 = getSyntheticUniformTypeImage(validData[1].doubleValue(), dataType, 3);
                break;
            case 2:
                // data-data
                image1 = getSyntheticUniformTypeImage(validData[0].doubleValue(), dataType, 1);
                image2 = getSyntheticUniformTypeImage(validData[1].doubleValue(), dataType, 1);
                image3Band1 = getSyntheticUniformTypeImage(validData[0].doubleValue(), dataType, 3);
                image3Band2 = getSyntheticUniformTypeImage(validData[1].doubleValue(), dataType, 3);
                break;
            }
            break;
        }

        RenderedImage[] array1Band = { image1, image2 };
        RenderedImage[] array3Band = { image3Band1, image3Band2 };
        // new layout that double the image
        ImageLayout layout = new ImageLayout(0, 0, array1Band[0].getWidth()
                + array1Band[1].getWidth(), array1Band[0].getHeight());
        layout.setTileHeight(array1Band[0].getHeight() / 16);
        layout.setTileWidth(array1Band[0].getWidth() / 16);
        // create the rendering hints
        hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);
        // All the data are saved in a mosaic bean
        tempMosaicBean.setDestinationNoData(destinationNoDataValue);
        tempMosaicBean.setImage(array1Band);
        tempMosaicBean.setImage3Bands(array3Band);
        // This bean is an inner bean for the mosaicNoDataOperation
        ImageMosaicBean[] imagesBean1Band = new ImageMosaicBean[array1Band.length];
        ImageMosaicBean[] imagesBean3Band = new ImageMosaicBean[array1Band.length];

        for (int k = 0; k < array1Band.length; k++) {
            ImageMosaicBean helpBean1Band = new ImageMosaicBean();
            ImageMosaicBean helpBean3Band = new ImageMosaicBean();
            helpBean1Band.setImage(array1Band[k]);

            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                helpBean1Band.setSourceNoData(new Range<Byte>(sourceNodata[k].byteValue(), true,
                        sourceNodata[k].byteValue(), true));
                helpBean3Band.setSourceNoData(new Range<Byte>(sourceNodata[k].byteValue(), true,
                        sourceNodata[k].byteValue(), true));
                break;
            case DataBuffer.TYPE_USHORT:
                // helpBean1Band.setSourceNoData(new Range<Short>(sourceNodata[k]
                // .shortValue(), true, sourceNodata[k].shortValue(), true));
                // helpBean3Band.setSourceNoData(new Range<Short>(sourceNodata[k]
                // .shortValue(), true, sourceNodata[k].shortValue(), true));
                // break;
            case DataBuffer.TYPE_SHORT:
                helpBean1Band.setSourceNoData(new Range<Short>(sourceNodata[k].shortValue(), true,
                        sourceNodata[k].shortValue(), true));
                helpBean3Band.setSourceNoData(new Range<Short>(sourceNodata[k].shortValue(), true,
                        sourceNodata[k].shortValue(), true));
                break;
            case DataBuffer.TYPE_INT:
                helpBean1Band.setSourceNoData(new Range<Integer>(sourceNodata[k].intValue(), true,
                        sourceNodata[k].intValue(), true));
                helpBean3Band.setSourceNoData(new Range<Integer>(sourceNodata[k].intValue(), true,
                        sourceNodata[k].intValue(), true));
                break;
            case DataBuffer.TYPE_FLOAT:
                helpBean1Band.setSourceNoData(new Range<Float>(sourceNodata[k].floatValue(), true,
                        sourceNodata[k].floatValue(), true));
                helpBean3Band.setSourceNoData(new Range<Float>(sourceNodata[k].floatValue(), true,
                        sourceNodata[k].floatValue(), true));
                break;
            case DataBuffer.TYPE_DOUBLE:
                helpBean1Band.setSourceNoData(new Range<Double>(sourceNodata[k].doubleValue(),
                        true, sourceNodata[k].doubleValue(), true));
                helpBean3Band.setSourceNoData(new Range<Double>(sourceNodata[k].doubleValue(),
                        true, sourceNodata[k].doubleValue(), true));
                break;
            }
            helpBean1Band.setAlphaChannel(null);
            helpBean1Band.setImageRoi(null);
            helpBean3Band.setAlphaChannel(null);
            helpBean3Band.setImageRoi(null);
            imagesBean1Band[k] = helpBean1Band;
            imagesBean3Band[k] = helpBean3Band;
        }

        tempMosaicBean.setInnerBean(imagesBean1Band);
        tempMosaicBean.setInnerBean3Band(imagesBean3Band);
        tempMosaicBean.setSourceValidData(validData);
        tempMosaicBean.setSourceNoData(sourceNodata);

        return tempMosaicBean;

    }

    // Method for image creation
    public static RenderedImage getSyntheticUniformTypeImage(Number value, int dataType,
            int numBands) {

        // parameter block initialization
        ParameterBlock pb = new ParameterBlock();
        pb.add(DEFAULT_WIDTH);
        pb.add(DEFAULT_HEIGTH);
        if (numBands == 3) {
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                byte valueByte = value.byteValue();
                Byte[] arraybyte = new Byte[] { valueByte, valueByte, valueByte };
                pb.add(arraybyte);
                break;
            case DataBuffer.TYPE_USHORT:
                short valueUShort = value.shortValue();
                Short[] arrayUShort = new Short[] { (short) (valueUShort & 0xffff),
                        (short) (valueUShort & 0xffff), (short) (valueUShort & 0xffff) };
                pb.add(arrayUShort);
                break;
            case DataBuffer.TYPE_SHORT:
                short valueShort = value.shortValue();
                Short[] arrayShort = new Short[] { valueShort, valueShort, valueShort };
                pb.add(arrayShort);
                break;
            case DataBuffer.TYPE_INT:
                int valueInt = value.intValue();
                Integer[] arrayInteger = new Integer[] { valueInt, valueInt, valueInt };
                pb.add(arrayInteger);
                break;
            case DataBuffer.TYPE_FLOAT:
                float valueFloat = value.floatValue();
                Float[] arrayFloat = new Float[] { valueFloat, valueFloat, valueFloat };
                pb.add(arrayFloat);
                break;
            case DataBuffer.TYPE_DOUBLE:
                double valueDouble = value.doubleValue();
                Double[] arrayDouble = new Double[] { valueDouble, valueDouble, valueDouble };
                pb.add(arrayDouble);
                break;
            }
        } else {
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                byte valueByte = value.byteValue();
                Byte[] arraybyte = new Byte[] { valueByte };
                pb.add(arraybyte);
                break;
            case DataBuffer.TYPE_USHORT:
                short valueUShort = value.shortValue();
                Short[] arrayUShort = new Short[] { (short) (valueUShort & 0xffff) };
                pb.add(arrayUShort);
                break;
            case DataBuffer.TYPE_SHORT:
                short valueShort = value.shortValue();
                Short[] arrayShort = new Short[] { valueShort };
                pb.add(arrayShort);
                break;
            case DataBuffer.TYPE_INT:
                int valueInt = value.intValue();
                Integer[] arrayInteger = new Integer[] { valueInt };
                pb.add(arrayInteger);
                break;
            case DataBuffer.TYPE_FLOAT:
                float valueFloat = value.floatValue();
                Float[] arrayFloat = new Float[] { valueFloat };
                pb.add(arrayFloat);
                break;
            case DataBuffer.TYPE_DOUBLE:
                double valueDouble = value.doubleValue();
                Double[] arrayDouble = new Double[] { valueDouble };
                pb.add(arrayDouble);
                break;
            }

        }

        // Create the constant operation.
        return JAI.create("constant", pb);

    }

    // Help-class for storing all the various data in a single container
    private static class MosaicBean implements Serializable {

        /** serialVersionUID */
        private static final long serialVersionUID = 1L;

        // 1 band image
        private RenderedImage[] image;

        // 3 band image
        private RenderedImage[] image3Bands;

        // inner bean used
        private ImageMosaicBean[] innerBean;

        // destination no data values
        private Number[] destinationNoData;

        // source valid data values
        private Number[] sourceValidData;

        // inner bean for 3 band used
        private ImageMosaicBean[] innerBean3Band;

        // source NoData useful for testing ROI
        private Number[] sourceNoData;

        // source aLPHA useful for testing ROI
        private Number[] sourceAlphaChannel;

        MosaicBean() {
        }

        public RenderedImage[] getImage() {
            return image;
        }

        public void setImage(RenderedImage[] image) {
            this.image = image;
        }

        public ImageMosaicBean[] getInnerBean() {
            return innerBean;
        }

        public void setInnerBean(ImageMosaicBean[] innerBean) {
            this.innerBean = innerBean;
        }

        public Number[] getDestinationNoData() {
            return destinationNoData;
        }

        public void setDestinationNoData(Number[] destinationNoData) {
            this.destinationNoData = destinationNoData;
        }

        public Number[] getSourceValidData() {
            return sourceValidData;
        }

        public void setSourceValidData(Number[] sourceValidData) {
            this.sourceValidData = sourceValidData;
        }

        public RenderedImage[] getImage3Bands() {
            return image3Bands;
        }

        public void setImage3Bands(RenderedImage[] image3Bands) {
            this.image3Bands = image3Bands;
        }

        public ImageMosaicBean[] getInnerBean3Band() {
            return innerBean3Band;
        }

        public void setInnerBean3Band(ImageMosaicBean[] innerBean3Band) {
            this.innerBean3Band = innerBean3Band;
        }

        public Number[] getSourceNoData() {
            return sourceNoData;
        }

        public void setSourceNoData(Number[] sourceNoData) {
            this.sourceNoData = sourceNoData;
        }

        public Number[] getSourceAlphaChannel() {
            return sourceAlphaChannel;
        }

        public void setSourceAlphaChannel(Number[] sourceAlphaChannel) {
            this.sourceAlphaChannel = sourceAlphaChannel;
        }

    }

}
