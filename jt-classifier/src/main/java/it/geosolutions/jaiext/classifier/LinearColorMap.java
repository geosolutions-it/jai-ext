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
package it.geosolutions.jaiext.classifier;

import it.geosolutions.jaiext.piecewise.DefaultLinearPiecewiseTransform1DElement;
import it.geosolutions.jaiext.piecewise.DefaultPiecewiseTransform1D;
import it.geosolutions.jaiext.piecewise.PiecewiseTransform1DElement;
import it.geosolutions.jaiext.piecewise.PiecewiseUtilities;
import it.geosolutions.jaiext.piecewise.Position;
import it.geosolutions.jaiext.piecewise.TransformationException;
import it.geosolutions.jaiext.range.Range;

import java.awt.Color;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.math.BigInteger;
import java.util.AbstractList;
import java.util.Arrays;

import javax.xml.crypto.dsig.TransformException;

/**
 * {@link ColorMapTransform} implementation defining a linear colormap transformation
 * 
 * @author Simone Giannecchini, GeoSolutions.
 * 
 * @source $URL$
 */
public final class LinearColorMap extends AbstractList<LinearColorMapElement> implements
        ColorMapTransform<LinearColorMapElement> {

    public final static class LinearColorMapType {
        /**
         * Values are displayed using a gradual ramp of colors.
         */
        public static final int TYPE_RAMP = 1;

        /**
         * Each value in the raster layer is to be displayed individually as single separate color.
         */
        public static final int TYPE_VALUES = 3;

        /**
         * Vales are grouped into classes in order to display each class with a single separate color.
         */
        public static final int TYPE_INTERVALS = 2;

        /**
         * Validates the provided color map type.
         * 
         * @param linearColorMapType
         * @return <code>false</code> if we cannot valid the provided type, <code>true</code> otherwise.
         */
        static public boolean validateColorMapTye(final int linearColorMapType) {
            switch (linearColorMapType) {
            case TYPE_RAMP:
            case TYPE_VALUES:
            case TYPE_INTERVALS:
                return true;
            default:
                return false;
            }
        }
    }

    /**
     * @uml.property name="colorModel"
     */
    private IndexColorModel colorModel;

    /**
     * @uml.property name="standardElements"
     * @uml.associationEnd multiplicity="(0 -1)"
     */
    private final LinearColorMapElement[] standardElements;

    /**
     * @uml.property name="preFilteringElements"
     * @uml.associationEnd multiplicity="(0 -1)"
     */
    private final LinearColorMapElement[] preFilteringElements;

    /** Color for default value */
    private final Color defaultColor;

    /** Piecewise transformation */
    private DefaultPiecewiseTransform1D<LinearColorMapElement> piecewise;

    /** Prefiltering Piecewsie */
    private DefaultPiecewiseTransform1D<LinearColorMapElement> preFilteringPiecewise;

    /** Color used in prefiltering */
    private Color preFilteringColor;

    /**
     * @uml.property name="name"
     */
    private String name;

    private int hashCode = -1;

    /**
     * Constructor which creates a {@link LinearColorMap} without a {@link NoDataCategory}. Keep in mind that if the list has gaps, if you try to
     * transform a value that falls into a gap you'll get a nice {@link TransformException}!
     * 
     * @param categories the array of a categories to use for this {@link LinearColorMap}.
     */
    public LinearColorMap(final CharSequence name, final LinearColorMapElement[] standardElements) {
        this(name, standardElements, null);

    }

    public LinearColorMap(final CharSequence name, final LinearColorMapElement[] standardElements,
            final Color defColor) {
        this(name, standardElements, null, defColor);

    }

    public LinearColorMap(final CharSequence name, final LinearColorMapElement[] standardElements,
            final LinearColorMapElement[] preFilteringElements, final Color defaultColor) {

        ColorMapUtilities.ensureNonNull("name", name);
        ColorMapUtilities.ensureNonNull("standardElements", standardElements);
        this.name = name + "";
        // /////////////////////////////////////////////////////////////////////
        //
        // Prefiltering transformation
        //
        // //
        //
        // All the prefiltering transformations must share the same color, hence
        // they must point to the same int
        //
        // /////////////////////////////////////////////////////////////////////
        preliminarChecks(standardElements, preFilteringElements);

        // /////////////////////////////////////////////////////////////////////
        //
        // Checking that same int in output means same color
        //
        // //
        //
        //
        //
        // /////////////////////////////////////////////////////////////////////
        if (preFilteringElements != null && preFilteringElements.length > 0) {
            this.preFilteringElements = (LinearColorMapElement[]) preFilteringElements.clone();
            Color color = this.preFilteringElements[0].getColors()[0];
            this.preFilteringColor = color;

        } else
            this.preFilteringElements = null;

        // /////////////////////////////////////////////////////////////////////
        //
        // Standard transformation
        //
        // /////////////////////////////////////////////////////////////////////
        this.standardElements = (LinearColorMapElement[]) standardElements.clone();

        // /////////////////////////////////////////////////////////////////////
        //
        // Default color to fill gaps, if provided.
        //
        // /////////////////////////////////////////////////////////////////////
        this.defaultColor = defaultColor;

    }

    public LinearColorMap(String string, LinearColorMapElement[] linearColorMapElements,
            LinearColorMapElement[] linearColorMapElements2) {
        this(string, linearColorMapElements, linearColorMapElements2, null);
    }

    /**
     * Performing additional check on the provided domain elements in order to control that we don't have any strange overlap in the output values
     * which could leave to strange color effect.
     * 
     * @param domainElements to check.
     * @param nodata category if we have any.
     * @return the original {@link LinearColorMapElement} if nothing went wrong.
     */
    private static void preliminarChecks(final LinearColorMapElement[] domainElements,
            final LinearColorMapElement[] domainElementsToPreserve) {

        // /////////////////////////////////////////////////////////////////////
        //
        // Cycle on all the domain elements to preserve
        //
        // /////////////////////////////////////////////////////////////////////
        ColorMapUtilities.checkPreservingElements(domainElementsToPreserve);

        // Make sure there are no overlapping ranges, optimized comparison by 
        // ensuring the ranges are sorted
        Arrays.sort(domainElements, (o1, o2) -> o1.getRange().compare(o2.getRange()));
        LinearColorMapElement prev = domainElements[0];
        for (int i = 1; i < domainElements.length; i++) {
            LinearColorMapElement curr = domainElements[1];
            if (prev.getRange().intersects(curr.getRange()) && !compatible(prev, curr))
                throw new IllegalArgumentException("Wrong values defined");
            for (int j = 0; j < domainElementsToPreserve.length; j++) {
                if (curr.getRange().intersects(domainElementsToPreserve[j].getRange()))
                    throw new IllegalArgumentException("Wrong values defined");
            }
            prev = curr;
        }
    }

    private static boolean compatible(LinearColorMapElement prev, LinearColorMapElement curr) {
        Color[] colors0 = prev.getColors();
        Range outRange0 = prev.getOutputRange();
        final int minimum0 = outRange0.getMin().intValue();
        final int maximum0 = outRange0.getMax().intValue();

        final Range outRange1 = curr.getOutputRange();


        // check the values
        final int minimum1 = outRange1.getMin().intValue();
        final int maximum1 = outRange1.getMax().intValue();
        final Color[] colors1 = curr.getColors();
        if (minimum1 == maximum0 && colors0[colors0.length - 1].equals(colors1[0]))
            return true;

        if (minimum0 == maximum1 && colors1[colors1.length - 1].equals(colors0[0]))
            return true;

        return false;
    }


    /**
     * Returns a color model for this category list. This method builds up the color model from each category's colors (as returned by
     * {@link #getColors} ).
     * 
     * @param visibleBand The band to be made visible (usually 0). All other bands, if any will be ignored.
     * @param numBands The number of bands for the color model (usually 1). The returned color model will renderer only the {@code        visibleBand} and
     *        ignore the others, but the existence of all {@code        numBands} will be at least tolerated. Supplemental bands, even invisible, are useful
     *        for processing with Java Advanced Imaging.
     * @return The requested color model, suitable for {@link RenderedImage} objects with values in the <code>       {@link #getRange}        </code>
     *         range.
     * @uml.property name="colorModel"
     */
    public IndexColorModel getColorModel() {
        initColorModel();
        return colorModel;
    }

    private synchronized void initColorModel() {
        if (colorModel == null) {
            // /////////////////////////////////////////////////////////////////////
            //
            // STEP 1
            //
            // ///
            // First of all let's create the palette for the standard values.
            // Afterward we look for the right values for the default color and
            // for the preserving values color if present since these could
            // reuse the same colors used for the standard values.
            //
            // /////////////////////////////////////////////////////////////////////

            // //
            //
            // Computes the number of entries required for the color
            // palette.
            // Note that in case the output range has non inclusive edges we
            // have already taken that into account when building the outMax
            // and outMin values hence we do not have to do that again.
            //
            // //
            BigInteger bits = new BigInteger("0");
            final boolean preFilteringValuesPresent = preFilteringColor != null;
            int elementsCount = standardElements.length + (preFilteringValuesPresent ? 1 : 0);
            int max = -1;
            for (int i = 0; i < elementsCount; i++) {
                final DefaultLinearPiecewiseTransform1DElement element = i < standardElements.length ? (DefaultLinearPiecewiseTransform1DElement) standardElements[i]
                        : preFilteringElements[0];
                final int elementMin = (int) element.getOutputMinimum();
                final int elementMax = (int) element.getOutputMaximum();
                for (int k = elementMin; k <= elementMax; k++)
                    bits = bits.setBit(k);
                max = (int) Math.max(max, elementMax);
            }
            // zero based indexing
            max++;

            // /////////////////////////////////////////////////////////////////////
            //
            // Interpolate the colors in the color palette for standard values
            //
            // /////////////////////////////////////////////////////////////////////
            int[] ARGB = new int[max];
            int outMax = 0, outMin = 0;
            for (int i = 0; i < elementsCount; i++) {
                // I know that all the categories here are both
                // ColorMapTransformElement and
                // DefaultLinearPiecewiseTransform1DElement. The
                // eventual NoDtaCategory as well.
                final LinearColorMapElement element = i < standardElements.length ? standardElements[i]
                        : preFilteringElements[0];
                // //
                //
                // Check if this category overlap with another one as far as
                // the output value but with the same color. We already
                // checked that they had the same color. If this happens we
                // prevent the code from creating a new entry at the same
                // place with the same color, no rocket science going on
                // here!
                //
                // //
                outMin = (int) element.getOutputMinimum();
                outMax = (int) (element.getOutputMaximum());
                // NOTE the second element is exclusive!!!
                ColorMapUtilities.expand(element.getColors(), ARGB, outMin, outMax + 1);
            }

            // create the prefiltering piecewise
            this.preFilteringPiecewise = preFilteringElements == null ? null
                    : new DefaultPiecewiseTransform1D<LinearColorMapElement>(preFilteringElements);

            // /////////////////////////////////////////////////////////////////////
            //
            // STEP 2
            //
            // ///
            // Now, if we do not have any default color we can proceed,
            // otherwise we got some work to do. First of all, we need to check
            // if the colors we are asked to use for the special cases are
            // already in use. In such a case we don't reserve another index in
            // the palette for them. Otherwise we have to add the new colors and
            // recreate the ASRGB array.
            //
            // /////////////////////////////////////////////////////////////////////
            final boolean lookForDefaultColor = defaultColor != null;
            boolean defaultColorFound = !lookForDefaultColor;
            if (lookForDefaultColor) {

                // //
                //
                // Search in the color map if we have the requested colors
                //
                // //
                int defaultColorIndex = -1;
                for (int i = 0; i < ARGB.length; i++) {
                    if (lookForDefaultColor && defaultColorIndex == -1 && bits.testBit(i)
                            && ARGB[i] == defaultColor.getRGB()) {
                        defaultColorIndex = i;
                        defaultColorFound = true;
                        break;
                    }

                }
                // now see what happened
                if (defaultColorFound)
                    this.piecewise = new DefaultPiecewiseTransform1D<LinearColorMapElement>(
                            this.standardElements, defaultColorIndex);
                else {
                    // check the bit vector for the first place available
                    int i = 0;
                    for (; i < max; i++)
                        if (!bits.testBit(i))
                            break;
                    if (i == max) {
                        max = i == max ? max + 1 : max;
                        bits = bits.setBit(i);
                        final int[] tempARGB = new int[max];
                        System.arraycopy(ARGB, 0, tempARGB, 0, ARGB.length);
                        tempARGB[tempARGB.length - 1] = defaultColor.getRGB();
                        ARGB = tempARGB;
                    }
                    // SG we use max-1 as a default value since the colormap
                    // goes from 0 to max - 1
                    this.piecewise = new DefaultPiecewiseTransform1D<LinearColorMapElement>(
                            this.standardElements, max - 1);
                }

            } else {
                this.piecewise = new DefaultPiecewiseTransform1D<LinearColorMapElement>(
                        this.standardElements);
            }

            colorModel = new IndexColorModel(ColorMapUtilities.getBitCount(max), max, ARGB, 0,
                    ColorMapUtilities.getTransferType(max), bits);
        }

    }

    /**
     * Creates a {@link SampleModel} compatible with the underlying {@link ColorModel} having the specified width and height.
     * 
     * @param width represents the width for the final {@link SampleModel}
     * @param height represents the height for the final {@link SampleModel}
     */
    public SampleModel getSampleModel(int width, int height) {
        if (width <= 0)
            throw new IllegalArgumentException("Width cannot be < 0");
        if (height <= 0)
            throw new IllegalArgumentException("Height cannot be < 0");
        // //
        //
        // force color model computation
        //
        // //
        initColorModel();

        // //
        //
        // create a suitable color model
        // NOTE:
        // IndexColorModel seems to badly choose its sample model. As of JDK
        // 1.4-rc1, it construct a ComponentSampleModel, which is drawn very
        // slowly to the screen. A much faster sample model is
        // PixelInterleavedSampleModel, which is the sample model used by
        // BufferedImage for TYPE_BYTE_INDEXED. We should check if this is fixed
        // in future J2SE release.
        //
        // //
        return new PixelInterleavedSampleModel(this.colorModel.getTransferType(), width, height, 1,
                width, new int[1]);
    }

    public LinearColorMapElement get(int index) {
        if (index < standardElements.length)
            return standardElements[index];
        if (preFilteringElements != null)
            return preFilteringElements[index - standardElements.length];
        throw new ArrayIndexOutOfBoundsException("Colormap Index is outside the colormap bounds");
    }

    public int size() {
        int size = this.preFilteringElements != null ? preFilteringElements.length : 0;
        size += this.standardElements.length;
        return size;
    }

    public double getDefaultValue() {
        initColorModel();
        return this.piecewise.getDefaultValue();
    }

    public boolean hasDefaultValue() {
        initColorModel();
        return this.piecewise.hasDefaultValue();
    }

    public Range getApproximateDomainRange() {
        initColorModel();
        return this.piecewise.getApproximateDomainRange();
    }

    public LinearColorMapElement findDomainElement(double sample) {
        initColorModel();
        final boolean prefiltering = this.preFilteringElements != null;
        LinearColorMapElement retValue = null;
        if (prefiltering)
            retValue = preFilteringPiecewise.findDomainElement(sample);
        if (retValue == null)
            retValue = piecewise.findDomainElement(sample);
        return retValue;
    }

    public LinearColorMapElement[] getDomainElements() {
        return (LinearColorMapElement[]) this.standardElements.clone();
    }

    /**
     * @return
     * @uml.property name="name"
     */
    public String getName() {
        initColorModel();
        return this.name;
    }

    public boolean hasGaps() {
        initColorModel();
        return this.piecewise.hasGaps();
    }

    public double transform(double value) throws TransformationException {
        initColorModel();
        PiecewiseTransform1DElement transform = (PiecewiseTransform1DElement) findDomainElement(value);
        if (transform != null)
            return transform.transform(value);
        return this.preFilteringPiecewise.transform(value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.opengis.referencing.operation.MathTransform#getSourceDimensions()
     */
    public int getSourceDimensions() {
        return 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.opengis.referencing.operation.MathTransform#getTargetDimensions()
     */
    public int getTargetDimensions() {
        return 1;
    }

    public Position transform(Position ptSrc, Position ptDst) throws TransformationException {
        // /////////////////////////////////////////////////////////////////////
        //
        // input checks
        //
        // /////////////////////////////////////////////////////////////////////
        ColorMapUtilities.ensureNonNull("ptSrc", ptSrc);
        if (ptDst == null) {
            ptDst = new Position();
        }
        ptDst.setOrdinatePosition(transform(ptSrc.getOrdinatePosition()));
        return ptDst;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof LinearColorMap))
            return false;
        final LinearColorMap that = (LinearColorMap) o;
        if (!PiecewiseUtilities.equals(name, that.name))
            return false;
        if (!PiecewiseUtilities.equals(defaultColor, that.defaultColor))
            return false;
        if (preFilteringColor != that.preFilteringColor)
            return false;
        if (!PiecewiseUtilities.equals(preFilteringElements, that.preFilteringElements))
            return false;
        if (!PiecewiseUtilities.equals(standardElements, that.standardElements))
            return false;
        return piecewise.equals(that.piecewise);

    }

    @Override
    public int hashCode() {
        if (hashCode >= 0)
            return hashCode;
        hashCode = 37;
        hashCode = PiecewiseUtilities.hash(name, hashCode);
        hashCode = PiecewiseUtilities.hash(defaultColor, hashCode);
        hashCode = PiecewiseUtilities.hash(preFilteringColor, hashCode);
        hashCode = PiecewiseUtilities.hash(preFilteringElements, hashCode);
        hashCode = PiecewiseUtilities.hash(standardElements, hashCode);
        hashCode = PiecewiseUtilities.hash(piecewise, hashCode);
        return hashCode;
    }
}
