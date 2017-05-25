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
package it.geosolutions.jaiext.affine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import it.geosolutions.jaiext.interpolators.InterpolationBicubic;
import it.geosolutions.jaiext.interpolators.InterpolationBilinear;
import it.geosolutions.jaiext.interpolators.InterpolationNearest;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;
import it.geosolutions.rendered.viewer.RenderedImageBrowser;

import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;

import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;


/**
 * This test-class is an extension of the TestBase class inside the jt-utilities project. By calling the testGlobalAffine() method with the selected
 * parameters is possible to create an image with the selected preferences and then process it with the preferred interpolation type. Inside the
 * testGlobalAffine() method are tested images with all the possible data type by calling the testImageAffine() method. This method is used for
 * creating an image with the user-defined parameters(data type, ROI, No Data Range) and then transforming it with 4 possible transformations:
 * 
 * <ul>
 * <li>Only Rotation</li>
 * <li>Only Scaling</li>
 * <li>Only Translation</li>
 * <li>Combination of the 3 above</li>
 * </ul>
 * 
 * The affine transformation is performed with the selected interpolation type. If the user wants to see the result image with the selected kind of
 * test, must set JAI.Ext.Interactive parameter to true, JAI.Ext.TestSelector from 0 to 5, JAI.Ext.TransformationSelector from 0 to 3 (one of the
 * above described transformations)and JAI.Ext.InverseScale to 0 or 1 (Magnification/reduction) to the Console. The testAllOperation() method is used
 * for grouping all the tests on the same image with 4 different transformations in only one test-method. The methods testImage() and testGlobal() are
 * not supported, they are defined in the jt-scale project.
 */
public class TestAffine extends TestBase {

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

    protected <T extends Number & Comparable<? super T>> void testImageAffine(
            RenderedImage sourceImage, int dataType, T noDataValue, boolean useROIAccessor,
            boolean isBinary, boolean bicubic2Disabled, boolean noDataRangeUsed,
            boolean roiPresent, boolean setDestinationNoData, TransformationType transformType,
            InterpolationType interpType, TestSelection testSelect, ScaleType scaleValue) {

        if (scaleValue == ScaleType.REDUCTION) {
            scaleX = 0.5f;
            scaleY = 0.5f;
        } else {
            scaleX = 1.5f;
            scaleY = 1.5f;
        }

        // No Data Range
        Range noDataRange = null;

        if (noDataRangeUsed && !isBinary) {
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
                        noDataValue.floatValue(), true,true);
                break;
            case DataBuffer.TYPE_DOUBLE:
                noDataRange = RangeFactory.create(noDataValue.doubleValue(), true,
                        noDataValue.doubleValue(), true,true);
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

        // Affine Transform selection
        AffineTransform transform = null;
        if (transformType == TransformationType.ROTATE_OP) {
            // Rotation
            transform = AffineTransform.getQuadrantRotateInstance(numquadrants, anchorX, anchorY);
        } else if (transformType == TransformationType.SCALE_OP) {
            // Scale (X and Y doubled)
            transform = AffineTransform.getScaleInstance(scaleX, scaleY);
        } else if (transformType == TransformationType.TRANSLATE_OP) {
            transX = DEFAULT_WIDTH;
            transY = 0;
            // Translation
            transform = AffineTransform.getTranslateInstance(transX, transY);
        } else if (transformType == TransformationType.ALL) {
            transX = 0;
            transY = -DEFAULT_HEIGHT;
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
        Interpolation interpN = null;
        Interpolation interpB = null;
        Interpolation interpBN = null;

        // Interpolators
        switch (interpType) {
        case NEAREST_INTERP:
            // Nearest-Neighbor
            interpN = new javax.media.jai.InterpolationNearest();

            // Affine operation
            destinationIMG = AffineDescriptor.create(sourceImage, transform, interpN, new double[] {destinationNoData},
                    (ROI) roi, useROIAccessor, setDestinationNoData, noDataRange, hints);

            break;
        case BILINEAR_INTERP:
            // Bilinear
            interpB = new javax.media.jai.InterpolationBilinear(DEFAULT_SUBSAMPLE_BITS);

            if (hints != null) {
                hints.add(new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender
                        .createInstance(BorderExtender.BORDER_COPY)));
            } else {
                hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                        BorderExtender.createInstance(BorderExtender.BORDER_COPY));
            }

            // Affine operation
            destinationIMG = AffineDescriptor.create(sourceImage, transform, interpB, new double[] {destinationNoData},
                    (ROI) roi, useROIAccessor, setDestinationNoData, noDataRange, hints);

            break;
        case BICUBIC_INTERP:
            // Bicubic
            interpBN = new javax.media.jai.InterpolationBicubic(DEFAULT_SUBSAMPLE_BITS);

            if (hints != null) {
                hints.add(new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender
                        .createInstance(BorderExtender.BORDER_COPY)));
            } else {
                hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                        BorderExtender.createInstance(BorderExtender.BORDER_COPY));
            }

            // Affine operation
            destinationIMG = AffineDescriptor.create(sourceImage, transform, interpBN, new double[] {destinationNoData},
                    (ROI) roi, useROIAccessor, setDestinationNoData, noDataRange, hints);

            break;
        default:
            break;
        }

        if (INTERACTIVE && dataType == DataBuffer.TYPE_BYTE
                && TEST_SELECTOR == testSelect.getType()
                && TRANSFORMATION_SELECTOR == transformType.getValue()
                && INVERSE_SCALE == scaleValue.getType()) {
            RenderedImageBrowser.showChain(destinationIMG, false, roiPresent);
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Forcing to retrieve an array of all the image tiles
            ((PlanarImage) destinationIMG).getTiles();
        }
        // Control if the operation has been correctly performed(can be done only because
        // the image is a square and so even if it is rotated, its dimensions are unchanged).

        // Control if the ROI has been expanded
        PlanarImage planarIMG = (PlanarImage) destinationIMG;

        if (transformType == TransformationType.SCALE_OP) {
            if (!isBinary && roiPresent) {
                // Control if the ROI has been expanded
                int imgWidthROI = destinationIMG.getWidth() * 3 / 4 - 2;
                int imgHeightROI = destinationIMG.getHeight() * 3 / 4 - 2;

                int tileInROIx = planarIMG.XToTileX(imgWidthROI);
                int tileInROIy = planarIMG.YToTileY(imgHeightROI);

                Raster testTile = destinationIMG.getTile(tileInROIx, tileInROIy);

                boolean interpNear = false;
                if (interpN != null) {
                    interpNear = true;
                }

                testROI(dataType, testTile, interpNear);

                // Check minimum and maximum value for a tile
                int xFirstTile = destinationIMG.getMinTileX() + destinationIMG.getNumXTiles() - 1;
                int ySecondTile = destinationIMG.getMinTileY() + destinationIMG.getNumYTiles() - 1;

                Raster simpleTile = destinationIMG.getTile(xFirstTile, ySecondTile);

                testEmptyImage(dataType, simpleTile, isBinary);

            }
            // width
            assertEquals((int) (DEFAULT_WIDTH * scaleX), destinationIMG.getWidth());
            // height
            assertEquals((int) (DEFAULT_HEIGHT * scaleY), destinationIMG.getHeight());
        } else if (transformType == TransformationType.TRANSLATE_OP) {
            if (!isBinary && roiPresent) {
                // Control if the ROI has been expanded
                int imgWidthROI = destinationIMG.getMinX() + destinationIMG.getWidth() / 4 - 1;
                int imgHeightROI = destinationIMG.getMinY() + destinationIMG.getHeight() * 3 / 4
                        - 1;

                int tileInROIx = planarIMG.XToTileX(imgWidthROI);
                int tileInROIy = planarIMG.YToTileY(imgHeightROI);

                Raster testTile = destinationIMG.getTile(tileInROIx, tileInROIy);

                boolean interpNear = false;
                if (interpN != null) {
                    interpNear = true;
                }

                testROI(dataType, testTile, interpNear);

                // Check minimum and maximum value for a tile
                int xFirstTile = destinationIMG.getMinTileX() + destinationIMG.getNumXTiles() - 1;
                int ySecondTile = destinationIMG.getMinTileY() + destinationIMG.getNumYTiles() - 1;

                Raster simpleTile = destinationIMG.getTile(xFirstTile, ySecondTile);
                testEmptyImage(dataType, simpleTile, isBinary);

            }
            double actualX = destinationIMG.getMinX();
            double actualY = destinationIMG.getMinY();

            double expectedX = sourceImage.getMinX() + transX;
            double expectedY = sourceImage.getMinY() + transY;

            double tolerance = 0.1f;
            // X axis
            assertEquals(expectedX, actualX, tolerance);
            // Y axis
            assertEquals(expectedY, actualY, tolerance);
        } else if (transformType == TransformationType.ROTATE_OP) {
            // Control if the ROI has been expanded
            if (!isBinary && roiPresent) {
                int imgWidthROI = destinationIMG.getMinX() + destinationIMG.getWidth() / 4 + 1;
                int imgHeightROI = destinationIMG.getMinY() + destinationIMG.getHeight() * 3 / 4
                        - 1;

                int tileInROIx = planarIMG.XToTileX(imgWidthROI);
                int tileInROIy = planarIMG.YToTileY(imgHeightROI);

                Raster testTile = destinationIMG.getTile(tileInROIx, tileInROIy);

                boolean interpNear = false;
                if (interpN != null) {
                    interpNear = true;
                }

                testROI(dataType, testTile, interpNear);

                // Check minimum and maximum value for a tile
                int xFirstTile = destinationIMG.getMinTileX() + 1; //+ destinationIMG.getNumXTiles() - 1;
                int ySecondTile = destinationIMG.getMinTileY()+ 1;

                Raster simpleTile = destinationIMG.getTile(xFirstTile, ySecondTile);

                testEmptyImage(dataType, simpleTile, isBinary);

            }
            // width
            assertEquals((int) (DEFAULT_WIDTH), destinationIMG.getHeight());
            // height
            assertEquals((int) (DEFAULT_HEIGHT), destinationIMG.getWidth());
        }

        // Final Image disposal
        if (destinationIMG instanceof RenderedOp) {
            ((RenderedOp) destinationIMG).dispose();
        }

    }

    // Test for checking if the ROI is correctly expanded or reduced
    protected void testROI(int dataType, Raster testTile, boolean interpNearest) {
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_INT:
            int value = 0;
            if (interpNearest) {
                value = testTile.getSample(testTile.getMinX(), testTile.getMinY() + 2, 0);
                assertFalse(value == (int) destinationNoData);
            } else {
                value = testTile.getSample(testTile.getMinX()+1, testTile.getMinY(), 0);
                assertFalse(value == (int) destinationNoData);
            }
            break;
        case DataBuffer.TYPE_FLOAT:
            if (interpNearest) {
                float valuef = testTile.getSampleFloat(testTile.getMinX(), testTile.getMinY() + 2,
                        0);
                assertFalse((int) valuef == (int) destinationNoData);
            } else {
                float valuef = testTile.getSampleFloat(testTile.getMinX(), testTile.getMinY(), 0);
                assertFalse((int) valuef == (int) destinationNoData);
            }

            break;
        case DataBuffer.TYPE_DOUBLE:
            if (interpNearest) {
                double valued = testTile.getSampleDouble(testTile.getMinX(),
                        testTile.getMinY() + 2, 0);
                assertFalse(valued == destinationNoData);
            } else {
                double valued = testTile.getSampleDouble(testTile.getMinX(), testTile.getMinY(), 0);
                assertFalse(valued == destinationNoData);
            }
            break;
        default:
            throw new IllegalArgumentException("Wrong data type");
        }
    }

    protected void testEmptyImage(int dataType, Raster simpleTile, boolean isBinary) {

        int tileminX = simpleTile.getMinX();
        int tileminY = simpleTile.getMinY();
        int tileWidth = tileminX + simpleTile.getWidth();
        int tileHeight = tileminY + simpleTile.getHeight();
        
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_INT:
            int minValue = Integer.MAX_VALUE;
            int maxValue = Integer.MIN_VALUE;

            for (int i = tileminY; i < tileHeight; i++) {
                for (int j = tileminX; j < tileWidth; j++) {
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
            //assertFalse(minValue == maxValue);
            assertFalse(minValue == Integer.MAX_VALUE);
            assertFalse(maxValue == Integer.MIN_VALUE);
            assertEquals(minValue, destinationNoData, 1E-6);
            assertEquals(maxValue, destinationNoData, 1E-6);
            break;
        case DataBuffer.TYPE_FLOAT:
            float minValuef = Float.MAX_VALUE;
            float maxValuef = -Float.MAX_VALUE;

            for (int i = tileminY; i < tileHeight; i++) {
                for (int j = tileminX; j < tileWidth; j++) {
                    float valuef = simpleTile.getSample(j, i, 0);
                    if (Float.isNaN(valuef) || valuef == Float.POSITIVE_INFINITY
                            || valuef == Float.POSITIVE_INFINITY) {
                        valuef = 255;
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
            //assertFalse((int) minValuef == (int) maxValuef);
            assertFalse(minValuef == Float.MAX_VALUE);
            assertFalse(maxValuef == -Float.MAX_VALUE);
            assertEquals(minValuef, destinationNoData, 1E-6);
            assertEquals(maxValuef, destinationNoData, 1E-6);
            break;
        case DataBuffer.TYPE_DOUBLE:
            double minValued = Double.MAX_VALUE;
            double maxValued = -Double.MAX_VALUE;

            for (int i = tileminY; i < tileHeight; i++) {
                for (int j = tileminX; j < tileWidth; j++) {
                    double valued = simpleTile.getSampleDouble(j, i, 0);
                    if (Double.isNaN(valued) || valued == Double.POSITIVE_INFINITY
                            || valued == Double.POSITIVE_INFINITY) {
                        valued = 255;
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
            //assertFalse((int) minValued == (int) maxValued);
            assertFalse(minValued == Double.MAX_VALUE);
            assertFalse(maxValued == -Double.MAX_VALUE);
            assertEquals(minValued, destinationNoData, 1E-6);
            assertEquals(maxValued, destinationNoData, 1E-6);
            break;
        default:
            throw new IllegalArgumentException("Wrong data type");
        }

    }

    protected void testGlobalAffine(boolean useROIAccessor, boolean isBinary,
            boolean bicubic2Disabled, boolean noDataRangeUsed, boolean roiPresent,
            boolean setDestinationNoData, InterpolationType interpType, TestSelection testSelect,
            ScaleType scaleValue) {

        Byte sourceNoDataByte = 100;
        Short sourceNoDataUshort = Short.MAX_VALUE - 1;
        Short sourceNoDataShort = -255;
        Integer sourceNoDataInt = Integer.MAX_VALUE - 1;
        Float sourceNoDataFloat = -15.2f;
        Double sourceNoDataDouble = Double.POSITIVE_INFINITY;

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
                noDataRangeUsed, roiPresent, setDestinationNoData, interpType, testSelect,
                scaleValue);

        dataType = DataBuffer.TYPE_USHORT;
        testAllOperation(dataType, isBinary, sourceNoDataUshort, useROIAccessor, bicubic2Disabled,
                noDataRangeUsed, roiPresent, setDestinationNoData, interpType, testSelect,
                scaleValue);

        dataType = DataBuffer.TYPE_INT;
        testAllOperation(dataType, isBinary, sourceNoDataInt, useROIAccessor, bicubic2Disabled,
                noDataRangeUsed, roiPresent, setDestinationNoData, interpType, testSelect,
                scaleValue);

        if (!isBinary) {
            dataType = DataBuffer.TYPE_SHORT;
            testAllOperation(dataType, isBinary, sourceNoDataShort, useROIAccessor,
                    bicubic2Disabled, noDataRangeUsed, roiPresent, setDestinationNoData,
                    interpType, testSelect, scaleValue);

            dataType = DataBuffer.TYPE_FLOAT;
            testAllOperation(dataType, isBinary, sourceNoDataFloat, useROIAccessor,
                    bicubic2Disabled, noDataRangeUsed, roiPresent, setDestinationNoData,
                    interpType, testSelect, scaleValue);
            dataType = DataBuffer.TYPE_DOUBLE;
            testAllOperation(dataType, isBinary, sourceNoDataDouble, useROIAccessor,
                    bicubic2Disabled, noDataRangeUsed, roiPresent, setDestinationNoData,
                    interpType, testSelect, scaleValue);
        }

    }

    protected <T extends Number & Comparable<? super T>> void testAllOperation(int dataType,
            boolean isBinary, T sourceNoData, boolean useROIAccessor, boolean bicubic2Disabled,
            boolean noDataRangeUsed, boolean roiPresent, boolean setDestinationNoData,
            InterpolationType interpType, TestSelection testSelect, ScaleType scaleValue) {

        RenderedImage sourceImage = createTestImage(dataType, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                sourceNoData, isBinary);
        testImageAffine(sourceImage, dataType, sourceNoData, useROIAccessor, isBinary,
                bicubic2Disabled, noDataRangeUsed, roiPresent, setDestinationNoData,
                TransformationType.ROTATE_OP, interpType, testSelect, scaleValue);
        testImageAffine(sourceImage, dataType, sourceNoData, useROIAccessor, isBinary,
                bicubic2Disabled, noDataRangeUsed, roiPresent, setDestinationNoData,
                TransformationType.TRANSLATE_OP, interpType, testSelect, scaleValue);
        testImageAffine(sourceImage, dataType, sourceNoData, useROIAccessor, isBinary,
                bicubic2Disabled, noDataRangeUsed, roiPresent, setDestinationNoData,
                TransformationType.SCALE_OP, interpType, testSelect, scaleValue);
        testImageAffine(sourceImage, dataType, sourceNoData, useROIAccessor, isBinary,
                bicubic2Disabled, noDataRangeUsed, roiPresent, setDestinationNoData,
                TransformationType.ALL, interpType, testSelect, scaleValue);

    }
}
