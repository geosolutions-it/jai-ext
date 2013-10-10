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
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.quadtree.Quadtree;

public class ZonalStatsTest extends TestBase {

    /** Tolerance value used for comparison between double */
    private final static double TOLERANCE = 0.1d;
    /** Boolean indicating if a classifier image must be used */
    private static final boolean CLASSIFIER = Boolean.parseBoolean("JAI.Ext.Classifies");

    private static StatsType[] stats;

    private static RenderedImage[] sourceIMG;

    private static ROIShape roi;

    private static List<ROI> roiList;

    private static RenderedImage classifier;

    private static Quadtree spatial;

    private static ArrayList<ZoneGeometry>[] zoneList;

    private static Rectangle union;

    private static int[] bands;

    private static byte noDataB;

    private static short noDataU;

    private static short noDataS;

    private static int noDataI;

    private static float noDataF;

    private static double noDataD;

    private static Range noDataByte;

    private static Range noDataUShort;

    private static Range noDataShort;

    private static Range noDataInt;

    private static Range noDataFloat;

    private static Range noDataDouble;

    private static double[] minBound;

    private static double[] maxBound;

    private static int[] numBins;

    @BeforeClass
    public static void initialSetup() {

        int initial_value = 10;
        int final_value = 30;

        int dimension = (final_value - initial_value);

        int interval = dimension / 10;

        
        // Definition of the Histogram, Mode and Median parameters
        minBound = new double[] { -3, -3, -3 };
        maxBound = new double[] { 3, 3, 3 };
        numBins = new int[] { 4, 4, 4 };
        
        // Statistics types
        stats = new StatsType[] { StatsType.MEAN, StatsType.SUM, StatsType.MAX, StatsType.MIN,
                StatsType.EXTREMA, StatsType.VARIANCE, StatsType.DEV_STD, StatsType.HISTOGRAM,
                StatsType.MODE, StatsType.MEDIAN};

        roiList = new ArrayList<ROI>();

        // ROI LIST
        for (int i = 0; i < 10; i++) {
            ROI roiGeom = new ROIShape(new Rectangle(initial_value, final_value, dimension,
                    dimension));
            roiList.add(roiGeom);
            initial_value += interval;
            final_value += interval;
        }

        // Band array creation
        bands = new int[] { 0 };

        // Spatial indexing
        // Creation of the spatial index
        spatial = new Quadtree();
        // Creation of a ZoneGeometry list, for storing the results
        zoneList = new ArrayList[4];
        for (int z = 0; z < 4; z++) {
            zoneList[z] = new ArrayList<ZoneGeometry>();
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
            spatial.insert(env, roi);
            // Union
            union = union.union(rect);
            // Addition to the geometries list
            for (int z = 0; z < 4; z++) {
                // Creation of a new ZoneGeometry
                ZoneGeometry geom = new ZoneGeometry(bands, stats, CLASSIFIER,minBound,maxBound,numBins);
                zoneList[z].add(geom);
            }
        }

        // Classifier
        classifier = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH, DEFAULT_HEIGHT, 2, false);

        Rectangle rectClass = new Rectangle(classifier.getMinX(), classifier.getMinY(),
                classifier.getWidth(), classifier.getHeight());

        RandomIter iterator = RandomIterFactory.create(classifier, rectClass, false, true);

        // ROI creation
        Rectangle roiBounds = new Rectangle(5, 5, DEFAULT_WIDTH / 4, DEFAULT_HEIGHT / 4);
        roi = new ROIShape(roiBounds);

        // No Data values
        noDataB = 50;
        noDataU = 50;
        noDataS = 50;
        noDataI = 50;
        noDataF = 50;
        noDataD = 50;

        boolean minIncluded = true;
        boolean maxIncluded = true;

        noDataByte = RangeFactory.create(noDataB, minIncluded, noDataB, maxIncluded);
        noDataUShort = RangeFactory.createU(noDataU, minIncluded, noDataU, maxIncluded);
        noDataShort = RangeFactory.create(noDataS, minIncluded, noDataS, maxIncluded);
        noDataInt = RangeFactory.create(noDataI, minIncluded, noDataI, maxIncluded);
        noDataFloat = RangeFactory.create(noDataF, minIncluded, noDataF, maxIncluded, true);
        noDataDouble = RangeFactory.create(noDataD, minIncluded, noDataD, maxIncluded, true);

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

                int minX = arrayRas.getMinX();
                int minY = arrayRas.getMinY();
                int maxX = minX + arrayRas.getWidth();
                int maxY = minY + arrayRas.getHeight();

                // Cycle on the Raster pixels
                for (int x = minX; x < maxX; x++) {
                    for (int y = minY; y < maxY; y++) {

                        if (union.contains(x, y)) {
                            byte value = (byte) arrayRas.getSample(x, y, 0);

                            Coordinate p = new Coordinate(x, y);

                            Envelope searchEnv = new Envelope(p);

                            // Query on the geometry list
                            List<ROI> geomList = spatial.query(searchEnv);
                            // Zone classifier initial value
                            int zone = 0;
                            // If the classifier is present then the zone value is taken
                            if (CLASSIFIER) {
                                // Selection of the zone point
                                zone = iterator.getSample(x, y, 0);
                            }
                            // Cycle on all the geometries found
                            for (ROI geometry : geomList) {
                                // if every geometry really contains the selected point
                                if (geometry.contains(x, y)) {

                                    // then the related zoneGeometry object statistics are updated
                                    int index = roiList.indexOf(geometry);

                                    // Cycle for all the 4 cases: No roi and No NoData, only roi, only NoData, both roi and NoData
                                    for (int z = 0; z < 4; z++) {
                                        ZoneGeometry zoneGeo = zoneList[z].get(index);
                                        switch (z) {
                                        case 0:
                                            zoneGeo.add(value, 0, zone, false);
                                            zoneList[z].set(index, zoneGeo);
                                            break;
                                        case 1:
                                            if (roi.contains(x, y)) {
                                                zoneGeo.add(value, 0, zone, false);
                                                zoneList[z].set(index, zoneGeo);
                                            }
                                            break;
                                        case 2:
                                            if (!noDataByte.contains(value)) {
                                                zoneGeo.add(value, 0, zone, false);
                                                zoneList[z].set(index, zoneGeo);
                                            }
                                            break;
                                        case 3:
                                            if (!noDataByte.contains(value) && roi.contains(x, y)) {
                                                zoneGeo.add(value, 0, zone, false);
                                                zoneList[z].set(index, zoneGeo);
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

    @Test
    public void testNoROINoNoData() {

        boolean roiUsed = false;
        boolean noDataRangeUsed = false;
        boolean useRoiAccessor = false;

        testZonalStats(sourceIMG[0], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[1], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[2], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[3], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[4], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[5], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);

    }

    @Test
    public void testROINoNoData() {

        boolean roiUsed = true;
        boolean noDataRangeUsed = false;
        boolean useRoiAccessor = false;

        testZonalStats(sourceIMG[0], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[1], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[2], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[3], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[4], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[5], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);

    }

    @Test
    public void testROIAccessorNoNoData() {

        boolean roiUsed = true;
        boolean noDataRangeUsed = false;
        boolean useRoiAccessor = true;

        testZonalStats(sourceIMG[0], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[1], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[2], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[3], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[4], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[5], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);

    }

    @Test
    public void testNoDataNoROI() {

        boolean roiUsed = false;
        boolean noDataRangeUsed = true;
        boolean useRoiAccessor = false;

        testZonalStats(sourceIMG[0], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[1], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[2], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[3], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[4], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[5], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);

    }

    @Test
    public void testROIAccessorNoData() {

        boolean roiUsed = true;
        boolean noDataRangeUsed = true;
        boolean useRoiAccessor = true;

        testZonalStats(sourceIMG[0], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[1], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[2], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[3], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[4], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[5], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);

    }

    @Test
    public void testROINoData() {

        boolean roiUsed = true;
        boolean noDataRangeUsed = true;
        boolean useRoiAccessor = false;

        testZonalStats(sourceIMG[0], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[1], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[2], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[3], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[4], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);
        testZonalStats(sourceIMG[5], CLASSIFIER, roiUsed, noDataRangeUsed, useRoiAccessor);

    }

    public void testZonalStats(RenderedImage source, boolean classifierUsed, boolean roiUsed,
            boolean noDataRangeUsed, boolean useRoiAccessor) {

        // The precalculated roi is used, if selected by the related boolean.
        ROI roiData;

        if (roiUsed) {
            roiData = roi;
        } else {
            roiData = null;
        }

        // The classifier is used, if selected by the related boolean.
        RenderedImage classifierIMG;

        if (classifierUsed) {
            classifierIMG = classifier;
        } else {
            classifierIMG = null;
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

        if (roiUsed && noDataRangeUsed) {
            statsIndex = 3;
        } else if (roiUsed) {
            statsIndex = 1;
        } else if (noDataRangeUsed) {
            statsIndex = 2;
        }

        // Simple statistics
        RenderedImage destination = ZonalStatsDescriptor.create(source, classifierIMG, null,
                roiList, roiData, noDataRange, useRoiAccessor, bands, stats, minBound, maxBound, numBins, null);
        // Statistic calculation
        List<ZoneGeometry> result = (List<ZoneGeometry>) destination
                .getProperty(ZonalStatsDescriptor.ZS_PROPERTY);

        // Test if the calculated values are equal with a tolerance value
        if (CLASSIFIER) {
            for (int i = 0; i < result.size(); i++) {
                ZoneGeometry zoneResult = result.get(i);
                ZoneGeometry zoneCalculated = zoneList[statsIndex].get(i);

                Map<Integer, Statistics[]> resultPerZone = (Map<Integer, Statistics[]>) zoneResult
                        .getStatsPerBand(0);

                Set<Integer> zoneset = resultPerZone.keySet();

                for (int zone : zoneset) {
                    Statistics[] statsResult = (Statistics[]) zoneResult.getStatsPerBandPerZone(0,
                            zone);
                    Statistics[] statsCalculated = (Statistics[]) zoneCalculated
                            .getStatsPerBandPerZone(0, zone);

                    assertEquals(statsResult.length, statsCalculated.length);

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
                            
                            for(int bin = 0; bin < histR.length; bin++){
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
            for (int i = 0; i < result.size(); i++) {
                ZoneGeometry zoneResult = result.get(i);
                ZoneGeometry zoneCalculated = zoneList[statsIndex].get(i);

                Statistics[] statsResult = (Statistics[]) zoneResult.getStatsPerBandPerZone(0, 0);
                Statistics[] statsCalculated = (Statistics[]) zoneCalculated
                        .getStatsPerBandPerZone(0, 0);

                assertEquals(statsResult.length, statsCalculated.length);

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
                    }
                }
            }
        }
    }

    @Override
    protected void testGlobal(boolean useROIAccessor, boolean isBinary, boolean bicubic2Disabled,
            boolean noDataRangeUsed, boolean roiPresent, InterpolationType interpType,
            TestSelection testSelect, ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported in this test class");
    }

    @Override
    protected <T extends Number & Comparable<? super T>> void testImage(int dataType,
            T noDataValue, boolean useROIAccessor, boolean isBinary, boolean bicubic2Disabled,
            boolean noDataRangeUsed, boolean roiPresent, InterpolationType interpType,
            TestSelection testSelect, ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported in this test class");
    }

    @Override
    protected <T extends Number & Comparable<? super T>> void testImageAffine(
            RenderedImage sourceImage, int dataType, T noDataValue, boolean useROIAccessor,
            boolean isBinary, boolean bicubic2Disabled, boolean noDataRangeUsed,
            boolean roiPresent, boolean setDestinationNoData, TransformationType transformType,
            InterpolationType interpType, TestSelection testSelect, ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported in this test class");
    }

    @Override
    protected void testGlobalAffine(boolean useROIAccessor, boolean isBinary,
            boolean bicubic2Disabled, boolean noDataRangeUsed, boolean roiPresent,
            boolean setDestinationNoData, InterpolationType interpType, TestSelection testSelect,
            ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported in this test class");
    }
}
