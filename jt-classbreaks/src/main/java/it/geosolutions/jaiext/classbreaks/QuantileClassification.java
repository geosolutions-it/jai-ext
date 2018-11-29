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

import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

/** Helper class used for raster quantile classification. */
public class QuantileClassification extends Classification {

    static final Logger LOGGER = Logger.getLogger(Classification.class.getName());

    int[] counts;
    SortedMap<Double, Integer>[] tables;

    public QuantileClassification(int numBands) {
        super(ClassificationMethod.QUANTILE, numBands);
        counts = new int[numBands];
        tables = new SortedMap[numBands];
    }

    public void count(double value, int band) {
        counts[band]++;

        SortedMap<Double, Integer> table = getTable(band);

        Integer count = table.get(value);
        table.put(value, count != null ? new Integer(count + 1) : new Integer(1));
    }

    public SortedMap<Double, Integer> getTable(int band) {
        SortedMap<Double, Integer> table = tables[band];
        if (table == null) {
            table = new TreeMap<Double, Integer>();
            tables[band] = table;
        }
        return table;
    }

    public int getCount(int band) {
        return counts[band];
    }

    void printTable() {
        for (int i = 0; i < tables.length; i++) {
            SortedMap<Double, Integer> table = getTable(i);
            for (Entry<Double, Integer> e : table.entrySet()) {
                LOGGER.info(String.format("%f: %d", e.getKey(), e.getValue()));
            }
        }
    }
}
