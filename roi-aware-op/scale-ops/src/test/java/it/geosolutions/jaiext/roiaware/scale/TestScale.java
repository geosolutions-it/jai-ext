package it.geosolutions.jaiext.roiaware.scale;

import static org.junit.Assert.assertEquals;

import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.IOException;

import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROIShape;

import org.geotools.renderedimage.viewer.RenderedImageBrowser;
import org.jaitools.numeric.Range;

import it.geosolutions.jaiext.roiaware.interpolators.InterpolationBicubicNew;
import it.geosolutions.jaiext.roiaware.interpolators.InterpolationBilinearNew;
import it.geosolutions.jaiext.roiaware.interpolators.InterpolationNearestNew;
import it.geosolutions.jaiext.roiaware.testclasses.TestBase;

public class TestScale<T extends Number & Comparable<? super T>> extends TestBase<T> {

    @Override
    protected void testGlobal(boolean useROIAccessor, boolean isBinary, boolean bicubic2Disabled,
            boolean noDataRangeUsed, boolean roiPresent,
            it.geosolutions.jaiext.roiaware.testclasses.TestBase.InterpolationType interpType,
            it.geosolutions.jaiext.roiaware.testclasses.TestBase.TestSelection testSelect) {

        Byte sourceNoDataByte = 100;
        Short sourceNoDataUshort = Short.MAX_VALUE - 1;
        Short sourceNoDataShort = -255;
        Integer sourceNoDataInt = Integer.MAX_VALUE - 1;
        Float sourceNoDataFloat = -15.2f;
        Double sourceNoDataDouble = Double.NaN;

        if (isBinary) {
            sourceNoDataByte = 1;
            sourceNoDataUshort = 1;
            sourceNoDataInt = 1;
        }

        T sourceNoData = null;

        // ImageTest
        // starting dataType
        int dataType = DataBuffer.TYPE_BYTE;
        sourceNoData = (T) sourceNoDataByte;
        testImage(dataType, sourceNoData, useROIAccessor, isBinary, bicubic2Disabled,
                noDataRangeUsed, roiPresent, interpType, testSelect);

        dataType = DataBuffer.TYPE_USHORT;
        sourceNoData = (T) sourceNoDataUshort;
        testImage(dataType, sourceNoData, useROIAccessor, isBinary, bicubic2Disabled,
                noDataRangeUsed, roiPresent, interpType, testSelect);

        dataType = DataBuffer.TYPE_INT;
        sourceNoData = (T) sourceNoDataInt;
        testImage(dataType, sourceNoData, useROIAccessor, isBinary, bicubic2Disabled,
                noDataRangeUsed, roiPresent, interpType, testSelect);

        if (!isBinary) {
            dataType = DataBuffer.TYPE_SHORT;
            sourceNoData = (T) sourceNoDataShort;
            testImage(dataType, sourceNoData, useROIAccessor, isBinary, bicubic2Disabled,
                    noDataRangeUsed, roiPresent, interpType, testSelect);

            dataType = DataBuffer.TYPE_FLOAT;
            sourceNoData = (T) sourceNoDataFloat;
            testImage(dataType, sourceNoData, useROIAccessor, isBinary, bicubic2Disabled,
                    noDataRangeUsed, roiPresent, interpType, testSelect);

            dataType = DataBuffer.TYPE_DOUBLE;
            sourceNoData = (T) sourceNoDataDouble;
            testImage(dataType, sourceNoData, useROIAccessor, isBinary, bicubic2Disabled,
                    noDataRangeUsed, roiPresent, interpType, testSelect);
        }

    }

    @Override
    protected void testImage(int dataType, T noDataValue, boolean useROIAccessor, boolean isBinary,
            boolean bicubic2Disabled, boolean noDataRangeUsed, boolean roiPresent,
            it.geosolutions.jaiext.roiaware.testclasses.TestBase.InterpolationType interpType,
            it.geosolutions.jaiext.roiaware.testclasses.TestBase.TestSelection testSelect) {
        // No Data Range
        Range<T> noDataRange = null;
        // Source test image
        RenderedImage sourceImage = null;
        if (isBinary) {
            // destination no data Value
            destinationNoData = 0;
        } else {
            // destination no data Value
            destinationNoData = 255;
        }

        sourceImage = createTestImage(dataType, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataValue,
                isBinary);

        if (noDataRangeUsed && !isBinary) {
            noDataRange = new Range<T>(noDataValue, true, noDataValue, true);
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
            interp = new InterpolationNearestNew(noDataRange, useROIAccessor, destinationNoData,
                    dataType);
            break;
        case BILINEAR_INTERP:
            // Bilinear
            interp = new InterpolationBilinearNew(DEFAULT_SUBSAMPLE_BITS, noDataRange,
                    useROIAccessor, destinationNoData, dataType);

            break;
        case BICUBIC_INTERP:
            // Bicubic
            interp = new InterpolationBicubicNew(DEFAULT_SUBSAMPLE_BITS, noDataRange,
                    useROIAccessor, destinationNoData, dataType, bicubic2Disabled,
                    DEFAULT_PRECISION_BITS);
            break;
        default:
            break;
        }

        // Scale operation
        RenderedImage destinationIMG = ScaleNoDataDescriptor.create(sourceImage, scaleX, scaleY,
                transX, transY, interp, roi, useROIAccessor, hints);

        if (INTERACTIVE && dataType == DataBuffer.TYPE_BYTE
                && TEST_SELECTOR == testSelect.getType()) {
            RenderedImageBrowser.showChain(destinationIMG, false, roiPresent);
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // image tile calculation for searching possible errors
                destinationIMG.getTile(0, 0);
        }
        // Forcing to retrieve an array of all the image tiles
        PlanarImage planarIMG = (PlanarImage) destinationIMG;
        planarIMG.getTiles();
        // Control if the scale operation has been correctly performed
        // width
        assertEquals((int) (DEFAULT_WIDTH * scaleX), destinationIMG.getWidth());
        // height
        assertEquals((int) (DEFAULT_HEIGHT * scaleY), destinationIMG.getHeight());
    }

    @Override
    protected void testImageAffine(RenderedImage sourceImage, int dataType, T noDataValue,
            boolean useROIAccessor, boolean isBinary, boolean bicubic2Disabled,
            boolean noDataRangeUsed, boolean roiPresent, boolean setDestinationNoData,
            it.geosolutions.jaiext.roiaware.testclasses.TestBase.TransformationType transformType,
            it.geosolutions.jaiext.roiaware.testclasses.TestBase.InterpolationType interpType,
            it.geosolutions.jaiext.roiaware.testclasses.TestBase.TestSelection testSelect) {
        throw new UnsupportedOperationException("This operation is not supported, use testImage instead");
        
    }

    @Override
    protected void testGlobalAffine(boolean useROIAccessor, boolean isBinary,
            boolean bicubic2Disabled, boolean noDataRangeUsed, boolean roiPresent,
            boolean setDestinationNoData,
            it.geosolutions.jaiext.roiaware.testclasses.TestBase.InterpolationType interpType,
            it.geosolutions.jaiext.roiaware.testclasses.TestBase.TestSelection testSelect) {
        throw new UnsupportedOperationException("This operation is not supported, use testGlobal instead");
        
    }

}
