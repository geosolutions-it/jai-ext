/* 
 *  Copyright (c) 2010, Michael Bedward. All rights reserved. 
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

package it.geosolutions.jaiext.jts;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Envelope;

/**
 * A lightweight implementation of JTS {@code CoordinateSequence} for 2D points.
 * 
 * @author Michael Bedward
 * @since 1.1
 * @version $Id$
 */
public final class CoordinateSequence2D implements CoordinateSequence, Cloneable {

    private final double[] x;
    private final double[] y;

    /**
     * Creates a new {@code CoordinateSequence2D} object with the given 
     * size.
     * 
     * @param n capacity (number of coordinates)
     */
    public CoordinateSequence2D(int n) {
        x = new double[n];
        y = new double[n];
    }

    /**
     * Creates a new {@code CoordinateSequence2D} object from a sequence of
     * (x,y) pairs.
     * <pre><code>
     * // Example: create an object with 3 coordinates specified
     * // as xy pairs
     * CoordinateSequence cs = new CoordinateSequence2D(1.1, 1.2, 2.1, 2.2, 3.1, 3.2);
     * </code></pre>
     * @param xy x and y values ordered as {@code x0, y0, x1, y1...}; 
     *        if {@code null} an empty object is created
     * 
     * @throws IllegalArgumentException if the number of values in {@code xy} is
     *         greater than 0 but not even
     */
    public CoordinateSequence2D(double... xy) {
        if (xy == null) {
            x = new double[0];
            y = new double[0];
        } else {
            if (xy.length % 2 != 0) {
                throw new IllegalArgumentException("xy must have an even number of values");
            }

            x = new double[xy.length / 2];
            y = new double[xy.length / 2];
            
            for (int i = 0, k = 0; k < xy.length; i++, k += 2) {
                x[i] = xy[k];
                y[i] = xy[k + 1];
            }
        }
    }

    /**
     * Gets the dimension of points stored by this {@code CoordinateSequence2D}.
     * 
     * @return always returns 2
     */
    public int getDimension() {
        return 2;
    }

    /**
     * Gets coordinate values at the specified index.
     * 
     * @param index an index &ge;0 and &lt; {@link #size()}
     * 
     * @return a new {@code Coordinate} object
     */
    public Coordinate getCoordinate(int index) {
        return new Coordinate(x[index], y[index]);
    }

    /**
     * Equivalent to {@link #getCoordinate(int)}.
     * 
     * @param index an index &ge;0 and &lt; {@link #size()}
     * 
     * @return a new {@code Coordinate} object
     */
    public Coordinate getCoordinateCopy(int index) {
        return getCoordinate(index);
    }

    /**
     * Copies the requested coordinate in the sequence to the supplied
     * {@code Coordinate} object. 
     * 
     * @param index an index &ge;0 and &lt; {@link #size()}
     * 
     * @param coord the destination object; ff {@code null} a new
     *        {@code Coordinate} will e created.
     */
    public void getCoordinate(int index, Coordinate coord) {
        if (coord == null) {
            coord = new Coordinate();
        }
        
        coord.x = x[index];
        coord.y = y[index];
    }

    /**
     * {@inheritDoc}
     * 
     * @param index an index &ge;0 and &lt; {@link #size()}
     */
    public double getX(int index) {
        return x[index];
    }

    /**
     * {@inheritDoc}
     * 
     * @param index an index &ge;0 and &lt; {@link #size()}
     */
    public double getY(int index) {
        return y[index];
    }

    /**
     * Returns the ordinate of a coordinate in this sequence.
     * 
     * @param index an index &ge;0 and &lt; {@link #size()}
     * 
     * @param ordinateIndex 0 for the X ordinate or 1 for the Y ordinate
     * 
     * @return the ordinate value
     * 
     * @throws IllegalArgumentException if {@code ordinateIndex} is not
     *         either 0 or 1
     */
    public double getOrdinate(int index, int ordinateIndex) {
        switch (ordinateIndex) {
            case 0:
                return x[index];

            case 1:
                return y[index];

            default:
                throw new IllegalArgumentException("invalid ordinate index: " + ordinateIndex);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return x.length;
    }

    /**
     * Sets the ordinate of a coordinate in this sequence.
     * 
     * @param index an index &ge;0 and &lt; {@link #size()}
     * 
     * @param ordinateIndex 0 for the X ordinate or 1 for the Y ordinate
     * 
     * @param value the new ordinate value
     * 
     * @throws IllegalArgumentException if {@code ordinateIndex} is not
     *         either 0 or 1
     */
    public void setOrdinate(int index, int ordinateIndex, double value) {
        switch (ordinateIndex) {
            case 0:
                x[index] = value;
                break;

            case 1:
                y[index] = value;
                break;

            default:
                throw new IllegalArgumentException("invalid ordinate index: " + ordinateIndex);
        }
    }

    /**
     * Sets the X ordinate of the point at the given index.
     * Equivalent to {@link #setOrdinate}(index, 0, value).
     * 
     * @param index an index &ge;0 and &lt; {@link #size()}
     * 
     * @param value the new value
     */
    public void setX(int index, double value) {
        setOrdinate(index, 0, value);
    }

    /**
     * Sets the Y ordinate of the point at the given index.
     * Equivalent to {@link #setOrdinate}(index, 1, value).
     * 
     * @param index an index &ge;0 and &lt; {@link #size()}
     * 
     * @param value the new value
     */
    public void setY(int index, double value) {
        setOrdinate(index, 1, value);
    }
    
    /**
     * Sets the coordinate at the given index.
     * 
     * @param index an index &ge;0 and &lt; {@link #size()}
     * 
     * @param x the new X ordinate value
     * @param y the new Y ordinate value
     */
    public void setXY(int index, double x, double y) {
        setOrdinate(index, 0, x);
        setOrdinate(index, 1, y);
    }
    /**
     * Returns an array of new {@code Coordinate} objects for the point values
     * in this sequence. 
     * 
     * @return array of coordinates
     */
    public Coordinate[] toCoordinateArray() {
        Coordinate[] coords = new Coordinate[x.length];

        for (int i = 0; i < x.length; i++) {
            coords[i] = new Coordinate(x[i], y[i]);
        }

        return coords;
    }

    /**
     * Returns an envelope which contains {@code env} and all points
     * in this sequence. If {@code env} contains all points it is 
     * returned unchanged.
     * 
     * @param env the test envelope; if {@code null} a new {@code Envelope} 
     *        is created
     * 
     * @return an envelope that includes {@code env} plus all points
     *         in this sequence
     */
    public Envelope expandEnvelope(Envelope env) {
        if (env == null) env = new Envelope();
        
        for (int i = 0; i < x.length; i++) {
            env.expandToInclude(x[i], y[i]);
        }
        
        return env;
    }

    /**
     * Creates a deep copy of this sequence.
     * 
     * @return a new sequence with values 
     */
    @Override
    public Object clone() {
        CoordinateSequence2D copy = new CoordinateSequence2D(x.length);
        for (int i = 0; i < x.length; i++) {
            copy.x[i] = x[i];
            copy.y[i] = y[i];
        }
        
        return copy;
    }
    
}
