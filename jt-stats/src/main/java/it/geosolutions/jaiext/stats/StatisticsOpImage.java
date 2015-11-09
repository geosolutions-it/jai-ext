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
package it.geosolutions.jaiext.stats;

import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.stats.Statistics.StatsType;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RenderedOp;
import javax.media.jai.iterator.RandomIter;

import com.sun.media.jai.util.PropertyUtil;

/**
 * This abstract class is used for defining the common methods for calculating image statistics. There are 2 subclasses of this class called
 * {@link SimpleStatsOpImage} and {@link ComplexStatsOpImage}. The first one is used for calculating simple statistics which does not requests the use
 * of an array for saving all the values, instead of the second class which stores the statistics on an array and then performs the final computations
 * when the result is requested. The 2 subclasses must only update their constructor for adding a control on which kind of statistics are calculated
 * and defining the computeTile() method which is used for calculating the statistics for each tile.
 */
public abstract class StatisticsOpImage extends OpImage {

    /** ROI extender */
    protected final static BorderExtender ROI_EXTENDER = BorderExtender
            .createInstance(BorderExtender.BORDER_ZERO);

    /** Object containing the current statistics for the selected bands and for the selected statistic types */
    protected Statistics[][] stats;

    /** Boolean indicating if a No Data Range is used */
    protected final boolean hasNoData;

    /** Boolean indicating if a ROI object is used */
    protected final boolean hasROI;

    /** Boolean indicating if a ROI RasterAccessor should be used */
    protected final boolean useROIAccessor;

    /** ROI image */
    protected final PlanarImage srcROIImage;

    /** Rectangle containing ROI bounds */
    protected final Rectangle roiBounds;

    /** Boolean indicating that there No Data and ROI are not used */
    protected final boolean caseA;

    /** Boolean indicating that only ROI is used */
    protected final boolean caseB;

    /** Boolean indicating that only No Data are used */
    protected final boolean caseC;

    /** Image bands number */
    protected int bandsNumber;

    /** Selected bands number */
    protected int selectedBands;

    /** Array containing the indexes of the selected bands */
    protected int[] bands;

    /** Array containing the type of statistics to calculate */
    protected StatsType[] statsTypes;

    /** Length of the statsTypes array */
    protected int statNum;

    /** Boolean indicating if the statistics have been already calculated(if false) or not */
    protected AtomicBoolean firstTime = new AtomicBoolean(true);

    /** Horizontal subsampling */
    protected final int xPeriod;

    /** Vertical subsampling */
    protected final int yPeriod;

    /** Boolean lookuptable used if no data are present */
    protected final boolean[] booleanLookupTable = new boolean[256];

    /** No Data Range */
    protected Range noData;

    /** Extended ROI image*/
    protected RenderedOp srcROIImgExt;

    public StatisticsOpImage(RenderedImage source,
            int xPeriod, int yPeriod, ROI roi, Range noData, boolean useROIAccessor, int[] bands,
            StatsType[] statsTypes, double[] minBound, double[] maxBound, int[] numBins) {
        super(vectorize(source), new ImageLayout(source), null, true);

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

        // Storage of the band indexes and length
        this.bands = bands;
        this.selectedBands = selectedBands;

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
            // Source Bounds
            Rectangle srcRect = new Rectangle(source.getMinX(), source.getMinY(),
                    source.getWidth(), source.getHeight());
            // Padding of the input ROI image in order to avoid the call of the getExtendedData() method
            // ROI bounds are saved 
            roiBounds = srcROIImage.getBounds();
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
            // The useRoiAccessor parameter is set
            this.useROIAccessor = useROIAccessor;
        } else {
            hasROI = false;
            this.useROIAccessor = false;
            roiBounds = null;
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
     * Returns a list of property names that are recognized by this image.
     * 
     * @return An array of <code>String</code>s containing valid property names.
     */
    public String[] getPropertyNames() {
        // Get statistics names and names from superclass.
        String[] statsNames = new String[] { Statistics.STATS_PROPERTY };
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
        // Filling of the container
        for (int i = 0; i < stats.length; i++) {
            for (int j = 0; j < statNum; j++) {
                stats[i][j].clearStats();
            }
        }
        // Setting the calculations to be performed
        firstTime.getAndSet(true);
    }

    /**
     * When the dispose method is called, then old dispose method is performed and also the statistic container is cleared.
     */
    public void dispose() {
        if(srcROIImgExt != null) {
            srcROIImgExt.dispose();
        }
        super.dispose();
        clearStatistic();
    }

    /**
     * Computes and returns all tiles in the image. The tiles are returned in a sequence corresponding to the row-major order of their respective tile
     * indices. The returned array may of course be ignored, e.g., in the case of a subclass which caches the tiles and the intent is to force their
     * computation. This method is overridden such that can be invoked only one time by using a flag for avoiding unnecessary computations.
     */
    public Raster[] getTiles() {
        if (firstTime.getAndSet(false)) {
            if (hasROI) {
                return getTiles(getTileIndices(roiBounds));
            }
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
            return stats.clone();
        } else {
            return super.getProperty(name);
        }
    }

    protected void byteLoop(RasterAccessor src, Rectangle srcRect, RasterAccessor roi,
            Statistics[][] statArray, RandomIter roiIter) {

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
                        int sample = srcData[bands[i]][posx + posy + srcBandOffsets[bands[i]]] & 0xFF;
                        for (int j = 0; j < statNum; j++) {
                            // Update of all the statistics
                            statArray[i][j].addSample(sample);
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
                                int sample = srcData[bands[i]][posx + posy + srcBandOffsets[bands[i]]] & 0xFF;
                                for (int j = 0; j < statNum; j++) {
                                    // Update of all the statistics
                                    statArray[i][j].addSample(sample);
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
                                    int sample = srcData[bands[i]][posx + posy + srcBandOffsets[bands[i]]] & 0xFF;
                                    for (int j = 0; j < statNum; j++) {
                                        // Update of all the statistics
                                        statArray[i][j].addSample(sample);
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
                        int sample = srcData[bands[i]][posx + posy + srcBandOffsets[bands[i]]] & 0xFF;
                        // Control if the sample is Not a NO Data
                        if (booleanLookupTable[sample]) {
                            for (int j = 0; j < statNum; j++) {
                                // Update of all the statistics
                                statArray[i][j].addSample(sample);
                            }
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
                                int sample = srcData[bands[i]][posx + posy + srcBandOffsets[bands[i]]] & 0xFF;
                                // Control if the sample is Not a NO Data
                                if (booleanLookupTable[sample]) {
                                    for (int j = 0; j < statNum; j++) {
                                        // Update of all the statistics
                                        statArray[i][j].addSample(sample);
                                    }
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
                                    int sample = srcData[bands[i]][posx + posy + srcBandOffsets[bands[i]]] & 0xFF;
                                    // Control if the sample is Not a NO Data
                                    if (booleanLookupTable[sample]) {
                                        for (int j = 0; j < statNum; j++) {
                                            // Update of all the statistics
                                            statArray[i][j].addSample(sample);
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

    protected void ushortLoop(RasterAccessor src, Rectangle srcRect, RasterAccessor roi,
            Statistics[][] statArray, RandomIter roiIter) {

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
                            statArray[i][j].addSample(sample);
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
                                    statArray[i][j].addSample(sample);
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
                                        statArray[i][j].addSample(sample);
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
                        boolean isData = !noData.contains((short) sample);
                        if (isData) {
                            for (int j = 0; j < statNum; j++) {
                                // Update of all the statistics
                                statArray[i][j].addSample(sample);
                            }
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
                                boolean isData = !noData.contains((short) sample);
                                if (isData) {
                                    for (int j = 0; j < statNum; j++) {
                                        // Update of all the statistics
                                        statArray[i][j].addSample(sample);
                                    }
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
                                    boolean isData = !noData.contains((short) sample);
                                    if (isData) {
                                        for (int j = 0; j < statNum; j++) {
                                            // Update of all the statistics
                                            statArray[i][j].addSample(sample);
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

    protected void shortLoop(RasterAccessor src, Rectangle srcRect, RasterAccessor roi,
            Statistics[][] statArray, RandomIter roiIter) {

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
                            statArray[i][j].addSample(sample);
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
                                    statArray[i][j].addSample(sample);
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
                                        statArray[i][j].addSample(sample);
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
                        if (isData) {
                            for (int j = 0; j < statNum; j++) {
                                // Update of all the statistics
                                statArray[i][j].addSample(sample);
                            }
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
                                if (isData) {
                                    for (int j = 0; j < statNum; j++) {
                                        // Update of all the statistics
                                        statArray[i][j].addSample(sample);
                                    }
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
                                    if (isData) {
                                        for (int j = 0; j < statNum; j++) {
                                            // Update of all the statistics
                                            statArray[i][j].addSample(sample);
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

    protected void intLoop(RasterAccessor src, Rectangle srcRect, RasterAccessor roi,
            Statistics[][] statArray, RandomIter roiIter) {

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
                            statArray[i][j].addSample(sample);
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
                                    statArray[i][j].addSample(sample);
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
                                        statArray[i][j].addSample(sample);
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
                        if (isData) {
                            for (int j = 0; j < statNum; j++) {
                                // Update of all the statistics
                                statArray[i][j].addSample(sample);
                            }
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
                                if (isData) {
                                    for (int j = 0; j < statNum; j++) {
                                        // Update of all the statistics
                                        statArray[i][j].addSample(sample);
                                    }
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
                                    if (isData) {
                                        for (int j = 0; j < statNum; j++) {
                                            // Update of all the statistics
                                            statArray[i][j].addSample(sample);
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

    protected void floatLoop(RasterAccessor src, Rectangle srcRect, RasterAccessor roi,
            Statistics[][] statArray, RandomIter roiIter) {

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
                            statArray[i][j].addSample(sample);
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
                                    statArray[i][j].addSample(sample);
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
                                        statArray[i][j].addSample(sample);
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
                        if (isData) {
                            for (int j = 0; j < statNum; j++) {
                                // Update of all the statistics
                                statArray[i][j].addSample(sample);
                            }
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
                                if (isData) {
                                    for (int j = 0; j < statNum; j++) {
                                        // Update of all the statistics
                                        statArray[i][j].addSample(sample);
                                    }
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
                                    if (isData) {
                                        for (int j = 0; j < statNum; j++) {
                                            // Update of all the statistics
                                            statArray[i][j].addSample(sample);
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

    protected void doubleLoop(RasterAccessor src, Rectangle srcRect, RasterAccessor roi,
            Statistics[][] statArray, RandomIter roiIter) {

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
                            statArray[i][j].addSample(sample);
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
                                    statArray[i][j].addSample(sample);
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
                                        statArray[i][j].addSample(sample);
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
                        if (isData) {
                            for (int j = 0; j < statNum; j++) {
                                // Update of all the statistics
                                statArray[i][j].addSample(sample);
                            }
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
                                if (isData) {
                                    for (int j = 0; j < statNum; j++) {
                                        // Update of all the statistics
                                        statArray[i][j].addSample(sample);
                                    }
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
                                    if (isData) {
                                        for (int j = 0; j < statNum; j++) {
                                            // Update of all the statistics
                                            statArray[i][j].addSample(sample);
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

    @Override
    public Rectangle mapDestRect(Rectangle destRect, int sourceIndex) {
        return destRect;
    }

    @Override
    public Rectangle mapSourceRect(Rectangle sourceRect, int sourceIndex) {
        return sourceRect;
    }

}
