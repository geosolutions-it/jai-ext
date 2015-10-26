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

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;

import javax.media.jai.ROI;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.iterator.RandomIter;

import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.stats.Statistics.StatsType;

/**
 * The ComplexStatsOpImage class performs various comples statistics operations on an image. The statistical operation are indicated by the
 * {@link StatsType} class. A comples operation is an operation which stores the pixel values into an array. These operations can be calculated
 * together by adding entries in the definition array "statsTypes". A ROI object passed to the constructor is taken into account by counting only the
 * samples inside of it; an eventual No Data Range is considered by counting only values that are not No Data. The statistical calculation is
 * performed by calling the getProperty() method. The statistics are calculated for every image tile but the results are saved into only one global
 * container. For avoiding to compromise the thread-safety of the class, every statistics object should handle concurrent threads. At the end of the
 * calculation the statistics container is passed to the getProperty() method as a Result. For avoiding unnecessary operations the statistics can be
 * calculated only the first time; but if the user needs to re-calculate the statistics, they can be cleared with the clearStatistic() method and then
 * returned by calling again the getProperty() method.
 */
public class ComplexStatsOpImage extends StatisticsOpImage {

    public ComplexStatsOpImage(RenderedImage source,
            int xPeriod, int yPeriod, ROI roi, Range noData, boolean useROIAccessor, int[] bands,
            StatsType[] statsTypes, double[] minBound, double[] maxBound, int[] numBins) {
        super(source, xPeriod, yPeriod, roi, noData, useROIAccessor, bands,
                statsTypes, minBound, maxBound, numBins);

        // Storage of the statistic types indexes if present, and check if they are not simple statistic
        // objects like Mean
        if (statsTypes != null) {
            for (int i = 0; i < statsTypes.length; i++) {
                if (statsTypes[i].getStatsId() < 6) {
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
        
        int[] numB = new int[bandsNumber];
        double[] lowValue = new double[bandsNumber];
        double[] highValue = new double[bandsNumber];
        
        for (int b = 0; b < bandsNumber; b++) {
            numB[b] = numBins.length == 1 ? numBins[0] : numBins[b];
            lowValue[b] = minBound.length == 1 ? minBound[0] : minBound[b];
            highValue[b] = maxBound.length == 1 ? maxBound[0] : maxBound[b];
        }

        // Creation of a global container of all the selected statistics for every band
        this.stats = new Statistics[selectedBands][statNum];
        // Filling of the container
        for (int i = 0; i < selectedBands; i++) {
            for (int j = 0; j < statNum; j++) {
                stats[i][j] = StatsFactory.createComplexStatisticsObjectFromInt(
                        statsTypes[j].getStatsId(), lowValue[i], highValue[i], numB[i]);
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

        // Computation of the statistics
        switch (src.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(src, srcRect, roi, stats, roiIter);
            break;
        case DataBuffer.TYPE_USHORT:
            ushortLoop(src, srcRect, roi, stats, roiIter);
            break;
        case DataBuffer.TYPE_SHORT:
            shortLoop(src, srcRect, roi, stats, roiIter);
            break;
        case DataBuffer.TYPE_INT:
            intLoop(src, srcRect, roi, stats, roiIter);
            break;
        case DataBuffer.TYPE_FLOAT:
            floatLoop(src, srcRect, roi, stats, roiIter);
            break;
        case DataBuffer.TYPE_DOUBLE:
            doubleLoop(src, srcRect, roi, stats, roiIter);
            break;
        }

        return source;
    }
}
