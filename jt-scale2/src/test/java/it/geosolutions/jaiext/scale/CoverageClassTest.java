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

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.NullDescriptor;

import org.junit.Test;

import it.geosolutions.jaiext.interpolators.InterpolationNearest;
import it.geosolutions.jaiext.range.Range;

/**
 * This test-class extends the TestScale class and is used for extending the code-coverage of the project. In this test-class are checked the
 * getProperty() method of the ScaleDescriptor class and the capability of the ScaleCRIF.create() method to call the TranslateIntOpImage class or the
 * CopyOpImage class when the requested operation is simply a translation or a copy of the source image without ROI object.
 */
public class CoverageClassTest extends TestScale2 {

    // this test-case is used for testing the getProperty() method of the ScaleDescriptor class
    @Test
    public void testROIProperty() {
        Scale2Descriptor descriptor = new Scale2Descriptor();
        Scale2PropertyGenerator propertyGenerator = (Scale2PropertyGenerator) descriptor
                .getPropertyGenerators()[0];

        boolean useROIAccessor = false;

        // Interpolators initialization
        // Nearest-Neighbor
        Interpolation interpNear = new javax.media.jai.InterpolationNearest();
        // Bilinear
        Interpolation interpBil = new javax.media.jai.InterpolationBilinear(DEFAULT_SUBSAMPLE_BITS);
        // Bicubic
        Interpolation interpBic = new javax.media.jai.InterpolationBicubic(DEFAULT_SUBSAMPLE_BITS);

        // ROI creation
        ROIShape roi = roiCreation();

        byte imageValue = 127;

        // Test image creation

        RenderedImage testImg = createTestImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                imageValue, false);

        RenderedOp testIMG = NullDescriptor.create(testImg, null);

        // Scaled images
        RenderedImage scaleImgNear = Scale2Descriptor.create(testIMG, scaleXd, scaleYd, transXd,
                transYd, interpNear, roi, useROIAccessor, null, null, null);

        RenderedImage scaleImgBil = Scale2Descriptor.create(testIMG, scaleXd, scaleYd, transXd,
                transYd, interpBil, roi, useROIAccessor, null, null, null);

        RenderedImage scaleImgBic = Scale2Descriptor.create(testIMG, scaleXd, scaleYd, transXd,
                transYd, interpBic, roi, useROIAccessor, null, null, null);

        scaleImgNear.getTile(0, 0);
        scaleImgBil.getTile(0, 0);
        scaleImgBic.getTile(0, 0);

        // Scale operstion on ROI
        ROI roiNear = (ROI) propertyGenerator.getProperty("roi", scaleImgNear);
        ROI roiBil = (ROI) propertyGenerator.getProperty("roi", scaleImgBil);
        ROI roiBic = (ROI) propertyGenerator.getProperty("roi", scaleImgBic);

        // ROI starting bounds
        int roiWidth = roi.getBounds().width;
        int roiHeight = roi.getBounds().height;
        // ROI end bounds
        int roiNearWidth = roiNear.getBounds().width;
        int roiNearHeight = roiNear.getBounds().height;

        Rectangle scaleImgBilBounds = new Rectangle(testIMG.getMinX() + interpBil.getLeftPadding(),
                testIMG.getMinY() + interpBil.getTopPadding(),
                testIMG.getWidth() - interpBil.getWidth() + 1,
                testIMG.getHeight() - interpBil.getHeight() + 1);

        int roiBoundWidth = (int) scaleImgBilBounds.getWidth();
        int roiBoundHeight = (int) scaleImgBilBounds.getHeight();

        int roiBilWidth = roiBil.getBounds().width;
        int roiBilHeighth = roiBil.getBounds().height;

        int roiBicWidth = roiBic.getBounds().width;
        int roiBicHeight = roiBic.getBounds().height;

        // Nearest
        assertEquals((int) (roiWidth * scaleX), roiNearWidth);
        assertEquals((int) (roiHeight * scaleY), roiNearHeight);
        // Bilinear
        assertEquals((int) (roiBoundWidth * scaleX), roiBilWidth);
        assertEquals((int) (roiBoundHeight * scaleY), roiBilHeighth);
        // Bicubic
        assertEquals((int) (roiWidth * scaleX), roiBicWidth);
        assertEquals((int) (roiHeight * scaleY), roiBicHeight);

        // Final Images disposal
        if (scaleImgNear instanceof RenderedOp) {
            ((RenderedOp) scaleImgNear).dispose();
        }
        if (scaleImgBil instanceof RenderedOp) {
            ((RenderedOp) scaleImgBil).dispose();
        }
        if (scaleImgBic instanceof RenderedOp) {
            ((RenderedOp) scaleImgBic).dispose();
        }
    }

    @Test
    public void testTranslation() {

        boolean useROIAccessor = false;
        int dataType = DataBuffer.TYPE_BYTE;
        Range noDataRange = null;

        double xScale = 1.0f;
        double yScale = 1.0f;
        double xTrans = 3f;
        double yTrans = 3f;

        byte imageValue = 127;

        // Nearest-Neighbor
        InterpolationNearest interpNear = new InterpolationNearest(noDataRange, useROIAccessor,
                destinationNoData, dataType);

        RenderedImage testIMG = createTestImage(dataType, DEFAULT_WIDTH, DEFAULT_HEIGHT, imageValue,
                false);

        // Scaled images
        PlanarImage scaleImgNear = Scale2Descriptor.create(testIMG, xScale, yScale, xTrans, yTrans,
                interpNear, null, useROIAccessor, null, null, null);
        scaleImgNear.getTiles();

        double actualX = scaleImgNear.getMinX();
        double actualY = scaleImgNear.getMinY();

        double expectedX = testIMG.getMinX() + xTrans;
        double expectedY = testIMG.getMinY() + yTrans;

        double tolerance = 0.1f;

        assertEquals(expectedX, actualX, tolerance);
        assertEquals(expectedY, actualY, tolerance);

        // Final Image disposal
        if (scaleImgNear instanceof RenderedOp) {
            ((RenderedOp) scaleImgNear).dispose();
        }

    }

    @Test
    public void testCopy() {

        boolean useROIAccessor = false;
        int dataType = DataBuffer.TYPE_BYTE;
        Range noDataRange = null;

        double xScale = 1.0f;
        double yScale = 1.0f;
        double xTrans = 0.0f;
        double yTrans = 0.0f;

        byte imageValue = 127;

        // Nearest-Neighbor
        InterpolationNearest interpNear = new InterpolationNearest(noDataRange, useROIAccessor,
                destinationNoData, dataType);

        RenderedImage testIMG = createTestImage(dataType, DEFAULT_WIDTH, DEFAULT_HEIGHT, imageValue,
                false);

        // Scaled images
        PlanarImage scaleImgNear = Scale2Descriptor.create(testIMG, xScale, yScale, xTrans, yTrans,
                interpNear, null, useROIAccessor, null, null, null);
        scaleImgNear.getTiles();

        double actualX = scaleImgNear.getMinX();
        double actualY = scaleImgNear.getMinY();

        double expectedX = testIMG.getMinX();
        double expectedY = testIMG.getMinY();

        double tolerance = 0.1f;

        assertEquals(expectedX, actualX, tolerance);
        assertEquals(expectedY, actualY, tolerance);

        // Final Image disposal
        if (scaleImgNear instanceof RenderedOp) {
            ((RenderedOp) scaleImgNear).dispose();
        }

    }
}
