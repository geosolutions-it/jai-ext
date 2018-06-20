/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2018 GeoSolutions


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

/*
 * Copyright (c) 2018, Michael Bedward. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */   

package it.geosolutions.jaiext.numeric;

/**
 * Provides static methods to compare floating point values, taking into account
 * an absolute or proportional tolerance. There are methods for both {@code float} and
 * {@code double} values.
 * The {@code acompare} and {@code aequal} methods use absolute tolerance while
 * the {@code pcompare} and {@code pequal} methods use proportional tolerance.
 * <p>
 * For the proportional tolerance methods, a corresponding absolute tolerance
 * is calculated as:
 * <pre><code>
 *     atol = |ptol| * MAX(|x1|,|x2|)
 * </code></pre>
 * <b>Note:</b> this class does not give any special consideration to the Float 
 * and Double constants {@code NEGATIVE_INFINITY}, {@code POSITIVE_INFINITY}
 * and {@code NaN} over that provided by Java itself.
 * 
 * @author Michael Bedward
 * @since 1.1
 * @version $Id$
 */
public class CompareOp {

    /** Default tolerance for double comparisons: 1.0e-8 */
    public static final double DTOL = 1.0e-8d;
    
    /** Default tolerance for float comparisons: 1.0e-4 */
    public static final float FTOL = 1.0e-4f;
    
    /**
     * Tests if the given {@code double} value is within the default tolerance
     * of zero.
     * 
     * @param x the value
     * @return {@code true} if zero; {@code false} otherwise
     */
    public static boolean isZero(double x) {
        return Math.abs(x) < DTOL;
    }
    
    /**
     * Tests if the given {@code float} value is within the default tolerance
     * of zero.
     * 
     * @param x the value
     * @return {@code true} if zero; {@code false} otherwise
     */
    public static boolean isZero(float x) {
        return Math.abs(x) < FTOL;
    }

    /**
     * Tests if the given {@code double} value is within the specified tolerance
     * of zero. Note that performance reasons, {@code tol} is <strong>assumed</strong>
     * to be positive, ie. this is not checked.
     * 
     * @param x the value
     * @param tol the tolerance
     * @return {@code true} if zero; {@code false} otherwise
     */
    public static boolean isZero(double x, double tol) {
        return Math.abs(x) < tol;
    }

    /**
     * Tests if the given {@code float} value is within the specified tolerance
     * of zero. Note that performance reasons, {@code tol} is <strong>assumed</strong>
     * to be positive, ie. this is not checked.
     * 
     * @param x the value
     * @param tol the tolerance
     * @return {@code true} if zero; {@code false} otherwise
     */
    public static boolean isZero(float x, float tol) {
        return Math.abs(x) < tol;
    }

    /**
     * Compares two {@code double} values using the default tolerance.
     * 
     * @param x1 first value
     * @param x2 second value
     * 
     * @return a value less than 0 if x1 is less than x2; 0 if x1 is equal to x2;
     * a value greater than 0 if x1 is greater than x2
     */
    public static int acompare(double x1, double x2) {
        if (isZero(x1 - x2)) {
            return 0;
        } else {
            return Double.compare(x1, x2);
        }
    }
    
    /**
     * Compares two {@code float} values using the default tolerance.
     * 
     * @param x1 first value
     * @param x2 second value
     * 
     * @return a value less than 0 if x1 is less than x2; 0 if x1 is equal to x2;
     * a value greater than 0 if x1 is greater than x2
     */
    public static int acompare(float x1, float x2) {
        if (isZero(x1 - x2)) {
            return 0;
        } else {
            return Float.compare(x1, x2);
        }
    }
    
    /**
     * Compares two {@code double} values using the specified tolerance.
     * Note that performance reasons, {@code tol} is <strong>assumed</strong>
     * to be positive, ie. this is not checked.
     * 
     * @param x1 first value
     * @param x2 second value
     * @param tol comparison tolerance
     * 
     * @return a value less than 0 if x1 is less than x2; 0 if x1 is equal to x2;
     * a value greater than 0 if x1 is greater than x2
     */
    public static int acompare(double x1, double x2, double tol) {
        if (isZero(x1 - x2, tol)) {
            return 0;
        } else {
            return Double.compare(x1, x2);
        }
    }

    /**
     * Compares two {@code float} values using the specified tolerance.
     * Note that performance reasons, {@code tol} is <strong>assumed</strong>
     * to be positive, ie. this is not checked.
     * 
     * @param x1 first value
     * @param x2 second value
     * @param tol comparison tolerance
     * 
     * @return a value less than 0 if x1 is less than x2; 0 if x1 is equal to x2;
     * a value greater than 0 if x1 is greater than x2
     */
    public static int acompare(float x1, float x2, float tol) {
        if (isZero(x1 - x2, tol)) {
            return 0;
        } else {
            return Float.compare(x1, x2);
        }
    }

    /**
     * Compares two {@code double} values using the specified proportional
     * tolerance. This is equivalent to:
     * <pre><code>
     *     double absoluteTol = Math.abs(propTol) * Math.max(Math.abs(x1), Math.abs(x2));
     *     int comp = acompare(x1, x2, absTol);
     * </code></pre>
     * 
     * @param x1 first value
     * @param x2 second value
     * @param propTol proportional tolerance between 0 and 1
     * 
     * @return a value less than 0 if x1 is less than x2; 0 if x1 is equal to x2;
     * a value greater than 0 if x1 is greater than x2
     */
    public static int pcompare(double x1, double x2, double propTol) {
        if (aequal(x1, x2)) {
            return 0;
        }
        
        int comp = acompare(Math.abs(x1), Math.abs(x2));
        double absTol = Math.abs(propTol) * (comp > 0 ? x1 : x2);
        return acompare(x1, x2, absTol);
    }

    /**
     * Compares two {@code float} values using the specified proportional
     * tolerance. This is equivalent to:
     * <pre><code>
     *     float absoluteTol = Math.abs(propTol) * Math.max(Math.abs(x1), Math.abs(x2));
     *     int comp = acompare(x1, x2, absTol);
     * </code></pre>
     * 
     * @param x1 first value
     * @param x2 second value
     * @param propTol proportional tolerance between 0 and 1
     * 
     * @return a value less than 0 if x1 is less than x2; 0 if x1 is equal to x2;
     * a value greater than 0 if x1 is greater than x2
     */
    public static int pcompare(float x1, float x2, float propTol) {
        if (aequal(x1, x2)) {
            return 0;
        }
        
        int comp = acompare(Math.abs(x1), Math.abs(x2));
        double absTol = Math.abs(propTol) * (comp > 0 ? x1 : x2);
        return acompare(x1, x2, absTol);
    }

    /**
     * Tests if two {@code double} values are equal within the default tolerance.
     * This is equivalent to {@code dzero(x1 - x2)}.
     * 
     * @param x1 first value
     * @param x2 second value
     * 
     * @return {@code true} if equal; {@code false} otherwise
     */
    public static boolean aequal(double x1, double x2) {
        return isZero(x1 - x2);
    }

    /**
     * Tests if two {@code float} values are equal within the default tolerance.
     * This is equivalent to {@code dzero(x1 - x2)}.
     * 
     * @param x1 first value
     * @param x2 second value
     * 
     * @return {@code true} if equal; {@code false} otherwise
     */
    public static boolean aequal(float x1, float x2) {
        return isZero(x1 - x2);
    }

    /**
     * Tests if two {@code double} values are equal within the specified tolerance.
     * This is equivalent to {@code dzero(x1 - x2, tol)}.
     * Note that performance reasons, {@code tol} is <strong>assumed</strong>
     * to be positive, ie. this is not checked.
     * 
     * @param x1 first value
     * @param x2 second value
     * @param tol comparison tolerance
     * 
     * @return {@code true} if equal; {@code false} otherwise
     */
    public static boolean aequal(double x1, double x2, double tol) {
        return isZero(x1 - x2, tol);
    }

    /**
     * Tests if two {@code float} values are equal within the specified tolerance.
     * This is equivalent to {@code dzero(x1 - x2, tol)}.
     * Note that performance reasons, {@code tol} is <strong>assumed</strong>
     * to be positive, ie. this is not checked.
     * 
     * @param x1 first value
     * @param x2 second value
     * @param tol comparison tolerance
     * 
     * @return {@code true} if equal; {@code false} otherwise
     */
    public static boolean aequal(float x1, float x2, float tol) {
        return isZero(x1 - x2, tol);
    }

    /**
     * Tests if two {@code double} values are equal within the specified 
     * proportional tolerance. This is equivalent to:
     * <pre><code>
     *     double absoluteTol = Math.abs(propTol) * Math.max(Math.abs(x1), Math.abs(x2));
     *     boolean b = aequal(x1, x2, absTol);
     * </code></pre>
     * 
     * @param x1 first value
     * @param x2 second value
     * @param propTol proportional tolerance between 0 and 1
     * 
     * @return {@code true} if equal; {@code false} otherwise
     */
    public static boolean pequal(double x1, double x2, double propTol) {
        if (aequal(x1, x2)) {
            return true;
        }
        
        int comp = acompare(Math.abs(x1), Math.abs(x2));
        double absTol = Math.abs(propTol) * (comp > 0 ? x1 : x2);
        return aequal(x1, x2, absTol);
    }

    /**
     * Tests if two {@code float} values are equal within the specified 
     * proportional tolerance. This is equivalent to:
     * <pre><code>
     *     float absoluteTol = Math.abs(propTol) * Math.max(Math.abs(x1), Math.abs(x2));
     *     boolean b = aequal(x1, x2, absTol);
     * </code></pre>
     * 
     * @param x1 first value
     * @param x2 second value
     * @param propTol proportional tolerance between 0 and 1
     * 
     * @return {@code true} if equal; {@code false} otherwise
     */
    public static boolean pequal(float x1, float x2, float propTol) {
        if (aequal(x1, x2)) {
            return true;
        }
        
        int comp = acompare(Math.abs(x1), Math.abs(x2));
        double absTol = Math.abs(propTol) * (comp > 0 ? x1 : x2);
        return aequal(x1, x2, absTol);
    }

}
