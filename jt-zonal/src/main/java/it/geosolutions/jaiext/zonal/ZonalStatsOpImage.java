package it.geosolutions.jaiext.zonal;

import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.stats.Statistics;
import it.geosolutions.jaiext.stats.Statistics.StatsType;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.iterator.RandomIter;

import sun.security.jca.GetInstance.Instance;

import com.sun.media.jai.codecimpl.util.RasterFactory;
import com.sun.media.jai.util.PropertyUtil;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.quadtree.Quadtree;

public class ZonalStatsOpImage extends OpImage {
    
    /** ROI extender */
    protected final static BorderExtender ROI_EXTENDER = BorderExtender
            .createInstance(BorderExtender.BORDER_ZERO);

    private static final Logger LOGGER = Logger.getAnonymousLogger(ZonalStatsOpImage.class
            .getName());

    /** Statistics property name */
    public final static String ZONAL_STATS_PROPERTY = "JAI-EXT.zonalstats";

    private volatile boolean firstTime;

    private final Quadtree spatial;

    private final boolean classPresent;

    private AffineTransform inverseTrans;

    private final boolean hasNoData;

    private final boolean hasROI;

    private final boolean caseA;

    private final boolean caseB;

    private final boolean caseC;

    private final Range noData;

    private final boolean useROIAccessor;

    private final PlanarImage srcROIImage;

    private final RandomIter roiIter;

    private final Rectangle roiBounds;

    private final boolean[] booleanLookupTable;

    private final boolean transformation;

    private final Rectangle union;

    private final ArrayList<ZoneGeometry> zoneList;

    private RenderedImage classifier;

    private Rectangle sourceBounds;

    private Rectangle zoneBounds;

    private RandomIter iterator; 

    public ZonalStatsOpImage(RenderedImage source, RenderedImage classifier,
            AffineTransform transform, ImageLayout layout, Map configuration, List<ROI> rois,
            ROI roiUsed, Range noData, boolean useROIAccessor, int[] bands, StatsType[] statsTypes) {
        super(vectorize(source), layout, configuration, true);

        // Check if the classifier is present
        classPresent = classifier != null && classifier instanceof RenderedImage;

        // Calculation of the inverse transformation
        zoneBounds = null;
        if (classPresent) {
            this.classifier = classifier; 
            sourceBounds = new Rectangle(
                    source.getMinX(), source.getMinY(),
                    source.getWidth(), source.getHeight());           
            if (transform == null) {
                inverseTrans = new AffineTransform();
                zoneBounds = sourceBounds;
            } else {
                try {
                    inverseTrans = transform.createInverse();
                    zoneBounds = inverseTrans.createTransformedShape(sourceBounds).getBounds();
                } catch (NoninvertibleTransformException ex) {
                    LOGGER.warning("The transformation matrix is non-invertible.");
                }
            }
            
            iterator = RandomIterFactory.create(classifier, zoneBounds, false, true);
            
        }
        // If transformation is present a flag is set
        transformation = classPresent && (inverseTrans != null) && !inverseTrans.isIdentity();
        
        // Creation of the spatial index
        spatial = new Quadtree();
        //Creation of a ZoneGeometry list, for storing the results
        zoneList = new ArrayList<ZoneGeometry>();
        // Bounds Union
        union = new Rectangle(rois.get(0).getBounds());
        // Insertion of the zones to the spatial index and union of the bounds for every ROI/Zone object
        for (int i = 0; i < rois.size(); i++) {            
            ROI roi = rois.get(i);
            
            // Spatial index creation
            Rectangle rect = roi.getBounds();
            double minX = rect.getMinX();
            double maxX = rect.getMaxX();
            double minY = rect.getMinY();
            double maxY = rect.getMaxY();
            Envelope env = new Envelope(minX, maxX, minY, maxY);
            spatial.insert(env, roi);
            // Union
            union.union(rect);
            // Creation of a new ZoneGeometry
            ZoneGeometry geom = new ZoneGeometry(i, bands, statsTypes, classPresent);
            
            zoneList.add(geom);
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
            hasROI = false;
            this.useROIAccessor = false;
            roiBounds = null;
            roiIter = null;
            srcROIImage = null;
        }

        // Creation of a lookuptable containing the values to use for no data
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
    }

    public Raster computeTile(int tileX, int tileY) {

        Raster tile = getSourceImage(0).getTile(tileX, tileY);

        Rectangle tileRect = tile.getBounds();

        if (union.intersects(tileRect)) {
            // STATISTICAL ELABORATIONS
            // selection of the format tags
            RasterFormatTag[] formatTags = getFormatTags();
            
            Rectangle computableArea = union.intersection(tileRect);
            
            // creation of the RasterAccessor
            RasterAccessor src = new RasterAccessor(tile, computableArea, formatTags[0], getSourceImage(0)
                    .getColorModel());

            // ROI calculations if roiAccessor is used
            RasterAccessor roi = null;
            if (useROIAccessor) {
                Raster roiRaster = srcROIImage.getExtendedData(computableArea, ROI_EXTENDER);

                // creation of the rasterAccessor
                roi = new RasterAccessor(roiRaster, computableArea, RasterAccessor.findCompatibleTags(
                        new RenderedImage[] { srcROIImage }, srcROIImage)[0],
                        srcROIImage.getColorModel());
            }
            
            int dataType = tile.getSampleModel().getDataType();
            
            switch(dataType){
            case DataBuffer.TYPE_BYTE:
                byteLoop(src,computableArea,roi);
                break;
            case DataBuffer.TYPE_USHORT:
                ushortLoop(src,computableArea,roi);
                break;
            case DataBuffer.TYPE_SHORT:
                shortLoop(src,computableArea,roi);
                break;
            case DataBuffer.TYPE_INT:
                intLoop(src,computableArea,roi);
                break;
            case DataBuffer.TYPE_FLOAT:
                floatLoop(src,computableArea,roi);
                break;
            case DataBuffer.TYPE_DOUBLE:
                doubleLoop(src,computableArea,roi);
                break;
                default:
                    throw new IllegalArgumentException("Wrong data type");
            }
        }
        
        return tile;
    }

    private void byteLoop(RasterAccessor src, Rectangle computableArea, RasterAccessor roi) {
        
        // Source RasterAccessor initial positions
        int srcX = src.getX();
        int srcY = src.getY();

//        final byte[] roiDataArray;
//        final int roiScanLineInc;
//        final int roiDataLength;
//
//        if (useROIAccessor) {
//            roiDataArray = roi.getByteDataArray(0);
//            roiScanLineInc = roi.getScanlineStride();
//            roiDataLength = roiDataArray.length;
//        } else {
//            roiDataArray = null;
//            roiScanLineInc = 0;
//            roiDataLength = 0;
//        }

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

                    // Cycle on the selected Bands
//                    for (int i = 0; i < selectedBands; i++) {
//                        byte sample = srcData[bands[i]][posx + posy + srcBandOffsets[bands[i]]];
//                        for (int j = 0; j < statNum; j++) {
//                            // Update of all the statistics
//                            statArray[i][j].addSampleNoNaN(sample, true);
//                        }
//                    }
                }
            }
            // ONLY ROI IS PRESENT
        }
        
        
        
        
        
    }

    private void ushortLoop(RasterAccessor src, Rectangle computableArea, RasterAccessor roi) {
        // TODO Auto-generated method stub
        
    }

    private void shortLoop(RasterAccessor src, Rectangle computableArea, RasterAccessor roi) {
        // TODO Auto-generated method stub
        
    }

    private void intLoop(RasterAccessor src, Rectangle computableArea, RasterAccessor roi) {
        // TODO Auto-generated method stub
        
    }

    private void floatLoop(RasterAccessor src, Rectangle computableArea, RasterAccessor roi) {
        // TODO Auto-generated method stub
        
    }

    private void doubleLoop(RasterAccessor src, Rectangle computableArea, RasterAccessor roi) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Rectangle mapDestRect(Rectangle destRect, int index) {
        return destRect;
    }

    @Override
    public Rectangle mapSourceRect(Rectangle sourceRect, int index) {
        if (transformation) {
            Rectangle transformed = inverseTrans.createTransformedShape(sourceRect).getBounds();
            return transformed;
        } else {
            return sourceRect;
        }
    }

    /**
     * Returns a list of property names that are recognized by this image.
     * 
     * @return An array of <code>String</code>s containing valid property names.
     */
    public String[] getPropertyNames() {
        // Get statistics names and names from superclass.
        String[] statsNames = new String[] { ZONAL_STATS_PROPERTY };
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
        if (Statistics.STATS_PROPERTY.equalsIgnoreCase(name)) {
            getTiles();
            return null;//TODO   THE RESULT MUST BE RETURNED
                        //FIXME  THE RESULT MUST BE RETURNED
        } else {
            return super.getProperty(name);
        }
    }

}
