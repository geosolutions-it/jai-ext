   
package it.geosolutions.jaiext.vectorbin;

import java.awt.geom.PathIterator;

/**
 * Subclass that provides a convenient efficient currentSegment(float[] coords) implementation that
 * reuses always the same double array. This class and the associated subclasses are not thread
 * safe.
 * This class was ported back and simplified from GeoTools, with permission from the author(s)
 * 
 * @author Andrea Aime
 */
public abstract class AbstractLiteIterator implements PathIterator {

    protected double[] dcoords = new double[2];

    /**
     * @see java.awt.geom.PathIterator#currentSegment(float[])
     */
    public int currentSegment(float[] coords) {
        int result = currentSegment(dcoords);
        coords[0] = (float) dcoords[0];
        coords[1] = (float) dcoords[1];

        return result;
    }

}
