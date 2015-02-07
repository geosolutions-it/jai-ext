/* 
 *  Copyright (c) 2011, Michael Bedward. All rights reserved. 
 *   
 *  Redistribution and use in source and binary forms, with or without modification, 
 *  are permitted provided that the following conditions are met: 
 *   
 *  - Redistributions of source code must retain the above copyright notice, this  
 *    list of conditions and the following disclaimer. 
 *   
 *  - Redistributions in binary form must reproduce the above copyright notice, this 
 *    list of conditions and the following disclaimer in the documentation and/or 
 *    other materials provided with the distribution.   
 *   
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR 
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */   
package it.geosolutions.jaiext.vectorbin;


import java.awt.geom.AffineTransform;

import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;


/**
 * A path iterator for the LiteShape class, specialized to iterate over Polygon
 * objects. This class was ported back and simplified from GeoTools, with permission from the author(s)
 *
 * @author Andrea Aime
 * @author simone giannecchini
 */
public final  class PolygonIterator extends AbstractLiteIterator {
    /** Transform applied on the coordinates during iteration */
    private AffineTransform at;

    /** The rings describing the polygon geometry */
    private LineString[] rings;

    /** The current ring during iteration */
    private int currentRing = 0;

    /** Current line coordinate */
    private int currentCoord = 0;

    /** The array of coordinates that represents the line geometry */
    private CoordinateSequence coords = null;

    /** True when the iteration is terminated */
    private boolean done = false;

    /**
     * Creates a new PolygonIterator object.
     *
     * @param p The polygon
     * @param at The affine transform applied to coordinates during iteration
     */
    public PolygonIterator(Polygon p, AffineTransform at) {
        int numInteriorRings = p.getNumInteriorRing();
        rings = new LineString[numInteriorRings + 1];
        rings[0] = p.getExteriorRing();

        for (int i = 0; i < numInteriorRings; i++) {
            rings[i + 1] = p.getInteriorRingN(i);
        }

        if (at == null) {
            at = new AffineTransform();
        }

        this.at = at;
        coords = rings[0].getCoordinateSequence();
    }

    /**
     * Returns the coordinates and type of the current path segment in the
     * iteration. The return value is the path-segment type: SEG_MOVETO,
     * SEG_LINETO, SEG_QUADTO, SEG_CUBICTO, or SEG_CLOSE. A double array of
     * length 6 must be passed in and can be used to store the coordinates of
     * the point(s). Each point is stored as a pair of double x,y coordinates.
     * SEG_MOVETO and SEG_LINETO types returns one point, SEG_QUADTO returns
     * two points, SEG_CUBICTO returns 3 points and SEG_CLOSE does not return
     * any points.
     *
     * @param coords an array that holds the data returned from this method
     *
     * @return the path-segment type of the current path segment.
     *
     * @see #SEG_MOVETO
     * @see #SEG_LINETO
     * @see #SEG_QUADTO
     * @see #SEG_CUBICTO
     * @see #SEG_CLOSE
     */
    public int currentSegment(double[] coords) {
        // first make sure we're not at the last element, this prevents us from exceptions
        // in the case where coords.size() == 0
        if (currentCoord == this.coords.size()) {
            return SEG_CLOSE;
        } else if (currentCoord == 0) {
            coords[0] = this.coords.getX(0);
            coords[1] = this.coords.getY(0);
            transform(coords, 0, coords, 0, 1);

            return SEG_MOVETO;
        } else {
            coords[0] = this.coords.getX(currentCoord);
            coords[1] = this.coords.getY(currentCoord);
            transform(coords, 0, coords, 0, 1);

            return SEG_LINETO;
        }
    }
    
    protected void transform(double[] src, int index, double[] dest, int destIndex, int numPoints){
            at.transform(src, index, dest, destIndex, numPoints);
    }

    /**
     * Return the winding rule for determining the interior of the path.
     *
     * @return <code>WIND_EVEN_ODD</code> by default.
     */
    public int getWindingRule() {
        return WIND_EVEN_ODD;
    }

    /**
     * Tests if the iteration is complete.
     *
     * @return <code>true</code> if all the segments have been read;
     *         <code>false</code> otherwise.
     */
    public boolean isDone() {
        return done;
    }

    /**
     * Moves the iterator to the next segment of the path forwards along the
     * primary direction of traversal as long as there are more points in that
     * direction.
     */
    public void next() {
        if (currentCoord == coords.size()) {
            if (currentRing < (rings.length - 1)) {
                currentCoord = 0;
                currentRing++;
                coords = rings[currentRing].getCoordinateSequence();
            } else {
                done = true;
            }
        } else {
            currentCoord++;
        }
    }
    
}
