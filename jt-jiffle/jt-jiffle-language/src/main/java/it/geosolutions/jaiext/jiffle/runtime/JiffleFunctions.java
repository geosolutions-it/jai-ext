/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2018 GeoSolutions
 *    
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


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

package it.geosolutions.jaiext.jiffle.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static it.geosolutions.jaiext.numeric.CompareOp.*;

import it.geosolutions.jaiext.numeric.SampleStats;

/**
 * Provides functions for Jiffle runtime objects. An instance of this class
 * is used as a field in {@link AbstractJiffleRuntime}.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class JiffleFunctions {
    
    private Random rr = new Random();
    
    /**
     * Converts an angle in degrees to radians.
     * 
     * @param x input angle in degrees
     * @return angle in radians
     */
    public double degToRad(double x) {
        return Math.PI * x / 180d;
    }

    /**
     * Return the sign of {@code x} as an integer. This method is used 
     * by Jiffle to implement its various {@code if} functions.
     * <p>
     * 
     * @param x test value
     * 
     * @return -1 if x is negative; 0 if x is 0; 1 if x is positive; 
     *         or {@code null} if x is NaN
     */
    public Integer sign(double x) {
        if (!Double.isNaN(x)) {
            return acompare(x, 0);
        }
        return null;
    }
    
    /**
     * Tests if x is infinite (equal to Double.POSITIVE_INFINITY or 
     * Double.NEGATIVE_INFINITY).
     * 
     * @param x test value
     * @return 1 if x is infinite; 0 if finite; or {@code Double.Nan}
     *         if x is {@code Double.Nan}
     */
    public double isinf(double x) {
        return (Double.isNaN(x) ? Double.NaN : (Double.isInfinite(x) ? 1d : 0d));
    }
    
    /**
     * Tests if x is equal to Double.NaN.
     * 
     * @param x test value
     * @return 1 if x is NaN; 0 otherwise
     */
    public double isnan(double x) {
        return Double.isNaN(x) ? 1d : 0d;
    }
    
    /**
     * Tests if x is null. This is the same as {@link #isnan(double)}.
     * 
     * @param x the test value
     * @return 1 if x is null; 0 otherwise
     */
    public double isnull(double x) {
        return Double.isNaN(x) ? 1d : 0d;
    }

    /**
     * Gets the log of x to base b.
     * 
     * @param x the value
     * @param b the base
     * @return log to base b
     */
    public double log2Arg(double x, double b) {
        return Math.log(x) / Math.log(b);
    }
    
    /**
     * Gets the maximum of the input values. Double.Nan (null)
     * values are ignored.
     * 
     * @param values the input values
     * @return the maximum value
     */
    public double max(List values) {
        return SampleStats.max(listToArray(values), true);
    }
    
    /**
     * Gets the mean of the input values. Double.Nan (null)
     * values are ignored.
     * 
     * @param values the input values
     * @return the mean value
     */
    public double mean(List values) {
        return SampleStats.mean(listToArray(values), true);
    }
    
    /**
     * Gets the median of the input values. Double.Nan (null)
     * values are ignored.
     * 
     * @param values the input values
     * @return the median value
     */
    public double median(List values) {
        return SampleStats.median(listToArray(values), true);
    }
    
    /**
     * Gets the minimum of the input values. Double.Nan (null)
     * values are ignored.
     * 
     * @param values the input values
     * @return the minimum value
     */
    public double min(List values) {
        return SampleStats.min(listToArray(values), true);
    }
    
    /**
     * Gets the mode of the input values. Double.Nan (null)
     * values are ignored.
     * 
     * @param values the input values
     * @return the modal value
     */
    public double mode(List values) {
        return SampleStats.mode(listToArray(values), true);
    }
    
    /**
     * Converts an angle in radians to degrees.
     * 
     * @param x input angle in radians
     * @return angle in degrees
     */
    public double radToDeg(double x) {
        return x / Math.PI * 180d;
    }
    
    /**
     * Gets a random value between 0 (inclusive) and x (exclusive).
     * 
     * @param x upper limit
     * @return the random value
     */
    public double rand(double x) {
        return rr.nextDouble() * x;
    }
    
    /**
     * Gets a random integer value (actually a truncated double) between 
     * 0 (inclusive) and {@code floor(x)} (exclusive).
     * 
     * @param x upper limit
     * @return the random value
     */
    public double randInt(double x) {
        return rr.nextInt((int) x);
    }
    
    /**
     * Gets the range of the input values. Double.Nan (null)
     * values are ignored.
     * 
     * @param values the input values
     * @return the range of the input values
     */
    public double range(List values) {
        return SampleStats.range(listToArray(values), true);
    }
    
    /**
     * Rounds the input value to the given precision.
     * 
     * @param x the input value
     * @param prec the desired precision
     * @return the rounded value
     */
    public double round2Arg(double x, double prec) {
        int ifac = (int) (prec + 0.5);
        return Math.round(x / ifac) * ifac;
    }
    
    /**
     * Gets the sample standard deviation of the input values. Double.Nan (null)
     * values are ignored.
     * 
     * @param values the input values
     * @return the standard deviation of the input values
     */
    public double sdev(List values) {
        return SampleStats.range(listToArray(values), true);
    }
    
    
    /**
     * Gets the sum of the input values. Double.Nan (null)
     * values are ignored.
     * 
     * @param values the input values
     * @return the sum of the input values
     */
    public double sum(List values) {
        return SampleStats.sum(listToArray(values), true);
    }

    /**
     * Gets the sample variance of the input values. Double.Nan (null)
     * values are ignored.
     * 
     * @param values the input values
     * @return the variance of the input values
     */
    public double variance(List values) {
        return SampleStats.variance(listToArray(values), true);
    }
    
    /**
     * Tests if either x or y is non-zero.
     * 
     * @param x x value
     * @param y y value
     * @return 1 if either x or y is non-zero; 0 if this is not the case;
     *         or {@code Double.Nan} if either x or y is {@code Double.Nan}
     */
    public double OR(double x, double y) {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return Double.NaN;
        }

        return (!isZero(x) || !isZero(y) ? 1d : 0d);
    }

    /**
     * Tests if both x and y are non-zero.
     * 
     * @param x x value
     * @param y y value
     * @return 1 if both x and y are non-zero; 0 if this is not the case;
     *         or {@code Double.Nan} if either x or y is {@code Double.Nan}
     */
    public double AND(double x, double y) {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return Double.NaN;
        }

        return (!isZero(x) && !isZero(y) ? 1d : 0d);
    }

    /**
     * Tests if just one of x or y is non-zero.
     * 
     * @param x x value
     * @param y y value
     * @return 1 if just one of x or y is non-zero; 0 if this is not the case;
     *         or {@code Double.Nan} if either x or y is {@code Double.Nan}
     */
    public double XOR(double x, double y) {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return Double.NaN;
        }

        return (!isZero(x) ^ !isZero(y) ? 1d : 0d);
    }

    /**
     * Tests if x is greater than y.
     * 
     * @param x x value
     * @param y y value
     * @return 1 if x is greater than y; 0 if this is not the case;
     *         or {@code Double.Nan} if either x or y is {@code Double.Nan}
     */
    public double GT(double x, double y) {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return Double.NaN;
        }

        return (acompare(x, y) > 0 ? 1d : 0d);
    }

    /**
     * Tests if x is greater than or equal to y.
     * 
     * @param x x value
     * @param y y value
     * @return 1 if x is greater than or equal to y; 0 if this is not the case;
     *         or {@code Double.Nan} if either x or y is {@code Double.Nan}
     */
    public double GE(double x, double y) {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return Double.NaN;
        }

        return (acompare(x, y) >= 0 ? 1d : 0d);
    }

    /**
     * Tests if x is less than y.
     * 
     * @param x x value
     * @param y y value
     * @return 1 if x is less than y; 0 if this is not the case;
     *         or {@code Double.Nan} if either x or y is {@code Double.Nan}
     */
    public double LT(double x, double y) {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return Double.NaN;
        }

        return (acompare(x, y) < 0 ? 1d : 0d);
    }

    /**
     * Tests if x is less than or equal to y.
     * 
     * @param x x value
     * @param y y value
     * @return 1 if x is less than or equal to y; 0 if this is not the case;
     *         or {@code Double.Nan} if either x or y is {@code Double.Nan}
     */
    public double LE(double x, double y) {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return Double.NaN;
        }

        return (acompare(x, y) <= 0 ? 1d : 0d);
    }

    /**
     * Tests if x is equal to y.
     * 
     * @param x x value
     * @param y y value
     * @return 1 if x is equal to y; 0 if this is not the case;
     *         or {@code Double.Nan} if either x or y is {@code Double.Nan}
     */
    public double EQ(double x, double y) {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return Double.NaN;
        }

        return (acompare(x, y) == 0 ? 1d : 0d);
    }

    /**
     * Tests if x is not equal to y.
     * 
     * @param x x value
     * @param y y value
     * @return 1 if x is not equal to y; 0 if this is not the case;
     *         or {@code Double.Nan} if either x or y is {@code Double.Nan}
     */
    public double NE(double x, double y) {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return Double.NaN;
        }

        return (acompare(x, y) != 0 ? 1d : 0d);
    }

    /**
     * Treats x as true if non-zero, or false if zero and then
     * returns the logical complement.
     * 
     * @param x the test value
     * @return 1 if x is zero; 0 if x is non-zero; 
     * or {@code Double.Nan} if x is {@code Double.Nan}
     */
    public double NOT(double x) {
        if (Double.isNaN(x)) {
            return Double.NaN;
        }

        return (isZero(x) ? 1d : 0d);
    }
    
    /**
     * Creates a new list by concatenating {2code x} and {@code list}.
     * 
     * @param x the value
     * @param list the list
     * @return a new list
     */
    public List concatDL(double x, List list) {
        List copy = new ArrayList(list);
        copy.add(x);
        return copy;
    }
    
    /**
     * Creates a new list by concatenating {@code list} and {2code x}.
     * 
     * @param list the list
     * @param x the value
     * @return a new list
     */
    public List concatLD(List list, double x) {
        List copy = new ArrayList(list);
        copy.add(x);
        return copy;
    }
    
    /**
     * Creates a new list by concatenating two existing lists.
     * 
     * @param list1 the first list
     * @param list2 the second list
     * @return a new list
     */
    public List concatLL(List list1, List list2) {
        List copy = new ArrayList(list1);
        copy.addAll(list2);
        return copy;
    }
    
    /**
     * Convert a list to a double array.
     * 
     * @param values input list
     * @return a new array
     */
    private Double[] listToArray(List values) {
        final int N = values.size();
        Double[] dvalues = new Double[values.size()];
        for (int i = 0; i < N; i++) {
            dvalues[i] = ((Number)values.get(i)).doubleValue();
        }
        return dvalues;
    }

    /**
     * Returns true if x is non zero and non null
     * @return
     */
    public boolean isTrue(Double x) {
        if (!Double.isNaN(x)) {
            return acompare(x, 0) != 0;
        }
        return false;
    }
}

