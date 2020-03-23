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


import java.util.logging.Logger;

/** Helper class used for raster classification. */
public class Classification {

    static final Logger LOGGER = Logger.getLogger(Classification.class.getName());

    /** classification method */
    ClassificationMethod method;

    /** the breaks */
    Double[][] breaks;

    /** min/max */
    Double[] min, max;

    double [] percentages;

    public Classification(ClassificationMethod method, int numBands) {
        this.method = method;
        this.breaks = new Double[numBands][];
        this.min = new Double[numBands];
        this.max = new Double[numBands];
    }

    public ClassificationMethod getMethod() {
        return method;
    }

    public Number[][] getBreaks() {
        return breaks;
    }

    public void setBreaks(int b, Double[] breaks) {
        this.breaks[b] = breaks;
    }

    public Double getMin(int b) {
        return min[b];
    }

    public void setMin(int b, Double min) {
        this.min[b] = min;
    }

    public Double getMax(int b) {
        return max[b];
    }

    public void setMax(int b, Double max) {
        this.max[b] = max;
    }

    public double[] getPercentages() {
        return percentages;
    }

    public void setPercentages(double[] percentages) {
        this.percentages = percentages;
    }

    public void print() {
        for (int i = 0; i < breaks.length; i++) {
            for (Double d : breaks[i]) {
                LOGGER.info(String.valueOf(d));
            }
        }
    }
}
