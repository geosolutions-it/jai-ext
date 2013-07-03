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
import org.junit.Before;
import org.junit.Test;

/**
 * This test class is used for checking the functionality of the MosaicNoDataOpImage. Every test is performed on all the DataBuffer types. The mosaic
 * operations are calculated in the OVERLAY mode. The first 3 series of tests execute a mosaic operation between:
 * <ul>
 * <li>two image with No Data values</li>
 * <li>two image with the first having No Data values and the second having Data values</li>
 * <li>two image with Data values</li>
 * </ul>
 * 
 * The 4th and 5th series make the same tests but using a ROI. The only difference is that two images with No Data values are not compared. The 6th
 * and 7th series make the same ROI tests but using a Alpha channel. The 8th series performs a test on 3 images which the first image contains only No
 * Data values, the second image uses a ROI, and the last image has an alpha channel. The 9th series performs a test on 3 images in the same above
 * condition but the mosaic type is BLEND, not OVERLAY. The 10th series executes a simple test on 3 images with only data values. The 11th series
 * checks that all the Exceptions are thrown in the right conditions.
 * 
 * <ul> </ul>
 * The other surrounding methods belongs to 2 categories:
 * <ul>
 * <li>test methods which execute the tests and check if the calculated values are correct</li>
 * <li>help methods for creating all the images and ROI with different structures</li>
 * </ul> 
 */

public class MosaicNoDataTest {

    // Default initialization set to false
    public final static boolean DEFAULT_SETUP_INITALIZATION = false;

    private final static MosaicType DEFAULT_MOSAIC_TYPE = MosaicDescriptor.MOSAIC_TYPE_OVERLAY;

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

    public enum DataDisplacement {
        NODATA_NODATA, NODATA_DATA, DATA_DATA;
    }

    @Before
    public void dataPreparation() {
        // Initialization setup methods
        initalSetup();
        initialSetup3image(false, true);
    }

    // THIS TESTS ARE WITH 2 IMAGES: THE FIRST IS IN THE LEFT SIDE AND THE SECOND IS TRANSLATED
    // ON THE RIGHT BY HALF OF ITS WIDTH (HALF IMAGE OVERLAY)

    // FIRST SERIES
    // Test No Data: image 1 and 2 only no data
    @Test
    public void testByteNoData1NoData2() {
        MosaicBean testBean = beanContainer[0][0];
        int dataType = DataBuffer.TYPE_BYTE;
        testMethodBody(testBean, dataType, null);

    }

    @Test
    public void testUShortNoData1NoData2() {
        MosaicBean testBean = beanContainer[1][0];
        int dataType = DataBuffer.TYPE_USHORT;
        testMethodBody(testBean, dataType, null);
    }

    @Test
    public void testShortNoData1NoData2() {
        MosaicBean testBean = beanContainer[2][0];
        int dataType = DataBuffer.TYPE_SHORT;
        testMethodBody(testBean, dataType, null);
    }

    @Test
    public void testIntNoData1NoData2() {
        MosaicBean testBean = beanContainer[3][0];
        int dataType = DataBuffer.TYPE_INT;
        testMethodBody(testBean, dataType, null);
    }

    @Test
    public void testFloatNoData1NoData2() {
        MosaicBean testBean = beanContainer[4][0];
        int dataType = DataBuffer.TYPE_FLOAT;
        testMethodBody(testBean, dataType, null);
    }

    @Test
    public void testDoubleNoData1NoData2() {
        MosaicBean testBean = beanContainer[5][0];
        int dataType = DataBuffer.TYPE_DOUBLE;
        testMethodBody(testBean, dataType, null);
    }

    // SECOND SERIES
    // Test No Data: image 1 with data and image 2 with no data
    @Test
    public void testByteNoData1Data2() {
        MosaicBean testBean = beanContainer[0][1];
        int dataType = DataBuffer.TYPE_BYTE;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, valideValueSource2);
    }

    @Test
    public void testUShortNoData1Data2() {
        MosaicBean testBean = beanContainer[1][1];
        int dataType = DataBuffer.TYPE_USHORT;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, valideValueSource2);
    }

    @Test
    public void testShortNoData1Data2() {
        MosaicBean testBean = beanContainer[2][1];
        int dataType = DataBuffer.TYPE_SHORT;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, valideValueSource2);
    }

    @Test
    public void testIntNoData1Data2() {
        MosaicBean testBean = beanContainer[3][1];
        int dataType = DataBuffer.TYPE_INT;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, valideValueSource2);
    }

    @Test
    public void testFloatNoData1Data2() {
        MosaicBean testBean = beanContainer[4][1];
        int dataType = DataBuffer.TYPE_FLOAT;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, valideValueSource2);
    }

    @Test
    public void testDoubleNoData1Data2() {
        MosaicBean testBean = beanContainer[5][1];
        int dataType = DataBuffer.TYPE_DOUBLE;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, valideValueSource2);
    }

    // THIRD SERIES
    // Test No data: image 1 and 2 with data
    @Test
    public void testByteData1Data2() {
        //
        MosaicBean testBean = beanContainer[0][2];
        int dataType = DataBuffer.TYPE_BYTE;
        Number valideValueSource1 = testBean.getSourceValidData()[0];
        testMethodBody(testBean, dataType, valideValueSource1);
    }

    @Test
    public void testUShortData1Data2() {
        //
        MosaicBean testBean = beanContainer[1][2];
        int dataType = DataBuffer.TYPE_USHORT;
        Number valideValueSource1 = testBean.getSourceValidData()[0];
        testMethodBody(testBean, dataType, valideValueSource1);
    }

    @Test
    public void testShortData1Data2() {
        //
        MosaicBean testBean = beanContainer[2][2];
        int dataType = DataBuffer.TYPE_SHORT;
        Number valideValueSource1 = testBean.getSourceValidData()[0];
        testMethodBody(testBean, dataType, valideValueSource1);
    }

    @Test
    public void testIntData1Data2() {
        //
        MosaicBean testBean = beanContainer[3][2];
        int dataType = DataBuffer.TYPE_INT;
        Number valideValueSource1 = testBean.getSourceValidData()[0];
        testMethodBody(testBean, dataType, valideValueSource1);
    }

    @Test
    public void testFloatData1Data2() {
        //
        MosaicBean testBean = beanContainer[4][2];
        int dataType = DataBuffer.TYPE_FLOAT;
        Number valideValueSource1 = testBean.getSourceValidData()[0];
        testMethodBody(testBean, dataType, valideValueSource1);
    }

    @Test
    public void testDoubleData1Data2() {
        //
        MosaicBean testBean = beanContainer[5][2];
        int dataType = DataBuffer.TYPE_DOUBLE;
        Number valideValueSource1 = testBean.getSourceValidData()[0];
        testMethodBody(testBean, dataType, valideValueSource1);
    }

    // FOURTH SERIES
    // Test ROI: image 1 and 2 with data
    @Test
    public void testROIByteData1Data2() {
        creationROI();
        //
        MosaicBean testBean = beanContainer[0][2];
        int dataType = DataBuffer.TYPE_BYTE;
        Number valideValueSource1 = testBean.getSourceValidData()[0];
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, null, valideValueSource2, valideValueSource1, true,
                false);
    }

    @Test
    public void testROIUShortData1Data2() {
        creationROI();
        //
        MosaicBean testBean = beanContainer[1][2];
        int dataType = DataBuffer.TYPE_USHORT;
        Number valideValueSource1 = testBean.getSourceValidData()[0];
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, null, valideValueSource2, valideValueSource1, true,
                false);
    }

    @Test
    public void testROIShortData1Data2() {
        creationROI();
        //
        MosaicBean testBean = beanContainer[2][2];
        int dataType = DataBuffer.TYPE_SHORT;
        Number valideValueSource1 = testBean.getSourceValidData()[0];
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, null, valideValueSource2, valideValueSource1, true,
                false);
    }

    @Test
    public void testROIIntData1Data2() {
        creationROI();
        //
        MosaicBean testBean = beanContainer[3][2];
        int dataType = DataBuffer.TYPE_INT;
        Number valideValueSource1 = testBean.getSourceValidData()[0];
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, null, valideValueSource2, valideValueSource1, true,
                false);
    }

    @Test
    public void testROIFloatData1Data2() {
        creationROI();
        //
        MosaicBean testBean = beanContainer[4][2];
        int dataType = DataBuffer.TYPE_FLOAT;
        Number valideValueSource1 = testBean.getSourceValidData()[0];
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, null, valideValueSource2, valideValueSource1, true,
                false);
    }

    @Test
    public void testROIDoubleData1Data2() {
        creationROI();
        //
        MosaicBean testBean = beanContainer[5][2];
        int dataType = DataBuffer.TYPE_DOUBLE;
        Number valideValueSource1 = testBean.getSourceValidData()[0];
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, null, valideValueSource2, valideValueSource1, true,
                false);
    }

    // FIFTH SERIES
    // Test ROI: image 1 with data and 2 with no data
    @Test
    public void testROIByteNoData1Data2() {
        creationROI();
        //
        MosaicBean testBean = beanContainer[0][1];
        int dataType = DataBuffer.TYPE_BYTE;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, null, valideValueSource2, valideValueSource2, true,
                false);
    }

    @Test
    public void testROIUShortNoData1Data2() {
        creationROI();
        //
        MosaicBean testBean = beanContainer[1][1];
        int dataType = DataBuffer.TYPE_USHORT;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, null, valideValueSource2, valideValueSource2, true,
                false);
    }

    @Test
    public void testROIShortNoData1Data2() {
        creationROI();
        //
        MosaicBean testBean = beanContainer[2][1];
        int dataType = DataBuffer.TYPE_SHORT;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, null, valideValueSource2, valideValueSource2, true,
                false);
    }

    @Test
    public void testROIIntNoData1Data2() {
        creationROI();
        //
        MosaicBean testBean = beanContainer[3][1];
        int dataType = DataBuffer.TYPE_INT;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, null, valideValueSource2, valideValueSource2, true,
                false);
    }

    @Test
    public void testROIFloatNoData1Data2() {
        creationROI();
        //
        MosaicBean testBean = beanContainer[4][1];
        int dataType = DataBuffer.TYPE_FLOAT;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, null, valideValueSource2, valideValueSource2, true,
                false);
    }

    @Test
    public void testROIDoubleNoData1Data2() {
        creationROI();
        //
        MosaicBean testBean = beanContainer[5][1];
        int dataType = DataBuffer.TYPE_DOUBLE;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, null, valideValueSource2, valideValueSource2, true,
                false);
    }

    // SIXTH SERIES
    // Test Alpha: image 1 and image 2 with data
    @Test
    public void testAlphaByteData1Data2() {
        creationAlpha();
        //
        MosaicBean testBean = beanContainer[0][2];
        int dataType = DataBuffer.TYPE_BYTE;
        Number valideValueSource1 = testBean.getSourceValidData()[0];
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, valideValueSource1, valideValueSource1,
                valideValueSource2, false, true);
    }

    @Test
    public void testAlphaUShortData1Data2() {
        creationAlpha();
        //
        MosaicBean testBean = beanContainer[1][2];
        int dataType = DataBuffer.TYPE_USHORT;
        Number valideValueSource1 = testBean.getSourceValidData()[0];
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, valideValueSource1, valideValueSource1,
                valideValueSource2, false, true);
    }

    @Test
    public void testAlphaShortData1Data2() {
        creationAlpha();
        //
        MosaicBean testBean = beanContainer[2][2];
        int dataType = DataBuffer.TYPE_SHORT;
        Number valideValueSource1 = testBean.getSourceValidData()[0];
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, valideValueSource1, valideValueSource1,
                valideValueSource2, false, true);
    }

    @Test
    public void testAlphaIntData1Data2() {
        creationAlpha();
        //
        MosaicBean testBean = beanContainer[3][2];
        int dataType = DataBuffer.TYPE_INT;
        Number valideValueSource1 = testBean.getSourceValidData()[0];
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, valideValueSource1, valideValueSource1,
                valideValueSource2, false, true);
    }

    @Test
    public void testAlphaFloatData1Data2() {
        creationAlpha();
        //
        MosaicBean testBean = beanContainer[4][2];
        int dataType = DataBuffer.TYPE_FLOAT;
        Number valideValueSource1 = testBean.getSourceValidData()[0];
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, valideValueSource1, valideValueSource1,
                valideValueSource2, false, true);
    }

    @Test
    public void testAlphaDoubleData1Data2() {
        creationAlpha();
        //
        MosaicBean testBean = beanContainer[5][2];
        int dataType = DataBuffer.TYPE_DOUBLE;
        Number valideValueSource1 = testBean.getSourceValidData()[0];
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, valideValueSource1, valideValueSource1,
                valideValueSource2, false, true);
    }

    // SEVENTH SERIES
    // Test Alpha: image 1 with no data and image 2 with data
    @Test
    public void testAlphaNoByteData1Data2() {
        creationAlpha();
        //
        MosaicBean testBean = beanContainer[0][1];
        int dataType = DataBuffer.TYPE_BYTE;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, null, valideValueSource2, valideValueSource2, false,
                true);
    }

    @Test
    public void testAlphaNoUShortData1Data2() {
        creationAlpha();
        //
        MosaicBean testBean = beanContainer[1][1];
        int dataType = DataBuffer.TYPE_USHORT;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, null, valideValueSource2, valideValueSource2, false,
                true);
    }

    @Test
    public void testAlphaNoShortData1Data2() {
        creationAlpha();
        //
        MosaicBean testBean = beanContainer[2][1];
        int dataType = DataBuffer.TYPE_SHORT;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, null, valideValueSource2, valideValueSource2, false,
                true);
    }

    @Test
    public void testAlphaIntNoData1Data2() {
        creationAlpha();
        //
        MosaicBean testBean = beanContainer[3][1];
        int dataType = DataBuffer.TYPE_INT;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, null, valideValueSource2, valideValueSource2, false,
                true);
    }

    @Test
    public void testAlphaFloatNoData1Data2() {
        creationAlpha();
        //
        MosaicBean testBean = beanContainer[4][1];
        int dataType = DataBuffer.TYPE_FLOAT;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, null, valideValueSource2, valideValueSource2, false,
                true);
    }

    @Test
    public void testAlphaDoubleNoData1Data2() {
        creationAlpha();
        //
        MosaicBean testBean = beanContainer[5][1];
        int dataType = DataBuffer.TYPE_DOUBLE;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        testMethodBody(testBean, dataType, null, valideValueSource2, valideValueSource2, false,
                true);
    }

    // TEST WITH 3 IMAGES: IMAGE 1 WITH NO DATA IN THE UPPER LEFT, IMAGE 2 WITH ROI IN THE UPPER RIGTH,
    // IMAGE 3 WITH ALPHA CHANNEL IN THE LOWER LEFT
    // EIGHTH SERIES
    @Test
    public void testByte3ImagesNoDataROIAlpha() {
        //
        MosaicBean testBean = beanContainer3Images[0];
        int dataType = DataBuffer.TYPE_BYTE;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        Number valideValueSource3 = testBean.getSourceValidData()[2];
        testMethodBody(testBean, dataType, null, valideValueSource2, null, valideValueSource3,
                valideValueSource2, valideValueSource3, false, false, true);
    }

    @Test
    public void testUShort3ImagesNoDataROIAlpha() {
        //
        MosaicBean testBean = beanContainer3Images[1];
        int dataType = DataBuffer.TYPE_USHORT;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        Number valideValueSource3 = testBean.getSourceValidData()[2];
        testMethodBody(testBean, dataType, null, valideValueSource2, null, valideValueSource3,
                valideValueSource2, valideValueSource3, false, false, true);
    }

    @Test
    public void testShort3ImagesNoDataROIAlpha() {
        //
        MosaicBean testBean = beanContainer3Images[2];
        int dataType = DataBuffer.TYPE_SHORT;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        Number valideValueSource3 = testBean.getSourceValidData()[2];
        testMethodBody(testBean, dataType, null, valideValueSource2, null, valideValueSource3,
                valideValueSource2, valideValueSource3, false, false, true);
    }

    @Test
    public void testInt3ImagesNoDataROIAlpha() {
        //
        MosaicBean testBean = beanContainer3Images[3];
        int dataType = DataBuffer.TYPE_INT;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        Number valideValueSource3 = testBean.getSourceValidData()[2];
        testMethodBody(testBean, dataType, null, valideValueSource2, null, valideValueSource3,
                valideValueSource2, valideValueSource3, false, false, true);
    }

    @Test
    public void testFloat3ImagesNoDataROIAlpha() {
        //
        MosaicBean testBean = beanContainer3Images[4];
        int dataType = DataBuffer.TYPE_FLOAT;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        Number valideValueSource3 = testBean.getSourceValidData()[2];
        testMethodBody(testBean, dataType, null, valideValueSource2, null, valideValueSource3,
                valideValueSource2, valideValueSource3, false, false, true);
    }

    @Test
    public void testDouble3ImagesNoDataROIAlpha() {
        //
        MosaicBean testBean = beanContainer3Images[5];
        int dataType = DataBuffer.TYPE_DOUBLE;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        Number valideValueSource3 = testBean.getSourceValidData()[2];
        testMethodBody(testBean, dataType, null, valideValueSource2, null, valideValueSource3,
                valideValueSource2, valideValueSource3, false, false, true);
    }

    // NINTH SERIES
    @Test
    public void testBLENDByte3Images() {
        //
        MosaicBean testBean = beanContainer3Images[0];
        int dataType = DataBuffer.TYPE_BYTE;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        Number valideValueSource3 = testBean.getSourceValidData()[2];
        testMethodBody(testBean, dataType, null, valideValueSource2, null, valideValueSource3,
                valideValueSource2, valideValueSource3, false, false, false);
    }

    @Test
    public void testBLENDUShort3Images() {
        //
        MosaicBean testBean = beanContainer3Images[1];
        int dataType = DataBuffer.TYPE_USHORT;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        Number valideValueSource3 = testBean.getSourceValidData()[2];
        testMethodBody(testBean, dataType, null, valideValueSource2, null, valideValueSource3,
                valideValueSource2, valideValueSource3, false, false, false);
    }

    @Test
    public void testBLENDShort3Images() {
        //
        MosaicBean testBean = beanContainer3Images[2];
        int dataType = DataBuffer.TYPE_SHORT;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        Number valideValueSource3 = testBean.getSourceValidData()[2];
        testMethodBody(testBean, dataType, null, valideValueSource2, null, valideValueSource3,
                valideValueSource2, valideValueSource3, false, false, false);
    }

    @Test
    public void testBLENDInt3Images() {
        //
        MosaicBean testBean = beanContainer3Images[3];
        int dataType = DataBuffer.TYPE_INT;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        Number valideValueSource3 = testBean.getSourceValidData()[2];
        testMethodBody(testBean, dataType, null, valideValueSource2, null, valideValueSource3,
                valideValueSource2, valideValueSource3, false, false, false);
    }

    @Test
    public void testBLENDFloat3Images() {
        //
        MosaicBean testBean = beanContainer3Images[4];
        int dataType = DataBuffer.TYPE_FLOAT;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        Number valideValueSource3 = testBean.getSourceValidData()[2];
        testMethodBody(testBean, dataType, null, valideValueSource2, null, valideValueSource3,
                valideValueSource2, valideValueSource3, false, false, false);
    }

    @Test
    public void testBLENDDouble3Images() {
        //
        MosaicBean testBean = beanContainer3Images[5];
        int dataType = DataBuffer.TYPE_DOUBLE;
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        Number valideValueSource3 = testBean.getSourceValidData()[2];
        testMethodBody(testBean, dataType, null, valideValueSource2, null, valideValueSource3,
                valideValueSource2, valideValueSource3, false, false, false);
    }

    // TENTH SERIES
    // TEST WITH 3 IMAGES: SAME POSITIONS OF THE THREE IMAGES ABOVE, NULL NO DATA, NO ROI OR ALPHA CHANNEL

    @Test
    public void testByteNullNoData() {
        initialSetup3image(true, false);

        MosaicBean testBean = beanContainer3Images[0];
        int dataType = DataBuffer.TYPE_BYTE;
        Number valideValueSource1 = testBean.getSourceValidData()[0];
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        Number valideValueSource3 = testBean.getSourceValidData()[2];
        testMethodBody(testBean, dataType, valideValueSource1, valideValueSource1,
                valideValueSource2, valideValueSource1, valideValueSource1, valideValueSource3,
                false, false, true);
    }

    @Test
    public void testUShortNullNoData() {
        initialSetup3image(true, false);

        MosaicBean testBean = beanContainer3Images[1];
        int dataType = DataBuffer.TYPE_USHORT;
        Number valideValueSource1 = testBean.getSourceValidData()[0];
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        Number valideValueSource3 = testBean.getSourceValidData()[2];
        testMethodBody(testBean, dataType, valideValueSource1, valideValueSource1,
                valideValueSource2, valideValueSource1, valideValueSource1, valideValueSource3,
                false, false, true);
    }

    @Test
    public void testShortNullNoData() {
        initialSetup3image(true, false);

        MosaicBean testBean = beanContainer3Images[2];
        int dataType = DataBuffer.TYPE_SHORT;
        Number valideValueSource1 = testBean.getSourceValidData()[0];
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        Number valideValueSource3 = testBean.getSourceValidData()[2];
        testMethodBody(testBean, dataType, valideValueSource1, valideValueSource1,
                valideValueSource2, valideValueSource1, valideValueSource1, valideValueSource3,
                false, false, true);
    }

    @Test
    public void testIntNullNoData() {
        initialSetup3image(true, false);

        MosaicBean testBean = beanContainer3Images[3];
        int dataType = DataBuffer.TYPE_INT;
        Number valideValueSource1 = testBean.getSourceValidData()[0];
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        Number valideValueSource3 = testBean.getSourceValidData()[2];
        testMethodBody(testBean, dataType, valideValueSource1, valideValueSource1,
                valideValueSource2, valideValueSource1, valideValueSource1, valideValueSource3,
                false, false, true);
    }

    @Test
    public void testFloatNullNoData() {
        initialSetup3image(true, false);

        MosaicBean testBean = beanContainer3Images[4];
        int dataType = DataBuffer.TYPE_FLOAT;
        Number valideValueSource1 = testBean.getSourceValidData()[0];
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        Number valideValueSource3 = testBean.getSourceValidData()[2];
        testMethodBody(testBean, dataType, valideValueSource1, valideValueSource1,
                valideValueSource2, valideValueSource1, valideValueSource1, valideValueSource3,
                false, false, true);
    }

    @Test
    public void testDoubleNullNoData() {
        initialSetup3image(true, false);

        MosaicBean testBean = beanContainer3Images[5];
        int dataType = DataBuffer.TYPE_DOUBLE;
        Number valideValueSource1 = testBean.getSourceValidData()[0];
        Number valideValueSource2 = testBean.getSourceValidData()[1];
        Number valideValueSource3 = testBean.getSourceValidData()[2];
        testMethodBody(testBean, dataType, valideValueSource1, valideValueSource1,
                valideValueSource2, valideValueSource1, valideValueSource1, valideValueSource3,
                false, false, true);
    }

    // ELEVENTH SERIES
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
            LOGGER.log(Level.INFO, "No sample model present");
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
        Number[] exampleData = { 15, 15 };
        mosaicArray[0] = getSyntheticUniformTypeImage(exampleData, null, DataBuffer.TYPE_INT, 3,
                DataDisplacement.DATA_DATA, true, false);
        mosaicArray[1] = getSyntheticUniformTypeImage(exampleData, null, DataBuffer.TYPE_INT, 1,
                DataDisplacement.DATA_DATA, false, false);

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

        Number[] exampleData = { 15, 15 };
        mosaicArray[0] = getSyntheticUniformTypeImage(exampleData, null, DataBuffer.TYPE_INT, 1,
                DataDisplacement.DATA_DATA, true, false);
        mosaicArray[1] = getSyntheticUniformTypeImage(exampleData, null, DataBuffer.TYPE_INT, 1,
                DataDisplacement.DATA_DATA, false, false);

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
        Number[] exampleData = { 15 };
        PlanarImage newAlphaChannel = (PlanarImage) getSyntheticUniformTypeImage(exampleData, null,
                DataBuffer.TYPE_INT, 3, DataDisplacement.DATA_DATA, true, false);
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
        int index = 1;
        exceptionMapRectBody(null, index, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExceptionImagesMapDestRectZeroIndex() {
        Rectangle testRect = new Rectangle(0, 0, 10, 10);
        int index = -1;
        exceptionMapRectBody(testRect, index, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExceptionImagesMapDestRectOverIndex() {
        Rectangle testRect = new Rectangle(0, 0, 10, 10);
        int index = 10;
        exceptionMapRectBody(testRect, index, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExceptionImagesMapSourceRectNullSource() {
        int index = 1;
        exceptionMapRectBody(null, index, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExceptionImagesMapSourceRectZeroIndex() {
        Rectangle testRect = new Rectangle(0, 0, 10, 10);
        int index = -1;
        exceptionMapRectBody(testRect, index, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExceptionImagesMapSourceRectOverIndex() {
        Rectangle testRect = new Rectangle(0, 0, 10, 10);
        int index = 10;
        exceptionMapRectBody(testRect, index, true);
    }

    private void exceptionMapRectBody(Rectangle testRect, int index, boolean sourceRect) {
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

        if (sourceRect) {
            Rectangle rect = new MosaicNoDataOpImage(mosaicList, layout, renderingHints, helpBean,
                    DEFAULT_MOSAIC_TYPE, destinationValues).mapSourceRect(testRect, index);
        } else {
            Rectangle rect = new MosaicNoDataOpImage(mosaicList, layout, renderingHints, helpBean,
                    DEFAULT_MOSAIC_TYPE, destinationValues).mapDestRect(testRect, index);
        }

    }

    private void testMethodBody(MosaicBean testBean, int dataType, Number firstValue) {
        testMethodBody(testBean, dataType, firstValue, null, null, null, null, null, false, false,
                false);
    }

    private void testMethodBody(MosaicBean testBean, int dataType, Number firstValue,
            Number secondValue, Number thirdValue, boolean roiused, boolean alphaused) {
        testMethodBody(testBean, dataType, firstValue, secondValue, thirdValue, null, null, null,
                roiused, alphaused, false);
    }

    private void testMethodBody(MosaicBean testBean, int dataType, Number firstValue,
            Number secondValue, Number thirdValue, Number fourthValue, Number fifthValue,
            Number sixthValue, boolean roiused, boolean alphaused, boolean overlay) {
        // Data initialization
        Number[][] arrayPixel = null;
        Number[][] arrayPixel3Band = null;

        if (fourthValue == null) {
            // Test for image with 1 band
            Number[][] arrayPixelArr = { testExecution(testBean, 1) };
            // Test for image with 3 band
            Number[][] arrayPixel3BandArr = { testExecution(testBean, 3) };
            // simple way for saving the returned values in the same array
            arrayPixel = arrayPixelArr;
            arrayPixel3Band = arrayPixel3BandArr;
        }
        if (firstValue == null && secondValue == null) {
            // Check if the pixel has the correct value
            assertEquality(0, arrayPixel, dataType, testBean.getDestinationNoData()[0]);
            // Check if the pixel has the correct value
            assertEquality(0, arrayPixel3Band, dataType, testBean.getDestinationNoData()[0]);
            assertEquality(1, arrayPixel3Band, dataType, testBean.getDestinationNoData()[1]);
            assertEquality(2, arrayPixel3Band, dataType, testBean.getDestinationNoData()[2]);
        } else {
            if (fourthValue != null) {
                // Test for image with 1 band
                arrayPixel = testExecution3Image(testBean, 1, overlay);
                // Test for image with 3 band
                arrayPixel3Band = testExecution3Image(testBean, 3, overlay);

                if (!overlay) {
                    double centerValue = (double) ((secondValue.doubleValue() + sixthValue
                            .doubleValue()) / 2);
                    if (dataType < 4) {
                        centerValue += 1;
                    }
                    fifthValue = centerValue;
                }
                if (firstValue == null && thirdValue == null) {
                    // Check if the pixel has the correct value
                    assertEquality(0, arrayPixel, dataType, testBean.getDestinationNoData()[0],
                            secondValue, testBean.getDestinationNoData()[0], fourthValue,
                            fifthValue, sixthValue);
                    // Check if the pixel has the correct value
                    assertEquality(0, arrayPixel3Band, dataType,
                            testBean.getDestinationNoData()[0], secondValue,
                            testBean.getDestinationNoData()[0], fourthValue, fifthValue, sixthValue);
                    assertEquality(1, arrayPixel3Band, dataType,
                            testBean.getDestinationNoData()[1], secondValue,
                            testBean.getDestinationNoData()[1], fourthValue, fifthValue, sixthValue);
                    assertEquality(2, arrayPixel3Band, dataType,
                            testBean.getDestinationNoData()[2], secondValue,
                            testBean.getDestinationNoData()[2], fourthValue, fifthValue, sixthValue);
                } else {
                    // Check if the pixel has the correct value
                    assertEquality(0, arrayPixel, dataType, firstValue, secondValue, thirdValue,
                            fourthValue, fifthValue, sixthValue);
                    // Check if the pixel has the correct value
                    assertEquality(0, arrayPixel3Band, dataType, firstValue, secondValue,
                            thirdValue, fourthValue, fifthValue, sixthValue);
                    assertEquality(1, arrayPixel3Band, dataType, firstValue, secondValue,
                            thirdValue, fourthValue, fifthValue, sixthValue);
                    assertEquality(2, arrayPixel3Band, dataType, firstValue, secondValue,
                            thirdValue, fourthValue, fifthValue, sixthValue);
                }
            } else {
                if (secondValue != null) {
                    if (roiused) {
                        // Test for image with 1 band
                        arrayPixel = testExecutionROI(testBean, 1);
                        // Test for image with 3 band
                        arrayPixel3Band = testExecutionROI(testBean, 3);
                    } else if (alphaused) {
                        // Test for image with 1 band
                        arrayPixel = testExecutionAlpha(testBean, 1);
                        // Test for image with 3 band
                        arrayPixel3Band = testExecutionAlpha(testBean, 3);
                    }
                    if (firstValue == null) {
                        // Check if the pixel has the correct value
                        assertEquality(0, arrayPixel, dataType, testBean.getDestinationNoData()[0],
                                secondValue, thirdValue);
                        // Check if the pixel has the correct value
                        assertEquality(0, arrayPixel3Band, dataType,
                                testBean.getDestinationNoData()[0], secondValue, thirdValue);
                        assertEquality(1, arrayPixel3Band, dataType,
                                testBean.getDestinationNoData()[0], secondValue, thirdValue);
                        assertEquality(2, arrayPixel3Band, dataType,
                                testBean.getDestinationNoData()[0], secondValue, thirdValue);
                    } else {
                        // Check if the pixel has the correct value
                        assertEquality(0, arrayPixel, dataType, firstValue, secondValue, thirdValue);
                        // Check if the pixel has the correct value
                        assertEquality(0, arrayPixel3Band, dataType, firstValue, secondValue,
                                thirdValue);
                        assertEquality(1, arrayPixel3Band, dataType, firstValue, secondValue,
                                thirdValue);
                        assertEquality(2, arrayPixel3Band, dataType, firstValue, secondValue,
                                thirdValue);
                    }
                } else {
                    assertEquality(0, arrayPixel, dataType, firstValue);
                    // Check if the pixel has the correct value
                    assertEquality(0, arrayPixel3Band, dataType, firstValue);
                    assertEquality(1, arrayPixel3Band, dataType, firstValue);
                    assertEquality(2, arrayPixel3Band, dataType, firstValue);
                }
            }
        }
    }

    private void assertEquality(int bandIndex, Number[][] arrayPixel, int dataType,
            Number firstValue) {
        assertEquality(bandIndex, arrayPixel, dataType, firstValue, null, null, null, null, null);
    }

    private void assertEquality(int bandIndex, Number[][] arrayPixel, int dataType,
            Number firstValue, Number secondValue, Number thirdValue) {
        assertEquality(bandIndex, arrayPixel, dataType, firstValue, secondValue, thirdValue, null,
                null, null);
    }

    private void assertEquality(int bandIndex, Number[][] arrayPixel, int dataType,
            Number firstValue, Number secondValue, Number thirdValue, Number fourthValue,
            Number fifthValue, Number sixthValue) {

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            // Position: if one data = center/if 3 data = upper left/ if 6 data = upper left
            if (bandIndex > 0 && secondValue == null) {
                assertEquals(firstValue.byteValue(), arrayPixel[0][bandIndex].byteValue());
            } else {
                assertEquals(firstValue.byteValue(), arrayPixel[bandIndex][0].byteValue());
            }
            if (secondValue != null) {
                // Position: if 3 data = upper center left/ if 6 data = upper center
                assertEquals(secondValue.byteValue(), arrayPixel[bandIndex][1].byteValue());
                // Position: if 3 data = upper center/ if 6 data = upper right
                assertEquals(thirdValue.byteValue(), arrayPixel[bandIndex][2].byteValue());
            }
            if (fourthValue != null) {
                // Position: center left
                assertEquals(fourthValue.byteValue(), arrayPixel[bandIndex][3].byteValue());
                // Position: center
                assertEquals(fifthValue.byteValue(), arrayPixel[bandIndex][4].byteValue());
                // Position: lower left
                assertEquals(sixthValue.byteValue(), arrayPixel[bandIndex][5].byteValue());
            }
            break;
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            if (bandIndex > 0 && secondValue == null) {
                assertEquals(firstValue.shortValue(), arrayPixel[0][bandIndex].shortValue());
            } else {
                assertEquals(firstValue.shortValue(), arrayPixel[bandIndex][0].shortValue());
            }
            if (secondValue != null) {
                assertEquals(secondValue.shortValue(), arrayPixel[bandIndex][1].shortValue());
                assertEquals(thirdValue.shortValue(), arrayPixel[bandIndex][2].shortValue());
            }
            if (fourthValue != null) {
                assertEquals(fourthValue.shortValue(), arrayPixel[bandIndex][3].shortValue());
                assertEquals(fifthValue.shortValue(), arrayPixel[bandIndex][4].shortValue());
                assertEquals(sixthValue.shortValue(), arrayPixel[bandIndex][5].shortValue());
            }
            break;
        case DataBuffer.TYPE_INT:
            if (bandIndex > 0 && secondValue == null) {
                assertEquals(firstValue.intValue(), arrayPixel[0][bandIndex].intValue());
            } else {
                assertEquals(firstValue.intValue(), arrayPixel[bandIndex][0].intValue());
            }
            if (secondValue != null) {
                assertEquals(secondValue.intValue(), arrayPixel[bandIndex][1].intValue());
                assertEquals(thirdValue.intValue(), arrayPixel[bandIndex][2].intValue());
            }
            if (fourthValue != null) {
                assertEquals(fourthValue.intValue(), arrayPixel[bandIndex][3].intValue());
                assertEquals(fifthValue.intValue(), arrayPixel[bandIndex][4].intValue());
                assertEquals(sixthValue.intValue(), arrayPixel[bandIndex][5].intValue());
            }
            break;
        case DataBuffer.TYPE_FLOAT:
            if (bandIndex > 0 && secondValue == null) {
                assertEquals(firstValue.floatValue(), arrayPixel[0][bandIndex].floatValue(),
                        DEFAULT_DELTA);
            } else {
                assertEquals(firstValue.floatValue(), arrayPixel[bandIndex][0].floatValue(),
                        DEFAULT_DELTA);
            }
            if (secondValue != null) {
                assertEquals(secondValue.floatValue(), arrayPixel[bandIndex][1].floatValue(),
                        DEFAULT_DELTA);
                assertEquals(thirdValue.floatValue(), arrayPixel[bandIndex][2].floatValue(),
                        DEFAULT_DELTA);
            }
            if (fourthValue != null) {
                assertEquals(fourthValue.floatValue(), arrayPixel[bandIndex][3].floatValue(),
                        DEFAULT_DELTA);
                assertEquals(fifthValue.floatValue(), arrayPixel[bandIndex][4].floatValue(),
                        DEFAULT_DELTA);
                assertEquals(sixthValue.floatValue(), arrayPixel[bandIndex][5].floatValue(),
                        DEFAULT_DELTA);
            }
            break;
        case DataBuffer.TYPE_DOUBLE:
            if (bandIndex > 0 && secondValue == null) {
                assertEquals(firstValue.doubleValue(), arrayPixel[0][bandIndex].doubleValue(),
                        DEFAULT_DELTA);
            } else {
                assertEquals(firstValue.doubleValue(), arrayPixel[bandIndex][0].doubleValue(),
                        DEFAULT_DELTA);
            }
            if (secondValue != null) {
                assertEquals(secondValue.doubleValue(), arrayPixel[bandIndex][1].doubleValue(),
                        DEFAULT_DELTA);
                assertEquals(thirdValue.doubleValue(), arrayPixel[bandIndex][2].doubleValue(),
                        DEFAULT_DELTA);
            }
            if (fourthValue != null) {
                assertEquals(fourthValue.doubleValue(), arrayPixel[bandIndex][3].doubleValue(),
                        DEFAULT_DELTA);
                assertEquals(fifthValue.doubleValue(), arrayPixel[bandIndex][4].doubleValue(),
                        DEFAULT_DELTA);
                assertEquals(sixthValue.doubleValue(), arrayPixel[bandIndex][5].doubleValue(),
                        DEFAULT_DELTA);
            }
            break;
        default:
            break;
        }
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
            // method for image creation
            beanContainer3Images[i] = imageSettings3Images(validData, sourceND, destinationND, i,
                    roiAlphaNotNull);
        }
    }

    private MosaicBean imageSettings3Images(Number[] validData, Number[] sourceNodata,
            Number[] destinationNoDataValue, int dataType, boolean roiAlphaNotNull) {

        MosaicBean tempMosaicBean = new MosaicBean();

        RenderedImage image1 = null;

        RenderedImage image3Band1 = null;

        if (sourceNodata[0] == null) {
            image1 = getSyntheticUniformTypeImage(validData, sourceNodata, dataType, 1,
                    DataDisplacement.DATA_DATA, true, false);
            image3Band1 = getSyntheticUniformTypeImage(validData, sourceNodata, dataType, 3,
                    DataDisplacement.DATA_DATA, true, false);
        } else {

            image1 = getSyntheticUniformTypeImage(validData, sourceNodata, dataType, 1,
                    DataDisplacement.NODATA_DATA, true, false);
            image3Band1 = getSyntheticUniformTypeImage(validData, sourceNodata, dataType, 3,
                    DataDisplacement.NODATA_DATA, true, false);
        }
        RenderedImage image2 = getSyntheticUniformTypeImage(validData, sourceNodata, dataType, 1,
                DataDisplacement.DATA_DATA, false, false);
        RenderedImage image3 = getSyntheticUniformTypeImage(validData, sourceNodata, dataType, 1,
                DataDisplacement.DATA_DATA, false, true);
        RenderedImage image3Band2 = getSyntheticUniformTypeImage(validData, sourceNodata, dataType,
                3, DataDisplacement.DATA_DATA, false, false);
        RenderedImage image3Band3 = getSyntheticUniformTypeImage(validData, sourceNodata, dataType,
                3, DataDisplacement.DATA_DATA, false, true);

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
                    Number[] alphaChannelData = { 30 };
                    PlanarImage alphaChannel = (PlanarImage) getSyntheticUniformTypeImage(
                            alphaChannelData, null, dataType, 1, DataDisplacement.DATA_DATA, true,
                            false);
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
        RenderedImage image5 = null;
        RenderedImage[] sourceArray = null;
        // start image array
        if (numBands == 3) {
            sourceArray = testBean.getImage3Bands();
        } else {
            sourceArray = testBean.getImage();
        }
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

        if (overlay) {
            // MosaicNoData operation
            image5 = MosaicNoDataDescriptor.create(mosaicArray, helpBean, DEFAULT_MOSAIC_TYPE,
                    destinationValues, hints);
        } else {
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

        // Raster Array
        Raster[] rasterArray = new Raster[6];

        // Selection of the upper left image tile
        rasterArray[0] = image5.getTile(minXTileCoord, minYTileCoord);
        // Selection of the upperCenter image tile
        rasterArray[1] = image5.getTile(medXCoord12, minYTileCoord);
        // Selection of the upperRight image tile
        rasterArray[2] = image5.getTile(medXCoord2, minYTileCoord);
        // Selection of the centerLeft image tile
        rasterArray[3] = image5.getTile(minXTileCoord, medYCoord13);
        // selection of a tile in the middle of the total image
        rasterArray[4] = image5.getTile(medXCoord12, medYCoord13);
        // Selection of the lowerLeft image tile
        rasterArray[5] = image5.getTile(minXTileCoord, medYCoord3);

        int[] arrayPixelInt = null;
        float[] arrayPixelFloat = null;
        double[] arrayPixelDouble = null;

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_INT:
            arrayPixelInt = new int[6];
            // first iteration : selection of the first pixel of the first image (no data)
            // second iteration : selection of the pixel in ROI(second image) but no data(first image)
            // third iteration : selection of the pixel not in ROI but in the second image
            // fourth iteration : selection of the pixel in alpha channel(third image) but no data(first image)
            // fifth iteration : selection of the pixel in alpha channel(third image), in ROI(second image) but no data(first image)
            // sixth iteration : selection of the pixel in alpha channel(third image)

            // Iteration around the bands
            for (int i = 0; i < numBands; i++) {
                // Iteration around the image
                for (int j = 0; j < 6; j++) {
                    arrayPixel[i][j] = rasterArray[j].getPixel(rasterArray[j].getMinX(),
                            rasterArray[j].getMinY(), arrayPixelInt)[i];
                }
            }
            break;
        case DataBuffer.TYPE_FLOAT:
            arrayPixelFloat = new float[6];
            // Iteration around the bands
            for (int i = 0; i < numBands; i++) {
                // Iteration around the image
                for (int j = 0; j < 6; j++) {
                    arrayPixel[i][j] = rasterArray[j].getPixel(rasterArray[j].getMinX(),
                            rasterArray[j].getMinY(), arrayPixelFloat)[i];
                }
            }
            break;
        case DataBuffer.TYPE_DOUBLE:
            arrayPixelDouble = new double[3];
            // Iteration around the bands
            for (int i = 0; i < numBands; i++) {
                // Iteration around the image
                for (int j = 0; j < 6; j++) {
                    arrayPixel[i][j] = rasterArray[j].getPixel(rasterArray[j].getMinX(),
                            rasterArray[j].getMinY(), arrayPixelDouble)[i];
                }
            }
            break;
        }
        return arrayPixel;
    }

    private Number[][] testExecutionAlpha(MosaicBean testBean, int numBands) {
        // Pre-allocated array for saving the pixel values
        Number[][] arrayPixel = new Number[3][3];

        RenderedImage[] sourceArray = null;
        // old image array
        if (numBands == 3) {
            sourceArray = testBean.getImage3Bands();
        } else {
            sourceArray = testBean.getImage();
        }
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

        // RasterArray
        Raster[] rasterArray = new Raster[3];

        // Selection of the first image tile
        rasterArray[0] = image5.getTile(0, 0);
        // selection of a tile in the middle of the total image
        rasterArray[1] = image5.getTile((maxXTile + 1) / 2 - 1, minYTile);
        // selection of the last tile of the second image
        rasterArray[2] = image5.getTile((maxXTile + 1) / 2 - 1 + (maxXTile + 1) / 4, minYTile);
        // X coordinates array
        int[] coordinateXarray = new int[3];
        coordinateXarray[0] = 0;
        int tileCenterWidth = rasterArray[1].getWidth();
        // selection of the first pixel of the tile
        coordinateXarray[1] = rasterArray[1].getMinX() + (tileCenterWidth / 2);
        // int minYCoordCenter = rasterArray[1].getMinY();

        int tileRightSecondImWidth = rasterArray[2].getWidth();
        // selection of the last pixel of the tile
        coordinateXarray[2] = rasterArray[2].getMinX() + tileRightSecondImWidth - 1;
        // int minYCoordRightSecondIm = rasterArray[2].getMinY();

        int[] arrayPixelInt = null;
        float[] arrayPixelFloat = null;
        double[] arrayPixelDouble = null;

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_INT:
            arrayPixelInt = new int[3];
            // first iteration : selection of the first pixel of the image
            // second iteration : selection of the pixel in the second ROI but not in the first ROI
            // third iteration : selection of the last pixel

            // Iteration around the bands
            for (int i = 0; i < numBands; i++) {
                // Iteration around the image
                for (int j = 0; j < 3; j++) {
                    arrayPixel[i][j] = rasterArray[j].getPixel(coordinateXarray[j], 0,
                            arrayPixelInt)[i];
                }
            }
            break;
        case DataBuffer.TYPE_FLOAT:
            arrayPixelFloat = new float[3];
            // Iteration around the bands
            for (int i = 0; i < numBands; i++) {
                // Iteration around the image
                for (int j = 0; j < 3; j++) {
                    arrayPixel[i][j] = rasterArray[j].getPixel(coordinateXarray[j], 0,
                            arrayPixelFloat)[i];
                }
            }
            break;
        case DataBuffer.TYPE_DOUBLE:
            arrayPixelDouble = new double[3];
            // Iteration around the bands
            for (int i = 0; i < numBands; i++) {
                // Iteration around the image
                for (int j = 0; j < 3; j++) {
                    arrayPixel[i][j] = rasterArray[j].getPixel(coordinateXarray[j], 0,
                            arrayPixelDouble)[i];
                }
            }
            break;
        }
        return arrayPixel;
    }

    private Number[] testExecution(MosaicBean testBean, int numBands) {
        // Pre-allocated array for saving the pixel values
        Number[] arrayPixel = new Number[3];
        // start image array
        RenderedImage[] sourceArray = null;
        if (numBands == 3) {
            sourceArray = testBean.getImage3Bands();
        } else {
            sourceArray = testBean.getImage();
        }
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

        // Raster dimension in pixel
        int numXPixel = upperCenter.getWidth();
        // Raster last pixel coordinates

        int mediumXPixelCoord = minXCoord + (numXPixel / 2);

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
            // Iteration around the bands
            for (int i = 0; i < numBands; i++) {
                arrayPixel[i] = upperCenter.getPixel(mediumXPixelCoord, 0, arrayPixelInt)[i];
            }
            break;
        case DataBuffer.TYPE_FLOAT:
            arrayPixelFloat = new float[3];
            // Iteration around the bands
            for (int i = 0; i < numBands; i++) {
                arrayPixel[i] = upperCenter.getPixel(mediumXPixelCoord, 0, arrayPixelFloat)[i];
            }
            break;
        case DataBuffer.TYPE_DOUBLE:
            arrayPixelDouble = new double[3];
            // Iteration around the bands
            for (int i = 0; i < numBands; i++) {
                arrayPixel[i] = upperCenter.getPixel(mediumXPixelCoord, 0, arrayPixelDouble)[i];
            }
            break;
        }
        return arrayPixel;
    }

    private Number[][] testExecutionROI(MosaicBean testBean, int numBands) {
        // Pre-allocated array for saving the pixel values
        Number[][] arrayPixel = new Number[3][3];

        RenderedImage[] sourceArray = null;
        if (numBands == 3) {
            sourceArray = testBean.getImage3Bands();
        } else {
            sourceArray = testBean.getImage();
        }
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

        // RasterArray
        Raster[] rasterArray = new Raster[3];

        // Selection of the first image tile
        rasterArray[0] = image5.getTile(0, 0);
        // selection of a tile in the middle of the first image
        rasterArray[1] = image5.getTile((maxXTile / 4) + 1, minYTile);
        // selection of the center tile
        rasterArray[2] = image5.getTile(((maxXTile + 1) / 2) - 1, minYTile);

        // X coordinates array
        int[] coordinateXarray = new int[3];
        coordinateXarray[0] = 0;
        // selection of the first pixel of the second tile
        coordinateXarray[1] = rasterArray[1].getMinX();

        int tileWidth = rasterArray[2].getWidth();
        // selection of the first pixel of the third tile
        coordinateXarray[2] = rasterArray[2].getMinX() + tileWidth - 1;

        int[] arrayPixelInt = null;
        float[] arrayPixelFloat = null;
        double[] arrayPixelDouble = null;

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_INT:
            arrayPixelInt = new int[3];
            // first iteration : selection of the first pixel of the image
            // second iteration : selection of the first pixel in the middle of the first image
            // third iteration : selection of the first pixel of the center tile of the global image

            // Iteration around the bands
            for (int i = 0; i < numBands; i++) {
                // Iteration around the image
                for (int j = 0; j < 3; j++) {
                    arrayPixel[i][j] = rasterArray[j].getPixel(coordinateXarray[j], 0,
                            arrayPixelInt)[i];
                }
            }
            break;
        case DataBuffer.TYPE_FLOAT:
            arrayPixelFloat = new float[3];
            // Iteration around the bands
            for (int i = 0; i < numBands; i++) {
                // Iteration around the image
                for (int j = 0; j < 3; j++) {
                    arrayPixel[i][j] = rasterArray[j].getPixel(coordinateXarray[j], 0,
                            arrayPixelFloat)[i];
                }
            }
            break;
        case DataBuffer.TYPE_DOUBLE:
            arrayPixelDouble = new double[3];
            // Iteration around the bands
            for (int i = 0; i < numBands; i++) {
                // Iteration around the image
                for (int j = 0; j < 3; j++) {
                    arrayPixel[i][j] = rasterArray[j].getPixel(coordinateXarray[j], 0,
                            arrayPixelDouble)[i];
                }
            }
            break;
        }
        return arrayPixel;
    }

    private void creationAlpha() {

        Number[] alphaChannelData = new Number[2];
        alphaChannelData[0] = 30;
        alphaChannelData[1] = 70;

        PlanarImage alphaChannel0 = null;
        PlanarImage alphaChannel1 = null;
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 3; j++) {
                alphaChannel0 = (PlanarImage) getSyntheticUniformTypeImage(alphaChannelData, null,
                        i, 1, DataDisplacement.DATA_DATA, true, false);
                alphaChannel1 = (PlanarImage) getSyntheticUniformTypeImage(alphaChannelData, null,
                        i, 1, DataDisplacement.DATA_DATA, false, false);

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

            }
        }

    }

    private void creationROI() {

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
            }
        }
    }

    private void initalSetup() {
        // creates all the image per datatype and with the
        // combination(nodata-nodata,data-nodata,data-data)
        beanContainer = new MosaicBean[6][3];
        Number[] sourceND = new Number[2];
        Number[] validData = new Number[2];
        Number[] destinationND = new Number[3];
        // source no data values
        sourceND[0] = 50;
        sourceND[1] = 50;
        // source possible valid data
        validData[0] = 15;
        validData[1] = 80;
        // destination no data
        destinationND[0] = 100;
        destinationND[1] = 100;
        destinationND[2] = 100;

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 3; j++) {
                // method for image creation
                beanContainer[i][j] = imageSettings(validData, sourceND, destinationND, i, j);
            }
        }

    }

    private MosaicBean imageSettings(Number[] validData, Number[] sourceNodata,
            Number[] destinationNoDataValue, int dataType, int imageDataNoData) {

        DataDisplacement dd = DataDisplacement.values()[imageDataNoData];

        MosaicBean tempMosaicBean = new MosaicBean();

        // no data-no data // no data- data // data-data
        RenderedImage image1 = getSyntheticUniformTypeImage(validData, sourceNodata, dataType, 1,
                dd, true, false);
        RenderedImage image2 = getSyntheticUniformTypeImage(validData, sourceNodata, dataType, 1,
                dd, false, false);
        RenderedImage image3Band1 = getSyntheticUniformTypeImage(validData, sourceNodata, dataType,
                3, dd, true, false);
        RenderedImage image3Band2 = getSyntheticUniformTypeImage(validData, sourceNodata, dataType,
                3, dd, false, false);

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
    public static RenderedImage getSyntheticUniformTypeImage(Number[] valueData,
            Number[] valueNoData, int dataType, int numBands, DataDisplacement dd,
            boolean isFirstImage, boolean isLastOf3) {

        Number value = 0;

        switch (dd) {
        case DATA_DATA:
            if (isFirstImage) {
                value = valueData[0];
            } else {
                value = valueData[1];
            }
            break;
        case NODATA_DATA:
            if (isFirstImage) {
                value = valueNoData[0];
            } else {
                value = valueData[1];
            }
            break;
        case NODATA_NODATA:
            if (isFirstImage) {
                value = valueNoData[0];
            } else {
                value = valueNoData[1];
            }
            break;
        default:
            break;
        }

        if (valueData.length == 3 && isLastOf3) {
            value = valueData[2];
        }

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
