/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2018 GeoSolutions
 *
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
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2016, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package it.geosolutions.jaiext.classbreaks;

import javax.media.jai.ROI;
import java.awt.image.RenderedImage;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

/**
 * Classification op for the natural breaks method.
 */
public class NaturalBreaksOpImage extends ClassBreaksOpImage {

    public NaturalBreaksOpImage(
            RenderedImage image,
            Integer numClasses,
            Double[][] extrema,
            ROI roi,
            Integer[] bands,
            Integer xStart,
            Integer yStart,
            Integer xPeriod,
            Integer yPeriod,
            Double noData,
            Boolean percentages) {
        super(image, numClasses, extrema, roi, bands, xStart, yStart, xPeriod, yPeriod, noData, percentages);
    }

    @Override
    protected Classification createClassification() {
        return new NaturalClassification(bands.length);
    }

    @Override
    protected void handleValue(double d, Classification c, int band) {
        if (extrema != null) {
            double min = extrema[0][band];
            double max = extrema[1][band];

            if (d < min || d > max) {
                return;
            }
        }
        ((NaturalClassification) c).count(d, band);
    }

    @Override
    protected void postCalculate(Classification c, int band) {
        NaturalClassification nc = (NaturalClassification) c;

        List<Double> data = nc.getValues(band);
        Collections.sort(data);

        final int k = numClasses;
        final int m = data.size();

        if (k >= m) {
            Double[] breaks = data.toArray(new Double[data.size()]);
            // just return all the values
            c.setBreaks(band, breaks);
            setPercentages(data, breaks, m, k, c);
            return;
        }

        int[][] iwork = new int[m + 1][k + 1];
        double[][] work = new double[m + 1][k + 1];

        for (int j = 1; j <= k; j++) {
            // the first item is always in the first class!
            iwork[0][j] = 1;
            iwork[1][j] = 1;
            // initialize work matirix
            work[1][j] = 0;
            for (int i = 2; i <= m; i++) {
                work[i][j] = Double.MAX_VALUE;
            }
        }

        // calculate the class for each data item
        for (int i = 1; i <= m; i++) {
            // sum of data values
            double s1 = 0;
            // sum of squares of data values
            double s2 = 0;

            double var = 0.0;
            // consider all the previous values
            for (int ii = 1; ii <= i; ii++) {
                // index in to sorted data array
                int i3 = i - ii + 1;
                // remember to allow for 0 index
                double val = data.get(i3 - 1);
                // update running totals
                s2 = s2 + (val * val);
                s1 += val;
                double s0 = (double) ii;
                // calculate (square of) the variance
                // (http://secure.wikimedia.org/wikipedia/en/wiki/Standard_deviation#Rapid_calculation_methods)
                var = s2 - ((s1 * s1) / s0);
                // System.out.println(s0+" "+s1+" "+s2);
                // System.out.println(i+","+ii+" var "+var);
                int ik = i3 - 1;
                if (ik != 0) {
                    // not the last value
                    for (int j = 2; j <= k; j++) {
                        // for each class compare current value to var + previous value
                        // System.out.println("\tis "+work[i][j]+" >= "+(var + work[ik][j - 1]));
                        if (work[i][j] >= (var + work[ik][j - 1])) {
                            // if it is greater or equal update classification
                            iwork[i][j] = i3 - 1;
                            // System.out.println("\t\tiwork["+i+"]["+j+"] = "+i3);
                            work[i][j] = var + work[ik][j - 1];
                        }
                    }
                }
            }
            // store the latest variance!
            iwork[i][1] = 1;
            work[i][1] = var;
        }

        TreeSet<Double> breaks = new TreeSet<>();

        // go through matrix and extract class breaks
        int ik = m - 1;
        breaks.add(data.get(ik));
        for (int j = k; j >= 2; j--) {
            int id = (int) iwork[ik][j] - 1; // subtract one as we want inclusive breaks on the
            // left?
            breaks.add(data.get(id));
            ik = (int) iwork[ik][j] - 1;
        }
        breaks.add(data.get(0));
        Double[] arrBreaks = breaks.toArray(new Double[breaks.size()]);
        nc.setBreaks(band, arrBreaks);
        setPercentages(data, arrBreaks, m, k, nc);
    }

    private void setPercentages(List<Double> data, Double[] breaks, int m, int k, Classification nc) {
        if (percentages.booleanValue()) {
            ClassPercentagesManager percentagesManager = new ClassPercentagesManager();
            int actualClassNumber = k > breaks.length ? breaks.length - 1 : k;
            double[] percentages = percentagesManager.getPercentages(data, breaks, m, actualClassNumber);
            nc.setPercentages(percentages);
        }
    }
}
