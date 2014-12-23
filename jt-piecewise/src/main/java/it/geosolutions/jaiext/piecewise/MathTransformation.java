package it.geosolutions.jaiext.piecewise;

public interface MathTransformation {

    double transform(double value);

    double derivative(double value);

    int getSourceDimensions();

    int getTargetDimensions();

    MathTransformation inverse();

    boolean isIdentity();
    
    Position transform(Position ptSrc, Position ptDst);
}
