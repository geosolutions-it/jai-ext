   
package it.geosolutions.jaiext.vectorbin;



/**
 * An iterator for empty geometries. This class was ported back and simplified from GeoTools, with permission from the author(s)
 * 
 */
public class EmptyIterator extends AbstractLiteIterator {
    
    public static final EmptyIterator INSTANCE = new EmptyIterator();
    
    public int getWindingRule() {
        return WIND_NON_ZERO;
    }

    public boolean isDone() {
        return true;
    }

    public void next() {
        throw new IllegalStateException();
    }

    public int currentSegment(double[] coords) {
        return 0;
    }
    
    public int currentSegment(float[] coords) {
        return 0;
    }
}
