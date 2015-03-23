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
 * {@link MathTransformation} implementation for single-dimensional operations
 * 
 * @author Nicola Lagomarsini geosolutions
 * 
 */
public class SingleDimensionTransformation implements MathTransformation {

    public static final SingleDimensionTransformation IDENTITY = new SingleDimensionTransformation(
            1, 0);

    protected SingleDimensionTransformation(double scale, double offset) {
        this.scale = scale;
        this.offset = offset;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public double getOffset() {
        return offset;
    }

    public void setOffset(double offset) {
        this.offset = offset;
    }

    /** Scale transformation parameter*/
    protected double scale;

    /** Offset transformation parameter*/
    protected double offset;

    private MathTransformation inverse;

    public double transform(double value) {
        return offset + scale * value;
    }

    public double derivative(double value) {
        return scale;
    }

    public int getSourceDimensions() {
        return 1;
    }

    public int getTargetDimensions() {
        return 1;
    }

    public MathTransformation inverseTransform() {
        if (inverse == null) {
            if (isIdentity()) {
                inverse = this;
            } else if (scale != 0) {
                final SingleDimensionTransformation inverse;
                inverse = create(1 / scale, -offset / scale);
                inverse.inverse = this;
                this.inverse = inverse;
            } else {
                throw new UnsupportedOperationException("Unable to invert such transformation");
            }
        }
        return inverse;
    }

    public boolean isIdentity() {
        return isIdentity(0);
    }

    /**
     * Returns true if the transformation is an identity, with a tolerance value
     */
    public boolean isIdentity(double tolerance) {
        tolerance = Math.abs(tolerance);
        return Math.abs(offset) <= tolerance && Math.abs(scale - 1) <= tolerance;
    }

    /**
     * Creates a {@link SingleDimensionTransformation} instance based on the input scale and offset
     * 
     * @param scale
     * @param offset
     * @return
     */
    public static SingleDimensionTransformation create(double scale, double offset) {
        if (scale == 0) {
            return new ConstantTransform(offset);
        }
        if (scale == 1 && offset == 0) {
            return IDENTITY;
        }
        return new SingleDimensionTransformation(scale, offset);
    }

    public Position transform(Position ptSrc, Position ptDst) {
        if (ptDst == null) {
            ptDst = new Position();
        }
        ptDst.setOrdinatePosition(transform(ptSrc.getOrdinatePosition()));
        return ptDst;
    }

    /**
     * {@link SingleDimensionTransformation} extension defining Constant transformations
     */
    public static class ConstantTransform extends SingleDimensionTransformation {

        protected ConstantTransform(double offset) {
            super(0, offset);
        }

        public double transform(double value) {
            return offset;
        }
    }
}
