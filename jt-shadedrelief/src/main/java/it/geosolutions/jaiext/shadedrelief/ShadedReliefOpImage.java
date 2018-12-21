/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2018 GeoSolutions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.geosolutions.jaiext.shadedrelief;

import com.sun.media.jai.util.ImageUtil;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.shadedrelief.ShadedReliefAlgorithm.DataProcessor;
import it.geosolutions.jaiext.shadedrelief.ShadedReliefAlgorithm.DataProcessorByte;
import it.geosolutions.jaiext.shadedrelief.ShadedReliefAlgorithm.DataProcessorDouble;
import it.geosolutions.jaiext.shadedrelief.ShadedReliefAlgorithm.DataProcessorFloat;
import it.geosolutions.jaiext.shadedrelief.ShadedReliefAlgorithm.DataProcessorInt;
import it.geosolutions.jaiext.shadedrelief.ShadedReliefAlgorithm.DataProcessorShort;
import it.geosolutions.jaiext.shadedrelief.ShadedReliefAlgorithm.ProcessingCase;
import it.geosolutions.jaiext.shadedrelief.ShadedReliefAlgorithm.ShadedReliefParameters;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import javax.media.jai.AreaOpImage;
import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.IntegerSequence;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.operator.BorderDescriptor;

/**
 * ShadedRelief op Image.
 */
class ShadedReliefOpImage extends AreaOpImage {

    private static final BorderExtender EXTENDER =
            BorderExtender.createInstance(BorderExtender.BORDER_COPY);

    /**
     * Constant indicating that the inner random iterators must pre-calculate an array of the image
     * positions
     */
    public static final boolean ARRAY_CALC = true;

    /** Constant indicating that the inner random iterators must cache the current tile position */
    public static final boolean TILE_CACHED = true;

    /** Boolean indicating that NoData must be checked */
    protected final boolean hasNoData;

    /** NoData element */
    protected Range srcNoData;

    /** Boolean indicating that no roi and no data check must be done */
    protected final boolean caseA;

    /** Boolean indicating that only roi check must be done */
    protected final boolean caseB;

    /** Boolean indicating that only no data check must be done */
    protected final boolean caseC;

    /** Boolean indicating that ROI must be checked */
    protected final boolean hasROI;

    /** ROI element */
    protected ROI roi;

    /** ROI bounds as a Shape */
    protected final Rectangle roiBounds;

    /** ROI related image */
    protected PlanarImage roiImage;

    protected double dstNoData;

    protected RenderedImage extendedIMG;

    protected Rectangle destBounds;

    private int maxX;

    private int maxY;

    private ShadedReliefAlgorithm.ShadedReliefParameters params;

    private static final int FIXED_PADDING = 1;

    public ShadedReliefOpImage(
            RenderedImage source,
            RenderingHints hints,
            ImageLayout l,
            ROI roi,
            Range srcNoData,
            double dstNoData,
            double resX,
            double resY,
            double verticalExaggeration,
            double verticalScale,
            double altitude,
            double azimuth,
            ShadedReliefAlgorithm algorithm) {
        super(
                source,
                l,
                hints,
                true,
                EXTENDER,
                FIXED_PADDING,
                FIXED_PADDING,
                FIXED_PADDING,
                FIXED_PADDING);

        maxX = minX + width - 1;
        maxY = minY + height - 1;

        // Check if ROI control must be done
        if (roi != null) {
            hasROI = true;
            // Roi object
            this.roi = roi;
            roiBounds = roi.getBounds();
        } else {
            hasROI = false;
            this.roi = null;
            roiBounds = null;
        }

        // Getting datatype
        int dataType = source.getSampleModel().getDataType();

        // Check if No Data control must be done
        if (srcNoData != null) {
            hasNoData = true;
            this.srcNoData = srcNoData;
        } else {
            hasNoData = false;
        }

        // Definition of the possible cases that can be found
        // caseA = no ROI nor No Data
        // caseB = ROI present but No Data not present
        // caseC = No Data present but ROI not present
        // Last case not defined = both ROI and No Data are present
        caseA = !hasNoData && !hasROI;
        caseB = !hasNoData && hasROI;
        caseC = hasNoData && !hasROI;

        // Destination No Data value will be clamped to the image data type
        this.dstNoData = dstNoData;

        this.params =
                new ShadedReliefParameters(
                        resX,
                        resY,
                        verticalExaggeration,
                        verticalScale,
                        altitude,
                        azimuth,
                        algorithm);

        if (this.extender != null) {
            RenderingHints borderHints = (RenderingHints) hints.clone();
            Object layout = borderHints.get(JAI.KEY_IMAGE_LAYOUT);
            ImageLayout il = null;
            if (layout != null && layout instanceof ImageLayout) {
                il = (ImageLayout) layout;
            } else {
                il = new ImageLayout(
                            source.getMinX() - leftPadding,
                            source.getMinY() - topPadding,
                            source.getWidth() + leftPadding + rightPadding,
                            source.getHeight() + topPadding + bottomPadding);
                borderHints.put(JAI.KEY_IMAGE_LAYOUT, il);
            }
            il.setTileGridXOffset(source.getTileGridXOffset());
            il.setTileGridYOffset(source.getTileGridYOffset());

            extendedIMG = BorderDescriptor.create(
                            source,
                            leftPadding, rightPadding,
                            topPadding, bottomPadding,
                            extender,
                            borderHints);
            this.destBounds = getBounds();
        } else {
            int x0 = getMinX() + leftPadding;
            int y0 = getMinY() + topPadding;

            int w = getWidth() - leftPadding - rightPadding;
            w = Math.max(w, 0);

            int h = getHeight() - topPadding - bottomPadding;
            h = Math.max(h, 0);

            this.destBounds = new Rectangle(x0, y0, w, h);
        }
    }

    /**
     * Performs the computation on a specified rectangle.
     *
     * The sources are cobbled.
     *
     * @param sources an array of source Rasters, guaranteed to provide all necessary source data
     *     for computing the output.
     * @param dest a WritableRaster tile containing the area to be computed.
     * @param destRect the rectangle within dest to be processed.
     */
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        Raster source = sources[0];
        Rectangle srcRect = mapDestRect(destRect, 0);

        RasterAccessor src =
                new RasterAccessor(
                        source, srcRect, formatTags[0], getSourceImage(0).getColorModel());
        RasterAccessor dst = new RasterAccessor(dest, destRect, formatTags[1], getColorModel());

        // ROI fields
        ROI roiTile = null;

        RandomIter roiIter = null;

        boolean roiContainsTile = false;
        boolean roiDisjointTile = false;

        // ROI check
        if (hasROI) {
            Rectangle srcRectExpanded = mapDestRect(destRect, 0);
            // The tile dimension is extended for avoiding border errors
            srcRectExpanded.setRect(
                    srcRectExpanded.getMinX() - 1,
                    srcRectExpanded.getMinY() - 1,
                    srcRectExpanded.getWidth() + 2,
                    srcRectExpanded.getHeight() + 2);
            if (!roiBounds.intersects(srcRectExpanded)) {
                roiDisjointTile = true;
            } else {
                roiTile = roi.intersect(new ROIShape(srcRectExpanded));
                roiContainsTile = roiTile.contains(srcRectExpanded);
                if (!roiContainsTile) {
                    if (!roiTile.intersects(srcRectExpanded)) {
                        roiDisjointTile = true;
                    } else {
                        PlanarImage roiIMG = getImage();
                        roiIter = RandomIterFactory.create(roiIMG, null /*, TILE_CACHED, ARRAY_CALC*/);
                    }
                }
            }
        }

        if (!hasROI || !roiDisjointTile) {
            switch (dst.getDataType()) {
                case DataBuffer.TYPE_BYTE:
                    byteLoop(src, dst, roiIter, roiContainsTile);
                    break;
                case DataBuffer.TYPE_USHORT:
                    ushortLoop(src, dst, roiIter, roiContainsTile);
                    break;
                case DataBuffer.TYPE_SHORT:
                    shortLoop(src, dst, roiIter, roiContainsTile);
                    break;
                case DataBuffer.TYPE_INT:
                    intLoop(src, dst, roiIter, roiContainsTile);
                    break;
                case DataBuffer.TYPE_FLOAT:
                    floatLoop(src, dst, roiIter, roiContainsTile);
                    break;
                case DataBuffer.TYPE_DOUBLE:
                    doubleLoop(src, dst, roiIter, roiContainsTile);
                    break;
                default:
                    throw new IllegalArgumentException("Wrong Data Type defined");
            }

            // If the RasterAccessor object set up a temporary buffer for the
            // op to write to, tell the RasterAccessor to write that data
            // to the raster no that we're done with it.
            if (dst.isDataCopy()) {
                dst.clampDataArrays();
                dst.copyDataToRaster();
            }
        } else {
            // Setting all as NoData
            double[] backgroundValues = new double[src.getNumBands()];
            Arrays.fill(backgroundValues, dstNoData);
            ImageUtil.fillBackground(dest, destRect, backgroundValues);
        }
    }


    public Raster computeTile(int tileX, int tileY) {
        if (!cobbleSources) {
            return super.computeTile(tileX, tileY);
        }
        // Special handling for Border Extender

        /* Create a new WritableRaster to represent this tile. */
        Point org = new Point(tileXToX(tileX), tileYToY(tileY));
        WritableRaster dest = createWritableRaster(sampleModel, org);

        /* Clip output rectangle to image bounds. */
        Rectangle rect =
                new Rectangle(org.x, org.y, sampleModel.getWidth(), sampleModel.getHeight());

        Rectangle destRect = rect.intersection(destBounds);
        if ((destRect.width <= 0) || (destRect.height <= 0)) {
            return dest;
        }

        /* account for padding in srcRectangle */
        PlanarImage s = getSourceImage(0);
        destRect = destRect.intersection(s.getBounds());
        Rectangle srcRect = new Rectangle(destRect);
        srcRect.x -= getLeftPadding();
        srcRect.width += getLeftPadding() + getRightPadding();
        srcRect.y -= getTopPadding();
        srcRect.height += getTopPadding() + getBottomPadding();

        /*
         * The tileWidth and tileHeight of the source image may differ from this tileWidth and tileHeight.
         */
        IntegerSequence srcXSplits = new IntegerSequence();
        IntegerSequence srcYSplits = new IntegerSequence();

        // there is only one source for an AreaOpImage
        s.getSplits(srcXSplits, srcYSplits, srcRect);

        // Initialize new sequences of X splits.
        IntegerSequence xSplits = new IntegerSequence(destRect.x, destRect.x + destRect.width);

        xSplits.insert(destRect.x);
        xSplits.insert(destRect.x + destRect.width);

        srcXSplits.startEnumeration();
        while (srcXSplits.hasMoreElements()) {
            int xsplit = srcXSplits.nextElement();
            int lsplit = xsplit - getLeftPadding();
            int rsplit = xsplit + getRightPadding();
            xSplits.insert(lsplit);
            xSplits.insert(rsplit);
        }

        // Initialize new sequences of Y splits.
        IntegerSequence ySplits = new IntegerSequence(destRect.y, destRect.y + destRect.height);

        ySplits.insert(destRect.y);
        ySplits.insert(destRect.y + destRect.height);

        srcYSplits.startEnumeration();
        while (srcYSplits.hasMoreElements()) {
            int ysplit = srcYSplits.nextElement();
            int tsplit = ysplit - getBottomPadding();
            int bsplit = ysplit + getTopPadding();
            ySplits.insert(tsplit);
            ySplits.insert(bsplit);
        }

        /*
         * Divide destRect into sub rectangles based on the source splits, and compute each sub rectangle separately.
         */
        int x1, x2, y1, y2;
        Raster[] sources = new Raster[1];

        ySplits.startEnumeration();
        for (y1 = ySplits.nextElement(); ySplits.hasMoreElements(); y1 = y2) {
            y2 = ySplits.nextElement();

            int h = y2 - y1;
            int py1 = y1 - getTopPadding();
            int py2 = y2 + getBottomPadding();
            int ph = py2 - py1;

            xSplits.startEnumeration();
            for (x1 = xSplits.nextElement(); xSplits.hasMoreElements(); x1 = x2) {
                x2 = xSplits.nextElement();

                int w = x2 - x1;
                int px1 = x1 - getLeftPadding();
                int px2 = x2 + getRightPadding();
                int pw = px2 - px1;

                // Fetch the padded src rectangle
                Rectangle srcSubRect = new Rectangle(px1, py1, pw, ph);
                sources[0] =
                        extender != null ? extendedIMG.getData(srcSubRect) : s.getData(srcSubRect);

                // Make a destRectangle
                Rectangle dstSubRect = new Rectangle(x1, y1, w, h);
                computeRect(sources, dest, dstSubRect);

                // Recycle the source tile
                if (s.overlapsMultipleTiles(srcSubRect)) {
                    recycleTile(sources[0]);
                }
            }
        }
        return dest;
    }

    /**
     * This method provides a lazy initialization of the image associated to the ROI.
     *
     * The method uses the Double-checked locking in order to maintain thread-safety
     *
     * @return
     */
    protected PlanarImage getImage() {
        PlanarImage img = roiImage;
        if (img == null) {
            synchronized (this) {
                img = roiImage;
                if (img == null) {
                    roiImage = img = roi.getAsImage();
                }
            }
        }
        return img;
    }

    protected void byteLoop(
            RasterAccessor src, RasterAccessor dst, RandomIter roiIter, boolean roiContainsTile) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();

        byte destNoDataTyped = ImageUtil.clampRoundByte(dstNoData);

        byte dstDataArrays[][] = dst.getByteDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        byte srcDataArrays[][] = src.getByteDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // precalcaculate offsets
        int centerScanlineOffset = srcScanlineStride;
        int dstX = dst.getX();
        int dstY = dst.getY();

        // X,Y positions
        int x0 = 0;
        int y0 = 0;
        int srcX = src.getX();
        int srcY = src.getY();

        double[] window = new double[9];
        boolean[] roiMask = new boolean[9];

        byte dstData[] = dstDataArrays[0];
        byte srcData[] = srcDataArrays[0];
        int srcScanlineOffset = srcBandOffsets[0];
        int dstScanlineOffset = dstBandOffsets[0];
        int srcPixelOffset;
        int dstPixelOffset;
        double destValue;
        DataProcessor data =
                new DataProcessorByte(srcData, hasNoData, srcNoData, dstNoData, params);

        if (caseA || (caseB && roiContainsTile)) {
            for (int y = 0; y < dheight; y++) {
                srcPixelOffset = srcScanlineOffset;
                dstPixelOffset = dstScanlineOffset;
                for (int x = 0; x < dwidth; x++) {
                    int sX = x + dstX;
                    int sY = y + dstY;
                    ProcessingCase currentCase = getCase(sX, sY);
                    destValue =
                            data.processWindow(
                                    window, x, srcPixelOffset, centerScanlineOffset, currentCase);
                    dstData[dstPixelOffset] = ImageUtil.clampRoundByte(destValue);
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }

        } else if (caseB) { // ROI Check
            for (int y = 0; y < dheight; y++) {
                srcPixelOffset = srcScanlineOffset;
                dstPixelOffset = dstScanlineOffset;

                y0 = srcY + y;

                for (int x = 0; x < dwidth; x++) {
                    x0 = srcX + x;

                    boolean inROI = false;
                    // ROI Check
                    for (int dy = 0; dy < 3; dy++) {
                        int yI = y0 + dy;
                        for (int dx = 0; dx < 3; dx++) {
                            int xI = x0 + dx;
                            if (roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0) {
                                inROI = true;
                                roiMask[dx + (3 * dy)] = true;
                            } else {
                                roiMask[dx + (3 * dy)] = false;
                            }
                        }
                    }

                    if (inROI) {
                        int sX = x + dstX;
                        int sY = y + dstY;
                        ProcessingCase currentCase = getCase(sX, sY);
                        destValue =
                                data.processWindowRoi(
                                        window,
                                        x,
                                        srcPixelOffset,
                                        centerScanlineOffset,
                                        currentCase,
                                        roiMask);
                        dstData[dstPixelOffset] = ImageUtil.clampRoundByte(destValue);
                    } else {
                        dstData[dstPixelOffset] = destNoDataTyped;
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }

        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) { // NoData Check
            for (int y = 0; y < dheight; y++) {
                srcPixelOffset = srcScanlineOffset;
                dstPixelOffset = dstScanlineOffset;

                for (int x = 0; x < dwidth; x++) {
                    int sX = x + dstX;
                    int sY = y + dstY;
                    ProcessingCase currentCase = getCase(sX, sY);
                    destValue = data.processWindowNoData(
                                    window, x, srcPixelOffset, centerScanlineOffset, currentCase);
                    dstData[dstPixelOffset] =
                            Double.isNaN(destValue)
                                    ? destNoDataTyped
                                    : ImageUtil.clampRoundByte(destValue);
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
            // ROI and No Data Check
        } else {
            for (int y = 0; y < dheight; y++) {
                srcPixelOffset = srcScanlineOffset;
                dstPixelOffset = dstScanlineOffset;

                y0 = srcY + y;

                for (int x = 0; x < dwidth; x++) {
                    x0 = srcX + x;
                    boolean inROI = false;
                    // ROI Check
                    for (int dy = 0; dy < 3; dy++) {
                        int yI = y0 + dy;
                        for (int dx = 0; dx < 3; dx++) {
                            int xI = x0 + dx;
                            if (roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0) {
                                inROI = true;
                                roiMask[dx + (3 * dy)] = true;
                            } else {
                                roiMask[dx + (3 * dy)] = false;
                            }
                        }
                    }

                    if (inROI) {
                        int sX = x + dstX;
                        int sY = y + dstY;
                        ProcessingCase currentCase = getCase(sX, sY);
                        destValue = data.processWindowRoiNoData(
                                        window, x, srcPixelOffset, centerScanlineOffset, currentCase, roiMask);
                        dstData[dstPixelOffset] =
                                Double.isNaN(destValue)
                                        ? destNoDataTyped
                                        : ImageUtil.clampRoundByte(destValue);
                    } else {
                        dstData[dstPixelOffset] = destNoDataTyped;
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    protected void ushortLoop(
            RasterAccessor src, RasterAccessor dst, RandomIter roiIter, boolean roiContainsTile) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();

        short destNoDataTyped = ImageUtil.clampRoundUShort(dstNoData);

        short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        short srcDataArrays[][] = src.getShortDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // precalcaculate offsets
        int centerScanlineOffset = srcScanlineStride;
        int dstX = dst.getX();
        int dstY = dst.getY();

        // X,Y positions
        int x0 = 0;
        int y0 = 0;
        int srcX = src.getX();
        int srcY = src.getY();

        double[] window = new double[9];
        boolean[] roiMask = new boolean[9];

        short dstData[] = dstDataArrays[0];
        short srcData[] = srcDataArrays[0];
        int srcScanlineOffset = srcBandOffsets[0];
        int dstScanlineOffset = dstBandOffsets[0];
        int srcPixelOffset;
        int dstPixelOffset;
        double destValue = Double.NaN;
        DataProcessor data =
                new DataProcessorShort(srcData, hasNoData, srcNoData, dstNoData, params);

        if (caseA || (caseB && roiContainsTile)) {
            for (int y = 0; y < dheight; y++) {
                srcPixelOffset = srcScanlineOffset;
                dstPixelOffset = dstScanlineOffset;

                for (int x = 0; x < dwidth; x++) {
                    int sX = x + dstX;
                    int sY = y + dstY;
                    ProcessingCase currentCase = getCase(sX, sY);
                    destValue =
                            data.processWindow(
                                    window, x, srcPixelOffset, centerScanlineOffset, currentCase);
                    dstData[dstPixelOffset] = ImageUtil.clampRoundUShort(destValue);
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }

        } else if (caseB) { // ROI Check
            for (int y = 0; y < dheight; y++) {
                srcPixelOffset = srcScanlineOffset;
                dstPixelOffset = dstScanlineOffset;

                y0 = srcY + y;

                for (int x = 0; x < dwidth; x++) {
                    x0 = srcX + x;

                    boolean inROI = false;
                    // ROI Check
                    for (int dy = 0; dy < 3; dy++) {
                        int yI = y0 + dy;
                        for (int dx = 0; dx < 3; dx++) {
                            int xI = x0 + dx;
                            if (roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0) {
                                inROI = true;
                                roiMask[dx + (3 * dy)] = true;
                            } else {
                                roiMask[dx + (3 * dy)] = false;
                            }
                        }
                    }

                    if (inROI) {
                        int sX = x + dstX;
                        int sY = y + dstY;
                        ProcessingCase currentCase = getCase(sX, sY);
                        destValue =
                                data.processWindowRoi(
                                        window,
                                        x,
                                        srcPixelOffset,
                                        centerScanlineOffset,
                                        currentCase,
                                        roiMask);
                        dstData[dstPixelOffset] = ImageUtil.clampRoundUShort(destValue);
                    } else {
                        dstData[dstPixelOffset] = destNoDataTyped;
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) { // NoData Check
            for (int y = 0; y < dheight; y++) {
                srcPixelOffset = srcScanlineOffset;
                dstPixelOffset = dstScanlineOffset;

                for (int x = 0; x < dwidth; x++) {
                    int sX = x + dstX;
                    int sY = y + dstY;
                    ProcessingCase currentCase = getCase(sX, sY);
                    destValue =
                            data.processWindowNoData(
                                    window, x, srcPixelOffset, centerScanlineOffset, currentCase);
                    dstData[dstPixelOffset] =
                            Double.isNaN(destValue)
                                    ? destNoDataTyped
                                    : ImageUtil.clampRoundUShort(destValue);
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }

        } else { // ROI and No Data Check
            for (int y = 0; y < dheight; y++) {
                srcPixelOffset = srcScanlineOffset;
                dstPixelOffset = dstScanlineOffset;

                y0 = srcY + y;

                for (int x = 0; x < dwidth; x++) {
                    x0 = srcX + x;
                    boolean inROI = false;
                    // ROI Check
                    for (int dy = 0; dy < 3; dy++) {
                        int yI = y0 + dy;
                        for (int dx = 0; dx < 3; dx++) {
                            int xI = x0 + dx;
                            if (roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0) {
                                inROI = true;
                                roiMask[dx + (3 * dy)] = true;
                            } else {
                                roiMask[dx + (3 * dy)] = false;
                            }
                        }
                    }

                    if (inROI) {
                        int sX = x + dstX;
                        int sY = y + dstY;
                        ProcessingCase currentCase = getCase(sX, sY);
                        destValue =
                                data.processWindowRoiNoData(
                                        window,
                                        x,
                                        srcPixelOffset,
                                        centerScanlineOffset,
                                        currentCase,
                                        roiMask);
                        dstData[dstPixelOffset] =
                                Double.isNaN(destValue)
                                        ? destNoDataTyped
                                        : ImageUtil.clampRoundUShort(destValue);
                    } else {
                        dstData[dstPixelOffset] = destNoDataTyped;
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }


    protected void shortLoop(
            RasterAccessor src, RasterAccessor dst, RandomIter roiIter, boolean roiContainsTile) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();

        short destNoDataTyped = ImageUtil.clampRoundShort(dstNoData);

        short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        short srcDataArrays[][] = src.getShortDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // precalcaculate offsets
        int centerScanlineOffset = srcScanlineStride;
        int dstX = dst.getX();
        int dstY = dst.getY();

        // X,Y positions
        int x0 = 0;
        int y0 = 0;
        int srcX = src.getX();
        int srcY = src.getY();

        double[] window = new double[9];
        boolean[] roiMask = new boolean[9];

        short dstData[] = dstDataArrays[0];
        short srcData[] = srcDataArrays[0];
        int srcScanlineOffset = srcBandOffsets[0];
        int dstScanlineOffset = dstBandOffsets[0];
        int srcPixelOffset;
        int dstPixelOffset;
        double destValue = Double.NaN;
        DataProcessor data =
                new DataProcessorShort(srcData, hasNoData, srcNoData, dstNoData, params);

        if (caseA || (caseB && roiContainsTile)) {
            for (int y = 0; y < dheight; y++) {
                srcPixelOffset = srcScanlineOffset;
                dstPixelOffset = dstScanlineOffset;
                for (int i = 0; i < dwidth; i++) {
                    int sX = i + dstX;
                    int sY = y + dstY;
                    ProcessingCase currentCase = getCase(sX, sY);
                    destValue =
                            data.processWindow(
                                    window, i, srcPixelOffset, centerScanlineOffset, currentCase);
                    dstData[dstPixelOffset] = ImageUtil.clampRoundShort(destValue);
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        } else if (caseB) { // ROI Check
            for (int y = 0; y < dheight; y++) {
                srcPixelOffset = srcScanlineOffset;
                dstPixelOffset = dstScanlineOffset;

                y0 = srcY + y;

                for (int x = 0; x < dwidth; x++) {
                    x0 = srcX + x;

                    boolean inROI = false;
                    // ROI Check
                    for (int dy = 0; dy < 3; dy++) {
                        int yI = y0 + dy;
                        for (int dx = 0; dx < 3; dx++) {
                            int xI = x0 + dx;
                            if (roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0) {
                                inROI = true;
                                roiMask[dx + (3 * dy)] = true;
                            } else {
                                roiMask[dx + (3 * dy)] = false;
                            }
                        }
                    }

                    if (inROI) {
                        int sX = x + dstX;
                        int sY = y + dstY;
                        ProcessingCase currentCase = getCase(sX, sY);
                        destValue =
                                data.processWindowRoi(
                                        window,
                                        x,
                                        srcPixelOffset,
                                        centerScanlineOffset,
                                        currentCase,
                                        roiMask);
                        dstData[dstPixelOffset] = ImageUtil.clampRoundShort(destValue);
                    } else {
                        dstData[dstPixelOffset] = destNoDataTyped;
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) { // NoData Check
            for (int y = 0; y < dheight; y++) {
                srcPixelOffset = srcScanlineOffset;
                dstPixelOffset = dstScanlineOffset;

                for (int x = 0; x < dwidth; x++) {
                    int sX = x + dstX;
                    int sY = y + dstY;
                    ProcessingCase currentCase = getCase(sX, sY);
                    destValue =
                            data.processWindowNoData(
                                    window, x, srcPixelOffset, centerScanlineOffset, currentCase);
                    dstData[dstPixelOffset] =
                            Double.isNaN(destValue)
                                    ? destNoDataTyped
                                    : ImageUtil.clampRoundShort(destValue);
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }

        } else { // ROI and No Data Check
            for (int y = 0; y < dheight; y++) {
                srcPixelOffset = srcScanlineOffset;
                dstPixelOffset = dstScanlineOffset;

                y0 = srcY + y;

                for (int x = 0; x < dwidth; x++) {
                    x0 = srcX + x;
                    boolean inROI = false;
                    // ROI Check
                    for (int dy = 0; dy < 3; dy++) {
                        int yI = y0 + dy;
                        for (int dx = 0; dx < 3; dx++) {
                            int xI = x0 + dx;
                            if (roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0) {
                                inROI = true;
                                roiMask[dx + (3 * dy)] = true;
                            } else {
                                roiMask[dx + (3 * dy)] = false;
                            }
                        }
                    }

                    if (inROI) {
                        int sX = x + dstX;
                        int sY = y + dstY;
                        ProcessingCase currentCase = getCase(sX, sY);
                        destValue =
                                data.processWindowRoiNoData(
                                        window,
                                        x,
                                        srcPixelOffset,
                                        centerScanlineOffset,
                                        currentCase,
                                        roiMask);
                        dstData[dstPixelOffset] =
                                Double.isNaN(destValue)
                                        ? destNoDataTyped
                                        : ImageUtil.clampRoundShort(destValue);
                    } else {
                        dstData[dstPixelOffset] = destNoDataTyped;
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }


    protected void intLoop(
            RasterAccessor src, RasterAccessor dst, RandomIter roiIter, boolean roiContainsTile) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();

        int destNoDataTyped = ImageUtil.clampRoundInt(dstNoData);

        int dstDataArrays[][] = dst.getIntDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        int srcDataArrays[][] = src.getIntDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // precalcaculate offsets
        int centerScanlineOffset = srcScanlineStride;
        int dstX = dst.getX();
        int dstY = dst.getY();

        // X,Y positions
        int x0 = 0;
        int y0 = 0;
        int srcX = src.getX();
        int srcY = src.getY();

        double[] window = new double[9];
        boolean[] roiMask = new boolean[9];

        int dstData[] = dstDataArrays[0];
        int srcData[] = srcDataArrays[0];
        int srcScanlineOffset = srcBandOffsets[0];
        int dstScanlineOffset = dstBandOffsets[0];
        int srcPixelOffset;
        int dstPixelOffset;
        double destValue = Double.NaN;
        DataProcessor data = new DataProcessorInt(srcData, hasNoData, srcNoData, dstNoData, params);

        if (caseA || (caseB && roiContainsTile)) {
            for (int y = 0; y < dheight; y++) {
                srcPixelOffset = srcScanlineOffset;
                dstPixelOffset = dstScanlineOffset;

                for (int x = 0; x < dwidth; x++) {
                    int sX = x + dstX;
                    int sY = y + dstY;
                    ProcessingCase currentCase = getCase(sX, sY);
                    destValue =
                            data.processWindow(
                                    window, x, srcPixelOffset, centerScanlineOffset, currentCase);
                    dstData[dstPixelOffset] = ImageUtil.clampRoundInt(destValue);
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        } else if (caseB) { // ROI Check
            for (int y = 0; y < dheight; y++) {
                srcPixelOffset = srcScanlineOffset;
                dstPixelOffset = dstScanlineOffset;

                y0 = srcY + y;

                for (int x = 0; x < dwidth; x++) {
                    x0 = srcX + x;

                    boolean inROI = false;
                    // ROI Check
                    for (int dy = 0; dy < 3; dy++) {
                        int yI = y0 + dy;
                        for (int dx = 0; dx < 3; dx++) {
                            int xI = x0 + dx;
                            if (roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0) {
                                inROI = true;
                                roiMask[dx + (3 * dy)] = true;
                            } else {
                                roiMask[dx + (3 * dy)] = false;
                            }
                        }
                    }

                    if (inROI) {
                        int sX = x + dstX;
                        int sY = y + dstY;
                        ProcessingCase currentCase = getCase(sX, sY);
                        destValue =
                                data.processWindowRoi(
                                        window,
                                        x,
                                        srcPixelOffset,
                                        centerScanlineOffset,
                                        currentCase,
                                        roiMask);
                        dstData[dstPixelOffset] = ImageUtil.clampRoundInt(destValue);
                    } else {
                        dstData[dstPixelOffset] = destNoDataTyped;
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) { // NoData Check
            for (int y= 0; y < dheight; y++) {
                srcPixelOffset = srcScanlineOffset;
                dstPixelOffset = dstScanlineOffset;

                for (int x = 0; x < dwidth; x++) {
                    int sX = x + dstX;
                    int sY = y + dstY;
                    ProcessingCase currentCase = getCase(sX, sY);
                    destValue =
                            data.processWindowNoData(
                                    window, x, srcPixelOffset, centerScanlineOffset, currentCase);
                    dstData[dstPixelOffset] =
                            Double.isNaN(destValue)
                                    ? destNoDataTyped
                                    : ImageUtil.clampRoundInt(destValue);
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }

        } else { // ROI and No Data Check
            for (int y = 0; y < dheight; y++) {
                srcPixelOffset = srcScanlineOffset;
                dstPixelOffset = dstScanlineOffset;

                y0 = srcY + y;

                for (int x = 0; x < dwidth; x++) {
                    x0 = srcX + x;
                    boolean inROI = false;
                    // ROI Check
                    for (int dy = 0; dy < 3; dy++) {
                        int yI = y0 + dy;
                        for (int dx = 0; dx < 3; dx++) {
                            int xI = x0 + dx;
                            if (roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0) {
                                inROI = true;
                                roiMask[dx + (3 * dy)] = true;
                            } else {
                                roiMask[dx + (3 * dy)] = false;
                            }
                        }
                    }

                    if (inROI) {
                        int sX = x + dstX;
                        int sY = y + dstY;
                        ProcessingCase currentCase = getCase(sX, sY);
                        destValue =
                                data.processWindowRoiNoData(
                                        window,
                                        x,
                                        srcPixelOffset,
                                        centerScanlineOffset,
                                        currentCase,
                                        roiMask);
                        dstData[dstPixelOffset] =
                                Double.isNaN(destValue)
                                        ? destNoDataTyped
                                        : ImageUtil.clampRoundInt(destValue);
                    } else {
                        dstData[dstPixelOffset] = destNoDataTyped;
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }


    protected void floatLoop(
            RasterAccessor src, RasterAccessor dst, RandomIter roiIter, boolean roiContainsTile) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();

        float destNoDataTyped = ImageUtil.clampFloat(dstNoData);

        float dstDataArrays[][] = dst.getFloatDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        float srcDataArrays[][] = src.getFloatDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // precalcaculate offsets
        int centerScanlineOffset = srcScanlineStride;
        int dstX = dst.getX();
        int dstY = dst.getY();

        // X,Y positions
        int x0 = 0;
        int y0 = 0;
        int srcX = src.getX();
        int srcY = src.getY();

        double[] window = new double[9];
        boolean[] roiMask = new boolean[9];

        float dstData[] = dstDataArrays[0];
        float srcData[] = srcDataArrays[0];
        int srcScanlineOffset = srcBandOffsets[0];
        int dstScanlineOffset = dstBandOffsets[0];
        int srcPixelOffset;
        int dstPixelOffset;
        double destValue = Double.NaN;
        DataProcessor data =
                new DataProcessorFloat(srcData, hasNoData, srcNoData, dstNoData, params);

        if (caseA || (caseB && roiContainsTile)) {
            for (int y = 0; y < dheight; y++) {
                srcPixelOffset = srcScanlineOffset;
                dstPixelOffset = dstScanlineOffset;
                for (int x = 0; x < dwidth; x++) {
                    int sX = x + dstX;
                    int sY = y + dstY;
                    ProcessingCase currentCase = getCase(sX, sY);
                    destValue =
                            data.processWindow(
                                    window, x, srcPixelOffset, centerScanlineOffset, currentCase);
                    dstData[dstPixelOffset] = (float)destValue; // checkme clamp
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        } else if (caseB) { //ROI Check
            for (int y = 0; y < dheight; y++) {
                srcPixelOffset = srcScanlineOffset;
                dstPixelOffset = dstScanlineOffset;

                y0 = srcY + y;

                for (int x = 0; x < dwidth; x++) {
                    x0 = srcX + x;

                    boolean inROI = false;
                    // ROI Check
                    for (int dy = 0; dy < 3; dy++) {
                        int yI = y0 + dy;
                        for (int dx = 0; dx < 3; dx++) {
                            int xI = x0 + dx;
                            if (roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0) {
                                inROI = true;
                                roiMask[dx + (3 * dy)] = true;
                            } else {
                                roiMask[dx + (3 * dy)] = false;
                            }
                        }
                    }

                    if (inROI) {
                        int sX = x + dstX;
                        int sY = y + dstY;
                        ProcessingCase currentCase = getCase(sX, sY);
                        destValue =
                                data.processWindowRoi(
                                        window,
                                        x,
                                        srcPixelOffset,
                                        centerScanlineOffset,
                                        currentCase,
                                        roiMask);
                        dstData[dstPixelOffset] = (float)destValue; // checkme clamp
                    } else {
                        dstData[dstPixelOffset] = destNoDataTyped;
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) { // NoData Check
            for (int y = 0; y < dheight; y++) {
                srcPixelOffset = srcScanlineOffset;
                dstPixelOffset = dstScanlineOffset;

                for (int x = 0; x < dwidth; x++) {
                    int sX = x + dstX;
                    int sY = y + dstY;
                    ProcessingCase currentCase = getCase(sX, sY);
                    destValue =
                            data.processWindowNoData(
                                    window, x, srcPixelOffset, centerScanlineOffset, currentCase);
                    dstData[dstPixelOffset] =
                            Double.isNaN(destValue)
                                    ? destNoDataTyped
                                    : (float)destValue; // checkme clamp
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        } else { // ROI and No Data Check
            for (int y = 0; y < dheight; y++) {
                srcPixelOffset = srcScanlineOffset;
                dstPixelOffset = dstScanlineOffset;

                y0 = srcY + y;

                for (int x = 0; x < dwidth; x++) {
                    x0 = srcX + x;
                    boolean inROI = false;
                    // ROI Check
                    for (int dy = 0; dy < 3; dy++) {
                        int yI = y0 + dy;
                        for (int dx = 0; dx < 3; dx++) {
                            int xI = x0 + dx;
                            if (roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0) {
                                inROI = true;
                                roiMask[dx + (3 * dy)] = true;
                            } else {
                                roiMask[dx + (3 * dy)] = false;
                            }
                        }
                    }

                    if (inROI) {
                        int sX = x + dstX;
                        int sY = y + dstY;
                        ProcessingCase currentCase = getCase(sX, sY);
                        destValue =
                                data.processWindowRoiNoData(
                                        window,
                                        x,
                                        srcPixelOffset,
                                        centerScanlineOffset,
                                        currentCase,
                                        roiMask);
                        dstData[dstPixelOffset] =
                                Double.isNaN(destValue)
                                        ? destNoDataTyped
                                        : (float)destValue; // checkme clamp
                    } else {
                        dstData[dstPixelOffset] = destNoDataTyped;
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    protected void doubleLoop(
            RasterAccessor src, RasterAccessor dst, RandomIter roiIter, boolean roiContainsTile) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();

        double destNoDataTyped = dstNoData;

        double dstDataArrays[][] = dst.getDoubleDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        double srcDataArrays[][] = src.getDoubleDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // precalcaculate offsets
        int centerScanlineOffset = srcScanlineStride;
        int dstX = dst.getX();
        int dstY = dst.getY();

        // X,Y positions
        int x0 = 0;
        int y0 = 0;
        int srcX = src.getX();
        int srcY = src.getY();

        double[] window = new double[9];
        boolean[] roiMask = new boolean[9];

        double dstData[] = dstDataArrays[0];
        double srcData[] = srcDataArrays[0];
        int srcScanlineOffset = srcBandOffsets[0];
        int dstScanlineOffset = dstBandOffsets[0];
        int srcPixelOffset;
        int dstPixelOffset;
        double destValue = Double.NaN;
        DataProcessor data =
                new DataProcessorDouble(srcData, hasNoData, srcNoData, dstNoData, params);

        if (caseA || (caseB && roiContainsTile)) {
            for (int y = 0; y < dheight; y++) {
                srcPixelOffset = srcScanlineOffset;
                dstPixelOffset = dstScanlineOffset;

                for (int x = 0; x < dwidth; x++) {
                    int sX = x + dstX;
                    int sY = y + dstY;
                    ProcessingCase currentCase = getCase(sX, sY);
                    destValue =
                            data.processWindow(
                                    window, x, srcPixelOffset, centerScanlineOffset, currentCase);
                    dstData[dstPixelOffset] = destValue;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }

        } else if (caseB) { // ROI Check
            for (int y = 0; y < dheight; y++) {
                srcPixelOffset = srcScanlineOffset;
                dstPixelOffset = dstScanlineOffset;

                y0 = srcY + y;

                for (int x = 0; x < dwidth; x++) {
                    x0 = srcX + x;

                    boolean inROI = false;
                    // ROI Check
                    for (int dy = 0; dy < 3; dy++) {
                        int yI = y0 + dy;
                        for (int dx = 0; dx < 3; dx++) {
                            int xI = x0 + dx;
                            if (roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0) {
                                inROI = true;
                                roiMask[dx + (3 * dy)] = true;
                            } else {
                                roiMask[dx + (3 * dy)] = false;
                            }
                        }
                    }

                    if (inROI) {
                        int sX = x + dstX;
                        int sY = y + dstY;
                        ProcessingCase currentCase = getCase(sX, sY);
                        destValue =
                                data.processWindowRoi(
                                        window,
                                        x,
                                        srcPixelOffset,
                                        centerScanlineOffset,
                                        currentCase,
                                        roiMask);
                        dstData[dstPixelOffset] = destValue;
                    } else {
                        dstData[dstPixelOffset] = destNoDataTyped;
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) { // NoData Check
            for (int y = 0; y < dheight; y++) {
                srcPixelOffset = srcScanlineOffset;
                dstPixelOffset = dstScanlineOffset;

                for (int x = 0; x < dwidth; x++) {
                    int sX = x + dstX;
                    int sY = y + dstY;
                    ProcessingCase currentCase = getCase(sX, sY);
                    destValue =
                            data.processWindowNoData(
                                    window, x, srcPixelOffset, centerScanlineOffset, currentCase);
                    dstData[dstPixelOffset] =
                            Double.isNaN(destValue)
                                    ? destNoDataTyped
                                    : destValue;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }

        } else { // ROI and No Data Check
            for (int y = 0; y < dheight; y++) {
                srcPixelOffset = srcScanlineOffset;
                dstPixelOffset = dstScanlineOffset;

                y0 = srcY + y;

                for (int x = 0; x < dwidth; x++) {
                    x0 = srcX + x;
                    boolean inROI = false;
                    // ROI Check
                    for (int dy = 0; dy < 3; dy++) {
                        int yI = y0 + dy;
                        for (int dx = 0; dx < 3; dx++) {
                            int xI = x0 + dx;
                            if (roiBounds.contains(xI, yI) && roiIter.getSample(xI, yI, 0) > 0) {
                                inROI = true;
                                roiMask[dx + (3 * dy)] = true;
                            } else {
                                roiMask[dx + (3 * dy)] = false;
                            }
                        }
                    }

                    if (inROI) {
                        int sX = x + dstX;
                        int sY = y + dstY;
                        ProcessingCase currentCase = getCase(sX, sY);
                        destValue =
                                data.processWindowRoiNoData(
                                        window,
                                        x,
                                        srcPixelOffset,
                                        centerScanlineOffset,
                                        currentCase,
                                        roiMask);
                        dstData[dstPixelOffset] =
                                Double.isNaN(destValue)
                                        ? destNoDataTyped
                                        : destValue;
                    } else {
                        dstData[dstPixelOffset] = destNoDataTyped;
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private ProcessingCase getCase(int x, int y) {
        if (y == minY) {
            if (x == minX) {
                return ProcessingCase.TOP_LEFT;
            } else if (x == maxX) {
                return ProcessingCase.TOP_RIGHT;
            } else {
                return ProcessingCase.TOP;
            }
        }
        else if (y == maxY) {
            if (x == minX) {
                return ProcessingCase.BOTTOM_LEFT;
            } else if (y == maxY) {
                return ProcessingCase.BOTTOM_RIGHT;
            } else {
                return ProcessingCase.BOTTOM;
            }
        } else if (x == minX) {
            return ProcessingCase.LEFT;
        } else if (x == maxX) {
            return ProcessingCase.RIGHT;
        } else {
            return ProcessingCase.STANDARD;
        }
    }
}
