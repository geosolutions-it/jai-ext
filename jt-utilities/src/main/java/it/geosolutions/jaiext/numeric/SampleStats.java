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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A collection of static methods to calculate summary statistics for
 * a sample of double-valued data. This class is used by both Jiffle
 * and the KernelStats operator.
 *
 * @author Michael Bedward
 * @author Daniele Romagnoli, GeoSolutions S.A.S.
 * @since 1.0
 * @version $Id$
 */
public class SampleStats {
    
    /**
     * Return the maximum of the given values.
     *
     * @param values sample values
     * @param ignoreNaN specifies whether to ignore NaN values
     * @return max value or Double.NaN if the sample is empty
     */
    public static double max(Double[] values, boolean ignoreNaN) {
        if (values == null || values.length == 0) {
            return Double.NaN;
        } else if (values.length == 1) {
            return values[0];
        }
        
        SortedSet<Double> set = new TreeSet<>();
        set.addAll(Arrays.asList(values));
        if (ignoreNaN) set.remove(Double.NaN);
        return set.last();
    }

    /**
     * Return the mean of the given values.
     *
     * @param values sample values
     * @param ignoreNaN specifies whether to ignore NaN values
     * @return mean value or Double.NaN if the sample is empty
     */
    public static double mean(Double[] values, boolean ignoreNaN) {
        if (values == null || values.length == 0) {
            return Double.NaN;
        } else if (values.length == 1) {
            return values[0];
        }

        double sum = 0.0d;
        int n = 0;
        for (Double val : values) {
            if (val.isNaN()) {
                if (!ignoreNaN) return Double.NaN;
            } else {
                sum += val;
                n++ ;
            }
        }

        return sum / n;
    }

    /**
     * Calculates the minimum of the given values.
     *
     * @param values sample values
     * @param ignoreNaN specifies whether to ignore NaN values
     * @return min value or Double.NaN if the sample is empty
     */
    public static double min(Double[] values, boolean ignoreNaN) {
        if (values == null || values.length == 0) {
            return Double.NaN;
        } else if (values.length == 1) {
            return values[0];
        }
        
        SortedSet<Double> set = new TreeSet<>();
        set.addAll(Arrays.asList(values));
        if (ignoreNaN) set.remove(Double.NaN);
        return set.first();
    }

    /**
     * Calculates the median of the given values. For a sample with an odd
     * number of elements the median is the mid-point value of the 
     * sorted sample. For an even number of elements it is the mean of
     * the two values on either side of the mid-point. 
     * 
     * @param values sample values (need not be pre-sorted)
     * @param ignoreNaN specifies whether to ignore NaN values
     * @return median value or Double.NaN if the sample is empty
     */
    @SuppressWarnings("empty-statement")
    public static double median(Double[] values, boolean ignoreNaN) {
        if (values == null) {
            return Double.NaN;
        }
        
        List<Double> nonNaNValues = new ArrayList<>();
        nonNaNValues.addAll(Arrays.asList(values));
        if (ignoreNaN) {
            while (nonNaNValues.remove(Double.NaN)) /* deliberately empty */ ;
        }
        
        if (nonNaNValues.isEmpty()) {
            return Double.NaN;
        } else if (nonNaNValues.size() == 1) {
            return nonNaNValues.get(0);
        } else if (nonNaNValues.size() == 2) {
            return (nonNaNValues.get(0) + nonNaNValues.get(1)) / 2;
        }
        
        Collections.sort(nonNaNValues);
        
        int midHi = nonNaNValues.size() / 2;
        int midLo = midHi - 1;
        boolean even = nonNaNValues.size() % 2 == 0;

        Double result = 0.0d;
        int k = 0;
        for (Double val : nonNaNValues) {
            if (k == midHi) {
                if (!even) {
                    return val;
                } else {
                    result += val;
                    return result / 2;
                }
            } else if (even && k == midLo) {
                result += val;
            }
            k++ ;
        }
        
        return 0;  // to suppress compiler warning
    }
    
    /**
     * Calculates the empirical mode (highest frequency value) of the given values.
     * Double.NaN values are ignored. If more than one data value occurs with
     * maximum frequency the following tie-break rules are used:
     * <ul>
     * <li> for an odd number of tied values, return their median
     * <li> for an even number of tied values, return the value below
     *      the mid-point of the sorted list of tied values
     * </ul>
     * This ensures that the calculated mode occurs in the sample data.
     * Whether or not the mode is meaningful for the sample is up to the user !
     * 
     * @param values sample values
     * @param ignoreNaN specifies whether to ignore NaN values
     * @return calculated mode or Double.NaN if the sample is empty
     */
    @SuppressWarnings("empty-statement")
    public static double mode(Double[] values, boolean ignoreNaN) {
        if (values == null) {
            return Double.NaN;
        }
        
        List<Double> list = new ArrayList<>();
        list.addAll(Arrays.asList(values));
        if (ignoreNaN) {
            while (list.remove(Double.NaN)) /* deliberately empty */ ;
        }
        
        if (list.isEmpty()) {
            return Double.NaN;
        } else if (list.size() == 1) {
            return list.get(0);
        }
        
        Collections.sort(list);
        
        List<Double> uniqueValues = new ArrayList<>();
        List<Integer> freq = new ArrayList<>();
        
        Double curVal = list.get(0);
        int curFreq = 1;
        int maxFreq = 1;
        
        for (int i = 1; i < list.size(); i++) {
            if (CompareOp.aequal(curVal, list.get(i))) {
                curFreq++ ;
            } else {
                uniqueValues.add(curVal);
                freq.add(curFreq);
                curVal = list.get(i);
                if (curFreq > maxFreq) maxFreq = curFreq;
                curFreq = 1;
            }
        }
        uniqueValues.add(curVal);
        freq.add(curFreq);
        if (curFreq > maxFreq) maxFreq = curFreq;
        
        List<Integer> maxFreqIndices = new ArrayList<>();
        int k = 0;
        for (Integer f : freq) {
            if (f == maxFreq) {
                maxFreqIndices.add(k);
            }
            k++ ;
        }
        
        if (maxFreqIndices.size() == 1) {
            return uniqueValues.get(maxFreqIndices.get(0));
        }

        boolean even = maxFreqIndices.size() % 2 == 0;
        int i = maxFreqIndices.size() / 2;
        if (even) i-- ;
        return uniqueValues.get(maxFreqIndices.get(i));
    }

    /**
     * Calculates the range (max - min) of a set of values.
     *
     * @param values sample values
     * @param ignoreNaN specifies whether to ignore NaN values
     * @return the range or Double.NaN if the set is empty
     */
    public static double range(Double[] values, boolean ignoreNaN) {
        if (values == null || values.length == 0) {
            return Double.NaN;
        } else if (values.length == 1) {
            return 0d;
        }
        
        SortedSet<Double> set = new TreeSet();
        set.addAll(Arrays.asList(values));
        if (ignoreNaN) set.remove(Double.NaN);
        return set.last() - set.first();
    }

    /**
     * Calculates sample variance using the running sample algorithm
     * of Welford (1962) described by Knuth in <i>The Art of Computer
     * Programming (3rd ed)</i> Vol.2 p.232
     * 
     * @param values sample values
     * @param ignoreNaN specifies whether to ignore NaN values
     * @return sample variance
     */
    public static double variance(Double[] values, boolean ignoreNaN) {
        if (values.length < 2) {
            return Double.NaN;
        }

        double mNew, mOld = 0.0d, s = 0.0d;

        int n = 0;
        for (int i = 0; i < values.length; i++) {
            if (Double.isNaN(values[i])) {
                if (!ignoreNaN) {
                    return Double.NaN;
                }
                
            } else {
                n++;
                if (n == 1) {
                    mNew = mOld = values[i];
                } else {
                    mNew = mOld + (values[i] - mOld) / n;
                    s = s + (values[i] - mOld) * (values[i] - mNew);
                    mOld = mNew;
                }
            }
        }

        if (n > 1) {
            return s / (n - 1);
        } else if (n == 1) {
            return 0.0d;
        } else {
            return Double.NaN;
        }
    }

    /**
     * Calculates sample standard deviation. This is a convenience
     * method that calls {@linkplain #variance(Double[], boolean) }
     * and returns the square-root of the result
     *
     * @param values sample values
     * @param ignoreNaN specifies whether to ignore NaN values
     * @return sample standard deviation as a double
     */
    public static double sdev(Double[] values, boolean ignoreNaN) {
        double var = variance(values, ignoreNaN);
        return (Double.isNaN(var) ? Double.NaN : Math.sqrt(var));
    }

    /**
     * Calculates the sum of the values.
     * 
     * @param values sample values
     * @param ignoreNaN specifies whether to ignore NaN values
     * @return sum of the values
     */
    public static double sum(Double[] values, boolean ignoreNaN) {
        double sum = 0.0d;

        for (int i = 0; i < values.length; i++) {
            if (Double.isNaN(values[i])) {
                if (!ignoreNaN) {
                    return Double.NaN;
                }
            } else {
              sum = sum + values[i];  
            }
        }
        
        return sum;
    }
}
