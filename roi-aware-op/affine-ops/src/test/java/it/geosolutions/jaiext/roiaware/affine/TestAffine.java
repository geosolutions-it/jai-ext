package it.geosolutions.jaiext.roiaware.affine;

import static org.junit.Assert.assertEquals;

import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.IOException;
import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;

import org.geotools.renderedimage.viewer.RenderedImageBrowser;
import org.jaitools.numeric.Range;
import it.geosolutions.jaiext.roiaware.affine.AffineNoDataDescriptor;
import it.geosolutions.jaiext.roiaware.interpolators.InterpolationBicubicNew;
import it.geosolutions.jaiext.roiaware.interpolators.InterpolationBilinearNew;
import it.geosolutions.jaiext.roiaware.interpolators.InterpolationNearestNew;
import it.geosolutions.jaiext.roiaware.testclasses.TestBase;

public class TestAffine  extends TestBase{
    
    /** Quadrant rotation number for the Affine transformation */
    protected int numquadrants = 1;

    /** X coordinate point for rotation */
    protected double anchorX = 0;

    /** Y coordinate point for rotation */
    protected double anchorY = DEFAULT_HEIGHT - 1;

    /** Integer indicating which operation should be visualized */
    public static Integer TRANSFORMATION_SELECTOR = Integer
            .getInteger("JAI.Ext.TransformationSelector");

    protected float transY = -DEFAULT_HEIGHT;
    
    @Override
    protected <T extends Number & Comparable<? super T>> void testImageAffine(
            RenderedImage sourceImage, int dataType, T noDataValue, boolean useROIAccessor,
            boolean isBinary, boolean bicubic2Disabled, boolean noDataRangeUsed,
            boolean roiPresent, boolean setDestinationNoData, TransformationType transformType,
            InterpolationType interpType, TestSelection testSelect) {
     // No Data Range
        Range<T> noDataRange = null;

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

        // Affine Transform selection
        AffineTransform transform = null;
        if (transformType == TransformationType.ROTATE_OP) {
            // Rotation
            transform = AffineTransform.getQuadrantRotateInstance(numquadrants, anchorX, anchorY);
        } else if (transformType == TransformationType.SCALE_OP) {
            // Scale (X and Y doubled)
            transform = AffineTransform.getScaleInstance(scaleX, scaleY);
        } else if (transformType == TransformationType.TRANSLATE_OP) {
            // Translation
            transform = AffineTransform.getTranslateInstance(transX, transY);
        } else if (transformType == TransformationType.ALL) {
            // Rotation
            transform = AffineTransform.getQuadrantRotateInstance(numquadrants, anchorX, anchorY);
            // + Scale (X and Y doubled)
            transform.concatenate(AffineTransform.getScaleInstance(scaleX, scaleY));
            // + Translation (translation towards the center of the image)
            transform.concatenate(AffineTransform.getTranslateInstance(transX, transY));
        } else {
            transform = new AffineTransform();
        }

        RenderedImage destinationIMG = null;

        // Interpolator initialization
        InterpolationNearestNew interpN = null;
        InterpolationBilinearNew interpB = null;
        InterpolationBicubicNew interpBN = null;

        // Interpolators
        switch (interpType) {
        case NEAREST_INTERP:
            // Nearest-Neighbor
            interpN = new InterpolationNearestNew(noDataRange, useROIAccessor, destinationNoData,
                    dataType);

            // Affine operation
            destinationIMG = AffineNoDataDescriptor.create(sourceImage, transform, interpN, null,
                    (ROI) roi, useROIAccessor, setDestinationNoData, hints);

            break;
        case BILINEAR_INTERP:
            // Bilinear
            interpB = new InterpolationBilinearNew(DEFAULT_SUBSAMPLE_BITS, noDataRange,
                    useROIAccessor, destinationNoData, dataType);

            // Affine operation
            destinationIMG = AffineNoDataDescriptor.create(sourceImage, transform, interpB, null,
                    (ROI) roi, useROIAccessor, setDestinationNoData, hints);

            break;
        case BICUBIC_INTERP:
            // Bicubic
            interpBN = new InterpolationBicubicNew(DEFAULT_SUBSAMPLE_BITS, noDataRange,
                    useROIAccessor, destinationNoData, dataType, bicubic2Disabled,
                    DEFAULT_PRECISION_BITS);

            // Affine operation
            destinationIMG = AffineNoDataDescriptor.create(sourceImage, transform, interpBN, null,
                    (ROI) roi, useROIAccessor, setDestinationNoData, hints);

            break;
        default:
            break;
        }

        if (INTERACTIVE && dataType == DataBuffer.TYPE_BYTE
                && TEST_SELECTOR == testSelect.getType()
                && TRANSFORMATION_SELECTOR == transformType.getValue()) {
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
        // Control if the operation has been correctly performed(can be done only because
        // the image is a square and so even if it is rotated, its dimensions are unchanged).

        if (transformType == TransformationType.SCALE_OP) {
            // width
            assertEquals((int) (DEFAULT_WIDTH * scaleX), destinationIMG.getWidth());
            // height
            assertEquals((int) (DEFAULT_HEIGHT * scaleY), destinationIMG.getHeight());
        } else if (transformType == TransformationType.TRANSLATE_OP) {

            double actualX = destinationIMG.getMinX();
            double actualY = destinationIMG.getMinY();

            double expectedX = sourceImage.getMinX() + transX;
            double expectedY = sourceImage.getMinY() + transY;

            double tolerance = 0.1f;
            // X axis
            assertEquals(expectedX, actualX, tolerance);
            // Y axis
            assertEquals(expectedY, actualY, tolerance);
        }

        
    }

   

    protected void testGlobalAffine(boolean useROIAccessor, boolean isBinary,
            boolean bicubic2Disabled, boolean noDataRangeUsed, boolean roiPresent,
            boolean setDestinationNoData, InterpolationType interpType, TestSelection testSelect) {

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
            // destination no data Value
            destinationNoData = 0;
        } else {
            // destination no data Value
            destinationNoData = 255;
        }

        // ImageTest
        // starting dataType
        int dataType = DataBuffer.TYPE_BYTE;
        testAllOperation(dataType, isBinary, sourceNoDataByte, useROIAccessor, bicubic2Disabled,
                noDataRangeUsed, roiPresent, setDestinationNoData, interpType, testSelect);

        dataType = DataBuffer.TYPE_USHORT;
        testAllOperation(dataType, isBinary, sourceNoDataUshort, useROIAccessor, bicubic2Disabled,
                noDataRangeUsed, roiPresent, setDestinationNoData, interpType, testSelect);

        dataType = DataBuffer.TYPE_INT;
        testAllOperation(dataType, isBinary, sourceNoDataInt, useROIAccessor, bicubic2Disabled,
                noDataRangeUsed, roiPresent, setDestinationNoData, interpType, testSelect);

        if (!isBinary) {
            dataType = DataBuffer.TYPE_SHORT;
            testAllOperation(dataType, isBinary, sourceNoDataShort, useROIAccessor, bicubic2Disabled,
                    noDataRangeUsed, roiPresent, setDestinationNoData, interpType, testSelect);

            dataType = DataBuffer.TYPE_FLOAT;
            testAllOperation(dataType, isBinary, sourceNoDataFloat, useROIAccessor, bicubic2Disabled,
                    noDataRangeUsed, roiPresent, setDestinationNoData, interpType, testSelect);
            dataType = DataBuffer.TYPE_DOUBLE;
            testAllOperation(dataType, isBinary, sourceNoDataDouble, useROIAccessor, bicubic2Disabled,
                    noDataRangeUsed, roiPresent, setDestinationNoData, interpType, testSelect);
        }

    }

    protected <T extends Number & Comparable<? super T>> void testAllOperation(int dataType, boolean isBinary, T sourceNoData,
            boolean useROIAccessor, boolean bicubic2Disabled, boolean noDataRangeUsed,
            boolean roiPresent, boolean setDestinationNoData, InterpolationType interpType,
            TestSelection testSelect) {

        RenderedImage sourceImage = createTestImage(dataType, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                sourceNoData, isBinary);
        testImageAffine(sourceImage, dataType, sourceNoData, useROIAccessor, isBinary,
                bicubic2Disabled, noDataRangeUsed, roiPresent, setDestinationNoData,
                TransformationType.ROTATE_OP, interpType, testSelect);
        testImageAffine(sourceImage, dataType, sourceNoData, useROIAccessor, isBinary,
                bicubic2Disabled, noDataRangeUsed, roiPresent, setDestinationNoData,
                TransformationType.TRANSLATE_OP, interpType, testSelect);
        testImageAffine(sourceImage, dataType, sourceNoData, useROIAccessor, isBinary,
                bicubic2Disabled, noDataRangeUsed, roiPresent, setDestinationNoData,
                TransformationType.SCALE_OP, interpType, testSelect);
        testImageAffine(sourceImage, dataType, sourceNoData, useROIAccessor, isBinary,
                bicubic2Disabled, noDataRangeUsed, roiPresent, setDestinationNoData,
                TransformationType.ALL, interpType, testSelect);

    }

    @Override
    protected void testGlobal(boolean useROIAccessor, boolean isBinary, boolean bicubic2Disabled,
            boolean noDataRangeUsed, boolean roiPresent,
            it.geosolutions.jaiext.roiaware.testclasses.TestBase.InterpolationType interpType,
            it.geosolutions.jaiext.roiaware.testclasses.TestBase.TestSelection testSelect) {
        
        throw new UnsupportedOperationException("This operation is not supported, use testGlobalAffine instead");
        
    }

   

    @Override
    protected <T extends Number & Comparable<? super T>> void testImage(int dataType,
            T noDataValue, boolean useROIAccessor, boolean isBinary, boolean bicubic2Disabled,
            boolean noDataRangeUsed, boolean roiPresent, InterpolationType interpType,
            TestSelection testSelect) {
        throw new UnsupportedOperationException("This operation is not supported, use testImageAffine instead");
        
    }

    }
