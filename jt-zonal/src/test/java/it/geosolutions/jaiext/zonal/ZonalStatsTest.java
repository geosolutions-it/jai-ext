package it.geosolutions.jaiext.zonal;

import static org.junit.Assert.assertEquals;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Collections;
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
import com.vividsolutions.jts.index.strtree.STRtree;

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
 * different images each one with a different data type (except Long data type). The operation is tested with and without the presence of NoData. The
 * classifier can be used by setting to true the JVM parameter JAI.Ext.Classifier. All the results are compared with the previously calculated
 * statistics for checking the correctness of the calculations.
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

        rangeList = new ArrayList[6];
        
        rangeList[0] = new ArrayList<Range>(1);
        rangeList[1] = new ArrayList<Range>(1);
        rangeList[2] = new ArrayList<Range>(1);
        rangeList[3] = new ArrayList<Range>(1);
        rangeList[4] = new ArrayList<Range>(1);
        rangeList[5] = new ArrayList<Range>(1);
        
        //Data Range creation
        rangeList[0].add(RangeFactory.create((byte)-1, true, (byte)100, true)); 
        rangeList[1].add(RangeFactory.createU((short)0.0, true, (short)100, true)); 
        rangeList[2].add(RangeFactory.create((short)-1, true,(short)100, true)); 
        rangeList[3].add(RangeFactory.create(-1, true, 100, true)); 
        rangeList[4].add(RangeFactory.create(-1f, true, 100f, true,false)); 
        rangeList[5].add(RangeFactory.create(-1d, true, 100d, true,false)); 
        
        // Band array creation
        bands = new int[] { 0 };

        // Spatial indexing
        // Creation of the spatial indexes
        spatial = new STRtree[6][2]; 
        
        for(int i = 0; i< 6; i++){
            spatial[i][0] = new STRtree();
            spatial[i][1] = new STRtree();
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
            for(int i = 0; i< 6; i++){
                for (int z = 0; z < 2; z++) {
                    // Creation of a new ZoneGeometry
                    ZoneGeometry geom = new ZoneGeometry(roi, rangeList[i], bands, stats, CLASSIFIER, minBound, maxBound,
                            numBins);
                    spatial[i][z].insert(env, geom);
                }
            }
        }

        // Classifier
        classifier = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH, DEFAULT_HEIGHT, 2, false);
        // Classifier bound
        Rectangle rectClass = new Rectangle(classifier.getMinX(), classifier.getMinY(),
                classifier.getWidth(), classifier.getHeight());
        // Iterator on the classifier image
        RandomIter iterator = RandomIterFactory.create(classifier, rectClass, false, true);

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
                        
                        for(int z = 0; z < 2; z++){
                            // Check if the selected pixel is inside the union of all the geometries
                            if (union.contains(x, y)) {
                                byte value = (byte) arrayRas.getSample(x, y, 0);
                                // pixel coordinate creation for spatial indexing
                                Coordinate p = new Coordinate(x, y);
                                // Creation of an Envelop containing the pixel coordinates
                                Envelope searchEnv = new Envelope(p);
                                // Query on the geometry list
                                for(int v =0; v < 6; v++){
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
                                                        zoneGeo.add(value, 0, classId, rangeList[v].get(0));
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
    public void testNoNoData() {
        // This test calculates zonal statistics without NoData
        boolean noDataRangeUsed = false;

        testZonalStats(sourceIMG[0], CLASSIFIER, noDataRangeUsed, rangeList[0]);
        testZonalStats(sourceIMG[1], CLASSIFIER, noDataRangeUsed, rangeList[1]);
        testZonalStats(sourceIMG[2], CLASSIFIER, noDataRangeUsed, rangeList[2]);
        testZonalStats(sourceIMG[3], CLASSIFIER, noDataRangeUsed, rangeList[3]);
        testZonalStats(sourceIMG[4], CLASSIFIER, noDataRangeUsed, rangeList[4]);
        testZonalStats(sourceIMG[5], CLASSIFIER, noDataRangeUsed, rangeList[5]);

    }

    @Test
    public void testNoData() {
        // This test calculates zonal statistics with NoData and without ROI
        boolean noDataRangeUsed = true;

        testZonalStats(sourceIMG[0], CLASSIFIER, noDataRangeUsed, rangeList[0]);
        testZonalStats(sourceIMG[1], CLASSIFIER, noDataRangeUsed, rangeList[1]);
        testZonalStats(sourceIMG[2], CLASSIFIER, noDataRangeUsed, rangeList[2]);
        testZonalStats(sourceIMG[3], CLASSIFIER, noDataRangeUsed, rangeList[3]);
        testZonalStats(sourceIMG[4], CLASSIFIER, noDataRangeUsed, rangeList[4]);
        testZonalStats(sourceIMG[5], CLASSIFIER, noDataRangeUsed, rangeList[5]);

    }

    public void testZonalStats(RenderedImage source, boolean classifierUsed,
            boolean noDataRangeUsed , List<Range> rangeList) {
        
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

        if (noDataRangeUsed) {
            statsIndex = 1;
        }

        // Creation of the Image
        RenderedImage destination = ZonalStatsDescriptor.create(source, classifierIMG, null,
                roiList, noDataRange, bands, stats, minBound, maxBound,
                numBins, rangeList, false, null);
        // Statistic calculation
        List<ZoneGeometry> result = (List<ZoneGeometry>) destination
                .getProperty(ZonalStatsDescriptor.ZS_PROPERTY);
        
        //Calculated Results
        List<ZoneGeometry>[] zoneList = new ArrayList[2];
        
        int dataType = destination.getSampleModel().getDataType();
        
        zoneList[statsIndex] = spatial[dataType][statsIndex].itemsTree();
        Collections.reverse(zoneList[statsIndex]);

        // Test if the calculated values are equal with a tolerance value
        if (CLASSIFIER) {
            // Cycle on all the geometries
            for (int i = 0; i < result.size(); i++) {
                ZoneGeometry zoneResult = result.get(i);
                ZoneGeometry zoneCalculated = zoneList[statsIndex].get(i);
                // Selection of the zone statistics for the selected band
                Map<Integer, Map<Range,Statistics[]>> resultPerClass = (Map<Integer, Map<Range,Statistics[]>>) zoneResult
                        .getStatsPerBand(0);
                // Set of all the keys indicating the various classifier zones
                Set<Integer> zoneset = resultPerClass.keySet();
                // Cycle on all the zones
                for (int zone : zoneset) {
                    // Result from ZonalStats operation
                    Statistics[] statsResult = (Statistics[]) zoneResult.getStatsPerBandNoRange(0, zone); 
                    // Result from calculation
                    Statistics[] statsCalculated = (Statistics[]) zoneResult.getStatsPerBandNoRange(0, zone);
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
                ZoneGeometry zoneCalculated = zoneList[statsIndex].get(i);
                // Selection of the statistics for the selected band
                // Result from ZonalStats operation
                Statistics[] statsResult = (Statistics[]) zoneResult.getStatsPerBandNoClassifierNoRange(0);
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
