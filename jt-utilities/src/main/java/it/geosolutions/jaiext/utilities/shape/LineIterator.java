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
import org.locationtech.jts.geom.LinearRing;

/**
 * A path iterator for the LiteShape class, specialized to iterate over LineString object. This class was ported back and simplified from GeoTools,
 * with permission from the author(s)
 * 
 * @author Andrea Aime
 * @author simone giannecchini
 */
public final class LineIterator extends AbstractLiteIterator {
    /** Transform applied on the coordinates during iteration */
    private AffineTransform at;

    /** The array of coordinates that represents the line geometry */
    private CoordinateSequence coordinates = null;

    /** Current line coordinate */
    private int currentCoord = 0;

    /** True when the iteration is terminated */
    private boolean done = false;

    /** True if the line is a ring */
    private boolean isClosed;

    /** Number of coordinates */
    private int coordinateCount;

    /** Identity transform */
    private static final AffineTransform NO_TRANSFORM = new AffineTransform();

    public LineIterator() {

    }

    /**
     * Creates a new instance of LineIterator
     * 
     * @param ls The line string the iterator will use
     * @param at The affine transform applied to coordinates during iteration
     */
    public LineIterator(LineString ls, AffineTransform at) {
        init(ls, at);
    }

    /**
     * @param ls
     * @param at
     */
    public void init(LineString ls, AffineTransform at) {
        if (at == null) {
            at = NO_TRANSFORM;
        }

        this.at = at;
        coordinates = ls.getCoordinateSequence();
        coordinateCount = coordinates.size();
        isClosed = ls instanceof LinearRing;

        done = false;
        currentCoord = 0;

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
        if (currentCoord == 0) {
            coords[0] = (double) coordinates.getX(0);
            coords[1] = (double) coordinates.getY(0);
            at.transform(coords, 0, coords, 0, 1);
            return SEG_MOVETO;
        } else if ((currentCoord == coordinateCount) && isClosed) {
            return SEG_CLOSE;
        } else {
            coords[0] = coordinates.getX(currentCoord);
            coords[1] = coordinates.getY(currentCoord);
            at.transform(coords, 0, coords, 0, 1);

            return SEG_LINETO;
        }
    }

}
