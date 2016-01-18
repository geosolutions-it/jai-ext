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
package it.geosolutions.jaiext.stats;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test-class verifies that the Statistics object is able to compute the statistics correctly. For achieving this purpose, all the subclasses of
 * the {@link Statistics} abstract class are tested. The first 3 tests compares the ability to calculate statistics of the subclasses. Then is checked
 * the ability to accumulate data previously calculated, by calling the method accumulateStats(). Also is tested the capability of clearing the stored
 * data. The last 7 tests evaluates if the subclasses are capable to throw an exception when they call the accumulateStats() method with another
 * Statistics object different from their type.
 */
public class StatisticsTest {
    /** Dimension of the random samples array */
    private final static int ARRAY_DIMENSIONS = 100;

    /** Tolerance value used for comparison between double */
    private final static double TOLERANCE = 0.1d;

    /** Random samples array */
    private static double[] testArray;

    /** Object used for calculating the mean */
    private static Statistics meanObj;

    /** Object used for calculating the sum */
    private static Statistics sumObj;

    /** Object used for calculating the maximum */
    private static Statistics maxObj;

    /** Object used for calculating the minimum */
    private static Statistics minObj;

    /** Object used for calculating the extrema */
    private static Statistics extremaObj;

    /** Object used for calculating the variance */
    private static Statistics varianceObj;

    /** Object used for calculating the standard deviation */
    private static Statistics devstdObj;

    /** Object used for calculating the histogram */
    private static Statistics histogramObj;

    /** Object used for calculating the mode */
    private static Statistics modeObj;

    /** Object used for calculating the median */
    private static Statistics medianObj;

    /** Minimum bound for complex statistics */
    private static double minBound;

    /** Maximum bound for complex statistics */
    private static double maxBound;

    /** Bin size for complex statistics */
    private static double binInterval;

    /** Bin number for complex statistics */
    private static int numBins;

    /** Values interval for complex statistics */
    private static Range interval;

    @BeforeClass
    public static void initialSetup() {
        // Creation of an array with random values
        testArray = new double[ARRAY_DIMENSIONS];

        // Definition of the Histogram parameters
        minBound = -3;
        maxBound = 3;
        numBins = 20;

        binInterval = (maxBound - minBound) / numBins;
        interval = RangeFactory.create(minBound, true, maxBound, false, false);
        for (int i = 0; i < ARRAY_DIMENSIONS; i++) {
            testArray[i] = Math.random() * 2 + 2;
        }
        // Creation of the Statistics Object
        meanObj = StatsFactory.createSimpleStatisticsObjectFromInt(0);
        sumObj = StatsFactory.createSimpleStatisticsObjectFromInt(1);
        maxObj = StatsFactory.createSimpleStatisticsObjectFromInt(2);
        minObj = StatsFactory.createSimpleStatisticsObjectFromInt(3);
        extremaObj = StatsFactory.createSimpleStatisticsObjectFromInt(4);
        varianceObj = StatsFactory.createSimpleStatisticsObjectFromInt(5);
        devstdObj = StatsFactory.createSimpleStatisticsObjectFromInt(6);
        histogramObj = StatsFactory.createComplexStatisticsObjectFromInt(7, minBound, maxBound,
                numBins);
        modeObj = StatsFactory.createComplexStatisticsObjectFromInt(8, minBound, maxBound, numBins);
        medianObj = StatsFactory.createComplexStatisticsObjectFromInt(9, minBound, maxBound,
                numBins);
    }

    // Private method for calculating the bin/index related to the sample
    private int getIndex(double sample) {
        int index = (int) ((sample - minBound) / binInterval);
        return index;
    }

    // This test is used for checking if the mean and sum objects
    // have a correct behavior
    @Test
    public void testMeanAndSum() {
        double mean = 0;
        double sum = calculateSumMean(sumObj, meanObj);
        // Comparison
        double sum2 = (Double) (sumObj.getResult());
        assertEquals(sum, sum2, TOLERANCE);
        mean = sum / (ARRAY_DIMENSIONS - 1);
        double mean2 = (Double) (meanObj.getResult());
        assertEquals(mean, mean2, TOLERANCE);
    }

    // tests mean not using a "static" (ie. not computed by the class under test) 
    // as the expected value
    @Test
    public void testMeanCorrectness() {
        MeanSum mean = new MeanSum(false);
        mean.addSample(2);
        mean.addSample(2);
        assertEquals(2d, (Double)(mean.getResult()), TOLERANCE);
    }

    // This test is used for checking if the min, max and extrema objects
    // have a correct behavior
    @Test
    public void testMinMaxExtrema() {
        double[] minMax = calculateMaxMinExtrema(minObj, maxObj, extremaObj);
        
        double min = minMax[0];
        double max = minMax[1];
        
        // Comparison
        double[] array = (double[]) (extremaObj.getResult());
        double max2 = array[1];
        double min2 = array[0];
        assertEquals(min, min2, TOLERANCE);
        assertEquals(max, max2, TOLERANCE);
        double min3 = (Double) (minObj.getResult());
        assertEquals(min, min3, TOLERANCE);
        double max3 = (Double) (maxObj.getResult());
        assertEquals(max, max3, TOLERANCE);
    }

    // This test is used for checking if the variance and devStd objects
    // have a correct behavior
    @Test
    public void testDevStdVariance() {
        double mean = 0;
        double variance = 0;
        double std = 0;
        double sum = calculateVarianceAndStd(varianceObj, devstdObj);
        mean = sum / (ARRAY_DIMENSIONS - 1);
        double sum2 = 0;
        for (int i = 0; i < ARRAY_DIMENSIONS; i++) {
            sum2 += Math.pow((testArray[i] - mean), 2);

        }
        // Comparison
        variance = sum2 / (ARRAY_DIMENSIONS - 1);
        double variance2 = (Double) (varianceObj.getResult());
        assertEquals(variance, variance2, TOLERANCE);

        std = Math.sqrt(variance);
        double std2 = (Double) (devstdObj.getResult());
        assertEquals(std, std2, TOLERANCE);
    }

    // This test is used for checking if the histogram,mode and median objects
    // have a correct behavior
    @Test
    public void testHistModeMedian() {
        double[] hist = new double[numBins];
        List<Double> listData = new ArrayList<Double>();
        double median = 0;

        // Mean and sum calculation
        for (int i = 0; i < ARRAY_DIMENSIONS; i++) {
            if (interval.contains(testArray[i])) {
                int index = getIndex(testArray[i]);
                hist[index]++;
                listData.add(testArray[i]);
            }
            modeObj.addSample(testArray[i]);
            histogramObj.addSample(testArray[i]);
            medianObj.addSample(testArray[i]);
        }

        // Selection of the median
        Collections.sort(listData);
        int listSize = listData.size();
        if (listSize == 0) {
            median = Double.NaN;
        } else if (listSize == 1) {
            median = listData.get(0);
        } else {
            int halfSize = listSize / 2;
            double halfValue = listData.get(halfSize);
            if (listData.size() % 2 == 1) {
                median = halfValue;
            } else {
                median = (halfValue + listData.get(halfSize + 1)) / 2;
            }
        }
        // Selection of the mode
        double max = Double.NEGATIVE_INFINITY;
        double indexMax = 0;
        for (int i = 0; i < numBins; i++) {
            if (hist[i] > max) {
                max = hist[i];
                indexMax = i;
            }
        }

        if (max != 0) {
            indexMax = indexMax + minBound;
        }

        // Comparison
        double indexMax2 = (Double) (modeObj.getResult());
        assertEquals(indexMax, indexMax2, TOLERANCE);
        double[] hist2 = (double[]) (histogramObj.getResult());
        for (int i = 0; i < numBins; i++) {
            assertEquals(hist[i], hist2[i], TOLERANCE);
        }
        double median2 = (Double) (medianObj.getResult());
        assertEquals(median, median2, TOLERANCE);
    }

    // This test is used for checking if the cumulation of the statistics continue to mantain
    // correct results
    @Test
    public void testCumulativeStats() {
    	// Calculation of the statistics
        Statistics oldMeanObj = StatsFactory.createMeanObject();
        Statistics oldSumObj = StatsFactory.createSumObject();
        Statistics oldMaxObj = StatsFactory.createMaxObject();
        Statistics oldMinObj = StatsFactory.createMinObject();
        Statistics oldExtremaObj = StatsFactory.createExtremaObject();
        Statistics oldVarianceObj = StatsFactory.createVarianceObject();
        Statistics oldDevStdObj = StatsFactory.createDevStdObject();
    	// Calculate sum and mean
        calculateSumMean(oldSumObj, oldMeanObj);
    	// Calculate Max and Min and Extrema
        calculateMaxMinExtrema(oldMinObj, oldMaxObj, oldExtremaObj);
        // Calculate Variance and Std
        calculateVarianceAndStd(oldVarianceObj, oldDevStdObj);
    	
        // Addition of dummy data
        Statistics newMeanObj = StatsFactory.createMeanObject();
        newMeanObj.addSample(1);

        Statistics newSumObj = StatsFactory.createSumObject();
        newSumObj.addSample(1);

        Statistics newMaxObj = StatsFactory.createMaxObject();
        newMaxObj.addSample(1);

        Statistics newMinObj = StatsFactory.createMinObject();
        newMinObj.addSample(1);

        Statistics newExtremaObj = StatsFactory.createExtremaObject();
        newExtremaObj.addSample(1);

        Statistics newVarianceObj = StatsFactory.createVarianceObject();
        newVarianceObj.addSample(1);

        Statistics newDevStdObj = StatsFactory.createDevStdObject();
        newDevStdObj.addSample(1);

        // Statistics accumulation
        newMeanObj.accumulateStats(oldMeanObj);
        newSumObj.accumulateStats(oldSumObj);
        newMaxObj.accumulateStats(oldMaxObj);
        newMinObj.accumulateStats(oldMinObj);
        newExtremaObj.accumulateStats(oldExtremaObj);
        newVarianceObj.accumulateStats(oldVarianceObj);
        newDevStdObj.accumulateStats(oldDevStdObj);

        // Storage of the updated statistics
        double newMeanUpdated = (Double) (newMeanObj.getResult());
        double newSumUpdated = (Double) (newSumObj.getResult());
        double newMaxUpdated = (Double) (newMaxObj.getResult());
        double newMinUpdated = (Double) (newMinObj.getResult());
        double[] newExtrema = (double[]) (newExtremaObj.getResult());
        double newExmin = newExtrema[0];
        double newExmax = newExtrema[1];
        double newVarianceUpdated = (Double) (newVarianceObj.getResult());
        double newStdUpdated = (Double) (newDevStdObj.getResult());
        // New calculation of the statistics
        double sum = 0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < ARRAY_DIMENSIONS; i++) {
            sum += testArray[i];
            if (testArray[i] > max) {
                max = testArray[i];
            }
            if (testArray[i] < min) {
                min = testArray[i];
            }
        }

        double meanCalc = (sum + 1) / ARRAY_DIMENSIONS;
        double sumCalc = (sum + 1);
        double maxCalc = 1 > max ? 1 : max;
        double minCalc = 1 < min ? 1 : min;

        double varianceCalc = 0;
        double stdCalc = 0;

        double sum2 = 0;
        for (int i = 0; i < ARRAY_DIMENSIONS; i++) {
            sum2 += Math.pow((testArray[i] - meanCalc), 2);

        }
        sum2 += Math.pow((1 - meanCalc), 2);

        varianceCalc = sum2 / (ARRAY_DIMENSIONS);
        stdCalc = Math.sqrt(varianceCalc);
        // Comparison
        assertEquals(meanCalc, newMeanUpdated, TOLERANCE);
        assertEquals(sumCalc, newSumUpdated, TOLERANCE);
        assertEquals(maxCalc, newMaxUpdated, TOLERANCE);
        assertEquals(minCalc, newMinUpdated, TOLERANCE);
        assertEquals(maxCalc, newExmax, TOLERANCE);
        assertEquals(minCalc, newExmin, TOLERANCE);
        assertEquals(varianceCalc, newVarianceUpdated, TOLERANCE);
        assertEquals(stdCalc, newStdUpdated, TOLERANCE);
    }

    // This test is used for checking if the statistics are correctly cleared
    @Test
    public void testEmptyStats() {
        // Addition of dummy data
        Statistics newMeanObj = StatsFactory.createMeanObject();
        newMeanObj.addSample(1);

        Statistics newSumObj = StatsFactory.createSumObject();
        newSumObj.addSample(1);

        Statistics newMaxObj = StatsFactory.createMaxObject();
        newMaxObj.addSample(1);

        Statistics newMinObj = StatsFactory.createMinObject();
        newMinObj.addSample(1);

        Statistics newExtremaObj = StatsFactory.createExtremaObject();
        newExtremaObj.addSample(1);

        Statistics newVarianceObj = StatsFactory.createVarianceObject();
        newVarianceObj.addSample(1);

        Statistics newDevStdObj = StatsFactory.createDevStdObject();
        newDevStdObj.addSample(1);

        Statistics newHistObj = StatsFactory.createHistogramObject(numBins, minBound, maxBound);
        newHistObj.addSample(1);

        Statistics newModeObj = StatsFactory.createModeObject(numBins, minBound, maxBound);
        newModeObj.addSample(1);

        Statistics newMedianObj = StatsFactory.createMedianObject(minBound, maxBound);
        newMedianObj.addSample(1);

        // Clearing of the statistics
        newMeanObj.clearStats();
        newSumObj.clearStats();
        newMaxObj.clearStats();
        newMinObj.clearStats();
        newExtremaObj.clearStats();
        newVarianceObj.clearStats();
        newDevStdObj.clearStats();
        newHistObj.clearStats();
        newModeObj.clearStats();
        newMedianObj.clearStats();

        // Storage of the cleared statistics
        double newMeanUpdated = (Double) (newMeanObj.getResult());
        double newSumUpdated = (Double) (newSumObj.getResult());
        double newMaxUpdated = (Double) (newMaxObj.getResult());
        double newMinUpdated = (Double) (newMinObj.getResult());
        double[] newExtrema = (double[]) (newExtremaObj.getResult());
        double newExmin = newExtrema[0];
        double newExmax = newExtrema[1];
        double newVarianceUpdated = (Double) (newVarianceObj.getResult());
        double newStdUpdated = (Double) (newDevStdObj.getResult());
        double[] newHistUpdated = (double[]) (newHistObj.getResult());
        double newModeUpdated = (Double) (newModeObj.getResult());
        double newMedianUpdated = (Double) (newMedianObj.getResult());

        // Comparison
        assertEquals(0, newMeanUpdated, TOLERANCE);
        assertEquals(0, newSumUpdated, TOLERANCE);
        assertEquals(Double.NEGATIVE_INFINITY, newMaxUpdated, TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, newMinUpdated, TOLERANCE);
        assertEquals(Double.NEGATIVE_INFINITY, newExmax, TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, newExmin, TOLERANCE);
        assertEquals(Double.NaN, newVarianceUpdated, TOLERANCE);
        assertEquals(Double.NaN, newStdUpdated, TOLERANCE);
        for (int i = 0; i < numBins; i++) {
            assertEquals(0, newHistUpdated[i], TOLERANCE);
        }
        assertEquals(0, newModeUpdated, TOLERANCE);
        assertEquals(Double.NaN, newMedianUpdated, TOLERANCE);
    }

    /*
     * These tests are used for checking if the accumulateStats() method returns an exception when the given statistical object does not belong to the
     * same StatsType of the receiver or if it is not supported
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMeanException() {
        meanObj.accumulateStats(sumObj);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSumException() {
        sumObj.accumulateStats(meanObj);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaxException() {
        maxObj.accumulateStats(sumObj);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinException() {
        minObj.accumulateStats(sumObj);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtremaException() {
        extremaObj.accumulateStats(sumObj);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVarianceException() {
        varianceObj.accumulateStats(sumObj);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDevStdException() {
        devstdObj.accumulateStats(sumObj);
    }
    @Test(expected = UnsupportedOperationException.class)
    public void testHistException() {
        histogramObj.accumulateStats(sumObj);
    }
    @Test(expected = UnsupportedOperationException.class)
    public void testModeException() {
        modeObj.accumulateStats(sumObj);
    }
    @Test(expected = UnsupportedOperationException.class)
    public void testMedianException() {
        medianObj.accumulateStats(sumObj);
    }
    
	private double calculateSumMean(Statistics sumObj, Statistics meanObj) {
		double sum = 0;
        // Mean and sum calculation
        for (int i = 0; i < ARRAY_DIMENSIONS; i++) {
            sum += testArray[i];
            sumObj.addSample(testArray[i]);
            meanObj.addSample(testArray[i]);
        }
		return sum;
	}
    
	private double[] calculateMaxMinExtrema(Statistics minObj, Statistics maxObj, Statistics extremaObj) {
		double[] minMax = new double[]{Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        // Maximum and minimum calculation
        for (int i = 0; i < ARRAY_DIMENSIONS; i++) {
            if (testArray[i] > minMax[1]) {
            	minMax[1] = testArray[i];
            }
            if (testArray[i] < minMax[0]) {
            	minMax[0] = testArray[i];
            }
            minObj.addSample(testArray[i]);
            maxObj.addSample(testArray[i]);
            extremaObj.addSample(testArray[i]);
        }
		return minMax;
	}
    
	private double calculateVarianceAndStd(Statistics varianceObj, Statistics devstdObj) {
		double sum = 0;
        // Variance and standard deviation calculation
        for (int i = 0; i < ARRAY_DIMENSIONS; i++) {
            sum += testArray[i];
            varianceObj.addSample(testArray[i]);
            devstdObj.addSample(testArray[i]);
        }
		return sum;
	}
}
