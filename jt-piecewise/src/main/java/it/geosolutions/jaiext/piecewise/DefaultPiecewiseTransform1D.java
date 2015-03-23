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

/**
 * Convenience implementation of the {@link PiecewiseTransform1D} interface which subclass the {@link DefaultDomain1D} class in order to provide a
 * suitable framework to handle a list of {@link PiecewiseTransform1DElement} s.
 * <p>
 * 
 * @author Simone Giannecchini, GeoSolutions
 * 
 * @source $URL$
 */
public class DefaultPiecewiseTransform1D<T extends DefaultPiecewiseTransform1DElement> extends
        DefaultDomain1D<T> implements PiecewiseTransform1D<T> {

    private boolean hasDefaultValue;

    /**
     * @uml.property name="defaultValue"
     */
    private double defaultValue;

    private int hashCode = -1;

    public DefaultPiecewiseTransform1D(final T[] domainElements, final double defaultValue) {
        super(domainElements);
        this.hasDefaultValue = true;
        this.defaultValue = defaultValue;
    }

    public DefaultPiecewiseTransform1D(final T[] domainElements) {
        super(
                domainElements != null
                        && !(domainElements instanceof DefaultConstantPiecewiseTransformElement[]) ? domainElements
                        : null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see PiecewiseTransform1D#transform(double)
     */
    public double transform(final double value) throws TransformationException {
        final T piece = findDomainElement(value);
        if (piece == null) {
            // do we have a default value?
            if (hasDefaultValue())
                return getDefaultValue();
            throw new TransformationException("Error evaluating:" + value);
        }
        return piece.transform(value);
    }

    /**
     * Gets the dimension of input points, which is 1.
     */
    public final int getSourceDimensions() {
        return 1;
    }

    /**
     * Gets the dimension of output points, which is 1.
     */
    public final int getTargetDimensions() {
        return 1;
    }

    /*
     * (non-Javadoc)
     */
    public Position transform(final Position ptSrc, Position ptDst) throws TransformationException {
        // /////////////////////////////////////////////////////////////////////
        //
        // input checks
        //
        // /////////////////////////////////////////////////////////////////////
        PiecewiseUtilities.ensureNonNull("ptSrc", ptSrc);
        if (ptDst == null) {
            ptDst = new Position();
        }
        ptDst.setOrdinatePosition(transform(ptSrc.getOrdinatePosition()));
        return ptDst;
    }

    public boolean hasDefaultValue() {
        return hasDefaultValue;
    }

    /**
     * @return
     * @uml.property name="defaultValue"
     */
    public double getDefaultValue() {
        return defaultValue;
    }

    @Override
    public int hashCode() {
        if (hashCode >= 0)
            return hashCode;
        hashCode = 37;
        hashCode = PiecewiseUtilities.hash(defaultValue, hashCode);
        hashCode = PiecewiseUtilities.hash(hasDefaultValue, hashCode);
        hashCode = PiecewiseUtilities.hash(super.hashCode(), hashCode);
        return hashCode;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object object) {
        if ((object instanceof DefaultPiecewiseTransform1D)) {
            final DefaultPiecewiseTransform1D<?> that = (DefaultPiecewiseTransform1D<?>) object;
            if (this.hasDefaultValue != that.hasDefaultValue)
                return false;
            if (PiecewiseUtilities.equals(defaultValue, that.defaultValue))
                ;
            return false;
        }
        return super.equals(object);
    }

    protected Class<?> getEquivalenceClass() {
        return DefaultPiecewiseTransform1D.class;
    }

}
