/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2014 - 2016 GeoSolutions


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
package it.geosolutions.jaiext.testclasses;


import it.geosolutions.jaiext.JAIExt;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.utilities.ImageComparator;
import it.geosolutions.jaiext.utilities.ImageUtilities;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.TiledImage;
import javax.xml.crypto.Data;

import it.geosolutions.jaiext.utilities.TestImageDumper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TestName;


/**
 * This class is an abstract class used for creating test images used by the affine and scale operation test-classes. The
 * only two methods defined are roiCreation() and createTestImage(). The first is used for creating a new ROI object with
 * height and width respectively half of the image height and width. The second is used for creating a test image of the
 * selected data type. The image can be filled with data inside by setting the JAI.Ext.ImageFill parameter to true from
 * the console, but this slows the test-computations. The image is by default a cross surrounded by lots of pixel with
 * value 0. For binary images a big rectangle is added into the left half of the image; for not-binary images a simple
 * square is added in the upper left of the image. The square is  useful for the rotate operation(inside the jt-affine
 * project) because it shows if the image has been correctly rotated.
 */
public abstract class TestBase {

    // Root folder for all test outputs
    private static final Path ROOT_OUT_DIR = Paths.get("src/test", "resources");

    public enum TestRoiNoDataType {
        NONE, BOTH, ROI, NODATA
    }

    /**
     * Boolean indicating if the old descriptor must be used
     */
    protected final static boolean OLD_DESCRIPTOR = Boolean.getBoolean("JAI.Ext.OldDescriptor");

    /**
     * Boolean indicating if the old descriptor must be used
     */
    protected final static boolean WRITE_RESULT = Boolean.getBoolean("JAI.Ext.WriteResult");

    public TestName name = new TestName();

    /**
     * Default value for image width
     */
    public static int DEFAULT_WIDTH = 256;

    /**
     * Default value for image height
     */
    public static int DEFAULT_HEIGHT = 256;

    /**
     * Default value for subsample bits
     *
     */
    public static final int DEFAULT_SUBSAMPLE_BITS = 8;

    /**
     * Default value for precision bits
     *
     */
    public static final int DEFAULT_PRECISION_BITS = 8;

    public static boolean INTERACTIVE = Boolean.getBoolean("JAI.Ext.Interactive");

    public static boolean IMAGE_FILLER = Boolean.valueOf(System.getProperty("JAI.Ext.ImageFill", "true"));

    public static Integer INVERSE_SCALE = Integer.getInteger("JAI.Ext.InverseScale", 0);

    public static Integer TEST_SELECTOR = Integer.getInteger("JAI.Ext.TestSelector", 0);

    protected double destinationNoData;

    protected float transX = 0;

    protected float transY = 0;

    protected float scaleX = 0.5f;

    protected float scaleY = 0.5f;

    private static final byte noDataB = 100;
    private static final short noDataUS = 100;
    private static final short noDataS = 100;
    private static final int noDataI = 100;
    private static final float noDataF = 100;
    private static final double noDataD = 100;

    public enum InterpolationType {
        NEAREST_INTERP(0), BILINEAR_INTERP(1), BICUBIC_INTERP(2), GENERAL_INTERP(3);

        private int type;

        InterpolationType(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }
    }

    public enum ScaleType {
        MAGNIFY(0), REDUCTION(1);

        private int type;

        ScaleType(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }
    }


    public enum TestSelection {
        NO_ROI_ONLY_DATA(0), ROI_ACCESSOR_ONLY_DATA(1), ROI_ONLY_DATA(2), ROI_ACCESSOR_NO_DATA(3), NO_ROI_NO_DATA(4), ROI_NO_DATA(5), BINARY_ROI_ACCESSOR_NO_DATA(6);

        private int type;

        TestSelection(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }
    }

    public enum TransformationType {
        ROTATE_OP(0), TRANSLATE_OP(2), SCALE_OP(1), ALL(3);

        private int value;

        TransformationType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    protected ROIShape roiCreation() {
        int roiHeight = DEFAULT_HEIGHT * 3 / 4;
        int roiWidth = DEFAULT_WIDTH * 3 / 4;

        Rectangle roiBound = new Rectangle(0, 0, roiWidth, roiHeight);

        ROIShape roi = new ROIShape(roiBound);
        return roi;
    }


    /**
     * Simple method for image creation
     */
    public static RenderedImage createTestImage(int dataType, int width, int height, Number noDataValue,
                                                boolean isBinary) {

        return createTestImage(dataType, width, height, noDataValue, isBinary, 3);
    }

    public static RenderedImage createTestImage(int dataType, int width, int height, Number noDataValue,
                                                boolean isBinary, int bands) {
        return createTestImage(dataType, width, height, noDataValue, isBinary, bands, null);

    }

    public static RenderedImage createTestImage(int dataType, int width, int height, boolean isBinary, int numBands) {
        return createTestImage(dataType, width, height, getDefaultNoData(dataType), isBinary, numBands, null);
    }


    public static RenderedImage createDefaultTestImage(int dataType, int numBands, boolean toggleFiller) {
        RenderedImage image;

        if (toggleFiller) {
            IMAGE_FILLER = true;
        }

        // Image creation
        switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                image = createTestImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH, DEFAULT_HEIGHT, false, numBands);
                break;
            case DataBuffer.TYPE_USHORT:
                image = createTestImage(DataBuffer.TYPE_USHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT, false, numBands);
                break;
            case DataBuffer.TYPE_SHORT:
                image = createTestImage(DataBuffer.TYPE_SHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT, false, numBands);
                break;
            case DataBuffer.TYPE_INT:
                image = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH, DEFAULT_HEIGHT, false, numBands);
                break;
            case DataBuffer.TYPE_FLOAT:
                image = createTestImage(DataBuffer.TYPE_FLOAT, DEFAULT_WIDTH, DEFAULT_HEIGHT, false, numBands);
                break;
            case DataBuffer.TYPE_DOUBLE:
                image = createTestImage(DataBuffer.TYPE_DOUBLE, DEFAULT_WIDTH, DEFAULT_HEIGHT, false, numBands);
                break;
            default:
                throw new IllegalArgumentException("Wrong data type");
        }

        if (toggleFiller) {
            IMAGE_FILLER = false;
        }
        return image;
    }


    /**
     * Simple method for image creation
     */
    public static RenderedImage createTestImage(int dataType, int width, int height, Number noDataValue,
                                                boolean isBinary, int numBands, Number validData) {
        final SampleModel sm;
        if (isBinary) {
            // Binary images Sample Model
            sm = new MultiPixelPackedSampleModel(dataType, width, height, 1);
            numBands = 1;
        } else {
            int imageDim = width * height;

            if (numBands == 3 || numBands == 4) {
                int[] bandOffsets = new int[numBands];
                for (int i = 0; i < numBands; i++) {
                    bandOffsets[i] = imageDim * i;
                }
                sm = new ComponentSampleModel(dataType, width, height, numBands, width, bandOffsets);
            } else {
                sm = new ComponentSampleModel(dataType, width, height, 1, width, new int[]{0});
            }
        }
        return createTestImage(width, height, noDataValue, numBands, validData, sm);

    }

    public static RenderedImage createTestImage(int width, int height,
                                                Number noDataValue, int numBands, Number validData, SampleModel sm) {
        boolean isBinary = ImageUtilities.isBinary(sm);

        // This values could be used for fill all the image
        byte valueB = validData != null ? validData.byteValue() : 64;
        short valueUS = validData != null ? validData.shortValue() : Short.MAX_VALUE / 4;
        short valueS = validData != null ? validData.shortValue() : -50;
        int valueI = validData != null ? validData.intValue() : 100;
        float valueF = validData != null ? validData.floatValue() : (255 / 2) * 5f;
        double valueD = validData != null ? validData.doubleValue() : (255 / 1) * 4d;

        boolean fillImage = IMAGE_FILLER;


        // Create the constant operation.
        // parameter block initialization
        int tileW = (int) Math.ceil(width / 8d);
        int tileH = (int) Math.ceil(height / 8d);
        TiledImage used;
        ColorModel cm = null;
        if (sm instanceof MultiPixelPackedSampleModel) {
            MultiPixelPackedSampleModel mpsm = (MultiPixelPackedSampleModel) sm;
            int bits = mpsm.getSampleSize(0);
            int size = (int) Math.pow(2, bits);
            byte[] reds = new byte[size];
            byte[] blues = new byte[size];
            byte[] greens = new byte[size];
            for (int i = 0; i < size; i++) {
                reds[i] = blues[i] = greens[i] = (byte) (255 * size / (double) (i + 1));
            }
            cm = new IndexColorModel(bits, size, reds, blues, greens);
            used = new TiledImage(0, 0, width, height, 0, 0, sm, cm);
        } else {
            used = new TiledImage(sm, tileW, tileH);
        }

        Byte crossValueByte = 0;
        Short crossValueUShort = 0;
        Short crossValueShort = 0;
        Integer crossValueInteger = 0;
        Float crossValueFloat = 0f;
        Double crossValueDouble = 0d;
        int dataType = sm.getDataType();
        if (noDataValue != null) {
            switch (dataType) {
                case DataBuffer.TYPE_BYTE:
                    crossValueByte = noDataValue.byteValue();
                    break;
                case DataBuffer.TYPE_USHORT:
                    crossValueUShort = noDataValue.shortValue();
                    break;
                case DataBuffer.TYPE_SHORT:
                    crossValueShort = noDataValue.shortValue();
                    break;
                case DataBuffer.TYPE_INT:
                    crossValueInteger = noDataValue.intValue();
                    break;
                case DataBuffer.TYPE_FLOAT:
                    crossValueFloat = noDataValue.floatValue();
                    break;
                case DataBuffer.TYPE_DOUBLE:
                    crossValueDouble = noDataValue.doubleValue();
                    break;
                default:
                    throw new IllegalArgumentException("Wrong data type");
            }
        }

        int imgBinX = width / 4;
        int imgBinY = height / 4;

        int imgBinWidth = imgBinX + width / 4;
        int imgBinHeight = imgBinY + height / 4;

        for (int b = 0; b < numBands; b++) {
            for (int j = 0; j < width; j++) {
                for (int k = 0; k < height; k++) {
                    //Addition of a cross on the image
                    if (noDataValue != null && (j == k || j == width - k - 1)) {
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
                        //If selected, the image could be filled
                    } else if (!isBinary && fillImage) {
                        // a little square of no data on the upper left is inserted
                        if ((j >= 20) && (j < 50) && (k >= 20) && (k < 50)) {
                            switch (dataType) {
                                case DataBuffer.TYPE_BYTE:
                                    used.setSample(j, k, b, 0);
                                    break;
                                case DataBuffer.TYPE_USHORT:
                                    used.setSample(j, k, b, 0);
                                    break;
                                case DataBuffer.TYPE_SHORT:
                                    used.setSample(j, k, b, 0);
                                    break;
                                case DataBuffer.TYPE_INT:
                                    used.setSample(j, k, b, 0);
                                    break;
                                case DataBuffer.TYPE_FLOAT:
                                    used.setSample(j, k, b, 0);
                                    break;
                                case DataBuffer.TYPE_DOUBLE:
                                    used.setSample(j, k, b, 0);
                                    break;
                                default:
                                    throw new IllegalArgumentException("Wrong data type");
                            }

                            if ((j >= 30) && (j < 40) && (k >= 20) && (k < 30)) {
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
                            }


                        } else {
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
                        }
                        //If is binary a rectangle is added
                    } else if (isBinary && (j > imgBinX && j < imgBinWidth) && (j > imgBinY && j < imgBinHeight)) {
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
                        // Else, a little square of no data on the upper left is inserted
                    } else {
                        if (noDataValue != null && ((j >= 2) && (j < 10) && (k >= 2) && (k < 10))) {
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
                        }

                        if ((j >= 150) && (j < 170) && (k >= 90) && (k < 110)) {
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
                        }
                    }
                }
            }
        }
        return used;
    }

    protected static RenderedImage createIndexedImage(int defaultWidth, int defaultHeight, boolean transparentPixel, boolean addAlpha) {
        IndexColorModel icm;
        int SIZE = 255;
        byte[] reds = new byte[SIZE];
        byte[] greens = new byte[SIZE];
        byte[] blues = new byte[SIZE];
        byte[] alphas = new byte[SIZE];
        for (int i = 0; i < reds.length; i++) {
            reds[i] = (byte) (0xFF & i);
            greens[i] = (byte) (0xFF & i);
            blues[i] = (byte) (0xFF & i);
            alphas[i] = (byte) (0xFF & i);
        }
        if (addAlpha) {
            if (!transparentPixel) {
                icm = new IndexColorModel(8, SIZE, reds, greens, blues, alphas);
            } else {
                throw new IllegalArgumentException("Unsupported combination, transparent pixel and alpha");
            }
        } else {
            if (transparentPixel) {
                icm = new IndexColorModel(8, SIZE, reds, greens, blues, 0);
            } else {
                icm = new IndexColorModel(8, SIZE, reds, greens, blues);
            }
        }

        BufferedImage bi = new BufferedImage(defaultWidth, defaultHeight, BufferedImage.TYPE_BYTE_INDEXED, icm);

        int k = 0;
        int[] pixel = new int[1];
        WritableRaster data = bi.getRaster();
        for (int i = 0; i < defaultHeight; i++) {
            for (int j = 0; j < defaultWidth; j++) {
                pixel[0] = k;
                data.setPixel(j, i, pixel);
                k = (k + 1) % 255;
            }
        }

        return bi;
    }


    @BeforeClass
    public static void setup() {
        JAIExt.initJAIEXT();
    }

    public static Range getRange(int dataType, TestRoiNoDataType testType) {
        Range range = null;
        boolean useRange = testType == TestRoiNoDataType.BOTH || testType == TestRoiNoDataType.NODATA;
        if (useRange && !OLD_DESCRIPTOR) {
            switch (dataType) {
                case DataBuffer.TYPE_BYTE:
                    range = RangeFactory.create(noDataB, true, noDataB, true);
                    break;
                case DataBuffer.TYPE_USHORT:
                    range = RangeFactory.createU(noDataUS, true, noDataUS, true);
                    break;
                case DataBuffer.TYPE_SHORT:
                    range = RangeFactory.create(noDataS, true, noDataS, true);
                    break;
                case DataBuffer.TYPE_INT:
                    range = RangeFactory.create(noDataI, true, noDataI, true);
                    break;
                case DataBuffer.TYPE_FLOAT:
                    range = RangeFactory.create(noDataF, true, noDataF, true, true);
                    break;
                case DataBuffer.TYPE_DOUBLE:
                    range = RangeFactory.create(noDataD, true, noDataD, true, true);
                    break;
                default:
                    throw new IllegalArgumentException("Wrong data type");
            }
        }
        return range;
    }

    @Test
    public void testBase() {
        testAllTypes(TestRoiNoDataType.NONE);
    }

    public static String getSuffix(TestRoiNoDataType testType, String prefix) {
        String suffix = prefix != null ? prefix : "";
        if (testType == TestRoiNoDataType.BOTH) {
            suffix += "_ROI_NoDataRange";
        } else if (testType == TestRoiNoDataType.ROI) {
            suffix += "_ROI";
        } else if (testType == TestRoiNoDataType.NODATA) {
            suffix += "_NoDataRange";
        } else {
            suffix += "";
        }
        return suffix;
    }

    public static ROI getROI(TestRoiNoDataType testType) {
        boolean useRoi = testType == TestRoiNoDataType.BOTH || testType == TestRoiNoDataType.ROI;
        ROI roi = null;
        if (useRoi) {
            Rectangle rect = new Rectangle(0, 0, DEFAULT_WIDTH / 4, DEFAULT_HEIGHT / 4);
            roi = new ROIShape(rect);
        }
        return roi;
    }

    public static String getDataTypeString(int dataType) {
        String dataTypeString = "";

        switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                dataTypeString += "Byte";
                break;
            case DataBuffer.TYPE_USHORT:
                dataTypeString += "UShort";
                break;
            case DataBuffer.TYPE_SHORT:
                dataTypeString += "Short";
                break;
            case DataBuffer.TYPE_INT:
                dataTypeString += "Integer";
                break;
            case DataBuffer.TYPE_FLOAT:
                dataTypeString += "Float";
                break;
            case DataBuffer.TYPE_DOUBLE:
                dataTypeString += "Double";
                break;
            default:
                throw new IllegalArgumentException("Wrong data type");
        }
        return dataTypeString;
    }

    public static Interpolation getInterpolation(int dataType, int interpolationType, Range range, double destinationNoData) {
        Interpolation interpolation;
        switch (interpolationType) {
            case 0:
                if (OLD_DESCRIPTOR) {
                    interpolation = new javax.media.jai.InterpolationNearest();
                } else {
                    interpolation = new it.geosolutions.jaiext.interpolators.InterpolationNearest(
                            range, false, destinationNoData, dataType);
                }
                break;
            case 1:
                if (OLD_DESCRIPTOR) {
                    interpolation = new javax.media.jai.InterpolationBilinear(DEFAULT_SUBSAMPLE_BITS);
                } else {
                    interpolation = new it.geosolutions.jaiext.interpolators.InterpolationBilinear(
                            DEFAULT_SUBSAMPLE_BITS, range, false, destinationNoData, dataType);
                }
                break;
            case 2:
                if (OLD_DESCRIPTOR) {
                    interpolation = new javax.media.jai.InterpolationBicubic(DEFAULT_SUBSAMPLE_BITS);
                } else {
                    interpolation = new it.geosolutions.jaiext.interpolators.InterpolationBicubic(
                            DEFAULT_SUBSAMPLE_BITS, range, false, destinationNoData, dataType, true,
                            DEFAULT_PRECISION_BITS);
                }
                break;
            case 3:
                interpolation = new javax.media.jai.InterpolationBicubic(DEFAULT_SUBSAMPLE_BITS);
                break;
            default:
                throw new IllegalArgumentException("Wrong interpolation type");
        }
        return interpolation;
    }

    public static String getInterpolationSuffix(int interpolationType) {
        switch (interpolationType) {
            case 0:
                return "Nearest";
            case 1:
                return "Bilinear";

            case 2:
                return "Bicubic";
            case 3:
                return "Bicubic2";
            default:
                throw new IllegalArgumentException("Wrong interpolation type");
        }
    }

    public static String getDescription(String opName) {
        return OLD_DESCRIPTOR ? "Old " + opName : "New " + opName;
    }

    private static int getDefaultNoData(int dataType) {
        switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                return noDataB;
            case DataBuffer.TYPE_USHORT:
                return noDataUS;
            case DataBuffer.TYPE_SHORT:
                return noDataS;
            case DataBuffer.TYPE_INT:
                return noDataI;
            case DataBuffer.TYPE_FLOAT:
                return (int) noDataF;
            case DataBuffer.TYPE_DOUBLE:
                return (int) noDataD;
            default:
                throw new IllegalArgumentException("Wrong data type");
        }
    }


    private static String dataTypeName(int dataType) {
        switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                return "Byte";
            case DataBuffer.TYPE_USHORT:
                return "UShort"; // unsigned 16-bit
            case DataBuffer.TYPE_SHORT:
                return "Short";
            case DataBuffer.TYPE_INT:
                return "Int";
            case DataBuffer.TYPE_FLOAT:
                return "Float";
            case DataBuffer.TYPE_DOUBLE:
                return "Double";
            default:
                return "Unknown";
        }
    }


    public void finalizeTest(String suffix, Integer dataType, RenderedImage image) {
        String testName = dataType != null ? "test" + dataTypeName(dataType) : name.getMethodName();
        Path path = null;
        try {
            path = preparePath(testName, suffix);
            if (WRITE_RESULT) {
                System.out.println("Saving image to: " + path.toAbsolutePath());
                TestImageDumper.saveAsDeflateTiff(path, image);
            } else {
                System.out.println("Testing: " + testName + (suffix != null ? (" " + suffix) : ""));
                BufferedImage expectedBI = readImage(path.toFile(), dataType);
                ImageComparator.imagesEqual(expectedBI, image);
                disposeImage(image);
                disposeImage(expectedBI);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to ");
        }
    }

    public static Path preparePath(String testName, String suffix) throws IOException {

        // Find the calling test class from the stack trace
        String testClassName = findCallingTestClass();
        System.out.println(testClassName);
        String packagePath = "";
        if (testClassName != null && testClassName.contains(".")) {
            testClassName = testClassName.replace("it.geosolutions.jaiext", "org.eclipse.imagen.media")
                    .replace("testclasses", "");
            String pkg = testClassName.substring(0, testClassName.lastIndexOf('.'));
            packagePath = pkg.replace('.', '/');
        }

        // Build final output dir
        Path outDir = ROOT_OUT_DIR;
        if (!packagePath.isEmpty()) {
            outDir = outDir.resolve(packagePath).resolve("test-data");
        }
        Files.createDirectories(outDir);
        String safeName = testName.replaceAll("Old|New", "").replaceAll("[^a-zA-Z0-9_.-]", "_");
        safeName += (suffix == null || suffix.trim().isEmpty()) ? "" : suffix;
        return outDir.resolve(safeName + ".tif");
    }

    public static BufferedImage readImage(File file) throws IOException {
        return readImage(file, null);
    }

    public static BufferedImage readImage(File file, Integer suggestedDataType) throws IOException {
        System.out.println("Comparing image with: " + file.getAbsolutePath());
        if (suggestedDataType == DataBuffer.TYPE_FLOAT || suggestedDataType == DataBuffer.TYPE_DOUBLE) {
            // Only the TIFF reader from the JDK supports floating point data
            try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {

                Iterator<ImageReader> it = ImageIO.getImageReaders(iis);
                while (it.hasNext()) {
                    ImageReader reader = it.next();
                    try {
                        String className = reader.getClass().getName();
                        if ("com.sun.imageio.plugins.tiff.TIFFImageReader".equals(className)) {
                            reader.setInput(iis, true, true);
                            return reader.read(0, null);
                        }
                    } finally {
                        reader.dispose();
                    }
                }
            }
        } else {
            return ImageIO.read(file);
        }
        throw new IOException("No TIFF ImageReader found");

    }

    private static String findCallingTestClass() {
                StackTraceElement[] stack = Thread.currentThread().getStackTrace();
                String candidate = null;

                for (StackTraceElement el : stack) {
                    String cls = el.getClassName();
                    if (cls.startsWith("java.") || cls.startsWith("sun.") ||
                            cls.equals(TestImageDumper.class.getName()) ||
                            cls.endsWith("TestBase")) {
                        continue; // skip infra/base classes
                    }
                    candidate = cls;
                    break;
                }
                return candidate;
            }

    protected void testAllTypes(TestRoiNoDataType testType) {
        for (int dataType = 0; dataType < 6; dataType++) {
            if (supportDataType(dataType)) {
                testOperation(dataType, testType);
            }
        }
    }

    protected boolean supportDataType(int dataType) {
        return dataType == DataBuffer.TYPE_BYTE || dataType == DataBuffer.TYPE_USHORT
                || /*dataType == DataBuffer.TYPE_SHORT || */dataType == DataBuffer.TYPE_INT
                || dataType == DataBuffer.TYPE_FLOAT || dataType == DataBuffer.TYPE_DOUBLE;
    }

    public void testOperation(int dataType, TestRoiNoDataType testType) {
        // empty implementation
    }

    public static void disposeImage(RenderedImage image) {
        // If the image is a PlanarImage or a TiledImage it has to be disposed
        if (image instanceof PlanarImage) {
            ((PlanarImage) image).dispose();
        }
    }

}
