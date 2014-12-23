package it.geosolutions.jaiext.piecewise;

public class Position {

    double ordinate;
    
    public Position() {}
    
    public Position(double ordinate){
        this.ordinate = ordinate;
    }

    public double getOrdinate() {
        return ordinate;
    }

    public void setOrdinate(double ordinate) {
        this.ordinate = ordinate;
    }

    public int getDimension() {
        return 1;
    }
}
