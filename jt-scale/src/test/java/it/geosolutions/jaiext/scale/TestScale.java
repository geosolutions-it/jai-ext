/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
*    http://www.geo-solutions.it/
*    Copyright 2014 GeoSolutions


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
package it.geosolutions.jaiext.scale;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.awt.*;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;

import it.geosolutions.jaiext.range.NoDataContainer;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;
import it.geosolutions.rendered.viewer.RenderedImageBrowser;

/**
 * This test-class is an extension of the TestBase class inside the jt-utilities project. By calling the testGlobal() method with the selected
 * parameters is possible to create an image with the selected preferences and then process it with the preferred interpolation type. Inside the
 * testGlobal() method are tested images with all the possible data type by calling the testImage() method. This method is used for creating an image
 * with the user-defined parameters(data type, ROI, No Data Range) and then scaling it with the supplied interpolation type. If the user wants to see
 * a scaled image with the selected type of test, must set JAI.Ext.Interactive parameter to true, JAI.Ext.TestSelector from 0 to 5 and
 * JAI.Ext.InverseScale to 0 or 1 (Magnification/reduction) to the Console. The methods testImageAffine() testGlobalAffine() are not supported, they
 * are defined in the jt-affine project.
 */
public class TestScale extends TestBase {


    protected void testGlobal(boolean useROIAccessor, boolean isBinary, boolean bicubic2Disabled,
            boolean noDataRangeUsed, boolean roiPresent,
            it.geosolutions.jaiext.testclasses.TestBase.InterpolationType interpType,
            it.geosolutions.jaiext.testclasses.TestBase.TestSelection testSelect,
            ScaleType scaleValue) {

        // ImageTest
        // starting dataType
        int dataType = DataBuffer.TYPE_BYTE;
        testImage(dataType, useROIAccessor, isBinary, bicubic2Disabled,
                noDataRangeUsed, roiPresent, interpType, testSelect, scaleValue);

        dataType = DataBuffer.TYPE_USHORT;
        testImage(dataType, useROIAccessor, isBinary, bicubic2Disabled,
                noDataRangeUsed, roiPresent, interpType, testSelect, scaleValue);

        dataType = DataBuffer.TYPE_INT;
        testImage(dataType, useROIAccessor, isBinary, bicubic2Disabled,
                noDataRangeUsed, roiPresent, interpType, testSelect, scaleValue);

        if (!isBinary) {
            dataType = DataBuffer.TYPE_SHORT;
            testImage(dataType, useROIAccessor, isBinary, bicubic2Disabled,
                    noDataRangeUsed, roiPresent, interpType, testSelect, scaleValue);

            dataType = DataBuffer.TYPE_FLOAT;
            testImage(dataType, useROIAccessor, isBinary, bicubic2Disabled,
                    noDataRangeUsed, roiPresent, interpType, testSelect, scaleValue);

            dataType = DataBuffer.TYPE_DOUBLE;
            testImage(dataType, useROIAccessor, isBinary, bicubic2Disabled,
                    noDataRangeUsed, roiPresent, interpType, testSelect, scaleValue);
        }

    }

    protected <T extends Number & Comparable<? super T>> void testImage(int dataType,
            boolean useROIAccessor, boolean isBinary, boolean bicubic2Disabled,
            boolean noDataRangeUsed, boolean roiPresent, InterpolationType interpType,
            TestSelection testSelect, ScaleType scaleValue) {

        Number noDataValue = getNoDataValueFor(dataType);

        testImage(dataType, useROIAccessor, isBinary, noDataRangeUsed, roiPresent, interpType,
                testSelect,
                scaleValue, noDataValue);

    }

    protected void testImage(int dataType, boolean useROIAccessor, boolean isBinary,
            boolean noDataRangeUsed, boolean roiPresent, InterpolationType interpType,
            TestSelection testSelect, ScaleType scaleValue, Number noDataValue) {
        RenderedImage sourceImage = createTestImage(dataType, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataValue,
                isBinary);

        testImage(dataType, noDataValue, useROIAccessor, isBinary, noDataRangeUsed, roiPresent,
                interpType,
                testSelect, scaleValue, sourceImage);
    }

    protected Number getNoDataValueFor(int dataType) {
        Number noDataValue;
        switch(dataType){
            case DataBuffer.TYPE_BYTE:
                noDataValue = (byte) 100;
                break;
            case DataBuffer.TYPE_USHORT:
                noDataValue = (short) (Short.MAX_VALUE - 1);
                break;
            case DataBuffer.TYPE_SHORT:
                noDataValue = (short) (-255);
                break;
            case DataBuffer.TYPE_INT:
                noDataValue = Integer.MAX_VALUE - 1;
                break;
            case DataBuffer.TYPE_FLOAT:
                noDataValue = -15.2f;
                break;
            case DataBuffer.TYPE_DOUBLE:
                noDataValue = Double.NEGATIVE_INFINITY;
                break;
            default:
                throw new IllegalArgumentException("Wrong data type " + dataType);

        }
        return noDataValue;
    }

    protected  void testImage(int dataType, Number noDataValue,
            boolean useROIAccessor, boolean isBinary, boolean noDataRangeUsed, boolean roiPresent,
            InterpolationType interpType, TestSelection testSelect,
            ScaleType scaleValue, RenderedImage sourceImage) {
        RenderedImage destinationIMG = testAndReturnImage(dataType, noDataValue, useROIAccessor,
                isBinary, noDataRangeUsed, roiPresent,
                interpType, testSelect, scaleValue, sourceImage);

        //Final Image disposal
        if(destinationIMG instanceof RenderedOp){
            ((RenderedOp)destinationIMG).dispose();
        }
    }

    protected RenderedImage testAndReturnImage(int dataType, Number noDataValue,
            boolean useROIAccessor, boolean isBinary, boolean noDataRangeUsed, boolean roiPresent,
            InterpolationType interpType, TestSelection testSelect,
            ScaleType scaleValue, RenderedImage sourceImage) {
        if (scaleValue == ScaleType.REDUCTION) {
            scaleX = 0.5f;
            scaleY = 0.5f;
        } else {
            scaleX = 1.5f;
            scaleY = 1.5f;
        }

        // No Data Range
        if (isBinary) {
            // destination no data Value
            destinationNoData = 0;
        } else {
            // destination no data Value
            destinationNoData = 255;
        }
        Range noDataRange = null;
        if (noDataRangeUsed && !isBinary) {
            switch(dataType){
            case DataBuffer.TYPE_BYTE:
                noDataRange = RangeFactory.create(noDataValue.byteValue(), true, noDataValue.byteValue(), true);
                break;
            case DataBuffer.TYPE_USHORT:
                noDataRange = RangeFactory.create(noDataValue.shortValue(), true, noDataValue.shortValue(), true);
                break;
            case DataBuffer.TYPE_SHORT:
                noDataRange = RangeFactory.create(noDataValue.shortValue(), true, noDataValue.shortValue(), true);
                break;
            case DataBuffer.TYPE_INT:
                noDataRange = RangeFactory.create(noDataValue.intValue(), true, noDataValue.intValue(), true);
                break;
            case DataBuffer.TYPE_FLOAT:
                noDataRange = RangeFactory.create(noDataValue.floatValue(), true, noDataValue.floatValue(), true,true);
                break;
            case DataBuffer.TYPE_DOUBLE:
                noDataRange = RangeFactory.create(noDataValue.doubleValue(), true, noDataValue.doubleValue(), true,true);
                break;
                default:
                    throw new IllegalArgumentException("Wrong data type");
            
            }
        }

        // ROI
        ROIShape roi = null;

        if (roiPresent) {
            roi = roiCreation();
        }

        // Hints are used only with roiAccessor
        RenderingHints hints = null;

        if (useROIAccessor) {
            hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                    BorderExtender.createInstance(BorderExtender.BORDER_ZERO));
        }

        // Interpolator initialization
        Interpolation interp = null;

        // Interpolators
        switch (interpType) {
        case NEAREST_INTERP:
            // Nearest-Neighbor
            interp = new javax.media.jai.InterpolationNearest();
            break;
        case BILINEAR_INTERP:
            // Bilinear
            interp = new javax.media.jai.InterpolationBilinear(DEFAULT_SUBSAMPLE_BITS);

            if (hints != null) {
                hints.add(new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender
                        .createInstance(BorderExtender.BORDER_COPY)));
            } else {
                hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                        BorderExtender.createInstance(BorderExtender.BORDER_COPY));
            }

            break;
        case BICUBIC_INTERP:
            // Bicubic
            interp = new javax.media.jai.InterpolationBicubic(DEFAULT_SUBSAMPLE_BITS);

            if (hints != null) {
                hints.add(new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender
                        .createInstance(BorderExtender.BORDER_COPY)));
            } else {
                hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                        BorderExtender.createInstance(BorderExtender.BORDER_COPY));
            }

            break;
        default:
            break;
        }

        // Background
        double[] bkg = new double[]{destinationNoData};

        // Scale operation
        RenderedImage destinationIMG = ScaleDescriptor.create(sourceImage, scaleX, scaleY,
                transX, transY, interp, roi, useROIAccessor, noDataRange, bkg, hints);

        if (INTERACTIVE && dataType == DataBuffer.TYPE_BYTE
                && TEST_SELECTOR == testSelect.getType() && INVERSE_SCALE == scaleValue.getType()) {
            RenderedImageBrowser.showChain(destinationIMG, false, roiPresent);
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // image tile calculation for searching possible errors
            ((PlanarImage) destinationIMG).getTiles();
        }

        // Check minimum and maximum value for a tile
        Raster simpleTile = destinationIMG.getTile(destinationIMG.getMinTileX(),
                destinationIMG.getMinTileY());

        int tileWidth = simpleTile.getWidth();
        int tileHeight = simpleTile.getHeight();

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_INT:
            if (!isBinary) {
                int minValue = Integer.MAX_VALUE;
                int maxValue = Integer.MIN_VALUE;

                for (int i = 0; i < tileHeight; i++) {
                    for (int j = 0; j < tileWidth; j++) {
                        int value = simpleTile.getSample(j, i, 0);
                        if (value > maxValue) {
                            maxValue = value;
                        }

                        if (value < minValue) {
                            minValue = value;
                        }
                    }
                }
                // Check if the values are not max and minimum value
                assertNotEquals(minValue, maxValue);
                assertNotEquals(minValue, Integer.MAX_VALUE);
                assertNotEquals(maxValue, Integer.MIN_VALUE);
            }
            break;
        case DataBuffer.TYPE_FLOAT:
            float minValuef = Float.MAX_VALUE;
            float maxValuef = -Float.MAX_VALUE;

            for (int i = 0; i < tileHeight; i++) {
                for (int j = 0; j < tileWidth; j++) {
                    float valuef = simpleTile.getSample(j, i, 0);
                    
                    if(Float.isNaN(valuef)||valuef==Float.POSITIVE_INFINITY||valuef==Float.POSITIVE_INFINITY){
                        valuef=255;
                    }
                    
                    if (valuef > maxValuef) {
                        maxValuef = valuef;
                    }

                    if (valuef < minValuef) {
                        minValuef = valuef;
                    }
                }
            }
            // Check if the values are not max and minimum value
            assertFalse((int) minValuef == (int) maxValuef);
            assertFalse(minValuef == Float.MAX_VALUE);
            assertFalse(maxValuef == -Float.MAX_VALUE);
            break;
        case DataBuffer.TYPE_DOUBLE:
            double minValued = Double.MAX_VALUE;
            double maxValued = -Double.MAX_VALUE;

            for (int i = 0; i < tileHeight; i++) {
                for (int j = 0; j < tileWidth; j++) {
                    double valued = simpleTile.getSampleDouble(j, i, 0);
                    
                    if(Double.isNaN(valued)||valued==Double.POSITIVE_INFINITY||valued==Double.POSITIVE_INFINITY){
                        valued=255;
                    }
                    
                    if (valued > maxValued) {
                        maxValued = valued;
                    }

                    if (valued < minValued) {
                        minValued = valued;
                    }
                }
            }
            // Check if the values are not max and minimum value
            assertFalse((int) minValued == (int) maxValued);
            assertFalse(minValued == Double.MAX_VALUE);
            assertFalse(maxValued == -Double.MAX_VALUE);
            break;
        default:
            throw new IllegalArgumentException("Wrong data type");
        }

        // Control if the ROI has been expanded
        PlanarImage planarIMG = (PlanarImage) destinationIMG;
        int imgWidthROI = destinationIMG.getWidth() * 3 / 4 - 1;
        int imgHeightROI = destinationIMG.getHeight() * 3 / 4 - 1;

        int tileInROIx = planarIMG.XToTileX(imgWidthROI);
        int tileInROIy = planarIMG.YToTileY(imgHeightROI);

        Raster testTile = destinationIMG.getTile(tileInROIx, tileInROIy);

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_INT:
            if (!isBinary) {
                int value = testTile.getSample(testTile.getMinX() + 2, testTile.getMinY() + 1, 0);
                assertFalse(value == (int) destinationNoData);
            }
            break;
        case DataBuffer.TYPE_FLOAT:
            float valuef = testTile.getSampleFloat(testTile.getMinX() + 2, testTile.getMinY() + 1,
                    0);
            assertFalse((int) valuef == (int) destinationNoData);
            break;
        case DataBuffer.TYPE_DOUBLE:
            double valued = testTile.getSampleDouble(testTile.getMinX() + 2,
                    testTile.getMinY() + 1, 0);

            assertFalse(valued == destinationNoData);
            break;
        default:
            throw new IllegalArgumentException("Wrong data type");
        }

        // Forcing to retrieve an array of all the image tiles
        // Control if the scale operation has been correctly performed
        // width
        assertEquals((int) (DEFAULT_WIDTH * scaleX), destinationIMG.getWidth());
        // height
        assertEquals((int) (DEFAULT_HEIGHT * scaleY), destinationIMG.getHeight());
        return destinationIMG;
    }

    public void assertNoDataBleedByte(Interpolation interpolation) {
        final byte constant = (byte) (0xff & 255);
        RenderedImage source = getConstantImage(10, 10, new Byte[] {constant});
        assertNoDataBleed(interpolation, source, 255);
    }
    
    public void assertNoDataBleedShort(Interpolation interpolation) {
        final short constant = (short) (0xffff & 65535);
        RenderedImage source = getConstantImage(10, 10, new Short[] {constant});
        assertNoDataBleed(interpolation, source, constant);
    }
    
    public void assertNoDataBleedFloat(Interpolation interpolation) {
        final float constant = 65535;
        RenderedImage source = getConstantImage(10, 10, new Float[] {constant});
        assertNoDataBleed(interpolation, source, (int) constant);
    }
    
    public void assertNoDataBleedDouble(Interpolation interpolation) {
        final double constant = 65535;
        RenderedImage source = getConstantImage(10, 10, new Double[] {constant});
        assertNoDataBleed(interpolation, source, (int) constant);
    }

    private void assertNoDataBleed(Interpolation interpolation, RenderedImage source, int expectedValue) {
        RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,BorderExtender.createInstance(BorderExtender.BORDER_COPY));
        RenderedImage scaled= ScaleDescriptor.create(source, 2f, 2f,
                0f, 0f, interpolation, null, null, RangeFactory.create(0, 0), null, hints);
        // make sure all pixels are solid like the input ones
        Raster raster = scaled.getData();
        for (int i = raster.getMinY(); i < raster.getMinY() + raster.getHeight(); i++) {
            for (int j = raster.getMinX(); j < raster.getMinX() + raster.getWidth(); j++) {
                int value = raster.getSample(j, i, 0);
                assertEquals("Unexpected value at " + i + ", " + j + ": " + value, expectedValue, value);
            }
        }
    }

    protected RenderedImage getConstantImage(float width, float height, Number[] values) {
        ParameterBlock pb = new ParameterBlock();
        pb.add(width);
        pb.add(height);
        pb.add(values);
        return JAI.create("constant", pb);
    }
    
    protected void assertInterpolateInHole(Interpolation interpolation) {
        assertInterpolateInHole(DataBuffer.TYPE_BYTE, interpolation);
        assertInterpolateInHole(DataBuffer.TYPE_USHORT, interpolation);
    }
    
    protected void assertInterpolateInHole(int dataType, Interpolation interpolation) {
        // build image with high ring at borders and almost nodata in the middle
        SampleModel sm = new ComponentSampleModel(dataType, 10, 10, 1, 10, new int[] {0});
        int width = 10;
        int height = 10;
        TiledImage source = new TiledImage(0, 0, 10, 10, 0, 0, sm, PlanarImage.createColorModel(sm));
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if(x == 0 || x == (width - 1) || y == 0 || y == (height - 1)) {
                    source.setSample(x, y, 0, 255);
                } else {
                    source.setSample(x, y, 0, 1);
                }
                
            }
        }
        RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,BorderExtender.createInstance(BorderExtender.BORDER_COPY));
        RenderedImage scaled= ScaleDescriptor.create(source, 2f, 2f,
                0f, 0f, interpolation, null, null, RangeFactory.create(0, 0), null, hints);
        // make sure none of the pixels became 0
        Raster raster = scaled.getData();
        for (int i = raster.getMinY(); i < raster.getMinY() + raster.getHeight(); i++) {
            for (int j = raster.getMinX(); j < raster.getMinX() + raster.getWidth(); j++) {
                int value = raster.getSample(j, i, 0);
                assertTrue("Expected valid value but found nodata", value > 0);
            }
        }
    }

    /**
     * Build a 4x4 image with pixels having value as the sum of their coordinates, attaches
     * a ROI covering the 4 central pixels, and scales up by a factor of 2 with the given interpolation
     * @param dataType
     * @return
     */
    protected RenderedImage buildImageWithROI(int dataType, Interpolation interpolation, boolean useROIAccessor, Range noData) {
        
        int width = 4;
        int height = 4;
        SampleModel sm = new ComponentSampleModel(dataType, width, height, 1, width, new int[] {0});
        TiledImage source =
                new TiledImage(0, 0, width, height, 0, 0, sm, PlanarImage.createColorModel(sm));
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                source.setSample(x, y, 0, x + y);
            }
        }
        
        // build a ROI covering the center of the image and associate
        ROI roi = new ROIShape(new Rectangle(1, 1, 2, 2));
        RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,BorderExtender.createInstance(BorderExtender.BORDER_COPY));
        hints.put(JAI.KEY_IMAGE_LAYOUT, new ImageLayout(0, 0, width * 2, height * 2, 0, 0, width * 2, height * 2, null, null));
        return ScaleDescriptor.create(source, 2f, 2f, 0f, 0f, interpolation, roi, useROIAccessor, noData, new double[] {0}, hints);
    }

    protected void testPackedImage(InterpolationType interpolation) {
        boolean roiPresent = true;
        boolean noDataRangeUsed = true;
        boolean useROIAccessor = true;
        Number noData = 3;
        Number validData = 1;

        int[] dataTypes =
                new int[] {DataBuffer.TYPE_BYTE, DataBuffer.TYPE_USHORT, DataBuffer.TYPE_INT};

        for (ScaleType scaleType : ScaleType.values()) {
            for (int dataType : dataTypes) {
                MultiPixelPackedSampleModel sm =
                        new MultiPixelPackedSampleModel(dataType, DEFAULT_WIDTH, DEFAULT_HEIGHT, 2);
                RenderedImage image =
                        createTestImage(DEFAULT_WIDTH, DEFAULT_HEIGHT, 0, 1, validData, sm);
                testImage(
                        image.getSampleModel().getDataType(),
                        noData,
                        useROIAccessor,
                        false,
                        noDataRangeUsed,
                        roiPresent,
                        interpolation,
                        TestSelection.ROI_ACCESSOR_NO_DATA,
                        scaleType,
                        image);
            }
        }
    }

    protected void testNoDataOutput(InterpolationType interpolation) {
        boolean roiPresent = true;
        boolean useROIAccessor = true;
        Number noData = 3;

        int[] dataTypes =
                new int[] {DataBuffer.TYPE_BYTE, DataBuffer.TYPE_USHORT, DataBuffer.TYPE_INT};
        for (ScaleType scaleType : ScaleType.values()) {
            for (int dataType : dataTypes) {
                Number[] noDataValues = new Number[] {null, getNoDataValueFor(dataType)};
                for (Number noDataValue : noDataValues) {

                    RenderedImage source = createTestImage(dataType, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataValue, false);
                    RenderedImage result = testAndReturnImage(
                            dataType,
                            noData,
                            useROIAccessor,
                            false,
                            noDataValue != null,
                            roiPresent,
                            interpolation,
                            TestSelection.ROI_ACCESSOR_NO_DATA,
                            scaleType,
                            source);

                    // right now it's using the background data as destination nodata... believe
                    // this is wrong, but won't get down this alley for now as it would require
                    // reviewing and changing many jai-ext operations
                    checkNoData(result, noDataValue == null ? null: 255);

                    ((RenderedOp) result).dispose();
                }
                                
                
                
            }
        }
    }

    private void checkNoData(RenderedImage result, Number noDataValue) {
        Object property = result.getProperty(NoDataContainer.GC_NODATA);
        if (noDataValue != null) {
            assertThat(property, instanceOf(NoDataContainer.class));
            NoDataContainer container = (NoDataContainer) property;
            assertEquals(noDataValue.doubleValue(), container.getAsSingleValue(), 0d);
        } else {
            assertEquals(property, Image.UndefinedProperty);
        }
    }

    protected void testROILayout(int interpolation) {
        testROILayout(DataBuffer.TYPE_BYTE, interpolation);
        testROILayout(DataBuffer.TYPE_USHORT, interpolation);
        testROILayout(DataBuffer.TYPE_SHORT, interpolation);
        testROILayout(DataBuffer.TYPE_INT, interpolation);
        testROILayout(DataBuffer.TYPE_FLOAT, interpolation);
        testROILayout(DataBuffer.TYPE_DOUBLE, interpolation);
    }

    protected void testROILayout(int dataType, int interpolationType) {
        RenderedImage testIMG = createTestImage(dataType, 1, 1, null,
                false);
        PlanarImage testImgWithROI = PlanarImage.wrapRenderedImage(testIMG);
        ROIShape roi = new ROIShape(new Rectangle(0, 0, 1, 1));
        testImgWithROI.setProperty("roi", roi);

        ImageLayout targetLayout = new ImageLayout();
        targetLayout.setTileWidth(512);
        targetLayout.setTileHeight(512);
        RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, targetLayout);
        RenderedOp scaled = ScaleDescriptor.create(testIMG, 1000f, 1000f, 0f, 0f,
                Interpolation.getInstance(interpolationType), roi, false, null, null,
                hints);
        ROI scaledRoi = (ROI) scaled.getProperty("roi");

        // ROI is aligned withe the image and has the expected tile size
        assertEquals(scaled.getBounds(), scaledRoi.getBounds());
        PlanarImage scaleRoiImage = scaledRoi.getAsImage();
        assertEquals(scaled.getTileHeight(), scaleRoiImage.getTileHeight());
        assertEquals(scaled.getTileWidth(), scaleRoiImage.getTileWidth());
        assertEquals(512, scaleRoiImage.getTileWidth());
        assertEquals(512, scaleRoiImage.getTileHeight());
    }
}
