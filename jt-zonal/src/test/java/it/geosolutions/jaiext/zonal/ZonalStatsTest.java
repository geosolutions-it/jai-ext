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
package it.geosolutions.jaiext.zonal;

import static org.junit.Assert.assertEquals;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.iterator.RandomIter;
import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.stats.Statistics;
import it.geosolutions.jaiext.stats.Statistics.StatsType;
import it.geosolutions.jaiext.testclasses.TestBase;
import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.strtree.STRtree;

/**
 * This test class is used for evaluating the functionalities of the ZonalStats operation. This operation consists of calculating different statistics
 * on a list of geometries passed to the image operator. The possible operations to do are:
 * <ul>
 * <li>Mean</li>
 * <li>Sum</li>
 * <li>Max</li>
 * <li>Min</li>
 * <li>Extrema</li>
 * <li>Variance</li>
 * <li>Standard Deviation</li>
 * <li>Histogram</li>
 * <li>Mode</li>
 * <li>Median</li>
 * </ul>
 * An additional Classifier image can be used for dividing the statistics between the classes defined by it. The ZonalStatsOpImage is tested on 6
 * different images each one with a different data type (except Long data type). The operation is tested with and without the presence of NoData, with
 * and without the presence of ROI. The classifier can be used by setting to true the JVM parameter JAI.Ext.Classifier. All the results are compared
 * with the previously calculated statistics for checking the correctness of the calculations.
 */
public class ZonalStatsTest extends TestBase {

    /** Tolerance value used for comparison between double */
    private final static double TOLERANCE = 0.1d;

    /** Boolean indicating if a classifier image must be used */
    private static final boolean CLASSIFIER = Boolean.getBoolean("JAI.Ext.Classifier");

    /** Statistics operation to execute */
    private static StatsType[] stats;

    /** Source image arrays */
    private static RenderedImage[] sourceIMG;

    /** List of geometries */
    private static List<ROI> roiList;

    /** Optional classifier image */
    private static RenderedImage classifier;

    /** Spatial index for fast searching the geometries associated with a selected pixel */
    private static STRtree[][] spatial;

    /** Array containing the list of all the zones */
    private static List<ZoneGeometry>[][] zonesLists;

    /** Union of all the input geometries bounds */
    private static Rectangle union;

    /** Selected bands to perform statistical calculations */
    private static int[] bands;

    /** NoData value for Byte images */
    private static byte noDataB;

    /** NoData value for Unsigned Short images */
    private static short noDataU;

    /** NoData value for Short images */
    private static short noDataS;

    /** NoData value for Integer images */
    private static int noDataI;

    /** NoData value for Float images */
    private static float noDataF;

    /** NoData value for Double images */
    private static double noDataD;

    /** NoData Range for Byte data */
    private static Range noDataByte;

    /** NoData Range for Unsigned Short data */
    private static Range noDataUShort;

    /** NoData Range for Short data */
    private static Range noDataShort;

    /** NoData Range for Integer data */
    private static Range noDataInt;

    /** NoData Range for Float data */
    private static Range noDataFloat;

    /** NoData Range for Double data */
    private static Range noDataDouble;

    /** Array indicating the minimum bounds for each band */
    private static double[] minBound;

    /** Array indicating the maximum bounds for each band */
    private static double[] maxBound;

    /** Array indicating the number of bins for each band */
    private static int[] numBins;

    private static List<Range>[] rangeList;

    private static ROI roiObject;

    @BeforeClass
    public static void initialSetup() {

        // Definition of the Histogram, Mode and Median parameters
        minBound = new double[] { -3, -3, -3 };
        maxBound = new double[] { 3, 3, 3 };
        numBins = new int[] { 4, 4, 4 };

        // Statistics types
        stats = new StatsType[] { StatsType.MEAN, StatsType.SUM, StatsType.MAX, StatsType.MIN,
                StatsType.EXTREMA, StatsType.VARIANCE, StatsType.DEV_STD, StatsType.HISTOGRAM,
                StatsType.MODE, StatsType.MEDIAN };

        roiList = new ArrayList<ROI>();

        // Geometry parameters
        int initial_value = 10;
        int final_value = 30;

        int dimension = (final_value - initial_value);

        int interval = dimension / 10;
        // ROI LIST
        for (int i = 0; i < 10; i++) {
            ROI roiGeom = new ROIShape(new Rectangle(initial_value, initial_value, dimension,
                    dimension));
            roiList.add(roiGeom);
            // Translation of the geometry with overlap
            initial_value += interval;
        }

        // Creation of the ROI
        // ROI creation
        Rectangle roiBounds = new Rectangle(5, 5, DEFAULT_WIDTH / 4, DEFAULT_HEIGHT / 4);
        roiObject = new ROIShape(roiBounds);

        rangeList = new ArrayList[6];

        rangeList[0] = new ArrayList<Range>(1);
        rangeList[1] = new ArrayList<Range>(1);
        rangeList[2] = new ArrayList<Range>(1);
        rangeList[3] = new ArrayList<Range>(1);
        rangeList[4] = new ArrayList<Range>(1);
        rangeList[5] = new ArrayList<Range>(1);

        // Data Range creation
        rangeList[0].add(RangeFactory.create((byte) 0, true, (byte) 100, true));
        rangeList[1].add(RangeFactory.createU((short) 0.0, true, (short) 100, true));
        rangeList[2].add(RangeFactory.create((short) -1, true, (short) 100, true));
        rangeList[3].add(RangeFactory.create(-1, true, 100, true));
        rangeList[4].add(RangeFactory.create(-1f, true, 100f, true, false));
        rangeList[5].add(RangeFactory.create(-1d, true, 100d, true, false));

        // Band array creation
        bands = new int[] { 0 };

        // Spatial indexing
        // Creation of the spatial indexes and of the final List results
        spatial = new STRtree[6][4];

        zonesLists = new ArrayList[6][4];

        for (int i = 0; i < 6; i++) {
            // Spatial Indexes
            spatial[i][0] = new STRtree();
            spatial[i][1] = new STRtree();
            spatial[i][2] = new STRtree();
            spatial[i][3] = new STRtree();
            // Zones Lists
            zonesLists[i][0] = new ArrayList<ZoneGeometry>();
            zonesLists[i][1] = new ArrayList<ZoneGeometry>();
            zonesLists[i][2] = new ArrayList<ZoneGeometry>();
            zonesLists[i][3] = new ArrayList<ZoneGeometry>();
        }

        // Bounds Union
        union = new Rectangle(roiList.get(0).getBounds());
        // Insertion of the zones to the spatial index and union of the bounds for every ROI/Zone object
        for (ROI roi : roiList) {

            // Spatial index creation
            Rectangle rect = roi.getBounds();
            double minX = rect.getMinX();
            double maxX = rect.getMaxX();
            double minY = rect.getMinY();
            double maxY = rect.getMaxY();
            Envelope env = new Envelope(minX, maxX, minY, maxY);
            // Union
            union = union.union(rect);
            // Addition to the geometries list
            for (int i = 0; i < 6; i++) {
                for (int z = 0; z < 4; z++) {
                    // Creation of a new ZoneGeometry
                    ZoneGeometry geom = new ZoneGeometry(roi, rangeList[i], bands, stats,
                            CLASSIFIER, minBound, maxBound, numBins);
                    spatial[i][z].insert(env, geom);

                    zonesLists[i][z].add(geom);

                }
            }
        }

        // Classifier
        classifier = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH, DEFAULT_HEIGHT, 2, false);
        // Classifier bound
        Rectangle rectClass = new Rectangle(classifier.getMinX(), classifier.getMinY(),
                classifier.getWidth(), classifier.getHeight());
        // Iterator on the classifier image
        RandomIter iterator = RandomIterFactory.create(classifier, rectClass, true, true);

        // No Data values
        noDataB = 50;
        noDataU = 50;
        noDataS = 50;
        noDataI = 50;
        noDataF = 50;
        noDataD = 50;
        // Range parameters
        boolean minIncluded = true;
        boolean maxIncluded = true;
        boolean nanIncluded = true;

        noDataByte = RangeFactory.create(noDataB, minIncluded, noDataB, maxIncluded);
        noDataUShort = RangeFactory.createU(noDataU, minIncluded, noDataU, maxIncluded);
        noDataShort = RangeFactory.create(noDataS, minIncluded, noDataS, maxIncluded);
        noDataInt = RangeFactory.create(noDataI, minIncluded, noDataI, maxIncluded);
        noDataFloat = RangeFactory.create(noDataF, minIncluded, noDataF, maxIncluded, nanIncluded);
        noDataDouble = RangeFactory.create(noDataD, minIncluded, noDataD, maxIncluded, nanIncluded);

        // Image creations
        sourceIMG = new RenderedImage[6];

        // Creation of the source images
        IMAGE_FILLER = false;

        sourceIMG[0] = createTestImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataB, false);
        sourceIMG[1] = createTestImage(DataBuffer.TYPE_USHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataU, false);
        sourceIMG[2] = createTestImage(DataBuffer.TYPE_SHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataS, false);
        sourceIMG[3] = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataI,
                false);
        sourceIMG[4] = createTestImage(DataBuffer.TYPE_FLOAT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataF, false);
        sourceIMG[5] = createTestImage(DataBuffer.TYPE_DOUBLE, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataD, false);

        // Image Tiles positions
        int minTileX = sourceIMG[0].getMinTileX();
        int minTileY = sourceIMG[0].getMinTileY();
        int maxTileX = minTileX + sourceIMG[0].getNumXTiles();
        int maxTileY = minTileY + sourceIMG[0].getNumYTiles();

        // STATISTICAL CALCULATION

        // Cycle on all pixels of the 6 images
        for (int i = minTileX; i < maxTileX; i++) {
            for (int j = minTileY; j < maxTileY; j++) {
                // Selection of a Raster
                Raster arrayRas = sourceIMG[0].getTile(i, j);
                // Raster pixels positions
                int minX = arrayRas.getMinX();
                int minY = arrayRas.getMinY();
                int maxX = minX + arrayRas.getWidth();
                int maxY = minY + arrayRas.getHeight();

                // Cycle on the Raster pixels
                for (int x = minX; x < maxX; x++) {
                    for (int y = minY; y < maxY; y++) {

                        for (int z = 0; z < 4; z++) {
                            // Check if the selected pixel is inside the union of all the geometries
                            if (union.contains(x, y)) {
                                byte value = (byte) arrayRas.getSample(x, y, 0);
                                // pixel coordinate creation for spatial indexing
                                Coordinate p = new Coordinate(x, y);
                                // Creation of an Envelop containing the pixel coordinates
                                Envelope searchEnv = new Envelope(p);
                                // Query on the geometry list
                                for (int v = 0; v < 6; v++) {
                                    List<ZoneGeometry> geomList = spatial[v][z].query(searchEnv);
                                    // classId classifier initial value
                                    int classId = 0;
                                    // If the classifier is present then the classId value is taken
                                    if (CLASSIFIER) {
                                        // Selection of the classId point
                                        classId = iterator.getSample(x, y, 0);
                                    }
                                    // Cycle on all the geometries found
                                    for (ZoneGeometry zoneGeo : geomList) {

                                        ROI geometry = zoneGeo.getROI();

                                        // if every geometry really contains the selected point
                                        if (geometry.contains(x, y)) {

                                            // Cycle for the 2 cases: with and without NoData
                                            switch (z) {
                                            case 0:
                                                zoneGeo.add(value, 0, classId, rangeList[v].get(0));
                                                break;
                                            case 1:
                                                if (!noDataByte.contains(value)) {
                                                    zoneGeo.add(value, 0, classId,
                                                            rangeList[v].get(0));
                                                }
                                                break;
                                            case 2:
                                                if (roiObject.contains(x, y)) {
                                                    zoneGeo.add(value, 0, classId,
                                                            rangeList[v].get(0));
                                                }
                                                break;
                                            case 3:
                                                if (!noDataByte.contains(value)
                                                        && roiObject.contains(x, y)) {
                                                    zoneGeo.add(value, 0, classId,
                                                            rangeList[v].get(0));
                                                }
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testNoNoDataNoROI() {
        // This test calculates zonal statistics without NoData
        boolean noDataRangeUsed = false;
        boolean roiUsed = false;
        boolean useROIAccessor = false;

        testZonalStats(sourceIMG[0], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[0]);
        testZonalStats(sourceIMG[1], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[1]);
        testZonalStats(sourceIMG[2], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[2]);
        testZonalStats(sourceIMG[3], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[3]);
        testZonalStats(sourceIMG[4], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[4]);
        testZonalStats(sourceIMG[5], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[5]);

    }

    @Test
    public void testOnlyROI() {
        // This test calculates zonal statistics with NoData and without ROI
        boolean noDataRangeUsed = false;
        boolean roiUsed = true;
        boolean useROIAccessor = false;

        testZonalStats(sourceIMG[0], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[0]);
        testZonalStats(sourceIMG[1], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[1]);
        testZonalStats(sourceIMG[2], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[2]);
        testZonalStats(sourceIMG[3], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[3]);
        testZonalStats(sourceIMG[4], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[4]);
        testZonalStats(sourceIMG[5], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[5]);

    }

    @Test
    public void testOnlyROIRasterAccessor() {
        // This test calculates zonal statistics without NoData
        boolean noDataRangeUsed = false;
        boolean roiUsed = true;
        boolean useROIAccessor = true;

        testZonalStats(sourceIMG[0], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[0]);
        testZonalStats(sourceIMG[1], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[1]);
        testZonalStats(sourceIMG[2], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[2]);
        testZonalStats(sourceIMG[3], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[3]);
        testZonalStats(sourceIMG[4], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[4]);
        testZonalStats(sourceIMG[5], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[5]);

    }

    @Test
    public void testOnlyNoData() {
        // This test calculates zonal statistics with NoData and without ROI
        boolean noDataRangeUsed = true;
        boolean roiUsed = false;
        boolean useROIAccessor = false;

        testZonalStats(sourceIMG[0], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[0]);
        testZonalStats(sourceIMG[1], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[1]);
        testZonalStats(sourceIMG[2], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[2]);
        testZonalStats(sourceIMG[3], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[3]);
        testZonalStats(sourceIMG[4], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[4]);
        testZonalStats(sourceIMG[5], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[5]);

    }

    @Test
    public void testNoDataAndROI() {
        // This test calculates zonal statistics without NoData
        boolean noDataRangeUsed = true;
        boolean roiUsed = true;
        boolean useROIAccessor = false;

        testZonalStats(sourceIMG[0], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[0]);
        testZonalStats(sourceIMG[1], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[1]);
        testZonalStats(sourceIMG[2], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[2]);
        testZonalStats(sourceIMG[3], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[3]);
        testZonalStats(sourceIMG[4], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[4]);
        testZonalStats(sourceIMG[5], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[5]);

    }

    @Test
    public void testNoDataAndROIRasterAccessor() {
        // This test calculates zonal statistics with NoData and without ROI
        boolean noDataRangeUsed = true;
        boolean roiUsed = true;
        boolean useROIAccessor = true;

        testZonalStats(sourceIMG[0], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[0]);
        testZonalStats(sourceIMG[1], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[1]);
        testZonalStats(sourceIMG[2], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[2]);
        testZonalStats(sourceIMG[3], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[3]);
        testZonalStats(sourceIMG[4], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[4]);
        testZonalStats(sourceIMG[5], CLASSIFIER, noDataRangeUsed, roiUsed, useROIAccessor,
                rangeList[5]);

    }

    @Test
    /**
     * Test building up a set of pre-defined ranges and classifying stats for each. 
     */
    public void testStatsEntireImageNoROI() {

        // build up the ranges
        int n = 4;
        byte min = rangeList[0].get(0).getMin().byteValue();
        byte max = rangeList[0].get(0).getMax().byteValue();
        byte delta = (byte)((max - min) / n);

        List<Range> ranges = new ArrayList<Range>();
        byte b = min;
        for (int i = 0; i < n; i++) {
            byte c = (byte) (b + delta);
            ranges.add(RangeFactory.create(b, true, c, i == n-1));

            b = c;
        }

        // calculate stats on the entire image 
        RenderedImage destination = ZonalStatsDescriptor.create(sourceIMG[0],
            null,
            null,
            null, 
            noDataByte,
            null,
            false,
            bands,
            new StatsType[]{StatsType.MEAN},
            ranges,
            true,
            null);

        List<ZoneGeometry> result = (List<ZoneGeometry>) destination.getProperty(ZonalStatsDescriptor.ZS_PROPERTY);
        Map<Range, Statistics[]> stats = result.get(0).getStatsPerBandPerClass(0, 0);
        assertEquals(4, stats.size());
    }

    public void testZonalStats(RenderedImage source, boolean classifierUsed,
            boolean noDataRangeUsed, boolean roiUsed, boolean useROIAccessor, List<Range> rangeList) {

        // The classifier is used, if selected by the related boolean.
        RenderedImage classifierIMG;

        if (classifierUsed) {
            classifierIMG = classifier;
        } else {
            classifierIMG = null;
        }

        ROI roi = null;

        if (roiUsed) {
            roi = roiObject;
        }

        // The precalculated NoData Range is used, if selected by the related boolean.
        Range noDataRange;

        if (noDataRangeUsed) {
            int dataType = source.getSampleModel().getDataType();
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                noDataRange = noDataByte;
                break;
            case DataBuffer.TYPE_USHORT:
                noDataRange = noDataUShort;
                break;
            case DataBuffer.TYPE_SHORT:
                noDataRange = noDataShort;
                break;
            case DataBuffer.TYPE_INT:
                noDataRange = noDataInt;
                break;
            case DataBuffer.TYPE_FLOAT:
                noDataRange = noDataFloat;
                break;
            case DataBuffer.TYPE_DOUBLE:
                noDataRange = noDataDouble;
                break;
            default:
                throw new IllegalArgumentException("Wrong data type");
            }
        } else {
            noDataRange = null;
        }

        // Index used for indicating which index of the "calculation" array must be taken for reading the precalculated statistic values
        int statsIndex = 0;

        if (noDataRangeUsed && roiUsed) {
            statsIndex = 3;
        } else if (noDataRangeUsed) {
            statsIndex = 1;
        } else if (roiUsed) {
            statsIndex = 2;
        }

        // Creation of the Image
        RenderedImage destination = ZonalStatsDescriptor.create(source, classifierIMG, null,
                roiList, noDataRange, roi, useROIAccessor, bands, stats, minBound, maxBound,
                numBins, rangeList, false, null);
        // Statistic calculation
        List<ZoneGeometry> result = (List<ZoneGeometry>) destination
                .getProperty(ZonalStatsDescriptor.ZS_PROPERTY);

        // Calculated Results

        int dataType = destination.getSampleModel().getDataType();

        List<ZoneGeometry> zoneList = zonesLists[dataType][statsIndex];

        // Test if the calculated values are equal with a tolerance value
        if (CLASSIFIER) {
            // Cycle on all the geometries
            for (int i = 0; i < result.size(); i++) {
                ZoneGeometry zoneResult = result.get(i);
                ZoneGeometry zoneCalculated = zoneList.get(i);
                // Selection of the zone statistics for the selected band
                Map<Integer, Map<Range, Statistics[]>> resultPerClass = (Map<Integer, Map<Range, Statistics[]>>) zoneResult
                        .getStatsPerBand(0);
                // Set of all the keys indicating the various classifier zones
                Set<Integer> zoneset = resultPerClass.keySet();
                // Cycle on all the zones
                for (int zone : zoneset) {
                    // Result from ZonalStats operation
                    Statistics[] statsResult = (Statistics[]) zoneResult.getStatsPerBandNoRange(0,
                            zone);
                    // Result from calculation
                    Statistics[] statsCalculated = (Statistics[]) zoneCalculated
                            .getStatsPerBandNoRange(0, zone);
                    // Check if the results have the same dimensions
                    assertEquals(statsResult.length, statsCalculated.length);
                    // Check if all the calculations are equal, with a tolerance value
                    for (int j = 0; j < statsResult.length; j++) {
                        Statistics statR = statsResult[j];
                        Statistics statC = statsCalculated[j];
                        switch (j) {
                        case 0:
                        case 1:
                        case 2:
                        case 3:
                        case 5:
                        case 6:
                        case 8:
                        case 9:
                            double valueR = (Double) statR.getResult();
                            double valueC = (Double) statC.getResult();
                            assertEquals(valueR, valueC, TOLERANCE);
                            break;
                        case 4:
                            double[] extremaR = (double[]) statR.getResult();
                            double maxR = extremaR[1];
                            double minR = extremaR[0];
                            double[] extremaC = (double[]) statC.getResult();
                            double maxC = extremaC[1];
                            double minC = extremaC[0];
                            assertEquals(minR, minC, TOLERANCE);
                            assertEquals(maxR, maxC, TOLERANCE);
                            break;
                        case 7:
                            double[] histR = (double[]) statR.getResult();
                            double[] histC = (double[]) statC.getResult();

                            assertEquals(histR.length, histC.length);

                            for (int bin = 0; bin < histR.length; bin++) {
                                double binR = histR[bin];
                                double binC = histC[bin];
                                assertEquals(binR, binC, TOLERANCE);
                            }

                            break;
                        }
                    }
                }
            }
        } else {
            // Cycle on all the geometries
            for (int i = 0; i < result.size(); i++) {
                ZoneGeometry zoneResult = result.get(i);
                ZoneGeometry zoneCalculated = zoneList.get(i);
                // Selection of the statistics for the selected band
                // Result from ZonalStats operation
                Statistics[] statsResult = (Statistics[]) zoneResult
                        .getStatsPerBandNoClassifierNoRange(0);
                // Result from calculation
                Statistics[] statsCalculated = (Statistics[]) zoneCalculated
                        .getStatsPerBandNoClassifier(0, rangeList.get(0));
                // Check if the results have the same dimensions
                assertEquals(statsResult.length, statsCalculated.length);
                // Check if all the calculations are equal, with a tolerance value
                for (int j = 0; j < statsResult.length; j++) {
                    Statistics statR = statsResult[j];
                    Statistics statC = statsCalculated[j];
                    switch (j) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 5:
                    case 6:
                    case 8:
                    case 9:
                        double valueR = (Double) statR.getResult();
                        double valueC = (Double) statC.getResult();
                        assertEquals(valueR, valueC, TOLERANCE);
                        break;
                    case 4:
                        double[] extremaR = (double[]) statR.getResult();
                        double maxR = extremaR[1];
                        double minR = extremaR[0];
                        double[] extremaC = (double[]) statC.getResult();
                        double maxC = extremaC[1];
                        double minC = extremaC[0];
                        assertEquals(minR, minC, TOLERANCE);
                        assertEquals(maxR, maxC, TOLERANCE);
                        break;
                    case 7:
                        double[] histR = (double[]) statR.getResult();
                        double[] histC = (double[]) statC.getResult();

                        assertEquals(histR.length, histC.length);

                        for (int bin = 0; bin < histR.length; bin++) {
                            double binR = histR[bin];
                            double binC = histC[bin];
                            assertEquals(binR, binC, TOLERANCE);
                        }
                    }
                }
            }
        }
    }
}
