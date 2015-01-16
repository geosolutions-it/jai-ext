package it.geosolutions.jaiext.piecewise;

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

    protected double scale;

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

    public MathTransformation inverse() {
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

    public boolean isIdentity(double tolerance) {
        tolerance = Math.abs(tolerance);
        return Math.abs(offset) <= tolerance && Math.abs(scale - 1) <= tolerance;
    }

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
        ptDst.setOrdinate(transform(ptSrc.getOrdinate()));
        return ptDst;
    }

    public static class ConstantTransform extends SingleDimensionTransformation {

        protected ConstantTransform(double offset) {
            super(0, offset);
        }

        public double transform(double value) {
            return offset;
        }
    }
}
