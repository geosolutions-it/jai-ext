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

import java.awt.*;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.CRIFImpl;
import javax.media.jai.ROI;

import static it.geosolutions.jaiext.classbreaks.ClassBreaksDescriptor.*;

/**
 * RIF for the ClassBreaks operation.
 *
 * <p>This factory ends up creating on of the following operations based on the "method" parameter.
 *
 * <ul>
 *   <li>{@link EqualIntervalBreaksOpImage}
 *   <li>{@link QuantileBreaksOpImage}
 *   <li>{@link NaturalBreaksOpImage}
 * </ul>
 */
public class ClassBreaksRIF extends CRIFImpl {
    
    public ClassBreaksRIF() {
        super("ClassBreaks");
    }

    public RenderedImage create(ParameterBlock pb, RenderingHints hints) {
        RenderedImage src = pb.getRenderedSource(0);

        int xStart = src.getMinX(); // default values
        int yStart = src.getMinY();

        Integer numBins = pb.getIntParameter(NUM_CLASSES_ARG);
        ClassificationMethod method = (ClassificationMethod) pb.getObjectParameter(METHOD_ARG);
        Double[][] extrema = (Double[][]) pb.getObjectParameter(EXTREMA_ARG);
        ROI roi = (ROI) pb.getObjectParameter(ROI_ARG);
        Integer[] bands = (Integer[]) pb.getObjectParameter(BAND_ARG);
        Integer xPeriod = pb.getIntParameter(X_PERIOD_ARG);
        Integer yPeriod = pb.getIntParameter(Y_PERIOD_ARG);
        Double noData = (Double) pb.getObjectParameter(NODATA_ARG);
        Boolean percentages = (Boolean) pb.getObjectParameter(PERCENTAGES_ARG);
        Boolean histogram = false;
        if (pb.getNumParameters() >= 9) {
            histogram = (Boolean) pb.getObjectParameter(HISTOGRAM_ARG);
        }
        Integer histogramBins = 256;
        if (pb.getNumParameters() >= 10)
            histogramBins = (Integer) pb.getObjectParameter(HISTOGRAM_BINS);

        switch (method) {
            case EQUAL_INTERVAL:
                return new EqualIntervalBreaksOpImage(
                        src, numBins, extrema, roi, bands, xStart, yStart, xPeriod, yPeriod,
                        noData, percentages);
            case QUANTILE:
                if (histogram) {
                    return new QuantileBreaksHistogramOpImage(
                            src,
                            numBins,
                            extrema,
                            roi,
                            bands,
                            xStart,
                            yStart,
                            xPeriod,
                            yPeriod,
                            noData,
                            histogramBins,
                            percentages);
                } else {
                    return new QuantileBreaksOpImage(
                            src, numBins, extrema, roi, bands, xStart, yStart, xPeriod, yPeriod,
                            noData, percentages);
                }
            case NATURAL_BREAKS:
                if (histogram) {
                    return new NaturalBreaksHistogramOpImage(
                            src,
                            numBins,
                            extrema,
                            roi,
                            bands,
                            xStart,
                            yStart,
                            xPeriod,
                            yPeriod,
                            noData,
                            histogramBins,
                            percentages);
                } else {
                    return new NaturalBreaksOpImage(
                            src, numBins, extrema, roi, bands, xStart, yStart, xPeriod, yPeriod,
                            noData, percentages);
                }
            default:
                throw new IllegalArgumentException(method.name());
        }
    }
}
