package it.geosolutions.jaiext.stats;

import static org.junit.Assert.*;
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
    /** Dimension of the random samples array*/
    private final static int ARRAY_DIMENSIONS = 100;
    /** Tolerance value used for comparison between double */
    private final static double TOLERANCE = 0.1d;
    /** Random samples array*/
    private static double[] testArray;
    /** Object used for calculating the mean*/
    private static Statistics meanObj;
    /** Object used for calculating the sum*/
    private static Statistics sumObj;
    /** Object used for calculating the maximum*/
    private static Statistics maxObj;
    /** Object used for calculating the minimum*/
    private static Statistics minObj;
    /** Object used for calculating the extrema*/
    private static Statistics extremaObj;
    /** Object used for calculating the variance*/
    private static Statistics varianceObj;
    /** Object used for calculating the standard deviation*/
    private static Statistics devstdObj;

    @BeforeClass
    public static void initialSetup() {
        // Creation of an array with random values
        testArray = new double[ARRAY_DIMENSIONS];

        for (int i = 0; i < ARRAY_DIMENSIONS; i++) {
            testArray[i] = Math.random() * 2 + 2;
        }
        // Creation of the Statistics Object
        meanObj = StatsFactory.createStatisticsObjectFromInt(0);
        sumObj = StatsFactory.createStatisticsObjectFromInt(1);
        maxObj = StatsFactory.createStatisticsObjectFromInt(2);
        minObj = StatsFactory.createStatisticsObjectFromInt(3);
        extremaObj = StatsFactory.createStatisticsObjectFromInt(4);
        varianceObj = StatsFactory.createStatisticsObjectFromInt(5);
        devstdObj = StatsFactory.createStatisticsObjectFromInt(6);
    }

    // This test is used for checking if the mean and sum objects
    // have a correct behavior
    @Test
    public void testMeanAndSum() {
        double mean = 0;
        double sum = 0;
        // Mean and sum calculation
        for (int i = 0; i < ARRAY_DIMENSIONS; i++) {
            sum += testArray[i];
            sumObj.addSampleNoNaN(testArray[i], true);
            meanObj.addSampleNoNaN(testArray[i], true);
        }
        // Comparison
        double sum2 = (Double) (sumObj.getResult());
        assertEquals(sum, sum2, TOLERANCE);
        mean = sum / (ARRAY_DIMENSIONS - 1);
        double mean2 = (Double) (meanObj.getResult());
        assertEquals(mean, mean2, TOLERANCE);
    }

    // This test is used for checking if the min, max and extrema objects
    // have a correct behavior
    @Test
    public void testMinMaxExtrema() {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        // Maximum and minimum calculation
        for (int i = 0; i < ARRAY_DIMENSIONS; i++) {
            if (testArray[i] > max) {
                max = testArray[i];
            }
            if (testArray[i] < min) {
                min = testArray[i];
            }
            minObj.addSampleNoNaN(testArray[i], true);
            maxObj.addSampleNoNaN(testArray[i], true);
            extremaObj.addSampleNoNaN(testArray[i], true);
        }
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
        double sum = 0;
        double variance = 0;
        double std = 0;
        // Variance and standard deviation calculation
        for (int i = 0; i < ARRAY_DIMENSIONS; i++) {
            sum += testArray[i];
            varianceObj.addSampleNoNaN(testArray[i], true);
            devstdObj.addSampleNoNaN(testArray[i], true);
        }
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

    // This test is used for checking if the cumulation of the statistics continue to mantain
    // correct results
    @Test
    public void testCumulativeStats() {

        // Addition of dummy data
        Statistics newMeanObj = StatsFactory.createMeanObject();
        newMeanObj.addSampleNoNaN(1, true);

        Statistics newSumObj = StatsFactory.createSumObject();
        newSumObj.addSampleNoNaN(1, true);

        Statistics newMaxObj = StatsFactory.createMaxObject();
        newMaxObj.addSampleNoNaN(1, true);

        Statistics newMinObj = StatsFactory.createMinObject();
        newMinObj.addSampleNoNaN(1, true);

        Statistics newExtremaObj = StatsFactory.createExtremaObject();
        newExtremaObj.addSampleNoNaN(1, true);

        Statistics newVarianceObj = StatsFactory.createVarianceObject();
        newVarianceObj.addSampleNoNaN(1, true);

        Statistics newDevStdObj = StatsFactory.createDevStdObject();
        newDevStdObj.addSampleNoNaN(1, true);

        // Statistics accumulation
        newMeanObj.accumulateStats(meanObj);
        newSumObj.accumulateStats(sumObj);
        newMaxObj.accumulateStats(maxObj);
        newMinObj.accumulateStats(minObj);
        newExtremaObj.accumulateStats(extremaObj);
        newVarianceObj.accumulateStats(varianceObj);
        newDevStdObj.accumulateStats(devstdObj);

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
        newMeanObj.addSampleNoNaN(1, true);

        Statistics newSumObj = StatsFactory.createSumObject();
        newSumObj.addSampleNoNaN(1, true);

        Statistics newMaxObj = StatsFactory.createMaxObject();
        newMaxObj.addSampleNoNaN(1, true);

        Statistics newMinObj = StatsFactory.createMinObject();
        newMinObj.addSampleNoNaN(1, true);

        Statistics newExtremaObj = StatsFactory.createExtremaObject();
        newExtremaObj.addSampleNoNaN(1, true);

        Statistics newVarianceObj = StatsFactory.createVarianceObject();
        newVarianceObj.addSampleNoNaN(1, true);

        Statistics newDevStdObj = StatsFactory.createDevStdObject();
        newDevStdObj.addSampleNoNaN(1, true);

        // Clearing of the statistics
        newMeanObj.clearStats();
        newSumObj.clearStats();
        newMaxObj.clearStats();
        newMinObj.clearStats();
        newExtremaObj.clearStats();
        newVarianceObj.clearStats();
        newDevStdObj.clearStats();

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

        // Comparison
        assertEquals(0, newMeanUpdated, TOLERANCE);
        assertEquals(0, newSumUpdated, TOLERANCE);
        assertEquals(Double.NEGATIVE_INFINITY, newMaxUpdated, TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, newMinUpdated, TOLERANCE);
        assertEquals(Double.NEGATIVE_INFINITY, newExmax, TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, newExmin, TOLERANCE);
        assertEquals(Double.NaN, newVarianceUpdated, TOLERANCE);
        assertEquals(Double.NaN, newStdUpdated, TOLERANCE);
    }

    // This last 7 tests are used for checking if the accumulateStats() method returns an exception when
    // the given statistical object does not belong to the same StatsType of the receiver.
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
}
