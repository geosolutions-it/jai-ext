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

import org.locationtech.jts.geom.Point;

/**
 * A path iterator for the LiteShape class, specialized to iterate over Point objects. This class was ported back and simplified from GeoTools, with
 * permission from the author(s)
 * 
 * @author Andrea Aime
 */
public final class PointIterator extends AbstractLiteIterator {
    /** Transform applied on the coordinates during iteration */
    private AffineTransform at;

    /** The point we are going to provide when asked for coordinates */
    private Point point;

    /** True when the point has been read once */
    private boolean done;

    /**
     * Creates a new PointIterator object.
     * 
     * @param point The point
     * @param at The affine transform applied to coordinates during iteration
     */
    public PointIterator(Point point, AffineTransform at) {
        if (at == null) {
            at = new AffineTransform();
        }

        this.at = at;
        this.point = point;
        done = false;
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
     * @see java.awt.geom.PathIterator#next()
     */
    public void next() {
        done = true;
    }

    /**
     * @see java.awt.geom.PathIterator#isDone()
     */
    public boolean isDone() {
        return done;
    }

    /**
     * @see java.awt.geom.PathIterator#currentSegment(double[])
     */
    public int currentSegment(double[] coords) {
        coords[0] = point.getX();
        coords[1] = point.getY();
        at.transform(coords, 0, coords, 0, 1);

        return SEG_MOVETO;
    }

}
