/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2005-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
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
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

import com.sun.media.jai.util.ImageUtil;

/**
 * Images are created using the {@code            GenericPiecewise.CRIF} inner class, where "CRIF" stands for
 * {@link java.awt.image.renderable.ContextualRenderedImageFactory} . The image operation name is "org.geotools.GenericPiecewise".
 * 
 * 
 * 
 * @source $URL$
 * @version $Id$
 * @author Simone Giannecchini - GeoSolutions
 * @since 2.4
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

    private final boolean isByteData;

    private byte[][] lut;

    private double gapsValue = Double.NaN;

    private boolean hasGapsValue = false;

    private final boolean useLast;

    private final boolean hasNoData;

    private final boolean hasROI;

    private Range nodata;

    private ROI roi;

    private Rectangle roiBounds;

    private PlanarImage roiImage;

    private byte gapsValueByte;

    /** Boolean indicating that there No Data and ROI are not used */
    private final boolean caseA;

    /** Boolean indicating that only ROI is used */
    private final boolean caseB;

    /** Boolean indicating that only No Data are used */
    private final boolean caseC;

    private Integer bandIndex;

    private boolean indexDefined;

    /**
     * Constructs a new {@code RasterClassifier}.
     * 
     * @param image The source image.
     * @param lic The DefaultPiecewiseTransform1D.
     * @param bandIndex
     * @param hints The rendering hints.
     */
    public GenericPiecewiseOpImage(final RenderedImage image, final PiecewiseTransform1D<T> lic,
            ImageLayout layout, Integer bandIndex, ROI roi, Range nodata, final RenderingHints hints) {
        super(image, layout, hints, true);
        this.piecewise = lic;
        // Ensure that the number of sets of breakpoints is either unity
        // or equal to the number of bands.
        final int numBands = sampleModel.getNumBands();
        
        // Check the bandIndex value
        if(bandIndex != null){
            this.bandIndex = bandIndex;
            this.indexDefined = bandIndex != null && bandIndex !=-1;
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
        
        // Check on the tile size
        if (sources[0].getWidth() != dest.getWidth()) {
            throw new IllegalArgumentException(
                    "Sourc and Destination image must have the same width");
        }
        if (sources[0].getHeight() != dest.getHeight()) {
            throw new IllegalArgumentException(
                    "Sourc and Destination image must have the same Height");
        }
        if (sources[0].getNumBands() != dest.getNumBands()) {
            throw new IllegalArgumentException(
                    "Sourc and Destination image must have the same Bands");
        }

        // Creating the RasterAccessors

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

        // Input position parameters
        int x0 = 0;
        int y0 = 0;

        int srcX = source.getMinX();
        int srcY = source.getMinY();

        int dstWidth = destRect.width;
        int dstHeight = destRect.height;
        int dstBands = indexDefined ? bandIndex + 1 : dest.getNumBands();
        int startBand = indexDefined ? bandIndex : 0;

        WritableRandomIter dstIter = RandomIterFactory.createWritable(dest, destRect);
        RandomIter srcIter = RandomIterFactory.create(source, destRect, ARRAY_CALC, ARRAY_CALC);

        if (hasROI) {
            for (int h = 0; h < dstHeight; h++) {

                y0 = srcY + h;

                for (int w = 0; w < dstWidth; w++) {

                    x0 = srcX + w;

                    if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                        for (int b = startBand; b < dstBands; b++) {
                            dstIter.setSample(x0, y0, b, gapsValue);
                        }
                        continue;
                    }

                    for (int b = startBand; b < dstBands; b++) {
                        // //
                        //
                        // get the input value to be transformed
                        //
                        // //
                        final int value = srcIter.getSample(x0, y0, b);
                        // //
                        //
                        // Transform this element
                        //
                        // //
                        int result = lut[b][value];
                        // //
                        //
                        // Set the result
                        //
                        // //
                        dstIter.setSample(x0, y0, b, result);
                    }
                }
            }
        } else {
            for (int h = 0; h < dstHeight; h++) {

                y0 = srcY + h;

                for (int w = 0; w < dstWidth; w++) {

                    x0 = srcX + w;

                    for (int b = startBand; b < dstBands; b++) {
                        // //
                        //
                        // get the input value to be transformed
                        //
                        // //
                        final int value = srcIter.getSample(x0, y0, b);
                        // //
                        //
                        // Transform this element
                        //
                        // //
                        int result = lut[b][value];
                        // //
                        //
                        // Set the result
                        //
                        // //
                        dstIter.setSample(x0, y0, b, result);
                    }
                }
            }
        }
    }

    private void computeRectGeneral(final Raster source, final WritableRaster dest,
            final Rectangle destRect, RandomIter roiIter, boolean roiContainsTile) {

        // Input position parameters
        int x0 = 0;
        int y0 = 0;

        int srcX = source.getMinX();
        int srcY = source.getMinY();

        int dstWidth = destRect.width;
        int dstHeight = destRect.height;
        int dstBands = indexDefined ? bandIndex + 1 : dest.getNumBands();
        int startBand = indexDefined ? bandIndex : 0;

        PiecewiseTransform1DElement element = null;

        WritableRandomIter dstIter = RandomIterFactory.createWritable(dest, destRect);
        RandomIter srcIter = RandomIterFactory.create(source, destRect, ARRAY_CALC, ARRAY_CALC);
        try {
            if (caseA || (caseB && roiContainsTile)) {
                for (int h = 0; h < dstHeight; h++) {

                    y0 = srcY + h;

                    for (int w = 0; w < dstWidth; w++) {

                        x0 = srcX + w;

                        for (int b = startBand; b < dstBands; b++) {
                            // //
                            //
                            // get the input value to be transformed
                            //
                            // //
                            final double value = srcIter.getSampleDouble(x0, y0, b);
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
                                dstIter.setSample(x0, y0, b, element.transform(value));
                            else {
                                // //
                                //
                                // if we did not find one let's try to use
                                // one of the nodata ones to fill the gaps,
                                // if we are allowed to (see above).
                                //
                                // //
                                if (hasGapsValue)
                                    dstIter.setSample(x0, y0, b, gapsValue);
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
                        }
                    }
                }
            } else if (caseB) {
                for (int h = 0; h < dstHeight; h++) {

                    y0 = srcY + h;

                    for (int w = 0; w < dstWidth; w++) {

                        x0 = srcX + w;

                        if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                            for (int b = startBand; b < dstBands; b++) {
                                dstIter.setSample(x0, y0, b, gapsValue);
                            }
                            continue;
                        }

                        for (int b = startBand; b < dstBands; b++) {
                            // //
                            //
                            // get the input value to be transformed
                            //
                            // //
                            final double value = srcIter.getSampleDouble(x0, y0, b);
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
                                dstIter.setSample(x0, y0, b, element.transform(value));
                            else {
                                // //
                                //
                                // if we did not find one let's try to use
                                // one of the nodata ones to fill the gaps,
                                // if we are allowed to (see above).
                                //
                                // //
                                if (hasGapsValue)
                                    dstIter.setSample(x0, y0, b, gapsValue);
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
                        }
                    }
                }
            } else if (caseC || (hasROI && hasNoData && roiContainsTile)) {
                for (int h = 0; h < dstHeight; h++) {

                    y0 = srcY + h;

                    for (int w = 0; w < dstWidth; w++) {

                        x0 = srcX + w;

                        for (int b = startBand; b < dstBands; b++) {

                            final double value = srcIter.getSampleDouble(x0, y0, b);

                            if (nodata.contains(value)) {
                                dstIter.setSample(x0, y0, b, gapsValue);
                            } else {
                                element = domainSearch(element, value);

                                if (element != null)
                                    dstIter.setSample(x0, y0, b, element.transform(value));
                                else {

                                    if (hasGapsValue) {
                                        dstIter.setSample(x0, y0, b, gapsValue);
                                    } else {
                                        throw new IllegalArgumentException(
                                                "Unable to set input Gap value");
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                for (int h = 0; h < dstHeight; h++) {

                    y0 = srcY + h;

                    for (int w = 0; w < dstWidth; w++) {

                        x0 = srcX + w;

                        if (!(roiBounds.contains(x0, y0) && roiIter.getSample(x0, y0, 0) > 0)) {
                            for (int b = startBand; b < dstBands; b++) {
                                dstIter.setSample(x0, y0, b, gapsValue);
                            }
                            continue;
                        }

                        for (int b = startBand; b < dstBands; b++) {

                            final double value = srcIter.getSampleDouble(x0, y0, b);

                            if (nodata.contains(value)) {
                                dstIter.setSample(x0, y0, b, gapsValue);
                            } else {
                                element = domainSearch(element, value);

                                if (element != null)
                                    dstIter.setSample(x0, y0, b, element.transform(value));
                                else {

                                    if (hasGapsValue) {
                                        dstIter.setSample(x0, y0, b, gapsValue);
                                    } else {
                                        throw new IllegalArgumentException(
                                                "Unable to set input Gap value");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            final RasterFormatException exception = new RasterFormatException(
                    e.getLocalizedMessage());
            exception.initCause(e);
            throw exception;
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
