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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.awt.*;
import java.awt.image.RenderedImage;
import java.util.Arrays;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ExtremaDescriptor;

import it.geosolutions.jaiext.JAIExt;
import it.geosolutions.jaiext.testclasses.TestBase;
import it.geosolutions.jaiext.utilities.ImageUtilities;

public class ClassBreaksOpImageTest extends TestBase {

    static final double EPS = 1e-3;

    static RenderedImage createImage() {
        return ImageUtilities.createImageFromArray(
                new Number[] {1, 1, 2, 3, 3, 8, 8, 9, 11, 14, 16, 24, 26, 26, 45, 53}, 4, 4);
    }

    @Test
    public void getMissingProperty() {
        RenderedImage image = createImage();

        ParameterBlockJAI pb = new ParameterBlockJAI(new ClassBreaksDescriptor());
        pb.addSource(image);
        pb.setParameter("method", ClassificationMethod.QUANTILE);
        pb.setParameter("numClasses", 5);
        // raw creation like in CoverageClassStats, otherwise the issue gets masked by JAI wrappers
        RenderedImage op = new ClassBreaksRIF().create(pb, null);

        // used to NPE here
        Object roi = op.getProperty("ROI");
        assertEquals(Image.UndefinedProperty, roi);
    }

    @Test
    public void testEqualInterval() throws Exception {
        RenderedImage image = createImage();

        ParameterBlockJAI pb = new ParameterBlockJAI(new ClassBreaksDescriptor());
        pb.addSource(image);
        pb.setParameter("method", ClassificationMethod.EQUAL_INTERVAL);
        pb.setParameter("numClasses", 4);
        RenderedImage op  = JAI.create("ClassBreaks", pb, null);
        Classification classification =
                (Classification) op.getProperty(ClassBreaksDescriptor.CLASSIFICATION_PROPERTY);
        assertNotNull(classification);
        Number[] breaks = classification.getBreaks()[0];

        assertEquals(5, breaks.length);
        assertEquals(1, breaks[0].doubleValue(), EPS);
        assertEquals(14, breaks[1].doubleValue(), EPS);
        assertEquals(27, breaks[2].doubleValue(), EPS);
        assertEquals(40, breaks[3].doubleValue(), EPS);
        assertEquals(53, breaks[4].doubleValue(), EPS);
    }

    @Test
    public void testQuantileBreaks() throws Exception {
        RenderedImage image = createImage();

        ParameterBlockJAI pb = new ParameterBlockJAI(new ClassBreaksDescriptor());
        pb.addSource(image);
        pb.setParameter("method", ClassificationMethod.QUANTILE);
        pb.setParameter("numClasses", 4);
        RenderedImage op  = JAI.create("ClassBreaks", pb, null);
        Classification classification =
                (Classification) op.getProperty(ClassBreaksDescriptor.CLASSIFICATION_PROPERTY);
        assertNotNull(classification);
        Number[] breaks = classification.getBreaks()[0];

        // 4 classes, 5 breaks
        // 1, 1, 2,
        // 3, 3, 8, 8, 9,
        // 11, 14, 16, 24,
        // 26, 26, 45, 53
        assertEquals(5, breaks.length);
        assertEquals(1, breaks[0].doubleValue(), EPS);
        assertEquals(3, breaks[1].doubleValue(), EPS);
        assertEquals(11, breaks[2].doubleValue(), EPS);
        assertEquals(26, breaks[3].doubleValue(), EPS);
        assertEquals(53, breaks[4].doubleValue(), EPS);
    }

    @Test
    public void testQuantileBreaksHistogram() throws Exception {
        RenderedImage image = createImage();

        ParameterBlockJAI pb = new ParameterBlockJAI(new ClassBreaksDescriptor());
        pb.addSource(image);
        pb.setParameter("method", ClassificationMethod.QUANTILE);
        pb.setParameter("numClasses", 4);
        pb.setParameter("extrema", getExtrema(image));
        pb.setParameter("histogram", true);
        pb.setParameter("histogramBins", 100);
        RenderedImage op  = JAI.create("ClassBreaks", pb, null);
        Classification classification =
                (Classification) op.getProperty(ClassBreaksDescriptor.CLASSIFICATION_PROPERTY);
        assertNotNull(classification);
        Number[] breaks = classification.getBreaks()[0];

        // 4 classes, 5 breaks (not the same as the exact count, slightly different approach,
        // but still correct)
        // 1, 1, 2, 3, 3,
        // 8, 8, 9,
        // 11, 14, 16, 24,
        // 26, 26, 45, 53
        assertEquals(5, breaks.length);
        assertEquals(1, breaks[0].doubleValue(), EPS);
        assertEquals(8, breaks[1].doubleValue(), EPS);
        assertEquals(11, breaks[2].doubleValue(), EPS);
        assertEquals(26, breaks[3].doubleValue(), EPS);
        assertEquals(53, breaks[4].doubleValue(), EPS);
    }

    @Test
    public void testNaturalBreaks() throws Exception {
        RenderedImage image = createImage();

        ParameterBlockJAI pb = new ParameterBlockJAI(new ClassBreaksDescriptor());
        pb.addSource(image);
        pb.setParameter("method", ClassificationMethod.NATURAL_BREAKS);
        pb.setParameter("numClasses", 4);
        RenderedImage op  = JAI.create("ClassBreaks", pb, null);
        Classification classification =
                (Classification) op.getProperty(ClassBreaksDescriptor.CLASSIFICATION_PROPERTY);
        assertNotNull(classification);
        Number[] breaks = classification.getBreaks()[0];

        // 4 classes, 5 breaks
        assertEquals(5, breaks.length);
        assertEquals(1, breaks[0].doubleValue(), EPS);
        assertEquals(3, breaks[1].doubleValue(), EPS);
        assertEquals(16, breaks[2].doubleValue(), EPS);
        assertEquals(26, breaks[3].doubleValue(), EPS);
        assertEquals(53, breaks[4].doubleValue(), EPS);
    }

    @Test
    public void testNaturalBreaksHistogram() throws Exception {
        RenderedImage image = createImage();

        ParameterBlockJAI pb = new ParameterBlockJAI(new ClassBreaksDescriptor());
        pb.addSource(image);
        pb.setParameter("method", ClassificationMethod.NATURAL_BREAKS);
        pb.setParameter("numClasses", 4);
        pb.setParameter("extrema", getExtrema(image));
        pb.setParameter("histogram", true);
        pb.setParameter("histogramBins", 100);
        RenderedImage op  = JAI.create("ClassBreaks", pb, null);
        Classification classification =
                (Classification) op.getProperty(ClassBreaksDescriptor.CLASSIFICATION_PROPERTY);
        assertNotNull(classification);
        Number[] breaks = classification.getBreaks()[0];

        // 4 classes, 5 breaks
        assertEquals(5, breaks.length);
        assertEquals(1, breaks[0].doubleValue(), EPS);
        assertEquals(3, breaks[1].doubleValue(), EPS);
        assertEquals(16, breaks[2].doubleValue(), EPS);
        assertEquals(26, breaks[3].doubleValue(), EPS);
        assertEquals(53, breaks[4].doubleValue(), EPS);
    }

    private Double[][] getExtrema(RenderedImage image) {
        RenderedOp extremaOp = ExtremaDescriptor.create(image, null, 1, 1, false, 1, null);
        double[][] extrema= (double[][]) extremaOp.getProperty("extrema");
        Double[][] result = new Double[2][];
        result[0] = new Double[] {extrema[0][0]};
        result[1] = new Double[] {extrema[1][0]};
        return result;
    }
}
