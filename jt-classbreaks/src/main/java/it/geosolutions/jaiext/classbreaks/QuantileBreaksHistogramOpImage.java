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
 *    (C) 2018, Open Source Geospatial Foundation (OSGeo)
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


import it.geosolutions.jaiext.classbreaks.HistogramClassification.Bucket;

import javax.media.jai.ROI;
import java.awt.image.RenderedImage;
import java.util.*;

/**
 * Classification op for the quantile method, using histograms instead of a fully developed list of
 * values
 */
public class QuantileBreaksHistogramOpImage extends ClassBreaksOpImage {

    int numBins;

    public QuantileBreaksHistogramOpImage(
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
            int numBins,
            Boolean percentages) {
        super(image, numClasses, extrema, roi, bands, xStart, yStart, xPeriod, yPeriod, noData, percentages);
        this.numBins = numBins;
    }

    @Override
    protected Classification createClassification() {
        return new HistogramClassification(bands.length, extrema, numBins);
    }

    @Override
    protected void handleValue(double d, Classification c, int band) {
        ((HistogramClassification) c).count(d, band);
    }

    @Override
    protected void postCalculate(Classification c, int band) {
        HistogramClassification hc = (HistogramClassification) c;
        List<Bucket> buckets = hc.getBuckets(band);

        // calculate the number of values per class
        int nvalues = buckets.stream().mapToInt(b -> b.getCount()).sum();
        int size = (int) Math.ceil(nvalues / (double) numClasses);
        // grab the key iterator
        Iterator<Bucket> it = buckets.iterator();

        TreeSet<Double> set = new TreeSet<Double>();
        Bucket e = it.next();

        int classIdx = 1;
        int count = 0;
        set.add(e.getMin());
        while (classIdx < numClasses && it.hasNext()) {
            count += e.getCount();
            e = it.next();

            if (count >= (size * classIdx)) {
                classIdx++;
                set.add(e.getMin());
            }

        }
        set.add(buckets.get(buckets.size() - 1).getMax());
        hc.setBreaks(band, set.toArray(new Double[set.size()]));
        if (this.percentages.booleanValue()) {
            int nBreaks = set.size();
            int actualClassNum = numClasses >= nBreaks ? nBreaks - 1 : numClasses;
            hc.setPercentages(getPercentages(new ArrayList<>(set), buckets, nvalues, actualClassNum));
        }
    }

    private double[] getPercentages(List<Double> tBreaks, List<Bucket> buckets, int nvalues, int numClasses) {
        Map<Double, Integer> values = new HashMap<>();
        for (Bucket b : buckets) {
            values.put(b.getMin(), b.getCount());
        }
        ClassPercentagesManager percentagesManager = new ClassPercentagesManager();
        return percentagesManager.getPercentages(values, tBreaks, nvalues, numClasses);
    }

}
