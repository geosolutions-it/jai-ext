package it.geosolutions.jaiext.stats;

import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.stats.Statistics.StatsType;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.iterator.RandomIter;
import com.sun.media.jai.util.PropertyUtil;

/**
 * The SimpleStatsOpImage class performs various statistics operations on an image. The various statistical operation are indicated by the
 * {@link StatsType} class. These operations can be calculated together by adding an entry in the definition array "statsTypes". A ROI object passed
 * to the constructor is taken into account by counting only the samples inside of it; an eventual No Data Range is considered by counting only values
 * that are not No Data. The statistical calculation is performed by calling the getProperty() method. The statistics are calculated for every image
 * tile and then the partial results are accumulated and passed to the getProperty() method. For avoiding unnecessary calculations the statistics can
 * be calculated only the first time; but if the user needs to re-calculate the statistics, they can be cleared with the clearStatistic() method and
 * then returned by calling again the getProperty() method.
 */
public class SimpleStatsOpImage extends OpImage {

    /** ROI extender */
    private final static BorderExtender ROI_EXTENDER = BorderExtender
            .createInstance(BorderExtender.BORDER_ZERO);

    /** Object containing the current statistics for the selected bands and for the selected statistic types */
    private final Statistics[][] stats;

    /** Boolean indicating if a No Data Range is used */
    private final boolean hasNoData;

    /** Boolean indicating if a ROI object is used */
    private final boolean hasROI;

    /** Boolean indicating if a ROI RasterAccessor should be used */
    private final boolean useROIAccessor;

    /** ROI image */
    private final PlanarImage srcROIImage;

    /** Random Iterator used iterating on the ROI data */
    private final RandomIter roiIter;

    /** Rectangle containing ROI bounds */
    private final Rectangle roiBounds;

    /** Boolean indicating that there No Data and ROI are not used */
    private final boolean caseA;

    /** Boolean indicating that only ROI is used */
    private final boolean caseB;

    /** Boolean indicating that only No Data are used */
    private final boolean caseC;

    /** Image bands number */
    private int bandsNumber;

    /** Selected bands number */
    private int selectedBands;

    /** Array containing the indexes of the selected bands */
    private final int[] bands;

    /** Array containing the type of statistics to calculate */
    private final StatsType[] statsTypes;

    /** Length of the statsTypes array */
    private final int statNum;

    /** Boolean indicating if the statistics have been already calculated(if false) or not */
    private volatile boolean firstTime = true;

    /** Horizontal subsampling */
    private final int xPeriod;

    /** Vertical subsampling */
    private final int yPeriod;

    /** Boolean lookuptable used if no data are present */
    protected final boolean[] booleanLookupTable = new boolean[256];

    /** No Data Range */
    protected Range noData;

    public SimpleStatsOpImage(RenderedImage source, ImageLayout layout, Map configuration,
            int xPeriod, int yPeriod, ROI roi, Range noData, boolean useROIAccessor, int[] bands,
            StatsType[] statsTypes) {
        super(vectorize(source), layout, configuration, true);

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
                            "Band index cannot be more than the Image bands");
                }
                if (bands[i] < 0) {
                    throw new IllegalArgumentException("Band index cannot be less than 0");
                }
            }
        }
        // definition and check of the sampling parameters and position parameters
        if (xPeriod < 1 || yPeriod < 1) {
            throw new UnsupportedOperationException("Oversampling cannot be calculated");
        }
        this.xPeriod = xPeriod;
        this.yPeriod = yPeriod;

        // Number of statistics calculated
        this.statNum = statsTypes.length;
        // Storage of the statistic types indexes
        this.statsTypes = statsTypes;

        // Storage of the band indexes and length
        this.bands = bands;
        this.selectedBands = selectedBands;

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
            this.noData = noData;
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
            roiBounds = null;
            roiIter = null;
            srcROIImage = null;
        }

        // Creation of a lookuptable containing the values to use for no data
        if (hasNoData && source.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE) {
            for (int i = 0; i < booleanLookupTable.length; i++) {
                byte value = (byte) i;
                booleanLookupTable[i] = !noData.contains(value);
            }
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
        // STATISTICAL ELABORATIONS
        // selection of the format tags
        RasterFormatTag[] formatTags = getFormatTags();
        // Selection of the RasterAccessor parameters
        Raster source = getSourceImage(0).getTile(tileX, tileY);
        // Control if the Period is bigger than the tile dimension, in that case, the
        // statistics are not updated
        if (xPeriod > getTileWidth() || yPeriod > getTileHeight()) {
            return source;
        }

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
            byteLoop(src, srcRect, roi);
            break;
        case DataBuffer.TYPE_USHORT:
            ushortLoop(src, srcRect, roi);
            break;
        case DataBuffer.TYPE_SHORT:
            shortLoop(src, srcRect, roi);
            break;
        case DataBuffer.TYPE_INT:
            intLoop(src, srcRect, roi);
            break;
        case DataBuffer.TYPE_FLOAT:
            floatLoop(src, srcRect, roi);
            break;
        case DataBuffer.TYPE_DOUBLE:
            doubleLoop(src, srcRect, roi);
            break;
        }
        return source;
    }

    private void byteLoop(RasterAccessor src, Rectangle srcRect, RasterAccessor roi) {

        // Creation of local objects containing the same statistics as the initials

        Statistics[][] statArray = new Statistics[selectedBands][statNum];
        // Filling of the container
        for (int i = 0; i < selectedBands; i++) {
            for (int j = 0; j < statNum; j++) {
                statArray[i][j] = StatsFactory.createStatisticsObjectFromInt(statsTypes[j]
                        .getStatsId());
            }
        }
        // Source RasterAccessor initial positions
        int srcX = src.getX();
        int srcY = src.getY();

        final byte[] roiDataArray;
        final int roiScanLineInc;
        final int roiDataLength;

        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiScanLineInc = roi.getScanlineStride() * yPeriod;
            roiDataLength = roiDataArray.length;
        } else {
            roiDataArray = null;
            roiScanLineInc = 0;
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
            for (int y = 0; y < srcHeight; y += yPeriod) {
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x += xPeriod) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;

                    // Cycle on the selected Bands
                    for (int i = 0; i < selectedBands; i++) {
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
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y += yPeriod) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // roi y position
                    int posyROI = y * roiScanLineInc;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x += xPeriod) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // ROI index position
                        int windex = x * xPeriod + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // Control if the sample is inside ROI
                        if (w != 0) {
                            // Cycle on the selected Bands
                            for (int i = 0; i < selectedBands; i++) {
                                byte sample = srcData[bands[i]][posx + posy
                                        + srcBandOffsets[bands[i]]];
                                for (int j = 0; j < statNum; j++) {
                                    // Update of all the statistics
                                    statArray[i][j].addSampleNoNaN(sample, true);
                                }
                            }
                        }
                    }
                }
                // ROI RasterAccessor is not used
            } else {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y += yPeriod) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x += xPeriod) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        // Control if the sample is inside ROI
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0) & 0xff;
                            if (w != 0) {
                                // Cycle on the selected Bands
                                for (int i = 0; i < selectedBands; i++) {
                                    byte sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    for (int j = 0; j < statNum; j++) {
                                        // Update of all the statistics
                                        statArray[i][j].addSampleNoNaN(sample, true);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // ONLY NO DATA ARE PRESENT
        } else if (caseC) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y += yPeriod) {
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x += xPeriod) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;
                    // Cycle on the selected Bands
                    for (int i = 0; i < selectedBands; i++) {
                        byte sample = srcData[bands[i]][posx + posy + srcBandOffsets[bands[i]]];
                        // Control if the sample is Not a NO Data
                        boolean isData = booleanLookupTable[sample & 0xFF];
                        for (int j = 0; j < statNum; j++) {
                            // Update of all the statistics
                            statArray[i][j].addSampleNoNaN(sample, isData);
                        }
                    }
                }
            }
            // BOTH NO DATA AND ROI ARE PRESENT
        } else {
            // ROI RasterAccessor is used
            if (useROIAccessor) {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y += yPeriod) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // roi y position
                    int posyROI = y * roiScanLineInc;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x += xPeriod) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // ROI index position
                        int windex = x * xPeriod + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // Control if the sample is inside ROI
                        if (w != 0) {
                            // Cycle on the selected Bands
                            for (int i = 0; i < selectedBands; i++) {
                                byte sample = srcData[bands[i]][posx + posy
                                        + srcBandOffsets[bands[i]]];
                                // Control if the sample is Not a NO Data
                                boolean isData = booleanLookupTable[sample & 0xFF];
                                for (int j = 0; j < statNum; j++) {
                                    // Update of all the statistics
                                    statArray[i][j].addSampleNoNaN(sample, isData);
                                }
                            }
                        }
                    }
                }
                // ROI RasterAccessor is not used
            } else {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y += yPeriod) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x += xPeriod) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        // Control if the sample is inside ROI
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0) & 0xff;
                            if (w != 0) {
                                // Cycle on the selected Bands
                                for (int i = 0; i < selectedBands; i++) {
                                    byte sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // Control if the sample is Not a NO Data
                                    boolean isData = booleanLookupTable[sample & 0xFF];
                                    for (int j = 0; j < statNum; j++) {
                                        // Update of all the statistics
                                        statArray[i][j].addSampleNoNaN(sample, isData);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cumulative addition (SYNCHRONIZED)

        synchronized (this) {
            // Cycle on the selected Bands
            for (int i = 0; i < selectedBands; i++) {
                for (int j = 0; j < statNum; j++) {
                    // Accumulation for the selected band and the selected statistic
                    stats[i][j].accumulateStats(statArray[i][j]);
                }
            }
        }
    }

    private void ushortLoop(RasterAccessor src, Rectangle srcRect, RasterAccessor roi) {

        // Creation of local objects containing the same statistics as the initials

        Statistics[][] statArray = new Statistics[selectedBands][statNum];
        // Filling of the container
        for (int i = 0; i < selectedBands; i++) {
            for (int j = 0; j < statNum; j++) {
                statArray[i][j] = StatsFactory.createStatisticsObjectFromInt(statsTypes[j]
                        .getStatsId());
            }
        }
        // Source RasterAccessor initial positions
        int srcX = src.getX();
        int srcY = src.getY();

        final byte[] roiDataArray;
        final int roiScanLineInc;
        final int roiDataLength;

        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiScanLineInc = roi.getScanlineStride() * yPeriod;
            roiDataLength = roiDataArray.length;
        } else {
            roiDataArray = null;
            roiScanLineInc = 0;
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
            for (int y = 0; y < srcHeight; y += yPeriod) {
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x += xPeriod) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;

                    // Cycle on the selected Bands
                    for (int i = 0; i < selectedBands; i++) {
                        int sample = srcData[bands[i]][posx + posy + srcBandOffsets[bands[i]]] & 0xFFFF;
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
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y += yPeriod) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // roi y position
                    int posyROI = y * roiScanLineInc;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x += xPeriod) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // ROI index position
                        int windex = x * xPeriod + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // Control if the sample is inside ROI
                        if (w != 0) {
                            // Cycle on the selected Bands
                            for (int i = 0; i < selectedBands; i++) {
                                int sample = srcData[bands[i]][posx + posy
                                        + srcBandOffsets[bands[i]]] & 0xFFFF;
                                for (int j = 0; j < statNum; j++) {
                                    // Update of all the statistics
                                    statArray[i][j].addSampleNoNaN(sample, true);
                                }
                            }
                        }
                    }
                }
                // ROI RasterAccessor is not used
            } else {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y += yPeriod) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x += xPeriod) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        // Control if the sample is inside ROI
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0) & 0xff;
                            if (w != 0) {
                                // Cycle on the selected Bands
                                for (int i = 0; i < selectedBands; i++) {
                                    int sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]] & 0xFFFF;
                                    for (int j = 0; j < statNum; j++) {
                                        // Update of all the statistics
                                        statArray[i][j].addSampleNoNaN(sample, true);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // ONLY NO DATA ARE PRESENT
        } else if (caseC) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y += yPeriod) {
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x += xPeriod) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;
                    // Cycle on the selected Bands
                    for (int i = 0; i < selectedBands; i++) {
                        int sample = srcData[bands[i]][posx + posy + srcBandOffsets[bands[i]]] & 0xFFFF;
                        // Control if the sample is Not a NO Data
                        boolean isData = !noData.contains((short)sample);
                        for (int j = 0; j < statNum; j++) {
                            // Update of all the statistics
                            statArray[i][j].addSampleNoNaN(sample, isData);
                        }
                    }
                }
            }
            // BOTH NO DATA AND ROI ARE PRESENT
        } else {
            // ROI RasterAccessor is used
            if (useROIAccessor) {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y += yPeriod) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // roi y position
                    int posyROI = y * roiScanLineInc;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x += xPeriod) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // ROI index position
                        int windex = x * xPeriod + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // Control if the sample is inside ROI
                        if (w != 0) {
                            // Cycle on the selected Bands
                            for (int i = 0; i < selectedBands; i++) {
                                int sample = srcData[bands[i]][posx + posy
                                        + srcBandOffsets[bands[i]]] & 0xFFFF;
                                // Control if the sample is Not a NO Data
                                boolean isData = !noData.contains((short)sample);
                                for (int j = 0; j < statNum; j++) {
                                    // Update of all the statistics
                                    statArray[i][j].addSampleNoNaN(sample, isData);
                                }
                            }
                        }
                    }
                }
                // ROI RasterAccessor is not used
            } else {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y += yPeriod) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x += xPeriod) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        // Control if the sample is inside ROI
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0) & 0xff;
                            if (w != 0) {
                                // Cycle on the selected Bands
                                for (int i = 0; i < selectedBands; i++) {
                                    int sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]] & 0xFFFF;
                                    // Control if the sample is Not a NO Data
                                    boolean isData = !noData.contains((short)sample);
                                    for (int j = 0; j < statNum; j++) {
                                        // Update of all the statistics
                                        statArray[i][j].addSampleNoNaN(sample, isData);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cumulative addition (SYNCHRONIZED)

        synchronized (this) {
            // Cycle on the selected Bands
            for (int i = 0; i < selectedBands; i++) {
                for (int j = 0; j < statNum; j++) {
                    // Accumulation for the selected band and the selected statistic
                    stats[i][j].accumulateStats(statArray[i][j]);
                }
            }
        }
    }

    private void shortLoop(RasterAccessor src, Rectangle srcRect, RasterAccessor roi) {

        // Creation of local objects containing the same statistics as the initials

        Statistics[][] statArray = new Statistics[selectedBands][statNum];
        // Filling of the container
        for (int i = 0; i < selectedBands; i++) {
            for (int j = 0; j < statNum; j++) {
                statArray[i][j] = StatsFactory.createStatisticsObjectFromInt(statsTypes[j]
                        .getStatsId());
            }
        }
        // Source RasterAccessor initial positions
        int srcX = src.getX();
        int srcY = src.getY();

        final byte[] roiDataArray;
        final int roiScanLineInc;
        final int roiDataLength;

        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiScanLineInc = roi.getScanlineStride() * yPeriod;
            roiDataLength = roiDataArray.length;
        } else {
            roiDataArray = null;
            roiScanLineInc = 0;
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
            for (int y = 0; y < srcHeight; y += yPeriod) {
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x += xPeriod) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;

                    // Cycle on the selected Bands
                    for (int i = 0; i < selectedBands; i++) {
                        short sample = srcData[bands[i]][posx + posy + srcBandOffsets[bands[i]]];
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
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y += yPeriod) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // roi y position
                    int posyROI = y * roiScanLineInc;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x += xPeriod) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // ROI index position
                        int windex = x * xPeriod + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // Control if the sample is inside ROI
                        if (w != 0) {
                            // Cycle on the selected Bands
                            for (int i = 0; i < selectedBands; i++) {
                                short sample = srcData[bands[i]][posx + posy
                                        + srcBandOffsets[bands[i]]];
                                for (int j = 0; j < statNum; j++) {
                                    // Update of all the statistics
                                    statArray[i][j].addSampleNoNaN(sample, true);
                                }
                            }
                        }
                    }
                }
                // ROI RasterAccessor is not used
            } else {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y += yPeriod) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x += xPeriod) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        // Control if the sample is inside ROI
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0) & 0xff;
                            if (w != 0) {
                                // Cycle on the selected Bands
                                for (int i = 0; i < selectedBands; i++) {
                                    short sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    for (int j = 0; j < statNum; j++) {
                                        // Update of all the statistics
                                        statArray[i][j].addSampleNoNaN(sample, true);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // ONLY NO DATA ARE PRESENT
        } else if (caseC) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y += yPeriod) {
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x += xPeriod) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;
                    // Cycle on the selected Bands
                    for (int i = 0; i < selectedBands; i++) {
                        short sample = srcData[bands[i]][posx + posy + srcBandOffsets[bands[i]]];
                        // Control if the sample is Not a NO Data
                        boolean isData = !noData.contains(sample);
                        for (int j = 0; j < statNum; j++) {
                            // Update of all the statistics
                            statArray[i][j].addSampleNoNaN(sample, isData);
                        }
                    }
                }
            }
            // BOTH NO DATA AND ROI ARE PRESENT
        } else {
            // ROI RasterAccessor is used
            if (useROIAccessor) {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y += yPeriod) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // roi y position
                    int posyROI = y * roiScanLineInc;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x += xPeriod) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // ROI index position
                        int windex = x * xPeriod + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // Control if the sample is inside ROI
                        if (w != 0) {
                            // Cycle on the selected Bands
                            for (int i = 0; i < selectedBands; i++) {
                                short sample = srcData[bands[i]][posx + posy
                                        + srcBandOffsets[bands[i]]];
                                // Control if the sample is Not a NO Data
                                boolean isData = !noData.contains(sample);
                                for (int j = 0; j < statNum; j++) {
                                    // Update of all the statistics
                                    statArray[i][j].addSampleNoNaN(sample, isData);
                                }
                            }
                        }
                    }
                }
                // ROI RasterAccessor is not used
            } else {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y += yPeriod) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x += xPeriod) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        // Control if the sample is inside ROI
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0) & 0xff;
                            if (w != 0) {
                                // Cycle on the selected Bands
                                for (int i = 0; i < selectedBands; i++) {
                                    short sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // Control if the sample is Not a NO Data
                                    boolean isData = !noData.contains(sample);
                                    for (int j = 0; j < statNum; j++) {
                                        // Update of all the statistics
                                        statArray[i][j].addSampleNoNaN(sample, isData);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cumulative addition (SYNCHRONIZED)

        synchronized (this) {
            // Cycle on the selected Bands
            for (int i = 0; i < selectedBands; i++) {
                for (int j = 0; j < statNum; j++) {
                    // Accumulation for the selected band and the selected statistic
                    stats[i][j].accumulateStats(statArray[i][j]);
                }
            }
        }
    }

    private void intLoop(RasterAccessor src, Rectangle srcRect, RasterAccessor roi) {

        // Creation of local objects containing the same statistics as the initials

        Statistics[][] statArray = new Statistics[selectedBands][statNum];
        // Filling of the container
        for (int i = 0; i < selectedBands; i++) {
            for (int j = 0; j < statNum; j++) {
                statArray[i][j] = StatsFactory.createStatisticsObjectFromInt(statsTypes[j]
                        .getStatsId());
            }
        }
        // Source RasterAccessor initial positions
        int srcX = src.getX();
        int srcY = src.getY();

        final byte[] roiDataArray;
        final int roiScanLineInc;
        final int roiDataLength;

        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiScanLineInc = roi.getScanlineStride() * yPeriod;
            roiDataLength = roiDataArray.length;
        } else {
            roiDataArray = null;
            roiScanLineInc = 0;
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
            for (int y = 0; y < srcHeight; y += yPeriod) {
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x += xPeriod) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;

                    // Cycle on the selected Bands
                    for (int i = 0; i < selectedBands; i++) {
                        int sample = srcData[bands[i]][posx + posy + srcBandOffsets[bands[i]]];
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
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y += yPeriod) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // roi y position
                    int posyROI = y * roiScanLineInc;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x += xPeriod) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // ROI index position
                        int windex = x * xPeriod + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // Control if the sample is inside ROI
                        if (w != 0) {
                            // Cycle on the selected Bands
                            for (int i = 0; i < selectedBands; i++) {
                                int sample = srcData[bands[i]][posx + posy
                                        + srcBandOffsets[bands[i]]];
                                for (int j = 0; j < statNum; j++) {
                                    // Update of all the statistics
                                    statArray[i][j].addSampleNoNaN(sample, true);
                                }
                            }
                        }
                    }
                }
                // ROI RasterAccessor is not used
            } else {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y += yPeriod) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x += xPeriod) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        // Control if the sample is inside ROI
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0) & 0xff;
                            if (w != 0) {
                                // Cycle on the selected Bands
                                for (int i = 0; i < selectedBands; i++) {
                                    int sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    for (int j = 0; j < statNum; j++) {
                                        // Update of all the statistics
                                        statArray[i][j].addSampleNoNaN(sample, true);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // ONLY NO DATA ARE PRESENT
        } else if (caseC) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y += yPeriod) {
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x += xPeriod) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;
                    // Cycle on the selected Bands
                    for (int i = 0; i < selectedBands; i++) {
                        int sample = srcData[bands[i]][posx + posy + srcBandOffsets[bands[i]]];
                        // Control if the sample is Not a NO Data
                        boolean isData = !noData.contains(sample);
                        for (int j = 0; j < statNum; j++) {
                            // Update of all the statistics
                            statArray[i][j].addSampleNoNaN(sample, isData);
                        }
                    }
                }
            }
            // BOTH NO DATA AND ROI ARE PRESENT
        } else {
            // ROI RasterAccessor is used
            if (useROIAccessor) {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y += yPeriod) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // roi y position
                    int posyROI = y * roiScanLineInc;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x += xPeriod) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // ROI index position
                        int windex = x * xPeriod + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // Control if the sample is inside ROI
                        if (w != 0) {
                            // Cycle on the selected Bands
                            for (int i = 0; i < selectedBands; i++) {
                                int sample = srcData[bands[i]][posx + posy
                                        + srcBandOffsets[bands[i]]];
                                // Control if the sample is Not a NO Data
                                boolean isData = !noData.contains(sample);
                                for (int j = 0; j < statNum; j++) {
                                    // Update of all the statistics
                                    statArray[i][j].addSampleNoNaN(sample, isData);
                                }
                            }
                        }
                    }
                }
                // ROI RasterAccessor is not used
            } else {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y += yPeriod) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x += xPeriod) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        // Control if the sample is inside ROI
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0) & 0xff;
                            if (w != 0) {
                                // Cycle on the selected Bands
                                for (int i = 0; i < selectedBands; i++) {
                                    int sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // Control if the sample is Not a NO Data
                                    boolean isData = !noData.contains(sample);
                                    for (int j = 0; j < statNum; j++) {
                                        // Update of all the statistics
                                        statArray[i][j].addSampleNoNaN(sample, isData);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cumulative addition (SYNCHRONIZED)

        synchronized (this) {
            // Cycle on the selected Bands
            for (int i = 0; i < selectedBands; i++) {
                for (int j = 0; j < statNum; j++) {
                    // Accumulation for the selected band and the selected statistic
                    stats[i][j].accumulateStats(statArray[i][j]);
                }
            }
        }
    }

    private void floatLoop(RasterAccessor src, Rectangle srcRect, RasterAccessor roi) {

        // Creation of local objects containing the same statistics as the initials

        Statistics[][] statArray = new Statistics[selectedBands][statNum];
        // Filling of the container
        for (int i = 0; i < selectedBands; i++) {
            for (int j = 0; j < statNum; j++) {
                statArray[i][j] = StatsFactory.createStatisticsObjectFromInt(statsTypes[j]
                        .getStatsId());
            }
        }
        // Source RasterAccessor initial positions
        int srcX = src.getX();
        int srcY = src.getY();

        final byte[] roiDataArray;
        final int roiScanLineInc;
        final int roiDataLength;

        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiScanLineInc = roi.getScanlineStride() * yPeriod;
            roiDataLength = roiDataArray.length;
        } else {
            roiDataArray = null;
            roiScanLineInc = 0;
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
            for (int y = 0; y < srcHeight; y += yPeriod) {
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x += xPeriod) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;

                    // Cycle on the selected Bands
                    for (int i = 0; i < selectedBands; i++) {
                        float sample = srcData[bands[i]][posx + posy + srcBandOffsets[bands[i]]];
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
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y += yPeriod) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // roi y position
                    int posyROI = y * roiScanLineInc;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x += xPeriod) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // ROI index position
                        int windex = x * xPeriod + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // Control if the sample is inside ROI
                        if (w != 0) {
                            // Cycle on the selected Bands
                            for (int i = 0; i < selectedBands; i++) {
                                float sample = srcData[bands[i]][posx + posy
                                        + srcBandOffsets[bands[i]]];
                                for (int j = 0; j < statNum; j++) {
                                    // Update of all the statistics
                                    statArray[i][j].addSampleNoNaN(sample, true);
                                }
                            }
                        }
                    }
                }
                // ROI RasterAccessor is not used
            } else {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y += yPeriod) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x += xPeriod) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        // Control if the sample is inside ROI
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0) & 0xff;
                            if (w != 0) {
                                // Cycle on the selected Bands
                                for (int i = 0; i < selectedBands; i++) {
                                    float sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    for (int j = 0; j < statNum; j++) {
                                        // Update of all the statistics
                                        statArray[i][j].addSampleNoNaN(sample, true);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // ONLY NO DATA ARE PRESENT
        } else if (caseC) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y += yPeriod) {
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x += xPeriod) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;
                    // Cycle on the selected Bands
                    for (int i = 0; i < selectedBands; i++) {
                        float sample = srcData[bands[i]][posx + posy + srcBandOffsets[bands[i]]];
                        // Control if the sample is Not a NO Data
                        boolean isData = !noData.contains(sample);
                        boolean isNaN = Float.isNaN(sample);
                        for (int j = 0; j < statNum; j++) {
                            // Update of all the statistics
                            statArray[i][j].addSampleNaN(sample, isData, isNaN);
                        }
                    }
                }
            }
            // BOTH NO DATA AND ROI ARE PRESENT
        } else {
            // ROI RasterAccessor is used
            if (useROIAccessor) {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y += yPeriod) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // roi y position
                    int posyROI = y * roiScanLineInc;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x += xPeriod) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // ROI index position
                        int windex = x * xPeriod + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // Control if the sample is inside ROI
                        if (w != 0) {
                            // Cycle on the selected Bands
                            for (int i = 0; i < selectedBands; i++) {
                                float sample = srcData[bands[i]][posx + posy
                                        + srcBandOffsets[bands[i]]];
                                // Control if the sample is Not a NO Data
                                boolean isData = !noData.contains(sample);
                                boolean isNaN = Float.isNaN(sample);
                                for (int j = 0; j < statNum; j++) {
                                    // Update of all the statistics
                                    statArray[i][j].addSampleNaN(sample, isData, isNaN);
                                }
                            }
                        }
                    }
                }
                // ROI RasterAccessor is not used
            } else {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y += yPeriod) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x += xPeriod) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        // Control if the sample is inside ROI
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0) & 0xff;
                            if (w != 0) {
                                // Cycle on the selected Bands
                                for (int i = 0; i < selectedBands; i++) {
                                    float sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // Control if the sample is Not a NO Data
                                    boolean isData = !noData.contains(sample);
                                    boolean isNaN = Float.isNaN(sample);
                                    for (int j = 0; j < statNum; j++) {
                                        // Update of all the statistics
                                        statArray[i][j].addSampleNaN(sample, isData, isNaN);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cumulative addition (SYNCHRONIZED)

        synchronized (this) {
            // Cycle on the selected Bands
            for (int i = 0; i < selectedBands; i++) {
                for (int j = 0; j < statNum; j++) {
                    // Accumulation for the selected band and the selected statistic
                    stats[i][j].accumulateStats(statArray[i][j]);
                }
            }
        }
    }

    private void doubleLoop(RasterAccessor src, Rectangle srcRect, RasterAccessor roi) {

        // Creation of local objects containing the same statistics as the initials

        Statistics[][] statArray = new Statistics[selectedBands][statNum];
        // Filling of the container
        for (int i = 0; i < selectedBands; i++) {
            for (int j = 0; j < statNum; j++) {
                statArray[i][j] = StatsFactory.createStatisticsObjectFromInt(statsTypes[j]
                        .getStatsId());
            }
        }
        // Source RasterAccessor initial positions
        int srcX = src.getX();
        int srcY = src.getY();

        final byte[] roiDataArray;
        final int roiScanLineInc;
        final int roiDataLength;

        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiScanLineInc = roi.getScanlineStride() * yPeriod;
            roiDataLength = roiDataArray.length;
        } else {
            roiDataArray = null;
            roiScanLineInc = 0;
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
            for (int y = 0; y < srcHeight; y += yPeriod) {
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x += xPeriod) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;

                    // Cycle on the selected Bands
                    for (int i = 0; i < selectedBands; i++) {
                        double sample = srcData[bands[i]][posx + posy + srcBandOffsets[bands[i]]];
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
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y += yPeriod) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // roi y position
                    int posyROI = y * roiScanLineInc;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x += xPeriod) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // ROI index position
                        int windex = x * xPeriod + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // Control if the sample is inside ROI
                        if (w != 0) {
                            // Cycle on the selected Bands
                            for (int i = 0; i < selectedBands; i++) {
                                double sample = srcData[bands[i]][posx + posy
                                        + srcBandOffsets[bands[i]]];
                                for (int j = 0; j < statNum; j++) {
                                    // Update of all the statistics
                                    statArray[i][j].addSampleNoNaN(sample, true);
                                }
                            }
                        }
                    }
                }
                // ROI RasterAccessor is not used
            } else {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y += yPeriod) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x += xPeriod) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        // Control if the sample is inside ROI
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0) & 0xff;
                            if (w != 0) {
                                // Cycle on the selected Bands
                                for (int i = 0; i < selectedBands; i++) {
                                    double sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    for (int j = 0; j < statNum; j++) {
                                        // Update of all the statistics
                                        statArray[i][j].addSampleNoNaN(sample, true);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // ONLY NO DATA ARE PRESENT
        } else if (caseC) {
            // Cycle on the y axis
            for (int y = 0; y < srcHeight; y += yPeriod) {
                // y position on the source data array
                int posy = y * srcScanlineStride;
                // Cycle on the x axis
                for (int x = 0; x < srcWidth; x += xPeriod) {
                    // x position on the source data array
                    int posx = x * srcPixelStride;
                    // Cycle on the selected Bands
                    for (int i = 0; i < selectedBands; i++) {
                        double sample = srcData[bands[i]][posx + posy + srcBandOffsets[bands[i]]];
                        // Control if the sample is Not a NO Data
                        boolean isData = !noData.contains(sample);
                        boolean isNaN = Double.isNaN(sample);
                        for (int j = 0; j < statNum; j++) {
                            // Update of all the statistics
                            statArray[i][j].addSampleNaN(sample, isData, isNaN);
                        }
                    }
                }
            }
            // BOTH NO DATA AND ROI ARE PRESENT
        } else {
            // ROI RasterAccessor is used
            if (useROIAccessor) {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y += yPeriod) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // roi y position
                    int posyROI = y * roiScanLineInc;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x += xPeriod) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // ROI index position
                        int windex = x * xPeriod + posyROI;
                        // ROI value
                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                        // Control if the sample is inside ROI
                        if (w != 0) {
                            // Cycle on the selected Bands
                            for (int i = 0; i < selectedBands; i++) {
                                double sample = srcData[bands[i]][posx + posy
                                        + srcBandOffsets[bands[i]]];
                                // Control if the sample is Not a NO Data
                                boolean isData = !noData.contains(sample);
                                boolean isNaN = Double.isNaN(sample);
                                for (int j = 0; j < statNum; j++) {
                                    // Update of all the statistics
                                    statArray[i][j].addSampleNaN(sample, isData, isNaN);
                                }
                            }
                        }
                    }
                }
                // ROI RasterAccessor is not used
            } else {
                // Cycle on the y axis
                for (int y = 0; y < srcHeight; y += yPeriod) {
                    // y position on the source data array
                    int posy = y * srcScanlineStride;
                    // Cycle on the x axis
                    for (int x = 0; x < srcWidth; x += xPeriod) {
                        // x position on the source data array
                        int posx = x * srcPixelStride;
                        // PixelPositions
                        int x0 = srcX + x;
                        int y0 = srcY + y;
                        // Control if the sample is inside ROI
                        if (roiBounds.contains(x0, y0)) {
                            // ROI value
                            int w = roiIter.getSample(x0, y0, 0) & 0xff;
                            if (w != 0) {
                                // Cycle on the selected Bands
                                for (int i = 0; i < selectedBands; i++) {
                                    double sample = srcData[bands[i]][posx + posy
                                            + srcBandOffsets[bands[i]]];
                                    // Control if the sample is Not a NO Data
                                    boolean isData = !noData.contains(sample);
                                    boolean isNaN = Double.isNaN(sample);
                                    for (int j = 0; j < statNum; j++) {
                                        // Update of all the statistics
                                        statArray[i][j].addSampleNaN(sample, isData, isNaN);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cumulative addition (SYNCHRONIZED)

        synchronized (this) {
            // Cycle on the selected Bands
            for (int i = 0; i < selectedBands; i++) {
                for (int j = 0; j < statNum; j++) {
                    // Accumulation for the selected band and the selected statistic
                    stats[i][j].accumulateStats(statArray[i][j]);
                }
            }
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
        if (Statistics.SIMPLE_STATS_PROPERTY.equalsIgnoreCase(name)) {
            getTiles();
            return stats.clone();
        } else {
            return super.getProperty(name);
        }
    }

    /**
     * Returns a list of property names that are recognized by this image.
     * 
     * @return An array of <code>String</code>s containing valid property names.
     */
    public String[] getPropertyNames() {
        // Get statistics names and names from superclass.
        String[] statsNames = new String[] { Statistics.SIMPLE_STATS_PROPERTY };
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
     * This method is used if the user needs to perform again the statistical calculations.
     */
    public synchronized void clearStatistic() {
        // Filling of the container
        for (int i = 0; i < stats.length; i++) {
            for (int j = 0; j < statNum; j++) {
                stats[i][j].clearStats();
            }
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

    @Override
    public Rectangle mapDestRect(Rectangle destRect, int sourceIndex) {
        return destRect;
    }

    @Override
    public Rectangle mapSourceRect(Rectangle sourceRect, int sourceIndex) {
        return sourceRect;
    }

}
