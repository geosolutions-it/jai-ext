package it.geosolutions.jaiext.zonal;

import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.stats.Statistics.StatsType;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.iterator.RandomIter;
import com.sun.media.jai.util.PropertyUtil;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.STRtree;

/**
 * This class extends the {@link OpImage} class and executes the "ZonalStats" operation. This operation consists of calculating the image statistics
 * on different locations, defined by their geometries, on the same image. In addition this operation supports the presence of ROI or No Data. The
 * calculations are performed only in a rectangle that contains the union of all the input geometries. For every input geometry, a
 * {@link ZoneGeometry} object is associated to it for storing its statistics. A spatial index is used for fast accessing the geometries that
 * intersects the selected image pixel (in the case of overlapping). The final results can be returned by calling the getProperty() method with the
 * ZonalStatsDescriptor.ZS_PROPERTY. This method returns a list containing all the ZoneGeometries objects associated with each input geometry object.
 * The statistic results can be returned for each band or for each Class(if the classifier is present). It is important to remember that the
 * classifier must be of integral data type.
 */
public class ZonalStatsOpImage extends OpImage {

    /** ROI extender */
    protected final static BorderExtender ROI_EXTENDER = BorderExtender
            .createInstance(BorderExtender.BORDER_ZERO);

    /** Logger object */
    private static final Logger LOGGER = Logger.getLogger(ZonalStatsOpImage.class.getName());

    /** Volatile variable indicating if the statistical computations has already been done or not */
    private volatile boolean firstTime;

    /** Spatial index for fast accessing the geometries that contain the selected pixel */
    private final STRtree spatial;

    /** Boolean indicating if the classifier is present */
    private final boolean classPresent;

    /** Affine transformation for mapping the source image pixels to the classifiers pixels */
    private AffineTransform inverseTrans;

    /** Boolean indicating if a NoData Range is used */
    private final boolean hasNoData;

    /** Boolean indicating if an optional ROI is used */
    private final boolean hasROI;

    /** Boolean indicating the case when no ROI nor NoData are present */
    private final boolean caseA;

    /** Boolean indicating the case when only ROI is present */
    private final boolean caseB;

    /** Boolean indicating the case when only NoData are present */
    private final boolean caseC;

    /** Range object used for checking if a pixel is or not a NoData */
    private final Range noData;

    /** Boolean indicatig if ROI RasterAccessor should be used during computations */
    private final boolean useROIAccessor;

    /** ROI image */
    private final PlanarImage srcROIImage;

    /** Random Iterator used iterating on the ROI data */
    private final RandomIter roiIter;

    /** Rectangle containing ROI bounds */
    private final Rectangle roiBounds;

    /** Boolean lookuptable used if no data are present */
    private final boolean[] booleanLookupTable;

    /** Rectangle containing the union of all the geometries */
    private Rectangle union;

    /** List of all the ZoneGeometry objects associated to every initial geometry */
    private final ArrayList<ZoneGeometry> zoneList;

    /** Source image bounds */
    private Rectangle sourceBounds;

    /** Classifier image bounds */
    private Rectangle classBounds;

    /** Iterator on the classifier image */
    private RandomIter iterator;

    /** Array indicating the source image selected bands */
    private int[] bands;

    /** Band array length */
    private int bandNum;

    /** List of all the input geometries */
    private List<ROI> rois;

    public ZonalStatsOpImage(RenderedImage source, ImageLayout layout, Map configuration,
            RenderedImage classifier, AffineTransform transform, List<ROI> rois, ROI roiUsed,
            Range noData, boolean useROIAccessor, int[] bands, StatsType[] statsTypes,
            double[] minBound, double[] maxBound, int[] numBins) {
        super(vectorize(source), layout, configuration, true);

        // Check if the classifier is present
        classPresent = classifier != null && classifier instanceof RenderedImage;
        // Check if the classifier is integral
        if (classPresent) {
            int classDataType = classifier.getSampleModel().getDataType();
            if (!(classDataType == DataBuffer.TYPE_BYTE || classDataType == DataBuffer.TYPE_USHORT
                    || classDataType == DataBuffer.TYPE_SHORT || classDataType == DataBuffer.TYPE_INT)) {
                throw new IllegalArgumentException("Classifier must be integral");
            }
        }

        // Calculation of the inverse transformation
        classBounds = null;
        if (classPresent) {
            // source image bounds
            sourceBounds = new Rectangle(source.getMinX(), source.getMinY(), source.getWidth(),
                    source.getHeight());
            if (transform == null) {
                // If no transformation is set, the classifier bounds are the same of the image bounds
                inverseTrans = new AffineTransform();
                classBounds = sourceBounds;
            } else {
                try {
                    // If the transformation is set, then the source image bounds are mapped to the zone image bounds
                    inverseTrans = transform.createInverse();
                    classBounds = inverseTrans.createTransformedShape(sourceBounds).getBounds();
                } catch (NoninvertibleTransformException ex) {
                    LOGGER.warning("The transformation matrix is non-invertible.");
                }
            }
            // Iterator on the classifier image bounds
            iterator = RandomIterFactory.create(classifier, classBounds, false, true);

        }

        // Band selection
        this.bands = bands;
        this.bandNum = bands.length;
        // Control on the bands number
        SampleModel sm = source.getSampleModel();
        int numBands = sm.getNumBands();
        if (bandNum > numBands) {
            throw new IllegalArgumentException(
                    "The selected bands number cannot be greater than that of "
                            + "image band number");
        }
        // If the band number is less or equal to 0, a new array containing only the first index is used.
        if (bandNum <= 0) {
            this.bands = new int[] { 0 };
            this.bandNum = 1;
        } else {
            // If a band index is greater than the maximum band index, an exception is thrown
            for (int i = 0; i < bandNum; i++) {
                if (bands[i] > numBands) {
                    throw new IllegalArgumentException(
                            "Band index cannot be greater than the image band number");
                }
            }
        }
        // Complex Statistic types validation
        boolean minBoundsNull = minBound == null;
        boolean maxBoundsNull = maxBound == null;
        boolean numBinsNull = numBins == null;
        // Boolean indicating if one of the input arrays is null
        boolean nullCondition = minBoundsNull || maxBoundsNull || numBinsNull;
        // Check if the bounds or the bins are not null
        for (int st = 0; st < statsTypes.length; st++) {
            int statId = statsTypes[st].getStatsId();
            if (statId > 6 && nullCondition) {
                throw new IllegalArgumentException(
                        "If complex statistics are used, Bounds and Bin number should be defined");
            }
        }
        // Check if the bounds or the bins have the same length
        int minLen = minBound.length;
        int maxLen = maxBound.length;
        int numLen = numBins.length;

        if ((minLen + maxLen + numLen) != (minLen * 3)) {
            throw new IllegalArgumentException("Bounds and Bin length must be equals");
        }
        // If the bounds and bins have a minor dimension than that of the bands array
        // the bounds and the bin related to the index 0 are repeated on all the bound
        // and bin array.
        double[] minBounds;
        double[] maxBounds;
        int[] numBinss;

        if (bandNum > minLen) {
            double[] minBoundsTmp = new double[bands.length];
            double[] maxBoundsTmp = new double[bands.length];
            int[] numbinsTmp = new int[bands.length];
            for (int i = 0; i < bands.length; i++) {
                minBoundsTmp[i] = minBound[0];
                maxBoundsTmp[i] = maxBound[0];
                numbinsTmp[i] = numBins[0];
            }

            minBounds = minBoundsTmp;
            maxBounds = maxBoundsTmp;
            numBinss = numbinsTmp;

        } else {
            minBounds = minBound;
            maxBounds = maxBound;
            numBinss = numBins;
        }

        // Creation of the spatial index
        spatial = new STRtree();
        // Creation of a ZoneGeometry list, for storing the results
        zoneList = new ArrayList<ZoneGeometry>();
        // Check if the rois are present. Otherwise the entire image statistics
        // are calculated
        if (rois == null) {
            this.rois = new ArrayList<ROI>();
            ROI roi = new ROIShape(sourceBounds);
            this.rois.add(roi);
            // Bounds Union
            union = new Rectangle(sourceBounds);
            // Spatial index creation
            Rectangle rect = roi.getBounds();
            double minX = rect.getMinX();
            double maxX = rect.getMaxX();
            double minY = rect.getMinY();
            double maxY = rect.getMaxY();
            Envelope env = new Envelope(minX, maxX, minY, maxY);
            spatial.insert(env, roi);

            // Creation of a new ZoneGeometry
            ZoneGeometry geom = new ZoneGeometry(bands, statsTypes, classPresent, minBounds,
                    maxBounds, numBinss);
            // Addition to the geometries list
            zoneList.add(geom);

        } else {
            // Bounds Union
            union = new Rectangle(rois.get(0).getBounds());
            // Insertion of the zones to the spatial index and union of the bounds for every ROI/Zone object
            for (ROI roi : rois) {
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
                // Creation of a new ZoneGeometry
                ZoneGeometry geom = new ZoneGeometry(bands, statsTypes, classPresent, minBounds,
                        maxBounds, numBinss);
                // Addition to the geometries list
                zoneList.add(geom);
            }
            // Sets of the roi list
            this.rois = rois;
        }
        // Check if No Data control must be done
        if (noData != null) {
            hasNoData = true;
            this.noData = noData;
        } else {
            hasNoData = false;
            this.noData = null;
        }

        // Check if ROI control must be done
        if (roiUsed != null) {
            hasROI = true;
            // Roi object
            ROI srcROI = roiUsed;
            // Creation of a PlanarImage containing the ROI data
            srcROIImage = srcROI.getAsImage();
            // ROI image bounds calculation
            final Rectangle rect = new Rectangle(srcROIImage.getBounds());
            // Roi image data store
            Raster data = srcROIImage.getData(rect);
            // Creation of a RandomIterator for selecting random pixel inside the ROI
            roiIter = RandomIterFactory.create(data, data.getBounds(), false, true);
            // ROI bounds are saved
            roiBounds = srcROIImage.getBounds();
            // The useRoiAccessor parameter is set
            this.useROIAccessor = useROIAccessor;
        } else {
            // The ROI parameters are all cleared
            hasROI = false;
            this.useROIAccessor = false;
            roiBounds = null;
            roiIter = null;
            srcROIImage = null;
        }

        // Creation of a boolean lookuptable indicating if the selected pixel is a NoData
        // used only for byte images.
        if (hasNoData && source.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE) {
            booleanLookupTable = new boolean[255];
            for (int i = 0; i < booleanLookupTable.length; i++) {
                byte value = (byte) i;
                booleanLookupTable[i] = !noData.contains(value);
            }
        } else {
            booleanLookupTable = null;
        }

        // Definition of the possible cases that can be found
        // caseA = no ROI nor No Data
        // caseB = ROI present but No Data not present
        // caseC = No Data present but ROI not present
        // Last case not defined = both ROI and No Data are present
        caseA = !hasNoData && !hasROI;
        caseB = !hasNoData && hasROI;
        caseC = hasNoData && !hasROI;
        // Setting the computation flag to true
        firstTime = true;
    }

    public Raster computeTile(int tileX, int tileY) {
        // Selection of the tile associated with the tile x and y indexes
        Raster tile = getSourceImage(0).getTile(tileX, tileY);
        // Selection of the tile bounds
        Rectangle tileRect = tile.getBounds();
        // Check if the tile is inside the geometry bound-union
        if (union.intersects(tileRect)) {
            // STATISTICAL ELABORATIONS
            // selection of the format tags
            RasterFormatTag[] formatTags = getFormatTags();
            // Selection of the active calculation area
            Rectangle computableArea = union.intersection(tileRect);

            // creation of the RasterAccessor
            RasterAccessor src = new RasterAccessor(tile, computableArea, formatTags[0],
                    getSourceImage(0).getColorModel());

            // ROI calculations if roiAccessor is used
            RasterAccessor roi = null;
            if (useROIAccessor) {
                Raster roiRaster = srcROIImage.getExtendedData(computableArea, ROI_EXTENDER);

                // creation of the rasterAccessor
                roi = new RasterAccessor(roiRaster, computableArea,
                        RasterAccessor.findCompatibleTags(new RenderedImage[] { srcROIImage },
                                srcROIImage)[0], srcROIImage.getColorModel());
            }
            // Image dataType
            int dataType = tile.getSampleModel().getDataType();
            // From the data type is possible to choose the right calculation method
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                byteLoop(src, computableArea, roi);
                break;
            case DataBuffer.TYPE_USHORT:
                ushortLoop(src, computableArea, roi);
                break;
            case DataBuffer.TYPE_SHORT:
                shortLoop(src, computableArea, roi);
                break;
            case DataBuffer.TYPE_INT:
                intLoop(src, computableArea, roi);
                break;
            case DataBuffer.TYPE_FLOAT:
                floatLoop(src, computableArea, roi);
                break;
            case DataBuffer.TYPE_DOUBLE:
                doubleLoop(src, computableArea, roi);
                break;
            default:
                throw new IllegalArgumentException("Wrong data type");
            }
        }

        return tile;
    }

    // NOTE: the statistic calculation is done in a synchronized block for avoiding race conditions
    private void byteLoop(RasterAccessor src, Rectangle computableArea, RasterAccessor roi) {

        // Source and ROI RasterAccessor initial parameters
        final int srcX = src.getX();
        final int srcY = src.getY();

        final byte[] roiDataArray;
        final int roiScanLineStride;
        final int roiDataLength;

        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiScanLineStride = roi.getScanlineStride();
            roiDataLength = roiDataArray.length;
        } else {
            roiDataArray = null;
            roiScanLineStride = 0;
            roiDataLength = 0;
        }

        byte srcData[][] = src.getByteDataArrays();

        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // NO DATA AND ROI ARE NOT PRESENT
        if (caseA) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y++) {
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x++) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;

                    // PixelPositions
                    int x0 = srcX + x;
                    int y0 = srcY + y;
                    // Coordinate object creation for the spatial indexing
                    Coordinate p1 = new Coordinate(x0, y0);
                    // Envelope associated to the coordinate object
                    Envelope searchEnv = new Envelope(p1);
                    // Query on the geometry list
                    List<ROI> geomList = spatial.query(searchEnv);
                    // classId classifier initial value
                    int classId = 0;
                    // If the classifier is present then the classId value is taken
                    if (classPresent) {
                        // Selection of the initial point
                        Point pointSrc = new Point(x0, y0);
                        // Initialization of the classId point
                        Point pointClass = new Point();
                        // Source point inverse transformation for finding the related zone point
                        try {
                            inverseTrans.inverseTransform(pointSrc, pointClass);

                        } catch (NoninvertibleTransformException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }
                        // Selection of the classId point
                        classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                    }
                    // Cycle on all the geometries found
                    for (ROI geometry : geomList) {
                        // if every geometry really contains the selected point
                        if (geometry.contains(x0, y0)) {
                            // then the related zoneGeometry object statistics are updated
                            int index = rois.indexOf(geometry);

                            synchronized (this) {
                                ZoneGeometry zoneGeo = zoneList.get(index);

                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    byte sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // Update of all the statistics
                                    zoneGeo.add(sample, bands[i], classId, false);
                                }
                                zoneList.set(index, zoneGeo);
                            }
                        }
                    }
                }
            }
            // ROI ACCESSOR USED
        } else if (caseB) {
            if (useROIAccessor) {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    int posyROI = y * roiScanLineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // ROI index position
                        int windex = x + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // Control if the sample is inside ROI
                        if (w != 0) {
                            // PixelPositions
                            int x0 = srcX + x;
                            int y0 = srcY + y;
                            // Coordinate object creation for the spatial indexing
                            Coordinate p1 = new Coordinate(x0, y0);
                            // Envelope associated to the coordinate object
                            Envelope searchEnv = new Envelope(p1);
                            // Query on the geometry list
                            List<ROI> geomList = spatial.query(searchEnv);
                            // classId classifier initial value
                            int classId = 0;
                            // If the classifier is present then the classId value is taken
                            if (classPresent) {
                                // Selection of the initial point
                                Point pointSrc = new Point(x0, y0);
                                // Initialization of the classId point
                                Point pointClass = new Point();
                                // Source point inverse transformation for finding the related zone point
                                try {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);

                                } catch (NoninvertibleTransformException e) {
                                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                }
                                // Selection of the zone point
                                classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                            }
                            // Cycle on all the geometries found
                            for (ROI geometry : geomList) {
                                // if every geometry really contains the selected point
                                if (geometry.contains(x0, y0)) {
                                    // then the related zoneGeometry object statistics are updated
                                    int index = rois.indexOf(geometry);

                                    synchronized (this) {
                                        ZoneGeometry zoneGeo = zoneList.get(index);

                                        // Cycle on the selected Bands
                                        for (int i = 0; i < bandNum; i++) {
                                            byte sample = srcData[bands[i]][posx + posy
                                                    + srcBandOffsets[bands[i]]];
                                            // Update of all the statistics
                                            zoneGeo.add(sample, bands[i], classId, false);
                                        }
                                        zoneList.set(index, zoneGeo);
                                    }
                                }
                            }
                        }
                    }
                }
                // ROI BOUNDS USED
            } else {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0);
                            // Control if the sample is inside ROI
                            if (w != 0) {
                                // Coordinate object creation for the spatial indexing
                                Coordinate p1 = new Coordinate(x0, y0);
                                // Envelope associated to the coordinate object
                                Envelope searchEnv = new Envelope(p1);
                                // Query on the geometry list
                                List<ROI> geomList = spatial.query(searchEnv);
                                // classId classifier initial value
                                int classId = 0;
                                // If the classifier is present then the zone value is taken
                                if (classPresent) {
                                    // Selection of the initial point
                                    Point pointSrc = new Point(x0, y0);
                                    // Initialization of the zone point
                                    Point pointClass = new Point();
                                    // Source point inverse transformation for finding the related zone point
                                    try {
                                        inverseTrans.inverseTransform(pointSrc, pointClass);

                                    } catch (NoninvertibleTransformException e) {
                                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                    }
                                    // Selection of the zone point
                                    classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                                }
                                // Cycle on all the geometries found
                                for (ROI geometry : geomList) {
                                    // if every geometry really contains the selected point
                                    if (geometry.contains(x0, y0)) {
                                        // then the related zoneGeometry object statistics are updated
                                        int index = rois.indexOf(geometry);

                                        synchronized (this) {
                                            ZoneGeometry zoneGeo = zoneList.get(index);

                                            // Cycle on the selected Bands
                                            for (int i = 0; i < bandNum; i++) {
                                                byte sample = srcData[bands[i]][posx + posy
                                                        + srcBandOffsets[bands[i]]];
                                                // Update of all the statistics
                                                zoneGeo.add(sample, bands[i], classId, false);
                                            }
                                            zoneList.set(index, zoneGeo);
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
            }
            // NO DATA USED (ROI NOT USED)
        } else if (caseC) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y++) {
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x++) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;
                    // PixelPositions
                    int x0 = srcX + x;
                    int y0 = srcY + y;
                    // Coordinate object creation for the spatial indexing
                    Coordinate p1 = new Coordinate(x0, y0);
                    // Envelope associated to the coordinate object
                    Envelope searchEnv = new Envelope(p1);
                    // Query on the geometry list
                    List<ROI> geomList = spatial.query(searchEnv);
                    // classId classifier initial value
                    int classId = 0;
                    // If the classifier is present then the zone value is taken
                    if (classPresent) {
                        // Selection of the initial point
                        Point pointSrc = new Point(x0, y0);
                        // Initialization of the zone point
                        Point pointClass = new Point();
                        // Source point inverse transformation for finding the related zone point
                        try {
                            inverseTrans.inverseTransform(pointSrc, pointClass);

                        } catch (NoninvertibleTransformException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }
                        // Selection of the zone point
                        classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                    }
                    // Cycle on all the geometries found
                    for (ROI geometry : geomList) {
                        // if every geometry really contains the selected point
                        if (geometry.contains(x0, y0)) {
                            // then the related zoneGeometry object statistics are updated
                            int index = rois.indexOf(geometry);

                            synchronized (this) {
                                ZoneGeometry zoneGeo = zoneList.get(index);

                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    byte sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // NoData check
                                    if (booleanLookupTable[(int) sample]) {
                                        // Update of all the statistics
                                        zoneGeo.add(sample, bands[i], classId, false);
                                    }
                                }
                                zoneList.set(index, zoneGeo);
                            }
                        }
                    }
                }
            }
        } else {
            if (useROIAccessor) {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    int posyROI = y * roiScanLineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // ROI index position
                        int windex = x + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // Control if the sample is inside ROI
                        if (w != 0) {
                            // PixelPositions
                            int x0 = srcX + x;
                            int y0 = srcY + y;
                            // Coordinate object creation for the spatial indexing
                            Coordinate p1 = new Coordinate(x0, y0);
                            // Envelope associated to the coordinate object
                            Envelope searchEnv = new Envelope(p1);
                            // Query on the geometry list
                            List<ROI> geomList = spatial.query(searchEnv);
                            // classId classifier initial value
                            int classId = 0;
                            // If the classifier is present then the zone value is taken
                            if (classPresent) {
                                // Selection of the initial point
                                Point pointSrc = new Point(x0, y0);
                                // Initialization of the zone point
                                Point pointClass = new Point();
                                // Source point inverse transformation for finding the related zone point
                                try {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);

                                } catch (NoninvertibleTransformException e) {
                                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                }
                                // Selection of the zone point
                                classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                            }
                            // Cycle on all the geometries found
                            for (ROI geometry : geomList) {
                                // if every geometry really contains the selected point
                                if (geometry.contains(x0, y0)) {
                                    // then the related zoneGeometry object statistics are updated
                                    int index = rois.indexOf(geometry);

                                    synchronized (this) {
                                        ZoneGeometry zoneGeo = zoneList.get(index);

                                        // Cycle on the selected Bands
                                        for (int i = 0; i < bandNum; i++) {
                                            byte sample = srcData[bands[i]][posx + posy
                                                    + srcBandOffsets[bands[i]]];
                                            // NoData check
                                            if (booleanLookupTable[(int) sample]) {
                                                // Update of all the statistics
                                                zoneGeo.add(sample, bands[i], classId, false);
                                            }
                                        }
                                        zoneList.set(index, zoneGeo);
                                    }
                                }
                            }
                        }
                    }
                }
                // ROI BOUNDS USED
            } else {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0);
                            // Control if the sample is inside ROI
                            if (w != 0) {
                                // Coordinate object creation for the spatial indexing
                                Coordinate p1 = new Coordinate(x0, y0);
                                // Envelope associated to the coordinate object
                                Envelope searchEnv = new Envelope(p1);
                                // Query on the geometry list
                                List<ROI> geomList = spatial.query(searchEnv);
                                // classId classifier initial value
                                int classId = 0;
                                // If the classifier is present then the zone value is taken
                                if (classPresent) {
                                    // Selection of the initial point
                                    Point pointSrc = new Point(x0, y0);
                                    // Initialization of the zone point
                                    Point pointClass = new Point();
                                    // Source point inverse transformation for finding the related zone point
                                    try {
                                        inverseTrans.inverseTransform(pointSrc, pointClass);

                                    } catch (NoninvertibleTransformException e) {
                                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                    }
                                    // Selection of the zone point
                                    classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                                }
                                // Cycle on all the geometries found
                                for (ROI geometry : geomList) {
                                    // if every geometry really contains the selected point
                                    if (geometry.contains(x0, y0)) {
                                        // then the related zoneGeometry object statistics are updated
                                        int index = rois.indexOf(geometry);

                                        synchronized (this) {
                                            ZoneGeometry zoneGeo = zoneList.get(index);

                                            // Cycle on the selected Bands
                                            for (int i = 0; i < bandNum; i++) {
                                                byte sample = srcData[bands[i]][posx + posy
                                                        + srcBandOffsets[bands[i]]];
                                                // NoData check
                                                if (booleanLookupTable[(int) sample]) {
                                                    // Update of all the statistics
                                                    zoneGeo.add(sample, bands[i], classId, false);
                                                }
                                            }
                                            zoneList.set(index, zoneGeo);
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

    private void ushortLoop(RasterAccessor src, Rectangle computableArea, RasterAccessor roi) {

        // Source and ROI RasterAccessor initial parameters
        final int srcX = src.getX();
        final int srcY = src.getY();

        final byte[] roiDataArray;
        final int roiScanLineStride;
        final int roiDataLength;

        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiScanLineStride = roi.getScanlineStride();
            roiDataLength = roiDataArray.length;
        } else {
            roiDataArray = null;
            roiScanLineStride = 0;
            roiDataLength = 0;
        }

        short srcData[][] = src.getShortDataArrays();

        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // NO DATA AND ROI ARE NOT PRESENT
        if (caseA) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y++) {
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x++) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;

                    // PixelPositions
                    int x0 = srcX + x;
                    int y0 = srcY + y;
                    // Coordinate object creation for the spatial indexing
                    Coordinate p1 = new Coordinate(x0, y0);
                    // Envelope associated to the coordinate object
                    Envelope searchEnv = new Envelope(p1);
                    // Query on the geometry list
                    List<ROI> geomList = spatial.query(searchEnv);
                    // classId classifier initial value
                    int classId = 0;
                    // If the classifier is present then the zone value is taken
                    if (classPresent) {
                        // Selection of the initial point
                        Point pointSrc = new Point(x0, y0);
                        // Initialization of the zone point
                        Point pointClass = new Point();
                        // Source point inverse transformation for finding the related zone point
                        try {
                            inverseTrans.inverseTransform(pointSrc, pointClass);

                        } catch (NoninvertibleTransformException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }
                        // Selection of the zone point
                        classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                    }
                    // Cycle on all the geometries found
                    for (ROI geometry : geomList) {
                        // if every geometry really contains the selected point
                        if (geometry.contains(x0, y0)) {
                            // then the related zoneGeometry object statistics are updated
                            int index = rois.indexOf(geometry);

                            synchronized (this) {
                                ZoneGeometry zoneGeo = zoneList.get(index);

                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    int sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]] & 0xFFFF;
                                    // Update of all the statistics
                                    zoneGeo.add(sample, bands[i], classId, false);
                                }
                                zoneList.set(index, zoneGeo);
                            }
                        }
                    }
                }
            }
            // ROI ACCESSOR USED
        } else if (caseB) {
            if (useROIAccessor) {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    int posyROI = y * roiScanLineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // ROI index position
                        int windex = x + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // Control if the sample is inside ROI
                        if (w != 0) {
                            // PixelPositions
                            int x0 = srcX + x;
                            int y0 = srcY + y;
                            // Coordinate object creation for the spatial indexing
                            Coordinate p1 = new Coordinate(x0, y0);
                            // Envelope associated to the coordinate object
                            Envelope searchEnv = new Envelope(p1);
                            // Query on the geometry list
                            List<ROI> geomList = spatial.query(searchEnv);
                            // classId classifier initial value
                            int classId = 0;
                            // If the classifier is present then the zone value is taken
                            if (classPresent) {
                                // Selection of the initial point
                                Point pointSrc = new Point(x0, y0);
                                // Initialization of the zone point
                                Point pointClass = new Point();
                                // Source point inverse transformation for finding the related zone point
                                try {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);

                                } catch (NoninvertibleTransformException e) {
                                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                }
                                // Selection of the zone point
                                classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                            }
                            // Cycle on all the geometries found
                            for (ROI geometry : geomList) {
                                // if every geometry really contains the selected point
                                if (geometry.contains(x0, y0)) {
                                    // then the related zoneGeometry object statistics are updated
                                    int index = rois.indexOf(geometry);

                                    synchronized (this) {
                                        ZoneGeometry zoneGeo = zoneList.get(index);

                                        // Cycle on the selected Bands
                                        for (int i = 0; i < bandNum; i++) {
                                            int sample = srcData[bands[i]][posx + posy
                                                    + srcBandOffsets[bands[i]]] & 0xFFFF;
                                            // Update of all the statistics
                                            zoneGeo.add(sample, bands[i], classId, false);
                                        }
                                        zoneList.set(index, zoneGeo);
                                    }
                                }
                            }
                        }
                    }
                }
                // ROI BOUNDS USED
            } else {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0);
                            // Control if the sample is inside ROI
                            if (w != 0) {
                                // Coordinate object creation for the spatial indexing
                                Coordinate p1 = new Coordinate(x0, y0);
                                // Envelope associated to the coordinate object
                                Envelope searchEnv = new Envelope(p1);
                                // Query on the geometry list
                                List<ROI> geomList = spatial.query(searchEnv);
                                // classId classifier initial value
                                int classId = 0;
                                // If the classifier is present then the zone value is taken
                                if (classPresent) {
                                    // Selection of the initial point
                                    Point pointSrc = new Point(x0, y0);
                                    // Initialization of the zone point
                                    Point pointClass = new Point();
                                    // Source point inverse transformation for finding the related zone point
                                    try {
                                        inverseTrans.inverseTransform(pointSrc, pointClass);

                                    } catch (NoninvertibleTransformException e) {
                                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                    }
                                    // Selection of the zone point
                                    classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                                }
                                // Cycle on all the geometries found
                                for (ROI geometry : geomList) {
                                    // if every geometry really contains the selected point
                                    if (geometry.contains(x0, y0)) {
                                        // then the related zoneGeometry object statistics are updated
                                        int index = rois.indexOf(geometry);

                                        synchronized (this) {
                                            ZoneGeometry zoneGeo = zoneList.get(index);

                                            // Cycle on the selected Bands
                                            for (int i = 0; i < bandNum; i++) {
                                                int sample = srcData[bands[i]][posx + posy
                                                        + srcBandOffsets[bands[i]]] & 0xFFFF;
                                                // Update of all the statistics
                                                zoneGeo.add(sample, bands[i], classId, false);
                                            }
                                            zoneList.set(index, zoneGeo);
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
            }
            // NO DATA USED (ROI NOT USED)
        } else if (caseC) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y++) {
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x++) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;
                    // PixelPositions
                    int x0 = srcX + x;
                    int y0 = srcY + y;
                    // Coordinate object creation for the spatial indexing
                    Coordinate p1 = new Coordinate(x0, y0);
                    // Envelope associated to the coordinate object
                    Envelope searchEnv = new Envelope(p1);
                    // Query on the geometry list
                    List<ROI> geomList = spatial.query(searchEnv);
                    // classId classifier initial value
                    int classId = 0;
                    // If the classifier is present then the zone value is taken
                    if (classPresent) {
                        // Selection of the initial point
                        Point pointSrc = new Point(x0, y0);
                        // Initialization of the zone point
                        Point pointClass = new Point();
                        // Source point inverse transformation for finding the related zone point
                        try {
                            inverseTrans.inverseTransform(pointSrc, pointClass);

                        } catch (NoninvertibleTransformException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }
                        // Selection of the zone point
                        classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                    }
                    // Cycle on all the geometries found
                    for (ROI geometry : geomList) {
                        // if every geometry really contains the selected point
                        if (geometry.contains(x0, y0)) {
                            // then the related zoneGeometry object statistics are updated
                            int index = rois.indexOf(geometry);

                            synchronized (this) {
                                ZoneGeometry zoneGeo = zoneList.get(index);

                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    int sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]] & 0xFFFF;
                                    // NoData check
                                    if (!noData.contains((short) sample)) {
                                        // Update of all the statistics
                                        zoneGeo.add(sample, bands[i], classId, false);
                                    }
                                }
                                zoneList.set(index, zoneGeo);
                            }
                        }
                    }
                }
            }
        } else {
            if (useROIAccessor) {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    int posyROI = y * roiScanLineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // ROI index position
                        int windex = x + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // Control if the sample is inside ROI
                        if (w != 0) {
                            // PixelPositions
                            int x0 = srcX + x;
                            int y0 = srcY + y;
                            // Coordinate object creation for the spatial indexing
                            Coordinate p1 = new Coordinate(x0, y0);
                            // Envelope associated to the coordinate object
                            Envelope searchEnv = new Envelope(p1);
                            // Query on the geometry list
                            List<ROI> geomList = spatial.query(searchEnv);
                            // classId classifier initial value
                            int classId = 0;
                            // If the classifier is present then the zone value is taken
                            if (classPresent) {
                                // Selection of the initial point
                                Point pointSrc = new Point(x0, y0);
                                // Initialization of the zone point
                                Point pointClass = new Point();
                                // Source point inverse transformation for finding the related zone point
                                try {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);

                                } catch (NoninvertibleTransformException e) {
                                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                }
                                // Selection of the zone point
                                classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                            }
                            // Cycle on all the geometries found
                            for (ROI geometry : geomList) {
                                // if every geometry really contains the selected point
                                if (geometry.contains(x0, y0)) {
                                    // then the related zoneGeometry object statistics are updated
                                    int index = rois.indexOf(geometry);

                                    synchronized (this) {
                                        ZoneGeometry zoneGeo = zoneList.get(index);

                                        // Cycle on the selected Bands
                                        for (int i = 0; i < bandNum; i++) {
                                            int sample = srcData[bands[i]][posx + posy
                                                    + srcBandOffsets[bands[i]]] & 0xFFFF;
                                            // NoData check
                                            if (!noData.contains((short) sample)) {
                                                // Update of all the statistics
                                                zoneGeo.add(sample, bands[i], classId, false);
                                            }
                                        }
                                        zoneList.set(index, zoneGeo);
                                    }
                                }
                            }
                        }
                    }
                }
                // ROI BOUNDS USED
            } else {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0);
                            // Control if the sample is inside ROI
                            if (w != 0) {
                                // Coordinate object creation for the spatial indexing
                                Coordinate p1 = new Coordinate(x0, y0);
                                // Envelope associated to the coordinate object
                                Envelope searchEnv = new Envelope(p1);
                                // Query on the geometry list
                                List<ROI> geomList = spatial.query(searchEnv);
                                // classId classifier initial value
                                int classId = 0;
                                // If the classifier is present then the zone value is taken
                                if (classPresent) {
                                    // Selection of the initial point
                                    Point pointSrc = new Point(x0, y0);
                                    // Initialization of the zone point
                                    Point pointClass = new Point();
                                    // Source point inverse transformation for finding the related zone point
                                    try {
                                        inverseTrans.inverseTransform(pointSrc, pointClass);

                                    } catch (NoninvertibleTransformException e) {
                                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                    }
                                    // Selection of the zone point
                                    classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                                }
                                // Cycle on all the geometries found
                                for (ROI geometry : geomList) {
                                    // if every geometry really contains the selected point
                                    if (geometry.contains(x0, y0)) {
                                        // then the related zoneGeometry object statistics are updated
                                        int index = rois.indexOf(geometry);

                                        synchronized (this) {
                                            ZoneGeometry zoneGeo = zoneList.get(index);

                                            // Cycle on the selected Bands
                                            for (int i = 0; i < bandNum; i++) {
                                                int sample = srcData[bands[i]][posx + posy
                                                        + srcBandOffsets[bands[i]]] & 0xFFFF;
                                                // NoData check
                                                if (!noData.contains((short) sample)) {
                                                    // Update of all the statistics
                                                    zoneGeo.add(sample, bands[i], classId, false);
                                                }
                                            }
                                            zoneList.set(index, zoneGeo);
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

    private void shortLoop(RasterAccessor src, Rectangle computableArea, RasterAccessor roi) {

        // Source and ROI RasterAccessor initial parameters
        final int srcX = src.getX();
        final int srcY = src.getY();

        final byte[] roiDataArray;
        final int roiScanLineStride;
        final int roiDataLength;

        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiScanLineStride = roi.getScanlineStride();
            roiDataLength = roiDataArray.length;
        } else {
            roiDataArray = null;
            roiScanLineStride = 0;
            roiDataLength = 0;
        }

        short srcData[][] = src.getShortDataArrays();

        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // NO DATA AND ROI ARE NOT PRESENT
        if (caseA) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y++) {
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x++) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;

                    // PixelPositions
                    int x0 = srcX + x;
                    int y0 = srcY + y;
                    // Coordinate object creation for the spatial indexing
                    Coordinate p1 = new Coordinate(x0, y0);
                    // Envelope associated to the coordinate object
                    Envelope searchEnv = new Envelope(p1);
                    // Query on the geometry list
                    List<ROI> geomList = spatial.query(searchEnv);
                    // classId classifier initial value
                    int classId = 0;
                    // If the classifier is present then the zone value is taken
                    if (classPresent) {
                        // Selection of the initial point
                        Point pointSrc = new Point(x0, y0);
                        // Initialization of the zone point
                        Point pointClass = new Point();
                        // Source point inverse transformation for finding the related zone point
                        try {
                            inverseTrans.inverseTransform(pointSrc, pointClass);

                        } catch (NoninvertibleTransformException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }
                        // Selection of the zone point
                        classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                    }
                    // Cycle on all the geometries found
                    for (ROI geometry : geomList) {
                        // if every geometry really contains the selected point
                        if (geometry.contains(x0, y0)) {
                            // then the related zoneGeometry object statistics are updated
                            int index = rois.indexOf(geometry);

                            synchronized (this) {
                                ZoneGeometry zoneGeo = zoneList.get(index);

                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    short sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // Update of all the statistics
                                    zoneGeo.add(sample, bands[i], classId, false);
                                }
                                zoneList.set(index, zoneGeo);
                            }
                        }
                    }
                }
            }
            // ROI ACCESSOR USED
        } else if (caseB) {
            if (useROIAccessor) {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    int posyROI = y * roiScanLineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // ROI index position
                        int windex = x + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // Control if the sample is inside ROI
                        if (w != 0) {
                            // PixelPositions
                            int x0 = srcX + x;
                            int y0 = srcY + y;
                            // Coordinate object creation for the spatial indexing
                            Coordinate p1 = new Coordinate(x0, y0);
                            // Envelope associated to the coordinate object
                            Envelope searchEnv = new Envelope(p1);
                            // Query on the geometry list
                            List<ROI> geomList = spatial.query(searchEnv);
                            // classId classifier initial value
                            int classId = 0;
                            // If the classifier is present then the zone value is taken
                            if (classPresent) {
                                // Selection of the initial point
                                Point pointSrc = new Point(x0, y0);
                                // Initialization of the zone point
                                Point pointClass = new Point();
                                // Source point inverse transformation for finding the related zone point
                                try {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);

                                } catch (NoninvertibleTransformException e) {
                                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                }
                                // Selection of the zone point
                                classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                            }
                            // Cycle on all the geometries found
                            for (ROI geometry : geomList) {
                                // if every geometry really contains the selected point
                                if (geometry.contains(x0, y0)) {
                                    // then the related zoneGeometry object statistics are updated
                                    int index = rois.indexOf(geometry);

                                    synchronized (this) {
                                        ZoneGeometry zoneGeo = zoneList.get(index);

                                        // Cycle on the selected Bands
                                        for (int i = 0; i < bandNum; i++) {
                                            short sample = srcData[bands[i]][posx + posy
                                                    + srcBandOffsets[bands[i]]];
                                            // Update of all the statistics
                                            zoneGeo.add(sample, bands[i], classId, false);
                                        }
                                        zoneList.set(index, zoneGeo);
                                    }
                                }
                            }
                        }
                    }
                }
                // ROI BOUNDS USED
            } else {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0);
                            // Control if the sample is inside ROI
                            if (w != 0) {
                                // Coordinate object creation for the spatial indexing
                                Coordinate p1 = new Coordinate(x0, y0);
                                // Envelope associated to the coordinate object
                                Envelope searchEnv = new Envelope(p1);
                                // Query on the geometry list
                                List<ROI> geomList = spatial.query(searchEnv);
                                // classId classifier initial value
                                int classId = 0;
                                // If the classifier is present then the zone value is taken
                                if (classPresent) {
                                    // Selection of the initial point
                                    Point pointSrc = new Point(x0, y0);
                                    // Initialization of the zone point
                                    Point pointClass = new Point();
                                    // Source point inverse transformation for finding the related zone point
                                    try {
                                        inverseTrans.inverseTransform(pointSrc, pointClass);

                                    } catch (NoninvertibleTransformException e) {
                                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                    }
                                    // Selection of the zone point
                                    classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                                }
                                // Cycle on all the geometries found
                                for (ROI geometry : geomList) {
                                    // if every geometry really contains the selected point
                                    if (geometry.contains(x0, y0)) {
                                        // then the related zoneGeometry object statistics are updated
                                        int index = rois.indexOf(geometry);

                                        synchronized (this) {
                                            ZoneGeometry zoneGeo = zoneList.get(index);

                                            // Cycle on the selected Bands
                                            for (int i = 0; i < bandNum; i++) {
                                                short sample = srcData[bands[i]][posx + posy
                                                        + srcBandOffsets[bands[i]]];
                                                // Update of all the statistics
                                                zoneGeo.add(sample, bands[i], classId, false);
                                            }
                                            zoneList.set(index, zoneGeo);
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
            }
            // NO DATA USED (ROI NOT USED)
        } else if (caseC) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y++) {
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x++) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;
                    // PixelPositions
                    int x0 = srcX + x;
                    int y0 = srcY + y;
                    // Coordinate object creation for the spatial indexing
                    Coordinate p1 = new Coordinate(x0, y0);
                    // Envelope associated to the coordinate object
                    Envelope searchEnv = new Envelope(p1);
                    // Query on the geometry list
                    List<ROI> geomList = spatial.query(searchEnv);
                    // classId classifier initial value
                    int classId = 0;
                    // If the classifier is present then the zone value is taken
                    if (classPresent) {
                        // Selection of the initial point
                        Point pointSrc = new Point(x0, y0);
                        // Initialization of the zone point
                        Point pointClass = new Point();
                        // Source point inverse transformation for finding the related zone point
                        try {
                            inverseTrans.inverseTransform(pointSrc, pointClass);

                        } catch (NoninvertibleTransformException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }
                        // Selection of the zone point
                        classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                    }
                    // Cycle on all the geometries found
                    for (ROI geometry : geomList) {
                        // if every geometry really contains the selected point
                        if (geometry.contains(x0, y0)) {
                            // then the related zoneGeometry object statistics are updated
                            int index = rois.indexOf(geometry);

                            synchronized (this) {
                                ZoneGeometry zoneGeo = zoneList.get(index);

                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    short sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // NoData check
                                    if (!noData.contains(sample)) {
                                        // Update of all the statistics
                                        zoneGeo.add(sample, bands[i], classId, false);
                                    }
                                }
                                zoneList.set(index, zoneGeo);
                            }
                        }
                    }
                }
            }
        } else {
            if (useROIAccessor) {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    int posyROI = y * roiScanLineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // ROI index position
                        int windex = x + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // Control if the sample is inside ROI
                        if (w != 0) {
                            // PixelPositions
                            int x0 = srcX + x;
                            int y0 = srcY + y;
                            // Coordinate object creation for the spatial indexing
                            Coordinate p1 = new Coordinate(x0, y0);
                            // Envelope associated to the coordinate object
                            Envelope searchEnv = new Envelope(p1);
                            // Query on the geometry list
                            List<ROI> geomList = spatial.query(searchEnv);
                            // classId classifier initial value
                            int classId = 0;
                            // If the classifier is present then the zone value is taken
                            if (classPresent) {
                                // Selection of the initial point
                                Point pointSrc = new Point(x0, y0);
                                // Initialization of the zone point
                                Point pointClass = new Point();
                                // Source point inverse transformation for finding the related zone point
                                try {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);

                                } catch (NoninvertibleTransformException e) {
                                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                }
                                // Selection of the zone point
                                classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                            }
                            // Cycle on all the geometries found
                            for (ROI geometry : geomList) {
                                // if every geometry really contains the selected point
                                if (geometry.contains(x0, y0)) {
                                    // then the related zoneGeometry object statistics are updated
                                    int index = rois.indexOf(geometry);

                                    synchronized (this) {
                                        ZoneGeometry zoneGeo = zoneList.get(index);

                                        // Cycle on the selected Bands
                                        for (int i = 0; i < bandNum; i++) {
                                            short sample = srcData[bands[i]][posx + posy
                                                    + srcBandOffsets[bands[i]]];
                                            // NoData check
                                            if (!noData.contains(sample)) {
                                                // Update of all the statistics
                                                zoneGeo.add(sample, bands[i], classId, false);
                                            }
                                        }
                                        zoneList.set(index, zoneGeo);
                                    }
                                }
                            }
                        }
                    }
                }
                // ROI BOUNDS USED
            } else {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0);
                            // Control if the sample is inside ROI
                            if (w != 0) {
                                // Coordinate object creation for the spatial indexing
                                Coordinate p1 = new Coordinate(x0, y0);
                                // Envelope associated to the coordinate object
                                Envelope searchEnv = new Envelope(p1);
                                // Query on the geometry list
                                List<ROI> geomList = spatial.query(searchEnv);
                                // classId classifier initial value
                                int classId = 0;
                                // If the classifier is present then the zone value is taken
                                if (classPresent) {
                                    // Selection of the initial point
                                    Point pointSrc = new Point(x0, y0);
                                    // Initialization of the zone point
                                    Point pointClass = new Point();
                                    // Source point inverse transformation for finding the related zone point
                                    try {
                                        inverseTrans.inverseTransform(pointSrc, pointClass);

                                    } catch (NoninvertibleTransformException e) {
                                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                    }
                                    // Selection of the zone point
                                    classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                                }
                                // Cycle on all the geometries found
                                for (ROI geometry : geomList) {
                                    // if every geometry really contains the selected point
                                    if (geometry.contains(x0, y0)) {
                                        // then the related zoneGeometry object statistics are updated
                                        int index = rois.indexOf(geometry);

                                        synchronized (this) {
                                            ZoneGeometry zoneGeo = zoneList.get(index);

                                            // Cycle on the selected Bands
                                            for (int i = 0; i < bandNum; i++) {
                                                short sample = srcData[bands[i]][posx + posy
                                                        + srcBandOffsets[bands[i]]];
                                                // NoData check
                                                if (!noData.contains(sample)) {
                                                    // Update of all the statistics
                                                    zoneGeo.add(sample, bands[i], classId, false);
                                                }
                                            }
                                            zoneList.set(index, zoneGeo);
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

    private void intLoop(RasterAccessor src, Rectangle computableArea, RasterAccessor roi) {

        // Source and ROI RasterAccessor initial parameters
        final int srcX = src.getX();
        final int srcY = src.getY();

        final byte[] roiDataArray;
        final int roiScanLineStride;
        final int roiDataLength;

        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiScanLineStride = roi.getScanlineStride();
            roiDataLength = roiDataArray.length;
        } else {
            roiDataArray = null;
            roiScanLineStride = 0;
            roiDataLength = 0;
        }

        int srcData[][] = src.getIntDataArrays();

        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // NO DATA AND ROI ARE NOT PRESENT
        if (caseA) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y++) {
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x++) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;

                    // PixelPositions
                    int x0 = srcX + x;
                    int y0 = srcY + y;
                    // Coordinate object creation for the spatial indexing
                    Coordinate p1 = new Coordinate(x0, y0);
                    // Envelope associated to the coordinate object
                    Envelope searchEnv = new Envelope(p1);
                    // Query on the geometry list
                    List<ROI> geomList = spatial.query(searchEnv);
                    // classId classifier initial value
                    int classId = 0;
                    // If the classifier is present then the zone value is taken
                    if (classPresent) {
                        // Selection of the initial point
                        Point pointSrc = new Point(x0, y0);
                        // Initialization of the zone point
                        Point pointClass = new Point();
                        // Source point inverse transformation for finding the related zone point
                        try {
                            inverseTrans.inverseTransform(pointSrc, pointClass);

                        } catch (NoninvertibleTransformException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }
                        // Selection of the zone point
                        classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                    }
                    // Cycle on all the geometries found
                    for (ROI geometry : geomList) {
                        // if every geometry really contains the selected point
                        if (geometry.contains(x0, y0)) {
                            // then the related zoneGeometry object statistics are updated
                            int index = rois.indexOf(geometry);

                            synchronized (this) {
                                ZoneGeometry zoneGeo = zoneList.get(index);

                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    int sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // Update of all the statistics
                                    zoneGeo.add(sample, bands[i], classId, false);
                                }
                                zoneList.set(index, zoneGeo);
                            }
                        }
                    }
                }
            }
            // ROI ACCESSOR USED
        } else if (caseB) {
            if (useROIAccessor) {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    int posyROI = y * roiScanLineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // ROI index position
                        int windex = x + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // Control if the sample is inside ROI
                        if (w != 0) {
                            // PixelPositions
                            int x0 = srcX + x;
                            int y0 = srcY + y;
                            // Coordinate object creation for the spatial indexing
                            Coordinate p1 = new Coordinate(x0, y0);
                            // Envelope associated to the coordinate object
                            Envelope searchEnv = new Envelope(p1);
                            // Query on the geometry list
                            List<ROI> geomList = spatial.query(searchEnv);
                            // classId classifier initial value
                            int classId = 0;
                            // If the classifier is present then the zone value is taken
                            if (classPresent) {
                                // Selection of the initial point
                                Point pointSrc = new Point(x0, y0);
                                // Initialization of the zone point
                                Point pointClass = new Point();
                                // Source point inverse transformation for finding the related zone point
                                try {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);

                                } catch (NoninvertibleTransformException e) {
                                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                }
                                // Selection of the zone point
                                classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                            }
                            // Cycle on all the geometries found
                            for (ROI geometry : geomList) {
                                // if every geometry really contains the selected point
                                if (geometry.contains(x0, y0)) {
                                    // then the related zoneGeometry object statistics are updated
                                    int index = rois.indexOf(geometry);

                                    synchronized (this) {
                                        ZoneGeometry zoneGeo = zoneList.get(index);

                                        // Cycle on the selected Bands
                                        for (int i = 0; i < bandNum; i++) {
                                            int sample = srcData[bands[i]][posx + posy
                                                    + srcBandOffsets[bands[i]]];
                                            // Update of all the statistics
                                            zoneGeo.add(sample, bands[i], classId, false);
                                        }
                                        zoneList.set(index, zoneGeo);
                                    }
                                }
                            }
                        }
                    }
                }
                // ROI BOUNDS USED
            } else {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0);
                            // Control if the sample is inside ROI
                            if (w != 0) {
                                // Coordinate object creation for the spatial indexing
                                Coordinate p1 = new Coordinate(x0, y0);
                                // Envelope associated to the coordinate object
                                Envelope searchEnv = new Envelope(p1);
                                // Query on the geometry list
                                List<ROI> geomList = spatial.query(searchEnv);
                                // classId classifier initial value
                                int classId = 0;
                                // If the classifier is present then the zone value is taken
                                if (classPresent) {
                                    // Selection of the initial point
                                    Point pointSrc = new Point(x0, y0);
                                    // Initialization of the zone point
                                    Point pointClass = new Point();
                                    // Source point inverse transformation for finding the related zone point
                                    try {
                                        inverseTrans.inverseTransform(pointSrc, pointClass);

                                    } catch (NoninvertibleTransformException e) {
                                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                    }
                                    // Selection of the zone point
                                    classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                                }
                                // Cycle on all the geometries found
                                for (ROI geometry : geomList) {
                                    // if every geometry really contains the selected point
                                    if (geometry.contains(x0, y0)) {
                                        // then the related zoneGeometry object statistics are updated
                                        int index = rois.indexOf(geometry);

                                        synchronized (this) {
                                            ZoneGeometry zoneGeo = zoneList.get(index);

                                            // Cycle on the selected Bands
                                            for (int i = 0; i < bandNum; i++) {
                                                int sample = srcData[bands[i]][posx + posy
                                                        + srcBandOffsets[bands[i]]];
                                                // Update of all the statistics
                                                zoneGeo.add(sample, bands[i], classId, false);
                                            }
                                            zoneList.set(index, zoneGeo);
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
            }
            // NO DATA USED (ROI NOT USED)
        } else if (caseC) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y++) {
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x++) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;
                    // PixelPositions
                    int x0 = srcX + x;
                    int y0 = srcY + y;
                    // Coordinate object creation for the spatial indexing
                    Coordinate p1 = new Coordinate(x0, y0);
                    // Envelope associated to the coordinate object
                    Envelope searchEnv = new Envelope(p1);
                    // Query on the geometry list
                    List<ROI> geomList = spatial.query(searchEnv);
                    // classId classifier initial value
                    int classId = 0;
                    // If the classifier is present then the zone value is taken
                    if (classPresent) {
                        // Selection of the initial point
                        Point pointSrc = new Point(x0, y0);
                        // Initialization of the zone point
                        Point pointClass = new Point();
                        // Source point inverse transformation for finding the related zone point
                        try {
                            inverseTrans.inverseTransform(pointSrc, pointClass);

                        } catch (NoninvertibleTransformException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }
                        // Selection of the zone point
                        classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                    }
                    // Cycle on all the geometries found
                    for (ROI geometry : geomList) {
                        // if every geometry really contains the selected point
                        if (geometry.contains(x0, y0)) {
                            // then the related zoneGeometry object statistics are updated
                            int index = rois.indexOf(geometry);

                            synchronized (this) {
                                ZoneGeometry zoneGeo = zoneList.get(index);

                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    int sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // NoData check
                                    if (!noData.contains(sample)) {
                                        // Update of all the statistics
                                        zoneGeo.add(sample, bands[i], classId, false);
                                    }
                                }
                                zoneList.set(index, zoneGeo);
                            }
                        }
                    }
                }
            }
        } else {
            if (useROIAccessor) {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    int posyROI = y * roiScanLineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // ROI index position
                        int windex = x + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // Control if the sample is inside ROI
                        if (w != 0) {
                            // PixelPositions
                            int x0 = srcX + x;
                            int y0 = srcY + y;
                            // Coordinate object creation for the spatial indexing
                            Coordinate p1 = new Coordinate(x0, y0);
                            // Envelope associated to the coordinate object
                            Envelope searchEnv = new Envelope(p1);
                            // Query on the geometry list
                            List<ROI> geomList = spatial.query(searchEnv);
                            // classId classifier initial value
                            int classId = 0;
                            // If the classifier is present then the zone value is taken
                            if (classPresent) {
                                // Selection of the initial point
                                Point pointSrc = new Point(x0, y0);
                                // Initialization of the zone point
                                Point pointClass = new Point();
                                // Source point inverse transformation for finding the related zone point
                                try {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);

                                } catch (NoninvertibleTransformException e) {
                                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                }
                                // Selection of the zone point
                                classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                            }
                            // Cycle on all the geometries found
                            for (ROI geometry : geomList) {
                                // if every geometry really contains the selected point
                                if (geometry.contains(x0, y0)) {
                                    // then the related zoneGeometry object statistics are updated
                                    int index = rois.indexOf(geometry);

                                    synchronized (this) {
                                        ZoneGeometry zoneGeo = zoneList.get(index);

                                        // Cycle on the selected Bands
                                        for (int i = 0; i < bandNum; i++) {
                                            int sample = srcData[bands[i]][posx + posy
                                                    + srcBandOffsets[bands[i]]];
                                            // NoData check
                                            if (!noData.contains(sample)) {
                                                // Update of all the statistics
                                                zoneGeo.add(sample, bands[i], classId, false);
                                            }
                                        }
                                        zoneList.set(index, zoneGeo);
                                    }
                                }
                            }
                        }
                    }
                }
                // ROI BOUNDS USED
            } else {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0);
                            // Control if the sample is inside ROI
                            if (w != 0) {
                                // Coordinate object creation for the spatial indexing
                                Coordinate p1 = new Coordinate(x0, y0);
                                // Envelope associated to the coordinate object
                                Envelope searchEnv = new Envelope(p1);
                                // Query on the geometry list
                                List<ROI> geomList = spatial.query(searchEnv);
                                // classId classifier initial value
                                int classId = 0;
                                // If the classifier is present then the zone value is taken
                                if (classPresent) {
                                    // Selection of the initial point
                                    Point pointSrc = new Point(x0, y0);
                                    // Initialization of the zone point
                                    Point pointClass = new Point();
                                    // Source point inverse transformation for finding the related zone point
                                    try {
                                        inverseTrans.inverseTransform(pointSrc, pointClass);

                                    } catch (NoninvertibleTransformException e) {
                                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                    }
                                    // Selection of the zone point
                                    classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                                }
                                // Cycle on all the geometries found
                                for (ROI geometry : geomList) {
                                    // if every geometry really contains the selected point
                                    if (geometry.contains(x0, y0)) {
                                        // then the related zoneGeometry object statistics are updated
                                        int index = rois.indexOf(geometry);

                                        synchronized (this) {
                                            ZoneGeometry zoneGeo = zoneList.get(index);

                                            // Cycle on the selected Bands
                                            for (int i = 0; i < bandNum; i++) {
                                                int sample = srcData[bands[i]][posx + posy
                                                        + srcBandOffsets[bands[i]]];
                                                // NoData check
                                                if (!noData.contains(sample)) {
                                                    // Update of all the statistics
                                                    zoneGeo.add(sample, bands[i], classId, false);
                                                }
                                            }
                                            zoneList.set(index, zoneGeo);
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

    private void floatLoop(RasterAccessor src, Rectangle computableArea, RasterAccessor roi) {

        // Source and ROI RasterAccessor initial parameters
        final int srcX = src.getX();
        final int srcY = src.getY();

        final byte[] roiDataArray;
        final int roiScanLineStride;
        final int roiDataLength;

        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiScanLineStride = roi.getScanlineStride();
            roiDataLength = roiDataArray.length;
        } else {
            roiDataArray = null;
            roiScanLineStride = 0;
            roiDataLength = 0;
        }

        float srcData[][] = src.getFloatDataArrays();

        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // NO DATA AND ROI ARE NOT PRESENT
        if (caseA) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y++) {
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x++) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;

                    // PixelPositions
                    int x0 = srcX + x;
                    int y0 = srcY + y;
                    // Coordinate object creation for the spatial indexing
                    Coordinate p1 = new Coordinate(x0, y0);
                    // Envelope associated to the coordinate object
                    Envelope searchEnv = new Envelope(p1);
                    // Query on the geometry list
                    List<ROI> geomList = spatial.query(searchEnv);
                    // classId classifier initial value
                    int classId = 0;
                    // If the classifier is present then the zone value is taken
                    if (classPresent) {
                        // Selection of the initial point
                        Point pointSrc = new Point(x0, y0);
                        // Initialization of the zone point
                        Point pointClass = new Point();
                        // Source point inverse transformation for finding the related zone point
                        try {
                            inverseTrans.inverseTransform(pointSrc, pointClass);

                        } catch (NoninvertibleTransformException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }
                        // Selection of the zone point
                        classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                    }
                    // Cycle on all the geometries found
                    for (ROI geometry : geomList) {
                        // if every geometry really contains the selected point
                        if (geometry.contains(x0, y0)) {
                            // then the related zoneGeometry object statistics are updated
                            int index = rois.indexOf(geometry);

                            synchronized (this) {
                                ZoneGeometry zoneGeo = zoneList.get(index);

                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    float sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // Update of all the statistics
                                    zoneGeo.add(sample, bands[i], classId, false);
                                }
                                zoneList.set(index, zoneGeo);
                            }
                        }
                    }
                }
            }
            // ROI ACCESSOR USED
        } else if (caseB) {
            if (useROIAccessor) {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    int posyROI = y * roiScanLineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // ROI index position
                        int windex = x + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // Control if the sample is inside ROI
                        if (w != 0) {
                            // PixelPositions
                            int x0 = srcX + x;
                            int y0 = srcY + y;
                            // Coordinate object creation for the spatial indexing
                            Coordinate p1 = new Coordinate(x0, y0);
                            // Envelope associated to the coordinate object
                            Envelope searchEnv = new Envelope(p1);
                            // Query on the geometry list
                            List<ROI> geomList = spatial.query(searchEnv);
                            // classId classifier initial value
                            int classId = 0;
                            // If the classifier is present then the zone value is taken
                            if (classPresent) {
                                // Selection of the initial point
                                Point pointSrc = new Point(x0, y0);
                                // Initialization of the zone point
                                Point pointClass = new Point();
                                // Source point inverse transformation for finding the related zone point
                                try {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);

                                } catch (NoninvertibleTransformException e) {
                                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                }
                                // Selection of the zone point
                                classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                            }
                            // Cycle on all the geometries found
                            for (ROI geometry : geomList) {
                                // if every geometry really contains the selected point
                                if (geometry.contains(x0, y0)) {
                                    // then the related zoneGeometry object statistics are updated
                                    int index = rois.indexOf(geometry);

                                    synchronized (this) {
                                        ZoneGeometry zoneGeo = zoneList.get(index);

                                        // Cycle on the selected Bands
                                        for (int i = 0; i < bandNum; i++) {
                                            float sample = srcData[bands[i]][posx + posy
                                                    + srcBandOffsets[bands[i]]];
                                            // Update of all the statistics
                                            zoneGeo.add(sample, bands[i], classId, false);
                                        }
                                        zoneList.set(index, zoneGeo);
                                    }
                                }
                            }
                        }
                    }
                }
                // ROI BOUNDS USED
            } else {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0);
                            // Control if the sample is inside ROI
                            if (w != 0) {
                                // Coordinate object creation for the spatial indexing
                                Coordinate p1 = new Coordinate(x0, y0);
                                // Envelope associated to the coordinate object
                                Envelope searchEnv = new Envelope(p1);
                                // Query on the geometry list
                                List<ROI> geomList = spatial.query(searchEnv);
                                // classId classifier initial value
                                int classId = 0;
                                // If the classifier is present then the zone value is taken
                                if (classPresent) {
                                    // Selection of the initial point
                                    Point pointSrc = new Point(x0, y0);
                                    // Initialization of the zone point
                                    Point pointClass = new Point();
                                    // Source point inverse transformation for finding the related zone point
                                    try {
                                        inverseTrans.inverseTransform(pointSrc, pointClass);

                                    } catch (NoninvertibleTransformException e) {
                                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                    }
                                    // Selection of the zone point
                                    classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                                }
                                // Cycle on all the geometries found
                                for (ROI geometry : geomList) {
                                    // if every geometry really contains the selected point
                                    if (geometry.contains(x0, y0)) {
                                        // then the related zoneGeometry object statistics are updated
                                        int index = rois.indexOf(geometry);

                                        synchronized (this) {
                                            ZoneGeometry zoneGeo = zoneList.get(index);

                                            // Cycle on the selected Bands
                                            for (int i = 0; i < bandNum; i++) {
                                                float sample = srcData[bands[i]][posx + posy
                                                        + srcBandOffsets[bands[i]]];
                                                // Update of all the statistics
                                                zoneGeo.add(sample, bands[i], classId, false);
                                            }
                                            zoneList.set(index, zoneGeo);
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
            }
            // NO DATA USED (ROI NOT USED)
        } else if (caseC) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y++) {
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x++) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;
                    // PixelPositions
                    int x0 = srcX + x;
                    int y0 = srcY + y;
                    // Coordinate object creation for the spatial indexing
                    Coordinate p1 = new Coordinate(x0, y0);
                    // Envelope associated to the coordinate object
                    Envelope searchEnv = new Envelope(p1);
                    // Query on the geometry list
                    List<ROI> geomList = spatial.query(searchEnv);
                    // classId classifier initial value
                    int classId = 0;
                    // If the classifier is present then the zone value is taken
                    if (classPresent) {
                        // Selection of the initial point
                        Point pointSrc = new Point(x0, y0);
                        // Initialization of the zone point
                        Point pointClass = new Point();
                        // Source point inverse transformation for finding the related zone point
                        try {
                            inverseTrans.inverseTransform(pointSrc, pointClass);

                        } catch (NoninvertibleTransformException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }
                        // Selection of the zone point
                        classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                    }
                    // Cycle on all the geometries found
                    for (ROI geometry : geomList) {
                        // if every geometry really contains the selected point
                        if (geometry.contains(x0, y0)) {
                            // then the related zoneGeometry object statistics are updated
                            int index = rois.indexOf(geometry);

                            synchronized (this) {
                                ZoneGeometry zoneGeo = zoneList.get(index);

                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    float sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // NoData check
                                    if (!noData.contains(sample)) {
                                        boolean isNaN = Float.isNaN(sample);
                                        // Update of all the statistics
                                        zoneGeo.add(sample, bands[i], classId, isNaN);
                                    }
                                }
                                zoneList.set(index, zoneGeo);
                            }
                        }
                    }
                }
            }
        } else {
            if (useROIAccessor) {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    int posyROI = y * roiScanLineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // ROI index position
                        int windex = x + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // Control if the sample is inside ROI
                        if (w != 0) {
                            // PixelPositions
                            int x0 = srcX + x;
                            int y0 = srcY + y;
                            // Coordinate object creation for the spatial indexing
                            Coordinate p1 = new Coordinate(x0, y0);
                            // Envelope associated to the coordinate object
                            Envelope searchEnv = new Envelope(p1);
                            // Query on the geometry list
                            List<ROI> geomList = spatial.query(searchEnv);
                            // classId classifier initial value
                            int classId = 0;
                            // If the classifier is present then the zone value is taken
                            if (classPresent) {
                                // Selection of the initial point
                                Point pointSrc = new Point(x0, y0);
                                // Initialization of the zone point
                                Point pointClass = new Point();
                                // Source point inverse transformation for finding the related zone point
                                try {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);

                                } catch (NoninvertibleTransformException e) {
                                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                }
                                // Selection of the zone point
                                classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                            }
                            // Cycle on all the geometries found
                            for (ROI geometry : geomList) {
                                // if every geometry really contains the selected point
                                if (geometry.contains(x0, y0)) {
                                    // then the related zoneGeometry object statistics are updated
                                    int index = rois.indexOf(geometry);

                                    synchronized (this) {
                                        ZoneGeometry zoneGeo = zoneList.get(index);

                                        // Cycle on the selected Bands
                                        for (int i = 0; i < bandNum; i++) {
                                            float sample = srcData[bands[i]][posx + posy
                                                    + srcBandOffsets[bands[i]]];
                                            // NoData check
                                            if (!noData.contains(sample)) {
                                                boolean isNaN = Float.isNaN(sample);
                                                // Update of all the statistics
                                                zoneGeo.add(sample, bands[i], classId, isNaN);
                                            }
                                        }
                                        zoneList.set(index, zoneGeo);
                                    }
                                }
                            }
                        }
                    }
                }
                // ROI BOUNDS USED
            } else {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0);
                            // Control if the sample is inside ROI
                            if (w != 0) {
                                // Coordinate object creation for the spatial indexing
                                Coordinate p1 = new Coordinate(x0, y0);
                                // Envelope associated to the coordinate object
                                Envelope searchEnv = new Envelope(p1);
                                // Query on the geometry list
                                List<ROI> geomList = spatial.query(searchEnv);
                                // classId classifier initial value
                                int classId = 0;
                                // If the classifier is present then the zone value is taken
                                if (classPresent) {
                                    // Selection of the initial point
                                    Point pointSrc = new Point(x0, y0);
                                    // Initialization of the zone point
                                    Point pointClass = new Point();
                                    // Source point inverse transformation for finding the related zone point
                                    try {
                                        inverseTrans.inverseTransform(pointSrc, pointClass);

                                    } catch (NoninvertibleTransformException e) {
                                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                    }
                                    // Selection of the zone point
                                    classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                                }
                                // Cycle on all the geometries found
                                for (ROI geometry : geomList) {
                                    // if every geometry really contains the selected point
                                    if (geometry.contains(x0, y0)) {
                                        // then the related zoneGeometry object statistics are updated
                                        int index = rois.indexOf(geometry);

                                        synchronized (this) {
                                            ZoneGeometry zoneGeo = zoneList.get(index);

                                            // Cycle on the selected Bands
                                            for (int i = 0; i < bandNum; i++) {
                                                float sample = srcData[bands[i]][posx + posy
                                                        + srcBandOffsets[bands[i]]];
                                                // NoData check
                                                if (!noData.contains(sample)) {
                                                    boolean isNaN = Float.isNaN(sample);
                                                    // Update of all the statistics
                                                    zoneGeo.add(sample, bands[i], classId, isNaN);
                                                }
                                            }
                                            zoneList.set(index, zoneGeo);
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

    private void doubleLoop(RasterAccessor src, Rectangle computableArea, RasterAccessor roi) {

        // Source and ROI RasterAccessor initial parameters
        final int srcX = src.getX();
        final int srcY = src.getY();

        final byte[] roiDataArray;
        final int roiScanLineStride;
        final int roiDataLength;

        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiScanLineStride = roi.getScanlineStride();
            roiDataLength = roiDataArray.length;
        } else {
            roiDataArray = null;
            roiScanLineStride = 0;
            roiDataLength = 0;
        }

        double srcData[][] = src.getDoubleDataArrays();

        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // NO DATA AND ROI ARE NOT PRESENT
        if (caseA) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y++) {
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x++) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;

                    // PixelPositions
                    int x0 = srcX + x;
                    int y0 = srcY + y;
                    // Coordinate object creation for the spatial indexing
                    Coordinate p1 = new Coordinate(x0, y0);
                    // Envelope associated to the coordinate object
                    Envelope searchEnv = new Envelope(p1);
                    // Query on the geometry list
                    List<ROI> geomList = spatial.query(searchEnv);
                    // classId classifier initial value
                    int classId = 0;
                    // If the classifier is present then the zone value is taken
                    if (classPresent) {
                        // Selection of the initial point
                        Point pointSrc = new Point(x0, y0);
                        // Initialization of the zone point
                        Point pointClass = new Point();
                        // Source point inverse transformation for finding the related zone point
                        try {
                            inverseTrans.inverseTransform(pointSrc, pointClass);

                        } catch (NoninvertibleTransformException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }
                        // Selection of the zone point
                        classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                    }
                    // Cycle on all the geometries found
                    for (ROI geometry : geomList) {
                        // if every geometry really contains the selected point
                        if (geometry.contains(x0, y0)) {
                            // then the related zoneGeometry object statistics are updated
                            int index = rois.indexOf(geometry);

                            synchronized (this) {
                                ZoneGeometry zoneGeo = zoneList.get(index);

                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    double sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // Update of all the statistics
                                    zoneGeo.add(sample, bands[i], classId, false);
                                }
                                zoneList.set(index, zoneGeo);
                            }
                        }
                    }
                }
            }
            // ROI ACCESSOR USED
        } else if (caseB) {
            if (useROIAccessor) {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    int posyROI = y * roiScanLineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // ROI index position
                        int windex = x + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // Control if the sample is inside ROI
                        if (w != 0) {
                            // PixelPositions
                            int x0 = srcX + x;
                            int y0 = srcY + y;
                            // Coordinate object creation for the spatial indexing
                            Coordinate p1 = new Coordinate(x0, y0);
                            // Envelope associated to the coordinate object
                            Envelope searchEnv = new Envelope(p1);
                            // Query on the geometry list
                            List<ROI> geomList = spatial.query(searchEnv);
                            // classId classifier initial value
                            int classId = 0;
                            // If the classifier is present then the zone value is taken
                            if (classPresent) {
                                // Selection of the initial point
                                Point pointSrc = new Point(x0, y0);
                                // Initialization of the zone point
                                Point pointClass = new Point();
                                // Source point inverse transformation for finding the related zone point
                                try {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);

                                } catch (NoninvertibleTransformException e) {
                                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                }
                                // Selection of the zone point
                                classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                            }
                            // Cycle on all the geometries found
                            for (ROI geometry : geomList) {
                                // if every geometry really contains the selected point
                                if (geometry.contains(x0, y0)) {
                                    // then the related zoneGeometry object statistics are updated
                                    int index = rois.indexOf(geometry);

                                    synchronized (this) {
                                        ZoneGeometry zoneGeo = zoneList.get(index);

                                        // Cycle on the selected Bands
                                        for (int i = 0; i < bandNum; i++) {
                                            double sample = srcData[bands[i]][posx + posy
                                                    + srcBandOffsets[bands[i]]];
                                            // Update of all the statistics
                                            zoneGeo.add(sample, bands[i], classId, false);
                                        }
                                        zoneList.set(index, zoneGeo);
                                    }
                                }
                            }
                        }
                    }
                }
                // ROI BOUNDS USED
            } else {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0);
                            // Control if the sample is inside ROI
                            if (w != 0) {
                                // Coordinate object creation for the spatial indexing
                                Coordinate p1 = new Coordinate(x0, y0);
                                // Envelope associated to the coordinate object
                                Envelope searchEnv = new Envelope(p1);
                                // Query on the geometry list
                                List<ROI> geomList = spatial.query(searchEnv);
                                // classId classifier initial value
                                int classId = 0;
                                // If the classifier is present then the zone value is taken
                                if (classPresent) {
                                    // Selection of the initial point
                                    Point pointSrc = new Point(x0, y0);
                                    // Initialization of the zone point
                                    Point pointClass = new Point();
                                    // Source point inverse transformation for finding the related zone point
                                    try {
                                        inverseTrans.inverseTransform(pointSrc, pointClass);

                                    } catch (NoninvertibleTransformException e) {
                                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                    }
                                    // Selection of the zone point
                                    classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                                }
                                // Cycle on all the geometries found
                                for (ROI geometry : geomList) {
                                    // if every geometry really contains the selected point
                                    if (geometry.contains(x0, y0)) {
                                        // then the related zoneGeometry object statistics are updated
                                        int index = rois.indexOf(geometry);

                                        synchronized (this) {
                                            ZoneGeometry zoneGeo = zoneList.get(index);

                                            // Cycle on the selected Bands
                                            for (int i = 0; i < bandNum; i++) {
                                                double sample = srcData[bands[i]][posx + posy
                                                        + srcBandOffsets[bands[i]]];
                                                // Update of all the statistics
                                                zoneGeo.add(sample, bands[i], classId, false);
                                            }
                                            zoneList.set(index, zoneGeo);
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
            }
            // NO DATA USED (ROI NOT USED)
        } else if (caseC) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y++) {
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x++) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;
                    // PixelPositions
                    int x0 = srcX + x;
                    int y0 = srcY + y;
                    // Coordinate object creation for the spatial indexing
                    Coordinate p1 = new Coordinate(x0, y0);
                    // Envelope associated to the coordinate object
                    Envelope searchEnv = new Envelope(p1);
                    // Query on the geometry list
                    List<ROI> geomList = spatial.query(searchEnv);
                    // classId classifier initial value
                    int classId = 0;
                    // If the classifier is present then the zone value is taken
                    if (classPresent) {
                        // Selection of the initial point
                        Point pointSrc = new Point(x0, y0);
                        // Initialization of the zone point
                        Point pointClass = new Point();
                        // Source point inverse transformation for finding the related zone point
                        try {
                            inverseTrans.inverseTransform(pointSrc, pointClass);

                        } catch (NoninvertibleTransformException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }
                        // Selection of the zone point
                        classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                    }
                    // Cycle on all the geometries found
                    for (ROI geometry : geomList) {
                        // if every geometry really contains the selected point
                        if (geometry.contains(x0, y0)) {
                            // then the related zoneGeometry object statistics are updated
                            int index = rois.indexOf(geometry);

                            synchronized (this) {
                                ZoneGeometry zoneGeo = zoneList.get(index);

                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    double sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // NoData check
                                    if (!noData.contains(sample)) {
                                        boolean isNaN = Double.isNaN(sample);
                                        // Update of all the statistics
                                        zoneGeo.add(sample, bands[i], classId, isNaN);
                                    }
                                }
                                zoneList.set(index, zoneGeo);
                            }
                        }
                    }
                }
            }
        } else {
            if (useROIAccessor) {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    int posyROI = y * roiScanLineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // ROI index position
                        int windex = x + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // Control if the sample is inside ROI
                        if (w != 0) {
                            // PixelPositions
                            int x0 = srcX + x;
                            int y0 = srcY + y;
                            // Coordinate object creation for the spatial indexing
                            Coordinate p1 = new Coordinate(x0, y0);
                            // Envelope associated to the coordinate object
                            Envelope searchEnv = new Envelope(p1);
                            // Query on the geometry list
                            List<ROI> geomList = spatial.query(searchEnv);
                            // classId classifier initial value
                            int classId = 0;
                            // If the classifier is present then the zone value is taken
                            if (classPresent) {
                                // Selection of the initial point
                                Point pointSrc = new Point(x0, y0);
                                // Initialization of the zone point
                                Point pointClass = new Point();
                                // Source point inverse transformation for finding the related zone point
                                try {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);

                                } catch (NoninvertibleTransformException e) {
                                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                }
                                // Selection of the zone point
                                classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                            }
                            // Cycle on all the geometries found
                            for (ROI geometry : geomList) {
                                // if every geometry really contains the selected point
                                if (geometry.contains(x0, y0)) {
                                    // then the related zoneGeometry object statistics are updated
                                    int index = rois.indexOf(geometry);

                                    synchronized (this) {
                                        ZoneGeometry zoneGeo = zoneList.get(index);

                                        // Cycle on the selected Bands
                                        for (int i = 0; i < bandNum; i++) {
                                            double sample = srcData[bands[i]][posx + posy
                                                    + srcBandOffsets[bands[i]]];
                                            // NoData check
                                            if (!noData.contains(sample)) {
                                                boolean isNaN = Double.isNaN(sample);
                                                // Update of all the statistics
                                                zoneGeo.add(sample, bands[i], classId, isNaN);
                                            }
                                        }
                                        zoneList.set(index, zoneGeo);
                                    }
                                }
                            }
                        }
                    }
                }
                // ROI BOUNDS USED
            } else {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0);
                            // Control if the sample is inside ROI
                            if (w != 0) {
                                // Coordinate object creation for the spatial indexing
                                Coordinate p1 = new Coordinate(x0, y0);
                                // Envelope associated to the coordinate object
                                Envelope searchEnv = new Envelope(p1);
                                // Query on the geometry list
                                List<ROI> geomList = spatial.query(searchEnv);
                                // classId classifier initial value
                                int classId = 0;
                                // If the classifier is present then the zone value is taken
                                if (classPresent) {
                                    // Selection of the initial point
                                    Point pointSrc = new Point(x0, y0);
                                    // Initialization of the zone point
                                    Point pointClass = new Point();
                                    // Source point inverse transformation for finding the related zone point
                                    try {
                                        inverseTrans.inverseTransform(pointSrc, pointClass);

                                    } catch (NoninvertibleTransformException e) {
                                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                    }
                                    // Selection of the zone point
                                    classId = iterator.getSample(pointClass.x, pointClass.y, 0);
                                }
                                // Cycle on all the geometries found
                                for (ROI geometry : geomList) {
                                    // if every geometry really contains the selected point
                                    if (geometry.contains(x0, y0)) {
                                        // then the related zoneGeometry object statistics are updated
                                        int index = rois.indexOf(geometry);

                                        synchronized (this) {
                                            ZoneGeometry zoneGeo = zoneList.get(index);

                                            // Cycle on the selected Bands
                                            for (int i = 0; i < bandNum; i++) {
                                                double sample = srcData[bands[i]][posx + posy
                                                        + srcBandOffsets[bands[i]]];
                                                // NoData check
                                                if (!noData.contains(sample)) {
                                                    boolean isNaN = Double.isNaN(sample);
                                                    // Update of all the statistics
                                                    zoneGeo.add(sample, bands[i], classId, isNaN);
                                                }
                                            }
                                            zoneList.set(index, zoneGeo);
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

    /** {@link OpImage} method that returns the destination image bounds, because source and destination images are equals*/
    @Override
    public Rectangle mapDestRect(Rectangle destRect, int index) {
        return destRect;
    }

    /** {@link OpImage} method that returns the source image bounds, because source and destination images are equals*/
    @Override
    public Rectangle mapSourceRect(Rectangle sourceRect, int index) {
            return sourceRect;
    }

    /**
     * Returns a list of property names that are recognized by this image.
     * 
     * @return An array of <code>String</code>s containing valid property names.
     */
    public String[] getPropertyNames() {
        // Get statistics names and names from superclass.
        String[] statsNames = new String[] { ZonalStatsDescriptor.ZS_PROPERTY };
        String[] superNames = super.getPropertyNames();

        // Return stats names if not superclass names.
        if (superNames == null) {
            return statsNames;
        }

        // Check for overlap between stats names and superclass names.
        List extraNames = new ArrayList();
        for (int i = 0; i < statsNames.length; i++) {
            String prefix = statsNames[i];
            String[] names = PropertyUtil.getPropertyNames(superNames, prefix);
            if (names != null) {
                for (int j = 0; j < names.length; j++) {
                    if (names[j].equalsIgnoreCase(prefix)) {
                        extraNames.add(prefix);
                    }
                }
            }
        }

        // If no overlap then return.
        if (extraNames.size() == 0) {
            return superNames;
        }

        // Combine superclass and extra names.
        String[] propNames = new String[superNames.length + extraNames.size()];
        System.arraycopy(superNames, 0, propNames, 0, superNames.length);
        int offset = superNames.length;
        for (int i = 0; i < extraNames.size(); i++) {
            propNames[offset++] = (String) extraNames.get(i);
        }

        // Return combined name set.
        return propNames;
    }

    /**
     * This method is used if the user needs to perform again the statistical calculations.
     */
    public synchronized void clearStatistic() {
        int zoneSize = zoneList.size();
        for (int i = zoneSize - 1; i >= 0; i--) {
            zoneList.remove(i);
        }
        firstTime = true;
    }

    /**
     * When the dispose method is called, then old dispose method is performed and also the statistic container is cleared.
     */
    public void dispose() {
        super.dispose();
        clearStatistic();
    }

    /**
     * Computes and returns all tiles in the image. The tiles are returned in a sequence corresponding to the row-major order of their respective tile
     * indices. The returned array may of course be ignored, e.g., in the case of a subclass which caches the tiles and the intent is to force their
     * computation. This method is overridden such that can be invoked only one time by using a flag for avoiding unnecessary computations.
     */
    public Raster[] getTiles() {
        if (firstTime) {
            firstTime = false;
            return getTiles(getTileIndices(getBounds()));
        } else {
            return null;
        }
    }

    /**
     * Get the specified property.
     * <p>
     * Use this method to retrieve the calculated statistics as an array per band and per statistic types.
     * 
     * @param name property name
     * 
     * @return the requested property
     */
    @Override
    public Object getProperty(String name) {
        // If the specified property is "JAI-EXT.stats", the calculations are performed.
        if (ZonalStatsDescriptor.ZS_PROPERTY.equalsIgnoreCase(name)) {
            getTiles();
            return zoneList.clone();
        } else {
            return super.getProperty(name);
        }
    }

}
