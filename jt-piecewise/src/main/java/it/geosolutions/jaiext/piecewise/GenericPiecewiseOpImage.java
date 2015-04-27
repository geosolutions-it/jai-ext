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
package it.geosolutions.jaiext.piecewise;

import it.geosolutions.jaiext.iterators.RandomIterFactory;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RasterFormatException;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Arrays;

import javax.media.jai.ColormapOpImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;

import com.sun.media.jai.util.ImageUtil;

/**
 * Images are created using the {@code GenericPiecewise.CRIF} inner class, where "CRIF" stands for
 * {@link java.awt.image.renderable.ContextualRenderedImageFactory} . The image operation name is "GenericPiecewise".
 * 
 * @author Simone Giannecchini - GeoSolutions
 */
public class GenericPiecewiseOpImage<T extends PiecewiseTransform1DElement> extends ColormapOpImage {

    /**
     * The operation name.
     */
    public static final String OPERATION_NAME = "GenericPiecewise";

    /** Constant indicating that the inner random iterators must pre-calculate an array of the image positions */
    public static final boolean ARRAY_CALC = true;

    /** Constant indicating that the inner random iterators must cache the current tile position */
    public static final boolean TILE_CACHED = true;

    /**
     * DefaultPiecewiseTransform1D that we'll use to transform this image. We'll apply it ato all of its bands.
     */
    private final PiecewiseTransform1D<T> piecewise;

    /** Boolean indicating if the output samplemodel has a Byte dataType */
    protected boolean isByteData;

    /** Byte LookupTable used for quickly calculating piecewise operation for Byte data */
    private byte[][] lut;

    /** Output NoData */
    private double gapsValue = Double.NaN;

    /** Boolean indicating if output nodata has been defined */
    private boolean hasGapsValue = false;

    /** Boolean indicating that the Last DefaultDomain can be used */
    private final boolean useLast;

    /** Boolean indicating that NoData Range is present */
    private final boolean hasNoData;

    /** Boolean indicating that ROI is present */
    private final boolean hasROI;

    /** NoData Range used for checking input NoData */
    private Range nodata;

    /** ROI object used for reducing calculations */
    private ROI roi;

    /** Rectangle containing ROI bounds */
    private Rectangle roiBounds;

    /** {@link PlanarImage} containing ROI data */
    private PlanarImage roiImage;

    /** Output NoData for Bytes */
    private byte gapsValueByte;

    /** Boolean indicating that there No Data and ROI are not used */
    private final boolean caseA;

    /** Boolean indicating that only ROI is used */
    private final boolean caseB;

    /** Boolean indicating that only No Data are used */
    private final boolean caseC;

    /** Optional value used for indicating that the calculations are made only on one band */
    private Integer bandIndex;

    /**
     * Constructs a new {@code RasterClassifier}.
     * 
     * @param image The source image.
     * @param lic The DefaultPiecewiseTransform1D.
     * @param bandIndex index used for defining the band to calculate
     * @param roi {@link ROI} used for reducing computation area
     * @param nodata {@link Range} used for defining NoData values
     * @param hints The rendering hints.
     */
    public GenericPiecewiseOpImage(final RenderedImage image, final PiecewiseTransform1D<T> lic,
            ImageLayout layout, Integer bandIndex, ROI roi, Range nodata,
            final RenderingHints hints, boolean cobbleSources) {
        super(image, layout, hints, cobbleSources);
        this.piecewise = lic;
        // Ensure that the number of sets of breakpoints is either unity
        // or equal to the number of bands.
        final int numBands = sampleModel.getNumBands();

        // Check the bandIndex value
        if (bandIndex != null) {
            this.bandIndex = bandIndex;
        }

        // Set the byte data flag.
        isByteData = sampleModel.getTransferType() == DataBuffer.TYPE_BYTE;

        // ////////////////////////////////////////////////////////////////////
        //
        // Check if we can make good use of a default piecewise element for filling gaps
        // in the input range
        //
        // ////////////////////////////////////////////////////////////////////
        if (this.piecewise.hasDefaultValue()) {
            gapsValue = piecewise.getDefaultValue();
            hasGapsValue = true;
            gapsValueByte = ImageUtil.clampRoundByte(gapsValue);
        }

        // Handling NoData
        hasNoData = nodata != null;
        if (hasNoData) {
            this.nodata = RangeFactory.convertToDoubleRange(nodata);
        }

        // Handling ROI (Notice that ROI is not considered when the ColorModel is IndexColorModel, source
        // image must have a component colormodel)
        hasROI = roi != null;
        if (hasROI) {
            this.roi = roi;
            roiBounds = roi.getBounds();
        }

        // Definition of the possible cases that can be found
        // caseA = no ROI nor No Data
        // caseB = ROI present but No Data not present
        // caseC = No Data present but ROI not present
        // Last case not defined = both ROI and No Data are present
        caseA = !hasNoData && !hasROI;
        caseB = !hasNoData && hasROI;
        caseC = hasNoData && !hasROI;

        if (!caseA && !hasGapsValue) {
            throw new IllegalArgumentException(
                    "Unable to set input Gap valuein presence of NoData and ROI");
        }

        // ////////////////////////////////////////////////////////////////////
        //
        // Check if we can optimize this operation by reusing the last used
        // piecewise element first. The speed up we get can be substantial since we avoid
        // an explicit search in the piecewise element list for the fitting piecewise element given
        // a certain sample value.
        //
        //
        // ////////////////////////////////////////////////////////////////////
        useLast = piecewise instanceof DefaultDomain1D;

        // Perform byte-specific initialization.
        if (isByteData) {
            // Initialize the lookup table.
            try {
                createLUT(numBands);
            } catch (final TransformationException e) {
                final RuntimeException re = new RuntimeException(e);
                throw re;
            }
        }

        // Set flag to permit in-place operation.
        permitInPlaceOperation();

        // Initialize the colormap if necessary.
        initializeColormapOperation();
    }

    @Override
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {

        // ROI Parameters initialization
        ROI roiTile = null;

        RandomIter roiIter = null;

        boolean roiContainsTile = false;
        boolean roiDisjointTile = false;

        // If a ROI is present, then only the part contained inside the current tile bounds is taken.
        if (hasROI) {
            Rectangle srcRectExpanded = mapDestRect(destRect, 0);
            // The tile dimension is extended for avoiding border errors
            srcRectExpanded.setRect(srcRectExpanded.getMinX() - 1, srcRectExpanded.getMinY() - 1,
                    srcRectExpanded.getWidth() + 2, srcRectExpanded.getHeight() + 2);
            roiTile = roi.intersect(new ROIShape(srcRectExpanded));

            if (!roiBounds.intersects(srcRectExpanded)) {
                roiDisjointTile = true;
            } else {
                roiContainsTile = roiTile.contains(srcRectExpanded);
                if (!roiContainsTile) {
                    if (!roiTile.intersects(srcRectExpanded)) {
                        roiDisjointTile = true;
                    } else {
                        PlanarImage roiIMG = getImage();
                        roiIter = RandomIterFactory.create(roiIMG, null, TILE_CACHED, ARRAY_CALC);
                    }
                }
            }
        }

        // Check on the num bands
        if (sources[0].getNumBands() != dest.getNumBands()) {
            throw new IllegalArgumentException(
                    "Sourc and Destination image must have the same Bands");
        }

        // Creating the RasterAccessors and testing
        if (!hasROI || !roiDisjointTile) {
            if (isByteData) {
                computeRectByte(sources[0], dest, destRect, roiIter, roiContainsTile);
            } else {
                computeRectGeneral(sources[0], dest, destRect, roiIter, roiContainsTile);
            }
        } else {
            // //
            //
            // if the tile is outside ROI
            // we use domain nodata for filling the tile bounds
            //
            // //
            double[] background = new double[dest.getSampleModel().getNumBands()];
            Arrays.fill(background, gapsValue);
            ImageUtil.fillBackground(dest, destRect, background);

        }
    }

    private void computeRectByte(final Raster source, final WritableRaster dest,
            final Rectangle destRect, RandomIter roiIter, boolean roiContainsTile) {

        int srcX = destRect.x;
        int srcY = destRect.y;

        // Input position parameters
        int x0 = srcX;
        int y0 = srcY;

        WritableRectIter dstIter = RectIterFactory.createWritable(dest, destRect);
        RectIter srcIter = RectIterFactory.create(source, destRect);

        // ////////////////////////////////////////////////////////////////////
        //
        // Prepare the iterator to work on the correct bands, if this is
        // requested.
        //
        // ////////////////////////////////////////////////////////////////////
        if (!dstIter.finishedBands() && !srcIter.finishedBands()) {
            for (int i = 0; i < bandIndex; i++) {
                dstIter.nextBand();
                srcIter.nextBand();
            }
        }

        if (hasROI) {
            int bandNumber = 0;
            do {
                try {
                    dstIter.startLines();
                    srcIter.startLines();
                    if (!dstIter.finishedLines() && !srcIter.finishedLines())
                        do {
                            dstIter.startPixels();
                            srcIter.startPixels();
                            if (!dstIter.finishedPixels() && !srcIter.finishedPixels())
                                do {
                                    // //
                                    //
                                    // get the input value to be transformed
                                    //
                                    // //
                                    final int in = srcIter.getSample() & 0xff;
                                    if (!(roiBounds.contains(x0, y0) && roiIter
                                            .getSample(x0, y0, 0) > 0)) {
                                        dstIter.setSample(gapsValue);
                                    } else {
                                        // //
                                        //
                                        // Transform this element
                                        //
                                        // //
                                        final int out = 0xff & lut[bandNumber][in];
                                        // //
                                        //
                                        // Set the result
                                        //
                                        // //
                                        dstIter.setSample(out);
                                    }
                                    x0++;
                                } while (!dstIter.nextPixelDone() && !srcIter.nextPixelDone());
                            y0++;
                            x0 = srcX;
                        } while (!dstIter.nextLineDone() && !srcIter.nextLineDone());
                } catch (final Exception cause) {
                    final RasterFormatException exception = new RasterFormatException(
                            cause.getLocalizedMessage());
                    exception.initCause(cause);
                    throw exception;
                }
                bandNumber++;
                y0 = srcY;
                x0 = srcX;
                if (bandIndex != -1)
                    break;
            } while (dstIter.finishedBands() && srcIter.finishedBands());
        } else {
            int bandNumber = 0;
            do {
                try {
                    dstIter.startLines();
                    srcIter.startLines();
                    if (!dstIter.finishedLines() && !srcIter.finishedLines())
                        do {
                            dstIter.startPixels();
                            srcIter.startPixels();
                            if (!dstIter.finishedPixels() && !srcIter.finishedPixels())
                                do {
                                    // //
                                    //
                                    // get the input value to be transformed
                                    //
                                    // //
                                    final int in = srcIter.getSample() & 0xff;
                                    // //
                                    //
                                    // Transform this element
                                    //
                                    // //
                                    final int out = 0xff & lut[bandNumber][in];
                                    // //
                                    //
                                    // Set the result
                                    //
                                    // //
                                    dstIter.setSample(out);
                                } while (!dstIter.nextPixelDone() && !srcIter.nextPixelDone());
                        } while (!dstIter.nextLineDone() && !srcIter.nextLineDone());
                } catch (final Exception cause) {
                    final RasterFormatException exception = new RasterFormatException(
                            cause.getLocalizedMessage());
                    exception.initCause(cause);
                    throw exception;
                }
                bandNumber++;
                if (bandIndex != -1)
                    break;
            } while (dstIter.finishedBands() && srcIter.finishedBands());
        }
    }

    private void computeRectGeneral(final Raster source, final WritableRaster dest,
            final Rectangle destRect, RandomIter roiIter, boolean roiContainsTile) {

        int srcX = destRect.x;
        int srcY = destRect.y;

        // Input position parameters
        int x0 = srcX;
        int y0 = srcY;

        PiecewiseTransform1DElement element = null;

        WritableRectIter dstIter = RectIterFactory.createWritable(dest, destRect);
        RectIter srcIter = RectIterFactory.create(source, destRect);

        if (!dstIter.finishedBands() && !srcIter.finishedBands()) {
            for (int i = 0; i < bandIndex; i++) {
                dstIter.nextBand();
                srcIter.nextBand();
            }
        }

        if (caseA || (caseB && roiContainsTile)) {
            do {
                try {
                    dstIter.startLines();
                    srcIter.startLines();
                    if (!dstIter.finishedLines() && !srcIter.finishedLines())
                        do {
                            dstIter.startPixels();
                            srcIter.startPixels();
                            if (!dstIter.finishedPixels() && !srcIter.finishedPixels())
                                do {
                                    // //
                                    //
                                    // get the input value to be transformed
                                    //
                                    // //
                                    final double value = srcIter.getSampleDouble();
                                    // //
                                    //
                                    // get the correct piecewise element for this
                                    // transformation
                                    //
                                    // //
                                    element = domainSearch(element, value);
                                    // //
                                    //
                                    // in case everything went fine let's apply the
                                    // transform.
                                    //
                                    // //
                                    if (element != null)
                                        dstIter.setSample(element.transform(value));
                                    else {
                                        // //
                                        //
                                        // if we did not find one let's try to use
                                        // one of the nodata ones to fill the gaps,
                                        // if we are allowed to (see above).
                                        //
                                        // //
                                        if (hasGapsValue)
                                            dstIter.setSample(gapsValue);
                                        else
                                            // //
                                            //
                                            // if we did not find one let's throw a
                                            // nice error message
                                            //
                                            // //
                                            throw new IllegalArgumentException(
                                                    "Unable to set input Gap value");
                                    }
                                } while (!dstIter.nextPixelDone() && !srcIter.nextPixelDone());
                        } while (!dstIter.nextLineDone() && !srcIter.nextLineDone());
                } catch (final Exception cause) {
                    final RasterFormatException exception = new RasterFormatException(
                            cause.getLocalizedMessage());
                    exception.initCause(cause);
                    throw exception;
                }
                if (bandIndex != -1)
                    break;
            } while (dstIter.finishedBands() && srcIter.finishedBands());
        } else if (caseB) {
            do {
                try {
                    dstIter.startLines();
                    srcIter.startLines();
                    if (!dstIter.finishedLines() && !srcIter.finishedLines())
                        do {
                            dstIter.startPixels();
                            srcIter.startPixels();
                            if (!dstIter.finishedPixels() && !srcIter.finishedPixels())
                                do {
                                    if (!(roiBounds.contains(x0, y0) && roiIter
                                            .getSample(x0, y0, 0) > 0)) {
                                        dstIter.setSample(gapsValue);
                                    } else {

                                        final double value = srcIter.getSampleDouble();

                                        element = domainSearch(element, value);

                                        if (element != null)
                                            dstIter.setSample(element.transform(value));
                                        else {

                                            if (hasGapsValue)
                                                dstIter.setSample(gapsValue);
                                            else

                                                throw new IllegalArgumentException(
                                                        "Unable to set input Gap value");
                                        }

                                    }
                                    x0++;
                                } while (!dstIter.nextPixelDone() && !srcIter.nextPixelDone());
                            y0++;
                            x0 = srcX;
                        } while (!dstIter.nextLineDone() && !srcIter.nextLineDone());
                } catch (final Exception cause) {
                    final RasterFormatException exception = new RasterFormatException(
                            cause.getLocalizedMessage());
                    exception.initCause(cause);
                    throw exception;
                }
                y0 = srcY;
                x0 = srcX;
                if (bandIndex != -1)
                    break;
            } while (dstIter.finishedBands() && srcIter.finishedBands());
        } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
            do {
                try {
                    dstIter.startLines();
                    srcIter.startLines();
                    if (!dstIter.finishedLines() && !srcIter.finishedLines())
                        do {
                            dstIter.startPixels();
                            srcIter.startPixels();
                            if (!dstIter.finishedPixels() && !srcIter.finishedPixels())
                                do {

                                    final double value = srcIter.getSampleDouble();

                                    if (nodata.contains(value)) {
                                        dstIter.setSample(gapsValue);
                                    } else {

                                        element = domainSearch(element, value);

                                        if (element != null)
                                            dstIter.setSample(element.transform(value));
                                        else {

                                            if (hasGapsValue)
                                                dstIter.setSample(gapsValue);
                                            else

                                                throw new IllegalArgumentException(
                                                        "Unable to set input Gap value");
                                        }
                                    }
                                } while (!dstIter.nextPixelDone() && !srcIter.nextPixelDone());
                        } while (!dstIter.nextLineDone() && !srcIter.nextLineDone());
                } catch (final Exception cause) {
                    final RasterFormatException exception = new RasterFormatException(
                            cause.getLocalizedMessage());
                    exception.initCause(cause);
                    throw exception;
                }
                if (bandIndex != -1)
                    break;
            } while (dstIter.finishedBands() && srcIter.finishedBands());
        } else {
            do {
                try {
                    dstIter.startLines();
                    srcIter.startLines();
                    if (!dstIter.finishedLines() && !srcIter.finishedLines())
                        do {
                            dstIter.startPixels();
                            srcIter.startPixels();
                            if (!dstIter.finishedPixels() && !srcIter.finishedPixels())
                                do {
                                    if (!(roiBounds.contains(x0, y0) && roiIter
                                            .getSample(x0, y0, 0) > 0)) {
                                        dstIter.setSample(gapsValue);
                                    } else {

                                        final double value = srcIter.getSampleDouble();

                                        if (nodata.contains(value)) {
                                            dstIter.setSample(gapsValue);
                                        } else {

                                            element = domainSearch(element, value);

                                            if (element != null)
                                                dstIter.setSample(element.transform(value));
                                            else {

                                                if (hasGapsValue)
                                                    dstIter.setSample(gapsValue);
                                                else

                                                    throw new IllegalArgumentException(
                                                            "Unable to set input Gap value");
                                            }

                                        }
                                    }
                                    x0++;
                                } while (!dstIter.nextPixelDone() && !srcIter.nextPixelDone());
                            y0++;
                            x0 = srcX;
                        } while (!dstIter.nextLineDone() && !srcIter.nextLineDone());
                } catch (final Exception cause) {
                    final RasterFormatException exception = new RasterFormatException(
                            cause.getLocalizedMessage());
                    exception.initCause(cause);
                    throw exception;
                }
                y0 = srcY;
                x0 = srcX;
                if (bandIndex != -1)
                    break;
            } while (dstIter.finishedBands() && srcIter.finishedBands());
        }
    }

    private PiecewiseTransform1DElement domainSearch(PiecewiseTransform1DElement last, double value)
            throws TransformationException {
        // //
        //
        // get the correct piecewise element for this
        // transformation
        //
        // //
        final PiecewiseTransform1DElement transformElement;
        if (useLast) {
            if (last != null && last.contains(value))
                transformElement = last;
            else {
                last = transformElement = (PiecewiseTransform1DElement) piecewise
                        .findDomainElement(value);
            }
        } else
            transformElement = (PiecewiseTransform1DElement) piecewise.findDomainElement(value);

        return transformElement;
    }

    /**
     * Create a lookup table to be used in the case of byte data.
     * 
     * @param numBands
     * @throws TransformationException
     */
    private void createLUT(final int numBands) throws TransformationException {
        // Allocate memory for the data array references.
        final byte[][] data = new byte[numBands][];

        // Generate the data for each band.
        for (int band = 0; band < numBands; band++) {
            // Allocate memory for this band.
            data[band] = new byte[256];

            // Cache the references to avoid extra indexing.
            final byte[] table = data[band];

            // Initialize the lookup table data.
            PiecewiseTransform1DElement lastPiecewiseElement = null;
            for (int value = 0; value < 256; value++) {

                boolean isNoData = hasNoData && nodata.contains((byte) value);

                if (isNoData) {
                    // //
                    //
                    // if we did find a nodata let's try to use
                    // one of the destination nodata to fill the gaps,
                    //
                    // //
                    table[value] = gapsValueByte;
                    continue;
                }

                // //
                //
                // get the correct piecewise element for this
                // transformation
                //
                // //
                final PiecewiseTransform1DElement piecewiseElement;
                if (useLast) {
                    if (lastPiecewiseElement != null && lastPiecewiseElement.contains(value))
                        piecewiseElement = lastPiecewiseElement;
                    else {
                        lastPiecewiseElement = piecewiseElement = (PiecewiseTransform1DElement) piecewise
                                .findDomainElement(value);
                    }
                } else
                    piecewiseElement = (PiecewiseTransform1DElement) piecewise
                            .findDomainElement(value);

                // //
                //
                // in case everything went fine let's apply the
                // transform.
                //
                // //
                if (piecewiseElement != null)
                    table[value] = ImageUtil.clampRoundByte(piecewiseElement.transform(value));
                else {
                    // //
                    //
                    // if we did not find one let's try to use
                    // one of the nodata ones to fill the gaps,
                    // if we are allowed to (see above).
                    //
                    // //
                    if (hasGapsValue)
                        table[value] = ImageUtil.clampRoundByte(gapsValue);
                    else

                        // //
                        //
                        // if we did not find one let's throw a
                        // nice error message
                        //
                        // //
                        throw new IllegalArgumentException("Unable to set the Gap value");
                }
            }
        }

        // Construct the lookup table.
        lut = data;
    }

    /**
     * Transform the colormap according to the rescaling parameters.
     */
    protected void transformColormap(final byte[][] colormap) {

        for (int b = 0; b < 3; b++) {
            final byte[] map = colormap[b];
            final byte[] luTable = lut[b >= lut.length ? 0 : b];
            final int mapSize = map.length;

            for (int i = 0; i < mapSize; i++) {
                map[i] = luTable[(map[i] & 0xFF)];
            }
        }
    }

    /**
     * This method provides a lazy initialization of the image associated to the ROI. The method uses the Double-checked locking in order to maintain
     * thread-safety
     * 
     * @return
     */
    private PlanarImage getImage() {
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
}
