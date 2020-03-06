package it.geosolutions.jaiext.classbreaks;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * This class gathers method for percentages' computation for natural breaks,
 * quantile and equal interval classifications
 */
public class ClassPercentagesManager {


    //Quantile
    public double[] getPercentages(Map<Double, Integer> data, List<Double> breaks, double nValues, int numClasses) {
        double[] percentages = new double[numClasses];
        Set<Double> keys = data.keySet();
        for (int i = 0; i < numClasses; i++) {
            double current = breaks.get(i);
            double next = breaks.get(i + 1);
            boolean last = numClasses == i + 1;
            double classMembers;
            if (last) {
                classMembers = getClassMembersCount(data, keys, v -> v.doubleValue() >= current
                        && v.doubleValue() <= next);
            } else {
                classMembers = getClassMembersCount(data, keys, v -> v.doubleValue() >= current
                        && v.doubleValue() < next);
            }
            percentages[i] = (classMembers / nValues) * 100;
        }
        return percentages;

    }

    private int getClassMembersCount(Map<Double, Integer> data, Set<Double> keys, Predicate<Double> predicate) {
        int classMembersCount = 0;
        for (Double key : keys) {
            if (predicate.test(key)) {
                classMembersCount += data.get(key);
            }
        }
        return classMembersCount;
    }


    // histogram natural breaks
    public double[] getPercentages(List<HistogramClassification.Bucket> buckets,
                                   Double[] breaks, int numClasses) {
        double[] percentages = new double[numClasses];
        int totalSize = 0;
        for (int i = 0; i < buckets.size(); i++) {
            totalSize += buckets.get(i).getCount();
        }
        int bucketSize = buckets.size();
        for (int i = 0; i < numClasses; i++) {
            double current = breaks[i];
            double next = breaks[i + 1];
            boolean last = numClasses == i + 1;
            double classMembers;
            if (last) {
                classMembers = getClassMembersCount(bucketSize, b -> b.getMax() >= current
                        && b.getMax() <= next, buckets);
            } else if (i == 0) {
                classMembers = getClassMembersCount(bucketSize, b -> b.getMin() >= current
                        && b.getMin() < next, buckets);
            } else {
                classMembers = getClassMembersCount(bucketSize, b -> b.getAverage() >= current
                        && b.getAverage() < next, buckets);
            }
            percentages[i] = (classMembers / totalSize) * 100;
        }
        return percentages;
    }

    private double getClassMembersCount(int bucketSize, Predicate<HistogramClassification.Bucket> predicate, List<HistogramClassification.Bucket> buckets) {
        int classMembers = 0;
        for (int i = 0; i < bucketSize; i++) {
            HistogramClassification.Bucket b = buckets.get(i);
            if (predicate.test(b)) {
                classMembers += b.getCount();
            }
        }
        return classMembers;
    }


    // Natural Breaks and Equal Interval
    public double[] getPercentages(List<Double> data, Double[] breaks,
                                   double totalSize, int numClasses) {
        double[] percentages = new double[numClasses];
        for (int i = 0; i < numClasses; i++) {
            double current = breaks[i];
            double next = breaks[i + 1];
            boolean last = numClasses == i + 1;
            double classMembers;
            if (last) {
                classMembers = getClassMembersCount(v -> v.doubleValue() >= current
                        && v.doubleValue() <= next, data);
            } else {
                classMembers = getClassMembersCount(v -> v.doubleValue() >= current
                        && v.doubleValue() < next, data);
            }
            percentages[i] = (classMembers / totalSize) * 100;
        }
        return percentages;
    }


    private double getClassMembersCount(Predicate<Double> predicate, List<Double> values) {
        int classMembers = 0;
        for (int i = 0; i < values.size(); i++) {
            if (predicate.test(values.get(i))) {
                classMembers++;
            }
        }
        return classMembers;
    }

}
