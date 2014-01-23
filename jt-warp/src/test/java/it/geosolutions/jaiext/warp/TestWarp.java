package it.geosolutions.jaiext.warp;

import it.geosolutions.jaiext.interpolators.InterpolationBicubic;
import it.geosolutions.jaiext.interpolators.InterpolationBilinear;
import it.geosolutions.jaiext.interpolators.InterpolationNearest;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;

import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.IOException;

import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;
import javax.media.jai.Warp;

import org.geotools.renderedimage.viewer.RenderedImageBrowser;

public class TestWarp extends TestBase {

    public void testWarp(RenderedImage source, boolean noDataUsed, boolean roiUsed, Warp warpObj,
            Number noDataValue, InterpolationType interpType, TestSelection testSelect, Interpolation interpolator) {

        // Image data type
        int dataType = source.getSampleModel().getDataType();

        // No Data Range
        Range noDataRange = null;

        if (noDataUsed) {
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                noDataRange = RangeFactory.create(noDataValue.byteValue(), true,
                        noDataValue.byteValue(), true);
                break;
            case DataBuffer.TYPE_USHORT:
                noDataRange = RangeFactory.create(noDataValue.shortValue(), true,
                        noDataValue.shortValue(), true);
                break;
            case DataBuffer.TYPE_SHORT:
                noDataRange = RangeFactory.create(noDataValue.shortValue(), true,
                        noDataValue.shortValue(), true);
                break;
            case DataBuffer.TYPE_INT:
                noDataRange = RangeFactory.create(noDataValue.intValue(), true,
                        noDataValue.intValue(), true);
                break;
            case DataBuffer.TYPE_FLOAT:
                noDataRange = RangeFactory.create(noDataValue.floatValue(), true,
                        noDataValue.floatValue(), true, true);
                break;
            case DataBuffer.TYPE_DOUBLE:
                noDataRange = RangeFactory.create(noDataValue.doubleValue(), true,
                        noDataValue.doubleValue(), true, true);
                break;
            default:
                throw new IllegalArgumentException("Wrong data type");
            }
        }

        // ROI
        ROI roi = null;

        if (roiUsed) {
            roi = roiCreation();
        }

        // Hints used for image expansion
        RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                BorderExtender.createInstance(BorderExtender.BORDER_COPY));

        RenderedOp destinationIMG = null;

        // Interpolator initialization
        Interpolation interp = null;

        // Interpolators
        switch (interpType) {
        case NEAREST_INTERP:
            // Nearest-Neighbor
            interp = new InterpolationNearest(noDataRange, false, destinationNoData,
                    dataType);
            break;
        case BILINEAR_INTERP:
            // Bilinear
            interp = new InterpolationBilinear(DEFAULT_SUBSAMPLE_BITS, noDataRange,
                    false, destinationNoData, dataType);

            break;
        case BICUBIC_INTERP:
            // Bicubic
            interp = new InterpolationBicubic(DEFAULT_SUBSAMPLE_BITS, noDataRange,
                    false, destinationNoData, dataType, true,
                    DEFAULT_PRECISION_BITS);

            break;
        case GENERAL_INTERP:
            // Bicubic
            interp = interpolator;

            break;
        default:
            break;
        }

        // Warp operation
        destinationIMG = WarpDescriptor.create(source, warpObj, interp, null, roi, hints);
        
        if (INTERACTIVE && dataType == DataBuffer.TYPE_BYTE
                && TEST_SELECTOR == testSelect.getType()) {
            RenderedImageBrowser.showChain(destinationIMG, false, roiUsed);
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Forcing to retrieve an array of all the image tiles
            destinationIMG.getTiles();
        }
        // Image disposal
        destinationIMG.dispose();
        
        // Control if the operation has been correctly performed(can be done only because
        // the image is a square and so even if it is rotated, its dimensions are unchanged).

        // Control if the ROI has been expanded
        //PlanarImage planarIMG = (PlanarImage) destinationIMG;
    }

}
