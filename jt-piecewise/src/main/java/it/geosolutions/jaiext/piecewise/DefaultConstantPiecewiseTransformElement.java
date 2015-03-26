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

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;

import java.io.Serializable;

/**
 * Convenience implementation of a {@link PiecewiseTransform1DElement} that can be used to map single values or an interval to a single output value.
 * 
 * @author Simone Giannecchini, GeoSolutions
 * 
 */
public class DefaultConstantPiecewiseTransformElement extends
        DefaultLinearPiecewiseTransform1DElement implements PiecewiseTransform1DElement,
        Comparable<DomainElement1D>, Serializable {

    /**
     * UID
     */
    private static final long serialVersionUID = 6704840161747974131L;

    /**
     * Constructor.
     * 
     * @param name for this {@link DomainElement1D}
     * @param inRange for this {@link DomainElement1D}
     * @param outVal for this {@link DefaultLinearPiecewiseTransform1DElement}
     * @throws IllegalArgumentException in case the input values are illegal.
     */
    public DefaultConstantPiecewiseTransformElement(CharSequence name, final Range inRange,
            final double outVal) throws IllegalArgumentException {
        super(name, inRange, RangeFactory.create(outVal, true, outVal, true, true));

    }

    /**
     * Constructor.
     * 
     * @param name for this {@link DomainElement1D}
     * @param inRange for this {@link DomainElement1D}
     * @param outVal for this {@link DefaultLinearPiecewiseTransform1DElement}
     * @throws IllegalArgumentException in case the input values are illegal.
     */
    public DefaultConstantPiecewiseTransformElement(CharSequence name, final Range inRange,
            final int outVal) throws IllegalArgumentException {
        super(name, inRange, RangeFactory.create(outVal, true, outVal, true));
    }

    /**
     * Constructor.
     * 
     * @param name for this {@link DomainElement1D}
     * @param inRange for this {@link DomainElement1D}
     * @param outVal for this {@link DefaultLinearPiecewiseTransform1DElement}
     * @throws IllegalArgumentException in case the input values are illegal.
     */
    public DefaultConstantPiecewiseTransformElement(CharSequence name, final Range inRange,
            final byte outVal) throws IllegalArgumentException {
        super(name, inRange, RangeFactory.create(outVal, true, outVal, true));
    }

    /**
     * The transformation we are specifying here is not always invertible, well, to be honest, strictly speaking it never really is. However when the
     * underlying transformation is a 1:1 mapping we can invert it.
     */
    public MathTransformation inverse() throws NoninvertibleTransformException {
        if (this.getInputMinimum() == getInputMaximum())
            return SingleDimensionTransformation.create(0, getInputMinimum());
        throw new UnsupportedOperationException(
                "Inverse operation is unsupported for Constant Transform");

    }

}
