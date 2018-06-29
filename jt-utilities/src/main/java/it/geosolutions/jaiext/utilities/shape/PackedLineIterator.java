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

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence.Double;

/**
 * A path iterator for the LiteShape class, specialized to iterate over LineString object. This class was ported back and simplified from GeoTools,
 * with permission from the author(s)
 * 
 * @author Andrea Aime
 * @author simone giannecchini
 */
public final class PackedLineIterator extends AbstractLiteIterator {
    /** Transform applied on the coordinates during iteration */
    private AffineTransform at;

    /** The array of coordinates that represents the line geometry */
    private PackedCoordinateSequence.Double coordinates = null;

    /** Current line coordinate */
    private int currentCoord = 0;

    /** True when the iteration is terminated */
    private boolean done = false;

    /** True if the line is a ring */
    private boolean isClosed;

    private int coordinateCount;

    /**
     * Creates a new instance of LineIterator
     * 
     * @param ls The line string the iterator will use
     * @param at The affine transform applied to coordinates during iteration
     */
    public PackedLineIterator(LineString ls, AffineTransform at) {
        if (at == null) {
            at = new AffineTransform();
        }

        this.at = at;
        coordinates = (Double) ls.getCoordinateSequence();
        coordinateCount = coordinates.size();
        isClosed = ls instanceof LinearRing;
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
    public int currentSegment(float[] coords) {
        if (currentCoord == 0) {
            coords[0] = (float) coordinates.getX(0);
            coords[1] = (float) coordinates.getY(0);
            at.transform(coords, 0, coords, 0, 1);

            return SEG_MOVETO;
        } else if ((currentCoord == coordinateCount) && isClosed) {
            return SEG_CLOSE;
        } else {
            coords[0] = (float) coordinates.getX(currentCoord);
            coords[1] = (float) coordinates.getY(currentCoord);
            at.transform(coords, 0, coords, 0, 1);

            return SEG_LINETO;
        }
    }

    /**
     * Returns the winding rule for determining the interior of the path.
     * 
     * @return the winding rule.
     * 
     * @see #WIND_EVEN_ODD
     * @see #WIND_NON_ZERO
     */
    public int getWindingRule() {
        return WIND_NON_ZERO;
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
        if (((currentCoord == (coordinateCount - 1)) && !isClosed)
                || ((currentCoord == coordinateCount) && isClosed)) {
            done = true;
        } else {
            currentCoord++;
        }
    }

    /**
     * @see java.awt.geom.PathIterator#currentSegment(double[])
     */
    public int currentSegment(double[] coords) {
        System.out.println("Double!");
        return 0;
    }

}
