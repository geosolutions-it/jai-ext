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
package it.geosolutions.jaiext.utilities.shape;

import java.awt.geom.AffineTransform;

import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;

/**
 * A path iterator for the LiteShape class, specialized to iterate over Polygon objects. This class was ported back and simplified from GeoTools, with
 * permission from the author(s)
 * 
 * @author Andrea Aime
 * @author simone giannecchini
 */
public final class PolygonIterator extends AbstractLiteIterator {
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
     * Returns the coordinates and type of the current path segment in the iteration. The return value is the path-segment type: SEG_MOVETO,
     * SEG_LINETO, SEG_QUADTO, SEG_CUBICTO, or SEG_CLOSE. A double array of length 6 must be passed in and can be used to store the coordinates of the
     * point(s). Each point is stored as a pair of double x,y coordinates. SEG_MOVETO and SEG_LINETO types returns one point, SEG_QUADTO returns two
     * points, SEG_CUBICTO returns 3 points and SEG_CLOSE does not return any points.
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

    protected void transform(double[] src, int index, double[] dest, int destIndex, int numPoints) {
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
     * @return <code>true</code> if all the segments have been read; <code>false</code> otherwise.
     */
    public boolean isDone() {
        return done;
    }

    /**
     * Moves the iterator to the next segment of the path forwards along the primary direction of traversal as long as there are more points in that
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
