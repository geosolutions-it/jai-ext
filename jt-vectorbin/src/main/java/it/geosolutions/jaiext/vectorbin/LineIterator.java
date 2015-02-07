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
import com.vividsolutions.jts.geom.LinearRing;


/**
 * A path iterator for the LiteShape class, specialized to iterate over
 * LineString object. This class was ported back and simplified from GeoTools, with permission from the author(s)
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

	private int coordinateCount;

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
     * @return <code>true</code> if all the segments have been read;
     *         <code>false</code> otherwise.
     */
    public boolean isDone() {
        return done;
    }

    /**
     * Moves the iterator to the next segment of the path forwards along the primary direction of
     * traversal as long as there are more points in that direction.
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
