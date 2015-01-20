package it.geosolutions.jaiext.stats;

import javax.media.jai.Histogram;

public class HistogramWrapper extends Histogram {

    public HistogramWrapper(int[] arg0, double[] arg1, double[] arg2, int[][] newBins) {
        super(arg0, arg1, arg2);
        // Setting the new Bin Values
        int[][] bins = getBins();
        for(int i = 0; i < bins.length; i++){
            bins[i] = newBins[i];
        }
    }
}
