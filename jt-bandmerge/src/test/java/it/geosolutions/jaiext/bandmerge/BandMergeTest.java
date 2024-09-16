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
package it.geosolutions.jaiext.bandmerge;

import com.sun.media.jai.util.ImageUtil;
import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.testclasses.TestBase;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.operator.BandSelectDescriptor;
import javax.media.jai.operator.TranslateDescriptor;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * This test class checks if the BandMergeOpImage is able to merge various multibanded images into a single multibanded image. The test is made by
 * taking 4 images for each JAI allowed data type, and then passing them to the BandMerge operator. The test ensures that the final image band number
 * is equal to the total sources band number and each sample value is equal to that of the related source image band. If No Data are present, then the
 * test check if the every No Data value is set to the "destination No Data value".
 * 
 * @author geosolutions
 * 
 */

public class BandMergeTest extends TestBase {

    /** Constant indicating the image height */
    private final static int IMAGE_HEIGHT = 128;

    /** Constant indicating the image width */
    private final static int IMAGE_WIDTH = 128;

    /** Constant indicating the final number of bands for each merged image */
    private final static int BAND_NUMBER = 4;

    /** Tolerance value used for double comparison */
    private final static double TOLERANCE = 0.1d;

    private static final int ROI_WIDTH = 40;

    private static final int ROI_HEIGHT = 40;

    /** RenderedImage array used for performing the tests */
    private static RenderedImage[][] images;

    /** No Data Range array for Byte images */
    private static Range[] noDataByte;

    /** No Data Range array for Unsigned Short images */
    private static Range[] noDataUShort;

    /** No Data Range array for Short images */
    private static Range[] noDataShort;

    /** No Data Range array for Integer images */
    private static Range[] noDataInt;

    /** No Data Range array for Float images */
    private static Range[] noDataFloat;

    /** No Data Range array for Double images */
    private static Range[] noDataDouble;

    /** Double value used for destination No Data */
    private static double destNoData;

    /** ROI to use */
    private static ROI roiData;

    @BeforeClass
    public static void initialSetup() {
        // This parameter is set to true so that the image are filled with data
        IMAGE_FILLER = true;
        // Creation of all the image array
        // The 6 inner arrays contain 4 images to merge
        // Each inner array is associated with a different data type
        images = new RenderedImage[6][4];

        byte noDataB = 50;
        short noDataS = 50;
        int noDataI = 50;
        float noDataF = 50;
        double noDataD = 50;

        for (int band = 0; band < BAND_NUMBER; band++) {
            images[DataBuffer.TYPE_BYTE][band] = createTestImage(DataBuffer.TYPE_BYTE, IMAGE_WIDTH,
                    IMAGE_HEIGHT, noDataB, false, 1);
            images[DataBuffer.TYPE_USHORT][band] = createTestImage(DataBuffer.TYPE_USHORT,
                    IMAGE_WIDTH, IMAGE_HEIGHT, noDataS, false, 1);
            images[DataBuffer.TYPE_SHORT][band] = createTestImage(DataBuffer.TYPE_SHORT,
                    IMAGE_WIDTH, IMAGE_HEIGHT, noDataS, false, 1);
            images[DataBuffer.TYPE_INT][band] = createTestImage(DataBuffer.TYPE_INT, IMAGE_WIDTH,
                    IMAGE_HEIGHT, noDataI, false, 1);
            images[DataBuffer.TYPE_FLOAT][band] = createTestImage(DataBuffer.TYPE_FLOAT,
                    IMAGE_WIDTH, IMAGE_HEIGHT, noDataF, false, 1);
            images[DataBuffer.TYPE_DOUBLE][band] = createTestImage(DataBuffer.TYPE_DOUBLE,
                    IMAGE_WIDTH, IMAGE_HEIGHT, noDataD, false, 1);
        }

        IMAGE_FILLER = false;

        // No Data Ranges
        boolean minIncluded = true;
        boolean maxIncluded = true;

        noDataByte = new Range[] { RangeFactory.create(noDataB, minIncluded, noDataB, maxIncluded) };
        noDataUShort = new Range[] { RangeFactory.createU(noDataS, minIncluded, noDataS,
                maxIncluded) };
        noDataShort = new Range[] { RangeFactory.create(noDataS, minIncluded, noDataS, maxIncluded) };
        noDataInt = new Range[] { RangeFactory.create(noDataI, minIncluded, noDataI, maxIncluded) };
        noDataFloat = new Range[] { RangeFactory.create(noDataF, minIncluded, noDataF, maxIncluded,
                true) };
        noDataDouble = new Range[] { RangeFactory.create(noDataD, minIncluded, noDataD,
                maxIncluded, true) };

        // Destination No Data
        destNoData = 100;

        // ROI
        roiData = new ROIShape(new Rectangle(0, 0, ROI_WIDTH, ROI_HEIGHT));
    }

    @Test
    public void testBandMerge() {
        // This test checks the BandMerge operation on all the possible data types without No Data
        boolean noDataUsed = false;
        boolean roiUsed = false;

        testBandMerge(images[0], noDataUsed, roiUsed, BAND_NUMBER);
        testBandMerge(images[1], noDataUsed, roiUsed, BAND_NUMBER);
        testBandMerge(images[2], noDataUsed, roiUsed, BAND_NUMBER);
        testBandMerge(images[3], noDataUsed, roiUsed, BAND_NUMBER);
        testBandMerge(images[4], noDataUsed, roiUsed, BAND_NUMBER);
        testBandMerge(images[5], noDataUsed, roiUsed, BAND_NUMBER);
    }

    @Test
    public void testBandMergeNoData() {
        // This test checks the BandMerge operation on all the possible data types with No Data
        boolean noDataUsed = true;
        boolean roiUsed = false;

        testBandMerge(images[0], noDataUsed, roiUsed, BAND_NUMBER);
        testBandMerge(images[1], noDataUsed, roiUsed, BAND_NUMBER);
        testBandMerge(images[2], noDataUsed, roiUsed, BAND_NUMBER);
        testBandMerge(images[3], noDataUsed, roiUsed, BAND_NUMBER);
        testBandMerge(images[4], noDataUsed, roiUsed, BAND_NUMBER);
        testBandMerge(images[5], noDataUsed, roiUsed, BAND_NUMBER);
    }

    @Test
    public void testBandMergeROI() {
        // This test checks the BandMerge operation on all the possible data types with ROI
        boolean noDataUsed = false;
        boolean roiUsed = true;

        testBandMerge(images[0], noDataUsed, roiUsed, BAND_NUMBER);
        testBandMerge(images[1], noDataUsed, roiUsed, BAND_NUMBER);
        testBandMerge(images[2], noDataUsed, roiUsed, BAND_NUMBER);
        testBandMerge(images[3], noDataUsed, roiUsed, BAND_NUMBER);
        testBandMerge(images[4], noDataUsed, roiUsed, BAND_NUMBER);
        testBandMerge(images[5], noDataUsed, roiUsed, BAND_NUMBER);
    }

    @Test
    public void testBandMergeNotIntersectingROI() {
        boolean roiUsed = true;

        // ROI to use
        ROI roi = null;
        if (roiUsed) {
            roi = roiData;
        }

        // BandMerge operation
        ImageLayout layout = new ImageLayout(images[0][0]);
        layout.setTileHeight(32);
        layout.setTileWidth(32);
        RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);
        RenderedOp merged = BandMergeDescriptor
                .create(null, destNoData, false, hints, null, roi, images[0]);

        // Raster object
        // Getting data out of ROI
        int outTileX = 3;
        int outTileY = 3;
        Raster outOfRoiTile = merged.getTile(outTileX, outTileY);

        // Tile bounds
        int minX = outOfRoiTile.getMinX();
        int minY = outOfRoiTile.getMinY();
        int maxX = outOfRoiTile.getWidth() + minX;
        int maxY = outOfRoiTile.getHeight() + minY;
        // Cycle on all the tile Bands
        for (int b = 0; b < 3; b++) {
            // Selection of the source raster associated with the band
            // Cycle on the y-axis
            for (int x = minX; x < maxX; x++) {
                // Cycle on the x-axis
                for (int y = minY; y < maxY; y++) {
                    // Calculated value
                    double value = outOfRoiTile.getSampleDouble(x, y, b);

                    // ROI CHECK
                    assertFalse(roi.contains(x, y));
                    // Comparison if the final value is not inside a ROI
                    assertEquals(value, destNoData, TOLERANCE);
                }
            }
        }
        // Disposal of the output image
        merged.dispose();
    }

    @Test
    public void testBandMergeNoDataROI() {
        // This test checks the BandMerge operation on all the possible data types with No Data and ROI
        boolean noDataUsed = true;
        boolean roiUsed = true;

        testBandMerge(images[0], noDataUsed, roiUsed, BAND_NUMBER);
        testBandMerge(images[1], noDataUsed, roiUsed, BAND_NUMBER);
        testBandMerge(images[2], noDataUsed, roiUsed, BAND_NUMBER);
        testBandMerge(images[3], noDataUsed, roiUsed, BAND_NUMBER);
        testBandMerge(images[4], noDataUsed, roiUsed, BAND_NUMBER);
        testBandMerge(images[5], noDataUsed, roiUsed, BAND_NUMBER);
    }

    @Test
    public void testExtendedBandMerge() {
        // This test checks the BandMerge operation on all the possible data types without No Data and ROI
        // Also it tests if the use of AffineTransformations is correct
        boolean noDataUsed = false;
        boolean roiUsed = false;

        testExtendedBandMerge(images[0], noDataUsed, roiUsed, BAND_NUMBER);
        testExtendedBandMerge(images[1], noDataUsed, roiUsed, BAND_NUMBER);
        testExtendedBandMerge(images[2], noDataUsed, roiUsed, BAND_NUMBER);
        testExtendedBandMerge(images[3], noDataUsed, roiUsed, BAND_NUMBER);
        testExtendedBandMerge(images[4], noDataUsed, roiUsed, BAND_NUMBER);
        testExtendedBandMerge(images[5], noDataUsed, roiUsed, BAND_NUMBER);
    }

    @Test
    public void testExtendedBandMergeNoData() {
        // This test checks the BandMerge operation on all the possible data types with No Data
        // Also it tests if the use of AffineTransformations is correct
        boolean noDataUsed = true;
        boolean roiUsed = false;

        testExtendedBandMerge(images[0], noDataUsed, roiUsed, BAND_NUMBER);
        testExtendedBandMerge(images[1], noDataUsed, roiUsed, BAND_NUMBER);
        testExtendedBandMerge(images[2], noDataUsed, roiUsed, BAND_NUMBER);
        testExtendedBandMerge(images[3], noDataUsed, roiUsed, BAND_NUMBER);
        testExtendedBandMerge(images[4], noDataUsed, roiUsed, BAND_NUMBER);
        testExtendedBandMerge(images[5], noDataUsed, roiUsed, BAND_NUMBER);
    }

    @Test
    public void testExtendedBandMergeROI() {
        // This test checks the BandMerge operation on all the possible data types with ROI
        // Also it tests if the use of AffineTransformations is correct
        boolean noDataUsed = false;
        boolean roiUsed = true;

        testExtendedBandMerge(images[0], noDataUsed, roiUsed, BAND_NUMBER);
        testExtendedBandMerge(images[1], noDataUsed, roiUsed, BAND_NUMBER);
        testExtendedBandMerge(images[2], noDataUsed, roiUsed, BAND_NUMBER);
        testExtendedBandMerge(images[3], noDataUsed, roiUsed, BAND_NUMBER);
        testExtendedBandMerge(images[4], noDataUsed, roiUsed, BAND_NUMBER);
        testExtendedBandMerge(images[5], noDataUsed, roiUsed, BAND_NUMBER);
    }

    @Test
    public void testExtendedBandMergeNoDataROI() {
        // This test checks the BandMerge operation on all the possible data types with No Data and ROI
        // Also it tests if the use of AffineTransformations is correct
        boolean noDataUsed = true;
        boolean roiUsed = true;

        testExtendedBandMerge(images[0], noDataUsed, roiUsed, BAND_NUMBER);
        testExtendedBandMerge(images[1], noDataUsed, roiUsed, BAND_NUMBER);
        testExtendedBandMerge(images[2], noDataUsed, roiUsed, BAND_NUMBER);
        testExtendedBandMerge(images[3], noDataUsed, roiUsed, BAND_NUMBER);
        testExtendedBandMerge(images[4], noDataUsed, roiUsed, BAND_NUMBER);
        testExtendedBandMerge(images[5], noDataUsed, roiUsed, BAND_NUMBER);
    }

    @AfterClass
    public static void disposal() {
        // Disposal of the images
        for (int band = 0; band < BAND_NUMBER; band++) {
            ((TiledImage) images[DataBuffer.TYPE_BYTE][band]).dispose();
            ((TiledImage) images[DataBuffer.TYPE_USHORT][band]).dispose();
            ((TiledImage) images[DataBuffer.TYPE_SHORT][band]).dispose();
            ((TiledImage) images[DataBuffer.TYPE_INT][band]).dispose();
            ((TiledImage) images[DataBuffer.TYPE_FLOAT][band]).dispose();
            ((TiledImage) images[DataBuffer.TYPE_DOUBLE][band]).dispose();
        }
    }

    private void testBandMerge(RenderedImage[] sources, boolean noDataUsed, boolean roiUsed, int numberOfBands) {
        // Optional No Data Range used
        Range[] noData;
        // Source image data type
        int dataType = sources[0].getSampleModel().getDataType();
        // If no Data are present, the No Data Range associated is used
        if (noDataUsed) {

            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                noData = noDataByte;
                break;
            case DataBuffer.TYPE_USHORT:
                noData = noDataUShort;
                break;
            case DataBuffer.TYPE_SHORT:
                noData = noDataShort;
                break;
            case DataBuffer.TYPE_INT:
                noData = noDataInt;
                break;
            case DataBuffer.TYPE_FLOAT:
                noData = noDataFloat;
                break;
            case DataBuffer.TYPE_DOUBLE:
                noData = noDataDouble;
                break;
            default:
                throw new IllegalArgumentException("Wrong data type");
            }
        } else {
            noData = null;
        }

        // ROI to use
        ROI roi = null;
        if (roiUsed) {
            roi = roiData;
        }

        // BandMerge operation
        RenderedOp merged = BandMergeDescriptor
                .create(noData, destNoData, false, null, null, roi, sources);
        // Check if the bands number is the same
        assertEquals(numberOfBands, merged.getNumBands());
        // Ensure the final ColorModel exists and has not an alpha band
        assertNotNull(merged.getColorModel());
        if (merged.getSampleModel().getDataType() != DataBuffer.TYPE_SHORT) {
            assertTrue(!merged.getColorModel().hasAlpha());
        }
        // Upper-Left tile indexes
        int minTileX = merged.getMinTileX();
        int minTileY = merged.getMinTileY();
        // Raster object
        Raster upperLeftTile = merged.getTile(minTileX, minTileY);
        // Tile bounds
        int minX = upperLeftTile.getMinX();
        int minY = upperLeftTile.getMinY();
        int maxX = upperLeftTile.getWidth() + minX;
        int maxY = upperLeftTile.getHeight() + minY;
        // Cycle on all the tile Bands
        for (int b = 0; b < BAND_NUMBER; b++) {
            // Selection of the source raster associated with the band
            Raster bandRaster = sources[b].getTile(minTileX, minTileY);
            // Cycle on the y-axis
            for (int x = minX; x < maxX; x++) {
                // Cycle on the x-axis
                for (int y = minY; y < maxY; y++) {
                    // Calculated value
                    double value = upperLeftTile.getSampleDouble(x, y, b);
                    // Old band value
                    double valueOld = bandRaster.getSampleDouble(x, y, 0);

                    // ROI CHECK
                    boolean contained = true;
                    if (roiUsed) {
                        if (!roi.contains(x, y)) {
                            contained = false;
                            // Comparison if the final value is not inside a ROI
                            assertEquals(value, destNoData, TOLERANCE);
                        }
                    }

                    if (contained) {
                        // If no Data are present, no data check is performed
                        if (noDataUsed) {
                            switch (dataType) {
                            case DataBuffer.TYPE_BYTE:
                                byte sampleB = ImageUtil.clampRoundByte(value);
                                byte sampleBOld = ImageUtil.clampRoundByte(valueOld);
                                if (noData[0].contains(sampleBOld)) {
                                    assertEquals(sampleB, destNoData, TOLERANCE);
                                } else {
                                    assertEquals(sampleB, valueOld, TOLERANCE);
                                }
                                break;
                            case DataBuffer.TYPE_USHORT:
                                short sampleUS = ImageUtil.clampRoundUShort(value);
                                short sampleUSOld = ImageUtil.clampRoundUShort(valueOld);
                                if (noData[0].contains(sampleUSOld)) {
                                    assertEquals(sampleUS, destNoData, TOLERANCE);
                                } else {
                                    assertEquals(sampleUS, valueOld, TOLERANCE);
                                }
                                break;
                            case DataBuffer.TYPE_SHORT:
                                short sampleS = ImageUtil.clampRoundShort(value);
                                short sampleSOld = ImageUtil.clampRoundShort(valueOld);
                                if (noData[0].contains(sampleSOld)) {
                                    assertEquals(sampleS, destNoData, TOLERANCE);
                                } else {
                                    assertEquals(sampleS, valueOld, TOLERANCE);
                                }
                                break;
                            case DataBuffer.TYPE_INT:
                                int sampleI = ImageUtil.clampRoundInt(value);
                                int sampleIOld = ImageUtil.clampRoundInt(valueOld);
                                if (noData[0].contains(sampleIOld)) {
                                    assertEquals(sampleI, destNoData, TOLERANCE);
                                } else {
                                    assertEquals(sampleI, valueOld, TOLERANCE);
                                }
                                break;
                            case DataBuffer.TYPE_FLOAT:
                                float sampleF = ImageUtil.clampFloat(value);
                                float sampleFOld = ImageUtil.clampFloat(valueOld);
                                if (noData[0].contains(sampleFOld)) {
                                    assertEquals(sampleF, destNoData, TOLERANCE);
                                } else {
                                    assertEquals(sampleF, valueOld, TOLERANCE);
                                }
                                break;
                            case DataBuffer.TYPE_DOUBLE:
                                if (noData[0].contains(valueOld)) {
                                    assertEquals(value, destNoData, TOLERANCE);
                                } else {
                                    assertEquals(value, valueOld, TOLERANCE);
                                }
                                break;
                            default:
                                throw new IllegalArgumentException("Wrong data type");
                            }
                        } else {
                            // Else a simple value comparison is done
                            assertEquals(value, valueOld, TOLERANCE);
                        }
                    }
                }
            }
        }
        // Disposal of the output image
        merged.dispose();
    }

    // This method is similar to the testBandMerge method but it tests the ExtendedBandMergeOpImage class
    private void testExtendedBandMerge(RenderedImage[] sources, boolean noDataUsed, boolean roiUsed, int numberOfBands) {
        // Optional No Data Range used
        Range[] noData;
        // Source image data type
        int dataType = sources[0].getSampleModel().getDataType();
        // If no Data are present, the No Data Range associated is used
        if (noDataUsed) {

            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                noData = noDataByte;
                break;
            case DataBuffer.TYPE_USHORT:
                noData = noDataUShort;
                break;
            case DataBuffer.TYPE_SHORT:
                noData = noDataShort;
                break;
            case DataBuffer.TYPE_INT:
                noData = noDataInt;
                break;
            case DataBuffer.TYPE_FLOAT:
                noData = noDataFloat;
                break;
            case DataBuffer.TYPE_DOUBLE:
                noData = noDataDouble;
                break;
            default:
                throw new IllegalArgumentException("Wrong data type");
            }
        } else {
            noData = null;
        }

        // ROI to use
        ROI roi = null;
        if (roiUsed) {
            roi = roiData;
        }

        // New array ofr the transformed source images
        RenderedOp[] translated = new RenderedOp[sources.length];

        List<AffineTransform> transform = new ArrayList<AffineTransform>();

        for (int i = 0; i < sources.length; i++) {
            // Translation coefficients
            int xTrans = (int) (Math.random() * 10);
            int yTrans = (int) (Math.random() * 10);
            // Translation operation
            AffineTransform tr = AffineTransform.getTranslateInstance(xTrans, yTrans);
            // Addition to the transformations list
            transform.add(tr);
            // Translation of the image
            translated[i] = TranslateDescriptor.create(sources[i], (float) xTrans, (float) yTrans,
                    null, null);
        }
        // Definition of the final image dimensions
        ImageLayout layout = new ImageLayout();
        layout.setMinX(sources[0].getMinX());
        layout.setMinY(sources[0].getMinY());
        layout.setWidth(sources[0].getWidth());
        layout.setHeight(sources[0].getHeight());

        RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);

        // BandMerge operation
        RenderedOp merged = BandMergeDescriptor.create(noData, destNoData, false, hints, transform, roi,
                translated);

        Assert.assertNotNull(merged.getTiles());
        // Check if the bands number is the same
        assertEquals(numberOfBands, merged.getNumBands());
        // Upper-Left tile indexes
        int minTileX = merged.getMinTileX();
        int minTileY = merged.getMinTileY();
        // Raster object
        Raster upperLeftTile = merged.getTile(minTileX, minTileY);
        // Tile bounds
        int minX = upperLeftTile.getMinX();
        int minY = upperLeftTile.getMinY();
        int maxX = upperLeftTile.getWidth() + minX;
        int maxY = upperLeftTile.getHeight() + minY;

        // Source corners
        final int dstMinX = merged.getMinX();
        final int dstMinY = merged.getMinY();
        final int dstMaxX = merged.getMaxX();
        final int dstMaxY = merged.getMaxY();

        Point2D ptDst = new Point2D.Double(0, 0);
        Point2D ptSrc = new Point2D.Double(0, 0);

        // Cycle on all the tile Bands
        for (int b = 0; b < numberOfBands; b++) {
            RandomIter iter = RandomIterFactory.create(translated[b], null, true, true);

            // Source corners
            final int srcMinX = translated[b].getMinX();
            final int srcMinY = translated[b].getMinY();
            final int srcMaxX = translated[b].getMaxX();
            final int srcMaxY = translated[b].getMaxY();

            // Cycle on the y-axis
            for (int x = minX; x < maxX; x++) {
                // Cycle on the x-axis
                for (int y = minY; y < maxY; y++) {
                    // Calculated value
                    double value = upperLeftTile.getSampleDouble(x, y, b);
                    // If the tile pixels are outside the image bounds, then no data is set.
                    if (x < dstMinX || x >= dstMaxX || y < dstMinY || y >= dstMaxY) {
                        value = destNoData;
                    }

                    // Set the x,y destination pixel location
                    ptDst.setLocation(x, y);
                    // Map destination pixel to source pixel
                    transform.get(b).transform(ptDst, ptSrc);
                    // Source pixel indexes
                    int srcX = round(ptSrc.getX());
                    int srcY = round(ptSrc.getY());

                    double valueOld = destNoData;

                    // Check if the pixel is inside the source bounds
                    if (!(srcX < srcMinX || srcX >= srcMaxX || srcY < srcMinY || srcY >= srcMaxY)) {
                        // Old band value
                        valueOld = iter.getSampleDouble(srcX, srcY, 0);
                    }

                    // ROI CHECK
                    boolean contained = true;
                    if (roiUsed) {
                        if (!roi.contains(x, y)) {
                            contained = false;
                            // Comparison if the final value is not inside a ROI
                            assertEquals(value, destNoData, TOLERANCE);
                        }
                    }

                    if (contained) {
                        // If no Data are present, no data check is performed
                        if (noDataUsed) {
                            switch (dataType) {
                            case DataBuffer.TYPE_BYTE:
                                byte sampleB = ImageUtil.clampRoundByte(value);
                                byte sampleBOld = ImageUtil.clampRoundByte(valueOld);
                                if (noData[0].contains(sampleBOld)) {
                                    assertEquals(sampleB, destNoData, TOLERANCE);
                                } else {
                                    assertEquals(sampleB, valueOld, TOLERANCE);
                                }
                                break;
                            case DataBuffer.TYPE_USHORT:
                                short sampleUS = ImageUtil.clampRoundUShort(value);
                                short sampleUSOld = ImageUtil.clampRoundUShort(valueOld);
                                if (noData[0].contains(sampleUSOld)) {
                                    assertEquals(sampleUS, destNoData, TOLERANCE);
                                } else {
                                    assertEquals(sampleUS, valueOld, TOLERANCE);
                                }
                                break;
                            case DataBuffer.TYPE_SHORT:
                                short sampleS = ImageUtil.clampRoundShort(value);
                                short sampleSOld = ImageUtil.clampRoundShort(valueOld);
                                if (noData[0].contains(sampleSOld)) {
                                    assertEquals(sampleS, destNoData, TOLERANCE);
                                } else {
                                    assertEquals(sampleS, valueOld, TOLERANCE);
                                }
                                break;
                            case DataBuffer.TYPE_INT:
                                int sampleI = ImageUtil.clampRoundInt(value);
                                int sampleIOld = ImageUtil.clampRoundInt(valueOld);
                                if (noData[0].contains(sampleIOld)) {
                                    assertEquals(sampleI, destNoData, TOLERANCE);
                                } else {
                                    assertEquals(sampleI, valueOld, TOLERANCE);
                                }
                                break;
                            case DataBuffer.TYPE_FLOAT:
                                float sampleF = ImageUtil.clampFloat(value);
                                float sampleFOld = ImageUtil.clampFloat(valueOld);
                                if (noData[0].contains(sampleFOld)) {
                                    assertEquals(sampleF, destNoData, TOLERANCE);
                                } else {
                                    assertEquals(sampleF, valueOld, TOLERANCE);
                                }
                                break;
                            case DataBuffer.TYPE_DOUBLE:
                                if (noData[0].contains(valueOld)) {
                                    assertEquals(value, destNoData, TOLERANCE);
                                } else {
                                    assertEquals(value, valueOld, TOLERANCE);
                                }
                                break;
                            default:
                                throw new IllegalArgumentException("Wrong data type");
                            }
                        } else {
                            // Else a simple value comparison is done
                            assertEquals(value, valueOld, TOLERANCE);
                        }
                    }
                }
            }
        }
        // Disposal of the output image
        merged.dispose();
    }

    /** Returns the "round" value of a float. */
    private static int round(double f) {
        return f >= 0 ? (int) (f + 0.5F) : (int) (f - 0.5F);
    }
    
    @Test
    public void testBandMergeBandSelected() {
        testBandMergeOnBandSelected(DataBuffer.TYPE_BYTE, (byte) 0, (byte) 1);
        testBandMergeOnBandSelected(DataBuffer.TYPE_USHORT, (short) 0, (short) 1);
        testBandMergeOnBandSelected(DataBuffer.TYPE_SHORT, (short) 0, (short) 1);
        testBandMergeOnBandSelected(DataBuffer.TYPE_INT, (int) 0, (int) 1);
        testBandMergeOnBandSelected(DataBuffer.TYPE_FLOAT, (float) 0, (float) 1);
        testBandMergeOnBandSelected(DataBuffer.TYPE_DOUBLE, (double) 0, (double) 1);
    }
    
    public void testBandMergeOnBandSelected(int dataType, Number noDataValue, Number dataValue) {
        testBandMergeOnBandSelected(dataType, noDataValue, dataValue, true);
        testBandMergeOnBandSelected(dataType, noDataValue, dataValue, false);
        testBandMergeOnBandSelected(dataType, null, dataValue, true);
        testBandMergeOnBandSelected(dataType, null, dataValue, false);
    }

    private void testBandMergeOnBandSelected(int dataType, Number noDataValue, Number dataValue,
            boolean addROI) {
        Range[] nodata;
        if (noDataValue == null) {
            nodata = null;
        } else {
            switch (dataType) {
            case DataBuffer.TYPE_BYTE: {
                final byte noDataPrimitive = noDataValue.byteValue();
                nodata = new Range[] { RangeFactory.create(noDataPrimitive, noDataPrimitive) };
                break;
            }
            case DataBuffer.TYPE_SHORT: {
                final short noDataPrimitive = noDataValue.shortValue();
                nodata = new Range[] { RangeFactory.create(noDataPrimitive, noDataPrimitive) };
                break;
            }
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_INT: {
                final int noDataPrimitive = noDataValue.intValue();
                nodata = new Range[] { RangeFactory.create(noDataPrimitive, noDataPrimitive) };
                break;
            }
            case DataBuffer.TYPE_FLOAT: {
                final float noDataPrimitive = noDataValue.floatValue();
                nodata = new Range[] { RangeFactory.create(noDataPrimitive, noDataPrimitive) };
                break;
            }
            case DataBuffer.TYPE_DOUBLE: {
                final double noDataPrimitive = noDataValue.doubleValue();
                nodata = new Range[] { RangeFactory.create(noDataPrimitive, noDataPrimitive) };
                break;
            }

            default:
                throw new IllegalArgumentException();
            }
        }

        TiledImage image = (TiledImage) createTestImage(dataType, 10, 10, noDataValue, false, 3,
                dataValue);
        for (int i = 0; i < image.getHeight(); i++) {
            for (int j = 0; j < image.getWidth(); j++) {
                for (int b = 0; b < image.getNumBands(); b++) {
                    int value = b == image.getNumBands() - 1 ? 1 : 0;
                    image.setSample(j, i, b, value);
                }
            }
        }

        RenderedOp selected = BandSelectDescriptor.create(image, new int[] { 2 }, null);

        RenderedOp bandMerged;
        if (addROI) {
            ROI roi = new ROIShape(new Rectangle2D.Double(0, 0, 100, 100));
            bandMerged = BandMergeDescriptor.create(nodata, 0d, false, (RenderingHints) null, null,
                    roi, selected, selected);
        } else {
            bandMerged = BandMergeDescriptor.create(nodata, 0d, false, (RenderingHints) null,
                    selected, selected);
        }

        final Raster data = bandMerged.getData();
        for (int i = 0; i < bandMerged.getHeight(); i++) {
            for (int j = 0; j < bandMerged.getWidth(); j++) {
                for (int b = 0; b < bandMerged.getNumBands(); b++) {
                    assertEquals(1, data.getSample(j, i, b));
                }
            }
        }
    }

    @Test
    public void testMultibandMerge() {
        // setup images
        final int MULTIBAND_BAND_COUNT = 13;
        RenderedImage[] images = new RenderedImage[MULTIBAND_BAND_COUNT];
        for (int i = 0; i < images.length; i++) {
            images[i] = createTestImage(DataBuffer.TYPE_BYTE, IMAGE_WIDTH, IMAGE_HEIGHT, (byte) 50, false, 1);
        }

        try {
            // normal bandmerge
            testBandMerge(images, false, false, MULTIBAND_BAND_COUNT);
            testBandMerge(images, true, false, MULTIBAND_BAND_COUNT);
            testBandMerge(images, false, true, MULTIBAND_BAND_COUNT);
            testBandMerge(images, true, true, MULTIBAND_BAND_COUNT);

            // extended one
            testExtendedBandMerge(images, false, false, MULTIBAND_BAND_COUNT);
            testExtendedBandMerge(images, true, false, MULTIBAND_BAND_COUNT);
            testExtendedBandMerge(images, false, true, MULTIBAND_BAND_COUNT);
            testExtendedBandMerge(images, true, true, MULTIBAND_BAND_COUNT);

        } finally {
            // cleanup
            for (int i = 0; i < images.length; i++) {
                RenderedImage image = images[i];
                ((TiledImage) image).dispose();
            }
        }
    }

    @Test
    public void testExtendedWithIdentityTransform() {
        assertBandMergeImplementation(AffineTransform.getScaleInstance(1 + 1e-12, 1 + 1e-12), BandMergeOpImage.class);
        assertBandMergeImplementation(AffineTransform.getScaleInstance(1 + 1e-6, 1 + 1e-6), ExtendedBandMergeOpImage.class);
        assertBandMergeImplementation(AffineTransform.getScaleInstance(0.5, 0.5), ExtendedBandMergeOpImage.class);
        assertBandMergeImplementation(AffineTransform.getShearInstance(1e-12, 1e-12), BandMergeOpImage.class);
        assertBandMergeImplementation(AffineTransform.getShearInstance(1e-6, 1e-6), ExtendedBandMergeOpImage.class);
        assertBandMergeImplementation(AffineTransform.getTranslateInstance(1e-12, 1e-12), BandMergeOpImage.class);
        assertBandMergeImplementation(AffineTransform.getTranslateInstance(0.6, 0.6), ExtendedBandMergeOpImage.class);
    }

    public void assertBandMergeImplementation(AffineTransform affine, Class opImageClass) {
        RenderedImage[] images = BandMergeTest.images[DataBuffer.TYPE_BYTE];
        List<AffineTransform> transforms = new ArrayList<AffineTransform>();
        for (int i = 0; i < images.length; i++) {
            transforms.add(affine);
        }
        RenderedOp merged = BandMergeDescriptor.create(null, 0d, false, null, transforms, null, images);
        try {
            assertTrue(opImageClass.isInstance(merged.getRendering()));
        } finally {
            // Disposal of the output image
            merged.dispose();
        }
    }
}
