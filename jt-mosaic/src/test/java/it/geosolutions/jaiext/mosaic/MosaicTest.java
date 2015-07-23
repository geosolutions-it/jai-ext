/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2014 - 2015 GeoSolutions


 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.geosolutions.jaiext.mosaic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.Serializable;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.MosaicType;
import javax.media.jai.operator.TranslateDescriptor;
import javax.media.jai.util.ImagingException;

import org.junit.Test;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;

/**
 * This test class is used for checking the functionality of the MosaicOpImage.
 * Every test is performed on all the DataBuffer types. The mosaic operations
 * are calculated in the OVERLAY mode. The first 3 series of tests execute a
 * mosaic operation between:
 * <ul>
 * <li>two image with No Data values</li>
 * <li>two image with the first having No Data values and the second having
 * valid Data values</li>
 * <li>two image with valid Data values</li>
 * </ul>
 * 
 * The 4th and 5th series make the same tests but using a ROI. The only
 * difference is that two images with No Data values are not compared. The 6th
 * and 7th series make the same ROI tests but using a Alpha channel. The 8th
 * series performs a test on 3 images which the first image contains only No
 * Data values, the second image uses a ROI, and the last image has an alpha
 * channel. The 9th series performs a test on 3 images in the same above
 * condition but the mosaic type is BLEND, not OVERLAY. The 10th series executes
 * a simple test on 3 images with only data values. The 11th series checks that
 * all the Exceptions are thrown in the right conditions.
 * 
 * <ul>
 * </ul>
 * The other surrounding methods belongs to 2 categories:
 * <ul>
 * <li>test methods which execute the tests and check if the calculated values
 * are correct</li>
 * <li>help methods for creating all the images and ROI with different
 * structures</li>
 * </ul>
 */

public class MosaicTest extends TestBase {

	// Default initialization set to false
	public final static boolean DEFAULT_SETUP_INITALIZATION = false;

	private final static MosaicType DEFAULT_MOSAIC_TYPE = javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_OVERLAY;

	// the default image dimensions;
	public final static float DEFAULT_WIDTH = 512;

	public final static float DEFAULT_HEIGTH = 512;

	public final static Logger LOGGER = Logger.getLogger(MosaicTest.class
			.toString());

	// default tolerance for comparison
	public final static double DEFAULT_DELTA = 1.5d;

	private static RenderingHints hints;

	public enum DataDisplacement {
		NODATA_NODATA, NODATA_DATA, DATA_DATA;
	}

	// THIS TESTS ARE WITH 2 IMAGES: THE FIRST IS IN THE LEFT SIDE AND THE
	// SECOND IS TRANSLATED
	// ON THE RIGHT BY HALF OF ITS WIDTH (HALF IMAGE OVERLAY)

	// FIRST SERIES
	// Test No Data: image 1 and 2 only no data
	@Test
	public void testByteNoData1NoData2() {
		int dataType = DataBuffer.TYPE_BYTE;
		TestBean testBean = createBean(dataType, 0, false, false);
		testMethodBody(testBean, dataType, null);

	}

	@Test
	public void testUShortNoData1NoData2() {
		int dataType = DataBuffer.TYPE_USHORT;
		TestBean testBean = createBean(dataType, 0, false, false);
		testMethodBody(testBean, dataType, null);
	}

	@Test
	public void testShortNoData1NoData2() {
		int dataType = DataBuffer.TYPE_SHORT;
		TestBean testBean = createBean(dataType, 0, false, false);
		testMethodBody(testBean, dataType, null);
	}

	@Test
	public void testIntNoData1NoData2() {
		int dataType = DataBuffer.TYPE_INT;
		TestBean testBean = createBean(dataType, 0, false, false);
		testMethodBody(testBean, dataType, null);
	}

	@Test
	public void testFloatNoData1NoData2() {
		int dataType = DataBuffer.TYPE_FLOAT;
		TestBean testBean = createBean(dataType, 0, false, false);
		testMethodBody(testBean, dataType, null);
	}

	@Test
	public void testDoubleNoData1NoData2() {
		int dataType = DataBuffer.TYPE_DOUBLE;
		TestBean testBean = createBean(dataType, 0, false, false);
		testMethodBody(testBean, dataType, null);
	}

	// SECOND SERIES
	// Test No Data: image 1 with data and image 2 with no data
	@Test
	public void testByteNoData1Data2() {
		int dataType = DataBuffer.TYPE_BYTE;
		TestBean testBean = createBean(dataType, 1, false, false);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, valideValueSource2);
	}

	@Test
	public void testUShortNoData1Data2() {
		int dataType = DataBuffer.TYPE_USHORT;
		TestBean testBean = createBean(dataType, 1, false, false);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, valideValueSource2);
	}

	@Test
	public void testShortNoData1Data2() {
		int dataType = DataBuffer.TYPE_SHORT;
		TestBean testBean = createBean(dataType, 1, false, false);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, valideValueSource2);
	}

	@Test
	public void testIntNoData1Data2() {
		int dataType = DataBuffer.TYPE_INT;
		TestBean testBean = createBean(dataType, 1, false, false);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, valideValueSource2);
	}

	@Test
	public void testFloatNoData1Data2() {
		int dataType = DataBuffer.TYPE_FLOAT;
		TestBean testBean = createBean(dataType, 1, false, false);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, valideValueSource2);
	}

	@Test
	public void testDoubleNoData1Data2() {
		int dataType = DataBuffer.TYPE_DOUBLE;
		TestBean testBean = createBean(dataType, 1, false, false);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, valideValueSource2);
	}

	// THIRD SERIES
	// Test No data: image 1 and 2 with data
	@Test
	public void testByteData1Data2() {
		int dataType = DataBuffer.TYPE_BYTE;
		TestBean testBean = createBean(dataType, 2, false, false);
		Number valideValueSource1 = testBean.getSourceValidData()[0];
		testMethodBody(testBean, dataType, valideValueSource1);
	}

	@Test
	public void testUShortData1Data2() {
		int dataType = DataBuffer.TYPE_USHORT;
		TestBean testBean = createBean(dataType, 2, false, false);
		Number valideValueSource1 = testBean.getSourceValidData()[0];
		testMethodBody(testBean, dataType, valideValueSource1);
	}

	@Test
	public void testShortData1Data2() {
		int dataType = DataBuffer.TYPE_SHORT;
		TestBean testBean = createBean(dataType, 2, false, false);
		Number valideValueSource1 = testBean.getSourceValidData()[0];
		testMethodBody(testBean, dataType, valideValueSource1);
	}

	@Test
	public void testIntData1Data2() {
		int dataType = DataBuffer.TYPE_INT;
		TestBean testBean = createBean(dataType, 2, false, false);
		Number valideValueSource1 = testBean.getSourceValidData()[0];
		testMethodBody(testBean, dataType, valideValueSource1);
	}

	@Test
	public void testFloatData1Data2() {
		int dataType = DataBuffer.TYPE_FLOAT;
		TestBean testBean = createBean(dataType, 2, false, false);
		Number valideValueSource1 = testBean.getSourceValidData()[0];
		testMethodBody(testBean, dataType, valideValueSource1);
	}

	@Test
	public void testDoubleData1Data2() {
		int dataType = DataBuffer.TYPE_DOUBLE;
		TestBean testBean = createBean(dataType, 2, false, false);
		Number valideValueSource1 = testBean.getSourceValidData()[0];
		testMethodBody(testBean, dataType, valideValueSource1);
	}

	// FOURTH SERIES
	// Test ROI: image 1 and 2 with data
	@Test
	public void testROIByteData1Data2() {
		int dataType = DataBuffer.TYPE_BYTE;
		TestBean testBean = createBean(dataType, 2, true, false);
		Number valideValueSource1 = testBean.getSourceValidData()[0];
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, null, valideValueSource2,
				valideValueSource1, true, false);
	}

	@Test
	public void testROIUShortData1Data2() {
		int dataType = DataBuffer.TYPE_USHORT;
		TestBean testBean = createBean(dataType, 2, true, false);
		Number valideValueSource1 = testBean.getSourceValidData()[0];
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, null, valideValueSource2,
				valideValueSource1, true, false);
	}

	@Test
	public void testROIShortData1Data2() {
		int dataType = DataBuffer.TYPE_SHORT;
		TestBean testBean = createBean(dataType, 2, true, false);
		Number valideValueSource1 = testBean.getSourceValidData()[0];
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, null, valideValueSource2,
				valideValueSource1, true, false);
	}

	@Test
	public void testROIIntData1Data2() {
		int dataType = DataBuffer.TYPE_INT;
		TestBean testBean = createBean(dataType, 2, true, false);
		Number valideValueSource1 = testBean.getSourceValidData()[0];
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, null, valideValueSource2,
				valideValueSource1, true, false);
	}

	@Test
	public void testROIFloatData1Data2() {
		int dataType = DataBuffer.TYPE_FLOAT;
		TestBean testBean = createBean(dataType, 2, true, false);
		Number valideValueSource1 = testBean.getSourceValidData()[0];
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, null, valideValueSource2,
				valideValueSource1, true, false);
	}

	@Test
	public void testROIDoubleData1Data2() {
		int dataType = DataBuffer.TYPE_DOUBLE;
		TestBean testBean = createBean(dataType, 2, true, false);
		Number valideValueSource1 = testBean.getSourceValidData()[0];
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, null, valideValueSource2,
				valideValueSource1, true, false);
	}

	// FIFTH SERIES
	// Test ROI: image 1 with data and 2 with no data
	@Test
	public void testROIByteNoData1Data2() {
		int dataType = DataBuffer.TYPE_BYTE;
		TestBean testBean = createBean(dataType, 1, true, false);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, null, valideValueSource2,
				valideValueSource2, true, false);
	}

	@Test
	public void testROIUShortNoData1Data2() {
		int dataType = DataBuffer.TYPE_USHORT;
		TestBean testBean = createBean(dataType, 1, true, false);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, null, valideValueSource2,
				valideValueSource2, true, false);
	}

	@Test
	public void testROIShortNoData1Data2() {
		int dataType = DataBuffer.TYPE_SHORT;
		TestBean testBean = createBean(dataType, 1, true, false);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, null, valideValueSource2,
				valideValueSource2, true, false);
	}

	@Test
	public void testROIIntNoData1Data2() {
		int dataType = DataBuffer.TYPE_INT;
		TestBean testBean = createBean(dataType, 1, true, false);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, null, valideValueSource2,
				valideValueSource2, true, false);
	}

	@Test
	public void testROIFloatNoData1Data2() {
		int dataType = DataBuffer.TYPE_FLOAT;
		TestBean testBean = createBean(dataType, 1, true, false);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, null, valideValueSource2,
				valideValueSource2, true, false);
	}

	@Test
	public void testROIDoubleNoData1Data2() {
		int dataType = DataBuffer.TYPE_DOUBLE;
		TestBean testBean = createBean(dataType, 1, true, false);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, null, valideValueSource2,
				valideValueSource2, true, false);
	}

	// SIXTH SERIES
	// Test Alpha: image 1 and image 2 with data
	@Test
	public void testAlphaByteData1Data2() {
		int dataType = DataBuffer.TYPE_BYTE;
		TestBean testBean = createBean(dataType, 2, false, true);
		Number valideValueSource1 = testBean.getSourceValidData()[0];
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, valideValueSource1,
				valideValueSource1, valideValueSource2, false, true);
	}

	@Test
	public void testAlphaUShortData1Data2() {
		int dataType = DataBuffer.TYPE_USHORT;
		TestBean testBean = createBean(dataType, 2, false, true);
		Number valideValueSource1 = testBean.getSourceValidData()[0];
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, valideValueSource1,
				valideValueSource1, valideValueSource2, false, true);
	}

	@Test
	public void testAlphaShortData1Data2() {
		int dataType = DataBuffer.TYPE_SHORT;
		TestBean testBean = createBean(dataType, 2, false, true);
		Number valideValueSource1 = testBean.getSourceValidData()[0];
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, valideValueSource1,
				valideValueSource1, valideValueSource2, false, true);
	}

	@Test
	public void testAlphaIntData1Data2() {
		int dataType = DataBuffer.TYPE_INT;
		TestBean testBean = createBean(dataType, 2, false, true);
		Number valideValueSource1 = testBean.getSourceValidData()[0];
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, valideValueSource1,
				valideValueSource1, valideValueSource2, false, true);
	}

	@Test
	public void testAlphaFloatData1Data2() {
		int dataType = DataBuffer.TYPE_FLOAT;
		TestBean testBean = createBean(dataType, 2, false, true);
		Number valideValueSource1 = testBean.getSourceValidData()[0];
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, valideValueSource1,
				valideValueSource1, valideValueSource2, false, true);
	}

	@Test
	public void testAlphaDoubleData1Data2() {
		int dataType = DataBuffer.TYPE_DOUBLE;
		TestBean testBean = createBean(dataType, 2, false, true);
		Number valideValueSource1 = testBean.getSourceValidData()[0];
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, valideValueSource1,
				valideValueSource1, valideValueSource2, false, true);
	}

	// SEVENTH SERIES
	// Test Alpha: image 1 with no data and image 2 with data
	@Test
	public void testAlphaNoByteData1Data2() {
		int dataType = DataBuffer.TYPE_BYTE;
		TestBean testBean = createBean(dataType, 1, false, true);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, null, valideValueSource2,
				valideValueSource2, false, true);
	}

	@Test
	public void testAlphaNoUShortData1Data2() {
		int dataType = DataBuffer.TYPE_USHORT;
		TestBean testBean = createBean(dataType, 1, false, true);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, null, valideValueSource2,
				valideValueSource2, false, true);
	}

	@Test
	public void testAlphaNoShortData1Data2() {
		int dataType = DataBuffer.TYPE_SHORT;
		TestBean testBean = createBean(dataType, 1, false, true);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, null, valideValueSource2,
				valideValueSource2, false, true);
	}

	@Test
	public void testAlphaIntNoData1Data2() {
		int dataType = DataBuffer.TYPE_INT;
		TestBean testBean = createBean(dataType, 1, false, true);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, null, valideValueSource2,
				valideValueSource2, false, true);
	}

	@Test
	public void testAlphaFloatNoData1Data2() {
		int dataType = DataBuffer.TYPE_FLOAT;
		TestBean testBean = createBean(dataType, 1, false, true);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, null, valideValueSource2,
				valideValueSource2, false, true);
	}

	@Test
	public void testAlphaDoubleNoData1Data2() {
		int dataType = DataBuffer.TYPE_DOUBLE;
		TestBean testBean = createBean(dataType, 1, false, true);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		testMethodBody(testBean, dataType, null, valideValueSource2,
				valideValueSource2, false, true);
	}

	// TEST WITH 3 IMAGES: IMAGE 1 WITH NO DATA IN THE UPPER LEFT, IMAGE 2 WITH
	// ROI IN THE UPPER RIGTH,
	// IMAGE 3 WITH ALPHA CHANNEL IN THE LOWER LEFT
	// EIGHTH SERIES
	@Test
	public void testByte3ImagesNoDataROIAlpha() {
		int dataType = DataBuffer.TYPE_BYTE;
		TestBean testBean = createBean3Images(dataType, false, true);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		Number valideValueSource3 = testBean.getSourceValidData()[2];
		testMethodBody(testBean, dataType, null, valideValueSource2, null,
				valideValueSource3, valideValueSource2, valideValueSource3,
				false, false, true);
	}

	@Test
	public void testUShort3ImagesNoDataROIAlpha() {
		int dataType = DataBuffer.TYPE_USHORT;
		TestBean testBean = createBean3Images(dataType, false, true);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		Number valideValueSource3 = testBean.getSourceValidData()[2];
		testMethodBody(testBean, dataType, null, valideValueSource2, null,
				valideValueSource3, valideValueSource2, valideValueSource3,
				false, false, true);
	}

	@Test
	public void testShort3ImagesNoDataROIAlpha() {
		int dataType = DataBuffer.TYPE_SHORT;
		TestBean testBean = createBean3Images(dataType, false, true);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		Number valideValueSource3 = testBean.getSourceValidData()[2];
		testMethodBody(testBean, dataType, null, valideValueSource2, null,
				valideValueSource3, valideValueSource2, valideValueSource3,
				false, false, true);
	}

	@Test
	public void testInt3ImagesNoDataROIAlpha() {
		int dataType = DataBuffer.TYPE_INT;
		TestBean testBean = createBean3Images(dataType, false, true);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		Number valideValueSource3 = testBean.getSourceValidData()[2];
		testMethodBody(testBean, dataType, null, valideValueSource2, null,
				valideValueSource3, valideValueSource2, valideValueSource3,
				false, false, true);
	}

	@Test
	public void testFloat3ImagesNoDataROIAlpha() {
		int dataType = DataBuffer.TYPE_FLOAT;
		TestBean testBean = createBean3Images(dataType, false, true);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		Number valideValueSource3 = testBean.getSourceValidData()[2];
		testMethodBody(testBean, dataType, null, valideValueSource2, null,
				valideValueSource3, valideValueSource2, valideValueSource3,
				false, false, true);
	}

	@Test
	public void testDouble3ImagesNoDataROIAlpha() {
		int dataType = DataBuffer.TYPE_DOUBLE;
		TestBean testBean = createBean3Images(dataType, false, true);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		Number valideValueSource3 = testBean.getSourceValidData()[2];
		testMethodBody(testBean, dataType, null, valideValueSource2, null,
				valideValueSource3, valideValueSource2, valideValueSource3,
				false, false, true);
	}

	// NINTH SERIES
	@Test
	public void testBLENDByte3Images() {
		int dataType = DataBuffer.TYPE_BYTE;
		TestBean testBean = createBean3Images(dataType, false, true);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		Number valideValueSource3 = testBean.getSourceValidData()[2];
		testMethodBody(testBean, dataType, null, valideValueSource2, null,
				valideValueSource3, valideValueSource2, valideValueSource3,
				false, false, false);
	}

	@Test
	public void testBLENDUShort3Images() {
		int dataType = DataBuffer.TYPE_USHORT;
		TestBean testBean = createBean3Images(dataType, false, true);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		Number valideValueSource3 = testBean.getSourceValidData()[2];
		testMethodBody(testBean, dataType, null, valideValueSource2, null,
				valideValueSource3, valideValueSource2, valideValueSource3,
				false, false, false);
	}

	@Test
	public void testBLENDShort3Images() {
		int dataType = DataBuffer.TYPE_SHORT;
		TestBean testBean = createBean3Images(dataType, false, true);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		Number valideValueSource3 = testBean.getSourceValidData()[2];
		testMethodBody(testBean, dataType, null, valideValueSource2, null,
				valideValueSource3, valideValueSource2, valideValueSource3,
				false, false, false);
	}

	@Test
	public void testBLENDInt3Images() {
		int dataType = DataBuffer.TYPE_INT;
		TestBean testBean = createBean3Images(dataType, false, true);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		Number valideValueSource3 = testBean.getSourceValidData()[2];
		testMethodBody(testBean, dataType, null, valideValueSource2, null,
				valideValueSource3, valideValueSource2, valideValueSource3,
				false, false, false);
	}

	@Test
	public void testBLENDFloat3Images() {
		int dataType = DataBuffer.TYPE_FLOAT;
		TestBean testBean = createBean3Images(dataType, false, true);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		Number valideValueSource3 = testBean.getSourceValidData()[2];
		testMethodBody(testBean, dataType, null, valideValueSource2, null,
				valideValueSource3, valideValueSource2, valideValueSource3,
				false, false, false);
	}

	@Test
	public void testBLENDDouble3Images() {
		int dataType = DataBuffer.TYPE_DOUBLE;
		TestBean testBean = createBean3Images(dataType, false, true);
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		Number valideValueSource3 = testBean.getSourceValidData()[2];
		testMethodBody(testBean, dataType, null, valideValueSource2, null,
				valideValueSource3, valideValueSource2, valideValueSource3,
				false, false, false);
	}

	// TENTH SERIES
	// TEST WITH 3 IMAGES: SAME POSITIONS OF THE THREE IMAGES ABOVE, NULL NO
	// DATA, NO ROI OR ALPHA CHANNEL

	@Test
	public void testByteNullNoData() {
		int dataType = DataBuffer.TYPE_BYTE;
		TestBean testBean = createBean3Images(dataType, true, false);
		Number valideValueSource1 = testBean.getSourceValidData()[0];
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		Number valideValueSource3 = testBean.getSourceValidData()[2];
		testMethodBody(testBean, dataType, valideValueSource1,
				valideValueSource1, valideValueSource2, valideValueSource1,
				valideValueSource1, valideValueSource3, false, false, true);
	}

	@Test
	public void testUShortNullNoData() {
		int dataType = DataBuffer.TYPE_USHORT;
		TestBean testBean = createBean3Images(dataType, true, false);
		Number valideValueSource1 = testBean.getSourceValidData()[0];
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		Number valideValueSource3 = testBean.getSourceValidData()[2];
		testMethodBody(testBean, dataType, valideValueSource1,
				valideValueSource1, valideValueSource2, valideValueSource1,
				valideValueSource1, valideValueSource3, false, false, true);
	}

	@Test
	public void testShortNullNoData() {
		int dataType = DataBuffer.TYPE_SHORT;
		TestBean testBean = createBean3Images(dataType, true, false);
		Number valideValueSource1 = testBean.getSourceValidData()[0];
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		Number valideValueSource3 = testBean.getSourceValidData()[2];
		testMethodBody(testBean, dataType, valideValueSource1,
				valideValueSource1, valideValueSource2, valideValueSource1,
				valideValueSource1, valideValueSource3, false, false, true);
	}

	@Test
	public void testIntNullNoData() {
		int dataType = DataBuffer.TYPE_INT;
		TestBean testBean = createBean3Images(dataType, true, false);
		Number valideValueSource1 = testBean.getSourceValidData()[0];
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		Number valideValueSource3 = testBean.getSourceValidData()[2];
		testMethodBody(testBean, dataType, valideValueSource1,
				valideValueSource1, valideValueSource2, valideValueSource1,
				valideValueSource1, valideValueSource3, false, false, true);
	}

	@Test
	public void testFloatNullNoData() {
		int dataType = DataBuffer.TYPE_FLOAT;
		TestBean testBean = createBean3Images(dataType, true, false);
		Number valideValueSource1 = testBean.getSourceValidData()[0];
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		Number valideValueSource3 = testBean.getSourceValidData()[2];
		testMethodBody(testBean, dataType, valideValueSource1,
				valideValueSource1, valideValueSource2, valideValueSource1,
				valideValueSource1, valideValueSource3, false, false, true);
	}

	@Test
	public void testDoubleNullNoData() {
		int dataType = DataBuffer.TYPE_DOUBLE;
		TestBean testBean = createBean3Images(dataType, true, false);
		Number valideValueSource1 = testBean.getSourceValidData()[0];
		Number valideValueSource2 = testBean.getSourceValidData()[1];
		Number valideValueSource3 = testBean.getSourceValidData()[2];
		testMethodBody(testBean, dataType, valideValueSource1,
				valideValueSource1, valideValueSource2, valideValueSource1,
				valideValueSource1, valideValueSource3, false, false, true);
	}

	// ELEVENTH SERIES
	// EXCEPTION TEST CLASS. THIS TESTS ARE USED FOR CHECKING IF THE MOSAIC
	// NODATA OPIMAGE
	// CLASS THROWS THE SELECTED EXCEPTIONS
	@Test
	public void testExceptionImagesLayoutNotValid() {
		TestBean testBean = createBean3Images(0, true, false);
		// start image array
		RenderedImage[] mosaicArray = new RenderedImage[3];

		// Creates an array for the destination band values
		double[] destinationValues = { testBean.getDestinationNoData()[0] };

		// Getting the rois, alphabands
		PlanarImage[] alphas = testBean.getAlphas();
		ROI[] rois = testBean.getRois();
		Range[] nd = testBean.getSourceNoDataRange3Bands();

		mosaicArray = new RenderedImage[0];

		// MosaicNoData operation
		RenderedImage image5 = MosaicDescriptor.create(mosaicArray,
				DEFAULT_MOSAIC_TYPE, alphas, rois, null, destinationValues, nd,
				null);

		// ensure exception
		boolean exception = false;
		try {
			image5.getTile(0, 0);
		} catch (ImagingException e) {
			LOGGER.log(Level.INFO, "IllegalArgumentException: Layout not valid");
			assertTrue(e.getRootCause().getMessage()
					.contains("Layout not valid"));
			exception = true;
		}
		// check the exception
		assertTrue(exception);

	}

	@Test
	public void testExceptionImagesNoSampleModelPresent() {

		ImageLayout layout = new ImageLayout();

		layout.setValid(ImageLayout.WIDTH_MASK);
		layout.setValid(ImageLayout.HEIGHT_MASK);
		layout.setValid(ImageLayout.SAMPLE_MODEL_MASK);
		layout.setWidth(4);
		layout.setHeight(4);

		TestBean testBean = createBean3Images(0, true, false);
		// start image array
		RenderedImage[] mosaicArray = new RenderedImage[3];

		// Creates an array for the destination band values
		double[] destinationValues = { testBean.getDestinationNoData()[0] };

		// Setting of the new RenderingHints
		RenderingHints renderingHints = new RenderingHints(
				JAI.KEY_IMAGE_LAYOUT, layout);

		// Getting the rois, alphabands
		PlanarImage[] alphas = testBean.getAlphas();
		ROI[] rois = testBean.getRois();
		Range[] nd = testBean.getSourceNoDataRange3Bands();

		mosaicArray = new RenderedImage[0];

		// MosaicNoData operation
		RenderedImage image5 = MosaicDescriptor.create(mosaicArray,
				DEFAULT_MOSAIC_TYPE, alphas, rois, null, destinationValues, nd,
				renderingHints);
		// ensure exception
		boolean exception = false;
		try {
			image5.getTile(0, 0);
		} catch (ImagingException e) {
			LOGGER.log(Level.INFO, "No sample model present");
			assertTrue(e.getRootCause().getMessage()
					.contains("No sample model present"));
			exception = true;
		}
		// check the exception
		assertTrue(exception);

	}

	@Test
	public void testExceptionImagesSameSizeSourcesAndNoData() {
		TestBean testBean = createBean3Images(0, true, false);

		// start image array
		RenderedImage[] mosaicArray = new RenderedImage[2];

		// Creates an array for the destination band values
		double[] destinationValues = { testBean.getDestinationNoData()[0] };

		ImageLayout layout = (ImageLayout) hints.get(JAI.KEY_IMAGE_LAYOUT);
		RenderingHints renderingHints = new RenderingHints(
				JAI.KEY_IMAGE_LAYOUT, layout);

		Number[] exampleData = { 15, 15 };
		mosaicArray[0] = getSyntheticUniformTypeImage(exampleData, null,
				DataBuffer.TYPE_INT, 1, DataDisplacement.DATA_DATA, true, false);
		mosaicArray[1] = getSyntheticUniformTypeImage(exampleData, null,
				DataBuffer.TYPE_INT, 1, DataDisplacement.DATA_DATA, false,
				false);

		// Updates the innerBean
		PlanarImage[] alphas = testBean.getAlphas();
		ROI[] rois = testBean.getRois();
		Range[] nd = new Range[10];

		// MosaicNoData operation
		RenderedImage image5 = MosaicDescriptor.create(mosaicArray,
				DEFAULT_MOSAIC_TYPE, alphas, rois, null, destinationValues, nd,
				renderingHints);
		// ensure exception
		boolean exception = false;
		try {
			image5.getTile(0, 0);
		} catch (ImagingException e) {
			assertTrue(e.getRootCause().getMessage()
					.contains("no data number is not equal to the source number"));
			LOGGER.log(Level.INFO,
					"no data number is not equal to the source number");
			exception = true;
		}
		// check the exception
		assertTrue(exception);

	}

	@Test
	public void testExceptionImagesAlphaBandUnic() {
		TestBean testBean = createBean3Images(0, true, false);

		// start image array
		RenderedImage[] mosaicArray = testBean.getImage3Bands();

		// Creates an array for the destination band values
		double[] destinationValues = { testBean.getDestinationNoData()[0] };

		ImageLayout layout = (ImageLayout) hints.get(JAI.KEY_IMAGE_LAYOUT);
		RenderingHints renderingHints = new RenderingHints(
				JAI.KEY_IMAGE_LAYOUT, layout);

		// Updates the innerBean
		PlanarImage[] alphas = new PlanarImage[3];
		ROI[] rois = testBean.getRois();
		Range[] nd = testBean.getSourceNoDataRange3Bands();
		Number[] exampleData = { 15 };
		PlanarImage newAlphaChannel = (PlanarImage) getSyntheticUniformTypeImage(
				exampleData, null, DataBuffer.TYPE_INT, 3,
				DataDisplacement.DATA_DATA, true, false);
		alphas[0] = alphas[1] = alphas[2] = newAlphaChannel;

		// MosaicNoData operation
		RenderedImage image5 = MosaicDescriptor.create(mosaicArray,
				DEFAULT_MOSAIC_TYPE, alphas, rois, null, destinationValues, nd,
				renderingHints);
		// ensure exception
		boolean exception = false;
		try {
			image5.getTile(0, 0);
		} catch (ImagingException e) {
			assertTrue(e.getRootCause().getMessage()
					.contains("Alpha bands number must be 1"));
			LOGGER.log(Level.INFO, "Alpha bands number must be 1");
			exception = true;
		}
		// check the exception
		assertTrue(exception);

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

	private void exceptionMapRectBody(Rectangle testRect, int index,
			boolean sourceRect) {
		TestBean testBean = createBean3Images(0, true, true);

		// start image array
		RenderedImage[] mosaicArray = testBean.getImage3Bands();
		Range[] nd = testBean.getSourceNoDataRange3Bands();

		List mosaicList = new Vector();

		for (int i = 0; i < mosaicArray.length; i++) {
			mosaicList.add(mosaicArray[i]);
		}

		// Creates an array for the destination band values
		double[] destinationValues = { testBean.getDestinationNoData()[0] };

		ImageLayout layout = (ImageLayout) hints.get(JAI.KEY_IMAGE_LAYOUT);
		RenderingHints renderingHints = new RenderingHints(
				JAI.KEY_IMAGE_LAYOUT, layout);

		// Getting the rois, alphabands
		PlanarImage[] alphas = testBean.getAlphas();
		ROI[] rois = testBean.getRois();

		if (sourceRect) {
			Rectangle rect = new MosaicOpImage(mosaicList, layout,
					renderingHints, DEFAULT_MOSAIC_TYPE, alphas, rois, null,
					destinationValues, nd).mapSourceRect(testRect, index);
		} else {
			Rectangle rect = new MosaicOpImage(mosaicList, layout,
					renderingHints, DEFAULT_MOSAIC_TYPE, alphas, rois, null,
					destinationValues, nd).mapDestRect(testRect, index);
		}
	}

	private void testMethodBody(TestBean testBean, int dataType,
			Number firstValue) {
		testMethodBody(testBean, dataType, firstValue, null, null, null, null,
				null, false, false, false);
	}

	private void testMethodBody(TestBean testBean, int dataType,
			Number firstValue, Number secondValue, Number thirdValue,
			boolean roiused, boolean alphaused) {
		testMethodBody(testBean, dataType, firstValue, secondValue, thirdValue,
				null, null, null, roiused, alphaused, false);
	}

	private void testMethodBody(TestBean testBean, int dataType,
			Number firstValue, Number secondValue, Number thirdValue,
			Number fourthValue, Number fifthValue, Number sixthValue,
			boolean roiused, boolean alphaused, boolean overlay) {
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
			assertEquality(0, arrayPixel, dataType,
					testBean.getDestinationNoData()[0]);
			// Check if the pixel has the correct value
			assertEquality(0, arrayPixel3Band, dataType,
					testBean.getDestinationNoData()[0]);
			assertEquality(1, arrayPixel3Band, dataType,
					testBean.getDestinationNoData()[1]);
			assertEquality(2, arrayPixel3Band, dataType,
					testBean.getDestinationNoData()[2]);
		} else {
			if (fourthValue != null) {
				// Test for image with 1 band
				arrayPixel = testExecution3Image(testBean, 1, overlay);
				// Test for image with 3 band
				arrayPixel3Band = testExecution3Image(testBean, 3, overlay);

				if (!overlay) {
					double centerValue = (secondValue.doubleValue() + sixthValue
							.doubleValue()) / 2;
					if (dataType < 4) {
						centerValue += 1;
					}
					fifthValue = centerValue;
				}
				if (firstValue == null && thirdValue == null) {
					// Check if the pixel has the correct value
					assertEquality(0, arrayPixel, dataType,
							testBean.getDestinationNoData()[0], secondValue,
							testBean.getDestinationNoData()[0], fourthValue,
							fifthValue, sixthValue);
					// Check if the pixel has the correct value
					assertEquality(0, arrayPixel3Band, dataType,
							testBean.getDestinationNoData()[0], secondValue,
							testBean.getDestinationNoData()[0], fourthValue,
							fifthValue, sixthValue);
					assertEquality(1, arrayPixel3Band, dataType,
							testBean.getDestinationNoData()[1], secondValue,
							testBean.getDestinationNoData()[1], fourthValue,
							fifthValue, sixthValue);
					assertEquality(2, arrayPixel3Band, dataType,
							testBean.getDestinationNoData()[2], secondValue,
							testBean.getDestinationNoData()[2], fourthValue,
							fifthValue, sixthValue);
				} else {
					// Check if the pixel has the correct value
					assertEquality(0, arrayPixel, dataType, firstValue,
							secondValue, thirdValue, fourthValue, fifthValue,
							sixthValue);
					// Check if the pixel has the correct value
					assertEquality(0, arrayPixel3Band, dataType, firstValue,
							secondValue, thirdValue, fourthValue, fifthValue,
							sixthValue);
					assertEquality(1, arrayPixel3Band, dataType, firstValue,
							secondValue, thirdValue, fourthValue, fifthValue,
							sixthValue);
					assertEquality(2, arrayPixel3Band, dataType, firstValue,
							secondValue, thirdValue, fourthValue, fifthValue,
							sixthValue);
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
						assertEquality(0, arrayPixel, dataType,
								testBean.getDestinationNoData()[0],
								secondValue, thirdValue);
						// Check if the pixel has the correct value
						assertEquality(0, arrayPixel3Band, dataType,
								testBean.getDestinationNoData()[0],
								secondValue, thirdValue);
						assertEquality(1, arrayPixel3Band, dataType,
								testBean.getDestinationNoData()[0],
								secondValue, thirdValue);
						assertEquality(2, arrayPixel3Band, dataType,
								testBean.getDestinationNoData()[0],
								secondValue, thirdValue);
					} else {
						// Check if the pixel has the correct value
						assertEquality(0, arrayPixel, dataType, firstValue,
								secondValue, thirdValue);
						// Check if the pixel has the correct value
						assertEquality(0, arrayPixel3Band, dataType,
								firstValue, secondValue, thirdValue);
						assertEquality(1, arrayPixel3Band, dataType,
								firstValue, secondValue, thirdValue);
						assertEquality(2, arrayPixel3Band, dataType,
								firstValue, secondValue, thirdValue);
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

	private void assertEquality(int bandIndex, Number[][] arrayPixel,
			int dataType, Number firstValue) {
		assertEquality(bandIndex, arrayPixel, dataType, firstValue, null, null,
				null, null, null);
	}

	private void assertEquality(int bandIndex, Number[][] arrayPixel,
			int dataType, Number firstValue, Number secondValue,
			Number thirdValue) {
		assertEquality(bandIndex, arrayPixel, dataType, firstValue,
				secondValue, thirdValue, null, null, null);
	}

	private void assertEquality(int bandIndex, Number[][] arrayPixel,
			int dataType, Number firstValue, Number secondValue,
			Number thirdValue, Number fourthValue, Number fifthValue,
			Number sixthValue) {

		switch (dataType) {
		case DataBuffer.TYPE_BYTE:
			// Position: if one data = center/if 3 data = upper left/ if 6 data
			// = upper left
			if (bandIndex > 0 && secondValue == null) {
				assertEquals(firstValue.byteValue(),
						arrayPixel[0][bandIndex].byteValue());
			} else {
				assertEquals(firstValue.byteValue(),
						arrayPixel[bandIndex][0].byteValue());
			}
			if (secondValue != null) {
				// Position: if 3 data = upper center left/ if 6 data = upper
				// center
				assertEquals(secondValue.byteValue(),
						arrayPixel[bandIndex][1].byteValue());
				// Position: if 3 data = upper center/ if 6 data = upper right
				assertEquals(thirdValue.byteValue(),
						arrayPixel[bandIndex][2].byteValue());
			}
			if (fourthValue != null) {
				// Position: center left
				assertEquals(fourthValue.byteValue(),
						arrayPixel[bandIndex][3].byteValue());
				// Position: center
				assertEquals(fifthValue.byteValue(),
						arrayPixel[bandIndex][4].byteValue());
				// Position: lower left
				assertEquals(sixthValue.byteValue(),
						arrayPixel[bandIndex][5].byteValue());
			}
			break;
		case DataBuffer.TYPE_USHORT:
		case DataBuffer.TYPE_SHORT:
			if (bandIndex > 0 && secondValue == null) {
				assertEquals(firstValue.shortValue(),
						arrayPixel[0][bandIndex].shortValue());
			} else {
				assertEquals(firstValue.shortValue(),
						arrayPixel[bandIndex][0].shortValue());
			}
			if (secondValue != null) {
				assertEquals(secondValue.shortValue(),
						arrayPixel[bandIndex][1].shortValue());
				assertEquals(thirdValue.shortValue(),
						arrayPixel[bandIndex][2].shortValue());
			}
			if (fourthValue != null) {
				assertEquals(fourthValue.shortValue(),
						arrayPixel[bandIndex][3].shortValue());
				assertEquals(fifthValue.shortValue(),
						arrayPixel[bandIndex][4].shortValue());
				assertEquals(sixthValue.shortValue(),
						arrayPixel[bandIndex][5].shortValue());
			}
			break;
		case DataBuffer.TYPE_INT:
			if (bandIndex > 0 && secondValue == null) {
				assertEquals(firstValue.intValue(),
						arrayPixel[0][bandIndex].intValue());
			} else {
				assertEquals(firstValue.intValue(),
						arrayPixel[bandIndex][0].intValue());
			}
			if (secondValue != null) {
				assertEquals(secondValue.intValue(),
						arrayPixel[bandIndex][1].intValue());
				assertEquals(thirdValue.intValue(),
						arrayPixel[bandIndex][2].intValue());
			}
			if (fourthValue != null) {
				assertEquals(fourthValue.intValue(),
						arrayPixel[bandIndex][3].intValue());
				assertEquals(fifthValue.intValue(),
						arrayPixel[bandIndex][4].intValue());
				assertEquals(sixthValue.intValue(),
						arrayPixel[bandIndex][5].intValue());
			}
			break;
		case DataBuffer.TYPE_FLOAT:
			if (bandIndex > 0 && secondValue == null) {
				assertEquals(firstValue.floatValue(),
						arrayPixel[0][bandIndex].floatValue(), DEFAULT_DELTA);
			} else {
				assertEquals(firstValue.floatValue(),
						arrayPixel[bandIndex][0].floatValue(), DEFAULT_DELTA);
			}
			if (secondValue != null) {
				assertEquals(secondValue.floatValue(),
						arrayPixel[bandIndex][1].floatValue(), DEFAULT_DELTA);
				assertEquals(thirdValue.floatValue(),
						arrayPixel[bandIndex][2].floatValue(), DEFAULT_DELTA);
			}
			if (fourthValue != null) {
				assertEquals(fourthValue.floatValue(),
						arrayPixel[bandIndex][3].floatValue(), DEFAULT_DELTA);
				assertEquals(fifthValue.floatValue(),
						arrayPixel[bandIndex][4].floatValue(), DEFAULT_DELTA);
				assertEquals(sixthValue.floatValue(),
						arrayPixel[bandIndex][5].floatValue(), DEFAULT_DELTA);
			}
			break;
		case DataBuffer.TYPE_DOUBLE:
			if (bandIndex > 0 && secondValue == null) {
				assertEquals(firstValue.doubleValue(),
						arrayPixel[0][bandIndex].doubleValue(), DEFAULT_DELTA);
			} else {
				assertEquals(firstValue.doubleValue(),
						arrayPixel[bandIndex][0].doubleValue(), DEFAULT_DELTA);
			}
			if (secondValue != null) {
				assertEquals(secondValue.doubleValue(),
						arrayPixel[bandIndex][1].doubleValue(), DEFAULT_DELTA);
				assertEquals(thirdValue.doubleValue(),
						arrayPixel[bandIndex][2].doubleValue(), DEFAULT_DELTA);
			}
			if (fourthValue != null) {
				assertEquals(fourthValue.doubleValue(),
						arrayPixel[bandIndex][3].doubleValue(), DEFAULT_DELTA);
				assertEquals(fifthValue.doubleValue(),
						arrayPixel[bandIndex][4].doubleValue(), DEFAULT_DELTA);
				assertEquals(sixthValue.doubleValue(),
						arrayPixel[bandIndex][5].doubleValue(), DEFAULT_DELTA);
			}
			break;
		default:
			break;
		}
	}

	// Method for creating a 3 image element with the associated ROI and Alphas
	private static TestBean imageSettings3Images(Number[] validData,
			Number[] sourceNodata, double[] destinationNoDataValue,
			int dataType, boolean roiAlphaNotNull, boolean nullNoData) {

		if (dataType == DataBuffer.TYPE_FLOAT && !nullNoData) {
			for (int j = 0; j < sourceNodata.length; j++) {
				sourceNodata[j] = Float.NaN;
			}
		} else if (dataType == DataBuffer.TYPE_DOUBLE && !nullNoData) {
			for (int j = 0; j < sourceNodata.length; j++) {
				sourceNodata[j] = Double.NaN;
			}
		}

		TestBean tempMosaicBean = new TestBean();

		RenderedImage image1 = null;

		RenderedImage image3Band1 = null;

		if (sourceNodata[0] == null) {
			image1 = getSyntheticUniformTypeImage(validData, sourceNodata,
					dataType, 1, DataDisplacement.DATA_DATA, true, false);
			image3Band1 = getSyntheticUniformTypeImage(validData, sourceNodata,
					dataType, 3, DataDisplacement.DATA_DATA, true, false);
		} else {

			image1 = getSyntheticUniformTypeImage(validData, sourceNodata,
					dataType, 1, DataDisplacement.NODATA_DATA, true, false);
			image3Band1 = getSyntheticUniformTypeImage(validData, sourceNodata,
					dataType, 3, DataDisplacement.NODATA_DATA, true, false);
		}
		RenderedImage image2 = getSyntheticUniformTypeImage(validData,
				sourceNodata, dataType, 1, DataDisplacement.DATA_DATA, false,
				false);
		RenderedImage image3 = getSyntheticUniformTypeImage(validData,
				sourceNodata, dataType, 1, DataDisplacement.DATA_DATA, false,
				true);
		RenderedImage image3Band2 = getSyntheticUniformTypeImage(validData,
				sourceNodata, dataType, 3, DataDisplacement.DATA_DATA, false,
				false);
		RenderedImage image3Band3 = getSyntheticUniformTypeImage(validData,
				sourceNodata, dataType, 3, DataDisplacement.DATA_DATA, false,
				true);
		RenderedImage[] array1Band = { image1, image2, image3 };
		RenderedImage[] array3Band = { image3Band1, image3Band2, image3Band3 };

		// new layout that double the image both in length and width
		ImageLayout layout = new ImageLayout(0, 0, array1Band[0].getWidth()
				+ array1Band[1].getWidth(), array1Band[0].getHeight()
				+ array1Band[1].getHeight());
		layout.setTileHeight(array1Band[0].getHeight() / 16);
		layout.setTileWidth(array1Band[0].getWidth() / 16);
		// create the rendering hints
		hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);

		// All the data are saved in a mosaic bean
		tempMosaicBean.setDestinationNoData(destinationNoDataValue);
		tempMosaicBean.setImage(array1Band);
		tempMosaicBean.setImage3Bands(array3Band);

		Range[] nd = new Range[array1Band.length];
		Range[] nd3Bands = new Range[array1Band.length];
		ROI[] rois = new ROI[array1Band.length];
		PlanarImage[] alphas = new PlanarImage[array1Band.length];
		for (int k = 0; k < array1Band.length; k++) {

			if (sourceNodata[k] != null) {
				switch (dataType) {
				case DataBuffer.TYPE_BYTE:
					nd[k] = RangeFactory.create(sourceNodata[k].byteValue(),
							true, sourceNodata[k].byteValue(), true);
					nd3Bands[k] = RangeFactory.create(
							sourceNodata[k].byteValue(), true,
							sourceNodata[k].byteValue(), true);
					break;
				case DataBuffer.TYPE_USHORT:
				case DataBuffer.TYPE_SHORT:
					nd[k] = RangeFactory.createU(sourceNodata[k].shortValue(),
							true, sourceNodata[k].shortValue(), true);
					nd3Bands[k] = RangeFactory.createU(
							sourceNodata[k].shortValue(), true,
							sourceNodata[k].shortValue(), true);
					break;
				case DataBuffer.TYPE_INT:
					nd[k] = RangeFactory.create(sourceNodata[k].intValue(),
							true, sourceNodata[k].intValue(), true);
					nd3Bands[k] = RangeFactory.create(
							sourceNodata[k].intValue(), true,
							sourceNodata[k].intValue(), true);
					break;
				case DataBuffer.TYPE_FLOAT:
					nd[k] = RangeFactory.create(sourceNodata[k].floatValue(),
							true, sourceNodata[k].floatValue(), true, true);
					nd3Bands[k] = RangeFactory.create(
							sourceNodata[k].floatValue(), true,
							sourceNodata[k].floatValue(), true, true);
					break;
				case DataBuffer.TYPE_DOUBLE:
					nd[k] = RangeFactory.create(sourceNodata[k].doubleValue(),
							true, sourceNodata[k].doubleValue(), true, true);
					nd3Bands[k] = RangeFactory.create(
							sourceNodata[k].doubleValue(), true,
							sourceNodata[k].doubleValue(), true, true);
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
					Rectangle roiImage1 = new Rectangle(startRoiXPixelImage1,
							startRoiYPixel, roiWidth, roiHeigth);
					ROIShape roiData1 = new ROIShape(roiImage1);

					// Only 1 ROI is set to the ImageMosaicBean for the second
					// image
					rois[k] = roiData1;
				}
				// alpha channel creation
				if (k == 2) {
					Number[] alphaChannelData = { 30 };
					PlanarImage alphaChannel = (PlanarImage) getSyntheticUniformTypeImage(
							alphaChannelData, null, dataType, 1,
							DataDisplacement.DATA_DATA, true, false);
					alphaChannel = TranslateDescriptor
							.create(alphaChannel, 0F,
									(float) (alphaChannel.getHeight() / 2),
									null, hints);

					alphas[k] = alphaChannel;
				}
			}
		}

		if (roiAlphaNotNull) {
			tempMosaicBean.setAlphas(alphas);
			tempMosaicBean.setRois(rois);
		}
		if (!nullNoData) {
			tempMosaicBean.setSourceNoDataRange(nd);
			tempMosaicBean.setSourceNoDataRange3Bands(nd3Bands);
		}
		tempMosaicBean.setSourceValidData(validData);

		return tempMosaicBean;
	}

	private Number[][] testExecution3Image(TestBean testBean, int numBands,
			boolean overlay) {
		// Pre-allocated array for saving the pixel values
		Number[][] arrayPixel = new Number[3][6];
		RenderedImage image5 = null;
		RenderedImage[] sourceArray = null;
		// start image array
		Range[] nd = null;
		if (numBands == 3) {
			sourceArray = testBean.getImage3Bands();
			nd = testBean.getSourceNoDataRange();
		} else {
			sourceArray = testBean.getImage();
			nd = testBean.getSourceNoDataRange3Bands();
		}
		// start image array
		RenderedImage[] mosaicArray = new RenderedImage[3];
		// Translation of the second image of half of its dimension
		// IMAGE ON THE UPPER RIGHT
		mosaicArray[1] = TranslateDescriptor.create(sourceArray[1],
				(float) (sourceArray[1].getWidth() / 2), 0F, null, hints);
		// The First image is only put in the new layout
		// IMAGE ON THE UPPER LEFT
		mosaicArray[0] = TranslateDescriptor.create(sourceArray[0], 0F, 0F,
				null, hints);
		// IMAGE ON THE LOWER LEFT
		mosaicArray[2] = TranslateDescriptor.create(sourceArray[2], 0F,
				(float) (sourceArray[2].getHeight() / 2), null, hints);
		// Getting the nodata, rois, alphabands
		PlanarImage[] alphas = testBean.getAlphas();
		ROI[] rois = testBean.getRois();

		// Creates an array for the destination band values
		double[] destinationValues = testBean.getDestinationNoData();

		if (overlay) {
			// MosaicNoData operation
			image5 = MosaicDescriptor.create(mosaicArray, DEFAULT_MOSAIC_TYPE,
					alphas, rois, null, destinationValues, nd, hints);
		} else {
			image5 = MosaicDescriptor
					.create(mosaicArray,
							javax.media.jai.operator.MosaicDescriptor.MOSAIC_TYPE_BLEND,
							alphas, rois, null, destinationValues, nd, hints);
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
			// first iteration : selection of the first pixel of the first image
			// (no data)
			// second iteration : selection of the pixel in ROI(second image)
			// but no data(first image)
			// third iteration : selection of the pixel not in ROI but in the
			// second image
			// fourth iteration : selection of the pixel in alpha channel(third
			// image) but no data(first image)
			// fifth iteration : selection of the pixel in alpha channel(third
			// image), in ROI(second image) but no data(first image)
			// sixth iteration : selection of the pixel in alpha channel(third
			// image)

			// Iteration around the bands
			for (int i = 0; i < numBands; i++) {
				// Iteration around the image
				for (int j = 0; j < 6; j++) {
					arrayPixel[i][j] = rasterArray[j].getPixel(
							rasterArray[j].getMinX(), rasterArray[j].getMinY(),
							arrayPixelInt)[i];
				}
			}
			break;
		case DataBuffer.TYPE_FLOAT:
			arrayPixelFloat = new float[6];
			// Iteration around the bands
			for (int i = 0; i < numBands; i++) {
				// Iteration around the image
				for (int j = 0; j < 6; j++) {
					arrayPixel[i][j] = rasterArray[j].getPixel(
							rasterArray[j].getMinX(), rasterArray[j].getMinY(),
							arrayPixelFloat)[i];
				}
			}
			break;
		case DataBuffer.TYPE_DOUBLE:
			arrayPixelDouble = new double[3];
			// Iteration around the bands
			for (int i = 0; i < numBands; i++) {
				// Iteration around the image
				for (int j = 0; j < 6; j++) {
					arrayPixel[i][j] = rasterArray[j].getPixel(
							rasterArray[j].getMinX(), rasterArray[j].getMinY(),
							arrayPixelDouble)[i];
				}
			}
			break;
		}
		// Final Image disposal
		if (image5 instanceof RenderedOp) {
			((RenderedOp) image5).dispose();
		}

		return arrayPixel;
	}

	private Number[][] testExecutionAlpha(TestBean testBean, int numBands) {
		// Pre-allocated array for saving the pixel values
		Number[][] arrayPixel = new Number[3][3];

		RenderedImage[] sourceArray = null;
		Range[] nd = null;
		if (numBands == 3) {
			sourceArray = testBean.getImage3Bands();
			nd = testBean.getSourceNoDataRange();
		} else {
			sourceArray = testBean.getImage();
			nd = testBean.getSourceNoDataRange3Bands();
		}
		// start image array
		RenderedImage[] mosaicArray = new RenderedImage[2];
		// Translation of the second image of half of its dimension
		// IMAGE ON THE RIGHT
		mosaicArray[1] = TranslateDescriptor.create(sourceArray[1],
				(float) (sourceArray[1].getWidth() / 2), 0F, null, hints);
		// The First image is only put in the new layout
		// IMAGE ON THE LEFT
		mosaicArray[0] = TranslateDescriptor.create(sourceArray[0], 0F, 0F,
				null, hints);
		// Getting the nodata, rois, alphabands
		PlanarImage[] alphas = testBean.getAlphas();
		ROI[] rois = testBean.getRois();
		// Creates an array for the destination band values
		double[] destinationValues = testBean.getDestinationNoData();
		// MosaicNoData operation
		RenderedImage image5 = MosaicDescriptor.create(mosaicArray,
				DEFAULT_MOSAIC_TYPE, alphas, rois, null, destinationValues, nd,
				hints);

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
		rasterArray[2] = image5.getTile((maxXTile + 1) / 2 - 1 + (maxXTile + 1)
				/ 4, minYTile);
		// X coordinates array
		int[] coordinateXarray = new int[3];
		coordinateXarray[0] = 0;
		int tileCenterWidth = rasterArray[1].getWidth();
		// selection of the first pixel of the tile
		coordinateXarray[1] = rasterArray[1].getMinX() + (tileCenterWidth / 2);
		// int minYCoordCenter = rasterArray[1].getMinY();

		int tileRightSecondImWidth = rasterArray[2].getWidth();
		// selection of the last pixel of the tile
		coordinateXarray[2] = rasterArray[2].getMinX() + tileRightSecondImWidth
				- 1;
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
			// second iteration : selection of the pixel in the second ROI but
			// not in the first ROI
			// third iteration : selection of the last pixel

			// Iteration around the bands
			for (int i = 0; i < numBands; i++) {
				// Iteration around the image
				for (int j = 0; j < 3; j++) {
					arrayPixel[i][j] = rasterArray[j].getPixel(
							coordinateXarray[j], 0, arrayPixelInt)[i];
				}
			}
			break;
		case DataBuffer.TYPE_FLOAT:
			arrayPixelFloat = new float[3];
			// Iteration around the bands
			for (int i = 0; i < numBands; i++) {
				// Iteration around the image
				for (int j = 0; j < 3; j++) {
					arrayPixel[i][j] = rasterArray[j].getPixel(
							coordinateXarray[j], 0, arrayPixelFloat)[i];
				}
			}
			break;
		case DataBuffer.TYPE_DOUBLE:
			arrayPixelDouble = new double[3];
			// Iteration around the bands
			for (int i = 0; i < numBands; i++) {
				// Iteration around the image
				for (int j = 0; j < 3; j++) {
					arrayPixel[i][j] = rasterArray[j].getPixel(
							coordinateXarray[j], 0, arrayPixelDouble)[i];
				}
			}
			break;
		}
		// Final Image disposal
		if (image5 instanceof RenderedOp) {
			((RenderedOp) image5).dispose();
		}

		return arrayPixel;
	}

	private Number[] testExecution(TestBean testBean, int numBands) {
		// Pre-allocated array for saving the pixel values
		Number[] arrayPixel = new Number[3];
		// start image array
		RenderedImage[] sourceArray = null;
		Range[] nd = null;
		if (numBands == 3) {
			sourceArray = testBean.getImage3Bands();
			nd = testBean.getSourceNoDataRange();
		} else {
			sourceArray = testBean.getImage();
			nd = testBean.getSourceNoDataRange3Bands();
		}
		RenderedImage[] mosaicArray = new RenderedImage[2];
		// Translation of the second image of half of its dimension
		// IMAGE ON THE RIGHT
		mosaicArray[1] = TranslateDescriptor.create(sourceArray[1],
				(float) (sourceArray[1].getWidth() / 2), 0F, null, hints);
		// The First image is only put in the new layout
		// IMAGE ON THE LEFT
		mosaicArray[0] = TranslateDescriptor.create(sourceArray[0], 0F, 0F,
				null, hints);
		// Creates an array for the destination band values
		double[] destinationValues = testBean.getDestinationNoData();
		// Getting the nodata, rois, alphabands
		PlanarImage[] alphas = testBean.getAlphas();
		ROI[] rois = testBean.getRois();
		// MosaicNoData operation
		RenderedImage image5 = MosaicDescriptor.create(mosaicArray,
				DEFAULT_MOSAIC_TYPE, alphas, rois, null, destinationValues, nd,
				hints);

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
				arrayPixel[i] = upperCenter.getPixel(mediumXPixelCoord, 0,
						arrayPixelInt)[i];
			}
			break;
		case DataBuffer.TYPE_FLOAT:
			arrayPixelFloat = new float[3];
			// Iteration around the bands
			for (int i = 0; i < numBands; i++) {
				arrayPixel[i] = upperCenter.getPixel(mediumXPixelCoord, 0,
						arrayPixelFloat)[i];
			}
			break;
		case DataBuffer.TYPE_DOUBLE:
			arrayPixelDouble = new double[3];
			// Iteration around the bands
			for (int i = 0; i < numBands; i++) {
				arrayPixel[i] = upperCenter.getPixel(mediumXPixelCoord, 0,
						arrayPixelDouble)[i];
			}
			break;
		}
		// Final Image disposal
		if (image5 instanceof RenderedOp) {
			((RenderedOp) image5).dispose();
		}

		return arrayPixel;
	}

	private Number[][] testExecutionROI(TestBean testBean, int numBands) {
		// Pre-allocated array for saving the pixel values
		Number[][] arrayPixel = new Number[3][3];

		RenderedImage[] sourceArray = null;
		Range[] nd = null;
		if (numBands == 3) {
			sourceArray = testBean.getImage3Bands();
			nd = testBean.getSourceNoDataRange();
		} else {
			sourceArray = testBean.getImage();
			nd = testBean.getSourceNoDataRange3Bands();
		}
		// start image array
		RenderedImage[] mosaicArray = new RenderedImage[2];
		// Translation of the second image of half of its dimension
		// IMAGE ON THE RIGHT
		mosaicArray[1] = TranslateDescriptor.create(sourceArray[1],
				(float) (sourceArray[1].getWidth() / 2), 0F, null, hints);
		// The First image is only put in the new layout
		// IMAGE ON THE LEFT
		mosaicArray[0] = TranslateDescriptor.create(sourceArray[0], 0F, 0F,
				null, hints);
		// Getting the nodata, rois, alphabands
		PlanarImage[] alphas = testBean.getAlphas();
		ROI[] rois = testBean.getRois();
		// Creates an array for the destination band values
		double[] destinationValues = testBean.getDestinationNoData();
		// MosaicNoData operation
		// MosaicNoData operation
		RenderedImage image5 = MosaicDescriptor.create(mosaicArray,
				DEFAULT_MOSAIC_TYPE, alphas, rois, null, destinationValues, nd,
				hints);

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
			// second iteration : selection of the first pixel in the middle of
			// the first image
			// third iteration : selection of the first pixel of the center tile
			// of the global image

			// Iteration around the bands
			for (int i = 0; i < numBands; i++) {
				// Iteration around the image
				for (int j = 0; j < 3; j++) {
					arrayPixel[i][j] = rasterArray[j].getPixel(
							coordinateXarray[j], 0, arrayPixelInt)[i];
				}
			}
			break;
		case DataBuffer.TYPE_FLOAT:
			arrayPixelFloat = new float[3];
			// Iteration around the bands
			for (int i = 0; i < numBands; i++) {
				// Iteration around the image
				for (int j = 0; j < 3; j++) {
					arrayPixel[i][j] = rasterArray[j].getPixel(
							coordinateXarray[j], 0, arrayPixelFloat)[i];
				}
			}
			break;
		case DataBuffer.TYPE_DOUBLE:
			arrayPixelDouble = new double[3];
			// Iteration around the bands
			for (int i = 0; i < numBands; i++) {
				// Iteration around the image
				for (int j = 0; j < 3; j++) {
					arrayPixel[i][j] = rasterArray[j].getPixel(
							coordinateXarray[j], 0, arrayPixelDouble)[i];
				}
			}
			break;
		}

		// Final Image disposal
		if (image5 instanceof RenderedOp) {
			((RenderedOp) image5).dispose();
		}

		return arrayPixel;
	}

	// Static method for adding the Alpha bands on the test images
	private static void creationAlpha(TestBean bean, int dataType) {
		// definition of the alpha band variables
		Number[] alphaChannelData = new Number[2];
		alphaChannelData[0] = 30;
		alphaChannelData[1] = 70;

		PlanarImage alphaChannel0 = null;
		PlanarImage alphaChannel1 = null;
		// Creation of the planar images
		alphaChannel0 = (PlanarImage) getSyntheticUniformTypeImage(
				alphaChannelData, null, dataType, 1,
				DataDisplacement.DATA_DATA, true, false);
		alphaChannel1 = (PlanarImage) getSyntheticUniformTypeImage(
				alphaChannelData, null, dataType, 1,
				DataDisplacement.DATA_DATA, false, false);

		alphaChannel1 = TranslateDescriptor.create(alphaChannel1,
				(float) (alphaChannel1.getWidth() / 2), 0F, null, hints);

		PlanarImage[] alphas = new PlanarImage[] { alphaChannel0, alphaChannel1 };

		bean.setAlphas(alphas);
	}

	// Static method for adding the ROI on the test images
	private static void creationROI(TestBean bean) {

		int imageWidth = bean.getImage()[0].getWidth();

		int startRoiXPixelImage1 = imageWidth - 2;
		int startRoiYPixel = 0;

		int roiWidth = imageWidth;
		int roiHeigth = 50;

		// Creation of 2 ROI
		Rectangle roiImage1 = new Rectangle(startRoiXPixelImage1,
				startRoiYPixel, roiWidth, roiHeigth);
		ROIShape roiData1 = new ROIShape(roiImage1);

		// the second ROI is translated towards the X direction
		int startRoiXPixelImage2 = imageWidth / 2;
		Rectangle roiImage2 = new Rectangle(startRoiXPixelImage2,
				startRoiYPixel, roiWidth, roiHeigth);
		ROIShape roiData2 = new ROIShape(roiImage2);

		ROI[] rois = new ROI[] { roiData1, roiData2 };
		// setting ROI
		bean.setRois(rois);
	}

	private static TestBean imageSettings(Number[] validData,
			Number[] sourceNodata, double[] destinationNoDataValue,
			int dataType, int imageDataNoData) {

		DataDisplacement dd = DataDisplacement.values()[imageDataNoData];

		TestBean tempMosaicBean = new TestBean();

		if (dataType == DataBuffer.TYPE_FLOAT) {
			for (int i = 0; i < sourceNodata.length; i++) {
				sourceNodata[i] = Float.NaN;
			}
		} else if (dataType == DataBuffer.TYPE_DOUBLE) {
			for (int i = 0; i < sourceNodata.length; i++) {
				sourceNodata[i] = Double.NaN;
			}
		}

		// no data-no data // no data- data // data-data
		RenderedImage image1 = getSyntheticUniformTypeImage(validData,
				sourceNodata, dataType, 1, dd, true, false);
		RenderedImage image2 = getSyntheticUniformTypeImage(validData,
				sourceNodata, dataType, 1, dd, false, false);
		RenderedImage image3Band1 = getSyntheticUniformTypeImage(validData,
				sourceNodata, dataType, 3, dd, true, false);
		RenderedImage image3Band2 = getSyntheticUniformTypeImage(validData,
				sourceNodata, dataType, 3, dd, false, false);

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

		Range[] nd = new Range[array1Band.length];
		Range[] nd3Bands = new Range[array1Band.length];
		for (int k = 0; k < array1Band.length; k++) {

			switch (dataType) {
			case DataBuffer.TYPE_BYTE:
				nd[k] = RangeFactory.create(sourceNodata[k].byteValue(), true,
						sourceNodata[k].byteValue(), true);
				nd3Bands[k] = RangeFactory.create(sourceNodata[k].byteValue(),
						true, sourceNodata[k].byteValue(), true);
				break;
			case DataBuffer.TYPE_USHORT:
			case DataBuffer.TYPE_SHORT:
				nd[k] = RangeFactory.createU(sourceNodata[k].shortValue(),
						true, sourceNodata[k].shortValue(), true);
				nd3Bands[k] = RangeFactory.createU(
						sourceNodata[k].shortValue(), true,
						sourceNodata[k].shortValue(), true);
				break;
			case DataBuffer.TYPE_INT:
				nd[k] = RangeFactory.create(sourceNodata[k].intValue(), true,
						sourceNodata[k].intValue(), true);
				nd3Bands[k] = RangeFactory.create(sourceNodata[k].intValue(),
						true, sourceNodata[k].intValue(), true);
				break;
			case DataBuffer.TYPE_FLOAT:
				nd[k] = RangeFactory.create(sourceNodata[k].floatValue(), true,
						sourceNodata[k].floatValue(), true, true);
				nd3Bands[k] = RangeFactory.create(sourceNodata[k].floatValue(),
						true, sourceNodata[k].floatValue(), true, true);
				break;
			case DataBuffer.TYPE_DOUBLE:
				nd[k] = RangeFactory.create(sourceNodata[k].doubleValue(),
						true, sourceNodata[k].doubleValue(), true, true);
				nd3Bands[k] = RangeFactory.create(
						sourceNodata[k].doubleValue(), true,
						sourceNodata[k].doubleValue(), true, true);
				break;
			}
		}
		tempMosaicBean.setSourceNoDataRange(nd);
		tempMosaicBean.setSourceNoDataRange3Bands(nd3Bands);
		tempMosaicBean.setSourceValidData(validData);

		return tempMosaicBean;

	}

	// creates all the Mosaic Bean per datatype and with the
	// combination(nodata-nodata,data-nodata,data-data)
	private static TestBean createBean(int dataType, int imageDataNoData,
			boolean useROI, boolean useAlpha) {
		// Data definition
		Number[] sourceND = new Number[2];
		Number[] validData = new Number[2];
		double[] destinationND = new double[3];
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

		// Bean creation
		TestBean bean = imageSettings(validData, sourceND, destinationND,
				dataType, imageDataNoData);
		if (useROI) {
			creationROI(bean);
		}
		if (useAlpha) {
			creationAlpha(bean, dataType);
		}
		return bean;
	}

	// creates all the Mosaic Bean per datatype and with the
	// combination(nodata-nodata,data-nodata,data-data)
	private static TestBean createBean3Images(int dataType,
			boolean nullNoData, boolean roiAlphaNotNull) {
		// one sourceND or valid data for source(same for every band)
		Number[] sourceND = new Number[3];
		Number[] validData = new Number[3];
		// one destination ND value for band(destination is only one image)
		double[] destinationND = new double[3];
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

		// Bean creation
		TestBean bean = imageSettings3Images(validData, sourceND,
				destinationND, dataType, roiAlphaNotNull, nullNoData);
		return bean;
	}

	// Method for image creation
	public static RenderedImage getSyntheticUniformTypeImage(
			Number[] valueData, Number[] valueNoData, int dataType,
			int numBands, DataDisplacement dd, boolean isFirstImage,
			boolean isLastOf3) {

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
				Short[] arrayUShort = new Short[] {
						(short) (valueUShort & 0xffff),
						(short) (valueUShort & 0xffff),
						(short) (valueUShort & 0xffff) };
				pb.add(arrayUShort);
				break;
			case DataBuffer.TYPE_SHORT:
				short valueShort = value.shortValue();
				Short[] arrayShort = new Short[] { valueShort, valueShort,
						valueShort };
				pb.add(arrayShort);
				break;
			case DataBuffer.TYPE_INT:
				int valueInt = value.intValue();
				Integer[] arrayInteger = new Integer[] { valueInt, valueInt,
						valueInt };
				pb.add(arrayInteger);
				break;
			case DataBuffer.TYPE_FLOAT:
				float valueFloat = value.floatValue();
				Float[] arrayFloat = new Float[] { valueFloat, valueFloat,
						valueFloat };
				pb.add(arrayFloat);
				break;
			case DataBuffer.TYPE_DOUBLE:
				double valueDouble = value.doubleValue();
				Double[] arrayDouble = new Double[] { valueDouble, valueDouble,
						valueDouble };
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
	private static class TestBean implements Serializable {

		/** serialVersionUID */
		private static final long serialVersionUID = 1L;

		// 1 band image
		private RenderedImage[] image;

		// 3 band image
		private RenderedImage[] image3Bands;

		// destination no data values
		private double[] destinationNoData;

		// source valid data values
		private Number[] sourceValidData;

		// source NoData Range useful for testing
		private Range[] sourceNoDataRange;

		// source NoData Range useful for testing 3 bands imgs
		private Range[] sourceNoDataRange3Bands;

		// input ROIs used for testing
		private ROI[] rois;

		// input Alpha used for testing
		private PlanarImage[] alphas;

		TestBean() {
		}

		public RenderedImage[] getImage() {
			return image;
		}

		public void setImage(RenderedImage[] image) {
			this.image = image;
		}

		public double[] getDestinationNoData() {
			return destinationNoData;
		}

		public void setDestinationNoData(double[] destinationNoData) {
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

		public Range[] getSourceNoDataRange() {
			return sourceNoDataRange;
		}

		public void setSourceNoDataRange(Range[] sourceNoDataRange) {
			this.sourceNoDataRange = sourceNoDataRange;
		}

		public Range[] getSourceNoDataRange3Bands() {
			return sourceNoDataRange3Bands;
		}

		public void setSourceNoDataRange3Bands(Range[] sourceNoDataRange3Bands) {
			this.sourceNoDataRange3Bands = sourceNoDataRange3Bands;
		}

		public ROI[] getRois() {
			return rois;
		}

		public void setRois(ROI[] rois) {
			this.rois = rois;
		}

		public PlanarImage[] getAlphas() {
			return alphas;
		}

		public void setAlphas(PlanarImage[] alphas) {
			this.alphas = alphas;
		}
	}
}
