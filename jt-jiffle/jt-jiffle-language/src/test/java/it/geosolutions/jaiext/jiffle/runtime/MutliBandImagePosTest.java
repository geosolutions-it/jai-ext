/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2018 GeoSolutions
 *    
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/* 
 *  Copyright (c) 2011, Michael Bedward. All rights reserved. 
 *   
 *  Redistribution and use in source and binary forms, with or without modification, 
 *  are permitted provided that the following conditions are met: 
 *   
 *  - Redistributions of source code must retain the above copyright notice, this  
 *    list of conditions and the following disclaimer. 
 *   
 *  - Redistributions in binary form must reproduce the above copyright notice, this 
 *    list of conditions and the following disclaimer in the documentation and/or 
 *    other materials provided with the distribution.   
 *   
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR 
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */   

package it.geosolutions.jaiext.jiffle.runtime;

import static org.junit.Assert.assertEquals;

import it.geosolutions.jaiext.jiffle.Jiffle;
import it.geosolutions.jaiext.jiffle.JiffleException;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.media.jai.TiledImage;

import it.geosolutions.jaiext.utilities.ImageUtilities;


/**
 * Unit tests for the source image pixel specifiers with multi-band images
 * <p>
 * Source image position can be specified as:
 * <pre>
 *     imageName[ b ][ xref, yref ]
 *
 * where:
 *     b is an expression for band number;
 *
 *     xref and yref are either expressions for absolute X and Y
 *     ordinates (if prefixed by '$' symbol) or offsets relative
 *     to current evaluation pixel (no prefix).
 * </pre>
 *
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class MutliBandImagePosTest {
    
    private static final int WIDTH = 10;
    private static final int NUM_PIXELS = WIDTH * WIDTH;
    private static final double TOL = 1.0e-8;

    interface Evaluator {
        double eval(double[] values);
    }

    private Map<String, Jiffle.ImageRole> imageParams;

    @Test
    public void noBandSpecifier() throws Exception {
        System.out.println("   no band specifier");

        String script = "dest = src;" ;

        Evaluator e = new Evaluator() {
            public double eval(double[] values) {
                return values[0];
            }
        };

        testScript(script, 3, e);
    }

    @Test
    public void constantBandSpecifier() throws Exception {
        System.out.println("   constant band specifier");

        String script = "dest = src[1];" ;

        Evaluator e = new Evaluator() {
            public double eval(double[] values) {
                return values[1];
            }
        };

        testScript(script, 3, e);
    }

    @Test
    public void variableBandSpecifier() throws Exception {
        System.out.println("   variable band specifier");

        String script = "init { i = 0; } dest = src[i]; i = (i + 1)%3;" ;

        Evaluator e = new Evaluator() {
            int k = 0;

            public double eval(double[] values) {
                int kk = k;
                k = (k + 1) % 3;
                return values[kk];
            }
        };

        testScript(script, 3, e);
    }

    @Test
    public void multipleBands() throws Exception {
        System.out.println("   script specifying multiple image bands");

        String script = "dest = src[0] + src[1] + src[2];" ;

        Evaluator e = new Evaluator() {

            public double eval(double[] values) {
                double sum = 0;
                for (int i = 0; i < values.length; i++) {
                    sum += values[i];
                }
                return sum;
            }
        };

        testScript(script, 3, e);
    }

    @Test
    public void bandRelativePixel() throws Exception {
        System.out.println("   band plus relative pixel position");

        String script = "dest = con(x() > 0 && y() > 0, src[1][-1,-1], NULL);" ;

        Evaluator e = new Evaluator() {
            int x = 0;
            int y = 0;
            double[] prevRow = new double[WIDTH];

            public double eval(double[] values) {
                double rtn = (x > 0 && y > 0 ? prevRow[x-1] : Double.NaN);
                prevRow[x] = values[1];
                if (++x == WIDTH) {
                    x = 0;
                    y++ ;
                }
                return rtn;
            }
        };

        testScript(script, 3, e);
    }


    @Test
    public void bandAbsolutePixel() throws Exception {
        System.out.println("   band plus absolute pixel position");

        String script = "dest = con(x() > 0 && y() > 0, src[1][$(x()-1), $(y()-1)], NULL);";

        Evaluator e = new Evaluator() {
            int x = 0;
            int y = 0;
            double[] prevRow = new double[WIDTH];

            public double eval(double[] values) {
                double rtn = (x > 0 && y > 0 ? prevRow[x-1] : Double.NaN);
                prevRow[x] = values[1];
                if (++x == WIDTH) {
                    x = 0;
                    y++ ;
                }
                return rtn;
            }
        };

        testScript(script, 3, e);
    }

    @Ignore("todo: get the compiler to throw an exception on this coding mistake")
    @Test(expected=JiffleException.class)
    public void emptyBandSpecifier() throws Exception {
        System.out.println("   malformed band specifier");

        String script = "dest = src[];" ;
        testScript(script, 3, null);
    }

    private void testScript(String script, int numSrcBands, Evaluator evaluator) throws Exception {
        imageParams = new HashMap<>();
        imageParams.put("dest", Jiffle.ImageRole.DEST);
        imageParams.put("src", Jiffle.ImageRole.SOURCE);

        Jiffle jiffle = new Jiffle(script, imageParams);
        JiffleDirectRuntime runtime = jiffle.getRuntimeInstance();

        TiledImage srcImg = createSequenceImage(numSrcBands);
        runtime.setSourceImage("src", srcImg);

        TiledImage destImg = ImageUtilities.createConstantImage(WIDTH, WIDTH, 0d);
        runtime.setDestinationImage("dest", destImg);

        runtime.evaluateAll(null);

        double[] values = new double[numSrcBands];
        for (int y = 0; y < WIDTH; y++) {
            for (int x = 0; x < WIDTH; x++) {
                for (int b = 0; b < numSrcBands; b++) {
                    values[b] = srcImg.getSampleDouble(x, y, b);
                }
                assertEquals(evaluator.eval(values), destImg.getSampleDouble(x, y, 0), TOL);
            }
        }
    }

    private double calculateValue(int x, int y, int band) {
        return band * NUM_PIXELS + y * WIDTH + x;
    }

    private TiledImage createSequenceImage(int numBands) {
        Double[] initVal = new Double[numBands];
        Arrays.fill(initVal, 0d);

        TiledImage img = ImageUtilities.createConstantImage(WIDTH, WIDTH, initVal);
        for (int b = 0; b < numBands; b++) {
            for (int y = 0; y < WIDTH; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    img.setSample(x, y, 0, calculateValue(x, y, b));
                }
            }
        }
        return img;
    }
    
}
