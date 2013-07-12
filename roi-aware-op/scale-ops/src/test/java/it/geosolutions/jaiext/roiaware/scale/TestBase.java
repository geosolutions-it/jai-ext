package it.geosolutions.jaiext.roiaware.scale;

import static org.junit.Assert.assertEquals;
import it.geosolutions.jaiext.roiaware.interpolators.InterpolationBicubicNew;
import it.geosolutions.jaiext.roiaware.interpolators.InterpolationBilinearNew;
import it.geosolutions.jaiext.roiaware.interpolators.InterpolationNearestNew;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.IOException;
import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROIShape;
import javax.media.jai.TiledImage;
import org.geotools.renderedimage.viewer.RenderedImageBrowser;
import org.jaitools.numeric.Range;

public class TestBase<T extends Number & Comparable<? super T>> {

    /** Default value for image width */
    public static int DEFAULT_WIDTH = 256;

    /** Default value for image height */
    public static int DEFAULT_HEIGHT = 256;

    /**
     * Default value for subsample bits
     * */
    public static final int DEFAULT_SUBSAMPLE_BITS = 8;

    /**
     * Default value for precision bits
     * */
    public static final int DEFAULT_PRECISION_BITS = 8;

    public static boolean INTERACTIVE = Boolean.getBoolean("JAI.Ext.Interactive");
    
    public static boolean IMAGE_FILLER = false;

    public static Integer TEST_SELECTOR = Integer.getInteger("JAI.Ext.TestSelector");

    protected double destinationNoData;

    protected float transX = 0;

    protected float transY = 0;

    protected float scaleX = 2;

    protected float scaleY = 2;

    T noDataValue;

    enum InterpolationType {
        NEAREST_INTERP(0), BILINEAR_INTERP(1), BICUBIC_INTERP(2);
        
        private int type;
        
        InterpolationType(int type){
            this.type=type;
        }
        
        protected int getType() {
            return type;
        }
    }

    enum TestSelection {
        NO_ROI_ONLY_DATA(0)
        , ROI_ACCESSOR_ONLY_DATA(1)
        , ROI_ONLY_DATA(2)
        , ROI_ACCESSOR_NO_DATA(3)
        ,BINARY_ROI_ACCESSOR_NO_DATA(4);

        private int type;

        TestSelection(int type) {
            this.type = type;
        }

        protected int getType() {
            return type;
        }
    }

    protected void testGlobal(boolean useROIAccessor, boolean isBinary, boolean bicubic2Disabled,
            boolean noDataRangeUsed, boolean roiPresent, InterpolationType interpType,
            TestSelection testSelect) {

        Byte sourceNoDataByte = 1;
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

    protected void testImage(int dataType, T noDataValue, boolean useROIAccessor, boolean isBinary,
            boolean bicubic2Disabled, boolean noDataRangeUsed, boolean roiPresent,
            InterpolationType interpType, TestSelection testSelect) {
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

    protected ROIShape roiCreation() {
        int roiHeight = DEFAULT_HEIGHT / 2;
        int roiWidth = DEFAULT_WIDTH / 2;

        Rectangle roiBound = new Rectangle(0, 0, roiWidth, roiHeight);

        ROIShape roi = new ROIShape(roiBound);
        return roi;
    }

    /** Simple method for image creation */
    protected static RenderedImage createTestImage(int dataType, int width, int height, Number noDataValue,
            boolean isBinary) {
        // This values could be used for fill all the image
        byte valueB = 64;
        short valueUS = Short.MAX_VALUE / 4;
        short valueS = -50;
        int valueI = 100;
        float valueF = (255 / 2) * 5;
        double valueD = (255 / 1) * 4;

        boolean fillImage = IMAGE_FILLER;
        
        // parameter block initialization
        int tileW = width / 8;
        int tileH = height / 8;
        int imageDim = width * height;

        final SampleModel sm;

        Byte crossValueByte = null;
        Short crossValueUShort = null;
        Short crossValueShort = null;
        Integer crossValueInteger = null;
        Float crossValueFloat = null;
        Double crossValueDouble = null;

        int numBands = 3;

        if (isBinary) {
            // Binary images Sample Model
            sm = new MultiPixelPackedSampleModel(dataType, width, height, 1);
            numBands = 1;
        } else {
            sm = new ComponentSampleModel(dataType, width, height, 3, width, new int[] { 0,
                    imageDim, imageDim * 2 });

        }

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            crossValueByte = (Byte) noDataValue;
            break;
        case DataBuffer.TYPE_USHORT:
            crossValueUShort = (Short) noDataValue;
            break;
        case DataBuffer.TYPE_SHORT:
            crossValueShort = (Short) noDataValue;
            break;
        case DataBuffer.TYPE_INT:
            crossValueInteger = (Integer) noDataValue;
            break;
        case DataBuffer.TYPE_FLOAT:
            crossValueFloat = (Float) noDataValue;
            break;
        case DataBuffer.TYPE_DOUBLE:
            crossValueDouble = (Double) noDataValue;
            break;
        default:
            throw new IllegalArgumentException("Wrong data type");
        }

        // Create the constant operation.
        TiledImage used = new TiledImage(sm, tileW, tileH);

        int imgBinX=width/4;
        int imgBinY=height/4;
        
        int imgBinWidth=imgBinX + width/4;
        int imgBinHeight=imgBinY + height/4;
        
        for (int b = 0; b < numBands; b++) {
            for (int j = 0; j < width; j++) {
                for (int k = 0; k < height; k++) {
                    if (j == k || j == width - k - 1) {
                        switch (dataType) {
                        case DataBuffer.TYPE_BYTE:
                            used.setSample(j, k, b, crossValueByte);
                            break;
                        case DataBuffer.TYPE_USHORT:
                            used.setSample(j, k, b, crossValueUShort);
                            break;
                        case DataBuffer.TYPE_SHORT:
                            used.setSample(j, k, b, crossValueShort);
                            break;
                        case DataBuffer.TYPE_INT:
                            used.setSample(j, k, b, crossValueInteger);
                            break;
                        case DataBuffer.TYPE_FLOAT:
                            used.setSample(j, k, b, crossValueFloat);
                            break;
                        case DataBuffer.TYPE_DOUBLE:
                            used.setSample(j, k, b, crossValueDouble);
                            break;
                        default:
                            throw new IllegalArgumentException("Wrong data type");
                        }
                    } else if (!isBinary && fillImage) {
                        switch (dataType) {
                        case DataBuffer.TYPE_BYTE:
                            used.setSample(j, k, b, valueB + b);
                            break;
                        case DataBuffer.TYPE_USHORT:
                            used.setSample(j, k, b, valueUS + b);
                            break;
                        case DataBuffer.TYPE_SHORT:
                            used.setSample(j, k, b, valueS + b);
                            break;
                        case DataBuffer.TYPE_INT:
                            used.setSample(j, k, b, valueI + b);
                            break;
                        case DataBuffer.TYPE_FLOAT:
                            float data = valueF + b / 3.0f;
                            used.setSample(j, k, b, data);
                            break;
                        case DataBuffer.TYPE_DOUBLE:
                            double dataD = valueD + b / 3.0d;
                            used.setSample(j, k, b, dataD);
                            break;
                        default:
                            throw new IllegalArgumentException("Wrong data type");
                        }
                    }else{
                        if (isBinary && (j>imgBinX && j<imgBinWidth) && (j>imgBinY && j<imgBinHeight)) {
                            switch (dataType) {
                            case DataBuffer.TYPE_BYTE:
                                used.setSample(j, k, b, crossValueByte);
                                break;
                            case DataBuffer.TYPE_USHORT:
                                used.setSample(j, k, b, crossValueUShort);
                                break;
                            case DataBuffer.TYPE_SHORT:
                                used.setSample(j, k, b, crossValueShort);
                                break;
                            case DataBuffer.TYPE_INT:
                                used.setSample(j, k, b, crossValueInteger);
                                break;
                            default:
                                throw new IllegalArgumentException("Wrong data type");
                            }

                        }
                    }
                }
            }
        }
        return used;
    }

}
