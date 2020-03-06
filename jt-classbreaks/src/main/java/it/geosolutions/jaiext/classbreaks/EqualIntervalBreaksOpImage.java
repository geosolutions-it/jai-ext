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

import java.awt.image.RenderedImage;
import java.util.List;

import javax.media.jai.ROI;

/** Classification op for the equal interval method. */
public class EqualIntervalBreaksOpImage extends ClassBreaksOpImage {

    public EqualIntervalBreaksOpImage(
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
        if (!percentages)
            return new Classification(ClassificationMethod.EQUAL_INTERVAL, bands.length);
        else
            return new NaturalClassification(ClassificationMethod.EQUAL_INTERVAL, bands.length);
    }

    @Override
    protected Classification preCalculate() {
        if (extrema != null) {
            Classification c = createClassification();

            // calculate the bins
            for (int b = 0; b < bands.length; b++) {
                double min = extrema[0][b];
                double max = extrema[1][b];

                c.setMin(b, min);
                c.setMax(b, max);

                calculateBreaks(c, b);
            }

            return c;
        }
        return null;
    }

    @Override
    protected void handleValue(double d, Classification c, int band) {
        c.setMin(band, c.getMin(band) == null ? d : Math.min(c.getMin(band), d));
        c.setMax(band, c.getMax(band) == null ? d : Math.max(c.getMax(band), d));
        // NaturalClassification allows to add values in order to reach them
        // later when computing percentages
        if (percentages) ((NaturalClassification) c).count(d, band);
    }

    @Override
    protected void postCalculate(Classification c, int band) {
        calculateBreaks(c, band);
    }

    void calculateBreaks(Classification c, int band) {
        Double[] breaks = new Double[numClasses + 1];

        // calculate the breaks
        double min = c.getMin(band);
        double max = c.getMax(band);

        double delta = (max - min) / (double) numClasses;
        double start = min;
        for (int j = 0; j < numClasses; j++) {
            breaks[j] = start;
            start += delta;
        }

        // last value
        breaks[numClasses] = max;
        c.setBreaks(band, breaks);
        if (percentages) {
            c.setPercentages(getPercentages(c, band, breaks));
        }
    }

    private double[] getPercentages(Classification c, int band, Double[] breaks) {
        ClassPercentagesManager percentagesManager = new ClassPercentagesManager();
        List<Double> data = ((NaturalClassification) c).getValues(band);
        return percentagesManager
                .getPercentages(data, breaks, data.size(), numClasses);
    }
}
