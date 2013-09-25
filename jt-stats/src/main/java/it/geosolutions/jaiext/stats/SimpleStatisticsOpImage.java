package it.geosolutions.jaiext.stats;

import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.stats.Statistics.StatsType;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.Map;
import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.NullOpImage;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.iterator.RandomIter;

public class SimpleStatisticsOpImage extends NullOpImage {

    /** ROI extender */
    private final static BorderExtender ROI_EXTENDER = BorderExtender
            .createInstance(BorderExtender.BORDER_ZERO);

    /** Statistics property name */
    private final static String SIMPLE_STATS_PROPERTY = "JAI-EXT.stats";

    private Statistics[][] stats;

    private final boolean hasNoData;

    private final boolean hasROI;

    private final boolean useROIAccessor;

    private PlanarImage srcROIImage;

    private RandomIter roiIter;

    private Rectangle roiBounds;

    private final boolean caseA;

    private final boolean caseB;

    private final boolean caseC;

    private int bandsNumber;

    private final int[] bands;

    private final StatsType[] statsTypes;

    private final int statNum;

    public SimpleStatisticsOpImage(RenderedImage source, ImageLayout layout, Map configuration,
            ROI roi, Range noData, boolean useROIAccessor, StatsType[] statsTypes, int[] bands) {
        super(source, layout, configuration, OpImage.OP_COMPUTE_BOUND);

        // Source Image bands
        bandsNumber = source.getSampleModel().getNumBands();
        // Selected Bands array dimension
        int selectedBands = bands.length;
        // BAND INDEX CONTROL
        if (selectedBands > bandsNumber) {
            throw new IllegalArgumentException(
                    "Number of Bands to analyze cannot be more than the Image bands");
        } else if (selectedBands <= 0) {
            throw new IllegalArgumentException(
                    "Number of Bands to analyze cannot be less or equal to 0");
        } else {
            for (int i = 0; i < selectedBands; i++) {
                if (bands[i] > bandsNumber) {
                    throw new IllegalArgumentException(
                            "Band index cannot be more than tthe Image bands");
                }
                if (bands[i] < 0) {
                    throw new IllegalArgumentException("Band index cannot be less than 0");
                }
            }
        }

        // Number of statistics calculated
        this.statNum = statsTypes.length;
        // Storage of the statistic types indexes
        this.statsTypes = statsTypes;

        // Storage of the band indexes
        this.bands = bands;

        // Creation of a global container of all the selected statistics for every band
        this.stats = new Statistics[selectedBands][statNum];
        // Filling of the container
        for (int i = 0; i < selectedBands; i++) {
            for (int j = 0; j < statNum; j++) {
                stats[i][j] = StatsFactory
                        .createStatisticsObjectFromInt(statsTypes[j].getStatsId());
            }
        }

        // Check if No Data control must be done
        if (noData != null) {
            hasNoData = true;
        } else {
            hasNoData = false;
        }

        // Check if ROI control must be done
        if (roi != null) {
            hasROI = true;
            // Roi object
            ROI srcROI = roi;
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

    /**
     * Returns a tile for reading.
     * 
     * @param tileX The X index of the tile.
     * @param tileY The Y index of the tile.
     * @return The tile as a <code>Raster</code>.
     */
    public Raster computeTile(int tileX, int tileY) {
        return getTile(tileX, tileY);
    }

    /**
     * Returns a tile for reading. And also stores the statistics.
     * 
     * @param tileX The X index of the tile.
     * @param tileY The Y index of the tile.
     * @return The tile as a <code>Raster</code>.
     */
    public Raster getTile(int tileX, int tileY) {

        // STATISTICAL ELABORATIONS
        // selection of the format tags
        RasterFormatTag[] formatTags = getFormatTags();
        // Selection of the RasterAccessor parameters
        Raster source = getSourceImage(0).getTile(tileX, tileY);
        Rectangle srcRect = source.getBounds();
        // creation of the RasterAccessor
        RasterAccessor src = new RasterAccessor(source, srcRect, formatTags[0], getSourceImage(0)
                .getColorModel());

        // ROI calculations if roiAccessor is used
        RasterAccessor roi = null;
        if (useROIAccessor) {
            Raster roiRaster = srcROIImage.getExtendedData(srcRect, ROI_EXTENDER);

            // creation of the rasterAccessor
            roi = new RasterAccessor(roiRaster, srcRect, RasterAccessor.findCompatibleTags(
                    new RenderedImage[] { srcROIImage }, srcROIImage)[0],
                    srcROIImage.getColorModel());
        }
        // Computation of the statistics
        switch (src.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(src, roi);
            break;
        case DataBuffer.TYPE_USHORT:
            ushortLoop(src, roi);
            break;
        case DataBuffer.TYPE_SHORT:
            shortLoop(src, roi);
            break;
        case DataBuffer.TYPE_INT:
            intLoop(src, roi);
            break;
        case DataBuffer.TYPE_FLOAT:
            floatLoop(src, roi);
            break;
        case DataBuffer.TYPE_DOUBLE:
            doubleLoop(src, roi);
            break;
        }
        return source;
    }

    private void byteLoop(RasterAccessor src, RasterAccessor roi) {

        // Creation of local objects containing the same statistics as the initials

        Statistics[][] statArray = new Statistics[bandsNumber][statNum];
        // Filling of the container
        for (int i = 0; i < bandsNumber; i++) {
            for (int j = 0; j < statNum; j++) {
                statArray[i][j] = StatsFactory.createStatisticsObjectFromInt(statsTypes[j]
                        .getStatsId());
            }
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

                    // Cycle on the selected Bands
                    for (int i = 0; i < bandsNumber; i++) {
                        byte sample = srcData[bands[i]][posx + posy + srcBandOffsets[bands[i]]];
                        for (int j = 0; j < statNum; j++) {
                            // Update of all the statistics
                            statArray[i][j].addSampleNoNaN(sample, true);
                        }
                    }
                }
            }
            // ONLY ROI IS PRESENT
        } else if (caseB) {
            // ROI RasterAccessor is used
            if (useROIAccessor) {

                // ROI RasterAccessor is not used
            } else {

            }
            // ONLY NO DATA ARE PRESENT
        } else if (caseC) {

            // BOTH NO DATA AND ROI ARE PRESENT
        } else {
            // ROI RasterAccessor is used
            if (useROIAccessor) {

                // ROI RasterAccessor is not used
            } else {

            }
        }

        
        // Cumulative addition (SYNCHRONIZED)

        synchronized (this) {
            // Cycle on the selected Bands
            for (int i = 0; i < bandsNumber; i++) {
                for (int j = 0; j < statNum; j++) {
                    // Accumulation for the selected band and the selected statistic
                    stats[i][j].accumulateStats(statArray[i][j]);
                }
            }
        }
    }

    private void ushortLoop(RasterAccessor src, RasterAccessor roi) {
        // TODO Auto-generated method stub

    }

    private void shortLoop(RasterAccessor src, RasterAccessor roi) {
        // TODO Auto-generated method stub

    }

    private void intLoop(RasterAccessor src, RasterAccessor roi) {
        // TODO Auto-generated method stub

    }

    private void floatLoop(RasterAccessor src, RasterAccessor roi) {
        // TODO Auto-generated method stub

    }

    private void doubleLoop(RasterAccessor src, RasterAccessor roi) {
        // TODO Auto-generated method stub

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
        if (SIMPLE_STATS_PROPERTY.equalsIgnoreCase(name)) {
            getTiles();
            return stats.clone();
        } else {
            return super.getProperty(name);
        }
    }

    /**
     * When the dispose method is called, then old dispose method is performed and also the statistic container is cleared by setting it to null.
     */
    public void dispose() {
        super.dispose();
        stats = null;
    }

}
