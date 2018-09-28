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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.awt.*;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;

import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;
import javax.xml.crypto.Data;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;

/**
 * This test-class extends the TestScale class and is used for testing the bilinear interpolation inside the Scale operation.
 * The first method tests the scale operation without the presence of a ROI or a No Data Range. The 2nd method introduces a 
 * ROI object calculated using a ROI RasterAccessor while the 3rd method uses an Iterator on the ROI Object. The 4th method 
 * performs the scale operation with all the components. The last method is similar to the 4th method but executes its operations 
 * on binary images. 
 */
public class BilinearScaleTest extends TestScale {
    
    public static boolean VERBOSE = Boolean.getBoolean("verbose");

    @Test
    public void testImageScaling() {
        boolean roiPresent=false;
        boolean noDataRangeUsed=false;
        boolean isBinary=false;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=false;
              
        testGlobal(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,InterpolationType.BILINEAR_INTERP, TestSelection.NO_ROI_ONLY_DATA,ScaleType.MAGNIFY);
        
        testGlobal(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,InterpolationType.BILINEAR_INTERP, TestSelection.NO_ROI_ONLY_DATA,ScaleType.REDUCTION);
    }

    @Test
    public void testImageScalingROIAccessor() {
        
        boolean roiPresent=true;
        boolean noDataRangeUsed=false;
        boolean isBinary=false;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=true;
              
        testGlobal(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,InterpolationType.BILINEAR_INTERP,TestSelection.ROI_ACCESSOR_ONLY_DATA,ScaleType.MAGNIFY);
        
        testGlobal(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,InterpolationType.BILINEAR_INTERP,TestSelection.ROI_ACCESSOR_ONLY_DATA,ScaleType.REDUCTION);
    }

    @Test
    public void testImageScalingROIBounds() {
        
        boolean roiPresent=true;
        boolean noDataRangeUsed=false;
        boolean isBinary=false;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=false;
              
        testGlobal(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,InterpolationType.BILINEAR_INTERP,TestSelection.ROI_ONLY_DATA,ScaleType.MAGNIFY);
        
        testGlobal(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,InterpolationType.BILINEAR_INTERP,TestSelection.ROI_ONLY_DATA,ScaleType.REDUCTION);
    }
    
    @Test
    public void testImageScalingTotal() {        
        
        boolean roiPresent=true;
        boolean noDataRangeUsed=true;
        boolean isBinary=false;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=true;
              
        testGlobal(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,InterpolationType.BILINEAR_INTERP,TestSelection.ROI_ACCESSOR_NO_DATA,ScaleType.MAGNIFY);
        
        testGlobal(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,InterpolationType.BILINEAR_INTERP,TestSelection.ROI_ACCESSOR_NO_DATA,ScaleType.REDUCTION);
    }

    @Test
    public void testImageScalingBinary() {
        boolean roiPresent=true;
        boolean noDataRangeUsed=true;
        boolean isBinary=true;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=true;
                      
        testGlobal(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,InterpolationType.BILINEAR_INTERP,TestSelection.BINARY_ROI_ACCESSOR_NO_DATA,ScaleType.MAGNIFY);
        
        testGlobal(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,InterpolationType.BILINEAR_INTERP,TestSelection.BINARY_ROI_ACCESSOR_NO_DATA,ScaleType.REDUCTION);
    }

    @Test
    public void testInterpolationNoDataEdges() {
        // build a 2x2 image with a noData in the bottom right pixel
        int width = 2;
        int height = 2;
        SampleModel sm = new ComponentSampleModel(DataBuffer.TYPE_BYTE, width, height, 1, 2, new int[] {0});
        TiledImage source = new TiledImage(0, 0, width, height, 0, 0, sm, PlanarImage.createColorModel(sm));
        int noDataValue = 1;

        // 3 gray pixels and a NoData pixel in the bottom right
        source.setSample(0, 0, 0, 255);
        source.setSample(0, 1, 0, 64);
        source.setSample(1, 0, 0, 32);
        source.setSample(1, 1, 0, noDataValue);
        RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,BorderExtender.createInstance(BorderExtender.BORDER_COPY));
        RenderedImage scaled = ScaleDescriptor.create(source, 32f, 32f,
                0f, 0f, Interpolation.getInstance(Interpolation.INTERP_BILINEAR), null, null, RangeFactory.create(noDataValue, noDataValue), null, hints);

        // The scaled image will be a 64*64 pixels
        // all pixels in the bottom right quarter of the image will be nodata too
        Raster raster = scaled.getData();
        int minX = raster.getMinX();
        int minY = raster.getMinY();
        int maxX = minX + raster.getWidth();
        int maxY = minY + raster.getHeight();
        int halfX = (minX + maxX) / 2;
        int halfY = (minY + maxY) / 2;
        for (int i = minY; i < maxY; i++) {
            for (int j = minX; j < maxX; j++) {
                int value = raster.getSample(j, i, 0);
                if (i >= halfY && j >= halfX) {
                    assertTrue("Expected noData value but found different value", value == noDataValue);
                } else {
                    assertTrue("Expected valid value but found nodata", value != noDataValue);
                }
            }
        }
    }

    @Test
    public void testInterpolationNoDataBleedByte() {
        assertNoDataBleedByte(Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
    }
    
    @Test
    public void testInterpolationNoDataBleedShort() {
        assertNoDataBleedShort(Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
    }
    
    @Test
    public void testInterpolationNoDataBleedFloat() {
        assertNoDataBleedFloat(Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
    }
    
    @Test
    public void testInterpolationNoDataBleedDouble() {
        assertNoDataBleedDouble(Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
    }
    
    @Test
    public void testInterpolateInHole() {
        assertInterpolateInHole(Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
    }

    @Test
    public void testBilinearInterpolationROI() {
        for (int i = 0; i <= 5; i++) {
            if (VERBOSE) {
                System.out.println("Testing data type " + i);
            }
            // using ROI accessor
            RenderedImage image =
                    buildImageWithROI(
                            i,
                            Interpolation.getInstance(Interpolation.INTERP_BILINEAR),
                            true,
                            null);
            assertBilinearInterpolationROI(image);
            if (VERBOSE) {
                System.out.println("Testing data type " + i + " no roi accessor");
            }
            // not using ROI accessor
            image =
                    buildImageWithROI(
                            i,
                            Interpolation.getInstance(Interpolation.INTERP_BILINEAR),
                            false,
                            null);
            assertBilinearInterpolationROI(image);
        }
    }

    public void assertBilinearInterpolationROI(RenderedImage image) {
        // Expected layout is
        // 0 0 0 0 0 0 0 0
        // 0 0 0 0 0 0 0 0
        // 0 0 2 2 3 3 0 0
        // 0 0 2 3 3 3 0 0
        // 0 0 3 3 4 4 0 0
        // 0 0 3 3 4 4 0 0
        // 0 0 0 0 0 0 0 0
        // 0 0 0 0 0 0 0 0
        Raster data = image.getData();

        printValues(image, data);
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int sample;
                if (image.getSampleModel().getDataType() < DataBuffer.TYPE_FLOAT) {
                    sample = data.getSample(c, r, 0);
                } else {
                    sample = (int) Math.round(data.getSampleDouble(c, r, 0));
                }
                
                if (r < 2 || r > 5 || c < 2 || c > 5) {
                    assertEquals("Unexpected value at " + c + " " + r, 0, sample);
                } else if (r < 4 && c < 4 && !(r == 3 && c == 3)) {
                    assertEquals("Unexpected value at " + c + " " + r, 2, sample);
                } else if (r < 4 || (r < 6 && c < 4)) {
                    assertEquals("Unexpected value at " + c + " " + r, 3, sample);
                } else {
                    assertEquals("Unexpected value at " + c + " " + r, 4, sample);
                }
            }
        }
    }

    public void printValues(RenderedImage image, Raster data) {
        if (VERBOSE) {
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    if (image.getSampleModel().getDataType() < DataBuffer.TYPE_FLOAT) {
                        int sample = data.getSample(c, r, 0);
                        System.out.print(sample + " ");
                    } else {
                        double sample = data.getSampleDouble(c, r, 0);
                        System.out.print(String.format("%1$8s", sample));
                    }
                }
                System.out.println();
            }
        }
    }

    @Test
    public void testBilinearInterpolationROINoData() {
        for (int i = 0; i <= 5; i++) {
            if (VERBOSE) {
                System.out.println("Testing data type " + i);
            }
            // using ROI accessor
            Range noData = RangeFactory.create(2, 2);
            RenderedImage image =
                    buildImageWithROI(
                            i,
                            Interpolation.getInstance(Interpolation.INTERP_BILINEAR),
                            true,
                            noData);
            assertBilinearInterpolationROINoData(image);
            if (VERBOSE) {
                System.out.println("Testing data type " + i + " no roi accessor");
            }
            // not using ROI accessor
            image =
                    buildImageWithROI(
                            i,
                            Interpolation.getInstance(Interpolation.INTERP_BILINEAR),
                            false,
                            noData);
            assertBilinearInterpolationROINoData(image);
        }
    }

    public void assertBilinearInterpolationROINoData(RenderedImage image) {
        // Expected layout is
        // 0 0 0 0 0 0 0 0
        // 0 0 0 0 0 0 0 0
        // 0 0 0 0 3 3 0 0
        // 0 0 0 0 3 3 0 0
        // 0 0 3 3 4 4 0 0
        // 0 0 3 3 4 4 0 0
        // 0 0 0 0 0 0 0 0
        // 0 0 0 0 0 0 0 0
        Raster data = image.getData();

        printValues(image, data);
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int sample;
                if (image.getSampleModel().getDataType() < DataBuffer.TYPE_FLOAT) {
                    sample = data.getSample(c, r, 0);
                } else {
                    sample = (int) Math.round(data.getSampleDouble(c, r, 0));
                }

                if (r < 2 || r > 5 || c < 2 || c > 5 || (r < 4 && c < 4)) {
                    assertEquals("Unexpected value at " + c + " " + r, 0, sample);
                } else if (r < 4 && c < 4 && !(r == 3 && c == 3)) {
                    assertEquals("Unexpected value at " + c + " " + r, 2, sample);
                } else if (r < 4 || (r < 6 && c < 4)) {
                    assertEquals("Unexpected value at " + c + " " + r, 3, sample);
                } else {
                    assertEquals("Unexpected value at " + c + " " + r, 4, sample);
                }
            }
        }
    }


}
