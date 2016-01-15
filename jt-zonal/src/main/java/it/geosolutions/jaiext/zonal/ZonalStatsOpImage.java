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

import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.Range.DataType;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.stats.Statistics.StatsType;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.RenderedOp;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
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
    private AtomicBoolean firstTime = new AtomicBoolean(true);

    /** Spatial index for fast accessing the geometries that contain the selected pixel */
    private final STRtree spatialIndex = new STRtree();

    /** Boolean indicating if the classifier is present */
    private final boolean classPresent;

    /** Affine transformation for mapping the source image pixels to the classifiers pixels */
    private AffineTransform inverseTrans;

    /** Boolean indicating if a NoData Range is not used */
    private final boolean notHasNoData;

    /** Range object used for checking if a pixel is or not a NoData */
    private final Range noData;

    /** Boolean lookuptable used if no data are present */
    private final boolean[] booleanLookupTable;

    /** Rectangle containing the union of all the geometries */
    private Rectangle union;

    /** Source image bounds */
    private Rectangle sourceBounds;

    /** Classifier image bounds */
    private Rectangle classBounds;

    /** Random Iterator on the classifier image, if transformation is present */
    private RandomIter randomIterator;

    /** Array indicating the source image selected bands */
    private int[] bands;

    /** Band array length */
    private int bandNum;

    /** List of all the input geometries */
    private List<ROI> rois;

    /** Boolean indicating if the eventual transformation is not an Identity */
    private boolean isNotIdentity;

    /** Boolean indicating if the eventual rectIterator needs to be updated */
    private final boolean updateIterator;

    /** Classifier image */
    private final RenderedImage classifier;

    private List<Range> rangeList;

    private final boolean ranges;

    private final boolean localStats;

    private Range rangeHelper;

    private final boolean rangesNoClass;

    private boolean hasROI;

    // private RandomIter roiIter;

    // private Rectangle roiBounds;

    private boolean useROIAccessor;

    private boolean caseA;

    private boolean caseB;

    private boolean caseC;

    private PlanarImage srcROIImage;

    private ROI srcROI;

    private List<ZoneGeometry> zoneList;

    private RenderedOp srcROIImgExt;

    public ZonalStatsOpImage(RenderedImage source, ImageLayout layout, Map configuration,
            RenderedImage classifier, AffineTransform transform, List<ROI> rois, Range noData,
            ROI mask, boolean useROIAccessor, int[] bands, StatsType[] statsTypes,
            double[] minBound, double[] maxBound, int[] numBins, List<Range> rangeData,
            boolean localStats) {
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
            this.classifier = classifier;
        } else {
            this.classifier = null;
        }

        // Calculation of the inverse transformation
        classBounds = null;
        isNotIdentity = false;
        if (classPresent) {
            // source image bounds
            sourceBounds = createBounds(source);
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
            isNotIdentity = !inverseTrans.isIdentity();

            // Iterator on the classifier image bounds
            if (isNotIdentity) {
                randomIterator = RandomIterFactory.create(classifier, classBounds, false, true);
            }

            updateIterator = classPresent && !isNotIdentity;
        } else {
            updateIterator = false;
        }

        // Band selection
        this.bands = bands;
        this.bandNum = bands.length;
        // Control on the bands number
        SampleModel sm = source.getSampleModel();
        // source data type
        int dataType = sm.getDataType();
        // Number of source bands
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

        double[] minBounds = null;
        double[] maxBounds = null;
        int[] numBinss = null;

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

        if (!nullCondition) {
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
        }

        // If Range list is present then it is saved else a full Range is used

        Range fullRange = RangeFactory.create(Double.NEGATIVE_INFINITY, true,
                Double.POSITIVE_INFINITY, true, false);
        List<Range> simpleRange = new ArrayList<Range>(1);
        simpleRange.add(fullRange);

        if (rangeData != null && !rangeData.isEmpty()) {

            for (Range r : rangeData) {
                DataType type = r.getDataType();
                if (type.getDataType() != dataType) {
                    throw new IllegalArgumentException("Wrong Range data type");
                }
            }

            this.rangeList = Collections.unmodifiableList(rangeData);

            this.ranges = true;
            this.localStats = localStats;

            if (!localStats) {
                this.rangeHelper = fullRange;
            }
        } else {
            this.rangeHelper = fullRange;
            this.ranges = false;
            this.localStats = false;
        }

        rangesNoClass = ranges && !classPresent;

        // Creation of a ZoneGeometry list, for storing the results
        // Check if the rois are present. Otherwise the entire image statistics
        // are calculated
        if (rois == null) {

            this.zoneList = new ArrayList<ZoneGeometry>(1);

            this.rois = new ArrayList<ROI>();
            if (sourceBounds == null) {
                sourceBounds = createBounds(source);
            }
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

            // Creation of a new ZoneGeometry
            ZoneGeometry geom;
            if (ranges && localStats) {
                geom = new ZoneGeometry(roi, rangeList, bands, statsTypes, classPresent, minBounds,
                        maxBounds, numBinss);
            } else {

                geom = new ZoneGeometry(roi, simpleRange, bands, statsTypes, classPresent,
                        minBounds, maxBounds, numBinss);
            }
            // Addition to the geometries list
            spatialIndex.insert(env, geom);

            zoneList.add(geom);

        } else {
            // Bounds Union
            union = new Rectangle(rois.get(0).getBounds());

            this.zoneList = new ArrayList<ZoneGeometry>(rois.size());
            // Insertion of the zones to the spatial index and union of the bounds for every ROI/Zone object
            for (ROI roi : rois) {
                // Spatial index creation
                Rectangle rect = roi.getBounds();
                double minX = rect.getMinX();
                double maxX = rect.getMaxX();
                double minY = rect.getMinY();
                double maxY = rect.getMaxY();
                Envelope env = new Envelope(minX, maxX, minY, maxY);
                // Union
                union = union.union(rect);
                // Creation of a new ZoneGeometry
                ZoneGeometry geom;
                if (ranges && localStats) {
                    geom = new ZoneGeometry(roi, rangeList, bands, statsTypes, classPresent,
                            minBounds, maxBounds, numBinss);
                } else {

                    geom = new ZoneGeometry(roi, simpleRange, bands, statsTypes, classPresent,
                            minBounds, maxBounds, numBinss);
                }
                // Addition to the geometries list
                spatialIndex.insert(env, geom);

                zoneList.add(geom);
            }
            // Sets of the roi list
            this.rois = rois;
        }

        // Building of the spatial index
        // Coordinate object creation for the spatial indexing
        Coordinate p1 = new Coordinate(0, 0);
        // Envelope associated to the coordinate object
        Envelope searchEnv = new Envelope(p1);
        // Query on the geometry list
        spatialIndex.query(searchEnv);

        // Check if No Data control must be done
        if (noData != null) {
            notHasNoData = false;
            this.noData = noData;
        } else {
            notHasNoData = true;
            this.noData = null;
        }

        // Creation of a boolean lookuptable indicating if the selected pixel is a NoData
        // used only for byte images.
        if (!notHasNoData && source.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE) {
            booleanLookupTable = new boolean[256];
            for (int i = 0; i < booleanLookupTable.length; i++) {
                byte value = (byte) i;
                booleanLookupTable[i] = !noData.contains(value);
            }
        } else {
            booleanLookupTable = null;
        }

        // Check if ROI control must be done
        if (mask != null) {
            hasROI = true;
            // Roi object
            srcROI = mask;
            // The useRoiAccessor parameter is set
            this.useROIAccessor = useROIAccessor;
            if (useROIAccessor) {
                // Creation of a PlanarImage containing the ROI data
                srcROIImage = srcROI.getAsImage();
                // Source Bounds
                Rectangle srcRect = new Rectangle(source.getMinX(), source.getMinY(),
                        source.getWidth(), source.getHeight());
                // Padding of the input ROI image in order to avoid the call of the getExtendedData() method
                // ROI bounds are saved 
                Rectangle roiBounds = srcROIImage.getBounds();
                int deltaX0 = (roiBounds.x - srcRect.x);
                int leftP = deltaX0 > 0 ? deltaX0 : 0;
                int deltaY0 = (roiBounds.y - srcRect.y);
                int topP = deltaY0 > 0 ? deltaY0 : 0;
                int deltaX1 = (srcRect.x + srcRect.width - roiBounds.x + roiBounds.width);
                int rightP = deltaX1 > 0 ? deltaX1 : 0;
                int deltaY1 = (srcRect.y + srcRect.height - roiBounds.y + roiBounds.height);
                int bottomP = deltaY1 > 0 ? deltaY1 : 0;
                // Extend the ROI image
                ParameterBlock pb = new ParameterBlock();
                pb.setSource(srcROIImage, 0);
                pb.set(leftP, 0);
                pb.set(rightP, 1);
                pb.set(topP, 2);
                pb.set(bottomP, 3);
                pb.set(ROI_EXTENDER, 4);
                srcROIImgExt = JAI.create("border", pb);
            }
        } else {
            hasROI = false;
            this.useROIAccessor = false;
            srcROIImage = null;
        }

        // Definition of the possible cases that can be found
        // caseA = no ROI nor No Data
        // caseB = ROI present but No Data not present
        // caseC = No Data present but ROI not present
        // Last case not defined = both ROI and No Data are present
        caseA = notHasNoData && !hasROI;
        caseB = notHasNoData && hasROI;
        caseC = !notHasNoData && !hasROI;
    }

    /**
     * Creates a rectangle from the bounds of the specified image. 
     */
    private Rectangle createBounds(RenderedImage source) {
        return new Rectangle(source.getMinX(), source.getMinY(), source.getWidth(), source.getHeight());
    }

    public Raster computeTile(int tileX, int tileY) {
        // Selection of the tile associated with the tile x and y indexes
        Raster tile = getSourceImage(0).getTile(tileX, tileY);
        // Selection of the tile bounds
        Rectangle tileRect = tile.getBounds();
        // Boolean indicating if the tile is inside the ROI
        boolean insideROIifPresent = true;
        synchronized (this) {
            insideROIifPresent = (hasROI && srcROI.intersects(tileRect) || !hasROI);
        }
        // Check if the tile is inside the geometry bound-union
        if (union.intersects(tileRect) && insideROIifPresent) {
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
                // Note that the getExtendedData() method is not called because the input images are padded.
                // For each image there is a check if the rectangle is contained inside the source image;
                // if this not happen, the data is taken from the padded image.
                Raster roiRaster = null;
                if(srcROIImage.getBounds().contains(computableArea)){
                    roiRaster = srcROIImage.getData(computableArea);
                }else{
                    roiRaster = srcROIImgExt.getData(computableArea);
                }

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
                byteLoop(src, computableArea, tileX, tileY, roi);
                break;
            case DataBuffer.TYPE_USHORT:
                ushortLoop(src, computableArea, tileX, tileY, roi);
                break;
            case DataBuffer.TYPE_SHORT:
                shortLoop(src, computableArea, tileX, tileY, roi);
                break;
            case DataBuffer.TYPE_INT:
                intLoop(src, computableArea, tileX, tileY, roi);
                break;
            case DataBuffer.TYPE_FLOAT:
                floatLoop(src, computableArea, tileX, tileY, roi);
                break;
            case DataBuffer.TYPE_DOUBLE:
                doubleLoop(src, computableArea, tileX, tileY, roi);
                break;
            default:
                throw new IllegalArgumentException("Wrong data type");
            }
        }

        return tile;
    }

    // NOTE: the statistic calculation is done in a synchronized block for avoiding race conditions
    private void byteLoop(RasterAccessor src, Rectangle computableArea, int tileX, int tileY,
            RasterAccessor roi) {

        // Source RasterAccessor initial parameters
        final int srcX = src.getX();
        final int srcY = src.getY();

        byte srcData[][] = src.getByteDataArrays();

        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        RectIter rectIterator = null;

        if (updateIterator) {
            Raster ras = classifier.getTile(tileX, tileY);
            rectIterator = RectIterFactory.create(ras, computableArea);
            rectIterator.startBands();
            rectIterator.startLines();
        }

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

        // NO DATA AND ROI NOT PRESENT
        if (caseA) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y++) {
                if (updateIterator) {
                    rectIterator.startPixels();
                }
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x++) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;

                    // PixelPositions
                    int x0 = srcX + x;
                    int y0 = srcY + y;

                    // check on containment
                    if (!union.contains(x0, y0)) {
                        // Update of the RectIterator
                        if (updateIterator) {
                            rectIterator.nextPixel();
                        }
                        continue;
                    }

                    // Coordinate object creation for the spatial indexing
                    Coordinate p1 = new Coordinate(x0, y0);
                    // Envelope associated to the coordinate object
                    Envelope searchEnv = new Envelope(p1);
                    // Query on the geometry list
                    List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                            if (isNotIdentity) {
                                inverseTrans.inverseTransform(pointSrc, pointClass);
                                // Selection of the classId point
                                classId = randomIterator.getSample(pointClass.x, pointClass.y, 0);
                            } else {
                                // Selection of the classId point
                                classId = rectIterator.getSample();
                                rectIterator.nextPixel();
                            }

                        } catch (NoninvertibleTransformException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }
                    }
                    // Cycle on all the geometries found
                    for (ZoneGeometry zoneGeo : geomList) {

                        ROI geometry = zoneGeo.getROI();
                        // if every geometry really contains the selected point
                        boolean contains = false;
                        synchronized (zoneGeo) { // HACK
                            contains = geometry.contains(x0, y0);
                        }

                        if (contains) {
                            // Cycle on the selected Bands
                            for (int i = 0; i < bandNum; i++) {
                                byte value = srcData[bands[i]][posx + posy
                                                               + srcBandOffsets[bands[i]]];
                                
                                int sample = value&0xFF;
                                // Update of all the statistics
                                // If a range list is present then the sample is checked if it is inside the range
                                if (rangesNoClass) {
                                    for (Range range : rangeList) {
                                        if (range.contains(value)) {
                                            // For local statistics the pixel is checked for every range
                                            if (localStats) {
                                                zoneGeo.add(sample, bands[i], classId, range);
                                            } else {
                                                // For non local statistics the pixel when the pixel is contained inside a singular range
                                                // it is added to the statistic container
                                                zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                                break;
                                            }
                                        }
                                    }
                                } else {
                                    zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                }
                            }
                        }
                    }
                }
                if (updateIterator) {
                    rectIterator.nextLine();
                }
            }

            // ONLY ROI PRESENT
        } else if (caseB) {
            if (useROIAccessor) {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    if (updateIterator) {
                        rectIterator.startPixels();
                    }
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // roi y position
                    int posYroi = y * roiScanLineStride;

                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;

                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;

                        // check on containment
                        if (!union.contains(x0, y0)) {
                            // Update of the RectIterator
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // ROI index position
                        int windex = x + posYroi;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                        if (w == 0) {
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // Coordinate object creation for the spatial indexing
                        Coordinate p1 = new Coordinate(x0, y0);
                        // Envelope associated to the coordinate object
                        Envelope searchEnv = new Envelope(p1);
                        // Query on the geometry list
                        List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                                if (isNotIdentity) {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);
                                    // Selection of the classId point
                                    classId = randomIterator.getSample(pointClass.x, pointClass.y,
                                            0);
                                } else {
                                    // Selection of the classId point
                                    classId = rectIterator.getSample();
                                    rectIterator.nextPixel();
                                }

                            } catch (NoninvertibleTransformException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                        // Cycle on all the geometries found
                        for (ZoneGeometry zoneGeo : geomList) {

                            ROI geometry = zoneGeo.getROI();
                            // if every geometry really contains the selected point
                            boolean contains = false;
                            synchronized (zoneGeo) { // HACK
                                contains = geometry.contains(x0, y0);
                            }

                            if (contains) {
                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    int sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]]&0xFF;
                                    // Update of all the statistics
                                    // If a range list is present then the sample is checked if it is inside the range
                                    if (rangesNoClass) {
                                        for (Range range : rangeList) {
                                            if (range.contains((byte)sample)) {
                                                // For local statistics the pixel is checked for every range
                                                if (localStats) {
                                                    zoneGeo.add(sample, bands[i], classId, range);
                                                } else {
                                                    // For non local statistics the pixel when the pixel is contained inside a singular range
                                                    // it is added to the statistic container
                                                    zoneGeo.add(sample, bands[i], classId,
                                                            rangeHelper);
                                                    break;
                                                }
                                            }
                                        }
                                    } else {
                                        zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                    }
                                }
                            }
                        }
                    }
                    if (updateIterator) {
                        rectIterator.nextLine();
                    }
                }
            } else {

                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    if (updateIterator) {
                        rectIterator.startPixels();
                    }
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;

                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;

                        // check on containment
                        if (!union.contains(x0, y0)) {
                            // Update of the RectIterator
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // ROI value
                        boolean insideROI = false;
                        synchronized (this) { // HACK
                            insideROI = srcROI.contains(x0, y0);
                        }

                        if (!insideROI) {
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // Coordinate object creation for the spatial indexing
                        Coordinate p1 = new Coordinate(x0, y0);
                        // Envelope associated to the coordinate object
                        Envelope searchEnv = new Envelope(p1);
                        // Query on the geometry list
                        List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                                if (isNotIdentity) {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);
                                    // Selection of the classId point
                                    classId = randomIterator.getSample(pointClass.x, pointClass.y,
                                            0);
                                } else {
                                    // Selection of the classId point
                                    classId = rectIterator.getSample();
                                    rectIterator.nextPixel();
                                }

                            } catch (NoninvertibleTransformException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                        // Cycle on all the geometries found
                        for (ZoneGeometry zoneGeo : geomList) {

                            ROI geometry = zoneGeo.getROI();
                            // if every geometry really contains the selected point
                            boolean contains = false;
                            synchronized (zoneGeo) { // HACK
                                contains = geometry.contains(x0, y0);
                            }

                            if (contains) {
                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    int sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]]&0xFF;
                                    // Update of all the statistics
                                    // If a range list is present then the sample is checked if it is inside the range
                                    if (rangesNoClass) {
                                        for (Range range : rangeList) {
                                            if (range.contains((byte)sample)) {
                                                // For local statistics the pixel is checked for every range
                                                if (localStats) {
                                                    zoneGeo.add(sample, bands[i], classId, range);
                                                } else {
                                                    // For non local statistics the pixel when the pixel is contained inside a singular range
                                                    // it is added to the statistic container
                                                    zoneGeo.add(sample, bands[i], classId,
                                                            rangeHelper);
                                                    break;
                                                }
                                            }
                                        }
                                    } else {
                                        zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                    }
                                }
                            }
                        }
                    }
                    if (updateIterator) {
                        rectIterator.nextLine();
                    }
                }
            }
            // ONLY NO DATA PRESENT
        } else if (caseC) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y++) {
                if (updateIterator) {
                    rectIterator.startPixels();
                }
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x++) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;
                    // PixelPositions
                    int x0 = srcX + x;
                    int y0 = srcY + y;

                    // check on containment
                    if (!union.contains(x0, y0)) {
                        // Update of the RectIterator
                        if (updateIterator) {
                            rectIterator.nextPixel();
                        }
                        continue;
                    }

                    // Coordinate object creation for the spatial indexing
                    Coordinate p1 = new Coordinate(x0, y0);
                    // Envelope associated to the coordinate object
                    Envelope searchEnv = new Envelope(p1);
                    // Query on the geometry list
                    List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                            if (isNotIdentity) {
                                inverseTrans.inverseTransform(pointSrc, pointClass);
                                // Selection of the classId point
                                classId = randomIterator.getSample(pointClass.x, pointClass.y, 0);
                            } else {
                                // Selection of the classId point
                                classId = rectIterator.getSample();
                                rectIterator.nextPixel();
                            }

                        } catch (NoninvertibleTransformException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }
                    }
                    // Cycle on all the geometries found
                    for (ZoneGeometry zoneGeo : geomList) {

                        ROI geometry = zoneGeo.getROI();

                        // if every geometry really contains the selected point
                        boolean contains = false;
                        synchronized (zoneGeo) { // HACK
                            contains = geometry.contains(x0, y0);
                        }
                        if (contains) {

                            // Cycle on the selected Bands
                            for (int i = 0; i < bandNum; i++) {
                                int sample = srcData[bands[i]][posx + posy
                                        + srcBandOffsets[bands[i]]]&0xFF;
                                // NoData check
                                if (booleanLookupTable[sample]) {
                                    // Update of all the statistics
                                    // If a range list is present then the sample is checked if it is inside the range
                                    if (rangesNoClass) {
                                        for (Range range : rangeList) {
                                            if (range.contains((byte)sample)) {
                                                // For local statistics the pixel is checked for every range
                                                if (localStats) {
                                                    zoneGeo.add(sample, bands[i], classId, range);
                                                } else {
                                                    // For non local statistics the pixel when the pixel is contained inside a singular range
                                                    // it is added to the statistic container
                                                    zoneGeo.add(sample, bands[i], classId,
                                                            rangeHelper);
                                                    break;
                                                }
                                            }
                                        }
                                    } else {
                                        zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                    }
                                }
                            }

                        }
                    }
                }
                if (updateIterator) {
                    rectIterator.nextLine();
                }
            }
            // ROI AND NO DATA ARE PRESENT
        } else {
            if (useROIAccessor) {

                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    if (updateIterator) {
                        rectIterator.startPixels();
                    }
                    // y position on the source data array
                    int posy = y * srcScanlineStride;

                    // roi y position
                    int posYroi = y * roiScanLineStride;

                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;

                        // check on containment
                        if (!union.contains(x0, y0)) {
                            // Update of the RectIterator
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // ROI index position
                        int windex = x + posYroi;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                        if (w == 0) {
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // Coordinate object creation for the spatial indexing
                        Coordinate p1 = new Coordinate(x0, y0);
                        // Envelope associated to the coordinate object
                        Envelope searchEnv = new Envelope(p1);
                        // Query on the geometry list
                        List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                                if (isNotIdentity) {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);
                                    // Selection of the classId point
                                    classId = randomIterator.getSample(pointClass.x, pointClass.y,
                                            0);
                                } else {
                                    // Selection of the classId point
                                    classId = rectIterator.getSample();
                                    rectIterator.nextPixel();
                                }

                            } catch (NoninvertibleTransformException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                        // Cycle on all the geometries found
                        for (ZoneGeometry zoneGeo : geomList) {

                            ROI geometry = zoneGeo.getROI();

                            // if every geometry really contains the selected point
                            boolean contains = false;
                            synchronized (zoneGeo) { // HACK
                                contains = geometry.contains(x0, y0);
                            }
                            if (contains) {

                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    int sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]]& 0xFF;
                                    // NoData check
                                    if (booleanLookupTable[sample ]) {
                                        // Update of all the statistics
                                        // If a range list is present then the sample is checked if it is inside the range
                                        if (rangesNoClass) {
                                            for (Range range : rangeList) {
                                                if (range.contains((byte)sample)) {
                                                    // For local statistics the pixel is checked for every range
                                                    if (localStats) {
                                                        zoneGeo.add(sample, bands[i], classId,
                                                                range);
                                                    } else {
                                                        // For non local statistics the pixel when the pixel is contained inside a singular range
                                                        // it is added to the statistic container
                                                        zoneGeo.add(sample, bands[i], classId,
                                                                rangeHelper);
                                                        break;
                                                    }
                                                }
                                            }
                                        } else {
                                            zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                        }
                                    }
                                }

                            }
                        }
                    }
                    if (updateIterator) {
                        rectIterator.nextLine();
                    }
                }
            } else {

                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    if (updateIterator) {
                        rectIterator.startPixels();
                    }
                    // y position on the source data array
                    int posy = y * srcScanlineStride;

                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;

                        // check on containment
                        if (!union.contains(x0, y0)) {
                            // Update of the RectIterator
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // ROI value
                        boolean insideROI = false;
                        synchronized (this) { // HACK
                            insideROI = srcROI.contains(x0, y0);
                        }
                        if (!insideROI) {
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // Coordinate object creation for the spatial indexing
                        Coordinate p1 = new Coordinate(x0, y0);
                        // Envelope associated to the coordinate object
                        Envelope searchEnv = new Envelope(p1);
                        // Query on the geometry list
                        List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                                if (isNotIdentity) {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);
                                    // Selection of the classId point
                                    classId = randomIterator.getSample(pointClass.x, pointClass.y,
                                            0);
                                } else {
                                    // Selection of the classId point
                                    classId = rectIterator.getSample();
                                    rectIterator.nextPixel();
                                }

                            } catch (NoninvertibleTransformException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                        // Cycle on all the geometries found
                        for (ZoneGeometry zoneGeo : geomList) {

                            ROI geometry = zoneGeo.getROI();

                            // if every geometry really contains the selected point
                            boolean contains = false;
                            synchronized (zoneGeo) { // HACK
                                contains = geometry.contains(x0, y0);
                            }
                            if (contains) {

                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    int sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]]& 0xFF;
                                    // NoData check
                                    if (booleanLookupTable[sample ]) {
                                        // Update of all the statistics
                                        // If a range list is present then the sample is checked if it is inside the range
                                        if (rangesNoClass) {
                                            for (Range range : rangeList) {
                                                if (range.contains((byte)sample)) {
                                                    // For local statistics the pixel is checked for every range
                                                    if (localStats) {
                                                        zoneGeo.add(sample, bands[i], classId,
                                                                range);
                                                    } else {
                                                        // For non local statistics the pixel when the pixel is contained inside a singular range
                                                        // it is added to the statistic container
                                                        zoneGeo.add(sample, bands[i], classId,
                                                                rangeHelper);
                                                        break;
                                                    }
                                                }
                                            }
                                        } else {
                                            zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                        }
                                    }
                                }

                            }
                        }
                    }
                    if (updateIterator) {
                        rectIterator.nextLine();
                    }
                }
            }
        }
    }

    private void ushortLoop(RasterAccessor src, Rectangle computableArea, int tileX, int tileY,
            RasterAccessor roi) {

        // Source RasterAccessor initial parameters
        final int srcX = src.getX();
        final int srcY = src.getY();

        short srcData[][] = src.getShortDataArrays();

        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        RectIter rectIterator = null;

        if (updateIterator) {
            Raster ras = classifier.getTile(tileX, tileY);
            rectIterator = RectIterFactory.create(ras, computableArea);
            rectIterator.startBands();
            rectIterator.startLines();
        }

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

        // NO DATA AND ROI NOT PRESENT
        if (caseA) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y++) {
                if (updateIterator) {
                    rectIterator.startPixels();
                }
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x++) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;

                    // PixelPositions
                    int x0 = srcX + x;
                    int y0 = srcY + y;

                    // check on containment
                    if (!union.contains(x0, y0)) {
                        // Update of the RectIterator
                        if (updateIterator) {
                            rectIterator.nextPixel();
                        }
                        continue;
                    }

                    // Coordinate object creation for the spatial indexing
                    Coordinate p1 = new Coordinate(x0, y0);
                    // Envelope associated to the coordinate object
                    Envelope searchEnv = new Envelope(p1);
                    // Query on the geometry list
                    List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                            if (isNotIdentity) {
                                inverseTrans.inverseTransform(pointSrc, pointClass);
                                // Selection of the classId point
                                classId = randomIterator.getSample(pointClass.x, pointClass.y, 0);
                            } else {
                                // Selection of the classId point
                                classId = rectIterator.getSample();
                                rectIterator.nextPixel();
                            }

                        } catch (NoninvertibleTransformException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }
                    }
                    // Cycle on all the geometries found
                    for (ZoneGeometry zoneGeo : geomList) {

                        ROI geometry = zoneGeo.getROI();
                        // if every geometry really contains the selected point
                        boolean contains = false;
                        synchronized (zoneGeo) { // HACK
                            contains = geometry.contains(x0, y0);
                        }
                        if (contains) {
                            // Cycle on the selected Bands
                            for (int i = 0; i < bandNum; i++) {
                                int sample = srcData[bands[i]][posx + posy
                                        + srcBandOffsets[bands[i]]] & 0xFFFF;
                                // Update of all the statistics
                                // If a range list is present then the sample is checked if it is inside the range
                                if (rangesNoClass) {
                                    for (Range range : rangeList) {
                                        if (range.contains((short) sample)) {
                                            // For local statistics the pixel is checked for every range
                                            if (localStats) {
                                                zoneGeo.add(sample, bands[i], classId, range);
                                            } else {
                                                // For non local statistics the pixel when the pixel is contained inside a singular range
                                                // it is added to the statistic container
                                                zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                                break;
                                            }
                                        }
                                    }
                                } else {
                                    zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                }
                            }
                        }
                    }
                }
                if (updateIterator) {
                    rectIterator.nextLine();
                }
            }

            // ONLY ROI PRESENT
        } else if (caseB) {
            if (useROIAccessor) {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    if (updateIterator) {
                        rectIterator.startPixels();
                    }
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // roi y position
                    int posYroi = y * roiScanLineStride;

                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;

                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;

                        // check on containment
                        if (!union.contains(x0, y0)) {
                            // Update of the RectIterator
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // ROI index position
                        int windex = x + posYroi;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                        if (w == 0) {
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // Coordinate object creation for the spatial indexing
                        Coordinate p1 = new Coordinate(x0, y0);
                        // Envelope associated to the coordinate object
                        Envelope searchEnv = new Envelope(p1);
                        // Query on the geometry list
                        List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                                if (isNotIdentity) {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);
                                    // Selection of the classId point
                                    classId = randomIterator.getSample(pointClass.x, pointClass.y,
                                            0);
                                } else {
                                    // Selection of the classId point
                                    classId = rectIterator.getSample();
                                    rectIterator.nextPixel();
                                }

                            } catch (NoninvertibleTransformException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                        // Cycle on all the geometries found
                        for (ZoneGeometry zoneGeo : geomList) {

                            ROI geometry = zoneGeo.getROI();
                            // if every geometry really contains the selected point
                            boolean contains = false;
                            synchronized (zoneGeo) { // HACK
                                contains = geometry.contains(x0, y0);
                            }
                            if (contains) {
                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    int sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]] & 0xFFFF;
                                    // Update of all the statistics
                                    // If a range list is present then the sample is checked if it is inside the range
                                    if (rangesNoClass) {
                                        for (Range range : rangeList) {
                                            if (range.contains((short) sample)) {
                                                // For local statistics the pixel is checked for every range
                                                if (localStats) {
                                                    zoneGeo.add(sample, bands[i], classId, range);
                                                } else {
                                                    // For non local statistics the pixel when the pixel is contained inside a singular range
                                                    // it is added to the statistic container
                                                    zoneGeo.add(sample, bands[i], classId,
                                                            rangeHelper);
                                                    break;
                                                }
                                            }
                                        }
                                    } else {
                                        zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                    }
                                }
                            }
                        }
                    }
                    if (updateIterator) {
                        rectIterator.nextLine();
                    }
                }
            } else {

                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    if (updateIterator) {
                        rectIterator.startPixels();
                    }
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;

                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;

                        // check on containment
                        if (!union.contains(x0, y0)) {
                            // Update of the RectIterator
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // ROI value
                        boolean insideROI = false;
                        synchronized (this) { // HACK
                            insideROI = srcROI.contains(x0, y0);
                        }
                        if (!insideROI) {
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // Coordinate object creation for the spatial indexing
                        Coordinate p1 = new Coordinate(x0, y0);
                        // Envelope associated to the coordinate object
                        Envelope searchEnv = new Envelope(p1);
                        // Query on the geometry list
                        List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                                if (isNotIdentity) {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);
                                    // Selection of the classId point
                                    classId = randomIterator.getSample(pointClass.x, pointClass.y,
                                            0);
                                } else {
                                    // Selection of the classId point
                                    classId = rectIterator.getSample();
                                    rectIterator.nextPixel();
                                }

                            } catch (NoninvertibleTransformException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                        // Cycle on all the geometries found
                        for (ZoneGeometry zoneGeo : geomList) {

                            ROI geometry = zoneGeo.getROI();
                            // if every geometry really contains the selected point
                            boolean contains = false;
                            synchronized (zoneGeo) { // HACK
                                contains = geometry.contains(x0, y0);
                            }
                            if (contains) {
                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    int sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]] & 0xFFFF;
                                    // Update of all the statistics
                                    // If a range list is present then the sample is checked if it is inside the range
                                    if (rangesNoClass) {
                                        for (Range range : rangeList) {
                                            if (range.contains((short) sample)) {
                                                // For local statistics the pixel is checked for every range
                                                if (localStats) {
                                                    zoneGeo.add(sample, bands[i], classId, range);
                                                } else {
                                                    // For non local statistics the pixel when the pixel is contained inside a singular range
                                                    // it is added to the statistic container
                                                    zoneGeo.add(sample, bands[i], classId,
                                                            rangeHelper);
                                                    break;
                                                }
                                            }
                                        }
                                    } else {
                                        zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                    }
                                }
                            }
                        }
                    }
                    if (updateIterator) {
                        rectIterator.nextLine();
                    }
                }
            }
            // ONLY NO DATA PRESENT
        } else if (caseC) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y++) {
                if (updateIterator) {
                    rectIterator.startPixels();
                }
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x++) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;
                    // PixelPositions
                    int x0 = srcX + x;
                    int y0 = srcY + y;

                    // check on containment
                    if (!union.contains(x0, y0)) {
                        // Update of the RectIterator
                        if (updateIterator) {
                            rectIterator.nextPixel();
                        }
                        continue;
                    }

                    // Coordinate object creation for the spatial indexing
                    Coordinate p1 = new Coordinate(x0, y0);
                    // Envelope associated to the coordinate object
                    Envelope searchEnv = new Envelope(p1);
                    // Query on the geometry list
                    List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                            if (isNotIdentity) {
                                inverseTrans.inverseTransform(pointSrc, pointClass);
                                // Selection of the classId point
                                classId = randomIterator.getSample(pointClass.x, pointClass.y, 0);
                            } else {
                                // Selection of the classId point
                                classId = rectIterator.getSample();
                                rectIterator.nextPixel();
                            }

                        } catch (NoninvertibleTransformException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }
                    }
                    // Cycle on all the geometries found
                    for (ZoneGeometry zoneGeo : geomList) {

                        ROI geometry = zoneGeo.getROI();

                        // if every geometry really contains the selected point
                        boolean contains = false;
                        synchronized (zoneGeo) { // HACK
                            contains = geometry.contains(x0, y0);
                        }
                        if (contains) {

                            // Cycle on the selected Bands
                            for (int i = 0; i < bandNum; i++) {
                                int sample = srcData[bands[i]][posx + posy
                                        + srcBandOffsets[bands[i]]] & 0xFFFF;
                                // NoData check
                                if (!noData.contains((short) sample)) {
                                    // Update of all the statistics
                                    // If a range list is present then the sample is checked if it is inside the range
                                    if (rangesNoClass) {
                                        for (Range range : rangeList) {
                                            if (range.contains((short) sample)) {
                                                // For local statistics the pixel is checked for every range
                                                if (localStats) {
                                                    zoneGeo.add(sample, bands[i], classId, range);
                                                } else {
                                                    // For non local statistics the pixel when the pixel is contained inside a singular range
                                                    // it is added to the statistic container
                                                    zoneGeo.add(sample, bands[i], classId,
                                                            rangeHelper);
                                                    break;
                                                }
                                            }
                                        }
                                    } else {
                                        zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                    }
                                }
                            }

                        }
                    }
                }
                if (updateIterator) {
                    rectIterator.nextLine();
                }
            }
            // ROI AND NO DATA ARE PRESENT
        } else {
            if (useROIAccessor) {

                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    if (updateIterator) {
                        rectIterator.startPixels();
                    }
                    // y position on the source data array
                    int posy = y * srcScanlineStride;

                    // roi y position
                    int posYroi = y * roiScanLineStride;

                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;

                        // check on containment
                        if (!union.contains(x0, y0)) {
                            // Update of the RectIterator
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // ROI index position
                        int windex = x + posYroi;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                        if (w == 0) {
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // Coordinate object creation for the spatial indexing
                        Coordinate p1 = new Coordinate(x0, y0);
                        // Envelope associated to the coordinate object
                        Envelope searchEnv = new Envelope(p1);
                        // Query on the geometry list
                        List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                                if (isNotIdentity) {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);
                                    // Selection of the classId point
                                    classId = randomIterator.getSample(pointClass.x, pointClass.y,
                                            0);
                                } else {
                                    // Selection of the classId point
                                    classId = rectIterator.getSample();
                                    rectIterator.nextPixel();
                                }

                            } catch (NoninvertibleTransformException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                        // Cycle on all the geometries found
                        for (ZoneGeometry zoneGeo : geomList) {

                            ROI geometry = zoneGeo.getROI();

                            // if every geometry really contains the selected point
                            boolean contains = false;
                            synchronized (zoneGeo) { // HACK
                                contains = geometry.contains(x0, y0);
                            }
                            if (contains) {

                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    int sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]] & 0xFFFF;
                                    // NoData check
                                    if (!noData.contains((short) sample)) {
                                        // Update of all the statistics
                                        // If a range list is present then the sample is checked if it is inside the range
                                        if (rangesNoClass) {
                                            for (Range range : rangeList) {
                                                if (range.contains((short) sample)) {
                                                    // For local statistics the pixel is checked for every range
                                                    if (localStats) {
                                                        zoneGeo.add(sample, bands[i], classId,
                                                                range);
                                                    } else {
                                                        // For non local statistics the pixel when the pixel is contained inside a singular range
                                                        // it is added to the statistic container
                                                        zoneGeo.add(sample, bands[i], classId,
                                                                rangeHelper);
                                                        break;
                                                    }
                                                }
                                            }
                                        } else {
                                            zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                        }
                                    }
                                }

                            }
                        }
                    }
                    if (updateIterator) {
                        rectIterator.nextLine();
                    }
                }
            } else {

                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    if (updateIterator) {
                        rectIterator.startPixels();
                    }
                    // y position on the source data array
                    int posy = y * srcScanlineStride;

                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;

                        // check on containment
                        if (!union.contains(x0, y0)) {
                            // Update of the RectIterator
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // ROI value
                        boolean insideROI = false;
                        synchronized (this) { // HACK
                            insideROI = srcROI.contains(x0, y0);
                        }
                        if (!insideROI) {
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // Coordinate object creation for the spatial indexing
                        Coordinate p1 = new Coordinate(x0, y0);
                        // Envelope associated to the coordinate object
                        Envelope searchEnv = new Envelope(p1);
                        // Query on the geometry list
                        List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                                if (isNotIdentity) {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);
                                    // Selection of the classId point
                                    classId = randomIterator.getSample(pointClass.x, pointClass.y,
                                            0);
                                } else {
                                    // Selection of the classId point
                                    classId = rectIterator.getSample();
                                    rectIterator.nextPixel();
                                }

                            } catch (NoninvertibleTransformException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                        // Cycle on all the geometries found
                        for (ZoneGeometry zoneGeo : geomList) {

                            ROI geometry = zoneGeo.getROI();

                            // if every geometry really contains the selected point
                            boolean contains = false;
                            synchronized (zoneGeo) { // HACK
                                contains = geometry.contains(x0, y0);
                            }
                            if (contains) {

                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    int sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]] & 0xFFFF;
                                    // NoData check
                                    if (!noData.contains((short) sample)) {
                                        // Update of all the statistics
                                        // If a range list is present then the sample is checked if it is inside the range
                                        if (rangesNoClass) {
                                            for (Range range : rangeList) {
                                                if (range.contains((short) sample)) {
                                                    // For local statistics the pixel is checked for every range
                                                    if (localStats) {
                                                        zoneGeo.add(sample, bands[i], classId,
                                                                range);
                                                    } else {
                                                        // For non local statistics the pixel when the pixel is contained inside a singular range
                                                        // it is added to the statistic container
                                                        zoneGeo.add(sample, bands[i], classId,
                                                                rangeHelper);
                                                        break;
                                                    }
                                                }
                                            }
                                        } else {
                                            zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                        }
                                    }
                                }

                            }
                        }
                    }
                    if (updateIterator) {
                        rectIterator.nextLine();
                    }
                }
            }
        }
    }

    private void shortLoop(RasterAccessor src, Rectangle computableArea, int tileX, int tileY,
            RasterAccessor roi) {

        // Source RasterAccessor initial parameters
        final int srcX = src.getX();
        final int srcY = src.getY();

        short srcData[][] = src.getShortDataArrays();

        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        RectIter rectIterator = null;

        if (updateIterator) {
            Raster ras = classifier.getTile(tileX, tileY);
            rectIterator = RectIterFactory.create(ras, computableArea);
            rectIterator.startBands();
            rectIterator.startLines();
        }

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

        // NO DATA AND ROI NOT PRESENT
        if (caseA) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y++) {
                if (updateIterator) {
                    rectIterator.startPixels();
                }
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x++) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;

                    // PixelPositions
                    int x0 = srcX + x;
                    int y0 = srcY + y;

                    // check on containment
                    if (!union.contains(x0, y0)) {
                        // Update of the RectIterator
                        if (updateIterator) {
                            rectIterator.nextPixel();
                        }
                        continue;
                    }

                    // Coordinate object creation for the spatial indexing
                    Coordinate p1 = new Coordinate(x0, y0);
                    // Envelope associated to the coordinate object
                    Envelope searchEnv = new Envelope(p1);
                    // Query on the geometry list
                    List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                            if (isNotIdentity) {
                                inverseTrans.inverseTransform(pointSrc, pointClass);
                                // Selection of the classId point
                                classId = randomIterator.getSample(pointClass.x, pointClass.y, 0);
                            } else {
                                // Selection of the classId point
                                classId = rectIterator.getSample();
                                rectIterator.nextPixel();
                            }

                        } catch (NoninvertibleTransformException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }
                    }
                    // Cycle on all the geometries found
                    for (ZoneGeometry zoneGeo : geomList) {

                        ROI geometry = zoneGeo.getROI();
                        // if every geometry really contains the selected point
                        boolean contains = false;
                        synchronized (zoneGeo) { // HACK
                            contains = geometry.contains(x0, y0);
                        }
                        if (contains) {
                            // Cycle on the selected Bands
                            for (int i = 0; i < bandNum; i++) {
                                short sample = srcData[bands[i]][posx + posy
                                        + srcBandOffsets[bands[i]]];
                                // Update of all the statistics
                                // If a range list is present then the sample is checked if it is inside the range
                                if (rangesNoClass) {
                                    for (Range range : rangeList) {
                                        if (range.contains(sample)) {
                                            // For local statistics the pixel is checked for every range
                                            if (localStats) {
                                                zoneGeo.add(sample, bands[i], classId, range);
                                            } else {
                                                // For non local statistics the pixel when the pixel is contained inside a singular range
                                                // it is added to the statistic container
                                                zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                                break;
                                            }
                                        }
                                    }
                                } else {
                                    zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                }
                            }
                        }
                    }
                }
                if (updateIterator) {
                    rectIterator.nextLine();
                }
            }

            // ONLY ROI PRESENT
        } else if (caseB) {
            if (useROIAccessor) {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    if (updateIterator) {
                        rectIterator.startPixels();
                    }
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // roi y position
                    int posYroi = y * roiScanLineStride;

                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;

                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;

                        // check on containment
                        if (!union.contains(x0, y0)) {
                            // Update of the RectIterator
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // ROI index position
                        int windex = x + posYroi;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                        if (w == 0) {
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // Coordinate object creation for the spatial indexing
                        Coordinate p1 = new Coordinate(x0, y0);
                        // Envelope associated to the coordinate object
                        Envelope searchEnv = new Envelope(p1);
                        // Query on the geometry list
                        List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                                if (isNotIdentity) {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);
                                    // Selection of the classId point
                                    classId = randomIterator.getSample(pointClass.x, pointClass.y,
                                            0);
                                } else {
                                    // Selection of the classId point
                                    classId = rectIterator.getSample();
                                    rectIterator.nextPixel();
                                }

                            } catch (NoninvertibleTransformException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                        // Cycle on all the geometries found
                        for (ZoneGeometry zoneGeo : geomList) {

                            ROI geometry = zoneGeo.getROI();
                            // if every geometry really contains the selected point
                            boolean contains = false;
                            synchronized (zoneGeo) { // HACK
                                contains = geometry.contains(x0, y0);
                            }
                            if (contains) {
                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    short sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // Update of all the statistics
                                    // If a range list is present then the sample is checked if it is inside the range
                                    if (rangesNoClass) {
                                        for (Range range : rangeList) {
                                            if (range.contains(sample)) {
                                                // For local statistics the pixel is checked for every range
                                                if (localStats) {
                                                    zoneGeo.add(sample, bands[i], classId, range);
                                                } else {
                                                    // For non local statistics the pixel when the pixel is contained inside a singular range
                                                    // it is added to the statistic container
                                                    zoneGeo.add(sample, bands[i], classId,
                                                            rangeHelper);
                                                    break;
                                                }
                                            }
                                        }
                                    } else {
                                        zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                    }
                                }
                            }
                        }
                    }
                    if (updateIterator) {
                        rectIterator.nextLine();
                    }
                }
            } else {

                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    if (updateIterator) {
                        rectIterator.startPixels();
                    }
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;

                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;

                        // check on containment
                        if (!union.contains(x0, y0)) {
                            // Update of the RectIterator
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // ROI value
                        boolean insideROI = false;
                        synchronized (this) { // HACK
                            insideROI = srcROI.contains(x0, y0);
                        }
                        if (!insideROI) {
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // Coordinate object creation for the spatial indexing
                        Coordinate p1 = new Coordinate(x0, y0);
                        // Envelope associated to the coordinate object
                        Envelope searchEnv = new Envelope(p1);
                        // Query on the geometry list
                        List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                                if (isNotIdentity) {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);
                                    // Selection of the classId point
                                    classId = randomIterator.getSample(pointClass.x, pointClass.y,
                                            0);
                                } else {
                                    // Selection of the classId point
                                    classId = rectIterator.getSample();
                                    rectIterator.nextPixel();
                                }

                            } catch (NoninvertibleTransformException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                        // Cycle on all the geometries found
                        for (ZoneGeometry zoneGeo : geomList) {

                            ROI geometry = zoneGeo.getROI();
                            // if every geometry really contains the selected point
                            boolean contains = false;
                            synchronized (zoneGeo) { // HACK
                                contains = geometry.contains(x0, y0);
                            }
                            if (contains) {
                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    short sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // Update of all the statistics
                                    // If a range list is present then the sample is checked if it is inside the range
                                    if (rangesNoClass) {
                                        for (Range range : rangeList) {
                                            if (range.contains(sample)) {
                                                // For local statistics the pixel is checked for every range
                                                if (localStats) {
                                                    zoneGeo.add(sample, bands[i], classId, range);
                                                } else {
                                                    // For non local statistics the pixel when the pixel is contained inside a singular range
                                                    // it is added to the statistic container
                                                    zoneGeo.add(sample, bands[i], classId,
                                                            rangeHelper);
                                                    break;
                                                }
                                            }
                                        }
                                    } else {
                                        zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                    }
                                }
                            }
                        }
                    }
                    if (updateIterator) {
                        rectIterator.nextLine();
                    }
                }
            }
            // ONLY NO DATA PRESENT
        } else if (caseC) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y++) {
                if (updateIterator) {
                    rectIterator.startPixels();
                }
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x++) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;
                    // PixelPositions
                    int x0 = srcX + x;
                    int y0 = srcY + y;

                    // check on containment
                    if (!union.contains(x0, y0)) {
                        // Update of the RectIterator
                        if (updateIterator) {
                            rectIterator.nextPixel();
                        }
                        continue;
                    }

                    // Coordinate object creation for the spatial indexing
                    Coordinate p1 = new Coordinate(x0, y0);
                    // Envelope associated to the coordinate object
                    Envelope searchEnv = new Envelope(p1);
                    // Query on the geometry list
                    List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                            if (isNotIdentity) {
                                inverseTrans.inverseTransform(pointSrc, pointClass);
                                // Selection of the classId point
                                classId = randomIterator.getSample(pointClass.x, pointClass.y, 0);
                            } else {
                                // Selection of the classId point
                                classId = rectIterator.getSample();
                                rectIterator.nextPixel();
                            }

                        } catch (NoninvertibleTransformException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }
                    }
                    // Cycle on all the geometries found
                    for (ZoneGeometry zoneGeo : geomList) {

                        ROI geometry = zoneGeo.getROI();

                        // if every geometry really contains the selected point
                        boolean contains = false;
                        synchronized (zoneGeo) { // HACK
                            contains = geometry.contains(x0, y0);
                        }
                        if (contains) {

                            // Cycle on the selected Bands
                            for (int i = 0; i < bandNum; i++) {
                                short sample = srcData[bands[i]][posx + posy
                                        + srcBandOffsets[bands[i]]];
                                // NoData check
                                if (!noData.contains(sample)) {
                                    // Update of all the statistics
                                    // If a range list is present then the sample is checked if it is inside the range
                                    if (rangesNoClass) {
                                        for (Range range : rangeList) {
                                            if (range.contains(sample)) {
                                                // For local statistics the pixel is checked for every range
                                                if (localStats) {
                                                    zoneGeo.add(sample, bands[i], classId, range);
                                                } else {
                                                    // For non local statistics the pixel when the pixel is contained inside a singular range
                                                    // it is added to the statistic container
                                                    zoneGeo.add(sample, bands[i], classId,
                                                            rangeHelper);
                                                    break;
                                                }
                                            }
                                        }
                                    } else {
                                        zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                    }
                                }
                            }

                        }
                    }
                }
                if (updateIterator) {
                    rectIterator.nextLine();
                }
            }
            // ROI AND NO DATA ARE PRESENT
        } else {
            if (useROIAccessor) {

                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    if (updateIterator) {
                        rectIterator.startPixels();
                    }
                    // y position on the source data array
                    int posy = y * srcScanlineStride;

                    // roi y position
                    int posYroi = y * roiScanLineStride;

                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;

                        // check on containment
                        if (!union.contains(x0, y0)) {
                            // Update of the RectIterator
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // ROI index position
                        int windex = x + posYroi;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                        if (w == 0) {
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // Coordinate object creation for the spatial indexing
                        Coordinate p1 = new Coordinate(x0, y0);
                        // Envelope associated to the coordinate object
                        Envelope searchEnv = new Envelope(p1);
                        // Query on the geometry list
                        List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                                if (isNotIdentity) {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);
                                    // Selection of the classId point
                                    classId = randomIterator.getSample(pointClass.x, pointClass.y,
                                            0);
                                } else {
                                    // Selection of the classId point
                                    classId = rectIterator.getSample();
                                    rectIterator.nextPixel();
                                }

                            } catch (NoninvertibleTransformException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                        // Cycle on all the geometries found
                        for (ZoneGeometry zoneGeo : geomList) {

                            ROI geometry = zoneGeo.getROI();

                            // if every geometry really contains the selected point
                            boolean contains = false;
                            synchronized (zoneGeo) { // HACK
                                contains = geometry.contains(x0, y0);
                            }
                            if (contains) {

                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    short sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // NoData check
                                    if (!noData.contains(sample)) {
                                        // Update of all the statistics
                                        // If a range list is present then the sample is checked if it is inside the range
                                        if (rangesNoClass) {
                                            for (Range range : rangeList) {
                                                if (range.contains(sample)) {
                                                    // For local statistics the pixel is checked for every range
                                                    if (localStats) {
                                                        zoneGeo.add(sample, bands[i], classId,
                                                                range);
                                                    } else {
                                                        // For non local statistics the pixel when the pixel is contained inside a singular range
                                                        // it is added to the statistic container
                                                        zoneGeo.add(sample, bands[i], classId,
                                                                rangeHelper);
                                                        break;
                                                    }
                                                }
                                            }
                                        } else {
                                            zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                        }
                                    }
                                }

                            }
                        }
                    }
                    if (updateIterator) {
                        rectIterator.nextLine();
                    }
                }
            } else {

                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    if (updateIterator) {
                        rectIterator.startPixels();
                    }
                    // y position on the source data array
                    int posy = y * srcScanlineStride;

                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;

                        // check on containment
                        if (!union.contains(x0, y0)) {
                            // Update of the RectIterator
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // ROI value
                        boolean insideROI = false;
                        synchronized (this) { // HACK
                            insideROI = srcROI.contains(x0, y0);
                        }
                        if (!insideROI) {
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // Coordinate object creation for the spatial indexing
                        Coordinate p1 = new Coordinate(x0, y0);
                        // Envelope associated to the coordinate object
                        Envelope searchEnv = new Envelope(p1);
                        // Query on the geometry list
                        List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                                if (isNotIdentity) {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);
                                    // Selection of the classId point
                                    classId = randomIterator.getSample(pointClass.x, pointClass.y,
                                            0);
                                } else {
                                    // Selection of the classId point
                                    classId = rectIterator.getSample();
                                    rectIterator.nextPixel();
                                }

                            } catch (NoninvertibleTransformException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                        // Cycle on all the geometries found
                        for (ZoneGeometry zoneGeo : geomList) {

                            ROI geometry = zoneGeo.getROI();

                            // if every geometry really contains the selected point
                            boolean contains = false;
                            synchronized (zoneGeo) { // HACK
                                contains = geometry.contains(x0, y0);
                            }
                            if (contains) {

                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    short sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // NoData check
                                    if (!noData.contains(sample)) {
                                        // Update of all the statistics
                                        // If a range list is present then the sample is checked if it is inside the range
                                        if (rangesNoClass) {
                                            for (Range range : rangeList) {
                                                if (range.contains(sample)) {
                                                    // For local statistics the pixel is checked for every range
                                                    if (localStats) {
                                                        zoneGeo.add(sample, bands[i], classId,
                                                                range);
                                                    } else {
                                                        // For non local statistics the pixel when the pixel is contained inside a singular range
                                                        // it is added to the statistic container
                                                        zoneGeo.add(sample, bands[i], classId,
                                                                rangeHelper);
                                                        break;
                                                    }
                                                }
                                            }
                                        } else {
                                            zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                        }
                                    }
                                }

                            }
                        }
                    }
                    if (updateIterator) {
                        rectIterator.nextLine();
                    }
                }
            }
        }
    }

    private void intLoop(RasterAccessor src, Rectangle computableArea, int tileX, int tileY,
            RasterAccessor roi) {

        // Source and ROI RasterAccessor initial parameters
        final int srcX = src.getX();
        final int srcY = src.getY();

        int srcData[][] = src.getIntDataArrays();

        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        RectIter rectIterator = null;

        if (updateIterator) {
            Raster ras = classifier.getTile(tileX, tileY);
            rectIterator = RectIterFactory.create(ras, computableArea);
            rectIterator.startBands();
            rectIterator.startLines();
        }

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

        // NO DATA AND ROI NOT PRESENT
        if (caseA) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y++) {
                if (updateIterator) {
                    rectIterator.startPixels();
                }
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x++) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;

                    // PixelPositions
                    int x0 = srcX + x;
                    int y0 = srcY + y;

                    // check on containment
                    if (!union.contains(x0, y0)) {
                        // Update of the RectIterator
                        if (updateIterator) {
                            rectIterator.nextPixel();
                        }
                        continue;
                    }

                    // Coordinate object creation for the spatial indexing
                    Coordinate p1 = new Coordinate(x0, y0);
                    // Envelope associated to the coordinate object
                    Envelope searchEnv = new Envelope(p1);
                    // Query on the geometry list
                    List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                            if (isNotIdentity) {
                                inverseTrans.inverseTransform(pointSrc, pointClass);
                                // Selection of the classId point
                                classId = randomIterator.getSample(pointClass.x, pointClass.y, 0);
                            } else {
                                // Selection of the classId point
                                classId = rectIterator.getSample();
                                rectIterator.nextPixel();
                            }

                        } catch (NoninvertibleTransformException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }
                    }
                    // Cycle on all the geometries found
                    for (ZoneGeometry zoneGeo : geomList) {

                        ROI geometry = zoneGeo.getROI();
                        // if every geometry really contains the selected point
                        boolean contains = false;
                        synchronized (zoneGeo) { // HACK
                            contains = geometry.contains(x0, y0);
                        }
                        if (contains) {
                            // Cycle on the selected Bands
                            for (int i = 0; i < bandNum; i++) {
                                int sample = srcData[bands[i]][posx + posy
                                        + srcBandOffsets[bands[i]]];
                                // Update of all the statistics
                                // If a range list is present then the sample is checked if it is inside the range
                                if (rangesNoClass) {
                                    for (Range range : rangeList) {
                                        if (range.contains(sample)) {
                                            // For local statistics the pixel is checked for every range
                                            if (localStats) {
                                                zoneGeo.add(sample, bands[i], classId, range);
                                            } else {
                                                // For non local statistics the pixel when the pixel is contained inside a singular range
                                                // it is added to the statistic container
                                                zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                                break;
                                            }
                                        }
                                    }
                                } else {
                                    zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                }
                            }
                        }
                    }
                }
                if (updateIterator) {
                    rectIterator.nextLine();
                }
            }

            // ONLY ROI PRESENT
        } else if (caseB) {
            if (useROIAccessor) {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    if (updateIterator) {
                        rectIterator.startPixels();
                    }
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // roi y position
                    int posYroi = y * roiScanLineStride;

                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;

                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;

                        // check on containment
                        if (!union.contains(x0, y0)) {
                            // Update of the RectIterator
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // ROI index position
                        int windex = x + posYroi;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                        if (w == 0) {
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // Coordinate object creation for the spatial indexing
                        Coordinate p1 = new Coordinate(x0, y0);
                        // Envelope associated to the coordinate object
                        Envelope searchEnv = new Envelope(p1);
                        // Query on the geometry list
                        List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                                if (isNotIdentity) {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);
                                    // Selection of the classId point
                                    classId = randomIterator.getSample(pointClass.x, pointClass.y,
                                            0);
                                } else {
                                    // Selection of the classId point
                                    classId = rectIterator.getSample();
                                    rectIterator.nextPixel();
                                }

                            } catch (NoninvertibleTransformException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                        // Cycle on all the geometries found
                        for (ZoneGeometry zoneGeo : geomList) {

                            ROI geometry = zoneGeo.getROI();
                            // if every geometry really contains the selected point
                            boolean contains = false;
                            synchronized (zoneGeo) { // HACK
                                contains = geometry.contains(x0, y0);
                            }
                            if (contains) {
                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    int sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // Update of all the statistics
                                    // If a range list is present then the sample is checked if it is inside the range
                                    if (rangesNoClass) {
                                        for (Range range : rangeList) {
                                            if (range.contains(sample)) {
                                                // For local statistics the pixel is checked for every range
                                                if (localStats) {
                                                    zoneGeo.add(sample, bands[i], classId, range);
                                                } else {
                                                    // For non local statistics the pixel when the pixel is contained inside a singular range
                                                    // it is added to the statistic container
                                                    zoneGeo.add(sample, bands[i], classId,
                                                            rangeHelper);
                                                    break;
                                                }
                                            }
                                        }
                                    } else {
                                        zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                    }
                                }
                            }
                        }
                    }
                    if (updateIterator) {
                        rectIterator.nextLine();
                    }
                }
            } else {

                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    if (updateIterator) {
                        rectIterator.startPixels();
                    }
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;

                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;

                        // check on containment
                        if (!union.contains(x0, y0)) {
                            // Update of the RectIterator
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // ROI value
                        boolean insideROI = false;
                        synchronized (this) { // HACK
                            insideROI = srcROI.contains(x0, y0);
                        }
                        if (!insideROI) {
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // Coordinate object creation for the spatial indexing
                        Coordinate p1 = new Coordinate(x0, y0);
                        // Envelope associated to the coordinate object
                        Envelope searchEnv = new Envelope(p1);
                        // Query on the geometry list
                        List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                                if (isNotIdentity) {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);
                                    // Selection of the classId point
                                    classId = randomIterator.getSample(pointClass.x, pointClass.y,
                                            0);
                                } else {
                                    // Selection of the classId point
                                    classId = rectIterator.getSample();
                                    rectIterator.nextPixel();
                                }

                            } catch (NoninvertibleTransformException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                        // Cycle on all the geometries found
                        for (ZoneGeometry zoneGeo : geomList) {

                            ROI geometry = zoneGeo.getROI();
                            // if every geometry really contains the selected point
                            boolean contains = false;
                            synchronized (zoneGeo) { // HACK
                                contains = geometry.contains(x0, y0);
                            }
                            if (contains) {
                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    int sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // Update of all the statistics
                                    // If a range list is present then the sample is checked if it is inside the range
                                    if (rangesNoClass) {
                                        for (Range range : rangeList) {
                                            if (range.contains(sample)) {
                                                // For local statistics the pixel is checked for every range
                                                if (localStats) {
                                                    zoneGeo.add(sample, bands[i], classId, range);
                                                } else {
                                                    // For non local statistics the pixel when the pixel is contained inside a singular range
                                                    // it is added to the statistic container
                                                    zoneGeo.add(sample, bands[i], classId,
                                                            rangeHelper);
                                                    break;
                                                }
                                            }
                                        }
                                    } else {
                                        zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                    }
                                }
                            }
                        }
                    }
                    if (updateIterator) {
                        rectIterator.nextLine();
                    }
                }
            }
            // ONLY NO DATA PRESENT
        } else if (caseC) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y++) {
                if (updateIterator) {
                    rectIterator.startPixels();
                }
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x++) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;
                    // PixelPositions
                    int x0 = srcX + x;
                    int y0 = srcY + y;

                    // check on containment
                    if (!union.contains(x0, y0)) {
                        // Update of the RectIterator
                        if (updateIterator) {
                            rectIterator.nextPixel();
                        }
                        continue;
                    }

                    // Coordinate object creation for the spatial indexing
                    Coordinate p1 = new Coordinate(x0, y0);
                    // Envelope associated to the coordinate object
                    Envelope searchEnv = new Envelope(p1);
                    // Query on the geometry list
                    List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                            if (isNotIdentity) {
                                inverseTrans.inverseTransform(pointSrc, pointClass);
                                // Selection of the classId point
                                classId = randomIterator.getSample(pointClass.x, pointClass.y, 0);
                            } else {
                                // Selection of the classId point
                                classId = rectIterator.getSample();
                                rectIterator.nextPixel();
                            }

                        } catch (NoninvertibleTransformException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }
                    }
                    // Cycle on all the geometries found
                    for (ZoneGeometry zoneGeo : geomList) {

                        ROI geometry = zoneGeo.getROI();

                        // if every geometry really contains the selected point
                        boolean contains = false;
                        synchronized (zoneGeo) { // HACK
                            contains = geometry.contains(x0, y0);
                        }
                        if (contains) {

                            // Cycle on the selected Bands
                            for (int i = 0; i < bandNum; i++) {
                                int sample = srcData[bands[i]][posx + posy
                                        + srcBandOffsets[bands[i]]];
                                // NoData check
                                if (!noData.contains(sample)) {
                                    // Update of all the statistics
                                    // If a range list is present then the sample is checked if it is inside the range
                                    if (rangesNoClass) {
                                        for (Range range : rangeList) {
                                            if (range.contains(sample)) {
                                                // For local statistics the pixel is checked for every range
                                                if (localStats) {
                                                    zoneGeo.add(sample, bands[i], classId, range);
                                                } else {
                                                    // For non local statistics the pixel when the pixel is contained inside a singular range
                                                    // it is added to the statistic container
                                                    zoneGeo.add(sample, bands[i], classId,
                                                            rangeHelper);
                                                    break;
                                                }
                                            }
                                        }
                                    } else {
                                        zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                    }
                                }
                            }

                        }
                    }
                }
                if (updateIterator) {
                    rectIterator.nextLine();
                }
            }
            // ROI AND NO DATA ARE PRESENT
        } else {
            if (useROIAccessor) {

                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    if (updateIterator) {
                        rectIterator.startPixels();
                    }
                    // y position on the source data array
                    int posy = y * srcScanlineStride;

                    // roi y position
                    int posYroi = y * roiScanLineStride;

                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;

                        // check on containment
                        if (!union.contains(x0, y0)) {
                            // Update of the RectIterator
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // ROI index position
                        int windex = x + posYroi;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                        if (w == 0) {
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // Coordinate object creation for the spatial indexing
                        Coordinate p1 = new Coordinate(x0, y0);
                        // Envelope associated to the coordinate object
                        Envelope searchEnv = new Envelope(p1);
                        // Query on the geometry list
                        List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                                if (isNotIdentity) {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);
                                    // Selection of the classId point
                                    classId = randomIterator.getSample(pointClass.x, pointClass.y,
                                            0);
                                } else {
                                    // Selection of the classId point
                                    classId = rectIterator.getSample();
                                    rectIterator.nextPixel();
                                }

                            } catch (NoninvertibleTransformException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                        // Cycle on all the geometries found
                        for (ZoneGeometry zoneGeo : geomList) {

                            ROI geometry = zoneGeo.getROI();

                            // if every geometry really contains the selected point
                            boolean contains = false;
                            synchronized (zoneGeo) { // HACK
                                contains = geometry.contains(x0, y0);
                            }
                            if (contains) {

                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    int sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // NoData check
                                    if (!noData.contains(sample)) {
                                        // Update of all the statistics
                                        // If a range list is present then the sample is checked if it is inside the range
                                        if (rangesNoClass) {
                                            for (Range range : rangeList) {
                                                if (range.contains(sample)) {
                                                    // For local statistics the pixel is checked for every range
                                                    if (localStats) {
                                                        zoneGeo.add(sample, bands[i], classId,
                                                                range);
                                                    } else {
                                                        // For non local statistics the pixel when the pixel is contained inside a singular range
                                                        // it is added to the statistic container
                                                        zoneGeo.add(sample, bands[i], classId,
                                                                rangeHelper);
                                                        break;
                                                    }
                                                }
                                            }
                                        } else {
                                            zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                        }
                                    }
                                }

                            }
                        }
                    }
                    if (updateIterator) {
                        rectIterator.nextLine();
                    }
                }
            } else {

                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    if (updateIterator) {
                        rectIterator.startPixels();
                    }
                    // y position on the source data array
                    int posy = y * srcScanlineStride;

                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;

                        // check on containment
                        if (!union.contains(x0, y0)) {
                            // Update of the RectIterator
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // ROI value
                        boolean insideROI = false;
                        synchronized (this) { // HACK
                            insideROI = srcROI.contains(x0, y0);
                        }
                        if (!insideROI) {
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // Coordinate object creation for the spatial indexing
                        Coordinate p1 = new Coordinate(x0, y0);
                        // Envelope associated to the coordinate object
                        Envelope searchEnv = new Envelope(p1);
                        // Query on the geometry list
                        List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                                if (isNotIdentity) {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);
                                    // Selection of the classId point
                                    classId = randomIterator.getSample(pointClass.x, pointClass.y,
                                            0);
                                } else {
                                    // Selection of the classId point
                                    classId = rectIterator.getSample();
                                    rectIterator.nextPixel();
                                }

                            } catch (NoninvertibleTransformException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                        // Cycle on all the geometries found
                        for (ZoneGeometry zoneGeo : geomList) {

                            ROI geometry = zoneGeo.getROI();

                            // if every geometry really contains the selected point
                            boolean contains = false;
                            synchronized (zoneGeo) { // HACK
                                contains = geometry.contains(x0, y0);
                            }
                            if (contains) {

                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    int sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // NoData check
                                    if (!noData.contains(sample)) {
                                        // Update of all the statistics
                                        // If a range list is present then the sample is checked if it is inside the range
                                        if (rangesNoClass) {
                                            for (Range range : rangeList) {
                                                if (range.contains(sample)) {
                                                    // For local statistics the pixel is checked for every range
                                                    if (localStats) {
                                                        zoneGeo.add(sample, bands[i], classId,
                                                                range);
                                                    } else {
                                                        // For non local statistics the pixel when the pixel is contained inside a singular range
                                                        // it is added to the statistic container
                                                        zoneGeo.add(sample, bands[i], classId,
                                                                rangeHelper);
                                                        break;
                                                    }
                                                }
                                            }
                                        } else {
                                            zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                        }
                                    }
                                }

                            }
                        }
                    }
                    if (updateIterator) {
                        rectIterator.nextLine();
                    }
                }
            }
        }

    }

    private void floatLoop(RasterAccessor src, Rectangle computableArea, int tileX, int tileY,
            RasterAccessor roi) {

        // Source RasterAccessor initial parameters
        final int srcX = src.getX();
        final int srcY = src.getY();

        float srcData[][] = src.getFloatDataArrays();

        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        RectIter rectIterator = null;

        if (updateIterator) {
            Raster ras = classifier.getTile(tileX, tileY);
            rectIterator = RectIterFactory.create(ras, computableArea);
            rectIterator.startBands();
            rectIterator.startLines();
        }

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

        // NO DATA AND ROI NOT PRESENT
        if (caseA) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y++) {
                if (updateIterator) {
                    rectIterator.startPixels();
                }
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x++) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;

                    // PixelPositions
                    int x0 = srcX + x;
                    int y0 = srcY + y;

                    // check on containment
                    if (!union.contains(x0, y0)) {
                        // Update of the RectIterator
                        if (updateIterator) {
                            rectIterator.nextPixel();
                        }
                        continue;
                    }

                    // Coordinate object creation for the spatial indexing
                    Coordinate p1 = new Coordinate(x0, y0);
                    // Envelope associated to the coordinate object
                    Envelope searchEnv = new Envelope(p1);
                    // Query on the geometry list
                    List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                            if (isNotIdentity) {
                                inverseTrans.inverseTransform(pointSrc, pointClass);
                                // Selection of the classId point
                                classId = randomIterator.getSample(pointClass.x, pointClass.y, 0);
                            } else {
                                // Selection of the classId point
                                classId = rectIterator.getSample();
                                rectIterator.nextPixel();
                            }

                        } catch (NoninvertibleTransformException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }
                    }
                    // Cycle on all the geometries found
                    for (ZoneGeometry zoneGeo : geomList) {

                        ROI geometry = zoneGeo.getROI();
                        // if every geometry really contains the selected point
                        boolean contains = false;
                        synchronized (zoneGeo) { // HACK
                            contains = geometry.contains(x0, y0);
                        }
                        if (contains) {
                            // Cycle on the selected Bands
                            for (int i = 0; i < bandNum; i++) {
                                float sample = srcData[bands[i]][posx + posy
                                        + srcBandOffsets[bands[i]]];
                                // Update of all the statistics
                                // If a range list is present then the sample is checked if it is inside the range
                                if (rangesNoClass) {
                                    for (Range range : rangeList) {
                                        if (range.contains(sample)) {
                                            // For local statistics the pixel is checked for every range
                                            if (localStats) {
                                                zoneGeo.add(sample, bands[i], classId, range);
                                            } else {
                                                // For non local statistics the pixel when the pixel is contained inside a singular range
                                                // it is added to the statistic container
                                                zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                                break;
                                            }
                                        }
                                    }
                                } else {
                                    zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                }
                            }
                        }
                    }
                }
                if (updateIterator) {
                    rectIterator.nextLine();
                }
            }

            // ONLY ROI PRESENT
        } else if (caseB) {
            if (useROIAccessor) {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    if (updateIterator) {
                        rectIterator.startPixels();
                    }
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // roi y position
                    int posYroi = y * roiScanLineStride;

                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;

                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;

                        // check on containment
                        if (!union.contains(x0, y0)) {
                            // Update of the RectIterator
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // ROI index position
                        int windex = x + posYroi;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                        if (w == 0) {
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // Coordinate object creation for the spatial indexing
                        Coordinate p1 = new Coordinate(x0, y0);
                        // Envelope associated to the coordinate object
                        Envelope searchEnv = new Envelope(p1);
                        // Query on the geometry list
                        List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                                if (isNotIdentity) {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);
                                    // Selection of the classId point
                                    classId = randomIterator.getSample(pointClass.x, pointClass.y,
                                            0);
                                } else {
                                    // Selection of the classId point
                                    classId = rectIterator.getSample();
                                    rectIterator.nextPixel();
                                }

                            } catch (NoninvertibleTransformException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                        // Cycle on all the geometries found
                        for (ZoneGeometry zoneGeo : geomList) {

                            ROI geometry = zoneGeo.getROI();
                            // if every geometry really contains the selected point
                            boolean contains = false;
                            synchronized (zoneGeo) { // HACK
                                contains = geometry.contains(x0, y0);
                            }
                            if (contains) {
                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    float sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // Update of all the statistics
                                    // If a range list is present then the sample is checked if it is inside the range
                                    if (rangesNoClass) {
                                        for (Range range : rangeList) {
                                            if (range.contains(sample)) {
                                                // For local statistics the pixel is checked for every range
                                                if (localStats) {
                                                    zoneGeo.add(sample, bands[i], classId, range);
                                                } else {
                                                    // For non local statistics the pixel when the pixel is contained inside a singular range
                                                    // it is added to the statistic container
                                                    zoneGeo.add(sample, bands[i], classId,
                                                            rangeHelper);
                                                    break;
                                                }
                                            }
                                        }
                                    } else {
                                        zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                    }
                                }
                            }
                        }
                    }
                    if (updateIterator) {
                        rectIterator.nextLine();
                    }
                }
            } else {

                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    if (updateIterator) {
                        rectIterator.startPixels();
                    }
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;

                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;

                        // check on containment
                        if (!union.contains(x0, y0)) {
                            // Update of the RectIterator
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // ROI value
                        boolean insideROI = false;
                        synchronized (this) { // HACK
                            insideROI = srcROI.contains(x0, y0);
                        }
                        if (!insideROI) {
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // Coordinate object creation for the spatial indexing
                        Coordinate p1 = new Coordinate(x0, y0);
                        // Envelope associated to the coordinate object
                        Envelope searchEnv = new Envelope(p1);
                        // Query on the geometry list
                        List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                                if (isNotIdentity) {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);
                                    // Selection of the classId point
                                    classId = randomIterator.getSample(pointClass.x, pointClass.y,
                                            0);
                                } else {
                                    // Selection of the classId point
                                    classId = rectIterator.getSample();
                                    rectIterator.nextPixel();
                                }

                            } catch (NoninvertibleTransformException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                        // Cycle on all the geometries found
                        for (ZoneGeometry zoneGeo : geomList) {

                            ROI geometry = zoneGeo.getROI();
                            // if every geometry really contains the selected point
                            boolean contains = false;
                            synchronized (zoneGeo) { // HACK
                                contains = geometry.contains(x0, y0);
                            }
                            if (contains) {
                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    float sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // Update of all the statistics
                                    // If a range list is present then the sample is checked if it is inside the range
                                    if (rangesNoClass) {
                                        for (Range range : rangeList) {
                                            if (range.contains(sample)) {
                                                // For local statistics the pixel is checked for every range
                                                if (localStats) {
                                                    zoneGeo.add(sample, bands[i], classId, range);
                                                } else {
                                                    // For non local statistics the pixel when the pixel is contained inside a singular range
                                                    // it is added to the statistic container
                                                    zoneGeo.add(sample, bands[i], classId,
                                                            rangeHelper);
                                                    break;
                                                }
                                            }
                                        }
                                    } else {
                                        zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                    }
                                }
                            }
                        }
                    }
                    if (updateIterator) {
                        rectIterator.nextLine();
                    }
                }
            }
            // ONLY NO DATA PRESENT
        } else if (caseC) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y++) {
                if (updateIterator) {
                    rectIterator.startPixels();
                }
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x++) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;
                    // PixelPositions
                    int x0 = srcX + x;
                    int y0 = srcY + y;

                    // check on containment
                    if (!union.contains(x0, y0)) {
                        // Update of the RectIterator
                        if (updateIterator) {
                            rectIterator.nextPixel();
                        }
                        continue;
                    }

                    // Coordinate object creation for the spatial indexing
                    Coordinate p1 = new Coordinate(x0, y0);
                    // Envelope associated to the coordinate object
                    Envelope searchEnv = new Envelope(p1);
                    // Query on the geometry list
                    List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                            if (isNotIdentity) {
                                inverseTrans.inverseTransform(pointSrc, pointClass);
                                // Selection of the classId point
                                classId = randomIterator.getSample(pointClass.x, pointClass.y, 0);
                            } else {
                                // Selection of the classId point
                                classId = rectIterator.getSample();
                                rectIterator.nextPixel();
                            }

                        } catch (NoninvertibleTransformException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }
                    }
                    // Cycle on all the geometries found
                    for (ZoneGeometry zoneGeo : geomList) {

                        ROI geometry = zoneGeo.getROI();

                        // if every geometry really contains the selected point
                        boolean contains = false;
                        synchronized (zoneGeo) { // HACK
                            contains = geometry.contains(x0, y0);
                        }
                        if (contains) {

                            // Cycle on the selected Bands
                            for (int i = 0; i < bandNum; i++) {
                                float sample = srcData[bands[i]][posx + posy
                                        + srcBandOffsets[bands[i]]];
                                // NoData check
                                if (!noData.contains(sample)) {
                                    // Update of all the statistics
                                    // If a range list is present then the sample is checked if it is inside the range
                                    if (rangesNoClass) {
                                        for (Range range : rangeList) {
                                            if (range.contains(sample)) {
                                                // For local statistics the pixel is checked for every range
                                                if (localStats) {
                                                    zoneGeo.add(sample, bands[i], classId, range);
                                                } else {
                                                    // For non local statistics the pixel when the pixel is contained inside a singular range
                                                    // it is added to the statistic container
                                                    zoneGeo.add(sample, bands[i], classId,
                                                            rangeHelper);
                                                    break;
                                                }
                                            }
                                        }
                                    } else {
                                        zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                    }
                                }
                            }

                        }
                    }
                }
                if (updateIterator) {
                    rectIterator.nextLine();
                }
            }
            // ROI AND NO DATA ARE PRESENT
        } else {
            if (useROIAccessor) {

                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    if (updateIterator) {
                        rectIterator.startPixels();
                    }
                    // y position on the source data array
                    int posy = y * srcScanlineStride;

                    // roi y position
                    int posYroi = y * roiScanLineStride;

                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;

                        // check on containment
                        if (!union.contains(x0, y0)) {
                            // Update of the RectIterator
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // ROI index position
                        int windex = x + posYroi;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                        if (w == 0) {
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // Coordinate object creation for the spatial indexing
                        Coordinate p1 = new Coordinate(x0, y0);
                        // Envelope associated to the coordinate object
                        Envelope searchEnv = new Envelope(p1);
                        // Query on the geometry list
                        List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                                if (isNotIdentity) {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);
                                    // Selection of the classId point
                                    classId = randomIterator.getSample(pointClass.x, pointClass.y,
                                            0);
                                } else {
                                    // Selection of the classId point
                                    classId = rectIterator.getSample();
                                    rectIterator.nextPixel();
                                }

                            } catch (NoninvertibleTransformException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                        // Cycle on all the geometries found
                        for (ZoneGeometry zoneGeo : geomList) {

                            ROI geometry = zoneGeo.getROI();

                            // if every geometry really contains the selected point
                            boolean contains = false;
                            synchronized (zoneGeo) { // HACK
                                contains = geometry.contains(x0, y0);
                            }
                            if (contains) {

                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    float sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // NoData check
                                    if (!noData.contains(sample)) {
                                        // Update of all the statistics
                                        // If a range list is present then the sample is checked if it is inside the range
                                        if (rangesNoClass) {
                                            for (Range range : rangeList) {
                                                if (range.contains(sample)) {
                                                    // For local statistics the pixel is checked for every range
                                                    if (localStats) {
                                                        zoneGeo.add(sample, bands[i], classId,
                                                                range);
                                                    } else {
                                                        // For non local statistics the pixel when the pixel is contained inside a singular range
                                                        // it is added to the statistic container
                                                        zoneGeo.add(sample, bands[i], classId,
                                                                rangeHelper);
                                                        break;
                                                    }
                                                }
                                            }
                                        } else {
                                            zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                        }
                                    }
                                }

                            }
                        }
                    }
                    if (updateIterator) {
                        rectIterator.nextLine();
                    }
                }
            } else {

                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    if (updateIterator) {
                        rectIterator.startPixels();
                    }
                    // y position on the source data array
                    int posy = y * srcScanlineStride;

                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;

                        // check on containment
                        if (!union.contains(x0, y0)) {
                            // Update of the RectIterator
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // ROI value
                        boolean insideROI = false;
                        synchronized (this) { // HACK
                            insideROI = srcROI.contains(x0, y0);
                        }
                        if (!insideROI) {
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // Coordinate object creation for the spatial indexing
                        Coordinate p1 = new Coordinate(x0, y0);
                        // Envelope associated to the coordinate object
                        Envelope searchEnv = new Envelope(p1);
                        // Query on the geometry list
                        List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                                if (isNotIdentity) {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);
                                    // Selection of the classId point
                                    classId = randomIterator.getSample(pointClass.x, pointClass.y,
                                            0);
                                } else {
                                    // Selection of the classId point
                                    classId = rectIterator.getSample();
                                    rectIterator.nextPixel();
                                }

                            } catch (NoninvertibleTransformException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                        // Cycle on all the geometries found
                        for (ZoneGeometry zoneGeo : geomList) {

                            ROI geometry = zoneGeo.getROI();

                            // if every geometry really contains the selected point
                            boolean contains = false;
                            synchronized (zoneGeo) { // HACK
                                contains = geometry.contains(x0, y0);
                            }
                            if (contains) {

                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    float sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // NoData check
                                    if (!noData.contains(sample)) {
                                        // Update of all the statistics
                                        // If a range list is present then the sample is checked if it is inside the range
                                        if (rangesNoClass) {
                                            for (Range range : rangeList) {
                                                if (range.contains(sample)) {
                                                    // For local statistics the pixel is checked for every range
                                                    if (localStats) {
                                                        zoneGeo.add(sample, bands[i], classId,
                                                                range);
                                                    } else {
                                                        // For non local statistics the pixel when the pixel is contained inside a singular range
                                                        // it is added to the statistic container
                                                        zoneGeo.add(sample, bands[i], classId,
                                                                rangeHelper);
                                                        break;
                                                    }
                                                }
                                            }
                                        } else {
                                            zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                        }
                                    }
                                }

                            }
                        }
                    }
                    if (updateIterator) {
                        rectIterator.nextLine();
                    }
                }
            }
        }

    }

    private void doubleLoop(RasterAccessor src, Rectangle computableArea, int tileX, int tileY,
            RasterAccessor roi) {

        // Source RasterAccessor initial parameters
        final int srcX = src.getX();
        final int srcY = src.getY();

        double srcData[][] = src.getDoubleDataArrays();

        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        RectIter rectIterator = null;

        if (updateIterator) {
            Raster ras = classifier.getTile(tileX, tileY);
            rectIterator = RectIterFactory.create(ras, computableArea);
            rectIterator.startBands();
            rectIterator.startLines();
        }

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

        // NO DATA AND ROI NOT PRESENT
        if (caseA) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y++) {
                if (updateIterator) {
                    rectIterator.startPixels();
                }
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x++) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;

                    // PixelPositions
                    int x0 = srcX + x;
                    int y0 = srcY + y;

                    // check on containment
                    if (!union.contains(x0, y0)) {
                        // Update of the RectIterator
                        if (updateIterator) {
                            rectIterator.nextPixel();
                        }
                        continue;
                    }

                    // Coordinate object creation for the spatial indexing
                    Coordinate p1 = new Coordinate(x0, y0);
                    // Envelope associated to the coordinate object
                    Envelope searchEnv = new Envelope(p1);
                    // Query on the geometry list
                    List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                            if (isNotIdentity) {
                                inverseTrans.inverseTransform(pointSrc, pointClass);
                                // Selection of the classId point
                                classId = randomIterator.getSample(pointClass.x, pointClass.y, 0);
                            } else {
                                // Selection of the classId point
                                classId = rectIterator.getSample();
                                rectIterator.nextPixel();
                            }

                        } catch (NoninvertibleTransformException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }
                    }
                    // Cycle on all the geometries found
                    for (ZoneGeometry zoneGeo : geomList) {

                        ROI geometry = zoneGeo.getROI();
                        // if every geometry really contains the selected point
                        boolean contains = false;
                        synchronized (zoneGeo) { // HACK
                            contains = geometry.contains(x0, y0);
                        }
                        if (contains) {
                            // Cycle on the selected Bands
                            for (int i = 0; i < bandNum; i++) {
                                double sample = srcData[bands[i]][posx + posy
                                        + srcBandOffsets[bands[i]]];
                                // Update of all the statistics
                                // If a range list is present then the sample is checked if it is inside the range
                                if (rangesNoClass) {
                                    for (Range range : rangeList) {
                                        if (range.contains(sample)) {
                                            // For local statistics the pixel is checked for every range
                                            if (localStats) {
                                                zoneGeo.add(sample, bands[i], classId, range);
                                            } else {
                                                // For non local statistics the pixel when the pixel is contained inside a singular range
                                                // it is added to the statistic container
                                                zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                                break;
                                            }
                                        }
                                    }
                                } else {
                                    zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                }
                            }
                        }
                    }
                }
                if (updateIterator) {
                    rectIterator.nextLine();
                }
            }

            // ONLY ROI PRESENT
        } else if (caseB) {
            if (useROIAccessor) {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    if (updateIterator) {
                        rectIterator.startPixels();
                    }
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // roi y position
                    int posYroi = y * roiScanLineStride;

                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;

                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;

                        // check on containment
                        if (!union.contains(x0, y0)) {
                            // Update of the RectIterator
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // ROI index position
                        int windex = x + posYroi;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                        if (w == 0) {
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // Coordinate object creation for the spatial indexing
                        Coordinate p1 = new Coordinate(x0, y0);
                        // Envelope associated to the coordinate object
                        Envelope searchEnv = new Envelope(p1);
                        // Query on the geometry list
                        List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                                if (isNotIdentity) {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);
                                    // Selection of the classId point
                                    classId = randomIterator.getSample(pointClass.x, pointClass.y,
                                            0);
                                } else {
                                    // Selection of the classId point
                                    classId = rectIterator.getSample();
                                    rectIterator.nextPixel();
                                }

                            } catch (NoninvertibleTransformException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                        // Cycle on all the geometries found
                        for (ZoneGeometry zoneGeo : geomList) {

                            ROI geometry = zoneGeo.getROI();
                            // if every geometry really contains the selected point
                            boolean contains = false;
                            synchronized (zoneGeo) { // HACK
                                contains = geometry.contains(x0, y0);
                            }
                            if (contains) {
                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    double sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // Update of all the statistics
                                    // If a range list is present then the sample is checked if it is inside the range
                                    if (rangesNoClass) {
                                        for (Range range : rangeList) {
                                            if (range.contains(sample)) {
                                                // For local statistics the pixel is checked for every range
                                                if (localStats) {
                                                    zoneGeo.add(sample, bands[i], classId, range);
                                                } else {
                                                    // For non local statistics the pixel when the pixel is contained inside a singular range
                                                    // it is added to the statistic container
                                                    zoneGeo.add(sample, bands[i], classId,
                                                            rangeHelper);
                                                    break;
                                                }
                                            }
                                        }
                                    } else {
                                        zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                    }
                                }
                            }
                        }
                    }
                    if (updateIterator) {
                        rectIterator.nextLine();
                    }
                }
            } else {

                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    if (updateIterator) {
                        rectIterator.startPixels();
                    }
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;

                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;

                        // check on containment
                        if (!union.contains(x0, y0)) {
                            // Update of the RectIterator
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // ROI value
                        boolean insideROI = false;
                        synchronized (this) { // HACK
                            insideROI = srcROI.contains(x0, y0);
                        }
                        if (!insideROI) {
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // Coordinate object creation for the spatial indexing
                        Coordinate p1 = new Coordinate(x0, y0);
                        // Envelope associated to the coordinate object
                        Envelope searchEnv = new Envelope(p1);
                        // Query on the geometry list
                        List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                                if (isNotIdentity) {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);
                                    // Selection of the classId point
                                    classId = randomIterator.getSample(pointClass.x, pointClass.y,
                                            0);
                                } else {
                                    // Selection of the classId point
                                    classId = rectIterator.getSample();
                                    rectIterator.nextPixel();
                                }

                            } catch (NoninvertibleTransformException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                        // Cycle on all the geometries found
                        for (ZoneGeometry zoneGeo : geomList) {

                            ROI geometry = zoneGeo.getROI();
                            // if every geometry really contains the selected point
                            boolean contains = false;
                            synchronized (zoneGeo) { // HACK
                                contains = geometry.contains(x0, y0);
                            }
                            if (contains) {
                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    double sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // Update of all the statistics
                                    // If a range list is present then the sample is checked if it is inside the range
                                    if (rangesNoClass) {
                                        for (Range range : rangeList) {
                                            if (range.contains(sample)) {
                                                // For local statistics the pixel is checked for every range
                                                if (localStats) {
                                                    zoneGeo.add(sample, bands[i], classId, range);
                                                } else {
                                                    // For non local statistics the pixel when the pixel is contained inside a singular range
                                                    // it is added to the statistic container
                                                    zoneGeo.add(sample, bands[i], classId,
                                                            rangeHelper);
                                                    break;
                                                }
                                            }
                                        }
                                    } else {
                                        zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                    }
                                }
                            }
                        }
                    }
                    if (updateIterator) {
                        rectIterator.nextLine();
                    }
                }
            }
            // ONLY NO DATA PRESENT
        } else if (caseC) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y++) {
                if (updateIterator) {
                    rectIterator.startPixels();
                }
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x++) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;
                    // PixelPositions
                    int x0 = srcX + x;
                    int y0 = srcY + y;

                    // check on containment
                    if (!union.contains(x0, y0)) {
                        // Update of the RectIterator
                        if (updateIterator) {
                            rectIterator.nextPixel();
                        }
                        continue;
                    }

                    // Coordinate object creation for the spatial indexing
                    Coordinate p1 = new Coordinate(x0, y0);
                    // Envelope associated to the coordinate object
                    Envelope searchEnv = new Envelope(p1);
                    // Query on the geometry list
                    List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                            if (isNotIdentity) {
                                inverseTrans.inverseTransform(pointSrc, pointClass);
                                // Selection of the classId point
                                classId = randomIterator.getSample(pointClass.x, pointClass.y, 0);
                            } else {
                                // Selection of the classId point
                                classId = rectIterator.getSample();
                                rectIterator.nextPixel();
                            }

                        } catch (NoninvertibleTransformException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }
                    }
                    // Cycle on all the geometries found
                    for (ZoneGeometry zoneGeo : geomList) {

                        ROI geometry = zoneGeo.getROI();

                        // if every geometry really contains the selected point
                        boolean contains = false;
                        synchronized (zoneGeo) { // HACK
                            contains = geometry.contains(x0, y0);
                        }
                        if (contains) {

                            // Cycle on the selected Bands
                            for (int i = 0; i < bandNum; i++) {
                                double sample = srcData[bands[i]][posx + posy
                                        + srcBandOffsets[bands[i]]];
                                // NoData check
                                if (!noData.contains(sample)) {
                                    // Update of all the statistics
                                    // If a range list is present then the sample is checked if it is inside the range
                                    if (rangesNoClass) {
                                        for (Range range : rangeList) {
                                            if (range.contains(sample)) {
                                                // For local statistics the pixel is checked for every range
                                                if (localStats) {
                                                    zoneGeo.add(sample, bands[i], classId, range);
                                                } else {
                                                    // For non local statistics the pixel when the pixel is contained inside a singular range
                                                    // it is added to the statistic container
                                                    zoneGeo.add(sample, bands[i], classId,
                                                            rangeHelper);
                                                    break;
                                                }
                                            }
                                        }
                                    } else {
                                        zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                    }
                                }
                            }

                        }
                    }
                }
                if (updateIterator) {
                    rectIterator.nextLine();
                }
            }
            // ROI AND NO DATA ARE PRESENT
        } else {
            if (useROIAccessor) {

                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    if (updateIterator) {
                        rectIterator.startPixels();
                    }
                    // y position on the source data array
                    int posy = y * srcScanlineStride;

                    // roi y position
                    int posYroi = y * roiScanLineStride;

                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;

                        // check on containment
                        if (!union.contains(x0, y0)) {
                            // Update of the RectIterator
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // ROI index position
                        int windex = x + posYroi;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                        if (w == 0) {
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // Coordinate object creation for the spatial indexing
                        Coordinate p1 = new Coordinate(x0, y0);
                        // Envelope associated to the coordinate object
                        Envelope searchEnv = new Envelope(p1);
                        // Query on the geometry list
                        List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                                if (isNotIdentity) {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);
                                    // Selection of the classId point
                                    classId = randomIterator.getSample(pointClass.x, pointClass.y,
                                            0);
                                } else {
                                    // Selection of the classId point
                                    classId = rectIterator.getSample();
                                    rectIterator.nextPixel();
                                }

                            } catch (NoninvertibleTransformException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                        // Cycle on all the geometries found
                        for (ZoneGeometry zoneGeo : geomList) {

                            ROI geometry = zoneGeo.getROI();

                            // if every geometry really contains the selected point
                            boolean contains = false;
                            synchronized (zoneGeo) { // HACK
                                contains = geometry.contains(x0, y0);
                            }
                            if (contains) {

                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    double sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // NoData check
                                    if (!noData.contains(sample)) {
                                        // Update of all the statistics
                                        // If a range list is present then the sample is checked if it is inside the range
                                        if (rangesNoClass) {
                                            for (Range range : rangeList) {
                                                if (range.contains(sample)) {
                                                    // For local statistics the pixel is checked for every range
                                                    if (localStats) {
                                                        zoneGeo.add(sample, bands[i], classId,
                                                                range);
                                                    } else {
                                                        // For non local statistics the pixel when the pixel is contained inside a singular range
                                                        // it is added to the statistic container
                                                        zoneGeo.add(sample, bands[i], classId,
                                                                rangeHelper);
                                                        break;
                                                    }
                                                }
                                            }
                                        } else {
                                            zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                        }
                                    }
                                }

                            }
                        }
                    }
                    if (updateIterator) {
                        rectIterator.nextLine();
                    }
                }
            } else {

                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y++) {
                    if (updateIterator) {
                        rectIterator.startPixels();
                    }
                    // y position on the source data array
                    int posy = y * srcScanlineStride;

                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x++) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;

                        // check on containment
                        if (!union.contains(x0, y0)) {
                            // Update of the RectIterator
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // ROI value
                        boolean insideROI = false;
                        synchronized (this) { // HACK
                            insideROI = srcROI.contains(x0, y0);
                        }
                        if (!insideROI) {
                            if (updateIterator) {
                                rectIterator.nextPixel();
                            }
                            continue;
                        }

                        // Coordinate object creation for the spatial indexing
                        Coordinate p1 = new Coordinate(x0, y0);
                        // Envelope associated to the coordinate object
                        Envelope searchEnv = new Envelope(p1);
                        // Query on the geometry list
                        List<ZoneGeometry> geomList = spatialIndex.query(searchEnv);
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
                                if (isNotIdentity) {
                                    inverseTrans.inverseTransform(pointSrc, pointClass);
                                    // Selection of the classId point
                                    classId = randomIterator.getSample(pointClass.x, pointClass.y,
                                            0);
                                } else {
                                    // Selection of the classId point
                                    classId = rectIterator.getSample();
                                    rectIterator.nextPixel();
                                }

                            } catch (NoninvertibleTransformException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                        // Cycle on all the geometries found
                        for (ZoneGeometry zoneGeo : geomList) {

                            ROI geometry = zoneGeo.getROI();

                            // if every geometry really contains the selected point
                            boolean contains = false;
                            synchronized (zoneGeo) { // HACK
                                contains = geometry.contains(x0, y0);
                            }
                            if (contains) {

                                // Cycle on the selected Bands
                                for (int i = 0; i < bandNum; i++) {
                                    double sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // NoData check
                                    if (!noData.contains(sample)) {
                                        // Update of all the statistics
                                        // If a range list is present then the sample is checked if it is inside the range
                                        if (rangesNoClass) {
                                            for (Range range : rangeList) {
                                                if (range.contains(sample)) {
                                                    // For local statistics the pixel is checked for every range
                                                    if (localStats) {
                                                        zoneGeo.add(sample, bands[i], classId,
                                                                range);
                                                    } else {
                                                        // For non local statistics the pixel when the pixel is contained inside a singular range
                                                        // it is added to the statistic container
                                                        zoneGeo.add(sample, bands[i], classId,
                                                                rangeHelper);
                                                        break;
                                                    }
                                                }
                                            }
                                        } else {
                                            zoneGeo.add(sample, bands[i], classId, rangeHelper);
                                        }
                                    }
                                }

                            }
                        }
                    }
                    if (updateIterator) {
                        rectIterator.nextLine();
                    }
                }
            }
        }

    }

    /** {@link OpImage} method that returns the destination image bounds, because source and destination images are equals */
    @Override
    public Rectangle mapDestRect(Rectangle destRect, int index) {
        return destRect;
    }

    /** {@link OpImage} method that returns the source image bounds, because source and destination images are equals */
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
        firstTime.set(true);
    }

    /**
     * Computes and returns all tiles in the image. The tiles are returned in a sequence corresponding to the row-major order of their respective tile
     * indices. The returned array may of course be ignored, e.g., in the case of a subclass which caches the tiles and the intent is to force their
     * computation. This method is overridden such that can be invoked only one time by using a flag for avoiding unnecessary computations.
     */
    public Raster[] getTiles() {
        if (firstTime.getAndSet(false)) {
            //return getTiles(getTileIndices(getBounds()));
            Point[] points = getTileIndices(union);
            
            if(points!=null){
                return getTiles(points);
            }else{
                return null;
            }
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

            //List<ZoneGeometry> copy = new ArrayList<ZoneGeometry>(zoneList);

            return Collections.unmodifiableList(zoneList);
        } else {
            return super.getProperty(name);
        }
    }
    
    @Override
    public synchronized void dispose() {
        if(srcROIImgExt != null) {
            srcROIImgExt.dispose();
        }
        super.dispose();
    }
}
