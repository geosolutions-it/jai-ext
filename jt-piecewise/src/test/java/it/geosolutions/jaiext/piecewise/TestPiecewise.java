/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2014 GeoSolutions


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
package it.geosolutions.jaiext.piecewise;

import it.geosolutions.jaiext.algebra.AlgebraDescriptor;
import it.geosolutions.jaiext.algebra.AlgebraDescriptor.Operator;
import it.geosolutions.jaiext.algebra.constant.OperationConstDescriptor;
import it.geosolutions.jaiext.bandselect.BandSelectDescriptor;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.stats.Statistics;
import it.geosolutions.jaiext.stats.Statistics.StatsType;
import it.geosolutions.jaiext.stats.StatisticsDescriptor;
import it.geosolutions.jaiext.testclasses.TestBase;
import it.geosolutions.jaiext.testclasses.TestData;
import it.geosolutions.rendered.viewer.RenderedImageBrowser;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.FormatDescriptor;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Class for testing the Generic Piecewise operation
 * 
 * @author Simone Giannecchini, GeoSolutions.
 * 
 * @source $URL$
 */
public class TestPiecewise extends TestBase {

    @BeforeClass
    public static void setUp() throws Exception {
        try {
            new ParameterBlockJAI(GenericPiecewiseOpImage.OPERATION_NAME);

        } catch (Exception e) {
            Assert.assertTrue("GenericPiecewise not registered", false);
        }

        // check that it exisits
        File file = TestData.file(TestPiecewise.class, "test.tif");
        Assert.assertTrue(file.exists());
    }

    /**
     * Testing {@link DefaultConstantPiecewiseTransformElement}.
     * 
     * @throws IOException
     * @throws TransformationException
     */
    @Test
    public void linearTransform() throws IOException, TransformationException {

        // /////////////////////////////////////////////////////////////////////
        //
        // byte
        //
        // /////////////////////////////////////////////////////////////////////
        DefaultPiecewiseTransform1DElement e0 = DefaultPiecewiseTransform1DElement.create("zero",
                RangeFactory.create(0, true, 100, true), RangeFactory.create(0, true, 200, true));
        Assert.assertTrue(e0 instanceof DefaultLinearPiecewiseTransform1DElement);
        // checks
        Assert.assertEquals(((DefaultLinearPiecewiseTransform1DElement) e0).getOutputMinimum(),
                e0.transform(0), 0.0);
        Assert.assertEquals(((DefaultLinearPiecewiseTransform1DElement) e0).getOutputMaximum(),
                e0.transform(e0.getInputMaximum()), 0.0);
        Assert.assertEquals(0.0, ((DefaultLinearPiecewiseTransform1DElement) e0).getOffset(), 0.0);
        Assert.assertEquals(2.0, ((DefaultLinearPiecewiseTransform1DElement) e0).getScale(), 0.0);
        Assert.assertFalse(e0.isIdentity());
        Assert.assertEquals(1, e0.getSourceDimensions());
        Assert.assertEquals(1, e0.getTargetDimensions());

        boolean exceptionThrown = false;
        try {
            Assert.assertEquals(e0.transform(Double.POSITIVE_INFINITY), 0.0, 0.0);
        } catch (Exception e) {
            exceptionThrown = true;
        }
        Assert.assertTrue(exceptionThrown);

        DefaultPiecewiseTransform1D<DefaultPiecewiseTransform1DElement> transform = new DefaultPiecewiseTransform1D<DefaultPiecewiseTransform1DElement>(
                new DefaultPiecewiseTransform1DElement[] { e0 });

        // checks
        Assert.assertEquals(0.0, transform.transform(0), 0);
        Assert.assertEquals(1, transform.getSourceDimensions());
        Assert.assertEquals(1, transform.getTargetDimensions());

    }

    /**
     * Testing {@link DefaultConstantPiecewiseTransformElement}.
     * 
     * @throws IOException
     * @throws TransformationException
     */
    @Test
    public void constantTransform() throws IOException, TransformationException {

        // /////////////////////////////////////////////////////////////////////
        //
        // byte
        //
        // /////////////////////////////////////////////////////////////////////
        DefaultPiecewiseTransform1DElement e0 = DefaultPiecewiseTransform1DElement.create("zero",
                RangeFactory.create(Double.NEGATIVE_INFINITY, true, Double.POSITIVE_INFINITY, true,
                        true), ((byte) 0));
        Assert.assertTrue(e0 instanceof DefaultConstantPiecewiseTransformElement);
        // checks
        Assert.assertEquals(0.0, e0.transform(0), 0.0);
        Assert.assertEquals(e0.transform(Double.POSITIVE_INFINITY), 0.0, 0.0);
        try {
            e0.inverse();
            Assert.assertTrue(false);
        } catch (Exception e) {

        }

        DefaultPiecewiseTransform1D<DefaultPiecewiseTransform1DElement> transform = new DefaultPiecewiseTransform1D<DefaultPiecewiseTransform1DElement>(
                new DefaultPiecewiseTransform1DElement[] { e0 });
        // checks
        Assert.assertEquals(0.0, transform.transform(0), 0);
        Assert.assertEquals(transform.transform(Double.POSITIVE_INFINITY), 0.0, 0.0);

        // /////////////////////////////////////////////////////////////////////
        //
        // int
        //
        // /////////////////////////////////////////////////////////////////////
        e0 = DefaultPiecewiseTransform1DElement.create("zero", RangeFactory.create(
                Double.NEGATIVE_INFINITY, true, Double.POSITIVE_INFINITY, true, true), 0);
        Assert.assertTrue(e0 instanceof DefaultConstantPiecewiseTransformElement);
        // checks
        Assert.assertEquals(0.0, e0.transform(0), 0.0);
        Assert.assertEquals(e0.transform(Double.POSITIVE_INFINITY), 0.0, 0.0);

        DefaultPiecewiseTransform1D<DefaultPiecewiseTransform1DElement> transform1 = new DefaultPiecewiseTransform1D<DefaultPiecewiseTransform1DElement>(
                new DefaultPiecewiseTransform1DElement[] { e0 });

        // checks
        Assert.assertEquals(0.0, transform1.transform(0), 0);
        Assert.assertEquals(transform1.transform(Double.POSITIVE_INFINITY), 0.0, 0.0);

        // hashcode and equals
        Assert.assertFalse(transform.equals(transform1));
        Assert.assertFalse(transform1.equals(transform));
        Assert.assertFalse(transform.equals(transform));
        Assert.assertFalse(transform1.equals(transform1));
        Assert.assertEquals(transform1.hashCode(), transform.hashCode());

        // /////////////////////////////////////////////////////////////////////
        //
        // double
        //
        // /////////////////////////////////////////////////////////////////////
        e0 = DefaultPiecewiseTransform1DElement.create("zero", RangeFactory.create(
                Double.NEGATIVE_INFINITY, true, Double.POSITIVE_INFINITY, true, true), 0.0);
        Assert.assertTrue(e0 instanceof DefaultConstantPiecewiseTransformElement);
        // checks
        Assert.assertEquals(0.0, e0.transform(0), 0.0);
        Assert.assertEquals(e0.transform(Double.POSITIVE_INFINITY), 0.0, 0.0);
        boolean exceptionThrown = false;
        try {
            e0.inverse();
            Assert.assertTrue(false);
        } catch (Exception e) {
            exceptionThrown = true;
        }
        Assert.assertTrue(exceptionThrown);

        transform = new DefaultPiecewiseTransform1D<DefaultPiecewiseTransform1DElement>(
                new DefaultPiecewiseTransform1DElement[] { e0 });

        // checks
        Assert.assertEquals(0.0, transform.transform(0), 0);
        Assert.assertEquals(transform.transform(Double.POSITIVE_INFINITY), 0.0, 0.0);

        // /////////////////////////////////////////////////////////////////////
        //
        // invertible
        //
        // /////////////////////////////////////////////////////////////////////
        e0 = DefaultPiecewiseTransform1DElement.create("zero",
                RangeFactory.create(3, true, 3, true), 0.0);
        Assert.assertTrue(e0 instanceof DefaultConstantPiecewiseTransformElement);
        // checks
        Assert.assertEquals(0.0, e0.transform(3), 0.0);
        Assert.assertEquals(3, e0.inverse().transform(new Position(0), null).getOrdinatePosition(),
                0);

        transform = new DefaultPiecewiseTransform1D<DefaultPiecewiseTransform1DElement>(
                new DefaultPiecewiseTransform1DElement[] { e0 });

        // checks
        Assert.assertEquals(0.0, e0.transform(3), 0);
        Assert.assertEquals(transform.transform(3), 0.0, 0.0);

    }

    /**
     * Testing testPiecewiseLogarithm.
     * 
     * @throws IOException
     * @throws TransformationException
     */
    @Test
    public void piecewiseLogarithm() throws IOException, TransformationException {

        // /////////////////////////////////////////////////////////////////////
        //
        // prepare the transform without no data management, which means gaps
        // won't be filled and exception will be thrown when trying to
        //
        // /////////////////////////////////////////////////////////////////////
        final DefaultPiecewiseTransform1DElement zero = DefaultPiecewiseTransform1DElement.create(
                "zero", RangeFactory.create(0, true, 0, true), 0);
        final DefaultPiecewiseTransform1DElement mainElement = new DefaultPiecewiseTransform1DElement(
                "natural logarithm", RangeFactory.create(0, false, 255, true),
                new MathTransformation() {

                    public Position transform(Position ptSrc, Position ptDst) {

                        return null;

                    }

                    public double transform(double value) {

                        return Math.log(value);

                    }

                    public boolean isIdentity() {

                        return false;

                    }

                    public MathTransformation inverseTransform() {

                        return null;

                    }

                    public int getTargetDimensions() {

                        return 1;

                    }

                    public int getSourceDimensions() {

                        return 1;

                    }

                    public double derivative(double value) {

                        return 1 / value;

                    }
                });
        DefaultPiecewiseTransform1D<DefaultPiecewiseTransform1DElement> transform = new DefaultPiecewiseTransform1D<DefaultPiecewiseTransform1DElement>(
                new DefaultPiecewiseTransform1DElement[] { zero, mainElement });

        // checks
        Assert.assertEquals(0.0, transform.transform(0), 0);
        Assert.assertEquals(0.0, transform.transform(1), 0);
        Assert.assertEquals(Math.log(255.0), transform.transform(255), 0);
        Assert.assertEquals(Math.log(124.0), transform.transform(124), 0);
        try {
            Assert.assertEquals(Math.log(255.0), transform.transform(256), 0);
            Assert.assertTrue(false);
        } catch (TransformationException e) {

        }

        // /////////////////////////////////////////////////////////////////////
        //
        // prepare the transform without no data management, which means gaps
        // won't be filled and exception will be thrown when trying to
        //
        // /////////////////////////////////////////////////////////////////////
        final DefaultPiecewiseTransform1DElement nodata = DefaultPiecewiseTransform1DElement
                .create("no-data", RangeFactory.create(-1, true, -1, true), Double.NaN);
        transform = new DefaultPiecewiseTransform1D<DefaultPiecewiseTransform1DElement>(
                new DefaultPiecewiseTransform1DElement[] { zero, mainElement, nodata });

        // checks
        Assert.assertEquals(0.0, transform.transform(0), 0);
        Assert.assertEquals(0.0, transform.transform(1), 0);
        Assert.assertEquals(Math.log(255.0), transform.transform(255), 0);
        Assert.assertEquals(Math.log(124.0), transform.transform(124), 0);
        try {
            Assert.assertTrue(Double.isNaN(transform.transform(256)));
            Assert.assertTrue(false);
        } catch (TransformationException e) {
            Assert.assertTrue(true);
        }

        // /////////////////////////////////////////////////////////////////////
        //
        // prepare the transform with categories that overlap, we should try an exception
        //
        // /////////////////////////////////////////////////////////////////////
        final DefaultPiecewiseTransform1DElement overlap = DefaultPiecewiseTransform1DElement
                .create("overlap", RangeFactory.create(-100, true, 12, true), Double.NaN);
        try {
            transform = new DefaultPiecewiseTransform1D<DefaultPiecewiseTransform1DElement>(
                    new DefaultPiecewiseTransform1DElement[] { zero, mainElement, overlap, nodata });
            Assert.assertTrue(false);
        } catch (Throwable e) {
            Assert.assertTrue(true);
        }

    }

    /**
     * Testing DefaultPiecewiseTransform1DElement.
     * 
     * @throws IOException
     * @throws TransformationException
     */
    @Test
    public void defaultTransform() throws IOException, TransformationException {
        // //
        //
        // Create first element and test it
        //
        // ///
        DefaultPiecewiseTransform1DElement t0 = new DefaultPiecewiseTransform1DElement("t0",
                RangeFactory.create(0.0, true, 1.0, true, false),
                PiecewiseUtilities.createLinearTransform1D(
                        RangeFactory.create(0.0, true, 1.0, true, false),
                        RangeFactory.create(200, true, 201, true)));
        Assert.assertEquals(t0.transform(0.5), 200.5, 0.0);
        Assert.assertTrue(t0.contains(0.5));
        Assert.assertTrue(t0.contains(RangeFactory.create(0.1, true, 0.9, true, false)));
        Assert.assertFalse(t0.contains(1.5));
        Assert.assertFalse(t0.contains(RangeFactory.create(0.1, true, 1.9, true, false)));
        Assert.assertTrue(t0.equals(t0));
        Assert.assertEquals(t0.inverse().transform(200.5), 0.5, 0.0);

        t0 = DefaultPiecewiseTransform1DElement.create("t0",
                RangeFactory.create(0.0, true, 1.0, true, false),
                RangeFactory.create(200, true, 201, true));
        Assert.assertFalse(t0.equals(DefaultPiecewiseTransform1DElement.create("t0",
                RangeFactory.create(0.0, true, 1.0, true, false),
                RangeFactory.create(200, true, 202, true))));
        Assert.assertEquals(t0.transform(0.5), 200.5, 0.0);
        Assert.assertEquals(t0.inverse().transform(200.5), 0.5, 0.0);

        // //
        //
        // Create second element and test it
        //
        // ///
        DefaultPiecewiseTransform1DElement t1 = DefaultPiecewiseTransform1DElement.create("t1",
                RangeFactory.create(1.0, false, 2.0, true, false), 201);
        Assert.assertEquals(t1.transform(1.5), 201, 0.0);
        Assert.assertEquals(t1.transform(1.6), 201, 0.0);
        Assert.assertFalse(t0.equals(t1));
        Assert.assertEquals(t1.transform(1.8), 201, 0.0);

        t1 = new DefaultConstantPiecewiseTransformElement("t1", RangeFactory.create(1.0, false,
                2.0, true, false), 201);
        Assert.assertEquals(t1.transform(1.5), 201, 0.0);
        Assert.assertEquals(t1.transform(1.6), 201, 0.0);
        Assert.assertEquals(t1.transform(1.8), 201, 0.0);

        DefaultPiecewiseTransform1D<DefaultPiecewiseTransform1DElement> transform = new DefaultPiecewiseTransform1D<DefaultPiecewiseTransform1DElement>(
                new DefaultPiecewiseTransform1DElement[] { t1 }, 12);
        Assert.assertEquals(transform.getName().toString(), t1.getName().toString());
        Assert.assertEquals(transform.getApproximateDomainRange().getMin().doubleValue(), 1.0, 0.0);
        Assert.assertEquals(transform.getApproximateDomainRange().getMax().doubleValue(), 2.0, 0.0);
        Assert.assertEquals(transform.transform(1.5), 201, 0.0);
        Assert.assertEquals(transform.transform(2.5), 0.0, 12.0);

        // //
        //
        // test bad cases
        //
        // ///
        boolean foundException = false;
        try {
            transform = new DefaultPiecewiseTransform1D<DefaultPiecewiseTransform1DElement>(
                    new DefaultPiecewiseTransform1DElement[] { DefaultLinearPiecewiseTransform1DElement
                            .create("", RangeFactory.create(0, true, 100, true), RangeFactory
                                    .create(Double.NEGATIVE_INFINITY, true,
                                            Double.POSITIVE_INFINITY, true, false)) });
        } catch (IllegalArgumentException e) {
            foundException = true;
        }
        Assert.assertTrue(foundException);
    }

    /**
     * Testing DefaultPassthroughPiecewiseTransform1DElement .
     * 
     * @throws IOException
     * @throws TransformationException
     */
    @Test
    public void passthroughTransform() throws IOException, TransformationException {
        // //
        //
        // testing the passthrough through direct instantion
        //
        // //
        final DefaultPassthroughPiecewiseTransform1DElement p0 = new DefaultPassthroughPiecewiseTransform1DElement(
                "p0", RangeFactory.create(0.0, true, 1.0, true, false));
        Assert.assertEquals(p0.getTargetDimensions(), 1);
        Assert.assertEquals(p0.getSourceDimensions(), 1);
        Assert.assertTrue(p0.isIdentity());
        Assert.assertEquals(p0.inverse(), SingleDimensionTransformation.IDENTITY);
        Assert.assertEquals(p0.transform(0.5), 0.5, 0.0);
        Assert.assertEquals(p0.inverse().transform(0.5), 0.5, 0.0);
        Assert.assertTrue(p0.transform(0.6) == 0.6);

        // //
        //
        // testing the transform
        //
        // //
        final DefaultPiecewiseTransform1D<DefaultPiecewiseTransform1DElement> piecewise = new DefaultPiecewiseTransform1D<DefaultPiecewiseTransform1DElement>(
                new DefaultPiecewiseTransform1DElement[] { p0 }, 11);

        Assert.assertEquals(piecewise.getApproximateDomainRange().getMin().doubleValue(), 0.0, 0.0);
        Assert.assertEquals(piecewise.getApproximateDomainRange().getMax().doubleValue(), 1.0, 0.0);
        Assert.assertEquals(piecewise.transform(0.5), 0.5, 0.0);
        Assert.assertEquals(piecewise.transform(1.5), 0.0, 11.0);

        // //
        //
        // testing the passthrough through indirect instantion
        //
        // //
        final DefaultPassthroughPiecewiseTransform1DElement p1 = new DefaultPassthroughPiecewiseTransform1DElement(
                "p1");
        Assert.assertEquals(p1.getTargetDimensions(), 1);
        Assert.assertEquals(p1.getSourceDimensions(), 1);
        Assert.assertTrue(p1.isIdentity());
        Assert.assertEquals(p1.inverse(), SingleDimensionTransformation.IDENTITY);
        Assert.assertEquals(p1.transform(0.5), 0.5, 0.0);
        Assert.assertEquals(p1.transform(111.5), 111.5, 0.0);
        Assert.assertEquals(p1.transform(123.5), 123.5, 0.0);
        Assert.assertEquals(p1.inverse().transform(657.5), 657.5, 0.0);
        Assert.assertTrue(p1.transform(0.6) == 0.6);
    }

    /**
     * Testing Short input values.
     * 
     * @throws IOException
     * @throws TransformationException
     */
    @Test
    public void lookupByteData() throws IOException, TransformationException {

        // /////////////////////////////////////////////////////////////////////
        //
        //
        //
        // /////////////////////////////////////////////////////////////////////
        RenderedImage image = JAI.create("ImageRead", TestData.file(this, "test.tif"));
        image = BandSelectDescriptor.create(image, new int[] { 0 }, null);
        image = FormatDescriptor.create(image, DataBuffer.TYPE_DOUBLE, null);

        // /////////////////////////////////////////////////////////////////////
        //
        // Build the categories
        //
        // /////////////////////////////////////////////////////////////////////
        final DefaultPiecewiseTransform1DElement c1 = DefaultLinearPiecewiseTransform1DElement
                .create("c1", RangeFactory.create(1, 128), RangeFactory.create(1, 255));
        final DefaultPiecewiseTransform1DElement c0 = DefaultLinearPiecewiseTransform1DElement
                .create("c0", RangeFactory.create(129, 255), RangeFactory.create(255, 255));
        final DefaultPiecewiseTransform1DElement nodata = DefaultLinearPiecewiseTransform1DElement
                .create("nodata", RangeFactory.create(0, 0), 0);
        final DefaultPiecewiseTransform1D<DefaultPiecewiseTransform1DElement> list = new DefaultPiecewiseTransform1D<DefaultPiecewiseTransform1DElement>(
                new DefaultPiecewiseTransform1DElement[] { c0, c1, nodata }, 0);
        ParameterBlockJAI pbj = new ParameterBlockJAI(GenericPiecewiseOpImage.OPERATION_NAME);
        pbj.addSource(image);
        pbj.setParameter("Domain1D", list);
        RenderedOp finalImage = JAI.create(GenericPiecewiseOpImage.OPERATION_NAME, pbj);
        if (INTERACTIVE) {
            RenderedImageBrowser.showChain(finalImage, false, false, null);
        } else {
            finalImage.getTiles();
        }
        finalImage.dispose();

        // ROI creation
        Rectangle roiBounds = new Rectangle(image.getMinX() + 5, image.getMinY() + 5,
                image.getWidth() / 4, image.getHeight() / 4);
        ROI roi = new ROIShape(roiBounds);

        // NoData creation
        Range nodataRange = RangeFactory.create(12, 13);

        // Testing with ROI
        pbj = new ParameterBlockJAI(GenericPiecewiseOpImage.OPERATION_NAME);
        pbj.addSource(image);
        pbj.setParameter("Domain1D", list);
        pbj.setParameter("roi", roi);
        finalImage = JAI.create(GenericPiecewiseOpImage.OPERATION_NAME, pbj);
        if (INTERACTIVE) {
            RenderedImageBrowser.showChain(finalImage, false, false, null);
        } else {
            finalImage.getTiles();
        }
        finalImage.dispose();

        // Testing with Nodata
        pbj = new ParameterBlockJAI(GenericPiecewiseOpImage.OPERATION_NAME);
        pbj.addSource(image);
        pbj.setParameter("Domain1D", list);
        pbj.setParameter("nodata", nodataRange);
        finalImage = JAI.create(GenericPiecewiseOpImage.OPERATION_NAME, pbj);
        if (INTERACTIVE) {
            RenderedImageBrowser.showChain(finalImage, false, false, null);
        } else {
            finalImage.getTiles();
        }
        finalImage.dispose();

        // Testing with both ROI and NoData
        pbj = new ParameterBlockJAI(GenericPiecewiseOpImage.OPERATION_NAME);
        pbj.addSource(image);
        pbj.setParameter("Domain1D", list);
        pbj.setParameter("roi", roi);
        pbj.setParameter("nodata", nodataRange);
        finalImage = JAI.create(GenericPiecewiseOpImage.OPERATION_NAME, pbj);
        if (INTERACTIVE) {
            RenderedImageBrowser.showChain(finalImage, false, false, null);
        } else {
            finalImage.getTiles();
        }
        finalImage.dispose();
    }

    /**
     * Binary test-case.
     * 
     * @throws IOException
     */
    @Test
    public void logarithmicTransform() throws IOException {
        JAI.getDefaultInstance().getTileScheduler().setParallelism(1);
        // /////////////////////////////////////////////////////////////////////
        //
        //
        // /////////////////////////////////////////////////////////////////////
        RenderedOp image = JAI.create("ImageRead", TestData.file(this, "test.tif"));
        image = BandSelectDescriptor.create(image, new int[] { 0 }, null);
        image = FormatDescriptor.create(image, DataBuffer.TYPE_DOUBLE, null);
        image = AlgebraDescriptor.create(Operator.ABSOLUTE, null, null, 0, null, image);
        image = OperationConstDescriptor.create(image, new double[] { 10 }, Operator.SUM, null,
                null, 0, null);

        StatsType[] stats = new StatsType[] { StatsType.EXTREMA };
        image = StatisticsDescriptor.create(image, 1, 1, null, null, false, new int[] { 0 }, stats,
                null);
        Statistics stat = ((Statistics[][]) image.getProperty(Statistics.STATS_PROPERTY))[0][0];
        double[] result = (double[]) stat.getResult();
        final double minimum = result[0];
        final double maximum = result[1];

        final DefaultPiecewiseTransform1DElement mainElement = new DefaultPiecewiseTransform1DElement(
                "natural logarithm", RangeFactory.create(minimum, true, maximum, true),
                new MathTransformation() {

                    public double derivative(double arg0) {

                        return 1 / arg0;
                    }

                    public double transform(double arg0) {

                        return minimum + 1.2 * Math.log(arg0 / minimum)
                                * ((maximum - minimum) / (Math.log(maximum / minimum)));
                    }

                    public boolean isIdentity() {
                        return false;
                    }

                    public int getSourceDimensions() {
                        return 1;
                    }

                    public int getTargetDimensions() {
                        return 1;
                    }

                    public MathTransformation inverseTransform() {
                        throw new UnsupportedOperationException();
                    }

                    public Position transform(Position ptSrc, Position ptDst) {
                        throw new UnsupportedOperationException();
                    }

                });
        DefaultPiecewiseTransform1D<DefaultPiecewiseTransform1DElement> transform = new DefaultPiecewiseTransform1D<DefaultPiecewiseTransform1DElement>(
                new DefaultPiecewiseTransform1DElement[] { mainElement }, 0);

        RenderedOp finalImage = null;

        ParameterBlockJAI pbj = new ParameterBlockJAI(GenericPiecewiseOpImage.OPERATION_NAME);
        pbj.addSource(image);
        pbj.setParameter("Domain1D", transform);
        pbj.setParameter("bandIndex", new Integer(0));
        finalImage = JAI.create(GenericPiecewiseOpImage.OPERATION_NAME, pbj, null);
        if (INTERACTIVE) {
            RenderedImageBrowser.showChain(finalImage, false, false, null);
        } else {
            finalImage.getTiles();
        }
        finalImage.dispose();

        // ROI creation
        Rectangle roiBounds = new Rectangle(image.getMinX() + 5, image.getMinY() + 5,
                image.getWidth() / 4, image.getHeight() / 4);
        ROI roi = new ROIShape(roiBounds);

        pbj = new ParameterBlockJAI(GenericPiecewiseOpImage.OPERATION_NAME);
        pbj.addSource(image);
        pbj.setParameter("Domain1D", transform);
        pbj.setParameter("bandIndex", new Integer(0));
        pbj.setParameter("roi", roi);
        finalImage = JAI.create(GenericPiecewiseOpImage.OPERATION_NAME, pbj, null);
        if (INTERACTIVE) {
            RenderedImageBrowser.showChain(finalImage, false, false, null);
        } else {
            finalImage.getTiles();
        }
        finalImage.dispose();
    }
}
