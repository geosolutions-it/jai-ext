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
import java.util.Map;
import javax.media.jai.ImageLayout;
import javax.media.jai.ROI;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.iterator.RandomIter;

/**
 * The SimpleStatsOpImage class performs various simple statistics operations on an image. The statistical operation are indicated by the
 * {@link StatsType} class. A simple operation is an operation which not stores the pixel values into an array but only updates every time its
 * statistical parameters. These operations can be calculated together by adding entries in the definition array "statsTypes". A ROI object passed to
 * the constructor is taken into account by counting only the samples inside of it; an eventual No Data Range is considered by counting only values
 * that are not No Data. The statistical calculation is performed by calling the getProperty() method. The statistics are calculated for every image
 * tile and then the partial results are accumulated and passed to the getProperty() method. For avoiding unnecessary calculations the statistics can
 * be calculated only the first time; but if the user needs to re-calculate the statistics, they can be cleared with the clearStatistic() method and
 * then returned by calling again the getProperty() method.
 */
public class SimpleStatsOpImage extends StatisticsOpImage {

    public SimpleStatsOpImage(RenderedImage source,
            int xPeriod, int yPeriod, ROI roi, Range noData, boolean useROIAccessor, int[] bands,
            StatsType[] statsTypes) {
        super(source, xPeriod, yPeriod, roi, noData, useROIAccessor, bands,
                statsTypes, null, null, null);

        // Storage of the statistic types indexes if present, and check if they are not complex statistic
        // objects like Histogram
        if (statsTypes != null) {
            for (int i = 0; i < statsTypes.length; i++) {
                if (statsTypes[i].getStatsId() > 6) {
                    throw new IllegalArgumentException("Wrong statistic types");
                }
            }
        } else {
            throw new IllegalArgumentException("Statistic types not present");
        }

        this.statsTypes = statsTypes;

        // Number of statistics calculated
        this.statNum = statsTypes.length;

        // Storage of the band indexes and length
        this.bands = bands;

        // Creation of a global container of all the selected statistics for every band
        this.stats = new Statistics[selectedBands][statNum];
        // Filling of the container
        for (int i = 0; i < selectedBands; i++) {
            for (int j = 0; j < statNum; j++) {
                stats[i][j] = StatsFactory.createSimpleStatisticsObjectFromInt(statsTypes[j]
                        .getStatsId());
            }
        }
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

        Rectangle srcRect = getSourceImage(0).getBounds().intersection(source.getBounds());
        // creation of the RasterAccessor
        RasterAccessor src = new RasterAccessor(source, srcRect, formatTags[0], getSourceImage(0)
                .getColorModel());

        // ROI calculations if roiAccessor is used
        RasterAccessor roi = null;
        RandomIter roiIter = null;
        if (useROIAccessor) {
            // Note that the getExtendedData() method is not called because the input images are padded.
            // For each image there is a check if the rectangle is contained inside the source image;
            // if this not happen, the data is taken from the padded image.
            Raster roiRaster = null;
            if(srcROIImage.getBounds().contains(srcRect)){
                roiRaster = srcROIImage.getData(srcRect);
            }else{
                roiRaster = srcROIImgExt.getData(srcRect);
            }

            // creation of the rasterAccessor
            roi = new RasterAccessor(roiRaster, srcRect, RasterAccessor.findCompatibleTags(
                    new RenderedImage[] { srcROIImage }, srcROIImage)[0],
                    srcROIImage.getColorModel());
        } else if(hasROI) {
            roiIter = RandomIterFactory.create(srcROIImage, srcROIImage.getBounds(), true, true);
        }

        // Creation of local objects containing the same statistics as the initials

        Statistics[][] statArray = new Statistics[selectedBands][statNum];
        // Filling of the container
        for (int i = 0; i < selectedBands; i++) {
            for (int j = 0; j < statNum; j++) {
                statArray[i][j] = StatsFactory.createSimpleStatisticsObjectFromInt(statsTypes[j]
                        .getStatsId());
            }
        }

        // Computation of the statistics
        switch (src.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(src, srcRect, roi, statArray, roiIter);
            break;
        case DataBuffer.TYPE_USHORT:
            ushortLoop(src, srcRect, roi, statArray, roiIter);
            break;
        case DataBuffer.TYPE_SHORT:
            shortLoop(src, srcRect, roi, statArray, roiIter);
            break;
        case DataBuffer.TYPE_INT:
            intLoop(src, srcRect, roi, statArray, roiIter);
            break;
        case DataBuffer.TYPE_FLOAT:
            floatLoop(src, srcRect, roi, statArray, roiIter);
            break;
        case DataBuffer.TYPE_DOUBLE:
            doubleLoop(src, srcRect, roi, statArray, roiIter);
            break;
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

        return source;
    }
}
