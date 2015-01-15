package it.geosolutions.jaiext.piecewise;

public abstract class SingleDimensionTransformation implements MathTransformation {

    public SingleDimensionTransformation(double scale, double offset) {
        this.scale = scale;
        this.offset = offset;
    }

    public static final MathTransformation IDENTITY = null;

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

    public abstract double transform(double value);

    public abstract double derivative(double value);

    public abstract int getSourceDimensions();

    public abstract int getTargetDimensions();

    public abstract MathTransformation inverse();

    public abstract boolean isIdentity();

    public static SingleDimensionTransformation create(double i, double minDestination) {
        return null;
    }
}
